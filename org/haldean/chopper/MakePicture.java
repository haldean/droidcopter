package org.haldean.chopper;

import java.io.IOException;
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
 * Interfaces with the camera to obtain preview frames and to take high-quality pictures.
 * @author Benjamin Bardin
 *
 */
public final class MakePicture extends Thread implements Constants {
	
	/**
	 * Stores preview frames for later access by other threads.  All read/writes should be externally synchronized.
	 */
	public static byte[] buffer = new byte[0];
	
	/**
	 * Used to send messages to the thread
	 */
	public static Handler handler;
	
	/* The current width of preview frames */
	private static AtomicInteger XPREV = new AtomicInteger(0);
	
	/* The current height of preview frames */
	private static AtomicInteger YPREV = new AtomicInteger(0);
	
	/* Used to attempt to change frame size--represents desired new width of preview frames */
	private static AtomicInteger nextx = new AtomicInteger(0);
	
	/* Used to attempt to change frame size--represents desired new height of preview frames */
	private static AtomicInteger nexty = new AtomicInteger(0);
	
	/* Holds the value representing the format in which preview frames are stored
	 * @see android.graphics.ImageFormat */
	private static AtomicInteger PREVFORMAT = new AtomicInteger(0);
	
	/* Flag indicating whether or not new frame has been captured.
	 * TransmitPicture thread sets this to false when it copies the stored frame. */
	private static AtomicBoolean newFrame = new AtomicBoolean(false);
	
	/* Internal array that stores a preview frame */
	private static byte[] storeFrame = new byte[0];
	
	/* Holds the camera object */
	private static Camera camera;
	
	/* Various callbacks */
	private static Camera.PictureCallback GoodPic;
	private static Camera.ErrorCallback error;
	private static volatile SurfaceHolder previewHolder;
	private static SurfaceHolder.Callback surfaceCallback;
	
	/* Desired compression rate of a high-quality jpeg image */
	private static final int HIGHQJPEG = 85;
	
	/* Tag for logging */
	private static final String TAG = "chopper.MakePicture";
	
	/**
	 * Constructs the thread, stores the surface for preview rendering.
	 * @param sh The SurfaceHolder to which the preview will be rendered
	 */
	public MakePicture(SurfaceHolder sh) {
		super("MakePicture");
		setPriority(Thread.MIN_PRIORITY);
		previewHolder = sh;		
	}
	
	/**
	 * Starts the thread--specifically, initializes camera callbacks and starts capturing camera preview.
	 */
	public void run() {
		Looper.prepare();
		System.out.println("MakePicture run() thread ID " + getId());
		
		setupCallbacks();
		//Handles incoming messages
		handler = new Handler() {
            public void handleMessage(Message msg)
            {
                switch (msg.what) {
                case TAKEGOODPIC:
                	camera.takePicture(null, null, GoodPic); //takes the pic
                	break;
                case STARTPREVIEW:
                	if (camera != null)
                		camera.startPreview();
                	break;
                case SENDSIZES:
                	sendSizes();
                	break;
                }
            }
        };
        
		try {
			//set the display
			camera.setPreviewDisplay(previewHolder);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
		//get available parameters, set specific ones.  Later, will configure these to be operated remotely.
		Camera.Parameters params = camera.getParameters();
		
		PREVFORMAT.set((params.getPreviewFormat()));
		
		List<Camera.Size> sizes = params.getSupportedPreviewSizes();
		//Send list of available frame sizes to the serverSystem.out.println("Message: " + message);
		if (sizes != null) {
			Camera.Size previewsize = sizes.get(0); //start with lowest resolution.
			params.setPreviewSize(previewsize.width, previewsize.height);
			XPREV.set(previewsize.width);
			YPREV.set(previewsize.height);
		}
		else { //Running off the emulator
			Camera.Size size = params.getPreviewSize();
			XPREV.set(size.width);
			YPREV.set(size.height);
		}
		nextx.set(XPREV.get());
		nexty.set(YPREV.get());
		synchronized (storeFrame) {
			storeFrame = new byte[XPREV.get() * YPREV.get() * ImageFormat.getBitsPerPixel(getPreviewFormat()) / 8];
			camera.addCallbackBuffer(storeFrame);
		}
		
		//Deal with FPS
		List<Integer> fps = params.getSupportedPreviewFrameRates();
		if (fps != null)
			params.setPreviewFrameRate(fps.get(0)); //lowest frame rate possible
		else //running off the emulator
			System.out.println("One available FPS: " + params.getPreviewFrameRate());

		//Some arbitrary parameters:
		params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
		params.setPictureFormat(PixelFormat.JPEG);
		params.setJpegQuality(HIGHQJPEG);
		params.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
		//Loads the new parameters.  Necessary!
		try {
			camera.setParameters(params);
		}
		catch (RuntimeException e) {
			e.printStackTrace();
			System.out.println("Parameters won't set.  Not a significant problem; moving on.");
		}
		sendSizes();
        
        //Get to work!
        handler.sendEmptyMessage(STARTPREVIEW);
		Looper.loop();
	}
	
	
	
	/**
	 * Initializes the camera object and associated callbacks.
	 */
	private static void setupCallbacks() {
		//Initialize the camera, get a lock
		if (camera == null)
			camera = Camera.open();

		//set up callbacks.  First is on error
		error = new ErrorCallback()
		{
			public void onError(int error, Camera camera)
			{
				System.out.println("Camera error, code " + error);
			}
		};
		
		camera.setErrorCallback(error);
		
		//callback class, with instructions on what to do with a high-quality image
		GoodPic = new Camera.PictureCallback()
		{
			public void onPictureTaken(byte[] imageData, Camera c)
			{
				if (imageData != null)
				{
					//TODO: store the picture
				}
				handler.sendEmptyMessage(STARTPREVIEW);
			}
		};
		
		//handles drawing the preview to the surface.  Not algorithmically necessary, droid-required security feature.
		surfaceCallback = new SurfaceHolder.Callback() {
			public void surfaceCreated(SurfaceHolder holder) {
				System.out.println("Surface callback, surface created");
				System.out.println("surface callback, thread " + Thread.currentThread().getId());
				if (camera == null)
					camera=Camera.open();

				try {
					camera.setPreviewDisplay(previewHolder);
				}
				catch (Throwable t) {
					t.printStackTrace();
				}
				handler.sendEmptyMessage(STARTPREVIEW);
			}

			public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
				System.out.println("Surface callback, surface changed");
			}

			public void surfaceDestroyed(SurfaceHolder holder) {
				System.out.println("Surface callback, surface destroyed");
				camera.stopPreview();
				/*camera.release();
				camera=null;*/
			}
		};
		//add the callback
		previewHolder.addCallback(surfaceCallback);
		
		//what to do with each preview frame captured
		PreviewCallback precall = new PreviewCallback (){
			public void onPreviewFrame(byte[] data, Camera camera) {
				synchronized (data) {
					synchronized (buffer) {
						buffer = data;
					}
				}
				setNewFrameTo(true);
				synchronized (storeFrame) {
					camera.addCallbackBuffer(storeFrame);
				}
			}
		};
		
		camera.setPreviewCallbackWithBuffer(precall);
		//Inner class defs done
		
	}
	
	/**
	 * Determines whether or not the most recent preview frame is a "new" one.
	 * @return The new-ness of the frame.
	 */
	public static boolean isFrameNew() {
		return newFrame.get();
	}
	
	/**
	 * Sets whether or not the most recent preview frame is a "new" one.
	 * @param newNess The new-ness of the frame.
	 */
	public static void setNewFrameTo(boolean newNess) {
		newFrame.set(newNess);
	}
	
	/**
	 * Registers a desired preview frame size, to which the camera will try to switch when updateFrameSize() is called.
	 * @param newx The desired new frame width. 
	 * @param newy The desired new frame height.
	 * @see #updateFrameSize() updateFrameSize()
	 */
	public static void tryNextFrameSize(int newx, int newy) {
		nextx.set(newx);
		nexty.set(newy);
	}
	
	/**
	 * Obtains the image format used for preview frames.
	 * @return The format.
	 * @see android.graphics.ImageFormat ImageFormat
	 */
	public static int getPreviewFormat() {
		return PREVFORMAT.get();
	}
	
	/**
	 * Obtains the current preview frame size.
	 * @return An array containing the size.  Index '0' is the current width, index '1' is the current height.
	 */
	public static int[] getFrameSize() {
		int[] myNums = {XPREV.get(), YPREV.get()};
		return myNums;
	}

	/**
	 * Sets a new SurfaceHolder to which preview frames should be rendered.
	 * @param sh The new SurfaceHolder
	 */
	public static void redrawPreviewHolder(SurfaceHolder sh) {
		Log.v(TAG, "Redrawing PrScreeneview Holder");
		
		//add the callback
		System.out.println("thread redraw, " + Thread.currentThread().getId());
		previewHolder.removeCallback(surfaceCallback);
		previewHolder = sh;
		previewHolder.addCallback(surfaceCallback);
	}
	
	/**
	 * Attempts to change the preview frame size to values nextx and nexty.
	 * @return true if the operation succeeded; false otherwise.
	 * @see #nextx nextx
	 * @see #nexty nexty
	 */
	public static boolean updateFrameSize() {
		if (camera == null)
			return false;
		/*synchronized (frameSizeLock) {
			synchronized (nextFrameSizeLock) {
				System.out.println("Updating frame size, from " + XPREV + ", " + YPREV + " to " + nextx + ", " + nexty);
			}
		}
		*/
		Camera.Parameters params = camera.getParameters();
		if (!(nextx.get() != XPREV.get() | nexty.get() != YPREV.get()))
			return false;
		try {
			params.setPreviewSize(nextx.get(), nexty.get());
			camera.stopPreview();
			camera.setParameters(params);
			XPREV.set(nextx.get());
			YPREV.set(nexty.get());
			synchronized (storeFrame) {
				storeFrame = new byte[XPREV.get() * YPREV.get() * ImageFormat.getBitsPerPixel(getPreviewFormat()) / 8];
			}
			handler.sendEmptyMessage(STARTPREVIEW);
		}
		catch (Throwable t) {
			nextx.set(XPREV.get());
			nexty.set(YPREV.get());
			Log.i(TAG, "Preview size not changed.");
			handler.sendEmptyMessage(STARTPREVIEW);
			return false;
		}
		return true;
	}
	
	/**
	 * Sends a list of available preview sizes to the control server.
	 */
	public static void sendSizes() {
		if (camera == null)
			return;
		Camera.Parameters params = camera.getParameters();
		List<Camera.Size> sizes = params.getSupportedPreviewSizes();
		//Send list of available frame sizes to the serverSystem.out.println("Message: " + message);
		if (sizes != null) {
			ListIterator<Camera.Size> i1 = sizes.listIterator();
			while (i1.hasNext()) {
				Camera.Size mysize = i1.next();
				Comm.sendMessage("IMAGE:AVAILABLESIZE:" + mysize.width + ":" + mysize.height);
			}
		}
		else {
			Camera.Size size = params.getPreviewSize();
			Comm.sendMessage("IMAGE:AVAILABLESIZE:" + size.width + ":" + size.height);
		}		
	}
}