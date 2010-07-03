/*
Copyright (C) 2001, 2010 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/

package gov.nasa.worldwind.ogc;

import gov.nasa.worldwind.util.xml.*;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/**
 * Parses an OGC DCPType element.
 *
 * @author tag
 * @version $Id$
 */
public class OGCDCType extends AbstractXMLEventParser
{
    protected QName GET;
    protected QName POST;
    protected QName HTTP;
    protected QName ONLINE_RESOURCE;

    protected String protocol;
    protected String requestMethod;
    protected OGCOnlineResource onlineResource;

    public OGCDCType(String namespaceURI)
    {
        super(namespaceURI);

        this.initialize();
    }

    @Override
    public XMLEventParser allocate(XMLEventParserContext ctx, XMLEvent event)
    {
        XMLEventParser defaultParser = null;

        if (ctx.isStartElement(event, ONLINE_RESOURCE))
            defaultParser = new OGCOnlineResource(this.getNamespaceURI());

        return ctx.allocate(event, defaultParser);
    }

    private void initialize()
    {
        GET = new QName(this.getNamespaceURI(), "Get");
        POST = new QName(this.getNamespaceURI(), "Post");
        HTTP = new QName(this.getNamespaceURI(), "HTTP");
        ONLINE_RESOURCE = new QName(this.getNamespaceURI(), "OnlineResource");
    }

    @Override
    protected void doParseEventContent(XMLEventParserContext ctx, XMLEvent event, Object... args)
        throws XMLStreamException
    {
        if (ctx.isStartElement(event, GET) || ctx.isStartElement(event, POST))
        {
            this.setRequestMethod(event.asStartElement().getName().getLocalPart());
        }
        else if (ctx.isStartElement(event, HTTP))
        {
            this.setProtocol(event.asStartElement().getName().getLocalPart());
        }
        else if (ctx.isStartElement(event, ONLINE_RESOURCE))
        {
            XMLEventParser parser = this.allocate(ctx, event);
            if (parser != null)
            {
                Object o = parser.parse(ctx, event, args);
                if (o != null && o instanceof OGCOnlineResource)
                    this.setOnlineResource((OGCOnlineResource) o);
            }
        }
    }

    public OGCOnlineResource getOnlineResource()
    {
        return onlineResource;
    }

    protected void setOnlineResource(OGCOnlineResource onlineResource)
    {
        this.onlineResource = onlineResource;
    }

    public String getProtocol()
    {
        return protocol;
    }

    protected void setProtocol(String protocol)
    {
        this.protocol = protocol;
    }

    public String getRequestMethod()
    {
        return requestMethod;
    }

    protected void setRequestMethod(String requestMethod)
    {
        this.requestMethod = requestMethod;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        if (this.getProtocol() != null)
            sb.append(this.getProtocol()).append(", ");
        if (this.getRequestMethod() != null)
            sb.append(this.getRequestMethod()).append(", ");
        if (this.getOnlineResource() != null)
            sb.append(this.getOnlineResource().toString());

        return sb.toString();
    }
}
