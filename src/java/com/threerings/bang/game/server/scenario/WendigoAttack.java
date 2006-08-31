//
// $Id$

package com.threerings.bang.game.server.scenario;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import java.awt.Rectangle;

import com.samskivert.util.IntListUtil;
import com.samskivert.util.RandomUtil;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.Stat;

import com.threerings.bang.game.server.ai.AILogic;
import com.threerings.bang.game.server.ai.RandomLogic;
import com.threerings.bang.game.server.ai.WendigoLogic;

import com.threerings.bang.game.data.BangObject;

import com.threerings.bang.game.data.effect.CountEffect;
import com.threerings.bang.game.data.effect.FadeBoardEffect;
import com.threerings.bang.game.data.effect.TalismanEffect;
import com.threerings.bang.game.data.effect.WendigoEffect;

import com.threerings.bang.game.data.piece.Bonus;
import com.threerings.bang.game.data.piece.Counter;
import com.threerings.bang.game.data.piece.Marker;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.PieceCodes;
import com.threerings.bang.game.data.piece.Prop;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.data.piece.Wendigo;

import com.threerings.bang.game.data.scenario.WendigoAttackInfo;

import com.threerings.bang.game.util.PieceSet;
import com.threerings.bang.game.util.PointSet;

import com.threerings.parlor.game.data.GameAI;

import com.threerings.presents.server.InvocationException;

import static com.threerings.bang.Log.log;

/**
 * A gameply scenario wherein:
 * <ul>
 * <li>Fill this in when the scenario is finalized.
 * </ul>
 */
public class WendigoAttack extends Scenario
    implements PieceCodes
{
    /**
     * Creates a wendigo attack scenario and registers its delegates.
     */
    public WendigoAttack ()
    {
        registerDelegate(new WendigoDelegate());
        registerDelegate(new RespawnDelegate(RespawnDelegate.RESPAWN_TICKS/2) {
            @Override // documentation inherited
            public void pieceWasKilled (BangObject bangobj, Piece piece) {
                int oldRT = _respawnTicks;
                // if units were killed by a wendigo they respawn quicker
                if (_wendigoRespawnTicks != null && piece.owner != -1) {
                    _respawnTicks = Math.min(
                        _wendigoRespawnTicks[piece.owner]++, _respawnTicks);
                }
                super.pieceWasKilled(bangobj, piece);
                _respawnTicks = oldRT;
            }
        });
    }

    @Override // documentation inherited
    public AILogic createAILogic (GameAI ai)
    {
        return new WendigoLogic(this);
    }

    @Override // documentation inherited
    public void filterPieces (
        BangObject bangobj, ArrayList<Piece> starts, ArrayList<Piece> pieces,
        ArrayList<Piece> updates)
    {
        super.filterPieces(bangobj, starts, pieces, updates);

        // extract and remove all the safe spots
        _safePoints.clear();
        _talismanSpots.clear();
        for (Iterator<Piece> iter = pieces.iterator(); iter.hasNext(); ) {
            Piece p = iter.next();
            if (Marker.isMarker(p, Marker.SAFE)) { 
                _safePoints.add(p.x, p.y);
                // we don't remove the markers here since we want to assign it
                // a pieceId

            } else if (Marker.isMarker(p, Marker.TALISMAN)) {
                _talismanSpots.add(p.x, p.y);
                iter.remove();

            } else if (p instanceof Prop &&
                     SACRED_LOCATION.equals(((Prop)p).getType())) {
                _safePoints.add(p.x, p.y);
            }
        }
    }

    @Override // documentation inherited
    public void roundWillStart (BangObject bangobj, ArrayList<Piece> starts,
                                PieceSet purchases)
        throws InvocationException
    {
        super.roundWillStart(bangobj, starts, purchases);

        int[] weights = new int[_talismanSpots.size()];
        Arrays.fill(weights, 1);
        int placed = 1;
        for (int ii = 0; (ii < bangobj.players.length - 1) && 
                (ii < weights.length); ii++) {
            int idx = RandomUtil.getWeightedIndex(weights);
            Bonus talisman = dropBonus(bangobj, TalismanEffect.TALISMAN_BONUS,
                _talismanSpots.getX(idx), _talismanSpots.getY(idx));
            weights[idx] = 0;
        }
    }

    @Override // documentation inherited
    public void recordStats (
        BangObject bangobj, int gameTime, int pidx, PlayerObject user)
    {
        super.recordStats(bangobj, gameTime, pidx, user);

        // record the number of wendigo survivals
        int survivals = bangobj.stats[pidx].getIntStat(
                Stat.Type.WENDIGO_SURVIVALS);
        if (survivals > 0) {
            user.stats.incrementStat(Stat.Type.WENDIGO_SURVIVALS, survivals);
        }
    }

    /**
     * Returns the set of safe points on the board.
     */
    public PointSet getSafePoints ()
    {
        return _safePoints;
    }

    protected class WendigoDelegate extends CounterDelegate
    {
        public WendigoDelegate ()
        {
            _nextWendigo = (short)RandomUtil.getInt(
                    MAX_WENDIGO_TICKS, MIN_WENDIGO_TICKS);
        }

        @Override // documentation inherited
        public void tick (BangObject bangobj, short tick)
        {
            if (_wendigos != null && tick >= _nextWendigo) {
                for (Wendigo wendigo : _wendigos) {
                    _bangmgr.addPiece(wendigo);
                }
                WendigoEffect effect = WendigoEffect.wendigosAttack(
                        bangobj, _wendigos);
                effect.safePoints = _safePoints;
                _wendigoRespawnTicks = new int[bangobj.players.length];
                Arrays.fill(_wendigoRespawnTicks, 3);
                _bangmgr.deployEffect(-1, effect);
                _wendigoRespawnTicks = null;
                updatePoints(bangobj);
                _wendigos = null;
                _nextWendigo += (short)RandomUtil.getInt(
                        MAX_WENDIGO_TICKS, MIN_WENDIGO_TICKS);
                if (_nextWendigo < bangobj.duration) {
                    _nextWendigo -= WENDIGO_WAIT;
                }
            }
            if (tick >= _nextWendigo) {
                createWendigos(bangobj, tick);
                _nextWendigo += WENDIGO_WAIT;
            }
        }

        @Override // documentation inherited
        protected int pointsPerCounter ()
        {
            return WendigoAttackInfo.POINTS_PER_SURVIVAL;
        }

        @Override // documentation inherited
        protected void checkAdjustedCounter (BangObject bangobj, Unit unit) {
            // nothing to do here
        }

        /**
         * Create several wendigos that will spawn just outside the playfield.
         */
        protected void createWendigos (BangObject bangobj, short tick) 
        {
            _bangmgr.deployEffect(-1, new FadeBoardEffect());
            Rectangle playarea = bangobj.board.getPlayableArea();
            // First decide horizontal or vertical attack
            boolean horiz = (RandomUtil.getInt(2) == 0);
            int max = (horiz ? playarea.height : playarea.width) / 2;
            int num = Math.min(max, Math.max(1, max * ++_numAttacks/6));
            _wendigos = new ArrayList<Wendigo>(num);
            int off = 0;
            int length = 0;
            if (horiz) {
                off = playarea.y;
                length = playarea.height - 1;
            } else {
                off = playarea.x;
                length = playarea.width - 1;
            }

            // pick the set of tiles to attack based on the number of units
            // in the attack zone
            int[] weights = new int[length];
            Arrays.fill(weights, 1);
            Piece[] pieces = bangobj.getPieceArray();
            for (Piece p : pieces) {
                if (p instanceof Unit && p.isAlive()) {
                    int coord = (horiz ? p.y : p.x) - off;
                    if (coord < length) {
                        weights[coord]++;
                    }
                    if (coord - 1 >= 0) {
                        weights[coord - 1]++;
                    }
                }
            }

            // generate the wendigos spread out along the edge
            for (int ii = 0; ii < num; ii++) {
                int idx = (IntListUtil.sum(weights) == 0 ?
                    RandomUtil.getInt(length) :
                    RandomUtil.getWeightedIndex(weights));
                int size = RandomUtil.getInt(2);
                weights[idx] = 0;
                boolean side = RandomUtil.getInt(2) == 0;
                createWendigo(bangobj, idx + off, horiz, side, 
                        playarea, false, tick);
                if (idx + 1 < weights.length) {
                    if (size > 0 && idx + 2 < weights.length && 
                            weights[idx + 2] > 0) {
                        createWendigo(bangobj, idx + 2 + off, horiz, side, 
                                playarea, true, tick);
                        num--;
                        weights[idx + 2] = 0;
                        if (idx + 3 < weights.length) {
                            weights[idx + 3] = 0;
                        }
                    }
                    weights[idx + 1] = 0;
                }
                if (idx - 1 >= 0) {
                    if (size > 0 && idx - 2 >= 0 && weights[idx - 2] > 0) {
                        createWendigo(bangobj, idx - 2 + off, horiz, side, 
                                playarea, true, tick);
                        num--;
                        weights[idx - 2] = 0;
                        if (idx - 3 >= 0) {
                            weights[idx - 3] = 0;
                        }
                    }
                    weights[idx - 1] = 0;
                }
                int sum = 0;
                for (int weight : weights) {
                    sum += weight;
                }
                if (sum == 0) {
                    break;
                }
            }
        }

        protected void createWendigo (
                BangObject bangobj, int idx, boolean horiz, boolean side, 
                Rectangle playarea, boolean claw, short tick)
        {
            Wendigo wendigo = new Wendigo(claw);
            wendigo.assignPieceId(bangobj);
            int orient = NORTH;
            if (horiz) {
                orient = (side) ? EAST : WEST;
                wendigo.position(playarea.x + 
                        (orient == EAST ? -4 : playarea.width + 2),
                    idx);
            } else {
                orient = (side) ? NORTH : SOUTH;
                wendigo.position(idx, playarea.y + 
                        (orient == SOUTH ? -4 : playarea.height + 2));
            }
            wendigo.orientation = (short)orient;
            wendigo.lastActed = tick;
            _wendigos.add(wendigo);
        }

        /**
         * Grant points for surviving units after a wendigo attack.
         */
        protected void updatePoints (BangObject bangobj)
        {
            int[] points = new int[bangobj.players.length];
            int[] talpoints = new int[bangobj.players.length];
            Piece[] pieces = bangobj.getPieceArray();
            for (Piece p : pieces) {
                if (p instanceof Unit && p.isAlive() && p.owner > -1) {
                    points[p.owner]++;
                    if (TalismanEffect.TALISMAN_BONUS.equals(
                                ((Unit)p).holding) &&
                            _safePoints.contains(p.x, p.y)) {
                        talpoints[p.owner] += TALISMAN_SAFE;
                    }
                }
            }

            bangobj.startTransaction();
            try {
                for (int idx = 0; idx < points.length; idx++) {
                    if (points[idx] > 0) {
                        bangobj.grantPoints(idx, points[idx] *
                                WendigoAttackInfo.POINTS_PER_SURVIVAL +
                                talpoints[idx]);
                        bangobj.stats[idx].incrementStat(
                                Stat.Type.WENDIGO_SURVIVALS, points[idx]);
                        bangobj.stats[idx].incrementStat(
                                Stat.Type.TALISMAN_POINTS, talpoints[idx]);
                    }
                }
            } finally {
                bangobj.commitTransaction();
            }

            if (_counters.size() == 0) {
                return;
            }

            for (Counter counter : _counters) {
                if (points[counter.owner] > 0) {
                    _bangmgr.deployEffect(
                            -1, CountEffect.changeCount(counter.pieceId,
                                counter.count + points[counter.owner]));
                }
            }
        }

        /** Our wendigo. */
        protected ArrayList<Wendigo> _wendigos;

        /** The tick when the next wendigo will spawn. */
        protected short _nextWendigo;

        /** The tick when the wendigos will attack. */
        protected short _attackTick;

        /** Current wendigo attack number. */
        protected int _numAttacks;

        /** Number of ticks before wendigo appears. */
        protected static final short MIN_WENDIGO_TICKS = 9;
        protected static final short MAX_WENDIGO_TICKS = 15;

        /** Number of ticks after wendigos appear before they attack. */
        protected static final short WENDIGO_WAIT = 4;

        /** Number of points for having a talisman on a safe zone. */
        protected static final int TALISMAN_SAFE = 50;
    }

    /** Set of the sacred location markers. */
    protected PointSet _safePoints = new PointSet();

    /** Respawn ticks for units. */
    protected int[] _wendigoRespawnTicks;

    /** Used to track the locations of all talisman spots. */
    protected PointSet _talismanSpots = new PointSet();

    /** The sacred locations. */
    protected static final String SACRED_LOCATION =
        "indian_post/special/sacred_location";
}
