/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime;

public final class FastRConfig {
    /**
     * Whether the internal grid emulation should use AWT backed graphical devices.
     */
    public static final boolean InternalGridAwtSupport;

    /**
     * Umbrella option, which changes default values of other options in a way that FastR will not
     * invoke any native code directly and other potentially security sensitive operations are
     * restricted. Can be configured via environment variable {@code FASTR_RFFI=managed}.
     */
    public static final boolean ManagedMode;

    static {
        String rffiVal = System.getenv("FASTR_RFFI");
        ManagedMode = rffiVal != null && rffiVal.equals("managed");
        if (ManagedMode) {
            InternalGridAwtSupport = false;
        } else {
            String val = System.getProperty("fastr.internal.grid.awt.support");
            InternalGridAwtSupport = val == null || val.equals("true");
        }
    }

    private FastRConfig() {
        // only static fields
    }
}
