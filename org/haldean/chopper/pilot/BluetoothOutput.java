package org.haldean.chopper.pilot;

import java.io.IOException;
import java.io.PrintStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

public class BluetoothOutput {
	private static String btAddress = "00:06:66:04:B1:BE";
	private BluetoothSocket btSocket;
	private PrintStream outputStream;
	
	public BluetoothOutput() throws IOException {
		Log.e("ChopperBluetooth", "Starting Bluetooth");
		BluetoothDevice btDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(btAddress);
		Log.e("ChopperBluetooth", "Device Acquired");
		btSocket = btDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
		Log.e("ChopperBluetooth", "Socket Created");
		outputStream = new PrintStream(btSocket.getOutputStream());
		Log.e("ChopperBluetooth", "Output Stream Created");
	}

	public void setMotorSpeeds(int m1, int m2, int m3, int m4) {
		Log.e("ChopperBluetooth", "Writing " + m1 + " " + m2 + " " + m3 + " " + m4);
		outputStream.print(m1 + " " + m2 + " " + m3 + " " + m4 + "\r");
	}
}
