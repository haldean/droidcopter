/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.formats.shapefile;

import gov.nasa.worldwind.exception.WWRuntimeException;
import gov.nasa.worldwind.util.*;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;

/**
 * @author Patrick Murris
 * @version $Id: DBaseFile.java 13118 2010-02-14 21:15:07Z tgaskins $
 */
public class DBaseFile
{
    protected static final int FIXED_HEADER_LENGTH = 32;
    public static final int FIELD_DESCRIPTOR_LENGTH = 32;

    protected class Header
    {
        public int fileCode;
        public Date lastModificationDate;
        public int numberOfRecords;
        public int headerLength;
        public int recordLength;
        public ByteBuffer fieldsHeaderBuffer;
    }

    private final File file;
    protected final Header header;
    private DBaseField[] fields;
    private List<DBaseRecord> records;

    public DBaseFile(File file)
    {
        if (file == null)
        {
            String message = Logging.getMessage("nullValue.FileIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.file = file;
        try
        {
            this.header = this.readHeaderFromFile(file);
            this.fields = this.readFieldsFromBuffer(this.header.fieldsHeaderBuffer);
            // Delay records loading until getRecords() is called.
        }
        catch (Exception e)
        {
            String message = Logging.getMessage("SHP.ExceptionAttemptingToReadFile", file.getPath());
            Logging.logger().log(java.util.logging.Level.SEVERE, message, e);
            throw new WWRuntimeException(message, e);
        }

    }

    public DBaseFile(InputStream is)
    {
        if (is == null)
        {
            String message = Logging.getMessage("nullValue.InputStreamIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.file = null;
        try
        {
            this.header = this.readHeaderFromStream(is);
            this.fields = this.readFieldsFromBuffer(this.header.fieldsHeaderBuffer);
            this.records = this.readRecordsFromStream(is);
        }
        catch (Exception e)
        {
            String message = Logging.getMessage("generic.ExceptionAttemptingToReadFrom", is.toString());
            Logging.logger().log(java.util.logging.Level.SEVERE, message, e);
            throw new WWRuntimeException(message, e);
        }

    }

    public File getFile()
    {
        return this.file;
    }

    public Date getLastModificationDate()
    {
        return this.header.lastModificationDate;
    }

    public int getNumberOfRecords()
    {
        return this.header.numberOfRecords;
    }

    public int getHeaderLength()
    {
        return this.header.headerLength;
    }

    public int getRecordLength()
    {
        return this.header.recordLength;
    }

    public DBaseField[] getFields()
    {
        return this.fields;
    }

    public List<DBaseRecord> getRecords()
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

    protected Header readHeaderFromStream(InputStream is) throws IOException
    {
        ReadableByteChannel channel = Channels.newChannel(is);
        // Read header fixed portion
        ByteBuffer headerBuffer = ShapefileUtils.readByteChannelToBuffer(channel, FIXED_HEADER_LENGTH);
        Header header = this.readHeaderFromBuffer(headerBuffer);
        // Read fields description header
        int fieldsHeaderLength = header.headerLength - FIXED_HEADER_LENGTH;
        header.fieldsHeaderBuffer = ShapefileUtils.readByteChannelToBuffer(channel, fieldsHeaderLength);

        return header;
    }

    protected Header readHeaderFromBuffer(ByteBuffer buffer) throws WWRuntimeException
    {
        // Read file code - first byte
        int fileCode = buffer.get();
        if (fileCode > 5)
        {
            String message = Logging.getMessage("SHP.NotADBaseFile", file.getPath());
            Logging.logger().log(java.util.logging.Level.SEVERE, message);
            throw new WWRuntimeException(message);
        }

        // Last update date
        int yy = 0xFF & buffer.get(); // unsigned
        int mm = buffer.get();
        int dd = buffer.get();

        // Number of records
        int numRecords = buffer.getInt();

        // Header struct length
        int headerLength = buffer.getShort();

        // Record length
        int recordLength = buffer.getShort();

        // Assemble header
        Header header = new Header();
        header.fileCode = fileCode;
        Calendar cal = Calendar.getInstance();
        cal.set(1900 + yy, mm - 1, dd);
        header.lastModificationDate = cal.getTime();
        header.numberOfRecords = numRecords;
        header.headerLength = headerLength;
        header.recordLength = recordLength;

        return header;
    }

    protected DBaseField[] readFieldsFromBuffer(ByteBuffer buffer)
    {
        int numFields = (this.header.headerLength - 1 - FIXED_HEADER_LENGTH) / FIELD_DESCRIPTOR_LENGTH;

        DBaseField[] fields = new DBaseField[numFields];
        for (int i = 0; i < numFields; i++)
            fields[i] = DBaseField.fromBuffer(buffer);

        return fields;
    }

    protected List<DBaseRecord> readRecordsFromFile(File file) throws IOException
    {
        List<DBaseRecord> records;
        InputStream is = null;

        try
        {
            is = new BufferedInputStream(new FileInputStream(file));
            WWIO.skipBytes(is, this.header.headerLength); // Skip over header
            records = this.readRecordsFromStream(is);
        }
        finally
        {
            if (is != null)
                is.close();
        }

        return records;
    }

    protected List<DBaseRecord> readRecordsFromStream(InputStream is) throws IOException
    {
        List<DBaseRecord> records = new ArrayList<DBaseRecord>();
        ByteBuffer recordBuffer = ByteBuffer.allocate(this.getRecordLength());
        ReadableByteChannel channel = Channels.newChannel(is);

        // Read all records
        for (int i = 0; i < this.getNumberOfRecords(); i++)
        {
            // Read record
            recordBuffer.rewind();
            ShapefileUtils.readByteChannelToBuffer(channel, this.getRecordLength(), recordBuffer);
            records.add(DBaseRecord.fromBuffer(this, recordBuffer, i + 1));
        }

        return records;
    }

    // Support methods

    public static String readZeroTerminatedString(ByteBuffer buffer, int length)
    {
        int pos = buffer.position();
        byte[] bytes = new byte[length];
        String string;
        int i;

        for (i = 0; i < length; i++)
        {
            byte b = buffer.get();
            if (b == 0)
                break;
            bytes[i] = b;
        }

        try
        {
            string = new String(bytes, 0, i, "UTF8");
        }
        catch (UnsupportedEncodingException e)
        {
            string = new String(bytes, 0, i);
        }

        buffer.position(pos + length);  // move buffer pos of full length
        return string;
    }

    // Tests
//    public static void main(String[] args)
//    {
//        File file = new File("J:\\Data\\Shapefiles\\Users\\wpl_oceans.dbf");
//        try
//        {
//            DBaseFile dbf = new DBaseFile(file);
//            //DBaseFile dbf = new DBaseFile(new BufferedInputStream(new FileInputStream(file)));
//            System.out.println("File: " + (dbf.getFile() != null ? dbf.getFile().getName() : "null"));
//            System.out.println("date: " + dbf.getLastModificationDate());
//            System.out.println("records: " + dbf.getNumberOfRecords());
//            System.out.println("header length: " + dbf.getHeaderLength());
//            System.out.println("record length: " + dbf.getRecordLength());
//
//            DBaseField[] fields = dbf.getFields();
//            for (DBaseField field : fields)
//                System.out.println("Field : " + field.getName() + ", " + field.getType() + ", " + field.getLength() + ", " + field.getDecimals());
//
//            List<DBaseRecord> records = dbf.getRecords();
//            System.out.println("records: " + records.size());
//            for (int i = 0; i < records.size() && i < 30; i++)
//            {
//                DBaseRecord record = records.get(i);
//                System.out.print("Record " + record.getRecordNumber() + " ");
//                for (DBaseField field : fields)
//                    System.out.print(record.getValue(field.getName()) + ", ");
//
//                System.out.println("");
//            }
//        }
//        catch (Exception e)
//        {
//            e.printStackTrace();
//        }
//
//    }


}