/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.charset.*;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

public class LocaleFunctions {

    @RBuiltin(name = "Sys.getlocale", kind = RBuiltinKind.INTERNAL, parameterNames = {"category"})
    public abstract static class GetLocale extends RBuiltinNode {

        @TruffleBoundary
        @Specialization
        protected Object getLocal(RAbstractIntVector categoryVec) {
            controlVisibility();
            int category = categoryVec.getDataAt(0);
            switch (category) {
                case 3: // "LC_CTYPE",
                    return RDataFactory.createStringVector(Charset.defaultCharset().name());
                case 1: // "LC_ALL"
                case 2: // "LC_COLLATE"
                case 4: // "LC_MONETARY"
                case 5: // "LC_NUMERIC"
                case 6: // "LC_TIME"
                case 7: // "LC_MESSAGES"
                case 8: // "LC_PAPER"
                case 9: // "LC_MEASUREMENT"
                default:
                    RError.nyi(getEncapsulatingSourceSection(), "category not implemented: " + category);

            }
            return RNull.instance;
        }

    }

    @RBuiltin(name = "Sys.setlocale", kind = RBuiltinKind.INTERNAL, parameterNames = {"category, locale"})
    public abstract static class SetLocale extends RBuiltinNode {

        @TruffleBoundary
        @Specialization
        protected Object setLocal(@SuppressWarnings("unused") RAbstractStringVector category) {
            controlVisibility();
            RError.nyi(getEncapsulatingSourceSection(), "setlocale");
            return RNull.instance;
        }
    }

    @RBuiltin(name = "l10n_info", kind = RBuiltinKind.INTERNAL, parameterNames = {})
    public abstract static class L10nInfo extends RBuiltinNode {
        private static final RStringVector NAMES = RDataFactory.createStringVector(new String[]{"MBCS", "UTF-8", "LATIN-1"}, RDataFactory.COMPLETE_VECTOR);

        @Specialization
        protected RList l10nInfo() {
            Object[] data = new Object[NAMES.getLength()];
            // TODO check locale properly
            data[0] = RRuntime.LOGICAL_FALSE;
            data[1] = RRuntime.LOGICAL_FALSE;
            data[2] = RRuntime.LOGICAL_FALSE;
            return RDataFactory.createList(data, NAMES);
        }
    }
}
