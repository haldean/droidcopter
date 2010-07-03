/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.applications.gos.awt;

import javax.swing.*;
import java.awt.*;

/**
 * @author dcollins
 * @version $Id: ResourcePanel.java 13127 2010-02-16 04:02:26Z dcollins $
 */
public class ResourcePanel extends JPanel
{
    protected static final String RESOURCE_COMPONENT = "ResourceComponent";
    protected static final String WAITING_COMPONENT = "WaitingComponent";

    protected boolean waiting = false;
    protected JComponent resourceComponent;
    protected JComponent waitingComponent;
    protected JLabel waitingLabel;
    protected JProgressBar waitingProgressBar;

    public ResourcePanel(JComponent resourceComponent)
    {
        this.resourceComponent = resourceComponent;
        this.waitingComponent = this.createWaitingComponent();

        this.setLayout(new CardLayout(0, 0)); // hgap, vgap
        this.add(this.resourceComponent, RESOURCE_COMPONENT);
        this.add(this.waitingComponent, WAITING_COMPONENT);
    }

    public boolean isWaiting()
    {
        return this.waiting;
    }

    public void setWaiting(boolean waiting)
    {
        this.waiting = waiting;

        CardLayout cl = (CardLayout) this.getLayout();
        cl.show(this, waiting ? WAITING_COMPONENT : RESOURCE_COMPONENT);
    }

    public String getWaitingMessage()
    {
        return this.waitingLabel.getText();
    }

    public void setWaitingMessage(String text)
    {
        this.waitingLabel.setText(text);
    }

    public float getWaitingAlignmentX()
    {
        return this.waitingLabel.getAlignmentX();
    }

    public void setWaitingAlignmentX(float alignmentX)
    {
        this.waitingLabel.setAlignmentX(alignmentX);
        this.waitingProgressBar.setAlignmentX(alignmentX);
    }

    public JComponent getResourceComponent()
    {
        return this.resourceComponent;
    }

    public JComponent getWaitingComponent()
    {
        return this.waitingComponent;
    }

    protected JComponent createWaitingComponent()
    {
        this.waitingLabel = new JLabel();
        this.waitingLabel.setOpaque(false);
        this.waitingProgressBar = new JProgressBar();
        this.waitingProgressBar.setIndeterminate(true);

        Box vbox = Box.createVerticalBox();
        vbox.add(Box.createVerticalGlue());
        vbox.add(this.waitingLabel);
        vbox.add(Box.createVerticalStrut(10));
        vbox.add(this.waitingProgressBar);
        vbox.add(Box.createVerticalGlue());

        return vbox;
    }
}
