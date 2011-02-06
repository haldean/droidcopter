package org.haldean.chopper.server.nav;

import java.awt.*;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Stack;
import java.util.Vector;

import org.haldean.chopper.server.StyleProvider;

/**
 * Stores, draws NavList data.
 */
public class DrawNavList extends LinkedList<NavData> implements NavData {
    private static final int TICK_LENGTH = 4;

    private boolean selected = false;
    private boolean highlighted = false;
    private boolean expanded = true;
    private String name;
    
    /** yRange[0] is the list-start block, and yRange[1] is the list-end block
     *  yRange[i][0] is the start point of the on-screen representation, and yRange[i][1] is the end point.
     */
    private int yRange[][] = new int[2][2];
    
    /**
     * Constructs the list.
     */
    private DrawNavList() {
        this("Navigation_List");
    }
    
    /**
     * Constructs the list.
     * @param str The name of the list.
     */
    private DrawNavList(String str) {
        super();
        name = str;
    }
    
    /**
     * Clones the list.
     */
    public DrawNavList clone() {
        DrawNavList newList = fromString(toString());
        return newList;
    }
    
    /**
     * Determines if the task is selected.
     * @param k The desired selection-state.
     */
    public void setSelected(boolean k) {
        selected = k;
    }
    
    /**
     * Determines if the task is highlighted.
     * @param k The desired highlighted-state.
     */
    public void setHighlighted(boolean k) {
        highlighted = k;
    }    
    
    /**
     * If the task is expanded, de-expands it; if it's not expanded, expands it.
     */
    public void switchExpanded() {
        expanded = !expanded;
    }
    
    /**
     * Obtains the 2D array of the y-values the object occupied when last drawn to screen.
     * yRange[0][0] is the start point, and yRange[0][1] is the end point.
     * @return The array.
     */
    public int[][] getYRange() {
        return yRange.clone();
    }
    
    /**
     * Serializes the DrawNavList to String form.
     */
    public String toString() {
	String me = new String();
	me = me.concat(" {");
	ListIterator<NavData> iterator = listIterator();
	while (iterator.hasNext()) {
	    me = me.concat(" ").concat(iterator.next().toString());
	}
	me = me.concat(" " + name + "}");
	return me;
    }
    
    /**
     * Draws the list and, recursively, all its elements.
     *
     * @param g2 The object to which to draw the list.
     * @param xpos The x-coordinate at which to start drawing the list.
     * @param ypos The y-coordinate at which to start drawing the list.
     * @param xSize The permissible width of the list.
     * @param registry The registry of currently drawn NavDatas, to which to add this list.
     */
    public void drawMe (Graphics2D g2, int xpos, int ypos, int xSize, Vector<NavData> registry) {
        //list-start block:
        yRange[0][0] = ypos;
        yRange[0][1] = ypos + 2 * FONTSIZE;
        
        if (highlighted) {
            g2.setColor(StyleProvider.foreground3());
            g2.fillRect(xpos, yRange[0][0], xSize, yRange[0][1] - yRange[0][0]);
        }
        
        if (selected) {
            g2.setColor(StyleProvider.foreground());
            g2.fillRect(xpos, yRange[0][0], xSize, yRange[0][1] - yRange[0][0]);
        }
        
            
	g2.setColor(selected ? StyleProvider.background() : StyleProvider.foreground());
	g2.drawString(name.replaceAll("_", " ") + " List", xpos, yRange[0][0] + FONTSIZE);
        
        
        //if expanded, reassigned later, when the actual end of last block in the list is known.
        yRange[1][0] = yRange[0][0];
        yRange[1][1] = yRange[0][1];
        
        registry.add(this);
        
        //Draw the lists' objects
        if (expanded) {
            ListIterator<NavData> i1 = listIterator();
            while (i1.hasNext()) { //iterate through each object
                int lastY = getDeepestY(registry); //find the position of the previous task
                i1.next().drawMe(g2, xpos, lastY + BUFFER, xSize, registry); //draw this item there.
            }
            
            //Draw the list-end block:
            int lastY = getDeepestY(registry);
            
            yRange[1][0] = lastY + BUFFER;
            yRange[1][1] = lastY + BUFFER + 2 * FONTSIZE;
            
            if (highlighted) {
                g2.setColor(StyleProvider.foreground3());
                g2.fillRect(xpos, yRange[1][0], xSize, yRange[1][1] - yRange[1][0]);
            }
            if (selected) {
                g2.setColor(StyleProvider.foreground());
                g2.fillRect(xpos, yRange[1][0], xSize, yRange[1][1] - yRange[1][0]);
            }
        }
	
	/* Top marker */
	g2.setColor(StyleProvider.foreground());
	g2.drawLine(xpos - 2, ypos - 2, xpos + xSize + 2, ypos - 2);
	g2.drawLine(xpos - 2, ypos + TICK_LENGTH, xpos - 2, ypos - 2);
	g2.drawLine(xpos + xSize + 2, ypos + TICK_LENGTH, xpos + xSize + 2, ypos - 2);

	/* Bottom marker */
	g2.drawLine(xpos - 2, yRange[1][1] + 1, xpos + xSize + 2, yRange[1][1] + 1);
	g2.drawLine(xpos - 2, yRange[1][1] - TICK_LENGTH, xpos - 2, yRange[1][1] + 1);
	g2.drawLine(xpos + xSize + 2, yRange[1][1] - TICK_LENGTH, xpos + xSize + 2, yRange[1][1] + 1);
    }
    
    /**
     * Returns the end-position of the deepest task in the supplied registry.
     * @param registry The registry to examine.
     * @return The y-position of the deepest task.
     */
    private static int getDeepestY(Vector<NavData> registry) {
        int maxY = 0;
        for (int i = 0; i < registry.size(); i++) {
            int[][] mRange = registry.get(i).getYRange();
            for (int j = 0; j < mRange.length; j++) {
                maxY = Math.max(maxY, mRange[j][1]);
            }
        }
        return maxY;
    }
    
    /**
	 * Deserializes a NavList from valid serialized String form.
	 * @param msg Serialized form of the NavList
	 * @return The newly-deserialized NavList
	 */
	public static DrawNavList fromString(String msg) {
		String[] tokens = msg.split(" ");
		Stack<NavData> myStack = new Stack<NavData>();
		for (int i = 0; i < tokens.length; i++){
			if (!tokens[i].endsWith("}")) {
				NavData myTask = null;
				if (tokens[i].startsWith("DEST")) {
					myTask = new DrawNavDest(tokens[i]);
					myStack.push(myTask);
				}
				if (tokens[i].startsWith("VEL")) {
					myTask = new DrawNavVel(tokens[i]);
					myStack.push(myTask);
				}
				if (tokens[i].startsWith("{")) {
					myStack.push(null);
				}
			}
			else {
				DrawNavList myList = new DrawNavList(tokens[i].substring(0, tokens[i].length() - 1));
				NavData myTask;
				while ((myTask = myStack.pop()) != null)
					myList.addFirst(myTask);
				myStack.push(myList);
			}
		}
		if (myStack.empty()) {
			return null;
		}
		else {
			NavData result = myStack.pop();
			if (result instanceof DrawNavList) {
				return (DrawNavList) result;
			}
			else {
				DrawNavList mL = new DrawNavList();
				mL.add(result);
				return mL;
			}
		}
	} 
}
