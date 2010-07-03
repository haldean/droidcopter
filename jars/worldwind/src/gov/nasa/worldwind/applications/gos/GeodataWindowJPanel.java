/*
Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.applications.gos;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.applications.gos.awt.*;
import gov.nasa.worldwind.applications.gos.event.*;
import gov.nasa.worldwind.applications.gos.globe.*;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.layers.*;
import gov.nasa.worldwind.layers.placename.PlaceNameLayer;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;

/**
 * @author dcollins
 * @version $Id: GeodataWindowJPanel.java 13127 2010-02-16 04:02:26Z dcollins $
 */
public class GeodataWindowJPanel extends JPanel implements GeodataWindow, HyperlinkListener
{
    protected GeodataController controller;
    protected WorldWindow wwd;
    protected RecordList recordList;
    protected RecordListLayer recordListLayer = new RecordListLayer();
    // Swing components.
    protected SearchPanel searchPanel = new SearchPanel();
    protected SearchOptionsPanel searchOptionsPanel = new SearchOptionsPanel();
    protected ControlPanel controlPanel = new ControlPanel();
    protected ResourcePanel contentPanel = new ResourcePanel(new RecordListPanel());
    protected PageNavigationPanel pageNavigationPanel = new PageNavigationPanel();

    public GeodataWindowJPanel(Dimension dimension)
    {
        this.searchOptionsPanel.setVisible(false);
        this.searchPanel.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent event)
            {
                onSearchActionPerformed();
            }
        });
        this.controlPanel.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent event)
            {
                onControlPanelActionPerformed();
            }
        });
        this.pageNavigationPanel.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent event)
            {
                onPageNavigationActionPerformed();
            }
        });
        this.recordListLayer.addHyperlinkListener(this);
        this.getRecordListPanel().addHyperlinkListener(this);

        this.contentPanel.setBackground(Color.WHITE);
        this.contentPanel.setWaitingMessage("Searching geodata.gov...");
        this.contentPanel.setWaitingAlignmentX(Component.CENTER_ALIGNMENT);
        this.contentPanel.getWaitingComponent().setBorder(BorderFactory.createEmptyBorder(0, 100, 200, 100));
        
        this.setBackground(Color.WHITE);
        this.layoutComponents(dimension);

        GeodataController geodataController =
            (GeodataController) WorldWind.createConfigurationComponent(GeodataKey.GEODATA_CONTROLLER_CLASS_NAME);
        this.setGeodataController(geodataController);
        this.setRecordList(null, null);
    }

    public GeodataWindowJPanel()
    {
        this(null);
    }

    public void setEnabled(boolean enabled)
    {
        super.setEnabled(enabled);
        this.searchPanel.setEnabled(enabled);
        this.searchOptionsPanel.setEnabled(enabled);
        this.controlPanel.setEnabled(enabled);
        this.getRecordListPanel().setEnabled(enabled);
        this.pageNavigationPanel.setEnabled(enabled);
    }

    public boolean isRecordListWaiting()
    {
        return this.contentPanel.isWaiting();
    }

    public void setRecordListWaiting(boolean waiting)
    {
        this.contentPanel.setWaiting(waiting);
    }

    public RecordList getRecordList()
    {
        return this.recordList;
    }

    public void setRecordList(RecordList recordList, AVList searchParams)
    {
        this.recordList = recordList;
        this.recordListLayer.setRecordList(recordList);
        this.controlPanel.setRecordList(recordList, searchParams);
        this.getRecordListPanel().setRecordList(recordList);
        this.pageNavigationPanel.setRecordList(recordList, searchParams);
    }

    public GeodataController getGeodataController()
    {
        return this.controller;
    }

    public void setGeodataController(GeodataController geodataController)
    {
        if (this.controller != null)
            this.controller.setGeodataWindow(null);

        this.controller = geodataController;

        if (this.controller != null)
            this.controller.setGeodataWindow(this);
    }

    public WorldWindow getWorldWindow()
    {
        return this.wwd;
    }

    public void setWorldWindow(WorldWindow wwd)
    {
        if (this.wwd == wwd)
            return;

        if (this.wwd != null)
        {
            this.wwd.removeSelectListener(this.recordListLayer);
            this.wwd.getModel().getLayers().remove(this.recordListLayer);
        }

        this.wwd = wwd;
        this.searchOptionsPanel.setWorldWindow(wwd);
        this.getRecordListPanel().setGlobeModel(new WorldWindowGlobeModel(this.wwd));

        if (this.wwd != null)
        {
            this.wwd.addSelectListener(this.recordListLayer);
            this.insertBeforePlacenames(this.recordListLayer);
        }
    }

    public void addHyperlinkListener(HyperlinkListener listener)
    {
        this.listenerList.add(HyperlinkListener.class, listener);
    }

    public void removeHyperlinkListener(HyperlinkListener listener)
    {
        this.listenerList.remove(HyperlinkListener.class, listener);
    }

    public HyperlinkListener[] getHyperlinkListeners()
    {
        return this.listenerList.getListeners(HyperlinkListener.class);
    }

    public void addSearchListener(SearchListener listener)
    {
        this.listenerList.add(SearchListener.class, listener);
    }

    public void removeSearchListener(SearchListener listener)
    {
        this.listenerList.remove(SearchListener.class, listener);
    }

    public SearchListener[] getSearchListeners()
    {
        return this.listenerList.getListeners(SearchListener.class);
    }

    public void hyperlinkUpdate(HyperlinkEvent event)
    {
        // Notify my listeners of the HyperlinkEvent.
        this.fireHyperlinkUpdate(event);
    }

    protected void fireHyperlinkUpdate(HyperlinkEvent event)
    {
        for (HyperlinkListener listener : this.getHyperlinkListeners())
        {
            listener.hyperlinkUpdate(event);
        }
    }

    protected void fireSearchPerformed(SearchEvent event)
    {
        for (SearchListener listener : this.getSearchListeners())
        {
            listener.searchPerformed(event);
        }
    }

    protected void onControlPanelActionPerformed()
    {
        AVList params = new AVListImpl();
        this.controlPanel.getParams(params);

        String s = params.getStringValue(GeodataKey.SHOW_SEARCH_OPTIONS);
        this.searchOptionsPanel.setVisible(s != null && s.toLowerCase().startsWith("t"));

        s = params.getStringValue(GeodataKey.SHOW_RECORD_ANNOTATIONS);
        this.recordListLayer.setShowRecordAnnotations(s != null && s.toLowerCase().startsWith("t"));

        s = params.getStringValue(GeodataKey.SHOW_RECORD_BOUNDS);
        this.recordListLayer.setShowRecordBounds(s != null && s.toLowerCase().startsWith("t"));
    }

    protected void onPageNavigationActionPerformed()
    {
        AVList params = new AVListImpl();
        this.searchPanel.getParams(params);
        this.searchOptionsPanel.getParams(params);
        this.pageNavigationPanel.getParams(params);

        this.fireSearchPerformed(new SearchEvent(this, params));
    }

    protected void onSearchActionPerformed()
    {
        AVList params = new AVListImpl();
        params.setValue(GeodataKey.RECORD_START_INDEX, 0);
        this.searchPanel.getParams(params);
        this.searchOptionsPanel.getParams(params);

        this.fireSearchPerformed(new SearchEvent(this, params));
    }

    protected RecordListPanel getRecordListPanel()
    {
        return (RecordListPanel) this.contentPanel.getResourceComponent();
    }

    protected void layoutComponents(Dimension dimension)
    {
        if (dimension == null)
            dimension = new Dimension(700, 800);

        this.setLayout(new BorderLayout(0, 0)); // hgap, vgap
        this.setPreferredSize(dimension);

        JPanel northPanel = new JPanel(new BorderLayout(0, 0)); // hgap, vgap
        northPanel.add(this.searchPanel, BorderLayout.CENTER);
        northPanel.add(this.controlPanel, BorderLayout.SOUTH);
        this.add(northPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(0, 0)); // hgap, vgap
        centerPanel.add(this.searchOptionsPanel, BorderLayout.WEST);
        centerPanel.add(this.contentPanel, BorderLayout.CENTER);
        this.add(centerPanel, BorderLayout.CENTER);

        JPanel southPanel = new JPanel(new BorderLayout(0, 0)); // hgap, vgap
        southPanel.add(this.pageNavigationPanel, BorderLayout.CENTER);
        this.add(southPanel, BorderLayout.SOUTH);
    }

    protected void insertBeforePlacenames(Layer layer)
    {
        // Insert the layer into the layer list just before the placenames.
        int compassPosition = 0;
        LayerList layers = this.wwd.getModel().getLayers();
        for (Layer l : layers)
        {
            if (l instanceof PlaceNameLayer)
                compassPosition = layers.indexOf(l);
        }
        layers.add(compassPosition, layer);
    }
}
