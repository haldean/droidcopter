package org.haldean.chopper.pilot;

import android.os.Message;

public interface BluetoothOutput {

	public static final String TAG = "BluetoothOutputImpl";

	/**
	 * Initialize, start the message handler.
	 */
	public void run();
	
	public void sendMessageToHandler(Message msg); 
}