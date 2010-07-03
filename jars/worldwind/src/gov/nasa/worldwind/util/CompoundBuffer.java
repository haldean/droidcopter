/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.util;

import com.sun.opengl.util.BufferUtil;

import java.nio.IntBuffer;

/**
 * CompoundBuffer provides a mechanism for constructing a single buffer from a collection of buffers with potentially
 * different length, then indexing that 'compound' buffer to reference the individual buffer content within. Compounding
 * separate buffers in this way is useful for grouping related data into a single block of memory, which may then be
 * managed easlily, and easily tranferred from one system to another.
 *
 * @author dcollins
 * @version $Id: CompoundBuffer.java 13326 2010-04-22 02:28:38Z dcollins $
 * @param <T> The backing buffer type of this CompoundBuffer.
 * @param <V> The sub-buffer type accepted and returned by this CompoundBuffer.
 */
public abstract class CompoundBuffer<T, V>
{
    protected static final int DEFAULT_INITIAL_CAPACITY = 8;

    protected int size;
    protected int numSubBuffers;
    protected T buffer;
    protected IntBuffer subPositionBuffer;
    protected IntBuffer subLengthBuffer;

    /**
     * Constructs a CompoundBuffer with the specified initial capacity, and the specified backing buffer.
     *
     * @param initialCapacity the initial capacity to allocate, in number of sub-buffers.
     * @param buffer          the backing buffer.
     *
     * @throws IllegalArgumentException if the initialCapacity is negative, or if the buffer is null.
     */
    public CompoundBuffer(int initialCapacity, T buffer)
    {
        if (initialCapacity < 0)
        {
            String message = Logging.getMessage("generic.SizeOutOfRange", initialCapacity);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (buffer == null)
        {
            String message = Logging.getMessage("nullValue.BufferIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.size = 0;
        this.numSubBuffers = 0;
        this.subPositionBuffer = BufferUtil.newIntBuffer(initialCapacity);
        this.subLengthBuffer = BufferUtil.newIntBuffer(initialCapacity);
        this.buffer = buffer;
    }

    /**
     * Creates a CompoundBuffer with the specified backing buffer size, backing buffer, sub-buffer starting positions,
     * sub-buffer lengths, and number of sub-buffers.
     *
     * @param size              the backing buffer size.
     * @param buffer            the backing buffer,
     * @param subPositionBuffer the sub-buffer logical starting positions.
     * @param subLengthBuffer   the sub-buffer logical sizes.
     * @param numSubBuffers     the number of sub-buffers.
     *
     * @throws IllegalArgumentException if size is negative, if any of the buffer, subPositionBuffer, subLengthBuffer
     *                                  are null, or if numSubBuffers is negative.
     */
    public CompoundBuffer(int size, T buffer, IntBuffer subPositionBuffer, IntBuffer subLengthBuffer, int numSubBuffers)
    {
        if (size < 0)
        {
            String message = Logging.getMessage("generic.SizeOutOfRange", size);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (buffer == null)
        {
            String message = Logging.getMessage("nullValue.BufferIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (subPositionBuffer == null)
        {
            String message = Logging.getMessage("nullValue.PositionsBufferIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (subLengthBuffer == null)
        {
            String message = Logging.getMessage("nullValue.LengthsBufferIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (numSubBuffers < 0)
        {
            String message = Logging.getMessage("generic.ArgumentOutOfRange", "numSubBuffers = " + numSubBuffers);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.size = size;
        this.numSubBuffers = numSubBuffers;
        this.subPositionBuffer = subPositionBuffer;
        this.subLengthBuffer = subLengthBuffer;
        this.buffer = buffer;
    }

    /**
     * Constructs a CompoundBuffer with the specified backing buffer, and a default initial capacity.
     *
     * @param buffer the compound buffer's backing buffer.
     *
     * @throws IllegalArgumentException if the buffer is null.
     */
    public CompoundBuffer(T buffer)
    {
        this(DEFAULT_INITIAL_CAPACITY, buffer);
    }

    /**
     * Returns the collective size of all sub buffers.
     *
     * @return the size of all sub buffers.
     */
    public int getTotalBufferSize()
    {
        return this.size;
    }

    /**
     * Returns the number of sub-buffers contained in the compound buffer.
     *
     * @return the number of sub-buffers.
     */
    public int getNumSubBuffers()
    {
        return this.numSubBuffers;
    }

    /**
     * Returns the backing buffer.
     *
     * @return the backing buffer.
     */
    public T getBackingBuffer()
    {
        return this.buffer;
    }

    /**
     * Returns the buffer which contains the logical starting position of each sub-buffer in the backing buffer.
     *
     * @return the buffer of sub-buffer logical starting positions.
     */
    public IntBuffer getSubPositionBuffer()
    {
        return this.subPositionBuffer;
    }

    /**
     * Returns the buffer which contains the logical size of each sub-buffer in the backing buffer.
     *
     * @return the buffer of sub-buffer logical sizes.
     */
    public IntBuffer getSubLengthBuffer()
    {
        return this.subLengthBuffer;
    }

    /**
     * Returns the sub-buffer identified by the specified index. Index i corresponds to the ith sub-buffer.
     *
     * @param index the index of the sub-buffer to get.
     *
     * @return the sub-buffer at the specified index.
     *
     * @throws IllegalArgumentException if the index is out of range.
     */
    public V getSubBuffer(int index)
    {
        if (index < 0 || index >= this.numSubBuffers)
        {
            String message = Logging.getMessage("generic.indexOutOfRange", index);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        int position = this.subPositionBuffer.get(index);
        int length = this.subLengthBuffer.get(index);
        return this.getSubBuffer(position, length);
    }

    /**
     * Returns a subset of this CompoundBuffer. The returned buffer has length <code>endIndex - beginIndex + 1</code>
     * and references this buffer's contents starting at <code>beginIndex</code>, and ending at <code>endIndex</code>.
     * The returned buffer shares this buffers's backing data. Changes to this buffer are reflected in the returned
     * buffer, and vice versa.
     *
     * @param beginIndex the index of the first sub-buffer to include in the subset.
     * @param endIndex   the index of the last sub-buffer to include in the subset.
     *
     * @return a new CompoundBuffer representing a subset of this CompoundBuffer.
     *
     * @throws IllegalArgumentException if beginIndex is out of range, if endIndex is out of range, or if beginIndex >
     *                                  endIndex.
     */
    public CompoundBuffer subCollection(int beginIndex, int endIndex)
    {
        if (beginIndex < 0 || beginIndex >= this.numSubBuffers)
        {
            String message = Logging.getMessage("generic.indexOutOfRange", beginIndex);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (endIndex < 0 || endIndex >= this.numSubBuffers)
        {
            String message = Logging.getMessage("generic.indexOutOfRange", endIndex);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (beginIndex > endIndex)
        {
            String message = Logging.getMessage("generic.indexOutOfRange", beginIndex);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        int count = endIndex - beginIndex + 1;
        IntBuffer positions = BufferUtil.newIntBuffer(count);
        IntBuffer lengths = BufferUtil.newIntBuffer(count);

        // Copy the sub-buffer position values in the range [beginIndex, endIndex].
        BufferStateHandler bsh = new BufferStateHandler();
        bsh.pushState(this.subPositionBuffer);
        try
        {
            this.subPositionBuffer.position(beginIndex);
            this.subPositionBuffer.limit(endIndex + 1);
            positions.put(this.subPositionBuffer);
            positions.rewind();
        }
        finally
        {
            bsh.popState(this.subPositionBuffer);
        }

        // Copy the sub-buffer length values in the range [beginIndex, endIndex].
        bsh.pushState(this.subLengthBuffer);
        try
        {
            this.subLengthBuffer.position(beginIndex);
            this.subLengthBuffer.limit(endIndex + 1);
            lengths.put(this.subLengthBuffer);
            lengths.rewind();
        }
        finally
        {
            bsh.popState(this.subLengthBuffer);
        }

        return this.createSubCollection(this, positions, lengths, count);
    }

    /**
     * Returns a subset of this CompoundBuffer. The returned buffer's length is equal to the length of
     * <code>indices</code>, and contains this buffer's contents for each index in <code>indices</code>. The returned
     * buffer shares this buffers's backing data. Changes to this buffer are reflected in the returned buffer, and vice
     * versa.
     *
     * @param indices an array containing the indices include in the subset.
     *
     * @return a new CompoundBuffer representing a subset of this CompoundBuffer.
     *
     * @throws IllegalArgumentException if the array of indices is null, or if any of the indices is out of range.
     */
    public CompoundBuffer subCollection(int[] indices)
    {
        if (indices == null)
        {
            String message = Logging.getMessage("nullValue.ArrayIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        int count = indices.length;
        IntBuffer positions = BufferUtil.newIntBuffer(count);
        IntBuffer lengths = BufferUtil.newIntBuffer(count);

        // Copy sub buffer start position and length within the source buffer.
        for (int i = 0; i < count; i++)
        {
            if (indices[i] < 0 || indices[i] >= this.numSubBuffers)
            {
                String message = Logging.getMessage("generic.indexOutOfRange", indices[i]);
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }

            positions.put(this.subPositionBuffer.get(indices[i]));
            lengths.put(this.subLengthBuffer.get(indices[i]));
        }

        positions.rewind();
        lengths.rewind();

        return this.createSubCollection(this, positions, lengths, count);
    }

    /**
     * Appends an empty sub-buffer to the end of this compound buffer.
     *
     * @return the new sub-buffer's index.
     */
    public int appendEmptyBuffer()
    {
        int index = this.numSubBuffers;

        this.subPositionBuffer.put(index, this.size);
        this.subLengthBuffer.put(index, 0);
        this.numSubBuffers++;

        return index;
    }

    /**
     * Appends the contents of the specified sub-buffer to the end of this compound buffer, incrementing the number of
     * sub-buffers by one. The backing buffer grows to accomodate the sub-buffer if it does not already have enough
     * capacity to hold it. The relative logical starting position and length of the new sub-buffer are stored in this
     * buffer, and may be recalled by either manually retrieving them from the buffers returned by #getSubPositionBuffer
     * and #getSubLengthBuffer, or by calling #getSubBuffer.
     *
     * @param subBuffer the sub-buffer to append.
     *
     * @return the new sub-buffer's index.
     *
     * @throws IllegalArgumentException if the subBuffer is null.
     */
    public int appendSubBuffer(V subBuffer)
    {
        if (subBuffer == null)
        {
            String message = Logging.getMessage("nullValue.BufferIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        int minSize = 1 + this.numSubBuffers;
        if (this.subPositionBuffer.remaining() < minSize)
        {
            int newSize = this.computeNewSize(this.subPositionBuffer.remaining(), minSize);
            this.subPositionBuffer = WWBufferUtil.copyOf(this.subPositionBuffer, newSize);
            this.subLengthBuffer = WWBufferUtil.copyOf(this.subLengthBuffer, newSize);
        }

        int thatSize = this.getSize(subBuffer);
        int index = this.numSubBuffers;

        this.append(subBuffer);
        this.subPositionBuffer.put(index, this.size);
        this.subLengthBuffer.put(index, thatSize);
        this.size += thatSize;
        this.numSubBuffers++;

        return index;
    }

    /**
     *
     */
    public void clear()
    {
        this.size = 0;
        this.numSubBuffers = 0;
    }

    protected int computeNewSize(int size, int minSize)
    {
        int newSize = size;
        while (newSize < minSize)
        {
            newSize = (3 * minSize) / 2;
        }
        return newSize;
    }

    protected abstract int getSize(V buffer);

    protected abstract V getSubBuffer(int position, int length);

    protected abstract CompoundBuffer<T, V> createSubCollection(CompoundBuffer<T, V> source,
        IntBuffer subPositionBuffer, IntBuffer subLengthBuffer, int numSubBuffers);

    protected abstract void append(V buffer);
}
