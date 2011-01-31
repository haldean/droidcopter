use <brock_b.scad>
use <hardware.scad>

module brock_pair() {
    brock_w = 70;
    brock_h = 7;
    brock_sp = 4;
    axle_len = 200;
    union() {
	brock_b();
	translate([0,brock_w,brock_h * 2 + brock_sp]) {
	    rotate(v=[1,0,0], 180) brock_b();
	}
	translate([brock_w / 2,
		(-axle_len+brock_w)/4,
		brock_h + brock_sp / 2]) axle(140);
    }
}

brock_pair();

union(){
    translate([0,70,22]) rotate(-90) brock_pair();
    translate([0,0,44]) brock_pair();
    translate([0,70,66]) rotate(-90) brock_pair();
}