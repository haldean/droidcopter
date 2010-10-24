axle_r = 5;
axle_separation = 30;

module axle(h) {
	rotate(v=[-1,0,0], a=90) cylinder(r=axle_r, h=h, $fn=10);
}

module axlepair(h=100) {
	union() {
		axle(h);
		translate(v=[axle_separation, 0, 0]) axle(h);
	}
}

module m3(h=16) {
	union() {
		cylinder(r=3.25, h=3);
		cylinder(r=2, h=h);
	}
}