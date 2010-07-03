/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.applications.gos;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.applications.gos.event.SearchListener;
import gov.nasa.worldwind.avlist.AVList;

import javax.swing.event.*;

/**
 * @author dcollins
 * @version $Id: GeodataWindow.java 13127 2010-02-16 04:02:26Z dcollins $
 */
public interface GeodataWindow
{
    boolean isEnabled();

    void setEnabled(boolean enabled);

    boolean isRecordListWaiting();

    void setRecordListWaiting(boolean waiting);

    RecordList getRecordList();

    void setRecordList(RecordList recordList, AVList searchParams);

    GeodataController getGeodataController();

    void setGeodataController(GeodataController geodataController);

    WorldWindow getWorldWindow();

    void setWorldWindow(WorldWindow wwd);

    void addHyperlinkListener(HyperlinkListener listener);

    void removeHyperlinkListener(HyperlinkListener listener);

    HyperlinkListener[] getHyperlinkListeners();

    void addSearchListener(SearchListener listener);

    void removeSearchListener(SearchListener listener);

    SearchListener[] getSearchListeners();
}
