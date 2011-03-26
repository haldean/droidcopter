package org.haldean.blob;

public interface Image {
    int[] getPixel(int i, int j);
    int[] getSize();
    void updateImage(byte[] data, int width, int height);
}