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

import java.util.List;
import java.util.Map;

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
import com.oracle.truffle.r.nodes.control.RepeatNode;
import com.oracle.truffle.r.nodes.control.ReplacementDispatchNode;
import com.oracle.truffle.r.nodes.control.ReplacementDispatchNode.LHSError;
import com.oracle.truffle.r.nodes.control.WhileNode;
import com.oracle.truffle.r.nodes.function.FormalArguments;
import com.oracle.truffle.r.nodes.function.FunctionDefinitionNode;
import com.oracle.truffle.r.nodes.function.FunctionExpressionNode;
import com.oracle.truffle.r.nodes.function.PostProcessArgumentsNode;
import com.oracle.truffle.r.nodes.function.RCallNode;
import com.oracle.truffle.r.nodes.function.RCallSpecialNode;
import com.oracle.truffle.r.nodes.function.SaveArgumentsNode;
import com.oracle.truffle.r.nodes.function.WrapDefaultArgumentNode;
import com.oracle.truffle.r.nodes.function.signature.MissingNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.FastROptions;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.FastPathFactory;
import com.oracle.truffle.r.runtime.data.REmpty;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;
import com.oracle.truffle.r.runtime.nodes.EvaluatedArgumentsVisitor;
import com.oracle.truffle.r.runtime.nodes.RCodeBuilder;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxCall;
import com.oracle.truffle.r.runtime.nodes.RSyntaxConstant;
import com.oracle.truffle.r.runtime.nodes.RSyntaxElement;
import com.oracle.truffle.r.runtime.nodes.RSyntaxLookup;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

/**
 * This class can be used to build fragments of Truffle AST that correspond to R language
 * constructs: calls, lookups, constants and functions.
 *
 * Additionally, this class has helper functions to issue (parser) warnings and
 *
 */
public final class RASTBuilder implements RCodeBuilder<RSyntaxNode> {

    private final Map<String, Object> constants;
    private CodeBuilderContext context = CodeBuilderContext.DEFAULT;

    public RASTBuilder() {
        this.constants = null;
    }

    public RASTBuilder(Map<String, Object> constants) {
        this.constants = constants;
    }

    private RCallNode unused() {
        return null; // we need reference to RCallNode, otherwise it won't compile, compilation bug?
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
                        return RepeatNode.create(source, args.get(0).value);
                    case "(":
                        return args.get(0).value;
                }
            } else if (args.size() == 2) {
                switch (symbol) {
                    case "while":
                        return WhileNode.create(source, args.get(0).value, args.get(1).value);
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
                case "missing":
                    return new MissingNode(source, lhs, createSignature(args), args.stream().map(a -> a.value).toArray(RSyntaxElement[]::new));
            }
        }

        ArgumentsSignature signature = createSignature(args);
        RSyntaxNode[] nodes = args.stream().map(
                        arg -> (arg.value == null && arg.name == null) ? ConstantNode.create(arg.source == null ? RSyntaxNode.SOURCE_UNAVAILABLE : arg.source, REmpty.instance) : arg.value).toArray(
                                        RSyntaxNode[]::new);

        return RCallSpecialNode.createCall(source, lhs.asRNode(), signature, nodes);
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
                if (c.getValue() instanceof String) {
                    name = (String) c.getValue();
                } else {
                    return new LHSError(source, operator, replacementLhs, replacementRhs);
                }
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

    private static String getFunctionDescription(SourceSection source, Object assignedTo) {
        if (assignedTo instanceof String) {
            return (String) assignedTo;
        } else if (assignedTo instanceof RSyntaxLookup) {
            return ((RSyntaxLookup) assignedTo).getIdentifier();
        } else {
            String functionBody = source.getCode();
            return functionBody.substring(0, Math.min(functionBody.length(), 40)).replace("\n", "\\n");
        }
    }

    private RSyntaxNode createReplacement(SourceSection source, RSyntaxNode lhs, RSyntaxNode rhs, String operator, boolean isSuper) {
        return new ReplacementDispatchNode(source, operator, lhs, rhs, isSuper, this.context.getReplacementVarsStartIndex());
    }

    public static FastPathFactory createFunctionFastPath(RSyntaxElement body, ArgumentsSignature signature) {
        return EvaluatedArgumentsVisitor.process(body, signature);
    }

    @Override
    public RSyntaxNode function(SourceSection source, List<Argument<RSyntaxNode>> params, RSyntaxNode body, Object assignedTo) {
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
    public void setContext(CodeBuilderContext context) {
        this.context = context;
    }

    @Override
    public CodeBuilderContext getContext() {
        return context;
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
        if (constants != null && symbol.startsWith("C")) {
            Object object = constants.get(symbol);
            if (object != null) {
                return ConstantNode.create(sourceIn, object);
            }
        }
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
