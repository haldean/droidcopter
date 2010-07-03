/*
Copyright (C) 2001, 2008 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/

package gov.nasa.worldwind.util;

import gov.nasa.worldwind.avlist.*;

import javax.media.opengl.GL;
import java.nio.*;

/**
 * BufferWrapper provides an interface for reading and writing primitive data to and from data buffers, without having
 * to know the underlying data type. BufferWrapper may be backed by a primitive data buffer of any type.
 *
 * @author tag
 * @version $Id: BufferWrapper.java 12803 2009-11-18 03:23:02Z dcollins $
 */
public abstract class BufferWrapper
{
    /**
     * Returns the length of the buffer, in units of the underlying data type (e.g. bytes, shorts, ints, floats,
     * doubles).
     *
     * @return the buffer's length.
     */
    public abstract int length();

    /**
     * Returns the OpenGL data type corresponding to the buffer's underlying data type (e.g. GL_BYTE, GL_SHORT, GL_INT,
     * GL_FLOAT, GL_DOUBLE).
     *
     * @return the buffer's OpenGL data type.
     */
    public abstract int getGLDataType();

    /**
     * Returns the size of this buffer, in bytes.
     *
     * @return the buffer's size in bytes.
     */
    public abstract long getSizeInBytes();

    /**
     * Returns the value at the specified index, cast to a byte.
     *
     * @param index the index of the value to be returned.
     *
     * @return the byte at the specified index.
     */
    public abstract byte getByte(int index);

    /**
     * Sets the value at the specified index as a byte. The byte is cast to the underlying data type.
     *
     * @param index the index of the value to be returned.
     * @param value the byte value to be set.
     */
    public abstract void putByte(int index, byte value);

    /**
     * Returns the value at the specified index, cast to a short.
     *
     * @param index the index of the value to be returned.
     *
     * @return the short at the specified index.
     */
    public abstract short getShort(int index);

    /**
     * Sets the value at the specified index as a short. The short is cast to the underlying data type.
     *
     * @param index the index of the value to be returned.
     * @param value the short value to be set.
     */
    public abstract void putShort(int index, short value);

    /**
     * Returns the value at the specified index, cast to an int.
     *
     * @param index the index of the value to be returned.
     *
     * @return the int at the specified index.
     */
    public abstract int getInt(int index);

    /**
     * Sets the value at the specified index as an int. The int is cast to the underlying data type.
     *
     * @param index the index of the value to be returned.
     * @param value the int value to be set.
     */
    public abstract void putInt(int index, int value);

    /**
     * Returns the value at the specified index, cast to a float.
     *
     * @param index the index of the value to be returned.
     *
     * @return the float at the specified index.
     */
    public abstract float getFloat(int index);

    /**
     * Sets the value at the specified index as a float. The float is cast to the underlying data type.
     *
     * @param index the index of the value to be returned.
     * @param value the float value to be set.
     */
    public abstract void putFloat(int index, float value);

    /**
     * Returns the value at the specified index, cast to a double.
     *
     * @param index the index of the value to be returned.
     *
     * @return the double at the specified index.
     */
    public abstract double getDouble(int index);

    /**
     * Sets the value at the specified index as a double. The double is cast to the underlying data type.
     *
     * @param index the index of the value to be returned.
     * @param value the double value to be set.
     */
    public abstract void putDouble(int index, double value);

    /**
     * Returns the sequence of values starting at the specified index and with the specified length, cast to bytes.
     *
     * @param index  the buffer starting index.
     * @param array  the array.
     * @param offset the array starting index.
     * @param length the number of values to get.
     */
    public abstract void getByte(int index, byte[] array, int offset, int length);

    /**
     * Sets the sequence of values starting at the specified index and with the specified length, as bytes. The bytes
     * are cast to the underlying data type.
     *
     * @param index  the buffer starting index.
     * @param array  the array.
     * @param offset the array starting index.
     * @param length the number of values to put.
     */
    public abstract void putByte(int index, byte[] array, int offset, int length);

    /**
     * Returns the sequence of values starting at the specified index and with the specified length, cast to shorts.
     *
     * @param index  the buffer starting index.
     * @param array  the array.
     * @param offset the array starting index.
     * @param length the number of values to get.
     */
    public abstract void getShort(int index, short[] array, int offset, int length);

    /**
     * Sets the sequence of values starting at the specified index and with the specified length, as ints. The ints are
     * cast to the underlying data type.
     *
     * @param index  the buffer starting index.
     * @param array  the array.
     * @param offset the array starting index.
     * @param length the number of values to put.
     */
    public abstract void putShort(int index, short[] array, int offset, int length);

    /**
     * Returns the sequence of values starting at the specified index and with the specified length, cast to ints.
     *
     * @param index  the buffer starting index.
     * @param array  the array.
     * @param offset the array starting index.
     * @param length the number of values to get.
     */
    public abstract void getInt(int index, int[] array, int offset, int length);

    /**
     * Sets the sequence of values starting at the specified index and with the specified length, as ints. The ints are
     * cast to the underlying data type.
     *
     * @param index  the buffer starting index.
     * @param array  the array.
     * @param offset the array starting index.
     * @param length the number of values to put.
     */
    public abstract void putInt(int index, int[] array, int offset, int length);

    /**
     * Returns the sequence of values starting at the specified index and with the specified length, cast to floats.
     *
     * @param index  the buffer starting index.
     * @param array  the array.
     * @param offset the array starting index.
     * @param length the number of values to get.
     */
    public abstract void getFloat(int index, float[] array, int offset, int length);

    /**
     * Sets the sequence of values starting at the specified index and with the specified length, as floats. The floats
     * are cast to the underlying data type.
     *
     * @param index  the buffer starting index.
     * @param array  the array.
     * @param offset the array starting index.
     * @param length the number of values to put.
     */
    public abstract void putFloat(int index, float[] array, int offset, int length);

    /**
     * Returns the sequence of values starting at the specified index and with the specified length, cast to doubles.
     *
     * @param index  the buffer starting index.
     * @param array  the array.
     * @param offset the array starting index.
     * @param length the number of values to get.
     */
    public abstract void getDouble(int index, double[] array, int offset, int length);

    /**
     * Sets the sequence of values starting at the specified index and with the specified length, as doubles. The
     * doubles are cast to the underlying data type.
     *
     * @param index  the buffer starting index.
     * @param array  the array.
     * @param offset the array starting index.
     * @param length the number of values to put.
     */
    public abstract void putDouble(int index, double[] array, int offset, int length);

    /**
     * Returns a new BufferWrapper which is a subsequence of this buffer. The new buffer starts with the value at the
     * specified index, and has the specified length. The two buffers share the same backing store, so changes to this
     * buffer are reflected in the new buffer, and visa versa.
     *
     * @param index  the new buffer's starting index.
     * @param length the new buffer's length.
     *
     * @return a subsequence of this buffer.
     */
    public abstract BufferWrapper getSubBuffer(int index, int length);

    /**
     * Sets a subsequence of this buffer with the contents of the specified buffer. The subsequence to set starts with
     * the value at the specified index, and has length equal to the specified buffer's length.
     *
     * @param index  the starting index to set.
     * @param buffer the buffer.
     */
    public abstract void putSubBuffer(int index, BufferWrapper buffer);

    /**
     * Returns the buffer's backing data sture. For the standard BufferWrapper types (ByteBufferWrapper,
     * ShortBufferWrapper, IntBufferWrapper, FloatBufferWrapper, and DoubleBufferWrapper), this returns the backing
     * {@link Buffer}.
     *
     * @return the backing data store.
     */
    public abstract Buffer getBackingBuffer();

    /**
     * Wraps the specified {@link ByteBuffer} with a BufferWrapper according to the specified parameters. The byteBuffer
     * is interpreted according to values found in the parameters. {@link AVKey#BYTE_ORDER} specifies the byteBuffer's
     * byte ordering,. and {@link AVKey#DATA_TYPE} specifies type of data store in the byteBuffer: shorts, ints, or
     * floats.
     *
     * @param byteBuffer the buffer to wrap.
     * @param parameters the parameters which describe how to interpret the buffer.
     *
     * @return a new BufferWrapper backed by the specified byteBuffer.
     *
     * @throws IllegalArgumentException if either the byteBuffer or parameters are null, or if the parameters do not
     *                                  completely describe how to interpret the byteBuffer.
     */
    public static BufferWrapper wrap(ByteBuffer byteBuffer, AVList parameters)
    {
        if (byteBuffer == null)
        {
            String message = Logging.getMessage("nullValue.ByteBufferIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (parameters == null)
        {
            String message = Logging.getMessage("nullValue.ParametersIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        String message = validate(parameters);
        if (message != null)
        {
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Object o = parameters.getValue(AVKey.BYTE_ORDER);
        byteBuffer.order(AVKey.LITTLE_ENDIAN.equals(o) ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);

        o = parameters.getValue(AVKey.DATA_TYPE);
        BufferWrapper wrapper = null;
        if (AVKey.INT8.equals(o))
            wrapper = new ByteBufferWrapper(byteBuffer);
        else if (AVKey.INT16.equals(o))
            wrapper = new ShortBufferWrapper(byteBuffer.asShortBuffer());
        else if (AVKey.INT32.equals(o))
            wrapper = new IntBufferWrapper(byteBuffer.asIntBuffer());
        else if (AVKey.FLOAT32.equals(o))
            wrapper = new FloatBufferWrapper(byteBuffer.asFloatBuffer());

        return wrapper;
    }

    public abstract static class AbstractBufferWrapper extends BufferWrapper
    {
        private Buffer buffer;
        protected final BufferStateHandler bufferStateHandler = new BufferStateHandler();

        public AbstractBufferWrapper(Buffer buffer)
        {
            if (buffer == null)
            {
                String message = Logging.getMessage("nullValue.BufferIsNull");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }

            this.buffer = buffer;
        }

        public int length()
        {
            return this.buffer.remaining();
        }

        public void getByte(int index, byte[] array, int offset, int length)
        {
            if (array == null)
            {
                String message = Logging.getMessage("nullValue.ArrayIsNull");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }

            this.bufferStateHandler.pushState(this.buffer);
            try
            {
                this.buffer.position(index);
                this.doGetByte(array, offset, length);
            }
            finally
            {
                this.bufferStateHandler.popState(this.buffer);
            }
        }

        public void putByte(int index, byte[] array, int offset, int length)
        {
            if (array == null)
            {
                String message = Logging.getMessage("nullValue.ArrayIsNull");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }

            this.bufferStateHandler.pushState(this.buffer);
            try
            {
                this.buffer.position(index);
                this.doPutByte(array, offset, length);
            }
            finally
            {
                this.bufferStateHandler.popState(this.buffer);
            }
        }

        public void getShort(int index, short[] array, int offset, int length)
        {
            if (array == null)
            {
                String message = Logging.getMessage("nullValue.ArrayIsNull");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }

            this.bufferStateHandler.pushState(this.buffer);
            try
            {
                this.buffer.position(index);
                this.doGetShort(array, offset, length);
            }
            finally
            {
                this.bufferStateHandler.popState(this.buffer);
            }
        }

        public void putShort(int index, short[] array, int offset, int length)
        {
            if (array == null)
            {
                String message = Logging.getMessage("nullValue.ArrayIsNull");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }

            this.bufferStateHandler.pushState(this.buffer);
            try
            {
                this.buffer.position(index);
                this.doPutShort(array, offset, length);
            }
            finally
            {
                this.bufferStateHandler.popState(this.buffer);
            }
        }

        public void getInt(int index, int[] array, int offset, int length)
        {
            if (array == null)
            {
                String message = Logging.getMessage("nullValue.ArrayIsNull");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }

            this.bufferStateHandler.pushState(this.buffer);
            try
            {
                this.buffer.position(index);
                this.doGetInt(array, offset, length);
            }
            finally
            {
                this.bufferStateHandler.popState(this.buffer);
            }
        }

        public void putInt(int index, int[] array, int offset, int length)
        {
            if (array == null)
            {
                String message = Logging.getMessage("nullValue.ArrayIsNull");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }

            this.bufferStateHandler.pushState(this.buffer);
            try
            {
                this.buffer.position(index);
                this.doPutInt(array, offset, length);
            }
            finally
            {
                this.bufferStateHandler.popState(this.buffer);
            }
        }

        public void getFloat(int index, float[] array, int offset, int length)
        {
            if (array == null)
            {
                String message = Logging.getMessage("nullValue.ArrayIsNull");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }

            this.bufferStateHandler.pushState(this.buffer);
            try
            {
                this.buffer.position(index);
                this.doGetFloat(array, offset, length);
            }
            finally
            {
                this.bufferStateHandler.popState(this.buffer);
            }
        }

        public void putFloat(int index, float[] array, int offset, int length)
        {
            if (array == null)
            {
                String message = Logging.getMessage("nullValue.ArrayIsNull");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }

            this.bufferStateHandler.pushState(this.buffer);
            try
            {
                this.buffer.position(index);
                this.doPutFloat(array, offset, length);
            }
            finally
            {
                this.bufferStateHandler.popState(this.buffer);
            }
        }

        public void getDouble(int index, double[] array, int offset, int length)
        {
            if (array == null)
            {
                String message = Logging.getMessage("nullValue.ArrayIsNull");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }

            this.bufferStateHandler.pushState(this.buffer);
            try
            {
                this.buffer.position(index);
                this.doGetDouble(array, offset, length);
            }
            finally
            {
                this.bufferStateHandler.popState(this.buffer);
            }
        }

        public void putDouble(int index, double[] array, int offset, int length)
        {
            if (array == null)
            {
                String message = Logging.getMessage("nullValue.ArrayIsNull");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }

            this.bufferStateHandler.pushState(this.buffer);
            try
            {
                this.buffer.position(index);
                this.doPutDouble(array, offset, length);
            }
            finally
            {
                this.bufferStateHandler.popState(this.buffer);
            }
        }

        public BufferWrapper getSubBuffer(int index, int length)
        {
            BufferWrapper subBuffer = null;

            this.bufferStateHandler.pushState(this.buffer);
            try
            {
                this.buffer.position(index);
                this.buffer.limit(index + length);
                subBuffer = this.doGetSubBuffer();
            }
            finally
            {
                this.bufferStateHandler.popState(this.buffer);
            }

            return subBuffer;
        }

        public void putSubBuffer(int index, BufferWrapper buffer)
        {
            if (buffer == null)
            {
                String message = Logging.getMessage("nullValue.BufferIsNull");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }

            this.bufferStateHandler.pushState(this.buffer);
            try
            {
                if (!this.doPutSubBuffer(index, buffer))
                {
                    int length = buffer.length();
                    double[] array = new double[length];
                    buffer.getDouble(0, array, 0, length);
                    this.putDouble(index, array, 0, length);
                }
            }
            finally
            {
                this.bufferStateHandler.popState(this.buffer);
            }
        }

        public Buffer getBackingBuffer()
        {
            return this.buffer;
        }

        protected abstract void doGetByte(byte[] array, int offset, int length);

        protected abstract void doPutByte(byte[] array, int offset, int length);

        protected abstract void doGetShort(short[] array, int offset, int length);

        protected abstract void doPutShort(short[] array, int offset, int length);

        protected abstract void doGetInt(int[] array, int offset, int length);

        protected abstract void doPutInt(int[] array, int offset, int length);

        protected abstract void doGetFloat(float[] array, int offset, int length);

        protected abstract void doPutFloat(float[] array, int offset, int length);

        protected abstract void doGetDouble(double[] array, int offset, int length);

        protected abstract void doPutDouble(double[] array, int offset, int length);

        protected abstract BufferWrapper doGetSubBuffer();

        protected abstract boolean doPutSubBuffer(int index, BufferWrapper buffer);
    }

    public static class ByteBufferWrapper extends BufferWrapper.AbstractBufferWrapper
    {
        private ByteBuffer byteBuffer;

        public ByteBufferWrapper(ByteBuffer buffer)
        {
            super(buffer);
            this.byteBuffer = buffer;
        }

        public int getGLDataType()
        {
            return GL.GL_BYTE;
        }

        public long getSizeInBytes()
        {
            return this.byteBuffer.capacity();
        }

        public byte getByte(int index)
        {
            return this.byteBuffer.get(index);
        }

        public void putByte(int index, byte value)
        {
            this.byteBuffer.put(index, value);
        }

        public short getShort(int index)
        {
            return this.byteBuffer.get(index);
        }

        public void putShort(int index, short value)
        {
            this.byteBuffer.put(index, (byte) value);
        }

        public int getInt(int index)
        {
            return this.byteBuffer.get(index);
        }

        public void putInt(int index, int value)
        {
            this.byteBuffer.put(index, (byte) value);
        }

        public float getFloat(int index)
        {
            return this.byteBuffer.get(index);
        }

        public void putFloat(int index, float value)
        {
            this.byteBuffer.put(index, (byte) value);
        }

        public double getDouble(int index)
        {
            return this.byteBuffer.get(index);
        }

        public void putDouble(int index, double value)
        {
            this.byteBuffer.put(index, (byte) value);
        }

        protected void doGetByte(byte[] array, int offset, int length)
        {
            this.byteBuffer.get(array, offset, length);
        }

        protected void doPutByte(byte[] array, int offset, int length)
        {
            this.byteBuffer.put(array, offset, length);
        }

        protected void doGetShort(short[] array, int offset, int length)
        {
            byte[] tmp = new byte[length];
            this.byteBuffer.get(tmp, 0, length);

            for (int i = 0; i < length; i++)
            {
                array[i + offset] = tmp[i];
            }
        }

        protected void doPutShort(short[] array, int offset, int length)
        {
            byte[] tmp = new byte[length];
            for (int i = 0; i < length; i++)
            {
                tmp[i] = (byte) array[i + offset];
            }

            this.byteBuffer.put(tmp, 0, length);
        }

        protected void doGetInt(int[] array, int offset, int length)
        {
            byte[] tmp = new byte[length];
            this.byteBuffer.get(tmp, 0, length);

            for (int i = 0; i < length; i++)
            {
                array[i + offset] = tmp[i];
            }
        }

        protected void doPutInt(int[] array, int offset, int length)
        {
            byte[] tmp = new byte[length];
            for (int i = 0; i < length; i++)
            {
                tmp[i] = (byte) array[i + offset];
            }

            this.byteBuffer.put(tmp, 0, length);
        }

        protected void doGetFloat(float[] array, int offset, int length)
        {
            byte[] tmp = new byte[length];
            this.byteBuffer.get(tmp, 0, length);

            for (int i = 0; i < length; i++)
            {
                array[i + offset] = tmp[i];
            }
        }

        protected void doPutFloat(float[] array, int offset, int length)
        {
            byte[] tmp = new byte[length];
            for (int i = 0; i < length; i++)
            {
                tmp[i] = (byte) array[i + offset];
            }

            this.byteBuffer.put(tmp, 0, length);
        }

        protected void doGetDouble(double[] array, int offset, int length)
        {
            byte[] tmp = new byte[length];
            this.byteBuffer.get(tmp, 0, length);

            for (int i = 0; i < length; i++)
            {
                array[i + offset] = tmp[i];
            }
        }

        protected void doPutDouble(double[] array, int offset, int length)
        {
            byte[] tmp = new byte[length];
            for (int i = 0; i < length; i++)
            {
                tmp[i] = (byte) array[i + offset];
            }

            this.byteBuffer.put(tmp, 0, length);
        }

        protected BufferWrapper doGetSubBuffer()
        {
            return new ByteBufferWrapper(this.byteBuffer.slice());
        }

        protected boolean doPutSubBuffer(int index, BufferWrapper buffer)
        {
            Buffer that = buffer.getBackingBuffer();
            if (that instanceof ByteBuffer)
            {
                this.bufferStateHandler.pushState(that);
                try
                {
                    this.byteBuffer.position(index);
                    this.byteBuffer.put((ByteBuffer) that);
                }
                finally
                {
                    this.bufferStateHandler.popState(that);
                }
                return true;
            }

            return false;
        }
    }

    public static class ShortBufferWrapper extends AbstractBufferWrapper
    {
        private ShortBuffer shortBuffer;

        public ShortBufferWrapper(ShortBuffer buffer)
        {
            super(buffer);
            this.shortBuffer = buffer;
        }

        public int getGLDataType()
        {
            return GL.GL_SHORT;
        }

        public long getSizeInBytes()
        {
            return (Short.SIZE / 8) * this.shortBuffer.capacity();
        }

        public byte getByte(int index)
        {
            return (byte) this.shortBuffer.get(index);
        }

        public void putByte(int index, byte value)
        {
            this.shortBuffer.put(index, value);
        }

        public short getShort(int index)
        {
            return this.shortBuffer.get(index);
        }

        public void putShort(int index, short value)
        {
            this.shortBuffer.put(index, value);
        }

        public int getInt(int index)
        {
            return this.shortBuffer.get(index);
        }

        public void putInt(int index, int value)
        {
            this.shortBuffer.put(index, (short) value);
        }

        public float getFloat(int index)
        {
            return this.shortBuffer.get(index);
        }

        public void putFloat(int index, float value)
        {
            this.shortBuffer.put(index, (short) value);
        }

        public double getDouble(int index)
        {
            return this.shortBuffer.get(index);
        }

        public void putDouble(int index, double value)
        {
            this.shortBuffer.put(index, (short) value);
        }

        protected void doGetByte(byte[] array, int offset, int length)
        {
            short[] tmp = new short[length];
            this.shortBuffer.get(tmp, 0, length);

            for (int i = 0; i < length; i++)
            {
                array[i + offset] = (byte) tmp[i];
            }
        }

        protected void doPutByte(byte[] array, int offset, int length)
        {
            short[] tmp = new short[length];
            for (int i = 0; i < length; i++)
            {
                tmp[i] = array[i + offset];
            }

            this.shortBuffer.put(tmp, 0, length);
        }

        protected void doGetShort(short[] array, int offset, int length)
        {
            this.shortBuffer.get(array, offset, length);
        }

        protected void doPutShort(short[] array, int offset, int length)
        {
            this.shortBuffer.put(array, offset, length);
        }

        protected void doGetInt(int[] array, int offset, int length)
        {
            short[] tmp = new short[length];
            this.shortBuffer.get(tmp, 0, length);

            for (int i = 0; i < length; i++)
            {
                array[i + offset] = tmp[i];
            }
        }

        protected void doPutInt(int[] array, int offset, int length)
        {
            short[] tmp = new short[length];
            for (int i = 0; i < length; i++)
            {
                tmp[i] = (short) array[i + offset];
            }

            this.shortBuffer.put(tmp, 0, length);
        }

        protected void doGetFloat(float[] array, int offset, int length)
        {
            short[] tmp = new short[length];
            this.shortBuffer.get(tmp, 0, length);

            for (int i = 0; i < length; i++)
            {
                array[i + offset] = tmp[i];
            }
        }

        protected void doPutFloat(float[] array, int offset, int length)
        {
            short[] tmp = new short[length];
            for (int i = 0; i < length; i++)
            {
                tmp[i] = (short) array[i + offset];
            }

            this.shortBuffer.put(tmp, 0, length);
        }

        protected void doGetDouble(double[] array, int offset, int length)
        {
            short[] tmp = new short[length];
            this.shortBuffer.get(tmp, 0, length);

            for (int i = 0; i < length; i++)
            {
                array[i + offset] = tmp[i];
            }
        }

        protected void doPutDouble(double[] array, int offset, int length)
        {
            short[] tmp = new short[length];
            for (int i = 0; i < length; i++)
            {
                tmp[i] = (short) array[i + offset];
            }

            this.shortBuffer.put(tmp, 0, length);
        }

        protected BufferWrapper doGetSubBuffer()
        {
            return new ShortBufferWrapper(this.shortBuffer.slice());
        }

        protected boolean doPutSubBuffer(int index, BufferWrapper buffer)
        {
            Buffer that = buffer.getBackingBuffer();
            if (that instanceof ShortBuffer)
            {
                this.bufferStateHandler.pushState(that);
                try
                {
                    this.shortBuffer.position(index);
                    this.shortBuffer.put((ShortBuffer) that);
                }
                finally
                {
                    this.bufferStateHandler.popState(that);
                }
                return true;
            }

            return false;
        }
    }

    public static class IntBufferWrapper extends AbstractBufferWrapper
    {
        private IntBuffer intBuffer;

        public IntBufferWrapper(IntBuffer buffer)
        {
            super(buffer);
            this.intBuffer = buffer;
        }

        public int getGLDataType()
        {
            return GL.GL_INT;
        }

        public long getSizeInBytes()
        {
            return (Integer.SIZE / 8) * this.intBuffer.capacity();
        }

        public byte getByte(int index)
        {
            return (byte) this.intBuffer.get(index);
        }

        public void putByte(int index, byte value)
        {
            this.intBuffer.put(index, value);
        }

        public short getShort(int index)
        {
            return (short) this.intBuffer.get(index);
        }

        public void putShort(int index, short value)
        {
            this.intBuffer.put(index, value);
        }

        public int getInt(int index)
        {
            return this.intBuffer.get(index);
        }

        public void putInt(int index, int value)
        {
            this.intBuffer.put(index, value);
        }

        public float getFloat(int index)
        {
            return this.intBuffer.get(index);
        }

        public void putFloat(int index, float value)
        {
            this.intBuffer.put(index, (int) value);
        }

        public double getDouble(int index)
        {
            return this.intBuffer.get(index);
        }

        public void putDouble(int index, double value)
        {
            this.intBuffer.put(index, (int) value);
        }

        protected void doGetByte(byte[] array, int offset, int length)
        {
            int[] tmp = new int[length];
            this.intBuffer.get(tmp, 0, length);

            for (int i = 0; i < length; i++)
            {
                array[i + offset] = (byte) tmp[i];
            }
        }

        protected void doPutByte(byte[] array, int offset, int length)
        {
            int[] tmp = new int[length];
            for (int i = 0; i < length; i++)
            {
                tmp[i] = array[i + offset];
            }

            this.intBuffer.put(tmp, 0, length);
        }

        protected void doGetShort(short[] array, int offset, int length)
        {
            int[] tmp = new int[length];
            this.intBuffer.get(tmp, 0, length);

            for (int i = 0; i < length; i++)
            {
                array[i + offset] = (short) tmp[i];
            }
        }

        protected void doPutShort(short[] array, int offset, int length)
        {
            int[] tmp = new int[length];
            for (int i = 0; i < length; i++)
            {
                tmp[i] = array[i + offset];
            }

            this.intBuffer.put(tmp, 0, length);
        }

        protected void doGetInt(int[] array, int offset, int length)
        {
            this.intBuffer.get(array, offset, length);
        }

        protected void doPutInt(int[] array, int offset, int length)
        {
            this.intBuffer.put(array, offset, length);
        }

        protected void doGetFloat(float[] array, int offset, int length)
        {
            int[] tmp = new int[length];
            this.intBuffer.get(tmp, 0, length);

            for (int i = 0; i < length; i++)
            {
                array[i + offset] = tmp[i];
            }
        }

        protected void doPutFloat(float[] array, int offset, int length)
        {
            int[] tmp = new int[length];
            for (int i = 0; i < length; i++)
            {
                tmp[i] = (int) array[i + offset];
            }

            this.intBuffer.put(tmp, 0, length);
        }

        protected void doGetDouble(double[] array, int offset, int length)
        {
            int[] tmp = new int[length];
            this.intBuffer.get(tmp, 0, length);

            for (int i = 0; i < length; i++)
            {
                array[i + offset] = tmp[i];
            }
        }

        protected void doPutDouble(double[] array, int offset, int length)
        {
            int[] tmp = new int[length];
            for (int i = 0; i < length; i++)
            {
                tmp[i] = (int) array[i + offset];
            }

            this.intBuffer.put(tmp, 0, length);
        }

        protected BufferWrapper doGetSubBuffer()
        {
            return new IntBufferWrapper(this.intBuffer.slice());
        }

        protected boolean doPutSubBuffer(int index, BufferWrapper buffer)
        {
            Buffer that = buffer.getBackingBuffer();
            if (that instanceof IntBuffer)
            {
                this.bufferStateHandler.pushState(that);
                try
                {
                    this.intBuffer.position(index);
                    this.intBuffer.put((IntBuffer) that);
                }
                finally
                {
                    this.bufferStateHandler.popState(that);
                }
                return true;
            }

            return false;
        }
    }

    public static class FloatBufferWrapper extends AbstractBufferWrapper
    {
        private FloatBuffer floatBuffer;

        public FloatBufferWrapper(FloatBuffer buffer)
        {
            super(buffer);
            this.floatBuffer = buffer;
        }

        public int getGLDataType()
        {
            return GL.GL_FLOAT;
        }

        public long getSizeInBytes()
        {
            return (Float.SIZE / 8) * this.floatBuffer.capacity();
        }

        public byte getByte(int index)
        {
            return (byte) this.floatBuffer.get(index);
        }

        public void putByte(int index, byte value)
        {
            this.floatBuffer.put(index, value);
        }

        public short getShort(int index)
        {
            return (short) this.floatBuffer.get(index);
        }

        public void putShort(int index, short value)
        {
            this.floatBuffer.put(index, value);
        }

        public int getInt(int index)
        {
            return (int) this.floatBuffer.get(index);
        }

        public void putInt(int index, int value)
        {
            this.floatBuffer.put(index, value);
        }

        public float getFloat(int index)
        {
            return this.floatBuffer.get(index);
        }

        public void putFloat(int index, float value)
        {
            this.floatBuffer.put(index, value);
        }

        public double getDouble(int index)
        {
            return this.floatBuffer.get(index);
        }

        public void putDouble(int index, double value)
        {
            this.floatBuffer.put(index, (float) value);
        }

        protected void doGetByte(byte[] array, int offset, int length)
        {
            float[] tmp = new float[length];
            this.floatBuffer.get(tmp, 0, length);

            for (int i = 0; i < length; i++)
            {
                array[i + offset] = (byte) tmp[i];
            }
        }

        protected void doPutByte(byte[] array, int offset, int length)
        {
            float[] tmp = new float[length];
            for (int i = 0; i < length; i++)
            {
                tmp[i] = array[i + offset];
            }

            this.floatBuffer.put(tmp, 0, length);
        }

        protected void doGetShort(short[] array, int offset, int length)
        {
            float[] tmp = new float[length];
            this.floatBuffer.get(tmp, 0, length);

            for (int i = 0; i < length; i++)
            {
                array[i + offset] = (short) tmp[i];
            }
        }

        protected void doPutShort(short[] array, int offset, int length)
        {
            float[] tmp = new float[length];
            for (int i = 0; i < length; i++)
            {
                tmp[i] = array[i + offset];
            }

            this.floatBuffer.put(tmp, 0, length);
        }

        protected void doGetInt(int[] array, int offset, int length)
        {
            float[] tmp = new float[length];
            this.floatBuffer.get(tmp, 0, length);

            for (int i = 0; i < length; i++)
            {
                array[i + offset] = (int) tmp[i];
            }
        }

        protected void doPutInt(int[] array, int offset, int length)
        {
            float[] tmp = new float[length];
            for (int i = 0; i < length; i++)
            {
                tmp[i] = (float) array[i + offset];
            }

            this.floatBuffer.put(tmp, 0, length);
        }

        protected void doGetFloat(float[] array, int offset, int length)
        {
            this.floatBuffer.get(array, offset, length);
        }

        protected void doPutFloat(float[] array, int offset, int length)
        {
            this.floatBuffer.put(array, offset, length);
        }

        protected void doGetDouble(double[] array, int offset, int length)
        {
            float[] tmp = new float[length];
            this.floatBuffer.get(tmp, 0, length);

            for (int i = 0; i < length; i++)
            {
                array[i + offset] = tmp[i];
            }
        }

        protected void doPutDouble(double[] array, int offset, int length)
        {
            float[] tmp = new float[length];
            for (int i = 0; i < length; i++)
            {
                tmp[i] = (float) array[i + offset];
            }

            this.floatBuffer.put(tmp, 0, length);
        }

        protected BufferWrapper doGetSubBuffer()
        {
            return new FloatBufferWrapper(this.floatBuffer.slice());
        }

        protected boolean doPutSubBuffer(int index, BufferWrapper buffer)
        {
            Buffer that = buffer.getBackingBuffer();
            if (that instanceof FloatBuffer)
            {
                this.bufferStateHandler.pushState(that);
                try
                {
                    this.floatBuffer.position(index);
                    this.floatBuffer.put((FloatBuffer) that);
                }
                finally
                {
                    this.bufferStateHandler.popState(that);
                }
                return true;
            }

            return false;
        }
    }

    public static class DoubleBufferWrapper extends AbstractBufferWrapper
    {
        private DoubleBuffer doubleBuffer;

        public DoubleBufferWrapper(DoubleBuffer buffer)
        {
            super(buffer);
            this.doubleBuffer = buffer;
        }

        public int getGLDataType()
        {
            return GL.GL_DOUBLE;
        }

        public long getSizeInBytes()
        {
            return (Double.SIZE / 8) * this.doubleBuffer.capacity();
        }

        public byte getByte(int index)
        {
            return (byte) this.doubleBuffer.get(index);
        }

        public void putByte(int index, byte value)
        {
            this.doubleBuffer.put(index, value);
        }

        public short getShort(int index)
        {
            return (short) this.doubleBuffer.get(index);
        }

        public void putShort(int index, short value)
        {
            this.doubleBuffer.put(index, value);
        }

        public int getInt(int index)
        {
            return (int) this.doubleBuffer.get(index);
        }

        public void putInt(int index, int value)
        {
            this.doubleBuffer.put(index, value);
        }

        public float getFloat(int index)
        {
            return (float) this.doubleBuffer.get(index);
        }

        public void putFloat(int index, float value)
        {
            this.doubleBuffer.put(index, value);
        }

        public double getDouble(int index)
        {
            return this.doubleBuffer.get(index);
        }

        public void putDouble(int index, double value)
        {
            this.doubleBuffer.put(index, value);
        }

        protected void doGetByte(byte[] array, int offset, int length)
        {
            double[] tmp = new double[length];
            this.doubleBuffer.get(tmp, 0, length);

            for (int i = 0; i < length; i++)
            {
                array[i + offset] = (byte) tmp[i];
            }
        }

        protected void doPutByte(byte[] array, int offset, int length)
        {
            double[] tmp = new double[length];
            for (int i = 0; i < length; i++)
            {
                tmp[i] = array[i + offset];
            }

            this.doubleBuffer.put(tmp, 0, length);
        }

        protected void doGetShort(short[] array, int offset, int length)
        {
            double[] tmp = new double[length];
            this.doubleBuffer.get(tmp, 0, length);

            for (int i = 0; i < length; i++)
            {
                array[i + offset] = (short) tmp[i];
            }
        }

        protected void doPutShort(short[] array, int offset, int length)
        {
            double[] tmp = new double[length];
            for (int i = 0; i < length; i++)
            {
                tmp[i] = array[i + offset];
            }

            this.doubleBuffer.put(tmp, 0, length);
        }

        protected void doGetInt(int[] array, int offset, int length)
        {
            double[] tmp = new double[length];
            this.doubleBuffer.get(tmp, 0, length);

            for (int i = 0; i < length; i++)
            {
                array[i + offset] = (int) tmp[i];
            }
        }

        protected void doPutInt(int[] array, int offset, int length)
        {
            double[] tmp = new double[length];
            for (int i = 0; i < length; i++)
            {
                tmp[i] = array[i + offset];
            }

            this.doubleBuffer.put(tmp, 0, length);
        }

        protected void doGetFloat(float[] array, int offset, int length)
        {
            double[] tmp = new double[length];
            this.doubleBuffer.get(tmp, 0, length);

            for (int i = 0; i < length; i++)
            {
                array[i + offset] = (float) tmp[i];
            }
        }

        protected void doPutFloat(float[] array, int offset, int length)
        {
            double[] tmp = new double[length];
            for (int i = 0; i < length; i++)
            {
                tmp[i] = array[i + offset];
            }

            this.doubleBuffer.put(tmp, 0, length);
        }

        protected void doGetDouble(double[] array, int offset, int length)
        {
            this.doubleBuffer.get(array, offset, length);
        }

        protected void doPutDouble(double[] array, int offset, int length)
        {
            this.doubleBuffer.put(array, offset, length);
        }

        protected BufferWrapper doGetSubBuffer()
        {
            return new DoubleBufferWrapper(this.doubleBuffer.slice());
        }

        protected boolean doPutSubBuffer(int index, BufferWrapper buffer)
        {
            Buffer that = buffer.getBackingBuffer();
            if (that instanceof DoubleBuffer)
            {
                this.bufferStateHandler.pushState(that);
                try
                {
                    this.doubleBuffer.position(index);
                    this.doubleBuffer.put((DoubleBuffer) that);
                }
                finally
                {
                    this.bufferStateHandler.popState(that);
                }
                return true;
            }

            return false;
        }
    }

    //**************************************************************//
    //********************  Support  *******************************//
    //**************************************************************//

    protected static String validate(AVList params)
    {
        StringBuilder sb = new StringBuilder();

        Object o = params.getValue(AVKey.BYTE_ORDER);
        if (o == null || !validateByteOrder(o))
            sb.append(sb.length() > 0 ? ", " : "").append(Logging.getMessage("term.byteOrder"));

        o = params.getValue(AVKey.DATA_TYPE);
        if (o == null || !validateDataType(o))
            sb.append(sb.length() > 0 ? ", " : "").append(Logging.getMessage("term.dataType"));

        if (sb.length() == 0)
            return null;

        return Logging.getMessage("BufferWrapper.InvalidBufferParameters", sb.toString());
    }

    protected static boolean validateByteOrder(Object o)
    {
        return AVKey.BIG_ENDIAN.equals(o)
            || AVKey.LITTLE_ENDIAN.equals(o);
    }

    protected static boolean validateDataType(Object o)
    {
        return AVKey.INT8.equals(o)
            || AVKey.INT16.equals(o)
            || AVKey.INT32.equals(o)
            || AVKey.FLOAT32.equals(o);
    }
}
