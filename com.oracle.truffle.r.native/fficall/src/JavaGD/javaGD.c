/*
 *  R : A Computer Language for Statistical Data Analysis
 *  Copyright (C) 1995, 1996  Robert Gentleman and Ross Ihaka
 *  Copyright (C) 1997--2003  Robert Gentleman, Ross Ihaka and the
 *			      R Development Core Team
 *  Copyright (c) 2020, 2022, Oracle and/or its affiliates
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

#define R_JAVAGD 1
#include "javaGD.h"
#include "jGDtalk.h"

double jGDdpiX = 100.0;
double jGDdpiY = 100.0;
double jGDasp  = 1.0;

/********************************************************/
/* If there are resources that are shared by all devices*/
/* of this type, you may wish to make them globals	*/
/* rather than including them in the device-specific	*/
/* parameters structure (especially if they are large !)*/
/********************************************************/

/* JavaGD Driver Specific parameters
 * with only one copy for all xGD devices */


/*  JavaGD Device Driver Arguments	:	*/
/*	1) display name			*/
/*	2) width (pixels)		*/
/*	3) height (pixels)		*/
/*	4) host to connect to		*/
/*	5) tcp port to connect to	*/

Rboolean newJavaGDDeviceDriver(NewDevDesc *dd,
			    const char *disp_name,
			    double width,
			    double height,
                double initps)
{
  newJavaGDDesc *xd;

#ifdef JGD_DEBUG
  printf("TD: newJavaGDDeviceDriver(\"%s\", %f, %f, %f)\n",disp_name,width,height,initps);
#endif

  xd = Rf_allocNewJavaGDDeviceDesc(initps);
  if (!newJavaGD_Open((NewDevDesc*)(dd), xd, disp_name, width, height)) {
    free(xd);
    return FALSE;
  }
  
  Rf_setNewJavaGDDeviceData((NewDevDesc*)(dd), 0.6, xd);
  
  return TRUE;
}

/**
  This fills the general device structure (dd) with the JavaGD-specific
  methods/functions. It also specifies the current values of the
  dimensions of the device, and establishes the fonts, line styles, etc.
 */
int
Rf_setNewJavaGDDeviceData(NewDevDesc *dd, double gamma_fac, newJavaGDDesc *xd)
{
#ifdef JGD_DEBUG
	printf("Rf_setNewJavaGDDeviceData\n");
#endif

    /*	Set up Data Structures. */
    setupJavaGDfunctions(dd);

    /* Set required graphics parameters. */

    /* Window Dimensions in Pixels */
    /* Initialise the clipping rect too */

    dd->left = dd->clipLeft = 0;			/* left */
    dd->right = dd->clipRight = xd->windowWidth;	/* right */
    dd->bottom = dd->clipBottom = xd->windowHeight;	/* bottom */
    dd->top = dd->clipTop = 0;			/* top */

    /* Nominal Character Sizes in Pixels */

    dd->cra[0] = 8;
    dd->cra[1] = 11;

    /* Character Addressing Offsets */
    /* These are used to plot a single plotting character */
    /* so that it is exactly over the plotting point */

    dd->xCharOffset = 0.4900;
    dd->yCharOffset = 0.3333;
    dd->yLineBias = 0.1;

    /* Inches per raster unit */

    dd->ipr[0] = 1/jGDdpiX;
    dd->ipr[1] = 1/jGDdpiY;
#if R_GE_version < 4
    dd->asp = jGDasp;

    /* Device capabilities */
    dd->canResizePlot = TRUE;
    dd->canChangeFont = TRUE;
    dd->canRotateText = TRUE;
    dd->canResizeText = TRUE;
#endif
    dd->canClip = TRUE;
    dd->canHAdj = 2;
    dd->canChangeGamma = FALSE;

    dd->startps = xd->basefontsize;
    dd->startcol = xd->col;
    dd->startfill = xd->fill;
    dd->startlty = LTY_SOLID;
    dd->startfont = 1;
    dd->startgamma = gamma_fac;

    dd->deviceSpecific = (void *) xd;

#if R_GE_version >= 13
    dd->deviceVersion = R_GE_definitions;
#endif

    dd->displayListOn = TRUE;

    return(TRUE);
}


/**
 This allocates an newJavaGDDesc instance  and sets its default values.
 */
newJavaGDDesc * Rf_allocNewJavaGDDeviceDesc(double ps)
{
    newJavaGDDesc *xd;
    /* allocate new device description */
    if (!(xd = (newJavaGDDesc*)calloc(1, sizeof(newJavaGDDesc))))
	return FALSE;

    /* From here on, if we need to bail out with "error", */
    /* then we must also free(xd). */

    /*	Font will load at first use.  */

    if (ps < 6 || ps > 24) ps = 12;
    xd->fontface = -1;
    xd->fontsize = -1;
    xd->basefontface = 1;
    xd->basefontsize = ps;

    return(xd);
}


typedef Rboolean (*JavaGDDeviceDriverRoutine)(NewDevDesc*, char*, 
					      double, double);

/*
static char *SaveString(SEXP sxp, int offset)
{
    char *s;
    if(!isString(sxp) || length(sxp) <= offset)
	error("invalid string argument");
    s = R_alloc(strlen(CHAR(STRING_ELT(sxp, offset)))+1, sizeof(char));
    strcpy(s, CHAR(STRING_ELT(sxp, offset)));
    return s;
} */

int startsWith(const char *pre, const char *str) {
    size_t lenpre = strlen(pre), lenstr = strlen(str);
    return lenstr < lenpre ? 0 : memcmp(pre, str, lenpre) == 0;
}

static GEDevDesc* 
Rf_addJavaGDDevice(const char *display, double width, double height, double initps)
{
    NewDevDesc *dev = NULL;
    GEDevDesc *dd;
    
    char *devname;
    if (startsWith("png::", display)) {
		devname ="PNG";
    } else if (startsWith("jpeg::", display)) {
		devname ="JPEG";
    } else if (startsWith("bmp::", display)) {
		devname ="BMP";
    } else if (startsWith("svg::", display)) {
		devname ="SVG";
    } else {
		devname ="JavaGD";
    }

    R_CheckDeviceAvailable();
#ifdef BEGIN_SUSPEND_INTERRUPTS
    BEGIN_SUSPEND_INTERRUPTS {
#endif
	/* Allocate and initialize the device driver data */
	if (!(dev = (NewDevDesc*)calloc(1, sizeof(NewDevDesc))))
	    return 0;
	/* Do this for early redraw attempts */
#if R_GE_version < 4
	dev->displayList = R_NilValue;
	dev->newDevStruct = 1;
	/* Make sure that this is initialised before a GC can occur.
	 * This (and displayList) get protected during GC
	 */
	dev->savedSnapshot = R_NilValue;
#endif
	/* Took out the GInit because MOST of it is setting up
	 * R base graphics parameters.  
	 * This is supposed to happen via addDevice now.
	 */
	if (!newJavaGDDeviceDriver(dev, display, width, height, initps))
	  {
	    free(dev);
		error("unable to start device %s", devname);
	    return 0;
	  }
	gsetVar(install(".Device"), mkString(devname), R_NilValue);
	dd = GEcreateDevDesc(dev);
	GEaddDevice(dd);
	GEinitDisplayList(dd);
#ifdef JGD_DEBUG
	printf("JavaGD> devNum=%d, dd=%lx\n", ndevNumber(dd), (unsigned long)dd);
#endif
#ifdef BEGIN_SUSPEND_INTERRUPTS
    } END_SUSPEND_INTERRUPTS;
#endif
    
    return(dd);
}

void resizedJavaGD(NewDevDesc *dd);

void reloadJavaGD(int *dn) {
	GEDevDesc *gd= GEgetDevice(*dn);
	if (gd) {
		NewDevDesc *dd=gd->dev;
#ifdef JGD_DEBUG
		printf("reloadJavaGD: dn=%d, dd=%lx\n", *dn, (unsigned long)dd);
#endif
		if (dd) resizedJavaGD(dd);
	}
}

// SEXP javaGDobjectCall(SEXP dev) {
//   int ds=NumDevices();
//   int dn;
//   GEDevDesc *gd;
//   void *ptr=0;
// 
//   dn = asInteger(dev);
//   if (dn < 0 || dn >= ds) return R_NilValue;
//   gd=GEgetDevice(dn);
//   if (gd) {
//     NewDevDesc *dd=gd->dev;
//     if (dd) {
//       newJavaGDDesc *xd=(newJavaGDDesc*) dd->deviceSpecific;
//       if (xd) ptr = xd->talk;
//     }
//   }
//   if (!ptr) return R_NilValue;
//   return R_MakeExternalPtr(ptr, R_NilValue, R_NilValue);
// }

static void javaGDresize_(int dev) {
    int ds = NumDevices();
    int i = 0;
    if (dev >= 0 && dev < ds) {
	i = dev;
	ds = dev + 1;
    }
    while (i < ds) {
        GEDevDesc *gd = GEgetDevice(i);
        if (gd) {
            NewDevDesc *dd = gd->dev;
#ifdef JGD_DEBUG
            printf("javaGDresize: device=%d, dd=%lx\n", i, (unsigned long)dd);
#endif
            if (dd) {
#ifdef JGD_DEBUG
                printf("dd->size=%lx\n", (unsigned long)dd->size);
#endif
                dd->size(&(dd->left), &(dd->right), &(dd->bottom), &(dd->top), dd);
                
                Rboolean record = gd->recordGraphics;
    			gd->recordGraphics = FALSE;
                GEplayDisplayList(gd);
    			gd->recordGraphics = record;
            }
        }
        i++;
    }
}

/* NOTE: we have to keep this for compatibility since it is referenced in Java code */
void javaGDresize(int *dev) {
    if (dev) javaGDresize_(*dev);
}

SEXP javaGDresizeCall(SEXP dev) {
    javaGDresize_(asInteger(dev));
    return dev;
}

void resizedJavaGD(NewDevDesc *dd) {
	int devNum;
	/* newJavaGDDesc *xd = (newJavaGDDesc *) dd->deviceSpecific; */
#ifdef JGD_DEBUG
	printf("dd->size=%lx\n", (unsigned long)dd->size);
#endif
	dd->size(&(dd->left), &(dd->right), &(dd->bottom), &(dd->top), dd);
	devNum = ndevNumber(dd);
	if (devNum > 0)
		GEplayDisplayList(GEgetDevice(devNum));
}

SEXP newJavaGD(SEXP name, SEXP sw, SEXP sh, SEXP sps) {
    double w = isNull(sw) ? 400 : asReal(sw), h = isNull(sh) ? 300 : asReal(sh), ps = isNull(sps) ? 12 : asReal(sps);
    w = ISNA(w) ? 400 : w;
    h = ISNA(h) ? 300 : h;
    ps = ISNA(ps) ? 12 : ps;
    if (TYPEOF(name) != STRSXP || LENGTH(name) < 1)
        Rf_error("invalid name");
    if (ISNAN(w) || w <= 0.0 || ISNAN(h) || h <= 0.0 || ISNAN(ps) || ps <= 0.0)
        Rf_error("invalid width, height or ps");
    Rf_addJavaGDDevice(CHAR(STRING_ELT(name, 0)), w, h, ps);
    return name;
}

SEXP do_X11(SEXP call, SEXP op, SEXP args, SEXP rho) {
	SEXP name, sw, sh, sps;

	name = CAR(args);
  	sw = CADR(args);
  	sh = CADDR(args);
	sps = CADDDR(args);
	return newJavaGD(name, sw, sh, sps);
}

SEXP javaGDgetSize(SEXP sDev) {
    SEXP res = R_NilValue;
    int dev = asInteger(sDev);
    int ds = NumDevices();
    if (dev < 0 || dev >= ds)
	Rf_error("invalid device");    
    {
        GEDevDesc *gd = GEgetDevice(dev);
        if (gd) {
            NewDevDesc *dd = gd->dev;
            /*
             if (dd) {
                 newJavaGDDesc *xd=(newJavaGDDesc*) dd->deviceSpecific;
                 if (xd) *obj=(int) xd->talk;
             }
             */
			if (dd) {
			    res = PROTECT(mkNamed(VECSXP, (const char*[]) {
					"x", "y", "width", "height", "dpiX", "dpiY", ""}));
			    SET_VECTOR_ELT(res, 0, ScalarReal(dd->left));
			    SET_VECTOR_ELT(res, 1, ScalarReal(dd->top));
			    SET_VECTOR_ELT(res, 2, ScalarReal(dd->right - dd->left));
			    SET_VECTOR_ELT(res, 3, ScalarReal(dd->bottom - dd->top));
			    SET_VECTOR_ELT(res, 4, ScalarReal(jGDdpiX));
			    SET_VECTOR_ELT(res, 5, ScalarReal(jGDdpiY));
			    UNPROTECT(1);
			} else {
#ifdef JGD_DEBUG
				printf("sizefailed>> device=%d, gd=%lx, dd=%lx\n", dev,
				       (unsigned long)gd, (unsigned long)dd);
#endif
			}	
        }
    }
    return res;
}

SEXP javaGDsetDisplayParam(SEXP pars) {
    int n;
    double *par;
    if (TYPEOF(pars) != REALSXP)
	pars = coerceVector(pars, REALSXP);
    par = REAL(pars);
    n = LENGTH(pars);
    if (n > 0) jGDdpiX = par[0];
    if (n > 1) jGDdpiY = par[1];
    if (n > 2) jGDasp  = par[2];
    return pars;
}

SEXP javaGDgetDisplayParam() {
    SEXP res = mkNamed(REALSXP, (const char*[]) { "dpiX", "dpiY", "aspect", "" });
    double *par = REAL(res);
    par[0] = jGDdpiX;
    par[1] = jGDdpiY;
    par[2] = jGDasp;
    return res;
}

SEXP javaGDversion() {
    return ScalarInteger(JAVAGD_VER);
}
