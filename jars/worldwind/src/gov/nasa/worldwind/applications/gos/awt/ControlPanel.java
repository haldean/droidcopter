/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.applications.gos.awt;

import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.applications.gos.*;
import gov.nasa.worldwind.applications.gos.html.*;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.util.WWUtil;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;

/**
 * @author dcollins
 * @version $Id: ControlPanel.java 13128 2010-02-16 04:48:05Z dcollins $
 */
public class ControlPanel extends ActionPanel
{
    protected boolean showSearchOptions = false;
    protected JButton toggleSearchOptionsButton;
    protected JCheckBox showRecordAnnotationsBox;
    protected JCheckBox showRecordBoundsBox;
    protected JTextPane recordListInfoPane;

    public ControlPanel()
    {
        this.toggleSearchOptionsButton = new JButton(new ShowSearchOptionsAction());
        this.showRecordAnnotationsBox = new JCheckBox("Show Annotations", true);
        this.showRecordAnnotationsBox.setOpaque(false);
        this.showRecordBoundsBox = new JCheckBox("Show Bounds", true);
        this.showRecordBoundsBox.setOpaque(false);
        this.recordListInfoPane = new JTextPane();
        this.recordListInfoPane.setEditable(false);
        this.recordListInfoPane.setOpaque(false);
        ((DefaultCaret) this.recordListInfoPane.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);

        this.showRecordAnnotationsBox.addActionListener(this);
        this.showRecordBoundsBox.addActionListener(this);

        this.setBackground(new Color(240, 247, 249));
        this.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 1, 0, new Color(107, 114, 218)), // top, left, bottom, right
            BorderFactory.createEmptyBorder(5, 5, 5, 5))); // top, left, bottom, right
        this.layoutComponents();
    }

    public void getParams(AVList outParams)
    {
        outParams.setValue(GeodataKey.SHOW_SEARCH_OPTIONS,
            Boolean.toString(this.showSearchOptions));
        outParams.setValue(GeodataKey.SHOW_RECORD_ANNOTATIONS,
            Boolean.toString(this.showRecordAnnotationsBox.isSelected()));
        outParams.setValue(GeodataKey.SHOW_RECORD_BOUNDS,
            Boolean.toString(this.showRecordBoundsBox.isSelected()));
    }

    public void setRecordList(RecordList recordList, AVList searchParams)
    {
        StringBuilder sb = new StringBuilder();
        HTMLFormatter formatter = new BasicHTMLFormatter();
        formatter.beginHTMLBody(sb);
        sb.append("<table><tr><td align=\"right\">");
        this.addRecordList(sb, recordList, searchParams);
        sb.append("</td></tr></table>");
        formatter.endHTMLBody(sb);

        this.recordListInfoPane.setContentType("text/html");
        this.recordListInfoPane.setText(sb.toString());
    }

    protected void addRecordList(StringBuilder sb, RecordList recordList, AVList searchParams)
    {
        if (recordList == null || searchParams == null)
        {
            sb.append(" ");
            return;
        }

        int count = recordList.getRecordCount();
        int startIndex = AVListImpl.getIntegerValue(searchParams, GeodataKey.RECORD_START_INDEX) + 1;
        int pageSize = AVListImpl.getIntegerValue(searchParams, GeodataKey.RECORD_PAGE_SIZE);
        int endIndex = startIndex + pageSize - 1;
        if (endIndex > count)
            endIndex = count;
        Integer max = Configuration.getIntegerValue(GeodataKey.MAX_RECORDS);

        Iterable<? extends Record> records = recordList.getRecords();
        if (records == null)
            sb.append("No Results");

        if (records != null && count <= pageSize)
            sb.append("<b>").append(count).append("</b>").append(" results");

        if (records != null && count > pageSize && count <= max)
            sb.append("Results ").append("<b>").append(startIndex).append(" - ").append(endIndex).append("</b>")
                .append(" of ").append("<b>").append(count).append("</b>");

        if (records != null && count > pageSize && count > max)
            sb.append("Results ").append("<b>").append(startIndex).append(" - ").append(endIndex).append("</b>")
                .append(" of about ").append("<b>").append(max).append("</b>");

        if (searchParams != null)
        {
            String s = searchParams.getStringValue(GeodataKey.SEARCH_TEXT);
            if (!WWUtil.isEmpty(s))
                sb.append(" for ").append("<b>").append(s).append("</b>");
        }

        sb.append(".");
    }

    protected void layoutComponents()
    {
        Box buttonBox = Box.createHorizontalBox();
        buttonBox.add(Box.createHorizontalGlue());
        buttonBox.add(this.showRecordAnnotationsBox);
        buttonBox.add(Box.createHorizontalStrut(5));
        buttonBox.add(this.showRecordBoundsBox);
        buttonBox.add(Box.createHorizontalGlue());

        this.setLayout(new BorderLayout(0, 0));
        this.add(this.toggleSearchOptionsButton, BorderLayout.WEST);
        this.add(buttonBox, BorderLayout.CENTER);
        this.add(this.recordListInfoPane, BorderLayout.EAST);
    }

    protected class ShowSearchOptionsAction extends AbstractAction
    {
        public ShowSearchOptionsAction()
        {
            super("Show Options...");
        }

        public void actionPerformed(ActionEvent event)
        {
            showSearchOptions = true;
            ((AbstractButton) event.getSource()).setAction(new HideSearchOptionsAction());
            fireActionPerformed(event);
        }
    }

    protected class HideSearchOptionsAction extends AbstractAction
    {
        public HideSearchOptionsAction()
        {
            super("Hide Options");
        }

        public void actionPerformed(ActionEvent event)
        {
            showSearchOptions = false;
            ((AbstractButton) event.getSource()).setAction(new ShowSearchOptionsAction());
            fireActionPerformed(event);            
        }
    }
}
