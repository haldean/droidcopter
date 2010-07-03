/* Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package WorldWindHackApps.elevationviewer;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.data.*;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.util.Level;

import javax.swing.*;
import java.awt.event.*;
import java.io.File;

/**
 * @author dcollins
 * @version $Id: AppController.java 13023 2010-01-21 00:18:48Z dcollins $
 */
public class AppController implements ActionListener
{
    public static final String OPEN_ELEVATIONS = "OpenElevations";
    public static final String SET_VISIBLE_LEVEL = "SetVisibleLevel";

    private ElevationViewerApp.AppFrame appFrame;
    private TiledElevationMesh currentMesh;
    private DataDescriptorReader dataDescriptorReader;
    private JFileChooser openFileChooser;

    public AppController(ElevationViewerApp.AppFrame appFrame)
    {
        this.appFrame = appFrame;
        this.dataDescriptorReader = new BasicDataDescriptorReader();
    }

    public void actionPerformed(ActionEvent actionEvent)
    {
        String command = actionEvent.getActionCommand();
        if (command == null)
            return;

        if (command.equals(OPEN_ELEVATIONS))
        {
            this.onOpenElevations();   
        }
        else if (command.equals(SET_VISIBLE_LEVEL))
        {
            this.onSetVisibleLevel();
        }
    }

    protected void onOpenElevations()
    {
        if (this.openFileChooser == null)
        {
            this.openFileChooser = new JFileChooser();
            this.openFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            this.openFileChooser.setMultiSelectionEnabled(false);
            this.openFileChooser.addChoosableFileFilter(new ElevationsFileFilter(this.dataDescriptorReader));
        }

        int returnVal = this.openFileChooser.showOpenDialog(null);
        if (returnVal != JFileChooser.APPROVE_OPTION)
            return;

        File selected = this.openFileChooser.getSelectedFile();
        DataDescriptor dataDescriptor;
        this.dataDescriptorReader.setSource(selected);
        try
        {
            File dir = selected.getParentFile();
            dataDescriptor = this.dataDescriptorReader.read();
            dataDescriptor.setFileStoreLocation(dir.getParentFile());
            dataDescriptor.setFileStorePath(dir.getName());
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return;
        }
        finally
        {
            this.dataDescriptorReader.setSource(null);
        }

        MeshCoords coords = TiledElevationMesh.createMeshCoords((Sector) dataDescriptor.getValue(AVKey.SECTOR));
        this.currentMesh = new TiledElevationMesh(dataDescriptor, coords);

        this.appFrame.getGLController().getScene().clearElements();
        this.appFrame.getGLController().getScene().addElement(this.currentMesh);

        this.appFrame.getInputPanel().getCurrentElevationsTextField().setText(selected.getPath());
        this.appFrame.getInputPanel().populateLevelBox(this.currentMesh.getLevelSet());

        this.appFrame.getGLController().getCameraController().lookAt(this.appFrame.getGLController().getCamera(), coords);
    }

    protected void onSetVisibleLevel()
    {
        Object selected = this.appFrame.getInputPanel().getLevelBox().getSelectedItem();
        if (selected == null || !(selected instanceof Level))
            return;

        Level level = (Level) selected;
        this.currentMesh.setVisibleLevel(level.getLevelNumber());
    }

    private static class ElevationsFileFilter extends javax.swing.filechooser.FileFilter implements java.io.FileFilter 
    {
        private DataDescriptorReader reader;

        private ElevationsFileFilter(DataDescriptorReader reader)
        {
            this.reader = reader;
        }

        public boolean accept(File file)
        {
            if (file.isDirectory())
                return true;

            this.reader.setSource(file);
            try
            {
                return this.reader.canRead();
            }
            catch (Exception e)
            {
                e.printStackTrace();
                return false;
            }
            finally
            {
                this.reader.setSource(null);
            }
        }

        public String getDescription()
        {
            return "Tiled Elevations";
        }
    }
}
