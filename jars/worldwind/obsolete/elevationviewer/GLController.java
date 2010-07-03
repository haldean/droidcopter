/* Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package WorldWindHackApps.elevationviewer;

import javax.media.opengl.*;
import java.awt.*;

/**
 * @author dcollins
 * @version $Id: GLController.java 13023 2010-01-21 00:18:48Z dcollins $
 */
public class GLController implements GLEventListener
{
    private Camera camera;
    private CameraController cameraController;
    private Scene scene;
    private Color clearColor = Color.DARK_GRAY;

    public GLController()
    {
        this.camera = new Camera();
        this.cameraController = new CameraController();
        this.scene = new Scene();
    }

    public Camera getCamera()
    {
        return this.camera;
    }

    public void setCamera(Camera camera)
    {
        this.camera = camera;
    }

    public CameraController getCameraController()
    {
        return this.cameraController;
    }

    public void setCameraController(CameraController cameraController)
    {
        this.cameraController = cameraController;
    }

    public Scene getScene()
    {
        return this.scene;
    }

    public void setScene(Scene scene)
    {
        this.scene = scene;
    }

    public Color getClearColor()
    {
        return this.clearColor;
    }

    public void setClearColor(Color clearColor)
    {
        this.clearColor = clearColor;
    }

    public void init(GLAutoDrawable glAutoDrawable)
    {
    }

    public void display(GLAutoDrawable glAutoDrawable)
    {
        GL gl = glAutoDrawable.getGL();
        this.clearBuffers(gl);
        this.beforeRender(gl);
        this.applyCamera(gl);
        this.renderScene(gl);
        this.afterRender(gl);

        PolledInputAdapter.getInstance().update();
    }

    protected void clearBuffers(GL gl)
    {
        float[] compArray = new float[4];
        this.clearColor.getRGBComponents(compArray);
        gl.glClearColor(compArray[0], compArray[1], compArray[2], compArray[3]);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
    }

    protected void applyCamera(GL gl)
    {
        this.cameraController.apply(this.camera);
        this.camera.apply(gl);
    }

    protected void renderScene(GL gl)
    {
        this.scene.render(gl, this.camera);
    }

    protected void beforeRender(GL gl)
    {
        gl.glPushAttrib(GL.GL_TRANSFORM_BIT);
        try
        {
            gl.glMatrixMode(GL.GL_MODELVIEW);
            gl.glPushMatrix();

            gl.glMatrixMode(GL.GL_PROJECTION);
            gl.glPushMatrix();

            gl.glMatrixMode(GL.GL_TEXTURE);
            gl.glPushMatrix();
        }
        finally
        {
            gl.glPopAttrib();
        }
    }

    protected void afterRender(GL gl)
    {
        gl.glPushAttrib(GL.GL_TRANSFORM_BIT);
        try
        {
            gl.glMatrixMode(GL.GL_MODELVIEW);
            gl.glPopMatrix();

            gl.glMatrixMode(GL.GL_PROJECTION);
            gl.glPopMatrix();

            gl.glMatrixMode(GL.GL_TEXTURE);
            gl.glPopMatrix();
        }
        finally
        {
            gl.glPopAttrib();
        }
    }

    public void reshape(GLAutoDrawable glAutoDrawable, int i, int i1, int i2, int i3)
    {
    }

    public void displayChanged(GLAutoDrawable glAutoDrawable, boolean b, boolean b1)
    {
    }
}
