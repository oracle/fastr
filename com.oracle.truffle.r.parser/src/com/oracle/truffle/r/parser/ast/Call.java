/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2012-2013, Purdue University
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.parser.ast;

import java.util.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.r.parser.ast.ArgumentList.*;

public abstract class Call extends ASTNode {

    final ArgumentList args;

    public Call(ArgumentList alist) {
        args = alist;
    }

    @Override
    public <R> List<R> visitAll(Visitor<R> v) {
        List<R> list = new ArrayList<>();
        for (Entry e : args) {
            ASTNode n = e.getValue();
            if (n != null) {
                list.add(n.accept(v));
            }
        }
        return list;
    }

    public ArgumentList getArgs() {
        return args;
    }

    public static ASTNode create(SourceSection src, ASTNode call, ArgumentList args) {
        if (call instanceof SimpleAccessVariable) {
            SimpleAccessVariable ccall = (SimpleAccessVariable) call;
            return create(src, ccall.getSymbol(), args);
        } else if (call instanceof Constant) {
            Constant c = (Constant) call;
            assert c.getType() == Constant.ConstantType.STRING;
            assert c.getValues().length == 1;
            return create(src, Symbol.getSymbol(c.getValues()[0]), args);
        }
        return null;
    }

    public static ASTNode create(SourceSection src, Symbol funName, ArgumentList args) {
        return new FunctionCall(src, funName, args);
    }

    public static ASTNode create(SourceSection src, CallOperator op, ASTNode lhs, ArgumentList args) {
        return new AccessVector(src, lhs, args, op == CallOperator.SUBSET);
    }

    public enum CallOperator {
        SUBSET, SUBSCRIPT
    }
}
