package org.haldean.chopper.server.nav;

import javax.swing.*;
import java.util.*;
import java.awt.*;
import org.haldean.chopper.nav.NavVelData;

/**
 * Stores, draws NavVel data.
 */
public class DrawNavVel extends NavVelData implements NavData {
	private boolean selected = false;
    private boolean highlighted = false;
    
    /* yRange[0][0] is the start point of the on-screen representation, and yRange[0][1] is the end point.*/
    private int yRange[][] = new int[1][2];
    
    /**
     * This is here so that we can use the needlessly static NavVel builder.
     */
	private DrawNavVel() {
	}
    
    /**
     * Constructs a DrawNavVel by deserializing the supplied string.
     * @param str
     */
    public DrawNavVel(String str) {
        super(str);
    }
    
    /**
     * Clones the DrawNavVel
     */
    public DrawNavVel clone() {
        return new DrawNavVel(toString());
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
     * Obtains the 2D array of the y-values the object occupied when last drawn to screen.
     * @return The array.
     */
    public int[][] getYRange() {
        return yRange.clone();
    }
    
    /**
     * Draws the task.
     * @param g2 The object to which to draw the task.
     * @param xpos The x-coordinate at which to start drawing the task.
     * @param ypos The y-coordinate at which to start drawing the task.
     * @param xSize The permissible width of the task.
     * @param registry The registry of currently drawn NavDatas, to which to add this task.
     */
    public void drawMe (Graphics2D g2, int xpos, int ypos, int xSize, Vector<NavData> registry) {
        int myHeight = 5 * FONTSIZE;
        yRange[0][0] = ypos;
        yRange[0][1] = ypos + myHeight;

        if (highlighted) {
            g2.setColor(Color.CYAN);
            g2.fillRect(xpos, ypos, xSize, myHeight);
        }
        
        if (selected) {
            g2.setColor(Color.BLUE);
            g2.fillRect(xpos, ypos, xSize, myHeight);
        }

        g2.setFont(globalFont);        
        g2.setColor(Color.BLACK);
        g2.drawString(name, xpos, ypos + FONTSIZE);
        String velStr = "";
        for (int i = 0; i < 3; i++) {
            velStr = velStr.concat(Double.toString(velocity[i]) + " ");
        }
        g2.drawString(velStr, xpos, ypos + 2 * FONTSIZE);
        g2.drawString(Double.toString(velocity[3]), xpos, ypos + 3 * FONTSIZE);
        g2.drawString(Long.toString(timeToExecute), xpos, ypos + 4 * FONTSIZE);
        
        
        g2.setColor(Color.RED);
        g2.drawRect(xpos, ypos, xSize, myHeight);

        registry.add(this);
    }
    
    /**
     * Creates a DrawNavVel with the supplied parameters.
     * @param velocities The target velocity, in form [dx dy dz theta].
     * @param timeToExecute How long to maintain this velocity.
     * @return The DrawNavVel.
     */
    public static DrawNavVel taskFor(double[] velocities, long timeToExecute) {
		DrawNavVel n = new DrawNavVel();
		n.velocity = velocities;
		n.timeToExecute = timeToExecute;
		return n;
	}
}

