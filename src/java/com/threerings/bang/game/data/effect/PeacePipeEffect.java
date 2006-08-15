//
// $Id$

package com.threerings.bang.game.data.effect;

import com.threerings.bang.game.data.piece.Hindrance;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

/**
 * An effect that limits a units ability to attack until it is attacked or
 * expires. 
 */
public class PeacePipeEffect extends SetHindranceEffect
{
    @Override // documentation inherited
    protected Hindrance createHindrance (final Unit target)
    {
        return new Hindrance() {
            public String getName () {
                return "peace_pipe";
            }
            public boolean validTarget (
                    Unit shooter, Piece targer, boolean allowSelf) {
                return _expired;
            }
            public void wasDamaged (int newDamage) {
                _expired = true;
            }
            public boolean isExpired (short tick) {
                return _expired || super.isExpired(tick);
            }
            protected int duration () {
                return 12;
            }
            protected boolean _expired = false;
        };
    }

    @Override // documentation inherited
    protected String getEffectName ()
    {
        return "indian_post/peace_pipe";
    }
}
