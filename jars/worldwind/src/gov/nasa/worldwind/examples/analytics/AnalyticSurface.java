/* Copyright (C) 2001, 2009 United States Government as represented by 
the Administrator of the National Aeronautics and Space Administration. 
All Rights Reserved. 
*/
package gov.nasa.worldwind.examples.analytics;

import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.pick.PickSupport;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.util.*;

import javax.media.opengl.GL;
import java.awt.*;
import java.util.*;

/**
 * AnalyticSurface represents a connected grid of geographic locations, covering a specified {@link Sector} at a
 * specified base altitude in meters. The number of grid locations is defined by the AnalyticSurface's dimensions. The
 * default dimensions are <code>(10, 10)</code>. Callers specify the dimensions by using one of the constructors
 * accepting <code>width</code> and <code>height</code>, or by invoking {@link #setDimensions(int, int)}. Each grid
 * point has the following set of attributes: <ul> <li>Scalar value : the grid point's height relative to the surface's
 * base altitude, both in meters</li> <li>Color : the grid point's RGBA color components</li> </ul> Callers specify the
 * attributes at each grid point by invoking {@link #setValues(Iterable)} with an {@link Iterable} of {@link
 * GridPointAttributes}. Grid points are assigned attributes from this iterable staring at the upper left hand corner,
 * and proceeding in row-first order across the grid. The iterable should contain at least <code>width * height</code>
 * values, where width and height are the AnalyticSurface's grid dimensions. If the caller does not specify any
 * GridPointAttributes, or the caller specified iterable contains too few values, the unassigned grid points are given
 * default attributes: the default scalar value is 0, and the default color is {@link java.awt.Color#BLACK}.
 *
 * @author dcollins
 * @version $Id: AnalyticSurface.java 13339 2010-04-25 23:16:04Z tgaskins $
 */
public class AnalyticSurface implements Renderable, PreRenderable
{
    /** GridPointAttributes defines the properties associated with a single grid point of an AnalyticSurface. */
    public interface GridPointAttributes
    {
        /**
         * Returns the scalar value associated with a grid point. By default, AnalyticSurface interprets this value as
         * the grid point's height relative to the AnalyticSurface's base altitude, both in meters.
         *
         * @return the grid point's scalar value.
         */
        double getValue();

        /**
         * Returns the {@link java.awt.Color} associated with a grid point. By default, AnalyticSurface interprets this
         * Color as the RGBA components of a grid point's RGBA color.
         *
         * @return the grid point's RGB color components.
         */
        java.awt.Color getColor();
    }

    protected static final double DEFAULT_ALTITUDE = 0d;
    protected static final int DEFAULT_DIMENSION = 10;
    protected static final double DEFAULT_VALUE = 0d;
    protected static final Color DEFAULT_COLOR = Color.BLACK;
    protected static final Color DEFAULT_SHADOW_COLOR = Color.BLACK;
    protected static final double MIN_DISTANCE_FROM_EYE = 1d;
    protected static final GridPointAttributes DEFAULT_GRID_POINT_ATTRIBUTES = createGridPointAttributes(
        DEFAULT_VALUE, DEFAULT_COLOR);

    protected boolean visible = true;
    protected Sector sector;
    protected double altitude;
    protected int width;
    protected int height;
    protected Iterable<? extends GridPointAttributes> values;
    protected double verticalScale = 1d;
    protected AnalyticSurfaceAttributes surfaceAttributes = new AnalyticSurfaceAttributes();
    protected Object pickObject;
    protected Layer clientLayer;
    // Runtime rendering state.
    protected double[] extremeValues;
    protected boolean expired = true;
    protected boolean updateFailed;
    protected Object globeStateKey;
    // Computed surface rendering properties.
    protected Vec4 referencePoint;
    protected RenderInfo surfaceRenderInfo;
    protected SurfaceShadow surfaceShadow;
    protected TiledSurfaceObjectRenderer surfaceShadowRenderer;
    protected final PickSupport pickSupport = new PickSupport();
    protected final OGLStackHandler stackHandler = new OGLStackHandler();
    protected final float[] floatArray = new float[4];

    /**
     * Constructs a new AnalyticSurface with the specified {@link Sector}, base altitude in meters, grid dimensions, and
     * iterable of GridPointAttributes. The iterable should contain at least <code>with * height</code> non-null
     * GridPointAttributes.
     *
     * @param sector   the Sector which defines the surface's geographic region.
     * @param altitude the base altitude to place the surface at, in meters.
     * @param width    the surface grid width, in number of grid points.
     * @param height   the surface grid height, in number of grid points.
     * @param iterable the attributes associated with each grid point.
     *
     * @throws IllegalArgumentException if the sector is null, if the width is less than 1, if the height is less than
     *                                  1, or if the iterable is null.
     */
    public AnalyticSurface(Sector sector, double altitude, int width, int height,
        Iterable<? extends GridPointAttributes> iterable)
    {
        if (sector == null)
        {
            String message = Logging.getMessage("nullValue.SectorIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (width <= 0)
        {
            String message = Logging.getMessage("Geom.WidthInvalid", width);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (height <= 0)
        {
            String message = Logging.getMessage("Geom.HeightInvalid", width);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (iterable == null)
        {
            String message = Logging.getMessage("nullValue.IterableIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.sector = sector;
        this.altitude = altitude;
        this.width = width;
        this.height = height;
        this.values = iterable;
        this.setExpired(true);
    }

    /**
     * Constructs a new AnalyticSurface with the specified {@link Sector}, base altitude in meters, and grid dimensions.
     * The new AnalyticSurface has the default {@link GridPointAttributes}.
     *
     * @param sector   the Sector which defines the surface's geographic region.
     * @param altitude the base altitude to place the surface at, in meters.
     * @param width    the surface grid width, in number of grid points.
     * @param height   the surface grid height, in number of grid points.
     *
     * @throws IllegalArgumentException if the sector is null, if the width is less than 1, or if the height is less
     *                                  than 1.
     */
    public AnalyticSurface(Sector sector, double altitude, int width, int height)
    {
        this(sector, altitude, width, height, createDefaultValues(width * height));
    }

    /**
     * Constructs a new AnalyticSurface with the specified {@link Sector} and base altitude in meters. The new
     * AnalyticSurface has default dimensions of <code>(10, 10)</code>, and default {@link GridPointAttributes}.
     *
     * @param sector   the Sector which defines the surface's geographic region.
     * @param altitude the base altitude to place the surface at, in meters.
     *
     * @throws IllegalArgumentException if the sector is null.
     */
    public AnalyticSurface(Sector sector, double altitude)
    {
        this(sector, altitude, DEFAULT_DIMENSION, DEFAULT_DIMENSION);
    }

    /**
     * Constructs a new AnalyticSurface with the specified grid dimensions. The new AnalyticSurface is has the default
     * Sector {@link Sector#EMPTY_SECTOR}, the default altitude of 0 meters, and default {@link GridPointAttributes}.
     *
     * @param width  the surface grid width, in number of grid points.
     * @param height the surface grid height, in number of grid points.
     *
     * @throws IllegalArgumentException if the sector is null.
     */
    public AnalyticSurface(int width, int height)
    {
        this(Sector.EMPTY_SECTOR, DEFAULT_ALTITUDE, width, height);
    }

    /**
     * Constructs a new AnalyticSurface with the default Sector {@link Sector#EMPTY_SECTOR}, the default altitude of 0
     * meters, default dimensions of <code>(10, 10)</code>, and default {@link GridPointAttributes}.
     */
    public AnalyticSurface()
    {
        this(DEFAULT_DIMENSION, DEFAULT_DIMENSION);
    }

    /**
     * Returns true if the surface is visible in the scene, and false otherwise.
     *
     * @return true if the surface is visible in the scene, and false otherwise
     */
    public boolean isVisible()
    {
        return this.visible;
    }

    /**
     * Sets whether or not the surface is visible in the scene.
     *
     * @param visible true to make the surface visible, and false to make it hidden.
     */
    public void setVisible(boolean visible)
    {
        this.visible = visible;
    }

    /**
     * Returns the {@link Sector} defining the geographic boundary of this surface.
     *
     * @return this surface's geographic boundary, as a Sector.
     */
    public Sector getSector()
    {
        return this.sector;
    }

    /**
     * Sets this surface's geographic boundary, as a {@link Sector}.
     *
     * @param sector this surface's new geographic boundary, as a Sector.
     *
     * @throws IllegalArgumentException if the surface is null.
     */
    public void setSector(Sector sector)
    {
        if (sector == null)
        {
            String message = Logging.getMessage("nullValue.SectorIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.sector = sector;
        this.setExpired(true);
    }

    /**
     * Returns this surface's base altitude, in meters.
     *
     * @return this surface's base altitude, in meters.
     */
    public double getAltitude()
    {
        return altitude;
    }

    /**
     * Sets this surface's base altitude, in meters.
     *
     * @param altitude the new base altitude, in meters.
     */
    public void setAltitude(double altitude)
    {
        this.altitude = altitude;
        this.setExpired(true);
    }

    /**
     * Returns the number of horizontal and vertical points composing this surface as an array with two values. The
     * value at index 0 indicates the grid width, and the value at index 1 indicates the grid height.
     *
     * @return the dimensions of this surface's grid.
     */
    public int[] getDimensions()
    {
        return new int[] {this.width, this.height};
    }

    /**
     * Sets the number of horizontal and vertical points composing this surface.
     *
     * @param width  the new grid width.
     * @param height the new grid height.
     *
     * @throws IllegalArgumentException if either width or heigth are less than 1.
     */
    public void setDimensions(int width, int height)
    {
        if (width <= 0)
        {
            String message = Logging.getMessage("Geom.WidthInvalid", width);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (height <= 0)
        {
            String message = Logging.getMessage("Geom.HeightInvalid", height);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.width = width;
        this.height = height;
        this.setExpired(true);
    }

    /**
     * Returns the surface's iterable of {@link GridPointAttributes}. See {@link #setValues(Iterable)} for details on
     * how this iterable is interpreted by AnalyticSurface.
     *
     * @return this surface's GridPointAttributes.
     */
    public Iterable<? extends GridPointAttributes> getValues()
    {
        return this.values;
    }

    /**
     * Sets this surface's iterable of {@link GridPointAttributes}. Grid points are assigned attributes from this
     * iterable staring at the upper left hand corner, and proceeding in row-first order across the grid. The iterable
     * should contain at least <code>width * height</code> values, where width and height are the AnalyticSurface's grid
     * dimensions. If the iterable contains too few values, the unassigned grid points are given default attributes: the
     * default scalar value is 0, and the default color is {@link java.awt.Color#BLACK}. (totally opaque).
     *
     * @param iterable the new grid point attributes.
     *
     * @throws IllegalArgumentException if the iterable is null.
     */
    public void setValues(Iterable<? extends GridPointAttributes> iterable)
    {
        if (iterable == null)
        {
            String message = Logging.getMessage("nullValue.IterableIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.values = iterable;
        this.extremeValues = computeExtremeValues(iterable);
        this.setExpired(true);
    }

    /**
     * Returns the scale applied to the value at each grid point.
     *
     * @return the surface's vertical scale coefficient.
     */
    public double getVerticalScale()
    {
        return this.verticalScale;
    }

    /**
     * Sets the scale applied to the value at each grid point. Before rendering, this value is applied to each grid
     * points scalar value, thus increasing or decreasing it's height relative to the surface's base altitude, both in
     * meters.
     *
     * @param scale the surface's vertical scale coefficient.
     */
    public void setVerticalScale(double scale)
    {
        this.verticalScale = scale;
        this.setExpired(true);
    }

    /**
     * Returns a copy of the rendering attributes associated with this surface. Modifying the contents of the returned
     * reference has no effect on this surface. In order to make an attribute change take effect, invoke {@link
     * #setSurfaceAttributes(AnalyticSurfaceAttributes)} with the modified attributes.
     *
     * @return a copy of this surface's rendering attributes.
     */
    public AnalyticSurfaceAttributes getSurfaceAttributes()
    {
        return this.surfaceAttributes.copy();
    }

    /**
     * Sets the rendering attributes associated with this surface. The caller cannot assume that modifying the attribute
     * reference after calling setSurfaceAttributes() will have any effect, as the implementation may defensively copy
     * the attribute reference. In order to make an attribute change take effect, invoke
     * setSurfaceAttributes(AnalyticSurfaceAttributes) again with the modified attributes.
     *
     * @param attributes this surface's new rendering attributes.
     *
     * @throws IllegalArgumentException if attributes is null.
     */
    public void setSurfaceAttributes(AnalyticSurfaceAttributes attributes)
    {
        if (attributes == null)
        {
            String message = Logging.getMessage("nullValue.AttributesIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.surfaceAttributes = attributes.copy();
        this.setExpired(true);
    }

    /**
     * Returns the object which is associated with this surface during picking. A null value is permitted and indicates
     * that the surface itself will be the object returned during picking.
     *
     * @return this surface's pick object.
     */
    public Object getPickObject()
    {
        return this.pickObject;
    }

    /**
     * Sets the object associated with this surface during picking. A null value is permitted and indicates that the
     * surface itself will be the object returned during picking.
     *
     * @param pickObject the object to associated with this surface during picking. A null value is permitted and
     *                   indicates that the surface itself will be the object returned during picking.
     */
    public void setPickObject(Object pickObject)
    {
        this.pickObject = pickObject;
    }

    /**
     * Returns the layer associated with this surface during picking.
     *
     * @return this surface's pick layer.
     */
    public Layer getClientLayer()
    {
        return this.clientLayer;
    }

    /**
     * Sets the layer associated with this surface during picking. A null value is permitted, and indicates that no
     * layer is associated with this surface.
     *
     * @param layer this surface's pick layer.
     */
    public void setClientLayer(Layer layer)
    {
        this.clientLayer = layer;
    }

    /**
     * {@inheritDoc}
     *
     * @param dc
     */
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

        if (!this.getExtent(dc).intersects(dc.getView().getFrustumInModelCoordinates()))
            return;

        if (this.isExpired(dc))
            this.update(dc);

        if (this.isExpired(dc))
            return;

        this.doPreRender(dc);
    }

    /**
     * {@inheritDoc}
     *
     * @param dc the <code>DrawContext</code> to be used
     */
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

        if (!this.getExtent(dc).intersects(dc.getView().getFrustumInModelCoordinates()))
            return;

        if (this.isExpired(dc))
            this.update(dc);

        if (this.isExpired(dc))
            return;

        this.doRender(dc);
    }

    /**
     * Returns this surface's extent in model coordinates.
     *
     * @param dc the current DrawContext.
     *
     * @return this surface's extent in the specified DrawContext.
     *
     * @throws IllegalArgumentException if the DrawContext is null.
     */
    public Extent getExtent(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        double minElevation = this.getAltitude();
        double maxElevation = this.getAltitude();

        if (this.extremeValues != null)
        {
            double[] surfaceExtremes = this.computeExtremeAltitudes(this.extremeValues[0], this.extremeValues[1]);
            if (minElevation > surfaceExtremes[0])
                minElevation = surfaceExtremes[0];
            if (maxElevation < surfaceExtremes[1])
                maxElevation = surfaceExtremes[1];
        }

        double[] globeExtremes = dc.getGlobe().getMinAndMaxElevations(this.getSector());
        if (globeExtremes != null)
        {
            if (minElevation > globeExtremes[0])
                minElevation = globeExtremes[0];
            if (maxElevation < globeExtremes[1])
                maxElevation = globeExtremes[1];
        }

        return Sector.computeBoundingCylinder(dc.getGlobe(), dc.getVerticalExaggeration(), this.getSector(),
            minElevation, maxElevation);
    }

    //**************************************************************//
    //********************  Attribute Constrution  *****************//
    //**************************************************************//

    /**
     * Returns the minimum and maximum values in the specified iterable of {@link GridPointAttributes}. Values
     * equivalent to the specified <code>missingDataSignal</code> are ignored. This returns null if the iterable is
     * empty or contains only missing values.
     *
     * @param iterable          the GridPointAttributes to search for the minimum and maximum value.
     * @param missingDataSignal the number indicating a specific value to ignore.
     *
     * @return an array containing the minimum value in index 0 and the maximum value in index 1, or null if the
     *         iterable is empty or contains only missing values.
     *
     * @throws IllegalArgumentException if the iterable is null.
     */
    public static double[] computeExtremeValues(Iterable<? extends GridPointAttributes> iterable,
        double missingDataSignal)
    {
        if (iterable == null)
        {
            String message = Logging.getMessage("nullValue.IterableIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        double minValue = Double.MAX_VALUE;
        double maxValue = -Double.MAX_VALUE;

        for (GridPointAttributes attr : iterable)
        {
            double value = attr.getValue();
            if (Double.compare(value, missingDataSignal) == 0)
                continue;

            if (minValue > value)
                minValue = value;
            if (maxValue < value)
                maxValue = value;
        }

        if (minValue == Double.MAX_VALUE || minValue == -Double.MIN_VALUE)
            return null;

        return new double[] {minValue, maxValue};
    }

    /**
     * Returns the minimum and maximum values in the specified iterable of {@link GridPointAttributes}. Values
     * equivalent to <code>Double.NaN</code> are ignored. This returns null if the buffer is empty or contains only NaN
     * values.
     *
     * @param iterable the GridPointAttributes to search for the minimum and maximum value.
     *
     * @return an array containing the minimum value in index 0 and the maximum value in index 1, or null if the
     *         iterable is empty or contains only NaN values.
     *
     * @throws IllegalArgumentException if the iterable is null.
     */
    public static double[] computeExtremeValues(Iterable<? extends GridPointAttributes> iterable)
    {
        if (iterable == null)
        {
            String message = Logging.getMessage("nullValue.IterableIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return computeExtremeValues(iterable, Double.NaN);
    }

    /**
     * Returns a new instance of {@link GridPointAttributes} with the specified value and color.
     *
     * @param value the new GridPointAttributes' value.
     * @param color the new GridPointAttributes' color.
     *
     * @return a new GridPointAttributes defined by the specified value and color.
     */
    public static GridPointAttributes createGridPointAttributes(final double value, final java.awt.Color color)
    {
        return new AnalyticSurface.GridPointAttributes()
        {
            public double getValue()
            {
                return value;
            }

            public Color getColor()
            {
                return color;
            }
        };
    }

    /**
     * Returns a new instance of {@link GridPointAttributes} with a Color computed from the specified value and value
     * range. The color's RGB components are computed by mapping value's relative position in the range <code>[minValue,
     * maxValue]</code> to the range of color hues <code>[minHue, maxHue]</code>. The color's Alpha component is
     * computed by mapping the values's relative position in the range <code>[minValue, minValue + (maxValue - minValue)
     * / 10]</code> to the range <code>[0, 1]</code>. This has the effect of interpolating hue and alpha based on the
     * grid point value.
     *
     * @param value    the new GridPointAttributes' value.
     * @param minValue the minimum value.
     * @param maxValue the maximum value.
     * @param minHue   the mimimum color hue, corresponding to the minimum value.
     * @param maxHue   the maximum color hue, corresponding to the maximum value.
     *
     * @return a new GridPointAttributes defined by the specified value, value range, and color hue range.
     */
    public static AnalyticSurface.GridPointAttributes createColorGradientAttributes(final double value,
        double minValue, double maxValue, double minHue, double maxHue)
    {
        double hueFactor = WWMath.computeInterpolationFactor(value, minValue, maxValue);
        Color color = Color.getHSBColor((float) WWMath.mixSmooth(hueFactor, minHue, maxHue), 1f, 1f);
        double opacity = WWMath.computeInterpolationFactor(value, minValue, minValue + (maxValue - minValue) * 0.1);
        Color rgbaColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (255 * opacity));

        return createGridPointAttributes(value, rgbaColor);
    }

    /**
     * Returns a new iterable populated with the default {@link GridPointAttributes}. The default GridPointAttributes
     * have a value of 0, and the color {@link java.awt.Color#BLACK}.
     *
     * @param count the desired number of GridPointAttributes to return.
     *
     * @return an iterable containing <code>count</code> default GridPointAttributes.
     */
    public static Iterable<? extends GridPointAttributes> createDefaultValues(int count)
    {
        ArrayList<GridPointAttributes> list = new ArrayList<GridPointAttributes>(count);
        Collections.fill(list, DEFAULT_GRID_POINT_ATTRIBUTES);
        return list;
    }

    /**
     * Returns a new iterable populated with {@link GridPointAttributes} computed by invoking {@link
     * #createColorGradientAttributes(double, double, double, double, double)} for each double value in the speicfied
     * {@link BufferWrapper}. Values equivalent to the specified <code>missingDataSignal</code> are replaced with the
     * specified <code>minValue</code>.
     *
     * @param values            the buffer of values.
     * @param missingDataSignal the number indicating a specific value to ignore.
     * @param minValue          the minimum value.
     * @param maxValue          the maximum value.
     * @param minHue            the mimimum color hue, corresponding to the minimum value.
     * @param maxHue            the maximum color hue, corresponding to the maximum value.
     *
     * @return an iiterable GridPointAttributes defined by the specified buffer of values.
     */
    public static Iterable<? extends AnalyticSurface.GridPointAttributes> createColorGradientValues(
        BufferWrapper values, double missingDataSignal, double minValue, double maxValue, double minHue, double maxHue)
    {
        if (values == null)
        {
            String message = Logging.getMessage("nullValue.BufferIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        ArrayList<AnalyticSurface.GridPointAttributes> attributesList
            = new ArrayList<AnalyticSurface.GridPointAttributes>();

        for (int i = 0; i < values.length(); i++)
        {
            double value = values.getDouble(i);
            if (Double.compare(value, missingDataSignal) == 0)
                value = minValue;

            attributesList.add(createColorGradientAttributes(value, minValue, maxValue, minHue, maxHue));
        }

        return attributesList;
    }

    //**************************************************************//
    //********************  Surface Rendering  *********************//
    //**************************************************************//

    protected void doPreRender(DrawContext dc)
    {
        if (this.surfaceAttributes.isDrawShadow())
        {
            if (this.surfaceShadowRenderer == null)
            {
                this.surfaceShadow = new SurfaceShadow(this);
                this.surfaceShadowRenderer = new TiledSurfaceObjectRenderer();
                this.surfaceShadowRenderer.setPickEnabled(false);
                this.surfaceShadowRenderer.setSurfaceObjects(Arrays.asList(this.surfaceShadow));
            }

            this.surfaceShadowRenderer.preRender(dc);
        }
    }

    protected void doRender(DrawContext dc)
    {
        Extent extent = this.getExtent(dc);
        double distanceFromEye = dc.getView().getEyePoint().distanceTo3(extent.getCenter()) - extent.getRadius();
        if (distanceFromEye < MIN_DISTANCE_FROM_EYE)
            distanceFromEye = MIN_DISTANCE_FROM_EYE;

        dc.addOrderedRenderable(new OrderedSurface(this, distanceFromEye));

        if (!dc.isPickingMode())
            dc.addOrderedRenderable(new OrderedSurfaceShadow(this, Double.MAX_VALUE));
    }

    protected void drawSurfaceShadow(DrawContext dc)
    {
        try
        {
            if (this.surfaceShadowRenderer != null)
                this.surfaceShadowRenderer.render(dc);
        }
        catch (Exception e)
        {
            String message = Logging.getMessage("generic.ExceptionWhileRenderingRenderable");
            Logging.logger().log(java.util.logging.Level.SEVERE, message, e);
        }
    }

    protected void drawSurface(DrawContext dc)
    {
        this.beginShapeRendering(dc);
        try
        {
            this.drawShape(dc);
        }
        catch (Exception e)
        {
            String message = Logging.getMessage("generic.ExceptionWhileRenderingRenderable");
            Logging.logger().log(java.util.logging.Level.SEVERE, message, e);
        }
        finally
        {
            this.endShapeRendering(dc);
        }
    }

    protected void drawShape(DrawContext dc)
    {
        // Draw the airspace shape using a multiple pass algorithm. The motivation for this algorithm is twofold:
        //
        // 1. The airspace outline appears both in front of and behind the shape. If the outline will be drawn using
        //    GL line smoothing, or GL blending, then either the line must be broken into two parts, or rendered in
        //    two passes.
        //
        // These issues are resolved by making several passes for the interior and outline, as follows:

        GL gl = dc.getGL();

        dc.getView().pushReferenceCenter(dc, this.referencePoint);
        try
        {
            this.surfaceRenderInfo.bindSurfaceData(dc);

            // If the outline and interior will be drawn, then draw the outline color, but do not affect the depth buffer
            // (outline pixels do not need the depth test). When the interior is drawn, it will draw on top of these
            // colors, and the outline will be visible behind the potentially transparent interior.
            if (this.surfaceAttributes.isDrawOutline() && this.surfaceAttributes.isDrawInterior())
            {
                gl.glColorMask(true, true, true, true);
                gl.glDepthMask(false);

                this.drawShapeOutline(dc);
            }

            if (this.surfaceAttributes.isDrawInterior())
            {
                gl.glColorMask(true, true, true, true);
                gl.glDepthMask(true);

                this.drawShapeInterior(dc);
            }

            // If the outline will be drawn, then draw the outline color, but do not affect the depth buffer (outline
            // pixels do not need the depth test). This will blend outline pixels with the interior pixels which are
            // behind the outline.
            if (this.surfaceAttributes.isDrawOutline())
            {
                gl.glColorMask(true, true, true, true);
                gl.glDepthMask(false);

                this.drawShapeOutline(dc);
            }
        }
        finally
        {
            dc.getView().popReferenceCenter(dc);
        }
    }

    protected void drawShapeInterior(DrawContext dc)
    {
        GL gl = dc.getGL();

        if (!dc.isPickingMode())
        {
            // Bind the shapes vertex colors as the diffuse material parameter.
            gl.glEnable(GL.GL_COLOR_MATERIAL);
            gl.glColorMaterial(GL.GL_FRONT_AND_BACK, GL.GL_DIFFUSE);
            gl.glEnableClientState(GL.GL_COLOR_ARRAY);
            this.surfaceAttributes.getInteriorMaterial().apply(gl, GL.GL_FRONT_AND_BACK,
                (float) this.surfaceAttributes.getInteriorOpacity());
        }

        gl.glCullFace(GL.GL_FRONT);
        this.surfaceRenderInfo.drawInterior(dc);

        gl.glCullFace(GL.GL_BACK);
        this.surfaceRenderInfo.drawInterior(dc);
    }

    protected void drawShapeOutline(DrawContext dc)
    {
        GL gl = dc.getGL();

        if (!dc.isPickingMode())
        {
            gl.glEnable(GL.GL_LINE_SMOOTH);
            // Unbind the shapes vertex colors as the diffuse material parameter.
            gl.glDisable(GL.GL_LIGHTING);
            gl.glDisable(GL.GL_COLOR_MATERIAL);
            // Set the outline color.
            Color color = this.surfaceAttributes.getOutlineMaterial().getDiffuse();
            // Convert the floating point opacity from the range [0, 1] to the unsigned byte range [0, 255].
            int alpha = (int) (255 * this.surfaceAttributes.getOutlineOpacity() + 0.5);
            gl.glColor4ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue(), (byte) alpha);
        }

        gl.glDisableClientState(GL.GL_COLOR_ARRAY);
        gl.glLineWidth((float) this.surfaceAttributes.getOutlineWidth());
        this.surfaceRenderInfo.drawOutline(dc);

        if (!dc.isPickingMode())
        {
            gl.glEnable(GL.GL_LIGHTING);
            gl.glDisable(GL.GL_LINE_SMOOTH);
            gl.glDisable(GL.GL_LINE_STIPPLE);
        }
    }

    protected void beginShapeRendering(DrawContext dc)
    {
        GL gl = dc.getGL();

        this.stackHandler.pushAttrib(gl, GL.GL_COLOR_BUFFER_BIT // for alpha test func and ref, blend func
            | GL.GL_CURRENT_BIT
            | GL.GL_DEPTH_BUFFER_BIT
            | GL.GL_LINE_BIT // for line width
            | GL.GL_POLYGON_BIT // for cull face
            | (!dc.isPickingMode() ? GL.GL_LIGHTING_BIT : 0) // for lighting.
            | (!dc.isPickingMode() ? GL.GL_TRANSFORM_BIT : 0)); // for normalize state.
        this.stackHandler.pushClientAttrib(gl, GL.GL_CLIENT_VERTEX_ARRAY_BIT);

        // Enable the alpha test.
        gl.glEnable(GL.GL_ALPHA_TEST);
        gl.glAlphaFunc(GL.GL_GREATER, 0.0f);

        gl.glEnable(GL.GL_CULL_FACE);
        gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL.GL_NORMAL_ARRAY);

        if (dc.isPickingMode())
        {
            Color color = dc.getUniquePickColor();

            this.pickSupport.beginPicking(dc);
            this.pickSupport.addPickableObject(color.getRGB(),
                (this.getPickObject() != null) ? this.getPickObject() : this,
                new Position(this.sector.getCentroid(), this.altitude), false);

            gl.glColor3ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue());
        }
        else
        {
            // Enable blending in non-premultiplied color mode. Premultiplied colors don't work with GL fixed
            // functionality lighting.
            gl.glEnable(GL.GL_BLEND);
            OGLUtil.applyBlending(gl, false);

            // Enable lighting with GL_LIGHT1.
            gl.glDisable(GL.GL_COLOR_MATERIAL);
            gl.glDisable(GL.GL_LIGHT0);
            gl.glEnable(GL.GL_LIGHTING);
            gl.glEnable(GL.GL_LIGHT1);
            gl.glEnable(GL.GL_NORMALIZE);
            // Configure the lighting model for two-sided smooth shading.
            gl.glLightModeli(GL.GL_LIGHT_MODEL_LOCAL_VIEWER, GL.GL_TRUE);
            gl.glLightModeli(GL.GL_LIGHT_MODEL_TWO_SIDE, GL.GL_TRUE);
            gl.glShadeModel(GL.GL_SMOOTH);
            // Configure GL_LIGHT1 as a white light eminating from the viewer's eye point.
            OGLUtil.applyLightingDirectionalFromViewer(gl, GL.GL_LIGHT1, new Vec4(1.0, 0.5, 1.0).normalize3());
        }
    }

    protected void endShapeRendering(DrawContext dc)
    {
        this.stackHandler.pop(dc.getGL());

        if (dc.isPickingMode())
        {
            this.pickSupport.endPicking(dc);
            this.pickSupport.resolvePick(dc, dc.getPickPoint(), this.getClientLayer());
        }
    }

    //**************************************************************//
    //********************  Surface Construction  ******************//
    //**************************************************************//

    protected boolean isExpired(DrawContext dc)
    {
        Object gsk = dc.getGlobe().getStateKey(dc);
        return this.expired || (this.globeStateKey != null ? !this.globeStateKey.equals(gsk) : gsk != null);
    }

    protected void setExpired(boolean expired)
    {
        this.expired = expired;

        if (this.expired && this.surfaceShadow != null)
            this.surfaceShadow.updateModifiedTime();
    }

    protected void update(DrawContext dc)
    {
        if (this.updateFailed)
            return;

        try
        {
            this.doUpdate(dc);
            this.setExpired(false);
            this.globeStateKey = dc.getGlobe().getStateKey(dc);
        }
        catch (Exception e)
        {
            String message = Logging.getMessage("generic.ExceptionWhileUpdating", this);
            Logging.logger().log(java.util.logging.Level.SEVERE, message, e);
            this.updateFailed = true;
        }
    }

    protected void doUpdate(DrawContext dc)
    {
        this.referencePoint = dc.getGlobe().computePointFromPosition(
            new Position(this.sector.getCentroid(), this.altitude));

        if (this.surfaceRenderInfo == null ||
            this.surfaceRenderInfo.getGridWidth() != this.width ||
            this.surfaceRenderInfo.getGridHeight() != this.height)
        {
            this.surfaceRenderInfo = RenderInfo.allocate(this.width, this.height, true);
        }

        RenderInfo heapRenderInfo = RenderInfo.allocate(this.width, this.height, false);
        this.updateSurfacePoints(dc, this.referencePoint, heapRenderInfo);
        this.updateSurfaceNormals(heapRenderInfo);
        RenderInfo.copyVertexData(heapRenderInfo, this.surfaceRenderInfo);
    }

    protected void updateSurfacePoints(DrawContext dc, Vec4 referencePoint, RenderInfo outRenderInfo)
    {
        Iterator<? extends GridPointAttributes> iter = this.values.iterator();

        double latStep = -this.sector.getDeltaLatDegrees() / (double) (this.height - 1);
        double lonStep = this.sector.getDeltaLonDegrees() / (double) (this.width - 1);

        double lat = this.sector.getMaxLatitude().degrees;
        for (int y = 0; y < this.height; y++)
        {
            double lon = this.sector.getMinLongitude().degrees;
            for (int x = 0; x < this.width; x++)
            {
                GridPointAttributes attr = iter.hasNext() ? iter.next() : null;
                this.updateSurfacePoint(dc, x, y, Angle.fromDegrees(lat), Angle.fromDegrees(lon), attr, referencePoint,
                    outRenderInfo);

                lon += lonStep;
            }
            lat += latStep;
        }
    }

    protected void updateSurfacePoint(DrawContext dc, int x, int y, Angle lat, Angle lon, GridPointAttributes attr,
        Vec4 referencePoint, RenderInfo outRenderInfo)
    {
        double value = DEFAULT_VALUE;
        if (attr != null)
            value = attr.getValue();

        double metersElevation = this.altitude + this.verticalScale * value;
        Vec4 point = dc.getGlobe().computePointFromPosition(lat, lon, metersElevation);
        if (referencePoint != null)
            point = point.subtract3(referencePoint);

        Color color = DEFAULT_COLOR;
        if (attr != null)
            color = attr.getColor();

        double opacity = (color.getAlpha() / 255d) * this.surfaceAttributes.getInteriorOpacity();

        Color shadowColor = DEFAULT_SHADOW_COLOR;
        double shadowOpacity = opacity * this.surfaceAttributes.getShadowOpacity();

        int index = x + y * this.width;
        point.toFloatArray(this.floatArray, 0, 4);
        outRenderInfo.cartesianVertexTupleBuffer.putFloat(index, this.floatArray);

        this.floatArray[0] = (float) lon.degrees;
        this.floatArray[1] = (float) lat.degrees;
        this.floatArray[2] = 1f;
        this.floatArray[3] = 0f;
        outRenderInfo.geographicVertexTupleBuffer.putFloat(index, this.floatArray);

        color.getRGBColorComponents(this.floatArray);
        this.floatArray[3] = (float) opacity;
        outRenderInfo.colorTupleBuffer.putFloat(index, this.floatArray);

        shadowColor.getRGBColorComponents(this.floatArray);
        this.floatArray[3] = (float) shadowOpacity;
        outRenderInfo.shadowColorTupleBuffer.putFloat(index, this.floatArray);
    }

    protected void updateSurfaceNormals(RenderInfo outRenderInfo)
    {
        computeNormalsFromIndexedTriStrip(outRenderInfo.interiorIndexBuffer.length(), outRenderInfo.interiorIndexBuffer,
            outRenderInfo.getNumVertices(), outRenderInfo.cartesianVertexTupleBuffer,
            outRenderInfo.cartesianNormalTupleBuffer);
    }

    protected static BufferWrapper tessellateInteriorAsTriStrip(int width, int height, BufferFactory factory)
    {
        int numIndices = (height - 1) * (2 * width) + (2 * (height - 2));
        BufferWrapper buffer = factory.newBuffer(numIndices);

        int pos, index = 0;
        for (int y = 0; y < height - 1; y++)
        {
            if (y != 0)
            {
                buffer.putInt(index++, (width - 1) + (y - 1) * width);
                buffer.putInt(index++, width + y * width);
            }

            for (int x = 0; x < width; x++)
            {
                pos = x + y * width;
                buffer.putInt(index++, pos + width);
                buffer.putInt(index++, pos);
            }
        }

        return buffer;
    }

    protected static BufferWrapper tessellateOutlineAsLineStrip(int width, int height, BufferFactory factory)
    {
        int numIndices = 2 * (width + height - 2);
        BufferWrapper buffer = factory.newBuffer(numIndices);

        int index = 0;
        for (int x = 0; x < width; x++)
        {
            buffer.putInt(index++, x);
        }

        for (int y = 1; y < height - 1; y++)
        {
            buffer.putInt(index++, (width - 1) + y * width);
        }

        for (int x = width - 1; x >= 0; x--)
        {
            buffer.putInt(index++, x + (height - 1) * width);
        }

        for (int y = height - 2; y >= 1; y--)
        {
            buffer.putInt(index++, y * width);
        }

        return buffer;
    }

    protected static void computeNormalsFromIndexedTriStrip(int numIndices, BufferWrapper indexBuffer,
        int numVertices, VecBuffer vertexBuffer, VecBuffer outNormalBuffer)
    {
        int[] indexArray = new int[numIndices];
        int[] faceIndices = new int[3];
        Vec4[] vertexArray = new Vec4[numVertices];
        Vec4[] normalArray = new Vec4[numVertices];
        float[] vecArray = new float[4];

        indexBuffer.getInt(0, indexArray, 0, numIndices);

        // Set each normal tuple to the zero vector (0, 0, 0).
        for (int pos = 0; pos < numVertices; pos++)
        {
            vertexBuffer.getFloat(pos, vecArray);
            vertexArray[pos] = Vec4.fromFloatArray(vecArray, 0, 4);
            normalArray[pos] = Vec4.ZERO;
        }

        // Compute the normal for each face, contributing that normal to each vertex of the face.
        for (int i = 2; i < numIndices; i++)
        {
            System.arraycopy(indexArray, i - 2, faceIndices, 0, 3);

            if ((i % 2) != 0)
            {
                int tmp = faceIndices[0];
                faceIndices[0] = faceIndices[1];
                faceIndices[1] = tmp;
            }

            // Compute the normal for this face.
            Vec4 faceNormal = WWMath.computeTriangleNormal(
                vertexArray[faceIndices[0]], vertexArray[faceIndices[1]], vertexArray[faceIndices[2]]);

            // Add this face normal to each the normal of each vertex on this face.
            for (int v = 0; v < 3; v++)
            {
                normalArray[faceIndices[v]] = normalArray[faceIndices[v]].add3(faceNormal);
            }
        }

        // Scale and normalize each normal tuple.
        for (int pos = 0; pos < numVertices; pos++)
        {
            normalArray[pos] = normalArray[pos].normalize3();
            normalArray[pos].toFloatArray(vecArray, 0, 4);
            outNormalBuffer.putFloat(pos, vecArray);
        }
    }

    protected double[] computeExtremeAltitudes(double minValue, double maxValue)
    {
        double altitude = this.getAltitude();
        double scale = this.getVerticalScale();
        return new double[] {altitude + scale * minValue, altitude + scale * maxValue};
    }

    //**************************************************************//
    //********************  Internal Support Classes  **************//
    //**************************************************************//

    protected static class OrderedSurface implements OrderedRenderable
    {
        protected final AnalyticSurface surface;
        protected final double distanceFromEye;

        public OrderedSurface(AnalyticSurface surface, double distanceFromEye)
        {
            this.surface = surface;
            this.distanceFromEye = distanceFromEye;
        }

        public double getDistanceFromEye()
        {
            return this.distanceFromEye;
        }

        public void render(DrawContext dc)
        {
            this.surface.drawSurface(dc);
        }

        public void pick(DrawContext dc, Point pickPoint)
        {
            this.surface.drawSurface(dc);
        }
    }

    protected static class OrderedSurfaceShadow implements OrderedRenderable
    {
        protected final AnalyticSurface surface;
        protected final double distanceFromEye;

        public OrderedSurfaceShadow(AnalyticSurface surface, double distanceFromEye)
        {
            this.surface = surface;
            this.distanceFromEye = distanceFromEye;
        }

        public double getDistanceFromEye()
        {
            return this.distanceFromEye;
        }

        public void render(DrawContext dc)
        {
            this.surface.drawSurfaceShadow(dc);
        }

        public void pick(DrawContext dc, Point pickPoint)
        {
            this.surface.drawSurfaceShadow(dc);
        }
    }

    protected static class RenderInfo
    {
        protected final int gridWidth;
        protected final int gridHeight;
        protected final int numVertices;
        protected final BufferWrapper interiorIndexBuffer;
        protected final BufferWrapper outlineIndexBuffer;
        protected final VecBuffer cartesianVertexTupleBuffer;
        protected final VecBuffer cartesianNormalTupleBuffer;
        protected final VecBuffer geographicVertexTupleBuffer;
        protected final VecBuffer colorTupleBuffer;
        protected final VecBuffer shadowColorTupleBuffer;

        protected RenderInfo(int gridWidth, int gridHeight,
            BufferFactory indexBufferFactory,
            TupleBufferFactory cartesianVertexBufferFactory,
            TupleBufferFactory geographicVertexBufferFactory,
            TupleBufferFactory colorBufferFactory)
        {
            this.gridWidth = gridWidth;
            this.gridHeight = gridHeight;
            this.numVertices = gridWidth * gridHeight;
            this.interiorIndexBuffer = tessellateInteriorAsTriStrip(gridWidth, gridHeight, indexBufferFactory);
            this.outlineIndexBuffer = tessellateOutlineAsLineStrip(gridWidth, gridHeight, indexBufferFactory);
            this.cartesianVertexTupleBuffer = cartesianVertexBufferFactory.newBuffer(this.numVertices);
            this.cartesianNormalTupleBuffer = cartesianVertexBufferFactory.newBuffer(this.numVertices);
            this.geographicVertexTupleBuffer = geographicVertexBufferFactory.newBuffer(this.numVertices);
            this.colorTupleBuffer = colorBufferFactory.newBuffer(this.numVertices);
            this.shadowColorTupleBuffer = colorBufferFactory.newBuffer(this.numVertices);
        }

        public static RenderInfo allocate(int gridWidth, int gridHeight, boolean allocteDirect)
        {
            return new RenderInfo(gridWidth, gridHeight,
                new BufferFactory.IntBufferFactory(allocteDirect),
                new TupleBufferFactory.FloatTupleBufferFactory(3, allocteDirect),
                new TupleBufferFactory.FloatTupleBufferFactory(2, allocteDirect),
                new TupleBufferFactory.FloatTupleBufferFactory(4, allocteDirect));
        }

        public static RenderInfo allocate(int gridWidth, int gridHeight)
        {
            return allocate(gridWidth, gridHeight, true);
        }

        public static void copyVertexData(RenderInfo src, RenderInfo dest)
        {
            dest.cartesianVertexTupleBuffer.putSubBuffer(0, src.cartesianVertexTupleBuffer);
            dest.cartesianNormalTupleBuffer.putSubBuffer(0, src.cartesianNormalTupleBuffer);
            dest.geographicVertexTupleBuffer.putSubBuffer(0, src.geographicVertexTupleBuffer);
            dest.colorTupleBuffer.putSubBuffer(0, src.colorTupleBuffer);
            dest.shadowColorTupleBuffer.putSubBuffer(0, src.shadowColorTupleBuffer);
        }

        public int getGridWidth()
        {
            return this.gridWidth;
        }

        public int getGridHeight()
        {
            return this.gridHeight;
        }

        public int getNumVertices()
        {
            return this.numVertices;
        }

        public void bindSurfaceData(DrawContext dc)
        {
            this.cartesianVertexTupleBuffer.bindAsVertexBuffer(dc);
            this.cartesianNormalTupleBuffer.bindAsNormalBuffer(dc);
            this.colorTupleBuffer.bindAsColorBuffer(dc);
        }

        public void bindShadowData(DrawContext dc)
        {
            this.geographicVertexTupleBuffer.bindAsVertexBuffer(dc);
            this.shadowColorTupleBuffer.bindAsColorBuffer(dc);
        }

        public void drawInterior(DrawContext dc)
        {
            dc.getGL().glDrawRangeElements(GL.GL_TRIANGLE_STRIP, 0, this.numVertices,
                this.interiorIndexBuffer.length(), GL.GL_UNSIGNED_INT, this.interiorIndexBuffer.getBackingBuffer());
        }

        public void drawOutline(DrawContext dc)
        {
            dc.getGL().glDrawRangeElements(GL.GL_LINE_LOOP, 0, this.numVertices,
                this.outlineIndexBuffer.length(), GL.GL_UNSIGNED_INT, this.outlineIndexBuffer.getBackingBuffer());
        }
    }

    protected static class SurfaceShadow extends AbstractSurfaceObject
    {
        protected AnalyticSurface analyticSurface;
        protected final OGLStackHandler stackHandler = new OGLStackHandler();
        protected final SurfaceShapeSupport surfaceShapeSupport = new SurfaceShapeSupport();

        public SurfaceShadow(AnalyticSurface analyticSurface)
        {
            this.analyticSurface = analyticSurface;
        }

        /** Overridden to expose updateModifiedTime as a public method. */
        public void updateModifiedTime()
        {
            super.updateModifiedTime();
        }

        protected void doRenderToRegion(DrawContext dc, Sector sector, int x, int y, int width, int height)
        {
            this.beginRenderToRegion(dc, sector, x, y, width, height);
            try
            {
                this.draw(dc);
            }
            catch (Exception e)
            {
                String message = Logging.getMessage("generic.ExceptionWhileRenderingRenderable");
                Logging.logger().log(java.util.logging.Level.SEVERE, message, e);
            }
            finally
            {
                this.endRenderToRegion(dc);
            }
        }

        protected void draw(DrawContext dc)
        {
            this.analyticSurface.surfaceRenderInfo.bindShadowData(dc);

            if (this.analyticSurface.surfaceAttributes.isDrawInterior())
                this.drawInterior(dc);

            if (this.analyticSurface.surfaceAttributes.isDrawOutline())
                this.drawOutline(dc);
        }

        protected void drawInterior(DrawContext dc)
        {
            GL gl = dc.getGL();

            if (!dc.isPickingMode())
            {
                gl.glEnableClientState(GL.GL_COLOR_ARRAY);
            }

            this.analyticSurface.surfaceRenderInfo.drawInterior(dc);
        }

        protected void drawOutline(DrawContext dc)
        {
            GL gl = dc.getGL();

            if (!dc.isPickingMode())
            {
                gl.glEnable(GL.GL_LINE_SMOOTH);
                gl.glDisableClientState(GL.GL_COLOR_ARRAY);
                gl.glColor4f(0f, 0f, 0f, (float) (this.analyticSurface.surfaceAttributes.getOutlineOpacity()
                    * this.analyticSurface.surfaceAttributes.getShadowOpacity()));
            }

            gl.glLineWidth((float) this.analyticSurface.surfaceAttributes.getOutlineWidth());
            this.analyticSurface.surfaceRenderInfo.drawOutline(dc);
        }

        protected void beginRenderToRegion(DrawContext dc, Sector sector, int x, int y, int width, int height)
        {
            GL gl = dc.getGL();

            this.stackHandler.pushAttrib(gl, GL.GL_COLOR_BUFFER_BIT // for alpha func and ref, blend func
                | GL.GL_CURRENT_BIT // for current RGBA color
                | GL.GL_DEPTH_BUFFER_BIT // for depth test disable
                | GL.GL_LINE_BIT); // for line width
            this.stackHandler.pushClientAttrib(gl, GL.GL_CLIENT_VERTEX_ARRAY_BIT);

            // Enable the alpha test.
            gl.glEnable(GL.GL_ALPHA_TEST);
            gl.glAlphaFunc(GL.GL_GREATER, 0.0f);

            // Disable the depth test.
            gl.glDisable(GL.GL_DEPTH_TEST);

            // Enable client vertex arrays.
            gl.glEnableClientState(GL.GL_VERTEX_ARRAY);

            if (!dc.isPickingMode())
            {
                // Enable blending in non-premultiplied color mode.
                gl.glEnable(GL.GL_BLEND);
                OGLUtil.applyBlending(gl, false);
            }

            // Configure the modelview matrix to transform from geographic coordinates to viewport coordinates.
            this.stackHandler.pushModelview(gl);
            Matrix modelview = Matrix.fromGeographicToViewport(sector, x, y, width, height);
            double[] modelviewArray = modelview.toArray(new double[16], 0, false);
            gl.glMultMatrixd(modelviewArray, 0);
        }

        protected void endRenderToRegion(DrawContext dc)
        {
            this.stackHandler.pop(dc.getGL());
        }

        public Iterable<? extends Sector> getSectors(DrawContext dc, double texelSizeRadians)
        {
            return this.surfaceShapeSupport.adjustSectorsByBorderWidth(Arrays.asList(this.analyticSurface.sector),
                this.analyticSurface.surfaceAttributes.getOutlineWidth(), texelSizeRadians);
        }
    }
}
