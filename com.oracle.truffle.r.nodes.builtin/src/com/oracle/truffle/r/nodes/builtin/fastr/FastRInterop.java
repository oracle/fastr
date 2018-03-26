/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.fastr;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.doubleValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.integerValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.notLogicalNA;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.numericValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.rawValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.typeName;
import static com.oracle.truffle.r.runtime.RVisibility.CUSTOM;
import static com.oracle.truffle.r.runtime.RVisibility.OFF;
import static com.oracle.truffle.r.runtime.RVisibility.ON;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.MalformedURLException;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleException;
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
import com.oracle.truffle.api.interop.java.JavaInterop;
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
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractRawVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.GetReadonlyData;
import com.oracle.truffle.r.runtime.interop.FastRInteropTryException;
import com.oracle.truffle.r.runtime.interop.FastrInteropTryContextState;
import com.oracle.truffle.r.runtime.interop.Foreign2R;
import com.oracle.truffle.r.runtime.interop.ForeignArray2R;
import com.oracle.truffle.r.runtime.interop.R2Foreign;
import com.oracle.truffle.r.runtime.interop.R2ForeignNodeGen;

public class FastRInterop {

    private static boolean isTesting = false;

    public static void testingMode() {
        isTesting = true;
    }

    @RBuiltin(name = "eval.external", visibility = CUSTOM, kind = PRIMITIVE, parameterNames = {"mimeType", "source", "path"}, behavior = COMPLEX)
    public abstract static class Eval extends RBuiltinNode.Arg3 {

        static {
            Casts casts = new Casts(Eval.class);
            casts.arg("mimeType").allowMissing().mustBe(stringValue()).asStringVector().mustBe(singleElement()).findFirst();
            casts.arg("source").allowMissing().mustBe(stringValue()).asStringVector().mustBe(singleElement()).findFirst();
            casts.arg("path").allowMissing().mustBe(stringValue()).asStringVector().mustBe(singleElement()).findFirst();
        }

        @Child private SetVisibilityNode setVisibilityNode = SetVisibilityNode.create();
        @Child private Foreign2R foreign2rNode = Foreign2R.create();

        protected DirectCallNode createCall(String mimeType, String source) {
            return Truffle.getRuntime().createDirectCallNode(parse(mimeType, source));
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"cachedMimeType != null", "cachedMimeType.equals(mimeType)", "cachedSource != null", "cachedSource.equals(source)"})
        protected Object evalCached(VirtualFrame frame, String mimeType, String source, RMissing path,
                        @Cached("mimeType") String cachedMimeType,
                        @Cached("source") String cachedSource,
                        @Cached("createCall(mimeType, source)") DirectCallNode call) {
            try {
                return foreign2rNode.execute(call.call(EMPTY_OBJECT_ARRAY));
            } finally {
                setVisibilityNode.execute(frame, true);
            }
        }

        @Specialization(replaces = "evalCached")
        protected Object eval(VirtualFrame frame, String mimeType, String source, @SuppressWarnings("unused") RMissing path) {
            try {
                return foreign2rNode.execute(parseAndCall(source, mimeType));
            } finally {
                setVisibilityNode.execute(frame, true);
            }
        }

        @TruffleBoundary
        private Object parseAndCall(String source, String mimeType) {
            return parse(mimeType, source).call();
        }

        @Specialization()
        @TruffleBoundary
        protected Object eval(@SuppressWarnings("unused") RMissing mimeType, @SuppressWarnings("unused") String source, @SuppressWarnings("unused") RMissing path) {
            throw RError.error(this, RError.Message.INVALID_ARG, "mimeType");
        }

        protected CallTarget parse(String mimeType, String source) {
            CompilerAsserts.neverPartOfCompilation();

            Source sourceObject = RSource.fromTextInternalInvisible(source, RSource.Internal.EVAL_WRAPPER, mimeType);
            try {
                return RContext.getInstance().getEnv().parse(sourceObject);
            } catch (Throwable t) {
                throw error(RError.Message.GENERIC, "Error while parsing: " + t.getMessage());
            }
        }

        @Specialization
        protected Object eval(VirtualFrame frame, String mimeType, @SuppressWarnings("unused") String source, String path) {
            try {
                return foreign2rNode.execute(parseFileAndCall(path, mimeType));
            } finally {
                setVisibilityNode.execute(frame, false);
            }
        }

        @Specialization
        protected Object eval(VirtualFrame frame, String mimeType, @SuppressWarnings("unused") RMissing source, String path) {
            try {
                return foreign2rNode.execute(parseFileAndCall(path, mimeType));
            } finally {
                setVisibilityNode.execute(frame, false);
            }
        }

        @Specialization
        protected Object eval(VirtualFrame frame, @SuppressWarnings("unused") RMissing mimeType, @SuppressWarnings("unused") RMissing source, String path) {
            try {
                return foreign2rNode.execute(parseFileAndCall(path, null));
            } finally {
                setVisibilityNode.execute(frame, false);
            }
        }

        @TruffleBoundary
        private Object parseFileAndCall(String path, String mimeType) {
            return parseFile(path, mimeType).call();
        }

        protected CallTarget parseFile(String path, String mimeType) {
            CompilerAsserts.neverPartOfCompilation();

            File file = new File(path);
            try {
                Builder<IOException, RuntimeException, RuntimeException> sourceBuilder = Source.newBuilder(file).name(file.getName()).internal();
                if (mimeType != null) {
                    sourceBuilder.mimeType(mimeType);
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
        protected Object eval(@SuppressWarnings("unused") RMissing source, @SuppressWarnings("unused") RMissing mimeType, @SuppressWarnings("unused") RMissing path) {
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

    @RBuiltin(name = "is.external.null", visibility = ON, kind = PRIMITIVE, parameterNames = {"value"}, behavior = COMPLEX)
    public abstract static class IsNull extends RBuiltinNode.Arg1 {

        @Child private Node node = Message.IS_NULL.createNode();

        static {
            Casts.noCasts(IsNull.class);
        }

        @Specialization
        public byte isNull(TruffleObject obj) {
            return RRuntime.asLogical(ForeignAccess.sendIsNull(node, obj));
        }

        @Fallback
        public byte isNull(@SuppressWarnings("unused") Object obj) {
            return RRuntime.asLogical(false);
        }
    }

    @RBuiltin(name = "is.external.executable", visibility = ON, kind = PRIMITIVE, parameterNames = {"value"}, behavior = COMPLEX)
    public abstract static class IsExecutable extends RBuiltinNode.Arg1 {

        @Child private Node node = Message.IS_EXECUTABLE.createNode();

        static {
            Casts.noCasts(IsExecutable.class);
        }

        @Specialization
        public byte isExecutable(TruffleObject obj) {
            return RRuntime.asLogical(ForeignAccess.sendIsExecutable(node, obj));
        }

        @Fallback
        public byte isExecutable(@SuppressWarnings("unused") Object obj) {
            return RRuntime.asLogical(false);
        }
    }

    @RBuiltin(name = "as.external.byte", visibility = ON, kind = PRIMITIVE, parameterNames = {"value"}, behavior = COMPLEX)
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

    @RBuiltin(name = "as.external.char", visibility = ON, kind = PRIMITIVE, parameterNames = {"value", "pos"}, behavior = COMPLEX)
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

    @RBuiltin(name = "as.external.float", visibility = ON, kind = PRIMITIVE, parameterNames = {"value"}, behavior = COMPLEX)
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

    @RBuiltin(name = "as.external.long", visibility = ON, kind = PRIMITIVE, parameterNames = {"value"}, behavior = COMPLEX)
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

    @RBuiltin(name = "as.external.short", visibility = ON, kind = PRIMITIVE, parameterNames = {"value"}, behavior = COMPLEX)
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

    @RBuiltin(name = ".fastr.interop.toBoolean", visibility = ON, kind = PRIMITIVE, parameterNames = {"value"}, behavior = COMPLEX)
    public abstract static class ToBoolean extends RBuiltinNode.Arg1 {

        static {
            Casts casts = new Casts(ToBoolean.class);
            casts.arg("value").mustBe(logicalValue()).asLogicalVector().mustBe(singleElement()).findFirst().mustBe(notLogicalNA()).map(Predef.toBoolean());
        }

        @Specialization
        public boolean toBoolean(boolean value) {
            return value;
        }
    }

    @RBuiltin(name = "new.java.class", visibility = ON, kind = PRIMITIVE, parameterNames = {"class", "silent"}, behavior = COMPLEX)
    public abstract static class JavaClass extends RBuiltinNode.Arg2 {

        private static final Object accessError = new Object();

        static {
            Casts casts = new Casts(JavaClass.class);
            casts.arg("class").mustBe(stringValue()).asStringVector().mustBe(Predef.singleElement()).findFirst();
            casts.arg("silent").mapMissing(Predef.constant(RRuntime.LOGICAL_FALSE)).mustBe(logicalValue().or(Predef.nullValue())).asLogicalVector().mustBe(singleElement()).findFirst().mustBe(
                            notLogicalNA()).map(Predef.toBoolean());
        }

        @Specialization
        @TruffleBoundary
        public TruffleObject javaClass(TruffleObject obj, boolean silent) {
            if (JavaInterop.isJavaObject(obj)) {
                return JavaInterop.toJavaClass(obj);
            }
            throw error(RError.Message.GENERIC, "unsupported type " + obj.getClass().getName());
        }

        protected boolean isClass(Object obj) {
            return obj != null && obj instanceof Class;
        }

        @Specialization(guards = {"isClass(clazz)", "clazz.equals(cachedClazz)"}, limit = "10")
        public TruffleObject javaClassCached(String clazz, boolean silent,
                        @Cached("clazz") String cachedClazz,
                        @Cached("getJavaClass(clazz, silent)") Object result,
                        @Cached("create()") BranchProfile interopExceptionProfile) {
            return javaClassToTruffleObject(clazz, result, interopExceptionProfile);
        }

        @Specialization(replaces = "javaClassCached")
        public TruffleObject javaClass(String clazz, boolean silent,
                        @Cached("create()") BranchProfile interopExceptionProfile) {
            Object result = getJavaClass(clazz, silent);
            return javaClassToTruffleObject(clazz, result, interopExceptionProfile);
        }

        @TruffleBoundary
        private TruffleObject javaClassToTruffleObject(String clazz, Object result, BranchProfile interopExceptionProfile) {
            if (result == RNull.instance) {
                return RNull.instance;
            }
            if (result instanceof Class<?>) {
                return JavaInterop.asTruffleObject(result);
            } else if (result == accessError) {
                CompilerDirectives.transferToInterpreter();
                throw error(RError.Message.GENERIC, "error while accessing Java class: " + clazz);
            } else {
                interopExceptionProfile.enter();
                if (result instanceof RuntimeException) {
                    throw RError.handleInteropException(this, (RuntimeException) result);
                } else {
                    assert result instanceof Throwable;
                    throw RError.handleInteropException(this, new RuntimeException((Throwable) result));
                }
            }
        }

        protected static Object getJavaClass(String className, boolean silent) {
            Class<?> clazz = getPrimitiveClass(className);
            if (clazz != null) {
                return clazz;
            }
            return loadClass(className, silent);
        }

        @TruffleBoundary
        private static Object loadClass(String clazz, boolean silent) {
            try {
                Class<?> result = RContext.getInstance().loadClass(clazz);
                if (result == null) {
                    // not found
                    if (silent) {
                        return RNull.instance;
                    } else {
                        return new ClassNotFoundException(clazz + " not found");
                    }
                }
                return result;
            } catch (SecurityException | IllegalArgumentException e) {
                return accessError;
            } catch (RuntimeException e) {
                if (silent) {
                    return RNull.instance;
                }
                return e;
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
            try {
                RContext ctx = RContext.getInstance();
                String[] entriesArr = new String[value.getLength()];
                for (int i = 0; i < value.getLength(); i++) {
                    entriesArr[i] = value.getDataAt(i);
                }
                ctx.addInteropClasspathEntries(entriesArr);
                return value;
            } catch (MalformedURLException e) {
                if (silent) {
                    return RNull.instance;
                }
                throw error(RError.Message.GENERIC, "error while adding classpath entry: " + e.getMessage());
            }
        }
    }

    @RBuiltin(name = "java.classpath", visibility = ON, kind = PRIMITIVE, parameterNames = {}, behavior = COMPLEX)
    public abstract static class JavaClasspath extends RBuiltinNode.Arg0 {

        static {
            Casts.noCasts(JavaClasspath.class);
        }

        @Specialization
        @TruffleBoundary
        public RAbstractStringVector getEntries() {
            RContext ctx = RContext.getInstance();
            String[] paths = ctx.getInteropClasspathEntries();
            return RDataFactory.createStringVector(paths, true);
        }
    }

    @ImportStatic({RRuntime.class})
    @RBuiltin(name = "java.class", visibility = ON, kind = PRIMITIVE, parameterNames = {"obj", "getClassName"}, behavior = COMPLEX)
    public abstract static class JavaClassName extends RBuiltinNode.Arg2 {

        static {
            Casts casts = new Casts(JavaClassName.class);
            casts.arg("getClassName").mapMissing(Predef.constant(RRuntime.LOGICAL_FALSE)).mustBe(logicalValue().or(Predef.nullValue())).asLogicalVector().mustBe(singleElement()).findFirst().mustBe(
                            notLogicalNA()).map(Predef.toBoolean());
        }

        @Specialization(guards = {"isJavaObject(obj)"})
        @TruffleBoundary
        public Object javaClassName(Object obj, boolean getClassName) {
            if (isJavaObject(obj)) {
                Object o = JavaInterop.asJavaObject(Object.class, (TruffleObject) obj);
                if (o == null) {
                    return RNull.instance;
                }
                if (getClassName && o instanceof Class) {
                    return ((Class<?>) o).getName();
                }
                return o.getClass().getName();
            } else {
                throw error(RError.Message.GENERIC, "unsupported type " + obj.getClass().getName());
            }
        }

        protected boolean isJavaObject(Object obj) {
            return RRuntime.isForeignObject(obj) && JavaInterop.isJavaObject(obj);
        }

        @Fallback
        public String javaClassName(@SuppressWarnings("unused") Object obj,
                        @SuppressWarnings("unused") Object getClassName) {
            throw error(RError.Message.GENERIC, "unsupported type " + obj.getClass().getName());
        }
    }

    @ImportStatic({Message.class, RRuntime.class})
    @RBuiltin(name = "is.external.array", visibility = ON, kind = PRIMITIVE, parameterNames = {"obj"}, behavior = COMPLEX)
    public abstract static class IsForeignArray extends RBuiltinNode.Arg1 {

        static {
            Casts.noCasts(IsForeignArray.class);
        }

        @Specialization(guards = {"isForeignObject(obj)"})
        public byte isArray(TruffleObject obj,
                        @Cached("HAS_SIZE.createNode()") Node hasSize) {
            return RRuntime.asLogical(ForeignAccess.sendHasSize(hasSize, obj));
        }

        @Fallback
        public byte isArray(@SuppressWarnings("unused") Object obj) {
            return RRuntime.LOGICAL_FALSE;
        }
    }

    @RBuiltin(name = ".fastr.interop.getJavaClass", visibility = ON, kind = PRIMITIVE, parameterNames = {"obj"}, behavior = COMPLEX)
    public abstract static class GetJavaClass extends RBuiltinNode.Arg1 {

        static {
            Casts.noCasts(GetJavaClass.class);
        }

        @Specialization
        @TruffleBoundary
        public TruffleObject javaClass(TruffleObject obj) {
            if (JavaInterop.isJavaObject(obj)) {
                return JavaInterop.toJavaClass(obj);
            }
            throw error(RError.Message.GENERIC, "unsupported type " + obj.getClass().getName());
        }
    }

    @ImportStatic({Message.class, RRuntime.class})
    @RBuiltin(name = ".fastr.interop.isIdentical", visibility = ON, kind = PRIMITIVE, parameterNames = {"x1", "x2"}, behavior = COMPLEX)
    public abstract static class JavaIsIdentical extends RBuiltinNode.Arg2 {

        static {
            Casts.noCasts(JavaIsIdentical.class);
        }

        @Specialization(guards = {"isJavaObject(x1)", "isJavaObject(x2)"})
        public byte isIdentical(TruffleObject x1, TruffleObject x2) {
            return RRuntime.asLogical(JavaInterop.asJavaObject(Object.class, x1) == JavaInterop.asJavaObject(Object.class, x2));
        }

        @Fallback
        @TruffleBoundary
        public byte isIdentical(@SuppressWarnings("unused") Object x1, @SuppressWarnings("unused") Object x2) {
            throw error(RError.Message.GENERIC, String.format("unsupported types: %s, %s", x1.getClass().getName(), x2.getClass().getName()));
        }

        protected boolean isJavaObject(TruffleObject obj) {
            return JavaInterop.isJavaObject(obj);
        }
    }

    @ImportStatic({Message.class, RRuntime.class})
    @RBuiltin(name = ".fastr.interop.asJavaTruffleObject", visibility = ON, kind = PRIMITIVE, parameterNames = {"x"}, behavior = COMPLEX)
    public abstract static class JavaAsTruffleObject extends RBuiltinNode.Arg1 {

        static {
            Casts.noCasts(JavaAsTruffleObject.class);
        }

        @Specialization
        public TruffleObject asTruffleObject(byte b) {
            return JavaInterop.asTruffleObject(RRuntime.fromLogical(b));
        }

        @Specialization
        public TruffleObject asTruffleObject(int i) {
            return JavaInterop.asTruffleObject(i);
        }

        @Specialization
        public TruffleObject asTruffleObject(double d) {
            return JavaInterop.asTruffleObject(d);
        }

        @Specialization
        public TruffleObject asTruffleObject(String s) {
            return JavaInterop.asTruffleObject(s);
        }

        @Specialization
        public TruffleObject asTruffleObject(RInteropByte b) {
            return JavaInterop.asTruffleObject(b.getValue());
        }

        @Specialization
        public TruffleObject asTruffleObject(RInteropChar c) {
            return JavaInterop.asTruffleObject(c.getValue());
        }

        @Specialization
        public TruffleObject asTruffleObject(RInteropFloat f) {
            return JavaInterop.asTruffleObject(f.getValue());
        }

        @Specialization
        public TruffleObject asTruffleObject(RInteropLong l) {
            return JavaInterop.asTruffleObject(l.getValue());
        }

        @Specialization
        public TruffleObject asTruffleObject(RInteropShort s) {
            return JavaInterop.asTruffleObject(s.getValue());
        }

        @Fallback
        @TruffleBoundary
        public byte asTruffleObject(@SuppressWarnings("unused") Object x) {
            throw error(RError.Message.GENERIC, String.format("unsupported type: %s", x.getClass().getName()));
        }
    }

    @ImportStatic({Message.class, RRuntime.class})
    @RBuiltin(name = ".fastr.interop.isAssignableFrom", visibility = ON, kind = PRIMITIVE, parameterNames = {"x1", "x2"}, behavior = COMPLEX)
    public abstract static class JavaIsAssignableFrom extends RBuiltinNode.Arg2 {

        static {
            Casts.noCasts(JavaIsAssignableFrom.class);
        }

        @Specialization(guards = {"isJavaObject(x1)", "isJavaObject(x2)"})
        public byte isAssignable(TruffleObject x1, TruffleObject x2) {
            Object jo1 = JavaInterop.asJavaObject(Object.class, x1);
            Class<?> cl1 = (jo1 instanceof Class) ? (Class<?>) jo1 : jo1.getClass();
            Object jo2 = JavaInterop.asJavaObject(Object.class, x2);
            Class<?> cl2 = (jo2 instanceof Class) ? (Class<?>) jo2 : jo2.getClass();
            return RRuntime.asLogical(cl2.isAssignableFrom(cl1));
        }

        @Fallback
        @TruffleBoundary
        public byte isAssignable(@SuppressWarnings("unused") Object x1, @SuppressWarnings("unused") Object x2) {
            throw error(RError.Message.GENERIC, String.format("unsupported types: %s, %s", x1.getClass().getName(), x2.getClass().getName()));
        }

        protected boolean isJavaObject(TruffleObject obj) {
            return JavaInterop.isJavaObject(obj);
        }
    }

    @ImportStatic({Message.class, RRuntime.class})
    @RBuiltin(name = ".fastr.interop.isInstance", visibility = ON, kind = PRIMITIVE, parameterNames = {"x1", "x2"}, behavior = COMPLEX)
    public abstract static class JavaIsInstance extends RBuiltinNode.Arg2 {

        static {
            Casts.noCasts(JavaIsInstance.class);
        }

        @Specialization(guards = {"isJavaObject(x1)", "isJavaObject(x2)"})
        public byte isInstance(TruffleObject x1, TruffleObject x2) {
            Object jo1 = JavaInterop.asJavaObject(Object.class, x1);
            Object jo2 = JavaInterop.asJavaObject(Object.class, x2);
            if (jo1 instanceof Class) {
                Class<?> cl1 = (Class<?>) jo1;
                return RRuntime.asLogical(cl1.isInstance(jo2));
            }
            return RRuntime.asLogical(jo1.getClass().isInstance(jo2));
        }

        @Specialization(guards = {"isJavaObject(x1)"})
        public byte isInstance(TruffleObject x1, RInteropScalar x2,
                        @Cached("createR2Foreign()") R2Foreign r2Foreign) {
            Object jo1 = JavaInterop.asJavaObject(Object.class, x1);
            if (jo1 instanceof Class) {
                Class<?> cl1 = (Class<?>) jo1;
                return RRuntime.asLogical(cl1.isInstance(r2Foreign.execute(x2)));
            }
            return RRuntime.asLogical(jo1.getClass().isInstance(x2));
        }

        @Specialization(guards = {"isJavaObject(x1)", "!isJavaObject(x2)", "!isInterop(x2)"})
        public byte isInstance(TruffleObject x1, Object x2) {
            Object jo1 = JavaInterop.asJavaObject(Object.class, x1);
            if (jo1 instanceof Class) {
                Class<?> cl1 = (Class<?>) jo1;
                return RRuntime.asLogical(cl1.isInstance(x2));
            }
            return RRuntime.asLogical(jo1.getClass().isInstance(x2));
        }

        @Fallback
        @TruffleBoundary
        public byte isInstance(@SuppressWarnings("unused") Object x1, @SuppressWarnings("unused") Object x2) {
            throw error(RError.Message.GENERIC, String.format("unsupported types: %s, %s", x1.getClass().getName(), x2.getClass().getName()));
        }

        protected boolean isJavaObject(Object obj) {
            return RRuntime.isForeignObject(obj) && JavaInterop.isJavaObject((TruffleObject) obj);
        }

        protected boolean isInterop(Object obj) {
            return obj instanceof RInteropScalar;
        }

        protected R2Foreign createR2Foreign() {
            return R2ForeignNodeGen.create();
        }
    }

    @RBuiltin(name = "new.java.array", visibility = ON, kind = PRIMITIVE, parameterNames = {"class", "dim"}, behavior = COMPLEX)
    public abstract static class NewJavaArray extends RBuiltinNode.Arg2 {

        static {
            Casts casts = new Casts(NewJavaArray.class);
            casts.arg("class").mustBe(stringValue()).asStringVector().mustBe(Predef.singleElement()).findFirst();
            casts.arg("dim").mustBe(integerValue().or(doubleValue()), RError.Message.INVALID_ARGUMENT_OF_TYPE, "dim", typeName()).asIntegerVector();
        }

        @Specialization
        @TruffleBoundary
        public Object newArray(String clazz, int length) {
            return JavaInterop.asTruffleObject(Array.newInstance(getClazz(clazz), length));
        }

        @Specialization
        @TruffleBoundary
        public Object newArray(String clazz, RAbstractIntVector dim) {
            int[] dima = new int[dim.getLength()];
            for (int i = 0; i < dima.length; i++) {
                dima[i] = dim.getDataAt(i);
            }
            return JavaInterop.asTruffleObject(Array.newInstance(getClazz(clazz), dima));
        }

        private Class<?> getClazz(String className) throws RError {
            Class<?> result = classForName(className);
            if (result == null) {
                throw error(RError.Message.GENERIC, "cannot access Java class %s", className);
            }
            return result;
        }
    }

    @ImportStatic({Message.class, RRuntime.class})
    @RBuiltin(name = "as.java.array", visibility = ON, kind = PRIMITIVE, parameterNames = {"x", "className", "flat"}, behavior = COMPLEX)
    public abstract static class ToJavaArray extends RBuiltinNode.Arg3 {

        static {
            Casts casts = new Casts(ToJavaArray.class);
            casts.arg("x").castForeignObjects(false).mustNotBeMissing();
            casts.arg("className").allowMissing().mustBe(stringValue()).asStringVector().mustBe(Predef.singleElement()).findFirst();
            casts.arg("flat").mapMissing(Predef.constant(RRuntime.LOGICAL_TRUE)).mustBe(logicalValue().or(Predef.nullValue())).asLogicalVector().mustBe(singleElement()).findFirst().mustBe(
                            notLogicalNA()).map(Predef.toBoolean());
        }

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
            return R2ForeignNodeGen.create();
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
            final Object array = Array.newInstance(clazz, dims);
            for (int d = 0; d < dims.length; d++) {
                int dim = dims[d];
                // TODO works only for flat
                for (int i = 0; i < dim; i++) {
                    vecToArray.toArray(array, i);
                }
            }
            return JavaInterop.asTruffleObject(array);
        }

        private Object toArray(RAbstractVector vec, boolean flat, Class<?> clazz, R2Foreign r2Foreign) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
            int[] dims = getDim(flat, vec);
            final Object array = Array.newInstance(clazz, dims);
            TruffleObject truffleArray = JavaInterop.asTruffleObject(array);

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
                Object o = JavaInterop.asJavaObject(Object.class, obj);
                if (o == null) {
                    return obj;
                }
                TruffleObject array = JavaInterop.asTruffleObject(Array.newInstance(o.getClass(), 1));
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

        private Class<?> getClazz(String className) throws RError {
            Class<?> result = classForName(className);
            if (result == null) {
                throw error(RError.Message.GENERIC, "cannot access Java class %s", className);
            }
            return result;
        }

        protected boolean isJavaObject(TruffleObject obj) {
            return JavaInterop.isJavaObject(obj);
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
    @RBuiltin(name = "new.external", visibility = ON, kind = PRIMITIVE, parameterNames = {"class", "..."}, behavior = COMPLEX)
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
            } catch (IllegalStateException | SecurityException | IllegalArgumentException | UnsupportedTypeException | ArityException | UnsupportedMessageException e) {
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
    @RBuiltin(name = "is.external", visibility = ON, kind = PRIMITIVE, parameterNames = {"obj"}, behavior = COMPLEX)
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

    private static Class<?> classForName(String className) {
        Class<?> clazz = getPrimitiveClass(className);
        if (clazz != null) {
            return clazz;
        }
        return RContext.getInstance().loadClass(className);
    }

    private static Class<?> getPrimitiveClass(String className) {
        if (Boolean.TYPE.getName().equals(className)) {
            return Boolean.TYPE;
        } else if (Byte.TYPE.getName().equals(className)) {
            return Byte.TYPE;
        } else if (Character.TYPE.getName().equals(className)) {
            return Character.TYPE;
        } else if (Double.TYPE.getName().equals(className)) {
            return Double.TYPE;
        } else if (Float.TYPE.getName().equals(className)) {
            return Float.TYPE;
        } else if (Integer.TYPE.getName().equals(className)) {
            return Integer.TYPE;
        } else if (Long.TYPE.getName().equals(className)) {
            return Long.TYPE;
        } else if (Short.TYPE.getName().equals(className)) {
            return Short.TYPE;
        }
        return null;
    }

    @RBuiltin(name = "do.call.external", visibility = ON, kind = PRIMITIVE, parameterNames = {"receiver", "what", "args"}, behavior = COMPLEX)
    public abstract static class DoCallExternal extends RBuiltinNode.Arg3 {

        static {
            Casts casts = new Casts(DoCallExternal.class);
            casts.arg("args").mustBe(RAbstractListVector.class, RError.Message.GENERIC, "third argument must be a list");
        }

        @Child private GetReadonlyData.ListData getDataNode;

        protected Node createInvoke(int nargs) {
            return Message.createInvoke(nargs).createNode();
        }

        @Specialization
        public Object invoke(TruffleObject obj, String what, RAbstractListVector args,
                        @Cached("create()") Foreign2R foreign2R,
                        @Cached("create()") R2Foreign r2Foreign,
                        @Cached("createInvoke(args.getLength())") Node invokeNode) {

            if (getDataNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getDataNode = insert(GetReadonlyData.ListData.create());
            }

            Object[] argValues = getDataNode.execute(args);
            for (int i = 0; i < argValues.length; i++) {
                argValues[i] = r2Foreign.execute(argValues[i]);
            }
            try {
                return foreign2R.execute(ForeignAccess.sendInvoke(invokeNode, obj, what, argValues));
            } catch (UnsupportedTypeException e) {
                throw error(RError.Message.GENERIC, "Invalid argument types provided");
            } catch (ArityException e) {
                throw error(RError.Message.INVALID_ARG_NUMBER, what);
            } catch (UnknownIdentifierException e) {
                throw error(RError.Message.UNKNOWN_FUNCTION, what);
            } catch (UnsupportedMessageException e) {
                throw error(RError.Message.MUST_BE_STRING_OR_FUNCTION, "what");
            } catch (RuntimeException e) {
                CompilerDirectives.transferToInterpreter();
                throw error(RError.Message.GENERIC, e.getMessage());
            }
        }
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
        public Object getException(VirtualFrame frame, byte silent, RMissing callerFor) {
            return getException(frame, silent, (String) null);
        }

        @Specialization
        public Object getException(VirtualFrame frame, byte silent, String showCallerOf) {
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
            return ret != null ? JavaInterop.asTruffleObject(ret) : RNull.instance;
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
