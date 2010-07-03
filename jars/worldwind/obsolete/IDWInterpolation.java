/*
Copyright (C) 2001, 2008 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.examples.util;

import gov.nasa.worldwind.data.BufferWrapperRaster;
import gov.nasa.worldwind.util.Logging;

/**
 * Utility class for filling in missing values in elevations.
 * Uses Inverse Distance Weighted method for determining the elevation value of a missing value.
 *
 * Uses 8 neighboring cells:
 * 4 cardinal directions and 4 diagnal
 * If a neighboring cell is also missing an elevation value the algorithm extends up to the MAX_SEARCH_RADIUS
 * in the same direction.
 *
 * Note: Currently only requires one neighbor to determine a value, should there be a requirement for number of neighbors
 * with values?
 *
 * @author jparsons
 * @version $Id: IDWInterpolation.java 13001 2010-01-12 20:15:48Z dcollins $
 */
public class IDWInterpolation
{
    private static int MAX_SEARCH_RADIUS = 3; 
    public final static int MAX_NUM_NEIGHBORS = 8;
    public final static int MIN_NUM_NEIGHBORS = 1;
    private int minRequiredNeighbors = 4;

    public IDWInterpolation(int min)
    {
        setMinNumNeighbors(min);
    }

    public int getMinNumNeighbors()
    {
        return minRequiredNeighbors;
    }

    public void setMinNumNeighbors(int min)
    {
        if ((min < MIN_NUM_NEIGHBORS) || (min > MAX_NUM_NEIGHBORS))
        {
            String message = Logging.getMessage("generic.ArgumentOutOfRange",
                String.format("IDW number of neighbors must be >= %d and <= %d", MIN_NUM_NEIGHBORS, MAX_NUM_NEIGHBORS));
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.minRequiredNeighbors = min;
    }


    /**
     * Fill missing values in elevation rasters
     *
     * @param raster elevation raster with missing values
     * @param xPixelSize size of cell in X direction
     * @param yPixelSize size of cell in Y direction
     */
    public void fillVoids(BufferWrapperRaster raster, double xPixelSize, double yPixelSize)
    {

        if ( raster == null)
        {
            String msg = Logging.getMessage("nullValue.RasterIsNull");
            Logging.logger().log(java.util.logging.Level.SEVERE, msg);
            throw new IllegalArgumentException(msg);
        }

        boolean stillTrans=false;
        for (int col=0; col<raster.getWidth(); col++ )
        {
            for (int row=0; row<raster.getHeight(); row++)
            {
                if (raster.getDoubleAtPosition(row, col) == raster.getTransparentValue())
                {
                    double idw = calcIDW(row, col, raster, xPixelSize, yPixelSize);
                    if (idw == raster.getTransparentValue())
                        stillTrans = true;

                    raster.setDoubleAtPosition(row, col, idw);
                }
            }
         }

        if (stillTrans)  //if still some transparent values process again from opposite corner
        {
            for (int col= (raster.getWidth()-1); col > -1; col-- )
            {
                for (int row= (raster.getHeight() -1); row > -1; row--)
                {
                    if (raster.getDoubleAtPosition(row, col) == raster.getTransparentValue())
                    {
                        double idw = calcIDW(row, col, raster, xPixelSize, yPixelSize);
                        raster.setDoubleAtPosition(row, col, idw);
                    }
                }
             }
        }
    }

    private double calcIDW(int row, int col, BufferWrapperRaster raster, double xPixelSize, double yPixelSize)
    {
        double[][] idwValues = new double[8][2]; //eight neighboring values used in interpolation
        double zValue = raster.getTransparentValue();
        double weight = 0.0;
        int sourceRow = row;
        int sourceCol = col;

        //init values
        for (int i=0; i < idwValues.length; i++)
            idwValues[i][0] = raster.getTransparentValue();


        //NE cell
        for (int i=0; i < idwValues.length; i++)
        {
            int currentRadius=0;
            boolean valueFilled = false;

            do
            {
                currentRadius++;
                //set source cell
                switch(i){
                    case 0: sourceRow = row-currentRadius;  //NE source
                            sourceCol = col-currentRadius;
                            break;
                    case 1: sourceRow = row-currentRadius;  //N source
                            sourceCol = col;
                            break;
                    case 2: sourceRow = row-currentRadius;  //NW source
                            sourceCol = col+currentRadius;
                            break;
                    case 3: sourceRow = row;
                            sourceCol = col-currentRadius;  //West
                            break;
                    case 4: sourceRow = row;
                            sourceCol = col+currentRadius;  //East
                            break;
                    case 5: sourceRow = row+currentRadius;  //SE
                            sourceCol = col-currentRadius;
                            break;
                    case 6: sourceRow = row + currentRadius;//South
                            sourceCol = col;
                            break;
                    case 7: sourceRow = row+currentRadius;  //SW
                            sourceCol = col+currentRadius;
                            break;
                }

                if ((idwValues[i][0] == raster.getTransparentValue()) &&
                    (isValidCell(sourceRow,sourceCol,raster.getHeight(), raster.getWidth()))  &&
                    (raster.getDoubleAtPosition(sourceRow,sourceCol) != raster.getTransparentValue()))
                {
                    idwValues[i][0] = raster.getDoubleAtPosition(sourceRow,sourceCol);
                    idwValues[i][1] = 1 / calcDistanceBetweenCells(row, col, sourceRow, sourceCol, xPixelSize, yPixelSize);
                    weight += idwValues[i][1];
                    valueFilled=true;
                }
            }while((!valueFilled) && (currentRadius < MAX_SEARCH_RADIUS) );
        }

        //require a minimum number of inputs?  one is enough now
        int count =0;
        for (int i=0; i< idwValues.length; i++)
        {
            if (idwValues[i][0] != raster.getTransparentValue())
            {
                count++;
                if (zValue == raster.getTransparentValue()) //init value to 0.0 with first good idwValue
                    zValue = 0.0d;

                idwValues[i][1] = idwValues[i][1] / weight;
                zValue += idwValues[i][0] * idwValues[i][1];
            }
        }

        if (count >= minRequiredNeighbors)
            return zValue;
        else
            return raster.getTransparentValue();
    }

    private boolean isValidCell(int row, int col, int maxRows, int maxCols)
    {
        if ( (row < 0) || (row > (maxRows-1)))
            return false;

        if ( (col < 0) || (col > (maxCols-1)))
            return false;

        return true;
    }

    private static double calcDistanceBetweenCells(int x1, int y1, int x2, int y2, double xPixelSize, double yPixelSize)
    {
        double xDistance = (x2-x1) * xPixelSize;
        double yDistance = (y2-y1) * yPixelSize;
        return Math.sqrt( (xDistance*xDistance) + (yDistance*yDistance));
    }
}
