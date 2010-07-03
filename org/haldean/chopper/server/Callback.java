package org.haldean.chopper.server;

/** An interface to provide a generic callback for when actions are
 *  completed. 
 *  @author William Brown */
public interface Callback {
    /** Called when the action has been completed */
    public void completed();
}