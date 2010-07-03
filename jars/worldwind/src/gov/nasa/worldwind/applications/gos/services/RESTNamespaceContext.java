/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.applications.gos.services;

import gov.nasa.worldwind.util.BasicNamespaceContext;

import javax.xml.XMLConstants;

/**
 * @author dcollins
 * @version $Id: RESTNamespaceContext.java 13127 2010-02-16 04:02:26Z dcollins $
 */
public class RESTNamespaceContext extends BasicNamespaceContext
{
    public static final String REST_NS_PREFIX = "rest";
    public static final String REST_NS_URI = "http://registry.gsdi.org/statuschecker/services/rest/";

    public RESTNamespaceContext()
    {
        this.addNamespace(REST_NS_PREFIX, REST_NS_URI);
        this.addNamespace(XMLConstants.DEFAULT_NS_PREFIX, REST_NS_URI);
    }
}
