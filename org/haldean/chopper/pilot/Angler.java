package org.haldean.chopper.pilot;

public interface Angler {

	/** Maximum permissible target velocity, in m/s; larger vectors will be resized */
	public static final double MAX_VEL = 2.0;
	/** The maximum angle, in degrees, guidance will permit the chopper to have */
	public static final double MAX_ANGLE = 10;
	public static final String TAG = new String("chopper.Angler");

	public abstract void getAngleTarget(double[] target);

}