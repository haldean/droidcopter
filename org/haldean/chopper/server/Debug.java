package org.haldean.chopper.server;

import java.io.IOException;
import java.io.Writer;

/** 
 * A class to write debug information to an updatable,
 * and optionally to write it to standard error 
 *
 * @author William Brown
 */
public class Debug {
    /** 
     * Set to true to enable debugging, or false to supress 
     * output (defaults to false)
     */
    private static boolean enable = false;
    private static Updatable debugOut;
    private static Writer writer;

    private Debug() {
	;
    }

    /**
     * Set the output stream to write logs to.
     */
    public static void setOutputStream(Writer writer) {
	Debug.writer = writer;
    }

    /** 
     * Enable or disable debugging
     */
    public static void setEnabled(boolean e) {
	enable = e;
    }

    /** 
     * Log a debug message
     *
     * @param s The message to debug
     */
    public static void log(String s) {
	/* Send it to standard error if enabled */
	if (enable)
	    System.err.println(s);
	/* Send it to the updatable if set */
	if (writer != null) {
	    try {
		writer.write(s);
	    } catch (IOException e) {
		System.err.println("Failed to write to debug log.");
	    }
	}
    }
}