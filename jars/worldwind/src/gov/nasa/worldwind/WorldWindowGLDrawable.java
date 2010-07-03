/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind;

import gov.nasa.worldwind.cache.TextureCache;

import javax.media.opengl.GLAutoDrawable;

/**
 * @author tag
 * @version $Id: WorldWindowGLDrawable.java 11421 2009-06-03 13:23:25Z tgaskins $
 */
public interface WorldWindowGLDrawable extends WorldWindow
{
    void initDrawable(GLAutoDrawable glAutoDrawable);

    void initTextureCache(TextureCache textureCache);

    void endInitalization();
}
