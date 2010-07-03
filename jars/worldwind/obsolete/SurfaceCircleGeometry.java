/*
Copyright (C) 2001, 2008 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render;

import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.util.Logging;

import java.awt.*;

/**
 * @author tag
 * @version $Id: SurfaceCircleGeometry.java 7434 2008-11-08 21:27:45Z tgaskins $
 */
public class SurfaceCircleGeometry extends SurfaceEllipseGeometry
{
    public SurfaceCircleGeometry(Globe globe, LatLon center, double radius, int intervals)
    {
        super(globe, center, radius, radius, null, intervals, null, null);
    }

    public SurfaceCircleGeometry(Globe globe, LatLon center, double radius, int intervals, Color interiorColor,
        Color borderColor)
    {
        super(globe, center, radius, radius, null, intervals, interiorColor, borderColor);
    }

    public double getRadius()
    {
        return super.getMajorAxisLength();
    }

    public void setRadius(double radius)
    {
        if (radius <= 0)
        {
            String message = Logging.getMessage("Geom.RadiusInvalid", radius);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        super.setAxisLengths(radius, radius);
    }
}