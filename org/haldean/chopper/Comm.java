package org.haldean.chopper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;

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
	
	//private static boolean die = false; //signal used to kill the thread

	private static SurfaceHolder sh;
	private static boolean makingconnection = false;
	
	public Comm(SurfaceHolder mysh)
	{
		super();
		sh = mysh;
	}
	public static void sendMessage(String message) //for when other classes want to send a message back to the control computer
	{
		System.out.println("Sending message: " + message);
		if (textout == null) {
			return;
		}
		try {
			textout.println(message);
		}
		catch (Throwable t) {
			t.printStackTrace();
			System.out.println("Connection appears to be lost.  Attempting to reconnect.");
			mHandler.sendEmptyMessage(MAKECONNECTION);
		}
	}
	
	private static void establishConnection()
	{
		if (makingconnection)
			return;
		TransmitPicture.stopLoop();
		makingconnection = true;
		while (makingconnection) {
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
		        
		        startReading();
				makingconnection = false;
			}
			catch (IOException e) {
				System.out.println("Connection unsuccesful.  Reestablishing in " + CONNECTIONINTERVAL);
				e.printStackTrace();
				try {
					sleep(CONNECTIONINTERVAL);
				}
				catch (InterruptedException f) {
					System.out.println("For real, guys?  I mean, really?");
					f.printStackTrace();
				}
			}
		}
	}
	
	//main functionality
	public void run()
	{
		Looper.prepare();
		setName("CommOut");
		System.out.println("CommOut run() thread ID " + getId()); //for debugging
		
		mHandler = new Handler() {
			public void handleMessage(Message msg)
            {
                switch (msg.what){
                case MAKECONNECTION:
                	establishConnection();
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
	
	private static void updateAll(String msg) {
		System.out.println(msg);
		String[] parts = msg.split(":");
		if (parts[0].equals("IMAGE")) {
			if (parts[1].equals("RECEIVED")) {
				TransmitPicture.mHandler.sendEmptyMessage(SENDAPIC);
			}
		}
	}
}
