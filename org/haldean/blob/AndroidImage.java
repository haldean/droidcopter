package org.haldean.blob;

public class AndroidImage implements Image {
	private int[][] mData;
	private int width;
	private int height;
	
	public void updateImage(byte[] yuvData, int w, int h) {
		width = w;
		height = h;
		
		if (mData.length != yuvData.length / 2)
			mData = new int[yuvData.length / 2][3];
		
		for (int i = 0; i < mData.length; i += 2) {
			yuvToRgb(mData[i], yuvData[2 * i], yuvData[2 * i + 1], yuvData[2 * i + 3]);
			yuvToRgb(mData[i + 1], yuvData[2 * i + 2], yuvData[2 * i + 1], yuvData[2 * i + 3]);
		}
		
	}
	
	public int[] getSize() {
		return new int[] {width, height};
	}
	
	public int[] getPixel(int x, int y) {
		return mData[y * width + x];
	}
	
	private void yuvToRgb(int[] target, int y, int u, int v) {
		target[0] = (int) (y + 1.13983 * v) + 128;
		target[1] = (int) (y - .39465 * u -.58060 * v) + 128;
		target[2] = (int) (y + 2.03211 * u) + 128;
	}
}
