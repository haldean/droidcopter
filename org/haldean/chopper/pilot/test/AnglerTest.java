package org.haldean.chopper.pilot.test;

import org.haldean.chopper.pilot.AnglerImpl;
import org.haldean.chopper.pilot.Constants;

import android.test.AndroidTestCase;
import android.util.Log;

public class AnglerTest extends AndroidTestCase implements Constants {
	public static final String TAG = "chopper.AnglerTest";
	
	private MockChopperStatus mCs;
	private MockNavigation mNav;
	private AnglerImpl mAngler;
	
	public void setUp() {
		mCs = new MockChopperStatus();
		mNav = new MockNavigation();
		mAngler = new AnglerImpl(mCs, mNav);
	}
	public void testVerticalAndRotation() throws Exception {
		double[] newNavData = new double[4];
		double[] newAngleTarget = new double[4];
		newNavData[2] = 343.1;
		newNavData[3] = 122;
		mNav.setNavTarget(newNavData);
		mAngler.getAngleTarget(newAngleTarget);
		assertEquals(Math.min(AnglerImpl.MAX_VEL, newNavData[2]), newAngleTarget[2]);
		assertEquals(newNavData[3], newAngleTarget[3]);
		
		newNavData[2] = 1.5;
		newNavData[3] = -122;
		mNav.setNavTarget(newNavData);
		mAngler.getAngleTarget(newAngleTarget);
		assertEquals(Math.min(AnglerImpl.MAX_VEL, newNavData[2]), newAngleTarget[2]);
		assertEquals(newNavData[3], newAngleTarget[3]);
		
		newNavData[2] = -.38;
		newNavData[3] = 0;
		mNav.setNavTarget(newNavData);
		mAngler.getAngleTarget(newAngleTarget);
		assertEquals(Math.max(-AnglerImpl.MAX_VEL, newNavData[2]), newAngleTarget[2]);
		assertEquals(newNavData[3], newAngleTarget[3]);
		
		newNavData[2] = -224;
		newNavData[3] = 15;
		mNav.setNavTarget(newNavData);
		mAngler.getAngleTarget(newAngleTarget);
		assertEquals(Math.max(-AnglerImpl.MAX_VEL, newNavData[2]), newAngleTarget[2]);
		assertEquals(newNavData[3], newAngleTarget[3]);
	}
	
	public void testFacingNorth() throws Exception {
		double[] newNavData = new double[4];
		double[] oldAngle = new double[4];
		double[] newAngle = new double[4];
		double[] finalAngle = new double[4];
		
		newNavData[0] = 1;
		newNavData[1] = 0;
		mNav.setNavTarget(newNavData);
		finalAngle[0] = AnglerImpl.MAX_ANGLE;
		checkAngles(oldAngle, newAngle, finalAngle);
		
		newNavData[0] = -2;
		newNavData[1] = 0;
		mNav.setNavTarget(newNavData);
		finalAngle[0] = -AnglerImpl.MAX_ANGLE;
		checkAngles(oldAngle, newAngle, finalAngle);
		
		mCs.setGpsField(SPEED, -3);
		mCs.setGpsField(BEARING, 90);
		finalAngle[0] = AnglerImpl.MAX_ANGLE;
		checkAngles(oldAngle, newAngle, finalAngle);
		
		mCs.setGpsField(SPEED, 3);
		mCs.setGpsField(BEARING, 270);
		finalAngle[0] = AnglerImpl.MAX_ANGLE;
		checkAngles(oldAngle, newAngle, finalAngle);
		
		mCs.setGpsField(BEARING, 180);
		finalAngle[0] = -AnglerImpl.MAX_ANGLE;
		finalAngle[1] = AnglerImpl.MAX_ANGLE;
		checkAngles(oldAngle, newAngle, finalAngle);
		
		mCs.setGpsField(BEARING, 225);
		newNavData[0] = 1;
		newNavData[1] = -4;
		finalAngle[0] = AnglerImpl.MAX_ANGLE;
		finalAngle[1] = AnglerImpl.MAX_ANGLE;
	}
	
	public void testFacingEast() throws Exception {
		double[] newNavData = new double[4];
		double[] oldAngle = new double[4];
		double[] newAngle = new double[4];
		double[] finalAngle = new double[4];
		
		mCs.setReadingField(AZIMUTH, 90);
		
		Log.v(TAG, "Test 1");
		newNavData[0] = 1;
		newNavData[1] = 0;
		mNav.setNavTarget(newNavData);
		finalAngle[1] = AnglerImpl.MAX_ANGLE;
		checkAngles(oldAngle, newAngle, finalAngle);
		
		Log.v(TAG, "Test 2");
		newNavData[0] = -2;
		newNavData[1] = 0;
		mNav.setNavTarget(newNavData);
		finalAngle[1] = -AnglerImpl.MAX_ANGLE;
		checkAngles(oldAngle, newAngle, finalAngle);
		
		Log.v(TAG, "Test 3");
		mCs.setGpsField(SPEED, -3);
		mCs.setGpsField(BEARING, 90);
		finalAngle[1] = AnglerImpl.MAX_ANGLE;
		checkAngles(oldAngle, newAngle, finalAngle);
		
		Log.v(TAG, "Test 4");
		mCs.setGpsField(SPEED, 3);
		mCs.setGpsField(BEARING, 270);
		finalAngle[1] = AnglerImpl.MAX_ANGLE;
		checkAngles(oldAngle, newAngle, finalAngle);
		
		Log.v(TAG, "Test 5 ");
		mCs.setGpsField(BEARING, 180);
		finalAngle[0] = -AnglerImpl.MAX_ANGLE;
		finalAngle[1] = -AnglerImpl.MAX_ANGLE;
		checkAngles(oldAngle, newAngle, finalAngle);
		
		Log.v(TAG, "Test 6");
		mCs.setGpsField(BEARING, 225);
		newNavData[0] = 1;
		newNavData[1] = -4;
		finalAngle[0] = AnglerImpl.MAX_ANGLE;
		finalAngle[1] = AnglerImpl.MAX_ANGLE;
	}
	
	public void testFacingSouth() throws Exception {
		double[] newNavData = new double[4];
		double[] oldAngle = new double[4];
		double[] newAngle = new double[4];
		double[] finalAngle = new double[4];
		
		mCs.setReadingField(AZIMUTH, 180);
		
		Log.v(TAG, "Test 1");
		newNavData[0] = 1;
		newNavData[1] = 0;
		mNav.setNavTarget(newNavData);
		finalAngle[0] = -AnglerImpl.MAX_ANGLE;
		checkAngles(oldAngle, newAngle, finalAngle);
		
		Log.v(TAG, "Test 2");
		newNavData[0] = -2;
		newNavData[1] = 0;
		mNav.setNavTarget(newNavData);
		finalAngle[0] = AnglerImpl.MAX_ANGLE;
		checkAngles(oldAngle, newAngle, finalAngle);
		
		Log.v(TAG, "Test 3");
		mCs.setGpsField(SPEED, -3);
		mCs.setGpsField(BEARING, 90);
		finalAngle[0] = -AnglerImpl.MAX_ANGLE;
		checkAngles(oldAngle, newAngle, finalAngle);
		
		Log.v(TAG, "Test 4");
		mCs.setGpsField(SPEED, 3);
		mCs.setGpsField(BEARING, 270);
		finalAngle[0] = -AnglerImpl.MAX_ANGLE;
		checkAngles(oldAngle, newAngle, finalAngle);
		
		Log.v(TAG, "Test 5 ");
		mCs.setGpsField(BEARING, 180);
		finalAngle[0] = AnglerImpl.MAX_ANGLE;
		finalAngle[1] = -AnglerImpl.MAX_ANGLE;
		checkAngles(oldAngle, newAngle, finalAngle);
		
		Log.v(TAG, "Test 6");
		mCs.setGpsField(BEARING, 225);
		newNavData[0] = 1;
		newNavData[1] = -4;
		finalAngle[0] = -AnglerImpl.MAX_ANGLE;
		finalAngle[1] = AnglerImpl.MAX_ANGLE;
	}
	
	public void testFacingWest() throws Exception {
		double[] newNavData = new double[4];
		double[] oldAngle = new double[4];
		double[] newAngle = new double[4];
		double[] finalAngle = new double[4];
		
		mCs.setReadingField(AZIMUTH, 270);
		
		Log.v(TAG, "Test 1");
		newNavData[0] = 1;
		newNavData[1] = 0;
		mNav.setNavTarget(newNavData);
		finalAngle[1] = -AnglerImpl.MAX_ANGLE;
		checkAngles(oldAngle, newAngle, finalAngle);
		
		Log.v(TAG, "Test 2");
		newNavData[0] = -2;
		newNavData[1] = 0;
		mNav.setNavTarget(newNavData);
		finalAngle[1] = AnglerImpl.MAX_ANGLE;
		checkAngles(oldAngle, newAngle, finalAngle);
		
		Log.v(TAG, "Test 3");
		mCs.setGpsField(SPEED, -3);
		mCs.setGpsField(BEARING, 90);
		finalAngle[1] = -AnglerImpl.MAX_ANGLE;
		checkAngles(oldAngle, newAngle, finalAngle);
		
		Log.v(TAG, "Test 4");
		mCs.setGpsField(SPEED, 3);
		mCs.setGpsField(BEARING, 270);
		finalAngle[1] = -AnglerImpl.MAX_ANGLE;
		checkAngles(oldAngle, newAngle, finalAngle);
		
		Log.v(TAG, "Test 5");
		mCs.setGpsField(BEARING, 180);
		finalAngle[0] = AnglerImpl.MAX_ANGLE;
		finalAngle[1] = AnglerImpl.MAX_ANGLE;
		checkAngles(oldAngle, newAngle, finalAngle);
		
		Log.v(TAG, "Test 6");
		mCs.setGpsField(BEARING, 225);
		newNavData[0] = 1;
		newNavData[1] = -4;
		finalAngle[0] = -AnglerImpl.MAX_ANGLE;
		finalAngle[1] = -AnglerImpl.MAX_ANGLE;
	}
	
	public void testFacingSouthEast() throws Exception {
		double[] newNavData = new double[4];
		double[] oldAngle = new double[4];
		double[] newAngle = new double[4];
		double[] finalAngle = new double[4];
		
		mCs.setReadingField(AZIMUTH, 135);
		
		Log.v(TAG, "Test 1");
		newNavData[0] = 1;
		newNavData[1] = 0;
		mNav.setNavTarget(newNavData);
		finalAngle[0] = -AnglerImpl.MAX_ANGLE;
		finalAngle[1] = AnglerImpl.MAX_ANGLE;
		checkAngles(oldAngle, newAngle, finalAngle);
		
		Log.v(TAG, "Test 2");
		newNavData[0] = -2;
		newNavData[1] = 0;
		mNav.setNavTarget(newNavData);
		finalAngle[0] = AnglerImpl.MAX_ANGLE;
		finalAngle[1] = -AnglerImpl.MAX_ANGLE;
		checkAngles(oldAngle, newAngle, finalAngle);
		
		Log.v(TAG, "Test 3");
		mCs.setGpsField(SPEED, -3);
		mCs.setGpsField(BEARING, 90);
		finalAngle[0] = -AnglerImpl.MAX_ANGLE;
		finalAngle[1] = AnglerImpl.MAX_ANGLE;
		checkAngles(oldAngle, newAngle, finalAngle);
		
		Log.v(TAG, "Test 4");
		mCs.setGpsField(SPEED, 3);
		mCs.setGpsField(BEARING, 270);
		finalAngle[0] = -AnglerImpl.MAX_ANGLE;
		finalAngle[1] = AnglerImpl.MAX_ANGLE;
		checkAngles(oldAngle, newAngle, finalAngle);
	}
	
	private void checkAngles(double[] oldAngle, double[] newAngle, double[] finalAngle) {
		for (int j = 0; j < 20; j++) {
			System.arraycopy(newAngle, 0, oldAngle, 0, 4);
			mAngler.getAngleTarget(newAngle);
			logArray("oldAngle", oldAngle);
			logArray("newAngle", newAngle);
			logArray("finalAngle", finalAngle);
			for (int i = 0; i < finalAngle.length; i++) {
				if (finalAngle[i] == oldAngle[i]) {
					assertEquals(finalAngle[i], newAngle[i]);
				} else if (finalAngle[i] > oldAngle[i]) {
					assertTrue(newAngle[i] >= oldAngle[i]);
					assertTrue(finalAngle[i] >= newAngle[i]);
				} else {
					assertTrue(newAngle[i] <= oldAngle[i]);
					assertTrue(finalAngle[i] <= newAngle[i]);
				}
			}
		}
		assertArrayEquals(finalAngle, newAngle);
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
}
