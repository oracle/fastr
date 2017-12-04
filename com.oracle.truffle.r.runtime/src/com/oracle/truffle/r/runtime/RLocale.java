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
package com.oracle.truffle.r.runtime;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.text.Collator;
import java.text.ParseException;
import java.text.RuleBasedCollator;
import java.util.EnumMap;
import java.util.Locale;

import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.RContext.ContextState;

public enum RLocale {
    COLLATE(true, true, true),
    CTYPE(false, true, true),
    MONETARY(true, true, true),
    NUMERIC(true, false, true),
    TIME(true, true, true),
    MESSAGES(true, true, true),
    PAPER(false, false, false),
    MEASUREMENT(false, false, false);

    private final String name;
    private final boolean needsLocale;
    private final boolean initializedAtStartup;
    private final boolean listed;

    RLocale(boolean needsLocale, boolean initializedAtStartup, boolean listed) {
        this.needsLocale = needsLocale;
        this.initializedAtStartup = initializedAtStartup;
        this.listed = listed;
        this.name = "LC_" + name();
    }

    /**
     * Returns the collator that should be used in order builtin or any place that should sort
     * elements like order. The {@code Locale} should be retrieved from {@link RContext}.
     */
    public static Collator getOrderCollator(Locale locale) {
        Collator baseCollator = Collator.getInstance(locale);
        String rules = ((RuleBasedCollator) baseCollator).getRules();
        Collator collator;
        try {
            collator = new RuleBasedCollator(rules.replaceAll("<'\u005f'", "<' '<'\u005f'"));
        } catch (ParseException e) {
            throw RInternalError.shouldNotReachHere(e);
        }
        return collator;
    }

    public static final class ContextStateImpl implements RContext.ContextState {
        private final EnumMap<RLocale, Locale> locales = new EnumMap<>(RLocale.class);
        private final EnumMap<RLocale, Charset> charsets = new EnumMap<>(RLocale.class);

        private ContextStateImpl() {
            // private constructor
        }

        private static String getDefinition(String name, REnvVars envVars) {
            // lookup order: LC_ALL, LC_<name>, LANG
            for (String identifier : new String[]{"LC_ALL", name, "LANG"}) {
                String value = envVars.get(identifier);
                if (value != null && !value.isEmpty()) {
                    return value;
                }
            }
            return null;
        }

        @Override
        public ContextState initialize(RContext context) {
            REnvVars envVars = context.stateREnvVars;
            for (RLocale locale : RLocale.values()) {
                if (locale.initializedAtStartup) {
                    setLocale(locale, getDefinition(locale.name, envVars), true);
                } else {
                    setLocale(locale, "C", true);
                }
            }
            return this;
        }

        private static Charset getCharset(String value) {
            try {
                int dot = value.indexOf('.');
                return dot == -1 ? Charset.forName(value) : Charset.forName(value.substring(dot + 1));
            } catch (UnsupportedCharsetException | IllegalCharsetNameException e) {
                return null;
            }
        }

        private static Locale getLocale(String value) {
            int dot = value.indexOf('.');
            String code = dot == -1 ? value : value.substring(0, dot);
            Locale.Builder builder = new Locale.Builder();
            switch (code.length()) {
                case 2:
                    return builder.setLanguage(code).build();
                case 5:
                    if (code.charAt(2) == '_') {
                        return builder.setLanguage(code.substring(0, 2)).setRegion(code.substring(3)).build();
                    }
                    break;
                default:
                    if (code.length() >= 7 && code.charAt(2) == '_' && code.charAt(5) == '_') {
                        return builder.setLanguage(code.substring(0, 2)).setRegion(code.substring(3)).setVariant(code.substring(6)).build();
                    }
                    break;
            }
            return null;
        }

        public void setLocale(RLocale locale, String value) {
            setLocale(locale, value, false);
        }

        public void setLocale(RLocale locale, String value, boolean startup) {
            Charset c = null;
            Locale l = null;
            if ("C".equals(value) || "POSIX".equals(value)) {
                c = StandardCharsets.US_ASCII;
            } else if (value != null) {
                c = getCharset(value);
                l = getLocale(value);
                if ((c == null && l == null) || (l == null && locale.needsLocale)) {
                    if (startup) {
                        RContext.getInstance().getConsole().printErrorln("Setting " + locale.name + " failed, using default");
                    } else {
                        RError.warning(RError.SHOW_CALLER, Message.OS_REQUEST_LOCALE, value);
                    }
                }
            }
            charsets.put(locale, c == null ? StandardCharsets.UTF_8 : c);
            locales.put(locale, l == null ? Locale.ROOT : l);
        }

        public Charset getCharset(RLocale locale) {
            return charsets.get(locale);
        }

        public Locale getLocale(RLocale locale) {
            return locales.get(locale);
        }

        public static ContextStateImpl newContextState() {
            return new ContextStateImpl();
        }
    }

    public String getRepresentation(RContext context) {
        ContextStateImpl state = context.stateRLocale;
        Locale l = state.locales.get(this);
        Charset c = state.charsets.get(this);
        if (c == StandardCharsets.US_ASCII) {
            if (l == Locale.ROOT) {
                return "C";
            } else {
                return l.toString();
            }
        } else {
            if (l == Locale.ROOT) {
                return c.name();
            } else {
                return l.toString() + "." + c.name();
            }
        }
    }

    public boolean isListed() {
        return listed;
    }
}
