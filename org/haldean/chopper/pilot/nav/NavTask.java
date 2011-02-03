package org.haldean.chopper.pilot.nav;

/**
 * Contains instructions for calculating a target velocity vector.
 * @author Benjamin Bardin
 */
public interface NavTask {
	/**
	 * Arbitrary value used by some NavTasks in deciding when next to evaluate the next navigation vector.
	 * Smaller values mean more accurate navigation vectors at the expense of CPU time.
	 */
	static final int NAVPAUSE = 1000;
	
	/**
	 * Calculates how long to wait before recalculating the target velocity vector.
	 * @return The time in ms.
	 */
	public long getInterval();
	
	/**
	 * Calculates a new target velocity vector.
	 * @param target The array in which to store the vector.  Must be at least length 4.
	 */
	public void getVelocity(double[] target);
	
	/**
	 * Calculates whether or not the NavTask has been functionally achieved.
	 * @return true if the NavTask has been completed, false otherwise.
	 */
	public boolean isComplete();
	
	/**
	 * Serializes a NavTask to String form.
	 * @return The String serialization of the NavTask.
	 */
	public abstract String toString();
}
