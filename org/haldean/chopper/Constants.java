package org.haldean.chopper;

/**
 * Contains package constants
 * @author Benjamin Bardin
 *
 */
public interface Constants {
	
	/**
	 * Message for MakePicture handler, instructing it to take a high-quality image
	 */
	public static final int TAKEGOODPIC = 100;
	
	/**
	 * Message for Comm handler, instructing it to send a status update to the server
	 */
	public static final int SENDSTATUSUPDATE = 101;
	
	/**
	 * Message for TransmitPicture handler, instructing it to send a frame of telemetry
	 */
	public static final int SENDAPIC = 102;
	
	/**
	 * Message for Comm handler, instructing it to attempt to make a text connection with the server
	 */
	public static final int MAKETEXTCONN = 103;
	
	/**
	 * Message for MakePicture handler, instructing it to start taking/rendering camera preview
	 */
	public static final int STARTPREVIEW = 104;
	
	/**
	 * Message for MakePicture handler, instructing to send a list of available preview sizes to the server
	 */
	public static final int SENDSIZES = 105;
	
	/**
	 * Message for Navigation handler, instructing it to calculate the desired velocity vector
	 */
	public static final int EVALNAV = 106;
	
	/**
	 * Message for Comm handler, instructing it to attempt to make a data (telemetry) connection with the server
	 */
	public static final int MAKEDATACONN = 107;
	
	/**
	 * Message for Guidance handler, instructing it to revise motor speeds based on current status
	 */
	public static final int EVALMOTORSPEED = 108;
	
	/**
	 * Used by internal data structures as an index for the chopper's azimuth, related data and objects
	 */
	public final static int AZIMUTH = 0;
	
	/**
	 * Used by internal data structures as an index for the chopper's pitch, related data and objects
	 */
	public final static int PITCH = 1;
	
	/**
	 * Used by internal data structures as an index for the chopper's roll, related data and objects
	 */
	public final static int ROLL = 2;
	
	/**
	 * Used by internal data structures as an index for the chopper's acceleration in the X direction, related data and objects
	 */
	public final static int XACCEL = 3;
	
	/**
	 * Used by internal data structures as an index for the chopper's acceleration in the Y direction, related data and objects
	 */
	public final static int YACCEL = 4;
	
	/**
	 * Used by internal data structures as an index for the chopper's acceleration in the Z direction, related data and objects
	 */
	public final static int ZACCEL = 5;
	
	/**
	 * Used by internal data structures as an index for the chopper's magnetic flux in the X direction, related data and objects
	 */
	public final static int MAG1 = 6; //microT
	
	/**
	 * Used by internal data structures as an index for the chopper's magnetic flux in the Y direction, related data and objects
	 */
	public final static int MAG2 = 7;
	
	/**
	 * Used by internal data structures as an index for the chopper's magnetic flux in the Z direction, related data and objects
	 */
	public final static int MAG3 = 8;
	
	/**
	 * Used by internal data structures as an index for the chopper's pressure, related data and objects
	 */
	public final static int PRESSURE = 9;
	
	/**
	 * Used by internal data structures as an index for the chopper's internal temperature, related data and objects
	 */
	public final static int TEMPERATURE = 10;
	
	/**
	 * Used by internal data structures as an index for the chopper's light reading, related data and objects
	 */
	public final static int LIGHT = 11;
	
	/**
	 * Used by internal data structures as an index for the chopper's proximity reading, related data and objects
	 */
	public final static int PROXIMITY = 12;
	
	/**
	 * Total number of sensor fields
	 */
	public final static int NUMSENSORS = 13;
	
	/**
	 * Used by internal data structures as an index for the chopper's GPS altitude reading, related data and objects
	 */
	public final static int ALTITUDE = 0;
	
	/**
	 * Used by internal data structures as an index for the chopper's GPS bearing reading, related data and objects
	 */
	public final static int BEARING = 1;
	
	/**
	 * Used by internal data structures as an index for the chopper's GPS longitude reading, related data and objects
	 */
	public final static int LONG = 2;
	
	/**
	 * Used by internal data structures as an index for the chopper's GPS latitude reading, related data and objects
	 */
	public final static int LAT = 3;
	
	/**
	 * Used by internal data structures as an index for the chopper's GPS speed reading, related data and objects
	 */
	public final static int SPEED = 4;
	
	/**
	 * Used by internal data structures as an index for the chopper's approximate change in altitude (based on GPS), related data and objects
	 */
	public final static int dALT = 5;
	
	/**
	 * Total number of GPS fields
	 */
	public final static int GPSFIELDS = 6;
	/* gpsaccuracy, gpstimestamp, gpsnumsats are not listed here! */
	
	/* Nav statuses: */
	/**
	 * Nav status indicating low battery
	 */
	public final static int LOWPOWER = 0;
	
	/**
	 * Nav status indicating standard autopilot
	 */
	public final static int BASICAUTO = 1;
	
	/**
	 * Nav status indicating surprise loss of connectivity
	 */
	public final static int NOCONN = 2;
	
	/**
	 * Number of available Nav statuses.
	 */
	public final static int NUMNAVSTATUSES = 3;
	
	/**
	 * Maximum velocity Guidance will try to attain
	 */
	public final static double MAXVEL = 2.0;
	
	/**
	 * Maximum battery level considered "low"
	 */
	public final static int LOWBATT = 30;
}
