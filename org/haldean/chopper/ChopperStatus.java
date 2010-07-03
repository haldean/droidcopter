package org.haldean.chopper;

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
	public static float[] reading = new float[NUMSENSORS]; //last known data for a given sensor
	public static int[] accuracy = new int[NUMSENSORS]; //last known accuracy for a given sensor
	public static long[] timestamp = new long[NUMSENSORS]; //timestamp in nanos of last reading
	public static int[] velocity = new int[3];
	
	public static double[] gps = new double[GPSFIELDS]; //last available GPS readings
	public static float gpsaccuracy; //accuracy of said reading
	public static long gpstimestamp; //timestamp of the reading
	public static int gpsnumsats; //number of satellites used to collect last reading
	
	public static float currbattery = 0;
	public static float maxbattery = 100;
	
	private static long lastaccelupdate = 0;
	private static double[] transformation = new double[9];
	
	private static Context context; //used to get location manager.
	private static LocationManager LocMan; //manages locations
	private static BroadcastReceiver batteryInfo;
	
	public static Handler mHandler; //Message handler. don't think I actually make this do anything useful. probably should.
	
	public ChopperStatus(Context mycontext)
	{
		super();
		setName("ChopperStatus");
		context = mycontext;
		
		batteryInfo = new BroadcastReceiver() {

			public void onReceive(Context context, Intent intent) {
				System.out.println("Receiving battery info");
				String action = intent.getAction();
				
				if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
					currbattery = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
					maxbattery = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
				}
			}
		};
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
		
		Comm.sendMessage("FLUX:" + reading[FLUX]);
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
		
		//System.out.println(UPDATEINTERVAL - (endtime - starttime));
		mHandler.sendEmptyMessageDelayed(SENDSTATUSUPDATE, Math.max(0, UPDATEINTERVAL - (endtime - starttime)));
	}
	
	public void run()
	{
		System.out.println("ChopperStatus run() thread ID " + getId());
		Looper.prepare(); //don't know what this does.
		
		//should probably use this.
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
		
        //Gets a sensor manager
		SensorManager sensors = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		
		//Registers this class as a sensor listener for every necessary sensor.
		sensors.registerListener(this, sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST);
		sensors.registerListener(this, sensors.getDefaultSensor(Sensor.TYPE_LIGHT), SensorManager.SENSOR_DELAY_NORMAL);
		sensors.registerListener(this, sensors.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL);
		sensors.registerListener(this, sensors.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_FASTEST);
		sensors.registerListener(this, sensors.getDefaultSensor(Sensor.TYPE_PRESSURE), SensorManager.SENSOR_DELAY_NORMAL);
		sensors.registerListener(this, sensors.getDefaultSensor(Sensor.TYPE_PROXIMITY), SensorManager.SENSOR_DELAY_NORMAL);
		sensors.registerListener(this, sensors.getDefaultSensor(Sensor.TYPE_TEMPERATURE), SensorManager.SENSOR_DELAY_NORMAL);
		
		//Sensor listed in the API, not sure how it's different from TYPE_ORIENTATION:
		//sensors.registerListener(this, sensors.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_FASTEST);
		
		//Initialize GPS reading:
		LocMan = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		LocMan.requestLocationUpdates(LocationManager.GPS_PROVIDER,	gpsmintime,	gpsmindist,	this);
		
		context.registerReceiver(batteryInfo, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		
		mHandler.sendEmptyMessageDelayed(SENDSTATUSUPDATE, UPDATEINTERVAL);
		Looper.loop();
	}
	
	//On location changed, get new location, store in data fields
	public void onLocationChanged(Location loc)
	{
		if (loc != null && gps != null) {
			gps[ALTITUDE] = loc.getAltitude();
			gps[BEARING] = (double) loc.getBearing();
			gps[LONG] = loc.getLongitude();
			gps[LAT] = loc.getLatitude();
			gps[SPEED] = (double)loc.getSpeed();

			gpsaccuracy = loc.getAccuracy();
			gpstimestamp = loc.getTime();
			if (loc.getExtras() != null)
				gpsnumsats = loc.getExtras().getInt("satellites");
		}
	}
	
	//Update datafields as required.
	public void onAccuracyChanged(Sensor sensor, int newaccuracy)
	{
		int type = sensor.getType();
		switch (type)
		{
			case Sensor.TYPE_ACCELEROMETER: 
				accuracy[XACCEL] = newaccuracy;
				accuracy[YACCEL] = newaccuracy;
				accuracy[ZACCEL] = newaccuracy;
				break;
			case Sensor.TYPE_LIGHT:
				accuracy[LIGHT] = newaccuracy;
				break;
			case Sensor.TYPE_MAGNETIC_FIELD:
				accuracy[FLUX] = newaccuracy;
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
				double alpha = -reading[PITCH] * Math.PI / 180.0;
				double beta = reading[ROLL] * Math.PI / 180.0;
				double gamma = -reading[AZIMUTH] * Math.PI / 180.0;
				transformation[0] = Math.cos(gamma) * Math.cos(beta);
				transformation[1] = Math.sin(alpha) * Math.sin(beta) * Math.cos(gamma) - Math.sin(gamma) * Math.cos(alpha);
				transformation[2] = Math.cos(alpha) * Math.sin(beta) * Math.cos(gamma) + Math.sin(gamma) * Math.sin(beta);
				transformation[3] = Math.sin(gamma) * Math.cos(beta);
				transformation[4] = Math.sin(alpha) * Math.sin(beta) * Math.sin(gamma) + Math.cos(alpha) * Math.cos(gamma);
				transformation[5] = Math.cos(alpha) * Math.sin(beta) * Math.sin(gamma) - Math.sin(beta) * Math.cos(gamma);
				transformation[6] = -Math.sin(beta);
				transformation[7] = Math.sin(alpha) * Math.cos(beta);
				transformation[8] = Math.cos(alpha) * Math.cos(beta);
				
				int[] newaccel = new int[3]; 
				for (int i = 0; i < 3; i++) {
					for (int j = 3 * i; j < i * 3 + 3; i++) {
						newaccel[i] += event.values[j - 3 * i] * transformation[j];
					}
				}
				
				
				if (lastaccelupdate == 0) {
					lastaccelupdate = event.timestamp;
					break;
				}
				double dt =(event.timestamp - lastaccelupdate) / 1000000000.0; //ns to s
					
				updateField(XACCEL, event.accuracy, event.timestamp, newaccel[0]);
				updateField(YACCEL, event.accuracy, event.timestamp, newaccel[1]);
				updateField(ZACCEL, event.accuracy, event.timestamp, newaccel[2] + SensorManager.STANDARD_GRAVITY);
				break;
			case Sensor.TYPE_LIGHT:
				updateField(LIGHT, event.accuracy, event.timestamp, event.values[0]);
				break;
			case Sensor.TYPE_MAGNETIC_FIELD:
				updateField(FLUX, event.accuracy, event.timestamp, event.values[0]);
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
	private static void updateField(int field, int myaccuracy, long mytimestamp, float value)
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
