package org.haldean.chopper.pilot;

import at.abraxas.amarino.Amarino;

public class BluetoothOutput {
    private static ChopperMain context;
    private static volatile boolean initialized;
    static final String BT_DEVICE_ADDR = "00:06:66:04:B1:BE";

    private BluetoothOutput() {
    	// Screw you and your non-utility classes!
    }
    
    public static void setContext(ChopperMain context) {
	BluetoothOutput.context = context;
	initialized = true;
    }
    
    public static boolean initialized() {
    	return initialized;
    }

    public static void setMotorSpeeds(double m1, double m2, double m3, double m4) {
	Amarino.sendDataToArduino(context, BT_DEVICE_ADDR, 'A', (int) (100 * m1));
	Amarino.sendDataToArduino(context, BT_DEVICE_ADDR, 'B', (int) (100 * m2));
	Amarino.sendDataToArduino(context, BT_DEVICE_ADDR, 'C', (int) (100 * m3));
	Amarino.sendDataToArduino(context, BT_DEVICE_ADDR, 'D', (int) (100 * m4));
    }
}