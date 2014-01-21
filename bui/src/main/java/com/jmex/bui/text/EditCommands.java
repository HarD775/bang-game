//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui.text;

/**
 * Defines the various commands handled by our text editing components.
 */
public interface EditCommands
{
    /** A text editing command. */
    public static final int ACTION = 0;

    /** A text editing command. */
    public static final int BACKSPACE = 1;

    /** A text editing command. */
    public static final int DELETE = 2;

    /** A text editing command. */
    public static final int CURSOR_LEFT = 3;

    /** A text editing command. */
    public static final int CURSOR_RIGHT = 4;

    /** A text editing command. */
    public static final int START_OF_LINE = 5;

    /** A text editing command. */
    public static final int END_OF_LINE = 6;

    /** A text editing command. */
    public static final int RELEASE_FOCUS = 7;

    /** A text editing command. */
    public static final int CLEAR = 8;
}
