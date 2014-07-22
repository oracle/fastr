/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;

import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.control.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

@RBuiltin(name = "formals", kind = RBuiltinKind.PRIMITIVE)
// TODO revert to INTERNAL when promises are lazy
public abstract class Formals extends RBuiltinNode {
    @Specialization
    public Object formals(Object funObj) {
        controlVisibility();
        if (funObj instanceof RFunction) {
            RFunction fun = (RFunction) funObj;
            if (fun.isBuiltin()) {
                return RNull.instance;
            }
            FunctionDefinitionNode fdNode = (FunctionDefinitionNode) fun.getTarget().getRootNode();
            Object[] params = fdNode.getParameterNames();
            if (params.length == 0) {
                return RNull.instance;
            }
            RNode[] defaults = getDefaults(fdNode);
            Object succ = null;
            for (int i = params.length - 1; i >= 0; i--) {
                RNode def = defaults[i];
                Object defValue = def == null ? RMissing.instance : RDataFactory.createLanguage(def);
                RPairList pl = new RPairList(defValue, succ, (String) params[i]);
                succ = pl;
            }
            return succ;
        } else {
            if (funObj instanceof String || funObj instanceof RStringVector) {
                throw RError.nyi(getEncapsulatingSourceSection(), "formals(string)");
            } else {
                return RNull.instance;
            }
        }
    }

    @SlowPath
    /**
     * Return the AST for the default values off the arguments of {@code node}.
     * TODO Should be easier with new function calling logic.
     * */
    private static RNode[] getDefaults(FunctionDefinitionNode node) {
        WriteVariableNode[] formals = getFormals(node);
        RNode[] result = new RNode[formals.length];
        for (int i = 0; i < formals.length; i++) {
            WriteVariableNode wvn = formals[i];
            AccessArgumentNode arg = (AccessArgumentNode) (wvn.getRhs());
            Node defaultValue = arg.getChildren().iterator().next();
            if (!(defaultValue instanceof ConstantNode.ConstantMissingNode)) {
                result[i] = (RNode) NodeUtil.cloneNode(defaultValue);
            }

        }
        return result;
    }

    /**
     * Return the formal arguments as an array of {@link WriteVariableNode} instances.
     */
    @SlowPath
    private static WriteVariableNode[] getFormals(FunctionDefinitionNode node) {
        int paramCount = node.getParameterCount();
        WriteVariableNode[] result = new WriteVariableNode[paramCount];
        Iterator<Node> iter = node.getChildren().iterator();
        SequenceNode body = (SequenceNode) iter.next();
        iter = body.getChildren().iterator();
        int i = 0;
        while (i < paramCount && iter.hasNext()) {
            Node child = iter.next();
            if (child instanceof WriteVariableNode) {
                result[i] = (WriteVariableNode) child;
            } else {
                break;
            }
            i++;
        }
        return result;

    }
}
