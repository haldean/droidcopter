include <droid.scad>
use <hardware.scad>

module droidmount() {
		endcap_depth = 30;
		wall_thickness = 7;
		bar_thickness = 6;
		bar_length = droid_length / 2 + wall_thickness + 10;
		screw_plate_width = 20;
		height = droid_height + wall_thickness;
		echo("Total length:");
		echo(bar_length+wall_thickness);
		
		difference() {
				union() {
						// End caps
						cube([endcap_depth, droid_width + 2 * wall_thickness, height]);
						// Lower supports
						for (y = [0, droid_width / 2, droid_width - 2 * bar_thickness + wall_thickness * 2]) {
								translate(v=[0,y,0]) cube([bar_length, bar_thickness, wall_thickness]);
						}
						// Tension screw mounts
						for (y=[-screw_plate_width, droid_width + screw_plate_width - wall_thickness]) {
								translate(v=[endcap_depth - 1.5 * wall_thickness, y, 0]) {
										difference() {
												cube([1.5 * wall_thickness, screw_plate_width, height]);
												rotate(v=[0,1,0], 90) translate(v=[-height/2,screw_plate_width/2,-6]) m5(65);
										}
								}
						}
				}
				#translate(v=[wall_thickness, wall_thickness, height - droid_height]) droid();
		}
}

union() {
		droidmount();
		translate(v=[droid_length + 10, droid_width + 10, 0]) rotate(180) droidmount();
}
