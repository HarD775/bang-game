//
// $Id$

package com.threerings.bang.game.server.scenario;

import java.util.ArrayList;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.IntListUtil;

import com.threerings.crowd.chat.server.SpeakProvider;
import com.threerings.presents.server.InvocationException;
import com.threerings.util.MessageBundle;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.util.PieceSet;
import com.threerings.bang.game.util.PointSet;

/**
 * A gameplay scenario where the last player standing is the winner.
 */
public class Shootout extends Scenario
{
    @Override // documentation inherited
    public void init (BangObject bangobj, ArrayList<Piece> markers,
                      PointSet bonusSpots, PieceSet purchases)
        throws InvocationException
    {
        super.init(bangobj, markers, bonusSpots, purchases);

        // create a fresh knockout array
        _knockoutOrder = new int[bangobj.players.length];
    }

    @Override // documentation inherited
    public boolean tick (BangObject bangobj, short tick)
    {
        super.tick(bangobj, tick);

        // check to see whether anyone's pieces are still alive
        _havers.clear();
        Piece[] pieces = bangobj.getPieceArray();
        for (int ii = 0; ii < pieces.length; ii++) {
            if ((pieces[ii] instanceof Unit) &&
                pieces[ii].isAlive()) {
                _havers.add(pieces[ii].owner);
            }
        }

        // score cash for anyone who is knocked out as of this tick
        int score = IntListUtil.getMaxValue(_knockoutOrder) + 1;
        for (int ii = 0; ii < _knockoutOrder.length; ii++) {
            if (_knockoutOrder[ii] == 0 && !_havers.contains(ii)) {
                _knockoutOrder[ii] = score;
                bangobj.setFundsAt(bangobj.funds[ii] + SCORE_CASH * score, ii);
                String msg = MessageBundle.tcompose(
                    "m.knocked_out", bangobj.players[ii]);
                SpeakProvider.sendInfo(bangobj, GameCodes.GAME_MSGS, msg);
            }
        }

        // the game ends when one or zero players are left standing
        if (_havers.size() < 2) {
            // score cash for the last player standing
            int winidx = _havers.get(0);
            bangobj.setFundsAt(bangobj.funds[winidx] +
                               SCORE_CASH * (score + 1), winidx);

            return true;
        }

        return false;
    }

    /** Used to calculate winners. */
    protected ArrayIntSet _havers = new ArrayIntSet();

    /** Used to track the order in which players are knocked out. */
    protected int[] _knockoutOrder;

    protected static final int SCORE_CASH = 50;
}
