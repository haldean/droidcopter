package org.haldean.chopper.server;

/**
 *  Describes an interface for classes that process incoming or
 *  outgoing messages.
 *
 *  @author William Brown
 */
public interface MessageHook {
    /**
     *  Returns whether the implementing class processes incoming
     *  messages (messages from the chopper).
     *
     *  @return True if this class processes incoming messages, or
     *  false otherwise.
     */
    boolean checkIncoming();

    /**
     *  Returns whether the implementing class processes outgoing
     *  messages (messages to the chopper).
     *
     *  @return True if this class processes outgoing messages, or
     *  false otherwise.
     */
    boolean checkOutgoing();

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
    