/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data;

import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.r.runtime.context.RContext;

/**
 * A tagging interface that indicates that a {@link TruffleObject} belongs to the R language. There
 * are actually two sets of such types; those that are reflected at the R language level and
 * therefore suitable for passing to other peer languages, e.g. {@link RIntVector}, and those that
 * are used within the implementation for handling the {@code R FFI}, which uses Truffle interop
 * internally. The latter types should not leak to other languages.
 *
 * As a convenience the interface provides a default implementation of
 * {@link TruffleObject#getForeignAccess()} that indirects through
 * {@link RContext#getRForeignAccessFactory()}. This is entirely optional and can be overridden to
 * use an alternate implementation.
 *
 */
public interface RTruffleObject extends TruffleObject {
    @Override
    default ForeignAccess getForeignAccess() {
        return RContext.getRForeignAccessFactory().getForeignAccess(this);
    }
}
