/*
Copyright (C) 2001, 2010 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/

package gov.nasa.worldwind.formats.shapefile;

import gov.nasa.worldwind.util.VecBuffer;

import java.awt.geom.*;
import java.nio.*;
import java.util.List;

/**
 * Holds the information for a single record of a MultiPoint shape.
 *
 * @author tag
 * @version $Id: ShapefileRecordMultiPoint.java 13262 2010-04-09 22:39:22Z dcollins $
 */
public class ShapefileRecordMultiPoint extends ShapefileRecord
{
    protected Rectangle2D boundingRectangle;
    protected double[] zRange; // non-null only for Z types
    protected double[] zValues; // non-null only for Z types
    protected double[] mRange; // will be null if no measures
    protected double[] mValues; // will be null if no measures

    /** {@inheritDoc} */
    public ShapefileRecordMultiPoint(Shapefile shapeFile, ByteBuffer buffer, VecBuffer pointBuffer,
        List<Integer> partsOffset, List<Integer> partsLength)
    {
        super(shapeFile, buffer, pointBuffer, partsOffset, partsLength);
    }

    public Rectangle2D getBoundingRectangle()
    {
        return this.boundingRectangle;
    }

    /**
     * Returns an iterator over all the points X and Y coordinates for a specified part of this record. Part numbers
     * start at zero.
     *
     * @param partNumber the number of the part of this record - zero based.
     *
     * @return an {@link Iterable} over the points X and Y coordinates.
     */
    public Iterable<double[]> getPoints(int partNumber)
    {
        return this.getBuffer(partNumber).getCoords();
    }

    /**
     * Returns the shape's Z range.
     *
     * @return the shape's Z range. The range minimum is at index 0, the maximum at index 1.
     */
    public double[] getZRange()
    {
        return this.zRange;
    }

    /**
     * Returns the shape's Z values.
     *
     * @return the shape's Z values.
     */
    public double[] getZValues()
    {
        return this.zValues;
    }

    /**
     * Returns the shape's optional measure range.
     *
     * @return the shape's measure range, or null if no measures are in the record. The range minimum is at index 0, the
     *         maximum at index 1.
     */
    public double[] getMRange()
    {
        return this.mRange;
    }

    /**
     * Returns the shape's optional measure values.
     *
     * @return the shape's measure values, or null if no measures are in the record.
     */
    public double[] getMValues()
    {
        return this.mValues;
    }

    /** {@inheritDoc} **/
    protected void normalizeLocations()
    {
        if (this.boundingRectangle.getX() >= -180 && this.boundingRectangle.getMaxX() <= 180)
            return;

        super.normalizeLocations();
        ShapefileUtils.normalizeRectangle(this.boundingRectangle);
    }

    /** {@inheritDoc} */
    protected void readFromBuffer(Shapefile shapefile, ByteBuffer buffer, VecBuffer pointBuffer,
        List<Integer> partsOffset, List<Integer> partsLength)
    {
        // Read record number and skip record length, big endian
        buffer.order(ByteOrder.BIG_ENDIAN);
        this.recordNumber = buffer.getInt();
        this.lengthInBytes = buffer.getInt() * 2;

        // Read shape type - little endian
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        int type = buffer.getInt();
        String shapetype = shapefile.getShapeType(type);
        this.validateShapeType(shapefile, shapetype);

        this.shapeType = shapetype;
        this.shapeFile = shapefile;

        if (shapeType.equals(Shapefile.SHAPE_NULL))
            return;

        // Bounding rectangle
        this.boundingRectangle = ShapefileUtils.readBounds(buffer, shapefile.getProjectionParams());

        // Parts and points
        this.firstPartNumber = partsOffset.size();
        this.numberOfParts = 1;
        this.numberOfPoints = buffer.getInt();

        // Put points in point buffer
        int pointBufferPosition = partsOffset.size() > 0 ?
            partsOffset.get(partsOffset.size() - 1) + partsLength.get(partsLength.size() - 1) : 0;

        ShapefileUtils.transferPoints(buffer, pointBuffer, pointBufferPosition, this.numberOfPoints);

        // Update parts offset and length lists with one part of N points
        partsOffset.add(pointBufferPosition);
        partsLength.add(this.numberOfPoints);

        if (this.isZType())
            this.readZ(buffer);

        if (this.isMeasureType())
            this.readOptionalMeasures(buffer);
    }

    /**
     * Read's the shape's Z values from the record buffer.
     *
     * @param buffer the record buffer to read from.
     */
    protected void readZ(ByteBuffer buffer)
    {
        this.zRange = ShapefileUtils.readDoubleArray(buffer, 2);
        this.zValues = ShapefileUtils.readDoubleArray(buffer, this.getNumberOfPoints());
    }

    /**
     * Reads any optional measure values from the record buffer.
     *
     * @param buffer the record buffer to read from.
     */
    protected void readOptionalMeasures(ByteBuffer buffer)
    {
        // Measure values are optional.
        if (buffer.hasRemaining() && (buffer.limit() - buffer.position()) >= (this.getNumberOfPoints() * 8))
        {
            this.mRange = ShapefileUtils.readDoubleArray(buffer, 2);
            this.mValues = ShapefileUtils.readDoubleArray(buffer, this.getNumberOfPoints());
        }
    }
}
