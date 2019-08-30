/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.ffi.processor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Tags an up-call argument that should, from the GC protection mechanisms perspective, have a
 * reference to the the result of the up-call. This is undocumented part of the R API, which is,
 * however, sometimes relied upon. Example is a pair-list holding attributes, retrieved via
 * {@code ATTRIB} up-call. GNU-R simply hands out the pair-list referenced by the given object, so
 * as long as the given object is protected from GC, the pair-list should be too.
 *
 * See documentation/dev/ffi.md for more details.
 *
 * This annotation doesn't have any effect and serves only as a documentation aid. In the future it
 * may be used for some automated check.
 */
@Target(ElementType.PARAMETER)
public @interface RFFIResultOwner {
}
