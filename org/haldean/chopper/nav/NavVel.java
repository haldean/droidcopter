package org.haldean.chopper.nav;

import org.haldean.chopper.Constants;

public class NavVel implements NavTask, Constants {
	private double[] velocity = new double[4];
	private long timeToExecute;
	private long firstCall = 0;
	
	public NavVel(String myString) {
		if (myString.startsWith("VEL!"))
			myString = myString.substring(4, myString.length());
		String[] tokens = myString.split("!");
		for (int i = 0; i < 4; i++)
			velocity[i] = new Double(tokens[i]);
		timeToExecute = new Long(tokens[4]);
	}
	
	public String toString() {
		String me = "VEL!";
		
		for (int i = 0; i < 4; i++) {
			me = me.concat(Double.toString(velocity[i]));
			me = me.concat("!");
		}
		me = me.concat(Long.toString(timeToExecute));
		return me;
	}
	
	public long getInterval() {
		if (firstCall == 0)
			return timeToExecute;
		else
			return Math.max(0, timeToExecute - (System.currentTimeMillis() - firstCall));
	}

	public void getVelocity(double[] target) {
		if (firstCall != 0)
			firstCall = System.currentTimeMillis();
		
		for (int i = 0; i < 4; i++)
			target[i] = velocity[i];

	}

	public boolean isComplete() {
		if ((firstCall != 0) && //The task has been called at least once
				(System.currentTimeMillis() - firstCall >= timeToExecute)) //the task has been running long enough
			return true;
		else
			return false;
	}

}
