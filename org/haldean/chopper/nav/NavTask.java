package org.haldean.chopper.nav;


public interface NavTask {
	public long getInterval();
	public boolean isComplete();
	public void getVelocity(double[] target);
	public abstract String toString();
}
