/*
Copyright (C) 2001, 2010 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.applications.gos.globe;

import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.ogc.wms.WMSCapabilities;

/**
 * @author dcollins
 * @version $Id: GlobeModel.java 13362 2010-04-30 19:43:27Z dcollins $
 */
public interface GlobeModel
{
    boolean hasLayer(String uuid, String layerName, String styleName);

    void addLayer(String uuid, WMSCapabilities caps, String layerName, String styleName, String displayName);

    void removeLayer(String uuid, String layerName, String styleName);

    void moveViewTo(Sector sector);
}
