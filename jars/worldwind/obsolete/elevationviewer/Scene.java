/* Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package WorldWindHackApps.elevationviewer;

import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.util.Logging;

import javax.media.opengl.GL;
import java.util.*;

/**
 * @author dcollins
 * @version $Id: Scene.java 13023 2010-01-21 00:18:48Z dcollins $
 */
public class Scene
{
    private List<SceneElement> sceneElements = new ArrayList<SceneElement>();
    private boolean enableLighting = true;
    private Material lightMaterial = Material.WHITE;
    private Vec4 lightDirection = new Vec4(0.6, 0.6, 1.0);

    public Scene()
    {
    }

    public void addElement(SceneElement elem)
    {
        this.sceneElements.add(elem);
    }

    public void addAllElements(Collection<? extends SceneElement> elems)
    {
        this.sceneElements.addAll(elems);
    }

    public void removeElement(SceneElement elem)
    {
        this.sceneElements.remove(elem);
    }

    public void removeAllElements(Collection<? extends SceneElement> elems)
    {
        this.sceneElements.removeAll(elems);
    }

    public void clearElements()
    {
        this.sceneElements.clear();
    }

    public List<SceneElement> getElements()
    {
        return Collections.unmodifiableList(this.sceneElements);
    }

    public boolean isEnableLighting()
    {
        return this.enableLighting;
    }

    public void setEnableLighting(boolean enable)
    {
        this.enableLighting = enable;
    }

    public Material getLightMaterial()
    {
        return this.lightMaterial;
    }

    public void setLightMaterial(Material material)
    {
        if (material == null)
        {
            String message = Logging.getMessage("nullValue.MaterialIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.lightMaterial = material;
    }

    public Vec4 getLightDirection()
    {
        return this.lightDirection;
    }

    public void setLightDirection(Vec4 direction)
    {
        if (direction == null)
        {
            String message = Logging.getMessage("nullValue.DirectionIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.lightDirection = direction;
    }

    public void render(GL gl, Camera camera)
    {
        this.renderAxes(gl, camera);

        this.beginRendering(gl);
        try
        {
            for (SceneElement elem : this.sceneElements)
                elem.render(gl, camera);
        }
        finally
        {
            this.endRendering(gl);
        }
    }

    protected void beginRendering(GL gl)
    {
        gl.glPushAttrib(GL.GL_CURRENT_BIT | GL.GL_LIGHTING_BIT | GL.GL_TRANSFORM_BIT);

        float[] modelAmbient  = new float[4];
        modelAmbient[0] = 1.0f;
        modelAmbient[1] = 1.0f;
        modelAmbient[2] = 1.0f;
        modelAmbient[3] = 0.0f;

        gl.glEnable(GL.GL_LIGHTING);
        gl.glLightModelfv(GL.GL_LIGHT_MODEL_AMBIENT, modelAmbient, 0);
        gl.glLightModeli(GL.GL_LIGHT_MODEL_LOCAL_VIEWER, GL.GL_TRUE);
        gl.glLightModeli(GL.GL_LIGHT_MODEL_TWO_SIDE, GL.GL_FALSE);
        gl.glShadeModel(GL.GL_SMOOTH);

        gl.glEnable(GL.GL_LIGHT0);
        this.setLightMaterial(gl, GL.GL_LIGHT0, this.lightMaterial);
        this.setLightDirection(gl, GL.GL_LIGHT0, this.lightDirection);
    }

    protected void endRendering(GL gl)
    {
        gl.glPopAttrib();
    }

    protected void setLightMaterial(GL gl, int light, Material material)
    {
        // The alpha value at a vertex is taken only from the diffuse material's alpha channel, without any
        // lighting computations applied. Therefore we specify alpha=0 for all lighting ambient, specular and
        // emission values. This will have no effect on material alpha.

        float[] ambient  = new float[4];
        float[] diffuse  = new float[4];
        float[] specular = new float[4];
        material.getDiffuse().getRGBColorComponents(diffuse);
        material.getSpecular().getRGBColorComponents(specular);
        ambient[3] = diffuse[3] = specular[3] = 0.0f;

        gl.glLightfv(light, GL.GL_AMBIENT, ambient, 0);
        gl.glLightfv(light, GL.GL_DIFFUSE, diffuse, 0);
        gl.glLightfv(light, GL.GL_SPECULAR, specular, 0);
    }

    protected void setLightDirection(GL gl, int light, Vec4 direction)
    {
        // Setup the light as a directional light coming from the viewpoint. This requires two state changes
        // (a) Set the light position as direction x, y, z, and set the w-component to 0, which tells OpenGL this is
        //     a directional light.
        // (b) Invoke the light position call with the identity matrix on the modelview stack. Since the position
        //     is transfomed by the

        Vec4 vec = direction.normalize3();
        float[] params = new float[4];
        params[0] = (float) vec.x;
        params[1] = (float) vec.y;
        params[2] = (float) vec.z;
        params[3] = 0.0f;

        gl.glPushAttrib(GL.GL_TRANSFORM_BIT);
        try
        {
            gl.glMatrixMode(GL.GL_MODELVIEW);
            gl.glPushMatrix();
            try
            {
                gl.glLoadIdentity();
                gl.glLightfv(light, GL.GL_POSITION, params, 0);
            }
            finally
            {
                gl.glPopMatrix();
            }
        }
        finally
        {
            gl.glPopAttrib();   
        }
    }

    private void renderAxes(GL gl, Camera camera)
    {
        double distance = camera.getFar();

        gl.glPushAttrib(GL.GL_CURRENT_BIT | GL.GL_LINE_BIT);
        try
        {
            gl.glLineWidth(1f);

            gl.glColor3f(0f, 0f, 1f); // Blue.
            RenderUtil.drawLine(gl, Vec4.ZERO, new Vec4(distance, 0d, 0d)); // X-axis.

            gl.glColor3f(0f, 1f, 0f); // Green.
            RenderUtil.drawLine(gl, Vec4.ZERO, new Vec4(0d, distance, 0d)); // Y-axis.

            gl.glColor3f(1f, 0f, 0f); // Red.
            RenderUtil.drawLine(gl, Vec4.ZERO, new Vec4(0d, 0d, distance)); // Z-axis.
        }
        finally
        {
            gl.glPopAttrib();
        }
    }
}
