package org.haldean.chopper.server;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

/** The superclass! This is the frame that encompasses everything
 *  else.
 *  @author William Brown */
public class ServerHost extends JFrame {
    /** The chopper name. We've been changing it enough that
     *  it's just easier to have it be an easily-changable string */
    final String heloName = "Robocopter";

    /* All sorts of components */
    /** The object responsible for receiving and sending
     *  data to and from the chopper */
    final DataReceiver dataReceiver;
    /** The component that displays the globe with tracking
     *  data and location selection */
    final WorldWindComponent globeComponent;
    /** The component that displays a 3D rendering of the
     *  current orientation of the chopper */
    final OrientationComponent orientationComponent;
    /** The component that displays telemetry and allows for
     *  manipulation of the image quality */
    final ImagePanel imagePanel;
    /** The component that displays graphs of the acceleration */
    final AccelerationComponent accelerationComponent;
    /** The component that displays error values from the four PID
     *  loops on the chopper. */
    final PidErrorComponent pidComponent;
    final PidTuner pidTuner;
    /** An Updatable that receives all messages from the chopper
     *  @see EchoUpdatable */
    final Updatable status;
    /** The component responsible for displaying sensor
     *  data that isn't displayed by one of the other components */
    final SensorComponent sensorComponent;
    /** The component that displays mission-critical data like
     *  battery levels and connection statuses */
    final StatusLabel statusLabel;
    /** A component to display and set motor speeds. */
    final MotorComponent motorComponent;
    /** The autopilot GUI. */
    final NavPanel navPanel;

    /* Custom controllers, mostly because Will is the effing man */
    private final List<UiController> pads;

    /** The components to put in the left tab pane in the UI */
    private LinkedList<JComponent> leftTabPanes;
    /** The components to put in the right tab pane in the UI */
    private LinkedList<JComponent> rightTabPanes;
    /** The left tab pane */
    JTabbedPane leftTabs;
    /** The right tab pane */
    JTabbedPane rightTabs;

    /** Create a new ServerHost. */
    public ServerHost() {
	super();
	/* Set the title of the JFrame */
	setTitle(heloName + " Control Server");

	Debug.log("Server exists in thread " + Thread.currentThread().getName());

	/* Create all the necessary components so we can feed them
	 * into each other */
	dataReceiver = DataReceiver.getInstance();

	globeComponent = new WorldWindComponent();
	orientationComponent = new OrientationComponent();
	imagePanel = new ImagePanel();
	accelerationComponent = new AccelerationComponent();
	pidComponent = new PidErrorComponent();
	pidTuner = new PidTuner();
	sensorComponent = new SensorComponent();
	status = new EchoUpdatable();
	motorComponent = new MotorComponent();
	navPanel = new NavPanel();

	statusLabel = new StatusLabel();
	dataReceiver.setStatusLabel(statusLabel);

	/* Create the sensor parser and tell it where to
	 * find all of the appropriate components */
	SensorParser sp = new SensorParser();
	sp.setWorldWindComponent(globeComponent);
	sp.setOrientationComponent(orientationComponent);
	sp.setAccelerationComponent(accelerationComponent);
	sp.setSensorComponent(sensorComponent);

	/* Tie the PID error visualization to the DataReceiver */
	dataReceiver.tie(pidComponent);
	dataReceiver.tie(PidTuningComponent.getInstance());
	dataReceiver.tieImage(imagePanel);

	dataReceiver.tie(pidTuner);

	MessageHookManager.addHook(motorComponent);
	MessageHookManager.addHook(sp);

	if (ServerCreator.getHeartbeatEnabled()) {
	    /* Tie the heartbeat to the DataReceiver */
	    dataReceiver.tie(HeartbeatThread.revive());
	}

	leftTabPanes = new LinkedList<JComponent>();
	rightTabPanes = new LinkedList<JComponent>();

	/* The left tab pane has the globe and the tilt */
	leftTabPanes.add(globeComponent);
	leftTabPanes.add(orientationComponent);
	leftTabPanes.add(motorComponent);

	/* The right has the telemetry, the acceleration and the sensor data */
	rightTabPanes.add(imagePanel);
	rightTabPanes.add(accelerationComponent);
	rightTabPanes.add(sensorComponent);
	rightTabPanes.add(pidComponent);
	rightTabPanes.add(navPanel);

	pads = new ArrayList<UiController>();
	pads.add(new PadController(this));
	pads.add(new KeyboardController(this));
    }

    public <T extends UiController> T getController(Class<T> controllerClass) {
	for (UiController c : pads)
	    if (controllerClass.isInstance(c))
		return controllerClass.cast(c);
	return null;
    }

    public String getInput(String prompt) {
	KeyboardController k = getController(KeyboardController.class);
	return k.getInput(prompt);
    }

    /** Start accepting data */
    public void accept() {
	/* Start the DataReceiver thread */
	(new Thread(dataReceiver)).start();
    }

    /** Initialize operating system specific stuff */
    public void osInit() {
	Debug.log("Running on " + System.getProperty("os.name") + " " +
		  System.getProperty("os.arch"));
	if (System.getProperty("os.name").startsWith("Mac"))
            System.setProperty("apple.laf.useScreenMenuBar", "true");
    }

    /** Start the ServerHost thread */
    public void start() {
	/* Update the Look and Feel of components created
	 * in the constructor */

	/* The right/left pane creator */
	JPanel horizontalPanel = new JPanel(new GridLayout(1,2));

	/* The two tab panes */
	leftTabs = new JTabbedPane();
	rightTabs = new JTabbedPane();

	/* Add all of the stuff on the left to the tabbed pane */
	for (JComponent c : leftTabPanes) {
	    c.updateUI();
	    leftTabs.add(c);
	}

	/* Do the same for the right */
	for (JComponent c : rightTabPanes) {
	    c.updateUI();
	    rightTabs.add(c);
	}

	horizontalPanel.add(leftTabs);
	add(horizontalPanel);

	/* The right pane */
	JPanel rawDataPanel = new JPanel(new BorderLayout());

	/* The title label*/
	JLabel titleLabel = new JLabel(heloName.toUpperCase() + " SERVER");
	titleLabel.setFont(StyleProvider.getFont(24, true));
	titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

	/* The status bar */
	JPanel statusPanel = new JPanel(new FlowLayout());
	statusPanel.add(new JLabel(ServerCreator.getServer()));

	/* The disconnect button */
	final JButton disconnectButton = new JButton("Disconnect");
	statusPanel.add(disconnectButton);
	disconnectButton.addActionListener(new ActionListener() {
		private boolean connected = true;
		public void actionPerformed(ActionEvent e) {
		    /* If connected, stop the DataReceiver and switch the
		     * text of the button. If not connected, restart the
		     * DataReceiver thread */
		    if (connected) {
			dataReceiver.stop();
			connected = false;
			disconnectButton.setText("Connect");
		    } else {
			(new Thread(dataReceiver)).start();
			connected = true;
			disconnectButton.setText("Disconnect");
		    }
		}
	    });

	/* The quit button */
	JButton quitButton = new JButton("Quit");
	statusPanel.add(quitButton);
	quitButton.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    /* Tell the DataReceiver to suck it and sleep until
		     * it does */
		    dataReceiver.die();
		    try {
			while (dataReceiver.isConnected())
			    Thread.sleep(200);
		    } catch (InterruptedException ex) {
			;
		    }
		    /* Exit without error */
		    System.exit(0);
		}
	    });

	/* Assemble right panel */
	rawDataPanel.add(titleLabel, BorderLayout.NORTH);
	rawDataPanel.add(rightTabs, BorderLayout.CENTER);
	rawDataPanel.add(statusLabel, BorderLayout.SOUTH);
	horizontalPanel.add(rawDataPanel);

	/* Show the frame */
	setPreferredSize(new Dimension(1100, 700));
	setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	pack();
	setVisible(true);

	for (UiController pad : pads) {
	    new Thread(pad).start();
	}
    }
}
