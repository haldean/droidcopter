package org.haldean.chopper.server;

import org.haldean.chopper.nav.NavDest;
import org.haldean.chopper.nav.NavTask;
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
  private static String navGoToManual = "NAV:SET:MANUAL";
  private static String navGoToAutomatic = "NAV:SET:AUTOPILOT";

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
    sendTask(NavDest.taskFor(location.getElevation(), location.getLongitude().getDegrees(),
			     location.getLatitude().getDegrees(), velocity, targetRadius));
  }

  /**
   *  Send raw motor speed values to the chopper.
   *
   *  @param speeds An array full of magic, where the values
   *  correspond to the new speeds for the "north", "south", "east"
   *  and "west" motors.
   *
   *  Note that the motor "directions" aren't directions at
   *  all. They're convenient ways of describing which motor is
   *  which. Changing the "north" motor doesn't mean you are changing
   *  the motor closest to North.
   */
  public static void setMotorSpeeds(double[] speeds) {
    String taskString = "GUID:VECTOR";
    for (int i=0; i<4; i++) {
      taskString += ":" + speeds[i];
    }
    DataReceiver.sendToDefault(navGoToManual);
    DataReceiver.sendToDefault(taskString);
  }

  /**
   *  Send PID tuning values to the helicopter.
   *
   *  @param motor The index of the motor whose loop needs tuning.
   *  @param parameter The parameter (0 for P, 1 for I, 2 for D) to tune.
   *  @param value The new value for the parameter.
   */
  public static void tunePid(int motor, int parameter, double value) {
    String taskString = "GUID:PID:" + motor + ":" + parameter + ":" + value;
    DataReceiver.sendToDefault(taskString);
  }

  /**
   *  Send a navigation task to the chopper.
   *
   *  @param task The task to issue.
   */
  private static void sendTask(NavTask task) {
    DataReceiver.sendToDefault(navGoToAutomatic);
    DataReceiver.sendToDefault(task.toString());
    Debug.log("Sent new navigation task: " + task.toString());
  }
}