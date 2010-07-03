/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/

package gov.nasa.worldwind.formats.shapefile;

import com.sun.opengl.util.BufferUtil;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.exception.WWRuntimeException;
import gov.nasa.worldwind.util.*;

import java.awt.geom.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

/**
 * Represents an ESRI Shapefile - *.shp.
 * <p/>
 * See http://webhelp.esri.com/arcgisdesktop/9.3/index.cfm?id=2729&pid=2727&topicname=Shapefile_file_extensions
 *
 * @author Patrick Murris
 * @version $Id: Shapefile.java 13262 2010-04-09 22:39:22Z dcollins $
 */
public class Shapefile
{
    protected static final int FILE_CODE = 0x0000270A;
    protected static final int HEADER_LENGTH = 100;
    protected static final int RECORD_HEADER_LENGTH = 8;

    protected static final String INDEX_FILE_SUFFIX = ".shx";
    protected static final String ATTRIBUTE_FILE_SUFFIX = ".dbf";

    public static final String SHAPE_NULL = "gov.nasa.worldwind.formats.shapefile.Shapefile.ShapeNull";
    public static final String SHAPE_POINT = "gov.nasa.worldwind.formats.shapefile.Shapefile.ShapePoint";
    public static final String SHAPE_MULTI_POINT = "gov.nasa.worldwind.formats.shapefile.Shapefile.ShapeMultiPoint";
    public static final String SHAPE_POLYLINE = "gov.nasa.worldwind.formats.shapefile.Shapefile.ShapePolyline";
    public static final String SHAPE_POLYGON = "gov.nasa.worldwind.formats.shapefile.Shapefile.ShapePolygon";

    public static final String SHAPE_POINT_M = "gov.nasa.worldwind.formats.shapefile.Shapefile.ShapePointM";
    public static final String SHAPE_MULTI_POINT_M = "gov.nasa.worldwind.formats.shapefile.Shapefile.ShapeMultiPointM";
    public static final String SHAPE_POLYLINE_M = "gov.nasa.worldwind.formats.shapefile.Shapefile.ShapePolylineM";
    public static final String SHAPE_POLYGON_M = "gov.nasa.worldwind.formats.shapefile.Shapefile.ShapePolygonM";

    public static final String SHAPE_POINT_Z = "gov.nasa.worldwind.formats.shapefile.Shapefile.ShapePointZ";
    public static final String SHAPE_MULTI_POINT_Z = "gov.nasa.worldwind.formats.shapefile.Shapefile.ShapeMultiPointZ";
    public static final String SHAPE_POLYLINE_Z = "gov.nasa.worldwind.formats.shapefile.Shapefile.ShapePolylineZ";
    public static final String SHAPE_POLYGON_Z = "gov.nasa.worldwind.formats.shapefile.Shapefile.ShapePolygonZ";

    public static final String SHAPE_MULTI_PATCH = "gov.nasa.worldwind.formats.shapefile.Shapefile.ShapeMultiPatch";

    protected static List<String> measureTypes = new ArrayList<String>(Arrays.asList(
        Shapefile.SHAPE_POINT_M, Shapefile.SHAPE_POINT_Z,
        Shapefile.SHAPE_MULTI_POINT_M, Shapefile.SHAPE_MULTI_POINT_Z,
        Shapefile.SHAPE_POLYLINE_M, Shapefile.SHAPE_POLYLINE_Z,
        Shapefile.SHAPE_POLYGON_M, Shapefile.SHAPE_POLYGON_Z
    ));

    protected static List<String> zTypes = new ArrayList<String>(Arrays.asList(
        Shapefile.SHAPE_POINT_Z,
        Shapefile.SHAPE_MULTI_POINT_Z,
        Shapefile.SHAPE_POLYLINE_Z,
        Shapefile.SHAPE_POLYGON_Z
    ));

    protected class Header
    {
        public int fileCode = FILE_CODE;
        public int fileLength;
        public int version;
        public String shapeType;
        public Rectangle2D boundingRectangle;
    }

    private final File file;
    protected AVList projectionParams;
    protected final Header header;
    protected int[] index;
    protected CompoundVecBuffer buffer;
    protected ArrayList<ShapefileRecord> records;

    /**
     * Creates a Shapefile instance from the given file refering to the shapefile main geometry file with a .shp
     * extension. Records are not loaded right away. The file(s) content will be read during the first call to {@link
     * #getRecords()}. If the specified projection parameter list is non-null and describes a recognized map projection,
     * the Shapefile's coordinates are converted to geographic coordinates when they're parsed.
     *
     * @param file             the shapefile geometry file *.shp.
     * @param projectionParams parameter list describing the Shapefile's map projection, or null to assume geographic
     *                         coordinates.
     *
     * @throws WWRuntimeException if the shapefile cannot be instantiated for any reason.
     */
    public Shapefile(File file, AVList projectionParams)
    {
        if (file == null)
        {
            String message = Logging.getMessage("nullValue.FileIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.file = file;
        this.projectionParams = projectionParams;

        try
        {
            this.header = readHeaderFromFile(file);
            // Delay records loading until getRecords() is called
        }
        catch (Exception e)
        {
            String message = Logging.getMessage("SHP.ExceptionAttemptingToReadFile", file.getPath());
            Logging.logger().log(java.util.logging.Level.SEVERE, message, e);
            throw new WWRuntimeException(message, e);
        }
    }

    /**
     * Creates a Shapefile instance from the given file refering to the shapefile main geometry file with a .shp
     * extension. Records are not loaded right away. The file(s) content will be read during the first call to {@link
     * #getRecords()}.
     *
     * @param file the shapefile geometry file *.shp.
     *
     * @throws WWRuntimeException if the shapefile cannot be instantiated for any reason.
     */
    public Shapefile(File file)
    {
        this(file, null);
    }

    /**
     * Creates a Shapefile instance from the given input streams. Records are loaded right away. If the specified
     * projection parameter list is non-null and describes a recognized map projection, the Shapefile's coordinates are
     * converted to geographic coordinates when they're parsed.
     *
     * @param shpStream        the shapefile geometry file stream - *.shp.
     * @param shxStream        the index file stream - .shx, can be null.
     * @param dbfStream        the attribute file stream - *.dbf, can be null.
     * @param projectionParams parameter list describing the Shapefile's map projection, or null to assume geographic
     *                         coordinates.
     *
     * @throws WWRuntimeException if the shapefile cannot be instantiated for any reason.
     */
    public Shapefile(InputStream shpStream, InputStream shxStream, InputStream dbfStream,
        AVList projectionParams)
    {
        if (shpStream == null)
        {
            String message = Logging.getMessage("nullValue.InputStreamIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.file = null;
        this.projectionParams = projectionParams;

        try
        {
            this.header = readHeaderFromStream(shpStream);
            this.records = readRecordsFromStream(shpStream, shxStream, dbfStream);
        }
        catch (Exception e)
        {
            String message = Logging.getMessage("generic.ExceptionAttemptingToReadFrom", shpStream.toString());
            Logging.logger().log(java.util.logging.Level.SEVERE, message, e);
            throw new WWRuntimeException(message, e);
        }
    }

    /**
     * Creates a Shapefile instance from the given input streams. Records are loaded right away.
     *
     * @param shpStream the shapefile geometry file stream - *.shp.
     * @param shxStream the index file stream - .shx, can be null.
     * @param dbfStream the attribute file stream - *.dbf, can be null.
     *
     * @throws WWRuntimeException if the shapefile cannot be instantiated for any reason.
     */
    public Shapefile(InputStream shpStream, InputStream shxStream, InputStream dbfStream)
    {
        this(shpStream, shxStream, dbfStream, null);
    }

    /**
     * Returns the source {@link File}, if any, of this shapefile.
     *
     * @return the shapefile's source file, or null if there is no source file, e.g., if the shapefile was read from a
     *         stream.
     */
    public File getFile()
    {
        return this.file;
    }

    /**
     * Returns a parameter list describing this Shapefile's map projection, or null if this shapefile assumes that all
     * coordinates are geographic.
     *
     * @return parameter list describing this Shapefile's map projection. Can be null.
     */
    public AVList getProjectionParams()
    {
        return this.projectionParams;
    }

    /**
     * Returns the shapefile's version field.
     *
     * @return the shapefile's version field.
     */
    public int getVersion()
    {
        return this.header.version;
    }

    /**
     * Returns the raw shapefile's length, in bytes.
     *
     * @return the raw shapefile's length in bytes.
     */
    public int getLength()
    {
        return this.header.fileLength;
    }

    /**
     * Returns the shapefile's shape type.
     *
     * @return the shapefile's shape type, one of the <code>Shapefile.SHAPE*</code> values defined by this class or by
     *         subclasses.
     */
    public String getShapeType()
    {
        return this.header.shapeType;
    }

    /**
     * Returns the bounding rectangle calculated from the X-Y ranges in the shapefile.
     *
     * @return the shapefile's bounding rectangle.
     */
    public Rectangle2D getBoundingRectangle()
    {
        return this.header.boundingRectangle;
    }

    /**
     * Returns a {@link List} containing the shapefile records. If the Shapefile was instantiated from a File, the
     * shapefile content - it's records and attributes, are lazily read during the first call to this method.
     *
     * @return a List containing all the shapefile records.
     */
    public List<ShapefileRecord> getRecords()
    {
        if (this.records == null && this.getFile() != null)
        {
            File file = this.getFile();
            try
            {
                this.records = this.readRecordsFromFile(file);
            }
            catch (Exception e)
            {
                String message = Logging.getMessage("SHP.ExceptionAttemptingToReadFile", file.getPath());
                Logging.logger().log(java.util.logging.Level.SEVERE, message, e);
                throw new WWRuntimeException(message, e);
            }
        }

        return this.records;
    }

    /**
     * Get the underlying {@link CompoundVecBuffer} describing the shapefiles X-Y points.
     *
     * @return the underlying {@link CompoundVecBuffer}.
     */
    public CompoundVecBuffer getBuffer()
    {
        return this.buffer;
    }

    /**
     * Returns <code>true</code> if this shapefile has an associated index file - .shx.
     *
     * @return Returns <code>true</code> if this shapefile has an associated index file - .shx.
     */
    protected boolean hasIndexFile()
    {
        File idxFile = this.getIndexFile();
        return idxFile != null && idxFile.exists();
    }

    /**
     * Returns <code>true</code> if this shapefile has an associated attribute file - .dbf.
     *
     * @return <code>true</code> if this shapefile has an associated attribute file - .dbf.
     */
    protected boolean hasAttributeFile()
    {
        File attrFile = this.getAttributeFile();
        return attrFile != null && attrFile.exists();
    }

    /**
     * Returns the number of records in the shapefile.
     *
     * @return the number of records in the shapefile.
     */
    protected int getNumberOfRecords()
    {
        int[] index = this.getIndex();
        return index != null ? index.length / 2 : -1;
    }

    /**
     * Reads and returns the next index in the index file.
     *
     * @return the next index in the index file.
     *
     * @throws WWRuntimeException if there are no more indices in the index file or an error occurs while reading the
     *                            file.
     */
    protected int[] getIndex()
    {
        if (this.index == null && this.hasIndexFile())
        {
            File indexFile = this.getIndexFile();
            try
            {
                this.index = this.readIndexFromFile(indexFile);
            }
            catch (Exception e)
            {
                String message = Logging.getMessage("SHP.ExceptionAttemptingToReadFile", indexFile.getPath());
                Logging.logger().log(java.util.logging.Level.SEVERE, message, e);
                throw new WWRuntimeException(message, e);
            }
        }

        return this.index;
    }

    /**
     * Returns the index {@link File}, if any, for this shapefile.
     *
     * @return the shapefile's index file, or null if there is no index file, e.g., if the index file was read from a
     *         stream.
     */
    protected File getIndexFile()
    {
        if (this.getFile() == null)
            return null;

        String filePath = WWIO.replaceSuffix(this.file.getAbsolutePath(), INDEX_FILE_SUFFIX);
        return new File(filePath);
    }

    /**
     * Returns the attribute {@link File}, if any, for this shapefile.
     *
     * @return the shapefile's attribute file, or null if there is no attribute file, e.g., if the attribute file was
     *         read from a stream.
     */
    protected File getAttributeFile()
    {
        if (this.getFile() == null)
            return null;

        String filePath = WWIO.replaceSuffix(this.file.getAbsolutePath(), ATTRIBUTE_FILE_SUFFIX);
        return new File(filePath);
    }

    //=== Shapefile header ===

    protected Header readHeaderFromFile(File file) throws IOException
    {
        InputStream is = null;
        Header header = null;
        try
        {
            is = new BufferedInputStream(new FileInputStream(file));
            header = this.readHeaderFromStream(is);
        }
        finally
        {
            if (is != null)
                is.close();
        }

        return header;
    }

    protected Header readHeaderFromStream(InputStream stream) throws IOException
    {
        ReadableByteChannel channel = Channels.newChannel(stream);
        ByteBuffer headerBuffer = ShapefileUtils.readByteChannelToBuffer(channel, HEADER_LENGTH);
        return this.readHeaderFromBuffer(headerBuffer);
    }

    protected Header readHeaderFromBuffer(ByteBuffer buffer) throws WWRuntimeException
    {
        // Read file code - first 4 bytes, big endian
        buffer.order(ByteOrder.BIG_ENDIAN);
        int fileCode = buffer.getInt();
        if (fileCode != FILE_CODE)
        {
            String message = Logging.getMessage("SHP.NotAShapeFile", file.getPath());
            Logging.logger().log(java.util.logging.Level.SEVERE, message);
            throw new WWRuntimeException(message);
        }

        // Skip 5 unused ints
        buffer.position(buffer.position() + 5 * 4);

        // File length
        int lengthInWords = buffer.getInt();

        // Switch to little endian for the remaining part
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        // Read remaining header data
        int version = buffer.getInt();
        int type = buffer.getInt();
        Rectangle2D rect = ShapefileUtils.readBounds(buffer, this.getProjectionParams());

        // Check whether the shape type is supported
        String shapeType = getShapeType(type);
        if (shapeType == null)
        {
            String message = Logging.getMessage("SHP.UnsupportedShapeType", type);
            Logging.logger().log(java.util.logging.Level.SEVERE, message);
            throw new WWRuntimeException(message);
        }

        // Assemble header
        Header header = new Header();
        header.fileLength = lengthInWords * 2; // one word = 2 bytes
        header.version = version;
        header.shapeType = shapeType;
        header.boundingRectangle = rect;

        return header;
    }

    // === Shapefile index ===

    protected int[] readIndexFromFile(File file) throws IOException
    {
        InputStream is = null;
        int[] index = null;
        try
        {
            is = new BufferedInputStream(new FileInputStream(file));
            index = this.readIndexFromStream(is);
        }
        finally
        {
            if (is != null)
                is.close();
        }

        return index;
    }

    protected int[] readIndexFromStream(InputStream stream) throws IOException
    {
        ByteBuffer indexBuffer = WWIO.readStreamToBuffer(stream);
        return this.readIndexFromBuffer(indexBuffer);
    }

    protected int[] readIndexFromBuffer(ByteBuffer buffer)
    {
        Header indexHeader = this.readHeaderFromBuffer(buffer);
        int numRecords = (indexHeader.fileLength - HEADER_LENGTH) / 8;
        int[] index = new int[numRecords * 2];

        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.position(HEADER_LENGTH); // Skip file header
        int idx = 0;
        for (int i = 0; i < numRecords; i++)
        {
            index[idx++] = buffer.getInt() * 2;  // record offset in bytes
            index[idx++] = buffer.getInt() * 2;  // record length in bytes
        }

        return index;
    }

    // === Shapefile records ===

    protected ArrayList<ShapefileRecord> readRecordsFromFile(File file) throws IOException
    {
        InputStream shpStream = null;
        InputStream shxStream = null;
        InputStream dbfStream = null;
        ArrayList<ShapefileRecord> records = null;
        try
        {
            shpStream = new BufferedInputStream(new FileInputStream(file));
            WWIO.skipBytes(shpStream, HEADER_LENGTH);
            if (this.hasIndexFile())
                shxStream = new BufferedInputStream(new FileInputStream(this.getIndexFile()));
            if (this.hasAttributeFile())
                dbfStream = new BufferedInputStream(new FileInputStream(this.getAttributeFile()));

            records = this.readRecordsFromStream(shpStream, shxStream, dbfStream);
        }
        finally
        {
            if (shpStream != null)
                shpStream.close();
            if (shxStream != null)
                shxStream.close();
            if (dbfStream != null)
                dbfStream.close();
        }

        return records;
    }

    protected ArrayList<ShapefileRecord> readRecordsFromStream(InputStream is, InputStream shxStream,
        InputStream dbfStream) throws IOException
    {
        ArrayList<ShapefileRecord> recordList = new ArrayList<ShapefileRecord>();
        DBaseFile attrFile = dbfStream != null ? new DBaseFile(dbfStream) : null;
        this.index = shxStream != null ? this.readIndexFromStream(shxStream) : null;
        ByteBuffer headerBuffer = ByteBuffer.allocate(RECORD_HEADER_LENGTH);
        ByteBuffer recordBuffer = null;

        // Allocate point buffer
        int numPoints = this.computeNumberOfPointsEstimate();
        VecBuffer pointBuffer = new VecBuffer(2, numPoints, new BufferFactory.DoubleBufferFactory());
        List<Integer> partsOffset = new ArrayList<Integer>();
        List<Integer> partsLength = new ArrayList<Integer>();

        // Get channel
        ReadableByteChannel channel = Channels.newChannel(is);

        // Read all records
        int bytesRead = 0;
        while (bytesRead < this.header.fileLength - HEADER_LENGTH)
        {
            // Read record header and get data length
            headerBuffer.clear();
            headerBuffer = ShapefileUtils.readByteChannelToBuffer(channel, RECORD_HEADER_LENGTH, headerBuffer);
            headerBuffer.order(ByteOrder.BIG_ENDIAN);
            int recLength = headerBuffer.getInt(4) * 2; // skip record number

            // Read record data
            int numBytes = RECORD_HEADER_LENGTH + recLength; // record header + data
            if (recordBuffer == null || recordBuffer.capacity() < numBytes)
                recordBuffer = ByteBuffer.allocate(numBytes);
            recordBuffer.limit(numBytes).rewind();
            recordBuffer.put(headerBuffer);
            recordBuffer = ShapefileUtils.readByteChannelToBuffer(channel, recLength, recordBuffer);

            // Create record
            ShapefileRecord record = this.readShapefileRecord(this, recordBuffer, pointBuffer, partsOffset,
                partsLength);

            // Set record attributes from dbf file
            record = this.setRecordAttributes(record, attrFile);

            // Add record to list if accepted
            if (this.acceptRecord(record))
                recordList.add(record);

            bytesRead += numBytes;
        }

        // Create CompoundVecBuffer
        int numParts = partsOffset.size();
        IntBuffer offsetBuffer = BufferUtil.newIntBuffer(numParts);
        IntBuffer lengthBuffer = BufferUtil.newIntBuffer(numParts);
        for (int i = 0; i < partsOffset.size(); i++)
        {
            offsetBuffer.put(partsOffset.get(i));
            lengthBuffer.put(partsLength.get(i));
        }
        offsetBuffer.rewind();
        lengthBuffer.rewind();

        // Convert the points to geographic coordinates if the projection parameters are non-null and describe a
        // recognized map projection.
        if (this.getProjectionParams() != null)
            WWBufferUtil.convertProjectedTuplesToGeographic(pointBuffer, this.getProjectionParams());

        this.buffer = new CompoundVecBuffer(pointBuffer, offsetBuffer, lengthBuffer, numParts,
            new BufferFactory.DoubleBufferFactory());

        if (recordList.size() == 0)
            return null;

        // Normalize geographic coordinates to the standard range.
        if (this.header.boundingRectangle.getX() < -180 || this.header.boundingRectangle.getMaxX() > 180)
            this.normalizeLocations(recordList);

        return recordList;
    }

    /**
     * Returns <code>true</code> if the given record is to be included in the record list. This method should be
     * overrided by subclasses that wish to enable record selection at load time rather than after all records have been
     * read.
     *
     * @param record the {@link ShapefileRecord} to accept.
     *
     * @return true if the record should be included in the record list.
     */
    @SuppressWarnings({"UnusedDeclaration"})
    protected boolean acceptRecord(ShapefileRecord record)
    {
        return true;
    }

    protected void normalizeLocations(List<ShapefileRecord> recordList)
    {
        for (ShapefileRecord record : recordList)
        {
            record.normalizeLocations();
        }
        ShapefileUtils.normalizeRectangle(this.header.boundingRectangle);
    }

    // === Record attributes ===

    protected ShapefileRecord setRecordAttributes(ShapefileRecord record, DBaseFile attrFile)
    {
        if (attrFile == null || attrFile.getRecords() == null)
            return record;

        if (record.getRecordNumber() > attrFile.getRecords().size())
        {
            String message = Logging.getMessage("generic.indexOutOfRange", record.getRecordNumber());
            Logging.logger().log(java.util.logging.Level.SEVERE, message);
            throw new WWRuntimeException(message);
        }

        record.attributes = attrFile.getRecords().get(record.getRecordNumber() - 1);
        return record;
    }

    /**
     * Maps the integer shape type from the shapefile to the corresponding shape type defined above.
     *
     * @param type the integer shape type.
     *
     * @return the mapped shape type.
     */
    protected String getShapeType(int type)
    {
        // Cases commented out indicate shape types not implemented
        switch (type)
        {
            case 0:
                return SHAPE_NULL;
            case 1:
                return SHAPE_POINT;
            case 3:
                return SHAPE_POLYLINE;
            case 5:
                return SHAPE_POLYGON;
            case 8:
                return SHAPE_MULTI_POINT;

            case 11:
                return SHAPE_POINT_Z;
            case 13:
                return SHAPE_POLYLINE_Z;
            case 15:
                return SHAPE_POLYGON_Z;
            case 18:
                return SHAPE_MULTI_POINT_Z;

            case 21:
                return SHAPE_POINT_M;
            case 23:
                return SHAPE_POLYLINE_M;
            case 25:
                return SHAPE_POLYGON_M;
            case 28:
                return SHAPE_MULTI_POINT_M;

//            case 31:
//                return SHAPE_MULTI_PATCH;

            default:
                return null; // unsupported shape type
        }
    }

    /**
     * Indicates whether a specified shape type may contain optional measure values.
     *
     * @param shapeType the shape type to analyze.
     *
     * @return true if the shape type is one that may contain measure values.
     *
     * @throws IllegalArgumentException if <code>shapeType</code> is null.
     */
    public static boolean isMeasureType(String shapeType)
    {
        if (shapeType == null)
        {
            String message = Logging.getMessage("nullValue.ShapeType");
            Logging.logger().log(java.util.logging.Level.SEVERE, message);
            throw new IllegalArgumentException(message);
        }

        return measureTypes.contains(shapeType);
    }

    /**
     * Indicates whether a specified shape type contains Z values.
     *
     * @param shapeType the shape type to analyze.
     *
     * @return true if the shape type is one that contains Z values.
     *
     * @throws IllegalArgumentException if <code>shapeType</code> is null.
     */
    public static boolean isZType(String shapeType)
    {
        if (shapeType == null)
        {
            String message = Logging.getMessage("nullValue.ShapeType");
            Logging.logger().log(java.util.logging.Level.SEVERE, message);
            throw new IllegalArgumentException(message);
        }

        return zTypes.contains(shapeType);
    }

    /**
     * Indicates whether a specified shape type is either {@link #SHAPE_POINT}, {@link #SHAPE_POINT_M} or {@link
     * #SHAPE_POINT_Z}.
     *
     * @param shapeType the shape type to analyze.
     *
     * @return true if the shape type is a point type.
     *
     * @throws IllegalArgumentException if <code>shapeType</code> is null.
     */
    public static boolean isPointType(String shapeType)
    {
        if (shapeType == null)
        {
            String message = Logging.getMessage("nullValue.ShapeType");
            Logging.logger().log(java.util.logging.Level.SEVERE, message);
            throw new IllegalArgumentException(message);
        }

        return shapeType.equals(Shapefile.SHAPE_POINT) || shapeType.equals(Shapefile.SHAPE_POINT_Z)
            || shapeType.equals(Shapefile.SHAPE_POINT_M);
    }

    /**
     * Indicates whether a specified shape type is either {@link #SHAPE_MULTI_POINT}, {@link #SHAPE_MULTI_POINT_M} or
     * {@link #SHAPE_MULTI_POINT_Z}.
     *
     * @param shapeType the shape type to analyze.
     *
     * @return true if the shape type is a mulit-point type.
     *
     * @throws IllegalArgumentException if <code>shapeType</code> is null.
     */
    public static boolean isMultiPointType(String shapeType)
    {
        if (shapeType == null)
        {
            String message = Logging.getMessage("nullValue.ShapeType");
            Logging.logger().log(java.util.logging.Level.SEVERE, message);
            throw new IllegalArgumentException(message);
        }

        return shapeType.equals(Shapefile.SHAPE_MULTI_POINT) || shapeType.equals(Shapefile.SHAPE_MULTI_POINT_Z)
            || shapeType.equals(Shapefile.SHAPE_MULTI_POINT_M);
    }

    /**
     * Indicates whether a specified shape type is either {@link #SHAPE_POLYLINE}, {@link #SHAPE_POLYLINE_M} or {@link
     * #SHAPE_POLYLINE_Z}.
     *
     * @param shapeType the shape type to analyze.
     *
     * @return true if the shape type is a polyline type.
     *
     * @throws IllegalArgumentException if <code>shapeType</code> is null.
     */
    public static boolean isPolylineType(String shapeType)
    {
        if (shapeType == null)
        {
            String message = Logging.getMessage("nullValue.ShapeType");
            Logging.logger().log(java.util.logging.Level.SEVERE, message);
            throw new IllegalArgumentException(message);
        }

        return shapeType.equals(Shapefile.SHAPE_POLYLINE) || shapeType.equals(Shapefile.SHAPE_POLYLINE_Z)
            || shapeType.equals(Shapefile.SHAPE_POLYLINE_M);
    }

    /**
     * Indicates whether a specified shape type is either {@link #SHAPE_POLYGON}, {@link #SHAPE_POLYGON_M} or {@link
     * #SHAPE_POLYGON_Z}.
     *
     * @param shapeType the shape type to analyze.
     *
     * @return true if the shape type is a polygon type.
     */
    public static boolean isPolygonType(String shapeType)
    {
        if (shapeType == null)
        {
            String message = Logging.getMessage("nullValue.ShapeType");
            Logging.logger().log(java.util.logging.Level.SEVERE, message);
            throw new IllegalArgumentException(message);
        }

        return shapeType.equals(Shapefile.SHAPE_POLYGON) || shapeType.equals(Shapefile.SHAPE_POLYGON_Z)
            || shapeType.equals(Shapefile.SHAPE_POLYGON_M);
    }

    /**
     * Estimater the number of points in a shapefile.
     *
     * @return a liberal estimate of the number of points in the shapefile.
     */
    @SuppressWarnings({"StringEquality"})
    protected int computeNumberOfPointsEstimate()
    {
        // Compute the header overhead, subtract it from the file size, then divide by point size to get the estimate.
        // The Parts array is not included in the overhead, so the estimate will be slightly greater than the number of
        // points needed if the shape is a type with a Parts array. Measure values and ranges are also not included in
        // the estimate because they are optional, so if they are included the estimate will also be greater than
        // necessary.

        final int numRecords = this.getNumberOfRecords();

        // Return very liberal estimate based on file size if num records unknown.
        if (numRecords < 0)
            return (this.getLength() - HEADER_LENGTH) / 16; // num X, Y tuples that can fit in the file length

        int overhead = HEADER_LENGTH + numRecords * 12; //12 bytes per record for record header and record shape type

        String shapeType = this.getShapeType();

        if (shapeType == SHAPE_POINT || shapeType == SHAPE_POINT_M)
            return (this.getLength() - overhead) / 16; // 16 = two doubles, X and Y

        if (shapeType == SHAPE_MULTI_POINT || shapeType == SHAPE_MULTI_POINT_M)
            // Add 32 bytes per record for bounding box + 4 bytes for one int per record
            return (this.getLength() - (overhead + numRecords * (32 + 4))) / 16; // 16 = two doubles, X and Y

        if (shapeType == SHAPE_POLYLINE || shapeType == SHAPE_POLYGON
            || shapeType == SHAPE_POLYLINE_M || shapeType == SHAPE_POLYGON_M)
            // Add 32 bytes per record for bounding box + 8 bytes for two ints per record
            return (this.getLength() - (overhead + numRecords * (32 + 8))) / 16; // 16 = two doubles, X and Y

        if (shapeType == SHAPE_POINT_Z)
            return (this.getLength() - overhead) / 24; // 24 = three doubles, X, Y, Z

        if (shapeType == SHAPE_MULTI_POINT_Z)
            // Add 48 bytes per record for bounding box + 4 bytes for one int per record
            return (this.getLength() - (overhead + numRecords * (48 + 4))) / 24; // 24 = three doubles, X, Y, Z

        if (shapeType == SHAPE_POLYLINE_Z || shapeType == SHAPE_POLYGON_Z)
            // Add 48 bytes per record for bounding box + 8 bytes for two ints per record
            return (this.getLength() - (overhead + numRecords * (48 + 8))) / 24; // 24 = three doubles, X, Y and Z

        // The shape type should have been checked before calling this method, so we shouldn't reach this code:
        String message = Logging.getMessage("SHP.UnsupportedShapeType", shapeType);
        Logging.logger().log(java.util.logging.Level.SEVERE, message);
        throw new WWRuntimeException(message);
    }

    /**
     * Creates a new {@link ShapefileRecord} instance from the given {@link java.nio.ByteBuffer}. Returns
     * <code>null</code> if the record shape type is {@link Shapefile#SHAPE_NULL}.
     * <p/>
     * The buffer current position is assumed to be set at the start of the record and will be set to the start of the
     * next record after this method has completed.
     *
     * @param shapeFile    the parent {@link Shapefile}.
     * @param recordBuffer the shapefile record {@link java.nio.ByteBuffer} to read from.
     * @param pointBuffer  the {@link VecBuffer} into which points are to be added.
     * @param partsOffset  the current list of parts offset for preceding records.
     * @param partsLength  the current list of parts length for preceding records.
     *
     * @return a new {@link ShapefileRecord} instance.
     */
    protected ShapefileRecord readShapefileRecord(Shapefile shapeFile, ByteBuffer recordBuffer, VecBuffer pointBuffer,
        List<Integer> partsOffset, List<Integer> partsLength)
    {
        String shapeType = this.readRecordShapeType(recordBuffer);

        // Select proper record class
        if (isPointType(shapeType))
        {
            return new ShapefileRecordPoint(shapeFile, recordBuffer, pointBuffer, partsOffset, partsLength);
        }
        else if (isMultiPointType(shapeType))
        {
            return new ShapefileRecordMultiPoint(shapeFile, recordBuffer, pointBuffer, partsOffset, partsLength);
        }
        else if (isPolylineType(shapeType))
        {
            return new ShapefileRecordPolyline(shapeFile, recordBuffer, pointBuffer, partsOffset, partsLength);
        }
        else if (isPolygonType(shapeType))
        {
            return new ShapefileRecordPolygon(shapeFile, recordBuffer, pointBuffer, partsOffset, partsLength);
        }

        return null;
    }

    /**
     * Read and return a record's shape type from a record buffer.
     *
     * @param recordBuffer the record buffer to read from.
     *
     * @return the record's shape type.
     */
    protected String readRecordShapeType(ByteBuffer recordBuffer)
    {
        // Read shape type - little endian
        recordBuffer.order(ByteOrder.LITTLE_ENDIAN);
        int type = recordBuffer.getInt(recordBuffer.position() + 2 * 4); // skip record number and length as ints

        String shapeType = this.getShapeType(type);
        if (shapeType == null)
        {
            String message = Logging.getMessage("SHP.UnsupportedShapeType", type);
            Logging.logger().log(java.util.logging.Level.SEVERE, message);
            throw new WWRuntimeException(message);
        }

        return shapeType;
    }

    // Tests
//    public static void main(String[] args)
//    {
//        File file = new File("J:\\Data\\Shapefiles\\World\\TM_WORLD_BORDERS_SIMPL-0.2.shp");
//        try
//        {
//            Shapefile shp = new Shapefile(file);
//            //Shapefile shp = new Shapefile(new BufferedInputStream(new FileInputStream(file)), null, null);
//
//            // Dump shapefile properties
//            System.out.println("Shapefile : " + (shp.getFile() != null ? shp.getFile().getName() : "null"));
//            System.out.println("has index : " + shp.hasIndexFile());
//            System.out.println("has attributes : " + shp.hasAttributeFile());
//            System.out.println("length : " + shp.getLength());
//            System.out.println("type : " + shp.getShapeType());
//            System.out.println("version : " + shp.getVersion());
//            System.out.println("sector : " + shp.getBoundingRectangle());
//
//            // Read index
//            int[] index = shp.getIndex();
//            if (index != null)
//            {
//                System.out.println("Index: " + index.length / 2 + " entries");
//                for (int i = 0; i < index.length / 2 && i < 10; i++)
//                    System.out.println("Rec " + (i + 1) + ", offset: " + index[i * 2] + ", length: " + index[i * 2 + 1]);
//            }
//
//            // Read records
//            List<ShapefileRecord> records = shp.getRecords();
//            System.out.println("records : " + records.size());
//
//            int totShapes = 0;
//            for (int i = 0; i < records.size() && i < 50; i++)
//            {
//                ShapefileRecord rec = records.get(i);
//
//                // Dump record properties
//                if (rec instanceof ShapefileRecordPoint)
//                {
//                    ShapefileRecordPoint point = (ShapefileRecordPoint)rec;
//                    System.out.println("record : " + point.getRecordNumber()
//                        + ", coord: " + LatLon.fromDegrees(point.getPoint()[1], point.getPoint()[0]));
//                    totShapes++;
//                }
//                if (rec instanceof ShapefileRecordPolyline)
//                {
//                    ShapefileRecordPolyline polyline = (ShapefileRecordPolyline)rec;
//                    System.out.println("record : " + polyline.getRecordNumber()
//                        + ", parts: " + polyline.getNumberOfParts()
//                        + ", points: " + polyline.getNumberOfPoints()
//                        + ", rect: " + polyline.getBoundingRectangle());
//                    totShapes += polyline.getNumberOfParts();
//                }
//
//                // Dump record attributes
//                if (rec.getAttributes() != null)
//                {
//                    System.out.print("Attributes : ");
//                    for (Map.Entry entry : rec.getAttributes().getEntries())
//                        System.out.print(entry.getKey() + " = " + entry.getValue() + ", ");
//                    System.out.println("");
//                }
//            }
//
//            System.out.println("Tot shapes : " + totShapes);
//        }
//        catch (Exception e)
//        {
//            e.printStackTrace();
//        }
//
//    }
}