//
// $Id$

package com.threerings.bang.tests;

import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.logging.Level;

import com.samskivert.util.Config;

import com.jme.image.Image;
import com.jme.input.InputHandler;
import com.jme.renderer.Renderer;
import com.jme.scene.Node;
import com.jme.system.DisplaySystem;

import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BRootNode;
import com.jmex.bui.BStyleSheet;

import com.threerings.cast.CharacterManager;
import com.threerings.cast.bundle.BundledComponentRepository;
import com.threerings.media.image.ImageManager;
import com.threerings.resource.ResourceManager;
import com.threerings.util.MessageBundle;
import com.threerings.util.MessageManager;

import com.threerings.jme.JmeApp;
import com.threerings.jme.camera.CameraHandler;
import com.threerings.openal.SoundManager;

import com.threerings.bang.avatar.data.AvatarCodes;
import com.threerings.bang.avatar.util.AvatarLogic;

import com.threerings.bang.client.BangApp;
import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.Model;
import com.threerings.bang.client.util.ImageCache;
import com.threerings.bang.client.util.ModelCache;
import com.threerings.bang.client.util.TextureCache;
import com.threerings.bang.util.BasicContext;

import static com.threerings.bang.Log.log;

/**
 * Initializes the various services needed to bootstrap any test program.
 */
public abstract class TestApp extends JmeApp
{
    protected void initTest ()
    {
        _ctx = new BasicContextImpl();
        _rsrcmgr = new ResourceManager("rsrc");
        _imgmgr = new ImageManager(
            _rsrcmgr, new ImageManager.OptimalImageCreator() {
            public BufferedImage createImage (int w, int h, int t) {
                switch (t) {
                case Transparency.OPAQUE:
                    return new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                default:
                    return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                }
            }
        });

        _msgmgr = new MessageManager(MESSAGE_MANAGER_PREFIX);
        _icache = new ImageCache(_ctx);
        _tcache = new TextureCache(_ctx);
        _mcache = new ModelCache(_ctx);

        ResourceManager.InitObserver obs = new ResourceManager.InitObserver() {
            public void progress (final int percent, long remaining) {
                // we need to get back onto a safe thread
                postRunnable(new Runnable() {
                    public void run () {
                        if (percent >= 0) {
                            postResourcesInit();
                        }
                    }
                });
            }
            public void initializationFailed (Exception e) {
                // TODO: we need to get back onto a safe thread
                // TODO: report to the client
                log.log(Level.WARNING, "Failed to initialize rsrcmgr.", e);
            }
        };
        try {
            _rsrcmgr.initBundles(
                null, "config/resource/manager.properties", obs);
        } catch (IOException ioe) {
            // TODO: report to the client
            log.log(Level.WARNING, "Failed to initialize rsrcmgr.", ioe);
        }
    }

    /**
     * This initialization routine is called once the resource manager has
     * finished unpacking and initializing our resource bundles.
     */
    protected void postResourcesInit ()
    {
        try {
            _charmgr = new CharacterManager(
                _imgmgr, new BundledComponentRepository(
                    _rsrcmgr, _imgmgr, AvatarCodes.AVATAR_RSRC_SET));
            _alogic = new AvatarLogic(
                _rsrcmgr, _charmgr.getComponentRepository());

        } catch (IOException ioe) {
            // TODO: report to the client
            log.log(Level.WARNING, "Initialization failed.", ioe);
        }
        BangUI.init(_ctx);

        BDecoratedWindow window = createWindow();
        createInterface(window);
        _ctx.getRootNode().addWindow(window);
        window.pack();
        window.center();
    }

    protected BDecoratedWindow createWindow ()
    {
        return new BDecoratedWindow(BangUI.stylesheet, null);
    }

    protected void createInterface (BDecoratedWindow window)
    {
    }

    /**
     * The context implementation. This provides access to all of the
     * objects and services that are needed by the operating client.
     */
    protected class BasicContextImpl implements BasicContext
    {
        public DisplaySystem getDisplay () {
            return getContext().getDisplay();
        }

        public Renderer getRenderer () {
            return getContext().getRenderer();
        }

        public CameraHandler getCameraHandler () {
            return getContext().getCameraHandler();
        }

        public Node getGeometry () {
            return getContext().getGeometry();
        }

        public Node getInterface () {
            return getContext().getInterface();
        }

        public InputHandler getInputHandler () {
            return getContext().getInputHandler();
        }

        public BRootNode getRootNode () {
            return getContext().getRootNode();
        }

        public ResourceManager getResourceManager () {
            return _rsrcmgr;
        }

        public MessageManager getMessageManager () {
            return _msgmgr;
        }

        public BStyleSheet getStyleSheet () {
            return BangUI.stylesheet;
        }

        public JmeApp getApp () {
            return TestApp.this;
        }

        public ImageManager getImageManager () {
            return _imgmgr;
        }

        public SoundManager getSoundManager () {
            return null;
        }

        public ImageCache getImageCache () {
            return _icache;
        }

        public TextureCache getTextureCache () {
            return _tcache;
        }

        public CharacterManager getCharacterManager () {
            return _charmgr;
        }

        public AvatarLogic getAvatarLogic () {
            return _alogic;
        }

        public String xlate (String bundle, String message) {
            MessageBundle mb = getMessageManager().getBundle(bundle);
            return (mb == null) ? message : mb.xlate(message);
        }

        public Model loadModel (String type, String name) {
            return _mcache.getModel(type, name);
        }

        public Image loadImage (String rsrcPath) {
            return _icache.getImage(rsrcPath);
        }
    }

    protected BasicContext _ctx;
    protected Config _config = new Config("bang");
    protected ResourceManager _rsrcmgr;
    protected ImageManager _imgmgr;
    protected MessageManager _msgmgr;
    protected ImageCache _icache;
    protected TextureCache _tcache;
    protected ModelCache _mcache;
    protected CharacterManager _charmgr;
    protected AvatarLogic _alogic;

    /** The prefix prepended to localization bundle names before looking
     * them up in the classpath. */
    protected static final String MESSAGE_MANAGER_PREFIX = "rsrc.i18n";
}
