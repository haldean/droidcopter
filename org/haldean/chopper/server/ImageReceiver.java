package org.haldean.chopper.server;

import java.io.*;
import java.net.*;

/** A thread to receive an image from an ObjectInputStream
 *  and send that image to an ImageComponent.
 *  @author William Brown
 *  @author Benjamin Bardin */
public class ImageReceiver implements Runnable {
    private ObjectInputStream ostream;
    private int len;
    private long time;
    private ImagePanel imageComp;
    private Callback callback;
    
    /** Create a new ImageReceiver thread.
     *  @param _in The ObjectInputStream to read the image from
     *  @param _header The incoming image message containing the length and capture time
     *  @param _imageComp The ImageComponent to send the image to after receipt
     *  @param _callback The callback to call once the image has been received 
     *  @throws IllegalArgumentException when the supplied header is not a valid image receipt */
    public ImageReceiver(ObjectInputStream _in, String _header, 
			 ImagePanel _imageComp, Callback _callback) 
	throws IllegalArgumentException {

	super();

	/* Image headers are of the form "IMAGE:LENGTH_IN_BYTES:CAPTURE_EPOCH_TIME" */
	String fields[] = _header.split(":");
	try {
	    len = new Integer(fields[1]);
	    time = new Long(fields[2]);
	} catch (Exception e) {
	    throw new IllegalArgumentException("Not an image receipt");
	}

	imageComp = _imageComp;
	ostream = _in;
	callback = _callback;
    }

    /** Start the image receipt. */
    public void run() {
	/* Just because it's good to know */
	Debug.log("Receiving image length " + len);
	/* We read the image into this array */
	byte[] imageData = new byte[len];

	try {
	    /* Read in the image from the socket */
	    ostream.readFully(imageData);

	    /* Set the image and the capture time of the component */
	    imageComp.setImage(imageData);

	    /* Call the callback once that reading is completed */
	    callback.completed();
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}