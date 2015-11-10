/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.parser.tools;

import java.util.*;

import com.oracle.truffle.r.parser.ast.*;
import com.oracle.truffle.r.parser.ast.Operation.Operator;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.nodes.*;

final class Info {
    public static final Info EMPTY = new Info(Collections.emptySet(), Collections.emptySet(), false);
    public static final Info ANY = new Info(Collections.emptySet(), Collections.emptySet(), true);

    public final Set<String> evaluatedNames;
    public final Set<String> maybeAssignedNames;
    public boolean assignsAny;

    private Info(Set<String> evaluatedNames, Set<String> maybeAssignedNames, boolean assignsAny) {
        this.evaluatedNames = evaluatedNames;
        this.maybeAssignedNames = maybeAssignedNames;
        this.assignsAny = assignsAny;
    }

    public static Info createNew() {
        return new Info(new HashSet<>(), new HashSet<>(), false);
    }

    public void addBefore(Info other) {
        if (other.assignsAny) {
            evaluatedNames.clear();
            assignsAny = true;
        }
        evaluatedNames.removeAll(other.maybeAssignedNames);
        evaluatedNames.addAll(other.evaluatedNames);
        maybeAssignedNames.addAll(other.maybeAssignedNames);
    }

    public static Info alternative(Info a, Info b) {
        Info result = createNew();
        result.evaluatedNames.addAll(a.evaluatedNames);
        result.evaluatedNames.retainAll(b.evaluatedNames);
        if (a.assignsAny || b.assignsAny) {
            result.assignsAny = true;
        } else {
            result.maybeAssignedNames.addAll(a.maybeAssignedNames);
            result.maybeAssignedNames.addAll(b.maybeAssignedNames);
        }
        return result;
    }
}

public final class EvaluatedArgumentsVisitor extends BasicVisitor<Info> {

    private static final Set<String> wellKnownFunctions = new HashSet<>(Arrays.asList("any", "dim", "dimnames", "is.null", "list", "names", "return", "print", "length", "rep", "max", "matrix",
                    "is.array", "is.element", "is.character", "exp", "all", "pmin", "pmax", "as.numeric", "as.integer", ".Call", "sum", "order", "rev", "integer", "double"));

    private EvaluatedArgumentsVisitor() {
        // private constructor
    }

    @Override
    public Info visit(ASTNode n) {
        return Info.ANY;
    }

    @Override
    public Info visit(Next n) {
        return Info.EMPTY;
    }

    @Override
    public Info visit(Break n) {
        return Info.EMPTY;
    }

    @Override
    public Info visit(SimpleAccessVariadicComponent n) {
        return Info.EMPTY;
    }

    @Override
    public Info visit(Missing m) {
        return Info.EMPTY;
    }

    @Override
    public Info visit(SimpleAccessTempVariable n) {
        return Info.EMPTY;
    }

    @Override
    public Info visit(Sequence n) {
        ASTNode[] exprs = n.getExpressions();

        Info info = Info.createNew();
        for (int i = exprs.length - 1; i >= 0; i--) {
            info.addBefore(exprs[i].accept(this));
        }
        return info;
    }

    @Override
    public Info visit(If n) {
        Info info;
        ASTNode t = n.getTrueCase();
        ASTNode f = n.getFalseCase();
        if (f == null) {
            info = Info.alternative(t.accept(this), Info.EMPTY);
        } else {
            info = Info.alternative(t.accept(this), f.accept(this));
        }
        info.addBefore(n.getCondition().accept(this));
        return info;
    }

    @Override
    public Info visit(BinaryOperation op) {
        Operator operator = op.getOperator();
        ASTNode left = op.getLHS();
        ASTNode right = op.getRHS();

        Info info = Info.createNew();
        if (operator == Operator.OR || operator == Operator.AND) {
            info.addBefore(Info.alternative(right.accept(this), Info.EMPTY));
            info.addBefore(left.accept(this));
        } else {
            info.addBefore(right.accept(this));
            info.addBefore(left.accept(this));
        }
        return info;
    }

    @Override
    public Info visit(UnaryOperation op) {
        return op.getLHS().accept(this);
    }

    @Override
    public Info visit(Constant n) {
        return Info.EMPTY;
    }

    @Override
    public Info visit(Repeat n) {
        return Info.alternative(n.getBody().accept(this), Info.EMPTY);
    }

    @Override
    public Info visit(While n) {
        Info info = Info.createNew();
        info.addBefore(Info.alternative(n.getBody().accept(this), Info.EMPTY));
        info.addBefore(n.getCondition().accept(this));
        return info;
    }

    @Override
    public Info visit(For n) {
        Info info = Info.createNew();
        info.addBefore(Info.alternative(n.getBody().accept(this), Info.EMPTY));
        info.addBefore(n.getRange().accept(this));
        return info;
    }

    @Override
    public Info visit(SimpleAssignVariable n) {
        if (n.isSuper()) {
            return Info.ANY;
        } else {
            Info info = Info.createNew();
            info.maybeAssignedNames.add(n.getVariable());
            info.addBefore(n.getExpr().accept(this));
            return info;
        }
    }

    @Override
    public Info visit(UpdateVector n) {
        Info info = Info.createNew();
        info.addBefore(n.getRHS().accept(this));
        info.addBefore(n.getVector().accept(this));
        return info;
    }

    @Override
    public Info visit(Replacement n) {
        if (n.isSuper()) {
            return Info.ANY;
        } else {
            Info info = Info.createNew();
            info.addBefore(n.getReplacementFunctionCall().accept(this));
            info.addBefore(n.getExpr().accept(this));
            return info;
        }
    }

    @Override
    public Info visit(FunctionCall n) {
        if (n.getLhs() instanceof String && wellKnownFunctions.contains(n.getLhs())) {
            List<ArgNode> arguments = n.getArguments();
            Info info = Info.createNew();
            for (int i = arguments.size() - 1; i >= 0; i--) {
                if (arguments.get(i).getValue() != null) {
                    info.addBefore(arguments.get(i).getValue().accept(this));
                }
            }
            return info;
        } else {
            return Info.ANY;
        }
    }

    @Override
    public Info visit(Function n) {
        return Info.ANY;
    }

    @Override
    public Info visit(SimpleAccessVariable n) {
        Info info = Info.createNew();
        info.evaluatedNames.add(n.getVariable());
        return info;
    }

    @Override
    public Info visit(FieldAccess n) {
        return n.getLhs().accept(this);
    }

    @Override
    public Info visit(UpdateField n) {
        Info info = Info.createNew();
        info.addBefore(n.getRHS().accept(this));
        info.addBefore(n.getVector().accept(this));
        return info;
    }

    public static FastPathFactory process(Function func) {
        Info info = func.getBody().accept(new EvaluatedArgumentsVisitor());
        boolean[] forcedArgument = new boolean[func.getSignature().size()];
        int cnt = 0;
        for (int i = 0; i < func.getSignature().size(); i++) {
            String argName = func.getSignature().get(i).getName();
            if (argName != null && !ArgumentsSignature.VARARG_NAME.equals(argName) && info.evaluatedNames.contains(argName)) {
                forcedArgument[i] = true;
                cnt++;
            }
        }
        if (cnt == 0) {
            return null;
        } else {
            return new FastPathFactory() {
                public RFastPathNode create() {
                    return null;
                }

                public boolean evaluatesArgument(int index) {
                    return false;
                }

                public boolean forcedEagerPromise(int index) {
                    return forcedArgument[index];
                }

                @Override
                public String toString() {
                    StringBuilder str = new StringBuilder();
                    for (int i = 0; i < func.getSignature().size(); i++) {
                        if (forcedArgument[i]) {
                            str.append(func.getSignature().get(i).getName()).append(' ');
                        }
                    }
                    return str.toString();
                }
            };
        }
    }
}
