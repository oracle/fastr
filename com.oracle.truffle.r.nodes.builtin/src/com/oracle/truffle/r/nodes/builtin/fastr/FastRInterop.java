/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.fastr;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.doubleValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.integerValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.notLogicalNA;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.numericValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.rawValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.runtime.RVisibility.CUSTOM;
import static com.oracle.truffle.r.runtime.RVisibility.OFF;
import static com.oracle.truffle.r.runtime.RVisibility.ON;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleException;
import com.oracle.truffle.api.TruffleFile;
import com.oracle.truffle.api.TruffleLanguage.Env;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.Source.Builder;
import com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.instanceOf;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.logicalValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.singleElement;
import com.oracle.truffle.r.nodes.builtin.NodeWithArgumentCasts;
import com.oracle.truffle.r.nodes.builtin.NodeWithArgumentCasts.Casts;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.function.call.RExplicitCallNode;
import com.oracle.truffle.r.nodes.function.visibility.SetVisibilityNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RSource;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RInteropScalar;
import com.oracle.truffle.r.runtime.data.RInteropScalar.RInteropByte;
import com.oracle.truffle.r.runtime.data.RInteropScalar.RInteropChar;
import com.oracle.truffle.r.runtime.data.RInteropScalar.RInteropFloat;
import com.oracle.truffle.r.runtime.data.RInteropScalar.RInteropLong;
import com.oracle.truffle.r.runtime.data.RInteropScalar.RInteropShort;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RRaw;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractRawVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.interop.FastRInteropTryException;
import com.oracle.truffle.r.runtime.interop.FastrInteropTryContextState;
import com.oracle.truffle.r.runtime.interop.Foreign2R;
import com.oracle.truffle.r.runtime.interop.ForeignArray2R;
import com.oracle.truffle.r.runtime.interop.R2Foreign;

public class FastRInterop {

    private static boolean isTesting = false;

    public static void testingMode() {
        isTesting = true;
    }

    @RBuiltin(name = "eval.polyglot", visibility = CUSTOM, kind = PRIMITIVE, parameterNames = {"languageId", "source", "path"}, behavior = COMPLEX)
    public abstract static class Eval extends RBuiltinNode.Arg3 {

        static {
            Casts casts = new Casts(Eval.class);
            casts.arg("languageId").allowMissing().mustBe(stringValue()).asStringVector().mustBe(singleElement()).findFirst();
            casts.arg("source").allowMissing().mustBe(stringValue()).asStringVector().mustBe(singleElement()).findFirst();
            casts.arg("path").allowMissing().mustBe(stringValue()).asStringVector().mustBe(singleElement()).findFirst();
        }

        @Child private SetVisibilityNode setVisibilityNode = SetVisibilityNode.create();
        @Child private Foreign2R foreign2rNode = Foreign2R.create();

        protected DirectCallNode createCall(String languageId, String source) {
            return Truffle.getRuntime().createDirectCallNode(parse(languageId, source));
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"cachedLanguageId != null", "cachedLanguageId.equals(languageId)", "cachedSource != null", "cachedSource.equals(source)"})
        protected Object evalCached(VirtualFrame frame, String languageId, String source, RMissing path,
                        @Cached("languageId") String cachedLanguageId,
                        @Cached("source") String cachedSource,
                        @Cached("createCall(languageId, source)") DirectCallNode call) {
            try {
                return foreign2rNode.execute(call.call(EMPTY_OBJECT_ARRAY));
            } finally {
                setVisibilityNode.execute(frame, true);
            }
        }

        @Specialization(replaces = "evalCached")
        protected Object eval(VirtualFrame frame, String languageId, String source, @SuppressWarnings("unused") RMissing path) {
            try {
                return foreign2rNode.execute(parseAndCall(source, languageId));
            } finally {
                setVisibilityNode.execute(frame, true);
            }
        }

        @TruffleBoundary
        private Object parseAndCall(String source, String languageId) {
            return parse(languageId, source).call();
        }

        @Specialization()
        @TruffleBoundary
        protected Object eval(@SuppressWarnings("unused") RMissing languageId, @SuppressWarnings("unused") String source, @SuppressWarnings("unused") RMissing path) {
            throw RError.error(this, RError.Message.INVALID_ARG, "languageId");
        }

        protected CallTarget parse(String languageId, String source) {
            CompilerAsserts.neverPartOfCompilation();

            Source sourceObject = RSource.fromTextInternalInvisible(source, RSource.Internal.EVAL_WRAPPER, languageId);
            try {
                return RContext.getInstance().getEnv().parse(sourceObject);
            } catch (Throwable t) {
                throw error(RError.Message.GENERIC, "Error while parsing: " + t.getMessage());
            }
        }

        @Specialization
        protected Object eval(VirtualFrame frame, String languageId, @SuppressWarnings("unused") String source, String path) {
            try {
                return foreign2rNode.execute(parseFileAndCall(path, languageId));
            } finally {
                setVisibilityNode.execute(frame, false);
            }
        }

        @Specialization
        protected Object eval(VirtualFrame frame, String languageId, @SuppressWarnings("unused") RMissing source, String path) {
            try {
                return foreign2rNode.execute(parseFileAndCall(path, languageId));
            } finally {
                setVisibilityNode.execute(frame, false);
            }
        }

        @Specialization
        protected Object eval(VirtualFrame frame, @SuppressWarnings("unused") RMissing languageId, @SuppressWarnings("unused") RMissing source, String path) {
            try {
                return foreign2rNode.execute(parseFileAndCall(path, null));
            } finally {
                setVisibilityNode.execute(frame, false);
            }
        }

        @TruffleBoundary
        private Object parseFileAndCall(String path, String languageId) {
            return parseFile(path, languageId).call();
        }

        protected CallTarget parseFile(String path, String languageId) {
            CompilerAsserts.neverPartOfCompilation();

            File file = new File(path);
            try {
                Builder<IOException, RuntimeException, RuntimeException> sourceBuilder = Source.newBuilder(file).name(file.getName());
                if (languageId != null) {
                    sourceBuilder.language(languageId);
                }
                Source sourceObject = sourceBuilder.build();
                return RContext.getInstance().getEnv().parse(sourceObject);
            } catch (IOException e) {
                throw error(RError.Message.GENERIC, "Error reading file: " + e.getMessage());
            } catch (Throwable t) {
                throw error(RError.Message.GENERIC, "Error while parsing: " + t.getMessage());
            }
        }

        @Specialization
        @TruffleBoundary
        protected Object eval(@SuppressWarnings("unused") RMissing source, @SuppressWarnings("unused") RMissing languageId, @SuppressWarnings("unused") RMissing path) {
            throw RError.error(this, RError.Message.INVALID_ARG, "'source' or 'path'");
        }
    }

    @RBuiltin(name = "export", visibility = OFF, kind = PRIMITIVE, parameterNames = {"name", "value"}, behavior = COMPLEX)
    public abstract static class Export extends RBuiltinNode.Arg2 {

        static {
            Casts casts = new Casts(Export.class);
            casts.arg("name").mustBe(stringValue()).asStringVector().mustBe(singleElement()).findFirst();
            casts.arg("value").boxPrimitive();
        }

        @Specialization(guards = "!isRMissing(value)")
        @TruffleBoundary
        protected Object exportSymbol(String name, TruffleObject value) {
            if (name == null) {
                throw error(RError.Message.INVALID_ARGUMENT, "name");
            }
            RContext.getInstance().getEnv().exportSymbol(name, value);
            return RNull.instance;
        }

        @Specialization
        protected Object exportSymbol(@SuppressWarnings("unused") String name, @SuppressWarnings("unused") RMissing value) {
            throw error(RError.Message.ARGUMENT_MISSING, "value");
        }

        @Fallback
        protected Object exportSymbol(@SuppressWarnings("unused") Object name, @SuppressWarnings("unused") Object value) {
            throw error(RError.Message.GENERIC, "only R language objects can be exported");
        }
    }

    @RBuiltin(name = "import", visibility = OFF, kind = PRIMITIVE, parameterNames = {"name"}, behavior = COMPLEX)
    public abstract static class Import extends RBuiltinNode.Arg1 {

        static {
            Casts casts = new Casts(Import.class);
            casts.arg("name").mustBe(stringValue()).asStringVector().mustBe(singleElement()).findFirst();
        }

        @Specialization
        @TruffleBoundary
        protected Object importSymbol(String name) {
            Object object = RContext.getInstance().getEnv().importSymbol(name);
            if (object == null) {
                throw error(RError.Message.NO_IMPORT_OBJECT, name);
            }
            return object;
        }
    }

    @RBuiltin(name = ".fastr.interop.asByte", visibility = ON, kind = PRIMITIVE, parameterNames = {"value"}, behavior = COMPLEX)
    public abstract static class ToByte extends RBuiltinNode.Arg1 {

        static {
            castToInteroptNumberType(new Casts(ToByte.class));
        }

        @Specialization
        public RInteropByte toByte(int value) {
            return RInteropByte.valueOf((byte) value);
        }

        @Specialization
        public RInteropByte toByte(double value) {
            return RInteropByte.valueOf((byte) value);
        }

        @Specialization
        public RInteropByte toByte(RRaw value) {
            return RInteropByte.valueOf(value.getValue());
        }
    }

    @RBuiltin(name = ".fastr.interop.asChar", visibility = ON, kind = PRIMITIVE, parameterNames = {"value", "pos"}, behavior = COMPLEX)
    public abstract static class ToChar extends RBuiltinNode.Arg2 {

        static {
            Casts casts = new Casts(ToChar.class);
            casts.arg("value").mustBe(integerValue().or(doubleValue().or(stringValue())), RError.Message.INVALID_ARGUMENT_OF_TYPE, "value", Predef.typeName()).asVector().mustBe(
                            singleElement()).findFirst();
            casts.arg("pos").allowMissing().mustBe(numericValue(), RError.Message.INVALID_ARGUMENT_OF_TYPE, "pos", Predef.typeName()).asIntegerVector().mustBe(singleElement()).findFirst();
        }

        @Specialization
        public RInteropChar toChar(int value, @SuppressWarnings("unused") RMissing unused) {
            return RInteropChar.valueOf((char) value);
        }

        @Specialization
        public RInteropChar toChar(double value, @SuppressWarnings("unused") RMissing unused) {
            return RInteropChar.valueOf((char) value);
        }

        @Specialization
        public RInteropChar toChar(String value, @SuppressWarnings("unused") RMissing unused) {
            return toChar(value, 0);
        }

        @SuppressWarnings("unused")
        @Specialization
        public RInteropChar toChar(int value, int pos) {
            throw RError.error(this, RError.Message.POS_NOT_ALLOWED_WITH_NUMERIC);
        }

        @SuppressWarnings("unused")
        @Specialization
        public RInteropChar toChar(double value, int pos) {
            throw RError.error(this, RError.Message.POS_NOT_ALLOWED_WITH_NUMERIC);
        }

        @Specialization
        public RInteropChar toChar(String value, int pos) {
            return RInteropChar.valueOf(value.charAt(pos));
        }
    }

    @RBuiltin(name = ".fastr.interop.asFloat", visibility = ON, kind = PRIMITIVE, parameterNames = {"value"}, behavior = COMPLEX)
    public abstract static class ToFloat extends RBuiltinNode.Arg1 {

        static {
            castToInteroptNumberType(new Casts(ToFloat.class));
        }

        @Specialization
        public RInteropFloat toFloat(int value) {
            return RInteropFloat.valueOf(value);
        }

        @Specialization
        public RInteropFloat toFloat(double value) {
            return RInteropFloat.valueOf((float) value);
        }

        @Specialization
        public RInteropFloat toFloat(RRaw value) {
            return RInteropFloat.valueOf(value.getValue());
        }
    }

    @RBuiltin(name = ".fastr.interop.asLong", visibility = ON, kind = PRIMITIVE, parameterNames = {"value"}, behavior = COMPLEX)
    public abstract static class ToLong extends RBuiltinNode.Arg1 {

        static {
            castToInteroptNumberType(new Casts(ToLong.class));
        }

        @Specialization
        public RInteropLong toLong(int value) {
            return RInteropLong.valueOf(value);
        }

        @Specialization
        public RInteropLong toLong(double value) {
            return RInteropLong.valueOf((long) value);
        }

        @Specialization
        public RInteropLong toLong(RRaw value) {
            return RInteropLong.valueOf(value.getValue());
        }
    }

    @RBuiltin(name = ".fastr.interop.asShort", visibility = ON, kind = PRIMITIVE, parameterNames = {"value"}, behavior = COMPLEX)
    public abstract static class ToShort extends RBuiltinNode.Arg1 {

        static {
            castToInteroptNumberType(new Casts(ToShort.class));
        }

        @Specialization
        public RInteropShort toShort(double value) {
            return RInteropShort.valueOf((short) value);
        }

        @Specialization
        public RInteropShort toShort(int value) {
            return RInteropShort.valueOf((short) value);
        }

        @Specialization
        public RInteropShort toShort(RRaw value) {
            return RInteropShort.valueOf(value.getValue());
        }
    }

    private static void castToInteroptNumberType(Casts casts) {
        casts.arg("value").mustBe(integerValue().or(doubleValue().or(rawValue())), RError.Message.INVALID_ARGUMENT_OF_TYPE, "value", Predef.typeName()).asVector().mustBe(singleElement()).findFirst();
    }

    @RBuiltin(name = "java.type", visibility = ON, kind = PRIMITIVE, parameterNames = {"className", "silent"}, behavior = COMPLEX)
    public abstract static class JavaType extends RBuiltinNode.Arg2 {

        static {
            Casts casts = new Casts(JavaType.class);
            casts.arg("className").mustBe(stringValue()).asStringVector().mustBe(Predef.singleElement()).findFirst();
            casts.arg("silent").mapMissing(Predef.constant(RRuntime.LOGICAL_FALSE)).mustBe(logicalValue().or(Predef.nullValue())).asLogicalVector().mustBe(singleElement()).findFirst().mustBe(
                            notLogicalNA()).map(Predef.toBoolean());
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"isClass(className)", "className.equals(cachedClazz)"}, limit = "getCacheSize(10)")
        public TruffleObject javaClassCached(String className, boolean silent,
                        @Cached("className") String cachedClazz,
                        @Cached("getJavaClass(className, silent)") Object result,
                        @Cached("create()") BranchProfile interopExceptionProfile) {
            return javaClassToTruffleObject(className, result, interopExceptionProfile);
        }

        @Specialization(replaces = "javaClassCached")
        public TruffleObject javaClass(String className, boolean silent,
                        @Cached("create()") BranchProfile interopExceptionProfile) {
            Object result = getJavaClass(className, silent);
            return javaClassToTruffleObject(className, result, interopExceptionProfile);
        }

        protected boolean isClass(Object obj) {
            return obj != null && obj instanceof Class;
        }

        protected Object getJavaClass(String clazz, boolean silent) {
            return classForName(clazz, silent);
        }

        @TruffleBoundary
        protected TruffleObject javaClassToTruffleObject(String clazz, Object result, BranchProfile interopExceptionProfile) {
            if (result == RNull.instance) {
                return RNull.instance;
            }
            if (result instanceof TruffleObject) {
                return (TruffleObject) result;
            } else {
                interopExceptionProfile.enter();
                if (result instanceof RuntimeException) {
                    throw RError.handleInteropException(this, (RuntimeException) result);
                } else {
                    assert result instanceof Throwable : "class " + clazz + " resulted into " + (result == null ? "NULL" : result.getClass().getName());
                    throw RError.handleInteropException(this, new RuntimeException((Throwable) result));
                }
            }
        }
    }

    @RBuiltin(name = "java.addToClasspath", visibility = OFF, kind = PRIMITIVE, parameterNames = {"value", "silent"}, behavior = COMPLEX)
    public abstract static class JavaAddToClasspath extends RBuiltinNode.Arg2 {

        static {
            Casts casts = new Casts(JavaAddToClasspath.class);
            casts.arg("value").mustBe(stringValue()).asStringVector();
            casts.arg("silent").mapMissing(Predef.constant(RRuntime.LOGICAL_FALSE)).mustBe(logicalValue().or(Predef.nullValue())).asLogicalVector().mustBe(singleElement()).findFirst().mustBe(
                            notLogicalNA()).map(Predef.toBoolean());
        }

        @Specialization
        @TruffleBoundary
        public TruffleObject addEntries(RAbstractStringVector value, boolean silent) {
            Env env = RContext.getInstance().getEnv();

            for (int i = 0; i < value.getLength(); i++) {
                TruffleFile file = env.getTruffleFile(value.getDataAt(i));
                try {
                    env.addToHostClassPath(file);
                } catch (Exception e) {
                    if (silent) {
                        return RNull.instance;
                    }
                    throw error(RError.Message.GENERIC, "error while adding classpath entry: " +
                                    e.getMessage());

                }
            }

            return RNull.instance;
        }
    }

    @RBuiltin(name = ".fastr.interop.isIdentical", visibility = ON, kind = PRIMITIVE, parameterNames = {"x1", "x2"}, behavior = COMPLEX)
    public abstract static class JavaIsIdentical extends RBuiltinNode.Arg2 {

        static {
            Casts.noCasts(JavaIsIdentical.class);
        }

        @Specialization(guards = {"isJavaObject(x1)", "isJavaObject(x2)"})
        public byte isIdentical(TruffleObject x1, TruffleObject x2) {
            RContext context = RContext.getInstance();
            return RRuntime.asLogical(context.getEnv().asHostObject(x1) == context.getEnv().asHostObject(x2));
        }

        @Fallback
        @TruffleBoundary
        public byte isIdentical(Object x1, Object x2) {
            throw error(RError.Message.GENERIC, String.format("unsupported types: %s, %s", x1.getClass().getName(), x2.getClass().getName()));
        }

        protected boolean isJavaObject(TruffleObject obj) {
            return RContext.getInstance().getEnv().isHostObject(obj);
        }
    }

    @RBuiltin(name = ".fastr.interop.asJavaTruffleObject", visibility = ON, kind = PRIMITIVE, parameterNames = {"x"}, behavior = COMPLEX)
    public abstract static class JavaAsTruffleObject extends RBuiltinNode.Arg1 {

        static {
            Casts.noCasts(JavaAsTruffleObject.class);
        }

        @Specialization
        public TruffleObject asTruffleObject(byte b) {
            return (TruffleObject) RContext.getInstance().getEnv().asBoxedGuestValue(RRuntime.fromLogical(b));
        }

        @Specialization
        public TruffleObject asTruffleObject(int i) {
            return (TruffleObject) RContext.getInstance().getEnv().asBoxedGuestValue(i);
        }

        @Specialization
        public TruffleObject asTruffleObject(double d) {
            return (TruffleObject) RContext.getInstance().getEnv().asBoxedGuestValue(d);
        }

        @Specialization
        public TruffleObject asTruffleObject(String s) {
            return (TruffleObject) RContext.getInstance().getEnv().asBoxedGuestValue(s);
        }

        @Specialization
        public TruffleObject asTruffleObject(RInteropByte b) {
            return (TruffleObject) RContext.getInstance().getEnv().asBoxedGuestValue(b.getValue());
        }

        @Specialization
        public TruffleObject asTruffleObject(RInteropChar c) {
            return (TruffleObject) RContext.getInstance().getEnv().asBoxedGuestValue(c.getValue());
        }

        @Specialization
        public TruffleObject asTruffleObject(RInteropFloat f) {
            return (TruffleObject) RContext.getInstance().getEnv().asBoxedGuestValue(f.getValue());
        }

        @Specialization
        public TruffleObject asTruffleObject(RInteropLong l) {
            return (TruffleObject) RContext.getInstance().getEnv().asBoxedGuestValue(l.getValue());
        }

        @Specialization
        public TruffleObject asTruffleObject(RInteropShort s) {
            return (TruffleObject) RContext.getInstance().getEnv().asBoxedGuestValue(s.getValue());
        }

        @Fallback
        @TruffleBoundary
        public byte asTruffleObject(Object x) {
            throw error(RError.Message.GENERIC, String.format("unsupported type: %s", x.getClass().getName()));
        }
    }

    @RBuiltin(name = ".fastr.interop.isAssignableFrom", visibility = ON, kind = PRIMITIVE, parameterNames = {"x1", "x2"}, behavior = COMPLEX)
    public abstract static class JavaIsAssignableFrom extends RBuiltinNode.Arg2 {

        static {
            Casts.noCasts(JavaIsAssignableFrom.class);
        }

        @Specialization(guards = {"isJavaObject(x1)", "isJavaObject(x2)"})
        public byte isAssignable(TruffleObject x1, TruffleObject x2) {
            RContext context = RContext.getInstance();
            Object jo1 = context.getEnv().asHostObject(x1);
            Class<?> cl1 = (jo1 instanceof Class) ? (Class<?>) jo1 : jo1.getClass();
            Object jo2 = context.getEnv().asHostObject(x2);
            Class<?> cl2 = (jo2 instanceof Class) ? (Class<?>) jo2 : jo2.getClass();
            return RRuntime.asLogical(cl2.isAssignableFrom(cl1));
        }

        @Fallback
        @TruffleBoundary
        public byte isAssignable(Object x1, Object x2) {
            throw error(RError.Message.GENERIC, String.format("unsupported types: %s, %s", x1.getClass().getName(), x2.getClass().getName()));
        }

        protected boolean isJavaObject(TruffleObject obj) {
            return RContext.getInstance().getEnv().isHostObject(obj);
        }
    }

    @RBuiltin(name = ".fastr.interop.isInstance", visibility = ON, kind = PRIMITIVE, parameterNames = {"x1", "x2"}, behavior = COMPLEX)
    public abstract static class JavaIsInstance extends RBuiltinNode.Arg2 {

        static {
            Casts.noCasts(JavaIsInstance.class);
        }

        @Specialization(guards = {"isJavaObject(x1)", "isJavaObject(x2)"})
        public byte isInstance(TruffleObject x1, TruffleObject x2) {
            RContext context = RContext.getInstance();
            Object jo1 = context.getEnv().asHostObject(x1);
            Object jo2 = context.getEnv().asHostObject(x2);
            if (jo1 instanceof Class) {
                Class<?> cl1 = (Class<?>) jo1;
                return RRuntime.asLogical(cl1.isInstance(jo2));
            }
            return RRuntime.asLogical(jo1.getClass().isInstance(jo2));
        }

        @Specialization(guards = {"isJavaObject(x1)"})
        public byte isInstance(TruffleObject x1, RInteropScalar x2,
                        @Cached("createR2Foreign()") R2Foreign r2Foreign) {
            Object jo1 = RContext.getInstance().getEnv().asHostObject(x1);
            if (jo1 instanceof Class) {
                Class<?> cl1 = (Class<?>) jo1;
                return RRuntime.asLogical(cl1.isInstance(r2Foreign.execute(x2)));
            }
            return RRuntime.asLogical(jo1.getClass().isInstance(x2));
        }

        @Specialization(guards = {"isJavaObject(x1)", "!isJavaObject(x2)", "!isInterop(x2)"})
        public byte isInstance(TruffleObject x1, Object x2) {
            Object jo1 = RContext.getInstance().getEnv().asHostObject(x1);
            if (jo1 instanceof Class) {
                Class<?> cl1 = (Class<?>) jo1;
                return RRuntime.asLogical(cl1.isInstance(x2));
            }
            return RRuntime.asLogical(jo1.getClass().isInstance(x2));
        }

        @Fallback
        @TruffleBoundary
        public byte isInstance(Object x1, Object x2) {
            throw error(RError.Message.GENERIC, String.format("unsupported types: %s, %s", x1.getClass().getName(), x2.getClass().getName()));
        }

        protected boolean isJavaObject(Object obj) {
            return RRuntime.isForeignObject(obj) && RContext.getInstance().getEnv().isHostObject(obj);
        }

        protected boolean isInterop(Object obj) {
            return obj instanceof RInteropScalar;
        }

        protected R2Foreign createR2Foreign() {
            return R2Foreign.create();
        }
    }

    @ImportStatic(Message.class)
    @RBuiltin(name = ".fastr.interop.asJavaArray", visibility = ON, kind = PRIMITIVE, parameterNames = {"x", "className", "flat"}, behavior = COMPLEX)
    public abstract static class ToJavaArray extends RBuiltinNode.Arg3 {

        BranchProfile interopExceptionProfile = BranchProfile.create();

        static {
            Casts casts = new Casts(ToJavaArray.class);
            casts.arg("x").castForeignObjects(false).mustNotBeMissing();
            casts.arg("className").allowMissing().mustBe(stringValue()).asStringVector().mustBe(Predef.singleElement()).findFirst();
            casts.arg("flat").mapMissing(Predef.constant(RRuntime.LOGICAL_TRUE)).mustBe(logicalValue().or(Predef.nullValue())).asLogicalVector().mustBe(singleElement()).findFirst().mustBe(
                            notLogicalNA()).map(Predef.toBoolean());
        }

        abstract Object execute(Object arg1, Object arg2, Object arg3);

        @Specialization
        @TruffleBoundary
        public Object toArray(RAbstractLogicalVector vec, @SuppressWarnings("unused") RMissing className, boolean flat,
                        @Cached("createR2Foreign()") R2Foreign r2Foreign) {
            return toArray(vec, flat, boolean.class, (array, i) -> Array.set(array, i, r2Foreign.execute(vec.getDataAt(i))));
        }

        @Specialization
        @TruffleBoundary
        public Object toArray(RAbstractLogicalVector vec, String className, boolean flat,
                        @Cached("createR2Foreign()") R2Foreign r2Foreign) {
            return toArray(vec, flat, getClazz(className), (array, i) -> Array.set(array, i, r2Foreign.execute(vec.getDataAt(i))));
        }

        @Specialization
        @TruffleBoundary
        public Object toArray(RAbstractIntVector vec, @SuppressWarnings("unused") RMissing className, boolean flat,
                        @Cached("createR2Foreign()") R2Foreign r2Foreign) {
            return toArray(vec, flat, int.class, (array, i) -> Array.set(array, i, r2Foreign.execute(vec.getDataAt(i))));
        }

        @Specialization
        @TruffleBoundary
        public Object toArray(RAbstractIntVector vec, String className, boolean flat) {
            return toArray(vec, flat, getClazz(className), (array, i) -> {
                if (Byte.TYPE.getName().equals(className)) {
                    Array.set(array, i, (byte) vec.getDataAt(i));
                } else if (Character.TYPE.getName().equals(className)) {
                    Array.set(array, i, (char) vec.getDataAt(i));
                } else if (Double.TYPE.getName().equals(className)) {
                    Array.set(array, i, (double) vec.getDataAt(i));
                } else if (Float.TYPE.getName().equals(className)) {
                    Array.set(array, i, (float) vec.getDataAt(i));
                } else if (Long.TYPE.getName().equals(className)) {
                    Array.set(array, i, (long) vec.getDataAt(i));
                } else if (Short.TYPE.getName().equals(className)) {
                    Array.set(array, i, (short) vec.getDataAt(i));
                } else {
                    Array.set(array, i, vec.getDataAt(i));
                }
            });
        }

        @Specialization
        @TruffleBoundary
        public Object toArray(RAbstractDoubleVector vec, @SuppressWarnings("unused") RMissing className, boolean flat) {
            return toArray(vec, flat, double.class, (array, i) -> Array.set(array, i, vec.getDataAt(i)));
        }

        @Specialization
        @TruffleBoundary
        public Object toArray(RAbstractDoubleVector vec, String className, boolean flat) {
            return toArray(vec, flat, getClazz(className), (array, i) -> {
                if (Byte.TYPE.getName().equals(className)) {
                    Array.set(array, i, (byte) vec.getDataAt(i));
                } else if (Character.TYPE.getName().equals(className)) {
                    Array.set(array, i, (char) vec.getDataAt(i));
                } else if (Float.TYPE.getName().equals(className)) {
                    Array.set(array, i, (float) vec.getDataAt(i));
                } else if (Integer.TYPE.getName().equals(className)) {
                    Array.set(array, i, (int) vec.getDataAt(i));
                } else if (Long.TYPE.getName().equals(className)) {
                    Array.set(array, i, (long) vec.getDataAt(i));
                } else if (Short.TYPE.getName().equals(className)) {
                    Array.set(array, i, (short) vec.getDataAt(i));
                } else {
                    Array.set(array, i, vec.getDataAt(i));
                }
            });
        }

        @Specialization
        @TruffleBoundary
        public Object toArray(RAbstractStringVector vec, @SuppressWarnings("unused") RMissing className, boolean flat) {
            return toArray(vec, flat, String.class, (array, i) -> Array.set(array, i, vec.getDataAt(i)));
        }

        @Specialization
        @TruffleBoundary
        public Object toArray(RAbstractStringVector vec, String className, boolean flat) {
            return toArray(vec, flat, getClazz(className), (array, i) -> Array.set(array, i, vec.getDataAt(i)));
        }

        @Specialization
        @TruffleBoundary
        public Object toArray(RAbstractRawVector vec, @SuppressWarnings("unused") RMissing className, boolean flat) {
            return toArray(vec, flat, byte.class, (array, i) -> Array.set(array, i, vec.getRawDataAt(i)));
        }

        @Specialization
        @TruffleBoundary
        public Object toArray(RAbstractRawVector vec, String className, boolean flat) {
            return toArray(vec, flat, getClazz(className), (array, i) -> Array.set(array, i, vec.getRawDataAt(i)));
        }

        @Specialization(guards = "!isJavaLikeVector(vec)")
        @TruffleBoundary
        public Object toArray(RAbstractVector vec, @SuppressWarnings("unused") RMissing className, boolean flat,
                        @Cached("createR2Foreign()") R2Foreign r2Foreign) {
            return toArray(vec, flat, Object.class, r2Foreign);
        }

        @Specialization(guards = "!isJavaLikeVector(vec)")
        @TruffleBoundary
        public Object toArray(RAbstractVector vec, String className, boolean flat,
                        @Cached("createR2Foreign()") R2Foreign r2Foreign) {
            return toArray(vec, flat, getClazz(className), r2Foreign);
        }

        @Specialization
        @TruffleBoundary
        public Object toArray(RInteropScalar ri, String className, boolean flat,
                        @Cached("createR2Foreign()") R2Foreign r2Foreign) {
            RList list = RDataFactory.createList(new Object[]{ri});
            return toArray(list, flat, getClazz(className), (array, i) -> Array.set(array, i, r2Foreign.execute(list.getDataAt(i))));
        }

        @Specialization
        @TruffleBoundary
        public Object toArray(RInteropScalar ri, @SuppressWarnings("unused") RMissing className, boolean flat,
                        @Cached("createR2Foreign()") R2Foreign r2Foreign) {
            RList list = RDataFactory.createList(new Object[]{ri});
            return toArray(list, flat, ri.getJavaType(), (array, i) -> Array.set(array, i, r2Foreign.execute(list.getDataAt(i))));
        }

        protected R2Foreign createR2Foreign() {
            return R2Foreign.createNoBox();
        }

        private static int[] getDim(boolean flat, RAbstractVector vec) {
            int[] dims;
            if (flat) {
                dims = new int[]{vec.getLength()};
            } else {
                dims = vec.getDimensions();
                if (dims == null) {
                    dims = new int[]{vec.getLength()};
                }
            }
            return dims;
        }

        private static Object toArray(RAbstractVector vec, boolean flat, Class<?> clazz, VecElementToArray vecToArray) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
            int[] dims = getDim(flat, vec);
            // TODO need ForeignAccess.sendNew(multiDimArrayClass, dims)
            final Object array = Array.newInstance(clazz, dims);
            for (int d = 0; d < dims.length; d++) {
                int dim = dims[d];
                // TODO works only for flat
                for (int i = 0; i < dim; i++) {
                    vecToArray.toArray(array, i);
                }
            }
            return RContext.getInstance().getEnv().asGuestValue(array);
        }

        private Object toArray(RAbstractVector vec, boolean flat, Class<?> clazz, R2Foreign r2Foreign) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
            int[] dims = getDim(flat, vec);
            final Object array = Array.newInstance(clazz, dims);
            TruffleObject truffleArray = (TruffleObject) RContext.getInstance().getEnv().asGuestValue(array);

            for (int d = 0; d < dims.length; d++) {
                int dim = dims[d];
                // TODO works only for flat
                for (int i = 0; i < dim; i++) {
                    try {
                        Object value = r2Foreign.execute(vec.getDataAtAsObject(i));
                        ForeignAccess.sendWrite(Message.WRITE.createNode(), truffleArray, i, value);
                    } catch (InteropException ex) {
                        throw error(RError.Message.GENERIC, ex.getMessage());
                    }
                }
            }
            return truffleArray;
        }

        private interface VecElementToArray {
            void toArray(Object array, Integer i);
        }

        @Specialization(guards = "isJavaObject(obj)")
        @TruffleBoundary
        public Object toArray(TruffleObject obj, @SuppressWarnings("unused") RMissing missing, @SuppressWarnings("unused") boolean flat,
                        @Cached("HAS_SIZE.createNode()") Node hasSize,
                        @Cached("WRITE.createNode()") Node write) {

            if (ForeignAccess.sendHasSize(hasSize, obj)) {
                // TODO should return copy?
                return obj;
            }
            try {
                RContext context = RContext.getInstance();
                Object o = context.getEnv().asHostObject(obj);
                if (o == null) {
                    return obj;
                }
                TruffleObject array = (TruffleObject) context.getEnv().asGuestValue(Array.newInstance(o.getClass(), 1));
                ForeignAccess.sendWrite(write, array, 0, obj);
                return array;
            } catch (UnsupportedTypeException | UnsupportedMessageException | UnknownIdentifierException e) {
                throw error(RError.Message.GENERIC, "error while creating array: " + e.getMessage());
            }
        }

        @SuppressWarnings("unused")
        @Fallback
        public Object toArray(Object o, Object className, Object flat) {
            throw error(RError.Message.GENERIC, "unsupported type");
        }

        @TruffleBoundary
        private Class<?> getClazz(String className) throws RError {
            Object result = classForName(className);

            if (result instanceof TruffleObject) {
                Object clazz = RContext.getInstance().getEnv().asHostObject(result);
                if (clazz instanceof Class) {
                    return (Class<?>) clazz;
                }
            }
            interopExceptionProfile.enter();
            if (result instanceof RuntimeException) {
                throw RError.handleInteropException(this, (RuntimeException) result);
            } else {
                assert result instanceof Throwable : "class " + className + " resulted into " + (result == null ? "NULL" : result.getClass().getName());
                throw RError.handleInteropException(this, new RuntimeException((Throwable) result));
            }
        }

        protected boolean isJavaObject(TruffleObject obj) {
            return RContext.getInstance().getEnv().isHostObject(obj);
        }

        protected boolean isJavaLikeVector(RAbstractVector vec) {
            return vec instanceof RAbstractLogicalVector ||
                            vec instanceof RAbstractIntVector ||
                            vec instanceof RAbstractDoubleVector ||
                            vec instanceof RAbstractStringVector ||
                            vec instanceof RAbstractRawVector;
        }
    }

    @ImportStatic({Message.class, RRuntime.class})
    @RBuiltin(name = ".fastr.interop.fromArray", visibility = ON, kind = PRIMITIVE, parameterNames = {"array", "recursive"}, behavior = COMPLEX)
    public abstract static class FromForeignArray extends RBuiltinNode.Arg2 {

        static {
            Casts casts = new Casts(FromForeignArray.class);
            casts.arg("array").castForeignObjects(false).mustNotBeMissing();
            casts.arg("recursive").mapMissing(Predef.constant(RRuntime.LOGICAL_FALSE)).mustBe(logicalValue().or(Predef.nullValue())).asLogicalVector().mustBe(singleElement()).findFirst().mustBe(
                            notLogicalNA()).map(Predef.toBoolean());
        }

        private final ConditionProfile isArrayProfile = ConditionProfile.createBinaryProfile();

        @Specialization(guards = {"isForeignObject(obj)"})
        @TruffleBoundary
        public Object fromArray(TruffleObject obj, boolean recursive,
                        @Cached("HAS_SIZE.createNode()") Node hasSize,
                        @Cached("create()") ForeignArray2R array2R) {
            if (isArrayProfile.profile(ForeignAccess.sendHasSize(hasSize, obj))) {
                return array2R.convert(obj, recursive);
            } else {
                throw error(RError.Message.GENERIC, "not a java array");
            }
        }

        @Fallback
        public Object fromObject(@SuppressWarnings("unused") Object obj, @SuppressWarnings("unused") Object recursive) {
            throw error(RError.Message.GENERIC, "not a java array");
        }
    }

    @ImportStatic({Message.class, RRuntime.class})
    @RBuiltin(name = ".fastr.interop.new", visibility = ON, kind = PRIMITIVE, parameterNames = {"class", "..."}, behavior = COMPLEX)
    public abstract static class InteropNew extends RBuiltinNode.Arg2 {

        static {
            Casts.noCasts(InteropNew.class);
        }

        @Specialization(guards = {"isForeignObject(clazz)"})
        @TruffleBoundary
        public Object interopNew(TruffleObject clazz, RArgsValuesAndNames args,
                        @Cached("createNew(0).createNode()") Node sendNew,
                        @Cached("create()") R2Foreign r2Foreign,
                        @Cached("create()") Foreign2R foreign2R) {
            try {
                Object[] argValues = new Object[args.getLength()];
                for (int i = 0; i < argValues.length; i++) {
                    argValues[i] = r2Foreign.execute(args.getArgument(i));
                }
                Object result = ForeignAccess.sendNew(sendNew, clazz, argValues);
                return foreign2R.execute(result);
            } catch (UnsupportedTypeException ute) {
                RContext context = RContext.getInstance();
                Env env = RContext.getInstance().getEnv();
                if (env.isHostObject(clazz)) {
                    Object obj = env.asHostObject(clazz);
                    if (obj instanceof Class) {
                        Class<?> cls = (Class<?>) obj;
                        if (cls.isArray()) {
                            // TODO temporary hot fix
                            // need ForeignAccess.sendNew(multiDimArrayClass, dims)
                            Object arg0 = args.getArgument(0);
                            if (arg0 instanceof RAbstractIntVector) {
                                RAbstractIntVector vec = (RAbstractIntVector) arg0;
                                int[] dims = new int[vec.getLength()];

                                for (int i = 0; i < vec.getLength(); i++) {
                                    Array.setInt(dims, i, vec.getDataAt(i));
                                }
                                cls = cls.getComponentType();
                                while (cls.isArray()) {
                                    cls = cls.getComponentType();
                                }

                                Object a = Array.newInstance(cls, dims);
                                return context.getEnv().asGuestValue(a);
                            }
                        }
                    }
                }
                String msg = isTesting ? "error during Java object instantiation" : "error during Java object instantiation: " + ute.getMessage();
                throw error(RError.Message.GENERIC, msg);
            } catch (IllegalStateException | SecurityException | IllegalArgumentException | ArityException | UnsupportedMessageException e) {
                String msg = isTesting ? "error during Java object instantiation" : "error during Java object instantiation: " + e.getMessage();
                throw error(RError.Message.GENERIC, msg);
            } catch (RuntimeException e) {
                throw RError.handleInteropException(this, e);
            }
        }

        @Fallback
        public Object interopNew(@SuppressWarnings("unused") Object clazz, @SuppressWarnings("unused") Object args) {
            throw error(RError.Message.GENERIC, "interop object needed as receiver of NEW message");
        }
    }

    @ImportStatic(RRuntime.class)
    @RBuiltin(name = "is.polyglot.value", visibility = ON, kind = PRIMITIVE, parameterNames = {"obj"}, behavior = COMPLEX)
    public abstract static class IsExternal extends RBuiltinNode.Arg1 {

        static {
            Casts.noCasts(IsExternal.class);
        }

        @Specialization(guards = {"isForeignObject(obj)"})
        public byte isExternal(@SuppressWarnings("unused") TruffleObject obj) {
            return RRuntime.LOGICAL_TRUE;
        }

        @Fallback
        public byte isExternal(@SuppressWarnings("unused") Object obj) {
            return RRuntime.LOGICAL_FALSE;
        }
    }

    private static Object classForName(String className) {
        return classForName(className, false);
    }

    private static Object classForName(String className, boolean silent) {
        Env env = RContext.getInstance().getEnv();
        if (env != null && env.isHostLookupAllowed()) {
            try {
                Object found = env.lookupHostSymbol(demangle(className));
                if (found != null) {
                    return found;
                }
            } catch (Exception ex) {
            }
        } else {
            throw RError.error(RError.SHOW_CALLER, RError.Message.GENERIC,
                            "Java Interop is not available, please run FastR with --jvm, e.g. '$bin/R --jvm CMD INSTALL' or '$bin/Rscript --jvm myscript.R'");
        }
        return silent ? RNull.instance : new ClassNotFoundException(className + " not found");
    }

    @TruffleBoundary
    private static String demangle(String className) {
        return className.replaceAll("/", ".");
    }

    @RBuiltin(name = ".fastr.interop.try", kind = PRIMITIVE, parameterNames = {"function", "check"}, behavior = COMPLEX)
    public abstract static class FastRInteropTry extends RBuiltinNode.Arg2 {
        @Node.Child private RExplicitCallNode call = RExplicitCallNode.create();

        static {
            Casts casts = new Casts(FastRInteropTry.class);
            casts.arg("function").mustBe(instanceOf(RFunction.class));
            casts.arg("check").mustBe(logicalValue()).asLogicalVector().mustBe(singleElement()).findFirst();
        }

        @Specialization
        public Object tryFunc(VirtualFrame frame, RFunction function, byte check) {
            getInteropTryState().stepIn();
            try {
                return call.call(frame, function, RArgsValuesAndNames.EMPTY);
            } catch (FastRInteropTryException e) {
                CompilerDirectives.transferToInterpreter();
                Throwable cause = e.getCause();
                if (cause instanceof TruffleException || cause.getCause() instanceof ClassNotFoundException) {
                    cause = cause.getCause();
                    if (RRuntime.fromLogical(check)) {
                        String causeName = cause.getClass().getName();
                        String msg = cause.getMessage();
                        msg = msg != null ? String.format("%s: %s", causeName, msg) : causeName;
                        throw RError.error(RError.SHOW_CALLER, RError.Message.GENERIC, msg);
                    } else {
                        getInteropTryState().lastException = cause;
                    }
                } else {
                    RInternalError.reportError(e);
                }
            } finally {
                getInteropTryState().stepOut();
            }
            return RNull.instance;
        }

    }

    @RBuiltin(name = ".fastr.interop.checkException", kind = PRIMITIVE, parameterNames = {"silent", "showCallerOf"}, behavior = COMPLEX)
    public abstract static class FastRInteropCheckException extends RBuiltinNode.Arg2 {
        static {
            Casts casts = new Casts(FastRInteropCheckException.class);
            casts.arg("silent").mustBe(logicalValue()).asLogicalVector().mustBe(singleElement()).findFirst();
            casts.arg("showCallerOf").allowMissing().mustBe(stringValue()).asStringVector().mustBe(singleElement()).findFirst();
        }

        @Specialization
        public Object getException(byte silent, @SuppressWarnings("unused") RMissing callerFor) {
            return getException(silent, (String) null);
        }

        @Specialization
        public Object getException(byte silent, String showCallerOf) {
            Throwable t = getInteropTryState().lastException;
            if (t != null) {
                CompilerDirectives.transferToInterpreter();
                getInteropTryState().lastException = null;
                if (!RRuntime.fromLogical(silent)) {
                    String causeName = t.getClass().getName();
                    String msg = t.getMessage();
                    msg = msg != null ? String.format("%s: %s", causeName, msg) : causeName;
                    if (showCallerOf == null) {
                        throw RError.error(RError.SHOW_CALLER, RError.Message.GENERIC, msg);
                    } else {
                        throw RError.error(new RError.ShowCallerOf(showCallerOf), RError.Message.GENERIC, msg);
                    }
                }
            }
            return RNull.instance;
        }
    }

    @RBuiltin(name = ".fastr.interop.getTryException", kind = PRIMITIVE, parameterNames = {"clear"}, behavior = COMPLEX)
    public abstract static class FastRInteropGetException extends RBuiltinNode.Arg1 {
        static {
            Casts casts = new Casts(FastRInteropGetException.class);
            casts.arg("clear").mustBe(logicalValue()).asLogicalVector().mustBe(singleElement()).findFirst();
        }

        @Specialization
        public Object getException(byte clear) {
            Throwable ret = getInteropTryState().lastException;
            if (RRuntime.fromLogical(clear)) {
                getInteropTryState().lastException = null;
            }
            return ret != null ? RContext.getInstance().getEnv().asGuestValue(ret) : RNull.instance;
        }
    }

    @RBuiltin(name = ".fastr.interop.clearTryException", kind = PRIMITIVE, parameterNames = {}, behavior = COMPLEX)
    public abstract static class FastRInteropClearException extends RBuiltinNode.Arg0 {

        static {
            NodeWithArgumentCasts.Casts.noCasts(FastRInteropClearException.class);
        }

        @Specialization
        public Object clearException() {
            getInteropTryState().lastException = null;
            return RNull.instance;
        }
    }

    private static FastrInteropTryContextState getInteropTryState() {
        return RContext.getInstance().stateInteropTry;
    }

}
