/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.function.opt.eval;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.Closure;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RPairList.RPairListSnapshotNode;
import com.oracle.truffle.r.runtime.data.RPairListLibrary;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RPromise.PromiseState;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.nodes.ShareObjectNode;
import com.oracle.truffle.r.runtime.gnur.SEXPTYPE;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

/**
 * TODO: There are several specializations caching {@link Closure}, which should be done otherwise,
 * as no runtime object should be stored in an AST to allow sharing that AST among different
 * contexts.
 */
public abstract class ArgValueSupplierNode extends Node {

    @Child private ShareObjectNode sharedObjectNode;

    protected final int monoCacheSize;

    protected ArgValueSupplierNode(boolean cached) {
        monoCacheSize = cached ? DSLConfig.getCacheSize(1) : 0;
    }

    public abstract Object execute(Object a, int i, CallInfo.ArgumentBuilderState argBuilderState, MaterializedFrame currentFrame, MaterializedFrame promiseEvalFrame,
                    PromiseHelperNode promiseHelper);

    boolean isFieldAccessor(CallInfo.ArgumentBuilderState argBuilderState, int i) {
        return argBuilderState.isFieldAccess && i == 1;
    }

    @Specialization(guards = {"isFieldAccessor(argBuilderState, i)"})
    Object buildArgFieldAccess(RSymbol sym, @SuppressWarnings("unused") int i, @SuppressWarnings("unused") CallInfo.ArgumentBuilderState argBuilderState,
                    @SuppressWarnings("unused") MaterializedFrame currentFrame,
                    @SuppressWarnings("unused") MaterializedFrame promiseEvalFrame, @SuppressWarnings("unused") PromiseHelperNode promiseHelper) {
        return sym.getName();
    }

    @TruffleBoundary
    Closure createSymbolClosure(RSymbol sym, int i) {
        RSyntaxNode lookupSyntaxNode = RContext.getASTBuilder().lookup(RSyntaxNode.LAZY_DEPARSE, sym.getName(), false);
        return Closure.createPromiseClosure(CallInfo.wrapArgNode(i, lookupSyntaxNode));
    }

    @Specialization(guards = {"sym.getName() == cachedSymName", "!isFieldAccessor(argBuilderState, i)"}, limit = "monoCacheSize")
    Object buildSymbolArgCached(@SuppressWarnings("unused") RSymbol sym, @SuppressWarnings("unused") int i, CallInfo.ArgumentBuilderState argBuilderState, MaterializedFrame currentFrame,
                    MaterializedFrame promiseEvalFrame,
                    PromiseHelperNode promiseHelper,
                    @SuppressWarnings("unused") @Cached("sym.getName()") String cachedSymName,
                    @Cached("createSymbolClosure(sym, i)") Closure cachedClosure) {
        Object arg;
        if (ArgumentsSignature.VARARG_NAME.equals(cachedSymName)) {
            if (argBuilderState.varArgs == null) {
                RPromise promise = RDataFactory.createPromise(PromiseState.Supplied, cachedClosure,
                                promiseEvalFrame);
                argBuilderState.varArgs = (RArgsValuesAndNames) promiseHelper.evaluate(currentFrame, promise);

            }
            arg = argBuilderState.varArgs;
        } else {
            arg = RDataFactory.createPromise(PromiseState.Supplied, cachedClosure, promiseEvalFrame);
        }

        return arg;
    }

    @Specialization(replaces = "buildSymbolArgCached", guards = "!isFieldAccessor(argBuilderState, i)")
    Object buildSymbolArg(RSymbol sym, int i, CallInfo.ArgumentBuilderState argBuilderState, MaterializedFrame currentFrame, MaterializedFrame promiseEvalFrame,
                    PromiseHelperNode promiseHelper) {
        return buildSymbolArgCached(sym, i, argBuilderState, currentFrame, promiseEvalFrame, promiseHelper, sym.getName(), createSymbolClosure(sym, i));
    }

    Closure createClosure(RPairList a, int i, RPairListLibrary plLib) {
        RPairList pl = RDataFactory.createPairList(plLib.car(a), plLib.cdr(a), plLib.getTag(a), SEXPTYPE.LANGSXP);
        Closure closure = CallInfo.createPromiseClosure(pl, a.getAttributes(), i);
        return closure;
    }

    @Specialization(guards = {"a.hasClosure()", "cachedFrameDescriptor == promiseEvalFrame.getFrameDescriptor()", "plSnapshotNode.execute(a)"}, limit = "monoCacheSize")
    Object buildPromiseUsingExistingClosure(@SuppressWarnings("unused") RPairList a, @SuppressWarnings("unused") int i,
                    @SuppressWarnings("unused") CallInfo.ArgumentBuilderState argBuilderState,
                    @SuppressWarnings("unused") MaterializedFrame currentFrame, MaterializedFrame promiseEvalFrame,
                    @SuppressWarnings("unused") PromiseHelperNode promiseHelper,
                    @SuppressWarnings("unused") @CachedLibrary(limit = "1") RPairListLibrary plLib,
                    @SuppressWarnings("unused") @Cached("create(a)") RPairListSnapshotNode plSnapshotNode,
                    @Cached("plLib.getClosure(a).clone()") Closure cachedClosure,
                    @SuppressWarnings("unused") @Cached("promiseEvalFrame.getFrameDescriptor()") FrameDescriptor cachedFrameDescriptor) {
        return RDataFactory.createPromise(PromiseState.Supplied, cachedClosure, promiseEvalFrame);
    }

    @Specialization(guards = {"!a.hasClosure()", "cachedFrameDescriptor == promiseEvalFrame.getFrameDescriptor()", "plSnapshotNode.execute(a)"}, limit = "monoCacheSize")
    Object buildPromiseUsingCachedClosure(@SuppressWarnings("unused") RPairList a, @SuppressWarnings("unused") int i,
                    @SuppressWarnings("unused") CallInfo.ArgumentBuilderState argBuilderState,
                    @SuppressWarnings("unused") MaterializedFrame currentFrame, MaterializedFrame promiseEvalFrame,
                    @SuppressWarnings("unused") PromiseHelperNode promiseHelper,
                    @SuppressWarnings("unused") @CachedLibrary(limit = "1") RPairListLibrary plLib,
                    @SuppressWarnings("unused") @Cached("create(a)") RPairListSnapshotNode plSnapshotNode,
                    @Cached("createClosure(a, i, plLib)") Closure cachedClosure,
                    @SuppressWarnings("unused") @Cached("promiseEvalFrame.getFrameDescriptor()") FrameDescriptor cachedFrameDescriptor) {
        return RDataFactory.createPromise(PromiseState.Supplied, cachedClosure, promiseEvalFrame);
    }

    @Specialization(replaces = {"buildPromiseUsingCachedClosure", "buildPromiseUsingExistingClosure"})
    Object buildPromise(RPairList a, int i, @SuppressWarnings("unused") CallInfo.ArgumentBuilderState argBuilderState, @SuppressWarnings("unused") MaterializedFrame currentFrame,
                    MaterializedFrame promiseEvalFrame, @SuppressWarnings("unused") PromiseHelperNode promiseHelper,
                    @CachedLibrary(limit = "1") RPairListLibrary plLib) {
        Closure closure = createClosure(a, i, plLib);
        return RDataFactory.createPromise(PromiseState.Supplied, closure, promiseEvalFrame);
    }

    @Fallback
    Object buildOther(Object a, @SuppressWarnings("unused") int i, @SuppressWarnings("unused") CallInfo.ArgumentBuilderState argBuilderState,
                    @SuppressWarnings("unused") MaterializedFrame currentFrame,
                    @SuppressWarnings("unused") MaterializedFrame promiseEvalFrame, @SuppressWarnings("unused") PromiseHelperNode promiseHelper) {
        if (sharedObjectNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            sharedObjectNode = insert(ShareObjectNode.create());
        }
        return sharedObjectNode.execute(a);
    }

}
