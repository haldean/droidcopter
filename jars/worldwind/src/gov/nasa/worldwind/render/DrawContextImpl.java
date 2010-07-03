/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render;

import com.sun.opengl.util.texture.TextureCoords;
import gov.nasa.worldwind.*;
import gov.nasa.worldwind.cache.TextureCache;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.layers.*;
import gov.nasa.worldwind.pick.*;
import gov.nasa.worldwind.terrain.SectorGeometryList;
import gov.nasa.worldwind.util.*;

import javax.media.opengl.*;
import javax.media.opengl.glu.GLU;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * @author Tom Gaskins
 * @version $Id: DrawContextImpl.java 13049 2010-01-27 03:08:26Z tgaskins $
 */
public class DrawContextImpl extends WWObjectImpl implements DrawContext
{
    private long frameTimestamp;
    private GLContext glContext;
    private GLRuntimeCapabilities glRuntimeCaps;
    private GLU glu = new GLU();
    private View view;
    private Model model;
    private Globe globe;
    private double verticalExaggeration = 1d;
    private Sector visibleSector;
    private SectorGeometryList surfaceGeometry;
    private PickedObjectList pickedObjects = new PickedObjectList();
    private int uniquePickNumber = 0;
    private Color clearColor = new Color(0, 0, 0, 0);
    private boolean isPickingMode = false;
    private Point pickPoint = null;
    private Point viewportCenterScreenPoint = null;
    private Position viewportCenterPosition = null;
//    private Vec4 viewportCenterSurfacePoint = null; // TODO: verify that these should be unused
//    private Vec4 viewportCenterGlobePoint = null;
    private SurfaceTileRenderer geographicSurfaceTileRenderer = new GeographicSurfaceTileRenderer();
    private AnnotationRenderer annotationRenderer = new BasicAnnotationRenderer();
    private TextureCache textureCache;
    private TextRendererCache textRendererCache;
    private Set<String> perFrameStatisticsKeys;
    private Collection<PerformanceStatistic> perFrameStatistics;
    private SectorVisibilityTree visibleSectors;
    private Layer currentLayer;
    private int redrawRequested = 0;
    private PickPointFrustumList pickFrustumList = new PickPointFrustumList();
    private Dimension pickPointFrustumDimension = new Dimension(3, 3);

    PriorityQueue<OrderedRenderable> orderedRenderables =
        new PriorityQueue<OrderedRenderable>(100, new Comparator<OrderedRenderable>()
        {
            public int compare(OrderedRenderable orA, OrderedRenderable orB)
            {
                double eA = orA.getDistanceFromEye();
                double eB = orB.getDistanceFromEye();

                return eA > eB ? -1 : eA == eB ? 0 : 1;
            }
        });

    /**
     * Free internal resources held by this draw context. A GL context must be current when this method is called.
     *
     * @throws javax.media.opengl.GLException - If an OpenGL context is not current when this method is called.
     */
    public void dispose()
    {
        this.geographicSurfaceTileRenderer.dispose();
    }

    public final GL getGL()
    {
        return this.getGLContext().getGL();
    }

    public final GLU getGLU()
    {
        return this.glu;
    }

    public final GLContext getGLContext()
    {
        return this.glContext;
    }

    public final int getDrawableHeight()
    {
        return this.getGLDrawable().getHeight();
    }

    public final int getDrawableWidth()
    {
        return this.getGLDrawable().getWidth();
    }

    public final GLDrawable getGLDrawable()
    {
        return this.getGLContext().getGLDrawable();
    }

    public GLRuntimeCapabilities getGLRuntimeCapabilities()
    {
        return this.glRuntimeCaps;
    }

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

    public final void initialize(GLContext glContext)
    {
        if (glContext == null)
        {
            String message = Logging.getMessage("nullValue.GLContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.glContext = glContext;

        this.visibleSector = null;
        if (this.surfaceGeometry != null)
            this.surfaceGeometry.clear();
        this.surfaceGeometry = null;

        this.pickedObjects.clear();
        this.orderedRenderables.clear();
        this.uniquePickNumber = 0;

        this.pickFrustumList.clear();

        this.currentLayer = null;
    }

    public final void setModel(Model model)
    {
        this.model = model;
        if (this.model == null)
            return;

        Globe g = this.model.getGlobe();
        if (g != null)
            this.globe = g;
    }

    public final Model getModel()
    {
        return this.model;
    }

    public final LayerList getLayers()
    {
        return this.model.getLayers();
    }

    public final Sector getVisibleSector()
    {
        return this.visibleSector;
    }

    public final void setVisibleSector(Sector s)
    {
        // don't check for null - it is possible that no globe is active, no view is active, no sectors visible, etc.
        this.visibleSector = s;
    }

    public void setSurfaceGeometry(SectorGeometryList surfaceGeometry)
    {
        this.surfaceGeometry = surfaceGeometry;
    }

    public SectorGeometryList getSurfaceGeometry()
    {
        return surfaceGeometry;
    }

    public final Globe getGlobe()
    {
        return this.globe != null ? this.globe : this.model.getGlobe();
    }

    public final void setView(View view)
    {
        this.view = view;
    }

    public final View getView()
    {
        return this.view;
    }

    public final void setGLContext(GLContext glContext)
    {
        if (glContext == null)
        {
            String message = Logging.getMessage("nullValue.GLContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.glContext = glContext;
    }

    public final double getVerticalExaggeration()
    {
        return verticalExaggeration;
    }

    public final void setVerticalExaggeration(double verticalExaggeration)
    {
        this.verticalExaggeration = verticalExaggeration;
    }

    public TextureCache getTextureCache()
    {
        return textureCache;
    }

    public void setTextureCache(TextureCache textureCache)
    {
        if (textureCache == null)
        {
            String msg = Logging.getMessage("nullValue.TextureCacheIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        this.textureCache = textureCache;
    }

    public TextRendererCache getTextRendererCache()
    {
        return textRendererCache;
    }

    public void setTextRendererCache(TextRendererCache textRendererCache)
    {
        if (textRendererCache == null)
        {
            String msg = Logging.getMessage("nullValue.TextRendererCacheIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        this.textRendererCache = textRendererCache;
    }

    public AnnotationRenderer getAnnotationRenderer()
    {
        return annotationRenderer;
    }

    public void setAnnotationRenderer(AnnotationRenderer ar)
    {
        if (ar == null)
        {
            String msg = Logging.getMessage("nullValue.AnnotationRendererIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        annotationRenderer = ar;
    }

    public Point getPickPoint()
    {
        return pickPoint;
    }

    public void setPickPoint(Point pickPoint)
    {
        this.pickPoint = pickPoint;
    }

    public Point getViewportCenterScreenPoint()
    {
        return viewportCenterScreenPoint;
    }

    public void setViewportCenterScreenPoint(Point viewportCenterScreenPoint)
    {
        this.viewportCenterScreenPoint = viewportCenterScreenPoint;
    }

    public Position getViewportCenterPosition()
    {
        return viewportCenterPosition;
    }

    public void setViewportCenterPosition(Position viewportCenterPosition)
    {
        this.viewportCenterPosition = viewportCenterPosition;
        // TODO: Verfiy that the code below is indeed supposed to be dead.
//        this.viewportCenterGlobePoint = null;
//        this.viewportCenterSurfacePoint = null;
//        if (viewportCenterPosition != null)
//        {
//            if (this.getGlobe() != null)
//                this.viewportCenterGlobePoint = this.getGlobe().computePointFromPosition(
//                    this.viewportCenterPosition.getLatitude(), this.viewportCenterPosition.getLongitude(), 0d);
//
//            if (this.getSurfaceGeometry() != null)
//                this.viewportCenterSurfacePoint =
//                    this.getSurfaceGeometry().getSurfacePoint(this.viewportCenterPosition);
//        }
    }

    /**
     * Add picked objects to the current list of picked objects.
     *
     * @param pickedObjects the list of picked objects to add
     *
     * @throws IllegalArgumentException if <code>pickedObjects is null</code>
     */
    public void addPickedObjects(PickedObjectList pickedObjects)
    {
        if (pickedObjects == null)
        {
            String msg = Logging.getMessage("nullValue.PickedObjectList");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (this.pickedObjects == null)
        {
            this.pickedObjects = pickedObjects;
            return;
        }

        for (PickedObject po : pickedObjects)
        {
            this.pickedObjects.add(po);
        }
    }

    /**
     * Adds a single insatnce of the picked object to the current picked-object list
     *
     * @param pickedObject the object to add
     *
     * @throws IllegalArgumentException if <code>picked Object is null</code>
     */
    public void addPickedObject(PickedObject pickedObject)
    {
        if (null == pickedObject)
        {
            String msg = Logging.getMessage("nullValue.PickedObject");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (null == this.pickedObjects)
            this.pickedObjects = new PickedObjectList();

        this.pickedObjects.add(pickedObject);
    }

    public PickedObjectList getPickedObjects()
    {
        return this.pickedObjects;
    }

    public Color getUniquePickColor()
    {
        this.uniquePickNumber++;
        int clearColorCode = this.getClearColor().getRGB();

        if (clearColorCode == this.uniquePickNumber)
            this.uniquePickNumber++;

        if (this.uniquePickNumber >= 0x00FFFFFF)
        {
            this.uniquePickNumber = 1;  // no black, no white
            if (clearColorCode == this.uniquePickNumber)
                this.uniquePickNumber++;
        }

        return new Color(this.uniquePickNumber, true); // has alpha
    }

    public Color getClearColor()
    {
        return this.clearColor;
    }

    /**
     * Returns true if the Picking mode is active, otherwise return false
     *
     * @return true for Picking mode, otherwise false
     */
    public boolean isPickingMode()
    {
        return this.isPickingMode;
    }

    /** Enables color picking mode */
    public void enablePickingMode()
    {
        this.isPickingMode = true;
    }

    /** Disables color picking mode */
    public void disablePickingMode()
    {
        this.isPickingMode = false;
    }

    public void addOrderedRenderable(OrderedRenderable orderedRenderable)
    {
        if (null == orderedRenderable)
        {
            String msg = Logging.getMessage("nullValue.OrderedRenderable");
            Logging.logger().warning(msg);
            return; // benign event
        }

        this.orderedRenderables.add(orderedRenderable);
    }

    public java.util.Queue<OrderedRenderable> getOrderedRenderables()
    {
        return this.orderedRenderables;
    }

    public void drawUnitQuad()
    {
        GL gl = this.getGL();

        gl.glBegin(GL.GL_QUADS); // TODO: use a vertex array or vertex buffer
        gl.glVertex2d(0d, 0d);
        gl.glVertex2d(1, 0d);
        gl.glVertex2d(1, 1);
        gl.glVertex2d(0d, 1);
        gl.glEnd();
    }

    public void drawUnitQuad(TextureCoords texCoords)
    {
        GL gl = this.getGL();

        gl.glBegin(GL.GL_QUADS); // TODO: use a vertex array or vertex buffer
        gl.glTexCoord2d(texCoords.left(), texCoords.bottom());
        gl.glVertex2d(0d, 0d);
        gl.glTexCoord2d(texCoords.right(), texCoords.bottom());
        gl.glVertex2d(1, 0d);
        gl.glTexCoord2d(texCoords.right(), texCoords.top());
        gl.glVertex2d(1, 1);
        gl.glTexCoord2d(texCoords.left(), texCoords.top());
        gl.glVertex2d(0d, 1);
        gl.glEnd();
    }

    public Vec4 getPointOnGlobe(Angle latitude, Angle longitude)
    {
        if (latitude == null || longitude == null)
        {
            String message = Logging.getMessage("nullValue.LatitudeOrLongitudeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (this.getVisibleSector() == null)
            return null;

        if (!this.getVisibleSector().contains(latitude, longitude))
            return null;

        SectorGeometryList sectorGeometry = this.getSurfaceGeometry();
        if (sectorGeometry != null)
        {
            Vec4 p = sectorGeometry.getSurfacePoint(latitude, longitude);
            if (p != null)
                return p;
        }

        return null;
    }

    public Vec4 getScreenPoint(Angle latitude, Angle longitude, double metersElevation)
    {
        if (latitude == null || longitude == null)
        {
            String message = Logging.getMessage("nullValue.LatitudeOrLongitudeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (this.getGlobe() == null)
            return null;

        Vec4 modelPoint = this.getGlobe().computePointFromPosition(latitude, longitude, metersElevation);
        if (modelPoint == null)
            return null;

        return this.getScreenPoint(modelPoint);
    }

    public Vec4 getScreenPoint(Position position)
    {
        if (position == null)
        {
            String message = Logging.getMessage("nullValue.PositionIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return this.getScreenPoint(position.getLatitude(), position.getLongitude(), position.getElevation());
    }

    public Vec4 getScreenPoint(Vec4 modelPoint)
    {
        if (modelPoint == null)
        {
            String message = Logging.getMessage("nullValue.Vec4IsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (this.getView() == null)
            return null;

        return this.getView().project(modelPoint);
    }

    public SurfaceTileRenderer getGeographicSurfaceTileRenderer()
    {
        return this.geographicSurfaceTileRenderer;
    }

    public Collection<PerformanceStatistic> getPerFrameStatistics()
    {
        return this.perFrameStatistics;
    }

    public void setPerFrameStatisticsKeys(Set<String> statKeys, Collection<PerformanceStatistic> stats)
    {
        this.perFrameStatisticsKeys = statKeys;
        this.perFrameStatistics = stats;
    }

    public Set<String> getPerFrameStatisticsKeys()
    {
        return perFrameStatisticsKeys;
    }

    public void setPerFrameStatistic(String key, String displayName, Object value)
    {
        if (this.perFrameStatistics == null || this.perFrameStatisticsKeys == null)
            return;

        if (key == null)
        {
            String message = Logging.getMessage("nullValue.KeyIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (displayName == null)
        {
            String message = Logging.getMessage("nullValue.DisplayNameIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (this.perFrameStatisticsKeys.contains(key) || this.perFrameStatisticsKeys.contains(PerformanceStatistic.ALL))
            this.perFrameStatistics.add(new PerformanceStatistic(key, displayName, value));
    }

    public void setPerFrameStatistics(Collection<PerformanceStatistic> stats)
    {
        if (this.perFrameStatistics == null || this.perFrameStatisticsKeys == null)
            return;

        if (stats == null)
        {
            String message = Logging.getMessage("nullValue.KeyIsNull"); // TODO
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        for (PerformanceStatistic stat : stats)
        {
            this.perFrameStatistics.add(stat);
        }
    }

    public long getFrameTimeStamp()
    {
        return this.frameTimestamp;
    }

    public void setFrameTimeStamp(long frameTimeStamp)
    {
        this.frameTimestamp = frameTimeStamp;
    }

    public List<Sector> getVisibleSectors(double[] resolutions, long timeLimit, Sector sector)
    {
        if (resolutions == null)
        {
            String message = Logging.getMessage("nullValue.ArrayIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (timeLimit <= 0)
        {
            String message = Logging.getMessage("generic.TimeNegative", timeLimit);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (sector == null)
            sector = this.visibleSector;

        if (this.visibleSectors == null)
            this.visibleSectors = new SectorVisibilityTree();
        else if (this.visibleSectors.getSectorSize() == resolutions[resolutions.length - 1]
            && this.visibleSectors.getTimeStamp() == this.frameTimestamp)
            return this.visibleSectors.getSectors();

        long start = System.currentTimeMillis();
        List<Sector> sectors = this.visibleSectors.refresh(this, resolutions[0], sector);
        for (int i = 1; i < resolutions.length && (System.currentTimeMillis() < start + timeLimit); i++)
        {
            sectors = this.visibleSectors.refresh(this, resolutions[i], sectors);
        }

        this.visibleSectors.setTimeStamp(this.frameTimestamp);

        return this.visibleSectors.getSectors();
    }

    public void setCurrentLayer(Layer layer)
    {
        this.currentLayer = layer;
    }

    public Layer getCurrentLayer()
    {
        return this.currentLayer;
    }

    private LinkedHashMap<ScreenCredit, Long> credits = new LinkedHashMap<ScreenCredit, Long>();

    public void addScreenCredit(ScreenCredit credit)
    {
        this.credits.put(credit, this.frameTimestamp);
    }

    public Map<ScreenCredit, Long> getScreenCredits()
    {
        return this.credits;
    }

    public int getRedrawRequested()
    {
        return redrawRequested;
    }

    public void setRedrawRequested(int redrawRequested)
    {
        this.redrawRequested = redrawRequested;
    }

    /**
     * Gets the FrustumList containing all the current Pick Frustums
     *
     * @return FrustumList of Pick Frustums
     */
    public PickPointFrustumList getPickFrustums()
    {
        return this.pickFrustumList;
    }

    /**
     * Set the size (in pixels) of the pick point frustum at the near plane.
     *
     * @param dim dimension of pick point frustum
     */
    public void setPickPointFrustumDimension(Dimension dim)
    {
        if (dim == null)
        {
            String message = Logging.getMessage("nullValue.DimensionIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (dim.width < 3 || dim.height < 3)
        {
            String message = Logging.getMessage("DrawContext.PickPointFrustumDimensionTooSmall");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.pickPointFrustumDimension = new Dimension(dim);
    }

    /**
     * Gets the dimension of the current Pick Point Frustum
     *
     * @return the dimension of the current Pick Point Frustum
     */
    public Dimension getPickPointFrustumDimension()
    {
        return this.pickPointFrustumDimension;
    }

    /**
     * Creates a frustum around the current pickpoint and adds it to the list of Pick Frustums. The frustum size is set
     * with setPickPointFrustumSize().
     */
    public void addPickPointFrustum()
    {
        //Compute the current picking frustum
        if (getPickPoint() != null)
        {
            Rectangle viewport = getView().getViewport();

            double viewportWidth = viewport.getWidth() <= 0.0 ? 1.0 : viewport.getWidth();
            double viewportHeight = viewport.getHeight() <= 0.0 ? 1.0 : viewport.getHeight();

            //Get the pick point and translate screen center to zero
            Point ptCenter = new Point(getPickPoint());
            ptCenter.y = (int) viewportHeight - ptCenter.y;
            ptCenter.translate((int) (-viewportWidth / 2), (int) (-viewportHeight / 2));

            //Number of pixels around pick point to include in frustum
            int offsetX = pickPointFrustumDimension.width / 2;
            int offsetY = pickPointFrustumDimension.height / 2;

            //If the frustum is not valid then don't add it and return silently
            if (offsetX == 0 || offsetY == 0)
                return;

            //Compute the distance to the near plane in screen coordinates
            double width = getView().getFieldOfView().tanHalfAngle() * getView().getNearClipDistance();
            double x = width / (viewportWidth / 2.0);
            double screenDist = getView().getNearClipDistance() / x;

            //Create the four vectors that define the top-left, top-right, bottom-left, and bottom-right vectors
            Vec4 vTL = new Vec4(ptCenter.x - offsetX, ptCenter.y + offsetY, -screenDist);
            Vec4 vTR = new Vec4(ptCenter.x + offsetX, ptCenter.y + offsetY, -screenDist);
            Vec4 vBL = new Vec4(ptCenter.x - offsetX, ptCenter.y - offsetY, -screenDist);
            Vec4 vBR = new Vec4(ptCenter.x + offsetX, ptCenter.y - offsetY, -screenDist);

            //Compute the frustum from these four vectors
            Frustum frustum = Frustum.fromPerspectiveVecs(vTL, vTR, vBL, vBR,
                getView().getNearClipDistance(), getView().getFarClipDistance());

            //Create the screen Rectangle associtated with this frustum
            Rectangle rectScreen = new Rectangle(getPickPoint().x - offsetX,
                (int) viewportHeight - getPickPoint().y - offsetY,
                pickPointFrustumDimension.width,
                pickPointFrustumDimension.height);

            //Transform the frustum to Model Coordinates
            Matrix modelviewTranspose = getView().getModelviewMatrix().getTranspose();
            if (modelviewTranspose != null)
                frustum = frustum.transformBy(modelviewTranspose);

            this.pickFrustumList.add(new PickPointFrustum(frustum, rectScreen));
        }
    }

    public void pushProjectionOffest(Double offset)
    {
        // Modify the projection transform to shift the depth values slightly toward the camera in order to
        // ensure the lines are selected during depth buffering.
        GL gl = this.getGL();

        float[] pm = new float[16];
        gl.glGetFloatv(GL.GL_PROJECTION_MATRIX, pm, 0);
        pm[10] *= offset != null ? offset : 0.99; // TODO: See Lengyel 2 ed. Section 9.1.2 to compute optimal offset


        gl.glPushAttrib(GL.GL_TRANSFORM_BIT);
        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glPushMatrix();
        gl.glLoadMatrixf(pm, 0);
    }

    public void popProjectionOffest()
    {
        GL gl = this.getGL();
        
        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glPopMatrix();
        gl.glPopAttrib();
    }
}
