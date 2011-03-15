package org.haldean.chopper.server;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;

import org.haldean.blob.JavaImage;
import org.haldean.blob.Segmenter;

public class AreaSelectorComponent extends JComponent {
    JavaImage image;
    Segmenter segmenter;
    int[] loc;
    float scale = 1;

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

	x /= scale;
	y /= scale;

	segmenter = Segmenter.getSegmenterForPoint(image, x, y);
	loc = segmenter.segment(image);

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
	    g2.drawImage(image.getImage(), transform, null);
	}

	if (loc != null) {
	    g2.setColor(Color.GREEN);
	    g2.fillRect((int) (loc[1] * scale) - 2, (int) (loc[0] * scale) - 2, 5, 5);
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