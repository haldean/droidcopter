package org.haldean.chopper.server;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/** A component to display the status of various mission-critical
 *  system components */
public class StatusLabel extends JPanel implements Updatable {
    private boolean globeMode;
    private boolean isConnected;
    private boolean isReceiving;
    private double battery;

    private Timer receiptTimer;
    private final int receivingDelay = 2000;

    private JLabel globeModeLabel;
    private JLabel connectedLabel;
    private JLabel receivingLabel;
    private JLabel batteryLabel;

    private final Color accept = Color.GREEN;
    private final Color reject = Color.RED;

    /** Create a new status label with default state */
    public StatusLabel() {
	super(new GridLayout(1, 4));

	globeModeLabel = new JLabel();
	globeModeLabel.setOpaque(true);
	globeModeLabel.setHorizontalAlignment(SwingConstants.CENTER);

	connectedLabel = new JLabel();
	connectedLabel.setOpaque(true);
	connectedLabel.setHorizontalAlignment(SwingConstants.CENTER);

	receivingLabel = new JLabel();
	receivingLabel.setOpaque(true);
	receivingLabel.setHorizontalAlignment(SwingConstants.CENTER);

	batteryLabel = new JLabel("Battery Unknown");
	batteryLabel.setOpaque(true);
	batteryLabel.setHorizontalAlignment(SwingConstants.CENTER);

	add(connectedLabel);
	add(receivingLabel);
	add(batteryLabel);
	add(globeModeLabel);
	
	setGlobeMode(false);
	setConnected(false);
	setReceiving(false);

    }

    /** Set the globe mode state of the UI
     *  @param _globeMode True if a pad is controlling the globe, false if not */
    public void setGlobeMode(boolean _globeMode) {
	globeMode = _globeMode;
	if (globeMode) {
	    globeModeLabel.setText("Globe Mode ON");
	    globeModeLabel.setBackground(reject);
	} else {
	    globeModeLabel.setText("Globe Mode OFF");
	    globeModeLabel.setBackground(accept);
	}
    }

    /** Set the connected state of the Data Receiver
     *  @param _isConnected True if connected to the server, false if not */
    public void setConnected(boolean _isConnected) {
	isConnected = _isConnected;
	if (isConnected) {
	    connectedLabel.setText("CONNECTED");
	    connectedLabel.setBackground(accept);
	} else {
	    connectedLabel.setText("NOT CONNECTED");
	    connectedLabel.setBackground(reject);
	}
    }

    /** Set the receiving status of the Data Receiver
     *  @param _isReceiving True if receiving data from chopper, false if not */
    public void setReceiving(boolean _isReceiving) {
	isReceiving = _isReceiving;
	if (isReceiving) {
	    receivingLabel.setText("RECEIVING");
	    receivingLabel.setBackground(accept);
	} else {
	    receivingLabel.setText("NOT RECEIVING");
	    receivingLabel.setBackground(reject);
	}
    }

    /** Set the displayed battery percentage of the phone. Show warning
     *  if percentage is less than 30%
     *  @param _battery The current battery level of the chopper */
    public void setBattery(double _battery) {
	battery = _battery;
	batteryLabel.setText("Battery at " + (int) (battery * 100) + "%");
	if (battery <= 0.3)
	    batteryLabel.setBackground(reject);
	else 
	    batteryLabel.setBackground(accept);
    }

    /** Listen for not-receiving signals 
     *  @param s SYS:RECEIVING:NO if not receiving, anything else if it is */
    public void update(String s) {
	if (s.equals("SYS:RECEIVING:NO"))
	    setReceiving(false);
	else {
	    setReceiving(true);
	    if (s.startsWith("BATTERY")) {
		String parts[] = s.split(":");
		setBattery(new Double(parts[1]));
	    }
	}
    }
}