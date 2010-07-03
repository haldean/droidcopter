/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.cache.TextureCache;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.pick.*;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.terrain.*;
import gov.nasa.worldwind.util.*;

import javax.media.opengl.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.logging.Level;

/**
 * @author tag
 * @version $Id: AbstractSceneController.java 13005 2010-01-14 00:30:41Z dcollins $
 */
public abstract class AbstractSceneController extends WWObjectImpl implements SceneController
{
    private Model model;
    private View view;
    private double verticalExaggeration = 1d;
    private DrawContext dc = new DrawContextImpl();
    private PickedObjectList lastPickedObjects;// These are for tracking performance
    private long frame = 0;
    private long timebase = System.currentTimeMillis();
    private double framesPerSecond;
    private double frameTime;
    private double pickTime;
    private Point pickPoint = null;
    private TextureCache textureCache;
    private TextRendererCache textRendererCache = new TextRendererCache();
    private Set<String> perFrameStatisticsKeys = new HashSet<String>();
    private Collection<PerformanceStatistic> perFrameStatistics = new ArrayList<PerformanceStatistic>();
    private ScreenCreditController screenCreditController;
    private GLRuntimeCapabilities glRuntimeCaps = new GLRuntimeCapabilities();

    public AbstractSceneController()
    {
        this.setVerticalExaggeration(Configuration.getDoubleValue(AVKey.VERTICAL_EXAGGERATION, 1d));
    }

    public void reinitialize()
    {
        if (this.textRendererCache != null)
            this.textRendererCache.dispose();
        this.textRendererCache = new TextRendererCache();
    }

    /** Releases resources associated with this scene controller. */
    public void dispose()
    {
        if (this.lastPickedObjects != null)
            this.lastPickedObjects.clear();
        this.lastPickedObjects = null;

        if (this.dc != null)
            this.dc.dispose();

        if (this.textRendererCache != null)
            this.textRendererCache.dispose();
    }

    public TextureCache getTextureCache()
    {
        return textureCache;
    }

    public void setTextureCache(TextureCache textureCache)
    {
        this.textureCache = textureCache;
    }

    public TextRendererCache getTextRendererCache()
    {
        return textRendererCache;
    }

    public Model getModel()
    {
        return this.model;
    }

    public View getView()
    {
        return this.view;
    }

    public void setModel(Model model)
    {
        if (this.model != null)
            this.model.removePropertyChangeListener(this);
        if (model != null)
            model.addPropertyChangeListener(this);

        Model oldModel = this.model;
        this.model = model;
        this.firePropertyChange(AVKey.MODEL, oldModel, model);
    }

    public void setView(View view)
    {
        if (this.view != null)
            this.view.removePropertyChangeListener(this);
        if (view != null)
            view.addPropertyChangeListener(this);

        View oldView = this.view;
        this.view = view;

        this.firePropertyChange(AVKey.VIEW, oldView, view);
    }

    public void setVerticalExaggeration(double verticalExaggeration)
    {
        Double oldVE = this.verticalExaggeration;
        this.verticalExaggeration = verticalExaggeration;
        this.firePropertyChange(AVKey.VERTICAL_EXAGGERATION, oldVE, verticalExaggeration);
    }

    public double getVerticalExaggeration()
    {
        return this.verticalExaggeration;
    }

    public void setPickPoint(java.awt.Point pickPoint)
    {
        this.pickPoint = pickPoint;
    }

    public Point getPickPoint()
    {
        return this.pickPoint;
    }

    public PickedObjectList getPickedObjectList()
    {
        return this.lastPickedObjects;
    }

    protected void setPickedObjectList(PickedObjectList pol)
    {
        this.lastPickedObjects = pol;
    }

    public SectorGeometryList getTerrain()
    {
        return this.dc.getSurfaceGeometry();
    }

    public DrawContext getDrawContext()
    {
        return this.dc;
    }

    public double getFramesPerSecond()
    {
        return this.framesPerSecond;
    }

    public double getFrameTime()
    {
        return this.frameTime;
    }

    public void setPerFrameStatisticsKeys(Set<String> keys)
    {
        this.perFrameStatisticsKeys.clear();
        if (keys == null)
            return;

        for (String key : keys)
        {
            if (key != null)
                this.perFrameStatisticsKeys.add(key);
        }
    }

    public Collection<PerformanceStatistic> getPerFrameStatistics()
    {
        return perFrameStatistics;
    }

    public ScreenCreditController getScreenCreditController()
    {
        return screenCreditController;
    }

    public void setScreenCreditController(ScreenCreditController screenCreditController)
    {
        this.screenCreditController = screenCreditController;
    }

    /** {@inheritDoc} */
    public GLRuntimeCapabilities getGLRuntimeCapabilities()
    {
        return this.glRuntimeCaps;
    }

    /** {@inheritDoc} */
    public void setGLRuntimeCapabilities(GLRuntimeCapabilities capabilities)
    {
        if (capabilities == null)
        {
            String message = Logging.getMessage("nullValue.GLRuntimeCapabilitiesIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.glRuntimeCaps = capabilities;
    }

    public int repaint()
    {
        this.frameTime = System.currentTimeMillis();

        this.perFrameStatistics.clear();
        this.glRuntimeCaps.initialize(GLContext.getCurrent());
        this.initializeDrawContext(this.dc);
        this.doRepaint(this.dc);

        ++this.frame;
        long time = System.currentTimeMillis();
        this.frameTime = System.currentTimeMillis() - this.frameTime;
        if (time - this.timebase > 2000) // recalculate every two seconds
        {
            this.framesPerSecond = frame * 1000d / (time - timebase);
            this.timebase = time;
            this.frame = 0;
        }
        this.dc.setPerFrameStatistic(PerformanceStatistic.FRAME_TIME, "Frame Time (ms)", (int) this.frameTime);
        this.dc.setPerFrameStatistic(PerformanceStatistic.FRAME_RATE, "Frame Rate (fps)", (int) this.framesPerSecond);
        this.dc.setPerFrameStatistic(PerformanceStatistic.PICK_TIME, "Pick Time (ms)", (int) this.pickTime);

        Set<String> perfKeys = dc.getPerFrameStatisticsKeys();
        if (perfKeys == null)
            return dc.getRedrawRequested();

        if (perfKeys.contains(PerformanceStatistic.MEMORY_CACHE) || perfKeys.contains(PerformanceStatistic.ALL))
        {
            this.dc.setPerFrameStatistics(WorldWind.getMemoryCacheSet().getPerformanceStatistics());
        }

        if (perfKeys.contains(PerformanceStatistic.TEXTURE_CACHE) || perfKeys.contains(PerformanceStatistic.ALL))
        {
            if (dc.getTextureCache() != null)
                this.dc.setPerFrameStatistic(PerformanceStatistic.TEXTURE_CACHE,
                    "Texture Cache size (Kb)", this.dc.getTextureCache().getUsedCapacity() / 1000);
        }

        if (perfKeys.contains(PerformanceStatistic.JVM_HEAP) || perfKeys.contains(PerformanceStatistic.ALL))
        {
            long totalMemory = Runtime.getRuntime().totalMemory();
            this.dc.setPerFrameStatistic(PerformanceStatistic.JVM_HEAP,
                "JVM total memory (Kb)", totalMemory / 1000);

            this.dc.setPerFrameStatistic(PerformanceStatistic.JVM_HEAP_USED,
                "JVM used memory (Kb)", (totalMemory - Runtime.getRuntime().freeMemory()) / 1000);
        }

        return dc.getRedrawRequested();
    }

    abstract protected void doRepaint(DrawContext dc);

    private void initializeDrawContext(DrawContext dc)
    {
        dc.initialize(GLContext.getCurrent());
        dc.setGLRuntimeCapabilities(this.glRuntimeCaps);
        dc.setPerFrameStatisticsKeys(this.perFrameStatisticsKeys, this.perFrameStatistics);
        dc.setTextureCache(this.textureCache);
        dc.setTextRendererCache(this.textRendererCache);
        dc.setModel(this.model);
        dc.setView(this.view);
        dc.setVerticalExaggeration(this.verticalExaggeration);
        dc.setPickPoint(this.pickPoint);
        dc.setViewportCenterScreenPoint(this.getViewportCenter(dc));
        dc.setFrameTimeStamp(System.currentTimeMillis());
    }

    private Point getViewportCenter(DrawContext dc)
    {
        View view = dc.getView();
        if (view == null)
            return null;

        Rectangle viewport = view.getViewport();
        if (viewport == null)
            return null;

        return new Point((int) (viewport.getCenterX() + 0.5), (int) (viewport.getCenterY() + 0.5));
    }

    protected void initializeFrame(DrawContext dc)
    {
        if (dc.getGLContext() == null)
        {
            String message = Logging.getMessage("BasicSceneController.GLContextNullStartRedisplay");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        javax.media.opengl.GL gl = dc.getGL();

        gl.glPushAttrib(GL.GL_VIEWPORT_BIT | GL.GL_ENABLE_BIT | GL.GL_TRANSFORM_BIT);

        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glPushMatrix();
        gl.glLoadIdentity();

        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glPushMatrix();
        gl.glLoadIdentity();

        gl.glEnable(GL.GL_DEPTH_TEST);
    }

    protected void clearFrame(DrawContext dc)
    {
        Color cc = dc.getClearColor();
        dc.getGL().glClearColor(cc.getRed(), cc.getGreen(), cc.getBlue(), cc.getAlpha());
        dc.getGL().glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
    }

    protected void finalizeFrame(DrawContext dc)
    {
        GL gl = dc.getGL();

        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glPopMatrix();

        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glPopMatrix();

        gl.glPopAttrib();

//        checkGLErrors(dc);
    }

    protected void applyView(DrawContext dc)
    {
        if (dc.getView() != null)
            dc.getView().apply(dc);
    }

    protected void createTerrain(DrawContext dc)
    {
        if (dc.getSurfaceGeometry() == null)
        {
            if (dc.getModel() != null && dc.getModel().getGlobe() != null)
            {
                SectorGeometryList sgl = dc.getModel().getGlobe().tessellate(dc);
                dc.setSurfaceGeometry(sgl);
                dc.setVisibleSector(sgl.getSector());
            }

            if (dc.getSurfaceGeometry() == null)
            {
                Logging.logger().warning("generic.NoSurfaceGeometry");
                dc.setPerFrameStatistic(PerformanceStatistic.TERRAIN_TILE_COUNT, "Terrain Tiles", 0);
                // keep going because some layers, etc. may have meaning w/o surface geometry
            }

            dc.setPerFrameStatistic(PerformanceStatistic.TERRAIN_TILE_COUNT, "Terrain Tiles",
                dc.getSurfaceGeometry().size());
        }
    }

    protected void preRender(DrawContext dc)
    {
        if (dc.getLayers() == null)
            return;

        for (Layer layer : dc.getLayers())
        {
            try
            {
                dc.setCurrentLayer(layer);
                layer.preRender(dc);
            }
            catch (Exception e)
            {
                String message = Logging.getMessage("SceneController.ExceptionWhilePickingInLayer",
                    (layer != null ? layer.getClass().getName() : Logging.getMessage("term.unknown")));
                Logging.logger().log(Level.SEVERE, message, e);
                // Don't abort; continue on to the next layer.
            }
        }

        dc.setCurrentLayer(null);
    }

    private ArrayList<Point> pickPoints = new ArrayList<Point>();

    protected void pickTerrain(DrawContext dc)
    {
        if (dc.isPickingMode() && dc.getVisibleSector() != null && dc.getSurfaceGeometry() != null &&
            dc.getSurfaceGeometry().size() > 0)
        {
            this.pickPoints.clear();
            if (dc.getPickPoint() != null)
                this.pickPoints.add(dc.getPickPoint());

            // Clear viewportCenterPosition.
            dc.setViewportCenterPosition(null);
            Point vpc = dc.getViewportCenterScreenPoint();
            if (vpc != null)
                this.pickPoints.add(vpc);

            if (this.pickPoints.size() == 0)
                return;

            List<PickedObject> pickedObjects = dc.getSurfaceGeometry().pick(dc, this.pickPoints);
            if (pickedObjects == null || pickedObjects.size() == 0)
                return;

            for (PickedObject po : pickedObjects)
            {
                if (po == null)
                    continue;
                if (po.getPickPoint().equals(dc.getPickPoint()))
                    dc.addPickedObject(po);
                else if (po.getPickPoint().equals(vpc))
                    dc.setViewportCenterPosition((Position) po.getObject());
            }
        }
    }

    protected void pickLayers(DrawContext dc)
    {
        if (dc.getLayers() != null)
        {
            for (Layer layer : dc.getLayers())
            {
                try
                {
                    if (layer != null && layer.isPickEnabled())
                    {
                        dc.setCurrentLayer(layer);
                        layer.pick(dc, dc.getPickPoint());
                    }
                }
                catch (Exception e)
                {
                    String message = Logging.getMessage("SceneController.ExceptionWhilePickingInLayer",
                        (layer != null ? layer.getClass().getName() : Logging.getMessage("term.unknown")));
                    Logging.logger().log(Level.SEVERE, message, e);
                    // Don't abort; continue on to the next layer.
                }
            }

            dc.setCurrentLayer(null);
        }
    }

    protected void resolveTopPick(DrawContext dc)
    {
        // Make a last reading to find out which is a top (resultant) color
        PickedObjectList pickedObjectsList = dc.getPickedObjects();
        if (pickedObjectsList != null && (pickedObjectsList.size() == 1))
        {
            pickedObjectsList.get(0).setOnTop();
        }
        else if (pickedObjectsList != null && (pickedObjectsList.size() > 1))
        {
            java.nio.ByteBuffer pixel = com.sun.opengl.util.BufferUtil.newByteBuffer(3);
            GL gl = dc.getGL();
            int yInGLCoords = dc.getView().getViewport().height - dc.getPickPoint().y - 1;
            gl.glReadPixels(dc.getPickPoint().x, yInGLCoords, 1, 1,
                GL.GL_RGB,
                GL.GL_UNSIGNED_BYTE, pixel);

            Color topColor = new Color(pixel.get(0) & 0xff, pixel.get(1) & 0xff, pixel.get(2) & 0xff, 0);
            int colorCode = topColor.getRGB();
            if (0 != colorCode)
            {   // let's find the picked object in the list and set "OnTop" flag
                for (PickedObject po : pickedObjectsList)
                {
                    if (null != po && po.getColorCode() == colorCode)
                    {
                        po.setOnTop();
                        break;
                    }
                }
            }
        } // end of top pixel reading
    }

    protected void pick(DrawContext dc)
    {
        this.pickTime = System.currentTimeMillis();
        this.lastPickedObjects = null;

        try
        {
            dc.enablePickingMode();
            this.pickTerrain(dc);

            if (dc.getPickPoint() == null)
                return;

            this.pickLayers(dc);

            if (this.screenCreditController != null)
                this.screenCreditController.pick(dc, dc.getPickPoint());

            // Pick against the deferred/ordered renderables
            while (dc.getOrderedRenderables().peek() != null)
            {
                dc.getOrderedRenderables().poll().pick(dc, dc.getPickPoint());
            }

            this.resolveTopPick(dc);
            this.lastPickedObjects = new PickedObjectList(dc.getPickedObjects());
        }
        catch (Throwable e)
        {
            Logging.logger().log(Level.SEVERE, Logging.getMessage("BasicSceneController.ExceptionDuringPick"), e);
        }
        finally
        {
            dc.disablePickingMode();
            this.pickTime = System.currentTimeMillis() - this.pickTime;
        }
    }

    protected void draw(DrawContext dc)
    {
        try
        {
            if (dc.getLayers() != null)
            {
                for (Layer layer : dc.getLayers())
                {
                    try
                    {
                        if (layer != null)
                        {
                            dc.setCurrentLayer(layer);
                            layer.render(dc);
                        }
                    }
                    catch (Exception e)
                    {
                        String message = Logging.getMessage("SceneController.ExceptionWhileRenderingLayer",
                            (layer != null ? layer.getClass().getName() : Logging.getMessage("term.unknown")));
                        Logging.logger().log(Level.SEVERE, message, e);
                        // Don't abort; continue on to the next layer.
                    }
                }

                dc.setCurrentLayer(null);
            }

            if (this.screenCreditController != null)
                this.screenCreditController.render(dc);

            while (dc.getOrderedRenderables().peek() != null)
            {
                dc.getOrderedRenderables().poll().render(dc);
            }

            // Diagnostic displays.
            if (dc.getSurfaceGeometry() != null && dc.getModel() != null && (dc.getModel().isShowWireframeExterior() ||
                dc.getModel().isShowWireframeInterior() || dc.getModel().isShowTessellationBoundingVolumes()))
            {
                Model model = dc.getModel();

                float[] previousColor = new float[4];
                dc.getGL().glGetFloatv(GL.GL_CURRENT_COLOR, previousColor, 0);

                for (SectorGeometry sg : dc.getSurfaceGeometry())
                {
                    if (model.isShowWireframeInterior() || model.isShowWireframeExterior())
                        sg.renderWireframe(dc, model.isShowWireframeInterior(), model.isShowWireframeExterior());

                    if (model.isShowTessellationBoundingVolumes())
                    {
                        dc.getGL().glColor3d(1, 0, 0);
                        sg.renderBoundingVolume(dc);
                    }
                }

                dc.getGL().glColor4fv(previousColor, 0);
            }
        }
        catch (Throwable e)
        {
            Logging.logger().log(Level.SEVERE, Logging.getMessage("BasicSceneController.ExceptionDuringRendering"), e);
        }
    }

    /**
     * Called to check for openGL errors. This method includes a "round-trip" between the application and renderer,
     * which is slow. Therefore, this method is excluded from the "normal" render pass. It is here as a matter of
     * convenience to developers, and is not part of the API.
     *
     * @param dc the relevant <code>DrawContext</code>
     */
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    protected void checkGLErrors(DrawContext dc)
    {
        GL gl = dc.getGL();

        for (int err = gl.glGetError(); err != GL.GL_NO_ERROR; err = gl.glGetError())
        {
            String msg = dc.getGLU().gluErrorString(err);
            msg += err;
            Logging.logger().severe(msg);
        }
    }
}
