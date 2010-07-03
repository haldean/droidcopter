/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.RestorableSupport;

import java.awt.*;
import java.util.ArrayList;

/**
 * This class provides a convenient way to create a polygon inscribed in an ellipse. Its
 * interface is basd on the SurfaceEllipse class.  The
 * constructor takes the geometric parameters of an ellipse along with the desired number
 * of polygon edges. Using a suficiently large number of edges results in a very good
 * approximation to an ellipse, <b>BUT</b> this is not recommended. If a true ellipse is actually
 * desired, you should use {@link gov.nasa.worldwind.render.ConformingEllipse} which is
 * optimized for defining and manipulating ellipses. While arbitrarily large values
 * for number of polygon edges (i.e., "int intervals") works with this class, as a general rule of
 * thumb, you should carefully consider before using more than 10 or 15.
 * @author Jim Miller
 * @version $Id: ConformingEllipticalPolygon.java 7671 2008-12-08 00:18:14Z jmiller $
 */

// The bulk of the source here was copied and adapted from SurfaceEllipse.java

public class ConformingEllipticalPolygon extends ConformingPolygon
{
    private LatLon center;
    private double semiMajorAxisLength;
    private double semiMinorAxisLength;
    private Angle orientation;
    private int intervals;

    public ConformingEllipticalPolygon(Globe globe, LatLon center,
        double semiMajorAxisLength, double semiMinorAxisLength, Angle orientation,
        int intervals)
    {
        super(globe,
            makePositions(globe, center, semiMajorAxisLength, semiMinorAxisLength, orientation, intervals),
            null, null, false);
        this.center = center;
        this.semiMajorAxisLength = semiMajorAxisLength;
        this.semiMinorAxisLength = semiMinorAxisLength;
        this.orientation = orientation;
        this.intervals = intervals;
    }

    public ConformingEllipticalPolygon(Globe globe, LatLon center,
        double semiMajorAxisLength, double semiMinorAxisLength, Angle orientation,
        int intervals, Color fillColor, Color borderColor)
    {
        super(globe,
            makePositions(globe, center, semiMajorAxisLength, semiMinorAxisLength, orientation, intervals),
            fillColor, borderColor, false);
        this.center = center;
        this.semiMajorAxisLength = semiMajorAxisLength;
        this.semiMinorAxisLength = semiMinorAxisLength;
        this.orientation = orientation;
        this.intervals = intervals;
    }

    @Override
    protected void doGetRestorableState(RestorableSupport rs, RestorableSupport.StateObject context)
    {
        super.doGetRestorableState(rs, context);

        rs.addStateValueAsDouble(context, "semiMajorAxisLength", this.getSemiMajorAxisLength());
        rs.addStateValueAsDouble(context, "semiMinorAxisLength", this.getSemiMinorAxisLength());
        rs.addStateValueAsLatLon(context, "center", this.getCenter());
        rs.addStateValueAsInteger(context, "intervals", this.getIntervals());
        rs.addStateValueAsDouble(context, "orientationDegrees", this.getOrientation().degrees);
    }

    @Override
    protected void doRestoreState(RestorableSupport rs, RestorableSupport.StateObject context)
    {
        super.doRestoreState(rs, context);

        Double major = rs.getStateValueAsDouble(context, "semiMajorAxisLength");
        Double minor = rs.getStateValueAsDouble(context, "semiMinorAxisLength");
        if (major != null && minor != null)
            this.setAxisLengths(major, minor);

        LatLon center = rs.getStateValueAsLatLon(context, "center");
        if (center != null)
            this.setCenter(center);

        Integer intervals = rs.getStateValueAsInteger(context, "intervals");
        if (intervals != null)
            this.setIntervals(intervals);

        Double od = rs.getStateValueAsDouble(context, "orientationDegrees");
        if (od != null)
            this.setOrientation(Angle.fromDegrees(od));
    }

    public LatLon getCenter()
    {
        return this.center;
    }

    public void setCenter(LatLon center)
    {
        this.center = center;
        this.setOriginalVertices(
            makePositions(this.globe, this.center, this.semiMajorAxisLength, this.semiMajorAxisLength, this.orientation,
                this.intervals));
    }

    public double getSemiMajorAxisLength()
    {
        return semiMajorAxisLength;
    }

    public double getSemiMinorAxisLength()
    {
        return semiMinorAxisLength;
    }

    public void setAxisLengths(double majorAxisLength, double minorAxisLength)
    {
        this.semiMajorAxisLength = majorAxisLength;
        this.semiMinorAxisLength = minorAxisLength;
        this.setOriginalVertices(
            makePositions(this.globe, this.center, this.semiMajorAxisLength, this.semiMinorAxisLength, this.orientation,
                this.intervals));
    }

    public Angle getOrientation()
    {
        return orientation;
    }

    public void setOrientation(Angle orientation)
    {
        this.orientation = orientation;
        this.setOriginalVertices(
            makePositions(this.globe, this.center, this.semiMajorAxisLength, this.semiMinorAxisLength, this.orientation,
                this.intervals));
    }

    public int getIntervals()
    {
        return intervals;
    }

    public void setIntervals(int intervals)
    {
        this.intervals = intervals;
        this.setOriginalVertices(
            makePositions(this.globe, this.center, this.semiMajorAxisLength, this.semiMinorAxisLength, this.orientation,
                this.intervals));
    }

    public static Iterable<LatLon> makePositions(Globe globe, LatLon center, double majorAxis, double minorAxis,
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
