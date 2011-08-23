package org.haldean.chopper.pilot.test;

import java.util.Arrays;

import org.haldean.chopper.pilot.Angler;

public class MockAngler implements Angler {
	private double[] angles = new double[4];
	
	@Override
	public void getAngleTarget(double[] target) {
		System.arraycopy(angles, 0, target, 0, 4);
	}
	
	public void setAngleTarget(double[] target) {
		System.arraycopy(target, 0, angles, 0, 4);
	}
	
	public void reset() {
		Arrays.fill(angles, 0.0);
	}
}
