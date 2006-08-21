//
// $Id$

package com.threerings.bang.server.ooo;

import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.Invoker;
import com.samskivert.util.StringUtil;

import com.threerings.util.IdentUtil;
import com.threerings.util.MessageBundle;
import com.threerings.util.Name;

import com.threerings.user.OOOUser;
import com.threerings.user.OOOUserManager;
import com.threerings.user.OOOUserRepository;

import com.threerings.presents.net.AuthRequest;
import com.threerings.presents.net.AuthResponse;
import com.threerings.presents.net.AuthResponseData;

import com.threerings.presents.server.Authenticator;
import com.threerings.presents.server.net.AuthingConnection;

import com.threerings.bang.admin.server.RuntimeConfig;

import com.threerings.bang.data.BangAuthResponseData;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.BangCredentials;
import com.threerings.bang.data.BangTokenRing;
import com.threerings.bang.server.BangClientResolver;
import com.threerings.bang.server.BangServer;
import com.threerings.bang.server.ServerConfig;
import com.threerings.bang.server.persist.PlayerRecord;
import com.threerings.bang.util.BangUtil;
import com.threerings.bang.util.DeploymentConfig;

import static com.threerings.bang.Log.log;
import static com.threerings.bang.data.BangAuthCodes.*;

/**
 * Delegates authentication to the OOO user manager.
 */
public class OOOAuthenticator extends Authenticator
{
    public OOOAuthenticator ()
    {
        try {
            // we get our user manager configuration from the ocean config
            _usermgr = new OOOUserManager(
                ServerConfig.config.getSubProperties("oooauth"),
                BangServer.conprov);
            _authrep = (OOOUserRepository)_usermgr.getRepository();
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to initialize OOO authenticator. " +
                    "Users will be unable to log in.", pe);
        }
    }

    @Override
    protected AuthResponseData createResponseData ()
    {
        return new BangAuthResponseData();
    }

    // from abstract Authenticator
    protected void processAuthentication (
            AuthingConnection conn, AuthResponse rsp)
        throws PersistenceException
    {
        AuthRequest req = conn.getAuthRequest();
        BangAuthResponseData rdata = (BangAuthResponseData) rsp.getData();

        // make sure we were properly initialized
        if (_authrep == null) {
            rdata.code = SERVER_ERROR;
            return;
        }

        // make sure they've got the correct version
        long cvers = 0L;
        long svers = DeploymentConfig.getVersion();
        try {
            cvers = Long.parseLong(req.getVersion());
        } catch (Exception e) {
            // ignore it and fail below
        }
        if (svers != cvers) {
            if (cvers > svers) {
                rdata.code = NEWER_VERSION;
                rsp.getData().code = MessageBundle.tcompose(
                    VERSION_MISMATCH, "" + svers);
            }
            log.info("Refusing wrong version " +
                     "[creds=" + req.getCredentials() +
                     ", cvers=" + cvers + ", svers=" + svers + "].");
            return;
        }

        // make sure they've sent valid credentials
        BangCredentials creds;
        try {
            creds = (BangCredentials) req.getCredentials();
        } catch (ClassCastException cce) {
            log.warning("Invalid creds " + req.getCredentials() + ".");
            rdata.code = SERVER_ERROR;
            return;
        }

        // check their provided machine identifier
        String username = creds.getUsername().toString();
        if (StringUtil.isBlank(creds.ident)) {
            log.warning("Received blank ident [creds=" + creds + "].");
            BangServer.generalLog(
                "refusing_spoofed_ident " + username +
                " ip:" + conn.getInetAddress());
            rdata.code = SERVER_ERROR;
            return;
        }

        // if they supplied a known non-unique machine identifier, create
        // one for them
        if (IdentUtil.isBogusIdent(creds.ident.substring(1))) {
            String sident = StringUtil.md5hex(
                "" + Math.random() + System.currentTimeMillis());
            creds.ident = "S" + IdentUtil.encodeIdent(sident);
            BangServer.generalLog("creating_ident " + username +
                                  " ip:" + conn.getInetAddress() +
                                  " id:" + creds.ident);
            rdata.ident = creds.ident;
        }

        // convert the encrypted ident to the original MD5 hash
        try {
            String prefix = creds.ident.substring(0, 1);
            creds.ident = prefix +
                IdentUtil.decodeIdent(creds.ident.substring(1));
        } catch (Exception e) {
            log.warning("Received spoofed ident [who=" + username +
                        ", err=" + e.getMessage() + "].");
            BangServer.generalLog("refusing_spoofed_ident " + username +
                                  " ip:" + conn.getInetAddress() +
                                  " id:" + creds.ident);
            rdata.code = SERVER_ERROR;
            return;
        }

        // load up their user account record
        OOOUser user = _authrep.loadUser(username, true);
        if (user == null) {
            rdata.code = NO_SUCH_USER;
            return;
        }

        // we need to find out if this account has ever logged in so that
        // we can decide how to handle tainted idents; so we load up the
        // player record for this account; if this player makes it through
        // the gauntlet, we'll stash this away in a place that the client
        // resolver can get its hands on it so that we can avoid loading
        // the record twice during authentication
        PlayerRecord prec = BangServer.playrepo.loadPlayer(username);

        // make sure this player has access to this server's town
        int serverTownIdx = BangUtil.getTownIndex(ServerConfig.townId);
        if (serverTownIdx > 0) {
            String townId = (prec == null) ?
                BangCodes.FRONTIER_TOWN : prec.townId;
            if (BangUtil.getTownIndex(townId) < serverTownIdx) {
                log.warning("Rejecting access to town server by " +
                            "non-ticket-holder [who=" + username +
                            ", stownId=" + ServerConfig.townId +
                            ", ptownId=" + townId + "].");
                rdata.code = NO_TICKET;
                return;
            }
        }

        // check to see whether this account has been banned or if this is
        // a first time user logging in from a tainted machine
        int vc = _authrep.validateUser(user, creds.ident, prec == null);
        switch (vc) {
            // various error conditions
            case OOOUserRepository.ACCOUNT_BANNED:
               rdata.code = BANNED;
               return;
            case OOOUserRepository.NEW_ACCOUNT_TAINTED:
               rdata.code = MACHINE_TAINTED;
               return;
        }

        // check whether we're restricting non-insider login
        if (!RuntimeConfig.server.openToPublic &&
            !user.holdsToken(OOOUser.INSIDER) &&
            !user.holdsToken(OOOUser.TESTER) &&
            !user.isSupportPlus()) {
            rdata.code = NON_PUBLIC_SERVER;
            return;
        }

        // check whether we're restricting non-admin login
        if (!RuntimeConfig.server.nonAdminsAllowed &&
            !user.isSupportPlus()) {
            rdata.code = UNDER_MAINTENANCE;
            return;
        }

        // now check their password
        if (!user.password.equals(creds.getPassword())) {
            rdata.code = INVALID_PASSWORD;
            return;
        }

        // configure a token ring for this user
        int tokens = 0;
        if (user.holdsToken(OOOUser.ADMIN)) {
            tokens |= BangTokenRing.ADMIN;
            tokens |= BangTokenRing.INSIDER;
        }
        if (user.holdsToken(OOOUser.INSIDER)) {
            tokens |= BangTokenRing.INSIDER;
        }
        rsp.authdata = new BangTokenRing(tokens);

        // replace the username in their credentials with the
        // canonical name in their user record as that username will
        // later be stuffed into their user object
        creds.setUsername(new Name(user.username));

        // log.info("User logged on [user=" + user.username + "].");
        rdata.code = BangAuthResponseData.SUCCESS;

        // pass their player record to the client resolver for retrieval
        // later in the logging on process
        if (prec != null) {
            BangClientResolver.stashPlayer(prec);
        }
    }

    protected OOOUserRepository _authrep;
    protected OOOUserManager _usermgr;
}
