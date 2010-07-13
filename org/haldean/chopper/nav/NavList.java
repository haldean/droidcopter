package org.haldean.chopper.nav;

import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Stack;

import org.haldean.chopper.ChopperStatus;
import org.haldean.chopper.Constants;

public class NavList extends LinkedList<NavTask> implements NavTask, Constants {

	private static final long serialVersionUID = 8506353357302343674L;

	NavTask currentTask;

	public long getInterval() {
		if (currentTask != null)
			return currentTask.getInterval();
		
		/* Should never happen, since currentTask is only null before the first call to getVelocity
		 * and after if the list is empty (i.e. this NavTask is complete).
		 */
		else
			return NAVPAUSE; //But just in case
	}

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
		 * If complete, this NavTask would be removed from its parent NavList.
		 */
		else {
			hover(target);
			return;
		}
	}

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
	
	private void hover(double[] target) {
		for (int i = 0; i < 3; i++) {
			target[i] = 0;
		}
		if (ChopperStatus.readingLock[AZIMUTH].tryLock()) {
			target[3] = ChopperStatus.reading[AZIMUTH];
			ChopperStatus.readingLock[AZIMUTH].unlock();
		}
	}
	
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
	
	public static NavList fromString(String msg) {
		String[] tokens = msg.split(" ");
		Stack<NavTask> myStack = new Stack<NavTask>();
		for (int i = 0; i < tokens.length; i++){
			if (!tokens[i].endsWith("}")) {
				NavTask myTask = null;
				if (tokens[i].startsWith("DEST")) {
					myTask = new NavDest(tokens[i]);
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
}
