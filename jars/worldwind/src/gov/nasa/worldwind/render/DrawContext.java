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
 * @version $Id: DrawContext.java 13049 2010-01-27 03:08:26Z tgaskins $
 */
public interface DrawContext extends WWObject, Disposable
{
    /**
     * Assigns this <code>DrawContext</code> a new </code>javax.media.opengl.GLContext</code>. May throw a
     * <code>NullPointerException</code> if <code>glContext</code> is null.
     *
     * @param glContext the new <code>javax.media.opengl.GLContext</code>
     *
     * @throws NullPointerException if glContext is null
     * @since 1.5
     */
    void setGLContext(GLContext glContext);

    /**
     * Retrieves this <code>DrawContext</code>s </code>javax.media.opengl.GLContext</code>. If this method returns null,
     * then there are potentially no active <code>GLContext</code>s and rendering should be aborted.
     *
     * @return this <code>DrawContext</code>s </code>javax.media.opengl.GLContext</code>.
     *
     * @since 1.5
     */
    GLContext getGLContext();

    /**
     * Retrieves the current <code>javax.media.opengl.GL</code>. A <code>GL</code> or <code>GLU</code> is required for
     * all graphical rendering in World Wind.
     *
     * @return the current <code>GL</code> if available, null otherwise
     *
     * @since 1.5
     */
    GL getGL();

    /**
     * Retrieves the current <code>javax.media.opengl.glu.GLU</code>. A <code>GLU</code> or <code>GL</code> is required
     * for all graphical rendering in World Wind.
     *
     * @return the current <code>GLU</code> if available, null otherwise
     *
     * @since 1.5
     */
    GLU getGLU();

    /**
     * Retrieves the current<code>javax.media.opengl.GLDrawable</code>. A <code>GLDrawable</code> can be used to create
     * a <code>GLContext</code>, which can then be used for rendering.
     *
     * @return the current <code>GLDrawable</code>, null if none available
     *
     * @since 1.5
     */
    GLDrawable getGLDrawable();

    /**
     * Retrieves the drawable width of this <code>DrawContext</code>.
     *
     * @return the drawable width of this <code>DrawCOntext</code>
     *
     * @since 1.5
     */
    int getDrawableWidth();

    /**
     * Retrieves the drawable height of this <code>DrawContext</code>.
     *
     * @return the drawable height of this <code>DrawCOntext</code>
     *
     * @since 1.5
     */
    int getDrawableHeight();

    /**
     * Returns the {@link GLRuntimeCapabilities} associated with this DrawContext.
     *
     * @return this DrawContext's associated GLRuntimeCapabilities.
     */
    GLRuntimeCapabilities getGLRuntimeCapabilities();

    /**
     * Sets the {@link GLRuntimeCapabilities} associated with this DrawContext to the specified parameter.
     *
     * @param capabilities the GLRuntimeCapabilities to be associated with this DrawContext.
     *
     * @throws IllegalArgumentException if the capabilities are null.
     */
    void setGLRuntimeCapabilities(GLRuntimeCapabilities capabilities);

    /**
     * Initializes this <code>DrawContext</code>. This method should be called at the beginning of each frame to prepare
     * the <code>DrawContext</code> for the coming render pass.
     *
     * @param glContext the <code>javax.media.opengl.GLContext</code> to use for this render pass
     *
     * @since 1.5
     */
    void initialize(GLContext glContext);

    /**
     * Assigns a new <code>View</code>. Some layers cannot function properly with a null <code>View</code>. It is
     * recommended that the <code>View</code> is never set to null during a normal render pass.
     *
     * @param view the enw <code>View</code>
     *
     * @since 1.5
     */
    void setView(View view);

    /**
     * Retrieves the current <code>View</code>, which may be null.
     *
     * @return the current <code>View</code>, which may be null
     *
     * @since 1.5
     */
    View getView();

    /**
     * Assign a new <code>Model</code>. Some layers cannot function properly with a null <code>Model</code>. It is
     * recommended that the <code>Model</code> is never set to null during a normal render pass.
     *
     * @param model the new <code>Model</code>
     *
     * @since 1.5
     */
    void setModel(Model model);

    /**
     * Retrieves the current <code>Model</code>, which may be null.
     *
     * @return the current <code>Model</code>, which may be null
     *
     * @since 1.5
     */
    Model getModel();

    /**
     * Retrieves the current <code>Globe</code>, which may be null.
     *
     * @return the current <code>Globe</code>, which may be null
     *
     * @since 1.5
     */
    Globe getGlobe();

    /**
     * Retrieves a list containing all the current layers. No guarantee is made about the order of the layers.
     *
     * @return a <code>LayerList</code> containing all the current layers
     *
     * @since 1.5
     */
    LayerList getLayers();

    /**
     * Retrieves a <code>Sector</code> which is at least as large as the current visible sector. The value returned is
     * the value passed to <code>SetVisibleSector</code>. This method may return null.
     *
     * @return a <code>Sector</code> at least the size of the curernt visible sector, null if unavailable
     *
     * @since 1.5
     */
    Sector getVisibleSector();

    /**
     * Sets the visible <code>Sector</code>. The new visible sector must completely encompass the Sector which is
     * visible on the display.
     *
     * @param s the new visible <code>Sector</code>
     *
     * @since 1.5
     */
    void setVisibleSector(Sector s);

    /**
     * Sets the vertical exaggeration. Vertical exaggeration affects the appearance of areas with varied elevation. A
     * vertical exaggeration of zero creates a surface which exactly fits the shape of the underlying
     * <code>Globe</code>. A vertical exaggeration of 3 will create mountains and valleys which are three times as
     * high/deep as they really are.
     *
     * @param verticalExaggeration the new vertical exaggeration.
     *
     * @since 1.5
     */
    void setVerticalExaggeration(double verticalExaggeration);

    /**
     * Retrieves the current vertical exaggeration. Vertical exaggeration affects the appearance of areas with varied
     * elevation. A vertical exaggeration of zero creates a surface which exactly fits the shape of the underlying
     * <code>Globe</code>. A vertical exaggeration of 3 will create mountains and valleys which are three times as
     * high/deep as they really are.
     *
     * @return the current vertical exaggeration
     *
     * @since 1.5
     */
    double getVerticalExaggeration();

    /**
     * Retrieves a list of all the sectors rendered so far this frame.
     *
     * @return a <code>SectorGeometryList</code> containing every <code>SectorGeometry</code> rendered so far this
     *         render pass.
     *
     * @since 1.5
     */
    SectorGeometryList getSurfaceGeometry();

    /**
     * Returns the list of objects picked during the most recent pick traversal.
     *
     * @return the list of picked objects
     */
    PickedObjectList getPickedObjects();

    /**
     * Adds a collection of picked objects to the current picked-object list
     *
     * @param pickedObjects the objects to add
     */
    void addPickedObjects(PickedObjectList pickedObjects);

    /**
     * Adds a single insatnce of the picked object to the current picked-object list
     *
     * @param pickedObject the object to add
     */
    void addPickedObject(PickedObject pickedObject);

    /**
     * Returns a unique color to serve as a pick identifier during picking.
     *
     * @return a unique pick color
     */
    java.awt.Color getUniquePickColor();

    java.awt.Color getClearColor();

    /** Enables color picking mode */
    void enablePickingMode();

    /**
     * Returns true if the Picking mode is active, otherwise return false
     *
     * @return true for Picking mode, otherwise false
     */
    boolean isPickingMode();

    /** Disables color picking mode */
    void disablePickingMode();

    void addOrderedRenderable(OrderedRenderable orderedRenderable);

    java.util.Queue<OrderedRenderable> getOrderedRenderables();

    void drawUnitQuad();

    void drawUnitQuad(TextureCoords texCoords);

    void setSurfaceGeometry(SectorGeometryList surfaceGeometry);

    Vec4 getPointOnGlobe(Angle latitude, Angle longitude);

    Vec4 getScreenPoint(Angle latitude, Angle longitude, double metersElevation);

    Vec4 getScreenPoint(Position position);

    Vec4 getScreenPoint(Vec4 modelPoint);

    SurfaceTileRenderer getGeographicSurfaceTileRenderer();

    Point getPickPoint();

    void setPickPoint(Point pickPoint);

    TextureCache getTextureCache();

    void setTextureCache(TextureCache textureCache);

    Collection<PerformanceStatistic> getPerFrameStatistics();

    void setPerFrameStatisticsKeys(Set<String> statKeys, Collection<PerformanceStatistic> stats);

    void setPerFrameStatistic(String key, String displayName, Object statistic);

    void setPerFrameStatistics(Collection<PerformanceStatistic> stats);

    Set<String> getPerFrameStatisticsKeys();

    Point getViewportCenterScreenPoint();

    void setViewportCenterScreenPoint(Point viewportCenterPoint);

    Position getViewportCenterPosition();

    void setViewportCenterPosition(Position viewportCenterPosition);

    TextRendererCache getTextRendererCache();

    void setTextRendererCache(TextRendererCache textRendererCache);

    /**
     * Returns the draw context's annotation renderer, typically used by annotations that are not contained in an {@link
     * AnnotationLayer}.
     *
     * @return the annotation renderer.
     */
    AnnotationRenderer getAnnotationRenderer();

    /**
     * Since {@link Annotation}s are {@link Renderable}s, they can be exist outside an {@link AnnotationLayer}, in which
     * case they are responsible for rendering themselves. The draw context's annotation renderer provides an active
     * renderer for that purpose.
     *
     * @param annotationRenderer the new annotation renderer for the draw context.
     *
     * @throws IllegalArgumentException if <code>annotationRenderer</code> is null;
     */
    void setAnnotationRenderer(AnnotationRenderer annotationRenderer);

    long getFrameTimeStamp();

    void setFrameTimeStamp(long frameTimeStamp);

    /**
     * Returns the visible sectors at one of several specified resolutions within a specfied search sector. Several
     * sectors resolutions may be specified along with a time limit. The best resolution that can be determined within
     * the time limit is returned.
     * <p/>
     * Adherence to the time limit is not precise. The limit is checked only between full searches at each resolution.
     * The search may take more than the specified time, but will terminate if no time is left before starting a
     * higher-resolution search.
     *
     * @param resolutions  the resolutions of the sectors to return, in latitude.
     * @param timeLimit    the amount of time, in milliseconds, to allow for searching.
     * @param searchSector the sector to decompose into visible sectors.
     *
     * @return the visible sectors at the best resolution achievable given the time limit. The actual resolution can be
     *         determined by examining the delta-latitude value of any of the returnced sectors.
     *
     * @throws IllegalArgumentException if the resolutions array or the search sector is null.
     */
    List<Sector> getVisibleSectors(double[] resolutions, long timeLimit, Sector searchSector);

    /**
     * Sets the current-layer field to the specified layer or null. The field is informative only and enables layer
     * contents to determine their containing layer.
     *
     * @param layer the current layer or null.
     */
    void setCurrentLayer(Layer layer);

    /**
     * Returns the current-layer. The field is informative only and enables layer contents to determine their containing
     * layer.
     *
     * @return the current layer, or null if no layer is current.
     */
    Layer getCurrentLayer();

    void addScreenCredit(ScreenCredit credit);

    Map<ScreenCredit, Long> getScreenCredits();

    int getRedrawRequested();

    void setRedrawRequested(int redrawRequested);

    /**
     * Gets the FrustumList containing all the current Pick Frustums
     *
     * @return FrustumList of Pick Frustums
     */
    PickPointFrustumList getPickFrustums();

    /**
     * Set the size (in pixels) of the pick point frustum at the near plane.
     *
     * @param dim dimension of pick point frustum
     */
    void setPickPointFrustumDimension(Dimension dim);

    /**
     * Gets the dimension of the current Pick Point Frustum
     *
     * @return the dimension of the current Pick Point Frustum
     */
    Dimension getPickPointFrustumDimension();

    /**
     * Creates a frustum around the current pickpoint and adds it to the list of Pick Frustums The frustum size is set
     * with setPickPointFrustumSize().
     */
    void addPickPointFrustum();

    /**
     * Modifies the current projection matrix to slightly offset subsequently drawn objects toward or away from the eye
     * point. This gives those objects visual priority over objects at the same or nearly the same position. After the
     * objects are drawn, call {@link #popProjectionOffest()} to cancel the effect for subsequently drawn objects.
     * <p/>
     * <em>Note:</em> This capability is meant to be applied only within a single Renderable. It is not intended as a
     * means to offset a whole Renderable or collection of Renderables.
     * <p/>
     * See "Mathematics for Game Programming and 3D Computer Graphics, 2 ed." by  Eric Lengyel, Section 9.1, "Depth
     * Value Offset" for a description of this technique.
     *
     * @param offset a reference to an offset value, typically near 1.0, or null to request use of the default value.
     *               Values less than 1.0 pull objects toward the eye point, values greater than 1.0 push objects away
     *               from the eye point. The default value is 0.99.
     *
     * @see #popProjectionOffest()
     */
    void pushProjectionOffest(Double offset);

    /**
     * Removes the current projection matrix offset added by {@link #pushProjectionOffest(Double)}.
     *
     * @see #pushProjectionOffest(Double)
     */
    void popProjectionOffest();
}
