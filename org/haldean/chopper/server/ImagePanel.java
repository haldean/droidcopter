package org.haldean.chopper.server;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;

/** A component to show an image along with image quality controls 
 *  @author William Brown */
public class ImagePanel extends JPanel implements Updatable {
    private JComboBox imageSizes;
    private JSlider imageQuality;
    private JLabel imagQualLabel;
    private ImageComponent image;
    private JButton sendButton;

    private JPanel bottomPanel;

    private final int defaultQuality = 25;

    private HashMap<Integer, ImageSizeEntry> sizes;

    /** Create a new Image Panel*/
    public ImagePanel() {
	super(new BorderLayout());

	/* This panel contains the quality controls */
	bottomPanel = new JPanel(new GridLayout(1, 4));
	add(bottomPanel, BorderLayout.SOUTH);

	/* This combo box allows the user to choose between
	 * supported sizes. Changing it reenables the set
	 * quality button */
	imageSizes = new JComboBox();
	imageSizes.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    sendButton.setEnabled(true);
		}
	    });
	bottomPanel.add(imageSizes);

	/* This slider adjusts the JPEG image compression.
	 * Changing it calls changeQuality, which updates the
	 * label and enables the set quality button */
	imageQuality = new JSlider(0, 100, defaultQuality);
	imageQuality.addChangeListener(new ChangeListener() {
		public void stateChanged(ChangeEvent e) {
		    changeQuality(imageQuality.getValue());
		}
	    });
	bottomPanel.add(imageQuality);

	/* This label displays the value of the slider */
	imagQualLabel = new JLabel();
	bottomPanel.add(imagQualLabel);
	changeQuality(defaultQuality);

	/* This button send the signal to the phone to
	 * change quality settings */
	sendButton = new JButton("Set Quality Settings");
	sendButton.setEnabled(false);
	sendButton.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    sendButton.setEnabled(false);
		    setQuality();
		}
	    });
	bottomPanel.add(sendButton);

	image = new ImageComponent();
	add(image, BorderLayout.CENTER);

	sizes = new HashMap<Integer, ImageSizeEntry>();
    }

    /** Used for Tab Pane
     *  @return The generic name for this component */
    public String getName() {
	return "Telemetry";
    }

    /** Update the look and feel of all child components */
    public void updateUI() {
	if (imageQuality != null) {
	    imagQualLabel.updateUI();
	    imageSizes.updateUI();
	    imageQuality.updateUI();
	    bottomPanel.updateUI();
	    sendButton.updateUI();
	}
    }

    /** Change the quality of the captured images
     *  @param quality The JPEG compression factor of the image, 0 ≤ quality ≤ 100 */
    public void changeQuality(int quality) {
	imagQualLabel.setText("Image Quality: " + quality);
	if (sendButton != null)
	    sendButton.setEnabled(true);
    }

    /** Send the quality value to the image capture device */
    private void setQuality() {
	DataReceiver.sendToDefault("IMAGE:SET:QUALITY:" + imageQuality.getValue());
	if (imageSizes.getItemCount() > 0)
	    DataReceiver.sendToDefault(((ImageSizeEntry) imageSizes.getSelectedItem()).setSizeString());
    }

    /** Set the image shown in the preview pane
     *  @param _image The JPEG-encoded byte array representing the image 
     *  @see org.haldean.chopper.server.ImageComponent#setImage */
    public void setImage(byte _image[]) {
	image.setImage(_image);
    }

    /** Listens for the available size signal from the image capture device 
     *  @param msg The message string received from the device */
    public void update(String msg) {
	if (msg.startsWith("IMAGE:AVAILABLESIZE")) {
	    String msgParts[] = msg.split(":");
	    ImageSizeEntry size = new ImageSizeEntry(msgParts[2], msgParts[3]);
	    imageSizes.addItem(size);
	    sizes.put(size.area(), size);
	} else if (msg.startsWith("IMAGE:PARAMS")) {
	    String msgParts[] = msg.split(":");
	    imageQuality.setValue(new Integer(msgParts[4]));
	    imageSizes.setSelectedItem(sizes.get(new Integer(msgParts[2]) * 
						 new Integer(msgParts[3])));
	}
    }

    /** A class used to represent image sizes in the combo box */
    private class ImageSizeEntry {
	public int width;
	public int height;

	/** Create a new ImageSizeEntry
	 *  @param _w The width of the image size option
	 *  @param _h The height of the image size option */
	public ImageSizeEntry(String _w, String _h) {
	    this(new Integer(_w), new Integer(_h));
	}

	/** Create a new ImageSizeEntry
	 *  @param _w The width of the image size option
	 *  @param _h The height of the image size option */
	public ImageSizeEntry(int _w, int _h) {
	    width = _w;
	    height = _h;
	}

	/** Get the string to send to the device to tell it to
	 *  switch image sizes 
	 *  @return A string of the format "IMAGE:SET:SIZE:W:H" */
	public String setSizeString() {
	    return new String("IMAGE:SET:SIZE:" + width + ":" + height);
	}

	/** The string representation of the size for the combo box
	 *  @return A string of the format "WxH" */
	public String toString() {
	    return new String(width + "x" + height);
	}

	/** Returns the area of the image size
	 *  @return The area (in pixels) of the image */
	public int area() {
	    return width * height;
	}
    }
}