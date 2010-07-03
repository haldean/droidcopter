/*
Copyright (C) 2001, 2007 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.applications.sar.actions;

import gov.nasa.worldwind.applications.sar.*;

import javax.swing.*;
import java.awt.event.*;

/**
 * @author tag
 * @version $Id: AppendPositionAction.java 11483 2009-06-07 21:37:23Z dcollins $
 */
public class AppendPositionAction extends AbstractAction
{
    protected final PositionTable table;

    public AppendPositionAction(final PositionTable table)
    {
        this.table = table;
        putValue(NAME, "Append New Position to Track");
        putValue(LONG_DESCRIPTION, "Add a new position to the end of the Track");
    }

    public void actionPerformed(ActionEvent e)
    {
        SARTrack st = table.getSarTrack();
        if (st == null)
            return;

        if (st.size() != 0)
            st.appendPosition(st.get(st.size() - 1));
        else
            st.appendPosition(new SARPosition());

        table.getSelectionModel().setSelectionInterval(st.size() - 1, st.size() - 1);
    }
}
