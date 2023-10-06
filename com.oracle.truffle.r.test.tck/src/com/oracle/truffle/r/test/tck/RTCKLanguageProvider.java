/*
 * Copyright (c) 2017, 2023, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test.tck;

import static org.graalvm.polyglot.tck.TypeDescriptor.OBJECT;
import static org.graalvm.polyglot.tck.TypeDescriptor.array;
import static org.graalvm.polyglot.tck.TypeDescriptor.intersection;

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
import java.util.function.Function;
import java.util.function.Predicate;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.SourceSection;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.tck.InlineSnippet;
import org.graalvm.polyglot.tck.LanguageProvider;
import org.graalvm.polyglot.tck.ResultVerifier;
import org.graalvm.polyglot.tck.Snippet;
import org.graalvm.polyglot.tck.TypeDescriptor;
import org.junit.Assert;

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
    public Snippet createIdentityFunctionSnippet(Context context) {
        // TODO HOTFIX
        // FastR should not converts foreign values already
        // at the moment when they are passed to a function
        Value value = createIdentityFunction(context);
        return (Snippet.newBuilder("identity", value, TypeDescriptor.ANY).parameterTypes(TypeDescriptor.ANY).resultVerifier(new IdentityFunctionResultVerifier()).build());
    }

    @Override
    public Collection<? extends Snippet> createValueConstructors(Context context) {
        List<Snippet> vals = new ArrayList<>();

        // Scalar types
        vals.add(createValueConstructor(context, "1L", intersection(TypeDescriptor.NUMBER, array(TypeDescriptor.NUMBER))));
        vals.add(createValueConstructor(context, "1.42", intersection(TypeDescriptor.NUMBER, array(TypeDescriptor.NUMBER))));
        vals.add(createValueConstructor(context, "FALSE", intersection(TypeDescriptor.BOOLEAN, array(TypeDescriptor.BOOLEAN))));
        vals.add(createValueConstructor(context, "'TEST'", intersection(TypeDescriptor.STRING, array(TypeDescriptor.STRING))));
        vals.add(createValueConstructor(context, "1+1i", intersection(OBJECT, array(OBJECT))));

        // TODO NULL, raw, s4, env, list, empty, ...
        vals.add(createValueConstructor(context, "NULL", TypeDescriptor.NULL));

        // Vectors & Lists
        Snippet v = createValueConstructor(context, "c(1L:10L)", TypeDescriptor.array(TypeDescriptor.NUMBER));
        vals.add(v);

        v = createValueConstructor(context, "c(1:10)", TypeDescriptor.array(TypeDescriptor.NUMBER));
        vals.add(v);

        vals.add(createValueConstructor(context, "c(TRUE, FALSE)", TypeDescriptor.array(TypeDescriptor.BOOLEAN)));
        vals.add(createValueConstructor(context, "c(1L, 'STRING')", TypeDescriptor.array(TypeDescriptor.STRING)));
        // vals.add(createValueConstructor(context, "c(1L, NULL)",
        // TypeDescriptor.array(TypeDescriptor.OBJECT)));
        return Collections.unmodifiableList(vals);
    }

    @Override
    public Collection<? extends Snippet> createExpressions(Context context) {
        List<Snippet> ops = new ArrayList<>();
        TypeDescriptor numOrBool = TypeDescriptor.union(TypeDescriptor.NUMBER, TypeDescriptor.BOOLEAN);
        TypeDescriptor numOrBoolOrNull = TypeDescriptor.union(numOrBool, TypeDescriptor.NULL);
        TypeDescriptor numOrBoolOrArray = TypeDescriptor.union(numOrBool, TypeDescriptor.ARRAY);
        TypeDescriptor numOrBoolOrArrayPrNull = TypeDescriptor.union(numOrBoolOrArray, TypeDescriptor.NULL);
        TypeDescriptor arrNumBool = TypeDescriptor.array(numOrBool);
        TypeDescriptor numOrBoolOrArrNumBool = TypeDescriptor.union(numOrBool, arrNumBool);
        TypeDescriptor numOrBoolOrNullOrArrNumBool = TypeDescriptor.union(numOrBoolOrNull, arrNumBool);
        TypeDescriptor boolOrArrBool = TypeDescriptor.union(TypeDescriptor.BOOLEAN, TypeDescriptor.array(TypeDescriptor.BOOLEAN));

        TypeDescriptor[] acceptedParameterTypes = new TypeDescriptor[]{numOrBoolOrNullOrArrNumBool, numOrBoolOrNullOrArrNumBool};
        TypeDescriptor[] declaredParameterTypes = new TypeDescriptor[]{numOrBoolOrArrayPrNull, numOrBoolOrArrayPrNull};

        var binaryOpsVerifier = new NonPrimitiveNumberParameterThrows(true,
                        RResultVerifier.newBuilder(acceptedParameterTypes, declaredParameterTypes).ignoreBigInts().primitiveAndArrayMismatchCheck().emptyArrayCheck().build());
        // +
        ops.add(createBinaryOperator(context, "+", numOrBoolOrArrNumBool, TypeDescriptor.ANY, TypeDescriptor.ANY, binaryOpsVerifier));
        // -
        ops.add(createBinaryOperator(context, "-", numOrBoolOrArrNumBool, TypeDescriptor.ANY, TypeDescriptor.ANY, binaryOpsVerifier));
        // *
        ops.add(createBinaryOperator(context, "*", numOrBoolOrArrNumBool, TypeDescriptor.ANY, TypeDescriptor.ANY, binaryOpsVerifier));
        // /
        ops.add(createBinaryOperator(context, "/", numOrBoolOrArrNumBool, TypeDescriptor.ANY, TypeDescriptor.ANY, binaryOpsVerifier));

        acceptedParameterTypes = new TypeDescriptor[]{TypeDescriptor.ANY, TypeDescriptor.ANY};
        // <
        ops.add(createBinaryOperator(context, "<", boolOrArrBool, TypeDescriptor.ANY, TypeDescriptor.ANY,
                        new NonPrimitiveNumberParameterThrows(RResultVerifier.newBuilder(acceptedParameterTypes).primitiveAndArrayMismatchCheck().compareParametersCheck().build())));
        // >
        ops.add(createBinaryOperator(context, ">", boolOrArrBool, TypeDescriptor.ANY, TypeDescriptor.ANY,
                        new NonPrimitiveNumberParameterThrows(RResultVerifier.newBuilder(acceptedParameterTypes).primitiveAndArrayMismatchCheck().compareParametersCheck().build())));
        // <=
        ops.add(createBinaryOperator(context, "<=", boolOrArrBool, TypeDescriptor.ANY, TypeDescriptor.ANY,
                        new NonPrimitiveNumberParameterThrows(RResultVerifier.newBuilder(acceptedParameterTypes).primitiveAndArrayMismatchCheck().compareParametersCheck().build())));
        // >=
        ops.add(createBinaryOperator(context, ">=", boolOrArrBool, TypeDescriptor.ANY, TypeDescriptor.ANY,
                        new NonPrimitiveNumberParameterThrows(RResultVerifier.newBuilder(acceptedParameterTypes).primitiveAndArrayMismatchCheck().compareParametersCheck().build())));
        // ==
        ops.add(createBinaryOperator(context, "==", boolOrArrBool, TypeDescriptor.ANY, TypeDescriptor.ANY,
                        new NonPrimitiveNumberParameterThrows(RResultVerifier.newBuilder(acceptedParameterTypes).primitiveAndArrayMismatchCheck().compareParametersCheck().build())));
        // !=
        ops.add(createBinaryOperator(context, "!=", boolOrArrBool, TypeDescriptor.ANY, TypeDescriptor.ANY,
                        new NonPrimitiveNumberParameterThrows(RResultVerifier.newBuilder(acceptedParameterTypes).primitiveAndArrayMismatchCheck().compareParametersCheck().build())));
        // // TODO &, |, &&, ||

        // !
        ops.add(createPrefixOperator(context, "!", boolOrArrBool, numOrBoolOrArray,
                        new NonPrimitiveNumberParameterThrows(RResultVerifier.newBuilder(new TypeDescriptor[]{numOrBoolOrNullOrArrNumBool},
                                        new TypeDescriptor[]{numOrBoolOrArray}).primitiveAndArrayMismatchCheck().emptyArrayCheck().build())));

        // TODO unary +, -, ...

        return Collections.unmodifiableList(ops);
    }

    @Override
    public Collection<? extends Snippet> createStatements(Context context) {
        Collection<Snippet> res = new ArrayList<>();
        TypeDescriptor numberOrBoolean = TypeDescriptor.union(TypeDescriptor.NUMBER, TypeDescriptor.BOOLEAN);
        TypeDescriptor arrayNumberBoolean = TypeDescriptor.array(numberOrBoolean);
        TypeDescriptor numOrBoolOrArray = TypeDescriptor.union(numberOrBoolean, TypeDescriptor.ARRAY);
        TypeDescriptor numberOrBooleanOrArrayNumberBoolean = TypeDescriptor.union(numberOrBoolean, arrayNumberBoolean);

        TypeDescriptor[] acceptedParameterTypes = new TypeDescriptor[]{numberOrBooleanOrArrayNumberBoolean};
        TypeDescriptor[] declaredParameterTypes = new TypeDescriptor[]{numOrBoolOrArray};
        // if
        String ifStatement = "if ({1}) '{'\n{0}<-TRUE\n'}' else '{'\n{0}<-FALSE\n'}'";
        res.add(createStatement(context, "if", ifStatement,
                        new NonPrimitiveNumberParameterThrows(
                                        RResultVerifier.newBuilder(acceptedParameterTypes, new TypeDescriptor[]{numOrBoolOrArray}).primitiveAndArrayMismatchCheck().emptyArrayCheck().build()),
                        TypeDescriptor.BOOLEAN, numOrBoolOrArray));

        // while
        String whileStatement = "while ({1})'{'\nbreak\n'}'";
        res.add(createStatement(context, "while", whileStatement,
                        new NonPrimitiveNumberParameterThrows(RResultVerifier.newBuilder(acceptedParameterTypes, declaredParameterTypes).primitiveAndArrayMismatchCheck().emptyArrayCheck().build()),
                        TypeDescriptor.NULL, numOrBoolOrArray));

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

    @Override
    public Collection<? extends InlineSnippet> createInlineScripts(Context context) {
        List<InlineSnippet> res = new ArrayList<>();
        res.add(createInlineSnippet(
                        context,
                        "resources/mandel.R",
                        25,
                        26,
                        "resources/mandel_inline1.R"));
        res.add(createInlineSnippet(
                        context,
                        "resources/mandel.R",
                        38,
                        39,
                        "resources/mandel_inline2.R"));
        res.add(createInlineSnippet(
                        context,
                        "resources/quicksort.R",
                        25,
                        41,
                        "resources/quicksort_inline.R"));
        return Collections.unmodifiableList(res);
    }

    private static Snippet createValueConstructor(
                    Context context,
                    String value,
                    TypeDescriptor type) {
        return Snippet.newBuilder(value, eval(context, String.format(PATTERN_VALUE_FNC, value)), type).build();
    }

    private static Snippet createBinaryOperator(
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

    private static Snippet createPrefixOperator(
                    Context context,
                    String operator,
                    TypeDescriptor type,
                    TypeDescriptor rtype,
                    ResultVerifier verifier) {
        Value fnc = eval(context, String.format(PATTERN_PREFIX_OP_FNC, operator));
        Snippet.Builder opb = Snippet.newBuilder(operator, fnc, type).parameterTypes(rtype).resultVerifier(verifier);
        return opb.build();
    }

    private static Snippet createStatement(
                    Context context,
                    String name,
                    String expression,
                    TypeDescriptor type,
                    TypeDescriptor... paramTypes) {
        return createStatement(context, name, expression, null, type, paramTypes);
    }

    private static Snippet createStatement(
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

    private static InlineSnippet createInlineSnippet(Context context, String sourceName, int l1, int l2, String snippetName) {
        Snippet script = loadScript(context, sourceName, TypeDescriptor.ANY, null);
        String simpleName = sourceName.substring(sourceName.lastIndexOf('/') + 1);
        try {
            InlineSnippet.Builder snippetBuilder = InlineSnippet.newBuilder(script, createSource(snippetName).getCharacters());
            if (l1 > 0) {
                Predicate<SourceSection> locationPredicate = (SourceSection ss) -> {
                    return ss.getSource().getName().endsWith(simpleName) && l1 <= ss.getStartLine() && ss.getEndLine() <= l2;
                };
                snippetBuilder.locationPredicate(locationPredicate);
            }
            snippetBuilder.resultVerifier((ResultVerifier.SnippetRun snippetRun) -> {
                PolyglotException exception = snippetRun.getException();
                if (exception != null) {
                    throw exception;
                }
                Value result = snippetRun.getResult();
                if (!result.isNumber()) {
                    throw new AssertionError("Wrong value " + result.toString() + " from " + sourceName);
                }
            });
            return snippetBuilder.build();
        } catch (IOException ioe) {
            throw new AssertionError("IOException while creating a test script.", ioe);
        }
    }

    private static Snippet loadScript(
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

    private static Value eval(Context context, String statement) {
        return context.eval(ID, statement);
    }

    final class IdentityFunctionResultVerifier implements ResultVerifier {
        ResultVerifier delegate = ResultVerifier.getIdentityFunctionDefaultResultVerifier();

        private IdentityFunctionResultVerifier() {
        }

        @Override
        public void accept(SnippetRun snippetRun) throws PolyglotException {
            // We ignore objects that FastR automatically unboxes to an R vector on the boundary,
            // but they happen to have some more extra traits that the TCK would than check
            // on the result and fail
            for (Value p : snippetRun.getParameters()) {
                if (isUnboxable(p) && hasExtraTrait(p)) {
                    return;
                }
            }
            delegate.accept(snippetRun);
        }

        public static boolean hasExtraTrait(Value value) {
            return value.hasMembers() || value.hasIterator() || value.canExecute() || value.hasArrayElements() ||
                            value.hasHashEntries() || value.hasBufferElements() || value.isMetaObject() ||
                            value.isDate() || value.isDuration() || value.isException() || value.isInstant() ||
                            value.isNativePointer() || value.isProxyObject() || value.isTime() || value.isTimeZone();
        }

        public static boolean isUnboxable(Value value) {
            return value.isBoolean() || value.isString() || value.fitsInByte() || value.fitsInShort() ||
                            value.fitsInInt() || value.fitsInLong() || value.fitsInFloat() || value.fitsInDouble() ||
                            value.isNull();
        }
    }

    private static class NonPrimitiveNumberParameterThrows implements ResultVerifier {

        private final boolean stringOrObjectParameterForbidden;
        private final ResultVerifier next;

        NonPrimitiveNumberParameterThrows(ResultVerifier next) {
            this(false, next);
        }

        NonPrimitiveNumberParameterThrows(boolean stringOrObjectParameterForbidden, ResultVerifier next) {
            this.next = next != null ? next : ResultVerifier.getDefaultResultVerifier();
            this.stringOrObjectParameterForbidden = stringOrObjectParameterForbidden;
        }

        @Override
        public void accept(ResultVerifier.SnippetRun snippetRun) throws PolyglotException {
            boolean stringOrObjectParameter = false;
            boolean nonPrimitiveNumberParameter = false;
            boolean nonPrimitiveNumberParameterWrappedInArray = false;
            boolean numberOrBooleanParameters = true;
            boolean numberOrBooleanOrStringParameters = true;
            for (Value actualParameter : snippetRun.getParameters()) {
                Value parameterToCheck = actualParameter;
                if (actualParameter.hasArrayElements() && actualParameter.getArraySize() > 0) {
                    parameterToCheck = actualParameter.getArrayElement(0);
                }
                if (!parameterToCheck.isBoolean() && !parameterToCheck.isNumber()) {
                    numberOrBooleanParameters = false;
                    if (!parameterToCheck.isString()) {
                        numberOrBooleanOrStringParameters = false;
                    }
                }
                if (parameterToCheck.isNumber() && !parameterToCheck.fitsInLong() && !parameterToCheck.fitsInDouble()) {
                    nonPrimitiveNumberParameter = true;
                    if (actualParameter != parameterToCheck) {
                        nonPrimitiveNumberParameterWrappedInArray = true;
                    }
                }
                if (parameterToCheck.isString() || (!parameterToCheck.isNumber() && !parameterToCheck.isBoolean() && !parameterToCheck.isString() && !parameterToCheck.isNull() &&
                                !(parameterToCheck.hasMember("re") && parameterToCheck.hasMember("im")))) {
                    stringOrObjectParameter = true;
                }
            }
            if ((numberOrBooleanParameters && nonPrimitiveNumberParameter) ||
                            (numberOrBooleanOrStringParameters && !numberOrBooleanParameters && nonPrimitiveNumberParameter && !nonPrimitiveNumberParameterWrappedInArray) ||
                            (stringOrObjectParameter && stringOrObjectParameterForbidden)) {
                if (snippetRun.getException() == null) {
                    throw new AssertionError("TypeError expected but no error has been thrown.");
                } // else exception expected => ignore
            } else {
                next.accept(snippetRun); // no exception expected
            }
        }
    }

    private static final class RResultVerifier implements ResultVerifier {
        /**
         * Declared is a superset of accepted; If a parameter is an object array, we declare it as
         * such, but a conversion to a fastr vector accepts it only of it contains homogenous values
         * of some specific type - e.g. new Object[] {Integer, Integer}
         * 
         */
        private TypeDescriptor[] declaredParameterTypes;
        private TypeDescriptor[] acceptedParameterTypes;
        BiFunction<Boolean, SnippetRun, Void> next;

        private RResultVerifier(
                        TypeDescriptor[] acceptedParameterTypes,
                        TypeDescriptor[] declaredParameterTypes,
                        BiFunction<Boolean, SnippetRun, Void> next) {
            this.acceptedParameterTypes = Objects.requireNonNull(acceptedParameterTypes, "The acceptedParameterTypes cannot be null.");
            this.declaredParameterTypes = declaredParameterTypes;
            this.next = Objects.requireNonNull(next, "The verifier chain cannot be null.");
        }

        @Override
        public void accept(SnippetRun snippetRun) throws PolyglotException {
            boolean hasValidArgumentTypes = isAssignable(acceptedParameterTypes, snippetRun.getParameters());
            List<? extends Value> args = snippetRun.getParameters();

            boolean hasValidDeclaredTypes = isAssignable(declaredParameterTypes, args);
            if (hasValidDeclaredTypes) {
                if (hasValidArgumentTypes) {
                    next.apply(hasValidArgumentTypes, snippetRun);
                }
            } else {
                next.apply(hasValidArgumentTypes, snippetRun);
            }
        }

        private static boolean isAssignable(TypeDescriptor[] types, List<? extends Value> args) {
            if (types == null) {
                return false;
            }
            for (int i = 0; i < types.length; i++) {
                if (!types[i].isAssignable(TypeDescriptor.forValue(args.get(i)))) {
                    return false;
                }
            }
            return true;
        }

        static Builder newBuilder(TypeDescriptor[] acceptedParameterTypes) {
            return new Builder(acceptedParameterTypes, null);
        }

        static Builder newBuilder(TypeDescriptor[] acceptedParameterTypes, TypeDescriptor[] declaredParameterTypes) {
            return new Builder(acceptedParameterTypes, declaredParameterTypes);
        }

        static final class Builder {
            private final TypeDescriptor[] acceptedParameterTypes;
            private final TypeDescriptor[] declaredParameterTypes;
            private BiFunction<Boolean, SnippetRun, Void> chain;

            private Builder(TypeDescriptor[] acceptedParameterTypes, TypeDescriptor[] declaredParameterTypes) {
                this.acceptedParameterTypes = acceptedParameterTypes;
                this.declaredParameterTypes = declaredParameterTypes;
                chain = (valid, snippetRun) -> {
                    ResultVerifier.getDefaultResultVerifier().accept(snippetRun);
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
                chain = new BiFunction<>() {
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

            /**
             * Ignores errors from interop values that unbox to a different type than what is their
             * array type from FastR default conversions point of view. Example: object that
             * {@code isString}, but its array elements are bytes. Another example: object that
             * looks like integer, but is also an integer array of size > 1.
             *
             * @return the Builder
             */
            Builder primitiveAndArrayMismatchCheck() {
                chain = new BiFunction<>() {
                    private final BiFunction<Boolean, SnippetRun, Void> next = chain;

                    @Override
                    public Void apply(Boolean valid, SnippetRun sr) {
                        if (valid && sr.getException() != null && hasMismatchingArgs(sr.getParameters())) {
                            return null;
                        }
                        return next.apply(valid, sr);
                    }

                    private boolean hasMismatchingArgs(List<? extends Value> args) {
                        for (Value arg : args) {
                            if (checkPrimitive(arg, Value::isString) ||
                                            checkPrimitive(arg, Value::fitsInByte) ||
                                            checkPrimitive(arg, Value::fitsInShort) ||
                                            checkPrimitive(arg, Value::fitsInInt) ||
                                            checkPrimitive(arg, Value::fitsInLong) ||
                                            checkPrimitive(arg, Value::fitsInDouble) ||
                                            checkPrimitive(arg, Value::fitsInFloat)) {
                                return true;
                            }
                        }
                        return false;
                    }

                    private boolean checkPrimitive(Value arg, Function<Value, Boolean> fitsIn) {
                        return fitsIn.apply(arg) && arg.hasArrayElements() && (arg.getArraySize() != 1 || !fitsIn.apply(arg.getArrayElement(0)));
                    }
                };
                return this;
            }

            /**
             * Ignores the result if the snippet contains pure big integers, i.e., numbers larger
             * than max long.
             */
            Builder ignoreBigInts() {
                chain = new BiFunction<>() {
                    private final BiFunction<Boolean, SnippetRun, Void> next = chain;

                    @Override
                    public Void apply(Boolean valid, SnippetRun sr) {
                        if (valid && sr.getException() != null && hasBigInt(sr.getParameters())) {
                            return null;
                        }
                        return next.apply(valid, sr);
                    }

                    private boolean hasBigInt(List<? extends Value> args) {
                        for (Value arg : args) {
                            if (arg.fitsInBigInteger() && !arg.fitsInLong()) {
                                return true;
                            }
                        }
                        return false;
                    }
                };
                return this;
            }

            // - not empty homogenous number, boolean or string arrays -> vector
            // - any other array -> list
            //
            // comparing:
            // - null with anything does not fail - logical(0)
            // - empty list or vector with anything does not fail - logical(0)
            // - atomic vectors does not fail
            // - a list with an atomic vector does not fail
            // - a list with another list FAILS
            // - other kind of object with anything but null or empty list or vector FAILS
            Builder compareParametersCheck() {
                chain = new BiFunction<>() {
                    private final BiFunction<Boolean, SnippetRun, Void> next = chain;

                    @Override
                    public Void apply(Boolean valid, SnippetRun sr) {
                        if (valid && sr.getException() != null && expectsException(sr.getParameters())) {
                            return null;
                        }
                        return next.apply(valid, sr);
                    }

                    private boolean expectsException(List<? extends Value> args) {
                        boolean parametersValid = false;
                        int mixed = 0;
                        for (Value arg : args) {
                            parametersValid = false;
                            if (arg.isNull()) {
                                // one of the given parameters is NULL
                                // this is never expected to fail
                                return false;
                            }
                            if (arg.isNumber() || arg.isString() || arg.isBoolean()) {
                                parametersValid = true;
                            } else if (arg.hasArrayElements()) {
                                if (arg.getArraySize() == 0) {
                                    // one of the given parameters is an emtpy list or vector,
                                    // this is never expected to fail
                                    return false;
                                } else {
                                    boolean str = false;
                                    boolean num = false;
                                    boolean other = false;
                                    for (int i = 0; i < arg.getArraySize(); i++) {
                                        TypeDescriptor td = TypeDescriptor.forValue(arg.getArrayElement(i));
                                        if (TypeDescriptor.STRING.isAssignable(td)) {
                                            str = true;
                                        } else if (TypeDescriptor.NUMBER.isAssignable(td) || TypeDescriptor.BOOLEAN.isAssignable(td)) {
                                            num = true;
                                        } else {
                                            other = true;
                                        }
                                    }
                                    parametersValid = !other;
                                    if (str && num) {
                                        mixed++;
                                    }
                                }
                            }
                            if (!parametersValid) {
                                break;
                            }
                        }
                        return !(parametersValid && mixed < args.size());
                    }
                };
                return this;
            }

            RResultVerifier build() {
                return new RResultVerifier(acceptedParameterTypes, declaredParameterTypes, chain);
            }
        }
    }
}
