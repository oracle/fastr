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
package com.oracle.truffle.r.nodes.unary;

import java.util.function.Function;
import java.util.function.Predicate;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.binary.BoxPrimitiveNode;
import com.oracle.truffle.r.nodes.binary.BoxPrimitiveNodeGen;
import com.oracle.truffle.r.nodes.builtin.CastBuilder.CastFunction;
import com.oracle.truffle.r.nodes.builtin.CastBuilder.CastFunction0;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

public class CastFunctions {

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static abstract class ArgumentConditionNode extends CastNode {

        private final CastFunction condition;
        private final CastFunction success;
        private final CastFunction failure;
        private final boolean boxPrimitives;

        @Child private BoxPrimitiveNode boxPrimitiveNode = BoxPrimitiveNodeGen.create();

        protected ArgumentConditionNode(CastFunction<?, Boolean> condition, CastFunction<?, Void> success, CastFunction<?, Void> failure, boolean boxPrimitives) {
            this.condition = condition;
            this.success = success;
            this.failure = failure;
            this.boxPrimitives = boxPrimitives;
        }

        @Specialization
        protected Object evalCondition(Object x) {
            Object y = boxPrimitives ? boxPrimitiveNode.execute(x) : x;
            if ((Boolean) condition.apply(this, y)) {
                success.apply(this, y);
            } else {
                failure.apply(this, y);
            }
            return x;
        }

    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static abstract class MapNode extends CastNode {

        private final Function mapFn;

        protected MapNode(Function<?, ?> mapFn) {
            this.mapFn = mapFn;
        }

        @Specialization
        protected Object mapNull(RNull x) {
            Object res = mapFn.apply(null);
            return res == null ? x : res;
        }

        @Specialization
        protected Object map(Object x) {
            return mapFn.apply(x);
        }

    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static abstract class ConditionalMapNode extends CastNode {

        private final Predicate predicate;
        private final Function mapFn;

        protected ConditionalMapNode(Predicate<?> predicate, Function<?, ?> mapFn) {
            this.predicate = predicate;
            this.mapFn = mapFn;
        }

        protected boolean doMap(Object x) {
            return predicate.test(x);
        }

        @Specialization(guards = "doMap(x)")
        protected Object map(Object x) {
            return mapFn.apply(x);
        }

        @Specialization
        protected Object mapNull(RNull x) {
            Object res = mapFn.apply(null);
            return res == null ? x : res;
        }

        @Specialization(guards = "!doMap(x)")
        protected Object noMap(Object x) {
            return x;
        }

    }

    public static abstract class FindFirstNode extends CastNode {

        @Child private BoxPrimitiveNode boxPrimitive = BoxPrimitiveNodeGen.create();

        protected FindFirstNode() {
        }

        @Specialization
        protected Object onVector(RAbstractVector x) {
            return x.getLength() > 0 ? x.getDataAtAsObject(0) : RNull.instance;
        }

        @Specialization
        protected Object onNull(RNull x) {
            return x;
        }

        @Fallback
        protected Object onNonVector(Object x) {
            Object y = boxPrimitive.execute(x);
            if (y instanceof RAbstractVector) {
                RAbstractVector v = (RAbstractVector) y;
                return v.getLength() > 0 ? v.getDataAtAsObject(0) : RNull.instance;
            } else {
                // todo: Should not an exception be thrown
                return RNull.instance;
            }
        }
    }

    public static abstract class NonNANode extends CastNode {

        protected NonNANode() {
        }

        @Specialization
        protected Object onLogical(byte x) {
            return RRuntime.isNA(x) ? RNull.instance : x;
        }

        @Specialization
        protected Object onBoolean(boolean x) {
            return x;
        }

        @Specialization
        protected Object onInteger(int x) {
            return RRuntime.isNA(x) ? RNull.instance : x;
        }

        @Specialization
        protected Object onDouble(double x) {
            return RRuntime.isNAorNaN(x) ? RNull.instance : x;
        }

        @Specialization
        protected Object onComplex(RComplex x) {
            return RRuntime.isNA(x) ? RNull.instance : x;
        }

        @Specialization
        protected Object onString(String x) {
            return RRuntime.isNA(x) ? RNull.instance : x;
        }

        @Specialization
        protected Object onNull(RNull x) {
            return x;
        }

    }

    public static abstract class FindFirstBooleanNode extends CastNode {

        protected FindFirstBooleanNode() {
        }

        @Specialization
        protected Object onLogical(byte x) {
            if (RRuntime.isNA(x)) {
                return RNull.instance;
            } else {
                return RRuntime.fromLogical(x);
            }
        }

        @Specialization
        protected Object onVector(RAbstractLogicalVector x) {
            if (x.getLength() > 0) {
                byte head = x.getDataAt(0);
                if (RRuntime.isNA(head)) {
                    return RNull.instance;
                } else {
                    return RRuntime.fromLogical(head);
                }
            } else {
                return RNull.instance;
            }
        }

        @Specialization
        protected Object onNull(RNull x) {
            return x;
        }

        @Fallback
        protected Object onNonLogical(@SuppressWarnings("unused") Object x) {
            return RNull.instance;
        }

    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static abstract class OptionalElementNode extends CastNode {

        private final CastFunction present;
        private final CastFunction0 missing;

        protected OptionalElementNode(CastFunction<?, ?> present, CastFunction0<?> missing) {
            this.present = present;
            this.missing = missing;
        }

        @Specialization
        protected Object missingElement(@SuppressWarnings("unused") RNull x) {
            return missing.apply(this);
        }

        @Fallback
        protected Object presentElement(Object x) {
            return present.apply(this, x);
        }

    }

}
