/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.parser.ast;

import java.util.*;

import com.oracle.truffle.api.source.*;

public abstract class Call extends ASTNode {

    private final List<ArgNode> arguments;

    protected Call(SourceSection source, List<ArgNode> arguments) {
        super(source);
        this.arguments = arguments;
    }

    @Override
    public <R> List<R> visitAll(Visitor<R> v) {
        List<R> list = new ArrayList<>();
        for (ArgNode e : arguments) {
            ASTNode n = e.getValue();
            if (n != null) {
                list.add(n.accept(v));
            }
        }
        return list;
    }

    public List<ArgNode> getArguments() {
        return arguments;
    }

    public static ASTNode create(SourceSection source, ASTNode call, List<ArgNode> arguments) {
        for (ArgNode a : arguments) {
            // otherwise "empty" indexes are not recorded at all
            if (a.getName() == null && a.getValue() == null) {
                a.value = new Missing(a.getSource());
            }
        }
        if (call instanceof SimpleAccessVariable) {
            SimpleAccessVariable ccall = (SimpleAccessVariable) call;
            return create(source, ccall.getVariable(), arguments);
        } else if (call instanceof Constant) {
            Constant c = (Constant) call;
            assert c.getType() == Constant.ConstantType.STRING;
            assert c.getValues().length == 1;
            return create(source, c.getValues()[0], arguments);
        } else if (call instanceof FunctionCall) {
            return new FunctionCall(source, (FunctionCall) call, arguments);
        } else {
            return new FunctionCall(source, call, arguments, false);
        }
    }

    public static ASTNode create(SourceSection src, String funName, List<ArgNode> args) {
        return new FunctionCall(src, funName, args);
    }

    public static ASTNode create(SourceSection src, CallOperator op, ASTNode lhs, List<ArgNode> args) {
        for (ArgNode a : args) {
            // otherwise "empty" indexes are not recorded at all
            if (a.getName() == null && a.getValue() == null) {
                a.value = new Missing(a.getSource());
            }
        }
        args.add(0, ArgNode.create(src, null, lhs));
        return new AccessVector(src, lhs, args, op == CallOperator.SUBSET);
    }

    public enum CallOperator {
        SUBSET,
        SUBSCRIPT
    }
}
