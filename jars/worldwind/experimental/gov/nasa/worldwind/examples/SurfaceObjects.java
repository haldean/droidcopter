/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.util.BasicDragger;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.event.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.layers.*;
import gov.nasa.worldwind.layers.placename.PlaceNameLayer;
import gov.nasa.worldwind.pick.*;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.render.markers.*;

import java.awt.*;
import java.awt.image.*;
import java.util.ArrayList;

/**
 * Surface objects experiments.
 *
 * @author Patrick Murris
 * @version $Id: SurfaceObjects.java 13151 2010-02-20 20:26:20Z tgaskins $
 */
public class SurfaceObjects extends ApplicationTemplate
{
    private static class AppFrame extends ApplicationTemplate.AppFrame
    {
        protected CustomMarker lastHighlitShape;
        protected BasicDragger dragger;
        protected boolean drawWireframeInterior = false;
        protected boolean drawWireframeExterior = false;
        protected Angle dragStartHeading = null;
        protected Angle markerStartHeading = null;
        protected Marker spinTarget= null;

        public AppFrame()
        {
            // Insert surface graticule layer
            insertBeforeCompass(this.getWwd(), this.buildGraticuleLayer());

            // Insert surface icons layer
            insertBeforeCompass(this.getWwd(), this.buildIconsLayer());

            // Insert surface objects layer
            insertBeforeCompass(this.getWwd(), this.buildShapesLayer());

            // Turn off placenames
            for (Layer layer : getWwd().getModel().getLayers())
                if (layer instanceof PlaceNameLayer)
                    layer.setEnabled(false);

            this.getLayerPanel().update(this.getWwd());

            // Pick and drag handlers
            this.dragger = new BasicDragger(getWwd());

            this.getWwd().addSelectListener(new SelectListener()
            {
                public void selected(SelectEvent event)
                {
                    //System.out.println(event.getEventAction() + " - " + event.getTopObject());
                    
                    if (lastHighlitShape != null
                        && (event.getTopObject() == null || !event.getTopObject().equals(lastHighlitShape)))
                    {
                        lastHighlitShape.setHighlighted(false);
                        lastHighlitShape = null;
                    }

                    // Have rollover events highlight the rolled-over object.
                    if (event.getEventAction().equals(SelectEvent.ROLLOVER) && !dragger.isDragging())
                    {
                        AppFrame.this.highlight(event.getTopObject());
                    }

                    // Have drag events drag the selected object.
                    else if (event.getEventAction().equals(SelectEvent.DRAG_END)
                        || event.getEventAction().equals(SelectEvent.DRAG))
                    {
                        // Delegate dragging computations to a dragger.
                        if (event.getTopPickedObject().getValue("Heading") == null)
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

            // Add select listener to handle drag events on the markers heading indicator
            this.getWwd().addSelectListener(new SelectListener()
            {
                public void selected(SelectEvent event)
                {
                    if (event.getTopObject() instanceof Marker)
                    {
                        Marker marker = (Marker)event.getTopObject();
                        if (event.getTopPickedObject().getValue("Heading") == null || marker.getHeading() == null)
                            return;

                        if (event.getEventAction().equals(SelectEvent.DRAG) && spinTarget == null)
                        {
                            Position mousePos = getWwd().getCurrentPosition();
                            spinTarget = marker;
                            dragStartHeading = LatLon.greatCircleAzimuth(spinTarget.getPosition(), mousePos);
                            markerStartHeading = marker.getHeading();
                        }
                        else if(event.getEventAction().equals(SelectEvent.DRAG_END))
                        {
                            spinTarget = null;
                        }
                    }
                }
            });

            // Add position listener to handle marker heading drag
            this.getWwd().addPositionListener(new PositionListener()
            {
                public void moved(PositionEvent event)
                {
                    Position mousePos = getWwd().getCurrentPosition();
                    if (spinTarget != null && mousePos != null)
                    {
                        Angle heading = LatLon.greatCircleAzimuth(spinTarget.getPosition(), mousePos);
                        double move = heading.degrees - dragStartHeading.degrees;
                        double newHeading = markerStartHeading.degrees + move;
                        newHeading = newHeading >= 0 ? newHeading : newHeading + 360;
                        spinTarget.setHeading(Angle.fromDegrees(newHeading));
                        getWwd().redraw();
                    }

                }
            });


        }

        protected void highlight(Object o)
        {
            // Same object selected.
            if (o == this.lastHighlitShape)
                return;

            if (this.lastHighlitShape == null && o instanceof CustomMarker)
            {
                this.lastHighlitShape = (CustomMarker) o;
                this.lastHighlitShape.setHighlighted(true);
            }
        }

        protected Layer buildIconsLayer()
        {
            RenderableLayer layer = new RenderableLayer();
            layer.setName("Surface Icons");
            layer.setPickEnabled(false);

            ArrayList<LatLon> locations = new ArrayList<LatLon>();
            for (int lat = 20; lat <= 60; lat++)
                for (int lon = -30; lon <= 40; lon++)
                    locations.add(LatLon.fromDegrees(lat, lon));
            
            SurfaceIcon icon = new SurfaceIcons("images/notched-compass.png", locations);
            icon.setOpacity(1);
            icon.setScale(.5);
            icon.setMaxSize(50e3);
            icon.setHeading(null); // follow eye - always facing
            layer.addRenderable(icon);

            return layer;
        }

        protected Layer buildGraticuleLayer()
        {
            RenderableLayer layer = new RenderableLayer();
            layer.setName("Surface Graticule");
            layer.setPickEnabled(false);

            // Graticule
            SurfaceGraticule sg = new SurfaceGraticule();
            sg.setOpacity(.6);
            layer.addRenderable(sg);

            return layer;
        }

        protected Layer buildShapesLayer()
        {
            RenderableLayer layer = new RenderableLayer();
            layer.setName("Surface Objects");

            // Surface text
            Font font = Font.decode("Arial-BOLD-24");
            Color color = Color.WHITE;
            layer.addRenderable(new SurfaceText("SEATTLE", LatLon.fromDegrees(47.60, -122.33), font, color));
            layer.addRenderable(new SurfaceText("PORTLAND", LatLon.fromDegrees(45.52, -122.67), font, color));
            layer.addRenderable(new SurfaceText("SAN FRANCSISCO", LatLon.fromDegrees(37.77, -122.42), font, color));
            color = Color.GREEN;
            layer.addRenderable(new SurfaceText("Mt SHASTA", LatLon.fromDegrees(41.40, -122.19), font, color));
            layer.addRenderable(new SurfaceText("Mt RAINIER", LatLon.fromDegrees(46.85, -121.76), font, color));
            //layer.addRenderable(new SurfaceText("Mt SAINT HELENS", LatLon.fromDegrees(46.19, -122.18), font, color));
            layer.addRenderable(new SurfaceText("Mt ADAMS", LatLon.fromDegrees(46.20, -121.49), font, color));

            SurfaceText text;
            //text = new SurfaceText("NICE", LatLon.fromDegrees(43.70, 7.26), font, Color.WHITE);
            //layer.addRenderable(text);
            text = new SurfaceText("ALPES", LatLon.fromDegrees(45.8, 7.6), Font.decode("Arial-BOLD-54"), Color.CYAN);
            layer.addRenderable(text);

//            // AnnotationShadow
//            GlobeAnnotation ga;
//            ga = new AnnotationShadow("<p>\n<b><font color=\"#664400\">MOUNT SAINT HELENS</font></b><br />\n<i>Alt: 2549m</i>\n</p>\n<p>\nMount St. Helens is an active stratovolcano located in Skamania County, Washington, USA.\n</p>",
//                    Position.fromDegrees(46.19, -122.18, 0));
//            ga.getAttributes().setFont(Font.decode("SansSerif-PLAIN-14"));
//            ga.getAttributes().setTextColor(Color.BLACK);
//            ga.getAttributes().setTextAlign(AVKey.RIGHT);
//            ga.getAttributes().setBackgroundColor(new Color(1f, 1f, 1f, .7f));
//            ga.getAttributes().setBorderColor(Color.BLACK);
//            ga.getAttributes().setSize(new Dimension(180, 0));
//            ga.getAttributes().setImageSource("images/32x32-icon-earth.png");
//            ga.getAttributes().setImageRepeat(Annotation.IMAGE_REPEAT_NONE);
//            ga.getAttributes().setImageOpacity(.6);
//            ga.getAttributes().setImageScale(.7);
//            ga.getAttributes().setImageOffset(new Point(12, 12));
//            ga.getAttributes().setInsets(new Insets(12, 20, 20, 12));
//            layer.addRenderable(ga);


            // Simple surface icons
            SurfaceIcon icon;
            icon = new SurfaceIcon("images/notched-compass.png", LatLon.fromDegrees(46, -121));
            icon.setOpacity(1);
            icon.setScale(.5);
            icon.setMaxSize(50e3);
            layer.addRenderable(icon);

            // Custom Marker with surface icons
            Position position = Position.fromDegrees(47.74, -123.44, 0);
            MarkerAttributes ma = new BasicMarkerAttributes();
            ma.setMaxMarkerSize(10e3);
            ma.setMinMarkerSize(10);
            ma.setMaterial(Material.RED);
            CustomMarker marker = new CustomMarker(position, ma);
            marker.setHeading(Angle.ZERO);
            layer.addRenderable(marker);

            return layer;
        }

        private class CustomMarker extends BasicMarker implements PreRenderable, Renderable, Movable
        {
            private ArrayList<Marker> markerList;
            private MarkerRenderer markerRenderer;
            private SurfaceIcon headingIcon;
            private SurfaceIcon highlightIcon;
            private PickSupport pickSupport = new PickSupport();
            private SurfaceTrail trail = new SurfaceTrail();
            private Material savedMaterial;

            public CustomMarker(Position position, MarkerAttributes attrs)
            {
                super(position, attrs);

                // Heading icon
                this.headingIcon = new SurfaceIcon("images/notched-compass.png", position);
                this.headingIcon.setOpacity(.5);
                this.headingIcon.setScale(.5);
                this.headingIcon.setMaxSize(50e3);
                this.headingIcon.setMaintainSize(true);

                // Highlight icon
                BufferedImage image = PatternFactory.createPattern(PatternFactory.PATTERN_CIRCLE, .9f, Color.RED);
                image = PatternFactory.blur(PatternFactory.blur(image, 5));
                this.highlightIcon = new SurfaceIcon(image, position);
                this.highlightIcon.setOpacity(.5);
                this.highlightIcon.setScale(3);
                this.highlightIcon.setVisible(false);
                this.highlightIcon.setMaintainSize(true);
            }

            public void setHighlighted(boolean state)
            {
                if (this.highlightIcon.isVisible() == state)
                    return;

                this.highlightIcon.setVisible(state);
                if (this.highlightIcon.isVisible())
                {
                    this.savedMaterial = this.getAttributes().getMaterial();
                    this.getAttributes().setMaterial(Material.RED);
                }
                else
                    this.getAttributes().setMaterial(this.savedMaterial);
            }

            public void preRender(DrawContext dc)
            {
                this.trail.preRender(dc);
                this.headingIcon.setHeading(this.getHeading());
                this.headingIcon.preRender(dc);
                this.highlightIcon.preRender(dc);
            }

            public void render(DrawContext dc)
            {
                if (this.markerList == null)
                {
                    this.markerList = new ArrayList<Marker>();
                    this.markerList.add(this);
                }
                if (this.markerRenderer == null)
                {
                    this.markerRenderer = new MarkerRenderer();
                    this.markerRenderer.setOverrideMarkerElevation(true);
                    this.markerRenderer.setElevation(0);
                }

                if (dc.isPickingMode())
                {
                    this.markerRenderer.pick(dc, this.markerList, dc.getPickPoint(), null);
                    // Separate pick color for the icon
                    this.pickSupport.clearPickList();
                    this.pickSupport.beginPicking(dc);
                    java.awt.Color color = dc.getUniquePickColor();
                    dc.getGL().glColor3ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue());
                    this.headingIcon.render(dc);
                    PickedObject po = new PickedObject(color.getRGB(), this);
                    po.setValue("Heading", true); // Attach heading key to picked object
                    this.pickSupport.addPickableObject(po);
                    this.pickSupport.endPicking(dc);
                    this.pickSupport.resolvePick(dc, dc.getPickPoint(), null);
                }
                else
                {
                    this.trail.render(dc);
                    this.markerRenderer.render(dc, this.markerList);
                    this.headingIcon.render(dc);
                    this.highlightIcon.render(dc);
                }

            }

            public Position getReferencePosition()
            {
                return this.getPosition();
            }

            public void move(Position delta)
            {
                this.moveTo(this.getReferencePosition().add(delta));
            }

            public void moveTo(Position position)
            {
                // Add current position to the trail
                this.trail.add(this.getReferencePosition());
                // Have the marker point in the move direction
                Angle heading = LatLon.greatCircleAzimuth(this.getReferencePosition(), position);
                this.setHeading(heading);
                // Update positions
                this.headingIcon.setLocation(position);
                this.highlightIcon.setLocation(position);
                this.setPosition(position);
            }
        }

        private class SurfaceTrail implements PreRenderable, Renderable
        {
            private int MAX_SIZE = 30;
            private ArrayList<SurfaceIcon> locations = new ArrayList<SurfaceIcon>();
            private TiledSurfaceObjectRenderer renderer = new TiledSurfaceObjectRenderer();
            private BufferedImage imageSource = PatternFactory.createPattern(PatternFactory.PATTERN_CIRCLE, .9f, Color.BLACK);

            public void add(LatLon location)
            {
                // Add shape
                SurfaceIcon icon = new SurfaceIcon(this.imageSource, location);
                icon.setScale(.5);
                this.locations.add(icon);
                if (this.locations.size() > MAX_SIZE)
                    this.locations.remove(0);

                this.renderer.setSurfaceObjects(this.locations);
                // Fade out
                for (int i = 0; i < this.locations.size(); i++)
                    this.locations.get(i).setOpacity(1d - (double)(this.locations.size() - i - 1) / MAX_SIZE);
            }

            public void preRender(DrawContext dc)
            {
                this.renderer.preRender(dc);
            }

            public void render(DrawContext dc)
            {
                this.renderer.render(dc);
            }
        }

    }

    public static void main(String[] args)
    {
        // Set up view initial state
        Configuration.setValue(AVKey.INITIAL_LATITUDE, 47.15);
        Configuration.setValue(AVKey.INITIAL_LONGITUDE, -122.74);
        Configuration.setValue(AVKey.INITIAL_ALTITUDE, 300e3);
        Configuration.setValue(AVKey.INITIAL_PITCH, 60);
        Configuration.setValue(AVKey.INITIAL_HEADING, 155);
        
        ApplicationTemplate.start("World Wind Surface Objects", AppFrame.class);
    }
}
