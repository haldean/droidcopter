/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render;

import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.geom.Sector;

/**
 * @author dcollins
 * @version $Id: SurfaceObject.java 12347 2009-07-17 22:30:44Z dcollins $
 */
public interface SurfaceObject extends AVList
{
    boolean isVisible();

    void setVisible(boolean visible);

    long getLastModifiedTime();

    Iterable<? extends Sector> getSectors(DrawContext dc, double texelSizeRadians);

    void renderToRegion(DrawContext dc, Sector sector, int x, int y, int width, int height);
}
