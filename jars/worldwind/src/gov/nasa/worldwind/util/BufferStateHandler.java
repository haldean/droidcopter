/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.util;

import java.nio.Buffer;
import java.util.ArrayList;

/**
 * BufferStateHandler is a utility class which encapsulates the common pattern of saving and restoring the essential
 * mutable properties of a {@link Buffer}: its position and limit.
 * <p/>
 * BufferStateHandler saves and restores Buffer properties on an internal stack, enabling the caller to save and restore
 * a nested series of Buffer states. Here is a typical usage pattern for BufferStateHandler:
 * <p/>
 * <code> Buffer myBuffer;<br/> BufferStateHandler bsh = new BufferStateHandler();<br/> bsh.pushState(myBuffer);<br/>
 * try <br/> {<br/> &nbsp&nbsp// Perform the desired operations on myBuffer.<br/> }<br/> finally<br/> {<br/>
 * &nbsp&nbspbsh.popState(myBuffer);<br/> }<br/> </code>
 *
 * @author dcollins
 * @version $Id: BufferStateHandler.java 12803 2009-11-18 03:23:02Z dcollins $
 */
public class BufferStateHandler
{
    protected static class BufferState
    {
        public final int position;
        public final int limit;

        public BufferState(int position, int limit)
        {
            this.position = position;
            this.limit = limit;
        }
    }

    protected ArrayList<BufferState> stack = new ArrayList<BufferState>();

    /** Constructs a new BufferStateHandler, initializing its internal stack. */
    public BufferStateHandler()
    {
    }

    /**
     * Push the specified buffer's state to the top of this BufferStateHandler's internal stack, incrementing the stack
     * size by one.
     *
     * @param buffer the buffer who's state is saved to the top of the stack.
     *
     * @throws IllegalArgumentException if buffer is null.
     */
    public void pushState(Buffer buffer)
    {
        if (buffer == null)
        {
            String message = Logging.getMessage("nullValue.BufferIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        BufferState state = new BufferState(buffer.position(), buffer.limit());
        this.stack.add(state);
    }

    /**
     * Pop the state off the top of this BufferStateHandler's internal stack and apply it to the specified buffer's,
     * decrementing the stack size by one. If the stack is empty, this returns false.
     *
     * @param buffer the buffer who's state is restored from the top of the stack.
     *
     * @return true if the stack had at least one state to pop, and false otherwise.
     *
     * @throws IllegalArgumentException if buffer is null.
     */
    public boolean popState(Buffer buffer)
    {
        if (buffer == null)
        {
            String message = Logging.getMessage("nullValue.BufferIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (this.stack.isEmpty())
            return false;

        BufferState state = this.stack.remove(this.stack.size() - 1);
        if (state == null)
            return false;

        buffer.position(state.position);
        buffer.limit(state.limit);
        return true;
    }
}
