/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.formats.shapefile;

import gov.nasa.worldwind.util.VecBuffer;

import java.nio.*;
import java.util.List;

/**
 * Holds the information for a single record of a Point shape.
 *
 * @author Patrick Murris
 * @version $Id: ShapefileRecordPoint.java 13263 2010-04-09 22:40:19Z dcollins $
 */
public class ShapefileRecordPoint extends ShapefileRecord
{
    protected Double z; // non-null only for Z types
    protected Double m; // non-null only for Measure types with measures specified

    /** {@inheritDoc} */
    public ShapefileRecordPoint(Shapefile shapeFile, ByteBuffer buffer, VecBuffer pointBuffer,
        List<Integer> partsOffset, List<Integer> partsLength)
    {
        super(shapeFile, buffer, pointBuffer, partsOffset, partsLength);
    }

    /**
     * Get the point X and Y coordinates.
     *
     * @return the point X and Y coordinates.
     */
    public double[] getPoint()
    {
        VecBuffer pointBuffer = this.getShapeFile().getBuffer().getSubBuffer(this.getFirstPartNumber());
        Iterable<double[]> iterable = pointBuffer.getCoords();

        return iterable.iterator().next();
    }

    /**
     * Returns the shape's Z value.
     *
     * @return the shape's Z value.
     */
    public Double getZ()
    {
        return this.z;
    }

    /**
     * Return the shape's optional measure value.
     *
     * @return the shape's measure, or null if no measure is in the record.
     */
    public Double getM()
    {
        return this.m;
    }

    /** {@inheritDoc} */
    protected void readFromBuffer(Shapefile shapefile, ByteBuffer buffer, VecBuffer pointBuffer,
        List<Integer> partsOffset, List<Integer> partsLength)
    {
        int pointBufferPosition = partsOffset.size() > 0 ?
            partsOffset.get(partsOffset.size() - 1) + partsLength.get(partsLength.size() - 1) : 0;

        // Read record number and skip record length, big endian
        buffer.order(ByteOrder.BIG_ENDIAN);
        this.recordNumber = buffer.getInt();
        this.lengthInBytes = buffer.getInt() * 2;

        // Read shape type - little endian
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        int type = buffer.getInt();
        String shapeType = shapefile.getShapeType(type);
        this.validateShapeType(shapefile, shapeType);

        // Set record state
        this.shapeType = shapeType;
        this.shapeFile = shapefile;
        this.firstPartNumber = partsOffset.size();
        this.numberOfParts = 1;

        if (shapeType.equals(Shapefile.SHAPE_NULL))
            return;

        // Read point X and Y and add to point buffer
        ShapefileUtils.transferPoints(buffer, pointBuffer, pointBufferPosition, 1);

        // Update parts offset and length lists with one part of one point
        partsOffset.add(pointBufferPosition);
        partsLength.add(1);

        if (this.isZType())
            this.readZ(buffer);

        if (this.isMeasureType())
            this.readOptionalMeasure(buffer);
    }

    /**
     * Read the record's Z value from the record buffer.
     *
     * @param buffer the record to read from.
     */
    protected void readZ(ByteBuffer buffer)
    {
        double[] zArray = ShapefileUtils.readDoubleArray(buffer, 1);
        this.z = zArray[0];
    }

    /**
     * Read any optional measure values from the record.
     *
     * @param buffer the record buffer to read from.
     */
    protected void readOptionalMeasure(ByteBuffer buffer)
    {
        // Measure values are optional.
        if (buffer.hasRemaining() && (buffer.limit() - buffer.position()) >= 8)
        {
            double[] mArray = ShapefileUtils.readDoubleArray(buffer, 1);
            this.m = mArray[0];
        }
    }
}