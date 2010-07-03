/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.applications.gos.awt;

import javax.swing.*;
import java.awt.*;

/**
 * @author dcollins
 * @version $Id: AWTUtil.java 13127 2010-02-16 04:02:26Z dcollins $
 */
public class AWTUtil
{
    public static void invalidateTree(Container container)
    {
        synchronized (container.getTreeLock())
        {
            for (Component comp : container.getComponents())
            {
                if (comp instanceof Container)
                {
                    invalidateTree((Container) comp);
                }
                else
                {
                    if (comp.isValid())
                        comp.invalidate();
                }
            }

            if (container.isValid())
                container.invalidate();
        }
    }

    public static void setComponentEnabled(Component comp, boolean enabled)
    {
        if (comp instanceof JScrollPane)
        {
            Component c = ((JScrollPane) comp).getViewport().getView();
            if (c instanceof Container)
                setTreeEnabled((Container) c, enabled);
        }
        else if (comp instanceof Container)
        {
            setTreeEnabled((Container) comp, enabled);
        }
        else
        {
            comp.setEnabled(enabled);
        }

        comp.setEnabled(enabled);
    }

    public static void setTreeEnabled(Container container, boolean enabled)
    {
        synchronized (container.getTreeLock())
        {
            for (Component comp : container.getComponents())
            {
                setComponentEnabled(comp, enabled);
            }
        }
    }

    public static JButton scaleButtonToIcon(JButton button, int hgap, int vgap)
    {
        Icon icon = button.getIcon();
        if (icon == null)
            return button;

        int prefWidth = icon.getIconWidth() + 2 * hgap;
        int prefHeight = icon.getIconHeight() + 2 * vgap;

        button.setMinimumSize(new Dimension(prefWidth, prefHeight));
        button.setMaximumSize(new Dimension(prefWidth, prefHeight));
        button.setPreferredSize(new Dimension(prefWidth, prefHeight));

        return button;
    }

    public static JButton scaleButton(JButton button, double scale)
    {
        Dimension size = button.getPreferredSize();

        int prefWidth = (int) (scale * size.width);
        if (prefWidth < 0)
            prefWidth = 0;

        int prefHeight = (int) (scale * size.height);
        if (prefHeight < 0)
            prefHeight = 0;

        button.setMinimumSize(new Dimension(prefWidth, prefHeight));
        button.setPreferredSize(new Dimension(prefWidth, prefHeight));

        Font font = button.getFont();
        button.setFont(font.deriveFont((float) (scale * font.getSize())));

        return button;
    }
}
