package org.haldean.chopper.pilot.nav;

import org.haldean.chopper.nav.NavDestData;
import org.haldean.chopper.pilot.ChopperStatus;
import org.haldean.chopper.pilot.Constants;

import android.location.Location;
import android.util.Log;
/**
 * A NavTask that determines target velocity based on a desired destination.
 */
public class NavDest extends NavDestData implements NavTask, Constants {
	/* Used to access Android location methods */
	private Location currentLoc;
	private Location destination;
	
	private static String TAG = "nav.NavDest";
	
	private ChopperStatus myCs;
		
	/**
	 *  Creates/deserializes a NavDest from a String.  The String should be of the format DEST!altitude!longitude!latitude!velocity!minimumDistance
	 * @param myString String to deserialize
	 * @throws IllegalArgumentException If the supplied String is not valid.
	 */
	public NavDest(String myString, ChopperStatus Cs) throws IllegalArgumentException {
		super(myString);
		myCs = Cs;
	}
	
	/**
	 * Get desired time until next calculation of target velocity vector.
	 */
	public long getInterval() {
		if (reallyClose)
			return NAVPAUSE / 2;
		else
			return NAVPAUSE;
	}
	
	/**
	 * Calculates the target velocity vector.
	 * @param target The array in which to store the vector.  Length must be at least 4.
	 * @throws IllegalArgumentException If the supplied array's length is less than 4.
	 */
	public void getVelocity(double[] target) throws IllegalArgumentException {
		if (target.length < 4)
			throw new IllegalArgumentException();
		Location myLoc = myCs.getLastLocation();
		if (myLoc != null) {
			currentLoc = myLoc;
			if (destination == null) {
				destination = new Location(currentLoc);
			
				destination.setAltitude(altitude);
				destination.setLongitude(longitude);
				destination.setLatitude(latitude);
			}
		}
		else {
			Log.w(TAG, "GPS Not Initialized");
			return;
		}
		
		
		//Bearing, in degrees
		double bearingDeg = currentLoc.bearingTo(destination);
		
		double horDistance = currentLoc.distanceTo(destination);
		double verDistance = destination.getAltitude() - currentLoc.getAltitude();
		double distance = Math.sqrt(Math.pow(horDistance, 2) +
									Math.pow(verDistance, 2));
		
		if (distance < 2 * destDist)
			reallyClose = true;
		else
			reallyClose = false;
		//System.out.println("Horizontal distance: " + horDistance);
		//System.out.println("Vertical distance: " + verDistance);
		//System.out.println("Total distance: " + distance);
		
		//Establish vector
		double bearingRad = bearingDeg / 180.0 * Math.PI;
		target[0] = Math.cos(bearingRad);
		target[1] = Math.sin(bearingRad);
		target[2] = verDistance / horDistance;
		
		//Determine magnitude, "normalize" to myVelocity
		double mag = Math.sqrt(Math.pow(target[0], 2) +
								Math.pow(target[1], 2) +
								Math.pow(target[2], 2));
		double adjustment = myVelocity / mag;
		
		for (int i = 0; i < 3; i++)
			target[i] *= adjustment;
		
		target[3] = bearingDeg;
	}
	
	/**
	 * Calculate whether the NavTask's goal has been achieved.
	 */
	public boolean isComplete() {
		if (currentLoc == null | destination == null)
			return false;
		double horDistance = currentLoc.distanceTo(destination);
		double verDistance = destination.getAltitude() - currentLoc.getAltitude();
		double distance = Math.sqrt(Math.pow(horDistance, 2) +
									Math.pow(verDistance, 2));
		if (distance <= destDist)
			return true;
		else
			return false;
	}
	
	

}
