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
import android.view.SurfaceHolder;

public final class Comm extends Thread implements Constants
{	
	private static Socket textsocket; //text communication
	private static Socket datasocket;
	
	private static PrintWriter textout; //outbound messages
	private static ObjectOutputStream dataout; //outbound objects--jpeg image.
	private static BufferedReader textin;
	private static Handler mHandler;
	
	private static Runnable textConnArg;
	private static Thread textConn;
	private static ReentrantLock textConnLock = new ReentrantLock();
	
	private static Runnable dataConnArg;
	private static Thread dataConn;
	private static ReentrantLock dataConnLock = new ReentrantLock();
	
	private static Timer countdown;
	
	private static SurfaceHolder sh;
	
	public Comm(SurfaceHolder mysh)
	{
		super("Comm");
		sh = mysh;
		countdown = new Timer();
		
		dataConnArg = new Runnable() {
			public void run() {
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
				catch (IOException e) {
					e.printStackTrace();
					System.out.println("Failed to establish data connection.  Reattempting in " + CONNECTIONINTERVAL);
					mHandler.sendEmptyMessageDelayed(MAKEDATACONN, CONNECTIONINTERVAL);
				}
				
				
			}
		};
		dataConn = new Thread(dataConnArg);
		
		//Runnable object that handles establishing a connection
		textConnArg = new Runnable() {
			public void run() {
				//Kills the transmit picture thread, will be restarted when a connection is established.
				
				try	{
					//Try to connect, set up sockets
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
					
					
			        
			        //Initializes heartbeat protocol
			        countdown.cancel();
			        countdown = new Timer();
			        countdown.schedule(new TimerTask() {
							public void run() {
								updateAll("SYS:NOCONN"); //If the timer runs down, connection lost; update the system.
							}
						}, FIRSTPULSE);
			        startReading();
				}
				catch (IOException e) {
					e.printStackTrace();
					System.out.println("Failed to establish text connection.  Reattempting in " + CONNECTIONINTERVAL);
					mHandler.sendEmptyMessageDelayed(MAKETEXTCONN, CONNECTIONINTERVAL);
				}
			}
		};
		textConn = new Thread(textConnArg);
	}
	
	private static void destroyDataConn() {
		System.out.println("Closing data sockets...");
		try {
			if (dataout != null)
				dataout.close();
			if (datasocket != null)
				datasocket.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//for when other classes want to send a message back to the control computer
	public static void sendMessage(String message) {
		//System.out.println(message);
		if (textout == null) { //connection not yet initialized
			return;
		}
		try {			
			textout.println(message);
			//textout.flush();
		}
		//If the connection has broken
		catch (Throwable t) {
			t.printStackTrace();
			System.out.println("Connection appears to be lost.  Attempting to reconnect.");
			mHandler.sendEmptyMessageDelayed(MAKETEXTCONN, CONNECTIONINTERVAL); //Try to reconnect soon
		}
	}
	
	//initial functionality
	public void run() {
		Looper.prepare();
		
		mHandler = new Handler() { //interthread, time-delayed communication
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
        
        //actual picture-taking and sending are run in separate threads, so the picture can be processed while other data is being sent.

        //This object takes pictures in an infinite loop.
        MakePicture takepic = new MakePicture(sh);
        sh = null;
        takepic.start();
        System.out.println("CommOut takepic thread ID " + takepic.getId()); //for debugging
        
        //Give everything a little delay, then try to connect
        //mHandler.sendEmptyMessageDelayed(MAKECONNECTION, CONNECTIONINTERVAL);
        mHandler.sendEmptyMessage(MAKETEXTCONN);
        //mHandler.sendEmptyMessage(MAKEDATACONN);
        Looper.loop();
	}
	
	//Start reading from the input thread
	private static void startReading() {
		String input;
		try {
			while ((input = textin.readLine()) != null) {
			    updateAll(input);
			    System.out.println("Waiting for input");
			}
		}
		catch (Throwable t) {
			mHandler.sendEmptyMessageDelayed(MAKETEXTCONN, CONNECTIONINTERVAL); //Try to reconnect soon
			t.printStackTrace();
		}
	}
	
	//Used to process received communications, as well as internal system messages.
	public static void updateAll(String msg) {
		System.out.println(msg);
		String[] parts = msg.split(":");
		if (parts[0].equals("IMAGE")) {
			if (parts[1].equals("RECEIVED")) {
				TransmitPicture.mHandler.sendEmptyMessage(SENDAPIC);
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
				mHandler.sendEmptyMessage(MAKEDATACONN);
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
				if (parts[1].equals("AUTOPILOT"))
					Navigation.autoPilot(true);
			}
		}
		if (parts[0].equals("SYS")) { //Internal message
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
