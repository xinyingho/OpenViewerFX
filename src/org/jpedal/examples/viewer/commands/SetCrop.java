/*
 * ===========================================
 * Java Pdf Extraction Decoding Access Library
 * ===========================================
 *
 * Project Info:  http://www.idrsolutions.com
 * Help section for developers at http://www.idrsolutions.com/support/
 *
 * (C) Copyright 1997-2015 IDRsolutions and Contributors.
 *
 * This file is part of JPedal/JPDF2HTML5
 *
     This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2.1 of the License, or (at your option) any later version.

    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA


 *
 * ---------------
 * SetCrop.java
 * ---------------
 */
package org.jpedal.examples.viewer.commands;

import java.awt.Container;
import javax.swing.JOptionPane;
import org.jpedal.PdfDecoderInt;
import org.jpedal.examples.viewer.Values;
import org.jpedal.examples.viewer.gui.generic.GUISearchWindow;
import org.jpedal.display.GUIThumbnailPanel;
import org.jpedal.examples.viewer.gui.popups.CropPDFPages;
import org.jpedal.examples.viewer.utils.ItextFunctions;
import org.jpedal.examples.viewer.utils.PropertiesFile;
import org.jpedal.gui.GUIFactory;
import org.jpedal.objects.PdfPageData;
import org.jpedal.utils.Messages;

/**
 * This class provides a method that will set the crop rectangle for 
 * the pages of your PDF document. It uses itext function setCrop.
 */
@SuppressWarnings({"UnusedAssignment","PMD"})
public class SetCrop {

    public static void execute(final Object[] args, final Values commonValues, final GUISearchWindow searchFrame,
            final GUIFactory currentGUI, final PdfDecoderInt decode_pdf, final PropertiesFile properties,
            final GUIThumbnailPanel thumbnails) {
        if (args == null) {
            if (commonValues.getSelectedFile() == null) {
                currentGUI.showMessageDialog(Messages.getMessage("PdfViewer.NoFile"));
            } else {
                //get values from user
                final CropPDFPages cropPage = new CropPDFPages(commonValues.getInputDir(), commonValues.getPageCount(), commonValues.getCurrentPage());
                final int cropPageChoice = cropPage.display((Container)currentGUI.getFrame(), Messages.getMessage("PdfViewerTooltip.PDFCropPages"));

                //get parameters and call if YES
                if (cropPageChoice == JOptionPane.OK_OPTION) {

                    final PdfPageData currentPageData = decode_pdf.getPdfPageData();

                    decode_pdf.closePdfFile();
                    final ItextFunctions itextFunctions = new ItextFunctions(currentGUI, commonValues.getSelectedFile(), decode_pdf);
                    ItextFunctions.setCrop(commonValues.getPageCount(), currentPageData, cropPage);
                    OpenFile.open(commonValues.getSelectedFile(), commonValues, searchFrame, currentGUI, decode_pdf, properties, thumbnails);
                }
            }
        } else {

        }
    }
}
