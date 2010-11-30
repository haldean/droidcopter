package org.haldean.chopper.server;

public abstract class UiController implements Runnable {
    protected final int POLL_PERIOD = 40;
    protected final int SLEEP_TIME = 1000;
    protected final int DEBOUNCE_TIME = POLL_PERIOD * 3;
}