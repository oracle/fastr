/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.nodes.access.AccessArgumentNode;
import com.oracle.truffle.r.nodes.access.ConstantNode;
import com.oracle.truffle.r.nodes.access.ReadVariadicComponentNode;
import com.oracle.truffle.r.nodes.access.WriteVariableNode;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.builtin.InternalNode;
import com.oracle.truffle.r.nodes.control.BlockNode;
import com.oracle.truffle.r.nodes.control.BreakNode;
import com.oracle.truffle.r.nodes.control.ForNodeGen;
import com.oracle.truffle.r.nodes.control.IfNode;
import com.oracle.truffle.r.nodes.control.NextNode;
import com.oracle.truffle.r.nodes.control.RepeatNode;
import com.oracle.truffle.r.nodes.control.ReplacementDispatchNode;
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
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.builtins.FastPathFactory;
import com.oracle.truffle.r.runtime.context.TruffleRLanguage;
import com.oracle.truffle.r.runtime.data.REmpty;
import com.oracle.truffle.r.runtime.data.RShareable;
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
 */
public final class RASTBuilder implements RCodeBuilder<RSyntaxNode> {

    private CodeBuilderContext context = CodeBuilderContext.DEFAULT;

    @Override
    public RSyntaxNode call(SourceSection source, RSyntaxNode lhs, List<Argument<RSyntaxNode>> args) {
        if (lhs instanceof RSyntaxLookup) {
            RSyntaxLookup lhsLookup = (RSyntaxLookup) lhs;
            String symbol = lhsLookup.getIdentifier();
            if (args.size() == 0) {
                switch (symbol) {
                    case "break":
                        return new BreakNode(source, lhsLookup);
                    case "next":
                        return new NextNode(source, lhsLookup);
                }
            } else if (args.size() == 1) {
                switch (symbol) {
                    case "repeat":
                        return new RepeatNode(source, lhsLookup, args.get(0).value);
                }
            } else if (args.size() == 2) {
                switch (symbol) {
                    case "$":
                    case "@":
                        convertSymbol(args);
                        break;
                    case "while":
                        return new WhileNode(source, lhsLookup, args.get(0).value, args.get(1).value);
                    case "if":
                        return new IfNode(source, lhsLookup, args.get(0).value, args.get(1).value, null);
                    case "=":
                    case "<-":
                    case ":=":
                    case "<<-":
                    case "->":
                    case "->>":
                        boolean isSuper = "<<-".equals(symbol) || "->>".equals(symbol);
                        boolean switchArgs = "->".equals(symbol) || "->>".equals(symbol);
                        // fix the operators while keeping the correct source sections
                        if ("->>".equals(symbol)) {
                            lhsLookup = (RSyntaxLookup) ReadVariableNode.wrap(lhs.getLazySourceSection(), ReadVariableNode.createForcedFunctionLookup("<<-"));
                        } else if ("->".equals(symbol)) {
                            lhsLookup = (RSyntaxLookup) ReadVariableNode.wrap(lhs.getLazySourceSection(), ReadVariableNode.createForcedFunctionLookup("<-"));
                        }
                        // switch the args if needed
                        RSyntaxNode lhsArg = args.get(switchArgs ? 1 : 0).value;
                        RSyntaxNode rhsArg = args.get(switchArgs ? 0 : 1).value;
                        return new ReplacementDispatchNode(source, lhsLookup, lhsArg, rhsArg, isSuper, context.getReplacementVarsStartIndex());
                }
            } else if (args.size() == 3) {
                switch (symbol) {
                    case "for":
                        if (args.get(0).value instanceof RSyntaxLookup) {
                            RSyntaxLookup var = (RSyntaxLookup) args.get(0).value;
                            return ForNodeGen.create(source, lhsLookup, var, args.get(2).value.asRNode(), args.get(1).value.asRNode());
                        }
                        break;
                    case "if":
                        return new IfNode(source, lhsLookup, args.get(0).value, args.get(1).value, args.get(2).value);
                }
            }
            switch (symbol) {
                case "{":
                    return new BlockNode(source, lhsLookup, args.stream().map(n -> n.value.asRNode()).toArray(RNode[]::new));
                case "missing":
                    return new MissingNode(source, lhsLookup, createSignature(args), args.stream().map(a -> a.value).toArray(RSyntaxElement[]::new));
                case ".Internal":
                    return InternalNode.create(source, lhsLookup, createSignature(args), args.stream().map(a -> a.value).toArray(RSyntaxNode[]::new));
            }
        }

        if (canBeForeignInvoke(lhs)) {
            return RCallNode.createCallDeferred(source, lhs.asRNode(), createSignature(args), createArguments(args));
        }
        return RCallSpecialNode.createCall(source, lhs.asRNode(), createSignature(args), createArguments(args));
    }

    /**
     * Tests if some syntax expression can be a call in form of {@code lhsReceiver$lhsMember(args)}.
     */
    private static boolean canBeForeignInvoke(RSyntaxNode expr) {

        if (expr instanceof RSyntaxCall) {
            RSyntaxCall call = (RSyntaxCall) expr;
            RSyntaxElement lhs = call.getSyntaxLHS();

            if (lhs instanceof RSyntaxLookup && "$".equals(((RSyntaxLookup) lhs).getIdentifier())) {
                RSyntaxElement[] syntaxArguments = call.getSyntaxArguments();
                return syntaxArguments.length == 2 && isAllowedElement(syntaxArguments[0]) && isAllowedElement(syntaxArguments[1]);
            }
        }

        return false;
    }

    private void convertSymbol(List<Argument<RSyntaxNode>> args) {
        if (args.get(1).value instanceof RSyntaxLookup) {
            RSyntaxLookup lookup = (RSyntaxLookup) args.get(1).value;
            // FastR differs from GNUR: we only use string constants to represent
            // field and slot lookups, while GNUR uses symbols
            args.set(1, RCodeBuilder.argument(args.get(1).source, args.get(1).name, constant(lookup.getLazySourceSection(), lookup.getIdentifier())));
        }
    }

    private static ArgumentsSignature createSignature(List<Argument<RSyntaxNode>> args) {
        String[] argumentNames = args.stream().map(arg -> arg.name).toArray(String[]::new);
        ArgumentsSignature signature = ArgumentsSignature.get(argumentNames);
        return signature;
    }

    private static RSyntaxNode[] createArguments(List<Argument<RSyntaxNode>> args) {
        RSyntaxNode[] nodes = new RSyntaxNode[args.size()];
        for (int i = 0; i < nodes.length; i++) {
            Argument<RSyntaxNode> arg = args.get(i);
            nodes[i] = (arg.value == null && arg.name == null) ? ConstantNode.create(arg.source == null ? RSyntaxNode.SOURCE_UNAVAILABLE : arg.source, REmpty.instance) : arg.value;
        }
        return nodes;
    }

    private static String getFunctionDescription(SourceSection source, Object assignedTo) {
        if (assignedTo instanceof String) {
            return (String) assignedTo;
        } else if (assignedTo instanceof RSyntaxLookup) {
            return ((RSyntaxLookup) assignedTo).getIdentifier();
        } else {
            CharSequence functionBody = source.getCharacters();
            return functionBody.subSequence(0, Math.min(functionBody.length(), 40)).toString().replace("\n", "\\n");
        }
    }

    public static FastPathFactory createFunctionFastPath(RSyntaxElement body, ArgumentsSignature signature) {
        return EvaluatedArgumentsVisitor.process(body, signature);
    }

    @Override
    public RSyntaxNode function(TruffleRLanguage language, SourceSection source, List<Argument<RSyntaxNode>> params, RSyntaxNode body, Object assignedTo) {
        String description = getFunctionDescription(source, assignedTo);
        RootCallTarget callTarget = rootFunction(language, source, params, body, description);
        return FunctionExpressionNode.create(source, callTarget);
    }

    @Override
    public RootCallTarget rootFunction(TruffleRLanguage language, SourceSection source, List<Argument<RSyntaxNode>> params, RSyntaxNode body, String name) {
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
        FunctionDefinitionNode rootNode = FunctionDefinitionNode.create(language, source, descriptor, argSourceSections, saveArguments, body, formals, name, argPostProcess);

        if (FastROptions.ForceSources.getBooleanValue()) {
            // forces source sections to be generated
            rootNode.getSourceSection();
        }
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
            return ConstantNode.create(source, Utils.intern((String) value));
        } else {
            if (value instanceof RShareable) {
                RShareable shareable = (RShareable) value;
                if (!shareable.isSharedPermanent()) {
                    return ConstantNode.create(source, shareable.makeSharedPermanent());
                }
            }
            return ConstantNode.create(source, value);
        }
    }

    @Override
    public RSyntaxNode lookup(SourceSection source, String symbol, boolean functionLookup) {
        assert source != null;
        if (!functionLookup) {
            int index = RSyntaxLookup.getVariadicComponentIndex(symbol);
            if (index != -1) {
                return new ReadVariadicComponentNode(source, index > 0 ? index - 1 : index);
            }
        }
        return ReadVariableNode.wrap(source, functionLookup ? ReadVariableNode.createForcedFunctionLookup(symbol) : ReadVariableNode.create(symbol));
    }

    private static boolean isAllowedElement(RSyntaxElement e) {
        return e instanceof RSyntaxLookup || e instanceof RSyntaxConstant;
    }
}
