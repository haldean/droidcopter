/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render;

import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.util.*;

import java.util.Iterator;

/**
 * @author dcollins
 * @version $Id: SurfacePolygon.java 12494 2009-08-20 22:52:52Z dcollins $
 */
public class SurfacePolygon extends SurfaceConcaveShape
{
    protected Iterable<? extends LatLon> locations;

    public SurfacePolygon(ShapeAttributes attributes, Iterable<? extends LatLon> iterable)
    {
        super(attributes);

        if (iterable == null)
        {
            String message = Logging.getMessage("nullValue.IterableIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.locations = iterable;
    }

    public SurfacePolygon(ShapeAttributes attributes)
    {
        this(attributes, new java.util.ArrayList<LatLon>());
    }

    public SurfacePolygon(Iterable<? extends LatLon> iterable)
    {
        this(new BasicShapeAttributes(), iterable);
    }

    public SurfacePolygon()
    {
        this(new BasicShapeAttributes());
    }

    public Iterable<? extends LatLon> getLocations(Globe globe)
    {
        if (globe == null)
        {
            String message = Logging.getMessage("nullValue.GlobeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return this.getLocations();
    }

    public Iterable<? extends LatLon> getLocations()
    {
        return this.locations;
    }

    public void setLocations(Iterable<? extends LatLon> iterable)
    {
        if (iterable == null)
        {
            String message = Logging.getMessage("nullValue.IterableIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.locations = iterable;
        this.onShapeChanged();
    }

    public Position getReferencePosition()
    {
        Iterator<? extends LatLon> iterator = this.locations.iterator();
        if (!iterator.hasNext())
            return null;

        return new Position(iterator.next(), 0);
    }

    protected Iterable<? extends LatLon> getLocations(Globe globe, double edgeIntervalsPerDegree)
    {
        if (globe == null)
        {
            String message = Logging.getMessage("nullValue.GlobeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        java.util.ArrayList<LatLon> newLocations = new java.util.ArrayList<LatLon>();
        getSurfaceShapeSupport().generateIntermediateLocations(this.locations, this.pathType,
            edgeIntervalsPerDegree, this.minEdgeIntervals, this.maxEdgeIntervals, true, newLocations);

        return newLocations;
    }

    protected void doMoveTo(Position oldReferencePosition, Position newReferencePosition)
    {
        java.util.ArrayList<LatLon> newLocations = new java.util.ArrayList<LatLon>();

        for (LatLon ll : this.locations)
        {
            Angle heading = LatLon.greatCircleAzimuth(oldReferencePosition, ll);
            Angle pathLength = LatLon.greatCircleDistance(oldReferencePosition, ll);
            newLocations.add(LatLon.greatCircleEndPosition(newReferencePosition, heading, pathLength));
        }

        this.setLocations(newLocations);
    }

    //**************************************************************//
    //******************** Restorable State  ***********************//
    //**************************************************************//

    protected void doGetRestorableState(RestorableSupport rs, RestorableSupport.StateObject context)
    {
        super.doGetRestorableState(rs, context);

        Iterable<? extends LatLon> iterable = this.getLocations();
        if (iterable != null)
            rs.addStateValueAsLatLonList(context, "locationList", iterable);
    }

    protected void doRestoreState(RestorableSupport rs, RestorableSupport.StateObject context)
    {
        super.doRestoreState(rs, context);

        Iterable<LatLon> iterable = rs.getStateValueAsLatLonList(context, "locationList");
        if (iterable != null)
            this.setLocations(iterable);
    }

    protected void legacyRestoreState(RestorableSupport rs, RestorableSupport.StateObject context)
    {
        super.legacyRestoreState(rs, context);

        java.util.ArrayList<LatLon> locations = rs.getStateValueAsLatLonList(context, "locations");
        if (locations != null)
            this.setLocations(locations);
    }
}
