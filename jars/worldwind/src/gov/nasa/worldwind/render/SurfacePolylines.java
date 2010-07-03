/*
Copyright (C) 2001, 2010 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render;

import gov.nasa.worldwind.Disposable;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.util.*;

import javax.media.opengl.*;
import java.util.*;

/**
 * This class renders fast multiple surface polylines in one pass. It relies on a {@link CompoundVecBuffer}.
 *
 * @author Dave Collins
 * @author Patrick Murris
 * @version $Id: SurfacePolylines.java 13349 2010-04-28 00:04:34Z dcollins $
 */
public class SurfacePolylines extends AbstractSurfaceShape implements Disposable // TODO: Review
{
    protected Iterable<? extends Sector> sectors;
    protected CompoundVecBuffer buffer;
    protected int outlineDisplayList;
    protected boolean needsOutlineTessellation = true;
    protected boolean crossesDateLine = false;

    public SurfacePolylines(CompoundVecBuffer buffer)
    {
        if (buffer == null)
        {
            String message = Logging.getMessage("nullValue.BufferIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.buffer = buffer;
    }

    public SurfacePolylines(Sector sector, CompoundVecBuffer buffer)
    {
        if (sector == null)
        {
            String message = Logging.getMessage("nullValue.SectorIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (buffer == null)
        {
            String message = Logging.getMessage("nullValue.BufferIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.sectors = Arrays.asList(sector);
        this.buffer = buffer;
    }

    /**
     * Get the underlying {@link CompoundVecBuffer} describing the geometry.
     *
     * @return the underlying {@link CompoundVecBuffer}.
     */
    public CompoundVecBuffer getBuffer()
    {
        return this.buffer;
    }

    public Iterable<? extends LatLon> getLocations(Globe globe)
    {
        if (globe == null)
        {
            String message = Logging.getMessage("nullValue.GlobeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return this.getLocations();
    }

    protected Iterable<? extends LatLon> getLocations(Globe globe, double edgeIntervalsPerDegree)
    {
        return getLocations(globe);
    }

    public Iterable<? extends LatLon> getLocations()
    {
        return this.buffer.getLocations();
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setLocations(Iterable<? extends LatLon> iterable)
    {
        throw new UnsupportedOperationException();
    }

    public Position getReferencePosition()
    {
        Iterator<? extends LatLon> iterator = this.getLocations().iterator();
        if (iterator.hasNext())
            return new Position(iterator.next(), 0);

        return null;
    }

    protected Iterable<? extends Sector> computeSectors(Globe globe, double texelSizeRadians)
    {
        if (this.sectors == null)
            this.sectors = getSurfaceShapeSupport().computeBoundingSectors(this.getLocations(), this.getPathType());

        return getSurfaceShapeSupport().adjustSectorsByBorderWidth(
            this.sectors, this.attributes.getOutlineWidth(), texelSizeRadians);
    }

    protected void doMoveTo(Position oldReferencePosition, Position newReferencePosition)
    {
        getSurfaceShapeSupport().doMoveTo(this.getBuffer(), oldReferencePosition, newReferencePosition);
        this.onGeometryChanged();
    }

    protected void onGeometryChanged()
    {
        this.sectors = null;
        this.needsOutlineTessellation = true;
        super.onShapeChanged();
    }

    public void dispose()
    {
        GLContext glContext = GLContext.getCurrent();
        if (glContext == null)
            return;

        if (this.outlineDisplayList > 0)
        {
            glContext.getGL().glDeleteLists(this.outlineDisplayList, 1);
            this.outlineDisplayList = 0;
        }
    }

    protected void assembleRenderState(DrawContext dc, Sector sector, int x, int y, int width, int height)
    {
        // Intentionally left blank in order to override the superclass behavior with nothing.
    }

    protected void doRenderInteriorToRegion(DrawContext dc, Sector sector, int x, int y, int width, int height)
    {
        // Polyline does not render an interior.
    }

    protected void doRenderOutlineToRegion(DrawContext dc, Sector sector, int x, int y, int width, int height)
    {
        Position referencePos = this.getReferencePosition();
        int hemisphereSign = (int) Math.signum(sector.getCentroid().getLongitude().degrees);

        if (this.outlineDisplayList <= 0 || this.needsOutlineTessellation)
        {
            this.tessellateOutline(dc, referencePos);
        }

        GL gl = dc.getGL();
        OGLStackHandler ogsh = new OGLStackHandler();
        ogsh.pushModelview(gl);
        try
        {
            getSurfaceShapeSupport().applyModelviewTransform(dc, sector, x, y, width, height, referencePos);
            getSurfaceShapeSupport().applyOutlineState(dc, this.attributes);
            gl.glCallList(this.outlineDisplayList);

            if (this.crossesDateLine)
            {
                // Apply hemisphere offset and draw again
                gl.glTranslated(360 * hemisphereSign, 0, 0);
                gl.glCallList(this.outlineDisplayList);
            }
        }
        finally
        {
            ogsh.pop(gl);
        }
    }

    protected void tessellateOutline(DrawContext dc, LatLon referenceLocation)
    {
        GL gl = dc.getGL();
        this.crossesDateLine = false;

        if (this.outlineDisplayList <= 0)
            this.outlineDisplayList = gl.glGenLists(1);

        gl.glNewList(this.outlineDisplayList, GL.GL_COMPILE);
        try
        {
            // Tessellate each part, note if crossing date line
            for (int i = 0; i < this.buffer.getNumSubBuffers(); i++)
            {
                if (this.tessellatePart(gl, this.buffer.getSubBuffer(i), referenceLocation))
                    this.crossesDateLine = true;
            }
        }
        finally
        {
            gl.glEndList();
        }

        this.needsOutlineTessellation = false;
    }

    protected boolean tessellatePart(GL gl, VecBuffer vecBuffer, LatLon referenceLocation)
    {
        Iterable<double[]> iterable = vecBuffer.getCoords(3);
        boolean dateLineCrossed = false;

        gl.glBegin(GL.GL_LINE_STRIP);
        try
        {
            int sign = 0; // hemisphere offset direction
            double previousLongitude = 0;

            for (double[] coords : iterable)
            {
                if (Math.abs(previousLongitude - coords[0]) > 180)
                {
                    // Crossing date line, sum departure point longitude sign for hemisphere offset
                    sign += (int) Math.signum(previousLongitude);
                    dateLineCrossed = true;
                }

                previousLongitude = coords[0];

                double lonDegrees = coords[0] - referenceLocation.getLongitude().degrees;
                double latDegrees = coords[1] - referenceLocation.getLatitude().degrees;
                lonDegrees += sign * 360; // apply hemisphere offset
                gl.glVertex3d(lonDegrees, latDegrees, 0d);
            }
        }
        finally
        {
            gl.glEnd();
        }

        return dateLineCrossed;
    }
}
