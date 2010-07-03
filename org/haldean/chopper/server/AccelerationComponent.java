package org.haldean.chopper.server;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.LinkedList;

/** A component to represent the different axes of acceleration 
 *  @author William Brown */
public class AccelerationComponent extends JPanel {
    /* The graphs for each axis of acceleration */
    private final GraphComponent xAccel;
    private final GraphComponent yAccel;
    private final GraphComponent zAccel;
    /* This plots the absolute magnitude of the acceleration */
    private final GraphComponent magAccel;
    /* This plots a running average */
    private final GraphComponent avgAccel;

    /* These show the numeric values of each component */
    private JPanel statsPanel;
    private final JLabel xLabel;
    private final JLabel yLabel;
    private final JLabel zLabel;
    private final JLabel avgLabel;
    private final JLabel deltaLabel;

    /* Components to choose the graph scale */
    private JPanel scalePanel;
    private JSlider scaleChooser;
    private final JLabel scaleLabel;

    /* The points used for calculating the average magnitude */
    private LinkedList<Double> points;
    /* The number of points to consider in the running average */
    private final int averagePointCount = 100;
    /* The default X-axis scale of the graphs */
    private final int defaultScale = 300;

    private final Color foreground = Color.white;
    private final Color background = new Color(28, 25, 20);

    /** Create a new Acceleration Component */
    public AccelerationComponent() {
	super(new BorderLayout());
	JPanel graphsPanel = new JPanel(new GridLayout(3,2));

	points = new LinkedList<Double>();

	xAccel = new GraphComponent("X");
	yAccel = new GraphComponent("Y");
	zAccel = new GraphComponent("Z");
	magAccel = new GraphComponent("Magnitude");
	avgAccel = new GraphComponent(averagePointCount + "-Sample Average");

	xLabel = new JLabel();
	yLabel = new JLabel();
	zLabel = new JLabel();
	avgLabel = new JLabel();
	deltaLabel = new JLabel();

	statsPanel = new JPanel(new GridLayout(5,1));
	statsPanel.add(xLabel);
	statsPanel.add(yLabel);
	statsPanel.add(zLabel);
	statsPanel.add(avgLabel);
	statsPanel.add(deltaLabel);

	scaleChooser = new JSlider(25, 500, defaultScale);
	scaleLabel = new JLabel(scaleChooser.getValue() + " samples");
	/* When the scale is changed, update the graphs to reflect
	 * that change */
	scaleChooser.addChangeListener(new ChangeListener() {
		public void stateChanged(ChangeEvent e) {
		    setScale(scaleChooser.getValue());
		}
	    });
	setScale(defaultScale);

	scalePanel = new JPanel(new BorderLayout());
	scalePanel.add(scaleLabel, BorderLayout.EAST);
	scalePanel.add(scaleChooser, BorderLayout.CENTER);

	graphsPanel.add(xAccel);
	graphsPanel.add(yAccel);
	graphsPanel.add(zAccel);
	graphsPanel.add(magAccel);
	graphsPanel.add(avgAccel);
	graphsPanel.add(statsPanel);

	add(graphsPanel, BorderLayout.CENTER);
	add(scalePanel, BorderLayout.SOUTH);
    }

    /** Used for TabPanes */
    public String getName() {
	return "Acceleration";
    }

    /** Set the scale of all of the underlying graphs
     *  @param s The number of samples to show along the X axis */
    public void setScale(int s) {
	xAccel.setSampleCount(s);
	yAccel.setSampleCount(s);
	zAccel.setSampleCount(s);
	magAccel.setSampleCount(s);
	avgAccel.setSampleCount(s);
	scaleLabel.setText(s + " samples");
    }

    /** Update the look and feel of this component */
    public void updateUI() {
	super.updateUI();
	if (statsPanel != null) {
	    statsPanel.updateUI();
	    xLabel.updateUI();
	    yLabel.updateUI();
	    zLabel.updateUI();
	    avgLabel.updateUI();
	    deltaLabel.updateUI();

	    scalePanel.updateUI();
	    scaleLabel.updateUI();
	    scaleChooser.updateUI();
	}
    }

    /** Find the running average of the acceleration magnitude
     *  @return The average over averagePointCount samples */
    private double average() {
	double sum = 0;
	int i;
	/* Don't allow changes to the list while calculating */
	synchronized (points) {
	    for (i=0; i<points.size(); i++)
		sum += points.get(i);
	}
	return sum / i;
    }

    /** Add a new acceleration data point 
     *  @param x The x-component of the acceleration
     *  @param y The y-component of the acceleration
     *  @param z The z-component of the acceleration */
    public void setAcceleration(double x, double y, double z) {
	/* Magnitude is sqrt(x^2 + y^2 + z^2). What I would
	 * give for a power operator. */
	double mag = Math.sqrt(Math.pow(x, 2) +
			       Math.pow(y, 2) +
			       Math.pow(z, 2));
	/* Add the magnitude to the average points */
	points.add(mag);

	/* This lock is necessary because multiple accelerations could
	 * be added at the same time in weird cases. This makes sure
	 * we don't delete too many. */
	synchronized (points) {
	    while (points.size() > averagePointCount)
		points.removeFirst();
	}
	double avg = average();

	xAccel.addPoint(x);
	yAccel.addPoint(y);
	zAccel.addPoint(z);
	magAccel.addPoint(mag);
	avgAccel.addPoint(avg);

	/* HTML is used to set the line label bold and the text plain */
	xLabel.setText("<html><b>Fx</b>: " + x + " N</html>");
	yLabel.setText("<html><b>Fy</b>: " + y + " N</html>");
	zLabel.setText("<html><b>Fz</b>: " + z + " N</html>");
	avgLabel.setText("<html><b>|Favg|</b>: " + avg + " N</html>");
	deltaLabel.setText("<html><b>|Favg - F|</b>: " + (avg - mag) + " N</html>"); 

	repaint();
    }
}