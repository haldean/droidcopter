include <hardware.scad>
use <stanley.scad>

module fred() {
	h = 2 * axle_r;
	difference() {
		cube([80, 40, h]);

		translate(v=[20, -10, h]) axlepair();

		translate(v=[20 + axle_separation / 2, 0, 0]) {
			union() {
				translate(v=[0,10,0]) m3();
				translate(v=[0,30,0]) m3();
			}
		}

		translate(v=[80, 20, 10]) {
			rotate(v=[1,0,0], 90) 
				rotate(v=[0,1,0], 90) stanley(solid_screw=true);
		}

		for (v = [[20,20,0],[20+axle_separation,20,0],
			     [20+axle_separation/2, 10, 0],
				[20+axle_separation/2, 30, 0]])
			translate(v=v) m3();
	}
}

fred();
