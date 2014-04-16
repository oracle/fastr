/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

@NodeChild("operand")
public abstract class RProxyNode extends RNode {

    protected Object proxyScalar(Object scalar) {
        return scalar;
    }

    protected RVector proxyVector(RVector vector) {
        return vector;
    }

    protected RSequence proxySequence(RSequence sequence) {
        return sequence;
    }

    @Specialization
    public RNull wrap(RNull x) {
        return proxy(x);
    }

    protected RNull proxy(RNull x) {
        return (RNull) proxyScalar(x);
    }

    @Specialization
    public byte wrap(byte x) {
        return proxy(x);
    }

    protected byte proxy(byte x) {
        return (byte) proxyScalar(x);
    }

    @Specialization
    public int wrap(int x) {
        return proxy(x);
    }

    protected int proxy(int x) {
        return (int) proxyScalar(x);
    }

    @Specialization
    public double wrap(double x) {
        return proxy(x);
    }

    protected double proxy(double x) {
        return (double) proxyScalar(x);
    }

    @Specialization
    public RRaw wrap(RRaw x) {
        return proxy(x);
    }

    protected RRaw proxy(RRaw x) {
        return (RRaw) proxyScalar(x);
    }

    @Specialization
    public RComplex wrap(RComplex x) {
        return proxy(x);
    }

    protected RComplex proxy(RComplex x) {
        return (RComplex) proxyScalar(x);
    }

    @Specialization
    public String wrap(String x) {
        return proxy(x);
    }

    protected String proxy(String x) {
        return (String) proxyScalar(x);
    }

    @Specialization
    public RFunction wrap(RFunction x) {
        return proxy(x);
    }

    protected RFunction proxy(RFunction x) {
        return (RFunction) proxyScalar(x);
    }

    @Specialization
    public RIntSequence wrap(RIntSequence x) {
        return proxy(x);
    }

    protected RIntSequence proxy(RIntSequence x) {
        return (RIntSequence) proxySequence(x);
    }

    @Specialization
    public RDoubleSequence wrap(RDoubleSequence x) {
        return x;
    }

    protected RDoubleSequence proxy(RDoubleSequence x) {
        return (RDoubleSequence) proxySequence(x);
    }

    @Specialization
    public RLogicalVector wrap(RLogicalVector x) {
        return proxy(x);
    }

    protected RLogicalVector proxy(RLogicalVector x) {
        return (RLogicalVector) proxyVector(x);
    }

    @Specialization
    public RIntVector wrap(RIntVector x) {
        return proxy(x);
    }

    protected RIntVector proxy(RIntVector x) {
        return (RIntVector) proxyVector(x);
    }

    @Specialization
    public RDoubleVector wrap(RDoubleVector x) {
        return proxy(x);
    }

    protected RDoubleVector proxy(RDoubleVector x) {
        return (RDoubleVector) proxyVector(x);
    }

    @Specialization
    public RRawVector wrap(RRawVector x) {
        return proxy(x);
    }

    protected RRawVector proxy(RRawVector x) {
        return (RRawVector) proxyVector(x);
    }

    @Specialization
    public RComplexVector wrap(RComplexVector x) {
        return proxy(x);
    }

    protected RComplexVector proxy(RComplexVector x) {
        return (RComplexVector) proxyVector(x);
    }

    @Specialization
    public RStringVector wrap(RStringVector x) {
        return proxy(x);
    }

    protected RStringVector proxy(RStringVector x) {
        return (RStringVector) proxyVector(x);
    }

    @Specialization
    public RList wrap(RList x) {
        return proxy(x);
    }

    protected RList proxy(RList x) {
        return (RList) proxyVector(x);
    }

    @Specialization
    public RMissing wrap(RMissing x) {
        return proxy(x);
    }

    protected RMissing proxy(RMissing x) {
        return (RMissing) proxyScalar(x);
    }

    @Specialization
    protected REnvironment wrap(REnvironment x) {
        return proxy(x);
    }

    protected REnvironment proxy(REnvironment x) {
        return (REnvironment) proxyScalar(x);
    }

    @Specialization
    protected RConnection wrap(RConnection x) {
        return proxy(x);
    }

    protected RConnection proxy(RConnection x) {
        return (RConnection) proxyScalar(x);
    }

    @Specialization
    protected RExpression wrap(RExpression x) {
        return proxy(x);
    }

    protected RExpression proxy(RExpression x) {
        return (RExpression) proxyScalar(x);
    }

    @Specialization
    protected Object[] wrap(Object[] x) {
        return proxy(x);
    }

    protected Object[] proxy(Object[] x) {
        return (Object[]) proxyScalar(x);
    }
}
