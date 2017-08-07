/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1997-2013,  The R Core Team
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.stats.deriv;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.asLogicalVector;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.boxPrimitive;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.chain;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.findFirst;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.lengthGte;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.lengthLte;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.logicalValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.map;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.notEmpty;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.nodes.RRootNode;
import com.oracle.truffle.r.nodes.access.ConstantNode;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.nodes.function.FormalArguments;
import com.oracle.truffle.r.nodes.function.RCallSpecialNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.TruffleRLanguage;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.REmpty;
import com.oracle.truffle.r.runtime.data.RExpression;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.nodes.RCodeBuilder;
import com.oracle.truffle.r.runtime.nodes.RCodeBuilder.Argument;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxCall;
import com.oracle.truffle.r.runtime.nodes.RSyntaxConstant;
import com.oracle.truffle.r.runtime.nodes.RSyntaxElement;
import com.oracle.truffle.r.runtime.nodes.RSyntaxFunction;
import com.oracle.truffle.r.runtime.nodes.RSyntaxLookup;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxVisitor;

//Transcribed from GnuR, library/stats/src/deriv.c

public abstract class Deriv extends RExternalBuiltinNode {

    static {
        Casts casts = new Casts(Deriv.class);
        casts.arg(1, "namevec").mustBe(stringValue()).asStringVector().mustBe(notEmpty(), RError.Message.INVALID_VARIABLE_NAMES);
        casts.arg(2, "function.arg").mapIf(logicalValue(), chain(asLogicalVector()).with(findFirst().logicalElement()).with(map(toBoolean())).end()).mapIf(stringValue(), boxPrimitive());
        casts.arg(3, "tag").defaultError(RError.Message.INVALID_VARIABLE_NAMES).mustBe(stringValue()).asStringVector().mustBe(notEmpty()).findFirst().mustBe(lengthGte(1).and(lengthLte(60)));
        casts.arg(4, "hessian").asLogicalVector().findFirst(RRuntime.LOGICAL_NA).map(toBoolean());
    }

    static final String LEFT_PAREN = "(";
    static final String PLUS = "+";
    static final String MINUS = "-";
    static final String TIMES = "*";
    static final String DIVIDE = "/";
    static final String POWER = "^";
    static final String LOG = "log";
    static final String EXP = "exp";
    static final String COS = "cos";
    static final String SIN = "sin";
    static final String TAN = "tan";
    static final String COSH = "cosh";
    static final String SINH = "sinh";
    static final String TANH = "tanh";
    static final String SQRT = "sqrt";
    static final String PNORM = "pnorm";
    static final String DNORM = "dnorm";
    static final String ASIN = "asin";
    static final String ACOS = "acos";
    static final String ATAN = "atan";
    static final String GAMMA = "gamma";
    static final String LGAMMA = "lgamma";
    static final String DIGAMMA = "digamma";
    static final String TRIGAMMA = "trigamma";
    static final String PSIGAMMA = "psigamma";

    public static Deriv create() {
        return DerivNodeGen.create();
    }

    public abstract Object execute(VirtualFrame frame, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5);

    @Override
    public Object call(VirtualFrame frame, RArgsValuesAndNames args) {
        checkLength(args, 5);
        return execute(frame, castArg(args, 0), castArg(args, 1), castArg(args, 2), castArg(args, 3), castArg(args, 4));
    }

    @Override
    protected Object call(RArgsValuesAndNames args) {
        throw RInternalError.shouldNotReachHere();
    }

    protected static boolean isConstant(Object expr) {
        return !(expr instanceof RLanguage || expr instanceof RExpression || expr instanceof RSymbol);
    }

    @Specialization(guards = "isConstant(expr)")
    protected Object derive(VirtualFrame frame, Object expr, RAbstractStringVector names, Object functionArg, String tag, boolean hessian) {
        return derive(frame.materialize(), createConstant(expr), names, functionArg, tag, hessian);
    }

    @TruffleBoundary
    private static ConstantNode createConstant(Object expr) {
        return ConstantNode.create(expr);
    }

    @Specialization
    protected Object derive(VirtualFrame frame, RSymbol expr, RAbstractStringVector names, Object functionArg, String tag, boolean hessian) {
        return derive(frame.materialize(), createLookup(expr), names, functionArg, tag, hessian);
    }

    @TruffleBoundary
    private static RBaseNode createLookup(RSymbol expr) {
        return (RBaseNode) RContext.getASTBuilder().lookup(RSyntaxNode.LAZY_DEPARSE, expr.getName(), false);
    }

    @Specialization
    protected Object derive(VirtualFrame frame, RExpression expr, RAbstractStringVector names, Object functionArg, String tag, boolean hessian,
                    @Cached("create()") Deriv derivNode) {
        return derivNode.execute(frame, expr.getDataAt(0), names, functionArg, tag, hessian);
    }

    @Specialization
    protected Object derive(VirtualFrame frame, RLanguage expr, RAbstractStringVector names, Object functionArg, String tag, boolean hessian) {
        return derive(frame.materialize(), expr.getRep(), names, functionArg, tag, hessian);
    }

    @TruffleBoundary
    private Object derive(MaterializedFrame frame, RBaseNode elem, RAbstractStringVector names, Object functionArg, String tag, boolean hessian) {
        return findDerive(elem, names, functionArg, tag, hessian).getResult(frame.materialize(), getRLanguage());
    }

    private static final class DerivResult {
        private final RExpression result;
        private final RSyntaxNode blockCall;
        private final List<Argument<RSyntaxNode>> targetArgs;

        private DerivResult(RExpression result) {
            this.result = result;
            blockCall = null;
            targetArgs = null;
        }

        private DerivResult(RSyntaxNode blockCall, List<Argument<RSyntaxNode>> targetArgs) {
            this.blockCall = blockCall;
            this.targetArgs = targetArgs;
            result = null;
        }

        private Object getResult(MaterializedFrame frame, TruffleRLanguage language) {
            if (result != null) {
                return result;
            }
            RootCallTarget callTarget = RContext.getASTBuilder().rootFunction(language, RSyntaxNode.LAZY_DEPARSE, targetArgs, blockCall, null);
            FrameSlotChangeMonitor.initializeEnclosingFrame(callTarget.getRootNode().getFrameDescriptor(), frame);
            return RDataFactory.createFunction(RFunction.NO_NAME, RFunction.NO_NAME, callTarget, null, frame);
        }
    }

    @TruffleBoundary
    private DerivResult findDerive(RBaseNode elem, RAbstractStringVector names, Object functionArg, String tag, boolean hessian) {
        LinkedList<RSyntaxNode> exprlist = new LinkedList<>();
        int fIndex = findSubexpression(elem, exprlist, tag);

        int nderiv = names.getLength();
        int[] dIndex = new int[nderiv];
        int[] d2Index = hessian ? new int[(nderiv * (1 + nderiv)) / 2] : null;
        for (int i = 0, k = 0; i < nderiv; i++) {
            RBaseNode dExpr = d(elem, names.getDataAt(i));
            dIndex[i] = findSubexpression(dExpr, exprlist, tag);

            if (hessian) {
                for (int j = i; j < nderiv; j++) {
                    RBaseNode d2Expr = d(dExpr, names.getDataAt(j));
                    d2Index[k] = findSubexpression(d2Expr, exprlist, tag);
                    k++;
                }
            }
        }

        int nexpr = exprlist.size();

        if (fIndex > 0) {
            exprlist.add(createLookup(tag + fIndex));
        } else {
            exprlist.add(cloneElement(elem.asRSyntaxNode()));
        }

        exprlist.add(null);
        if (hessian) {
            exprlist.add(null);
        }

        for (int i = 0, k = 0; i < nderiv; i++) {
            if (dIndex[i] > 0) {
                exprlist.add(createLookup(tag + dIndex[i]));

                if (hessian) {
                    RBaseNode dExpr = d(elem, names.getDataAt(i));
                    for (int j = i; j < nderiv; j++) {
                        if (d2Index[k] > 0) {
                            exprlist.add(createLookup(tag + d2Index[k]));
                        } else {
                            exprlist.add((RSyntaxNode) d(dExpr, names.getDataAt(j)));
                        }
                        k++;
                    }
                }
            } else {
                // the first derivative is constant or simple variable
                // TODO: do not call the d twice
                RBaseNode dExpr = d(elem, names.getDataAt(i));
                exprlist.add((RSyntaxNode) dExpr);

                if (hessian) {
                    for (int j = i; j < nderiv; j++) {
                        if (d2Index[k] > 0) {
                            exprlist.add(createLookup(tag + d2Index[k]));
                        } else {
                            RBaseNode d2Expr = d(dExpr, names.getDataAt(j));
                            if (isZero((RSyntaxElement) d2Expr)) {
                                exprlist.add(null);
                            } else {
                                exprlist.add((RSyntaxNode) d2Expr);
                            }
                        }
                        k++;
                    }
                }
            }
        }

        exprlist.add(null);
        exprlist.add(null);
        if (hessian) {
            exprlist.add(null);
        }

        for (int i = 0; i < nexpr; i++) {
            String subexprName = tag + (i + 1);
            if (countOccurences(subexprName, exprlist, i + 1) < 2) {
                replace(subexprName, exprlist.get(i), exprlist, i + 1);
                exprlist.set(i, null);
            } else {
                exprlist.set(i, createAssignNode(subexprName, exprlist.get(i)));
            }
        }

        int p = nexpr;
        exprlist.set(p++, createAssignNode(".value", exprlist.get(nexpr))); // .value <-
        exprlist.set(p++, createGrad(names)); // .grad <-
        if (hessian) {
            exprlist.set(p++, createHess(names)); // .hessian
        }
        // .grad[, "..."] <- ...
        for (int i = 0; i < nderiv; i++) {
            RSyntaxNode ans = exprlist.get(p);
            exprlist.set(p, derivAssign(names.getDataAt(i), ans));
            p++;

            if (hessian) {
                for (int j = i; j < nderiv; j++, p++) {
                    ans = exprlist.get(p);
                    if (ans != null) {
                        if (i == j) {
                            exprlist.set(p, hessAssign1(names.getDataAt(i), addParens(ans)));
                        } else {
                            exprlist.set(p, hessAssign2(names.getDataAt(i), names.getDataAt(j), addParens(ans)));
                        }
                    }
                }
            }
        }
        // attr(.value, "gradient") <- .grad
        exprlist.set(p++, addGrad());
        if (hessian) {
            exprlist.set(p++, addHess());
        }

        // .value
        exprlist.set(p++, createLookup(".value"));

        // prune exprlist
        exprlist.removeAll(Collections.singleton(null));

        List<Argument<RSyntaxNode>> blockStatements = new ArrayList<>(exprlist.size());
        for (RSyntaxNode e : exprlist) {
            blockStatements.add(RCodeBuilder.argument(e));
        }
        RSyntaxNode blockCall = RContext.getASTBuilder().call(RSyntaxNode.LAZY_DEPARSE, createLookup("{"), blockStatements);

        if (functionArg instanceof RAbstractStringVector) {
            RAbstractStringVector funArgNames = (RAbstractStringVector) functionArg;
            List<Argument<RSyntaxNode>> targetArgs = new ArrayList<>();
            for (int i = 0; i < funArgNames.getLength(); i++) {
                targetArgs.add(RCodeBuilder.argument(RSyntaxNode.LAZY_DEPARSE, funArgNames.getDataAt(i), ConstantNode.create(RMissing.instance)));
            }

            return new DerivResult(blockCall, targetArgs);
        } else if (functionArg == Boolean.TRUE) {
            List<Argument<RSyntaxNode>> targetArgs = new ArrayList<>();
            for (int i = 0; i < names.getLength(); i++) {
                targetArgs.add(RCodeBuilder.argument(RSyntaxNode.LAZY_DEPARSE, names.getDataAt(i), ConstantNode.create(RMissing.instance)));
            }

            return new DerivResult(blockCall, targetArgs);
        } else if (functionArg instanceof RFunction) {
            RFunction funTemplate = (RFunction) functionArg;
            FormalArguments formals = ((RRootNode) funTemplate.getRootNode()).getFormalArguments();
            RNode[] defArgs = formals.getArguments();
            List<Argument<RSyntaxNode>> targetArgs = new ArrayList<>();
            for (int i = 0; i < defArgs.length; i++) {
                targetArgs.add(RCodeBuilder.argument(RSyntaxNode.LAZY_DEPARSE, formals.getSignature().getName(i), cloneElement((RSyntaxNode) defArgs[i])));
            }

            return new DerivResult(blockCall, targetArgs);
        } else {
            RLanguage lan = RDataFactory.createLanguage(blockCall.asRNode());
            RExpression res = RDataFactory.createExpression(new Object[]{lan});
            return new DerivResult(res);
        }
    }

    private int findSubexpression(RBaseNode expr, List<RSyntaxNode> exprlist, String tag) {
        if (!(expr instanceof RSyntaxElement)) {
            throw RError.error(RError.SHOW_CALLER, RError.Message.INVALID_EXPRESSION, "FindSubexprs");
        }

        RSyntaxVisitor<Integer> vis = new RSyntaxVisitor<Integer>() {
            @Override
            protected Integer visit(RSyntaxCall call) {
                if (call.getSyntaxLHS() instanceof RSyntaxLookup && ((RSyntaxLookup) call.getSyntaxLHS()).getIdentifier() == LEFT_PAREN) {
                    return accept(call.getSyntaxArguments()[0]);
                }

                RSyntaxElement[] args = call.getSyntaxArguments();
                List<Argument<RSyntaxNode>> newArgs = new ArrayList<>();
                for (int i = 0; i < args.length; i++) {
                    int k = accept(args[i]);
                    if (k > 0) {
                        newArgs.add(RCodeBuilder.argument(createLookup(tag + k)));
                    } else {
                        newArgs.add(RCodeBuilder.argument(cloneElement(args[i])));
                    }
                }
                RSyntaxNode newCall = RContext.getASTBuilder().call(call.getSourceSection(), cloneElement(call.getSyntaxLHS()), newArgs);
                return accumulate(newCall, exprlist);
            }

            @Override
            protected Integer visit(RSyntaxConstant element) {
                return checkConstant(element.getValue());
            }

            @Override
            protected Integer visit(RSyntaxLookup element) {
                return 0;
            }

            @Override
            protected Integer visit(RSyntaxFunction element) {
                throw RError.error(RError.SHOW_CALLER, RError.Message.INVALID_EXPRESSION, "FindSubexprs");
            }
        };
        return vis.accept((RSyntaxElement) expr);
    }

    private static int checkConstant(Object val) {
        if (val instanceof Double || val instanceof Integer || val instanceof RComplex || val instanceof Byte || val instanceof RSymbol) {
            return 0;
        } else {
            throw RError.error(RError.SHOW_CALLER, RError.Message.INVALID_EXPRESSION, "FindSubexprs");
        }
    }

    private static boolean isDoubleValue(RSyntaxElement elem, double value) {
        if (elem instanceof RSyntaxConstant) {
            Object val = ((RSyntaxConstant) elem).getValue();
            if (val instanceof Number) {
                return ((Number) val).doubleValue() == value;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    static boolean isZero(RSyntaxElement elem) {
        return isDoubleValue(elem, 0.);
    }

    static boolean isOne(RSyntaxElement elem) {
        return isDoubleValue(elem, 1.);
    }

    private int accumulate(RSyntaxElement expr, List<RSyntaxNode> exprlist) {
        for (int k = 0; k < exprlist.size(); k++) {
            if (equal(expr, exprlist.get(k))) {
                return k + 1;
            }
        }
        exprlist.add((RSyntaxNode) expr);
        return exprlist.size();
    }

    // TODO: move to a utility class
    private boolean equal(RSyntaxElement expr1, RSyntaxElement expr2) {
        if (expr1.getClass() != expr2.getClass()) {
            return false;
        }
        if (expr1 instanceof RSyntaxLookup) {
            return ((RSyntaxLookup) expr1).getIdentifier() == ((RSyntaxLookup) expr2).getIdentifier();
        }
        if (expr1 instanceof RSyntaxConstant) {
            return ((RSyntaxConstant) expr1).getValue().equals(((RSyntaxConstant) expr2).getValue());
        }
        if (expr1 instanceof RSyntaxCall) {
            RSyntaxElement[] args1 = ((RSyntaxCall) expr1).getSyntaxArguments();
            RSyntaxElement[] args2 = ((RSyntaxCall) expr2).getSyntaxArguments();
            if (args1.length != args2.length) {
                return false;
            }
            if (!equal(((RSyntaxCall) expr1).getSyntaxLHS(), ((RSyntaxCall) expr2).getSyntaxLHS())) {
                return false;
            }
            for (int i = 0; i < args1.length; i++) {
                if (!equal(args1[i], args2[i])) {
                    return false;
                }
            }
            return true;
        }

        throw RError.error(RError.SHOW_CALLER, RError.Message.INVALID_EXPRESSION, "equal");
    }

    static String getFunctionName(RSyntaxElement expr) {
        if (expr instanceof RSyntaxCall) {
            RSyntaxCall call = (RSyntaxCall) expr;
            return call.getSyntaxLHS() instanceof RSyntaxLookup ? ((RSyntaxLookup) call.getSyntaxLHS()).getIdentifier() : null;
        } else {
            return null;
        }
    }

    static RSyntaxNode cloneElement(RSyntaxElement element) {
        return RContext.getASTBuilder().process(element);
    }

    private static RBaseNode d(RBaseNode expr, String var) {
        if (!(expr instanceof RSyntaxElement)) {
            throw RError.error(RError.SHOW_CALLER, RError.Message.INVALID_EXPRESSION, "FindSubexprs");
        }

        RSyntaxVisitor<RSyntaxElement> vis = new DerivVisitor(var);
        return (RBaseNode) vis.accept((RSyntaxElement) expr);
    }

    private static int argsLength(RSyntaxElement elem) {
        if (elem instanceof RSyntaxCall) {
            return ((RSyntaxCall) elem).getSyntaxArguments().length;
        } else {
            return 0;
        }
    }

    static RSyntaxElement arg(RSyntaxElement elem, int argIndex) {
        assert elem instanceof RSyntaxCall && (argIndex < ((RSyntaxCall) elem).getSyntaxArguments().length);
        return ((RSyntaxCall) elem).getSyntaxArguments()[argIndex];
    }

    private static RSyntaxElement setArg(RSyntaxElement elem, int argIndex, RSyntaxElement arg) {
        assert elem instanceof RSyntaxCall && (argIndex < ((RSyntaxCall) elem).getSyntaxArguments().length);
        RSyntaxCall call = (RSyntaxCall) elem;
        RSyntaxElement[] args = call.getSyntaxArguments();
        RSyntaxNode[] newArgs = new RSyntaxNode[args.length];
        for (int i = 0; i < args.length; i++) {
            if (i == argIndex) {
                newArgs[i] = (RSyntaxNode) arg;
            } else {
                newArgs[i] = cloneElement(args[i]);
            }
        }
        return RCallSpecialNode.createCall(call.getSourceSection(), (RNode) cloneElement(call.getSyntaxLHS()), ArgumentsSignature.empty(args.length), newArgs);
    }

    static RSyntaxNode newCall(String functionName, RSyntaxElement arg1, RSyntaxElement arg2) {
        if (arg2 == null) {
            return RContext.getASTBuilder().call(RSyntaxNode.SOURCE_UNAVAILABLE, createLookup(functionName), (RSyntaxNode) arg1);
        } else {
            return RContext.getASTBuilder().call(RSyntaxNode.SOURCE_UNAVAILABLE, createLookup(functionName), (RSyntaxNode) arg1, (RSyntaxNode) arg2);
        }
    }

    private static int countOccurences(String subexprName, List<RSyntaxNode> exprlist, int fromIndex) {
        if (fromIndex >= exprlist.size()) {
            return 0;
        }

        RSyntaxNode exprListNode = exprlist.get(fromIndex);
        if (exprListNode == null) {
            return countOccurences(subexprName, exprlist, fromIndex + 1);
        }

        RSyntaxVisitor<Integer> vis = new RSyntaxVisitor<Integer>() {
            @Override
            protected Integer visit(RSyntaxCall element) {
                RSyntaxElement[] args = element.getSyntaxArguments();
                int cnt = 0;
                for (int i = 0; i < args.length; i++) {
                    cnt += accept(args[i]);
                }
                return cnt;
            }

            @Override
            protected Integer visit(RSyntaxConstant element) {
                return 0;
            }

            @Override
            protected Integer visit(RSyntaxLookup element) {
                return subexprName.equals(element.getIdentifier()) ? 1 : 0;
            }

            @Override
            protected Integer visit(RSyntaxFunction element) {
                throw RInternalError.shouldNotReachHere();
            }
        };

        return vis.accept(exprListNode) + countOccurences(subexprName, exprlist, fromIndex + 1);
    }

    private static void replace(String subexprName, RSyntaxNode replacement, List<RSyntaxNode> exprlist, int fromIndex) {
        if (fromIndex >= exprlist.size()) {
            return;
        }

        RSyntaxElement exprListNode = exprlist.get(fromIndex);
        if (exprListNode == null) {
            replace(subexprName, replacement, exprlist, fromIndex + 1);
            return;
        }

        RSyntaxVisitor<RSyntaxElement> vis = new RSyntaxVisitor<RSyntaxElement>() {

            // TODO: do not create a new call node after the first replacement

            @Override
            protected RSyntaxElement visit(RSyntaxCall call) {
                RSyntaxElement[] args = call.getSyntaxArguments();
                RSyntaxNode[] newArgs = new RSyntaxNode[args.length];
                for (int i = 0; i < args.length; i++) {
                    newArgs[i] = (RSyntaxNode) accept(args[i]);
                }
                return RCallSpecialNode.createCall(call.getSourceSection(), (RNode) call.getSyntaxLHS(), ArgumentsSignature.empty(args.length), newArgs);
            }

            @Override
            protected RSyntaxElement visit(RSyntaxConstant element) {
                return element;
            }

            @Override
            protected RSyntaxElement visit(RSyntaxLookup element) {
                return subexprName.equals(element.getIdentifier()) ? replacement : element;
            }

            @Override
            protected RSyntaxElement visit(RSyntaxFunction element) {
                throw RInternalError.shouldNotReachHere();
            }
        };

        exprlist.set(fromIndex, (RSyntaxNode) vis.accept(exprListNode));

        replace(subexprName, replacement, exprlist, fromIndex + 1);
    }

    private static RSyntaxNode createLookup(String name) {
        return RContext.getASTBuilder().lookup(RSyntaxNode.SOURCE_UNAVAILABLE, name, false);
    }

    private static RSyntaxNode createAssignNode(String varName, RSyntaxNode rhs) {
        return RContext.getASTBuilder().call(RSyntaxNode.SOURCE_UNAVAILABLE, createLookup("<-"), createLookup(varName.intern()), addParens(rhs));
    }

    private static RSyntaxNode hessAssign1(String varName, RSyntaxNode rhs) {
        RSyntaxNode tmp = RContext.getASTBuilder().call(RSyntaxNode.SOURCE_UNAVAILABLE, createLookup("["), createLookup(".hessian"), ConstantNode.create(REmpty.instance),
                        ConstantNode.create(varName.intern()), ConstantNode.create(varName.intern()));
        return RContext.getASTBuilder().call(RSyntaxNode.SOURCE_UNAVAILABLE, createLookup("<-"), tmp, rhs);
    }

    private static RSyntaxNode hessAssign2(String varName1, String varName2, RSyntaxNode rhs) {
        RSyntaxNode tmp1 = RContext.getASTBuilder().call(RSyntaxNode.SOURCE_UNAVAILABLE, createLookup("["), createLookup(".hessian"), ConstantNode.create(REmpty.instance),
                        ConstantNode.create(varName1.intern()), ConstantNode.create(varName2.intern()));
        RSyntaxNode tmp2 = RContext.getASTBuilder().call(RSyntaxNode.SOURCE_UNAVAILABLE, createLookup("["), createLookup(".hessian"), ConstantNode.create(REmpty.instance),
                        ConstantNode.create(varName2.intern()), ConstantNode.create(varName1.intern()));

        RSyntaxNode tmp3 = RContext.getASTBuilder().call(RSyntaxNode.SOURCE_UNAVAILABLE, createLookup("<-"), tmp2, rhs);
        return RContext.getASTBuilder().call(RSyntaxNode.SOURCE_UNAVAILABLE, createLookup("<-"), tmp1, tmp3);
    }

    private static RSyntaxNode createGrad(RAbstractStringVector names) {
        int n = names.getLength();
        List<Argument<RSyntaxNode>> cArgs = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            cArgs.add(RCodeBuilder.argument(ConstantNode.create(names.getDataAt(i).intern())));
        }
        RSyntaxNode tmp1 = RContext.getASTBuilder().call(RSyntaxNode.SOURCE_UNAVAILABLE, createLookup("c"), cArgs);
        RSyntaxNode dimnames = RContext.getASTBuilder().call(RSyntaxNode.SOURCE_UNAVAILABLE, createLookup("list"), ConstantNode.create(RNull.instance), tmp1);

        RSyntaxNode tmp2 = RContext.getASTBuilder().call(RSyntaxNode.SOURCE_UNAVAILABLE, createLookup("length"), createLookup(".value"));
        RSyntaxNode dim = RContext.getASTBuilder().call(RSyntaxNode.SOURCE_UNAVAILABLE, createLookup("c"), tmp2, ConstantNode.create(n));
        ConstantNode data = ConstantNode.create(0.);

        RSyntaxNode p = RContext.getASTBuilder().call(RSyntaxNode.SOURCE_UNAVAILABLE, createLookup("array"), data, dim, dimnames);
        return RContext.getASTBuilder().call(RSyntaxNode.SOURCE_UNAVAILABLE, createLookup("<-"), createLookup(".grad"), p);
    }

    private static RSyntaxNode createHess(RAbstractStringVector names) {
        int n = names.getLength();
        List<Argument<RSyntaxNode>> cArgs = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            cArgs.add(RCodeBuilder.argument(ConstantNode.create(names.getDataAt(i).intern())));
        }
        RSyntaxNode tmp1 = RContext.getASTBuilder().call(RSyntaxNode.SOURCE_UNAVAILABLE, createLookup("c"), cArgs);
        RSyntaxNode tmp1Clone = cloneElement(tmp1);
        RSyntaxNode dimnames = RContext.getASTBuilder().call(RSyntaxNode.SOURCE_UNAVAILABLE, createLookup("list"), ConstantNode.create(RNull.instance), tmp1, tmp1Clone);

        RSyntaxNode tmp2 = RContext.getASTBuilder().call(RSyntaxNode.SOURCE_UNAVAILABLE, createLookup("length"), createLookup(".value"));
        RSyntaxNode dim = RContext.getASTBuilder().call(RSyntaxNode.SOURCE_UNAVAILABLE, createLookup("c"), tmp2, ConstantNode.create(n), ConstantNode.create(n));
        ConstantNode data = ConstantNode.create(0.);

        RSyntaxNode p = RContext.getASTBuilder().call(RSyntaxNode.SOURCE_UNAVAILABLE, createLookup("array"), data, dim, dimnames);
        return RContext.getASTBuilder().call(RSyntaxNode.SOURCE_UNAVAILABLE, createLookup("<-"), createLookup(".hessian"), p);
    }

    private static RSyntaxNode derivAssign(String name, RSyntaxNode expr) {
        RSyntaxNode tmp = RContext.getASTBuilder().call(RSyntaxNode.SOURCE_UNAVAILABLE, createLookup("["), createLookup(".grad"), ConstantNode.create(REmpty.instance),
                        ConstantNode.create(name.intern()));
        return RContext.getASTBuilder().call(RSyntaxNode.SOURCE_UNAVAILABLE, createLookup("<-"), tmp, expr);
    }

    private static RSyntaxNode addGrad() {
        RSyntaxNode tmp = RContext.getASTBuilder().call(RSyntaxNode.SOURCE_UNAVAILABLE, createLookup("attr"), createLookup(".value"), ConstantNode.create("gradient"));
        return RContext.getASTBuilder().call(RSyntaxNode.SOURCE_UNAVAILABLE, createLookup("<-"), tmp, createLookup(".grad"));
    }

    private static RSyntaxNode addHess() {
        RSyntaxNode tmp = RContext.getASTBuilder().call(RSyntaxNode.SOURCE_UNAVAILABLE, createLookup("attr"), createLookup(".value"), ConstantNode.create("hessian"));
        return RContext.getASTBuilder().call(RSyntaxNode.SOURCE_UNAVAILABLE, createLookup("<-"), tmp, createLookup(".hessian"));
    }

    private static boolean isForm(RSyntaxElement expr, String functionName) {
        return argsLength(expr) == 2 && getFunctionName(expr) == functionName;
    }

    static RSyntaxNode addParens(RSyntaxElement node) {
        RSyntaxElement expr = node;
        if (node instanceof RSyntaxCall) {
            RSyntaxCall call = (RSyntaxCall) node;
            RSyntaxElement[] args = call.getSyntaxArguments();
            RSyntaxNode[] newArgs = new RSyntaxNode[args.length];
            for (int i = 0; i < args.length; i++) {
                newArgs[i] = addParens(args[i]);
            }
            expr = RCallSpecialNode.createCall(call.getSourceSection(), (RNode) cloneElement(call.getSyntaxLHS()), ArgumentsSignature.empty(args.length), newArgs);
        }

        if (isForm(expr, PLUS)) {
            if (isForm(arg(expr, 1), PLUS)) {
                expr = setArg(expr, 1, newCall(LEFT_PAREN, arg(expr, 1), null));
            }
        } else if (isForm(expr, MINUS)) {
            if (isForm(arg(expr, 1), PLUS) || isForm(arg(expr, 1), MINUS)) {
                expr = setArg(expr, 1, newCall(LEFT_PAREN, arg(expr, 1), null));
            }
        } else if (isForm(expr, TIMES)) {
            if (isForm(arg(expr, 1), PLUS) || isForm(arg(expr, 1), MINUS) || isForm(arg(expr, 1), TIMES) ||
                            isForm(arg(expr, 1), DIVIDE)) {
                expr = setArg(expr, 1, newCall(LEFT_PAREN, arg(expr, 1), null));
            }
            if (isForm(arg(expr, 0), MINUS) || isForm(arg(expr, 0), MINUS)) {
                expr = setArg(expr, 0, newCall(LEFT_PAREN, arg(expr, 0), null));
            }
        } else if (isForm(expr, DIVIDE)) {
            if (isForm(arg(expr, 1), PLUS) || isForm(arg(expr, 1), MINUS) || isForm(arg(expr, 1), TIMES) ||
                            isForm(arg(expr, 1), DIVIDE)) {
                expr = setArg(expr, 1, newCall(LEFT_PAREN, arg(expr, 1), null));
            }
            if (isForm(arg(expr, 0), PLUS) || isForm(arg(expr, 0), MINUS)) {
                expr = setArg(expr, 0, newCall(LEFT_PAREN, arg(expr, 0), null));
            }
        } else if (isForm(expr, POWER)) {
            if (isForm(arg(expr, 0), POWER)) {
                expr = setArg(expr, 0, newCall(LEFT_PAREN, arg(expr, 0), null));
            }
            if (isForm(arg(expr, 1), PLUS) || isForm(arg(expr, 1), MINUS) || isForm(arg(expr, 1), TIMES) ||
                            isForm(arg(expr, 1), DIVIDE)) {
                expr = setArg(expr, 1, newCall(LEFT_PAREN, arg(expr, 1), null));
            }
        }
        return (RSyntaxNode) expr;
    }
}
