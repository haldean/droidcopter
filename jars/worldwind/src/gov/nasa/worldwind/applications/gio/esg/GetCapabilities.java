/*
Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.applications.gio.esg;

import gov.nasa.worldwind.applications.gio.catalogui.*;
import gov.nasa.worldwind.applications.gio.csw.CSWConnectionPool;
import gov.nasa.worldwind.applications.gio.ebrim.*;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.wms.Capabilities;

import java.net.URI;
import java.util.*;

/**
 * @author dcollins
 * @version $Id: GetCapabilities.java 9600 2009-03-22 20:04:40Z tgaskins $
 */
public class GetCapabilities
{
    private ESGResultModel resultModel;
    private ServicePackage servicePackage;

    public GetCapabilities(ESGResultModel resultModel)
    {
        if (resultModel == null)
        {
            String message = "nullValue.ResultModelIsNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (resultModel.getServicePackage() == null)
        {
            String message = "nullValue.ResultModelServicePackageIsNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (resultModel.getServicePackage().getService() == null)
        {
            String message = "nullValue.ResultModelServiceIsNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.resultModel = resultModel;
        this.servicePackage = resultModel.getServicePackage();
    }

    public void executeRequest(CSWConnectionPool connectionPool) throws Exception
    {
        if (connectionPool == null)
        {
            String message = "nullValue.ConnectionPoolIsNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        // Attempt to get WMS capabilities if the service is not known,
        // or if the service is WMS.
        Object o = this.resultModel.getValue(CatalogKey.SERVICE_TYPE);
        if (o == null || o.equals(CatalogKey.WMS))
        {
            setCapabilities();
            setServiceData();
        }
    }

    protected void setCapabilities() throws Exception
    {
        String uriString = null;
        boolean hasGetCaps = false;
        boolean hasGetMap = false;
        Service service = this.servicePackage.getService();
        for (Iterator<ServiceBinding> iter = service.getServiceBindingIterator(); iter.hasNext(); )
        {
            ServiceBinding serviceBinding = iter.next();
            if (serviceBinding != null)
            {
                InternationalString is = serviceBinding.getName();
                if (is != null)
                {
                    String s = RegistryObjectUtils.getStringForLocale(is, Locale.getDefault());
                    if ("GetCapabilities".equalsIgnoreCase(s))
                    {
                        uriString = serviceBinding.getAccessURI();
                        hasGetCaps = true;
                    }
                    else if ("GetMap".equalsIgnoreCase(s))
                    {
                        hasGetMap = true;
                    }
                }
            }
        }

        Capabilities caps = null;
        if (uriString != null && hasGetMap && hasGetCaps)
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
                CatalogException esgEx = new CatalogException(message, null);
                this.resultModel.addException(esgEx);
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

    protected void setServiceData()
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
                        associateWithServiceData(caps, layerCaps);
                    }
                }
            }
        }
    }

    protected void associateWithServiceData(Capabilities caps, org.w3c.dom.Element layerCaps)
    {
        String layerName = null;
        if (caps != null && layerCaps != null)
        {
            layerName = caps.getLayerName(layerCaps);
        }

        ServiceData layerData = null;
        for (Iterator<ServiceData> iter = this.servicePackage.getServiceDataIterator(); iter.hasNext(); )
        {
            ServiceData serviceData = iter.next();
            if (serviceData != null)
            {
                ExtrinsicObject eo = serviceData.getExtrinsicObject();
                if (eo != null)
                {
                    InternationalString is = eo.getName();
                    if (is != null)
                    {
                        String s = RegistryObjectUtils.getStringForLocale(is, Locale.getDefault());
                        if (s != null)
                        {
                            if (s.equalsIgnoreCase(layerName))
                            {
                                layerData = serviceData;
                                break;
                            }
                        }
                    }
                }
            }
        }

        if (layerData != null)
        {
            makeLayerParams(layerData);
        }
    }

    protected void makeLayerParams(ServiceData serviceData)
    {
        if (serviceData == null)
            return;

        ExtrinsicObject eo = serviceData.getExtrinsicObject();
        if (eo != null)
        {
            InternationalString is = eo.getName();
            if (is != null)
            {
                String s = RegistryObjectUtils.getStringForLocale(is, Locale.getDefault());
                if (s != null)
                    serviceData.setValue(AVKey.LAYER_NAMES, s);
            }

            for (Iterator<Slot> iter = eo.getSlotIterator(); iter.hasNext(); )
            {
                Slot slot = iter.next();
                if (slot != null)
                {
                    String name = slot.getName();
                    if (name != null && (name.contains("title") || name.contains("Title")))
                    {
                        String[] sv = RegistryObjectUtils.getValues(slot);
                        if (sv != null)
                            serviceData.setValue(AVKey.TITLE, makeWWJLayerTitle(sv));
                    }
                }
            }
        }

        serviceData.setValue(CatalogKey.LAYER_STATE, CatalogKey.LAYER_STATE_READY);

        // Provide a non-null value for UI elements looking for this action.
        serviceData.setValue(ESGKey.ACTION_COMMAND_SERVICE_DATA_PRESSED, serviceData);
    }

    protected String makeWWJLayerTitle(String[] defaultTitle)
    {
        StringBuilder sb = new StringBuilder();
        for (String s : defaultTitle)
        {
            if (sb.length() > 0)
                sb.append("<br>");
            sb.append(s);
        }
        sb.insert(0, "<html>");
        sb.append("<br>");
        sb.append("(Retrieved from Catalog)");
        sb.append("</html>");
        return sb.toString();
    }
}
