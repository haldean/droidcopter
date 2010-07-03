/* Copyright (C) 2001, 2009 United States Government as represented by 
the Administrator of the National Aeronautics and Space Administration. 
All Rights Reserved. 
*/
package gov.nasa.worldwind.applications.gos.services;

import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.util.WWUtil;

/**
 * @author dcollins
 * @version $Id: RESTUtil.java 13127 2010-02-16 04:02:26Z dcollins $
 */
public class RESTUtil
{
    public static String stripNewlineCharacters(String s)
    {
        s = s.replaceAll("\n", "");
        s = s.replaceAll("\r", "");
        return s;
    }

    public static String replaceEncodedURLCharacters(String s)
    {
        s = s.replaceAll("%20", " ");
        s = s.replaceAll("%21", "!");
        s = s.replaceAll("%22", "\"");
        s = s.replaceAll("%23", "#");
        s = s.replaceAll("%24", "$");
        s = s.replaceAll("%25", "%");
        s = s.replaceAll("%26", "&");
        s = s.replaceAll("%27", "\'");
        s = s.replaceAll("%28", "(");
        s = s.replaceAll("%29", ")");
        s = s.replaceAll("%2A", "*");
        s = s.replaceAll("%2B", "+");
        s = s.replaceAll("%2C", ",");
        s = s.replaceAll("%2D", "-");
        s = s.replaceAll("%2E", ".");
        s = s.replaceAll("%2F", "/");
        s = s.replaceAll("%30", "0");
        s = s.replaceAll("%31", "1");
        s = s.replaceAll("%32", "2");
        s = s.replaceAll("%33", "3");
        s = s.replaceAll("%34", "4");
        s = s.replaceAll("%35", "5");
        s = s.replaceAll("%36", "6");
        s = s.replaceAll("%37", "7");
        s = s.replaceAll("%38", "8");
        s = s.replaceAll("%39", "9");
        s = s.replaceAll("%3A", ":");
        s = s.replaceAll("%3B", ";");
        s = s.replaceAll("%3C", "<");
        s = s.replaceAll("%3D", "=");
        s = s.replaceAll("%3E", ">");
        s = s.replaceAll("%3F", "?");
        s = s.replaceAll("%40", "@");
        s = s.replaceAll("%41", "A");
        s = s.replaceAll("%42", "B");
        s = s.replaceAll("%43", "C");
        s = s.replaceAll("%44", "D");
        s = s.replaceAll("%45", "E");
        s = s.replaceAll("%46", "F");
        s = s.replaceAll("%47", "G");
        s = s.replaceAll("%48", "H");
        s = s.replaceAll("%49", "I");
        s = s.replaceAll("%4A", "J");
        s = s.replaceAll("%4B", "K");
        s = s.replaceAll("%4C", "L");
        s = s.replaceAll("%4D", "M");
        s = s.replaceAll("%4E", "N");
        s = s.replaceAll("%4F", "O");
        s = s.replaceAll("%50", "P");
        s = s.replaceAll("%51", "Q");
        s = s.replaceAll("%52", "R");
        s = s.replaceAll("%53", "S");
        s = s.replaceAll("%54", "T");
        s = s.replaceAll("%55", "U");
        s = s.replaceAll("%56", "V");
        s = s.replaceAll("%57", "W");
        s = s.replaceAll("%58", "X");
        s = s.replaceAll("%59", "Y");
        s = s.replaceAll("%5A", "Z");
        s = s.replaceAll("%5B", "[");
        s = s.replaceAll("%5C", "\\");
        s = s.replaceAll("%5D", "]");
        s = s.replaceAll("%5E", "^");
        s = s.replaceAll("%5F", "_");
        s = s.replaceAll("%60", "`");
        s = s.replaceAll("%61", "a");
        s = s.replaceAll("%62", "b");
        s = s.replaceAll("%63", "c");
        s = s.replaceAll("%64", "d");
        s = s.replaceAll("%65", "e");
        s = s.replaceAll("%66", "f");
        s = s.replaceAll("%67", "g");
        s = s.replaceAll("%68", "h");
        s = s.replaceAll("%69", "i");
        s = s.replaceAll("%6A", "j");
        s = s.replaceAll("%6B", "k");
        s = s.replaceAll("%6C", "l");
        s = s.replaceAll("%6D", "m");
        s = s.replaceAll("%6E", "n");
        s = s.replaceAll("%6F", "o");
        s = s.replaceAll("%70", "p");
        s = s.replaceAll("%71", "q");
        s = s.replaceAll("%72", "r");
        s = s.replaceAll("%73", "s");
        s = s.replaceAll("%74", "t");
        s = s.replaceAll("%75", "u");
        s = s.replaceAll("%76", "v");
        s = s.replaceAll("%77", "w");
        s = s.replaceAll("%78", "x");
        s = s.replaceAll("%79", "y");
        s = s.replaceAll("%7A", "z");
        s = s.replaceAll("%7B", "{");
        s = s.replaceAll("%7C", "|");
        s = s.replaceAll("%7D", "}");
        s = s.replaceAll("%7E", "~");
        return s;
    }

    public static AVList parseHTTPGetString(String s)
    {
        String[] tokens = s.split("[?]", 2);
        if (tokens == null || tokens.length < 1)
            return null;

        AVList params = new AVListImpl();
        params.setValue(AVKey.SERVICE, tokens[0]);

        if (tokens.length < 2)
            return params;

        AVList queryParams = parseQueryString(tokens[1]);
        if (queryParams == null)
            return params;

        params.setValues(queryParams);

        return params;
    }

    public static AVList parseQueryString(String s)
    {
        String[] propertyArray = s.split("[&]");
        if (propertyArray == null || propertyArray.length < 1)
            return null;

        AVList params = new AVListImpl();

        for (String property : propertyArray)
        {
            if (WWUtil.isEmpty(property))
                continue;

            String[] kvp = property.split("[=]");
            if (kvp == null || kvp.length < 2)
                continue;

            params.setValue(kvp[0], kvp[1]);
        }

        return params;
    }
}
