package org.haldean.chopper.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class MessageHookManager extends Thread {
    List<MessageHook> hooks;
    Map<String, List<MessageHook>> prefixes;
    Queue<String> queue;

    public MessageHookManager() {
	setName("Message hook manager");

	hooks = new ArrayList<MessageHook>();
	prefixes = new HashMap<String, List<MessageHook>>();
	queue = new LinkedList<String>();

	defaultHooks();
    }

    public void defaultHooks() {
	addHook(new PidLogger());
    }

    public void addHook(MessageHook hook) {
	hooks.add(hook);

	String[] hookPrefixes = hook.processablePrefixes();
	for (String prefix : hookPrefixes) {
	    if (prefixes.containsKey(prefix)) {
		prefixes.get(prefix).add(hook);
	    } else {
		ArrayList<MessageHook> prefixHooks = new ArrayList<MessageHook>();
		prefixHooks.add(hook);
		prefixes.put(prefix, prefixHooks);
	    }
	}
    }

    private void processMessage(String message) {
	Message m = new Message(message);
	
	for (String prefix : prefixes.keySet()) {
	    if (m.prefixMatches(prefix)) {
		for (MessageHook hook : prefixes.get(prefix)) {
		    Debug.log(hook.getClass().toString());
		    hook.process(m);
		}
	    }
	}
    }

    public void queue(String message) {
	queue.add(message);
    }

    public void run() {
	while (true) {
	    String message = queue.poll();
	    if (message != null) {
		processMessage(message);
	    } else {
		try {
		    Thread.sleep(50);
		} catch (InterruptedException e) {
		    Debug.log("MessageHookManager's sleep was interrupted.");
		}
	    }
	}
    }
}
		