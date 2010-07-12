package org.haldean.chopper;

import java.io.IOException;
import java.util.List;
import java.util.ListIterator;

import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.ErrorCallback;
import android.hardware.Camera.PreviewCallback;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.SurfaceHolder;

public final class MakePicture extends Thread implements Constants {
	private static Camera camera;
	public static byte[] buffer = new byte[0];
	private static byte[] storeFrame;

	private static Camera.PictureCallback GoodPic;
	private static Camera.ErrorCallback error;
	private static volatile SurfaceHolder previewHolder;
	private static SurfaceHolder.Callback surfaceCallback;
	
	public static int XPREV = 0;
	public static int YPREV = 0;
	public static int nextx = 0;
	public static int nexty = 0;
	public static int PREVFORMAT = 0;
	
	public static Handler mHandler;
	
	public static boolean newFrame = false;
	
	public MakePicture(SurfaceHolder sh)
	{
		super("Take Telemetry");
		setPriority(Thread.MIN_PRIORITY);
		previewHolder = sh;		
	}
	
	public static void redrawPreviewHolder(SurfaceHolder sh) {
		//System.out.println("Redrawing");
		//add the callback
		System.out.println("thread redraw, " + Thread.currentThread().getId());
		previewHolder.removeCallback(surfaceCallback);
		previewHolder = sh;
		previewHolder.addCallback(surfaceCallback);
	}
	
	public void run()
	{
		Looper.prepare();
		System.out.println("MakePicture run() thread ID " + getId());
		
		//Handles incoming messages
		mHandler = new Handler() {
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
				mHandler.sendEmptyMessage(STARTPREVIEW);
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
				mHandler.sendEmptyMessage(STARTPREVIEW);
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

		try
		{
			//set the display
			camera.setPreviewDisplay(previewHolder);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		//what to do with each preview frame captured
		PreviewCallback precall = new PreviewCallback (){
			public void onPreviewFrame(byte[] data, Camera camera) {
				//System.out.println("PreviewFrame");
				synchronized (buffer) {
					buffer = data;
				}
				newFrame = true;
				
				camera.addCallbackBuffer(storeFrame);
			}
		};
		
		camera.setPreviewCallbackWithBuffer(precall);
		//Inner class defs done
		
		//get available parameters, set specific ones.  Later, will configure these to be operated remotely.
		Camera.Parameters params = camera.getParameters();
		
		PREVFORMAT = params.getPreviewFormat();
		
		List<Camera.Size> sizes = params.getSupportedPreviewSizes();
		//Send list of available frame sizes to the serverSystem.out.println("Message: " + message);
		if (sizes != null) {
			Camera.Size previewsize = sizes.get(Math.min(1, sizes.size() - 1)); //second worst option, if available. if one option available, use it
			params.setPreviewSize(previewsize.width, previewsize.height);
			XPREV = previewsize.width;
			YPREV = previewsize.height;
			
		}
		else { //Running off the emulator
			Camera.Size size = params.getPreviewSize();
			XPREV = size.width;
			YPREV = size.height;
		}
		nextx = XPREV;
		nexty = YPREV;
		storeFrame = new byte[XPREV * YPREV * ImageFormat.getBitsPerPixel(PREVFORMAT) / 8];
		camera.addCallbackBuffer(storeFrame);
		
		//Deal with FPS
		List<Integer> fps = params.getSupportedPreviewFrameRates();
		if (fps != null)
			params.setPreviewFrameRate(fps.get(0)); //lowest frame rate possible
		else //running off the emulator
			System.out.println("One available FPS: " + params.getPreviewFrameRate());
		
		
		
		//Some arbitrary parameters:
		params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
		params.setPictureSize(XPIC, YPIC);
		params.setPictureFormat(PixelFormat.JPEG);
		params.setJpegQuality(INITIALJPEGQ);
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
        mHandler.sendEmptyMessage(STARTPREVIEW);
		Looper.loop();
	}
	
	public static boolean updateFrameSize() {
		if (camera == null)
			return false;
		System.out.println("Updating frame size, from " + XPREV + ", " + YPREV + " to " + nextx + ", " + nexty);
		Camera.Parameters params = camera.getParameters();
		try {
			params.setPreviewSize(nextx, nexty);
			camera.stopPreview();
			camera.setParameters(params);
			XPREV = nextx;
			YPREV = nexty;
			
		}
		catch (Throwable t) {
			t.printStackTrace();
			return false;
		}
		finally { //if it succeeds, nothing happens.  if it doesn't, nextx/nexty are reset.
			nextx = XPREV;
			nexty = YPREV;
			storeFrame = new byte[XPREV * YPREV * ImageFormat.getBitsPerPixel(PREVFORMAT) / 8];
			mHandler.sendEmptyMessage(STARTPREVIEW);
		}
		return true;
	}
	
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