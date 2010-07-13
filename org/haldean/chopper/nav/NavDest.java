package org.haldean.chopper.nav;

import org.haldean.chopper.ChopperStatus;
import org.haldean.chopper.Constants;

import android.location.Location;

public class NavDest implements NavTask, Constants {
	private Location currentLoc;
	private Location destination;
	
	private double altitude;
	private double longitude;
	private double latitude;
	
	private double myVelocity;
	private boolean reallyClose;
	private double destDist;
	
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
	
	public NavDest (Location loc, double velocity, double acceptableDistance) {
		destination = loc;
		myVelocity = velocity;
		destDist = acceptableDistance;
		reallyClose = false;
	}
	
	public String toString() {
		return "DEST" +
				"!" + altitude +
				"!" + latitude +
				"!" + longitude +
				"!" + myVelocity +
				"!" + destDist;
	}
	
	public long getInterval() {
		if (reallyClose)
			return NAVPAUSE / 2;
		else
			return NAVPAUSE;
	}

	public void getVelocity(double[] target) {
		ChopperStatus.lastLocLock.lock();
		currentLoc = new Location(ChopperStatus.lastLoc);
		ChopperStatus.lastLocLock.unlock();
		
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
		if (distance < ARRIVINGDIST)
			reallyClose = true;
		
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

	public boolean isComplete() {
		if (currentLoc == null | destination == null)
			return false;
		if (currentLoc.distanceTo(destination) <= destDist)
			return true;
		else
			return false;
	}

}
