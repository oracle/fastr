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

void init_graphicsdevices(JNIEnv *env) {
}

int ndevNumber(pDevDesc x) {
    return (int) unimplemented("ndevNumber");
}


int NumDevices(void) {
	return  (int)unimplemented("NumDevices");
}


void R_CheckDeviceAvailable(void) {
	unimplemented("R_CheckDeviceAvailable");
}

Rboolean R_CheckDeviceAvailableBool(void) {
    unimplemented("R_CheckDeviceAvailable");
    return FALSE;
}


int curDevice(void) {
	return (int) unimplemented("curDevice");
}


int nextDevice(int x) {
	return (int) unimplemented("curDevice");
}


int prevDevice(int x) {
	return (int) unimplemented("prevDevice");
}


int selectDevice(int x) {
	return (int) unimplemented("selectDevice");
}


void killDevice(int x) {
    unimplemented("killDevice");
}


int NoDevices(void) {
	return (int) unimplemented("killDevice");
}


void NewFrameConfirm(pDevDesc x) {
    unimplemented("NoDevices");
}

