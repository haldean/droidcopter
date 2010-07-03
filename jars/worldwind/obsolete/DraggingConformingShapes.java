/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.event.SelectListener;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.layers.IconLayer;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.pick.PickedObjectList;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.util.BasicDragger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.font.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author tag
 * @version $Id: DraggingShapes.java 7478 2008-11-11 14:54:53Z jmiller $
 */
public class DraggingConformingShapes extends ApplicationTemplate
{
    private static class AppFrame extends ApplicationTemplate.AppFrame
    {
        private boolean	drawWireframeInterior = false, drawWireframeExterior = false;
        private boolean buildConformingShapes = true;
        private boolean drawShapeInterior = true, drawShapeBoundary = true;
        private RenderableLayer rLayer;
        private int numShapesInLayer = -1; // -1 indicates demo set should be built.

        public AppFrame()
        {
            super(true,true,true);
            rLayer = new RenderableLayer();
            rebuildRenderableShapeLayer(this.getWwd().getModel().getGlobe());
            insertBeforeCompass(this.getWwd(), rLayer);
            insertBeforeCompass(this.getWwd(), this.buildIconLayer());
            this.getLayerPanel().update(this.getWwd());

            makeControlPanel(this.getLayerPanel());

            this.getWwd().addSelectListener(new SelectListener()
            {
                private WWIcon lastToolTipIcon = null;
                private BasicDragger dragger = new BasicDragger(getWwd());

                public void selected(SelectEvent event)
                {
                    // Have hover selections show a picked icon's tool tip.
                    if (event.getEventAction().equals(SelectEvent.HOVER))
                    {
                        // If a tool tip is already showing, undisplay it.
                        if (lastToolTipIcon != null)
                        {
                            lastToolTipIcon.setShowToolTip(false);
                            this.lastToolTipIcon = null;
                            AppFrame.this.getWwd().repaint();
                        }

                        // If there's a selection, we're not dragging, and the selection is an icon, show tool tip.
                        if (event.hasObjects() && !this.dragger.isDragging())
                        {
                            if (event.getTopObject() instanceof WWIcon)
                            {
                                this.lastToolTipIcon = (WWIcon) event.getTopObject();
                                lastToolTipIcon.setShowToolTip(true);
                                AppFrame.this.getWwd().repaint();
                            }
                            else if (event.getTopObject() instanceof ConformingPolygon)
                            {
                                ConformingPolygon cp = (ConformingPolygon)event.getTopObject();
                                Globe g = AppFrame.this.getWwd().getModel().getGlobe();
                                double length = cp.getLength(g);
                                double area = cp.getArea(g);
                                double perimeter = cp.getPerimeter(g);
                                double width = cp.getWidth(g);
                                double height = cp.getHeight(g);
                                System.out.println("length = " + length + ", area = " + area + ", perimeter = " + perimeter);
                                System.out.println("width = " + width + ", height = " + height);
                            }
                        }
                    }
                    // Have rollover events highlight the rolled-over object.
                    else if (event.getEventAction().equals(SelectEvent.ROLLOVER) && !this.dragger.isDragging())
                    {
                        AppFrame.this.highlight(event.getTopObject());
                    }

                    // Have drag events drag the selected object.
                    else if (event.getEventAction().equals(SelectEvent.DRAG_END)
                        || event.getEventAction().equals(SelectEvent.DRAG))
                    {
                        // Delegate dragging computations to a dragger.
                        this.dragger.selected(event);

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

        WWIcon lastPickedIcon;

        private void highlight(Object o)
        {
            // Manage highlighting of icons.

            if (this.lastPickedIcon == o)
                return; // same thing selected

            // Turn off highlight if on.
            if (this.lastPickedIcon != null)
            {
                this.lastPickedIcon.setHighlighted(false);
                this.lastPickedIcon = null;
            }

            // Turn on highlight if object selected.
            if (o != null && o instanceof WWIcon)
            {
                this.lastPickedIcon = (WWIcon) o;
                this.lastPickedIcon.setHighlighted(true);
            }
        }

        private IconLayer buildIconLayer()
        {
            IconLayer layer = new IconLayer();
            Font font = this.makeToolTipFont();

            // Distribute little NASA icons around the equator. Put a few at non-zero altitude.
            for (double lat = 0; lat < 10; lat += 10)
            {
                for (double lon = -180; lon < 180; lon += 10)
                {
                    double alt = 0;
                    if (lon % 90 == 0)
                        alt = 2000000;
                    WWIcon icon = new UserFacingIcon("images/32x32-icon-nasa.png",
                        new Position(Angle.fromDegrees(lat), Angle.fromDegrees(lon), alt));
                    icon.setHighlightScale(1.5);
                    icon.setToolTipFont(font);
                    icon.setToolTipText(icon.getImageSource().toString());
                    icon.setToolTipTextColor(java.awt.Color.YELLOW);
                    layer.addIcon(icon);
                }
            }

            return layer;
        }

        private void makeControlPanel(LayerPanel lp)
        {
            JPanel controlPanel = new JPanel();
            controlPanel.setLayout(new GridLayout(0,1));

            String[] shapeChoices = { "Demo", "25", "50", "100", "200", "400", "800", "1600", "3200" };
            JComboBox jcb = new JComboBox(shapeChoices);
            jcb.setSelectedIndex(0);
            jcb.addActionListener( new ActionListener()
            {
                private int[] num = { -1, 25, 50, 100, 200, 400, 800, 1600, 3200 };
                public void actionPerformed(ActionEvent e)
                {
                    JComboBox theBox = (JComboBox)e.getSource();
                    int which = theBox.getSelectedIndex();
                    AppFrame.this.numShapesInLayer = num[which];
                    AppFrame.this.rLayer.removeAllRenderables();
                    AppFrame.this.rebuildRenderableShapeLayer(
                            AppFrame.this.getWwd().getModel().getGlobe());
                    AppFrame.this.getWwd().redraw();
                }
            });
            controlPanel.add(jcb);

            JCheckBox buildWhatCB = new JCheckBox("Build 'Conforming' Shapes", buildConformingShapes);
            controlPanel.add(buildWhatCB);
            buildWhatCB.addActionListener( new ActionListener() {
                public void actionPerformed(ActionEvent e)
                {
                    buildConformingShapes = !buildConformingShapes;
                    AppFrame.this.rLayer.removeAllRenderables();
                    AppFrame.this.rebuildRenderableShapeLayer(
                            AppFrame.this.getWwd().getModel().getGlobe());
                    AppFrame.this.getWwd().redraw();
                }} );

            JCheckBox fillCB = new JCheckBox("Fill shape boundaries", drawShapeInterior);
            controlPanel.add(fillCB);
            fillCB.addActionListener( new ActionListener() {
                public void actionPerformed(ActionEvent e)
                {
                    drawShapeInterior = !drawShapeInterior;
                    for (Renderable r : rLayer.getRenderables())
                        if (r instanceof ConformingShape)
                        {
                            ((ConformingShape)r).setDrawInterior(drawShapeInterior);
                        }
                        else
                        {
                            ShapeAttributes attr = ((SurfaceShape) r).getAttributes();
                            attr.setDrawInterior(drawShapeInterior);
                            ((SurfaceShape) r).setAttributes(attr);
                        }
                    AppFrame.this.getWwd().redraw();
                }} );

            JCheckBox bdyCB = new JCheckBox("Draw shape boundaries", drawShapeBoundary);
            controlPanel.add(bdyCB);
            bdyCB.addActionListener( new ActionListener() {
                public void actionPerformed(ActionEvent e)
                {
                    drawShapeBoundary = !drawShapeBoundary;
                    for (Renderable r : rLayer.getRenderables())
                        if (r instanceof ConformingShape)
                        {
                            ((ConformingShape)r).setDrawBorder(drawShapeBoundary);
                        }
                        else
                        {
                            ShapeAttributes attr = ((SurfaceShape) r).getAttributes();
                            attr.setDrawOutline(drawShapeBoundary);
                            ((SurfaceShape) r).setAttributes(attr);
                        }
                    AppFrame.this.getWwd().redraw();
                }} );

            JCheckBox extCB = new JCheckBox("Show wireframe exterior", drawWireframeExterior);
            if (drawWireframeExterior) this.getWwd().getModel().setShowWireframeExterior(true);
            controlPanel.add(extCB);
            extCB.addActionListener( new ActionListener() {
                public void actionPerformed(ActionEvent e)
                {
                    drawWireframeExterior = !drawWireframeExterior;
                    WorldWindowGLCanvas wwd = AppFrame.this.getWwd();
                    wwd.getModel().setShowWireframeExterior(drawWireframeExterior);
                    wwd.redraw();
                }} );

            JCheckBox intCB = new JCheckBox("Show wireframe interior", drawWireframeInterior);
            if (drawWireframeInterior) this.getWwd().getModel().setShowWireframeInterior(true);
            controlPanel.add(intCB);
            intCB.addActionListener( new ActionListener() {
                public void actionPerformed(ActionEvent e)
                {
                    drawWireframeInterior = !drawWireframeInterior;
                    WorldWindowGLCanvas wwd = AppFrame.this.getWwd();
                    wwd.getModel().setShowWireframeInterior(drawWireframeInterior);
                    wwd.redraw();
                }} );

            lp.add(controlPanel,BorderLayout.SOUTH);
        }

        private Font makeToolTipFont()
        {
            HashMap<TextAttribute, Object> fontAttributes = new HashMap<TextAttribute, Object>();

            fontAttributes.put(TextAttribute.BACKGROUND, new java.awt.Color(0.4f, 0.4f, 0.4f, 1f));
            return Font.decode("Arial-BOLD-14").deriveFont(fontAttributes);
        }

        private void rebuildRenderableShapeLayer(Globe g)
        {
            if (numShapesInLayer > 0)
                buildRandomShapes(g);
            else
                buildDemoShapes(g);

        }

        private void buildDemoShapes(Globe g)
        {
            // a triangle over the Rockies.
            ArrayList<LatLon> v1 = new ArrayList<LatLon>();
            v1.add(new LatLon(Angle.fromDegrees(35),Angle.fromDegrees(-115)));
            v1.add(new LatLon(Angle.fromDegrees(35),Angle.fromDegrees(-105)));
            v1.add(new LatLon(Angle.fromDegrees(40),Angle.fromDegrees(-110)));
            v1.add(new LatLon(Angle.fromDegrees(35),Angle.fromDegrees(-115)));
            Color c1B = new Color(1.0f,0.0f,1.0f,1.0f);
            Color c1F = new Color(0.8f,0.0f,0.0f,0.3f);
            ConformingShape s1 = new ConformingPolygon(g,v1,c1F,c1B);
            s1.setBorderWidth(3.0);
            s1.setDrawBorder(drawShapeBoundary); s1.setDrawInterior(drawShapeInterior);
            rLayer.addRenderable(s1);

            // a concave shape over the eastern half of the U.S.
            ArrayList<LatLon> v2 = new ArrayList<LatLon>();
            v2.add(new LatLon(Angle.fromDegrees(30),Angle.fromDegrees(-80)));
            v2.add(new LatLon(Angle.fromDegrees(32),Angle.fromDegrees(-78)));
            v2.add(new LatLon(Angle.fromDegrees(34),Angle.fromDegrees(-72)));
            v2.add(new LatLon(Angle.fromDegrees(34),Angle.fromDegrees(-76)));
            v2.add(new LatLon(Angle.fromDegrees(38),Angle.fromDegrees(-72)));
            v2.add(new LatLon(Angle.fromDegrees(36),Angle.fromDegrees(-76)));
            v2.add(new LatLon(Angle.fromDegrees(40),Angle.fromDegrees(-80)));
            v2.add(new LatLon(Angle.fromDegrees(30),Angle.fromDegrees(-80)));
            ConformingShape s2 = new ConformingPolygon(g,v2);
            s2.setBorderWidth(2.0);
            s2.setDrawBorder(drawShapeBoundary); s2.setDrawInterior(drawShapeInterior);
            rLayer.addRenderable(s2);

            // a pentagon approximately over Washington D.C.
            ConformingEllipticalPolygon s3 = new ConformingEllipticalPolygon(g,
                    new LatLon(Angle.fromDegrees(39),Angle.fromDegrees(-77)),
                    1.1e5,1.1e5, Angle.ZERO, 5);
            s3.setDrawBorder(drawShapeBoundary); s3.setDrawInterior(drawShapeInterior);
            rLayer.addRenderable(s3);

            // a hexagon inscribed in an ellipse in southern Minnesota
            ConformingEllipticalPolygon s4 = new ConformingEllipticalPolygon(g,
                    new LatLon(Angle.fromDegrees(44),Angle.fromDegrees(-96)),
                    3.75e5,2.0e5, Angle.ZERO, 6);
            s4.setDrawBorder(drawShapeBoundary); s4.setDrawInterior(drawShapeInterior);
            rLayer.addRenderable(s4);

            // a rectangle in Oregon
            ConformingQuad s5 = new ConformingQuad(g,
                    new LatLon(Angle.fromDegrees(43.75),Angle.fromDegrees(-121)),
                    1.5e5,8e4,Angle.ZERO);
            s5.setDrawBorder(drawShapeBoundary); s5.setDrawInterior(drawShapeInterior);
            rLayer.addRenderable(s5);

            // another rectangle in the southeast
            ConformingQuad s6 = new ConformingQuad(g,
                    new LatLon(Angle.fromDegrees(25),Angle.fromDegrees(-90)),
                    4.5e5,3e4,Angle.ZERO);
            s6.setDrawBorder(drawShapeBoundary); s6.setDrawInterior(drawShapeInterior);
            rLayer.addRenderable(s6);

            // a hexagon straddling the international date line
            ConformingEllipticalPolygon s7 = new ConformingEllipticalPolygon(g,
                    new LatLon(Angle.fromDegrees(20),Angle.fromDegrees(179)),
                    4.25e5,2.0e5, Angle.ZERO, 6);
            s7.setDrawBorder(drawShapeBoundary); s7.setDrawInterior(drawShapeInterior);
            rLayer.addRenderable(s7);

            // a triangle straddling the date line
            ArrayList<LatLon> v8 = new ArrayList<LatLon>();
            v8.add(new LatLon(Angle.fromDegrees(-35),Angle.fromDegrees(170)));
            v8.add(new LatLon(Angle.fromDegrees(-35),Angle.fromDegrees(-170)));
            v8.add(new LatLon(Angle.fromDegrees(-20),Angle.fromDegrees(-175)));
            v8.add(new LatLon(Angle.fromDegrees(-35),Angle.fromDegrees(170)));
            Color c8B = new Color(1.0f,0.0f,1.0f,1.0f);
            Color c8F = new Color(0.8f,0.0f,0.0f,0.3f);
            ConformingShape s8 = new ConformingPolygon(g,v8,c8F,c8B);
            s8.setBorderWidth(2.0);
            s8.setDrawBorder(drawShapeBoundary); s8.setDrawInterior(drawShapeInterior);
            rLayer.addRenderable(s8);

            // another triangle straddling the date line
            ArrayList<LatLon> v9 = new ArrayList<LatLon>();
            v9.add(new LatLon(Angle.fromDegrees(-15),Angle.fromDegrees(170)));
            v9.add(new LatLon(Angle.fromDegrees(-15),Angle.fromDegrees(-170)));
            v9.add(new LatLon(Angle.fromDegrees(  0),Angle.fromDegrees(170)));
            v9.add(new LatLon(Angle.fromDegrees(-15),Angle.fromDegrees(170)));
            Color c9B = new Color(1.0f,0.0f,1.0f,1.0f);
            Color c9F = new Color(0.8f,0.0f,0.0f,0.3f);
            ConformingShape s9 = new ConformingPolygon(g,v9,c9F,c9B);
            s9.setBorderWidth(2.0);
            s9.setDrawBorder(drawShapeBoundary); s9.setDrawInterior(drawShapeInterior);
            rLayer.addRenderable(s9);

            // new Circle and ellipse shapes
            ConformingShape n1 = new ConformingEllipse(new LatLon(Angle.fromDegrees(20),Angle.fromDegrees(-100)),
                3.75e5,2.0e5, null);
            n1.setDrawBorder(drawShapeBoundary); n1.setDrawInterior(drawShapeInterior);
            rLayer.addRenderable(n1);

            ConformingShape n2 = new ConformingCircle(new LatLon(Angle.fromDegrees(30),Angle.fromDegrees(-100)),
                3.75e5);
            n2.setDrawBorder(drawShapeBoundary); n2.setDrawInterior(drawShapeInterior);
            rLayer.addRenderable(n2);
        }

        private void buildRandomShapes(Globe g)
        {
            int[] c1Int = { 3, 4, 5, 6, 7, 8 };
            Color[] c1B = { new Color(1.0f,0.0f,0.0f,1.0f),
                            new Color(0.0f,1.0f,0.0f,1.0f),
                            new Color(0.0f,0.0f,1.0f,1.0f),
                            new Color(0.0f,1.0f,1.0f,1.0f),
                            new Color(1.0f,0.0f,1.0f,1.0f),
                            new Color(1.0f,1.0f,0.0f,1.0f) };
            float fillAlpha = 0.3f;
            float brighten  = 0.3f;
            Color[] c1F = { new Color(1.0f,brighten,brighten,fillAlpha),
                            new Color(brighten,1.0f,brighten,fillAlpha),
                            new Color(brighten,brighten,1.0f,fillAlpha),
                            new Color(brighten,1.0f,1.0f,fillAlpha),
                            new Color(1.0f,brighten,1.0f,fillAlpha),
                            new Color(1.0f,1.0f,brighten,fillAlpha) };
            double latMin = 0.0, latMax = 80.0;
            double lonMin = -170, lonMax = 0.0;

            for (int i=0 ; i<numShapesInLayer ; i++)
            {
                // randomly choose center
                int j = i%c1Int.length;
                double cLat = latMin + Math.random()*(latMax - latMin);
                double cLon = lonMin + Math.random()*(lonMax - lonMin);
                if (buildConformingShapes)
                {
                    ConformingEllipticalPolygon ceShape = new ConformingEllipticalPolygon(g,
                        new LatLon(Angle.fromDegrees(cLat),Angle.fromDegrees(cLon)),
                        1.75e5,1.0e5, Angle.ZERO, c1Int[j], c1F[j], c1B[j]);
                    ceShape.setDrawBorder(drawShapeBoundary);
                    ceShape.setDrawInterior(drawShapeInterior);
                    rLayer.addRenderable(ceShape);
                }
                else
                {
                    SurfaceEllipse seShape = new SurfaceEllipse(
                        new LatLon(Angle.fromDegrees(cLat),Angle.fromDegrees(cLon)),
                        1.75e5, 1.0e5, Angle.ZERO);
                    ShapeAttributes attr = new BasicShapeAttributes();
                    attr.setDrawInterior(drawShapeInterior);
                    attr.setDrawOutline(drawShapeBoundary);
                    attr.setInteriorMaterial(new Material(c1F[j]));
                    attr.setOutlineMaterial(new Material(c1B[j]));
                    attr.setInteriorOpacity(c1F[j].getAlpha() / 255.0);
                    attr.setOutlineOpacity(c1B[j].getAlpha() / 255.0);
                    seShape.setAttributes(attr);
                    rLayer.addRenderable(seShape);
                }
            }
        }
    }

    public static void main(String[] args)
    {
        ApplicationTemplate.start("Conforming Surface Shape Dragging", AppFrame.class);
    }
}
