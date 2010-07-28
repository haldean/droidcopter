package org.haldean.chopper;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

/**
 * Determines motor speeds, based on chopper's status and desired velocity vector.
 * @author Benjamin Bardin
 */
public class Guidance implements Runnable, Constants {
	
	/* How many times per second the PID loop should run */
	private static int PIDREPS = 5;
	
	/* Maximum permissible target velocity; larger vectors will be resized */
	private static final double MAXVEL = 2.0;
	
	/* The maximum angle guidance will permit the chopper to have */
	private static final double MAXANGLE = 20;
	
	/* The maximum change in motor speed permitted at one time.  Must be positive. */
	private static final double MAXD = .1F;
	
	/* Used when a really big number is needed, still small enough to prevent overflow. */
	private static final double REALLYBIG = 10000;
	
	/* Handles messages for the thread */
	private static Handler handler;
	
	/* Stores orientation data locally */
	private static double azimuth;
	private static double pitchdeg;
	private static double rolldeg;
	private static double pitchrad;
	private static double rollrad;
	private static double gpsBearing;
	private static double gpsSpeed;
	private static double gpsDalt;
	
	/* Stores desired velocity */
	private static double[] target = new double[4];
	
	/* Stores the current velocity, relative to the chopper */
	private static double[] current = new double[4];
	
	/* Stores current PID error */
	private static double[][] errors = new double[4][3];
	
	/* Manages integral error */
	private static int integralindex = 0;
	private static double[][] integralerrors = new double[4][PIDREPS];
	
	/* Timestamp of last PID evaluation */
	private static long lastupdate = 0;
	
	/* Sum of errors * tuning parameter for a given PID loop */
	private static double[] torques = new double[4];
	
	/* Stores motor speeds temporarily */
	private static double[] tempmotorspeed = new double[4];
	
	/* Set to true if the chopper's angle is too great; set to false otherwise */
	private static boolean stabilizing;
	
	/* The chopper's angle of ascent, in degrees */
	private static double ascentdeg;
	
	/* Tuning parameters */
	private static double[][] gain = new double[4][3];
	
	/* Motor speed */
	private static double[] motorspeed = new double[4]; //ORDER: North, South, East, West
	
	/* If set to true, disregards lateral velocity commands */
	private static boolean horizontaldrift = false; //if true, does not consider dx, dy or azimuth error; makes for maximally efficient altitude control
	
	/* If true, prints debug messages */
	private static boolean inSimulator = false;
	
	/* Tag for logging */
	private static final String TAG = new String("chopper.Guidance");
	
	/**
	 * Constructs the thread, assigns maximum priority
	 */
	public Guidance() {
		//Temporary: need real tuning values at some point:
		for (int i = 0; i < 4; i++)
			for (int j = 0; j < 3; j++)
				gain[i][j] = .05;
	}
	
	/**
	 * Starts the guidance thread
	 */
	public void run() {
		Looper.prepare();
		Thread.currentThread().setName("Guidance");
		Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
		handler = new Handler() {
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case EVALMOTORSPEED:
					reviseMotorSpeed();
					break;
				}
			}
		};
		handler.sendEmptyMessage(EVALMOTORSPEED);
		Looper.loop();
	}
	
	/* Core of the class; calculates new motor speeds based on status */
	private static void reviseMotorSpeed() {
		if (inSimulator)
			System.out.println();
		long starttime = System.currentTimeMillis();
		stabilizing = false; //initializing value
		//Retrieve current orientation.

		azimuth = ChopperStatus.getReadingFieldNow(AZIMUTH, azimuth);
		pitchdeg = -ChopperStatus.getReadingFieldNow(PITCH, -pitchdeg);
		rolldeg = ChopperStatus.getReadingFieldNow(ROLL, rolldeg);
		
		pitchrad = pitchdeg * Math.PI / 180.0;
		rollrad = rolldeg * Math.PI / 180.0;
		
		double gradient = Math.sqrt(
				Math.pow(Math.tan(rollrad), 2) +
				Math.pow(Math.tan(pitchrad), 2)
				);
		double ascentrad = Math.atan(gradient);
		ascentdeg = ascentrad * 180.0 / Math.PI;
		//if orientation is out-of-bounds,
		if ((ascentdeg > MAXANGLE) | (pitchdeg > 90.0) | (pitchdeg < -90.0)) {
			stabilizing = true;
			//set target velocity to some big number in the direction of maximum ascent
			double gradangle = Math.atan2(
						Math.tan(rollrad) ,
						Math.tan(pitchrad)
						);
			target[0] = REALLYBIG * Math.sin(gradangle);
			target[1] = REALLYBIG * Math.cos(gradangle);
			
			//Make sure the velocity vector components point in the right directions.
			target[0] *= Math.signum(target[0]) * Math.signum(rolldeg);
			target[1] *= Math.signum(target[1]) * Math.signum(pitchdeg);
			target[2] = 0;
			target[3] = azimuth;
			//System.out.println(target[0] + ", " + target[1]);
		}
		else {
			//Retrieve target velocity from nav,
			//Transform absolute target velocity to relative target velocity
			double theta = -azimuth * Math.PI / 180.0;
			
			try {
				double[] absTarget = Navigation.getTarget();
				target[0] = absTarget[0] * Math.sin(theta) + absTarget[1] * Math.cos(theta);
				target[1] = absTarget[0] * Math.cos(theta) - absTarget[1] * Math.sin(theta);
				target[2] = absTarget[2];
				target[3] = absTarget[3];
				
				//Calculate recorded velocity; reduce, if necessary, to MAXVEL
				double myVel = 0;
				for (int i = 0; i < 3; i++) {
					myVel += Math.pow(target[i], 2);
				}
				myVel = Math.sqrt(myVel);
				if (myVel > MAXVEL) {
					if (inSimulator)
						System.out.println("Reducing");
					double adjustment = MAXVEL / myVel;
					for (int i = 0; i < 3; i++) {
						target[i] *= adjustment;
					}
				}
			}
			catch (IllegalAccessException e) {
				Log.w(TAG, "Nav Target lock not available.");
			}
			Log.v(TAG, "Relative target: " + target[0] + ", " + target[1]);
		}
		
		
		
		long thistime = System.currentTimeMillis();
		
		//Retrieve current absolute velocity.  For now, only from GPS data; later, maybe write a kalman filter to use accelerometer data as well. 
		//Transform current velocity from absolute to relative
		
		//CHECK SIGN HERE:
		gpsBearing = ChopperStatus.getGpsFieldNow(BEARING, gpsBearing); 
		double theta = (gpsBearing - azimuth) * Math.PI / 180.0;
		
		gpsSpeed = ChopperStatus.getGpsFieldNow(SPEED, gpsSpeed);
		current[0] = gpsSpeed * Math.cos(theta);
		current[1] = gpsSpeed * Math.sin(theta);
		
		gpsDalt = ChopperStatus.getGpsFieldNow(dALT, gpsDalt);
		current[2] = gpsDalt;
		current[3] = azimuth;
		
		
		for (int i = 0; i < 4; i++) {
			//Calculate proportional errors
			double err = target[i] - current[i];
			if (i == 3) { //For azimuth, multiple possibilities exist for error, each equally valid; but only the error nearest zero makes practical sense.
				if (err > 180.0)
					err -= 360.0;
				if (err < -180.0)
					err += 360.0;
			}

			//Calculate derivative errors.
			errors[i][2] = (err - errors[i][0]) * 1000.0 / (thistime - lastupdate);
			
			
			//Mark proportional error
			errors[i][0] = err;
			
			//Update integral errors
			errors[i][1] -= integralerrors[i][integralindex];
			integralerrors[i][integralindex] = err;
			errors[i][1] += err;
			integralindex = ++integralindex % PIDREPS;
			
			double dmotor = 0;
			
			//Calculate changes in output
			for (int j = 0; j < 3; j++) {
				dmotor += errors[i][j] * gain[i][j];
				
			}
			double phi = 0;
			switch (i) {
			case 0: //X velocity
				phi = Math.sin(rollrad);
				phi = Math.abs(phi);
				if (phi == 0)
					dmotor = 2 * dmotor; 
				else
					dmotor = dmotor / phi;
				break;
			case 1: //Y velocity
				phi = Math.sin(pitchrad);
				phi = Math.abs(phi);
				if (phi == 0)
					dmotor = 2 * dmotor;
				else
					dmotor = dmotor / phi;
				break;
			case 2: //Z velocity
				phi = Math.cos(ascentrad);
				phi = Math.abs(phi);
				if (phi == 0)
					dmotor = 0; //Don't bother with altitude control, gives more efficiency to torque[0, 1] for stabilization
				else
					dmotor = dmotor / phi;
				break;
			case 3: //Azimuth
				break;
			}				
			torques[i] = dmotor;
			if (inSimulator)
				System.out.println("phi: " + phi);
			if (inSimulator)
				System.out.println("dmotor: " +dmotor);	
		}
		lastupdate = thistime;
		
		if ((!horizontaldrift) || (stabilizing)) { //if horizontal drift is on, motor speeds give full efficiency to altitude control
		//but if the chopper is stabilizing, under no circumstances ignore torques 0, 1
			//changes torques to motor values
			tempmotorspeed[0] -= torques[1] / 2F;
			tempmotorspeed[1] += torques[1] / 2F;
			
			tempmotorspeed[2] -= torques[0] / 2F;
			tempmotorspeed[3] += torques[0] / 2F;
			
			
			
			double spintorque = torques[3] / 4F;
			tempmotorspeed[0] += spintorque;
			tempmotorspeed[1] += spintorque;
			tempmotorspeed[2] -= spintorque;
			tempmotorspeed[3] -= spintorque;
		}
		
		double dalttorque = torques[2] / 4F;
		for (int i = 0; i < 4; i++) {
			tempmotorspeed[i] += dalttorque;
		}
		
		//Sanity Check--values must be between zero and one.
		for (int i = 0; i < 4; i++) {
			if (tempmotorspeed[i] < 0)
				tempmotorspeed[i] = 0;
			else if (tempmotorspeed[i] > 1)
				tempmotorspeed[i] = 1;
			double diff = tempmotorspeed[i] - motorspeed[i];
			if (diff > 0)
				motorspeed[i] += Math.min(diff, MAXD);
			else if (diff < 0)
				motorspeed[i] += Math.max(diff, -MAXD);
			tempmotorspeed[i] = motorspeed[i];
			
		}
	
		//Pass filtered values to motors.
		ChopperStatus.setMotorFields(motorspeed);
		Log.v(TAG, "motors: " + motorspeed[0] + ", " + motorspeed[1] + ", " + motorspeed[2] + ", " + motorspeed[3]);
		//Sleep a while
		long timetonext = (1000 / PIDREPS) - (System.currentTimeMillis() - starttime);
		if (timetonext > 0)
			handler.sendEmptyMessageDelayed(EVALMOTORSPEED, timetonext);
		else
			handler.sendEmptyMessage(EVALMOTORSPEED);
	}
}

