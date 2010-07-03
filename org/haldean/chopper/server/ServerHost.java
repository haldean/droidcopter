package org.haldean.chopper.server;

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
    public final String heloName = new String("Horizon");

    /* All sorts of components */
    /** The object responsible for receiving and sending
     *  data to and from the chopper */
    public final DataReceiver r;
    /** The component that displays the globe with tracking
     *  data and location selection */
    public final WorldWindComponent lc;
    /** The component that displays a 3D rendering of the 
     *  current orientation of the chopper */
    public final OrientationComponent tc;
    /** The component that displays telemetry and allows for
     *  manipulation of the image quality */
    public final ImagePanel ic;
    /** The component that displays graphs of the acceleration */
    public final AccelerationComponent ac;
    /** An Updatable that receives all messages from the chopper
     *  @see EchoUpdatable */
    public final Updatable status;
    /** The text area where debug information is printed */
    public final UpdatableTextArea debug;
    /** The component responsible for displaying sensor
     *  data that isn't displayed by one of the other components */
    public final SensorComponent sc;
    /** The component that displays mission-critical data like
     *  battery levels and connection statuses */
    public final StatusLabel sl;

    /* Custom controllers, mostly because Will is the effing man */
    private final PadController pad;

    /** The components to put in the left tab pane in the UI */
    private LinkedList<Component> leftTabPanes;
    /** The components to put in the right tab pane in the UI */
    private LinkedList<Component> rightTabPanes;
    /** The left tab pane */
    public JTabbedPane leftTabs;
    /** The right tab pane */
    public JTabbedPane rightTabs;

    /* Whether we are allowed to create a WorldWindComponent.
     * WWJ doesn't run on Linux 86-64, so Ben's high-fallutin' 
     * 64-bit Ubuntu can't have a globe */
    private boolean allowGlobe = true;

    /** Create a new ServerHost
     *  @param s The server address and port in the form hostname:port. If passed null,
     *           it will automatically show a JOptionPane to ask for one */
    public ServerHost() {
	super();
	/* Set the title of the JFrame */
	setTitle(heloName + " Control Server");

	Debug.log("Server exists in thread " + Thread.currentThread().getName());

	/* Create all the necessary components so we can feed them
	 * into each other */
	r = DataReceiver.getInstance();

	if (allowGlobe)
	    lc = new WorldWindComponent();
	else
	    lc = null;

	tc = new OrientationComponent();
	ic = new ImagePanel();
	ac = new AccelerationComponent();
	sc = new SensorComponent();
	status = new EchoUpdatable();
	debug = new UpdatableTextArea("Debug");

	sl = new StatusLabel();
	r.setStatusLabel(sl);

	/* Sets the output for all the glorious error messages */
	Debug.setDebugOut(debug);

	/* Create the sensor parser and tell it where to
	 * find all of the appropriate components */
	SensorParser sp = new SensorParser();
	sp.setWorldWindComponent(lc);
	sp.setOrientationComponent(tc);
	sp.setAccelerationComponent(ac);
	sp.setSensorComponent(sc);

	/* Tie the updatables to the DataReceiver */
	r.tie(sp);
	//	r.tie(status);
	r.tieImage(ic);

	leftTabPanes = new LinkedList<Component>();
	rightTabPanes = new LinkedList<Component>();

	/* The left tab pane has the globe, the tilt and the debug feed */
	if (allowGlobe)
	    leftTabPanes.add(lc);
	leftTabPanes.add(tc);
	leftTabPanes.add(debug);

	/* The right has the telemetry, the acceleration and the sensor data */
	rightTabPanes.add(ic);
	rightTabPanes.add(ac);
	rightTabPanes.add(sc);

	pad = new PadController(this);
    }

    /** Start accepting data */
    public void accept() {
	/* Start the DataReceiver thread */
	(new Thread(r)).start();
    }

    /** Get the UI font
     *  @param size The size in pixels */
    private Font getFont(int size) {
	return getFont(size, false);
    }

    /** Get the UI font
     *  @param size The size in pixels
     *  @param bold True if bold font is desired, false if not */
    private Font getFont(int size, boolean bold) {
	return new Font("Helvetica", (bold) ? Font.BOLD : Font.PLAIN, size);
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
	debug.updateUI();
	ac.updateUI();
	sc.updateUI();
	lc.updateUI();
	ic.updateUI();

	/* The right/left pane creator */
	JPanel horizontalPanel = new JPanel(new GridLayout(1,2));

	/* The two tab panes */
	leftTabs = new JTabbedPane();
	rightTabs = new JTabbedPane();

	/* Add all of the stuff on the left to the tabbed pane */
	for (int i=0; i<leftTabPanes.size(); i++) {
	    leftTabs.add(leftTabPanes.get(i));
	}

	/* Do the same for the right */
	for (int i=0; i<rightTabPanes.size(); i++) {
	    rightTabs.add(rightTabPanes.get(i));
	}

	horizontalPanel.add(leftTabs);
	add(horizontalPanel);

	/* The right pane */
	JPanel rawDataPanel = new JPanel(new BorderLayout());

	/* The title label*/
	JLabel titleLabel = new JLabel(heloName.toUpperCase() + " SERVER");
	titleLabel.setFont(getFont(24, true));
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
			r.stop();
			connected = false;
			disconnectButton.setText("Connect");
		    } else {
			(new Thread(r)).start();
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
		    r.die();
		    try {
			while (r.connected())
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
	rawDataPanel.add(sl, BorderLayout.SOUTH);
	horizontalPanel.add(rawDataPanel);

	/* Show the frame */
	setPreferredSize(new Dimension(1100, 700));
	setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	pack();
	setVisible(true);

	new Thread(pad).start();
    }
}