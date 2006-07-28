//
// $Id$

package com.threerings.bang.game.client.effect;

import com.jme.light.PointLight;
import com.jme.math.FastMath;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.Controller;
import com.jme.scene.Node;
import com.jme.scene.SharedMesh;
import com.jme.scene.VBOInfo;
import com.jme.scene.shape.Sphere;
import com.jme.scene.state.LightState;
import com.jme.scene.state.MaterialState;
import com.jme.scene.state.TextureState;
import com.jmex.effects.particles.ParticleMesh;

import com.samskivert.util.RandomUtil;

import com.threerings.bang.client.BangPrefs;
import com.threerings.bang.data.TerrainConfig;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.util.RenderUtil;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays an explosion.
 */
public class ExplosionViz extends ParticleEffectViz
{
    @Override // documentation inherited
    public void display (PieceSprite target)
    {
        // set up and add the dust ring
        if (_dustring != null) {
            prepareDustRing(target);
            displayParticles(target, _dustring, false);
        }
        
        // add the explosion effect
        displayEffect("frontier_town/explosion", target);
        
        // and the streamers
        if (_streamers != null) {
            for (int i = 0; i < _streamers.length; i++) {
                displayParticles(target, _streamers[i].particles, true);
            }
        }
        
        // and the light
        if (BangPrefs.isMediumDetail()) {
            Vector3f location = new Vector3f(target.getLocalTranslation());
            location.z += TILE_SIZE/2;
            _view.createLightFlash(location, LIGHT_FLASH_COLOR,
                LIGHT_FLASH_DURATION);
        }
        
        // note that the effect was displayed
        effectDisplayed();
    }
    
    @Override // documentation inherited
    protected void didInit ()
    {
        // create the dust ring for explosions on the ground
        if (!_target.isAirborne() && BangPrefs.isHighDetail()) {
            _dustring = ParticlePool.getDustRing();
        }
        
        // create a few streamers from the explosion
        if (BangPrefs.isHighDetail()) {
            _streamers = new Streamer[NUM_STREAMERS_AVG +
                RandomUtil.getInt(+NUM_STREAMERS_DEV, -NUM_STREAMERS_DEV)];
            for (int i = 0; i < _streamers.length; i++) {
                _streamers[i] = new Streamer();
            }
        }
    }
    
    /**
     * (Re)initializes the dust ring particle system for use on the specified
     * kind of terrain.
     */
    protected void prepareDustRing (PieceSprite target)
    {
        TerrainConfig terrain = TerrainConfig.getConfig(
            _view.getBoard().getPredominantTerrain(_target.x, _target.y));
        ColorRGBA color = RenderUtil.getGroundColor(_ctx, terrain.code);
        _dustring.getStartColor().set(color.r, color.g, color.b,
            terrain.dustiness);
        _dustring.getEndColor().set(color.r, color.g, color.b, 0f);
        
        _dustring.setLocalTranslation(target.getLocalTranslation());
        _dustring.setLocalRotation(target.getLocalRotation());
    }
    
    /**
     * Handles a streamer flying from the blast.
     */
    protected class Streamer
    {
        /** The particle manager for the streamer. */
        public ParticleMesh particles;
        
        public Streamer ()
        {
            particles = ParticlePool.getStreamer();
            particles.setOriginOffset(new Vector3f());
            
            // fire the streamer in a random direction
            float azimuth = RandomUtil.getFloat(FastMath.TWO_PI),
                elevation = RandomUtil.getFloat(FastMath.HALF_PI) -
                    FastMath.PI * 0.25f;
            _velocity = new Vector3f(
                FastMath.cos(azimuth) * FastMath.cos(elevation),
                FastMath.sin(azimuth) * FastMath.cos(elevation),
                FastMath.sin(elevation));
            _velocity.mult(TILE_SIZE / 2, particles.getOriginOffset());
            _velocity.multLocal(STREAMER_INIT_SPEED);
            
            particles.addController(new Controller() {
                public void update (float time) {
                    // update the position and velocity of the emitter
                    Vector3f origin = particles.getOriginOffset();
                    origin.scaleAdd(time, _velocity, origin);
                    _velocity.scaleAdd(time, STREAMER_ACCEL, _velocity);
                    
                    // remove streamer if its lifespan has elapsed
                    if ((_age += time) > STREAMER_LIFESPAN) {
                        particles.removeController(this);
                        removeParticles(particles);
                    }
                }
            });
        }
        
        /** The velocity of the streamer's emitter. */
        protected Vector3f _velocity;
        
        /** The age of this streamer in seconds. */
        protected float _age;
    }
    
    protected ParticleMesh _dustring;
    protected Streamer[] _streamers;
    protected PointLight _light;
    
    /** The average number of streamers to throw from the explosion. */
    protected static final int NUM_STREAMERS_AVG = 4;
    
    /** The deviation of the number of streamers. */
    protected static final int NUM_STREAMERS_DEV = 2;
    
    /** The initial speed of the streamers. */
    protected static final float STREAMER_INIT_SPEED = 25f;
    
    /** The acceleration of the streamers. */
    protected static final Vector3f STREAMER_ACCEL = new Vector3f(0f, 0f, -100f);
    
    /** The amount of time in seconds to keep the streamers alive. */
    protected static final float STREAMER_LIFESPAN = 2f;
    
    /** The color of the flash of light generate by the explosion. */
    protected static final ColorRGBA LIGHT_FLASH_COLOR =
        new ColorRGBA(1f, 0.75f, 0.25f, 1f);
    
    /** The duration of the light flash. */
    protected static final float LIGHT_FLASH_DURATION = 0.125f;
}
