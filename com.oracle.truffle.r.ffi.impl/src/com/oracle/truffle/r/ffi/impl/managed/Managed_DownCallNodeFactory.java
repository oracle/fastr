/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.ffi.impl.managed;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.EnumSet;
import java.util.Set;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.ForeignAccess.StandardFactory;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.ffi.DownCallNodeFactory;
import com.oracle.truffle.r.runtime.ffi.NativeFunction;
import com.oracle.truffle.r.runtime.ffi.interop.NativeCharArray;

public final class Managed_DownCallNodeFactory extends DownCallNodeFactory {

    static final Managed_DownCallNodeFactory INSTANCE = new Managed_DownCallNodeFactory();

    private Managed_DownCallNodeFactory() {
    }

    @Override
    public DownCallNode createDownCallNode(NativeFunction f) {
        return new DownCallNode(f) {
            @Override
            protected TruffleObject getTarget(NativeFunction function) {
                if (function == NativeFunction.getpid) {
                    return new GetPID();
                } else if (function == NativeFunction.mkdtemp) {
                    return new Mkdtemp();
                } else if (function == NativeFunction.getcwd) {
                    return new Getwd();
                }
                return new DummyFunctionObject(function);
            }

            @Override
            protected long beforeCall(NativeFunction nativeFunction, TruffleObject function, Object[] args) {
                // Report unsupported functions at invocation time
                if (function instanceof DummyFunctionObject) {
                    throw Managed_RFFIFactory.unsupported(((DummyFunctionObject) function).function.getCallName());
                }
                return 0;
            }

            @Override
            protected void afterCall(long before, NativeFunction function, TruffleObject target, Object[] args) {
                // nop
            }
        };
    }

    private static final class DummyFunctionObject implements TruffleObject {
        final NativeFunction function;

        private DummyFunctionObject(NativeFunction function) {
            this.function = function;
        }

        @Override
        public ForeignAccess getForeignAccess() {
            return null;
        }
    }

    // PID is used as a seed for random numbers generation. We still want to support random numbers
    // in managed mode so we make up some (random) value
    private static final class GetPID implements TruffleObject {
        private static final int fakePid = (int) System.currentTimeMillis();

        @Override
        public ForeignAccess getForeignAccess() {
            return ForeignAccess.create(GetPID.class, new StandardFactory() {
                @Override
                public CallTarget accessIsExecutable() {
                    return Truffle.getRuntime().createCallTarget(RootNode.createConstantNode(true));
                }

                @Override
                public CallTarget accessExecute(int argumentsLength) {
                    return Truffle.getRuntime().createCallTarget(RootNode.createConstantNode(fakePid));
                }
            });
        }
    }

    /**
     * Implements simplified version of the {@code mkdtemp} from {@code stdlib}. The reason why we
     * do not use only Java version is that the real {@code mkdtemp} seems to be more reliable and
     * secure.
     */
    private static final class Mkdtemp implements TruffleObject {
        private static final FileAttribute<Set<PosixFilePermission>> irwxuPermissions = PosixFilePermissions.asFileAttribute(
                        EnumSet.of(PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));

        @Override
        public ForeignAccess getForeignAccess() {
            return ForeignAccess.create(GetPID.class, new StandardFactory() {
                @Override
                public CallTarget accessIsExecutable() {
                    return Truffle.getRuntime().createCallTarget(RootNode.createConstantNode(true));
                }

                @Override
                public CallTarget accessExecute(int argumentsLength) {
                    return Truffle.getRuntime().createCallTarget(new RootNode(null) {
                        @Override
                        @TruffleBoundary
                        public Object execute(VirtualFrame frame) {
                            NativeCharArray templateBytes = (NativeCharArray) ForeignAccess.getArguments(frame).get(0);
                            String template = templateBytes.getString();
                            if (!template.endsWith("XXXXXX")) {
                                throw new IllegalArgumentException("template must end with XXXXXX");
                            }
                            String templatePrefix = template.substring(0, template.length() - 6);
                            Path path = null;
                            int counter = 0;
                            boolean done = false;
                            while (!done) {
                                try {
                                    path = Paths.get(templatePrefix + String.format("%06d", counter++));
                                    if (Files.exists(path)) {
                                        continue;
                                    }
                                    Files.createDirectories(path, irwxuPermissions);
                                    done = true;
                                } catch (FileAlreadyExistsException e) {
                                    // nop
                                } catch (IOException e) {
                                    throw RError.error(RError.NO_CALLER, Message.GENERIC, "Cannot create temp directories.");
                                }
                            }
                            byte[] resultBytes = path.toString().getBytes();
                            System.arraycopy(resultBytes, 0, templateBytes.getValue(), 0, Math.min(resultBytes.length, templateBytes.getValue().length));
                            return 1;
                        }
                    });
                }
            });
        }
    }

    /**
     * Gives the current working directory. For some reasons, this is not exactly equivalent to
     * calling the C function, which manifests itself during codetools package installation.
     */
    private static final class Getwd implements TruffleObject {
        @Override
        public ForeignAccess getForeignAccess() {
            return ForeignAccess.create(GetPID.class, new StandardFactory() {
                @Override
                public CallTarget accessIsExecutable() {
                    return Truffle.getRuntime().createCallTarget(RootNode.createConstantNode(true));
                }

                @Override
                public CallTarget accessExecute(int argumentsLength) {
                    return Truffle.getRuntime().createCallTarget(new RootNode(null) {
                        @Override
                        @TruffleBoundary
                        public Object execute(VirtualFrame frame) {
                            NativeCharArray buffer = (NativeCharArray) ForeignAccess.getArguments(frame).get(0);
                            byte[] bytes = Paths.get(".").toAbsolutePath().normalize().toString().getBytes();
                            System.arraycopy(bytes, 0, buffer.getValue(), 0, Math.min(bytes.length, buffer.getValue().length));
                            return 1;
                        }
                    });
                }
            });
        }
    }
}
