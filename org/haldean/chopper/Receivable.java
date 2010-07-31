package org.haldean.chopper;

/**
 * An interface for classes that can receive messages.
 * @author Benjamin Bardin
 */
public interface Receivable {
	/**
	 * Processes a message.
	 * @param msg The message to process.
	 * @param source The source of the message, if a reply is needed.  May be null.
	 */
	public abstract void receiveMessage(String msg, Receivable source);
}
