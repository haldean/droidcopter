/* Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package WorldWindHackApps.elevationviewer;

import gov.nasa.worldwind.geom.*;

import java.awt.event.*;

/**
 * @author dcollins
 * @version $Id: CameraController.java 13023 2010-01-21 00:18:48Z dcollins $
 */
public class CameraController
{
    private static final double TRANSLATION_SCALE = 1000.0;
    private static final long TRANSLATION_TIME_FALLOFF = 300L;
    private static final double TRANSLATION_HEIGHT_FALLOFF = 10000.0;
    private static final double ROTATION_SCALE = 0.1;

    public CameraController()
    {
    }

    public void apply(Camera camera)
    {
        this.applyRotation(camera);
        this.applyTranslation(camera);
    }

    public void lookAt(Camera camera, MeshCoords coords)
    {
        Vec4 center = new Vec4(coords.right - coords.left, coords.bottom - coords.top, 0d);
        double distance = Math.abs(coords.top - coords.bottom) / camera.getFov().tanHalfAngle();
        Vec4 eye = center.add3(Vec4.UNIT_Y.multiply3(distance));

        camera.setEye(eye);
        camera.setCenter(center);
        camera.setUp(Vec4.UNIT_NEGATIVE_Z);
    }

    private void applyRotation(Camera camera)
    {
        Vec4 forward = camera.getCenter().subtract3(camera.getEye());
        Vec4 up = camera.getUp();

        Vec4[] axes = this.buildRotation(forward, up);
        forward = axes[0];
        up = axes[1];

        Vec4 center = camera.getEye().add3(forward);
        camera.setCenter(center);
        camera.setUp(up);
    }

    private void applyTranslation(Camera camera)
    {
        Vec4 forward = camera.getCenter().subtract3(camera.getEye());
        Vec4 up = camera.getUp();

        Vec4 trans = this.buildTranslation(camera.getEye(), forward, up);

        Vec4 eye = camera.getEye().add3(trans);
        Vec4 center = camera.getCenter().add3(trans);
        camera.setEye(eye);
        camera.setCenter(center);
    }

    private Vec4[] buildRotation(Vec4 forward, Vec4 up)
    {
        forward = forward.normalize3();
        up = up.normalize3();
        Vec4 s = forward.cross3(up);
        up = s.cross3(forward);
        up = up.normalize3();

        PolledInputAdapter input = PolledInputAdapter.getInstance();
        if (input.getMouseModifiers() == MouseEvent.BUTTON1_MASK)
        {
            Vec4 mouseMove = input.getMouseMovement();
            double xDegrees = this.getRotationAmount(-mouseMove.y);
            double yDegrees = this.getRotationAmount(-mouseMove.x);

            Matrix m = Matrix.fromAxisAngle(Angle.fromDegrees(xDegrees), s);
            forward = forward.transformBy3(m);
            up = up.transformBy3(m);

            m = Matrix.fromRotationY(Angle.fromDegrees(yDegrees));
            forward = forward.transformBy3(m);
            up = up.transformBy3(m);

            forward = forward.normalize3();
            up = up.normalize3();
            s = forward.cross3(up);
            up = s.cross3(forward);
            up = up.normalize3();
        }

        return new Vec4[] {forward, up};
    }

    private Vec4 buildTranslation(Vec4 eye, Vec4 forward, Vec4 up)
    {
        forward = forward.normalize3();
        up = up.normalize3();
        Vec4 right = forward.cross3(up);
        //up = right.cross3(forward);
        //up = up.normalize3();

        PolledInputAdapter input = PolledInputAdapter.getInstance();
        Vec4 vec = Vec4.ZERO;
        // Translation right/left.
        vec = vec.add3(right.multiply3(this.getTranslationAmount(eye, input.getKeyState(KeyEvent.VK_D))));
        vec = vec.subtract3(right.multiply3(this.getTranslationAmount(eye, input.getKeyState(KeyEvent.VK_A))));
        // Translation up/down.
        vec = vec.add3(Vec4.UNIT_Y.multiply3(this.getTranslationAmount(eye, input.getKeyState(KeyEvent.VK_SPACE))));
        vec = vec.subtract3(Vec4.UNIT_Y.multiply3(this.getTranslationAmount(eye, input.getKeyState(KeyEvent.VK_SHIFT))));
        // Translation forward/back.
        vec = vec.add3(forward.multiply3(this.getTranslationAmount(eye, input.getKeyState(KeyEvent.VK_W))));
        vec = vec.subtract3(forward.multiply3(this.getTranslationAmount(eye, input.getKeyState(KeyEvent.VK_S))));
        return vec;
    }

    private double getRotationAmount(double amount)
    {
        return ROTATION_SCALE * amount;
    }

    private double getTranslationAmount(Vec4 eye, PolledInputAdapter.KeyState state)
    {
        // TODO: handle case when we haven't reached "full speed" and the key is released

        if (state == null)
            return 0d;

        long elapsed = System.currentTimeMillis() - state.getWhen();
        
        double t;
        if (state.getState() == KeyEvent.KEY_PRESSED)
        {
            if (elapsed > TRANSLATION_TIME_FALLOFF)
            {
                t = 1d;
            }
            else
            {
                t = elapsed / (double) TRANSLATION_TIME_FALLOFF;
                t = (t < 0d) ? 0d : (t > 1d ? 1d : t);
            }
        }
        else // KEY_RELEASED
        {
            if (elapsed > TRANSLATION_TIME_FALLOFF)
            {
                t = 0d;
            }
            else
            {
                t = 1d - (elapsed / (double) TRANSLATION_TIME_FALLOFF);
                t = (t < 0d) ? 0d : (t > 1d ? 1d : t);
            }
        }

        double hMin = 0.00001;
        double h = Math.abs(eye.y) / TRANSLATION_HEIGHT_FALLOFF;
        h = (h < hMin) ? hMin : (h > 1d ? 1d : h);
        h = h * h;

        return TRANSLATION_SCALE * t * h;
    }
}
