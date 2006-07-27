//
// $Id$

package com.threerings.bang.game.data.piece;

import java.util.ArrayList;

import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.client.sprite.TreeBedSprite;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.FetishEffect;
import com.threerings.bang.game.data.effect.TreeBedEffect;

/**
 * A tree bed that grows a tree in distinct phases, with the damage keeping
 * track of progress to the next phase.
 */
public class TreeBed extends Prop
{
    /** The final phase of the tree's growth. */
    public static final byte FULLY_GROWN = 3;

    /** The current growth phase of the tree, from 0 to FULLY_GROWN. */
    public byte growth;
    
    public TreeBed ()
    {
        damage = 100;
    }
    
    @Override // documentation inherited
    public void init ()
    {
        damage = 100;
        growth = 0;
    }
    
    @Override // documentation inherited
    public boolean isAlive ()
    {
        return true;
    }
    
    /**
     * "Damages" this tree bed by the specified amount, causing it to grow
     * higher if the increment is negative and lower if the increment is
     * positive.
     */
    public void damage (int dinc)
    {
        int nprogress = growth * 100 + (100 - damage) - dinc;
        if (nprogress < 0) {
            growth = 0;
            damage = 100;
        } else if (nprogress >= (FULLY_GROWN + 1) * 100) {
            growth = FULLY_GROWN;
            damage = 0;
        } else {
            growth = (byte)(nprogress / 100);
            damage = 100 - (nprogress % 100);
        }
    }
    
    @Override // documentation inherited
    public ArrayList<Effect> tick (short tick, BangBoard board, Piece[] pieces)
    {
        // normal units cause the tree to grow; logging robots cause it to
        // shrink
        int dinc = 0, ddec = 0;
        boolean doubleGrowth = false;
        ArrayList<Piece> growers = new ArrayList<Piece>();
        for (Piece piece : pieces) {
            if (piece instanceof Unit && getDistance(piece) == 1 &&
                piece.isAlive()) {
                Unit unit = (Unit)piece;
                if (FetishEffect.FROG_FETISH.equals(unit.holding)) {
                    doubleGrowth = true;
                }
                int pdamage = unit.getTreeProximityDamage();
                if (pdamage > 0) {
                    dinc += pdamage;
                } else {
                    ddec += pdamage;
                }
                growers.add(piece);
            }
        }
        int tdamage = dinc + ddec * (doubleGrowth ? 2 : 1);
        if (tdamage == 0 || (growth == 0 && damage == 100 && tdamage > 0) ||
            (growth == FULLY_GROWN && damage == 0 && tdamage < 0)) {
            return null;
        }
        ArrayList<Effect> effects = new ArrayList<Effect>();
        effects.add(new TreeBedEffect(this,
            growers.toArray(new Piece[growers.size()]), tdamage));
        return effects;
    }
    
    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new TreeBedSprite();
    }
}
