/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.applications.gos.services;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.applications.gos.*;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.util.Logging;

import java.net.*;

/**
 * @author dcollins
 * @version $Id: RESTGeodataController.java 13130 2010-02-16 05:04:35Z dcollins $
 */
public class RESTGeodataController extends AbstractGeodataController
{
    public RESTGeodataController()
    {
    }

    protected void doExecuteSearch(final AVList params, final Runnable afterSearch)
    {
        ResourceUtil.getAppTaskService().execute(new Runnable()
        {
            public void run()
            {
                try
                {
                    retrieveAndUpdateRecordList(params);
                }
                finally
                {
                    afterSearch.run();
                }
            }
        });
    }

    protected void retrieveAndUpdateRecordList(AVList params)
    {
        RecordList recordList = retrieveRecordList(params);
        this.updateRecordList(recordList, params);
    }

    protected RecordList retrieveRecordList(AVList params)
    {
        URI uri;

        String service = Configuration.getStringValue(GeodataKey.REST_SERVICE_URI);
        try
        {
            RESTRecordListRequest request = new RESTRecordListRequest(new URI(service), params);
            uri = request.getUri();
        }
        catch (URISyntaxException e)
        {
            String message = Logging.getMessage("gosApp.RestServiceURIInvalid", service);
            Logging.logger().log(java.util.logging.Level.SEVERE, message, e);
            return null;
        }

        try
        {
            return RESTRecordList.retrieve(uri);
        }
        catch (Exception e)
        {
            String message = Logging.getMessage("gosApp.ExceptionRetrievingRecordList", service);
            Logging.logger().log(java.util.logging.Level.SEVERE, message, e);
            return null;
        }
    }
}
