package org.haldean.chopper.server;

import gov.nasa.worldwind.geom.Position;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import org.haldean.chopper.server.nav.DrawNavDest;
import org.haldean.chopper.server.nav.NavGui;

public class NavPanel extends UpdateUiPanel {
    private NavGui navGui;

    private JButton delete;
    private JButton copy;
    private JButton move;
    private JButton newList;
    private JButton execute;

    public NavPanel() {
	super(new BorderLayout());

	navGui = new NavGui();
	JScrollPane pane = new JScrollPane(navGui);

        add(pane, BorderLayout.CENTER);

        JPanel buttons = new JPanel();
        add(buttons, BorderLayout.SOUTH);
        
	delete = new JButton("Delete");
	copy = new JButton("Copy");
	move = new JButton("Move");
	newList = new JButton("New List");
	execute = new JButton("Execute");
        
	NavPanelActionListener ml = new NavPanelActionListener();
        delete.addActionListener(ml);
        copy.addActionListener(ml);
        move.addActionListener(ml);
        newList.addActionListener(ml);
	execute.addActionListener(ml);

        buttons.setLayout(new GridLayout(1, 5));
        buttons.add(delete);
        buttons.add(copy);
        buttons.add(move);
        buttons.add(newList);
	buttons.add(execute);
    }
    
    private class NavPanelActionListener implements ActionListener {
	public void actionPerformed(ActionEvent e) {
	    if (e.getSource() == delete) {
		navGui.deleteSelection();
	    } else if (e.getSource() == copy) {
		navGui.copySelection();
	    } else if (e.getSource() == move) {
		navGui.moveSelection();
	    } else if (e.getSource() == newList) {
		String listName = ServerCreator.getServerHost().getInput("New navigation list name:");
		navGui.insertNavList(listName.replaceAll(" ", "_"));
	    } else if (e.getSource() == execute) {
		navGui.makeItSo();
	    }
	}
    }

    public void insertDestination(Position location, double velocity, double targetRadius) {
	String name = ServerCreator.getServerHost().getInput("New destination name:").replaceAll(" ", "_");
	navGui.insertNavDest(DrawNavDest.taskFor(name, location.getElevation(), 
						 location.getLongitude().getDegrees(),
						 location.getLatitude().getDegrees(), 
						 velocity, targetRadius));
    }

    public String getName() {
	return "Navigation Tasks";
    }

    public static void main(String[] args) {
	JFrame f = new JFrame();
	f.add(new NavPanel());
	f.setVisible(true);
    }
}