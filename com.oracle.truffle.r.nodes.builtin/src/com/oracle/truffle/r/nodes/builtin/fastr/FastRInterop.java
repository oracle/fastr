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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.logicalValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.notLogicalNA;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.singleElement;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.runtime.RVisibility.OFF;
import static com.oracle.truffle.r.runtime.RVisibility.ON;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import java.io.File;
import java.io.IOException;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.ForeignAccess;
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
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.doubleValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.integerValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.numericValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.rawValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.typeName;
import com.oracle.truffle.r.nodes.builtin.NodeWithArgumentCasts.Casts;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RSource;
import static com.oracle.truffle.r.runtime.RType.Array;
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
import com.oracle.truffle.r.runtime.data.RTypedValue;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FastRInterop {

    @RBuiltin(name = ".fastr.interop.eval", visibility = OFF, kind = PRIMITIVE, parameterNames = {"mimeType", "source"}, behavior = COMPLEX)
    public abstract static class Eval extends RBuiltinNode.Arg2 {

        static {
            Casts casts = new Casts(Eval.class);
            casts.arg("mimeType").mustBe(stringValue()).asStringVector().mustBe(singleElement()).findFirst();
            casts.arg("source").mustBe(stringValue()).asStringVector().mustBe(singleElement()).findFirst();
        }

        protected CallTarget parse(String mimeType, String source) {
            CompilerAsserts.neverPartOfCompilation();

            Source sourceObject = RSource.fromTextInternal(source, RSource.Internal.EVAL_WRAPPER, mimeType);
            try {
                return RContext.getInstance().getEnv().parse(sourceObject);
            } catch (Throwable t) {
                throw error(Message.GENERIC, "Error while parsing: " + t.getMessage());
            }
        }

        protected DirectCallNode createCall(String mimeType, String source) {
            return Truffle.getRuntime().createDirectCallNode(parse(mimeType, source));
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"cachedMimeType != null", "cachedMimeType.equals(mimeType)", "cachedSource != null", "cachedSource.equals(source)"})
        protected Object evalCached(String mimeType, String source,
                        @Cached("mimeType") String cachedMimeType,
                        @Cached("source") String cachedSource,
                        @Cached("createCall(mimeType, source)") DirectCallNode call) {
            return call.call(EMPTY_OBJECT_ARRAY);
        }

        @Specialization(replaces = "evalCached")
        @TruffleBoundary
        protected Object eval(String mimeType, String source) {
            return parse(mimeType, source).call();
        }
    }

    @RBuiltin(name = ".fastr.interop.evalFile", visibility = OFF, kind = PRIMITIVE, parameterNames = {"path", "mimeType"}, behavior = COMPLEX)
    public abstract static class EvalFile extends RBuiltinNode.Arg2 {

        static {
            Casts casts = new Casts(EvalFile.class);
            casts.arg("path").mustBe(stringValue()).asStringVector().mustBe(singleElement()).findFirst();
            casts.arg("mimeType").allowMissing().mustBe(stringValue()).asStringVector().mustBe(singleElement()).findFirst();
        }

        protected CallTarget parse(String path, String mimeType) {
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
                throw error(Message.GENERIC, "Error reading file: " + e.getMessage());
            } catch (Throwable t) {
                throw error(Message.GENERIC, "Error while parsing: " + t.getMessage());
            }
        }

        @Specialization
        @TruffleBoundary
        protected Object eval(String path, @SuppressWarnings("unused") RMissing missing) {
            return parse(path, null).call();
        }

        @Specialization
        @TruffleBoundary
        protected Object eval(String path, String mimeType) {
            return parse(path, mimeType).call();
        }
    }

    @RBuiltin(name = ".fastr.interop.export", visibility = OFF, kind = PRIMITIVE, parameterNames = {"name", "value"}, behavior = COMPLEX)
    public abstract static class Export extends RBuiltinNode.Arg2 {

        static {
            Casts casts = new Casts(Export.class);
            casts.arg("name").mustBe(stringValue()).asStringVector().mustBe(singleElement()).findFirst();
            casts.arg("value").boxPrimitive();
        }

        @Specialization(guards = "!isRMissing(value)")
        @TruffleBoundary
        protected Object exportSymbol(String name, RTypedValue value) {
            if (name == null) {
                throw error(RError.Message.INVALID_ARGUMENT, "name");
            }
            RContext.getInstance().getExportedSymbols().put(name, value);
            return RNull.instance;
        }

        @Specialization
        @TruffleBoundary
        protected Object exportSymbol(@SuppressWarnings("unused") String name, @SuppressWarnings("unused") RMissing value) {
            throw error(Message.ARGUMENT_MISSING, "value");
        }

        @Fallback
        @TruffleBoundary
        protected Object exportSymbol(@SuppressWarnings("unused") Object name, @SuppressWarnings("unused") Object value) {
            throw error(Message.GENERIC, "only R language objects can be exported");
        }
    }

    @RBuiltin(name = ".fastr.interop.import", visibility = OFF, kind = PRIMITIVE, parameterNames = {"name"}, behavior = COMPLEX)
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

    @RBuiltin(name = ".fastr.interop.hasSize", visibility = ON, kind = PRIMITIVE, parameterNames = {"value"}, behavior = COMPLEX)
    public abstract static class HasSize extends RBuiltinNode.Arg1 {

        @Child private Node node = com.oracle.truffle.api.interop.Message.HAS_SIZE.createNode();

        static {
            Casts.noCasts(HasSize.class);
        }

        @Specialization
        public byte hasSize(TruffleObject obj) {
            return RRuntime.asLogical(ForeignAccess.sendHasSize(node, obj));
        }
    }

    @RBuiltin(name = ".fastr.interop.isNull", visibility = ON, kind = PRIMITIVE, parameterNames = {"value"}, behavior = COMPLEX)
    public abstract static class IsNull extends RBuiltinNode.Arg1 {

        @Child private Node node = com.oracle.truffle.api.interop.Message.IS_NULL.createNode();

        static {
            Casts.noCasts(IsNull.class);
        }

        @Specialization
        public byte isNull(TruffleObject obj) {
            return RRuntime.asLogical(ForeignAccess.sendIsNull(node, obj));
        }
    }

    @RBuiltin(name = ".fastr.interop.isExecutable", visibility = ON, kind = PRIMITIVE, parameterNames = {"value"}, behavior = COMPLEX)
    public abstract static class IsExecutable extends RBuiltinNode.Arg1 {

        @Child private Node node = com.oracle.truffle.api.interop.Message.IS_EXECUTABLE.createNode();

        static {
            Casts.noCasts(IsExecutable.class);
        }

        @Specialization
        public byte isExecutable(TruffleObject obj) {
            return RRuntime.asLogical(ForeignAccess.sendIsExecutable(node, obj));
        }
    }

    @RBuiltin(name = ".fastr.interop.toByte", visibility = ON, kind = PRIMITIVE, parameterNames = {"value"}, behavior = COMPLEX)
    public abstract static class ToByte extends RBuiltinNode.Arg1 {

        static {
            castToJavaNumberType(new Casts(ToByte.class));
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

    @RBuiltin(name = ".fastr.interop.toChar", visibility = ON, kind = PRIMITIVE, parameterNames = {"value", "pos"}, behavior = COMPLEX)
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

        @Specialization
        public RInteropChar toChar(int value, int pos) {
            throw RError.error(this, RError.Message.POS_NOT_ALLOWED_WITH_NUMERIC);
        }

        @Specialization
        public RInteropChar toChar(double value, int pos) {
            throw RError.error(this, RError.Message.POS_NOT_ALLOWED_WITH_NUMERIC);
        }

        @Specialization
        public RInteropChar toChar(String value, int pos) {
            return RInteropChar.valueOf(value.charAt(pos));
        }
    }

    @RBuiltin(name = ".fastr.interop.toFloat", visibility = ON, kind = PRIMITIVE, parameterNames = {"value"}, behavior = COMPLEX)
    public abstract static class ToFloat extends RBuiltinNode.Arg1 {

        static {
            castToJavaNumberType(new Casts(ToFloat.class));
        }

        @Specialization
        public RInteropFloat toFloat(int value) {
            return RInteropFloat.valueOf((float) value);
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

    @RBuiltin(name = ".fastr.interop.toLong", visibility = ON, kind = PRIMITIVE, parameterNames = {"value"}, behavior = COMPLEX)
    public abstract static class ToLong extends RBuiltinNode.Arg1 {

        static {
            castToJavaNumberType(new Casts(ToLong.class));
        }

        @Specialization
        public RInteropLong toLong(int value) {
            return RInteropLong.valueOf((long) value);
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

    @RBuiltin(name = ".fastr.interop.toShort", visibility = ON, kind = PRIMITIVE, parameterNames = {"value"}, behavior = COMPLEX)
    public abstract static class ToShort extends RBuiltinNode.Arg1 {

        static {
            castToJavaNumberType(new Casts(ToShort.class));
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

    private static void castToJavaNumberType(Casts casts) {
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

    @RBuiltin(name = ".fastr.java.class", visibility = ON, kind = PRIMITIVE, parameterNames = {"class"}, behavior = COMPLEX)
    public abstract static class JavaClass extends RBuiltinNode.Arg1 {

        static {
            Casts casts = new Casts(JavaClass.class);
            casts.arg("class").mustBe(stringValue()).asStringVector().mustBe(Predef.singleElement()).findFirst();
        }

        @Specialization
        @TruffleBoundary
        public TruffleObject javaClass(String clazz) {
            try {
                return JavaInterop.asTruffleObject(Class.forName(clazz));
            } catch (ClassNotFoundException | SecurityException | IllegalArgumentException e) {
                throw error(Message.GENERIC, "error while accessing Java class: " + e.getMessage());
            }
        }
    }

    @ImportStatic({Message.class, RRuntime.class})
    @RBuiltin(name = ".fastr.java.isArray", visibility = ON, kind = PRIMITIVE, parameterNames = {"obj"}, behavior = COMPLEX)
    public abstract static class IsJavaArray extends RBuiltinNode.Arg1 {

        static {
            Casts.noCasts(IsJavaArray.class);
        }

        private final ConditionProfile isJavaProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile isArrayProfile = ConditionProfile.createBinaryProfile();

        @Specialization(guards = {"isForeignObject(obj)"})
        @TruffleBoundary
        public Object isArray(TruffleObject obj) {
            // TODO does this return true only for java arrays, or also
            // js arrays?
            boolean result = isJavaProfile.profile(JavaInterop.isJavaObject(Object.class, obj)) && isArrayProfile.profile(JavaInterop.isArray(obj));
            return RRuntime.java2R(result);
        }

        @Fallback
        public Object isArray(Object obj) {
            return RRuntime.java2R(false);
        }
    }

    @RBuiltin(name = ".fastr.java.newArray", visibility = ON, kind = PRIMITIVE, parameterNames = {"class", "dim"}, behavior = COMPLEX)
    public abstract static class NewJavaArray extends RBuiltinNode.Arg2 {

        static {
            Casts casts = new Casts(NewJavaArray.class);
            casts.arg("class").mustBe(stringValue()).asStringVector().mustBe(Predef.singleElement()).findFirst();
            casts.arg("dim").mustBe(integerValue(), RError.Message.INVALID_ARGUMENT_OF_TYPE, "dim", typeName()).asIntegerVector();
        }

        @Specialization
        @TruffleBoundary
        public Object newArray(String clazz, int length) {
            try {
                // TODO new via ForeignAccess
                return JavaInterop.asTruffleObject(Array.newInstance(Class.forName(clazz), length));
            } catch (ClassNotFoundException e) {
                throw error(RError.Message.GENERIC, "error while accessing Java class: " + e.getMessage());
            }
        }

        @Specialization
        @TruffleBoundary
        public Object newArray(String clazz, RAbstractIntVector dim) {
            try {
                int[] dima = new int[dim.getLength()];
                // TODO new via ForeignAccess
                return JavaInterop.asTruffleObject(Array.newInstance(Class.forName(clazz), dima));
            } catch (ClassNotFoundException e) {
                throw error(RError.Message.GENERIC, "error while accessing Java class: " + e.getMessage());
            }
        }
    }

    @ImportStatic({Message.class, RRuntime.class})
    @RBuiltin(name = ".fastr.java.toArray", visibility = ON, kind = PRIMITIVE, parameterNames = {"x", "className", "flat"}, behavior = COMPLEX)
    public abstract static class ToJavaArray extends RBuiltinNode.Arg3 {

        static {
            Casts casts = new Casts(ToJavaArray.class);
            casts.arg("x").mustNotBeMissing();
            casts.arg("className").allowMissing().mustBe(stringValue()).asStringVector().mustBe(Predef.singleElement()).findFirst();
            casts.arg("flat").mapMissing(Predef.constant(RRuntime.asLogical(true))).mustBe(logicalValue().or(Predef.nullValue())).asLogicalVector().mustBe(singleElement()).findFirst().mustBe(
                            notLogicalNA()).map(Predef.toBoolean());
        }

        @Specialization
        @TruffleBoundary
        public Object toArray(RAbstractLogicalVector vec, @SuppressWarnings("unused") RMissing className, boolean flat) {
            return toArray(vec, flat, boolean.class, (array, i) -> Array.set(array, i, RRuntime.r2Java(vec.getDataAt(i))));
        }

        @Specialization
        @TruffleBoundary
        public Object toArray(RAbstractLogicalVector vec, String className, boolean flat) {
            return toArray(vec, flat, getClazz(className), (array, i) -> Array.set(array, i, RRuntime.r2Java(vec.getDataAt(i))));
        }

        @Specialization
        @TruffleBoundary
        public Object toArray(RAbstractIntVector vec, @SuppressWarnings("unused") RMissing className, boolean flat) {
            return toArray(vec, flat, int.class, (array, i) -> Array.set(array, i, RRuntime.r2Java(vec.getDataAt(i))));
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
        public Object toArray(RAbstractVector vec, @SuppressWarnings("unused") RMissing className, boolean flat) {
            return toArray(vec, flat, Object.class, (array, i) -> Array.set(array, i, RRuntime.r2Java(vec.getDataAtAsObject(i))));
        }

        @Specialization
        @TruffleBoundary
        public Object toArray(RAbstractVector vec, String className, boolean flat) {
            return toArray(vec, flat, getClazz(className), (array, i) -> Array.set(array, i, RRuntime.r2Java(vec.getDataAtAsObject(i))));
        }

        @Specialization
        @TruffleBoundary
        public Object toArray(RInteropScalar ri, String className, boolean flat) {
            RList list = RDataFactory.createList(new Object[]{ri});
            return toArray(list, flat, getClazz(className), (array, i) -> Array.set(array, i, RRuntime.r2Java(list.getDataAt(i))));
        }

        @Specialization
        @TruffleBoundary
        public Object toArray(RInteropScalar ri, RMissing className, boolean flat) {
            RList list = RDataFactory.createList(new Object[]{ri});
            return toArray(list, flat, ri.getJavaType(), (array, i) -> Array.set(array, i, RRuntime.r2Java(list.getDataAt(i))));
        }

        private Class<?> getClazz(String className) throws RError {
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
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw error(RError.Message.GENERIC, "error while accessing Java class: " + e.getMessage());
            }
        }

        private int[] getDim(boolean flat, RAbstractVector vec) {
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

        private Object toArray(RAbstractVector vec, boolean flat, Class<?> clazz, VecElementToArray vecToArray) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
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

        @Specialization
        @TruffleBoundary
        public Object toArray(TruffleObject obj, @SuppressWarnings("unused") RMissing missing, @SuppressWarnings("unused") boolean flat,
                        @Cached("WRITE.createNode()") Node write) {
            if (JavaInterop.isJavaObject(Object.class, obj)) {
                if (JavaInterop.isArray(obj)) {
                    // TODO should return copy?
                    return obj;
                }
                try {
                    // TODO should create array with the same component type as the JavaObject
                    TruffleObject array = JavaInterop.asTruffleObject(Array.newInstance(Object.class, 1));
                    ForeignAccess.sendWrite(write, array, 0, obj);
                    return array;
                } catch (UnsupportedMessageException | UnknownIdentifierException e) {
                    throw error(RError.Message.GENERIC, "error while creating array: " + e.getMessage());
                } catch (UnsupportedTypeException ex) {
                    Logger.getLogger(FastRInterop.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            throw error(RError.Message.GENERIC, "can't create array from " + obj);
        }

        @Fallback
        public Object toArray(Object o, @SuppressWarnings("unused") Object className, @SuppressWarnings("unused") Object flat) {
            throw error(RError.Message.GENERIC, "unsupported type");
        }

    }

    @RBuiltin(name = ".fastr.java.fromArray", visibility = ON, kind = PRIMITIVE, parameterNames = {"array"}, behavior = COMPLEX)
    public abstract static class FromJavaArray extends RBuiltinNode.Arg1 {
        @Child Node getSize = Message.GET_SIZE.createNode();
        @Child Node read;
        @Child Node isNull;
        @Child Node isBoxed;
        @Child Node unbox;
        static {
            Casts casts = new Casts(FromJavaArray.class);
            casts.arg("array").mustNotBeMissing();
        }

        protected boolean isJavaArray(TruffleObject obj) {
            return JavaInterop.isJavaObject(Object.class, obj) && JavaInterop.isArray(obj);
        }

        @Specialization(guards = {"isJavaArray(obj)"})
        @TruffleBoundary
        public Object fromArray(TruffleObject obj) {
            int size;
            try {
                size = (int) ForeignAccess.sendGetSize(getSize, obj);
                if (size == 0) {
                    return RDataFactory.createList();
                }
                Object[] elements = new Object[size];
                boolean allBoolean = true;
                boolean allInteger = true;
                boolean allDouble = true;
                boolean allString = true;
                for (int i = 0; i < size; i++) {
                    if (read == null) {
                        read = insert(Message.READ.createNode());
                    }
                    Object element = ForeignAccess.sendRead(read, obj, i);
                    if (element instanceof TruffleObject) {
                        if (isNull == null) {
                            isNull = insert(Message.IS_NULL.createNode());
                        }
                        if (ForeignAccess.sendIsNull(isNull, (TruffleObject) element)) {
                            element = null;
                        } else {
                            if (isBoxed == null) {
                                isBoxed = insert(Message.IS_BOXED.createNode());
                            }
                            if (ForeignAccess.sendIsBoxed(isBoxed, (TruffleObject) element)) {
                                if (unbox == null) {
                                    unbox = insert(Message.UNBOX.createNode());
                                }
                                element = ForeignAccess.sendIsBoxed(unbox, (TruffleObject) element);
                            }
                        }
                    }
                    allBoolean &= element instanceof Boolean;
                    allInteger &= element instanceof Byte || element instanceof Integer || element instanceof Short;
                    allDouble &= element instanceof Double || element instanceof Float || element instanceof Long;
                    allString &= element instanceof Character || element instanceof String;

                    elements[i] = RRuntime.java2R(element);
                }
                if (allBoolean) {
                    byte[] ret = new byte[size];
                    for (int i = 0; i < size; i++) {
                        ret[i] = ((Number) elements[i]).byteValue();
                    }
                    return RDataFactory.createLogicalVector(ret, true);
                }
                if (allInteger) {
                    int[] ret = new int[size];
                    for (int i = 0; i < size; i++) {
                        ret[i] = ((Number) elements[i]).intValue();
                    }
                    return RDataFactory.createIntVector(ret, true);
                }
                if (allDouble) {
                    double[] ret = new double[size];
                    for (int i = 0; i < size; i++) {
                        ret[i] = ((Number) elements[i]).doubleValue();
                    }
                    return RDataFactory.createDoubleVector(ret, true);
                }
                if (allString) {
                    String[] ret = new String[size];
                    for (int i = 0; i < size; i++) {
                        ret[i] = String.valueOf(elements[i]);
                    }
                    return RDataFactory.createStringVector(ret, true);
                }
                return RDataFactory.createList(elements);
            } catch (UnsupportedMessageException | UnknownIdentifierException e) {
                throw error(RError.Message.GENERIC, "error while converting array: " + e.getMessage());
            }
        }

        @Fallback
        public Object fromArray(@SuppressWarnings("unused") Object obj) {
            throw error(RError.Message.GENERIC, "not a java array");
        }
    }

    @ImportStatic({Message.class, RRuntime.class})
    @RBuiltin(name = ".fastr.interop.new", visibility = ON, kind = PRIMITIVE, parameterNames = {"class", "..."}, behavior = COMPLEX)
    public abstract static class InteropNew extends RBuiltinNode.Arg2 {

        static {
            Casts.noCasts(InteropNew.class);
        }

        @Specialization(limit = "99", guards = {"isForeignObject(clazz)", "length == args.getLength()"})
        @TruffleBoundary
        public Object interopNew(TruffleObject clazz, RArgsValuesAndNames args,
                        @SuppressWarnings("unused") @Cached("args.getLength()") int length,
                        @Cached("createNew(length).createNode()") Node sendNew) {
            try {
                Object[] argValues = new Object[args.getLength()];
                for (int i = 0; i < argValues.length; i++) {
                    argValues[i] = RRuntime.r2Java(args.getArgument(i));
                }
                Object result = ForeignAccess.sendNew(sendNew, clazz, argValues);
                return RRuntime.java2R(result);
            } catch (SecurityException | IllegalArgumentException | UnsupportedTypeException | ArityException | UnsupportedMessageException e) {
                throw error(Message.GENERIC, "error during Java object instantiation: " + e.getMessage());
            }
        }

        @Fallback
        public Object interopNew(@SuppressWarnings("unused") Object clazz, @SuppressWarnings("unused") Object args) {
            throw error(Message.GENERIC, "interop object needed as receiver of NEW message");
        }
    }
}
