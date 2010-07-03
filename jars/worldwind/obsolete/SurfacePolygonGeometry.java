/*
Copyright (C) 2001, 2008 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render;

import gov.nasa.worldwind.geom.*;

import java.awt.*;

/**
 * @author tag
 * @version $Id: SurfacePolygonGeometry.java 7434 2008-11-08 21:27:45Z tgaskins $
 */
public class SurfacePolygonGeometry extends SurfaceShapeGeometry
{

    /**
     * A Renderable polygon shape defined by a list of LatLon
     *
     * @param positions   the list of LatLon positions that makes the polygon
     * @param color       the interior fill color
     * @param borderColor the border color
     */
    public SurfacePolygonGeometry(Iterable<? extends LatLon> positions, Color color, Color borderColor)
    {
        super(positions, color, borderColor);
    }

    /**
     * A Renderable polygon shape defined by a list of LatLon
     *
     * @param positions the list of LatLon positions that makes the polygon
     */
    public SurfacePolygonGeometry(Iterable<? extends LatLon> positions)
    {
        super(positions, null, null);
    }


    /**
     * Returns the drawing LatLon relative to a given Sector and a longitude offset Can go beyond +-180 degrees
     * longitude if the offset is zero
     *
     * @param pos       the real LatLon
     * @param sector    the drawing Sector
     * @param lonOffset the current longitude offset in degrees
     * @return the appropiate drawing LatLon
     */
    private LatLon computeDrawLatLon(LatLon pos, Sector sector, double lonOffset)
    {
        int directionOffset;
        directionOffset = sector.getMaxLongitude().degrees - pos.getLongitude().getDegrees() > 180 ?
            360 : 0;
        directionOffset = pos.getLongitude().getDegrees() - sector.getMinLongitude().getDegrees() > 180 ?
            -360 : directionOffset;
        return LatLon.fromDegrees(pos.getLatitude().getDegrees(),
            pos.getLongitude().getDegrees() + directionOffset + lonOffset);
    }
}