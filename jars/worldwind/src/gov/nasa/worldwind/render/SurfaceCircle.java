/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render;

import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.util.Logging;

/**
 * @author dcollins
 * @version $Id: SurfaceCircle.java 9992 2009-04-08 04:38:54Z dcollins $
 */
public class SurfaceCircle extends SurfaceEllipse
{
    public SurfaceCircle(ShapeAttributes attributes, LatLon center, double radius, int intervals)
    {
        super(attributes, center, radius, radius, Angle.ZERO, intervals);
    }

    public SurfaceCircle(ShapeAttributes attributes, LatLon center, double radius)
    {
        this(attributes, center, radius, ELLIPSE_DEFAULT_NUM_INTERVALS);
    }

    public SurfaceCircle(ShapeAttributes attributes, int intervals)
    {
        this(attributes, LatLon.ZERO, 0, intervals);
    }

    public SurfaceCircle(ShapeAttributes attributes)
    {
        this(attributes, ELLIPSE_DEFAULT_NUM_INTERVALS);
    }

    public SurfaceCircle(LatLon center, double radius, int intervals)
    {
        this(new BasicShapeAttributes(), center, radius, intervals);
    }

    public SurfaceCircle(LatLon center, double radius)
    {
        this(center, radius, ELLIPSE_DEFAULT_NUM_INTERVALS);
    }

    public SurfaceCircle()
    {
        this(new BasicShapeAttributes(), ELLIPSE_DEFAULT_NUM_INTERVALS);
    }

    public double getRadius()
    {
        return this.getMajorRadius();
    }

    public void setRadius(double radius)
    {
        if (radius < 0)
        {
            String message = Logging.getMessage("generic.ArgumentOutOfRange", "radius < 0");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.setRadii(radius, radius);
    }
}
