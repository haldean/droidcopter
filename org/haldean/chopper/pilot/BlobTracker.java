package org.haldean.chopper.pilot;

import org.haldean.blob.Segmenter;
import org.haldean.blob.Image;

public final class BlobTracker implements Runnable, Receivable {
    Image image;
    Segmenter segmenter;
    int[] lastLocation;
    int[] lastVector;

    private static final int TRACKING_PERIOD_MS = 200;

    public BlobTracker() {
	lastLocation = new int[2];
	lastVector = new int[3];
	segmenter = null;
	image = null;
    }

    public void receiveMessage(String msg, Receivable source) {
	if (msg.startsWith("SEGMENT")) {
	    segmenter = Segmenter.fromString(msg);
	}
    }

    public int[] getVector() {
	return null;//Arrays.copyOf(lastVector, lastVector.length);
    }

    private void calculateVector() {
	if (segmenter == null || image == null) return;
	synchronized (image) {
	    int[] imageSize = image.getSize();
	    lastLocation = segmenter.segment(image);

	    synchronized (lastVector) {
		lastVector[0] = lastLocation[0] - imageSize[0] / 2;
		lastVector[1] = lastLocation[1] - imageSize[1] / 2;
	    }
	}
    }
    
    public void run() {
	//image = getImage();
	calculateVector();
    }
}