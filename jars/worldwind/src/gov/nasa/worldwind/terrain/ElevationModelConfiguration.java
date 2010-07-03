/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.terrain;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.ogc.wms.WMSCapabilities;
import gov.nasa.worldwind.util.*;
import gov.nasa.worldwind.wms.Capabilities;
import org.w3c.dom.*;

import javax.xml.xpath.XPath;

/**
 * An implementation of the {@link gov.nasa.worldwind.DataConfiguration} interface, which uses a {@link
 * gov.nasa.worldwind.globes.ElevationModel} configuration document as its backing store.
 *
 * @author dcollins
 * @version $Id: ElevationModelConfiguration.java 13280 2010-04-10 23:12:08Z tgaskins $
 */
public class ElevationModelConfiguration extends BasicDataConfiguration
{
    /**
     * Creates an instance of ElevationModelConfiguration backed by a specified DOM {@link org.w3c.dom.Element}.
     *
     * @param domElement the backing DOM Element.
     */
    public ElevationModelConfiguration(Element domElement)
    {
        super(domElement);
    }

    /**
     * Returns the text content of the <code>DisplayName</code> element.
     *
     * @return elevation model's display name.
     */
    public String getName()
    {
        return this.getString("DisplayName");
    }

    /**
     * Returns the string "ElevationModel".
     *
     * @return "ElevationModel"
     */
    public String getType()
    {
        return "ElevationModel";
    }

    /**
     * Returns the elevation model's version.
     *
     * @return elevation model's version.
     */
    public String getVersion()
    {
        return this.getString("@version");
    }

    protected DataConfiguration createChildConfigInfo(Element domElement)
    {
        return new ElevationModelConfiguration(domElement);
    }

    /**
     * Returns true if a specified DOM document should be accepted as an ElevationModel configuration document, and
     * false otherwise.
     *
     * @param domElement the DOM document in question.
     *
     * @return true if the document is an ElevationModel configuration document; false otherwise.
     *
     * @throws IllegalArgumentException if document is null.
     */
    public static boolean isElevationModelDocument(Element domElement)
    {
        if (domElement == null)
        {
            String message = Logging.getMessage("nullValue.DocumentIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        XPath xpath = WWXML.makeXPath();
        Element[] elements = WWXML.getElements(domElement, "//ElevationModel", xpath);

        return elements != null && elements.length > 0;
    }

    /**
     * Creates a configuration document for the basic elevation model described by the specified params. This document's
     * root element may be passed to a ElevationModelConfiguration constructor, and it may be passed to the constructor
     * of a {@link gov.nasa.worldwind.terrain.BasicElevationModel}.
     *
     * @param params parameters describing the basic elevation model.
     *
     * @return a configuration document for the basic elevation model.
     */
    public static Document createBasicElevationModelDocument(AVList params)
    {
        Document doc = WWXML.createDocumentBuilder(true).newDocument();

        Element root = WWXML.setDocumentElement(doc, "ElevationModel");
        // Note: no type attribute denotes the default elevation model, which currently is BasicElevationModel.
        WWXML.setIntegerAttribute(root, "version", 1);

        createElevationModelElements(params, root);
        createBasicElevationModelElements(params, root);

        return doc;
    }

    /**
     * Parses elevation model parameters from a specified DOM document. This writes output as key-value pairs to params.
     * If a parameter from the XML document already exists in params, that parameter is ignored. Supported key and
     * parameter names are: <table> <th><td>Key</td><td>Name</td><td>Type</td></th> <tr><td>{@link
     * gov.nasa.worldwind.avlist.AVKey#DISPLAY_NAME}</td><td>DisplayName</td><td>String</td></tr> <tr><td>{@link
     * gov.nasa.worldwind.avlist.AVKey#NETWORK_RETRIEVAL_ENABLED}</td><td>NetworkRetrievalEnabled</td><td>Boolean</td></tr>
     * <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#MISSING_DATA_SIGNAL}</td><td>MissingData/@signal</td><td>Double</td></tr>
     * <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#MISSING_DATA_REPLACEMENT}</td><td>MissingData/@replacement</td><td>Double</td></tr>
     * <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#DETAIL_HINT}</td><td>DataDetailHint</td><td>Double</td></tr>
     * </table>
     *
     * @param domElement the XML document root to parse for elevaiton model parameters.
     * @param params     the output key-value pairs which recieve the elevation model parameters. A null reference is
     *                   permitted.
     *
     * @return a reference to params, or a new AVList if params is null.
     *
     * @throws IllegalArgumentException if the document is null.
     */
    public static AVList getElevationModelParams(Element domElement, AVList params)
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
        WWXML.checkAndSetBooleanParam(domElement, params, AVKey.NETWORK_RETRIEVAL_ENABLED, "NetworkRetrievalEnabled",
            xpath);
        WWXML.checkAndSetDoubleParam(domElement, params, AVKey.MISSING_DATA_SIGNAL, "MissingData/@signal", xpath);
        WWXML.checkAndSetDoubleParam(domElement, params, AVKey.MISSING_DATA_REPLACEMENT, "MissingData/@replacement",
            xpath);
        WWXML.checkAndSetDoubleParam(domElement, params, AVKey.DETAIL_HINT, "DataDetailHint", xpath);
        WWXML.checkAndSetIntegerParam(domElement, params, AVKey.MAX_ABSENT_TILE_ATTEMPTS, "MaxAbsentTileAttempts",
            xpath);
        WWXML.checkAndSetIntegerParam(domElement, params, AVKey.MIN_ABSENT_TILE_CHECK_INTERVAL,
            "MinAbsentTileCheckInterval", xpath);

        return params;
    }

    /**
     * Parses basic elevation model parameters from a specified DOM document. This also parses LevelSet parameters by
     * invoking {@link gov.nasa.worldwind.util.DataConfigurationUtils#getLevelSetParams(org.w3c.dom.Element,
     * gov.nasa.worldwind.avlist.AVList)}. This writes output as key-value pairs to params. If a parameter from the XML
     * document already exists in params, that parameter is ignored. Supported key and parameter names are: <table>
     * <th><td>Key</td><td>Name</td><td>Type</td></th> <tr><td>{@link AVKey#SERVICE_NAME}</td><td>Service/@serviceName</td><td>String</td></tr>
     * <tr><td>{@link AVKey#IMAGE_FORMAT}</td><td>ImageFormat</td><td>String</td></tr> <tr><td>{@link
     * AVKey#AVAILABLE_IMAGE_FORMATS}</td><td>AvailableImageFormats/ImageFormat</td><td>String array</td></tr>
     * <tr><td>{@link AVKey#PIXEL_TYPE}</td><td>DataType</td><td>String</td></tr> <tr><td>{@link
     * AVKey#BYTE_ORDER}</td><td>DataType/@byteOrder</td><td>String</td></tr> <tr><td>{@link
     * AVKey#ELEVATION_EXTREMES_FILE}</td><td>ExtremeElevations/FileName</td><td>String</td></tr> <tr><td>{@link
     * AVKey#ELEVATION_MAX}</td><td>ExtremeElevations/@max</td><td>Double</td></tr> <tr><td>{@link
     * AVKey#ELEVATION_MIN}</td><td>ExtremeElevations/@min</td><td>Double</td></tr> </table>
     *
     * @param domElement the XML document root to parse for basic elevation model parameters.
     * @param params     the output key-value pairs which recieve the basic elevation model parameters. A null reference
     *                   is permitted.
     *
     * @return a reference to params, or a new AVList if params is null.
     *
     * @throws IllegalArgumentException if the document is null.
     */
    public static AVList getBasicElevationModelParams(Element domElement, AVList params)
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

        // Data type properties.
        if (params.getValue(AVKey.PIXEL_TYPE) == null)
        {
            String s = WWXML.getText(domElement, "DataType/@type", xpath);
            if (s != null && s.length() > 0)
            {
                s = WWXML.parseDataType(s);
                if (s != null && s.length() > 0)
                    params.setValue(AVKey.PIXEL_TYPE, s);
            }
        }

        if (params.getValue(AVKey.BYTE_ORDER) == null)
        {
            String s = WWXML.getText(domElement, "DataType/@byteOrder", xpath);
            if (s != null && s.length() > 0)
            {
                s = WWXML.parseByteOrder(s);
                if (s != null && s.length() > 0)
                    params.setValue(AVKey.BYTE_ORDER, s);
            }
        }

        // Elevation data properties.
        WWXML.checkAndSetStringParam(domElement, params, AVKey.ELEVATION_EXTREMES_FILE, "ExtremeElevations/FileName",
            xpath);
        WWXML.checkAndSetDoubleParam(domElement, params, AVKey.ELEVATION_MAX, "ExtremeElevations/@max", xpath);
        WWXML.checkAndSetDoubleParam(domElement, params, AVKey.ELEVATION_MIN, "ExtremeElevations/@min", xpath);

        return params;
    }

    /**
     * Parses WMS elevation model parameters from a specified DOM document. This also parses common WMS layer parameters
     * by invoking {@link DataConfigurationUtils#getWMSLayerParams(org.w3c.dom.Element,
     * gov.nasa.worldwind.avlist.AVList)}. This writes output as key-value pairs to params. If a parameter from the XML
     * document already exists in params, that parameter is ignored. Supported key and parameter names are: <table>
     * <th><td>Key</td><td>Name</td><td>Type</td></th> <tr><td>{@link AVKey#PIXEL_TYPE}</td><td>PixelType</td><td>String</td></tr>
     * </table>
     *
     * @param domElement the XML document root to parse for tiled image layer parameters.
     * @param params     the output key-value pairs which recieve the tiled image layer parameters. A null reference is
     *                   permitted.
     *
     * @return a reference to params, or a new AVList if params is null.
     *
     * @throws IllegalArgumentException if the document is null.
     */
    public static AVList getWMSElevationModelParams(Element domElement, AVList params)
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
     * Parses WMS elevation model parameters from a specified WMS {@link Capabilities} document. This also parses common
     * WMS layer parameters by invoking {@link DataConfigurationUtils#getWMSLayerParams(gov.nasa.worldwind.wms.Capabilities,
     * String[], gov.nasa.worldwind.avlist.AVList)}. This writes output as key-value pairs to params. Supported key and
     * parameter names are: <table> <th><td>Key</td><td>Value</td><td>Type</td></th> <tr><td>{@link
     * AVKey#ELEVATION_MAX}</td><td>WMS layer's maximum extreme elevation</td><td>Double</td></tr> <tr><td>{@link
     * AVKey#ELEVATION_MIN}</td><td>WMS layer's minimum extreme elevation</td><td>Double</td></tr> <tr><td>{@link
     * AVKey#PIXEL_TYPE}</td><td>Translate WMS layer's image format to a matching pixel type</td><td>String</td></tr>
     * </table>
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
    public static AVList getWMSElevationModelParams(Object caps, String[] formatOrderPreference, AVList params)
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

        if (caps instanceof Capabilities)
            DataConfigurationUtils.getWMSLayerParams((Capabilities) caps, formatOrderPreference, params);
        else if (caps instanceof WMSCapabilities)
            DataConfigurationUtils.getWMSLayerParams((WMSCapabilities) caps, formatOrderPreference, params);

        // Attempt to extract the WMS layer names from the specified parameters.
        String layerNames = params.getStringValue(AVKey.LAYER_NAMES);
        if (layerNames == null || layerNames.length() == 0)
        {
            String message = Logging.getMessage("nullValue.WMSLayerNames");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        String[] names = layerNames.split(",");
        if (names == null || names.length == 0)
        {
            String message = Logging.getMessage("nullValue.WMSLayerNames");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        // Get the layer's extreme elevations.
        Double[] extremes = null;
        if (caps instanceof Capabilities)
            extremes = ((Capabilities) caps).getLayerExtremeElevations((Capabilities) caps, names);
        else if (caps instanceof WMSCapabilities)
            extremes = ((WMSCapabilities) caps).getLayerExtremeElevations((WMSCapabilities) caps, names);

        Double d = (Double) params.getValue(AVKey.ELEVATION_MIN);
        if (d == null && extremes != null && extremes[0] != null)
            params.setValue(AVKey.ELEVATION_MIN, extremes[0]);

        d = (Double) params.getValue(AVKey.ELEVATION_MAX);
        if (d == null && extremes != null && extremes[1] != null)
            params.setValue(AVKey.ELEVATION_MAX, extremes[1]);

        // Compute the internal pixel type from the image format.
        if (params.getValue(AVKey.PIXEL_TYPE) == null && params.getValue(AVKey.IMAGE_FORMAT) != null)
        {
            String s = WWIO.makePixelTypeForMimeType(params.getValue(AVKey.IMAGE_FORMAT).toString());
            if (s != null)
                params.setValue(AVKey.PIXEL_TYPE, s);
        }

        // Use the default pixel type.
        if (params.getValue(AVKey.PIXEL_TYPE) == null)
            params.setValue(AVKey.PIXEL_TYPE, AVKey.INT16);

        // Use the default byte order.
        if (params.getValue(AVKey.BYTE_ORDER) == null)
            params.setValue(AVKey.BYTE_ORDER, AVKey.LITTLE_ENDIAN);

        return params;
    }

    /**
     * Appends elevation model parameters as elements to a specified context. If a parameter key exists, that parameter
     * is appended to the context. Supported key and element paths are: <table> <th><td>Key</td><td>Name</td><td>Type</td></th>
     * <tr><td>{@link AVKey#DISPLAY_NAME}</td><td>DisplayName</td><td>String</td></tr> <tr><td>{@link
     * AVKey#NETWORK_RETRIEVAL_ENABLED}</td><td>NetworkRetrievalEnabled</td><td>Boolean</td></tr> <tr><td>{@link
     * AVKey#MISSING_DATA_SIGNAL}</td><td>MissingData/@signal</td><td>Double</td></tr> <tr><td>{@link
     * AVKey#MISSING_DATA_REPLACEMENT}</td><td>MissingData/@replacement</td><td>Double</td></tr> <tr><td>{@link
     * AVKey#DETAIL_HINT}</td><td>DataDetailHint</td><td>Double</td></tr> </table>
     *
     * @param params  the key-value pairs which define the elevation model parameters.
     * @param context the XML document root on which to append parameter elements.
     *
     * @return a reference to context.
     *
     * @throws IllegalArgumentException if either the parameters or the context are null.
     */
    public static Element createElevationModelElements(AVList params, Element context)
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
        WWXML.checkAndAppendBooleanElement(params, AVKey.NETWORK_RETRIEVAL_ENABLED, context, "NetworkRetrievalEnabled");

        if (params.getValue(AVKey.MISSING_DATA_SIGNAL) != null ||
            params.getValue(AVKey.MISSING_DATA_REPLACEMENT) != null)
        {
            Element el = WWXML.getElement(context, "MissingData", null);
            if (el == null)
                el = WWXML.appendElementPath(context, "MissingData");

            Double d = AVListImpl.getDoubleValue(params, AVKey.MISSING_DATA_SIGNAL);
            if (d != null)
                WWXML.setDoubleAttribute(el, "signal", d);

            d = AVListImpl.getDoubleValue(params, AVKey.MISSING_DATA_REPLACEMENT);
            if (d != null)
                WWXML.setDoubleAttribute(el, "replacement", d);
        }

        WWXML.checkAndAppendDoubleElement(params, AVKey.DETAIL_HINT, context, "DataDetailHint");

        return context;
    }

    /**
     * Appends basic elevation model parameters as elements to a specified context. If a parameter key exists, that
     * parameter is appended to the context. This also writes LevelSet parameters by invoking {@link
     * DataConfigurationUtils#createLevelSetElements(gov.nasa.worldwind.avlist.AVList, org.w3c.dom.Element)}. Supported
     * key and element paths are: <table> <th><td>Key</td><td>Name</td><td>Type</td></th> <tr><td>{@link
     * AVKey#SERVICE_NAME}</td><td>Service/@serviceName</td><td>String</td></tr> <tr><td>{@link
     * AVKey#IMAGE_FORMAT}</td><td>ImageFormat</td><td>String</td></tr> <tr><td>{@link
     * AVKey#AVAILABLE_IMAGE_FORMATS}</td><td>AvailableImageFormats/ImageFormat</td><td>String array</td></tr>
     * <tr><td>{@link AVKey#PIXEL_TYPE}</td><td>PixelType</td><td>String</td></tr> <tr><td>{@link
     * AVKey#BYTE_ORDER}</td><td>ByteOrder</td><td>String</td></tr> <tr><td>{@link AVKey#ELEVATION_EXTREMES_FILE}</td><td>ExtremeElevations/FileName</td><td>String</td></tr>
     * <tr><td>{@link AVKey#ELEVATION_MAX}</td><td>ExtremeElevations/@max</td><td>Double</td></tr> <tr><td>{@link
     * AVKey#ELEVATION_MIN}</td><td>ExtremeElevations/@min</td><td>Double</td></tr> </table>
     *
     * @param params  the key-value pairs which define the basic elevation model parameters.
     * @param context the XML document root on which to append parameter elements.
     *
     * @return a reference to context.
     *
     * @throws IllegalArgumentException if either the parameters or the context are null.
     */
    public static Element createBasicElevationModelElements(AVList params, Element context)
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

        // Data type properties.
        if (params.getValue(AVKey.PIXEL_TYPE) != null || params.getValue(AVKey.BYTE_ORDER) != null)
        {
            Element el = WWXML.getElement(context, "DataType", null);
            if (el == null)
                el = WWXML.appendElementPath(context, "DataType");

            s = params.getStringValue(AVKey.PIXEL_TYPE);
            if (s != null && s.length() > 0)
            {
                s = WWXML.dataTypeAsText(s);
                if (s != null && s.length() > 0)
                    WWXML.setTextAttribute(el, "type", s);
            }

            s = params.getStringValue(AVKey.BYTE_ORDER);
            if (s != null && s.length() > 0)
            {
                s = WWXML.byteOrderAsText(s);
                if (s != null && s.length() > 0)
                    WWXML.setTextAttribute(el, "byteOrder", s);
            }
        }

        // Elevation data properties.
        Element el = WWXML.appendElementPath(context, "ExtremeElevations");
        WWXML.checkAndAppendTextElement(params, AVKey.ELEVATION_EXTREMES_FILE, el, "FileName");

        Double d = AVListImpl.getDoubleValue(params, AVKey.ELEVATION_MAX);
        if (d != null)
            WWXML.setDoubleAttribute(el, "max", d);

        d = AVListImpl.getDoubleValue(params, AVKey.ELEVATION_MIN);
        if (d != null)
            WWXML.setDoubleAttribute(el, "min", d);

        return context;
    }

    /**
     * Appends WMS basic elevation model parameters as elements to a specified context. If a parameter key exists, that
     * parameter is appended to the context. This also writes common WMS layer elements by invoking {@link
     * DataConfigurationUtils#createWMSLayerElements(gov.nasa.worldwind.avlist.AVList, org.w3c.dom.Element)}. <table>
     * <th><td>Key</td><td>Name</td><td>Type</td></th> <tr><td>{@link AVKey#PIXEL_TYPE}</td><td>PixelType</td><td>String</td></tr>
     * </table>
     *
     * @param params  the key-value pairs which define the WMS basic elevation model parameters.
     * @param context the XML document root on which to append parameter elements.
     *
     * @return a reference to context.
     *
     * @throws IllegalArgumentException if either the parameters or the context are null.
     */
    public static Element createWMSBasicElevationModelElements(AVList params, Element context)
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
