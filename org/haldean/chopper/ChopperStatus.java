package org.haldean.chopper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
import android.util.Log;

/**
 * Central "storehouse" for information about the chopper's status--maintains updated sensor readings, gps readings, etc.
 * @author Benjamin Bardin
 * @author Will Brown
 */
public final class ChopperStatus implements Runnable, SensorEventListener, Constants, LocationListener
{	
	/* How often (in ms) status updates should be sent by ChopperStatus to the server */
	private static final int UPDATEINTERVAL = 500;
	
	/* Parameter to specify GPS minimum update distance, with the usual trade-off between accuracy and power consumption.
	 * Value of '0' means maximum accuracy. */
	private static final float gpsMinDist = 0;
	
	/* Parameter to specify GPS minimum update time, with the usual trade-off between accuracy and power consumption.
	 * Value of '0' means maximum accuracy. */
	private static final long gpsMinTime = 0;
	
	/* Holds data from various sensors, like gyroscope, acceleration and magnetic flux. */
	private static double[] reading = new double[NUMSENSORS]; //last known data for a given sensor
	
	/* Locks for fields in reading[]. */
	private static ReentrantLock[] readingLock;
	
	/* Timestamp of last write to corresponding field in reading[]. */
	//private static long[] timestamp = new long[NUMSENSORS]; //timestamp in nanos of last reading
	
	/* Holds GPS data. */
	private static double[] gps = new double[GPSFIELDS]; //last available GPS readings
	
	/* Locks for fields in gps[]. */
	private static ReentrantLock[] gpsLock;
	
	/* Stores the location object last returned by the GPS. */
	private static Location lastLoc;
	
	/* Stores the speeds last submitted to the motors. */
	private static double[] motorspeed = new double[4];
	
	/* Lock for motorspeed[]. */
	private static ReentrantLock motorLock;
	
	/* Current battery level. */
	private static int currbattery = 0;
	
	/* Max battery level. */
	private static int maxbattery = 100;
	
	/* Lock for battery data */
	private static ReentrantLock batteryLock;	
	
	/* Accuracy of last GPS reading.  This field is only accessed by one thread, so no lock is needed. */
	private static float gpsAccuracy; //accuracy of said reading

	/* Timestamp of last GPS reading.  This field is only accessed by one thread, so no lock is needed. */
	private static long gpsTimeStamp; //timestamp of the reading
	
	/* Number of satellites used to obtain last GPS reading.  This field is only accessed by one thread, so no lock is needed. */
	private static int gpsnumsats; //number of satellites used to collect last reading
	
	/* Lock for gps extras */
	private static ReentrantLock gpsExtrasLock;
	
	/* Used to obtain location manager. */
	private static Context context;
	
	/* Handles messages */
	private static Handler handler;
	
	/* Tag for logging */
	private static String TAG = "Chopper.ChopperStatus";
	
	/* Thread pool for mutator methods */
	private static ExecutorService mutatorPool;
	
	/* Number of threads in the mutator pool */
	private static final int numMutatorThreads = 3;
	
	/**
	 * Initializes the locks
	 * @param mycontext Application context
	 */
	public ChopperStatus(Context mycontext)	{
		context = mycontext;
		
		//Initialize the data locks
		readingLock = new ReentrantLock[NUMSENSORS];
		for (int i = 0; i < NUMSENSORS; i++) {
			readingLock[i] = new ReentrantLock();
		}
		
		gpsLock = new ReentrantLock[GPSFIELDS];
		for (int i = 0; i < GPSFIELDS; i++) {
			gpsLock[i] = new ReentrantLock();
		}
		
		motorLock = new ReentrantLock();
		gpsExtrasLock = new ReentrantLock();
		batteryLock = new ReentrantLock();
		
		mutatorPool = Executors.newFixedThreadPool(numMutatorThreads);
	}
	
	/* Sends a status report to the control server; iterates through all important fields to do it. */
	private static void sendUpdate() {		
		long starttime = System.currentTimeMillis(); //to ensure that messages are sent no faster than UPDATEINTERVAL
		
		/* Lock data, send it, unlock.  If the lock is unavailable (unlikely), skip this datapiece for this iteration */
		try {
			double myAzimuth = getReadingFieldNow(AZIMUTH);
			double myPitch = getReadingFieldNow(PITCH);
			double myRoll = getReadingFieldNow(ROLL);
			Comm.sendMessage("ORIENT:" + myAzimuth + ":" + myPitch + ":" + myRoll);
		}
		catch (IllegalAccessException e) {
			Log.w(TAG, "Orientation Report Unavailable");
		}
		
		try {
			double myXaccel = getReadingFieldNow(XACCEL);
			double myYaccel = getReadingFieldNow(YACCEL);
			double myZaccel = getReadingFieldNow(ZACCEL);
			Comm.sendMessage("ACCEL:" + myXaccel + ":" + myYaccel + ":" + myZaccel);
		}
		catch (IllegalAccessException e) {
			Log.w(TAG, "Acceleration Report Unavailable");
		}

		try {
			double myXflux = getReadingFieldNow(XFLUX);
			double myYflux = getReadingFieldNow(YFLUX);
			double myZflux = getReadingFieldNow(ZFLUX);
			Comm.sendMessage("FLUX:" + myXflux + ":" + myYflux + ":" + myZflux);
		}
		catch (IllegalAccessException e) {
			Log.w(TAG, "Flux Report Unavailable");
		}
		
		try {
			double[] mySpeeds = getMotorFieldsNow();
			Comm.sendMessage("MOTORSPEED:" + mySpeeds[0] +
					":" + mySpeeds[1] +
					":" + mySpeeds[2] +
					":" + mySpeeds[3]);
		}
		catch (IllegalAccessException e) {
			Log.w(TAG, "MotorSpeeds Report Unavailable");
		}
		try {
			double myLight = getReadingFieldNow(LIGHT);
			Comm.sendMessage("LIGHT:" + myLight);
		}
		catch (IllegalAccessException e) {
			Log.w(TAG, "Light Report Unavailable");
		}
		
		try {
			double myProximity = getReadingFieldNow(PROXIMITY);
			Comm.sendMessage("PROXIMITY:" + myProximity);
		}
		catch (IllegalAccessException e) {
			Log.w(TAG, "Proximity Report Unavailable");
		}
		
		try {
			double myPressure = getReadingFieldNow(PRESSURE);
			Comm.sendMessage("PRESSURE:" + myPressure);
		}
		catch (IllegalAccessException e) {
			Log.w(TAG, "Pressure Report Unavailable");
		}
		
		try {
			double myTemp = getReadingFieldNow(TEMPERATURE);
			Comm.sendMessage("TEMPERATURE:" + myTemp);
		}
		catch (IllegalAccessException e) {
			Log.w(TAG, "Temperature Report Unavailable");
		}
		
		if (batteryLock.tryLock()) {
			try {
				Comm.sendMessage("BATTERY:" + ((float) currbattery / (float) maxbattery));
			}
			finally {
				batteryLock.unlock();
			}
		}
		
		/* Send GPS data */
		String gpsData = new String("GPS");
		try {
			for (int i = 0; i < GPSFIELDS; i++) {
				double myValue = getGpsFieldNow(i);
				gpsData += ":" + myValue;
			}
			
			if (gpsExtrasLock.tryLock()) {
				gpsData += ":" + ChopperStatus.gpsAccuracy + 
				":" + ChopperStatus.gpsnumsats +
				":" + ChopperStatus.gpsTimeStamp;
				gpsExtrasLock.unlock();
			}
			else {
				throw new IllegalAccessException();
			}
			
			Comm.sendMessage(gpsData);
		}
		catch (IllegalAccessException e) {
			Log.w(TAG, "GPS Report Unavailable");
		}
		
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
	 * Returns the value stored at the specified GPS index, if its lock is available.  Otherwise, returns the supplied default value.
	 * @param whichField The index of the desired GPS data.
	 * @param expectedValue The default to return, should its lock be unavailable.
	 * @return Either the GPS data or the supplied default, depending on whether or not its lock is available.
	 */
	public static double getGpsFieldNow(int whichField, double expectedValue) {
		double myValue;
		if (gpsLock[whichField].tryLock()) {
			myValue = gps[whichField];
			gpsLock[whichField].unlock();
		}
		else {
			myValue = expectedValue;
		}
		return myValue;
	}
	
	/**
	 * Returns a copy of the most recent Location object delivered by the GPS.
	 * @return The Location, if there is one; null if the GPS has not yet obtained a fix.
	 */
	public static Location getLastLocation() {
		if (lastLoc == null)
			return null;
		Location myLocation;
		synchronized (lastLoc) {
			myLocation = new Location(lastLoc);
		}
		return myLocation;
	}
	/**
	 * Returns the value stored at the specified GPS index, if its lock is available.  Otherwise, throws an exception.
	 * @param whichField The index of the desired GPS data.
	 * @return The desired GPS data.
	 * @throws IllegalAccessException If the lock for the desired data is unavailable.
	 */
	public static double getGpsFieldNow(int whichField) throws IllegalAccessException {
		double myValue;
		if (gpsLock[whichField].tryLock()) {
			myValue = gps[whichField];
			gpsLock[whichField].unlock();
		}
		else {
			Log.w(TAG, "GPS Field " + whichField + " is locked.");
			throw new IllegalAccessException();
		}
		return myValue;
	}
	
	/**
	 * Returns the value stored at the specified GPS index.  If its lock is unavailable, blocks until it is.
	 * @param whichField The index of the desired GPS data.
	 * @return The desired GPS data.
	 */
	public static double getGpsField(int whichField) {
		double myValue;
		gpsLock[whichField].lock();
		myValue = gps[whichField];
		gpsLock[whichField].unlock();
		return myValue;
	}
	
	/**
	 * Returns the reading specified by the supplied index, if its lock is available.  Otherwise, returns the supplied default value.
	 * @param whichField The index of the desired reading.
	 * @param expectedValue The default to return, should its lock be unavailable.
	 * @return Either the reading or the supplied default, depending on whether or not its lock is available.
	 */
	public static double getReadingFieldNow(int whichField, double expectedValue) {
		double myValue;
		if (readingLock[whichField].tryLock()) {
			myValue = reading[whichField];
			readingLock[whichField].unlock();
		}
		else {
			Log.w(TAG, "Reading field " + whichField + " is locked.");
			myValue = expectedValue;
		}
		return myValue;
	}
	
	/**
	 * Returns the reading specified by the supplied index, if its lock is available.  Otherwise, throws an exception.
	 * @param whichField The index of the desired reading.
	 * @return The desired reading.
	 * @throws IllegalAccessException If the lock for the desired reading is unavailable.
	 */
	public static double getReadingFieldNow(int whichField) throws IllegalAccessException {
		double myValue;
		if (readingLock[whichField].tryLock()) {
			myValue = reading[whichField];
			readingLock[whichField].unlock();
		}
		else {
			Log.w(TAG, "Reading field " + whichField + " is locked.");
			throw new IllegalAccessException();
		}
		return myValue;
	}
	
	/**
	 * Obtains the current motor speeds.
	 * @return A copy of the array containing the motor speeds.
	 * @throws IllegalAccessException If the lock is unavailable.
	 */
	public static double[] getMotorFieldsNow() throws IllegalAccessException {
		double[] myValues = new double[4];
		if (motorLock.tryLock()) {
			for (int i = 0; i < 4; i++) {
				myValues[i] = motorspeed[i];
			}
			motorLock.unlock();
		}
		else {
			Log.w(TAG, "motorspeed is locked.");
			throw new IllegalAccessException();
		}
		return myValues;
	}
	
	/**
	 * Writes the supplied GPS value at the specified GPS field. 
	 * @param whichField The index of the GPS data to store.
	 * @param value The GPS data.
	 */
	private static void setGpsField(final int whichField, final double value) {
		mutatorPool.submit(new Runnable() {
			public void run() {
				gpsLock[whichField].lock();
				gps[whichField] = value;
				gpsLock[whichField].unlock();
			}
		});
	}
	
	/**
	 * Writes the supplied reading at the specified field. 
	 * @param whichField The index of the reading to store.
	 * @param value The reading.
	 */
	private static void setReadingField(final int whichField, final double value) {
		mutatorPool.submit(new Runnable() {
			public void run() {
				readingLock[whichField].lock();
				reading[whichField] = value;
				readingLock[whichField].unlock();
			}
		});
	}
	
	/**
	 * Spawns a thread that sets the motor speeds info to the supplied values, after cloning them for safe multi-threading.
	 * Uses a new thread to permit the caller (usually Guidance) to move on to a new task immediately.
	 * @param mySpeeds The data to which the motor speeds should be set.
	 */
	protected static void setMotorFields(double[] mySpeeds) {
		if (mySpeeds.length < 4)
			return;
		final double[] speeds = mySpeeds.clone();
		mutatorPool.submit(new Runnable() {
			public void run() {
				motorLock.lock();
				for (int i = 0; i < 4; i++) {
					motorspeed[i] = speeds[i];
				}
				motorLock.unlock();
			}
		});
	}
	
	/**
	 * Registers the thread as a sensor listener for all desired sensors.
	 */
	public void run()
	{
		//System.out.println("ChopperStatus run() thread ID " + getId());
		Looper.prepare();
		Thread.currentThread().setName("ChopperStatus");
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
					/* int read/writes are uninterruptible, no lock needed */
					batteryLock.lock();
					currbattery = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
					maxbattery = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
					float batteryPercent = (float) currbattery * 100F / (float) maxbattery;
					batteryLock.unlock();
					if (batteryPercent <= LOWBATT)
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
		LocMan.requestLocationUpdates(LocationManager.GPS_PROVIDER,	gpsMinTime,	gpsMinDist,	this);
		
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
			if (!loc.hasAltitude()) {
				loc.setAltitude(300.0);
				Log.w(TAG, "No altitude fix");
			}
			double newalt = loc.getAltitude();
			System.out.println("new altitude: " + newalt);
			/* Vertical velocity does not update until vertical position does; prevents false conclusions that vertical velocity == 0 */
			double oldAlt = getGpsField(ALTITUDE);
			if (newalt != oldAlt) {
				gpsExtrasLock.lock();
				long timeElapsed = gpsTimeStamp - loc.getTime();
				gpsExtrasLock.unlock();
				
				setGpsField(dALT, (newalt - oldAlt / (double) timeElapsed) * 1000.0);
			}
			
			setGpsField(ALTITUDE, newalt);
			setGpsField(BEARING, loc.getBearing());
			setGpsField(LONG, loc.getLongitude());
			setGpsField(LAT, loc.getLatitude());			
			setGpsField(SPEED, loc.getSpeed());
			
			gpsExtrasLock.lock();
			gpsAccuracy = loc.getAccuracy();
			gpsTimeStamp = loc.getTime();
			if (loc.getExtras() != null)
				gpsnumsats = loc.getExtras().getInt("satellites");
			gpsExtrasLock.unlock();
			
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
				updateField(XFLUX, event.accuracy, event.timestamp, event.values[0]);
				updateField(YFLUX, event.accuracy, event.timestamp, event.values[1]);
				updateField(ZFLUX, event.accuracy, event.timestamp, event.values[2]);
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
		//timestamp[field] = mytimestamp;
		setReadingField(field, value);
	}
	
	/**
	 * Informs server that a GPS provider is disabled.
	 * @param provider The name of the provider that has been disabled
	 */
	public void onProviderDisabled(String provider)	{
		System.out.println("GPS disabled");
		Comm.sendMessage("GPS:STATUS:DISABLED");
	}
	
	/**
	 * Informs server that a GPS provider is enabled.
	 * @param provider The name of the provider that has been enabled
	 */
	public void onProviderEnabled(String provider) {
		System.out.println("GPS enabled");
		Comm.sendMessage("GPS:STATUS:ENABLED.");
	}
	
	/**
	 * Informs server that a GPS provider's status has changed.
	 * @param provider The name of the provider whose status has changed
	 * @param status The new status of the provider
	 * @param extras Provider-specific extra data
	 */
	public void onStatusChanged(String provider, int status, Bundle extras)	{
		System.out.println("GPS status changed " + status);
		switch (status) {
			case LocationProvider.OUT_OF_SERVICE:
				Comm.sendMessage("GPS:STATUS:OUT.OF.SERVICE");
				break;
			case LocationProvider.TEMPORARILY_UNAVAILABLE:
				Comm.sendMessage("GPS:STATUS:TEMPORARILY.UNAVAILABLE");
				break;
			case LocationProvider.AVAILABLE:
				Comm.sendMessage("GPS:STATUS:AVAILABLE");
				break;
		}
	}
}
