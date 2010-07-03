/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render;

import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.globes.Globe;

import java.awt.*;
import java.util.ArrayList;

/**
 * @author tag
 * @version $Id: SurfaceQuadGeometry.java 7434 2008-11-08 21:27:45Z tgaskins $
 */
public class SurfaceQuadGeometry extends SurfacePolygonGeometry
{
    private LatLon center;
    private double width;
    private double height;
    private Angle orientation;

    public SurfaceQuadGeometry(Globe globe, LatLon center, double width, double height, Angle orientation)
    {
        super(makePositions(globe, center, width, height, orientation), null, null);
        this.globe = globe;
        this.center = center;
        this.width = width;
        this.height = height;
        this.orientation = orientation;
    }

    public SurfaceQuadGeometry(Globe globe, LatLon center, double width, double height, Angle orientation, Color interiorColor,
        Color borderColor)
    {
        super(makePositions(globe, center, width, height, orientation), interiorColor, borderColor);
        this.globe = globe;
        this.center = center;
        this.width = width;
        this.height = height;
        this.orientation = orientation;
    }

    public LatLon getCenter()
    {
        return this.center;
    }

    public void setCenter(LatLon center)
    {
        this.center = center;
        this.setPositions(makePositions(this.globe, this.center, this.width, this.height, this.orientation));
    }

    public double getWidth()
    {
        return width;
    }

    public double getHeight()
    {
        return height;
    }

    public void setSize(double width, double height)
    {
        this.width = width;
        this.height = height;
        this.setPositions(makePositions(this.globe, this.center, this.width, this.height, this.orientation));
    }

    public Angle getOrientation()
    {
        return orientation;
    }

    public void setOrientation(Angle orientation)
    {
        this.orientation = orientation;
        this.setPositions(makePositions(this.globe, this.center, this.width, this.height, this.orientation));
    }

    private static Iterable<LatLon> makePositions(Globe globe, LatLon center, double width, double height,
        Angle orientation)
    {
        if (orientation == null)
            orientation = Angle.ZERO;

        if (globe == null)
        {
            String message = Logging.getMessage("nullValue.GlobeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (center == null)
        {
            String message = Logging.getMessage("nullValue.CenterIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (width <= 0)
        {
            String message = Logging.getMessage("Geom.WidthInvalid", width);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (height <= 0)
        {
            String message = Logging.getMessage("Geom.HeightInvalid", height);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        double[] cornerAngles = new double[]
            {
                Math.atan2(height, width),
                Math.atan2(height, -width),
                Math.atan2(-height, -width),
                Math.atan2(-height, width),
                Math.atan2(height, width),
            };

        final ArrayList<LatLon> positions = new ArrayList<LatLon>(cornerAngles.length);
        double radius = globe.getRadiusAt(center.getLatitude(), center.getLongitude());
        double distance = Math.sqrt(width * width + height * height);
        for (double cornerAngle : cornerAngles)
        {
            double azimuth = Math.PI / 2 - (cornerAngle + orientation.radians);
            LatLon p = LatLon.greatCircleEndPosition(center, azimuth, distance / radius);
            positions.add(p);
        }

        return positions;
    }
}