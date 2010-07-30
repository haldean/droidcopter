/**
 * 
 */
package org.haldean.chopper;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Benjamin Bardin
 *
 */
public class PersistentThread extends Thread {
	AtomicBoolean stillAlive = new AtomicBoolean(false);
	Throwable myBad = null;
	
	public void run() {
		super.run();
	}
	/*
		while (stillAlive.get()) {
			try {
				super.run();
			}
			catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}*/
	
	public Throwable getLastThrowable() {
		synchronized (myBad) {
			if (myBad == null) {
				return null;
			}
			else {
				Throwable myCopy = new Throwable(myBad);
				myCopy.setStackTrace(myBad.getStackTrace());
				return myCopy;
			}
		}
	}
	
	public void setPersistent(boolean isPersistent) {
		stillAlive.set(isPersistent);
	}
	
	/**
	 * Constructs a persistent thread.
	 */
	public PersistentThread() {
		super();
	}

	/**
	 * 
	 * @param runnable
	 */
	public PersistentThread(Runnable runnable) {
		super(runnable);
	}

	/**
	 * @param threadName
	 */
	public PersistentThread(String threadName) {
		super(threadName);
	}

	/**
	 * @param runnable
	 * @param threadName
	 */
	public PersistentThread(Runnable runnable, String threadName) {
		super(runnable, threadName);
	}

	/**
	 * @param group
	 * @param runnable
	 */
	public PersistentThread(ThreadGroup group, Runnable runnable) {
		super(group, runnable);
	}

	/**
	 * @param group
	 * @param threadName
	 */
	public PersistentThread(ThreadGroup group, String threadName) {
		super(group, threadName);
	}

	/**
	 * @param group
	 * @param runnable
	 * @param threadName
	 */
	public PersistentThread(ThreadGroup group, Runnable runnable,
			String threadName) {
		super(group, runnable, threadName);
	}

	/**
	 * @param group
	 * @param runnable
	 * @param threadName
	 * @param stackSize
	 */
	public PersistentThread(ThreadGroup group, Runnable runnable,
			String threadName, long stackSize) {
		super(group, runnable, threadName, stackSize);
	}

}
