/*
Copyright (C) 2001, 2008 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render;

import com.sun.opengl.util.*;
import gov.nasa.worldwind.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.*;
import gov.nasa.worldwind.terrain.*;
import gov.nasa.worldwind.util.*;

import javax.media.opengl.*;
import javax.media.opengl.glu.*;
import java.awt.*;
import java.nio.*;
import java.util.*;

/**
 * @author $Author$
 * @version $Id: SurfaceShapeGeometry.java 12522 2009-08-26 22:09:01Z tgaskins $
 */
public abstract class SurfaceShapeGeometry implements Renderable, Disposable, Movable {

    protected Globe globe;
    protected ArrayList<LatLon> positions = new ArrayList<LatLon>();
    private Sector sector;
    private Vector<Triangle> triangles;
    private Vector<Polygon> polygons = new Vector<Polygon>();
    private Polyline polyline;
    private Extent extent;
    private long lastFrameTime = 0;
    private boolean forceSurfaceIntersect = true;

    private Color interiorColor;
    private float[] interiorColorGL = new float[4];
    private boolean drawBorder = true;
    private boolean drawInterior = true;
    private boolean antiAlias = true;

    private DoubleBuffer buff;
    private int[] firsts;
    private int[] counts;

    private static long THROTTLE_RATE = 1000L;
    private static final double EPSILON = 1.e-10;
    private static final Color DEFAULT_COLOR = new Color(1f, 1f, 0f, 0.4f);
    private static final Color DEFAULT_BORDER_COLOR = new Color(1f, 1f, 0f, 0.7f);

    // symbolic indices into double[] arrays holding coordinates...
    private static final int X = 0;
    private static final int Y = 1;
    private static final int Z = 2;


    public SurfaceShapeGeometry(Iterable<? extends LatLon> positions, Color color, Color borderColor) {
        if (positions == null) {
            String message = Logging.getMessage("nullValue.PositionsListIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        setInteriorColor((color != null) ? color : DEFAULT_COLOR);

        this.polyline = new Polyline();
        this.polyline.setColor(borderColor != null ? borderColor : DEFAULT_BORDER_COLOR);
        this.polyline.setFollowTerrain(true);
        this.polyline.setPathType(Polyline.LINEAR);

        // Copy positions list.
        this.replacePositions(positions);
    }

    private void replacePositions(Iterable<? extends LatLon> newPositions) {
        this.positions.clear();
        for (LatLon position : newPositions) {
            this.positions.add(position);
        }
        this.polyline.setPositions(this.positions, 0.);
        resetBounds();
    }

    public void dispose() {
    }


    public ArrayList<Sector> getSectors() {
        ArrayList<Sector> sectors = new ArrayList<Sector>();
        sectors.add(this.sector);
        return sectors;
    }

    public Iterable<LatLon> getPositions() {
        return this.positions;
    }

    public void setPositions(Iterable<? extends LatLon> positions) {
        this.replacePositions(positions);
    }

    public Paint getInteriorColor() {
        return interiorColor;
    }

    public void setInteriorColor(Color color) {
        this.interiorColor = color;
        this.interiorColorGL[0] = this.interiorColor.getRed() / 255f;
        this.interiorColorGL[1] = this.interiorColor.getGreen() / 255f;
        this.interiorColorGL[2] = this.interiorColor.getBlue() / 255f;
        this.interiorColorGL[3] = this.interiorColor.getAlpha() / 255f;
    }

    public Color getBorderColor() {
        return this.polyline.getColor();
    }

    public void setBorderColor(Color borderColor) {
        this.polyline.setColor(borderColor);
    }

    public void setBorderWidth(double width) {
        this.polyline.setLineWidth(width);
    }

    public double getBorderWidth() {
        return this.polyline.getLineWidth();
    }

    public boolean isDrawBorder() {
        return drawBorder;
    }

    public void setDrawBorder(boolean drawBorder) {
        this.drawBorder = drawBorder;
    }

    public boolean isDrawInterior() {
        return drawInterior;
    }

    public void setDrawInterior(boolean drawInterior) {
        this.drawInterior = drawInterior;
    }

    public boolean isAntiAlias() {
        return antiAlias;
    }

    public void setAntiAlias(boolean antiAlias) {
        this.antiAlias = antiAlias;
    }

    /*
     * ***********************
     * public double getNumEdgeIntervalsPerDegree() {
     * return numEdgeIntervalsPerDegree;
     * }
     * <p/>
     * public void setNumEdgeIntervalsPerDegree(double numEdgeIntervals) {
     * this.numEdgeIntervalsPerDegree = numEdgeIntervals;
     * this.clearTextureData();
     * }
     * *********************
     */

    public double getPerimeter()
    {
        return this.polyline.getLength();
    }

    public double getArea() throws IllegalStateException {
        if (this.polygons == null || this.polygons.size() == 0 || this.globe == null) {
            // TODO:  I8N this...
            throw new IllegalStateException("NPE in getArea");
        }

        double area = 0.;
        double[] PQ = new double[3];
        double[] PR = new double[3];
        for (Polygon p : this.polygons) {
            // We have 3,4,5,6 sided, convex polygons. Treat them like a triangle-fan, and compute the area
            // of the individual triangles using the cross-product method.
            for (int i = 2; i < p.numVerts; i++) {
                // The convention here is that we have a triangle with vertices P, Q, and R.
                // Compute the cross-product of the vectors PQ and PR...

                // NOTE: we are purposefully avoiding the use of Vec4 here to avoid having to allocate
                // so many instances.
                // Also note that we've previously stored the polygon coordinates with the ReferencePoint subtracted,
                // so we are not dealing with as huge of numbers in these calculations.
                PQ[0] = (p.xy[i - 1][X]) - (p.xy[0][X]);
                PQ[1] = (p.xy[i - 1][Y]) - (p.xy[0][Y]);
                PQ[2] = (p.xy[i - 1][Z]) - (p.xy[0][Z]);
                PR[0] = (p.xy[i][X]) - (p.xy[0][X]);
                PR[1] = (p.xy[i][Y]) - (p.xy[0][Y]);
                PR[2] = (p.xy[i][Z]) - (p.xy[0][Z]);

                // Compute cross product: PQ X PR.  Note that the crossY component should be negated for the actual
                // cross product. But we're just after the magnitude of the vector, and are going to "square-away"
                // the sign, so it doesn't matter.
                double crossX = PQ[Y] * PR[Z] - PR[Y] * PQ[Z];
                double crossY = PQ[X] * PR[Z] - PR[X] * PQ[Z];
                double crossZ = PQ[X] * PR[Y] - PR[X] * PQ[Y];
                area += 0.5 * Math.sqrt(crossX * crossX + crossY * crossY + crossZ * crossZ);
            }
        }

        return area;
    }

    public void render(DrawContext dc) {
        // Capture this right away, as several subsequent computations require it....
        this.globe = dc.getGlobe();

        Frustum f = dc.getView().getFrustumInModelCoordinates();
        if (!getExtent(dc).intersects(f))
            return;

        // Do we need to perform top-level tessellation?  THis is generally performed once, the first time around...
        if (this.triangles == null) {
            this.tessellate();
        }

        if ((System.currentTimeMillis() - this.lastFrameTime) > THROTTLE_RATE || this.forceSurfaceIntersect) {
            Iterable<SectorGeometry> geom = getIntersectingGeometryTiles(dc.getSurfaceGeometry());
            intersectSurfaceGeometry(geom);
            packagePolygonsForDrawing();
            this.lastFrameTime = System.currentTimeMillis();
            this.forceSurfaceIntersect = false;
        }
        renderPolygons(dc);
    }

    //
    // Package up the polygon data into a vertex array.
    //
    private void packagePolygonsForDrawing() {
        if (polygons.size() == 0)
            return;
        
        int buffSize = 0;
        for (Polygon p : this.polygons) {
            buffSize += p.numVerts;
        }

        this.buff = BufferUtil.newDoubleBuffer(buffSize*3);
        this.firsts = new int[polygons.size()];
        this.counts = new int[polygons.size()];
        buffSize = 0;
        int i = 0;
        for (Polygon p : this.polygons) {
            this.firsts[i] = buffSize;
            this.counts[i] = p.numVerts;
            for (int j=0; j<p.numVerts; j++) {
                this.buff.put(p.xy[j]);
            }
            buffSize += p.numVerts;
            i++;
        }
    }

    private void renderPolygons(DrawContext dc) {
        Vec4 refPoint = getReferencePoint();

        GL gl = dc.getGL();

        gl.glPushAttrib(GL.GL_COLOR_BUFFER_BIT | GL.GL_POLYGON_BIT | GL.GL_CURRENT_BIT);
        dc.getView().pushReferenceCenter(dc, refPoint);
        this.pushOffset(dc);

        try {
            if (!dc.isPickingMode()) {
                gl.glEnable(GL.GL_BLEND);
                gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
                gl.glColor4fv(this.interiorColorGL, 0);
            }


            if (this.drawInterior || dc.isPickingMode()) {
                gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_FILL);
                gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
                buff.rewind();
                gl.glVertexPointer(3, GL.GL_DOUBLE, 0, buff);
                gl.glMultiDrawArrays(GL.GL_TRIANGLE_FAN, this.firsts, 0, this.counts, 0, this.firsts.length);
                gl.glDisableClientState(GL.GL_VERTEX_ARRAY);
            }


            /** pre vertex-array style drawing.  Leave for now; not convinced VA's have helped enough here
             *  to warrant their memory overhead.
            if (this.drawInterior) {
                gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_FILL);
                for (Polygon p : polygons) {
                    gl.glBegin(GL.GL_TRIANGLE_FAN);
                    for (int i = 0; i < p.numVerts; i++) {
                        gl.glVertex3d(p.xy[i][X]-refPoint.x, p.xy[i][Y]-refPoint.y, p.xy[i][Z]-refPoint.z);
                    }
                    gl.glEnd();
                }
            }
             ***********/

            /******************
             // Draw the border...
             if (!dc.isPickingMode()) {
             gl.glColor4fv(this.borderColorGL, 0);
             }
             gl.glBegin(GL.GL_LINE_LOOP);
             for (LatLon p : positions) {
             elev = dc.getGlobe().getElevation(p.getLatitude(), p.getLongitude());
             Position pos = new Position(p, elev);
             Vec4 pnt = this.globe.computePointFromPosition(pos);
             pnt = pnt.subtract3(refCenterPoint);
             gl.glVertex3d(pnt.x, pnt.y, pnt.z);
             }
             gl.glEnd();
             ********************/

            // delegate to render the border...
            if (!dc.isPickingMode() && this.drawBorder)
                this.polyline.render(dc);
        }
        finally {
            this.popOffset(dc);
            dc.getView().popReferenceCenter(dc);
            gl.glPopAttrib();
        }
    }

    private void pushOffset(DrawContext dc) {
        // Modify the projection transform to shift the depth values slightly toward the camera in order to
        // ensure the lines are selected during depth buffering.
        GL gl = dc.getGL();

        float[] pm = new float[16];
        gl.glGetFloatv(GL.GL_PROJECTION_MATRIX, pm, 0);
        pm[10] *= 0.99; // TODO: See Lengyel 2 ed. Section 9.1.2 to compute optimal/minimal offset

        gl.glPushAttrib(GL.GL_TRANSFORM_BIT);
        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glPushMatrix();
        gl.glLoadMatrixf(pm, 0);
    }

    private void popOffset(DrawContext dc) {
        GL gl = dc.getGL();
        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glPopMatrix();
        gl.glPopAttrib();
    }

    //
    // Intersects the collection of triangles that make up this object with a set of  SectorGeometry
    // objects.
    //
    private void intersectSurfaceGeometry(Iterable<SectorGeometry> geom) {

        // clear existing polygon list...
        this.polygons.clear();

        if (geom == null)
            return;

        for (SectorGeometry g : geom) {
            RectangularTessellator.RectGeometry terrain = RectangularTessellator.getTerrainGeometry(g);
            for (Triangle t : this.triangles) {
                if (t.spansDateline)
                    splitAtDateline(t, terrain);
                else
                    intersectTerrain(t, terrain);
            }
        }
    }

    //
    // Computes the intersect of a Triangle with the given terrain grid. Output is a collection of
    // 3,4,5,6 sided polygons.
    //
    // This algorithm is modelled after a standard polygon scan-line conversion algorithm (see Foley and Van Dam),
    // except that rather than a boolean decision about whether a "pixel" is inside/outside, we need to compute
    // the exact intersect wherever terrain gridcells intersect the Triangle edges.
    //
    private void intersectTerrain(Triangle t, RectangularTessellator.RectGeometry terrain) {

        ActiveEdge e0 = new ActiveEdge(t.vertices[0], t.vertices[1]);
        ActiveEdge e1 = new ActiveEdge(t.vertices[1], t.vertices[2]);
        ActiveEdge e2 = new ActiveEdge(t.vertices[0], t.vertices[2]);

        ArrayList<ActiveEdge> edges = new ArrayList<ActiveEdge>(3);

        // Insert the edges into the list sorted by their p0-latitude. Don't insert an edge whose endpoints
        // have equal latitude; i.e,. one that runs horizontally.

        if (Math.abs(e0.deltaY) > EPSILON) {
            edges.add(e0);
        }
        if (Math.abs(e1.deltaY) > EPSILON) {
            if (e1.p0Lat < e0.p0Lat) {
                edges.add(0, e1);
            } else {
                edges.add(e1);
            }
        }
        if (Math.abs(e2.deltaY) > EPSILON) {
            if (e2.p0Lat < edges.get(0).p0Lat) {
                edges.add(0, e2);
            } else if (edges.size() == 2 && e2.p0Lat < edges.get(1).p0Lat) {
                edges.add(1, e2);
            } else {
                edges.add(e2);
            }
        }
        if (edges.size() < 2) { // degenerate triangle!
            return;
        }

        // Get the first two edges;  skip any edge that is completely below our terrain grid...
        Iterator<ActiveEdge> iter = edges.iterator();
        e0 = iter.next();
        e1 = iter.next();
        if (e0.p1Lat <= terrain.getMinLatitude()) {
            if (iter.hasNext()) {
                e0 = e1;
                e1 = iter.next();
            } else {
                return;
            }
        }
        if (e1.p1Lat <= terrain.getMinLatitude()) {
            if (iter.hasNext()) {
                e1 = iter.next();
            } else {
                return;
            }
        }

        double leftLon = e0.p0Lon;
        double rightLon = e1.p0Lon;
        double lowerLat = e0.p0Lat;
        int currRow = terrain.getRowAtLat(lowerLat);
        if (currRow >= terrain.getNumRows()) { // all edges above our terrain...
            return;
        }

        if (currRow < 0) {
            // one or both edges extend below our terrain grid
            currRow = 0;
            lowerLat = terrain.getLatAtRow(currRow);
            leftLon = e0.getLonAtLat(lowerLat);
            rightLon = e1.getLonAtLat(lowerLat);
        }

        // make sure left is left, right is right...
        if (leftLon > rightLon) {
            double tmp = leftLon;
            leftLon = rightLon;
            rightLon = tmp;
        }

        Vertex lowerLeft = new Vertex(leftLon, lowerLat);
        Vertex lowerRight = new Vertex(rightLon, lowerLat);

        // Begin the "scan converting" against the rows of the terrain grid...
        while (true) {
            double upperLat = terrain.getLatAtRow(currRow + 1);
            if (upperLat <= e0.p1Lat && upperLat <= e1.p1Lat) {
                currRow++;
            } else {
                if (upperLat > e0.p1Lat) {
                    upperLat = e0.p1Lat;
                }
                if (upperLat > e1.p1Lat) {
                    upperLat = e1.p1Lat;
                }
            }

            Vertex upperLeft;
            upperLeft = new Vertex(e0.getLonAtLat(upperLat), upperLat);

            Vertex upperRight;
            upperRight = new Vertex(e1.getLonAtLat(upperLat), upperLat);

            if (upperLeft.x > upperRight.x) {
                Vertex tmp = upperLeft;
                upperLeft = upperRight;
                upperRight = tmp;
            }

            // At this point, we have a quad that has two parallel sides coinciding with rows of the terrain grid,
            // the other two opposite sides are from the triangle. This quad possibly extends out beyond the
            // grid. Clip it against each grid cell that it touches.

            double minLon = (lowerLeft.x < upperLeft.x) ? lowerLeft.x : upperLeft.x;
            double maxLon = (lowerRight.x > upperRight.x) ? lowerRight.x : upperRight.x;
            int begCol = terrain.getColAtLon(minLon);
            int endCol = terrain.getColAtLon(maxLon);
            if (!(begCol >= terrain.getNumCols() || endCol < 0)) {

                Polygon quadStrip = new Polygon();
                quadStrip.addVertex(lowerLeft.x, lowerLeft.y);
                quadStrip.addVertex(upperLeft.x, upperLeft.y);
                quadStrip.addVertex(upperRight.x, upperRight.y);
                quadStrip.addVertex(lowerRight.x, lowerRight.y);

                if (begCol < 0) {
                    // clip at left edge of our terrain grid...
                    quadStrip.splitAtXPlane(terrain.getMinLongitude());
                    begCol = 0;
                }
                if (endCol >= terrain.getNumCols()) {
                    // clip at right edge of a our terrain grid...
                    quadStrip = quadStrip.splitAtXPlane(terrain.getMaxLongitude());
                    endCol = terrain.getNumCols() - 1;
                }

                // now clip against any interior grid cells...
                for (int i = begCol + 1; i <= endCol; i++) {
                    // clip against the ith grid boundary...
                    double lon = terrain.getLonAtCol(i);

                    Polygon left = quadStrip.splitAtXPlane(lon);
                    if (left != null) {
                        left.convertToXYZ(terrain);
                        this.polygons.add(left);
                    }
                }

                quadStrip.convertToXYZ(terrain);
                this.polygons.add(quadStrip);


            }

            // reached the "upper" vertex of an edge?
            if (upperLat >= e0.p1Lat) {
                if (iter.hasNext()) {
                    e0 = iter.next();
                } else {
                    break;
                }
            }
            if (upperLat >= e1.p1Lat) {
                if (iter.hasNext()) {
                    e1 = iter.next();
                } else {
                    break;
                }
            }

            // off the top of our terrain grid?
            if (currRow >= terrain.getNumRows()) {
                break;
            }
            lowerLeft = upperLeft;
            lowerRight = upperRight;
        }

    }

    private void splitAtDateline(Triangle t, RectangularTessellator.RectGeometry terrain) {
        Polygon right = new Polygon();

        for (int i=0; i<3; i++) {
            double lat = t.vertices[i].getLatitude().degrees;
            double lon = t.vertices[i].getLongitude().degrees;
            if (lon > 0.) lon -= Angle.POS360.degrees;
            right.addVertex(lon, lat);
        }

        Polygon left = right.splitAtXPlane(Angle.NEG180.degrees);

        // Make triangles out of our two polygons and throw them at the terrain intersector.
        // Since the polygons are convex, we'll effectively generate a triangle fan.
        Triangle tri = new Triangle();
        for (int i=2; i<left.numVerts; i++) {
            // We need to "normalize" the longitudes for the "left" polygon...
            tri.vertices[0] = new LatLon(Angle.fromDegrees(left.xy[0][1]), Angle.fromDegrees(left.xy[0][0]+Angle.POS360.degrees));
            tri.vertices[1] = new LatLon(Angle.fromDegrees(left.xy[i-1][1]), Angle.fromDegrees(left.xy[i-1][0]+Angle.POS360.degrees));
            tri.vertices[2] = new LatLon(Angle.fromDegrees(left.xy[i][1]), Angle.fromDegrees(left.xy[i][0]+Angle.POS360.degrees));
            intersectTerrain(tri, terrain);
        }

        for (int i=2; i<right.numVerts; i++) {
            tri.vertices[0] = new LatLon(Angle.fromDegrees(right.xy[0][1]), Angle.fromDegrees(right.xy[0][0]));
            tri.vertices[1] = new LatLon(Angle.fromDegrees(right.xy[i-1][1]), Angle.fromDegrees(right.xy[i-1][0]));
            tri.vertices[2] = new LatLon(Angle.fromDegrees(right.xy[i][1]), Angle.fromDegrees(right.xy[i][0])); 
            intersectTerrain(tri, terrain);
        }

    }

    //
    // This Polygon class is used to encode pieces of the intersect of our SurfaceShapeGeometry with a terrain grid.
    //
    private class Polygon {
        double[][] xy;
        int numVerts;

        public Polygon() {
            xy = new double[6][2];
            numVerts = 0;
        }

        public void addVertex(double x, double y) throws IllegalStateException {
            if (numVerts >= xy.length) {
                // TODO:  I8N this
                throw new IllegalStateException("SOMETHING'S DREADFULLY WRONG!!");
            }
            xy[numVerts][0] = x;
            xy[numVerts][1] = y;
            ++numVerts;
        }

        //
        // Splits this Polygon into two at the given "splitPlane". The splitPlane is presumed to be vertical,
        // i.e., a line of constant longitude.
        //
        // Returns the Polygon to the left of the splitPlane, and modifies this Polygon to contain the
        // righthand result of the split.
        //
        public Polygon splitAtXPlane(double splitPlane) {
            double[][] leftPoly = new double[6][2];
            double[][] rightPoly = new double[6][2];
            int numLeft = 0;
            int numRight = 0;

            for (int i = 0; i < numVerts; i++) {
                if (xy[i][0] < splitPlane) {
                    leftPoly[numLeft++] = xy[i];
                } else if (xy[i][0] > splitPlane) {
                    rightPoly[numRight++] = xy[i];
                }

                int j = (i + 1) % numVerts;
                double t = intersectAtX(splitPlane, xy[i][0], xy[j][0]);

                if (t < 0. || t > 1.) {
                    continue;
                }
                double[] newXY = new double[2];
                newXY[0] = splitPlane;
                newXY[1] = valAtParam(t, xy[i][1], xy[j][1]);
                if (newXY[1] == Double.MAX_VALUE) {
                    newXY[1] = xy[i][1];
                }  // parallel...

                // NOTE THAT BOTH POLYGONS SHARE A REFERENCE TO THE SAME ARRAY!
                leftPoly[numLeft++] = newXY;
                rightPoly[numRight++] = newXY;
            }
            xy = rightPoly;
            numVerts = numRight;

            Polygon p = null;
            if (numLeft > 0) {
                p = new Polygon();
                p.xy = leftPoly;
                p.numVerts = numLeft;
            }

            return p;
        }

        //
        // Computes the value along a parameterized line.
        //
        private double valAtParam(double t, double p0, double p1) {
            double delta = p1 - p0;
            if (Math.abs(delta) < 1.e-10) {
                return Double.MAX_VALUE;
            }
            return p0 + t * delta;
        }

        //
        // Returns the parameter along a parameterized line where the given X-intersect occurs.
        //
        private double intersectAtX(double x, double x2, double x1) {
            double delta = x2 - x1;
            if (Math.abs(delta) < 1.e-10) {
                return Double.MAX_VALUE;
            }
            return (x2 - x) / delta;
        }

        //
        // Replaces the lat-lon coordinates with XYZ values interpolated from the terrain grid.
        //
        public void convertToXYZ(RectangularTessellator.RectGeometry terrain) {
            Vec4 refPoint = getReferencePoint();
            for (int i=0; i<this.numVerts; i++) {
                // Note in the following call, we have a double[2] going in, replaced by a double[3] coming out...
                xy[i] = terrain.getPointAt(xy[i][Y], xy[i][X]);
                xy[i][X] -= refPoint.x;
                xy[i][Y] -= refPoint.y;
                xy[i][Z] -= refPoint.z;
            }
        }
    }

    //
    // TODO:  Is there still utility in using this class in leu of something else?   --RLB
    //
    private class Vertex {

        public Vertex(double x, double y) {
            this.x = x;
            this.y = y;
        }

        double x;
        double y;
    }

    //
    // A bundle of info needed to track "active edges" during our "scan conversion" of triangles against the
    // terrain raster (grid).
    //
    private class ActiveEdge {

        double p0Lon, p0Lat;
        double p1Lon, p1Lat;
        double deltaX;
        double deltaY;
        int begRow, endRow;
        int begCol, endCol;

        public ActiveEdge(LatLon p0, LatLon p1) {
            // we want to guarantee p0 <= p1
            if (p0.getLatitude().degrees > p1.getLatitude().degrees) {
                this.p0Lon = p1.getLongitude().degrees;
                this.p0Lat = p1.getLatitude().degrees;
                this.p1Lon = p0.getLongitude().degrees;
                this.p1Lat = p0.getLatitude().degrees;
            } else {
                this.p0Lon = p0.getLongitude().degrees;
                this.p0Lat = p0.getLatitude().degrees;
                this.p1Lon = p1.getLongitude().degrees;
                this.p1Lat = p1.getLatitude().degrees;
            }

            deltaX = p1Lon - p0Lon;
            deltaY = p1Lat - p0Lat;
        }

        public double getLonAtLat(double lat) {
            if (Math.abs(deltaY) < 1.e-10) {
                return Double.MAX_VALUE;
            }
            double t = (lat - p0Lat) / deltaY;
            return (p0Lon + t * deltaX);
        }

        public double getLatAtLon(double lon) {
            if (Math.abs(deltaX) < 1.e-10) {
                return Double.MAX_VALUE;
            }
            double t = (lon - p0Lon) / deltaX;
            return (p0Lat + t * deltaY);
        }
    }

    //
    // Returns a list of SectorGeometry objects whose extent overlaps that of this object.
    //
    protected Iterable<SectorGeometry> getIntersectingGeometryTiles(SectorGeometryList sg) {
        ArrayList<SectorGeometry> intersectingGeom = null;

        for (SectorGeometry geom : sg) {
            if (!this.getSector().intersects(geom.getSector()))
                continue;

            if (intersectingGeom == null)
                intersectingGeom = new ArrayList<SectorGeometry>();

            intersectingGeom.add(geom);
        }

        return intersectingGeom;
    }


    public Position getReferencePosition() {
        // Here we pick a vertex arbitrarily. Whereas it might seem reasonable to pick the centroid of the
        // collection of points, computing that value is tricky in cases where the shape spans the dateline.
        return new Position(positions.get(0), 0);
    }

    public Vec4 getReferencePoint() {
        Position ref = getReferencePosition();
        double elev = this.globe.getElevation(ref.getLatitude(), ref.getLongitude());
        return this.globe.computePointFromPosition(ref.getLatitude(), ref.getLongitude(), elev);
    }

    public void move(Position delta) {
        if (delta == null) {
            String msg = Logging.getMessage("nullValue.PositionIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        this.moveTo(this.getReferencePosition().add(delta));
    }

    /**
     * Move the shape over the sphereoid surface without maintaining its original azimuth -- its orientation relative to
     * North.
     *
     * @param position the new position to move the shapes reference position to.
     */
    public void shiftTo(Position position) {
        if (this.globe == null)
            return;

        if (position == null) {
            String msg = Logging.getMessage("nullValue.PositionIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        Vec4 p1 = globe.computePointFromPosition(this.getReferencePosition().getLatitude(),
                this.getReferencePosition().getLongitude(), 0);
        Vec4 p2 = globe.computePointFromPosition(position.getLatitude(), position.getLongitude(), 0);
        Vec4 delta = p2.subtract3(p1);

        for (int i = 0; i < this.positions.size(); i++) {
            LatLon ll = this.positions.get(i);
            Vec4 p = globe.computePointFromPosition(ll.getLatitude(), ll.getLongitude(), 0);
            p = p.add3(delta);
            Position pos = globe.computePositionFromPoint(p);

            this.positions.set(i, new LatLon(pos.getLatitude(), pos.getLongitude()));
        }

        this.polyline.setPositions(this.positions, 0.);

        for (Triangle t : triangles) {
            for (int i = 0; i < 3; i++) {
                LatLon ll = t.vertices[i];
                Vec4 p = globe.computePointFromPosition(ll.getLatitude(), ll.getLongitude(), 0);
                p = p.add3(delta);
                Position pos = globe.computePositionFromPoint(p);
                t.vertices[i] = new LatLon(pos.getLatitude(), pos.getLongitude());
            }
            t.setOrClearDatelineFlag();
        }

        resetBounds();
        this.forceSurfaceIntersect = true;
    }

    /**
     * Move the shape over the sphereoid surface while maintaining its original azimuth -- its orientation relative to
     * North.
     *
     * @param position the new position to move the shapes reference position to.
     */
    public void moveTo(Position position) {
        if (LatLon.locationsCrossDateLine(this.positions)) {
            // TODO: Replace this hack by figuring out how to *accurately* move date-line crossing shapes using the
            // distance/azimuth method used below for shapes that do not cross the dateline.
            shiftTo(position);
            return;
        }

        LatLon oldRef = this.getReferencePosition();

        for (int i = 0; i < this.positions.size(); i++) {
            LatLon p = this.positions.get(i);
            double distance = LatLon.greatCircleDistance(oldRef, p).radians;
            double azimuth = LatLon.greatCircleAzimuth(oldRef, p).radians;
            LatLon pp = LatLon.greatCircleEndPosition(position, azimuth, distance);
            this.positions.set(i, pp);
        }

        this.polyline.setPositions(positions, 0.);

        for (Triangle t : this.triangles) {
            for (int i = 0; i < 3; i++) {
                LatLon p = t.vertices[i];
                double distance = LatLon.greatCircleDistance(oldRef, p).radians;
                double azimuth = LatLon.greatCircleAzimuth(oldRef, p).radians;
                LatLon pp = LatLon.greatCircleEndPosition(position, azimuth, distance);
                t.vertices[i] = pp;
            }
            t.setOrClearDatelineFlag();
        }

        resetBounds();
        this.forceSurfaceIntersect = true;
    }

    public static SurfaceShapeGeometry createEllipse(Globe globe, LatLon center, double majorAxisLength,
                                                     double minorAxisLength, Angle orientation, int intervals,
                                                     Color interiorColor, Color borderColor) {
        if (orientation == null)
            orientation = Angle.ZERO;

        if (majorAxisLength <= 0) {
            String message = Logging.getMessage("Geom.MajorAxisInvalid");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (minorAxisLength <= 0) {
            String message = Logging.getMessage("Geom.MajorAxisInvalid");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        int numPositions = 1 + Math.max(intervals, 4);
        final ArrayList<LatLon> positions = new ArrayList<LatLon>();

        double radius = globe.getRadiusAt(center.getLatitude(), center.getLongitude());
        double da = 2 * Math.PI / (numPositions - 1);
        for (int i = 0; i < numPositions; i++) {
            // azimuth runs positive clockwise from north and through 360 degrees.
            double angle = (i != numPositions - 1) ? i * da : 0;
            double azimuth = Math.PI / 2 - (angle + orientation.radians);
            double xLength = majorAxisLength * Math.cos(angle);
            double yLength = minorAxisLength * Math.sin(angle);
            double distance = Math.sqrt(xLength * xLength + yLength * yLength);
            LatLon p = LatLon.greatCircleEndPosition(center, azimuth, distance / radius);
            positions.add(p);
        }

        return new SurfacePolygonGeometry(positions, interiorColor, borderColor);
    }


    private Sector getSector() {
        if (this.sector == null)
            this.sector = Sector.boundingSector(this.positions);
        return this.sector;
    }

    private Extent getExtent(DrawContext dc) {
        if (this.extent == null)
            this.extent = this.globe.computeBoundingCylinder(dc.getVerticalExaggeration(), this.getSector());
        return extent;
    }

    private void resetBounds() {
        this.sector = null;
        this.extent = null;
    }


    //
    // Uses OpenGL's GLU facilities to perform a top-level triangulation of our SurfaceShape.
    //
    private void tessellate() {
        this.triangles = new Vector<Triangle>();

        GLU glu = new GLU();
        GLUtessellator tobj = glu.gluNewTess();

        TessCallback tessCallback = new TessCallback(glu);
        glu.gluTessCallback(tobj, GLU.GLU_TESS_VERTEX, tessCallback);
        glu.gluTessCallback(tobj, GLU.GLU_TESS_COMBINE, tessCallback);
        glu.gluTessCallback(tobj, GLU.GLU_EDGE_FLAG, tessCallback);
        glu.gluTessCallback(tobj, GLU.GLU_TESS_BEGIN, tessCallback);
        glu.gluTessCallback(tobj, GLU.GLU_TESS_END, tessCallback);
        glu.gluTessCallback(tobj, GLU.GLU_TESS_ERROR, tessCallback);

        // GLU wants double arrays for the vertices...
        int len = this.positions.size();
        double[][] pnts = new double[len][3];

        glu.gluTessBeginPolygon(tobj, null);
        glu.gluTessBeginContour(tobj);
        for (int i = 0; i < len; i++) {
            LatLon p = this.positions.get(i);
            pnts[i][X] = p.getLongitude().degrees;
            if (pnts[i][X] > 0.)
                pnts[i][X] -= Angle.POS360.degrees;  // deal with dateline issues; will get correct at triangle re-assembly...
            pnts[i][Y] = p.getLatitude().degrees;
            // NOTE: found out the hard way that these polygons had better be planar!
            // The tessellation fails or misbehaves otherwise.
            pnts[i][Z] = 0.;
            glu.gluTessVertex(tobj, pnts[i], 0, pnts[i]);
        }
        glu.gluTessEndContour(tobj);
        glu.gluTessEndPolygon(tobj);

        glu.gluDeleteTess(tobj);  // free this resource.
    }

    //
    // Tessellation callback implementation.
    //
    private class TessCallback extends GLUtessellatorCallbackAdapter {

        public TessCallback(GLU glu) {
            this.glu = glu;
        }

        public void begin(int type) {
            this.state = 0;
            this.type = type;
            this.flipOrder = false;
            this.currTriangle = new Triangle();
        }

        public void end() {
            // NO-OP
        }

        public void vertex(Object data) {
            if (!(data instanceof double[])) {
                return;
            }

            double[] d = (double[]) data;
            Angle lon = Angle.normalizedLongitude(Angle.fromDegrees(d[0]));
            Angle lat = Angle.normalizedLatitude(Angle.fromDegrees(d[1]));
            this.currTriangle.vertices[this.state++] = new LatLon(lat, lon);

            if (this.state == 3) {
                this.currTriangle.setOrClearDatelineFlag();
                SurfaceShapeGeometry.this.triangles.add(this.currTriangle);

                // Depending upon type of triangles we're being handed, we may already have 2 vertices for the
                // next triangle;  gather those accordingly.
                switch (this.type) {
                    case GL.GL_TRIANGLE_FAN:
                        Triangle t = new Triangle();
                        t.vertices[0] = this.currTriangle.vertices[0];  // these indices reflect the definition of
                        t.vertices[1] = this.currTriangle.vertices[2];  // TRIANGLE_FAN
                        this.currTriangle = t;
                        this.state = 2;
                        break;
                    case GL.GL_TRIANGLE_STRIP:
                        t = new Triangle();
                        short first;
                        short second;
                        if (this.flipOrder) {
                            first = 0;      // Again, these indices reflect how TRIANGLE_STRIPs work.
                            second = 2;
                        } else {
                            first = 2;
                            second = 1;
                        }
                        this.flipOrder = !this.flipOrder;
                        t.vertices[0] = this.currTriangle.vertices[first];
                        t.vertices[1] = this.currTriangle.vertices[second];
                        this.currTriangle = t;
                        this.state = 2;
                        break;
                    case GL.GL_TRIANGLES:
                        this.currTriangle = new Triangle();
                        this.state = 0;
                        break;
                    default:
                        String msg = Logging.getMessage("SurfaceShape.UnknownTriangleForm", this.type);
                        Logging.logger().severe(msg);
                }
            }
        }

        public void combine(double[] coords, Object[] data, float[] weight, Object[] outData) {
            double[] newCoord = new double[3];
            newCoord[X] = coords[X];
            newCoord[Y] = coords[Y];
            newCoord[Z] = coords[Z];
            outData[0] = newCoord;
        }

        public void edgeFlag(boolean isEdge) {
        }

        public void error(int errnum) {
            String glErrorMsg = glu.gluErrorString(errnum);
            String msg = Logging.getMessage("SurfaceShape.TessellationError", glErrorMsg);
            Logging.logger().severe(msg);
            throw new RuntimeException();
        }

        private GLU glu;
        private int type;
        private Triangle currTriangle;
        private short state;        // state is the number of vertices we've collected to make a triangle...
        private boolean flipOrder;  // reflect the rules for gathering vertices from a triangle_strip
    }

    private static class Triangle {
        LatLon[] vertices;
        boolean spansDateline;

        public Triangle() {
            vertices = new LatLon[3];
            spansDateline = false;
        }

        public void setOrClearDatelineFlag() {
            spansDateline = false;
            if (vertices[0] != null || vertices[1] != null || vertices[2] != null) {
                if (LatLon.locationsCrossLongitudeBoundary(vertices[0], vertices[1]) ||
                        LatLon.locationsCrossLongitudeBoundary(vertices[1], vertices[2]) ||
                        LatLon.locationsCrossLongitudeBoundary(vertices[0], vertices[2])) {
                    spansDateline = true;
                }
            }
        }
    }

}