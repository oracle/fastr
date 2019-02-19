/*
 * Copyright (c) 1995, 1996, 1997  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1995-2014, The R Core Team
 * Copyright (c) 2002-2008, The R Foundation
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, a copy is available at
 * https://www.R-project.org/Licenses/
 */

package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.foreign;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.nodes.access.AccessSlotNode;
import com.oracle.truffle.r.nodes.access.AccessSlotNodeGen;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.function.RCallNode;
import com.oracle.truffle.r.nodes.function.RCallNode.BuiltinCallNode;
import com.oracle.truffle.r.nodes.function.opt.UpdateShareableChildValueNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.Closure;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxCall;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

/**
 * Unevaluated lookups (promises) are used as arguments for slot accesses, e.g. in {@code foo@bar},
 * the second argument to {@code `@`} won't be evaluated, but passed in as promise to {@link Slot}
 * or {@link UpdateSlot}. This node factors out the common code to extract the String name out of
 * the promise.
 */
final class PromiseAsNameNode extends Node {
    private static final String ERROR_MARKER = new String();
    @CompilationFinal private String value;

    public String execute(Object nameObj) {
        if (value == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            value = getValue(nameObj);
        }
        if (Utils.identityEquals(value, ERROR_MARKER)) {
            CompilerDirectives.transferToInterpreter();
            throw RError.error(RError.NO_CALLER, RError.Message.GENERIC, "invalid type or length for slot name");
        }
        return value;
    }

    private static String getValue(Object nameObj) {
        String value = null;
        if (nameObj instanceof RPromise) {
            RPromise promise = (RPromise) nameObj;
            Closure closure = promise.getClosure();
            if (closure.asStringConstant() != null) {
                value = closure.asStringConstant();
            } else if (closure.asSymbol() != null) {
                value = closure.asSymbol();
            }
            assert value == null || Utils.isInterned(value);
            // Note: the promise is never evaluated even in GNU R
        }
        return value == null ? ERROR_MARKER : value;
    }
}

@RBuiltin(name = "@", kind = PRIMITIVE, parameterNames = {"", ""}, nonEvalArgs = 1, behavior = COMPLEX)
public abstract class Slot extends RBuiltinNode.Arg2 {

    private static final int UNINITIALIZED = 0;
    private static final int IS_LHS = 1;
    private static final int IS_NOT_LHS = 2;

    @CompilationFinal private int isLhsState = UNINITIALIZED;
    @Child private UpdateShareableChildValueNode sharedAttrUpdate;
    @Child private AccessSlotNode accessSlotNode;
    @Child private PromiseAsNameNode promiseAsNameNode;

    static {
        Casts casts = new Casts(Slot.class);
        casts.arg(0).castForeignObjects(false).returnIf(foreign()).asAttributable(true, true, true);
    }

    private String getName(Object nameObj) {
        if (promiseAsNameNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            promiseAsNameNode = insert(new PromiseAsNameNode());
        }
        return promiseAsNameNode.execute(nameObj);
    }

    private static boolean isLhsOfSyntaxCall(RSyntaxNode n) {
        Node unwrapParent = RASTUtils.unwrapParent(n.asNode());
        return unwrapParent instanceof RSyntaxCall && ((RSyntaxCall) unwrapParent).getSyntaxLHS() == n;
    }

    private boolean isLhsOfCall() {
        if (isLhsState == UNINITIALIZED) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            Node unwrapParent = RASTUtils.unwrapParent(this);
            assert ((BuiltinCallNode) unwrapParent).getBuiltin() == this;
            if (unwrapParent instanceof BuiltinCallNode && isLhsOfSyntaxCall(((RBaseNode) unwrapParent).asRSyntaxNode())) {
                isLhsState = IS_LHS;
            } else {
                isLhsState = IS_NOT_LHS;
            }
        }
        return isLhsState == IS_LHS;
    }

    protected boolean isLhsOfForeignCall(Object o) {
        return RRuntime.isForeignObject(o) && isLhsOfCall();
    }

    @Specialization(guards = "isLhsOfForeignCall(object)")
    protected Object getSlot(TruffleObject object, Object nameObj,
                    @Cached("createClassProfile()") ValueProfile nameObjProfile) {
        String name = getName(nameObjProfile.profile(nameObj));

        // just return evaluated receiver object and name
        return RCallNode.createDeferredMemberAccess(object, name);
    }

    @Specialization(guards = "!isLhsOfForeignCall(object)")
    protected Object getSlot(Object object, Object nameObj,
                    @Cached("createClassProfile()") ValueProfile nameObjProfile) {
        String name = getName(nameObjProfile.profile(nameObj));

        if (accessSlotNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            accessSlotNode = insert(AccessSlotNodeGen.create(true));
        }
        Object result = accessSlotNode.executeAccess(object, name);

        // since we give the slot away, we probably have to increase the refCount
        if (sharedAttrUpdate == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            sharedAttrUpdate = insert(UpdateShareableChildValueNode.create());
        }
        sharedAttrUpdate.execute(object, result);
        return result;
    }

}
