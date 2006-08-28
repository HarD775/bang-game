//
// $Id$

package com.threerings.bang.game.data.effect;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Hindrance;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

/**
 * An effect that causes the next shot by a piece to damage themself.
 */
public class MisfireEffect extends SetHindranceEffect
{
    @Override // documentation inherited
    public boolean isApplicable ()
    {
        return super.isApplicable() && _unit.getConfig().gunUser;
    }

    @Override // documentation inherited
    protected Hindrance createHindrance (Unit target)
    {
        return new Hindrance() {
            public String getName () {
                return "misfire";
            }

            public ShotEffect shoot (BangObject bangobj, Unit shooter,
                    Piece target, float scale)
            {
                _expired = true;
                return new FailedShotEffect(shooter, target,
                        shooter.computeScaledDamage(bangobj, target, scale));
            }

            public boolean isExpired (short tick)
            {
                return _expired;
            }

            public boolean didAdjustAttack ()
            {
                return true;
            }

            boolean _expired = false;
        };
    }

    @Override // documentation inherited
    protected String getEffectName ()
    {
        return "frontier_town/misfire";
    }
}
