/*
Copyright (C) 2001, 2009 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/

package gov.nasa.worldwind.render;

import com.sun.opengl.util.texture.*;
import gov.nasa.worldwind.util.*;

import javax.media.opengl.*;
import java.awt.image.*;
import java.io.InputStream;

/**
 * This class holds information for a texture and provides utilities for binding and initializing textures.
 *
 * @author tag
 * @version $Id: BasicWWTexture.java 13201 2010-03-12 01:59:03Z tgaskins $
 */
public class BasicWWTexture implements WWTexture
{
    private Object imageSource;
    private boolean useMipMaps;
    private boolean useAnisotropy = true;

    protected int width;
    protected int height;
    protected TextureCoords texCoords;
    protected boolean textureInitialized = false;
    protected boolean textureInitializationFailed = false;

    /**
     * Constructs a texture object from an image source.
     * <p/>
     * The texture's image source is opened, if a file, only when the texture is displayed. If the texture is not
     * displayed the image source is not read.
     *
     * @param imageSource the source of the image, either a file path {@link String} or a {@link BufferedImage}.
     * @param useMipMaps  Indicates whether to generate and use mipmaps for the image.
     *
     * @throws IllegalArgumentException if the <code>imageSource</code> is null.
     */
    public BasicWWTexture(Object imageSource, boolean useMipMaps)
    {
        initialize(imageSource, useMipMaps);
    }

    /**
     * Constructs a texture object.
     * <p/>
     * The texture's image source is opened, if a file, only when the texture is displayed. If the texture is not
     * displayed the image source is not read.
     *
     * @param imageSource the source of the image, either a file path {@link String} or a {@link BufferedImage}.
     *
     * @throws IllegalArgumentException if the <code>imageSource</code> is null.
     */
    public BasicWWTexture(Object imageSource)
    {
        this(imageSource, false);
    }

    protected void initialize(Object imageSource, boolean useMipMaps)
    {
        if (imageSource == null)
        {
            String message = Logging.getMessage("nullValue.ImageSource");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.imageSource = imageSource;
        this.useMipMaps = useMipMaps;
    }

    public Object getImageSource()
    {
        return imageSource;
    }

    public int getWidth(DrawContext dc)
    {
        if (!this.textureInitialized)
            this.initializeTexture(dc, this.getImageSource());

        return width;
    }

    public int getHeight(DrawContext dc)
    {
        if (!this.textureInitialized)
            this.initializeTexture(dc, this.getImageSource());

        return height;
    }

    /**
     * Indicates whether the texture creates and uses mipmaps.
     *
     * @return true if mipmaps are used, false if  not.
     */
    public boolean isUseMipMaps()
    {
        return useMipMaps;
    }

    public TextureCoords getTexCoords()
    {
        return texCoords;
    }

    public boolean isTextureCurrent(DrawContext dc)
    {
        return true;
    }

    /**
     * Indicates whether texture anisotropy is applied to the texture when rendered.
     *
     * @return useAnisotropy true if anisotropy is to be applied, otherwise false.
     */
    public boolean isUseAnisotropy()
    {
        return useAnisotropy;
    }

    /**
     * Specifies whether texture anisotropy is applied to the texture when rendered.
     *
     * @param useAnisotropy true if anisotropy is to be applied, otherwise false.
     */
    public void setUseAnisotropy(boolean useAnisotropy)
    {
        this.useAnisotropy = useAnisotropy;
    }

    public boolean isTextureInitializationFailed()
    {
        return textureInitializationFailed;
    }

    public boolean bind(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        Texture t = dc.getTextureCache().get(this.imageSource);
        if (t == null)
        {
            t = this.initializeTexture(dc, this.imageSource);
            if (t != null)
                return true; // texture was bound during initialization.
        }

        if (t != null)
            t.bind();

        if (t != null && this.width == 0 && this.height == 0)
        {
            this.width = t.getWidth();
            this.height = t.getHeight();
            this.texCoords = t.getImageTexCoords();
        }

        return t != null;
    }

    public void applyInternalTransform(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        // Use the tile's texture if available.
        Texture t = dc.getTextureCache().get(this.imageSource);
        if (t == null)
            t = this.initializeTexture(dc, this.imageSource);

        if (t != null)
        {
            if (t.getMustFlipVertically())
            {
                GL gl = GLContext.getCurrent().getGL();
                gl.glMatrixMode(GL.GL_TEXTURE);
                gl.glLoadIdentity();
                gl.glScaled(1, -1, 1);
                gl.glTranslated(0, -1, 0);
            }
        }
    }

    protected Texture initializeTexture(DrawContext dc, Object imageSource)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        if (this.textureInitializationFailed)
            return null;

        Texture t;
        boolean haveMipMapData;

        if (imageSource instanceof String)
        {
            String path = (String) imageSource;

            Object streamOrException = WWIO.getFileOrResourceAsStream(path, this.getClass());
            if (streamOrException == null || streamOrException instanceof Exception)
            {
                Logging.logger().log(java.util.logging.Level.SEVERE, "generic.ExceptionAttemptingToReadImageFile",
                    streamOrException != null ? streamOrException : path);
                this.textureInitializationFailed = true;
                return null;
            }

            try
            {
                TextureData td = TextureIO.newTextureData((InputStream) streamOrException, this.useMipMaps, null);
                t = TextureIO.newTexture(td);
                haveMipMapData = td.getMipmapData() != null;
            }
            catch (Exception e)
            {
                String msg = Logging.getMessage("layers.TextureLayer.ExceptionAttemptingToReadTextureFile",
                    imageSource);
                Logging.logger().log(java.util.logging.Level.SEVERE, msg, e);
                this.textureInitializationFailed = true;
                return null;
            }
        }
        else if (imageSource instanceof BufferedImage)
        {
            try
            {
                TextureData td = TextureIO.newTextureData((BufferedImage) imageSource, this.useMipMaps);
                t = TextureIO.newTexture(td);
                haveMipMapData = td.getMipmapData() != null;
            }
            catch (Exception e)
            {
                String msg = Logging.getMessage("generic.IOExceptionDuringTextureInitialization");
                Logging.logger().log(java.util.logging.Level.SEVERE, msg, e);
                this.textureInitializationFailed = true;
                return null;
            }
        }
        else
        {
            Logging.logger().log(java.util.logging.Level.SEVERE, "generic.UnrecognizedImageSourceType",
                imageSource.getClass().getName());
            this.textureInitializationFailed = true;
            return null;
        }

        if (t == null) // In case JOGL TextureIO returned null
        {
            Logging.logger().log(java.util.logging.Level.SEVERE, "generic.TextureUnreadable",
                imageSource instanceof String ? imageSource : imageSource.getClass().getName());
            this.textureInitializationFailed = true;
            return null;
        }

        // Textures with the same path are assumed to be identical textures, so key the texture id off the
        // image source.
        dc.getTextureCache().put(imageSource, t);
        t.bind();

        // Enable the appropriate mip-mapping texture filters if the caller has specified that mip-mapping should be
        // enabled, and the texture itself supports mip-mapping.
        boolean useMipMapFilter = this.useMipMaps && (haveMipMapData || t.isUsingAutoMipmapGeneration());

        GL gl = dc.getGL();
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER,
            useMipMapFilter ? GL.GL_LINEAR_MIPMAP_LINEAR : GL.GL_LINEAR);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);

        if (this.isUseAnisotropy() && useMipMapFilter)
        {
            double maxAnisotropy = dc.getGLRuntimeCapabilities().getMaxTextureAnisotropy();
            if (dc.getGLRuntimeCapabilities().isUseAnisotropicTextureFilter() && maxAnisotropy >= 2.0)
            {
                gl.glTexParameterf(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAX_ANISOTROPY_EXT, (float) maxAnisotropy);
            }
        }

        this.width = t.getWidth();
        this.height = t.getHeight();
        this.texCoords = t.getImageTexCoords();
        this.textureInitialized = true;

        return t;
    }
}
