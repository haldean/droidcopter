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
	AtomicBoolean mStillAlive = new AtomicBoolean(false);
	Throwable mBad = null;
	Runnable mCallback;
	
	public void run() {
		do {
			try {
				super.run();
			}
			catch (Throwable t) {
				t.printStackTrace();
				if (mBad != null) {
					synchronized (mBad) {
						mBad = t;
					}
				}
				else {
					mBad = t;
				}
			}
			try {
				if (mCallback != null) {
					synchronized (mCallback) {
						mCallback.run();
					}
				}
			}
			catch (Throwable t) {
				t.printStackTrace();
				synchronized (mBad) {
					mBad = t;
				}
			}
		} while (mStillAlive.get());
	}
	
	public Throwable getLastThrowable() {
		synchronized (mBad) {
			if (mBad == null) {
				return null;
			}
			else {
				Throwable mCopy = new Throwable(mBad);
				mCopy.setStackTrace(mBad.getStackTrace());
				return mCopy;
			}
		}
	}
	
	public void setOnResetCallback(Runnable onReset) {
		if (mCallback != null) {
			synchronized (mCallback) {
				mCallback = onReset;
			}
		}
		else {
			mCallback = onReset;
		}
	}
	
	public void setPersistent(boolean isPersistent) {
		mStillAlive.set(isPersistent);
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
