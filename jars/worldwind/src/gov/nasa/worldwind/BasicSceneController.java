/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind;

import gov.nasa.worldwind.render.DrawContext;

/**
 * @author Tom Gaskins
 * @version $Id: BasicSceneController.java 12618 2009-09-21 06:56:22Z jaddison $
 */
public class BasicSceneController extends AbstractSceneController
{
    public void doRepaint(DrawContext dc)
    {
        this.initializeFrame(dc);
        try
        {
            this.applyView(dc);
            dc.addPickPointFrustum();
            this.createTerrain(dc);
            this.preRender(dc);
            this.clearFrame(dc);
            this.pick(dc);
            this.clearFrame(dc);
            this.draw(dc);
        }
        finally
        {
            this.finalizeFrame(dc);
        }
    }
}
