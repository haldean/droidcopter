package org.haldean.chopper.server.nav;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class NavGuiTest implements ActionListener {
	private NavGui gui;
    private JButton delete = new JButton("Delete");
    private JButton copy = new JButton("Copy");
    private JButton move = new JButton("Move");
    private JButton newList = new JButton("New List");
    
	public static void main (String args[]) {
        NavGuiTest tester = new NavGuiTest();
    }
    
    public NavGuiTest() {
        System.out.println("hello");
		JFrame frame = new JFrame();
        frame.setLayout(new BorderLayout());
        System.out.println(frame.getPreferredSize());
        
        gui = new NavGui();
        gui.setPreferredSize(new Dimension(NavGui.PLANS * NavGui.xSize, 500));
        JScrollPane pane = new JScrollPane(gui);
        frame.add(pane, BorderLayout.CENTER);

        JPanel buttons = new JPanel();
        frame.add(buttons, BorderLayout.EAST);
        
        delete.addActionListener(this);
        copy.addActionListener(this);
        move.addActionListener(this);
        newList.addActionListener(this);
        
        buttons.setLayout(new GridLayout(5,1));
        buttons.add(delete);
        buttons.add(copy);
        buttons.add(move);
        buttons.add(newList);
        
        frame.pack();
        frame.setVisible(true);
        frame.validate();
        frame.repaint();
	}
    
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == delete) {
            gui.deleteSelection();
        }
        if (e.getSource() == copy) {
            gui.copySelection();
        }
        if (e.getSource() == move) {
            gui.moveSelection();
        }
        if (e.getSource() == newList) {
            gui.insertNavList("ImARealList");
        }
    }
}

