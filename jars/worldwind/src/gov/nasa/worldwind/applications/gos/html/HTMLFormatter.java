/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.applications.gos.html;

import gov.nasa.worldwind.applications.gos.OnlineResource;

import java.net.URI;

/**
 * @author dcollins
 * @version $Id: HTMLFormatter.java 13127 2010-02-16 04:02:26Z dcollins $
 */
public interface HTMLFormatter
{
    boolean isEnableAdvancedHTML();

    void setEnableAdvancedHTML(boolean enable);

    void addImage(StringBuilder sb, URI uri, String text);

    void addLineBreak(StringBuilder sb);

    void addResourceHyperlink(StringBuilder sb, OnlineResource resource, String displayText, String color);

    void addResourceHyperlink(StringBuilder sb, OnlineResource resource);

    void addResourceHyperlinkInSeries(StringBuilder sb, OnlineResource resource, String displayText,
        String color);

    void addResourceHyperlinkInSeries(StringBuilder sb, OnlineResource resource);

    void addResourceImage(StringBuilder sb, OnlineResource resource);

    void beginFont(StringBuilder sb, String color);

    void endFont(StringBuilder sb);

    void beginHeading(StringBuilder sb, int level);

    void endHeading(StringBuilder sb, int level);

    void beginHTMLBody(StringBuilder sb);

    void endHTMLBody(StringBuilder sb);

    void beginHyperlink(StringBuilder sb, String href);

    void endHyperlink(StringBuilder sb);

    void beginHyperlinkSeries(StringBuilder sb);

    void endHyperlinkSeries(StringBuilder sb);

    void addSpace(StringBuilder sb);

    void addText(StringBuilder sb, String text, int maxCharacters);
}
