package org.haldean.chopper.nav;

import org.haldean.chopper.Constants;

/**
 * A NavTask that returns a specific velocity for a specific amount of time.
 */
public class NavVel implements NavTask, Constants {
	private double[] velocity = new double[4];
	private long timeToExecute;
	private long firstCall = -1;
	
	/**
	 * Constructs/deserializes a NavVel from a String.
	 * @param myString The string to deserialize.
	 * @throws IllegalArgumentException If the supplied String is not valid.
	 */
	public NavVel(String myString) throws IllegalArgumentException {
		if (myString.startsWith("VEL!"))
			myString = myString.substring(4, myString.length());
		String[] tokens = myString.split("!");
		if (tokens.length < 5)
			throw new IllegalArgumentException();
		try {
			for (int i = 0; i < 4; i++)
				velocity[i] = new Double(tokens[i]);
		
			timeToExecute = 1000 * new Long(tokens[4]);
		}
		catch (NumberFormatException e) {
			throw new IllegalArgumentException();
		}
	}
	
	/**
	 * Get desired time until next calculation of target velocity vector.
	 */
	public long getInterval() {
		if (firstCall == -1)
			return timeToExecute;
		else
			return Math.max(NAVPAUSE, timeToExecute - (System.currentTimeMillis() - firstCall));
	}
	
	/**
	 * Calculates the target velocity vector.
	 * @param target The array in which to store the vector.  Length must be at least 4.
	 * @throws IllegalArgumentException If the supplied array's length is less than 4.
	 */
	public void getVelocity(double[] target) throws IllegalArgumentException {
		if (target.length < 4)
			throw new IllegalArgumentException();
		
		if (firstCall == -1)
			firstCall = System.currentTimeMillis();
		
		for (int i = 0; i < 4; i++)
			target[i] = velocity[i];

	}
	
	/**
	 * Calculate whether the NavTask's goal has been achieved.
	 */
	public boolean isComplete() {
		if ((firstCall != -1) && //The task has been called at least once
				(System.currentTimeMillis() - firstCall >= timeToExecute)) //the task has been running long enough
			return true;
		else
			return false;
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

}
