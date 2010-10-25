package org.haldean.chopper.server;

import java.util.HashMap;
import java.util.Map;

/**
 *  Send a "heartbeat" message to the helicopter every second. This
 *  tells the helicopter that we're receiving and have control of
 *  it. It also receives responses from the chopper, and in this way
 *  we can tell what the round-trip time is between here and the
 *  chopper.
 *
 *  @author William Brown
 */
public class HeartbeatThread implements Runnable, Updatable {
    private static HeartbeatThread instance;

    /* Milliseconds between updates */
    private int period = 500;
    private int lastMessageId = 0;

    /* Maps messages to when they were sent. */
    private Map<String, Long> messageTimes;
    
    /* The amount of time it took the chopper to respond to the
     * message. */
    private long roundTripTime = 0;

    /**
     *  Starts the heart. If the heartbeat has already been started,
     *  this does nothing. Note that the heartbeat thread will not
     *  send a message if the DataReceiver is not connected, so this
     *  is safe to run at any time.
     *
     *  @return The HeartbeatThread instance
     */
    public static HeartbeatThread revive() {
	if (instance == null) {
	    instance = new HeartbeatThread();
	    new Thread(instance).start();
	}
	return instance;
    }

    private HeartbeatThread() {
	messageTimes = new HashMap<String, Long>();
    }

    /**
     *  Set the time delay between heartbeats. The default period is one second.
     *
     *  @param period The number of milliseconds to wait between
     *  heartbeat messages 
     */
    public void setPeriod(int newPeriod) {
	period = newPeriod;
    }

    /**
     *  Start the heartbeat. This will be run with the {@link revive}
     *  method and does not need to be called by an API user.
     */
    public void run() {
	while (true) {
	    if (DataReceiver.getInstance().isConnected()) {
		String message = "COMM:PULSE:" + lastMessageId++;
		messageTimes.put(message, System.currentTimeMillis());
		DataReceiver.sendToDefault(message);
	    }

	    try {
		Thread.sleep(period);
	    } catch (InterruptedException e) {
		Debug.log("Heartbeat was interrupted.");
		e.printStackTrace();
	    }
	}
    }

    /**
     *  Reads in a heartbeat response and calculates the last round trip time.
     */
    public void update(String message) {
	if (message.startsWith("COMM:PULSE:")) {
	    roundTripTime = System.currentTimeMillis() - messageTimes.remove(message);
	    Debug.log("Round trip time: " + roundTripTime);
	}
    }
	
}