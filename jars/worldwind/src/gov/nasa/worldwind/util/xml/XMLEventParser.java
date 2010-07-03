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
 * @author tag
 * @version $Id: XMLEventParser.java 13107 2010-02-06 08:25:24Z tgaskins $
 */
public interface XMLEventParser
{
    /**
     * Parse the event and initialize the parser's values to those found in the event.
     *
     * @param context a current parser context.
     * @param event   the event to parse.
     * @param args    an optional list of arguments that may by used by subclasses.
     *
     * @return if parsing is successful, returns <code>this</code>, otherwise returns null.
     *
     * @throws XMLStreamException if an exception occurs during event-stream reading.
     */
    Object parse(XMLEventParserContext context, XMLEvent event, Object... args) throws XMLStreamException;

    /**
     * Creates a new empty parser instance of the same type. This is used by {@link
     * gov.nasa.worldwind.util.xml.XMLEventParserContext} when creating parsers associated with specific event types.
     * The returned parser has the same namespace as the instance creating it, but has empty fields for all other
     * values.
     *
     * @return a new parser instance. The namespace URI is the same as the creating parser, but all other fields are
     *         empty.
     *
     * @throws Exception if an error or exception occurs while attempting to create the parser.
     */
    XMLEventParser newInstance() throws Exception;
}
