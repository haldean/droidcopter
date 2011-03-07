package org.haldean.chopper.nav;

public class NavVel extends NavData {

	public NavVel() {
		type = "VEL";
		mData = new double[7];
	}
	
	public double[] getVelocity() {
		double[] vel = new double[4];
		for (int i = 0; i < 4; i++) {
			vel[i] = mData[i];
		}
		return vel;
	}
	
	public double getTime() {
		return mData[4];
	}
	
	public double getFirstCall() {
		return mData[5];
	}
	
	public void setFirstCall(double time) {
		mData[5] = time;
	}
	
	public double getID() {
		return mData[6];
	}
}
