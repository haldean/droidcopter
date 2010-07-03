package org.haldean.chopper.server;

import gov.nasa.worldwind.geom.*;

/** This takes the sensor messages from the DataReceiver and parses
 *  it, notifying the appropriate components with the new data 
 *  @author William Brown */
public class SensorParser implements Updatable {
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

    /** Update the components with new data 
     *  @param msg The received message to parse */
    public void update(String msg) {
	String parts[] = msg.split(":");

	/* If this is a GPS signal notify the World Wind component */
	if (parts[0].equals("GPS")) {
	    double lat = new Double(parts[LAT]);
	    double lon = new Double(parts[LON]);
	    double alt = new Double(parts[ALT]);

	    /* If this is true, the phone isn't receiving a GPS signal */
	    if (! (lat == 0 || lon == 0 || alt == 0) && wwc != null)
		wwc.addWaypoint(Position.fromDegrees(lat, lon, alt));
	}

	/* Orientation */
	else if (parts[0].equals("ORIENT")) {
	    Orientation o = new Orientation(new Double(parts[ROLL]),
					    new Double(parts[TILT]),
					    new Double(parts[PITCH]));
	    orient.setOrientation(o);
	}

	/* Acceleration */
	else if (parts[0].equals("ACCEL")) {
	    accel.setAcceleration(new Double(parts[XACCEL]),
				  new Double(parts[YACCEL]),
				  new Double(parts[ZACCEL]));
	}

	/* Sensors. All other sensors unsupported by phone and .:. ignored. */
	else if (parts[0].equals("FLUX"))
	    sensors.setFlux(new Double(parts[1]));

	else if (parts[0].equals("TEMPERATURE"))
	    sensors.setTemperature(new Double(parts[1]));
    }
}
