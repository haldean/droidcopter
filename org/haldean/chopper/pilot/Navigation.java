package org.haldean.chopper.pilot;

import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.haldean.chopper.nav.NavData;
import org.haldean.chopper.nav.NavList;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
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
public class Navigation implements Runnable, Constants, Receivable {
	
	/** Tag for logging */
	public static final String TAG = "chopper.Navigation";
	
	/** How long (in ms) Navigation should instruct the chopper to hover
	 * when autopilot has run out of NavTasks */
	public static final int HOVER_PAUSE = 1000;
	
	/** Velocity to achieve.  Must be locked on each read/write. */
	private double[] mTarget = new double[4];
	
	/** Lock for target[] */
	private ReentrantLock mTargetLock;
	
	/** Used to calculate next nav vector; prevents the thread
	 * from holding a lock on mTarget itself for too long */
	private double[] mTempTarget = new double[4];
	
	/** True if autopilot is engaged */
	private final AtomicBoolean mAutoPilot = new AtomicBoolean(false);
	
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
	
	/** Handles messages */
	private Handler mHandler;
	
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
		
		mTargetLock = new ReentrantLock();

		mStatus = status;
	}
	
	/**
	 * Changes autopilot status (on or off)
	 * @param onoff The new autopilot status
	 */
	public void autoPilot(boolean onoff) {
		mAutoPilot.set(onoff);
		mHandler.removeMessages(EVAL_NAV);
		if (mAutoPilot.get()) {
			mHandler.sendEmptyMessage(EVAL_NAV);
			Log.i(TAG, "Autopilot engaged");
		}
	}
	
	/** Evaluates a new navigation vector, based on current status and the relevant NavTask */
	private void evalNextVector() {

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
		
		
		
		long interval = mTask.getInterval(myList);
		Log.v(TAG, "Nav Interval is " + interval);
		//Send the current NavList to the server, in case any tasks have been completed
		updateReceivers("NAV:AUTOTASK:" + thisStatus + ":" + myList.toString());
		
		mHandler.removeMessages(EVAL_NAV);
		if (interval > 0) {
			mHandler.sendEmptyMessageDelayed(EVAL_NAV, interval);
		}
		else {
			mHandler.sendEmptyMessage(EVAL_NAV);
		}
	}
	
	/**
	 * Starts the navigation thread
	 */
	public void run() {
		Looper.prepare();
		Thread.currentThread().setName("Navigation");
		mHandler = new Handler() {
			public void handleMessage(Message msg)
            {
                switch (msg.what) {
                case EVAL_NAV:
                	if (mAutoPilot.get())
                		evalNextVector();
                	break;
                }
            }
		};

		//FOR TESTING ONLY:
		/*String taskList = "{ { VEL!0!10!0!0!300 VEL!5!10!5!10!180 } " + 
			"{ DEST!300!-74.012345!40.74!10!100 { DEST!300!-77.07950!38.97300!100!250 " +
				" DEST!587!-117.15!32.72!10!600 } } }";*/
		String taskList = "{ VEL!name1!1!0!0!0!10000!-10 VEL!name1!-1!0!0!0!10000!-10 VEL!name1!0!1!0!0!10000!-10 VEL!name1!0!-1!0!0!10000!-10 VEL!name1!0!0!1!0!10000!-10 VEL!name1!0!0!-1!0!10000!-10 -4}";
		setTask(BASIC_AUTO, taskList);
		setTask(NO_CONN, "{ VEL!No_Conn!0!0!-1!0!1000000!-1!-5 -6}");
		setTask(LOW_POWER, "{ VEL!Low_Power!0!0!-1!0!1000000!-1!-7 -8}");
		
		mNavStatus.set(BASIC_AUTO);
		autoPilot(true);
		
		Looper.loop();
	}
	
	/**
	 * Obtains current navigation target vector.  If the data is
	 * locked, immediately throws an exception.
	 *
	 * @return A new array containing the navigation target vector.
	 * @throws IllegalAccessException If the data is currently locked.
	 */
	public double[] getTarget() throws IllegalAccessException {
		double[] myTarget = new double[4];
		if (mTargetLock.tryLock()) {
			for (int i = 0; i < 4; i++) {
				myTarget[i] = mTarget[i];
			}
			mTargetLock.unlock();
		}
		else {
			Log.w(TAG, "Target vector is locked.");
			throw new IllegalAccessException();
		}
		return mTarget;
	}
	
	/**
	 * Writes current navigation target vector into supplied
	 * array.  If the data is locked, immediately returns with
	 * neither data update nor warning.
	 *
	 * @param expectedValues The array in which to write the
	 * vector--must be at least of length 4.
	 */
	public void getTarget(double[] expectedValues) {
		if (expectedValues.length < 4)
			return;
		if (mTargetLock.tryLock()) {
			for (int i = 0; i < 4; i++) {
				expectedValues[i] = mTarget[i];
			}
			mTargetLock.unlock();
		}
		else {
			Log.w(TAG, "Target vector is locked.");
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
		mTempTarget[3] = mStatus.getReadingFieldNow(AZIMUTH, mTempTarget[3]);
		setTarget(mTempTarget);
		mHandler.removeMessages(EVAL_NAV);
		mHandler.sendEmptyMessageDelayed(EVAL_NAV, HOVER_PAUSE);
	}
	
	/**
	 * Receive a message.
	 * @param source The source of the message, if a reply is needed.  May be null.
	 */
	public void receiveMessage(String msg, Receivable source) {
		Log.d(TAG, "Receiving " + msg);
		String[] parts = msg.split(":");
		if (parts[0].equals("NAV")) {
			if (parts[1].equals("SET")) {
				Log.v(TAG, "Updating Nav Status");				
				if (parts[2].equals("MANUAL")) {
					autoPilot(false);
					if (parts.length > 3) {
						updateReceivers("GUID:LOCALVEC");
						updateReceivers("GUID:AUTOMATIC");
						double[] newTarget = new double[4];
						for (int i = 0; i < 4; i++) {
							newTarget[i] = new Double(parts[i + 3]);
						}
						setTarget(newTarget);
					}
				}
				if (parts[2].equals("AUTOPILOT")) {
					mNavStatus.set(BASIC_AUTO);
					updateReceivers("GUID:ABSVEC");
					updateReceivers("GUID:AUTOMATIC");
					autoPilot(true);
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
				Log.d(TAG, "no conn in Nav");
				updateStatus(NO_CONN);
				autoPilot(true);
				updateReceivers("GUID:AUTOMATIC");
			}
			if (parts[1].equals("LOWPOWER")) {
				updateStatus(LOW_POWER);
				autoPilot(true);
				updateReceivers("GUID:AUTOMATIC");
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
	protected void setTarget(double[] newTarget) {
		if (newTarget.length < 4) {
			return;
		}
		final double[] myCopy = newTarget.clone();
		new Thread() {
			public void run() {
			String newNav = "New_Nav_Vector: ";
			mTargetLock.lock();
			for (int i = 0; i < 4; i++) {
				mTarget[i] = myCopy[i];
				newNav += myCopy[i] + ":";
			}
			mTargetLock.unlock();
			Log.i(TAG, newNav);
			updateReceivers(newNav);
			}
		}.start();
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
			if (mAutoPilot.get())
				evalNextVector();
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
