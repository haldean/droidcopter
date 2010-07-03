/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.util;

import gov.nasa.worldwind.DataConfiguration;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.layers.LayerConfiguration;
import org.w3c.dom.*;

import javax.xml.stream.events.XMLEvent;
import javax.xml.xpath.XPath;

/**
 * A utility class for transforming a World Wind .NET LayerSet document to a standard Layer configuration document.
 *
 * @author dcollins
 * @version $Id: WWDotNetLayerSetConfiguration.java 13257 2010-04-08 16:55:59Z dcollins $
 */
public class WWDotNetLayerSetConfiguration
{
    /**
     * Returns true if a specified document should be accepted as a World Wind .NET LayerSet configuration document, and
     * false otherwise.
     *
     * @param domElement the document in question.
     *
     * @return true if the document is a LayerSet configuration document; false otherwise.
     *
     * @throws IllegalArgumentException if document is null.
     */
    public static boolean isLayerSetDocument(Element domElement)
    {
        if (domElement == null)
        {
            String message = Logging.getMessage("nullValue.DocumentIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        XPath xpath = WWXML.makeXPath();
        Element[] elements = WWXML.getElements(domElement, "/LayerSet", xpath);

        return elements != null && elements.length > 0;
    }

    /**
     * Returns true if a specified XML event should be accepted as the root of a World Wind .NET LayerSet configuration
     * document, and false otherwise.
     *
     * @param event the XML event in question.
     *
     * @return true if the event is a LayerSet configuration document element; false otherwise.
     *
     * @throws IllegalArgumentException if the event is null.
     */
    public static boolean isLayerSetEvent(XMLEvent event)
    {
        if (event == null)
        {
            String message = Logging.getMessage("nullValue.EventIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (!event.isStartElement())
            return false;

        String name = WWXML.getUnqalifiedName(event.asStartElement());
        return name != null && name.equals("LayerSet");
    }

    /**
     * Parses tiled image layer parameters from a specified LayerSet configuration document. This writes output as
     * key-value pairs to params. If a parameter from the LayerSet document already exists in params, that parameter is
     * ignored. Supported key and parameter names are: <table> <tr><th>Key</th><th>Name</th><th>Type</th></tr>
     * <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#DISPLAY_NAME}</td><td>QuadTileSet/Name<td></td><td>String</td></tr>
     * <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#DATASET_NAME}</td><td>QuadTileSet/Name<td></td><td>String</td></tr>
     * <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#OPACITY}</td><td>QuadTileSet/Opacity<td></td><td>Double</td></tr>
     * <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#SERVICE_NAME}</td><td>"Offline" (string
     * constant)<td></td><td>String</td></tr> <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#FORMAT_SUFFIX}</td><td>QuadTileSet/ImageAccessor/ImageFileExtension<td></td><td>String</td></tr>
     * <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#IMAGE_FORMAT}</td><td>QuadTileSet/ImageAccessor/ImageFileExtension
     * (converted to mime type)<td></td><td>String</td></tr> <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#AVAILABLE_IMAGE_FORMATS}</td><td>QuadTileSet/ImageAccessor/ImageFileExtension
     * (converted to mime type)<td></td><td>String array</td></tr> <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#NUM_LEVELS}</td><td>QuadTileSet/ImageAccessor/NumberLevels<td></td><td>Integer</td></tr>
     * <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#NUM_EMPTY_LEVELS}</td><td>0 (integer
     * constant)<td></td><td>Integer</td></tr> <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#SECTOR}</td><td>QuadTileSet/BoundingBox<td></td><td>Sector</td></tr>
     * <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#TILE_ORIGIN}</td><td>(-90, -180) (geographic location
     * constant)<td></td><td>LatLon</td></tr> <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#LEVEL_ZERO_TILE_DELTA}</td><td>QuadTileSet/ImageAccessor/LevelZeroTileSizeDegrees<td></td><td>LatLon</td></tr>
     * <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#TILE_WIDTH}</td><td>QuadTileSet/ImageAccessor/TextureSizePixels<td></td><td>Integer</td></tr>
     * <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#TILE_HEIGHT}</td><td>QuadTileSet/ImageAccessor/TextureSizePixels<td></td><td>Integer</td></tr>
     * <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#NETWORK_RETRIEVAL_ENABLED}</td><td>false (boolean
     * constant)<td></td><td>Boolean</td></tr> <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#COMPRESS_TEXTURES}</td><td>true
     * if ImageAccessor/ImageFileExtension is "dds"; false otherwise<td></td><td>Boolean</td></tr> <tr><td>{@link
     * gov.nasa.worldwind.avlist.AVKey#USE_MIP_MAPS}</td><td>true (boolean constant)<td></td><td>Boolean</td></tr>
     * <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#USE_TRANSPARENT_TEXTURES}</td><td>true (boolean
     * constant)<td></td><td>Boolean</td></tr> </table>
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

        XPath xpath = WWXML.makeXPath();

        // Find the first QuadTileSet element in this LayerSet document.
        Element el = WWXML.getElement(domElement, "QuadTileSet", xpath);
        if (el == null)
            return params;

        if (params == null)
            params = new AVListImpl();

        // Title and cache name properties.
        WWXML.checkAndSetStringParam(el, params, AVKey.DISPLAY_NAME, "Name", xpath);
        WWXML.checkAndSetStringParam(el, params, AVKey.DATASET_NAME, "Name", xpath);

        // Display properties.
        if (params.getValue(AVKey.OPACITY) == null)
        {
            Double d = WWXML.getDouble(el, "Opacity", xpath);
            if (d != null)
                params.setValue(AVKey.OPACITY, d / 255d);
        }

        // Service properties.
        // LayerSet documents always describe an offline pyramid of tiled imagery in the file store, Therefore we define
        // the service as "Offline".
        if (params.getValue(AVKey.SERVICE_NAME) == null)
            params.setValue(AVKey.SERVICE_NAME, "Offline");

        // Image format properties.
        if (params.getValue(AVKey.FORMAT_SUFFIX) == null)
        {
            String s = WWXML.getText(el, "ImageAccessor/ImageFileExtension", xpath);
            if (s != null && s.length() != 0)
            {
                if (!s.startsWith("."))
                    s = "." + s;
                params.setValue(AVKey.FORMAT_SUFFIX, s);
            }
        }

        // LayerSet documents contain a format suffix, but not image format type. Convert the format suffix to a
        // mime type, then use it to populate the IMAGE_FORMAT and AVAILABLE_IMAGE_FORMAT properties.
        if (params.getValue(AVKey.FORMAT_SUFFIX) != null)
        {
            String s = WWIO.makeMimeTypeForSuffix(params.getValue(AVKey.FORMAT_SUFFIX).toString());
            if (s != null)
            {
                if (params.getValue(AVKey.IMAGE_FORMAT) == null)
                    params.setValue(AVKey.IMAGE_FORMAT, s);
                if (params.getValue(AVKey.AVAILABLE_IMAGE_FORMATS) == null)
                    params.setValue(AVKey.AVAILABLE_IMAGE_FORMATS, new String[] {s});
            }
        }

        // Tile structure properties.
        WWXML.checkAndSetIntegerParam(el, params, AVKey.NUM_LEVELS, "ImageAccessor/NumberLevels", xpath);

        if (params.getValue(AVKey.NUM_EMPTY_LEVELS) == null)
            params.setValue(AVKey.NUM_EMPTY_LEVELS, 0);

        if (params.getValue(AVKey.SECTOR) == null)
        {
            Sector s = getLayerSetSector(el, "BoundingBox", xpath);
            if (s != null)
                params.setValue(AVKey.SECTOR, s);
        }

        if (params.getValue(AVKey.TILE_ORIGIN) == null)
            params.setValue(AVKey.TILE_ORIGIN, new LatLon(Angle.NEG90, Angle.NEG180));

        if (params.getValue(AVKey.LEVEL_ZERO_TILE_DELTA) == null)
        {
            LatLon ll = getLayerSetLatLon(el, "ImageAccessor/LevelZeroTileSizeDegrees", xpath);
            if (ll != null)
                params.setValue(AVKey.LEVEL_ZERO_TILE_DELTA, ll);
        }

        Integer tileDimension = WWXML.getInteger(el, "ImageAccessor/TextureSizePixels", xpath);
        if (tileDimension != null)
        {
            if (params.getValue(AVKey.TILE_WIDTH) == null)
                params.setValue(AVKey.TILE_WIDTH, tileDimension);
            if (params.getValue(AVKey.TILE_HEIGHT) == null)
                params.setValue(AVKey.TILE_HEIGHT, tileDimension);
        }

        // LayerSet documents always describe an offline pyramid of tiled imagery in the file store. Therefore we can
        // safely assume that network retrieval should be disabled. Because we know nothing about the nature of the
        // imagery, it's best to enable mipmapping and transparent textures by default.
        if (params.getValue(AVKey.NETWORK_RETRIEVAL_ENABLED) == null)
            params.setValue(AVKey.NETWORK_RETRIEVAL_ENABLED, false);
        if (params.getValue(AVKey.USE_MIP_MAPS) == null)
            params.setValue(AVKey.USE_MIP_MAPS, true);
        if (params.getValue(AVKey.USE_TRANSPARENT_TEXTURES) == null)
            params.setValue(AVKey.USE_TRANSPARENT_TEXTURES, true);

        // Enable texture compression for any non-DDS tile format.
        if (params.getValue(AVKey.COMPRESS_TEXTURES) == null)
        {
            if (params.getValue(AVKey.FORMAT_SUFFIX) == null ||
                !params.getValue(AVKey.FORMAT_SUFFIX).toString().endsWith("dds"))
            {
                params.setValue(AVKey.COMPRESS_TEXTURES, true);
            }
        }

        return params;
    }

    /**
     * Creates a standard layer configuration from a World Wind .NET LayerSet document.
     *
     * @param domElement backing document.
     *
     * @return Layer document, or null if the LayerSet document cannot be transformed to a standard document.
     *
     * @throws IllegalArgumentException if the document is null.
     */
    public static DataConfiguration createDataConfig(Element domElement)
    {
        if (domElement == null)
        {
            String message = Logging.getMessage("nullValue.DocumentIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Document newDoc = transformLayerSetDocument(domElement);
        if (newDoc == null || newDoc.getDocumentElement() == null)
            return null;

        Element newDomElement = newDoc.getDocumentElement();
        return new LayerConfiguration(newDomElement);
    }

    /**
     * Transforms a World Wind .NET LayerSet document to a standard layer configuration document.
     *
     * @param domElement LayerSet document to transform.
     *
     * @return standard Layer document, or null if the LayerSet document cannot be transformed to a standard document.
     *
     * @throws IllegalArgumentException if the document is null.
     */
    public static Document transformLayerSetDocument(Element domElement)
    {
        if (domElement == null)
        {
            String message = Logging.getMessage("nullValue.DocumentIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        XPath xpath = WWXML.makeXPath();

        Element[] els = WWXML.getElements(domElement, "/LayerSet/QuadTileSet", xpath);
        if (els == null || els.length == 0)
            return null;

        // Ignore all but the first QuadTileSet element.
        Document outDoc = WWXML.createDocumentBuilder(true).newDocument();
        transformLayerSet(els[0], outDoc, xpath);

        return outDoc;
    }

    protected static void transformLayerSet(Element context, Document outDoc, XPath xpath)
    {
        Element el = WWXML.setDocumentElement(outDoc, "Layer");
        WWXML.setIntegerAttribute(el, "version", 1);
        WWXML.setTextAttribute(el, "layerType", "TiledImageLayer");

        transformQuadTileSet(context, el, xpath);
    }

    protected static void transformQuadTileSet(Element context, Element outElem, XPath xpath)
    {
        // Display name and dataset name properties.
        String s = WWXML.getText(context, "Name", xpath);
        if (s != null && s.length() != 0)
        {
            WWXML.appendText(outElem, "DisplayName", s);
            WWXML.appendText(outElem, "DatasetName", s);
        }

        // Display properties.
        Double d = WWXML.getDouble(context, "Opacity", xpath);
        if (d != null)
            WWXML.appendDouble(outElem, "Opacity", d / 255d);

        // Service properties.
        // LayerSet documents always describe an offline pyramid of tiled imagery in the file store, Therefore we define
        // the service as "Offline".
        Element el = WWXML.appendElementPath(outElem, "Service");
        WWXML.setTextAttribute(el, "serviceName", "Offline");

        // Image format properties.
        s = WWXML.getText(context, "ImageAccessor/ImageFileExtension", xpath);
        if (s != null && s.length() != 0)
        {
            if (!s.startsWith("."))
                s = "." + s;
            WWXML.appendText(outElem, "FormatSuffix", s);

            // LayerSet documents contain a format suffix, but not image format type. Convert the format suffix to a
            // mime type, then use it to populate the ImageFormat and AvailableImageFormat elements in the transformed
            // Layer configuration document.
            String mimeType = WWIO.makeMimeTypeForSuffix(s);
            if (mimeType != null && mimeType.length() != 0)
            {
                WWXML.appendText(outElem, "ImageFormat", mimeType);
                WWXML.appendText(outElem, "AvailableImageFormats/ImageFormat", mimeType);
            }
        }

        // Tile structure properties.
        Integer numLevels = WWXML.getInteger(context, "ImageAccessor/NumberLevels", xpath);
        if (numLevels != null)
        {
            el = WWXML.appendElementPath(outElem, "NumLevels");
            WWXML.setIntegerAttribute(el, "count", numLevels);
            WWXML.setIntegerAttribute(el, "numEmpty", 0);
        }

        Sector sector = getLayerSetSector(context, "BoundingBox", xpath);
        if (sector != null)
            WWXML.appendSector(outElem, "Sector", sector);

        WWXML.appendLatLon(outElem, "TileOrigin/LatLon", new LatLon(Angle.NEG90, Angle.NEG180));

        LatLon ll = getLayerSetLatLon(context, "ImageAccessor/LevelZeroTileSizeDegrees", xpath);
        if (ll != null)
            WWXML.appendLatLon(outElem, "LevelZeroTileDelta/LatLon", ll);

        Integer tileDimension = WWXML.getInteger(context, "ImageAccessor/TextureSizePixels", xpath);
        if (tileDimension != null)
        {
            el = WWXML.appendElementPath(outElem, "TileSize/Dimension");
            WWXML.setIntegerAttribute(el, "width", tileDimension);
            WWXML.setIntegerAttribute(el, "height", tileDimension);
        }

        // LayerSet documents always describe an offline pyramid of tiled imagery in the file store. Therefore we can
        // safely assume that network retrieval should be disabled. Because we know nothing about the nature of
        // the imagery, it's best to enable mipmapping and transparent textures by default.
        WWXML.appendBoolean(outElem, "NetworkRetrievalEnabled", false);
        WWXML.appendBoolean(outElem, "UseMipMaps", true);
        WWXML.appendBoolean(outElem, "UseTransparentTextures", true);

        // Enable texture compression for any non-DDS tile format.
        s = WWXML.getText(context, "ImageAccessor/ImageFileExtension", xpath);
        if (s == null || !s.endsWith("dds"))
            WWXML.appendBoolean(outElem, "CompressTextures", true);
    }

    protected static LatLon getLayerSetLatLon(Element context, String path, XPath xpath)
    {
        Double degrees = WWXML.getDouble(context, path, xpath);
        if (degrees == null)
            return null;

        return LatLon.fromDegrees(degrees, degrees);
    }

    protected static Sector getLayerSetSector(Element context, String path, XPath xpath)
    {
        Element el = (path == null) ? context : WWXML.getElement(context, path, xpath);
        if (el == null)
            return null;

        Double minLatDegrees = WWXML.getDouble(el, "South/Value", xpath);
        Double maxLatDegrees = WWXML.getDouble(el, "North/Value", xpath);
        Double minLonDegrees = WWXML.getDouble(el, "West/Value", xpath);
        Double maxLonDegrees = WWXML.getDouble(el, "East/Value", xpath);

        if (minLatDegrees == null || maxLatDegrees == null || minLonDegrees == null || maxLonDegrees == null)
            return null;

        return Sector.fromDegrees(minLatDegrees, maxLatDegrees, minLonDegrees, maxLonDegrees);
    }
}
