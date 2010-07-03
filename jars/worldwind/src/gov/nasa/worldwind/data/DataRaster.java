/* Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.data;

import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.avlist.AVList;

/**
 * @author dcollins
 * @version $Id: DataRaster.java 13165 2010-02-26 22:04:24Z garakl $
 */
public interface DataRaster extends AVList
{
    int getWidth();

    int getHeight();

    Sector getSector();

    void drawOnCanvas(DataRaster canvas, Sector clipSector);

    void drawOnCanvas(DataRaster canvas);
}
