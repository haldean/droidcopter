/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render;

import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.globes.Globe;

import java.awt.*;
import java.util.ArrayList;

/**
 * @author Jim Miller
 * @version $Id: ConformingSector.java 7671 2008-12-08 00:18:14Z jmiller $
 */

// The bulk of the code here was copied and adapted from SurfaceSector.java

public class ConformingSector extends ConformingPolygon
{
    public ConformingSector(Globe globe, Sector sector, Color color, Color borderColor)
    {
        super(globe, makePositions(sector), color, borderColor, false);
    }

    public ConformingSector(Globe globe, Sector sector)
    {
        super(globe, makePositions(sector), null, null, false);
    }

    public void setSector(Sector sector)
    {
        this.setOriginalVertices(makePositions(sector));
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
