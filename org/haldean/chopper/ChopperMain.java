package org.haldean.chopper;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public final class ChopperMain extends Activity implements Constants
{
	protected PowerManager.WakeLock mWakeLock; 
	private static boolean firstRun = true;
	private static MakePicture takepic;
	
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState); //call to parent class
		
		setContentView(R.layout.main);

		Thread.currentThread().setName("ChopperMain");
		System.out.println("ChopperMain onCreate() thread ID " + Thread.currentThread().getId());
		
		// Lock awakes
		final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        this.mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
        this.mWakeLock.acquire();
        
		
        //Camera stuff
        SurfaceView preview = (SurfaceView) findViewById(R.id.preview);
        SurfaceHolder previewHolder = preview.getHolder();
        previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        
        if (firstRun) {
	        
	      //Initialize and start sensor process
			ChopperStatus status = new ChopperStatus(getApplicationContext());
			status.start();
			
			takepic = new MakePicture(previewHolder);
	        takepic.start();
	        
	        //Initialize and start the processes that send data back to the control computer.
			Comm send = new Comm(previewHolder);
			send.start();
			
			Navigation nav = new Navigation();
			nav.start();
			
			Guidance guid = new Guidance();
			guid.start();
        }
        else {
        	MakePicture.redrawPreviewHolder(previewHolder);
        }
        firstRun = false;
	}

	protected void onDestroy() {
		this.mWakeLock.release();
		super.onDestroy();
	} 
	
	/*protected void onResume() {
		super.onResume();
		if (MakePicture.mHandler != null)
			MakePicture.mHandler.sendEmptyMessage(STARTPREVIEW);
	}*/
}
