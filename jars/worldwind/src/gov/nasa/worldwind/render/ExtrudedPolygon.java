/*
Copyright (C) 2001, 2010 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/

package gov.nasa.worldwind.render;

import com.sun.opengl.util.BufferUtil;
import gov.nasa.worldwind.Disposable;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.util.*;

import javax.media.opengl.GL;
import javax.media.opengl.glu.*;
import java.awt.*;
import java.nio.*;
import java.util.*;
import java.util.List;

/**
 * A multi-sided 3D shell formed by a base polygon in latitude and longitude extruded to a specified height. The height
 * is relative to a reference position, designated by one of the specified polygon positions. The extruded polygon is
 * capped with a plane at that height and parallel to the ellipsoid at the reference position. Since positions other
 * than the reference position may resolve to points at elevations other than that of the reference position, the
 * distances from those points to the planar cap are adjusted so that the adjacent sides precisely meet the cap.
 * <p/>
 * Extruded polygons are safe to share among World Windows. They should not be shared among layers in the same World
 * Window.
 *
 * @author tag
 * @version $Id: ExtrudedPolygon.java 13215 2010-03-16 18:53:44Z tgaskins $
 */
public class ExtrudedPolygon extends AVListImpl implements GeographicExtent, Disposable
{
    // These static hash maps hold the vertex indices that define the shape geometry. Their contents depend only on the
    // number of locations in the source polygon, so can be reused by all shapes with the same location count.
    private static HashMap<Integer, IntBuffer> fillIndexBuffers = new HashMap<Integer, IntBuffer>();
    private static HashMap<Integer, IntBuffer> edgeIndexBuffers = new HashMap<Integer, IntBuffer>();

    private List<LatLon> locations = new ArrayList<LatLon>();
    private double height = 1;
    private int referenceIndex = 0;
    private ShapeAttributes sideAttributes;
    private ShapeAttributes capAttributes;
    private List<WWTexture> textures;

    protected IntBuffer sideIndices;
    protected IntBuffer edgeIndices;
    protected Cap cap; // the top of the extruded polygon
    protected long frameID; // the ID of the most recent rendering frame

    // These values are computed every frame, thus they are safe for multi-window usage.
    protected Vec4[] vertices; // the shape's vertices: base vertices are first, then the top vertices.
    protected FloatBuffer vertexBuffer; // the shape's vertices arranged in a buffer to pass to JOGL
    protected FloatBuffer sideTexCoordsBuffer; // texture coordinates for the shape's sides
    protected Vec4 referencePoint; // the Cartesian coordinates of the specified reference point
    protected Vec4 polygonNormal;
    protected Extent extent; // the shape's extent
    protected Sector sector; // the shape's bouding sector

    /** Construct a shape with an empty position list and a default height of 1 meter. */
    public ExtrudedPolygon()
    {
        this.initialize();
    }

    /**
     * Construct a shape for a specified list of locations and a height.
     *
     * @param corners the list of locations defining the polygon.
     * @param height  the shape height, in meters.
     *
     * @throws IllegalArgumentException if the location list is null or the height is less than or equal to zero.
     */
    public ExtrudedPolygon(Iterable<? extends LatLon> corners, double height)
    {
        if (locations == null)
        {
            String message = Logging.getMessage("nullValue.IterableIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (height <= 0)
        {
            String message = Logging.getMessage("generic.ArgumentOutOfRange", "height <= 0");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.setLocations(corners);
        this.height = height;

        this.initialize();
    }

    protected void initialize()
    {
        this.sideAttributes = new BasicShapeAttributes();
        this.sideAttributes.setInteriorMaterial(Material.LIGHT_GRAY);
        this.sideAttributes.setOutlineMaterial(Material.DARK_GRAY);

        this.capAttributes = new BasicShapeAttributes(this.sideAttributes);
        this.capAttributes.setInteriorMaterial(Material.GRAY);
    }

    /** {@inheritDoc} */
    public void dispose()
    {
        // Remove references to textures and NIO buffers. Not necessary, but prevents dangling references to large
        // chunks of memory.
        if (this.textures != null)
            this.textures.clear();
        this.textures = null;

        this.vertexBuffer = null;
        this.sideTexCoordsBuffer = null;
        this.vertices = null;

        if (this.cap != null)
            this.cap.dispose();
    }

    /**
     * Returns the list of locations defining the polygon.
     *
     * @return the latitude and longitude of the polygon locations originally specified.
     */
    public Iterable<? extends LatLon> getLocations()
    {
        return this.locations;
    }

    /**
     * Specifies the latitude and longitude of the locations defining the polygon.
     *
     * @param corners the polygon locations.
     *
     * @throws IllegalArgumentException if the location list is null.
     */
    public void setLocations(Iterable<? extends LatLon> corners)
    {
        if (corners == null)
        {
            String message = Logging.getMessage("nullValue.IterableIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.locations = new ArrayList<LatLon>();

        for (LatLon corner : corners)
        {
            this.locations.add(corner);
        }

        this.reinitialize();
    }

    /**
     * Returns the specified shape height.
     *
     * @return the shape height originally specified, in meters.
     */
    public double getHeight()
    {
        return height;
    }

    /**
     * Specifies the shape height.
     *
     * @param height the shape height, in meters
     *
     * @throws IllegalArgumentException if the height is less than or equal to zero.
     */
    public void setHeight(double height)
    {
        if (height <= 0)
        {
            String message = Logging.getMessage("generic.ArgumentOutOfRange", "height <= 0");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.height = height;

        this.reinitialize();
    }

    /**
     * Returns the index of the position in the list of polygon positions to use as the reference position.
     *
     * @return the reference index.
     */
    public int getReferenceIndex()
    {
        return this.referenceIndex;
    }

    /**
     * Specifies the index of the position in the list of polygon positions to use as the reference position.
     *
     * @param referencePositionIndex the index of the reference position.
     *
     * @throws IllegalArgumentException if the index is invalid for the current position list.
     */
    public void setReferenceIndex(int referencePositionIndex)
    {
        if (referencePositionIndex < 0 || referencePositionIndex >= this.locations.size())
        {
            String message = Logging.getMessage("generic.indexOutOfRange", referencePositionIndex);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.referenceIndex = referencePositionIndex;
    }

    public ShapeAttributes getSideAttributes()
    {
        return sideAttributes;
    }

    public void setSideAttributes(ShapeAttributes attributes)
    {
        this.sideAttributes = attributes;
    }

    public ShapeAttributes getCapAttributes()
    {
        return this.capAttributes;
    }

    public void setCapAttributes(ShapeAttributes attributes)
    {
        this.capAttributes = attributes;
    }

    public List<Object> getImageSources()
    {
        List<Object> imageSources = new ArrayList<Object>(this.textures.size());

        for (WWTexture image : this.textures)
        {
            imageSources.add(image.getImageSource());
        }

        return imageSources;
    }

    protected WWTexture getTexture(int side)
    {
        if (this.textures == null || this.textures.size() == 0)
            return null;

        return this.textures.size() > side ? this.textures.get(side) : null;
    }

    public void setImageSources(List<?> imageSources)
    {
        this.textures = new ArrayList<WWTexture>(imageSources.size());

        for (Object source : imageSources)
        {
            if (source != null)
                this.textures.add(new BasicWWTexture(source, true));
            else
                this.textures.add(null);
        }
    }

    /** Resets the internal state to reflect the current polygon positions. */
    protected void reinitialize()
    {
        //noinspection StringEquality
        if (WWMath.computeWindingOrderOfLocations(this.locations) != AVKey.COUNTER_CLOCKWISE)
            Collections.reverse(this.locations);

        int bufferSize = 3 * (this.locations.size() + 1) * 2; // x,y,z for 2(n+1) vertices
        if (this.vertexBuffer == null || this.vertexBuffer.capacity() < bufferSize)
            this.vertexBuffer = BufferUtil.newFloatBuffer(bufferSize);

        this.sideIndices = this.getSideIndices(this.locations.size());
        this.edgeIndices = this.getEdgeIndices(this.locations.size());

        this.cap = null;
        this.vertices = null;
        this.sideTexCoordsBuffer = null;
        this.extent = null;
        this.sector = null;
    }

    /** {@inheritDoc} */
    public void render(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        if (this.locations.size() < 3)
            return;

        if (!this.isVisible(dc))
            return;

        // The shape's geometry is computed every frame
        if (dc.getFrameTimeStamp() != this.frameID)
            this.computeGeometry(dc);

        if (this.cap == null)
            this.cap = new Cap(this.vertexBuffer, this.locations.size(), this.polygonNormal);

        this.frameID = dc.getFrameTimeStamp();

        GL gl = dc.getGL();
        OGLStackHandler ogsh = new OGLStackHandler();

        boolean applyTexture = (!dc.isPickingMode() && this.textures != null && this.textures.size() > 0);

        ogsh.pushClientAttrib(gl, GL.GL_CLIENT_VERTEX_ARRAY_BIT);
        ogsh.pushAttrib(gl, GL.GL_POLYGON_BIT | GL.GL_CURRENT_BIT | GL.GL_DEPTH_BUFFER_BIT
            | (applyTexture ? (GL.GL_TEXTURE_BIT | GL.GL_TRANSFORM_BIT) : 0));
        try
        {
            dc.getView().pushReferenceCenter(dc, this.referencePoint);
            gl.glEnableClientState(GL.GL_VERTEX_ARRAY);

            gl.glEnable(GL.GL_CULL_FACE);
            gl.glFrontFace(GL.GL_CCW);

            // All the draw methods use the same vertex buffer
            gl.glVertexPointer(3, GL.GL_FLOAT, 0, this.vertexBuffer.rewind());

            if (applyTexture)
            {
                // Push an identity texture matrix. This prevents drawSides() from leaking GL texture matrix state. The
                // texture matrix stack is popped from OGLStackHandler.pop(), in the finally block below.
                ogsh.pushTextureIdentity(gl);
                dc.getGL().glEnable(GL.GL_TEXTURE_2D);

                gl.glEnableClientState(GL.GL_TEXTURE_COORD_ARRAY);
                if (this.sideTexCoordsBuffer == null)
                    this.fillSideTexCoordBuffer(this.vertices);
                gl.glTexCoordPointer(2, GL.GL_FLOAT, 0, this.sideTexCoordsBuffer.rewind());
            }

            this.drawSides(dc, applyTexture);

            if (applyTexture)
                gl.glDisable(GL.GL_TEXTURE_2D);

            if (!applyTexture)
                this.drawEdges(dc);

            if (this.cap != null)
                this.cap.draw(dc, this.capAttributes);
        }
        finally
        {
            ogsh.pop(gl);
            dc.getView().popReferenceCenter(dc);
        }
    }

    /**
     * Indicates whether the shape is visible in the current view.
     *
     * @param dc the draw context.
     *
     * @return true if the shape is visible, otherwise false.
     */
    @SuppressWarnings({"RedundantIfStatement"})
    protected boolean isVisible(DrawContext dc)
    {
        Extent extent = this.getExtent();
        if (extent == null)
            return true; // don't know the visibility, shape hasn't been computed yet

        if (dc.isPickingMode())
            return dc.getPickFrustums().intersectsAny(extent);

        return dc.getView().getFrustumInModelCoordinates().intersects(extent);
    }

    /**
     * Draws the shape's sides.
     *
     * @param dc           the draw context.
     * @param applyTexture apply texture to sides if true, otherwise don't apply texture to sides
     */
    protected void drawSides(DrawContext dc, boolean applyTexture)
    {
        GL gl = dc.getGL();

        if (!dc.isPickingMode())
        {
            Color sc = this.getSideAttributes().getInteriorMaterial().getDiffuse();
            gl.glColor3ub((byte) sc.getRed(), (byte) sc.getGreen(), (byte) sc.getBlue());
        }

        this.sideIndices.rewind();
        for (int i = 0; i < this.locations.size(); i++)
        {
            if (applyTexture)
            {
                this.textures.get(i).bind(dc);
                this.textures.get(i).applyInternalTransform(dc);
            }

            this.sideIndices.position(4 * i);
            this.sideIndices.limit(4 * (i + 1));
            gl.glDrawElements(GL.GL_TRIANGLE_STRIP, 4, GL.GL_UNSIGNED_INT, this.sideIndices);
        }
    }

    /**
     * Draws the shape's edges. Assumes the vertex buffer has already been set in the OpenGL context.
     *
     * @param dc the draw context.
     */
    protected void drawEdges(DrawContext dc)
    {
        GL gl = dc.getGL();

        if (!dc.isPickingMode())
        {
            Color lc = this.getSideAttributes().getOutlineMaterial().getDiffuse();
            gl.glColor3ub((byte) lc.getRed(), (byte) lc.getGreen(), (byte) lc.getBlue());
        }

        dc.pushProjectionOffest(0.997);
        gl.glDrawElements(GL.GL_LINES, this.edgeIndices.limit(), GL.GL_UNSIGNED_INT, this.edgeIndices.rewind());
        dc.popProjectionOffest();
    }

    /**
     * Computes the Cartesian coordinates of the shape's vertices and attaches them to the shape. The computed Cartesian
     * coordinates are relative to a reference point, which is also computed and attached to the shape here.
     * <p/>
     * The shape's Cartesian, modelling coordinate extent is also computed here.
     *
     * @param dc the draw context.
     *
     * @return the computed reference point.
     */
    protected Vec4 computeGeometry(DrawContext dc)
    {
        int n = this.locations.size();
        int k = this.getReferenceIndex();

        if (this.vertices == null || this.vertices.length < 2 * n)
            this.vertices = new Vec4[2 * n];

        // The vertices around the base of the shape are stored first, followed by the vertices around the top. The
        // order for both top and bottom is CCW as one looks down from space onto the base polygon. For a 4-sided
        // polygon (defined by 4 lat/lon locations) the vertex order is 0123,4567.

        // Compute the Cartesian coordinates of the reference location
        Vec4 vert = dc.getSurfaceGeometry().getSurfacePoint(this.locations.get(k));
        if (vert == null)
            vert = this.computeGlobePoint(dc, this.locations.get(k));

        Vec4 va = vert;
        double vaLength = va.getLength3();
        this.vertices[k] = Vec4.ZERO; // the reference vertex is the origin of the polygon

        // Compute the cap corner corresponding to the reference location
        Vec4 vaa = va.multiply3(this.getHeight() / va.getLength3()); // ref point surface normal scaled to height
        double vaaLength = vaa.getLength3();
        this.vertices[k + n] = vaa;

        // Assign the other corners
        for (int i = 0; i < n; i++)
        {
            if (i == k)
                continue; // already added verts to array

            vert = dc.getSurfaceGeometry().getSurfacePoint(this.locations.get(i));
            if (vert == null)
                vert = this.computeGlobePoint(dc, this.locations.get(i));
            this.vertices[i] = vert.subtract3(va);

            double delta = vaLength - vert.dot3(va) / vaLength;
            vert = vert.add3(vaa.multiply3(1d + delta / vaaLength));
            this.vertices[i + n] = vert.subtract3(va);
        }

        this.referencePoint = va;
        this.polygonNormal = va.normalize3();
        this.fillVertexBuffer(this.vertices);
        this.extent = this.computeExtent();

        return va;
    }

    protected Vec4 computeGlobePoint(DrawContext dc, LatLon location)
    {
        // Compute a point from the globe. THis is done if the point can't be computed from the sector geometry.
        double elev = dc.getGlobe().getElevation(location.getLatitude(), location.getLongitude());
        elev *= dc.getVerticalExaggeration();

        return dc.getGlobe().computePointFromPosition(location.getLatitude(), location.getLongitude(), elev);
    }

    protected void fillVertexBuffer(Vec4[] verts)
    {
        int n = verts.length / 2;

        int size = 2 * n * 4 * 2; // 2n of 4 verts w/x,y,z
        if (this.vertexBuffer == null || this.vertexBuffer.limit() < size)
            this.vertexBuffer = BufferUtil.newFloatBuffer(size);
        FloatBuffer vBuf = this.vertexBuffer;

        // Fill the vertex buffer with coordinates for each independent face -- 4 vertices per face. Vertices need to be
        // independent in order to have different texture coordinates and normals per face.
        // For a 4-sided polygon the vertex order is 0154,1265,2376,3047. Note the clockwise ordering.
        int b = 0;
        for (int i = 0; i < n - 1; i++)
        {
            int v = i;
            vBuf.put(b++, (float) verts[v].x).put(b++, (float) verts[v].y).put(b++, (float) verts[v].z);

            v = i + 1;
            vBuf.put(b++, (float) verts[v].x).put(b++, (float) verts[v].y).put(b++, (float) verts[v].z);

            v = i + n + 1;
            vBuf.put(b++, (float) verts[v].x).put(b++, (float) verts[v].y).put(b++, (float) verts[v].z);

            v = i + n;
            vBuf.put(b++, (float) verts[v].x).put(b++, (float) verts[v].y).put(b++, (float) verts[v].z);
        }

        // The last side uses the first and n-th vertices
        int v = n - 1;
        vBuf.put(b++, (float) verts[v].x).put(b++, (float) verts[v].y).put(b++, (float) verts[v].z);

        v = 0;
        vBuf.put(b++, (float) verts[v].x).put(b++, (float) verts[v].y).put(b++, (float) verts[v].z);

        v = n;
        vBuf.put(b++, (float) verts[v].x).put(b++, (float) verts[v].y).put(b++, (float) verts[v].z);

        v = 2 * n - 1;
        //noinspection UnusedAssignment
        vBuf.put(b++, (float) verts[v].x).put(b++, (float) verts[v].y).put(b++, (float) verts[v].z);
    }

    protected void fillSideTexCoordBuffer(Vec4[] verts)
    {
        int n = verts.length / 2;
        double lengths[] = new double[n + 1];

        // Find the top-to-bottom lengths of the corners in order to determine their relative lenghts
        for (int i = 0; i < n; i++)
        {
            lengths[i] = verts[i].distanceTo3(verts[i + n]);
        }
        lengths[n] = lengths[0]; // duplicate the first length to ease iteration below

        int size = 2 * n * 4 * 2; // 2n of 4 verts w/s,t
        if (this.sideTexCoordsBuffer == null || this.sideTexCoordsBuffer.limit() < size)
            this.sideTexCoordsBuffer = BufferUtil.newFloatBuffer(size);
        FloatBuffer tBuf = this.sideTexCoordsBuffer;

        // Fill the vertex buffer with texture coordinates for each independent face in the same order as the vertices
        // in the vertex buffer.
        int b = 0;
        for (int i = 0; i < n; i++)
        {
            // Set the base t texture coord to 0 for the longer side and a proportional value for the shorter side.
            if (lengths[i] > lengths[i + 1])
            {
                tBuf.put(b++, 0).put(b++, 0);
                tBuf.put(b++, 1).put(b++, (float) (1d - lengths[i + 1] / lengths[i]));
            }
            else
            {
                tBuf.put(b++, 0).put(b++, (float) (1d - lengths[i] / lengths[i + 1]));
                tBuf.put(b++, 1).put(b++, 0);
            }
            tBuf.put(b++, 1).put(b++, 1);
            tBuf.put(b++, 0).put(b++, 1);
        }
    }

    protected Extent getExtent()
    {
        return this.extent;
    }

    protected Extent computeExtent() // TODO: Compute a tighter extent
    {
        Sphere boundingSphere = Sphere.createBoundingSphere(this.vertices);
        return boundingSphere != null ?
            new Sphere(this.referencePoint.add3(boundingSphere.getCenter()), boundingSphere.getRadius()) : null;
    }

    public Sector getSector()
    {
        if (this.sector == null && this.getLocations() != null)
            this.sector = Sector.boundingSector(this.getLocations());

        return this.sector;
    }

    /**
     * Returns the indices defining the vertices of each side of the shape.
     *
     * @param n the number of positions in the polygon.
     *
     * @return a buffer of indices that can be passed to OpenGL to draw all sides of the shape.
     */
    protected IntBuffer getSideIndices(int n)
    {
        IntBuffer ib = fillIndexBuffers.get(n);
        if (ib != null)
            return ib;

        // Compute them if not already computed. Each side is two triangles defined by one triangle strip. All edges
        // can't be combined into one tri-strip because each side can have its own texture.
        ib = BufferUtil.newIntBuffer(n * 4);
        for (int i = 0; i < n; i++)
        {
            ib.put(4 * i + 3).put(4 * i).put(4 * i + 2).put(4 * i + 1);
        }

        fillIndexBuffers.put(n, ib);

        return ib;
    }

    /**
     * Returns the indices defining the vertices of each edge of the shape.
     *
     * @param n the number of positions in the polygon.
     *
     * @return a buffer of indices that can be passed to OpenGL to draw all the shape's edges.
     */
    protected IntBuffer getEdgeIndices(int n)
    {
        IntBuffer ib = edgeIndexBuffers.get(n);
        if (ib != null)
            return ib;

        // The edges are two-point lines connecting vertex pairs.
        // Bottom
        ib = BufferUtil.newIntBuffer((2 * n) * 3); // 2n for top and bottom + 2n for corners
        for (int i = 0; i < n; i++)
        {
            ib.put(4 * i).put(4 * i + 1);
        }

        // Top
        for (int i = 0; i < n; i++)
        {
            ib.put(4 * i + 2).put(4 * i + 3);
        }

        // Corners
        for (int i = 0; i < n; i++)
        {
            ib.put(4 * i).put(4 * i + 3);
        }

        edgeIndexBuffers.put(n, ib);

        return ib;
    }

    /** An internal class used to tessellate the polygon itself and produce the indices needed to draw it. */
    protected static class Cap implements Disposable
    {
        protected IntBuffer fillIndices;
        protected ArrayList<IntBuffer> fillIndexBuffers;
        protected ArrayList<Integer> primTypes;

        public Cap(FloatBuffer vertices, int numVertices, Vec4 normal)
        {
            this.tessellatePolygon(vertices, numVertices, normal);
        }

        public void dispose()
        {
            // Remove references to NIO buffers
            this.fillIndices = null;
            this.fillIndexBuffers.clear();
        }

        /**
         * Tessellates the polygon from its vertices.
         *
         * @param vertices the polygon vertices.
         * @param numVerts the number of vertices from the buffer to use. The first <code>numVerts</code> are used.
         * @param normal   a unit normal vector for the plane containing the polygon vertices.
         */
        protected void tessellatePolygon(FloatBuffer vertices, int numVerts, Vec4 normal)
        {
            GLU glu = new GLU();
            GLUtessellator tess = glu.gluNewTess();

            TessellatorCallback cb = new TessellatorCallback();
            glu.gluTessCallback(tess, GLU.GLU_TESS_VERTEX, cb);
            glu.gluTessCallback(tess, GLU.GLU_TESS_BEGIN, cb);
            glu.gluTessCallback(tess, GLU.GLU_TESS_END, cb);
            glu.gluTessCallback(tess, GLU.GLU_TESS_COMBINE, cb);

            glu.gluTessNormal(tess, normal.x, normal.y, normal.z);
            glu.gluTessBeginPolygon(tess, null);
            glu.gluTessBeginContour(tess);
            double[] coords = new double[3];
            for (int i = 0; i < numVerts; i++)
            {
                int j = 4 * i + 3;
                coords[0] = vertices.get(j * 3);
                coords[1] = vertices.get(j * 3 + 1);
                coords[2] = vertices.get(j * 3 + 2);

                glu.gluTessVertex(tess, coords, 0, j);
            }
            glu.gluTessEndContour(tess);
            glu.gluTessEndPolygon(tess);
            glu.gluDeleteTess(tess);

            this.makeIndexLists(cb);
        }

        protected void makeIndexLists(TessellatorCallback cb)
        {
            this.fillIndices = BufferUtil.newIntBuffer(cb.getNumIndices());
            this.fillIndexBuffers = new ArrayList<IntBuffer>(cb.getPrimTypes().size());
            this.primTypes = cb.getPrimTypes();

            for (ArrayList<Integer> prim : cb.getPrims())
            {
                IntBuffer ib = this.fillIndices.slice();
                for (Integer i : prim)
                {
                    ib.put(i);
                }
                ib.flip();
                this.fillIndexBuffers.add(ib);
                this.fillIndices.position(this.fillIndices.position() + ib.limit());
            }
        }

        public void draw(DrawContext dc, ShapeAttributes attributes)
        {
            GL gl = dc.getGL();

            if (!dc.isPickingMode())
            {
                attributes.getInteriorMaterial().apply(gl, GL.GL_FRONT);
                Color sc = attributes.getInteriorMaterial().getDiffuse();
                gl.glColor3ub((byte) sc.getRed(), (byte) sc.getGreen(), (byte) sc.getBlue());
            }

            for (int i = 0; i < this.primTypes.size(); i++)
            {
                IntBuffer ib = this.fillIndexBuffers.get(i);
                gl.glDrawElements(this.primTypes.get(i), ib.limit(), GL.GL_UNSIGNED_INT, ib.rewind());
            }
        }
    }

    private static class TessellatorCallback extends GLUtessellatorCallbackAdapter
    {
        protected int numIndices;
        protected int currentType;
        protected ArrayList<Integer> currentPrim;
        protected ArrayList<ArrayList<Integer>> prims = new ArrayList<ArrayList<Integer>>();
        protected ArrayList<Integer> primTypes = new ArrayList<Integer>();

        public ArrayList<ArrayList<Integer>> getPrims()
        {
            return prims;
        }

        public ArrayList<Integer> getPrimTypes()
        {
            return primTypes;
        }

        public int getNumIndices()
        {
            return this.numIndices;
        }

        public void begin(int type)
        {
            this.currentType = type;
            this.currentPrim = new ArrayList<Integer>();
        }

        public void vertex(Object vertexData)
        {
            this.currentPrim.add((Integer) vertexData);
            ++this.numIndices;
        }

        @Override
        public void end()
        {
            this.primTypes.add(this.currentType);
            this.prims.add(this.currentPrim);

            this.currentPrim = null;
        }

        public void combine(double[] coords, Object[] data, float[] weight, Object[] outData)
        {
//            System.out.println("COMBINE CALLED");
            outData[0] = data[0];
        }
    }
}
