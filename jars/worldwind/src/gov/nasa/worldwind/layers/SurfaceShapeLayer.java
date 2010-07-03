/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.layers;

import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.util.Logging;

/**
 * The SurfaceShapeLayer class manages a collection of {@link gov.nasa.worldwind.render.Renderable} objects for
 * rendering, picking, and disposal. Renderable objects which implement the
 * {@link gov.nasa.worldwind.render.SurfaceShape} interface will be handled separately from other Renderable objects.
 * These SurfaceShape object will be gathered in bulk and passed to this layer's internal
 * {@link gov.nasa.worldwind.render.TiledSurfaceObjectRenderer} for prerendering, rendering and picking operations.
 * The remaining Renderable objects will be handled just as if this were a
 * {@link gov.nasa.worldwind.layers.RenderableLayer}.
 *
 * @author dcollins
 * @version $Id: SurfaceShapeLayer.java 13314 2010-04-14 14:59:04Z dcollins $
 * @see gov.nasa.worldwind.render.SurfaceShape
 * @see gov.nasa.worldwind.render.TiledSurfaceObjectRenderer
 */
public class SurfaceShapeLayer extends RenderableLayer
{
    protected TiledSurfaceObjectRenderer renderer;

    /**
     * Creates a new SurfaceShapeLayer with an empty collection of SurfaceShapes.
     */
    public SurfaceShapeLayer()
    {
        this.renderer = new TiledSurfaceObjectRenderer();

        // Uncomment this code to view surface tile assembly.
        //this.renderer.setShowTileOutlines(true);
        //this.renderer.setTileBackgroundColor(new java.awt.Color(30, 30, 30, 128));
    }

    public TiledSurfaceObjectRenderer getSurfaceObjectRenderer()
    {
        return this.renderer;
    }

    public void setSurfaceObjectRenderer(TiledSurfaceObjectRenderer renderer)
    {
        if (renderer == null)
        {
            String msg = Logging.getMessage("nullValue.RendererIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        this.renderer = renderer;
    }

    protected void doPreRender(DrawContext dc)
    {
        // Process SurfaceShapes together in bulk with the TiledSurfaceShapeRenderer. Process all other Renderables
        // separately.

        java.util.ArrayList<SurfaceObject> surfaceShapes = new java.util.ArrayList<SurfaceObject>();
        java.util.ArrayList<Renderable> nonSurfaceShapes = new java.util.ArrayList<Renderable>();
        this.separateSurfaceShapes(this.getActiveRenderables(), surfaceShapes, nonSurfaceShapes);

        this.getSurfaceObjectRenderer().setPickEnabled(this.isPickEnabled());
        this.getSurfaceObjectRenderer().setSurfaceObjects(surfaceShapes);
        this.getSurfaceObjectRenderer().preRender(dc);
        this.doPreRender(dc, nonSurfaceShapes);
    }

    protected void doPick(DrawContext dc, java.awt.Point pickPoint)
    {
        // Process SurfaceShapes together in bulk with the TiledSurfaceShapeRenderer. Process all other Renderables
        // separately.

        java.util.ArrayList<Renderable> nonSurfaceShapes = new java.util.ArrayList<Renderable>();
        this.separateSurfaceShapes(this.getActiveRenderables(), null, nonSurfaceShapes);

        this.getSurfaceObjectRenderer().pick(dc, pickPoint, this);
        this.doPick(dc, nonSurfaceShapes, pickPoint);
    }

    protected void doRender(DrawContext dc)
    {
        // Process SurfaceShapes together in bulk with the TiledSurfaceShapeRenderer. Process all other Renderables
        // separately.

        java.util.ArrayList<Renderable> nonSurfaceShapes = new java.util.ArrayList<Renderable>();
        this.separateSurfaceShapes(this.getActiveRenderables(), null, nonSurfaceShapes);

        this.getSurfaceObjectRenderer().render(dc);
        this.doRender(dc, nonSurfaceShapes);
    }

    protected void separateSurfaceShapes(Iterable<? extends Renderable> renderables,
        java.util.List<SurfaceObject> surfaceShapes,
        java.util.List<Renderable> nonSurfaceShapes)
    {
        // Separate the SurfaceShapes from other Renderables in the iterable of renderable objects.

        for (Renderable renderable : renderables)
        {
            // If the caller has specified their own Iterable,
            // then we cannot make any guarantees about its contents.
            if (renderable != null)
            {
                if (renderable instanceof SurfaceShape)
                {
                    if (surfaceShapes != null)
                        surfaceShapes.add((SurfaceShape) renderable);
                }
                else
                {
                    if (nonSurfaceShapes != null)
                        nonSurfaceShapes.add(renderable);
                }
            }
        }
    }

    public String toString()
    {
        return Logging.getMessage("layers.SurfaceShapeLayer.Name");
    }
}