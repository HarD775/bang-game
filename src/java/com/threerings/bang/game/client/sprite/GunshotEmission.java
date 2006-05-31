//
// $Id$

package com.threerings.bang.game.client.sprite;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import java.nio.IntBuffer;
import java.nio.FloatBuffer;

import java.util.HashMap;
import java.util.Properties;

import com.jme.math.FastMath;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.scene.Controller;
import com.jme.scene.SharedMesh;
import com.jme.scene.Spatial;
import com.jme.scene.TriMesh;
import com.jme.scene.state.LightState;
import com.jme.scene.state.TextureState;
import com.jme.system.DisplaySystem;
import com.jme.util.geom.BufferUtils;
import com.jmex.effects.particles.ParticleFactory;
import com.jmex.effects.particles.ParticleMesh;

import com.samskivert.util.StringUtil;

import com.threerings.jme.model.Model;
import com.threerings.jme.model.TextureProvider;
import com.threerings.jme.sprite.PathUtil;
import com.threerings.util.RandomUtil;

import com.threerings.bang.game.client.effect.ParticlePool;
import com.threerings.bang.util.RenderUtil;

/**
 * A gunshot effect with muzzle flash and bullet trail.
 */
public class GunshotEmission extends SpriteEmission
{
    @Override // documentation inherited
    public void configure (Properties props, Spatial target)
    {
        super.configure(props, target);
        if (_animations == null) {
            return;
        }
        _animShotFrames = new HashMap<String, int[]>();
        for (String anim : _animations) {
            _animShotFrames.put(anim, StringUtil.parseIntArray(
                props.getProperty(anim + ".shot_frames", "")));
        }
        _size = Float.valueOf(props.getProperty("size", "1"));
        _trails = new Trail[Integer.valueOf(props.getProperty("trails", "1"))];
        _spread = Float.valueOf(props.getProperty("spread", "0"));
    }
    
    @Override // documentation inherited
    public void init (Model model)
    {
        super.init(model);
        _model = model;
        
        if (_fmesh == null) {
            createFlareMesh();
        }
        if (RenderUtil.blendAlpha == null) {
            RenderUtil.initStates();
        }
        _flare = new Flare();
        if (_ftex != null) {
            _flare.setRenderState(_ftex);
        }
        for (int ii = 0; ii < _trails.length; ii++) {
            _trails[ii] = new Trail();
        }
        
        _smoke = new ParticleMesh("smoke", 16);
        _smoke.addController(
            new ParticlePool.TransientParticleController(_smoke));
        _smoke = ParticleFactory.buildParticles("smoke", 16);
        _smoke.setMinimumLifeTime(500f);
        _smoke.setInitialVelocity(0.005f);
        _smoke.setEmissionDirection(Vector3f.UNIT_Z);
        _smoke.setMaximumAngle(FastMath.PI / 16);
        _smoke.setRandomMod(0f);
        _smoke.getParticleController().setPrecision(FastMath.FLT_EPSILON);
        _smoke.getParticleController().setControlFlow(false);
        _smoke.setStartSize(0.25f * _size);
        _smoke.setEndSize(2f * _size);
        _smoke.setStartColor(new ColorRGBA(0.2f, 0.2f, 0.2f, 0.75f));
        _smoke.setEndColor(new ColorRGBA(0.35f, 0.35f, 0.35f, 0f));
        _smoke.getParticleController().setRepeatType(Controller.RT_CLAMP);
        _smoke.getParticleController().setActive(false);
        if (_smoketex != null) {
            _smoke.setRenderState(_smoketex);
        }
        _smoke.setRenderState(RenderUtil.blendAlpha);
        _smoke.setRenderState(RenderUtil.overlayZBuf);
    }
    
    @Override // documentation inherited
    public void resolveTextures (TextureProvider tprov)
    {
        if (_smoketex == null) {
            _smoketex = tprov.getTexture("/textures/effects/dust.png");
            _ftex = tprov.getTexture("/textures/effects/flash.png");
        }
        _smoke.setRenderState(_smoketex);
        _flare.setRenderState(_ftex);
    }
    
    @Override // documentation inherited
    public Controller putClone (
        Controller store, Model.CloneCreator properties)
    {
        GunshotEmission gstore;
        if (store == null) {
            gstore = new GunshotEmission();
        } else {
            gstore = (GunshotEmission)store;
        }
        super.putClone(gstore, properties);
        gstore._animShotFrames = _animShotFrames;
        gstore._size = _size;
        gstore._trails = new Trail[_trails.length];
        gstore._spread = _spread;
        return gstore;
    }
    
    // documentation inherited from interface Externalizable
    public void writeExternal (ObjectOutput out)
        throws IOException
    {
        super.writeExternal(out);
        out.writeObject(_animShotFrames);
        out.writeFloat(_size);
        out.writeInt(_trails.length);
        out.writeFloat(_spread);
    }
    
    // documentation inherited from interface Externalizable
    public void readExternal (ObjectInput in)
        throws IOException, ClassNotFoundException
    {
        super.readExternal(in);
        _animShotFrames = (HashMap<String, int[]>)in.readObject();
        _size = in.readFloat();
        _trails = new Trail[in.readInt()];
        _spread = in.readFloat();
    }
    
    // documentation inherited
    public void update (float time)
    {
        if (_shotFrames == null || _idx >= _shotFrames.length) {
            return;
        }
        int frame = (int)((_elapsed += time) / _frameDuration);
        if (frame >= _shotFrames[_idx]) {
            fireShot();
            _idx++;
        }
    }
    
    @Override // documentation inherited
    protected void animationStarted (String name)
    {
        super.animationStarted(name);
        
        // get the frames at which shots go off, if any
        _shotFrames = (_animShotFrames == null) ?
            null : _animShotFrames.get(name);
        if (_shotFrames == null) {
            return;
        }
        
        // set initial animation state
        _frameDuration = 1f / _model.getAnimation(name).frameRate;
        _idx = 0;
        _elapsed = 0f;
    }
    
    /**
     * Creates the shared flare mesh geometry.
     */
    protected void createFlareMesh ()
    {
        FloatBuffer vbuf = BufferUtils.createVector3Buffer(5),
            tbuf = BufferUtils.createVector2Buffer(5);
        IntBuffer ibuf = BufferUtils.createIntBuffer(12);
        
        vbuf.put(0f).put(0f).put(0f);
        vbuf.put(1f).put(0f).put(1f);
        vbuf.put(1f).put(1f).put(0f);
        vbuf.put(1f).put(0f).put(-1f);
        vbuf.put(1f).put(-1f).put(0f);
        
        tbuf.put(0f).put(0.5f);
        tbuf.put(1f).put(1f);
        tbuf.put(1f).put(0f);
        tbuf.put(1f).put(1f);
        tbuf.put(1f).put(0f);
        
        ibuf.put(0).put(2).put(1);
        ibuf.put(0).put(3).put(2);
        ibuf.put(0).put(1).put(4);
        ibuf.put(0).put(4).put(3);
        
        _fmesh = new TriMesh("fmesh", vbuf, null, null, tbuf, ibuf);
    }
    
    /**
     * Activates the shot effect.
     */
    protected void fireShot ()
    {
        getEmitterLocation(_eloc);
        getEmitterDirection(_edir);
        
        // fire off a flash of light if we're in the real view
        if (_view != null) {
            _view.createLightFlash(_eloc, LIGHT_FLASH_COLOR, 0.125f);
        }
        
        // and a muzzle flare
        _flare.activate(_eloc, _edir);
        
        // and one or more bullet trails
        for (int ii = 0; ii < _trails.length; ii++) {
            _trails[ii].activate(_eloc, _edir);
        }
        
        // and a burst of smoke
        if (!_smoke.getParticleController().isActive()) {
            _model.getEmissionNode().attachChild(_smoke);
            _smoke.updateRenderState();
            _smoke.getParticleController().setActive(true);
        }
        _smoke.setOriginOffset(_eloc);
        _smoke.forceRespawn();
    }
    
    /**
     * Finds a random direction at most <code>spread</code> radians away from
     * the given direction.
     */
    protected void getRandomDirection (
        Vector3f dir, float spread, Vector3f result)
    {
        result.set(Vector3f.UNIT_X);
        _rot.fromAngleNormalAxis(RandomUtil.getFloat(spread),
            Vector3f.UNIT_Y).multLocal(result);
        _rot.fromAngleNormalAxis(RandomUtil.getFloat(FastMath.TWO_PI),
            Vector3f.UNIT_X).multLocal(result);
        PathUtil.computeAxisRotation(Vector3f.UNIT_Z, dir, _rot).multLocal(
            result);
    }
    
    /** Handles the appearance and fading of the muzzle flare. */
    protected class Flare extends SharedMesh
    {
        public Flare ()
        {
            super("flare", _fmesh);
            
            setLightCombineMode(LightState.OFF);
            setRenderQueueMode(Renderer.QUEUE_TRANSPARENT);
            setRenderState(RenderUtil.overlayZBuf);
            setRenderState(RenderUtil.addAlpha);
            setLocalScale(1.5f * _size);
            updateRenderState();
        }
        
        public void activate (Vector3f eloc, Vector3f edir)
        {
            // set the flare location based on the marker position/direction
            getLocalTranslation().set(eloc);
            PathUtil.computeAxisRotation(Vector3f.UNIT_Z, edir,
                getLocalRotation());
            
            _model.getEmissionNode().attachChild(this);
            updateRenderState();
            _elapsed = 0f;
        }
        
        public void updateWorldData (float time)
        {
            super.updateWorldData(time);
            if ((_elapsed += time) >= FLARE_DURATION) {
                _model.getEmissionNode().detachChild(this);
            }
            getBatch(0).getDefaultColor().interpolate(ColorRGBA.white,
                ColorRGBA.black, _elapsed / FLARE_DURATION);
        }
        
        protected float _elapsed;
    }
    
    /** Handles the appearance and fading of the bullet trail. */
    protected class Trail extends TriMesh
    {
        public Trail ()
        {
            super("trail");
            
            // use shared vertex and index buffers, but a unique color buffer
            if (_tvbuf == null) {
                _tvbuf = BufferUtils.createVector3Buffer(4);
                _tvbuf.put(0f).put(0.5f).put(0f);
                _tvbuf.put(0f).put(-0.5f).put(0f);
                _tvbuf.put(1f).put(-0.5f).put(0f);
                _tvbuf.put(1f).put(0.5f).put(0f);
                _tibuf = BufferUtils.createIntBuffer(6);
                _tibuf.put(0).put(1).put(2);
                _tibuf.put(0).put(2).put(3);
            }
            _cbuf = BufferUtils.createFloatBuffer(4 * 4);
            reconstruct(_tvbuf, null, _cbuf, null, _tibuf);
            
            setLightCombineMode(LightState.OFF);
            setRenderQueueMode(Renderer.QUEUE_TRANSPARENT);
            setRenderState(RenderUtil.overlayZBuf);
            setRenderState(RenderUtil.blendAlpha);
            updateRenderState();
        }
        
        public void activate (Vector3f eloc, Vector3f edir)
        {
            // set the transation based on the location of the source
            getLocalTranslation().set(eloc);
            
            // set the scale based on the distance to the target (if it exists)
            _tdist = 10f;
            if (_sprite instanceof MobileSprite) {
                PieceSprite target = ((MobileSprite)_sprite).getTargetSprite();
                if (target != null) {
                    _tdist = target.getWorldTranslation().distance(eloc);
                }
            }
            getLocalScale().set(0f, 0.175f * _size, 1f);
            
            // choose a direction within the spread range
            if (_spread > 0) {
                getRandomDirection(edir, _spread, _sdir);
            } else {
                _sdir.set(edir);
            }
            
            // set the orientation based on the eye vector and direction
            Renderer renderer = DisplaySystem.getDisplaySystem().getRenderer();
            renderer.getCamera().getLocation().subtract(eloc, _eye);
            _eye.cross(_sdir, _yvec).normalizeLocal();
            _sdir.cross(_yvec, _zvec);
            getLocalRotation().fromAxes(_sdir, _yvec, _zvec);
            
            _model.getEmissionNode().attachChild(this);
            _elapsed = 0f;
        }
        
        public void updateWorldData (float time)
        {
            super.updateWorldData(time);
            float lscale, a0, a1;
            if ((_elapsed += time) >=
                TRAIL_EXTEND_DURATION + TRAIL_FADE_DURATION) {
                _model.getEmissionNode().detachChild(this);
                return;
                
            } else if (_elapsed >= TRAIL_EXTEND_DURATION) {
                lscale = a1 = 1f;
                a0 = (_elapsed - TRAIL_EXTEND_DURATION) / TRAIL_FADE_DURATION;
                
            } else {
                lscale = a1 = _elapsed / TRAIL_EXTEND_DURATION;
                a0 = 0f;
            }
            getLocalScale().x = _tdist * lscale;
            
            _color.interpolate(TRAIL_START_COLOR, TRAIL_END_COLOR, a0);
            BufferUtils.setInBuffer(_color, _cbuf, 0);
            BufferUtils.setInBuffer(_color, _cbuf, 1);
            
            _color.interpolate(TRAIL_START_COLOR, TRAIL_END_COLOR, a1);
            BufferUtils.setInBuffer(_color, _cbuf, 2);
            BufferUtils.setInBuffer(_color, _cbuf, 3);
        }
        
        protected FloatBuffer _cbuf;
        
        protected float _elapsed, _tdist;
        protected Vector3f _eye = new Vector3f(), _yvec = new Vector3f(),
            _zvec = new Vector3f();
        protected ColorRGBA _color = new ColorRGBA();
    }
    
    /** For each animation, the frames at which the shots go off. */
    protected HashMap<String, int[]> _animShotFrames;
    
    /** The size of the shots. */
    protected float _size;
    
    /** The trails' maximum angular distance from the firing direction. */
    protected float _spread;
    
    /** The frames at which the shots go off for the current animation. */
    protected int[] _shotFrames;
    
    /** The duration of a single frame in seconds. */
    protected float _frameDuration;
    
    /** The index of the current frame. */
    protected int _idx;
    
    /** The time elapsed since the start of the animation. */
    protected float _elapsed;
    
    /** Result variables to reuse. */
    protected Vector3f _eloc = new Vector3f(), _edir = new Vector3f(),
        _sdir = new Vector3f(), _axis = new Vector3f();
    protected Quaternion _rot = new Quaternion();
    
    /** The model to which this emission is bound. */
    protected Model _model;
    
    /** The muzzle flare handler. */
    protected Flare _flare;
    
    /** The bullet trail handler. */
    protected Trail[] _trails;
    
    /** The gunsmoke particle system. */
    protected ParticleMesh _smoke;
    
    /** The shared flare mesh. */
    protected static TriMesh _fmesh;
    
    /** The flare texture. */
    protected static TextureState _ftex;
    
    /** The smoke texture. */
    protected static TextureState _smoketex;
    
    /** The shared vertex buffer for the bullet trails. */
    protected static FloatBuffer _tvbuf;
    
    /** The shared index buffer for the bullet trails. */
    protected static IntBuffer _tibuf;
    
    /** The color of the flash of light generated by the shot. */
    protected static final ColorRGBA LIGHT_FLASH_COLOR =
        new ColorRGBA(1f, 1f, 0.9f, 1f);
        
    /** The duration of the muzzle flare. */
    protected static final float FLARE_DURATION = 0.125f;
    
    /** The amount of time it takes for the bullet trail to extend fully. */
    protected static final float TRAIL_EXTEND_DURATION = 0.15f;
    
    /** The amount of time it takes for the bullet trail to fade away. */
    protected static final float TRAIL_FADE_DURATION = 0.05f;
    
    /** The starting color of the bullet trail. */
    protected static final ColorRGBA TRAIL_START_COLOR =
        new ColorRGBA(0.75f, 0.75f, 0.75f, 0.75f);
    
    /** The ending color of the bullet trail. */
    protected static final ColorRGBA TRAIL_END_COLOR =
        new ColorRGBA(0.75f, 0.75f, 0.75f, 0f);
        
    private static final long serialVersionUID = 1;
}
