/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.applications.gos.awt;

import gov.nasa.worldwind.applications.gos.*;
import gov.nasa.worldwind.applications.gos.globe.*;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

/**
 * @author dcollins
 * @version $Id: RecordListPanel.java 13127 2010-02-16 04:02:26Z dcollins $
 */
public class RecordListPanel extends HyperlinkPanel
{
    protected RecordList recordList;
    protected GlobeModel globeModel;
    // Swing components.
    protected JPanel contentPanel;
    protected JScrollPane scrollPane;
    protected ArrayList<RecordPanel> recordPanelList = new ArrayList<RecordPanel>();

    public RecordListPanel()
    {
        this.contentPanel = new JPanel();
        this.contentPanel.setLayout(new BoxLayout(this.contentPanel, BoxLayout.Y_AXIS));
        this.contentPanel.setBackground(Color.WHITE);
        this.contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // top, left, bottom, right

        JPanel dummyPanel = new JPanel(new BorderLayout(0, 0));
        dummyPanel.setBackground(Color.WHITE);
        dummyPanel.add(this.contentPanel, BorderLayout.NORTH);

        this.scrollPane = new JScrollPane(dummyPanel,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        this.scrollPane.setAutoscrolls(false);
        this.scrollPane.setBorder(BorderFactory.createEmptyBorder());

        this.setBackground(Color.WHITE);
        this.setLayout(new BorderLayout(0, 0)); // hgap, vgap
        this.add(this.scrollPane, BorderLayout.CENTER);
    }

    public RecordList getRecordList()
    {
        return this.recordList;
    }

    public void setRecordList(RecordList recordList)
    {
        this.recordList = recordList;
        this.updateRecordPanels();
    }

    public GlobeModel getGlobeModel()
    {
        return this.globeModel;
    }

    public void setGlobeModel(GlobeModel globeModel)
    {
        this.globeModel = globeModel;
    }

    public void invalidate()
    {
        super.invalidate();
        AWTUtil.invalidateTree(this);
    }

    protected void updateRecordPanels()
    {
        for (RecordPanel rp : this.recordPanelList)
        {
            rp.removeHyperlinkListener(this);
        }

        this.contentPanel.removeAll();
        this.recordPanelList.clear();

        if (this.recordList == null)
            return;

        Iterable<? extends Record> records = this.recordList.getRecords();
        if (records == null)
            return;

        for (Record r : records)
        {
            if (r == null)
                continue;

            RecordPanel rp = new RecordPanel(r, this.getGlobeModel());
            rp.addHyperlinkListener(this);
            rp.setAlignmentX(Component.LEFT_ALIGNMENT);
            this.contentPanel.add(rp);
            this.contentPanel.add(Box.createVerticalStrut(10));
            this.recordPanelList.add(rp);
        }

        this.validate();
    }
}
