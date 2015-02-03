/*
 *  R : A Computer Language for Statistical Data Analysis
 *  Copyright (C) 2004-12   The R Core Team.
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
 *  along with this program; if not, a copy is available at
 *  http://www.r-project.org/Licenses/
 */

#include <Rinternals.h>
#include <R_ext/Boolean.h>
#include <R_ext/GraphicsEngine.h> /* for DevDesc */

#ifdef ENABLE_NLS
#include <libintl.h>
#undef _
#define _(String) dgettext ("grDevices", String)
#else
#define _(String) (String)
#endif

SEXP R_CreateAtVector(SEXP axp, SEXP usr, SEXP nint, SEXP is_log) { return NULL; }
SEXP R_GAxisPars(SEXP usr, SEXP is_log, SEXP nintLog) { return NULL; }

SEXP PicTeX(SEXP args) { return NULL; }

SEXP PostScript(SEXP args) { return NULL; }
SEXP XFig(SEXP args) { return NULL; }
SEXP PDF(SEXP args) { return NULL; }
SEXP Type1FontInUse(SEXP arg1, SEXP arg2) { return NULL; }
SEXP CIDFontInUse(SEXP arg1, SEXP arg2) { return NULL; }

#ifndef _WIN32
SEXP Quartz(SEXP args) { return NULL; }
SEXP makeQuartzDefault() { return NULL; }

SEXP X11(SEXP call, SEXP op, SEXP args, SEXP rho) { return NULL; }
SEXP savePlot(SEXP call, SEXP op, SEXP args, SEXP rho) { return NULL; }
#endif

SEXP devCairo(SEXP args) { return NULL; }

Rboolean
PSDeviceDriver(pDevDesc dd, const char *file, const char *paper,
	       const char *family, const char **afmpaths, const char *encoding,
	       const char *bg, const char *fg, double width, double height,
	       Rboolean horizontal, double ps,
	       Rboolean onefile, Rboolean pagecentre, Rboolean printit,
	       const char *cmd, const char *title, SEXP fonts,
	       const char *colormodel, int useKern, Rboolean fillOddEven) { return FALSE; }

Rboolean
PDFDeviceDriver(pDevDesc dd, const char *file, const char *paper,
		const char *family, const char **afmpaths,
		const char *encoding,
		const char *bg, const char *fg, double width, double height,
		double ps, int onefile, int pagecentre,
		const char *title, SEXP fonts,
		int versionMajor, int versionMinor,
		const char *colormodel, int dingbats, int useKern,
		Rboolean fillOddEven, Rboolean useCompression) { return FALSE; }

#ifdef _WIN32
SEXP devga(SEXP) { return NULL; }
SEXP savePlot(SEXP) { return NULL; }
SEXP bringToTop(SEXP, SEXP) { return NULL; }
SEXP msgWindow(SEXP, SEXP) { return NULL; }
#endif

SEXP devcap(SEXP args) { return NULL; }
SEXP devcapture(SEXP args) { return NULL; }
SEXP devcontrol(SEXP args) { return NULL; }
SEXP devcopy(SEXP args) { return NULL; }
SEXP devcur(SEXP args) { return NULL; }
SEXP devdisplaylist(SEXP args) { return NULL; }
SEXP devholdflush(SEXP args) { return NULL; }
SEXP devnext(SEXP args) { return NULL; }
SEXP devoff(SEXP args) { return NULL; }
SEXP devprev(SEXP args) { return NULL; }
SEXP devset(SEXP args) { return NULL; }
SEXP devsize(SEXP args) { return NULL; }

SEXP chull(SEXP x) { return NULL; }

SEXP contourLines(SEXP call, SEXP op, SEXP args, SEXP rho) { return NULL; }
SEXP getSnapshot(SEXP call, SEXP op, SEXP args, SEXP rho) { return NULL; }
SEXP playSnapshot(SEXP call, SEXP op, SEXP args, SEXP rho) { return NULL; }
SEXP getGraphicsEvent(SEXP call, SEXP op, SEXP args, SEXP rho) { return NULL; }
SEXP getGraphicsEventEnv(SEXP call, SEXP op, SEXP args, SEXP rho) { return NULL; }
SEXP setGraphicsEventEnv(SEXP call, SEXP op, SEXP args, SEXP rho) { return NULL; }
SEXP devAskNewPage(SEXP call, SEXP op, SEXP args, SEXP env) { return NULL; }

#ifndef DEVWINDOWS
SEXP rgb(SEXP r, SEXP g, SEXP b, SEXP a, SEXP MCV, SEXP nam) { return NULL; }
SEXP hsv(SEXP h, SEXP s, SEXP v, SEXP a) { return NULL; }
SEXP hcl(SEXP h, SEXP c, SEXP l, SEXP a, SEXP sfixup) { return NULL; }
SEXP gray(SEXP lev, SEXP a) { return NULL; }
SEXP colors(void) { return NULL; }
SEXP col2rgb(SEXP colors, SEXP alpha) { return NULL; }
SEXP palette(SEXP value) { return NULL; }
SEXP palette2(SEXP value) { return NULL; }
SEXP RGB2hsv(SEXP rgb) { return NULL; }
#endif

unsigned int inRGBpar3(SEXP arg1, int arg2, unsigned int arg3) { return 0; }
const char *incol2name(unsigned int col) { return NULL; }
unsigned int inR_GE_str2col(const char *s) { return 0; }
void initPalette(void) { }


