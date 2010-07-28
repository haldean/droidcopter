package org.haldean.chopper;

import java.util.ListIterator;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.haldean.chopper.nav.NavList;
import org.haldean.chopper.nav.NavTask;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

/**
 * Handles Navigation routines; calculates next target velocity vector.
 * @author Benjamin Bardin
 */
public class Navigation extends Thread implements Constants {
	
	/* Velocity to achieve.  Must be externally locked on each read/write. */
	private static double[] target = new double[4];
	
	/* Lock for target[] */
	private static ReentrantLock targetLock;
	
	/* How long (in ms) Navigation should instruct the chopper to hover
	 * when autopilot has run out of NavTasks */
	private static final int HOVERPAUSE = 10000;
	
	/* Stores local variable*/
	private static double[] tempTarget = new double[4];
	
	/* True if autopilot is engaged */
	private static final AtomicBoolean autopilot = new AtomicBoolean(false);
	
	/* Chopper's navigation status */
	private static final AtomicInteger status = new AtomicInteger(NUMNAVSTATUSES);
	
	/* Holds all flight plans */
	private static Vector<NavTask> travelPlans = new Vector<NavTask>(); //Vector --> already thread-safe
	
	/* Different flight plans depending on Nav status */
	private static NavTask lowPower;
	private static NavTask flightPath;
	private static NavTask onMyOwn;
	
	/* Tag for logging */
	private static String TAG = "chopper.Navigation";
	
	/* Handles messages */
	private static Handler handler;
	
	/**
	 * Constructs a navigation object, initializes NavLists
	 */
	public Navigation() {
		super("Navigation");
		lowPower = new NavList();
		flightPath = new NavList();
		onMyOwn = new NavList();
		
		travelPlans.add(lowPower);
		travelPlans.add(flightPath);
		travelPlans.add(onMyOwn);
		
		targetLock = new ReentrantLock();
	}
	
	/**
	 * Writes current navigation target vector into supplied array.  If the data is locked, immediately returns with neither data update nor warning.
	 * @param expectedValues  The array in which to write the vector--must be at least of length 4.
	 */
	public static void getTarget(double[] expectedValues) {
		if (expectedValues.length < 4)
			return;
		if (targetLock.tryLock()) {
			for (int i = 0; i < 4; i++) {
				expectedValues[i] = target[i];
			}
			targetLock.unlock();
		}
		else {
			Log.w(TAG, "Target vector is locked.");
		}
	}
	
	/**
	 * Obtains current navigation target vector.  If the data is locked, immediately throws an exception.
	 * @return A new array containing the navigation target vector.
	 * @throws IllegalAccessException If the data is currently locked.
	 */
	public static double[] getTarget() throws IllegalAccessException {
		double[] myTarget = new double[4];
		if (targetLock.tryLock()) {
			for (int i = 0; i < 4; i++) {
				myTarget[i] = target[i];
			}
			targetLock.unlock();
		}
		else {
			Log.w(TAG, "Target vector is locked.");
			throw new IllegalAccessException();
		}
		return target;
	}
	
	/**
	 * Sets the navigation target vector to the values contained by the supplied array (length must be at least 4).
	 * @param newTarget The new target vector.
	 */
	protected static void setTarget(double[] newTarget) {
		if (newTarget.length < 4) {
			return;
		}
		final double[] myCopy = newTarget.clone();
		new Thread() {
			public void run() {
			targetLock.lock();
			for (int i = 0; i < 4; i++) {
				target[i] = myCopy[i];
			}
			targetLock.unlock();
			}
		}.start();
	}
	
	/**
	 * Starts the navigation thread
	 */
	public void run() {
		Looper.prepare();
		Thread.currentThread().setName("Navigation");
		handler = new Handler() {
			public void handleMessage(Message msg)
            {
                switch (msg.what) {
                case EVALNAV:
                	if (autopilot.get())
                		evalNextVector();
                	break;
                }
            } 
		};
		
		//FOR TESTING ONLY:
		String taskList = "{ { VEL!0!10!0!0!300 VEL!5!10!5!10!180 } " + 
			"{ DEST!300!-74.012345!40.74!10!100 { DEST!300!-77.07950!38.97300!100!250 " +
				" DEST!587!-117.15!32.72!10!600 } } }";
		setTask(BASICAUTO, taskList);
		updateStatus(BASICAUTO);
		autoPilot(true);
		Looper.loop();
	}
	
	/**
	 * Obtains all scheduled flight plans
	 * @return An array of strings representing all flight plans (serialized form)
	 */
	public static String[] getTasks() {
		ListIterator<NavTask> iterator = travelPlans.listIterator();
		String[] myTasks = new String[travelPlans.size()];
		while (iterator.hasNext()) {
			myTasks[iterator.nextIndex()] = iterator.next().toString();
		}
		return myTasks;
	}
	
	/** 
	 * Sets a supplied NavList as flight plan for the specified Nav status.
	 * @param whichPlan The Nav status for which to set the new flight plan
	 * @param myTask The new flight plan
	 */
	public static void setTask(int whichPlan, String myTask) {
		NavList myList = NavList.fromString(myTask);
		if (myList != null) {
			//Make change:
			travelPlans.set(whichPlan, myList);
			
			//Confirm change to server:
			Comm.sendMessage("NAV:AUTOTASK:" + whichPlan + ":" + myList.toString());
		}
	}
	
	/**
	 * Compares the supplied status with the current status; stores the most important.
	 * @param newstatus The new (potential) status
	 */
	public static void updateStatus(int newstatus) {
		status.set(Math.min(status.get(), newstatus));
	}
	
	/* Evaluates a new navigation vector, based on current status and the relevant NavTask */
	private static void evalNextVector() {
		//System.out.println("Evaluating next nav vector");
		/*Determine what the current task should be.  Copies to a local variable in case
		 * 'status'	changes during execution of the method */
		int thisStatus = status.get();
		
		NavTask myList = travelPlans.get(thisStatus);
		if (myList.isComplete()) {
			System.out.println("Hovering");
			hover();
			return;
		}
		myList.getVelocity(tempTarget);
		setTarget(tempTarget);
		
		System.out.print("New NAV Vector: ");
		for (int i = 0; i < 4; i++) {
			System.out.print(tempTarget[i] + " ");
		}
		System.out.println();
		
		long interval = myList.getInterval();
		
		//Send the current NavList to the server, in case any tasks have been completed
		Comm.sendMessage("NAV:AUTOTASK:" + thisStatus + ":" + myList.toString());
		
		handler.removeMessages(EVALNAV);
		if (interval > 0)
			handler.sendEmptyMessageDelayed(EVALNAV, interval);
		else
			handler.sendEmptyMessage(EVALNAV);
	}
	
	/* Orders the chopper to remain in place */
	private static void hover() {
		
		for (int i = 0; i < 3; i++) {
			tempTarget[i] = 0;
		}
		tempTarget[3] = ChopperStatus.getReadingFieldNow(AZIMUTH, tempTarget[3]);
		setTarget(tempTarget);
		handler.removeMessages(EVALNAV);
		handler.sendEmptyMessageDelayed(EVALNAV, HOVERPAUSE);
	}
	
	/**
	 * Changes autopilot status (on or off)
	 * @param onoff The new autopilot status
	 */
	public static void autoPilot(boolean onoff) {
		autopilot.set(onoff);
		handler.removeMessages(EVALNAV);
		if (autopilot.get()) {
			handler.sendEmptyMessage(EVALNAV);
			System.out.println("Autopilot engaged");
		}
	}
}
