droid_length = 116;
droid_width = 61;
droid_height = 14;

module droid() {
    union() {
	cube([droid_length, droid_width, droid_height]);
	translate(v=[11,6,-10]) cylinder(r=3, h=10);
    }
}
