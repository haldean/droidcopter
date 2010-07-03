/*
Copyright (C) 2001, 2010 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.applications.gos;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.applications.gos.services.FGDCServiceStatus;
import gov.nasa.worldwind.cache.*;
import gov.nasa.worldwind.ogc.wms.WMSCapabilities;
import gov.nasa.worldwind.util.*;
import org.w3c.dom.*;

import javax.imageio.ImageIO;
import java.awt.image.*;
import java.beans.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;

/**
 * @author dcollins
 * @version $Id: ResourceUtil.java 13362 2010-04-30 19:43:27Z dcollins $
 */
public class ResourceUtil // TODO: standardize, back-port stadard usage
{
    protected static final int DEFAULT_APP_THREAD_COUNT = 4;
    protected static ExecutorService executorService = Executors.newFixedThreadPool(
        Configuration.getIntegerValue(GeodataKey.APP_THREAD_COUNT, DEFAULT_APP_THREAD_COUNT));

    public static ExecutorService getAppTaskService()
    {
        return executorService;
    }

    public static URI getResourceURI(String name)
    {
        Element resourceList = Configuration.getElement("//ResourceList");
        if (resourceList == null)
            return null;

        String uriString = WWXML.getText(resourceList, "./Resource[@name=\"" + name + "\"]/@uri", null);
        try
        {
            if (!WWUtil.isEmpty(uriString))
                return new URI(uriString);
        }
        catch (URISyntaxException e)
        {
            String message = Logging.getMessage("gosApp.ResourceURIInvalid", uriString);
            Logging.logger().severe(message);
        }

        return null;
    }

    public static WMSCapabilities getCapabilities(OnlineResource resource)
    {
        URI uri = resource.getURI();
        if (uri == null)
            return null;

        return openCapabilitiesURI(uri);
    }

    public static BufferedImage getImage(String name)
    {
        URI uri = getResourceURI(name);
        if (uri == null)
            return null;

        return openImageURI(uri);
    }

    public static BufferedImage getImage(OnlineResource resource)
    {
        URI uri = resource.getURI();
        if (uri == null)
            return null;

        return openImageURI(uri);
    }

    public static ServiceStatus getCachedServiceStatus(OnlineResource resource)
    {
        URI uri = resource.getURI();
        if (uri == null)
            return null;

        return (ServiceStatus) WorldWind.getSessionCache().get(uri.toString());
    }

    public static ServiceStatus getOrRetrieveServiceStatus(OnlineResource resource, PropertyChangeListener listener)
    {
        URI uri = resource.getURI();
        if (uri == null)
            return null;

        return getOrRetrieveServiceStatusURI(uri, listener);
    }

    public static WMSCapabilities openCapabilitiesURI(URI uri)
    {
        WMSCapabilities caps = (WMSCapabilities) WorldWind.getSessionCache().get(uri.toString());

        if (caps == null && !WorldWind.getSessionCache().contains(uri.toString()))
        {
            caps = retrieveCapabilities(uri);
            WorldWind.getSessionCache().put(uri.toString(), caps);
        }

        return caps;
    }

    public static Document openDocumentURI(URI uri)
    {
        Document doc = (Document) WorldWind.getSessionCache().get(uri.toString());

        if (doc == null && !WorldWind.getSessionCache().contains(uri.toString()))
        {
            doc = retrieveDocument(uri);
            WorldWind.getSessionCache().put(uri.toString(), doc);
        }

        return doc;
    }

    public static BufferedImage openImageURI(URI uri)
    {
        BufferedImage image = (BufferedImage) getIconCache().getObject(uri.toString());

        if (image == null && !getIconCache().contains(uri.toString()))
        {
            image = retrieveImage(uri);
            getIconCache().add(uri.toString(), image, (image != null) ? ImageUtil.computeSizeInBytes(image) : 1L);
        }

        return image;
    }

    public static ServiceStatus getOrRetrieveServiceStatusURI(final URI uri, final PropertyChangeListener listener)
    {
        ServiceStatus status = (ServiceStatus) WorldWind.getSessionCache().get(uri.toString());

        if (status == null && !WorldWind.getSessionCache().contains(uri.toString()))
        {
            getAppTaskService().execute(new Runnable()
            {
                public void run()
                {
                    ServiceStatus status = retrieveServiceStatus(uri);
                    WorldWind.getSessionCache().put(uri.toString(), status);

                    if (listener != null)
                    {
                        listener.propertyChange(new PropertyChangeEvent(uri.toString(), GeodataKey.SERVICE_STATUS,
                            null, status));
                    }
                }
            });
        }

        return status;
    }

    protected static WMSCapabilities retrieveCapabilities(URI uri)
    {
        try
        {
            WMSCapabilities caps = WMSCapabilities.retrieve(uri);
            if (caps != null)
                caps.parse();
        }
        catch (Exception e)
        {
            String message = Logging.getMessage("generic.ExceptionAttemptingToRetrieveCapabilities", uri.toString());
            Logging.logger().severe(message);
        }

        return null;
    }

    protected static Document retrieveDocument(URI uri)
    {
        try
        {
            return WWXML.openDocumentURL(uri.toURL());
        }
        catch (Exception e)
        {
            String message = Logging.getMessage("generic.ExceptionAttemptingToParseXml", uri.toString());
            Logging.logger().severe(message);
        }

        return null;
    }

    protected static BufferedImage retrieveImage(URI uri)
    {
        try
        {
            if (uri.isAbsolute())
            {
                return ImageIO.read(uri.toURL());
            }

            InputStream is = null;
            try
            {
                is = WWIO.openFileOrResourceStream(uri.toString(), null);
                if (is != null)
                    return ImageIO.read(is);
            }
            finally
            {
                if (is != null)
                    WWIO.closeStream(is, uri.toString());
            }
        }
        catch (IOException e)
        {
            String message = Logging.getMessage("generic.ExceptionAttemptingToReadImageFile", uri.toString());
            Logging.logger().severe(message);
        }

        return null;
    }

    protected static ServiceStatus retrieveServiceStatus(URI uri)
    {
        try
        {
            return FGDCServiceStatus.retrieve(uri);
        }
        catch (Exception e)
        {
            String message = Logging.getMessage("gosApp.ExceptionRetrievingServiceStatus", uri.toString());
            Logging.logger().severe(message);
        }

        return null;
    }

    protected static MemoryCache getIconCache()
    {
        if (!WorldWind.getMemoryCacheSet().containsCache(GeodataKey.IMAGE_CACHE))
        {
            long capacity = Configuration.getLongValue(GeodataKey.IMAGE_CACHE_SIZE, 10000000L);
            MemoryCache cache = new BasicMemoryCache((long) (0.85 * capacity), capacity);
            WorldWind.getMemoryCacheSet().addCache(GeodataKey.IMAGE_CACHE, cache);
        }

        return WorldWind.getMemoryCache(GeodataKey.IMAGE_CACHE);
    }
}
