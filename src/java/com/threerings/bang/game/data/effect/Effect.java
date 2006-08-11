//
// $Id$

package com.threerings.bang.game.data.effect;

import java.awt.Rectangle;
import java.awt.Point;

import java.util.ArrayList;
import java.util.Random;

import com.samskivert.util.IntIntMap;

import com.threerings.util.MessageBundle;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.bang.data.Stat;
import com.threerings.bang.game.client.EffectHandler;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

import static com.threerings.bang.Log.log;

/**
 * Encapsulates any effect on a piece in the game. All state changes are
 * communicated through an ordered stream of effects.
 */
public abstract class Effect extends SimpleStreamableObject
{
    /** An effect to use when a piece's internal status is updated and it
     * should be refreshed, but no other visible change will take place. */
    public static final String UPDATED = "updated";

    /** Provides a mechanism for observing the individual effects that take
     * place when applying an effect to the board and pieces. */
    public static interface Observer
    {
        /**
         * Indicates that the specified piece was added to the board.
         */
        public void pieceAdded (Piece piece);

        /**
         * Indicates that the specified piece was affected with the named
         * effect. The piece's sprite should be updated to reflect the piece's
         * new internal state after an appropriate visualization has been
         * displayed.
         */
        public void pieceAffected (Piece piece, String effect);

        /**
         * Indicates that the board was affected with the named visual effect.
         */
        public void boardAffected (String effect);
        
        /**
         * Indicates that the specified piece was moved or reoriented.
         */
        public void pieceMoved (Piece piece);

        /**
         * Indicates that the piece in question was killed (killed pieces are
         * not automatically removed; if a piece was removed that will be
         * reported separately with a call to {@link #pieceRemoved}).
         */
        public void pieceKilled (Piece piece);

        /**
         * Indicates that the specified piece was removed from the board.
         */
        public void pieceRemoved (Piece piece);

        /**
         * Indicates that the tick was delayed for the specified amount of time
         * in order to let an effect run its course.
         */
        public void tickDelayed (long extraTime);
    }

    /**
     * Handles a collision that moves and damages a unit.
     *
     * @param collider the index of the user causing the collision, or -1.
     * @param damage the amount of damage caused by the collision.
     *
     * @return true if all went well, false if we failed to collide or do
     * damage.
     */
    public static boolean collide (
        BangObject bangobj, Observer obs, int collider, int targetId,
        int damage, int x, int y, String effect)
    {
        Piece target = (Piece)bangobj.pieces.get(targetId);
        if (target == null) {
            log.warning("Missing collision target " +
                        "[targetId=" + targetId + "].");
            return false;
        }

        return collide(bangobj, obs, collider, target, 
                Math.min(100, target.damage + damage), x, y, effect);

    }

    /**
     * Handles a collision that moves and damages a unit.
     *
     * @param collider the index of the user causing the collision, or -1.
     * @param newDamage the new total damage to assign to the damaged piece.
     *
     * @return true if all went well, false if we failed to collide or do
     * damage.
     */
    public static boolean collide (
        BangObject bangobj, Observer obs, int collider, Piece target,
        int newDamage, int x, int y, String effect)
    {

        // move the target to its new coordinates
        if (target instanceof Unit && (target.x != x || target.y != y)) {
            moveAndReport(bangobj, target, x, y, obs);
        }

        // damage the target if it's still alive
        if (target.isAlive()) {
            return damage(bangobj, obs, collider, target, newDamage, effect);
        }

        return true;
    }

    /**
     * Damages the supplied piece by the specified amount, properly removing it
     * from the board if appropriate and reporting the specified effect.
     *
     * @param shooter the index of the player doing the damage or -1 if the
     * damage was not originated by a player.
     * @param newDamage the new total damage to assign to the damaged piece.
     *
     * @return true if the damage was applied, false if the target was already
     * dead and we were unable to apply the damage.
     */
    public static boolean damage (BangObject bangobj, Observer obs, int shooter,
                                  Piece target, int newDamage, String effect)
    {
        // sanity check
        if (!target.isAlive()) {
            log.warning("Not damaging already dead target " +
                        "[target=" + target + ", shooter=" + shooter +
                        ", nd=" + newDamage + ", effect=" + effect + "].");
            return false;
        }

        // effect the actual damage
//         log.info("Damaging " + target + " -> " + newDamage + ".");
        target.wasDamaged(newDamage);
        if (!target.isAlive()) {
            target.wasKilled(bangobj.tick);
        }
        
        // report that the target was affected
        reportEffect(obs, target, effect);

        // if the target is not dead, we can stop here
        if (target.isAlive()) {
            return true;
        }

        // report that the target was killed
        reportKill(obs, target);

        // airborn targets must land when they die
        if (target.isAirborne() && !target.removeWhenDead()) {
            Point pt = bangobj.board.getOccupiableSpot(
                target.x, target.y, 5, new Random(bangobj.tick));
            if (pt != null) {
                moveAndReport(bangobj, target, pt.x, pt.y, obs);
            }
        }

        // if we have a shooter and we're on the server, record the kill
        if (shooter != -1 && bangobj.getManager().isManager(bangobj) &&
                target instanceof Unit) {
            // record the kill statistics
            bangobj.stats[shooter].incrementStat(Stat.Type.UNITS_KILLED, 1);
            if (target.owner != -1) {
                bangobj.stats[target.owner].incrementStat(
                    Stat.Type.UNITS_LOST, 1);
            }
        }

        // if the should be removed when killed, do so now
        if (target.removeWhenDead()) {
            bangobj.removePieceDirect(target);
            reportRemoval(obs, target);

        // otherwise reshadow the piece to block all ground travel across
        } else {
            int owner = target.owner;
            target.owner = -1;
            bangobj.board.shadowPiece(target);
            target.owner = owner;
        }

        return true;
    }

    /**
     * Initializes this effect (called only on the server) with the piece that
     * activated the bonus.
     */
    public void init (Piece piece)
    {
    }

    /**
     * Returns an array of pieces we must wait for before activating this
     * effect. The default implementation returns nothing, however, some
     * effects may require that we wait for a piece to finish animating before
     * starting but not necessarily block that piece from moving on to the next
     * animation once this effect's visualization is started.
     *
     * <p>Note: it is not necessary to return any pieces that are already
     * returned by {@link #getAffectedPieces} as those will automatically be
     * waited for.
     */
    public int[] getWaitPieces ()
    {
        return NO_PIECES;
    }

    /**
     * Returns the bounds we must have exclusive access to before activating
     * this effect.  Can return null for no area exclusion.
     */
    public Rectangle getBounds ()
    {
        return null;
    }

    /** Returns an array of the ids of all pieces affected by this effect. */
    public abstract int[] getAffectedPieces ();

    /**
     * Prepares this effect for application. This is executed on the server
     * before the effect is applied on the server and then distributed to the
     * client for application there. The effect should determine which pieces
     * it will impact as well as decide where it will be placing new pieces
     * (and update the board shadow to reflect those piece additions, though it
     * should not actually add the pieces until it is applied).
     *
     * @param dammap a mapping that should be used to record damage done
     * to a particular player's units (player index -> accumulated
     * damage).
     */
    public abstract void prepare (BangObject bangobj, IntIntMap dammap);

    /**
     * A method only used by effects created by cards that returns true if this
     * effect will serve its intended purpose or false if the card that caused
     * the effect should be considered unplayed and the effect cancelled. The
     * default implementation always assumes applicability.
     */
    public boolean isApplicable ()
    {
        return true;
    }
    
    /**
     * Applies this effect to the board and pieces. Any modifications to pieces
     * or the board should be made directly as this is executed on both the
     * client and server. <em>Note:</em> effects should always compute and
     * store the final result of their effects in {@link #prepare} and then
     * simply apply those results in {@link #apply} rather than do any
     * computation in {@link #apply} as we cannot rely on the values in the
     * piece during the apply to be the same as they would be on the server
     * when the effect is applied. The only truly safe time to inspect the
     * condition of the affected pieces is during {@link #prepare}.
     *
     * @param observer an observer to inform of piece additions, updates and
     * removals (for display purposes on the client). This may be null.
     *
     * @return false if for some reason the effect could not be applied (this
     * is only used for in the field debugging).
     */
    public abstract boolean apply (BangObject bangobj, Observer observer);
    
    /**
     * Creates an {@link EffectHandler} to manage the (potentially complicated)
     * visualization of this effect.
     */
    public EffectHandler createHandler (BangObject bangobj)
    {
        return new EffectHandler();
    }
    
    /**
     * Returns the base amount of damage inflicted on the specified piece
     * (that is, the amount before it's limited by the piece's remaining
     * health).
     */
    public int getBaseDamage (Piece piece)
    {
        return 0;
    }
    
    /**
     * Returns a translatable description of the effect to display on the
     * client, or <code>null</code> for none.
     *
     * @param pidx the client's player index
     */
    public String getDescription (BangObject bangobj, int pidx)
    {
        return null;
    }
    
    @Override // documentation inherited
    public String toString ()
    {
        String cname = getClass().getName();
        return cname.substring(cname.lastIndexOf(".")+1) + ":" +
            super.toString();
    }

    /** A helper function for adding a piece and reporting it. */
    protected static void addAndReport (
        BangObject bangobj, Piece piece, Observer obs)
    {
        bangobj.addPieceDirect(piece);
        piece.wasAdded(bangobj);
        if (obs != null) {
            obs.pieceAdded(piece);
        }
    }
    
    /** A helper function for reporting a piece affecting. */
    protected static void reportEffect (
        Observer obs, Piece piece, String effect)
    {
        if (obs != null) {
            obs.pieceAffected(piece, effect);
        }
    }

    /** A helper function for affecting the board and reporting it. */
    protected static void affectBoard (
        BangObject bangobj, String effect, boolean persist, Observer obs)
    {
        if (persist) {
            bangobj.boardEffect = effect;
        }
        if (obs != null) {
            obs.boardAffected(effect);
        }
    }
    
    /** A helper function for moving a piece and reporting it. */
    protected static void moveAndReport (
        BangObject bangobj, Piece piece, int nx, int ny, Observer obs)
    {
        bangobj.board.clearShadow(piece);
        piece.position(nx, ny);
        bangobj.board.shadowPiece(piece);
        if (obs != null) {
            obs.pieceMoved(piece);
        }
    }

    /** A helper function for reporting piece death. */
    protected static void reportKill (Observer obs, Piece piece)
    {
        if (obs != null) {
            obs.pieceKilled(piece);
        }
    }

    /** A helper function for reporting a piece addition. */
    protected static void reportRemoval (Observer obs, Piece piece)
    {
        if (obs != null) {
            obs.pieceRemoved(piece);
        }
    }

    /** A helper function for reporting a tick delay. */
    protected static void reportDelay (Observer obs, long extraTime)
    {
        if (obs != null) {
            obs.tickDelayed(extraTime);
        }
    }

    /** Concatenates two integer arrays and returns the result. */
    protected static int[] concatenate (int[] a1, int[] a2)
    {
        int[] result = new int[a1.length + a2.length];
        System.arraycopy(a1, 0, result, 0, a1.length);
        System.arraycopy(a2, 0, result, a1.length, a2.length);
        return result;
    }
    
    /** Returns a translatable string representing the names of the pieces
     * identified in the piece id array owned by the specified player. */
    protected String getPieceNames (
        BangObject bangobj, int pidx, int[] pieceIds)
    {
        ArrayList<String> names = new ArrayList<String>();
        for (int pieceId : pieceIds) {
            Piece piece = bangobj.pieces.get(pieceId);
            if (piece != null && piece.owner == pidx) {
                names.add(piece.getName());
            }
        }
        int nsize = names.size();
        if (nsize == 0) {
            return null;
        } else if (nsize == 1) {
            return names.get(0);
        } else {
            return MessageBundle.compose("m.times_" + nsize,
                names.toArray(new String[nsize]));
        }
    }
    
    /** Used by {@link #getWaitPieces}. */
    protected static final int[] NO_PIECES = new int[0];
}
