/*
Copyright (C) 2001, 2010 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render;

import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.util.*;

/**
 * @author dcollins
 * @version $Id: SurfaceEllipse.java 13349 2010-04-28 00:04:34Z dcollins $
 */
public class SurfaceEllipse extends SurfaceConcaveShape
{
    protected static final int ELLIPSE_MIN_NUM_INTERVALS = 8;
    protected static final int ELLIPSE_DEFAULT_NUM_INTERVALS = 32;

    protected LatLon center;
    protected double majorRadius;
    protected double minorRadius;
    protected Angle heading;
    private int intervals;

    public SurfaceEllipse(ShapeAttributes attributes, LatLon center, double majorRadius, double minorRadius,
        Angle heading, int intervals)
    {
        super(attributes);

        if (center == null)
        {
            String message = Logging.getMessage("nullValue.CenterIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (majorRadius < 0)
        {
            String message = Logging.getMessage("generic.ArgumentOutOfRange", "majorRadius < 0");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (minorRadius < 0)
        {
            String message = Logging.getMessage("generic.ArgumentOutOfRange", "minorRadius < 0");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (heading == null)
        {
            String message = Logging.getMessage("nullValue.HeadingIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (intervals < ELLIPSE_MIN_NUM_INTERVALS)
        {
            String message = Logging.getMessage("generic.ArgumentOutOfRange",
                "intervals < " + ELLIPSE_MIN_NUM_INTERVALS);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.center = center;
        this.majorRadius = majorRadius;
        this.minorRadius = minorRadius;
        this.heading = heading;
        this.intervals = intervals;
    }

    public SurfaceEllipse(ShapeAttributes attributes, LatLon center, double majorRadius, double minorRadius,
        Angle heading)
    {
        this(attributes, center, majorRadius, minorRadius, heading, ELLIPSE_DEFAULT_NUM_INTERVALS);
    }

    public SurfaceEllipse(ShapeAttributes attributes, int intervals)
    {
        this(attributes, LatLon.ZERO, 0, 0, Angle.ZERO, intervals);
    }

    public SurfaceEllipse(ShapeAttributes attributes)
    {
        this(attributes, ELLIPSE_DEFAULT_NUM_INTERVALS);
    }

    public SurfaceEllipse(LatLon center, double majorRadius, double minorRadius, Angle heading,
        int intervals)
    {
        this(new BasicShapeAttributes(), center, majorRadius, minorRadius, heading, intervals);
    }

    public SurfaceEllipse(LatLon center, double majorRadius, double minorRadius, Angle heading)
    {
        this(center, majorRadius, minorRadius, heading, ELLIPSE_DEFAULT_NUM_INTERVALS);
    }

    public SurfaceEllipse()
    {
        this(new BasicShapeAttributes(), ELLIPSE_DEFAULT_NUM_INTERVALS);
    }

    public LatLon getCenter()
    {
        return this.center;
    }

    public void setCenter(LatLon center)
    {
        if (center == null)
        {
            String message = Logging.getMessage("nullValue.CenterIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.center = center;
        this.onShapeChanged();
    }

    public double getMajorRadius()
    {
        return this.majorRadius;
    }

    public double getMinorRadius()
    {
        return this.minorRadius;
    }

    public void setMajorRadius(double radius)
    {
        if (radius < 0)
        {
            String message = Logging.getMessage("generic.ArgumentOutOfRange", "radius < 0");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.majorRadius = radius;
        this.onShapeChanged();
    }

    public void setMinorRadius(double radius)
    {
        if (radius < 0)
        {
            String message = Logging.getMessage("generic.ArgumentOutOfRange", "radius < 0");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.minorRadius = radius;
        this.onShapeChanged();
    }

    public void setRadii(double majorRadius, double minorRadius)
    {
        this.setMajorRadius(majorRadius);
        this.setMinorRadius(minorRadius);
    }

    public Angle getHeading()
    {
        return this.heading;
    }

    public void setHeading(Angle heading)
    {
        if (heading == null)
        {
            String message = Logging.getMessage("nullValue.HeadingIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.heading = heading;
        this.onShapeChanged();
    }

    public int getIntervals()
    {
        return this.intervals;
    }

    public void setIntervals(int intervals)
    {
        if (intervals < ELLIPSE_MIN_NUM_INTERVALS)
        {
            String message = Logging.getMessage("generic.ArgumentOutOfRange",
                "intervals < " + ELLIPSE_MIN_NUM_INTERVALS);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.intervals = intervals;
        this.onShapeChanged();
    }

    public Iterable<? extends LatLon> getLocations(Globe globe)
    {
        if (globe == null)
        {
            String message = Logging.getMessage("nullValue.GlobeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return this.computeLocations(globe, this.intervals);
    }

    protected Iterable<? extends Sector> computeSectors(Globe globe, double texelSizeRadians)
    {
        // TODO: Compute a better fitting bounding sector for SurfaceEllipse.
        double radius = Math.max(this.majorRadius, this.minorRadius);
        Iterable<? extends Sector> sectors = java.util.Arrays.asList(
            Sector.splitBoundingSectors(globe, this.center, radius));
        sectors = getSurfaceShapeSupport().adjustSectorsByBorderWidth(sectors, this.attributes.getOutlineWidth(),
            texelSizeRadians);
        return sectors;
    }

    public Position getReferencePosition()
    {
        return new Position(this.center, 0);
    }

    protected void doMoveTo(Position oldReferencePosition, Position newReferencePosition)
    {
        Angle heading = LatLon.greatCircleAzimuth(oldReferencePosition, this.center);
        Angle pathLength = LatLon.greatCircleDistance(oldReferencePosition, this.center);
        this.setCenter(LatLon.greatCircleEndPosition(newReferencePosition, heading, pathLength));
    }

    protected LatLon computeLocationFor(Globe globe, Angle angle)
    {
        if (globe == null)
        {
            String message = Logging.getMessage("nullValue.GlobeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (angle == null)
        {
            String message = Logging.getMessage("nullValue.AngleIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        double xLength = this.majorRadius * Math.cos(angle.radians);
        double yLength = this.minorRadius * Math.sin(angle.radians);
        double distance = Math.sqrt(xLength * xLength + yLength * yLength);
        double globeRadius = globe.getRadiusAt(this.center.getLatitude(), this.center.getLongitude());

        // azimuth runs positive clockwise from north and through 360 degrees.
        double azimuth = (Math.PI / 2.0) - (Math.acos(xLength / distance) * Math.signum(yLength)
            - this.heading.radians);

        return LatLon.greatCircleEndPosition(this.center, azimuth, distance / globeRadius);
    }

    protected Iterable<LatLon> computeLocations(Globe globe, int intervals)
    {
        if (globe == null)
        {
            String message = Logging.getMessage("nullValue.GlobeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        int numPositions = 1 + Math.max(ELLIPSE_MIN_NUM_INTERVALS, intervals);
        double da = (2 * Math.PI) / (numPositions - 1);
        LatLon[] locations = new LatLon[numPositions];

        for (int i = 0; i < numPositions; i++)
        {
            double angle = (i != numPositions - 1) ? i * da : 0;
            locations[i] = this.computeLocationFor(globe, Angle.fromRadians(angle));
        }

        return java.util.Arrays.asList(locations);
    }

    protected Iterable<? extends LatLon> getLocations(Globe globe, double edgeIntervalsPerDegree)
    {
        if (globe == null)
        {
            String message = Logging.getMessage("nullValue.GlobeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        int intervals = this.computeNumIntervals(globe, edgeIntervalsPerDegree);
        return this.computeLocations(globe, intervals);
    }

    protected int computeNumIntervals(Globe globe, double edgeIntervalsPerDegree)
    {
        if (globe == null)
        {
            String message = Logging.getMessage("nullValue.GlobeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        int numEdgeIntervals = this.computeNumEdgeIntervals(globe, edgeIntervalsPerDegree);
        return numEdgeIntervals * this.intervals;
    }

    protected int computeNumEdgeIntervals(Globe globe, double edgeIntervalsPerDegree)
    {
        if (globe == null)
        {
            String message = Logging.getMessage("nullValue.GlobeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        int numPositions = 1 + Math.max(ELLIPSE_MIN_NUM_INTERVALS, intervals);
        double radius = Math.max(this.majorRadius, this.minorRadius);
        double da = (2 * Math.PI) / (numPositions - 1);
        Angle edgePathLength = Angle.fromRadians(da * radius / globe.getRadiusAt(this.center));

        double edgeIntervals = WWMath.clamp(edgeIntervalsPerDegree * edgePathLength.degrees,
            this.minEdgeIntervals, this.maxEdgeIntervals);

        return (int) Math.ceil(edgeIntervals);
    }

    //**************************************************************//
    //******************** Restorable State  ***********************//
    //**************************************************************//

    protected void doGetRestorableState(RestorableSupport rs, RestorableSupport.StateObject context)
    {
        super.doGetRestorableState(rs, context);

        rs.addStateValueAsLatLon(context, "center", this.getCenter());
        rs.addStateValueAsDouble(context, "majorRadius", this.getMajorRadius());
        rs.addStateValueAsDouble(context, "minorRadius", this.getMinorRadius());
        rs.addStateValueAsDouble(context, "headingDegrees", this.getHeading().degrees);
        rs.addStateValueAsInteger(context, "intervals", this.getIntervals());
    }

    protected void doRestoreState(RestorableSupport rs, RestorableSupport.StateObject context)
    {
        super.doRestoreState(rs, context);

        LatLon ll = rs.getStateValueAsLatLon(context, "center");
        if (ll != null)
            this.setCenter(ll);

        Double d = rs.getStateValueAsDouble(context, "majorRadius");
        if (d != null)
            this.setMajorRadius(d);

        d = rs.getStateValueAsDouble(context, "minorRadius");
        if (d != null)
            this.setMinorRadius(d);

        d = rs.getStateValueAsDouble(context, "headingDegrees");
        if (d != null)
            this.setHeading(Angle.fromDegrees(d));

        Integer i = rs.getStateValueAsInteger(context, "intervals");
        if (d != null)
            this.setIntervals(i);
    }

    protected void legacyRestoreState(RestorableSupport rs, RestorableSupport.StateObject context)
    {
        super.legacyRestoreState(rs, context);

        // These properties has not changed since the last version, but they're shown here for reference.
        //Double major = rs.getStateValueAsDouble(context, "majorRadius");
        //Double minor = rs.getStateValueAsDouble(context, "minorRadius");
        //if (major != null && minor != null)
        //    this.setAxisLengths(major, minor);

        // This property has not changed since the last version, but it's shown here for reference.
        //LatLon center = rs.getStateValueAsLatLon(context, "center");
        //if (center != null)
        //    this.setCenter(center);

        // This property has not changed since the last version, but it's shown here for reference.
        //Integer intervals = rs.getStateValueAsInteger(context, "intervals");
        //if (intervals != null)
        //    this.setIntervals(intervals);

        Double od = rs.getStateValueAsDouble(context, "orientationDegrees");
        if (od != null)
            this.setHeading(Angle.fromDegrees(od));
    }
}

