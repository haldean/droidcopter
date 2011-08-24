package org.haldean.chopper.pilot.test;

import org.haldean.chopper.pilot.Navigation;
import org.haldean.chopper.pilot.Receivable;

public class MockNavigation implements Navigation {
	double[] navTarget = new double[4];
	
	@Override
	public void evalNextVector(double[] newNavTarget) {
		System.arraycopy(navTarget, 0, newNavTarget, 0, 4);
	}

	@Override
	public void getTarget(double[] newNavTarget) {
		System.arraycopy(navTarget, 0, newNavTarget, 0, 4);
	}

	@Override
	public String[] getTasks() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void receiveMessage(String msg, Receivable source) {
		// TODO Auto-generated method stub

	}

	@Override
	public void registerReceiver(Receivable rec) {
		// TODO Auto-generated method stub
	}
	
	public void setNavTarget(double[] newNavTarget) {
		System.arraycopy(newNavTarget, 0, navTarget, 0, 4);
	}
}
