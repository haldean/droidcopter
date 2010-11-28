package org.haldean.chopper.server;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.LinkedList;

import org.haldean.simplegraph.StreamingGraphComponent;

/** 
 *  A component to display the PID errors from the tuning loops.
 *
 *  @author William Brown
 */
public class PidErrorComponent extends JPanel implements Updatable {
    private final StreamingGraphComponent loop1;
    private final StreamingGraphComponent loop2;
    private final StreamingGraphComponent loop3;
    private final StreamingGraphComponent loop4;

    private final StreamingGraphComponent total;

    /* Components to choose the graph scale */
    private JPanel scalePanel;
    private JSlider scaleChooser;
    private final JLabel scaleLabel;
    /* The default X-axis scale of the graphs */
    private final int defaultScale = 300;

    public PidErrorComponent() {
	super(new BorderLayout());
	JPanel graphPanel = new JPanel(new GridLayout(2,2));
	GridBagConstraints gbc = new GridBagConstraints();

	loop1 = new StreamingGraphComponent(StyleProvider.graphFor("Loop 1 Error"));
	loop2 = new StreamingGraphComponent(StyleProvider.graphFor("Loop 2 Error"));
	loop3 = new StreamingGraphComponent(StyleProvider.graphFor("Loop 3 Error"));
	loop4 = new StreamingGraphComponent(StyleProvider.graphFor("Loop 4 Error"));
	total = new StreamingGraphComponent(StyleProvider.graphFor("Total Absolute Error"));

	gbc.fill = GridBagConstraints.HORIZONTAL;
	gbc.gridx = 0;
	gbc.gridy = 0;
	graphPanel.add(loop1);
	
	gbc.gridx = 1;
	graphPanel.add(loop2);

	gbc.gridx = 0;
	gbc.gridy = 1;
	graphPanel.add(loop3);

	gbc.gridx = 1;
	graphPanel.add(loop4);

	gbc.gridx = 0;
	gbc.gridy = 2;
	gbc.gridwidth = 2;
	//graphPanel.add(total, gbc);
     
	add(graphPanel, BorderLayout.CENTER);

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

	add(scalePanel, BorderLayout.SOUTH);

	repaint();
    }

    /** Used for TabPanes */
    public String getName() {
	return "Tuning Errors";
    }

    /** Set the scale of all of the underlying graphs
     *  @param s The number of samples to show along the X axis */
    public void setScale(int s) {
	loop1.setSampleCount(s);
	loop2.setSampleCount(s);
	loop3.setSampleCount(s);
	loop4.setSampleCount(s);
	total.setSampleCount(s);
	scaleLabel.setText(s + " samples");
    }

    /** Update the look and feel of this component */
    public void updateUI() {
	super.updateUI();
	if (scalePanel != null) {
	    scalePanel.updateUI();
	    scaleLabel.updateUI();
	    scaleChooser.updateUI();
	}
    }

    /** Add a new acceleration data point 
     *  @param x The x-component of the acceleration
     *  @param y The y-component of the acceleration
     *  @param z The z-component of the acceleration */
    public void setErrors(double l1, double l2, double l3, double l4) {
	loop1.addValue(l1);
	loop2.addValue(l2);
	loop3.addValue(l3);
	loop4.addValue(l4);
	total.addValue(Math.abs(l1) + Math.abs(l2) + Math.abs(l3) + Math.abs(l4));
	repaint();
    }
    
    public void update(String message) {
	if (!message.startsWith("GUID:ERROR")) return;
	System.out.println(message);
	String parts[] = message.split(":");
	setErrors(new Double(parts[2]), new Double(parts[3]), 
		  new Double(parts[4]), new Double(parts[5]));
    }
}