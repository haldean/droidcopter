/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.formats.shapefile;

import com.sun.opengl.util.BufferUtil;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.exception.WWRuntimeException;
import gov.nasa.worldwind.formats.worldfile.WorldFile;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.util.*;

import java.awt.geom.*;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.ReadableByteChannel;
import java.util.*;

/**
 * Utilities for working with shapefiles.
 *
 * @author Patrick Murris
 * @version $Id: ShapefileUtils.java 13308 2010-04-13 07:23:04Z dcollins $
 */
public class ShapefileUtils
{
    /**
     * Opens an shapefile given the file's location in the file system or on the classpath. There must be a shape index
     * file and a shape attribute file in the same folder as the shapefile, with the same filename, and with suffixes
     * ".shx" and ".dbf, respectively. If a projection file exists in the same folder as the shapefile, with the same
     * filename, and with the suffix ".prj", this attempts to parse it as a well-known text description of the
     * shapefile's coordinate system and projection information. If the projection file does not exist, or cannot be
     * read for any reason, this opens the shapefile without the projection information.
     *
     * @param filePath the path to the file. Must be an absolute path or a path relative to a location in the
     *                 classpath.
     * @param c        the class that is used to find a path relative to the classpath.
     *
     * @return a {@link gov.nasa.worldwind.formats.shapefile.Shapefile} for the file, or null if the specified file
     *         cannot be found.
     *
     * @throws IllegalArgumentException if the file path is null.
     * @throws WWRuntimeException       if an exception or error occurs while opening and parsing the file. The causing
     *                                  exception is included in this exception's {@link Throwable#initCause(Throwable)}
     *                                  .
     */
    public static Shapefile openShapefileFile(String filePath, Class c)
    {
        if (filePath == null)
        {
            String message = Logging.getMessage("nullValue.FileIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        String shxFilePath = WWIO.replaceSuffix(filePath, ".shx");
        String dbfFilePath = WWIO.replaceSuffix(filePath, ".dbf");
        String prjFilePath = WWIO.replaceSuffix(filePath, ".prj");

        AVList projectionParams = null;
        InputStream prjStream = null;
        try
        {
            prjStream = WWIO.openFileOrResourceStream(prjFilePath, c);
            if (prjStream != null)
            {
                ByteBuffer buffer = WWIO.readStreamToBuffer(prjStream);
                File tmp = WWIO.saveBufferToTempFile(buffer, ".prj");
                projectionParams = WorldFile.decodeWorldFiles(new File[] {tmp}, null);
            }
        }
        catch (Exception e)
        {
            Logging.logger().warning(Logging.getMessage("WorldFile.ExceptionReading", filePath));
        }
        finally
        {
            WWIO.closeStream(prjStream, prjFilePath);
        }

        InputStream shpStream = null, shxStream = null, dbfStream = null;
        try
        {
            shpStream = WWIO.openFileOrResourceStream(filePath, c);
            shxStream = WWIO.openFileOrResourceStream(shxFilePath, c);
            dbfStream = WWIO.openFileOrResourceStream(dbfFilePath, c);

            return new Shapefile(shpStream, shxStream, dbfStream, projectionParams);
        }
        catch (Exception e)
        {
            throw new WWRuntimeException(
                Logging.getMessage("generic.ExceptionAttemptingToReadShapefile", filePath), e);
        }
        finally
        {
            WWIO.closeStream(shpStream, filePath);
            WWIO.closeStream(shxStream, shxFilePath);
            WWIO.closeStream(dbfStream, dbfFilePath);
        }
    }

    /**
     * Opens a shapefile given a generic {@link java.net.URL} reference. There must be a shape index file and a shape
     * attribute file in the same URL path as the shapefile, with the same filename, and with suffixes ".shx" and ".dbf,
     * respectively. If a projection file exists in the same URL path as the shapefile, with the same filename, and with
     * the suffix ".prj", this attempts to parse it as a well-known text description of the shapefile's coordinate
     * system and projection information. If the projection file does not exist, or cannot be read for any reason, this
     * opens the shapefile without the projection information.
     *
     * @param url the URL to the shapefile.
     *
     * @return a {@link gov.nasa.worldwind.formats.shapefile.Shapefile} for the URL.
     *
     * @throws IllegalArgumentException if the url is null.
     * @throws WWRuntimeException       if an exception or error occurs while opening and parsing the url. The causing
     *                                  exception is included in this exception's {@link Throwable#initCause(Throwable)}
     *                                  .
     */
    public static Shapefile openShapefileURL(URL url)
    {
        if (url == null)
        {
            String message = Logging.getMessage("nullValue.URLIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        URL shx, dbf, prj;
        try
        {
            shx = new URL(WWIO.replaceSuffix(url.toString(), ".shx"));
            dbf = new URL(WWIO.replaceSuffix(url.toString(), ".dbf"));
            prj = new URL(WWIO.replaceSuffix(url.toString(), ".prj"));
        }
        catch (MalformedURLException e)
        {
            throw new WWRuntimeException(
                Logging.getMessage("generic.ExceptionAttemptingToReadShapefile", url.toString()), e);
        }

        AVList projectionParams = null;
        try
        {
            ByteBuffer buffer = WWIO.readURLContentToBuffer(prj);
            File tmp = WWIO.saveBufferToTempFile(buffer, ".prj");
            projectionParams = WorldFile.decodeWorldFiles(new File[] {tmp}, null);
        }
        catch (Exception e)
        {
            Logging.logger().warning(Logging.getMessage("WorldFile.ExceptionReading", url.toString()));
        }

        InputStream shpStream = null, shxStream = null, dbfStream = null;
        try
        {
            shpStream = url.openStream();
            shxStream = shx.openStream();
            dbfStream = dbf.openStream();

            return new Shapefile(shpStream, shxStream, dbfStream, projectionParams);
        }
        catch (Exception e)
        {
            throw new WWRuntimeException(
                Logging.getMessage("generic.ExceptionAttemptingToReadShapefile", url.toString()), e);
        }
        finally
        {
            WWIO.closeStream(shpStream, url.toString());
            WWIO.closeStream(shxStream, shx.toString());
            WWIO.closeStream(dbfStream, dbf.toString());
        }
    }

    /**
     * Open an shapefile from a general source. The source type may be one of the following: <ul> <li>{@link URL}</li>
     * <li>{@link File}</li> <li>{@link String} containing a valid URL description or a file or resource name available
     * on the classpath.</li> </ul>
     *
     * @param source the source of the shapefile.
     *
     * @return the shapefile as a {@link gov.nasa.worldwind.formats.shapefile.Shapefile}, or null if the source object
     *         is a string that does not identify a URL, a file or a resource available on the classpath.
     */
    public static Shapefile openShapefile(Object source)
    {
        if (source == null || WWUtil.isEmpty(source))
        {
            String message = Logging.getMessage("nullValue.SourceIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (source instanceof URL)
        {
            return openShapefileURL((URL) source);
        }
        else if (source instanceof File)
        {
            return openShapefileFile(((File) source).getPath(), null);
        }
        else if (!(source instanceof String))
        {
            String message = Logging.getMessage("generic.UnrecognizedSourceType", source.toString());
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        String sourceName = (String) source;

        URL url = WWIO.makeURL(sourceName);
        if (url != null)
            return openShapefileURL(url);

        return openShapefileFile(sourceName, null);
    }

    /**
     * Normalizes a specified value to the range [-180, 180].
     *
     * @param longitudeDegrees the value to normalize.
     *
     * @return the normalized value.
     */
    public static double normalizeLongitude(double longitudeDegrees)
    {
        while (longitudeDegrees < -180)
        {
            longitudeDegrees += 360;
        }
        while (longitudeDegrees > 180)
        {
            longitudeDegrees -= 360;
        }

        return longitudeDegrees;
    }

    /**
     * Normalize the min-X and max-X values of a rectangle to the range [-180, 180].
     *
     * @param rect the rectangle to normalize.
     *
     * @return the input rectangle with X values normalized.
     *
     * @throws IllegalArgumentException if the specified rectangle reference is null.
     */
    public static Rectangle2D normalizeRectangle(Rectangle2D rect)
    {
        if (rect == null)
        {
            String message = Logging.getMessage("nullValue.RectangleIsNull");
            Logging.logger().log(java.util.logging.Level.SEVERE, message);
            throw new IllegalArgumentException(message);
        }

        if (rect.getMinX() >= -180 && rect.getMaxX() <= 180)
            return rect;

        rect.setRect(-180, rect.getY(), 360, rect.getHeight());
        return rect;
    }

    // *** I/O ***

    // TODO: Replace the next two methods with use of the WWIO equivalents.

    public static ByteBuffer readByteChannelToBuffer(ReadableByteChannel channel, int numBytes) throws IOException
    {
        return readByteChannelToBuffer(channel, numBytes, null);
    }

    public static ByteBuffer readByteChannelToBuffer(ReadableByteChannel channel, int numBytes,
        ByteBuffer buffer) throws IOException
    {
        if (buffer == null)
            buffer = ByteBuffer.allocate(numBytes);

        int bytesRead = 0;
        int count = 0;
        while (count >= 0 && (numBytes - bytesRead) > 0)
        {
            count = channel.read(buffer);
            if (count > 0)
            {
                bytesRead += count;
            }
        }

        buffer.flip();
        buffer.order(ByteOrder.LITTLE_ENDIAN); // Default to least significant byte first order.

        return buffer;
    }

    /**
     * Reads and returns an array of integers from a byte buffer.
     *
     * @param buffer     the byte buffer to read from.
     * @param numEntries the number of integers to read.
     *
     * @return the integers read.
     *
     * @throws IllegalArgumentException if the specified buffer reference is null.
     */
    public static int[] readIntArray(ByteBuffer buffer, int numEntries)
    {
        if (buffer == null)
        {
            String message = Logging.getMessage("nullValue.InputBufferIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        int[] array = new int[numEntries];
        for (int i = 0; i < numEntries; i++)
        {
            array[i] = buffer.getInt();
        }

        return array;
    }

    /**
     * Reads and returns an array of doubles from a byte buffer.
     *
     * @param buffer     the byte buffer to read from.
     * @param numEntries the number of doubles to read.
     *
     * @return the doubles read.
     *
     * @throws IllegalArgumentException if the specified buffer reference is null.
     */
    public static double[] readDoubleArray(ByteBuffer buffer, int numEntries)
    {
        if (buffer == null)
        {
            String message = Logging.getMessage("nullValue.InputBufferIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        double[] array = new double[numEntries];
        for (int i = 0; i < numEntries; i++)
        {
            array[i] = buffer.getDouble();
        }

        return array;
    }

    /**
     * Reads a Shapefile bounding rectangle from the specified buffer. This reads four doubles and interprets them as a
     * bounding rectangle in the following order: (minX, minY, maxX, maxY). If the specified parameter list is non-null
     * and describes recognized map projection, this converts the bounding rectangle from to a geographic Sector.
     * Otherwise this returns the bounding rectangle unchanged.
     *
     * @param buffer the buffer to read from.
     * @param params parameter list describing the rectangle's map projection, or null to read the bounding rectangle
     *               unchanged.
     *
     * @return the bounding rectangle, potentially converted to geographic coordinates.
     */
    public static Rectangle2D readBounds(ByteBuffer buffer, AVList params)
    {
        // Read the bounding rectangle coordinates in the following order: minx, miny, maxx, maxy.
        double[] coords = ShapefileUtils.readDoubleArray(buffer, 4);
        Rectangle2D rect = new Rectangle2D.Double(coords[0], coords[1], coords[2] - coords[0], coords[3] - coords[1]);

        // If the parameters describe a UTM projection, convert the UTM bounding rectangle to geographic coordinates.
        if (params != null)
        {
            // Projected coordinates are specified in the following order: minx, maxx, miny, maxy.
            Sector sector = Sector.fromProjectedBounds(coords[0], coords[2], coords[1], coords[3], params);
            if (sector != null)
                rect = sector.toRectangleDegrees();
        }

        return rect;
    }

    /**
     * Copy a record's X-Y points from a byte buffer to a vector buffer.
     *
     * @param from      the buffer to copy from.
     * @param to        the buffer to copy to.
     * @param position  the position in the output buffer at which to place the values.
     * @param numPoints the number of X-Y points to copy. No points are copied if the value is less than or equal to
     *                  zero.
     *
     * @throws IllegalArgumentException if either the input or output buffer reference is null, or if the specified
     *                                  position is invalid.
     */
    public static void transferPoints(ByteBuffer from, VecBuffer to, int position, int numPoints)
    {
        if (from == null)
        {
            String message = Logging.getMessage("nullValue.InputBufferIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (to == null)
        {
            String message = Logging.getMessage("nullValue.OutputBufferIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (numPoints <= 0)
            return;

        DoubleBuffer doubleBuffer = from.slice().order(ByteOrder.LITTLE_ENDIAN).asDoubleBuffer();
        doubleBuffer.limit(numPoints * 2);
        VecBuffer vecBuffer = new VecBuffer(2, new BufferWrapper.DoubleBufferWrapper(doubleBuffer));
        to.putSubBuffer(position, vecBuffer);

        from.position(from.position() + 2 * 8 * numPoints);
    }

    // *** Record selection ***

    /**
     * Selects shapefile records for which a given attribute match a given value.
     *
     * @param records         the shapefile record list.
     * @param attributeName   the name of the attribute that should match the given value.
     * @param value           the value the attribute should match.
     * @param acceptNullValue if true the filtering process will accept records which do not have the given attribute.
     *
     * @return the filtered record list or <code>null</code> if the provided record list is empty.
     */
    public static List<ShapefileRecord> selectRecords(List<ShapefileRecord> records, String attributeName,
        Object value, boolean acceptNullValue)
    {
        Iterator<ShapefileRecord> iter = records.iterator();
        if (!iter.hasNext())
            return null;

        ShapefileRecord record;
        while (iter.hasNext())
        {
            record = iter.next();
            if (record == null)
                iter.remove();
            else
            {
                Object o = record.getAttributes().getValue(attributeName);
                if ((o == null && !acceptNullValue)
                    || (o != null && o instanceof String && !((String) o).equalsIgnoreCase((String) value))
                    || (o != null && !o.equals(value)))

                    iter.remove();
            }
        }

        return records;
    }

    /**
     * Selects shapefile records that are contained in or intersect with a given {@link Sector}.
     *
     * @param records the shapefile record list.
     * @param sector  the geographic sector that records must be contained in or intersect with.
     *
     * @return the filtered record list or <code>null</code> if the provided record list is empty.
     */
    public static List<ShapefileRecord> selectRecords(List<ShapefileRecord> records, Sector sector)
    {
        Iterator<ShapefileRecord> iter = records.iterator();
        if (!iter.hasNext())
            return null;

        Rectangle2D rectangle = sector.toRectangleDegrees();
        ShapefileRecord record;
        while (iter.hasNext())
        {
            record = iter.next();
            if (record == null)
                iter.remove();
            else
            {
                if (record instanceof ShapefileRecordPoint)
                {
                    double[] point = ((ShapefileRecordPoint) record).getPoint();
                    if (!rectangle.contains(point[0], point[1]))
                        iter.remove();
                }
                else if (record instanceof ShapefileRecordPolyline)  // catches polygons too
                {
                    if (!rectangle.intersects(((ShapefileRecordPolyline) record).getBoundingRectangle()))
                        iter.remove();
                }
            }
        }

        return records;
    }

    /**
     * Returns a new {@link CompoundVecBuffer} that only contains the selected records sub-buffers or parts. Records are
     * selected when the specified attribute has the given value. Note that the original backing {@link VecBuffer} is
     * not duplicated in the process.
     * <p/>
     * String values are not case sensitive while attributes names are.
     *
     * @param shapeFile     the shapefile to select records from.
     * @param attributeName the name of the attribute which value is to be compared to the given value.
     * @param value         the value to compare the attribute with.
     *
     * @return a new {@link CompoundVecBuffer} that only contains the selected records sub-buffers or parts.
     */
    public static CompoundVecBuffer createBufferFromAttributeValue(Shapefile shapeFile, String attributeName,
        Object value)
    {
        List<ShapefileRecord> records = new ArrayList<ShapefileRecord>(shapeFile.getRecords());
        // Filter record list
        ShapefileUtils.selectRecords(records, attributeName, value, false);

        return ShapefileUtils.createBufferFromRecords(records);
    }

    /**
     * Returns a new {@link CompoundVecBuffer} that only contains the given records sub-buffers or parts. Note that the
     * original backing {@link VecBuffer} is not duplicated in the process. This returns null if the specified iterable
     * is empty, or contains only null elements.
     *
     * @param records a list of records to include in the buffer.
     *
     * @return a new {@link CompoundVecBuffer} that only contains the given records sub-buffers or parts, or null if the
     *         given records is empty or contains only null elements.
     *
     * @throws IllegalArgumentException if the iterable is null.
     */
    public static CompoundVecBuffer createBufferFromRecords(Iterable<? extends ShapefileRecord> records)
    {
        if (records == null)
        {
            String message = Logging.getMessage("nullValue.IterableIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (!records.iterator().hasNext())
            return null;

        // Count parts
        int numParts = 0;
        for (ShapefileRecord record : records)
        {
            numParts += record.getNumberOfParts();
        }

        // Get source geometry buffer
        CompoundVecBuffer sourceBuffer = null;

        // Create new offset and length buffers
        IntBuffer offsetBuffer = BufferUtil.newIntBuffer(numParts);
        IntBuffer lengthBuffer = BufferUtil.newIntBuffer(numParts);
        for (ShapefileRecord record : records)
        {
            if (record == null)
                continue;

            if (sourceBuffer == null)
                sourceBuffer = record.getShapeFile().getBuffer();

            for (int part = 0; part < record.getNumberOfParts(); part++)
            {
                offsetBuffer.put(sourceBuffer.getSubPositionBuffer().get(record.getFirstPartNumber() + part));
                lengthBuffer.put(sourceBuffer.getSubLengthBuffer().get(record.getFirstPartNumber() + part));
            }
        }
        offsetBuffer.rewind();
        lengthBuffer.rewind();

        if (sourceBuffer == null)
            return null;

        return new CompoundVecBuffer(sourceBuffer.getBackingBuffer(), offsetBuffer, lengthBuffer, numParts,
            sourceBuffer.getBufferFactory());
    }

    /**
     * Returns the {@link java.awt.geom.Rectangle2D} bounding all non-null {@link gov.nasa.worldwind.formats.shapefile.ShapefileRecord}
     * instances in the specified iterable. This returns null if the iterable is empty, contains only null elements, or
     * if none of the records has a non-null bounding rectangle.
     *
     * @param records the shapefile records to compute a bounding rectangle for.
     *
     * @return the rectangle bounding the specified records.
     */
    public static Rectangle2D computeBounds(Iterable<? extends ShapefileRecord> records)
    {
        if (records == null)
        {
            String message = Logging.getMessage("nullValue.IterableIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Rectangle2D bounds = null;

        for (ShapefileRecord rec : records)
        {
            if (rec == null)
                continue;

            Rectangle2D rect = null;
            if (rec instanceof ShapefileRecordPoint)
            {
                double[] coord = ((ShapefileRecordPoint) rec).getPoint();
                rect = new Rectangle2D.Double(coord[0], coord[1], 0, 0);
            }
            if (rec instanceof ShapefileRecordMultiPoint)
                rect = ((ShapefileRecordMultiPoint) rec).getBoundingRectangle();
            else if (rec instanceof ShapefileRecordPolyline)
                rect = ((ShapefileRecordPolyline) rec).getBoundingRectangle();

            if (rect == null)
                continue;

            bounds = (bounds != null) ? bounds.createUnion(rect) : rect;
        }

        return bounds;
    }
}
