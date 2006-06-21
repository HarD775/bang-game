//
// $Id$

package com.threerings.bang.game.data.card;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.AdjustTickEffect;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

/**
 * A card that allows the player to skip a unit ahead 2 ticks.
 */
public class HalfGiddyUp extends Card
{
    @Override // documentation inherited
    public String getType ()
    {
        return "half_giddy_up";
    }

    @Override // documentation inherited
    public int getWeight ()
    {
        return 60;
    }

    @Override // documentation inherited
    public boolean isValidPiece (BangObject bangobj, Piece target)
    {
        return (target instanceof Unit && target.isAlive());
    }

    @Override // documentation inherited
    public Effect activate (Object target)
    {
        return new AdjustTickEffect((Integer)target, -2);
    }

    @Override // documentation inherited
    public int getScripCost ()
    {
        return 200;
    }

    @Override // documentation inherited
    public int getCoinCost ()
    {
        return 0;
    }
}
