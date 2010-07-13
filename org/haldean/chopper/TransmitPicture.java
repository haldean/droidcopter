package org.haldean.chopper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

public final class TransmitPicture extends Thread implements Constants
{
	private static ObjectOutputStream dataout;
	private static byte[] sendpic;
	private static ByteArrayOutputStream baos;
	public static Handler mHandler;
	public static int PREVQUALITY = INITIALPREVQ;
	private static boolean NEWCOMPRESSMETHOD = false;
	
	public TransmitPicture(ObjectOutputStream mydata)
	{
		super("Transmit Telemetry");
		baos = new ByteArrayOutputStream();
		dataout = mydata;
	}
	
	public void run()
	{
		Looper.prepare();
		
		mHandler = new Handler() {
            public void handleMessage(Message msg)
            {
                switch (msg.what) {
                case SENDAPIC:
                	try	{
                		transmit();
                	}
                	catch (IOException e) {
                		System.out.println("Connection failed, reconnecting in " + CONNECTIONINTERVAL);
                		//Does not actually try to reconnect; it is assumed that chopperStatus will also fail and give the command to reconnect.
                		e.printStackTrace();
                	}
                	break;
                }
            }
        };
		
		if (dataout == null) //For debugging only; should not happen
			System.out.println("Null dataout");
		System.out.println("TransmitPicture run() thread ID " + getId()); //debugging
		
		mHandler.sendEmptyMessageDelayed(SENDAPIC, CAMERAINTERVAL);//Send first picture, after giving the camera time to warm up.
		Looper.loop();
	}
	
	//Kills the transmit picture thread, so it can be restarted
	public static void stopLoop() {
		if (mHandler == null)
			return;
		mHandler.getLooper().quit();
	}
	
	private static void transmit() throws IOException {
		if (!MakePicture.newFrame) {
			mHandler.sendEmptyMessageDelayed(SENDAPIC, CAMERAINTERVAL); //wait a bit, try again later.
			System.out.println("Same pic");
			return;
		}
		long starttime = System.currentTimeMillis();
		synchronized (MakePicture.buffer) //get a lock on the variable
		{
			sendpic = MakePicture.buffer.clone(); //create a new copy.
		//	System.out.println("Copytime: " + (System.currentTimeMillis() - starttime));
		}
		
		MakePicture.newFrame = false;
		
		//System.out.println("Retrieved frame");
		if (sendpic.length == 0)
		{
			System.out.println("temppic unprocessed");
			mHandler.sendEmptyMessageDelayed(SENDAPIC, CONNECTIONINTERVAL); //wait a bit, try again later.
			return;
		}
		
		if (MakePicture.nextx != MakePicture.XPREV) {//picture parameters need updating
			if (MakePicture.updateFrameSize())
				System.out.println("Picture updated succesfully.");
			else
				Comm.sendMessage("IMAGE:REQUEST:DENIED");
		}

		if (NEWCOMPRESSMETHOD)
		{
			YuvImage sourcePic = null;
			try {
				sourcePic = new YuvImage(sendpic, MakePicture.PREVFORMAT, XPIC, YPIC, null);
			}
			catch (Throwable t)
			{
				t.printStackTrace();
			}
			
			//System.out.println(MakePicture.XPREV + ", " + MakePicture.YPREV + "; next: " + MakePicture.nextx + ", " + MakePicture.nexty);
		
			System.out.println("Compressing to jpeg");
			try {
				sourcePic.compressToJpeg(new Rect(0, 0, XPIC, YPIC), PREVQUALITY, baos);
			}
			catch (Throwable t) {
				System.out.println("Compress fail");
				t.printStackTrace();
			}
			System.out.println("Finished compressing");
		}
		else {
			System.out.println("Bitmap compression");
			Bitmap mBitMap;
			int[] rgb = new int[MakePicture.XPREV * MakePicture.YPREV];
			decodeYUV420SP(rgb, sendpic, MakePicture.XPREV, MakePicture.YPREV);
			//System.out.println(MakePicture.XPREV + ", " + MakePicture.YPREV + "; next: " + MakePicture.nextx + ", " + MakePicture.nexty);
			mBitMap = Bitmap.createBitmap(rgb, MakePicture.XPREV, MakePicture.YPREV, Bitmap.Config.RGB_565);
			if (mBitMap == null) {
				//System.out.println("Bitmap decode failed");
				return;
			}
			mBitMap.compress(Bitmap.CompressFormat.JPEG, PREVQUALITY, baos);
		}
		if (baos == null)
			System.out.println("bad stream");
		byte[] temppic = baos.toByteArray();
		baos.reset();
		long endtime = System.currentTimeMillis();
    	System.out.println("Pic Processing took " + (endtime - starttime));
		System.out.println("Sending a pic, length " + temppic.length);
		//Notifies the control console that the next transmission will be an image.
		//i.e. not a text-based one.
		String PicPrep = "IMAGE:" + Integer.toString(temppic.length) + ":" + System.currentTimeMillis();
		Comm.sendMessage(PicPrep);
		try	{
			//sends the picture
			dataout.write(temppic);
			dataout.flush();
		}
		catch (Throwable t) {
			t.printStackTrace();
			System.out.println("TransmitPic throwing exception");
			mHandler.sendEmptyMessageDelayed(SENDAPIC, CONNECTIONINTERVAL); //wait a bit, try again later.
		}
		System.out.println("Pic sent, ms: " + (System.currentTimeMillis() - endtime));
	}
	
	private static void decodeYUV420SP(int[] rgb, byte[] yuv420sp, int width, int height) {

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
