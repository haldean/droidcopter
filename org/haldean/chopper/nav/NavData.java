package org.haldean.chopper.nav;

public abstract class NavData {
	protected double[] mData;
	protected String name;
	protected String type;
	
	public String toString() {
		String str = type + "!" + name;
		for (double d : mData) {
			str = str + "!" + d;
		}
		return str;
	}
	
	public NavData() {
	}
	
	public double[] getData() {
		return mData.clone();
	}
		
	public static NavData fromString(String str) {
		//System.out.println("\'" + str + "\'");
		String[] params = str.split("!");
		String type = params[0];
		NavData mNav = null;
		
		if (str.startsWith("{")) {
			mNav = NavList.fromString(str);
			return mNav;
		}
		
		if (type.equals("VEL")) {
			mNav = new NavVel();
		} else if (type.equals("DEST")) {
			mNav = new NavDest();
		} else if (type.equals("TRACK")) {
		    mNav = new NavTrack();
		}
        
		mNav.type = type;
		mNav.name = params[1];
		int offset = 2;
		mNav.mData = new double[params.length - offset];
		for (int i = 0; i < mNav.mData.length; i++) {
			mNav.mData[i] = new Double(params[i + offset]);
		}
		return mNav;
	}
	
	public abstract double getID();
    
	public String getName() {
		return name;
	}
}