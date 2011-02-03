package org.haldean.chopper.server.nav;

import java.util.*;
import java.awt.*;

public class DrawNavList extends LinkedList<NavData> implements NavData {
    
    private boolean selected = false;
    private boolean highlighted = false;
    private boolean expanded = true;
    private String name;
    private int yRange[][] = new int[2][2];
    
    private DrawNavList() {
        this("Godzilla");
    }
    
    private DrawNavList(String str) {
        super();
        name = str;
    }
    
    public DrawNavList clone() {
        DrawNavList newList = fromString(toString());
        return newList;
    }
    
    public void switchExpanded() {
        expanded = !expanded;
    }
    
    public void setSelected(boolean k) {
        selected = k;
    }
    
    public void setHighlighted(boolean k) {
        highlighted = k;
    }    
    
    public int[][] getYRange() {
        return yRange.clone();
    }
    
    /**
	 * Serializes the NavList to String form.
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
    
    public void drawMe (Graphics2D g2, int xpos, int ypos, int xSize, Vector<NavData> registry) {
        yRange[0][0] = ypos;
        yRange[0][1] = ypos + 2 * FONTSIZE;
        
        if (highlighted) {
            g2.setColor(Color.CYAN);
            g2.fillRect(xpos, yRange[0][0], xSize, yRange[0][1] - yRange[0][0]);
        }
        
        if (selected) {
            g2.setColor(Color.BLUE);
            g2.fillRect(xpos, yRange[0][0], xSize, yRange[0][1] - yRange[0][0]);
        }
        
        g2.setColor(Color.RED);
        g2.drawRect(xpos, yRange[0][0], xSize, 2 * FONTSIZE);
        
        g2.setColor(Color.BLACK);
        if (expanded)
            g2.drawString(name + " Start", xpos, yRange[0][0] + FONTSIZE);
        else
            g2.drawString(name + " List", xpos, yRange[0][0] + FONTSIZE);
        
        
        //reassigned later, when the actual end of last block in the list is known, if expanded.
        yRange[1][0] = yRange[0][0];
        yRange[1][1] = yRange[0][1];
        
        registry.add(this);
        
        if (expanded) {
            ListIterator<NavData> i1 = listIterator();
            while (i1.hasNext()) {
                int lastY = getDeepestY(registry);
                i1.next().drawMe(g2, xpos, lastY + BUFFER, xSize, registry);
            }
        
            int lastY = getDeepestY(registry);
            
            yRange[1][0] = lastY + BUFFER;
            yRange[1][1] = lastY + BUFFER + 2 * FONTSIZE;
            
            if (highlighted) {
                g2.setColor(Color.CYAN);
                g2.fillRect(xpos, yRange[1][0], xSize, yRange[1][1] - yRange[1][0]);
            }
            
            if (selected) {
                g2.setColor(Color.BLUE);
                g2.fillRect(xpos, yRange[1][0], xSize, yRange[1][1] - yRange[1][0]);
            }
            
            g2.setColor(Color.RED);
            g2.drawRect(xpos, yRange[1][0], xSize, yRange[1][1] - yRange[1][0]);
            g2.setColor(Color.BLACK);
            g2.drawString(name + " End", xpos, yRange[1][0] + FONTSIZE);
        }
    }
    
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
	 * @param cs The ChopperStatus with which to construct NavDests.
	 * May be null, in which case null we be passed to the NavDest constructor. 
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
