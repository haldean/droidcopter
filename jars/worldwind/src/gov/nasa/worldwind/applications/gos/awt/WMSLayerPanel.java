/*
Copyright (C) 2001, 2010 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.applications.gos.awt;

import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.applications.gos.*;
import gov.nasa.worldwind.applications.gos.globe.GlobeModel;
import gov.nasa.worldwind.applications.gos.html.*;
import gov.nasa.worldwind.ogc.wms.*;
import gov.nasa.worldwind.util.WWUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.*;
import java.util.List;

/**
 * @author dcollins
 * @version $Id: WMSLayerPanel.java 13362 2010-04-30 19:43:27Z dcollins $
 */
public class WMSLayerPanel extends JPanel
{
    protected Record record;
    protected GlobeModel globeModel;
    protected WMSCapabilities caps;

    public WMSLayerPanel(Record record, GlobeModel globeModel)
    {
        this.record = record;
        this.globeModel = globeModel;

        this.setBackground(Color.WHITE);
    }

    public WMSCapabilities getCapabilities()
    {
        return this.caps;
    }

    public void setCapabilities(WMSCapabilities caps)
    {
        this.caps = caps;
        this.onCapabilitiesChanged();
    }

    protected void onCapabilitiesChanged()
    {
        this.removeAll();

        if (this.caps == null)
            return;

        List<WMSLayerCapabilities> layerList = this.caps.getNamedLayers();
        if (layerList == null || layerList.size() == 0)
            return;

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JLabel label = new JLabel(this.createTitle());
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        this.add(label);
        this.add(Box.createVerticalStrut(10));

        layerList = this.sortLayerList(layerList);

        for (WMSLayerCapabilities layer : layerList)
        {
            if (layer == null)
                continue;

            Set<WMSLayerStyle> styleSet = layer.getStyles();
            if (styleSet == null || styleSet.size() == 0)
            {
                this.addLayerButton(layer, null);
            }
            else
            {
                for (WMSLayerStyle style : styleSet)
                {
                    if (style == null)
                        continue;

                    this.addLayerButton(layer, style);
                }
            }
        }
    }

    protected String createTitle()
    {
        StringBuilder sb = new StringBuilder();
        HTMLFormatter formatter = new BasicHTMLFormatter();
        formatter.setEnableAdvancedHTML(false);

        formatter.beginHTMLBody(sb);
        formatter.beginHeading(sb, 1);
        sb.append("Map layers");

        String s = this.caps.getServiceInformation().getServiceTitle();
        if (WWUtil.isEmpty(s))
            s = "No name";

        sb.append(" for \"").append(s).append("\"");
        formatter.endHeading(sb, 1);

        s = this.caps.getRequestURL("GetCapabilities", "http", "get");
        if (!WWUtil.isEmpty(s))
        {
            formatter.beginFont(sb, "#888888");
            sb.append(" [").append(s).append("]");
            formatter.endFont(sb);
        }

        formatter.endHTMLBody(sb);

        return sb.toString();
    }

    protected List<WMSLayerCapabilities> sortLayerList(List<WMSLayerCapabilities> list)
    {
        Collections.sort(list, new Comparator<WMSLayerCapabilities>()
        {
            public int compare(WMSLayerCapabilities a, WMSLayerCapabilities b)
            {
                return String.CASE_INSENSITIVE_ORDER.compare(a.getName(), b.getName());
            }
        });

        return list;
    }

    protected void addLayerButton(WMSLayerCapabilities layer, WMSLayerStyle style)
    {
        if (this.globeModel == null)
            return;

        String layerName = layer.getName();
        String styleName = (style != null) ? style.getName() : null;
        String displayName = this.createLayerDisplayName(layer, style);

        if (WWUtil.isEmpty(layerName))
            return;

        Action action = this.getLayerAction(layerName, styleName, displayName);
        JButton button = new JButton(action);
        AWTUtil.scaleButtonToIcon(button, 2, 2);

        JLabel label = new JLabel(displayName);
        label.setBackground(Color.WHITE);

        Box box = Box.createHorizontalBox();
        box.setAlignmentX(Component.LEFT_ALIGNMENT);
        box.add(button);
        box.add(Box.createHorizontalStrut(3));
        box.add(label);
        this.add(box);
        this.add(Box.createVerticalStrut(3));
    }

    protected String createLayerDisplayName(WMSLayerCapabilities layer, WMSLayerStyle style)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(!WWUtil.isEmpty(layer.getTitle()) ? layer.getTitle() : layer.getName());

        if (style != null)
            sb.append(!WWUtil.isEmpty(style.getTitle()) ? style.getTitle() : style.getName());

        return sb.toString();
    }

    protected Action getLayerAction(String layerName, String styleName, String displayName)
    {
        if (this.globeModel.hasLayer(this.record.getIdentifier(), layerName, styleName))
        {
            return new RemoveLayerAction(layerName, styleName, displayName);
        }
        else
        {
            return new AddLayerAction(layerName, styleName, displayName);
        }
    }

    protected class LayerAction extends AbstractAction
    {
        protected String layerName;
        protected String styleName;
        protected String displayName;

        public LayerAction(String layerName, String styleName, String displayName, String iconKey)
        {
            this.layerName = layerName;
            this.styleName = styleName;
            this.displayName = displayName;

            String s = Configuration.getStringValue(iconKey);
            if (!WWUtil.isEmpty(s))
            {
                BufferedImage image = ResourceUtil.getImage(s);
                if (image != null)
                    this.putValue(Action.LARGE_ICON_KEY, new ImageIcon(image));
            }
        }

        public void actionPerformed(ActionEvent event)
        {
            Action newAction = getLayerAction(this.layerName, this.styleName, this.displayName);
            ((AbstractButton) event.getSource()).setAction(newAction);
        }
    }

    protected class AddLayerAction extends LayerAction
    {
        public AddLayerAction(String layerName, String styleName, String displayName)
        {
            super(layerName, styleName, displayName, GeodataKey.ADD_LAYER_ICON);
        }

        public void actionPerformed(ActionEvent event)
        {
            String globeLayerName = this.getGlobeLayerName();
            globeModel.addLayer(record.getIdentifier(), caps, this.layerName, this.styleName, globeLayerName);
            super.actionPerformed(event);
        }

        protected String getGlobeLayerName()
        {
            StringBuilder sb = new StringBuilder();
            sb.append(this.displayName);

            String s = Configuration.getStringValue(GeodataKey.RETRIEVED_LAYER_SUFFIX);
            if (!WWUtil.isEmpty(s))
                sb.append(s);

            return sb.toString();
        }
    }

    protected class RemoveLayerAction extends LayerAction
    {
        public RemoveLayerAction(String layerName, String styleName, String displayName)
        {
            super(layerName, styleName, displayName, GeodataKey.REMOVE_LAYER_ICON);
        }

        public void actionPerformed(ActionEvent event)
        {
            globeModel.removeLayer(record.getIdentifier(), this.layerName, this.styleName);
            super.actionPerformed(event);
        }
    }
}
