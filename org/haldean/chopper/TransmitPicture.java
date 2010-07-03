package org.haldean.chopper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

public final class TransmitPicture extends Thread implements Constants
{
	private static ObjectOutputStream dataout;
	private static byte[] sendpic;
	private static byte[] lastpic = new byte[0];
	private static ByteArrayOutputStream baos;
	public static Handler mHandler;
	
	public TransmitPicture(ObjectOutputStream mydata)
	{
		super();
		baos = new ByteArrayOutputStream();
		setName("TransmitPicture");
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
                	try
                	{
                		transmit();
                	}
                	catch (IOException e)
                	{
                		System.out.println("Connection failed, reconnecting in " + CONNECTIONINTERVAL);
                		e.printStackTrace();
                	}
                	break;
                }
            }
        };
		
		if (dataout == null)
			System.out.println("Null dataout");
		System.out.println("TransmitPicture run() thread ID " + getId()); //debugging
		
		mHandler.sendEmptyMessageDelayed(SENDAPIC, CAMERAINTERVAL);
		Looper.loop();
	}
	
	public static void stopLoop() {
		if (mHandler == null)
			return;
		mHandler.getLooper().quit();
	}
	
	private static void transmit() throws IOException
	{
		synchronized (MakePicture.buffer) //get a lock on the variable
		{
			sendpic = MakePicture.buffer; //create a new reference in case buffer is changed by another thread.
		}
		if (sendpic.length == 0)
		{
			System.out.println("temppic unprocessed");
			mHandler.sendEmptyMessageDelayed(SENDAPIC, CONNECTIONINTERVAL); //wait a bit, try again later.
			return;
		}
		
		if (sendpic.length == 0)
			System.out.println("FUCK");
		else
			System.out.println(sendpic.length);
		if (lastpic != sendpic) //if the buffer is not still the last picture sent, and sendpic isn't null
		{
			long starttime = System.currentTimeMillis();
			int[] rgb = new int[XPREV * YPREV];
			decodeYUV420SP(rgb, sendpic, XPREV, YPREV);
			Bitmap mBitMap = Bitmap.createBitmap(rgb, XPREV, YPREV, Bitmap.Config.RGB_565);
			if (mBitMap == null) {
				System.out.println("Bitmap decode failed");
				return;
			}
			if (baos == null)
				System.out.println("bad stream");
			mBitMap.compress(Bitmap.CompressFormat.JPEG, JPEGQUALITY, baos);
			byte[] temppic = baos.toByteArray();
			baos.reset();
			long endtime = System.currentTimeMillis();
	    	Comm.sendMessage("Pic Processing took " + (endtime - starttime));
			System.out.println("Sending a pic, length " + temppic.length);
			//Notifies the control console that the next transmission will be an image.
			//COMMUNICATION PROTOCOL: startsWith("PREPARE.") indicates to prep for a special transmission,
			//i.e. not a text-based one.
			String PicPrep = "IMAGE:" + Integer.toString(temppic.length) + ":" + System.currentTimeMillis();
			Comm.sendMessage(PicPrep);
			try
			{
				//sends the picture
				dataout.write(temppic);
				dataout.flush();
				
				//stores the sent picture, to compare it to succeeding images and make sure the same image isn't sent twice (wasteful)
				lastpic = sendpic;
			}
			catch (IOException e)
			{
				e.printStackTrace();
				mHandler.sendEmptyMessageDelayed(SENDAPIC, CONNECTIONINTERVAL); //wait a bit, try again later.
			}
		}
		else //if the buffer is null or was identical to the last frame sent--need to wait a bit for the camera.
		{
			mHandler.sendEmptyMessageDelayed(SENDAPIC, CONNECTIONINTERVAL); //wait a bit, try again later.
		}
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
