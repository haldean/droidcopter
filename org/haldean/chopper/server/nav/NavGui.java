package org.haldean.chopper.server.nav;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

import org.haldean.chopper.server.EnsignCrusher;
import org.haldean.chopper.server.StyleProvider;

/**
 * Maintains a GUI of AutoPilot data, so it can be graphically manipulated.
 */
public class NavGui extends JComponent implements MouseListener, MouseMotionListener {
    /* Constants */
    public static final int PLANS = 3; //Number of NavLists to maintain
    public static final int BUFFER = 10; //For leaving empty space between drawn items
    public int xSize;
    
    /* Information on a selected DrawNav */
    private DrawNav selected = null;
    private DrawNavList taskParent = null;
    
    /* Highlighted DrawNav */
    private DrawNav highlighted = null;
    
    /* Highlighted slots */
    private DrawNavList parentList = null;
    private int listSlot = 0;
    private int slotCol = 0;
    
    /* Selected slots */
    private DrawNavList selParList = null;
    private int selListSlot = 0;
    private int selSlotCol = 0;
    
    /* Stores the NavLists */
    private Vector<DrawNavList> travelPlans = new Vector<DrawNavList>(PLANS);
    
    /* Registry of all items and their positions in the component */
    private Vector<Vector<DrawNav>> registry = new Vector<Vector<DrawNav>>(travelPlans.size());
    
    /**
     * Constructs the Component, initializes the core NavLists and the registry of DrawNavs
     */
    public NavGui() {
        super();
        travelPlans.add(DrawNavList.fromString("{ Name1}"));
        travelPlans.add(DrawNavList.fromString("{ Name2}"));
        travelPlans.add(DrawNavList.fromString("{ Name3}"));
        System.out.println(0 + " " + travelPlans.get(0));
        for (int i = 0; i < travelPlans.size(); i++) {
            registry.add(new Vector<DrawNav>());
        }

        addMouseMotionListener(this);
        addMouseListener(this);
    }
    
    /**
     * Unhighlights the currently highlighted object, if any.
     */
    private void clearHighlighted() {
        if (highlighted != null) {
            highlighted.setHighlighted(false);
            highlighted = null;
        }            
    }

    /**
     * Deselects the currently selected object, if any.
     */
    private void clearSelected() {
        if (selected != null) {
            selected.setSelected(false);
            selected = null;
            taskParent = null;
        }
    }
    
    /**
     * If both a DrawNav and a slot are selected, copies the former to the latter.
     */
    public void copySelection() {
        if ((selected == null) || (selParList == null))
            return;
        DrawNav copyMe = selected.clone();
        selParList.add(selListSlot, copyMe);
        repaint();
        revalidate();
    }

    /**
     * Sends the selected command to the chopper.
     */
    public void makeItSo() {
	if (selected == null) 
	    return;

	EnsignCrusher.makeItSo(selected);
    }
    
    /**
     * If a DrawNav is selected, deletes it.
     */
    public void deleteSelection() {
        if (taskParent == null)
            return;
        taskParent.remove(selected);
        selected = null;
        taskParent = null;
        selParList = null;
        repaint();
        revalidate();
    }
    
    /**
     * If both a DrawNav and a slot not contained by the DrawNav are selected, moves the former to the latter.
     */
    public void moveSelection() {
        if (taskParent == null)
            return;
        copySelection();
        deleteSelection();
        repaint();
        revalidate();
    }
    
    /**
     * Gets the tasks stored by the GUI.
     * @return An array of serialized DrawNavLists.
     */
    public String[] getLists() {
        String[] mTasks = new String[travelPlans.size()];
        for (int i = 0; i < mTasks.length; i++) {
            mTasks[i] = travelPlans.get(i).toString();
        }
        return mTasks;
    }
    
    /**
     * Sets the specified list to the supplied string.
     * @param i The number of the list to overwrite.
     * @param str The string from which to deserialize the DrawNavList.
     */
    public void setList(int i, String str) {
        if ((i >= travelPlans.size()) || (i < 0))
            return;
        travelPlans.set(i, DrawNavList.fromString(str));
        repaint();
        revalidate();
    }
    
    /**
     * If a slot is selected, copies the supplied DrawNavDest and inserts the copy at the slot.
     * @param dest the DrawNavDest to insert.
     */
    public void insertNavDest(DrawNavDest dest) {
        if (selParList == null)
            return;
        if (dest == null)
            System.out.println("null dest");
        selParList.add(selListSlot, dest.clone());
        repaint();
        revalidate();
    }
    
    /**
     * If a slot is selected, copies the supplied DrawNavVel and inserts the copy at the slot.
     * @param dest the DrawNavVel to insert.
     */
    public void insertNavVel(DrawNavVel vel) {
        if (selParList == null)
            return;
        selParList.add(selListSlot, vel.clone());
        repaint();
        revalidate();
    }
    
    /**
     * If a slot is selected, creates a DrawNavList with the supplied name at the slot.
     * @param name The name of the new DrawNavList.
     */
    public void insertNavList(String name) {
        if (selParList == null) {
            System.out.println("null");
            return;
        }
        selParList.add(selListSlot, DrawNavList.fromString("{ " + name + "}"));
        repaint();
        revalidate();
    }
    
    /**
     * Paints the component
     * @param g The supplied graphics object
     */
    public void paint(Graphics g) {
        super.paint(g);
        Graphics2D g2 = (Graphics2D) g;
	
	Dimension size = getSize();

	g2.setColor(StyleProvider.background());
	g2.fillRect(0, 0, (int) size.getWidth(), (int) size.getHeight());

	xSize = (int) (size.getWidth() / travelPlans.size()) - 3 * (int) BUFFER;

        for (int i = 0; i < travelPlans.size(); i++) {
            //clear the registry
            registry.get(i).removeAllElements();
            //recursively draw each element in the list
            travelPlans.get(i).drawMe(g2, i * xSize + BUFFER, BUFFER, xSize - 2 * BUFFER, registry.get(i));
        }
        
        //draw highlighted slot, if one exists:
        if (parentList != null) {
            int yPos;
            int[][] yRange;
            if (listSlot == 0) {
                yRange = parentList.getYRange();
                yPos = yRange[0][1];
            }
            else {
                yRange = parentList.get(listSlot - 1).getYRange();
                yPos = yRange[yRange.length - 1][1];
            }
            yPos += .375 * DrawNav.BUFFER;
            g2.setColor(StyleProvider.foreground3());
            g2.fillRect(slotCol * xSize + BUFFER, yPos, xSize - 2 * BUFFER, (int) (.25 * DrawNav.BUFFER));
        }
        
        //draw selected slot, if one exists:
        if (selParList != null) {
            int yPos;
            int[][] yRange;
            if (selListSlot == 0) {
                yRange = selParList.getYRange();
                yPos = yRange[0][1];
            }
            else {
                yRange = selParList.get(selListSlot - 1).getYRange();
                yPos = yRange[yRange.length - 1][1];
            }
            yPos += .375 * DrawNav.BUFFER;
            g2.setColor(StyleProvider.foreground2());
            g2.fillRect(selSlotCol * xSize + BUFFER, yPos, xSize - 2 * BUFFER, (int) (.25 * DrawNav.BUFFER));
        }
        
        //Update the size, so the containing JScrollPane doesn't flip out.
        setPreferredSize(new Dimension(PLANS * xSize, getDeepestY() + BUFFER));
    }
    
    /**
     * Selects, deselects items as per click.
     * @param e The MouseEvent to process.
     */
    public void mouseClicked(MouseEvent e) {
        DrawNav myTask = findTask(e.getX(), e.getY());
        if (e.getClickCount() == 2) {
            if ((myTask != null) && (myTask instanceof DrawNavList)) {
                ((DrawNavList) myTask).switchExpanded();
                repaint();
                revalidate();
            }
            return;
        }
        else if (myTask == null) {
            DrawNavList newList = findParentList(e.getX(), e.getY());
            if ((selParList == newList) && (selListSlot == listSlot)) {
                selParList = null;
                repaint();
                return;
            }
            else if (newList != null) {
                selParList = newList;
                selListSlot = findSlotInList(parentList, e.getY());
                selSlotCol = e.getX() / xSize;
                repaint();
                return;
            }
        }
        else if (myTask == selected) {
            clearSelected();
            repaint();
            return;
        }
        else {
            //new task selected:
            clearSelected();
            selected = myTask;
            taskParent = findParentList(e.getX(), e.getY());
            myTask.setSelected(true);
            repaint();
            return;
        }
    }
    
    /**
     * Tracks the cursor to highlight the appropriate object.
     * @param e The MouseEvent to process.
     */
    public void mouseMoved(MouseEvent e) {
        DrawNav myTask = findTask(e.getX(), e.getY());
        clearHighlighted();
        if (myTask == null) {
            parentList = findParentList(e.getX(), e.getY());
            if (parentList != null) {
                listSlot = findSlotInList(parentList, e.getY());
                slotCol = e.getX() / xSize;
            }
            repaint();
            return;
        }
        else {
            parentList = null;
            highlighted = myTask;
            myTask.setHighlighted(true);
            repaint();
        }
    }
    
    /**
     * Unused.  Implemented to override abstract method.
     * @param e The MouseEvent to ignore.
     */
    public void mouseDragged(MouseEvent e) {
        //Nothing to do here
    }
    
    /**
     * Unused.  Implemented to override abstract method.
     * @args e The MouseEvent to ignore.
     */
    public void mouseEntered(MouseEvent e) {
        //Nothing to do here
    }
    
    /**
     * Unused.  Implemented to override abstract method.
     * @param e The MouseEvent to ignore.
     */
    public void mouseExited(MouseEvent e) {
        //Nothing to do here
    }
    
    /**
     * Unused.  Implemented to override abstract method.
     * @param e The MouseEvent to ignore.
     */
    public void mousePressed(MouseEvent e) {
        //Nothing to do here
    }
    
    /**
     * Unused.  Implemented to override abstract method.
     * @param e The MouseEvent to ignore.
     */
    public void mouseReleased(MouseEvent e) {
        //Nothing to do here
    }
    
    /**
     * Finds the DrawNav that countains the given point, if any. May return null.
     * @param mx The x coordinate of the point to examine.
     * @param my The y coordinate of the point to examine.
     * @return The containing DrawNav.
     */
    private DrawNav findTask (int mX, int mY) {
        Vector<DrawNav> myList = getColRegistry(mX, mY);
        if (myList == null)
            return null;
        for (int i = 0; i < myList.size(); i++) {
            int[][] yRange = myList.get(i).getYRange();
            for (int j = 0; j < yRange.length; j++) {
                if ((mY > yRange[j][0]) && (mY < yRange[j][1]))
                    return myList.get(i);
            }
        }
        return null;
    }
    
    /**
     * Examines the registry, returns the lower-bound Y-value of the lowest DrawNav.
     * Used to resize the component as necessary.
     * @return The deepest y-value.
     */
    private int getDeepestY() {
        ListIterator<Vector<DrawNav>> i1 = registry.listIterator();
        int maxY = 0;
        while (i1.hasNext()) {
            Vector<DrawNav> mReg = i1.next();
            for (int i = 0; i < mReg.size(); i++) {
                int[][] mRange = mReg.get(i).getYRange();
                for (int j = 0; j < mRange.length; j++) {
                    maxY = Math.max(maxY, mRange[j][1]);
                }
            }
        }
        return maxY;
    }
    
    /**
     * Finds the nearest DrawNavList that countains the given point, if any. May return null.
     * @param mx The x coordinate of the point to examine.
     * @param my The y coordinate of the point to examine.
     * @return The containing DrawNavList.
     */
    private DrawNavList findParentList(int mX, int mY) {
        Vector<DrawNav> myList = getColRegistry(mX, mY);
        if (myList == null)
            return null;
        DrawNavList myParent = null;
        for (int i = 0; i < myList.size(); i++) {
            if (myList.get(i) instanceof DrawNavList) {
                    int[][] yRange = myList.get(i).getYRange();
                if ( (yRange[0][1] < mY) && (yRange[1][0] > mY) ) {
                    if (myParent == null)
                        myParent = (DrawNavList) myList.get(i);
                    else {
                        if (yRange[0][1] > myParent.getYRange()[0][1])
                            myParent = (DrawNavList) myList.get(i);
                    }
                }
            }
        }
        return myParent;
    }
    
    /**
     * Returns the index in the supplied NavList to which the supplied y-value corresponds.
     * @param parent the DrawNavList to examine
     * @param my The y-value
     * @return The index in the list.
     */
    private int findSlotInList(DrawNavList parent, int mY) {
        ListIterator<DrawNav> i1 = parent.listIterator();
        int prevElts = 0;
        while (i1.hasNext()) {
            int[][] yRange = i1.next().getYRange();
            if (yRange[0][1] < mY)
                prevElts++;
        }
        return prevElts;
    }
    
    /**
     * Obtains the registry corresponding to the column in which the supplied point is found.
     * @param mX The x-value of the point to examine.
     * @param mY The y-value of the point to examine.
     * @return The appropriate column's registry.
     */
    private Vector<DrawNav> getColRegistry(int mX, int mY) {
        int mXr = mX % xSize;
        if (mXr < BUFFER)
            return null;
        if (mXr > xSize - BUFFER)
            return null;
        int col = (int) mX / xSize;
        if (col >= registry.size())
            return null;
        return registry.get(col);
    }
}
