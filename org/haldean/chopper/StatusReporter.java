package org.haldean.chopper;

import java.util.LinkedList;
import java.util.ListIterator;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

/**
 * Sends regular chopper status reports to all registered receivers
 * @author Benjamin Bardin
 */
public class StatusReporter implements Runnable, Constants {
	private ChopperStatus mStatus;
	private LinkedList<Receivable> mRec;
	private Handler mHandler;
	public static final String TAG = "chopper.StatusReporter";
	
	/** How often (in ms) status updates should be sent by ChopperStatus to the server */
	public int updateInterval = 500;
	
	public StatusReporter(ChopperStatus status) {
		mStatus = status;
		mRec = new LinkedList<Receivable>();
	}
	
	public void registerReceiver(Receivable rec) {
		synchronized (mRec) {
			mRec.add(rec);
		}
	}
	
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

		try {
			double myXflux = mStatus.getReadingFieldNow(X_FLUX);
			double myYflux = mStatus.getReadingFieldNow(Y_FLUX);
			double myZflux = mStatus.getReadingFieldNow(Z_FLUX);
			infoList.add("FLUX:" + myXflux + ":" + myYflux + ":" + myZflux);
		}
		catch (IllegalAccessException e) {
			Log.w(TAG, "Flux Report Unavailable");
		}
		
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
	
	private void updateReceivers(String str) {
		synchronized (mRec) {
			ListIterator<Receivable> myList = mRec.listIterator();
			while (myList.hasNext()) {
				myList.next().receiveMessage(str, null);
			}
		}
	}
	
	public void run() {
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
}
