package org.haldean.chopper.nav;

import org.haldean.chopper.ChopperStatus;
import org.haldean.chopper.Constants;
import org.haldean.chopper.Navigation;

import android.location.Location;

/**
 * A navtask that determines target velocity based on a desired destination.
 */
public class NavDest implements NavTask, Constants {
	/* Used to access Android location methods */
	private Location currentLoc;
	private Location destination;
	
	/* Destination parameters */
	private double altitude;
	private double longitude;
	private double latitude;
	
	/* Travel speed */
	private double myVelocity;
	
	/* Evaluate vectors faster when arriving */
	private boolean reallyClose;
	
	/* Maximum tolerable distance from destination to declare task complete */
	private double destDist;
	
	/**
	 *  Creates/deserializes a NavDest from a String
	 * @param myString String to deserialize
	 */
	public NavDest(String myString) {
		//altitude, longitude, latitude, travelspeed, destDist
		if (myString.startsWith("DEST!"))
			myString = myString.substring(5, myString.length());
		String[] params = myString.split("!");

		altitude = new Double(params[0]);
		latitude = new Double(params[1]);
		longitude = new Double(params[2]);
		
		myVelocity = new Double(params[3]);
		destDist = new Double(params[4]);
		reallyClose = false;
	}
	
	/**
	 * Creates a NavDest.
	 * @param loc Desired destination.
	 * @param velocity Desired travel velocity.
	 * @param acceptableDistance How close to the destination to get.
	 */
	public NavDest (Location loc, double velocity, double acceptableDistance) {
		destination = loc;
		myVelocity = velocity;
		destDist = acceptableDistance;
		reallyClose = false;
	}
	
	/**
	 * Serializes a NavDest to a String.
	 * @return The NavDest in serialized form.
	 */
	public String toString() {
		return "DEST" +
				"!" + altitude +
				"!" + latitude +
				"!" + longitude +
				"!" + myVelocity +
				"!" + destDist;
	}
	
	/**
	 * Get desired time until next calculation of target velocity vector.
	 */
	public long getInterval() {
		if (reallyClose)
			return Navigation.NAVPAUSE / 2;
		else
			return Navigation.NAVPAUSE;
	}
	
	/**
	 * Calculates the target velocity vector.
	 * @param target The array in which to store the vector.  Length must be at least 4.
	 */
	public void getVelocity(double[] target) {
		synchronized (ChopperStatus.lastLoc) { 
			currentLoc = new Location(ChopperStatus.lastLoc);
		}
		
		if (currentLoc == null) {
			System.out.println("Nav not ready");
			return;
		}
		
		if (destination == null)
			destination = new Location(ChopperStatus.lastLoc);
		
		destination.setAltitude(altitude);
		destination.setLongitude(longitude);
		destination.setLatitude(latitude);
		
		double bearing = currentLoc.bearingTo(destination);
		float distance = currentLoc.distanceTo(destination);
		if (distance < 2 * destDist)
			reallyClose = true;
		else
			reallyClose = false;
		
		//Establish vector
		target[0] = Math.cos(bearing);
		target[1] = Math.sin(bearing);
		target[2] = (destination.getAltitude() - currentLoc.getAltitude() / distance);
		
		//Determine magnitude, "normalize" to myVelocity
		double mag = Math.sqrt(Math.pow(target[0], 2) +
								Math.pow(target[1], 2) +
								Math.pow(target[2], 2));
		double adjustment = myVelocity / mag;
		
		for (int i = 0; i < 3; i++)
			target[i] *= adjustment;
		
		target[3] = bearing;
	}
	
	/**
	 * Calculate whether the NavTask's goal has been achieved.
	 */
	public boolean isComplete() {
		if (currentLoc == null | destination == null)
			return false;
		if (currentLoc.distanceTo(destination) <= destDist)
			return true;
		else
			return false;
	}

}