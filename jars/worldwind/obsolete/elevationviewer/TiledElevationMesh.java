/* Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package WorldWindHackApps.elevationviewer;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.cache.*;
import gov.nasa.worldwind.data.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.util.*;

import javax.media.opengl.GL;
import java.awt.geom.*;
import java.io.File;
import java.util.*;

/**
 * @author dcollins
 * @version $Id: TiledElevationMesh.java 13023 2010-01-21 00:18:48Z dcollins $
 */
public class TiledElevationMesh implements SceneElement
{
    private static class MeshTile extends Tile
    {
        private final MeshCoords coords;

        private MeshTile(Sector sector, Level level, int row, int column, MeshCoords coords)
        {
            super(sector, level, row, column);
            this.coords = coords;
        }

        public MeshCoords getMeshCoords()
        {
            return this.coords;
        }

        public MeshTile[] subdivide(Level nextLevel)
        {
            Angle p0 = this.getSector().getMinLatitude();
            Angle p2 = this.getSector().getMaxLatitude();
            Angle p1 = Angle.midAngle(p0, p2);

            Angle t0 = this.getSector().getMinLongitude();
            Angle t2 = this.getSector().getMaxLongitude();
            Angle t1 = Angle.midAngle(t0, t2);

            int row = this.getRow();
            int col = this.getColumn();

            MeshCoords[] coords = this.coords.subdivide();

            MeshTile[] subTiles = new MeshTile[4];
            subTiles[0] = new MeshTile(new Sector(p0, p1, t0, t1), nextLevel, 2 * row, 2 * col, coords[0]); // Lower left quadrant.
            subTiles[1] = new MeshTile(new Sector(p0, p1, t1, t2), nextLevel, 2 * row, 2 * col + 1, coords[1]); // Lower right quadrant.
            subTiles[2] = new MeshTile(new Sector(p1, p2, t1, t2), nextLevel, 2 * row + 1, 2 * col + 1, coords[2]); // Upper right quadrant
            subTiles[3] = new MeshTile(new Sector(p1, p2, t0, t1), nextLevel, 2 * row + 1, 2 * col, coords[3]); // Upper left quadrant.
            return subTiles;
        }
    }

    private LevelSet levelSet;
    private DataDescriptor dataDescriptor;
    private MeshCoords coords;
    private double verticalOffset = 0d;
    private double verticalScale = 1d;

    private int visibleLevel;
    private MeshTile[] topLevelTiles;
    private ArrayList<MeshTile> currentTiles = new ArrayList<MeshTile>();         
    private MemoryCache cache;

    private static final long DEFAULT_CACHE_SIZE = 268435456L;

    public TiledElevationMesh(DataDescriptor dataDescriptor, MeshCoords coords)
    {
        this.levelSet = new LevelSet(dataDescriptor);
        this.dataDescriptor = dataDescriptor;
        this.coords = coords;

        this.visibleLevel = this.levelSet.getFirstLevel().getLevelNumber();
        this.cache = new BasicMemoryCache((long) (0.85 * DEFAULT_CACHE_SIZE), DEFAULT_CACHE_SIZE);

        this.topLevelTiles = createTiles(this.levelSet, this.visibleLevel, this.coords);
        this.preloadTiles(this.topLevelTiles);
    }

    public static MeshCoords createMeshCoords(Sector sector)
    {
        double aspect = sector.getDeltaLonDegrees() / sector.getDeltaLatDegrees();

        float width = 100.0f;
        float height = (float) (width * aspect);

        return new MeshCoords(0f, 0f, height, width);
    }

    public LevelSet getLevelSet()
    {
        return this.levelSet;
    }

    public DataDescriptor getDataDescriptor()
    {
        return this.dataDescriptor;
    }

    public MeshCoords getMeshCoords()
    {
        return this.coords;
    }

    public int getVisibleLevel()
    {
        return this.visibleLevel;
    }

    public void setVisibleLevel(int levelNumber)
    {
        this.visibleLevel = levelNumber;
    }

    public void render(GL gl, Camera camera)
    {
        this.currentTiles.clear();
        this.assembleTiles(this.currentTiles);

        for (MeshTile tile : this.currentTiles)
            this.drawTile(gl, camera, tile);
    }

    private void assembleTiles(List<MeshTile> tileList)
    {
        for (MeshTile tile : this.topLevelTiles)
            this.addTileOrDescendands(tile, tileList);
    }

    private void addTileOrDescendands(MeshTile tile, List<MeshTile> tileList)
    {
        if (!tile.getSector().intersects(this.levelSet.getSector()))
            return;

        if (tile.getLevelNumber() == this.visibleLevel)
        {
            tileList.add(tile);
            return;
        }

        if (this.levelSet.isFinalLevel(tile.getLevelNumber()))
        {
            return;
        }

        MeshTile[] subTiles = tile.subdivide(this.levelSet.getLevel(tile.getLevelNumber() + 1));
        for (MeshTile subTile : subTiles)
        {
            addTileOrDescendands(subTile, tileList);
        }
    }

    private void drawTile(GL gl, Camera camera, MeshTile tile)
    {
        TileKey key = tile.getTileKey();

        ElevationMesh mesh = (ElevationMesh) this.cache.getObject(key);
        if (mesh == null && !this.cache.contains(key))
        {
            BufferWrapperRaster raster = this.readTileRaster(tile);
            mesh = this.createMesh(tile, raster, this.verticalOffset, this.verticalScale);
            long size = (mesh != null) ? mesh.getSizeInBytes() : 1L;
            this.cache.add(key, mesh, size);
        }

        if (mesh != null)
            mesh.render(gl, camera);
    }

    private ElevationMesh createMesh(MeshTile tile, BufferWrapperRaster raster, double verticalOffset, double verticalScale)
    {
        if (raster == null)
            return null;
        return new ElevationMesh(raster, tile.getMeshCoords(), verticalOffset, verticalScale);
    }

    private BufferWrapperRaster readTileRaster(MeshTile tile)
    {
        File file = new File(this.dataDescriptor.getFileStoreLocation(), tile.getPath());
        if (!file.exists())
            return null;

        DataSource source = new BasicDataSource(file);
        source.setValue(AVKey.SECTOR, tile.getSector());

        BILRasterReader reader = new BILRasterReader();
        DataRaster[] rasters;
        try
        {
            rasters = reader.read(source);    
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }

        return (BufferWrapperRaster) rasters[0];
    }

    private void preloadTiles(MeshTile[] tiles)
    {
        double minValue = Double.MAX_VALUE;
        double minScale = 1d;

        BufferWrapperRaster[] rasters = new BufferWrapperRaster[tiles.length];
        for (int i = 0; i < tiles.length; i++)
        {
            rasters[i] = this.readTileRaster(tiles[i]);

            double[] minAndMax = ElevationMesh.getMinAndMaxValues(rasters[i]);
            if (minAndMax[0] < minValue)
                minValue = minAndMax[0];

            double scale = ElevationMesh.suggestVerticalScale(tiles[i].getSector(), tiles[i].getMeshCoords());
            if (scale < minScale)
                minScale = scale;
        }

        if (minValue == Double.MAX_VALUE)
            minValue = 0d;
        this.verticalOffset = -minValue;
        this.verticalScale = minScale;

        for (int i = 0; i < tiles.length; i++)
        {
            ElevationMesh mesh = this.createMesh(tiles[i], rasters[i], this.verticalOffset, this.verticalScale);
            this.cache.add(tiles[i].getTileKey(), mesh);
        }
    }

    private static MeshTile[] createTiles(LevelSet levelSet, int levelNumber, MeshCoords coords)
    {
        Sector sector = levelSet.getSector();
        Level level = levelSet.getLevel(levelNumber);
        Angle dLat = level.getTileDelta().getLatitude();
        Angle dLon = level.getTileDelta().getLongitude();
        Angle latOrigin = levelSet.getTileOrigin().getLatitude();
        Angle lonOrigin = levelSet.getTileOrigin().getLongitude();

        // Determine the row and column offset from the common World Wind global tiling origin.
        int firstRow = Tile.computeRow(dLat, sector.getMinLatitude(), latOrigin);
        int firstCol = Tile.computeColumn(dLon, sector.getMinLongitude(), lonOrigin);
        int lastRow = Tile.computeRow(dLat, sector.getMaxLatitude(), latOrigin);
        int lastCol = Tile.computeColumn(dLon, sector.getMaxLongitude(), lonOrigin);

        int numLatTiles = lastRow - firstRow + 1;
        int numLonTiles = lastCol - firstCol + 1;

        AffineTransform sectorTransform = createTransform(sector, coords);

        MeshTile[] tiles = new MeshTile[numLatTiles * numLonTiles];
        int index = 0;

        Angle p1 = Tile.computeRowLatitude(firstRow, dLat, latOrigin);

        for (int row = firstRow; row <= lastRow; row++)
        {
            Angle p2 = p1.add(dLat);

            Angle t1 = Tile.computeColumnLongitude(firstCol, dLon, lonOrigin);

            for (int col = firstCol; col <= lastCol; col++)
            {
                Angle t2 = t1.add(dLon);

                Sector tileSector = new Sector(p1, p2, t1, t2);
                MeshCoords tileCoords = transformSector(sectorTransform, tileSector);
                tiles[index++] = new MeshTile(tileSector, level, row, col, tileCoords);
                
                t1 = t2;
            }
            p1 = p2;
        }

        return tiles;
    }

    private static MeshCoords transformSector(AffineTransform transform, Sector sector)
    {
        java.awt.geom.Point2D p = new java.awt.geom.Point2D.Double();
        java.awt.geom.Point2D ll = new java.awt.geom.Point2D.Double();
        java.awt.geom.Point2D ur = new java.awt.geom.Point2D.Double();

        p.setLocation(sector.getMinLongitude().degrees, sector.getMinLatitude().degrees);
        transform.transform(p, ll);

        p.setLocation(sector.getMaxLongitude().degrees, sector.getMaxLatitude().degrees);
        transform.transform(p, ur);

        return new MeshCoords(
            (float) ur.getY(),  // Top
            (float) ll.getX(),  // Left
            (float) ll.getY(),  // Bottom
            (float) ur.getX()); // Right
    }

    private static AffineTransform createTransform(Sector source, MeshCoords destination)
    {
        java.awt.geom.AffineTransform transform = new java.awt.geom.AffineTransform();
        transform.translate(
            destination.left,
            destination.bottom);
        transform.scale(
            (destination.right - destination.left) / source.getDeltaLonDegrees(),
            (destination.top - destination.bottom) / source.getDeltaLatDegrees());
        transform.translate(
            -source.getMinLongitude().degrees,
            -source.getMinLatitude().degrees);
        return transform;
    }
}
