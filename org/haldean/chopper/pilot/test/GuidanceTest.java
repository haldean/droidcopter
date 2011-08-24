package org.haldean.chopper.pilot.test;

import org.haldean.chopper.pilot.BluetoothOutput;
import org.haldean.chopper.pilot.Constants;
import org.haldean.chopper.pilot.Guidance;

import android.os.Message;
import android.test.AndroidTestCase;
import android.util.Log;

public class GuidanceTest extends AndroidTestCase implements Constants {
	public static final String TAG = "chopper.GuidanceTest";
	
	private Guidance guid;
	private MockChopperStatus mCs = new MockChopperStatus();
	private MockAngler mAngler = new MockAngler();
	
	public void setUp() throws Exception {
		mCs.reset();
		mAngler.reset();
		guid = new Guidance(mCs,
							new BluetoothOutput() {
								public void run() {}
								public void sendMessageToHandler(Message msg) {}
							},
							mAngler);
		
	}

	public void tearDown() throws Exception {
		
	}
	
	public void testFacingNorth() throws Exception {
		guid.receiveMessage("GUID:AUTOPILOT", null);
		double[] angleTarget = new double[4];
		double[] oldSpeeds = new double[4];
		double[] newSpeeds = new double[4];
		double[] motorTarget = new double[4];
		
		Log.v(TAG, " ");
		Log.v(TAG, "10 degrees to the right");
		angleTarget[0] = 10.0;
		mAngler.setAngleTarget(angleTarget);
		incrementGpsTimeStamp();
		
		motorTarget[3] = 1.0;
		motorTarget[2] = 0.0;
		checkAngles(oldSpeeds, newSpeeds, motorTarget);
		
		Log.v(TAG, " ");
		Log.v(TAG, "10 degrees to the right, pitch 5");
		mCs.setReadingField(PITCH, 5);
		angleTarget[0] = 10.0;
		mAngler.setAngleTarget(angleTarget);
		incrementGpsTimeStamp();
		motorTarget[0] = 1.0;
		motorTarget[3] = 1.0;
		motorTarget[2] = 0.0;
		checkAngles(oldSpeeds, newSpeeds, motorTarget);
		
		Log.v(TAG, " ");
		Log.v(TAG, "10 degrees to the right");
		mCs.setReadingField(ROLL, 5);
		angleTarget[0] = 10.0;
		mAngler.setAngleTarget(angleTarget);
		incrementGpsTimeStamp();
		motorTarget[3] = 1.0;
		motorTarget[2] = 0.0;
		checkAngles(oldSpeeds, newSpeeds, motorTarget);
		
		Log.v(TAG, " ");
		Log.v(TAG, "10 degrees to the right");
		mCs.setReadingField(PITCH, -5);
		angleTarget[0] = 10.0;
		mAngler.setAngleTarget(angleTarget);
		incrementGpsTimeStamp();
		motorTarget[3] = 1.0;
		motorTarget[2] = 0.0;
		motorTarget[0] = 0.0;
		motorTarget[1] = 1.0;
		checkAngles(oldSpeeds, newSpeeds, motorTarget);
		
		Log.v(TAG, " ");
		Log.v(TAG, "10 degrees to the right");
		mCs.setReadingField(ROLL, -5);
		angleTarget[0] = 10.0;
		mAngler.setAngleTarget(angleTarget);
		incrementGpsTimeStamp();
		motorTarget[3] = 1.0;
		motorTarget[2] = 0.0;
		checkAngles(oldSpeeds, newSpeeds, motorTarget);
		
		
		Log.v(TAG, " ");
		Log.v(TAG, "10 degrees to the right");
		mCs.setReadingField(ROLL, 0);
		mCs.setReadingField(PITCH, 0);
		angleTarget[0] = 10.0;
		mAngler.setAngleTarget(angleTarget);
		incrementGpsTimeStamp();
		motorTarget[3] = 1.0;
		motorTarget[2] = 0.0;
		checkAngles(oldSpeeds, newSpeeds, motorTarget);

		Log.v(TAG, " ");
		Log.v(TAG, "10 degrees to the left");
		angleTarget[0] = -10.0;
		mAngler.setAngleTarget(angleTarget);
		incrementGpsTimeStamp();
		motorTarget[2] = 1.0;
		motorTarget[3] = 0.0;
		checkAngles(oldSpeeds, newSpeeds, motorTarget);
		
		Log.v(TAG, " ");
		Log.v(TAG, "10 degrees to the left");
		mCs.setReadingField(ROLL, 5);
		angleTarget[0] = -10.0;
		mAngler.setAngleTarget(angleTarget);
		incrementGpsTimeStamp();
		motorTarget[2] = 1.0;
		motorTarget[3] = 0.0;
		checkAngles(oldSpeeds, newSpeeds, motorTarget);
		
		Log.v(TAG, " ");
		Log.v(TAG, "10 degrees to the left");
		mCs.setReadingField(ROLL, -5);
		angleTarget[0] = -10.0;
		mAngler.setAngleTarget(angleTarget);
		incrementGpsTimeStamp();
		motorTarget[2] = 1.0;
		motorTarget[3] = 0.0;
		checkAngles(oldSpeeds, newSpeeds, motorTarget);

		Log.v(TAG, " ");
		Log.v(TAG, "10 degrees forward, 10 degrees to the left");
		angleTarget[1] = 10.0;
		mAngler.setAngleTarget(angleTarget);
		incrementGpsTimeStamp();
		motorTarget[1] = 1.0;
		motorTarget[0] = 0.0;
		checkAngles(oldSpeeds, newSpeeds, motorTarget);

		Log.v(TAG, " ");
		Log.v(TAG, "10 degrees backward, 10 degrees to the left");
		angleTarget[1] = -10.0;
		mAngler.setAngleTarget(angleTarget);
		incrementGpsTimeStamp();
		motorTarget[1] = 0.0;
		motorTarget[0] = 1.0;
		checkAngles(oldSpeeds, newSpeeds, motorTarget);

		Log.v(TAG, " ");
		Log.v(TAG, "10 Up");
		angleTarget[0] = 0.0;
		angleTarget[1] = 0.0;
		angleTarget[2] = 10.0;
		mAngler.setAngleTarget(angleTarget);
		incrementGpsTimeStamp();
		motorTarget[0] = 1.0;
		motorTarget[1] = 1.0;
		motorTarget[2] = 1.0;
		motorTarget[3] = 1.0;
		checkAngles(oldSpeeds, newSpeeds, motorTarget);

		Log.v(TAG, " ");
		Log.v(TAG, "10 Down");
		angleTarget[2] = -10.0;
		mAngler.setAngleTarget(angleTarget);
		incrementGpsTimeStamp();
		motorTarget[0] = 1.0;  // Residue from back-left command.
		motorTarget[1] = 0.0;
		motorTarget[2] = 1.0;  // Residue from back-left command.
		motorTarget[3] = 0.0;
		checkAngles(oldSpeeds, newSpeeds, motorTarget);
	}
	
	public void testRotation() throws Exception {
		guid.receiveMessage("GUID:AUTOPILOT", null);
		double[] angleTarget = new double[4];
		double[] oldSpeeds = new double[4];
		double[] newSpeeds = new double[4];
		double[] motorTarget = new double[4];
		
		mCs.setReadingField(AZIMUTH, 45);
		motorTarget[0] = 0.0;
		motorTarget[1] = 0.0;
		motorTarget[2] = 1.0;
		motorTarget[3] = 1.0;
		checkAngles(oldSpeeds, newSpeeds, motorTarget);
		
		mCs.setReadingField(AZIMUTH, 135);
		motorTarget[0] = 0.0;
		motorTarget[1] = 0.0;
		motorTarget[2] = 1.0;
		motorTarget[3] = 1.0;
		checkAngles(oldSpeeds, newSpeeds, motorTarget);
		
		mCs.setReadingField(AZIMUTH, -45);
		motorTarget[0] = 1.0;
		motorTarget[1] = 1.0;
		motorTarget[2] = 0.0;
		motorTarget[3] = 0.0;
		checkAngles(oldSpeeds, newSpeeds, motorTarget);
		
		mCs.setReadingField(AZIMUTH, -135);
		motorTarget[0] = 1.0;
		motorTarget[1] = 1.0;
		motorTarget[2] = 0.0;
		motorTarget[3] = 0.0;
		checkAngles(oldSpeeds, newSpeeds, motorTarget);
		
		mCs.setReadingField(AZIMUTH, 225);
		motorTarget[0] = 1.0;
		motorTarget[1] = 1.0;
		motorTarget[2] = 0.0;
		motorTarget[3] = 0.0;
		checkAngles(oldSpeeds, newSpeeds, motorTarget);
		
		mCs.setReadingField(AZIMUTH, 0);
		angleTarget[3] = -45.0;
		mAngler.setAngleTarget(angleTarget);
		incrementGpsTimeStamp();
		motorTarget[0] = 0.0;
		motorTarget[1] = 0.0;
		motorTarget[2] = 1.0;
		motorTarget[3] = 1.0;
		checkAngles(oldSpeeds, newSpeeds, motorTarget);
		
		mCs.setReadingField(AZIMUTH, 0);
		angleTarget[3] = 45.0;
		mAngler.setAngleTarget(angleTarget);
		incrementGpsTimeStamp();
		motorTarget[0] = 1.0;
		motorTarget[1] = 1.0;
		motorTarget[2] = 0.0;
		motorTarget[3] = 0.0;
		checkAngles(oldSpeeds, newSpeeds, motorTarget);
	}
	
	private void checkAngles(double[] oldSpeed, double[] newSpeed, double[] finalSpeed) {
		for (int j = 0; j < 160; j++) {
			mCs.getMotorFields(oldSpeed);
			guid.reviseMotorSpeed();
			mCs.getMotorFields(newSpeed);
			logArray("oldSpeed", oldSpeed);
			logArray("newSpeed", newSpeed);
			logArray("finalSpeed", finalSpeed);
			for (int i = 0; i < finalSpeed.length; i++) {
				if (finalSpeed[i] == oldSpeed[i]) {
					assertEquals(finalSpeed[i], newSpeed[i]);
				} else if (finalSpeed[i] > oldSpeed[i]) {
					assertTrue(newSpeed[i] >= oldSpeed[i]);
					assertTrue(finalSpeed[i] >= newSpeed[i]);
				} else {
					assertTrue(newSpeed[i] <= oldSpeed[i]);
					assertTrue(finalSpeed[i] <= newSpeed[i]);
				}
			}
		}
		assertArrayEquals(finalSpeed, newSpeed);
	}
	
	private static void logArray(String id, double[] array) {
		String output = id + ": ";
		for (int i = 0; i < array.length; i++) {
			output += array[i] + ", ";
		}
		Log.v(TAG, output);
	}
	
	private static void assertArrayEquals(double[] expected, double[] actual) {
		assertEquals(expected.length, actual.length);
		for (int i = 0; i < expected.length; i++) {
			assertEquals(expected[i], actual[i]);
		}
	}
	
	private void incrementGpsTimeStamp() {
		mCs.setGpsTimeStamp(mCs.getGpsTimeStamp() + 1);
	}
}
