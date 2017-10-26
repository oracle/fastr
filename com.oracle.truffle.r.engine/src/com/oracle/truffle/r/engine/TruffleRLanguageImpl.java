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

import java.util.HashMap;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.Scope;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.instrumentation.Instrumenter;
import com.oracle.truffle.api.instrumentation.ProvidedTags;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.engine.interop.RForeignAccessFactoryImpl;
import com.oracle.truffle.r.nodes.RASTBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinPackages;
import com.oracle.truffle.r.nodes.instrumentation.RSyntaxTags;
import com.oracle.truffle.r.runtime.ExitException;
import com.oracle.truffle.r.runtime.FastROptions;
import com.oracle.truffle.r.runtime.RAccuracyInfo;
import com.oracle.truffle.r.runtime.RDeparse;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.context.Engine.IncompleteSourceException;
import com.oracle.truffle.r.runtime.context.Engine.ParseException;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.TruffleRLanguage;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RTypedValue;
import com.oracle.truffle.r.runtime.env.RScope;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;
import com.oracle.truffle.r.runtime.interop.R2Foreign;
import com.oracle.truffle.r.runtime.interop.R2ForeignNodeGen;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

@TruffleLanguage.Registration(name = "R", id = "R", version = "3.3.2", mimeType = {RRuntime.R_APP_MIME, RRuntime.R_TEXT_MIME}, interactive = true)
@ProvidedTags({StandardTags.CallTag.class, StandardTags.StatementTag.class, StandardTags.RootTag.class, RSyntaxTags.LoopTag.class})
public final class TruffleRLanguageImpl extends TruffleRLanguage {

    private final HashMap<String, RFunction> builtinFunctionCache = new HashMap<>();

    @Override
    public HashMap<String, RFunction> getBuiltinFunctionCache() {
        return builtinFunctionCache;
    }

    /**
     * The choice of {@link RFFIFactory} is made statically so that it is bound into an AOT-compiled
     * VM. The decision is made directly in {@link RFFIFactory} to avoid some project dependencies
     * that cause build problems.
     */
    private static void initialize() {
        try {
            RAccuracyInfo.initialize();
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

    public static final String MIME = RRuntime.R_APP_MIME;

    @Override
    protected boolean isObjectOfLanguage(Object object) {
        return object instanceof RTypedValue;
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
        RContext result = RContext.create(this, env, env.lookup(Instrumenter.class), initialContext);
        return result;
    }

    @Override
    protected void disposeContext(RContext context) {
        context.dispose();
    }

    @Override
    protected String toString(RContext context, Object value) {
        // the debugger also passes result of TruffleRLanguage.findMetaObject() to this method
        Object unwrapped = value;
        if (unwrapped instanceof RPromise) {
            RPromise promise = (RPromise) unwrapped;
            if (promise.isEvaluated()) {
                unwrapped = promise.getValue();
            }
        }
        if (unwrapped instanceof String) {
            return (String) unwrapped;
        }
        if (unwrapped instanceof RTypedValue) {
            return RDeparse.deparse(unwrapped, RDeparse.MAX_CUTOFF, true, RDeparse.KEEPINTEGER, -1, 1024 * 1024);
        }
        return RRuntime.toString(unwrapped);
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
        Object unwrappedValue = value;
        if (unwrappedValue instanceof RPromise) {
            RPromise promise = (RPromise) unwrappedValue;
            if (promise.isEvaluated()) {
                unwrappedValue = promise.getValue();
            }
        }
        // Wrap scalars Integer, Double, etc.
        unwrappedValue = RRuntime.convertScalarVectors(unwrappedValue);
        if (unwrappedValue instanceof RTypedValue) {
            return ((RTypedValue) unwrappedValue).getRType().getName();
        } else {
            return "Object";
        }
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

    @Override
    protected CallTarget parse(ParsingRequest request) throws Exception {
        CompilerAsserts.neverPartOfCompilation();
        try {
            return RContext.getEngine().parseToCallTarget(request.getSource(), request.getFrame());
        } catch (IncompleteSourceException e) {
            throw e;
        } catch (ParseException e) {
            throw e.throwAsRError();
        }
    }

    private static final R2Foreign r2foreign = R2ForeignNodeGen.create();

    @Override
    protected Object getLanguageGlobal(RContext context) {
        // TODO: what's the meaning of "language global" for R?
        return null;
    }

    public static RContext getCurrentContext() {
        return TruffleLanguage.getCurrentContext(TruffleRLanguage.class);
    }

    public static TruffleRLanguage getCurrentLanguage() {
        return TruffleLanguage.getCurrentLanguage(TruffleRLanguage.class);
    }

    @Override
    public Iterable<Scope> findLocalScopes(RContext langContext, Node node, Frame frame) {
        return RScope.createLocalScopes(langContext, node, frame);
    }

    @Override
    protected Iterable<Scope> findTopScopes(RContext langContext) {
        return RScope.createTopScopes(langContext);
    }
}
