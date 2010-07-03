/*
Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.applications.gio.esg;

import gov.nasa.worldwind.applications.gio.catalogui.CatalogException;
import gov.nasa.worldwind.applications.gio.catalogui.CatalogExceptionList;
import gov.nasa.worldwind.applications.gio.catalogui.CatalogKey;
import gov.nasa.worldwind.applications.gio.catalogui.ResultModel;
import gov.nasa.worldwind.wms.Capabilities;

/**
 * @author dcollins
 * @version $Id: ESGResultModel.java 5517 2008-07-15 23:36:34Z dcollins $
 */
public class ESGResultModel extends ResultModel
{
    private ServicePackage servicePackage;
    private Capabilities capabilities;
    private CatalogExceptionList exceptionList;

    public ESGResultModel()
    {
    }

    public ServicePackage getServicePackage()
    {
        return this.servicePackage;
    }

    public void setServicePackage(ServicePackage servicePackage)
    {
        this.servicePackage = servicePackage;
    }

    public Capabilities getCapabilities()
    {
        return this.capabilities;
    }

    public void setCapabilities(Capabilities capabilities)
    {
        this.capabilities = capabilities;
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
