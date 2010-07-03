/* Copyright (C) 2001, 2010 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.applications.sar;

import gov.nasa.worldwind.applications.gos.*;
import gov.nasa.worldwind.*;

import javax.swing.*;
import java.awt.*;

/**
 * @author dcollins
 * @version $Id: GOSPortalFrame.java 13131 2010-02-16 05:38:14Z dcollins $
 */
public class GOSPortalFrame extends JFrame
{
    protected GeodataWindowJPanel gwd;

    public GOSPortalFrame(WorldWindow wwd) throws HeadlessException
    {
        super(Configuration.getStringValue(GeodataKey.DISPLAY_NAME_SHORT));

        this.gwd = this.createGeodataWindow();
        this.gwd.setWorldWindow(wwd);

        this.setBackground(Color.WHITE);
        this.getContentPane().setLayout(new BorderLayout(0, 0)); // hgap, vgap
        this.getContentPane().add(this.gwd, BorderLayout.CENTER);
        this.pack();
    }

    public GeodataWindow getGeodataWindow()
    {
        return this.gwd;
    }

    protected GeodataWindowJPanel createGeodataWindow()
    {
        return new GeodataWindowJPanel();
    }
}
