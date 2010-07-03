/*
Copyright (C) 2001, 2010 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/

package gov.nasa.worldwind.util.xml;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/**
 * Parse a string from an XML event.
 *
 * @author tag
 * @version $Id: StringXMLEventParser.java 13107 2010-02-06 08:25:24Z tgaskins $
 */
public class StringXMLEventParser extends AbstractXMLEventParser
{
    public Object parse(XMLEventParserContext ctx, XMLEvent stringEvent, Object... args) throws XMLStreamException
    {
        StringBuilder value = new StringBuilder();

        for (XMLEvent event = ctx.nextEvent(); event != null; event = ctx.nextEvent())
        {
            if (ctx.isEndElement(event, stringEvent))
                return value.length() > 0 ? value.toString() : null;

            if (event.isCharacters())
            {
                String s = ctx.getCharacters(event);
                if (s != null)
                    value.append(s);
            }
        }

        return null;
    }

    public String parseString(XMLEventParserContext ctx, XMLEvent stringEvent, Object... args) throws XMLStreamException
    {
        return (String) this.parse(ctx, stringEvent, args);
    }
}
