/*
 * Copyright (c) 2019, 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data;

import java.lang.ref.WeakReference;
import java.util.function.Supplier;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.TruffleLogger;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RLogger;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.VirtualEvalFrame;
import com.oracle.truffle.r.runtime.context.FastROptions;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxCall;
import com.oracle.truffle.r.runtime.nodes.RSyntaxConstant;
import com.oracle.truffle.r.runtime.nodes.RSyntaxElement;
import com.oracle.truffle.r.runtime.nodes.RSyntaxLookup;

/**
 * A closure for creating promises and languages.
 */
public final class Closure implements Cloneable {
    private final Object cacheLock;
    private CallTargetCacheImpl callTargetCache;

    public static final String PROMISE_CLOSURE_WRAPPER_NAME = new String("<promise>");
    public static final String LANGUAGE_CLOSURE_WRAPPER_NAME = new String("<language>");

    private static final RStringVector NULL_MARKER = new RStringVector(new String[0], true);

    private final RBaseNode expr;
    private final String symbol;
    private final String stringConstant;
    private final String closureName;

    // the first entry in the "names" attribute (special case for pairlist representation)
    private final String syntaxLHSName;

    private RStringVector namesVector; // may be null if never queried

    public Closure(String closureName, RBaseNode expr, String syntaxLHSName) {
        this.closureName = closureName;
        this.expr = expr;
        this.syntaxLHSName = syntaxLHSName;
        if (expr.asRSyntaxNode() instanceof RSyntaxLookup) {
            this.symbol = Utils.intern(((RSyntaxLookup) expr.asRSyntaxNode()).getIdentifier());
        } else {
            this.symbol = null;
        }
        if (expr.asRSyntaxNode() instanceof RSyntaxConstant) {
            Object constant = ((RSyntaxConstant) expr.asRSyntaxNode()).getValue();
            if (constant instanceof String) {
                this.stringConstant = (String) constant;
            } else if (constant instanceof RStringVector && ((RStringVector) constant).getLength() == 1) {
                this.stringConstant = ((RStringVector) constant).getDataAt(0);
            } else {
                this.stringConstant = null;
            }
        } else {
            this.stringConstant = null;
        }
        RContext context = RContext.getInstance(expr);
        cacheLock = context.getOption(FastROptions.EnableClosureCallTargetsCache) ? new Object() : null;
    }

    @Override
    public Closure clone() {
        return new Closure(closureName, (RNode) RContext.getASTBuilder().process(expr.asRSyntaxNode()), syntaxLHSName);
    }

    public static Closure createPromiseClosure(RBaseNode expr) {
        return new Closure(PROMISE_CLOSURE_WRAPPER_NAME, expr, null);
    }

    public static Closure createLanguageClosure(RBaseNode expr, String lhsName) {
        return new Closure(LANGUAGE_CLOSURE_WRAPPER_NAME, expr, lhsName);
    }

    public static Closure createLanguageClosure(RBaseNode expr) {
        return new Closure(LANGUAGE_CLOSURE_WRAPPER_NAME, expr, null);
    }

    public static Closure create(String name, RBaseNode expr) {
        return new Closure(name, expr, null);
    }

    public String getSyntaxLHSName() {
        return syntaxLHSName;
    }

    private RootCallTarget getCallTarget(FrameDescriptor desc, boolean canReuseExpr) {
        if (!RContext.getInstance().getOption(FastROptions.EnableClosureCallTargetsCache)) {
            return CallTargetCacheImpl.createCallTarget(CallTargetCache.processExpr(canReuseExpr, expr), closureName);
        }
        synchronized (cacheLock) {
            if (callTargetCache == null) {
                callTargetCache = new CallTargetCacheImpl();
            }
            return callTargetCache.get(desc, canReuseExpr, expr, closureName);
        }
    }

    /**
     * Evaluates a {@link com.oracle.truffle.r.runtime.data.Closure} in {@code frame}.
     */
    public Object eval(MaterializedFrame frame) {
        CompilerAsserts.neverPartOfCompilation();

        FrameDescriptor desc = frame.getFrameDescriptor();
        RootCallTarget callTarget = getCallTarget(desc, true);
        return callTarget.call(frame);
    }

    /**
     * Evaluates this closure in {@code envir} using caller {@code caller}.
     */
    public Object eval(REnvironment envir, Object callerFrame, RCaller caller, RFunction function) {
        CompilerAsserts.neverPartOfCompilation();

        FrameDescriptor desc = envir.getFrame().getFrameDescriptor();
        RootCallTarget callTarget = getCallTarget(desc, false);
        // Note: because we're creating new frame, we must not reuse expr, which may have cached
        // some frame slots
        MaterializedFrame vFrame = VirtualEvalFrame.create(envir.getFrame(), function, callerFrame, caller);
        return callTarget.call(vFrame);
    }

    public RBaseNode getExpr() {
        return expr;
    }

    public RSyntaxElement getSyntaxElement() {
        return expr.asRSyntaxNode();
    }

    /**
     * If this closure represents a lookup, this returns the looked up symbol, otherwise returns
     * {@code null}. The string is already interned.
     */
    public String asSymbol() {
        return symbol;
    }

    public String asStringConstant() {
        return stringConstant;
    }

    public RStringVector getNamesVector() {
        if (namesVector == null) {
            initializeNamesVector();
        }
        return namesVector == NULL_MARKER ? null : namesVector;
    }

    @TruffleBoundary
    private void initializeNamesVector() {
        RSyntaxElement node = getSyntaxElement();
        if (node instanceof RSyntaxCall) {
            RSyntaxCall call = (RSyntaxCall) node;
            /*
             * If the function or any argument has a name, then all arguments (and the function) are
             * given names, with unnamed arguments getting "". However, if no arguments have names,
             * the result is NULL (null)
             */
            boolean hasName = false;
            String functionName = "";
            if (getSyntaxLHSName() != null) {
                hasName = true;
                functionName = getSyntaxLHSName();
            }
            ArgumentsSignature sig = call.getSyntaxSignature();
            if (!hasName) {
                for (int i = 0; i < sig.getLength(); i++) {
                    if (sig.getName(i) != null) {
                        hasName = true;
                        break;
                    }
                }
            }
            if (!hasName) {
                namesVector = NULL_MARKER;
            } else {
                String[] data = new String[sig.getLength() + 1];
                data[0] = functionName; // function
                for (int i = 0; i < sig.getLength(); i++) {
                    String name = sig.getName(i);
                    data[i + 1] = name == null ? "" : name;
                }
                namesVector = RDataFactory.getPermanent().createStringVector(data, RDataFactory.COMPLETE_VECTOR);
            }
        } else {
            namesVector = NULL_MARKER;
        }
    }

    public static final class CallTargetCacheImpl extends CallTargetCache {
        private static final TruffleLogger LOGGER = RLogger.getLogger(CallTargetCache.class.getName());

        @Override
        protected RootCallTarget generateCallTarget(RNode n, String closureName) {
            return createCallTarget(n, closureName);
        }

        static RootCallTarget createCallTarget(RNode n, String closureName) {
            return RContext.getEngine().makePromiseCallTarget(n, closureName + System.identityHashCode(n));
        }

        @Override
        protected void log(Supplier<String> messageSupplier) {
            LOGGER.fine(messageSupplier);
        }
    }

    public abstract static class CallTargetCache {
        private static final int CACHE_SIZE = 2;

        @SuppressWarnings({"unchecked", "rawtypes"}) private WeakReference<FrameDescriptor>[] cacheKeys = new WeakReference[CACHE_SIZE];
        private RootCallTarget[] cacheValues = new RootCallTarget[CACHE_SIZE];
        // contains values from 0 to CACHE_SIZE-1, or garbage if cacheKeys[idx] == null
        private byte[] cacheLastUsed = new byte[CACHE_SIZE];

        public RootCallTarget get(FrameDescriptor desc, boolean canReuseExpr, RBaseNode expr, String closureName) {
            // purge the cache, find the existing value, empty slot, and last used slot
            int emptyIdx = -1;
            int lastUsedIdx = -1;
            int lastUsedValue = CACHE_SIZE;
            int resultIdx = -1;
            byte itemsCount = 0;
            for (int i = 0; i < CACHE_SIZE; i++) {
                WeakReference<FrameDescriptor> key = cacheKeys[i];
                if (key == null) {
                    emptyIdx = i;
                    continue;
                }
                FrameDescriptor keyFd = key.get();
                if (keyFd == null) {
                    cacheKeys[i] = null;
                    cacheValues[i] = null;
                    cacheLastUsed[i] = 0;
                    emptyIdx = i;
                    continue;
                }
                itemsCount++;
                if (cacheLastUsed[i] < lastUsedValue) {
                    lastUsedIdx = i;
                    lastUsedValue = cacheLastUsed[i];
                }
                if (keyFd == desc) {
                    resultIdx = i;
                }
            }
            // put the value into the cache if not found
            if (resultIdx == -1) {
                if (emptyIdx != -1) {
                    resultIdx = emptyIdx;
                } else {
                    // cache must be full if we didn't find empty slot
                    assert itemsCount == CACHE_SIZE;
                    int finalLastUsedIdx = lastUsedIdx;
                    int finalLastUsedValue = lastUsedValue;
                    log(() -> String.format("Closure Cache for '%s' evicted item %d with last used value %d", closureName, finalLastUsedIdx, finalLastUsedValue));
                    resultIdx = lastUsedIdx;
                }
                cacheKeys[resultIdx] = new WeakReference<>(desc);
                cacheValues[resultIdx] = generateCallTarget(processExpr(canReuseExpr, expr), closureName);
            }
            // update last used
            for (int i = 0; i < CACHE_SIZE; i++) {
                cacheLastUsed[i] = (byte) Math.max(0, cacheLastUsed[i] - 1);
            }
            cacheLastUsed[resultIdx] = CACHE_SIZE - 1;

            return cacheValues[resultIdx];
        }

        public static RNode processExpr(boolean canReuseExpr, RBaseNode expr) {
            return (RNode) (canReuseExpr ? expr : RContext.getASTBuilder().process(expr.asRSyntaxNode()));
        }

        protected abstract RootCallTarget generateCallTarget(RNode n, String closureName);

        protected abstract void log(Supplier<String> messageSupplier);

        // Used for tests
        public boolean check(FrameDescriptor desc, boolean present, int expectedLastUse) {
            for (int i = 0; i < CACHE_SIZE; i++) {
                WeakReference<FrameDescriptor> key = cacheKeys[i];
                if (key != null && key.get() == desc) {
                    return present && cacheLastUsed[i] == expectedLastUse;
                }
            }
            return !present;
        }
    }
}
