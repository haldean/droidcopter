package gov.nasa.worldwind;

import gov.nasa.worldwind.avlist.AVList;

/**
 * General factory interface.
 *
 * @author tag
 * @version $Id: Factory.java 12579 2009-09-12 01:07:05Z tgaskins $
 */
public interface Factory
{
    /**
     * Create a component from a specified data configuration, potentially overriding values from the configuration with
     * values from the specified parameters.
     *
     * @param dataConfig the configuration information describing the component.
     * @param params     key-value parameters which override or supplement the information provided in the specified
     *                   configuration. A null reference is permitted.
     *
     * @return an instance of the requested class.
     *
     * @throws gov.nasa.worldwind.exception.WWRuntimeException
     *          if the class cannot be created for any reason.
     */
    Object createFromDataConfig(DataConfiguration dataConfig, AVList params);

    /**
     * Creates an object from a general configuration source.
     *
     * @param configSource the configuration source.
     * @param params properties to apply during object creation.
     *
     * @return the new object.
     *
     * @throws IllegalArgumentException if the configuration source is null or an empty string.
     * @throws gov.nasa.worldwind.exception.WWUnrecognizedException
     *                                  if the type of source or some object-specific value is unrecognized.
     * @throws gov.nasa.worldwind.exception.WWRuntimeException
     *                                  if object creation fails. The exception indicating the source of the failure is
     *                                  included as the {@link Exception#initCause(Throwable)}.
     */
    Object createFromConfigSource(Object configSource, AVList params);
}
