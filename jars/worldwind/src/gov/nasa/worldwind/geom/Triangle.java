/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.geom;

import gov.nasa.worldwind.util.Logging;

/**
 * @author Eric Dalgliesh 30/11/2006
 * @version $Id: Triangle.java 11565 2009-06-11 01:22:16Z garakl $
 */
public class Triangle
{
    private static final double EPSILON = 0.0000001; // used in intersects method

    private final Vec4 a;
    private final Vec4 b;
    private final Vec4 c;

    public Triangle(Vec4 a, Vec4 b, Vec4 c)
    {
        if (a == null || b == null || c == null)
        {
            String msg = Logging.getMessage("nullValue.PointIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        this.a = a;
        this.b = b;
        this.c = c;
    }

    public Vec4 getA()
    {
        return this.a;
    }

    public Vec4 getB()
    {
        return this.b;
    }

    public Vec4 getC()
    {
        return this.c;
    }

//    private Plane getPlane()
//    {
//        Vector ab, ac;
//        ab = new Vector(this.b.subtract(this.a)).normalize();
//        ac = new Vector(this.c.subtract(this.a)).normalize();
//
//        Vector n = new Vector(new Point(ab.x(), ab.y(), ab.z(), ab.w()).cross(new Point(ac.x(), ac.y(), ac.z(), ac.w())));
//
//        return new gov.nasa.worldwind.geom.Plane(n);
//    }

//    private Point temporaryIntersectPlaneAndLine(Line line, Plane plane)
//    {
//        Vector n = line.getDirection();
//        Point v0 = Point.fromOriginAndDirection(plane.getDistance(), plane.getNormal(), Point.ZERO);
//        Point p0 = line.getPointAt(0);
//        Point p1 = line.getPointAt(1);
//
//        double r1 = n.dot(v0.subtract(p0))/n.dot(p1.subtract(p0));
//        if(r1 >= 0)
//            return line.getPointAt(r1);
//        return null;
//    }
//
//    private Triangle divide(double d)
//    {
//        d  = 1/d;
//        return new Triangle(this.a.multiply(d), this.b.multiply(d), this.c.multiply(d));
//    }


    public boolean contains( Vec4 p )
    {
        // Compute vectors
        Vec4 v0 = this.c.subtract3( this.a );
        Vec4 v1 = this.b.subtract3( this.a );
        Vec4 v2 = p.subtract3( this.a );

        // Compute dot products
        double dot00 = v0.dotSelf3();
        double dot01 = v0.dot3( v1 );
        double dot02 = v0.dot3( v2 );
        double dot11 = v1.dotSelf3();
        double dot12 = v1.dot3( v2 );

        // Compute barycentric coordinates
        double det = (dot00 * dot11 - dot01 * dot01);

        double detInv = 1 / det;
        double u = (dot11 * dot02 - dot01 * dot12) * detInv;
        double v = (dot00 * dot12 - dot01 * dot02) * detInv;

        // Check if point is contained in triangle (including edges and vertices)
        return (u >= 0d) && (v >= 0d) && (u + v <= 1d);

        // Check if point is contained inside triangle (NOT including edges or vertices)
//        return (u > 0d) && (v > 0d) && (u + v < 1d);
    }

    public Vec4 intersect(Line line)
    {
        if (line == null)
        {
            String msg = Logging.getMessage("nullValue.LineIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        // taken from Moller and Trumbore
        // http://www.cs.virginia.edu/~gfx/Courses/2003/ImageSynthesis/papers/Acceleration/
        // Fast%20MinimumStorage%20RayTriangle%20Intersection.pdf

        Vec4 origin = line.getOrigin();
        Vec4 dir = line.getDirection();

        double u, v;

        // find vectors for two edges sharing Point a
        Vec4 edge1 = this.b.subtract3(this.a);
        Vec4 edge2 = this.c.subtract3(this.a);

        // start calculating determinant
        Vec4 pvec = dir.cross3(edge2);

        // get determinant.
        double det = edge1.dot3(pvec);

        if (det > -EPSILON && det < EPSILON)
        {// If det is near zero, then ray lies on plane of triangle
            return null;
        }

        double detInv = 1d / det;

        // distance from vert0 to ray origin
        Vec4 tvec = origin.subtract3(this.a);

        // calculate u parameter and test bounds
        u = tvec.dot3(pvec) * detInv;
        if (u < 0 || u > 1)
        {
            return null;
        }

        // prepare to test v parameter
        Vec4 qvec = tvec.cross3(edge1);

        //calculate v parameter and test bounds
        v = dir.dot3(qvec) * detInv;
        if (v < 0 || u + v > 1)
        {
            return null;
        }

        double t = edge2.dot3(qvec) * detInv;
        if (t < 0)
        {
            return null;
        }

        return line.getPointAt(t);
    }

    // Test intersection with line
//    public static void main(String[] args)
//    {
//        Triangle t = new Triangle(
//                new Vec4(0, 0, 0),
//                new Vec4(1, 0, 0),
//                new Vec4(0, 1, 0));
//
//        Line l = new Line(new Vec4(.5, .5, 1), new Vec4(-.2, -.2, -1));
//
//        Vec4 intersect = t.intersect(l);
//        System.out.println(intersect);
//    }

    public String toString()
    {
        return "Triangle (" + a + ", " + b + ", " + c +")"; 
    }

}
