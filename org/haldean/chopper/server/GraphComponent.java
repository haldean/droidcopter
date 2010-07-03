package org.haldean.chopper.server;

import java.util.LinkedList;
import java.util.Iterator;
import javax.swing.*;
import java.awt.*;

/**
 *  A class which draws a graph based on a series of given result sets.
 *  The graph automatically scales along the Y axis and can be manually
 *  scaled along the X axis.
 *  @author William Brown
 */
public class GraphComponent extends JComponent {
    private LinkedList<Double> series;
    private double max = 1;
    private double min = -1;
    /* Area left at the top and bottom to ensure the
     * graph never quite touches the edge */
    private double margin = 1;

    private int sampleCount = 100;
    /* The index of the first displayed item in the graph */
    private int firstIndex = 0;

    private Color background = new Color(28, 25, 20);
    private Color border = Color.darkGray;
    private Color line = Color.white;
    private Color axes = Color.lightGray;
    private Font labelFont = new Font(Font.SANS_SERIF, Font.PLAIN, 10);

    /* The graph's name is displayed on the X axis */
    private String name;

    /** Create a new GraphComponent with no name */
    public GraphComponent() {
	this(new String());
    }

    /** Create a new GraphComponent
     *  @param _name The name of the component */
    public GraphComponent(String _name) {
	super();
	name = _name;
	series = new LinkedList<Double>();
    }

    /** Manually set the maximum. Note that if a data point larger than this
     *  maximum is added, the graph will automatically scale. This sets a lower
     *  bound on the actual maximum of the graph.
     *  @param _max The lower bound for the maximum */
    public void setMax(double _max) {
	max = _max;
	repaint();
    }

    /** Manually set the minimum. Note that if a data point smaller than this
     *  minimum is added, the graph will automatically scale. This sets an upper
     *  bound on the actual minimum of the graph.
     *  @param _min The upper bound for the minimum */
    public void setMin(double _min) {
	min = _min;
	repaint();
    }

    /** Set the name of the graph */
    public void setName(String _name) {
	name = _name;
	repaint();
    }

    /** Set the scale along the X axis. 
     *  @param _sampleCount The number of samples shown along the X axis. The graph will
     *                      automatically adjust to show the most recent _sampleCount samples. */
    public void setSampleCount(int _sampleCount) {
	sampleCount = _sampleCount;
	/* Figure out which index is the first one displayed
	 * given the current graph scale */
	firstIndex = Math.max(0, series.size() - sampleCount);
	repaint();
    }

    /** Add a sample to the graph.
     *  @param p The Y-value of the sample to add */
    public void addPoint(double p) {
	/* Create a lock on the series list */
	synchronized (series) {
	    series.add(new Double(p));
	    /* Figure out which index is the first one displayed
	     * given the current graph scale */
	    firstIndex = Math.max(0, series.size() - sampleCount);
	    /* Adjust the bounds if necessary */
	    if (p > (max - margin))
		max = p + margin;
	    if (p < (min + margin))
		min = p - margin;
	}
	repaint();
    }

    /** Convert a value to a canvas pixel location 
     *  @param p The sample value
     *  @return The pixel Y corresponding to that sample value */
    public int pointToY(double p) {
	return (int) (((max - p) / (max - min)) * getSize().getHeight());
    }

    /** Convert a time-index to a canvas pixel location
     *  @param x The time index
     *  @return The pixel X corresponding to that time index */
    public int pointToX(int x) {
	return (int) (((float) x / (float) sampleCount) * getSize().getWidth());
    }

    /** Paints the graph onto the provided graphics object
     *  @param g The graphics object to paint onto
     */
    public void paintComponent(Graphics g) {
	Graphics2D g2 = (Graphics2D)g;

	int width = (int) getSize().getWidth();
	int height = (int) getSize().getHeight();

	/* The Y component of the X axis can shift based on scaling, so
	 * we calculate it once to save computation */
	int y0 = pointToY(0);

	/* Background */
	g2.setColor(background);
	g2.fillRect(0, 0, width, height);

	/* Border */
	g2.setColor(border);
	g2.drawRect(0, 0, width, height);

	/* Horizontal axis */
	g2.setColor(axes);
	g2.drawLine(0, y0, width, y0);

	/* Graph label */
	g2.setFont(labelFont);
	g2.drawString(name, 1, y0 - 2);

	g2.setColor(line);

	/* Create a lock on the series list so that the series
	 * cannot be updated while we are drawing */
	synchronized (series) {
	    if (series.size() > 0) {
		int lastY = pointToY(series.get(firstIndex));
		int lastX = pointToX(0);

		/* Loop through the points in X, connecting them as we go. */
		for (int i=firstIndex; i<series.size() && i < (firstIndex + sampleCount); i++) {
		    int y = pointToY(series.get(i));
		    int x = pointToX(i - firstIndex);

		    g2.drawLine(lastX, lastY, x, y);
		    lastY = y;
		    lastX = x;
		}
	    }
	}
    }

    /** Test code: draw an amplifying sine wave. Run this. It's mesmerizing. 
     *  @param args All command line arguments are ignored */
    public static void main(String args[]) {
	JFrame f = new JFrame();
	final GraphComponent gc = new GraphComponent("Amplifying Sine");
	gc.setPreferredSize(new Dimension(700, 400));
	gc.setSampleCount(720);
	f.add(gc);
	f.pack();
	f.setVisible(true);
	
	try {
	    double j=0;
	    for (int i=0; i<=360; i++) {
		j += 0.1;
		gc.addPoint(j * Math.sin(Math.toRadians(i)));
		Thread.sleep(20);
		if (i==360)
		    i = 0;
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}