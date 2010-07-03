/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render;

import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.util.Logging;

/**
 * @author dcollins
 * @version $Id: AbstractSurfaceObject.java 12347 2009-07-17 22:30:44Z dcollins $
 */
public abstract class AbstractSurfaceObject extends AVListImpl implements SurfaceObject
{
    protected boolean visible;
    protected long lastModifiedTime;

    public AbstractSurfaceObject()
    {
        this.visible = true;
        this.updateModifiedTime();
    }

    public boolean isVisible()
    {
        return this.visible;
    }

    public void setVisible(boolean visible)
    {
        this.visible = visible;
        this.updateModifiedTime();
    }

    public long getLastModifiedTime()
    {
        return this.lastModifiedTime;
    }

    public void renderToRegion(DrawContext dc, Sector sector, int x, int y, int width, int height)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (sector == null)
        {
            String message = Logging.getMessage("nullValue.SectorIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (!this.isVisible())
            return;

        this.doRenderToRegion(dc, sector, x, y, width, height);
    }

    //**************************************************************//
    //********************  Protected Interface  *******************//
    //**************************************************************//

    protected abstract void doRenderToRegion(DrawContext dc, Sector sector, int x, int y, int width, int height);

    protected void updateModifiedTime()
    {
        this.lastModifiedTime = System.currentTimeMillis();
    }
}
