//
//  Java Graphics Device
//
//  Created by Simon Urbanek on Thu Aug 05 2004.
//  Copyright (c) 2004-2009 Simon Urbanek. All rights reserved.
//
//  This library is free software; you can redistribute it and/or
//  modify it under the terms of the GNU Lesser General Public
//  License as published by the Free Software Foundation;
//  version 2.1 of the License.
//
//  This library is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
//  Lesser General Public License for more details.
//
//  You should have received a copy of the GNU Lesser General Public
//  License along with this library; if not, write to the Free Software
//  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
//
package org.rosuda.javaGD;

import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.function.Consumer;

import javax.swing.JFrame;

/**
 * JavaGD is an implementation of the {@link GDInterface} protocol which displays the R graphics in
 * an AWT window (via {@link GDCanvas}). It can be used as an example for implementing custom
 * display classes which can then be used by JavaGD. Three sample back-ends are included in the
 * JavaGD sources: {@link GDCanvas} (AWT), {@link JGDPanel} (Swing) and {@link JGDBufferedPanel}
 * (Swing with cached update).
 */
public class JavaGD extends GDInterface implements WindowListener {
    /** frame containing the graphics canvas */
    public Frame f;

    private final Consumer<Integer> resizer;
    private final Consumer<Integer> devOffCall;

    /**
     * default, public constructor - creates a new JavaGD instance. The actual window (and canvas)
     * is not created until {@link GDInterface#gdOpen} is called.
     */
    public JavaGD(Consumer<Integer> resizer, Consumer<Integer> devOffCall) {
        super();
        this.resizer = resizer;
        this.devOffCall = devOffCall;
    }

    /**
     * creates a new graphics window containing a canvas
     *
     * @param w width of the canvas
     * @param h height of the canvas
     */
    @Override
    public void gdOpen(double w, double h) {
        if (f != null)
            gdClose();

        f = new JFrame("JavaGD");
        f.addWindowListener(this);
        c = new JGDBufferedPanel(w, h, resizer);
        f.add((JGDPanel) c);
        f.pack();
        f.setVisible(true);
    }

    @Override
    public void gdActivate() {
        super.gdActivate();
        if (f != null) {
            f.requestFocus();
            f.setTitle("JavaGD " + ((devNr > 0) ? ("(" + (devNr + 1) + ")") : "") + " *active*");
        }
    }

    @Override
    public void gdClose() {
        super.gdClose();
        if (f != null) {
            c = null;
            f.removeAll();
            f.dispose();
            f = null;
        }
    }

    @Override
    public void gdDeactivate() {
        super.gdDeactivate();
        if (f != null)
            f.setTitle("JavaGD " + ((devNr > 0) ? ("(" + (devNr + 1) + ")") : ""));
    }

    @Override
    public void gdNewPage(int deviceNr, int pageNumber) { // new API: provides the device Nr.
        super.gdNewPage(deviceNr, pageNumber);
        if (f != null)
            f.setTitle("JavaGD (" + (deviceNr + 1) + ")" + (active ? " *active*" : ""));
    }

    /*-- WindowListener interface methods */

    /** listener response to "Close" - effectively invokes <code>dev.off()</code> on the device */
    @Override
    public void windowClosing(WindowEvent e) {
        if (c != null)
            executeDevOff();
    }

    @Override
    public void windowClosed(WindowEvent e) {
    }

    @Override
    public void windowOpened(WindowEvent e) {
    }

    @Override
    public void windowIconified(WindowEvent e) {
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
    }

    @Override
    public void windowActivated(WindowEvent e) {
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
    }

    /** close the device in R associated with this instance */
    public void executeDevOff() {
        if (c == null || c.getDeviceNumber() < 0)
            return;
        devOffCall.accept(c.getDeviceNumber() + 1);
    }
}
