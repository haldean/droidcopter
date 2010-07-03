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
        if (preview == null)
        {
        	System.out.println("Null, damn!");
        	System.exit(-1);
        }
        SurfaceHolder previewHolder = preview.getHolder();
        previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        
		//Initialize and start the processes that send data back to the control computer.
		Comm send = new Comm(previewHolder);
		send.start();
		
		//Initialize and start sensor process
		ChopperStatus status = new ChopperStatus(getApplicationContext());
		status.start();
	
	}

	protected void onDestroy() {
		this.mWakeLock.release();
		super.onDestroy();
	} 
	
	protected void onResume() {
		super.onResume();
		MakePicture.mHandler.sendEmptyMessage(STARTPREVIEW);
	}
}
