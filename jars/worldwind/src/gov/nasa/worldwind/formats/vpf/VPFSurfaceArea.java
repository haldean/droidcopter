/*
Copyright (C) 2001, 2010 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.formats.vpf;

import com.sun.opengl.util.BufferUtil;
import gov.nasa.worldwind.Disposable;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.util.*;

import javax.media.opengl.*;
import javax.media.opengl.glu.*;
import java.awt.*;
import java.nio.IntBuffer;
import java.util.Arrays;

/**
 * @author dcollins
 * @version $Id: VPFSurfaceArea.java 13349 2010-04-28 00:04:34Z dcollins $
 */
public class VPFSurfaceArea extends SurfacePolygon implements Disposable // TODO: consolidate with SurfacePolygons
{
    protected VPFFeature feature;
    protected VPFPrimitiveData primitiveData;
    protected CompoundVecBuffer buffer;
    protected int interiorDisplayList;
    protected LatLon referenceLocation;

    public VPFSurfaceArea(VPFFeature feature, VPFPrimitiveData primitiveData)
    {
        this.feature = feature;
        this.primitiveData = primitiveData;
        this.buffer = computeAreaFeatureCoords(feature, primitiveData);
        this.referenceLocation = feature.getBounds().toSector().getCentroid();
    }

    protected static CompoundVecBuffer computeAreaFeatureCoords(VPFFeature feature, VPFPrimitiveData primitiveData)
    {
        final int numEdges = traverseAreaEdges(feature, primitiveData, null);
        final IntBuffer offsetBuffer = BufferUtil.newIntBuffer(numEdges);
        final IntBuffer lengthBuffer = BufferUtil.newIntBuffer(numEdges);
        final CompoundVecBuffer buffer = primitiveData.getPrimitiveCoords(VPFConstants.EDGE_PRIMITIVE_TABLE);

        traverseAreaEdges(feature, primitiveData, new EdgeListener()
        {
            public void nextEdge(int edgeId, VPFPrimitiveData.EdgeInfo edgeInfo)
            {
                offsetBuffer.put(buffer.getSubPositionBuffer().get(edgeId));
                lengthBuffer.put(buffer.getSubLengthBuffer().get(edgeId));
            }
        });

        offsetBuffer.rewind();
        lengthBuffer.rewind();

        return new CompoundVecBuffer(buffer.getBackingBuffer(), offsetBuffer, lengthBuffer, numEdges, null);
    }

    protected interface EdgeListener
    {
        void nextEdge(int edgeId, VPFPrimitiveData.EdgeInfo edgeInfo);
    }

    protected static int traverseAreaEdges(VPFFeature feature, VPFPrimitiveData primitiveData, EdgeListener listener)
    {
        int count = 0;

        String primitiveName = feature.getFeatureClass().getPrimitiveTableName();

        for (int id : feature.getPrimitiveIds())
        {
            VPFPrimitiveData.FaceInfo faceInfo = (VPFPrimitiveData.FaceInfo) primitiveData.getPrimitiveInfo(
                primitiveName, id);

            VPFPrimitiveData.Ring outerRing = faceInfo.getOuterRing();
            count += traverseRingEdges(outerRing, primitiveData, listener);

            for (VPFPrimitiveData.Ring ring : faceInfo.getInnerRings())
            {
                count += traverseRingEdges(ring, primitiveData, listener);
            }
        }

        return count;
    }

    protected static int traverseRingEdges(VPFPrimitiveData.Ring ring, VPFPrimitiveData primitiveData,
        EdgeListener listener)
    {
        int count = 0;

        for (int edgeId : ring.edgeId)
        {
            VPFPrimitiveData.EdgeInfo edgeInfo = (VPFPrimitiveData.EdgeInfo)
                primitiveData.getPrimitiveInfo(VPFConstants.EDGE_PRIMITIVE_TABLE, edgeId);

            if (!edgeInfo.isOnTileBoundary())
            {
                if (listener != null)
                    listener.nextEdge(edgeId, edgeInfo);
                count++;
            }
        }

        return count;
    }

    protected Iterable<? extends Sector> computeSectors(Globe globe, double texelSizeRadians)
    {
        return getSurfaceShapeSupport().adjustSectorsByBorderWidth(
            Arrays.asList(this.feature.getBounds().toSector()), this.attributes.getOutlineWidth(), texelSizeRadians);
    }

    public Iterable<? extends LatLon> getLocations()
    {
        return this.buffer.getLocations();
    }

    public void setLocations(Iterable<? extends LatLon> iterable)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Position getReferencePosition()
    {
        return new Position(this.referenceLocation, 0d);
    }

    public void dispose()
    {
        GLContext glContext = GLContext.getCurrent();
        if (glContext == null)
            return;

        if (this.interiorDisplayList > 0)
        {
            glContext.getGL().glDeleteLists(this.interiorDisplayList, 1);
            this.interiorDisplayList = 0;
        }
    }

    protected void assembleRenderState(DrawContext dc, Sector sector, int x, int y, int width, int height)
    {
        // Intentionally left blank in order to override the superclass behavior with nothing.
    }

    protected void doRenderInteriorToRegion(DrawContext dc, Sector sector, int x, int y, int width, int height)
    {
        // Concave shape makes no assumptions about the nature or structure of the shape's vertices. The interior is
        // treated as a potentially complex polygon, and this code will do its best to rasterize that polygon. The
        // outline is treated as a simple line loop, regardless of whether the shape's vertices actually define a
        // closed path.

        GL gl = dc.getGL();
        OGLStackHandler ogsh = new OGLStackHandler();
        ogsh.pushModelview(gl);
        try
        {
            Matrix transform = Matrix.fromGeographicToViewport(sector, x, y, width, height);
            double[] matrixArray = transform.toArray(new double[16], 0, false);
            gl.glMultMatrixd(matrixArray, 0);

            // Apply interior attributes using a reference location of (0, 0), because VPFSurfaceArea's coordinates
            // are not offset with respect to a reference location.
            getSurfaceShapeSupport().applyInteriorState(dc, this.attributes, this.getInteriorTexture(), sector,
                new Rectangle(x, y, width, height), LatLon.ZERO);

            if (this.interiorDisplayList <= 0)
            {
                this.interiorDisplayList = gl.glGenLists(1);
                gl.glNewList(this.interiorDisplayList, GL.GL_COMPILE);
                // Tessellate the interior vertices using a reference location of (0, 0), because VPFSurfaceArea's
                // coordinates do not neet to be offset with respect to a reference location.
                this.tessellateInterior(dc, new SurfaceConcaveShape.ImmediateModeCallback(dc), LatLon.ZERO);
                gl.glEndList();
            }

            gl.glCallList(this.interiorDisplayList);
        }
        finally
        {
            ogsh.pop(gl);
        }
    }

    protected void doRenderOutlineToRegion(DrawContext dc, Sector sector, int x, int y, int width, int height)
    {
        GL gl = dc.getGL();
        OGLStackHandler ogsh = new OGLStackHandler();
        ogsh.pushModelview(gl);
        try
        {
            Matrix transform = Matrix.fromGeographicToViewport(sector, x, y, width, height);
            double[] matrixArray = transform.toArray(new double[16], 0, false);
            gl.glMultMatrixd(matrixArray, 0);

            getSurfaceShapeSupport().applyOutlineState(dc, this.attributes);

            // Edges features are not necessarily closed loops, therefore each edge must be rendered as separate line strip.
            this.buffer.bindAsVertexBuffer(dc);
            this.buffer.multiDrawArrays(dc, GL.GL_LINE_STRIP);
        }
        finally
        {
            ogsh.pop(gl);
        }
    }

    protected WWTexture getInteriorTexture()
    {
        if (this.attributes.getInteriorImageSource() == null)
        {
            this.texture = null;
        }
        else if (this.texture == null || this.texture.getImageSource() != this.attributes.getInteriorImageSource())
        {
            this.texture = new BasicWWTexture(this.attributes.getInteriorImageSource(),
                ((VPFSymbolAttributes) this.attributes).isMipMapIconImage());
        }

        return this.texture;
    }

    protected void doTessellate(DrawContext dc, GLU glu, GLUtessellator tess, GLUtessellatorCallback callback,
        LatLon referenceLocation)
    {
        String primitiveName = this.feature.getFeatureClass().getPrimitiveTableName();

        // Setup the winding order to correctly tessellate the outer and inner rings. The outer ring is specified
        // with a clockwise winding order, while inner rings are specified with a counter-clockwise order. Inner
        // rings are subtracted from the outer ring, producing an area with holes.
        glu.gluTessProperty(tess, GLU.GLU_TESS_WINDING_RULE, GLU.GLU_TESS_WINDING_NEGATIVE);
        glu.gluTessBeginPolygon(tess, null);

        for (int id : this.feature.getPrimitiveIds())
        {
            VPFPrimitiveData.FaceInfo faceInfo = (VPFPrimitiveData.FaceInfo) primitiveData.getPrimitiveInfo(
                primitiveName, id);

            this.tessellateRing(glu, tess, faceInfo.getOuterRing());

            for (VPFPrimitiveData.Ring ring : faceInfo.getInnerRings())
            {
                this.tessellateRing(glu, tess, ring);
            }
        }

        glu.gluTessEndPolygon(tess);
    }

    protected void tessellateRing(GLU glu, GLUtessellator tess, VPFPrimitiveData.Ring ring)
    {
        CompoundVecBuffer buffer = this.primitiveData.getPrimitiveCoords(VPFConstants.EDGE_PRIMITIVE_TABLE);
        glu.gluTessBeginContour(tess);

        int numEdges = ring.getNumEdges();
        for (int i = 0; i < numEdges; i++)
        {
            VecBuffer vecBuffer = buffer.getSubBuffer(ring.getEdgeId(i));
            Iterable<double[]> iterable = (ring.getEdgeOrientation(i) < 0) ?
                vecBuffer.getReverseCoords(3) : vecBuffer.getCoords(3);

            for (double[] coords : iterable)
            {
                glu.gluTessVertex(tess, coords, 0, coords);
            }
        }

        glu.gluTessEndContour(tess);
    }
}
