package gov.nasa.worldwind.render;

import com.sun.opengl.util.texture.TextureCoords;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.util.Logging;

import javax.media.opengl.GL;
import java.awt.geom.*;
import java.util.*;

/**
 * Renders an icon image over the terrain surface in many locations.
 *
 * @author Patrick Murris
 * @version $Id: SurfaceIcons.java 13064 2010-01-29 01:36:49Z patrickmurris $
 */
public class SurfaceIcons extends SurfaceIcon
{
    private Iterable<? extends LatLon> locations;

    public SurfaceIcons(Object imageSource, Iterable<? extends LatLon> locations)
    {
        super(imageSource);
        this.setLocations(locations);
    }

    public Iterable<? extends LatLon> getLocations()
    {
        return this.locations;
    }

    public void setLocations(Iterable<? extends LatLon> newLocations)
    {
        this.locations = newLocations;
        this.updateModifiedTime();
    }

    public Iterable<? extends Sector> getSectors(DrawContext dc, double texelSizeRadians)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (this.locations == null || !this.locations.iterator().hasNext())
            return null;

        if (this.isMaintainAppearance())
            return this.computeSectors(dc, texelSizeRadians);

        // If the icon(s) does(o) not redraw all the time, then cache it's/their bounding sector, using texelSize
        // and last modified time as keys which uniquely identify the sector.
        CacheEntry<Iterable<? extends Sector>> entry = this.sectorCache.get(texelSizeRadians);

        if (entry != null && entry.getValue() != null && entry.getLastModifiedTime() >= this.getLastModifiedTime())
            return entry.getValue();

        Iterable<? extends Sector> sectors = this.computeSectors(dc, texelSizeRadians);

        this.sectorCache.put(texelSizeRadians,
            new CacheEntry<Iterable<? extends Sector>>(sectors, this.getLastModifiedTime()));

        return sectors;
    }

    protected Iterable<? extends Sector> computeSectors(DrawContext dc, double texelSizeRadians)
    {
        // Compute all locations bounding sector, then add some padding for the icon half diagonal extent
        Sector sector = Sector.boundingSector(this.locations);
        // Compute padding
        double minCosLat = Math.min(sector.getMinLatitude().cos(), sector.getMaxLatitude().cos());
        minCosLat = Math.max(minCosLat, .01); // avoids division by zero at the poles
        double regionPixelSize = texelSizeRadians * dc.getGlobe().getRadius();
        Rectangle2D iconDimension = this.computeDrawDimension(regionPixelSize);
        double diagonalLength = Math.sqrt(iconDimension.getWidth() * iconDimension.getWidth()
            + iconDimension.getHeight() * iconDimension.getHeight());
        double padLatRadians = diagonalLength / 2 / dc.getGlobe().getRadius();
        double padLonRadians = diagonalLength / 2 / dc.getGlobe().getRadius() / minCosLat;
        // Apply padding to sector
        Angle minLat = sector.getMinLatitude().subtractRadians(padLatRadians);
        Angle maxLat = sector.getMaxLatitude().addRadians(padLatRadians);
        Angle minLon = sector.getMinLongitude().subtractRadians(padLonRadians);
        Angle maxLon = sector.getMaxLongitude().addRadians(padLatRadians);

        return computeNormalizedSectors(new Sector(minLat, maxLat, minLon, maxLon));
    }

    protected void drawIcon(DrawContext dc, Sector sector, int x, int y, int width, int height)
    {
        if (this.locations == null)
            return;
        
        GL gl = dc.getGL();
        gl.glMatrixMode(GL.GL_MODELVIEW);
        double drawScale = 1;
        TextureCoords textureCoords = new TextureCoords(0, 0, 1, 1);
        Matrix geoTransform = Matrix.fromGeographicToViewport(sector, x, y, width, height);

        // Compute draw scale only once if not maintaining strict appearance
        if (!this.isMaintainAppearance())
            drawScale = this.computeDrawScale(dc, sector, width, height, null);
        // Determine which locations are to be drawn
        Iterable<? extends LatLon> drawLocations = this.computeDrawLocations(dc, sector, width, height);
        // Draw icons
        for (LatLon location : drawLocations)
        {
            gl.glPushMatrix();

            if (this.isMaintainAppearance())
                drawScale = this.computeDrawScale(dc, sector, width, height, location);
            this.applyDrawTransform(dc, sector, x, y, width, height, location, drawScale, geoTransform);
            gl.glScaled(this.imageWidth, this.imageHeight, 1d);
            dc.drawUnitQuad(textureCoords);

            gl.glPopMatrix();
        }
    }

    protected Iterable<? extends LatLon> computeDrawLocations(DrawContext dc, Sector sector, int width, int height)
    {
        ArrayList<LatLon> drawList = new ArrayList<LatLon>();
        double safeDistanceDegreesSquared = Math.pow(this.computeSafeRadius(dc, sector, width, height).degrees, 2);
        for (LatLon location : this.getLocations())
        {
            if (this.computeLocationDistanceDegreesSquared(sector, location) <= safeDistanceDegreesSquared)
                drawList.add(location);
        }
        return drawList;
    }

    protected Angle computeSafeRadius(DrawContext dc, Sector drawSector, int width, int height)
    {
        double regionPixelSize = computeDrawPixelSize(dc, drawSector, width, height);
        Angle sectorRadius = this.computeSectorRadius(drawSector);
        Angle iconRadius = this.computeIconRadius(dc, regionPixelSize, drawSector);
        return sectorRadius.add(iconRadius);
    }

    protected Angle computeSectorRadius(Sector sector)
    {
        double dLat = sector.getDeltaLatRadians();
        double dLon = sector.getDeltaLonRadians();
        return Angle.fromRadians(Math.sqrt(dLat * dLat + dLon * dLon) / 2);
    }

    protected Angle computeIconRadius(DrawContext dc, double regionPixelSize, Sector drawSector)
    {
        double minCosLat = Math.min(drawSector.getMinLatitude().cos(), drawSector.getMaxLatitude().cos());
        if (minCosLat < 0.001)
            return Angle.POS180;

        Rectangle2D iconDimension = this.computeDrawDimension(regionPixelSize); // Meter
        double dLat = iconDimension.getHeight() / dc.getGlobe().getRadius();
        double dLon = iconDimension.getWidth() / dc.getGlobe().getRadius() / minCosLat;
        return Angle.fromRadians(Math.sqrt(dLat * dLat + dLon * dLon) / 2);
    }

    protected double computeLocationDistanceDegreesSquared(Sector drawSector, LatLon location)
    {
        double lonOffset = computeHemisphereOffset(drawSector, location);
        double dLat = location.getLatitude().degrees - drawSector.getCentroid().getLatitude().degrees;
        double dLon = location.getLongitude().degrees - drawSector.getCentroid().getLongitude().degrees + lonOffset;
        return dLat * dLat + dLon * dLon;
    }

}
