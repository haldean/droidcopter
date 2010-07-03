/*
Copyright (C) 2001, 2010 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.util.*;
import gov.nasa.worldwind.util.measure.LengthMeasurer;

import javax.media.opengl.GL;
import java.awt.*;
import java.util.*;

/**
 * @author tag
 * @version $Id: Polyline.java 13355 2010-04-30 06:59:25Z tgaskins $
 */
public class Polyline extends AVListImpl implements Renderable, Movable, Restorable, MeasurableLength, ExtentHolder
{
    public final static int GREAT_CIRCLE = 0;
    public final static int LINEAR = 1;
    public final static int RHUMB_LINE = 2;
    public final static int LOXODROME = RHUMB_LINE;

    public final static int ANTIALIAS_DONT_CARE = GL.GL_DONT_CARE;
    public final static int ANTIALIAS_FASTEST = GL.GL_FASTEST;
    public final static int ANTIALIAS_NICEST = GL.GL_NICEST;

    protected ArrayList<Position> positions;
    protected Vec4 referenceCenterPoint;
    protected int antiAliasHint = GL.GL_FASTEST;
    protected Color color = Color.WHITE;
    protected double lineWidth = 1;
    protected boolean filled = false; // makes it a polygon
    protected boolean closed = false; // connect last point to first
    protected boolean followTerrain = false;
    protected double offset = 0;
    protected double terrainConformance = 10;
    protected int pathType = GREAT_CIRCLE;
    protected ArrayList<ArrayList<Vec4>> currentSpans;
    protected short stipplePattern = (short) 0xAAAA;
    protected int stippleFactor = 0;
    protected int numSubsegments = 10;
    protected boolean highlighted = false;
    protected Color highlightColor = new Color(1f, 1f, 1f, 0.5f);
    protected LengthMeasurer measurer = new LengthMeasurer();
    protected long geomGenTimeStamp = -Long.MAX_VALUE;
    protected double geomGenVE = 1;

    // Manage an extent for each globe the polyline's associated with.
    protected static class ExtentInfo
    {
        // The extent depends on the state of the globe used to compute it, and the vertical exaggeration.
        protected Extent extent;
        protected double verticalExaggeration;
        protected Globe globe;
        protected Object globeStateKey;

        public ExtentInfo(Extent extent, DrawContext dc)
        {
            this.extent = extent;
            this.verticalExaggeration = dc.getVerticalExaggeration();
            this.globe = dc.getGlobe();
            this.globeStateKey = dc.getGlobe().getStateKey(dc);
        }

        protected boolean isValid(DrawContext dc)
        {
            return this.verticalExaggeration == dc.getVerticalExaggeration() && this.globe == dc.getGlobe()
                && globeStateKey.equals(dc.getGlobe().getStateKey(dc));
        }
    }

    protected HashMap<Globe, ExtentInfo> extents = new HashMap<Globe, ExtentInfo>(2); // usually only 1, but few at most

    public Polyline()
    {
        this.setPositions(null);
        this.measurer.setFollowTerrain(this.followTerrain);
        this.measurer.setPathType(this.pathType);
    }

    public Polyline(Iterable<? extends Position> positions)
    {
        this.setPositions(positions);
        this.measurer.setFollowTerrain(this.followTerrain);
        this.measurer.setPathType(this.pathType);
    }

    public Polyline(Iterable<? extends LatLon> positions, double elevation)
    {
        this.setPositions(positions, elevation);
        this.measurer.setFollowTerrain(this.followTerrain);
        this.measurer.setPathType(this.pathType);
    }

    private void reset()
    {
        if (this.currentSpans != null)
            this.currentSpans.clear();
        this.currentSpans = null;
    }

    public Color getColor()
    {
        return color;
    }

    public void setColor(Color color)
    {
        if (color == null)
        {
            String msg = Logging.getMessage("nullValue.ColorIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        this.color = color;
    }

    public int getAntiAliasHint()
    {
        return antiAliasHint;
    }

    public void setAntiAliasHint(int hint)
    {
        if (!(hint == ANTIALIAS_DONT_CARE || hint == ANTIALIAS_FASTEST || hint == ANTIALIAS_NICEST))
        {
            String msg = Logging.getMessage("generic.InvalidHint");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        this.antiAliasHint = hint;
    }

    public boolean isFilled()
    {
        return filled;
    }

    public void setFilled(boolean filled)
    {
        this.filled = filled;
    }

    public int getPathType()
    {
        return pathType;
    }

    public String getPathTypeString()
    {
        return this.getPathType() == GREAT_CIRCLE ? AVKey.GREAT_CIRCLE
            : this.getPathType() == RHUMB_LINE ? AVKey.RHUMB_LINE : AVKey.LINEAR;
    }

    /**
     * Sets the type of path to draw, one of {@link #GREAT_CIRCLE}, which draws each segment of the path as a great
     * circle, {@link #LINEAR}, which determines the intermediate positions between segments by interpolating the
     * segment endpoints, or {@link #RHUMB_LINE}, which draws each segment of the path as a line of constant heading.
     *
     * @param pathType the type of path to draw.
     */
    public void setPathType(int pathType)
    {
        this.reset();
        this.pathType = pathType;
        this.measurer.setPathType(pathType);
    }

    public void setPathType(String pathType)
    {
        if (pathType == null)
        {
            String msg = Logging.getMessage("nullValue.PathTypeIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        this.setPathType(pathType.equals(AVKey.GREAT_CIRCLE) ? GREAT_CIRCLE
            : pathType.equals(AVKey.RHUMB_LINE) ? RHUMB_LINE : LINEAR);
    }

    public boolean isFollowTerrain()
    {
        return followTerrain;
    }

    /**
     * Indicates whether the path should follow the terrain's surface. If the value is <code>true</code>, the elevation
     * values in this path's positions are ignored and the path is drawn on the terrain surface. Otherwise the path is
     * drawn according to the elevations given in the path's positions. If following the terrain, the path may also have
     * an offset. See {@link #setOffset(double)};
     *
     * @param followTerrain <code>true</code> to follow the terrain, otherwise <code>false</code>.
     */
    public void setFollowTerrain(boolean followTerrain)
    {
        this.reset();
        this.followTerrain = followTerrain;
        this.measurer.setFollowTerrain(followTerrain);
        this.extents.clear();
    }

    public double getOffset()
    {
        return offset;
    }

    /**
     * Specifies an offset, in meters, to add to the path points when the path's follow-terrain attribute is true. See
     * {@link #setFollowTerrain(boolean)}.
     *
     * @param offset the path pffset in meters.
     */
    public void setOffset(double offset)
    {
        this.reset();
        this.offset = offset;
        this.extents.clear();
    }

    public double getTerrainConformance()
    {
        return terrainConformance;
    }

    /**
     * Specifies the precision to which the path follows the terrain when the follow-terrain attribute is true. The
     * conformance value indicates the approximate length of each sub-segment of the path as it's drawn, in pixels.
     * Lower values specify higher precision, but at the cost of performance.
     *
     * @param terrainConformance the path conformance in pixels.
     */
    public void setTerrainConformance(double terrainConformance)
    {
        this.terrainConformance = terrainConformance;
    }

    public double getLineWidth()
    {
        return lineWidth;
    }

    public void setLineWidth(double lineWidth)
    {
        this.lineWidth = lineWidth;
    }

    /**
     * Returns the length of the line as drawn. If the path follows the terrain, the length returned is the distance one
     * would travel if on the surface. If the path does not follow the terrain, the length returned is the distance
     * along the full length of the path at the path's elevations and current path type.
     *
     * @return the path's length in meters.
     */
    public double getLength()
    {
        Iterator<ExtentInfo> infos = this.extents.values().iterator();
        return infos.hasNext() ? this.measurer.getLength(infos.next().globe) : 0;
    }

    public double getLength(Globe globe)
    {
        // The length measurer will throw an exception and log the error if globe is null
        return this.measurer.getLength(globe);
    }

    public LengthMeasurer getMeasurer()
    {
        return this.measurer;
    }

    public short getStipplePattern()
    {
        return stipplePattern;
    }

    /**
     * Sets the stipple pattern for specifying line types other than solid. See the OpenGL specification or programming
     * guides for a description of this parameter. Stipple is also affected by the path's stipple factor, {@link
     * #setStippleFactor(int)}.
     *
     * @param stipplePattern the stipple pattern.
     */
    public void setStipplePattern(short stipplePattern)
    {
        this.stipplePattern = stipplePattern;
    }

    public int getStippleFactor()
    {
        return stippleFactor;
    }

    /**
     * Sets the stipple factor for specifying line types other than solid. See the OpenGL specification or programming
     * guides for a description of this parameter. Stipple is also affected by the path's stipple pattern, {@link
     * #setStipplePattern(short)}.
     *
     * @param stippleFactor the stipple factor.
     */
    public void setStippleFactor(int stippleFactor)
    {
        this.stippleFactor = stippleFactor;
    }

    public int getNumSubsegments()
    {
        return numSubsegments;
    }

    /**
     * Specifies the number of intermediate segments to draw for each segment between positions. The end points of the
     * intermediate segments are calculated according to the current path type and follow-terrain setting.
     *
     * @param numSubsegments the number of intermediate subsegments.
     */
    public void setNumSubsegments(int numSubsegments)
    {
        this.reset();
        this.numSubsegments = numSubsegments;
    }

    public boolean isHighlighted()
    {
        return highlighted;
    }

    public void setHighlighted(boolean highlighted)
    {
        this.highlighted = highlighted;
    }

    public Color getHighlightColor()
    {
        return this.highlightColor;
    }

    public void setHighlightColor(Color highlightColor)
    {
        if (highlightColor == null)
        {
            String message = Logging.getMessage("nullValue.ColorIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        this.highlightColor = highlightColor;
    }

    /**
     * Specifies the path's positions.
     *
     * @param inPositions the path positions.
     */
    public void setPositions(Iterable<? extends Position> inPositions)
    {
        this.reset();
        this.positions = new ArrayList<Position>();
        this.extents.clear();
        if (inPositions != null)
        {
            for (Position position : inPositions)
            {
                this.positions.add(position);
            }
            this.measurer.setPositions(this.positions);
        }

        if ((this.filled && this.positions.size() < 3))
        {
            String msg = Logging.getMessage("generic.InsufficientPositions");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
    }

    /**
     * Sets the paths positions as latitude and longitude values at a constant altitude.
     *
     * @param inPositions the latitudes and longitudes of the positions.
     * @param elevation   the elevation to assign each position.
     */
    public void setPositions(Iterable<? extends LatLon> inPositions, double elevation)
    {
        this.reset();
        this.positions = new ArrayList<Position>();
        this.extents.clear();
        if (inPositions != null)
        {
            for (LatLon position : inPositions)
            {
                this.positions.add(new Position(position, elevation));
            }
            this.measurer.setPositions(this.positions);
        }

        if (this.filled && this.positions.size() < 3)
        {
            String msg = Logging.getMessage("generic.InsufficientPositions");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
    }

    public Iterable<Position> getPositions()
    {
        return this.positions;
    }

    public boolean isClosed()
    {
        return closed;
    }

    public void setClosed(boolean closed)
    {
        this.closed = closed;
    }
//
//    protected Extent getExtent(DrawContext dc)
//    {
//        double ve = dc.getVerticalExaggeration();
//        if (this.extent == null || ve != this.extentVerticalExaggeration)
//        {
//            Sector sector = Sector.boundingSector(this.getPositions());
//            double[] minAndMaxElevations;
//            if (this.isFollowTerrain())
//            {
//                minAndMaxElevations = dc.getGlobe().getMinAndMaxElevations(sector);
//            }
//            else
//            {
//                minAndMaxElevations = computeElevationExtremes(this.getPositions());
//            }
//            minAndMaxElevations[0] += this.getOffset();
//            minAndMaxElevations[1] += this.getOffset();
//            this.extent = dc.getGlobe().computeBoundingCylinder(ve, sector, minAndMaxElevations[0] * ve,
//                minAndMaxElevations[1] * ve);
//            this.extentVerticalExaggeration = ve;
//        }
//
//        return this.extent;
//    }

    /**
     * Returns this Polyline's enclosing volume as an {@link gov.nasa.worldwind.geom.Extent} in model coordinates, given
     * a specified {@link gov.nasa.worldwind.globes.Globe} and vertical exaggeration (see {@link
     * gov.nasa.worldwind.SceneController#getVerticalExaggeration()}.
     *
     * @param globe                the Globe this Polyline is related to.
     * @param verticalExaggeration the vertical exaggeration of the scene containing this Polyline.
     *
     * @return this Polyline's Extent in model coordinates.
     *
     * @throws IllegalArgumentException if the Globe is null.
     */
    public Extent getExtent(Globe globe, double verticalExaggeration)
    {
        if (globe == null)
        {
            String message = Logging.getMessage("nullValue.GlobeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return this.computeExtent(globe, verticalExaggeration);
    }

    /**
     * Returns this Polyline's enclosing volume as an {@link gov.nasa.worldwind.geom.Extent} in model coordinates, given
     * a specified {@link gov.nasa.worldwind.render.DrawContext}. The returned Extent may be different than the Extent
     * returned by calling {@link #getExtent(gov.nasa.worldwind.globes.Globe, double)} with the DrawContext's Globe and
     * vertical exaggeration. Additionally, this may cache the computed extent and is therefore potentially faster than
     * calling {@link #getExtent(gov.nasa.worldwind.globes.Globe, double)} with the same DrawContext.
     *
     * @param dc the current DrawContext.
     *
     * @return this Polyline's Extent in model coordinates.
     *
     * @throws IllegalArgumentException if the DrawContext is null, or if the Globe held by the DrawContext is null.
     */
    public Extent getExtent(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (dc.getGlobe() == null)
        {
            String message = Logging.getMessage("nullValue.DrawingContextGlobeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        ExtentInfo extentInfo = this.extents.get(dc.getGlobe());
        if (extentInfo != null && extentInfo.isValid(dc))
        {
            return extentInfo.extent;
        }
        else
        {
            extentInfo = new ExtentInfo(this.computeExtent(dc), dc);
            this.extents.put(dc.getGlobe(), extentInfo);
            return extentInfo.extent;
        }
    }

    protected Extent computeExtent(Globe globe, double verticalExaggeration)
    {
        Sector sector = Sector.boundingSector(this.getPositions());

        double[] minAndMaxElevations;
        if (this.isFollowTerrain())
        {
            minAndMaxElevations = globe.getMinAndMaxElevations(sector);
        }
        else
        {
            minAndMaxElevations = computeElevationExtremes(this.getPositions());
        }
        minAndMaxElevations[0] += this.getOffset();
        minAndMaxElevations[1] += this.getOffset();

        return Sector.computeBoundingBox(globe, verticalExaggeration, sector, minAndMaxElevations[0],
            minAndMaxElevations[1]);
    }

    protected Extent computeExtent(DrawContext dc)
    {
        return this.computeExtent(dc.getGlobe(), dc.getVerticalExaggeration());
    }

    protected static double[] computeElevationExtremes(Iterable<? extends Position> positions)
    {
        double[] extremes = new double[] {Double.MAX_VALUE, -Double.MAX_VALUE};
        for (Position pos : positions)
        {
            if (extremes[0] > pos.getElevation())
                extremes[0] = pos.getElevation(); // min
            if (extremes[1] < pos.getElevation())
                extremes[1] = pos.getElevation(); // max
        }

        return extremes;
    }

    public void render(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        if (this.positions.size() < 2)
            return;

        // vertices potentially computed every frame to follow terrain changes
        if (this.currentSpans == null || (this.followTerrain && this.geomGenTimeStamp != dc.getFrameTimeStamp())
            || this.geomGenVE != dc.getVerticalExaggeration())
        {
            // Reference center must be computed prior to computing vertices.
            this.computeReferenceCenter(dc);
            this.makeVertices(dc);
            this.geomGenTimeStamp = dc.getFrameTimeStamp();
            this.geomGenVE = dc.getVerticalExaggeration();
        }

        if (this.currentSpans == null || this.currentSpans.size() < 1)
            return;

        // Because getLength() requires a globe and globes are captured in the extents list, the extent always has
        // to be computed even though it's needed only during pick-frustum testing.
        Extent extent = this.getExtent(dc);
        if (dc.isPickingMode() && !dc.getPickFrustums().intersectsAny(extent))
            return;

        GL gl = dc.getGL();

        int attrBits = GL.GL_HINT_BIT | GL.GL_CURRENT_BIT | GL.GL_LINE_BIT;
        if (!dc.isPickingMode())
        {
            if (this.color.getAlpha() != 255)
                attrBits |= GL.GL_COLOR_BUFFER_BIT;
        }

        gl.glPushAttrib(attrBits);
        dc.getView().pushReferenceCenter(dc, this.referenceCenterPoint);

        boolean projectionOffsetPushed = false; // keep track for error recovery

        try
        {
            if (!dc.isPickingMode())
            {
                if (this.color.getAlpha() != 255)
                {
                    gl.glEnable(GL.GL_BLEND);
                    gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
                }
                dc.getGL().glColor4ub((byte) this.color.getRed(), (byte) this.color.getGreen(),
                    (byte) this.color.getBlue(), (byte) this.color.getAlpha());
            }

            if (this.stippleFactor > 0)
            {
                gl.glEnable(GL.GL_LINE_STIPPLE);
                gl.glLineStipple(this.stippleFactor, this.stipplePattern);
            }
            else
            {
                gl.glDisable(GL.GL_LINE_STIPPLE);
            }

            int hintAttr = GL.GL_LINE_SMOOTH_HINT;
            if (this.filled)
                hintAttr = GL.GL_POLYGON_SMOOTH_HINT;
            gl.glHint(hintAttr, this.antiAliasHint);

            int primType = GL.GL_LINE_STRIP;
            if (this.filled)
                primType = GL.GL_POLYGON;

            if (dc.isPickingMode())
                gl.glLineWidth((float) this.lineWidth + 8);
            else
                gl.glLineWidth((float) this.lineWidth);

            if (this.followTerrain)
            {
                dc.pushProjectionOffest(0.99);
                projectionOffsetPushed = true;
            }

            for (ArrayList<Vec4> span : this.currentSpans)
            {
                if (span == null)
                    continue;

                // Since segements can very often be very short -- two vertices -- use explicit rendering. The
                // overhead of batched rendering, e.g., gl.glDrawArrays, is too high because it requires copying
                // the vertices into a DoubleBuffer, and DoubleBuffer creation and access performs relatively poorly.
                gl.glBegin(primType);
                for (Vec4 p : span)
                {
                    gl.glVertex3d(p.x, p.y, p.z);
                }
                gl.glEnd();
            }

            if (this.highlighted)
            {
                if (!dc.isPickingMode())
                {
                    if (this.highlightColor.getAlpha() != 255)
                    {
                        gl.glEnable(GL.GL_BLEND);
                        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
                    }
                    dc.getGL().glColor4ub((byte) this.highlightColor.getRed(), (byte) this.highlightColor.getGreen(),
                        (byte) this.highlightColor.getBlue(), (byte) this.highlightColor.getAlpha());

                    gl.glLineWidth((float) this.lineWidth + 2);
                    for (ArrayList<Vec4> span : this.currentSpans)
                    {
                        if (span == null)
                            continue;

                        gl.glBegin(primType);
                        for (Vec4 p : span)
                        {
                            gl.glVertex3d(p.x, p.y, p.z);
                        }
                        gl.glEnd();
                    }
                }
            }
        }
        finally
        {
            if (projectionOffsetPushed)
                dc.popProjectionOffest();

            gl.glPopAttrib();
            dc.getView().popReferenceCenter(dc);
        }
    }

    //private void pushOffest(DrawContext dc)
    //{
    //    // Modify the projection transform to shift the depth values slightly toward the camera in order to
    //    // ensure the lines are selected during depth buffering.
    //    GL gl = dc.getGL();
    //
    //    float[] pm = new float[16];
    //    gl.glGetFloatv(GL.GL_PROJECTION_MATRIX, pm, 0);
    //    pm[10] *= 0.99; // TODO: See Lengyel 2 ed. Section 9.1.2 to compute optimal/minimal offset
    //
    //    gl.glPushAttrib(GL.GL_TRANSFORM_BIT);
    //    gl.glMatrixMode(GL.GL_PROJECTION);
    //    gl.glPushMatrix();
    //    gl.glLoadMatrixf(pm, 0);
    //}
    //
    //private void popOffest(DrawContext dc)
    //{
    //    GL gl = dc.getGL();
    //    gl.glMatrixMode(GL.GL_PROJECTION);
    //    gl.glPopMatrix();
    //    gl.glPopAttrib();
    //}

    protected void makeVertices(DrawContext dc)
    {
        if (this.currentSpans == null)
            this.currentSpans = new ArrayList<ArrayList<Vec4>>();
        else
            this.currentSpans.clear();

        if (this.positions.size() < 1)
            return;

        Position posA = this.positions.get(0);
        Vec4 ptA = this.computePoint(dc, posA, true);
        for (int i = 1; i <= this.positions.size(); i++)
        {
            Position posB;
            if (i < this.positions.size())
                posB = this.positions.get(i);
            else if (this.closed)
                posB = this.positions.get(0);
            else
                break;

            Vec4 ptB = this.computePoint(dc, posB, true);

            if (this.followTerrain && !this.isSegmentVisible(dc, posA, posB, ptA, ptB))
            {
                posA = posB;
                ptA = ptB;
                continue;
            }

            ArrayList<Vec4> span;
            span = this.makeSegment(dc, posA, posB, ptA, ptB);

            if (span != null)
                this.addSpan(span);

            posA = posB;
            ptA = ptB;
        }
    }

    protected void addSpan(ArrayList<Vec4> span)
    {
        if (span != null && span.size() > 0)
            this.currentSpans.add(span);
    }

    protected boolean isSegmentVisible(DrawContext dc, Position posA, Position posB, Vec4 ptA, Vec4 ptB)
    {
        Frustum f = dc.getView().getFrustumInModelCoordinates();

        if (f.contains(ptA))
            return true;

        if (f.contains(ptB))
            return true;

        if (ptA.equals(ptB))
            return false;

        Position posC = Position.interpolate(0.5, posA, posB);
        Vec4 ptC = this.computePoint(dc, posC, true);
        if (f.contains(ptC))
            return true;

        double r = Line.distanceToSegment(ptA, ptB, ptC);
        Cylinder cyl = new Cylinder(ptA, ptB, r == 0 ? 1 : r);
        return cyl.intersects(dc.getView().getFrustumInModelCoordinates());
    }

    protected Vec4 computePoint(DrawContext dc, Position pos, boolean applyOffset)
    {
        if (this.followTerrain)
        {
            double height = !applyOffset ? 0 : this.offset;
            // computeTerrainPoint will apply vertical exaggeration
            return this.computeTerrainPoint(dc, pos.getLatitude(), pos.getLongitude(), height);
        }
        else
        {
            double height = pos.getElevation() + (applyOffset ? this.offset : 0);
            return dc.getGlobe().computePointFromPosition(pos.getLatitude(), pos.getLongitude(),
                height * dc.getVerticalExaggeration());
        }
    }

    protected double computeSegmentLength(DrawContext dc, Position posA, Position posB)
    {
        LatLon llA = new LatLon(posA.getLatitude(), posA.getLongitude());
        LatLon llB = new LatLon(posB.getLatitude(), posB.getLongitude());

        Angle ang = LatLon.greatCircleDistance(llA, llB);

        if (this.followTerrain)
        {
            return ang.radians * (dc.getGlobe().getRadius() + this.offset * dc.getVerticalExaggeration());
        }
        else
        {
            double height = this.offset + 0.5 * (posA.getElevation() + posB.getElevation());
            return ang.radians * (dc.getGlobe().getRadius() + height * dc.getVerticalExaggeration());
        }
    }

    protected ArrayList<Vec4> makeSegment(DrawContext dc, Position posA, Position posB, Vec4 ptA, Vec4 ptB)
    {
        ArrayList<Vec4> span = null;

        double arcLength = this.computeSegmentLength(dc, posA, posB);
        if (arcLength <= 0) // points differing only in altitude
        {
            span = this.addPointToSpan(ptA, span);
            if (!ptA.equals(ptB))
                span = this.addPointToSpan(ptB, span);
            return span;
        }
        // Variables for great circle and rhumb computation.
        Angle segmentAzimuth = null;
        Angle segmentDistance = null;

        for (double s = 0, p = 0; s < 1;)
        {
            if (this.followTerrain)
                p += this.terrainConformance * dc.getView().computePixelSizeAtDistance(
                    ptA.distanceTo3(dc.getView().getEyePoint()));
            else
                p += arcLength / this.numSubsegments;

            s = p / arcLength;

            Position pos;
            if (s >= 1)
            {
                pos = posB;
            }
            else if (this.pathType == LINEAR)
            {
                pos = Position.interpolate(s, posA, posB);
            }
            else if (this.pathType == RHUMB_LINE) // or LOXODROME
            {
                if (segmentAzimuth == null)
                {
                    segmentAzimuth = LatLon.rhumbAzimuth(posA, posB);
                    segmentDistance = LatLon.rhumbDistance(posA, posB);
                }
                Angle distance = Angle.fromRadians(s * segmentDistance.radians);
                LatLon latLon = LatLon.rhumbEndPosition(posA, segmentAzimuth, distance);
                pos = new Position(latLon, (1 - s) * posA.getElevation() + s * posB.getElevation());
            }
            else // GREAT_CIRCLE
            {
                if (segmentAzimuth == null)
                {
                    segmentAzimuth = LatLon.greatCircleAzimuth(posA, posB);
                    segmentDistance = LatLon.greatCircleDistance(posA, posB);
                }
                Angle distance = Angle.fromRadians(s * segmentDistance.radians);
                LatLon latLon = LatLon.greatCircleEndPosition(posA, segmentAzimuth, distance);
                pos = new Position(latLon, (1 - s) * posA.getElevation() + s * posB.getElevation());
            }

            ptB = this.computePoint(dc, pos, true);
            span = this.clipAndAdd(dc, ptA, ptB, span);

            ptA = ptB;
        }

        return span;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    protected ArrayList<Vec4> clipAndAdd(DrawContext dc, Vec4 ptA, Vec4 ptB, ArrayList<Vec4> span)
    {
        // Line clipping appears to be useful only for long lines with few segments. It's costly otherwise.
        // TODO: Investigate trade-off of line clipping.
//        if (Line.clipToFrustum(ptA, ptB, dc.getView().getFrustumInModelCoordinates()) == null)
//        {
//            if (span != null)
//            {
//                this.addSpan(span);
//                span = null;
//            }
//            return span;
//        }

        if (span == null)
            span = this.addPointToSpan(ptA, span);

        return this.addPointToSpan(ptB, span);
    }

    protected ArrayList<Vec4> addPointToSpan(Vec4 p, ArrayList<Vec4> span)
    {
        if (span == null)
            span = new ArrayList<Vec4>();

        span.add(p.subtract3(this.referenceCenterPoint));

        return span;
    }

    protected void computeReferenceCenter(DrawContext dc)
    {
        // The reference position is null if this Polyline has no positions. In this case computing the Polyline's
        // Cartesian reference point is meaningless because the Polyline has no geographic location. Therefore we exit
        // without updating the reference point.
        Position refPos = this.getReferencePosition();
        if (refPos == null)
            return;

        this.referenceCenterPoint = this.computeTerrainPoint(dc, refPos.getLatitude(), refPos.getLongitude(),
            this.offset);
    }

    public Position getReferencePosition()
    {
        if (this.positions.size() < 1)
        {
            return null;
        }
        else if (this.positions.size() < 3)
        {
            return this.positions.get(0);
        }
        else
        {
            return this.positions.get(this.positions.size() / 2);
        }
    }

    protected Vec4 computeTerrainPoint(DrawContext dc, Angle lat, Angle lon, double offset)
    {
        Vec4 p = dc.getSurfaceGeometry().getSurfacePoint(lat, lon, offset * dc.getVerticalExaggeration());

        if (p == null)
        {
            p = dc.getGlobe().computePointFromPosition(lat, lon,
                (offset + dc.getGlobe().getElevation(lat, lon)) * dc.getVerticalExaggeration());
        }

        return p;
    }

    public void move(Position delta)
    {
        if (delta == null)
        {
            String msg = Logging.getMessage("nullValue.PositionIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        Position refPos = this.getReferencePosition();

        // The reference position is null if this Polyline has no positions. In this case moving the Polyline by a
        // relative delta is meaningless because the Polyline has no geographic location. Therefore we fail softly by
        // exiting and doing nothing.
        if (refPos == null)
            return;

        this.moveTo(refPos.add(delta));
    }

    public void moveTo(Position position)
    {
        if (position == null)
        {
            String msg = Logging.getMessage("nullValue.PositionIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        this.reset();
        this.extents.clear();

        Position oldRef = this.getReferencePosition();

        // The reference position is null if this Polyline has no positions. In this case moving the Polyline to a new
        // reference position is meaningless because the Polyline has no geographic location. Therefore we fail softly
        // by exiting and doing nothing.
        if (oldRef == null)
            return;

        double elevDelta = position.getElevation() - oldRef.getElevation();

        for (int i = 0; i < this.positions.size(); i++)
        {
            Position pos = this.positions.get(i);

            Angle distance = LatLon.greatCircleDistance(oldRef, pos);
            Angle azimuth = LatLon.greatCircleAzimuth(oldRef, pos);
            LatLon newLocation = LatLon.greatCircleEndPosition(position, azimuth, distance);
            double newElev = pos.getElevation() + elevDelta;

            this.positions.set(i, new Position(newLocation, newElev));
        }
    }

    /**
     * Returns an XML state document String describing the public attributes of this Polyline.
     *
     * @return XML state document string describing this Polyline.
     */
    public String getRestorableState()
    {
        RestorableSupport restorableSupport = RestorableSupport.newRestorableSupport();
        // Creating a new RestorableSupport failed. RestorableSupport logged the problem, so just return null.
        if (restorableSupport == null)
            return null;

        if (this.color != null)
        {
            String encodedColor = RestorableSupport.encodeColor(this.color);
            if (encodedColor != null)
                restorableSupport.addStateValueAsString("color", encodedColor);
        }

        if (this.highlightColor != null)
        {
            String encodedColor = RestorableSupport.encodeColor(this.highlightColor);
            if (encodedColor != null)
                restorableSupport.addStateValueAsString("highlightColor", encodedColor);
        }

        if (this.positions != null)
        {
            // Create the base "positions" state object.
            RestorableSupport.StateObject positionsStateObj = restorableSupport.addStateObject("positions");
            if (positionsStateObj != null)
            {
                for (Position p : this.positions)
                {
                    // Save each position only if all parts (latitude, longitude, and elevation) can be
                    // saved. We will not save a partial iconPosition (for example, just the elevation).
                    if (p != null && p.getLatitude() != null && p.getLongitude() != null)
                    {
                        // Create a nested "position" element underneath the base "positions".
                        RestorableSupport.StateObject pStateObj =
                            restorableSupport.addStateObject(positionsStateObj, "position");
                        if (pStateObj != null)
                        {
                            restorableSupport.addStateValueAsDouble(pStateObj, "latitudeDegrees",
                                p.getLatitude().degrees);
                            restorableSupport.addStateValueAsDouble(pStateObj, "longitudeDegrees",
                                p.getLongitude().degrees);
                            restorableSupport.addStateValueAsDouble(pStateObj, "elevation",
                                p.getElevation());
                        }
                    }
                }
            }
        }

        restorableSupport.addStateValueAsInteger("antiAliasHint", this.antiAliasHint);
        restorableSupport.addStateValueAsBoolean("filled", this.filled);
        restorableSupport.addStateValueAsBoolean("closed", this.closed);
        restorableSupport.addStateValueAsBoolean("highlighted", this.highlighted);
        restorableSupport.addStateValueAsInteger("pathType", this.pathType);
        restorableSupport.addStateValueAsBoolean("followTerrain", this.followTerrain);
        restorableSupport.addStateValueAsDouble("offset", this.offset);
        restorableSupport.addStateValueAsDouble("terrainConformance", this.terrainConformance);
        restorableSupport.addStateValueAsDouble("lineWidth", this.lineWidth);
        restorableSupport.addStateValueAsInteger("stipplePattern", this.stipplePattern);
        restorableSupport.addStateValueAsInteger("stippleFactor", this.stippleFactor);
        restorableSupport.addStateValueAsInteger("numSubsegments", this.numSubsegments);

        RestorableSupport.StateObject so = restorableSupport.addStateObject(null, "avlist");
        for (Map.Entry<String, Object> avp : this.getEntries())
        {
            restorableSupport.addStateValueAsString(so, avp.getKey(),
                avp.getValue() != null ? avp.getValue().toString() : "");
        }

        return restorableSupport.getStateAsXml();
    }

    /**
     * Restores publicly settable attribute values found in the specified XML state document String. The document
     * specified by <code>stateInXml</code> must be a well formed XML document String, or this will throw an
     * IllegalArgumentException. Unknown structures in <code>stateInXml</code> are benign, because they will simply be
     * ignored.
     *
     * @param stateInXml an XML document String describing a Polyline.
     *
     * @throws IllegalArgumentException If <code>stateInXml</code> is null, or if <code>stateInXml</code> is not a well
     *                                  formed XML document String.
     */
    public void restoreState(String stateInXml)
    {
        if (stateInXml == null)
        {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        RestorableSupport restorableSupport;
        try
        {
            restorableSupport = RestorableSupport.parse(stateInXml);
        }
        catch (Exception e)
        {
            // Parsing the document specified by stateInXml failed.
            String message = Logging.getMessage("generic.ExceptionAttemptingToParseStateXml", stateInXml);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message, e);
        }

        String colorState = restorableSupport.getStateValueAsString("color");
        if (colorState != null)
        {
            Color color = RestorableSupport.decodeColor(colorState);
            if (color != null)
                setColor(color);
        }

        colorState = restorableSupport.getStateValueAsString("highlightColor");
        if (colorState != null)
        {
            Color color = RestorableSupport.decodeColor(colorState);
            if (color != null)
                setHighlightColor(color);
        }

        // Get the base "positions" state object.
        RestorableSupport.StateObject positionsStateObj = restorableSupport.getStateObject("positions");
        if (positionsStateObj != null)
        {
            ArrayList<Position> newPositions = new ArrayList<Position>();
            // Get the nested "position" states beneath the base "positions".
            RestorableSupport.StateObject[] positionStateArray =
                restorableSupport.getAllStateObjects(positionsStateObj, "position");
            if (positionStateArray != null && positionStateArray.length != 0)
            {
                for (RestorableSupport.StateObject pStateObj : positionStateArray)
                {
                    if (pStateObj != null)
                    {
                        // Restore each position only if all parts are available.
                        // We will not restore a partial position (for example, just the elevation).
                        Double latitudeState = restorableSupport.getStateValueAsDouble(pStateObj, "latitudeDegrees");
                        Double longitudeState = restorableSupport.getStateValueAsDouble(pStateObj, "longitudeDegrees");
                        Double elevationState = restorableSupport.getStateValueAsDouble(pStateObj, "elevation");
                        if (latitudeState != null && longitudeState != null && elevationState != null)
                            newPositions.add(Position.fromDegrees(latitudeState, longitudeState, elevationState));
                    }
                }
            }

            // Even if there are no actual positions specified, we set positions as an empty list.
            // An empty set of positions is still a valid state.
            setPositions(newPositions);
        }

        Integer antiAliasHintState = restorableSupport.getStateValueAsInteger("antiAliasHint");
        if (antiAliasHintState != null)
            setAntiAliasHint(antiAliasHintState);

        Boolean isFilledState = restorableSupport.getStateValueAsBoolean("filled");
        if (isFilledState != null)
            setFilled(isFilledState);

        Boolean isClosedState = restorableSupport.getStateValueAsBoolean("closed");
        if (isClosedState != null)
            setClosed(isClosedState);

        Boolean isHighlightedState = restorableSupport.getStateValueAsBoolean("highlighted");
        if (isHighlightedState != null)
            setHighlighted(isHighlightedState);

        Integer pathTypeState = restorableSupport.getStateValueAsInteger("pathType");
        if (pathTypeState != null)
            setPathType(pathTypeState);

        Boolean isFollowTerrainState = restorableSupport.getStateValueAsBoolean("followTerrain");
        if (isFollowTerrainState != null)
            setFollowTerrain(isFollowTerrainState);

        Double offsetState = restorableSupport.getStateValueAsDouble("offset");
        if (offsetState != null)
            setOffset(offsetState);

        Double terrainConformanceState = restorableSupport.getStateValueAsDouble("terrainConformance");
        if (terrainConformanceState != null)
            setTerrainConformance(terrainConformanceState);

        Double lineWidthState = restorableSupport.getStateValueAsDouble("lineWidth");
        if (lineWidthState != null)
            setLineWidth(lineWidthState);

        Integer stipplePatternState = restorableSupport.getStateValueAsInteger("stipplePattern");
        if (stipplePatternState != null)
            setStipplePattern(stipplePatternState.shortValue());

        Integer stippleFactorState = restorableSupport.getStateValueAsInteger("stippleFactor");
        if (stippleFactorState != null)
            setStippleFactor(stippleFactorState);

        Integer numSubsegmentsState = restorableSupport.getStateValueAsInteger("numSubsegments");
        if (numSubsegmentsState != null)
            setNumSubsegments(numSubsegmentsState);

        RestorableSupport.StateObject so = restorableSupport.getStateObject(null, "avlist");
        if (so != null)
        {
            RestorableSupport.StateObject[] avpairs = restorableSupport.getAllStateObjects(so, "");
            if (avpairs != null)
            {
                for (RestorableSupport.StateObject avp : avpairs)
                {
                    if (avp != null)
                        this.setValue(avp.getName(), avp.getValue());
                }
            }
        }
    }
}
