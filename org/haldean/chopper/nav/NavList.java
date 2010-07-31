package org.haldean.chopper.nav;

import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Stack;

import org.haldean.chopper.ChopperStatus;
import org.haldean.chopper.Constants;

import android.util.Log;

/**
 * Holds a list of NavTasks in ordered form.
 * @author Benjamin Bardin
 */
public class NavList extends LinkedList<NavTask> implements NavTask, Constants {
	
	/** Tag for logging */
	public static final String TAG = "nav.NavList";
	
	/** Version ID */
	private static final long serialVersionUID = 1L;
	
	/** Task currently being performed */
	private NavTask currentTask;
	
	/**
	 * Creates a NavList.
	 */
	public NavList() {
		super();
	}
	
	/**
	 * Deserializes a NavList from valid serialized String form.
	 * @param msg Serialized form of the NavList
	 * @param cs The ChopperStatus with which to construct NavDests.
	 * May be null, in which case null we be passed to the NavDest constructor. 
	 * @return The newly-deserialized NavList
	 */
	public static NavList fromString(String msg, ChopperStatus cs) {
		String[] tokens = msg.split(" ");
		Stack<NavTask> myStack = new Stack<NavTask>();
		for (int i = 0; i < tokens.length; i++){
			if (!tokens[i].endsWith("}")) {
				NavTask myTask = null;
				if (tokens[i].startsWith("DEST")) {
					myTask = new NavDest(tokens[i], cs);
					myStack.push(myTask);
				}
				if (tokens[i].startsWith("VEL")) {
					myTask = new NavVel(tokens[i]);
					myStack.push(myTask);
				}
				if (tokens[i].startsWith("{")) {
					myStack.push(null);
				}
			}
			else {
				NavList myList = new NavList();
				NavTask myTask;
				while ((myTask = myStack.pop()) != null)
					myList.addFirst(myTask);
				myStack.push(myList);
			}
		}
		if (myStack.empty())
			return null;
		else
			return (NavList) myStack.pop();
	}
	
	/**
	 * Get desired time until next calculation of target velocity vector.
	 */
	public long getInterval() {
		if (currentTask != null)
			return currentTask.getInterval();
		
		/* Should never happen, since currentTask is only null before the first call to getVelocity
		 * and after if the list is empty (i.e. this NavTask is complete). */
		else {
			System.out.println("Navigation has bad manners.");
			return NAVPAUSE; //But just in case
		}
	}
	
	/**
	 * Calculates the target velocity vector.  
	 * @param target The array in which to write the velocity vector.  Length must be at least 4.
	 */
	public void getVelocity(double[] target) {
		while (size() > 0) {
			currentTask = getFirst();
			if (currentTask.isComplete()) {
				currentTask = null;
				removeFirst();
			}
			else
				break;
		}
		
		if (currentTask != null)
			currentTask.getVelocity(target);
		
		/* The following else should never happen, since getVelocity() should always be preceded with isComplete().
		 * If complete, this NavTask would be removed from its parent NavList. */
		else {
			Log.wtf(TAG, "Completed task not removed from NavList");
		}
	}
	
	/**
	 * Returns true if each task in the NavList has been completed (in order).
	 */
	public boolean isComplete() {
		ListIterator<NavTask> iterator = listIterator();
		while (iterator.hasNext()) {
			if (!iterator.next().isComplete()) //if the task is incomplete, return false
				return false;
			else //if it is complete, remove it
				iterator.remove();
		}
		return true; //if all tasks are complete, return true;
	}
	
	/**
	 * Serializes the NavList to String form.
	 */
	public String toString() {
		String me = new String();
		me = me.concat(" {");
		ListIterator<NavTask> iterator = listIterator();
		while (iterator.hasNext()) {
			me = me.concat(" ").concat(iterator.next().toString());
		}
		me = me.concat(" }");
		return me;
	}
}
