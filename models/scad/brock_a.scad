include <hardware.scad>

module brock_a(with_screws=0) {
	h = 1.3 * axle_r;
	corner = 20;
	width = 70;
	screw = 5;
	z_off = 6.1;
	m5_z_off = -5;

	difference() {
		union() {
			cube([width, width, h]);
			if (with_screws == 1) {
				for (x=[corner+screw, width-(corner+screw)])
					for (y=[corner+screw, width-(corner+screw)])
						translate(v=[x,y,m5_z_off]) m5();
				for (v = [[screw, corner+screw, z_off],
						[screw, width-(corner+screw), z_off],
						[corner+screw, screw, z_off],
						[corner+screw, width-screw, z_off],
						[width-(corner+screw), screw, z_off],
						[width-(corner+screw), width-screw, z_off],
						[width-screw, corner+screw, z_off],
						[width-screw, width-(corner+screw), z_off]])
					translate(v=v) rotate(v=[1,0,0], 180) m2();
			}
		}

		for (v = [[0,0,0],[width-corner,0,0],
				[0,width-corner,0],[width-corner,width-corner,0]]) 
			translate(v=v) cube([corner, corner, h]);

		
		if (with_screws == 0) {
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
		}

		translate(v=[width/2,-0.5,h]) axle(h=width+1);
	}
}

brock_a();