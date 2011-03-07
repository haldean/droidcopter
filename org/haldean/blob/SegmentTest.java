package org.haldean.blob;

import java.awt.image.BufferedImage;

public class SegmentTest {
    public static void main(String args[]) {
	byte[] targetColor = new byte[] {(byte) 0xCA, (byte) 0x3B, (byte) 0x11};
	Segmenter seg = new Segmenter(targetColor, 0, 20);
	JavaImage img = null;

	try {
	    img = new JavaImage("/home/haldean/Downloads/input2.jpg");
	} catch (Exception e) {
	    e.printStackTrace();
	    return;
	}

	int[] size = img.getSize();
	seg.output = new BufferedImage(size[0], size[1], img.image.getType());

	int[] xy = seg.segment(img);
	System.out.println(xy[0]);
	System.out.println(xy[1]);
    }
}