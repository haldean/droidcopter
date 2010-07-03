/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.applications.gos.html;

import gov.nasa.worldwind.applications.gos.*;
import gov.nasa.worldwind.util.WWUtil;

/**
 * @author dcollins
 * @version $Id: RecordHTMLFormatter.java 13128 2010-02-16 04:48:05Z dcollins $
 */
public class RecordHTMLFormatter extends BasicHTMLFormatter
{
    public RecordHTMLFormatter()
    {
    }

    public void addRecordDescription(StringBuilder sb, Record record, int maxCharacters)
    {
        String s = record.getAbstract();
        if (!WWUtil.isEmpty(s))
        {
            this.addText(sb, s, maxCharacters);
            this.addLineBreak(sb);
        }
    }

    public void addRecordDescription(StringBuilder sb, Record record)
    {
        this.addRecordDescription(sb, record, 200);
    }

    public void addRecordIcons(StringBuilder sb, Record record)
    {
        OnlineResource r = record.getResource(GeodataKey.IMAGE);
        if (r != null)
        {
            this.addResourceImage(sb, r);
            this.addLineBreak(sb);
            this.addLineBreak(sb);
        }

        r = record.getResource(GeodataKey.SERVICE_STATUS);
        if (r != null)
        {
            ServiceStatus serviceStatus = ResourceUtil.getCachedServiceStatus(r);
            if (serviceStatus != null)
            {
                OnlineResource sr = serviceStatus.getScoreImageResource();
                if (sr != null)
                    this.addResourceImage(sb, sr);
            }
        }
    }

    public void addRecordLinks(StringBuilder sb, Record record)
    {
        this.beginHyperlinkSeries(sb);

        OnlineResource r = record.getResource(GeodataKey.METADATA);
        if (r != null)
            this.addResourceHyperlinkInSeries(sb, r);

        r = record.getResource(GeodataKey.SERVICE_STATUS_METADATA);
        if (r != null)
            this.addResourceHyperlinkInSeries(sb, r);

        this.endHyperlinkSeries(sb);
    }

    public void addRecordTitle(StringBuilder sb, Record record)
    {
        String s = record.getTitle();
        if (!WWUtil.isEmpty(s))
        {
            this.beginHeading(sb, 3);
            this.addResourceHyperlink(sb, record.getResource(GeodataKey.WEBSITE), s, null);
            this.endHeading(sb, 3);
        }
    }
}
