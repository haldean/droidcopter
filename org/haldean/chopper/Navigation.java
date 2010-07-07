package org.haldean.chopper;

import java.util.LinkedList;
import java.util.Vector;
import java.util.concurrent.locks.ReentrantLock;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;


public class Navigation extends Thread implements Constants {
	public static double[] target = new double[4];
	public static ReentrantLock targetLock;
	
	private static double[] tempTarget = new double[4];
	
	public static boolean autopilot = false;
	public static Handler mHandler;
	private static int status;
	
	private static Vector<LinkedList<NavTask>> travelPlans = new Vector<LinkedList<NavTask>>();
	
	private static LinkedList<NavTask> flightPath = new LinkedList<NavTask>();
	private static LinkedList<NavTask> lowPower = new LinkedList<NavTask>();
	private static LinkedList<NavTask> onMyOwn = new LinkedList<NavTask>();
	
	private static NavTask currentTask;
	
	
	
	public Navigation() {
		super("Navigation");
		travelPlans.add(lowPower);
		travelPlans.add(flightPath);
		travelPlans.add(onMyOwn);
		
		targetLock = new ReentrantLock();
	}
	
	public void run() {
		Looper.prepare();
		mHandler = new Handler() {
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
	
	public static void updateStatus(int newstatus) {
		status = Math.min(status, newstatus);
	}
	
	private static void evalNextVector() {
		//Determine what the current task should be
		LinkedList<NavTask> myList = travelPlans.get(status);
		if (myList.size() == 0) {
			hover();
			return;
		}
		currentTask = myList.getFirst();
		while (currentTask.isComplete()) {
			myList.removeFirst();
			if (myList.size() == 0) {
				hover();
				return;
			}
			currentTask = myList.getFirst();
		}

		currentTask.setVelocity(tempTarget);
		targetLock.lock();
		for (int i = 0; i < 4; i++)
			target[i] = tempTarget[i];
		targetLock.unlock();
		
		long interval = currentTask.getInterval();
		if (interval > 0)
			mHandler.sendEmptyMessageDelayed(EVALNAV, interval);
		
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
		mHandler.sendEmptyMessageDelayed(EVALNAV, HOVERPAUSE);
	}
	
	public static void autoPilot(boolean onoff) {
		autopilot = onoff;
	}
}
