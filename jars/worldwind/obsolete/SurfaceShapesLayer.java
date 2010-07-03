/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.layers;

import com.sun.opengl.util.texture.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.pick.*;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.terrain.*;
import gov.nasa.worldwind.util.*;

import javax.media.opengl.*;
import java.awt.image.*;
import java.util.*;

/**
 * The <code>SurfaceShapesLayer</code> class manages a collection of {@link Renderable} objects
 * for rendering, picking, and disposal.
 *
 * @author tag
 * @version $Id: RenderableLayer.java 6041 2008-08-18 19:35:29Z dcollins $
 * @see Renderable
 */
public class SurfaceShapesLayer extends AbstractLayer
{
//****    private java.util.Collection<Renderable> renderables = new java.util.concurrent.ConcurrentLinkedQueue<Renderable>();
    private java.util.Collection<SurfaceShape> shapes = new java.util.concurrent.ConcurrentLinkedQueue<SurfaceShape>();
    private Iterable<Renderable> renderablesOverride;
    private final PickSupport pickSupport = new PickSupport();
    private final Layer delegateOwner;

    private static final int DEFAULT_TEXTURE_WIDTH = 512;
    private static final int DEFAULT_TEXTURE_HEIGHT = 512;

    /**
     * Creates a new <code>SurfaceShapesLayer</code> with a null <code>delegateOwner</code>
     */
    public SurfaceShapesLayer()
    {
        this.delegateOwner = null;
    }

    /**
     * Creates a new <code>SurfaceShapesLayer</code> with the specified <code>delegateOwner</code>.
     *
     * @param delegateOwner Layer that is this layer's delegate owner.
     */
    public SurfaceShapesLayer(Layer delegateOwner)
    {
        this.delegateOwner = delegateOwner;
    }

    /**
     * Adds the specified <code>renderable</code> to this layer's internal collection.
     * If this layer's internal collection has been overriden with a call to {@link #setRenderables},
     * this will throw an exception.
     *
     * @param renderable Renderable to add.
     * @throws IllegalArgumentException If <code>renderable</code> is null.
     * @throws IllegalStateException If a custom Iterable has been specified by a call to <code>setRenderables</code>.
     */
    /********
    public void addRenderable(Renderable renderable)
    {
        if (renderable == null)
        {
            String msg = Logging.getMessage("nullValue.RenderableIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        if (this.renderablesOverride != null)
        {
            String msg = Logging.getMessage("generic.LayerIsUsingCustomIterable");
            Logging.logger().severe(msg);
            throw new IllegalStateException(msg);
        }

        this.renderables.add(renderable);
    }
    *****/
    public void addSurfaceShape(SurfaceShape shape)
    {
        if (shape == null)
        {
            // TODO: CHANGE WORDING...
            String msg = Logging.getMessage("nullValue.RenderableIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        if (this.renderablesOverride != null)
        {
            String msg = Logging.getMessage("generic.LayerIsUsingCustomIterable");
            Logging.logger().severe(msg);
            throw new IllegalStateException(msg);
        }

        this.shapes.add(shape);
    }

    /**
     * Adds the contents of the specified <code>renderables</code> to this layer's internal collection.
     * If this layer's internal collection has been overriden with a call to {@link #setRenderables},
     * this will throw an exception.
     *
     * @param renderables Renderables to add.
     * @throws IllegalArgumentException If <code>renderables</code> is null.
     * @throws IllegalStateException If a custom Iterable has been specified by a call to <code>setRenderables</code>.
     */
    /*****
     public void addRenderables(Iterable<Renderable> renderables)
     {
         if (renderables == null)
         {
             String msg = Logging.getMessage("nullValue.IterableIsNull");
             Logging.logger().severe(msg);
             throw new IllegalArgumentException(msg);
         }
         if (this.renderablesOverride != null)
         {
             String msg = Logging.getMessage("generic.LayerIsUsingCustomIterable");
             Logging.logger().severe(msg);
             throw new IllegalStateException(msg);
         }

         for (Renderable renderable : renderables)
         {
             // Internal list of renderables does not accept null values.
             if (renderable != null)
                 this.renderables.add(renderable);
         }
     }
     **************/
     public void addSurfaceShapes(Iterable<? extends SurfaceShape> shapes)
     {
         if (shapes == null)
         {
             // TODO: CHANGE WORDING...
             String msg = Logging.getMessage("nullValue.IterableIsNull");
             Logging.logger().severe(msg);
             throw new IllegalArgumentException(msg);
         }
         if (this.renderablesOverride != null)
         {
             // TODO:  WHAT IS THIS AND IS IT NEEDED HERE?
             String msg = Logging.getMessage("generic.LayerIsUsingCustomIterable");
             Logging.logger().severe(msg);
             throw new IllegalStateException(msg);
         }

         for (SurfaceShape shape : shapes)
         {
             // Internal list of renderables does not accept null values.
             if (shape != null)
                 this.shapes.add(shape);
         }
     }

    /**
     * Removes the specified <code>renderable</code> from this layer's internal collection, if it exists.
     * If this layer's internal collection has been overriden with a call to {@link #setRenderables},
     * this will throw an exception.
     *
     * @param renderable Renderable to remove.
     * @throws IllegalArgumentException If <code>renderable</code> is null.
     * @throws IllegalStateException If a custom Iterable has been specified by a call to <code>setRenderables</code>.
     */
    /*************
     public void removeRenderable(Renderable renderable)
     {
         if (renderable == null)
         {
             String msg = Logging.getMessage("nullValue.RenderableIsNull");
             Logging.logger().severe(msg);
             throw new IllegalArgumentException(msg);
         }
         if (this.renderablesOverride != null)
         {
             String msg = Logging.getMessage("generic.LayerIsUsingCustomIterable");
             Logging.logger().severe(msg);
             throw new IllegalStateException(msg);
         }

         this.renderables.remove(renderable);
     }
     *****/
     public void removeSurfaceShape(SurfaceShape shape)
     {
         if (shape == null)
         {
             // TODO: CHANGE WORDING...
             String msg = Logging.getMessage("nullValue.RenderableIsNull");
             Logging.logger().severe(msg);
             throw new IllegalArgumentException(msg);
         }
         if (this.renderablesOverride != null)
         {
             // TODO:  NEEDED HERE?
             String msg = Logging.getMessage("generic.LayerIsUsingCustomIterable");
             Logging.logger().severe(msg);
             throw new IllegalStateException(msg);
         }

         this.shapes.remove(shape);
     }

    /**
     * Clears the contents of this layer's internal Renderable collection.
     * If this layer's internal collection has been overriden with a call to {@link #setRenderables},
     * this will throw an exception.
     *
     * @throws IllegalStateException If a custom Iterable has been specified by a call to <code>setRenderables</code>.
     */
/******
 public void removeAllRenderables()
 {
     if (this.renderablesOverride != null)
     {
         String msg = Logging.getMessage("generic.LayerIsUsingCustomIterable");
         Logging.logger().severe(msg);
         throw new IllegalStateException(msg);
     }

     clearRenderables();
 }

 private void clearRenderables()
 {
     if (this.renderables != null && this.renderables.size() > 0)
         this.renderables.clear();
 }
 *********/
public void removeAllSurfaceShapes()
{
    if (this.renderablesOverride != null)
    {
        // TODO: NEEDED?
        String msg = Logging.getMessage("generic.LayerIsUsingCustomIterable");
        Logging.logger().severe(msg);
        throw new IllegalStateException(msg);
    }

    clearShapes();
}

    private void clearShapes()
    {
        if (this.shapes != null && this.shapes.size() > 0)
            this.shapes.clear();
    }

    /**
     * Returns the Iterable of Renderables currently in use by this layer.
     * If the caller has specified a custom Iterable via {@link #setRenderables}, this will returns a reference
     * to that Iterable. If the caller passed <code>setRenderables</code> a null parameter,
     * or if <code>setRenderables</code> has not been called, this returns a view of this layer's internal
     * collection of Renderables.
     *
     * @return Iterable of currently active Renderables.
     */
    /****
    public Iterable<Renderable> getRenderables()
    {
        return getActiveRenderables();
    }
     *****/
    public Iterable<SurfaceShape> getSurfaceShapes()
    {
        return getActiveShapes();
    }

    /**
     * Returns the Iterable of currently active Renderables.
     * If the caller has specified a custom Iterable via {@link #setRenderables}, this will returns a reference
     * to that Iterable. If the caller passed <code>setRenderables</code> a null parameter,
     * or if <code>setRenderables</code> has not been called, this returns a view of this layer's internal
     * collection of Renderables.
     *
     * @return Iterable of currently active Renderables.
     */
    /******
    private Iterable<Renderable> getActiveRenderables()
    {
        if (this.renderablesOverride != null)
        {
            return this.renderablesOverride;
        }
        else
        {
            // Return an unmodifiable reference to the internal list of renderables.
            // This prevents callers from changing this list and invalidating any invariants we have established.
            return java.util.Collections.unmodifiableCollection(this.renderables);
        }
    }
     ******/
    private Iterable<SurfaceShape> getActiveShapes()
    {
        if (this.renderablesOverride != null)
        {
            // TODO:  NEED TO UNDERSTAND THIS AND WHAT TO RETURN HERE
            return null; //return this.renderablesOverride;
        }
        else
        {
            // Return an unmodifiable reference to the internal list of renderables.
            // This prevents callers from changing this list and invalidating any invariants we have established.
            return java.util.Collections.unmodifiableCollection(this.shapes);
        }
    }

    /**
     * Overrides the collection of currently active Renderables with the specified <code>renderableIterable</code>.
     * This layer will maintain a reference to <code>renderableIterable</code> strictly for picking and rendering.
     * This layer will not modify the reference, or dispose of its contents. This will also clear and dispose of
     * the internal collection of Renderables, and will prevent any modification to its contents via
     * <code>addRenderable, addRenderables, removeRenderables, or dispose</code>.
     *
     * If the specified <code>renderableIterable</code> is null, this layer will revert to maintaining its internal
     * collection.
     *
     * @param renderableIterable Iterable to use instead of this layer's internal collection, or null to use this
     *                           layer's internal collection.
     */
    /***********   TODO: WHAT THE HECK IS THIS...SKIP FOR NOW....
    public void setRenderables(Iterable<Renderable> renderableIterable)
    {
        this.renderablesOverride = renderableIterable;
        // Dispose of the internal collection of Renderables.
        disposeRenderables();
        // Clear the internal collection of Renderables.
        clearRenderables();
    }
     *********/

    /**
     * Disposes the contents of this layer's internal Renderable collection, but does not remove any elements from
     * that collection.
     *
     * @throws IllegalStateException If a custom Iterable has been specified by a call to <code>setRenderables</code>.
     */
    /************** TODO: SKIP THESE FOR NOW....
    public void dispose()
    {
        if (this.renderablesOverride != null)
        {
            String msg = Logging.getMessage("generic.LayerIsUsingCustomIterable");
            Logging.logger().severe(msg);
            throw new IllegalStateException(msg);
        }

        disposeRenderables();
    }

    private void disposeRenderables()
    {
        if (this.renderables != null && this.renderables.size() > 0)
        {
            for (Renderable renderable : this.renderables)
            {
                if (renderable instanceof Disposable)
                    ((Disposable) renderable).dispose();
            }
        }
    }
     ************/

    /*************************
    @Override
    protected void doPick(DrawContext dc, java.awt.Point pickPoint)
    {
        this.pickSupport.clearPickList();
        this.pickSupport.beginPicking(dc);

        for (Renderable renderable : getActiveRenderables())
        {
            // If the caller has specified their own Iterable,
            // then we cannot make any guarantees about its contents.
            if (renderable != null)
            {
                float[] inColor = new float[4];
                dc.getGL().glGetFloatv(GL.GL_CURRENT_COLOR, inColor, 0);
                java.awt.Color color = dc.getUniquePickColor();
                dc.getGL().glColor3ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue());

                renderable.render(dc);

                dc.getGL().glColor4fv(inColor, 0);

                if (renderable instanceof Locatable)
                {
                    this.pickSupport.addPickableObject(color.getRGB(), renderable,
                        ((Locatable) renderable).getPosition(), false);
                }
                else
                {
                    this.pickSupport.addPickableObject(color.getRGB(), renderable);
                }
            }
        }

        this.pickSupport.resolvePick(dc, pickPoint, this.delegateOwner != null ? this.delegateOwner : this);
        this.pickSupport.endPicking(dc);
    }

    @Override
    protected void doRender(DrawContext dc)
    {
        for (Renderable renderable : getActiveRenderables())
        {
            // If the caller has specified their own Iterable,
            // then we cannot make any guarantees about its contents.
            if (renderable != null)
                renderable.render(dc);
        }
    }
    *************/
    @Override
    protected void doPick(DrawContext dc, java.awt.Point pickPoint)
    {
        /***************************
        this.pickSupport.clearPickList();
        this.pickSupport.beginPicking(dc);

        for (SurfaceShape shape : getActiveShapes())
        {
            // If the caller has specified their own Iterable,
            // then we cannot make any guarantees about its contents.
            if (shape != null)
            {
                float[] inColor = new float[4];
                dc.getGL().glGetFloatv(GL.GL_CURRENT_COLOR, inColor, 0);
                java.awt.Color color = dc.getUniquePickColor();
                dc.getGL().glColor3ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue());

                shape.render(dc);

                dc.getGL().glColor4fv(inColor, 0);

                if (shape instanceof Locatable)
                {
                    this.pickSupport.addPickableObject(color.getRGB(), shape,
                        ((Locatable) shape).getPosition(), false);
                }
                else
                {
                    this.pickSupport.addPickableObject(color.getRGB(), shape);
                }
            }
        }

        this.pickSupport.resolvePick(dc, pickPoint, this.delegateOwner != null ? this.delegateOwner : this);
        this.pickSupport.endPicking(dc);
         ********************/
    }

    @Override
    protected void doRender(DrawContext dc)
    {
        SectorGeometryList sgList = dc.getSurfaceGeometry();
        Sector visibleSector = dc.getVisibleSector();

        int numSectors = 0;
        int numShapesTotal = 0;

        for (SectorGeometry sg : sgList)
        {
            Sector sgSector = sg.getSector();
            if (!sgSector.intersects(visibleSector))
                continue;
            TextureTile tile = null;
            BufferedImage texture = null;
            for (SurfaceShape shape : getActiveShapes())
            {
                int numShapes = 0;
                ArrayList<Sector> shapeSectors = shape.getSectors();
                for (Sector s : shapeSectors)
                {
                    if (s.intersects(sgSector))
                    {
                        if (tile == null)
                        {
                            tile = new TextureTile(sgSector);
                            texture = new BufferedImage(DEFAULT_TEXTURE_WIDTH, DEFAULT_TEXTURE_HEIGHT,
                                BufferedImage.TYPE_4BYTE_ABGR);
                            numSectors++;
                        }
                        shape.renderIntoTexture(dc.getGlobe(), sgSector, texture);
                        numShapesTotal++;
                        numShapes++;
                    }
                }
            }

            if (tile != null)
            {
                TextureData td = new TextureData(GL.GL_RGBA, GL.GL_RGBA, false, texture);
                td.setMustFlipVertically(false);
                tile.setTextureData(td);
                GL gl = dc.getGL();

                gl.glPushAttrib(GL.GL_COLOR_BUFFER_BIT | GL.GL_POLYGON_BIT);

                try
                {
                    if (!dc.isPickingMode())
                    {
                        gl.glEnable(GL.GL_BLEND);
                        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
                    }

                    gl.glPolygonMode(GL.GL_FRONT, GL.GL_FILL);
                    gl.glEnable(GL.GL_CULL_FACE);
                    gl.glCullFace(GL.GL_BACK);

                    SurfaceTileRenderer tr = dc.getGeographicSurfaceTileRenderer();
                    tr.setShowImageTileOutlines(true);
                    tr.renderTile(dc, tile);
                }
                finally
                {
                    gl.glPopAttrib();
                }
            }
        }
        //RLB System.out.println(numSectors + ", " + numShapesTotal);
        
    }

    /**
     * Returns this layer's delegate owner, or null if none has been specified.
     *
     * @return Layer that is this layer's delegate owner.
     */
    public Layer getDelegateOwner()
    {
        return delegateOwner;
    }

    @Override
    public String toString()
    {
        // TODO: CHANGE NAME
        return Logging.getMessage("layers.RenderableLayer.Name");
    }
}