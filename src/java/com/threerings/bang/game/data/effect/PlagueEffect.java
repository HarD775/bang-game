//
// $Id$

package com.threerings.bang.game.data.effect;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.ArrayUtil;
import com.samskivert.util.IntIntMap;
import com.samskivert.util.StringUtil;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

import static com.threerings.bang.Log.log;

/**
 * An effect that replaces all units in excess of the per-player average
 * with 40% health windup gunslingers owned by the original player.
 */
public class PlagueEffect extends BonusEffect
{
    /** The identifier for the type of effect that we produce. */
    public static final String PLAGUED = "bonuses/plague/activate";

    public int owner;
    public int[] pieceIds;
    public Piece[] newPieces;

    @Override // documentation inherited
    public void init (Piece piece)
    {
        owner = piece.owner;
    }

    @Override // documentation inherited
    public int[] getAffectedPieces ()
    {
        return pieceIds;
    }

    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        // everyone gets to keep the "average" count or at least two
        // pieces, whichever is higher
        int save = Math.max(2, bangobj.getAverageUnitCount());

        // subtract off the "reserved" count from each player
        int[] ucount = bangobj.getUnitCount();
        for (int ii = 0; ii < ucount.length; ii++) {
            ucount[ii] = Math.max(0, ucount[ii] - save);
        }

        log.info("Plaguing [avg=" + save +
                 ", ucount=" + StringUtil.toString(ucount) +
                 ", ocount=" + StringUtil.toString(bangobj.getUnitCount()) +
                 "].");

        // determine which pieces will be affected
        ArrayIntSet pids = new ArrayIntSet();
        Piece[] pieces = bangobj.getPieceArray();
        ArrayUtil.shuffle(pieces);

        for (int ii = 0; ii < pieces.length; ii++) {
            Piece p = pieces[ii];
            if (p.owner >= 0 && p.isAlive() && ucount[p.owner] > 0 &&
                // make sure we don't try to turn a dirigible over a
                // building or water into a windup gunman
                bangobj.board.isGroundOccupiable(p.x, p.y)) {
                ucount[p.owner]--;
                pids.add(p.pieceId);
            }
        }

        pieceIds = pids.toIntArray();
        newPieces = new Piece[pieceIds.length];
        for (int ii = 0; ii < newPieces.length; ii++) {
            newPieces[ii] = Unit.getUnit("windupslinger");
            newPieces[ii].init();
            newPieces[ii].assignPieceId(bangobj);
            newPieces[ii].owner =
                ((Piece)bangobj.pieces.get(pieceIds[ii])).owner;
            newPieces[ii].damage = 40;
        }
    }

    @Override // documentation inherited
    public void apply (BangObject bangobj, Observer obs)
    {
        super.apply(bangobj, obs);

        // remove the old pieces and add new windup gun slingers instead
        for (int ii = 0; ii < pieceIds.length; ii++) {
            Piece p = (Piece)bangobj.pieces.get(pieceIds[ii]);
            if (p == null) {
                continue;
            }
            bangobj.removePieceDirect(p);
            reportEffect(obs, p, PLAGUED);
            reportRemoval(obs, p);

            newPieces[ii].position(p.x, p.y);
            bangobj.addPieceDirect(newPieces[ii]);
            reportAddition(obs, newPieces[ii]);
        }

        // the balance of power has shifted, recompute our metrics
        bangobj.updateData();
    }
}
