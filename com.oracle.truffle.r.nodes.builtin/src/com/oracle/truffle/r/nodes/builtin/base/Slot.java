/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996, 1997  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1995-2014, The R Core Team
 * Copyright (c) 2002-2008, The R Foundation
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
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

@RBuiltin(name = "@", kind = PRIMITIVE, parameterNames = {"", ""}, nonEvalArgs = 1, behavior = COMPLEX)
public abstract class Slot extends RBuiltinNode.Arg2 {

    private static final int UNINITIALIZED = 0;
    private static final int IS_LHS = 1;
    private static final int IS_NOT_LHS = 2;

    @CompilationFinal private int isLhsState = UNINITIALIZED;
    @Child private UpdateShareableChildValueNode sharedAttrUpdate = UpdateShareableChildValueNode.create();
    @Child private AccessSlotNode accessSlotNode = AccessSlotNodeGen.create(true);

    static {
        Casts casts = new Casts(Slot.class);
        casts.arg(0).returnIf(foreign()).asAttributable(true, true, true);
    }

    private String getName(Object nameObj) {
        if (nameObj instanceof RPromise) {
            RPromise promise = (RPromise) nameObj;
            Closure closure = promise.getClosure();
            if (closure.asStringConstant() != null) {
                return closure.asStringConstant();
            } else if (closure.asSymbol() != null) {
                return closure.asSymbol();
            }
        }
        CompilerDirectives.transferToInterpreter();
        throw error(RError.Message.GENERIC, "invalid type or length for slot name");
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
        assert Utils.isInterned(name);

        // just return evaluated receiver object and name
        return RCallNode.createDeferredMemberAccess(object, name);
    }

    @Specialization(guards = "!isLhsOfForeignCall(object)")
    protected Object getSlot(Object object, Object nameObj,
                    @Cached("createClassProfile()") ValueProfile nameObjProfile) {
        String name = getName(nameObjProfile.profile(nameObj));
        assert Utils.isInterned(name);
        Object result = accessSlotNode.executeAccess(object, name);

        // since we give the slot away, we probably have to increase the refCount
        sharedAttrUpdate.execute(object, result);
        return result;
    }

}
