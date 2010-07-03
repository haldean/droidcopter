/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.formats.shapefile;

import gov.nasa.worldwind.exception.WWRuntimeException;
import gov.nasa.worldwind.util.Logging;

import java.nio.ByteBuffer;

/**
 * @author Patrick Murris
 * @version $Id: DBaseField.java 13081 2010-02-01 00:45:48Z patrickmurris $
 */
public class DBaseField
{
    public static final String TYPE_CHAR = "DBase.FieldTypeChar";
    public static final String TYPE_NUMBER = "DBase.FieldTypeNumber";
    public static final String TYPE_DATE = "DBase.FieldTypeDate";
    public static final String TYPE_BOOLEAN = "DBase.FieldTypeBoolean";

    private String name;
    private String type;
    private int length;
    private int decimals;

    public String getName()
    {
        return this.name;
    }

    public String getType()
    {
        return this.type;
    }

    public int getLength()
    {
        return this.length;
    }

    public int getDecimals()
    {
        return this.decimals;
    }

    protected static DBaseField fromBuffer(ByteBuffer buffer)
    {
        DBaseField field = new DBaseField();

        int pos = buffer.position();

        field.name = DBaseFile.readZeroTerminatedString(buffer, 11);
        
        char type = (char)buffer.get();
        field.type = getFieldType(type);
        if (field.type == null)
        {
            String message = Logging.getMessage("SHP.UnsupportedDBaseFieldType", type);
            Logging.logger().log(java.util.logging.Level.SEVERE, message);
            throw new WWRuntimeException(message);
        }

        // Skip four bytes
        buffer.getInt();

        field.length = 0xff & buffer.get();    // unsigned
        field.decimals = 0xff & buffer.get();

        buffer.position(pos + DBaseFile.FIELD_DESCRIPTOR_LENGTH); // move to next field
        return field;
    }

    public static String getFieldType(char type)
    {
        switch (type)
        {
            case 'C':
                return TYPE_CHAR;
            case 'D':
                return TYPE_DATE;
            case 'F':
                return TYPE_NUMBER;
            case 'L':
                return TYPE_BOOLEAN;
            case 'N':
                return TYPE_NUMBER;
            default:
                return null;
        }
    }
}
