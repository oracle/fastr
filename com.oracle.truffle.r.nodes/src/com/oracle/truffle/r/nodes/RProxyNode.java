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
package com.oracle.truffle.r.nodes;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.runtime.conn.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.env.*;

@NodeChild("operand")
public abstract class RProxyNode extends RNode {

    protected RVector proxyVector(RVector vector) {
        return vector;
    }

    protected RSequence proxySequence(RSequence sequence) {
        return sequence;
    }

    protected RDataFrame proxyDataFrame(RDataFrame dataFrame) {
        return dataFrame;
    }

    protected RFactor proxyFactor(RFactor factor) {
        return factor;
    }

    @Specialization
    protected RNull wrap(RNull x) {
        return proxy(x);
    }

    protected RNull proxy(RNull x) {
        return x;
    }

    @Specialization
    protected byte wrap(byte x) {
        return proxy(x);
    }

    protected byte proxy(byte x) {
        return x;
    }

    @Specialization
    protected int wrap(int x) {
        return proxy(x);
    }

    protected int proxy(int x) {
        return x;
    }

    @Specialization
    protected double wrap(double x) {
        return proxy(x);
    }

    protected double proxy(double x) {
        return x;
    }

    @Specialization
    protected RRaw wrap(RRaw x) {
        return proxy(x);
    }

    protected RRaw proxy(RRaw x) {
        return x;
    }

    @Specialization
    protected RComplex wrap(RComplex x) {
        return proxy(x);
    }

    protected RComplex proxy(RComplex x) {
        return x;
    }

    @Specialization
    protected String wrap(String x) {
        return proxy(x);
    }

    protected String proxy(String x) {
        return x;
    }

    @Specialization
    protected RFunction wrap(RFunction x) {
        return proxy(x);
    }

    protected RFunction proxy(RFunction x) {
        return x;
    }

    @Specialization
    protected RIntSequence wrap(RIntSequence x) {
        return proxy(x);
    }

    protected RIntSequence proxy(RIntSequence x) {
        return (RIntSequence) proxySequence(x);
    }

    @Specialization
    protected RDoubleSequence wrap(RDoubleSequence x) {
        return x;
    }

    protected RDoubleSequence proxy(RDoubleSequence x) {
        return (RDoubleSequence) proxySequence(x);
    }

    @Specialization
    protected RLogicalVector wrap(RLogicalVector x) {
        return proxy(x);
    }

    protected RLogicalVector proxy(RLogicalVector x) {
        return (RLogicalVector) proxyVector(x);
    }

    @Specialization
    protected RIntVector wrap(RIntVector x) {
        return proxy(x);
    }

    protected RIntVector proxy(RIntVector x) {
        return (RIntVector) proxyVector(x);
    }

    @Specialization
    protected RDoubleVector wrap(RDoubleVector x) {
        return proxy(x);
    }

    protected RDoubleVector proxy(RDoubleVector x) {
        return (RDoubleVector) proxyVector(x);
    }

    @Specialization
    protected RRawVector wrap(RRawVector x) {
        return proxy(x);
    }

    protected RRawVector proxy(RRawVector x) {
        return (RRawVector) proxyVector(x);
    }

    @Specialization
    protected RComplexVector wrap(RComplexVector x) {
        return proxy(x);
    }

    protected RComplexVector proxy(RComplexVector x) {
        return (RComplexVector) proxyVector(x);
    }

    @Specialization
    protected RStringVector wrap(RStringVector x) {
        return proxy(x);
    }

    protected RStringVector proxy(RStringVector x) {
        return (RStringVector) proxyVector(x);
    }

    @Specialization
    protected RList wrap(RList x) {
        return proxy(x);
    }

    protected RList proxy(RList x) {
        return (RList) proxyVector(x);
    }

    @Specialization
    protected RDataFrame wrap(RDataFrame x) {
        return proxy(x);
    }

    protected RDataFrame proxy(RDataFrame x) {
        return proxyDataFrame(x);
    }

    @Specialization
    protected RFactor wrap(RFactor x) {
        return proxy(x);
    }

    protected RFactor proxy(RFactor x) {
        return proxyFactor(x);
    }

    @Specialization
    protected RMissing wrap(RMissing x) {
        return proxy(x);
    }

    protected RMissing proxy(RMissing x) {
        return x;
    }

    @Specialization
    protected REnvironment wrap(REnvironment x) {
        return proxy(x);
    }

    protected REnvironment proxy(REnvironment x) {
        return x;
    }

    @Specialization
    protected RConnection wrap(RConnection x) {
        return proxy(x);
    }

    protected RConnection proxy(RConnection x) {
        return x;
    }

    @Specialization
    protected RExpression wrap(RExpression x) {
        return proxy(x);
    }

    protected RExpression proxy(RExpression x) {
        return x;
    }

    @Specialization
    protected RSymbol wrap(RSymbol x) {
        return proxy(x);
    }

    protected RSymbol proxy(RSymbol x) {
        return x;
    }

    @Specialization
    protected RExternalPtr wrap(RExternalPtr x) {
        return proxy(x);
    }

    protected RExternalPtr proxy(RExternalPtr x) {
        return x;
    }

    @Specialization
    protected RLanguage wrap(RLanguage x) {
        return proxy(x);
    }

    protected RLanguage proxy(RLanguage x) {
        return x;
    }

    @Specialization
    protected RPromise wrap(RPromise x) {
        return proxy(x);
    }

    protected RPromise proxy(RPromise x) {
        return x;
    }

    @Specialization
    protected RPairList wrap(RPairList x) {
        return proxy(x);
    }

    protected RPairList proxy(RPairList x) {
        return x;
    }

    @Specialization
    protected RS4Object wrap(RS4Object x) {
        return proxy(x);
    }

    protected RS4Object proxy(RS4Object x) {
        return x;
    }

    @Specialization
    protected Object[] wrap(Object[] x) {
        return proxy(x);
    }

    protected Object[] proxy(Object[] x) {
        return x;
    }

    @Specialization
    protected RFormula wrap(RFormula x) {
        return proxy(x);
    }

    protected RFormula proxy(RFormula x) {
        return x;
    }

    @Specialization
    protected RArgsValuesAndNames wrap(RArgsValuesAndNames x) {
        return proxy(x);
    }

    protected RArgsValuesAndNames proxy(RArgsValuesAndNames x) {
        return x;
    }

}
