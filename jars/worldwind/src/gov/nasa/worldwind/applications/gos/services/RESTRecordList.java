/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.applications.gos.services;

import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.applications.gos.*;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.exception.WWRuntimeException;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.retrieve.URLRetriever;
import gov.nasa.worldwind.util.*;
import org.w3c.dom.*;

import javax.xml.xpath.XPath;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;

/**
 * @author dcollins
 * @version $Id: RESTRecordList.java 13127 2010-02-16 04:02:26Z dcollins $
 */
public class RESTRecordList extends AVListImpl implements RecordList
{
    //protected int startIndex;
    //protected int pageSize;
    protected int recordCount;
    protected ArrayList<Record> records = new ArrayList<Record>();

    public static RecordList retrieve(URI uri) throws Exception
    {
        // TODO: draw defaults form configuration
        return retrieve(uri, 20000, 20000);
    }

    public static RecordList retrieve(URI uri, Integer connectTimeout, Integer readTimeout) throws Exception
    {
        if (uri == null)
        {
            String message = Logging.getMessage("nullValue.URIIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        InputStream is = null;
        try
        {
            URLRetriever retriever = URLRetriever.createRetriever(uri.toURL(), null);

            if (retriever == null)
            {
                String message = Logging.getMessage("generic.UnrecognizedProtocol", uri.getScheme());
                Logging.logger().severe(message);
                throw new WWRuntimeException(message);
            }

            if (connectTimeout != null)
                retriever.setConnectTimeout(connectTimeout);

            if (readTimeout != null)
                retriever.setReadTimeout(readTimeout);

            retriever.call();

            if (!retriever.getState().equals(URLRetriever.RETRIEVER_STATE_SUCCESSFUL))
            {
                String message = Logging.getMessage("generic.RetrievalFailed", uri.toString());
                Logging.logger().severe(message);
                throw new WWRuntimeException(message);
            }

            if (retriever.getBuffer() == null || retriever.getBuffer().limit() == 0)
            {
                String message = Logging.getMessage("generic.RetrievalReturnedNoContent", uri.toString());
                Logging.logger().severe(message);
                throw new WWRuntimeException(message);
            }

            is = WWIO.getInputStreamFromByteBuffer(retriever.getBuffer());

            return RESTRecordList.parse(WWXML.openDocumentStream(is, true));
        }
        catch (URISyntaxException e)
        {
            String message = Logging.getMessage("generic.URIInvalid", uri.toString());
            Logging.logger().log(java.util.logging.Level.SEVERE, message, e);
            throw e;
        }
        finally
        {
            WWIO.closeStream(is, uri.toString());
        }
    }

    public static RecordList parse(Document doc)
    {
        XPath xpath = WWXML.makeXPath();

        //Integer pageSize = WWXML.getInteger(doc.getDocumentElement(),
        //    "/html/head/meta[@name=\"itemsPerPage\"]/@content", xpath);
        //if (pageSize == null)
        //    pageSize = 0;
        //
        //Integer startIndex = WWXML.getInteger(doc.getDocumentElement(),
        //    "/html/head/meta[@name=\"startIndex\"]/@content", xpath);
        //if (startIndex == null)
        //    startIndex = 0;
        //else
        //    startIndex = startIndex - 1;

        Integer recordCount = WWXML.getInteger(doc.getDocumentElement(),
            "/html/head/meta[@name=\"totalResults\"]/@content", xpath);
        if (recordCount == null)
            recordCount = 0;

        ArrayList<Record> list = new ArrayList<Record>();

        Element[] els = WWXML.getElements(doc.getDocumentElement(), "/html/body/div[@class=\"snippet\"]", xpath);
        if (els != null && els.length > 0)
        {
            for (Element el : els)
            {
                if (el == null)
                    continue;

                Record r = createRecord(el, xpath);
                if (r == null)
                    continue;

                list.add(r);
            }
        }

        return new RESTRecordList(/*startIndex, pageSize, */recordCount, list);
    }

    public RESTRecordList(/*int startIndex, int pageSize, */int recordCount, ArrayList<Record> records)
    {
        //this.startIndex = startIndex;
        //this.pageSize = pageSize;
        this.recordCount = recordCount;
        this.records = records;
    }

    //public int getPageSize()
    //{
    //    return this.pageSize;
    //}

    //public int getStartIndex()
    //{
    //    return this.startIndex;
    //}

    public int getRecordCount()
    {
        return this.recordCount;
    }

    public Iterable<? extends Record> getRecords()
    {
        return this.records;
    }

    protected static Record createRecord(Element context, XPath xpath)
    {
        String title = WWXML.getText(context, "./div[@class=\"title\"]", xpath);
        if (title != null)
            title = RESTUtil.stripNewlineCharacters(title);

        String abstractText = WWXML.getText(context, "./div[@class=\"abstract\"]", xpath);
        if (abstractText != null)
            abstractText = RESTUtil.stripNewlineCharacters(abstractText);

        HashMap<String, OnlineResource> resourceMap = new HashMap<String, OnlineResource>();

        // Populate generic resources from the record's links.
        Element[] els = WWXML.getElements(context, "./div[@class=\"links\"]/A", xpath);
        if (els != null && els.length > 0)
        {
            for (Element el : els)
            {
                if (el == null)
                    continue;

                OnlineResource r = createLinkResource(el, xpath);
                if (r == null)
                    continue;

                resourceMap.put(r.getName(), r);
            }
        }

        // Populate the icon resource.
        Element el = WWXML.getElement(context, "./div[@class=\"title\"]/img", xpath);
        if (el != null)
        {
            OnlineResource r = createImageResource(el, xpath);
            if (r != null)
                resourceMap.put(r.getName(), r);
        }

        // Populate the uuid and service type properties from any resource which can supply those values.
        String uuid = null;
        String serviceType = null;
        String mapServer = null;
        for (OnlineResource r : resourceMap.values())
        {
            AVList params = RESTUtil.parseHTTPGetString(r.getURI().toString());
            if (params == null)
                continue;

            String s = params.getStringValue("uuid");
            if (!WWUtil.isEmpty(s))
                uuid = RESTUtil.replaceEncodedURLCharacters(s);

            s = params.getStringValue("serviceType");
            if (!WWUtil.isEmpty(s))
                serviceType = RESTUtil.replaceEncodedURLCharacters(s);

            if (r.getName().equals("map"))
            {
                s = params.getStringValue("server");
                if (!WWUtil.isEmpty(s))
                    mapServer = RESTUtil.replaceEncodedURLCharacters(s);
            }
        }

        // Populate the sector property from the record's metadata documents.
        Sector sector = null;
        if (uuid != null)
        {
            Document metadata = getMetadata(uuid);
            if (metadata != null)
            {
                String s = WWXML.getText(metadata.getDocumentElement(), "//xmlDoc", xpath);
                if (!WWUtil.isEmpty(s))
                {
                    InputStream is = null;
                    try
                    {
                        is = WWIO.getInputStreamFromString(s);

                        Document xmlDoc = WWXML.openDocumentStream(is, false);
                        el = WWXML.getElement(xmlDoc.getDocumentElement(), "//spdom/bounding", xpath);
                        if (el != null)
                        {
                            Double minLat = WWXML.getDouble(el, "southbc", xpath);
                            Double maxLat = WWXML.getDouble(el, "northbc", xpath);
                            Double minLon = WWXML.getDouble(el, "westbc", xpath);
                            Double maxLon = WWXML.getDouble(el, "eastbc", xpath);
                            if (minLat != null && maxLat != null && minLon != null && maxLon != null)
                            {
                                sector = Sector.fromDegrees(minLat, maxLat, minLon, maxLon);
                            }
                        }
                    }
                    finally
                    {
                        WWIO.closeStream(is, null);
                    }
                }
            }
        }

        // Populate the metadata resource.
        if (uuid != null)
        {
            OnlineResource r = createMetadataResource(uuid);
            if (r != null)
                resourceMap.put(r.getName(), r);
        }

        // Populate the service status resources.
        if (uuid != null && serviceType != null)
        {
            OnlineResource r = FGDCServiceStatus.createServiceStatusMetadataResource(uuid);
            if (r != null)
                resourceMap.put(r.getName(), r);

            r = FGDCServiceStatus.createServiceStatusResource(uuid, serviceType);
            if (r != null)
                resourceMap.put(r.getName(), r);
        }

        if (serviceType != null && serviceType.equalsIgnoreCase("wms") && mapServer != null)
        {
            OnlineResource r = createOGCCapabilitiesResource(mapServer);
            if (r != null)
                resourceMap.put(r.getName(), r);
        }

        // Populate the website resource.
        OnlineResource r = resourceMap.get("website");
        if (r != null)
        {
            AVList params = RESTUtil.parseHTTPGetString(r.getURI().toString());
            if (params != null)
            {
                String s = params.getStringValue("url");
                if (!WWUtil.isEmpty(s))
                {
                    s = RESTUtil.replaceEncodedURLCharacters(s);
                    r = createWebsiteResource(s);
                    if (r != null)
                        resourceMap.put(r.getName(), r);
                }
            }
        }

        ShapeAttributes shapeAttr = new BasicShapeAttributes();
        shapeAttr.setInteriorMaterial(Material.BLUE);
        shapeAttr.setOutlineMaterial(new Material(WWUtil.makeColorBrighter(Color.BLUE)));
        shapeAttr.setInteriorOpacity(0.4);
        shapeAttr.setOutlineOpacity(0.8);
        shapeAttr.setOutlineWidth(3);

        return new BasicRecord(uuid, title, sector, abstractText, resourceMap, shapeAttr);
    }

    protected static OnlineResource createResource(String key, String displayText, URI uri)
    {
        return new BasicOnlineResource(key, displayText, uri);
    }

    protected static OnlineResource createImageResource(Element context, XPath xpath)
    {
        String alt = WWXML.getText(context, "@alt", xpath);
        String src = WWXML.getText(context, "@src", xpath);

        URI uri = null;
        try
        {
            if (!WWUtil.isEmpty(src))
                uri = new URI(src);
        }
        catch (URISyntaxException e)
        {
            String message = Logging.getMessage("gosApp.ImageURIInvalid", src);
            Logging.logger().severe(message);
        }

        return createResource(GeodataKey.IMAGE, alt, uri);
    }

    protected static OnlineResource createLinkResource(Element context, XPath xpath)
    {
        String text = WWXML.getText(context, "text()", xpath);
        if (WWUtil.isEmpty(text))
            return null;

        String href = WWXML.getText(context, "@HREF", xpath);
        if (WWUtil.isEmpty(href))
            return null;

        URI uri;
        try
        {
            uri = new URI(href);
        }
        catch (URISyntaxException e)
        {
            String message = Logging.getMessage("gosApp.ResourceURIInvalid", href);
            Logging.logger().severe(message);
            return null;
        }

        return createResource(text.toLowerCase(), text, uri);
    }

    protected static OnlineResource createMetadataResource(String uuid)
    {
        String baseURI = Configuration.getStringValue(GeodataKey.METADATA_URI);
        if (WWUtil.isEmpty(baseURI))
            return null;

        URI uri;
        try
        {
            Request request = new Request(new URI(baseURI));
            request.setParam("uuid", uuid);
            request.setParam("xmltransform", "metadata_details.xsl");
            uri = request.getUri();
        }
        catch (URISyntaxException e)
        {
            String message = Logging.getMessage("gosApp.MetadataURIInvalid", baseURI);
            Logging.logger().severe(message);
            return null;
        }

        return createResource(GeodataKey.METADATA, "Metadata", uri);
    }

    protected static OnlineResource createOGCCapabilitiesResource(String service)
    {
        if (WWUtil.isEmpty(service))
            return null;

        URI uri;
        try
        {
            uri = new URI(service);
        }
        catch (URISyntaxException e)
        {
            String message = Logging.getMessage("gosApp.GetCapabilitiesURIInvalid", service);
            Logging.logger().severe(message);
            return null;
        }

        return createResource(GeodataKey.CAPABILITIES, "GetCapabilities", uri);
    }

    protected static OnlineResource createWebsiteResource(String urlString)
    {
        if (WWUtil.isEmpty(urlString))
            return null;

        URI uri;
        try
        {
            uri = new URI(urlString);
        }
        catch (URISyntaxException e)
        {
            String message = Logging.getMessage("gosApp.WebsiteURIInvalid", urlString);
            Logging.logger().severe(message);
            return null;
        }

        return createResource(GeodataKey.WEBSITE, "Website", uri);
    }

    protected static Document getMetadata(String uuid) // TODO: replace this with proper BBOX extraction from record
    {
        String baseURI = Configuration.getStringValue(GeodataKey.METADATA_URI);
        if (WWUtil.isEmpty(baseURI))
            return null;

        try
        {
            Request request = new Request(new URI(baseURI));
            request.setParam("uuid", uuid);
            return ResourceUtil.openDocumentURI(request.getUri());
        }
        catch (Exception e)
        {
            return null;
        }
    }
}
