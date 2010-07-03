/*
Copyright (C) 2001, 2009 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render;

import com.sun.opengl.util.j2d.TextRenderer;
import gov.nasa.worldwind.Movable;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.util.*;

import javax.media.opengl.GL;
import java.awt.*;
import java.awt.geom.*;
import java.util.Arrays;

/**
 * Surface text.
 *
 * @author Patrick Murris
 * @version $Id: SurfaceText.java 13234 2010-03-20 02:26:06Z tgaskins $
 */
public class SurfaceText extends AbstractSurfaceRenderable implements Movable  // TODO: implement GeographicText?
{
    private String text;
    private LatLon location;
    private Font font = Font.decode("Arial-BOLD-24");
    private Color color = Color.WHITE;

    protected TextRenderer textRenderer;
    protected Rectangle2D textBounds;

    // TODO: add min and max text dimension in meter
    // TODO: add location offset
    // TODO: handle opacity
    // TODO: add outline effect

    public SurfaceText(String text, LatLon location, Font font, Color color)
    {
        this.setText(text);
        this.setLocation(location);
        this.setFont(font);
        this.setColor(color);
    }

    public String getText()
    {
        return this.text;
    }

    public void setText(String text)
    {
        if (text == null)
        {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.text = text;
        this.updateTextBounds();
    }

    public LatLon getLocation()
    {
        return this.location;
    }

    public void setLocation(LatLon location)
    {
        if (location == null)
        {
            String message = Logging.getMessage("nullValue.LatLonIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.location = location;
    }

    public Font getFont()
    {
        return this.font;
    }

    public void setFont(Font font)
    {
        if (font == null)
        {
            String message = Logging.getMessage("nullValue.FontIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.font = font;
        if (this.textRenderer != null)
            this.textRenderer.dispose();
        this.textRenderer = null;
    }

    public Color getColor()
    {
        return this.color;
    }

    public void setColor(Color color)
    {
        if (color == null)
        {
            String message = Logging.getMessage("nullValue.ColorIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.color = color;
    }

    //*** SurfaceObject

    public long getLastModifiedTime()
    {
        return System.currentTimeMillis();  // Refresh all the time
    }

    public Iterable<? extends Sector> getSectors(DrawContext dc, double texelSizeRadians)
    {
        return Arrays.asList(computeSector(dc));
    }

    public void doRenderToRegion(DrawContext dc, Sector sector, int x, int y, int width, int height)
    {
        GL gl = dc.getGL();
        OGLStackHandler ogsh = new OGLStackHandler();
        ogsh.pushAttrib(gl,
            GL.GL_CURRENT_BIT       // For current color (used by JOGL TextRenderer).
                | GL.GL_TRANSFORM_BIT); // For matrix mode.
        ogsh.pushModelview(gl);
        try
        {
            this.applyDrawTransform(dc, sector, x, y, width, height);

            // Draw text
            this.drawText(this.text, Vec4.ZERO);
        }
        finally
        {
            // Restore gl state
            ogsh.pop(gl);
        }
    }

    protected Sector computeSector(DrawContext dc)
    {
        // Compute text extent depending on distance from eye
        Globe globe = dc.getGlobe();
        double pixelSize = computePixelSizeAtLocation(dc, this.location);
        double dLatRadians = this.textBounds.getHeight() * pixelSize / globe.getRadius();
        double dLonRadians = this.textBounds.getWidth() * pixelSize / globe.getRadius() / location.getLatitude().cos();
        Sector sector = new Sector(
            location.getLatitude(),
            location.getLatitude().addRadians(dLatRadians),
            location.getLongitude().subtractRadians(dLonRadians / 2),
            location.getLongitude().addRadians(dLonRadians / 2)
        );

        // Rotate sector around location
        return computeRotatedSectorBounds(sector, this.location, computeDrawHeading(dc));
    }

    protected Angle computeDrawHeading(DrawContext dc)
    {
        return getViewHeading(dc);
    }

    protected TextRenderer getTextRenderer()
    {
        if (this.textRenderer == null)
            this.textRenderer = new TextRenderer(this.font, true, true);

        return this.textRenderer;
    }

    // TODO: use text renderer cache from dc
//    protected TextRenderer getTextRenderer(DrawContext dc, Font font)
//    {
//        TextRenderer tr = dc.getTextRendererCache().get(font);
//        if (tr == null)
//        {
//            tr = new TextRenderer(font, true, true);
//            tr.setUseVertexArrays(false);
//            dc.getTextRendererCache().add(font, tr);
//        }
//        return tr;
//    }

    protected void updateTextBounds()
    {
        this.textBounds = getTextRenderer().getBounds(this.text);
    }

    protected void applyDrawTransform(DrawContext dc, Sector sector, int x, int y, int width, int height)
    {
        // Compute text viewport point
        Vec4 point = computeDrawPoint(this.location, sector, x, y, width, height);

        // Compute scaling depending on eye distance
        double regionPixelSize = computeDrawPixelSize(dc, sector, width, height);
        double viewPixelSize = computePixelSizeAtLocation(dc, this.location);
        double drawScale = viewPixelSize / regionPixelSize;

        GL gl = dc.getGL();
        gl.glTranslated(point.x(), point.y(), point.z());

        // Add x scaling transform to maintain text width and aspect ratio at any latitude
        gl.glScaled(drawScale / this.location.getLatitude().cos(), drawScale, 1);

        // Add rotation to maintain text facing the eye
        gl.glRotated(computeDrawHeading(dc).degrees, 0, 0, -1);
    }

    protected void drawText(String text, Vec4 screenPoint)
    {
        TextRenderer tr = getTextRenderer();
        int x = (int) (screenPoint.x() - this.textBounds.getWidth() / 2d);
        int y = (int) screenPoint.y();

        tr.begin3DRendering();

        tr.setColor(this.computeBackgroundColor(this.color));
        tr.draw(text, x + 1, y - 1);
        tr.setColor(this.color);
        tr.draw(text, x, y);

        tr.end3DRendering();
    }

    private final float[] compArray = new float[4];

    // Compute background color for best contrast
    protected Color computeBackgroundColor(Color color)
    {
        Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), compArray);
        if (compArray[2] > 0.5)
            return new Color(0, 0, 0, 0.7f);
        else
            return new Color(1, 1, 1, 0.7f);
    }

    // *** Movable interface

    public Position getReferencePosition()
    {
        return new Position(this.location, 0);
    }

    public void move(Position position)
    {
        moveTo(getReferencePosition().add(position));
    }

    public void moveTo(Position position)
    {
        this.setLocation(position);
    }
}
