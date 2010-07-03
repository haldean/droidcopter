package org.haldean.chopper;

import java.io.Serializable;

public interface NavTask extends Serializable {
	public abstract long getInterval();
	public abstract boolean isComplete();
	public abstract void setVelocity(double[] target);
}
