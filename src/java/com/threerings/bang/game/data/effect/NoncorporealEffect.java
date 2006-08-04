//
// $Id$

package com.threerings.bang.game.data.effect;

import com.threerings.bang.data.TerrainConfig;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.piece.Influence;
import com.threerings.bang.game.data.piece.Unit;

/**
 * Causes a unit to become noncorporeal.
 */
public class NoncorporealEffect extends SetInfluenceEffect
{
    @Override // documentation inherited
    protected Influence createInfluence (Unit target)
    {
        return new Influence() {
            public String getName () {
                return "spirit_walk";
            }
            public int adjustTraversalCost (
                    TerrainConfig terrain, int traversalCost) {
                return BangBoard.BASE_TRAVERSAL;
            }
            public boolean adjustCorporeality (boolean corporeal) {
                return false;
            }
        };
    }

    @Override // documentation inherited
    protected String getEffectName ()
    {
        return "bonuses/indian_post/spirit_walk/activate";
    }
}
