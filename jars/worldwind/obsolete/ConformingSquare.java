/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render;

import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.util.Logging;

import java.awt.*;

/**
 * @author Jim Miller
 * @version $Id: ConformingSquare.java 7671 2008-12-08 00:18:14Z jmiller $
 */

// The bulk of the code here was copied and adapted from SurfaceSquare.java

public class ConformingSquare extends ConformingQuad
{
    public ConformingSquare(Globe globe, LatLon center, double halfWidth)
    {
        //noinspection SuspiciousNameCombination
        super(globe, center, halfWidth, halfWidth, null, null, null);
    }

    public ConformingSquare(Globe globe, LatLon center, double width, Color interiorColor, Color borderColor)
    {
        //noinspection SuspiciousNameCombination
        super(globe, center, width, width, null, interiorColor, borderColor);
    }

    public double getHalfWidth()
    {
        return super.getHalfWidth();
    }

    public void setHalfWidth(double halfWidth)
    {
        if (halfWidth <= 0)
        {
            String message = Logging.getMessage("Geom.WidthInvalid", halfWidth);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        //noinspection SuspiciousNameCombination
        super.setSize(halfWidth, halfWidth);
    }
}
