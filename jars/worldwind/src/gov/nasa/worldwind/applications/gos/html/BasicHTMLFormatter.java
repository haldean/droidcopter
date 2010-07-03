/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.applications.gos.html;

import gov.nasa.worldwind.applications.gos.OnlineResource;
import gov.nasa.worldwind.util.WWUtil;

import java.net.URI;

/**
 * @author dcollins
 * @version $Id: BasicHTMLFormatter.java 13127 2010-02-16 04:02:26Z dcollins $
 */
public class BasicHTMLFormatter implements HTMLFormatter
{
    protected boolean enableAdvancedHtml = true;
    protected int numLinksInSeries;

    public BasicHTMLFormatter()
    {
    }

    public boolean isEnableAdvancedHTML()
    {
        return this.enableAdvancedHtml;
    }

    public void setEnableAdvancedHTML(boolean enable)
    {
        this.enableAdvancedHtml = enable;
    }

    public void addImage(StringBuilder sb, URI uri, String text)
    {
        sb.append("<img ");

        if (uri != null)
            sb.append(" src=\"").append(uri).append("\"");

        if (!WWUtil.isEmpty(text))
            sb.append(" alt=\"").append(text).append("\"");

        sb.append("/>");
    }

    public void addLineBreak(StringBuilder sb)
    {
        sb.append("<br/>");
    }

    public void addResourceHyperlink(StringBuilder sb, OnlineResource resource, String displayText, String color)
    {
        if (WWUtil.isEmpty(displayText) && resource != null)
            displayText = resource.getDisplayText();

        if (resource != null && resource.getURI() != null)
            this.beginHyperlink(sb, resource.getURI().toString());

        if (color != null)
            this.beginFont(sb, color);

        if (!WWUtil.isEmpty(displayText))
            sb.append(displayText);

        if (color != null)
            this.endFont(sb);

        if (resource != null && resource.getURI() != null)
            this.endHyperlink(sb);
    }

    public void addResourceHyperlink(StringBuilder sb, OnlineResource resource)
    {
        this.addResourceHyperlink(sb, resource, null, null);
    }

    public void addResourceHyperlinkInSeries(StringBuilder sb, OnlineResource resource, String displayText,
        String color)
    {
        if (this.numLinksInSeries > 0)
        {
            this.addSpace(sb);
            sb.append("-");
            this.addSpace(sb);
        }

        this.addResourceHyperlink(sb, resource, displayText, color);
        this.numLinksInSeries++;
    }

    public void addResourceHyperlinkInSeries(StringBuilder sb, OnlineResource resource)
    {
        this.addResourceHyperlinkInSeries(sb, resource, null, null);
    }

    public void addResourceImage(StringBuilder sb, OnlineResource resource)
    {
        this.addImage(sb, resource.getURI(), resource.getDisplayText());
    }

    public void addSpace(StringBuilder sb)
    {
        sb.append(this.isEnableAdvancedHTML() ? "&nbsp;" : " ");
    }

    public void addText(StringBuilder sb, String text, int maxCharacters)
    {
        boolean truncate = false;

        if (maxCharacters > 0)
        {
            int len = maxCharacters - 3;
            if (text.endsWith("..."))
                len = len - 3;

            if (len > 0 && len < text.length() - 1)
            {
                text = text.substring(0, len);
                text = text.trim();
                truncate = true;
            }
        }

        sb.append(text);

        if (truncate)
            sb.append("...");
    }

    public void beginFont(StringBuilder sb, String color)
    {
        sb.append("<font color=\"").append(color).append("\">");
    }

    public void endFont(StringBuilder sb)
    {
        sb.append("</font>");
    }

    public void beginHeading(StringBuilder sb, int level)
    {
        if (this.isEnableAdvancedHTML())
        {
            sb.append("<h").append(level).append(">");
        }
        else
        {
            sb.append("<b>");
        }
    }

    public void endHeading(StringBuilder sb, int level)
    {
        if (this.isEnableAdvancedHTML())
        {
            sb.append("</h").append(level).append(">");
        }
        else
        {
            sb.append("</b><br/>");
        }
    }

    public void beginHTMLBody(StringBuilder sb)
    {
        sb.append("<html><head/><body>");
    }

    public void endHTMLBody(StringBuilder sb)
    {
        sb.append("</body></html>");
    }

    public void beginHyperlink(StringBuilder sb, String href)
    {
        sb.append("<a href=\"").append(href).append("\">");
    }

    public void endHyperlink(StringBuilder sb)
    {
        sb.append("</a>");
    }

    public void beginHyperlinkSeries(StringBuilder sb)
    {
        this.numLinksInSeries = 0;
    }

    public void endHyperlinkSeries(StringBuilder sb)
    {
        this.numLinksInSeries = 0;
    }
}
