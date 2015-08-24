/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

// For now, failing implementations of the functions from GnuR src/main/engine.c

#include "rffiutils.h"
#include <GraphicsEngine.h>

void init_graphicsengine(JNIEnv *env) {
}

R_GE_lineend GE_LENDpar(SEXP value, int ind) {
	return (R_GE_lineend) unimplemented("GE_LENDpar");
}

SEXP GE_LENDget(R_GE_lineend lend) {
	return (SEXP) unimplemented("GE_LENDget");
}

R_GE_linejoin GE_LJOINpar(SEXP value, int ind) {
	return (R_GE_linejoin) unimplemented("GE_LJOINpar");
}

SEXP GE_LJOINget(R_GE_linejoin ljoin) {
	return (SEXP) unimplemented("GE_LJOINget");
}


void GESetClip(double x1, double y1, double x2, double y2, pGEDevDesc dd) {
	unimplemented("GESetClip");
}

void GENewPage(const pGEcontext gc, pGEDevDesc dd) {
	unimplemented("GENewPage");
}

void GELine(double x1, double y1, double x2, double y2,
		const pGEcontext gc, pGEDevDesc dd) {
	unimplemented("GELine");
}

void GEPolyline(int n, double *x, double *y,
		const pGEcontext gc, pGEDevDesc dd) {
	unimplemented("GEPolyline");
}

void GEPolygon(int n, double *x, double *y,
		const pGEcontext gc, pGEDevDesc dd) {
	unimplemented("GEPolygon");
}

SEXP GEXspline(int n, double *x, double *y, double *s, Rboolean open,
		Rboolean repEnds, Rboolean draw,
		const pGEcontext gc, pGEDevDesc dd) {
	return (SEXP) unimplemented("GEXspline");
}

void GECircle(double x, double y, double radius,
		const pGEcontext gc, pGEDevDesc dd) {
	unimplemented("");
}

void GERect(double x0, double y0, double x1, double y1,
		const pGEcontext gc, pGEDevDesc dd) {
	unimplemented("GECircle");
}

void GEPath(double *x, double *y,
		int npoly, int *nper,
		Rboolean winding,
		const pGEcontext gc, pGEDevDesc dd) {
	unimplemented("GEPath");
}

void GERaster(unsigned int *raster, int w, int h,
		double x, double y, double width, double height,
		double angle, Rboolean interpolate,
		const pGEcontext gc, pGEDevDesc dd) {
	unimplemented("GERaster");
}

SEXP GECap(pGEDevDesc dd) {
	return (SEXP) unimplemented("GECap");
}

void GEText(double x, double y, const char * const str, cetype_t enc,
		double xc, double yc, double rot,
		const pGEcontext gc, pGEDevDesc dd) {
	unimplemented("GEText");
}

void GEMode(int mode, pGEDevDesc dd) {
	unimplemented("GEText");
}

void GESymbol(double x, double y, int pch, double size,
		const pGEcontext gc, pGEDevDesc dd) {
	unimplemented("GESymbol");
}

void GEPretty(double *lo, double *up, int *ndiv) {
	unimplemented("GEPretty");
}

void GEMetricInfo(int c, const pGEcontext gc,
		double *ascent, double *descent, double *width,
		pGEDevDesc dd) {
	unimplemented("GEPretty");
}

double GEStrWidth(const char *str, cetype_t enc,
		const pGEcontext gc, pGEDevDesc dd) {
	unimplemented("GEStrWidth");
	return 0.0;
}

double GEStrHeight(const char *str, cetype_t enc,
		const pGEcontext gc, pGEDevDesc dd) {
	unimplemented("GEStrHeight");
	return 0.0;
}

void GEStrMetric(const char *str, cetype_t enc, const pGEcontext gc,
		double *ascent, double *descent, double *width,
		pGEDevDesc dd) {
	unimplemented("GEStrMetric");
}

int GEstring_to_pch(SEXP pch) {
	return (int) unimplemented("GEstring_to_pch");
}

unsigned int GE_LTYpar(SEXP x, int y) {
    return unimplemented("GE_LTYpar");
}

SEXP GE_LTYget(unsigned int x) {
	return (SEXP) unimplemented("GE_LTYget");
}

void R_GE_rasterScale(unsigned int *sraster, int sw, int sh,
                      unsigned int *draster, int dw, int dh) {
    unimplemented("R_GE_rasterScale");
}

void R_GE_rasterInterpolate(unsigned int *sraster, int sw, int sh,
                            unsigned int *draster, int dw, int dh) {
    unimplemented("R_GE_rasterInterpolate");
}

void R_GE_rasterRotatedSize(int w, int h, double angle,
                            int *wnew, int *hnew) {
    unimplemented("R_GE_rasterRotatedSize");
}

void R_GE_rasterRotatedOffset(int w, int h, double angle, int botleft,
                              double *xoff, double *yoff) {
    unimplemented("R_GE_rasterRotatedOffset");
}

void R_GE_rasterResizeForRotation(unsigned int *sraster,
                                  int w, int h,
                                  unsigned int *newRaster,
                                  int wnew, int hnew,
                                  const pGEcontext gc) {
    unimplemented("R_GE_rasterResizeForRotation");
}

void R_GE_rasterRotate(unsigned int *sraster, int w, int h, double angle,
                       unsigned int *draster, const pGEcontext gc,
                       Rboolean perPixelAlpha) {
    unimplemented("R_GE_rasterRotate");
}

double GEExpressionWidth(SEXP expr,
			 const pGEcontext gc, pGEDevDesc dd) {
    unimplemented("");
    return 0.0;
}

double GEExpressionHeight(SEXP expr,
			  const pGEcontext gc, pGEDevDesc dd) {
    unimplemented("GEExpressionHeight");
    return 0.0;
}

void GEExpressionMetric(SEXP expr, const pGEcontext gc,
                        double *ascent, double *descent, double *width,
                        pGEDevDesc dd) {
    unimplemented("GEExpressionMetric");
}

void GEMathText(double x, double y, SEXP expr,
		double xc, double yc, double rot,
		const pGEcontext gc, pGEDevDesc dd) {
    unimplemented("GEMathText");
}

SEXP GEcontourLines(double *x, int nx, double *y, int ny,
		    double *z, double *levels, int nl) {
    return (SEXP) unimplemented("GEcontourLines");
}

double R_GE_VStrWidth(const char *s, cetype_t enc, const pGEcontext gc, pGEDevDesc dd) {
    unimplemented("R_GE_VStrWidth");
    return 0.0;
}


double R_GE_VStrHeight(const char *s, cetype_t enc, const pGEcontext gc, pGEDevDesc dd) {
    unimplemented("R_GE_VStrHeight");
    return 0.0;
}

void R_GE_VText(double x, double y, const char * const s, cetype_t enc,
		double x_justify, double y_justify, double rotation,
		const pGEcontext gc, pGEDevDesc dd) {
    unimplemented("R_GE_VText");
}

pGEDevDesc GEcurrentDevice(void) {
    return (pGEDevDesc) unimplemented("GEcurrentDevice");
}

Rboolean GEdeviceDirty(pGEDevDesc dd) {
    unimplemented("GEcurrentDevice");
    return FALSE;
}

void GEdirtyDevice(pGEDevDesc dd) {
    unimplemented("GEdirtyDevice");
}

Rboolean GEcheckState(pGEDevDesc dd) {
    unimplemented("GEcheckState");
    return FALSE;
}

Rboolean GErecording(SEXP call, pGEDevDesc dd) {
    unimplemented("GErecording");
    return FALSE;
}

void GErecordGraphicOperation(SEXP op, SEXP args, pGEDevDesc dd) {
    unimplemented("GErecordGraphicOperation");
}

void GEinitDisplayList(pGEDevDesc dd) {
    unimplemented("GEinitDisplayList");
}

void GEplayDisplayList(pGEDevDesc dd) {
    unimplemented("GEplayDisplayList");
}

void GEcopyDisplayList(int fromDevice) {
    unimplemented("GEcopyDisplayList");
}

SEXP GEcreateSnapshot(pGEDevDesc dd) {
	return (SEXP) unimplemented("GEcreateSnapshot");
}

void GEplaySnapshot(SEXP snapshot, pGEDevDesc dd) {
    unimplemented("");
}

void GEonExit(void) {
    unimplemented("GEplaySnapshot");
}

void GEnullDevice(void) {
    unimplemented("GEnullDevice");
}

SEXP CreateAtVector(double* x, double* y, int z, Rboolean w) {
    return (SEXP) unimplemented("CreateAtVector");
}

void GAxisPars(double *min, double *max, int *n, Rboolean log, int axis) {
    unimplemented("GAxisPars");
}

pGEDevDesc desc2GEDesc(pDevDesc dd) {
	return (pGEDevDesc) unimplemented("desc2GEDesc");
}

int GEdeviceNumber(pGEDevDesc x) {
    return (int) unimplemented("GEdeviceNumber");
}

pGEDevDesc GEgetDevice(int x) {
	return (pGEDevDesc) unimplemented("GEgetDevice");
}

void GEaddDevice(pGEDevDesc x) {
    unimplemented("GEaddDevice");
}

void GEaddDevice2(pGEDevDesc x, const char *y) {
    unimplemented("GEaddDevice2");
}

void GEkillDevice(pGEDevDesc x) {
    unimplemented("GEkillDevice");
}

pGEDevDesc GEcreateDevDesc(pDevDesc dev) {
    return (pGEDevDesc) unimplemented("");
}

void GEdestroyDevDesc(pGEDevDesc dd) {
    unimplemented("GEdestroyDevDesc");
}

void *GEsystemState(pGEDevDesc dd, int index) {
    return unimplemented("GEsystemState");
}

void GEregisterWithDevice(pGEDevDesc dd) {
    unimplemented("GEregisterWithDevice");
}

void GEregisterSystem(GEcallback callback, int *systemRegisterIndex) {
    // TODO for now we just ignore this to allow the grid package to load
}

void GEunregisterSystem(int registerIndex) {
	// TODO for now we just ignore this to allow the grid package to unload
}

SEXP GEhandleEvent(GEevent event, pDevDesc dev, SEXP data) {
	return (SEXP) unimplemented("GEhandleEvent");
}


double fromDeviceX(double value, GEUnit to, pGEDevDesc dd) {
    unimplemented("fromDeviceX");
    return 0.0;
}

double toDeviceX(double value, GEUnit from, pGEDevDesc dd) {
    unimplemented("toDeviceX");
    return 0.0;
}

double fromDeviceY(double value, GEUnit to, pGEDevDesc dd) {
    unimplemented("fromDeviceY");
    return 0.0;
}

double toDeviceY(double value, GEUnit from, pGEDevDesc dd) {
    unimplemented("toDeviceY");
    return 0.0;
}

double fromDeviceWidth(double value, GEUnit to, pGEDevDesc dd) {
    unimplemented("fromDeviceWidth");
    return 0.0;
}

double toDeviceWidth(double value, GEUnit from, pGEDevDesc dd) {
    unimplemented("toDeviceWidth");
    return 0.0;
}

double fromDeviceHeight(double value, GEUnit to, pGEDevDesc dd) {
    unimplemented("fromDeviceHeight");
    return 0.0;
}

double toDeviceHeight(double value, GEUnit from, pGEDevDesc dd) {
    unimplemented("toDeviceHeight");
    return 0.0;
}

rcolor Rf_RGBpar(SEXP x, int y) {
	return (rcolor) unimplemented("RGBpar");
}

rcolor Rf_RGBpar3(SEXP x, int y, rcolor z) {
    return (rcolor) unimplemented("RGBpar3");
}


const char *Rf_col2name(rcolor col) {
    return (const char *) unimplemented("col2name");
}
