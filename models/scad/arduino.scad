use <hardware.scad>

module arduino(with_screws=0) {
	module screws() {
		module screw(h=16) {
				union() {
						m2(h);
						translate(v=[0,0,h]) rotate(v=[1,0,0],a=180) m2(h);
				}
		}
		translate(v=[14,49.5,-12]) screw();
		translate(v=[65,34.5,-12]) screw();
		translate(v=[65,6.5,-12]) screw();
	}

	difference() {	
		union() {
			color([0,0.4,0.6,1]) cube([69,53.5,2]);

			// USB Port
			color([0.8,0.8,0.8]) translate(v=[-6.5,32.5,2]) cube([16.5,12.5,10.8]);

			// Power / Analog headers
			color([0,0,0]) translate(v=[31,1,0]) cube([33.5, 3, 10.2]);

			// Digital IO headers
			color([0,0,0]) translate(v=[22,49.5,0]) cube([43.5, 3, 10.2]);

			// IC and socket
			color([0,0,0]) translate(v=[28,11.4,0]) cube([35.5,10,9]);

			// ICSP header
			color([0,0,0]) translate(v=[63,24,0]) cube([5,7.5,10.5]);

			// DC barrel jack
			color([0,0,0]) translate(v=[-2,3,2]) cube([14.25,9.25,11]);

			// Reset button
			color([0.9,0.9,0.9]) translate(v=[52,24,0]) cube(6);

			if (with_screws == 1) screws();
		}

		if (with_screws == 0) screws();
	}
}

arduino(1);