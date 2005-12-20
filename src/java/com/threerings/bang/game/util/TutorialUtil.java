//
// $Id$

package com.threerings.bang.game.util;

import java.util.HashMap;
import java.util.logging.Level;

import com.threerings.resource.ResourceManager;
import com.threerings.util.CompiledConfig;

import com.threerings.bang.game.data.TutorialConfig;

import static com.threerings.bang.Log.log;

/**
 * Contains tutorial related utility methods, chiefly loading and caching
 * tutorial configurations.
 */
public class TutorialUtil
{
    /**
     * Loads and caches the tutorial with the specified identifier. The
     * returned configuration will a clone so that it may be manipulated by the
     * caller.
     */
    public static TutorialConfig loadTutorial (
        ResourceManager rmgr, String ident)
    {
        TutorialConfig config = _configs.get(ident);
        if (config == null) {
            String path = "tutorials/" + ident + ".dat";
            try {
                config = (TutorialConfig)
                    CompiledConfig.loadConfig(rmgr.getResource(path));
            } catch (Exception e) {
                log.log(Level.WARNING, "Failed to load tutorial config " +
                        "[path=" + path + "]", e);
                config = _error;
            }
            _configs.put(ident, config);
        }
        return (TutorialConfig)config.clone();
    }

    /** We keep all loaded tutorial configs in memory. */
    protected static HashMap<String,TutorialConfig> _configs =
        new HashMap<String,TutorialConfig>();

    /** An error tutorial used if there's a problem loading. */
    protected static TutorialConfig _error;
    static {
        _error = new TutorialConfig();
        _error.board = "error";
        _error.players = 1;
        TutorialConfig.Text action = new TutorialConfig.Text();
        action.message = "loading_error";
        _error.addAction(action);
    }
}
