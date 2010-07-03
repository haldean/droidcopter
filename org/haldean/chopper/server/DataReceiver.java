package org.haldean.chopper.server;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import javax.swing.Timer;
import java.awt.event.*;

/** A huge singleton class to receive data from the chopper that
 *  operates within its own thread.
 *  @author William Brown */
public class DataReceiver implements Runnable {
    /* These three statements enforce singularity */
    private static DataReceiver instance = null;

    private DataReceiver() {
	/* Declared private so no one can instantiate it */
    }

    /** Get the instance of the DataReceiver class, creating
     *  one if one does not exist.
     *  @return The DataReceiver instance */
    public static DataReceiver getInstance() {
	if (instance == null) {
	    instance = new DataReceiver();
	}
	return instance;
    }

    /** Send a line from a static context to the server.
     *  @param s The string to send */
    public static void sendToDefault(String s) {
	DataReceiver.getInstance().sendln(s);
    }

    /* Begin actual class */

    /* Server addresses and port numbers. Images are transmitted
     * over a separate socket from text, and through a different port */
    private String serverAddr;
    private int dataPort; 
    private int imgPort;
    
    /* The Socket and Reader/Writer pair for textual data */
    private Socket dataConnection;
    private BufferedReader data;
    private BufferedWriter output;

    /* The Socket and Reader for incoming images */
    private Socket imgConnection;
    private ObjectInputStream image;

    /* The objects to be updated on incoming text data */
    private LinkedList<Updatable> tied;
    /* The ImagePanel to pass retrieved images to */
    private ImagePanel imageTied;

    /* Are we connected to the server? */
    private boolean isConnected;
    /* Should we stop the communications thread */
    private boolean stopThread;

    /* This is a visual representation of our status */
    private StatusLabel statusLabel;

    /* Timeout timer */
    private Timer timeout;
    private final int timeoutLength = 4000;
    private boolean receiving;

    /** Initialize this DataReceiver object, destroying all previous state.
     *  @param _serverAddr The IP address or hostname of the transmitting server
     *  @param _dataPort The port to connect to for textual data
     *  @param _imgPort The port to connect to for images */
    public void initialize(String _serverAddr, int _dataPort, int _imgPort) {
	serverAddr = _serverAddr;
	dataPort = _dataPort;
	imgPort = _imgPort;

	if (tied == null)
	    tied = new LinkedList<Updatable>();
	stopThread = false;
	isConnected = false;

	/* Close all open sockets */
	try {
	    if (dataConnection != null)
		dataConnection.close();
	    if (imgConnection != null)
		imgConnection.close();
	} catch (IOException e) {
	    e.printStackTrace();
	}

	receiving = false;
	timeout = new Timer(timeoutLength, new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    receiptTimeout();
		}
	    });
	timeout.setInitialDelay(timeoutLength);
	timeout.setRepeats(false);
    }

    /** Tie the component to a status label.
     *  @param sl The status label that represents the data receiver */
    public void setStatusLabel(StatusLabel sl) {
	tie(sl);
	statusLabel = sl;
    }

    /** Tie an object to the DataReceiver.
     *  @param u Object to update on incoming data */
    public void tie(Updatable u) {
	if (tied == null)
	    tied = new LinkedList<Updatable>();
	tied.add(u);
    }

    /** Tie the ImageComponent to the DataReceiver.
     *  @param i Component to update with new images */
    public void tieImage(ImagePanel i) {
	imageTied = i;
	tied.add(i);
    }

    /** Update all tied classes.
     *  @param msg The received message */
    private void updateAll(String msg) {
	/* If this message means there's an incoming image,
	 * get ready to receive it. */
	if (msg.startsWith("SYS"))
	    Debug.log("MSG " + msg);
	if (msg.startsWith("IMAGE"))
	    receiveImage(msg);
	for (int i=0; i<tied.size(); i++)
	    tied.get(i).update(msg);
	if (! msg.startsWith("SYS")) {
	    timeout.restart();
	    if (! receiving)
		onReceiving();
	    receiving = true;
	}
    }

    /** Called when data receipt times out. */
    private void receiptTimeout() {
	Debug.log("Chopper Timed Out");
	updateAll("SYS:RECEIVING:NO");
	
	if (receiving) {
	    try {
		if (image != null)
		    image.close();
		image = null;

		if (imgConnection != null) {
		    Debug.log("Closing image sockets");
		    imgConnection.close();
		    imgConnection = new Socket(serverAddr, imgPort);
		}
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	}
	receiving = false;
    }

    /** Create a thread to receive an incoming image.
     *  @param msg The image incoming message. This is necessary because it contains the
     *             length of the image to be received */
    private void receiveImage(String msg) {
	try {
	    /* Create a new ObjectInputStream if it doesn't already exist. */
	    if (image == null) {
		image = new ObjectInputStream(imgConnection.getInputStream());
		Debug.log("Created a new ObjectInputStream");
	    }
	} catch (IOException e) {
	    e.printStackTrace();
	    return;
	}

	/* Create a new ImageReceiver thread that transmits a string
	 * to the phone when it is done receiving. This will cue the phone
	 * to send the next string */
	try {
	    ImageReceiver r = new ImageReceiver(image, msg, imageTied, new Callback() {
		    public void completed() {
			sendln("IMAGE:RECEIVED");
		    }
		});
	    Thread imgThread = new Thread(r);
	    imgThread.setName("Image receiver");
	    imgThread.start();
	} catch (IllegalArgumentException e) {
	    ;
	}
    }

    /** Returns the status of the connection.
     *  @return True if connected to server, false if not */
    public boolean connected() {
	return isConnected;
    }

    /** Tell the DataReceiver to break its connection to the server. */
    public void stop() {
	stopThread = true;
    }

    /** This gets called when the host is turning off. */
    public void die() {
	try {
	    sendln("SERVER:CLOSING");
	    stopThread = true;
	} catch (Exception e) {
	    /* Don't do anything -- we're killing the program anyway, and
	     * this is sort of expected to happen anyway. dataConnection
	     * should close after stopThread is set to true */
	}
    }

    /** Send a line to the phone.
     *  @param s The string to send to the phone */
    public void sendln(String s) {
	System.err.println("Sending: " + s);
	try {
	    output.write(s + "\n");
	    output.flush();
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }

    private void setConnected(boolean _connected) {
	isConnected = _connected;
	statusLabel.setConnected(isConnected);
    }

    private void onReceiving() {
	sendln("IMAGE:AVAILABLESIZES");
	sendln("IMAGE:GET");
	Debug.log("Receiving");
    }

    /** Run the DataReceiver thread. */
    public void run() {
	Thread.currentThread().setName("Data receiver");
	Debug.log("DataReceiver thread " + Thread.currentThread().getName() + " started");
	/* This loop automatically reestablishes the connections if they die
	 * unless the stopThread flag is set true */
	while (! stopThread) {
	    try {
		/* Throw an exception if we haven't been given a server address */
		if (serverAddr == null)
		    throw new IOException();

		Debug.log("Connecting on " + serverAddr + " ports " + dataPort + " and " + imgPort);
		/* Connect data and imagery */
		dataConnection = new Socket(serverAddr, dataPort);
		imgConnection = new Socket(serverAddr, imgPort);

		/* Create Reader/Writer pair for textual data */
		data = new BufferedReader(new InputStreamReader(dataConnection.getInputStream()));
		output = new BufferedWriter(new OutputStreamWriter(dataConnection.getOutputStream())); 

		Debug.log("Connected");
		
		setConnected(true);
		/* Send a line to the server telling it we've connected */
		sendln("SERVER:HELLO");

		/* Read data in until we're told to stop or we lose the connection */
		String in;
		while (! stopThread && (in = data.readLine()) != null)
		    updateAll(in);
		    
		System.out.println("Disconnected");
		sendln("SERVER:DISCONNECTED");

		/* Close connections. We are no longer connected */
		dataConnection.close();
		imgConnection.close();
		setConnected(false);
	    } catch (IOException e) {
		setConnected(false);
		e.printStackTrace();

		Debug.log("Error initializing sockets: " + e.toString());

		/* If connecting fails, wait 5 seconds before trying again */
		try {
		    Thread.sleep(5000);
		} catch (InterruptedException exception) {
		    return;
		}
	    } 
	}
	setConnected(false);
	stopThread = false;
    }
}
	