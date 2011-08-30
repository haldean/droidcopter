package org.haldean.chopper.pilot;

import org.haldean.chopper.nav.NavData;
import org.haldean.chopper.nav.NavDest;
import org.haldean.chopper.nav.NavList;
import org.haldean.chopper.nav.NavTrack;
import org.haldean.chopper.nav.NavVel;

import android.location.Location;
import android.util.Log;

/**
 * Contains instructions for calculating a target velocity vector.
 * @author Benjamin Bardin
 */
public class NavTask {
	private ChopperStatus myCs;
	private Location currentLoc;
	private Location destination;
	public static final String TAG = "NavTask";
	
	public NavTask(ChopperStatus cs) {
		myCs = cs;
	}
	/**
	 * Arbitrary value used by some NavTasks in deciding when next to evaluate the next navigation vector.
	 * Smaller values mean more accurate navigation vectors at the expense of CPU time.
	 */
	static final int NAVPAUSE = 1000;
	
	/**
	 * Calculates how long to wait before recalculating the target velocity vector.
	 * @return The time in ms.
	 */
	public long getInterval(NavData nav) {
		if (nav instanceof NavDest) {
			return NAVPAUSE;
		}
		else if (nav instanceof NavList) {
			return getInterval(((NavList) nav).getCurrentTask());
		}
		else if (nav instanceof NavVel) {
			NavVel vel = (NavVel) nav;
			if (vel.getFirstCall() <= 0)
				return (long) vel.getTime();
			else
				return (long) Math.max(NAVPAUSE, vel.getTime() - (System.currentTimeMillis() - vel.getFirstCall()));
		}
		else if (nav instanceof NavTrack) {
			NavTrack track = (NavTrack) nav;
			if (track.getStartTime() <= 0)
				return (long) track.getTrackTime();
			else
				return (long) Math.max(NAVPAUSE, track.getTrackTime() - (System.currentTimeMillis() - track.getStartTime()));
		}
		return NAVPAUSE;
	}
	
	/**
	 * Calculates a new target velocity vector.
	 * @param target The array in which to store the vector.  Must be at least length 4.
	 */
	public void getVelocity(NavData nav, double[] target) {
		if (target.length < 4)
			throw new IllegalArgumentException();
		if (nav instanceof NavDest) {
			getDestTarg(nav, target);
		}
		else if (nav instanceof NavVel) {
			getVelTarg(nav, target);
		}
		else if (nav instanceof NavList) {
			NavList lastList = (NavList) nav;
			NavData curTask = lastList.getCurrentTask();
			while (isComplete(curTask)) {
				curTask = lastList.nextTask();
			}
			if (curTask != null) {
				getVelocity(curTask, target);
			}
			else {
				Log.wtf(TAG, "Completed task not removed from NavList.  Carry on.");
			}
		}
		else if (nav instanceof NavTrack) {
		    getTrackTarg((NavTrack) nav, target);
		}
	}
	/**
	 * Calculates whether or not the NavTask has been functionally achieved.
	 * @return true if the NavTask has been completed, false otherwise.
	 */
	public boolean isComplete(NavData nav) {
		if (nav instanceof NavDest) {
			if (currentLoc == null | destination == null)
				return false;
			double horDistance = currentLoc.distanceTo(destination);
			double verDistance = destination.getAltitude() - currentLoc.getAltitude();
			double distance = Math.sqrt(Math.pow(horDistance, 2) +
										Math.pow(verDistance, 2));
			if (distance <= ((NavDest) nav).getRadius())
				return true;
			else
				return false;
		}
		else if (nav instanceof NavVel) {
			NavVel vel = (NavVel) nav;
			if ((vel.getFirstCall() > 0) && //The task has been called at least once
					(System.currentTimeMillis() - vel.getFirstCall() >= vel.getTime())) {//the task has been running long enough
				
				System.out.println("Completing navVel");
				System.out.println(System.currentTimeMillis());
				System.out.println(vel.getFirstCall());
				System.out.println(vel.getTime());
				return true;
			}
			else
				return false;
		}
		else if (nav instanceof NavList) {
			NavList lastList = (NavList) nav;
			NavData curTask = lastList.getCurrentTask();
			while ((curTask != null) && (isComplete(curTask))) {
				curTask = lastList.nextTask();
			}
			if (curTask == null)
				return true;
			else
				return false;
		}
		else if (nav instanceof NavTrack) {
			NavTrack track = (NavTrack) nav;
			if (track.started() && (System.currentTimeMillis() - track.getStartTime() >= track.getTrackTime()))
				return true;
			else
				return false;
		}
		return true;
	}
	
	/**
	 * Get velocity target from a NavTrack
	 * @param nav The NavTrack
	 * @param target Writes the velocity here.
	 */
    private void getTrackTarg(NavTrack nav, double[] target) {
    	if (!nav.started()) {
    		nav.start();
    	}
    }
	
    /**
	 * Get velocity target from a NavVel
	 * @param nav The NavVel
	 * @param target Writes the velocity here.
	 */
	private void getVelTarg(NavData nav, double[] target) {
		NavVel vel = (NavVel) nav;
		
		if (vel.getFirstCall() <= 0) {
			vel.setFirstCall(System.currentTimeMillis());
		}
		double[] velocity = vel.getVelocity();
		
		for (int i = 0; i < 4; i++) {
			target[i] = velocity[i];
		}
	}
	
	/**
	 * Get velocity target from a NavDest
	 * @param nav The NavDest
	 * @param target Writes the velocity here.
	 */
	private void getDestTarg(NavData nav, double[] target) {
		NavDest lastDest = (NavDest) nav;
		Location myLoc = myCs.getLastLocation();
		if (myLoc == null) {
			Log.w(TAG, "GPS Not Initialized");
			return;
		}
		currentLoc = myLoc;
		if (destination == null) {
			destination = new Location(currentLoc);
		}
		
		destination.setAltitude(lastDest.getAltitude());
		destination.setLongitude(lastDest.getLongitude());
		destination.setLatitude(lastDest.getLatitude());
		
		//Bearing, in degrees
		double bearingDeg = currentLoc.bearingTo(destination);
		double horDistance = currentLoc.distanceTo(destination);
		double verDistance = destination.getAltitude() - currentLoc.getAltitude();
		
		//Establish vector
		double bearingRad = bearingDeg / 180.0 * Math.PI;
		target[0] = Math.sin(bearingRad);
		target[1] = Math.cos(bearingRad);
		if (horDistance != 0) {
			target[2] = verDistance / horDistance;
		} else {
			target[2] = verDistance; 
		}
		
		//Determine magnitude, "normalize" to myVelocity
		double mag = Math.sqrt(Math.pow(target[0], 2) +
								Math.pow(target[1], 2) +
								Math.pow(target[2], 2));
		double adjustment;
		if (mag != 0) {
			adjustment = lastDest.getVelocity() / mag;
		} else {
			adjustment = 0.0;
		}
		
		for (int i = 0; i < 3; i++)
			target[i] *= adjustment;
		
		target[3] = bearingDeg;
	}
}
