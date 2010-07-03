/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.layers;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.cache.FileStore;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.util.Logging;

import java.beans.PropertyChangeEvent;

/**
 * @author tag
 * @version $Id: AbstractLayer.java 12535 2009-08-31 21:12:57Z tgaskins $
 */
public abstract class AbstractLayer extends WWObjectImpl implements Layer
{
    private boolean enabled = true;
    private boolean pickable = true;
    private double opacity = 1d;
    private double minActiveAltitude = -Double.MAX_VALUE;
    private double maxActiveAltitude = Double.MAX_VALUE;
    private boolean networkDownloadEnabled = true;
    private long expiryTime = 0;
    private ScreenCredit screenCredit = null;
    private FileStore dataFileStore = WorldWind.getDataFileStore();

    public boolean isEnabled()
    {
        return this.enabled;
    }

    public boolean isPickEnabled()
    {
        return pickable;
    }

    public void setPickEnabled(boolean pickable)
    {
        this.pickable = pickable;
    }

    public void setEnabled(boolean enabled)
    {
        Boolean oldEnabled = this.enabled;
        this.enabled = enabled;
        this.propertyChange(new PropertyChangeEvent(this, "Enabled", oldEnabled, this.enabled));
    }

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

    public double getOpacity()
    {
        return opacity;
    }

    public void setOpacity(double opacity)
    {
        this.opacity = opacity;
    }

    public double getMinActiveAltitude()
    {
        return minActiveAltitude;
    }

    public void setMinActiveAltitude(double minActiveAltitude)
    {
        this.minActiveAltitude = minActiveAltitude;
    }

    public double getMaxActiveAltitude()
    {
        return maxActiveAltitude;
    }

    public void setMaxActiveAltitude(double maxActiveAltitude)
    {
        this.maxActiveAltitude = maxActiveAltitude;
    }

    public double getScale()
    {
        Object o = this.getValue(AVKey.MAP_SCALE);
        return o != null && o instanceof Double ? (Double) o : 1;
    }

    public boolean isNetworkRetrievalEnabled()
    {
        return networkDownloadEnabled;
    }

    public void setNetworkRetrievalEnabled(boolean networkDownloadEnabled)
    {
        this.networkDownloadEnabled = networkDownloadEnabled;
    }

    public FileStore getDataFileStore()
    {
        return this.dataFileStore;
    }

    public void setDataFileStore(FileStore fileStore)
    {
        if (fileStore == null)
        {
            String message = Logging.getMessage("nullValue.FileStoreIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        this.dataFileStore = fileStore;
    }

    /**
     * Indicates whether the layer is in the view. The method implemented here is a default indicating the layer is in
     * view. Subclasses able to determine their presence in the view should override this implementation.
     *
     * @param dc the current draw context
     * @return <code>true</code> if the layer is in the view, <code>false</code> otherwise.
     */
    public boolean isLayerInView(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        return true;
    }

    /**
     * Indicates whether the layer is active based on arbitrary criteria. The method implemented here is a default
     * indicating the layer is active if the current altitude is within the layer's min and max active altitudes.
     * Subclasses able to consider more criteria should override this implementation.
     *
     * @param dc the current draw context
     * @return <code>true</code> if the layer is active, <code>false</code> otherwise.
     */
    public boolean isLayerActive(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        if (null == dc.getView())
        {
            String message = Logging.getMessage("layers.AbstractLayer.NoViewSpecifiedInDrawingContext");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        Position eyePos = dc.getView().getEyePosition();
        if (eyePos == null)
            return false;

        double altitude = eyePos.getElevation();
        return altitude >= this.minActiveAltitude && altitude <= this.maxActiveAltitude;
    }

    public void preRender(DrawContext dc)
    {
        if (!this.enabled)
            return; // Don't check for arg errors if we're disabled

        if (null == dc)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        if (null == dc.getGlobe())
        {
            String message = Logging.getMessage("layers.AbstractLayer.NoGlobeSpecifiedInDrawingContext");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        if (null == dc.getView())
        {
            String message = Logging.getMessage("layers.AbstractLayer.NoViewSpecifiedInDrawingContext");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        if (!this.isLayerActive(dc))
            return;

        if (!this.isLayerInView(dc))
            return;

        this.doPreRender(dc);
    }

    /**
     * @param dc the current draw context
     * @throws IllegalArgumentException if <code>dc</code> is null, or <code>dc</code>'s <code>Globe</code> or
     *                                  <code>View</code> is null
     */
    public void render(DrawContext dc)
    {
        if (!this.enabled)
            return; // Don't check for arg errors if we're disabled

        if (null == dc)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        if (null == dc.getGlobe())
        {
            String message = Logging.getMessage("layers.AbstractLayer.NoGlobeSpecifiedInDrawingContext");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        if (null == dc.getView())
        {
            String message = Logging.getMessage("layers.AbstractLayer.NoViewSpecifiedInDrawingContext");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        if (!this.isLayerActive(dc))
            return;

        if (!this.isLayerInView(dc))
            return;

        this.doRender(dc);
    }

    public void pick(DrawContext dc, java.awt.Point point)
    {
        if (!this.enabled)
            return; // Don't check for arg errors if we're disabled

        if (null == dc)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        if (null == dc.getGlobe())
        {
            String message = Logging.getMessage("layers.AbstractLayer.NoGlobeSpecifiedInDrawingContext");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        if (null == dc.getView())
        {
            String message = Logging.getMessage("layers.AbstractLayer.NoViewSpecifiedInDrawingContext");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        if (!this.isLayerActive(dc))
            return;

        if (!this.isLayerInView(dc))
            return;

        this.doPick(dc, point);
    }

    protected void doPick(DrawContext dc, java.awt.Point point)
    {
        // any state that could change the color needs to be disabled, such as GL_TEXTURE, GL_LIGHTING or GL_FOG.
        // re-draw with unique colors
        // store the object info in the selectable objects table
        // read the color under the coursor
        // use the color code as a key to retrieve a selected object from the selectable objects table
        // create an instance of the PickedObject and add to the dc via the dc.addPickedObject() method
    }

    public void dispose() // override if disposal is a supported operation
    {
    }

    protected void doPreRender(DrawContext dc)
    {
    }

    protected abstract void doRender(DrawContext dc);

    public  boolean isAtMaxResolution()
    {
        return !this.isMultiResolution();
    }

    public boolean isMultiResolution()
    {
        return false;
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

    public void setExpiryTime(long expiryTime)
    {
        this.expiryTime = expiryTime;
    }

    public long getExpiryTime()
    {
        return this.expiryTime;
    }

    protected ScreenCredit getScreenCredit()
    {
        return screenCredit;
    }

    protected void setScreenCredit(ScreenCredit screenCredit)
    {
        this.screenCredit = screenCredit;
    }
}
