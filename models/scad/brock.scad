include <hardware.scad>

module brock() {
	h = 1.5 * axle_r;
	echo(h);
	corner = 20;
	width = 80;
	screw = 5;
	z_off = -6;

	difference() {
		cube([width, width, h]);

		for (v = [[0,0,0],[width-corner,0,0],
				[0,width-corner,0],[width-corner,width-corner,0]]) 
			translate(v=v) cube([corner, corner, h]);

		for (v = [[screw, corner+screw, z_off],
				[screw, width-(corner+screw), z_off],
				[corner+screw, screw, z_off],
				[corner+screw, width-screw, z_off],
				[width-(corner+screw), screw, z_off],
				[width-(corner+screw), width-screw, z_off],
				[width-screw, corner+screw, z_off],
				[width-screw, width-(corner+screw), z_off],
				[width/2, width/2, z_off]])
			translate(v=v) m3();

		for (x=[corner+screw, width-(corner+screw)])
			for (y=[corner+screw, width-(corner+screw)])
				translate(v=[x,y,z_off]) m5();

		translate(v=[width/2,-0.5,h]) axle(h=width+1);
	}
}

brock();