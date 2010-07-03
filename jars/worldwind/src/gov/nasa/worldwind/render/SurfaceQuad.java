/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render;

import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.util.*;

/**
 * @author dcollins
 * @version $Id: SurfaceQuad.java 9992 2009-04-08 04:38:54Z dcollins $
 */
public class SurfaceQuad extends SurfaceConcaveShape
{
    protected static final int MIN_NUM_INTERVALS = 1;

    protected LatLon center;
    protected double width;
    protected double height;
    protected Angle heading;

    public SurfaceQuad(ShapeAttributes attributes, LatLon center, double width, double height,
        Angle heading)
    {
        super(attributes);

        if (center == null)
        {
            String message = Logging.getMessage("nullValue.CenterIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (width < 0)
        {
            String message = Logging.getMessage("generic.ArgumentOutOfRange", "width < 0");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (height < 0)
        {
            String message = Logging.getMessage("generic.ArgumentOutOfRange", "height < 0");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (heading == null)
        {
            String message = Logging.getMessage("nullValue.HeadingIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.center = center;
        this.width = width;
        this.height = height;
        this.heading = heading;
    }

    public SurfaceQuad(ShapeAttributes attributes)
    {
        this(attributes, LatLon.ZERO, 0, 0, Angle.ZERO);
    }

    public SurfaceQuad(LatLon center, double width, double height, Angle heading)
    {
        this(new BasicShapeAttributes(), center, width, height, heading);
    }

    public SurfaceQuad()
    {
        this(new BasicShapeAttributes());
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

    public double getWidth()
    {
        return this.width;
    }

    public double getHeight()
    {
        return this.height;
    }

    public void setWidth(double width)
    {
        if (width < 0)
        {
            String message = Logging.getMessage("generic.ArgumentOutOfRange", "width < 0");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.width = width;
        this.onShapeChanged();
    }

    public void setHeight(double height)
    {
        if (height < 0)
        {
            String message = Logging.getMessage("generic.ArgumentOutOfRange", "height < 0");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.height = height;
        this.onShapeChanged();
    }

    public void setSize(double width, double height)
    {
        if (width < 0)
        {
            String message = Logging.getMessage("generic.ArgumentOutOfRange", "width < 0");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (height < 0)
        {
            String message = Logging.getMessage("generic.ArgumentOutOfRange", "height < 0");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.width = width;
        this.height = height;
        this.onShapeChanged();
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

    public Iterable<? extends LatLon> getLocations(Globe globe)
    {
        if (globe == null)
        {
            String message = Logging.getMessage("nullValue.GlobeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        double hw = this.width / 2.0;
        double hh = this.height / 2.0;
        double globeRadius = globe.getRadiusAt(this.center.getLatitude(), this.center.getLongitude());
        double distance = Math.sqrt(hw * hw + hh * hh);
        double pathLength = distance / globeRadius;

        double[] cornerAngles = new double[]
            {
                Math.atan2(-hh, -hw),
                Math.atan2(-hh, hw),
                Math.atan2(hh, hw),
                Math.atan2(hh, -hw),
                Math.atan2(-hh, -hw),
            };

        LatLon[] locations = new LatLon[cornerAngles.length];

        for (int i = 0; i < cornerAngles.length; i++)
        {
            double azimuth = (Math.PI / 2.0) - (cornerAngles[i] - this.heading.radians);
            locations[i] = LatLon.greatCircleEndPosition(this.center, azimuth, pathLength);
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

        Iterable<? extends LatLon> originalLocations = this.getLocations(globe);
        java.util.ArrayList<LatLon> newLocations = new java.util.ArrayList<LatLon>();
        getSurfaceShapeSupport().generateIntermediateLocations(originalLocations, this.pathType,
            edgeIntervalsPerDegree, this.minEdgeIntervals, this.maxEdgeIntervals, false, newLocations);

        return newLocations;
    }

    //**************************************************************//
    //******************** Restorable State  ***********************//
    //**************************************************************//

    protected void doGetRestorableState(RestorableSupport rs, RestorableSupport.StateObject context)
    {
        super.doGetRestorableState(rs, context);

        rs.addStateValueAsLatLon(context, "center", this.getCenter());
        rs.addStateValueAsDouble(context, "width", this.getWidth());
        rs.addStateValueAsDouble(context, "height", this.getHeight());
        rs.addStateValueAsDouble(context, "headingDegrees", this.getHeading().degrees);
    }

    protected void doRestoreState(RestorableSupport rs, RestorableSupport.StateObject context)
    {
        super.doRestoreState(rs, context);

        LatLon ll = rs.getStateValueAsLatLon(context, "center");
        if (ll != null)
            this.setCenter(ll);

        Double d = rs.getStateValueAsDouble(context, "width");
        if (d != null)
            this.setWidth(d);

        d = rs.getStateValueAsDouble(context, "height");
        if (d != null)
            this.setHeight(d);

        d = rs.getStateValueAsDouble(context, "headingDegrees");
        if (d != null)
            this.setHeading(Angle.fromDegrees(d));
    }

    protected void legacyRestoreState(RestorableSupport rs, RestorableSupport.StateObject context)
    {
        super.legacyRestoreState(rs, context);

        // Previous versions of SurfaceQuad used half-width and half-height properties. We are now using standard
        // width and height, so these restored values must be converted.
        Double width = rs.getStateValueAsDouble(context, "halfWidth");
        Double height = rs.getStateValueAsDouble(context, "halfHeight");
        if (width != null && height != null)
            this.setSize(2 * width, 2 * height);

        // This property has not changed since the previos version, but it's shown here for reference.
        //LatLon center = rs.getStateValueAsLatLon(context, "center");
        //if (center != null)
        //    this.setCenter(center);

        Double od = rs.getStateValueAsDouble(context, "orientationDegrees");
        if (od != null)
            this.setHeading(Angle.fromDegrees(od));
    }
}
