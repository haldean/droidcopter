package org.haldean.chopper;

public interface Constants
{
	public static final String control = new String("pices.dynalias.org");
	public static final int textoutport = 23; //port on the server to connect to for text
	public static final int dataoutport = 24; //ditto, but for images.
	
	public static final int XPIC = 1280; //Dimension of pictures.
	public static final int YPIC = 960;
	
	public static final int FPS = 5;
	
	
	public static final int INITIALJPEGQ = 50;
	public static final int INITIALPREVQ = 45;
	
	public static final int TAKEGOODPIC = 100;
	public static final int SENDSTATUSUPDATE = 101;
	public static final int SENDAPIC = 102;
	public static final int MAKECONNECTION = 103;
	public static final int STARTPREVIEW = 104;
	public static final int SENDSIZES = 105;
	public static final int EVALNAV = 106;
	
	public static final int UPDATEINTERVAL = 200; //for sending status report, millis
	public static final int CAMERAINTERVAL = 200; //How much the camera should sleep between shots.
	public static final int PICTUREUPDATEINTERVAL = 50;
	public static final int CONNECTIONINTERVAL = 5000;
	public static final int PULSERATE = 3000;
	public static final int FIRSTPULSE = 20000;
	public static final int HOVERPAUSE = 10000;
	
	public static final float gpsmindist = 0; //minimum distance upon which GPS will update itself. Lower values increase accuracy/reliability, decrease battery.
	public static final long gpsmintime = 0; //minimum time upon which GPS will update itself. Ditto.
	
	//Guidance constants
	public static final float MAXANGLE = 20;
	public static final float MAXD = .1F; //Must be positive
	public static final float REALLYBIG = 10000;
	public static int PIDREPS = 10;
	
	//degrees
	public final static int AZIMUTH = 0;
	public final static int PITCH = 1;
	public final static int ROLL = 2;
	
	//m/s^2:
	public final static int XACCEL = 3;
	public final static int YACCEL = 4;
	public final static int ZACCEL = 5;
	
	public final static int MAG1 = 6; //microT
	public final static int MAG2 = 7;
	public final static int MAG3 = 8;
	
	public final static int PRESSURE = 9;
	public final static int TEMPERATURE = 10;
	
	public final static int LIGHT = 11; //lux
	public final static int PROXIMITY = 12; //cm
	
	public final static int NUMSENSORS = 13;
	
	//gps data array is separate
	public final static int ALTITUDE = 0;
	public final static int BEARING = 1;
	public final static int LONG = 2;
	public final static int LAT = 3;
	public final static int SPEED = 4;
	public final static int dALT = 5;
	
	public final static int GPSFIELDS = 6;
	//gpsaccuracy, gpstimestamp, gpsnumsats are not listed here!
	
	//Nav statuses:
	public final static int LOWPOWER = 0;
	public final static int BASICAUTO = 1;
	public final static int NOCONN = 2;
	
	public final static int LOWBATT = 30;//percent
}
