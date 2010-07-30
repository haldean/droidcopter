package org.haldean.chopper;

public interface Receivable {
	public abstract void receiveMessage(String msg, Receivable source);
}
