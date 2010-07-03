/*
Copyright (C) 2001, 2010 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/

package gov.nasa.worldwindow.core.layermanager;

import gov.nasa.worldwind.util.WWUtil;

import java.util.*;

/**
 * @author tag
 * @version $Id: LayerPath.java 13266 2010-04-10 02:47:09Z tgaskins $
 */
public class LayerPath extends ArrayList<String>
{
    public LayerPath()
    {
    }

    public LayerPath(LayerPath initialPath, String... args)
    {
        this.addAll(initialPath);

        for (String pathElement : args)
        {
            if (!WWUtil.isEmpty(pathElement))
                this.add(pathElement);
        }
    }

    public LayerPath(String initialPathEntry, String... args)
    {
        this.add(initialPathEntry);

        for (String pathElement : args)
        {
            if (!WWUtil.isEmpty(pathElement))
                this.add(pathElement);
        }
    }

    public LayerPath(List<String> initialPathEntries)
    {
        this.addAll(initialPathEntries);
    }

    public LayerPath lastButOne()
    {
        return this.subPath(0, this.size() - 1);
    }

    public LayerPath subPath(int start, int end)
    {
        return new LayerPath(this.subList(start, end));
    }

    public static boolean isEmptyPath(LayerPath path)
    {
        return path == null || path.size() == 0 || WWUtil.isEmpty(path.get(0));
    }
}
