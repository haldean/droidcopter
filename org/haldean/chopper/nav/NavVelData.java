package org.haldean.chopper.nav;

public class NavVelData {
    protected double[] velocity = new double[4];
	protected long timeToExecute;
    protected String name;
    
    /**
	 * Constructs/deserializes a NavVelData from a String.
	 * @param myString The string to deserialize.
	 * @throws IllegalArgumentException If the supplied String is not valid.
	 */
	public NavVelData(String myString) throws IllegalArgumentException {
		if (myString.startsWith("VEL!"))
			myString = myString.substring(4, myString.length());
		String[] tokens = myString.split("!");
		if (tokens.length < 6)
			throw new IllegalArgumentException();
		try {
			for (int i = 0; i < 4; i++)
				velocity[i] = new Double(tokens[i]);
		
			timeToExecute = new Long(tokens[4]);
            name = tokens[5].replace(' ', '_');
		}
		catch (NumberFormatException e) {
			throw new IllegalArgumentException();
		}
	}
    
    /**
	 * Serializes a NavVel to a String.
	 * @return The NavVel in serialized form.
	 */
	public String toString() {
		String me = "VEL!";
		
		for (int i = 0; i < 4; i++) {
			me = me.concat(Double.toString(velocity[i]));
			me = me.concat("!");
		}
		me = me.concat(Long.toString(timeToExecute));
        me = me.concat("!" + name);
		return me;
	}
	
	protected NavVelData() {
	}
}
