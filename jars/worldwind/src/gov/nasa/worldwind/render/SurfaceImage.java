/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
 */
package gov.nasa.worldwind.render;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.util.Logging;

import javax.media.opengl.GL;
import java.util.*;

/**
 * Renders a single image contained in a local file or <code>BufferedImage</code>
 *
 * @version $Id: SurfaceImage.java 13339 2010-04-25 23:16:04Z tgaskins $
 */
public class SurfaceImage implements SurfaceTile, Renderable, PreRenderable, Movable, Disposable
{
    // TODO: Handle date-line spanning sectors

    private Sector sector;
    private Position referencePosition;
    private double opacity = 1.0;
    private boolean pickEnabled = true;

    protected WWTexture sourceTexture;
    protected WWTexture generatedTexture;
    protected List<LatLon> corners;
    protected WWTexture previousSourceTexture;
    protected WWTexture previousGeneratedTexture;

    /**
     * Renders a single image tile from a local image source.
     *
     * @param imageSource either the file path to a local image or a <code>BufferedImage</code> reference.
     * @param sector      the sector covered by the image.
     */
    public SurfaceImage(Object imageSource, Sector sector)
    {
        if (imageSource == null)
        {
            String message = Logging.getMessage("nullValue.ImageSource");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (sector == null)
        {
            String message = Logging.getMessage("nullValue.SectorIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        initializeGeometry(sector);
        initializeSourceTexture(imageSource);
    }

    public SurfaceImage(Object imageSource, Iterable<? extends LatLon> corners)
    {
        if (imageSource == null)
        {
            String message = Logging.getMessage("nullValue.ImageSource");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (corners == null)
        {
            String message = Logging.getMessage("nullValue.LocationsListIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        initializeGeometry(corners);
        initializeSourceTexture(imageSource);
    }

    public void dispose()
    {
        this.generatedTexture = null;
    }

    public void setImageSource(Object imageSource, Sector sector)
    {
        this.previousSourceTexture = this.sourceTexture;
        this.previousGeneratedTexture = this.generatedTexture;

        initializeGeometry(sector);
        initializeSourceTexture(imageSource);
    }

    public void setImageSource(Object imageSource, Iterable<? extends LatLon> corners)
    {
        this.previousSourceTexture = this.sourceTexture;
        this.previousGeneratedTexture = this.generatedTexture;

        initializeGeometry(corners);
        initializeSourceTexture(imageSource);
    }

    public boolean isPickEnabled()
    {
        return this.pickEnabled;
    }

    public void setPickEnabled(boolean pickEnabled)
    {
        this.pickEnabled = pickEnabled;
    }

    protected void initializeGeometry(Iterable<? extends LatLon> corners)
    {
        this.corners = new ArrayList<LatLon>(4);
        for (LatLon ll : corners)
        {
            this.corners.add(ll);
        }

        this.sector = Sector.boundingSector(this.corners);
        this.referencePosition = new Position(sector.getCentroid(), 0);
        this.generatedTexture = null;
    }

    protected void initializeSourceTexture(Object imageSource)
    {
        this.sourceTexture = new BasicWWTexture(imageSource, true);
        if (Sector.isSector(this.corners) && sector.isSameSector(this.corners))
            this.generatedTexture = this.sourceTexture;
    }

    public Object getImageSource()
    {
        return this.sourceTexture.getImageSource();
    }

    public double getOpacity()
    {
        return opacity;
    }

    public void setOpacity(double opacity)
    {
        this.opacity = opacity;
    }

    // SurfaceTile interface

    public Sector getSector()
    {
        return this.sector;
    }

    protected void setSector(Sector sector)
    {
        this.sector = sector;
    }

    public void setCorners(Iterable<? extends LatLon> corners)
    {
        if (corners == null)
        {
            String message = Logging.getMessage("nullValue.LocationsListIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.initializeGeometry(corners);
    }

    public List<LatLon> getCorners()
    {
        return new ArrayList<LatLon>(this.corners);
    }

    public Extent getExtent(DrawContext dc)
    {
        if (dc == null)
        {
            String msg = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        return Sector.computeBoundingCylinder(dc.getGlobe(), dc.getVerticalExaggeration(), this.getSector());
    }

    public boolean bind(DrawContext dc)
    {
        return this.generatedTexture != null && this.generatedTexture.bind(dc);
    }

    public void applyInternalTransform(DrawContext dc)
    {
        if (this.generatedTexture != null)
            this.generatedTexture.applyInternalTransform(dc);
    }

    // Renderable interface

    public void render(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        if (dc.isPickingMode() && !this.isPickEnabled())
            return;

        if (!this.getSector().intersects(dc.getVisibleSector()))
            return;

        GL gl = dc.getGL();
        try
        {
            if (!dc.isPickingMode())
            {
                double opacity = dc.getCurrentLayer() != null
                    ? this.getOpacity() * dc.getCurrentLayer().getOpacity() : this.getOpacity();

                if (opacity < 1)
                {
                    gl.glPushAttrib(GL.GL_COLOR_BUFFER_BIT | GL.GL_POLYGON_BIT | GL.GL_CURRENT_BIT);
                    // Enable blending using white premultiplied by the current opacity.
                    gl.glColor4d(opacity, opacity, opacity, opacity);
                }
                else
                {
                    gl.glPushAttrib(GL.GL_COLOR_BUFFER_BIT | GL.GL_POLYGON_BIT);
                }
                gl.glEnable(GL.GL_BLEND);
                gl.glBlendFunc(GL.GL_ONE, GL.GL_ONE_MINUS_SRC_ALPHA);
            }
            else
            {
                gl.glPushAttrib(GL.GL_POLYGON_BIT);
            }

            gl.glPolygonMode(GL.GL_FRONT, GL.GL_FILL);
            gl.glEnable(GL.GL_CULL_FACE);
            gl.glCullFace(GL.GL_BACK);

            dc.getGeographicSurfaceTileRenderer().renderTile(dc, this);
//            this.sitr.renderTile(dc, this);
//            dc.removeKey(AVKey.TEXTURE_COORDINATES);
        }
        finally
        {
            gl.glPopAttrib();
//            if (this.generatedTexture != null && this.generatedTexture != this.sourceTexture)
//            {
//                dc.getTextureCache().remove(this.generatedTexture);
//                this.generatedTexture = null;
//            }
        }
    }

    public void preRender(DrawContext dc)
    {
        if (this.previousGeneratedTexture != null)
        {
            dc.getTextureCache().remove(this.previousGeneratedTexture);
            this.previousGeneratedTexture = null;
        }

        if (this.previousSourceTexture != null)
        {
            dc.getTextureCache().remove(this.previousSourceTexture);
            this.previousSourceTexture = null;
        }

        if (this.generatedTexture == null)
            this.generatedTexture = this.makeGeneratedTexture(dc);
        else if (!this.generatedTexture.isTextureCurrent(dc))
            this.generatedTexture = this.makeGeneratedTexture(dc);
    }

    protected WWTexture makeGeneratedTexture(DrawContext dc)
    {
        if (dc.getGLRuntimeCapabilities().isUseFramebufferObject())
            return new FBOTexture(dc, this.sourceTexture, this.sector, this.corners);
        else
            return new FramebufferTexture(dc, this.sourceTexture, this.sector, this.corners);
    }

    // --- Movable interface ---

    public void move(Position delta)
    {
        if (delta == null)
        {
            String msg = Logging.getMessage("nullValue.PositionIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        this.moveTo(this.getReferencePosition().add(delta));
    }

    public void moveTo(Position position)
    {
        LatLon oldRef = this.getReferencePosition();

        for (int i = 0; i < this.corners.size(); i++)
        {
            LatLon p = this.corners.get(i);
            double distance = LatLon.greatCircleDistance(oldRef, p).radians;
            double azimuth = LatLon.greatCircleAzimuth(oldRef, p).radians;
            LatLon pp = LatLon.greatCircleEndPosition(position, azimuth, distance);
            this.corners.set(i, pp);
        }

        this.setCorners(this.corners);
    }

    public Position getReferencePosition()
    {
        return this.referencePosition;
    }

    protected void setReferencePosition(Position referencePosition)
    {
        this.referencePosition = referencePosition;
    }

    public boolean equals(Object o)
    {
        if (this == o)
            return true;

        if (o == null || this.getClass() != o.getClass())
            return false;

        SurfaceImage that = (SurfaceImage) o;
        return this.sourceTexture.getImageSource().equals(that.getImageSource())
            && this.getSector().equals(that.getSector());
    }

    public int hashCode()
    {
        int result;
        result = this.getImageSource().hashCode();
        result = 31 * result + this.getSector().hashCode();
        return result;
    }

//    protected WWTexture makeGeneratedTexture(DrawContext dc)
//    {
//
//    private SurfaceImageTileRenderer sitr = new SurfaceImageTileRenderer();
//
//    private class SurfaceImageTileRenderer extends GeographicSurfaceTileRenderer
//    {
//        private SectorGeometry sg;
//
//        @Override
//        protected void preComputeTextureTransform(DrawContext dc, SectorGeometry sg, Transform t)
//        {
//            this.sg = sg;
//
//            // No translation and no scaling
//            t.HShift = 0;
//            t.VShift = 0;
//            t.HScale = 1;
//            t.VScale = 1;
//        }
//
//        @Override
//        protected void computeTextureTransform(DrawContext dc, SurfaceTile tile, Transform t)
//        { // TODO: cache computed texture coordinates. compute them only when necessary re: render/pick
//            dc.setValue(AVKey.TEXTURE_COORDINATES, this.computeTextureCoords(dc, this.sg, tile));
//        }
//
//        @SuppressWarnings({"UnusedDeclaration"})
//        DoubleBuffer computeTextureCoords(DrawContext dc, SectorGeometry sg, SurfaceTile tile)
//        {
//            Vec4[] llp = new Vec4[4];
//            int i = 0;
//            for (LatLon corner : tile.getCorners())
//            {
//                llp[i++] = new Vec4(corner.getLongitude().radians, corner.getLatitude().radians, 0);
//            }
//            final BarycentricQuadrilateral bcq = new BarycentricQuadrilateral(llp[0], llp[1], llp[2], llp[3]);
//
//            return sg.makeTextureCoordinates(new SectorGeometry.GeographicTextureCoordinateComputer()
//            {
//                public double[] compute(Angle latitude, Angle longitude)
//                {
//                    return bcq.getBilinearCoords(new Vec4(longitude.radians, latitude.radians, 0));
//                }
//            });
//        }
//    }
}