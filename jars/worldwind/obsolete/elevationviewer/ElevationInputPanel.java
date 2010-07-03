/* Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package WorldWindHackApps.elevationviewer;

import gov.nasa.worldwind.util.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * @author dcollins
 * @version $Id: ElevationInputPanel.java 13023 2010-01-21 00:18:48Z dcollins $
 */
public class ElevationInputPanel extends JPanel
{
    private ActionListener actionListener;
    private JButton openButton;
    private JTextField currentElevationsTextField;
    private JComboBox levelBox;

    public ElevationInputPanel()
    {
        this.initComponents();
    }

    public JButton getOpenButton()
    {
        return this.openButton;
    }

    public JTextField getCurrentElevationsTextField()
    {
        return this.currentElevationsTextField;
    }

    public JComboBox getLevelBox()
    {
        return this.levelBox;
    }

    public void populateLevelBox(LevelSet levelSet)
    {
        this.levelBox.removeAllItems();
        for (Level level : levelSet.getLevels())
        {
            this.levelBox.addItem(level);
        }
    }

    public ActionListener getActionListener()
    {
        return this.actionListener;
    }

    public void setActionListener(ActionListener actionListener)
    {
        if (this.actionListener != null)
        {
            this.openButton.removeActionListener(this.actionListener);
            this.levelBox.removeActionListener(this.actionListener);
        }
        
        this.actionListener = actionListener;

        if (this.actionListener != null)
        {
            this.openButton.addActionListener(this.actionListener);
            this.levelBox.addActionListener(this.actionListener);
        }
    }

    private void initComponents()
    {
        this.openButton = new JButton("Open...");
        this.openButton.setActionCommand(AppController.OPEN_ELEVATIONS);

        this.currentElevationsTextField = new JTextField("");
        this.currentElevationsTextField.setEnabled(false);

        this.levelBox = new JComboBox();
        this.levelBox.setActionCommand(AppController.SET_VISIBLE_LEVEL);

        if (actionListener != null)
        {
            this.openButton.addActionListener(this.actionListener);
            this.levelBox.addActionListener(this.actionListener);
        }

        this.setLayout(new BorderLayout(0, 10)); // hgap, vgap
        this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // top, left, bottom, right

        JPanel panel = new JPanel();
        {
            panel.setLayout(new BorderLayout(5, 0)); // hgap, vgap
            panel.add(this.openButton, BorderLayout.WEST);
            panel.add(this.currentElevationsTextField, BorderLayout.CENTER);
        }
        this.add(panel, BorderLayout.NORTH);

        panel = new JPanel();
        {
            panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
            this.levelBox.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(this.levelBox);
            panel.add(Box.createVerticalGlue());
        }
        this.add(panel, BorderLayout.CENTER);
    }
}
