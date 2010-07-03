/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.geom;

import gov.nasa.worldwind.util.Logging;

/**
 * @author Tom Gaskins
 * @version $Id: Intersection.java 2471 2007-07-31 21:50:57Z tgaskins $
 */
public final class Intersection // Instances are immutable
{

    private final Vec4 intersectionPoint;
    private final boolean isTangent;

    /**
     * @param intersectionPoint
     * @param isTangent
     * @throws IllegalArgumentException if <code>intersectionpoint</code> is null
     */
    public Intersection(Vec4 intersectionPoint, boolean isTangent)
    {
        if (intersectionPoint == null)
        {
            String message = Logging.getMessage("nullValue.IntersectionPointIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        this.intersectionPoint = intersectionPoint;
        this.isTangent = isTangent;
    }

    public final Vec4 getIntersectionPoint()
    {
        return intersectionPoint;
    }

    public final boolean isTangent()
    {
        return isTangent;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        final gov.nasa.worldwind.geom.Intersection that = (gov.nasa.worldwind.geom.Intersection) o;

        if (isTangent != that.isTangent)
            return false;
        //noinspection RedundantIfStatement
        if (!intersectionPoint.equals(that.intersectionPoint))
            return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        int result;
        result = intersectionPoint.hashCode();
        result = 29 * result + (isTangent ? 1 : 0);
        return result;
    }

    @Override
    public String toString()
    {
        String pt = "Intersection Point: " + this.intersectionPoint;
        String tang = this.isTangent ? " is a tangent." : " not a tangent";
        return pt + tang;
    }
}
