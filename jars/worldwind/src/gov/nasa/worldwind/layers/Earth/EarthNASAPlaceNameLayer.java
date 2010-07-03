/*
Copyright (C) 2001, 2006 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.layers.Earth;

import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.layers.placename.*;
import gov.nasa.worldwind.util.Logging;
import java.util.GregorianCalendar;

/**
 * @author Paul Collins
 * @version $Id: EarthNASAPlaceNameLayer.java 5322 2008-05-16 14:08:38Z jparsons $
 */
public class EarthNASAPlaceNameLayer extends PlaceNameLayer
{
    
    public EarthNASAPlaceNameLayer()
    {
        super(makePlaceNameServiceSet());
    }

    private static PlaceNameServiceSet makePlaceNameServiceSet()
    {
        //final String service = "http://builds.worldwind.arc.nasa.gov/geoserver/wfs";
        final String service = "http://worldwind25.arc.nasa.gov/geoservercache/geoservercache.aspx";
        final String fileCachePath = "Earth/PlaceNames/EarthPlaceNames";
        final boolean addVersionTag=false;  //true if pointing to a new wfs server

        PlaceNameServiceSet placeNameServiceSet = new PlaceNameServiceSet();
        placeNameServiceSet.setExpiryTime(new GregorianCalendar(2008, 1, 11).getTimeInMillis());
        
        // Oceans
        PlaceNameService placeNameService = new PlaceNameService(service, "topp:wpl_oceans", fileCachePath, Sector.FULL_SPHERE, GRID_1x1,
            java.awt.Font.decode("Arial-BOLDITALIC-12"), addVersionTag);
        placeNameService.setColor(new java.awt.Color(200, 200, 200));
        placeNameService.setMinDisplayDistance(0d);
        placeNameService.setMaxDisplayDistance(LEVEL_A);
        placeNameServiceSet.addService(placeNameService, false);
        // Continents
        placeNameService = new PlaceNameService(service, "topp:wpl_continents", fileCachePath, Sector.FULL_SPHERE,
            GRID_1x1, java.awt.Font.decode("Arial-BOLD-12"), addVersionTag);
        placeNameService.setColor(new java.awt.Color(255, 255, 240));
        placeNameService.setMinDisplayDistance(LEVEL_G);
        placeNameService.setMaxDisplayDistance(LEVEL_A);
        placeNameServiceSet.addService(placeNameService, false);

        // Water Bodies
        placeNameService = new PlaceNameService(service, "topp:wpl_waterbodies", fileCachePath, Sector.FULL_SPHERE,
            GRID_4x8, java.awt.Font.decode("Arial-ITALIC-10"), addVersionTag);
        placeNameService.setColor(java.awt.Color.cyan);
        placeNameService.setMinDisplayDistance(0d);
        placeNameService.setMaxDisplayDistance(LEVEL_B);
        placeNameServiceSet.addService(placeNameService, false);
        // Trenches & Ridges
        placeNameService = new PlaceNameService(service, "topp:wpl_trenchesridges", fileCachePath, Sector.FULL_SPHERE,
            GRID_4x8, java.awt.Font.decode("Arial-BOLDITALIC-10"), addVersionTag);
        placeNameService.setColor(java.awt.Color.cyan);
        placeNameService.setMinDisplayDistance(0d);
        placeNameService.setMaxDisplayDistance(LEVEL_B);
        placeNameServiceSet.addService(placeNameService, false);
        // Deserts & Plains
        placeNameService = new PlaceNameService(service, "topp:wpl_desertsplains", fileCachePath, Sector.FULL_SPHERE,
            GRID_4x8, java.awt.Font.decode("Arial-BOLDITALIC-10"), addVersionTag);
        placeNameService.setColor(java.awt.Color.orange);
        placeNameService.setMinDisplayDistance(0d);
        placeNameService.setMaxDisplayDistance(LEVEL_B);
        placeNameServiceSet.addService(placeNameService, false);

        // Lakes & Rivers
        placeNameService = new PlaceNameService(service, "topp:wpl_lakesrivers", fileCachePath, Sector.FULL_SPHERE,
            GRID_8x16, java.awt.Font.decode("Arial-ITALIC-10"), addVersionTag);
        placeNameService.setColor(java.awt.Color.cyan);
        placeNameService.setMinDisplayDistance(0d);
        placeNameService.setMaxDisplayDistance(LEVEL_C);
        placeNameServiceSet.addService(placeNameService, false);
        // Mountains & Valleys
        placeNameService = new PlaceNameService(service, "topp:wpl_mountainsvalleys", fileCachePath, Sector.FULL_SPHERE,
            GRID_8x16, java.awt.Font.decode("Arial-BOLDITALIC-10"), addVersionTag);
        placeNameService.setColor(java.awt.Color.orange);
        placeNameService.setMinDisplayDistance(0d);
        placeNameService.setMaxDisplayDistance(LEVEL_C);
        placeNameServiceSet.addService(placeNameService, false);

        // Countries
        placeNameService = new PlaceNameService(service, "topp:countries", fileCachePath, Sector.FULL_SPHERE, GRID_4x8,
            java.awt.Font.decode("Arial-BOLD-10"), addVersionTag);
        placeNameService.setColor(java.awt.Color.white);
        placeNameService.setMinDisplayDistance(LEVEL_G);
        placeNameService.setMaxDisplayDistance(LEVEL_D);
        placeNameServiceSet.addService(placeNameService, false);
        // GeoNet World Capitals
        placeNameService = new PlaceNameService(service, "topp:wpl_geonet_p_pplc", fileCachePath, Sector.FULL_SPHERE,
            GRID_8x16, java.awt.Font.decode("Arial-BOLD-10"), addVersionTag);
        placeNameService.setColor(java.awt.Color.yellow);
        placeNameService.setMinDisplayDistance(0d);
        placeNameService.setMaxDisplayDistance(LEVEL_D);
        placeNameServiceSet.addService(placeNameService, false);
        // US Cities (Population Over 500k)
        placeNameService = new PlaceNameService(service, "topp:wpl_uscitiesover500k", fileCachePath, Sector.FULL_SPHERE,
            GRID_8x16, java.awt.Font.decode("Arial-BOLD-10"), addVersionTag);
        placeNameService.setColor(java.awt.Color.yellow);
        placeNameService.setMinDisplayDistance(0d);
        placeNameService.setMaxDisplayDistance(LEVEL_D);
        placeNameServiceSet.addService(placeNameService, false);

        // US Cities (Population Over 100k)
        placeNameService = new PlaceNameService(service, "topp:wpl_uscitiesover100k", fileCachePath, Sector.FULL_SPHERE,
            GRID_8x16, java.awt.Font.decode("Arial-PLAIN-10"), addVersionTag);
        placeNameService.setColor(java.awt.Color.yellow);
        placeNameService.setMinDisplayDistance(0d);
        placeNameService.setMaxDisplayDistance(LEVEL_F);
        placeNameServiceSet.addService(placeNameService, false);

        // US Cities (Population Over 50k)
        placeNameService = new PlaceNameService(service, "topp:wpl_uscitiesover50k", fileCachePath, Sector.FULL_SPHERE,
            GRID_8x16, java.awt.Font.decode("Arial-PLAIN-10"), addVersionTag);
        placeNameService.setColor(java.awt.Color.yellow);
        placeNameService.setMinDisplayDistance(0d);
        placeNameService.setMaxDisplayDistance(LEVEL_I);
        placeNameServiceSet.addService(placeNameService, false);

        // US Cities (Population Over 10k)
        placeNameService = new PlaceNameService(service, "topp:wpl_uscitiesover10k", fileCachePath, Sector.FULL_SPHERE,
            GRID_8x16, java.awt.Font.decode("Arial-PLAIN-10"), addVersionTag);
        placeNameService.setColor(java.awt.Color.yellow);
        placeNameService.setMinDisplayDistance(0d);
        placeNameService.setMaxDisplayDistance(LEVEL_J);
        placeNameServiceSet.addService(placeNameService, false);

        // US Cities (Population Over 1k)
        placeNameService = new PlaceNameService(service, "topp:wpl_uscitiesover1k", fileCachePath, Sector.FULL_SPHERE,
            GRID_16x32, java.awt.Font.decode("Arial-PLAIN-10"), addVersionTag);
        placeNameService.setColor(java.awt.Color.yellow);
        placeNameService.setMinDisplayDistance(0d);
        placeNameService.setMaxDisplayDistance(LEVEL_K);
        placeNameServiceSet.addService(placeNameService, false);

        // US Cities (Population Over 0)
        placeNameService = new PlaceNameService(service, "topp:wpl_uscitiesover0", fileCachePath, Sector.FULL_SPHERE,
            GRID_16x32, java.awt.Font.decode("Arial-PLAIN-10"), addVersionTag);
        placeNameService.setColor(java.awt.Color.yellow);
        placeNameService.setMinDisplayDistance(0d);
        placeNameService.setMaxDisplayDistance(LEVEL_L);
        placeNameServiceSet.addService(placeNameService, false);

        // US Cities (No Population)
        placeNameService = new PlaceNameService(service, "topp:wpl_uscities0", fileCachePath, Sector.FULL_SPHERE,
            GRID_288x576, java.awt.Font.decode("Arial-PLAIN-10"), addVersionTag);
        placeNameService.setColor(java.awt.Color.yellow);
        placeNameService.setMinDisplayDistance(0d);
        placeNameService.setMaxDisplayDistance(LEVEL_M);
        placeNameServiceSet.addService(placeNameService, false);

        return placeNameServiceSet;
    }

    @Override
    public String toString()
    {
        return Logging.getMessage("layers.Earth.PlaceName.Name");
    }
}
