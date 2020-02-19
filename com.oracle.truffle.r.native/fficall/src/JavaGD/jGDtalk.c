#include "javaGD.h"
#include "jGDtalk.h"
#include "fastrUpcalls.h"
#include <Rdefines.h>
#include <string.h>

int initJavaGD(newJavaGDDesc* xd);

char *symbol2utf8(const char *c); /* from s2u.c */

/* Device Driver Actions */

#ifdef JGD_DEBUG
#define gdWarning(S) { printf("[javaGD warning] %s\n", S); }
#else
#define gdWarning(S)
#endif

#if R_VERSION < 0x10900
#error This JavaGD needs at least R version 1.9.0
#endif

#if R_VERSION >= R_Version(2,7,0)
#define constxt const
#else
#define constxt
#endif

/* the internal representation of a color in this API is RGBa with a=0 meaning transparent and a=255 meaning opaque (hence a means 'opacity'). previous implementation was different (inverse meaning and 0x80 as NA), so watch out. */
#if R_VERSION < 0x20000
#define CONVERT_COLOR(C) ((((C)==0x80000000) || ((C)==-1))?0:(((C)&0xFFFFFF)|((0xFF000000-((C)&0xFF000000)))))
#else
#define CONVERT_COLOR(C) (C)
#endif

static void newJavaGD_Activate(NewDevDesc *dd);
static void newJavaGD_Circle(double x, double y, double r,
			  R_GE_gcontext *gc,
			  NewDevDesc *dd);
static void newJavaGD_Clip(double x0, double x1, double y0, double y1,
			NewDevDesc *dd);
static void newJavaGD_Close(NewDevDesc *dd);
static void newJavaGD_Deactivate(NewDevDesc *dd);
static void newJavaGD_Hold(NewDevDesc *dd);
static Rboolean newJavaGD_Locator(double *x, double *y, NewDevDesc *dd);
static void newJavaGD_Line(double x1, double y1, double x2, double y2,
			R_GE_gcontext *gc,
			NewDevDesc *dd);
static void newJavaGD_MetricInfo(int c, 
			      R_GE_gcontext *gc,
			      double* ascent, double* descent,
			      double* width, NewDevDesc *dd);
static void newJavaGD_Mode(int mode, NewDevDesc *dd);
static void newJavaGD_NewPage(R_GE_gcontext *gc, NewDevDesc *dd);
Rboolean newJavaGD_Open(NewDevDesc *dd, newJavaGDDesc *xd,
		     const char *dsp, double w, double h);
static void newJavaGD_Path(double *x, double *y, int npoly, int *nper, Rboolean winding,
                           R_GE_gcontext *gc, NewDevDesc *dd);
static void newJavaGD_Polygon(int n, double *x, double *y,
			   R_GE_gcontext *gc,
			   NewDevDesc *dd);
static void newJavaGD_Polyline(int n, double *x, double *y,
			     R_GE_gcontext *gc,
			     NewDevDesc *dd);
static void newJavaGD_Rect(double x0, double y0, double x1, double y1,
			 R_GE_gcontext *gc,
			 NewDevDesc *dd);
static void newJavaGD_Size(double *left, double *right,
			 double *bottom, double *top,
			 NewDevDesc *dd);
static double newJavaGD_StrWidth(constxt char *str, 
			       R_GE_gcontext *gc,
			       NewDevDesc *dd);
static double newJavaGD_StrWidthUTF8(constxt char *str, 
			       R_GE_gcontext *gc,
			       NewDevDesc *dd);
static void newJavaGD_Text(double x, double y, constxt char *str,
			 double rot, double hadj,
			 R_GE_gcontext *gc,
			 NewDevDesc *dd);
static void newJavaGD_TextUTF8(double x, double y, constxt char *str,
			 double rot, double hadj,
			 R_GE_gcontext *gc,
			 NewDevDesc *dd);
static void newJavaGD_Raster(unsigned int *raster, int w, int h,
			   double x, double y, double width, double height,
			   double rot, Rboolean interpolate,
			   R_GE_gcontext *gc, NewDevDesc *dd);

static R_GE_gcontext lastGC; /** last graphics context. the API send changes, not the entire context, so we cache it for comparison here */

	
#define checkGC(xd,gc) sendGC(xd,gc,0)

/** check changes in GC and issue corresponding commands if necessary */
static void sendGC(newJavaGDDesc *xd, R_GE_gcontext *gc, int sendAll) {
    if (sendAll || gc->col != lastGC.col) {
//         mid = (*env)->GetMethodID(env, xd->talkClass, "gdcSetColor", "(I)V");
//         if (mid) (*env)->CallVoidMethod(env, xd->talk, mid, CONVERT_COLOR(gc->col));
//         else gdWarning("checkGC.gdcSetColor: can't get mid");
// 		chkX(env);
		gdcSetColor(xd->gdId, CONVERT_COLOR(gc->col));
    }

    if (sendAll || gc->fill != lastGC.fill)  {
//         mid = (*env)->GetMethodID(env, xd->talkClass, "gdcSetFill", "(I)V");
//         if (mid) (*env)->CallVoidMethod(env, xd->talk, mid, CONVERT_COLOR(gc->fill));
//         else gdWarning("checkGC.gdcSetFill: can't get mid");
// 		chkX(env);
		gdcSetFill(xd->gdId, CONVERT_COLOR(gc->fill));
    }

    if (sendAll || gc->lwd != lastGC.lwd || gc->lty != lastGC.lty) {
//         mid = (*env)->GetMethodID(env, xd->talkClass, "gdcSetLine", "(DI)V");
//         if (mid) (*env)->CallVoidMethod(env, xd->talk, mid, gc->lwd, gc->lty);
//         else gdWarning("checkGC.gdcSetLine: can't get mid");
// 		chkX(env);
		gdcSetLine(xd->gdId, gc->lwd, gc->lty);
    }

    if (sendAll || gc->cex!=lastGC.cex || gc->ps!=lastGC.ps || gc->lineheight!=lastGC.lineheight || gc->fontface!=lastGC.fontface || strcmp(gc->fontfamily, lastGC.fontfamily)) {
//         jstring s = (*env)->NewStringUTF(env, gc->fontfamily);
//         mid = (*env)->GetMethodID(env, xd->talkClass, "gdcSetFont", "(DDDILjava/lang/String;)V");
//         if (mid) (*env)->CallVoidMethod(env, xd->talk, mid, gc->cex, gc->ps, gc->lineheight, gc->fontface, s);
//         else gdWarning("checkGC.gdcSetFont: can't get mid");
// 		chkX(env);
		gdcSetFont(xd->gdId, gc->cex, gc->ps, gc->lineheight, gc->fontface, gc->fontfamily);
    }
    memcpy(&lastGC, gc, sizeof(lastGC));
}

/* re-set the GC - i.e. send commands for all monitored GC entries */
static void sendAllGC(newJavaGDDesc *xd, R_GE_gcontext *gc) {
    /*
    printf("Basic GC:\n col=%08x\n fill=%08x\n gamma=%f\n lwd=%f\n lty=%08x\n cex=%f\n ps=%f\n lineheight=%f\n fontface=%d\n fantfamily=\"%s\"\n\n",
	 gc->col, gc->fill, gc->gamma, gc->lwd, gc->lty,
	 gc->cex, gc->ps, gc->lineheight, gc->fontface, gc->fontfamily);
     */
    sendGC(xd, gc, 1);
}

/*------- the R callbacks begin here ... ------------------------*/

static void newJavaGD_Activate(NewDevDesc *dd)
{
    newJavaGDDesc *xd = (newJavaGDDesc *) dd->deviceSpecific;
    
    if(!xd) return;
    
//     mid = (*env)->GetMethodID(env, xd->talkClass, "gdActivate", "()V");
//     if (mid) (*env)->CallVoidMethod(env, xd->talk, mid);
// 	chkX(env);
	gdActivate(xd->gdId);
}

static void newJavaGD_Circle(double x, double y, double r,  R_GE_gcontext *gc,  NewDevDesc *dd)
{
    newJavaGDDesc *xd = (newJavaGDDesc *) dd->deviceSpecific;
    
    if(!xd) return;

    checkGC(xd, gc);

//    mid = (*env)->GetMethodID(env, xd->talkClass, "gdCircle", "(DDD)V");
//    if (mid) (*env)->CallVoidMethod(env, xd->talk, mid, x, y, r);
//	chkX(env);
	gdCircle(xd->gdId, x, y, r);
}

static void newJavaGD_Clip(double x0, double x1, double y0, double y1,  NewDevDesc *dd)
{
    newJavaGDDesc *xd = (newJavaGDDesc *) dd->deviceSpecific;
    
    if(!xd) return;
       
//    mid = (*env)->GetMethodID(env, xd->talkClass, "gdClip", "(DDDD)V");
//    if (mid) (*env)->CallVoidMethod(env, xd->talk, mid, x0, x1, y0, y1);
//	chkX(env);
	gdClip(xd->gdId, x0, x1, y0, y1);
}

static void newJavaGD_Close(NewDevDesc *dd)
{
    newJavaGDDesc *xd = (newJavaGDDesc *) dd->deviceSpecific;
    
    if(!xd) return;
    
//    mid = (*env)->GetMethodID(env, xd->talkClass, "gdClose", "()V");
//    if (mid) (*env)->CallVoidMethod(env, xd->talk, mid);
//	chkX(env);
	gdClose(xd->gdId);
}

static void newJavaGD_Deactivate(NewDevDesc *dd)
{
    newJavaGDDesc *xd = (newJavaGDDesc *) dd->deviceSpecific;
    
    if(!xd) return;
    
//    mid = (*env)->GetMethodID(env, xd->talkClass, "gdDeactivate", "()V");
//    if (mid) (*env)->CallVoidMethod(env, xd->talk, mid);
//	chkX(env);
	gdDeactivate(xd->gdId);
}

static void newJavaGD_Hold(NewDevDesc *dd)
{
    newJavaGDDesc *xd = (newJavaGDDesc *) dd->deviceSpecific;

    if(!xd) return;

//    mid = (*env)->GetMethodID(env, xd->talkClass, "gdHold", "()V");
//    if (mid) (*env)->CallVoidMethod(env, xd->talk, mid);
//    chkX(env);
	gdHold(xd->gdId);
}

static int  newJavaGD_HoldFlush(NewDevDesc *dd, int level)
{
    int ol;
    newJavaGDDesc *xd = (newJavaGDDesc *) dd->deviceSpecific;
    
    if (!xd) return 0;
    ol = xd->holdlevel;
    xd->holdlevel += level;
    if (xd->holdlevel < 0)
	xd->holdlevel = 0;
    
//    mid = (*env)->GetMethodID(env, xd->talkClass, "gdFlush", "(Z)V");
//    if (mid) {
	if (xd->holdlevel == 0) /* flush */
//	    (*env)->CallVoidMethod(env, xd->talk, mid, 1);
		gdFlush(xd->gdId, 1);
	else if (ol == 0) /* first hold */
//	    (*env)->CallVoidMethod(env, xd->talk, mid, 0);
		gdFlush(xd->gdId, 0);
//	chkX(env);
//    }
    return xd->holdlevel;
}

static Rboolean newJavaGD_Locator(double *x, double *y, NewDevDesc *dd)
{
    newJavaGDDesc *xd = (newJavaGDDesc *) dd->deviceSpecific;
    
    if(!xd) return FALSE;
    
//     mid = (*env)->GetMethodID(env, xd->talkClass, "gdLocator", "()[D");
//     if (mid) {
//         jobject o=(*env)->CallObjectMethod(env, xd->talk, mid);
//         if (o) {
//             jdouble *ac=(jdouble*)(*env)->GetDoubleArrayElements(env, o, 0);
//             if (!ac) {
// 	      (*env)->DeleteLocalRef(env, o);
// 	      return FALSE;
// 	    }
//             *x=ac[0]; *y=ac[1];
//             (*env)->ReleaseDoubleArrayElements(env, o, ac, 0);
// 	    (*env)->DeleteLocalRef(env, o);
// 	    chkX(env);
//             return TRUE;
//         }        
//     }
// 	chkX(env);
    double* ac = gdLocator(xd->gdId);
    if (!ac)
    	return FALSE;

    *x=ac[0]; *y=ac[1];
    
    return TRUE;
}

static void newJavaGD_Line(double x1, double y1, double x2, double y2,  R_GE_gcontext *gc,  NewDevDesc *dd)
{
    newJavaGDDesc *xd = (newJavaGDDesc *) dd->deviceSpecific;
    
    if(!xd) return;
    
    checkGC(xd, gc);
    
//    mid = (*env)->GetMethodID(env, xd->talkClass, "gdLine", "(DDDD)V");
//    if (mid) (*env)->CallVoidMethod(env, xd->talk, mid, x1, y1, x2, y2);
//	chkX(env);
	gdLine(xd->gdId, x1, y1, x2, y2);
}

static void newJavaGD_MetricInfo(int c,  R_GE_gcontext *gc,  double* ascent, double* descent,  double* width, NewDevDesc *dd)
{
    newJavaGDDesc *xd = (newJavaGDDesc *) dd->deviceSpecific;
    
    if(!xd) return;
    
    if (abs(c) == 77 && xd->cachedMMetricInfo
	    && gc->cex == xd->cachedMMetricInfo->cex 
	    && gc->ps == xd->cachedMMetricInfo->ps
	    && gc->fontface == xd->cachedMMetricInfo->face
	    && strcmp(gc->fontfamily, xd->cachedMMetricInfo->family) == 0) {
	    *ascent = xd->cachedMMetricInfo->ascent; 
	    *descent = xd->cachedMMetricInfo->descent; 
	    *width = xd->cachedMMetricInfo->width; 
	    return;
	}

    checkGC(xd, gc);
    
    if(c <0) c = -c;
//     mid = (*env)->GetMethodID(env, xd->talkClass, "gdMetricInfo", "(I)[D");
//     if (mid) {
//         jobject o=(*env)->CallObjectMethod(env, xd->talk, mid, c);
//         if (o) {
//             jdouble *ac=(jdouble*)(*env)->GetDoubleArrayElements(env, o, 0);
//             if (!ac) {
// 	      (*env)->DeleteLocalRef(env, o);
// 	      return;
// 	    }
//             *ascent=ac[0]; *descent=ac[1]; *width=ac[2];
//             (*env)->ReleaseDoubleArrayElements(env, o, ac, 0);
// 	    (*env)->DeleteLocalRef(env, o);
//         }        
//     }
// 	chkX(env);
	double* ac = gdMetricInfo(xd->gdId, c);
	if (ac) {
		*ascent=ac[0]; *descent=ac[1]; *width=ac[2];
	}

	// Cache metric for 'M'
	if(abs(c) == 77) {
		CachedMetricInfo *cmi;
		cmi = (CachedMetricInfo*)calloc(1, sizeof(CachedMetricInfo));
	    cmi->cex = gc->cex;
	    cmi->ps = gc->ps;
	    cmi->face = gc->fontface;
		strcpy(cmi->family, gc->fontfamily);
	    cmi->ascent = *ascent; 
	    cmi->descent = *descent; 
	    cmi->width = *width;
		xd->cachedMMetricInfo = cmi;
	}
}

static void newJavaGD_Mode(int mode, NewDevDesc *dd)
{
    newJavaGDDesc *xd = (newJavaGDDesc *) dd->deviceSpecific;
    
    if(!xd) return;
    
//    mid = (*env)->GetMethodID(env, xd->talkClass, "gdMode", "(I)V");
//    if (mid) (*env)->CallVoidMethod(env, xd->talk, mid, mode);
//	chkX(env);
	gdMode(xd->gdId, mode);
}

static void newJavaGD_NewPage(R_GE_gcontext *gc, NewDevDesc *dd)
{
    newJavaGDDesc *xd = (newJavaGDDesc *) dd->deviceSpecific;
    int devNr;
    
    if(!xd) return;
    
    devNr = ndevNumber(dd);

// 	mid = (*env)->GetMethodID(env, xd->talkClass, "gdNewPage", "(I)V");
// 	if (mid) (*env)->CallVoidMethod(env, xd->talk, mid, devNr);
// 	chkX(env);
	gdNewPage(xd->gdId, devNr, -1);

    /* this is an exception - we send all GC attributes just after the NewPage command */
    sendAllGC(xd, gc);
}

int javaGDDeviceCounter = 0;

Rboolean newJavaGD_Open(NewDevDesc *dd, newJavaGDDesc *xd, const char *dsp, double w, double h)
{   
    if (initJavaGD(xd)) return FALSE;
    
    // FastR specific
    xd->gdId = javaGDDeviceCounter++;
    
    xd->fill = 0x00ffffff; /* transparent, was R_RGB(255, 255, 255); */
    xd->col = R_RGB(0, 0, 0);
    xd->canvas = R_RGB(255, 255, 255);
    xd->windowWidth = w;
    xd->windowHeight = h;
    xd->holdlevel = 0;
        
    {
        if(!xd) {
            gdWarning("gdOpen: xd or talk is null");
            return FALSE;
        }
        
        /* we're not using dsp atm! */
//         mid = (*env)->GetMethodID(env, xd->talkClass, "gdOpen", "(DD)V");
//         if (mid)
//             (*env)->CallVoidMethod(env, xd->talk, mid, w, h);
//         else {
//             gdWarning("gdOpen: can't get mid");
// 			chkX(env);
//             return FALSE;
//         }
// 		chkX(env);
		gdOpen(xd->gdId, dsp, w, h);
    }
    
    return TRUE;
}

static double* newDoubleArray(int n, double *ct)
{
	double *dae = (double *) malloc(n * sizeof(double));
	memcpy(dae, ct, sizeof(double) * n);
	return dae;
}

static void newJavaGD_Path(double *x, double *y, int npoly, int *nper, Rboolean winding,
        R_GE_gcontext *gc, NewDevDesc *dd)
{
    newJavaGDDesc *xd = (newJavaGDDesc *) dd->deviceSpecific;
    int* na;
    double *xa, *ya;
    int n;

    if (!xd) return;

    checkGC(xd, gc);

    na = (int *) malloc(npoly * sizeof(int));
    if (!na) return;
    //(*env)->SetIntArrayRegion(env, na, 0, npoly, (jint *) nper);
	memcpy(na, nper, sizeof(int) * npoly);
    n = 0;
    for (int i = 0; i < npoly; ++i)
        n += nper[i];
    xa = newDoubleArray(n, x);
    if (!xa) return;
    ya = newDoubleArray(n, y);
    if (!ya) return;
    
//     mid = (*env)->GetMethodID(env, xd->talkClass, "gdPath", "(I[I[D[DZ)V");
//     if (mid) (*env)->CallVoidMethod(env, xd->talk, mid, (jint) npoly, na, xa, ya, winding);
//     (*env)->DeleteLocalRef(env, na);
//     (*env)->DeleteLocalRef(env, xa); 
//     (*env)->DeleteLocalRef(env, ya);
//     chkX(env);
	gdPath(xd->gdId, npoly, na, n, xa, ya, winding);
}

static void newJavaGD_Polygon(int n, double *x, double *y,  R_GE_gcontext *gc,  NewDevDesc *dd)
{
    newJavaGDDesc *xd = (newJavaGDDesc *) dd->deviceSpecific;
    double *xa, *ya;
    
    if(!xd) return;

    checkGC(xd, gc);

    xa = newDoubleArray(n, x);
    if (!xa) return;
    ya = newDoubleArray(n, y);
    if (!ya) return;
    
//     mid = (*env)->GetMethodID(env, xd->talkClass, "gdPolygon", "(I[D[D)V");
//     if (mid) (*env)->CallVoidMethod(env, xd->talk, mid, n, xa, ya);
//     (*env)->DeleteLocalRef(env, xa); 
//     (*env)->DeleteLocalRef(env, ya);
//     chkX(env);
	gdPolygon(xd->gdId, n, xa, ya);
}

static void newJavaGD_Polyline(int n, double *x, double *y,  R_GE_gcontext *gc,  NewDevDesc *dd)
{
    newJavaGDDesc *xd = (newJavaGDDesc *) dd->deviceSpecific;
    double *xa, *ya;
    
    if(!xd) return;
    
    checkGC(xd, gc);
    
    xa = newDoubleArray(n, x);
    if (!xa) return;
    ya = newDoubleArray(n, y);
    if (!ya) return;
    
//     mid = (*env)->GetMethodID(env, xd->talkClass, "gdPolyline", "(I[D[D)V");
//     if (mid) (*env)->CallVoidMethod(env, xd->talk, mid, n, xa, ya);
//     else gdWarning("gdPolyline: can't get mid ");
//     (*env)->DeleteLocalRef(env, xa); 
//     (*env)->DeleteLocalRef(env, ya);
//     chkX(env);
	gdPolyline(xd->gdId, n, xa, ya);
}

static void newJavaGD_Rect(double x0, double y0, double x1, double y1,  R_GE_gcontext *gc,  NewDevDesc *dd)
{
    newJavaGDDesc *xd = (newJavaGDDesc *) dd->deviceSpecific;
    
    if(!xd) return;
    
    checkGC(xd, gc);
    
//     mid = (*env)->GetMethodID(env, xd->talkClass, "gdRect", "(DDDD)V");
//     if (mid) (*env)->CallVoidMethod(env, xd->talk, mid, x0, y0, x1, y1);
//     else gdWarning("gdRect: can't get mid ");
// 	chkX(env);
	gdRect(xd->gdId, x0, y0, x1, y1);
}

static void newJavaGD_Size(double *left, double *right,  double *bottom, double *top,  NewDevDesc *dd)
{
    newJavaGDDesc *xd = (newJavaGDDesc *) dd->deviceSpecific;
    
    if(!xd) return;
    
//     mid = (*env)->GetMethodID(env, xd->talkClass, "gdSize", "()[D");
//     if (mid) {
//         jobject o=(*env)->CallObjectMethod(env, xd->talk, mid);
//         if (o) {
//             jdouble *ac=(jdouble*)(*env)->GetDoubleArrayElements(env, o, 0);
//             if (!ac) {
// 	      (*env)->DeleteLocalRef(env, o);
// 	      gdWarning("gdSize: cant's get double*");
// 	      return;
// 	    }
//             *left=ac[0]; *right=ac[1]; *bottom=ac[2]; *top=ac[3];
//             (*env)->ReleaseDoubleArrayElements(env, o, ac, 0);
// 	    (*env)->DeleteLocalRef(env, o);
//         } else gdWarning("gdSize: gdSize returned null");
//     }
//     else gdWarning("gdSize: can't get mid ");
// 	chkX(env);
	double* ac = gdSize(xd->gdId);
	if (ac) {
		*left=ac[0]; *right=ac[1]; *bottom=ac[2]; *top=ac[3];
	}
}

static constxt char *convertToUTF8(constxt char *str, R_GE_gcontext *gc)
{
    if (gc->fontface == 5) /* symbol font needs re-coding to UTF-8 */
	str = symbol2utf8(str);
#ifdef translateCharUTF8
    else { /* first check whether we are dealing with non-ASCII at all */
	int ascii = 1;
	constxt unsigned char *c = (constxt unsigned char*) str;
	while (*c) { if (*c > 127) { ascii = 0; break; } c++; }
	if (!ascii) /* non-ASCII, we need to convert it to UTF8 */
	    str = translateCharUTF8(mkCharCE(str, CE_NATIVE));
    }
#endif
    return str;
}

static double newJavaGD_StrWidth(constxt char *str,  R_GE_gcontext *gc,  NewDevDesc *dd)
{
    return newJavaGD_StrWidthUTF8(convertToUTF8(str, gc), gc, dd);
}

static double newJavaGD_StrWidthUTF8(constxt char *str,  R_GE_gcontext *gc,  NewDevDesc *dd)
{
    newJavaGDDesc *xd = (newJavaGDDesc *) dd->deviceSpecific;
    const char* s;
    double res = 0.0;
    
    if(!xd) return 0.0;
    
    checkGC(xd, gc);
    
    //s = (*env)->NewStringUTF(env, str);
    s = str;
//     mid = (*env)->GetMethodID(env, xd->talkClass, "gdStrWidth", "(Ljava/lang/String;)D");
//     if (mid) res = (*env)->CallDoubleMethod(env, xd->talk, mid, s);
//     (*env)->DeleteLocalRef(env, s);
//     chkX(env);
	res = getStrWidth(xd->gdId, s);
    return res;
}

static void newJavaGD_Text(double x, double y, constxt char *str,  double rot, double hadj,  R_GE_gcontext *gc,  NewDevDesc *dd)
{
    newJavaGD_TextUTF8(x, y, convertToUTF8(str, gc), rot, hadj, gc, dd);
}

static void newJavaGD_TextUTF8(double x, double y, constxt char *str,  double rot, double hadj,  R_GE_gcontext *gc,  NewDevDesc *dd)
{
    newJavaGDDesc *xd = (newJavaGDDesc *) dd->deviceSpecific;
    const char* s;
    
    if(!xd) return;
        
    checkGC(xd, gc);
    
    //s = (*env)->NewStringUTF(env, str);
    s = str;
//     mid = (*env)->GetMethodID(env, xd->talkClass, "gdText", "(DDLjava/lang/String;DD)V");
//     if (mid)
//         (*env)->CallVoidMethod(env, xd->talk, mid, x, y, s, rot, hadj);
//     (*env)->DeleteLocalRef(env, s);  
//     chkX(env);
	gdText(xd->gdId, x, y, s, rot, hadj);
}

static void newJavaGD_Raster(unsigned int *raster, int w, int h,
			   double x, double y, double width, double height,
			   double rot, Rboolean interpolate,
			   R_GE_gcontext *gc, NewDevDesc *dd)
{
    newJavaGDDesc *xd = (newJavaGDDesc *) dd->deviceSpecific;

    if(!xd) return;
    checkGC(xd, gc);
//     mid = (*env)->GetMethodID(env, xd->talkClass, "gdRaster", "([BIIDDDDDZ)V");
//     if (mid) {
// 	jbyteArray img = (*env)->NewByteArray(env, w * h * 4);
// 	(*env)->SetByteArrayRegion(env, img, 0, w * h * 4, (jbyte*) raster);
//         (*env)->CallVoidMethod(env, xd->talk, mid, img, w, h, x, y, width, height, rot, interpolate);
// 	(*env)->DeleteLocalRef(env, img);
//     }
//     chkX(env);

	gdRaster(xd->gdId, raster, w, h, x, y, width, height, rot, interpolate);
}


/*-----------------------------------------------------------------------*/

/** fill the R device structure with callback functions */
void setupJavaGDfunctions(NewDevDesc *dd) {
    dd->close = newJavaGD_Close;
    dd->activate = newJavaGD_Activate;
    dd->deactivate = newJavaGD_Deactivate;
    dd->size = newJavaGD_Size;
    dd->newPage = newJavaGD_NewPage;
    dd->clip = newJavaGD_Clip;
    dd->strWidth = newJavaGD_StrWidth;
    dd->text = newJavaGD_Text;
    dd->rect = newJavaGD_Rect;
    dd->circle = newJavaGD_Circle;
    dd->line = newJavaGD_Line;
    dd->polyline = newJavaGD_Polyline;
    dd->polygon = newJavaGD_Polygon;
    dd->locator = newJavaGD_Locator;
    dd->mode = newJavaGD_Mode;
    dd->metricInfo = newJavaGD_MetricInfo;
#if R_GE_version >= 4
    dd->hasTextUTF8 = TRUE;
    dd->strWidthUTF8 = newJavaGD_StrWidthUTF8;
    dd->textUTF8 = newJavaGD_TextUTF8;
#if R_GE_version >= 6
    dd->raster = newJavaGD_Raster;
#if R_GE_version >= 8
    dd->path = newJavaGD_Path;
#if R_GE_version >= 9
    dd->holdflush = newJavaGD_HoldFlush;
#endif
#endif
#endif
#else
    dd->hold = newJavaGD_Hold;
#endif
}

/*---------------- R-accessible functions -------------------*/

int initJavaGD(newJavaGDDesc* xd) {                
//    {
//       jobject o = 0;
//       int releaseO = 1;
//       jmethodID mid;
//       jclass c=0;
//       char *customClass=getenv("JAVAGD_CLASS_NAME");
//       if (!getenv("JAVAGD_USE_RJAVA")) {
// 	if (customClass) { c=(*env)->FindClass(env, customClass); chkX(env); }
// 	if (!c) { c=(*env)->FindClass(env, "org/rosuda/javaGD/JavaGD"); chkX(env); }
// 	if (!c) { c=(*env)->FindClass(env, "JavaGD"); chkX(env); }
//       }
//       if (!c) {
// 	/* use rJava to instantiate the JavaGD class */
// 	SEXP cl;
// 	int  te;
// 	if (!customClass || !*customClass) customClass="org/rosuda/javaGD/JavaGD";
// 	/* require(rJava) to make sure it's loaded */
// 	cl = R_tryEval(lang2(install("require"), install("rJava")), R_GlobalEnv, &te);
// 	if (te == 0 && asLogical(cl)) { /* rJava is available and loaded */
// 	  /* if .jniInitialized is FALSE then no one actually loaded rJava before, so */
// 	  cl = eval(lang2(install(".jnew"), mkString(customClass)), R_GlobalEnv);
// 	  chkX(env);
// 	  if (cl != R_NilValue && inherits(cl, "jobjRef")) {
// 	    o = (jobject) R_ExternalPtrAddr(GET_SLOT(cl, install("jobj")));
// 	    releaseO = 0;
// 	    c = (*env)->GetObjectClass(env, o);
// 	  }
// 	}
//       }
//       if (!c && !o) error("Cannot find JavaGD class.");
//       if (!o) {
// 	mid=(*env)->GetMethodID(env, c, "<init>", "()V");
//         if (!mid) {
//             (*env)->DeleteLocalRef(env, c);  
//             error("Cannot find default JavaGD contructor.");
//         }
//         o=(*env)->NewObject(env, c, mid);
//         if (!o) {
// 	  (*env)->DeleteLocalRef(env, c);  
// 	  error("Connot instantiate JavaGD object.");
//         }
//       }
// 
//       xd->talk = (*env)->NewGlobalRef(env, o);
//       xd->talkClass = (*env)->NewGlobalRef(env, c);
//       (*env)->DeleteLocalRef(env, c);
//       if (releaseO) (*env)->DeleteLocalRef(env, o);
//     }
//     
    return 0;
}

