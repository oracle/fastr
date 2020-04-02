/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInterface;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.r.runtime.context.Engine;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.TruffleRLanguage;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RAttributesLayout.RAttribute;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxElement;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;
import com.oracle.truffle.r.runtime.nodes.unary.CastNode;

/**
 * A collection of methods that need access to the AST types, needed by code that resides in the
 * runtime project, which does not have direct access, as it would introduce project circularities.
 */
public interface RRuntimeASTAccess {

    /**
     * Returns the real caller associated with {@code rl}, by locating the {@code RSyntaxNode}
     * associated with the node stored with {@code rl}. It may return {@code null} if there is no
     * valid caller.
     */
    RPairList getSyntaxCaller(RCaller rl);

    /**
     * Gets {@code TruffleRLanguage} avoiding project circularity.
     */
    Class<? extends TruffleRLanguage> getTruffleRLanguage();

    /**
     * Returns a string for a call as represented by {@code rl}, returned originally by
     * {@link #getSyntaxCaller}.
     */
    String getCallerSource(RPairList rl);

    /**
     * Used by error/warning handling to try to find the call that provoked the error/warning.
     *
     * If there is no caller, return {@link RNull#instance}, e.g. "call" of a builtin from the
     * global env, otherwise return an {@code RPairList} instance that represents the call.
     *
     * @param call may be {@code null} or it may be the {@link Node} that was executing when the
     *            error.warning was generated (builtin or associated node).
     */
    Object findCaller(RBaseNode call);

    /**
     * Convenience method for {@code getCallerSource(getSyntaxCaller(caller))}.
     */
    default String getCallerSource(RCaller caller) {
        final RPairList syntaxCaller = getSyntaxCaller(caller);
        return syntaxCaller == null ? "<invalid call>" : getCallerSource(syntaxCaller);
    }

    /**
     * Callback to an R function from the internal implementation. Since this is part of the
     * implementation and not part of the user-visible execution debug handling is disabled across
     * the call.
     */
    Object callback(RFunction f, Object[] args);

    /**
     * Force a promise by slow-path evaluation.
     */
    Object forcePromise(String identifier, Object val);

    /**
     * Access to Rm.removeFromEnv.
     */
    boolean removeFromEnv(REnvironment env, String key);

    /**
     * Force a promise by slow-path evaluation.
     */
    Object forcePromise(RPromise promise);

    interface ExplicitFunctionCall extends NodeInterface {
        Object call(VirtualFrame frame, RFunction function, RArgsValuesAndNames args);
    }

    /**
     * Access to RExplicitCallNode.
     */
    ExplicitFunctionCall createExplicitFunctionCall();

    /**
     * Returns the {@link ArgumentsSignature} for {@code f}.
     */
    ArgumentsSignature getArgumentsSignature(RFunction f);

    /**
     * Returns the default parameters for a builtin.
     */
    Object[] getBuiltinDefaultParameterValues(RFunction f);

    /**
     * Update the {@code name} in a {@code FunctionDefinitionNode}.
     */
    void setFunctionName(RootNode node, String name);

    Engine createEngine(RContext context);

    /**
     * Returns {@code true} iff {@code node} is an instance of {@code FunctionDefinitionNode}, which
     * is not visible from {@code runtime}, or {@code false} otherwise.
     */
    boolean isFunctionDefinitionNode(Node node);

    /**
     * Project circularity workaround.
     */
    void traceAllFunctions();

    /**
     * Project circularity workaround. Equivalent to
     * RASTUtils.unwrap(promise.getRep()).asRSyntaxNode().
     */
    RSyntaxNode unwrapPromiseRep(RPromise promise);

    /**
     * cf. {@code Node.isTaggedWith(tag)}.
     */
    boolean isTaggedWith(Node node, Class<?> tag);

    boolean enableDebug(RFunction func, boolean once);

    boolean disableDebug(RFunction func);

    boolean isDebugged(RFunction func);

    /*
     * Support for R/RScript sessions ("processes") in an isolated RContext, see
     * .fastr.context.r/rscript. The args are everything you might legally enter into a
     * shell,including I/O redirection. The result is an integer status code if "intern==false",
     * otherwise it is a character vector of the output, with a 'status' attribute containing the
     * status code. The env arguments are an optional settings of environment variables of the form
     * X=Y.
     */

    Object rcommandMain(RContext contexrt, String[] args, String[] env, boolean intern, int timeoutSecs);

    Object rscriptMain(RContext contexrt, String[] args, String[] env, boolean intern, int timeoutSecs);

    String encodeDouble(double x);

    String encodeDouble(double x, int digits);

    String encodeComplex(RComplex x);

    String encodeComplex(RComplex x, int digits);

    RAbstractStringVector getClassHierarchy(RAttributable value);

    RContext getCurrentContext();

    Object createLanguageElement(RSyntaxElement element);

    interface ArrayAttributeAccess extends NodeInterface {
        RAttribute[] execute(Object attrs);
    }

    ArrayAttributeAccess createArrayAttributeAccess(boolean cached);

    interface UpdateSlotAccess extends NodeInterface {
        Object execute(VirtualFrame frame, Object object, Object name, Object value);
    }

    UpdateSlotAccess createUpdateSlotAccess();

    interface AccessSlotAccess extends NodeInterface {
        Object execute(Object o, Object name);
    }

    AccessSlotAccess createAccessSlotAccess();

    CastNode getNamesAttributeValueCastNode();

    CastNode getDimAttributeValueCastNode();
}
