/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.applications.gos;

import gov.nasa.worldwind.avlist.AVList;

import java.net.URL;

/**
 * @author dcollins
 * @version $Id: GeodataController.java 13127 2010-02-16 04:02:26Z dcollins $
 */
public interface GeodataController
{
    GeodataWindow getGeodataWindow();

    void setGeodataWindow(GeodataWindow gwd);

    void executeSearch(AVList params);

    void openBrowser(URL url);
}
