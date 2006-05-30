//
// $Id$

package com.threerings.bang.util;

import java.net.URL;
import java.util.logging.Level;

import com.samskivert.util.Config;

import com.threerings.presents.client.Client;

import static com.threerings.bang.Log.log;

/**
 * Properties that are specific to a particular Bang! game deployment (client
 * and server code plus game media) are accessed via this class.
 */
public class DeploymentConfig
{
    /** Provides access to our config properties. <em>Do not</em> modify these
     * properties! */
    public static Config config = new Config("deployment");

    /** Contains our deployment version information. */
    public static Config build = new Config("build");

    /**
     * Returns the version associated with this build of the deployment's code.
     */
    public static long getVersion ()
    {
        return build.getValue("version", 0L);
    }

    /**
     * Returns the default locale for this server. This isn't the actual locale
     * setting of the server or client (use {@link java.util.Locale#getDefault}
     * to obtain that), but is used to determine how the server will handle
     * generation and validation of non-translated proper names.
     */
    public static String getDefaultLocale ()
    {
        return config.getValue("default_locale", "en");
    }

    /**
     * Returns the hostname of the server to which we should connect when
     * logging in.
     */
    public static String getServerHost ()
    {
        return config.getValue("server_host", "localhost");
    }

    /**
     * Returns the port on which we should connect to the server.
     * @see #getServerHost
     */
    public static int[] getServerPorts ()
    {
        return config.getValue("server_ports", Client.DEFAULT_SERVER_PORTS);
    }

    /**
     * Returns the URL from which HTML content is loaded.
     */
    public static URL getDocBaseURL ()
    {
        return getURL("doc_base_url");
    }

    /**
     * Returns the URL to which bug reports should be submitted.
     */
    public static URL getBugSubmitURL ()
    {
        return getURL("bug_submit_url");
    }

    /**
     * Returns the URL to send players to create a new account.
     */
    public static URL getNewAccountURL ()
    {
        return getURL("new_account_url");
    }

    /**
     * Returns the URL for the server status page.
     */
    public static URL getServerStatusURL ()
    {
        return getURL("server_status_url");
    }

    /** Helper function for getting URL properties. */
    protected static URL getURL (String key)
    {
        String url = config.getValue(key, "not_specified");
        try {
            return new URL(url);
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to parse " + key + ": " + url, e);
            return null;
        }
    }

    /** Our most recent cached deployment version. */
    protected static long _deploymentVersion;

    /** The time at which we last cached our deployment version. */
    protected static long _lastVersionCheck;

    /** We recheck our deployment version every minute. */
    protected static final long VERSION_CHECK_INTERVAL = 60 * 1000L;
}
