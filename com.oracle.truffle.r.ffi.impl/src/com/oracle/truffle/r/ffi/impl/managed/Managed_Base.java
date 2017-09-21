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
package com.oracle.truffle.r.ffi.impl.managed;

import static com.oracle.truffle.r.ffi.impl.managed.FilesystemUtils.permissionsFromMode;
import static com.oracle.truffle.r.ffi.impl.managed.Managed_RFFIFactory.unsupported;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.ffi.BaseRFFI;

public class Managed_Base implements BaseRFFI {

    private static final class ManagedGetpidNode extends Node implements GetpidNode {
        private int fakePid = (int) System.currentTimeMillis();

        @Override
        public int execute() {
            return fakePid;
        }
    }

    /**
     * Process id is used as seed for random number generator. We return another "random" number.
     */
    @Override
    public GetpidNode createGetpidNode() {
        return new ManagedGetpidNode();
    }

    private static final class ManagedGetwdNode extends Node implements GetwdNode {
        @Override
        @TruffleBoundary
        public String execute() {
            return Paths.get(".").toAbsolutePath().normalize().toString();
        }
    }

    @Override
    public GetwdNode createGetwdNode() {
        return new ManagedGetwdNode();
    }

    private static final class ManagedSetwdNode extends Node implements SetwdNode {
        @Override
        public int execute(String dir) {
            throw unsupported("setwd");
        }
    }

    @Override
    public SetwdNode createSetwdNode() {
        return new ManagedSetwdNode();
    }

    private static final class ManagedMkdirNode extends Node implements MkdirNode {
        @Override
        @TruffleBoundary
        public void execute(String dir, int mode) throws IOException {
            Set<PosixFilePermission> permissions = permissionsFromMode(mode);
            Files.createDirectories(Paths.get(dir), PosixFilePermissions.asFileAttribute(permissions));
        }
    }

    @Override
    public MkdirNode createMkdirNode() {
        return new ManagedMkdirNode();
    }

    private static final class ManagedReadLinkNode extends Node implements ReadlinkNode {
        @Override
        public String execute(String path) throws IOException {
            throw unsupported("linknode");
        }
    }

    @Override
    public ReadlinkNode createReadlinkNode() {
        return new ManagedReadLinkNode();
    }

    private static final class ManagedMkdtempNode extends Node implements MkdtempNode {
        @Override
        @TruffleBoundary
        public String execute(String template) {
            Path path = null;
            boolean done = false;
            while (!done) {
                try {
                    path = Paths.get(template);
                    Files.createDirectories(path);
                    done = true;
                } catch (FileAlreadyExistsException e) {
                    // nop
                } catch (IOException e) {
                    throw RError.error(RError.NO_CALLER, Message.GENERIC, "Cannot create temp directories.");
                }
            }
            return path.toString();
        }
    }

    @Override
    public MkdtempNode createMkdtempNode() {
        return new ManagedMkdtempNode();
    }

    private static final class ManagedChmodNode extends Node implements ChmodNode {
        @Override
        @TruffleBoundary
        public int execute(String path, int mode) {
            try {
                Files.setPosixFilePermissions(Paths.get(path), permissionsFromMode(mode));
                return mode;
            } catch (IOException e) {
                throw RError.error(RError.NO_CALLER, Message.GENERIC, "Cannot change file permissions.");
            }
        }
    }

    @Override
    public ChmodNode createChmodNode() {
        return new ManagedChmodNode();
    }

    @Override
    public StrolNode createStrolNode() {
        return null;
    }

    private static final class ManagedUnameNode extends Node implements UnameNode {
        @Override
        public UtsName execute() {
            return new UtsName() {
                @Override
                public String sysname() {
                    return System.getProperty("os.name");
                }

                @Override
                public String release() {
                    return "";
                }

                @Override
                public String version() {
                    return System.getProperty("os.version");
                }

                @Override
                public String machine() {
                    return System.getProperty("os.arch");
                }

                @Override
                public String nodename() {
                    return "";
                }
            };
        }
    }

    @Override
    public UnameNode createUnameNode() {
        return new ManagedUnameNode();
    }

    @Override
    public GlobNode createGlobNode() {
        return null;
    }

    private static final class ManagedSetShutdownFlagNode extends Node implements SetShutdownFlagNode {
    
        @Override
        public void execute(boolean value) {
            // do nothing
        }
    }

    @Override
    public SetShutdownFlagNode createSetShutdownFlagNode() {
        return new ManagedSetShutdownFlagNode();
    }
}
