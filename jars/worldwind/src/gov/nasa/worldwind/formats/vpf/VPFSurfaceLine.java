/*
Copyright (C) 2001, 2010 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.formats.vpf;

import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.util.*;

import javax.media.opengl.GL;
import java.util.Arrays;

/**
 * @author dcollins
 * @version $Id: VPFSurfaceLine.java 13349 2010-04-28 00:04:34Z dcollins $
 */
public class VPFSurfaceLine extends SurfacePolyline // TODO: consolidate with SurfacePolylines
{
    protected Sector sector;
    protected CompoundVecBuffer buffer;
    protected LatLon referenceLocation;

    public VPFSurfaceLine(VPFFeature feature, VPFPrimitiveData primitiveData)
    {
        String primitiveName = feature.getFeatureClass().getPrimitiveTableName();
        int[] primitiveIds = feature.getPrimitiveIds();

        this.sector = feature.getBounds().toSector();
        this.buffer = (CompoundVecBuffer) primitiveData.getPrimitiveCoords(primitiveName).subCollection(primitiveIds);
        this.referenceLocation = feature.getBounds().toSector().getCentroid();
    }

    protected Iterable<? extends Sector> computeSectors(Globe globe, double texelSizeRadians)
    {
        return getSurfaceShapeSupport().adjustSectorsByBorderWidth(
            Arrays.asList(this.sector), this.attributes.getOutlineWidth(), texelSizeRadians);
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

    protected void assembleRenderState(DrawContext dc, Sector sector, int x, int y, int width, int height)
    {
        // Intentionally left blank in order to override the superclass behavior with nothing.
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

            int drawMode = (this.isClosed() ? GL.GL_LINE_LOOP : GL.GL_LINE_STRIP);
            this.buffer.bindAsVertexBuffer(dc);
            this.buffer.multiDrawArrays(dc, drawMode);
        }
        finally
        {
            ogsh.pop(gl);
        }
    }
}
