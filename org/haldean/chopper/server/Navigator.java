package org.haldean.chopper.server;

import org.haldean.chopper.nav.NavDest;
import org.haldean.chopper.nav.NavVel;

import gov.nasa.worldwind.geom.Position;

/**
 *  This is the class responsible for sending navigation commands to
 *  the helicopter. Right now, it can only send the helicopter
 *  messages to tell it a target velocity or location, although
 *  eventually this will be the place where it creates lists of tasks
 *  for the helicopter to complete.
 *
 *  @author William Brown
 */
public class Navigator {
  /**
   *  This is a convenience class that is never instantiated.
   */
  private Navigator() {
    // You'll never reach me in here! AAAH HA HAA.
  }

  /**
   *  Tell the helicopter to go at a specific velocity in a specific
   *  direction.
   *
   *  @param velocities An array whose values correspond to the North,
   *  South, East and West portions of the desired velocity.
   *
   *  @param duration The amount of time in milliseconds that the
   *  chopper should stay on this course before going back to
   *  hovering.
   */
  public static void setVelocity(double[] velocities, long duration) {
    sendTask(NavVel.taskFor(velocities, duration));
  }

  /**
   *  Tell the helicopter to travel to a specific location.
   *
   *  @param location The target latitude, longitude and altitude.
   */
  public static void setTargetLocation(Position location, double velocity,
				       double targetRadius) {
    sendTask(NavDest.taskFor(location.getElevation(), location.getLongitude(),
			     location.getLatitude(), velocity, targetRadius));
  }

  /**
   *  Send raw motor speed values to the chopper. Each speed is
   *  assumed to be a magic value, because the API on the chopper
   *  doesn't give any indication as to the units for these values,
   *  nor does it mention that these values can be set. Huzzah!
   *
   *  @param speeds An array full of magic.
   */
  public static void setMotorSpeeds(double[] speeds) {
    String taskString = navPrefix + "MANUAL";
    for (int i=0; i<4; i++) {
      taskString += ":" + speeds[i];
    }
    DataReceiver.sendToDefault(taskString);
  }

  /**
   *  Send a navigation task to the chopper.
   *
   *  @param task The task to issue.
   */
  private static void sendTask(NavTask task) {
    DataReceiver.sendToDefault(task.toString());
    Debug.log("Sent new navigation task: " + task.toString());
  }
}