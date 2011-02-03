package org.haldean.chopper.server.nav;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import org.haldean.chopper.nav.NavDestData;

public class DrawNavDest extends NavDestData implements NavData {
    private boolean selected = false;
    private boolean highlighted = false;
    private int yRange[][] = new int[1][2];
        
    public DrawNavDest(String str) {
        super(str);
    }
    
    public DrawNavDest clone() {
        return new DrawNavDest(toString());
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
        g2.drawString(Double.toString(altitude), xpos, ypos + 2 * FONTSIZE);
        g2.drawString(Double.toString(longitude), xpos, ypos + 3 * FONTSIZE);
        g2.drawString(Double.toString(latitude), xpos, ypos + 4 * FONTSIZE);
        
        
        g2.setColor(Color.RED);
        g2.drawRect(xpos, ypos, xSize, myHeight);

        registry.add(this);
    }
    
    /**
	 *  Create a NavDest for a given set of parameters.
	 *
	 *  @param altitude The target altitude.
	 *  @param longitude The target longitude.
	 *  @param latitude The target latitude.
	 *  @param velocity The velocity at which to move to the target.
	 *  @param destDist The radius around the target at which we
	 *  will consider this task complete.
	 */
	public static DrawNavDest taskFor(double altitude, double longitude, double latitude,
				      double velocity, double destDist) {
		DrawNavDest n = new DrawNavDest();
		n.altitude = altitude;
		n.longitude = longitude;
		n.latitude = latitude;
		n.myVelocity = velocity;
		n.destDist = destDist;
		return n;
	}

	private DrawNavDest() {
		/* This is here so that we can use the static NavDest builder. */
	}
}
