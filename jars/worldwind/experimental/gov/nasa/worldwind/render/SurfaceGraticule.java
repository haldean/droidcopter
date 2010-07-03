/*
Copyright (C) 2001, 2006, 2009 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render;

import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.util.Logging;

import javax.media.opengl.GL;
import java.awt.*;
import java.util.Arrays;

/**
 * Renders a lat-lon graticule.
 *
 * @author Patrick Murris
 * @version $Id: SurfaceGraticule.java 12643 2009-09-24 17:29:55Z dcollins $
 */
public class SurfaceGraticule extends AbstractSurfaceRenderable
{
    protected final static Sector extentSector = Sector.FULL_SPHERE;

    private double minResolution = .01; // degrees
    private double maxResolution = 10; // degrees
    private int density = 14;

    public double getMinResolution()
    {
        return this.minResolution;
    }

    public void setMinResolution(double resolution)
    {
        this.minResolution = resolution;
    }

    public double getMaxResolution()
    {
        return this.maxResolution;
    }

    public void setMaxResolution(double resolution)
    {
        this.maxResolution = resolution;
    }

    public int getDensity()
    {
        return this.density;
    }

    public void setDensity(int density)
    {
        this.density = density;
    }

    // *** SurfaceObject interface ***

    public long getLastModifiedTime()
    {
        return 1; // only draw tiles once
    }

    public Iterable<? extends Sector> getSectors(DrawContext dc, double texelSizeRadians)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return Arrays.asList(extentSector);
    }

    public void doRenderToRegion(DrawContext dc, Sector sector, int x, int y, int width, int height)
    {
        this.beginDraw(dc);
        try
        {
            double pixelSize = computeDrawPixelSize(dc, sector, width, height); // meter
            double resolution = this.computeResolution(dc, sector);             // degrees

            this.applyDrawTransform(dc, sector, x, y, width, height);
            this.draw(dc, sector, resolution, pixelSize);
        }
        catch (Exception e)
        {
            // TODO: log error
        }
        finally
        {
            // Restore gl state
            this.endDraw(dc);
        }
    }

    protected TiledSurfaceObjectRenderer getRenderer()
    {
        if (this.renderer == null)
            super.getRenderer().setUseMipmaps(true);

        return this.renderer;
    }

    protected void beginDraw(DrawContext dc)
    {
        GL gl = dc.getGL();

        int attributeMask = GL.GL_TRANSFORM_BIT // for modelview
                | GL.GL_CURRENT_BIT // for current color
                | GL.GL_COLOR_BUFFER_BIT // for alpha test func and ref, and blend
                | GL.GL_ENABLE_BIT // for enable/disable changes
                | GL.GL_TEXTURE_BIT; // For texture binding and enable/disable.
        gl.glPushAttrib(attributeMask);

        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glPushMatrix();

        gl.glDisable(GL.GL_TEXTURE_2D);        // no textures
        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL.GL_ONE, GL.GL_ONE_MINUS_SRC_ALPHA);
        gl.glDisable(GL.GL_DEPTH_TEST);
        gl.glEnable(GL.GL_LINE_WIDTH);

        if (dc.isPickingMode())
        {
            this.pickSupport.beginPicking(dc);
        }
    }

    protected void endDraw(DrawContext dc)
    {
        if (dc.isPickingMode())
            this.pickSupport.endPicking(dc);

        GL gl = dc.getGL();
        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glPopMatrix();

        gl.glPopAttrib();
    }

    @SuppressWarnings({"UnusedDeclaration"})
    protected double computeResolution(DrawContext dc, Sector sector)
    {
        double targetResolution = Math.max(this.getMinResolution(), sector.getDeltaLatDegrees() / this.getDensity());    // degrees
        double logTenRes = Math.ceil(Math.log10(targetResolution));
        return Math.pow(10, logTenRes);
    }

    protected void applyDrawTransform(DrawContext dc, Sector sector, int x, int y, int width, int height)
    {
        GL gl = dc.getGL();
        // Set up a geographic transform in degrees
        gl.glTranslated(-x, -y, 0);
        gl.glScaled(width / sector.getDeltaLonDegrees(), height / sector.getDeltaLatDegrees(), 1.0);
        gl.glTranslated(-sector.getMinLongitude().degrees, -sector.getMinLatitude().degrees, 0);
    }

    protected void draw(DrawContext dc, Sector sector, double resolution, double pixelSize)
    {
        // Draw grids from finest to coarsest
        while (resolution > 0)
        {
            double widthDegrees = this.computeLineWidthDegrees(dc.getGlobe(), this.getLineWidth(resolution), pixelSize);
            this.applyDrawAttributes(dc, resolution);
            this.drawLatitudeLines(dc, sector, resolution, widthDegrees);
            this.drawLongitudeLines(dc, sector, resolution, widthDegrees);
            resolution = this.getNextLargerResolution(resolution);
        }
    }

    protected void applyDrawAttributes(DrawContext dc, double resolution)
    {
        if (!dc.isPickingMode())
            applyPremultipliedAlphaColor(dc.getGL(), this.getColor(resolution), getOpacity());
    }

    @SuppressWarnings({"UnusedDeclaration"})
    protected double getLineWidth(double resolution)
    {
        return 3; // TODO: expose line width, color, opacity...
    }

    protected double computeLineWidthDegrees(Globe globe, double lineWidth, double pixelSize)
    {
        return Angle.fromRadians(lineWidth * pixelSize / globe.getRadius()).degrees;
    }

    protected Color getColor(double resolution)
    {
        if (resolution >= 10)
            return Color.WHITE;
        else if (resolution >= 1)
            return Color.BLUE;
        else if (resolution >= .1)
            return Color.GREEN;
        else if (resolution >= .01)
            return Color.YELLOW;
        else if (resolution >= .001)
            return Color.CYAN;
        else if (resolution >= .0001)
            return Color.PINK;
        return Color.ORANGE;
    }

    protected double getNextLargerResolution(double resolution)
    {
        resolution *= 10;
        return resolution <= this.getMaxResolution() ? resolution : -1;
    }

    protected void drawLatitudeLines(DrawContext dc, Sector sector, double resolution, double width)
    {
        double lat = (Math.floor(sector.getMinLatitude().degrees / resolution) - 1) * resolution;
        double minLon = sector.getMinLongitude().degrees;
        double maxLon = sector.getMaxLongitude().degrees;
        while (lat <= sector.getMaxLatitude().degrees)
        {
            this.drawGraticuleLine(dc, width, minLon, lat, maxLon, lat);
            lat += resolution;
        }
    }

    protected void drawLongitudeLines(DrawContext dc, Sector sector, double resolution, double width)
    {
        double minLon = (Math.floor(sector.getMinLongitude().degrees / resolution) - 1) * resolution;
        double minLat = sector.getMinLatitude().degrees;
        double maxLat = sector.getMaxLatitude().degrees;
        double cosLat = sector.getCentroid().getLatitude().cos();
        for (double lon  = minLon; lon <= sector.getMaxLongitude().degrees; lon += resolution)
        {
            // skip odd lines when tile width is less then a third it's height
            if (cosLat < .3 && ((int)(lon / resolution)) % 2 != 0)
                continue;

            this.drawGraticuleLine(dc, width, lon, minLat, lon, maxLat);
        }
    }

    protected void drawGraticuleLine(DrawContext dc, double width, double x1, double y1, double x2, double y2)
    {
        GL gl = dc.getGL();
        double hw = width / 2;
        if (x1 == x2)
        {
            // Meridian
            double f1 = Angle.fromDegrees(y1).cos(); // latitude 'compression' factors
            double f2 = Angle.fromDegrees(y2).cos();
            if (Math.abs(f2 - f1) < .1)
            {
                // small 'slope' - draw a single rectangle with average width
                hw /= (f1 + f2) / 2;
                gl.glBegin(GL.GL_TRIANGLE_STRIP);
                gl.glVertex3d(x1 - hw, y1, 0);
                gl.glVertex3d(x1 + hw, y1, 0);
                gl.glVertex3d(x2 - hw, y2, 0);
                gl.glVertex3d(x2 + hw, y2, 0);
                gl.glEnd();
            }
            else
            {
                // larger delta - draw several stacks with varying width
                gl.glBegin(GL.GL_TRIANGLE_STRIP);
                int stacks = 12;
                double dy = (y2 - y1) / stacks;
                double y;
                for (int i = 0; i <= stacks; i++)
                {
                    y = y1 + dy * i;
                    double hwStep = hw / Math.max(0.05, Angle.fromDegrees(y).cos());
                    gl.glVertex3d(x1 - hwStep, y, 0);
                    gl.glVertex3d(x1 + hwStep, y, 0);
                }
                gl.glEnd();
            }
        }
        else if (y1 == y2)
        {
            // Parallel - single rectangle
            gl.glBegin(GL.GL_TRIANGLE_STRIP);
            gl.glVertex3d(x1, y1 + hw, 0);
            gl.glVertex3d(x1, y1 - hw, 0);
            gl.glVertex3d(x2, y2 + hw, 0);
            gl.glVertex3d(x2, y2 - hw, 0);
            gl.glEnd();
        }
    }

}
