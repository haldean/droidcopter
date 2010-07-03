/*
Copyright (C) 2001, 2008 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/

package gov.nasa.worldwind.terrain;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.cache.FileStore;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.ElevationModel;
import gov.nasa.worldwind.util.Logging;

import java.util.List;

/**
 * @author tag
 * @version $Id: AbstractElevationModel.java 13293 2010-04-12 20:52:57Z tgaskins $
 */
abstract public class AbstractElevationModel extends WWObjectImpl implements ElevationModel
{
    private FileStore dataFileStore = WorldWind.getDataFileStore();
    protected double missingDataFlag = -Double.MAX_VALUE;
    protected double missingDataValue = 0;

    protected Double transparentElevationValue;
    private boolean networkRetrievalEnabled = true;
    private long expiryTime = 0;

    public String getName()
    {
        Object n = this.getValue(AVKey.DISPLAY_NAME);

        return n != null ? n.toString() : this.toString();
    }

    public void setName(String name)
    {
        this.setValue(AVKey.DISPLAY_NAME, name);
    }

    public String toString()
    {
        Object n = this.getValue(AVKey.DISPLAY_NAME);

        return n != null ? n.toString() : super.toString();
    }

    public boolean isNetworkRetrievalEnabled()
    {
        return this.networkRetrievalEnabled;
    }

    public void setNetworkRetrievalEnabled(boolean enabled)
    {
        this.networkRetrievalEnabled = enabled;
    }

    public long getExpiryTime()
    {
        return this.expiryTime;
    }

    public void setExpiryTime(long expiryTime)
    {
        this.expiryTime = expiryTime;
    }

    public double getMissingDataSignal()
    {
        return missingDataFlag;
    }

    public void setMissingDataSignal(double missingDataFlag)
    {
        this.missingDataFlag = missingDataFlag;
    }

    public double getMissingDataReplacement()
    {
        return missingDataValue;
    }

    public void setMissingDataReplacement(double missingDataValue)
    {
        this.missingDataValue = missingDataValue;
    }

    public Double getTransparentElevationValue()
    {
        return transparentElevationValue;
    }

    public void setTransparentElevationValue(Double transparentElevationValue)
    {
        this.transparentElevationValue = transparentElevationValue;
    }

    public double getDetailHint(Sector sector)
    {
        if (sector == null)
        {
            String msg = Logging.getMessage("nullValue.SectorIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        return 0.0;
    }

    public FileStore getDataFileStore()
    {
        return dataFileStore;
    }

    public void setDataFileStore(FileStore dataFileStore)
    {
        this.dataFileStore = dataFileStore;
    }

    public double getElevation(Angle latitude, Angle longitude)
    {
        if (latitude == null || longitude == null)
        {
            String msg = Logging.getMessage("nullValue.LatitudeOrLongitudeIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        double e = this.getUnmappedElevation(latitude, longitude);
        return e == this.missingDataFlag ? this.missingDataValue : e;
    }

    public String getRestorableState()
    {
        return null;
    }

    public void restoreState(String stateInXml)
    {
        String message = Logging.getMessage("RestorableSupport.RestoreNotSupported");
        Logging.logger().severe(message);
        throw new UnsupportedOperationException(message);
    }

    public void composeElevations(Sector sector, List<? extends LatLon> latlons, int tileWidth, double[] buffer)
        throws Exception
    {
        if (sector == null)
        {
            String msg = Logging.getMessage("nullValue.SectorIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (latlons == null)
        {
            String msg = Logging.getMessage("nullValue.LatLonListIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (buffer == null)
        {
            String msg = Logging.getMessage("nullValue.ElevationsBufferIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (buffer.length < latlons.size() || tileWidth > latlons.size())
        {
            String msg = Logging.getMessage("ElevationModel.ElevationsBufferTooSmall", latlons.size());
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        for (int i = 0; i < latlons.size(); i++)
        {
            LatLon ll = latlons.get(i);
            double e = this.getUnmappedElevation(ll.getLatitude(), ll.getLongitude());
            if (e != this.getMissingDataSignal() && !this.isTransparentValue(e))
                buffer[i] = e;
        }
    }

    protected boolean isTransparentValue(Double value)
    {
        return ((value == null || value.equals(this.getMissingDataSignal()))
            && this.getMissingDataReplacement() == this.getMissingDataSignal())
            || (value != null && this.getTransparentElevationValue() != null
            && value.equals(this.getTransparentElevationValue()));
    }
}
