/*
Copyright (C) 2001, 2006, 2007 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.pick.*;

import javax.media.opengl.GL;

/**
 * Represent a text label and its rendering attributes.
 *
 * @author Patrick Murris
 * @version $Id: Annotation.java 13150 2010-02-20 20:19:13Z tgaskins $
 */
public interface Annotation extends Renderable, Disposable, Restorable
{
    public static final String IMAGE_REPEAT_NONE = "render.Annotation.RepeatNone";
    public static final String IMAGE_REPEAT_X = "render.Annotation.RepeatX";
    public static final String IMAGE_REPEAT_Y = "render.Annotation.RepeatY";
    public static final String IMAGE_REPEAT_XY = "render.Annotation.RepeatXY";

    public final static int ANTIALIAS_DONT_CARE = GL.GL_DONT_CARE;
    public final static int ANTIALIAS_FASTEST = GL.GL_FASTEST;
    public final static int ANTIALIAS_NICEST = GL.GL_NICEST;

    public final static String SIZE_FIXED = "render.Annotation.SizeFixed";
    public final static String SIZE_FIT_TEXT = "render.Annotation.SizeFitText";

    boolean isAlwaysOnTop();

    void setAlwaysOnTop(boolean alwaysOnTop);

    boolean isPickEnabled();

    void setPickEnabled(boolean enable);

    String getText();

    void setText(String text);

    AnnotationAttributes getAttributes();

    void setAttributes(AnnotationAttributes attrs);

    java.util.List<? extends Annotation> getChildren();

    void addChild(Annotation annotation);

    boolean removeChild(Annotation annotation);

    void removeAllChildren();

    AnnotationLayoutManager getLayout();

    void setLayout(AnnotationLayoutManager layoutManager);

    PickSupport getPickSupport();

    void setPickSupport(PickSupport pickSupport);

    Object getDelegateOwner();

    void setDelegateOwner(Object delegateOwner);

    java.awt.Dimension getPreferredSize(DrawContext dc);

    /**
     * Draws the annotation immedately on the specified DrawContext. Rendering is not be delayed by use of the
     * DrawContext's ordered mechanism, or any other delayed rendering mechanism. This is typically called by an
     * AnnotationRenderer while batch rendering. The GL should have its model view set to the identity matrix.
     *
     * @param dc the current DrawContext.
     *
     * @throws IllegalArgumentException if <code>dc</code> is null.
     */
    void renderNow(DrawContext dc);

    /**
     * Draws the annotation without transforming to its screen position, or applying any scaling. This Annotation is
     * draw with the specified width, height, and opacity. The GL should have its model view set to whatever
     * transformation is desired.
     *
     * @param dc           the current DrawContext.
     * @param width        the width of the Annotation.
     * @param height       the height of the Annotation.
     * @param opacity      the opacity of the Annotation.
     * @param pickPosition the picked Position assigned to the Annotation, if picking is enabled.
     *
     * @throws IllegalArgumentException if <code>dc</code> is null.
     */
    void draw(DrawContext dc, int width, int height, double opacity, Position pickPosition);

    /**
     * Get the annotation bounding {@link java.awt.Rectangle} using OGL coordinates - bottom-left corner x and y
     * relative to the {@link WorldWindow} bottom-left corner, and the annotation callout width and height.
     * <p/>
     * The annotation offset from it's reference point is factored in such that the callout leader shape and reference
     * point are included in the bounding rectangle.
     *
     * @param dc the current DrawContext.
     *
     * @return the annotation bounding {@link java.awt.Rectangle} using OGL viewport coordinates.
     *
     * @throws IllegalArgumentException if <code>dc</code> is null.
     */
    java.awt.Rectangle getBounds(DrawContext dc);

    /**
     * Returns the minimum eye altititude, in meters, for which the annotation is displayed.
     *
     * @return the minimum altitude, in meters, for which the annotation is displayed.
     *
     * @see {@link #setMinActiveAltitude(double)}
     * @see {@link #getMaxActiveAltitude()}
     */
    double getMinActiveAltitude();

    /**
     * Specifies the minimum eye altititude, in meters, for which the annotation is displayed.
     *
     * @param minActiveAltitude the minimum altitude, in meters, for which the annotation is displayed.
     *
     * @see {@link #getMinActiveAltitude()}
     * @see {@link #setMaxActiveAltitude(double)}
     */
    void setMinActiveAltitude(double minActiveAltitude);

    /**
     * Returns the maximum eye altititude, in meters, for which the annotation is displayed.
     *
     * @return the maximum altitude, in meters, for which the annotation is displayed.
     *
     * @see {@link #setMaxActiveAltitude(double)}
     * @see {@link #getMinActiveAltitude()}
     */
    double getMaxActiveAltitude();

    /**
     * Specifies the maximum eye altititude, in meters, for which the annotation is displayed.
     *
     * @param maxActiveAltitude the maximum altitude, in meters, for which the annotation is displayed.
     *
     * @see {@link #getMaxActiveAltitude()}
     * @see {@link #setMinActiveAltitude(double)}
     */
    void setMaxActiveAltitude(double maxActiveAltitude);
}
