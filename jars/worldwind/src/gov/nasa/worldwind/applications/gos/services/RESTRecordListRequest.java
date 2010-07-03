/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.applications.gos.services;

import gov.nasa.worldwind.applications.gos.GeodataKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.util.WWUtil;

import java.net.*;

/**
 * @author dcollins
 * @version $Id: RESTRecordListRequest.java 13127 2010-02-16 04:02:26Z dcollins $
 */
public class RESTRecordListRequest extends Request
{
    public RESTRecordListRequest()
    {
    }

    public RESTRecordListRequest(URI uri, AVList params) throws URISyntaxException
    {
        super(uri);
        this.initialize(params);
    }

    public RESTRecordListRequest(Request sourceRequest) throws URISyntaxException
    {
        super(sourceRequest);
    }

    protected void initialize(AVList params)
    {
        if (params == null)
            return;

        // Set the bounding box to search, and the spatial relationship between the bounding box and the database
        // records.
        Object o = params.getValue(GeodataKey.BBOX);
        if (o != null && o instanceof Sector)
        {
            Sector s = (Sector) o;
            StringBuilder sb = new StringBuilder();
            sb.append(s.getMinLongitude().getDegrees());
            sb.append(",").append(s.getMinLatitude().getDegrees());
            sb.append(",").append(s.getMaxLongitude().getDegrees());
            sb.append(",").append(s.getMaxLatitude().getDegrees());

            // Set the bounding box: two pairs of comma delimited coordinates repsesenting the west-south corner and
            // east-north corner.
            this.setParam("bbox", sb.toString());
            // Set the spatial relationship. The metadata envelope must at least overlap with the requested
            // bounding box.
            this.setParam("spatialRel", "esriSpatialRelOverlaps");
        }

        // Set the text to be searched within record metadata. Search for any word specified in the search text.
        o = params.getValue(GeodataKey.SEARCH_TEXT);
        if (o != null)
        {
            String s = o.toString();
            if (!WWUtil.isEmpty(s))
            {
                this.setParam("searchText", s);
                this.setParam("contains", "true");
            }
        }

        // Set the content types to search.
        o = params.getValue(GeodataKey.CONTENT_TYPE_LIST);
        if (o != null && o instanceof Iterable)
        {
            StringBuilder sb = new StringBuilder();
            for (Object type : (Iterable) o)
            {
                if (sb.length() > 0)
                    sb.append(",");
                sb.append(type);
            }

            if (sb.length() > 0)
                this.setParam("contentType", sb.toString());
        }

        // Set the data categories to search.
        o = params.getValue(GeodataKey.DATA_CATEGORY_LIST);
        if (o != null && o instanceof Iterable)
        {
            StringBuilder sb = new StringBuilder();
            for (Object subject : (Iterable) o)
            {
                if (sb.length() > 0)
                    sb.append(",");
                sb.append(subject);
            }

            if (sb.length() > 0)
                this.setParam("dataCategory", sb.toString());
        }

        // Set the 'after' parameter here.

        // Set the 'before' parameter here.

        // Set the result feed sort order.
        o = params.getValue(GeodataKey.SORT_ORDER);
        if (o != null)
        {
            String s = o.toString();
            if (!WWUtil.isEmpty(s))
                this.setParam("orderBy", s);
        }

        // Set the starting record position in the result feed.
        o = params.getValue(GeodataKey.RECORD_START_INDEX);
        if (o != null)
        {
            Integer i = WWUtil.convertStringToInteger(o.toString());
            if (i != null)
                this.setParam("start", Integer.toString(i + 1));
        }

        // Set the maximum number of records in the result feed.
        o = params.getValue(GeodataKey.RECORD_PAGE_SIZE);
        if (o != null)
        {
            String s = o.toString();
            if (!WWUtil.isEmpty(s))
                this.setParam("max", s);
        }

        // Set the geometry returned in the result feed to poygons.
        this.setParam("geometryType", "esriGeometryPolygon");

        // Set the 'source' parameter here.

        // Set the search scope to include all of the content of GOS.
        this.setParam("isPartOf", "geodata.gov");
        
        // Set the record output format to HTML.
        this.setParam("f", "html");

        // Set the 'style' parameter here.

        // Set the 'target' parameter here.
    }
}
