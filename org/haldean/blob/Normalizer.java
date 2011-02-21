package org.haldean.blob;


public class Normalizer {
	public static final int[] black = {-128, -128, -128};
	public static final int[] white = {128, -128, -128};
	public static final int[] red = {38, -19, 78};
	public static final int[] green = {75, -37, -66};
	public static final String TAG = "Normalizer";
	int[] normal;
	
	public Normalizer() {
		this(black);		
	}
	
	public Normalizer(int[] target) {
		super();
		if (target.length == 3) {
			normal = target.clone();
		}
		else {
			normal = black.clone();
		}
	}
	public void normalize(byte[] input, short[] output) {
		if (input.length != 2 * output.length) {
			return;
		}
		for (int i = 0; i < output.length; i += 2) {
			int j = 2 * i;
			int chroma = Math.abs(input[j + 1] - normal[1]) +
						 Math.abs(input[j + 3] - normal[2]);
			
			output[i    ] = (short) (chroma + Math.abs(input[j    ] - normal[0]));
			output[i + 1] = (short) (chroma + Math.abs(input[j + 2] - normal[0]));
		}
	}
	
	public void normalizeYuv(byte[] input, byte[] output) {
		if (input.length != output.length) {
			return;
		}
		for (int i = 0; i < output.length; i += 4) {
			int chroma = Math.abs(input[i + 1] - normal[1]) +
						 Math.abs(input[i + 3] - normal[2]);
			//Log.v(TAG, Integer.toString((chroma + Math.abs(input[i    ] - normal[0])) / 3));
			output[i    ] = (byte) (((chroma + Math.abs(input[i    ] - normal[0])) / 3) - 127);
			output[i + 1] = -128;
			output[i + 2] = (byte) (((chroma + Math.abs(input[i + 2] - normal[0])) / 3) - 127);
			output[i + 3] = -128;
		}
	}
	
	public void setNormal(byte y, byte u, byte v) {
		normal = new int[3];
		normal[0] = y;
		normal[1] = u;
		normal[2] = v;
	}
}
