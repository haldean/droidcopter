/*
Copyright (C) 2001, 2010 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render;

import gov.nasa.worldwind.Movable;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.util.*;
import gov.nasa.worldwind.util.measure.AreaMeasurer;

import javax.media.opengl.GL;
import java.awt.*;
import java.util.*;

/**
 * @author dcollins
 * @version $Id: AbstractSurfaceShape.java 13349 2010-04-28 00:04:34Z dcollins $
 */
public abstract class AbstractSurfaceShape extends AbstractSurfaceObject implements SurfaceShape, Movable
{
    protected static final String DEFAULT_PATH_TYPE = AVKey.GREAT_CIRCLE;
    protected static final int DEFAULT_TEXELS_PER_EDGE_INTERVAL = 50;
    protected static final int DEFAULT_MIN_EDGE_INTERVALS = 0;
    protected static final int DEFAULT_MAX_EDGE_INTERVALS = 100;
    protected static final int DEFAULT_CACHE_CAPACITY = 16;

    protected ShapeAttributes attributes;
    protected String pathType = DEFAULT_PATH_TYPE;
    protected double texelsPerEdgeInterval = DEFAULT_TEXELS_PER_EDGE_INTERVAL;
    protected int minEdgeIntervals = DEFAULT_MIN_EDGE_INTERVALS;
    protected int maxEdgeIntervals = DEFAULT_MAX_EDGE_INTERVALS;
    protected boolean showBoundingSectors = false;
    // Shape rendering components.
    private static SurfaceShapeSupport surfaceShapeSupport;
    protected TiledSurfaceObjectRenderer renderer;
    protected java.util.List<LatLon> drawLocations;
    // Cached computations.
    protected BoundedHashMap<Object, CachedSectors> sectorCache = new BoundedHashMap<Object, CachedSectors>(
        DEFAULT_CACHE_CAPACITY, true);
    protected BoundedHashMap<Object, CachedLocations> locationCache = new BoundedHashMap<Object, CachedLocations>(
        DEFAULT_CACHE_CAPACITY, true);
    protected AreaMeasurer areaMeasurer;
    protected long areaMeasurerLastModifiedTime;

    public AbstractSurfaceShape(ShapeAttributes attributes)
    {
        if (attributes == null)
        {
            String message = Logging.getMessage("nullValue.AttributesIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.setAttributes(attributes);
    }

    public AbstractSurfaceShape()
    {
        this(new BasicShapeAttributes());
    }

    public ShapeAttributes getAttributes()
    {
        // Defensively copy the shape attributes to ensure that the does not modify its contents.
        return this.attributes.copy();
    }

    public void setAttributes(ShapeAttributes attributes)
    {
        if (attributes == null)
        {
            String message = Logging.getMessage("nullValue.AttributesIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        // Defensively copy the shape attributes to ensure that the caller does not modify its contents.
        this.attributes = attributes.copy();
        this.onShapeChanged();
    }

    public String getPathType()
    {
        return this.pathType;
    }

    public void setPathType(String pathType)
    {
        if (pathType == null)
        {
            String message = Logging.getMessage("nullValue.PathTypeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.pathType = pathType;
        this.onShapeChanged();
    }

    public double getTexelsPerEdgeInterval()
    {
        return this.texelsPerEdgeInterval;
    }

    public void setTexelsPerEdgeInterval(double texelsPerEdgeInterval)
    {
        if (texelsPerEdgeInterval <= 0)
        {
            String message = Logging.getMessage("generic.ArgumentOutOfRange", "texelsPerEdgeInterval <= 0");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.texelsPerEdgeInterval = texelsPerEdgeInterval;
        this.onShapeChanged();
    }

    public int[] getMinAndMaxEdgeIntervals()
    {
        return new int[] {this.minEdgeIntervals, this.maxEdgeIntervals};
    }

    public void setMinAndMaxEdgeIntervals(int minEdgeIntervals, int maxEdgeIntervals)
    {
        if (minEdgeIntervals < 0)
        {
            String message = Logging.getMessage("generic.ArgumentOutOfRange", "minEdgeIntervals < 0");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (maxEdgeIntervals < 0)
        {
            String message = Logging.getMessage("generic.ArgumentOutOfRange", "maxEdgeIntervals < 0");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.minEdgeIntervals = minEdgeIntervals;
        this.maxEdgeIntervals = maxEdgeIntervals;
        this.onShapeChanged();
    }

    public boolean isShowBoundingSectors()
    {
        return this.showBoundingSectors;
    }

    public void setShowBoundingSectors(boolean showBoundingSectors)
    {
        this.showBoundingSectors = showBoundingSectors;
        this.onShapeChanged();
    }

    public Iterable<? extends Sector> getSectors(DrawContext dc, double texelSizeRadians)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        CachedSectors entry = this.sectorCache.get(texelSizeRadians);
        if (entry != null && entry.getValue() != null && entry.getLastModifiedTime() >= this.getLastModifiedTime())
            return entry.getValue();

        Iterable<? extends Sector> sectors = this.computeSectors(dc, texelSizeRadians);
        this.sectorCache.put(texelSizeRadians, new CachedSectors(sectors, this.getLastModifiedTime()));

        return sectors;
    }

    protected Iterable<? extends Sector> computeSectors(DrawContext dc, double texelSizeRadians)
    {
        return this.computeSectors(dc.getGlobe(), texelSizeRadians);
    }

    protected Iterable<? extends Sector> computeSectors(Globe globe, double texelSizeRadians)
    {
        Iterable<? extends LatLon> locations = this.getLocations(globe);
        if (locations == null)
            return null;

        Iterable<? extends Sector> sectors = getSurfaceShapeSupport().computeBoundingSectors(locations, this.pathType);
        return getSurfaceShapeSupport().adjustSectorsByBorderWidth(
            sectors, this.attributes.getOutlineWidth(), texelSizeRadians);
    }

    /**
     * Returns this SurfaceShape's enclosing volume as an {@link gov.nasa.worldwind.geom.Extent} in model coordinates,
     * given a specified {@link gov.nasa.worldwind.globes.Globe} and vertical exaggeration (see {@link
     * gov.nasa.worldwind.SceneController#getVerticalExaggeration()}.
     *
     * @param globe                the Globe this SurfaceShape is related to.
     * @param verticalExaggeration the vertical exaggeration of the scene containing this SurfaceShape.
     *
     * @return this SurfaceShape's Extent in model coordinates.
     *
     * @throws IllegalArgumentException if the Globe is null.
     */
    public Extent getExtent(Globe globe, double verticalExaggeration)
    {
        if (globe == null)
        {
            String message = Logging.getMessage("nullValue.GlobeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return this.computeExtent(globe, verticalExaggeration);
    }

    protected Extent computeExtent(Globe globe, double verticalExaggeration)
    {
        // Compute this shape's sectors using a texel size of 0 radians, since we have no context to determine the
        // size of a texel on the surface.
        Iterable<? extends Sector> sectors = this.computeSectors(globe, 0d);
        if (sectors == null)
            return null;

        ArrayList<Cylinder> cylinders = new ArrayList<Cylinder>();
        for (Sector s : sectors)
        {
            if (s == null)
                continue;

            cylinders.add(Sector.computeBoundingCylinder(globe, verticalExaggeration, s));
        }

        // This should never happen, but we check anyway.
        if (cylinders.size() == 0)
        {
            return null;
        }
        // This surface shape does not cross the international dateline, and therefore has a single bounding sector.
        // Return the Cylinder which bounds that sector.
        else if (cylinders.size() == 1)
        {
            return cylinders.get(0);
        }
        // This surface shape crosses the international dateline, and its bounding sectors are split along the dateline.
        // Return a Sphere which contains both Cylinders bounding this surface shape's sectors.
        else
        {
            return Sphere.createBoundingSphere(cylinders);
        }
    }

    public void preRender(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (!this.isVisible())
            return;

        this.getRenderer().preRender(dc);
    }

    public void render(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (!this.isVisible())
            return;

        this.getRenderer().render(dc);
    }

    public String getRestorableState()
    {
        RestorableSupport rs = RestorableSupport.newRestorableSupport();
        this.doGetRestorableState(rs, null);

        return rs.getStateAsXml();
    }

    public void restoreState(String stateInXml)
    {
        if (stateInXml == null)
        {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        RestorableSupport rs;
        try
        {
            rs = RestorableSupport.parse(stateInXml);
        }
        catch (Exception e)
        {
            // Parsing the document specified by stateInXml failed.
            String message = Logging.getMessage("generic.ExceptionAttemptingToParseStateXml", stateInXml);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message, e);
        }

        this.doRestoreState(rs, null);
    }

    public double getArea(Globe globe)
    {
        if (globe == null)
        {
            String message = Logging.getMessage("nullValue.GlobeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        AreaMeasurer areaMeasurer = this.setupAreaMeasurer(globe);
        return areaMeasurer.getArea(globe);
    }

    public double getArea(Globe globe, boolean terrainConformant)
    {
        if (globe == null)
        {
            String message = Logging.getMessage("nullValue.GlobeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        AreaMeasurer areaMeasurer = this.setupAreaMeasurer(globe);
        areaMeasurer.setFollowTerrain(terrainConformant);
        return areaMeasurer.getArea(globe);
    }

    public double getPerimeter(Globe globe)
    {
        if (globe == null)
        {
            String message = Logging.getMessage("nullValue.GlobeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        AreaMeasurer areaMeasurer = this.setupAreaMeasurer(globe);
        return areaMeasurer.getPerimeter(globe);
    }

    public double getWidth(Globe globe)
    {
        if (globe == null)
        {
            String message = Logging.getMessage("nullValue.GlobeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        AreaMeasurer areaMeasurer = this.setupAreaMeasurer(globe);
        return areaMeasurer.getWidth(globe);
    }

    public double getHeight(Globe globe)
    {
        if (globe == null)
        {
            String message = Logging.getMessage("nullValue.GlobeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        AreaMeasurer areaMeasurer = this.setupAreaMeasurer(globe);
        return areaMeasurer.getHeight(globe);
    }

    public double getLength(Globe globe)
    {
        if (globe == null)
        {
            String message = Logging.getMessage("nullValue.GlobeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        AreaMeasurer areaMeasurer = this.setupAreaMeasurer(globe);
        return areaMeasurer.getLength(globe);
    }

    public void move(Position position)
    {
        if (position == null)
        {
            String message = Logging.getMessage("nullValue.PositionIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Position referencePosition = this.getReferencePosition();
        if (referencePosition == null)
            return;

        this.moveTo(referencePosition.add(position));
    }

    public void moveTo(Position position)
    {
        if (position == null)
        {
            String message = Logging.getMessage("nullValue.PositionIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Position oldReferencePosition = this.getReferencePosition();
        if (oldReferencePosition == null)
            return;

        this.doMoveTo(oldReferencePosition, position);
    }

    public abstract Position getReferencePosition();

    //**************************************************************//
    //********************  SurfaceShape Rendering  ****************//
    //**************************************************************//

    protected void doRenderToRegion(DrawContext dc, Sector sector, int x, int y, int width, int height)
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

        this.beginRenderToRegion(dc);
        try
        {
            this.doRenderShape(dc, sector, x, y, width, height);
        }
        finally
        {
            this.endRenderToRegion(dc);
        }
    }

    protected void beginRenderToRegion(DrawContext dc)
    {
        getSurfaceShapeSupport().beginRendering(dc);
    }

    protected void endRenderToRegion(DrawContext dc)
    {
        getSurfaceShapeSupport().endRendering(dc);
    }

    protected void doRenderShape(DrawContext dc, Sector sector, int x, int y, int width, int height)
    {
        this.assembleRenderState(dc, sector, x, y, width, height);

        if (this.attributes.isDrawInterior() && this.attributes.getInteriorOpacity() > 0)
            this.doRenderInteriorToRegion(dc, sector, x, y, width, height);

        if (this.attributes.isDrawOutline() && this.attributes.getOutlineOpacity() > 0)
            this.doRenderOutlineToRegion(dc, sector, x, y, width, height);

        if (this.isShowBoundingSectors())
            this.doRenderBoundingSectors(dc, sector, x, y, width, height);
    }

    protected void assembleRenderState(DrawContext dc, Sector sector, int x, int y, int width, int height)
    {
        Iterable<? extends LatLon> locations = this.getCachedLocations(dc, sector, x, y, width, height);

        if (this.drawLocations == null)
            this.drawLocations = new java.util.ArrayList<LatLon>();
        this.drawLocations.clear();
        getSurfaceShapeSupport().fixDatelineCrossingLocations(sector, locations, this.drawLocations);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    protected void doRenderBoundingSectors(DrawContext dc, Sector sector, int x, int y, int width, int height)
    {
        double texelSizeRadians = sector.getDeltaLatRadians() / height;
        Iterable<? extends Sector> sectors = this.getSectors(dc, texelSizeRadians);
        if (sectors == null)
            return;

        Position referencePos = this.getReferencePosition();

        GL gl = dc.getGL();
        OGLStackHandler ogsh = new OGLStackHandler();
        ogsh.pushAttrib(gl, GL.GL_LINE_BIT);
        ogsh.pushModelview(gl);
        try
        {
            getSurfaceShapeSupport().applyModelviewTransform(dc, sector, x, y, width, height, referencePos);
            getSurfaceShapeSupport().applyColorState(dc, Color.GREEN, 1d);
            gl.glLineWidth(1f);

            for (Sector s : sectors)
            {
                getSurfaceShapeSupport().drawLocations(dc, GL.GL_LINE_LOOP, s, 4, referencePos);
            }
        }
        finally
        {
            ogsh.pop(gl);
        }
    }

    protected abstract void doRenderInteriorToRegion(DrawContext dc, Sector sector, int x, int y, int width,
        int height);

    protected abstract void doRenderOutlineToRegion(DrawContext dc, Sector sector, int x, int y, int width, int height);

    //**************************************************************//
    //********************  Protected Interface  *******************//
    //**************************************************************//

    protected abstract void doMoveTo(Position oldReferencePosition, Position newReferencePosition);

    protected abstract Iterable<? extends LatLon> getLocations(Globe globe, double edgeIntevalsPerDegree);

    protected void onShapeChanged()
    {
        this.updateModifiedTime();
    }

    protected static SurfaceShapeSupport getSurfaceShapeSupport()
    {
        if (surfaceShapeSupport == null)
        {
            surfaceShapeSupport = new SurfaceShapeSupport();
        }

        return surfaceShapeSupport;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    protected double computeEdgeIntervalsPerDegree(DrawContext dc, Sector sector, int x, int y, int width, int height)
    {
        double texelsPerDegree = Math.max(width / sector.getDeltaLonDegrees(), height / sector.getDeltaLatDegrees());
        double intervalsPerTexel = 1.0 / this.getTexelsPerEdgeInterval();

        return intervalsPerTexel * texelsPerDegree;
    }

    protected Iterable<? extends LatLon> getCachedLocations(DrawContext dc, double edgeIntervalsPerDegree)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        CachedLocations entry = this.locationCache.get(edgeIntervalsPerDegree);
        if (entry != null && entry.getValue() != null && entry.getLastModifiedTime() >= this.getLastModifiedTime())
            return entry.getValue();

        Iterable<? extends LatLon> locations = this.getLocations(dc.getGlobe(), edgeIntervalsPerDegree);
        this.locationCache.put(edgeIntervalsPerDegree, new CachedLocations(locations, this.getLastModifiedTime()));

        return locations;
    }

    protected Iterable<? extends LatLon> getCachedLocations(DrawContext dc, Sector sector, int x, int y,
        int width, int height)
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

        double edgeIntervalsPerDegree = this.computeEdgeIntervalsPerDegree(dc, sector, x, y, width, height);
        return this.getCachedLocations(dc, edgeIntervalsPerDegree);
    }

    protected TiledSurfaceObjectRenderer getRenderer()
    {
        if (this.renderer == null)
        {
            this.renderer = new TiledSurfaceObjectRenderer();
            // Don't need the tiled surface object renderer to handle picking. Picking for this shape will be handled
            // like any other Renderable.
            this.renderer.setPickEnabled(false);
            this.renderer.setSurfaceObjects(java.util.Arrays.asList(this));

            // Uncomment this code to view surface shape tile assembly.
            //this.renderer.setShowTileOutlines(true);
            //this.renderer.setTileBackgroundColor(new java.awt.Color(30, 30, 30, 60));
        }

        return this.renderer;
    }

    protected AreaMeasurer setupAreaMeasurer(Globe globe)
    {
        if (globe == null)
        {
            String message = Logging.getMessage("nullValue.GlobeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (this.areaMeasurer == null)
        {
            this.areaMeasurer = new AreaMeasurer();
        }

        // Try to use the currently cached locations. If the AreaMeasurer is out of sync with this shape's state,
        // then update the AreaMeasurer's internal location list.
        if (this.areaMeasurerLastModifiedTime < this.getLastModifiedTime())
        {
            // The AreaMeasurer requires an ArrayList reference, but SurfaceShapes use an opaque iterable. Copy the
            // iterable contents into an ArrayList to satisfy AreaMeasurer without compromising the generality of the
            // shape's iterator.
            java.util.ArrayList<LatLon> arrayList = new java.util.ArrayList<LatLon>();
            for (LatLon ll : this.getLocations(globe))
            {
                arrayList.add(ll);
            }
            getSurfaceShapeSupport().makeClosedPath(arrayList);

            this.areaMeasurer.setPositions(arrayList, 0);
            this.areaMeasurerLastModifiedTime = this.getLastModifiedTime();
        }

        // Surface shapes follow the terrain by definition.
        this.areaMeasurer.setFollowTerrain(true);

        return this.areaMeasurer;
    }

    //**************************************************************//
    //******************** Restorable State  ***********************//
    //**************************************************************//

    protected void doGetRestorableState(RestorableSupport rs, RestorableSupport.StateObject context)
    {
        // Note: showBoundingCylinders is a diagnostic flag, therefore it is not saved or restored.

        rs.addStateValueAsBoolean(context, "visible", this.isVisible());
        rs.addStateValueAsString(context, "pathType", this.getPathType());
        rs.addStateValueAsDouble(context, "texelsPerEdgeInterval", this.getTexelsPerEdgeInterval());

        int[] minAndMaxEdgeIntervals = this.getMinAndMaxEdgeIntervals();
        rs.addStateValueAsInteger(context, "minEdgeIntervals", minAndMaxEdgeIntervals[0]);
        rs.addStateValueAsInteger(context, "maxEdgeIntervals", minAndMaxEdgeIntervals[1]);

        this.attributes.getRestorableState(rs, rs.addStateObject(context, "attributes"));

        RestorableSupport.StateObject so = rs.addStateObject(null, "avlist");
        for (Map.Entry<String, Object> avp : this.getEntries())
        {
            rs.addStateValueAsString(so, avp.getKey(),
                avp.getValue() != null ? avp.getValue().toString() : "");
        }
    }

    protected void doRestoreState(RestorableSupport rs, RestorableSupport.StateObject context)
    {
        // Invoke the legacy restore functionality. This will enable the shape to recognize state XML elements
        // from the previous version of SurfaceShape.
        this.legacyRestoreState(rs, context);

        // Note: showBoundingCylinders is a diagnostic flag, therefore it is not saved or restored.

        Boolean b = rs.getStateValueAsBoolean(context, "visible");
        if (b != null)
            this.setVisible(b);

        String s = rs.getStateValueAsString(context, "pathType");
        if (s != null)
        {
            String pathType = this.pathTypeFromString(s);
            if (pathType != null)
                this.setPathType(pathType);
        }

        Double d = rs.getStateValueAsDouble(context, "texelsPerEdgeInterval");
        if (d != null)
            this.setTexelsPerEdgeInterval(d);

        int[] minAndMaxEdgeIntervals = this.getMinAndMaxEdgeIntervals();

        Integer minEdgeIntervals = rs.getStateValueAsInteger(context, "minEdgeIntervals");
        if (minEdgeIntervals != null)
            minAndMaxEdgeIntervals[0] = minEdgeIntervals;

        Integer maxEdgeIntervals = rs.getStateValueAsInteger(context, "maxEdgeIntervals");
        if (maxEdgeIntervals != null)
            minAndMaxEdgeIntervals[1] = maxEdgeIntervals;

        if (minEdgeIntervals != null || maxEdgeIntervals != null)
            this.setMinAndMaxEdgeIntervals(minAndMaxEdgeIntervals[0], minAndMaxEdgeIntervals[1]);

        RestorableSupport.StateObject so = rs.getStateObject(context, "attributes");
        if (so != null)
            this.attributes.restoreState(rs, so);

        so = rs.getStateObject(null, "avlist");
        if (so != null)
        {
            RestorableSupport.StateObject[] avpairs = rs.getAllStateObjects(so, "");
            if (avpairs != null)
            {
                for (RestorableSupport.StateObject avp : avpairs)
                {
                    if (avp != null)
                        this.setValue(avp.getName(), avp.getValue());
                }
            }
        }

        // We've potentially modified the shapes attributes in either legacyRestoreState(), or in
        // attributes.restoreState(). Flag that the shape has changed in order to ensure that any cached data associated
        // with the shape is invalidated.
        this.onShapeChanged();
    }

    /**
     * Restores state values from previous versions of the SurfaceShape state XML. These values are stored or named
     * differently than the current implementation. Those values which have not changed are ignored here, and will
     * restored in {# doRestoreState }.
     *
     * @param rs      RestorableSupport object which contains the state value properties.
     * @param context active context in the RestorableSupport to read state from.
     */
    protected void legacyRestoreState(RestorableSupport rs, RestorableSupport.StateObject context)
    {
        // Ignore texture width and height parameters, they're no longer used.
        //Integer width = rs.getStateValueAsInteger(context, "textureWidth");
        //Integer height = rs.getStateValueAsInteger(context, "textureHeight");
        //if (width != null && height != null)
        //    this.setTextureSize(new Dimension(width, height));

        java.awt.Color color = rs.getStateValueAsColor(context, "color");
        if (color != null)
            this.attributes.setInteriorMaterial(new Material(color));

        color = rs.getStateValueAsColor(context, "borderColor");
        if (color != null)
            this.attributes.setOutlineMaterial(new Material(color));

        Double dub = rs.getStateValueAsDouble(context, "lineWidth");
        if (dub != null)
            this.attributes.setOutlineWidth(dub);

        // Ignore numEdgeIntervalsPerDegree, since it's no longer used.
        //Double intervals = rs.getStateValueAsDouble(context, "numEdgeIntervalsPerDegree");
        //if (intervals != null)
        //    this.setEdgeIntervalsPerDegree(intervals.intValue());

        Boolean booleanState = rs.getStateValueAsBoolean(context, "drawBorder");
        if (booleanState != null)
            this.attributes.setDrawOutline(booleanState);

        booleanState = rs.getStateValueAsBoolean(context, "drawInterior");
        if (booleanState != null)
            this.attributes.setDrawInterior(booleanState);

        booleanState = rs.getStateValueAsBoolean(context, "antialias");
        if (booleanState != null)
            this.attributes.setEnableAntialiasing(booleanState);

        // Positions data is a per object property now. This value will be picked up by SurfacePolygon, SurfacePolyline,
        // and SurfaceSector. Other shapes will ignore this property.
        //ArrayList<LatLon> locations = rs.getStateValueAsLatLonList(context, "locations");
        //if (locations != null)
        //    this.positions = locations;
    }

    protected String pathTypeFromString(String s)
    {
        if (s == null)
            return null;

        if (s.equals(AVKey.GREAT_CIRCLE))
        {
            return AVKey.GREAT_CIRCLE;
        }
        else if (s.equals(AVKey.LINEAR))
        {
            return AVKey.LINEAR;
        }
        else if (s.equals(AVKey.LOXODROME))
        {
            return AVKey.LOXODROME;
        }
        else if (s.equals(AVKey.RHUMB_LINE))
        {
            return AVKey.RHUMB_LINE;
        }

        return null;
    }

    //**************************************************************//
    //******************** Cache Utilities  ************************//
    //**************************************************************//

    protected static class CacheEntry<T>
    {
        protected final T value;
        protected final long lastModifiedTime;

        public CacheEntry(T value, long lastModifiedTime)
        {
            this.value = value;
            this.lastModifiedTime = lastModifiedTime;
        }

        public T getValue()
        {
            return this.value;
        }

        public final long getLastModifiedTime()
        {
            return this.lastModifiedTime;
        }
    }

    protected static class CachedSectors extends CacheEntry<Iterable<? extends Sector>>
    {
        public CachedSectors(Iterable<? extends Sector> value, long lastModifiedTime)
        {
            super(value, lastModifiedTime);
        }
    }

    protected static class CachedLocations extends CacheEntry<Iterable<? extends LatLon>>
    {
        public CachedLocations(Iterable<? extends LatLon> value, long lastModifiedTime)
        {
            super(value, lastModifiedTime);
        }
    }
}
