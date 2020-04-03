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