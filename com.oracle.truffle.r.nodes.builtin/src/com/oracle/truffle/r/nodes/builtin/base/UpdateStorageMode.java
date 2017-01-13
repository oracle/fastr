/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.nodes.attributes.ArrayAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SetAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.SetClassAttributeNode;
import com.oracle.truffle.r.nodes.attributes.TypeFromModeNode;
import com.oracle.truffle.r.nodes.attributes.TypeFromModeNodeGen;
import com.oracle.truffle.r.nodes.binary.CastTypeNode;
import com.oracle.truffle.r.nodes.binary.CastTypeNodeGen;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.unary.IsFactorNode;
import com.oracle.truffle.r.nodes.unary.TypeofNode;
import com.oracle.truffle.r.nodes.unary.TypeofNodeGen;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RAttributesLayout;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;

@RBuiltin(name = "storage.mode<-", kind = PRIMITIVE, parameterNames = {"x", "value"}, behavior = PURE)
public abstract class UpdateStorageMode extends RBuiltinNode {

    @Child private TypeFromModeNode typeFromMode = TypeFromModeNodeGen.create();
    @Child private TypeofNode typeof;
    @Child private CastTypeNode castTypeNode;
    @Child private IsFactorNode isFactor;
    @Child private SetClassAttributeNode setClassAttrNode;

    private final BranchProfile errorProfile = BranchProfile.create();

    @Specialization
    protected Object update(Object x, String value,
                    @Cached("create()") ArrayAttributeNode attrAttrAccess,
                    @Cached("create()") SetAttributeNode setAttrNode) {
        RType mode = typeFromMode.execute(value);
        if (mode == RType.DefunctReal || mode == RType.DefunctSingle) {
            errorProfile.enter();
            throw RError.error(this, RError.Message.USE_DEFUNCT, mode.getName(), mode == RType.DefunctSingle ? "mode<-" : "double");
        }
        initTypeOfNode();
        RType typeX = typeof.execute(x);
        if (typeX == mode) {
            return x;
        }
        initFactorNode();
        if (isFactor.executeIsFactor(x)) {
            errorProfile.enter();
            throw RError.error(this, RError.Message.INVALID_STORAGE_MODE_UPDATE);
        }
        initCastTypeNode();
        if (mode != null) {
            Object result = castTypeNode.execute(x, mode);
            if (result != null) {
                if (x instanceof RAttributable && result instanceof RAbstractContainer) {
                    RAttributable rx = (RAttributable) x;
                    DynamicObject attrs = rx.getAttributes();
                    if (attrs != null) {
                        RAbstractContainer rresult = (RAbstractContainer) result;
                        for (RAttributesLayout.RAttribute attr : attrAttrAccess.execute(attrs)) {
                            String attrName = attr.getName();
                            Object v = attr.getValue();
                            if (attrName.equals(RRuntime.CLASS_ATTR_KEY)) {

                                if (setClassAttrNode == null) {
                                    CompilerDirectives.transferToInterpreterAndInvalidate();
                                    setClassAttrNode = insert(SetClassAttributeNode.create());
                                }

                                if (v == RNull.instance) {
                                    setClassAttrNode.reset(rresult);
                                } else {
                                    setClassAttrNode.execute(rresult, v);
                                }
                            } else {
                                setAttrNode.execute(rresult, Utils.intern(attrName), v);
                            }
                        }
                        return rresult;
                    }
                }
                return result;
            }
        }
        errorProfile.enter();
        throw RError.error(this, RError.Message.INVALID_UNNAMED_VALUE);
    }

    private void initCastTypeNode() {
        if (castTypeNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castTypeNode = insert(CastTypeNodeGen.create(null, null));
        }
    }

    private void initFactorNode() {
        if (isFactor == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            isFactor = insert(new IsFactorNode());
        }
    }

    private void initTypeOfNode() {
        if (typeof == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            typeof = insert(TypeofNodeGen.create());
        }
    }

    @SuppressWarnings("unused")
    @Specialization
    protected Object update(Object x, Object value) {
        CompilerDirectives.transferToInterpreter();
        throw RError.error(this, RError.Message.MUST_BE_NONNULL_STRING, "value");
    }
}
