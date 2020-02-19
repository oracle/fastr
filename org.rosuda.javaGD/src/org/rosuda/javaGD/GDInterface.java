//
//  GDInterface.java
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

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;

/**
 * <code>CGInterface</code> defines an interface (and provides a simple implementation) between the
 * JavaGD R device and the Java code. Any back-end that desires to display R graphics in Java can
 * subclass this class are provide its name to JavaGD package via JAVAGD_CLASS_NAME environment
 * variable. The default implementation handles most callbacks, but subclasses should override at
 * least {@link #gdOpen} to create an instance of {@link GDContainer} {@link #c} which will be used
 * for all subsequent drawing.
 * <p>
 * <b>external API: those methods are called via JNI from the GD C code</b>
 * <p>
 *
 * <pre>
 * public void gdOpen(int devNr, double w, double h);
 *
 * public void gdActivate();
 *
 * public void gdCircle(double x, double y, double r);
 *
 * public void gdClip(double x0, double x1, double y0, double y1);
 *
 * public void gdClose();
 *
 * public void gdDeactivate();
 *
 * public void gdFlush(boolean flush);
 *
 * public void gdHold();
 *
 * public double[] gdLocator();
 *
 * public void gdLine(double x1, double y1, double x2, double y2);
 *
 * public double[] gdMetricInfo(int ch);
 *
 * public void gdMode(int mode);
 *
 * public void gdNewPage(int deviceNumber);
 *
 * public void gdPath(int npoly, int[] nper, double[] x, double[] y, int mode);
 *
 * public void gdPolygon(int n, double[] x, double[] y);
 *
 * public void gdPolyline(int n, double[] x, double[] y);
 *
 * public void gdRaster(byte img[], int img_w, int img_h, double x, double y, double w, double h, double rot, boolean interpolate);
 *
 * public void gdRect(double x0, double y0, double x1, double y1);
 *
 * public double[] gdSize();
 *
 * public double gdStrWidth(String str);
 *
 * public void gdText(double x, double y, String str, double rot, double hadj);
 * </pre>
 * <p>
 * <b>GDC - manipulation of the current graphics state</b>
 * <p>
 *
 * <pre>
 * public void gdcSetColor(int cc);
 *
 * public void gdcSetFill(int cc);
 *
 * public void gdcSetLine(double lwd, int lty);
 *
 * public void gdcSetFont(double cex, double ps, double lineheight, int fontface, String fontfamily);
 * </pre>
 */
public class GDInterface {
    /** flag indicating whether this device is active (current) in R */
    public boolean active = false;
    /** flag indicating whether this device has currently an open instance */
    public boolean open = false;
    /** flag indicating whether hold is in progress */
    public boolean holding = false;
    /** device number as supplied by R in {@link #gdNewPage()} (-1 if undefined) */
    int devNr = -1;

    /**
     * container that will receive all drawing methods. It should be created by subclasses in the
     * {@link #gdOpen} method.
     */
    public GDContainer c = null;

    /** synchronization object for locator calls */
    public LocatorSync ls = null;

    /**
     * requests a new device of the specified size
     *
     * @param w width of the device
     * @param h height of the device
     */
    public void gdOpen(double w, double h) {
        open = true;
    }

    /** the device became active (current) */
    public void gdActivate() {
        active = true;
    }

    /**
     * draw a circle
     *
     * @param x x coordinate of the center
     * @param y y coordinate of the center
     * @param r radius
     */
    public void gdCircle(double x, double y, double r) {
        if (c == null)
            return;
        c.add(new GDCircle(x, y, r));
    }

    /**
     * clip drawing to the specified region
     *
     * @param x0 left coordinate
     * @param x1 right coordinate
     * @param y0 top coordinate
     * @param y1 bottom coordinate
     */
    public void gdClip(double x0, double x1, double y0, double y1) {
        if (c == null)
            return;
        c.add(new GDClip(x0, y0, x1, y1));
    }

    /** close the display */
    public void gdClose() {
        if (c != null)
            c.closeDisplay();
        open = false;
    }

    /** the device became inactive (i.e. another device is now current) */
    public void gdDeactivate() {
        active = false;
    }

    /** (unimplemented - this call is now obsolete in R) */
    public void gdHold() {
    }

    /**
     * hold/flush
     *
     * @param flush if <code>false</code> then the device started holding and no updates should be
     *            shown on screen, if <code>true</code> then the device should flush right away and
     *            resume normal operation after than. Note that the flush must either be
     *            synchronous, or it must be guaranteed that shown content will be identical to the
     *            state up till now, otherwise the device will break animations.
     */
    public void gdFlush(boolean flush) {
        holding = !flush;
        if (flush && c != null)
            c.syncDisplay(true);
    }

    /**
     * invoke the locator
     *
     * @return array of indices or <code>null</code> is cancelled
     */
    public double[] gdLocator() {
        if (c == null)
            return null;
        if (ls == null)
            ls = new LocatorSync();
        if (!c.prepareLocator(ls))
            return null;
        return ls.waitForAction();
    }

    /**
     * draw a line
     *
     * @param x1 x coordinate of the origin
     * @param y1 y coordinate of the origin
     * @param x2 x coordinate of the end
     * @param y2 y coordinate of the end
     */
    public void gdLine(double x1, double y1, double x2, double y2) {
        if (c == null)
            return;
        c.add(new GDLine(x1, y1, x2, y2));
    }

    /**
     * retrieve font metrics info for the given unicode character
     *
     * @param ch character (encoding may depend on the font type)
     * @return an array consisting for three doubles: ascent, descent and width
     */
    public double[] gdMetricInfo(int ch) {
        double[] res = new double[3];
        double ascent = 0.0, descent = 0.0, width = 8.0;
        if (c != null) {
            Graphics g = c.getGraphics();
            if (g != null) {
                Font f = c.getGState().f;
                if (f != null) {
                    FontMetrics fm = g.getFontMetrics(c.getGState().f);
                    if (fm != null) {
                        ascent = fm.getAscent();
                        descent = fm.getDescent();
                        width = fm.charWidth((ch == 0) ? 77 : ch);
                    }
                }
            }
        }
        res[0] = ascent;
        res[1] = descent;
        res[2] = width;
        return res;
    }

    /**
     * R signalled a mode change
     *
     * @param mode mode as signalled by R (currently 0=R stopped drawing, 1=R started drawing,
     *            2=graphical input exists)
     */
    public void gdMode(int mode) {
        if (!holding && c != null)
            c.syncDisplay(mode == 0);
    }

    /** create a new, blank page (old API, not used anymore) */
    public void gdNewPage() {
        if (c != null)
            c.reset(-1);
    }

    /**
     * create a new, blank page
     *
     * @param devNr device number assigned to this device by R
     * @param pageNumber it allows set the page number explicitly. It is ignored if negative. Used
     *            in tests.
     */
    public void gdNewPage(@SuppressWarnings("hiding") int devNr, int pageNumber) { // new API:
                                                                                   // provides the
                                                                                   // device
        // Nr.
        this.devNr = devNr;
        if (c != null) {
            c.reset(pageNumber);
            c.setDeviceNumber(devNr);
        }
    }

    /**
     * create multi-polygon path
     *
     * @param winding use winding rule (true) or odd-even rule (false)
     */
    public void gdPath(@SuppressWarnings("unused") int npoly, int[] nper, double[] x, double[] y, boolean winding) {
        if (c == null)
            return;
        c.add(new GDPath(nper, x, y, winding));
    }

    public void gdPolygon(int n, double[] x, double[] y) {
        if (c == null)
            return;
        c.add(new GDPolygon(n, x, y, false));
    }

    public void gdPolyline(int n, double[] x, double[] y) {
        if (c == null)
            return;
        c.add(new GDPolygon(n, x, y, true));
    }

    public void gdRect(double x0, double y0, double x1, double y1) {
        if (c == null)
            return;
        c.add(new GDRect(x0, y0, x1, y1));
    }

    public void gdRaster(byte img[], int img_w, int img_h, double x, double y, double w, double h, double rot, boolean interpolate) {
        if (c == null)
            return;
        c.add(new GDRaster(img, img_w, img_h, x, y, w, h, rot, interpolate));
    }

    /**
     * retrieve the current size of the device
     *
     * @return an array of four doubles: 0, width, height, 0
     */
    public double[] gdSize() {
        double[] res = new double[4];
        double width = 0d, height = 0d;
        if (c != null) {
            Dimension d = c.getSize();
            width = d.getWidth();
            height = d.getHeight();
        }
        res[0] = 0d;
        res[1] = width;
        res[2] = height;
        res[3] = 0;
        return res;
    }

    /**
     * retrieve width of the given text when drawn in the current font
     *
     * @param str text
     * @return width of the text
     */
    public double gdStrWidth(String str) {
        double width = (8 * str.length()); // rough estimate
        if (c != null) { // if canvas is active, we can do better
            Graphics g = c.getGraphics();
            if (g != null) {
                Font f = c.getGState().f;
                if (f != null) {
                    FontMetrics fm = g.getFontMetrics(f);
                    if (fm != null)
                        width = fm.stringWidth(str);
                }
            }
        }
        return width;
    }

    /**
     * draw text
     *
     * @param x x coordinate of the origin
     * @param y y coordinate of the origin
     * @param str text to draw
     * @param rot rotation (in degrees)
     * @param hadj horizontal adjustment with respect to the text size (0=left-aligned wrt origin,
     *            0.5=centered, 1=right-aligned wrt origin)
     */
    public void gdText(double x, double y, String str, double rot, double hadj) {
        if (c == null)
            return;
        c.add(new GDText(x, y, rot, hadj, str));
    }

    /*-- GDC - manipulation of the current graphics state */
    /**
     * set drawing color
     *
     * @param cc color
     */
    public void gdcSetColor(int cc) {
        if (c == null)
            return;
        c.add(new GDColor(cc));
    }

    /**
     * set fill color
     *
     * @param cc color
     */
    public void gdcSetFill(int cc) {
        if (c == null)
            return;
        c.add(new GDFill(cc));
    }

    /**
     * set line width and type
     *
     * @param lwd line width (see <code>lwd</code> parameter in R)
     * @param lty line type (see <code>lty</code> parameter in R)
     */
    public void gdcSetLine(double lwd, int lty) {
        if (c == null)
            return;
        c.add(new GDLinePar(lwd, lty));
    }

    /**
     * set current font
     *
     * @param cex character expansion (see <code>cex</code> parameter in R)
     * @param ps point size (see <code>ps</code> parameter in R - for all practical purposes the
     *            requested font size in points is <code>cex * ps</code>)
     * @param lineheight line height
     * @param fontface font face (see <code>font</code> parameter in R: 1=plain, 2=bold, 3=italic,
     *            4=bold-italic, 5=symbol)
     * @param fontfamily font family (see <code>family</code> parameter in R)
     */
    public void gdcSetFont(double cex, double ps, double lineheight, int fontface, String fontfamily) {
        if (c == null)
            return;
        GDFont f = new GDFont(cex, ps, lineheight, fontface, fontfamily);
        c.add(f);
        c.getGState().f = f.getFont();
    }

    /**
     * returns the device number
     *
     * @return device number or -1 is unknown
     */
    public int getDeviceNumber() {
        return (c == null) ? devNr : c.getDeviceNumber();
    }

    /** close the device in R associted with this instance */
    public void executeDevOff() {
// if (c == null || c.getDeviceNumber() < 0)
// return;
// try { // for now we use no cache - just pure reflection API for:
// // Rengine.getMainEngine().eval("...")
// Class cl = Class.forName("org.rosuda.JRI.Rengine");
// if (cl == null)
// System.out.println(">> can't find Rengine, close function disabled. [c=null]");
// else {
// Method m = cl.getMethod("getMainEngine", null);
// Object o = m.invoke(null, null);
// if (o != null) {
// Class[] par = new Class[1];
// par[0] = Class.forName("java.lang.String");
// m = cl.getMethod("eval", par);
// Object[] pars = new Object[1];
// pars[0] = "try({ dev.set(" + (c.getDeviceNumber() + 1) + "); dev.off()},silent=TRUE)";
// m.invoke(o, pars);
// }
// }
// } catch (Exception e) {
// System.out.println(">> can't find Rengine, close function disabled. [x:" + e.getMessage() + "]");
// }
    }
}
