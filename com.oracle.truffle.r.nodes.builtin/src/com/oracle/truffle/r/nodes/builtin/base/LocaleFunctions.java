/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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
import static com.oracle.truffle.r.runtime.builtins.RBehavior.MODIFIES_STATE;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.READS_STATE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import java.nio.charset.Charset;
import java.util.Locale;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.NodeWithArgumentCasts.Casts;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

public class LocaleFunctions {

    enum LC {
        COLLATE(stdDefault()),
        CTYPE(stdDefault()),
        MONETARY(stdDefault()),
        NUMERIC("C"),
        TIME(stdDefault()),
        MESSAGES(stdDefault()),
        PAPER(""),
        MEASUREMENT("");

        private String value;
        private String defaultValue;

        LC(String defaultValue) {
            this.defaultValue = defaultValue;
        }

        static String stdDefault() {
            String defLocale = Locale.getDefault().toString() + "_";
            String defCharSet = Charset.defaultCharset().name();
            return defLocale + defCharSet;
        }

        private String getLCEnvVar() {
            if (value != null) {
                return value;
            }
            String val = RContext.getInstance().stateREnvVars.get("LC_" + name());
            if (val == null) {
                return defaultValue;
            } else {
                return val;
            }
        }

        private static boolean isAll(int category) {
            return category == 1;
        }

        private static LC getLCCategory(int category) {
            return LC.values()[category - 2];
        }
    }

    @RBuiltin(name = "Sys.getlocale", kind = INTERNAL, parameterNames = {"category"}, behavior = READS_STATE)
    public abstract static class GetLocale extends RBuiltinNode.Arg1 {

        static {
            Casts casts = new Casts(GetLocale.class);
            CastsHelper.category(casts);
        }

        private static final int[] ALL_CATEGORIES = new int[]{3, 2, 4, 5, 6, 7, 8, 9};

        @Specialization
        @TruffleBoundary
        protected Object getLocale(int category) {
            return RDataFactory.createStringVector(getLocaleData(category));
        }

        protected String getLocaleData(int category) {
            if (LC.isAll(category)) {
                // "LC_ALL"
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < ALL_CATEGORIES.length; i++) {
                    String d = getLocaleData(ALL_CATEGORIES[i]);
                    if (d.length() > 0) {
                        sb.append(d);
                        if (i != ALL_CATEGORIES.length - 1) {
                            sb.append('/');
                        }
                    }
                }
                return sb.toString();
            } else {
                return LC.getLCCategory(category).getLCEnvVar();
            }
        }
    }

    @RBuiltin(name = "Sys.setlocale", kind = INTERNAL, parameterNames = {"category", "locale"}, behavior = MODIFIES_STATE)
    public abstract static class SetLocale extends RBuiltinNode.Arg2 {

        static {
            Casts casts = new Casts(SetLocale.class);
            CastsHelper.category(casts);
            casts.arg("locale").mustBe(stringValue()).asStringVector().mustBe(singleElement()).findFirst();
        }

        @Specialization
        @TruffleBoundary
        protected Object setLocale(int category, String locale) {
            if (LC.isAll(category)) {
                for (LC lc : LC.values()) {
                    lc.value = locale;
                }
            } else {
                LC.getLCCategory(category).value = locale;
            }
            return locale;
        }
    }

    @RBuiltin(name = "Sys.localeconv", kind = INTERNAL, parameterNames = {}, behavior = READS_STATE)
    public abstract static class LocaleConv extends RBuiltinNode.Arg0 {
        @Specialization
        @TruffleBoundary
        protected Object localeconv() {
            RError.nyi(this, "localeconv");
            return RNull.instance;
        }
    }

    @RBuiltin(name = "l10n_info", kind = INTERNAL, parameterNames = {}, behavior = READS_STATE)
    public abstract static class L10nInfo extends RBuiltinNode.Arg0 {
        private static final RStringVector NAMES = RDataFactory.createStringVector(new String[]{"MBCS", "UTF-8", "Latin-1"}, RDataFactory.COMPLETE_VECTOR);

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
    public abstract static class Enc2Native extends RBuiltinNode.Arg1 {

        static {
            Casts casts = new Casts(Enc2Native.class);
            CastsHelper.xCharacterVector(casts);
        }

        @Specialization
        protected Object enc2Native(RAbstractStringVector x) {
            // TODO implement properly
            return x;
        }
    }

    @RBuiltin(name = "enc2utf8", kind = PRIMITIVE, parameterNames = "x", behavior = READS_STATE)
    public abstract static class Enc2Utf8 extends RBuiltinNode.Arg1 {

        static {
            Casts casts = new Casts(Enc2Utf8.class);
            CastsHelper.xCharacterVector(casts);
        }

        @Specialization
        protected Object enc2Native(RAbstractStringVector x) {
            // TODO implement properly
            return x;
        }
    }

    @RBuiltin(name = "bindtextdomain", kind = PRIMITIVE, parameterNames = {"domain", "dirname"}, behavior = READS_STATE)
    public abstract static class BindTextDomain extends RBuiltinNode.Arg2 {

        static {
            Casts casts = new Casts(BindTextDomain.class);
            casts.arg("domain").mustBe(stringValue(), INVALID_VALUE, "domain");
        }

        @SuppressWarnings("unused")
        @Specialization
        protected RNull bindtextdomain(RAbstractStringVector domain, Object dirname) {
            // TODO implement properly
            return RNull.instance;
        }
    }

    private static final class CastsHelper {
        private static void xCharacterVector(Casts casts) {
            casts.arg("x").mustBe(stringValue(), ARGUMENT_NOT_CHAR_VECTOR);
        }

        private static void category(Casts casts) {
            casts.arg("category").mustBe(numericValue(), INVALID_ARGUMENT, "category").asIntegerVector().findFirst();
        }
    }

    public static void main(String[] args) {
        System.out.println(Locale.getDefault());
    }
}
