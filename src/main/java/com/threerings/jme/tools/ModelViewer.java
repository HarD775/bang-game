//
// $Id$
//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2010 Three Rings Design, Inc., All Rights Reserved
// http://code.google.com/p/nenya/
//
// This library is free software; you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published
// by the Free Software Foundation; either version 2.1 of the License, or
// (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

package com.threerings.jme.tools;

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import java.net.MalformedURLException;
import java.net.URL;

import java.text.DecimalFormat;

import java.util.HashMap;
import java.util.logging.Level;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSlider;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

import com.google.common.base.Objects;
import com.google.common.collect.Maps;

import com.jme.bounding.BoundingBox;
import com.jme.bounding.BoundingVolume;
import com.jme.image.Texture;
import com.jme.light.DirectionalLight;
import com.jme.math.FastMath;
import com.jme.math.Vector3f;
import com.jme.renderer.Camera;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.scene.Controller;
import com.jme.scene.Line;
import com.jme.scene.Node;
import com.jme.scene.Spatial;
import com.jme.scene.state.LightState;
import com.jme.scene.state.MaterialState;
import com.jme.scene.state.TextureState;
import com.jme.scene.state.WireframeState;
import com.jme.scene.state.ZBufferState;
import com.jme.system.JmeException;
import com.jme.util.LoggingSystem;
import com.jme.util.TextureKey;
import com.jme.util.TextureManager;
import com.jme.util.export.binary.BinaryImporter;
import com.jme.util.geom.Debugger;
import com.jmex.effects.particles.ParticleGeometry;

import com.samskivert.swing.GroupLayout;
import com.samskivert.swing.Spacer;
import com.samskivert.util.PrefsConfig;
import com.samskivert.util.StringUtil;

import com.threerings.resource.ResourceManager;
import com.threerings.util.MessageBundle;
import com.threerings.util.MessageManager;

import com.threerings.jme.JmeCanvasApp;
import com.threerings.jme.camera.CameraHandler;
import com.threerings.jme.model.Model;
import com.threerings.jme.model.TextureProvider;
import com.threerings.jme.util.ShaderCache;
import com.threerings.jme.util.SpatialVisitor;

import static com.threerings.jme.Log.log;

/**
 * A simple viewer application that allows users to examine models and their animations by loading
 * them from their uncompiled <code>.properties</code> / <code>.mxml</code> representations or
 * their compiled <code>.dat</code> representations.
 */
public class ModelViewer extends JmeCanvasApp
{
    public static void main (String[] args)
    {
        // write standard output and error to a log file
        if (System.getProperty("no_log_redir") == null) {
            String dlog = "viewer.log";
            try {
                PrintStream logOut = new PrintStream(
                    new FileOutputStream(dlog), true);
                System.setOut(logOut);
                System.setErr(logOut);

            } catch (IOException ioe) {
                log.warning("Failed to open debug log [path=" + dlog +
                            ", error=" + ioe + "].");
            }
        }
        LoggingSystem.getLoggingSystem().setLevel(Level.WARNING);
        new ModelViewer(args.length > 0 ? args[0] : null);
    }

    /**
     * Creates and initializes an instance of the model viewer application.
     *
     * @param path the path of the model to view, or <code>null</code> for
     * none
     */
    public ModelViewer (String path)
    {
        super(1024, 768);
        _rsrcmgr = new ResourceManager("rsrc");
        _scache = new ShaderCache(_rsrcmgr);
        _msg = new MessageManager("rsrc.i18n").getBundle("jme.viewer");
        _path = path;

        JPopupMenu.setDefaultLightWeightPopupEnabled(false);

        _frame = new JFrame(_msg.get("m.title"));
        _frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JMenuBar menu = new JMenuBar();
        _frame.setJMenuBar(menu);

        JMenu file = new JMenu(_msg.get("m.file_menu"));
        file.setMnemonic(KeyEvent.VK_F);
        menu.add(file);
        Action load = new AbstractAction(_msg.get("m.file_load")) {
            public void actionPerformed (ActionEvent e) {
                showLoadDialog();
            }
        };
        load.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_L);
        load.putValue(Action.ACCELERATOR_KEY,
            KeyStroke.getKeyStroke(KeyEvent.VK_L, KeyEvent.CTRL_MASK));
        file.add(load);

        Action importAction = new AbstractAction(_msg.get("m.file_import")) {
            public void actionPerformed (ActionEvent e) {
                showImportDialog();
            }
        };
        importAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_I);
        importAction.putValue(Action.ACCELERATOR_KEY,
            KeyStroke.getKeyStroke(KeyEvent.VK_I, KeyEvent.CTRL_MASK));
        file.add(importAction);

        file.addSeparator();
        Action quit = new AbstractAction(_msg.get("m.file_quit")) {
            public void actionPerformed (ActionEvent e) {
                System.exit(0);
            }
        };
        quit.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_Q);
        quit.putValue(Action.ACCELERATOR_KEY,
            KeyStroke.getKeyStroke(KeyEvent.VK_Q, KeyEvent.CTRL_MASK));
        file.add(quit);

        JMenu view = new JMenu(_msg.get("m.view_menu"));
        view.setMnemonic(KeyEvent.VK_V);
        menu.add(view);

        Action wireframe = new AbstractAction(_msg.get("m.view_wireframe")) {
            public void actionPerformed (ActionEvent e) {
                _wfstate.setEnabled(!_wfstate.isEnabled());
            }
        };
        wireframe.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_W);
        wireframe.putValue(Action.ACCELERATOR_KEY,
            KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyEvent.CTRL_MASK));
        view.add(new JCheckBoxMenuItem(wireframe));
        view.addSeparator();

        _pivots = new JCheckBoxMenuItem(_msg.get("m.view_pivots"));
        _pivots.setMnemonic(KeyEvent.VK_P);
        _pivots.setAccelerator(
            KeyStroke.getKeyStroke(KeyEvent.VK_P, KeyEvent.CTRL_MASK));
        view.add(_pivots);

        _bounds = new JCheckBoxMenuItem(_msg.get("m.view_bounds"));
        _bounds.setMnemonic(KeyEvent.VK_B);
        _bounds.setAccelerator(
            KeyStroke.getKeyStroke(KeyEvent.VK_B, KeyEvent.CTRL_MASK));
        view.add(_bounds);

        _normals = new JCheckBoxMenuItem(_msg.get("m.view_normals"));
        _normals.setMnemonic(KeyEvent.VK_N);
        _normals.setAccelerator(
            KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_MASK));
        view.add(_normals);
        view.addSeparator();

        Action campos = new AbstractAction(_msg.get("m.view_campos")) {
            public void actionPerformed (ActionEvent e) {
                _campos.setVisible(!_campos.isVisible());
            }
        };
        campos.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_A);
        campos.putValue(Action.ACCELERATOR_KEY,
            KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.CTRL_MASK));
        view.add(new JCheckBoxMenuItem(campos));
        view.addSeparator();

        _vmenu = new JMenu(_msg.get("m.model_variant"));
        view.add(_vmenu);

        JMenu amode = new JMenu(_msg.get("m.animation_mode"));
        final JRadioButtonMenuItem flipbook =
            new JRadioButtonMenuItem(_msg.get("m.mode_flipbook")),
            morph = new JRadioButtonMenuItem(_msg.get("m.mode_morph")),
            skin = new JRadioButtonMenuItem(_msg.get("m.mode_skin"));
        ButtonGroup mgroup = new ButtonGroup() {
            public void setSelected (ButtonModel model, boolean b) {
                super.setSelected(model, b);
                if (b) {
                    if (flipbook.isSelected()) {
                        _animMode = Model.AnimationMode.FLIPBOOK;
                    } else if (morph.isSelected()) {
                        _animMode = Model.AnimationMode.MORPH;
                    } else {
                        _animMode = Model.AnimationMode.SKIN;
                    }
                    if (_loaded != null) {
                        loadModel(_loaded); // reload
                    }
                }
            }
        };

        mgroup.add(flipbook);
        mgroup.add(morph);
        mgroup.add(skin);
        mgroup.setSelected(skin.getModel(), true);
        amode.add(skin);
        amode.add(morph);
        amode.add(flipbook);
        view.add(amode);
        view.addSeparator();

        Action rlight = new AbstractAction(_msg.get("m.view_light")) {
            public void actionPerformed (ActionEvent e) {
                if (_rldialog == null) {
                    _rldialog = new RotateLightDialog();
                    _rldialog.pack();
                }
                _rldialog.setVisible(true);
            }
        };
        rlight.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_R);
        rlight.putValue(Action.ACCELERATOR_KEY,
            KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.CTRL_MASK));
        view.add(new JMenuItem(rlight));

        Action rcamera = new AbstractAction(_msg.get("m.view_recenter")) {
            public void actionPerformed (ActionEvent e) {
                ((OrbitCameraHandler)_camhand).recenter();
                updateCameraPosition();
            }
        };
        rcamera.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_C);
        rcamera.putValue(Action.ACCELERATOR_KEY,
            KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_MASK));
        view.add(new JMenuItem(rcamera));

        _frame.getContentPane().add(getCanvas(), BorderLayout.CENTER);

        JPanel bpanel = new JPanel(new BorderLayout());
        _frame.getContentPane().add(bpanel, BorderLayout.SOUTH);

        _animctrls = new JPanel();
        _animctrls.setBorder(BorderFactory.createEtchedBorder());
        bpanel.add(_animctrls, BorderLayout.NORTH);
        _animctrls.add(new JLabel(_msg.get("m.anim_select")));
        _animctrls.add(_animbox = new JComboBox<String>());
        _animctrls.add(new JButton(
            new AbstractAction(_msg.get("m.anim_start")) {
                public void actionPerformed (ActionEvent e) {
                    String anim = (String)_animbox.getSelectedItem();
                    if (_model.hasAnimation(anim)) {
                        _model.startAnimation(anim);
                    } else { // it's a sequence
                        if (_model.getAnimation() != null) {
                            _sequence = null;
                            _model.stopAnimation();
                        }
                        _sequence = StringUtil.parseStringArray(
                            _model.getProperties().getProperty(
                                anim + ".animations", ""));
                        if (_sequence.length == 0) {
                            _sequence = null;
                        } else {
                            _model.startAnimation(_sequence[_seqidx = 0]);
                        }
                    }
                }
            }));
        _animctrls.add(_animstop = new JButton(
            new AbstractAction(_msg.get("m.anim_stop")) {
                public void actionPerformed (ActionEvent e) {
                    _model.stopAnimation();
                }
            }));
        _animstop.setEnabled(false);
        _animctrls.add(new Spacer(50, 1));
        _animctrls.add(new JLabel(_msg.get("m.anim_speed")));
        _animctrls.add(_animspeed = new JSlider(-100, +100, 0));
        _animspeed.setMajorTickSpacing(10);
        _animspeed.setSnapToTicks(true);
        _animspeed.setPaintTicks(true);
        _animspeed.addChangeListener(new ChangeListener() {
            public void stateChanged (ChangeEvent e) {
                updateAnimationSpeed();
            }
        });
        _animctrls.setVisible(false);

        JPanel spanel = new JPanel(new BorderLayout());
        bpanel.add(spanel, BorderLayout.SOUTH);

        _status = new JLabel(" ");
        _status.setHorizontalAlignment(JLabel.LEFT);
        _status.setBorder(BorderFactory.createEtchedBorder());
        spanel.add(_status, BorderLayout.CENTER);

        _campos = new JLabel(" ");
        _campos.setBorder(BorderFactory.createEtchedBorder());
        _campos.setVisible(false);
        spanel.add(_campos, BorderLayout.EAST);

        _frame.pack();
        _frame.setVisible(true);

        run();
    }

    @Override
    public boolean init ()
    {
        if (!super.init()) {
            return false;
        }
        if (_path != null) {
            loadModel(new File(_path));
        }
        return true;
    }

    @Override
    protected void initDisplay ()
        throws JmeException
    {
        super.initDisplay();
        _ctx.getRenderer().setBackgroundColor(ColorRGBA.gray);
        _ctx.getRenderer().getQueue().setTwoPassTransparency(false);
    }

    @Override
    protected void initInput ()
    {
        super.initInput();

        _camhand.setTiltLimits(-FastMath.HALF_PI, FastMath.HALF_PI);
        _camhand.setZoomLimits(1f, 500f);
        _camhand.tiltCamera(-FastMath.PI * 7.0f / 16.0f);
        updateCameraPosition();

        MouseOrbiter orbiter = new MouseOrbiter();
        _canvas.addMouseListener(orbiter);
        _canvas.addMouseMotionListener(orbiter);
        _canvas.addMouseWheelListener(orbiter);
    }

    /**
     * Updates the camera position label.
     */
    protected void updateCameraPosition ()
    {
        Camera cam = _camhand.getCamera();
        Vector3f pos = cam.getLocation(), dir = cam.getDirection();
        float heading = -FastMath.atan2(dir.x, dir.y) * FastMath.RAD_TO_DEG,
            pitch = FastMath.asin(dir.z) * FastMath.RAD_TO_DEG;
        _campos.setText(
            "XYZ: " + CAMPOS_FORMAT.format(pos.x) + ", " +
            CAMPOS_FORMAT.format(pos.y) + ", " +
            CAMPOS_FORMAT.format(pos.z) +
            " HP: " + CAMPOS_FORMAT.format(heading) + ", " +
            CAMPOS_FORMAT.format(pitch));
    }

    @Override
    protected CameraHandler createCameraHandler (Camera camera)
    {
        return new OrbitCameraHandler(camera);
    }

    @Override
    protected void initRoot ()
    {
        super.initRoot();

        // set default states
        MaterialState mstate = _ctx.getRenderer().createMaterialState();
        mstate.getDiffuse().set(ColorRGBA.white);
        mstate.getAmbient().set(ColorRGBA.white);
        _ctx.getGeometry().setRenderState(mstate);
        _ctx.getGeometry().setRenderState(
            _wfstate = _ctx.getRenderer().createWireframeState());
        _ctx.getGeometry().setNormalsMode(Spatial.NM_GL_NORMALIZE_PROVIDED);
        _wfstate.setEnabled(false);

        // create a grid on the XY plane to provide some reference
        Vector3f[] points = new Vector3f[GRID_SIZE*2 + GRID_SIZE*2];
        float halfLength = (GRID_SIZE - 1) * GRID_SPACING / 2;
        int idx = 0;
        for (int xx = 0; xx < GRID_SIZE; xx++) {
            points[idx++] = new Vector3f(
                -halfLength + xx*GRID_SPACING, -halfLength, 0f);
            points[idx++] = new Vector3f(
                -halfLength + xx*GRID_SPACING, +halfLength, 0f);
        }
        for (int yy = 0; yy < GRID_SIZE; yy++) {
            points[idx++] = new Vector3f(
                -halfLength, -halfLength + yy*GRID_SPACING, 0f);
            points[idx++] = new Vector3f(
                +halfLength, -halfLength + yy*GRID_SPACING, 0f);

        }
        Line grid = new Line("grid", points, null, null, null);
        grid.getBatch(0).getDefaultColor().set(0.25f, 0.25f, 0.25f, 1f);
        grid.setLightCombineMode(LightState.OFF);
        grid.setModelBound(new BoundingBox());
        grid.updateModelBound();
        _ctx.getGeometry().attachChild(grid);
        grid.updateRenderState();

        // attach a dummy node to draw debugging views
        _ctx.getGeometry().attachChild(new Node("debug") {
            public void onDraw (Renderer r) {
                if (_model == null) {
                    return;
                }
                if (_pivots.getState()) {
                    drawPivots(_model, r);
                }
                if (_bounds.getState()) {
                    Debugger.drawBounds(_model, r);
                }
                if (_normals.getState()) {
                    Debugger.drawNormals(_model, r, 5f, true);
                }
            }
        });
    }

    /**
     * Draws the pivot axes of the given node and its children.
     */
    protected void drawPivots (Spatial spatial, Renderer r)
    {
        if (_axes == null) {
            _axes = new Line("axes",
                new Vector3f[] { Vector3f.ZERO, Vector3f.UNIT_X, Vector3f.ZERO,
                    Vector3f.UNIT_Y, Vector3f.ZERO, Vector3f.UNIT_Z }, null,
                new ColorRGBA[] { ColorRGBA.red, ColorRGBA.red,
                    ColorRGBA.green, ColorRGBA.green, ColorRGBA.blue,
                    ColorRGBA.blue }, null);
            _axes.setRenderQueueMode(Renderer.QUEUE_SKIP);
            _axes.setRenderState(r.createZBufferState());
            LightState lstate = r.createLightState();
            lstate.setEnabled(false);
            _axes.setRenderState(lstate);
            _axes.updateRenderState();
        }
        _axes.getRenderState(ZBufferState.RS_ZBUFFER).apply();
        _axes.getRenderState(LightState.RS_LIGHT).apply();
        _axes.getWorldTranslation().set(spatial.getWorldTranslation());
        _axes.getWorldRotation().set(spatial.getWorldRotation());
        _axes.draw(r);

        if (spatial instanceof Node) {
            Node node = (Node)spatial;
            for (int ii = 0, nn = node.getQuantity(); ii < nn; ii++) {
                drawPivots(node.getChild(ii), r);
            }
        }
    }

    @Override
    protected void initLighting ()
    {
        _dlight = new DirectionalLight();
        _dlight.setEnabled(true);
        _dlight.getDirection().set(-1f, 0f, -1f).normalizeLocal();
        _dlight.getAmbient().set(0.25f, 0.25f, 0.25f, 1f);

        LightState lstate = _ctx.getRenderer().createLightState();
        lstate.attach(_dlight);
        _ctx.getGeometry().setRenderState(lstate);
        _ctx.getGeometry().setLightCombineMode(LightState.REPLACE);
    }

    /**
     * Shows the load model dialog.
     */
    protected void showLoadDialog ()
    {
        if (_chooser == null) {
            _chooser = new JFileChooser();
            _chooser.setDialogTitle(_msg.get("m.load_title"));
            _chooser.setFileFilter(new FileFilter() {
                public boolean accept (File file) {
                    if (file.isDirectory()) {
                        return true;
                    }
                    String path = file.toString().toLowerCase();
                    return path.endsWith(".properties") ||
                        path.endsWith(".dat");
                }
                public String getDescription () {
                    return _msg.get("m.load_filter");
                }
            });
            File dir = new File(_config.getValue("dir", "."));
            if (dir.exists()) {
                _chooser.setCurrentDirectory(dir);
            }
        }
        if (_chooser.showOpenDialog(_frame) == JFileChooser.APPROVE_OPTION) {
            loadModel(_chooser.getSelectedFile());
        }
        _config.setValue("dir", _chooser.getCurrentDirectory().toString());
    }

    /**
     * Attempts to load a model from the specified location.
     */
    protected void loadModel (File file)
    {
        String fpath = file.toString();
        try {
            if (fpath.endsWith(".properties")) {
                compileModel(file);
            } else if (fpath.endsWith(".dat")) {
                loadCompiledModel(file);
            } else {
                throw new Exception(_msg.get("m.invalid_type"));
            }
            _status.setText(_msg.get("m.loaded_model", fpath));
            _loaded = file;

        } catch (Exception e) {
            e.printStackTrace();
            _status.setText(_msg.get("m.load_error", fpath, e));
        }
    }

    /**
     * Attempts to compile and load a model.
     */
    protected void compileModel (File file)
        throws Exception
    {
        _status.setText(_msg.get("m.compiling_model", file));
        Model model = CompileModel.compile(file);
        if (model != null) {
            setModel(model, file);
            return;
        }

        // if compileModel returned null, the .dat file is up-to-date
        String fpath = file.toString();
        int didx = fpath.lastIndexOf('.');
        fpath = (didx == -1) ? fpath : fpath.substring(0, didx);
        loadCompiledModel(new File(fpath + ".dat"));
    }

    /**
     * Attempts to load a model that has already been compiled.
     */
    protected void loadCompiledModel (File file)
        throws IOException
    {
        _status.setText(_msg.get("m.loading_model", file));
        setModel(Model.readFromFile(file), file);
    }

    /**
     * Sets the model once it's been loaded.
     *
     * @param file the file from which the model was loaded
     */
    protected void setModel (Model model, File file)
    {
        if (_model != null) {
            _ctx.getGeometry().detachChild(_model);
        }
        _ctx.getGeometry().attachChild(_model = _omodel = model);
        _model.setAnimationMode(_animMode);
        _model.configureShaders(_scache);
        _model.lockStaticMeshes(_ctx.getRenderer(), true, true);

        // load the model's textures
        resolveModelTextures(file);

        // recenter the camera
        _model.updateGeometricState(0f, true);
        ((OrbitCameraHandler)_camhand).recenter();
        updateCameraPosition();

        // configure the variant menu
        _variant = null;
        _vmenu.removeAll();
        ButtonGroup vgroup = new ButtonGroup() {
            public void setSelected (ButtonModel model, boolean b) {
                super.setSelected(model, b);
                String variant = model.getActionCommand();
                if (b && !Objects.equal(variant, _variant)) {
                    setVariant(model.getActionCommand());
                }
            }
        };
        JRadioButtonMenuItem def = new JRadioButtonMenuItem(
            _msg.get("m.variant_default"));
        def.setActionCommand(null);
        vgroup.add(def);
        _vmenu.add(def);
        for (String variant : _model.getVariantNames()) {
            JRadioButtonMenuItem vitem = new JRadioButtonMenuItem(variant);
            vitem.setActionCommand(variant);
            vgroup.add(vitem);
            _vmenu.add(vitem);
        }
        vgroup.setSelected(def.getModel(), true);

        // configure the animation panel
        String[] anims = _model.getAnimationNames();
        if (anims.length == 0) {
            _animctrls.setVisible(false);
            return;
        }
        _model.addAnimationObserver(_animobs);
        _animctrls.setVisible(true);
        DefaultComboBoxModel<String> abmodel = new DefaultComboBoxModel<String>(anims);
        _animbox.setModel(abmodel);
        updateAnimationSpeed();

        // if there are any sequences, add those as well
        String[] seqs = StringUtil.parseStringArray(
            _model.getProperties().getProperty("sequences", ""));
        for (String seq : seqs) {
            abmodel.addElement(seq);
        }
    }

    /**
     * Switches to the named variant.
     */
    protected void setVariant (String variant)
    {
        _model.stopAnimation();
        _ctx.getGeometry().detachChild(_model);
        _ctx.getGeometry().attachChild(
            _model = _omodel.createPrototype(variant));
        _model.addAnimationObserver(_animobs);
        updateAnimationSpeed();
        resolveModelTextures(_loaded);
        _variant = variant;
    }

    /**
     * Resolve the textures from the file's directory.
     */
    protected void resolveModelTextures (File file)
    {
        final File dir = file.getParentFile();
        _model.resolveTextures(new TextureProvider() {
            public TextureState getTexture (String name) {
                TextureState tstate = _tstates.get(name);
                if (tstate == null) {
                    File file;
                    if (name.startsWith("/")) {
                        file = _rsrcmgr.getResourceFile(name.substring(1));
                    } else {
                        file = new File(dir, name);
                    }
                    Texture tex = TextureManager.loadTexture(file.toString(),
                        Texture.MM_LINEAR_LINEAR, Texture.FM_LINEAR);
                    if (tex == null) {
                        log.warning("Couldn't find texture [path=" + file +
                            "].");
                        return null;
                    }
                    tex.setWrap(Texture.WM_WRAP_S_WRAP_T);
                    tstate = _ctx.getRenderer().createTextureState();
                    tstate.setTexture(tex);
                    _tstates.put(name, tstate);
                }
                return tstate;
            }
            protected HashMap<String, TextureState> _tstates = Maps.newHashMap();
        });
        _model.updateRenderState();
    }

    /**
     * Shows the import particle system dialog.
     */
    protected void showImportDialog ()
    {
        if (_ichooser == null) {
            _ichooser = new JFileChooser();
            _ichooser.setDialogTitle(_msg.get("m.import_title"));
            _ichooser.setFileFilter(new FileFilter() {
                public boolean accept (File file) {
                    if (file.isDirectory()) {
                        return true;
                    }
                    String path = file.toString().toLowerCase();
                    return path.endsWith(".jme");
                }
                public String getDescription () {
                    return _msg.get("m.import_filter");
                }
            });
            File dir = new File(_config.getValue("import_dir", "."));
            if (dir.exists()) {
                _ichooser.setCurrentDirectory(dir);
            }
        }
        if (_ichooser.showOpenDialog(_frame) == JFileChooser.APPROVE_OPTION) {
            importFile(_ichooser.getSelectedFile());
        }
        _config.setValue("import_dir",
            _ichooser.getCurrentDirectory().toString());
    }

    /**
     * Attempts to import the specified file as a JME binary scene.
     */
    protected void importFile (File file)
    {
        final File parent = file.getParentFile();
        TextureKey.setLocationOverride(new TextureKey.LocationOverride() {
            public URL getLocation (String name)
                throws MalformedURLException {
                return new URL(parent.toURI().toURL(), name);
            }
        });
        try {
            new ImportDialog(file,
                (Spatial)BinaryImporter.getInstance().load(
                    file)).setVisible(true);

        } catch (Exception e) {
            e.printStackTrace();
            _status.setText(_msg.get("m.load_error", file, e));
        }
        TextureKey.setLocationOverride(null);
    }

    /**
     * Updates the model's animation speed based on the position of the
     * animation speed slider.
     */
    protected void updateAnimationSpeed ()
    {
        _model.setAnimationSpeed(
            FastMath.pow(2f, _animspeed.getValue() / 25f));
    }

    /** The resource manager. */
    protected ResourceManager _rsrcmgr;

    /** The shader cache. */
    protected ShaderCache _scache;

    /** The translation bundle. */
    protected MessageBundle _msg;

    /** The path of the initial model to load. */
    protected String _path;

    /** The last model successfully loaded. */
    protected File _loaded;

    /** The viewer frame. */
    protected JFrame _frame;

    /** The variant menu. */
    protected JMenu _vmenu;

    /** Debug view switches. */
    protected JCheckBoxMenuItem _pivots, _bounds, _normals;

    /** The animation controls. */
    protected JPanel _animctrls;

    /** The animation selector. */
    protected JComboBox<String> _animbox;

    /** The "stop animation" button. */
    protected JButton _animstop;

    /** The animation speed slider. */
    protected JSlider _animspeed;

    /** The status bar. */
    protected JLabel _status;

    /** The camera position display. */
    protected JLabel _campos;

    /** The model file chooser. */
    protected JFileChooser _chooser;

    /** The import file chooser. */
    protected JFileChooser _ichooser;

    /** The desired animation mode. */
    protected Model.AnimationMode _animMode;

    /** The desired variant. */
    protected String _variant;

    /** The light rotation dialog. */
    protected RotateLightDialog _rldialog;

    /** The scene light. */
    protected DirectionalLight _dlight;

    /** Used to toggle wireframe rendering. */
    protected WireframeState _wfstate;

    /** The currently loaded model. */
    protected Model _model;

    /** The original model (before switching to a variant). */
    protected Model _omodel;

    /** Reused to draw pivot axes. */
    protected Line _axes;

    /** The current animation sequence, if any. */
    protected String[] _sequence;

    /** The current index in the animation sequence. */
    protected int _seqidx;

    /** Enables and disables the stop button when animations start and stop. */
    protected Model.AnimationObserver _animobs =
        new Model.AnimationObserver() {
        public boolean animationStarted (Model model, String name) {
            _animstop.setEnabled(true);
            return true;
        }
        public boolean animationCompleted (Model model, String name) {
            if (_sequence != null && ++_seqidx < _sequence.length) {
                _model.startAnimation(_sequence[_seqidx]);
            } else {
                _animstop.setEnabled(false);
                _sequence = null;
            }
            return true;
        }
        public boolean animationCancelled (Model model, String name) {
            if (_sequence != null && ++_seqidx < _sequence.length &&
                _model.getAnimation(name).repeatType != Controller.RT_CLAMP) {
                _model.startAnimation(_sequence[_seqidx]);
            } else {
                _animstop.setEnabled(false);
                _sequence = null;
            }
            return true;
        }
    };

    /** Allows users to manipulate an imported JME file. */
    protected class ImportDialog extends JDialog
        implements ChangeListener
    {
        public ImportDialog (File file, Spatial spatial)
        {
            super(_frame, _msg.get("m.import", file), false);
            _spatial = spatial;

            // rotate from y-up to z-up and set initial scale
            _spatial.getLocalRotation().fromAngleNormalAxis(FastMath.HALF_PI,
                Vector3f.UNIT_X);
            _spatial.setLocalScale(0.025f);

            JPanel cpanel = GroupLayout.makeVBox();
            getContentPane().add(cpanel, BorderLayout.CENTER);

            JPanel spanel = new JPanel();
            spanel.add(new JLabel(_msg.get("m.scale")));
            spanel.add(_scale = new JSlider(0, 1000, 250));
            _scale.addChangeListener(this);
            cpanel.add(spanel);

            JPanel bpanel = new JPanel();
            bpanel.add(new JButton(new AbstractAction(
                _msg.get("m.respawn_particles")) {
                public void actionPerformed (ActionEvent e) {
                    _respawner.traverse(_spatial);
                }
            }));
            bpanel.add(new JButton(new AbstractAction(_msg.get("m.close")) {
                public void actionPerformed (ActionEvent e) {
                    setVisible(false);
                }
            }));
            getContentPane().add(bpanel, BorderLayout.SOUTH);
            pack();
        }

        // documentation inherited from interface ChangeListener
        public void stateChanged (ChangeEvent e)
        {
            _spatial.setLocalScale(_scale.getValue() * 0.0001f);
        }

        @Override
        public void setVisible (boolean visible)
        {
            super.setVisible(visible);
            if (visible && _spatial.getParent() == null) {
                _ctx.getGeometry().attachChild(_spatial);
                _spatial.updateRenderState();
            } else if (!visible && _spatial.getParent() != null) {
                _ctx.getGeometry().detachChild(_spatial);
            }
        }

        /** The imported scene. */
        protected Spatial _spatial;

        /** The scale slider. */
        protected JSlider _scale;
    }

    /** Allows users to move the directional light around. */
    protected class RotateLightDialog extends JDialog
        implements ChangeListener
    {
        public RotateLightDialog ()
        {
            super(_frame, _msg.get("m.rotate_light"), false);

            JPanel cpanel = GroupLayout.makeVBox();
            getContentPane().add(cpanel, BorderLayout.CENTER);

            JPanel apanel = new JPanel();
            apanel.add(new JLabel(_msg.get("m.azimuth")));
            apanel.add(_azimuth = new JSlider(-180, +180, 0));
            _azimuth.addChangeListener(this);
            cpanel.add(apanel);

            JPanel epanel = new JPanel();
            epanel.add(new JLabel(_msg.get("m.elevation")));
            epanel.add(_elevation = new JSlider(-90, +90, 45));
            _elevation.addChangeListener(this);
            cpanel.add(epanel);

            JPanel bpanel = new JPanel();
            bpanel.add(new JButton(new AbstractAction(_msg.get("m.close")) {
                public void actionPerformed (ActionEvent e) {
                    setVisible(false);
                }
            }));
            getContentPane().add(bpanel, BorderLayout.SOUTH);
        }

        // documentation inherited from interface ChangeListener
        public void stateChanged (ChangeEvent e)
        {
            float az = _azimuth.getValue() * FastMath.DEG_TO_RAD,
                el = _elevation.getValue() * FastMath.DEG_TO_RAD;
            _dlight.getDirection().set(
                -FastMath.cos(az) * FastMath.cos(el),
                -FastMath.sin(az) * FastMath.cos(el),
                -FastMath.sin(el));
        }

        /** Azimuth and elevation sliders. */
        protected JSlider _azimuth, _elevation;
    }

    /** Moves the camera using mouse input. */
    protected class MouseOrbiter extends MouseAdapter
        implements MouseMotionListener, MouseWheelListener
    {
        @Override
        public void mousePressed (MouseEvent e)
        {
            _mloc.setLocation(e.getX(), e.getY());
        }

        // documentation inherited from interface MouseMotionListener
        public void mouseMoved (MouseEvent e)
        {
        }

        // documentation inherited from interface MouseMotionListener
        public void mouseDragged (MouseEvent e)
        {
            int dx = e.getX() - _mloc.x, dy = e.getY() - _mloc.y;
            _mloc.setLocation(e.getX(), e.getY());
            int mods = e.getModifiers();
            if ((mods & MouseEvent.BUTTON1_MASK) != 0) {
                _camhand.tiltCamera(dy * FastMath.PI / 1000);
                _camhand.orbitCamera(-dx * FastMath.PI / 1000);
            } else if ((mods & MouseEvent.BUTTON2_MASK) != 0) {
                _camhand.zoomCamera(dy/8f);
            } else {
                _camhand.panCamera(-dx/8f, dy/8f);
            }
            updateCameraPosition();
        }

        // documentation inherited from interface MouseWheelListener
        public void mouseWheelMoved (MouseWheelEvent e)
        {
            _camhand.zoomCamera(e.getWheelRotation() * 10f);
            updateCameraPosition();
        }

        /** The last recorded position of the mouse cursor. */
        protected Point _mloc = new Point();
    }

    /** A camera handler that pans in directions orthogonal to the camera
     * direction. */
    protected class OrbitCameraHandler extends CameraHandler
    {
        public OrbitCameraHandler (Camera camera)
        {
            super(camera);
            _gpoint = super.getGroundPoint();
        }

        @Override
        public void panCamera (float x, float y) {
            Vector3f offset = _camera.getLeft().mult(-x).addLocal(
                _camera.getUp().mult(y));
            getGroundPoint().addLocal(offset);
            _camera.getLocation().addLocal(offset);
            _camera.onFrameChange();
        }

        @Override
        public Vector3f getGroundPoint ()
        {
            return _gpoint;
        }

        /**
         * Resets the ground point to the center of the grid or, if there is
         * one, the center of the model.
         */
        public void recenter ()
        {
            Vector3f target = new Vector3f();
            if (_model != null) {
                BoundingVolume bound = _model.getWorldBound();
                if (bound != null) {
                    bound.getCenter(target);
                }
            }
            Vector3f offset = target.subtract(_gpoint);
            _camera.getLocation().addLocal(offset);
            _camera.onFrameChange();
            _gpoint.set(target);
        }

        /** The point at which the camera is looking. */
        protected Vector3f _gpoint;
    }

    /** The app configuration. */
    protected static PrefsConfig _config = new PrefsConfig("com/threerings/jme/tools/ModelViewer");

    /** Forces all particle systems to respawn. */
    protected static SpatialVisitor<ParticleGeometry> _respawner =
        new SpatialVisitor<ParticleGeometry>(ParticleGeometry.class) {
        protected void visit (ParticleGeometry geom) {
            geom.forceRespawn();
        }
    };

    /** The number of lines on the grid in each direction. */
    protected static final int GRID_SIZE = 32;

    /** The spacing between lines on the grid. */
    protected static final float GRID_SPACING = 2.5f;

    /** The number formal used for the camera position. */
    protected static final DecimalFormat CAMPOS_FORMAT =
        new DecimalFormat("+000.000;-000.000");
}
