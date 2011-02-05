package org.haldean.chopper.server.nav;

import java.awt.*;
import java.util.*;

/**
 * An interface for server-side Navigation tasks.
 */
public interface NavData extends Cloneable {
	public static final int FONTSIZE = 12;
    public static final int BUFFER = 20;
    public static final Font globalFont = new Font("Helvetica", Font.PLAIN, FONTSIZE);
    
	public String toString();
    public void drawMe(Graphics2D g2, int xpos, int ypos, int xSize, Vector<NavData> registry);
    public void setSelected(boolean k);
    public void setHighlighted(boolean k);
    public int[][] getYRange();
    public NavData clone();
}

