package gov.nasa.worldwind.render;

import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.util.Logging;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Renders a set of contour lines on the terrain at given elevations. The contour line set extent
 * can be bounded by a <code>Sector</code>.
 *
 * @author Patrick Murris
 * @version $Id: SurfaceContourLineSet.java 13058 2010-01-27 22:58:05Z tgaskins $
 */
public class SurfaceContourLineSet implements PreRenderable, Renderable
{
    private Sector sector;
    private List<SurfaceContourLine> lines = new ArrayList<SurfaceContourLine>();
    private ShapeAttributes defaultAttributes = new BasicShapeAttributes();
    protected List<SurfaceShape> surfaceShapes = new ArrayList<SurfaceShape>();
    protected TiledSurfaceObjectRenderer renderer = new TiledSurfaceObjectRenderer();
    private boolean enabled = true;

    public SurfaceContourLineSet(double[] elevations, Sector sector)
    {
        this.sector = sector;

        // Init default attributes
        this.defaultAttributes.setDrawInterior(false);
        this.defaultAttributes.setOutlineMaterial(Material.CYAN);

        // Create contour lines
        for (double elevation : elevations)
        {
            SurfaceContourLine scl = new SurfaceContourLine();
            scl.setAttributes(this.getAttributes(elevation));
            scl.setElevation(elevation);
            scl.setSector(sector);
            this.lines.add(scl);
        }
    }

    public boolean isEnabled()
    {
        return this.enabled;
    }

    public void setEnabled(boolean state)
    {
        this.enabled = state;
    }

    protected ShapeAttributes getAttributes(double elevation)
    {
        return this.defaultAttributes;
    }

    public void preRender(DrawContext dc)
    {
        if (this.isValid(dc))
        {
            this.assembleShapes(dc);
            if (this.surfaceShapes.size() > 0)
            {
                this.renderer.setSurfaceObjects(this.surfaceShapes);
                this.renderer.preRender(dc);
            }
        }
    }

    public void pick(DrawContext dc, Point pickPoint)
    {
        if (this.isValid(dc) && this.surfaceShapes.size() > 0)
            this.renderer.pick(dc, pickPoint, null);
    }

    public void render(DrawContext dc)
    {
        if (this.isValid(dc) && this.surfaceShapes.size() > 0)
            this.renderer.render(dc);
    }

    protected boolean isValid(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (!this.isEnabled())
            return false;

        if (!this.sector.intersects(dc.getVisibleSector()))
            return false;

        return true;
    }

    protected void assembleShapes(DrawContext dc)
    {
        this.surfaceShapes.clear();

        for (SurfaceContourLine scl : this.lines)
        {
            List<SurfaceShape> shapes = scl.getSurfaceShapes(dc);
            if (shapes != null)
                this.surfaceShapes.addAll(shapes);

        }
    }
    
}
