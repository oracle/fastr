/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

public abstract class FirstStringNode extends CastNode {

    private final RError.Message emptyError;
    private final String argumentName;

    protected FirstStringNode(Message emptyError, String argumentName) {
        this.emptyError = emptyError;
        this.argumentName = argumentName;
    }

    public final String executeString(Object argument) {
        return (String) execute(argument);
    }

    @Specialization
    protected String firstScalar(String argument) {
        return argument;
    }

    @Specialization(replaces = "firstScalar")
    protected String firstVector(RAbstractStringVector argument) {
        if (argument.getLength() != 1) {
            throw error(emptyError, argumentName);
        }
        return argument.getDataAt(0);
    }

    @Fallback
    protected String firstVectorFallback(@SuppressWarnings("unused") Object argument) {
        throw error(emptyError, argumentName);
    }

    public static FirstStringNode createWithError(RError.Message emptyError, String argumentName) {
        return FirstStringNodeGen.create(emptyError, argumentName);
    }
}
