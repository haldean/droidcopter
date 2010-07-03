/*
Copyright (C) 2001, 2010 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/

package gov.nasa.worldwind.util.xml;

import gov.nasa.worldwind.avlist.AVList;

import javax.xml.namespace.QName;
import javax.xml.stream.*;
import javax.xml.stream.events.XMLEvent;

/**
 * Provides services and resources used by XML event parsers during event reading and parsing.
 *
 * @author tag
 * @version $Id: XMLEventParserContext.java 13107 2010-02-06 08:25:24Z tgaskins $
 */
public interface XMLEventParserContext extends AVList
{
    /**
     * Returns the event reader associated with the context.
     *
     * @return the associated event reader, or null if no reader is associated.
     */
    XMLEventReader getEventReader();

    /**
     * Returns a new parser for a specified event.
     *
     * @param event indicates the element name for which a parser is created.
     *
     * @return the new parser, or null if no parser has been registred for the specified event's element name.
     */
    XMLEventParser getParser(XMLEvent event);

    /**
     * Returns a new parser for a specified element name.
     *
     * @param eventName indicates the element name for which a parser is created.
     *
     * @return the new parser, or null if no parser has been registred for the specified element name.
     */
    XMLEventParser getParser(QName eventName);

    /**
     * Determines whether an event is a start event for a specific event type.
     *
     * @param event       an event identifying the event type of interest.
     * @param elementName the event name.
     *
     * @return true if the event is a start event for the named event type.
     */
    boolean isStartElement(XMLEvent event, QName elementName);

    /**
     * Determines whether an event is the corresponding end element for a specified start event.
     * <p/>
     * Note: Only the event's element name and type are compared. The method returns true if the start and end events
     * are the corresponding event types for an element of the same name.
     *
     * @param event        the event of interest.
     * @param startElement the start event associated with the potential end event.
     *
     * @return true if the event is the corresponding end event to the specified start event, otherwise false.
     */
    boolean isEndElement(XMLEvent event, XMLEvent startElement);

    /**
     * Returns the text associated with the event.
     *
     * @param event the event of interest.
     *
     * @return the event's characters, or null if the event is not a character event.
     */
    String getCharacters(XMLEvent event);

    /**
     * Returns a parser for a simple string. The parser context may maintain one string parser for re-use.
     *
     * @return a string parser.
     */
    StringXMLEventParser getStringParser();

    /**
     * Returns a parser for a simple number. The parser context may maintain one Double parser for re-use.
     *
     * @return a Double parser.
     */
    DoubleXMLEventParser getDoubleParser();

    /**
     * Registers a parser for a specified element name. A parser of the same type and namespace is returned when {@link
     * #getParser(javax.xml.stream.events.XMLEvent)} is called for the same element name.
     *
     * @param elementName the element name for which to return a parser.
     * @param parser      the parser to register.
     */
    void registerParser(QName elementName, XMLEventParser parser);

    /**
     * Indicates whether the event stream associated with this context contains another event.
     *
     * @return true if the stream contains another event, otherwise false.
     *
     * @see javax.xml.stream.XMLEventReader#hasNext()
     */
    boolean hasNext();

    /**
     * Returns the next event in the event stream associated with this context.
     *
     * @return the next event,
     *
     * @throws XMLStreamException if there is an error with the underlying XML.
     * @see javax.xml.stream.XMLEventReader#nextEvent()
     */
    XMLEvent nextEvent() throws XMLStreamException;

    /**
     * Returns the context's default namespace URI.
     *
     * @return the context's default namespace URI.
     *
     * @see #setDefaultNamespaceURI(String)
     */
    String getDefaultNamespaceURI();

    /**
     * Specifies the context's default namespace URI.
     *
     * @param defaultNamespaceURI the default namespace URI.
     *
     * @see #getDefaultNamespaceURI()
     * @see #isSameName(javax.xml.namespace.QName, javax.xml.namespace.QName)
     */
    void setDefaultNamespaceURI(String defaultNamespaceURI);

    /**
     * Determines whether two element names are the same.
     *
     * @param qa
     * @param qb
     *
     * @return true if both names have the same namespace (or no namesace) and local name, or if either name has no
     *         namespace but the namespace of the other is the context's default namespace.
     */
    boolean isSameName(QName qa, QName qb);

    /**
     * Create a parser for a specified event's element name, if a parser for that name is registered with the context.
     *
     * @param event         the event whose element name identifies the parser to create.
     * @param defaultParser a parser to return if no parser is registered for the specified name. May be null.
     *
     * @return a new parser, or the specified default parser if no parser has been registered for the element name.
     */
    XMLEventParser allocate(XMLEvent event, XMLEventParser defaultParser);
}
