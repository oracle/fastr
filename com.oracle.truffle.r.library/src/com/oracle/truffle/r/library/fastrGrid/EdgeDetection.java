/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (C) 2001-3 Paul Murrell
 * Copyright (c) 1998-2013, The R Core Team
 * Copyright (c) 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.fastrGrid;

import static com.oracle.truffle.r.library.fastrGrid.GridUtils.fmax;
import static com.oracle.truffle.r.library.fastrGrid.GridUtils.fmin;
import static com.oracle.truffle.r.runtime.nmath.MathConstants.M_PI;
import static com.oracle.truffle.r.runtime.nmath.RMath.fmax2;
import static com.oracle.truffle.r.runtime.nmath.RMath.fmin2;
import static com.oracle.truffle.r.runtime.nmath.TOMS708.fabs;

import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;

/**
 * Contains static method related to edge detection for bounds calculations.
 */
final class EdgeDetection {
    private EdgeDetection() {
        // only static members
    }

    /**
     * Do two lines intersect? Algorithm from Paul Bourke
     * (http://www.swin.edu.au/astronomy/pbourke/geometry/lineline2d/index.html)
     */
    private static boolean linesIntersect(double x1, double x2, double x3, double x4,
                    double y1, double y2, double y3, double y4) {
        double denom = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1);
        double ua = ((x4 - x3) * (y1 - y3) - (y4 - y3) * (x1 - x3));
        // If the lines are parallel ...
        if (denom == 0) {
            // If the lines are coincident ...
            if (ua == 0) {
                // If the lines are vertical ...
                if (x1 == x2) {
                    // Compare y-values
                    if (!((y1 < y3 && fmax2(y1, y2) < fmin2(y3, y4)) || (y3 < y1 && fmax2(y3, y4) < fmin2(y1, y2)))) {
                        return true;
                    }
                } else {
                    // Compare x-values
                    if (!((x1 < x3 && fmax2(x1, x2) < fmin2(x3, x4)) || (x3 < x1 && fmax2(x3, x4) < fmin2(x1, x2)))) {
                        return true;
                    }
                }
            }
        } else {
            // ... otherwise, calculate where the lines intersect ...
            double ub = ((x2 - x1) * (y1 - y3) - (y2 - y1) * (x1 - x3));
            ua = ua / denom;
            ub = ub / denom;
            // Check for overlap
            if ((ua > 0 && ua < 1) && (ub > 0 && ub < 1)) {
                return true;
            }
        }
        return false;
    }

    private static boolean edgesIntersect(double x1, double x2, double y1, double y2, Rectangle r) {
        return linesIntersect(x1, x2, r.x[0], r.x[1], y1, y2, r.y[0], r.y[1]) ||
                        linesIntersect(x1, x2, r.x[1], r.x[2], y1, y2, r.y[1], r.y[2]) ||
                        linesIntersect(x1, x2, r.x[2], r.x[3], y1, y2, r.y[2], r.y[3]) ||
                        linesIntersect(x1, x2, r.x[3], r.x[0], y1, y2, r.y[3], r.y[0]);
    }

    static Point rectEdge(double xmin, double ymin, double xmax, double ymax, double theta) {
        double xm = (xmin + xmax) / 2;
        double ym = (ymin + ymax) / 2;
        double dx = (xmax - xmin) / 2;
        double dy = (ymax - ymin) / 2;
        /*
         * GNUR fixme: Special case 0 width or 0 height
         */
        // Special case angles
        if (theta == 0) {
            return new Point(xmax, ym);
        } else if (theta == 270) {
            return new Point(xm, ymin);
        } else if (theta == 180) {
            return new Point(xmin, ym);
        } else if (theta == 90) {
            return new Point(xm, ymax);
        } else {
            double cutoff = dy / dx;
            double angle = theta / 180 * M_PI;
            double tanTheta = Math.tan(angle);
            double cosTheta = Math.cos(angle);
            double sinTheta = Math.sin(angle);
            if (fabs(tanTheta) < cutoff) { /* Intersect with side */
                if (cosTheta > 0) { /* Right side */
                    return new Point(xmax, ym + tanTheta * dx);
                } else { /* Left side */
                    return new Point(xmin, ym - tanTheta * dx);
                }
            } else { /* Intersect with top/bottom */
                if (sinTheta > 0) { /* Top */
                    return new Point(xm + dy / tanTheta, ymax);
                } else { /* Bottom */
                    return new Point(xm - dy / tanTheta, ymin);
                }
            }
        }
    }

    static Point polygonEdge(double[] x, double[] y, int n, double theta) {
        // centre of the polygon
        double xmin = fmin(Double.MAX_VALUE, x);
        double xmax = fmax(Double.MIN_VALUE, x);
        double ymin = fmin(Double.MAX_VALUE, y);
        double ymax = fmax(Double.MIN_VALUE, y);
        double xm = (xmin + xmax) / 2;
        double ym = (ymin + ymax) / 2;

        // Special case zero-width or zero-height
        if (fabs(xmin - xmax) < 1e-6) {
            double resultY = theta == 90 ? ymax : theta == 270 ? ymin : ym;
            return new Point(xmin, resultY);
        }
        if (fabs(ymin - ymax) < 1e-6) {
            double resultX = theta == 0 ? xmax : theta == 180 ? xmin : xm;
            return new Point(resultX, ymin);
        }

        /*
         * Find edge that intersects line from centre at angle theta
         */
        boolean found = false;
        double angle = theta / 180 * M_PI;
        double vangle1;
        double vangle2;
        int v1 = 0;
        int v2 = 1;
        for (int i = 0; i < n; i++) {
            v1 = i;
            v2 = v1 + 1;
            if (v2 == n) {
                v2 = 0;
            }
            /*
             * Result of atan2 is in range -PI, PI so convert to 0, 360 to correspond to angle
             */
            vangle1 = Math.atan2(y[v1] - ym, x[v1] - xm);
            if (vangle1 < 0) {
                vangle1 = vangle1 + 2 * M_PI;
            }
            vangle2 = Math.atan2(y[v2] - ym, x[v2] - xm);
            if (vangle2 < 0) {
                vangle2 = vangle2 + 2 * M_PI;
            }
            /*
             * If vangle1 < vangle2 then angles are either side of 0 so check is more complicated
             */
            if ((vangle1 >= vangle2 &&
                            vangle1 >= angle && vangle2 < angle) ||
                            (vangle1 < vangle2 &&
                                            ((vangle1 >= angle && 0 <= angle) ||
                                                            (vangle2 < angle && 2 * M_PI >= angle)))) {
                found = true;
                break;
            }
        }
        /*
         * Find intersection point of "line from centre to bounding rect" and edge
         */
        if (!found) {
            throw RError.error(RError.NO_CALLER, Message.GENERIC, "polygon edge not found");
        }
        double x3 = x[v1];
        double y3 = y[v1];
        double x4 = x[v2];
        double y4 = y[v2];
        Point tmp = rectEdge(xmin, ymin, xmax, ymax, theta);
        double x2 = tmp.x;
        double y2 = tmp.y;
        double numa = ((x4 - x3) * (ym - y3) - (y4 - y3) * (xm - x3));
        double denom = ((y4 - y3) * (x2 - xm) - (x4 - x3) * (y2 - ym));
        double ua = numa / denom;
        if (!Double.isFinite(ua)) {
            /*
             * Should only happen if lines are parallel, which shouldn't happen! Unless, perhaps the
             * polygon has zero extent vertically or horizontally ... ?
             */
            throw RInternalError.shouldNotReachHere("polygon edge not found (zero-width or zero-height?)");
        }
        /*
         * numb = ((x2 - x1)*(y1 - y3) - (y2 - y1)*(x1 - x3)); ub = numb/denom;
         */
        return new Point(xm + ua * (x2 - xm), ym + ua * (y2 - ym));
    }

    public static Point hullEdge(GridContext ctx, double[] xx, double[] yy, double theta) {
        RDoubleVector xVec = RDataFactory.createDoubleVector(xx, RDataFactory.COMPLETE_VECTOR);
        RDoubleVector yVec = RDataFactory.createDoubleVector(yy, RDataFactory.COMPLETE_VECTOR);
        Object hullObj = ctx.evalInternalRFunction("chullWrapper", xVec, yVec);
        RAbstractIntVector hull = GridUtils.asIntVector(hullObj);
        double[] newXX = new double[hull.getLength()];
        double[] newYY = new double[hull.getLength()];
        for (int i = 0; i < hull.getLength(); i++) {
            newXX[i] = xx[hull.getDataAt(i) - 1];
            newYY[i] = yy[hull.getDataAt(i) - 1];
        }
        return polygonEdge(newXX, newYY, newXX.length, theta);
    }

    public static Point circleEdge(Point loc, double radius, double theta) {
        double angle = theta / 180 * Math.PI;
        return new Point(loc.x + radius * Math.cos(angle), loc.y + radius * Math.sin(angle));
    }

    /**
     * An arbitrarily-oriented rectangle. The vertices are assumed to be in order going
     * anticlockwise around the rectangle.
     */
    public static final class Rectangle {
        public final double[] x;
        public final double[] y;

        Rectangle(Point p1, Point p2, Point p3, Point p4) {
            x = new double[]{p1.x, p2.x, p3.x, p4.x};
            y = new double[]{p1.y, p2.y, p3.y, p4.y};
        }

        public boolean intersects(Rectangle r2) {
            return edgesIntersect(this.x[0], this.x[1], this.y[0], this.y[1], r2) ||
                            edgesIntersect(this.x[1], this.x[2], this.y[1], this.y[2], r2) ||
                            edgesIntersect(this.x[2], this.x[3], this.y[2], this.y[3], r2) ||
                            edgesIntersect(this.x[3], this.x[0], this.y[3], this.y[0], r2);
        }
    }

    /**
     * Represents min and max value for X and Y coordinates and provides convenient methods to
     * update them.
     */
    public static final class Bounds {
        public double minX = Double.MAX_VALUE;
        public double maxX = Double.MIN_VALUE;
        public double minY = Double.MAX_VALUE;
        public double maxY = Double.MIN_VALUE;

        public void update(Point p) {
            updateX(p.x);
            updateY(p.y);
        }

        public void updateX(double... values) {
            minX = GridUtils.fmin(minX, values);
            maxX = GridUtils.fmax(maxX, values);
        }

        public void updateY(double... values) {
            minY = GridUtils.fmin(minY, values);
            maxY = GridUtils.fmax(maxY, values);
        }

        public double getWidth() {
            return maxX - minX;
        }

        public double getHeight() {
            return maxY - minY;
        }
    }
}
