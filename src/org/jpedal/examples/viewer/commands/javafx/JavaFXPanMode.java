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
 * JavaFXPanMode.java
 * ---------------
 */
package org.jpedal.examples.viewer.commands.javafx;

import java.net.URL;
import javafx.scene.Cursor;
import org.jpedal.PdfDecoderFX;
import org.jpedal.PdfDecoderInt;
import org.jpedal.examples.viewer.Commands;
import org.jpedal.examples.viewer.MouseMode;
import org.jpedal.gui.GUIFactory;

/**
 * This Class Enables PanMode when activated via the top drop-down menu, When
 * PanMode is enabled, Snapshot and TextSelect functionality is disabled.
 */
public class JavaFXPanMode {

    public static void execute(final Object[] args, final GUIFactory currentGUI, final MouseMode mouseMode, final PdfDecoderInt decode_pdf) {
        if (args == null) {

            //Disable TextSelection & PanMode
            currentGUI.getMenuItems().setCheckMenuItemSelected(Commands.PANMODE, true);
            currentGUI.getMenuItems().setCheckMenuItemSelected(Commands.TEXTSELECT, false);
            currentGUI.getButtons().getButton(Commands.SNAPSHOT).setEnabled(false);
            currentGUI.setPannable(true);

            //Set Mouse Mode
            mouseMode.setMouseMode(MouseMode.MOUSE_MODE_PANNING);

            //Update buttons
            final URL url = currentGUI.getGUICursor().getURLForImage("mouse_pan.png");
            if (url != null) {
                currentGUI.getButtons().getButton(Commands.MOUSEMODE).setIcon(url);
            }

            //Update Cursor
            ((PdfDecoderFX) decode_pdf).setDefaultCursor(Cursor.MOVE);

        } else {

        }
    }

}
