/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.cache.MemoryCache;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.terrain.SectorGeometry;
import gov.nasa.worldwind.util.RestorableSupport;
import gov.nasa.worldwind.util.measure.AreaMeasurer;

import javax.media.opengl.GL;
import java.awt.*;
import java.util.ArrayList;

/**
 * @author Jim Miller
 * @version $Id: ConformingEllipse.java 7671 2008-12-08 00:18:14Z jmiller $
 */

public class ConformingEllipse extends ConformingShape
{
    private static class CachedShapeDescription
    {
        ArrayList<SectorGeometry.ExtractedShapeDescription> esdL;
        CachedShapeDescription(ArrayList<SectorGeometry.ExtractedShapeDescription> esdL)
        {
            this.esdL = esdL;
        }
    }

    private LatLon  center;
    private double  semiMajorAxisLength;
    private double  semiMinorAxisLength;
    private Angle   orientation;

    // data computed from defining parameters -- not saved/restored
    private Sector  bounds;
    private int     serialNumber;
    // A cartesian reference frame for ellipse:
    Vec4    Cxyz=null, uHat=null, vHat=null;

    private AreaMeasurer   areaMeasurer = null;

    public ConformingEllipse(LatLon center,
        double semiMajorAxisLength, double semiMinorAxisLength, Angle orientation)
    {
        this(center,semiMajorAxisLength,semiMinorAxisLength,orientation,null,null);
    }

    public ConformingEllipse(LatLon center,
        double semiMajorAxisLength, double semiMinorAxisLength, Angle orientation,
        Color fillColor, Color borderColor)
    {
        super(fillColor,borderColor);
        this.center = center;
        this.semiMajorAxisLength = semiMajorAxisLength;
        this.semiMinorAxisLength = semiMinorAxisLength;
        this.orientation = ((orientation == null) ? Angle.ZERO : orientation);
        this.serialNumber = ConformingShape.getUniqueSerialNumber();
        this.bounds = null; // will be set on first draw request
    }

    @Override
    protected void doGetRestorableState(RestorableSupport rs, RestorableSupport.StateObject context)
    {
        super.doGetRestorableState(rs, context);

        rs.addStateValueAsDouble(context, "semiMajorAxisLength", this.getSemiMajorAxisLength());
        rs.addStateValueAsDouble(context, "semiMinorAxisLength", this.getSemiMinorAxisLength());
        rs.addStateValueAsLatLon(context, "center", this.getCenter());
        rs.addStateValueAsDouble(context, "orientationDegrees", this.getOrientation().degrees);
    }

    @Override
    protected void doRestoreState(RestorableSupport rs, RestorableSupport.StateObject context)
    {
        super.doRestoreState(rs, context);

        Double major = rs.getStateValueAsDouble(context, "semiMajorAxisLength");
        Double minor = rs.getStateValueAsDouble(context, "semiMinorAxisLength");
        if (major != null && minor != null)
            this.setAxisLengths(major, minor);

        LatLon center = rs.getStateValueAsLatLon(context, "center");
        if (center != null)
            this.setCenter(center);

        Double od = rs.getStateValueAsDouble(context, "orientationDegrees");
        if (od != null)
            this.setOrientation(Angle.fromDegrees(od));
    }

    public LatLon getCenter()
    {
        return this.center;
    }

    public void setCenter(LatLon center)
    {
        invalidateCache();
        this.center = center;
    }

    public double getSemiMajorAxisLength()
    {
        return semiMajorAxisLength;
    }

    public double getSemiMinorAxisLength()
    {
        return semiMinorAxisLength;
    }

    public void setAxisLengths(double majorAxisLength, double minorAxisLength)
    {
        invalidateCache();
        this.semiMajorAxisLength = majorAxisLength;
        this.semiMinorAxisLength = minorAxisLength;
    }

    public Angle getOrientation()
    {
        return orientation;
    }

    public void setOrientation(Angle orientation)
    {
        invalidateCache();
        this.orientation = orientation;
    }

    public Position getReferencePosition()
    {
        return new Position(this.center.getLatitude(),this.center.getLongitude(),0.0);
    }

    public double getLength(Globe globe)
    {
        if (areaMeasurer == null)
            makeAreaMeasurer(globe);
        return areaMeasurer.getLength(globe);
    }

    public double getArea(Globe globe)
    {
        if (areaMeasurer == null)
            makeAreaMeasurer(globe);
        return areaMeasurer.getArea(globe);
    }

    public double getPerimeter(Globe globe)
    {
        if (areaMeasurer == null)
            makeAreaMeasurer(globe);
        return areaMeasurer.getPerimeter(globe);
    }

    public double getWidth(Globe globe)
    {
        if (areaMeasurer == null)
            makeAreaMeasurer(globe);
        return areaMeasurer.getWidth(globe);
    }

    public double getHeight(Globe globe)
    {
        if (areaMeasurer == null)
            makeAreaMeasurer(globe);
        return areaMeasurer.getHeight(globe);
    }

    private void makeAreaMeasurer(Globe globe)
    {
        if (Cxyz == null)
            makeEllipticalReferenceFrame(globe);
        final int numIntervals = 200;
        ArrayList<Position> positions = new ArrayList<Position>(numIntervals+1);
        double dTheta = 2.0*Math.PI / numIntervals;
        double theta = 0.0;
        double radius = globe.getRadiusAt(center.getLatitude(), center.getLongitude());
        for (int i=0 ; i<numIntervals ; i++)
        {
            // azimuth runs positive clockwise from north and through 360 degrees.
            double xLength = semiMajorAxisLength * Math.cos(theta);
            double yLength = semiMinorAxisLength * Math.sin(theta);
            theta += dTheta;
            double distance = Math.sqrt(xLength * xLength + yLength * yLength);
            double azimuth = Math.PI / 2 - (Math.acos(xLength/distance) * Math.signum(yLength) + orientation.radians);
            LatLon p = LatLon.greatCircleEndPosition(center, azimuth, distance / radius);
            positions.add(new Position(p.getLatitude(),p.getLongitude(),0.0));
        }
        positions.add(positions.get(0));
        areaMeasurer = new AreaMeasurer(positions);
        areaMeasurer.setFollowTerrain(true);
    }

    private void makeEllipticalReferenceFrame(Globe globe)
    {
        Cxyz = globe.computePointFromPosition(center.getLatitude(),center.getLongitude(),0);
        double radius = globe.getRadiusAt(center.getLatitude(), center.getLongitude());

        // points on the positive major radius direction and positive minor radius
        // direction allow us to determine unit vectors in those two directions:

        // compute uHat
        double azimuth = Math.PI / 2 + orientation.radians;
        ArrayList<LatLon> p = new ArrayList<LatLon>(4);
        p.add(LatLon.greatCircleEndPosition(center, azimuth, semiMajorAxisLength / radius));
        p.add(LatLon.greatCircleEndPosition(center, azimuth,-semiMajorAxisLength / radius));
        Vec4 uPoint = globe.computePointFromPosition(p.get(0).getLatitude(),p.get(0).getLongitude(),0);
        uHat = uPoint.subtract3(Cxyz).normalize3();

        // compute vHat
        azimuth = orientation.radians;
        p.add(LatLon.greatCircleEndPosition(center, azimuth, semiMinorAxisLength / radius));
        p.add(LatLon.greatCircleEndPosition(center, azimuth,-semiMinorAxisLength / radius));
        Vec4 vPoint = globe.computePointFromPosition(p.get(2).getLatitude(),p.get(2).getLongitude(),0);
        vHat = vPoint.subtract3(Cxyz).normalize3();

        bounds = Sector.boundingSector(p);
    }

    protected void invalidateCache()
    {
        MemoryCache partitionCache = WorldWind.getMemoryCache(CONFORMINGSHAPE_CACHE_KEY);
        partitionCache.remove( new CacheKey(this.getClass(), null, this.serialNumber) );
        Cxyz = null;
        uHat = null; vHat = null;
        areaMeasurer = null;
    }

    public void moveTo(Position position)
    {
        invalidateCache();
        center = new LatLon(position.getLatitude(),position.getLongitude());
    }

    protected boolean renderInterior(DrawContext dc, GL gl)
    {
        boolean foundIntersectingPartition = false;
        if (drawInterior || drawBorder)
        {
            Globe globe = dc.getGlobe();
            if (Cxyz == null)
                makeEllipticalReferenceFrame(globe);
            float[] colorArray = new float[4]; 
            MemoryCache partitionCache = WorldWind.getMemoryCache(CONFORMINGSHAPE_CACHE_KEY);
            CacheKey key = new CacheKey(this.getClass(), null, this.serialNumber);
            CachedShapeDescription csd = (CachedShapeDescription)partitionCache.getObject(key);
            ArrayList<SectorGeometry.ExtractedShapeDescription> esdL = null;
            if (csd != null) esdL = csd.esdL;
            if ((esdL == null) || isExpired(dc))
            {
                if (esdL != null)
                {
                    partitionCache.remove(key);
                    esdL = null;
                }
                for (SectorGeometry sg : dc.getSurfaceGeometry())
                {
                    if (sg.getSector().intersects(bounds))
                    {
                        SectorGeometry.ExtractedShapeDescription esd = sg.getIntersectingTessellationPieces(
                            Cxyz,uHat,vHat, semiMajorAxisLength,semiMinorAxisLength);
                        if (esd != null)
                        {
                            if (esdL == null)
                                esdL = new ArrayList<SectorGeometry.ExtractedShapeDescription>(4);
                            esdL.add(esd);
                            foundIntersectingPartition = true;
                        }
                    }
                }
                if (esdL != null)
                    partitionCache.add(key, new CachedShapeDescription(esdL), sizeInBytesOf(esdL));
            }
            if (drawInterior && (esdL != null))
                for (SectorGeometry.ExtractedShapeDescription esd : esdL)
                    for (Vec4[] sgT : esd.interiorPolys)
                    {
                        if (!dc.isPickingMode())
                            gl.glColor4fv(fillColor.getComponents(colorArray),0);
                        gl.glBegin(GL.GL_POLYGON);
                        for (Vec4 v : sgT)
                            gl.glVertex3d(v.getX(),v.getY(),v.getZ());
                        gl.glEnd();
                    }
        }
        updateExpiryCriteria(dc);
        return foundIntersectingPartition;
    }

    protected void renderBoundary(DrawContext dc, GL gl, boolean knownToBeVisible)
    {
        if (drawBorder)
        {
            MemoryCache partitionCache = WorldWind.getMemoryCache(CONFORMINGSHAPE_CACHE_KEY);
            CacheKey key = new CacheKey(this.getClass(), null, this.serialNumber);
            CachedShapeDescription csd = (CachedShapeDescription)partitionCache.getObject(key);
            ArrayList<SectorGeometry.ExtractedShapeDescription> esdL = null;
            if (csd != null) esdL = csd.esdL;
            if (esdL != null)
            {
                float[] colorArray = new float[4];
                if (!dc.isPickingMode())
                    gl.glColor4fv(borderColor.getComponents(colorArray),0);
                gl.glBegin(GL.GL_LINES);
                for (SectorGeometry.ExtractedShapeDescription esd : esdL)
                    for(SectorGeometry.BoundaryEdge be : esd.shapeOutline)
                    {
                        gl.glVertex3d(be.vertices[be.i1].x, be.vertices[be.i1].y, be.vertices[be.i1].z);
                        gl.glVertex3d(be.vertices[be.i2].x, be.vertices[be.i2].y, be.vertices[be.i2].z);
                    }
                gl.glEnd();
            }
        }
    }
}
