package org.haldean.chopper.nav;

import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Stack;

public class NavList extends NavData {
	protected LinkedList<NavData> mList;
    private NavData mCurTask;
    
    public NavList() {
        mData = new double[2];
        mList = new LinkedList<NavData>();
    }
    
	/**
	 * Deserializes a NavList from valid serialized String form.
	 * @param msg Serialized form of the NavList
	 * @param cs The ChopperStatus with which to construct NavDests.
	 * May be null, in which case null we be passed to the NavDestReader constructor. 
	 */
	
	public static NavList fromString(String str) {
		String[] tokens = str.split(" ");
		Stack<NavData> myStack = new Stack<NavData>();
		for (int i = 0; i < tokens.length; i++){
			if (!tokens[i].endsWith("}")) {
				NavData myTask = null;
				if (tokens[i].startsWith("{")) {
					myStack.push(null);
				}
				else if (!tokens[i].equals("")){
					myTask = NavData.fromString(tokens[i]);
					myStack.push(myTask);
				}
			}
			else {
				NavList myList = new NavList();
				try {
					myList.name = tokens[i].substring(0, tokens[i].length() - 1);
				}
				catch (Exception e) {
					e.printStackTrace();
				}
				NavData myTask;
				while ((myTask = myStack.pop()) != null)
					myList.mList.addFirst(myTask);
				myStack.push(myList);
			}
		}
		if (!myStack.isEmpty()) {
			NavList myList = (NavList) myStack.pop();
			myList.mCurTask = myList.firstTask();
			return myList;
		}
		else {
			return null;
		}
	}
	
	public NavData getCurrentTask() {
		return mCurTask;
	}
	
    private void removeFirstTask() {
        //For the top-level call
        if (mList.isEmpty()) {
            return;
        }
        
        if (mList.getFirst() instanceof NavList) {
            NavList recurse = (NavList) mList.getFirst();
            recurse.removeFirstTask();
            if (recurse.mList.isEmpty()) {
                mList.removeFirst();
            }
        }
        else {
            mList.removeFirst();
            mCurTask = null;
        }
    }
    
    public LinkedList<NavData> copyList() {
    	NavList newList = fromString(toString());
    	return newList.mList;
    }
    
    public NavData nextTask() {
        removeFirstTask();
        mCurTask = firstTask();
        return mCurTask;
	}
	
	private NavData firstTask() {
		if (mList.isEmpty()) {
            return null;
        }
        
        mCurTask = mList.getFirst();
        while (mCurTask instanceof NavList) {
            mCurTask = ((NavList) mCurTask).mList.getFirst();
        }
        return mCurTask;
	}
	
	public double getID() {
		return mData[0];
	}
	
	/**
	 * Serializes the NavList to String form.
	 */
	public String toString() {
		String me = new String();
		me = me.concat(" {");
		//ListIterator<NavData> iterator = mList.listIterator();
		ListIterator<NavData> iterator = mList.listIterator();
		while (iterator.hasNext()) {
			me = me.concat(" ").concat(iterator.next().toString());
		}
		me = me.concat(" " + name + "}");
		return me;
	}
	
	public String getName() {
		return name;
	}
}