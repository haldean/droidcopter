/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.data;

import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.util.Logging;

import java.util.Map;

/**
 * @author Lado Garakanidze
 * @version $Id: AbstractDataRaster.java 13196 2010-03-10 07:40:57Z garakl $
 */
public abstract class AbstractDataRaster extends AVListImpl implements DataRaster
{
    private int width = 0;
    private int height = 0;
    private Sector sector = null;

    protected AbstractDataRaster(int width, int height, Sector sector) throws IllegalArgumentException
    {
        super();
        
        if (width < 0)
        {
            String message = Logging.getMessage("generic.InvalidWidth", width );
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (height < 0)
        {
            String message = Logging.getMessage("generic.InvalidHeight", height );
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
//        if (sector == null)
//        {
//            String message = Logging.getMessage("nullValue.SectorIsNull");
//            Logging.logger().severe(message);
//            throw new IllegalArgumentException(message);
//        }

        // for performance reasons we are "caching" these parameters in addition to AVList
        this.width = width;
        this.height = height;
        this.sector = sector;
        
        this.setValue( AVKey.SECTOR, sector );
        this.setValue( AVKey.WIDTH,  width  );
        this.setValue( AVKey.HEIGHT, height );
    }

    protected AbstractDataRaster(int width, int height, Sector sector, AVList list) throws IllegalArgumentException
    {
        this(width, height, sector);

        if( null != list)
        {
            for(Map.Entry<String, Object> entry : list.getEntries())
                this.setValue(entry.getKey(), entry.getValue());
        }        
    }

    public int getWidth()
    {
        return this.width;
    }

    public int getHeight()
    {
        return this.height;
    }

    public Sector getSector()
    {
        return this.sector;
    }

    @Override
    public Object setValue(String key, Object value)
    {
        if( null == key )
        {
            String message = Logging.getMessage("nullValue.KeyIsNull" );
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        // Do not allow to change existing WIDTH or HEIGHT

        if( this.hasKey(key) )
        {
            if( AVKey.WIDTH.equals(key) && this.getWidth() != (Integer)value )
            {
                String message = Logging.getMessage("generic.AttemptToChangeReadOnlyProperty", key );
                Logging.logger().finest(message);
                // TODO relax restriction, just log and continue
//                throw new IllegalArgumentException(message);
                return this;
            }
            else if( AVKey.HEIGHT.equals(key) && this.getHeight() != (Integer)value ) 
            {
                String message = Logging.getMessage("generic.AttemptToChangeReadOnlyProperty", key );
                Logging.logger().finest(message);
                // TODO relax restriction, just log and continue
//                throw new IllegalArgumentException(message);
                return this;
            }
        }
        return super.setValue( key, value );
    }
}
