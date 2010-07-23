package org.haldean.chopper;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
/**
 * Class to launch all component subroutines
 * @author Benjamin Bardin
 */
public final class ChopperMain extends Activity implements Constants
{
	/**
	 * Holds the wakelock; needed to keep the camera preview rendering on
	 * certain phones/android OS versions
	 */
	protected PowerManager.WakeLock mWakeLock;
	
	/**
	 * The Activity is destroyed and restarted whenever the phone rotates--
	 * the threads it starts, however, persist and need only be started on the first run. 
	 */
	private static boolean firstRun = true;
	
	/**
	 * Initializes program.
	 * On first call, starts subroutines.  On subsequent calls, only redraws camera preview display.
	 * @param savedInstanceState Loads state data.  Not used in this program.
	 */
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState); //call to parent class
		Thread.currentThread().setName("ChopperMain");
		System.out.println("ChopperMain onCreate() thread ID " + Thread.currentThread().getId());
		
		/* Acquires wakelock */
		final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
        mWakeLock.acquire();
        
		
        /* Camera stuff */
        setContentView(R.layout.main);
        SurfaceView preview = (SurfaceView) findViewById(R.id.preview);
        SurfaceHolder previewHolder = preview.getHolder();
        previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        
        if (firstRun) {
	        
        	/* Initialize and start sensor process */
			new ChopperStatus(getApplicationContext()).start();
			
			//new MakePicture(previewHolder).start();
	        
	        /* Initialize and start the processes that send data back to the control computer. */
			new Comm().start();
			
			new Navigation().start();
			
			//new Guidance().start();
        }
        else {
        	MakePicture.redrawPreviewHolder(previewHolder);
        }
        firstRun = false;
	}
	
	/**
	 * Releases the wakelock, destroys activity.
	 */
	protected void onDestroy() {
		mWakeLock.release();
		super.onDestroy();
	} 
}
