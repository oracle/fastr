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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode;
import com.oracle.truffle.r.nodes.function.visibility.GetVisibilityNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.nodes.RNode;

final class WithVisibleSpecial extends RNode {

    @Child private RNode delegate;
    @Child private GetVisibilityNode visibility = GetVisibilityNode.create();

    protected WithVisibleSpecial(RNode delegate) {
        this.delegate = delegate;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object value = delegate.visibleExecute(frame);
        if (value == RMissing.instance) {
            CompilerDirectives.transferToInterpreter();
            throw RError.error(this, Message.ARGUMENT_MISSING, "x");
        }
        return RDataFactory.createList(new Object[]{value, RRuntime.asLogical(visibility.execute(frame))}, WithVisible.LISTNAMES);
    }
}

// TODO The base package manual says this is a primitive but GNU R implements it as .Internal.
// That causes problems as the .Internal adds another layer of visibility setting that
// gets the wrong result. I believe that the only way to handle it as a .Internal would be to
// set noEvalArgs and evaluate the argument here and set the visibility explicitly.
@RBuiltin(name = "withVisible", kind = PRIMITIVE, parameterNames = "x", behavior = COMPLEX, nonEvalArgs = {0})
public abstract class WithVisible extends RBuiltinNode {

    static final RStringVector LISTNAMES = (RStringVector) RDataFactory.createStringVector(new String[]{"value", "visible"}, RDataFactory.COMPLETE_VECTOR).makeSharedPermanent();

    public static RNode createSpecial(@SuppressWarnings("unused") ArgumentsSignature signature, RNode[] arguments, @SuppressWarnings("unused") boolean inReplacement) {
        return arguments.length == 1 ? new WithVisibleSpecial(arguments[0]) : null;
    }

    @Specialization
    protected RList withVisible(VirtualFrame frame, RPromise x,
                    @Cached("create()") GetVisibilityNode visibility,
                    @Cached("new()") PromiseHelperNode promiseHelper) {
        if (x.isEvaluated()) {
            return RDataFactory.createList(new Object[]{x.getValue(), RRuntime.LOGICAL_TRUE}, LISTNAMES);
        }
        Object value = promiseHelper.evaluate(frame, x);
        if (value == RMissing.instance) {
            CompilerDirectives.transferToInterpreter();
            throw RError.error(this, Message.ARGUMENT_MISSING, "x");
        }
        return RDataFactory.createList(new Object[]{value, RRuntime.asLogical(visibility.execute(frame))}, LISTNAMES);
    }

    static {
        Casts.noCasts(WithVisible.class);
    }

    @Specialization
    protected RList withVisible(@SuppressWarnings("unused") RMissing x) {
        CompilerDirectives.transferToInterpreter();
        throw RError.error(this, Message.ARGUMENT_MISSING, "x");
    }
}
