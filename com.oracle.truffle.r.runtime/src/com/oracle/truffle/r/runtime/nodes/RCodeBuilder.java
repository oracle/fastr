/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.runtime.nodes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.TruffleRLanguage;
import com.oracle.truffle.r.runtime.data.REmpty;
import com.oracle.truffle.r.runtime.data.RNull;

/**
 * Implementers of this interface can be used to generate a representation of an R closure.
 */
public interface RCodeBuilder<T> {

    /**
     * Overrides the last token reported by {@link #token(SourceSection, RCodeToken, String)}.
     */
    void modifyLastToken(RCodeToken newToken);

    /**
     * Overrides the last token reported by {@link #token(SourceSection, RCodeToken, String)}, but
     * only if that token was equal to {@code oldToken}.
     */
    void modifyLastTokenIf(RCodeToken oldToken, RCodeToken newToken);

    /**
     * Reports a new token to the parse metadata builder. Note: this is only used from the R parser
     * in the context of parse builtin invoked with {@code keep.source=T}.
     */
    void token(SourceSection source, RCodeToken token, String text);

    final class Argument<T> {
        public final SourceSection source;
        public final String name;
        public final T value;

        private Argument(SourceSection source, String name, T value) {
            this.source = source;
            this.name = name;
            this.value = value;
        }
    }

    final class CodeBuilderContext {
        public static final CodeBuilderContext DEFAULT = new CodeBuilderContext(0);

        private final int replacementVarsStartIndex;

        public CodeBuilderContext(int replacementVarsStartIndex) {
            this.replacementVarsStartIndex = replacementVarsStartIndex;
        }

        /**
         * Used to initialize {@code ReplacementBlockNode}. When we are processing a replacement AST
         * that is within another replacement, example {@code x[x[1]<-2]<-3}, we set this value so
         * that newly created replacements within the original replacement have different temporary
         * variable names.
         */
        public int getReplacementVarsStartIndex() {
            return replacementVarsStartIndex;
        }
    }

    /**
     * Creates a function call argument.
     */
    static <T> Argument<T> argument(SourceSection source, String name, T expression) {
        return new Argument<>(source, name, expression);
    }

    /**
     * Creates an unnamed function call argument.
     */
    static <T> Argument<T> argument(T expression) {
        return new Argument<>(null, null, expression);
    }

    /**
     * Creates an empty function call argument.
     */
    static <T> Argument<T> argumentEmpty() {
        return new Argument<>(null, null, null);
    }

    /**
     * Create a call with an arbitrary number of named or unnamed arguments.
     */
    T call(SourceSection source, T lhs, List<Argument<T>> arguments, DynamicObject attributes);

    default T call(SourceSection source, T lhs, List<Argument<T>> arguments) {
        return call(source, lhs, arguments, null);
    }

    /**
     * Creates a constant, the value is expected to be one of FastR's scalar types (byte, int,
     * double, RComplex, String, RNull).
     */
    T constant(SourceSection source, Object value);

    T specialLookup(SourceSection source, String symbol, boolean functionLookup);

    /**
     * Creates a symbol lookup, either of mode "function" if {@code functionLookup} is true, and
     * "any" otherwise.
     */
    T lookup(SourceSection source, String symbol, boolean functionLookup);

    /**
     * Creates a function expression literal. {@code assignedTo} can be used to determine function
     * names - if it is non-null, it represents the left hand side that this function is assigned
     * to.
     */
    T function(TruffleRLanguage language, SourceSection source, List<Argument<T>> arguments, T body, Object assignedTo);

    /**
     * Creates a new call target from a given function expression literal.
     */
    RootCallTarget rootFunction(TruffleRLanguage language, SourceSection source, List<Argument<T>> arguments, T body, String name);

    /**
     * Given a {@link com.oracle.truffle.r.runtime.data.RPairList} or {@link RNull}, this method
     * creates a corresponding list of named arguments with default values if any, like if passed to
     * the `function` expression.
     */
    List<Argument<RSyntaxNode>> getFunctionExprArgs(Object args);

    void setContext(CodeBuilderContext context);

    CodeBuilderContext getContext();

    /**
     * This method returns a newly created AST fragment for the given original element. This
     * functionality can be used to quickly create new AST snippets for existing code.
     */
    default T process(RSyntaxElement original) {
        T result = new RSyntaxVisitor<T>() {

            @Override
            protected T visit(RSyntaxCall element) {
                ArrayList<Argument<T>> args = createArguments(element.getSyntaxSignature(), element.getSyntaxArguments());
                return call(element.getLazySourceSection(), accept(element.getSyntaxLHS()), args, element.getAttributes());
            }

            private ArrayList<Argument<T>> createArguments(ArgumentsSignature signature, RSyntaxElement[] arguments) {
                ArrayList<Argument<T>> args = new ArrayList<>(arguments.length);
                for (int i = 0; i < arguments.length; i++) {
                    args.add(RCodeBuilder.argument(arguments[i] == null ? null : arguments[i].getLazySourceSection(), signature.getName(i), arguments[i] == null ? null : accept(arguments[i])));
                }
                return args;
            }

            @Override
            protected T visit(RSyntaxConstant element) {
                return constant(element.getLazySourceSection(), element.getValue());
            }

            @Override
            protected T visit(RSyntaxLookup element) {
                if (element.getIdentifier().isEmpty()) {
                    return constant(element.getLazySourceSection(), REmpty.instance);
                }
                return lookup(element.getLazySourceSection(), element.getIdentifier(), element.isFunctionLookup());
            }

            @Override
            protected T visit(RSyntaxFunction element) {
                ArrayList<Argument<T>> params = createArguments(element.getSyntaxSignature(), element.getSyntaxArgumentDefaults());
                return function(RContext.getInstance().getLanguage(), element.getLazySourceSection(), params, accept(element.getSyntaxBody()), element.getSyntaxDebugName());
            }
        }.accept(original);
        if (result instanceof Node) {
            ((Node) result).adoptChildren();
        }
        return result;
    }

    static <T> ArrayList<Argument<T>> createArgumentList(ArgumentsSignature signature, T[] arguments) {
        ArrayList<Argument<T>> args = new ArrayList<>(arguments.length);
        for (int i = 0; i < arguments.length; i++) {
            args.add(RCodeBuilder.argument(null, signature.getName(i), arguments[i]));
        }
        return args;
    }

    /** @see #process(RSyntaxElement) */
    default T process(RSyntaxElement original, CodeBuilderContext context) {
        CodeBuilderContext saved = getContext();
        setContext(context);
        T result = process(original);
        setContext(saved);
        return result;
    }

    /**
     * Helper function: create a call with no arguments.
     */
    default T call(SourceSection source, T lhs) {
        return call(source, lhs, Collections.emptyList());
    }

    /**
     * Helper function: create a call with one unnamed arguments.
     */
    default T call(SourceSection source, T lhs, T argument1) {
        return call(source, lhs, Arrays.asList(argument(argument1)));
    }

    /**
     * Helper function: create a call with one unnamed arguments.
     */
    default T call(SourceSection source, T lhs, T argument1, DynamicObject attributes) {
        return call(source, lhs, Arrays.asList(argument(argument1)), attributes);
    }

    /**
     * Helper function: create a call with two unnamed arguments.
     */
    default T call(SourceSection source, T lhs, T argument1, T argument2) {
        return call(source, lhs, Arrays.asList(argument(argument1), argument(argument2)));
    }

    /**
     * Helper function: create a call with two unnamed arguments.
     */
    default T call(SourceSection source, T lhs, T argument1, T argument2, DynamicObject attributes) {
        return call(source, lhs, Arrays.asList(argument(argument1), argument(argument2)), attributes);
    }

    /**
     * Helper function: create a call with three unnamed arguments.
     */
    default T call(SourceSection source, T lhs, T argument1, T argument2, T argument3) {
        return call(source, lhs, Arrays.asList(argument(argument1), argument(argument2), argument(argument3)));
    }

    /**
     * Helper function: create a call with four unnamed arguments.
     */
    default T call(SourceSection source, T lhs, T argument1, T argument2, T argument3, T argument4) {
        return call(source, lhs, Arrays.asList(argument(argument1), argument(argument2), argument(argument3), argument(argument4)));
    }

    enum RCodeToken {
        UNKNOWN(-1),
        AND2(286),
        LEFT_ASSIGN(266),
        EQ_ASSIGN(267),
        AT(64, "'@'"),
        SYMBOL(263),
        BREAK(276),
        CARET(94, "'^'"),
        COLON(58, "':'"),
        COMMA(43, "','"),
        COMMENT(290),
        NUM_CONST(261),
        DIV(47, "'/'"),
        AND(284),
        OR(285),
        ELSE(273),
        EQ(294),
        EXPONENT(94, "'^'"),
        FIELD(36, "'$'"),
        FOR(270),
        FUNCTION(264),
        GE(279),
        GT(278),
        IF(272),
        IN(271),
        LBB(269),
        LBRACE(123, "'{'"),
        LBRAKET(91, "'['"),
        LE(281),
        LPAR(40, "'('"),
        LT(280),
        MINUS(45, "'-'"),
        MULT(42, "'*'"),
        NE(283),
        NEXT(275),
        NOT(33, "'!'"),
        NS_GET(288),
        NS_GET_INT(289),
        NULL_CONST(262),
        SPECIAL(304),
        OR2(287),
        PLUS(43, "'+'"),
        QM(63, "'?'"),
        RBRACE(125, "'}'"),
        RBRAKET(93, "']'"),
        REPEAT(277),
        RIGHT_ASSIGN(268),
        RPAR(41, "')'"),
        SEMICOLON(59, "';'"),
        STR_CONST(260),
        TILDE(126, "'~'"),
        WHILE(274),
        // Note: the following tokens are context sensitive and so cannot be recognized only by the
        // lexer
        SYMBOL_SUB(295),            // x[[symbol_sub = 42]]
        EQ_SUB(294),                // ______________^
        SLOT(299),                  // foo@slot
        SYMBOL_FORMALS(293),        // function(bar = 42)
        EQ_FORMALS(293),            // _____________^
        SYMBOL_PACKAGE(297),        // symbol_package::foo OR symbol_package:::foo
        SYMBOL_FUNCTION_CALL(296);  // symbol_function_call(1,2,3)

        private final int code;
        private final String tokenName;

        RCodeToken(int code) {
            this.code = code;
            this.tokenName = name();
        }

        RCodeToken(int code, String tokenName) {
            this.code = code;
            this.tokenName = tokenName;
        }

        public int getCode() {
            return code;
        }

        public String getTokenName() {
            return tokenName;
        }
    }
}
