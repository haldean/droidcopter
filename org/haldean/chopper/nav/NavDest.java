package org.haldean.chopper.nav;

public class NavDest extends NavData {
	
	public NavDest() {
		type = "DEST";
		mData = new double[6];
	}
	
	public double getAltitude() {
		return mData[0];
	}
	
	public double getLongitude() {
		return mData[1];
	}
	
	public double getLatitude() {
		return mData[2];
	}
	
	public double getVelocity() {
		return mData[3];
	}
	
	public double getRadius() {
		return mData[4];
	}
	
	public double getID() {
		return mData[5];
	}
}