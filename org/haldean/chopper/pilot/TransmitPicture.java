package org.haldean.chopper.pilot;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.concurrent.atomic.AtomicInteger;

import org.haldean.blob.Normalizer;

import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

/**
 * Transmits telemetry frames to the control server. <P>
 * 
 * May send the following messages to registered Receivables:<br>
 * <pre>
 * IMAGE:
 *       REQUEST:DENIED (as a reply to an invalid IMAGE:SET:QUALITY:&lt;quality&gt; request)
 *       FRAMEQUALITY:&lt;quality&gt; (as a reply to IMAGE:GETPARAMS)
 * </pre>
 * 
 * May receive the following messages from Chopper components:
 * <pre>
 * IMAGE:
 *       RECEIVED
 *       SET:QUALITY:&lt;quality&gt;
 *       GETPARAMS
 * </pre>
 * 
 * @author Benjamin Bardin
 */
public final class TransmitPicture implements Runnable, Receivable, Constants
{	
	/**
	 * Handles thread scheduling, instructions from other threads
	 */
	public Handler mHandler;
	
	/** Quality of a compressed preview frame.  Minimum is 0, maximum is 100, default is 25. */
	private final AtomicInteger mPrevQuality = new AtomicInteger(25);
	
	/** How long (in ms) TransmitPicture should wait, if no new preview frame is available for transmission,
	 * before trying again. */
	private static final int CAMERA_INTERVAL = 200;
	
	/** Output stream */
	private ObjectOutputStream mDataOut;
	
	/** For local JPEG compression */
	private ByteArrayOutputStream mBaos;
	
	private byte[] mPicFrame; 
	
	/** Tag for logging */
	private static final String TAG = "chopper.TransmitPicture";
	
	/** Handle to other chopper components */
	private MakePicture myMakePic;
	private Comm mComm;
	
	private Normalizer norm;
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
		norm = new Normalizer(Normalizer.red);
	}
	
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
        mComm.registerReceiver(IMAGE, this);
		if (mDataOut == null) //For debugging only; should not happen
			System.out.println("Null dataout");
		
		mHandler.sendEmptyMessageDelayed(SEND_PIC, CAMERA_INTERVAL);//Send first picture, after giving the camera time to warm up.
		Looper.loop();
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
					if ((newq <= 100) && (newq > 0)) {
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
		
		//long starttime = System.currentTimeMillis();
		
		YuvImage sourcePic = null;
		norm.normalizeYuv(picFrame, picFrame);
		try {
			sourcePic = new YuvImage(picFrame, myMakePic.getPreviewFormat(), frameSize[0], frameSize[1], null);
		}
		catch (Throwable t)
		{
			t.printStackTrace();
		}
		
		//System.out.println(MakePicture.XPREV + ", " + MakePicture.YPREV + "; next: " + MakePicture.nextx + ", " + MakePicture.nexty);
	
		//System.out.println("Compressing to jpeg");
		try {
			sourcePic.compressToJpeg(new Rect(0, 0, frameSize[0], frameSize[1]), mPrevQuality.get(), mBaos);
		}
		catch (Throwable t) {
			Log.e(TAG,"Compress fail");
			t.printStackTrace();
		}
		//System.out.println("Finished compressing");
		
		if (mBaos == null)
			System.out.println("bad stream");
		byte[] sendPic = mBaos.toByteArray();
		mBaos.reset();
		//long endtime = System.currentTimeMillis();
    	//Log.v(TAG, "Pic Processing took " + (endtime - starttime));
    	return sendPic;
	}
	
	/**
	 * Transmits a frame of telemetry.
	 * @throws IOException If the connection fails
	 */
	private void transmit() throws IOException {
		if (!myMakePic.isFrameNew()) {
			mHandler.sendEmptyMessageDelayed(SEND_PIC, CAMERA_INTERVAL); //wait a bit, try again later.
			//System.out.println("Same pic");
			return;
		}
		int[] frameSize = myMakePic.getFrameSize();
		if ((mPicFrame == null) || (mPicFrame.length != myMakePic.getFrameArrayLength()))
				mPicFrame = new byte[myMakePic.getFrameArrayLength()];
		myMakePic.getBufferCopy(mPicFrame); //create a new copy.
		byte[] temppic = encodePic(mPicFrame, frameSize);
		myMakePic.setFrameNewnessTo(false);
		
		//System.out.println("Retrieved frame");
		if (mPicFrame.length == 0)
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
