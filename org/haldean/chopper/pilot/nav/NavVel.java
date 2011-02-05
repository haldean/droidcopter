package org.haldean.chopper.pilot.nav;

import org.haldean.chopper.nav.NavVelData;
import org.haldean.chopper.pilot.Constants;

/**
 * A NavTask that returns a specific velocity for a specific amount of time.
 */
public class NavVel extends NavVelData implements NavTask, Constants {
	
	private long firstCall = -1;

	public NavVel(String str) { 
		super(str);
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
	
	

}
