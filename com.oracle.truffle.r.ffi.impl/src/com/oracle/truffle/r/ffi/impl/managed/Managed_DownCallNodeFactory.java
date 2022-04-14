/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.ffi.impl.managed;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.EnumSet;
import java.util.Set;

import com.oracle.truffle.api.TruffleFile;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.r.ffi.impl.managed.Managed_DownCallNodeFactoryFactory.Managed_DownCallNodeGen;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.ffi.DownCallNodeFactory;
import com.oracle.truffle.r.runtime.ffi.NativeFunction;
import com.oracle.truffle.r.runtime.ffi.interop.NativeCharArray;

public final class Managed_DownCallNodeFactory extends DownCallNodeFactory {

    static final Managed_DownCallNodeFactory INSTANCE = new Managed_DownCallNodeFactory();

    private Managed_DownCallNodeFactory() {
    }

    @Override
    public DownCallNode createDownCallNode() {
        return Managed_DownCallNodeGen.create();
    }

    @GenerateUncached
    protected abstract static class Managed_DownCallNode extends DownCallNode {

        public Managed_DownCallNode() {
            super();
        }

        @Specialization
        protected Object doCall(Frame frame, NativeFunction f, Object[] args) {
            return doCallImpl(frame, f, args);
        }

        @Override
        protected TruffleObject createTarget(RContext ctx, NativeFunction function) {
            if (function == NativeFunction.getpid) {
                return new GetPID();
            } else if (function == NativeFunction.mkdtemp) {
                return new Mkdtemp();
            } else if (function == NativeFunction.getcwd) {
                return new Getwd();
            } else if (function == NativeFunction.initEventLoop) {
                return new InitEventLoop();
            }
            return new DummyFunctionObject(function);
        }

        @Override
        protected Object beforeCall(Frame frame, NativeFunction nativeFunction, TruffleObject function, Object[] args) {
            // Report unsupported functions at invocation time
            if (function instanceof DummyFunctionObject) {
                throw Managed_RFFIFactory.unsupported(((DummyFunctionObject) function).function.getCallName());
            }
            return 0;
        }

        @Override
        protected void afterCall(Frame frame, Object before, NativeFunction function, TruffleObject target, Object[] args) {
            // nop
        }
    }

    private static final class DummyFunctionObject implements TruffleObject {
        final NativeFunction function;

        private DummyFunctionObject(NativeFunction function) {
            this.function = function;
        }
    }

    @ExportLibrary(InteropLibrary.class)
    protected static final class InitEventLoop implements TruffleObject {

        @SuppressWarnings("static-method")
        @ExportMessage
        public boolean isExecutable() {
            return true;
        }

        @SuppressWarnings("static-method")
        @ExportMessage
        public Object execute(@SuppressWarnings("unused") Object... args) {
            // by returning -1 we indicate that the native handlers loop is not available
            return -1;
        }
    }

    // PID is used as a seed for random numbers generation. We still want to support random numbers
    // in managed mode so we make up some (random) value
    @ExportLibrary(InteropLibrary.class)
    protected static final class GetPID implements TruffleObject {
        private static final int fakePid = (int) System.currentTimeMillis();

        @SuppressWarnings("static-method")
        @ExportMessage
        public boolean isExecutable() {
            return true;
        }

        @SuppressWarnings("static-method")
        @ExportMessage
        public Object execute(@SuppressWarnings("unused") Object... args) {
            return fakePid;
        }
    }

    /**
     * Implements simplified version of the {@code mkdtemp} from {@code stdlib}. The reason why we
     * do not use only Java version is that the real {@code mkdtemp} seems to be more reliable and
     * secure.
     */
    @ExportLibrary(InteropLibrary.class)
    protected static final class Mkdtemp implements TruffleObject {
        private static final FileAttribute<Set<PosixFilePermission>> irwxuPermissions = PosixFilePermissions.asFileAttribute(
                        EnumSet.of(PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));

        @SuppressWarnings("static-method")
        @ExportMessage
        public boolean isExecutable() {
            return true;
        }

        @SuppressWarnings("static-method")
        @ExportMessage
        public Object execute(Object[] args,
                        @CachedLibrary("this") InteropLibrary interop) {
            RContext ctx = RContext.getInstance(interop);
            NativeCharArray templateBytes = (NativeCharArray) args[0];
            String template = templateBytes.getString();
            if (!template.endsWith("XXXXXX")) {
                throw new IllegalArgumentException("template must end with XXXXXX");
            }
            String templatePrefix = template.substring(0, template.length() - 6);
            TruffleFile dir = null;
            int counter = 0;
            boolean done = false;
            while (!done) {
                try {
                    dir = ctx.getSafeTruffleFile(templatePrefix + String.format("%06d", counter++));
                    if (dir.exists()) {
                        continue;
                    }
                    dir.createDirectories(irwxuPermissions);
                    done = true;
                } catch (FileAlreadyExistsException e) {
                    // nop
                } catch (IOException e) {
                    throw RError.error(RError.NO_CALLER, Message.GENERIC, "Cannot create temp directories.");
                }
            }
            byte[] resultBytes = dir.toString().getBytes();
            System.arraycopy(resultBytes, 0, templateBytes.getValue(), 0, Math.min(resultBytes.length, templateBytes.getValue().length));
            return 1;
        }
    }

    /**
     * Gives the current working directory. For some reasons, this is not exactly equivalent to
     * calling the C function, which manifests itself during codetools package installation.
     */
    @ExportLibrary(InteropLibrary.class)
    protected static final class Getwd implements TruffleObject {

        @SuppressWarnings("static-method")
        @ExportMessage
        public boolean isExecutable() {
            return true;
        }

        @SuppressWarnings("static-method")
        @ExportMessage
        public Object execute(Object[] args,
                        @CachedLibrary("this") InteropLibrary interop) {
            RContext ctx = RContext.getInstance(interop);
            NativeCharArray buffer = (NativeCharArray) args[0];
            byte[] bytes = ctx.getSafeTruffleFile(".").getAbsoluteFile().normalize().toString().getBytes();
            System.arraycopy(bytes, 0, buffer.getValue(), 0, Math.min(bytes.length, buffer.getValue().length));
            return 1;
        }
    }
}
