package org.haldean.chopper.server;

/** An Updatable to write everything to standard error 
 *  @author William Brown */
public class EchoUpdatable implements Updatable {
    public void update(String s) {
	System.out.println("Update received: " + s);
    }
}