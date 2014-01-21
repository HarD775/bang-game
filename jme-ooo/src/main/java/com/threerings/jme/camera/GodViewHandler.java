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

package com.threerings.jme.camera;

import com.jme.math.FastMath;

import com.jme.input.InputHandler;
import com.jme.input.KeyBindingManager;
import com.jme.input.KeyInput;
import com.jme.input.action.*;
import com.jme.input.action.InputActionEvent;

/**
 * Sets up camera controls for moving around from a top-down perspective,
 * suitable for strategy games and their ilk. The "ground" is assumed to be the
 * XY plane.
 */
public class GodViewHandler extends InputHandler
{
    /**
     * Creates the handler.
     *
     * @param camhand The camera to move with this handler.
     */
    public GodViewHandler (CameraHandler camhand)
    {
        _camhand = camhand;
        setKeyBindings();
        addActions();
    }

    protected void setKeyBindings ()
    {
        KeyBindingManager keyboard = KeyBindingManager.getKeyBindingManager();

        // the key bindings for the pan actions
        keyboard.set("forward", KeyInput.KEY_W);
        keyboard.set("arrow_forward", KeyInput.KEY_UP);
        keyboard.set("backward", KeyInput.KEY_S);
        keyboard.set("arrow_backward", KeyInput.KEY_DOWN);
        keyboard.set("left", KeyInput.KEY_A);
        keyboard.set("arrow_left", KeyInput.KEY_LEFT);
        keyboard.set("right", KeyInput.KEY_D);
        keyboard.set("arrow_right", KeyInput.KEY_RIGHT);

        // the key bindings for the zoom actions
        keyboard.set("zoomIn", KeyInput.KEY_UP);
        keyboard.set("zoomOut", KeyInput.KEY_DOWN);

        // the key bindings for the orbit actions
        keyboard.set("turnRight", KeyInput.KEY_RIGHT);
        keyboard.set("turnLeft", KeyInput.KEY_LEFT);

        // the key bindings for the tilt actions
        keyboard.set("tiltForward", KeyInput.KEY_HOME);
        keyboard.set("tiltBack", KeyInput.KEY_END);

        keyboard.set("screenshot", KeyInput.KEY_F12);
    }

    protected void addActions ()
    {
        addAction(new KeyScreenShotAction(), "screenshot", false);
        addPanActions();
        addZoomActions();
        addOrbitActions();
        addTiltActions();
    }

    /**
     * Adds actions for panning the camera around the scene.
     */
    protected void addPanActions ()
    {
        InputAction forward = new InputAction() {
            public void performAction (InputActionEvent evt) {
                _camhand.panCamera(0, speed * evt.getTime());
            }
        };
        forward.setSpeed(0.5f);
        addAction(forward, "forward", true);
        addAction(forward, "arrow_forward", true);

        InputAction backward = new InputAction() {
            public void performAction (InputActionEvent evt) {
                _camhand.panCamera(0, -speed * evt.getTime());
            }
        };
        backward.setSpeed(0.5f);
        addAction(backward, "backward", true);
        addAction(backward, "arrow_backward", true);

        InputAction left = new InputAction() {
            public void performAction (InputActionEvent evt) {
                _camhand.panCamera(-speed * evt.getTime(), 0);
            }
        };
        left.setSpeed(0.5f);
        addAction(left, "left", true);
        addAction(left, "arrow_left", true);

        InputAction right = new InputAction() {
            public void performAction (InputActionEvent evt) {
                _camhand.panCamera(speed * evt.getTime(), 0);
            }
        };
        right.setSpeed(0.5f);
        addAction(right, "right", true);
        addAction(right, "arrow_right", true);
    }

    /**
     * Adds actions for zooming the camaera in and out.
     */
    protected void addZoomActions ()
    {
        InputAction zoomIn = new InputAction() {
            public void performAction (InputActionEvent evt) {
                _camhand.zoomCamera(-speed * evt.getTime());
            }
        };
        zoomIn.setSpeed(0.5f);
        addAction(zoomIn, "zoomIn", true);

        InputAction zoomOut = new InputAction() {
            public void performAction (InputActionEvent evt) {
                _camhand.zoomCamera(speed * evt.getTime());
            }
        };
        zoomOut.setSpeed(0.5f);
        addAction(zoomOut, "zoomOut", true);
    }

    /**
     * Adds actions for orbiting the camera around the viewpoint.
     */
    protected void addOrbitActions ()
    {
        addAction(new OrbitAction(-FastMath.PI / 2), "turnRight", true);
        addAction(new OrbitAction(FastMath.PI / 2), "turnLeft", true);
    }

    /**
     * Adds actions for tilting the camera (rotating around the yaw axis).
     */
    protected void addTiltActions ()
    {
        addAction(new TiltAction(-FastMath.PI / 2), "tiltForward", true);
        addAction(new TiltAction(FastMath.PI / 2), "tiltBack", true);
    }

    protected class OrbitAction extends InputAction
    {
        public OrbitAction (float radPerSec)
        {
            _radPerSec = radPerSec;
        }

        public void performAction (InputActionEvent evt)
        {
            _camhand.orbitCamera(_radPerSec * evt.getTime());
        }

        protected float _radPerSec;
    }

    protected class TiltAction extends InputAction
    {
        public TiltAction (float radPerSec)
        {
            _radPerSec = radPerSec;
        }

        public void performAction (InputActionEvent evt)
        {
            _camhand.tiltCamera(_radPerSec * evt.getTime());
        }

        protected float _radPerSec;
    }

    protected CameraHandler _camhand;
}
