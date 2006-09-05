//
// $Id$

package com.threerings.bang.util;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import java.io.File;
import java.io.IOException;

import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;
import java.util.logging.Level;

import javax.imageio.ImageIO;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.Pbuffer;

import com.jme.image.Image;
import com.jme.image.Texture;
import com.jme.light.DirectionalLight;
import com.jme.math.FastMath;
import com.jme.math.Quaternion;
import com.jme.math.Vector2f;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.renderer.TextureRenderer;
import com.jme.scene.Spatial;
import com.jme.scene.VBOInfo;
import com.jme.scene.shape.Quad;
import com.jme.scene.state.AlphaState;
import com.jme.scene.state.CullState;
import com.jme.scene.state.LightState;
import com.jme.scene.state.MaterialState;
import com.jme.scene.state.RenderState;
import com.jme.scene.state.TextureState;
import com.jme.scene.state.ZBufferState;
import com.jme.system.DisplaySystem;
import com.jme.util.TextureManager;
import com.jme.util.geom.BufferUtils;

import com.jmex.bui.util.Dimension;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.RandomUtil;

import com.threerings.jme.JmeCanvasApp;
import com.threerings.media.image.Colorization;

import com.threerings.bang.client.BangPrefs;
import com.threerings.bang.data.TerrainConfig;
import com.threerings.bang.util.BasicContext;

import static com.threerings.bang.Log.*;
import static com.threerings.bang.client.BangMetrics.*;

/**
 * Useful graphics related utility methods.
 */
public class RenderUtil
{
    public static AlphaState blendAlpha;

    public static AlphaState addAlpha;

    public static AlphaState opaqueAlpha;

    public static ZBufferState alwaysZBuf;

    public static ZBufferState lequalZBuf;

    public static ZBufferState overlayZBuf;

    public static CullState backCull;

    public static CullState frontCull;

    public static LightState noLights;

    /**
     * Initializes our commonly used render states and terrain textures.
     */
    public static void init (BasicContext ctx)
    {
        initStates();
    }

    /**
     * Initializes just the shared render states.
     */
    public static void initStates ()
    {
        Renderer renderer = DisplaySystem.getDisplaySystem().getRenderer();

        addAlpha = renderer.createAlphaState();
        addAlpha.setBlendEnabled(true);
        addAlpha.setSrcFunction(AlphaState.SB_SRC_ALPHA);
        addAlpha.setDstFunction(AlphaState.DB_ONE);
        addAlpha.setEnabled(true);

        blendAlpha = renderer.createAlphaState();
        blendAlpha.setBlendEnabled(true);
        blendAlpha.setSrcFunction(AlphaState.SB_SRC_ALPHA);
        blendAlpha.setDstFunction(AlphaState.DB_ONE_MINUS_SRC_ALPHA);
        blendAlpha.setEnabled(true);

        opaqueAlpha = renderer.createAlphaState();
        opaqueAlpha.setBlendEnabled(false);

        alwaysZBuf = renderer.createZBufferState();
        alwaysZBuf.setWritable(false);
        alwaysZBuf.setEnabled(true);
        alwaysZBuf.setFunction(ZBufferState.CF_ALWAYS);

        lequalZBuf = renderer.createZBufferState();
        lequalZBuf.setEnabled(true);
        lequalZBuf.setFunction(ZBufferState.CF_LEQUAL);

        overlayZBuf = renderer.createZBufferState();
        overlayZBuf.setEnabled(true);
        overlayZBuf.setWritable(false);
        overlayZBuf.setFunction(ZBufferState.CF_LEQUAL);

        backCull = renderer.createCullState();
        backCull.setCullMode(CullState.CS_BACK);

        frontCull = renderer.createCullState();
        frontCull.setCullMode(CullState.CS_FRONT);

        noLights = renderer.createLightState();
        noLights.setEnabled(false);
    }

    /** Rounds the supplied value up to a power of two. */
    public static int nextPOT (int value)
    {
        return (Integer.bitCount(value) > 1) ?
            (Integer.highestOneBit(value) << 1) : value;
    }

    /**
     * Returns the average color of the terrain with the given code.
     */
    public static ColorRGBA getGroundColor (BasicContext ctx, int code)
    {
        ColorRGBA color = _groundColors.get(code);
        if (color != null) {
            return color;
        }
        // if we haven't computed it already, determine the overall color
        // average for the texture
        Image img = getGroundTexture(ctx, code).getTexture().getImage();
        ByteBuffer imgdata = img.getData();
        int r = 0, g = 0, b = 0, bytes = imgdata.limit();
        float divisor = 255f * (bytes / 3);
        for (int ii = 0; ii < bytes; ii += 3) {
            // the bytes are stored unsigned in the image but java is going
            // to interpret them as signed, so we need to do some fiddling
            r += btoi(imgdata.get(ii));
            g += btoi(imgdata.get(ii+1));
            b += btoi(imgdata.get(ii+2));
        }
        color = new ColorRGBA(r / divisor, g / divisor, b / divisor, 1f);
        _groundColors.put(code, color);
        return color;
    }

    /**
     * Returns a randomly selected ground texture for the specified terrain
     * type.
     */
    public static TextureState getGroundTexture (BasicContext ctx, int code)
    {
        ArrayList<TextureState> texs = _groundTexs.get(code);
        if (texs == null) {
            TerrainConfig terrain = TerrainConfig.getConfig(code);
            if (terrain == null) {
                log.warning("Requested ground texture for unknown terrain " +
                    "[code=" + code + "].");
                return null;
            }
            _groundTexs.put(code, texs = new ArrayList<TextureState>());
            String prefix = "terrain/" + terrain.type + "/texture";
            for (int ii = 1; ; ii++) {
                String path = prefix + ii + ".png";
                if (!ctx.getResourceManager().getResourceFile(path).exists()) {
                    break;
                }
                TextureState tstate = createTextureState(ctx, path,
                    BangPrefs.isMediumDetail() ? 1f : 0.5f);
                tstate.getTexture().setScale(
                    new Vector3f(1/terrain.scale, 1/terrain.scale, 1f));
                texs.add(tstate);
            }
            if (texs.size() == 0) {
                log.warning("Found no ground textures [type=" + terrain + "].");
            }
        }
        return (texs.size() == 0) ? null : RandomUtil.pickRandom(texs);
    }

    /**
     * Creates a {@link Quad} with a texture configured to display the supplied
     * text. The text will be white, but its color may be set with {@link
     * Quad#setDefaultColor}.
     */
    public static Quad createTextQuad (BasicContext ctx, Font font, String text)
    {
        Vector2f[] tcoords = new Vector2f[4];
        Dimension size = new Dimension();
        TextureState tstate = ctx.getRenderer().createTextureState();
        Texture tex = createTextTexture(
            ctx, font, ColorRGBA.white, null, text, tcoords, size);
        tstate.setTexture(tex);

        Quad quad = new Quad("text", size.width, size.height);
        tstate.setEnabled(true);
        quad.setRenderState(tstate);

        quad.setTextureBuffer(0, BufferUtils.createFloatBuffer(tcoords));
        quad.setRenderState(blendAlpha);
        quad.updateRenderState();

        return quad;
    }

    /**
     * Renders the specified text into an image (which will be sized
     * appropriately for the text) and creates a texture from it.
     *
     * @param ocolor if not-null the text will be outlined in the supplied
     * color.
     * @param tcoords should be a four element array which will be filled
     * in with the appropriate texture coordinates to only display the
     * text.
     */
    public static Texture createTextTexture (
        BasicContext ctx, Font font, ColorRGBA color, ColorRGBA ocolor,
        String text, Vector2f[] tcoords, Dimension size)
    {
        Graphics2D gfx = _scratch.createGraphics();
        Color acolor = new Color(color.r, color.g, color.b, color.a);
        Color oacolor = (ocolor == null) ? null :
            new Color(ocolor.r, ocolor.g, ocolor.b, ocolor.a);
        TextLayout layout;
        try {
            gfx.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                                 RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            gfx.setColor(acolor);
            layout = new TextLayout(text, font, gfx.getFontRenderContext());
        } finally {
            gfx.dispose();
        }

        // determine the size of our rendered text
        // TODO: do the Mac hack to get the real bounds
        Rectangle2D bounds = (oacolor == null) ? layout.getBounds() :
            layout.getOutline(null).getBounds();
        int width = (int)(Math.max(bounds.getX(), 0) + bounds.getWidth());
        int height = (int)(layout.getLeading() + layout.getAscent() +
                           layout.getDescent());

        if (size != null) {
            size.width = width;
            size.height = height;
        }

        // now determine the size of our texture image which must be
        // square and a power of two (yay!)
        int tsize = nextPOT(Math.max(width, Math.max(height, 1)));

        // render the text into the image
        BufferedImage image = ctx.getImageCache().createCompatibleImage(
            tsize, tsize, true);
        gfx = image.createGraphics();
        try {
            gfx.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                 RenderingHints.VALUE_ANTIALIAS_ON);
            gfx.setColor(acolor);
            if (oacolor != null) {
                gfx.translate(0, layout.getAscent());
                gfx.fill(layout.getOutline(null));
                gfx.setColor(oacolor);
                gfx.draw(layout.getOutline(null));
            } else {
                gfx.setComposite(AlphaComposite.SrcOut);
                layout.draw(gfx, -(float)bounds.getX(), layout.getAscent());
            }
        } finally {
            gfx.dispose();
        }

        // fill in the texture coordinates
        float tsf = tsize;
        tcoords[0] = new Vector2f(0, 0);
        tcoords[1] = new Vector2f(0, height/tsf);
        tcoords[2] = new Vector2f(width/tsf, height/tsf);
        tcoords[3] = new Vector2f(width/tsf, 0);

        return createTexture(ctx.getImageCache().convertImage(image));
    }

    /**
     * Loads a texture from the local file system.
     */
    public static Texture loadTexture (
        BasicContext ctx, String path, Colorization[] zations)
    {
        try {
            BufferedImage bimg = ImageIO.read(new File(path));
            return createTexture(zations == null ?
                ctx.getImageCache().createImage(bimg, true) :
                ctx.getImageCache().createImage(bimg, zations, true));
        } catch (IOException ioe) {
            log.log(Level.WARNING, "Unable to load texture [path=" + path +
                ", error=" + ioe + "].");
            return null;
        }
    }

    /**
     * Creates a texture using the supplied image.
     */
    public static Texture createTexture (Image image)
    {
        Texture texture = new Texture();
        texture.setCorrection(Texture.CM_PERSPECTIVE);
        texture.setFilter(Texture.FM_LINEAR);
        texture.setMipmapState(Texture.MM_LINEAR_LINEAR);
        texture.setWrap(Texture.WM_WRAP_S_WRAP_T);
        texture.setImage(image);
        return texture;
    }

    /**
     * Creates a texture state using the image with the supplied path. The
     * texture is loaded via the texture cache.
     */
    public static TextureState createTextureState (
        BasicContext ctx, String path)
    {
        return createTextureState(ctx, path, 1f);
    }

    /**
     * Creates a texture state using the image with the supplied path and scale
     * factor. The texture is loaded via the texture cache.
     */
    public static TextureState createTextureState (
        BasicContext ctx, String path, float scale)
    {
        return createTextureState(ctx,
            ctx.getTextureCache().getTexture(path, scale));
    }

    /**
     * Creates a texture state using the supplied texture.
     */
    public static TextureState createTextureState (
        BasicContext ctx, Texture texture)
    {
        TextureState tstate = ctx.getRenderer().createTextureState();
        tstate.setEnabled(true);
        tstate.setTexture(texture);
        return tstate;
    }

    /**
     * Configures the specified texture's scale and translation so as to select
     * the <code>tile</code>th tile from a <code>size</code> x
     * <code>size</code> grid.
     */
    public static void setTextureTile (Texture texture, int size, int tile)
    {
        texture.setScale(new Vector3f(1/(float)size, 1/(float)size, 0));
        int y = size - tile / size - 1, x = tile % size;
        texture.setTranslation(new Vector3f(x/(float)size, y/(float)size, 0));
    }

    /**
     * Creates the shadow texture for the specified light parameters.
     */
    public static TextureState createShadowTexture (
        BasicContext ctx, float length, float rotation, float intensity)
    {
        // reuse our existing texture if possible; if not, release old
        // texture
        if (_shadtex != null && _slength == length &&
            _srotation == rotation && _sintensity == intensity) {
            return _shadtex;

        } else if (_shadtex != null) {
            _shadtex.deleteAll();
        }

        _slength = length;
        _srotation = rotation;
        _sintensity = intensity;

        float yscale = length / TILE_SIZE;
        int size = SHADOW_TEXTURE_SIZE, hsize = size / 2;
        ByteBuffer pbuf = ByteBuffer.allocateDirect(size * size * 4);
        byte[] pixel = new byte[] { 0, 0, 0, 0 };
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float xd = (float)(x - hsize) / hsize,
                    yd = yscale * (y - hsize) / hsize,
                    d = FastMath.sqrt(xd*xd + yd*yd),
                    val = d < 0.25f ? intensity : intensity *
                        Math.max(0f, 1.333f - 1.333f*d);
                pixel[3] = (byte)(val * 255);
                pbuf.put(pixel);
            }
        }
        pbuf.rewind();

        // we must rotate the shadow into place and translate to recenter
        Texture stex = new Texture();
        stex.setImage(new Image(Image.RGBA8888, size, size, pbuf));
        Quaternion rot = new Quaternion();
        rot.fromAngleNormalAxis(-rotation, Vector3f.UNIT_Z);
        stex.setRotation(rot);
        Vector3f trans = new Vector3f(0.5f, 0.5f, 0f);
        rot.multLocal(trans);
        stex.setTranslation(new Vector3f(0.5f - trans.x, 0.5f - trans.y, 0f));

        _shadtex = ctx.getRenderer().createTextureState();
        _shadtex.setTexture(stex);
        return _shadtex;
    }

    /**
     * Creates and returns a texture renderer for a target with the given
     * dimensions.
     */
    public static TextureRenderer createTextureRenderer (BasicContext ctx,
        int width, int height)
    {
        // if the video card supports rendering straight to texture, use
        // JME's texture renderer; otherwise, render to the back buffer
        // (temporarily disabled)
        int caps = Pbuffer.getCapabilities();
        if (false && (caps & Pbuffer.RENDER_TEXTURE_SUPPORTED) != 0) {
            return ctx.getDisplay().createTextureRenderer(width, height, true,
                false, false, false, TextureRenderer.RENDER_TEXTURE_2D, 0);

        } else {
            return new BackTextureRenderer(ctx, width, height);
        }
    }

    /**
     * Deletes all the vertex buffer objects identified in the given info.
     */
    public static void deleteVBOs (BasicContext ctx, VBOInfo vboinfo)
    {
        Renderer r = ctx.getRenderer();
        r.deleteVBO(vboinfo.getVBOColorID());
        r.deleteVBO(vboinfo.getVBOIndexID());
        r.deleteVBO(vboinfo.getVBONormalID());
        r.deleteVBO(vboinfo.getVBOVertexID());
        for (int ii = 0; ii < 2; ii++) {
            r.deleteVBO(vboinfo.getVBOTextureID(ii));
        }
    }

    /**
     * Creates a JME {@link ColorRGBA} object with alpha equal to one from a
     * packed RGB value.
     */
    public static ColorRGBA createColorRGBA (int rgb)
    {
        return new ColorRGBA(((rgb >> 16) & 0xFF) / 255f,
                             ((rgb >> 8) & 0xFF) / 255f,
                             (rgb & 0xFF) / 255f, 1f);
    }

    protected static final int btoi (byte value)
    {
        return (value < 0) ? 256 + value : value;
    }

    protected static HashIntMap<ArrayList<TextureState>> _groundTexs =
        new HashIntMap<ArrayList<TextureState>>();

    /** Maps terrain codes to average colors. */
    protected static HashIntMap<ColorRGBA> _groundColors =
        new HashIntMap<ColorRGBA>();

    /** Our most recently created shadow texture. */
    protected static TextureState _shadtex;

    /** The parameters used to create our shadow texture. */
    protected static float _slength, _srotation, _sintensity;

    /** The maximum number of different variations we might have for a
     * particular ground tile. */
    protected static final int MAX_TILE_VARIANT = 4;

    /** Used to obtain a graphics context for measuring text before we
     * create the real image. */
    protected static BufferedImage _scratch =
        new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

    /** Used to fill an image with transparency. */
    protected static Color BLANK = new Color(1.0f, 1.0f, 1.0f, 0f);

    /** The size of the shadow texture. */
    protected static final int SHADOW_TEXTURE_SIZE = 128;
}
