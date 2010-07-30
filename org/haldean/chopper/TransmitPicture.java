package org.haldean.chopper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.concurrent.atomic.AtomicInteger;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

/**
 * Transmits telemetry frames to the control server
 * @author Benjamin Bardin
 */
public final class TransmitPicture implements Receivable, Constants
{	
	/**
	 * Handles thread scheduling, instructions from other threads
	 */
	public Handler handler;
	
	/* Quality of a compressed preview frame.  Minimum is 0, maximum is 100, default is 25. */
	private final AtomicInteger prevQuality = new AtomicInteger(25);
	
	/* How long (in ms) TransmitPicture should wait, if no new preview frame is available for transmission,
	 * before trying again. */
	private static final int CAMERAINTERVAL = 2000;
	
	/* Output stream */
	private ObjectOutputStream dataout;
	
	/* Local copy of frame to transmit */
	private byte[] sendpic;
	
	/* For local JPEG compression */
	private ByteArrayOutputStream baos;
	
	/* Android 2.2 comes with new YUV --> JPEG compression algorithms that sometimes don't work.
	 * This flags whether or not to try to use them.
	 */
	private static final boolean NEWCOMPRESSMETHOD = false;
	
	/* Used as part of the YUV --> JPEG coding process if NEWCOMPRESSMETHOD is false */
	private int[] rgb = new int[0];
	
	/* Tag for logging */
	private static final String TAG = "chopper.TransmitPicture";
	
	private MakePicture myMakePic;
	private Comm mComm;
	
	private Runner myRunner;
	private static PersistentThread myThread;
	
	/**
	 * Constructs the TransmitPicture thread.
	 * @param mydata The outputstream over which telemetry frames should be sent.
	 */
	public TransmitPicture(ObjectOutputStream mydata, MakePicture makePic, Comm comm)
	{
		if ((mydata == null) | (makePic == null) | (comm == null))
			throw new NullPointerException();
		myMakePic = makePic;
		mComm = comm;
		baos = new ByteArrayOutputStream();
		dataout = mydata;
	}
	
	public PersistentThread getPersistentThreadInstance() {
		if (myRunner == null) {
			myRunner = new Runner(this);
		}
		if (myThread == null) {
			myThread = new PersistentThread(myRunner);
		}
		return myThread;
	}
	
	private class Runner implements Runnable {
		private TransmitPicture mTp;
		
		private Runner(TransmitPicture tP) {
			mTp = tP;
		}
		
		/**
		 * Runs the thread.
		 */
		public void run()
		{
			Looper.prepare();
			
			Thread.currentThread().setName("TransmitPicture");
			handler = new Handler() {
	            public void handleMessage(Message msg)
	            {
	                switch (msg.what) {
	                case SEND_PIC:
	                	try	{
	                		transmit();
	                	}
	                	catch (IOException e) {
	                		System.out.println("Connection failed, reconnecting in " + Comm.CONNECTION_INTERVAL);
	                		//Does not actually try to reconnect; it is assumed that chopperStatus will also fail and give the command to reconnect.
	                		e.printStackTrace();
	                	}
	                	break;
	                }
	            }
	        };
	        Log.d(TAG, "Handler initialized");
	        mComm.registerReceiver(IMAGE, mTp);
			if (dataout == null) //For debugging only; should not happen
				System.out.println("Null dataout");
			
			
			handler.sendEmptyMessageDelayed(SEND_PIC, CAMERAINTERVAL);//Send first picture, after giving the camera time to warm up.
			Looper.loop();
		}
	}
	
	public void receiveMessage(String msg, Receivable source) {
		String[] parts = msg.split(":");
		if (parts[0].equals("IMAGE")) {
			if (parts[1].equals("RECEIVED")) {
				handler.sendEmptyMessage(SEND_PIC);
			}
			if (parts[1].equals("SET")) {
				if (parts[2].equals("QUALITY")) {
					Integer newq = new Integer(parts[3]);
					if (Math.abs(newq) <= 100) {
						setPreviewQuality(newq);
					}
					else {
						if (source != null) {
							source.receiveMessage("IMAGE:REQUEST:DENIED", this);
						}
					}
				}
			}
			if (parts[1].equals("GETPARAMS")) {
				if (source != null) {
					source.receiveMessage("IMAGE:FRAMEQUALITY:" + getPreviewQuality(), this);
				}
			}
		}
	}
	/**
	 * Obtains the JPEG compression quality of each transmitted frame.
	 * @return The quality.
	 */
	public int getPreviewQuality() {
		return prevQuality.get();
	}
	
	/**
	 * Sets the JPEG compression quality of each transmitted frame.
	 * @param newQ The new quality.
	 */
	public void setPreviewQuality(int newQ) {
		prevQuality.set(newQ);
	}
	
	public void setOutputStream(ObjectOutputStream newDataOut) {
		synchronized (dataout) {
			dataout = newDataOut;
		}
	}
	
	/**
	 * Transmits a frame of telemetry.
	 * @throws IOException If the connection fails
	 */
	private void transmit() throws IOException {
		if (!myMakePic.isFrameNew()) {
			handler.sendEmptyMessageDelayed(SEND_PIC, CAMERAINTERVAL); //wait a bit, try again later.
			System.out.println("Same pic");
			return;
		}
		
		long starttime = System.currentTimeMillis();
			sendpic = myMakePic.getBufferCopy(); //create a new copy.

		
		myMakePic.setFrameNewnessTo(false);
		
		//System.out.println("Retrieved frame");
		if (sendpic.length == 0)
		{
			System.out.println("temppic unprocessed");
			handler.sendEmptyMessageDelayed(SEND_PIC, Comm.CONNECTION_INTERVAL); //wait a bit, try again later.
			return;
		}
		
		if (myMakePic.updateFrameSize())
			System.out.println("Picture updated succesfully.");
		else
			mComm.sendMessage("IMAGE:REQUEST:DENIED");
		int[] frameSize = myMakePic.getFrameSize();
		
		if (NEWCOMPRESSMETHOD)
		{
			YuvImage sourcePic = null;
			try {
				
				sourcePic = new YuvImage(sendpic, myMakePic.getPreviewFormat(), frameSize[0], frameSize[1], null);
			}
			catch (Throwable t)
			{
				t.printStackTrace();
			}
			
			//System.out.println(MakePicture.XPREV + ", " + MakePicture.YPREV + "; next: " + MakePicture.nextx + ", " + MakePicture.nexty);
		
			System.out.println("Compressing to jpeg");
			try {
				sourcePic.compressToJpeg(new Rect(0, 0, frameSize[0], frameSize[1]), prevQuality.get(), baos);
			}
			catch (Throwable t) {
				System.out.println("Compress fail");
				t.printStackTrace();
			}
			System.out.println("Finished compressing");
		}
		else {
			//System.out.println("Bitmap compression");
			if (frameSize[0] * frameSize[1] != rgb.length)
				rgb = new int[frameSize[0] * frameSize[1]];
			decodeYUV420SP(rgb, sendpic, frameSize[0], frameSize[1]);
			//System.out.println(MakePicture.XPREV + ", " + MakePicture.YPREV + "; next: " + MakePicture.nextx + ", " + MakePicture.nexty);
			Bitmap mBitMap = Bitmap.createBitmap(rgb, frameSize[0], frameSize[1], Bitmap.Config.RGB_565);
			mBitMap.compress(Bitmap.CompressFormat.JPEG, prevQuality.get(), baos);
		}
		if (baos == null)
			System.out.println("bad stream");
		byte[] temppic = baos.toByteArray();
		baos.reset();
		long endtime = System.currentTimeMillis();
    	Log.v(TAG, "Pic Processing took " + (endtime - starttime));
		Log.i(TAG, "Sending a pic, length " + temppic.length);
		//Notifies the control console that the next transmission will be an image.
		//i.e. not a text-based one.
		String PicPrep = "IMAGE:" + Integer.toString(temppic.length) + ":" + System.currentTimeMillis();
		mComm.sendMessage(PicPrep);
		try	{
			//sends the picture
			if (dataout != null) {
				synchronized (dataout) {
					dataout.write(temppic);
					dataout.flush();
				}
			}
		}
		catch (Throwable t) {
			Log.w(TAG, "Exception thrown in telemetry transmission.");
			t.printStackTrace();
			handler.sendEmptyMessageDelayed(SEND_PIC, Comm.CONNECTION_INTERVAL); //wait a bit, try again later.
		}
		//System.out.println("Pic sent, ms: " + (System.currentTimeMillis() - endtime));
	}
	
	/**
	 * Transcodes a YUV 4:2:0 SP frame (delivered by the camera preview) to bitmap (compressible to jpeg)
	 * @param rgb The array in which the new bitmap will be stored.
	 * @param yuv420sp The source image.
	 * @param width The width of the image.
	 * @param height The height of the image.
	 * @author justinbonnar
	 */
	private static final void decodeYUV420SP(int[] rgb, byte[] yuv420sp, int width, int height) {

    	final int frameSize = width * height;
    	
    	for (int j = 0, yp = 0; j < height; j++) {
    		int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
    		for (int i = 0; i < width; i++, yp++) {
    			int y = (0xff & ((int) yuv420sp[yp])) - 16;
    			if (y < 0) y = 0;
    			if ((i & 1) == 0) {
    				v = (0xff & yuv420sp[uvp++]) - 128;
    				u = (0xff & yuv420sp[uvp++]) - 128;
    			}
    			
    			int y1192 = 1192 * y;
    			int r = (y1192 + 1634 * v);
    			int g = (y1192 - 833 * v - 400 * u);
    			int b = (y1192 + 2066 * u);
    			
    			if (r < 0) r = 0; else if (r > 262143) r = 262143;
    			if (g < 0) g = 0; else if (g > 262143) g = 262143;
    			if (b < 0) b = 0; else if (b > 262143) b = 262143;
    			
    			rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
    		}
    	}
	}
}
