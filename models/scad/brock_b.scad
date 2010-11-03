include <hardware.scad>

module brock_b() {
	h = 7;

	corner = 20;
	width = 70;
	screw = 5;
	z_off = 6.1;
	m5_z_off = -6;

	difference() {
		cube([width, width, h]);

		for (v = [[0,0,0],[width-corner,0,0],
				[0,width-corner,0],[width-corner,width-corner,0]]) 
			translate(v=v) cube([corner, corner, h]);

		for (x=[corner+screw, width-(corner+screw)])
			for (y=[corner+screw, width-(corner+screw)])
				translate(v=[x,y,m5_z_off]) m5();

		translate(v=[width/2,-0.5,h]) axle(h=width+1);
	}
}

brock_b();