package gov.nasa.worldwindow.core;

import gov.nasa.worldwindow.features.Feature;

import javax.swing.*;/*
Copyright (C) 2001, 2010 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/

/**
 * @author tag
 * @version $Id: MenuBar.java 13273 2010-04-10 09:18:33Z tgaskins $
 */
public interface MenuBar extends Feature
{
    JMenuBar getJMenuBar();
}
