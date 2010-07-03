/* Copyright (C) 2001, 2009 United States Government as represented by 
the Administrator of the National Aeronautics and Space Administration. 
All Rights Reserved. 
*/
package gov.nasa.worldwind.util;

import gov.nasa.worldwind.geom.Vec4;

import javax.media.opengl.GL;

/**
 * A collection of OpenGL utility methods, all static.
 *
 * @author dcollins
 * @version $Id: OGLUtil.java 13147 2010-02-18 23:52:15Z dcollins $
 */
public class OGLUtil
{
    protected static final String GL_EXT_BLEND_FUNC_SEPARATE = "GL_EXT_blend_func_separate";

    protected static final Vec4 DEFAULT_LIGHT_DIRECTION = new Vec4(0, 0, -1, 0);

    /**
     * Sets the GL blending state according to the specified color mode. If <code>havePremultipliedColors</code> is
     * true, this applies a blending function appropriate for colors premultiplied by the alpha component. Otherwise,
     * this applies a blending function appropriate for non-premultiplied colors.
     *
     * @param gl                      the GL context.
     * @param havePremultipliedColors true to configure blending for colors premultiplied by the alpha components, and
     *                                false to configure blending for non-premultiplied colors.
     *
     * @throws IllegalArgumentException if the GL is null.
     */
    public static void applyBlending(GL gl, boolean havePremultipliedColors)
    {
        if (gl == null)
        {
            String message = Logging.getMessage("nullValue.GLIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (havePremultipliedColors)
        {
            gl.glBlendFunc(GL.GL_ONE, GL.GL_ONE_MINUS_SRC_ALPHA);
        }
        else
        {
            // The separate blend function correctly handles regular (non-premultiplied) colors. We want
            //     Cd = Cs*As + Cf*(1-As)
            //     Ad = As    + Af*(1-As)
            // So we use GL_EXT_blend_func_separate to specify different blending factors for source color and source
            // alpha.

            boolean haveExtBlendFuncSeparate = gl.isExtensionAvailable(GL_EXT_BLEND_FUNC_SEPARATE);
            if (haveExtBlendFuncSeparate)
            {
                gl.glBlendFuncSeparate(
                    GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA, // rgb   blending factors
                    GL.GL_ONE, GL.GL_ONE_MINUS_SRC_ALPHA);      // alpha blending factors
            }
            else
            {
                gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
            }
        }
    }

    /**
     * Sets the GL color state to the specified {@link java.awt.Color} and opacity, and with the specified color mode.
     * If <code>premultiplyColors</code> is true, this premultipies the Red, Green, and Blue color values by the opacity
     * value. Otherwise, this does not modify the Red, Green, and Blue color values.
     *
     * @param gl                the GL context.
     * @param color             the Red, Green, and Blue values to set.
     * @param opacity           the opacity to set.
     * @param premultiplyColors true to premultiply the Red, Green, and Blue color values by the opacity value, false to
     *                          leave the Red, Green, and Blue values unmodified.
     *
     * @throws IllegalArgumentException if the GL is null, if the Color is null, if the opacity is less than 0, or if
     *                                  the opacity is greater than 1.
     */
    public static void applyColor(GL gl, java.awt.Color color, double opacity, boolean premultiplyColors)
    {
        if (gl == null)
        {
            String message = Logging.getMessage("nullValue.GLIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (color == null)
        {
            String message = Logging.getMessage("nullValue.ColorIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (opacity < 0d || opacity > 1d)
        {
            String message = Logging.getMessage("generic.OpacityOutOfRange", opacity);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        float[] compArray = new float[4];
        color.getRGBComponents(compArray);
        compArray[3] = (float) opacity;

        if (premultiplyColors)
        {
            compArray[0] *= compArray[3];
            compArray[1] *= compArray[3];
            compArray[2] *= compArray[3];
        }

        gl.glColor4fv(compArray, 0);
    }

    /**
     * Sets the GL color state to the specified {@link java.awt.Color}, and with the specified color mode. If
     * <code>premultiplyColors</code> is true, this premultipies the Red, Green, and Blue color values by the Alpha
     * value. Otherwise, this does not modify the Red, Green, and Blue color values.
     *
     * @param gl                the GL context.
     * @param color             the Red, Green, Blue, and Alpha values to set.
     * @param premultiplyColors true to premultiply the Red, Green and Blue color values by the Alpha value, false to
     *                          leave the Red, Green, and Blue values unmodified.
     *
     * @throws IllegalArgumentException if the GL is null, if the Color is null, if the opacity is less than 0, or if
     *                                  the opacity is greater than 1.
     */
    public static void applyColor(GL gl, java.awt.Color color, boolean premultiplyColors)
    {
        if (gl == null)
        {
            String message = Logging.getMessage("nullValue.GLIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (color == null)
        {
            String message = Logging.getMessage("nullValue.ColorIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        float[] compArray = new float[4];
        color.getRGBComponents(compArray);

        if (premultiplyColors)
        {
            compArray[0] *= compArray[3];
            compArray[1] *= compArray[3];
            compArray[2] *= compArray[3];
        }

        gl.glColor4fv(compArray, 0);
    }

    /**
     * Sets the GL lighting state to a white light originating from the eye position and pointed in the specified
     * direction, in model coordinates. The light direction is always relative to the current eye point and viewer
     * direction. If the direction is null, this the light direction defaults to (0, 0, -1), which points directly along
     * the forward vector form the eye point
     *
     * @param gl        the GL context.
     * @param light     the GL light name to set.
     * @param direction the light direction in model coordinates, may be null.
     *
     * @throws IllegalArgumentException if the GL is null.
     */
    public static void applyLightingDirectionalFromViewer(GL gl, int light, Vec4 direction)
    {
        if (gl == null)
        {
            String message = Logging.getMessage("nullValue.GLIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (direction == null)
            direction = DEFAULT_LIGHT_DIRECTION;

        float[] ambient = {1f, 1f, 1f, 0f};
        float[] diffuse = {1f, 1f, 1f, 0f};
        float[] specular = {1f, 1f, 1f, 0f};
        float[] position = {(float) direction.x, (float) direction.y, (float) direction.z, 0.0f};

        gl.glLightfv(light, GL.GL_AMBIENT, ambient, 0);
        gl.glLightfv(light, GL.GL_DIFFUSE, diffuse, 0);
        gl.glLightfv(light, GL.GL_SPECULAR, specular, 0);

        OGLStackHandler ogsh = new OGLStackHandler();
        ogsh.pushModelviewIdentity(gl);
        try
        {
            gl.glLightfv(light, GL.GL_POSITION, position, 0);
        }
        finally
        {
            ogsh.pop(gl);
        }
    }
}
