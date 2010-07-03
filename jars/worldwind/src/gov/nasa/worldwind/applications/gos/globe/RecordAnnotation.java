/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.applications.gos.globe;

import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.applications.gos.html.RecordHTMLFormatter;
import gov.nasa.worldwind.applications.gos.*;
import gov.nasa.worldwind.geom.Position;

import java.awt.*;
import java.awt.image.*;

/**
 * @author dcollins
 * @version $Id: RecordAnnotation.java 13127 2010-02-16 04:02:26Z dcollins $
 */
public class RecordAnnotation extends GlobeAnnotation
{
    protected Record record;

    public RecordAnnotation(Record record)
    {
        super("", Position.ZERO);

        // Assemble the annotation's text.
        StringBuilder sb = new StringBuilder();
        RecordHTMLFormatter formatter = new RecordHTMLFormatter();
        formatter.setEnableAdvancedHTML(false);
        formatter.beginHTMLBody(sb);
        formatter.addRecordTitle(sb, record);
        formatter.addLineBreak(sb);
        formatter.addRecordDescription(sb, record, 128);
        formatter.addLineBreak(sb);
        formatter.addRecordLinks(sb, record);
        formatter.endHTMLBody(sb);
        this.setText(sb.toString());

        // Define the annotation's position.
        this.setPosition(new Position(record.getSector().getCentroid(), 0));

        // Assemble the annotation's default attributes.
        AnnotationAttributes attr = new AnnotationAttributes();
        attr.setSize(new Dimension(300, 0));
        attr.setInsets(new Insets(20, 60, 20, 20)); // top, left, bottom, right
        attr.setImageRepeat(Annotation.IMAGE_REPEAT_NONE);
        attr.setImageOffset(new Point(10, 10));
        this.getAttributes().setDefaults(attr);

        OnlineResource resource = record.getResource(GeodataKey.IMAGE);
        if (resource != null)
        {
            BufferedImage image = ResourceUtil.getImage(resource);
            if (image != null)
                this.getAttributes().setImageSource(image);
        }
    }

    public Record getRecord()
    {
        return this.record;
    }
}
