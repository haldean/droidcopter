package org.haldean.chopper.server;

import java.awt.Component;
import java.awt.LayoutManager;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 *  This is essentially a normal panel, but with a smarter 
 *  {@link updateUI} method that updates its children.
 *
 *  @author William Brown
 */
public class UpdateUiPanel extends JPanel {
    public UpdateUiPanel() {
	super();
    }

    public UpdateUiPanel(LayoutManager l) {
	super(l);
    }

    public void updateUI() {
	super.updateUI();
	for (Component c : getComponents()) {
	    if (c instanceof JComponent) {
		((JComponent) c).updateUI();
	    }
	}
    }
}