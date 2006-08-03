//
// $Id$

package com.threerings.bang.game.server.ai;

import java.awt.Point;

import java.util.List;

import com.samskivert.util.RandomUtil;

import com.threerings.media.util.AStarPathUtil;

import com.threerings.presents.server.InvocationException;

import com.threerings.bang.data.UnitConfig;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.server.BangManager;
import com.threerings.bang.game.util.PointSet;

/**
 * Handles the logic for a single AI player in a scenario.
 */
public abstract class AILogic
{
    /**
     * Initializes the AI logic before the start of a round.
     */
    public void init (BangManager bangmgr, int pidx)
    {
        _bangmgr = bangmgr;
        _bangobj = (BangObject)_bangmgr.getPlaceObject();
        _pidx = pidx;
    }
    
    /**
     * Returns the type of Big Shot desired by the AI.
     */
    public abstract String getBigShotType ();
    
    /**
     * Returns the types of cards desired by the AI (or <code>null</code> for
     * no cards, which is what the default implementation returns).
     */
    public String[] getCardTypes ()
    {
        return null;
    }
    
    /**
     * Returns the types of units that the AI wants for its team.
     *
     * @param count the number of units allowed
     */
    public abstract String[] getUnitTypes (int count);
    
    /**
     * Called on every tick to let the AI move its pieces.  Default
     * implementation calls {@link #moveUnit} for each unit owned by the
     * AI that is ready to move.
     *
     * @param pieces the array of pieces on the board
     * @param tick the current tick
     */
    public void tick (Piece[] pieces, short tick)
    {
        for (int ii = 0; ii < pieces.length; ii++) {
            if (pieces[ii] instanceof Unit && pieces[ii].owner == _pidx &&
                pieces[ii].isAlive() &&
                pieces[ii].ticksUntilMovable(tick) == 0) {
                Unit unit = (Unit)pieces[ii];
                _moves.clear();
                _attacks.clear();
                unit.computeMoves(_bangobj.board, _moves, _attacks);
                moveUnit(pieces, unit, _moves, _attacks);
            }
        }
    }
    
    /**
     * Moves an owned, ticked-up unit.
     *
     * @param pieces the array of pieces on the board
     * @param unit the unit to move
     * @param moves the places to which the unit can move
     * @param attacks the places the unit can attack
     */
    protected void moveUnit (
        Piece[] pieces, Unit unit, PointSet moves, PointSet attacks)
    {
    }
    
    /**
     * Orders a unit to move.
     *
     * @param unit the unit to move
     * @param x the x coordinate to which to move or {@link Short#MAX_VALUE} if
     * the unit should be moved to the closest valid firing position to the
     * target.
     * @param y the y coordinate to which to move, this is ignored if {@link
     * Short#MAX_VALUE} is supplied for x.
     * @param target the piece for the unit to shoot, or <code>null</code> for
     * none
     * @return true if the order was executed, false if there was some error in
     * executing the order
     */
    protected boolean executeOrder (Unit unit, int x, int y, Piece target)
    {
        try {
            int targetId = (target == null) ? -1 : target.pieceId;
            _bangmgr.executeOrder(unit, x, y, targetId, true);
            return true;
        } catch (InvocationException e) {
            return false;
        }
    }
    
    /**
     * Returns a number of unique unit types by evaluating the provided array
     * of configurations and making weighted random selections.
     */
    protected String[] getWeightedUnitTypes (
        UnitConfig[] configs, UnitConfigEvaluator evaluator, int count)
    {
        // compute the weights
        int[] weights = new int[configs.length];
        for (int ii = 0; ii < configs.length; ii++) {
            weights[ii] = evaluator.getWeight(configs[ii]);
        }
        
        // use the weights to select the desired number of types
        String[] types = new String[count];
        for (int ii = 0; ii < count; ii++) {
            int idx = RandomUtil.getWeightedIndex(weights);
            types[ii] = configs[idx].type;
            weights[idx] = 0;
        }
        return types;
    }
    
    /**
     * Attempts to move the unit towards the provided destination and fire
     * off a shot at the best target.
     *
     * @return true if we successfully moved towards the destination,
     * false if we couldn't find a path
     */
    protected boolean moveUnit (
        Piece[] pieces, Unit unit, PointSet moves, int dx, int dy,
        TargetEvaluator evaluator)
    {
        Point dest = getClosestPoint(unit, moves, dx, dy);
        if (dest == null) {
            return false;
        }
        executeOrder(unit, dest.x, dest.y, getBestTarget(pieces, unit, dest.x,
            dest.y, evaluator));
        return true;
    }
    
    /**
     * Gets the closest point to the provided destination that the unit can
     * reach in one move (or <code>null</code> if the destination is
     * unreachable).
     */
    protected Point getClosestPoint (Unit unit, PointSet moves, int dx, int dy)
    {
        List<Point> path = AStarPathUtil.getPath(
                _bangobj.board, unit.getStepper(), unit, 
                getMaxLookahead(), unit.x, unit.y, dx, dy, true);
        if (path == null || path.size() < 2) {
            return null;
        }
        for (int ii = path.size() - 1; ii >= 0; ii--) {
            Point pt = path.get(ii);
            if (moves.contains(pt.x, pt.y)) {
                return pt;
            }
        }
        return null;
    }
    
    /**
     * Returns the maximum lookahead for destinations beyond what units can
     * reach in the current tick.
     */
    protected int getMaxLookahead ()
    {
        return _bangobj.board.getWidth() / 2;
    }
    
    /**
     * Finds and returns the best target that the unit can reach according to
     * the provided evaluator.
     */
    protected Piece getBestTarget (Piece[] pieces, Unit unit, PointSet attacks,
            PointSet preferredMoves, TargetEvaluator evaluator)
    {
        Piece best = null;
        int bweight = -1;
        for (int ii = 0; ii < pieces.length; ii++) {
            if (!unit.validTarget(pieces[ii], false) ||
                !attacks.contains(pieces[ii].x, pieces[ii].y)) {
                continue;
            }
            int tweight = evaluator.getWeight(_bangobj, unit, pieces[ii], 
                    pieces[ii].getDistance(unit.x, unit.y), preferredMoves);
            if (tweight > bweight) {
                best = pieces[ii];
                bweight = tweight;
            }
        }
        return best;
    }
    
    /**
     * Finds and returns the best target that the unit can reach after moving
     * to the given destination, according to the provided evaluator.
     */
    protected Piece getBestTarget (
        Piece[] pieces, Unit unit, int dx, int dy, TargetEvaluator evaluator)
    {
        Piece best = null;
        int bweight = -1;
        for (int ii = 0; ii < pieces.length; ii++) {
            if (!unit.validTarget(pieces[ii], false)) {
                continue;
            }
            int dist = pieces[ii].getDistance(dx, dy);
            if (dist < unit.getMinFireDistance() ||
                dist > unit.getMaxFireDistance()) {
                continue;
            }
            int tweight = evaluator.getWeight(_bangobj, unit, pieces[ii], dist,
                    EMPTY_POINT_SET);
            if (tweight > bweight) {
                best = pieces[ii];
                bweight = tweight;
            }
        }
        return best;
    }

    /**
     * Returns the best target that can be reached with the supplied
     * destination moves and evaluator.
     *
     * @param dest will be set to the location to move to for the target
     * if one is found
     */
    protected Piece getBestTargetInMoves (
            Piece[] pieces, Unit unit, PointSet attacks, PointSet moves,
            Point dest, TargetEvaluator evaluator)
    {
        Piece best = null;
        int bweight = -1;
        for (Piece p : pieces) {
            if (!unit.validTarget(p, false) || !attacks.contains(p.x, p.y)) {
               continue;
            }
            Point move = unit.computeShotLocation(
                        _bangobj.board, p, moves, true);
            if (move == null) {
                continue;
            }
            int tweight = evaluator.getWeight(_bangobj, unit, p,
                    p.getDistance(unit.x, unit.y), EMPTY_POINT_SET);
            if (tweight > bweight) {
                best = p;
                bweight = tweight;
                dest.setLocation(move);
            }
        }
        return best;
    }

    /**
     * Computes and returns the average location of all of our owned and
     * living pieces.
     */
    protected Point getControlCenter (Piece[] pieces)
    {
        Point center = new Point();
        int owned = 0;
        for (int ii = 0; ii < pieces.length; ii++) {
            if (pieces[ii].owner == _pidx && pieces[ii].isAlive()) {
                center.x += pieces[ii].x;
                center.y += pieces[ii].y;
                owned++;
            }
        }
        center.x /= owned;
        center.y /= owned;
        return center;
    }
    
    /** Used to evaluate unit configs for weighted random selections. */
    protected interface UnitConfigEvaluator
    {
        /** Returns the weight of the described unit. */
        public int getWeight (UnitConfig config);
    }
    
    /** Used to rank potential targets. */
    protected interface TargetEvaluator
    {
        /** Returns the weight of the specified target for the given unit. */
        public int getWeight (BangObject bangobj, Unit unit, Piece target, 
                int dist, PointSet preferredMoves);
    }
    
    /** The game manager. */
    protected BangManager _bangmgr;
    
    /** The game object. */
    protected BangObject _bangobj;
    
    /** The index of the AI player. */
    protected int _pidx;
    
    /** Used to compute a unit's potential moves or attacks. */
    protected PointSet _moves = new PointSet(), _attacks = new PointSet();

    protected static final PointSet EMPTY_POINT_SET = new PointSet();
}
