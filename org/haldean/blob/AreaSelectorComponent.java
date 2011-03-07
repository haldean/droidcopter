package org.haldean.blob;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;

public class AreaSelectorComponent extends JComponent {
    JavaImage image;
    Segmenter segmenter;
    int[] loc;

    public AreaSelectorComponent(JavaImage image) {
	this.image = image;
	addMouseListener(new MouseAdapter() {
		public void mouseClicked(MouseEvent e) {
		    segmentImage(e.getX(), e.getY());
		}
	    });
    }

    private void segmentImage(int x, int y) {
	Dimension size = getSize();
	System.out.println("start " + x + " " + y);
	segmenter = Segmenter.getSegmenterForPoint(image, x, y);
	loc = segmenter.segment(image);
	System.out.println("done " + loc[0] + " " + loc[1]);
	repaint();
    }

    public void paintComponent(Graphics g) {
	Graphics2D g2 = (Graphics2D) g;

	int width = (int) getSize().getWidth();
	int height = (int) getSize().getHeight();

	if (image != null) {
	    int[] imageSize = image.getSize();
	    double scale = 1;
	    /* Create a new square affine transform for that scaling */
	    AffineTransform transform = AffineTransform.getScaleInstance(scale, scale);
	    g2.drawImage(image.image, transform, null);
	}

	if (loc != null) {
	    System.out.println("not null");
	    g2.setColor(Color.GREEN);
	    g2.fillRect(loc[1]-2, loc[0]-2, 5, 5);
	}
    }

    public static void main(String[] args) {
	JavaImage img;
	try {
	    img = new JavaImage(args[0]);
	} catch (Exception e) {
	    e.printStackTrace();
	    return;
	}

	JFrame frame = new JFrame();
	frame.add(new AreaSelectorComponent(img));
	frame.pack();
	frame.setVisible(true);
    }
}
