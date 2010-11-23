use <brock_a.scad>
use <hardware.scad>
use <arduino.scad>

module benedict() {
	width = 70;
	bar = 10;
	corner = 20;
	height = 8;
	offset = 3;

	difference() {
		union() {
			// OUTER GRID BARS
			// South
			translate(v=[corner,0,0]) bar();
			// North
			translate(v=[corner,width-bar,0]) bar();
			// West
 			translate(v=[bar,corner,0]) rotate(90) bar();
			// East
			translate(v=[width,corner,0]) rotate(90) bar();

			// CORNER ANGLE BARS
			// Southeast
			difference() {
				union() {
					translate(v=[width-corner,0,0]) anglebar(height + offset);
					translate(v=[width-corner+7,7,0]) cube([bar, bar, height+offset]);
				}
				translate(v=[width,corner,0]) rotate(90) 
					cube([width - 2 * corner, bar*2, height + offset]);
				translate(v=[corner,0,0]) cube([width - 2 * corner, bar*2, height + offset]);
				translate(v=[width-corner+10,-10,0]) anglebar(height + offset);
			}

			// Southwest
			translate(v=[0,corner,0]) rotate(-90) anglebar();

			// Northwest
			difference() {
				translate(v=[corner,width,0]) rotate(180) anglebar(height + offset);
				translate(v=[bar*2,corner,0]) rotate(90)
					cube([width - 2 * corner, bar*2, height + offset]);
				translate(v=[corner,width-2*bar,0]) 
					cube([width - 2 * corner, bar*2, height + offset]);
			}

			// Northeast
			difference() {
				union() {
					translate(v=[width, width / 2,0]) rotate(90) 
						cube([width / 2 - corner, bar + 3, height + offset]);
					translate(v=[width,width-corner,0]) rotate(90) anglebar(height + offset);
				}
				translate(v=[corner,width-2*bar,0]) 
					cube([width - 2 * corner, bar*2, height + offset]);
			}
		}

		// Arduino
		translate(v=[-3,5,12]) rotate(0) arduino(1);
		// Brock A attachment
		translate(v=[70,0,0]) rotate(v=[0,1,0], a=180) brock_a(1);
	}

	module bar(h=height) {
		cube([width - 2 * corner, bar, h]);
	}

	module anglebar(h=height) {
		rotate(45) cube([sqrt(2)*corner, bar, h]);
	}
}

benedict();