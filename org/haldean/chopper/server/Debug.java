package org.haldean.chopper.server;

/** A class to write debug information to an updatable,
 *  and optionally to write it to standard error 
 *  @author William Brown */
public class Debug {
    /** Set to true to enable debugging, or false to supress 
     *  output (defaults to false) */
    private static boolean enable = false;
    private static Updatable debugOut;

    /** This class cannot be instantiated -- it can only
     *  be accessed in a static context */
    private Debug() {
	;
    }

    /** Set the component to update with debug messages
     *  @param d The component to update when a debug message is received */
    public static void setDebugOut(Updatable d) {
	debugOut = d;
    }

    /** Enable or disable debugging */
    public static void setEnabled(boolean e) {
	enable = e;
    }

    /** Log a debug message
     *  @param s The message to debug */
    public static void log(String s) {
	/* Send it to standard error if enabled */
	if (enable)
	    System.err.println(s);
	/* Send it to the updatable if set */
	if (debugOut != null)
	    debugOut.update(s);
    }
}