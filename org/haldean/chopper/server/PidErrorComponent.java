package org.haldean.chopper.server;

import java.awt.*;
import java.awt.event.*;
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

    /* Components to choose the graph scale */
    private JPanel scalePanel;
    private JSlider scaleChooser;
    private final JLabel scaleLabel;
    /* The default X-axis scale of the graphs */
    private final int defaultScale = 300;
    private JButton pidButton;

    public PidErrorComponent() {
	super(new BorderLayout());
	JPanel graphPanel = new JPanel(new GridLayout(2,2));

	loop1 = new StreamingGraphComponent(StyleProvider.graphFor("Loop 1 Error"));
	loop2 = new StreamingGraphComponent(StyleProvider.graphFor("Loop 2 Error"));
	loop3 = new StreamingGraphComponent(StyleProvider.graphFor("Loop 3 Error"));
	loop4 = new StreamingGraphComponent(StyleProvider.graphFor("Loop 4 Error"));

	graphPanel.add(loop1);
	graphPanel.add(loop2);
	graphPanel.add(loop3);
	graphPanel.add(loop4);
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

	pidButton = new JButton("Change Tuning Parameters");
	pidButton.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    PidTuningComponent.activate();
		}
	    });

	scalePanel = new JPanel(new BorderLayout());
	scalePanel.add(scaleLabel, BorderLayout.EAST);
	scalePanel.add(scaleChooser, BorderLayout.CENTER);
	scalePanel.add(pidButton, BorderLayout.WEST);

	add(scalePanel, BorderLayout.SOUTH);

	repaint();
    }

    /** 
     *  Used for TabPanes 
     */
    public String getName() {
	return "Tuning Errors";
    }

    /** 
     *  Set the scale of all of the underlying graphs
     *
     *  @param s The number of samples to show along the X axis
     */
    public void setScale(int s) {
	loop1.setSampleCount(s);
	loop2.setSampleCount(s);
	loop3.setSampleCount(s);
	loop4.setSampleCount(s);
	scaleLabel.setText(s + " samples");
    }

    /** 
     *  Update the look and feel of this component
     */
    public void updateUI() {
	super.updateUI();
	if (scalePanel != null) {
	    scalePanel.updateUI();
	    scaleLabel.updateUI();
	    scaleChooser.updateUI();
	    pidButton.updateUI();
	}
    }

    /** 
     *  Add a new acceleration data point 
     *
     *  @param l1 The error in Loop 1
     *  @param l2 The error in Loop 2
     *  @param l3 The error in Loop 3
     *  @param l4 The error in Loop 4
     */
    public void setErrors(double l1, double l2, double l3, double l4) {
	loop1.addValue(l1);
	loop2.addValue(l2);
	loop3.addValue(l3);
	loop4.addValue(l4);
	repaint();
    }
    
    public void update(String message) {
	if (!message.startsWith("GUID:ERROR")) return;
	String parts[] = message.split(":");
	setErrors(new Double(parts[2]), new Double(parts[3]), 
		  new Double(parts[4]), new Double(parts[5]));
    }
}