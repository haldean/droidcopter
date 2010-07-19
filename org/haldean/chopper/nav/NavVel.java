package org.haldean.chopper.nav;

import org.haldean.chopper.Constants;

/**
 * A navtask that returns a specific velocity for a specific amount of time.
 */
public class NavVel implements NavTask, Constants {
	private double[] velocity = new double[4];
	private long timeToExecute;
	private long firstCall = 0;
	
	/**
	 * Constructs/deserializes a NavVel from a String.
	 * @param myString String to deserialize.
	 */
	public NavVel(String myString) {
		if (myString.startsWith("VEL!"))
			myString = myString.substring(4, myString.length());
		String[] tokens = myString.split("!");
		for (int i = 0; i < 4; i++)
			velocity[i] = new Double(tokens[i]);
		timeToExecute = new Long(tokens[4]);
	}
	
	/**
	 * Serializes a NavVel to a String.
	 * @return The NavVel in serialized form.
	 */
	public String toString() {
		String me = "VEL!";
		
		for (int i = 0; i < 4; i++) {
			me = me.concat(Double.toString(velocity[i]));
			me = me.concat("!");
		}
		me = me.concat(Long.toString(timeToExecute));
		return me;
	}
	
	/**
	 * Get desired time until next calculation of target velocity vector.
	 */
	public long getInterval() {
		if (firstCall == 0)
			return timeToExecute;
		else
			return Math.max(0, timeToExecute - (System.currentTimeMillis() - firstCall));
	}
	
	/**
	 * Calculates the target velocity vector.
	 * @param target The array in which to store the vector.  Length must be at least 4.
	 */
	public void getVelocity(double[] target) {
		if (firstCall != 0)
			firstCall = System.currentTimeMillis();
		
		for (int i = 0; i < 4; i++)
			target[i] = velocity[i];

	}
	
	/**
	 * Calculate whether the NavTask's goal has been achieved.
	 */
	public boolean isComplete() {
		if ((firstCall != 0) && //The task has been called at least once
				(System.currentTimeMillis() - firstCall >= timeToExecute)) //the task has been running long enough
			return true;
		else
			return false;
	}

}
