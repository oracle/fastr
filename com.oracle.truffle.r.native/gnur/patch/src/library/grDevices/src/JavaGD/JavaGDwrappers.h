/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
#ifndef __JAVAGD_WRAPPERS_H__
#define __JAVAGD_WRAPPERS_H__

#include <Rdefines.h>
#include <R.h>
#include <Rdefines.h>

SEXP api_gdcSetColor(SEXP);
SEXP api_gdcSetFill(SEXP);
SEXP api_gdcSetLine(SEXP, SEXP);
SEXP api_gdcSetFont(SEXP, SEXP, SEXP, SEXP, SEXP);
SEXP api_gdNewPage(SEXP, SEXP);
SEXP api_gdActivate();
SEXP api_gdCircle(SEXP, SEXP, SEXP);
SEXP api_gdClip(SEXP, SEXP, SEXP, SEXP);
SEXP api_gdClose();
SEXP api_gdDeactivate();
SEXP api_gdHold();
SEXP api_gdFlush(SEXP);
SEXP api_gdLocator();
SEXP api_gdLine(SEXP, SEXP, SEXP, SEXP);
SEXP api_gdMode(SEXP);
SEXP api_gdOpen(SEXP name, SEXP w, SEXP h);
SEXP api_gdPath(SEXP, SEXP, SEXP, SEXP, SEXP, SEXP);
SEXP api_gdPolygon(SEXP, SEXP, SEXP);
SEXP api_gdPolygon2(SEXP, SEXP, SEXP);
SEXP api_gdPolyline(SEXP, SEXP, SEXP);
SEXP api_gdRect(SEXP, SEXP, SEXP, SEXP);
SEXP api_gdSize();
SEXP api_getStrWidth(SEXP);
SEXP api_gdText(SEXP, SEXP, SEXP, SEXP, SEXP);
SEXP api_gdRaster(SEXP img, SEXP img_w, SEXP img_h, SEXP x, SEXP y, SEXP w, SEXP h, SEXP rot, SEXP interpolate);
SEXP api_gdMetricInfo(SEXP);

#endif // __JAVAGD_WRAPPERS_H__
