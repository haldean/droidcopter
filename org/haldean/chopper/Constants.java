package org.haldean.chopper;

public interface Constants
{
	public static final String control = new String("pices.dynalias.org");
	public static final int textoutport = 23; //port on the server to connect to for text
	public static final int dataoutport = 24; //ditto, but for images.
	
	public static final int XPIC = 1280; //Dimension of pictures.
	public static final int YPIC = 960;
	public static final int XPREV = 320;
	public static final int YPREV = 240;
	public static final int FPS = 5;
	
	public static final int INITIALJPEGQ = 50;
	public static final int JPEGQUALITY = 45;
	
	public static final int TAKEGOODPIC = 100;
	public static final int SENDSTATUSUPDATE = 101;
	public static final int SENDAPIC = 102;
	public static final int MAKECONNECTION = 103;
	public static final int STARTPREVIEW = 104;
	
	public static final int UPDATEINTERVAL = 100; //for sending status report, millis
	public static final int CAMERAINTERVAL = 10; //How much the camera should sleep between shots.
	public static final int PICTUREUPDATEINTERVAL = 50;
	public static final int CONNECTIONINTERVAL = 5000;
	
	
	public static final float gpsmindist = 0; //minimum distance upon which GPS will update itself. Lower values increase accuracy/reliability, decrease battery.
	public static final long gpsmintime = 0; //minimum time upon which GPS will update itself. Ditto.
	
	
	//degrees
	public final static int AZIMUTH = 0;
	public final static int PITCH = 1;
	public final static int ROLL = 2;
	
	//m/s^2:
	public final static int XACCEL = 3;
	public final static int YACCEL = 4;
	public final static int ZACCEL = 5;
	
	public final static int FLUX = 6; //microT
	
	public final static int LIGHT = 7; //lux
	
	public final static int PROXIMITY = 8; //cm
	
	public final static int PRESSURE = 9;
	public final static int TEMPERATURE = 10;
	
	public final static int NUMSENSORS = 11;
	
	//gps data array is separate
	public final static int ALTITUDE = 0;
	public final static int BEARING = 1;
	public final static int LONG = 2;
	public final static int LAT = 3;
	public final static int SPEED = 4;
	
	public final static int GPSFIELDS = 5;
	//gpsaccuracy, gpstimestamp, gpsnumsats are not listed here!
	
	public static final int XVEL = 0;
	public static final int YVEL = 1;
	public static final int ZVEL = 2;
	
	public static final int VELFIELDS = 3;
}
