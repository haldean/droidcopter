/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.util;

/**
 * TupleBufferFactory provides a general factory interface for creating instances of {@link VecBuffer}, without having
 * to know the underlying data type or the number of coordinates per tuple. Once created, a VecBuffer abstracts reading
 * and writing tuples of primitive values from the underlying data type and tuple size. When VecBuffer is combined with
 * TupleBufferFactory, a component may create and work with tuple data in a type agnostic manner.
 * <p/>
 * TupleBufferFactory is itself abstract and defines the factory interface. It defines several implementations as static
 * inner classes, which serve the most common data types: {@link TupleBufferFactory.ByteTupleBufferFactory}, {@link
 * TupleBufferFactory.ShortTupleBufferFactory}, {@link TupleBufferFactory.IntTupleBufferFactory}, {@link
 * TupleBufferFactory.FloatTupleBufferFactory}, and {@link TupleBufferFactory.DoubleTupleBufferFactory}.
 *
 * @author dcollins
 * @version $Id: TupleBufferFactory.java 12804 2009-11-18 08:41:17Z dcollins $
 * @see gov.nasa.worldwind.util.VecBuffer
 * @see gov.nasa.worldwind.util.BufferFactory
 */
public abstract class TupleBufferFactory
{
    /**
     * Constructs a new VecBuffer with the specified size.
     *
     * @param size the new buffer's size, in number of tuples.
     *
     * @return the new buffer.
     *
     * @throws IllegalArgumentException if size is negative.
     */
    public abstract VecBuffer newBuffer(int size);

    /**
     * Implementation of TupleBufferFactory which constructs instances of {@link gov.nasa.worldwind.util.VecBuffer}
     * according to a specified tuple size and {@link BufferFactory}.
     */
    public static class BasicTupleBufferFactory extends TupleBufferFactory
    {
        protected final int tupleSize;
        protected final BufferFactory bufferFactory;

        /**
         * Constructs a new BasicTupleBufferFactory with the specified tuple size and BufferFactory.
         *
         * @param tupleSize     the number of coordinates per logical tuple.
         * @param bufferFactory the factory to use for constructing the VecBuffer's backing BufferWrapper.
         *
         * @throws IllegalArgumentException is the tuple size is negative, or if the buffer factory is null.
         */
        public BasicTupleBufferFactory(int tupleSize, BufferFactory bufferFactory)
        {
            if (tupleSize < 0)
            {
                String message = Logging.getMessage("generic.SizeOutOfRange", tupleSize);
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }

            if (bufferFactory == null)
            {
                String message = Logging.getMessage("nullValue.FactoryIsNull");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }

            this.tupleSize = tupleSize;
            this.bufferFactory = bufferFactory;
        }

        /**
         * Constructs a new VecBuffer with the specified size, backed by a {@link gov.nasa.worldwind.util.BufferWrapper}.
         *
         * @param size the new VecBuffer's size, in tuples.
         *
         * @return the new VecBuffer.
         *
         * @throws IllegalArgumentException if size is negative.
         */
        public VecBuffer newBuffer(int size)
        {
            if (size < 0)
            {
                String message = Logging.getMessage("generic.SizeOutOfRange", size);
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }

            return this.newTupleBuffer(this.tupleSize, this.bufferFactory.newBuffer(this.tupleSize * size));
        }

        protected VecBuffer newTupleBuffer(int tupleSize, BufferWrapper buffer)
        {
            return new VecBuffer(tupleSize, buffer);
        }
    }

    /**
     * Implementation of TupleBufferFactory which constructs instances of {@link gov.nasa.worldwind.util.VecBuffer},
     * backed by a {@link gov.nasa.worldwind.util.BufferWrapper.ByteBufferWrapper}.
     */
    public static class ByteTupleBufferFactory extends BasicTupleBufferFactory
    {
        /**
         * Constructs a new ByteTupleBufferFactory with the specified tuple size and buffer allocation policy.
         *
         * @param tupleSize      the number of coordinates per logical tuple.
         * @param allocateDirect true to allocate ByteBufferWrappers backed by direct buffers, false to allocate
         *                       ByteBufferWrappers backed by non-direct buffers.
         */
        public ByteTupleBufferFactory(int tupleSize, boolean allocateDirect)
        {
            super(tupleSize, new BufferFactory.ByteBufferFactory(allocateDirect));
        }

        /**
         * Constructs a new ByteTupleBufferFactory with the specified tuple size and the default buffer allocation
         * policy. This factory allocates ByteBufferWrappers backed by direct buffers.
         *
         * @param tupleSize the number of coordinates per logical tuple.
         */
        public ByteTupleBufferFactory(int tupleSize)
        {
            this(tupleSize, true);
        }
    }

    /**
     * Implementation of TupleBufferFactory which constructs instances of {@link gov.nasa.worldwind.util.VecBuffer},
     * backed by a {@link gov.nasa.worldwind.util.BufferWrapper.ShortBufferWrapper}.
     */
    public static class ShortTupleBufferFactory extends BasicTupleBufferFactory
    {
        /**
         * Constructs a new ShortTupleBufferFactory with the specified tuple size and buffer allocation policy.
         *
         * @param tupleSize      the number of coordinates per logical tuple.
         * @param allocateDirect true to allocate ShortBufferWrappers backed by direct buffers, false to allocate
         *                       ShortBufferWrappers backed by non-direct buffers.
         *
         * @throws IllegalArgumentException is the tuple size is negative, or if the buffer factory is null.
         */
        public ShortTupleBufferFactory(int tupleSize, boolean allocateDirect)
        {
            super(tupleSize, new BufferFactory.ShortBufferFactory(allocateDirect));
        }

        /**
         * Constructs a new ShortTupleBufferFactory with the specified tuple size and the default buffer allocation
         * policy. This factory allocates ShortBufferWrappers backed by direct buffers.
         *
         * @param tupleSize the number of coordinates per logical tuple.
         */
        public ShortTupleBufferFactory(int tupleSize)
        {
            this(tupleSize, true);
        }
    }

    /**
     * Implementation of TupleBufferFactory which constructs instances of {@link gov.nasa.worldwind.util.VecBuffer},
     * backed by a {@link gov.nasa.worldwind.util.BufferWrapper.IntBufferWrapper}.
     */
    public static class IntTupleBufferFactory extends BasicTupleBufferFactory
    {
        /**
         * Constructs a new IntTupleBufferFactory with the specified tuple size and buffer allocation policy.
         *
         * @param tupleSize      the number of coordinates per logical tuple.
         * @param allocateDirect true to allocate IntBufferWrappers backed by direct buffers, false to allocate
         *                       IntBufferWrappers backed by non-direct buffers.
         *
         * @throws IllegalArgumentException is the tuple size is negative, or if the buffer factory is null.
         */
        public IntTupleBufferFactory(int tupleSize, boolean allocateDirect)
        {
            super(tupleSize, new BufferFactory.IntBufferFactory(allocateDirect));
        }

        /**
         * Constructs a new IntTupleBufferFactory with the specified tuple size and the default buffer allocation
         * policy. This factory allocates IntBufferWrappers backed by direct buffers.
         *
         * @param tupleSize the number of coordinates per logical tuple.
         */
        public IntTupleBufferFactory(int tupleSize)
        {
            this(tupleSize, true);
        }
    }

    /**
     * Implementation of TupleBufferFactory which constructs instances of {@link gov.nasa.worldwind.util.VecBuffer},
     * backed by a {@link gov.nasa.worldwind.util.BufferWrapper.FloatBufferWrapper}.
     */
    public static class FloatTupleBufferFactory extends BasicTupleBufferFactory
    {
        /**
         * Constructs a new FloatTupleBufferFactory with the specified tuple size and buffer allocation policy.
         *
         * @param tupleSize      the number of coordinates per logical tuple.
         * @param allocateDirect true to allocate FloatBufferWrappers backed by direct buffers, false to allocate
         *                       FloatBufferWrappers backed by non-direct buffers.
         *
         * @throws IllegalArgumentException is the tuple size is negative, or if the buffer factory is null.
         */
        public FloatTupleBufferFactory(int tupleSize, boolean allocateDirect)
        {
            super(tupleSize, new BufferFactory.FloatBufferFactory(allocateDirect));
        }

        /**
         * Constructs a new FloatTupleBufferFactory with the specified tuple size and the default buffer allocation
         * policy. This factory allocates FloatBufferWrappers backed by direct buffers.
         *
         * @param tupleSize the number of coordinates per logical tuple.
         */
        public FloatTupleBufferFactory(int tupleSize)
        {
            this(tupleSize, true);
        }
    }

    /**
     * Implementation of TupleBufferFactory which constructs instances of {@link gov.nasa.worldwind.util.VecBuffer},
     * backed by a {@link gov.nasa.worldwind.util.BufferWrapper.DoubleBufferWrapper}.
     */
    public static class DoubleTupleBufferFactory extends BasicTupleBufferFactory
    {
        /**
         * Constructs a new DoubleTupleBufferFactory with the specified tuple size and buffer allocation policy.
         *
         * @param tupleSize      the number of coordinates per logical tuple.
         * @param allocateDirect true to allocate DoubleBufferWrappers backed by direct buffers, false to allocate
         *                       DoubleBufferWrappers backed by non-direct buffers.
         *
         * @throws IllegalArgumentException is the tuple size is negative, or if the buffer factory is null.
         */
        public DoubleTupleBufferFactory(int tupleSize, boolean allocateDirect)
        {
            super(tupleSize, new BufferFactory.DoubleBufferFactory(allocateDirect));
        }

        /**
         * Constructs a new DoubleTupleBufferFactory with the specified tuple size and the default buffer allocation
         * policy. This factory allocates DoubleBufferWrappers backed by direct buffers.
         *
         * @param tupleSize the number of coordinates per logical tuple.
         */
        public DoubleTupleBufferFactory(int tupleSize)
        {
            this(tupleSize, true);
        }
    }
}
