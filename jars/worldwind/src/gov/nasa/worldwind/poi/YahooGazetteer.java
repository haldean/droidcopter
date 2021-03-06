/*
Copyright (C) 2001, 2008 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/

package gov.nasa.worldwind.poi;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.exception.*;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.util.Logging;

import javax.xml.parsers.*;
import javax.xml.xpath.*;
import java.io.*;
import java.net.URLEncoder;
import java.util.*;
import java.util.logging.Level;

/**
 * A gazetteer that uses Yahoo's geocoding service to find locations for requested places.
 *
 * @author tag
 * @version $Id: YahooGazetteer.java 12942 2009-12-20 02:01:23Z patrickmurris $
 */
public class YahooGazetteer implements Gazetteer
{
    protected static final String GEOCODE_SERVICE =
        "http://local.yahooapis.com/MapsService/V1/geocode?appid=nasaworldwind&location=";

    public List<PointOfInterest> findPlaces(String lookupString) throws NoItemException, ServiceException
    {
        if (lookupString == null || lookupString.length() < 1)
            return null;

        String urlString;
        try
        {
            urlString = GEOCODE_SERVICE + URLEncoder.encode(lookupString, "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            urlString = GEOCODE_SERVICE + lookupString.replaceAll(" ", "+");
        }
        String locationString = POIUtils.callService(urlString);
        
        if (locationString == null || locationString.length() < 1)
            return null;

        return this.parseLocationString(locationString);
    }

    protected ArrayList<PointOfInterest> parseLocationString(String locationString) throws WWRuntimeException
    {
        try
        {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            docBuilderFactory.setNamespaceAware(false);
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            org.w3c.dom.Document doc = docBuilder.parse(new ByteArrayInputStream(locationString.getBytes("UTF-8")));

            XPathFactory xpFactory = XPathFactory.newInstance();
            XPath xpath = xpFactory.newXPath();

            org.w3c.dom.NodeList resultNodes =
                (org.w3c.dom.NodeList) xpath.evaluate("/ResultSet/Result", doc, XPathConstants.NODESET);

            ArrayList<PointOfInterest> positions = new ArrayList<PointOfInterest>(resultNodes.getLength());

            for (int i = 0; i < resultNodes.getLength(); i++)
            {
                org.w3c.dom.Node location = resultNodes.item(i);
                String lat = xpath.evaluate("Latitude", location);
                String lon = xpath.evaluate("Longitude", location);
                StringBuilder displayName = new StringBuilder();
                String str = xpath.evaluate("Address", location);               
                if (str.length() > 0)
                {
                    displayName.append(str);
                    displayName.append(", ");
                }
                displayName.append(xpath.evaluate("City", location));
                displayName.append(", ");
                displayName.append(xpath.evaluate("State", location));

                if (lat != null && lon != null)
                {
                    LatLon latlon = LatLon.fromDegrees(Double.parseDouble(lat), Double.parseDouble(lon));
                    PointOfInterest loc = new BasicPointOfInterest(latlon);
                    loc.setValue(AVKey.DISPLAY_NAME, displayName.toString());
                    positions.add(loc);
                }
            }

            return positions;
        }
        catch (Exception e)
        {
            String msg = Logging.getMessage("Gazetteer.URLException", locationString);
            Logging.logger().log(Level.SEVERE, msg);
            throw new WWRuntimeException(msg);
        }
    }
}
