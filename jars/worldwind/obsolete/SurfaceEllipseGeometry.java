/*
Copyright (C) 2001, 2008 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render;

import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.util.Logging;

import java.awt.*;
import java.util.ArrayList;

/**
 * @author tag
 * @version $Id: SurfaceEllipseGeometry.java 7434 2008-11-08 21:27:45Z tgaskins $
 */
public class SurfaceEllipseGeometry extends SurfacePolygonGeometry
{
    private LatLon center;
    private double majorAxisLength;
    private double minorAxisLength;
    private Angle orientation;
    private int intervals;

    public SurfaceEllipseGeometry(Globe globe, LatLon center, double majorAxisLength, double minorAxisLength, Angle orientation,
        int intervals)
    {
        super(makePositions(globe, center, majorAxisLength, minorAxisLength, orientation, intervals), null, null);
        this.globe = globe;
        this.center = center;
        this.majorAxisLength = majorAxisLength;
        this.minorAxisLength = minorAxisLength;
        this.orientation = orientation;
        this.intervals = intervals;
    }

    public SurfaceEllipseGeometry(Globe globe, LatLon center, double majorAxisLength, double minorAxisLength, Angle orientation,
        int intervals, Color interiorColor, Color borderColor)
    {
        super(makePositions(globe, center, majorAxisLength, minorAxisLength, orientation, intervals), interiorColor,
            borderColor);
        this.globe = globe;
        this.center = center;
        this.majorAxisLength = majorAxisLength;
        this.minorAxisLength = minorAxisLength;
        this.orientation = orientation;
        this.intervals = intervals;
    }

    public LatLon getCenter()
    {
        return this.center;
    }

    public void setCenter(LatLon center)
    {
        this.center = center;
        this.setPositions(
            makePositions(this.globe, this.center, this.majorAxisLength, this.majorAxisLength, this.orientation,
                this.intervals));
    }

    public double getMajorAxisLength()
    {
        return majorAxisLength;
    }

    public double getMinorAxisLength()
    {
        return minorAxisLength;
    }

    public void setAxisLengths(double majorAxisLength, double minorAxisLength)
    {
        this.majorAxisLength = majorAxisLength;
        this.minorAxisLength = minorAxisLength;
        this.setPositions(
            makePositions(this.globe, this.center, this.majorAxisLength, this.minorAxisLength, this.orientation,
                this.intervals));
    }

    public Angle getOrientation()
    {
        return orientation;
    }

    public void setOrientation(Angle orientation)
    {
        this.orientation = orientation;
        this.setPositions(
            makePositions(this.globe, this.center, this.majorAxisLength, this.minorAxisLength, this.orientation,
                this.intervals));
    }

    public int getIntervals()
    {
        return intervals;
    }

    public void setIntervals(int intervals)
    {
        this.intervals = intervals;
        this.setPositions(
            makePositions(this.globe, this.center, this.majorAxisLength, this.minorAxisLength, this.orientation,
                this.intervals));
    }

    private static Iterable<LatLon> makePositions(Globe globe, LatLon center, double majorAxis, double minorAxis,
        Angle orientation, int intervals)
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

        if (majorAxis <= 0)
        {
            String message = Logging.getMessage("Geom.MajorAxisInvalid", majorAxis);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (minorAxis <= 0)
        {
            String message = Logging.getMessage("Geom.MajorAxisInvalid", minorAxis);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        int numPositions = 1 + Math.max(intervals, 4);
        final ArrayList<LatLon> positions = new ArrayList<LatLon>();

        double radius = globe.getRadiusAt(center.getLatitude(), center.getLongitude());
        double da = 2 * Math.PI / (numPositions - 1);
        for (int i = 0; i < numPositions; i++)
        {
            // azimuth runs positive clockwise from north and through 360 degrees.
            double angle = (i != numPositions - 1) ? i * da : 0;
            double xLength = majorAxis * Math.cos(angle);
            double yLength = minorAxis * Math.sin(angle);
            double distance = Math.sqrt(xLength * xLength + yLength * yLength);
            double azimuth = Math.PI / 2 - (Math.acos(xLength/distance) * Math.signum(yLength) + orientation.radians);
            LatLon p = LatLon.greatCircleEndPosition(center, azimuth, distance / radius);
            positions.add(p);
        }

        return positions;
    }
}