/*
Copyright (C) 2001, 2008 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/

package gov.nasa.worldwind.render;

import com.sun.opengl.util.texture.*;
import gov.nasa.worldwind.pick.*;
import gov.nasa.worldwind.util.*;

import javax.media.opengl.*;
import java.awt.*;

/**
 * Draws an image parallel to the screen at a specified screen location relative to the World Window.
 *
 * @author tag
 * @version $Id: ScreenImage.java 13058 2010-01-27 22:58:05Z tgaskins $
 */
public class ScreenImage implements Renderable
{
    protected BasicWWTexture texture;
    private OrderedImage orderedImage = new OrderedImage();
    private PickSupport pickSupport = new PickSupport();
    private Point screenLocation;
    private double opacity = 1d;

    private class OrderedImage implements OrderedRenderable
    {
        public double getDistanceFromEye()
        {
            return 0;
        }

        public void pick(DrawContext dc, Point pickPoint)
        {
            ScreenImage.this.draw(dc);
        }

        public void render(DrawContext dc)
        {
            ScreenImage.this.draw(dc);
        }
    }

    /**
     * Returns the location of the image on the screen. The position is relative to the upper-left corner of the World
     * Window. The image is centered on this position.
     *
     * @return the current screen position.
     */
    public Point getScreenLocation()
    {
        return this.screenLocation;
    }

    /**
     * Specifies the location of the image on the screen. The position is relative to the upper-left corner of the World
     * Window. The image is centered on this position.
     *
     * @param screenLocation the screen location on which to center the image. May be null, in which case the image is
     *                       not displayed.
     */
    public void setScreenLocation(Point screenLocation)
    {
        this.screenLocation = screenLocation;
    }

    /**
     * Returns the current image source.
     *
     * @return the current image source.
     *
     * @see #getImageSource()
     */
    public Object getImageSource()
    {
        return this.getTexture() != null ? this.getTexture().getImageSource() : null;
    }

    /**
     * Specifies the image source, which may be either a file path {@link String}or a {@link
     * java.awt.image.BufferedImage}.
     *
     * @param imageSource the image source, either a file path {@link String}or a {@link java.awt.image.BufferedImage}.
     *
     * @throws IllegalArgumentException if the <code>imageSource</code> is null.
     */
    public void setImageSource(Object imageSource)
    {
        if (imageSource == null)
        {
            String message = Logging.getMessage("nullValue.ImageSource");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.texture = new BasicWWTexture(imageSource, true);
        this.texture.setUseAnisotropy(false);
    }

    /**
     * Returns the opacity of the surface. A value of 1 or greater means the surface is fully opaque, a value of 0 means
     * that the surface is fully transparent.
     *
     * @return the surface opacity.
     */
    public double getOpacity()
    {
        return opacity;
    }

    /**
     * Sets the opacity of the surface. A value of 1 or greater means the surface is fully opaque, a value of 0 means
     * that the surface is fully transparent.
     *
     * @param opacity a positive value indicating the opacity of the surface.
     *
     * @throws IllegalArgumentException if the specified opacity is less than zero.
     */
    public void setOpacity(double opacity)
    {
        if (opacity < 0)
        {
            String message = Logging.getMessage("generic.OpacityOutOfRange", opacity);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.opacity = opacity;
    }

    /**
     * Returns the width of the source image, or 0 if no source image has been specified.
     *
     * @param dc the current draw context.
     *
     * @return the source image width.
     */
    public int getImageWidth(DrawContext dc)
    {
        return this.getTexture() != null ? this.getTexture().getWidth(dc) : 0;
    }

    /**
     * Returns the height of the source image, or null if no source image has been specified.
     *
     * @param dc the current draw context.
     *
     * @return the source image height.
     */
    public int getImageHeight(DrawContext dc)
    {
        return this.getTexture() != null ? this.getTexture().getHeight(dc) : 0;
    }

    protected BasicWWTexture getTexture()
    {
        return this.texture;
    }

    public void render(DrawContext dc)
    {
        this.doRender(dc);
    }

    public void pick(DrawContext dc, Point pickPoint)
    {
        this.doRender(dc);
    }

    protected void doRender(DrawContext dc)
    {
        dc.addOrderedRenderable(this.orderedImage);
    }

    private void draw(DrawContext dc)
    {
        if (this.screenLocation == null || this.getImageSource() == null)
            return;

        GL gl = dc.getGL();

        boolean attribsPushed = false;
        boolean modelviewPushed = false;
        boolean projectionPushed = false;

        try
        {
            gl.glPushAttrib(GL.GL_DEPTH_BUFFER_BIT
                | GL.GL_COLOR_BUFFER_BIT
                | GL.GL_ENABLE_BIT
                | GL.GL_TEXTURE_BIT
                | GL.GL_TRANSFORM_BIT
                | GL.GL_VIEWPORT_BIT
                | GL.GL_CURRENT_BIT);
            attribsPushed = true;

            // Don't depth buffer.
            gl.glDisable(GL.GL_DEPTH_TEST);

            // Suppress any fully transparent image pixels
            gl.glEnable(GL.GL_ALPHA_TEST);
            gl.glAlphaFunc(GL.GL_GREATER, 0.001f);

            java.awt.Rectangle viewport = dc.getView().getViewport();
            gl.glMatrixMode(javax.media.opengl.GL.GL_PROJECTION);
            gl.glPushMatrix();
            projectionPushed = true;
            gl.glLoadIdentity();
            gl.glOrtho(0d, viewport.width, 0d, viewport.height, -1, 1);

            gl.glMatrixMode(GL.GL_MODELVIEW);
            gl.glPushMatrix();
            modelviewPushed = true;
            gl.glLoadIdentity();
            gl.glTranslated(this.screenLocation.x - this.getImageWidth(dc) / 2,
                viewport.height - (this.screenLocation.y + this.getImageHeight(dc) / 2), 0d);

            if (!dc.isPickingMode())
            {
                if (this.getTexture() != null)
                {
                    gl.glEnable(GL.GL_TEXTURE_2D);
                    if (this.getTexture().bind(dc))
                    {

                        gl.glColor4d(1d, 1d, 1d, this.opacity);
                        gl.glEnable(GL.GL_BLEND);
                        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
                        TextureCoords texCoords = this.getTexture().getTexCoords();
                        gl.glScaled(this.getImageWidth(dc), this.getImageHeight(dc), 1d);
                        dc.drawUnitQuad(texCoords);
                    }
                }
            }
            else
            {
                this.pickSupport.clearPickList();
                this.pickSupport.beginPicking(dc);
                Color color = dc.getUniquePickColor();
                int colorCode = color.getRGB();
                this.pickSupport.addPickableObject(colorCode, this, null, false);
                gl.glColor3ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue());
                gl.glScaled(this.getImageWidth(dc), this.getImageHeight(dc), 1d);
                dc.drawUnitQuad();
                this.pickSupport.endPicking(dc);
                this.pickSupport.resolvePick(dc, dc.getPickPoint(), dc.getCurrentLayer());
            }
        }
        finally
        {
            if (projectionPushed)
            {
                gl.glMatrixMode(GL.GL_PROJECTION);
                gl.glPopMatrix();
            }
            if (modelviewPushed)
            {
                gl.glMatrixMode(GL.GL_MODELVIEW);
                gl.glPopMatrix();
            }
            if (attribsPushed)
                gl.glPopAttrib();
        }
    }
}
