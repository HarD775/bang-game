//
// $Id$

package com.threerings.bang.game.data.piece;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.HoldEffect;
import com.threerings.bang.game.data.effect.FoolsNuggetEffect;
import com.threerings.bang.game.data.effect.NuggetEffect;
import com.threerings.bang.game.data.effect.ShotEffect;
import com.threerings.bang.game.data.piece.Piece;

/**
 * Handles some special custom behavior needed for the Sharpshooter.
 */
public class Sharpshooter extends Unit
{
    @Override // documentation inherited
    public Effect willShoot (BangObject bangobj, Piece target, ShotEffect shot)
    {
        // sharpshooters always cause their target to drop any bonus they
        // are holding whether they die or not
        if (target instanceof Unit) {
            Unit unit = (Unit)target;
            if (unit.holding != null) {
                return HoldEffect.dropBonus(bangobj, unit, pieceId,
                    unit.holding);
            }
        }
        return null;
    }

    @Override // documentation inherited
    protected int computeDamage (Piece target)
    {
        // we do 150% of the damage of the gunslinger in exchange for not
        // being able to shoot immediately next door
        return 15 * super.computeDamage(target) / 10;
    }
}
