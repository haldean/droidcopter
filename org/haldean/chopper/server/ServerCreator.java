package org.haldean.chopper.server;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class ServerCreator {
    private static HashMap<String, String> arguments;
    private static ServerHost serverHost;

    private static String uri;
    private static Integer dataPort;
    private static Integer imagePort;
    private static boolean enableHeartbeat = true;

    public static ServerHost getServerHost() {
	return serverHost;
    }

    public static String getUri() {
	return arguments.get("host");
    }

    public static String getServer() {
	return getUri() + ":" + dataPort;
    }

    public static Integer getDataPort() {
	return dataPort;
    }

    public static Integer getImagePort() {
	return imagePort;
    }

    public static boolean getHeartbeatEnabled() {
	return enableHeartbeat;
    }

    public static String getArgument(String argumentName) throws IllegalArgumentException {
	if (! arguments.containsKey(argumentName)) {
	    throw new IllegalArgumentException(argumentName + 
					       " was not specified on the command line.");
	}

	return arguments.get(argumentName);
    }

    /** Run the chopper host 
     *  @param args -d enables printing debug information to the command line,
     *  and -h followed by a hostname specifies the hostname to connect to 
     *  @throws Exception if the provided host name is invalid */
    public static void main(String args[]) {
	/* Parse command line arguments */
	arguments = new HashMap<String, String>();

	for (String arg : args) {
	    if (arg.length() > 0) {
		String[] argparts = arg.split("=", 2);
		String value = argparts.length == 2 ? argparts[1] : null;
		arguments.put(argparts[0], value);
	    }
	}
	 
	if (arguments.containsKey("debug")) {
	    Debug.setEnabled(true);
	    if (arguments.containsKey("debugout")) {
		try {
		    Debug.setOutputStream(new FileWriter(getArgument("debuglog")));
		} catch (IOException e) {
		    Debug.log("Could not write to the provided debug log");
		}
	    }
	}

	dataPort = new Integer(arguments.get("port"));
	imagePort = dataPort + 1;
	enableHeartbeat = !arguments.containsKey("heartless");

	DataReceiver.getInstance().initialize(getUri(), getDataPort(), getImagePort());

	serverHost = new ServerHost();
	serverHost.osInit();

	try {
	    SwingUtilities.invokeAndWait(new Runnable() {
		    public void run() {
			try {
			    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (Exception e) {
			    System.out.println("Unable to set LaF: " + e.toString());
			}
			serverHost.start();
		    }
		});
	} catch (InterruptedException e) {
	    System.out.println("Interrupted when starting ServerHost: " + e.toString());
	    System.exit(1);
	} catch (InvocationTargetException e) {
	    System.out.println("Unable to invoke target: " + e.toString());
	}

	serverHost.accept();
    }
}