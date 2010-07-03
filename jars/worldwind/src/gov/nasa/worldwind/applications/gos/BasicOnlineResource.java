/* Copyright (C) 2001, 2009 United States Government as represented by 
the Administrator of the National Aeronautics and Space Administration. 
All Rights Reserved. 
*/
package gov.nasa.worldwind.applications.gos;

import java.net.URI;

/**
 * @author dcollins
 * @version $Id: BasicOnlineResource.java 13127 2010-02-16 04:02:26Z dcollins $
 */
public class BasicOnlineResource implements OnlineResource
{
    protected String name;
    protected String displayText;
    protected URI uri;

    public BasicOnlineResource(String name, String displayText, URI uri)
    {
        this.name = name;
        this.displayText = displayText;
        this.uri = uri;
    }

    public String getName()
    {
        return this.name;
    }

    public String getDisplayText()
    {
        return this.displayText;
    }

    public URI getURI()
    {
        return this.uri;
    }
}
