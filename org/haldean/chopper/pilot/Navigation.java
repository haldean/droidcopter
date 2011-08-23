package org.haldean.chopper.pilot;

public interface Navigation {

	/** Tag for logging */
	public static final String TAG = "chopper.Navigation";
	/** How long (in ms) NavigationImpl should instruct the chopper to hover
	 * when autopilot has run out of NavTasks */
	public static final int HOVER_PAUSE = 1000;
	public static final String THREE_HRS_IN_MS = "10800000";

	/** 
	 * Evaluates a new navigation vector, based on current status and the relevant NavTask.
	 * @param newNavTarget If supplied and has length >= 4, writes the new target here.  May be null.
	 */
	public abstract void evalNextVector(double[] newNavTarget);

	/**
	 * Writes current navigation target vector into supplied
	 * array.  If the data is locked, immediately returns with
	 * neither data update nor warning.
	 *
	 * @param expectedValues The array in which to write the
	 * vector--must be at least of length 4.
	 */
	public abstract void getTarget(double[] navTarget);

	/**
	 * Obtains all scheduled flight plans
	 * @return An array of strings representing all flight plans (serialized form)
	 */
	public abstract String[] getTasks();

	/**
	 * Receive a message.
	 * @param source The source of the message, if a reply is needed.  May be null.
	 */
	public abstract void receiveMessage(String msg, Receivable source);

	/**
	 * Registers a receiver to receive Nav updates.
	 * @param rec
	 */
	public abstract void registerReceiver(Receivable rec);

}