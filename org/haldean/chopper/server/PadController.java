package org.haldean.chopper.server;

import net.java.games.input.*;
import javax.swing.*;

/** A thread to take input from a game pad and cue various events in the UI 
 *  @author William Brown */
public class PadController extends UiController {
    private ServerHost ui;
    private Controller ctrl;
    private StatusLabel sl;

    private boolean enabled = true;

    /* The array of buttons and the array index for each */
    private Component buttons[];
    public final int BUTTON_A = 0;
    public final int BUTTON_B = 1;
    public final int BUTTON_X = 2;
    public final int BUTTON_Y = 3;
    public final int BUTTON_L = 4;
    public final int BUTTON_R = 5;
    public final int BUTTON_START = 6;
    public final int BUTTON_XBOX = 7;
    public final int BUTTON_BACK = 8;
    public final int JOYSTICK_L = 9;
    public final int JOYSTICK_R = 10;
    public final int BUTTON_COUNT = 11;

    /* The array of axes and the array index for each */
    private Component axes[];
    public float lastAxisValue[];
    public final int AXIS_L_H = 0;
    public final int AXIS_L_V = 1;
    public final int AXIS_L_TRIGGER = 2;
    public final int AXIS_R_H = 3;
    public final int AXIS_R_V = 4;
    public final int AXIS_R_TRIGGER = 5;
    public final int D_PAD = 6;
    public final int AXIS_COUNT = 7;

    private int lastButtonMask = 0;

    private boolean globeMovement = false;
    
    private final double minDiff = 250; //ms
    private long lastAxesUpdate;
    
    /** Create a new PadController 
     *  @param _ui The ServerHost to act upon */
    public PadController(ServerHost _ui) {
	ui = _ui;
	sl = ui.sl;
	
	ControllerEnvironment env = ControllerEnvironment.getDefaultEnvironment();
	Controller controllers[] = env.getControllers();

	if (controllers.length == 0) {
	    Debug.log("No controllers found");
	    return;
	} else {
	    Debug.log("Found " + controllers.length + " controllers");
	}

	ctrl = null;
	for (Controller c : controllers)
	    if (c.getType() == Controller.Type.GAMEPAD)
		ctrl = c;

	if (ctrl == null) 
	    return;

	Component components[] = ctrl.getComponents();
	Debug.log("Using game pad with " + components.length + " components");

	buttons = new Component[BUTTON_COUNT];
	axes = new Component[AXIS_COUNT];
	lastAxisValue = new float[AXIS_COUNT];
	
	for (Component c : components) {
	    if (!c.isAnalog() && !c.isRelative()) {
		Component.Identifier id = c.getIdentifier();
		if (id == Component.Identifier.Button.A)
		    buttons[BUTTON_A] = c;
		else if (id == Component.Identifier.Button.B)
		    buttons[BUTTON_B] = c;
		else if (id == Component.Identifier.Button.X)
		    buttons[BUTTON_X] = c;
		else if (id == Component.Identifier.Button.Y)
		    buttons[BUTTON_Y] = c;
		else if (id == Component.Identifier.Button.BACK)
		    buttons[BUTTON_BACK] = c;
		else if (id == Component.Identifier.Button.LEFT_THUMB)
		    buttons[BUTTON_L] = c;
		else if (id == Component.Identifier.Button.RIGHT_THUMB)
		    buttons[BUTTON_R] = c;
		else if (id == Component.Identifier.Button.UNKNOWN)
		    buttons[BUTTON_START] = c;
		else if (id == Component.Identifier.Button.MODE)
		    buttons[BUTTON_XBOX] = c;
		else if (id == Component.Identifier.Button.LEFT_THUMB3)
		    buttons[JOYSTICK_L] = c;
		else if (id == Component.Identifier.Button.RIGHT_THUMB3)
		    buttons[JOYSTICK_R] = c;
	    } else {
		Component.Identifier id = c.getIdentifier();
		if (id == Component.Identifier.Axis.X)
		    axes[AXIS_L_H] = c;
		else if (id == Component.Identifier.Axis.Y)
		    axes[AXIS_L_V] = c;
		else if (id == Component.Identifier.Axis.Z)
		    axes[AXIS_L_TRIGGER] = c;
		else if (id == Component.Identifier.Axis.RX)
		    axes[AXIS_R_H] = c;
		else if (id == Component.Identifier.Axis.RY)
		    axes[AXIS_R_V] = c;
		else if (id == Component.Identifier.Axis.RZ)
		    axes[AXIS_R_TRIGGER] = c;
		else if (id == Component.Identifier.Axis.POV)
		    axes[D_PAD] = c;
	    }
	}
    }

    /**
     * Enable or disable the pad controller. This will not stop it
     * from polling the controller, but it will stop it from acting on
     * any button or controller inputs.
     */
    public void setEnabled(boolean enabled) {
	this.enabled = enabled;
    }

    /** Get an integer bitmask representing each of the buttons */
    private int buttonMask() {
	int mask = 0;
	for (int i=0; i<buttons.length; i++)
	    if (buttons[i] != null && buttons[i].getPollData() == 1)
		mask += (1 << i);
	return mask;
    }

    /** Check to see if a button is set in a bitmask 
     *  @param mask The bitmask to check against
     *  @param button The index of the button 
     *  @return True if the button is depressed, false if not */
    private boolean buttonIsSet(int mask, int button) {
	return ((mask >> button) & 1) == 1;
    }

    /**
     * Check to see if a button was pressed in the last poll.
     *
     * @param button The index of the button
     */
    public boolean buttonIsSet(int button) {
	return buttonIsSet(lastButtonMask, button);
    }

    /** Perform an action based on the status of the buttons
     *  @param mask The mask representings buttons that have changed state */
    private void buttonAction(int mask) {
	int currentMask = buttonMask();

	/* Left button moves to the tab to the left.
	 * Holding A moves left tab pane, else right tab pane */
	if (buttonIsSet(mask, BUTTON_L)) {
	    JTabbedPane p;
	    if (buttonIsSet(currentMask, BUTTON_A)) 
		p = ui.leftTabs;
	    else
		p = ui.rightTabs;

	    int tabIndex = p.getSelectedIndex() - 1;
	    if (tabIndex < 0)
		tabIndex = p.getTabCount() - 1;
	    p.setSelectedIndex(tabIndex);
	}

	/* Right button moves to the tab to the right */
	if (buttonIsSet(mask, BUTTON_R)) {
	    JTabbedPane p;
	    if (buttonIsSet(currentMask, BUTTON_A)) 
		p = ui.leftTabs;
	    else
		p = ui.rightTabs;

	    int tabIndex = p.getSelectedIndex() + 1;
	    if (tabIndex >= p.getTabCount())
		tabIndex = 0;
	    p.setSelectedIndex(tabIndex);
	}

	/* Toggle Globe Movement state */
	if (buttonIsSet(mask, BUTTON_BACK)) {
	    globeMovement = ! globeMovement;
	    sl.setGlobeMode(globeMovement);
	    Debug.log("Globe control is now " + ((globeMovement) ? "on" : "off"));
	}

	/* In Globe Mode, B activates or disactivates follow mode */
	if (globeMovement && buttonIsSet(mask, BUTTON_B))
	    ui.lc.toggleFollow();
    }

    /** Get the value of a joystick axis, filtering out noisy results 
     *  @param axis The index of the axis to check
     *  @return A number from -1 to 1 representing the value of the joystick */
    public float getAxis(int axis) {
	if (Math.abs(axes[axis].getPollData()) < 0.2)
	    return 0;
	else
	    return 1.25F * (axes[axis].getPollData() - 0.2F);
    }

    /** Trigger events based on the values of the axes */
    private void axesAction() {
	if (globeMovement) {
	    float zoom = getAxis(AXIS_L_TRIGGER) - getAxis(AXIS_R_TRIGGER);
	    ui.lc.moveView(getAxis(AXIS_L_H), getAxis(AXIS_L_V), 
			   zoom, getAxis(AXIS_R_V), getAxis(AXIS_R_H));
	} else {
	    double[] vels = new double[3];
	    vels[0] = getAxis(AXIS_L_H);
	    vels[1] = getAxis(AXIS_L_V);
	    vels[2] = getAxis(AXIS_R_V);
		
	    boolean updateVec = false;
	    if (System.currentTimeMillis() - lastAxesUpdate > minDiff) {
		updateVec = true;
	    }
		
	    if (updateVec) {
		lastAxesUpdate = System.currentTimeMillis();
		//3.0 is the value of the maximum normal vector
		double adjustment = EnsignCrusher.MAX_VELOCITY / Math.sqrt(3.0);
		for (double v : vels) {
		    v *= adjustment;
		}

		EnsignCrusher.manualVelocity(vels);
	    }
	}
    }

    /** Run the thread that takes input from the game pad. The thread
     *  polls the game pad every 10ms */
    public void run() {
	while (ctrl != null) {
	    ctrl.poll();

	    int mask = buttonMask();
	    int newButtons = (mask ^ lastButtonMask) & mask;
	    lastButtonMask = mask;

	    if (enabled) {
		axesAction();
		if (newButtons != 0) {
		    buttonAction(newButtons);
		}
	    }

	    try {
		Thread.sleep(POLL_PERIOD);
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	}
    }
}
