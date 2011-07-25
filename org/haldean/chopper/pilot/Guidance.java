package org.haldean.chopper.pilot;

import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.concurrent.atomic.AtomicInteger;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

/**
 * Determines motor speeds, based on chopper's status and desired velocity vector.
 * 
 * May send the following messages to registered Receivables:<br>
 * <pre>
 * GUID:ERROR:&lt;loop_1_error&gt;:&lt;loop_2_error&gt;:&lt;loop_3_error&gt;:&lt;loop_4_error&gt;
 * GUID:PID:VALUE:&lt;pid_loop_number&gt;:&lt;pid_parameter_index&gt;:&lt;pid_parameter_value&gt;
 * </pre>
 * 
 * May receive the following messages from Chopper components:
 * <pre>
 * GUID:
 *      PID:
 *          SET:&lt;pid_loop_number&gt;:&lt;pid_parameter_index&gt;:&lt;pid_parameter_value&gt;
 *          GET
 *      AUTOPILOT
 *      VECTOR:&lt;north_motor_speed&gt;:&lt;south_motor_speed&gt;:&lt;east_motor_speed&gt;:&lt;west_motor_speed&gt;
 * 		LOCALVEC
 * 		ABSVEC
 * 
 * </pre>
 * 
 * @author Benjamin Bardin
 */
public class Guidance implements Runnable, Constants, Receivable {
	
	/** How many times per second the PID loop will run */
	public static final int PIDREPS = 40;
	
	/** The maximum change in motor speed permitted at one time.  Must be positive. */
	public static final double MAX_DMOTOR = .05;
	
	/** The maximum change in motor speed permitted at one time if the chopper is stabilizing.  Must be positive. */
	public static final double MAX_DSTABLE = .1;
	
	/** Tag for logging */
	public static final String TAG = new String("chopper.Guidance");
	
	/** Handles messages for the thread */
	private Handler mHandler;
	
	private Angler mAngler;
	
	/** Stores orientation data persistently, as expected values in case lock is not immediately available*/
	private double mAzimuth;
	private double mPitchDeg;
	private double mRollDeg;
	
	/** Log file name **/
	public static final String logname = "/sdcard/chopper/guidlog.txt";
	
	/** Log file writer **/
	private FileWriter logfile;
	
	/** Note that some of the following objects are declared outside their smallest scope.
	 * This is to relieve unnecessary stress on the GC.  Many of these data holders
	 * can easily be reused from iteration to iteration, and since the PID loops
	 * may run as much as 20+ times a second this is considerably more efficient,
	 * though somewhat less readable.  As a compromise, primitives are declared/
	 * initialized each time in their scope, and reusable objects (especially arrays)
	 * remain persistent from iteration to iteration. 
	 */
	
	/** Stores current PID error */
	private double[][] mErrors = new double[4][3];
	
	/** Manages integral error */
	private int mIntegralIndex = 0;
	private double[][] mIntegralErrors = new double[4][PIDREPS];
	
	/** Timestamp of last PID evaluation */
	private long mLastUpdate = 0;
	
	private double[] mControlVars = new double[4];
	private double[] mAngleTarget = new double[4];
	private long mLastGpsTimestamp;
	private AtomicInteger mGuidanceMode = new AtomicInteger();
	
	private static final int DIRECT = 0;
	private static final int MANUAL = 1;
	private static final int AUTOPILOT = 2;
	
	/** Tuning parameters */
	private double[][] mGain = new double[4][3];
	
	/** Motor speed */
	private double[] mMotorSpeed = new double[4]; //ORDER: North, South, East, West
	
	/** List of registered receivers */
	private LinkedList<Receivable> mRec;
	
	/** Handles to other chopper components */
	private ChopperStatus mStatus;
	private BluetoothOutput mBt;
	
	/** Flag for writing motor speeds to output file **/
	public final static boolean mEnableLogging = true;
	
	/**
	 * Constructs a Guidance object
	 * @param status The source status information.
	 * @param nav The source of navigation target information.
	 */
	public Guidance(ChopperStatus status, Navigation nav, BluetoothOutput bT) {
		if (status == null | nav == null) {
			throw new NullPointerException();
		}
		mStatus = status;
		mRec = new LinkedList<Receivable>();
		mBt = bT;
		mAngler = new Angler(status, nav);
		
		//Temporary: need real tuning values at some point. Crap.
		for (int i = 0; i < 3; i++)
			mGain[i][0] = .1;
		mGain[3][0] = 0;
				
		try {
			if (mEnableLogging)
				logfile = new FileWriter(logname, false);
		}
		catch (IOException e) {
			e.printStackTrace();
			Log.e(TAG, "Cannot open log file.");
		}
	}
	
	/**
	 * Obtains the current P error values, concatenates into a string
	 * @return A string representing the error values.
	 */
	private String getErrorString() {
		return "GUID:ERROR:" + mErrors[0][0]
		               + ":" + mErrors[1][0]
		               + ":" + mErrors[2][0]
		               + ":" + mErrors[3][0];
	}
	
	/**
	 * Closes the log file.
	 */
	public void onDestroy() {
		try {
			if (logfile != null)
				logfile.close();
		}
		catch (IOException e) {
			Log.e(TAG, "Cannot close logfile.");
		}
	}
	
	/**
	 * Starts the guidance thread
	 */
	public void run() {
		Looper.prepare();
		Thread.currentThread().setName("Guidance");
		Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
		mHandler = new Handler() {
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case EVAL_MOTOR_SPEED:
					reviseMotorSpeed();
					//Log.d(TAG, getErrorString());
					updateReceivers(getErrorString());
					break;
				case NEW_PID_VALUE:
					mGain[msg.arg1][msg.arg2] = (Double)msg.obj;
					break;
				case NEW_GUID_VECTOR:
					Double[] mVector = (Double[])msg.obj;
					for (int i = 0; i < 4; i++) {
						mMotorSpeed[i] = mVector[i];
					}
					updateMotors();
					break;
				case GET_PIDS:
					Receivable source = (Receivable) msg.obj;
					
					//Send each PID value to the requesting object
					for (int i = 0; i < 4; i++) {
						for (int j = 0; j < 3; j++) {
							source.receiveMessage("GUID:PID:VALUE:" +
												  i + ":" + j + ":" +
												  mGain[i][j],
												  null);
									
						}
					}							
					break;
				}
			}
		};
		//mHandler.sendEmptyMessage(EVAL_MOTOR_SPEED);
		receiveMessage("DIRECT:0:0:0:0", null);
		Looper.loop();
	}
	
	/**
	 * Receive a message.
	 * @param msg The message to process.
	 * @param source The source of the message, if a reply is needed.  May be null.
	 */
	public void receiveMessage(String msg, Receivable source) {
		//Log.d(TAG, "Receiving message " + msg);
		String[] parts = msg.split(":");
		if (parts[0].equals("CSYS")) {
			if (parts[1].equals("NOCONN")) {
				Log.v(TAG, "NoConn in guidance");
				receiveMessage("GUID:AUTOPILOT", source);
			}
		}
		if (parts[0].equals("GUID")) {
			if (parts[1].equals("PID")) {
				if (parts[2].equals("SET")) {
					Message newValue = Message.obtain(mHandler,
													  NEW_PID_VALUE,
													  new Integer(parts[3]),
													  new Integer(parts[4]), 
													  new Double(parts[5]));
					newValue.sendToTarget();
				}
				if (parts[2].equals("GET")) {
					Message getPids = Message.obtain(mHandler, GET_PIDS, source);
					getPids.sendToTarget();
				}
			}
			if (parts[1].equals("AUTOPILOT")) {
				Log.v(TAG, "AUTOPILOT mode");
				mGuidanceMode.set(AUTOPILOT);
				mHandler.removeMessages(NEW_GUID_VECTOR);
				if (!mHandler.hasMessages(EVAL_MOTOR_SPEED))
					mHandler.sendEmptyMessage(EVAL_MOTOR_SPEED);
			}
			if (parts[1].equals("DIRECT")) {
				Log.v(TAG, "direct mode");
				mGuidanceMode.set(DIRECT);
				mHandler.removeMessages(EVAL_MOTOR_SPEED);
				Double[] myVector = new Double[4];
				for (int i = 0; i < 4; i++) {
					myVector[i] = new Double(parts[i + 2]);
				}
				Message newValue = Message.obtain(mHandler, NEW_GUID_VECTOR, myVector);
				newValue.sendToTarget();
			}
			if (parts[1].equals("MANUAL")) {
				//autoPilot(false);
				String log = "manual mode: ";				
				if (parts.length > 2) {
					double[] newTarget = new double[4];
					for (int i = 0; i < 4; i++) {
						newTarget[i] = new Double(parts[i + 2]);
						log += newTarget[i] + ": ";
					}
					//Log.v(TAG, log);
					newTarget[0] *= Angler.MAX_ANGLE;
					newTarget[1] *= Angler.MAX_ANGLE;
					newTarget[2] += 1.0;
					//newTarget[2] *= 2.0;
					synchronized (mAngleTarget) {
						System.arraycopy(newTarget, 0, mAngleTarget, 0, 4);
					}
				}
				mGuidanceMode.set(MANUAL);
				mHandler.removeMessages(NEW_GUID_VECTOR);
				if (!mHandler.hasMessages(EVAL_MOTOR_SPEED))
					mHandler.sendEmptyMessage(EVAL_MOTOR_SPEED);
			}
		}
	}
	
	/**
	 * Registers a receiver to receive Guidance updates.
	 * @param rec
	 */
	public void registerReceiver(Receivable rec) {
		synchronized (mRec) {
			mRec.add(rec);
		}
	}
	
	/** Core of the class; calculates new motor speeds based on status */
	private void reviseMotorSpeed() {
		mHandler.removeMessages(EVAL_MOTOR_SPEED);
		long starttime = System.currentTimeMillis();
		updateAngleTarget();
		
		//Retrieve current orientation.		
		
		mAzimuth = mStatus.getReadingField(AZIMUTH);		
		mPitchDeg = mStatus.getReadingField(PITCH);
		mRollDeg = -mStatus.getReadingField(ROLL);
		
		double[] errors = new double[4];
		synchronized (mAngleTarget) {
			errors[0] = mAngleTarget[0] - mRollDeg;
			errors[1] = mAngleTarget[1] - mPitchDeg;
			errors[2] = mAngleTarget[2] - mStatus.getGpsField(dALT);
			errors[3] = mAngleTarget[3] - mAzimuth;
		}
		
		String errs = "errors: ";
		for (int i = 0; i < 4; i++) {
			errs += errors[i] + ": ";
		}
		//Log.v(TAG, errs);
		//For azimuth, multiple possibilities exist for error, each equally valid; but only the error nearest zero makes practical sense.
		if (errors[3] > 180.0)
			errors[3] -= 360.0;
		if (errors[3] < -180.0)
			errors[3] += 360.0;
		
		
		for (int i = 0; i < 4; i++) {
			//Calculate proportional errors
			double err = errors[i];//mTarget[i] - mCurrent[i];
			

			//Calculate derivative errors.
			mErrors[i][2] = (err - mErrors[i][0]) * 1000.0 / (starttime - mLastUpdate);
			
			
			//Mark proportional error
			mErrors[i][0] = err;
			/*if (i == 2)
				Log.v(TAG, "guid, dalt err is " + err);*/
			//Update integral errors
			mErrors[i][1] -= mIntegralErrors[i][mIntegralIndex];
			mIntegralErrors[i][mIntegralIndex] = err;
			mErrors[i][1] += err;
			mIntegralIndex = ++mIntegralIndex % PIDREPS;
			
			//Calculate changes in output
			for (int j = 0; j < 3; j++) {
				mControlVars[i] += mErrors[i][j] * mGain[i][j];
			}
		}
		if (mGuidanceMode.get() == MANUAL) {
			synchronized (mAngleTarget) {
				mControlVars[2] = mAngleTarget[2];
			}
		}
		mLastUpdate = starttime;
		
		// Constrain control vars:
		mControlVars[0] = constrainValue(mControlVars[0], -1, 1);
		mControlVars[1] = constrainValue(mControlVars[1], -1, 1);
		mControlVars[2] = constrainValue(mControlVars[2], 0, 4);
		mControlVars[3] = constrainValue(mControlVars[3], -2, 2);
		
		/*String vars = "Control vars: ";
		for (int i = 0; i < 4; i++) {
			vars += mControlVars[i] + ": ";
		}
		Log.v(TAG, vars);*/
		
		controlVarsToMotorSpeeds();
		
		//Send motor values to motors here:
		updateMotors();
		
		//Log.v(TAG, "motors: " + mMotorSpeed[0] + ", " + mMotorSpeed[1] + ", " + mMotorSpeed[2] + ", " + mMotorSpeed[3]);
		//Sleep a while
		long timetonext = (1000 / PIDREPS) - (System.currentTimeMillis() - starttime);
		Log.v(TAG, "time to next: " + timetonext);
		int currentMode = mGuidanceMode.get();
		if ((currentMode == MANUAL) || (currentMode == AUTOPILOT)) {
			if (timetonext > 0)
				mHandler.sendEmptyMessageDelayed(EVAL_MOTOR_SPEED, timetonext);
			else {
				Log.e(TAG, "Guidance too slow");
				mHandler.sendEmptyMessage(EVAL_MOTOR_SPEED);
			}
		}
	}
	
	private void controlVarsToMotorSpeeds() {
		double pitchrad = mPitchDeg * Math.PI / 180.0;
		double rollrad = mRollDeg * Math.PI / 180.0;
		double gradient = Math.sqrt(
				Math.pow(Math.tan(rollrad), 2) +
				Math.pow(Math.tan(pitchrad), 2)
				);
		double ascentRad = Math.atan(gradient);
		
		double cosGrad = Math.cos(ascentRad);
		
		double x = mControlVars[0];
		double y = mControlVars[1];
		double z = mControlVars[2] / cosGrad;
		double t = mControlVars[3];
		
		mMotorSpeed[0] = Math.sqrt(constrainValue(t - 2*y + z, 0, 1));
		mMotorSpeed[1] = Math.sqrt(constrainValue(t + 2*y + z, 0, 1));
		mMotorSpeed[2] = Math.sqrt(constrainValue(-t - 2*x + z, 0, 1));
		mMotorSpeed[3] = Math.sqrt(constrainValue(-t + 2*x + z, 0 ,1));
	}
	
	private void updateAngleTarget() {
		if (mGuidanceMode.get() != AUTOPILOT) {
			return;
		}
		long currentGpsTimeStamp = mStatus.getGpsTimeStamp();
		if (mLastGpsTimestamp == currentGpsTimeStamp) {
			return;
		}
		mLastGpsTimestamp = currentGpsTimeStamp;
		synchronized (mAngleTarget) {
			mAngler.getAngleTarget(mAngleTarget);
		}
	}
	
	private static double constrainValue(double requested, double min, double max) {
		if (requested > max) {
			return max;
		}
		if (requested < min) {
			return min;
		}
		return requested;
	}
	
	/**
	 * Write motor values to ChopperStatus, BluetoothOutput, logfile.
	 */
	private void updateMotors() {
		//Pass filtered values to ChopperStatus.
		mStatus.setMotorFields(mMotorSpeed);
		String logline = Long.toString(System.currentTimeMillis()) + " " + mMotorSpeed[0] + " " + mMotorSpeed[1] + " " + mMotorSpeed[2] + " " + mMotorSpeed[3] + "\n";
		try {
			if (logfile != null) {
				logfile.write(logline);
				logfile.flush();
			}
		}
		catch (IOException e) {
			//Log.e(TAG, "Cannot write to logfile");
		}
		//Pass motor values to motor controller!
		Message msg = Message.obtain(mBt.mHandler, SEND_MOTOR_SPEEDS, mMotorSpeed);
		msg.sendToTarget();
		//Log.i(TAG, "Guidance sending message.");
		
	}
	
	/**
	 * Updates all receivers
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

