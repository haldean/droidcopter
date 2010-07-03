/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.util;

import java.nio.IntBuffer;

/**
 * An implementation of {@link CompoundBuffer} which manages a collection of {@link CharSequence} buffers in a single
 * {@link StringBuilder}.
 *
 * @author dcollins
 * @version $Id: CompoundStringBuffer.java 13324 2010-04-21 23:21:37Z dcollins $
 */
public class CompoundStringBuffer extends CompoundBuffer<StringBuilder, CharSequence>
{
    /**
     * Constructs a CompoundStringBuffer with the specified initial capacity.
     *
     * @param initialCapacity   the compound buffer's initial capacity.
     * @param subSequenceLength an estimate of the size of each sub-sequence.
     *
     * @throws IllegalArgumentException if the initialCapacity is negative.
     */
    public CompoundStringBuffer(int initialCapacity, int subSequenceLength)
    {
        super(initialCapacity, new StringBuilder(initialCapacity * subSequenceLength));
    }

    /**
     * Constructs a CompoundStringBuffer with the specified backing buffer, sub-buffer starting positions, sub-buffer
     * lengths, and number of sub-buffers.
     *
     * @param buffer            the backing buffer,
     * @param subPositionBuffer the sub-buffer logical starting positions.
     * @param subLengthBuffer   the sub-buffer logical sizes.
     * @param numSubBuffers     the number of sub-buffers.
     *
     * @throws IllegalArgumentException if any of the buffer, subPositionBuffer, subLengthBuffer are null, if
     *                                  numSubBuffers is negative.
     */
    public CompoundStringBuffer(StringBuilder buffer, IntBuffer subPositionBuffer, IntBuffer subLengthBuffer,
        int numSubBuffers)
    {
        super(buffer.length(), buffer, subPositionBuffer, subLengthBuffer, numSubBuffers);
    }

    /**
     * Returns the substring identified by the specified index. Index i corresponds to the ith string.
     *
     * @param index the index of the string to get.
     *
     * @return the string at the specified index.
     *
     * @throws IllegalArgumentException if the index is out of range.
     */
    public String getSubString(int index)
    {
        CharSequence subSequence = this.getSubBuffer(index);
        return subSequence.toString();
    }

    protected int getSize(CharSequence buffer)
    {
        return buffer.length();
    }

    protected CharSequence getSubBuffer(int position, int length)
    {
        CharSequence subSequence = this.buffer.subSequence(position, position + length);
        return WWUtil.trimCharSequence(subSequence);
    }

    protected CompoundBuffer<StringBuilder, CharSequence> createSubCollection(
        CompoundBuffer<StringBuilder, CharSequence> source, IntBuffer subPositionBuffer, IntBuffer subLengthBuffer,
        int numSubBuffers)
    {
        return new CompoundStringBuffer(source.getBackingBuffer(), subPositionBuffer, subLengthBuffer, numSubBuffers);
    }

    protected void append(CharSequence buffer)
    {
        this.buffer.append(buffer);
    }
}
