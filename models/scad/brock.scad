include <hardware.scad>

module brock() {
	h = 1.5 * axle_r;
	echo("Total Height = ", h * 8);
	corner = 20;
	width = 80;
	screw = 5;
	z_off = 6.1;
	m5_z_off = -6;

	difference() {
		cube([width, width, h]);

		for (v = [[0,0,0],[width-corner,0,0],
				[0,width-corner,0],[width-corner,width-corner,0]]) 
			translate(v=v) cube([corner, corner, h]);

		if (type == "a")
		for (v = [[screw, corner+screw, z_off],
				[screw, width-(corner+screw), z_off],
				[corner+screw, screw, z_off],
				[corner+screw, width-screw, z_off],
				[width-(corner+screw), screw, z_off],
				[width-(corner+screw), width-screw, z_off],
				[width-screw, corner+screw, z_off],
				[width-screw, width-(corner+screw), z_off]])
			translate(v=v) rotate(v=[1,0,0], 180) m2();

		for (x=[corner+screw, width-(corner+screw)])
			for (y=[corner+screw, width-(corner+screw)])
				translate(v=[x,y,m5_z_off]) m5();

		translate(v=[width/2,-0.5,h]) axle(h=width+1);
	}
}

brock();