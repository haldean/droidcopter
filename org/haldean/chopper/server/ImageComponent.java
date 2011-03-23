package org.haldean.chopper.server;

import org.haldean.blob.JavaImage;
import org.haldean.blob.Segmenter;

import java.io.*;
import java.util.LinkedList;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.awt.geom.*;
import javax.swing.*;
import javax.imageio.*;

/** A component to display images 
 *  @author William Brown
 *  @author Benjamin Bardin */
public class ImageComponent extends JComponent {
    private final Color background = StyleProvider.background();
    private final Color labelColor = StyleProvider.foreground();
    private final Font labelFont = StyleProvider.fontSmall();

    private JavaImage img;
    private AffineTransform transform;

    private long lastCaptureTime;
    private double framerate;

    private int frameCount;
    private final int averageSamples = 30;
    private LinkedList<Double> frameRates;
    private double frameRateSum;

    Segmenter segmenter;
    int[] loc;
    float scale = 1;

    /** Create an empty ImageComponent */
    public ImageComponent() {
	transform = AffineTransform.getScaleInstance(1, 1);
	img = null;

	frameCount = 0;
	frameRateSum = 0;
	frameRates = new LinkedList<Double>();

	addMouseListener(new MouseAdapter() {
		public void mouseClicked(MouseEvent e) {
		    segmentImage(e.getX(), e.getY());
		}
	    });
    }

    /** For TabPanes */
    public String getName() {
	return "Telemetry";
    }

    private void segmentImage(int x, int y) {
	if (img != null) {
	    Dimension size = getSize();
	    int[] imageSize = img.getSize();

	    x /= scale;
	    y /= scale;

	    segmenter = Segmenter.getSegmenterForPoint(img, x, y);
	    loc = segmenter.segment(img);

	    repaint();
	}
    }	

    /** Set the image displayed by the component 
     *  @param _imgData A JPEG-encoded image in a byte array */
    public void setImage(BufferedImage newImage) {
	try {
	    /* Make sure it isn't the same image we already have. */
	    if (newImage != null && (img == null || img.getImage() != newImage)) {
		/* Rotate the image one quadrant clockwise. */
		AffineTransform rotateTransform = AffineTransform
		    .getQuadrantRotateInstance(1, newImage.getWidth() / 2, 
					       newImage.getHeight() / 2);
		AffineTransformOp rotateOperation = 
		    new AffineTransformOp(rotateTransform, AffineTransformOp.TYPE_BILINEAR);
		BufferedImage bufferedImage = rotateOperation.filter(newImage, null);
		img = new JavaImage(bufferedImage);

		if (frameCount > 0) {
		    framerate = 1.0 / 
			((System.currentTimeMillis() - lastCaptureTime) / 1000.0);

		    frameRateSum += framerate;
		    frameRates.add(new Double(framerate));

		    if (frameRates.size() > averageSamples)
			frameRateSum -= frameRates.removeFirst();
		}

		frameCount++;
		lastCaptureTime = System.currentTimeMillis();
	    }
	} catch (Exception e) {
	    img = null;
	    e.printStackTrace();
	} finally {
	    repaint();
	}
    }

    /** Paint the image and metadata onto the canvas */
    public void paintComponent(Graphics g) {
	Graphics2D g2 = (Graphics2D) g;

	int width = (int) getSize().getWidth();
	int height = (int) getSize().getHeight();
	g2.setColor(background);
	g2.fillRect(0, 0, width, height);

	g2.setColor(labelColor);
	g2.setFont(labelFont);

	if (img != null) {
	    /* Calculate the appropriate transform to fit the image to
	     * the canvas. */
	    int[] imageSize = img.getSize();
	    scale = Math.min((float) width / (float) imageSize[0],
			     (float) height / (float) imageSize[1]);

	    /* Create a new square affine transform for that scaling */
	    transform = AffineTransform.getScaleInstance(scale, scale);

	    g2.drawImage(img.getImage(), transform, null);
	
	    /* Draw the image resolution to the component */
	    g2.drawString("Size: " + imageSize[0] + "x" + 
			  (int) imageSize[1], 1, height - 10);
	    /* Draw the capture time to the component */
	    g2.drawString("Instantaneous Framerate: " + framerate + 
			  " fps", 1, height - 22);
	    g2.drawString("Average Framerate: " + 
			  (frameRateSum / frameRates.size()), 1, height - 34);

	    if (segmenter != null) {
		loc = segmenter.segment(img);
	    }

	    if (loc != null) {
		g2.setColor(Color.GREEN);
		g2.fillRect((int) (loc[1] * scale) - 2, (int) (loc[0] * scale) - 2, 5, 5);
	    }
	}
	
    }
}