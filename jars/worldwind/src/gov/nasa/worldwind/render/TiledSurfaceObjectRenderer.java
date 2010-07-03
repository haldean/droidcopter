/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render;

import com.sun.opengl.util.texture.*;
import gov.nasa.worldwind.View;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.layers.*;
import gov.nasa.worldwind.pick.*;
import gov.nasa.worldwind.terrain.SectorGeometryList;
import gov.nasa.worldwind.util.*;

import javax.media.opengl.GL;
import java.awt.*;

/**
 * @author dcollins
 * @version $Id: TiledSurfaceObjectRenderer.java 13339 2010-04-25 23:16:04Z tgaskins $
 */
public class TiledSurfaceObjectRenderer
{
    // Default attribute values.
    protected static final LatLon DEFAULT_TILE_DELTA = LatLon.fromDegrees(36, 36);
    protected static final Sector DEFAULT_SECTOR = Sector.FULL_SPHERE;
    protected static final int DEFAULT_NUM_LEVELS = 17;
    protected static final int DEFAULT_NUM_EMPTY_LEVELS = 0;
    protected static final int DEFAULT_TILE_WIDTH = 512;
    protected static final int DEFAULT_TILE_HEIGHT = 512;
    protected static final boolean DEFAULT_PICK_ENABLED = false;
    protected static final boolean DEFAULT_USE_MIPMAPS = false;
    protected static final boolean DEFAULT_DRAW_BOUNDING_SECTORS = false;
    // Default with a ratio of 1.5 surface tile texels to 1 screen pixel. This has been emperically determined to
    // produce good looking results with the standard class of SurfaceShapes such as SurfacePolygon.
    protected static final double DEFAULT_TEXEL_TO_PIXEL_RATIO = 1.5;
    protected static final java.awt.Color DEFAULT_TILE_BACKGROUND_COLOR = new java.awt.Color(0, 0, 0, 0);
    protected static final boolean DEFAULT_SHOW_TILE_OUTLINES = false;
    protected static final String DEFAULT_TILE_COUNT_NAME = "Surface Object Tiles";
    // Constants for pick tile row and column. One pick tile is associated with each tile dimension used by a
    // TiledSurfaceObjectRenderer. The pick tile is defined as tile with Level 0, and row coloumn of -1 -1,
    // respectively. Unlike standard texture tiles, the pick tile's sector changes depending on the pick position.
    protected static final int PICK_TILE_ROW = -1;
    protected static final int PICK_TILE_COL = -1;

    protected static long uniqueId = 1;

    protected Iterable<? extends SurfaceObject> surfaceObjectIterable;
    protected boolean pickEnabled;
    protected boolean useMipmaps;
    protected boolean drawBoundingSectors;
    protected double texelToPixelRatio;
    protected java.awt.Color tileBackgroundColor;
    // State computed in each rendering pass.
    protected java.util.Map<java.awt.Dimension, LevelSet> levelSetMap;
    protected java.util.Map<TileKey, PickTile> pickTileMap;
    protected java.util.List<SurfaceObjectInfo> currentSurfaceObjects;
    protected java.util.List<TextureTile> currentTiles;
    protected PickTile currentPickTile;
    protected java.util.Map<TileKey, SurfaceObjectState> tileStateMap;
    // Rendering support components.
    protected PickSupport pickSupport = new PickSupport();
    protected OGLRenderToTextureSupport rttSupport = new OGLRenderToTextureSupport();
    // Rendering diagnostic attrbiutes
    protected boolean showTileOutlines;
    protected String tileCountName;

    public TiledSurfaceObjectRenderer()
    {
        this.useMipmaps = DEFAULT_USE_MIPMAPS;
        this.drawBoundingSectors = DEFAULT_DRAW_BOUNDING_SECTORS;
        this.tileBackgroundColor = DEFAULT_TILE_BACKGROUND_COLOR;
        this.texelToPixelRatio = DEFAULT_TEXEL_TO_PIXEL_RATIO;

        this.levelSetMap = new java.util.HashMap<java.awt.Dimension, LevelSet>();
        this.pickTileMap = new java.util.HashMap<TileKey, PickTile>();
        this.currentSurfaceObjects = new java.util.ArrayList<SurfaceObjectInfo>();
        this.currentTiles = new java.util.ArrayList<TextureTile>();
        this.tileStateMap = new java.util.HashMap<TileKey, SurfaceObjectState>();

        this.showTileOutlines = DEFAULT_SHOW_TILE_OUTLINES;
        this.tileCountName = DEFAULT_TILE_COUNT_NAME;
    }

    public boolean isPickEnabled()
    {
        return this.pickEnabled;
    }

    public void setPickEnabled(boolean enabled)
    {
        this.pickEnabled = enabled;
    }

    public boolean isUseMipmaps()
    {
        return this.useMipmaps;
    }

    public void setUseMipmaps(boolean useMipmaps)
    {
        this.useMipmaps = useMipmaps;
    }

    public boolean isDrawBoundingSectors()
    {
        return this.drawBoundingSectors;
    }

    public void setDrawBoundingSectors(boolean draw)
    {
        this.drawBoundingSectors = draw;
    }

    /**
     * Returns the surface object texel to screen pixel ratio. See {#link setPixelToTexelRatio(double} for a description
     * of this value's meaning.
     *
     * @return surface object texel to screen pixel ratio.
     */
    public double getTexelToPixelRatio()
    {
        return this.texelToPixelRatio;
    }

    /**
     * Sets the surface object texel to screen pixel ratio. <ul> <li> If <code>ratio</code> is 1, the renderer will
     * attempt to create tiles whose texels have approximately the same size as a screen pixel. </li> <li> If
     * <code>ratio</code> is less than 1, the renderer will attempt to create tiles whose texels are smaller than a
     * screen pixel. For example, specifying ratio=0.5 indicates that a surface object texel should be half the size of
     * a screen pixel. </li> <li> If <code>ratio</code> is greater than 1, the renderer will attempt to create tiles
     * whose texels are larger than a screen pixel. For example, specifying ratio=2 indicates that a surface object
     * texel should be twice the size of a screen pixel.
     *
     * @param ratio surface object texel to screen pixel ratio.
     *
     * @throws IllegalArgumentException if <code>ratio</code> is less than or equal to zero.
     */
    public void setTexelToPixelRatio(double ratio)
    {
        if (ratio <= 0)
        {
            String message = Logging.getMessage("generic.ArgumentOutOfRange", "ratio <= 0");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.texelToPixelRatio = ratio;
    }

    public java.awt.Color getTileBackgroundColor()
    {
        return this.tileBackgroundColor;
    }

    public void setTileBackgroundColor(java.awt.Color color)
    {
        if (color == null)
        {
            String message = Logging.getMessage("nullValue.ColorIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.tileBackgroundColor = color;
    }

    public boolean isShowTileOutlines()
    {
        return this.showTileOutlines;
    }

    public void setShowTileOutlines(boolean show)
    {
        this.showTileOutlines = show;
    }

    public Iterable<? extends SurfaceObject> getSurfaceObjects()
    {
        return this.surfaceObjectIterable;
    }

    public void setSurfaceObjects(Iterable<? extends SurfaceObject> objects)
    {
        this.surfaceObjectIterable = objects;
    }

    public void preRender(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.clearSurfaceObjects(dc);
        this.clearTiles(dc);
        this.currentPickTile = null;

        if (this.surfaceObjectIterable == null)
            return;

        // We've cleared any surface object and tile assembly state from the last rendering pass. Determine whether or
        // not we can assemble and update the tiles. If not, we're done.
        if (!this.canAssembleTiles(dc))
            return;

        LevelSet levelSet = this.getLevelSet(dc, DEFAULT_TILE_WIDTH, DEFAULT_TILE_HEIGHT);
        this.assembleSurfaceObjects(dc, levelSet);
        this.assembleTiles(dc, levelSet);

        this.updateTiles(dc, levelSet);
    }

    public void render(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (this.surfaceObjectIterable == null)
            return;

        this.draw(dc, false);
    }

    public void pick(DrawContext dc, java.awt.Point pickPoint, Layer layer)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (this.surfaceObjectIterable == null)
            return;

        if (!this.pickEnabled)
            return;

        this.pickSupport.beginPicking(dc);
        try
        {
            this.draw(dc, true);
        }
        finally
        {
            this.pickSupport.endPicking(dc);
        }

        this.resolvePick(dc, pickPoint, layer);
        this.pickSupport.clearPickList();
    }

    protected LevelSet getLevelSet(DrawContext dc, int preferredTileWidth, int preferredTileHeight)
    {
        java.awt.Dimension tileDimension = this.computeTileDimension(dc, preferredTileWidth, preferredTileHeight);
        LevelSet levelSet = this.levelSetMap.get(tileDimension);
        if (levelSet == null)
        {
            levelSet = this.createLevelSet(tileDimension.width, tileDimension.height);
            this.levelSetMap.put(tileDimension, levelSet);
        }

        return levelSet;
    }

    protected PickTile getPickTile(LevelSet levelSet)
    {
        Level level = levelSet.getFirstLevel();
        int row = PICK_TILE_ROW;
        int col = PICK_TILE_COL;

        TileKey key = new TileKey(level.getLevelNumber(), row, col, level.getCacheName());
        PickTile tile = this.pickTileMap.get(key);
        if (tile == null)
        {
            tile = new PickTile(Sector.EMPTY_SECTOR, level, row, col);
            this.pickTileMap.put(key, tile);
        }

        return tile;
    }

    protected LevelSet createLevelSet(int tileWidth, int tileHeight)
    {
        AVList params = new AVListImpl();
        params.setValue(AVKey.LEVEL_ZERO_TILE_DELTA, DEFAULT_TILE_DELTA);
        params.setValue(AVKey.SECTOR, DEFAULT_SECTOR);
        params.setValue(AVKey.NUM_LEVELS, DEFAULT_NUM_LEVELS);
        params.setValue(AVKey.NUM_EMPTY_LEVELS, DEFAULT_NUM_EMPTY_LEVELS);
        params.setValue(AVKey.TILE_WIDTH, tileWidth);
        params.setValue(AVKey.TILE_HEIGHT, tileHeight);
        // Create a unique string representing this instance's cache name and dataset name.
        String cacheId = createUniqueCacheId();
        params.setValue(AVKey.DATA_CACHE_NAME, cacheId);
        params.setValue(AVKey.DATASET_NAME, cacheId);
        // We won't use any tile resource paths, so just supply a dummy format suffix.
        params.setValue(AVKey.FORMAT_SUFFIX, ".xyz");

        return new LevelSet(params);
    }

    protected java.awt.Dimension computeTileDimension(DrawContext dc, int preferredWidth, int preferredHeight)
    {
        int dimension = Math.max(preferredWidth, preferredHeight);

        // The viewport may be smaller than the desired dimensions. For that reason, we constrain the desired tile
        // dimension by the viewport width and height.
        java.awt.Rectangle viewport = dc.getView().getViewport();
        if (dimension > viewport.width)
            dimension = viewport.width;
        if (dimension > viewport.height)
            dimension = viewport.height;

        // The final dimension used to render all surface tiles will be the power of two which is less than or equal to
        // the preferred dimension, and which fits into the viewport.
        int potDimension = WWMath.powerOfTwoFloor(dimension);
        return new java.awt.Dimension(potDimension, potDimension);
    }

    protected static String createUniqueCacheId()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(TiledSurfaceObjectRenderer.class.getName());
        sb.append("/");
        sb.append(nextUniqueId());

        return sb.toString();
    }

    protected static long nextUniqueId()
    {
        return uniqueId++;
    }

    //**************************************************************//
    //********************  Tile Rendering  ************************//
    //**************************************************************//

    @SuppressWarnings({"UnusedDeclaration"})
    protected boolean canAssembleTiles(DrawContext dc)
    {
        java.awt.Rectangle viewport = dc.getView().getViewport();
        return viewport.getWidth() > 0 && viewport.getHeight() > 0;
    }

    protected void draw(DrawContext dc, boolean isPickCall)
    {
        if (this.currentTiles.isEmpty())
            return;

        OGLStackHandler ogsh = new OGLStackHandler();

        this.beginRendering(dc, ogsh);
        try
        {
            this.drawTiles(dc, isPickCall);
        }
        finally
        {
            this.endRendering(dc, ogsh);
        }
    }

    protected void drawTiles(DrawContext dc, boolean isPickCall)
    {
        if (isPickCall)
        {
            this.drawPickTiles(dc);
        }
        else
        {
            this.drawRenderTiles(dc);
        }
    }

    protected void drawRenderTiles(DrawContext dc)
    {
        dc.getGeographicSurfaceTileRenderer().setShowImageTileOutlines(this.showTileOutlines);
        dc.getGeographicSurfaceTileRenderer().renderTiles(dc, this.currentTiles);
    }

    protected void drawPickTiles(DrawContext dc)
    {
        // The surface object pick colors are rendered into the pickTile's texture during the preRender stage. We must
        // therefore render the pickTile's texture as though it was a typical RGBA texture, although the colors are
        // all pick colors or transparent black. Note that blending must be enabled to keep the transparent regions
        // from obscuring other pick colors.

        if (this.currentPickTile == null)
            return;

        boolean isPickingMode = dc.isPickingMode();
        if (isPickingMode)
        {
            dc.disablePickingMode();
        }

        dc.getGeographicSurfaceTileRenderer().setShowImageTileOutlines(false);
        dc.getGeographicSurfaceTileRenderer().renderTile(dc, this.currentPickTile);

        if (isPickingMode)
        {
            dc.enablePickingMode();
        }
    }

    protected void beginRendering(DrawContext dc, OGLStackHandler ogsh)
    {
        GL gl = dc.getGL();

        int attribBits = GL.GL_COLOR_BUFFER_BIT // for alpha test func and ref, blend func
            | GL.GL_POLYGON_BIT;     // For polygon mode, cull enable and cull face.
        ogsh.pushAttrib(gl, attribBits);

        // Enable the alpha test.
        gl.glEnable(GL.GL_ALPHA_TEST);
        gl.glAlphaFunc(GL.GL_GREATER, 0.0f);

        gl.glEnable(GL.GL_CULL_FACE);
        gl.glCullFace(GL.GL_BACK);
        gl.glPolygonMode(GL.GL_FRONT, GL.GL_FILL);

        if (!dc.isPickingMode())
        {
            // Enable blending in premultiplied color mode.
            gl.glEnable(GL.GL_BLEND);
            OGLUtil.applyBlending(gl, true);
        }
    }

    protected void endRendering(DrawContext dc, OGLStackHandler ogsh)
    {
        ogsh.pop(dc.getGL());
    }

    protected PickedObject bindPickableObject(DrawContext dc, Object userObject, Object objectId)
    {
        java.awt.Color pickColor = dc.getUniquePickColor();
        int colorCode = pickColor.getRGB();
        dc.getGL().glColor3ub((byte) pickColor.getRed(), (byte) pickColor.getGreen(), (byte) pickColor.getBlue());

        PickedObject po = new PickedObject(colorCode, userObject);
        if (objectId != null)
        {
            po.setValue(AVKey.PICKED_OBJECT_ID, objectId);
        }

        this.pickSupport.addPickableObject(po);

        return po;
    }

    protected void resolvePick(DrawContext dc, java.awt.Point pickPoint, Layer layer)
    {
        if (pickPoint == null)
            return;

        PickedObject topObject = this.getTopPickedSurfaceObject(dc, pickPoint);
        if (topObject == null)
            return;

        // Surface objects are by definition on the geometry defined by the terrain. Therefore if no terrain has been
        // picked, we know that no surface object has been picked. But the converse is not true.
        Position terrainPosition = this.getPickedTerrainPosition(dc);
        if (terrainPosition != null)
        {
            topObject.setPosition(terrainPosition);
        }

        if (layer != null)
        {
            topObject.setParentLayer(layer);
        }

        dc.addPickedObject(topObject);
    }

    protected PickedObject getTopPickedSurfaceObject(DrawContext dc, java.awt.Point pickPoint)
    {
        PickedObject topPickedObject = this.pickSupport.getTopObject(dc, pickPoint);
        if (topPickedObject == null)
        {
            return null;
        }

        Object topObject = topPickedObject.getObject();
        if (topObject == null || !(topObject instanceof SurfaceObject))
        {
            return null;
        }

        return topPickedObject;
    }

    protected Position getPickedTerrainPosition(DrawContext dc)
    {
        PickedObject pickedTerrain = dc.getPickedObjects().getTerrainObject();
        if (pickedTerrain != null)
        {
            return pickedTerrain.getPosition();
        }

        View view = dc.getView();
        Globe globe = dc.getGlobe();
        if (view == null || globe == null)
        {
            return null;
        }

        java.awt.Point pickPoint = dc.getPickPoint();
        if (pickPoint == null)
        {
            return null;
        }

        Line pickRay = view.computeRayFromScreenPoint(pickPoint.getX(), pickPoint.getY());

        SectorGeometryList sectorGeometry = dc.getSurfaceGeometry();
        if (sectorGeometry != null)
        {
            Intersection[] intersection = sectorGeometry.intersect(pickRay);
            if (intersection != null && intersection.length != 0)
            {
                return globe.computePositionFromPoint(intersection[0].getIntersectionPoint());
            }
        }

        return globe.getIntersectionPosition(pickRay);
    }

    //**************************************************************//
    //********************  Surface Object Assembly  ***************//
    //**************************************************************//

    protected static class SurfaceObjectInfo
    {
        protected SurfaceObject surfaceObject;
        protected Iterable<? extends Sector> firstLevelSectors;

        public SurfaceObjectInfo(SurfaceObject object, Iterable<? extends Sector> firstLevelSectors)
        {
            if (object == null)
            {
                String message = Logging.getMessage("nullValue.ObjectIsNull");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }

            if (firstLevelSectors == null)
            {
                String message = Logging.getMessage("nullValue.IterableIsNull");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }

            this.surfaceObject = object;
            this.firstLevelSectors = firstLevelSectors;
        }

        public SurfaceObject getSurfaceObject()
        {
            return this.surfaceObject;
        }

        public Iterable<? extends Sector> getFirstLevelSectors()
        {
            return this.firstLevelSectors;
        }

        public boolean intersects(DrawContext dc, double texelSizeRadians, Sector sector)
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

            for (Sector s : this.surfaceObject.getSectors(dc, texelSizeRadians))
            {
                if (s.intersects(sector))
                {
                    return true;
                }
            }

            return false;
        }
    }

    protected static class SurfaceObjectState
    {
        protected long lastModifiedTime;
        protected java.util.List<SurfaceObject> surfaceObjectList;

        public SurfaceObjectState(java.util.List<SurfaceObject> currentSurfaceObjects)
        {
            this.surfaceObjectList = currentSurfaceObjects;

            for (SurfaceObject surfaceObject : currentSurfaceObjects)
            {
                if (this.lastModifiedTime < surfaceObject.getLastModifiedTime())
                    this.lastModifiedTime = surfaceObject.getLastModifiedTime();
            }
        }

        public long getLastModifiedTime()
        {
            return this.lastModifiedTime;
        }

        public void setLastModifiedTime(long time)
        {
            this.lastModifiedTime = time;
        }

        public Iterable<? extends SurfaceObject> getSurfaceObjects()
        {
            return this.surfaceObjectList;
        }

        public boolean isStateNewerOrModified(SurfaceObjectState that)
        {
            //noinspection SimplifiableIfStatement
            if (that == null)
            {
                return false;
            }

            return (this.lastModifiedTime < that.lastModifiedTime)
                || !this.surfaceObjectList.equals(that.surfaceObjectList);
        }
    }

    @SuppressWarnings({"UnusedDeclaration"})
    protected void clearSurfaceObjects(DrawContext dc)
    {
        this.currentSurfaceObjects.clear();
    }

    protected void assembleSurfaceObjects(DrawContext dc, LevelSet levelSet)
    {
        for (SurfaceObject surfaceObject : this.surfaceObjectIterable)
        {
            this.addSurfaceObject(dc, levelSet, surfaceObject);
        }
    }

    @SuppressWarnings({"UnusedDeclaration"})
    protected void addSurfaceObject(DrawContext dc, LevelSet levelSet, SurfaceObject surfaceObject)
    {
        double firstLevelTexelSize = levelSet.getFirstLevel().getTexelSize();

        Iterable<? extends Sector> sectors = surfaceObject.getSectors(dc, firstLevelTexelSize);
        if (sectors == null)
            return;

        SurfaceObjectInfo objectInfo = new SurfaceObjectInfo(surfaceObject, sectors);
        this.currentSurfaceObjects.add(objectInfo);
    }

    protected Sector computeCurrentBoundingSector()
    {
        Sector boundingSector = null;

        for (SurfaceObjectInfo objectInfo : this.currentSurfaceObjects)
        {
            for (Sector sector : objectInfo.getFirstLevelSectors())
            {
                boundingSector = (boundingSector != null) ? boundingSector.union(sector) : sector;
            }
        }

        return boundingSector;
    }

    protected SurfaceObjectState getCurrentStateFor(DrawContext dc, Tile tile)
    {
        java.util.List<SurfaceObject> intersectingObjects = new java.util.ArrayList<SurfaceObject>();

        for (SurfaceObjectInfo objectInfo : this.currentSurfaceObjects)
        {
            if (objectInfo.intersects(dc, tile.getLevel().getTexelSize(), tile.getSector()))
            {
                intersectingObjects.add(objectInfo.getSurfaceObject());
            }
        }

        return new SurfaceObjectState(intersectingObjects);
    }

    //**************************************************************//
    //********************  Tile Assembly  *************************//
    //**************************************************************//

    @SuppressWarnings({"UnusedDeclaration"})
    protected void clearTiles(DrawContext dc)
    {
        this.currentTiles.clear();
    }

    protected void assembleTiles(DrawContext dc, LevelSet levelSet)
    {
        if (this.currentSurfaceObjects.isEmpty())
            return;

        Sector bounds = this.computeCurrentBoundingSector();
        if (bounds == null || bounds.equals(Sector.EMPTY_SECTOR))
            return;

        TextureTile[] topLevelTiles = this.assembleTopLevelTiles(levelSet, bounds);
        for (TextureTile tile : topLevelTiles)
        {
            this.addTileOrDescendants(dc, levelSet, bounds, tile);
        }

        dc.setPerFrameStatistic(PerformanceStatistic.IMAGE_TILE_COUNT, this.tileCountName,
            this.currentTiles.size());

        this.assemblePickTile(dc, levelSet);
    }

    protected void assemblePickTile(DrawContext dc, LevelSet levelSet)
    {
        if (!this.pickEnabled)
            return;

        Position pickedTerrainPosition = this.getPickedTerrainPosition(dc);
        if (pickedTerrainPosition != null)
        {
            TextureTile pickedTile = this.getIntersectingTile(
                pickedTerrainPosition.getLatitude(), pickedTerrainPosition.getLongitude(), this.currentTiles);
            if (pickedTile != null)
            {
                this.currentPickTile = this.getPickTile(levelSet);
                this.currentPickTile.setSector(pickedTile.getSector());
            }
        }
    }

    protected void addTileOrDescendants(DrawContext dc, LevelSet levelSet, Sector boundingSector, TextureTile tile)
    {
        if (!this.isTileVisible(dc, tile))
            return;

        if (!this.isTileNeeded(dc, boundingSector, tile))
            return;

        if (this.isTileMeetsRenderCriteria(dc, levelSet, tile))
        {
            this.addTile(dc, tile);
            return;
        }

        Level nextLevel = levelSet.getLevel(tile.getLevelNumber() + 1);
        for (TextureTile subTile : tile.createSubTiles(nextLevel))
        {
            this.addTileOrDescendants(dc, levelSet, boundingSector, subTile);
        }
    }

    @SuppressWarnings({"UnusedDeclaration"})
    protected void addTile(DrawContext dc, TextureTile tile)
    {
        this.currentTiles.add(tile);
    }

    protected boolean isTileVisible(DrawContext dc, SurfaceTile tile)
    {
        if (dc.getVisibleSector() != null && !dc.getVisibleSector().intersects(tile.getSector()))
        {
            return false;
        }

        //noinspection RedundantIfStatement
        if (!tile.getExtent(dc).intersects(dc.getView().getFrustumInModelCoordinates()))
        {
            return false;
        }

        return true;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    protected boolean isTileNeeded(DrawContext dc, Sector boundingSector, Tile tile)
    {
        // If tile does not intersect any surface object's bounding sector, the tile is not needed.
        if (!tile.getSector().intersects(boundingSector))
        {
            return false;
        }

        // If the tile falls completely outside the bounding sectors of all surface objects, then this tile is not
        // needed.
        boolean haveIntersection = false;
        for (SurfaceObjectInfo objectInfo : this.currentSurfaceObjects)
        {
            if (objectInfo.intersects(dc, tile.getLevel().getTexelSize(), tile.getSector()))
            {
                haveIntersection = true;
                break;
            }
        }

        return haveIntersection;
    }

    protected boolean isTileMeetsRenderCriteria(DrawContext dc, LevelSet levelSet, Tile tile)
    {
        if (levelSet.isFinalLevel(tile.getLevel().getLevelNumber()))
        {
            return true;
        }

        // Test the tile against screen metrics. This will attempt to keep the tile texel to screen pixel ratio
        // close to the user specified value. Substitude the current tile viewport for the level's width and height,
        // sinde the viewport dimensions are used to define the actual texture dimensions for this tile.
        return !this.needToSubdivide(dc, tile.getSector(), tile.getWidth(), tile.getHeight());
    }

    protected TextureTile getIntersectingTile(Angle latitude, Angle longitude, Iterable<? extends TextureTile> tiles)
    {
        for (TextureTile tile : tiles)
        {
            if (tile.getSector().contains(latitude, longitude))
            {
                return tile;
            }
        }

        return null;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    protected boolean needToSubdivide(DrawContext dc, Sector sector, int width, int height)
    {
        // Compute a point in the sector that is nearest to the eye point. This point will be used to estimate the
        // distance from the eye to the sector.
        Vec4 nearestPoint = this.getNearestPointInSector(dc, sector);

        // Compute the size in meters of a texel on the surface sector given the data dimensions (width, height). If
        // the data dimension is zero, than replace it with 1, which effectively ignores the data dimension.
        double texelSize = nearestPoint.getLength3() * sector.getDeltaLatRadians()
            / (height <= 0 ? 1d : (double) height);

        // Compute the size in meters that a screen pixel would cover on the surface.
        double eyeToSurfaceDistance = dc.getView().getEyePoint().distanceTo3(nearestPoint);
        double pixelSize = dc.getView().computePixelSizeAtDistance(eyeToSurfaceDistance);

        // Compute the the ratio of surface texels to screen pixels.
        // * If ratio is 1, then a surface texel will be about the same size as a screen pixel.
        // * If ratio is less than 1, then a surface texel will be smaller than a screen pixel.
        // * If ratio is greater than 1, then a surface texel will be larger than a screen pixel.
        double ratio = texelSize / pixelSize;

        // Subdivide when the ratio of surface texels to screen pixels to is greater than the desired ratio.
        // Subdividing will create sub tiles with a smaller ratio; the the texel size will decrease while the pixel
        // size stays constant.
        return ratio > this.texelToPixelRatio;
    }

    protected Vec4 getNearestPointInSector(DrawContext dc, Sector sector)
    {
        Position eyePos = dc.getView().getEyePosition();
        if (sector.contains(eyePos))
        {
            Vec4 point = dc.getPointOnGlobe(eyePos.getLatitude(), eyePos.getLongitude());
            if (point == null)
            {
                double elev = dc.getGlobe().getElevation(eyePos.getLatitude(), eyePos.getLongitude())
                    * dc.getVerticalExaggeration();
                return dc.getGlobe().computePointFromPosition(eyePos.getLatitude(), eyePos.getLongitude(),
                    elev);
            }

            return point;
        }

        LatLon nearestCorner = null;
        Angle nearestDistance = Angle.fromDegrees(Double.MAX_VALUE);
        for (LatLon ll : sector)
        {
            Angle d = LatLon.greatCircleDistance(ll, eyePos);
            if (d.compareTo(nearestDistance) < 0)
            {
                nearestCorner = ll;
                nearestDistance = d;
            }
        }

        if (nearestCorner == null)
            return null;

        Vec4 point = dc.getPointOnGlobe(nearestCorner.getLatitude(), nearestCorner.getLongitude());
        if (point != null)
            return point;

        double elev = dc.getGlobe().getElevation(nearestCorner.getLatitude(), nearestCorner.getLongitude())
            * dc.getVerticalExaggeration();
        return dc.getGlobe().computePointFromPosition(nearestCorner.getLatitude(), nearestCorner.getLongitude(),
            elev);
    }

    protected TextureTile[] assembleTopLevelTiles(LevelSet levelSet, Sector sector)
    {
        Level level = levelSet.getFirstLevel();
        Angle dLat = level.getTileDelta().getLatitude();
        Angle dLon = level.getTileDelta().getLongitude();
        Angle latOrigin = levelSet.getTileOrigin().getLatitude();
        Angle lonOrigin = levelSet.getTileOrigin().getLongitude();

        // Determine the row and column offset from the common World Wind global tiling origin.
        int firstRow = Tile.computeRow(dLat, sector.getMinLatitude(), latOrigin);
        int firstCol = Tile.computeColumn(dLon, sector.getMinLongitude(), lonOrigin);
        int lastRow = Tile.computeRow(dLat, sector.getMaxLatitude(), latOrigin);
        int lastCol = Tile.computeColumn(dLon, sector.getMaxLongitude(), lonOrigin);

        int nLatTiles = lastRow - firstRow + 1;
        int nLonTiles = lastCol - firstCol + 1;
        int tileIndex = 0;
        TextureTile[] tiles = new TextureTile[nLatTiles * nLonTiles];

        Angle p1 = Tile.computeRowLatitude(firstRow, dLat, latOrigin);
        for (int row = firstRow; row <= lastRow; row++)
        {
            Angle p2;
            p2 = p1.add(dLat);

            Angle t1 = Tile.computeColumnLongitude(firstCol, dLon, lonOrigin);
            for (int col = firstCol; col <= lastCol; col++)
            {
                Angle t2;
                t2 = t1.add(dLon);

                TileKey tileKey = new TileKey(level.getLevelNumber(), row, col, level.getCacheName());
                TextureTile tile = this.getTileFromMemoryCache(tileKey);
                if (tile == null)
                {
                    tile = new TextureTile(new Sector(p1, p2, t1, t2), level, row, col);
                    TextureTile.getMemoryCache().add(tileKey, tile);
                }

                tiles[tileIndex++] = tile;

                t1 = t2;
            }
            p1 = p2;
        }

        return tiles;
    }

    protected TextureTile getTileFromMemoryCache(TileKey tileKey)
    {
        return (TextureTile) TextureTile.getMemoryCache().getObject(tileKey);
    }

    //**************************************************************//
    //********************  Tile Data Creation  ********************//
    //**************************************************************//

    protected void updateTiles(DrawContext dc, LevelSet levelSet)
    {
        if (this.currentTiles.isEmpty())
            return;

        // The tile drawing rectangle has the same dimensions as the current tile viewport, but it's lower left corner
        // is placed at the origin. This is because the orthographic projection setup by OGLRenderToTextureSupport
        // maps (0, 0) to the lower left corner of the drawing region, therefore we can drop the (x, y) offset when
        // drawing pixels to the texture, as (0, 0) is automatically mapped to (x, y).
        Level firstLevel = levelSet.getFirstLevel();
        java.awt.Rectangle drawRect = new java.awt.Rectangle(0, 0,
            firstLevel.getTileWidth(), firstLevel.getTileHeight());

        this.beginUpdateTiles(dc, drawRect);

        try
        {
            for (TextureTile tile : this.currentTiles)
            {
                this.updateTile(dc, tile, drawRect);
            }

            // Must update the pick tile data every frame.
            if (this.pickEnabled && this.currentPickTile != null)
            {
                this.updatePickTile(dc, this.currentPickTile, drawRect);
            }
        }
        finally
        {
            this.endUpdateTiles(dc);
        }
    }

    protected void beginUpdateTiles(DrawContext dc, Rectangle viewport)
    {
        this.rttSupport.beginRendering(dc, viewport.x, viewport.y, viewport.width, viewport.height);
    }

    protected void endUpdateTiles(DrawContext dc)
    {
        this.rttSupport.endRendering(dc);
    }

    protected void updateTile(DrawContext dc, TextureTile tile, java.awt.Rectangle viewport)
    {
        SurfaceObjectState currentTileState = this.getCurrentStateFor(dc, tile);
        if (this.isTileCurrent(dc, tile, currentTileState))
        {
            return;
        }

        this.updateTileData(dc, tile, currentTileState, viewport);

        currentTileState.setLastModifiedTime(System.currentTimeMillis());
        this.setTileState(tile, currentTileState);
    }

    protected void updatePickTile(DrawContext dc, PickTile tile, java.awt.Rectangle viewport)
    {
        // Temporarily force the DrawContext into picking mode while the surface objects are rendered into the pick
        // tile.
        boolean isPicking = dc.isPickingMode();
        if (!isPicking)
        {
            dc.enablePickingMode();
        }

        OGLStackHandler ogsh = new OGLStackHandler();
        ogsh.pushAttrib(dc.getGL(), GL.GL_CURRENT_BIT); // For current color (pick color).
        this.pickSupport.beginPicking(dc);

        try
        {
            this.updatePickTileData(dc, tile, viewport);
        }
        finally
        {
            this.pickSupport.endPicking(dc);
            ogsh.pop(dc.getGL());

            // Restore the DrawContext's previous picking mode state.
            if (!isPicking)
            {
                dc.disablePickingMode();
            }
        }
    }

    protected boolean isTileCurrent(DrawContext dc, TextureTile tile, SurfaceObjectState state)
    {
        SurfaceObjectState currentState = this.getTileState(tile);
        //noinspection SimplifiableIfStatement
        if (currentState == null || currentState.isStateNewerOrModified(state))
        {
            return false;
        }

        return tile.getTexture(dc.getTextureCache()) != null;
    }

    protected SurfaceObjectState getTileState(TextureTile tile)
    {
        return this.tileStateMap.get(tile.getTileKey());
    }

    protected void setTileState(TextureTile tile, SurfaceObjectState state)
    {
        this.tileStateMap.put(tile.getTileKey(), state);
    }

    protected void updateTileData(DrawContext dc, TextureTile tile, SurfaceObjectState state,
        java.awt.Rectangle viewport)
    {
        Texture texture = this.setupTextureTile(dc, tile, this.useMipmaps);

        try
        {
            this.rttSupport.setColorTarget(dc, texture);
            this.rttSupport.clear(dc, this.tileBackgroundColor);

            for (SurfaceObject surfaceObject : state.getSurfaceObjects())
            {
                surfaceObject.renderToRegion(dc, tile.getSector(), viewport.x, viewport.y,
                    viewport.width, viewport.height);
            }

            if (this.isDrawBoundingSectors())
            {
                this.renderBoundingSectors(dc, state.getSurfaceObjects(), tile.getSector(), 0, 0,
                    viewport.width, viewport.height);
            }
        }
        finally
        {
            this.rttSupport.setColorTarget(dc, null);
        }
    }

    protected void updatePickTileData(DrawContext dc, PickTile tile, java.awt.Rectangle viewport)
    {
        Texture texture = this.setupTextureTile(dc, tile.getDelegate(), this.useMipmaps);

        try
        {
            this.rttSupport.setColorTarget(dc, texture);
            this.rttSupport.clear(dc, new java.awt.Color(0, 0, 0, 0));

            for (SurfaceObjectInfo objectInfo : this.currentSurfaceObjects)
            {
                if (objectInfo.intersects(dc, tile.getTexelSize(), tile.getSector()))
                {
                    this.bindPickableObject(dc, objectInfo.surfaceObject, null);
                    objectInfo.surfaceObject.renderToRegion(dc, tile.getSector(), viewport.x, viewport.y,
                        viewport.width, viewport.height);
                }
            }
        }
        finally
        {
            this.rttSupport.setColorTarget(dc, null);
        }
    }

    protected void renderBoundingSectors(DrawContext dc, Iterable<? extends SurfaceObject> surfaceObjects,
        Sector sector, int x, int y, int width, int height)
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

        GL gl = dc.getGL();

        OGLStackHandler ogsh = new OGLStackHandler();
        ogsh.pushAttrib(gl, GL.GL_COLOR_BUFFER_BIT // for alpha test func and ref, blend func
            | GL.GL_CURRENT_BIT     // for current color
            | GL.GL_LINE_BIT        // For line width.
            | GL.GL_POLYGON_BIT     // For polygon mode.
            | GL.GL_TEXTURE_BIT     // For texture disable.
            | GL.GL_TRANSFORM_BIT); // For matrix mode.
        ogsh.pushModelviewIdentity(gl);

        try
        {
            // Enable the alpha test.
            gl.glEnable(GL.GL_ALPHA_TEST);
            gl.glAlphaFunc(GL.GL_GREATER, 0.0f);

            // Setup GL to use blending with non premultiplied alpha colors. The values actually written to the framebuffer
            // will have their color components multiplied by alpha. However, to use GL line or polygon blending, GL is
            // expecting to work with non premultiplied colors.
            gl.glEnable(GL.GL_BLEND);
            OGLUtil.applyBlending(gl, false);

            // Set the current RGB color to green.
            gl.glColor3ub((byte) 0, (byte) 255, (byte) 0);

            // Disable texturing.
            gl.glDisable(GL.GL_TEXTURE_2D);

            // Set the line width, and set the polygon mode to draw filled primitives as lines.
            gl.glLineWidth(1f);
            gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_LINE);

            double[] matrixArray = new double[16];
            Matrix matrix = Matrix.fromGeographicToViewport(sector, x, y, width, height);
            matrix.toArray(matrixArray, 0, false);
            gl.glLoadMatrixd(matrixArray, 0);

            double texelSizeRadians = sector.getDeltaLatRadians() / height;

            for (SurfaceObject so : surfaceObjects)
            {
                Iterable<? extends Sector> sectors = so.getSectors(dc, texelSizeRadians);

                for (Sector s : sectors)
                {
                    gl.glRectd(
                        s.getMinLongitude().degrees, s.getMinLatitude().degrees,
                        s.getMaxLongitude().degrees, s.getMaxLatitude().degrees);
                }
            }
        }
        finally
        {
            ogsh.pop(gl);
        }
    }

    protected Texture setupTextureTile(DrawContext dc, TextureTile tile, boolean useMipmaps)
    {
        // Update the tile texture with the current framebuffer contents. If the tile does not have a texture, then
        // create new texture data .
        Texture texture = tile.getTexture(dc.getTextureCache());
        if (texture != null)
            return texture;

        OGLStackHandler ogsh = new OGLStackHandler();
        ogsh.pushAttrib(dc.getGL(), GL.GL_TEXTURE_BIT); // For texture binding.
        try
        {
            // Update the texture data and bind the tile. This will transfer any in-memory texture data to GL and create
            // the GL texture object.
            tile.setTextureData(this.createTileTextureData(tile.getWidth(), tile.getHeight(), useMipmaps));
            tile.bind(dc);

            texture = tile.getTexture(dc.getTextureCache());
            // This should never happen, but we check anyway.
            if (texture == null)
            {
                String message = Logging.getMessage("nullValue.TextureIsNull");
                Logging.logger().warning(message);
            }
        }
        finally
        {
            ogsh.pop(dc.getGL());
        }

        return texture;
    }

    protected TextureData createTileTextureData(int width, int height, boolean mipmap)
    {
        return new TextureData(
            GL.GL_RGBA,          // internal format
            width, height,       // width, height
            0,                   // border
            GL.GL_RGBA,          // pixel format
            GL.GL_UNSIGNED_BYTE, // pixel type
            mipmap,              // mipmap
            false, false,        // dataIsCompressed, mustFlipVertically
            null, null);         // buffer, flusher
    }

    //**************************************************************//
    //********************  Pick Tile  *****************************//
    //**************************************************************//

    protected static class PickTile implements SurfaceTile
    {
        protected Sector sector;
        protected final int width;
        protected final int height;
        protected TextureTile delegate;

        public PickTile(Sector sector, Level level, int row, int column)
        {
            this.sector = sector;
            this.width = level.getTileWidth();
            this.height = level.getTileHeight();
            this.delegate = new TextureTile(sector, level, row, column)
            {
                protected void setTextureParameters(DrawContext dc, Texture t)
                {
                    // Tiled surface object picking is accomplished by rendering the object pick colors into a surface
                    // tile. That tile is then applied to the surface so that object pick colors appear on the surface
                    // like render colors. However the pick colors cannot be blended or filtered to produce any color
                    // other than the original pick color. Therefore we choose the nearest neighbor texture filter to
                    // ensure that the pick texture colors are not filtered during application on the surface.
                    t.setTexParameteri(GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
                    t.setTexParameteri(GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
                }
            };
        }

        public Sector getSector()
        {
            return this.sector;
        }

        public void setSector(Sector sector)
        {
            this.sector = sector;
        }

        public int getWidth()
        {
            return this.width;
        }

        public int getHeight()
        {
            return this.height;
        }

        public TextureTile getDelegate()
        {
            return this.delegate;
        }

        public double getTexelSize()
        {
            return this.sector.getDeltaLatRadians() / this.height;
        }

        public boolean bind(DrawContext dc)
        {
            return this.delegate.bind(dc);
        }

        public void applyInternalTransform(DrawContext dc)
        {
            this.delegate.applyInternalTransform(dc);
        }

        public java.util.List<? extends LatLon> getCorners()
        {
            return this.sector.asList();
        }

        public Extent getExtent(DrawContext dc)
        {
            if (dc == null)
            {
                String message = Logging.getMessage("nullValue.DrawContextIsNull");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }

            return Sector.computeBoundingCylinder(dc.getGlobe(), dc.getVerticalExaggeration(), this.getSector());
        }
    }
}
