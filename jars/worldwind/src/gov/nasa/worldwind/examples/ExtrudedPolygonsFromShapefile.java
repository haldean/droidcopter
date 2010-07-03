/*
Copyright (C) 2001, 2010 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/

package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.event.*;
import gov.nasa.worldwind.formats.shapefile.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.layers.CachedRenderableLayer;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import gov.nasa.worldwind.util.VecBuffer;

import javax.swing.*;
import java.awt.event.*;
import java.io.File;
import java.util.*;

/**
 * Shows how to make extruded shapes from shapefiles.
 *
 * @author tag
 * @version $Id: ExtrudedPolygonsFromShapefile.java 13233 2010-03-20 02:12:56Z tgaskins $
 */
public class ExtrudedPolygonsFromShapefile extends ApplicationTemplate
{
    public static class AppFrame extends ApplicationTemplate.AppFrame
    {
        public AppFrame()
        {
            super(true, true, false);

            this.makeMenu();
        }

        public class WorkerThread extends Thread
        {
            public WorkerThread(File file, final WorldWindow wwd)
            {
                Shapefile sf = new Shapefile(file);
                List<ShapefileRecord> records = sf.getRecords();

//                printShapefileInfo(sf, records);

                Sector boundingSector = null;
                for (ShapefileRecord r : records)
                {
                    if (r.getNumberOfPoints() < 4)
                        continue;

                    if (boundingSector == null)
                        boundingSector = Sector.boundingSector(r.getBuffer(0).getLocations());
                    else
                        boundingSector = boundingSector.union(Sector.boundingSector(r.getBuffer(0).getLocations()));
                }

                final CachedRenderableLayer layer = new CachedRenderableLayer(boundingSector, 3);
                for (ShapefileRecord r : records)
                {
                    if (r.getNumberOfPoints() < 4)
                        continue;

                    layer.add(this.makeShape(r));
                }

                SwingUtilities.invokeLater(new Runnable()
                {
                    public void run()
                    {
                        insertBeforePlacenames(wwd, layer);
                        AppFrame.this.getLayerPanel().update(wwd);
                    }
                });

                wwd.addSelectListener(new SelectListener()
                {
                    public void selected(SelectEvent event)
                    {
                        if (event.getTopObject() instanceof ExtrudedPolygon)
                            System.out.println("EXTRUDED POLYGON SELECTED");
                    }
                });
            }

            protected Sector makeBoundingSector(ShapefileRecord record)
            {
                return Sector.boundingSector(record.getBuffer(0).getLocations());
            }

            protected ExtrudedPolygon makeShape(ShapefileRecord record)
            {
                ExtrudedPolygon pgon = new ExtrudedPolygon();
                VecBuffer vb = record.getBuffer(0);
                pgon.setLocations(vb.getLocations());

                Object o = record.getAttributes().getValue("Height");
                if (o != null)
                {
                    double height = Double.parseDouble(o.toString());
                    pgon.setHeight(height);
                }

                return pgon;
            }
        }

        protected void makeMenu()
        {
            final JFileChooser fileChooser = new JFileChooser();
            fileChooser.addChoosableFileFilter(new Shapefiles.SHPFileFilter());

            JMenuBar menuBar = new JMenuBar();
            this.setJMenuBar(menuBar);
            JMenu fileMenu = new JMenu("File");
            menuBar.add(fileMenu);
            JMenuItem openMenuItem = new JMenuItem(new AbstractAction("Open File...")
            {
                public void actionPerformed(ActionEvent actionEvent)
                {
                    try
                    {
                        int status = fileChooser.showOpenDialog(AppFrame.this);
                        if (status == JFileChooser.APPROVE_OPTION)
                        {
                            Thread t = new WorkerThread(fileChooser.getSelectedFile(), getWwd());
                            t.start();
                        }
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            });

            fileMenu.add(openMenuItem);
        }
    }

    public static void main(String[] args)
    {
        ApplicationTemplate.start("Extruded Polygons from Shapefile", AppFrame.class);
    }

    public static void printShapefileInfo(Shapefile shapefile, List<ShapefileRecord> records)
    {
        if (records == null)
            records = shapefile.getRecords();

        for (ShapefileRecord r : records)
        {
            System.out.printf("%d, %s: %d parts, %d points", r.getRecordNumber(), r.getShapeType(),
                r.getNumberOfParts(), r.getNumberOfPoints());
            for (Map.Entry<String, Object> a : r.getAttributes().getEntries())
            {
                if (a.getKey() != null)
                    System.out.printf(", %s", a.getKey());
                if (a.getValue() != null)
                    System.out.printf(", %s", a.getValue());
            }
            System.out.println();

            System.out.print("\tAttributes: ");
            for (Map.Entry<String, Object> entry : r.getAttributes().getEntries())
            {
                System.out.printf("%s = %s, ", entry.getKey(), entry.getValue());
            }
            System.out.println();

            VecBuffer vb = r.getBuffer(0);
            for (LatLon ll : vb.getLocations())
            {
                System.out.printf("\t%f, %f\n", ll.getLatitude().degrees, ll.getLongitude().degrees);
            }
        }
    }
}
