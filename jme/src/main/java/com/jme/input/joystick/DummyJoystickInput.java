/*
 * Copyright (c) 2003-2006 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors 
 *   may be used to endorse or promote products derived from this software 
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.jme.input.joystick;

import java.util.ArrayList;

import com.jme.util.LoggingSystem;

/**
 * Dummy JoystickInput to disable joystick support.
 */
public class DummyJoystickInput extends JoystickInput {
    private DummyJoystick dummyJoystick = new DummyJoystick();

    public DummyJoystickInput() {
        LoggingSystem.getLogger().info( "Joystick support is disabled");
    }

    /**
     * @return number of attached game controllers
     */
    @Override
	public int getJoystickCount() {
        return 0;
    }

    /**
     * Game controller at specified index.
     *
     * @param index index of the controller (0 <= index <= {@link #getJoystickCount()})
     * @return game controller
     */
    @Override
	public Joystick getJoystick( int index ) {
        return null;
    }

    @Override
    public ArrayList<Joystick> findJoysticksByAxis(String... axis) {
        return null;
    }
    
    /**
     * This is a method to obtain a single joystick. It's simple to used but not
     * recommended (user may have multiple joysticks!).
     *
     * @return what the implementation thinks is the main joystick, not null!
     */
    @Override
	public Joystick getDefaultJoystick() {
        return dummyJoystick;
    }

    @Override
	protected void destroy() {

    }

    /**
     * Poll data for this input system part (update the values) and send events to all listeners
     * (events will not be generated if no listeners were added via addListener).
     */
    @Override
	public void update() {

    }

    public static class DummyJoystick implements Joystick {
        @Override
		public void rumble( int axis, float intensity ) {
        }

        @Override
		public String[] getAxisNames() {
            return new String[0];
        }

        @Override
		public int getAxisCount() {
            return 0;
        }

        @Override
		public float getAxisValue( int axis ) {
            return 0;
        }

        @Override
		public int getButtonCount() {
            return 0;
        }

        @Override
		public boolean isButtonPressed( int button ) {
            return false;
        }

        @Override
		public String getName() {
            return "Dummy";
        }

        public void setDeadZone( int axis, float value ) {

        }

        @Override
		public int findAxis(String name) {
            return -1;
        }
    }

}
