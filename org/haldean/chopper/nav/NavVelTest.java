package org.haldean.chopper.nav;

import junit.framework.TestCase;

class NavVelTest extends TestCase {
	private static NavVel[] vels = new NavVel[3];
	public void testNavVel() {
		vels[0] = new NavVel("VEL:1:2:3:4:20");
		vels[1] = new NavVel("VEL:5:6:8.8:7:300");
		vels[2] = new NavVel("VEL:23.2:21:-2:45:15");
	}

	public void testToString() {
		testNavVel();
		for (int i = 0; i < 3; i++)
			System.out.println(vels[i].toString());
	}

}
