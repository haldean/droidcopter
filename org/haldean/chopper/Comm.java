package org.haldean.chopper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantLock;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

/**
 * Handles connectivity with control server
 * @author Benjamin Bardin
 *
 */
public final class Comm extends Thread implements Constants {	
	
	/**
	 * How long (in ms) to wait, upon connectivity failure, before attempting to reestablish connection
	 */
	public static final int CONNECTIONINTERVAL = 5000;
	
	/**
	 * URL of the control server
	 */
	public static final String control = new String("pices.dynalias.org");
	
	/**
	 * Port used for text connection
	 */
	public static final int textoutport = 23;
	
	/**
	 * Port used for data connection (telemetry)
	 */
	public static final int dataoutport = 24;
	
	/**
	 * How long (in ms) to wait for the first PULSE signal before assuming connectivity failure
	 */
	public static final int FIRSTPULSE = 20000;
	
	/**
	 * How long (in ms) to wait for subsequent PULSE signals before assuming connectivity failure.
	 */
	public static final int PULSERATE = 3000;
	
	/* Communication sockets */
	private static Socket textsocket;
	private static Socket datasocket;
	
	/* Reading & Riting */
	private static PrintWriter textout;
	private static ObjectOutputStream dataout;
	private static BufferedReader textin;
	
	/* Message handler*/
	private static Handler handler;
	
	/* For initializing a text connection */
	private static Runnable textConnArg;
	private static Thread textConn;
	private static ReentrantLock textConnLock = new ReentrantLock();
	
	/* For initializing a data connection */
	private static Runnable dataConnArg;
	private static Thread dataConn;
	private static ReentrantLock dataConnLock = new ReentrantLock();
	
	/* Heartbeat Timer */
	private static Timer countdown;
	
	/**
	 * Initializes the object; instructions on how to connect to server.
	 */
	public Comm()
	{
		super("Comm");
		countdown = new Timer();
		
		/* Runnable that establishes a text connection */
		textConnArg = new Runnable() {
			public void run() {
				
				try	{
					System.out.println("Closing text sockets...");
					if (textout != null)
						textout.close();
					if (textsocket != null)
						textsocket.close();
					
					System.out.println("Initializing text sockets... ");
					textsocket = new Socket(control, textoutport);
					textout = new PrintWriter(textsocket.getOutputStream(), true);
					
					if (textin != null)
						textin.close();
					textin = new BufferedReader(new InputStreamReader(textsocket.getInputStream()));
					
					System.out.println("\tText Sockets initialized.");
					System.out.println("Connection established");
					
					
			        
			        /* Initializes heartbeat protocol */
			        countdown.cancel();
			        countdown = new Timer();
			        countdown.schedule(new TimerTask() {
							public void run() {
								updateAll("SYS:NOCONN"); //If the timer runs down, connection lost; update the system.
							}
						}, FIRSTPULSE);
			        startReading();
				}
				/* Something's wrong with the internet connection.  Try again soon. */
				catch (IOException e) {
					e.printStackTrace();
					System.out.println("Failed to establish text connection.  Reattempting in " + CONNECTIONINTERVAL);
					handler.sendEmptyMessageDelayed(MAKETEXTCONN, CONNECTIONINTERVAL);
				}
			}
		};
		textConn = new Thread(textConnArg);
		
		/* Runnable that establishes a data connection */
		dataConnArg = new Runnable() {
			public void run() {
				
				/* Kills the transmit picture thread, will be restarted when a connection is established. */
				TransmitPicture.stopLoop();
				try {
					
					destroyDataConn();
					
					System.out.println("Initializing data sockets... ");
					datasocket = new Socket(control, dataoutport);
					dataout = new ObjectOutputStream(datasocket.getOutputStream());
					TransmitPicture transpic = new TransmitPicture(dataout);
			        System.out.println("CommOut transpic thread ID " + transpic.getId());
			        transpic.start();
					System.out.println("\tData Sockets initialized.");
				}
				/* Something's wrong with the internet connection.  Try again soon. */
				catch (IOException e) {
					e.printStackTrace();
					System.out.println("Failed to establish data connection.  Reattempting in " + CONNECTIONINTERVAL);
					handler.sendEmptyMessageDelayed(MAKEDATACONN, CONNECTIONINTERVAL);
				}
				
				
			}
		};
		dataConn = new Thread(dataConnArg);
	}
	
	/* Tears down the data (telemetry) connection. */
	private static void destroyDataConn() {
		System.out.println("Closing data sockets...");
		try {
			if (dataout != null)
				dataout.close();
			if (datasocket != null)
				datasocket.close();
		}
		/* Sockets might already be closed */
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Sends a message back to the control server.  Called from other threads/classes.
	 * @param message The message to send.
	 */
	public static void sendMessage(String message) {
		//System.out.println(message);
		if (textout == null) { //connection not yet initialized
			return;
		}
		try {			
			textout.println(message);
		}
		/* The connection might be broken */
		catch (Throwable t) {
			t.printStackTrace();
			System.out.println("Connection appears to be lost.  Attempting to reconnect.");
			handler.sendEmptyMessageDelayed(MAKETEXTCONN, CONNECTIONINTERVAL); //Try to reconnect soon
		}
	}
	
	/**
	 * Starts the communication thread.
	 */
	public void run() {
		Looper.prepare();
		
		/* Registers actions */
		handler = new Handler() {
			public void handleMessage(Message msg) {
                switch (msg.what) {
                case MAKETEXTCONN:
                	if (textConnLock.tryLock()) { //if not, the thread is in the process of being created. No action necessary
	                	if (!textConn.isAlive()) {//a connection is being established
	                		textConn = new Thread(textConnArg);
	            			textConn.start(); //Try to connect
	            			textConnLock.unlock();
	                	}
                	}
                	break;
                case MAKEDATACONN:
                	if (dataConnLock.tryLock()) { //if not, the thread is in the process of being created. No action necessary
	                	if (!dataConn.isAlive()) {//a connection is being established
	                		dataConn = new Thread(dataConnArg);
	                		dataConn.start(); //Try to connect
	                		dataConnLock.unlock();
	                	}
                	}
                	break;
                }
            }
		};
        
        /* Make a connection */
        handler.sendEmptyMessage(MAKETEXTCONN);
        
        Looper.loop();
	}
	
	/* Starts reading from the text socket. */
	private static void startReading() {
		String input;
		try {
			while ((input = textin.readLine()) != null) {
			    updateAll(input);
			    //System.out.println("Waiting for input");
			}
		}
		catch (Throwable t) {
			handler.sendEmptyMessageDelayed(MAKETEXTCONN, CONNECTIONINTERVAL); //Try to reconnect soon
			t.printStackTrace();
		}
	}
	/**
	 * Processes communications received from the server and internal system-wide messages.
	 * @param msg The method to process.
	 */
	public static void updateAll(String msg) {
		System.out.println(msg);
		String[] parts = msg.split(":");
		if (parts[0].equals("IMAGE")) {
			if (parts[1].equals("RECEIVED")) {
				TransmitPicture.handler.sendEmptyMessage(SENDAPIC);
			}
			if (parts[1].equals("SET")) {
				if (parts[2].equals("QUALITY")) {
					Integer newq = new Integer(parts[3]);
					if (Math.abs(newq) <= 100)
						TransmitPicture.PREVQUALITY = newq;
					else
						Comm.sendMessage("IMAGE:REQUEST:DENIED");
				}
				if (parts[2].equals("SIZE")) {
					MakePicture.nextx = new Integer(parts[3]);
					MakePicture.nexty = new Integer(parts[4]);
				}
			}
			if (parts[1].equals("AVAILABLESIZES")) {
				MakePicture.sendSizes();
			}
			if (parts[1].equals("GETPARAMS")) {
				Comm.sendMessage("IMAGE:PARAMS:" + MakePicture.XPREV + ":" + MakePicture.YPREV + ":" + TransmitPicture.PREVQUALITY);
			}
			if (parts[1].equals("SETUP")) {
				handler.sendEmptyMessage(MAKEDATACONN);
			}
		}
		if (parts[0].equals("COMM")) {
			if (parts[1].equals("PULSE")) {
				//Reset the heartbeat countdown
				countdown.cancel();
		        countdown = new Timer();
		        countdown.schedule(new TimerTask() {
						public void run() {
							updateAll("SYS:NOCONN");
						}
					}, PULSERATE);
			}
		}
		if (parts[0].equals("NAV")) {
			if (parts[1].equals("SET")) {
				if (parts[2].equals("MANUAL")) {
					Navigation.autoPilot(false);
					Navigation.targetLock.lock();
					for (int i = 0; i < 4; i++) {
						Navigation.target[i] = new Double(parts[i + 3]);
					}
					Navigation.targetLock.unlock();
				}
				if (parts[2].equals("AUTOPILOT"))
					Navigation.autoPilot(true);
				if (parts[2].equals("AUTOTASK")) {
					Integer taskList = new Integer(parts[3]);
					Navigation.setTask(taskList, parts[4]);
				}
			}
			if (parts[1].equals("GET")) {
				if (parts[2].equals("AUTOTASK")) {
					String[] myTasks = Navigation.getTasks();
					for (int i = 0; i < myTasks.length; i++) {
						Comm.sendMessage("NAV:AUTOTASK:" + myTasks[i].toString());
					}
				}
			}
		}
		/* Internal Messages */
		if (parts[0].equals("SYS")) {
			if (parts[1].equals("NOCONN")) {
				//Navigation.updateStatus(NOCONN);
				//Navigation.autoPilot(true);
				//mHandler.sendEmptyMessageDelayed(MAKECONNECTION, CONNECTIONINTERVAL); //Try to reconnect soon

			}
			if (parts[1].equals("LOWPOWER")) {
				Navigation.updateStatus(LOWPOWER);
			}
		}
	}
}
