package org.haldean.chopper.pilot;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import at.abraxas.amarino.Amarino;
import at.abraxas.amarino.AmarinoIntent;
import at.abraxas.amarino.AmarinoService;

/**
 * Interfaces Pilot with Amarino
 */
public class BluetoothOutput implements Constants, Runnable {
    static final String BT_DEVICE_ADDR = "00:06:66:04:B1:BE";
    public Handler mHandler;
    private AmarinoService amService;
    public static final String TAG = "BluetoothOutput";
    
    /**
     * Initialize, start the message handler.
     */
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
    
    /**
     * Send motor speeds to arduino.
     * @param m1 First motor's speed.
     * @param m2 Second motor's speed.
     * @param m3 Third motor's speed.
     * @param m4 Fourth motor's speed.
     */
    private void setMotorSpeeds(double m1, double m2, double m3, double m4) {
	sendDataToArduino(BT_DEVICE_ADDR, 'A', (int) (100 * m1));
	sendDataToArduino(BT_DEVICE_ADDR, 'C', (int) (100 * m2));
	sendDataToArduino(BT_DEVICE_ADDR, 'B', (int) (100 * m3));
	sendDataToArduino(BT_DEVICE_ADDR, 'D', (int) (100 * m4));
    }
    
    /**
	 * Sends an int value to Arduino
	 * 
	 * @param context the context
	 * @param address the Bluetooth device you want to send data to
	 * @param flag the flag Arduino has registered a function for to receive this data
	 * @param data your data you want to send
	 */
	private void sendDataToArduino(String address, char flag, int data) {
		Intent intent = getSendIntent(address, AmarinoIntent.INT_EXTRA, flag);
		intent.putExtra(AmarinoIntent.EXTRA_DATA, data);
		amService = Amarino.getAmarinoService();
		if (amService != null) {
			amService.onStart(intent, 0); //Pretty sure that last 0 can be anything.
		}
		//else
			//Log.e(TAG, "amarino service is null");
	}
	
	/**
	 * Form Intent to pass to AmarinoService
	 * @param address Address of the arduino
	 * @param dataType Add this as an "extra."
	 * @param flag Which motor to change.
	 * @return
	 */
    private static Intent getSendIntent(String address, int dataType, char flag){
		Intent intent = new Intent(AmarinoIntent.ACTION_SEND);
		intent.putExtra(AmarinoIntent.EXTRA_DEVICE_ADDRESS, address);
		intent.putExtra(AmarinoIntent.EXTRA_DATA_TYPE, dataType);
		intent.putExtra(AmarinoIntent.EXTRA_FLAG, flag);
		return intent;
	}
}