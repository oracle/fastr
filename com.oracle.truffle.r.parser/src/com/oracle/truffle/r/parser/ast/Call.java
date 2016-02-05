/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.parser.ast;

import java.util.*;

import com.oracle.truffle.api.source.*;

public final class Call extends ASTNode {

    // LHS of a call does not need to be a symbol, it can be a lambda expression (FunctionCall)
    private final Object lhs;
    private final List<ArgNode> arguments;

    private Call(SourceSection source, Object lhs, List<ArgNode> arguments) {
        super(source);
        this.lhs = lhs;
        this.arguments = arguments;
    }

    public Object getLhs() {
        return lhs;
    }

    public boolean isSymbol() {
        return lhs instanceof String;
    }

    public String getName() {
        String result = (String) lhs;
        return result;
    }

    public Call getFunctionCall() {
        return (Call) lhs;
    }

    public ASTNode getLhsNode() {
        return (ASTNode) lhs;
    }

    @Override
    public <R> R accept(Visitor<R> v) {
        return v.visit(this);
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

    public static ASTNode create(SourceSection src, String fun, List<ArgNode> args) {
        return new Call(src, fun, args);
    }

    public static ASTNode create(SourceSection src, ASTNode call, List<ArgNode> arguments) {
        for (ArgNode a : arguments) {
            // otherwise "empty" indexes are not recorded at all
            if (a.getName() == null && a.getValue() == null) {
                a.setValue(new Missing(a.getSource()));
            }
        }
        // Add "call"'s source to src
        SourceSection callSrc = combineSource(call.getSource(), src);
        if (call instanceof AccessVariable) {
            AccessVariable ccall = (AccessVariable) call;
            return new Call(callSrc, ccall.getVariable(), arguments);
        } else if (call instanceof Constant) {
            Constant c = (Constant) call;
            assert c.getValue() instanceof String;
            return new Call(callSrc, c.getValue(), arguments);
        } else {
            return new Call(callSrc, call, arguments);
        }
    }

    public static ASTNode create(SourceSection src, CallOperator op, ASTNode lhs, ASTNode... additionalArgs) {
        List<ArgNode> args = new ArrayList<>();
        args.add(ArgNode.create(lhs.getSource(), null, lhs));
        for (ASTNode arg : additionalArgs) {
            args.add(ArgNode.create(arg.getSource(), null, arg));
        }
        return new Call(src, op.getOpName(), args);
    }

    public static ASTNode create(SourceSection src, CallOperator op, ASTNode lhs, List<ArgNode> args) {
        for (ArgNode a : args) {
            // otherwise "empty" indexes are not recorded at all
            if (a.getName() == null && a.getValue() == null) {
                a.setValue(new Missing(a.getSource()));
            }
        }
        // lhs is actually the first argument when rewritten as a call, `[`(lhs, args)
        args.add(0, ArgNode.create(lhs.getSource(), null, lhs));
        // adjust src to encompass the entire expression
        SourceSection newSrc = combineSource(lhs.getSource(), src);
        if (args.size() == 1) {
            args.add(ArgNode.create(null, null, new Missing(null)));
        }
        return new Call(newSrc, op.getOpName(), args);
    }

    public enum CallOperator {
        SUBSET("["),
        SUBSCRIPT("[["),
        FIELD("$"),
        AT("@");

        private final String opName;

        CallOperator(String opName) {
            this.opName = opName;
        }

        public String getOpName() {
            return opName;
        }
    }
}
