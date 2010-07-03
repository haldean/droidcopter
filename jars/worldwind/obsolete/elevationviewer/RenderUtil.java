/* Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package WorldWindHackApps.elevationviewer;

import gov.nasa.worldwind.geom.*;

import javax.media.opengl.GL;

/**
 * @author dcollins
 * @version $Id: RenderUtil.java 13023 2010-01-21 00:18:48Z dcollins $
 */
public class RenderUtil
{
    public static void drawOutlinedQuad(GL gl, float[] coords)
    {
        gl.glPushAttrib(GL.GL_CURRENT_BIT | GL.GL_POLYGON_BIT | GL.GL_LIGHTING_BIT);
        try
        {
            gl.glDisable(GL.GL_LIGHTING);
            
            gl.glColor4f(0.5f, 0.5f, 0.5f, 0.5f);
            gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_FILL);
            drawQuad(gl, coords);

            gl.glColor4f(0.5f, 0.0f, 0.0f, 0.5f);
            gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_LINE);
            drawQuad(gl, coords);
        }
        finally
        {
            gl.glPopAttrib();
        }
    }

    public static void drawQuad(GL gl, float[] coords)
    {
        gl.glBegin(GL.GL_QUADS);
        try
        {
            for (int i = 0; i < 12; i += 3)
                gl.glVertex3fv(coords, i);
        }
        finally
        {
            gl.glEnd();
        }
    }

    public static void drawLine(GL gl, Vec4 begin, Vec4 end)
    {
        gl.glBegin(GL.GL_LINES);
        try
        {
            gl.glVertex3d(begin.x, begin.y, begin.z);
            gl.glVertex3d(end.x, end.y, end.z);
        }
        finally
        {
            gl.glEnd();
        }
    }
}
