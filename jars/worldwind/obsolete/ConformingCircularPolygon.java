/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render;

import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.util.Logging;

import java.awt.*;

/**
 * This class provides a convenient way to create a polygon inscribed in a circle. Its
 * interface is based on the SurfaceCircle class.  The
 * constructor takes the geometric parameters of a circle along with the desired number
 * of polygon edges. Using a suficiently large number of edges results in a very good
 * approximation to a circle, <b>BUT</b> this is not recommended. If a true circle is actually
 * desired, you should use {@link gov.nasa.worldwind.render.ConformingCircle} which is
 * optimized for defining and manipulating circles. While arbitrarily large values
 * for number of polygon edges (i.e., "int intervals") works with this class, as a general rule of
 * thumb, you should carefully consider before using more than 10 or 15.
 * @author Jim Miller
 * @version $Id: ConformingCircularPolygon.java 7671 2008-12-08 00:18:14Z jmiller $
 */

public class ConformingCircularPolygon extends ConformingEllipticalPolygon
{
    public ConformingCircularPolygon(Globe globe, LatLon center, double radius, int intervals)
    {
        super(globe, center, radius, radius, null, intervals, null, null);
    }

    public ConformingCircularPolygon(Globe globe, LatLon center, double radius, int intervals, Color fillColor,
        Color borderColor)
    {
        super(globe, center, radius, radius, null, intervals, fillColor, borderColor);
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
