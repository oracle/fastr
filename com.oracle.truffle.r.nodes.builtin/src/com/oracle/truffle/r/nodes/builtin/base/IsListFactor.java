/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2000--2015, The R Core Team
 * Copyright (c) 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.IsListFactorNodeGen.IsListFactorInternalNodeGen;
import com.oracle.truffle.r.nodes.unary.IsFactorNode;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;

// from apply.c

@RBuiltin(name = "islistfactor", kind = INTERNAL, parameterNames = {"x", "recursive"}, behavior = PURE)
public abstract class IsListFactor extends RBuiltinNode {

    protected abstract static class IsListFactorInternal extends Node {

        public final boolean recursive;

        @Child private IsListFactorInternal recursiveNode;

        public abstract boolean execute(Object value);

        IsListFactorInternal(boolean recursive) {
            this.recursive = recursive;
        }

        @Specialization(guards = "list.getLength() > 0")
        protected boolean islistfactor(RAbstractListVector list, //
                        @Cached("new()") IsFactorNode isFactor) {
            for (int i = 0; i < list.getLength(); i++) {
                Object value = list.getDataAt(i);
                if (recursive && value instanceof RAbstractListVector) {
                    if (recursiveNode == null) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        recursiveNode = insert(IsListFactorInternalNodeGen.create(recursive));
                    }
                    if (!recursiveNode.execute(value)) {
                        return false;
                    }
                } else if (!isFactor.executeIsFactor(value)) {
                    return false;
                }
            }
            return true;
        }

        @Fallback
        protected boolean islistfactor(@SuppressWarnings("unused") Object list) {
            return false;
        }
    }

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.arg("recursive").asLogicalVector().findFirst().map(toBoolean());
    }

    protected static IsListFactorInternal createNode(boolean recursive) {
        return IsListFactorInternalNodeGen.create(recursive);
    }

    @Specialization(guards = "recursive == node.recursive")
    protected byte isListFactor(Object value, @SuppressWarnings("unused") boolean recursive, //
                    @Cached("createNode(recursive)") IsListFactorInternal node) {
        return RRuntime.asLogical(node.execute(value));
    }
}
