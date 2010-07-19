package org.haldean.chopper.nav;

import junit.framework.TestCase;

class NavDestTest extends TestCase {
	private NavDest[] tasks = new NavDest[3];
	
	protected void setUp() throws Exception {
		super.setUp();
		
	}

	public void testNavDestString() {
		tasks[0] = new NavDest("DEST:300:40.78:-73.97:2:10");
		tasks[1] = new NavDest("DEST:120:38.98:-77.07:1.8:15");
		tasks[2] = new NavDest("DEST:1000:32.72:-117.15:2.34:3");
	}

	public void testToString() {
		testNavDestString();
		
		for (int i = 0; i < 3; i++) {
			if (tasks[i] == null)
				System.out.println("null task");
			if (tasks[i].toString() == null)
				System.out.println("null string");
			else
				System.out.println(tasks[i].toString());
		}
	}

}
