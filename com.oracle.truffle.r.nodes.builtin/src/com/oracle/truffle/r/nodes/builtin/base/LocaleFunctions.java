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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.numericValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.singleElement;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.runtime.RError.Message.ARGUMENT_NOT_CHAR_VECTOR;
import static com.oracle.truffle.r.runtime.RError.Message.INVALID_ARGUMENT;
import static com.oracle.truffle.r.runtime.RError.Message.INVALID_VALUE;
import static com.oracle.truffle.r.runtime.RError.NO_CALLER;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.MODIFIES_STATE;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.READS_STATE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import java.nio.charset.Charset;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

public class LocaleFunctions {

    @RBuiltin(name = "Sys.getlocale", kind = INTERNAL, parameterNames = {"category"}, behavior = READS_STATE)
    public abstract static class GetLocale extends RBuiltinNode {

        @Override
        protected void createCasts(CastBuilder casts) {
            Casts.category(casts);
        }

        @Specialization
        @TruffleBoundary
        protected Object getLocale(int category) {
            // TODO implement all: for now just return not available (NULL)
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

    @RBuiltin(name = "Sys.setlocale", kind = INTERNAL, parameterNames = {"category", "locale"}, behavior = MODIFIES_STATE)
    public abstract static class SetLocale extends RBuiltinNode {

        @Override
        protected void createCasts(CastBuilder casts) {
            Casts.category(casts);
            casts.arg("locale").mustBe(stringValue()).asStringVector().mustBe(singleElement()).findFirst();
        }

        @Specialization
        @TruffleBoundary
        protected Object setLocale(@SuppressWarnings("unused") int category, String locale) {
            // TODO implement properly!!
            return locale;
        }
    }

    @RBuiltin(name = "Sys.localeconv", kind = INTERNAL, parameterNames = {}, behavior = READS_STATE)
    public abstract static class LocaleConv extends RBuiltinNode {
        @Specialization
        @TruffleBoundary
        protected Object localeconv() {
            RError.nyi(this, "localeconv");
            return RNull.instance;
        }
    }

    @RBuiltin(name = "l10n_info", kind = INTERNAL, parameterNames = {}, behavior = READS_STATE)
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

    @RBuiltin(name = "enc2native", kind = PRIMITIVE, parameterNames = "x", behavior = READS_STATE)
    public abstract static class Enc2Native extends RBuiltinNode {
        @Override
        protected void createCasts(CastBuilder casts) {
            Casts.xCharacterVector(casts);
        }

        @Specialization
        protected Object enc2Native(RAbstractStringVector x) {
            // TODO implement properly
            return x;
        }
    }

    @RBuiltin(name = "enc2utf8", kind = PRIMITIVE, parameterNames = "x", behavior = READS_STATE)
    public abstract static class Enc2Utf8 extends RBuiltinNode {
        @Override
        protected void createCasts(CastBuilder casts) {
            Casts.xCharacterVector(casts);
        }

        @Specialization
        protected Object enc2Native(RAbstractStringVector x) {
            // TODO implement properly
            return x;
        }
    }

    @RBuiltin(name = "bindtextdomain", kind = PRIMITIVE, parameterNames = {"domain", "dirname"}, behavior = READS_STATE)
    public abstract static class BindTextDomain extends RBuiltinNode {
        @Override
        protected void createCasts(@SuppressWarnings("unused") CastBuilder casts) {
            casts.arg("domain").mustBe(stringValue(), INVALID_VALUE, "domain");
        }

        @SuppressWarnings("unused")
        @Specialization
        protected RNull bindtextdomain(RAbstractStringVector domain, Object dirname) {
            // TODO implement properly
            return RNull.instance;
        }
    }

    private static final class Casts {
        private static void xCharacterVector(CastBuilder casts) {
            casts.arg("x").mustBe(stringValue(), ARGUMENT_NOT_CHAR_VECTOR);
        }

        private static void category(CastBuilder casts) {
            casts.arg("category").mustBe(numericValue(), NO_CALLER, INVALID_ARGUMENT, "category").asIntegerVector().findFirst();
        }
    }
}
