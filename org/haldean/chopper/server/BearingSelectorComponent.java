package org.haldean.chopper.server;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class BearingSelectorComponent extends UpdateUiPanel {
    private final BearingSelector selector;
    private PadController controller;

    public BearingSelectorComponent() {
	super(new BorderLayout());

	add(new JLabel("Select a new heading using the left stick"), 
	    BorderLayout.NORTH);

	selector = new BearingSelector();
	add(selector, BorderLayout.CENTER);

	controller = ServerCreator.getServerHost().getController(PadController.class);
    }

    private void setBearing() {
	float x = controller.getAxis(PadController.AXIS_L_H);
	float y = controller.getAxis(PadController.AXIS_L_V);
	selector.bearing = Math.atan2(y, x);
    }

    private class BearingSelector extends JComponent {
	private double bearing;

	public BearingSelector() {
	    bearing = Math.toRadians(45);
	}

	public void paintComponent(Graphics g) {
	    Graphics2D g2 = (Graphics2D) g;

	    int width = (int) getSize().getWidth();
	    int height = (int) getSize().getHeight();
	    int center_x = width / 2;
	    int center_y = height / 2;
	    int radius = (int) (0.4 * Math.min(height, width));
	    
	    g2.setColor(StyleProvider.background());
	    g2.fillRect(0, 0, width, height);

	    g2.setColor(StyleProvider.foreground());
	    g2.drawLine(center_x, center_y, 
			center_x + (int) (radius * Math.sin(bearing)),
			center_y - (int) (radius * Math.cos(bearing)));

	    g2.setColor(StyleProvider.foreground3());
	    g2.drawOval(center_x - radius, center_y - radius,
			radius * 2, radius * 2);
	    g2.fillOval(center_x - 2, center_y - 2, 4, 4);
	}
    }

    public static void main(String[] args) {
	JFrame f = new JFrame();
	f.add(new BearingSelectorComponent());
	f.pack();
	f.setVisible(true);
    }
}