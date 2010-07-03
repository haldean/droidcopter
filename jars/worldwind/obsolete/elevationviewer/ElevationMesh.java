/* Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package WorldWindHackApps.elevationviewer;

import com.sun.opengl.util.BufferUtil;
import gov.nasa.worldwind.cache.Cacheable;
import gov.nasa.worldwind.data.BufferWrapperRaster;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.util.GeometryBuilder;

import javax.media.opengl.GL;
import java.awt.*;
import java.nio.*;

/**
 * @author dcollins
 * @version $Id: ElevationMesh.java 13023 2010-01-21 00:18:48Z dcollins $
 */
public class ElevationMesh implements SceneElement, Cacheable
{
    private int width;
    private int height;
    private MeshCoords coords;
    private boolean drawFill = true;
    private boolean drawBorder = true;
    private Material fillMaterial = new Material(Color.WHITE, Color.LIGHT_GRAY, Color.BLACK, Color.BLACK, 100f);
    private Color borderColor = Color.RED;

    private int fillDrawMode;
    private int fillDrawCount;
    private int borderDrawMode;
    private int borderDrawCount;
    private IntBuffer fillIndices;
    private IntBuffer borderIndices;
    private FloatBuffer vertices;
    private FloatBuffer normals;

    public ElevationMesh(int width, int height, float[] vertexArray, MeshCoords coords)
    {
        this.width = width;
        this.height = height;
        this.coords = coords;

        this.fillDrawMode = getFillDrawMode();
        this.fillDrawCount = getFillIndexCount(width, height);
        this.borderDrawMode = getBorderDrawMode();
        this.borderDrawCount = getBorderIndexCount(width, height);

        int[] fillIndexArray = fillIndicesForMesh(width, height);
        int[] borderIndexArray = borderIndicesForMesh(width, height);
        float[] normalArray = normalsForMesh(width, height, fillIndexArray, vertexArray);
        
        this.fillIndices = BufferUtil.newIntBuffer(fillIndexArray.length);
        this.borderIndices = BufferUtil.newIntBuffer(fillIndexArray.length);
        this.vertices = BufferUtil.newFloatBuffer(vertexArray.length);
        this.normals = BufferUtil.newFloatBuffer(normalArray.length);

        this.fillIndices.put(fillIndexArray);
        this.borderIndices.put(borderIndexArray);
        this.vertices.put(vertexArray);
        this.normals.put(normalArray);

        this.fillIndices.rewind();
        this.borderIndices.rewind();
        this.vertices.rewind();
        this.normals.rewind();
    }

    public ElevationMesh(BufferWrapperRaster raster, MeshCoords coords, double verticalOffset, double verticalScale)
    {
        this(raster.getWidth(), raster.getHeight(), verticesForRaster(raster, coords, verticalOffset, verticalScale), coords);
    }

    public static double[] getMinAndMaxValues(BufferWrapperRaster raster)
    {
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (int i = 0; i < raster.getBuffer().length(); i++)
        {
            double val = raster.getBuffer().getDouble(i);
            if (val != raster.getTransparentValue())
            {
                if (val < min)
                    min = val;
                if (val > max)
                    max = val;
            }
        }

        if (min == Double.MAX_VALUE && max == -Double.MAX_VALUE)
            min = max = 0d;
        return new double[] {min, max};
    }

    public static double suggestVerticalScale(Sector sector, MeshCoords coords)
    {
        double exaggeration = 3.0f;
        double earthRadius = 6378137.0;
        double latScale = (sector.getDeltaLatRadians() * earthRadius) / Math.abs(coords.right - coords.left);
        return exaggeration * (1d / latScale);
    }

    public int getWidth()
    {
        return this.width;
    }

    public int getHeight()
    {
        return this.height;
    }

    public MeshCoords getMeshCoords()
    {
        return this.coords;
    }

    public boolean isDrawFill()
    {
        return this.drawFill;
    }

    public void setDrawFill(boolean draw)
    {
        this.drawFill = draw;
    }

    public boolean isDrawBorder()
    {
        return this.drawBorder;
    }

    public void setDrawBorder(boolean draw)
    {
        this.drawBorder = draw;
    }

    public Material getFillMaterial()
    {
        return this.fillMaterial;
    }

    public void setFillMaterial(Material fillMaterial)
    {
        this.fillMaterial = fillMaterial;
    }

    public Color getBorderColor()
    {
        return this.borderColor;
    }

    public void setBorderColor(Color color)
    {
        this.borderColor = color;
    }

    public void render(GL gl, Camera camera)
    {
        gl.glPushAttrib(GL.GL_CURRENT_BIT | GL.GL_DEPTH_BUFFER_BIT | GL.GL_LIGHTING_BIT | GL.GL_POLYGON_BIT);
        try
        {
            gl.glEnable(GL.GL_DEPTH_TEST);
            gl.glEnable(GL.GL_CULL_FACE);

            if (this.drawFill)
            {
                gl.glDepthFunc(GL.GL_LESS);
                gl.glEnable(GL.GL_POLYGON_OFFSET_FILL);
                gl.glPolygonOffset(1.0f, 1.0f);
                this.fillMaterial.apply(gl, GL.GL_FRONT_AND_BACK, 1.0f);
                this.drawFill(gl);
            }

            if (this.drawBorder)
            {
                gl.glDisable(GL.GL_LIGHTING);
                gl.glDepthFunc(GL.GL_LEQUAL);
                float[] compArray = new float[4];
                this.borderColor.getRGBComponents(compArray);
                gl.glColor4f(compArray[0], compArray[1], compArray[2], compArray[3]);
                this.drawBorder(gl);
            }
        }
        finally
        {
            gl.glPopAttrib();
        }
    }

    public long getSizeInBytes()
    {
        return (Integer.SIZE / 8 ) * this.fillIndices.capacity()
             + (Float.SIZE / 8) * this.vertices.capacity()
             + (Float.SIZE / 8) * this.normals.capacity();
    }

    private void drawFill(GL gl)
    {
        gl.glPushClientAttrib(GL.GL_CLIENT_VERTEX_ARRAY_BIT);
        try
        {
            gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
            gl.glEnableClientState(GL.GL_NORMAL_ARRAY);

            gl.glVertexPointer(3, GL.GL_FLOAT, 0, this.vertices);
            gl.glNormalPointer(GL.GL_FLOAT, 0, this.normals);

            gl.glDrawElements(this.fillDrawMode, this.fillDrawCount, GL.GL_UNSIGNED_INT, this.fillIndices);
        }
        finally
        {
            gl.glPopClientAttrib();
        }
    }

    private void drawBorder(GL gl)
    {
        gl.glPushClientAttrib(GL.GL_CLIENT_VERTEX_ARRAY_BIT);
        try
        {
            gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
            gl.glVertexPointer(3, GL.GL_FLOAT, 0, this.vertices);
            gl.glDrawElements(this.borderDrawMode, this.borderDrawCount, GL.GL_UNSIGNED_INT, this.borderIndices);
        }
        finally
        {
            gl.glPopClientAttrib();
        }
    }

    private static int getFillDrawMode()
    {
        GeometryBuilder gb = new GeometryBuilder();
        return gb.getBilinearSurfaceFillDrawMode();
    }

    private static int getFillIndexCount(int width, int height)
    {
        GeometryBuilder gb = new GeometryBuilder();
        return gb.getBilinearSurfaceFillIndexCount(width - 1, height - 1);
    }

    private static int[] fillIndicesForMesh(int width, int height)
    {
        GeometryBuilder gb = new GeometryBuilder();
        int numIndices = gb.getBilinearSurfaceFillIndexCount(width - 1, height - 1);
        int[] indexArray = new int[numIndices];
        gb.makeBilinearSurfaceFillIndices(0, width - 1, height - 1, 0, indexArray);
        return indexArray;
    }

    private static int getBorderDrawMode()
    {
        return GL.GL_LINE_LOOP;
    }

    private static int getBorderIndexCount(int width, int height)
    {
        return 2 * width + 2 * (height - 2);
    }

    private static int[] borderIndicesForMesh(int width, int height)
    {
        int numIndices = getBorderIndexCount(width, height);
        int[] indexArray = new int[numIndices];

        int vertex;
        int index = 0;

        for (int i = 0; i < width; i++)
        {
            vertex = i;
            indexArray[index++] = vertex;
        }

        for (int j = 1; j < height - 1; j++)
        {
            vertex = (width - 1) + j * width;
            indexArray[index++] = vertex;
        }

        for (int i = width - 1; i >= 0; i--)
        {
            vertex = i + (width - 1) * width;
            indexArray[index++] = vertex;
        }

        for (int j = height - 2; j >= 1; j--)
        {
            vertex = j * width;
            indexArray[index++] = vertex;
        }

        return indexArray;
    }

    private static float[] verticesForRaster(BufferWrapperRaster raster, MeshCoords coords, double verticalOffset, double verticalScale)
    {
        GeometryBuilder gb = new GeometryBuilder();
        int width = raster.getWidth();
        int height = raster.getHeight();
        int numVertices = gb.getBilinearSurfaceVertexCount(width - 1, height - 1);
        float[] vertexArray = new float[3 * numVertices];
        float[] controlPoints = new float[12];
        coords.toControlPoints(controlPoints);
        gb.makeBilinearSurfaceVertices(controlPoints, 0, width - 1, height - 1, vertexArray);

        for (int j = 0; j < height; j++)
        {
            for (int i = 0; i < width; i++)
            {
                int vertexPos = i + j * width;
                int bufferPos = i + (width - j - 1) * width;
                double value = raster.getBuffer().getDouble(bufferPos);
                if (value == raster.getTransparentValue())
                    value = 0d;
                vertexArray[3 * vertexPos + 1] = (float) (verticalScale * (verticalOffset + value));
            }
        }

        return vertexArray;
    }

    private static float[] normalsForMesh(int width, int height, int[] indexArray, float[] vertexArray)
    {
        GeometryBuilder gb = new GeometryBuilder();
        int numIndices = gb.getBilinearSurfaceFillIndexCount(width - 1, height - 1);
        int numVertices = gb.getBilinearSurfaceVertexCount(width - 1, height - 1);
        float[] normalArray = new float[3 * numVertices];
        gb.makeIndexedTriangleStripNormals(
            0, numIndices, indexArray,
            0, numVertices, vertexArray,
            normalArray);
        return normalArray;
    }
}
