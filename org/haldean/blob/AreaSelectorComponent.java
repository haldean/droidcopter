package org.haldean.blob;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;

public class AreaSelectorComponent extends JComponent {
    JavaImage image;
    Segmenter segmenter;
    int[] loc;
    float scale;

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
	int[] imageSize = image.getSize();

	System.out.println(x);
	System.out.println(y);
	x /= scale;
	y /= scale;
	System.out.println(x);
	System.out.println(y);

	segmenter = Segmenter.getSegmenterForPoint(image, x, y);
	loc = segmenter.segment(image);

	System.out.println(loc[0]);
	System.out.println(loc[1]);
	System.out.println(loc[0] * scale);
	System.out.println(loc[1] * scale);

	repaint();
    }

    public void paintComponent(Graphics g) {
	Graphics2D g2 = (Graphics2D) g;

	int width = (int) getSize().getWidth();
	int height = (int) getSize().getHeight();

	if (image != null) {
	    int[] imageSize = image.getSize();
	    scale = Math.min((float) width / (float) imageSize[0],
			     (float) height / (float) imageSize[1]);
	    /* Create a new square affine transform for that scaling */
	    AffineTransform transform = AffineTransform.getScaleInstance(scale, scale);
	    g2.drawImage(image.image, transform, null);
	}

	if (loc != null) {
	    g2.setColor(Color.GREEN);

	    System.out.println("paint");
	    g2.fillRect((int) (loc[1] * scale) - 1, (int) (loc[0] * scale) - 1, 3, 3);
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