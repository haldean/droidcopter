package org.haldean.chopper.pilot;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.ErrorCallback;
import android.hardware.Camera.PreviewCallback;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;

/**
 * Interfaces with the camera to obtain preview frames and to take high-quality pictures. <P>
 * 
 * May send the following messages to registered Receivables:<br>
 * <pre>
 * IMAGE:
 *       AVAILABLESIZE:&lt;width&gt;:&lt;height&gt;
 *       FRAMESIZE:&lt;width&gt;:&lt;height&gt; (as a reply to IMAGE:GETPARAMS) 
 * </pre>
 * 
 * May receive the following messages from Chopper components:
 * <pre>
 * IMAGE:
 *       SET:SIZE:&lt;width&gt;:&lt;height&gt;
 *       AVAILABLESIZES
 *       GETPARAMS
 * </pre>
 * 
 * @author Benjamin Bardin
 */
public final class MakePicture implements Constants, Receivable {
	
	/** Desired compression rate of a high-quality jpeg image */
	public static final int HIGH_Q_JPEG = 85;
	
	/** Tag for logging */
	public static final String TAG = "chopper.MakePicture";
	
	/** The current width of preview frames */
	private final AtomicInteger mXprev = new AtomicInteger(0);
	
	/** The current height of preview frames */
	private final AtomicInteger mYprev = new AtomicInteger(0);
	
	/** Used to attempt to change frame size--holds desired new width of preview frames */
	private final AtomicInteger mNextX = new AtomicInteger(0);
	
	/** Used to attempt to change frame size--holds desired new height of preview frames */
	private final AtomicInteger mNextY = new AtomicInteger(0);
	
	/** Holds the value representing the format in which preview frames are stored
	 * @see android.graphics.ImageFormat */
	private final AtomicInteger mPrevFormat = new AtomicInteger(0);
	
	/** Flag indicating whether or not new frame has been captured.
	 * TransmitPicture thread sets this to false when it copies the stored frame. */
	private final AtomicBoolean mNewFrame = new AtomicBoolean(false);
	
	/** Internal array that stores a preview frame */
	private byte[] mStoreFrame = new byte[0];
	
	/** Holds the camera object */
	private Camera mCamera;
	
	/** Used to send messages to the thread */
	private Handler mHandler;
	
	/** Various callbacks */
	private Camera.PictureCallback mGoodPic;
	private Camera.ErrorCallback mCamError;
	private SurfaceHolder mPreviewHolder;
	private SurfaceHolder.Callback mSurfaceCallback;
	
	/** Hides Runnability, ensures singleton-ness */
	private Runnable mRunner;
	private static PersistentThread myThread;
	
	/** Registered receivers */
	private LinkedList<Receivable> mRec = new LinkedList<Receivable>();
	
	/**
	 * Constructs the thread, stores the surface for preview rendering.
	 * @param sh The SurfaceHolder to which the preview will be rendered
	 */
	public MakePicture(SurfaceHolder sh) {
		mPreviewHolder = sh;
	}
	
	/**
	 * Gets a copy of the last preview frame.
	 * @return The frame copy.
	 */
	public byte[] getBufferCopy() {
		synchronized (mStoreFrame) {
			return mStoreFrame.clone();
		}
	}
	
	/**
	 * Obtains the current preview frame size.
	 * @return An array containing the size.  Index '0' is the current width, index '1' is the current height.
	 */
	public int[] getFrameSize() {
		int[] myNums = {mXprev.get(), mYprev.get()};
		return myNums;
	}
	
	/**
	 * Obtains the thread that runs the MakePicture routines.
	 * On first call to this method, the PersistentThread is created.
	 * But since two or more instances of MakePicture should not be run concurrently,
	 * subsequent calls to this method return only that first thread.
	 * @return The PersistentThread that runs MakePicture.
	 */
	public PersistentThread getPersistentThreadInstance() {
		if (mRunner == null) {
			mRunner = new Runnable() {
				/**
				 * Starts the thread--specifically, initializes camera callbacks and starts capturing camera preview.
				 */
				public void run() {
					Thread.currentThread().setName("MakePicture");
					
					Looper.prepare();
					System.out.println("MakePicture run() thread ID " + Thread.currentThread().getId());
					
					
					
					//Handles incoming messages
					mHandler = new Handler() {
			            public void handleMessage(Message msg)
			            {
			                switch (msg.what) {
			                case TAKE_GOOD_PIC:
			                	mCamera.takePicture(null, null, mGoodPic); //takes the pic
			                	break;
			                case START_PREVIEW:
			                	if (mCamera != null)
			                		mCamera.startPreview();
			                	break;
			                case SEND_SIZES:
			                	sendSizes();
			                	break;
			                
			                }
			            }
			        };
			        initParams();
					initCallbacks();
					
					//Get to work!
			        mHandler.sendEmptyMessage(START_PREVIEW);
					Looper.loop();
				}
			};
		}
		if (myThread == null) {
			myThread = new PersistentThread(mRunner);
		}
		return myThread;
	}
	
	/**
	 * Obtains the image format used for preview frames.
	 * @return The format.
	 * @see android.graphics.ImageFormat ImageFormat
	 */
	public int getPreviewFormat() {
		return mPrevFormat.get();
	}
	
	
	/**
	 * Determines whether or not the most recent preview frame is a "new" one.
	 * @return The new-ness of the frame.
	 */
	public boolean isFrameNew() {
		return mNewFrame.get();
	}
	
	/**
	 * Processes a message
	 * @param msg The message to process
	 * @param source The source of the message, if a reply is expected.  May be null.
	 */
	public void receiveMessage(String msg, Receivable source) {
		Log.i(TAG, "Receiving msg: " + msg);
		String[] parts = msg.split(":");
		if (parts[0].equals("IMAGE")) {
			if (parts[1].equals("SET")) {
				if (parts[2].equals("SIZE")) {
					Log.v(TAG, "Set size to " + parts[3] + ", " + parts[4]);
					setNextFrameSize(new Integer(parts[3]), new Integer(parts[4]));
				}
			}
			else if (parts[1].equals("AVAILABLESIZES")) {
				sendSizes();
			}
			else if (parts[1].equals("GETPARAMS")) {
				int[] mySize = getFrameSize();
				if (source != null) {
					source.receiveMessage("IMAGE:FRAMESIZE:" + mySize[0] + ":" + mySize[1], this);// + ":" + transPic.getPreviewQuality());
				}
			}
		}
	}
	
	/**
	 * Registers a receivable to receive telemetry-related updates, especially a Comm object.
	 * @param rec The receivable to register.
	 * @see Comm Comm
	 */
	public void registerReceiver(Receivable rec) {
		synchronized (mRec) {
			mRec.add(rec);
		}
	}
	
	/**
	 * Sends a list of available preview sizes to all registered receivers.
	 */
	public void sendSizes() {
		if (mCamera == null)
			return;
		Camera.Parameters params = mCamera.getParameters();
		List<Camera.Size> sizes = params.getSupportedPreviewSizes();
		//Send list of available frame sizes to the serverSystem.out.println("Message: " + message);
		if (sizes != null) {
			ListIterator<Camera.Size> i1 = sizes.listIterator();
			while (i1.hasNext()) {
				Camera.Size mysize = i1.next();
				updateReceivers("IMAGE:AVAILABLESIZE:" + mysize.width + ":" + mysize.height);
			}
		}
		else {
			Camera.Size size = params.getPreviewSize();
			updateReceivers("IMAGE:AVAILABLESIZE:" + size.width + ":" + size.height);
		}		
	}
	
	/**
	 * Sets whether or not the most recent preview frame is a "new" one.
	 * @param newNess The new-ness of the frame.
	 */
	public void setFrameNewnessTo(boolean newNess) {
		mNewFrame.set(newNess);
	}
	
	/**
	 * Attempts to change the preview frame size to values previously registered.
	 * @return true if the operation succeeded; false otherwise.
	 * @see #setNextFrameSize(int, int) setNextFrameSize(int, int)
	 */
	public boolean updateFrameSize() {
		if (mCamera == null)
			return false;

		Camera.Parameters params = mCamera.getParameters();
		if (!(mNextX.get() != mXprev.get() | mNextY.get() != mYprev.get()))
			return false;
		try {
			Log.d(TAG, "updateFrameSize: " + mNextX.get() + ", " + mNextY.get());
			mCamera.stopPreview();
			params.setPreviewSize(mNextX.get(), mNextY.get());
			mCamera.setParameters(params);
			mXprev.set(mNextX.get());
			mYprev.set(mNextY.get());
			Log.v(TAG, "Init prev array: " + mXprev.get() + ", " + mYprev.get());
			synchronized (mStoreFrame) {
				mStoreFrame = new byte[mXprev.get() * mYprev.get() * ImageFormat.getBitsPerPixel(getPreviewFormat()) / 8];
				Log.e(TAG, "Array size: " + mStoreFrame.length);
			}
			initPrevCallback();
			mHandler.sendEmptyMessage(START_PREVIEW);
		}
		catch (Throwable t) {
			mNextX.set(mXprev.get());
			mNextY.set(mYprev.get());
			Log.i(TAG, "Preview size not changed.");
			mHandler.sendEmptyMessage(START_PREVIEW);
			return false;
		}
		return true;
	}
	
	/** Initializes the camera callbacks. */
	private void initCallbacks() {
		if (mCamera == null) {
			mCamera = Camera.open();
		}
		initErrorCallback();
		initGoodPicCallback();
		initSurfaceCallback();
		initPrevCallback();
	}
	
	/** Initializes, registers the error callback. */
	private void initErrorCallback() {
		
		//set up callbacks.  First is on error
		mCamError = new ErrorCallback()
		{
			public void onError(int error, Camera camera)
			{
				System.out.println("Camera error, code " + error);
			}
		};
		mCamera.setErrorCallback(mCamError);
	}
	
	/** Initializes the High-quality picture callback. */
	private void initGoodPicCallback() {
		//callback class, with instructions on what to do with a high-quality image
		mGoodPic = new Camera.PictureCallback()
		{
			public void onPictureTaken(byte[] imageData, Camera c)
			{
				if (imageData != null)
				{
					//TODO: store the picture
				}
				mHandler.sendEmptyMessage(START_PREVIEW);
			}
		};
	}
	
	/** Initializes the camera parameters. */
	private void initParams() {
		Log.i(TAG, "Initializing camera");
		if (mCamera == null) {
			mCamera = Camera.open();
		}
		try {
			//set the display
			mCamera.setPreviewDisplay(mPreviewHolder);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
		//get available parameters, set specific ones.  Later, will configure these to be operated remotely.
		Camera.Parameters params = mCamera.getParameters();
		
		mPrevFormat.set((params.getPreviewFormat()));
		
		List<Camera.Size> sizes = params.getSupportedPreviewSizes();
		//Send list of available frame sizes to the serverSystem.out.println("Message: " + message);
		if (sizes != null) {
			int mSize;
			if (sizes.size() > 1) { //at least 2 elements
				mSize = 1; //second lowest frame size, probably
			}
			else {
				mSize = 0; //only element
			}
			Camera.Size previewsize = sizes.get(mSize);
			params.setPreviewSize(previewsize.width, previewsize.height);
			mXprev.set(previewsize.width);
			mYprev.set(previewsize.height);
		}
		else { //Running off the emulator
			Camera.Size size = params.getPreviewSize();
			mXprev.set(size.width);
			mYprev.set(size.height);
		}
		mNextX.set(mXprev.get());
		mNextY.set(mYprev.get());
		
		//Deal with FPS
		List<Integer> fps = params.getSupportedPreviewFrameRates();
		if (fps != null) {
			params.setPreviewFrameRate(fps.get(0)); //lowest frame rate possible
		}
		else { //running off the emulator
			Log.d(TAG, "One available FPS: " + params.getPreviewFrameRate());
		}

		//Some arbitrary parameters:
		params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
		params.setPictureFormat(PixelFormat.JPEG);
		params.setJpegQuality(HIGH_Q_JPEG);
		params.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
		//Loads the new parameters.  Necessary!
		try {
			mCamera.setParameters(params);
		}
		catch (RuntimeException e) {
			e.printStackTrace();
			Log.w(TAG, "Camera parameters rejected.");
		}
	}
	
	/** Initializes the preview callback */
	private void initPrevCallback() {
		//what to do with each preview frame captured
		PreviewCallback precall = new PreviewCallback (){
			public void onPreviewFrame(byte[] data, Camera camera) {
				setFrameNewnessTo(true);
				synchronized (mStoreFrame) {
					camera.addCallbackBuffer(mStoreFrame);
				}
			}
		};
		mCamera.setPreviewCallbackWithBuffer(null);
		mCamera.setPreviewCallbackWithBuffer(precall);
		
		synchronized (mStoreFrame) {
			mStoreFrame = new byte[mXprev.get() * mYprev.get() * ImageFormat.getBitsPerPixel(getPreviewFormat()) / 8];
			mCamera.addCallbackBuffer(mStoreFrame);
		}
		//Inner class defs done
	}
	
	/** Initializes the surface callback. */
	private void initSurfaceCallback() {
		//handles drawing the preview to the surface.  Not algorithmically necessary, droid-required security feature.
		mSurfaceCallback = new SurfaceHolder.Callback() {
			public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
				System.out.println("Surface callback, surface changed");
			}

			public void surfaceCreated(SurfaceHolder holder) {
				System.out.println("Surface callback, surface created");
				System.out.println("surface callback, thread " + Thread.currentThread().getId());
				if (mCamera == null) {
					mCamera = Camera.open();
				}
				try {
					mCamera.setPreviewDisplay(mPreviewHolder);
				}
				catch (Throwable t) {
					t.printStackTrace();
				}
				mHandler.sendEmptyMessage(START_PREVIEW);
			}

			public void surfaceDestroyed(SurfaceHolder holder) {
				System.out.println("Surface callback, surface destroyed");
				mCamera.stopPreview();
			}
		};
		//add the callback
		mPreviewHolder.addCallback(mSurfaceCallback);
	}

	/**
	 * Registers a desired preview frame size, to which the camera will try to switch when updateFrameSize() is called.
	 * @param newx The desired new frame width. 
	 * @param newy The desired new frame height.
	 * @see #updateFrameSize() updateFrameSize()
	 */
	private void setNextFrameSize(int newx, int newy) {
		mNextX.set(newx);
		mNextY.set(newy);
	}
	
	/**
	 * Updates all registered receivers
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
}