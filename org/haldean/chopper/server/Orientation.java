package org.haldean.chopper.server;

/** A class to represent the chopper's orientation 
 *  @author William Brown */
public class Orientation {
    private double rollRad;
    private double rollDeg;
    
    private double tiltRad;
    private double tiltDeg;

    private double pitchRad;
    private double pitchDeg;

    /** Request a return value in radians 
     *  @see Orientation#getRoll
     *  @see Orientation#getTilt 
     *  @see Orientation#getPitch */
    public static final int RADIANS = 0;
    /** Request a return value in degrees 
     *  @see Orientation#getRoll
     *  @see Orientation#getTilt 
     *  @see Orientation#getPitch */
    public static final int DEGREES = 1;

    /** Create a new Orientation object
     *  @param _roll The roll of the chopper
     *  @param _tilt The tilt of the chopper
     *  @param _pitch The pitch of the chopper */
    public Orientation(double _roll, double _tilt, double _pitch) {
	setRoll(_roll);
	setTilt(_tilt);
	setPitch(_pitch);
    }

    /** Set the roll of the chopper
     *  @param _roll The roll in degrees */
    public void setRoll(double _roll) {
	rollDeg = _roll;
	rollRad = Math.toRadians(_roll);
    }

    /** Set the tilt of the chopper
     *  @param _tilt The tilt in degrees */
    public void setTilt(double _tilt) {
	tiltDeg = _tilt;
	tiltRad = Math.toRadians(_tilt);
    }

    /** Set the pitch of the chopper
     *  @param _pitch The pitch in degrees */
    public void setPitch(double _pitch) {
	pitchDeg = _pitch;
	pitchRad = Math.toRadians(_pitch);
    }

    /** Get the roll of the chopper
     *  @param unit Either Orientation.DEGREES or Orientation.RADIANS
     *  @return The roll in the given unit */
    public double getRoll(int unit) {
	if (unit == RADIANS)
	    return rollRad;
	else
	    return rollDeg;
    }

    /** Get the tilt of the chopper
     *  @param unit Either Orientation.DEGREES or Orientation.RADIANS
     *  @return The tilt in the given unit */
    public double getTilt(int unit) {
	if (unit == RADIANS)
	    return tiltRad;
	else
	    return tiltDeg;
    }

    /** Get the pitch of the chopper
     *  @param unit Either Orientation.DEGREES or Orientation.RADIANS
     *  @return The pitch in the given unit */
    public double getPitch(int unit) {
	if (unit == RADIANS)
	    return pitchRad;
	else
	    return pitchDeg;
    }

    /** A string representation of the orientation
     *  @return A Unicode string: roll(deg), tilt(deg), pitch(deg) */
    public String toString() {
	return (new Double(rollDeg)).toString() + "\u00B0, " + 
	    (new Double(tiltDeg)).toString() + "\u00B0, " +
	    (new Double(pitchDeg)).toString() + "\u00B0";
    }
}