package org.haldean.chopper.nav;

import java.util.Arrays;

public class NavTrack extends NavData {
	
    public NavTrack() {
	type = "TRACK";
	mData = new double[8];
    }

    public int[] getColor() {
	return null; //Arrays.copyOfRange(mData, 1, 4);
    }

    public int getThreshold() {
	return (int) mData[5];
    }

    public int getTargetArea() {
	return (int) mData[4];
    }

    public int getTrackTime() {
	return (int) mData[6];
    }

    public int getStartTime() {
	return (int) mData[7];
    }

    public boolean started() {
	return mData[7] > 0;
    }

    public void start() {
	mData[7] = System.currentTimeMillis();
    }

	@Override
	public double getID() {
		// TODO Auto-generated method stub
		return 0;
	}
}