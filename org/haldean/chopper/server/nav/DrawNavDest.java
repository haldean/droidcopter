package org.haldean.chopper.server.nav;

import javax.swing.*;
import java.awt.*;
import java.util.*;

import org.haldean.chopper.nav.*;
import org.haldean.chopper.server.StyleProvider;

/**
 * Stores, draws NavDest data
 */
 
public class DrawNavDest extends NavDest implements DrawNav {
    private boolean selected = false;
    private boolean highlighted = false;
    
    /* yRange[0][0] is the start point of the on-screen representation, and yRange[0][1] is the end point.*/
    private int yRange[][] = new int[1][2];
    
    /**
     * This is here so that we can use the needlessly static NavDest builder.
     */
    private DrawNavDest() {
        super();
    }
    
    public DrawNavDest(NavData source) {
        super();
        name = source.getName();
        mData = source.getData();
    }
    
    /**
     * Clones the DrawNavDest
     */
    public DrawNavDest clone() {
        return DrawNavDest.taskFor(name, mData[0], mData[1], mData[2], mData[3], mData[4]);
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
        
        if (selected) {
            g2.setColor(StyleProvider.foreground());
        } else if (highlighted) {
            g2.setColor(StyleProvider.foreground3());
        } else {
            g2.setColor(StyleProvider.background());
        }

        g2.fillRect(xpos, ypos, xSize, myHeight);

        g2.setColor(StyleProvider.foreground3());
        g2.drawRect(xpos, ypos, xSize, myHeight);

        g2.setFont(globalFont);        
        g2.setColor(selected ? StyleProvider.background() : StyleProvider.foreground());
        g2.drawString(name.replaceAll("_", " "), xpos + 3, ypos + FONTSIZE + 3);

        g2.setColor(selected ? StyleProvider.background() : StyleProvider.foreground2());
        g2.drawString(String.format("Alt: %.3f", getAltitude()), xpos + 3, 3 + ypos + 3 * FONTSIZE);
        g2.drawString(String.format("Position: (%.3f, %.3f)", getLongitude(), getLatitude()), xpos + 3, 3 + ypos + 2 * FONTSIZE);
        
        registry.add(this);
    }
    
    /**
	 *  Create a DrawNavDest for a given set of parameters.
	 *
	 *  @param altitude The target altitude.
	 *  @param longitude The target longitude.
	 *  @param latitude The target latitude.
	 *  @param velocity The velocity at which to move to the target.
	 *  @param destDist The radius around the target at which we
	 *  will consider this task complete.
	 *  @return The DrawNavDest.
	 */
    public static DrawNavDest taskFor(String name, double altitude, double longitude, double latitude,
				      double velocity, double destDist) {
		DrawNavDest n = new DrawNavDest();
		n.mData[0] = altitude;
		n.mData[1] = longitude;
		n.mData[2] = latitude;
		n.mData[3] = velocity;
		n.mData[4] = destDist;
        //n.mData[5] = ID#;
		n.name = name;
        if (n==null)
            System.out.println("null in taskFor");
		return n;
	}
}
