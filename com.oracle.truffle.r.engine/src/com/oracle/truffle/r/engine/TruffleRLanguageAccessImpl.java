/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.engine;

import java.util.function.Supplier;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage.Env;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.r.nodes.RASTBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinPackages;
import com.oracle.truffle.r.nodes.builtin.base.printer.ValuePrinterNode;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode;
import com.oracle.truffle.r.nodes.function.RMissingHelper;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.conn.StdConnections.ContextStateImpl;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.TruffleRLanguageAccess;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RInteropNA;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.env.REnvironment;

public final class TruffleRLanguageAccessImpl implements TruffleRLanguageAccess {

    @Override
    public void onInitializeContext(Env env) {
        RContext.initializeGlobalState(new RASTBuilder(false), new RRuntimeASTAccessImpl(), RBuiltinPackages.getInstance());
    }

    @Override
    public String toDisplayString(RContext context, Object value, boolean sideEffects) {
        // these types implement their own toDisplayString method
        assert !(value instanceof RPromise);
        assert !(value instanceof RInteropNA);

        // print missing explicitly, because "print" would report missing argument
        if (RMissingHelper.isMissing(value)) {
            return "missing";
        }

        if (!sideEffects) {
            return toSimpleDisplayString(context, value);
        }

        Object printObj = REnvironment.baseEnv(context).get("print");
        if (printObj instanceof RPromise) {
            printObj = PromiseHelperNode.evaluateSlowPath((RPromise) printObj);
        }
        if (!(printObj instanceof RFunction)) {
            // This can happen, e.g., during engine initialization
            return toSimpleDisplayString(context, value);
        }
        MaterializedFrame callingFrame = REnvironment.globalEnv(context).getFrame();
        ContextStateImpl stateStdConnections = context.stateStdConnections;
        try {
            StringBuilder buffer = new StringBuilder();
            stateStdConnections.setBuffer(buffer);
            RContext.getEngine().evalFunction((RFunction) printObj, callingFrame, RCaller.topLevel, false, ArgumentsSignature.empty(1), value);
            // remove the last "\n", which is useful for REPL, but not here
            if (buffer.length() > 0 && buffer.charAt(buffer.length() - 1) == '\n') {
                buffer.setLength(buffer.length() - 1);
            }
            return buffer.toString();
        } finally {
            stateStdConnections.resetBuffer();
        }
    }

    private static String toSimpleDisplayString(RContext context, Object value) {
        RootCallTarget valuePrinter = context.getOrCreateCachedCallTarget(ValuePrinterRootNode.class, ValuePrinterRootNodeSupplier.INSTANCE);
        return (String) valuePrinter.call(value);
    }

    private static final class ValuePrinterRootNodeSupplier implements Supplier<RootCallTarget> {
        private static final ValuePrinterRootNodeSupplier INSTANCE = new ValuePrinterRootNodeSupplier();

        @Override
        public RootCallTarget get() {
            return Truffle.getRuntime().createCallTarget(new ValuePrinterRootNode());
        }
    }

    private static final class ValuePrinterRootNode extends RootNode {
        @Child private ValuePrinterNode valuePrinterNode = new ValuePrinterNode();

        ValuePrinterRootNode() {
            super(null);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return valuePrinterNode.execute(frame.getArguments()[0], RNull.instance, true, RNull.instance, RNull.instance, false, RNull.instance, true);
        }
    }

}
