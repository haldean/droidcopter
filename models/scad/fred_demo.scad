use <fred.scad>
use <hardware.scad>
use <stanley.scad>

rotate(v=[0,1,0], a=270)
union() {
    fred();
    translate([0,40,34]) rotate(v=[1,0,0],180) fred();
    translate([20,-10,17]) axlepair(150);
    translate([80,20,17]) {
	rotate(v=[0,1,0], 90) {
	    rotate(90) stanley(false);
	}
    }
}