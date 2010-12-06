package org.haldean.chopper.server;

/**
 *  Represents a message sent to or received from the chopper.
 *
 *  @author William Brown
 */
public class Message {
    public String message;
    private String[] messageParts;

    /**
     *  Create a new message for a string.
     *
     *  @param m The string representation of the message.
     */
    public Message(String m) {
	message = m;
	messageParts = m.split(":");
    }

    /**
     *  @return The number of parts in the message.
     */
    public int length() {
	return messageParts.length;
    }

    /**
     *  Get a part of the message.
     *
     *  @param index The index of the element to retrieve.
     *  @return The string at that index of the message.
     */
    public String getPart(int index) {
	return messageParts[index];
    }

    /**
     *  Returns true if this message matches the given prefix.
     *
     *  @param prefix A prefix to check against the message.
     *  @return True if this message has the prefix given.
     */
    public boolean prefixMatches(String prefix) {
	return message.startsWith(prefix);
    }

    /**
     *  Returns true if the first element of the message matches the
     *  given high-level type.
     *
     *  @param type The type of the message (e.g. "GUID", "ACCEL").
     *  @return True if this message is of the given type.
     */
    public boolean isType(String type) {
	return getPart(0).equals(type);
    }
}