/**
 * 
 */
package org.haldean.chopper;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A thread of execution in a program.
 * @author Benjamin Bardin
 */
public class PersistentThread extends Thread {
	/** Whether the thread should restart on task termination */
	private AtomicBoolean mStillAlive = new AtomicBoolean(true);
	
	/** The last throwable */
	private Throwable mBad = null;
	
	/** Callback to run on task termination */
	private Runnable mCallback;
	
	/**
	 * Constructs a persistent thread.
	 */
	public PersistentThread() {
		super();
	}
	
	/**
	 * Constructs a persistent thread.
	 * @param runnable the object whose run method is called.
	 */
	public PersistentThread(Runnable runnable) {
		super(runnable);
	}
	
	/**
	 * Constructs a persistent thread.
	 * @param runnable the object whose run method is called.
	 * @param threadName the name of the new thread.
	 */
	public PersistentThread(Runnable runnable, String threadName) {
		super(runnable, threadName);
	}
	
	/**
	 * Constructs a persistent thread.
	 * @param threadName the name of the new thread.
	 */
	public PersistentThread(String threadName) {
		super(threadName);
	}
	
	/**
	 * Constructs a persistent thread.
	 * @param group the thread group.
	 * @param runnable the object whose run method is called.
	 */
	public PersistentThread(ThreadGroup group, Runnable runnable) {
		super(group, runnable);
	}

	/**
	 * Constructs a persistent thread.
	 * @param group the thread group.
	 * @param runnable the object whose run method is called.
	 * @param threadName the name of the new thread.
	 */
	public PersistentThread(ThreadGroup group, Runnable runnable,
			String threadName) {
		super(group, runnable, threadName);
	}

	/**
	 * Constructs a persistent thread.
	 * @param group the thread group.
	 * @param runnable the object whose run method is called.
	 * @param threadName the name of the new thread.
	 * @param stackSize the desired stack size for the new thread, or zero to indicate that this parameter is to be ignored.
	 */
	public PersistentThread(ThreadGroup group, Runnable runnable,
			String threadName, long stackSize) {
		super(group, runnable, threadName, stackSize);
	}

	/**
	 * Constructs a persistent thread.
	 * @param group the thread group.
	 * @param threadName the name of the new thread.
	 */
	public PersistentThread(ThreadGroup group, String threadName) {
		super(group, threadName);
	}

	/**
	 * Retrieves the last throwable, if any.
	 * @return The last throwable thrown.  May be null.
	 */
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

	/**
	 * Runs the task.
	 */
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
				if (mBad != null) {
					synchronized (mBad) {
						mBad = t;
					}
				}
				else {
					mBad = t;
				}
			}
		} while (mStillAlive.get());
	}

	/**
	 * Sets a callback to be run should the core task terminate.
	 * @param onReset The callback.
	 */
	public void setOnTerminationCallback(Runnable onReset) {
		if (mCallback != null) {
			synchronized (mCallback) {
				mCallback = onReset;
			}
		}
		else {
			mCallback = onReset;
		}
	}

	/**
	 * Sets whether the thread should restart on task termination.
	 * @param isPersistent The persistence of the thread.
	 */
	public void setPersistent(boolean isPersistent) {
		mStillAlive.set(isPersistent);
	}

}
