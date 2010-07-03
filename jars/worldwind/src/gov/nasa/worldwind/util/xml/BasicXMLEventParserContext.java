/*
Copyright (C) 2001, 2010 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/

package gov.nasa.worldwind.util.xml;

import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.util.Logging;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.*;
import javax.xml.stream.events.XMLEvent;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author tag
 * @version $Id: BasicXMLEventParserContext.java 13107 2010-02-06 08:25:24Z tgaskins $
 */
public class BasicXMLEventParserContext extends AVListImpl implements XMLEventParserContext
{
    protected static QName DOUBLE = new QName("Double");
    protected static QName STRING = new QName("String");

    protected XMLEventReader reader;
    protected StringXMLEventParser stringParser;
    protected DoubleXMLEventParser doubleParser;
    protected String defaultNamespaceURI = XMLConstants.NULL_NS_URI;

    protected ConcurrentHashMap<QName, XMLEventParser> parsers = new ConcurrentHashMap<QName, XMLEventParser>();

    public BasicXMLEventParserContext()
    {
        this.initializeParsers();
    }

    public BasicXMLEventParserContext(XMLEventReader eventReader)
    {
        this.reader = eventReader;

        this.initializeParsers();
    }

    protected void initializeParsers()
    {
        this.parsers.put(STRING, new StringXMLEventParser());
        this.parsers.put(DOUBLE, new DoubleXMLEventParser());
    }

    public XMLEventReader getEventReader()
    {
        return this.reader;
    }

    public void setEventReader(XMLEventReader reader)
    {
        this.reader = reader;
    }

    public String getDefaultNamespaceURI()
    {
        return defaultNamespaceURI;
    }

    public void setDefaultNamespaceURI(String defaultNamespaceURI)
    {
        this.defaultNamespaceURI = defaultNamespaceURI;
    }

    public boolean hasNext()
    {
        return this.getEventReader().hasNext();
    }

    public XMLEvent nextEvent() throws XMLStreamException
    {
        while (this.hasNext())
        {
            XMLEvent event = this.getEventReader().nextEvent();

            if (event.isCharacters() && event.asCharacters().isWhiteSpace())
                continue;

            return event;
        }

        return null;
    }

    public XMLEventParser allocate(XMLEvent event, XMLEventParser defaultParser)
    {
        return this.getParser(event, defaultParser);
    }

    public XMLEventParser getParser(XMLEvent event)
    {
        return this.getParser(event, null);
    }

    public XMLEventParser getParser(XMLEvent event, XMLEventParser defaultParser)
    {
        if (event == null)
        {
            String message = Logging.getMessage("nullValue.EventIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        QName elementName = event.asStartElement().getName();
        if (elementName == null)
            return null;

        XMLEventParser parser = this.getParser(elementName);

        return parser != null ? parser : defaultParser;
    }

    public StringXMLEventParser getStringParser()
    {
        if (this.stringParser == null)
            this.stringParser = (StringXMLEventParser) this.getParser(STRING);

        return this.stringParser;
    }

    public DoubleXMLEventParser getDoubleParser()
    {
        if (this.doubleParser == null)
            this.doubleParser = (DoubleXMLEventParser) this.getParser(DOUBLE);

        return this.doubleParser;
    }

    public String getCharacters(XMLEvent event)
    {
        if (event == null)
        {
            String message = Logging.getMessage("nullValue.EventIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return event.isCharacters() ? event.asCharacters().getData() : null;
    }

    @SuppressWarnings({"SimplifiableIfStatement"})
    public boolean isSameName(QName qa, QName qb)
    {
        if (qa.equals(qb))
            return true;

        if (!qa.getLocalPart().equals(qb.getLocalPart()))
            return false;

        if (qa.getNamespaceURI().equals(XMLConstants.NULL_NS_URI))
            return qb.getNamespaceURI().equals(this.getDefaultNamespaceURI());

        if (qb.getNamespaceURI().equals(XMLConstants.NULL_NS_URI))
            return qa.getNamespaceURI().equals(this.getDefaultNamespaceURI());

        return false;
    }

    public boolean isStartElement(XMLEvent event, QName elementName)
    {
        if (event == null)
        {
            String message = Logging.getMessage("nullValue.EventIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (elementName == null)
        {
            String message = Logging.getMessage("nullValue.ElementNameIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return (event.isStartElement() && this.isSameName(event.asStartElement().getName(), elementName));
    }

    public boolean isStartElement(XMLEvent event, List<QName> elementNames)
    {
        if (event == null)
        {
            String message = Logging.getMessage("nullValue.EventIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (elementNames == null)
        {
            String message = Logging.getMessage("nullValue.ElementNameIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (!event.isStartElement())
            return false;

        for (QName name : elementNames)
        {
            if (this.isSameName(event.asStartElement().getName(), name))
                return true;
        }

        return false;
    }

    public boolean isEndElement(XMLEvent event, XMLEvent startElement)
    {
        if (event == null || startElement == null)
        {
            String message = Logging.getMessage("nullValue.EventIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return isEndElementEvent(event, startElement);
    }

    public static boolean isEndElementEvent(XMLEvent event, XMLEvent startElement)
    {
        if (event == null || startElement == null)
        {
            String message = Logging.getMessage("nullValue.EventIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return (event.isEndElement()
            && event.asEndElement().getName().equals(startElement.asStartElement().getName()));
    }

    public void registerParser(QName elementName, XMLEventParser parser)
    {
        if (parser == null)
        {
            String message = Logging.getMessage("nullValue.ParserIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (elementName == null)
        {
            String message = Logging.getMessage("nullValue.ElementNameIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.parsers.put(elementName, parser);
    }

    public XMLEventParser getParser(QName name)
    {
        if (name == null)
        {
            String message = Logging.getMessage("nullValue.ElementNameIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        XMLEventParser templateParser = this.parsers.get(name);
        if (templateParser == null)
        {
            // Try alternate forms that assume a default namespace in either the input name or the table key.
            if (this.isNullNamespace(name.getNamespaceURI()))
            {
                // input name has no namespace but table key has the default namespace
                QName altName = new QName(this.getDefaultNamespaceURI(), name.getLocalPart());
                templateParser = this.parsers.get(altName);
            }
            else if (this.isDefaultNamespace(name.getNamespaceURI()))
            {
                // input name has the default namespace but table name has no namespace
                QName altName = new QName(name.getLocalPart());
                templateParser = this.parsers.get(altName);
            }
        }

        try
        {
            return templateParser != null ? templateParser.newInstance() : null;
        }
        catch (Exception e)
        {
            String message = Logging.getMessage("XML.ParserCreationException", name);
            Logging.logger().log(java.util.logging.Level.WARNING, message, e);
            return null;
        }
    }

    public boolean isNullNamespace(String namespaceURI)
    {
        return namespaceURI == null || XMLConstants.NULL_NS_URI.equals(namespaceURI);
    }

    public boolean isDefaultNamespace(String namespaceURI)
    {
        return this.getDefaultNamespaceURI() != null && this.getDefaultNamespaceURI().equals(namespaceURI);
    }
}
