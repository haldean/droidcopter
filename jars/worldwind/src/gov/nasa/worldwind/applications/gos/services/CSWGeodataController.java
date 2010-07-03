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
 * @version $Id: CSWGeodataController.java 13130 2010-02-16 05:04:35Z dcollins $
 */
public class CSWGeodataController extends AbstractGeodataController
{
    public CSWGeodataController()
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
        String requestString = null;

        String service = Configuration.getStringValue(GeodataKey.CSW_SERVICE_URI);
        try
        {
            CSWGetRecordsRequest request = new CSWGetRecordsRequest(new URI(service));
            uri = request.getUri();
        }
        catch (URISyntaxException e)
        {
            String message = Logging.getMessage("gosApp.CSWServiceURIInvalid", service);
            Logging.logger().log(java.util.logging.Level.SEVERE, message, e);
            return null;
        }

        if (params != null)
            requestString = this.createRequestString(params);

        try
        {
            return CSWRecordList.retrieve(uri, requestString);
        }
        catch (Exception e)
        {
            String message = Logging.getMessage("gosApp.ExceptionRetrievingRecordList", service);
            Logging.logger().log(java.util.logging.Level.SEVERE, message, e);
            return null;
        }
    }

    protected String createRequestString(AVList params)
    {
        CSWQueryBuilder qb = new CSWQueryBuilder(params);
        return qb.getGetRecordsString();
    }
}
