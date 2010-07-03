/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.util.*;

import javax.media.opengl.GL;
import java.awt.*;

/**
 * @author dcollins
 * @version $Id: SurfaceShapeSupport.java 13327 2010-04-22 02:34:39Z dcollins $
 */
public class SurfaceShapeSupport
{
    protected VecBuffer vertexBuffer;
    protected double[] vertexArray;
    protected OGLStackHandler stackHandler = new OGLStackHandler();

    protected final static double[] planeS = new double[] {1, 0, 0, 1};
    protected final static double[] planeT = new double[] {0, 1, 0, 1};

    public SurfaceShapeSupport()
    {
    }

    //**************************************************************//
    //********************  Shape Rendering  ***********************//
    //**************************************************************//

    public void beginRendering(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        GL gl = dc.getGL();

        this.stackHandler.pushAttrib(gl, GL.GL_COLOR_BUFFER_BIT // For alpha test func and ref, blend func
            | GL.GL_CURRENT_BIT     // For current color.
            | GL.GL_ENABLE_BIT      // For disable depth test.
            | GL.GL_LINE_BIT        // For line width, line smooth, line stipple.
            | GL.GL_POLYGON_BIT     // For cull enable and cull face.
            | GL.GL_TEXTURE_BIT     // For texture binding and texture enable/disable.
            | GL.GL_TRANSFORM_BIT); // For matrix mode.

        this.stackHandler.pushClientAttrib(gl, GL.GL_CLIENT_VERTEX_ARRAY_BIT);

        this.stackHandler.pushTextureIdentity(gl);
        this.stackHandler.pushProjection(gl);
        this.stackHandler.pushModelview(gl);

        // Enable the alpha test.
        gl.glEnable(GL.GL_ALPHA_TEST);
        gl.glAlphaFunc(GL.GL_GREATER, 0.0f);

        // Disable the depth test.
        gl.glDisable(GL.GL_DEPTH_TEST);

        // Enable backface culling.
        gl.glEnable(GL.GL_CULL_FACE);
        gl.glCullFace(GL.GL_BACK);

        // Enable client vertex arrays.
        gl.glEnableClientState(GL.GL_VERTEX_ARRAY);

        // Enable blending.
        if (!dc.isPickingMode())
        {
            gl.glEnable(GL.GL_BLEND);
        }
    }

    public void endRendering(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        GL gl = dc.getGL();

        this.stackHandler.pop(gl);
    }

    public void applyModelviewTransform(DrawContext dc, Sector sector, int x, int y, int width, int height,
        LatLon referenceLocation)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (sector == null)
        {
            String message = Logging.getMessage("nullValue.SectorIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (referenceLocation == null)
        {
            String message = Logging.getMessage("nullValue.LatLonIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        double refLatDegrees = referenceLocation.getLatitude().degrees;
        double refLonDegrees = referenceLocation.getLongitude().degrees;

        Matrix transform = Matrix.IDENTITY;
        transform = transform.multiply(Matrix.fromGeographicToViewport(sector, x, y, width, height));
        transform = transform.multiply(Matrix.fromTranslation(refLonDegrees, refLatDegrees, 0d));

        double[] matrixArray = transform.toArray(new double[16], 0, false);
        dc.getGL().glMultMatrixd(matrixArray, 0);
    }

    public void applyColorState(DrawContext dc, Color color, double opacity)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (color == null)
        {
            String message = Logging.getMessage("nullValue.ColorIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        GL gl = dc.getGL();

        if (!dc.isPickingMode())
        {
            // Convert the floating point opacity from the range [0, 1] to the unsigned byte range [0, 255].
            int alpha = (int) (255 * opacity + 0.5);
            // Apply blending in non-premultiplied color mode, and set the current RGBA color to the specified color and
            // opacity.
            OGLUtil.applyBlending(gl, false);
            gl.glColor4ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue(), (byte) alpha);
        }

        // Disable textures.
        gl.glDisable(GL.GL_TEXTURE_2D);
        gl.glDisable(GL.GL_TEXTURE_GEN_S);
        gl.glDisable(GL.GL_TEXTURE_GEN_T);
    }

    public void applyTextureState(DrawContext dc, WWTexture texture, Sector sector, Rectangle drawRect,
        double scale, double opacity, LatLon refPos)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (texture == null)
        {
            String message = Logging.getMessage("nullValue.TextureIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (sector == null)
        {
            String message = Logging.getMessage("nullValue.SectorIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (drawRect == null)
        {
            String message = Logging.getMessage("nullValue.RectangleIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (refPos == null)
        {
            String message = Logging.getMessage("nullValue.LatLonIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        GL gl = dc.getGL();

        if (!texture.bind(dc))
            return;

        if (!dc.isPickingMode())
        {
            // Apply blending in premultiplied color mode, and set the current RGBA color to white, with the specified
            // opacity.
            OGLUtil.applyBlending(gl, true);
            OGLUtil.applyColor(gl, Color.WHITE, opacity, true);
        }

        // Texture coordinates generation
        this.applyTextureCoordinateGenerationState(dc, sector, drawRect);

        // Texture transform
        this.applyTextureTransform(dc, texture, scale, sector, drawRect, refPos);

        // Texture setup
        gl.glEnable(GL.GL_TEXTURE_2D);
        gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_MODULATE);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_REPEAT);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_REPEAT);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    protected void applyTextureCoordinateGenerationState(DrawContext dc, Sector sector, Rectangle drawRect)
    {
        GL gl = dc.getGL();
        gl.glTexGeni(GL.GL_S, GL.GL_TEXTURE_GEN_MODE, GL.GL_OBJECT_LINEAR);
        gl.glTexGeni(GL.GL_T, GL.GL_TEXTURE_GEN_MODE, GL.GL_OBJECT_LINEAR);
        gl.glTexGendv(GL.GL_S, GL.GL_OBJECT_PLANE, planeS, 0);
        gl.glTexGendv(GL.GL_T, GL.GL_OBJECT_PLANE, planeT, 0);
        gl.glEnable(GL.GL_TEXTURE_GEN_S);
        gl.glEnable(GL.GL_TEXTURE_GEN_T);
    }

    protected void applyTextureTransform(DrawContext dc, WWTexture texture, double scale, Sector sector,
        Rectangle drawRect, LatLon referenceLocation)
    {
        GL gl = dc.getGL();

        Matrix transform = Matrix.IDENTITY;

        // Translate geographic coordinates to the reference location.
        double refLatDegrees = referenceLocation.getLatitude().degrees;
        double refLonDegrees = referenceLocation.getLongitude().degrees;
        transform = Matrix.fromTranslation(refLonDegrees, refLatDegrees, 0d).multiply(transform);
        
        // Premultiply pattern scaling and cos latitude to compensate latitude distortion on x
        double cosLat = referenceLocation.getLatitude().cos();
        transform = Matrix.fromScale(cosLat / scale, 1d / scale, 1d).multiply(transform);

        // To maintain the pattern apparent size, we scale it so that one texture pixel match one draw tile pixel.
        double regionPixelSize = dc.getGlobe().getRadius() * sector.getDeltaLatRadians() / drawRect.height;
        double texturePixelSize = dc.getGlobe().getRadius() * Angle.fromDegrees(1).radians / texture.getHeight(dc);
        double drawScale = texturePixelSize / regionPixelSize;
        transform = Matrix.fromScale(drawScale, drawScale, 1d).multiply(transform); // Pre multiply

        // Apply texture coordinates transform
        double[] matrixArray = transform.toArray(new double[16], 0, false);
        gl.glMatrixMode(GL.GL_TEXTURE);
        gl.glLoadIdentity();
        texture.applyInternalTransform(dc);
        gl.glMultMatrixd(matrixArray, 0);
        gl.glMatrixMode(GL.GL_MODELVIEW);
    }

    public void applyInteriorState(DrawContext dc, ShapeAttributes attributes)
    {
        this.applyInteriorState(dc, attributes, null, null, null, null);
    }

    public void applyInteriorState(DrawContext dc, ShapeAttributes attributes, WWTexture texture, Sector sector,
        Rectangle drawRect, LatLon refPos)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (attributes == null)
        {
            String message = Logging.getMessage("nullValue.AttributesIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (texture != null && (sector == null || drawRect == null))
        {
            String message = Logging.getMessage(
                sector == null ? "nullValue.SectorIsNull" : "nullValue.RectangleIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (texture == null || dc.isPickingMode())
        {
            this.applyColorState(dc, attributes.getInteriorMaterial().getDiffuse(), attributes.getInteriorOpacity());
        }
        else
        {
            this.applyTextureState(dc, texture, sector, drawRect,
                attributes.getInteriorImageScale(), attributes.getInteriorOpacity(), refPos);
        }
    }

    public void applyOutlineState(DrawContext dc, ShapeAttributes attributes)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (attributes == null)
        {
            String message = Logging.getMessage("nullValue.AttributesIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        GL gl = dc.getGL();
        // Apply line width state
        double lineWidth = attributes.getOutlineWidth();
        if (dc.isPickingMode() && !attributes.isDrawInterior())
        {
            if (lineWidth != 0)
                lineWidth += 5;
        }
        gl.glLineWidth((float) lineWidth);
        // Apply line smooth state
        if (!dc.isPickingMode() && attributes.isEnableAntialiasing())
        {
            gl.glEnable(GL.GL_LINE_SMOOTH);
        }
        else
        {
            gl.glDisable(GL.GL_LINE_SMOOTH);
        }
        // Apply line stipple state.
        if (dc.isPickingMode() || (attributes.getOutlineStippleFactor() <= 0))
        {
            gl.glDisable(GL.GL_LINE_STIPPLE);
        }
        else
        {
            gl.glEnable(GL.GL_LINE_STIPPLE);
            gl.glLineStipple(
                attributes.getOutlineStippleFactor(),
                attributes.getOutlineStipplePattern());
        }

        this.applyColorState(dc, attributes.getOutlineMaterial().getDiffuse(), attributes.getOutlineOpacity());
    }

    public void drawLocations(DrawContext dc, int drawMode, Iterable<? extends LatLon> locations, int count,
        LatLon referenceLocation)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (locations == null)
        {
            String message = Logging.getMessage("nullValue.IterableIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (referenceLocation == null)
        {
            String message = Logging.getMessage("nullValue.LatLonIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        double[] array = this.getVertexArray(count);
        VecBuffer buffer = this.getVertexBuffer(count);

        int index = 0;
        for (LatLon ll : locations)
        {
            array[index++] = ll.getLongitude().degrees - referenceLocation.getLongitude().degrees;
            array[index++] = ll.getLatitude().degrees - referenceLocation.getLatitude().degrees;
        }

        buffer.putAll(0, array, count);
        buffer.bindAsVertexBuffer(dc);
        buffer.drawArrays(dc, drawMode);
    }

    protected double[] getVertexArray(int size)
    {
        if (this.vertexArray == null || this.vertexArray.length < 2 * size)
        {
            this.vertexArray = new double[2 * size];
        }

        return this.vertexArray;
    }

    protected VecBuffer getVertexBuffer(int size)
    {
        if (this.vertexBuffer == null || this.vertexBuffer.getSize() < size)
        {
            this.vertexBuffer = new VecBuffer(2, size, new BufferFactory.DoubleBufferFactory());
        }

        return (this.vertexBuffer.getSize() > size) ? this.vertexBuffer.getSubBuffer(0, size) : this.vertexBuffer;
    }

    //**************************************************************//
    //********************  Shape Sector Assembly  *****************//
    //**************************************************************//

    @SuppressWarnings({"StringEquality"})
    public Iterable<? extends Sector> computeBoundingSectors(Iterable<? extends LatLon> locations, String pathType)
    {
        if (locations == null)
        {
            String message = Logging.getMessage("nullValue.IterableIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Iterable<? extends Sector> sectors;

        if (LatLon.locationsCrossDateLine(locations))
        {
            sectors = java.util.Arrays.asList(Sector.splitBoundingSectors(locations));
        }
        else
        {
            sectors = java.util.Arrays.asList(Sector.boundingSector(locations));
        }

        // Great circle paths between two latitudes may result in a latitude which is greater or smaller than either of
        // the two latitudes. All other path types are bounded by the defining locations.
        if (pathType != null && pathType == AVKey.GREAT_CIRCLE)
        {
            java.util.List<Sector> adjustedSectors = new java.util.ArrayList<Sector>();

            for (Sector sector : sectors)
            {
                Angle[] extremeLatitudes = this.greatArcExtremeLatitudes(locations);

                double minLatDegrees = sector.getMinLatitude().degrees;
                double maxLatDegrees = sector.getMaxLatitude().degrees;

                if (minLatDegrees > extremeLatitudes[0].degrees)
                    minLatDegrees = extremeLatitudes[0].degrees;
                if (maxLatDegrees < extremeLatitudes[1].degrees)
                    maxLatDegrees = extremeLatitudes[1].degrees;

                Angle minLat = Angle.fromDegreesLatitude(minLatDegrees);
                Angle maxLat = Angle.fromDegreesLatitude(maxLatDegrees);

                adjustedSectors.add(new Sector(minLat, maxLat, sector.getMinLongitude(), sector.getMaxLongitude()));
            }

            sectors = adjustedSectors;
        }

        return sectors;
    }

    /**
     * Returns a copy of the specified sectors, increased in size outward from their centroids according to the
     * specified line width in texels, and texel size in arc radians. This computation compensates for lines which
     * straddle the sector's perimeter, which implicitly increases the sector's size.
     *
     * @param sectors          the sectors to expand according to the line width and texel size.
     * @param lineWidth        the line width in texels straddling the sector boundaries.
     * @param texelSizeRadians the size of a line texel in arc radians.
     *
     * @return a copy of the specified sectors, adjusted according to the specified line width and texel size.
     *
     * @throws IllegalArgumentException if sectors is null, or if lineWidth is less than zero.
     */
    public Iterable<? extends Sector> adjustSectorsByBorderWidth(Iterable<? extends Sector> sectors, double lineWidth,
        double texelSizeRadians)
    {
        if (sectors == null)
        {
            String message = Logging.getMessage("nullValue.IterableIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (lineWidth < 0)
        {
            String message = Logging.getMessage("Geom.LineWidthInvalid", lineWidth);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        // TODO
        // we are adjusting the bounding sectors to accomodate border width after the dateline split. This means the
        // extra room for the border will not extend across the dateline.

        // Compute the latitude or longitude delta degrees which the border extends beyond the shape fill geometry.
        // Since the border straddles the shape fill geometry, the delta will be 1/2 of the actual border size.
        double borderPaddingRadians = texelSizeRadians * lineWidth / 2.0;

        java.util.List<Sector> adjustedSectors = new java.util.ArrayList<Sector>();

        for (Sector sector : sectors)
        {
            double minLatRadians = sector.getMinLatitude().radians - borderPaddingRadians;
            double maxLatRadians = sector.getMaxLatitude().radians + borderPaddingRadians;
            double minLonRadians = sector.getMinLongitude().radians;
            double maxLonRadians = sector.getMaxLongitude().radians;

            if (LatLon.locationsCrossDateLine(sector))
            {
                minLonRadians += borderPaddingRadians;
                maxLonRadians -= borderPaddingRadians;
            }
            else
            {
                minLonRadians -= borderPaddingRadians;
                maxLonRadians += borderPaddingRadians;
            }

            Angle minLat = Angle.fromRadiansLatitude(minLatRadians);
            Angle maxLat = Angle.fromRadiansLatitude(maxLatRadians);
            Angle minLon = Angle.fromRadiansLongitude(minLonRadians);
            Angle maxLon = Angle.fromRadiansLongitude(maxLonRadians);

            adjustedSectors.add(new Sector(minLat, maxLat, minLon, maxLon));
        }

        return adjustedSectors;
    }

    //**************************************************************//
    //********************  Shape Location Assembly  ***************//
    //**************************************************************//

    public void generateIntermediateLocations(Iterable<? extends LatLon> locations,
        String pathType, double edgeIntervalsPerDegree, int minEdgeIntervals, int maxEdgeIntervals,
        boolean makeClosedPath,
        java.util.List<LatLon> outLocations)
    {
        if (locations == null)
        {
            String message = Logging.getMessage("nullValue.IterableIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (outLocations == null)
        {
            String message = Logging.getMessage("nullValue.LocationsListIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        LatLon firstLocation = null;
        LatLon lastLocation = null;

        for (LatLon ll : locations)
        {
            if (firstLocation == null)
            {
                firstLocation = ll;
            }

            if (lastLocation != null)
            {
                this.addIntermediateLocations(lastLocation, ll, pathType,
                    edgeIntervalsPerDegree, minEdgeIntervals, maxEdgeIntervals, outLocations);
            }

            outLocations.add(ll);
            lastLocation = ll;
        }

        // If the caller has instructed us to generate locations for a closed path, then check to see if the specified
        // locations define a closed path. If not, then we need to generate intermediate locations between the last
        // and first locations, then close the path by repeating the first location.
        if (makeClosedPath)
        {
            if (firstLocation != null && lastLocation != null && !firstLocation.equals(lastLocation))
            {
                this.addIntermediateLocations(lastLocation, firstLocation, pathType,
                    edgeIntervalsPerDegree, minEdgeIntervals, maxEdgeIntervals, outLocations);
                outLocations.add(firstLocation);
            }
        }
    }

    protected Angle[] greatArcExtremeLatitudes(Iterable<? extends LatLon> locations)
    {
        double minLat = Angle.POS90.degrees;
        double maxLat = Angle.NEG90.degrees;

        LatLon lastLocation = null;

        for (LatLon ll : locations)
        {
            if (lastLocation != null)
            {
                LatLon[] extremes = LatLon.greatCircleArcExtremeLocations(lastLocation, ll);
                if (extremes != null)
                {
                    for (LatLon extreme : extremes)
                    {
                        if (minLat > extreme.getLatitude().degrees)
                            minLat = extreme.getLatitude().degrees;
                        if (maxLat < extreme.getLatitude().degrees)
                            maxLat = extreme.getLatitude().degrees;
                    }
                }
            }

            lastLocation = ll;
        }

        return new Angle[] {Angle.fromDegrees(minLat), Angle.fromDegrees(maxLat)};
    }

    @SuppressWarnings({"StringEquality"})
    protected void addIntermediateLocations(LatLon a, LatLon b, String pathType,
        double edgeIntervalsPerDegree, int minEdgeIntervals, int maxEdgeIntervals, java.util.List<LatLon> outLocations)
    {
        if (pathType != null && pathType == AVKey.GREAT_CIRCLE)
        {
            Angle pathLength = LatLon.greatCircleDistance(a, b);

            double edgeIntervals = WWMath.clamp(edgeIntervalsPerDegree * pathLength.degrees,
                minEdgeIntervals, maxEdgeIntervals);
            int numEdgeIntervals = (int) Math.ceil(edgeIntervals);

            if (numEdgeIntervals > 1)
            {
                double headingRadians = LatLon.greatCircleAzimuth(a, b).radians;
                double stepSizeRadians = pathLength.radians / (numEdgeIntervals + 1);

                for (int i = 1; i <= numEdgeIntervals; i++)
                {
                    LatLon newLocation = LatLon.greatCircleEndPosition(a, headingRadians, i * stepSizeRadians);
                    outLocations.add(newLocation);
                }
            }
        }
        else if (pathType != null && (pathType == AVKey.RHUMB_LINE || pathType == AVKey.LOXODROME))
        {
            Angle pathLength = LatLon.rhumbDistance(a, b);

            double edgeIntervals = WWMath.clamp(edgeIntervalsPerDegree * pathLength.degrees,
                minEdgeIntervals, maxEdgeIntervals);
            int numEdgeIntervals = (int) Math.ceil(edgeIntervals);

            if (numEdgeIntervals > 1)
            {
                double headingRadians = LatLon.rhumbAzimuth(a, b).radians;
                double stepSizeRadians = pathLength.radians / (numEdgeIntervals + 1);

                for (int i = 1; i <= numEdgeIntervals; i++)
                {
                    LatLon newLocation = LatLon.rhumbEndPosition(a, headingRadians, i * stepSizeRadians);
                    outLocations.add(newLocation);
                }
            }
        }
        else // Default to linear interpolation in latitude and longitude.
        {
            // Linear interpolation between 2D coordinates is already performed by GL during shape rasterization.
            // There is no need to duplicate that effort here.
        }
    }

    public void fixDatelineCrossingLocations(Sector sector, Iterable<? extends LatLon> iterable,
        java.util.List<LatLon> outLocations)
    {
        if (sector == null)
        {
            String message = Logging.getMessage("nullValue.SectorIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (iterable == null)
        {
            String message = Logging.getMessage("nullValue.IterableIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (outLocations == null)
        {
            String message = Logging.getMessage("nullValue.LocationsListIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        boolean locationsCrossDateLine = LatLon.locationsCrossDateLine(iterable);
        boolean inWesternHemisphere = sector.getMaxLongitude().degrees < 0;

        for (LatLon ll : iterable)
        {
            double lat = ll.getLatitude().degrees;
            double lon = ll.getLongitude().degrees;

            if (locationsCrossDateLine)
            {
                if (inWesternHemisphere && ll.getLongitude().degrees > 0)
                {
                    lon -= 360;
                }
                else if (!inWesternHemisphere && ll.getLongitude().degrees < 0)
                {
                    lon += 360;
                }
            }

            outLocations.add(LatLon.fromDegrees(lat, lon));
        }
    }

    public void makeClosedPath(java.util.List<LatLon> outLocations)
    {
        if (outLocations == null)
        {
            String message = Logging.getMessage("nullValue.LocationsListIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (outLocations.size() < 2)
            return;

        int lastIndex = outLocations.size() - 1;
        if (!outLocations.get(0).equals(outLocations.get(lastIndex)))
        {
            outLocations.add(outLocations.get(0));
        }
    }

    //**************************************************************//
    //***** Movable interface support for CompoundVecBuffer  *******//
    //**************************************************************//

    public void doMoveTo(CompoundVecBuffer buffer, Position oldReferencePosition, Position newReferencePosition)
    {
        for (int part = 0; part < buffer.getNumSubBuffers(); part++)
        {
            int numPoints = buffer.getSubLengthBuffer().get(part);
            int partOffset = buffer.getSubPositionBuffer().get(part);
            for (int i = 0; i < numPoints; i++)
            {
                LatLon ll = buffer.getBackingBuffer().getLocation(partOffset + i);
                Angle heading = LatLon.greatCircleAzimuth(oldReferencePosition, ll);
                Angle pathLength = LatLon.greatCircleDistance(oldReferencePosition, ll);
                buffer.getBackingBuffer().putLocation(partOffset + i,
                    LatLon.greatCircleEndPosition(newReferencePosition, heading, pathLength));
            }
        }
    }
}
