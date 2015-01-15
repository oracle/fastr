/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

/**
 * Handles all builtin functions of the form {@code is.xxx}, where is {@code xxx} is a "type".
 */
@SuppressWarnings("unused")
public class IsTypeFunctions {

    protected abstract static class ErrorAdapter extends RBuiltinNode {
        protected final BranchProfile errorProfile = BranchProfile.create();

        protected RError missingError() throws RError {
            errorProfile.enter();
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.ARGUMENT_MISSING, "x");
        }
    }

    protected abstract static class IsFalseAdapter extends ErrorAdapter {
        @Override
        public RNode[] getParameterValues() {
            return new RNode[]{ConstantNode.create(RMissing.instance)};
        }

        @Specialization
        protected byte isType(RMissing value) throws RError {
            controlVisibility();
            throw missingError();
        }

        @Fallback
        protected byte isType(Object value) {
            return RRuntime.LOGICAL_FALSE;
        }
    }

    protected abstract static class IsTrueAdapter extends ErrorAdapter {
        @Override
        public RNode[] getParameterValues() {
            return new RNode[]{ConstantNode.create(RMissing.instance)};
        }

        @Specialization
        protected byte isType(RMissing value) throws RError {
            controlVisibility();
            throw missingError();
        }

        @Fallback
        protected byte isType(Object value) {
            return RRuntime.LOGICAL_TRUE;
        }
    }

    @RBuiltin(name = "is.array", kind = PRIMITIVE, parameterNames = {"x"})
    public abstract static class IsArray extends IsFalseAdapter {

        @Specialization
        protected byte isType(RAbstractVector vector) {
            controlVisibility();
            return RRuntime.asLogical(vector.isArray());
        }

    }

    @RBuiltin(name = "is.recursive", kind = PRIMITIVE, parameterNames = {"x"})
    public abstract static class IsRecursive extends IsTrueAdapter {

        @Specialization
        protected byte isRecursive(RNull arg) {
            controlVisibility();
            return RRuntime.LOGICAL_FALSE;
        }

        @Specialization(guards = "!isListVector")
        protected byte isRecursive(RAbstractVector arg) {
            controlVisibility();
            return RRuntime.LOGICAL_FALSE;
        }

        @Specialization
        protected byte isAtomic(RList arg) {
            controlVisibility();
            return RRuntime.LOGICAL_TRUE;
        }

        @Specialization
        protected byte isAtomic(RFactor arg) {
            controlVisibility();
            return RRuntime.LOGICAL_FALSE;
        }

        protected boolean isListVector(RAbstractVector arg) {
            return arg.getElementClass() == Object.class;
        }

    }

    @RBuiltin(name = "is.atomic", kind = PRIMITIVE, parameterNames = {"x"})
    public abstract static class IsAtomic extends IsFalseAdapter {

        @Specialization
        protected byte isAtomic(RNull arg) {
            controlVisibility();
            return RRuntime.LOGICAL_TRUE;
        }

        @Specialization(guards = "!isListVector")
        protected byte isAtomic(RAbstractVector arg) {
            controlVisibility();
            return RRuntime.LOGICAL_TRUE;
        }

        @Specialization
        protected byte isAtomic(RList arg) {
            controlVisibility();
            return RRuntime.LOGICAL_FALSE;
        }

        protected boolean isListVector(RAbstractVector arg) {
            return arg.getElementClass() == Object.class;
        }

        @Specialization
        protected byte isAtomic(RFactor arg) {
            controlVisibility();
            return RRuntime.LOGICAL_TRUE;
        }

    }

    @RBuiltin(name = "is.call", kind = PRIMITIVE, parameterNames = {"x"})
    public abstract static class IsCall extends IsFalseAdapter {

        @Specialization
        protected byte isType(RLanguage lang) {
            return RRuntime.LOGICAL_TRUE;
        }
    }

    @RBuiltin(name = "is.character", kind = PRIMITIVE, parameterNames = {"x"})
    public abstract static class IsCharacter extends IsFalseAdapter {

        @Specialization
        protected byte isType(String value) {
            controlVisibility();
            return RRuntime.LOGICAL_TRUE;
        }

        @Specialization
        protected byte isType(RStringVector value) {
            controlVisibility();
            return RRuntime.LOGICAL_TRUE;
        }
    }

    @RBuiltin(name = "is.complex", kind = PRIMITIVE, parameterNames = {"x"})
    public abstract static class IsComplex extends IsFalseAdapter {

        @Specialization
        protected byte isType(RComplex value) {
            controlVisibility();
            return RRuntime.LOGICAL_TRUE;
        }

        @Specialization
        protected byte isType(RComplexVector value) {
            controlVisibility();
            return RRuntime.LOGICAL_TRUE;
        }
    }

    @RBuiltin(name = "is.data.frame", kind = SUBSTITUTE, parameterNames = {"x"})
    // TODO revert to R
    public abstract static class IsDataFrame extends IsFalseAdapter {

        @Specialization
        protected byte isType(RDataFrame operand) {
            return RRuntime.LOGICAL_TRUE;
        }

    }

    @RBuiltin(name = "is.double", kind = PRIMITIVE, parameterNames = {"x"})
    public abstract static class IsDouble extends IsFalseAdapter {

        @Specialization
        protected byte isType(double value) {
            controlVisibility();
            return RRuntime.LOGICAL_TRUE;
        }

        @Specialization
        protected byte isType(RAbstractDoubleVector value) {
            controlVisibility();
            return RRuntime.LOGICAL_TRUE;
        }

    }

    @RBuiltin(name = "is.expression", kind = PRIMITIVE, parameterNames = {"x"})
    public abstract static class IsExpression extends IsFalseAdapter {

        @Specialization
        protected byte isType(RExpression expr) {
            return RRuntime.LOGICAL_TRUE;
        }
    }

    @RBuiltin(name = "is.function", kind = PRIMITIVE, parameterNames = {"x"})
    public abstract static class IsFunction extends IsFalseAdapter {

        @Specialization
        protected byte isType(RFunction value) {
            return RRuntime.LOGICAL_TRUE;
        }
    }

    @RBuiltin(name = "is.integer", kind = PRIMITIVE, parameterNames = {"x"})
    public abstract static class IsInteger extends IsFalseAdapter {

        @Specialization
        protected byte isType(int value) {
            controlVisibility();
            return RRuntime.LOGICAL_TRUE;
        }

        @Specialization
        protected byte isType(RAbstractIntVector value) {
            controlVisibility();
            return RRuntime.LOGICAL_TRUE;
        }

    }

    @RBuiltin(name = "is.language", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"x"})
    public abstract static class IsLanguage extends IsFalseAdapter {
        @Specialization
        protected byte isType(RSymbol value) {
            return RRuntime.LOGICAL_TRUE;
        }

        @Specialization
        protected byte isType(RExpression value) {
            return RRuntime.LOGICAL_TRUE;
        }

        @Specialization
        protected byte isType(RLanguage value) {
            return RRuntime.LOGICAL_TRUE;
        }
    }

    @RBuiltin(name = "is.list", kind = PRIMITIVE, parameterNames = {"x"})
    public abstract static class IsList extends IsFalseAdapter {

        private final ConditionProfile dataFrameIsListProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile isAbsVectorListProfile = ConditionProfile.createBinaryProfile();

        @Specialization
        protected byte isType(RList value) {
            controlVisibility();
            return RRuntime.LOGICAL_TRUE;
        }

        @Specialization
        protected byte isType(RDataFrame value) {
            controlVisibility();
            if (dataFrameIsListProfile.profile(isList(value))) {
                return RRuntime.LOGICAL_TRUE;
            } else {
                return RRuntime.LOGICAL_FALSE;
            }
        }

        @Specialization
        protected byte isType(RPairList pl) {
            controlVisibility();
            return RRuntime.LOGICAL_TRUE;
        }

        protected boolean isList(RAbstractVector vector) {
            return vector.getElementClass() == Object.class;
        }

        protected boolean isList(RDataFrame frame) {
            return isList(frame.getVector());
        }

        /**
         * All the subclasses of {@link RAbstractVector} are lists iff the class of the vector
         * element is {@code Object}.
         */
        @Specialization
        protected byte isType(RAbstractVector value) {
            controlVisibility();
            if (isAbsVectorListProfile.profile(isList(value))) {
                return RRuntime.LOGICAL_TRUE;
            } else {
                return RRuntime.LOGICAL_FALSE;
            }
        }

    }

    @RBuiltin(name = "is.logical", kind = PRIMITIVE, parameterNames = {"x"})
    public abstract static class IsLogical extends IsFalseAdapter {

        @Specialization
        protected byte isType(byte value) {
            controlVisibility();
            return RRuntime.LOGICAL_TRUE;
        }

        @Specialization
        protected byte isType(RLogicalVector value) {
            controlVisibility();
            return RRuntime.LOGICAL_TRUE;
        }
    }

    @RBuiltin(name = "is.matrix", kind = PRIMITIVE, parameterNames = {"x"})
    public abstract static class IsMatrix extends IsFalseAdapter {

        @Specialization
        protected byte isType(RAbstractVector vector) {
            controlVisibility();
            return RRuntime.asLogical(vector.isMatrix());
        }
    }

    @RBuiltin(name = "is.name", aliases = {"is.symbol"}, kind = PRIMITIVE, parameterNames = {"x"})
    public abstract static class IsName extends IsFalseAdapter {

        @Specialization
        protected byte isType(RSymbol value) {
            controlVisibility();
            return RRuntime.LOGICAL_TRUE;
        }
    }

    @RBuiltin(name = "is.numeric", kind = PRIMITIVE, parameterNames = {"x"})
    public abstract static class IsNumeric extends IsFalseAdapter {

        public abstract byte execute(VirtualFrame frame, Object value);

        @Specialization
        protected byte isType(int value) {
            controlVisibility();
            return RRuntime.LOGICAL_TRUE;
        }

        @Specialization
        protected byte isType(RIntVector value) {
            controlVisibility();
            return RRuntime.LOGICAL_TRUE;
        }

        @Specialization
        protected byte isType(double value) {
            controlVisibility();
            return RRuntime.LOGICAL_TRUE;
        }

        @Specialization
        protected byte isType(RDoubleVector value) {
            controlVisibility();
            return RRuntime.LOGICAL_TRUE;
        }

        @Specialization
        protected byte isType(RIntSequence value) {
            controlVisibility();
            return RRuntime.LOGICAL_TRUE;
        }

        @Specialization
        protected byte isType(RDoubleSequence value) {
            controlVisibility();
            return RRuntime.LOGICAL_TRUE;
        }
    }

    @RBuiltin(name = "is.null", kind = PRIMITIVE, parameterNames = {"x"})
    public abstract static class IsNull extends IsFalseAdapter {

        @Specialization
        protected byte isType(RNull value) {
            controlVisibility();
            return RRuntime.LOGICAL_TRUE;
        }
    }

    /**
     * The specification is not quite what you might expect. Several builtin types, e.g.,
     * {@code expression} respond to {@code class(e)} but return {@code FALSE} to {@code is.object}.
     * Essentially, this method should only return {@code TRUE} if a {@code class} attribute has
     * been added explicitly to the object. If the attribute is removed, it should return
     * {@code FALSE}.
     */
    @RBuiltin(name = "is.object", kind = PRIMITIVE, parameterNames = {"x"})
    public abstract static class IsObject extends IsFalseAdapter {

        @Specialization
        protected byte isObject(RAbstractContainer arg) {
            controlVisibility();
            return arg.isObject() ? RRuntime.LOGICAL_TRUE : RRuntime.LOGICAL_FALSE;
        }

        @Specialization
        protected byte isObject(RConnection conn) {
            // No need to enquire, connections always have a class attribute.
            return RRuntime.LOGICAL_TRUE;
        }
    }

    @RBuiltin(name = "is.pairlist", kind = PRIMITIVE, parameterNames = {"x"})
    public abstract static class IsPairList extends IsFalseAdapter {
        @Specialization
        protected byte isType(RNull value) {
            controlVisibility();
            return RRuntime.LOGICAL_TRUE;
        }

        @Specialization
        protected byte isType(RPairList value) {
            controlVisibility();
            return RRuntime.LOGICAL_TRUE;
        }
    }

    @RBuiltin(name = "is.raw", kind = PRIMITIVE, parameterNames = {"x"})
    public abstract static class IsRaw extends IsFalseAdapter {

        @Specialization
        protected byte isType(RRaw value) {
            controlVisibility();
            return RRuntime.LOGICAL_TRUE;
        }

        @Specialization
        protected byte isType(RRawVector value) {
            controlVisibility();
            return RRuntime.LOGICAL_TRUE;
        }
    }

    @RBuiltin(name = "is.vector", kind = INTERNAL, parameterNames = {"x", "mode"})
    public abstract static class IsVector extends ErrorAdapter {

        @Override
        public RNode[] getParameterValues() {
            // x, mode = "any"
            return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RType.Any.getName())};
        }

        @Specialization
        protected byte isVector(RMissing value, String mode) {
            controlVisibility();
            throw missingError();
        }

        @Specialization
        protected byte isVector(RAbstractVector x, String mode) {
            controlVisibility();
            if (!namesOnlyOrNoAttr(x) || !modeIsAnyOrMatches(x, mode)) {
                return RRuntime.LOGICAL_FALSE;
            } else {
                return RRuntime.LOGICAL_TRUE;
            }
        }

        @Fallback
        protected byte isVector(Object x, Object mode) {
            return RRuntime.LOGICAL_FALSE;
        }

        protected static boolean namesOnlyOrNoAttr(RAbstractVector x) {
            // there should be no attributes other than names
            if (x.getNames() == RNull.instance) {
                assert x.getAttributes() == null || x.getAttributes().size() > 0;
                return x.getAttributes() == null ? true : false;
            } else {
                assert x.getAttributes() != null;
                return x.getAttributes().size() == 1 ? true : false;
            }
        }

        protected boolean modeIsAnyOrMatches(RAbstractVector x, String mode) {
            return RType.Any.getName().equals(mode) || (x.getElementClass() == Object.class && mode.equals("list")) || (x.getElementClass() == RDouble.class && RType.Double.getName().equals(mode)) ||
                            RRuntime.classToString(x.getElementClass()).equals(mode);
        }
    }

}
