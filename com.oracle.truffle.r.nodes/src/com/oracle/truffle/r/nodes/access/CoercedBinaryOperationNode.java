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
package com.oracle.truffle.r.nodes.access;

import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.nodes.*;

public abstract class CoercedBinaryOperationNode extends RNode {

    // Execute evaluated methods.

    public abstract RLogicalVector executeEvaluated(VirtualFrame frame, RLogicalVector vector, byte right) throws UnexpectedResultException;

    public abstract RLogicalVector executeEvaluated(VirtualFrame frame, RLogicalVector vector, RLogicalVector right) throws UnexpectedResultException;

    public abstract RIntVector executeEvaluated(VirtualFrame frame, RIntVector vector, int right) throws UnexpectedResultException;

    public abstract RIntVector executeEvaluated(VirtualFrame frame, RIntVector vector, RIntVector right) throws UnexpectedResultException;

    public abstract RDoubleVector executeEvaluated(VirtualFrame frame, RDoubleVector vector, double right) throws UnexpectedResultException;

    public abstract RDoubleVector executeEvaluated(VirtualFrame frame, RDoubleVector vector, RDoubleVector right) throws UnexpectedResultException;

    public abstract RComplexVector executeEvaluated(VirtualFrame frame, RComplexVector vector, RComplex right) throws UnexpectedResultException;

    public abstract RComplexVector executeEvaluated(VirtualFrame frame, RComplexVector vector, RComplexVector right) throws UnexpectedResultException;

    public abstract RStringVector executeEvaluated(VirtualFrame frame, RStringVector vector, String right) throws UnexpectedResultException;

    public abstract RStringVector executeEvaluated(VirtualFrame frame, RStringVector vector, RStringVector right) throws UnexpectedResultException;

    public abstract RAbstractVector executeEvaluated(VirtualFrame frame, RList list, RAbstractVector right) throws UnexpectedResultException;

    public abstract RAbstractVector executeEvaluated(VirtualFrame frame, RList list, RNull right) throws UnexpectedResultException;
}
