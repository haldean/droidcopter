package org.haldean.chopper.nav;

/**
 * Stores NavDest data, and (de)serialization methods.
 */
public class NavDestData {
	/* Destination parameters */
	protected double altitude;
	protected double longitude;
	protected double latitude;
	
	/* Travel speed */
	protected double myVelocity;
	
	/* Evaluate vectors faster when arriving */
	protected boolean reallyClose;
	
	/* Maximum tolerable distance from destination to declare task complete */
	protected double destDist;
    
    /* Human-readable descriptive label */
    protected String name;
	
    /**
     * Empty constructor for subclasses.
     */
	protected NavDestData() {
	}
	
	/**
	 *  Creates/deserializes a NavDestData from a String.  The String should be of the format DEST!altitude!longitude!latitude!velocity!minimumDistance
	 * @param myString String to deserialize
	 * @throws IllegalArgumentException If the supplied String is not valid.
	 */
	public NavDestData(String myString) throws IllegalArgumentException {
		//altitude, longitude, latitude, travelspeed, destDist
		if (myString.startsWith("DEST!"))
			myString = myString.substring(5, myString.length());
		String[] params = myString.split("!");
		if (params.length < 6)
			throw new IllegalArgumentException();
		try {
			altitude = new Double(params[0]);
			longitude = new Double(params[1]);
			latitude = new Double(params[2]);
			
			myVelocity = new Double(params[3]);
			destDist = new Double(params[4]);
		}
		catch (NumberFormatException e) {
			System.out.println(myString);
            throw new IllegalArgumentException();
		}
        name = params[5].replace(' ', '_');
		reallyClose = false;
	}
	
	/**
	 * Serializes a NavDestData to a String.
	 * @return The NavDestData in serialized form.
	 */
	public String toString() {
		return "DEST" +
				"!" + altitude +
				"!" + longitude +
				"!" + latitude +
				"!" + myVelocity +
				"!" + destDist +
                "!" + name;
	}
}
