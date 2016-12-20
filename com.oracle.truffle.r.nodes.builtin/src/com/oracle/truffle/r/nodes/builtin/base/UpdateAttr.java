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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.runtime.RError.SHOW_CALLER;
import static com.oracle.truffle.r.runtime.RError.Message.MUST_BE_NONNULL_STRING;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.nodes.EmptyTypeSystemFlatLayout;
import com.oracle.truffle.r.nodes.attributes.SetAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.SetClassAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.SetDimAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.SetRowNamesAttributeNode;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.UpdateAttrNodeGen.InternStringNodeGen;
import com.oracle.truffle.r.nodes.unary.CastIntegerNode;
import com.oracle.truffle.r.nodes.unary.CastIntegerNodeGen;
import com.oracle.truffle.r.nodes.unary.CastListNode;
import com.oracle.truffle.r.nodes.unary.CastToVectorNode;
import com.oracle.truffle.r.nodes.unary.CastToVectorNodeGen;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RAttributeProfiles;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RShareable;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

@RBuiltin(name = "attr<-", kind = PRIMITIVE, parameterNames = {"x", "which", "value"}, behavior = PURE)
public abstract class UpdateAttr extends RBuiltinNode {

    private final BranchProfile errorProfile = BranchProfile.create();
    private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

    @Child private UpdateNames updateNames;
    @Child private UpdateDimNames updateDimNames;
    @Child private CastIntegerNode castInteger;
    @Child private CastToVectorNode castVector;
    @Child private CastListNode castList;
    @Child private SetClassAttributeNode setClassAttrNode;
    @Child private SetRowNamesAttributeNode setRowNamesAttrNode;
    @Child private SetAttributeNode setGenAttrNode;
    @Child private SetDimAttributeNode setDimNode;

    @Child private InternStringNode intern = InternStringNodeGen.create();

    @TypeSystemReference(EmptyTypeSystemFlatLayout.class)
    public abstract static class InternStringNode extends Node {

        public abstract String execute(String value);

        @Specialization(limit = "3", guards = "value == cachedValue")
        protected static String internCached(@SuppressWarnings("unused") String value,
                        @SuppressWarnings("unused") @Cached("value") String cachedValue,
                        @Cached("intern(value)") String interned) {
            return interned;
        }

        @Specialization(contains = "internCached")
        protected static String intern(String value) {
            return Utils.intern(value);
        }
    }

    @Override
    protected void createCasts(CastBuilder casts) {
        // Note: cannot check 'attributability' easily because atomic values, e.g int, are not
        // RAttributable.
        casts.arg("x"); // disallows null
        casts.arg("which").defaultError(SHOW_CALLER, MUST_BE_NONNULL_STRING, "name").mustBe(stringValue()).asStringVector().findFirst();
    }

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
    protected RAbstractContainer updateAttr(RAbstractContainer container, String name, RNull value) {
        String internedName = intern.execute(name);
        RAbstractContainer result = (RAbstractContainer) container.getNonShared();
        // the name is interned, so identity comparison is sufficient
        if (internedName == RRuntime.DIM_ATTR_KEY) {
            if (setDimNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                setDimNode = insert(SetDimAttributeNode.create());
            }
            setDimNode.setDimensions(result, null);
        } else if (internedName == RRuntime.NAMES_ATTR_KEY) {
            return updateNames(result, value);
        } else if (internedName == RRuntime.DIMNAMES_ATTR_KEY) {
            return updateDimNames(result, value);
        } else if (internedName == RRuntime.CLASS_ATTR_KEY) {
            if (setClassAttrNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                setClassAttrNode = insert(SetClassAttributeNode.create());
            }
            setClassAttrNode.reset(result);
            return result;
        } else if (internedName == RRuntime.ROWNAMES_ATTR_KEY) {
            if (setRowNamesAttrNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                setRowNamesAttrNode = insert(SetRowNamesAttributeNode.create());
            }
            setRowNamesAttrNode.setRowNames(result, null);
        } else if (result.getAttributes() != null) {
            result.removeAttr(attrProfiles, internedName);
        }
        return result;
    }

    @TruffleBoundary
    protected static RStringVector convertClassAttrFromObject(Object value) {
        if (value instanceof RStringVector) {
            return (RStringVector) value;
        } else if (value instanceof String) {
            return RDataFactory.createStringVector((String) value);
        } else {
            throw RError.error(RError.SHOW_CALLER2, RError.Message.SET_INVALID_CLASS_ATTR);
        }
    }

    @Specialization(guards = "!nullValue(value)")
    protected RAbstractContainer updateAttr(RAbstractContainer container, String name, Object value) {
        String internedName = intern.execute(name);
        RAbstractContainer result = (RAbstractContainer) container.getNonShared();
        // the name is interned, so identity comparison is sufficient
        if (internedName == RRuntime.DIM_ATTR_KEY) {
            RAbstractIntVector dimsVector = castInteger(castVector(value));
            if (dimsVector.getLength() == 0) {
                errorProfile.enter();
                throw RError.error(this, RError.Message.LENGTH_ZERO_DIM_INVALID);
            }
            if (setDimNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                setDimNode = insert(SetDimAttributeNode.create());
            }
            setDimNode.setDimensions(result, dimsVector.materialize().getDataCopy());
        } else if (internedName == RRuntime.NAMES_ATTR_KEY) {
            return updateNames(result, value);
        } else if (internedName == RRuntime.DIMNAMES_ATTR_KEY) {
            return updateDimNames(result, value);
        } else if (internedName == RRuntime.CLASS_ATTR_KEY) {
            if (setClassAttrNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                setClassAttrNode = insert(SetClassAttributeNode.create());
            }
            setClassAttrNode.execute(result, convertClassAttrFromObject(value));
            return result;
        } else if (internedName == RRuntime.ROWNAMES_ATTR_KEY) {
            if (setRowNamesAttrNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                setRowNamesAttrNode = insert(SetRowNamesAttributeNode.create());
            }
            setRowNamesAttrNode.setRowNames(result, castVector(value));
        } else {
            // generic attribute
            if (setGenAttrNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                setGenAttrNode = insert(SetAttributeNode.create());
            }
            setGenAttrNode.execute(result, internedName, value);
        }

        return result;
    }

    // the guard is necessary as RNull and Object cannot be distinguished in case of multiple
    // specializations, such as in: x<-1; attr(x, "dim")<-1; attr(x, "dim")<-NULL
    protected boolean nullValue(Object value) {
        return value == RNull.instance;
    }

    /**
     * All other, non-performance centric, {@link RAttributable} types.
     */
    @Fallback
    protected Object updateAttr(Object obj, Object name, Object value) {
        assert name instanceof String : "casts should not pass anything but String";
        Object object = obj;
        if (object instanceof RShareable) {
            object = ((RShareable) object).getNonShared();
        }
        String internedName = intern.execute((String) name);
        if (object instanceof RAttributable) {
            RAttributable attributable = (RAttributable) object;
            if (value == RNull.instance) {
                attributable.removeAttr(attrProfiles, internedName);
            } else {
                attributable.setAttr(internedName, value);
            }
            return object;
        } else {
            throw RError.nyi(this, "object cannot be attributed");
        }
    }
}
