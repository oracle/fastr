/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.charset.Charset;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RBuiltinKind;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

public class LocaleFunctions {

    @RBuiltin(name = "Sys.getlocale", kind = RBuiltinKind.INTERNAL, parameterNames = {"category"})
    public abstract static class GetLocale extends RBuiltinNode {

        @Specialization
        @TruffleBoundary
        protected Object getLocale(RAbstractIntVector categoryVec) {
            controlVisibility();
            // TODO implement all: for now just return not available (NULL)
            int category = categoryVec.getDataAt(0);
            switch (category) {
                case 3: // "LC_CTYPE",
                    return RDataFactory.createStringVector(Charset.defaultCharset().name());
                case 1: // "LC_ALL"
                    break;
                case 2: // "LC_COLLATE"
                    break;
                case 4: // "LC_MONETARY"
                    break;
                case 5: // "LC_NUMERIC"
                    break;
                case 6: // "LC_TIME"
                    break;
                case 7: // "LC_MESSAGES"
                    break;
                case 8: // "LC_PAPER"
                    return RDataFactory.createStringVectorFromScalar("");
                case 9: // "LC_MEASUREMENT"
                    break;
                default:
                    throw RInternalError.shouldNotReachHere();
            }
            return RNull.instance;
        }
    }

    @RBuiltin(name = "Sys.setlocale", kind = RBuiltinKind.INTERNAL, parameterNames = {"category", "locale"})
    public abstract static class SetLocale extends RBuiltinNode {

        @Specialization
        @TruffleBoundary
        protected Object setLocale(@SuppressWarnings("unused") RAbstractIntVector categoryVec, RAbstractStringVector locale) {
            controlVisibility();
            // TODO implement properly!!
            return locale;
        }

        @Specialization
        @TruffleBoundary
        protected Object setLocale(@SuppressWarnings("unused") RAbstractIntVector categoryVec, RNull locale) {
            controlVisibility();
            // TODO implement properly!!
            return locale;
        }
    }

    @RBuiltin(name = "Sys.localeconv", kind = RBuiltinKind.INTERNAL, parameterNames = {})
    public abstract static class LocaleConv extends RBuiltinNode {
        @Specialization
        @TruffleBoundary
        protected Object localeconv() {
            controlVisibility();
            RError.nyi(this, "localeconv");
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
            data[0] = RRuntime.LOGICAL_TRUE;
            data[1] = RRuntime.LOGICAL_TRUE;
            data[2] = RRuntime.LOGICAL_FALSE;
            return RDataFactory.createList(data, NAMES);
        }
    }

    @RBuiltin(name = "enc2native", kind = RBuiltinKind.PRIMITIVE, parameterNames = "x")
    public abstract static class Enc2Native extends RBuiltinNode {
        @Specialization
        protected Object enc2Native(RAbstractStringVector x) {
            // TODO implement properly
            return x;
        }
    }

    @RBuiltin(name = "enc2utf8", kind = RBuiltinKind.PRIMITIVE, parameterNames = "x")
    public abstract static class Enc2Utf8 extends RBuiltinNode {
        @Specialization
        protected Object enc2Native(RAbstractStringVector x) {
            // TODO implement properly
            return x;
        }
    }

    @RBuiltin(name = "bindtextdomain", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"domain", "dirname"})
    public abstract static class BindTextDomain extends RBuiltinNode {
        @SuppressWarnings("unused")
        @Specialization
        protected RNull bindtextdomain(RAbstractStringVector domain, Object dirname) {
            // TODO implement properly
            return RNull.instance;
        }
    }
}
