/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test.tck;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.tck.Snippet;
import org.graalvm.polyglot.tck.TypeDescriptor;
import org.junit.Assert;
import org.graalvm.polyglot.tck.LanguageProvider;
import org.graalvm.polyglot.tck.ResultVerifier;

public final class RTCKLanguageProvider implements LanguageProvider {

    private static final String ID = "R";
    private static final String PATTERN_VALUE_FNC = "function () {\n" +
                    "%s\n" +
                    "}";
    private static final String PATTERN_BIN_OP_FNC = "function(a,b) {\n" +
                    "a %s b\n" +
                    "}";
    private static final String PATTERN_PREFIX_OP_FNC = "function(a) {\n" +
                    "%s a\n" +
                    "}";
    private static final String[] PATTERN_STATEMENT = {
                    "function() {\n" +
                                    "r <- NULL\n" +
                                    "%s\n" +
                                    "r\n" +
                                    "}",
                    "function(p1) {\n" +
                                    "r <- NULL\n" +
                                    "%s\n" +
                                    "r\n" +
                                    "}",
                    "function(p1, p2) {\n" +
                                    "r <- NULL\n" +
                                    "%s\n" +
                                    "r\n" +
                                    "}"
    };

    public RTCKLanguageProvider() {
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Value createIdentityFunction(Context context) {
        return eval(context, "function(a) {\n" +
                        "a\n" +
                        "}\n");
    }

    @Override
    public Collection<? extends Snippet> createValueConstructors(Context context) {
        List<Snippet> vals = new ArrayList<>();

        // Scalar types
        vals.add(createValueConstructor(context, "1L", TypeDescriptor.NUMBER));
        vals.add(createValueConstructor(context, "1.42", TypeDescriptor.NUMBER));
        vals.add(createValueConstructor(context, "FALSE", TypeDescriptor.BOOLEAN));
        vals.add(createValueConstructor(context, "'TEST'", TypeDescriptor.STRING));
        vals.add(createValueConstructor(context, "1+1i", TypeDescriptor.intersection())); // generic
                                                                                          // type

        // TODO NULL, raw, s4, env, list, empty, ...
        // vals.add(createValueConstructor(context, "NULL", TypeDescriptor.NULL));

        // Vectors & Lists
        Snippet v = createValueConstructor(context, "c(1L:10L)", TypeDescriptor.array(TypeDescriptor.NUMBER));
        vals.add(v);

        v = createValueConstructor(context, "c(1:10)", TypeDescriptor.array(TypeDescriptor.NUMBER));
        vals.add(v);

        vals.add(createValueConstructor(context, "c(TRUE, FALSE)", TypeDescriptor.array(TypeDescriptor.BOOLEAN)));
        vals.add(createValueConstructor(context, "c(1L, 'STRING')", TypeDescriptor.array(TypeDescriptor.STRING)));
        return Collections.unmodifiableList(vals);
    }

    @Override
    public Collection<? extends Snippet> createExpressions(Context context) {
        List<Snippet> ops = new ArrayList<>();
        TypeDescriptor numOrBool = TypeDescriptor.union(TypeDescriptor.NUMBER, TypeDescriptor.BOOLEAN);
        TypeDescriptor numOrBoolOrNull = TypeDescriptor.union(numOrBool, TypeDescriptor.NULL);
        TypeDescriptor arrNumBool = TypeDescriptor.array(numOrBool);
        TypeDescriptor numOrBoolOrArrNumBool = TypeDescriptor.union(numOrBool, arrNumBool);
        TypeDescriptor numOrBoolOrNullOrArrNumBool = TypeDescriptor.union(numOrBoolOrNull, arrNumBool);
        TypeDescriptor boolOrArrBool = TypeDescriptor.union(TypeDescriptor.BOOLEAN, TypeDescriptor.array(TypeDescriptor.BOOLEAN));
        TypeDescriptor strOrNumOrBool = TypeDescriptor.union(TypeDescriptor.STRING, numOrBool);
        TypeDescriptor arrStrNumBool = TypeDescriptor.array(strOrNumOrBool);
        TypeDescriptor strOrNumOrBoolOrArrStrNumBool = TypeDescriptor.union(strOrNumOrBool, arrStrNumBool);

        // +
        ops.add(createBinaryOperator(context, "+", numOrBoolOrArrNumBool, numOrBoolOrNullOrArrNumBool, numOrBoolOrNullOrArrNumBool,
                        RResultVerifier.newBuilder(numOrBoolOrNullOrArrNumBool, numOrBoolOrNullOrArrNumBool).emptyArrayCheck().build()));
        // -
        ops.add(createBinaryOperator(context, "-", numOrBoolOrArrNumBool, numOrBoolOrNullOrArrNumBool, numOrBoolOrNullOrArrNumBool,
                        RResultVerifier.newBuilder(numOrBoolOrNullOrArrNumBool, numOrBoolOrNullOrArrNumBool).emptyArrayCheck().build()));
        // *
        ops.add(createBinaryOperator(context, "*", numOrBoolOrArrNumBool, numOrBoolOrNullOrArrNumBool, numOrBoolOrNullOrArrNumBool,
                        RResultVerifier.newBuilder(numOrBoolOrNullOrArrNumBool, numOrBoolOrNullOrArrNumBool).emptyArrayCheck().build()));
        // /
        ops.add(createBinaryOperator(context, "/", numOrBoolOrArrNumBool, numOrBoolOrNullOrArrNumBool, numOrBoolOrNullOrArrNumBool,
                        RResultVerifier.newBuilder(numOrBoolOrNullOrArrNumBool, numOrBoolOrNullOrArrNumBool).emptyArrayCheck().build()));

        // <
        ops.add(createBinaryOperator(context, "<", boolOrArrBool, strOrNumOrBoolOrArrStrNumBool, strOrNumOrBoolOrArrStrNumBool,
                        RResultVerifier.newBuilder(strOrNumOrBoolOrArrStrNumBool, strOrNumOrBoolOrArrStrNumBool).mixedArraysCheck().emptyArrayCheck().build()));
        // >
        ops.add(createBinaryOperator(context, ">", boolOrArrBool, strOrNumOrBoolOrArrStrNumBool, strOrNumOrBoolOrArrStrNumBool,
                        RResultVerifier.newBuilder(strOrNumOrBoolOrArrStrNumBool, strOrNumOrBoolOrArrStrNumBool).mixedArraysCheck().emptyArrayCheck().build()));
        // <=
        ops.add(createBinaryOperator(context, "<=", boolOrArrBool, strOrNumOrBoolOrArrStrNumBool, strOrNumOrBoolOrArrStrNumBool,
                        RResultVerifier.newBuilder(strOrNumOrBoolOrArrStrNumBool, strOrNumOrBoolOrArrStrNumBool).mixedArraysCheck().emptyArrayCheck().build()));
        // >=
        ops.add(createBinaryOperator(context, ">=", boolOrArrBool, strOrNumOrBoolOrArrStrNumBool, strOrNumOrBoolOrArrStrNumBool,
                        RResultVerifier.newBuilder(strOrNumOrBoolOrArrStrNumBool, strOrNumOrBoolOrArrStrNumBool).mixedArraysCheck().emptyArrayCheck().build()));
        // ==
        ops.add(createBinaryOperator(context, "==", boolOrArrBool, strOrNumOrBoolOrArrStrNumBool, strOrNumOrBoolOrArrStrNumBool,
                        RResultVerifier.newBuilder(strOrNumOrBoolOrArrStrNumBool, strOrNumOrBoolOrArrStrNumBool).mixedArraysCheck().emptyArrayCheck().build()));
        // !=
        ops.add(createBinaryOperator(context, "!=", boolOrArrBool, strOrNumOrBoolOrArrStrNumBool, strOrNumOrBoolOrArrStrNumBool,
                        RResultVerifier.newBuilder(strOrNumOrBoolOrArrStrNumBool, strOrNumOrBoolOrArrStrNumBool).mixedArraysCheck().emptyArrayCheck().build()));
        // // TODO &, |, &&, ||

        // !
        ops.add(createPrefixOperator(context, "!", boolOrArrBool, numOrBoolOrArrNumBool, null));

        // TODO unary +, -, ...

        return Collections.unmodifiableList(ops);
    }

    @Override
    public Collection<? extends Snippet> createStatements(Context context) {
        Collection<Snippet> res = new ArrayList<>();
        TypeDescriptor numberOrBoolean = TypeDescriptor.union(TypeDescriptor.NUMBER, TypeDescriptor.BOOLEAN);
        TypeDescriptor arrayNumberBoolean = TypeDescriptor.array(numberOrBoolean);
        TypeDescriptor numberOrBooleanOrArrayNumberBoolean = TypeDescriptor.union(numberOrBoolean, arrayNumberBoolean);

        // if
        String ifStatement = "if ({1}) '{'\n{0}<-TRUE\n'}' else '{'\n{0}<-FALSE\n'}'";
        res.add(createStatement(context, "if", ifStatement,
                        RResultVerifier.newBuilder(numberOrBooleanOrArrayNumberBoolean).emptyArrayCheck().build(),
                        TypeDescriptor.BOOLEAN, numberOrBooleanOrArrayNumberBoolean));

        // ifelse
        String ifelseStatement = "ifelse ({1}, TRUE, FALSE)";
        res.add(createStatement(context, "ifelse", ifelseStatement, TypeDescriptor.NULL,
                        TypeDescriptor.union(
                                        TypeDescriptor.BOOLEAN,
                                        TypeDescriptor.NUMBER,
                                        TypeDescriptor.STRING,
                                        TypeDescriptor.ARRAY)));

        // while
        String whileStatement = "while ({1})'{'\nbreak\n'}'";
        res.add(createStatement(context, "while", whileStatement,
                        RResultVerifier.newBuilder(numberOrBooleanOrArrayNumberBoolean).emptyArrayCheck().build(),
                        TypeDescriptor.NULL, numberOrBooleanOrArrayNumberBoolean));

        // for
        String forStatement = "for (val in {1}) '{'\n'}'";
        res.add(createStatement(context, "for", forStatement, TypeDescriptor.NULL, TypeDescriptor.ANY));

        return Collections.unmodifiableCollection(res);
    }

    @Override
    public Collection<? extends Snippet> createScripts(Context context) {
        List<Snippet> res = new ArrayList<>();
        res.add(loadScript(
                        context,
                        "resources/quicksort.R",
                        TypeDescriptor.BOOLEAN,
                        (snippetRun) -> {
                            Assert.assertEquals(true, snippetRun.getResult().asBoolean());
                        }));
        res.add(loadScript(
                        context,
                        "resources/mandel.R",
                        TypeDescriptor.NUMBER,
                        (snippetRun) -> {
                            Assert.assertEquals(14791, snippetRun.getResult().asInt());
                        }));
        res.add(loadScript(
                        context,
                        "resources/rand_mat_mul.R",
                        TypeDescriptor.BOOLEAN,
                        (snippetRun) -> {
                            Assert.assertEquals(true, snippetRun.getResult().asBoolean());
                        }));
        res.add(loadScript(
                        context,
                        "resources/rand_mat_stat.R",
                        TypeDescriptor.BOOLEAN,
                        (snippetRun) -> {
                            Assert.assertEquals(true, snippetRun.getResult().asBoolean());
                        }));
        res.add(loadScript(
                        context,
                        "resources/pi_sum.R",
                        TypeDescriptor.BOOLEAN,
                        (snippetRun) -> {
                            Assert.assertEquals(true, snippetRun.getResult().asBoolean());
                        }));
        res.add(loadScript(
                        context,
                        "resources/fib.R",
                        TypeDescriptor.NUMBER,
                        (snippetRun) -> {
                            Assert.assertEquals(6765, snippetRun.getResult().asInt());
                        }));
        return Collections.unmodifiableList(res);
    }

    @Override
    public Collection<? extends Source> createInvalidSyntaxScripts(Context context) {
        try {
            List<Source> res = new ArrayList<>();
            res.add(createSource("resources/invalidSyntax01.R"));
            return Collections.unmodifiableList(res);
        } catch (IOException ioe) {
            throw new AssertionError("IOException while creating a test script.", ioe);
        }
    }

    private Snippet createValueConstructor(
                    Context context,
                    String value,
                    TypeDescriptor type) {
        return Snippet.newBuilder(value, eval(context, String.format(PATTERN_VALUE_FNC, value)), type).build();
    }

    private Snippet createBinaryOperator(
                    Context context,
                    String operator,
                    TypeDescriptor type,
                    TypeDescriptor ltype,
                    TypeDescriptor rtype,
                    ResultVerifier verifier) {
        Value fnc = eval(context, String.format(PATTERN_BIN_OP_FNC, operator));
        Snippet.Builder opb = Snippet.newBuilder(operator, fnc, type).parameterTypes(ltype, rtype).resultVerifier(verifier);
        return opb.build();
    }

    private Snippet createPrefixOperator(
                    Context context,
                    String operator,
                    TypeDescriptor type,
                    TypeDescriptor rtype,
                    ResultVerifier verifier) {
        Value fnc = eval(context, String.format(PATTERN_PREFIX_OP_FNC, operator));
        Snippet.Builder opb = Snippet.newBuilder(operator, fnc, type).parameterTypes(rtype).resultVerifier(verifier);
        return opb.build();
    }

    private Snippet createStatement(
                    Context context,
                    String name,
                    String expression,
                    TypeDescriptor type,
                    TypeDescriptor... paramTypes) {
        return createStatement(context, name, expression, null, type, paramTypes);
    }

    private Snippet createStatement(
                    Context context,
                    String name,
                    String expression,
                    ResultVerifier verifier,
                    TypeDescriptor type,
                    TypeDescriptor... paramTypes) {
        String fncFormat = PATTERN_STATEMENT[paramTypes.length];
        Object[] formalParams = new String[paramTypes.length + 1];
        formalParams[0] = "r";
        for (int i = 1; i < formalParams.length; i++) {
            formalParams[i] = "p" + i;
        }
        String exprWithFormalParams = MessageFormat.format(expression, formalParams);
        Value fnc = eval(context, String.format(fncFormat, exprWithFormalParams));
        Snippet.Builder opb = Snippet.newBuilder(name, fnc, type).parameterTypes(paramTypes).resultVerifier(verifier);
        return opb.build();
    }

    private Snippet loadScript(
                    Context context,
                    String resourceName,
                    TypeDescriptor type,
                    ResultVerifier verifier) {
        try {
            Source src = createSource(resourceName);
            return Snippet.newBuilder(src.getName(), context.eval(src), type).resultVerifier(verifier).build();
        } catch (IOException ioe) {
            throw new AssertionError("IOException while creating a test script.", ioe);
        }
    }

    private static Source createSource(String resourceName) throws IOException {
        int slashIndex = resourceName.lastIndexOf('/');
        String scriptName = slashIndex >= 0 ? resourceName.substring(slashIndex + 1) : resourceName;
        Reader in = new InputStreamReader(RTCKLanguageProvider.class.getResourceAsStream(resourceName), "UTF-8");
        return Source.newBuilder(ID, in, scriptName).build();
    }

    private Value eval(Context context, String statement) {
        return context.eval(ID, statement);
    }

    private static final class RResultVerifier implements ResultVerifier {
        private TypeDescriptor[] expectedParameterTypes;
        BiFunction<Boolean, SnippetRun, Void> next;

        private RResultVerifier(
                        TypeDescriptor[] expectedParameterTypes,
                        BiFunction<Boolean, SnippetRun, Void> next) {
            this.expectedParameterTypes = Objects.requireNonNull(expectedParameterTypes, "The expectedParameterTypes cannot be null.");
            this.next = Objects.requireNonNull(next, "The verifier chain cannot be null.");
        }

        @Override
        public void accept(SnippetRun snippetRun) throws PolyglotException {
            next.apply(hasValidArgumentTypes(snippetRun.getParameters()), snippetRun);
        }

        private boolean hasValidArgumentTypes(List<? extends Value> args) {
            for (int i = 0; i < expectedParameterTypes.length; i++) {
                if (!expectedParameterTypes[i].isAssignable(TypeDescriptor.forValue(args.get(i)))) {
                    return false;
                }
            }
            return true;
        }

        static Builder newBuilder(TypeDescriptor... expectedParameterTypes) {
            return new Builder(expectedParameterTypes);
        }

        static final class Builder {
            private final TypeDescriptor[] expectedParameterTypes;
            private BiFunction<Boolean, SnippetRun, Void> chain;

            private Builder(TypeDescriptor[] expectedParameterTypes) {
                this.expectedParameterTypes = expectedParameterTypes;
                chain = (valid, snippetRun) -> {
                    ResultVerifier.getDefaultResultVerfier().accept(snippetRun);
                    return null;
                };
            }

            /**
             * Enables result verifier to handle empty arrays. Use this for R expressions,
             * statements which accept array but not an empty array
             * 
             * @return the Builder
             */
            Builder emptyArrayCheck() {
                chain = new BiFunction<Boolean, SnippetRun, Void>() {
                    private final BiFunction<Boolean, SnippetRun, Void> next = chain;

                    @Override
                    public Void apply(Boolean valid, SnippetRun sr) {
                        if (valid && sr.getException() != null && hasEmptyArrayArg(sr.getParameters())) {
                            return null;
                        }
                        return next.apply(valid, sr);
                    }

                    private boolean hasEmptyArrayArg(List<? extends Value> args) {
                        for (Value arg : args) {
                            if (arg.hasArrayElements() && arg.getArraySize() == 0) {
                                return true;
                            }
                        }
                        return false;
                    }
                };
                return this;
            }

            // Todo: Is it R bug or should verifier handle this?
            // [1,"TEST"] < [1,2] works
            // [1,"TEST"] < [1,"TEST"] fails
            Builder mixedArraysCheck() {
                chain = new BiFunction<Boolean, SnippetRun, Void>() {
                    private final BiFunction<Boolean, SnippetRun, Void> next = chain;

                    @Override
                    public Void apply(Boolean valid, SnippetRun sr) {
                        if (valid && sr.getException() != null && areMixedArrays(sr.getParameters())) {
                            return null;
                        }
                        return next.apply(valid, sr);
                    }

                    private boolean areMixedArrays(List<? extends Value> args) {
                        for (Value arg : args) {
                            if (!arg.hasArrayElements()) {
                                return false;
                            }
                            boolean str = false;
                            boolean num = false;
                            for (int i = 0; i < arg.getArraySize(); i++) {
                                TypeDescriptor td = TypeDescriptor.forValue(arg.getArrayElement(i));
                                str |= TypeDescriptor.STRING.isAssignable(td);
                                num |= TypeDescriptor.NUMBER.isAssignable(td) || TypeDescriptor.BOOLEAN.isAssignable(td);
                            }
                            if ((!str & !num) || (str ^ num)) {
                                return false;
                            }
                        }
                        return !args.isEmpty();
                    }
                };
                return this;
            }

            RResultVerifier build() {
                return new RResultVerifier(expectedParameterTypes, chain);
            }
        }
    }
}
