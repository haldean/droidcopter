/*
Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.applications.gos;

import gov.nasa.worldwind.applications.gos.event.*;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.util.*;

import javax.swing.*;
import javax.swing.event.*;
import java.net.URL;

/**
 * @author dcollins
 * @version $Id: AbstractGeodataController.java 13127 2010-02-16 04:02:26Z dcollins $
 */
public abstract class AbstractGeodataController implements GeodataController, HyperlinkListener, SearchListener
{
    protected GeodataWindow gwd;
    protected boolean searchEnabled = true;

    public AbstractGeodataController()
    {
    }

    protected abstract void doExecuteSearch(AVList params, Runnable afterSearch);

    public boolean isSearchEnabled()
    {
        return this.searchEnabled;
    }

    public void setSearchEnabled(boolean enabled)
    {
        this.searchEnabled = enabled;

        if (this.gwd != null)
            this.gwd.setEnabled(enabled);
    }

    public GeodataWindow getGeodataWindow()
    {
        return this.gwd;
    }

    public void setGeodataWindow(GeodataWindow gwd)
    {
        if (this.gwd == gwd)
            return;

        if (this.gwd != null)
        {
            this.gwd.removeHyperlinkListener(this);
            this.gwd.removeSearchListener(this);
        }

        this.gwd = gwd;

        if (this.gwd != null)
        {
            this.gwd.setEnabled(this.searchEnabled);
            this.gwd.addHyperlinkListener(this);
            this.gwd.addSearchListener(this);
        }
    }

    public void executeSearch(final AVList params)
    {
        if (!this.isSearchEnabled())
            return;

        this.beforeSearch();
        this.doExecuteSearch(params, new Runnable()
        {
            public void run()
            {
                afterSearch();
            }
        });
    }

    public void openBrowser(URL url)
    {
        if (url == null)
        {
            String message = Logging.getMessage("nullValue.URLIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        try
        {
            BrowserOpener.browse(url);
        }
        catch (Exception e)
        {
            String message = Logging.getMessage("generic.ExceptionAttemptingToInvokeWebBrower", url.toString());
            Logging.logger().log(java.util.logging.Level.SEVERE, message, e);
        }
    }

    public void hyperlinkUpdate(HyperlinkEvent event)
    {
        if (event == null)
            return;

        if (event.getSource() instanceof JComponent)
        {
            JComponent c = ((JComponent) event.getSource());
            c.setToolTipText(null);

            if (event.getEventType() == HyperlinkEvent.EventType.ENTERED && !WWUtil.isEmpty(event.getDescription()))
                c.setToolTipText(event.getDescription());
        }

        if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED && event.getURL() != null)
        {
            openBrowser(event.getURL());
        }
    }

    public void searchPerformed(SearchEvent event)
    {
        if (event == null)
            return;

        this.executeSearch(event.getParams());
    }

    protected void updateRecordList(final RecordList recordList, final AVList searchParams)
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    updateRecordList(recordList, searchParams);
                }
            });
        }
        else
        {
            this.getGeodataWindow().setRecordList(recordList, searchParams);
        }
    }

    protected void beforeSearch()
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    beforeSearch();
                }
            });
        }
        else
        {
            this.setSearchEnabled(false);
            this.gwd.setRecordListWaiting(true);
        }
    }

    protected void afterSearch()
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    afterSearch();
                }
            });
        }
        else
        {
            this.setSearchEnabled(true);
            this.gwd.setRecordListWaiting(false);
        }
    }
}
