package org.haldean.chopper.server;

import java.awt.*;
import javax.swing.*;

/** A component that contains a TextArea that can be updated 
 *  @author William Brown */
public class UpdatableTextArea extends JPanel implements Updatable {
    private JTextArea area;
    private JScrollPane scroll;
    private JCheckBox scrollLock;
    private String name;

    /** Create an UpdatableTextArea with a given name */
    public UpdatableTextArea(String _name) {
	super(new BorderLayout());

	name = _name;

	/* Create a non-editable textarea and put it in a scrollpane */
	area = new JTextArea();
	area.setEditable(false);
	scroll = new JScrollPane(area);

	/* This checkbox tells us whether we should have the textarea
	 * automatically keep up with new input */
	scrollLock = new JCheckBox("Scroll Lock", false);
	scrollLock.setSelected(true);

	add(scroll, BorderLayout.CENTER);
	add(scrollLock, BorderLayout.SOUTH);
    }

    /** Used for TabPanes */
    public String getName() {
	return name;
    }

    /** Update the TextArea with a new message 
     *  @param msg The string to append onto the text area */
    public void update(String msg) {
	/* Append the message */
	area.setText(area.getText() + "\n" + msg);
	/* If the scroll lock checkbox is checked, place the scrollbar at its
	 * vertical maximum so the textarea shows the most current data */
	if (scrollLock.isSelected()) {
	    scroll.getVerticalScrollBar().setValue(scroll.getVerticalScrollBar().getMaximum());
	    /* 
	    try {
		SwingUtilities.invokeAndWait(new Runnable() {
			public void run() {
			    scroll.getVerticalScrollBar().setValue(scroll.getVerticalScrollBar().getMaximum());
			}
		    });
	    } catch (Exception e) {
		e.printStackTrace();
		} */
	}
    }

    /** Update the look and feel of this component */
    public void updateUI() {
	if (scrollLock != null) {
	    area.updateUI();
	    scroll.updateUI();
	    scroll.getVerticalScrollBar().updateUI();
	    scrollLock.updateUI();
	}
	super.updateUI();
    }
}