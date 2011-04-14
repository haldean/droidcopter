package org.haldean.blob;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class JavaImage implements Image {
    BufferedImage image;

    public JavaImage(String path) throws IOException {
	image = ImageIO.read(new File(path));
    }

    public JavaImage(BufferedImage image) {
	this.image = image;
    }

    public BufferedImage getImage() {
	return image;
    }

    public int[] getPixel(int i, int j) {
	return rgbIntToTriple(image.getRGB(i, j));
    }

    public int[] getSize() {
	return new int[] {image.getWidth(), image.getHeight()};
    }

    public static int[] rgbIntToTriple(int rgb) {
	return new int[] {rgb >> 16 & 0xFF, 
			  rgb >> 8 & 0xFF, 
			  rgb & 0xFF};
    }

    public void updateImage(byte[] data, int width, int height) {
	/* Unimplemented. */
    }
}