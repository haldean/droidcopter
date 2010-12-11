use <hardware.scad>

module stanley(solid_screw = false) {
    motor_r = 23;
    stem_r = 5.5;
    retainer_r = stem_r;
    motor_disk_height = 6.3;
    screw_count = 4;
		stem_height = motor_disk_height * 3;
		back_axle_length = 18;

    difference () {
				union() {
						difference() {
								cylinder(r=motor_r, h=motor_disk_height);
								for (i = [0:screw_count-1]) {
										rotate(v=[0,0,1], a = i * 360 / screw_count) {
												translate(v=[16.5, 0, 0]) m3();
										}
								}
						}
	
						difference() {
								translate(v=[0, 0, -stem_height]) {
										cylinder(r=stem_r, h=stem_height);
								}
								
								if (!solid_screw) {
										translate(v=[0, 10, -stem_height + motor_disk_height]) {
												rotate (v=[1, 0, 0], 90) m3(40);
										}
								}
						}

						translate(v=[0, 0, -motor_disk_height - stem_height]) {
								cylinder(r=retainer_r, h=motor_disk_height);
						}

						if (solid_screw) {
								translate(v=[0, 20, -motor_disk_height]) {
										rotate (v=[1, 0, 0], 90) m3(40);
								}
						}
				}

				translate(v=[0, 0, -back_axle_length + motor_disk_height]) {
						cylinder(h=back_axle_length, r=1.75);
				}
		}
}			

rotate(v=[1,0,0], 180) stanley();