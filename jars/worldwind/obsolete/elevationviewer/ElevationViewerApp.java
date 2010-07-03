/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package WorldWindHackApps.elevationviewer;

import com.sun.opengl.util.*;

import javax.media.opengl.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * @author tag
 * @version $Id: ElevationViewerApp.java 13023 2010-01-21 00:18:48Z dcollins $
 */
public class ElevationViewerApp
{
    public static class AppFrame extends JFrame
    {
        private static final int DEFAULT_MAX_FRAMERATE = 60;

        private GLCanvas canvas;
        private Animator animator;
        private GLController glController;
        private AppController appController;
        private ElevationInputPanel elevationInputPanel;

        public AppFrame(Dimension canvasSize)
        {
            initComponents(canvasSize);
            setGLController(new GLController());
            setAppController(new AppController(this));
            PolledInputAdapter.attachTo(this.getCanvas());
        }

        public GLCanvas getCanvas()
        {
            return this.canvas;
        }

        public GLController getGLController()
        {
            return this.glController;
        }

        public void setGLController(GLController glController)
        {
            if (this.glController == glController)
                return;

            if (this.glController != null)
                this.canvas.removeGLEventListener(this.glController);

            this.glController = glController;

            if (this.glController != null)
                this.canvas.addGLEventListener(this.glController);
        }

        public AppController getAppController()
        {
            return this.appController;
        }

        public void setAppController(AppController appController)
        {
            this.appController = appController;
            this.elevationInputPanel.setActionListener(this.appController);
        }

        public ElevationInputPanel getInputPanel()
        {
            return this.elevationInputPanel;
        }

        public void start()
        {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    animator.start();
                }
            });
        }

        public void stop()
        {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    animator.stop();
                }
            });
        }

        private void initComponents(Dimension canvasSize)
        {
            this.canvas = new GLCanvas();
            this.canvas.setPreferredSize((canvasSize != null) ? canvasSize : new Dimension(640, 480));
            this.animator = new FPSAnimator(DEFAULT_MAX_FRAMERATE);
            this.animator.add(this.canvas);
            
            this.elevationInputPanel = new ElevationInputPanel();

            this.getContentPane().setLayout(new BorderLayout());
            this.getContentPane().add(this.canvas, BorderLayout.CENTER);
            this.getContentPane().add(this.elevationInputPanel, BorderLayout.SOUTH);

            this.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent windowEvent) {
                    stop();
                }
            });

            this.pack();
        }
    }

    public static void main(String[] arg)
    {
        final AppFrame frame = new AppFrame(new Dimension(1280, 800));
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                frame.setVisible(true);
                frame.start();
            }
        });
    }
}