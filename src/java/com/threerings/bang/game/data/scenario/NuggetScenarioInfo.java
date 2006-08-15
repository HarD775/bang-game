//
// $Id$

package com.threerings.bang.game.data.scenario;

import com.threerings.bang.data.Stat;
import com.threerings.bang.game.data.effect.NuggetEffect;

/**
 * Contains metadata on a scenario involving nuggets.
 */
public abstract class NuggetScenarioInfo extends ScenarioInfo
{
    /** The amount of points earned per nugget at the end of the game. */
    public static final int POINTS_PER_NUGGET = 50;

    @Override // from ScenarioInfo
    public Stat.Type getObjective ()
    {
        return Stat.Type.NUGGETS_CLAIMED;
    }

    @Override // from ScenarioInfo
    public int getPointsPerObjective ()
    {
        return POINTS_PER_NUGGET;
    }

    /**
     * Returns the path to sound clips that should be preloaded when playing
     * this scenario.
     */
    public String[] getPreLoadClips ()
    {
        return PRELOAD_CLIPS;
    }

    /** Sound clips that must be preloaded for nugget scenarios. */
    protected static final String[] PRELOAD_CLIPS = {
        "rsrc/effects/" + NuggetEffect.NUGGET_ADDED + ".wav",
        "rsrc/effects/" + NuggetEffect.NUGGET_REMOVED + ".wav",
        "rsrc/effects/" + NuggetEffect.PICKED_UP_NUGGET + ".wav",
    };
}
