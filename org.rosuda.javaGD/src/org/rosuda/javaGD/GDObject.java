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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;

/** GDObject is an arbitrary object that can be painted */
public abstract class GDObject {
    public abstract void paint(Component c, GDState gs, Graphics g);
}

class GDLine extends GDObject {
    double x1, y1, x2, y2;

    public GDLine(double x1, double y1, double x2, double y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    @Override
    public void paint(Component c, GDState gs, Graphics g) {
        if (gs.col != null)
            g.drawLine((int) (x1 + 0.5), (int) (y1 + 0.5), (int) (x2 + 0.5), (int) (y2 + 0.5));
    }
}

class GDRect extends GDObject {
    double x1, y1, x2, y2;

    public GDRect(double ax1, double ay1, double ax2, double ay2) {
        double tmp;
        double bx1 = ax1;
        double by1 = ay1;
        double bx2 = ax2;
        double by2 = ay2;
        if (bx1 > bx2) {
            tmp = bx1;
            bx1 = bx2;
            bx2 = tmp;
        }
        if (by1 > by2) {
            tmp = by1;
            by1 = by2;
            by2 = tmp;
        }
        this.x1 = bx1;
        this.y1 = by1;
        this.x2 = bx2;
        this.y2 = by2;
        // System.out.println(">> RECT "+x1+":"+y1+" "+x2+":"+y2);
    }

    @Override
    public void paint(Component c, GDState gs, Graphics g) {
        // System.out.println(" paint> rect: "+x1+":"+y1+" "+x2+":"+y2);
        int x = (int) (x1 + 0.5);
        int y = (int) (y1 + 0.5);
        int w = (int) (x2 + 0.5) - x;
        int h = (int) (y2 + 0.5) - y;
        if (gs.fill != null) {
            g.setColor(gs.fill);
            g.fillRect(x, y, w + 1, h + 1);
            if (gs.col != null)
                g.setColor(gs.col);
        }
        if (gs.col != null)
            g.drawRect(x, y, w, h);
    }
}

class GDClip extends GDObject {
    double x1, y1, x2, y2;

    public GDClip(double ax1, double ay1, double ax2, double ay2) {
        double tmp;
        double bx1 = ax1;
        double by1 = ay1;
        double bx2 = ax2;
        double by2 = ay2;
        if (bx1 > bx2) {
            tmp = bx1;
            bx1 = bx2;
            bx2 = tmp;
        }
        if (by1 > by2) {
            tmp = by1;
            by1 = by2;
            by2 = tmp;
        }
        this.x1 = bx1;
        this.y1 = by1;
        this.x2 = bx2;
        this.y2 = by2;
    }

    @Override
    public void paint(Component c, GDState gs, Graphics g) {
        g.setClip((int) (x1 + 0.5), (int) (y1 + 0.5), (int) (x2 - x1 + 1.7), (int) (y2 - y1 + 1.7));
    }
}

class GDCircle extends GDObject {
    double x, y, r;

    public GDCircle(double x, double y, double r) {
        this.x = x;
        this.y = y;
        this.r = r;
    }

    @Override
    public void paint(Component c, GDState gs, Graphics g) {
        if (gs.fill != null) {
            g.setColor(gs.fill);
            g.fillOval((int) (x - r + 0.5), (int) (y - r + 0.5), (int) (r + r + 1.5), (int) (r + r + 1.5));
            if (gs.col != null)
                g.setColor(gs.col);
        }
        if (gs.col != null)
            g.drawOval((int) (x - r + 0.5), (int) (y - r + 0.5), (int) (r + r + 1.5), (int) (r + r + 1.5));
    }
}

class GDText extends GDObject {
    double x, y, r, h;
    String txt;

    public GDText(double x, double y, double r, double h, String txt) {
        this.x = x;
        this.y = y;
        this.r = r;
        this.h = h;
        this.txt = txt;
    }

    @Override
    public void paint(Component c, GDState gs, Graphics g) {
        if (gs.col != null) {
            double rx = x, ry = y;
            double hc = 0d;
            if (h != 0d) {
                FontMetrics fm = g.getFontMetrics();
                int w = fm.stringWidth(txt);
                hc = w * h;
                rx = x - w * h;
            }
            int ix = (int) (rx + 0.5), iy = (int) (ry + 0.5);

            if (r != 0d) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.translate(x, y);
                double rr = -r / 180d * Math.PI;
                g2d.rotate(rr);
                if (hc != 0d)
                    g2d.translate(-hc, 0d);
                g2d.drawString(txt, 0, 0);
                if (hc != 0d)
                    g2d.translate(hc, 0d);
                g2d.rotate(-rr);
                g2d.translate(-x, -y);
            } else
                g.drawString(txt, ix, iy);
        }
    }
}

class GDFont extends GDObject {
    double cex, ps, lineheight;
    int face;
    String family;

    Font font;

    public static boolean useSymbolFont = true;

    static { // this is to work around a bug in Java on Windows where the Symbol font is incorrectly
             // mapped and requires us to force another font for the Symbol characters (yes, it's
             // stupid but apparently for backward compatibility ... *sigh*)
        // we let the user override this detection by setting javagd.usesymbolfont property to
        // true/false (we also support yes/no and 1/0)
        String sfp = System.getProperty("javagd.usesymbolfont");
        if (sfp != null && sfp.length() > 0)
            useSymbolFont = (sfp.equals("true") || sfp.equals("yes") || sfp.equals("1"));
        else { // ok - my tests show that Mac OS X is fine with Symbol, Windows is not, so we'll fix
               // this for Windows only
            String os = System.getProperty("os.name");
            if (os.length() > 2 && os.substring(0, 3).equals("Win"))
                useSymbolFont = false;
        }
    }

    public GDFont(double cex, double ps, double lineheight, int face, String family) {
        // System.out.println(">>
        // FONT(cex="+cex+",ps="+ps+",lh="+lineheight+",face="+face+",\""+family+"\")");
        this.cex = cex;
        this.ps = ps;
        this.lineheight = lineheight;
        this.face = face;
        this.family = family;
        int jFT = Font.PLAIN;
        if (face == 2)
            jFT = Font.BOLD;
        if (face == 3)
            jFT = Font.ITALIC;
        if (face == 4)
            jFT = Font.BOLD | Font.ITALIC;
        if (face == 5 && useSymbolFont)
            this.family = "Symbol";
        font = new Font(this.family.equals("") ? null : this.family, jFT, (int) (cex * ps + 0.5));
    }

    public Font getFont() {
        return font;
    }

    @Override
    public void paint(Component c, GDState gs, Graphics g) {
        g.setFont(font);
        gs.f = font;
    }
}

class GDPolygon extends GDObject {
    int n;
    double x[], y[];
    int xi[], yi[];
    boolean isPolyline;

    public GDPolygon(int n, double[] x, double[] y, boolean isPolyline) {
        this.x = x;
        this.y = y;
        this.n = n;
        this.isPolyline = isPolyline;
        int i = 0;
        xi = new int[n];
        yi = new int[n];
        while (i < n) {
            xi[i] = (int) (x[i] + 0.5);
            yi[i] = (int) (y[i] + 0.5);
            i++;
        }
    }

    @Override
    public void paint(Component c, GDState gs, Graphics g) {
        if (gs.fill != null && !isPolyline) {
            g.setColor(gs.fill);
            g.fillPolygon(xi, yi, n);
            if (gs.col != null)
                g.setColor(gs.col);
        }
        if (gs.col != null) {
            if (isPolyline)
                g.drawPolyline(xi, yi, n);
            else
                g.drawPolygon(xi, yi, n);
        }
    }
}

class GDPath extends GDObject {
    int[] np; // # of points per path (np.length == # of paths)
    double x[], y[]; // copy of the R-supplied data; FIXME: this is not really needed since it's
                     // already in the path object - so NOT rely on this in subclasses!
    boolean winding; // winding rule (true) or odd/even rule (false)
    GeneralPath path;

    public GDPath(int[] np, double[] x, double[] y, boolean winding) {
        this.x = x;
        this.y = y;
        this.np = np;
        this.winding = winding;

        path = new GeneralPath(winding ? GeneralPath.WIND_NON_ZERO : GeneralPath.WIND_EVEN_ODD, x.length);
        int k = 0, end = 0;
        for (int i = 0; i < np.length; i++) {
            end += np[i];
            path.moveTo((float) x[k], (float) y[k]);
            k++;
            for (; k < end; k++)
                path.lineTo((float) x[k], (float) y[k]);
            path.closePath();
        }
    }

    @Override
    public void paint(Component c, GDState gs, Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        if (gs.fill != null) {
            g2.setColor(gs.fill);
            g2.fill(path);
            if (gs.col != null)
                g2.setColor(gs.col);
        }
        if (gs.col != null)
            g2.draw(path);
    }
}

class GDColor extends GDObject {
    int col;
    Color gc;

    public GDColor(int col) {
        this.col = col;
        // System.out.println(">> COLOR: "+Integer.toString(col,16));
        if ((col & 0xff000000) == 0)
            gc = null; // opacity=0 -> no color -> don't paint
        else
            gc = new Color((col & 255) / 255f,
                            ((col >> 8) & 255) / 255f,
                            ((col >> 16) & 255) / 255f,
                            ((col >> 24) & 255) / 255f);
        // System.out.println(" "+gc);
    }

    @Override
    public void paint(Component c, GDState gs, Graphics g) {
        gs.col = gc;
        // System.out.println(" paint > color> (col="+col+") "+gc);
        if (gc != null)
            g.setColor(gc);
    }
}

class GDFill extends GDObject {
    int col;
    Color gc;

    public GDFill(int col) {
        this.col = col;
        // System.out.println(">> FILL COLOR: "+Integer.toString(col,16));
        if ((col & 0xff000000) == 0)
            gc = null; // opacity=0 -> no color -> don't paint
        else
            gc = new Color((col & 255) / 255f,
                            ((col >> 8) & 255) / 255f,
                            ((col >> 16) & 255) / 255f,
                            ((col >> 24) & 255) / 255f);
        // System.out.println(" "+gc);
    }

    @Override
    public void paint(Component c, GDState gs, Graphics g) {
        gs.fill = gc;
    }
}

class GDLinePar extends GDObject {
    double lwd;
    int lty;
    BasicStroke bs;

    public GDLinePar(double lwd, int lty) {
        this.lwd = lwd;
        this.lty = lty;
        // System.out.println(">> LINE TYPE: width="+lwd+", type="+Integer.toString(lty,16));
        bs = null;
        if (lty == 0)
            bs = new BasicStroke((float) lwd);
        else if (lty == -1)
            bs = new BasicStroke(0f);
        else {
            int l = 0;
            int dt = lty;
            while (dt > 0) {
                dt >>= 4;
                l++;
            }
            float[] dash = new float[l];
            dt = lty;
            l = 0;
            while (dt > 0) {
                int rl = dt & 15;
                dash[l++] = rl;
                dt >>= 4;
            }
            bs = new BasicStroke((float) lwd, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 3f, dash, 0f);
        }
    }

    @Override
    public void paint(Component c, GDState gs, Graphics g) {
        if (bs != null)
            ((Graphics2D) g).setStroke(bs);
    }
}

class GDRaster extends GDObject {
    boolean interpolate;
    BufferedImage image;
    AffineTransform atrans;

    public GDRaster(byte img[], int img_w, int img_h, double x, double y, double w, double h, double rot, boolean interpolate) {
        this(new DataBufferByte(img, img_w * img_h, 0), img_w, img_h, x, y, w, h, rot, interpolate);
    }

    public GDRaster(int img[], int img_w, int img_h, double x, double y, double w, double h, double rot, boolean interpolate) {
        this(new DataBufferInt(img, img_w * img_h, 0), img_w, img_h, x, y, w, h, rot, interpolate);
    }

    public GDRaster(DataBuffer dbuf, int img_w, int img_h, double ax, double ay, double aw, double ah,
                    double rot, boolean interpolate) {
        this.interpolate = interpolate;
        atrans = new AffineTransform();
        // R seems to use flipped y coordinates
        double x = ax, y = ay, w = aw, h = ah;
        y += h;
        h = -h;

        double sx = w / img_w, sy = h / img_h;
        atrans.translate(x, y);
        atrans.rotate(-rot / 180 * Math.PI, 0, y);
        atrans.scale(sx, sy);

        // System.out.println("GDRaster(["+img.length+": "+img_w+" x "+img_h+", ("+x+","+y+") -
        // ("+w+","+h+"), "+rot+", "+interpolate);
        // System.out.println(" - at = "+atrans);

        int comp_off[] = {0, 1, 2, 3};
        SampleModel sm = new PixelInterleavedSampleModel(dbuf.getDataType(), img_w, img_h, 4, img_w * 4, comp_off);
        WritableRaster raster = Raster.createWritableRaster(sm, dbuf, null);
        ColorModel cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB),
                        true, false, Transparency.TRANSLUCENT, dbuf.getDataType());
        image = new BufferedImage(cm, raster, false, null);
        // System.out.println(" - image = " + image);
    }

    @Override
    public void paint(Component c, GDState gs, Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        Object oh = g2.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        try {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolate ? RenderingHints.VALUE_INTERPOLATION_BILINEAR : RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g2.drawImage(image, atrans, null);
        } finally {
            if (oh != null)
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oh);
        }
    }
}
