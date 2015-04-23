/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.env.*;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@RBuiltin(name = "attr<-", kind = PRIMITIVE, parameterNames = {"x", "which", ""})
// 2nd parameter is "value", but should not be matched against, so ""
@SuppressWarnings("unused")
public abstract class UpdateAttr extends RInvisibleBuiltinNode {

    private final BranchProfile errorProfile = BranchProfile.create();
    private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

    @Child private UpdateNames updateNames;
    @Child private UpdateDimNames updateDimNames;
    @Child private CastIntegerNode castInteger;
    @Child private CastToVectorNode castVector;
    @Child private CastListNode castList;

    @CompilationFinal private String cachedName = "";
    @CompilationFinal private String cachedInternedName = "";

    private RAbstractContainer updateNames(VirtualFrame frame, RAbstractContainer container, Object o) {
        if (updateNames == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            updateNames = insert(UpdateNamesNodeGen.create(new RNode[2], null, null));
        }
        return (RAbstractContainer) updateNames.executeStringVector(frame, container, o);
    }

    private RAbstractContainer updateDimNames(VirtualFrame frame, RAbstractContainer container, Object o) {
        if (updateDimNames == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            updateDimNames = insert(UpdateDimNamesNodeGen.create(new RNode[2], null, null));
        }
        return updateDimNames.executeRAbstractContainer(frame, container, o);
    }

    private RAbstractIntVector castInteger(VirtualFrame frame, RAbstractVector vector) {
        if (castInteger == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castInteger = insert(CastIntegerNodeGen.create(null, true, false, false));
        }
        return (RAbstractIntVector) castInteger.executeCast(frame, vector);
    }

    private RAbstractVector castVector(VirtualFrame frame, Object value) {
        if (castVector == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castVector = insert(CastToVectorNodeGen.create(null, false, false, false, false));
        }
        return (RAbstractVector) castVector.executeObject(frame, value);
    }

    private RList castList(VirtualFrame frame, Object value) {
        if (castList == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castList = insert(CastListNodeGen.create(null, true, false, false));
        }
        return castList.executeList(frame, value);
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
    protected RAbstractContainer updateAttr(VirtualFrame frame, RAbstractContainer container, String name, RNull value) {
        controlVisibility();
        String internedName = intern(name);
        RAbstractContainer result = container.materializeNonShared();
        // the name is interned, so identity comparison is sufficient
        if (internedName == RRuntime.DIM_ATTR_KEY) {
            result.setDimensions(null);
        } else if (internedName == RRuntime.NAMES_ATTR_KEY) {
            return updateNames(frame, result, value);
        } else if (internedName == RRuntime.DIMNAMES_ATTR_KEY) {
            return updateDimNames(frame, result, value);
        } else if (internedName == RRuntime.CLASS_ATTR_KEY) {
            return (RAbstractContainer) result.setClassAttr(null, false);
        } else if (internedName == RRuntime.ROWNAMES_ATTR_KEY) {
            result.setRowNames(null);
        } else if (result.getAttributes() != null) {
            result.removeAttr(attrProfiles, internedName);
        }
        return result;
    }

    @TruffleBoundary
    public static RStringVector convertClassAttrFromObject(Object value) {
        if (value instanceof RStringVector) {
            return (RStringVector) value;
        } else if (value instanceof String) {
            return RDataFactory.createStringVector((String) value);
        } else {
            throw RError.error(RError.Message.SET_INVALID_CLASS_ATTR);
        }
    }

    @Specialization(guards = "!nullValue(value)")
    protected RAbstractContainer updateAttr(VirtualFrame frame, RAbstractContainer container, String name, Object value) {
        controlVisibility();
        String internedName = intern(name);
        RAbstractContainer result = container.materializeNonShared();
        // the name is interned, so identity comparison is sufficient
        if (internedName == RRuntime.DIM_ATTR_KEY) {
            RAbstractIntVector dimsVector = castInteger(frame, castVector(frame, value));
            if (dimsVector.getLength() == 0) {
                errorProfile.enter();
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.LENGTH_ZERO_DIM_INVALID);
            }
            result.setDimensions(dimsVector.materialize().getDataCopy());
        } else if (internedName == RRuntime.NAMES_ATTR_KEY) {
            return updateNames(frame, result, value);
        } else if (internedName == RRuntime.DIMNAMES_ATTR_KEY) {
            return updateDimNames(frame, result, value);
        } else if (internedName == RRuntime.CLASS_ATTR_KEY) {
            return (RAbstractContainer) result.setClassAttr(convertClassAttrFromObject(value), false);
        } else if (internedName == RRuntime.ROWNAMES_ATTR_KEY) {
            result.setRowNames(castVector(frame, value));
        } else {
            // generic attribute
            result.setAttr(internedName, value);
        }

        return result;
    }

    @Specialization(guards = "!nullValue(value)")
    protected RAbstractContainer updateAttr(VirtualFrame frame, RAbstractVector vector, RStringVector name, Object value) {
        controlVisibility();
        return updateAttr(frame, vector, name.getDataAt(0), value);
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
    protected Object updateAttr(VirtualFrame frame, Object object, Object name, Object value) {
        controlVisibility();
        String sname = RRuntime.asString(name);
        if (sname == null) {
            errorProfile.enter();
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.MUST_BE_NONNULL_STRING, "name");
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
            throw RError.nyi(getEncapsulatingSourceSection(), "object cannot be attributed");
        }
    }
}
