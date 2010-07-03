/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.wms;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.util.Logging;
import org.w3c.dom.Element;

/**
 * An implementation of the {@link gov.nasa.worldwind.DataConfiguration} interface, which uses a WMS {@link
 * gov.nasa.worldwind.wms.Capabilities} document as its backing store.
 *
 * @author dcollins
 * @version $Id: CapabilitiesConfiguration.java 11666 2009-06-16 15:49:14Z dcollins $
 */
public class CapabilitiesConfiguration extends BasicDataConfiguration
{
    protected Capabilities caps;

    protected CapabilitiesConfiguration(Element domElement, Capabilities capabilities)
    {
        super(domElement);
        this.caps = capabilities;
    }

    /**
     * Creates an instance of CapabilitiesConfiguration backed by a specified WMS Capabilities document.
     *
     * @param capabilities the backing Capabilities document.
     */
    public CapabilitiesConfiguration(Capabilities capabilities)
    {
        this(getConfigElement(capabilities), capabilities);
    }

    /**
     * Returns the Capabilities document title.
     *
     * @return Capabilities document title.
     */
    public String getName()
    {
        return this.caps.getTitle();
    }

    /**
     * Returns the string "Capabilities".
     *
     * @return "Capabilities"
     */
    public String getType()
    {
        return "Capabilities";
    }

    /**
     * Returns the Capabilities version string.
     *
     * @return Capabilities version.
     */
    public String getVersion()
    {
        return this.caps.getVersion();
    }

    /**
     * Returns this data configurations's backing Capabilities.
     *
     * @return the backing Capabilities.
     */
    public Object getSource()
    {
        return this.caps;
    }

    protected static Element getConfigElement(Capabilities capabilities)
    {
        if (capabilities == null)
        {
            String message = Logging.getMessage("nullValue.CapabilitiesIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return capabilities.getDocument().getDocumentElement();
    }

    protected DataConfiguration createChildConfigInfo(Element domElement)
    {
        return new CapabilitiesConfiguration(domElement, this.caps);
    }
}
