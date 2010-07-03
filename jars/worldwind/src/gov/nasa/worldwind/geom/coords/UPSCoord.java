/*
Copyright (C) 2001, 2008 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.geom.coords;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.util.Logging;

/**
 * This immutable class holds a set of UPS coordinates along with it's
 * corresponding latitude and longitude.
 *
 * @author Patrick Murris
 * @version $Id$
 */

public class UPSCoord
{
    private final Angle latitude;
    private final Angle longitude;
    private final char hemisphere;
    private final double easting;
    private final double northing;

    /**
     * Create a set of UPS coordinates from a pair of latitude and longitude
     * for a WGS84 globe.
     *
     * @param latitude the latitude <code>Angle</code>.
     * @param longitude the longitude <code>Angle</code>.
     * @return the corresponding <code>UPSCoord</code>.
     * @throws IllegalArgumentException if <code>latitude</code> or <code>longitude</code> is null,
     * or the conversion to UPS coordinates fails.
     */
    public static UPSCoord fromLatLon(Angle latitude, Angle longitude)
    {
        return fromLatLon(latitude, longitude, null);
    }

    /**
     * Create a set of UPS coordinates from a pair of latitude and longitude
     * for the given <code>Globe</code>.
     *
     * @param latitude the latitude <code>Angle</code>.
     * @param longitude the longitude <code>Angle</code>.
     * @param globe the <code>Globe</code> - can be null (will use WGS84).
     * @return the corresponding <code>UPSCoord</code>.
     * @throws IllegalArgumentException if <code>latitude</code> or <code>longitude</code> is null,
     * or the conversion to UPS coordinates fails.
     */
    public static UPSCoord fromLatLon(Angle latitude, Angle longitude, Globe globe)
    {
        if (latitude == null || longitude == null)
        {
            String message = Logging.getMessage("nullValue.LatitudeOrLongitudeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        final UPSCoordConverter converter = new UPSCoordConverter(globe);
        long err = converter.convertGeodeticToUPS(latitude.radians, longitude.radians);

        if (err != UPSCoordConverter.UPS_NO_ERROR)
        {
            String message = Logging.getMessage("Coord.UPSConversionError");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return new UPSCoord(latitude, longitude, converter.getHemisphere(),
                converter.getEasting(), converter.getNorthing());
    }

    /**
     * Create a set of UPS coordinates for a WGS84 globe.
     *
     * @param hemisphere the hemisphere 'N' or 'S'.
     * @param easting the easting distance in meters
     * @param northing the northing distance in meters.
     * @return the corresponding <code>UPSCoord</code>.
     * @throws IllegalArgumentException if the conversion to UPS coordinates fails.
     */
    public static UPSCoord fromUTM(char hemisphere, double easting, double northing)
    {
        return fromUPS(hemisphere, easting, northing, null);
    }

    /**
     * Create a set of UPS coordinates for the given <code>Globe</code>.
     *
     * @param hemisphere the hemisphere 'N' or 'S'.
     * @param easting the easting distance in meters
     * @param northing the northing distance in meters.
     * @param globe the <code>Globe</code> - can be null (will use WGS84).
     * @return the corresponding <code>UPSCoord</code>.
     * @throws IllegalArgumentException if the conversion to UPS coordinates fails.
     */
    public static UPSCoord fromUPS(char hemisphere, double easting, double northing, Globe globe)
    {
        final UPSCoordConverter converter = new UPSCoordConverter(globe);
        long err = converter.convertUPSToGeodetic(hemisphere, easting, northing);

        if (err != UTMCoordConverter.UTM_NO_ERROR)
        {
            String message = Logging.getMessage("Coord.UTMConversionError");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return new UPSCoord(Angle.fromRadians(converter.getLatitude()),
                Angle.fromRadians(converter.getLongitude()),
                hemisphere, easting, northing);
    }


    /**
     * Create an arbitrary set of UPS coordinates with the given values.
     *
     * @param latitude the latitude <code>Angle</code>.
     * @param longitude the longitude <code>Angle</code>.
     * @param hemisphere the hemisphere 'N' or 'S'.
     * @param easting the easting distance in meters
     * @param northing the northing distance in meters.
     * @throws IllegalArgumentException if <code>latitude</code> or <code>longitude</code> is null.
     */
    public UPSCoord(Angle latitude, Angle longitude, char hemisphere, double easting, double northing)
    {
        if (latitude == null || longitude == null)
        {
            String message = Logging.getMessage("nullValue.LatitudeOrLongitudeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.latitude = latitude;
        this.longitude = longitude;
        this.hemisphere = hemisphere;
        this.easting = easting;
        this.northing = northing;
    }

    public Angle getLatitude()
    {
        return this.latitude;
    }

    public Angle getLongitude()
    {
        return this.longitude;
    }

    public char getHemisphere()
    {
        return this.hemisphere;
    }

    public double getEasting()
    {
        return this.easting;
    }

    public double getNorthing()
    {
        return this.northing;
    }

    public String toString()
    {
        return hemisphere + " " + (int)easting + "E" + " " + (int)northing + "N";
    }


}
