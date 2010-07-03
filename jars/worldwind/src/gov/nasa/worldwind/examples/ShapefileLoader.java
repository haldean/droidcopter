/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.examples;

import com.sun.opengl.util.BufferUtil;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.formats.shapefile.*;
import gov.nasa.worldwind.formats.worldfile.WorldFile;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.layers.*;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.util.*;

import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.net.URL;
import java.nio.IntBuffer;
import java.util.*;

/**
 * @author Patrick Murris
 * @version $Id$
 */
public class ShapefileLoader
{
    private static final Color[] colors = new Color[] {
        Color.YELLOW, Color.BLUE, Color.RED, Color.GREEN, Color.CYAN, Color.ORANGE, Color.MAGENTA};
    static int colorIndex = 0;

    private static class TextAndShapesLayer extends SurfaceShapeLayer
    {
        private ArrayList<GeographicText> labels = new ArrayList<GeographicText>();
        private GeographicTextRenderer textRenderer = new GeographicTextRenderer();

        public TextAndShapesLayer()
        {
            this.textRenderer.setCullTextEnabled(true);
            this.textRenderer.setCullTextMargin(2);
            this.textRenderer.setDistanceMaxScale(2);
            this.textRenderer.setDistanceMinScale(.5);
            this.textRenderer.setDistanceMinOpacity(.5);
            this.textRenderer.setEffect(AVKey.TEXT_EFFECT_OUTLINE);
        }

        public void addLabel(GeographicText label)
        {
            this.labels.add(label);
        }

        public void doRender(DrawContext dc)
        {
            super.doRender(dc);
            this.setActiveLabels(dc);
            this.textRenderer.render(dc, this.labels);
        }

        protected void setActiveLabels(DrawContext dc)
        {
            for (GeographicText label : this.labels)
            {
                if (label instanceof Label)
                    if (((Label) label).isActive(dc))
                        label.setVisible(true);
                    else
                        label.setVisible(false);
            }
        }
    }

    private static class Label extends UserFacingText
    {
        private double minActiveAltitude = -Double.MAX_VALUE;
        private double maxActiveAltitude = Double.MAX_VALUE;

        public Label(String text, Position position)
        {
            super(text, position);
        }

        public void setMinActiveAltitude(double altitude)
        {
            this.minActiveAltitude = altitude;
        }

        public void setMaxActiveAltitude(double altitude)
        {
            this.maxActiveAltitude = altitude;
        }

        public boolean isActive(DrawContext dc)
        {
            double eyeElevation = dc.getView().getEyePosition().getElevation();
            return this.minActiveAltitude <= eyeElevation && eyeElevation <= this.maxActiveAltitude;
        }
    }

    public static Layer makeShapefileLayer(URL url)
    {
        try
        {
            Shapefile shp = new Shapefile(url.openStream(), null, null); // TODO: handle .idx and .dbf
            return makeShapefileLayer(shp);
        }
        catch (Exception e)
        {
            return null;
        }
    }

    public static Layer makeShapefileLayer(File file)
    {
        AVList projectionParams = null;
        try
        {
            File[] worldFiles = WorldFile.getWorldFiles(file);
            if (worldFiles != null && worldFiles.length > 0)
                projectionParams = WorldFile.decodeWorldFiles(worldFiles, null);
        }
        catch (IOException e)
        {
            Logging.logger().severe(Logging.getMessage("WorldFile.ExceptionReading", file));
        }

        Shapefile shp = new Shapefile(file, projectionParams);
        if (file.getName().equals("places.shp"))
        {
            return makeOSMPlacesLayer(shp);  // Test record selection on specific shapefile
        }
        return makeShapefileLayer(shp);
    }

    public static Layer makeShapefileLayer(Shapefile shp)
    {
        String shapeType = shp.getShapeType();
        if (Shapefile.isPointType(shapeType) || Shapefile.isMultiPointType(shapeType))
        {
            return makePointLayer(shp);
        }
        else if (Shapefile.isPolylineType(shapeType))
        {
            return makePolylineLayer2(shp);
        }
        else if (Shapefile.isPolygonType(shapeType))
        {
            return makePolylineLayer2(shp);
        }
        return null;
    }

    // Generic point layer
    protected static Layer makePointLayer(Shapefile shp)
    {
        TextAndShapesLayer layer = new TextAndShapesLayer();
        Color color = colors[colorIndex++ % colors.length];
        shp.getRecords(); // load records at least once

        int totPoints = addPoints(layer, shp.getBuffer(), color, 1, 0);
        System.out.println("Tot points: " + totPoints);

        return layer;
    }

    // Specific point layer for OSM places.
    protected static Layer makeOSMPlacesLayer(Shapefile shp)
    {
        TextAndShapesLayer layer = new TextAndShapesLayer();
        java.util.List<ShapefileRecord> records = shp.getRecords();

        // Filter records for a particular sector
        records = getRecordsSubset(records, Sector.fromDegrees(43, 45, 5, 8));

        // Add points with different rendering attribute for different subsets
        int totPoints = 0;
        totPoints += addPoints(layer, getRecordsSubset(records, "type", "hamlet"), Color.BLACK, .3, 30e3);
        totPoints += addPoints(layer, getRecordsSubset(records, "type", "village"), Color.GREEN, .5, 100e3);
        totPoints += addPoints(layer, getRecordsSubset(records, "type", "town"), Color.CYAN, 1, 500e3);
        totPoints += addPoints(layer, getRecordsSubset(records, "type", "city"), Color.YELLOW, 2, 3000e3);

        System.out.println("Tot points: " + totPoints);

        return layer;
    }

    protected static java.util.List<ShapefileRecord> getRecordsSubset(java.util.List<ShapefileRecord> records,
        String attributeName, Object value)
    {
        ArrayList<ShapefileRecord> recordList = new ArrayList<ShapefileRecord>(records);
        return ShapefileUtils.selectRecords(recordList, attributeName, value, false);
    }

    protected static java.util.List<ShapefileRecord> getRecordsSubset(java.util.List<ShapefileRecord> records,
        Sector sector)
    {
        ArrayList<ShapefileRecord> recordList = new ArrayList<ShapefileRecord>(records);
        return ShapefileUtils.selectRecords(recordList, sector);
    }

    protected static int addPoints(TextAndShapesLayer layer, java.util.List<ShapefileRecord> records, Color color,
        double scale, double labelMaxAltitude)
    {
        if (records == null)
            return 0;

        Font font = new Font("Arial", Font.BOLD, 10 + (int) (3 * scale));
        Color background = WWUtil.computeContrastingColor(color);

        // Gather point locations
        ArrayList<LatLon> locations = new ArrayList<LatLon>();
        for (ShapefileRecord rec : records)
        {
            if (rec == null || !rec.getShapeType().equals(Shapefile.SHAPE_POINT))
                continue;

            ShapefileRecordPoint point = (ShapefileRecordPoint) rec;
            // Note: points are stored in the buffer as a sequence of X and Y with X = longitude, Y = latitude.
            double[] pointCoords = point.getPoint();
            LatLon location = LatLon.fromDegrees(pointCoords[1], pointCoords[0]);
            locations.add(location);

            // Add label
            if (labelMaxAltitude > 0)
            {
                Label label = getRecordLabel(rec);
                if (label != null)
                {
                    label.setFont(font);
                    label.setColor(color);
                    label.setBackgroundColor(background);
                    label.setMaxActiveAltitude(labelMaxAltitude);
                    label.setPriority(labelMaxAltitude);
                    layer.addLabel(label);
                }
            }
        }

        // Use one SurfaceIcons instance for all points
        BufferedImage image = PatternFactory.createPattern(PatternFactory.PATTERN_CIRCLE, .8f, color);
        SurfaceIcons sis = new SurfaceIcons(image, locations);
        sis.setMaxSize(4e3 * scale); // 4km
        sis.setMinSize(100);  // 100m
        sis.setScale(scale);
        sis.setOpacity(.8);
        layer.addRenderable(sis);

        return records.size();
    }

    @SuppressWarnings({"UnusedDeclaration"})
    protected static int addPoints(TextAndShapesLayer layer, CompoundVecBuffer buffer, Color color,
        double scale, double labelMaxAltitude)
    {
        // Use one SurfaceIcons instance for all points
        BufferedImage image = PatternFactory.createPattern(PatternFactory.PATTERN_CIRCLE, .8f, color);
        SurfaceIcons sis = new SurfaceIcons(image, buffer.getLocations());
        sis.setMaxSize(4e3 * scale); // 4km
        sis.setMinSize(100);  // 100m
        sis.setScale(scale);
        sis.setOpacity(.8);
        layer.addRenderable(sis);

        return buffer.getTotalBufferSize();
    }

    // Handles polylines and polygons with a CompoundVecBuffer
    protected static Layer makePolylineLayer(Shapefile shp)
    {
        TextAndShapesLayer layer = new TextAndShapesLayer();

        // Create surface shape
        java.util.List<ShapefileRecord> records = shp.getRecords();
        Sector sector = Sector.fromDegrees(shp.getBoundingRectangle());
        CompoundVecBuffer buffer = shp.getBuffer();
        boolean filled = shp.getShapeType().equals(Shapefile.SHAPE_POLYGON);
        SurfaceShape sp;
        if (filled)
        {
            sp = new SurfacePolygons(sector, buffer);
            // Set polygon group for each record
            int[] polygonGroups = new int[records.size()];
            for (int i = 0; i < records.size(); i++)
            {
                polygonGroups[i] = records.get(i).getFirstPartNumber();
            }

            ((SurfacePolygons) sp).setPolygonRingGroups(polygonGroups);
        }
        else
        {
            sp = new SurfacePolylines(sector, buffer);
        }

        // Set rendering attributes
        setShapeAttributes(sp);

        layer.addRenderable(sp);
        return layer;
    }

    // Handles polygons with a CompoundVecBuffer one record per shape
    protected static Layer makePolylineLayer2(Shapefile shp)
    {
        TextAndShapesLayer layer = new TextAndShapesLayer();

        // Create surface shape
        java.util.List<ShapefileRecord> records = shp.getRecords();
        Sector sector = Sector.fromDegrees(shp.getBoundingRectangle());
        CompoundVecBuffer buffer = shp.getBuffer();
        boolean filled = shp.getShapeType().equals(Shapefile.SHAPE_POLYGON);

        if (filled)
        {
            for (ShapefileRecord record : records)
            {
                sector = Sector.fromDegrees(((ShapefileRecordPolygon) record).getBoundingRectangle());
                // Use a subset from the shapefile buffer
                int beginIndex = record.getFirstPartNumber();
                int endIndex = record.getFirstPartNumber() + record.getNumberOfParts() - 1;
                CompoundVecBuffer subsetBuffer = (CompoundVecBuffer) shp.getBuffer().subCollection(beginIndex,
                    endIndex);
                SurfacePolygons sp = new SurfacePolygons(sector, subsetBuffer);
                // Set one polygon group starting at first sub-buffer
                sp.setPolygonRingGroups(new int[] {0});
                // Set rendering attributes
                setShapeAttributes(sp);
                layer.addRenderable(sp);
            }
        }
        else
        {
            SurfacePolylines sp = new SurfacePolylines(sector, buffer);
            // Set rendering attributes
            setShapeAttributes(sp);
            layer.addRenderable(sp);
        }

        return layer;
    }

    protected static CompoundVecBuffer makeBuffer(java.util.List<? extends LatLon> locations)
    {
        int numPoints = locations.size();
        int numParts = 1;
        // Create buffers
        VecBuffer pointBuffer = new VecBuffer(2, numPoints, new BufferFactory.DoubleBufferFactory());
        IntBuffer offsetBuffer = BufferUtil.newIntBuffer(numParts);
        IntBuffer lengthBuffer = BufferUtil.newIntBuffer(numParts);
        // Feed buffers
        pointBuffer.putLocations(0, locations);
        offsetBuffer.put(0);
        lengthBuffer.put(numPoints);
        offsetBuffer.rewind();
        lengthBuffer.rewind();
        // Assemble compound buffer
        return new CompoundVecBuffer(pointBuffer, offsetBuffer, lengthBuffer, numParts,
            new BufferFactory.DoubleBufferFactory());
    }

    protected static void setShapeAttributes(SurfaceShape shape)
    {
        Color color = colors[colorIndex++ % colors.length];
        ShapeAttributes attrs = new BasicShapeAttributes();
        attrs.setDrawOutline(true);
        attrs.setDrawInterior(shape instanceof SurfacePolygon || shape instanceof SurfacePolygons);
        attrs.setOutlineMaterial(new Material(color));
        attrs.setInteriorMaterial(new Material(color.brighter()));
        attrs.setOutlineOpacity(1);
        attrs.setOutlineWidth(1.2);
        attrs.setInteriorOpacity(.5);
        shape.setAttributes(attrs);
    }

    protected static Label getRecordLabel(ShapefileRecord record)
    {
        String text = getRecordLabelText(record);
        if (text == null || text.length() == 0)
            return null;

        Position position = getRecordLabelPosition(record);
        if (position == null)
            return null;

        return new Label(text, position);
    }

    protected static String getRecordLabelText(ShapefileRecord record)
    {
        AVList attr = record.getAttributes();
        if (attr.getEntries() == null || attr.getEntries().size() == 0)
            return null;

        for (Map.Entry entry : attr.getEntries())
        {
            if (((String) entry.getKey()).toUpperCase().equals("NAME"))
                return (String) entry.getValue();
        }

        return null;
    }

    protected static Position getRecordLabelPosition(ShapefileRecord record)
    {
        Position position = null;
        if (record.getShapeType().equals(Shapefile.SHAPE_POINT))
        {
            double[] point = ((ShapefileRecordPoint) record).getPoint();
            position = Position.fromDegrees(point[1], point[0], 0);
        }
        else if (record.getShapeType().equals(Shapefile.SHAPE_POLYLINE)
            || record.getShapeType().equals(Shapefile.SHAPE_POLYGON))
        {
            Sector boundingSector = Sector.fromDegrees(((ShapefileRecordPolyline) record).getBoundingRectangle());
            position = new Position(boundingSector.getCentroid(), 0);
        }

        return position;
    }
}
