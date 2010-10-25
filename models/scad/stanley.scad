use <hardware.scad>

module stanley(solid_screw = false) {
	motor_r = 20;
	stem_r = 7;
	retainer_r = 12;
	motor_disk_height = 6.3;
	union() {
		difference() {
			cylinder(r=motor_r, h=motor_disk_height);
			for (i = [0:2]) {
				rotate(v=[0,0,1], a = i * 120)
					translate(v=[2 * motor_r / 3, 0, 0])
						m3();
			}
		}
	
		difference() {
			translate(v=[0, 0, -motor_disk_height * 2])
				cylinder(r=stem_r, h=motor_disk_height * 2);
			if (!solid_screw)
				translate(v=[0, 10, -motor_disk_height])
					rotate (v=[1, 0, 0], 90) m3(40);
		}

		translate(v=[0, 0, -motor_disk_height * 3])
			cylinder(r=retainer_r, h=motor_disk_height);

		if (solid_screw) {
			translate(v=[0, 20, -motor_disk_height])
				rotate (v=[1, 0, 0], 90) m3(40);
		}
	}
}

stanley();