//
// $Id$

package com.samskivert.bang.data.piece;

import com.samskivert.bang.client.sprite.BonusSprite;
import com.samskivert.bang.client.sprite.PieceSprite;
import com.samskivert.bang.data.effect.Effect;

/**
 * Represents an exciting bonus waiting to be picked up by a player on the
 * board. Bonuses may generate full-blown effects or just influence the
 * piece that picked them up.
 */
public abstract class Bonus extends Piece
{
    /**
     * Called when a piece has landed on this bonus and is activating it,
     * this should return an object indicating the effect that the bonus
     * has on this piece or the entire board. Those effects will be
     * processed at the end of the tick.
     */
    public abstract Effect affect (Piece other);

    @Override // documentation inherited
    public boolean preventsOverlap (Piece lapper)
    {
        return false;
    }

    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new BonusSprite("unknown");
    }
}
