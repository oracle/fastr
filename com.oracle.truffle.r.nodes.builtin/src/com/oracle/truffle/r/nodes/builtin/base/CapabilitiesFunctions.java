/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RBuiltinKind;
import com.oracle.truffle.r.runtime.RCmdOptions;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RStringVector;

public class CapabilitiesFunctions {

    @RBuiltin(name = "capabilities", kind = RBuiltinKind.INTERNAL, parameterNames = {})
    public abstract static class Capabilities extends RBuiltinNode {
        private enum Capability {
            jpeg(false, null),
            png(false, null),
            tiff(false, null),
            tcltk(false, null),
            X11(false, null),
            aqua(false, null),
            http_fttp(true, "http/ftp"),
            sockets(true, null),
            libxml(false, null),
            fifo(false, null),
            cledit(false, null),
            iconv(false, null),
            nls(false, "NLS"),
            profmem(false, null),
            cairo(false, null);

            private final boolean defValue;
            private final String rName;

            Capability(boolean defValue, String nameOverride) {
                this.defValue = defValue;
                this.rName = nameOverride == null ? name() : nameOverride;
            }

            static String[] rNames() {
                Capability[] values = values();
                String[] result = new String[values.length];
                for (Capability c : values) {
                    result[c.ordinal()] = c.rName;
                }
                return result;
            }

        }

        private static final RStringVector NAMES = RDataFactory.createStringVector(Capability.rNames(), RDataFactory.COMPLETE_VECTOR);

        @Specialization
        protected RLogicalVector capabilities() {
            controlVisibility();
            byte[] data = new byte[NAMES.getLength()];
            for (Capability c : Capability.values()) {
                boolean value = c.defValue;
                switch (c) {
                    case cledit:
                        value = RContext.getInstance().isInteractive() && !RContext.getInstance().getOptions().getBoolean(RCmdOptions.RCmdOption.NO_READLINE);
                        break;
                }
                data[c.ordinal()] = RRuntime.asLogical(value);
            }
            return RDataFactory.createLogicalVector(data, RDataFactory.COMPLETE_VECTOR, NAMES);
        }
    }
}
