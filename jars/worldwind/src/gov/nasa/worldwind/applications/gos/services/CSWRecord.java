/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.applications.gos.services;

import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.applications.gos.*;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.util.*;
import org.w3c.dom.Element;

import javax.xml.xpath.XPath;
import java.awt.*;
import java.net.*;
import java.util.*;

/**
 * @author dcollins
 * @version $Id: CSWRecord.java 13127 2010-02-16 04:02:26Z dcollins $
 */
public class CSWRecord extends AVListImpl implements Record
{
    protected Element domElement;
    protected XPath xpath;
    protected Sector sector;
    protected Map<String, OnlineResource> resourceMap = new HashMap<String, OnlineResource>();
    protected ShapeAttributes shapeAttr = new BasicShapeAttributes();

    protected CSWRecord(Element domElement, XPath xpath)
    {
        this.domElement = domElement;
        this.xpath = xpath;
        this.init();
    }

    protected void init()
    {
        String format = WWXML.getText(this.domElement, "./dct:hasFormat", this.xpath);

        // Initialize the "Server" resource.
        String s = WWXML.getText(this.domElement,
            "./dct:references[@scheme=\"urn:x-esri:specification:ServiceType:ArcIMS:Metadata:Server\"]", this.xpath);
        if (!WWUtil.isEmpty(s))
        {
            URI uri = uriFromString(s);
            if (uri != null)
            {
                if (!WWUtil.isEmpty(format) && format.equalsIgnoreCase("wms"))
                    this.addResource(new BasicOnlineResource(GeodataKey.CAPABILITIES, "GetCapabilities", uri));
            }
        }

        //// Initialize the "Document" resource.
        //s = WWXML.getText(this.domElement,
        //    "./dct:references[@scheme=\"urn:x-esri:specification:ServiceType:ArcIMS:Metadata:Document\"]", this.xpath);
        //if (!WWUtil.isEmpty(s))
        //{
        //    URI uri = uriFromString(s);
        //    if (uri != null)
        //        this.addResource(new BasicOnlineResource(GeodataKey.METADATA, "Metadata", uri));
        //}

        // Initialize the "Onlink" resource.
        s = WWXML.getText(this.domElement,
            "./dct:references[@scheme=\"urn:x-esri:specification:ServiceType:ArcIMS:Metadata:Onlink\"]", this.xpath);
        if (!WWUtil.isEmpty(s))
        {
            URI uri = uriFromString(s);
            if (uri != null)
                this.addResource(new BasicOnlineResource(GeodataKey.WEBSITE, "Website", uri));
        }

        // Initialize the image resource.
        if (!WWUtil.isEmpty(this.getType()))
        {
            Element el = Configuration.getElement("//ContentTypeList/Category[@key=\"" + this.getType() + "\"]");
            if (el != null)
            {
                String imageResource = WWXML.getText(el, "./@imageResource", this.xpath);
                String displayName = WWXML.getText(el, "./@displayName", this.xpath);
                if (!WWUtil.isEmpty(imageResource))
                {
                    URI uri = ResourceUtil.getResourceURI(imageResource);
                    if (uri != null)
                        this.addResource(new BasicOnlineResource(GeodataKey.IMAGE, displayName, uri));
                }
            }
        }

        // Initialize the metadata resource.
        if (!WWUtil.isEmpty(this.getIdentifier()))
        {
            URI uri = getStyledMetadataURI(this.getIdentifier());
            if (uri != null)
                this.addResource(new BasicOnlineResource(GeodataKey.METADATA, "Metadata", uri));
        }

        // Initialize the service status resources.
        if (!WWUtil.isEmpty(this.getIdentifier()) && !WWUtil.isEmpty(this.getType()) && this.getType().equalsIgnoreCase(
            "liveData"))
        {
            OnlineResource r = FGDCServiceStatus.createServiceStatusMetadataResource(this.getIdentifier());
            if (r != null)
                this.addResource(r);

            if (!WWUtil.isEmpty(format))
            {
                r = FGDCServiceStatus.createServiceStatusResource(this.getIdentifier(), format);
                if (r != null)
                    this.addResource(r);
            }
        }

        // Initialize the geographic bounds.
        Element el = WWXML.getElement(this.domElement, "./ows:WGS84BoundingBox", this.xpath);
        if (el != null)
            this.sector = sectorFromOWSBoundingBox(el, this.xpath);

        // Initialize shape attributes.
        this.shapeAttr.setInteriorMaterial(Material.BLUE);
        this.shapeAttr.setOutlineMaterial(new Material(WWUtil.makeColorBrighter(Color.BLUE)));
        this.shapeAttr.setInteriorOpacity(0.4);
        this.shapeAttr.setOutlineOpacity(0.8);
        this.shapeAttr.setOutlineWidth(3);
    }

    protected void addResource(OnlineResource resource)
    {
        this.resourceMap.put(resource.getName(), resource);
    }

    public Record parse(Element domElement)
    {
        return new CSWRecord(domElement, WWXML.makeXPath());
    }

    public String getIdentifier()
    {
        return WWXML.getText(this.domElement, "./dc:identifier", this.xpath);
    }

    public String getTitle()
    {
        return WWXML.getText(this.domElement, "./dc:title", this.xpath);
    }

    public String getType()
    {
        return WWXML.getText(this.domElement, "./dc:type", this.xpath);
    }

    public Sector getSector()
    {
        return this.sector;
    }

    public String getAbstract()
    {
        return WWXML.getText(this.domElement, "./dct:abstract", this.xpath);
    }

    public OnlineResource getResource(String key)
    {
        return this.resourceMap.get(key);
    }

    public Iterable<OnlineResource> getResources()
    {
        return this.resourceMap.values();
    }

    public Iterable<LatLon> getLocations()
    {
        Sector sector = this.getSector();
        return (sector != null) ? sector.asList() : null;
    }

    public ShapeAttributes getShapeAttributes()
    {
        return this.shapeAttr;
    }

    protected static Sector sectorFromOWSBoundingBox(Element el, XPath xpath)
    {
        String lowerCorner = WWXML.getText(el, "./ows:LowerCorner", xpath);
        String upperCorner = WWXML.getText(el, "./ows:UpperCorner", xpath);
        if (WWUtil.isEmpty(lowerCorner) || WWUtil.isEmpty(upperCorner))
            return null;

        LatLon ll = latlonFromOWSCoordinates(lowerCorner);
        LatLon ur = latlonFromOWSCoordinates(upperCorner);
        if (ll == null || ur == null)
            return null;

        return new Sector(ll.getLatitude(), ur.getLatitude(), ll.getLongitude(), ur.getLongitude());
    }

    protected static LatLon latlonFromOWSCoordinates(String text)
    {
        String[] tokens = text.split(" ");
        if (tokens == null || tokens.length != 2 || WWUtil.isEmpty(tokens[0]) || WWUtil.isEmpty(tokens[1]))
            return null;

        // OWS coordinates are stored in the following order: (lon lat).
        Double lon = WWUtil.convertStringToDouble(tokens[0]);
        Double lat = WWUtil.convertStringToDouble(tokens[1]);
        if (lon == null || lat == null)
            return null;

        return LatLon.fromDegrees(lat, lon);
    }

    protected static URI uriFromString(String uriString)
    {
        try
        {
            return new URI(uriString);
        }
        catch (URISyntaxException e)
        {
            String message = Logging.getMessage("gosApp.ResourceURIInvalid", uriString);
            Logging.logger().severe(message);
            return null;
        }
    }

    protected static URI getStyledMetadataURI(String uuid)
    {
        String baseURI = Configuration.getStringValue(GeodataKey.METADATA_URI);
        if (WWUtil.isEmpty(baseURI))
            return null;

        try
        {
            Request request = new Request(new URI(baseURI));
            request.setParam("uuid", uuid);
            request.setParam("xmltransform", "metadata_details.xsl");
            return request.getUri();
        }
        catch (URISyntaxException e)
        {
            String message = Logging.getMessage("gosApp.MetadataURIInvalid", baseURI);
            Logging.logger().severe(message);
            return null;
        }
    }
}
