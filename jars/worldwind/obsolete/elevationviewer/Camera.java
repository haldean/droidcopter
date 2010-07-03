/* Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package WorldWindHackApps.elevationviewer;

import gov.nasa.worldwind.geom.*;

import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;

/**
 * @author dcollins
 * @version $Id: Camera.java 13023 2010-01-21 00:18:48Z dcollins $
 */
public class Camera
{
    private Vec4 eye = new Vec4(0d, 0d, 1);
    private Vec4 center = new Vec4(0d, 0d, 0d);
    private Vec4 up = new Vec4(0d, 1d, 0d);
    private double near = 1d;
    private double far = 1000d;
    private Angle fov = Angle.fromDegrees(45d);

    public Camera()
    {
    }

    public Vec4 getEye()
    {
        return this.eye;
    }

    public void setEye(Vec4 eye)
    {
        this.eye = eye;
    }

    public Vec4 getCenter()
    {
        return this.center;
    }

    public void setCenter(Vec4 center)
    {
        this.center = center;
    }

    public Vec4 getUp()
    {
        return this.up;
    }

    public void setUp(Vec4 up)
    {
        this.up = up;
    }

    public double getNear()
    {
        return this.near;
    }

    public void setNear(double near)
    {
        this.near = near;
    }

    public double getFar()
    {
        return this.far;
    }

    public void setFar(double far)
    {
        this.far = far;
    }

    public Angle getFov()
    {
        return this.fov;
    }

    public void setFov(Angle fov)
    {
        this.fov = fov;
    }

    public void apply(GL gl)
    {
        gl.glPushAttrib(GL.GL_TRANSFORM_BIT);
        try
        {
            gl.glMatrixMode(GL.GL_MODELVIEW);
            new GLU().gluLookAt(
                this.eye.x, this.eye.y, this.eye.z,
                this.center.x, this.center.y, this.center.z,
                this.up.x, this.up.y, this.up.z);

            gl.glMatrixMode(GL.GL_PROJECTION);
            int[] viewport = new int[4];
            gl.glGetIntegerv(GL.GL_VIEWPORT, viewport, 0);
            double aspect = viewport[2] / (double) viewport[3];
            new GLU().gluPerspective(this.fov.degrees, aspect, this.near, this.far);            
        }
        finally
        {
            gl.glPopAttrib();
        }
    }
}
