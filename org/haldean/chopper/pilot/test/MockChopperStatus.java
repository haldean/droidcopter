package org.haldean.chopper.pilot.test;

import java.util.Arrays;

import org.haldean.chopper.pilot.ChopperStatus;
import org.haldean.chopper.pilot.Constants;
import org.haldean.chopper.pilot.Receivable;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.location.Location;
import android.os.Bundle;

public class MockChopperStatus implements ChopperStatus, Constants {
	/** Holds GPS data. */
	private double[] mGps = new double[GPS_FIELDS]; //last available GPS readings
	
	/** Timestamp of last GPS reading. */
	private long mGpsTimeStamp; //timestamp of the reading
	
	/** Stores the speeds last submitted to the motors. */
	private double[] mMotorSpeed = new double[4];
	
	/** Holds data from various sensors, like gyroscope, acceleration and magnetic flux. */
	private double[] mReading = new double[SENSORS]; //last known data for a given sensor
	
	public MockChopperStatus() {
		reset();
	}
	
	@Override
	public void close() {
	}

	@Override
	public float getBatteryLevel() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getGpsExtras() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double getGpsField(int whichField) {
		return mGps[whichField];
	}

	@Override
	public long getGpsTimeStamp() {
		return mGpsTimeStamp;
	}

	@Override
	public Location getLastLocation() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void getMotorFields(double[] myValues) {
		System.arraycopy(mMotorSpeed, 0, myValues, 0, 4);

	}

	@Override
	public void getMotorPowerFields(double[] myValues) {
		// TODO Auto-generated method stub

	}

	@Override
	public double getReadingField(int whichField) {
		return mReading[whichField];
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int newaccuracy) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onLocationChanged(Location loc) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub

	}

	@Override
	public void registerReceiver(Receivable rec) {
		// TODO Auto-generated method stub

	}
	
	public void reset() {
		Arrays.fill(mReading, 0.0);
		Arrays.fill(mMotorSpeed, 0.0);
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub

	}

	public void setGpsField(int whichfield, int value) {
		mGps[whichfield] = value;
	}
	
	public void setGpsTimeStamp(long time) {
		mGpsTimeStamp = time;
	}
	
	@Override
	public void setMotorFields(double[] mySpeeds) {
		System.arraycopy(mySpeeds, 0, mMotorSpeed, 0, 4);
	}

	@Override
	public void setMotorPowerFields(double[] myPowers) {
		// TODO Auto-generated method stub

	}
	
	public void setReadingField(int whichfield, int value) {
		mReading[whichfield] = value;
	}
}
