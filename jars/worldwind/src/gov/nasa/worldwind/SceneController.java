/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind;

import gov.nasa.worldwind.cache.TextureCache;
import gov.nasa.worldwind.pick.PickedObjectList;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.terrain.SectorGeometryList;
import gov.nasa.worldwind.util.PerformanceStatistic;

import java.util.*;

/**
 * @author Tom Gaskins
 * @version $Id: SceneController.java 13005 2010-01-14 00:30:41Z dcollins $
 */
public interface SceneController extends WWObject, Disposable
{
    public Model getModel();

    public void setModel(Model model);

    public View getView();

    public void setView(View view);

    public int repaint();

    void setVerticalExaggeration(double verticalExaggeration);

    double getVerticalExaggeration();

    PickedObjectList getPickedObjectList();

    double getFramesPerSecond();

    double getFrameTime();

    void setPickPoint(java.awt.Point pickPoint);

    java.awt.Point getPickPoint();

    void setTextureCache(TextureCache textureCache);

    Collection<PerformanceStatistic> getPerFrameStatistics();

    void setPerFrameStatisticsKeys(Set<String> keys);

    SectorGeometryList getTerrain();

    DrawContext getDrawContext();

    void reinitialize();

    ScreenCreditController getScreenCreditController();

    void setScreenCreditController(ScreenCreditController screenCreditRenderer);

    /**
     * Returns the {@link GLRuntimeCapabilities} associated with this SceneController.
     *
     * @return this SceneController's associated GLRuntimeCapabilities.
     */
    GLRuntimeCapabilities getGLRuntimeCapabilities();

    /**
     * Sets the {@link GLRuntimeCapabilities} associated with this SceneController to the specified parameter.
     *
     * @param capabilities the GLRuntimeCapabilities to be associated with this SceneController.
     *
     * @throws IllegalArgumentException if the capabilities are null.
     */
    void setGLRuntimeCapabilities(GLRuntimeCapabilities capabilities);
}
