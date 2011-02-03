package org.haldean.chopper.server.nav;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import javax.swing.event.*;
import java.awt.event.*;


public class NavGui extends JComponent implements MouseListener, MouseMotionListener {
    //private static final int COL_WID = 100;
    public static final int PLANS = 3;
    public static final int BUFFER = 10;
    public static final int xSize = 140;
    
    private NavData selected = null;
    private NavData highlighted = null;
    private DrawNavList taskParent = null;
    
    /* Highlighted slots */
    private DrawNavList parentList = null;
    private int listSlot = 0;
    private int slotCol = 0;
    
    /* Selected slots */
    private DrawNavList selParList = null;
    private int selListSlot = 0;
    private int selSlotCol = 0;
    
    private Vector<DrawNavList> travelPlans = new Vector<DrawNavList>(PLANS);
    private Vector<Vector<NavData>> registry = new Vector<Vector<NavData>>(travelPlans.size());
    
	
    
    public NavGui() {
        super();
        String str = "I can fly".replace(' ', '_');
        travelPlans.add( DrawNavList.fromString("{ DEST!300!40.78!-73.97!2!10!TEST1 VEL!3!4!5!6!6000!" + str + " Fuck}"));
        travelPlans.add( DrawNavList.fromString("{ DEST!100!39.33!-77.3!5!50!TEST2 { DEST!100!39.33!-77.3!5!50!TEST4 DEST!300!40.78!-73.97!2!10!TEST5 Seriously} DEST!300!40.78!-73.97!2!10!TEST3 Swing}"));
        travelPlans.add( DrawNavList.fromString("{ boo}"));
        
        for (int i = 0; i < travelPlans.size(); i++) {
            registry.add(new Vector<NavData>());
        }
        addMouseMotionListener(this);
        addMouseListener(this);
    }
    
    public void copySelection() {
        if ((selected == null) || (selParList == null))
            return;
        NavData copyMe = selected.clone();
        selParList.add(selListSlot, copyMe);
        repaint();
    }
    
    public void moveSelection() {
        if (taskParent == null)
            return;
        copySelection();
        deleteSelection();
        repaint();
    }

    public void insertNavDest(DrawNavDest dest) {
        if (selParList == null)
            return;
        selParList.add(selListSlot, dest.clone());
        repaint();
    }
    
    public void insertNavVel(DrawNavVel vel) {
        if (selParList == null)
            return;
        selParList.add(selListSlot, vel.clone());
        repaint();
    }
    
    public void insertNavList(String name) {
        if (selParList == null) {
            System.out.println("null");
            return;
        }
        selParList.add(selListSlot, DrawNavList.fromString("{ " + name + "}"));
        repaint();
    }
    
    
    public void deleteSelection() {
        if (taskParent == null)
            return;
        taskParent.remove(selected);
        selected = null;
        taskParent = null;
        selParList = null;
        repaint();
    }
    
    public void paint(Graphics g) {
        super.paint(g);
        Graphics2D g2 = (Graphics2D) g;

        for (int i = 0; i < travelPlans.size(); i++) {
            registry.get(i).removeAllElements();
            travelPlans.get(i).drawMe(g2, i * xSize + BUFFER, BUFFER, xSize - 2 * BUFFER, registry.get(i));
        }
        
        //draw highlighted slot:
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
            yPos += .375 * NavData.BUFFER;
            g2.setColor(Color.CYAN);
            g2.fillRect(slotCol * xSize + BUFFER, yPos, xSize - 2 * BUFFER, (int) (.25 * NavData.BUFFER));
        }
        
        //draw selected slot:
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
            yPos += .375 * NavData.BUFFER;
            g2.setColor(Color.BLUE);
            g2.fillRect(selSlotCol * xSize + BUFFER, yPos, xSize - 2 * BUFFER, (int) (.25 * NavData.BUFFER));
        }
        setPreferredSize(new Dimension(NavGui.PLANS * NavGui.xSize, getDeepestY() + BUFFER));
        revalidate();
    }
    
    private void clearSelected() {
        if (selected != null) {
            selected.setSelected(false);
            selected = null;
            taskParent = null;
        }
    }
    
    private void clearHighlighted() {
        if (highlighted != null) {
            highlighted.setHighlighted(false);
            highlighted = null;
        }            
    }
    
    public void mouseDragged(MouseEvent e) {
        //Nothing to do here
    }
    
    public void mouseMoved(MouseEvent e) {
        NavData myTask = findTask(e.getX(), e.getY());
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
    
    public void mouseClicked(MouseEvent e) {
        NavData myTask = findTask(e.getX(), e.getY());
        if (e.getClickCount() == 2) {
            if ((myTask != null) && (myTask instanceof DrawNavList)) {
                ((DrawNavList) myTask).switchExpanded();
                repaint();
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
    
    public void mouseEntered(MouseEvent e) {
        //Nothing to do here
    }
    
    public void mouseExited(MouseEvent e) {
        //Nothing to do here
    }
    
    public void mousePressed(MouseEvent e) {
        //Nothing to do here
    }
    
    public void mouseReleased(MouseEvent e) {
        //Nothing to do here
    }
    
    private NavData findTask (int mX, int mY) {
        Vector<NavData> myList = getColRegistry(mX, mY);
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
    
    private int getDeepestY() {
        ListIterator<Vector<NavData>> i1 = registry.listIterator();
        int maxY = 0;
        while (i1.hasNext()) {
            Vector<NavData> mReg = i1.next();
            for (int i = 0; i < mReg.size(); i++) {
                int[][] mRange = mReg.get(i).getYRange();
                for (int j = 0; j < mRange.length; j++) {
                    maxY = Math.max(maxY, mRange[j][1]);
                }
            }
        }
        return maxY;
    }
    
    private DrawNavList findParentList(int mX, int mY) {
        Vector<NavData> myList = getColRegistry(mX, mY);
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
    
    private int findSlotInList(DrawNavList parent, int mY) {
        ListIterator<NavData> i1 = parent.listIterator();
        int prevElts = 0;
        while (i1.hasNext()) {
            int[][] yRange = i1.next().getYRange();
            if (yRange[0][1] < mY)
                prevElts++;
        }
        return prevElts;
    }
    
    private Vector<NavData> getColRegistry(int mX, int mY) {
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

