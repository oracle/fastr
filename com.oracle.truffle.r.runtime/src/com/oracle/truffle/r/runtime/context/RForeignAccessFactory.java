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
package com.oracle.truffle.r.runtime.context;

import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.r.runtime.data.RTruffleObject;

public interface RForeignAccessFactory {

    /**
     * Return the appropriate {@link ForeignAccess} instance for {@code obj}.
     */
    ForeignAccess getForeignAccess(RTruffleObject obj);

    /**
     * Return the {@link TruffleLanguage} instance for R. (Project circularity workaround).
     */
    Class<? extends TruffleLanguage<RContext>> getTruffleLanguage();

    /**
     * Changes the interpretation of {@RNull} as {@code null} to {@code value}. This allows the
     * {@code FFI} implementations to prevent {@RNull} being converted across the {@code FFI}
     * interface, which would be incorrect.
     *
     * @return the previous setting
     */
    boolean setIsNull(boolean value);
}
