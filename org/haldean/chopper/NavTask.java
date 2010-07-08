package org.haldean.chopper;

import java.io.Serializable;

public interface NavTask extends Serializable {
	public long getInterval();
	public boolean isComplete();
	public void setVelocity(double[] target);
}
