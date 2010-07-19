package org.haldean.chopper.nav;

/**
 * Contains instructions for calculating a target velocity vector.
 * @author Benjamin Bardin
 */
public interface NavTask {
	/**
	 * Calculates how long to wait before recalculating the target velocity vector.
	 * @return The time in ms.
	 */
	public long getInterval();
	
	/**
	 * Calculates whether or not the NavTask has been functionally achieved.
	 * @return true if the NavTask has been completed, false otherwise.
	 */
	public boolean isComplete();
	
	/**
	 * Calculates a new target velocity vector.
	 * @param target The array in which to store the vector.  Must be at least length 4.
	 */
	public void getVelocity(double[] target);
	
	/**
	 * Serializes a NavTask to String form.
	 * @return The String serialization of the NavTask.
	 */
	public abstract String toString();
}
