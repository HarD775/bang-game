//
// $Id$

package com.threerings.bang.game.server.ai;

import java.awt.Point;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.IntListUtil;
import com.samskivert.util.RandomUtil;

import com.threerings.bang.data.UnitConfig;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Cow;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.PieceCodes;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.util.PointSet;

import static com.threerings.bang.Log.*;

/**
 * A simple AI for the cattle rustling scenario.
 */
public class CattleRustlingLogic extends AILogic
    implements PieceCodes
{
    // documentation inherited
    public String getBigShotType ()
    {
        // prefer a big shot with greater move distance
        UnitConfig[] configs = UnitConfig.getTownUnits(_bangobj.townId,
            UnitConfig.Rank.BIGSHOT);
        return getWeightedUnitTypes(configs, MOVE_DISTANCE_EVALUATOR, 1)[0];
    }

    // documentation inherited
    public String[] getUnitTypes (int count)
    {
        UnitConfig[] configs = UnitConfig.getTownUnits(_bangobj.townId,
            UnitConfig.Rank.NORMAL);
        return getWeightedUnitTypes(configs, MOVE_DISTANCE_EVALUATOR, count);
    }

    // documentation inherited
    protected void moveUnit (
        Piece[] pieces, Unit unit, PointSet moves, PointSet attacks)
    {
        // use special logic for the big shot
        if (unit.getConfig().rank == UnitConfig.Rank.BIGSHOT) {
            moveBigShot(pieces, unit, moves, attacks);
            return;
        }
        // find out if we can spook any cows towards the herd (including ones
        // we own; we don't want them straying too far)
        Point herd = getControlCenter(pieces);
        Unit bshot = null;
        Cow ccow = null;
        Piece ctarget = null;
        _sresults.clear();
        for (int ii = 0; ii < pieces.length; ii++) {
            if (pieces[ii] instanceof Unit && pieces[ii].owner == _pidx &&
                pieces[ii].isAlive() && ((Unit)pieces[ii]).getConfig().rank ==
                    UnitConfig.Rank.BIGSHOT) {
                bshot = (Unit)pieces[ii];
                continue;
            
            } else if (unit.validTarget(_bangobj, pieces[ii], false) &&
                (ctarget == null || unit.getDistance(pieces[ii]) <
                    unit.getDistance(ctarget)) &&
                unit.validTarget(_bangobj, pieces[ii], false)) {
                ctarget = pieces[ii];
                continue;
                
            } else if (!(pieces[ii] instanceof Cow)) {
                continue;
            }
            Cow cow = (Cow)pieces[ii];
            if (cow.owner != _pidx && (ccow == null ||
                    unit.getDistance(cow) < unit.getDistance(ccow))) {
                ccow = cow;
            }
            updateSpookResults((Cow)pieces[ii], moves, herd);
        }
        if (_sresults.isEmpty()) {
            moveUnit(pieces, unit, moves, bshot, herd, ccow, ctarget);
            return;
        }
        
        // find the spot that scares the most cows towards the herd
        Map.Entry<Point, SpookResult> best = null;
        for (Map.Entry<Point, SpookResult> entry : _sresults.entrySet()) {
            if (best == null || entry.getValue().getTowardsIndex() >
                    best.getValue().getTowardsIndex()) {
                best = entry;
            }
        }
        if (best.getValue().getTowardsIndex() <= 0) {
            moveUnit(pieces, unit, moves, bshot, herd, ccow, ctarget);
            return;
        }
        Point dest = best.getKey();
        executeOrder(unit, dest.x, dest.y,
            getBestTarget(pieces, unit, dest.x, dest.y, TARGET_EVALUATOR));
    }
    
    /**
     * After it has been determined that the unit can't usefully spook any
     * cows, this method attempts to move the unit towards the Big Shot
     * (if present), then towards the herd (if not solely comprised of the
     * unit), then towards the closest unowned cow (if any), and finally
     * towards the closest valid target (if any).
     */
    protected void moveUnit (
        Piece[] pieces, Unit unit, PointSet moves, Unit bshot, Point herd,
        Cow ccow, Piece ctarget)
    {
        if (bshot != null && moveUnit(pieces, unit, moves, bshot.x, bshot.y)) {
            return;
        } else if (herd != null && (herd.x != unit.x || herd.y != unit.x) &&
            moveUnit(pieces, unit, moves, herd.x, herd.y)) {
            return;
        } else if (ccow != null &&
            moveUnit(pieces, unit, moves, ccow.x, ccow.y)) {
            return;
        } else if (ctarget != null) {
            moveUnit(pieces, unit, moves, ctarget.x, ctarget.y);
        }
    }
    
    /**
     * Attempts to move the unit towards the provided destination and fire
     * off a shot at the best target.
     *
     * @return true if we successfully moved towards the destination,
     * false if we couldn't find a path
     */
    protected boolean moveUnit (
        Piece[] pieces, Unit unit, PointSet moves, int dx, int dy)
    {
        return moveUnit(pieces, unit, moves, dx, dy, TARGET_EVALUATOR);
    }
    
    /**
     * Moves the big shot.
     */
    protected void moveBigShot (
        Piece[] pieces, Unit unit, PointSet moves, PointSet attacks)
    {
        // among the cows that we haven't already branded, find the closest
        // and any that we can reach right now
        Point herd = getControlCenter(pieces);
        Cow ccow = null;
        Piece ctarget = null;
        _sresults.clear();
        for (int ii = 0; ii < pieces.length; ii++) {
            if (unit.validTarget(_bangobj, pieces[ii], false) &&
                (ctarget == null || unit.getDistance(pieces[ii]) <
                    unit.getDistance(ctarget)) &&
                unit.validTarget(_bangobj, pieces[ii], false)) {
                ctarget = pieces[ii];
                continue;
                
            } else if (!(pieces[ii] instanceof Cow)) {
                continue;
            }
            Cow cow = (Cow)pieces[ii];
            if (cow.owner == _pidx) {
                continue;
            }
            if (ccow == null || unit.getDistance(cow) <
                    unit.getDistance(ccow)) {
                ccow = cow;
            }
            updateSpookResults(cow, moves, herd);
        }
        
        // if we can't brand any right now, move towards the closest (and shoot
        // anyone in range)
        if (_sresults.isEmpty()) {
            moveUnit(pieces, unit, moves, null, null, ccow, ctarget);
            return;
        }
        
        // if we can brand some cattle now, find the best location
        Map.Entry<Point, SpookResult> best = null;
        for (Map.Entry<Point, SpookResult> entry : _sresults.entrySet()) {
            if (best == null ||
                entry.getValue().compareTo(best.getValue()) > 0) {
                best = entry;
            }
        }
        Point dest = best.getKey();
        executeOrder(unit, dest.x, dest.y,
            getBestTarget(pieces, unit, dest.x, dest.y, TARGET_EVALUATOR));
    }
    
    /**
     * Updates the spook results for the specified cow.
     *
     * @param moves the moves the unit can make (used to determine the
     * directions in which the unit can spook the cow)
     * @param herd the location of the herd towards which we would like to
     * spook the cow
     */
    protected void updateSpookResults (Cow cow, PointSet moves, Point herd)
    {
        for (int ii = 0; ii < 4; ii++) {
            _point.setLocation(cow.x + DX[ii], cow.y + DY[ii]);
            if (!moves.contains(_point.x, _point.y)) {
                continue;
            }
            SpookResult result = _sresults.get(_point);
            if (result == null) {
                _sresults.put((Point)_point.clone(),
                    result = new SpookResult());
            }
            result.addCow(cow, ii, herd);
        }
    }
    
    /** Contains the effect of moving to a certain location in terms of
     * spooking cows. */
    protected class SpookResult
        implements Comparable
    {
        /** The number of cows spooked towards and away from the herd. */
        public int towards, away;
        
        /**
         * Adds a spooked cow to the result.
         *
         * @param cow the cow being spooked
         * @param dir the direction *from* which the cow was spooked
         * @param herd the location of the herd
         */
        public void addCow (Cow cow, int dir, Point herd)
        {
            // spooking a cow from a direction is like moving the herd in that
            // direction (moving the cow in the opposite direction)
            int nx = herd.x + DX[dir], ny = herd.y + DY[dir];
            if (cow.getDistance(nx, ny) < cow.getDistance(herd.x, herd.y)) {
                towards++;
            } else {
                away++;
            }
        }
        
        /**
         * Returns the total number of cows spooked.
         */
        public int getTotalCows ()
        {
            return towards + away;
        }
        
        /**
         * Returns the "towards index," which balances cows spooked towards the
         * herd against cows spooked away.
         */
        public int getTowardsIndex ()
        {
            return towards - away;
        }
        
        // documentation inherited from interface Comparable
        public int compareTo (Object other)
        {
            // towards is better than away, but more is better than fewer
            SpookResult oresult = (SpookResult)other;
            int tdiff = getTotalCows() - oresult.getTotalCows();
            if (tdiff != 0) {
                return tdiff;
            }
            return towards - oresult.towards;
        }
    }
    
    /** Used to store spook results by location. */
    protected HashMap<Point, SpookResult> _sresults =
        new HashMap<Point, SpookResult>();
    
    /** A temporary point object. */
    protected Point _point = new Point();
    
    /** Ranks units by move distance. */
    protected static final UnitConfigEvaluator MOVE_DISTANCE_EVALUATOR =
        new UnitConfigEvaluator() {
        public int getWeight (UnitConfig config) {
            return config.moveDistance * config.moveDistance;
        }
    };
    
    /** Ranks potential targets by rank, the amount of damage the unit will do,
     * and the amount of damage the target has already taken. */
    protected static final TargetEvaluator TARGET_EVALUATOR =
        new TargetEvaluator() {
        public int getWeight (BangObject bangobj, Unit unit, Piece target, 
                int dist, PointSet preferredMoves) {
            UnitConfig.Rank rank = (target instanceof Unit ? 
                    ((Unit)target).getConfig().rank : null);
            return (rank == UnitConfig.Rank.BIGSHOT ? 1000 : 0) +
                unit.computeScaledDamage(bangobj, target, 1f) * 
                100 + target.damage;
        }
    };
}
