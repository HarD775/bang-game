//
// $Id$

package com.threerings.bang.game.server.ai;

import java.awt.Point;

import com.threerings.bang.data.UnitConfig;

import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.PieceCodes;
import com.threerings.bang.game.data.piece.TotemBase;
import com.threerings.bang.game.data.piece.TotemBonus;
import com.threerings.bang.game.data.piece.Unit;

import com.threerings.bang.game.util.PointSet;

/**
 * A simple AI for the totem building scenario.
 */
public class TotemLogic extends AILogic
    implements PieceCodes
{
    // documentation inherited
    public String getBigShotType ()
    {
        // prefer a big shot with greater move distance
        UnitConfig[] configs = UnitConfig.getTownUnits(_bangobj.townId,
            UnitConfig.Rank.BIGSHOT);
        return getWeightedUnitTypes(configs, OFFENSE_EVALUATOR, 1)[0];
    }

    // documentation inherited
    public String[] getUnitTypes (int count)
    {
        UnitConfig[] configs = UnitConfig.getTownUnits(_bangobj.townId,
            UnitConfig.Rank.NORMAL);
        return getWeightedUnitTypes(configs, OFFENSE_EVALUATOR, count);
    }

    // documentation inherited
    protected void moveUnit (
        Piece[] pieces, Unit unit, PointSet moves, PointSet attacks)
    {
        TotemBase obase = null, cbase = null, ebase = null, lbase = null;
        Unit ctarget = null;
        Piece ctotem = null;
        boolean breached = false;
        for (int ii = 0; ii < pieces.length; ii++) {
            if (pieces[ii] instanceof TotemBase) {
                TotemBase base = (TotemBase)pieces[ii];
                if (base.getTopOwner() == _pidx || base.getTopOwner() == -1 && 
                    (obase == null ||
                     unit.getDistance(base) < unit.getDistance(obase))) {
                    obase = base;
                } else if (base.getTotemHeight() > 0 && (cbase == null ||
                    unit.getDistance(base) < unit.getDistance(cbase))) {
                    lbase = cbase;
                    cbase = base;
                }
            } else if (pieces[ii] instanceof TotemBonus) {
                if (ctotem == null || unit.getDistance(pieces[ii]) <
                        unit.getDistance(ctotem)) {
                    ctotem = pieces[ii];
                }
            } else if (pieces[ii] instanceof Unit &&
                    pieces[ii].owner != _pidx) {
                Unit target = (Unit)pieces[ii];
                if (TotemBonus.isHolding(target) &&
                    (ctarget == null ||
                     unit.getDistance(target) < unit.getDistance(ctarget)) &&
                    unit.validTarget(target, false)) {
                    ctarget = target;
                }
            }
        }
        if (obase == null) {
            obase = cbase;
            cbase = lbase;
        }
        if (_baseloc == null) {
            _baseloc = new Point(obase.x, obase.y);
        }
        // if we have a totem or our base is in danger, haul ass back home
        if ((TotemBonus.isHolding(unit) || 
             (breached && obase.getTotemHeight() > 0 &&
              unit.getDistance(obase) > DEFENSIVE_PERIMETER)) &&
            moveUnit(pieces, unit, moves, obase)) {
            return;

        // if there's a totem within reach, grab it
        } else if (ctotem != null && moves.contains(ctotem.x, ctotem.y)) {
            executeOrder(unit, ctotem.x, ctotem.y, getBestTarget(
                pieces, unit, ctotem.x, ctotem.y, TARGET_EVALUATOR));

        // if there's a totem holding target within reach, shoot it
        } else if (ctarget != null && attacks.contains(ctarget.x, ctarget.y)) {
            executeOrder(unit, Short.MAX_VALUE, 0, ctarget);

        // if there's a loaded base within reach, shoot it 
        } else if (cbase != null && attacks.contains(cbase.x, cbase.y)) {
            executeOrder(unit, Short.MAX_VALUE, 0, cbase);

        // otherwise, move towards nearest free totem
        } else if (ctotem != null && moveUnit(pieces, unit, moves, ctotem)) {
            return;

        // or nearest loaded base
        } else if (cbase != null && moveUnit(pieces, unit, moves, cbase)) {
            return;

        // or nearest totem holding target
        } else if (ctarget != null && moveUnit(pieces, unit, moves, ctarget)) {
            return;

        // or just try to find something to shoot
        } else {
            Piece target = getBestTarget(pieces, unit, attacks,
                TARGET_EVALUATOR);
            if (target != null) {
                executeOrder(unit, Short.MAX_VALUE, 0, target);
            }
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
        Piece[] pieces, Unit unit, PointSet moves, Piece target)
    {
        return moveUnit(pieces, unit, moves, target.x, target.y,
            TARGET_EVALUATOR);
    }

    /**
     * Determines whether the point set contains any points adjacent to the
     * given piece.
     */
    protected static boolean containsAdjacent (PointSet moves, Piece piece)
    {
        for (int ii = 0; ii < DIRECTIONS.length; ii++) {
            if (moves.contains(piece.x + DX[ii], piece.y + DY[ii])) {
                return true;
            }
        }
        return false;
    }

    /** The location of our own totem base. */
    protected Point _baseloc;

    /** Ranks units by properties that should make them good at gathering
     * totems: speed and attack power. */
    protected static final UnitConfigEvaluator OFFENSE_EVALUATOR =
        new UnitConfigEvaluator() {
        public int getWeight (UnitConfig config) {
            return config.moveDistance*10 + config.damage;
        }
    };

    /** Ranks potential targets by totem holdingness, the amount of damage the
     * unit will do, and the amount of damage the target has already taken. */
    protected static final TargetEvaluator TARGET_EVALUATOR =
        new TargetEvaluator() {
        public int getWeight (Unit unit, Piece target) {
            return ((target instanceof Unit) && 
                    TotemBonus.isHolding((Unit)target) ? 1000 : 0) +
                unit.computeScaledDamage(target, 1f) * 100 + target.damage;
        }
    };

    /** When enemy units get this close to our totem base, we start
     * sending units to defend it. */
    protected static final int DEFENSIVE_PERIMETER = 3;

}
