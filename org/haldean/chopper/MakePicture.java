package org.haldean.chopper;

import java.io.IOException;
import java.util.List;

import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.ErrorCallback;
import android.hardware.Camera.PreviewCallback;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.SurfaceHolder;

public final class MakePicture extends Thread implements Constants
{
	private static Camera camera;
	public static byte[] buffer = new byte[0];
	
	//Variables used for calculating how long it takes to make a snapshot.
	
	private static Camera.PictureCallback GoodPic;
	private static Camera.ErrorCallback error;
	private static SurfaceHolder previewHolder;
	
	public static Handler mHandler;
	
	public MakePicture(SurfaceHolder sh)
	{
		super();
		setPriority(Thread.MIN_PRIORITY);
		previewHolder = sh;
		setName("MakePicture");
		
		//Initialize the camera, get a lock
		if (camera == null)
			camera = Camera.open();

		//set up callbacks.  First is on error, second is on picture taken.
		error = new ErrorCallback()
		{
			public void onError(int error, Camera camera)
			{
				System.out.println("Camera error, code " + error);
			}
		};
		
		//anonymous callback class, with instructions on what to do with the image data once available
		GoodPic = new Camera.PictureCallback()
		{
			public void onPictureTaken(byte[] imageData, Camera c)
			{
				if (imageData != null)
				{
					//TODO: store the picture
				}
				camera.startPreview();
			}
		};
		
		SurfaceHolder.Callback surfaceCallback=new SurfaceHolder.Callback() {
			public void surfaceCreated(SurfaceHolder holder) {
				if (camera == null)
					camera=Camera.open();

				try {
					camera.setPreviewDisplay(previewHolder);
				}
				catch (Throwable t) {
					t.printStackTrace();
				}
			}

			public void surfaceChanged(SurfaceHolder holder,
																 int format, int width,
																 int height) {
				Camera.Parameters parameters=camera.getParameters();

				parameters.setPreviewSize(width, height);
				parameters.setPictureFormat(PixelFormat.JPEG);

				camera.setParameters(parameters);
				camera.startPreview();
			}

			public void surfaceDestroyed(SurfaceHolder holder) {
			/*	camera.stopPreview();
				camera.release();
				camera=null;
				*/
			}
		};
		
		PreviewCallback precall = new PreviewCallback (){

			public void onPreviewFrame(byte[] data, Camera camera) {
				System.out.println("preview callback");
				synchronized (buffer) {
					buffer = data;
				}
			}
		};		
		//Inner class defs done
		
		previewHolder.addCallback(surfaceCallback);
		camera.setErrorCallback(error);
		try
		{
			camera.setPreviewDisplay(previewHolder);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		//get available parameters, set specific ones.  Later, may configure these to be operated remotely.
		Camera.Parameters params = camera.getParameters();
		List<Camera.Size> sizes = params.getSupportedPreviewSizes();
		
		
		//Send list of available frame sizes to the server
		Comm.sendMessage("IMAGE:NUMSIZES:" + sizes.size());
		for (int i = 0; i < sizes.size(); i++) {
			Comm.sendMessage("IMAGE:AVAILABLESIZE:" + sizes.get(i).width + ":" + sizes.get(i).height);
		}
		
		List<Integer> fps = params.getSupportedPreviewFrameRates();
		params.setPreviewFrameRate(fps.get(0)); //lowest frame rate possible
		
		Camera.Size previewsize = sizes.get(Math.min(1, sizes.size() - 1)); //second worst option, if available. if one option available, use it
		
		params.setPreviewSize(previewsize.width, previewsize.height);
		camera.setPreviewCallback(precall);
		
		params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
		
		params.setPictureSize(XPIC, YPIC);
		params.setPictureFormat(PixelFormat.JPEG);
		params.setJpegQuality(INITIALJPEGQ);
		
		//Loads the new parameters.  Necessary!
		camera.setParameters(params);
	}
	
	public void run()
	{
		Looper.prepare();
		
		camera.startPreview();
		System.out.println("MakePicture run() thread ID " + getId());
		
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
                }
            }
        };
		Looper.loop();
	}
}
