
#include "JavaGDwrappers.h"

extern int javaGDDeviceCounter;

static int gdId;

extern void gdcSetColor(int gdId, int cc);

SEXP api_gdcSetColor(SEXP cc) {
	gdcSetColor(gdId, INTEGER(cc)[0]);
    return R_NilValue;
}

extern void gdcSetFill(int gdId, int cc);

SEXP api_gdcSetFill(SEXP cc) {
	gdcSetFill(gdId, INTEGER(cc)[0]);
    return R_NilValue;
}

extern void gdcSetLine(int gdId, double lwd, int lty);

SEXP api_gdcSetLine(SEXP lwd, SEXP lty) {
	gdcSetLine(gdId, REAL(lwd)[0], INTEGER(lty)[0]);
    return R_NilValue;
}

extern void gdcSetFont(int gdId, double cex, double ps, double lineheight, int fontface, const char* fontfamily);

SEXP api_gdcSetFont(SEXP cex, SEXP ps, SEXP lineheight, SEXP fontface, SEXP fontfamily) {
	gdcSetFont(gdId, REAL(cex)[0], REAL(ps)[0], REAL(lineheight)[0], INTEGER(fontface)[0], CHAR(STRING_ELT(fontfamily, 0)));
    return R_NilValue;
}

extern void gdNewPage(int gdId, int deviceNumber, int pageNumber);

SEXP api_gdNewPage(SEXP deviceNumber, SEXP pageNumber) {
	gdNewPage(gdId, INTEGER(deviceNumber)[0], INTEGER(pageNumber)[0]);
    return R_NilValue;
}

extern void gdActivate(int gdId);

SEXP api_gdActivate() {
	gdActivate(gdId);
    return R_NilValue;
}

extern void gdCircle(int gdId, double x, double y, double r);

SEXP api_gdCircle(SEXP x, SEXP y, SEXP r) {
	gdCircle(gdId, REAL(x)[0], REAL(y)[0], REAL(r)[0]);
    return R_NilValue;
}

extern void gdClip(int gdId, double x0, double x1, double y0, double y1);

SEXP api_gdClip(SEXP x0, SEXP x1, SEXP y0, SEXP y1) {
	gdClip(gdId, REAL(x0)[0], REAL(x1)[0], REAL(y0)[0], REAL(y1)[0]);
    return R_NilValue;
}

extern void gdDeactivate(int gdId);

SEXP api_gdDeactivate() {
	gdDeactivate(gdId);
    return R_NilValue;
}

extern void gdHold(int gdId);

SEXP api_gdHold() {
	gdHold(gdId);
    return R_NilValue;
}

extern void gdFlush(int gdId, int flush);

SEXP api_gdFlush(SEXP flush) {
	gdFlush(gdId, INTEGER(flush)[0]);
    return R_NilValue;
}

extern double* gdLocator(int gdId);

SEXP api_gdLocator() {
	double *ret = gdLocator(gdId);
	if (!ret) {
		return R_NilValue;
	}
	SEXP rets = PROTECT(allocVector(REALSXP, 2));
	REAL(rets)[0] = ret[0];
	REAL(rets)[1] = ret[1];
	UNPROTECT(1);
    return rets;
}

extern void gdLine(int gdId, double x1, double y1, double x2, double y2);

SEXP api_gdLine(SEXP x1, SEXP y1, SEXP x2, SEXP y2) {
	gdLine(gdId, REAL(x1)[0], REAL(y1)[0], REAL(x2)[0], REAL(y2)[0]);
    return R_NilValue;
}

extern void gdMode(int gdId, int mode);

SEXP api_gdMode(SEXP mode) {
	gdMode(gdId, INTEGER(mode)[0]);
    return R_NilValue;
}

extern void gdOpen(int gdId, const char*, double, double);

SEXP api_gdOpen(SEXP name, SEXP w, SEXP h) {
	const char *nm = CHAR(STRING_ELT(name, 0));
	gdId = javaGDDeviceCounter++;
	gdOpen(gdId, nm, REAL(w)[0], REAL(h)[0]);
    return R_NilValue;
}

extern void gdClose(int gdId);

SEXP api_gdClose() {
	gdClose(gdId);
	gdId = -1;
    return R_NilValue;
}

extern void gdPath(int gdId, int npoly, int *nper, int n, double *x, double *y, Rboolean winding);

SEXP api_gdPath(SEXP npoly, SEXP nper, SEXP n, SEXP x, SEXP y, SEXP winding) {
	gdPath(gdId, INTEGER(npoly)[0], INTEGER(nper), INTEGER(npoly)[0], REAL(x), REAL(y), INTEGER(winding)[0]);
    return R_NilValue;
}

extern void gdPolygon(int gdId, int n, double* x, double* y);

SEXP api_gdPolygon(SEXP n, SEXP x, SEXP y) {
	gdPolygon(gdId, INTEGER(n)[0], REAL(x), REAL(y));
    return R_NilValue;
}

SEXP api_gdPolygon2(SEXP n, SEXP x, SEXP y) {
	int len = INTEGER(n)[0];
	double *xx = (double *) malloc(len * sizeof(double));
	memcpy(xx, REAL(x), sizeof(double) * len);
	double *yy = (double *) malloc(len * sizeof(double));
	memcpy(yy, REAL(y), sizeof(double) * len);
	gdPolygon(gdId, len, xx, yy);
    return R_NilValue;
}

extern void gdPolyline(int gdId, int n, double* x, double* y);

SEXP api_gdPolyline(SEXP n, SEXP x, SEXP y) {
	gdPolyline(gdId, INTEGER(n)[0], REAL(x), REAL(y));
    return R_NilValue;
}

extern void gdRect(int gdId, double x0, double y0, double x1, double y1);

SEXP api_gdRect(SEXP x0, SEXP y0, SEXP x1, SEXP y1) {
	gdRect(gdId, REAL(x0)[0], REAL(y0)[0], REAL(x1)[0], REAL(y1)[0]);
    return R_NilValue;
}

extern double* gdSize(int gdId);

SEXP api_gdSize() {
	double *ret = gdSize(gdId);
	if (!ret) {
		return R_NilValue;
	}
	SEXP rets = PROTECT(allocVector(REALSXP, 4));
	REAL(rets)[0] = ret[0];
	REAL(rets)[1] = ret[1];
	REAL(rets)[2] = ret[2];
	REAL(rets)[3] = ret[3];
	UNPROTECT(1);
    return rets;
}

extern double getStrWidth(int gdId, const char* str);

SEXP api_getStrWidth(SEXP str) {
	return ScalarReal(getStrWidth(gdId, CHAR(STRING_ELT(str, 0))));
}

extern void gdText(int gdId, double x, double y, const char* str, double rot, double hadj);

SEXP api_gdText(SEXP x, SEXP y, SEXP str, SEXP rot, SEXP hadj) {
	gdText(gdId, REAL(x)[0], REAL(y)[0], CHAR(STRING_ELT(str, 0)), REAL(rot)[0], REAL(hadj)[0]);
    return R_NilValue;
}

extern void gdRaster(int gdId, unsigned int *img, int img_w, int img_h, double x, double y, double w, double h, double rot, Rboolean interpolate);

SEXP api_gdRaster(SEXP img, SEXP img_w, SEXP img_h, SEXP x, SEXP y, SEXP w, SEXP h, SEXP rot, SEXP interpolate) {
	gdRaster(gdId, INTEGER(img), INTEGER(img_w)[0], INTEGER(img_h)[0], REAL(x)[0], REAL(y)[0], REAL(w)[0], REAL(h)[0], REAL(rot)[0], INTEGER(interpolate)[0]);
    return R_NilValue;
}

double* gdMetricInfo(int gdId, int ch);

SEXP api_gdMetricInfo(SEXP ch) {
	double *ret = gdMetricInfo(gdId, INTEGER(ch)[0]);
	if (!ret) {
		return R_NilValue;
	}
	SEXP rets = PROTECT(allocVector(REALSXP, 3));
	REAL(rets)[0] = ret[0];
	REAL(rets)[1] = ret[1];
	REAL(rets)[2] = ret[2];
	UNPROTECT(1);
    return rets;
}
