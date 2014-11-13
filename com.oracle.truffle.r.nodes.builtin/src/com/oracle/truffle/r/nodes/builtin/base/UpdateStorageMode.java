/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.binary.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.RAttributes.RAttribute;

@RBuiltin(name = "storage.mode<-", kind = PRIMITIVE, parameterNames = {"x", ""})
// 2nd parameter is "value", but should not be matched against, so ""
public abstract class UpdateStorageMode extends RBuiltinNode {

    @Child private TypeofNode typeof;
    @Child private CastTypeNode castTypeNode;
    @Child private IsFactorNode isFactor;

    private final ValueProfile modeProfile = ValueProfile.createIdentityProfile();
    private final BranchProfile errorProfile = BranchProfile.create();

    @Specialization
    protected Object update(VirtualFrame frame, Object x, String value) {
        controlVisibility();
        RType mode = RType.fromString(modeProfile.profile(value));
        if (mode == RType.DefunctReal || mode == RType.DefunctSingle) {
            errorProfile.enter();
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.USE_DEFUNCT, mode.getName(), mode == RType.DefunctSingle ? "mode<-" : "double");
        }
        initTypeOfNode();
        RType typeX = typeof.execute(frame, x);
        if (typeX == mode) {
            return x;
        }
        initFactorNode();
        if (isFactor.execute(frame, x) == RRuntime.LOGICAL_TRUE) {
            errorProfile.enter();
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_STORAGE_MODE_UPDATE);
        }
        initCastTypeNode();
        if (mode != null) {
            Object result = castTypeNode.execute(frame, x, mode);
            if (result != null) {
                if (x instanceof RAttributable && result instanceof RAttributable) {
                    RAttributable rx = (RAttributable) x;
                    RAttributes attrs = rx.getAttributes();
                    if (attrs != null) {
                        RAttributable rresult = (RAttributable) result;
                        for (RAttribute attr : attrs) {
                            String attrName = attr.getName();
                            Object v = attr.getValue();
                            if (attrName.equals(RRuntime.CLASS_ATTR_KEY)) {
                                if (v == RNull.instance) {
                                    rresult = rresult.setClassAttr(null);
                                } else {
                                    rresult = rresult.setClassAttr((RStringVector) v);
                                }
                            } else {
                                rresult.setAttr(attrName, v);
                            }
                        }
                    }
                }
                return result;
            }
        }
        errorProfile.enter();
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_UNNAMED_VALUE);
    }

    private void initCastTypeNode() {
        if (castTypeNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castTypeNode = insert(CastTypeNodeFactory.create(null, null));
        }
    }

    private void initFactorNode() {
        if (isFactor == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            isFactor = insert(IsFactorNodeFactory.create(null));
        }
    }

    private void initTypeOfNode() {
        if (typeof == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            typeof = insert(TypeofNodeFactory.create(null));
        }
    }

    @SuppressWarnings("unused")
    @Specialization
    protected Object update(Object x, Object value) {
        controlVisibility();
        CompilerDirectives.transferToInterpreter();
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.NULL_VALUE);
    }
}
