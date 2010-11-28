package org.haldean.chopper.server;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.HashMap;

/**
 *  An interface to send new tuning parameters to the helicopter.
 *
 *  @author William Brown
 */
public class PidTuningComponent extends JFrame implements Updatable {
    public enum TuningLoop { DX, DY, DZ, DT };
    private HashMap<TuningLoop, TuningPanel> panels;
    private UpdateUiPanel mainPanel;

    private static PidTuningComponent instance = new PidTuningComponent();
    public static PidTuningComponent getInstance() {
	return instance;
    }

    private PidTuningComponent() {
	super("PID Tuning Parameters");
	panels = new HashMap<TuningLoop, TuningPanel>();

	UpdateUiPanel tuningPanel = new UpdateUiPanel(new GridLayout(1, 4, 10, 10));
	tuningPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
	for (TuningLoop t : TuningLoop.values()) {
	    TuningPanel p = new TuningPanel(t);
	    panels.put(t, p);
	    tuningPanel.add(p);
	}

	UpdateUiPanel buttonPanel = new UpdateUiPanel(new FlowLayout(FlowLayout.RIGHT));
	buttonPanel.add(cancelButton());
	buttonPanel.add(applyButton());

	mainPanel = new UpdateUiPanel(new BorderLayout());
	mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
	mainPanel.add(tuningPanel, BorderLayout.CENTER);
	mainPanel.add(buttonPanel, BorderLayout.SOUTH);
	add(mainPanel);

	setPreferredSize(new Dimension(500,230));
	pack();
    }

    public void updateUI() {
	if (mainPanel != null) {
	    mainPanel.updateUI();
	}
    }

    private void apply() {
	for (TuningPanel p : panels.values()) {
	    p.sendNewParameters();
	}
    }

    private void close() {
	setVisible(false);
    }

    private TuningPanel getTuningPanel(int indexFromChopper) {
	if (indexFromChopper == 0) return panels.get(TuningLoop.DX);
	if (indexFromChopper == 1) return panels.get(TuningLoop.DY);
	if (indexFromChopper == 2) return panels.get(TuningLoop.DZ);
	if (indexFromChopper == 3) return panels.get(TuningLoop.DT);
	else return null;
    }

    public void update(String message) {
	if (!message.startsWith("GUID:PID:VALUE")) return;

	String[] parts = message.split(":");
	getTuningPanel(new Integer(parts[3]))
	    .setParameter(new Integer(parts[4]), parts[5]);
    }

    private JComponent cancelButton() {
	JButton cancelButton = new JButton("Cancel");
	cancelButton.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    close();
		}
	    });
	return cancelButton;
    }

    private JComponent applyButton() {
	JButton applyButton = new JButton("Apply");
	applyButton.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    apply();
		}
	    });
	return applyButton;
    }
	
    private class TuningPanel extends UpdateUiPanel {
	JTextField pValue = new JTextField("0");
	JTextField iValue = new JTextField("0");
	JTextField dValue = new JTextField("0");
	TuningLoop loop;

	public TuningPanel(TuningLoop loop) {
	    setLayout(new GridLayout(4, 1, 0, 10));
	    this.loop = loop;

	    JLabel titleLabel = new JLabel(loop.toString(), JLabel.CENTER);
	    titleLabel.setFont(StyleProvider.getFont(16, true));
	    add(titleLabel);
	    
	    UpdateUiPanel pPair = new UpdateUiPanel();
	    pPair.setLayout(new BoxLayout(pPair, BoxLayout.X_AXIS));
	    pPair.add(label("P:"));
	    pPair.add(pValue);
	    add(pPair);

	    UpdateUiPanel iPair = new UpdateUiPanel();
	    iPair.setLayout(new BoxLayout(iPair, BoxLayout.X_AXIS));
	    iPair.add(label("I:"));
	    iPair.add(iValue);
	    add(iPair);

	    UpdateUiPanel dPair = new UpdateUiPanel();
	    dPair.setLayout(new BoxLayout(dPair, BoxLayout.X_AXIS));
	    dPair.add(label("D:"));
	    dPair.add(dValue);
	    add(dPair);
	}

	private JLabel label(String s) {
	    JLabel l = new JLabel(s);
	    l.setPreferredSize(new Dimension(20,30));
	    return l;
	}

	public void setParameter(int parameterIndex, String value) {
	    switch(parameterIndex) {
	    case 0:
		pValue.setText(value);
		break;
	    case 1:
		iValue.setText(value);
		break;
	    case 2:
		dValue.setText(value);
		break;
	    }
	}
	
	public void sendNewParameters() {
	    try {
		double p = new Double(pValue.getText());
		double i = new Double(iValue.getText());
		double d = new Double(dValue.getText());

		Navigator.tunePid(loop.ordinal(), 0, p);
		Navigator.tunePid(loop.ordinal(), 1, i);
		Navigator.tunePid(loop.ordinal(), 2, d);
	    } catch (NumberFormatException e) {
		e.printStackTrace();
	    }
	}
    }

    public static void activate() {
	updatePID();
	instance.updateUI();
	instance.setVisible(true);
    }

    public static void updatePID() {
	Navigator.requestPidValues();
    }
}