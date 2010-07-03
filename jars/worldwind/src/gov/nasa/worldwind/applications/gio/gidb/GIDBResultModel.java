/*
Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.applications.gio.gidb;

import gov.nasa.worldwind.applications.gio.catalogui.CatalogException;
import gov.nasa.worldwind.applications.gio.catalogui.CatalogExceptionList;
import gov.nasa.worldwind.applications.gio.catalogui.CatalogKey;
import gov.nasa.worldwind.applications.gio.catalogui.ResultModel;
import gov.nasa.worldwind.wms.Capabilities;

/**
 * @author dcollins
 * @version $Id: GIDBResultModel.java 5517 2008-07-15 23:36:34Z dcollins $
 */
public class GIDBResultModel extends ResultModel
{
    private Server server;
    private Capabilities capabilities;
    private LayerList layerList;
    private CatalogExceptionList exceptionList;    

    public GIDBResultModel()
    {
    }

    public Server getServer()
    {
        return this.server;
    }

    public void setServer(Server server)
    {
        this.server = server;
    }

    public Capabilities getCapabilities()
    {
        return this.capabilities;
    }

    public void setCapabilities(Capabilities capabilities)
    {
        this.capabilities = capabilities;
    }

    public LayerList getLayerList()
    {
        return this.layerList;
    }

    public void setLayerList(LayerList layerList)
    {
        this.layerList = layerList;
    }

    public void addLayer(Layer layer)
    {
        if (this.layerList == null)
            this.layerList = new LayerListImpl();
        this.layerList.addLayer(layer);
    }

    public CatalogExceptionList getExceptionList()
    {
        return this.exceptionList;
    }

    public void setExceptionList(CatalogExceptionList exceptionList)
    {
        this.exceptionList = exceptionList;
    }

    public void addException(CatalogException e)
    {
        if (this.exceptionList == null)
        {
            this.exceptionList = new CatalogExceptionList();
            if (getValue(CatalogKey.EXCEPTIONS) == null)
                setValue(CatalogKey.EXCEPTIONS, this.exceptionList);
        }
        this.exceptionList.addException(e);
    }
}
