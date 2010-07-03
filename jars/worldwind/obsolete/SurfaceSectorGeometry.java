/*
Copyright (C) 2001, 2008 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render;

import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.util.Logging;

import java.awt.*;
import java.util.ArrayList;

/**
 * @author tag
 * @version $Id: SurfaceSectorGeometry.java 7434 2008-11-08 21:27:45Z tgaskins $
 */
public class SurfaceSectorGeometry extends SurfacePolygonGeometry
{
    public SurfaceSectorGeometry(Sector sector, Color color, Color borderColor)
    {
        super(makePositions(sector), color, borderColor);
    }

    public SurfaceSectorGeometry(Sector sector)
    {
        super(makePositions(sector), null, null);
    }

    public void setSector(Sector sector)
    {
        this.setPositions(makePositions(sector));
    }

    public Sector getSector()
    {
        return this.getSectors().get(0); // TODO: coallesce split sectors into one?
    }

    private static Iterable<LatLon> makePositions(Sector sector)
    {
        if (sector == null)
        {
            String message = Logging.getMessage("nullValue.SectorIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        ArrayList<LatLon> positions = new ArrayList<LatLon>(5);

        positions.add(new LatLon(sector.getMinLatitude(), sector.getMinLongitude()));
        positions.add(new LatLon(sector.getMinLatitude(), sector.getMaxLongitude()));
        positions.add(new LatLon(sector.getMaxLatitude(), sector.getMaxLongitude()));
        positions.add(new LatLon(sector.getMaxLatitude(), sector.getMinLongitude()));
        positions.add(new LatLon(sector.getMinLatitude(), sector.getMinLongitude()));

        return positions;
    }
}