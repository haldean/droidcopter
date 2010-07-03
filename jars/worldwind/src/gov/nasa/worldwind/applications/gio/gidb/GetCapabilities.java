/*
Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.applications.gio.gidb;

import gov.nasa.worldwind.applications.gio.catalogui.*;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.wms.Capabilities;

import java.net.URI;

/**
 * @author dcollins
 * @version $Id: GetCapabilities.java 9600 2009-03-22 20:04:40Z tgaskins $
 */
public class GetCapabilities
{
    private GIDBResultModel resultModel;
    private Server server;

    public GetCapabilities(GIDBResultModel resultModel)
    {
        if (resultModel == null)
        {
            String message = "nullValue.ResultModelIsNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (resultModel.getServer() == null)
        {
            String message = "nullValue.ResultModelServerIsNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.resultModel = resultModel;
        this.server = resultModel.getServer();
    }

    public void executeRequest() throws Exception
    {
        // Attempt to get WMS capabilities if the service is not known,
        // or if the service is WMS.
        Object o = this.resultModel.getValue(CatalogKey.SERVICE_TYPE);
        if (o == null || o.equals(CatalogKey.WMS))
        {
            setCapabilities();
            setLayers();
        }
    }

    protected void setCapabilities() throws Exception
    {
        String uriString = null;
        if (this.server.getURL() != null)
            uriString = this.server.getURL().getValue();

        Capabilities caps = null;
        if (uriString != null)
        {
            try
            {
//                // Retrieve the server's capabilities document and parse it into a DOM.
//                // Set up the DOM.
//                DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
//                docBuilderFactory.setNamespaceAware(true);
//                DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
//                Document doc;
//
//                // Request the capabilities document from the server.
//                URI uri = new URI(uriString);
//                CapabilitiesRequest req = new CapabilitiesRequest(uri);
//                doc = docBuilder.parse(req.toString());
//
//                // Parse the DOM as a capabilities document.
//                caps = Capabilities.parse(doc);
                caps = Capabilities.retrieve(new URI(uriString), "WMS");
            }
            catch (Exception e)
            {
                String message = "Cannot read WMS Capabilities document at " + uriString;
                Logging.logger().log(java.util.logging.Level.SEVERE, message, e);
                CatalogException ex = new CatalogException(message, null);
                this.resultModel.addException(ex);
            }
        }
        else
        {
            String message = "Service does not specify access points for GetCapabilities and GetMap.";
            CatalogException e = new CatalogException(message, null);
            this.resultModel.addException(e);
        }

        this.resultModel.setCapabilities(caps);

        // WMS capabilities request succeeded, so the service must be WMS.
        // Designate the services as WMS unless a designation already exists.
        if (caps != null)
            if (this.resultModel.getValue(CatalogKey.SERVICE_TYPE) == null)
                this.resultModel.setValue(CatalogKey.SERVICE_TYPE, CatalogKey.WMS);
    }

    protected void setLayers()
    {
        Capabilities caps = this.resultModel.getCapabilities();
        if (caps != null)
        {
            // Gather up all the named layers and make a world wind layer for each.
            org.w3c.dom.Element[] namedLayers = caps.getNamedLayers();
            if (namedLayers != null)
            {
                for (org.w3c.dom.Element layerCaps : namedLayers)
                {
                    if (layerCaps != null)
                    {
                        org.w3c.dom.Element[] styles = caps.getLayerStyles(layerCaps);
                        addLayer(caps, layerCaps, styles);
                    }
                }
            }
        }
    }

    protected void addLayer(Capabilities caps, org.w3c.dom.Element layerCaps, org.w3c.dom.Element[] styles)
    {
        if (caps == null)
        {
            String message = "nullValue.CapsIsNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (layerCaps == null)
        {
            String message = "nullValue.LayerCapsIsNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (styles == null)
            styles = new org.w3c.dom.Element[1];

        for (org.w3c.dom.Element styleCaps : styles)
        {
            Layer layer = new Layer();
            layer.setServer(this.server);

            String s = caps.getLayerName(layerCaps);
            if (s != null)
                layer.setName(s);

            if (styleCaps != null)
            {
                s = caps.getStyleName(layerCaps, styleCaps);
                if (s != null)
                    layer.setStyle(s);
            }

            makeLayerParams(caps, layerCaps, styleCaps, layer);

            LayerList ll = this.resultModel.getLayerList();
            if (ll == null)
            {
                ll = new LayerListImpl();
                this.resultModel.setLayerList(ll);
            }
            ll.addLayer(layer);
        }
    }

    protected void makeLayerParams(Capabilities caps, org.w3c.dom.Element layerCaps, org.w3c.dom.Element styleCaps,
                                   Layer dest)
    {
        if (caps == null)
        {
            String message = "nullValue.CapsIsNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (layerCaps == null)
        {
            String message = "nullValue.LayerCapsIsNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (dest == null)
        {
            String message = "nullValue.DestIsNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        String name = caps.getLayerName(layerCaps);
        if (name != null)
        {
            dest.setValue(AVKey.LAYER_NAMES, name);
            dest.setValue(CatalogKey.NAME, name);
        }

        String style = null;
        if (styleCaps != null)
        {
            style = caps.getStyleName(layerCaps, styleCaps);
            if (style != null)
                dest.setValue(AVKey.STYLE_NAMES, style);
        }

        String s = makeTitle(caps, name, style);
        if (s != null)
        {
            dest.setValue(AVKey.TITLE, makeWWJTitle(s));
            dest.setValue(CatalogKey.TITLE, s);
        }

        s = caps.getLayerAbstract(layerCaps);
        if (s != null)
        {
            dest.setValue(CatalogKey.ABSTRACT, s);
            dest.setValue(CatalogKey.DESCRIPTION, s);
        }

        dest.setValue(CatalogKey.LAYER_STATE, CatalogKey.LAYER_STATE_READY);

        // Provide a non-null value for UI elements looking for this action.
        dest.setValue(GIDBKey.ACTION_COMMAND_LAYER_PRESSED, dest);
    }

    protected String makeWWJTitle(String title)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        sb.append(title);
        sb.append("<br>");
        sb.append("(Retrieved from Catalog)");
        sb.append("</html>");
        return sb.toString();
    }

    private static String makeTitle(Capabilities caps, String layerNames, String styleNames)
    {
        String[] lNames = layerNames.split(",");
        String[] sNames = styleNames != null ? styleNames.split(",") : null;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lNames.length; i++)
        {
            if (sb.length() > 0)
                sb.append(", ");

            String layerName = lNames[i];
            org.w3c.dom.Element layer = caps.getLayerByName(layerName);
            String layerTitle = caps.getLayerTitle(layer);
            sb.append(layerTitle != null ? layerTitle : layerName);

            if (sNames == null || sNames.length <= i)
                continue;

            String styleName = sNames[i];
            org.w3c.dom.Element style = caps.getLayerStyleByName(layer, styleName);
            if (style == null)
                continue;

            sb.append(" : ");
            String styleTitle = caps.getStyleTitle(layer, style);
            sb.append(styleTitle != null ? styleTitle : styleName);
        }

        return sb.toString();
    }
}
