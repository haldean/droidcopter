package org.haldean.chopper.server;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import javax.imageio.*;

/** 
 * A thread to receive an image from an ObjectInputStream
 * and send that image to an ImageComponent.
 *
 * @author William Brown
 * @author Benjamin Bardin
 */
public class ImageReceiver implements Runnable {
  private static String imageStoreDirectory;
  static {
    try {
      imageStoreDirectory = ServerCreator.getArgument("imgdir");
    } catch (IllegalArgumentException e) {
      imageStoreDirectory = null;
    }
  }

  private ObjectInputStream ostream;
  private int len;
  private long time;
  private ImagePanel imageComp;
  private Callback callback;
    
  /** 
   * Create a new ImageReceiver thread.
   *
   * @param in The ObjectInputStream to read the image from
   * @param header The incoming image message 
   * @param imageComp The ImageComponent to send the image to 
   * @param callback The callback to call after receipt
   * @throws IllegalArgumentException when the supplied header is not
   * a valid image receipt
   */
  public ImageReceiver(ObjectInputStream in, String header, 
		       ImagePanel imageComp, Callback callback) 
    throws IllegalArgumentException {
    super();

    /* Image headers are of the form "IMAGE:LENGTH_IN_BYTES:CAPTURE_EPOCH_TIME" */
    String fields[] = header.split(":");
    try {
      len = new Integer(fields[1]);
      time = new Long(fields[2]);
    } catch (Exception e) {
      throw new IllegalArgumentException("Not an image receipt");
    }

    this.imageComp = imageComp;
    this.ostream = in;
    this.callback = callback;
  }

  /**
   * Start the image receipt.
   */
  public void run() {
    /* Just because it's good to know */
    Debug.log("Receiving image length " + len);
    /* We read the image into this array */
    byte[] imageData = new byte[len];

    try {
      /* Read in the image from the socket */
      ostream.readFully(imageData);
      BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
	
      /* Set the image and the capture time of the component */
      imageComp.setImage(image);

      /* Save the image to disk */
      if (imageStoreDirectory != null) {
	ImageIO.write(image, "JPEG", new File(imageStoreDirectory + time + ".jpg"));
      }

      /* Call the callback once that reading is completed */
      callback.completed();
    } catch (Exception e) {
      Debug.log("imageData exception, thread ID " + Thread.currentThread().getId());
      e.printStackTrace();
    }
  }
}