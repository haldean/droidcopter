/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.event.*;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.pick.PickedObjectList;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.util.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.*;
import java.util.List;

/**
 * @author Patrick Murris
 * @version $Id: Shapefiles.java 13125 2010-02-16 01:25:07Z tgaskins $
 */
public class Shapefiles extends ApplicationTemplate
{
    public static class AppFrame extends ApplicationTemplate.AppFrame
    {
        JFileChooser fc = new JFileChooser(Configuration.getUserHomeDirectory());

        protected List<Layer> layers = new ArrayList<Layer>();
        protected SurfaceShape lastHighlitShape;
        protected ShapeAttributes lastShapeAttributes;
        protected BasicDragger dragger;
        private JCheckBox pickCheck, dragCheck;

        public AppFrame()
        {
            this.dragger = new BasicDragger(getWwd());

            // Add our control panel
            this.getLayerPanel().add(makeControlPanel(), BorderLayout.SOUTH);

            // Setup file chooser
            this.fc = new JFileChooser(Configuration.getUserHomeDirectory());
            this.fc.addChoosableFileFilter(new SHPFileFilter());

            // Add select listener for shapes dragging
            this.setupSelectListener();
        }

        protected JPanel makeControlPanel()
        {
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
            panel.setBorder(new CompoundBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4),
                new TitledBorder("Shapefiles")));

            // Open shapefile button
            JPanel buttonPanel = new JPanel(new GridLayout(1, 1, 0, 0)); // nrows, ncols, hgap, vgap
            buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // top, left, bottom, right
            JButton button = new JButton("Open Shapefile");
            button.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent actionEvent)
                {
                    showOpenDialog();
                }
            });
            buttonPanel.add(button);
            panel.add(buttonPanel);

            // Picking and dragging checkboxes
            JPanel pickPanel = new JPanel(new GridLayout(1, 1, 10, 10)); // nrows, ncols, hgap, vgap
            pickPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // top, left, bottom, right
            this.pickCheck = new JCheckBox("Allow picking");
            this.pickCheck.setSelected(true);
            this.pickCheck.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent actionEvent)
                {
                    enablePicking(((JCheckBox)actionEvent.getSource()).isSelected());
                }
            });
            pickPanel.add(this.pickCheck);

            this.dragCheck = new JCheckBox("Allow dragging");
            this.dragCheck.setSelected(false);
            this.dragCheck.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent actionEvent)
                {
                }
            });
            pickPanel.add(this.dragCheck);

            panel.add(pickPanel);

            return panel;
        }

        protected void setupSelectListener()
        {
            this.getWwd().addSelectListener(new SelectListener()
            {
                public void selected(SelectEvent event)
                {
                    if (lastHighlitShape != null
                        && (event.getTopObject() == null || !event.getTopObject().equals(lastHighlitShape)))
                    {
                        lastHighlitShape.setAttributes(lastShapeAttributes);
                        lastHighlitShape = null;
                    }

                    // Have rollover events highlight the rolled-over object.
                    if (event.getEventAction().equals(SelectEvent.ROLLOVER) && !dragger.isDragging())
                    {
                        AppFrame.this.highlight(event.getTopObject());
                    }

                    // Have drag events drag the selected object.
                    else if (dragCheck.isSelected() && (event.getEventAction().equals(SelectEvent.DRAG_END)
                        || event.getEventAction().equals(SelectEvent.DRAG)))
                    {
                        // Delegate dragging computations to a dragger.
                        dragger.selected(event);

                        // We missed any roll-over events while dragging, so highlight any under the cursor now,
                        // or de-highlight the dragged shape if it's no longer under the cursor.
                        if (event.getEventAction().equals(SelectEvent.DRAG_END))
                        {
                            PickedObjectList pol = getWwd().getObjectsAtCurrentPosition();
                            if (pol != null)
                            {
                                AppFrame.this.highlight(pol.getTopObject());
                                AppFrame.this.getWwd().repaint();
                            }
                        }
                    }
                }
            });
        }

        protected void highlight(Object o)
        {
            // Same shape selected.
            if (o == this.lastHighlitShape)
                return;

            if (this.lastHighlitShape == null && o instanceof AbstractSurfaceShape)
            {
                this.lastHighlitShape = (AbstractSurfaceShape) o;
                this.lastShapeAttributes = this.lastHighlitShape.getAttributes();
                ShapeAttributes selectedAttributes = this.getHighlightAttributes(this.lastShapeAttributes);
                this.lastHighlitShape.setAttributes(selectedAttributes);
            }
        }

        protected ShapeAttributes getHighlightAttributes(ShapeAttributes attributes)
        {
            ShapeAttributes selectedAttributes = new BasicShapeAttributes(attributes);

            if (selectedAttributes.isDrawInterior())
            {
                selectedAttributes.setInteriorMaterial(Material.WHITE);
                selectedAttributes.setInteriorImageSource(null);
            }
            else if (selectedAttributes.isDrawOutline())
            {
                selectedAttributes.setOutlineMaterial(Material.WHITE);
            }

            return selectedAttributes;
        }

        protected void enablePicking(boolean enabled)
        {
            this.dragCheck.setEnabled(enabled);
            for (Layer layer : this.layers)
                layer.setPickEnabled(enabled);
        }

        public void showOpenDialog()
        {
            int retVal = this.fc.showOpenDialog(this);
            if (retVal != JFileChooser.APPROVE_OPTION)
                return;

            File file = this.fc.getSelectedFile();
            this.addShapefileLayer(file);
        }

        public void addShapefileLayer(File file)
        {
            Layer layer = ShapefileLoader.makeShapefileLayer(file);
            if (layer != null)
            {
                layer.setPickEnabled(this.pickCheck.isSelected());
                layer.setName(file.getName());
                insertBeforePlacenames(this.getWwd(), layer);
                this.getLayerPanel().update(this.getWwd());
                this.layers.add(layer);
            }
        }
    }


    public static class SHPFileFilter extends FileFilter
    {
        public boolean accept(File file)
        {
            if (file == null)
            {
                String message = Logging.getMessage("nullValue.FileIsNull");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }

            return file.isDirectory() || file.getName().toLowerCase().endsWith(".shp");
        }

        public String getDescription()
        {
            return "ESRI Shapefiles (shp)";
        }
    }

    public static void main(String[] args)
    {
        start("World Wind Shapefiles", AppFrame.class);
    }
}
