/*
 *  R : A Computer Language for Statistical Data Analysis
 *  Copyright (C) 2012   The R Core Team.
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

#ifdef ENABLE_NLS
#include <libintl.h>
#undef _
#define _(String) dgettext ("graphics", String)
#else
#define _(String) (String)
#endif

SEXP C_contour(SEXP args) { return NULL; }
SEXP C_contourDef(void) { return NULL; }
SEXP C_filledcontour(SEXP args) { return NULL; }
SEXP C_image(SEXP args) { return NULL; }
SEXP C_persp(SEXP args) { return NULL; }

SEXP C_abline(SEXP args) { return NULL; }
SEXP C_arrows(SEXP args) { return NULL; }
SEXP C_axis(SEXP args) { return NULL; }
SEXP C_box(SEXP args) { return NULL; }
SEXP C_clip(SEXP args) { return NULL; }
SEXP C_convertX(SEXP args) { return NULL; }
SEXP C_convertY(SEXP args) { return NULL; }
SEXP C_dend(SEXP args) { return NULL; }
SEXP C_dendwindow(SEXP args) { return NULL; }
SEXP C_erase(SEXP args) { return NULL; }
SEXP C_layout(SEXP args) { return NULL; }
SEXP C_mtext(SEXP args) { return NULL; }
SEXP C_path(SEXP args) { return NULL; }
SEXP C_plotXY(SEXP args) { return NULL; }
SEXP C_plot_window(SEXP args) { return NULL; }
SEXP C_polygon(SEXP args) { return NULL; }
SEXP C_raster(SEXP args) { return NULL; }
SEXP C_rect(SEXP args) { return NULL; }
SEXP C_segments(SEXP args) { return NULL; }
SEXP C_strHeight(SEXP args) { return NULL; }
SEXP C_strWidth (SEXP args) { return NULL; }
SEXP C_symbols(SEXP args) { return NULL; }
SEXP C_text(SEXP args) { return NULL; }
SEXP C_title(SEXP args) { return NULL; }
SEXP C_xspline(SEXP args) { return NULL; }


SEXP C_par(SEXP call, SEXP op, SEXP args, SEXP rho) { return NULL; }
SEXP C_plot_new(SEXP call, SEXP op, SEXP args, SEXP rho) { return NULL; }
SEXP C_locator(SEXP call, SEXP op, SEXP args, SEXP rho) { return NULL; }
SEXP C_identify(SEXP call, SEXP op, SEXP args, SEXP rho) { return NULL; }

void registerBase(void) { }
void unregisterBase(void) { }
SEXP RunregisterBase(void) { return NULL; }

SEXP C_StemLeaf(SEXP x, SEXP scale, SEXP swidth, SEXP atom) { return NULL; }
SEXP C_BinCount(SEXP x, SEXP breaks, SEXP right, SEXP lowest) { return NULL; }

Rboolean isNAcol(SEXP col, int index, int ncol) { return FALSE; }
