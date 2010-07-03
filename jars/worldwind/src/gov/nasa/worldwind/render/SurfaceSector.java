/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.util.*;

/**
 * @author dcollins
 * @version $Id: SurfaceSector.java 12496 2009-08-20 22:55:15Z dcollins $
 */
public class SurfaceSector extends SurfaceConcaveShape
{
    protected static final String SECTOR_DEFAULT_PATH_TYPE = AVKey.GREAT_CIRCLE;

    protected Sector sector;

    public SurfaceSector(ShapeAttributes attributes, Sector sector)
    {
        super(attributes);

        if (sector == null)
        {
            String message = Logging.getMessage("nullValue.SectorIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.sector = sector;
        this.setPathType(SECTOR_DEFAULT_PATH_TYPE);
    }

    public SurfaceSector(ShapeAttributes attributes)
    {
        this(attributes, Sector.EMPTY_SECTOR);
    }

    public SurfaceSector(Sector sector)
    {
        this(new BasicShapeAttributes(), sector);
    }

    public SurfaceSector()
    {
        this(new BasicShapeAttributes());
    }

    public Sector getSector()
    {
        return this.sector;
    }

    public void setSector(Sector sector)
    {
        if (sector == null)
        {
            String message = Logging.getMessage("nullValue.SectorIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.sector = sector;
        this.onShapeChanged();
    }

    public Position getReferencePosition()
    {
        return new Position(this.sector.getCentroid(), 0);
    }

    public Iterable<? extends LatLon> getLocations(Globe globe)
    {
        if (globe == null)
        {
            String message = Logging.getMessage("nullValue.GlobeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        LatLon[] locations = new LatLon[5];
        System.arraycopy(this.sector.getCorners(), 0, locations, 0, 4);
        locations[4] = locations[0];

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

    protected void doMoveTo(Position oldReferencePosition, Position newReferencePosition)
    {
        LatLon[] locations = new LatLon[]
            {
                new LatLon(this.sector.getMinLatitude(), this.sector.getMinLongitude()),
                new LatLon(this.sector.getMaxLatitude(), this.sector.getMaxLongitude())
            };

        LatLon[] newLocations = new LatLon[2];
        for (int i = 0; i < 2; i++)
        {
            Angle heading = LatLon.greatCircleAzimuth(oldReferencePosition, locations[i]);
            Angle pathLength = LatLon.greatCircleDistance(oldReferencePosition, locations[i]);
            newLocations[i] = LatLon.greatCircleEndPosition(newReferencePosition, heading, pathLength);
        }

        this.setSector(new Sector(
            newLocations[0].getLatitude(), newLocations[1].getLatitude(),
            newLocations[0].getLongitude(), newLocations[1].getLongitude()));
    }

    //**************************************************************//
    //******************** Restorable State  ***********************//
    //**************************************************************//

    protected void doGetRestorableState(RestorableSupport rs, RestorableSupport.StateObject context)
    {
        super.doGetRestorableState(rs, context);

        rs.addStateValueAsSector(context, "sector", this.getSector());
    }

    protected void doRestoreState(RestorableSupport rs, RestorableSupport.StateObject context)
    {
        super.doRestoreState(rs, context);

        Sector sector = rs.getStateValueAsSector(context, "sector");
        if (sector != null)
            this.setSector(sector);
    }

    protected void legacyRestoreState(RestorableSupport rs, RestorableSupport.StateObject context)
    {
        super.legacyRestoreState(rs, context);

        // Previous versions of SurfaceSector would have stored the locations produced by treating the sector as a list
        // of polygon locations. To restore an shape saved from the previous version, we compute the bounding sector of
        // those locations to define a sector.
        java.util.ArrayList<LatLon> locations = rs.getStateValueAsLatLonList(context, "locations");
        if (locations != null)
            this.setSector(Sector.boundingSector(locations));
    }
}
