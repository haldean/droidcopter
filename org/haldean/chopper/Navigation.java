package org.haldean.chopper;

import java.util.ListIterator;
import java.util.Vector;
import java.util.concurrent.locks.ReentrantLock;

import org.haldean.chopper.nav.NavList;
import org.haldean.chopper.nav.NavTask;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;


public class Navigation extends Thread implements Constants {
	public static double[] target = new double[4];
	public static ReentrantLock targetLock;
	
	private static double[] tempTarget = new double[4];
	
	public static boolean autopilot = false;
	private static Handler handler;
	private static int status;
	
	private static Vector<NavTask> travelPlans = new Vector<NavTask>(); //Vector --> already synchronized
	
	private static NavTask flightPath;
	private static NavTask lowPower;
	private static NavTask onMyOwn;
	
	private static NavTask myList;
	
	
	
	public Navigation() {
		super("Navigation");
		flightPath = new NavList();
		lowPower = new NavList();
		onMyOwn = new NavList();
		
		travelPlans.add(lowPower);
		travelPlans.add(flightPath);
		travelPlans.add(onMyOwn);
		
		targetLock = new ReentrantLock();
	}
	
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
		Looper.loop();
	}
	
	public static String[] getTasks() {
		ListIterator<NavTask> iterator = travelPlans.listIterator();
		String[] myTasks = new String[travelPlans.size()];
		while (iterator.hasNext()) {
			myTasks[iterator.nextIndex()] = iterator.next().toString();
		}
		return myTasks;
	}
	public static void setTask(int whichPlan, String myTask) {
		NavList myList = NavList.fromString(myTask);
		if (myList != null)
			travelPlans.set(whichPlan, myList);
	}
	
	public static void updateStatus(int newstatus) {
		status = Math.min(status, newstatus);
	}
	
	private static void evalNextVector() {
		//Determine what the current task should be
		myList = travelPlans.get(status);
		if (myList.isComplete()) {
			hover();
			return;
		}
		myList.getVelocity(tempTarget);
		
		targetLock.lock();
		for (int i = 0; i < 4; i++)
			target[i] = tempTarget[i];
		targetLock.unlock();
		
		long interval = myList.getInterval();
		handler.removeMessages(EVALNAV);
		if (interval > 0)
			handler.sendEmptyMessageDelayed(EVALNAV, interval);
		else
			handler.sendEmptyMessage(EVALNAV);
	}
	
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
	
	public static void autoPilot(boolean onoff) {
		autopilot = onoff;
		handler.removeMessages(EVALNAV);
		handler.sendEmptyMessage(EVALNAV);
	}
}
