package org.haldean.chopper.server;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.HashMap;

import org.haldean.chopper.server.StyleProvider;

public class MotorComponent extends JPanel {
    private MotorDisplay display;
    private final int MOTOR_MAX = 1023;
    private final int MOTOR_MIN = 0;
    private HashMap<Motor, MotorController> motorControllers;
    private JPanel controlPanel;
    private JButton applyButton;

    public enum Motor {
	XPOS, YPOS, XNEG, YNEG;
    }

    public MotorComponent() {
	super(new BorderLayout());
	motorControllers = new HashMap<Motor, MotorController>();

	display = new MotorDisplay();
	add(display, BorderLayout.CENTER);

	controlPanel = new JPanel(new GridLayout(2, 3, 10, 10));
	for (Motor m : Motor.values()) {
	    controlPanel.add(new MotorController(m));
	}

	applyButton = new JButton("Update");
	applyButton.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    updateSpeeds();
		    applyButton.setEnabled(false);
		}
	    });
	controlPanel.add(applyButton, 2);
	controlPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
	add(controlPanel, BorderLayout.SOUTH);

	updateUI();
    }

    public String getName() {
	return "Motor Speeds";
    }

    public void updateUI() {
	super.updateUI();
	if (controlPanel != null) {
	    applyButton.updateUI();
	    controlPanel.updateUI();
	    for(MotorController m : motorControllers.values()) {
		m.updateUI();
	    }
	}
    }

    private void updateSpeeds() {
	double[] speeds = { 
	    motorControllers.get(Motor.YPOS).getSpeed(), 
	    motorControllers.get(Motor.YNEG).getSpeed(),
	    motorControllers.get(Motor.XPOS).getSpeed(), 
	    motorControllers.get(Motor.XNEG).getSpeed()
	};
	Navigator.setMotorSpeeds(speeds);
    }

    private class MotorController extends JPanel {
	private Motor m;
	private JSlider slider;
	private JLabel label;

	public MotorController(Motor m) {
	    this.m = m;
	    motorControllers.put(m, this);

	    slider = new JSlider(MOTOR_MIN, MOTOR_MAX, 0);
	    slider.addChangeListener(new ChangeListener() {
		    public void stateChanged(ChangeEvent e) {
			applyButton.setEnabled(true);
		    }
		});

	    label = new JLabel(m.toString());
	    label.setLabelFor(slider);

	    setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
	    add(label);
	    add(slider);
	}

	public int getSpeed() {
	    return slider.getValue();
	}

	public void updateUI() {
	    super.updateUI();
	    if (slider != null) slider.updateUI();
	    if (label != null) label.updateUI();
	}
    }

    private class MotorDisplay extends JComponent {
	private final int motorRadius = 20;
	private final int motorCount = 4;
	private float[] motorSpeeds;

	private Color highSpeedColor = StyleProvider.highValue();
	private Color lowSpeedColor = StyleProvider.lowValue();
	private Color backgroundColor = StyleProvider.background();
	private Color chopperColor = StyleProvider.foreground3();
	private Color labelColor = StyleProvider.background();

	public MotorDisplay() {
	    motorSpeeds = new float[motorCount];
	    setPreferredSize(new Dimension(300, 300));
	}

	public void setMotorSpeed(Motor motor, float speed) {
	    if (speed < MOTOR_MIN || speed > MOTOR_MAX)
		return;
	    motorSpeeds[motor.ordinal()] = speed;
	    repaint();
	}

	private void motorCircle(Graphics2D g2, Motor m, int width, int height) {
	    int x = 0, y = 0;
	    switch (m) {
	    case XPOS:
		x = width - motorRadius * 2 - 30;
		y = height / 2 - motorRadius;
		break;
	    case YPOS:
		x = width / 2 - motorRadius;
		y = 30;
		break;
	    case XNEG:
		x = 30;
		y = height / 2 - motorRadius;
		break;
	    case YNEG:
		x = width / 2 - motorRadius;
		y = height - motorRadius * 2 - 30;
		break;
	    }

	    g2.setColor(StyleProvider.forValue((MOTOR_MAX - motorSpeeds[m.ordinal()]) / MOTOR_MAX));
	    g2.fillOval(x, y, motorRadius * 2, motorRadius * 2);

	    g2.setColor(labelColor);
	    String label = new Integer((int) (motorSpeeds[m.ordinal()] * 100)).toString();
	    g2.drawString(m.toString(), (int) (x + motorRadius - 3 - (4 * (m.toString().length() - 1))),
			  y + (int) (1.2 * motorRadius) - 3);
	    g2.drawString(label, (int) (x + motorRadius - 3 - (4 * (label.length() - 1))),
			  y + (int) (1.2 * motorRadius) + 8);
	}

	@Override public void paintComponent(Graphics g) {
	    Graphics2D g2 = (Graphics2D) g;
	    int width = (int) getSize().getWidth();
	    int height = (int) getSize().getHeight();

	    g2.setColor(backgroundColor);
	    g2.fillRect(0, 0, width, height);

	    g2.setColor(chopperColor);
	    g2.setFont(StyleProvider.fontSmall());
	    /* Vertical chopper line */
	    g2.fillRect(width/2 - 2, 20, 4, height - 40);
	    /* Horizontal chopper line */
	    g2.fillRect(20, height/2 - 2, width - 40, 4);

	    for (Motor m : Motor.values())
		motorCircle(g2, m, width, height);
	}
    }
}