package org.haldean.chopper.server;

import net.java.games.input.*;
import static net.java.games.input.Component.Identifier.Key;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import javax.swing.JOptionPane;

public class KeyboardController extends UiController {
    private boolean enable = true;
    private boolean dryRun = false;

    private ArrayList<Keyboard> keyboards;
    private HashMap<Key, Long> keypresses;
    private ServerHost ui;

    private double[] lastVelocities;
    private double theta = 0;
	
    private int POLL_PERIOD = 40;
    private int SLEEP_TIME = 500;

    public KeyboardController(ServerHost ui) {
	this.ui = ui;
	keypresses = new HashMap<Key, Long>();
	keyboards = new ArrayList<Keyboard>();

	getKeyboards();
	if (keyboards == null || keyboards.size() == 0) {
	    Debug.log("Keyboard couldn't be found. Run 'run " +
		      "enable-keyboard' to enable keyboard access.");
	    enable = false;
	} else {
	    Debug.log("Keyboard controller initialized.");
	}
    }

    private void getKeyboards() {
	keyboards = new ArrayList<Keyboard>();
	Controller[] controllers = 
	    ControllerEnvironment.getDefaultEnvironment().getControllers();
	    
	for (Controller c : controllers) {
	    if (c.getType().equals(Controller.Type.KEYBOARD)) {
		keyboards.add((Keyboard) c);
	    }
	}
    }

    public void run() {
	while (true) {
	    if (enable) {
		checkKeypresses();
	    }

	    try {
		Thread.sleep(enable ? POLL_PERIOD : SLEEP_TIME);
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	}
    }

    public void checkKeypresses() {
	for (Keyboard k : keyboards) {
	    k.poll();
	}

	double[] newVelocities = new double[3];

	/* WASD for forward-left-back-right. I miss Lisp. */
	newVelocities[0] = 
	    isKeyPressed(Key.W, true) && isKeyPressed(Key.S, true) ? 0 :
	    (isKeyPressed(Key.W, true) ? EnsignCrusher.VELOCITY :
	     (isKeyPressed(Key.S, true) ? -EnsignCrusher.VELOCITY : 0));

	newVelocities[1] =
	    isKeyPressed(Key.A, true) && isKeyPressed(Key.D, true) ? 0 :
	    (isKeyPressed(Key.D, true) ? EnsignCrusher.VELOCITY :
	     (isKeyPressed(Key.A, true) ? -EnsignCrusher.VELOCITY : 0));

	newVelocities[2] =
	    isKeyPressed(Key.Q, true) && isKeyPressed(Key.E, true) ? 0 :
	    (isKeyPressed(Key.Q, true) ? EnsignCrusher.VELOCITY :
	     (isKeyPressed(Key.E, true) ? -EnsignCrusher.VELOCITY : 0));

	/* Rotation */
	boolean newRotation = isKeyPressed(Key.R);
	if (newRotation) getRotation();

	if (! Arrays.equals(newVelocities, lastVelocities) || newRotation) {
	    setVelocity(newVelocities, theta);
	    lastVelocities = newVelocities;
	}
    }

    private void setVelocity(double[] velocities, double bearing) {
	if (dryRun) {
	    System.out.println(Arrays.toString(velocities) + ", " + bearing);
	} else {
	    EnsignCrusher.manualVelocity(velocities, bearing);
	}
    }

    private void getRotation() {
	String value = JOptionPane.
	    showInputDialog(ui, "Target bearing (in right-hand degrees from North)", "0.0");
	try {
	    Double theta = new Double(value);
	    if (theta < 0 || theta >= 360) {
		throw new NumberFormatException("Bearing must be between 0 and 360.");
	    }
	    this.theta = theta;
	} catch (NumberFormatException e) {
	    JOptionPane.showMessageDialog(ui, e.toString(), "Rotation Command Failed",
					  JOptionPane.ERROR_MESSAGE);
	} catch (NullPointerException e) {
	    /* This happens when the user left it blank. Just ignore them. */
	}
    }
    
    private boolean isKeyPressed(Key key) {
	return isKeyPressed(key, false);
    }

    private boolean isKeyReleased(Key key) {
	for (Keyboard k : keyboards) {
	    if (k.isKeyDown(key)) {
		return false;
	    }
	}

	if (keypresses.containsKey(key) && 
	    keypresses.get(key) < System.currentTimeMillis() - DEBOUNCE_TIME) {
	    keypresses.remove(key);
	    return true;
	}

	return false;
    }

    private boolean isKeyPressed(Key key, boolean allowRepeated) {
	for (Keyboard k : keyboards) {
	    if (k.isKeyDown(key)) {
		if (keypresses.containsKey(key) && !allowRepeated &&
		    keypresses.get(key) > System.currentTimeMillis() - DEBOUNCE_TIME) {
		    Debug.log("Fail.");
		    keypresses.put(key, System.currentTimeMillis());
		    return false;
		}

		keypresses.put(key, System.currentTimeMillis());
		return true;
	    }
	}
	return false;
    }

    public static void main(String args[]) {
	KeyboardController k = new KeyboardController(null);
	k.dryRun = true;
	k.run();
    }
}
