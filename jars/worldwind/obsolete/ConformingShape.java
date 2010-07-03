/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render;

import gov.nasa.worldwind.Disposable;
import gov.nasa.worldwind.Movable;
import gov.nasa.worldwind.Restorable;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.cache.BasicMemoryCache;
import gov.nasa.worldwind.cache.MemoryCache;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.terrain.SectorGeometry;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.RestorableSupport;

import javax.media.opengl.GL;
import java.awt.*;
import java.util.ArrayList;

/**
 * The design of the class hierarchy for which this class is the base was derived in part from
 * that of the class hierarchy rooted by SurfaceShape. The major implementation difference is that
 * instances of this class extract portions of the current tessellation to render the interior of
 * shapes. There is somewhat of a performance penalty for this.
 * <p>For the most part, any SurfaceXxx class can be replaced by the corresponding ConformingXxx
 * class, and all other usage (method names and prototypes) will be the same. The only
 * exceptions are:
 * <ol><li>class <code>ConformingShape</code> is abstract. Any direct construction of a SurfaceShape object
 * can be replaced by an instance of ConformingPolygon using the same actual parameters</li>
 * <li>{@link SurfaceEllipse} is replaced by {@link ConformingEllipticalPolygon} and {@link ConformingCircularPolygon}
 * since they are actually implemented by generating polygons. The constructor (and other methods) for the corresponding
 * classes are identical.</li>
 * <li>"New" classes {@link ConformingEllipse} and {@link ConformingCircle} are used to create shapes
 * that always have an elliptical or circular shape. These shapes are created <i>without</i> an
 * "int intervals" parameter and are always rendered as ellipses or circles mapped onto the terrain.</li>
 * </ol>
 * @author Jim Miller
 * @version $Id: ConformingShape.java 7671 2008-12-08 00:18:14Z jmiller $
 */

public abstract class ConformingShape
    implements Renderable, Disposable, Movable, Restorable, MeasurableArea, MeasurableLength
{
    protected static class CacheKey
    {
        private final Class cls;
        private final Sector[] bounds;
        private final int serialNumber;
        private int hash = 0;

        public CacheKey(Class conformingShapeClass, Sector[] bounds, int serialNumber)
        {
            this.cls = conformingShapeClass;
            this.bounds = bounds;
            this.serialNumber = serialNumber;
        }

        public boolean equals(Object o)
        {
            if (this == o)
                return true;
            if (o == null || this.getClass() != o.getClass())
                return false;

            CacheKey cacheKey = (CacheKey) o;

            return (this.serialNumber == cacheKey.serialNumber);
        }

        public int hashCode()
        {
            if (this.hash == 0)
            {
                this.hash = (this.cls != null ? cls.hashCode() : 0);
                if (this.bounds == null)
                    this.hash = 31 * this.hash;
                else
                    for (Sector s : this.bounds)
                        this.hash = 31 * this.hash + (s != null ? s.hashCode() : 0);
                this.hash = 31 * this.hash + 131 * this.serialNumber;
            }
            return this.hash;
        }
    } // end class CacheKey

    // Attributes
    protected Color fillColor;
    protected Color borderColor;
    protected double borderWidth = 1d;
    protected boolean drawBorder = true;
    protected boolean drawInterior = true;
    private boolean antiAlias = true;

    private boolean useFrustumCull;
    private boolean combatZBufferFightingWithModelingXform = false;
    private boolean combatZBufferFightingWithProjectionXform = true;

    private static final Color DEFAULT_FILL_COLOR = new Color(1f, 1f, 0f, 0.4f);
    private static final Color DEFAULT_BORDER_COLOR = new Color(1f, 1f, 0f, 0.7f);

    // Cache-related information
    protected static final String CONFORMINGSHAPE_CACHE_NAME = "Conforming Shape Cache";
    protected static final String CONFORMINGSHAPE_CACHE_KEY = ConformingShape.class.getName();
    private static final long DEFAULT_CONFORMINGSHAPE_CACHE_SIZE = 16777216L; // TODO: determine reasonable default cache size
    private static int startCountDown = 1;
    private int countDown = 0;
    private static int nextSerialNumber = 1001;

    public ConformingShape()
    {
        this(null,null);
    }

    public ConformingShape(Color fillColor, Color borderColor)
    {
        // Set draw attributes
        this.fillColor = fillColor != null ? fillColor : DEFAULT_FILL_COLOR;
        this.borderColor = borderColor != null ? borderColor : DEFAULT_BORDER_COLOR;

        this.useFrustumCull = false;

        if (!WorldWind.getMemoryCacheSet().containsCache(CONFORMINGSHAPE_CACHE_KEY))
        {
            long size = DEFAULT_CONFORMINGSHAPE_CACHE_SIZE; //Configuration.getLongValue(AVKey.CONFORMINGSHAPE_CACHE_SIZE, DEFAULT_CONFORMINGSHAPE_CACHE_SIZE);
            MemoryCache cache = new BasicMemoryCache((long) (0.85 * size), size);
            cache.setName(CONFORMINGSHAPE_CACHE_NAME);
            WorldWind.getMemoryCacheSet().addCache(CONFORMINGSHAPE_CACHE_KEY, cache);
        }
    }

    public void dispose()
    {
    }

    public Paint getInteriorColor()
    {
        return fillColor;
    }

    public void setInteriorColor(Color interiorColor)
    {
        invalidateCache();
        this.fillColor = interiorColor;
    }

    public Color getBorderColor()
    {
        return borderColor;
    }

    public void setBorderColor(Color borderColor)
    {
        invalidateCache();
        this.borderColor = borderColor;
    }

    public double getBorderWidth()
    {
        return borderWidth;
    }

    public void setBorderWidth(double borderWidth)
    {
        invalidateCache();
        this.borderWidth = borderWidth;
    }

    public boolean isDrawBorder()
    {
        return drawBorder;
    }

    public void setDrawBorder(boolean drawBorder)
    {
        invalidateCache();
        this.drawBorder = drawBorder;
    }

    public boolean isDrawInterior()
    {
        return drawInterior;
    }

    public void setDrawInterior(boolean drawInterior)
    {
        invalidateCache();
        this.drawInterior = drawInterior;
    }

    public boolean isAntiAlias()
    {
        return antiAlias;
    }

    public void setAntiAlias(boolean antiAlias)
    {
        this.antiAlias = antiAlias;
    }

    public abstract Position getReferencePosition();

    protected static int getUniqueSerialNumber()
    {
        return ConformingShape.nextSerialNumber++;
    }

    public boolean getUseFrustumCull() { return useFrustumCull; }
    public void setUseFrustumCull(boolean c) { useFrustumCull = c; }

    public void move(Position delta)
    {
        if (delta == null)
        {
            String msg = Logging.getMessage("nullValue.PositionIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        this.moveTo(this.getReferencePosition().add(delta));
    }

    public abstract void moveTo(Position position);

    protected static long sizeInBytesOf(ArrayList<SectorGeometry.ExtractedShapeDescription> esdL)
    {
        int nv = 0, nOutlines = 0;
        for (SectorGeometry.ExtractedShapeDescription esd : esdL)
        {
            ArrayList<Vec4[]> df = esd.interiorPolys;
            for (Vec4[] verts : df)
                nv += verts.length;

            if (esd.shapeOutline != null)
                nOutlines += esd.shapeOutline.size();
        }
        // each Vec4 has 4 doubles; each BoundaryEdge is 48 bytes
        return 32*nv + nOutlines*48;
    }

    protected abstract void invalidateCache();

    protected boolean isExpired(DrawContext dc)
    {
        return (this.countDown == 0);
    }

    protected void updateExpiryCriteria(DrawContext dc)
    {
        if (this.countDown == 0)
        {
            this.countDown = ConformingShape.startCountDown;
            if (--ConformingShape.startCountDown < 1)
                ConformingShape.startCountDown = 30; // each frame has a pick and a render pass
        }
        else
            this.countDown--;
    }

    protected abstract boolean renderInterior(DrawContext dc, GL gl);
    protected abstract void renderBoundary(DrawContext dc, GL gl, boolean knownToBeVisible);

    public void render(DrawContext dc)
    {
        GL gl = dc.getGL();

        // set attributes for this shape....
        gl.glPushAttrib(GL.GL_COLOR_BUFFER_BIT | GL.GL_CURRENT_BIT | GL.GL_LINE_BIT | GL.GL_POLYGON_BIT);
        gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_FILL);
        if (!dc.isPickingMode())
        {
            gl.glBlendFunc(GL.GL_SRC_ALPHA,GL.GL_ONE_MINUS_SRC_ALPHA);
            gl.glEnable(GL.GL_BLEND);
        }
        gl.glLineWidth((float)borderWidth);

        if (combatZBufferFightingWithModelingXform)
        {
            gl.glPushMatrix();
            double scale = 0.9;
            gl.glScaled(scale,scale,scale);
        }

        if (combatZBufferFightingWithProjectionXform)
        {
            // taken from Polyline.java:
            float[] pm = new float[16];
            gl.glGetFloatv(GL.GL_PROJECTION_MATRIX, pm, 0);
            pm[10] *= 0.99; // TODO: See Lengyel 2 ed. Section 9.1.2 to compute optimal/minimal offset

            gl.glPushAttrib(GL.GL_TRANSFORM_BIT);
            gl.glMatrixMode(GL.GL_PROJECTION);
            gl.glPushMatrix();
            gl.glLoadMatrixf(pm, 0);
        }

        boolean knownVisible = renderInterior(dc,gl);
        renderBoundary(dc,gl,knownVisible);

        if (combatZBufferFightingWithModelingXform)
            gl.glPopMatrix();
        if (combatZBufferFightingWithProjectionXform)
        {
            // taken from Polyline.java:
            gl.glMatrixMode(GL.GL_PROJECTION);
            gl.glPopMatrix();
            gl.glPopAttrib();
        }
        gl.glPopAttrib(); // for glPushAttrib at start of this method.
    }

    public String getRestorableState()
    {
        RestorableSupport rs = RestorableSupport.newRestorableSupport();
        this.doGetRestorableState(rs, null);

        return rs.getStateAsXml();
    }

    protected void doGetRestorableState(RestorableSupport rs, RestorableSupport.StateObject context)
    {
        if (this.getInteriorColor() instanceof Color) // Only color persisted
            rs.addStateValueAsColor(context, "color", (Color) this.getInteriorColor());
        rs.addStateValueAsDouble(context, "lineWidth", this.getBorderWidth());
        rs.addStateValueAsColor(context, "borderColor", this.getBorderColor());
        rs.addStateValueAsBoolean(context, "drawInterior", this.isDrawInterior());
        rs.addStateValueAsBoolean(context, "drawBorder", this.isDrawBorder());
        rs.addStateValueAsBoolean(context, "antialias", this.isAntiAlias());
    }

    public void restoreState(String stateInXml)
    {
        if (stateInXml == null)
        {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        RestorableSupport rs;
        try
        {
            rs = RestorableSupport.parse(stateInXml);
        }
        catch (Exception e)
        {
            // Parsing the document specified by stateInXml failed.
            String message = Logging.getMessage("generic.ExceptionAttemptingToParseStateXml", stateInXml);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message, e);
        }

        this.doRestoreState(rs, null);
    }

    protected void doRestoreState(RestorableSupport rs, RestorableSupport.StateObject context)
    {
        Color color = rs.getStateValueAsColor(context, "color");
        if (color != null)
            this.setInteriorColor(color);

        color = rs.getStateValueAsColor(context, "borderColor");
        if (color != null)
            this.setBorderColor(color);

        Double dub = rs.getStateValueAsDouble(context, "lineWidth");
        if (dub != null)
            this.setBorderWidth(dub);

        Boolean booleanState = rs.getStateValueAsBoolean(context, "drawBorder");
        if (booleanState != null)
            this.setDrawBorder(booleanState);

        booleanState = rs.getStateValueAsBoolean(context, "drawInterior");
        if (booleanState != null)
            this.setDrawInterior(booleanState);

        booleanState = rs.getStateValueAsBoolean(context, "antialias");
        if (booleanState != null)
            this.setAntiAlias(booleanState);
    }
}
