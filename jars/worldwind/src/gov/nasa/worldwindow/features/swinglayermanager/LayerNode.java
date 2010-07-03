/*
Copyright (C) 2001, 2010 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/

package gov.nasa.worldwindow.features.swinglayermanager;

import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwindow.features.WMSLayerInfo;

/**
 * @author tag
 * @version $Id: LayerNode.java 13330 2010-04-22 18:33:28Z tgaskins $
 */
public interface LayerNode
{
    Object getID();

    String getTitle();

    void setTitle(String title);

    Layer getLayer();

    void setLayer(Layer layer);

    boolean isSelected();

    void setSelected(boolean selected);

    WMSLayerInfo getWmsLayerInfo();

    String getToolTipText();

    void setToolTipText(String toolTipText);

    void setEnableSelectionBox(boolean tf);

    boolean isEnableSelectionBox();
}
