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

public final class ChopperStatus extends Thread implements SensorEventListener, Constants, LocationListener
{	
	public static double[] reading = new double[NUMSENSORS]; //last known data for a given sensor
	public static ReentrantLock[] readinglock = new ReentrantLock[NUMSENSORS];
	
	public static int[] accuracy = new int[NUMSENSORS]; //last known accuracy for a given sensor
	public static long[] timestamp = new long[NUMSENSORS]; //timestamp in nanos of last reading
	
	public static double[] gps = new double[GPSFIELDS]; //last available GPS readings
	public static float gpsaccuracy; //accuracy of said reading
	public static long gpstimestamp; //timestamp of the reading
	public static int gpsnumsats; //number of satellites used to collect last reading
	
	public static float currbattery = 0;
	public static float maxbattery = 100;
	
	private static Context context; //used to get location manager.
	//private static LocationManager LocMan; //manages locations
	//private static BroadcastReceiver batteryInfo;
	
	public static Handler mHandler; //Message handler.
	
	public ChopperStatus(Context mycontext)	{
		super("Chopper Status");
		context = mycontext;
	}
	
	private static void sendUpdate()
	{		
		//System.out.println("Should be sending update");
		long starttime = System.currentTimeMillis(); //to ensure that messages are sent no faster than UPDATEINTERVAL
		
		Comm.sendMessage("ORIENT:" + reading[AZIMUTH] +
						":" + reading[PITCH] + 
						":" + reading[ROLL]);
		Comm.sendMessage("ACCEL:" + reading[XACCEL] +
						":" + reading[YACCEL] +
						":" + reading[ZACCEL]);
		Comm.sendMessage("FLUX:" + reading[MAG1] +
						":" + reading[MAG2] +
						":" + reading[MAG3]);
		Comm.sendMessage("MOTORSPEED:" + Guidance.motorspeed[0] +
				":" + Guidance.motorspeed[1] +
				":" + Guidance.motorspeed[2] +
				":" + Guidance.motorspeed[3]);
		Comm.sendMessage("LIGHT:" + reading[LIGHT]);
		Comm.sendMessage("PROXIMITY:" + reading[PROXIMITY]);
		Comm.sendMessage("PRESSURE:" + reading[PRESSURE]);
		Comm.sendMessage("TEMPERATURE:" + reading[TEMPERATURE]);
		Comm.sendMessage("BATTERY:" + (float) (currbattery/maxbattery));
		//Send GPS data
		String gpsData = new String("GPS");
		for (int i = 0; i < GPSFIELDS; i++) {
			gpsData += ":" + ChopperStatus.gps[i];
		}
		gpsData += ":" + ChopperStatus.gpsaccuracy + 
			":" + ChopperStatus.gpsnumsats +
			":" + ChopperStatus.gpstimestamp;
		Comm.sendMessage(gpsData);
		
		//Ensure loop time is no faster than UPDATEINTERVAL
		long endtime = System.currentTimeMillis();
		
		long timetonext = UPDATEINTERVAL - (endtime - starttime);
		if (timetonext > 0)
			mHandler.sendEmptyMessageDelayed(SENDSTATUSUPDATE, timetonext);
	}
	
	public void run()
	{
		System.out.println("ChopperStatus run() thread ID " + getId());
		Looper.prepare(); //don't know what this does.
		
		mHandler = new Handler() {
            public void handleMessage(Message msg)
            {
                switch (msg.what){
                case SENDSTATUSUPDATE:
                		sendUpdate();
                	break;
                }
            }
        };
        BroadcastReceiver batteryInfo = new BroadcastReceiver() {

			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				
				if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
					currbattery = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
					maxbattery = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
					if (currbattery <= LOWBATT)
						Comm.updateAll("SYS:LOWPOWER");
				}
			}
		};
		
        //Gets a sensor manager
		SensorManager sensors = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		
		//Registers this class as a sensor listener for every necessary sensor.
		sensors.registerListener(this, sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
		sensors.registerListener(this, sensors.getDefaultSensor(Sensor.TYPE_LIGHT), SensorManager.SENSOR_DELAY_NORMAL);
		sensors.registerListener(this, sensors.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL);
		sensors.registerListener(this, sensors.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_FASTEST);
		sensors.registerListener(this, sensors.getDefaultSensor(Sensor.TYPE_PRESSURE), SensorManager.SENSOR_DELAY_NORMAL);
		sensors.registerListener(this, sensors.getDefaultSensor(Sensor.TYPE_PROXIMITY), SensorManager.SENSOR_DELAY_NORMAL);
		sensors.registerListener(this, sensors.getDefaultSensor(Sensor.TYPE_TEMPERATURE), SensorManager.SENSOR_DELAY_NORMAL);
		
		//Initialize GPS reading:
		LocationManager LocMan = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);; //manages locations
		LocMan.requestLocationUpdates(LocationManager.GPS_PROVIDER,	gpsmintime,	gpsmindist,	this);
		
		context.registerReceiver(batteryInfo, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		
		mHandler.sendEmptyMessageDelayed(SENDSTATUSUPDATE, UPDATEINTERVAL);
		Looper.loop();
	}
	
	//On location changed, get new location, store in data fields
	public void onLocationChanged(Location loc) {
		if (loc != null && gps != null) {
			double newalt = loc.getAltitude();
			if (newalt != gps[ALTITUDE]) //vertical velocity does not update until vertical position does; prevents false conclusions that vertical velocity == 0.
				gps[dALT] = (newalt - gps[ALTITUDE]) / (gpstimestamp - loc.getTime()) * 1000.0; //that last 1000 converts from m/ms to m/s
				
			gps[ALTITUDE] = newalt;
			gps[BEARING] = loc.getBearing();
			gps[LONG] = loc.getLongitude();
			gps[LAT] = loc.getLatitude();
			gps[SPEED] = loc.getSpeed();
			
			gpsaccuracy = loc.getAccuracy();
			gpstimestamp = loc.getTime();
			if (loc.getExtras() != null)
				gpsnumsats = loc.getExtras().getInt("satellites");
		}
	}
	
	//Update datafields as required.
	public void onAccuracyChanged(Sensor sensor, int newaccuracy) {
		int type = sensor.getType();
		switch (type) {
			case Sensor.TYPE_ACCELEROMETER: 
				accuracy[XACCEL] = newaccuracy;
				accuracy[YACCEL] = newaccuracy;
				accuracy[ZACCEL] = newaccuracy;
				break;
			case Sensor.TYPE_LIGHT:
				accuracy[LIGHT] = newaccuracy;
				break;
			case Sensor.TYPE_MAGNETIC_FIELD:
				accuracy[MAG1] = newaccuracy;
				accuracy[MAG2] = newaccuracy;
				accuracy[MAG3] = newaccuracy;
				break;
			case Sensor.TYPE_ORIENTATION:
				accuracy[AZIMUTH] = newaccuracy;
				accuracy[PITCH] = newaccuracy;
				accuracy[ROLL] = newaccuracy;
				break;
			case Sensor.TYPE_PRESSURE:
				accuracy[PRESSURE] = newaccuracy;
				break;
			case Sensor.TYPE_PROXIMITY:
				accuracy[PROXIMITY] = newaccuracy;
				break;
			case Sensor.TYPE_TEMPERATURE:
				accuracy[TEMPERATURE] = newaccuracy;
				break;
		}
	}
	
	//Update datafields as required.
	public void onSensorChanged(SensorEvent event)
	{
		int type = event.sensor.getType();
		switch (type)
		{
			case Sensor.TYPE_ACCELEROMETER:
				
					
				
				//obtain velocity:
				
				//transform acceleration frame of reference into an absolute frame.
				//build transformation matrix:
				/*float alpha = reading[PITCH];
				float beta = -reading[ROLL];
				float gamma = -reading[AZIMUTH];
				
				Matrix.setRotateEulerM(transformation, 0, alpha, beta, gamma);
				
				float[] newaccel = new float[3];
				float[] augmentedaccel = new float[4];
				for (int i = 0; i < 3; i++) {
					augmentedaccel[i] = event.values[i];
				}
				
				for (int i = 0; i < 3; i++) {
					for (int j = 4 * i; j < i * 4 + 4; j++) {
						newaccel[i] += augmentedaccel[j - 4 * i] * transformation[j];
					}
				}
					*/
				updateField(XACCEL, event.accuracy, event.timestamp, event.values[0]);
				updateField(YACCEL, event.accuracy, event.timestamp, event.values[1]);
				updateField(ZACCEL, event.accuracy, event.timestamp, event.values[2]);
				
				/*if (lastaccelupdate == 0) {
					lastaccelupdate = event.timestamp;
				}
				else {
					double dt =(event.timestamp - lastaccelupdate) / 1000000000.0; //ns to s
					lastaccelupdate = event.timestamp;
					for (int i = 0; i < 3; i++) {
						velocity[i] += dt * reading[XACCEL + i];
					}
				}*/
				
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
	
	//for less typing.
	private static void updateField(int field, int myaccuracy, long mytimestamp, double value)
	{
		accuracy[field] = myaccuracy;
		timestamp[field] = mytimestamp;
		reading[field] = value;
	}
	
	//Need to find a way to turn it back on via code. Don't know that there is one.
	public void onProviderDisabled(String provider)
	{
		//For now, just tells the user there's a problem.
		Comm.sendMessage("MSG:GPS Disabled!");
	}
	
	//Yay.
	public void onProviderEnabled(String provider)
	{
		Comm.sendMessage("MSG:GPS Enabled.");
	}
	
	//Forwards the status update to the user.
	public void onStatusChanged(String provider, int status, Bundle extras)
	{
		switch (status)
		{
		case LocationProvider.OUT_OF_SERVICE:
			Comm.sendMessage("MSG:GPS Out Of Service.");
			break;
		case LocationProvider.TEMPORARILY_UNAVAILABLE:
			Comm.sendMessage("MSG:GPS Temporarily Unavailable.");
			break;
		case LocationProvider.AVAILABLE:
			Comm.sendMessage("MSG:GPS Available.");
			break;
		}
	}
}
