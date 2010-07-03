/* I LOVE YOU NASA */

package org.haldean.chopper.server;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

/* World Wind imports. */
import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.awt.*;
import gov.nasa.worldwind.event.*;
import gov.nasa.worldwind.exception.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.util.*;
import gov.nasa.worldwind.view.*;
import gov.nasa.worldwind.view.orbit.*;

/* Layer imports. */
import gov.nasa.worldwind.layers.*;
import gov.nasa.worldwind.layers.placename.*;
import gov.nasa.worldwind.layers.Earth.*;

/** A component that shows a globe with the chopper's path, along with
 *  some basic following and location-setting controls 
 *  @author William Brown */
public class WorldWindComponent extends JPanel {
    /* The WorldWind component. */
    private final WorldWindowGLCanvas wwd;
    
    /* The locations and the line connecting them */
    private java.util.List<Position> locs;
    private Polyline pathLine;
    private SurfaceCircle chopperTargetInner;
    private SurfaceCircle chopperTargetOuter;
    private final SurfaceCircle clickLocation;

    /* Used to have the component follow the last position
     * of the chopper */
    private JPanel statusPane;
    private JPanel followPane;
    private JCheckBox follow;
    private double followAltitude = 1000;
    private final JTextField altField;

    /* Displays status messages */
    private JButton statusLabel;

    private final int maxMovePixels = 15;

    /** Create a new WorldWindComponent */
    public WorldWindComponent() {
	super(new BorderLayout());

	wwd = new WorldWindowGLCanvas();

	/* Create the default model as described in the current worldwind properties. */
	Model m = (Model) WorldWind.createConfigurationComponent(AVKey.MODEL_CLASS_NAME);
	wwd.setModel(m);

	/* Add a listener to implement the right-click functionality */
	wwd.addMouseListener(new MouseAdapter() {
		public void mouseClicked(MouseEvent e) {
		    /* Button 3 is the right click button */
		    if (e.getButton() == MouseEvent.BUTTON3) {
			/* clickPosition now holds the position under the mouse cursor */
			Position clickPosition = wwd.getView().computePositionFromScreenPoint(e.getX(), e.getY());
			/* Move the green circle */
			clickLocation.setCenter(clickPosition);
			/* Update the button */
			statusLabel.setText("Send to (" + 
					    Math.round(1000 * clickPosition.getLatitude().getDegrees()) / 1000.0 + "\u00B0, " +
					    Math.round(1000 * clickPosition.getLongitude().getDegrees()) / 1000.0 + "\u00B0)");
		    }
		}
	    });

	/* Add wwd to the panel */
	add(wwd, BorderLayout.CENTER);

	/* This list will hold the points in our path */
	locs = new LinkedList<Position>();
	/* And this is the line created by connecting those points */
	pathLine = new Polyline(locs);

	/* These are the attributes for the displayed SurfaceCircles */
	ShapeAttributes attributes = new BasicShapeAttributes();
	attributes.setDrawInterior(false);
	attributes.setDrawOutline(true);
	attributes.setOutlineMaterial(new Material(Color.RED));
	attributes.setOutlineOpacity(0.9);
	attributes.setOutlineWidth(2);

	chopperTargetInner = new SurfaceCircle(attributes);
	chopperTargetInner.setRadius(50);
	chopperTargetOuter = new SurfaceCircle(attributes);
	chopperTargetOuter.setRadius(500);

	attributes.setOutlineMaterial(new Material(Color.GREEN));
	clickLocation = new SurfaceCircle(attributes);
	clickLocation.setRadius(100);

	/* Set the path color to a slightly-transparent red */
	pathLine.setColor(new Color(255, 0, 0, 200));
	pathLine.setLineWidth(2);
	/* The line is ugly without this */
	pathLine.setAntiAliasHint(Polyline.ANTIALIAS_NICEST);

	/* Create a layer for the polyline and add it to the layer list. */
	RenderableLayer polyLayer = new RenderableLayer();
	polyLayer.addRenderable(pathLine);

	SurfaceShapeLayer shapeLayer = new SurfaceShapeLayer();
	shapeLayer.addRenderable(chopperTargetInner);
	shapeLayer.addRenderable(chopperTargetOuter);
	shapeLayer.addRenderable(clickLocation);

	/* Note that the layers must be added in this order for everything to 
	 * show up correctly; the first added are at the "bottom" in terms of 
	 * display ordering */
	LayerList layers = m.getLayers();
	/* Add high-quality topography data for the US. I fucking love NASA / USGS */
	layers.add(new USGSTopoHighRes());
	/* Add high-quality city satellite imagery. Thanks Microsoft! */
	layers.add(new MSVirtualEarthLayer(MSVirtualEarthLayer.LAYER_HYBRID));
	/* Add chopper location and path layers */
	layers.add(polyLayer);
	layers.add(shapeLayer);

	/* Follow pane holds the check box and the textarea */
	followPane = new JPanel(new FlowLayout());
	follow = new JCheckBox("Follow Altitude: ");
	altField = new JTextField(new Double(followAltitude).toString());
	/* When the text is changed, automatically update the
	 * follow altitude */
	altField.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent event) {
		    try {
			followAltitude = new Double(altField.getText());
		    } catch (Exception e) {
			;
		    }
		}
	    });
	
	followPane.add(follow);
	followPane.add(altField);

	/* Status button is in the bottom right */
	statusLabel = new JButton("Right click to select location");

	/* Status pane holds everything that is not the globe */
	statusPane = new JPanel(new BorderLayout());
	statusPane.add(followPane, BorderLayout.WEST);
	statusPane.add(statusLabel, BorderLayout.EAST);

	add(statusPane, BorderLayout.SOUTH);
    }

    /** Used for TabPanes */
    public String getName() {
	return "Globe";
    }

    /** Move the view by a given amount
     *  @param dx Amount to move the view in the x direction
     *  @param dy Amount to move the view in the y direction
     *  @param dz Amount to zoom the view in and out
     *  @param dt Amount to move the tilt of the camera
     *  @param dp Amount of move the pan of the camera */
    public void moveView(float dx, float dy, float dz, float dt, float dp) {
	Dimension componentSize = wwd.getSize();
	BasicOrbitView currentView = (BasicOrbitView) wwd.getView();
	Position moveToPos = currentView.computePositionFromScreenPoint((int) (componentSize.getWidth() / 2 + dx * maxMovePixels),
									(int) (componentSize.getHeight() / 2 + dy * maxMovePixels));
	currentView.addCenterAnimator(currentView.getCenterPosition(), moveToPos, 20, true);
	
	double zoom = currentView.getZoom();
	currentView.addZoomAnimator(zoom, zoom * (1 + 3 * dz));

	Angle heading = Angle.fromDegrees(currentView.getHeading().getDegrees() + 45.0 * dp);
	Angle pitch = Angle.fromDegrees(currentView.getPitch().getDegrees() + 30.0 * dt);
	currentView.addHeadingPitchAnimator(currentView.getHeading(), heading, currentView.getPitch(), pitch);

	wwd.redrawNow();
    }

    /** Invert the follow variable and check box status */
    public void toggleFollow() {
	follow.setSelected(! follow.isSelected());
    }

    /** Add a waypoint, and optionally follow if the box is checked 
     *  @param _w The position to append to the path */
    public void addWaypoint(Position _w) {
	/* Requires a lock on locs or World Wind gets mad */
	synchronized (locs) {
	    locs.add(_w);
	    pathLine.setPositions(locs);
	    chopperTargetInner.setCenter(_w);
	    chopperTargetOuter.setCenter(_w);
	}
	/* If follow is checked and we aren't already animating, move 
	 * the view to a kilometer above the helicopter's current position */
	if (follow.isSelected() && ! wwd.getView().isAnimating())
	    wwd.getView().goTo(_w, _w.getElevation() + followAltitude);
    }

    /** Update the look and feel of this component */
    public void updateUI() {
	if (statusPane != null)
	    statusPane.updateUI();
	if (follow != null)
	    follow.updateUI();
	if (altField != null)
	    altField.updateUI();
	if (statusLabel != null)
	    statusLabel.updateUI();
	if (followPane != null)
	    followPane.updateUI();
    }

    /** Test code that simulates a flight in which the chopper 
     *  takes off from Nussbaum and fly West */
    public static void main(String args[]) {
	System.err.println("Running");
	JFrame f = new JFrame("World Wind Test");
	WorldWindComponent w = new WorldWindComponent();
	f.add(w);
	f.setPreferredSize(new Dimension(600, 600));
	f.pack();
	f.setVisible(true);

	try {
	    w.addWaypoint(Position.fromDegrees(40.80728, -73.962579, 100));
	    double alt = 200;
	    for (double lon = -73.962579; true; lon -= 0.0001) {
		Thread.sleep(500);
		w.addWaypoint(Position.fromDegrees(40.80728, lon, alt++));
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
