package org.haldean.chopper.pilot;

import java.util.LinkedList;
import java.util.ListIterator;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

/**
 * Sends regular chopper status reports to all registered receivers. <P>
 * 
 * May send the following messages to registered Receivables:<br>
 * <pre>
 * ORIENT:&lt;azimuth&gt;:&lt;pitch&gt;:&lt;roll&gt;
 * ACCEL:&lt;x_acceleration&gt;:&lt;y_acceleration&gt;:&lt;z_acceleration&gt;
 * FLUX:&lt;x_flux&gt;:&lt;y_flux&gt;:&lt;z_flux&gt;
 * MOTORSPEED:&lt;speed_north&gt;:&lt;speed_south&gt;:&lt;speed_east&gt;:&lt;speed_west&gt;
 * LIGHT:&lt;light&gt;
 * PROXIMITY:&lt;proximity&gt;
 * PRESSURE:&lt;pressure&gt;
 * TEMPERATURE:&lt;temperature&gt;
 * BATTERY:&lt;battery_level&gt;
 * GPS:&lt;altitude&gt;:&lt;bearing&gt;:&lt;longitude&gt;:&lt;latitude&gt;:&lt;speed&gt;:&lt;delta_altitude&gt;:&lt;accuracy&gt;:&lt;number_of_satellites&gt;:&lt;timestamp&gt;
 * </pre>
 * 
 * @author Benjamin Bardin
 */
public class StatusReporter implements Runnable, Constants {
	
	/** How often (in ms) status updates should be sent by ChopperStatus to the server */
	public int updateInterval = 350;
	
	/** Tag for logging */
	public static final String TAG = "chopper.StatusReporter";
	
	/** The ChopperStatus from which to compile status reports */
	private ChopperStatus mStatus;
	
	/** List of registered receivers */
	private LinkedList<Receivable> mRec;
	
	/** Handles task scheduling */
	private Handler mHandler;
	
	/**
	 * Constructs the object.
	 * @param status The ChopperStatus from which to compile status reports
	 */
	public StatusReporter(ChopperStatus status) {
		mRec = new LinkedList<Receivable>();
		mStatus = status;
	}
	
	 /**
	  * Obtains a list of strings embodying a status report.
	  * @return The status report
	  */
	public LinkedList<String> getStatusReport() {
		LinkedList<String> infoList = new LinkedList<String>();
		
		/* Lock data, send it, unlock.  If the lock is unavailable (unlikely), skip this datapiece for this iteration */
		try {
			double myAzimuth = mStatus.getReadingFieldNow(AZIMUTH);
			double myPitch = mStatus.getReadingFieldNow(PITCH);
			double myRoll = mStatus.getReadingFieldNow(ROLL);
			infoList.add("ORIENT:" + myAzimuth + ":" + myPitch + ":" + myRoll);
		}
		catch (IllegalAccessException e) {
			Log.w(TAG, "Orientation Report Unavailable");
		}
		
		try {
			double myXaccel = mStatus.getReadingFieldNow(X_ACCEL);
			double myYaccel = mStatus.getReadingFieldNow(Y_ACCEL);
			double myZaccel = mStatus.getReadingFieldNow(Z_ACCEL);
			infoList.add("ACCEL:" + myXaccel + ":" + myYaccel + ":" + myZaccel);
		}
		catch (IllegalAccessException e) {
			Log.w(TAG, "Acceleration Report Unavailable");
		}

		/*try {
			double myXflux = mStatus.getReadingFieldNow(X_FLUX);
			double myYflux = mStatus.getReadingFieldNow(Y_FLUX);
			double myZflux = mStatus.getReadingFieldNow(Z_FLUX);
			infoList.add("FLUX:" + myXflux + ":" + myYflux + ":" + myZflux);
		}
		catch (IllegalAccessException e) {
			Log.w(TAG, "Flux Report Unavailable");
		}*/
		
		try {
			double[] mySpeeds = mStatus.getMotorFieldsNow();
			infoList.add("MOTORSPEED:" + mySpeeds[0] +
					":" + mySpeeds[1] +
					":" + mySpeeds[2] +
					":" + mySpeeds[3]);
		}
		catch (IllegalAccessException e) {
			Log.w(TAG, "MotorSpeeds Report Unavailable");
		}
		/*
		try {
			double[] myPowers = mStatus.getMotorFieldsNow();
			infoList.add("MOTORPOWER:" + myPowers[0] +
					":" + myPowers[1] +
					":" + myPowers[2] +
					":" + myPowers[3]);
		}
		catch (IllegalAccessException e) {
			Log.w(TAG, "MotorPowers Report Unavailable");
		}
		try {
			double myLight = mStatus.getReadingFieldNow(LIGHT);
			infoList.add("LIGHT:" + myLight);
		}
		catch (IllegalAccessException e) {
			Log.w(TAG, "Light Report Unavailable");
		}
		
		try {
			double myProximity = mStatus.getReadingFieldNow(PROXIMITY);
			infoList.add("PROXIMITY:" + myProximity);
		}
		catch (IllegalAccessException e) {
			Log.w(TAG, "Proximity Report Unavailable");
		}
		
		try {
			double myPressure = mStatus.getReadingFieldNow(PRESSURE);
			infoList.add("PRESSURE:" + myPressure);
		}
		catch (IllegalAccessException e) {
			Log.w(TAG, "Pressure Report Unavailable");
		}
		*/
		try {
			double myTemp = mStatus.getReadingFieldNow(TEMPERATURE);
			infoList.add("TEMPERATURE:" + myTemp);
		}
		catch (IllegalAccessException e) {
			Log.w(TAG, "Temperature Report Unavailable");
		}
		
		infoList.add("BATTERY:" + mStatus.getBatteryLevel());
		
		/* Send GPS data */
		String gpsData = new String("GPS");
		try {
			for (int i = 0; i < GPS_FIELDS; i++) {
				double myValue = mStatus.getGpsFieldNow(i);
				gpsData += ":" + myValue;
			}
			
			gpsData += ":" + mStatus.getGpsExtrasNow();			
			infoList.add(gpsData);
		}
		catch (IllegalAccessException e) {
			Log.w(TAG, "GPS Report Unavailable");
		}
		
		return infoList;
	}
	
	/**
	 * Registers a receiver to receive status reports, especially a Comm object.
	 * @param rec The Receivable to register.
	 * @see Comm Comm
	 */
	public void registerReceiver(Receivable rec) {
		synchronized (mRec) {
			mRec.add(rec);
		}
	}
	
	/**
	 * Initializes the handler, schedules the status report update.
	 */
	public void run() {
		Thread.currentThread().setName("StatusReporter");
		Looper.prepare();
		mHandler = new Handler() {
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case STATUS_UPDATE:
					sendStatusUpdate();
					break;
				}
			}
		};
		
		mHandler.sendEmptyMessage(STATUS_UPDATE);
		Looper.loop();
	}
	
	/**
	 * Compiles, sends a status report to all receivers.
	 */
	private void sendStatusUpdate() {
		long starttime = System.currentTimeMillis(); //to ensure that messages are sent no faster than UPDATEINTERVAL
		
		LinkedList<String> infoList = getStatusReport();
		ListIterator<String> strList = infoList.listIterator();
		while (strList.hasNext()) {
			updateReceivers(strList.next());
		}
		/* Ensure loop time is no faster than UPDATEINTERVAL */
		long endtime = System.currentTimeMillis();
		
		/* Schedule the next status update */
		long timetonext = updateInterval - (endtime - starttime);
		if (timetonext > 0)
			mHandler.sendEmptyMessageDelayed(STATUS_UPDATE, timetonext);
		else
			mHandler.sendEmptyMessage(STATUS_UPDATE);
	}
	
	/**
	 * Updates all receivers.
	 * @param str The message to send.
	 */
	private void updateReceivers(String str) {
		synchronized (mRec) {
			ListIterator<Receivable> myList = mRec.listIterator();
			while (myList.hasNext()) {
				myList.next().receiveMessage(str, null);
			}
		}
	}
}
