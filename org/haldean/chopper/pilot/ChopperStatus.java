package org.haldean.chopper.pilot;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.location.Location;
import android.os.Bundle;

public interface ChopperStatus {

	/** Point of "officially" low battery */
	public final static int LOW_BATT = 30;
	/** Tag for logging */
	public static final String TAG = "chopper.ChopperStatus";

	public void close();

	/**
	 * Gets the phone battery level
	 * @return The power level, as a float between 0 and 1.
	 */
	public float getBatteryLevel();

	/**
	 * Gets GPS "extras" data, through a string of the form accuracy:numberOfSatellitesInLatFix:timeStampOfLastFix
	 * @return The data string
	 */
	public String getGpsExtras();

	/**
	 * Returns the value stored at the specified GPS index.  If its lock is unavailable, blocks until it is.
	 * @param whichField The index of the desired GPS data.
	 * @return The desired GPS data.
	 */
	public double getGpsField(int whichField);

	public long getGpsTimeStamp();

	/**
	 * Returns a copy of the most recent Location object delivered by the GPS.
	 * @return The Location, if there is one; null if the GPS has not yet obtained a fix.
	 */
	public Location getLastLocation();

	/**
	 * Obtains the current motor speeds.
	 * @param myValues A copy of the array containing the motor speeds.  Must have length >= 4.
	 */
	public void getMotorFields(double[] myValues);

	/** Obtains the current motor power levels.
	 * @param myValues A copy of the array containing the motor power levels.  Must have length >= 4.
	 */
	public void getMotorPowerFields(double[] myValues);

	/**
	 * Runs the Thread
	 */
	public void run();

	/**
	 * Returns the reading specified by the supplied index, if its lock is available.  Otherwise, throws an exception.
	 * @param whichField The index of the desired reading.
	 * @return The desired reading.
	 */
	public double getReadingField(int whichField);

	/**
	 * Registers a change in sensor accuracy.  Not used in this application.
	 * @param sensor Sensor registering change in accuracy.
	 * @param newaccuracy New accuracy value.
	 */
	public void onAccuracyChanged(Sensor sensor, int newaccuracy);

	/**
	 * Changes the value of the local GPS fields based on the data contained in a new GPS fix.
	 * @param loc New GPS fix
	 */
	public void onLocationChanged(Location loc);

	/**
	 * Informs all receivers that a GPS provider is disabled.
	 * @param provider The name of the provider that has been disabled
	 */
	public void onProviderDisabled(String provider);

	/**
	 * Informs all receivers that a GPS provider is enabled.
	 * @param provider The name of the provider that has been enabled
	 */
	public void onProviderEnabled(String provider);

	/**
	 * Registers a change in sensor data.
	 * @param event Object containing new data--sensor type, accuracy, timestamp and value.
	 */
	public void onSensorChanged(SensorEvent event);

	/**
	 * Informs all receivers that a GPS provider's status has changed.
	 * @param provider The name of the provider whose status has changed
	 * @param status The new status of the provider
	 * @param extras Provider-specific extra data
	 */
	public void onStatusChanged(String provider, int status, Bundle extras);

	/**
	 * Registers a receivable for certain status-related updates, especially a NavigationImpl object.
	 * @param rec The receivable to register.
	 * @see NavigationImpl NavigationImpl
	 */
	public void registerReceiver(Receivable rec);
	
	/**
	 * Sets the motor speed data to the supplied array.
	 * @param mySpeeds The data to which the motor speeds should be set.
	 */
	public void setMotorFields(double[] mySpeeds);
	
	/**
	 * Sets the motor power levels to the supplied array.
	 * @param myPowers The data to which the motor power levels should be set.
	 */
	public void setMotorPowerFields(final double[] myPowers);

}