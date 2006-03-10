//
// $Id$

package com.threerings.bang.game.client;

import java.awt.image.BufferedImage;

import java.io.File;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import javax.imageio.ImageIO;

import com.jme.image.Image;
import com.jme.image.Texture;
import com.jme.math.FastMath;
import com.jme.math.Quaternion;
import com.jme.math.Vector2f;
import com.jme.math.Vector3f;
import com.jme.renderer.Camera;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.renderer.TextureRenderer;
import com.jme.scene.Node;
import com.jme.scene.TriMesh;
import com.jme.scene.shape.Disk;
import com.jme.scene.shape.Dome;
import com.jme.scene.state.LightState;
import com.jme.scene.state.TextureState;
import com.jme.system.DisplaySystem;
import com.jme.util.geom.BufferUtils;

import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.RenderUtil;

import static com.threerings.bang.Log.log;

/**
 * Used to display the sky.
 */
public class SkyNode extends Node
{
    public SkyNode (BasicContext ctx)
    {
        super("skynode");
        _ctx = ctx;
        
        setLightCombineMode(LightState.OFF);
        
        // create the dome geometry
        _dome = new Dome("dome", DOME_PLANES, DOME_RADIAL_SAMPLES,
            DOME_RADIUS);
        Quaternion rot = new Quaternion();
        rot.fromAngleNormalAxis(FastMath.HALF_PI, Vector3f.UNIT_X);
        _dome.setLocalRotation(rot);
        _dome.setLocalTranslation(new Vector3f(0f, 0f, -20f));
        _dome.setRenderState(_gtstate =
            _ctx.getRenderer().createTextureState());
        _gtstate.setTexture(null, 0);
        _dome.lockMeshes(ctx.getRenderer());
        attachChild(_dome);
        _dome.updateRenderState();
        
        // create the cloud plane geometry, which fades out towards the edge
        _clouds = new Disk("clouds", CLOUD_SHELL_SAMPLES, CLOUD_RADIAL_SAMPLES,
            CLOUD_RADIUS);
        _clouds.setLocalTranslation(new Vector3f(0f, 0f, CLOUD_HEIGHT));
        FloatBuffer cbuf = BufferUtils.createColorBuffer(1 +
            (CLOUD_SHELL_SAMPLES - 1) * CLOUD_RADIAL_SAMPLES);
        float[] color = new float[] { 1f, 1f, 1f, 1f };
        cbuf.put(color);
        int rings = CLOUD_SHELL_SAMPLES - 1;
        float d;
        for (int ii = 0; ii < CLOUD_RADIAL_SAMPLES; ii++) {
            for (int jj = 0; jj < rings; jj++) {
                d = (float)(jj + 1) / rings;
                color[3] = (d < 0.5f) ? 1f : 1f - (d - 0.5f) / 0.5f;
                cbuf.put(color);
            }
        }
        _clouds.setColorBuffer(cbuf);
        _clouds.setRenderState(_ctstate =
            _ctx.getRenderer().createTextureState());
        _ctstate.setTexture(null, 0);
        _clouds.lockMeshes(ctx.getRenderer());
        attachChild(_clouds);
        _clouds.setRenderState(_ctstate = RenderUtil.createTextureState(ctx,
            "textures/environ/clouds.png"));
        Texture ctex = _ctstate.getTexture();
        ctex.setWrap(Texture.WM_WRAP_S_WRAP_T);
        ctex.setScale(new Vector3f(CLOUD_TEXTURE_SCALE,
            CLOUD_TEXTURE_SCALE, CLOUD_TEXTURE_SCALE));
        ctex.setTranslation(new Vector3f());
        _clouds.setRenderQueueMode(Renderer.QUEUE_TRANSPARENT);
        _clouds.setRenderState(RenderUtil.blendAlpha);
        _clouds.setRenderState(RenderUtil.alwaysZBuf);
        _clouds.updateRenderState();
    }
    
    /**
     * Initializes the sky geometry using data from the given board
     * and saves the board reference for later updates.
     */
    public void createBoardSky (BangBoard board)
    {
        _board = board;
        
        // (re)create the gradient texture
        refreshGradient();
    }
    
    /**
     * Updates the gradient texture according to the board parameters.
     */
    public void refreshGradient ()
    {
        _gtstate.deleteAll();
        _gtstate.setTexture(createGradientTexture());
        _dome.updateRenderState();
    }
    
    @Override // documentation inherited
    public void updateWorldData (float time)
    {
        super.updateWorldData(time);
        if (_board == null) {
            return;
        }
        
        // move the clouds according to the wind velocity
        float wdir = _board.getWindDirection(), wspeed = _board.getWindSpeed();
        _ctstate.getTexture().getTranslation().addLocal(
            time * wspeed * FastMath.cos(wdir) * 0.001f,
            time * wspeed * FastMath.sin(wdir) * 0.001f, 0f);
    }
    
    @Override // documentation inherited
    public void draw (Renderer renderer)
    {
        // match the position of the camera
        worldTranslation.set(renderer.getCamera().getLocation());
        _dome.updateWorldVectors();
        _clouds.updateWorldVectors();
        
        super.draw(renderer);
    }
    
    /**
     * Creates and returns the gradient texture that fades from the horizon
     * color to the overhead color.
     */
    protected Texture createGradientTexture ()
    {
        int size = GRADIENT_TEXTURE_SIZE;
        ByteBuffer pbuf = ByteBuffer.allocateDirect(size * 3);
        ColorRGBA hcolor = RenderUtil.createColorRGBA(
            _board.getSkyHorizonColor()),
                ocolor = RenderUtil.createColorRGBA(
            _board.getSkyOverheadColor()),
                tcolor = new ColorRGBA();
        float falloff = _board.getSkyFalloff();
        
        for (int i = 0; i < size; i++) {
            float s = i / (size-1f),
                a = FastMath.exp(-falloff * s);
            tcolor.interpolate(ocolor, hcolor, a);
            
            pbuf.put((byte)(tcolor.r * 255));
            pbuf.put((byte)(tcolor.g * 255));
            pbuf.put((byte)(tcolor.b * 255));
        }
        pbuf.rewind();
        
        Texture texture = new Texture();
        texture.setImage(new Image(Image.RGB888, 1, size, pbuf));
        texture.setFilter(Texture.FM_LINEAR);
        return texture;
    }
    
    /** The application context. */
    protected BasicContext _ctx;
    
    /** The dome geometry. */
    protected Dome _dome;
    
    /** The gradient texture state. */
    protected TextureState _gtstate;
    
    /** The cloud plane geometry. */
    protected Disk _clouds;
    
    /** The cloud texture state. */
    protected TextureState _ctstate;
    
    /** The current board object. */
    protected BangBoard _board;
    
    /** The number of vertical samples in the sky dome. */
    protected static final int DOME_PLANES = 16;
    
    /** The number of radial samples for the sky dome. */
    protected static final int DOME_RADIAL_SAMPLES = 32;
    
    /** The radius of the sky dome. */
    protected static final float DOME_RADIUS = 1000f;
    
    /** The size of the one-dimensional gradient texture. */
    protected static final int GRADIENT_TEXTURE_SIZE = 1024;
    
    /** The number of rings in the cloud plane. */
    protected static final int CLOUD_SHELL_SAMPLES = 16;
    
    /** The number of radial samples in the cloud plane. */
    protected static final int CLOUD_RADIAL_SAMPLES = 16;
    
    /** The radius of the cloud plane. */
    protected static final float CLOUD_RADIUS = 10000f;
    
    /** The height of the cloud plane. */
    protected static final float CLOUD_HEIGHT = 500f;
    
    /** The texture scale (tiling factor) of the cloud texture. */
    protected static final float CLOUD_TEXTURE_SCALE = 10f;
}
