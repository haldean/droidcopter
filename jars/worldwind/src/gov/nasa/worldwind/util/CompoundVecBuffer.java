/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.util;

import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.render.DrawContext;

import javax.media.opengl.GL;
import java.nio.IntBuffer;
import java.util.Iterator;

/**
 * An implementation of {@link CompoundBuffer} which manages a collection of {@link VecBuffer} buffers in a single
 * {@link VecBuffer}.
 *
 * @author dcollins
 * @version $Id: CompoundVecBuffer.java 13324 2010-04-21 23:21:37Z dcollins $
 */
public class CompoundVecBuffer extends CompoundBuffer<VecBuffer, VecBuffer>
{
    protected BufferFactory bufferFactory;

    /**
     * Constructs a CompoundVecBuffer with the specified coordsPerElem, bufferFactory, and initial capacity.
     *
     * @param coordsPerVec    the number of coordinates per logical vector.
     * @param factory         the {@link BufferFactory} which creates backing data for the internal VecBuffer.
     * @param initialCapacity the compound buffer's initial capacity.
     * @param subBufferLength an estimate of the size of each sub-sequence.
     *
     * @throws IllegalArgumentException if the initialCapacity is negative, or if the factory is null.
     */
    public CompoundVecBuffer(int coordsPerVec, BufferFactory factory, int initialCapacity, int subBufferLength)
    {
        super(initialCapacity, new VecBuffer(coordsPerVec, initialCapacity * subBufferLength, factory));
        this.bufferFactory = factory;
    }

    /**
     * Creates a CompoundBuffer with the specified backing buffer, sub-buffer starting positions, sub-buffer lengths,
     * and number of sub-buffers.
     *
     * @param buffer            the backing buffer,
     * @param subPositionBuffer the sub-buffer logical starting positions.
     * @param subLengthBuffer   the sub-buffer logical sizes.
     * @param numSubBuffers     the number of sub-buffers.
     * @param factory           the {@link BufferFactory} which creates backing data for the internal VecBuffer.
     *
     * @throws IllegalArgumentException if any of the buffer, subPositionBuffer, subLengthBuffer are null, if
     *                                  numSubBuffers is negative.
     */
    public CompoundVecBuffer(VecBuffer buffer, IntBuffer subPositionBuffer, IntBuffer subLengthBuffer,
        int numSubBuffers, BufferFactory factory)
    {
        super(buffer.getSize(), buffer, subPositionBuffer, subLengthBuffer, numSubBuffers);
        this.bufferFactory = factory;
    }

    /**
     * Returns the {@link BufferFactory} which creates backing data for the internal VecBuffer.
     *
     * @return the {@link BufferFactory} which creates backing data for the internal VecBuffer.
     */
    public BufferFactory getBufferFactory()
    {
        return this.bufferFactory;
    }

    /**
     * Returns the number of coordinates per logical vector element.
     *
     * @return the cardinality of a logical vector element.
     */
    public int getCoordsPerVec()
    {
        return this.buffer.getCoordsPerVec();
    }

    /**
     * Returns an iterator over this buffer's logical vectors, as double[] coordinate arrays. The array returned from
     * each call to Iterator.next() will be newly allocated, and will have length equal to coordsPerVec.
     *
     * @return iterator over this buffer's vectors, as double[] arrays.
     */
    public Iterable<double[]> getCoords()
    {
        return this.getCoords(this.getCoordsPerVec());
    }

    /**
     * Returns an iterator over this buffer's logical vectors, as double[] coordinate arrays. The array returned from a
     * call to Iterator.next() will be newly allocated, and will have length equal to coordsPerVec or minCoordsPerVec,
     * whichever is larger. If minCoordsPerVec is larger than coordsPerVec, then the elements in the returned array will
     * after index "coordsPerVec - 1" will be undefined.
     *
     * @param minCoordsPerVec the minimum number of coordinates returned in each double[] array.
     *
     * @return iterator over this buffer's vectors, as double[] arrays.
     */
    public Iterable<double[]> getCoords(final int minCoordsPerVec)
    {
        return new Iterable<double[]>()
        {
            public Iterator<double[]> iterator()
            {
                return new CompoundIterator<double[]>(new CoordIterable(minCoordsPerVec));
            }
        };
    }

    /**
     * Returns a reverse iterator over this buffer's logical vectors, as double[] coordinate arrays. The array returned
     * from a call to Iterator.next() will be newly allocated, and will have length equal to coordsPerVec or
     * minCoordsPerVec, whichever is larger. If minCoordsPerVec is larger than coordsPerVec, then the elements in the
     * returned array will after index "coordsPerVec - 1" will be undefined.
     *
     * @param minCoordsPerVec the minimum number of coordinates returned in each double[] array.
     *
     * @return reverse iterator over this buffer's vectors, as double[] arrays.
     */
    public Iterable<double[]> getReverseCoords(final int minCoordsPerVec)
    {
        return new Iterable<double[]>()
        {
            public Iterator<double[]> iterator()
            {
                return new ReverseCompoundIterator<double[]>(new CoordIterable(minCoordsPerVec));
            }
        };
    }

    /**
     * Returns an iterator over this buffer's logical vectors, as Vec4 references.
     *
     * @return iterator over this buffer's vectors, as Vec4 references.
     */
    public Iterable<? extends Vec4> getVectors()
    {
        return new Iterable<Vec4>()
        {
            public Iterator<Vec4> iterator()
            {
                return new CompoundIterator<Vec4>(new VectorIterable());
            }
        };
    }

    /**
     * Returns a reverse iterator over this buffer's logical vectors, as Vec4 references.
     *
     * @return reverse iterator over this buffer's vectors, as Vec4 references.
     */
    public Iterable<? extends Vec4> getReverseVectors()
    {
        return new Iterable<Vec4>()
        {
            public Iterator<Vec4> iterator()
            {
                return new ReverseCompoundIterator<Vec4>(new VectorIterable());
            }
        };
    }

    /**
     * Returns an iterator over this buffer's logical vectors, as LatLon locations.
     *
     * @return iterator over this buffer's vectors, as LatLon locations.
     */
    public Iterable<? extends LatLon> getLocations()
    {
        return new Iterable<LatLon>()
        {
            public Iterator<LatLon> iterator()
            {
                return new CompoundIterator<LatLon>(new LocationIterable());
            }
        };
    }

    /**
     * Returns a reverse iterator over this buffer's logical vectors, as LatLon locations.
     *
     * @return reverse iterator over this buffer's vectors, as LatLon locations.
     */
    public Iterable<? extends LatLon> getReverseLocations()
    {
        return new Iterable<LatLon>()
        {
            public Iterator<LatLon> iterator()
            {
                return new ReverseCompoundIterator<LatLon>(new LocationIterable());
            }
        };
    }

    /**
     * Returns an iterator over this buffer's logical vectors, as geographic Positions.
     *
     * @return iterator over this buffer's vectors, as geographic Positions.
     */
    public Iterable<? extends Position> getPositions()
    {
        return new Iterable<Position>()
        {
            public Iterator<Position> iterator()
            {
                return new CompoundIterator<Position>(new PositionIterable());
            }
        };
    }

    /**
     * Returns a reverse iterator over this buffer's logical vectors, as geographic Positions.
     *
     * @return reverse iterator over this buffer's vectors, as geographic Positions.
     */
    public Iterable<? extends Position> getReversePositions()
    {
        return new Iterable<Position>()
        {
            public Iterator<Position> iterator()
            {
                return new ReverseCompoundIterator<Position>(new PositionIterable());
            }
        };
    }

    /**
     * Binds this buffer as the source of normal coordinates to use when rendering OpenGL primitives. The normal type is
     * equal to buffer's underlying BufferWrapper GL type, the stride is 0, and the vertex data itself is this buffer's
     * backing NIO {@link java.nio.Buffer}. This buffer's vector size must be 3.
     *
     * @param dc the current {@link DrawContext}.
     *
     * @throws IllegalArgumentException if the DrawContext is null, or if this buffer is not compatible as a normal
     *                                  buffer.
     */
    public void bindAsNormalBuffer(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.buffer.bindAsNormalBuffer(dc);
    }

    /**
     * Binds this buffer as the source of vertex coordinates to use when rendering OpenGL primitives. The vertex size is
     * equal to coordsPerVertex, the vertex type is equal to buffer's underlying BufferWrapper GL type, the stride is 0,
     * and the normal data itself is this buffer's backing NIO Buffer. This buffer's vector size must be 2, 3, or 4.
     *
     * @param dc the current DrawContext.
     *
     * @throws IllegalArgumentException if the DrawContext is null, or if this buffer is not compatible as a vertex
     *                                  buffer.
     */
    public void bindAsVertexBuffer(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.buffer.bindAsVertexBuffer(dc);
    }

    /**
     * Binds this buffer as the source of texture coordinates to use when rendering OpenGL primitives.  The texture
     * coordinate size is equal to coordsPerVertex, the texture coordinate type is equal to buffer's underlying
     * BufferWrapper GL type, the stride is 0, and the texture coordinate data itself is this buffer's backing NIO
     * Buffer. This buffer's vector size must be 1, 2, 3, or 4.
     *
     * @param dc the current DrawContext.
     *
     * @throws IllegalArgumentException if the DrawContext is null, or if this buffer is not compatible as a normal
     *                                  buffer.
     */
    public void bindAsTexCoordBuffer(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.buffer.bindAsTexCoordBuffer(dc);
    }

    /**
     * Renders <code>getTotalBufferSize()</code> elements from the currently bounds OpenGL coordinate buffers, beginning
     * with element 0. The specified drawMode indicates which type of OpenGL primitives to render.
     *
     * @param dc       the current DrawContext.
     * @param drawMode the type of OpenGL primtives to render.
     *
     * @throws IllegalArgumentException if the DrawContext is null.
     */
    public void drawArrays(DrawContext dc, int drawMode)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.buffer.drawArrays(dc, drawMode);
    }

    /**
     * Renders elements from the currently bounds OpenGL coordinate buffers. This behaves exactly like {@link
     * #drawArrays(gov.nasa.worldwind.render.DrawContext, int)}, except that each sub-buffer is rendered independently.
     * The specified drawMode indicates which type of OpenGL primitives to render.
     *
     * @param dc       the current DrawContext.
     * @param drawMode the type of OpenGL primtives to render.
     *
     * @throws IllegalArgumentException if the DrawContext is null.
     */
    public void multiDrawArrays(DrawContext dc, int drawMode)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        GL gl = dc.getGL();

        if (this.haveMultiDrawArrays(dc))
        {
            gl.glMultiDrawArrays(drawMode, this.subPositionBuffer, this.subLengthBuffer, this.numSubBuffers);
        }
        else
        {
            for (int i = 0; i < this.numSubBuffers; i++)
            {
                gl.glDrawArrays(drawMode, this.subPositionBuffer.get(i), this.subLengthBuffer.get(i));
            }
        }
    }

    protected boolean haveMultiDrawArrays(DrawContext dc)
    {
        return dc.getGL().isFunctionAvailable("glMultiDrawArrays");
    }

    protected int getSize(VecBuffer buffer)
    {
        return buffer.getSize();
    }

    protected VecBuffer getSubBuffer(int position, int length)
    {
        return this.buffer.getSubBuffer(position, length);
    }

    protected CompoundBuffer<VecBuffer, VecBuffer> createSubCollection(CompoundBuffer<VecBuffer, VecBuffer> source,
        IntBuffer subPositionBuffer, IntBuffer subLengthBuffer, int numSubBuffers)
    {
        return new CompoundVecBuffer(source.getBackingBuffer(), subPositionBuffer, subLengthBuffer, numSubBuffers,
            null);
    }

    protected void append(VecBuffer buffer)
    {
        if (this.bufferFactory == null)
        {
            String message = Logging.getMessage("nullValue.FactoryIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        int minSize = this.size + buffer.getSize();
        if (this.buffer.getSize() < minSize)
        {
            int newSize = this.computeNewSize(this.buffer.getSize(), minSize);
            int coordsPerElem = this.buffer.getCoordsPerVec();
            BufferWrapper newBuffer = WWBufferUtil.copyOf(this.buffer.getBufferWrapper(), coordsPerElem * newSize,
                this.bufferFactory);
            this.buffer = new VecBuffer(coordsPerElem, newBuffer);
        }

        this.buffer.putSubBuffer(this.size, buffer);
    }

    //**************************************************************//
    //********************  Iterators  *****************************//
    //**************************************************************//

    protected class CompoundIterator<T> implements Iterator<T>
    {
        protected int subBuffer = -1;
        protected SubBufferIterable<T> subBufferIterable;
        protected Iterator<T> subIterator;

        protected CompoundIterator(SubBufferIterable<T> subBufferIterable)
        {
            this.subBufferIterable = subBufferIterable;
        }

        public boolean hasNext()
        {
            return (this.subBuffer < numSubBuffers - 1)
                || (this.subIterator == null || this.subIterator.hasNext());
        }

        public T next()
        {
            if (this.subIterator == null || !this.subIterator.hasNext())
            {
                this.subIterator = this.subBufferIterable.iterator(++this.subBuffer);
            }

            return this.subIterator.next();
        }

        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }

    protected class ReverseCompoundIterator<T> implements Iterator<T>
    {
        protected int subBuffer = numSubBuffers;
        protected SubBufferIterable<T> subBufferIterable;
        protected Iterator<T> subIterator;

        public ReverseCompoundIterator(SubBufferIterable<T> subBufferIterable)
        {
            this.subBufferIterable = subBufferIterable;
        }

        public boolean hasNext()
        {
            return (this.subBuffer > 0)
                || (this.subIterator == null || this.subIterator.hasNext());
        }

        public T next()
        {
            if (this.subIterator == null || !this.subIterator.hasNext())
            {
                this.subIterator = this.subBufferIterable.reverseIterator(--this.subBuffer);
            }

            return this.subIterator.next();
        }

        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }

    protected interface SubBufferIterable<T>
    {
        Iterator<T> iterator(int index);

        Iterator<T> reverseIterator(int index);
    }

    protected class CoordIterable implements SubBufferIterable<double[]>
    {
        private int minCoordsPerVec;

        public CoordIterable(int minCoordsPerVec)
        {
            this.minCoordsPerVec = minCoordsPerVec;
        }

        public Iterator<double[]> iterator(int index)
        {
            return getSubBuffer(index).getCoords(this.minCoordsPerVec).iterator();
        }

        public Iterator<double[]> reverseIterator(int index)
        {
            return getSubBuffer(index).getReverseCoords(this.minCoordsPerVec).iterator();
        }
    }

    protected class VectorIterable implements SubBufferIterable<Vec4>
    {
        public Iterator<Vec4> iterator(int index)
        {
            return getSubBuffer(index).getVectors().iterator();
        }

        public Iterator<Vec4> reverseIterator(int index)
        {
            return getSubBuffer(index).getReverseVectors().iterator();
        }
    }

    protected class LocationIterable implements SubBufferIterable<LatLon>
    {
        public Iterator<LatLon> iterator(int index)
        {
            return getSubBuffer(index).getLocations().iterator();
        }

        public Iterator<LatLon> reverseIterator(int index)
        {
            return getSubBuffer(index).getReverseLocations().iterator();
        }
    }

    protected class PositionIterable implements SubBufferIterable<Position>
    {
        public Iterator<Position> iterator(int index)
        {
            return getSubBuffer(index).getPositions().iterator();
        }

        public Iterator<Position> reverseIterator(int index)
        {
            return getSubBuffer(index).getReversePositions().iterator();
        }
    }
}
