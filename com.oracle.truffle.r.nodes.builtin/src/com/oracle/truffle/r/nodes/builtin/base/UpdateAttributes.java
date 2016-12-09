/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.instanceOf;
import static com.oracle.truffle.r.runtime.RError.Message.ATTRIBUTES_LIST_OR_NULL;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.attributes.SetAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetNamesAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.SetDimAttributeNode;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.unary.CastIntegerNode;
import com.oracle.truffle.r.nodes.unary.CastIntegerNodeGen;
import com.oracle.truffle.r.nodes.unary.CastToVectorNode;
import com.oracle.truffle.r.nodes.unary.CastToVectorNodeGen;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RAttributeProfiles;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RShareable;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RTypesGen;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

@RBuiltin(name = "attributes<-", kind = PRIMITIVE, parameterNames = {"obj", "value"}, behavior = PURE)
public abstract class UpdateAttributes extends RBuiltinNode {
    private final ConditionProfile numAttributesProfile = ConditionProfile.createBinaryProfile();
    private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();
    @Child private GetNamesAttributeNode getNamesNode = GetNamesAttributeNode.create();

    @Child private UpdateNames updateNames;
    @Child private UpdateDimNames updateDimNames;
    @Child private CastIntegerNode castInteger;
    @Child private CastToVectorNode castVector;
    @Child private SetAttributeNode setAttrNode;
    @Child private SetDimAttributeNode setDimNode;

    @Override
    protected void createCasts(CastBuilder casts) {
        // Note: cannot check 'attributability' easily because atomic values, e.g int, are not
        // RAttributable.
        casts.arg("obj"); // by default disallows RNull
        casts.arg("value").conf(c -> c.allowNull()).mustBe(instanceOf(RList.class), this, ATTRIBUTES_LIST_OR_NULL);
    }

    // it's OK for the following two methods to update attributes in-place as the container has been
    // already materialized to non-shared

    private RAbstractContainer updateNames(RAbstractContainer container, Object o) {
        if (updateNames == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            updateNames = insert(UpdateNamesNodeGen.create());
        }
        return (RAbstractContainer) updateNames.executeStringVector(container, o);
    }

    private RAbstractContainer updateDimNames(RAbstractContainer container, Object o) {
        if (updateDimNames == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            updateDimNames = insert(UpdateDimNamesNodeGen.create());
        }
        return updateDimNames.executeRAbstractContainer(container, o);
    }

    private RAbstractIntVector castInteger(RAbstractVector vector) {
        if (castInteger == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castInteger = insert(CastIntegerNodeGen.create(true, false, false));
        }
        return (RAbstractIntVector) castInteger.execute(vector);
    }

    private RAbstractVector castVector(Object value) {
        if (castVector == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castVector = insert(CastToVectorNodeGen.create(false));
        }
        return (RAbstractVector) castVector.execute(value);
    }

    @Specialization
    protected RAbstractContainer updateAttributes(RAbstractContainer abstractContainer, @SuppressWarnings("unused") RNull list) {
        RAbstractContainer resultVector = (RAbstractContainer) abstractContainer.getNonShared();
        resultVector.resetAllAttributes(true);
        return resultVector;
    }

    @Specialization
    protected RAbstractContainer updateAttributes(RAbstractContainer container, RList list) {
        Object listNamesObject = getNamesNode.getNames(list);
        if (listNamesObject == null || listNamesObject == RNull.instance) {
            throw RError.error(this, RError.Message.ATTRIBUTES_NAMED);
        }
        RStringVector listNames = (RStringVector) listNamesObject;
        RAbstractContainer result = (RAbstractContainer) container.getNonShared();
        if (numAttributesProfile.profile(list.getLength() == 0)) {
            result.resetAllAttributes(true);
        } else {
            result.resetAllAttributes(false);
            // error checking is a little weird - seems easier to separate it than weave it into the
            // update loop
            if (listNames.getLength() > 1) {
                checkAttributeForEmptyValue(list);
            }
            // has to be reported if no other name is undefined
            if (listNames.getDataAt(0).equals(RRuntime.NAMES_ATTR_EMPTY_VALUE)) {
                throw RError.error(this, RError.Message.ZERO_LENGTH_VARIABLE);
            }
            // set the dim attribute first
            setDimAttribute(result, list);
            // set the remaining attributes in order
            result = setRemainingAttributes(result, list);
        }
        return result;
    }

    @TruffleBoundary
    private void checkAttributeForEmptyValue(RList rlist) {
        RStringVector listNames = rlist.getNames(attrProfiles);
        int length = rlist.getLength();
        assert length > 0 : "Length should be > 0 for ExplodeLoop";
        for (int i = 1; i < length; i++) {
            String attrName = listNames.getDataAt(i);
            if (attrName.equals(RRuntime.NAMES_ATTR_EMPTY_VALUE)) {
                throw RError.error(this, RError.Message.ALL_ATTRIBUTES_NAMES, i + 1);
            }
        }
    }

    private void setDimAttribute(RAbstractContainer result, RList sourceList) {
        RStringVector listNames = getNamesNode.getNames(sourceList);
        int length = sourceList.getLength();
        assert length > 0 : "Length should be > 0 for ExplodeLoop";
        for (int i = 0; i < sourceList.getLength(); i++) {
            Object value = sourceList.getDataAt(i);
            String attrName = listNames.getDataAt(i);
            if (attrName.equals(RRuntime.DIM_ATTR_KEY)) {

                if (setDimNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    setDimNode = insert(SetDimAttributeNode.create());
                }

                if (value == RNull.instance) {
                    setDimNode.setDimensions(result, null);
                } else {
                    RAbstractIntVector dimsVector = castInteger(castVector(value));
                    if (dimsVector.getLength() == 0) {
                        throw RError.error(this, RError.Message.LENGTH_ZERO_DIM_INVALID);
                    }
                    setDimNode.setDimensions(result, dimsVector.materialize().getDataCopy());
                }
            }
        }
    }

    private RAbstractContainer setRemainingAttributes(RAbstractContainer result, RList sourceList) {
        RStringVector listNames = getNamesNode.getNames(sourceList);
        int length = sourceList.getLength();
        assert length > 0 : "Length should be > 0 for ExplodeLoop";
        RAbstractContainer res = result;
        for (int i = 0; i < sourceList.getLength(); i++) {
            Object value = sourceList.getDataAt(i);
            String attrName = listNames.getDataAt(i);
            if (attrName.equals(RRuntime.DIM_ATTR_KEY)) {
                continue;
            } else if (attrName.equals(RRuntime.NAMES_ATTR_KEY)) {
                res = updateNames(res, value);
            } else if (attrName.equals(RRuntime.DIMNAMES_ATTR_KEY)) {
                res = updateDimNames(res, value);
            } else if (attrName.equals(RRuntime.CLASS_ATTR_KEY)) {
                if (value == RNull.instance) {
                    res.setClassAttr(null);
                } else {
                    res.setClassAttr(UpdateAttr.convertClassAttrFromObject(value));
                }
                res = result;
            } else if (attrName.equals(RRuntime.ROWNAMES_ATTR_KEY)) {
                res.setRowNames(castVector(value));
            } else {
                if (value == RNull.instance) {
                    res.removeAttr(attrProfiles, attrName);
                } else {
                    if (setAttrNode == null) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        setAttrNode = insert(SetAttributeNode.create());
                    }
                    setAttrNode.execute(res, attrName.intern(), value);
                }
            }
        }
        return res;
    }

    protected static boolean isAbstractContainer(Object value) {
        return RTypesGen.isImplicitRAbstractContainer(value);
    }

    /**
     * All other, non-performance centric, {@link RAttributable} types, or error case for RNull
     * value.
     */
    @Specialization(guards = {"!isAbstractContainer(o)"})
    @TruffleBoundary
    protected Object doOtherNull(Object o, @SuppressWarnings("unused") RNull operand) {
        checkAttributable(o);
        Object obj = getNonShared(o);
        RAttributable attrObj = (RAttributable) obj;
        attrObj.removeAllAttributes();
        attrObj.setClassAttr(null);
        return obj;
    }

    /**
     * All other, non-performance centric, {@link RAttributable} types, or error case for list
     * value.
     */
    @Specialization(guards = {"!isAbstractContainer(o)"})
    @TruffleBoundary
    protected Object doOtherList(Object o, RList operand) {
        checkAttributable(o);
        Object obj = getNonShared(o);
        RAttributable attrObj = (RAttributable) obj;
        attrObj.removeAllAttributes();
        RStringVector listNames = operand.getNames(attrProfiles);
        if (listNames == null) {
            throw RError.error(this, RError.Message.ATTRIBUTES_NAMED);
        }
        for (int i = 0; i < operand.getLength(); i++) {
            String attrName = listNames.getDataAt(i);
            if (attrName == null) {
                throw RError.error(this, RError.Message.ATTRIBUTES_NAMED);
            }
            if (RRuntime.CLASS_ATTR_KEY.equals(attrName)) {
                Object attrValue = operand.getDataAt(i);
                if (attrValue == null) {
                    throw RError.error(this, RError.Message.SET_INVALID_CLASS_ATTR);
                }

                attrObj.setClassAttr(UpdateAttr.convertClassAttrFromObject(attrValue));
            } else {
                attrObj.setAttr(attrName.intern(), operand.getDataAt(i));
            }
        }
        return obj;
    }

    private void checkAttributable(Object obj) {
        if (!(obj instanceof RAttributable)) {
            throw RError.error(this, RError.Message.INVALID_OR_UNIMPLEMENTED_ARGUMENTS);
        }
    }

    private static Object getNonShared(Object obj) {
        if (obj instanceof RShareable) {
            return ((RShareable) obj).getNonShared();
        }
        return obj;
    }
}
