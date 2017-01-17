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

package com.oracle.truffle.r.runtime;

import java.util.HashMap;
import java.util.Map;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.r.runtime.context.Engine.ParseException;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RExpression;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.env.REnvironment;

/**
 * Represents an internal R code snippet that can be used to implement parts of the runtime that
 * were previously implemented using native languages and invoked through R external mechanism.
 */
public final class RInternalCode {
    private final RContext context;
    private final String basePackage;
    private final Source source;

    private REnvironment evaluatedEnvironment;

    private RInternalCode(RContext context, String basePackage, Source source) {
        this.context = context;
        this.source = source;
        this.basePackage = basePackage;
    }

    @TruffleBoundary
    public static Source loadSourceRelativeTo(Class<?> clazz, String fileName) {
        return Utils.getResourceAsSource(clazz, fileName);
    }

    private REnvironment evaluate() {
        try {
            RExpression parsedCode = context.getThisEngine().parse(source);
            REnvironment statsPackage = REnvironment.getRegisteredNamespace(context, basePackage);
            evaluatedEnvironment = RDataFactory.createNewEnv(null, true, 10);
            evaluatedEnvironment.setParent(statsPackage);
            // caller is put into arguments by eval, internal code is assumed to be well-behaved and
            // not accessing it
            context.getThisEngine().eval(parsedCode, evaluatedEnvironment, RCaller.createInvalid(null));
            return evaluatedEnvironment;
        } catch (ParseException e) {
            throw e.throwAsRError();
        }
    }

    public RFunction lookupFunction(String name) {
        REnvironment env = this.evaluatedEnvironment;
        if (env == null) {
            env = evaluate();
            this.evaluatedEnvironment = env;
        }
        return (RFunction) env.get(name);
    }

    public static RInternalCode lookup(RContext context, String basePackage, Source source) {
        ContextStateImpl state = context.stateInternalCode;
        RInternalCode code = state.get(source);
        if (code == null) {
            code = new RInternalCode(context, basePackage, source);
            state.put(source, code);
        }
        return code;
    }

    public static final class ContextStateImpl implements RContext.ContextState {

        private final Map<Source, RInternalCode> codes = new HashMap<>();

        RInternalCode get(Source source) {
            return codes.get(source);
        }

        void put(Source source, RInternalCode code) {
            codes.put(source, code);
        }

        public static ContextStateImpl newContextState() {
            return new ContextStateImpl();
        }
    }
}
