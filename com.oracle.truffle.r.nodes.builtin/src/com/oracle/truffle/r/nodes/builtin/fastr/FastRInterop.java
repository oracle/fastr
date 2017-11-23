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
package com.oracle.truffle.r.nodes.builtin.fastr;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.doubleValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.integerValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.logicalValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.notLogicalNA;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.numericValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.rawValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.singleElement;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.typeName;
import static com.oracle.truffle.r.runtime.RVisibility.OFF;
import static com.oracle.truffle.r.runtime.RVisibility.ON;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.MalformedURLException;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.interop.java.JavaInterop;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.Source.Builder;
import com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef;
import com.oracle.truffle.r.nodes.builtin.NodeWithArgumentCasts.Casts;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RSource;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RDataFactory;
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
import com.oracle.truffle.r.runtime.interop.Foreign2R;
import com.oracle.truffle.r.runtime.interop.ForeignArray2R;
import com.oracle.truffle.r.runtime.interop.R2Foreign;
import com.oracle.truffle.r.runtime.interop.R2ForeignNodeGen;

public class FastRInterop {

    private static boolean isTesting = false;

    public static void testingMode() {
        isTesting = true;
    }

    @RBuiltin(name = "eval.external", visibility = OFF, kind = PRIMITIVE, parameterNames = {"mimeType", "source", "path"}, behavior = COMPLEX)
    public abstract static class Eval extends RBuiltinNode.Arg3 {

        static {
            Casts casts = new Casts(Eval.class);
            casts.arg("mimeType").allowMissing().mustBe(stringValue()).asStringVector().mustBe(singleElement()).findFirst();
            casts.arg("source").allowMissing().mustBe(stringValue()).asStringVector().mustBe(singleElement()).findFirst();
            casts.arg("path").allowMissing().mustBe(stringValue()).asStringVector().mustBe(singleElement()).findFirst();
        }

        protected DirectCallNode createCall(String mimeType, String source) {
            return Truffle.getRuntime().createDirectCallNode(parse(mimeType, source));
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"cachedMimeType != null", "cachedMimeType.equals(mimeType)", "cachedSource != null", "cachedSource.equals(source)"})
        protected Object evalCached(String mimeType, String source, RMissing path,
                        @Cached("mimeType") String cachedMimeType,
                        @Cached("source") String cachedSource,
                        @Cached("createCall(mimeType, source)") DirectCallNode call) {
            return call.call(EMPTY_OBJECT_ARRAY);
        }

        @Specialization(replaces = "evalCached")
        @TruffleBoundary
        protected Object eval(String mimeType, String source, @SuppressWarnings("unused") RMissing path) {
            return parse(mimeType, source).call();
        }

        @Specialization()
        @TruffleBoundary
        protected Object eval(@SuppressWarnings("unused") RMissing mimeType, @SuppressWarnings("unused") String source, @SuppressWarnings("unused") RMissing path) {
            throw RError.error(this, RError.Message.INVALID_ARG, "mimeType");
        }

        protected CallTarget parse(String mimeType, String source) {
            CompilerAsserts.neverPartOfCompilation();

            Source sourceObject = RSource.fromTextInternal(source, RSource.Internal.EVAL_WRAPPER, mimeType);
            try {
                return RContext.getInstance().getEnv().parse(sourceObject);
            } catch (Throwable t) {
                throw error(RError.Message.GENERIC, "Error while parsing: " + t.getMessage());
            }
        }

        @Specialization
        @TruffleBoundary
        protected Object eval(String mimeType, @SuppressWarnings("unused") String source, String path) {
            return parseFile(path, mimeType).call();
        }

        @Specialization
        @TruffleBoundary
        protected Object eval(String mimeType, @SuppressWarnings("unused") RMissing source, String path) {
            return parseFile(path, mimeType).call();
        }

        @Specialization
        @TruffleBoundary
        protected Object eval(@SuppressWarnings("unused") RMissing mimeType, @SuppressWarnings("unused") RMissing source, String path) {
            return parseFile(path, null).call();
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

        static {
            Casts casts = new Casts(JavaClass.class);
            casts.arg("class").mustBe(stringValue()).asStringVector().mustBe(Predef.singleElement()).findFirst();
            casts.arg("silent").mapMissing(Predef.constant(RRuntime.LOGICAL_FALSE)).mustBe(logicalValue().or(Predef.nullValue())).asLogicalVector().mustBe(singleElement()).findFirst().mustBe(
                            notLogicalNA()).map(Predef.toBoolean());
        }

        @Specialization
        @TruffleBoundary
        public TruffleObject javaClass(String clazz, boolean silent) {
            try {
                return JavaInterop.asTruffleObject(RContext.getInstance().loadClass(clazz.replaceAll("/", ".")));
            } catch (ClassNotFoundException | SecurityException | IllegalArgumentException e) {
                if (silent) {
                    return RNull.instance;
                }
                throw error(RError.Message.GENERIC, "error while accessing Java class: " + e.getMessage());
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

    @ImportStatic({RRuntime.class})
    @RBuiltin(name = "java.class", visibility = ON, kind = PRIMITIVE, parameterNames = {"class"}, behavior = COMPLEX)
    public abstract static class JavaClassName extends RBuiltinNode.Arg1 {

        static {
            Casts.noCasts(JavaClassName.class);
        }

        @Specialization(guards = {"isJavaObject(obj)"})
        @TruffleBoundary
        public Object javaClassName(TruffleObject obj) {
            Object o = JavaInterop.asJavaObject(Object.class, obj);
            if (o == null) {
                return RNull.instance;
            }
            return o.getClass().getName();
        }

        protected boolean isJavaObject(TruffleObject obj) {
            return JavaInterop.isJavaObject(obj);
        }

        @Fallback
        public String javaClassName(@SuppressWarnings("unused") Object obj) {
            throw error(RError.Message.GENERIC, "unsupported type");
        }
    }

    @ImportStatic({Message.class, RRuntime.class})
    @RBuiltin(name = "is.external.array", visibility = ON, kind = PRIMITIVE, parameterNames = {"obj"}, behavior = COMPLEX)
    public abstract static class IsForeignArray extends RBuiltinNode.Arg1 {

        static {
            Casts.noCasts(IsForeignArray.class);
        }

        @Specialization(guards = {"isForeignObject(obj)"})
        @TruffleBoundary
        public byte isArray(TruffleObject obj,
                        @Cached("HAS_SIZE.createNode()") Node hasSize) {
            return RRuntime.asLogical(ForeignAccess.sendHasSize(hasSize, obj));
        }

        @Fallback
        public byte isArray(@SuppressWarnings("unused") Object obj) {
            return RRuntime.LOGICAL_FALSE;
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
            try {
                return classForName(className);
            } catch (ClassNotFoundException e) {
                throw error(RError.Message.GENERIC, "error while accessing Java class: " + e.getMessage());
            }
        }
    }

    @ImportStatic({Message.class, RRuntime.class})
    @RBuiltin(name = "as.java.array", visibility = ON, kind = PRIMITIVE, parameterNames = {"x", "className", "flat"}, behavior = COMPLEX)
    public abstract static class ToJavaArray extends RBuiltinNode.Arg3 {

        static {
            Casts casts = new Casts(ToJavaArray.class);
            casts.arg("x").mustNotBeMissing();
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
            return toArray(vec, flat, getClazz(className), (array, i) -> Array.set(array, i, vec.getDataAt(i)));
        }

        @Specialization
        @TruffleBoundary
        public Object toArray(RAbstractDoubleVector vec, @SuppressWarnings("unused") RMissing className, boolean flat) {
            return toArray(vec, flat, double.class, (array, i) -> Array.set(array, i, vec.getDataAt(i)));
        }

        @Specialization
        @TruffleBoundary
        public Object toArray(RAbstractDoubleVector vec, String className, boolean flat) {
            return toArray(vec, flat, getClazz(className), (array, i) -> Array.set(array, i, vec.getDataAt(i)));
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
            return toArray(vec, flat, Object.class, (array, i) -> Array.set(array, i, r2Foreign.execute(vec.getDataAtAsObject(i))));
        }

        @Specialization(guards = "!isJavaLikeVector(vec)")
        @TruffleBoundary
        public Object toArray(RAbstractVector vec, String className, boolean flat,
                        @Cached("createR2Foreign()") R2Foreign r2Foreign) {
            return toArray(vec, flat, getClazz(className), (array, i) -> Array.set(array, i, r2Foreign.execute(vec.getDataAtAsObject(i))));
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
            try {
                return classForName(className);
            } catch (ClassNotFoundException e) {
                throw error(RError.Message.GENERIC, "error while accessing Java class: " + e.getMessage());
            }
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
    @RBuiltin(name = ".fastr.interop.fromArray", visibility = ON, kind = PRIMITIVE, parameterNames = {"array"}, behavior = COMPLEX)
    public abstract static class FromForeignArray extends RBuiltinNode.Arg1 {

        static {
            Casts casts = new Casts(FromForeignArray.class);
            casts.arg("array").castForeignObjects(false).mustNotBeMissing();
        }

        private final ConditionProfile isArrayProfile = ConditionProfile.createBinaryProfile();

        @Specialization(guards = {"isForeignObject(obj)"})
        @TruffleBoundary
        public Object fromArray(TruffleObject obj,
                        @Cached("HAS_SIZE.createNode()") Node hasSize,
                        @Cached("create()") ForeignArray2R array2R) {
            if (isArrayProfile.profile(ForeignAccess.sendHasSize(hasSize, obj))) {
                return array2R.convert(obj);
            } else {
                throw error(RError.Message.GENERIC, "not a java array");
            }
        }

        @Fallback
        public Object fromObject(@SuppressWarnings("unused") Object obj) {
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

    private static Class<?> classForName(String className) throws ClassNotFoundException {
        if (className.equals(Byte.TYPE.getName())) {
            return Byte.TYPE;
        }
        if (className.equals(Boolean.TYPE.getName())) {
            return Boolean.TYPE;
        }
        if (className.equals(Character.TYPE.getName())) {
            return Character.TYPE;
        }
        if (className.equals(Double.TYPE.getName())) {
            return Double.TYPE;
        }
        if (className.equals(Float.TYPE.getName())) {
            return Float.TYPE;
        }
        if (className.equals(Integer.TYPE.getName())) {
            return Integer.TYPE;
        }
        if (className.equals(Long.TYPE.getName())) {
            return Long.TYPE;
        }
        if (className.equals(Short.TYPE.getName())) {
            return Short.TYPE;
        }
        return Class.forName(className);
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

}
