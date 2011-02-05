package org.haldean.chopper.pilot.nav;

import junit.framework.TestCase;

class NavListTest extends TestCase {
	private static NavTask[] dest;
	private static String myString;
	
	protected void setUp() throws Exception {
		super.setUp();
	}

	public void testToString() {
		dest = new NavTask[5];
		dest[0] = new NavDest("DEST!300!40.78!-73.97!2!10", null);
		dest[1] = new NavVel("VEL!1!2!3!4!20");
		dest[2] = new NavDest("DEST!120!38.98!-77.07!1.8!15", null);
		dest[3] = new NavDest("DEST!1000!32.72!-117.15!2.34!3", null);
		dest[4] = new NavVel("VEL!5!6!8.8!7!300");
		
		NavList list1 = new NavList();
		list1.add(dest[0]);
		list1.add(dest[1]);
		NavList list2 = new NavList();
		list2.add(dest[2]);
		list2.add(dest[3]);
		NavList list3 = new NavList();
		list3.add(list1);
		list3.add(list2);
		list3.add(dest[4]);
		
		myString = list3.toString();
		System.out.println(list3.toString());
		System.out.println("HI");
	}

	public void testFromString() {
		
		NavList myList = NavList.fromString(myString, null);
		System.out.println(myList.toString());
	}

}
