/*
Copyright (C) 2001, 2008 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/

package gov.nasa.worldwind.render.markers;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.pick.*;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.Logging;

import javax.media.opengl.GL;
import java.util.*;

/**
 * @author tag
 * @version $Id: MarkerRenderer.java 12847 2009-11-30 22:10:40Z patrickmurris $
 */
public class MarkerRenderer
{
    private double elevation = 10d;
    private boolean overrideMarkerElevation = false;
    private boolean keepSeparated = true;
    private boolean enablePickSizeReturn = false;
    private long frameTimeStamp = 0;
    ArrayList<Vec4> surfacePoints = new ArrayList<Vec4>();
    protected PickSupport pickSupport = new PickSupport();

    public double getElevation()
    {
        return elevation;
    }

    public void setElevation(double elevation)
    {
        this.elevation = elevation;
    }

    public boolean isOverrideMarkerElevation()
    {
        return overrideMarkerElevation;
    }

    public void setOverrideMarkerElevation(boolean overrideMarkerElevation)
    {
        this.overrideMarkerElevation = overrideMarkerElevation;
    }

    public boolean isKeepSeparated()
    {
        return keepSeparated;
    }

    public void setKeepSeparated(boolean keepSeparated)
    {
        this.keepSeparated = keepSeparated;
    }

    public boolean isEnablePickSizeReturn()
    {
        return enablePickSizeReturn;
    }

    public void setEnablePickSizeReturn(boolean enablePickSizeReturn)
    {
        this.enablePickSizeReturn = enablePickSizeReturn;
    }

    public void render(DrawContext dc, Iterable<Marker> markers)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        if (markers == null)
        {
            String message = Logging.getMessage("nullValue.MarkerListIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        this.draw(dc, markers);
    }

    public void pick(DrawContext dc, Iterable<Marker> markers, java.awt.Point pickPoint, Layer layer)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        if (pickPoint == null)
        {
            String message = Logging.getMessage("nullValue.PickPoint");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        this.pickSupport.clearPickList();
        this.draw(dc, markers);
        this.pickSupport.resolvePick(dc, pickPoint, layer);
        this.pickSupport.clearPickList(); // to ensure entries can be garbage collected
    }

    protected void draw(DrawContext dc, Iterable<Marker> markers)
    {
        if (this.isKeepSeparated())
            this.drawSeparated(dc, markers);
        else
            this.drawAll(dc, markers);
    }

    private MarkerAttributes previousAttributes; // used only by drawSeparated and drawMarker

    protected void drawSeparated(DrawContext dc, Iterable<Marker> markers)
    {
        this.begin(dc);
        {
            try
            {
                List<Marker> markerList;
                if (markers instanceof List)
                {
                    markerList = (List<Marker>) markers;
                }
                else
                {
                    markerList = new ArrayList<Marker>();
                    for (Marker m : markers)
                    {
                        markerList.add(m);
                    }
                }

                if (markerList.size() == 0)
                    return;

                this.previousAttributes = null;

                Marker m1 = markerList.get(0);
                Vec4 p1 = this.computeSurfacePoint(dc, m1.getPosition());
                double r1 = this.computeMarkerRadius(dc, p1, m1);
                this.drawMarker(dc, 0, m1, p1, r1);

                if (markerList.size() < 2)
                    return;

                Marker m2 = markerList.get(markerList.size() - 1);
                Vec4 p2 = this.computeSurfacePoint(dc, m2.getPosition());
                double r2 = this.computeMarkerRadius(dc, p2, m2);
                this.drawMarker(dc, markerList.size() - 1, m2, p2, r2);

                if (markerList.size() < 3)
                    return;

                this.drawInBetweenMarkers(dc, 0, p1, r1, markerList.size() - 1, p2, r2, markerList);
            }
            finally
            {
                this.end(dc);
            }
        }
    }

    private void drawInBetweenMarkers(DrawContext dc, int im1, Vec4 p1, double r1, int im2, Vec4 p2, double r2,
        List<Marker> markerList)
    {
        if (im2 == im1 + 1)
            return;

        if (p1.distanceTo3(p2) <= r1 + r2)
            return;

        int im = (im1 + im2) / 2;
        Marker m = markerList.get(im);
        Vec4 p = this.computeSurfacePoint(dc, m.getPosition());
        double r = this.computeMarkerRadius(dc, p, m);

        boolean b1 = false, b2 = false;
        if (p.distanceTo3(p1) > r + r1)
        {
            this.drawInBetweenMarkers(dc, im1, p1, r1, im, p, r, markerList);
            b1 = true;
        }

        if (p.distanceTo3(p2) > r + r2)
        {
            this.drawInBetweenMarkers(dc, im, p, r, im2, p2, r2, markerList);
            b2 = true;
        }

        if (b1 && b2)
            this.drawMarker(dc, im, m, p, r);
    }

    private void drawMarker(DrawContext dc, int index, Marker marker, Vec4 point, double radius)
    {
        if (!dc.getView().getFrustumInModelCoordinates().contains(point))
            return;

        if (dc.isPickingMode())
        {
            // Eliminate markers not within the pick frustum.
            if (!dc.getPickFrustums().intersectsAny(new Sphere(point, radius)))
                return;

            java.awt.Color color = dc.getUniquePickColor();
            int colorCode = color.getRGB();
            PickedObject po = new PickedObject(colorCode, marker, marker.getPosition(), false);
            po.setValue(AVKey.PICKED_OBJECT_ID, index);
            if (this.enablePickSizeReturn)
                po.setValue(AVKey.PICKED_OBJECT_SIZE, 2 * radius);
            this.pickSupport.addPickableObject(po);
            dc.getGL().glColor3ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue());
        }

        MarkerAttributes attrs = marker.getAttributes();
        if (attrs != this.previousAttributes) // equality is intentional to avoid constant equals() calls
        {
            attrs.apply(dc);
            this.previousAttributes = attrs;
        }

        marker.render(dc, point, radius);
    }

    protected void computeSurfacePoints(DrawContext dc, Iterable<Marker> markers)
    {
        surfacePoints.clear();
        for (Marker marker : markers)
        {
            // If the marker is null, add a null reference to the surfacePoints cache array so that it is
            // the same size as the marker iterator.
            if (marker == null)
            {
                surfacePoints.add(null);
                continue;
            }
            // Compute the surface point
            Position pos = marker.getPosition();
            Vec4 point = this.computeSurfacePoint(dc, pos);
            // Check to see that the point is within the frustum.  If it is not, place a null reference in the
            // surfacePoints array.  This will let the drawAll method know not to render it on the 2nd pass.
            if (!dc.getView().getFrustumInModelCoordinates().contains(point))
            {
                surfacePoints.add(null);
                continue;
            }
            // Add the point to the cache array.
            surfacePoints.add(point);
        }
    }

    protected void drawAll(DrawContext dc, Iterable<Marker> markers)
    {
        this.begin(dc);
        {
            // If this is a new frame, recompute surface points.
            if (dc.getFrameTimeStamp() != this.frameTimeStamp)
            {
                this.frameTimeStamp = dc.getFrameTimeStamp();
                this.computeSurfacePoints(dc, markers);
            }

            try
            {
                MarkerAttributes previousAttrs = null;

                Iterator<Marker> markerIterator = markers.iterator();
                for (int index = 0; markerIterator.hasNext(); index++)
                {
                    Marker marker = markerIterator.next();
                    Vec4 surfacePoint = surfacePoints.get(index); // TODO: check performance of this buffer access
                    // The surface point is null if the marker in this position is null or if the
                    // surface point is not in the view frustum.
                    if (surfacePoint == null)
                        continue;

                    MarkerAttributes attrs = marker.getAttributes();
                    if (attrs != previousAttrs) // equality test is intentional to avoid constant equals() calls
                    {
                        attrs.apply(dc);
                        previousAttrs = attrs;
                    }

                    double radius = this.computeMarkerRadius(dc, surfacePoint, marker);
                    if (dc.isPickingMode())
                    {
                        // Eliminate markers not within the pick frustum.
                        if (!dc.getPickFrustums().intersectsAny(new Sphere(surfacePoint, radius)))
                            continue;

                        java.awt.Color color = dc.getUniquePickColor();
                        int colorCode = color.getRGB();
                        PickedObject po = new PickedObject(colorCode, marker, marker.getPosition(), false);
                        po.setValue(AVKey.PICKED_OBJECT_ID, index);
                        if (this.enablePickSizeReturn)
                            po.setValue(AVKey.PICKED_OBJECT_SIZE, 2 * radius);
                        this.pickSupport.addPickableObject(po);
                        dc.getGL().glColor3ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue());
                    }
                    
                    marker.render(dc, surfacePoint, radius);
                }
            }
            finally
            {
                this.end(dc);
            }
        }
    }

    protected void begin(DrawContext dc)
    {
        GL gl = dc.getGL();
        Vec4 cameraPosition = dc.getView().getEyePoint();

        if (dc.isPickingMode())
        {
            this.pickSupport.beginPicking(dc);

            gl.glPushAttrib(GL.GL_ENABLE_BIT | GL.GL_CURRENT_BIT | GL.GL_TRANSFORM_BIT);
            gl.glDisable(GL.GL_TEXTURE_2D);
            gl.glDisable(GL.GL_COLOR_MATERIAL);
        }
        else
        {
            gl.glPushAttrib(
                GL.GL_TEXTURE_BIT | GL.GL_ENABLE_BIT | GL.GL_CURRENT_BIT | GL.GL_LIGHTING_BIT | GL.GL_TRANSFORM_BIT
                    | GL.GL_COLOR_BUFFER_BIT);
            gl.glDisable(GL.GL_TEXTURE_2D);

            float[] lightPosition =
                {(float) (cameraPosition.x * 2), (float) (cameraPosition.y / 2), (float) (cameraPosition.z), 0.0f};
            float[] lightDiffuse = {1.0f, 1.0f, 1.0f, 1.0f};
            float[] lightAmbient = {1.0f, 1.0f, 1.0f, 1.0f};
            float[] lightSpecular = {1.0f, 1.0f, 1.0f, 1.0f};

            gl.glDisable(GL.GL_COLOR_MATERIAL);

            gl.glLightfv(GL.GL_LIGHT1, GL.GL_POSITION, lightPosition, 0);
            gl.glLightfv(GL.GL_LIGHT1, GL.GL_DIFFUSE, lightDiffuse, 0);
            gl.glLightfv(GL.GL_LIGHT1, GL.GL_AMBIENT, lightAmbient, 0);
            gl.glLightfv(GL.GL_LIGHT1, GL.GL_SPECULAR, lightSpecular, 0);

            gl.glDisable(GL.GL_LIGHT0);
            gl.glEnable(GL.GL_LIGHT1);
            gl.glEnable(GL.GL_LIGHTING);
            gl.glEnable(GL.GL_NORMALIZE);

            // Set up for opacity, either explictly via attributes or implicitly as alpha in the marker color
            dc.getGL().glEnable(GL.GL_BLEND);
            dc.getGL().glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
        }

        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glPushMatrix();
    }

    protected void end(DrawContext dc)
    {
        GL gl = dc.getGL();

        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glPopMatrix();

        if (dc.isPickingMode())
        {
            this.pickSupport.endPicking(dc);
        }
        else
        {
            gl.glDisable(GL.GL_LIGHT1);
            gl.glEnable(GL.GL_LIGHT0);
            gl.glDisable(GL.GL_LIGHTING);
            gl.glDisable(GL.GL_NORMALIZE);
        }

        gl.glPopAttrib();
    }

    protected Vec4 computeSurfacePoint(DrawContext dc, Position pos)
    {
        double ve = dc.getVerticalExaggeration();
        if (!this.overrideMarkerElevation)
            return dc.getGlobe().computePointFromPosition(pos, pos.getElevation() * ve);

        // Compute points that are at the renderer-specified elevation
        Vec4 point = dc.getSurfaceGeometry().getSurfacePoint(pos.getLatitude(), pos.getLongitude(), this.elevation * ve);
        if (point != null)
            return point;

        // Point is outside the current sector geometry, so compute it from the globe.
        return dc.getGlobe().computePointFromPosition(pos.getLatitude(), pos.getLongitude(), this.elevation * ve);
    }

    protected double computeMarkerRadius(DrawContext dc, Vec4 point, Marker marker)
    {
        double d = point.distanceTo3(dc.getView().getEyePoint());
        double radius = marker.getAttributes().getMarkerPixels() * dc.getView().computePixelSizeAtDistance(d);
        if (radius < marker.getAttributes().getMinMarkerSize())
            radius = marker.getAttributes().getMinMarkerSize();
        else if (radius > marker.getAttributes().getMaxMarkerSize())
            radius = marker.getAttributes().getMaxMarkerSize();

        return radius;
    }
}
