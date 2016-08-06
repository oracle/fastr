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

import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.unary.CastIntegerNode;
import com.oracle.truffle.r.nodes.unary.CastIntegerNodeGen;
import com.oracle.truffle.r.nodes.unary.CastListNode;
import com.oracle.truffle.r.nodes.unary.CastToVectorNode;
import com.oracle.truffle.r.nodes.unary.CastToVectorNodeGen;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.RRuntime;
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

    @CompilationFinal private String cachedName = "";
    @CompilationFinal private String cachedInternedName = "";

    private RAbstractContainer updateNames(RAbstractContainer container, Object o) {
        if (updateNames == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            updateNames = insert(UpdateNamesNodeGen.create(null));
        }
        return (RAbstractContainer) updateNames.executeStringVector(container, o);
    }

    private RAbstractContainer updateDimNames(RAbstractContainer container, Object o) {
        if (updateDimNames == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            updateDimNames = insert(UpdateDimNamesNodeGen.create(null));
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

    private String intern(String name) {
        if (cachedName == null) {
            // unoptimized case
            return name.intern();
        }
        if (cachedName == name) {
            // cached case
            return cachedInternedName;
        }
        CompilerDirectives.transferToInterpreterAndInvalidate();
        // Checkstyle: stop StringLiteralEquality
        if (cachedName == "") {
            // Checkstyle: resume StringLiteralEquality
            cachedName = name;
            cachedInternedName = name.intern();
        } else {
            cachedName = null;
            cachedInternedName = null;
        }
        return name.intern();
    }

    @Specialization
    protected RAbstractContainer updateAttr(RAbstractContainer container, String name, RNull value) {
        String internedName = intern(name);
        RAbstractContainer result = (RAbstractContainer) container.getNonShared();
        // the name is interned, so identity comparison is sufficient
        if (internedName == RRuntime.DIM_ATTR_KEY) {
            result.setDimensions(null);
        } else if (internedName == RRuntime.NAMES_ATTR_KEY) {
            return updateNames(result, value);
        } else if (internedName == RRuntime.DIMNAMES_ATTR_KEY) {
            return updateDimNames(result, value);
        } else if (internedName == RRuntime.CLASS_ATTR_KEY) {
            return (RAbstractContainer) result.setClassAttr(null);
        } else if (internedName == RRuntime.ROWNAMES_ATTR_KEY) {
            result.setRowNames(null);
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
        String internedName = intern(name);
        RAbstractContainer result = (RAbstractContainer) container.getNonShared();
        // the name is interned, so identity comparison is sufficient
        if (internedName == RRuntime.DIM_ATTR_KEY) {
            RAbstractIntVector dimsVector = castInteger(castVector(value));
            if (dimsVector.getLength() == 0) {
                errorProfile.enter();
                throw RError.error(this, RError.Message.LENGTH_ZERO_DIM_INVALID);
            }
            result.setDimensions(dimsVector.materialize().getDataCopy());
        } else if (internedName == RRuntime.NAMES_ATTR_KEY) {
            return updateNames(result, value);
        } else if (internedName == RRuntime.DIMNAMES_ATTR_KEY) {
            return updateDimNames(result, value);
        } else if (internedName == RRuntime.CLASS_ATTR_KEY) {
            return (RAbstractContainer) result.setClassAttr(convertClassAttrFromObject(value));
        } else if (internedName == RRuntime.ROWNAMES_ATTR_KEY) {
            result.setRowNames(castVector(value));
        } else {
            // generic attribute
            result.setAttr(internedName, value);
        }

        return result;
    }

    @Specialization(guards = "!nullValue(value)")
    protected RAbstractContainer updateAttr(RAbstractVector vector, RStringVector name, Object value) {
        return updateAttr(vector, name.getDataAt(0), value);
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
        Object object = obj;
        String sname = RRuntime.asString(name);
        if (sname == null) {
            errorProfile.enter();
            throw RError.error(this, RError.Message.MUST_BE_NONNULL_STRING, "name");
        }
        if (object instanceof RShareable) {
            object = ((RShareable) object).getNonShared();
        }
        String internedName = intern(sname);
        if (object instanceof RAttributable) {
            RAttributable attributable = (RAttributable) object;
            if (value == RNull.instance) {
                attributable.removeAttr(attrProfiles, internedName);
            } else {
                attributable.setAttr(internedName, value);
            }
            return object;
        } else {
            errorProfile.enter();
            if (object instanceof RNull) {
                throw RError.error(this, Message.GENERIC, "attempt to set an attribute on NULL");
            } else {
                throw RError.nyi(this, "object cannot be attributed");
            }
        }
    }
}
