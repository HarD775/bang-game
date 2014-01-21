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

package com.threerings.jme;

import java.awt.AWTEvent;
import java.awt.Canvas;
import java.awt.Graphics;
import java.awt.EventQueue;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import org.lwjgl.LWJGLException;

import com.jme.renderer.lwjgl.LWJGLRenderer;
import com.jme.scene.Node;
import com.jme.system.DisplaySystem;
import com.jmex.awt.JMECanvas;
import com.jmex.awt.JMECanvasImplementor;
import com.jmex.awt.lwjgl.LWJGLCanvas;

import com.jmex.bui.CanvasRootNode;

import static com.threerings.jme.Log.log;

/**
 * Extends the basic {@link JmeApp} with the necessary wiring to use the
 * GL/AWT bridge to display our GL interface in an AWT component.
 */
public class JmeCanvasApp extends JmeApp
{
    public JmeCanvasApp (int width, int height)
    {
        _display = DisplaySystem.getDisplaySystem("LWJGL");
        
        // create a throwaway canvas so that the display system knows it's
        // created, then create our custom canvas
        _display.createCanvas(width, height);
        try {
            _canvas = createCanvas();
        } catch (LWJGLException e) {
            log.warning("Failed to create LWJGL canvas [error=" + e + "].");
        }
        ((JMECanvas)_canvas).setImplementor(_winimp);
        _canvas.setBounds(0, 0, width, height);
        _canvas.addComponentListener(new ComponentAdapter() {
            public void componentResized (ComponentEvent ce) {
                _winimp.resizeCanvas(_canvas.getWidth(), _canvas.getHeight());
            }
        });
    }

    /**
     * Returns the AWT canvas that contains our GL display.
     */
    public Canvas getCanvas ()
    {
        return _canvas;
    }

    public void run ()
    {
        Thread t = new Thread() {
            public void run () {
                while (!_finished) {
                    // queue up another repaint
                    _canvas.repaint();
                    try {
                        Thread.sleep(10);
                    } catch (Exception e) {
                    }
                }
            }
        };
        t.setDaemon(true);
        t.start();
    }

    // documentation inherited from interface RunQueue
    public void postRunnable (Runnable r)
    {
        EventQueue.invokeLater(r);
    }

    // documentation inherited from interface RunQueue
    public boolean isDispatchThread ()
    {
        return EventQueue.isDispatchThread();
    }
    
    /**
     * Creates and returns the LWJGL canvas instance.
     */
    protected Canvas createCanvas ()
        throws LWJGLException
    {
        // LWJGL's canvas releases the context after painting.  we make it
        // current again, because we want it valid when we process events.
        // block mouse and keyboard events until the context is valid.
        return new LWJGLCanvas() {
            public void addNotify () {
                super.addNotify();
                disableEvents(MOUSE_KEY_EVENT_MASK);
            }
            public void update (Graphics g) {
                super.update(g);
                try {
                    makeCurrent();
                } catch (LWJGLException e) {
                    log.warning("Failed to make context current [error=" +
                        e + "].");
                }
            }
            protected void initGL () {
                super.initGL();
                enableEvents(MOUSE_KEY_EVENT_MASK);
            }
        };
    }
    
    // documentation inherited
    protected DisplaySystem createDisplay ()
    {
        // we've already created our display earlier so just return it
        _api = "LWJGL";
        return _display;
    }

    // documentation inherited
    protected void initInterface ()
    {
        _iface = new Node("Interface");
        _root.attachChild(_iface);

        _rnode = new CanvasRootNode(_canvas);
        _iface.attachChild(_rnode);
    }

    /** This is used if we embed our GL display in an AWT component. */
    protected JMECanvasImplementor _winimp = new JMECanvasImplementor() {
        public void doSetup () {
            super.doSetup();

            LWJGLRenderer renderer =
                new LWJGLRenderer(_canvas.getWidth(), _canvas.getHeight());
            renderer.setHeadless(true);
            setRenderer(renderer);
            _display.setRenderer(renderer);
            _display.getCurrentContext().setupRecords(renderer);
            DisplaySystem.updateStates(renderer);

            if (!init()) {
                log.warning("JmeCanvasApp init failed.");
            }
        }

        public void doUpdate () {
        }

        public void doRender () {
            // here we do our normal frame processing
            try {
                processFrame();
                // we don't process events as the AWT queue handles them
                _failures = 0;
            } catch (Throwable t) {
                log.warning(t);
                // stick a fork in things if we fail too many
                // times in a row
                if (++_failures > MAX_SUCCESSIVE_FAILURES) {
                    JmeCanvasApp.this.stop();
                }
            }
        }
    };

    protected Canvas _canvas;
    
    protected static final long MOUSE_KEY_EVENT_MASK =
        AWTEvent.MOUSE_EVENT_MASK |
        AWTEvent.MOUSE_MOTION_EVENT_MASK |
        AWTEvent.MOUSE_WHEEL_EVENT_MASK |
        AWTEvent.KEY_EVENT_MASK;
}
