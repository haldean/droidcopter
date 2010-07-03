/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render;

import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.util.Logging;

import java.awt.*;

/**
 * @author Jim Miller
 * @version $Id: ConformingCircle.java 7671 2008-12-08 00:18:14Z jmiller $
 */

public class ConformingCircle extends ConformingEllipse
{
    public ConformingCircle(LatLon center, double radius)
    {
        super(center, radius, radius, null, null, null);
    }

    public ConformingCircle(LatLon center, double radius, Color fillColor,
        Color borderColor)
    {
        super(center, radius, radius, null, fillColor, borderColor);
    }

    public double getRadius()
    {
        return super.getSemiMajorAxisLength();
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
