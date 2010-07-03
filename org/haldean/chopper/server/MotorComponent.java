package org.haldean.chopper.harness;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JComponent;
import javax.swing.JFrame;

import org.haldean.chopper.server.StyleProvider;

public class MotorComponent extends JComponent {
    private final int motorCount = 4;
    private final int motorRadius = 20;
    private float[] motorSpeeds;

    private Color highSpeedColor = StyleProvider.highValue();
    private Color lowSpeedColor = StyleProvider.lowValue();
    private Color backgroundColor = StyleProvider.background();
    private Color chopperColor = StyleProvider.foreground3();
    private Color labelColor = StyleProvider.background();

    public enum Motor {
	XPOS, YPOS, XNEG, YNEG;
    }

    public MotorComponent() {
	motorSpeeds = new float[motorCount];
	setPreferredSize(new Dimension(300, 300));
    }

    public void setMotorSpeed(Motor motor, float speed) {
	if (speed < 0 || speed > 1)
	    return;
	motorSpeeds[motor.ordinal()] = speed;
	repaint();
    }

    private void motorCircle(Graphics2D g2, Motor m, int width, int height) {
	int x = 0, y = 0;
	switch (m) {
	case XPOS:
	    x = width - motorRadius * 2 - 10;
	    y = height / 2 - motorRadius;
	    break;
	case YPOS:
	    x = width / 2 - motorRadius;
	    y = 10;
	    break;
	case XNEG:
	    x = 10;
	    y = height / 2 - motorRadius;
	    break;
	case YNEG:
	    x = width / 2 - motorRadius;
	    y = height - motorRadius * 2 - 10;
	    break;
	}

	g2.setColor(StyleProvider.forValue(1 - motorSpeeds[m.ordinal()]));
	g2.fillOval(x, y, motorRadius * 2, motorRadius * 2);

	g2.setColor(labelColor);
	String label = new Integer((int) (motorSpeeds[m.ordinal()] * 100)).toString();
	g2.drawString(label, (int) (x + motorRadius - 3 - (4 * (label.length() - 1))),
		      y + (int) (1.2 * motorRadius));
    }

    @Override public void paintComponent(Graphics g) {
	Graphics2D g2 = (Graphics2D) g;
	int width = (int) getSize().getWidth();
	int height = (int) getSize().getHeight();

	g2.setColor(backgroundColor);
	g2.fillRect(0, 0, width, height);

	g2.setColor(chopperColor);
	/* Vertical chopper line */
	g2.fillRect(width/2 - 2, 20, 4, height - 40);
	/* Horizontal chopper line */
	g2.fillRect(20, height/2 - 2, width - 40, 4);

	for (Motor m : Motor.values())
	    motorCircle(g2, m, width, height);
    }

    public static void main(String[] args) {
	MotorComponent m = new MotorComponent();
	JFrame f = new JFrame();
	f.add(m);
	f.pack();
	f.setVisible(true);

	try {
	    float i;
	    m.setMotorSpeed(MotorComponent.Motor.YNEG, 1);
	    while (true) {
		for (Motor motor : MotorComponent.Motor.values()) {
		    for (i=0; i <= 1; i += 0.01) {
			m.setMotorSpeed(motor, i);
			Thread.sleep(10);
		    }
		    for (; i >= 0; i -= 0.01) {
			m.setMotorSpeed(motor, i);
			Thread.sleep(10);
		    }
		}
	    }
	} catch (Exception e) {
	    ;
	}
    }
}