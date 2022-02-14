/*
 * Copyright (c) 2013, 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.context;

import java.util.HashMap;

import org.graalvm.options.OptionDescriptors;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.TruffleLanguage.ContextPolicy;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Instrumenter;
import com.oracle.truffle.api.instrumentation.ProvidedTags;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.ExecutableNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.r.runtime.ExitException;
import com.oracle.truffle.r.runtime.RAccuracyInfo;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RSuicide;
import com.oracle.truffle.r.runtime.conn.RFileTypeDetector;
import com.oracle.truffle.r.runtime.context.Engine.IncompleteSourceException;
import com.oracle.truffle.r.runtime.context.Engine.ParseException;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RTruffleObject;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.env.RScope;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;
import com.oracle.truffle.r.runtime.instrument.RSyntaxTags;
import com.oracle.truffle.r.runtime.instrument.RSyntaxTags.FunctionBodyBlockTag;
import com.oracle.truffle.r.runtime.interop.ConvertForeignObjectNode;
import com.oracle.truffle.r.runtime.interop.Foreign2R;

@TruffleLanguage.Registration(name = "R", id = "R", version = "4.0.3", implementationName = "FastR", characterMimeTypes = {RRuntime.R_APP_MIME,
                RRuntime.R_TEXT_MIME}, defaultMimeType = RRuntime.R_APP_MIME, contextPolicy = ContextPolicy.EXCLUSIVE, interactive = true, fileTypeDetectors = RFileTypeDetector.class, dependentLanguages = "llvm")
@ProvidedTags({StandardTags.CallTag.class, StandardTags.StatementTag.class, StandardTags.RootBodyTag.class, StandardTags.RootTag.class, RSyntaxTags.LoopTag.class, FunctionBodyBlockTag.class})
public final class TruffleRLanguage extends TruffleLanguage<RContext> {

    private static int activeContexts = 0;

    public String getRHome() {
        return getLanguageHome();
    }

    private static final ContextReference<RContext> contextReference = ContextReference.create(TruffleRLanguage.class);
    private static final LanguageReference<TruffleRLanguage> languageReference = LanguageReference.create(TruffleRLanguage.class);
    private static final String ACCESS_CLASS = "com.oracle.truffle.r.engine.TruffleRLanguageAccessImpl";
    private static final TruffleRLanguageAccess access;

    static {
        try {
            access = (TruffleRLanguageAccess) Class.forName(ACCESS_CLASS).newInstance();
        } catch (Exception e) {
            throw RSuicide.rSuicide("Failed to instantiate class: " + ACCESS_CLASS + ": " + e);
        }
    }

    private final HashMap<String, RFunction> builtinFunctionCache = new HashMap<>();

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

    public static boolean isAnyContextActive() {
        return activeContexts > 0;
    }

    private static boolean systemInitialized;

    @Override
    protected void initializeContext(RContext context) throws Exception {
        activeContexts++;
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
            access.onInitializeContext(env);
        }
        return RContext.create(this, env, env.lookup(Instrumenter.class), initialContext);
    }

    @Override
    protected OptionDescriptors getOptionDescriptors() {
        return FastROptions.getDescriptors();
    }

    @Override
    protected void disposeContext(RContext context) {
        activeContexts--;
        context.dispose();
    }

    @Override
    @TruffleBoundary
    protected Object getLanguageView(RContext context, Object value) {
        assert !(value instanceof RTruffleObject);
        Object rValue = Foreign2R.getUncached().convert(value);
        Object vector = RRuntime.asAbstractVector(rValue);
        if (rValue != vector) {
            return vector;
        }
        if (rValue instanceof TruffleObject) {
            return null;
        }
        RootCallTarget convertCallTarget = context.getOrCreateCachedCallTarget(ConvertForeignRootNode.class, () -> new ConvertForeignRootNode().getCallTarget());
        return convertCallTarget.call(rValue);
    }

    public static String toDisplayString(RContext context, Object value, boolean sideEffects) {
        return access.toDisplayString(context, value, sideEffects);
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
    protected CallTarget parse(ParsingRequest request) throws Exception {
        CompilerAsserts.neverPartOfCompilation();
        Source source = request.getSource();
        try {
            if (request.getArgumentNames().size() == 0) {
                return RContext.getEngine().parseToCallTarget(source, null);
            } else {
                return RContext.getEngine().parseToCallTargetWithArguments(source, request.getArgumentNames());
            }
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

    /**
     * For null {@code node} parameter this is a slow-path version.
     */
    public static RContext getCurrentContext(Node node) {
        return contextReference.get(node);
    }

    /**
     * Slow path version of get language.
     */
    public static TruffleRLanguage getCurrentLanguage() {
        return languageReference.get(null);
    }

    public static TruffleRLanguage getCurrentLanguage(Node node) {
        return languageReference.get(node);
    }

    @Override
    protected Object getScope(RContext context) {
        REnvironment globalEnv = REnvironment.globalEnv(context);
        return new RScope(globalEnv, globalEnv.getFrameAccess(), null);
    }

    private static final class ConvertForeignRootNode extends RootNode {
        @Child private ConvertForeignObjectNode convertForeignNode = ConvertForeignObjectNode.create();

        protected ConvertForeignRootNode() {
            super(null);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return convertForeignNode.convert((TruffleObject) frame.getArguments()[0]);
        }
    }
}
