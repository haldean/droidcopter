package org.haldean.chopper.pilot;

/**
 * Contains package constants
 * @author Benjamin Bardin
 *
 */
public interface Constants {
	
	/* Message constants */
	
	/** Message for MakePicture handler, instructing it to take a high-quality image */
	public static final int TAKE_GOOD_PIC = 100;
	
	/** Message for Comm handler, instructing it to send a status update to the server */
	public static final int STATUS_UPDATE = 101;
	
	/** Message for TransmitPicture handler, instructing it to send a frame of telemetry */
	public static final int SEND_PIC = 102;
	
	/** Message for Comm handler, instructing it to attempt to make a text connection with the server */
	public static final int MAKE_TEXT_CONN = 103;
	
	/** Message for MakePicture handler, instructing it to start taking/rendering camera preview */
	public static final int START_PREVIEW = 104;
	
	/** Message for MakePicture handler, instructing to send a list of available preview sizes to the server */
	public static final int SEND_SIZES = 105;
	
	/** Message for Navigation handler, instructing it to calculate the desired velocity vector */
	public static final int EVAL_NAV = 106;
	
	/** Message for Comm handler, instructing it to attempt to make a data (telemetry) connection with the server */
	public static final int MAKE_DATA_CONN = 107;
	
	/** Message for Guidance handler, instructing it to revise motor speeds based on current status */
	public static final int EVAL_MOTOR_SPEED = 108;
	
	
	/* Sensor indeces */
	
	/** Used by internal data structures as an index for the chopper's azimuth, related data and objects */
	public final static int AZIMUTH = 0;
	
	/** Used by internal data structures as an index for the chopper's pitch, related data and objects */
	public final static int PITCH = 1;
	
	/** Used by internal data structures as an index for the chopper's roll, related data and objects */
	public final static int ROLL = 2;
	
	/** Used by internal data structures as an index for the chopper's acceleration in the X direction, related data and objects */
	public final static int X_ACCEL = 3;
	
	/** Used by internal data structures as an index for the chopper's acceleration in the Y direction, related data and objects */
	public final static int Y_ACCEL = 4;
	
	/** Used by internal data structures as an index for the chopper's acceleration in the Z direction, related data and objects */
	public final static int Z_ACCEL = 5;
	
	/** Used by internal data structures as an index for the chopper's magnetic flux in the X direction, related data and objects */
	public final static int X_FLUX = 6; //microT
	
	/** Used by internal data structures as an index for the chopper's magnetic flux in the Y direction, related data and objects */
	public final static int Y_FLUX = 7;
	
	/** Used by internal data structures as an index for the chopper's magnetic flux in the Z direction, related data and objects */
	public final static int Z_FLUX = 8;
	
	/** Used by internal data structures as an index for the chopper's pressure, related data and objects */
	public final static int PRESSURE = 9;
	
	/** Used by internal data structures as an index for the chopper's internal temperature, related data and objects */
	public final static int TEMPERATURE = 10;
	
	/** Used by internal data structures as an index for the chopper's light reading, related data and objects */
	public final static int LIGHT = 11;
	
	/** Used by internal data structures as an index for the chopper's proximity reading, related data and objects */
	public final static int PROXIMITY = 12;
	
	/** Total number of sensor fields */
	public final static int SENSORS = 13;
	
	
	/* GPS indeces */
	
	/** Used by internal data structures as an index for the chopper's GPS altitude reading, related data and objects */
	public final static int ALTITUDE = 0;
	
	/** Used by internal data structures as an index for the chopper's GPS bearing reading, related data and objects */
	public final static int BEARING = 1;
	
	/** Used by internal data structures as an index for the chopper's GPS longitude reading, related data and objects */
	public final static int LONG = 2;
	
	/** Used by internal data structures as an index for the chopper's GPS latitude reading, related data and objects */
	public final static int LAT = 3;
	
	/** Used by internal data structures as an index for the chopper's GPS speed reading, related data and objects */
	public final static int SPEED = 4;
	
	/** Used by internal data structures as an index for the chopper's approximate change in altitude (based on GPS), related data and objects */
	public final static int dALT = 5;
	
	/** Total number of GPS fields */
	public final static int GPS_FIELDS = 6;
	/* gpsaccuracy, gpstimestamp, gpsnumsats are not listed here! */
	
	
	/* Nav statuses: */
	
	/** Nav status indicating low battery */
	public final static int LOW_POWER = 0;
	
	/** Nav status indicating standard autopilot */
	public final static int BASIC_AUTO = 1;
	
	/** Nav status indicating surprise loss of connectivity */
	public final static int NO_CONN = 2;
	
	/** Number of available Nav statuses. */
	public final static int NAV_STATUSES = 3;
	
	
	/* Server message categories */
	
	/** Indicates Image-related message from server */
	public final static int IMAGE = 0;
	
	/** Indicates Comm-related message from server */
	public final static int COMM = 1;
	
	/** Indicates Nav-related message from server */
	public final static int NAV = 2;
	
	/** Indicates chopper-system-wide message */
	public final static int CSYS = 3;
	
	/** Number of inbound message types. */
	public final static int MSG_TYPES = 4;
}
