package org.haldean.chopper.server.nav;

import javax.swing.*;
import java.util.*;
import java.awt.*;
import org.haldean.chopper.nav.*;

/**
 * Stores, draws NavVel data.
 */
public class DrawNavVel extends NavVel implements DrawNav {
	private boolean selected = false;
    private boolean highlighted = false;
    
    /* yRange[0][0] is the start point of the on-screen representation, and yRange[0][1] is the end point.*/
    private int yRange[][] = new int[1][2];
    
    /**
     * This is here so that we can use the needlessly static NavVel builder.
     */
	private DrawNavVel() {
        type = "VEL";
	}
    
    
    public DrawNavVel(NavVel source) {
        type = "VEL";
        name = source.getName();
        mData = source.getData();
    }
    
     /**
     * Clones the DrawNavVel
     */
    public DrawNavVel clone() {
        return DrawNavVel.taskFor(getVelocity(), getTime());
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
    public void drawMe (Graphics2D g2, int xpos, int ypos, int xSize, Vector<DrawNav> registry) {
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
        double[] velocity = getVelocity();
        for (int i = 0; i < 3; i++) {
            velStr = velStr.concat(Double.toString(velocity[i]) + " ");
        }
        g2.drawString(velStr, xpos, ypos + 2 * FONTSIZE);
        g2.drawString(Double.toString(velocity[3]), xpos, ypos + 3 * FONTSIZE);
        g2.drawString( Double.toString( getTime()), xpos, ypos + 4 * FONTSIZE);
        
        
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
    public static DrawNavVel taskFor(double[] velocities, double timeToExecute) {
		DrawNavVel n = new DrawNavVel();
        for (int i = 0; i < 4; i++) {
            n.mData[i] = velocities[i];
        }
		n.mData[4] = timeToExecute;
        //n.mData[6] = ID#;
		return n;
	}
}

