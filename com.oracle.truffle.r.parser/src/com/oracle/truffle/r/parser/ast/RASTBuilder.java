/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.parser.ast;

import java.util.List;

import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.parser.RCodeBuilder;
import com.oracle.truffle.r.parser.ast.Operation.ArithmeticOperator;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RNull;

public final class RASTBuilder implements RCodeBuilder<ASTNode, ArgNode> {

    public ArgNode argument(SourceSection source, String name, ASTNode expression) {
        return new ArgNode(source, name, expression == null && name == null ? new Missing() : expression);
    }

    public ArgNode argument(ASTNode expression) {
        return new ArgNode(expression.getSource(), null, expression);
    }

    public ArgNode argumentEmpty() {
        return new ArgNode(null, null, new Missing());
    }

    public ASTNode call(SourceSection source, ASTNode lhs, List<ArgNode> arguments) {
        if (lhs instanceof AccessVariable) {
            String symbol = ((AccessVariable) lhs).getVariable();
            if (arguments.size() == 0) {
                switch (symbol) {
                    case "break":
                        return new Break(source);
                    case "next":
                        return new Next(source);
                }
            } else if (arguments.size() == 1) {
                // handle unary arithmetics, for the time being
                for (ArithmeticOperator op : ArithmeticOperator.values()) {
                    if (op.getName().equals(symbol)) {
                        return new UnaryOperation(source, lhs.getSource(), op, arguments.get(0).getValue());
                    }
                }
                switch (symbol) {
                    case "repeat":
                        return new Repeat(source, arguments.get(0).getValue());
                    case "(":
                        return arguments.get(0).getValue();
                }
            } else if (arguments.size() == 2) {
                // handle binary arithmetics, for the time being
                for (ArithmeticOperator op : ArithmeticOperator.values()) {
                    if (op.getName().equals(symbol)) {
                        return new BinaryOperation(source, lhs.getSource(), op, arguments.get(0).getValue(), arguments.get(1).getValue());
                    }
                }
                switch (symbol) {
                    case "while":
                        return new While(source, arguments.get(0).getValue(), arguments.get(1).getValue());
                    case "if":
                        return new If(source, arguments.get(0).getValue(), arguments.get(1).getValue(), null);
                    case "=":
                    case "<-":
                    case ":=":
                        return AssignVariable.create(source, lhs.getSource(), false, arguments.get(0).getValue(), arguments.get(1).getValue());
                    case "<<-":
                        return AssignVariable.create(source, lhs.getSource(), true, arguments.get(0).getValue(), arguments.get(1).getValue());
                    case "->":
                        return AssignVariable.create(source, lhs.getSource(), false, arguments.get(1).getValue(), arguments.get(0).getValue());
                    case "->>":
                        return AssignVariable.create(source, lhs.getSource(), true, arguments.get(1).getValue(), arguments.get(0).getValue());
                }
            } else if (arguments.size() == 3) {
                switch (symbol) {
                    case "for":
                        if (arguments.get(0).getValue() instanceof AccessVariable) {
                            String name = ((AccessVariable) arguments.get(0).getValue()).getVariable();
                            return new For(source, name, arguments.get(1).getValue(), arguments.get(2).getValue());
                        }
                        break;
                    case "if":
                        return new If(source, arguments.get(0).getValue(), arguments.get(1).getValue(), arguments.get(2).getValue());
                }
            }
            switch (symbol) {
                case "{":
                    return new Sequence(source, arguments.stream().map(ArgNode::getValue).toArray(ASTNode[]::new));
            }
        }
        return Call.create(source, lhs.getSource(), lhs, arguments);
    }

    public ASTNode function(SourceSection source, List<ArgNode> params, ASTNode body) {
        return new Function(source, params, body);
    }

    public ASTNode constant(SourceSection source, Object value) {
        assert value instanceof Byte || value instanceof Integer || value instanceof Double || value instanceof RComplex || value instanceof String || value instanceof RNull : value.getClass();
        if (value instanceof String && !RRuntime.isNA((String) value)) {
            return new Constant(source, ((String) value).intern());
        } else {
            return new Constant(source, value);
        }
    }

    public ASTNode lookup(SourceSection source, String symbol, boolean functionLookup) {
        if (AccessVariadicComponent.getVariadicComponentIndex(symbol) != -1) {
            assert !functionLookup;
            return new AccessVariadicComponent(source, symbol);
        }
        return new AccessVariable(source, symbol, functionLookup);
    }

    public void warning(Message message, Object... arguments) {
        RError.warning(RError.NO_CALLER, message, arguments);
    }
}
