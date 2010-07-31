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
	public Handler mHandler;
	
	/** Quality of a compressed preview frame.  Minimum is 0, maximum is 100, default is 25. */
	private final AtomicInteger mPrevQuality = new AtomicInteger(25);
	
	/** How long (in ms) TransmitPicture should wait, if no new preview frame is available for transmission,
	 * before trying again. */
	private static final int CAMERA_INTERVAL = 2000;
	
	/** Output stream */
	private ObjectOutputStream mDataOut;
	
	/** For local JPEG compression */
	private ByteArrayOutputStream mBaos;
	
	/** Android 2.2 comes with new YUV --> JPEG compression algorithms that sometimes don't work.
	 * This flags whether or not to try to use them. */
	private static final boolean NEWCOMPRESSMETHOD = false;
	
	/** Used as part of the YUV --> JPEG coding process if NEWCOMPRESSMETHOD is false */
	private int[] rgb = new int[0];
	
	/** Tag for logging */
	private static final String TAG = "chopper.TransmitPicture";
	
	/** Handle to other chopper components */
	private MakePicture myMakePic;
	private Comm mComm;
	
	/** Hides runnability, ensures singleton-ness */
	private Runnable mRunner;
	private static PersistentThread sThread;
	
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
		mBaos = new ByteArrayOutputStream();
		mDataOut = mydata;
	}
	
	/**
	 * Transcodes a YUV 4:2:0 SP frame (delivered by the camera preview) to bitmap
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
	
	/**
	 * Obtains the thread that runs the telemetry transmission routines.
	 * On first call to this method, the PersistentThread is created.
	 * But since two or more instances of TransmitPicture should not be run concurrently,
	 * subsequent calls to this method return only that first thread.
	 * @return The PersistentThread that runs TransmitPicture.
	 */
	public PersistentThread getPersistentThreadInstance() {
		final TransmitPicture mTp = this;
		if (mRunner == null) {
			mRunner = new Runnable() {
				/**
				 * Runs the thread.
				 */
				public void run()
				{
					Looper.prepare();
					
					Thread.currentThread().setName("TransmitPicture");
					mHandler = new Handler() {
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
					if (mDataOut == null) //For debugging only; should not happen
						System.out.println("Null dataout");
					
					mHandler.sendEmptyMessageDelayed(SEND_PIC, CAMERA_INTERVAL);//Send first picture, after giving the camera time to warm up.
					Looper.loop();
				}
			};
		}
		if (sThread == null) {
			sThread = new PersistentThread(mRunner);
		}
		return sThread;
	}
	/**
	 * Obtains the JPEG compression quality of each transmitted frame.
	 * @return The quality.
	 */
	public int getPreviewQuality() {
		return mPrevQuality.get();
	}
	
	/**
	 * Processes a message.
	 * @param msg The message to receive.
	 * @param source The source of the message, if a reply is needed.  May be null.
	 */
	public void receiveMessage(String msg, Receivable source) {
		String[] parts = msg.split(":");
		if (parts[0].equals("IMAGE")) {
			if (parts[1].equals("RECEIVED")) {
				mHandler.sendEmptyMessage(SEND_PIC);
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
	 * Sets the telemetry output stream.
	 * @param newDataOut The new output stream.
	 */
	public void setOutputStream(ObjectOutputStream newDataOut) {
		synchronized (mDataOut) {
			mDataOut = newDataOut;
		}
	}
	
	/**
	 * Sets the JPEG compression quality of each transmitted frame.
	 * @param newQ The new quality.
	 */
	public void setPreviewQuality(int newQ) {
		mPrevQuality.set(newQ);
	}
	
	/**
	 * Encodes a YUV picture frame into a JPEG array.
	 * @param picFrame The YUV frame to encode.
	 * @param frameSize The size of the frame (index 0, width: index 1, height)
	 * @return The encoded JPEG array.
	 */
	private byte[] encodePic(byte[] picFrame, int[] frameSize) {
		
		long starttime = System.currentTimeMillis();
		
		if (NEWCOMPRESSMETHOD)
		{
			YuvImage sourcePic = null;
			try {
				
				sourcePic = new YuvImage(picFrame, myMakePic.getPreviewFormat(), frameSize[0], frameSize[1], null);
			}
			catch (Throwable t)
			{
				t.printStackTrace();
			}
			
			//System.out.println(MakePicture.XPREV + ", " + MakePicture.YPREV + "; next: " + MakePicture.nextx + ", " + MakePicture.nexty);
		
			System.out.println("Compressing to jpeg");
			try {
				sourcePic.compressToJpeg(new Rect(0, 0, frameSize[0], frameSize[1]), mPrevQuality.get(), mBaos);
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
			decodeYUV420SP(rgb, picFrame, frameSize[0], frameSize[1]);
			//System.out.println(MakePicture.XPREV + ", " + MakePicture.YPREV + "; next: " + MakePicture.nextx + ", " + MakePicture.nexty);
			Bitmap mBitMap = Bitmap.createBitmap(rgb, frameSize[0], frameSize[1], Bitmap.Config.RGB_565);
			mBitMap.compress(Bitmap.CompressFormat.JPEG, mPrevQuality.get(), mBaos);
		}
		if (mBaos == null)
			System.out.println("bad stream");
		byte[] sendPic = mBaos.toByteArray();
		mBaos.reset();
		long endtime = System.currentTimeMillis();
    	Log.v(TAG, "Pic Processing took " + (endtime - starttime));
    	return sendPic;
	}
	
	/**
	 * Transmits a frame of telemetry.
	 * @throws IOException If the connection fails
	 */
	private void transmit() throws IOException {
		if (!myMakePic.isFrameNew()) {
			mHandler.sendEmptyMessageDelayed(SEND_PIC, CAMERA_INTERVAL); //wait a bit, try again later.
			System.out.println("Same pic");
			return;
		}
		int[] frameSize = myMakePic.getFrameSize();
		byte[] picFrame = myMakePic.getBufferCopy(); //create a new copy.
		byte[] temppic = encodePic(picFrame, frameSize);
		myMakePic.setFrameNewnessTo(false);
		
		//System.out.println("Retrieved frame");
		if (picFrame.length == 0)
		{
			System.out.println("temppic unprocessed");
			mHandler.sendEmptyMessageDelayed(SEND_PIC, Comm.CONNECTION_INTERVAL); //wait a bit, try again later.
			return;
		}
		Log.i(TAG, "Sending a pic, length " + temppic.length);
		//Notifies the control console that the next transmission will be an image.
		//i.e. not a text-based one.
		String PicPrep = "IMAGE:" + Integer.toString(temppic.length) + ":" + System.currentTimeMillis();
		mComm.sendMessage(PicPrep);
		try	{
			//sends the picture
			if (mDataOut != null) {
				synchronized (mDataOut) {
					mDataOut.write(temppic);
					mDataOut.flush();
				}
			}
		}
		catch (Throwable t) {
			Log.w(TAG, "Exception thrown in telemetry transmission.");
			t.printStackTrace();
			mHandler.sendEmptyMessageDelayed(SEND_PIC, Comm.CONNECTION_INTERVAL); //wait a bit, try again later.
		}
		
		if (myMakePic.updateFrameSize())
			System.out.println("Picture updated succesfully.");
		else
			mComm.sendMessage("IMAGE:REQUEST:DENIED");
		//System.out.println("Pic sent, ms: " + (System.currentTimeMillis() - endtime));
	}
}
