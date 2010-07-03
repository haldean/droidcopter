/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind;

import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.util.LevelSet;

/**
 * An interface to heirarchical properties which describe the data for a particular component, or set of components.
 * <p/>
 * The parameter name property accepted by the getter methods denotes an abstract configuration path, which is evaluated
 * according to the specified type. This configuration's values and its children's values are evaluated in this path.
 * However a null path indicates that only this configuration is evaluated.
 *
 * @author dcollins
 * @version $Id: DataConfiguration.java 13207 2010-03-12 08:54:15Z tgaskins $
 */
public interface DataConfiguration
{
    /**
     * Returns the name of this configuration information. This value is likely a simple title for the component this
     * configuration information describes.
     *
     * @return a String title for the described component.
     */
    String getName();

    /**
     * Returns the type of this configuration information. For example, if this is a Layer configuration this returns
     * "Layer", and if this describes an ElevationModel configuration this returns "ElevationModel".
     *
     * @return the type of configuration information.
     */
    String getType();

    /**
     * Returns the version of this configuration information, or null if no version exists.
     *
     * @return the configuration version, or null if none exists.
     */
    String getVersion();

    /**
     * Returns the backing source of configuration information. Implementations are potentially backed by any source
     * types. For example, if this configuration information is backed by an XML document object model (DOM), this
     * likely returns a DOM {@link org.w3c.dom.Element} reference.
     *
     * @return the backing configuration source.
     */
    Object getSource();

    /**
     * Returns the child configuration with a specified name.
     *
     * @param paramName name of the child configuration.
     *
     * @return a child configuration, or null if none exists.
     *
     * @throws IllegalArgumentException if the parameter name is null.
     */
    DataConfiguration getChild(String paramName);

    /**
     * Returns an array of child configurations with a specified name.
     *
     * @param paramName name of the child configurations.
     *
     * @return an array of child configurations, or null if none exists.
     *
     * @throws IllegalArgumentException if the parameter name is null.
     */
    DataConfiguration[] getChildren(String paramName);

    /**
     * Returns the {@link String} value identified by a specified parameter name.
     *
     * @param paramName the parameter name.
     *
     * @return String value of a parameter with the specified name. Returns null if no parameter exists with the
     *         specified name.
     */
    String getString(String paramName);

    /**
     * Returns all String values identified by a specified parameter name.
     *
     * @param paramName the parameter name.
     *
     * @return an array containing the value of each parameter matching the specified name. Returns null if no
     *         parameters exist with the specified name.
     */
    String[] getStringArray(String paramName);

    /**
     * Returns all unique String values identified by a specified parameter name.
     *
     * @param paramName the parameter name.
     *
     * @return an array containing the value of each parameter matching the specified name, and containing a unique
     *         value. Returns null if no parameters exist with the specified name.
     */
    String[] getUniqueStrings(String paramName);

    /**
     * Returns the {@link Integer} value identified by a specified parameter name.
     *
     * @param paramName the parameter name.
     *
     * @return Integer value of a parameter with the specified name. Returns null if no parameter exists with the
     *         specified name.
     */
    Integer getInteger(String paramName);

    /**
     * Returns the {@link Long} value identified by a specified parameter name.
     *
     * @param paramName the parameter name.
     *
     * @return Long value of a parameter with the specified name. Returns null if no parameter exists with the specified
     *         name.
     */
    Long getLong(String paramName);

    /**
     * Returns the {@link Double} value identified by a specified parameter name.
     *
     * @param paramName the parameter name.
     *
     * @return Double value of a parameter with the specified name. Returns null if no parameter exists with the
     *         specified name.
     */
    Double getDouble(String paramName);

    /**
     * Returns the {@link Boolean} value identified by a specified parameter name.
     *
     * @param paramName the parameter name.
     *
     * @return Boolean value of a parameter with the specified name. Returns null if no parameter exists with the
     *         specified name.
     */
    Boolean getBoolean(String paramName);

    /**
     * Returns the {@link LatLon} value identified by a specified parameter name.
     *
     * @param paramName the parameter name.
     *
     * @return LatLon value of a parameter with the specified name. Returns null if no parameter exists with the
     *         specified name.
     */
    LatLon getLatLon(String paramName);

    /**
     * Returns the {@link gov.nasa.worldwind.geom.Sector} value identified by a specified parameter name.
     *
     * @param paramName the parameter name.
     *
     * @return Sector value of a parameter with the specified name. Returns null if no parameter exists with the
     *         specified name.
     */
    Sector getSector(String paramName);

    /**
     * Returns the {@link gov.nasa.worldwind.util.LevelSet.SectorResolution} value identified by a specified parameter
     * name.
     *
     * @param paramName the parameter name.
     *
     * @return LevelSet.SectorResolution value of a parameter with the specified name. Returns null if no parameter
     *         exists with the specified name.
     */
    LevelSet.SectorResolution getSectorResolutionLimit(String paramName);

    /**
     * Returns the time value in milliseconds identified by a specified parameter name.
     *
     * @param paramName the parameter name.
     *
     * @return time in milliseconds of a parameter with the specified name. Returns null if no parameter exists with the
     *         specified name.
     */
    Long getTimeInMillis(String paramName);

    /**
     * Returns the date and time value in milliseconds identified by a specified parameter name and date-format pattern.
     * The element contents must either match the pattern or be directly convertible to a long. The value returned is
     * the number of milliseconds from the epoch to the date.
     *
     * @param paramName the parameter name.
     * @param pattern   the format pattern of the date. See {@link java.text.DateFormat} for the pattern symbols.
     *
     * @return time in milliseconds of a parameter with the specified name. Returns null if no parameter exists with the
     *         specified name.
     */
    Long getDateTimeInMillis(String paramName, String pattern);
}
