//
// $Id$

package com.threerings.bang.server;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.logging.Level;

import com.samskivert.util.Config;
import com.samskivert.util.StringUtil;

import com.threerings.presents.client.Client;
import com.threerings.presents.server.Authenticator;

import com.threerings.bang.data.BangCodes;

import static com.threerings.bang.Log.log;

/**
 * Provides access to installation specific configuration. Properties that
 * are specific to a particular Bang! server installation are accessed via
 * this class.
 */
public class ServerConfig
{
    /** The name assigned to this server installation. */
    public static String serverName;

    /** The root directory of the server installation. */
    public static File serverRoot;

    /** The ports on which we are listening for client connections. */
    public static int[] serverPorts;

    /** Provides access to our config properties. <em>Do not</em> modify
     * these properties! */
    public static Config config;

    /**
     * Returns the JDBC configuration.
     */
    public static Properties getJDBCConfig ()
    {
        return config.getSubProperties("db");
    }

    /**
     * Instantiates and returns the authenticator that the server will use
     * to authenticate client connections.
     */
    public static Authenticator getAuthenticator ()
    {
        String aclass = config.getValue("server_auth", "");
        try {
            if (!StringUtil.isBlank(aclass)) {
                return (Authenticator)Class.forName(aclass).newInstance();
            }
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to instantiate authenticator " +
                    "[class=" + aclass + "]", e);
        }
        return null;
    }

    /**
     * Returns the town that is handled by this server instance.
     */
    public static String getTownId ()
    {
        return config.getValue("town_id", BangCodes.FRONTIER_TOWN);
    }

    /**
     * Configures the install config with the path to our installation
     * properties file. This method is called automatically.
     */
    protected static void init (String propPath)
    {
        Properties props = new Properties();
        try {
            if (propPath != null) {
                propPath = propPath + File.separator + "server.properties";
                props.load(new FileInputStream(propPath));
            } else {
                propPath = "server.properties";
                props.load(ServerConfig.class.getClassLoader().
                           getResourceAsStream(propPath));
            }
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to load install properties " +
                    "[path=" + propPath + "].", e);
        }
        config = new Config("server", props);

        // fill in our standard properties
        serverName = config.getValue("server_name", "bang");
        serverRoot = new File(config.getValue("server_root", "/tmp"));
        serverPorts = config.getValue(
            "server_ports", Client.DEFAULT_SERVER_PORTS);
    }

    static {
        init(System.getProperty("bang.home"));
    }
}
