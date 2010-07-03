package org.haldean.chopper.server;

import javax.swing.*;
import java.awt.*;

/** A component to show the orientation in three axes
 *  of the chopper. This is a series of 2D projections
 *  unlike {@link OrientationComponent}, which uses a single
 *  3D rendering.
 *  @deprecated Use {@link OrientationComponent} instead 
 *  @author William Brown */
public class TiltComponent extends JComponent {
    /* The current orientation */
    private Orientation tilt;

    private final Color line1 = Color.white;
    private final Color line2 = Color.lightGray;
    private final Color background = new Color(28, 25, 20);

    /** Create a new TiltComponent */
    public TiltComponent() {
	tilt = null;
    }

    /** For TabPanes */
    public String getName() {
	return "Orientation";
    }

    /** Set the current tilt of the chopper
     *  @param _tilt An object representing its current orientation */
    public void setTilt(Orientation _tilt) {
	tilt = _tilt;
	repaint();
    }

    /** The canvas is split into four quadrants. 
     *  @return The shortest side length of a quarter of the canvas */
    private int getQuadrantDiameter() {
	Dimension d = getSize();
	return (int) (Math.min(d.getHeight(), d.getWidth()) / 2);
    }

    /** Get the X-component of the center of a given quadrant
     *  @param Q The quadrant number (1 is top right, 2 is top left, 3 is bottom left, 4 is bottom right) 
     *  @return The X-value of that quadrant's center point */
    private int getQuadrantCenterX(int Q) {
	int qWidth = (int) (getSize().getWidth() / 4);
	return qWidth + ((Q == 1 || Q == 4) ? 2 * qWidth : 0);
    }

    /** Get the Y-component of the center of a given quadrant
     *  @param Q The quadrant number (1 is top right, 2 is top left, 3 is bottom left, 4 is bottom right) 
     *  @return The Y-value of that quadrant's center point */
    private int getQuadrantCenterY(int Q) {
	int qHeight = (int) (getSize().getHeight() / 4);
	return qHeight + ((Q >= 3) ? 2 * qHeight : 0);
    }
    
    /** Paint the TiltComponent
     *  @param g The graphics object to pain the component to */
    public void paint(Graphics g) {
	Graphics2D g2 = (Graphics2D) g;
       
	/* Background coloring */
	g2.setColor(background);
	g2.fillRect(0, 0, (int) getSize().getWidth(), (int) getSize().getHeight());

	if (tilt == null)
	    return;

	/* Tilt drawing 
	 * The tilt drawing has two lines that are
	 * centered on the quadrant center and are always
	 * parallel. The lines are different colors so the user can
	 * tell whether the chopper is upside-down or not */
	int lineLength = (int) ((0.4 * getQuadrantDiameter()));
	int tilt_x_center = getQuadrantCenterX(2);
	int tilt_x_component = (int) (lineLength * Math.cos(tilt.getTilt(Orientation.RADIANS)));
	int tilt_y_center = getQuadrantCenterY(2);
	int tilt_y_component = (int) (lineLength * Math.sin(tilt.getTilt(Orientation.RADIANS)));
	int tilt_negate = (Math.abs(tilt.getTilt(Orientation.DEGREES)) > 90) ? -1 : 1;

	/* Draw the first line */
	g2.setColor(line1);
	g2.drawLine(tilt_x_center - tilt_x_component, tilt_y_center - tilt_y_component,
		    tilt_x_center + tilt_x_component, tilt_y_center + tilt_y_component);
	/* Draw the second line offset by a slight amount */
	g2.setColor(line2);
	g2.drawLine(tilt_x_center - tilt_x_component + tilt_negate, tilt_y_center - tilt_y_component + 2 * tilt_negate,
		    tilt_x_center + tilt_x_component + tilt_negate, tilt_y_center + tilt_y_component + 2 * tilt_negate);

	/* Pitch drawing 
	 * The pitch drawing is a single line eminating upwards out
	 * of a point located at the quadrant center */
	int pitch_x_center = getQuadrantCenterX(1);
	int pitch_x_component = (int) (lineLength * Math.sin(tilt.getPitch(Orientation.RADIANS)));
	int pitch_y_center = getQuadrantCenterY(1);
	int pitch_y_component = (int) (lineLength * Math.cos(tilt.getPitch(Orientation.RADIANS)));

	g2.setColor(line1);
	/* The line whose orientation emulates the pitch of the chopper */
	g2.drawLine(pitch_x_center, pitch_y_center,
		    pitch_x_center - pitch_x_component, pitch_y_center - pitch_y_component);
	/* The center circle */
	g2.fillOval(pitch_x_center - 2, pitch_y_center - 2, 5, 5);

	/* Roll drawing 
	 * The roll drawing looks like a compass, with a
	 * static line pointing North and large circle, and a moving
	 * "needle" that represents the actual direction of the
	 * chopper */
	int roll_x_center = getQuadrantCenterX(4);
	int roll_x_component = (int) (lineLength * Math.sin(tilt.getRoll(Orientation.RADIANS)));
	int roll_y_center = getQuadrantCenterY(4);
	int roll_y_component = (int) (lineLength * Math.cos(tilt.getRoll(Orientation.RADIANS)));

	g2.setColor(line2);
	/* The North-facing line */
	g2.drawLine(roll_x_center, roll_y_center - lineLength - 10, roll_x_center, roll_y_center);
	g2.setColor(line1);
	/* The large circle */
	g2.drawOval(roll_x_center - lineLength, roll_y_center - lineLength, lineLength * 2, lineLength * 2);
	/* The line representing the direction of the chopper */
	g2.drawLine(roll_x_center, roll_y_center,
		    roll_x_center - roll_x_component, roll_y_center - roll_y_component);
	/* The center point */
	g2.fillOval(roll_x_center - 2, roll_y_center - 2, 5, 5);
	
	/* Label drawings */
	g2.setColor(line1);
	/* Write out values for the tilt, pitch and roll in the bottom-left corner */
	g2.drawString("Tilt: " + Math.round(tilt.getTilt(Orientation.DEGREES)) + "\u00B0", 
		      2, (int) getSize().getHeight() - 30);
	g2.drawString("Pitch: " + Math.round(tilt.getPitch(Orientation.DEGREES)) + "\u00B0",
		      2, (int) getSize().getHeight() - 18);
	g2.drawString("Roll: " + Math.round(tilt.getRoll(Orientation.DEGREES)) + "\u00B0",
		      2, (int) getSize().getHeight() - 6);

	/* If it's upside-down, print out a big red scary message */
	if (Math.abs(tilt.getTilt(Orientation.DEGREES)) > 90) {
	    g2.setColor(Color.red);
	    g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 20));
	    g2.drawString("OH FUCK IT'S UPSIDE DOWN.", 2, (int) getSize().getHeight() - 45);
	}
    }
}