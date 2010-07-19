package org.haldean.chopper;

import java.util.concurrent.locks.ReentrantLock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

/**
 * Central "storehouse" for information about the chopper's status--maintains updated sensor readings, gps readings, etc.
 * @author Benjamin Bardin
 */
public final class ChopperStatus extends Thread implements SensorEventListener, Constants, LocationListener
{	
	/**
	 * How often (in ms) status updates should be sent by ChopperStatus to the server
	 */
	public static final int UPDATEINTERVAL = 500;
	
	/**
	 * Parameter to specify GPS minimum update distance, with the usual trade-off between accuracy and power consumption.
	 * Value of '0' means maximum accuracy.
	 */
	public static final float gpsmindist = 0;
	
	/**
	 * Parameter to specify GPS minimum update time, with the usual trade-off between accuracy and power consumption.
	 * Value of '0' means maximum accuracy.
	 */
	public static final long gpsmintime = 0;
	
	/**
	 * Holds data from various sensors, like gyroscope, acceleration and magnetic flux.  Each read/write must be locked with its corresponding lock in readingLock[].
	 * @see Constants Sensor Data Used
	 * @see #readingLock readingLock[]
	 */
	public static double[] reading = new double[NUMSENSORS]; //last known data for a given sensor
	
	/**
	 * Locks for fields in reading[].  A corresponding lock must be externally obtained for each read/write to a field in reading[].
	 * @see #reading reading[] 
	 */
	public static ReentrantLock[] readingLock;
	
	/* Timestamp of last write to corresponding field in reading[]. */
	private static long[] timestamp = new long[NUMSENSORS]; //timestamp in nanos of last reading
	
	/**
	 * Holds GPS data.  Each read/write must be locked with its corresponding lock in gpsLock[]
	 * @see Constants GPS Data Used
	 * @see #gpsLock gpsLock[]
	 */
	public static double[] gps = new double[GPSFIELDS]; //last available GPS readings
	
	/**
	 * Locks for fields in gps[].  A corresponding lock must be externally obtained for each read/write to a field in gps[].
	 * @see #gps gps[] 
	 */
	public static ReentrantLock[] gpsLock;
	
	/**
	 * Stores the location object last returned by the GPS.  Must be externally synchronized.
	 */
	public static Location lastLoc;
	
	/**
	 * Stores the speeds last submitted to the motors.  Lock must be externally obtained for each read/write to motorspeed.
	 * @see #motorLock motorLock
	 */
	public static double[] motorspeed = new double[4];
	
	/**
	 * Lock for motorspeed[].  Must be externally obtained.
	 * @see #motorspeed motorspeed[]
	 */
	public static ReentrantLock motorLock;
	
	/**
	 * Current battery level.
	 */
	public static int currbattery = 0;
	
	/**
	 * Max battery level.
	 */
	public static int maxbattery = 100;
	
	/* Accuracy of last GPS reading.  This field is only accessed by one thread, so no lock is needed. */
	private static float gpsaccuracy; //accuracy of said reading
	
	/* Timestamp of last GPS reading.  This field is only accessed by one thread, so no lock is needed. */
	private static long gpstimestamp; //timestamp of the reading
	
	/* Number of satellites used to obtain last GPS reading.  This field is only accessed by one thread, so no lock is needed. */
	private static int gpsnumsats; //number of satellites used to collect last reading
	
	/* Used to obtain location manager. */
	private static Context context;
	
	/* Handles messages */
	private static Handler handler;
	
	/**
	 * Initializes the locks
	 * @param mycontext Application context
	 */
	public ChopperStatus(Context mycontext)	{
		super("Chopper Status");
		context = mycontext;
		
		//Initialize the data locks
		readingLock = new ReentrantLock[NUMSENSORS];
		for (int i = 0; i < NUMSENSORS; i++)
			readingLock[i] = new ReentrantLock();
		
		gpsLock = new ReentrantLock[GPSFIELDS];
		for (int i = 0; i < GPSFIELDS; i++)
			gpsLock[i] = new ReentrantLock();
		
		motorLock = new ReentrantLock();
	}
	
	/* Sends a status report to the control server; iterates through all important fields to do it. */
	private static void sendUpdate() {		
		long starttime = System.currentTimeMillis(); //to ensure that messages are sent no faster than UPDATEINTERVAL
		
		/* Lock data, send it, unlock.  If the lock is unavailable (unlikely), skip this datapiece for this iteration */
		if (readingLock[AZIMUTH].tryLock()) {
			if (readingLock[PITCH].tryLock()) {
				if (readingLock[ROLL].tryLock()) {

					Comm.sendMessage("ORIENT:" + reading[AZIMUTH] +
									":" + reading[PITCH] + 
									":" + reading[ROLL]);
					
					readingLock[ROLL].unlock();
				}
				readingLock[PITCH].unlock();
			}
			readingLock[AZIMUTH].unlock();
		}
		
		if (readingLock[XACCEL].tryLock()) { 
			if (readingLock[YACCEL].tryLock()) {
				if (readingLock[ZACCEL].tryLock()) {
	
					Comm.sendMessage("ACCEL:" + reading[XACCEL] +
									":" + reading[YACCEL] +
									":" + reading[ZACCEL]);
					
					readingLock[ZACCEL].unlock();
				}
				readingLock[YACCEL].unlock();
			}
			readingLock[XACCEL].unlock();
		}
		
		if (readingLock[MAG1].tryLock()) {
			if (readingLock[MAG2].tryLock()) {
				if (readingLock[MAG3].tryLock()) {
			
					Comm.sendMessage("FLUX:" + reading[MAG1] +
							":" + reading[MAG2] +
							":" + reading[MAG3]);
				
					readingLock[MAG3].unlock();
				}
				readingLock[MAG2].unlock();
			}
			readingLock[MAG1].unlock();
		}
		
		if (motorLock.tryLock()) {
	
			Comm.sendMessage("MOTORSPEED:" + motorspeed[0] +
					":" + motorspeed[1] +
					":" + motorspeed[2] +
					":" + motorspeed[3]);
			motorLock.unlock();
		}
		
		if (readingLock[LIGHT].tryLock()) {
			Comm.sendMessage("LIGHT:" + reading[LIGHT]);
			readingLock[LIGHT].unlock();
		}
		if (readingLock[PROXIMITY].tryLock()) {
			Comm.sendMessage("PROXIMITY:" + reading[PROXIMITY]);
			readingLock[PROXIMITY].unlock();
		}
		if (readingLock[PRESSURE].tryLock()) {
			Comm.sendMessage("PRESSURE:" + reading[PRESSURE]);
			readingLock[PRESSURE].unlock();
		}
		if (readingLock[TEMPERATURE].tryLock()) {
			Comm.sendMessage("TEMPERATURE:" + reading[TEMPERATURE]);
			readingLock[TEMPERATURE].unlock();
		}
		
		/* ints are atomic, no lock necessary */
		Comm.sendMessage("BATTERY:" + ((float) currbattery / (float) maxbattery));
		
		/* Send GPS data */
		String gpsData = new String("GPS");
		for (int i = 0; i < GPSFIELDS; i++) {
			if (gpsLock[i].tryLock()) {
				gpsData += ":" + ChopperStatus.gps[i];
				gpsLock[i].unlock();
			}
			else
				gpsData += ":-0";
		}
		
		gpsData += ":" + ChopperStatus.gpsaccuracy + 
		":" + ChopperStatus.gpsnumsats +
		":" + ChopperStatus.gpstimestamp;
			
		Comm.sendMessage(gpsData);
		
		/* Ensure loop time is no faster than UPDATEINTERVAL */
		long endtime = System.currentTimeMillis();
		
		/* Schedule the next status update */
		long timetonext = UPDATEINTERVAL - (endtime - starttime);
		if (timetonext > 0)
			handler.sendEmptyMessageDelayed(SENDSTATUSUPDATE, timetonext);
		else
			handler.sendEmptyMessage(SENDSTATUSUPDATE);
	}
	
	/**
	 * Registers the thread as a sensor listener for all desired sensors.
	 */
	public void run()
	{
		//System.out.println("ChopperStatus run() thread ID " + getId());
		Looper.prepare();
		
		handler = new Handler() {
            public void handleMessage(Message msg)
            {
                switch (msg.what){
                case SENDSTATUSUPDATE:
                		sendUpdate();
                	break;
                }
            }
        };
        /* Register to receive battery status updates */
        BroadcastReceiver batteryInfo = new BroadcastReceiver() {
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
					//ints are atomic; no lock needed
					currbattery = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
					maxbattery = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
					if (currbattery <= LOWBATT)
						Comm.updateAll("SYS:LOWPOWER");
				}
			}
		};
		
        /* Gets a sensor manager */
		SensorManager sensors = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		
		/* Registers this class as a sensor listener for every necessary sensor. */
		sensors.registerListener(this, sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
		sensors.registerListener(this, sensors.getDefaultSensor(Sensor.TYPE_LIGHT), SensorManager.SENSOR_DELAY_NORMAL);
		sensors.registerListener(this, sensors.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL);
		sensors.registerListener(this, sensors.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_FASTEST);
		sensors.registerListener(this, sensors.getDefaultSensor(Sensor.TYPE_PRESSURE), SensorManager.SENSOR_DELAY_NORMAL);
		sensors.registerListener(this, sensors.getDefaultSensor(Sensor.TYPE_PROXIMITY), SensorManager.SENSOR_DELAY_NORMAL);
		sensors.registerListener(this, sensors.getDefaultSensor(Sensor.TYPE_TEMPERATURE), SensorManager.SENSOR_DELAY_NORMAL);
		
		/* Initialize GPS reading: */
		LocationManager LocMan = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		LocMan.requestLocationUpdates(LocationManager.GPS_PROVIDER,	gpsmintime,	gpsmindist,	this);
		
		context.registerReceiver(batteryInfo, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		
		handler.sendEmptyMessageDelayed(SENDSTATUSUPDATE, UPDATEINTERVAL);
		Looper.loop();
	}
	
	/**
	 * Changes the value of the local GPS fields based on the data contained in a new GPS fix.
	 * @param loc New GPS fix
	 */
	public void onLocationChanged(Location loc) {
		if (loc != null && gps != null) {
			
			double newalt = loc.getAltitude();
			/* Vertical velocity does not update until vertical position does; prevents false conclusions that vertical velocity == 0 */
			if (newalt != gps[ALTITUDE]) {
				gpsLock[dALT].lock();
				gps[dALT] = (newalt - gps[ALTITUDE]) / (gpstimestamp - loc.getTime()) * 1000.0; //that last 1000 converts from m/ms to m/s
				gpsLock[dALT].unlock();
			}
			
			gpsLock[ALTITUDE].lock();
			gps[ALTITUDE] = newalt;
			gpsLock[ALTITUDE].unlock();
			
			gpsLock[BEARING].lock();
			gps[BEARING] = loc.getBearing();
			gpsLock[BEARING].unlock();
			
			gpsLock[LONG].lock();
			gps[LONG] = loc.getLongitude();
			gpsLock[LONG].unlock();
			
			gpsLock[LAT].lock();
			gps[LAT] = loc.getLatitude();
			gpsLock[LAT].unlock();
			
			gpsLock[SPEED].lock();
			gps[SPEED] = loc.getSpeed();
			gpsLock[SPEED].unlock();
			
			gpsaccuracy = loc.getAccuracy();
			gpstimestamp = loc.getTime();
			if (loc.getExtras() != null)
				gpsnumsats = loc.getExtras().getInt("satellites");
			
			lastLoc = loc;
			
			synchronized (lastLoc) {
				lastLoc = loc;
			}
			
		}
	}
	
	/**
	 * Registers a change in sensor accuracy.  Not used in this application.
	 * @param sensor Sensor registering change in accuracy.
	 * @param newaccuracy New accuracy value.
	 */
	public void onAccuracyChanged(Sensor sensor, int newaccuracy) {
	}
	
	/**
	 * Registers a change in sensor data.
	 * @param event Object containing new data--sensor type, accuracy, timestamp and value.
	 */
	public void onSensorChanged(SensorEvent event) {
		int type = event.sensor.getType();
		switch (type) {
			//Locks handled in updateField()
			case Sensor.TYPE_ACCELEROMETER:
				updateField(XACCEL, event.accuracy, event.timestamp, event.values[0]);
				updateField(YACCEL, event.accuracy, event.timestamp, event.values[1]);
				updateField(ZACCEL, event.accuracy, event.timestamp, event.values[2]);				
				break;
			case Sensor.TYPE_LIGHT:
				updateField(LIGHT, event.accuracy, event.timestamp, event.values[0]);
				break;
			case Sensor.TYPE_MAGNETIC_FIELD:
				updateField(MAG1, event.accuracy, event.timestamp, event.values[0]);
				updateField(MAG2, event.accuracy, event.timestamp, event.values[1]);
				updateField(MAG3, event.accuracy, event.timestamp, event.values[2]);
				break;
			case Sensor.TYPE_ORIENTATION:
				updateField(AZIMUTH, event.accuracy, event.timestamp, event.values[0]);
				updateField(PITCH, event.accuracy, event.timestamp, event.values[1]);
				updateField(ROLL, event.accuracy, event.timestamp, event.values[2]);
				break;
			case Sensor.TYPE_PRESSURE:
				updateField(PRESSURE, event.accuracy, event.timestamp, event.values[0]);
				break;
			case Sensor.TYPE_PROXIMITY:
				updateField(PROXIMITY, event.accuracy, event.timestamp, event.values[0]);
				break;
			case Sensor.TYPE_TEMPERATURE:
				updateField(TEMPERATURE, event.accuracy, event.timestamp, event.values[0]);
				break;
		}
	}
	
	/* Updates the relevant fields with the specified data. */
	private static void updateField(int field, int myaccuracy, long mytimestamp, double value) {
		timestamp[field] = mytimestamp;
		
		readingLock[field].lock();
		reading[field] = value;
		readingLock[field].unlock();
	}
	
	/**
	 * Informs server that a GPS provider is disabled.
	 * @param provider The name of the provider that has been disabled
	 */
	public void onProviderDisabled(String provider)	{
		Comm.sendMessage("GPS:DISABLED");
	}
	
	/**
	 * Informs server that a GPS provider is enabled.
	 * @param provider The name of the provider that has been enabled
	 */
	public void onProviderEnabled(String provider) {
		Comm.sendMessage("GPS:ENABLED.");
	}
	
	/**
	 * Informs server that a GPS provider's status has changed.
	 * @param provider The name of the provider whose status has changed
	 * @param status The new status of the provider
	 * @param extras Provider-specific extra data
	 */
	public void onStatusChanged(String provider, int status, Bundle extras)	{
		switch (status) {
			case LocationProvider.OUT_OF_SERVICE:
				Comm.sendMessage("GPS:OUT.OF.SERVICE");
				break;
			case LocationProvider.TEMPORARILY_UNAVAILABLE:
				Comm.sendMessage("GPS:TEMPORARILY.UNAVAILABLE");
				break;
			case LocationProvider.AVAILABLE:
				Comm.sendMessage("GPS:AVAILABLE");
				break;
		}
	}
}
