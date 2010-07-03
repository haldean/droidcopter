/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.cache;

import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.util.*;

import java.io.*;

/**
 * @author Tom Gaskins
 * @version $Id: BasicDataFileStore.java 12546 2009-09-03 05:36:57Z tgaskins $
 */
public class BasicDataFileStore extends AbstractFileStore
{
    public BasicDataFileStore()
    {
        String configPath = Configuration.getStringValue(AVKey.DATA_FILE_STORE_CONFIGURATION_FILE_NAME);
        if (configPath == null)
        {
            String message = Logging.getMessage("FileStore.NoConfiguration");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        java.io.InputStream is = null;
        File configFile = new File(configPath);
        if (configFile.exists())
        {
            try
            {
                is = new FileInputStream(configFile);
            }
            catch (FileNotFoundException e)
            {
                String message = Logging.getMessage("FileStore.LocalConfigFileNotFound", configPath);
                Logging.logger().finest(message);
            }
        }

        if (is == null)
        {
            is = this.getClass().getClassLoader().getResourceAsStream(configPath);
        }

        if (is == null)
        {
            String message = Logging.getMessage("FileStore.ConfigurationNotFound", configPath);
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        this.initialize(is);
    }

    public BasicDataFileStore(File location)
    {
        StringBuilder sb = new StringBuilder("<?xml version=\"1.0\"?>");

        sb.append("<dataFileStore><writeLocations><location wwDir=\"");
        sb.append(location.getAbsolutePath());
        sb.append("\" create=\"true\"/></writeLocations></dataFileStore>");

        this.initialize(WWIO.getInputStreamFromString(sb.toString()));
    }
}
