package org.haldean.chopper;

import java.util.ListIterator;
import java.util.Vector;
import java.util.concurrent.locks.ReentrantLock;

import org.haldean.chopper.nav.NavList;
import org.haldean.chopper.nav.NavTask;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

/**
 * Handles Navigation routines; calculates next target velocity vector
 * @author Benjamin Bardin
 */
public class Navigation extends Thread implements Constants {
	
	/**
	 * How long (in ms) Navigation should instruct the chopper to hover when autopilot has run out of NavTasks
	 */
	public static final int HOVERPAUSE = 10000;
	
	/**
	 * Arbitrary value used by some NavTasks in deciding when next to evaluate the next navigation vector.
	 * Smaller values mean more accurate navigation vectors at the expense of CPU time.
	 */
	public static final int NAVPAUSE = 1000;
	
	/**
	 * Velocity to achieve.  Must be externally locked on each read/write.
	 * @see #targetLock targetLock
	 */
	public static double[] target = new double[4];
	
	/**
	 * Lock for target[]
	 * @see #target target[]
	 */
	public static ReentrantLock targetLock;
	
	/* Stores local variable*/
	private static double[] tempTarget = new double[4];
	
	/* True if autopilot is engaged */
	private static boolean autopilot = false;
	
	/* Chopper's navigation status */
	private static int status = NUMNAVSTATUSES;
	
	/* Holds all flight plans */
	private static Vector<NavTask> travelPlans = new Vector<NavTask>(); //Vector --> already synchronized
	
	/* Different flight plans depending on Nav status */
	private static NavTask lowPower;
	private static NavTask flightPath;
	private static NavTask onMyOwn;
	
	
	
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
	 * Starts the navigation thread
	 */
	public void run() {
		Looper.prepare();
		handler = new Handler() {
			public void handleMessage(Message msg)
            {
                switch (msg.what) {
                case EVALNAV:
                	if (autopilot)
                		evalNextVector();
                	break;
                }
            } 
		};
		
		//FOR TESTING ONLY:
		String taskList = "{ DEST!300!-74.012345!40.74!10!100 { DEST!300!-77.07950!38.97300!100!250 DEST!587!-117.15!32.72!10!600 } }";
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
	 * @param whichPlan The index at which to set the new flight plan
	 * @param myTask The new flight plan
	 */
	public static void setTask(int whichPlan, String myTask) {
		NavList myList = NavList.fromString(myTask);
		System.out.println("UberList, length " + myList.size());
		if (myList != null)
			travelPlans.set(whichPlan, myList);
	}
	
	/**
	 * Compares the supplied status with the current status; stores the most important.
	 * @param newstatus The new (potential) status
	 */
	public static void updateStatus(int newstatus) {
		status = Math.min(status, newstatus);
	}
	
	/* Evaluates a new navigation vector, based on current status and the relevant NavTask */
	private static void evalNextVector() {
		System.out.println("Evaluating next nav vector");
		//Determine what the current task should be
		NavTask myList = travelPlans.get(status);
		if (myList.isComplete()) {
			System.out.println("Hovering");
			hover();
			return;
		}
		myList.getVelocity(tempTarget);
		targetLock.lock();
		System.out.print("New vector: ");
		for (int i = 0; i < 4; i++) {
			target[i] = tempTarget[i];
			System.out.print(target[i] + " ");
		}
		System.out.println();
		targetLock.unlock();
		
		long interval = myList.getInterval();
		handler.removeMessages(EVALNAV);
		if (interval > 0)
			handler.sendEmptyMessageDelayed(EVALNAV, interval);
		else
			handler.sendEmptyMessage(EVALNAV);
	}
	
	/* Orders the chopper to remain in place */
	private static void hover() {
		targetLock.lock();
		for (int i = 0; i < 3; i++) {
			target[i] = 0;
		}
		if (ChopperStatus.readingLock[AZIMUTH].tryLock()) {
			target[3] = ChopperStatus.reading[AZIMUTH];
			ChopperStatus.readingLock[AZIMUTH].unlock();
		}
		targetLock.unlock();
		handler.removeMessages(EVALNAV);
		handler.sendEmptyMessageDelayed(EVALNAV, HOVERPAUSE);
	}
	
	/**
	 * Changes autopilot status (on or off)
	 * @param onoff The new autopilot status
	 */
	public static void autoPilot(boolean onoff) {
		autopilot = onoff;
		handler.removeMessages(EVALNAV);
		if (autopilot) {
			handler.sendEmptyMessage(EVALNAV);
			System.out.println("Autopilot engaged");
		}
	}
}
