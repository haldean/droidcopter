/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.util;

import com.sun.opengl.util.BufferUtil;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.formats.worldfile.WorldFile;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.coords.UTMCoord;

import java.nio.*;

/**
 * A collection of useful {@link Buffer} methods, all static.
 *
 * @author dcollins
 * @version $Id: WWBufferUtil.java 13299 2010-04-13 03:08:12Z dcollins $
 */
public class WWBufferUtil
{
    /** The size of a char primitive type, in bytes. */
    public static final int SIZEOF_CHAR = 2;

    /**
     * Allocates a new direct {@link CharBuffer} of the specified size, in chars.
     *
     * @param size the new buffer's size.
     *
     * @return the new buffer.
     *
     * @throws IllegalArgumentException if size is negative.
     */
    public static CharBuffer newCharBuffer(int size)
    {
        if (size < 0)
        {
            String message = Logging.getMessage("generic.SizeOutOfRange", size);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(SIZEOF_CHAR * size).order(ByteOrder.nativeOrder());
        return byteBuffer.asCharBuffer();
    }

    /**
     * Allocates a new {@link BufferWrapper} of the specified size, in bytes. The BufferWrapper is backed by a Buffer of
     * bytes.
     *
     * @param size           the new BufferWrapper's size.
     * @param allocateDirect true to allocate and return a direct buffer, false to allocate and return a non-direct
     *                       buffer.
     *
     * @return the new BufferWrapper.
     *
     * @throws IllegalArgumentException if size is negative.
     */
    public static BufferWrapper newByteBufferWrapper(int size, boolean allocateDirect)
    {
        if (size < 0)
        {
            String message = Logging.getMessage("generic.SizeOutOfRange", size);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        ByteBuffer buffer = (allocateDirect ? BufferUtil.newByteBuffer(size) : ByteBuffer.allocate(size));
        return new BufferWrapper.ByteBufferWrapper(buffer);
    }

    /**
     * Allocates a new {@link BufferWrapper} of the specified size, in shorts. The BufferWrapper is backed by a Buffer
     * of shorts.
     *
     * @param size           the new BufferWrapper's size.
     * @param allocateDirect true to allocate and return a direct buffer, false to allocate and return a non-direct
     *                       buffer.
     *
     * @return the new BufferWrapper.
     *
     * @throws IllegalArgumentException if size is negative.
     */
    public static BufferWrapper newShortBufferWrapper(int size, boolean allocateDirect)
    {
        if (size < 0)
        {
            String message = Logging.getMessage("generic.SizeOutOfRange", size);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        ShortBuffer buffer = (allocateDirect ? BufferUtil.newShortBuffer(size) : ShortBuffer.allocate(size));
        return new BufferWrapper.ShortBufferWrapper(buffer);
    }

    /**
     * Allocates a new {@link BufferWrapper} of the specified size, in ints. The BufferWrapper is backed by a Buffer of
     * ints.
     *
     * @param size           the new BufferWrapper's size.
     * @param allocateDirect true to allocate and return a direct buffer, false to allocate and return a non-direct
     *                       buffer.
     *
     * @return the new BufferWrapper.
     *
     * @throws IllegalArgumentException if size is negative.
     */
    public static BufferWrapper newIntBufferWrapper(int size, boolean allocateDirect)
    {
        if (size < 0)
        {
            String message = Logging.getMessage("generic.SizeOutOfRange", size);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        IntBuffer buffer = (allocateDirect ? BufferUtil.newIntBuffer(size) : IntBuffer.allocate(size));
        return new BufferWrapper.IntBufferWrapper(buffer);
    }

    /**
     * Allocates a new {@link BufferWrapper} of the specified size, in floats. The BufferWrapper is backed by a Buffer
     * of floats.
     *
     * @param size           the new BufferWrapper's size.
     * @param allocateDirect true to allocate and return a direct buffer, false to allocate and return a non-direct
     *                       buffer.
     *
     * @return the new BufferWrapper.
     *
     * @throws IllegalArgumentException if size is negative.
     */
    public static BufferWrapper newFloatBufferWrapper(int size, boolean allocateDirect)
    {
        if (size < 0)
        {
            String message = Logging.getMessage("generic.SizeOutOfRange", size);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        FloatBuffer buffer = (allocateDirect ? BufferUtil.newFloatBuffer(size) : FloatBuffer.allocate(size));
        return new BufferWrapper.FloatBufferWrapper(buffer);
    }

    /**
     * Allocates a new {@link BufferWrapper} of the specified size, in doubles. The BufferWrapper is backed by a Buffer
     * of doubles.
     *
     * @param size           the new BufferWrapper's size.
     * @param allocateDirect true to allocate and return a direct buffer, false to allocate and return a non-direct
     *                       buffer.
     *
     * @return the new BufferWrapper.
     *
     * @throws IllegalArgumentException if size is negative.
     */
    public static BufferWrapper newDoubleBufferWrapper(int size, boolean allocateDirect)
    {
        if (size < 0)
        {
            String message = Logging.getMessage("generic.SizeOutOfRange", size);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        DoubleBuffer buffer = (allocateDirect ? BufferUtil.newDoubleBuffer(size) : DoubleBuffer.allocate(size));
        return new BufferWrapper.DoubleBufferWrapper(buffer);
    }

    /**
     * Returns a copy of the specified buffer, with the specified new size. The new size must be greater than or equal
     * to the specified buffer's size. If the new size is greater than the specified buffer's size, this returns a new
     * buffer which is partially filled with the contents of the specified buffer.
     *
     * @param buffer  the buffer to copy.
     * @param newSize the new buffer's size, in bytes.
     *
     * @return the new buffer, with the specified size.
     *
     * @throws IllegalArgumentException if the buffer is null, if the new size is negative, or if the new size is less
     *                                  than the buffer's remaing elements.
     */
    public static ByteBuffer copyOf(ByteBuffer buffer, int newSize)
    {
        if (newSize < 0 || newSize < buffer.remaining())
        {
            String message = Logging.getMessage("generic.SizeOutOfRange", newSize);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        ByteBuffer newBuffer = BufferUtil.newByteBuffer(newSize);

        BufferStateHandler bsh = new BufferStateHandler();
        bsh.pushState(buffer);
        try
        {
            newBuffer.put(buffer);
            newBuffer.rewind();
        }
        finally
        {
            bsh.popState(buffer);
        }

        return newBuffer;
    }

    /**
     * Returns a copy of the specified buffer, with the specified new size. The new size must be greater than or equal
     * to the specified buffer's size. If the new size is greater than the specified buffer's size, this returns a new
     * buffer which is partially filled with the contents of the specified buffer.
     *
     * @param buffer  the buffer to copy.
     * @param newSize the new buffer's size, in chars.
     *
     * @return the new buffer, with the specified size.
     *
     * @throws IllegalArgumentException if the buffer is null, if the new size is negative, or if the new size is less
     *                                  than the buffer's remaing elements.
     */
    public static CharBuffer copyOf(CharBuffer buffer, int newSize)
    {
        if (newSize < 0 || newSize < buffer.remaining())
        {
            String message = Logging.getMessage("generic.SizeOutOfRange", newSize);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        CharBuffer newBuffer = newCharBuffer(newSize);

        BufferStateHandler bsh = new BufferStateHandler();
        bsh.pushState(buffer);
        try
        {
            newBuffer.put(buffer);
            newBuffer.rewind();
        }
        finally
        {
            bsh.popState(buffer);
        }

        return newBuffer;
    }

    /**
     * Returns a copy of the specified buffer, with the specified new size. The new size must be greater than or equal
     * to the specified buffer's size. If the new size is greater than the specified buffer's size, this returns a new
     * buffer which is partially filled with the contents of the specified buffer.
     *
     * @param buffer  the buffer to copy.
     * @param newSize the new buffer's size, in shorts.
     *
     * @return the new buffer, with the specified size.
     *
     * @throws IllegalArgumentException if the buffer is null, if the new size is negative, or if the new size is less
     *                                  than the buffer's remaing elements.
     */
    public static ShortBuffer copyOf(ShortBuffer buffer, int newSize)
    {
        if (newSize < 0 || newSize < buffer.remaining())
        {
            String message = Logging.getMessage("generic.SizeOutOfRange", newSize);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        ShortBuffer newBuffer = BufferUtil.newShortBuffer(newSize);

        BufferStateHandler bsh = new BufferStateHandler();
        bsh.pushState(buffer);
        try
        {
            newBuffer.put(buffer);
            newBuffer.rewind();
        }
        finally
        {
            bsh.popState(buffer);
        }

        return newBuffer;
    }

    /**
     * Returns a copy of the specified buffer, with the specified new size. The new size must be greater than or equal
     * to the specified buffer's size. If the new size is greater than the specified buffer's size, this returns a new
     * buffer which is partially filled with the contents of the specified buffer.
     *
     * @param buffer  the buffer to copy.
     * @param newSize the new buffer's size, in ints.
     *
     * @return the new buffer, with the specified size.
     *
     * @throws IllegalArgumentException if the buffer is null, if the new size is negative, or if the new size is less
     *                                  than the buffer's remaing elements.
     */
    public static IntBuffer copyOf(IntBuffer buffer, int newSize)
    {
        if (newSize < 0 || newSize < buffer.remaining())
        {
            String message = Logging.getMessage("generic.SizeOutOfRange", newSize);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        IntBuffer newBuffer = BufferUtil.newIntBuffer(newSize);

        BufferStateHandler bsh = new BufferStateHandler();
        bsh.pushState(buffer);
        try
        {
            newBuffer.put(buffer);
            newBuffer.rewind();
        }
        finally
        {
            bsh.popState(buffer);
        }

        return newBuffer;
    }

    /**
     * Returns a copy of the specified buffer, with the specified new size. The new size must be greater than or equal
     * to the specified buffer's size. If the new size is greater than the specified buffer's size, this returns a new
     * buffer which is partially filled with the contents of the specified buffer.
     *
     * @param buffer  the buffer to copy.
     * @param newSize the new buffer's size, in floats.
     *
     * @return the new buffer, with the specified size.
     *
     * @throws IllegalArgumentException if the buffer is null, if the new size is negative, or if the new size is less
     *                                  than the buffer's remaing elements.
     */
    public static FloatBuffer copyOf(FloatBuffer buffer, int newSize)
    {
        if (newSize < 0 || newSize < buffer.remaining())
        {
            String message = Logging.getMessage("generic.SizeOutOfRange", newSize);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        FloatBuffer newBuffer = BufferUtil.newFloatBuffer(newSize);

        BufferStateHandler bsh = new BufferStateHandler();
        bsh.pushState(buffer);
        try
        {
            newBuffer.put(buffer);
            newBuffer.rewind();
        }
        finally
        {
            bsh.popState(buffer);
        }

        return newBuffer;
    }

    /**
     * Returns a copy of the specified buffer, with the specified new size. The new size must be greater than or equal
     * to the specified buffer's size. If the new size is greater than the specified buffer's size, this returns a new
     * buffer which is partially filled with the contents of the specified buffer.
     *
     * @param buffer  the buffer to copy.
     * @param newSize the new buffer's size, in doubles.
     *
     * @return the new buffer, with the specified size.
     *
     * @throws IllegalArgumentException if the buffer is null, if the new size is negative, or if the new size is less
     *                                  than the buffer's remaing elements.
     */
    public static DoubleBuffer copyOf(DoubleBuffer buffer, int newSize)
    {
        if (newSize < 0 || newSize < buffer.remaining())
        {
            String message = Logging.getMessage("generic.SizeOutOfRange", newSize);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        DoubleBuffer newBuffer = BufferUtil.newDoubleBuffer(newSize);

        BufferStateHandler bsh = new BufferStateHandler();
        bsh.pushState(buffer);
        try
        {
            newBuffer.put(buffer);
            newBuffer.rewind();
        }
        finally
        {
            bsh.popState(buffer);
        }

        return newBuffer;
    }

    /**
     * Returns a copy of the specified buffer, with the specified new size. The new size must be greater than or equal
     * to the specified buffer's size. If the new size is greater than the specified buffer's size, this returns a new
     * buffer which is partially filled with the contents of the specified buffer.
     *
     * @param buffer  the buffer to copy.
     * @param newSize the new buffer's size, in doubles.
     * @param factory the factory used to create the new buffer.
     *
     * @return the new buffer, with the specified size.
     *
     * @throws IllegalArgumentException if the buffer is null, if the new size is negative, or if the new size is less
     *                                  than the buffer's remaing elements.
     */
    public static BufferWrapper copyOf(BufferWrapper buffer, int newSize, BufferFactory factory)
    {
        if (buffer == null)
        {
            String message = Logging.getMessage("nullValue.BufferIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (newSize < 0 || newSize < buffer.length())
        {
            String message = Logging.getMessage("generic.SizeOutOfRange", newSize);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (factory == null)
        {
            String message = Logging.getMessage("nullValue.FactoryIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        BufferWrapper newBuffer = factory.newBuffer(newSize);
        newBuffer.putSubBuffer(0, buffer);
        return newBuffer;
    }

    /**
     * Converts the specified VecBuffer of projected tuples to geographic locations, according to the specified
     * projection parameters. The tuples are assumed to be non-geographic. This converts each projected tuple to a
     * geographic location, then replaces the first two coordinates of each tuple with the geographic location using the
     * same coordinate layout as {@link gov.nasa.worldwind.util.VecBuffer#putLocation(int,
     * gov.nasa.worldwind.geom.LatLon)}. If the specified parameter list describes a UTM projection, this is equivalent
     * to calling {@link #convertUTMTuplesToGeographic(VecBuffer, gov.nasa.worldwind.avlist.AVList)}.
     *
     * @param buffer the VecBuffer of projected tuples.
     * @param params parameter list describing the projection.
     *
     * @throws IllegalArgumentException if either the buffer or the parameter list is null.
     */
    public static void convertProjectedTuplesToGeographic(VecBuffer buffer, AVList params)
    {
        if (buffer == null)
        {
            String message = Logging.getMessage("nullValue.BufferIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (params == null)
        {
            String message = Logging.getMessage("nullValue.ParamsIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        String projectionCode = (String) params.getValue(WorldFile.WORLD_FILE_PROJECTION);
        if (projectionCode == null)
            return;

        if (projectionCode.equalsIgnoreCase("utm"))
        {
            convertUTMTuplesToGeographic(buffer, params);
        }
    }

    /**
     * Converts the specified VecBuffer of UTM tuples to geographic locations, according to the specified UTM projection
     * parameters. This assumes each tuple has at least two coordinates defining the UTM easting and northing, both in
     * meters. This converts each UTM tuple in the specified buffer to a geographic location, then replaces the first
     * two coordinates of each tuple with the geographic location using the same coordinate layout as {@link
     * gov.nasa.worldwind.util.VecBuffer#putLocation(int, gov.nasa.worldwind.geom.LatLon)}. The parameter list must
     * define the UTM zone and hemisphere in the parameters {@link gov.nasa.worldwind.formats.worldfile.WorldFile#WORLD_FILE_ZONE}
     * and {@link gov.nasa.worldwind.formats.worldfile.WorldFile#WORLD_FILE_HEMISPHERE}, respectively.
     *
     * @param buffer the VecBuffer of UTM tuples.
     * @param params parameter list describing the UTM projection.
     *
     * @throws IllegalArgumentException if the buffer is null, if the buffer has fewer than 2 coordinates per tuple, if
     *                                  the parameter list is null, or if the parameter list does not contain the
     *                                  parameters {@link gov.nasa.worldwind.formats.worldfile.WorldFile#WORLD_FILE_ZONE}
     *                                  and {@link gov.nasa.worldwind.formats.worldfile.WorldFile#WORLD_FILE_HEMISPHERE}.
     */
    public static void convertUTMTuplesToGeographic(VecBuffer buffer, AVList params)
    {
        if (buffer == null)
        {
            String message = Logging.getMessage("nullValue.BufferIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (buffer.getCoordsPerVec() < 2)
        {
            String message = Logging.getMessage("generic.InvalidTupleSize", buffer.getCoordsPerVec());
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (params == null)
        {
            String message = Logging.getMessage("nullValue.ParamsIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (params.getValue(WorldFile.WORLD_FILE_ZONE) == null)
        {
            String message = Logging.getMessage("WorldFile.NoUTMZoneSpecified");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (params.getValue(WorldFile.WORLD_FILE_HEMISPHERE) == null)
        {
            String message = Logging.getMessage("WorldFile.NoUTMHemisphereSpecified");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        int zone = WWUtil.makeInteger((String) params.getValue(WorldFile.WORLD_FILE_ZONE));
        char hemisphere = ((String) params.getValue(WorldFile.WORLD_FILE_HEMISPHERE)).charAt(0);

        double[] coord = new double[2];

        for (int i = 0; i < buffer.getSize(); i++)
        {
            buffer.get(i, coord);
            LatLon location = UTMCoord.locationFromUTMCoord(zone, hemisphere, coord[0], coord[1], null);
            buffer.putLocation(i, location);
        }
    }

    /**
     * Returns the minimum and maximum floating point values in the specified buffer. Values equivalent to the specified
     * <code>missingDataSignal</code> are ignored. This returns null if the buffer is empty or contains only missing
     * values.
     *
     * @param buffer            the buffer to search for the minimum and maximum values.
     * @param missingDataSignal the number indicating a specific floating point value to ignore.
     *
     * @return an array containing the minimum value in index 0 and the maximum value in index 1, or null if the buffer
     *         is empty or contains only missing values.
     *
     * @throws IllegalArgumentException if the buffer is null.
     */
    public static double[] computeExtremeValues(BufferWrapper buffer, double missingDataSignal)
    {
        if (buffer == null)
        {
            String message = Logging.getMessage("nullValue.BufferIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;

        for (int i = 0; i < buffer.length(); i++)
        {
            double value = buffer.getDouble(i);

            if (Double.compare(value, missingDataSignal) == 0)
                continue;

            if (min > value)
                min = value;
            if (max < value)
                max = value;
        }

        if (Double.compare(min, Double.MAX_VALUE) == 0 || Double.compare(max, -Double.MAX_VALUE) == 0)
            return null;

        return new double[] {min, max};
    }

    /**
     * Returns the minimum and maximum floating point values in the specified buffer. Values equivalent to
     * <code>Double.NaN</code> are ignored. This returns null if the buffer is empty or contains only NaN values.
     *
     * @param buffer the buffer to search for the minimum and maximum values.
     *
     * @return an array containing the minimum value in index 0 and the maximum value in index 1, or null if the buffer
     *         is empty or contains only NaN values.
     *
     * @throws IllegalArgumentException if the buffer is null.
     */
    public static double[] computeExtremeValues(BufferWrapper buffer)
    {
        if (buffer == null)
        {
            String message = Logging.getMessage("nullValue.BufferIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return computeExtremeValues(buffer, Double.NaN);
    }
}
