package org.haldean.chopper.pilot;

import android.util.Log;

public class Angler implements Constants {
	/** Maximum permissible target velocity, in m/s; larger vectors will be resized */
	public static final double MAX_VEL = 2.0;
	
	/** The maximum angle, in degrees, guidance will permit the chopper to have */
	public static final double MAX_ANGLE = 10;
	
	public static final String TAG = new String("chopper.Angler");
	
	private Navigation mNav;
	private ChopperStatus mStatus;
	private double[] mNavTarget;
	private double[] mRelativeTarget;
	private double[] mCurrent;
	
	public Angler(ChopperStatus status, Navigation nav) {
		mNav = nav;
		mStatus = status;
		mCurrent = new double[4];
		mNavTarget = new double[4];
		mRelativeTarget = new double[4];
		
	}
	
	public void getAngleTarget(double[] target) {
		if ((target == null) || (target.length < 4)) {
			return;
		}
		mNav.evalNextVector(mNavTarget);
		
		double mAzimuth = mStatus.getReadingField(AZIMUTH);		

		// Calculate target relative velocity.
		double theta = mAzimuth * Math.PI / 180.0;
		mRelativeTarget[0] = mNavTarget[0] * Math.cos(theta) - mNavTarget[1] * Math.sin(theta);
		mRelativeTarget[1] = mNavTarget[0] * Math.sin(theta) + mNavTarget[1] * Math.cos(theta);
		mRelativeTarget[2] = mNavTarget[2];
		mRelativeTarget[3] = mNavTarget[3];
			
		//Calculate recorded velocity; reduce, if necessary, to MAXVEL
		double myVel = 0;
		for (int i = 0; i < 3; i++) {
			myVel += Math.pow(mRelativeTarget[i], 2);
		}
		myVel = Math.sqrt(myVel);
		if (myVel > MAX_VEL) {
			Log.v(TAG, "guid, Reducing requested velocity");
			double adjustment = MAX_VEL / myVel;
			for (int i = 0; i < 3; i++) {
				mRelativeTarget[i] *= adjustment;
			}
		}
		
		// Calculate current relative velocity.
		double mGpsBearing = mStatus.getGpsField(BEARING);
		double phi = (mGpsBearing - mAzimuth) * Math.PI / 180.0;
		
		double mGpsSpeed = mStatus.getGpsField(SPEED);
		mCurrent[0] = mGpsSpeed * Math.sin(phi);
		mCurrent[1] = mGpsSpeed * Math.cos(phi);
		mCurrent[2] = mStatus.getGpsField(dALT);
		mCurrent[3] = mAzimuth;
		
		if (mCurrent[0] < mRelativeTarget[0]) {
			target[0] += 1.0;
		} else {
			target[0] -= 1.0;
		}
		
		if (mCurrent[1] < mRelativeTarget[1]) {
			target[1] += 1.0;
		} else {
			target[1] -= 1.0;
		}
		
		target[0] = restrainedTarget(target[0]);
		target[1] = restrainedTarget(target[1]);
		target[2] = mRelativeTarget[2];
		target[3] = mRelativeTarget[3];
	}
	
	private static double restrainedTarget(double requested) {
		if (requested < -MAX_ANGLE) {
			return -MAX_ANGLE;
		}
		if (requested > MAX_ANGLE) {
			return MAX_ANGLE;
		}
		return requested;
	}
}
