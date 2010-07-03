/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render;

import gov.nasa.worldwind.Restorable;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;

/**
 * The SurfaceShape is a common interface for surface conforming shapes such as polygon, sector, ellipse, and
 * quadrilateral. SurfaceShapes implement the {@link PreRenderable} and {@link Renderable} interfaces, so a surface
 * shape may be aggregated within any layer or within some arbitrary rendering code. While SurfaceShapes may be rendered
 * on an individual basis, they are designed to be aggregated with other SurfaceShapes in a {@link
 * gov.nasa.worldwind.layers.SurfaceShapeLayer}. By letting SurfaceShapeLayer handle a collection of shapes in bulk, the
 * shapes share a common set of system resources, which will increse the scalability of rendering a large number of
 * shapes.
 * <p/>
 * Most implementations of SurfaceShape will require that {@link #preRender(DrawContext)} is called before {@link
 * #render(DrawContext)}, and that preRender is called at the appropriate stage in the current rendering cycle.
 * Furthermore, most implementations will be designed such that calling preRender will lock in the visual attributes of
 * the shape for any subsequent calls to render, until the next time preRender is called.
 * <p/>
 * An instance of SurfaceShape is also a {@link SurfaceObject}, which allows SurfaceShape to be used anywhere that a
 * SurfaceObject is accepted. Most importantly, this means that a SurfaceShape does not need to be rendered by calling
 * preRender and render. Instead, a SurfaceShape may be rendered as input to {@link
 * gov.nasa.worldwind.render.TiledSurfaceObjectRenderer}.
 *
 * @author dcollins
 * @version $Id$
 * @see gov.nasa.worldwind.layers.SurfaceShapeLayer
 */
public interface SurfaceShape extends SurfaceObject, PreRenderable, Renderable, Restorable, ExtentHolder,
    MeasurableArea, MeasurableLength
{
    /**
     * Returns a copy of the rendering attributes associated with this SurfaceShape. Modifying the contents of the
     * returned reference has no effect on this shape. In order to make an attribute change take effect, invoke {@link
     * #setAttributes(ShapeAttributes)} with the modified attributes.
     *
     * @return a copy of this shape's rendering attributes.
     */
    ShapeAttributes getAttributes();

    /**
     * Sets the rendering attributes associated with this SurfaceShape. The caller cannot assume that modifying the
     * attribute reference after calling setAttributes() will have any effect, as the implementation may defensively
     * copy the attribute reference. In order to make an attribute change take effect, invoke
     * setAttributes(ShapeAttributes) again with the modified attributes.
     *
     * @param attributes this shapes new rendering attributes.
     *
     * @throws IllegalArgumentException if <code>attributes</code> is null.
     */
    void setAttributes(ShapeAttributes attributes);

    /**
     * Returns the path type used to interpolate between locations on this SurfaceShape.
     *
     * @return path interpolation type.
     */
    String getPathType();

    /**
     * Sets the path type used to interpolate between locations on this SurfaceShape. This should be one of <ul>
     * <li>gov.nasa.worldwind.avlist.AVKey.GREAT_CIRCLE</li> <li>gov.nasa.worldwind.avlist.AVKey.LINEAR</li>
     * <li>gov.nasa.worldwind.avlist.AVKey.LOXODROME</li> <li>gov.nasa.worldwind.avlist.AVKey.RHUMB</li> </ul>
     *
     * @param pathType path interpolation type.
     *
     * @throws IllegalArgumentException if <code>pathType</code> is null.
     */
    void setPathType(String pathType);

    /**
     * Returns the number of texels per shape edge interval.
     *
     * @return texels per shape edge interval.
     *
     * @see #setTexelsPerEdgeInterval(double)
     */
    double getTexelsPerEdgeInterval();

    /**
     * Sets the number of texels per shape edge interval. This value controls how many interpolated intervals will be
     * added to each shape edge, depending on size of the original edge in texels. Each shape is responsible for
     * defining what an edge is, though for most shapes it will simply be defined as the edge between implicit or
     * caller-specified shape locations. The number of interpolated intervals is limited by the values set in a call to
     * {@link #setMinAndMaxEdgeIntervals(int, int)}.
     *
     * @param texelsPerEdgeInterval the size, in texels, of each interpolated edge interval.
     *
     * @throws IllegalArgumentException if <code>texelsPerEdgeInterval</code> is less than or equal to zero.
     * @see #setMinAndMaxEdgeIntervals(int, int)
     */
    void setTexelsPerEdgeInterval(double texelsPerEdgeInterval);

    /**
     * Returns the minimum and maximum number of interpolated intervals that may be added to each shape edge.
     *
     * @return array of two elements, the first element is minEdgeIntervals, the second element is maxEdgeIntervals.
     *
     * @see #setMinAndMaxEdgeIntervals(int, int)
     */
    int[] getMinAndMaxEdgeIntervals();

    /**
     * Sets the minimum and maximum number of interpolated intervals that may be added to each shape edge. The minimum
     * and maximum values may be 0, or any positive integer. Note that Setting either of <code>minEdgeIntervals</code>
     * or <code>maxEdgeIntervals</code> too large may adversely impact surface shape rendering performance.
     *
     * @param minEdgeIntervals the minimum number of interpolated edge intervals.
     * @param maxEdgeIntervals the maximum number of interpolated edge intervals.
     *
     * @throws IllegalArgumentException if either of <code>minEdgeIntervals</code> or <code>maxEdgeIntervals</code> is
     *                                  less than or equal to zero.
     * @see #setTexelsPerEdgeInterval(double)
     */
    void setMinAndMaxEdgeIntervals(int minEdgeIntervals, int maxEdgeIntervals);

    /**
     * Returns the shape's locations as they appear on the specified <code>globe</code>.
     *
     * @param globe the globe the shape is related to.
     *
     * @return the shapes locations on the globe.
     *
     * @throws IllegalArgumentException if <code>globe</code> is null.
     */
    Iterable<? extends LatLon> getLocations(Globe globe);

    /**
     * Returns the shapes's area in square meters. If <code>terrainConformant</code> is true, the area returned is the
     * surface area of the terrain, including its hillsides and other undulations.
     *
     * @param globe             the globe the shape is related to.
     * @param terrainConformant whether or not the returned area should treat the shape as conforming to the terrain.
     *
     * @return the shape's area in square meters. Returns -1 if the object does not form an area due to an insufficient
     *         number of vertices or any other condition.
     *
     * @throws IllegalArgumentException if <code>globe</code> is null.
     */
    double getArea(Globe globe, boolean terrainConformant);
}
