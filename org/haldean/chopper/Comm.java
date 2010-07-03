package org.haldean.chopper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

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
	private static Runnable newconnarg;
	private static Thread newconn;
	private static TimerTask heartbeat;
	private static Timer countdown;
	
	private static SurfaceHolder sh;
	
	public Comm(SurfaceHolder mysh)
	{
		super("Comm");
		sh = mysh;
		countdown = new Timer();
		heartbeat = new TimerTask() {
			public void run() {
				updateAll("SYS:NOCONN");

			}
		};
		
		newconnarg = new Runnable() {
			public void run() {
				TransmitPicture.stopLoop();
				try	{
					//Try to connect, set up sockets
					System.out.println("Initializing network sockets... ");
					textsocket = new Socket(control, textoutport);
					textout = new PrintWriter(textsocket.getOutputStream(), true);
					
					datasocket = new Socket(control, dataoutport);
					dataout = new ObjectOutputStream(datasocket.getOutputStream());
					
					textin = new BufferedReader(new InputStreamReader(textsocket.getInputStream()));
					
					System.out.println("\tSockets initialized.");
					System.out.println("Connection established");
					
					TransmitPicture transpic = new TransmitPicture(dataout);
			        System.out.println("CommOut transpic thread ID " + transpic.getId());
			        transpic.start();
			        countdown.schedule(heartbeat, FIRSTPULSE);
			        startReading();
				}
				catch (IOException e) {
					e.printStackTrace();
					System.out.println("Failed to establish connection.  Reattempting in " + CONNECTIONINTERVAL);
					mHandler.sendEmptyMessageDelayed(MAKECONNECTION, CONNECTIONINTERVAL);
				}
			}
		};
		newconn = new Thread(newconnarg);
	}
	public static void sendMessage(String message) { //for when other classes want to send a message back to the control computer
		if (textout == null) {
			return;
		}
		try {			
			textout.println(message);
			textout.flush();
		}
		catch (Throwable t) {
			t.printStackTrace();
			System.out.println("Connection appears to be lost.  Attempting to reconnect.");
			mHandler.sendEmptyMessageDelayed(MAKECONNECTION, CONNECTIONINTERVAL);
		}
	}
	
	//main functionality
	public void run() {
		Looper.prepare();
		
		mHandler = new Handler() {
			public void handleMessage(Message msg) {
                switch (msg.what) {
                case MAKECONNECTION:
                	synchronized (newconn) {
	                	if (!newconn.isAlive()) {//a connection is being established
	                		newconn = new Thread(newconnarg);
	            			newconn.start();
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
        System.out.println("CommOut takepic thread ID " + takepic.getId());
        mHandler.sendEmptyMessageDelayed(MAKECONNECTION, CONNECTIONINTERVAL);
        
        Looper.loop();
	}
	
	private static void startReading() {
		String input;
		try {
			while ((input = textin.readLine()) != null) {
			    updateAll(input);
			    System.out.println("Waiting for input");
			}
		}
		catch (IOException e) {
			e.printStackTrace();
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
			if (parts[1].equals("GET")) {
				Comm.sendMessage("IMAGE:PARAMS:" + MakePicture.XPREV + ":" + MakePicture.YPREV + ":" + TransmitPicture.PREVQUALITY);
			}
		}
		if (parts[0].equals("NAV")) {
			if (parts[1].equals("SET")) {
				if (parts[2].equals("MANUAL")) {
					Navigation.autoPilot(false);
					for (int i = 0; i < 4; i++) {
						Navigation.target[i] = new Double(parts[i + 3]);
					}
				}
				if (parts[1].equals("AUTOPILOT"))
					Navigation.autoPilot(true);
			}
			if (parts[1].equals("PULSE")) {
				countdown.purge();
				countdown.schedule(heartbeat, PULSERATE);
			}
		}
		if (parts[0].equals("SYS")) { //Internal message
			if (parts[1].equals("NOCONN")) {
				//Navigation.updateStatus(NOCONN);
				//Navigation.autoPilot(true);
			}
			if (parts[1].equals("LOWPOWER")) {
				Navigation.updateStatus(LOWPOWER);
			}
		}
	}
}
