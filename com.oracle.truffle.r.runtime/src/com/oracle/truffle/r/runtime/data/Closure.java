/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data;

import java.util.WeakHashMap;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.VirtualEvalFrame;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
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
public final class Closure {
    private WeakHashMap<FrameDescriptor, RootCallTarget> callTargets;

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

    private Closure(String closureName, RBaseNode expr, String syntaxLHSName) {
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
            } else if (constant instanceof RAbstractStringVector && ((RAbstractStringVector) constant).getLength() == 1) {
                this.stringConstant = ((RAbstractStringVector) constant).getDataAt(0);
            } else {
                this.stringConstant = null;
            }
        } else {
            this.stringConstant = null;
        }
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

    private synchronized RootCallTarget getCallTarget(FrameDescriptor desc, boolean canReuseExpr) {
        // This whole method is synchronized, not only the hash-map, so that we can lazily
        // initialize the call targets hash-map, and reuse 'expr' in case we're the first thread
        // executing this method
        // Create lazily, as it is not needed at all for INLINED promises!
        RootCallTarget result;
        if (callTargets == null) {
            callTargets = new WeakHashMap<>();
            result = generateCallTarget((RNode) (canReuseExpr ? expr : RContext.getASTBuilder().process(expr.asRSyntaxNode())));
            callTargets.put(desc, result);
        } else {
            result = callTargets.get(desc);
            if (result == null) {
                result = generateCallTarget((RNode) RContext.getASTBuilder().process(expr.asRSyntaxNode()));
                callTargets.put(desc, result);
            }
        }
        return result;
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
     * Evaluates this clousure in {@code envir} using caller {@code caller}.
     */
    public Object eval(REnvironment envir, RCaller caller) {
        CompilerAsserts.neverPartOfCompilation();

        FrameDescriptor desc = envir.getFrame().getFrameDescriptor();
        RootCallTarget callTarget = getCallTarget(desc, false);
        // Note: because we're creating new frame, we must not reuse expr, which may have cached
        // some frame slots
        MaterializedFrame vFrame = VirtualEvalFrame.create(envir.getFrame(), (RFunction) null, caller);
        return callTarget.call(vFrame);
    }

    private RootCallTarget generateCallTarget(RNode n) {
        return RContext.getEngine().makePromiseCallTarget(n, closureName + System.identityHashCode(n));
    }

    public RBaseNode getExpr() {
        return expr;
    }

    public RSyntaxElement getSyntaxElement() {
        return expr.asRSyntaxNode();
    }

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
}
