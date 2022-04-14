/*
 * Copyright (c) 2019, 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.interop;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.interop.AccessForeignElementNodeFactory.ReadElementNodeGen;
import com.oracle.truffle.r.runtime.interop.AccessForeignElementNodeFactory.WriteElementNodeGen;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

@GenerateUncached
public abstract class AccessForeignElementNode extends RBaseNode {
    protected RError invalidIdentifierError(Object pos) throws RError {
        CompilerDirectives.transferToInterpreter();
        throw RError.error(this, RError.Message.GENERIC, "invalid index/identifier during foreign access: " + pos);
    }

    protected RContext getContext() {
        return getRContext();
    }

    @ImportStatic({DSLConfig.class, ToJavaStaticNode.class})
    @GenerateUncached
    public abstract static class ReadElementNode extends AccessForeignElementNode {

        public static ReadElementNode create() {
            return ReadElementNodeGen.create();
        }

        public abstract Object execute(Object object, Object position);

        @Specialization(guards = "interop.hasArrayElements(object)", limit = "getInteropLibraryCacheSize()")
        public Object readArray(TruffleObject object, int position,
                        @CachedLibrary("object") InteropLibrary interop) {
            return readArray(interop, position, object);
        }

        @Specialization(guards = "interop.hasArrayElements(object)", limit = "getInteropLibraryCacheSize()")
        public Object readArray(TruffleObject object, double position,
                        @CachedLibrary("object") InteropLibrary interop) {
            return readArray(interop, (long) position, object);
        }

        private Object readArray(InteropLibrary interop, long position, TruffleObject object) {
            try {
                return interop.readArrayElement(object, position);
            } catch (InvalidArrayIndexException e) {
                throw invalidIdentifierError(position);
            } catch (InteropException ex) {
                CompilerDirectives.transferToInterpreter();
                throw RError.interopError(RError.findParentRBase(this), ex, object);
            }
        }

        @Specialization(guards = {"!interop.hasArrayElements(object)"}, limit = "getInteropLibraryCacheSize()")
        public Object invalid(@SuppressWarnings("unused") TruffleObject object, @SuppressWarnings("unused") int position,
                        @SuppressWarnings("unused") @CachedLibrary("object") InteropLibrary interop) {
            throw invalidIdentifierError(position);
        }

        @Specialization(guards = {"!interop.hasArrayElements(object)"}, limit = "getInteropLibraryCacheSize()")
        public Object invalid(@SuppressWarnings("unused") TruffleObject object, @SuppressWarnings("unused") double position,
                        @SuppressWarnings("unused") @CachedLibrary("object") InteropLibrary interop) {
            throw invalidIdentifierError(position);
        }

        @Specialization(guards = {"hasReadableMember(interop, position, object)"}, limit = "getInteropLibraryCacheSize()")
        public Object readMember(TruffleObject object, String position,
                        @CachedLibrary("object") InteropLibrary interop) {
            try {
                return interop.readMember(object, position);
            } catch (UnknownIdentifierException e) {
                throw invalidIdentifierError(position);
            } catch (InteropException ex) {
                CompilerDirectives.transferToInterpreter();
                throw RError.interopError(RError.findParentRBase(this), ex, object);
            }
        }

        @Specialization(guards = {"!hasReadableMember(interop, position, object)"}, limit = "getInteropLibraryCacheSize()")
        public Object readJavaStaticField(TruffleObject object, String position,
                        @CachedLibrary("object") InteropLibrary interop,
                        @Cached("create()") ToJavaStaticNode toStatic) {
            TruffleObject clazz = null;
            try {
                clazz = toStatic.execute(object);
                if (clazz == null) {
                    throw invalidIdentifierError(position);
                }
                return interop.readMember(clazz, position);
            } catch (UnknownIdentifierException e) {
                throw invalidIdentifierError(position);
            } catch (InteropException e) {
                if (clazz != null && interop.isMemberReadable(clazz, position)) {
                    CompilerDirectives.transferToInterpreter();
                    throw RError.error(this, RError.Message.GENERIC, "error in foreign access: " + position + " " + e.getMessage());
                }
                CompilerDirectives.transferToInterpreter();
                throw RError.interopError(RError.findParentRBase(this), e, object);
            }
        }

        protected static boolean hasReadableMember(InteropLibrary interop, String position, TruffleObject object) {
            return interop.isMemberExisting(object, position) && interop.isMemberReadable(object, position);
        }
    }

    @ImportStatic({DSLConfig.class, ToJavaStaticNode.class})
    public abstract static class WriteElementNode extends AccessForeignElementNode {

        public static WriteElementNode create() {
            return WriteElementNodeGen.create();
        }

        public abstract void execute(TruffleObject object, Object position, Object writtenValue);

        @Specialization(guards = "interop.hasArrayElements(object)", limit = "getInteropLibraryCacheSize()")
        public void writeArray(TruffleObject object, int position, Object value,
                        @Cached("create()") R2Foreign r2Foreign,
                        @CachedLibrary("object") InteropLibrary interop) {
            write(interop, r2Foreign, object, position, value);
        }

        @Specialization(guards = "interop.hasArrayElements(object)", limit = "getInteropLibraryCacheSize()")
        public void writeArray(TruffleObject object, double position, Object value,
                        @Cached("create()") R2Foreign r2Foreign,
                        @CachedLibrary("object") InteropLibrary interop) {
            write(interop, r2Foreign, object, (long) position, value);
        }

        private void write(InteropLibrary interop, R2Foreign r2Foreign, TruffleObject object, long position, Object value) throws RError {
            try {
                interop.writeArrayElement(object, position, r2Foreign.convert(value));
            } catch (InvalidArrayIndexException e) {

                throw invalidIdentifierError(position);
            } catch (InteropException e) {
                CompilerDirectives.transferToInterpreter();
                throw RError.interopError(RError.findParentRBase(this), e, object);
            }
        }

        @Specialization(guards = "!interop.hasArrayElements(object)", limit = "getInteropLibraryCacheSize()")
        public void invalid(@SuppressWarnings("unused") TruffleObject object, @SuppressWarnings("unused") int position, @SuppressWarnings("unused") Object value,
                        @SuppressWarnings("unused") @CachedLibrary("object") InteropLibrary interop) {
            throw invalidIdentifierError(position);
        }

        @Specialization(guards = "!interop.hasArrayElements(object)", limit = "getInteropLibraryCacheSize()")
        public void invalid(@SuppressWarnings("unused") TruffleObject object, @SuppressWarnings("unused") double position, @SuppressWarnings("unused") Object value,
                        @SuppressWarnings("unused") @CachedLibrary("object") InteropLibrary interop) {
            throw invalidIdentifierError(position);
        }

        @Specialization(guards = {"hasWritableMember(interop, position, object)"}, limit = "getInteropLibraryCacheSize()")
        public void writeMember(TruffleObject object, String position, Object value,
                        @Cached("create()") R2Foreign r2Foreign,
                        @CachedLibrary("object") InteropLibrary interop) {
            try {
                interop.writeMember(object, position, r2Foreign.convert(value));
            } catch (UnknownIdentifierException e) {
                throw invalidIdentifierError(position);
            } catch (InteropException ex) {
                CompilerDirectives.transferToInterpreter();
                throw RError.interopError(RError.findParentRBase(this), ex, object);
            }
        }

        @Specialization(guards = {"!hasWritableMember(interop, position, object)"}, limit = "getInteropLibraryCacheSize()")
        public void writeJavaStaticField(TruffleObject object, String position, Object value,
                        @Cached("create()") R2Foreign r2Foreign,
                        @Cached("create()") ToJavaStaticNode toStatic,
                        @CachedLibrary("object") InteropLibrary interop) {
            TruffleObject clazz = null;
            try {
                clazz = toStatic.execute(object);
                if (clazz == null) {
                    throw invalidIdentifierError(position);
                }
                interop.writeMember(clazz, position, r2Foreign.convert(value));
            } catch (UnknownIdentifierException e) {
                throw invalidIdentifierError(position);
            } catch (InteropException e) {
                if (clazz != null && interop.isMemberWritable(clazz, position)) {
                    CompilerDirectives.transferToInterpreter();
                    throw RError.error(this, RError.Message.GENERIC, "error in foreign access: " + position + " " + e.getMessage());
                }
                CompilerDirectives.transferToInterpreter();
                throw RError.interopError(RError.findParentRBase(this), e, object);
            }
        }

        protected static boolean hasWritableMember(InteropLibrary interop, String position, TruffleObject object) {
            return interop.isMemberWritable(object, position);
        }
    }

}
