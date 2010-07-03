/*
Copyright (C) 2001, 2010 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.applications.gos.globe;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.applications.gos.GeodataKey;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.layers.*;
import gov.nasa.worldwind.ogc.wms.WMSCapabilities;
import gov.nasa.worldwind.util.*;

/**
 * @author dcollins
 * @version $Id: WorldWindowGlobeModel.java 13362 2010-04-30 19:43:27Z dcollins $
 */
public class WorldWindowGlobeModel implements GlobeModel
{
    protected WorldWindow wwd;

    public WorldWindowGlobeModel(WorldWindow wwd)
    {
        this.wwd = wwd;
    }

    public WorldWindow getWorldWindow()
    {
        return this.wwd;
    }

    public void addLayer(String uuid, WMSCapabilities caps, String layerName, String styleName, String displayName)
    {
        AVList params = new AVListImpl();
        params.setValue(AVKey.LAYER_NAMES, layerName);
        params.setValue(AVKey.STYLE_NAMES, styleName);

        Factory factory = (Factory) WorldWind.createConfigurationComponent(AVKey.LAYER_FACTORY);
        Layer layer = (Layer) factory.createFromConfigSource(caps, params.copy());
        if (layer == null)
            return;

        layer.setName(displayName);
        layer.setValue(GeodataKey.UUID, uuid);
        layer.setValue(AVKey.LAYER_NAMES, layerName);
        layer.setValue(AVKey.STYLE_NAMES, styleName);

        this.insertAfterRecordListLayer(layer);
    }

    public boolean hasLayer(String uuid, String layerName, String styleName)
    {
        return this.getLayer(uuid, layerName, styleName) != null;
    }

    public void removeLayer(String uuid, String layerName, String styleName)
    {
        Layer layer = this.getLayer(uuid, layerName, styleName);
        if (layer == null)
            return;

        this.wwd.getModel().getLayers().remove(layer);
    }

    public void moveViewTo(Sector sector)
    {
        Globe globe = this.wwd.getModel().getGlobe();
        double ve = this.wwd.getSceneController().getVerticalExaggeration();

        Extent extent = Sector.computeBoundingCylinder(globe, ve, sector);
        if (extent == null)
        {
            String message = Logging.getMessage("nullValue.SectorIsNull");
            Logging.logger().warning(message);
            return;
        }

        View view = this.wwd.getView();
        Angle fov = view.getFieldOfView();

        Position centerPos = new Position(sector.getCentroid(), 0d);
        double zoom = extent.getRadius() / (fov.tanHalfAngle() * fov.cosHalfAngle());
        this.wwd.getView().goTo(centerPos, zoom);
    }

    protected Layer getLayer(String uuid, String layerName, String styleName)
    {
        if (WWUtil.isEmpty(uuid) || WWUtil.isEmpty(layerName))
            return null;

        for (Layer layer : this.wwd.getModel().getLayers())
        {
            String s = layer.getStringValue(GeodataKey.UUID);
            if (WWUtil.isEmpty(s) || !s.equals(uuid))
                continue;

            s = layer.getStringValue(AVKey.LAYER_NAMES);
            if (WWUtil.isEmpty(s) || !s.equals(layerName))
                continue;

            s = layer.getStringValue(AVKey.STYLE_NAMES);
            if (!WWUtil.isEmpty(styleName) ? (WWUtil.isEmpty(s) || !s.equals(styleName)) : !WWUtil.isEmpty(s))
                continue;

            return layer;
        }

        return null;
    }

    protected void insertAfterRecordListLayer(Layer layer)
    {
        // Insert the layer into the layer list just before the placenames.
        int lastTiledImageLayer = 0;
        LayerList layers = this.wwd.getModel().getLayers();
        for (Layer l : layers)
        {
            if (l instanceof RecordListLayer)
                lastTiledImageLayer = layers.indexOf(l);
        }
        layers.add(lastTiledImageLayer + 1, layer);
    }
}
