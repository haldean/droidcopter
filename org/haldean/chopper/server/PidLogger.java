package org.haldean.chopper.server;

import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;

/**
 *  Logs salient data about PID values, motor speeds and accelerations
 *  to a file for later processing and examination.
 *
 *  This class expects the pidlog variable to be set on the command
 *  line. If it is not, it will disable itself.
 *
 *  @author William Brown
 */
public class PidLogger implements MessageHook {
    private OutputStreamWriter output;
    private boolean activated;

    public PidLogger() {
	try {
	    String fileName = ServerCreator.getArgument("pidlog");
	    output = new FileWriter(fileName);
	    Debug.log("Capturing PID data to " + fileName);
	    activated = true;
	} catch (IllegalArgumentException e) {
	    Debug.log("PidLogger disabled: no output file specified.");
	    activated = false;
	    return;
	} catch (IOException e) {
	    Debug.log("PidLogger disabled: " + e.getMessage());
	    activated = false;
	    return;
	}
    }

    public boolean checkIncoming() {
	return true;
    }

    public boolean checkOutgoing() {
	return true;
    }

    public String[] processablePrefixes() {
	if (activated) {
	    return new String[] {"ACCEL", "MOTORSPEED", "GUID:PID:SET", "GUID:PID:VALUE"};
	} else {
	    return new String[0];
	}
    }

    public void process(Message message) {
	if (activated) {
	    try {
		/* If it's one of the PID messages, we don't need to
		 * know what kind it is. The following code writes
		 * "PID:loop#:param#:value" to the log, ignoring
		 * whether it was "GUID:PID:SET" or
		 * "GUID:PID:VALUE". */
		if (message.isType("GUID")) {
		    output.write("PID");
		    for (int i=3; i<message.length(); i++) {
			output.write(":" + message.getPart(i));
		    }
		    output.write("\n");
		} else {
		    Debug.log(message.message);
		    output.write(message.message + "\n");
		}

		output.flush();
	    } catch (IOException e) {
		e.printStackTrace();
	    }
	}
    }
}