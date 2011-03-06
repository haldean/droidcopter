package org.haldean.chopper.server.nav;

import java.awt.*;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Stack;
import java.util.Vector;

import org.haldean.chopper.server.StyleProvider;
import org.haldean.chopper.nav.*;

/**
 * Stores, draws NavList data.
 */
public class DrawNavList extends NavList implements DrawNav {
    private static final int TICK_LENGTH = 4;

    private boolean selected = false;
    private boolean highlighted = false;
    private boolean expanded = true;
    private String name;
    private LinkedList<DrawNav> mList;
    
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
        mList = new LinkedList<DrawNav>();
    }
    
    public DrawNavList(NavList list) {
        this(list.getName());
        ListIterator<NavData> i1 = list.copyList().listIterator();
        while (i1.hasNext()) {
            NavData data = i1.next();
            if (data instanceof NavDest) {
                mList.add(new DrawNavDest( (NavDest) data));
            }
            else if (data instanceof NavVel) {
                mList.add(new DrawNavVel( (NavVel) data));
            }
            else if (data instanceof NavList) {
                mList.add(new DrawNavList( (NavList) data));
            }
        }
    }
    
    public void add(int i, DrawNav nav) {
        mList.add(i, nav);
    }
    
    public void remove(DrawNav nav) {
        mList.remove(nav);
    }
    
    public DrawNav get(int i) {
        return mList.get(i);
    }
    
    public ListIterator<DrawNav> listIterator() {
        return mList.listIterator();
    }
    
    /**
     * Clones the list.
     */
    public DrawNavList clone() {
        System.out.println(toString());
        DrawNavList newList = fromString(toString());
        System.out.println(newList);
        return newList;
    }
    
    public String toString() {
		String me = new String();
		me = me.concat(" {");
		ListIterator<DrawNav> iterator = mList.listIterator();
		while (iterator.hasNext()) {
			me = me.concat(" ").concat(iterator.next().toString());
		}
		me = me.concat(" " + name + "}");
		return me;
	}
    
    public static DrawNavList fromString(String str) {
        return new DrawNavList(NavList.fromString(str));
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
     * Draws the list and, recursively, all its elements.
     *
     * @param g2 The object to which to draw the list.
     * @param xpos The x-coordinate at which to start drawing the list.
     * @param ypos The y-coordinate at which to start drawing the list.
     * @param xSize The permissible width of the list.
     * @param registry The registry of currently drawn NavDatas, to which to add this list.
     */
    public void drawMe (Graphics2D g2, int xpos, int ypos, int xSize, Vector<DrawNav> registry) {
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
        if (name == null)
            System.out.println("null name");
        g2.drawString(name.replaceAll("_", " ") + " List", xpos, yRange[0][0] + FONTSIZE);
        
        
        //if expanded, reassigned later, when the actual end of last block in the list is known.
        yRange[1][0] = yRange[0][0];
        yRange[1][1] = yRange[0][1];
        
        registry.add(this);
        
        //Draw the lists' objects
        if (expanded) {
            ListIterator<DrawNav> i1 = mList.listIterator();
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
    private static int getDeepestY(Vector<DrawNav> registry) {
        int maxY = 0;
        for (int i = 0; i < registry.size(); i++) {
            int[][] mRange = registry.get(i).getYRange();
            for (int j = 0; j < mRange.length; j++) {
                maxY = Math.max(maxY, mRange[j][1]);
            }
        }
        return maxY;
    }
}
