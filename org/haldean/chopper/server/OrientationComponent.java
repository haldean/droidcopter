package org.haldean.chopper.server;

import javax.media.j3d.*;
import javax.vecmath.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import com.sun.j3d.utils.universe.*; 
import com.sun.j3d.utils.geometry.*;
import java.util.*;

/** A component that uses Java3D to display a 3D rendering 
 *  of the current orientation of the chopper 
 *  @author William Brown */
public class OrientationComponent extends JPanel {
    TransformGroup chopperRotator;
    SetAngleBehavior angleBehavior;

    /** Create a new Orientation Component */
    public OrientationComponent() {
	this(false);
    }

    /** Create a new Orientation Component
     *  @param rotate Rotates the model the model at a constant rate if set to true */
    public OrientationComponent(boolean rotate) {
	super(new GridLayout(1,1));
	GraphicsConfiguration config = 
	    SimpleUniverse.getPreferredConfiguration();
	Canvas3D c3d = new Canvas3D(config);
	add(c3d);

	SimpleUniverse u = new SimpleUniverse(c3d);
	u.getViewingPlatform().setNominalViewingTransform();

	u.addBranchGraph(createSceneGraph(rotate));
    }

    /** Used for Tab Panes
     *  @return The string "Orientation" */
    public String getName() {
	return "Orientation";
    }

    /** Create the scene graph to insert into the SimpleUniverse
     *  @param rotate The object has a rotator applied to it if this is true
     *  @return A BranchGroup containing the chopper and the rotator behavior */
    private BranchGroup createSceneGraph(boolean rotate) {
	BranchGroup objectRoot = new BranchGroup();

	TransformGroup chopperModel = getChopperModel();

	/* The region in which the behavior is allowed to take place */
	BoundingSphere bounds = new BoundingSphere(new Point3d(0, 0, 0), 100);

	angleBehavior = new SetAngleBehavior(chopperModel);
	angleBehavior.setSchedulingBounds(bounds);

	TransformGroup rotateGroup = new TransformGroup();
	rotateGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);

	Alpha rotationAlpha = new Alpha(-1, 8000);
	RotationInterpolator rotator = 
	    new RotationInterpolator(rotationAlpha, rotateGroup);

	rotator.setSchedulingBounds(bounds);

	objectRoot.addChild(rotateGroup);
	rotateGroup.addChild(chopperModel);
	rotateGroup.addChild(angleBehavior);
	if (rotate)
	    rotateGroup.addChild(rotator);

	/* Optimize! Enhance! */
	objectRoot.compile();
	return objectRoot;
    }

    /** Get the 3D representation of the chopper
     *  @return A TransformGroup whose transform is writeable that contains 
     *  a 3D representation of the quadricopter */
    private TransformGroup getChopperModel() {
	BranchGroup node = new BranchGroup();

	Appearance metal = new Appearance();
	metal.setColoringAttributes(new ColoringAttributes(0.5f, 0.5f, 0.5f,
						       ColoringAttributes.SHADE_GOURAUD));
	Appearance red = new Appearance();
	red.setColoringAttributes(new ColoringAttributes(1.0f, 0f, 0f,
						       ColoringAttributes.SHADE_GOURAUD));
	//red.setLineAttributes(new LineAttributes(1, LineAttributes.PATTERN_SOLID, true

	Appearance green = new Appearance();
	green.setColoringAttributes(new ColoringAttributes(0f, 1.0f, 0f,
						       ColoringAttributes.SHADE_GOURAUD));

	Appearance blue = new Appearance();
	blue.setColoringAttributes(new ColoringAttributes(1.0f, 1.0f, 0f,
						       ColoringAttributes.SHADE_GOURAUD));

	/* The X bars */
	Cylinder xbar1 = new Cylinder(0.01f, 1f, metal);
	Cylinder xbar2 = new Cylinder(0.01f, 1f, metal);

	/* Move bar 1 to be perpendicular */
	Transform3D rotateZ = new Transform3D();
	rotateZ.rotZ(Math.PI / 2d);
	TransformGroup grpZ = new TransformGroup(rotateZ);
	grpZ.addChild(xbar1);

	/* Rotate them both to be in the XY plane */
	Transform3D rotateToXY = new Transform3D();
	rotateToXY.rotX(Math.PI / 2d);
	TransformGroup grpXY = new TransformGroup(rotateToXY);
	grpXY.addChild(xbar2);
	grpXY.addChild(grpZ);

	/* The downwards-pointing vector */
	Cylinder cyl = new Cylinder(0.005f, 0.25f, red);
	Transform3D translate = new Transform3D();
	translate.set(new Vector3f(0f, -0.125f, 0f));
	TransformGroup cylTransform = new TransformGroup(translate);
	cylTransform.addChild(cyl);

	/* The "forwards" pointing vector */
	Cylinder forwards = new Cylinder(0.015f, 0.25f, red);
	translate = new Transform3D();
	translate.set(new Vector3f(0f, 0f, -0.125f));
	Transform3D rotate = new Transform3D();
	rotate.rotX(Math.PI / 2d);
	translate.mul(rotate);
	
	TransformGroup forwardsVector = new TransformGroup(translate);
	forwardsVector.addChild(forwards);
	    
	/* The "propellers" */
	Cylinder prop1 = new Cylinder(0.03f, 0.1f, blue);
	Cylinder prop2 = new Cylinder(0.03f, 0.1f, red);
	Cylinder prop3 = new Cylinder(0.03f, 0.1f, blue);
	Cylinder prop4 = new Cylinder(0.03f, 0.1f, red);

	Transform3D prop1Trans = new Transform3D();
	prop1Trans.set(new Vector3f(0.5f, 0, 0));
	TransformGroup prop1g = new TransformGroup(prop1Trans);
	prop1g.addChild(prop1);
	node.addChild(prop1g);

	Transform3D prop2Trans = new Transform3D();
	prop2Trans.set(new Vector3f(0, 0, 0.5f));
	TransformGroup prop2g = new TransformGroup(prop2Trans);
	prop2g.addChild(prop2);
	node.addChild(prop2g);

	Transform3D prop3Trans = new Transform3D();
	prop3Trans.set(new Vector3f(-0.5f, 0, 0));
	TransformGroup prop3g = new TransformGroup(prop3Trans);
	prop3g.addChild(prop3);
	node.addChild(prop3g);

	Transform3D prop4Trans = new Transform3D();
	prop4Trans.set(new Vector3f(0, 0, -0.5f));
	TransformGroup prop4g = new TransformGroup(prop4Trans);
	prop4g.addChild(prop4);
	node.addChild(prop4g);

	node.addChild(grpXY);
	node.addChild(cylTransform);
	node.addChild(forwardsVector);

	chopperRotator = new TransformGroup();
	chopperRotator.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);

	chopperRotator.addChild(node);
	return chopperRotator;
    }

    /** Visualize the current orientation of the chopper 
     *  @param o The current orientation of the chopper */
    public void setOrientation(Orientation o) {
	angleBehavior.setAngle(o);
	angleBehavior.processStimulus(null);
    }

    /** A Behavior class that allows the angle of the enclosed
     *  TransformGroup to be arbitrarily set along all three axes */
    public class SetAngleBehavior extends Behavior {
        private TransformGroup targetTG;
        private Transform3D rotationX = new Transform3D();
	private Transform3D rotationZ = new Transform3D();
	private Orientation angle;

	/** Create a new SetAngleBehavior 
	 *  @param _targetTG The transform group to operate upon */
	public SetAngleBehavior(TransformGroup _targetTG) {
	    targetTG = _targetTG;
	    angle = new Orientation(0, 0, 0);
	}

	/** Empty initialization */
	public void initialize() {
	    ;
	}

	/** Update the universe with the current angle 
	 *  @param criteria Ignored criteria for updating */
	public void processStimulus(Enumeration criteria) {
	    Transform3D rotationY = new Transform3D();
	    rotationY.rotY(- angle.getRoll(Orientation.RADIANS));

	    rotationX.rotX(- angle.getTilt(Orientation.RADIANS));
	    rotationZ.rotZ(angle.getPitch(Orientation.RADIANS));
	    rotationY.mul(rotationZ);
	    rotationY.mul(rotationX);
	    
	    targetTG.setTransform(rotationY);
	}

	/** Set the current orientation of the chopper
	 *  @param _angle The orientation of the chopper */
	public void setAngle(Orientation _angle) {
	    angle = _angle;
	}
    }

    /** Test method that displays a rotating chopper set at a constant 45
     *  degrees along both axes */
    public static void main(String args[]) {
	Debug.setEnabled(true);
	JFrame frame = new JFrame();
	frame.setPreferredSize(new Dimension(300, 300));
	OrientationComponent o = new OrientationComponent(true);
	frame.add(o);
	frame.pack();
	frame.setVisible(true);

	o.setOrientation(new Orientation(0, 45, 45));
    }
}