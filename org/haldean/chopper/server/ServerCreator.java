package org.haldean.chopper.server;

import java.lang.reflect.InvocationTargetException;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class ServerCreator {
    private static String uri;
    private static Integer dataPort;
    private static Integer imagePort;
    private static boolean enableHeartbeat = true;

    public static String getURI() {
	return uri;
    }

    public static String getServer() {
	return uri + ":" + dataPort;
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

    /** Run the chopper host 
     *  @param args -d enables printing debug information to the command line,
     *  and -h followed by a hostname specifies the hostname to connect to 
     *  @throws Exception if the provided host name is invalid */
    public static void main(String args[]) {
	/* Parse command line arguments */
	String uriString = null;
	for (int i=0; i<args.length; i++) {
	    if (args[i].equals("-d")) {
		Debug.setEnabled(true);
	    } else if (args[i].startsWith("-h")) {
		if (args.length - 1 == i) {
		    System.err.println("You must supply a host:port after the -h flag.");
		    System.exit(-1);
		}
		uriString = args[++i];
	    } else if (args[i].equals("--heartless")) {
		enableHeartbeat = false;
	    }
	}

	String[] uriParts = uriString.split(":");
	uri = uriParts[0];
	dataPort = new Integer(uriParts[1]);
	imagePort = new Integer(uriParts[1]) + 1;

	DataReceiver.getInstance().initialize(getURI(), getDataPort(), getImagePort());

	final ServerHost s = new ServerHost();
	s.osInit();

	try {
	    SwingUtilities.invokeAndWait(new Runnable() {
		    public void run() {
			try {
			    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (Exception e) {
			    System.out.println("Unable to set LaF: " + e.toString());
			}
			s.start();
		    }
		});
	} catch (InterruptedException e) {
	    System.out.println("Interrupted when starting ServerHost: " + e.toString());
	    System.exit(1);
	} catch (InvocationTargetException e) {
	    System.out.println("Unable to invoke target: " + e.toString());
	}

	s.accept();
    }
}