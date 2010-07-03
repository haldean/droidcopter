/* Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package WorldWindHackApps.elevationviewer;

import gov.nasa.worldwind.geom.Vec4;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

/**
 * @author dcollins
 * @version $Id: PolledInputAdapter.java 13023 2010-01-21 00:18:48Z dcollins $
 */
public class PolledInputAdapter implements KeyListener, MouseListener, MouseMotionListener
{
    public static class KeyState
    {
        private int keyCode;
        private int state;
        private int modifiers;
        private long when;

        public KeyState(int keyCode, int state, int modifiers, long when)
        {
            this.keyCode = keyCode;
            this.state = state;
            this.modifiers = modifiers;
            this.when = when;
        }

        public boolean isKeyDown()
        {
            return this.state == KeyEvent.KEY_PRESSED;   
        }

        public int getKeyCode()
        {
            return this.keyCode;
        }

        public int getState()
        {
            return this.state;
        }

        public int getModifiers()
        {
            return this.modifiers;
        }

        public long getWhen()
        {
            return this.when;
        }
    }

    private Map<Object, KeyState> keyMap = new HashMap<Object, KeyState>();
    private Vec4 mousePoint;
    private Vec4 mouseMove = Vec4.ZERO;
    private int mouseModifiers;
    private static PolledInputAdapter instance;

    private PolledInputAdapter()
    {
    }

    public static PolledInputAdapter getInstance()
    {
        if (instance == null)
            instance = new PolledInputAdapter();
        return instance;
    }

    public static void attachTo(Component c)
    {
        c.addKeyListener(getInstance());
        c.addMouseListener(getInstance());
        c.addMouseMotionListener(getInstance());
    }

    public void update()
    {
        this.mouseMove = Vec4.ZERO;
    }

    public boolean isKeyDown(int keyCode)
    {
        KeyState state = this.getKeyState(keyCode);
        return state != null && state.isKeyDown();
    }

    public KeyState getKeyState(int keyCode)
    {
        return this.keyMap.get(keyCode);
    }

    public Vec4 getMousePoint()
    {
        return this.mousePoint;
    }

    public Vec4 getMouseMovement()
    {
        return this.mouseMove;
    }

    public int getMouseModifiers()
    {
        return this.mouseModifiers;
    }

    public void keyTyped(KeyEvent e)
    {
    }

    public void keyPressed(KeyEvent e)
    {
        this.onKeyEvent(e, KeyEvent.KEY_PRESSED);
    }

    public void keyReleased(KeyEvent e)
    {
        this.onKeyEvent(e, KeyEvent.KEY_RELEASED);        
    }

    public void mouseClicked(MouseEvent e)
    {
        this.onMouseEvent(e);
    }

    public void mousePressed(MouseEvent e)
    {
        this.onMouseEvent(e);
    }

    public void mouseReleased(MouseEvent e)
    {
        this.onMouseEvent(e);
    }

    public void mouseEntered(MouseEvent e)
    {
        this.onMouseEvent(e);
    }

    public void mouseExited(MouseEvent e)
    {
        this.onMouseEvent(e);
    }

    public void mouseDragged(MouseEvent e)
    {
        this.onMouseEvent(e);
    }

    public void mouseMoved(MouseEvent e)
    {
        this.onMouseEvent(e);
    }

    private void onKeyEvent(KeyEvent e, int state)
    {
        int keyCode = e.getKeyCode();
        KeyState keyState = this.keyMap.get(keyCode);
        long when = (keyState != null && keyState.getState() == state) ? keyState.getWhen() : e.getWhen();

        keyState = new KeyState(keyCode, state, e.getModifiers(), when);
        this.keyMap.put(keyCode, keyState);
    }

    private void onMouseEvent(MouseEvent e)
    {
        this.mouseModifiers = e.getModifiers();

        if (this.mousePoint != null)
        {
            this.mouseMove = new Vec4(e.getX() - this.mousePoint.x, e.getY() - this.mousePoint.y, 0);
        }
        else
        {
            this.mouseMove = new Vec4(0, 0, 0);
        }

        this.mousePoint = new Vec4(e.getX(), e.getY(), 0);
    }
}
