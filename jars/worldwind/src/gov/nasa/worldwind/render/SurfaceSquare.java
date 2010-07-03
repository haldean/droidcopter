/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render;

import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.util.Logging;

/**
 * @author dcollins
 * @version $Id: SurfaceSquare.java 9992 2009-04-08 04:38:54Z dcollins $
 */
public class SurfaceSquare extends SurfaceQuad
{
    public SurfaceSquare(ShapeAttributes attributes, LatLon center, double size, Angle heading)
    {
        super(attributes, center, size, size, heading);
    }

    public SurfaceSquare(ShapeAttributes attributes)
    {
        this(attributes, LatLon.ZERO, 0, Angle.ZERO);
    }

    public SurfaceSquare(LatLon center, double size, Angle heading)
    {
        this(new BasicShapeAttributes(), center, size, heading);
    }

    public SurfaceSquare(LatLon center, double size)
    {
        this(center, size, Angle.ZERO);
    }

    public SurfaceSquare()
    {
        this(new BasicShapeAttributes());
    }

    public double getSize()
    {
        return this.getWidth();
    }

    public void setSize(double size)
    {
        if (size < 0)
        {
            String message = Logging.getMessage("generic.ArgumentOutOfRange", "size < 0");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.setSize(size, size);
    }
}
