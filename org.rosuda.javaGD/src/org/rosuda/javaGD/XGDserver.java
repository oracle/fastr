//
//  XGDserver.java
//  A sample implementation of the XGD1 protocol using GDCanvas for drawing
//
//  Created by Simon Urbanek on Sun Apr 05 2004.
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
//  $Id: XGDserver.java 3165 2009-09-02 13:21:35Z urbanek $

package org.rosuda.javaGD;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * XGDserver is a sample implementation of a server understanding the XGD1 protocol using
 * {@link GDCanvas} for drawing. It is meant rather as a template for projects that wish to use R
 * graphics device remotely via the <a href=http://rforge.net/xGD>xGD package</a>. The main
 * customization for other projects can be done for Open (1) and Close (2) commands.
 */
public class XGDserver extends Thread {
    /** default TCP port used by XGD */
    public static final int XGD_Port = 1427;

    public static final byte CMD_Open = 0x01;
    public static final byte CMD_Close = 0x02;
    public static final byte CMD_Activate = 0x03;
    public static final byte CMD_Deactivate = 0x04;
    public static final byte CMD_Circle = 0x05;
    public static final byte CMD_Clip = 0x06;
    public static final byte CMD_Hold = 0x07;
    public static final byte CMD_Locator = 0x48;
    public static final byte CMD_Line = 0x09;
    public static final byte CMD_MetricInfo = 0x4a;
    public static final byte CMD_Mode = 0x0b;
    public static final byte CMD_NewPage = 0x0c;
    public static final byte CMD_Polygon = 0x0d;
    public static final byte CMD_Polyline = 0x0e;
    public static final byte CMD_Rect = 0x0f;
    public static final byte CMD_GetSize = 0x50;
    public static final byte CMD_StrWidth = 0x51;
    public static final byte CMD_Text = 0x12;

    public static final byte CMD_SetColor = 0x13;
    public static final byte CMD_SetFill = 0x14;
    public static final byte CMD_SetFont = 0x15;
    public static final byte CMD_SetLinePar = 0x16;

    /** TCP port to listen on */
    int port;

    private final Consumer<Integer> resizer;

    /**
     * start a new XGD server on a specific port
     *
     * @param port TCP port to listen on
     */
    public XGDserver(int port, Consumer<Integer> resizer) {
        this.port = port;
        this.resizer = resizer;
    }

    /** start a new XGD server on the default port */
    public XGDserver(Consumer<Integer> resizer) {
        this.port = XGD_Port;
        this.resizer = resizer;
    }

    /** worker class serving one client connection */
    class XGDworker extends Thread {
        /** socket for communication with the client */
        public Socket s;
        /**
         * flag determining whether the client is big-endian such that the transmission byte order
         * is maintianed accordingly
         */
        boolean isBE;
        /** canvas used for drawing */
        GDCanvas c;
        /** window (frame) containing the canvas */
        Frame f;

        /**
         * converts an integer (32-bit) from the packet stream
         *
         * @param b packet byte stream
         * @param o offset in the stream
         * @return integer represented in the packet and the given offset
         */
        int getInt(byte[] b, int o) {
            return (isBE) ? ((b[o + 3] & 255) | ((b[o + 2] & 255) << 8) | ((b[o + 1] & 255) << 16) | ((b[o] & 255) << 24))
                            : ((b[o] & 255) | ((b[o + 1] & 255) << 8) | ((b[o + 2] & 255) << 16) | ((b[o + 3] & 255) << 24));
        }

        /**
         * converts a long integer (64-bit) from the packet stream
         *
         * @param b packet byte stream
         * @param offset offset in the stream
         * @return long integer represented in the packet and the given offset
         */
        long getLong(byte[] b, int offset) {
            long l1, l2;
            if (isBE) {
                l1 = ((long) getInt(b, offset + 4)) & 0xffffffffL;
                l2 = ((long) getInt(b, offset)) & 0xffffffffL;
            } else {
                l1 = ((long) getInt(b, offset)) & 0xffffffffL;
                l2 = ((long) getInt(b, offset + 4)) & 0xffffffffL;
            }
            return l1 | (l2 << 32);
        }

        /**
         * converts a double from the packet stream
         *
         * @param b packet byte stream
         * @param offset offset in the stream
         * @return double represented in the packet and the given offset
         */
        double getDouble(byte[] b, int offset) {
            return Double.longBitsToDouble(getLong(b, offset));
        }

        /**
         * stores an integer (32-bit) into the packet stream
         *
         * @param v integer value to store
         * @param buf packet byte stream
         * @param o offset in the stream to store to (takes 4 bytes)
         */
        void setInt(int v, byte[] buf, int o) {
            if (!isBE) {
                buf[o] = (byte) (v & 255);
                o++;
                buf[o] = (byte) ((v & 0xff00) >> 8);
                o++;
                buf[o] = (byte) ((v & 0xff0000) >> 16);
                o++;
                buf[o] = (byte) ((v & 0xff000000) >> 24);
            } else {
                buf[o + 3] = (byte) (v & 255);
                buf[o + 2] = (byte) ((v & 0xff00) >> 8);
                buf[o + 1] = (byte) ((v & 0xff0000) >> 16);
                buf[o] = (byte) ((v & 0xff000000) >> 24);
            }
        }

        /**
         * stores a long integer (64-bit) into the packet stream
         *
         * @param l long integer value to store
         * @param buf packet byte stream
         * @param o offset in the stream to store to (takes 8 bytes)
         */
        void setLong(long l, byte[] buf, int o) {
            setInt((int) (l & 0xffffffffL), buf, isBE ? o + 4 : o);
            setInt((int) (l >> 32), buf, isBE ? o : o + 4);
        }

        /**
         * stores a double into the packet stream
         *
         * @param d double value to store
         * @param buf packet byte stream
         * @param o offset in the stream to store to (takes 8 bytes)
         */
        void setDouble(double d, byte[] buf, int o) {
            setLong(Double.doubleToLongBits(d), buf, o);
        }

        /**
         * dumps a byte stream in hex form to {@link System#out} (with a prefix and trailing new
         * line)
         *
         * @param s prefix string to print before the dump
         * @param b byte array to print in hex
         */
        void dump(String s, byte[] b) {
            System.out.print(s);
            int i = 0;
            while (i < b.length) {
                System.out.print(Integer.toString((int) b[i], 16) + " ");
                i++;
            }
            System.out.println("");
        }

        /** main thread method servicing the client */
        @Override
        public void run() {
            try {
                s.setTcpNoDelay(true); // send packets immediately (important, because R is waiting
                                       // for the response)
                // s.setSoTimeout(1);
                System.out.println("XGDworker started with socket " + s);
                InputStream sis = s.getInputStream();
                OutputStream sos = s.getOutputStream();
                byte[] id = new byte[16];
                int n = sis.read(id);
                if (n != 16) {
                    System.out.println("Required 16 bytes, but got " + n + ". Invalid protocol.");
                    s.close();
                    return;
                }
                if (id[0] == 0x58 && id[1] == 0x47 && id[2] == 0x44) {
                    System.out.println("Connected to XGD version " + (id[3] - 48) + " on PPC-endian machine");
                    isBE = true;
                } else if (id[3] == 0x58 && id[2] == 0x47 && id[1] == 0x44) {
                    System.out.println("Connected to XGD version " + (id[0] - 48) + " on Intel-endian machine");
                    isBE = false;
                } else {
                    System.out.println("Unknown protocol, bailing out");
                    s.close();
                    return;
                }

                byte[] hdr = new byte[4];
                while (true) {
                    n = sis.read(hdr);
                    if (n < 4) {
                        System.out.println("Needed 4 bytes, got " + n);
                        break;
                    }
                    // dump("Got header: ",hdr);
                    int len = getInt(hdr, 0);
                    int cmd = len & 0xff;
                    len = len >> 8;
                    System.out.println("CMD: " + cmd + ", length: " + len);

                    byte[] par = new byte[len];

                    if (len > 0) {
                        int n2 = sis.read(par);
                        if (n2 != len) {
                            System.out.println("Needed " + len + " bytes, got " + n2);
                            break;
                        }
                    }

                    // dump("Got pars: ",par);

                    if (cmd == CMD_Open) {
                        double w = getDouble(par, 0);
                        double h = getDouble(par, 8);
                        System.out.println("Open(" + w + ",+" + h + ")");

                        if (f != null) {
                            f.removeAll();
                            f.dispose();
                            f = null;
                            if (c != null)
                                c = null;
                        }
                        f = new Frame();
                        c = new GDCanvas((int) w, (int) h, resizer);
                        f.add(c);
                        f.pack();
                        f.setVisible(true);
                    }

                    if (cmd == CMD_Close) {
                        if (f != null) {
                            f.removeAll();
                            f.dispose();
                            f = null;
                            if (c != null)
                                c = null;
                        }
                        System.out.println("Device closed.");
                        return;
                    }

                    if (cmd == CMD_Line && c != null) {
                        c.add(new GDLine(getDouble(par, 0), getDouble(par, 8), getDouble(par, 16), getDouble(par, 24)));
                    }

                    if (cmd == CMD_Rect && c != null) {
                        c.add(new GDRect(getDouble(par, 0), getDouble(par, 8), getDouble(par, 16), getDouble(par, 24)));
                    }

                    if (cmd == CMD_Circle && c != null) {
                        c.add(new GDCircle(getDouble(par, 0), getDouble(par, 8), getDouble(par, 16)));
                    }

                    if (cmd == CMD_Mode && c != null) {
                        if (getInt(par, 0) == 0)
                            c.repaint();
                    }

                    if (cmd == CMD_NewPage && c != null) {
                        c.reset(-1);
                    }

                    if ((cmd == CMD_Polygon || cmd == CMD_Polyline) && c != null) {
                        int pn = getInt(par, 0);
                        int i = 0;
                        double x[], y[];
                        x = new double[pn];
                        y = new double[pn];
                        while (i < pn) {
                            x[i] = getDouble(par, 4 + i * 8);
                            y[i] = getDouble(par, 4 + i * 8 + pn * 8);
                            i++;
                        }
                        c.add(new GDPolygon(pn, x, y, cmd == CMD_Polyline));
                    }

                    if (cmd == CMD_Text && c != null) {
                        c.add(new GDText(getDouble(par, 0), getDouble(par, 8), getDouble(par, 16), getDouble(par, 24), new String(par, 32, par.length - 33)));
                    }

                    if (cmd == CMD_SetColor && c != null) {
                        c.add(new GDColor(getInt(par, 0)));
                    }

                    if (cmd == CMD_SetFill && c != null) {
                        c.add(new GDFill(getInt(par, 0)));
                    }

                    if (cmd == CMD_SetFont && c != null) {
                        GDFont xf = new GDFont(getDouble(par, 0), getDouble(par, 8), getDouble(par, 16), getInt(par, 24), new String(par, 32, par.length - 33));
                        c.add(xf);
                        // we need to set Canvas' internal font to this new font for further metrics
                        // calculations
                        c.gs.f = xf.font;
                    }

                    if (cmd == CMD_SetLinePar && c != null) {
                        c.add(new GDLinePar(getDouble(par, 0), getInt(par, 8)));
                    }

                    if (cmd == CMD_GetSize) {
                        byte[] b = new byte[4 * 8 + 4];
                        setInt((0x50 | 0x80) | ((4 * 8) << 8), b, 0);
                        double width = 0d, height = 0d;
                        if (c != null) {
                            Dimension d = c.getSize();
                            width = d.getWidth();
                            height = d.getHeight();
                        }
                        setDouble(0d, b, 4);
                        setDouble(width, b, 12);
                        setDouble(height, b, 20);
                        setDouble(0d, b, 28);
                        sos.write(b);
                        sos.flush();
                    }

                    if (cmd == CMD_StrWidth) { // StrWidth
                        String s = new String(par, 0, par.length - 1);
                        System.out.println("Request: get string width of \"" + s + "\"");
                        byte[] b = new byte[12];
                        setInt((0x51 | 0x80) | 0x800, b, 0);
                        double width = (double) (8 * s.length()); // rough estimate
                        if (c != null) {
                            Graphics g = c.getGraphics();
                            if (g != null) {
                                FontMetrics fm = g.getFontMetrics(c.gs.f);
                                if (fm != null)
                                    width = (double) fm.stringWidth(s);
                            }
                        }
                        System.out.println(">> WIDTH: " + width);
                        setDouble(width, b, 4);
                        // dump("Sending: ",b);
                        sos.write(b);
                        sos.flush();
                    }

                    if (cmd == CMD_MetricInfo) { // MetricInfo
                        int ch = getInt(par, 0);
                        System.out.println("Request: metric info for char " + ch);
                        byte[] b = new byte[4 + 3 * 8];

                        double ascent = 0.0, descent = 0.0, width = 8.0;
                        if (c != null) {
                            Graphics g = c.getGraphics();
                            if (g != null) {
                                FontMetrics fm = g.getFontMetrics(c.gs.f);
                                if (fm != null) {
                                    ascent = (double) fm.getAscent();
                                    descent = (double) fm.getDescent();
                                    width = (double) fm.charWidth((ch == 0) ? 77 : ch);
                                }
                            }
                        }
                        System.out.println(">> MI: ascent=" + ascent + ", descent=" + descent + ", width=" + width);
                        setInt((0x4a | 0x80) | ((3 * 8) << 8), b, 0);
                        setDouble(ascent, b, 4);
                        setDouble(descent, b, 12);
                        setDouble(width, b, 20);
                        // dump("Sending: ",b);
                        sos.write(b);
                        sos.flush();
                    }
                }
            } catch (Exception e) {
                System.out.println("XGDworker" + this + ", exception: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * main server method listening on port 1427 and serving clients. It dispatches new worker
     * threads for each accepted connection.
     */
    @Override
    public void run() {
        try {
            ServerSocket s = new ServerSocket(port);
            while (true) {
                Socket cs = s.accept();
                System.out.println("Accepted connection, spawning new worker thread.");
                XGDworker w = new XGDworker();
                w.s = cs;
                w.start();
                w = null;
            }
        } catch (Exception e) {
            System.out.println("XGDserver, exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * starts a new XGD server. Note that there can only be one active server at the time unless the
     * TCP port number is changed.
     *
     * @return new XGD server instance
     */
    public static XGDserver startServer(Consumer<Integer> resizer) {
        XGDserver s = new XGDserver(resizer);
        s.start();
        return s;
    }

    /**
     * main method - calls {@link #startServer(Consumer)}
     *
     * @param args command line arguments (currently ignored)
     */
    public static void main(String[] args) {
        System.out.println("Starting XGDserver.");
        startServer(null); // TODO:
    }
}
