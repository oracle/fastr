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
package com.oracle.truffle.r.nodes.binary;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@NodeChildren({@NodeChild(value = "left", type = RNode.class), @NodeChild(value = "right", type = RNode.class)})
public abstract class BinaryNode extends RNode {

    protected abstract RNode getLeft();

    protected abstract RNode getRight();

    public static boolean isEmpty(RAbstractVector left, @SuppressWarnings("unused") Object right) {
        return left.getLength() == 0;
    }

    public static boolean isEmpty(@SuppressWarnings("unused") Object left, RAbstractVector right) {
        return right.getLength() == 0;
    }

    public static boolean isEmpty(RAbstractVector left, RAbstractVector right) {
        return left.getLength() == 0 || right.getLength() == 0;
    }

    // int

    public static boolean isNA(int left) {
        return RRuntime.isNA(left);
    }

    public static boolean isNA(int left, int right) {
        return RRuntime.isNA(left) || RRuntime.isNA(right);
    }

    public static boolean isNA(int left, double right) {
        return RRuntime.isNA(left) || RRuntime.isNA(right);
    }

    public static boolean isNA(double left, int right) {
        return RRuntime.isNA(left) || RRuntime.isNA(right);
    }

    public static boolean isNA(int left, byte right) {
        return RRuntime.isNA(left) || RRuntime.isNA(right);
    }

    public static boolean isNA(byte left, int right) {
        return RRuntime.isNA(left) || RRuntime.isNA(right);
    }

    public static boolean isNA(int left, String right) {
        return RRuntime.isNA(left) || RRuntime.isNA(right);
    }

    public static boolean isNA(String left, int right) {
        return RRuntime.isNA(left) || RRuntime.isNA(right);
    }

    public static boolean isNA(int left, RComplex right) {
        return RRuntime.isNA(left) || RRuntime.isNA(right);
    }

    public static boolean isNA(RComplex left, int right) {
        return RRuntime.isNA(left) || RRuntime.isNA(right);
    }

    public static boolean isNA(int left, @SuppressWarnings("unused") RRaw right) {
        return RRuntime.isNA(left);
    }

    public static boolean isNA(@SuppressWarnings("unused") RRaw left, int right) {
        return RRuntime.isNA(right);
    }

    // double

    public static boolean isNA(double left) {
        return RRuntime.isNA(left);
    }

    public static boolean isNA(double left, double right) {
        return RRuntime.isNA(left) || RRuntime.isNA(right);
    }

    public static boolean isNA(double left, byte right) {
        return RRuntime.isNA(left) || RRuntime.isNA(right);
    }

    public static boolean isNA(byte left, double right) {
        return RRuntime.isNA(left) || RRuntime.isNA(right);
    }

    public static boolean isNA(double left, String right) {
        return RRuntime.isNA(left) || RRuntime.isNA(right);
    }

    public static boolean isNA(String left, double right) {
        return RRuntime.isNA(left) || RRuntime.isNA(right);
    }

    public static boolean isNA(double left, RComplex right) {
        return RRuntime.isNA(left) || RRuntime.isNA(right);
    }

    public static boolean isNA(RComplex left, double right) {
        return RRuntime.isNA(left) || RRuntime.isNA(right);
    }

    public static boolean isNA(double left, @SuppressWarnings("unused") RRaw right) {
        return RRuntime.isNA(left);
    }

    public static boolean isNA(@SuppressWarnings("unused") RRaw left, double right) {
        return RRuntime.isNA(right);
    }

    // logical

    public static boolean isNA(byte left) {
        return RRuntime.isNA(left);
    }

    public static boolean isNA(byte left, byte right) {
        return RRuntime.isNA(left) || RRuntime.isNA(right);
    }

    public static boolean isNA(byte left, String right) {
        return RRuntime.isNA(left) || RRuntime.isNA(right);
    }

    public static boolean isNA(String left, byte right) {
        return RRuntime.isNA(left) || RRuntime.isNA(right);
    }

    public static boolean isNA(byte left, RComplex right) {
        return RRuntime.isNA(left) || RRuntime.isNA(right);
    }

    public static boolean isNA(RComplex left, byte right) {
        return RRuntime.isNA(left) || RRuntime.isNA(right);
    }

    public static boolean isNA(byte left, @SuppressWarnings("unused") RRaw right) {
        return RRuntime.isNA(left);
    }

    public static boolean isNA(@SuppressWarnings("unused") RRaw left, byte right) {
        return RRuntime.isNA(right);
    }

    // string

    public static boolean isNA(String left) {
        return RRuntime.isNA(left);
    }

    public static boolean isNA(String left, String right) {
        return RRuntime.isNA(left) || RRuntime.isNA(right);
    }

    public static boolean isNA(String left, RComplex right) {
        return RRuntime.isNA(left) || RRuntime.isNA(right);
    }

    public static boolean isNA(RComplex left, String right) {
        return RRuntime.isNA(left) || RRuntime.isNA(right);
    }

    public static boolean isNA(String left, @SuppressWarnings("unused") RRaw right) {
        return RRuntime.isNA(left);
    }

    public static boolean isNA(@SuppressWarnings("unused") RRaw left, String right) {
        return RRuntime.isNA(right);
    }

    // complex

    public static boolean isNA(RComplex left) {
        return RRuntime.isNA(left);
    }

    public static boolean isNA(RComplex left, RComplex right) {
        return RRuntime.isNA(left) || RRuntime.isNA(right);
    }

    public static boolean isNA(RComplex left, @SuppressWarnings("unused") RRaw right) {
        return RRuntime.isNA(left);
    }

    public static boolean isNA(@SuppressWarnings("unused") RRaw left, RComplex right) {
        return RRuntime.isNA(right);
    }

}
