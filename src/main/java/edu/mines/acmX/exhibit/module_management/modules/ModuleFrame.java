/**
 * Copyright (C) 2013 Colorado School of Mines
 *
 * This file is part of the Interface Software Development Kit (SDK).
 *
 * The InterfaceSDK is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The InterfaceSDK is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the InterfaceSDK.  If not, see <http://www.gnu.org/licenses/>.
 */
package edu.mines.acmX.exhibit.module_management.modules;

import processing.core.PApplet;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;

public class ModuleFrame extends JFrame {
    private volatile PApplet resident;

    public ModuleFrame(PApplet resident){
        super();
        GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
        setUndecorated(true);

        //resident.init();
        resident.frame = this;
        this.resident = resident;
        setVisible(true);

        setLayout(new BorderLayout());
        add(resident, BorderLayout.CENTER);

        Insets insets = getInsets();
        //setSize(env.getMaximumWindowBounds().getSize());
        setExtendedState(Frame.MAXIMIZED_BOTH);
        resident.setBounds(insets.left, insets.top, getWidth(), getHeight());
    }
}
