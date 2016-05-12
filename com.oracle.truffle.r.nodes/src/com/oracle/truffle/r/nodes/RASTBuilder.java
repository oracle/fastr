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
package com.oracle.truffle.r.nodes;

import java.util.ArrayList;
import java.util.List;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.nodes.access.AccessArgumentNode;
import com.oracle.truffle.r.nodes.access.ConstantNode;
import com.oracle.truffle.r.nodes.access.ReadVariadicComponentNode;
import com.oracle.truffle.r.nodes.access.WriteVariableNode;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.control.BlockNode;
import com.oracle.truffle.r.nodes.control.BreakNode;
import com.oracle.truffle.r.nodes.control.ForNode;
import com.oracle.truffle.r.nodes.control.IfNode;
import com.oracle.truffle.r.nodes.control.NextNode;
import com.oracle.truffle.r.nodes.control.ReplacementNode;
import com.oracle.truffle.r.nodes.control.WhileNode;
import com.oracle.truffle.r.nodes.function.FormalArguments;
import com.oracle.truffle.r.nodes.function.FunctionDefinitionNode;
import com.oracle.truffle.r.nodes.function.FunctionExpressionNode;
import com.oracle.truffle.r.nodes.function.PostProcessArgumentsNode;
import com.oracle.truffle.r.nodes.function.RCallNode;
import com.oracle.truffle.r.nodes.function.SaveArgumentsNode;
import com.oracle.truffle.r.nodes.function.WrapDefaultArgumentNode;
import com.oracle.truffle.r.nodes.unary.GetNonSharedNodeGen;
import com.oracle.truffle.r.parser.tools.EvaluatedArgumentsVisitor;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.FastROptions;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.FastPathFactory;
import com.oracle.truffle.r.runtime.data.REmpty;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;
import com.oracle.truffle.r.runtime.nodes.RCodeBuilder;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxCall;
import com.oracle.truffle.r.runtime.nodes.RSyntaxConstant;
import com.oracle.truffle.r.runtime.nodes.RSyntaxElement;
import com.oracle.truffle.r.runtime.nodes.RSyntaxFunction;
import com.oracle.truffle.r.runtime.nodes.RSyntaxLookup;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxVisitor;

/**
 * This class can be used to build fragments of Truffle AST that correspond to R language
 * constructs: calls, lookups, constants and functions.
 *
 * Additionally, this class has helper functions to issue (parser) warnings and
 *
 */
public final class RASTBuilder implements RCodeBuilder<RSyntaxNode> {

    @Override
    public RSyntaxNode process(RSyntaxElement original) {
        return new RSyntaxVisitor<RSyntaxNode>() {

            @Override
            protected RSyntaxNode visit(RSyntaxCall element) {
                ArrayList<Argument<RSyntaxNode>> args = createArguments(element.getSyntaxSignature(), element.getSyntaxArguments());
                return call(element.getSourceSection(), accept(element.getSyntaxLHS()), args);
            }

            private ArrayList<Argument<RSyntaxNode>> createArguments(ArgumentsSignature signature, RSyntaxElement[] arguments) {
                ArrayList<Argument<RSyntaxNode>> args = new ArrayList<>(arguments.length);
                for (int i = 0; i < arguments.length; i++) {
                    args.add(RCodeBuilder.argument(arguments[i] == null ? null : arguments[i].getSourceSection(), signature.getName(i), arguments[i] == null ? null : accept(arguments[i])));
                }
                return args;
            }

            @Override
            protected RSyntaxNode visit(RSyntaxConstant element) {
                return constant(element.getSourceSection(), element.getValue());
            }

            @Override
            protected RSyntaxNode visit(RSyntaxLookup element) {
                return lookup(element.getSourceSection(), element.getIdentifier(), element.isFunctionLookup());
            }

            @Override
            protected RSyntaxNode visit(RSyntaxFunction element) {
                ArrayList<Argument<RSyntaxNode>> params = createArguments(element.getSyntaxSignature(), element.getSyntaxArgumentDefaults());
                return function(element.getSourceSection(), params, accept(element.getSyntaxBody()), null);
            }
        }.accept(original);
    }

    @Override
    public RSyntaxNode call(SourceSection source, RSyntaxNode lhs, List<Argument<RSyntaxNode>> args) {
        if (lhs instanceof RSyntaxLookup) {
            String symbol = ((RSyntaxLookup) lhs).getIdentifier();
            if (args.size() == 0) {
                switch (symbol) {
                    case "break":
                        return new BreakNode(source);
                    case "next":
                        return new NextNode(source);
                }
            } else if (args.size() == 1) {
                switch (symbol) {
                    case "repeat":
                        return WhileNode.create(source, ConstantNode.create(RRuntime.LOGICAL_TRUE), args.get(0).value, true);
                    case "(":
                        return args.get(0).value;
                }
            } else if (args.size() == 2) {
                switch (symbol) {
                    case "while":
                        return WhileNode.create(source, args.get(0).value, args.get(1).value, false);
                    case "if":
                        return IfNode.create(source, args.get(0).value, args.get(1).value, null);
                    case "=":
                    case "<-":
                    case ":=":
                    case "<<-":
                    case "->":
                    case "->>":
                        boolean isSuper = "<<-".equals(symbol) || "->>".equals(symbol);
                        boolean switchArgs = "->".equals(symbol) || "->>".equals(symbol);
                        String operator = "=".equals(symbol) ? "=" : isSuper ? "<<-" : "<-";
                        return createReplacement(source, operator, isSuper, args.get(switchArgs ? 1 : 0).value, args.get(switchArgs ? 0 : 1).value);
                }
            } else if (args.size() == 3) {
                switch (symbol) {
                    case "for":
                        if (args.get(0).value instanceof RSyntaxLookup) {
                            String name = ((RSyntaxLookup) args.get(0).value).getIdentifier();
                            WriteVariableNode cvar = WriteVariableNode.create(source, name, null, false);
                            return ForNode.create(source, cvar, args.get(1).value, args.get(2).value);
                        }
                        break;
                    case "if":
                        return IfNode.create(source, args.get(0).value, args.get(1).value, args.get(2).value);
                }
            }
            switch (symbol) {
                case "{":
                    return new BlockNode(source, args.stream().map(n -> n.value.asRNode()).toArray(RNode[]::new));
            }
        }

        ArgumentsSignature signature = createSignature(args);
        RSyntaxNode[] nodes = args.stream().map(
                        arg -> (arg.value == null && arg.name == null) ? ConstantNode.create(arg.source == null ? RSyntaxNode.SOURCE_UNAVAILABLE : arg.source, REmpty.instance) : arg.value).toArray(
                                        RSyntaxNode[]::new);

        return RCallNode.createCall(source, lhs.asRNode(), signature, nodes);
    }

    private RSyntaxNode createReplacement(SourceSection source, String operator, boolean isSuper, RSyntaxNode replacementLhs, RSyntaxNode replacementRhs) {
        if (replacementLhs instanceof RSyntaxCall) {
            return createReplacement(source, replacementLhs, replacementRhs, operator, isSuper);
        } else {
            String name;
            if (replacementLhs instanceof RSyntaxLookup) {
                name = ((RSyntaxLookup) replacementLhs).getIdentifier();
            } else if (replacementLhs instanceof RSyntaxConstant) {
                RSyntaxConstant c = (RSyntaxConstant) replacementLhs;
                assert c.getValue() instanceof String;
                name = (String) c.getValue();
            } else {
                throw RInternalError.unimplemented("unexpected lhs type: " + replacementLhs.getClass());
            }
            return (RSyntaxNode) WriteVariableNode.create(source, name, replacementRhs.asRNode(), isSuper);
        }
    }

    private static ArgumentsSignature createSignature(List<Argument<RSyntaxNode>> args) {
        String[] argumentNames = args.stream().map(arg -> arg.name).toArray(String[]::new);
        ArgumentsSignature signature = ArgumentsSignature.get(argumentNames);
        return signature;
    }

    private static String getFunctionDescription(SourceSection source, RSyntaxNode assignedTo) {
        if (assignedTo instanceof RSyntaxLookup) {
            return ((RSyntaxLookup) assignedTo).getIdentifier();
        } else {
            String functionBody = source.getCode();
            return functionBody.substring(0, Math.min(functionBody.length(), 40)).replace("\n", "\\n");
        }
    }

    /**
     * Creates a call that looks like {@code fun} but has the first argument replaced with
     * {@code newLhs}.
     */
    private RCallNode createFunctionQuery(RSyntaxNode newLhs, RSyntaxCall fun) {
        RSyntaxElement[] arguments = fun.getSyntaxArguments();

        RSyntaxNode[] argNodes = new RSyntaxNode[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            argNodes[i] = i == 0 ? newLhs : process(arguments[i]);
        }

        return RCallNode.createCall(fun.getSourceSection(), process(fun.getSyntaxLHS()).asRNode(), fun.getSyntaxSignature(), argNodes);
    }

    /**
     * Creates a call that looks like {@code fun}, but has its first argument replaced with
     * {@code newLhs}, its target turned into an update function ("foo<-"), with the given value
     * added to the arguments list.
     */
    private RCallNode createFunctionUpdate(SourceSection source, RSyntaxNode newLhs, RSyntaxNode rhs, RSyntaxCall fun) {
        RSyntaxElement[] arguments = fun.getSyntaxArguments();

        ArgumentsSignature signature = fun.getSyntaxSignature();
        RSyntaxNode[] argNodes = new RSyntaxNode[arguments.length + 1];
        String[] names = new String[argNodes.length];
        for (int i = 0; i < arguments.length; i++) {
            names[i] = signature.getName(i);
            argNodes[i] = i == 0 ? newLhs : process(arguments[i]);
        }
        argNodes[argNodes.length - 1] = rhs;
        names[argNodes.length - 1] = "value";

        RSyntaxElement syntaxLHS = fun.getSyntaxLHS();
        RSyntaxNode newSyntaxLHS;
        if (syntaxLHS instanceof RSyntaxLookup) {
            RSyntaxLookup lookupLHS = (RSyntaxLookup) syntaxLHS;
            String symbol = lookupLHS.getIdentifier();
            if ("slot".equals(symbol) || "@".equals(symbol)) {
                // this is pretty gross, but at this point seems like the only way to get setClass
                // to
                // work properly
                argNodes[0] = GetNonSharedNodeGen.create(argNodes[0].asRNode());
            }
            newSyntaxLHS = lookup(lookupLHS.getSourceSection(), symbol + "<-", true);
        } else {
            // data types (and lengths) are verified in isNamespaceLookupCall
            RSyntaxCall callLHS = (RSyntaxCall) syntaxLHS;
            RSyntaxElement[] oldArgs = callLHS.getSyntaxArguments();
            RSyntaxNode[] newArgs = new RSyntaxNode[2];
            newArgs[0] = (RSyntaxNode) oldArgs[0];
            newArgs[1] = lookup(oldArgs[1].getSourceSection(), ((RSyntaxLookup) oldArgs[1]).getIdentifier() + "<-", true);
            newSyntaxLHS = RCallNode.createCall(callLHS.getSourceSection(), ((RSyntaxNode) callLHS.getSyntaxLHS()).asRNode(), callLHS.getSyntaxSignature(), newArgs);
        }
        return RCallNode.createCall(source, newSyntaxLHS.asRNode(), ArgumentsSignature.get(names), argNodes);
    }

    /*
     * Determines if syntax call is of the form foo::bar
     */
    private static boolean isNamespaceLookupCall(RSyntaxElement e) {
        if (e instanceof RSyntaxCall) {
            RSyntaxCall call = (RSyntaxCall) e;
            // check for syntax nodes as this will be required to recreate a call during
            // replacement form construction in createFunctionUpdate
            if (call.getSyntaxLHS() instanceof RSyntaxLookup && call.getSyntaxLHS() instanceof RSyntaxNode) {
                if (((RSyntaxLookup) call.getSyntaxLHS()).getIdentifier().equals("::")) {
                    RSyntaxElement[] args = call.getSyntaxArguments();
                    if (args.length == 2 && args[0] instanceof RSyntaxLookup && args[0] instanceof RSyntaxNode && args[1] instanceof RSyntaxLookup && args[1] instanceof RSyntaxNode) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // used to create unique temp names for nested replacements
    private int tempNamesCount;

    /**
     * This method builds the sequence of calls needed to represent a replacement.<br/>
     * For example, the replacement {@code a(b(c(x),i),j) <- z} should be decomposed into the
     * following statements:
     *
     * <pre>
     * t3 <- x
     * t2 <- c(t3)
     * t1 <- b(t2,i)
     * tt0 <- z
     * tt1 <- `a<-`(t1, j, tt0) // b(...) with a replaced
     * tt2 <- `b<-`(t2, i, tt1) // a(...) with b replaced
     * x <- `c<-`(t3, tt2) // x with c replaced
     * </pre>
     */
    private RSyntaxNode createReplacement(SourceSection source, RSyntaxNode lhs, RSyntaxNode rhs, String operator, boolean isSuper) {
        /*
         * Collect all the function calls in this replacement. For "a(b(x)) <- z", this would be
         * "a(...)" and "b(...)".
         */
        List<RSyntaxCall> calls = new ArrayList<>();
        RSyntaxElement current = lhs;
        while (!(current instanceof RSyntaxLookup)) {
            if (!(current instanceof RSyntaxCall)) {
                throw RError.error(RError.NO_CALLER, RError.Message.NON_LANG_ASSIGNMENT_TARGET);
            }
            RSyntaxCall call = (RSyntaxCall) current;
            calls.add(call);

            if (call.getSyntaxArguments().length == 0 || !(call.getSyntaxLHS() instanceof RSyntaxLookup || isNamespaceLookupCall(call.getSyntaxLHS()))) {
                // TODO: this should only be signaled when run, not when parsed
                throw RInternalError.unimplemented("proper error message for RError.INVALID_LHS");
            }
            current = call.getSyntaxArguments()[0];
        }
        RSyntaxLookup variable = (RSyntaxLookup) current;

        List<RNode> instructions = new ArrayList<>();
        int tempNamesIndex = tempNamesCount;
        tempNamesCount += calls.size() + 1;

        /*
         * Create the calls that extract inner components - only needed for complex replacements
         * like "a(b(x)) <- z" (where we would extract "b(x)").
         */
        for (int i = calls.size() - 1; i >= 1; i--) {
            RCallNode update = createFunctionQuery(ReadVariableNode.create("*tmp*" + (tempNamesIndex + i + 1)), calls.get(i));
            instructions.add(WriteVariableNode.createAnonymous("*tmp*" + (tempNamesIndex + i), update, WriteVariableNode.Mode.INVISIBLE));
        }
        /*
         * Create the update calls, for "a(b(x)) <- z", this would be `a<-` and `b<-`.
         */
        for (int i = 0; i < calls.size(); i++) {
            RCallNode update = createFunctionUpdate(source, ReadVariableNode.create("*tmp*" + (tempNamesIndex + i + 1)), ReadVariableNode.create("*tmpr*" + (tempNamesIndex + i - 1)),
                            calls.get(i));
            if (i < calls.size() - 1) {
                instructions.add(WriteVariableNode.createAnonymous("*tmpr*" + (tempNamesIndex + i), update, WriteVariableNode.Mode.INVISIBLE));
            } else {
                instructions.add(WriteVariableNode.createAnonymous(variable.getIdentifier(), update, WriteVariableNode.Mode.INVISIBLE, isSuper));
            }
        }

        ReadVariableNode variableValue = createReplacementForVariableUsing(variable, isSuper);
        ReplacementNode newReplacementNode = new ReplacementNode(source, operator, lhs, rhs, "*tmpr*" + (tempNamesIndex - 1),
                        variableValue, "*tmp*" + (tempNamesIndex + calls.size()), instructions);

        tempNamesCount -= calls.size() + 1;
        return newReplacementNode;
    }

    private static ReadVariableNode createReplacementForVariableUsing(RSyntaxLookup var, boolean isSuper) {
        if (isSuper) {
            return ReadVariableNode.createSuperLookup(var.getSourceSection(), var.getIdentifier());
        } else {
            return ReadVariableNode.create(var.getSourceSection(), var.getIdentifier(), true);
        }
    }

    public static FastPathFactory createFunctionFastPath(RootCallTarget callTarget) {
        FunctionDefinitionNode def = (FunctionDefinitionNode) callTarget.getRootNode();
        return EvaluatedArgumentsVisitor.process(def.getBody(), def.getSignature());
    }

    @Override
    public RSyntaxNode function(SourceSection source, List<Argument<RSyntaxNode>> params, RSyntaxNode body, RSyntaxNode assignedTo) {
        String description = getFunctionDescription(source, assignedTo);
        RootCallTarget callTarget = rootFunction(source, params, body, description);
        return FunctionExpressionNode.create(source, callTarget);
    }

    @Override
    public RootCallTarget rootFunction(SourceSection source, List<Argument<RSyntaxNode>> params, RSyntaxNode body, String name) {
        // Parse argument list
        RNode[] defaultValues = new RNode[params.size()];
        SaveArgumentsNode saveArguments;
        AccessArgumentNode[] argAccessNodes = new AccessArgumentNode[params.size()];
        SourceSection[] argSourceSections = new SourceSection[params.size()];
        PostProcessArgumentsNode argPostProcess;
        RNode[] init = new RNode[params.size()];
        for (int i = 0; i < params.size(); i++) {
            Argument<RSyntaxNode> arg = params.get(i);
            // Parse argument's default value
            RNode defaultValue;
            if (arg.value != null) {
                // default argument initialization is, in a sense, quite similar to local
                // variable write and thus should do appropriate state transition and/or
                // RShareable copy if need be
                defaultValue = WrapDefaultArgumentNode.create(arg.value.asRNode());
            } else {
                defaultValue = null;
            }

            // Create an initialization statement
            AccessArgumentNode accessArg = AccessArgumentNode.create(i);
            argAccessNodes[i] = accessArg;
            init[i] = WriteVariableNode.createArgSave(arg.name, accessArg);

            // Store formal arguments
            defaultValues[i] = defaultValue;

            argSourceSections[i] = arg.source;
        }

        saveArguments = new SaveArgumentsNode(init);
        if (!params.isEmpty() && true && !FastROptions.RefCountIncrementOnly.getBooleanValue()) {
            argPostProcess = PostProcessArgumentsNode.create(params.size());
        } else {
            argPostProcess = null;
        }

        FormalArguments formals = FormalArguments.createForFunction(defaultValues, createSignature(params));

        for (AccessArgumentNode access : argAccessNodes) {
            access.setFormals(formals);
        }

        FrameDescriptor descriptor = new FrameDescriptor();
        FrameSlotChangeMonitor.initializeFunctionFrameDescriptor(name != null && !name.isEmpty() ? name : "<function>", descriptor);
        FunctionDefinitionNode rootNode = FunctionDefinitionNode.create(source, descriptor, argSourceSections, saveArguments, body, formals, name, argPostProcess);
        return Truffle.getRuntime().createCallTarget(rootNode);
    }

    @Override
    public RSyntaxNode constant(SourceSection source, Object value) {
        if (value instanceof String && !RRuntime.isNA((String) value)) {
            return ConstantNode.create(source, ((String) value).intern());
        } else {
            return ConstantNode.create(source, value);
        }
    }

    private static int getVariadicComponentIndex(String symbol) {
        if (symbol.length() > 2 && symbol.charAt(0) == '.' && symbol.charAt(1) == '.') {
            for (int i = 2; i < symbol.length(); i++) {
                if (symbol.charAt(i) < '\u0030' || symbol.charAt(i) > '\u0039') {
                    return -1;
                }
            }
            return Integer.parseInt(symbol.substring(2));
        }
        return -1;
    }

    @Override
    public RSyntaxNode lookup(SourceSection sourceIn, String symbol, boolean functionLookup) {
        /*
         * TODO Ideally, sourceIn != null always, however ReplacementNodes can cause this on the
         * rewrite nodes.
         */
        SourceSection source = sourceIn == null ? RSyntaxNode.INTERNAL : sourceIn;
        if (!functionLookup && getVariadicComponentIndex(symbol) != -1) {
            int ind = getVariadicComponentIndex(symbol);
            return new ReadVariadicComponentNode(source, ind > 0 ? ind - 1 : ind);
        }
        return functionLookup ? ReadVariableNode.createForcedFunctionLookup(source, symbol) : ReadVariableNode.create(source, symbol, false);
    }
}
