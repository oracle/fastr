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
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.interop.java.JavaInterop;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.Source.Builder;
import com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RSource;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RTypedValue;
import com.oracle.truffle.r.runtime.data.model.RAbstractAtomicVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractRawVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

public class FastRInterop {

    @RBuiltin(name = ".fastr.interop.eval", visibility = OFF, kind = PRIMITIVE, parameterNames = {"mimeType", "source"}, behavior = COMPLEX)
    public abstract static class Eval extends RBuiltinNode {

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
    public abstract static class EvalFile extends RBuiltinNode {

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
    public abstract static class Export extends RBuiltinNode {

        static {
            Casts casts = new Casts(Export.class);
            casts.arg("name").mustBe(stringValue()).asStringVector().mustBe(singleElement()).findFirst();
            casts.arg("value").boxPrimitive();
        }

        @Specialization(guards = "!isRMissing(value)")
        @TruffleBoundary
        protected Object exportSymbol(String name, RTypedValue value) {
            if (name == null) {
                throw error(RError.Message.INVALID_ARG_TYPE, "name");
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
    public abstract static class Import extends RBuiltinNode {

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
    public abstract static class HasSize extends RBuiltinNode {

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
    public abstract static class IsNull extends RBuiltinNode {

        @Child private Node node = com.oracle.truffle.api.interop.Message.IS_NULL.createNode();

        static {
            Casts.noCasts(IsNull.class);
        }

        @Specialization
        public byte hasSize(TruffleObject obj) {
            return RRuntime.asLogical(ForeignAccess.sendIsNull(node, obj));
        }
    }

    @RBuiltin(name = ".fastr.interop.isExecutable", visibility = ON, kind = PRIMITIVE, parameterNames = {"value"}, behavior = COMPLEX)
    public abstract static class IsExecutable extends RBuiltinNode {

        @Child private Node node = com.oracle.truffle.api.interop.Message.IS_EXECUTABLE.createNode();

        static {
            Casts.noCasts(IsExecutable.class);
        }

        @Specialization
        public byte hasSize(TruffleObject obj) {
            return RRuntime.asLogical(ForeignAccess.sendIsExecutable(node, obj));
        }
    }

    @RBuiltin(name = ".fastr.interop.toBoolean", visibility = ON, kind = PRIMITIVE, parameterNames = {"value"}, behavior = COMPLEX)
    public abstract static class ToBoolean extends RBuiltinNode {

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
    public abstract static class JavaClass extends RBuiltinNode {

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

    @ImportStatic({com.oracle.truffle.api.interop.Message.class, RRuntime.class})
    @RBuiltin(name = ".fastr.interop.new", visibility = ON, kind = PRIMITIVE, parameterNames = {"class", "..."}, behavior = COMPLEX)
    public abstract static class InteropNew extends RBuiltinNode {

        static {
            Casts.noCasts(InteropNew.class);
        }

        private static Object toJava(Object value) {
            Object vector = RRuntime.asAbstractVector(value);
            if (vector instanceof RAbstractAtomicVector && ((RAbstractAtomicVector) vector).getLength() == 1) {
                if (vector instanceof RAbstractDoubleVector) {
                    RAbstractDoubleVector v = (RAbstractDoubleVector) vector;
                    return v.getDataAt(0);
                } else if (vector instanceof RAbstractLogicalVector) {
                    RAbstractLogicalVector v = (RAbstractLogicalVector) vector;
                    return v.getDataAt(0) == RRuntime.LOGICAL_TRUE;
                } else if (vector instanceof RAbstractRawVector) {
                    RAbstractRawVector v = (RAbstractRawVector) vector;
                    return v.getDataAt(0).getValue();
                } else if (vector instanceof RAbstractStringVector) {
                    RAbstractStringVector v = (RAbstractStringVector) vector;
                    return v.getDataAt(0);
                }
            }
            return value;
        }

        @Specialization(limit = "99", guards = {"isForeignObject(clazz)", "length == args.getLength()"})
        @TruffleBoundary
        public Object interopNew(TruffleObject clazz, RArgsValuesAndNames args,
                        @SuppressWarnings("unused") @Cached("args.getLength()") int length,
                        @Cached("createNew(length).createNode()") Node sendNew) {
            try {
                Object[] argValues = new Object[args.getLength()];
                for (int i = 0; i < argValues.length; i++) {
                    argValues[i] = toJava(args.getArgument(i));
                }
                return ForeignAccess.sendNew(sendNew, clazz, argValues);
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
