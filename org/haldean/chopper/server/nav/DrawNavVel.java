package org.haldean.chopper.server.nav;

import javax.swing.*;
import java.util.*;
import java.awt.*;
import org.haldean.chopper.nav.NavVelData;

public class DrawNavVel extends NavVelData implements NavData {
	private boolean selected = false;
    private boolean highlighted = false;
    private int yRange[][] = new int[1][2];
    
    public DrawNavVel clone() {
        return new DrawNavVel(toString());
    }
	
    public DrawNavVel(String str) {
        super(str);
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
    
    public static DrawNavVel taskFor(double[] velocities, long timeToExecute) {
		DrawNavVel n = new DrawNavVel();
		n.velocity = velocities;
		n.timeToExecute = timeToExecute;
		return n;
	}

	private DrawNavVel() {
		/* This is here so that we can use the static NavVel builder. */
	}
}

