/* Copyright (C) 2001, 2009 United States Government as represented by 
the Administrator of the National Aeronautics and Space Administration. 
All Rights Reserved. 
*/
package gov.nasa.worldwind.applications.gos.event;

import java.util.EventListener;

/**
 * @author dcollins
 * @version $Id: SearchListener.java 13127 2010-02-16 04:02:26Z dcollins $
 */
public interface SearchListener extends EventListener
{
    void searchPerformed(SearchEvent event);
}
