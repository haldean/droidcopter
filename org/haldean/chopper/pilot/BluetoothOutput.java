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
    private static UUID btUuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    
    private BluetoothSocket btSocket;
    private PrintStream outputStream;
	
    public BluetoothOutput() throws IOException {
    	Log.e("ChopperBluetooth", "Starting Bluetooth");
    	BluetoothDevice btDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(btAddress);
    	Log.e("ChopperBluetooth", "Device Acquired");
    	btSocket = btDevice.createRfcommSocketToServiceRecord(btUuid);
    	Log.e("ChopperBluetooth", "Socket Created");
    	btSocket.connect();
    	Log.e("ChopperBluetooth", "And we're connected!");
    	outputStream = new PrintStream(btSocket.getOutputStream());
    	Log.e("ChopperBluetooth", "Output Stream Created");
    	outputStream.print("Hello there, good sir");
    	Log.e("ChopperBluetooth", "Wrote string to output.");
    	setMotorSpeeds(1d, 1d, 1d, 1d);
    }

    public void setMotorSpeeds(double m1, double m2, double m3, double m4) {
    	String message = String.format("%03d%03d%03d%03d", m1, m2, m3, m4);
    	Log.e("ChopperBluetooth", "Writing " + message);
    	outputStream.print(message);
    }
}
