package org.haldean.chopper.pilot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

/**
 * Handles connectivity with control server. <P>
 * 
 * Will send any message received from the control server to registered Receivables. <br>
 * 
 * Additionally, may independently send the following messages to registered Receivables:<br>
 * <pre>
 * CSYS:NOCONN
 * </pre>
 * 
 * When sending the following messages to registered Receivables, the Comm component will also process them locally:<br>
 * <pre>
 * IMAGE:SETUP
 * COMM:PULSE
 * CSYS:NOCONN
 * </pre>
 * 
 * The Comm component will forward any message received from Chopper components to the control server.<br>
 * 
 * @author Benjamin Bardin
 */
public final class Comm implements Runnable, Receivable, Constants {	
	
	/** How long (in ms) to wait, upon connectivity failure, before attempting to reestablish connection. */
	public final static int CONNECTION_INTERVAL = 5000;
	
	/** URL of the control server */
	private final String mControl = new String("droidcopter.cs.columbia.edu");
	
	/** Port used for text connection */
	private final int mTextOutPort = 7000;
	
	/** Port used for data connection (telemetry) */
	private final int mDataOutPort = 7001;
	
	/** How long (in ms) to wait for the first PULSE signal before assuming connectivity failure */
	public static final int FIRST_PULSE = 30000;
	
	/** How long (in ms) to wait for subsequent PULSE signals before assuming connectivity failure. */
	public static final int PULSE_RATE = 4000;
	
	/** Tag for logging */
	public static final String TAG = "chopper.Comm";	
	
	/** Communication sockets */
	private Socket mTextSocket;
	private Socket mDataSocket;
	
	/** Reading & Riting */
	private PrintWriter mTextOut;
	private ObjectOutputStream mDataOut;
	private BufferedReader mTextIn;
	
	/** Message handler */
	private Handler mHandler;
	
	/** For initializing a text connection */
	private Runnable mTextConnArg;
	private Thread mTextConn;
	private ReentrantLock mTextConnLock = new ReentrantLock();
	
	/** For initializing a data connection */
	private Runnable mDataConnArg;
	private Thread mDataConn;
	private ReentrantLock mDataConnLock = new ReentrantLock();
	
	/** Receivers */
	private Vector<LinkedList<Receivable>> mMsgTypes;
	
	/** Heartbeat Timer */
	private Timer mCountdown;
	private TimerTask mHeartbeat;
	
	/** Handles to other chopper components */
	private MakePicture mTelemSrc;	
	private TransmitPicture mPic;
	
	/** Thread pool for mutator methods */
	private ExecutorService mPool;
	
	/** Number of threads to run in the pool */
	private static int sNumPoolThreads = 5;
	
	/** When set to false, only transmits messages; does not receive */
	private boolean mAcceptMsgs;
	
	/**
	 * Initializes the object; instructions on how to connect to server.
	 * @param takeMsgs If set to false, will only transmit; must be true to receive/process instructions
	 */
	public Comm(boolean takeMsgs)
	{
		final Comm mComm = this;
		mAcceptMsgs = takeMsgs;
		mPool = Executors.newFixedThreadPool(sNumPoolThreads);
		
		mCountdown = new Timer();
		mMsgTypes = new Vector<LinkedList<Receivable>>(MSG_TYPES);
		for (int i = 0; i < MSG_TYPES; i++) {
			mMsgTypes.add(new LinkedList<Receivable>());
		}
		
		mHeartbeat = new TimerTask() {
			public void run() {
				updateReceivers("CSYS:NOCONN");
			}
		};
		
		/* Runnable that establishes a text connection */
		mTextConnArg = new Runnable() {
			public void run() {
				Thread.currentThread().setName("MakeTextConn");
				try	{
					destroyTextConn();
					
					Log.i(TAG, "Initializing text sockets... ");
					mTextSocket = new Socket(mControl, mTextOutPort);
					mTextOut = new PrintWriter(mTextSocket.getOutputStream(), true);
					mTextIn = new BufferedReader(new InputStreamReader(mTextSocket.getInputStream()));
					
					Log.i(TAG, "\tText Sockets initialized.");
					Log.i(TAG, "Connection established");

			        /* Initializes heartbeat protocol */
					mCountdown.purge();
			        mCountdown.schedule(mHeartbeat, FIRST_PULSE);
			        startReading();
				}
				/* Something's wrong with the internet connection.  Try again soon. */
				catch (IOException e) {
					e.printStackTrace();
					System.out.println("Failed to establish text connection.  Reattempting in " + CONNECTION_INTERVAL);
					mHandler.sendEmptyMessageDelayed(MAKE_TEXT_CONN, CONNECTION_INTERVAL);
				}
			}
		};
		mTextConn = new Thread(mTextConnArg);
		
		/* Runnable that establishes a data connection */
		mDataConnArg = new Runnable() {
			public void run() {
				Thread.currentThread().setName("MakeDataConn");
				/* Kills the transmit picture thread, will be restarted when a connection is established. */
				try {
					
					destroyDataConn();
					
					Log.i(TAG, "Initializing data sockets... ");
					mDataSocket = new Socket(mControl, mDataOutPort);
					mDataOut = new ObjectOutputStream(mDataSocket.getOutputStream());
					
					if (mPic == null) { //First time being run
						mPic = new TransmitPicture(mDataOut, mTelemSrc, mComm);
						PersistentThread mTransPicThread = mPic.getPersistentThreadInstance();
						if (!mTransPicThread.isAlive()) {
							mTransPicThread.start();
						}
					}
					else { //Thread has already been started, simply being reset
						synchronized (mDataOut) {
							mPic.setOutputStream(mDataOut);
						}
						if (mPic.mHandler != null) {
							mPic.mHandler.sendEmptyMessage(SEND_PIC);
						}
					}
					
					Log.i(TAG, "\tData Sockets initialized.");
				}
				/* Something's wrong with the internet connection.  Try again soon. */
				catch (IOException e) {
					e.printStackTrace();
					System.out.println("Failed to establish data connection.  Reattempting in " + CONNECTION_INTERVAL);
					mHandler.sendEmptyMessageDelayed(MAKE_DATA_CONN, CONNECTION_INTERVAL);
				}
				
				
			}
		};
		mDataConn = new Thread(mDataConnArg);
	}
	
	/**
	 * Processes a message by sending it to the control server.
	 * @param msg The message to process.
	 * @param source The source of the message.  May be null.
	 */
	public void receiveMessage(String msg, Receivable source) {
		sendMessage(msg);
	}
	
	/**
	 * Registers a receiver to receive a category of Comm updates, especially MakePicture and Navigation objects.
	 * @param msgType The type of updates for which to register.
	 * @param receiver The receiver to register.
	 * @see MakePicture MakePicture
	 * @see Navigation Navigation
	 */
	public void registerReceiver(int msgType, Receivable receiver) {
		LinkedList<Receivable> myList = mMsgTypes.get(msgType);
		synchronized (myList) {
			myList.add(receiver);
		}
	}
	
	/**
	 * Registers a receiver to receive all categories of Comm updates.
	 * @param receiver
	 */
	public void registerReceiver(Receivable receiver) {
		for (int i = 0; i < MSG_TYPES; i++) {
			registerReceiver(i, receiver);
		}
	}
	
	/**
	 * Starts the communication thread.
	 */
	public void run() {
		Looper.prepare();
		Log.d(TAG, "Starting comm thread");
		Thread.currentThread().setName("Comm");
		/* Registers actions */
		mHandler = new Handler() {
			public void handleMessage(Message msg) {
                switch (msg.what) {
                case MAKE_TEXT_CONN:
                	Log.i(TAG, "Making Text connection");
                	if (mTextConnLock.tryLock()) { //if not, the thread is in the process of being created. No action necessary
	                	if (!mTextConn.isAlive()) {//a connection is being established
	                		mTextConn = new Thread(mTextConnArg);
	            			mTextConn.start(); //Try to connect
	            			mTextConnLock.unlock();
	                	}
                	}
                	break;
                case MAKE_DATA_CONN:
                	Log.i(TAG, "Making data connection");
                	if (mDataConnLock.tryLock()) { //if not, the thread is in the process of being created. No action necessary
	                	if (!mDataConn.isAlive()) {//a connection is being established
	                		mDataConn = new Thread(mDataConnArg);
	                		mDataConn.start(); //Try to connect
	                		mDataConnLock.unlock();
	                	}
                	}
                	break;
                }
            }
		};
        /* Make a connection */
        mHandler.sendEmptyMessage(MAKE_TEXT_CONN);
        Looper.loop();
	}

	/**
	 * Sends a message back to the control server.  Called from other threads/classes.
	 * @param message The message to send.
	 */
	public void sendMessage(final String message) {
		if (mTextOut == null) { //connection not yet initialized
			return;
		}
		else {
			mPool.submit(new Runnable() {
				public void run() {
					try {
						mTextOut.println(message);
						mTextOut.flush();
					}
					/* The connection might be broken */
					catch (Throwable t) {
						t.printStackTrace();
						Log.w(TAG, "Connection appears to be lost.  Attempting to reconnect.");
						mHandler.sendEmptyMessageDelayed(MAKE_TEXT_CONN, CONNECTION_INTERVAL); //Try to reconnect soon
					}
				}
			});
		}
	}
	
	/**
	 * On first call, sets a MakePicture as the telemetry source.
	 * Subsequent calls have no effect.
	 * @param mP The MakePicture to set as the source.
	 */
	public void setTelemetrySource(MakePicture mP) {
		if (mTelemSrc == null) {
			mTelemSrc = mP;
		}
	}
	
	/** Tears down the data (telemetry) connection. */
	private void destroyDataConn() {
		Log.i(TAG, "Closing data sockets...");
		try {
			if (mDataOut != null)
				mDataOut.close();
			if (mDataSocket != null)
				mDataSocket.close();
		}
		/* Sockets might already be closed */
		catch (IOException e) {
			Log.i(TAG, "Data sockets already closed.");
		}
	}
	
	/** Tears down the text connection. */
	private void destroyTextConn() {
		Log.i(TAG, "Closing text sockets...");
		try {
			if (mTextIn != null) 
				mTextIn.close();
			if (mTextOut != null)
				mTextOut.close();
			if (mTextSocket != null)
				mTextSocket.close();
		}
		catch (IOException e) {
			Log.i(TAG, "Text sockets already closed.");
		}
	}
	
	/** Processes a message meant specifically for the Comm component,
	 * and not simply to be relayed to other components.  */
	private boolean isItForMe(String msg) {
		String[] parts = msg.split(":");
		if (parts[0].equals("IMAGE")) {
			if (parts[1].equals("SETUP")) {
				if (mTelemSrc != null) {
					mHandler.sendEmptyMessage(MAKE_DATA_CONN);
				}
				return true;
			}
		}
		if (parts[0].equals("COMM")) {
			if (parts[1].equals("PULSE")) {
				sendMessage(msg);
				//Reset the heartbeat countdown
				mCountdown.purge();
		        mCountdown.schedule(mHeartbeat, PULSE_RATE);
		        return true;
			}
		}
		if (parts[0].equals("CSYS")) {
			if (parts[1].equals("NOCONN")) {
				mHandler.sendEmptyMessageDelayed(MAKE_TEXT_CONN, CONNECTION_INTERVAL); //Try to reconnect soon
				return true;
			}
			
		}
		return false;
	}
	
	/** Spawns a thread that starts reading from the text socket. */
	private void startReading() {
		if (!mAcceptMsgs) {
			return;
		}
		new Thread(new Runnable() {
			public void run() {
				Thread.currentThread().setName("TextConn");
				String input;
				try {
					while ((input = mTextIn.readLine()) != null) {
						final String mInput = input;
						mPool.submit(new Runnable() {
							public void run() {
								updateReceivers(mInput);
							}
						});
					}
					mHandler.sendEmptyMessageDelayed(MAKE_TEXT_CONN, CONNECTION_INTERVAL); //Try to reconnect soon
					Log.w(TAG, "Error reading from Socket.  Reconnecting.");
				}
				catch (Throwable t) {
					mHandler.sendEmptyMessageDelayed(MAKE_TEXT_CONN, CONNECTION_INTERVAL); //Try to reconnect soon
					Log.w(TAG, "Error reading from Socket.  Reconnecting.");
					t.printStackTrace();
				}
			}
		}).start();
	}
	
	/**
	 * Processes communications received from the server, and some internal system-wide messages.
	 * @param msg The method to process.
	 */
	private void updateReceivers(String msg) {
		Log.i(TAG, msg);
		
		isItForMe(msg);
		
		ListIterator<Receivable> myList = null;
		
		if (msg.startsWith("IMAGE:")) {
			myList = mMsgTypes.get(IMAGE).listIterator();
		}
		
		if (msg.startsWith("NAV:")) {
			myList = mMsgTypes.get(NAV).listIterator();
		}
		
		if (msg.startsWith("COMM:")) {
			myList = mMsgTypes.get(COMM).listIterator();
		}
		
		if (msg.startsWith("CSYS:")) {
			myList = mMsgTypes.get(CSYS).listIterator();
		}
		
		if (msg.startsWith("GUID:")) {
			myList = mMsgTypes.get(GUID).listIterator();
		}
		
		if (myList != null) {
			synchronized (myList) {
				while (myList.hasNext()) {
					myList.next().receiveMessage(msg, this);
				}
			}
		}
	}
}
