package org.haldean.chopper.pilot;

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
	/** Tag for logging */
	public static final String TAG = "chopper.ChopperMain";
	
	/**
	 * Holds the wakelock; needed to keep the camera preview rendering on
	 * certain phones/android OS versions
	 */
	protected PowerManager.WakeLock mWakeLock;
	
	/**
	 * Constructs the Chopper activity.
	 */
	public ChopperMain() {
		super();
	}
	
	/**
	 * Initializes program by creating, linking and starting chopper components.
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
        
        Comm comm = new Comm(true);
        ChopperStatus status = new ChopperStatus(getApplicationContext());
        StatusReporter reporter = new StatusReporter(status);
        MakePicture pic = new MakePicture(previewHolder);
        
        Navigation nav = new Navigation(status);
        Guidance guid = new Guidance(status, nav);
        
        comm.setTelemetrySource(pic);
        comm.registerReceiver(IMAGE, pic);
        comm.registerReceiver(NAV, nav);
        comm.registerReceiver(GUID, guid);
        
        nav.registerReceiver(comm);
        
        status.registerReceiver(nav);
        
        reporter.registerReceiver(comm);
        pic.registerReceiver(comm);
        guid.registerReceiver(comm);
        
        new PersistentThread(comm).start();
        status.getPersistentThreadInstance().start();
        new PersistentThread(reporter).start();
        pic.getPersistentThreadInstance().start();
    	nav.getPersistentThreadInstance().start();
    	guid.getPersistentThreadInstance().start();
	}
	
	/**
	 * Releases the wakelock, destroys activity.
	 */
	protected void onDestroy() {
		mWakeLock.release();
		super.onDestroy();
	} 
}
