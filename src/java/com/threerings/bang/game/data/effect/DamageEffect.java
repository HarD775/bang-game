//
// $Id$

package com.threerings.bang.game.data.effect;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.IntIntMap;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;

import static com.threerings.bang.Log.log;

/**
 * Generic damage effect.
 */
public class DamageEffect extends Effect
{
    /** The id of the damaged piece. */
    public int pieceId;

    /** The base amount of damage to inflict. */
    public int baseDamage;
    
    /** The piece's new damage amount. */
    public int ndamage;
    
    /** The piece's death effect, if it died. */
    public Effect deathEffect;
    
    public DamageEffect ()
    {
    }
    
    public DamageEffect (Piece piece, int damage)
    {
        pieceId = piece.pieceId;
        baseDamage = damage;
    }
    
    @Override // documentation inherited
    public int[] getAffectedPieces ()
    {
        if (deathEffect == null) {
            return new int[] { pieceId };
        }
        return ArrayUtil.append(deathEffect.getAffectedPieces(), pieceId);
    }
    
    @Override // documentation inherited
    public int[] getWaitPieces ()
    {
        return (deathEffect == null) ? NO_PIECES : deathEffect.getWaitPieces();
    }
    
    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        Piece piece = bangobj.pieces.get(pieceId);
        if (piece != null) {
            ndamage = Math.min(100, piece.damage + baseDamage);
            dammap.increment(piece.owner, ndamage - piece.damage);
            if (ndamage == 100 && piece.damage < 100) {
                deathEffect = piece.willDie(bangobj, -1);
                if (deathEffect != null) {
                    deathEffect.prepare(bangobj, dammap);
                }
            }
        } else {
            log.warning("Damage effect missing piece [id=" + pieceId + "].");
        }
    }
    
    @Override // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        if (deathEffect != null) {
            deathEffect.apply(bangobj, obs);
        }
        Piece piece = bangobj.pieces.get(pieceId);
        return damage(bangobj, obs, -1, piece, ndamage, ShotEffect.DAMAGED);
    }
    
    @Override // documentation inherited
    public int getBaseDamage (Piece piece)
    {
        return baseDamage;
    }
}
