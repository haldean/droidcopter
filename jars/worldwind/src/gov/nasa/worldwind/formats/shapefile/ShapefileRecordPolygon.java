/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.formats.shapefile;

import gov.nasa.worldwind.util.VecBuffer;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * Holds the information for a single record of a Polygon shape.
 *
 * @author Patrick Murris
 * @version $Id: ShapefileRecordPolygon.java 13118 2010-02-14 21:15:07Z tgaskins $
 */
public class ShapefileRecordPolygon extends ShapefileRecordPolyline
{
    /** {@inheritDoc} */
    public ShapefileRecordPolygon(Shapefile shapeFile, ByteBuffer buffer, VecBuffer pointBuffer,
        List<Integer> partsOffset, List<Integer> partsLength)
    {
        super(shapeFile, buffer, pointBuffer, partsOffset, partsLength);
    }
}
