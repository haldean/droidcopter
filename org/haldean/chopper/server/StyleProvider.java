package org.haldean.chopper.server;

import java.awt.Color;
import java.awt.Font;

import org.haldean.simplegraph.GraphConfiguration;

public class StyleProvider {
    private static Color bg = new Color(28, 25, 20);

    private static Color fg1 = Color.white;
    private static Color fg2 = Color.lightGray;
    private static Color fg3 = Color.darkGray;

    private static Color highValue = Color.red;
    private static Color lowValue = Color.green;

    private static Font fontSmall = new Font(Font.SANS_SERIF, Font.PLAIN, 10);

    private StyleProvider() {
	;
    }

    public static Color background() {
	return bg;
    }

    public static Color foreground() {
	return foreground1();
    }

    public static Color foreground1() {
	return fg1;
    }

    public static Color foreground2() {
	return fg2;
    }

    public static Color foreground3() {
	return fg3;
    }

    public static Color highValue() {
	return highValue;
    }

    public static Color lowValue() {
	return lowValue;
    }

    public static GraphConfiguration graphFor(String title) {
	GraphConfiguration config = new GraphConfiguration(title);
	config.setAxisColor(fg2);
	config.setBackgroundColor(bg);
	config.setBorderColor(fg3);
	config.setEnableInspector(true);
	config.setEnableTickLabels(true);
	config.setInspectorColor(highValue);
	config.setLabelFont(fontSmall);
	config.setLineColor(fg1);
	return config;
    }

    /** Blend the high and low value colors
     *  <p> The colors will be blended such that a value of 1.0 is 
     *  high color, a value of 0.0 is low color, and values in between
     *  have the appropriate blend.
     *  @param val The weight given to the high value color */
    public static Color forValue(double val) {
	int red = (int) (((1.0 - val) * (double) lowValue.getRed()) + 
			 (val * (double) highValue.getRed()));
	int green = (int) (((1.0 - val) * (double) lowValue.getGreen()) +
			   (val * (double) highValue.getGreen()));
	int blue = (int) (((1.0 - val) * (double) lowValue.getBlue()) +
			  (val * (double) highValue.getBlue()));
	return new Color(red, green, blue);
    }

    public static Font fontSmall() {
	return fontSmall;
    }

    /** Get the UI font
     *  @param size The size in pixels */
    public static Font getFont(int size) {
	return getFont(size, false);
    }

    /** Get the UI font
     *  @param size The size in pixels
     *  @param bold True if bold font is desired, false if not */
    public static Font getFont(int size, boolean bold) {
	return new Font("Helvetica", (bold) ? Font.BOLD : Font.PLAIN, size);
    }
}