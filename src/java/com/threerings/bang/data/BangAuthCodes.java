//
// $Id$

package com.threerings.bang.data;

import com.threerings.presents.data.AuthCodes;

/**
 * Additional auth codes for the Bang! server.
 */
public interface BangAuthCodes extends AuthCodes
{
    /** A message bundle used during client authorization. */
    public static final String AUTH_MSGS = "logon";

    /** A code indicating that the user has been banned from the
     * server. */
    public static final String BANNED = "m.banned";

    /** A code indicating that the user has been temporarily banned from the
     * server. */
    public static final String TEMP_BANNED = "m.temp_banned";

    /** A code indicating that the machine is tainted and no new accounts will
     * be able to logon from it. */
    public static final String MACHINE_TAINTED = "m.machine_tainted";

    /** A code indicating a bounced check or reversed payment. */
    public static final String DEADBEAT = "m.deadbeat";

    /** A code indicating that the server is under maintenance and normal user
     * login is not allowed. */
    public static final String UNDER_MAINTENANCE = "m.under_maintenance";

    /** A code indicating that the server is under heavy load and this player's
     * authentication request was throttled. */
    public static final String UNDER_LOAD = "m.under_load";

    /** A code indicating that the client version is out of date. */
    public static final String VERSION_MISMATCH = "m.version_mismatch";

    /** A code indicating that the client has a newer version of the code than
     * the server which generally means we're in the middle of updating the
     * game. */
    public static final String NEWER_VERSION = "m.newer_version";

    /** A code used during account creation authentication indicating that the
     * username requested already exists. */
    public static final String NAME_IN_USE = "m.name_in_use";
}
