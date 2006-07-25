//
// $Id$

package com.threerings.bang.game.server.scenario;

import java.util.ArrayList;
import java.util.Arrays;

import com.samskivert.util.RandomUtil;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.UnitConfig;

import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.BangObject;

import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

import com.threerings.bang.game.util.PieceSet;

import com.threerings.presents.server.InvocationException;

/**
 * Handles the server side operation of practive scenarios.
 */
public class Practice extends Scenario
{
    @Override // documentation inherited
    public void startNextPhase (BangObject bangobj)
    {
        switch (bangobj.state) {
          case BangObject.POST_ROUND:
          case BangObject.PRE_GAME:
          case BangObject.SELECT_PHASE:
          case BangObject.BUYING_PHASE:
            _bangmgr.startPhase(BangObject.PRE_PRACTICE);
            break;

          case BangObject.PRE_PRACTICE:
            _bangmgr.startPhase(BangObject.IN_PLAY);
            break;

          default:
            super.startNextPhase(bangobj);
            break;
        }
    }

    @Override // documentation inherited
    public void roundWillStart (BangObject bangobj, ArrayList<Piece> starts,
                                PieceSet purchases)
        throws InvocationException
    {
        super.roundWillStart(bangobj, starts, purchases);

        // The user gets 3 units of the specified type
        BangConfig bconfig = (BangConfig)_bangmgr.getConfig();
        Unit[] units = new Unit[NUM_UNITS];
        for (int ii = 0; ii < units.length; ii++) {
            units[ii] = Unit.getUnit(bconfig.scenarios[0]);
        }
        
        for (int ii = 0; ii < bangobj.players.length; ii++) {
            if (!_bangmgr.isAI(ii)) {
                _bangmgr.initAndPrepareUnits(units, ii);
            }
        }

        // The AI gets 3 random units
        UnitConfig[] ucs = UnitConfig.getTownUnits(bangobj.townId);
        int[] weights = new int[ucs.length];
        Arrays.fill(weights, 1);
        units = new Unit[NUM_UNITS];
        for (int ii = 0; ii < units.length; ii++) {
            int idx = RandomUtil.getWeightedIndex(weights);
            units[ii] = Unit.getUnit(ucs[idx].type);
            weights[idx] = 0;
        }
        for (int ii = 0; ii < bangobj.players.length; ii++) {
            if (_bangmgr.isAI(ii)) {
                _bangmgr.initAndPrepareUnits(units, ii);
            }
        }
    }

    @Override // documentation inherited
    public void tick (BangObject bangobj, short tick)
    {
        super.tick(bangobj, tick);

        // end the scenario if all the units on one side are dead
        for (int ii = 0; ii < bangobj.pdata.length; ii++) {
           if (_bangmgr.isActivePlayer(ii) && !bangobj.hasLiveUnits(ii)) {
              bangobj.setLastTick(tick);
           }
        } 
    }

    @Override // documentation inherited
    public boolean addBonus (BangObject bangobj, Piece[] pieces)
    {
        // no automatic bonuses in practice scenario
        return false;
    }

    @Override // documentation inherited
    public boolean shouldPayEarnings (PlayerObject user)
    {
        return false;
    }

    @Override // documentation inherited
    public short getBaseDuration ()
    {
        // practice scenarios normally expire after one side is dead,
        // but we do want to make sure they end eventually
        return 4000;
    }

    protected static final int NUM_UNITS = 3;
}
