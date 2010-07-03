/*
Copyright (C) 2001, 2008 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.data.BILRasterReader;
import gov.nasa.worldwind.data.BILRasterWriter;
import gov.nasa.worldwind.data.BasicDataSource;
import gov.nasa.worldwind.data.BufferWrapperRaster;
import gov.nasa.worldwind.data.DataRaster;
import gov.nasa.worldwind.data.DataSource;
import gov.nasa.worldwind.data.ReadableDataRaster;
import gov.nasa.worldwind.data.TiledElevationProducer;
import gov.nasa.worldwind.examples.util.IDWInterpolation;
import gov.nasa.worldwind.util.Logging;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;

/**
 * Example application to demostrate filling in missing elevation values.
 * User selects a source and destination .bil file
 *
 * @author jparsons
 * @version $Id: ElevationFill.java 13001 2010-01-12 20:15:48Z dcollins $
 */
public class ElevationFill extends JPanel implements ActionListener
{
    private JFileChooser fileChooser;
    private JButton sourceButton, targetButton, fillButton;
    private File sourceFile, targetFile;
    private JTextArea info;
    private JComboBox numRequiredBox;

    public ElevationFill()
    {
        super(new BorderLayout());
        initUIComponents();
    }

    public void actionPerformed(ActionEvent e)
    {
        if (e.getSource() == sourceButton)
        {
            int returnVal = fileChooser.showOpenDialog(ElevationFill.this);
            if (returnVal == JFileChooser.APPROVE_OPTION)
            {
                sourceFile = fileChooser.getSelectedFile();
                buildInfoMessage();
            }else
            {
                sourceFile = null;
            }

        }else if (e.getSource() == targetButton)
        {
            int returnVal = fileChooser.showSaveDialog(ElevationFill.this);
            if (returnVal == JFileChooser.APPROVE_OPTION)
            {
                targetFile = fileChooser.getSelectedFile(); 
                if (targetFile.exists())
                {
                    if (showOverwritePrompt(this.fileChooser, targetFile) != 0)
                        targetFile = null;
                }
                buildInfoMessage();
            }
            else
            {
                targetFile = null;
            }
        }else if (e.getSource() == fillButton)
        {
            this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            fillElevation();
            buildInfoMessage("Fill Completed ..."); //note: could add stats on number of cells filled or left empty
            this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }


        if ((sourceFile != null) && (targetFile != null))
            fillButton.setEnabled(true);
        else
            fillButton.setEnabled(false);
    }

    private void buildInfoMessage()
    {
        buildInfoMessage(null);    
    }

    private void buildInfoMessage(String msg)
    {
        StringBuffer sb = new StringBuffer();
        if (sourceFile != null)
        {
            sb.append("Source file: ");
            sb.append(sourceFile.getAbsolutePath());
            sb.append("\n");
        }
        if (targetFile != null)
        {
            sb.append("Target file: ");
            sb.append(targetFile.getAbsolutePath());
            sb.append("\n");

        }

        if (msg != null)
            sb.append(msg);

        info.setText(sb.toString());
    }


    private void fillElevation()
    {
        DataSource source = new BasicDataSource(sourceFile);
        BILRasterWriter writer = new BILRasterWriter();
        ReadableDataRaster reader;
        try
        {
            reader = new ReadableDataRaster(source, new BILRasterReader() );
            //now fill the missing values
            DataRaster[] rasters = reader.getReader().read(source);
            if (rasters.length > 0)
            {
                Object oXSize = source.getValue(gov.nasa.worldwind.formats.worldfile.WorldFile.WORLD_FILE_X_PIXEL_SIZE);
                Object oYSize = source.getValue(gov.nasa.worldwind.formats.worldfile.WorldFile.WORLD_FILE_Y_PIXEL_SIZE);
                Double xPixelSize = 1.0;
                Double yPixelSize = 1.0;
                //need both values, else assume square cells and use 1.0
                if ((oXSize != null && oXSize instanceof Double) && (oYSize != null && oYSize instanceof Double))
                {
                    xPixelSize = (Double) oXSize;
                    yPixelSize = (Double) oYSize;
                }

                //get value from combobox
                int num = Integer.valueOf(numRequiredBox.getSelectedItem().toString());
                IDWInterpolation idw = new IDWInterpolation(num);
                idw.fillVoids((BufferWrapperRaster) rasters[0], xPixelSize, yPixelSize);
                writer.write(rasters[0], "bil", targetFile);
            }
        }catch (IOException ex)
        {
            //exception reading/writing file
            Logging.logger().log(java.util.logging.Level.SEVERE, ex.getMessage());
        }
    }

    private void initUIComponents()
    {
        info = new JTextArea(5,60);
        info.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(info);

        fileChooser = new JFileChooser();
        //set file filter
        fileChooser.setFileFilter(new InstalledData.TiledRasterProducerFilter(
            TiledElevationProducer.class));

        sourceButton = new JButton("Select an elevation file to fill...");
        sourceButton.addActionListener(this);
        targetButton = new JButton("Select a file to save filled elevation...");
        targetButton.addActionListener(this);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(sourceButton);
        buttonPanel.add(targetButton);

        JPanel optPanel = new JPanel();
        optPanel.add(new JLabel("The number of input cells required to determine a value:"));

        //Populate dropdown with range from IDWInterpolation
        String[] selectionValues = new String[IDWInterpolation.MAX_NUM_NEIGHBORS];
        for (int i=IDWInterpolation.MIN_NUM_NEIGHBORS; i<IDWInterpolation.MAX_NUM_NEIGHBORS+1; i++)
            selectionValues[i-1] = String.valueOf(i);

        numRequiredBox = new JComboBox(selectionValues);
        optPanel.add(numRequiredBox);
        
        buttonPanel.add(optPanel);

        JPanel controlPanel = new JPanel(new GridLayout(2,1));
        controlPanel.add(buttonPanel);
        controlPanel.add(optPanel);
        //add(optionsPanel, BorderLayout.NORTH);
        add(controlPanel, BorderLayout.PAGE_START);
        add(logScrollPane, BorderLayout.CENTER);

        fillButton = new JButton("Fill missing data...");
        fillButton.addActionListener(this);
        fillButton.setEnabled(false);
        add(fillButton, BorderLayout.PAGE_END);
    }

    private static int showOverwritePrompt(Component parent, File file)
    {
        String message;
        if (file != null)
            message = String.format("Overwrite existing file\n\"%s\"?", file.getPath());
        else
            message = "Overwrite existing file?";


        return JOptionPane.showOptionDialog(
                parent,
                message,
                "Save File",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                new Object[] {"Overwrite", "Cancel"},
                "Overwrite");
    }

    private void showGUI()
    {
        JFrame frame = new JFrame("Elevation Fill Example");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(this);
        frame.pack();
        frame.setVisible(true);
    }

    public static void main(String[] args)
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run() {
                new ElevationFill().showGUI();
            }
        });
    }
}
