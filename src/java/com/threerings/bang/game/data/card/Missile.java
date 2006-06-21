//
// $Id$

package com.threerings.bang.game.data.card;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.AreaDamageEffect;
import com.threerings.bang.game.data.effect.Effect;

/**
 * A card that allows the player to launch a missile that does an area of
 * effect damage.
 */
public class Missile extends AreaCard
{
    public int power = 60;

    public int radius = 1;

    @Override // documentation inherited
    public void init (BangObject bangobj, int owner)
    {
        super.init(bangobj, owner);

        // if they're getting this card during a game, potentially adjust it
        // based on their current rank
        if (bangobj.state == BangObject.IN_PLAY) {
            if (bangobj.pdata[owner].power < 30) {
                power = 80;
                radius = 3;
            } else if (bangobj.pdata[owner].powerFactor < 0.34) {
                power = 70;
                radius = 2;
            }
        }
    }

    @Override // documentation inherited
    public String getType ()
    {
        return "missile";
    }

    @Override // documentation inherited
    public int getRadius ()
    {
        return radius;
    }

    @Override // documentation inherited
    public int getWeight ()
    {
        return 25;
    }

    @Override // documentation inherited
    public Effect activate (int x, int y)
    {
        return new AreaDamageEffect(owner, power, getRadius(), x, y);
    }

    @Override // documentation inherited
    public int getScripCost ()
    {
        return 150;
    }

    @Override // documentation inherited
    public int getCoinCost ()
    {
        return 0;
    }
}
