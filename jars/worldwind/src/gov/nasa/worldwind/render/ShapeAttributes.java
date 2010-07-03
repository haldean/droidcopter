/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render;

import gov.nasa.worldwind.util.RestorableSupport;

/**
 * @author dcollins
 * @version $Id$
 */
public interface ShapeAttributes
{
    ShapeAttributes copy();

    boolean isDrawInterior();

    void setDrawInterior(boolean draw);

    boolean isDrawOutline();

    void setDrawOutline(boolean draw);

    boolean isEnableAntialiasing();

    void setEnableAntialiasing(boolean enable);

    Material getInteriorMaterial();

    void setInteriorMaterial(Material material);

    Material getOutlineMaterial();

    void setOutlineMaterial(Material material);

    double getInteriorOpacity();

    void setInteriorOpacity(double opacity);

    double getOutlineOpacity();

    void setOutlineOpacity(double opacity);

    double getOutlineWidth();

    void setOutlineWidth(double width);

    int getOutlineStippleFactor();

    void setOutlineStippleFactor(int factor);

    short getOutlineStipplePattern();

    void setOutlineStipplePattern(short pattern);

    Object getInteriorImageSource();

    void setInteriorImageSource(Object imageSource);

    double getInteriorImageScale();

    void setInteriorImageScale(double scale);

    void getRestorableState(RestorableSupport rs, RestorableSupport.StateObject so);

    void restoreState(RestorableSupport rs, RestorableSupport.StateObject so);
}
