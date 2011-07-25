package org.haldean.chopper.pilot;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.concurrent.atomic.AtomicInteger;

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
import android.os.Environment;
import android.os.Looper;
import android.util.Log;

/**
 * Central "storehouse" for information about the chopper's status--maintains updated sensor readings, gps readings, etc.
 * <P>
 * May send the following messages to registered Receivables:<br>
 * <pre>
 * CSYS:LOWPOWER
 * GPS:STATUS:
 *            DISABLED
 *            ENABLED
 *            OUT.OF.SERVICE
 *            TEMPORARILY.UNAVAILABLE
 *            AVAILABLE
 * </pre>
 * @author Benjamin Bardin
 * @author Will Brown
 */
public final class ChopperStatus implements Runnable, SensorEventListener, Constants, LocationListener
{	
	/** Point of "officially" low battery */
	public final static int LOW_BATT = 30;
	
	public static final String logfilename = "fastest.txt";
	public static BufferedWriter logwriter;
	
	/** Tag for logging */
	public static final String TAG = "chopper.ChopperStatus";
	
	/** Parameter to specify GPS minimum update distance, with the usual trade-off between accuracy and power consumption.
	 * Value of '0' means maximum accuracy. */
	private static final float GPS_MIN_DIST = 0;
	
	/** Parameter to specify GPS minimum update time, with the usual trade-off between accuracy and power consumption.
	 * Value of '0' means maximum accuracy. */
	private static final long GPS_MIN_TIME = 0;
	
	/** Used to obtain location manager. */
	private Context mContext;
	
	/** Current battery level. */
	private final AtomicInteger mCurrBatt = new AtomicInteger(0);
	
	/** Holds GPS data. */
	private double[] mGps = new double[GPS_FIELDS]; //last available GPS readings
	
	/** Accuracy of last GPS reading.  This field is only accessed by one thread, so no lock is needed. */
	private float mGpsAccuracy; //accuracy of said reading
	
	/** Lock for gps extras */
	private Object mGpsExtrasLock;
	
	/** Locks for fields in gps[]. */
	private Object[] mGpsLock;
	
	/** Number of satellites used to obtain last GPS reading. */
	private int mGpsNumSats; //number of satellites used to collect last reading
	
	/** Timestamp of last GPS reading. */
	private long mGpsTimeStamp; //timestamp of the reading
	
	/** Stores the location object last returned by the GPS. */
	private Location mLastLoc;
	
	/** Max battery level. */
	private final AtomicInteger mMaxBatt = new AtomicInteger(100);

	/** Stores the speeds last submitted to the motors. */
	private double[] mMotorSpeed = new double[4];
	
	/** Stores power levels for the motors. */
	private double[] mMotorPower = new double[4];
	
	/** Holds data from various sensors, like gyroscope, acceleration and magnetic flux. */
	private double[] mReading = new double[SENSORS]; //last known data for a given sensor

	/** Locks for fields in reading[]. */
	private Object[] mReadingLock;
	
	/** List of registered receivers */
	private LinkedList<Receivable> mRec;
	
	private float[] orientation = new float[3];
	private float[] rotationMatrix = new float[9];
	private float[] flux = new float[3];

	/**
	 * Initializes the locks, registers application context for runtime use.
	 * @param mycontext Application context
	 */
	public ChopperStatus(Context mycontext)	{
		mContext = mycontext;
		mRec = new LinkedList<Receivable>();
		
		//Initialize the data locks
		mReadingLock = new Object[SENSORS];
		
		for (int i = 0; i < SENSORS; i++) {
			mReadingLock[i] = new Object();
		}
		
		mGpsLock = new Object[GPS_FIELDS];
		for (int i = 0; i < GPS_FIELDS; i++) {
			mGpsLock[i] = new Object();
		}
		mGpsExtrasLock = new Object();
	}
	
	public void close() {
		if (logwriter != null) {
			try {
				logwriter.close();
			} catch (IOException e) {
				Log.e(TAG, "Canno close logfile");
			}
		}
	}
	
	/**
	 * Gets the phone battery level
	 * @return The power level, as a float between 0 and 1.
	 */
	public float getBatteryLevel() {
		return (float) mCurrBatt.get() / (float) mMaxBatt.get();
	}
	
	/**
	 * Gets GPS "extras" data, through a string of the form accuracy:numberOfSatellitesInLatFix:timeStampOfLastFix
	 * @return The data string
	 */
	public String getGpsExtras() {
		String gpsData = "";
		synchronized (mGpsExtrasLock) {
			gpsData += mGpsAccuracy + 
			":" + mGpsNumSats +
			":" + mGpsTimeStamp;
		}
		return gpsData;
	}
	
	/**
	 * Returns the value stored at the specified GPS index.  If its lock is unavailable, blocks until it is.
	 * @param whichField The index of the desired GPS data.
	 * @return The desired GPS data.
	 */
	public double getGpsField(int whichField) {
		double myValue;
		synchronized (mGpsLock[whichField]) {
			myValue = mGps[whichField];
		}
		return myValue;
	}
	
	public long getGpsTimeStamp() {
		synchronized(mGpsExtrasLock) {
			return mGpsTimeStamp;
		}
	}
	
	/**
	 * Returns a copy of the most recent Location object delivered by the GPS.
	 * @return The Location, if there is one; null if the GPS has not yet obtained a fix.
	 */
	public Location getLastLocation() {
		if (mLastLoc == null)
			return null;
		Location myLocation;
		synchronized (mLastLoc) {
			myLocation = new Location(mLastLoc);
		}
		return myLocation;
	}
	
	/**
	 * Obtains the current motor speeds.
	 * @param myValues A copy of the array containing the motor speeds.  Must have length >= 4.
	 */
	public void getMotorFields (double[] myValues){
		if (myValues.length < 4) {
			throw new IllegalArgumentException();
		}
		synchronized (mMotorSpeed) {
			System.arraycopy(mMotorSpeed, 0, myValues, 0, 4);
		}
	}
	
	/** Obtains the current motor power levels.
	 * @param myValues A copy of the array containing the motor power levels.  Must have length >= 4.
	 */
	public void getMotorPowerFields(double[] myValues) {
		if (myValues.length < 4) {
			throw new IllegalArgumentException();
		}
		synchronized (mMotorPower) {
			System.arraycopy(mMotorPower, 0, myValues, 0, 4);
		}
	}
	
	/**
	 * Runs the Thread
	 */
	public void run()
	{
		//System.out.println("ChopperStatus run() thread ID " + getId());
		Looper.prepare();
		Thread.currentThread().setName("ChopperStatus");
		File root = Environment.getExternalStorageDirectory();
		if (root == null) Log.e(TAG, "No root directory found");
		try {
			logwriter = new BufferedWriter(new FileWriter(root + "/chopper/" + logfilename, false));
		} catch (IOException e) {
			Log.e(TAG, "No ChopperStatus logfile");
			e.printStackTrace();
		}
        /* Register to receive battery status updates */
        BroadcastReceiver batteryInfo = new BroadcastReceiver() {
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
					/* int read/writes are uninterruptible, no lock needed */
					mCurrBatt.set(intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0));
					mMaxBatt.set(intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100));
					float batteryPercent = (float) mCurrBatt.get() * 100F / (float) mMaxBatt.get();
					if (batteryPercent <= LOW_BATT) {
						updateReceivers("CSYS:LOWPOWER");
					}
				}
			}
        };
	
        /* Gets a sensor manager */
		final SensorManager sensors = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
		/* Registers this class as a sensor listener for every necessary sensor. */
		//sensors.registerListener(this, sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST);
		//sensors.registerListener(this, sensors.getDefaultSensor(Sensor.TYPE_LIGHT), SensorManager.SENSOR_DELAY_NORMAL);
		sensors.registerListener(this, sensors.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_FASTEST);				
		
		//sensors.registerListener(this, sensors.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_FASTEST);
		//sensors.registerListener(this, sensors.getDefaultSensor(Sensor.TYPE_PRESSURE), SensorManager.SENSOR_DELAY_NORMAL);
		//sensors.registerListener(this, sensors.getDefaultSensor(Sensor.TYPE_PROXIMITY), SensorManager.SENSOR_DELAY_NORMAL);
		//sensors.registerListener(this, sensors.getDefaultSensor(Sensor.TYPE_TEMPERATURE), SensorManager.SENSOR_DELAY_NORMAL);
		sensors.registerListener(this, sensors.getDefaultSensor(Sensor.TYPE_GRAVITY), SensorManager.SENSOR_DELAY_FASTEST);				
		

		/* Initialize GPS reading: */
		LocationManager LocMan = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
		LocMan.requestLocationUpdates(LocationManager.GPS_PROVIDER,	GPS_MIN_TIME, GPS_MIN_DIST,	this);
		
		mContext.registerReceiver(batteryInfo, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

		Looper.loop();
	}
	
	/**
	 * Returns the reading specified by the supplied index, if its lock is available.  Otherwise, throws an exception.
	 * @param whichField The index of the desired reading.
	 * @return The desired reading.
	 */
	public double getReadingField(int whichField) {
		double myValue;
		synchronized (mReadingLock[whichField]) {
			myValue = mReading[whichField];
		}
		return myValue;
	}
	
	/**
	 * Registers a change in sensor accuracy.  Not used in this application.
	 * @param sensor Sensor registering change in accuracy.
	 * @param newaccuracy New accuracy value.
	 */
	public void onAccuracyChanged(Sensor sensor, int newaccuracy) {
	}
	
	/**
	 * Changes the value of the local GPS fields based on the data contained in a new GPS fix.
	 * @param loc New GPS fix
	 */
	public void onLocationChanged(Location loc) {
		if (loc != null && mGps != null) {
			if (!loc.hasAltitude()) {
			//	loc.setAltitude(300.0);
				Log.w(TAG, "No altitude fix");
			}
			double newalt = loc.getAltitude();
			Log.i(TAG, "new altitude: " + newalt);
			/* Vertical velocity does not update until vertical position does; prevents false conclusions that vertical velocity == 0 */
			double oldAlt = getGpsField(ALTITUDE);
			if (newalt != oldAlt) {
				long timeElapsed;
				synchronized (mGpsExtrasLock) {
					timeElapsed = mGpsTimeStamp - loc.getTime();
				}
				double newdalt = ((newalt - oldAlt) / (double) timeElapsed) * 1000.0;
				setGpsField(dALT, newdalt);
				Log.i(TAG, "new dalt: " + newdalt);
				setGpsField(ALTITUDE, newalt);
			}
			setGpsField(BEARING, loc.getBearing());
			setGpsField(LONG, loc.getLongitude());
			setGpsField(LAT, loc.getLatitude());			
			setGpsField(SPEED, loc.getSpeed());
			
			synchronized (mGpsExtrasLock) {
				mGpsAccuracy = loc.getAccuracy();
				mGpsTimeStamp = loc.getTime();
				if (loc.getExtras() != null)
					mGpsNumSats = loc.getExtras().getInt("satellites");
			}
			if (mLastLoc != null) {
				synchronized (mLastLoc) {
					mLastLoc = loc;
				}
			}
			else {
				mLastLoc = loc;
			}
		}
	}
	
	/**
	 * Informs all receivers that a GPS provider is disabled.
	 * @param provider The name of the provider that has been disabled
	 */
	public void onProviderDisabled(String provider)	{
		System.out.println("GPS disabled");
		updateReceivers("GPS:STATUS:DISABLED");
	}
	
	/**
	 * Informs all receivers that a GPS provider is enabled.
	 * @param provider The name of the provider that has been enabled
	 */
	public void onProviderEnabled(String provider) {
		System.out.println("GPS enabled");
		updateReceivers("GPS:STATUS:ENABLED.");
	}
	
	/**
	 * Registers a change in sensor data.
	 * @param event Object containing new data--sensor type, accuracy, timestamp and value.
	 */
	public void onSensorChanged(SensorEvent event) {
		long time = System.currentTimeMillis();
		int type = event.sensor.getType();
		switch (type) {			
			case Sensor.TYPE_ACCELEROMETER:
				setReadingField(X_ACCEL, event.values[0]);
				setReadingField(Y_ACCEL, event.values[1]);
				setReadingField(Z_ACCEL, event.values[2]);
				break;
			case Sensor.TYPE_LIGHT:
				setReadingField(LIGHT, event.values[0]);
				break;
			case Sensor.TYPE_MAGNETIC_FIELD:
				System.arraycopy(event.values, 0, flux, 0, 3);
				setReadingField(X_FLUX, event.values[0]);
				setReadingField(Y_FLUX, event.values[1]);
				setReadingField(Z_FLUX, event.values[2]);
				break;
			case Sensor.TYPE_PRESSURE:
				setReadingField(PRESSURE, event.values[0]);
				break;
			case Sensor.TYPE_PROXIMITY:
				setReadingField(PROXIMITY, event.values[0]);
				break;
			case Sensor.TYPE_TEMPERATURE:
				setReadingField(TEMPERATURE, event.values[0]);
				break;
			case Sensor.TYPE_GRAVITY:
				//Log.v(TAG, "grav time: " + (time - grav_time));
				String timestring = Long.toString(time);
				try {
					if (logwriter != null) {
						logwriter.write(timestring + "\n");
						logwriter.flush();
					}
				} catch (IOException e) {
					// Do nothing.
				}
				/*Log.v(TAG, "");
				Log.v(TAG, "" + event.values[0]);
				Log.v(TAG, "" + event.values[1]);
				Log.v(TAG, "" + event.values[2]);
				Log.v(TAG, ""); */
				SensorManager.getRotationMatrix(rotationMatrix,
				  							    null,
												event.values,
												flux);
				/*for (int i = 0; i < 9; i++) {
					Log.v(TAG, "" + rotationMatrix[i]);
				}*/
				SensorManager.getOrientation(rotationMatrix, orientation);
				//Log.v(TAG, "" + orientation[i]);
				setReadingField(AZIMUTH, orientation[0] * 180.0 / Math.PI);
				setReadingField(PITCH, orientation[1] * 180.0 / Math.PI);
				setReadingField(ROLL, orientation[2] * -180.0 / Math.PI);
				break;
		}
	}
	
	/**
	 * Informs all receivers that a GPS provider's status has changed.
	 * @param provider The name of the provider whose status has changed
	 * @param status The new status of the provider
	 * @param extras Provider-specific extra data
	 */
	public void onStatusChanged(String provider, int status, Bundle extras)	{
		System.out.println("GPS status changed " + status);
		switch (status) {
			case LocationProvider.OUT_OF_SERVICE:
				updateReceivers("GPS:STATUS:OUT.OF.SERVICE");
				break;
			case LocationProvider.TEMPORARILY_UNAVAILABLE:
				updateReceivers("GPS:STATUS:TEMPORARILY.UNAVAILABLE");
				break;
			case LocationProvider.AVAILABLE:
				updateReceivers("GPS:STATUS:AVAILABLE");
				break;
		}
	}
	
	/**
	 * Registers a receivable for certain status-related updates, especially a Navigation object.
	 * @param rec The receivable to register.
	 * @see Navigation Navigation
	 */
	public void registerReceiver(Receivable rec) {
		synchronized (mRec) {
			mRec.add(rec);
		}
	}
	
	/**
	 * Sets the motor speed data to the supplied array.
	 * @param mySpeeds The data to which the motor speeds should be set.
	 */
	protected void setMotorFields(final double[] mySpeeds) {
		if (mySpeeds.length < 4)
			return;
		synchronized (mMotorSpeed) {
			System.arraycopy(mySpeeds, 0, mMotorSpeed, 0, 4);
			/*Log.v(TAG, "vector " + mMotorSpeed[0] + ", "
								 + mMotorSpeed[1] + ", "
								 + mMotorSpeed[2] + ", "
								 + mMotorSpeed[3]);*/
		}
		//Log.v(TAG, "Done changing motorspeeds");
	}
	
	/**
	 * Sets the motor power levels to the supplied array.
	 * @param myPowers The data to which the motor power levels should be set.
	 */
	protected void setMotorPowerFields(final double[] myPowers) {
		if (myPowers.length < 4)
			return;
		synchronized (mMotorPower) {
			System.arraycopy(myPowers, 0, mMotorPower, 0, 4);
		}
	}
	
	/**
	 * Writes the supplied GPS value at the specified GPS field. 
	 * @param whichField The index of the GPS data to store.
	 * @param value The GPS data.
	 */
	private void setGpsField(final int whichField, final double value) {
		synchronized (mGpsLock[whichField]) {
			mGps[whichField] = value;
		}
	}
	
	
	/**
	 * Writes the supplied reading at the specified field. 
	 * @param whichField The index of the reading to store.
	 * @param value The reading.
	 */
	private void setReadingField(final int whichField, final double value) {
		synchronized (mReadingLock[whichField]) {
			mReading[whichField] = value;
		}
	}
	
	/** Updates all registered receivers with the specified String */
	private void updateReceivers(String str) {
		synchronized (mRec) {
			ListIterator<Receivable> myList = mRec.listIterator();
			while (myList.hasNext()) {
				myList.next().receiveMessage(str, null);
			}
		}
	}
}