/*
Copyright (C) 2001, 2009 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind;

import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.exception.*;
import gov.nasa.worldwind.ogc.OGCCapabilities;
import gov.nasa.worldwind.util.*;
import gov.nasa.worldwind.wms.Capabilities;
import org.w3c.dom.*;

import java.io.File;

/**
 * A basic implementation of the {@link Factory} interface.
 *
 * @author tag
 * @version $Id: BasicFactory.java 13320 2010-04-21 00:28:50Z tgaskins $
 */
public class BasicFactory implements Factory
{
    /**
     * Static method to create an object from a factory and configuration source.
     *
     * @param factoryKey   the key identifying the factory in {@link Configuration}.
     * @param configSource the configuration source. May be any of the types listed for {@link
     *                     #createFromConfigSource(Object, gov.nasa.worldwind.avlist.AVList)}
     *
     * @return a new instance of the requested object.
     */
    public static Object create(String factoryKey, Object configSource)
    {
        if (factoryKey == null)
        {
            String message = Logging.getMessage("generic.FactoryKeyIsNull");
            throw new IllegalArgumentException(message);
        }

        if (WWUtil.isEmpty(configSource))
        {
            String message = Logging.getMessage("generic.ConfigurationSourceIsInvalid", configSource);
            throw new IllegalArgumentException(message);
        }

        Factory factory = (Factory) WorldWind.createConfigurationComponent(factoryKey);
        return factory.createFromConfigSource(configSource, null);
    }

    /**
     * Creates an object from a general configuration source. The source can be one of the following: <ul> <li>a {@link
     * java.net.URL}</li> <li>a {@link java.io.File}</li> <li>a {@link java.io.InputStream}</li> <li>{@link
     * Element}</li> <li>a {@link String} holding a file name, a name of a resource on the classpath, or a string
     * represenation of a URL</li></ul>
     * <p/>
     *
     * @param configSource the configuration source. See above for supported types.
     * @param params       key-value parameters to override or supplement the information provided in the specified
     *                     configuration. May be null.
     *
     * @return the new object.
     *
     * @throws IllegalArgumentException if the configuration file name is null or an empty string.
     * @throws WWUnrecognizedException  if the source type is unrecognized.
     * @throws WWRuntimeException       if object creation fails. The exception indicating the source of the failure is
     *                                  included as the {@link Exception#initCause(Throwable)}.
     */
    public Object createFromConfigSource(Object configSource, AVList params)
    {
        if (WWUtil.isEmpty(configSource))
        {
            String message = Logging.getMessage("generic.ConfigurationSourceIsInvalid", configSource);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Object o = null;

        try
        {
            if (configSource instanceof Element)
            {
                o = this.doCreateFromElement((Element) configSource, params);
            }
            else if (configSource instanceof OGCCapabilities)
                o = this.doCreateFromCapabilities((OGCCapabilities) configSource, params);
            else
            {
                Document doc = WWXML.openDocument(configSource);
                if (doc != null)
                    o = this.doCreateFromElement(doc.getDocumentElement(), params);
            }
        }
        catch (Exception e)
        {
            String msg = Logging.getMessage("generic.CreationFromConfigurationFileFailed", configSource);
            throw new WWRuntimeException(msg, e);
        }

        return o;
    }

    /**
     * A no-op implementation of the {@link Factory#createFromDataConfig(DataConfiguration,
     * gov.nasa.worldwind.avlist.AVList)} method provided so that this class can be instantiated. Not all factories need
     * a <code>createFromDataConfig</code> method; this class is therefore useful as is in these cases.
     *
     * @param dataConfig normally the configuration information describing the component.
     * @param params     normally key-value parameters which will override or supplement the information provided in the
     *                   specified configuration. A null reference is permitted.
     *
     * @return this no-op method always returns null.
     */
    public Object createFromDataConfig(DataConfiguration dataConfig, AVList params)
    {
        return null;
    }

    /**
     * Create an object such as a layer or elevation model given a local OGC capabilities document containing named
     * layer descriptions.
     *
     * @param capsFileName the path to the capabilities file. The file must be either an absolute path or a relative
     *                     path available on the classpath. The file contents must be a valid OGC capabilities
     *                     document.
     * @param params       a list of configuration properties. These properties override any specified in the
     *                     capabilities document. The list should contain the {@link AVKey#LAYER_NAMES} property for
     *                     services that define layer, indicating which named layers described in the capabilities
     *                     document to create. If this argumet is null or contains no layers, the first named layer is
     *                     used.
     *
     * @return the requested object.
     *
     * @throws IllegalArgumentException if the file name is null.
     * @throws IllegalStateException    if the capabilites document contains no named layer definitions.
     * @throws WWRuntimeException       if an error occurs while opening, reading or parsing the capabilities document.
     */
    public Object createFromCapabilities(String capsFileName, AVList params)
    {
        if (capsFileName == null)
        {
            String message = Logging.getMessage("nullValue.FilePathIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        File capsFile = new File(capsFileName);
        if (!capsFile.exists())
        {
            String message = Logging.getMessage("generic.FileNotFound", capsFile.getPath());
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Capabilities caps = Capabilities.parse(WWXML.openDocumentFile(capsFileName, BasicFactory.class));

        return this.doCreateFromCapabilities(caps, params);
    }

    /**
     * Implemented by subclasses to perform the actual object creation. This default implementation always returns
     * null.
     *
     * @param caps   the capabilities document.
     * @param params a list of configuration properties. These properties override any specified in the capabilities
     *               document. The list should contain the {@link AVKey#LAYER_NAMES} property for services that define
     *               layers, indicating which named layers described in the capabilities document to create. If this
     *               argumet is null or contains no layers, the first named layer is used.
     *
     * @return the requested object.
     */
    protected Object doCreateFromCapabilities(Capabilities caps, AVList params)
    {
        return null;
    }

    /**
     * Implemented by subclasses to perform the actual object creation. This default implementation always returns
     * null.
     *
     * @param caps   the capabilities document.
     * @param params a list of configuration properties. These properties override any specified in the capabilities
     *               document. The list should contain the {@link AVKey#LAYER_NAMES} property for services that define
     *               layers, indicating which named layers described in the capabilities document to create. If this
     *               argumet is null or contains no layers, the first named layer is used.
     *
     * @return the requested object.
     */
    protected Object doCreateFromCapabilities(OGCCapabilities caps, AVList params)
    {
        return null;
    }

    protected Object doCreateFromElement(Element domElement, AVList params) throws Exception
    {
        return null;
    }
}
