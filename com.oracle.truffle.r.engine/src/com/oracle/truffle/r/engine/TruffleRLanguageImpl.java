/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.util.HashMap;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.Scope;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.instrumentation.Instrumenter;
import com.oracle.truffle.api.instrumentation.ProvidedTags;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.ExecutableNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.engine.interop.RForeignAccessFactoryImpl;
import com.oracle.truffle.r.nodes.RASTBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinPackages;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode;
import com.oracle.truffle.r.nodes.function.RMissingHelper;
import com.oracle.truffle.r.nodes.instrumentation.RSyntaxTags;
import com.oracle.truffle.r.nodes.instrumentation.RSyntaxTags.FunctionBodyBlockTag;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.ExitException;
import com.oracle.truffle.r.runtime.context.FastROptions;
import com.oracle.truffle.r.runtime.RAccuracyInfo;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RDeparse;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RSuicide;
import com.oracle.truffle.r.runtime.conn.StdConnections.ContextStateImpl;
import com.oracle.truffle.r.runtime.context.Engine.IncompleteSourceException;
import com.oracle.truffle.r.runtime.context.Engine.ParseException;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.TruffleRLanguage;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RInteropNA;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RTypedValue;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.env.RScope;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import org.graalvm.options.OptionDescriptors;

@TruffleLanguage.Registration(name = "R", id = "R", version = "3.5.1", mimeType = {RRuntime.R_APP_MIME, RRuntime.R_TEXT_MIME}, interactive = true, fileTypeDetectors = RFileTypeDetector.class)
@ProvidedTags({StandardTags.CallTag.class, StandardTags.StatementTag.class, StandardTags.RootTag.class, RSyntaxTags.LoopTag.class, FunctionBodyBlockTag.class})
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
                RSuicide.rSuicide("error during R language initialization");
            } catch (ExitException ex) {
                System.exit(ex.getStatus());
            }
        }
    }

    private static boolean systemInitialized;

    @Override
    protected boolean isObjectOfLanguage(Object object) {
        return object instanceof RTypedValue;
    }

    @Override
    protected void initializeContext(RContext context) throws Exception {
        if (!systemInitialized) {
            initialize();
            systemInitialized = true;
        }
        context.initializeContext();
    }

    @Override
    protected RContext createContext(Env env) {
        boolean initialContext = !systemInitialized;
        if (initialContext) {
            RContext.initializeGlobalState(new RASTBuilder(false), new RRuntimeASTAccessImpl(), RBuiltinPackages.getInstance(), new RForeignAccessFactoryImpl());
        }
        return RContext.create(this, env, env.lookup(Instrumenter.class), initialContext);
    }

    @Override
    protected OptionDescriptors getOptionDescriptors() {
        return FastROptions.getDescriptors();
    }

    @Override
    protected void disposeContext(RContext context) {
        context.dispose();
    }

    @Override
    protected String toString(RContext context, Object value) {
        // primitive values are never produced by FastR so we don't print them as R vectors
        if (value instanceof Boolean) {
            // boolean constants are capitalized like in R
            return (boolean) value ? "TRUE" : "FALSE";
        }
        if (value instanceof Number || value instanceof String || value instanceof Character) {
            return value.toString();
        }

        // special class designated to exchange NA values with the outside world
        // this value is a scalar, the only way to get it is via getArrayMember on an R vector
        if (value instanceof RInteropNA) {
            return "NA";
        }

        // the debugger also passes result of TruffleRLanguage.findMetaObject() to this method
        Object unwrapped = value;
        // print promises by other means than the "print" function to avoid evaluating them
        if (unwrapped instanceof RPromise) {
            RPromise promise = (RPromise) unwrapped;
            if (promise.isEvaluated() || promise.isOptimized()) {
                unwrapped = promise.getValue();
            } else {
                return RDeparse.deparse(unwrapped, RDeparse.MAX_CUTOFF, true, RDeparse.KEEPINTEGER, -1, 1024 * 1024);
            }
        }
        // print missing explicitly, because "print" would report missing argument
        if (RMissingHelper.isMissing(unwrapped)) {
            return "missing";
        }

        // the value unwrapped from an RPromise can be primitive Java type, but now we know that we
        // are dealing with primitive that is supposed to be treated as R vector
        unwrapped = RRuntime.asAbstractVector(unwrapped);
        if (!(unwrapped instanceof TruffleObject)) {
            throw RError.error(RError.NO_CALLER, Message.GENERIC, String.format("Printing value of type '%s' is not supported by the R language.", unwrapped.getClass().getSimpleName()));
        }
        Object printObj = REnvironment.baseEnv(context).get("print");
        if (printObj instanceof RPromise) {
            printObj = PromiseHelperNode.evaluateSlowPath((RPromise) printObj);
        }
        if (!(printObj instanceof RFunction)) {
            throw RError.error(RError.NO_CALLER, Message.GENERIC, "Cannot retrieve the 'print' function from the base package.");
        }
        MaterializedFrame callingFrame = REnvironment.globalEnv(context).getFrame();
        ContextStateImpl stateStdConnections = context.stateStdConnections;
        try {
            StringBuilder buffer = new StringBuilder();
            stateStdConnections.setBuffer(buffer);
            RContext.getEngine().evalFunction((RFunction) printObj, callingFrame, RCaller.topLevel, false, ArgumentsSignature.empty(1), unwrapped);
            // remove the last "\n", which is useful for REPL, but not here
            if (buffer.length() > 0 && buffer.charAt(buffer.length() - 1) == '\n') {
                buffer.setLength(buffer.length() - 1);
            }
            return buffer.toString();
        } finally {
            stateStdConnections.resetBuffer();
        }
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
        Source source = request.getSource();
        try {
            return RContext.getEngine().parseToCallTarget(source, null);
        } catch (IncompleteSourceException e) {
            throw e;
        } catch (ParseException e) {
            if (source.isInteractive()) {
                throw e.throwAsRError();
            } else {
                throw e;
            }
        }
    }

    @Override
    protected ExecutableNode parse(InlineParsingRequest request) throws Exception {
        CompilerAsserts.neverPartOfCompilation();
        try {
            return RContext.getEngine().parseToExecutableNode(request.getSource());
        } catch (IncompleteSourceException e) {
            throw e;
        } catch (ParseException e) {
            if (request.getSource().isInteractive()) {
                throw e.throwAsRError();
            } else {
                throw e;
            }
        }
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
