/*
Copyright (C) 2001, 2010 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/

package gov.nasa.worldwindow.features.swinglayermanager;

import gov.nasa.worldwindow.features.WMSLayerInfo;

/**
 * @author tag
 * @version $Id: LayerTreeGroupNode.java 13330 2010-04-22 18:33:28Z tgaskins $
 */
public class LayerTreeGroupNode extends LayerTreeNode
{
    public LayerTreeGroupNode()
    {
    }

    public LayerTreeGroupNode(String title)
    {
        super(title);
    }

    public LayerTreeGroupNode(WMSLayerInfo layerInfo)
    {
        super(layerInfo);
    }

    public LayerTreeGroupNode(LayerTreeGroupNode layerNode)
    {
        super(layerNode);
    }
}
