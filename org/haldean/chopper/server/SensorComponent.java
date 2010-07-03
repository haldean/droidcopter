package org.haldean.chopper.server;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;

/** A component to display graphs of on-board sensors 
 *  @author William Brown */
public class SensorComponent extends JPanel {
    /* Graphs and labels to display magnetic flux and temperature */
    private final GraphComponent flux;
    private final GraphComponent temp;

    private JPanel statsPanel;
    private final JLabel fluxLabel;
    private final JLabel tempLabel;

    /* Components for choosing graph scale */
    private JPanel scalePanel;
    private JSlider scaleChooser;
    private final JLabel scaleLabel;

    /* The default X-axis scale */
    private final int defaultScale = 300;

    private final Color foreground = Color.white;
    private final Color background = new Color(28, 25, 20);

    /** Create a new SensorComponent */
    public SensorComponent() {
	super(new BorderLayout());
	JPanel graphsPanel = new JPanel(new GridLayout(3,1));

	flux = new GraphComponent("Flux");
	temp = new GraphComponent("Internal Temperature");
	fluxLabel = new JLabel();
	tempLabel = new JLabel();

	statsPanel = new JPanel(new GridLayout(2,1));
	statsPanel.add(fluxLabel);
	statsPanel.add(tempLabel);

	scaleChooser = new JSlider(25, 500, defaultScale);
	scaleLabel = new JLabel(scaleChooser.getValue() + " samples");
	/* Automatically update X scales when slider is changed */
	scaleChooser.addChangeListener(new ChangeListener() {
		public void stateChanged(ChangeEvent e) {
		    setScale(scaleChooser.getValue());
		}
	    });
	setScale(defaultScale);

	scalePanel = new JPanel(new BorderLayout());
	scalePanel.add(scaleLabel, BorderLayout.EAST);
	scalePanel.add(scaleChooser, BorderLayout.CENTER);

	graphsPanel.add(flux);
	graphsPanel.add(temp);
	graphsPanel.add(statsPanel);

	add(graphsPanel, BorderLayout.CENTER);
	add(scalePanel, BorderLayout.SOUTH);
    }

    /** Used for TabPanes */
    public String getName() {
	return "Sensors";
    }

    /** Set the X-axis scales of the graphs
     *  @param s The number of samples to show along the X-axis */
    public void setScale(int s) {
	flux.setSampleCount(s);
	temp.setSampleCount(s);
	scaleLabel.setText(s + " samples");
    }

    /** Update the look and feel of the component */
    public void updateUI() {
	super.updateUI();
	if (statsPanel != null) {
	    statsPanel.updateUI();
	    fluxLabel.updateUI();
	    tempLabel.updateUI();
	    scaleLabel.updateUI();

	    scalePanel.updateUI();
	    scaleLabel.updateUI();
	    scaleChooser.updateUI();
	}
    }

    /** Add a new magnetic flux datapoint
     *  @param _f The flux in microtesla */
    public void setFlux(double _f) {
	flux.addPoint(_f);
	fluxLabel.setText("<html><b>Flux</b>: " + _f + " \u00B5T</html>");
	repaint();
    }

    /** Add a new internal temperature datapoint
     *  @param _t The internal temperature of the phone in degrees Celcius */
    public void setTemperature(double _t) {
	temp.addPoint(_t);
	tempLabel.setText("<html><b>Internal Temperature</b>: " + _t + "\u00B0 C</html>");
	repaint();
    }
}