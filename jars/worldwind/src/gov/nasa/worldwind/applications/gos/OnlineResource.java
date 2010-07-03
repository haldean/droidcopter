/* Copyright (C) 2001, 2009 United States Government as represented by 
the Administrator of the National Aeronautics and Space Administration. 
All Rights Reserved. 
*/
package gov.nasa.worldwind.applications.gos;

import java.net.URI;

/**
 * @author dcollins
 * @version $Id: OnlineResource.java 13127 2010-02-16 04:02:26Z dcollins $
 */
public interface OnlineResource
{
    String getName();

    String getDisplayText();

    URI getURI();
}
