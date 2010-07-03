/*
Copyright (C) 2001, 2008 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.globes.ElevationModel;
import gov.nasa.worldwind.terrain.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Hashtable;

/**
 * Using the elevation model detail hint.
 *
 * @author Patrick Murris
 * @version $Id: TerrainLOD.java 12430 2009-08-10 12:25:32Z patrickmurris $
 */
public class TerrainLOD extends ApplicationTemplate
{
    public static class AppFrame extends ApplicationTemplate.AppFrame
    {
        public AppFrame()
        {
            // Add detail hint slider panel
            this.getLayerPanel().add(makeDetailHintControlPanel(), BorderLayout.SOUTH);
        }

        private JPanel makeDetailHintControlPanel()
        {
            JPanel controlPanel = new JPanel(new BorderLayout(0, 10));
            controlPanel.setBorder(new CompoundBorder(BorderFactory.createEmptyBorder(9, 9, 9, 9),
                new TitledBorder("Detail Hint")));

            JPanel sliderPanel = new JPanel(new BorderLayout(0, 5));
            {
                int MIN = -10;
                int MAX = 10;
                int cur = (int) (this.getWwd().getModel().getGlobe().getElevationModel().getDetailHint(Sector.FULL_SPHERE) * 10);
                cur = cur < MIN ? MIN : (cur > MAX ? MAX : cur);
                JSlider slider = new JSlider(MIN, MAX, cur);
                slider.setMajorTickSpacing(10);
                slider.setMinorTickSpacing(1);
                slider.setPaintTicks(true);
                Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
                labelTable.put(-10, new JLabel("-1.0"));
                labelTable.put(0, new JLabel("0.0"));
                labelTable.put(10, new JLabel("1.0"));
                slider.setLabelTable(labelTable);
                slider.setPaintLabels(true);
                slider.addChangeListener(new ChangeListener()
                {
                    public void stateChanged(ChangeEvent e)
                    {
                        double hint = ((JSlider) e.getSource()).getValue() / 10d;
                        setDetailHint(hint);
                        getWwd().redraw();
                    }
                });
                sliderPanel.add(slider, BorderLayout.SOUTH);
            }


            JPanel checkBoxPanel = new JPanel(new BorderLayout(0, 5));
            {

                JCheckBox checkBox = new JCheckBox("Show wireframe");
                checkBox.addActionListener(new ActionListener()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        boolean selected = ((JCheckBox)e.getSource()).isSelected();
                        getWwd().getSceneController().getModel().setShowWireframeInterior(selected);
                        getWwd().redraw();
                    }
                });
                checkBoxPanel.add(checkBox, BorderLayout.SOUTH);
            }

            controlPanel.add(checkBoxPanel, BorderLayout.NORTH);
            controlPanel.add(sliderPanel, BorderLayout.SOUTH);
            return controlPanel;
        }

        private void setDetailHint(double hint)
        {
            ElevationModel em = getWwd().getModel().getGlobe().getElevationModel();
            if (em instanceof BasicElevationModel)
            {
                ((BasicElevationModel)em).setDetailHint(hint);
                System.out.println("Detail hint set to " + hint);
                getWwd().redraw();
            }
            else if (em instanceof CompoundElevationModel)
            {
                for (ElevationModel m : ((CompoundElevationModel)em).getElevationModels())
                {
                    if (m instanceof BasicElevationModel)
                    {
                        ((BasicElevationModel)m).setDetailHint(hint);
                        System.out.println("Detail hint set to " + hint);
                    }
                }
            }
        }


    }

    public static void main(String[] args)
    {
        ApplicationTemplate.start("World Wind Terrain Level Of Detail", AppFrame.class);
    }
}
