package org.haldean.chopper.server;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 *  An interface to send new tuning parameters to the helicopter.
 *
 *  @author William Brown
 */
public class PidTuningComponent extends JFrame {
    MotorTuning[] motors = new MotorTuning[4];
    private static PidTuningComponent instance = new PidTuningComponent();

    private PidTuningComponent() {
	super("PID Tuning Parameters");
	for (int i=0; i<4; i++)
	    motors[i] = new MotorTuning(i);

	JPanel motorPanel = new JPanel(new GridLayout(3, 3, 10, 10));
	for (int i=0; i<9; i++) {
	    if ((i - 1) % 2 == 0) {
		motorPanel.add(motors[(int) (i - 1) / 2]);
	    } else if (i == 6) {
		motorPanel.add(cancelPanel());
	    } else if (i == 8) {
		motorPanel.add(applyPanel());
	    } else {
		motorPanel.add(new JPanel());
	    }
	}

	motorPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
	add(motorPanel);

	setPreferredSize(new Dimension(400,340));
	pack();
    }

    private void apply() {
	for (int i=0; i<4; i++) {
	    motors[i].sendNewParameters();
	}
    }

    private void close() {
	setVisible(false);
    }

    private JPanel cancelPanel() {
	JPanel cancelPanel = new JPanel(new GridLayout(3, 1, 0, 10));
	cancelPanel.add(new JPanel());

	JButton cancelButton = new JButton("Cancel");
	cancelButton.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    close();
		}
	    });
	cancelPanel.add(cancelButton, BorderLayout.CENTER);
	return cancelPanel;
    }

    private JPanel applyPanel() {
	JPanel applyPanel = new JPanel(new GridLayout(3, 1, 0, 10));
	applyPanel.add(new JPanel());

	JButton applyButton = new JButton("Apply");
	applyButton.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    apply();
		}
	    });
	applyPanel.add(applyButton, BorderLayout.CENTER);
	return applyPanel;
    }
	
    private class MotorTuning extends JPanel {
	JTextField pValue = new JTextField("0");
	JTextField iValue = new JTextField("0");
	JTextField dValue = new JTextField("0");
	int motorIndex;

	public MotorTuning(int motor) {
	    setLayout(new GridLayout(3, 1, 0, 10));
	    motorIndex = motor;
	    
	    JPanel pPair = new JPanel();
	    pPair.setLayout(new BoxLayout(pPair, BoxLayout.X_AXIS));
	    pPair.add(label("P:"));
	    pPair.add(pValue);
	    add(pPair);

	    JPanel iPair = new JPanel();
	    iPair.setLayout(new BoxLayout(iPair, BoxLayout.X_AXIS));
	    iPair.add(label("I:"));
	    iPair.add(iValue);
	    add(iPair);

	    JPanel dPair = new JPanel();
	    dPair.setLayout(new BoxLayout(dPair, BoxLayout.X_AXIS));
	    dPair.add(label("D:"));
	    dPair.add(dValue);
	    add(dPair);
	}

	private JLabel label(String s) {
	    JLabel l = new JLabel(s);
	    l.setPreferredSize(new Dimension(30,30));
	    return l;
	}
	
	public void sendNewParameters() {
	    try {
		double p = new Double(pValue.getText());
		double i = new Double(iValue.getText());
		double d = new Double(dValue.getText());

		Navigator.tunePid(motorIndex, 0, p);
		Navigator.tunePid(motorIndex, 1, i);
		Navigator.tunePid(motorIndex, 2, d);
	    } catch (NumberFormatException e) {
		e.printStackTrace();
	    }
	}
    }

    public static void activate() {
	instance.setVisible(true);
    }
}