package org.haldean.chopper.server;

/**
 *  Describes an interface for classes that process incoming or
 *  outgoing messages.
 *
 *  @author William Brown
 */
public interface MessageHook {
    /**
     *  Gets the list of string prefixes representing messages that
     *  this class can process.
     *
     *  @return A list of string prefixes that define what this class
     *  can receive.
     */
    String[] processablePrefixes();

    /**
     *  Process a message.
     *
     *  @param message The most recent incoming or outgoing message.
     */
    void process(Message message);
}
    