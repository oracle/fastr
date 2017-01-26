/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.engine;

import java.util.Locale;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Instrumenter;
import com.oracle.truffle.api.instrumentation.ProvidedTags;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.engine.interop.RForeignAccessFactoryImpl;
import com.oracle.truffle.r.nodes.RASTBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinPackages;
import com.oracle.truffle.r.nodes.instrumentation.RSyntaxTags;
import com.oracle.truffle.r.runtime.ExitException;
import com.oracle.truffle.r.runtime.FastROptions;
import com.oracle.truffle.r.runtime.RAccuracyInfo;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RVersionInfo;
import com.oracle.truffle.r.runtime.TempPathName;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.context.Engine.IncompleteSourceException;
import com.oracle.truffle.r.runtime.context.Engine.ParseException;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.RContext.RCloseable;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RTypedValue;
import com.oracle.truffle.r.runtime.ffi.Load_RFFIFactory;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/**
 * Only does the minimum for running under the debugger. It is not completely clear how to correctly
 * integrate the R startup in {@code RCommand} with this API.
 */
@TruffleLanguage.Registration(name = "R", version = "0.1", mimeType = {RRuntime.R_APP_MIME, RRuntime.R_TEXT_MIME}, interactive = true)
@ProvidedTags({StandardTags.CallTag.class, StandardTags.StatementTag.class, StandardTags.RootTag.class, RSyntaxTags.LoopTag.class})
public final class TruffleRLanguage extends TruffleLanguage<RContext> {

    /**
     * The choice of {@link RFFIFactory} is made statically so that it is bound into an AOT-compiled
     * VM. The decision is made directly in {@link RFFIFactory} to avoid some project dependencies
     * that cause build problems.
     */
    private static void initialize() {
        try {
            Load_RFFIFactory.initialize(true);
            Locale.setDefault(Locale.ROOT);
            RAccuracyInfo.initialize();
            RVersionInfo.initialize();
            TempPathName.initialize();

        } catch (Throwable t) {
            t.printStackTrace();
            /*
             * Truffle currently has no distinguished exception to indicate language initialization
             * failure, so nothing good can come from throwing the exception, which is what
             * Utils.rSuicide does. For now we catch it and exit the process.
             */
            try {
                Utils.rSuicide("error during R language initialization");
            } catch (ExitException ex) {
                System.exit(ex.getStatus());
            }
        }
    }

    private static boolean systemInitialized;

    public static final TruffleRLanguage INSTANCE = new TruffleRLanguage();

    public static final String MIME = RRuntime.R_APP_MIME;

    private TruffleRLanguage() {
    }

    @Override
    protected boolean isObjectOfLanguage(Object object) {
        return false;
    }

    @Override
    protected void initializeContext(RContext context) throws Exception {
        if (!systemInitialized) {
            FastROptions.initialize();
            initialize();
            systemInitialized = true;
        }
        context.initializeContext();
    }

    @Override
    protected RContext createContext(Env env) {
        boolean initialContext = !systemInitialized;
        if (initialContext) {
            RContext.initializeGlobalState(new RASTBuilder(), new RRuntimeASTAccessImpl(), RBuiltinPackages.getInstance(), new RForeignAccessFactoryImpl());
        }
        RContext result = RContext.create(env, env.lookup(Instrumenter.class), initialContext);
        return result;
    }

    @Override
    protected void disposeContext(RContext context) {
        context.destroy();
    }

    @Override
    protected String toString(RContext context, Object value) {
        // Btw debugger passes result of TruffleRLanguage.findMetaObject() to this method too
        if (value instanceof String) {
            return (String) value;
        }
        if (value instanceof RPromise) {
            RPromise promise = (RPromise) value;
            if (promise.isEvaluated()) {
                value = promise.getValue();
            }
        }
        return RRuntime.toString(value);
    }

    @Override
    protected boolean isVisible(RContext context, Object value) {
        // Always returning false means that PolyglotEngine.eval() will not call our impl of
        // TruffleRLanguage.toString(RContext,Object).
        // Instead we are responsible for proper printing of the resulting value based on
        // Source.isInteractive() flag (evaluated in REngine.PolyglotEngineRootNode).
        return false;
    }

    @Override
    protected Object findMetaObject(RContext context, Object value) {
        String ret = "Object";
        if (value instanceof RPromise) {
            RPromise promise = (RPromise) value;
            if (promise.isEvaluated()) {
                value = promise.getValue();
            }
        }
        value = RRuntime.asAbstractVector(value); // Wrap scalars "Integer", "Double" etc.
        if (value instanceof RTypedValue) {
            ret = ((RTypedValue) value).getRType().getName();
        }
        return ret;
    }

    @Override
    protected SourceSection findSourceLocation(RContext context, Object value) {
        if (value instanceof RPromise) {
            RPromise promise = (RPromise) value;
            RBaseNode expr = promise.getClosure().getExpr();
            return expr.getEncapsulatingSourceSection();
        }

        if (value instanceof RFunction) {
            RFunction f = (RFunction) value;
            return f.getTarget().getRootNode().getSourceSection();
        }
        return null;
    }

    @SuppressWarnings("try")
    @Override
    protected CallTarget parse(ParsingRequest request) throws Exception {
        CompilerAsserts.neverPartOfCompilation();
        try (RCloseable c = RContext.withinContext(findContext(createFindContextNode()))) {
            try {
                return RContext.getEngine().parseToCallTarget(request.getSource(), request.getFrame());
            } catch (IncompleteSourceException e) {
                throw new com.oracle.truffle.api.vm.IncompleteSourceException(e);
            } catch (ParseException e) {
                return Truffle.getRuntime().createCallTarget(new RootNode(TruffleRLanguage.class, null, new FrameDescriptor()) {
                    @Override
                    public Object execute(VirtualFrame frame) {
                        try {
                            throw e.throwAsRError();
                        } catch (RError e2) {
                            return null;
                        }
                    }
                });
            } catch (RError e) {
                return Truffle.getRuntime().createCallTarget(new RootNode(TruffleRLanguage.class, null, new FrameDescriptor()) {
                    @Override
                    public Object execute(VirtualFrame frame) {
                        return null;
                    }
                });
            }
        }
    }

    @Override
    protected Object findExportedSymbol(RContext context, String globalName, boolean onlyExplicit) {
        return context.getExportedSymbols().get(globalName);
    }

    @Override
    protected Object getLanguageGlobal(RContext context) {
        // TODO: what's the meaning of "language global" for R?
        return null;
    }

    // TODO: why isn't the original method public?
    public Node actuallyCreateFindContextNode() {
        return createFindContextNode();
    }

    public RContext actuallyFindContext0(Node contextNode) {
        return findContext(contextNode);
    }
}
