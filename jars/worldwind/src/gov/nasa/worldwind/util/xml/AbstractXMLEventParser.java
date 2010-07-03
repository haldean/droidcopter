/*
Copyright (C) 2001, 2010 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/

package gov.nasa.worldwind.util.xml;

import gov.nasa.worldwind.util.Logging;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.lang.reflect.Constructor;

/**
 * Base class for XML event parsers. Handles parsing control and creation of new parser instances.
 * <p/>
 * A parser holds the information parsed from the event stream. That information can be queried via the parser's
 * accessors. A parser typically does not maintain a reference to the event stream it parsed or the parser context used
 * during parsing.
 * <p/>
 * Parsers are created when events of the associated type are encountered in the input stream. An {@link
 * #allocate(XMLEventParserContext, javax.xml.stream.events.XMLEvent)} method in the parser typically creates a default
 * parser prior to consulting the {@link XMLEventParserContext}, which returns a new parser whose type is determined by
 * consulting a table of event types. The default parser is returned if the table contains no entry for the event type.
 * <p/>
 * A parser can be associated with a specific namespace. The namespace is used to qualify the parser's association with
 * event types.
 *
 * @author tag
 * @version $Id: AbstractXMLEventParser.java 13107 2010-02-06 08:25:24Z tgaskins $
 */
abstract public class AbstractXMLEventParser implements XMLEventParser
{
    protected final String namespaceURI;

    /** Construct a parser with no qualifying namespace. */
    public AbstractXMLEventParser()
    {
        this.namespaceURI = null;
    }

    /**
     * Constructs a parser and qualifies it for a specified namespace.
     *
     * @param namespaceURI the qualifying namespace URI. May be null to indicate no namespace qualification.
     */
    public AbstractXMLEventParser(String namespaceURI)
    {
        this.namespaceURI = namespaceURI;
    }

    /**
     * Returns the qualifying namespace URI specified at construction.
     *
     * @return the namesapce URI. Returns null if no name space was specified at construction.
     */
    public String getNamespaceURI()
    {
        return this.namespaceURI;
    }

    /** {@inheritDoc} */
    public XMLEventParser newInstance() throws Exception
    {
        Constructor<? extends AbstractXMLEventParser> constructor = this.getAConstructor(String.class);
        if (constructor != null)
            return constructor.newInstance(this.getNamespaceURI());

        constructor = this.getAConstructor();
        if (constructor != null)
            return constructor.newInstance();

        return null;
    }

    protected Constructor<? extends AbstractXMLEventParser> getAConstructor(Class... parameterTypes)
    {
        try
        {
            return this.getClass().getConstructor(parameterTypes);
        }
        catch (NoSuchMethodException e)
        {
            return null;
        }
    }

    /**
     * Create a parser for a specified event.
     *
     * @param ctx   the current parser context.
     * @param event the event for which the parser is created. Only the event type is used; the new parser can operate
     *              on any event of that type.
     *
     * @return the new parser.
     */
    public XMLEventParser allocate(XMLEventParserContext ctx, XMLEvent event)
    {
        return ctx.allocate(event, null);
    }

    /** {@inheritDoc} */
    public Object parse(XMLEventParserContext ctx, XMLEvent inputEvent, Object... args) throws XMLStreamException
    {
        if (ctx == null)
        {
            String message = Logging.getMessage("nullValue.ParserContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (inputEvent == null)
        {
            String message = Logging.getMessage("nullValue.EventIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        // Parse this event's attributes and text content.
        this.doParseEventAttributes(ctx, inputEvent, args);

        // Parse the events subelements.
        for (XMLEvent event = ctx.nextEvent(); ctx.hasNext(); event = ctx.nextEvent())
        {
            if (event == null)
                continue;

            if (ctx.isEndElement(event, inputEvent))
                return this;

            this.doParseEventContent(ctx, event, args);
        }

        return null;
    }

    /**
     * Parse an event's sub-elements.
     *
     * @param ctx   a current parser context.
     * @param event the event to parse.
     * @param args  an optional list of arguments that may by used by subclasses.
     *
     * @throws XMLStreamException if an exception occurs during event-stream reading.
     */
    protected void doParseEventContent(XMLEventParserContext ctx, XMLEvent event, Object... args)
        throws XMLStreamException
    {
        // Override in subclass to parse an event's sub-elements.
    }

    /**
     * Parse an event's attributes.
     *
     * @param ctx   a current parser context.
     * @param event the event to parse.
     * @param args  an optional list of arguments that may by used by subclasses.
     */
    protected void doParseEventAttributes(XMLEventParserContext ctx, XMLEvent event, Object... args)
    {
        // Override in subclass to parse event's attributes, if any.
    }
}
