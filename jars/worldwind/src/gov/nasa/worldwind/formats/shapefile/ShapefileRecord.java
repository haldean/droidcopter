/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.formats.shapefile;

import gov.nasa.worldwind.exception.WWRuntimeException;
import gov.nasa.worldwind.util.*;

import java.nio.ByteBuffer;
import java.util.*;

/**
 * Represents a single record of a shapefile.
 *
 * @author Patrick Murris
 * @version $Id: ShapefileRecord.java 13118 2010-02-14 21:15:07Z tgaskins $
 */
public abstract class ShapefileRecord
{
    protected Shapefile shapeFile;
    protected int recordNumber;
    protected String shapeType;
    protected DBaseRecord attributes;

    protected int numberOfParts;
    protected int firstPartNumber;

    protected int numberOfPoints;

    protected int lengthInBytes;

    protected static List<String> measureTypes = new ArrayList<String>(Arrays.asList(
        Shapefile.SHAPE_POINT_M, Shapefile.SHAPE_POINT_Z,
        Shapefile.SHAPE_MULTI_POINT_M, Shapefile.SHAPE_MULTI_POINT_Z,
        Shapefile.SHAPE_POLYLINE_M, Shapefile.SHAPE_POLYLINE_Z,
        Shapefile.SHAPE_POLYGON_M, Shapefile.SHAPE_POLYGON_Z
    ));

    /**
     * Constructs a record instance from the given {@link java.nio.ByteBuffer}. The buffer's current position must be
     * the start of the record, and will be the start of the next record when the constructor returns.
     *
     * @param shapeFile   the parent {@link Shapefile}.
     * @param buffer      the shapefile record {@link java.nio.ByteBuffer} to read from.
     * @param pointBuffer the {@link VecBuffer} into which points are added.
     * @param partsOffset the current list of parts offset for preceding records.
     * @param partsLength the current list of parts length for preceding records.
     *
     * @throws IllegalArgumentException if any argument is null or otherwise invalid.
     * @throws gov.nasa.worldwind.exception.WWRuntimeException
     *                                  if the record's shape type does not match that of the shapefile.
     */
    public ShapefileRecord(Shapefile shapeFile, ByteBuffer buffer, VecBuffer pointBuffer,
        List<Integer> partsOffset, List<Integer> partsLength)
    {
        if (shapeFile == null)
        {
            String message = Logging.getMessage("nullValue.ShapefileIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (buffer == null)
        {
            String message = Logging.getMessage("nullValue.InputBufferIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (pointBuffer == null)
        {
            String message = Logging.getMessage("nullValue.OutputBufferIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (partsOffset == null)
        {
            String message = Logging.getMessage("nullValue.OutputBufferIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (partsLength == null)
        {
            String message = Logging.getMessage("nullValue.OutputBufferIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.readFromBuffer(shapeFile, buffer, pointBuffer, partsOffset, partsLength);
    }

    /**
     * Returns the shapefile containing this record.
     *
     * @return the shapefile containing this record.
     */
    public Shapefile getShapeFile()
    {
        return this.shapeFile;
    }

    /**
     * Returns the zero-orgin ordinal position of the record in the shapefile.
     *
     * @return the record's ordinal position in the shapefile.
     */
    public int getRecordNumber()
    {
        return this.recordNumber;
    }

    /**
     * Returns the record's shape type.
     *
     * @return the record' shape type. See {@link Shapefile} for a list of the defined shape types.
     */
    public String getShapeType()
    {
        return this.shapeType;
    }

    /**
     * Returns the record's attributes.
     *
     * @return the record's attributes.
     */
    public DBaseRecord getAttributes()
    {
        return this.attributes;
    }

    /**
     * Returns the number of parts in the record.
     *
     * @return the number of parts in the record.
     */
    public int getNumberOfParts()
    {
        return this.numberOfParts;
    }

    /**
     * Returns the first part number in the record.
     *
     * @return the first part number in the record.
     */
    public int getFirstPartNumber()
    {
        return this.firstPartNumber;
    }

    /**
     * Returns the number of points in the record.
     *
     * @return the number of points in the record.
     */
    public int getNumberOfPoints()
    {
        return this.numberOfPoints;
    }

    /**
     * Returns the number of points in a specified part of the record.
     *
     * @param partNumber the part number for which to return the number of points.
     *
     * @return the number of points in the specified part.
     */
    public int getNumberOfPoints(int partNumber)
    {
        return this.getBuffer(partNumber).getSize();
    }

    /**
     * Returns the {@link gov.nasa.worldwind.util.VecBuffer} holding the X and Y points of a specified part.
     *
     * @param partNumber the part for which to return the point buffer.
     *
     * @return the buffer holding the part's points. The points are ordered X0,Y0,X1,Y1,...Xn-1,Yn-1, where "n" is the
     *         number of points in the part.
     */
    public VecBuffer getBuffer(int partNumber)
    {
        if (partNumber < 0 || partNumber >= this.getNumberOfParts())
        {
            String message = Logging.getMessage("generic.SizeOutOfRange", partNumber);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return this.getShapeFile().getBuffer().getSubBuffer(this.getFirstPartNumber() + partNumber);
    }

    /** Modifies the longitudes of the record's points (X values) to the range [-180, 180]. */
    protected void normalizeLocations()
    {
        double[] point = new double[2];
        for (int part = 0; part < this.getNumberOfParts(); part++)
        {
            VecBuffer buffer = this.getBuffer(part);
            for (int i = 0; i < buffer.getSize(); i++)
            {
                buffer.get(i, point);
                point[0] = ShapefileUtils.normalizeLongitude(point[0]);
                buffer.put(i, point);
            }
        }
    }

    /**
     * Reads and parses the contents of a shapefile record from a specified buffer. The buffer's current position must
     * be the start of the record and will be the start of the next record when the constructor returns.
     *
     * @param shapefile   the containing {@link Shapefile}.
     * @param buffer      the shapefile record {@link java.nio.ByteBuffer} to read from.
     * @param pointBuffer the {@link VecBuffer} into which points are to be added.
     * @param partsOffset the current list of parts offset for preceding records.
     * @param partsLength the current list of parts length for preceding records.
     */
    protected abstract void readFromBuffer(Shapefile shapefile, ByteBuffer buffer, VecBuffer pointBuffer,
        List<Integer> partsOffset, List<Integer> partsLength);

    /**
     * Verifies that the record's shape type matches the expected one, typically that of the shapefile. Throws an
     * exception if the types do not match and the shape type is not {@link Shapefile#SHAPE_NULL}.
     *
     * @param shapefile the shapefile.
     * @param shapeType the record's shape type.
     *
     * @throws WWRuntimeException       if the shape types do not match.
     * @throws IllegalArgumentException if the specified shape type is null.
     */
    protected void validateShapeType(Shapefile shapefile, String shapeType)
    {
        if (shapeType == null)
        {
            String message = Logging.getMessage("nullValue.ShapeType");
            Logging.logger().log(java.util.logging.Level.SEVERE, message);
            throw new IllegalArgumentException(message);
        }

        if (!(shapeType.equals(shapefile.getShapeType()) || shapeType.equals(Shapefile.SHAPE_NULL)))
        {
            String message = Logging.getMessage("SHP.UnsupportedShapeType", shapeType);
            Logging.logger().log(java.util.logging.Level.SEVERE, message);
            throw new WWRuntimeException(message);
        }
    }

    /**
     * Indicates whether the record is a shape type capable of containing optional measure values. Does not indicate
     * whether the record actually contains measure values.
     *
     * @return true if the record may contain measure values.
     */
    protected boolean isMeasureType()
    {
        return Shapefile.isMeasureType(this.getShapeType());
    }

    /**
     * Indicates whether the record is a shape type containing Z values.
     *
     * @return true if the record is a type containing Z values.
     */
    protected boolean isZType()
    {
        return Shapefile.isZType(this.getShapeType());
    }
}