//
// $Id$

package com.threerings.bang.game.client.effect;

import com.jme.math.FastMath;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.scene.BillboardNode;
import com.jme.scene.Geometry;
import com.jme.scene.Node;
import com.jme.scene.SharedMesh;
import com.jme.scene.Spatial;
import com.jme.scene.shape.Quad;
import com.jme.scene.state.LightState;

import com.threerings.jme.sprite.PathUtil;

import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.data.card.Card;
import com.threerings.bang.game.data.effect.AreaDamageEffect;
import com.threerings.bang.game.data.effect.RepairEffect;
import com.threerings.bang.game.data.effect.ShotEffect;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.util.RenderUtil;

import static com.threerings.bang.Log.*;
import static com.threerings.bang.client.BangMetrics.*;
import java.util.Iterator;

/**
 * An effect visualization that floats an icon above the sprite, letting users
 * know what happened in terms of gaining or losing health, switching sides,
 * etc., with consistent symbology.
 */
public class IconViz extends EffectViz
{
    /**
     * Creates an icon visualization for the given effect identifier, or
     * returns <code>null</code> if no icon is necessary.
     */
    public static IconViz createIconViz (Piece piece, String effect)
    {
        if (effect.equals(RepairEffect.REPAIRED)) {
            return new IconViz("textures/effects/repaired.png");
            
        } else {
            return null;
        }
    }
    
    /**
     * Creates a visualization that drops a representation of the specified
     * card down onto the piece or coordinates.
     */
    public static IconViz createCardViz (Card card)
    {
        return new IconViz(card.getIconPath("icon"), true);
    }
    
    protected IconViz (String ipath)
    {
        _ipath = ipath;
    }
    
    protected IconViz (String ipath, ColorRGBA color)
    {
        _ipath = ipath;
        _color = color;
    }
    
    protected IconViz (String ipath, boolean card)
    {
        _ipath = ipath;
        _card = true;
    }
    
    @Override // documentation inherited
    protected void didInit ()
    {
        createBillboard();
        if (_ipath != null) {
            if (_card) {
                Quad bg = createIconQuad(
                    "textures/effects/cardback.png", ICON_SIZE, ICON_SIZE),
                     icon = createIconQuad(_ipath, CARD_WIDTH, ICON_SIZE);
                _billboard.attachChild(bg);
                _billboard.attachChild(icon);
                
            } else {
                Quad icon = createIconQuad(_ipath, ICON_SIZE, ICON_SIZE);
                if (_color != null) {
                    icon.setDefaultColor(new ColorRGBA(_color));
                } else if (_target != null) {
                    icon.setDefaultColor(new ColorRGBA(
                        JPIECE_COLORS[_target.owner + 1]));
                }
                _billboard.attachChild(icon);
            }
        }
    }
    
    @Override // documentation inherited
    public void display (PieceSprite target)
    {
        if (target != null) {
            target.attachChild(_billboard);
        } else {
            // wrap the billboard in a container node for ease of
            // transformation
            float tx = (_coords.x + 0.5f) * TILE_SIZE,
                ty = (_coords.y + 0.5f) * TILE_SIZE,
                tz = _view.getTerrainNode().getHeightfieldHeight(tx, ty);
            Node xform = new Node("icon") {
                public int detachChild (Spatial child) {
                    parent.detachChild(this);
                    return super.detachChild(child);
                }
            };
            xform.getLocalTranslation().set(tx, ty, tz);
            PathUtil.computeRotation(Vector3f.UNIT_Z, Vector3f.UNIT_Z,
                _view.getTerrainNode().getHeightfieldNormal(tx, ty),
                xform.getLocalRotation());
            xform.attachChild(_billboard);
            _view.getPieceNode().attachChild(xform);
        }
        _billboard.updateRenderState();
        _billboard.updateGeometricState(0f, true);
    }

    /**
     * Create an icon quad.
     */
    protected Quad createIconQuad (String name, float width, float height)
    {
        return RenderUtil.createIcon(
            RenderUtil.createTextureState(_ctx, name), width, height);
    }

    /**
     * Creates the named billboard.
     */
    protected void createBillboard ()
    {
        _billboard = new BillboardNode("billboard") {
            public void updateWorldData (float time) {
                super.updateWorldData(time);
                float alpha;
                if ((_elapsed += time) >= RISE_DURATION + LINGER_DURATION +
                    FADE_DURATION) {
                    parent.detachChild(this);
                    billboardDetached();
                    return;
                    
                } else if (_elapsed >= RISE_DURATION + LINGER_DURATION) {
                    alpha = 1f - (_elapsed - RISE_DURATION - LINGER_DURATION) /
                        FADE_DURATION;
                    
                } else if (_elapsed >= RISE_DURATION) {
                    alpha = 1f;
                    localTranslation.z = TILE_SIZE * 1;
                    billboardLinger(_elapsed);
                    
                } else {
                    alpha = _elapsed / RISE_DURATION;
                    localTranslation.z = TILE_SIZE *
                        FastMath.LERP(alpha, _card ? 1.5f : 0.5f, 1f);
                    billboardRise(_elapsed);
                }
                Iterator iter = children.iterator();
                while (iter.hasNext()) {
                    Spatial child = (Spatial)iter.next();
                    if (child instanceof Geometry) {
                        ((Geometry)child).getBatch(0).getDefaultColor().a = 
                            alpha;
                    }
                }
            }
            protected float _elapsed;
        };
    }

    /**
     * Used to add special animation during the rise phase.
     */
    protected void billboardRise (float elapsed)
    {
        // nothing to do here
    }

    /**
     * Used to add special animation during the linger phase.
     */
    protected void billboardLinger (float elapsed)
    {
        // nothing to do here
    }

    /**
     * Called when the billboard detaches itself.
     */
    protected void billboardDetached ()
    {
        // nothing to do here
    }
    
    /** The path of the icon to display. */
    protected String _ipath;
    
    /** The color in which to display the icon (or null for the default). */
    protected ColorRGBA _color;
    
    /** If true, this is a card, so slide it down rather than up and use the
     * card icon background. */
    protected boolean _card;
    
    /** The icon billboard. */
    protected BillboardNode _billboard;
    
    /** The size of the icon. */
    protected static final float ICON_SIZE = TILE_SIZE / 2;
    
    /** The width of cards, if {@link #ICON_SIZE} is the height. */
    protected static final float CARD_WIDTH = ICON_SIZE * 30 / 39;
    
    /** The length of time it takes for the icon to rise up and fade in. */
    protected static final float RISE_DURATION = 0.5f;
    
    /** The length of time the icon lingers before fading out. */
    protected static final float LINGER_DURATION = 1.25f;
    
    /** The length of time it takes for the icon to fade out. */
    protected static final float FADE_DURATION = 0.25f;
}
