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

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Collection;
import java.util.Vector;
import java.util.function.Consumer;

public class GDCanvas extends Canvas implements GDContainer, MouseListener {
    private static final long serialVersionUID = 1L;

    Vector<GDObject> l;

    boolean listChanged;

    public static boolean forceAntiAliasing = true;

    GDState gs;

    Refresher r;

    Dimension lastSize;

    public int devNr = -1;

    private final Consumer<Integer> resizer;

    public GDCanvas(double w, double h, Consumer<Integer> resizer) {
        this((int) w, (int) h, resizer);
    }

    public GDCanvas(int w, int h, Consumer<Integer> resizer) {
        l = new Vector<>();
        gs = new GDState();
        gs.f = new Font(null, 0, 12);
        setSize(w, h);
        lastSize = getSize();
        setBackground(Color.white);
        addMouseListener(this);
        this.resizer = resizer;

        (r = new Refresher(this)).start();
    }

    @Override
    public GDState getGState() {
        return gs;
    }

    @Override
    public void setDeviceNumber(int dn) {
        devNr = dn;
    }

    @Override
    public int getDeviceNumber() {
        return devNr;
    }

    @Override
    public void closeDisplay() {
    }

    @Override
    public void syncDisplay(boolean finish) {
        repaint();
    }

    public void initRefresh() {
        resizer.accept(devNr);
    }

    @Override
    public synchronized void add(GDObject o) {
        l.add(o);
        listChanged = true;
    }

    @Override
    public Collection<GDObject> getGDObjects() {
        return l;
    }

    @Override
    public synchronized void reset(int pageNumber) {
        l.removeAllElements();
        listChanged = true;
    }

    LocatorSync lsCallback = null;

    @Override
    public synchronized boolean prepareLocator(LocatorSync ls) {
        if (lsCallback != null && lsCallback != ls) // make sure we cause no deadlock
            lsCallback.triggerAction(null);
        lsCallback = ls;

        return true;
    }

    // MouseListener for the Locator support
    @Override
    public void mouseClicked(MouseEvent e) {
        if (lsCallback != null) {
            double[] pos = null;
            if ((e.getModifiersEx() & InputEvent.BUTTON1_DOWN_MASK) > 0 &&
                    (e.getModifiersEx() & (InputEvent.BUTTON2_DOWN_MASK | InputEvent.BUTTON3_DOWN_MASK)) == 0) { // B1
                                                                                                                 // =
                                                                                                                 // return
                                                                                                                 // position
                pos = new double[2];
                pos[0] = e.getX();
                pos[1] = e.getY();
            }

            // pure security measure to make sure the trigger doesn't mess with the locator sync
            // object
            LocatorSync ls = lsCallback;
            lsCallback = null; // reset the callback - we'll get a new one if necessary
            ls.triggerAction(pos);
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    long lastUpdate;
    long lastUpdateFinished;
    boolean updatePending = false;

    @Override
    public void update(Graphics g) {
        if (System.currentTimeMillis() - lastUpdate < 200) {
            updatePending = true;
            if (System.currentTimeMillis() - lastUpdateFinished > 700) {
                g.setColor(Color.white);
                g.fillRect(0, 0, 250, 25);
                g.setColor(Color.blue);
                g.drawString("Building plot... (" + l.size() + " objects)", 10, 10);
                lastUpdateFinished = System.currentTimeMillis();
            }
            lastUpdate = System.currentTimeMillis();
            return;
        }
        updatePending = false;
        super.update(g);
        lastUpdateFinished = lastUpdate = System.currentTimeMillis();
    }

    class Refresher extends Thread {
        GDCanvas c;
        boolean active;

        public Refresher(GDCanvas c) {
            this.c = c;
        }

        @Override
        public void run() {
            active = true;
            while (active) {
                try {
                    Thread.sleep(300);
                } catch (Exception e) {
                }
                if (!active)
                    break;
                if (c.updatePending && (System.currentTimeMillis() - lastUpdate > 200)) {
                    c.repaint();
                }
            }
            c = null;
        }
    }

    @Override
    public synchronized void paint(Graphics g) {
        updatePending = false;
        Dimension d = getSize();
        if (!d.equals(lastSize)) {
            initRefresh();
            lastSize = d;
            return;
        }

        if (forceAntiAliasing) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }

        int i = 0, j = l.size();
        g.setFont(gs.f);
        g.setClip(0, 0, d.width, d.height); // reset clipping rect
        while (i < j) {
            GDObject o = l.elementAt(i++);
            o.paint(this, gs, g);
        }
        lastUpdate = System.currentTimeMillis();
    }

}
