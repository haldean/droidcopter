/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.layers;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.render.ScreenCredit;
import gov.nasa.worldwind.util.*;
import gov.nasa.worldwind.wms.Capabilities;
import org.w3c.dom.*;

import javax.xml.xpath.XPath;

/**
 * An implementation of the {@link gov.nasa.worldwind.DataConfiguration} interface, which uses a {@link Layer}
 * configuration document as its backing store.
 *
 * @author dcollins
 * @version $Id: LayerConfiguration.java 13257 2010-04-08 16:55:59Z dcollins $
 */
public class LayerConfiguration extends BasicDataConfiguration
{
    /**
     * Creates an instance of LayerConfiguration backed by a specified DOM {@link org.w3c.dom.Element}.
     *
     * @param domElement the backing DOM Element.
     */
    public LayerConfiguration(Element domElement)
    {
        super(domElement);
    }

    /**
     * Returns the text content of the <code>DisplayName</code> element.
     *
     * @return Layer's display name.
     */
    public String getName()
    {
        return this.getString("DisplayName");
    }

    /**
     * Returns the string "Layer".
     *
     * @return "Layer"
     */
    public String getType()
    {
        return "Layer";
    }

    /**
     * Returns the layer's version.
     *
     * @return layer's version.
     */
    public String getVersion()
    {
        return this.getString("@version");
    }

    protected DataConfiguration createChildConfigInfo(Element domElement)
    {
        return new LayerConfiguration(domElement);
    }

    /**
     * Returns true if a specified DOM document should be accepted as a Layer configuration document, and false
     * otherwise.
     *
     * @param domElement the DOM document in question.
     *
     * @return true if the document is a Layer configuration document; false otherwise.
     *
     * @throws IllegalArgumentException if document is null.
     */
    public static boolean isLayerDocument(Element domElement)
    {
        if (domElement == null)
        {
            String message = Logging.getMessage("nullValue.DocumentIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        XPath xpath = WWXML.makeXPath();
        Element[] elements = WWXML.getElements(domElement, "//Layer", xpath);

        return elements != null && elements.length > 0;
    }

    /**
     * Creates a configuration document for the tiled image layer described by the specified params. This document's
     * root element may be passed to a LayerConfiguration constructor, and it may be passed to the constructor of a
     * {@link gov.nasa.worldwind.layers.BasicTiledImageLayer}.
     *
     * @param params parameters describing the tiled image layer.
     *
     * @return a configuration document for the tiled image layer.
     */
    public static Document createTiledImageLayerDocument(AVList params)
    {
        Document doc = WWXML.createDocumentBuilder(true).newDocument();

        Element root = WWXML.setDocumentElement(doc, "Layer");
        WWXML.setIntegerAttribute(root, "version", 1);
        WWXML.setTextAttribute(root, "layerType", "TiledImageLayer");

        createLayerElements(params, root);
        createTiledImageLayerElements(params, root);

        return doc;
    }

    /**
     * Parses layer parameters from a specified DOM document. This writes output as key-value pairs to params. If a
     * parameter from the XML document already exists in params, that parameter is ignored. Supported key and parameter
     * names are: <table> <tr><th>Key</th><th>Name</th><th>Type</th></tr> <tr><td>{@link
     * AVKey#DISPLAY_NAME}</td><td>DisplayName</td><td>String</td></tr> <tr><td>{@link
     * AVKey#OPACITY}</td><td>Opacity</td><td>Double</td></tr> <tr><td>{@link AVKey#MAX_ACTIVE_ALTITUDE}</td><td>ActiveAltitudes/@max</td><td>Double</td></tr>
     * <tr><td>{@link AVKey#MIN_ACTIVE_ALTITUDE}</td><td>ActiveAltitudes/@min</td><td>Double</td></tr> <tr><td>{@link
     * AVKey#NETWORK_RETRIEVAL_ENABLED}</td><td>NetworkRetrievalEnabled</td><td>Boolean</td></tr> <tr><td>{@link
     * AVKey#MAP_SCALE}</td><td>MapScale</td><td>Double</td></tr> <tr><td>{@link AVKey#SCREEN_CREDIT}</td><td>ScreenCredit</td><td>{@link
     * ScreenCredit}</td></tr> </table>
     *
     * @param domElement the XML document root to parse for layer parameters.
     * @param params     the output key-value pairs which recieve the layer parameters. A null reference is permitted.
     *
     * @return a reference to params, or a new AVList if params is null.
     *
     * @throws IllegalArgumentException if the document is null.
     */
    public static AVList getLayerParams(Element domElement, AVList params)
    {
        if (domElement == null)
        {
            String message = Logging.getMessage("nullValue.DocumentIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (params == null)
            params = new AVListImpl();

        XPath xpath = WWXML.makeXPath();

        WWXML.checkAndSetStringParam(domElement, params, AVKey.DISPLAY_NAME, "DisplayName", xpath);
        WWXML.checkAndSetDoubleParam(domElement, params, AVKey.OPACITY, "Opacity", xpath);
        WWXML.checkAndSetDoubleParam(domElement, params, AVKey.MAX_ACTIVE_ALTITUDE, "ActiveAltitudes/@max", xpath);
        WWXML.checkAndSetDoubleParam(domElement, params, AVKey.MIN_ACTIVE_ALTITUDE, "ActiveAltitudes/@min", xpath);
        WWXML.checkAndSetBooleanParam(domElement, params, AVKey.NETWORK_RETRIEVAL_ENABLED, "NetworkRetrievalEnabled",
            xpath);
        WWXML.checkAndSetDoubleParam(domElement, params, AVKey.MAP_SCALE, "MapScale", xpath);
        WWXML.checkAndSetScreenCreditParam(domElement, params, AVKey.SCREEN_CREDIT, "ScreenCredit", xpath);
        WWXML.checkAndSetIntegerParam(domElement, params, AVKey.MAX_ABSENT_TILE_ATTEMPTS, "MaxAbsentTileAttempts",
            xpath);
        WWXML.checkAndSetIntegerParam(domElement, params, AVKey.MIN_ABSENT_TILE_CHECK_INTERVAL,
            "MinAbsentTileCheckInterval", xpath);

        return params;
    }

    /**
     * Parses tiled image layer parameters from a specified DOM document. This also parses common LevelSet parameters by
     * invoking {@link gov.nasa.worldwind.util.DataConfigurationUtils#getLevelSetParams(org.w3c.dom.Element,
     * gov.nasa.worldwind.avlist.AVList)}. This writes output as key-value pairs to params. If a parameter from the XML
     * document already exists in params, that parameter is ignored. Supported key and parameter names are: <table>
     * <tr><th>Key</th><th>Name</th><th>Type</th></tr> <tr><td>{@link AVKey#SERVICE_NAME}</td><td>Service/@serviceName</td><td>String</td></tr>
     * <tr><td>{@link AVKey#IMAGE_FORMAT}</td><td>ImageFormat</td><td>String</td></tr> <tr><td>{@link
     * AVKey#AVAILABLE_IMAGE_FORMATS}</td><td>AvailableImageFormats/ImageFormat</td><td>String array</td></tr>
     * <tr><td>{@link AVKey#FORCE_LEVEL_ZERO_LOADS}</td><td>ForceLevelZeroLoads</td><td>Boolean</td></tr> <tr><td>{@link
     * AVKey#RETAIN_LEVEL_ZERO_TILES}</td><td>RetainLevelZeroTiles</td><td>Boolean</td></tr> <tr><td>{@link
     * AVKey#COMPRESS_TEXTURES}</td><td>CompressTextures</td><td>Boolean</td></tr> <tr><td>{@link
     * AVKey#USE_MIP_MAPS}</td><td>UseMipMaps</td><td>Boolean</td></tr> <tr><td>{@link
     * AVKey#USE_TRANSPARENT_TEXTURES}</td><td>UseTransparentTextures</td><td>Boolean</td></tr> <tr><td>{@link
     * AVKey#URL_CONNECT_TIMEOUT}</td><td>RetrievalTimeouts/ConnectTimeout/Time</td><td>Integer milliseconds</td></tr>
     * <tr><td>{@link AVKey#URL_READ_TIMEOUT}</td><td>RetrievalTimeouts/ReadTimeout/Time</td><td>Integer
     * milliseconds</td></tr> <tr><td>{@link AVKey#RETRIEVAL_QUEUE_STALE_REQUEST_LIMIT}</td><td>RetrievalTimeouts/StaleRequestLimit/Time</td><td>Integer
     * milliseconds</td></tr> </table>
     *
     * @param domElement the XML document root to parse for tiled image layer parameters.
     * @param params     the output key-value pairs which recieve the tiled image layer parameters. A null reference is
     *                   permitted.
     *
     * @return a reference to params, or a new AVList if params is null.
     *
     * @throws IllegalArgumentException if the document is null.
     */
    public static AVList getTiledImageLayerParams(Element domElement, AVList params)
    {
        if (domElement == null)
        {
            String message = Logging.getMessage("nullValue.DocumentIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (params == null)
            params = new AVListImpl();

        XPath xpath = WWXML.makeXPath();

        // LevelSet properties.
        DataConfigurationUtils.getLevelSetParams(domElement, params);

        // Service properties.
        WWXML.checkAndSetStringParam(domElement, params, AVKey.SERVICE_NAME, "Service/@serviceName", xpath);
        WWXML.checkAndSetBooleanParam(domElement, params, AVKey.RETRIEVE_PROPERTIES_FROM_SERVICE,
            "RetrievePropertiesFromService", xpath);

        // Image format properties.
        WWXML.checkAndSetStringParam(domElement, params, AVKey.IMAGE_FORMAT, "ImageFormat", xpath);
        WWXML.checkAndSetUniqueStringsParam(domElement, params, AVKey.AVAILABLE_IMAGE_FORMATS,
            "AvailableImageFormats/ImageFormat", xpath);

        // Optional behavior properties.
        WWXML.checkAndSetBooleanParam(domElement, params, AVKey.FORCE_LEVEL_ZERO_LOADS, "ForceLevelZeroLoads", xpath);
        WWXML.checkAndSetBooleanParam(domElement, params, AVKey.RETAIN_LEVEL_ZERO_TILES, "RetainLevelZeroTiles", xpath);
        WWXML.checkAndSetBooleanParam(domElement, params, AVKey.COMPRESS_TEXTURES, "CompressTextures",
            xpath);
        WWXML.checkAndSetBooleanParam(domElement, params, AVKey.USE_MIP_MAPS, "UseMipMaps", xpath);
        WWXML.checkAndSetBooleanParam(domElement, params, AVKey.USE_TRANSPARENT_TEXTURES, "UseTransparentTextures",
            xpath);
        WWXML.checkAndSetDoubleParam(domElement, params, AVKey.SPLIT_SCALE, "SplitScale", xpath);
        WWXML.checkAndSetColorArrayParam(domElement, params, AVKey.TRANSPARENCY_COLORS, "TransparencyColors/Color",
            xpath);

        // Retrieval properties. Convert the Long time values to Integers, because BasicTiledImageLayer is expecting
        // Integer values.
        WWXML.checkAndSetTimeParamAsInteger(domElement, params, AVKey.URL_CONNECT_TIMEOUT,
            "RetrievalTimeouts/ConnectTimeout/Time", xpath);
        WWXML.checkAndSetTimeParamAsInteger(domElement, params, AVKey.URL_READ_TIMEOUT,
            "RetrievalTimeouts/ReadTimeout/Time", xpath);
        WWXML.checkAndSetTimeParamAsInteger(domElement, params, AVKey.RETRIEVAL_QUEUE_STALE_REQUEST_LIMIT,
            "RetrievalTimeouts/StaleRequestLimit/Time", xpath);

        return params;
    }

    /**
     * Parses WMS tiled image layer parameters from a specified DOM document. This also parses common WMS layer
     * parameters by invoking {@link DataConfigurationUtils#getWMSLayerParams(org.w3c.dom.Element,
     * gov.nasa.worldwind.avlist.AVList)}. This writes output as key-value pairs to params. If a parameter from the XML
     * document already exists in params, that parameter is ignored.
     *
     * @param domElement the XML document root to parse for WMS tiled image layer parameters.
     * @param params     the output key-value pairs which recieve the WMS tiled image layer parameters. A null reference
     *                   is permitted.
     *
     * @return a reference to params, or a new AVList if params is null.
     *
     * @throws IllegalArgumentException if the document is null.
     */
    public static AVList getWMSTiledImageLayerParams(Element domElement, AVList params)
    {
        if (domElement == null)
        {
            String message = Logging.getMessage("nullValue.DocumentIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (params == null)
            params = new AVListImpl();

        // Common WMS layer properties.
        DataConfigurationUtils.getWMSLayerParams(domElement, params);

        return params;
    }

    /**
     * Parses WMS tiled image layer parameters from a specified WMS {@link Capabilities} document. This also parses
     * common WMS layer parameters by invoking {@link DataConfigurationUtils#getWMSLayerParams(gov.nasa.worldwind.wms.Capabilities,
     * String[], gov.nasa.worldwind.avlist.AVList)}. This writes output as key-value pairs to params.
     *
     * @param caps                  the WMS Capabilities document to parse for WMS layer parameters.
     * @param formatOrderPreference an ordered array of preferred image formats, or null to use the default format.
     * @param params                the output key-value pairs which recieve the WMS layer parameters.
     *
     * @return a reference to params.
     *
     * @throws IllegalArgumentException if either the document or params are null, or if params does not contain the
     *                                  required key-value pairs.
     * @throws gov.nasa.worldwind.exception.WWRuntimeException
     *                                  if the Capabilities document does not contain any of the required information.
     */
    public static AVList getWMSTiledImageLayerParams(Capabilities caps, String[] formatOrderPreference, AVList params)
    {
        if (caps == null)
        {
            String message = Logging.getMessage("nullValue.WMSCapabilities");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (params == null)
        {
            String message = Logging.getMessage("nullValue.ElevationModelConfigParams");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        DataConfigurationUtils.getWMSLayerParams(caps, formatOrderPreference, params);

        return params;
    }

    /**
     * Appends layer parameters as elements to a specified context. If a parameter key exists, that parameter is
     * appended to the context. Supported key and element paths are: <table> <tr><th>Key</th><th>Name</th><th>Type</th></tr>
     * <tr><td>{@link AVKey#DISPLAY_NAME}</td><td>DisplayName</td><td>String</td></tr> <tr><td>{@link
     * AVKey#OPACITY}</td><td>Opacity</td><td>Double</td></tr> <tr><td>{@link AVKey#MAX_ACTIVE_ALTITUDE}</td><td>ActiveAltitudes/@max</td><td>Double</td></tr>
     * <tr><td>{@link AVKey#MIN_ACTIVE_ALTITUDE}</td><td>ActiveAltitudes/@min</td><td>Double</td></tr> <tr><td>{@link
     * AVKey#NETWORK_RETRIEVAL_ENABLED}</td><td>NetworkRetrievalEnabled</td><td>Boolean</td></tr> <tr><td>{@link
     * AVKey#MAP_SCALE}</td><td>MapScale</td><td>Double</td></tr> <tr><td>{@link AVKey#SCREEN_CREDIT}</td><td>ScreenCredit</td><td>ScreenCredit</td></tr>
     * </table>
     *
     * @param params  the key-value pairs which define the layer parameters.
     * @param context the XML document root on which to append parameter elements.
     *
     * @return a reference to context.
     *
     * @throws IllegalArgumentException if either the parameters or the context are null.
     */
    public static Element createLayerElements(AVList params, Element context)
    {
        if (params == null)
        {
            String message = Logging.getMessage("nullValue.ParametersIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (context == null)
        {
            String message = Logging.getMessage("nullValue.ContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        WWXML.checkAndAppendTextElement(params, AVKey.DISPLAY_NAME, context, "DisplayName");
        WWXML.checkAndAppendDoubleElement(params, AVKey.OPACITY, context, "Opacity");

        Double maxAlt = AVListImpl.getDoubleValue(params, AVKey.MAX_ACTIVE_ALTITUDE);
        Double minAlt = AVListImpl.getDoubleValue(params, AVKey.MIN_ACTIVE_ALTITUDE);
        if (maxAlt != null || minAlt != null)
        {
            Element el = WWXML.appendElementPath(context, "ActiveAltitudes");
            if (maxAlt != null)
                WWXML.setDoubleAttribute(el, "max", maxAlt);
            if (minAlt != null)
                WWXML.setDoubleAttribute(el, "min", minAlt);
        }

        WWXML.checkAndAppendBooleanElement(params, AVKey.NETWORK_RETRIEVAL_ENABLED, context, "NetworkRetrievalEnabled");
        WWXML.checkAndAppendDoubleElement(params, AVKey.MAP_SCALE, context, "MapScale");
        WWXML.checkAndAppendScreenCreditElement(params, AVKey.SCREEN_CREDIT, context, "ScreenCredit");

        return context;
    }

    /**
     * Appends tiled image layer parameters as elements to a specified context. If a parameter key exists, that
     * parameter is appended to the context. This also writes LevelSet parameters by invoking {@link
     * DataConfigurationUtils#createLevelSetElements(gov.nasa.worldwind.avlist.AVList, org.w3c.dom.Element)}. Supported
     * key and element paths are: <table> <tr><th>Key</th><th>Name</th><th>Type</th></tr> <tr><td>{@link
     * AVKey#SERVICE_NAME}</td><td>Service/@serviceName</td><td>String</td></tr> <tr><td>{@link
     * AVKey#IMAGE_FORMAT}</td><td>ImageFormat</td><td>String</td></tr> <tr><td>{@link
     * AVKey#AVAILABLE_IMAGE_FORMATS}</td><td>AvailableImageFormats/ImageFormat</td><td>String array</td></tr>
     * <tr><td>{@link AVKey#FORCE_LEVEL_ZERO_LOADS}</td><td>ForceLevelZeroLoads</td><td>Boolean</td></tr> <tr><td>{@link
     * AVKey#RETAIN_LEVEL_ZERO_TILES}</td><td>RetainLevelZeroTiles</td><td>Boolean</td></tr> <tr><td>{@link
     * AVKey#COMPRESS_TEXTURES}</td><td>CompressTextures</td><td>Boolean</td></tr> <tr><td>{@link
     * AVKey#USE_MIP_MAPS}</td><td>UseMipMaps</td><td>Boolean</td></tr> <tr><td>{@link
     * AVKey#USE_TRANSPARENT_TEXTURES}</td><td>UseTransparentTextures</td><td>Boolean</td></tr> <tr><td>{@link
     * AVKey#URL_CONNECT_TIMEOUT}</td><td>RetrievalTimeouts/ConnectTimeout/Time</td><td>Integer milliseconds</td></tr>
     * <tr><td>{@link AVKey#URL_READ_TIMEOUT}</td><td>RetrievalTimeouts/ReadTimeout/Time</td><td>Integer
     * milliseconds</td></tr> <tr><td>{@link AVKey#RETRIEVAL_QUEUE_STALE_REQUEST_LIMIT}</td><td>RetrievalTimeouts/StaleRequestLimit/Time</td><td>Integer
     * milliseconds</td></tr> </table>
     *
     * @param params  the key-value pairs which define the tiled image layer parameters.
     * @param context the XML document root on which to append parameter elements.
     *
     * @return a reference to context.
     *
     * @throws IllegalArgumentException if either the parameters or the context are null.
     */
    public static Element createTiledImageLayerElements(AVList params, Element context)
    {
        if (params == null)
        {
            String message = Logging.getMessage("nullValue.ParametersIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (context == null)
        {
            String message = Logging.getMessage("nullValue.ContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        XPath xpath = WWXML.makeXPath();

        // LevelSet properties.
        DataConfigurationUtils.createLevelSetElements(params, context);

        // Service properties.
        // Try to get the SERVICE_NAME property, but default to "WWTileService".
        String s = AVListImpl.getStringValue(params, AVKey.SERVICE_NAME, "WWTileService");
        if (s != null && s.length() > 0)
        {
            // The service element may already exist, in which case we want to append to it.
            Element el = WWXML.getElement(context, "Service", xpath);
            if (el == null)
                el = WWXML.appendElementPath(context, "Service");
            WWXML.setTextAttribute(el, "serviceName", s);
        }

        WWXML.checkAndAppendBooleanElement(params, AVKey.RETRIEVE_PROPERTIES_FROM_SERVICE, context,
            "RetrievePropertiesFromService");

        // Image format properties.
        WWXML.checkAndAppendTextElement(params, AVKey.IMAGE_FORMAT, context, "ImageFormat");

        Object o = params.getValue(AVKey.AVAILABLE_IMAGE_FORMATS);
        if (o != null && o instanceof String[])
        {
            String[] strings = (String[]) o;
            if (strings.length > 0)
            {
                // The available image formats element may already exists, in which case we want to append to it, rather
                // than create entirely separate paths.
                Element el = WWXML.getElement(context, "AvailableImageFormats", xpath);
                if (el == null)
                    el = WWXML.appendElementPath(context, "AvailableImageFormats");
                WWXML.appendTextArray(el, "ImageFormat", strings);
            }
        }

        // Optional behavior properties.
        WWXML.checkAndAppendBooleanElement(params, AVKey.FORCE_LEVEL_ZERO_LOADS, context, "ForceLevelZeroLoads");
        WWXML.checkAndAppendBooleanElement(params, AVKey.RETAIN_LEVEL_ZERO_TILES, context, "RetainLevelZeroTiles");
        WWXML.checkAndAppendBooleanElement(params, AVKey.COMPRESS_TEXTURES, context, "CompressTextures");
        WWXML.checkAndAppendBooleanElement(params, AVKey.USE_MIP_MAPS, context, "UseMipMaps");
        WWXML.checkAndAppendBooleanElement(params, AVKey.USE_TRANSPARENT_TEXTURES, context, "UseTransparentTextures");
        WWXML.checkAndAppendDoubleElement(params, AVKey.SPLIT_SCALE, context, "SplitScale");

        // Retrieval properties.
        if (params.getValue(AVKey.URL_CONNECT_TIMEOUT) != null ||
            params.getValue(AVKey.URL_READ_TIMEOUT) != null ||
            params.getValue(AVKey.RETRIEVAL_QUEUE_STALE_REQUEST_LIMIT) != null)
        {
            Element el = WWXML.getElement(context, "RetrievalTimeouts", xpath);
            if (el == null)
                el = WWXML.appendElementPath(context, "RetrievalTimeouts");

            WWXML.checkAndAppendTimeElement(params, AVKey.URL_CONNECT_TIMEOUT, el, "ConnectTimeout/Time");
            WWXML.checkAndAppendTimeElement(params, AVKey.URL_READ_TIMEOUT, el, "ReadTimeout/Time");
            WWXML.checkAndAppendTimeElement(params, AVKey.RETRIEVAL_QUEUE_STALE_REQUEST_LIMIT, el,
                "StaleRequestLimit/Time");
        }

        return context;
    }

    /**
     * Appends WMS tiled image layer parameters as elements to a specified context. If a parameter key exists, that
     * parameter is appended to the context. This also writes common WMS layer elements by invoking {@link
     * DataConfigurationUtils#createWMSLayerElements(gov.nasa.worldwind.avlist.AVList, org.w3c.dom.Element)}.
     *
     * @param params  the key-value pairs which define the WMS tiled image layer parameters.
     * @param context the XML document root on which to append parameter elements.
     *
     * @return a reference to context.
     *
     * @throws IllegalArgumentException if either the parameters or the context are null.
     */
    public static Element createWMSTiledImageLayerElements(AVList params, Element context)
    {
        if (params == null)
        {
            String message = Logging.getMessage("nullValue.ParametersIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (context == null)
        {
            String message = Logging.getMessage("nullValue.ContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        // Common WMS layer elements.
        DataConfigurationUtils.createWMSLayerElements(params, context);

        return context;
    }
}
