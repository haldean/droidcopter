/* Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package WorldWindHackApps.elevationviewer;

import javax.media.opengl.GL;

/**
 * @author dcollins
 * @version $Id: SceneElement.java 13023 2010-01-21 00:18:48Z dcollins $
 */
public interface SceneElement
{
    void render(GL gl, Camera camera);
}
