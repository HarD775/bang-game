//
// $Id$

package com.samskivert.bang.data.effect;

import java.util.Iterator;

import com.samskivert.util.IntListUtil;

import com.samskivert.bang.data.BangObject;
import com.samskivert.bang.data.surprise.MissileSurprise;
import com.samskivert.bang.data.surprise.Surprise;

import static com.samskivert.bang.Log.log;

/**
 * Delivers a surprise to the specified player.
 */
public class GrantSurpriseEffect extends Effect
{
    public int player;

    public GrantSurpriseEffect (int player)
    {
        this.player = player;
    }

    public GrantSurpriseEffect ()
    {
    }

    public void prepare (BangObject bangobj)
    {
        int[] power = bangobj.computePower();
        int tpower = IntListUtil.sum(power);

        // make sure our player has room for another surprise
        int have = 0;
        for (Iterator iter = bangobj.surprises.iterator(); iter.hasNext(); ) {
            Surprise s = (Surprise)iter.next();
            if (s.owner == player) {
                have++;
            }
        }
        if (have >= 3) {
            log.info("No soup four you! " + player + ".");
            return;
        }

        MissileSurprise ms = new MissileSurprise();
        ms.init(player);
        // if our player is "in the nooksak", give them a big missile
        if (power[player] < 30) {
            ms.power = 100;
            ms.radius = 4;
        } else if (power[player] < tpower/3) {
            ms.power = 80;
            ms.radius = 3;
        }
        bangobj.addToSurprises(ms);
    }

    public void apply (BangObject bangobj, Observer obs)
    {
        // NOOP
    }
}
