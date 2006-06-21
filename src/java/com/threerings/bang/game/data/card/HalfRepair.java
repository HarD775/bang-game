//
// $Id$

package com.threerings.bang.game.data.card;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.RepairEffect;
import com.threerings.bang.game.data.piece.Piece;

/**
 * A card that allows the player to half repair a single unit.
 */
public class HalfRepair extends Card
{
    @Override // documentation inherited
    public String getType ()
    {
        return "half_repair";
    }

    @Override // documentation inherited
    public boolean isValidPiece (BangObject bangobj, Piece target)
    {
        return (target.isTargetable());
    }

    @Override // documentation inherited
    public int getWeight ()
    {
        return 75;
    }

    @Override // documentation inherited
    public Effect activate (Object target)
    {
        RepairEffect effect = new RepairEffect((Integer)target);
        effect.baseRepair = 50;
        return effect;
    }

    @Override // documentation inherited
    public int getScripCost ()
    {
        return 80;
    }

    @Override // documentation inherited
    public int getCoinCost ()
    {
        return 0;
    }
}
