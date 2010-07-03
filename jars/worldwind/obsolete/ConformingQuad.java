/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render;

import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.RestorableSupport;

import java.util.ArrayList;
import java.awt.*;

/**
 * @author Jim Miller
 * @version $Id: ConformingQuad.java 7671 2008-12-08 00:18:14Z jmiller $
 */

// The bulk of the code here was copied and adapted from SurfaceQuad.java

public class ConformingQuad extends ConformingPolygon
{
    private LatLon center;
    private double halfWidth;
    private double halfHeight;
    private Angle orientation;

    public ConformingQuad(Globe globe,
        LatLon center, double halfWidth, double halfHeight, Angle orientation)
    {
        super(globe,
                makePositions(globe, center, halfWidth, halfHeight, orientation),
                null, null, false);
        this.center = center;
        this.halfWidth = halfWidth;
        this.halfHeight = halfHeight;
        this.orientation = orientation;
    }

    public ConformingQuad(Globe globe,
        LatLon center, double halfWidth, double halfHeight, Angle orientation,
        Color fillColor, Color borderColor)
    {
        super(globe,
                makePositions(globe, center, halfWidth, halfHeight, orientation),
                fillColor, borderColor, false);
        this.center = center;
        this.halfWidth = halfWidth;
        this.halfHeight = halfHeight;
        this.orientation = orientation;
    }

    @Override
    protected void doGetRestorableState(RestorableSupport rs, RestorableSupport.StateObject context)
    {
        super.doGetRestorableState(rs, context);

        rs.addStateValueAsDouble(context, "halfWidth", this.getHalfWidth());
        rs.addStateValueAsDouble(context, "halfHeight", this.getHalfHeight());
        rs.addStateValueAsLatLon(context, "center", this.getCenter());
        if (this.orientation != null)
            rs.addStateValueAsDouble(context, "orientationDegrees", this.getOrientation().degrees);
    }

    @Override
    protected void doRestoreState(RestorableSupport rs, RestorableSupport.StateObject context)
    {
        super.doRestoreState(rs, context);

        Double width = rs.getStateValueAsDouble(context, "halfWidth");
        Double height = rs.getStateValueAsDouble(context, "halfHeight");
        if (width != null && height != null)
            this.setSize(width, height);

        LatLon center = rs.getStateValueAsLatLon(context, "center");
        if (center != null)
            this.setCenter(center);

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
        this.setOriginalVertices(makePositions(this.globe,
                this.center, this.halfWidth, this.halfHeight, this.orientation));
    }

    public double getHalfWidth()
    {
        return halfWidth;
    }

    public double getHalfHeight()
    {
        return halfHeight;
    }

    public void setSize(double width, double height)
    {
        this.halfWidth = width;
        this.halfHeight = height;
        this.setOriginalVertices(makePositions(this.globe,
                this.center, this.halfWidth, this.halfHeight, this.orientation));
    }

    public Angle getOrientation()
    {
        return orientation;
    }

    public void setOrientation(Angle orientation)
    {
        this.orientation = orientation;
        this.setOriginalVertices(makePositions(this.globe, this.center, this.halfWidth, this.halfHeight, this.orientation));
    }

    public static Iterable<LatLon> makePositions(Globe globe, LatLon center, double width, double height,
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
