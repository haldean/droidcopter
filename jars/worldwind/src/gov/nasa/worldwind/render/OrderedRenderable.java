/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render;

/**
 * @author tag
 * @version $Id$
 */
public interface OrderedRenderable extends Renderable
{
    double getDistanceFromEye();

    public void pick(DrawContext dc, java.awt.Point pickPoint);
}
