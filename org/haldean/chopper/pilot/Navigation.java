package org.haldean.chopper.pilot;

import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import org.haldean.chopper.nav.NavData;
import org.haldean.chopper.nav.NavList;

import android.util.Log;

/**
 * Handles Navigation routines; calculates next target velocity vector. <P>
 * 
 * May send the following messages to registered Receivables:<br>
 * <pre>
 * NAV:AUTOTASK:&lt;travel_plan_index&gt;:&lt;serialized_NavTask&gt;
 * </pre>
 * 
 * May receive the following messages from Chopper components:
 * <pre>
 * NAV:
 *     SET:
 *         MANUAL:&lt;north_south_vel&gt;:&lt;east_west_vel&gt;:&lt;up_down_vel&gt;:&lt;orientation&gt;
 *         AUTOPILOT
 *         AUTOTASK:&lt;travel_plan_index&gt;:&lt;serialized_NavTask&gt;
 *         FLIGHTPLAN:&lt;new_plan&gt;
 *     GET:AUTOTASKS
 * CSYS:
 *      NOCONN
 *      LOWPOWER
 * </pre>
 * 
 * @author Benjamin Bardin
 */
public class Navigation implements Constants, Receivable {
	
	/** Tag for logging */
	public static final String TAG = "chopper.Navigation";
	
	/** How long (in ms) Navigation should instruct the chopper to hover
	 * when autopilot has run out of NavTasks */
	public static final int HOVER_PAUSE = 1000;
	
	public static final String THREE_HRS_IN_MS = "10800000";
	
	/** Velocity to achieve.  Must be locked on each read/write. */
	private double[] mTarget = new double[4];
	
	/** Used to calculate next nav vector; prevents the thread
	 * from holding a lock on mTarget itself for too long */
	private double[] mTempTarget = new double[4];
	
	/** Chopper's navigation status */
	private final AtomicInteger mNavStatus = new AtomicInteger(NAV_STATUSES - 1);
	
	/** Holds all flight plans */
	private Vector<NavData> mTravelPlans = new Vector<NavData>(); //Vector --> already thread-safe
	
	/** Different flight plans depending on Nav status */
	private NavData mLowPower;
	private NavData mFlightPath;
	private NavData mOnMyOwn;
	
	/** Handle for other chopper components */
	private ChopperStatus mStatus;
	
	/** Registered receivers */
	private LinkedList<Receivable> mRec;
	
	private NavTask mTask;
	
	/**
	 * Constructs a navigation object, initializes NavLists
	 * @param status The ChopperStatus from which to obtain location information. 
	 */
	public Navigation(ChopperStatus status) {
		if (status == null) {
			throw new NullPointerException();
		}
		mRec = new LinkedList<Receivable>();
		mLowPower = NavList.fromString("{ -1}");
		mFlightPath = NavList.fromString("{ -2}");
		mOnMyOwn = NavList.fromString("{ -3}");
		
		mTask = new NavTask(status);
		
		mTravelPlans.add(mLowPower);
		mTravelPlans.add(mFlightPath);
		mTravelPlans.add(mOnMyOwn);

		mStatus = status;
		
		//FOR TESTING ONLY:
		/*String taskList = "{ " + 
			"{ DEST!targ1!300!-74.012345!40.74!10!100!232 { DEST!targ2!300!-77.07950!38.97300!100!250!233 " +
				" DEST!targ3!587!-117.15!32.72!10!600!234 } } }";*/
		String taskList = "{ VEL!name1!1!0!0!0!180000!-10 VEL!name1!-1!0!0!0!10000!-10 VEL!name1!0!1!0!0!10000!-10 VEL!name1!0!-1!0!0!10000!-10 VEL!name1!0!0!1!0!10000!-10 VEL!name1!0!0!-1!0!10000!-10 -4}";
		setTask(BASIC_AUTO, taskList);
		setTask(NO_CONN, "{ VEL!No_Conn!0!0!-1!0!" + THREE_HRS_IN_MS + "!-1!-5 -6}");
		setTask(LOW_POWER, "{ VEL!Low_Power!0!0!-1!0!"+ THREE_HRS_IN_MS + "!-1!-7 -8}");
		//mNavStatus.set(BASIC_AUTO);
		
		
		//autoPilot(true);
	}
	
	
	
	/** 
	 * Evaluates a new navigation vector, based on current status and the relevant NavTask.
	 * @param newNavTarget If supplied and has length >= 4, writes the new target here.  May be null.
	 */
	public void evalNextVector(double[] newNavTarget) {

		/*Determine what the current task should be.  Copies to a local variable in case
		 * 'status'	changes during execution of the method */
		int thisStatus = mNavStatus.get();
		
		NavData myList = mTravelPlans.get(thisStatus);
		Log.v(TAG, "Nav using index " + thisStatus + ", task " + myList.toString());
		if (mTask.isComplete(myList)) {
			Log.i(TAG, "Nav is Hovering");
			hover();
			return;
		}
		mTask.getVelocity(myList, mTempTarget);
		setTarget(mTempTarget);
		if (newNavTarget != null) {
			getTarget(newNavTarget);
		}

		//long interval = mTask.getInterval(myList);
		//Log.v(TAG, "Nav Interval is " + interval);
		//Send the current NavList to the server, in case any tasks have been completed
		updateReceivers("NAV:AUTOTASK:" + thisStatus + ":" + myList.toString());
	}
	
	/**
	 * Writes current navigation target vector into supplied
	 * array.  If the data is locked, immediately returns with
	 * neither data update nor warning.
	 *
	 * @param expectedValues The array in which to write the
	 * vector--must be at least of length 4.
	 */
	public void getTarget(double[] navTarget) {
		if (navTarget.length < 4)
			return;
		synchronized (mTarget) {
			System.arraycopy(mTarget, 0, navTarget, 0, 4);
		}
	}
	
	/**
	 * Obtains all scheduled flight plans
	 * @return An array of strings representing all flight plans (serialized form)
	 */
	public String[] getTasks() {
		ListIterator<NavData> iterator = mTravelPlans.listIterator();
		String[] myTasks = new String[mTravelPlans.size()];
		while (iterator.hasNext()) {
			myTasks[iterator.nextIndex()] = iterator.next().toString();
		}
		return myTasks;
	}
	
	/** Orders the chopper to remain in place */
	private void hover() {
		
		for (int i = 0; i < 3; i++) {
			mTempTarget[i] = 0;
		}
		mTempTarget[3] = mStatus.getReadingField(AZIMUTH);
		setTarget(mTempTarget);
	}
	
	/**
	 * Receive a message.
	 * @param source The source of the message, if a reply is needed.  May be null.
	 */
	public void receiveMessage(String msg, Receivable source) {
		String[] parts = msg.split(":");
		
		if (parts[0].equals("NAV")) {
			if (parts[1].equals("SET")) {
				Log.v(TAG, "Updating Nav Status");				
				
				if (parts[2].equals("AUTOPILOT")) {
					mNavStatus.set(BASIC_AUTO);
					updateReceivers("GUID:ABSVEC");
					updateReceivers("GUID:AUTOMATIC");
					//autoPilot(true);
				}
				if (parts[2].equals("AUTOTASK")) {
					Integer taskList = new Integer(parts[3]);
					Log.v(TAG, "Nav setting index " + taskList + " to " + parts[4]);
					setTask(taskList, parts[4]);
				}
				if (parts[2].equals("FLIGHTPLAN")) {
					mNavStatus.set(new Integer(parts[3]));
				}
			}
			if (parts[1].equals("GET")) {
				if (parts[2].equals("AUTOTASKS")) {
					String[] myTasks = getTasks();
					for (int i = 0; i < myTasks.length; i++) {
						if (source != null) {
							source.receiveMessage("NAV:AUTOTASK:" + i + ":" + myTasks[i], this);
						}
					}
				}
			}
		}
		if (parts[0].equals("CSYS")) {
			if (parts[1].equals("NOCONN")) {
				updateStatus(NO_CONN);
				//autoPilot(true);
			}
			if (parts[1].equals("LOWPOWER")) {
				updateStatus(LOW_POWER);
				//autoPilot(true);
			}
		}
	}
	
	/**
	 * Registers a receiver to receive Nav updates.
	 * @param rec
	 */
	public void registerReceiver(Receivable rec) {
		synchronized (mRec) {
			mRec.add(rec);
		}
	}
	
	/**
	 * Sets the navigation target vector to the values contained by the supplied array (length must be at least 4).
	 * @param newTarget The new target vector.
	 */
	private void setTarget(double[] newTarget) {
		if (newTarget.length < 4) {
			return;
		}
		synchronized (mTarget) {
			String newNav = "New_Nav_Vector: ";
			for (int i = 0; i < 4; i++) {				
				mTarget[i] = newTarget[i];
				newNav += newTarget[i] + ":";
			}
			updateReceivers(newNav);
		}
	}
	
	/** 
	 * Sets a supplied NavList as flight plan for the specified Nav status.
	 * @param whichPlan The Nav status for which to set the new flight plan
	 * @param myTask The new flight plan
	 */
	private void setTask(int whichPlan, String myTask) {
		Log.v(TAG, "Nav about to myList");
		NavList myList = null;
		try {
			myList = NavList.fromString(myTask);
		} catch (Exception e) {
			e.printStackTrace();
		}
		Log.v(TAG, "Nav myList is " + myList);
		if (myList != null) {
			//Make change:
			mTravelPlans.set(whichPlan, myList);
			Log.i(TAG, "Nav set index " + whichPlan + " to task " + myList);
			//Confirm change to server:
			updateReceivers("NAV:AUTOTASK:" + whichPlan + ":" + myList.toString());
		}
		else {
			Log.e(TAG, "Nav received invalid task!");
		}
	}
	
	/**
	 * Updates all receivers
	 * @param str The message to send.
	 */
	private void updateReceivers(String str) {
		synchronized (mRec) {
			ListIterator<Receivable> myList = mRec.listIterator();
			while (myList.hasNext()) {
				myList.next().receiveMessage(str, this);
			}
		}
	}
	
	/**
	 * Compares the supplied status with the current status; stores the most important.
	 * @param newstatus The new (potential) status
	 */
	private void updateStatus(int newstatus) {
		mNavStatus.set(Math.min(mNavStatus.get(), newstatus));
	}
}
