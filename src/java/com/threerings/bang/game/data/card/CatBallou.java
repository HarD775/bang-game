//
// $Id$

package com.threerings.bang.game.data.card;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.DropCardEffect;
import com.threerings.bang.game.data.effect.Effect;

/**
 * A card that allows the player to retire one of an opponent's cards at
 * random.
 */
public class CatBallou extends Card
{
    @Override // documentation inherited
    public String getType ()
    {
        return "cat_ballou";
    }

    @Override // documentation inherited
    public PlacementMode getPlacementMode ()
    {
        return PlacementMode.VS_PLAYER;
    }

    @Override // documentation inherited
    public String getTownId ()
    {
        return BangCodes.FRONTIER_TOWN;
    }

    @Override // documentation inherited
    public int getWeight ()
    {
        return 35;
    }

    @Override // documentation inherited
    public boolean isValidPlayer (BangObject bangobj, int pidx)
    {
        return pidx != owner && bangobj.countPlayerCards(pidx) > 0;
    }

    @Override // documentation inherited
    public Effect activate (BangObject bangobj, Object target)
    {
        return new DropCardEffect(((Integer)target).intValue());
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
