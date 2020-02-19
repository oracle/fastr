#ifndef __FASTR_UPCALLS_H__
#define __FASTR_UPCALLS_H__

#include <Rdefines.h>

void gdcSetColor(int, int);
void gdcSetFill(int, int);
void gdcSetLine(int, double, int);
void gdcSetFont(int, double, double, double, int, const char *);
void gdNewPage(int, int, int);
void gdActivate(int);
void gdCircle(int, double, double, double);
void gdClip(int, double, double, double, double);
void gdClose(int);
void gdDeactivate(int);
void gdHold(int);
void gdFlush(int, int);
double* gdLocator();
void gdLine(int, double, double, double, double);
void gdMode(int, int);
void gdOpen(int, const char*, double, double);
void gdPath(int, int, int*, int, double*, double*, Rboolean);
void gdPolygon(int, int, double*, double*);
void gdPolyline(int, int, double*, double*);
void gdRect(int, double, double, double, double);
double* gdSize(int);
double getStrWidth(int, const char*);
void gdText(int, double, double, const char*, double, double);
void gdRaster(int, unsigned int *, int, int, double, double, double, double, double, Rboolean);
double* gdMetricInfo(int, int);

#endif // __FASTR_UPCALLS_H__