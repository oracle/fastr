#ifndef _DEV_JAVAGD_H
#define _DEV_JAVAGD_H

#define JAVAGD_VER 0x000601 /* JavaGD v0.6-1 */

#ifdef HAVE_CONFIG_H
# include <config.h>
#endif

#include <R.h>
#include <Rversion.h>
#include <Rinternals.h>
#include <R_ext/GraphicsEngine.h>
#include <R_ext/GraphicsDevice.h>

/* for compatibility with older R versions */ 
#if R_GE_version < 4
#include <Rgraphics.h>
#include <Rdevices.h>
#define GEaddDevice(X) addDevice((DevDesc*)(X))
#define GEdeviceNumber(X) devNumber((DevDesc*)(X))
#define GEgetDevice(X) ((GEDevDesc*) GetDevice(X))
#define ndevNumber(X) devNumber((DevDesc*)(X))
#define GEkillDevice(X) KillDevice(X)
#define desc2GEDesc(X) ((DevDesc*) GetDevice(devNumber((DevDesc*) (X))))
#endif
#if R_VERSION >= R_Version(2,8,0)
#ifndef NewDevDesc
#define NewDevDesc DevDesc
#endif
#endif

Rboolean newJavaGDDeviceDriver(NewDevDesc*, const char*, double, double, double);

/********************************************************/
/* Each driver can have its own device-specic graphical */
/* parameters and resources.  these should be wrapped	*/
/* in a structure (like the x11Desc structure below)	*/
/* and attached to the overall device description via	*/
/* the dd->deviceSpecific pointer			*/
/* NOTE that there are generic graphical parameters	*/
/* which must be set by the device driver, but are	*/
/* common to all device types (see Graphics.h)		*/
/* so go in the GPar structure rather than this device- */
/* specific structure					*/
/********************************************************/

typedef struct {
	double ascent;
	double descent;
	double width;
	int face;
	char family[201];
	double cex; 
	double ps;

} CachedMetricInfo;

typedef struct {
	int gdId; 					  // FastR specific
	/*
	 * Caches a metric info for 'M'. It fixes a caching 
	 * problem of GEMetricInfo in engine.c. (FastR specific)
	 */
	CachedMetricInfo *cachedMMetricInfo;

    /* Graphics Parameters */
    /* Local device copy so that we can detect */
    /* when parameter changes. */

    /* cex retained -- its a GRZ way of specifying text size, but
     * its too much work to change at this time (?)
     */
    double cex;				/* Character expansion */
    /* srt removed -- its a GRZ parameter and is not used in devX11.c
     */
    int lty;				/* Line type */
    double lwd;
    int col;				/* Color */
    /* fg and bg removed -- only use col and new param fill
     */
    int fill;
    int canvas;				/* Canvas */
    int fontface;			/* Typeface */
    int fontsize;			/* Size in points */
    int basefontface;			/* Typeface */
    int basefontsize;			/* Size in points */

    /* X11 Driver Specific */
    /* Parameters with copy per X11 device. */

    int windowWidth;			/* Window width (pixels) */
    int windowHeight;			/* Window height (pixels) */
    int resize;				/* Window resized */

    int holdlevel;                      /* current hold level (0=no holding) */
} newJavaGDDesc;

newJavaGDDesc * Rf_allocNewJavaGDDeviceDesc(double ps);
int Rf_setNewJavaGDDeviceData(NewDevDesc *dd, double gamma_fac, newJavaGDDesc *xd);

#endif

