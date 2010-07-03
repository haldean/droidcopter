/*
Copyright (C) 2001, 2007 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.view.orbit.OrbitView;
import gov.nasa.worldwind.WorldWindow;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Controlling the view to 'look around'.
 *
 * @author Patrick Murris
 * @version $Id: ViewLookAround.java 12530 2009-08-29 17:55:54Z jterhorst $
 */
public class ViewLookAround extends ApplicationTemplate
{
    public static class AppFrame extends ApplicationTemplate.AppFrame
    {
        private ViewControlPanel vcp;

        public AppFrame()
        {
            super(true, true, false);

            // Add view control panel to the layer panel
            this.vcp = new ViewControlPanel(getWwd());
            this.getLayerPanel().add(this.vcp,  BorderLayout.SOUTH);
        }

        private class ViewControlPanel extends JPanel
        {
            private WorldWindow wwd;
            private JSlider pitchSlider;
            private JSlider headingSlider;
            private JSlider fovSlider;

            private boolean suspendEvents = false;

            public ViewControlPanel(WorldWindow wwd)
            {
                this.wwd = wwd;
                // Add view property listener
                this.wwd.getView().addPropertyChangeListener(new PropertyChangeListener()
                {
                    public void propertyChange(PropertyChangeEvent propertyChangeEvent)
                    {
                        update();
                    }
                });

                // -- Compose panel ---
                this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

                // -- Pitch slider ------------------------------------
                JPanel pitchPanel = new JPanel(new GridLayout(0, 1, 5, 5));
                pitchPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
                pitchPanel.add(new JLabel("Pitch:"));
                pitchSlider = new JSlider(0, 90, 0);
                pitchSlider.addChangeListener(new ChangeListener()
                {
                    public void stateChanged(ChangeEvent changeEvent)
                    {
                        updateView();
                    }
                });
                pitchPanel.add(pitchSlider);

                // -- Heading slider ------------------------------------
                JPanel headingPanel = new JPanel(new GridLayout(0, 1, 5, 5));
                headingPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
                headingPanel.add(new JLabel("Heading:"));
                headingSlider = new JSlider(-180, 180, 0);
                headingSlider.addChangeListener(new ChangeListener()
                {
                    public void stateChanged(ChangeEvent changeEvent)
                    {
                        updateView();
                    }
                });
                headingPanel.add(headingSlider);

                // -- Field of view slider ------------------------------
                JPanel fovPanel = new JPanel(new GridLayout(0, 1, 5, 5));
                fovPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
                fovPanel.add(new JLabel("Field of view:"));
                fovSlider = new JSlider(10, 120, 45);
                fovSlider.addChangeListener(new ChangeListener()
                {
                    public void stateChanged(ChangeEvent changeEvent)
                    {
                        updateView();
                    }
                });
                fovPanel.add(fovSlider);

                // -- Assembly -----------------------------------------
                this.add(pitchPanel);
                this.add(headingPanel);
                this.add(fovPanel);
                this.setBorder(
                        new CompoundBorder(BorderFactory.createEmptyBorder(9, 9, 9, 9), new TitledBorder("View")));
                this.setToolTipText("View controls");
            }

            // Update view settings from control panel in a 'first person' perspective
            private void updateView()
            {
                if (!suspendEvents)
                {
                    OrbitView view = (OrbitView) this.wwd.getView();

                    // Stop iterators first
                    view.stopAnimations();

                    // Save current eye position
                    final Position pos = view.getEyePosition();

                    // Set view heading, pitch and fov
                    view.setHeading(Angle.fromDegrees(this.headingSlider.getValue()));
                    view.setPitch(Angle.fromDegrees(this.pitchSlider.getValue()));
                    view.setFieldOfView(Angle.fromDegrees(this.fovSlider.getValue()));
                    view.setZoom(0);

                    // Restore eye position
                    view.setCenterPosition(pos);

                    // Redraw
                    this.wwd.redraw();
                }
            }

            // Update control panel from view
            public void update()
            {
                this.suspendEvents = true;
                {
                    OrbitView view = (OrbitView) wwd.getView();
                    this.pitchSlider.setValue((int)view.getPitch().degrees);
                    this.headingSlider.setValue((int)view.getHeading().degrees);
                    this.fovSlider.setValue((int)view.getFieldOfView().degrees);
                }
                this.suspendEvents = false;
            }

        }
    }

    public static void main(String[] args)
    {
        ApplicationTemplate.start("World Wind View Look Around", AppFrame.class);
    }
}