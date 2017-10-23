/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.nodes;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.builtins.RBuiltinDescriptor;
import com.oracle.truffle.r.runtime.builtins.RBuiltinKind;
import com.oracle.truffle.r.runtime.context.RContext;

final class Info {
    public static final Info EMPTY = new Info(Collections.emptySet(), Collections.emptySet(), false);
    public static final Info ANY = new Info(Collections.emptySet(), Collections.emptySet(), true);

    public final Set<String> evaluatedNames;
    public final Set<String> maybeAssignedNames;
    private boolean assignsAny;

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

public final class EvaluatedArgumentsVisitor extends RSyntaxVisitor<Info> {

    private static final Set<String> wellKnownFunctions = new HashSet<>(Arrays.asList("c", "$", "@", "[", "[[", "any", "dim", "dimnames", "rownames", "colnames", "is.null", "list", "names", "return",
                    "print", "length", "rep", "inherits", "min", "max", "matrix", "table", "is.array", "is.element", "is.character", "exp", "all", "pmin", "pmax", "as.numeric", "proc.time",
                    "as.integer", "as.character", "as.matrix", ".Call", "sum", "order", "rev", "integer", "double", "as.numeric", "as.list", "as.integer", ".Call", ".FastR", "unname", "log", "lgamma",
                    "sin", "cos", "tan", "exp", "log", "expm1", "sinh", "sinpi", "cosh", "cospi", "tanh", "tanpi", "asin", "asinh", "acos", "acosh", "atan", "atanh", "+", "-",
                    "*", "/", "%%", "^", ":", ">=", ">", "<=", "<", "==", "!=", "||", "|", "&&", "&", "!", "%o%", "%*%", "%/%", "%in%", "{", "for", "while", "repeat", "if", "attributes", "attr"));

    private EvaluatedArgumentsVisitor() {
        // private constructor
    }

    @Override
    protected Info visit(RSyntaxCall element) {
        RSyntaxElement lhs = element.getSyntaxLHS();
        if (lhs instanceof RSyntaxLookup) {
            String symbol = ((RSyntaxLookup) lhs).getIdentifier();
            RSyntaxElement[] arguments = element.getSyntaxArguments();
            if (".Internal".equals(symbol) && arguments.length == 1 && arguments[0] instanceof RSyntaxCall) {
                RSyntaxCall innerCall = (RSyntaxCall) arguments[0];
                if (innerCall.getSyntaxLHS() instanceof RSyntaxLookup) {
                    String innerSymbol = ((RSyntaxLookup) innerCall.getSyntaxLHS()).getIdentifier();
                    RSyntaxElement[] innerArguments = innerCall.getSyntaxArguments();
                    RBuiltinDescriptor builtin = RContext.lookupBuiltinDescriptor(innerSymbol);
                    if (builtin != null && builtin.getKind() == RBuiltinKind.INTERNAL) {
                        ArgumentsSignature signature = builtin.getSignature();
                        if (signature.getVarArgCount() == 0) {
                            // holds only for well-formed code, so we cannot rely on it:
                            // assert innerArguments.length == signature.getLength();
                        } else {
                            assert signature.getVarArgCount() == 1 : signature;
                            assert innerArguments.length == signature.getLength() || signature.getVarArgIndex() == signature.getLength() - 1 : signature;
                            // holds only for well-formed code, so we cannot rely on it:
                            // assert innerArguments.length >= signature.getLength() - 1 : signature
                            // + " " + innerArguments.length;
                        }
                        Info info = Info.createNew();
                        for (int i = innerArguments.length - 1; i >= 0; i--) {
                            if (innerArguments[i] != null) {
                                if (builtin.evaluatesArg(Math.min(i, signature.getLength() - 1))) {
                                    info.addBefore(accept(innerArguments[i]));
                                } else {
                                    info.addBefore(Info.alternative(accept(innerArguments[i]), Info.EMPTY));
                                }
                            }
                        }
                        return info;
                    }
                }
            }
            if (symbol.equals("<-")) {
                Info info = Info.createNew();
                RSyntaxElement current = arguments[0];
                if (arguments[0] instanceof RSyntaxLookup) {
                    info.maybeAssignedNames.add(((RSyntaxLookup) arguments[0]).getIdentifier());
                } else {
                    info.addBefore(accept(arguments[0]));
                    while (current instanceof RSyntaxCall) {
                        current = ((RSyntaxCall) current).getSyntaxArguments()[0];
                    }
                    if (current instanceof RSyntaxLookup) {
                        info.evaluatedNames.add(((RSyntaxLookup) current).getIdentifier());
                    } else {
                        return Info.ANY;
                    }
                }
                if (arguments.length == 2) {
                    info.addBefore(accept(arguments[1]));
                }
                return info;
            } else if (wellKnownFunctions.contains(symbol)) {
                Info info = Info.createNew();
                switch (symbol) {
                    case "||":
                    case "&&":
                        assert arguments.length == 2;
                        if (arguments[1] != null) {
                            info.addBefore(Info.alternative(accept(arguments[1]), Info.EMPTY));
                        }
                        if (arguments[0] != null) {
                            info.addBefore(accept(arguments[0]));
                        }
                        return info;
                    case "repeat":
                        assert arguments.length == 1;
                        return accept(arguments[0]);
                    case "while":
                        assert arguments.length == 2;
                        info.addBefore(Info.alternative(accept(arguments[1]), Info.EMPTY));
                        info.addBefore(accept(arguments[0]));
                        return info;
                    case "for":
                        assert arguments.length == 3;
                        info.addBefore(Info.alternative(accept(arguments[2]), Info.EMPTY));
                        info.addBefore(accept(arguments[1]));
                        return info;
                    case "if":
                        assert arguments.length == 2 || arguments.length == 3;
                        if (arguments.length == 2) {
                            info = Info.alternative(accept(arguments[1]), Info.EMPTY);
                        } else {
                            info = Info.alternative(accept(arguments[1]), accept(arguments[2]));
                        }
                        info.addBefore(accept(arguments[0]));
                        return info;
                    default:
                        for (int i = arguments.length - 1; i >= 0; i--) {
                            if (arguments[i] != null) {
                                info.addBefore(accept(arguments[i]));
                            }
                        }
                        return info;
                }
            }
        }
        return Info.ANY;
    }

    @Override
    protected Info visit(RSyntaxConstant element) {
        return Info.EMPTY;
    }

    @Override
    protected Info visit(RSyntaxLookup element) {
        Info info = Info.createNew();
        info.evaluatedNames.add(element.getIdentifier());
        return info;
    }

    @Override
    protected Info visit(RSyntaxFunction element) {
        return Info.ANY;
    }

    public static EvaluatedArgumentsFastPath process(RSyntaxElement body, ArgumentsSignature signature) {
        Info info = new EvaluatedArgumentsVisitor().accept(body);
        boolean[] forcedArguments = new boolean[signature.getLength()];
        int cnt = 0;
        for (int i = 0; i < signature.getLength(); i++) {
            String argName = signature.getName(i);
            if (argName != null && info.evaluatedNames.contains(argName)) {
                forcedArguments[i] = true;
                cnt++;
            }
        }
        if (cnt == 0) {
            return null;
        } else {
            return new EvaluatedArgumentsFastPath(forcedArguments);
        }
    }

    public static boolean isSimpleArgument(RSyntaxElement node) {
        if (node instanceof RSyntaxCall) {
            RSyntaxCall call = (RSyntaxCall) node;
            RSyntaxElement lhs = call.getSyntaxLHS();
            if (lhs instanceof RSyntaxLookup) {
                if (wellKnownFunctions.contains(((RSyntaxLookup) lhs).getIdentifier())) {
                    for (RSyntaxElement arg : call.getSyntaxArguments()) {
                        if (!isSimpleArgument(arg)) {
                            return false;
                        }
                    }
                    return true;
                }
            }
        } else if (node instanceof RSyntaxLookup) {
            return true;
        } else if (node instanceof RSyntaxConstant) {
            return true;
        }
        return false;
    }

    /**
     * If any of the arguments has assignment call in it, we fallback to unoptimized promises.
     */
    public static boolean hasAssignmentCall(RSyntaxElement node) {
        if (node instanceof RSyntaxCall) {
            RSyntaxCall call = (RSyntaxCall) node;
            RSyntaxElement lhs = call.getSyntaxLHS();
            if (lhs instanceof RSyntaxLookup) {
                if (((RSyntaxLookup) lhs).getIdentifier().equals("<-")) {
                    return true;
                }
                for (RSyntaxElement arg : call.getSyntaxArguments()) {
                    if (hasAssignmentCall(arg)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
