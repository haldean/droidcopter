/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.layers.Moon;

import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.util.Logging;

/**
 * Base (one image) layer for Moon Clementine 40xx color dataset.
 * @author Patrick Murris
 * @version $Id: Clementine40BaseLayer.java 8315 2009-01-02 06:57:35Z tgaskins $
 */
public class Clementine40BaseLayer extends RenderableLayer
{
    public Clementine40BaseLayer()
    {
        String path = Configuration.getStringValue(AVKey.MOON_CLEMENTIN40_ONE_IMAGE_PATH);
        if (path == null)
        {
            String message = Logging.getMessage("layers.Moon.Clementine40BaseLayer.PathNotGiven");
            throw new IllegalStateException(message);
        }

        this.setName(Logging.getMessage("layers.Moon.Clementine40BaseLayer.Name"));
        // TODO: Implement image retrieval.
//        this.addRenderable(new SurfaceImage(path, Sector.FULL_SPHERE, this, "Moon/Clementine40_Base"));

        // Disable picking for the layer because it covers the full sphere and will override a terrain pick.
        this.setPickEnabled(false);
    }

    @Override
    public String toString()
    {
        return Logging.getMessage("layers.Moon.Clementine40BaseLayer.Name");
    }
}
