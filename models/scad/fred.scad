include <axle.scad>

module fred() {
	h = 2 * axle_r;
	difference() {
		cube([80, 40, h]);
		translate(v=[20, -10, h]) axlepair();
		m3(50);
	}
}

fred();
