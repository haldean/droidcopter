package org.haldean.chopper.pilot;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import at.abraxas.amarino.Amarino;
import at.abraxas.amarino.AmarinoIntent;
import at.abraxas.amarino.AmarinoService;

public class BluetoothOutput implements Constants, Runnable {
    private ChopperMain context;
    private volatile boolean initialized;
    static final String BT_DEVICE_ADDR = "00:06:66:04:B1:BE";
    public Handler mHandler;
    private AmarinoService amService;
    private ServiceConnection mConnection;
    public static final String TAG = "BluetoothOutput";
    
    public BluetoothOutput(ChopperMain context) {
    	this.context = context;
    	initialized = true;
    	mConnection = new ServiceConnection() {
    	    public void onServiceConnected(ComponentName className, IBinder service) {
    	    	Log.i(TAG, "binder data: " + service.toString());
    	    	/*try {
    	    		Log.i(TAG, "binder name: " + service.getInterfaceDescriptor());
    	    	}
    	    	catch (RemoteException e) {
    	    		Log.e(TAG, "Remote exception. what?");
    	    		e.printStackTrace();
    	    	}*/
    	    	if (service instanceof AmarinoService.AmarinoServiceBinder) {
    	    		Log.i(TAG, "binder is as expected.");
    	    	}
    	    	else
    	    		Log.i(TAG, "binder is not and amserbind");
    	    	if (service instanceof Binder)
    	    		Log.i(TAG, "binder is a Binder");
    	    	else
    	    		Log.i(TAG, "binder is not a Binder");
    	    	amService = ((AmarinoService.AmarinoServiceBinder)service).getService();
    	    	Log.i(TAG, "binding initialized");
    	    }

    	    public void onServiceDisconnected(ComponentName className) {
    	    	amService = null;
    	    	Log.e(TAG, "Lost binding to Amarino Service");
    	    }
    	};
    }
    
    public void run() {
    	Looper.prepare();
		Thread.currentThread().setName("BluetoothOutput");
		/* Intent intent = new Intent(AmarinoIntent.ACTION_SEND);
		boolean isbound = context.bindService(intent, mConnection, Activity.BIND_AUTO_CREATE);
		Log.i(TAG, "binding success: " + isbound); */
		mHandler = new Handler() {
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case SEND_MOTOR_SPEEDS:
					double[] newspeeds = (double[])msg.obj;
					setMotorSpeeds(newspeeds[0], newspeeds[1], newspeeds[2], newspeeds[3]);
					break;
				}
			}
		};
		Looper.loop();
    }
    
    public boolean initialized() {
    	return initialized;
    }

    public void setMotorSpeeds(double m1, double m2, double m3, double m4) {
	sendDataToArduino(context, BT_DEVICE_ADDR, 'A', (int) (100 * m1));
	sendDataToArduino(context, BT_DEVICE_ADDR, 'C', (int) (100 * m2));
	sendDataToArduino(context, BT_DEVICE_ADDR, 'B', (int) (100 * m3));
	sendDataToArduino(context, BT_DEVICE_ADDR, 'D', (int) (100 * m4));
    }
    
    /**
	 * Sends an int value to Arduino
	 * 
	 * @param context the context
	 * @param address the Bluetooth device you want to send data to
	 * @param flag the flag Arduino has registered a function for to receive this data
	 * @param data your data you want to send
	 */
	private void sendDataToArduino(Context context, String address, char flag, int data) {
		Intent intent = getSendIntent(address, AmarinoIntent.INT_EXTRA, flag);
		intent.putExtra(AmarinoIntent.EXTRA_DATA, data);
		amService = Amarino.getAmarinoService();
		if (amService != null) {
			Log.v(TAG, "sending to amarino");
			amService.onStart(intent, 0); //Pretty sure that last 0 can be anything.
		}
		else
			Log.e(TAG, "amarino service is null");
	}
	
    private static Intent getSendIntent(String address, int dataType, char flag){
		Intent intent = new Intent(AmarinoIntent.ACTION_SEND);
		intent.putExtra(AmarinoIntent.EXTRA_DEVICE_ADDRESS, address);
		intent.putExtra(AmarinoIntent.EXTRA_DATA_TYPE, dataType);
		intent.putExtra(AmarinoIntent.EXTRA_FLAG, flag);
		return intent;
	}
}