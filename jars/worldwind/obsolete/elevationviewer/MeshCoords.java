/* Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package WorldWindHackApps.elevationviewer;

import javax.media.opengl.GL;

/**
 * @author dcollins
 * @version $Id: MeshCoords.java 13023 2010-01-21 00:18:48Z dcollins $
 */
public class MeshCoords
{
    final float top;
    final float left;
    final float bottom;
    final float right;

    public MeshCoords(float top, float left, float bottom, float right)
    {
        this.top = top;
        this.left = left;
        this.bottom = bottom;
        this.right = right;
    }

    public void drawCoords(GL gl)
    {
        float[] coords = new float[12];
        this.toControlPoints(coords);
        RenderUtil.drawOutlinedQuad(gl, coords);
    }

    public MeshCoords[] subdivide()
    {
        float hCenter = this.left + (this.right - this.left) / 2f;
        float vCenter = this.bottom + (this.top - this.bottom) / 2f;

        MeshCoords[] subCoords = new MeshCoords[4];
        subCoords[0] = new MeshCoords(vCenter,  this.left, this.bottom, hCenter);    // Lower left quadrant.
        subCoords[1] = new MeshCoords(vCenter,  hCenter,   this.bottom, this.right); // Lower right quadrant.
        subCoords[2] = new MeshCoords(this.top, hCenter,   vCenter,     this.right); // Upper right quadrant.
        subCoords[3] = new MeshCoords(this.top, this.left, vCenter,     hCenter); // Upper left quadrant.
        return subCoords;
    }

    public void toControlPoints(float[] outCoords)
    {
        // Lower left control point.
        outCoords[0] = this.left;
        outCoords[1] = 0f;
        outCoords[2] = this.bottom;

        // Lower right control point
        outCoords[3] = this.right;
        outCoords[4] = 0f;
        outCoords[5] = this.bottom;

        // Upper right control point
        outCoords[6] = this.right;
        outCoords[7] = 0f;
        outCoords[8] = this.top;

        // Upper left control point
        outCoords[9] = this.left;
        outCoords[10] = 0f;
        outCoords[11] = this.top;
    }
}
