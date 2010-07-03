/*
Copyright (C) 2001, 2010 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.applications.gos.awt;

import gov.nasa.worldwind.applications.gos.*;
import gov.nasa.worldwind.applications.gos.globe.GlobeModel;
import gov.nasa.worldwind.applications.gos.html.RecordHTMLFormatter;
import gov.nasa.worldwind.ogc.wms.WMSCapabilities;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;

/**
 * @author dcollins
 * @version $Id: RecordPanel.java 13362 2010-04-30 19:43:27Z dcollins $
 */
public class RecordPanel extends HyperlinkPanel
{
    protected Record record;
    protected GlobeModel globeModel;
    // Swing components.
    protected JTextPane descriptionPane;
    protected JTextPane iconPane;
    protected JButton goToButton;
    protected JButton toggleContentButton;
    protected ResourcePanel contentPanel;

    public RecordPanel(Record record, GlobeModel globeModel)
    {
        this.record = record;
        this.globeModel = globeModel;

        this.descriptionPane = new GOSTextPane();
        this.descriptionPane.setEditable(false);
        this.descriptionPane.addHyperlinkListener(this);
        ((DefaultCaret) this.descriptionPane.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);

        this.iconPane = new JTextPane();
        this.iconPane.setEditable(false);
        this.iconPane.addHyperlinkListener(this);
        ((DefaultCaret) this.iconPane.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);

        if (this.globeModel != null && this.record.getSector() != null)
        {
            this.goToButton = new JButton(new GoToAction());
            AWTUtil.scaleButton(this.goToButton, 0.75);
        }

        JComponent c = this.createResourceComponent();
        if (c != null)
        {
            this.toggleContentButton = new JButton(new ShowContentAction());
            this.contentPanel = new ResourcePanel(c);
            this.contentPanel.setVisible(false);
            this.contentPanel.setBackground(Color.WHITE);
            this.contentPanel.setWaitingMessage("Downloading content...");
            this.contentPanel.setWaitingAlignmentX(Component.LEFT_ALIGNMENT);
            this.contentPanel.getWaitingComponent().setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 200));
            AWTUtil.scaleButton(this.toggleContentButton, 0.75);
        }

        // Retrieve the record's service status via the GOS Application's threaded task service. Update the record's
        // text once the retrieval completes.
        OnlineResource r = record.getResource(GeodataKey.SERVICE_STATUS);
        if (r != null)
        {
            ResourceUtil.getOrRetrieveServiceStatus(r, new PropertyChangeListener()
            {
                public void propertyChange(PropertyChangeEvent evt)
                {
                    SwingUtilities.invokeLater(new Runnable()
                    {
                        public void run()
                        {
                            updateText();
                        }
                    });
                }
            });
        }

        this.setBackground(Color.WHITE);
        this.layoutComponents();
    }

    public Record getRecord()
    {
        return this.record;
    }

    public GlobeModel getGlobeModel()
    {
        return this.globeModel;
    }

    public boolean isContentVisible()
    {
        return this.contentPanel.isVisible();
    }

    public void setContentVisible(boolean visible)
    {
        this.contentPanel.setVisible(visible);

        if (!visible)
            return;

        this.beforeShowContent();
        ResourceUtil.getAppTaskService().execute(new Runnable()
        {
            public void run()
            {
                try
                {
                    doShowContent();
                }
                finally
                {
                    afterShowContent();
                }
            }
        });
    }

    protected void layoutComponents()
    {
        if (this.record == null)
            return;

        this.setLayout(new BorderLayout(0, 0)); // hgap, vgap
        this.updateText();

        Box contentBox = Box.createVerticalBox();
        this.descriptionPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentBox.add(this.descriptionPane, BorderLayout.CENTER);

        if (this.toggleContentButton != null || this.goToButton != null)
        {
            Box buttonBox = Box.createHorizontalBox();
            buttonBox.setAlignmentX(Component.LEFT_ALIGNMENT);
            contentBox.add(Box.createVerticalStrut(5));
            contentBox.add(buttonBox);

            if (this.toggleContentButton != null)
            {
                buttonBox.add(this.toggleContentButton);
                buttonBox.add(Box.createHorizontalStrut(5));
            }

            if (this.goToButton != null)
            {
                buttonBox.add(this.goToButton);
            }
        }

        if (this.contentPanel != null)
        {
            this.contentPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            this.contentPanel.setBorder(BorderFactory.createEmptyBorder(15, 30, 30, 30));
            contentBox.add(this.contentPanel);
        }

        Box mainBox = Box.createHorizontalBox();
        mainBox.add(this.iconPane);
        mainBox.add(contentBox);

        this.add(mainBox, BorderLayout.NORTH);
        this.validate();
    }

    protected void updateText()
    {
        StringBuilder sb = new StringBuilder();
        RecordHTMLFormatter formatter = new RecordHTMLFormatter();

        sb.delete(0, sb.capacity());
        formatter.beginHTMLBody(sb);
        formatter.addRecordTitle(sb, this.record);
        formatter.addRecordDescription(sb, this.record);
        formatter.addRecordLinks(sb, this.record);
        formatter.endHTMLBody(sb);
        this.descriptionPane.setContentType("text/html");
        this.descriptionPane.setText(sb.toString());

        sb.delete(0, sb.capacity());
        formatter.beginHTMLBody(sb);
        sb.append("<table><tr><td align=\"center\" valign=\"top\">");
        formatter.addRecordIcons(sb, this.record);
        sb.append("</td></tr></table>");
        formatter.endHTMLBody(sb);
        this.iconPane.setContentType("text/html");
        this.iconPane.setText(sb.toString());
    }

    protected JComponent createResourceComponent()
    {
        OnlineResource capsResource = this.record.getResource(GeodataKey.CAPABILITIES);
        if (capsResource == null)
            return null;

        return new WMSLayerPanel(this.record, this.globeModel);
    }

    protected void doShowContent()
    {
        OnlineResource capsResource = this.record.getResource(GeodataKey.CAPABILITIES);
        if (capsResource == null)
            return;

        WMSLayerPanel layerPanel = (WMSLayerPanel) this.contentPanel.getResourceComponent();

        WMSCapabilities caps = ResourceUtil.getCapabilities(capsResource);
        if (layerPanel.getCapabilities() != caps)
            layerPanel.setCapabilities(caps);
    }

    protected void beforeShowContent()
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    beforeShowContent();
                }
            });
        }
        else
        {
            this.toggleContentButton.setEnabled(false);
            this.contentPanel.setWaiting(true);
        }
    }

    protected void afterShowContent()
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    afterShowContent();
                }
            });
        }
        else
        {
            this.toggleContentButton.setEnabled(true);
            this.contentPanel.setWaiting(false);
        }
    }

    protected class GoToAction extends AbstractAction
    {
        public GoToAction()
        {
            super("Go There");
        }

        public void actionPerformed(ActionEvent event)
        {
            if (record == null || record.getSector() == null)
                return;

            globeModel.moveViewTo(record.getSector());
        }
    }

    protected class ShowContentAction extends AbstractAction
    {
        public ShowContentAction()
        {
            super("Show Content");
        }

        public void actionPerformed(ActionEvent event)
        {
            setContentVisible(true);
            ((AbstractButton) event.getSource()).setAction(new HideContentAction());
        }
    }

    protected class HideContentAction extends AbstractAction
    {
        public HideContentAction()
        {
            super("Hide Content");
        }

        public void actionPerformed(ActionEvent event)
        {
            setContentVisible(false);
            ((AbstractButton) event.getSource()).setAction(new ShowContentAction());
        }
    }
}
