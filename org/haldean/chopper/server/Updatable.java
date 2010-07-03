package org.haldean.chopper.server;

/** An interface for classes that need to be periodically updated 
 *  @author William Brown */
public interface Updatable {
    /** Called when there is data that the class needs to be notified of 
     *  @param msg The message for the class */
    public void update(String msg);
}