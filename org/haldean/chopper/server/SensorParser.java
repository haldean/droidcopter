package org.haldean.chopper.server;

import gov.nasa.worldwind.geom.*;

/** 
 *  This takes the sensor messages from the DataReceiver and parses
 *  it, notifying the appropriate components with the new data 
 *
 *  @author William Brown 
 */
public class SensorParser implements MessageHook {
    /* The messages are all essentially flattened arrays
     * divided by colons. These constants are used to determine
     * which "index" in the string corresponds to what data */
    private final int TILT = 2;
    private final int PITCH = 3;
    private final int ROLL = 1;
	
    private final int XACCEL = 1;
    private final int YACCEL = 2;
    private final int ZACCEL = 3;

    private final int ALT = 1;
    private final int BEARING = 2;
    private final int LON = 3;
    private final int LAT = 4;
    private final int SPEED = 5;
    private final int ACCURACY = 6;

    /* These are the components that are updated
     * when new data comes in */
    private WorldWindComponent wwc;
    private OrientationComponent orient;
    private AccelerationComponent accel;
    private SensorComponent sensors;

    /** Create a new SensorParser */
    public SensorParser() {
	;
    }

    public String[] processablePrefixes() {
	return new String[] {"GPS", "ORIENT", "ACCEL", "FLUX", "TEMPERATURE", "PING"};
    }

    /** Set the notified NASA World Wind globe component 
     *  @param _wwc The World Wind component to notify */
    public void setWorldWindComponent(WorldWindComponent _wwc) {
	wwc = _wwc;
    }

    /** Set the notified orientation component
     *  @param _orient The orientation component to notify */
    public void setOrientationComponent(OrientationComponent _orient) {
	orient = _orient;
    }

    /** Set the notified acceleration component 
     *  @param _accel The acceleration component to notify */
    public void setAccelerationComponent(AccelerationComponent _accel) {
	accel = _accel;
    }
    
    /** Set the notified sensor component 
     *  @param _sensors The sensor component to notify */
    public void setSensorComponent(SensorComponent _sensors) {
	sensors = _sensors;
    }

    /** 
     *  Update the components with new data 
     *
     *  @param msg The received message to process
     */
    public void process(Message message) {
	/* If this is a GPS signal notify the World Wind component */
	if (message.getPart(0).equals("GPS")) {
		try {
		    double lat = new Double(message.getPart(LAT));
		    double lon = new Double(message.getPart(LON));
		    double alt = new Double(message.getPart(ALT));
	
		    /* If this is true, the phone isn't receiving a GPS signal */
		    if (! (lat == 0 || lon == 0 || alt == 0) && wwc != null)
			wwc.addWaypoint(Position.fromDegrees(lat, lon, alt));
		}
		catch (Exception e) {
			System.out.println("UNPARSABLE: " + message);
			e.printStackTrace();
		}
	}
	/* Orientation */
	else if (message.getPart(0).equals("ORIENT")) {
	    Orientation o = new Orientation(new Double(message.getPart(ROLL)),
					    new Double(message.getPart(TILT)),
					    new Double(message.getPart(PITCH)));
	    orient.setOrientation(o);
	}
	

	/* Acceleration */
	else if (message.getPart(0).equals("ACCEL")) {
	    accel.setAcceleration(new Double(message.getPart(XACCEL)),
				  new Double(message.getPart(YACCEL)),
				  new Double(message.getPart(ZACCEL)));
	}

	/* Sensors. All other sensors unsupported by phone and .:. ignored. */
	else if (message.getPart(0).equals("FLUX"))
	    sensors.setFlux(Math.sqrt(Math.pow(new Double(message.getPart(1)), 2) +
				      Math.pow(new Double(message.getPart(2)), 2) +
				      Math.pow(new Double(message.getPart(3)), 2)));

	else if (message.getPart(0).equals("TEMPERATURE"))
	    sensors.setTemperature(new Double(message.getPart(1)));

	else if (message.getPart(0).equals("PING"))
	    sensors.setPing(new Integer(message.getPart(1)));
    }
}
