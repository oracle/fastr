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

import static com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
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
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.env.*;

@RBuiltin(name = "class<-", kind = PRIMITIVE, parameterNames = {"x", ""})
// 2nd parameter is "value", but should not be matched against, so ""
public abstract class UpdateClass extends RBuiltinNode {

    @Child private CastTypeNode castTypeNode;
    @Child private CastStringNode castStringNode;
    @Child private TypeofNode typeof;

    private final ValueProfile modeProfile = ValueProfile.createIdentityProfile();

    public abstract Object execute(VirtualFrame frame, RAbstractContainer vector, Object o);

    @Specialization(guards = "!isStringVector")
    protected Object setClass(VirtualFrame frame, RAbstractContainer arg, RAbstractVector className) {
        controlVisibility();
        if (className.getLength() == 0) {
            return setClass(arg, RNull.instance);
        }
        initCastStringNode();
        Object result = castStringNode.executeCast(frame, className);
        return setClass(arg, (RStringVector) result);
    }

    private void initCastStringNode() {
        if (castStringNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castStringNode = insert(CastStringNodeGen.create(null, false, false, false, false));
        }
    }

    @Specialization
    @TruffleBoundary
    protected Object setClass(RAbstractContainer arg, @SuppressWarnings("unused") RNull className) {
        controlVisibility();
        RVector resultVector = arg.materializeNonSharedVector();
        return RVector.setVectorClassAttr(resultVector, null, arg.getElementClass() == RDataFrame.class ? arg : null, arg.getElementClass() == RFactor.class ? arg : null);
    }

    @Specialization
    protected Object setClass(VirtualFrame frame, RAbstractContainer arg, String className) {
        controlVisibility();
        initTypeof();
        if (!arg.isObject()) {
            RType argType = this.typeof.execute(frame, arg);
            if (argType.equals(className) || (RType.Numeric.getName().equals(className) && (argType == RType.Integer || argType == RType.Double))) {
                // "explicit" attribute might have been set (e.g. by oldClass<-)
                return setClass(arg, RNull.instance);
            }
        }
        initCastTypeNode();
        RType mode = RType.fromString(modeProfile.profile(className));
        if (mode != null) {
            Object result = castTypeNode.execute(frame, arg, mode);
            if (result != null) {
                return setClass((RAbstractVector) result, RNull.instance);
            }
        }
        RVector resultVector = arg.materializeNonSharedVector();
        if (RType.Matrix.getName().equals(className)) {
            if (resultVector.isMatrix()) {
                return setClass(resultVector, RNull.instance);
            }
            final int[] dimensions = resultVector.getDimensions();
            int dimLength = 0;
            if (dimensions != null) {
                dimLength = dimensions.length;
            }
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.NOT_A_MATRIX_UPDATE_CLASS, dimLength);
        }
        if (RType.Array.getName().equals(className)) {
            if (resultVector.isArray()) {
                return setClass(resultVector, RNull.instance);
            }
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.NOT_ARRAY_UPDATE_CLASS);
        }

        return RVector.setVectorClassAttr(resultVector, RDataFactory.createStringVector(className), arg.getElementClass() == RDataFrame.class ? arg : null,
                        arg.getElementClass() == RFactor.class ? arg : null);
    }

    @Specialization
    @TruffleBoundary
    protected Object setClass(RAbstractContainer arg, RStringVector className) {
        controlVisibility();
        RVector resultVector = arg.materializeNonSharedVector();
        return RVector.setVectorClassAttr(resultVector, className, arg.getElementClass() == RDataFrame.class ? arg : null, arg.getElementClass() == RFactor.class ? arg : null);
    }

    @Specialization
    public Object setClass(RFunction arg, RAbstractStringVector className) {
        controlVisibility();
        arg.setClassAttr(className.materialize());
        return arg;
    }

    @Specialization
    public Object setClass(RFunction arg, @SuppressWarnings("unused") RNull className) {
        controlVisibility();
        arg.setClassAttr(null);
        return arg;
    }

    @Specialization
    public Object setClass(REnvironment arg, RAbstractStringVector className) {
        controlVisibility();
        arg.setClassAttr(className.materialize());
        return arg;
    }

    @Specialization
    public Object setClass(REnvironment arg, @SuppressWarnings("unused") RNull className) {
        controlVisibility();
        arg.setClassAttr(null);
        return arg;
    }

    @Specialization
    public Object setClass(RSymbol arg, RAbstractStringVector className) {
        controlVisibility();
        arg.setClassAttr(className.materialize());
        return arg;
    }

    @Specialization
    public Object setClass(RSymbol arg, @SuppressWarnings("unused") RNull className) {
        controlVisibility();
        arg.setClassAttr(null);
        return arg;
    }

    @Specialization
    public Object setClass(RExternalPtr arg, RAbstractStringVector className) {
        controlVisibility();
        arg.setClassAttr(className.materialize());
        return arg;
    }

    @Specialization
    public Object setClass(RExternalPtr arg, @SuppressWarnings("unused") RNull className) {
        controlVisibility();
        arg.setClassAttr(null);
        return arg;
    }

    private void initCastTypeNode() {
        if (castTypeNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castTypeNode = insert(CastTypeNodeGen.create(null, null));
        }
    }

    private void initTypeof() {
        if (typeof == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            typeof = insert(TypeofNodeGen.create(null));
        }
    }

    protected boolean isStringVector(@SuppressWarnings("unused") RAbstractContainer arg, RAbstractVector className) {
        return className.getElementClass() == RString.class;
    }
}
