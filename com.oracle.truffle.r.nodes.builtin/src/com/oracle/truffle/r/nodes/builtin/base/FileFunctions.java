/*
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, a copy is available at
 * https://www.R-project.org/Licenses/
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.constant;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.gte;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.instanceOf;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.intNA;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.logicalNA;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.lte;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.nullValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.size;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.RVisibility.OFF;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.IO;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.NoSuchFileException;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleFile;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.unary.CastStringNode;
import com.oracle.truffle.r.nodes.unary.CastStringNodeGen;
import com.oracle.truffle.r.runtime.FileSystemUtils;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.RContext.ConsoleIO;
import com.oracle.truffle.r.runtime.context.TruffleRLanguage;
import com.oracle.truffle.r.runtime.data.RBaseObject;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.nodes.attributes.SpecialAttributesFunctions.SetClassAttributeNode;

// Much of this code was influences/transcribed from GnuR src/main/platform.c

public class FileFunctions {

    @RBuiltin(name = "file.access", kind = INTERNAL, parameterNames = {"names", "mode"}, behavior = IO)
    public abstract static class FileAccess extends RBuiltinNode.Arg2 {
        private static final int EXECUTE = 1;
        private static final int WRITE = 2;
        private static final int READ = 4;

        static {
            Casts casts = new Casts(FileAccess.class);
            casts.arg("names").mustBe(stringValue()).asStringVector();
            casts.arg("mode").asIntegerVector().findFirst().mustBe(gte(0).and(lte(7)));
        }

        @Specialization
        @TruffleBoundary
        protected Object fileAccess(RStringVector names, int mode,
                        @CachedContext(TruffleRLanguage.class) TruffleLanguage.ContextReference<RContext> ctxRef) {
            int[] data = new int[names.getLength()];
            for (int i = 0; i < data.length; i++) {
                TruffleFile file = ctxRef.get().getSafeTruffleFile(names.getDataAt(i));
                if (file.exists()) {
                    if ((mode & EXECUTE) != 0 && !file.isExecutable()) {
                        data[i] = -1;
                    }
                    if ((mode & READ) != 0 && !file.isReadable()) {
                        data[i] = -1;
                    }
                    if ((mode & WRITE) != 0 && !file.isWritable()) {
                        data[i] = -1;
                    }
                } else {
                    data[i] = -1;
                }
            }
            return RDataFactory.createIntVector(data, RDataFactory.COMPLETE_VECTOR);
        }
    }

    @RBuiltin(name = "file.append", kind = INTERNAL, parameterNames = {"file1", "file2"}, behavior = IO)
    public abstract static class FileAppend extends RBuiltinNode.Arg2 {

        static {
            Casts casts = new Casts(FileAppend.class);
            casts.arg("file1").mustBe(stringValue()).asStringVector();
            casts.arg("file2").mustBe(stringValue()).asStringVector();
        }

        @Specialization
        @TruffleBoundary
        protected RLogicalVector doFileAppend(RStringVector file1Vec, RStringVector file2Vec,
                        @CachedContext(TruffleRLanguage.class) TruffleLanguage.ContextReference<RContext> ctxRef) {
            /*
             * There are two simple (non-trivial) cases and one tricky 1. 1. Append one or more
             * files to a single file (len1 == 1, len2 >= 1) 2. Append one file to one file for
             * several files (len1 == len2)
             *
             * The tricky case is when len1 > 1 && len2 > len1. E.g. f1,f2 <- g1,g2,g3 In this case,
             * this is really f1,f2,f1 <- g1,g2,g3
             */

            int len1 = file1Vec.getLength();
            int len2 = file2Vec.getLength();
            if (len1 < 1) {
                throw error(RError.Message.FILE_APPEND_TO);
            }
            if (len2 < 1) {
                return RDataFactory.createEmptyLogicalVector();
            }
            int len = len1 > len2 ? len1 : len2;
            byte[] status = new byte[len];
            if (len1 == 1) {
                String file1 = file1Vec.getDataAt(0);
                if (!RRuntime.isNA(file1)) {
                    RContext context = ctxRef.get();
                    try (BufferedOutputStream out = new BufferedOutputStream(context.getSafeTruffleFile(file1).newOutputStream(StandardOpenOption.APPEND))) {
                        for (int f2 = 0; f2 < len2; f2++) {
                            String file2 = file2Vec.getDataAt(f2);
                            status[f2] = RRuntime.asLogical(appendFile(context, out, file2));
                        }
                    } catch (IOException ex) {
                        // failure to open output file not reported as error by GnuR, just status
                    }
                }
            } else {
                // align vectors, redundant if len1 == len2, but avoids duplication
                String[] file1A = new String[len];
                String[] file2A = new String[len];
                for (int f = 0; f < len; f++) {
                    file1A[f] = file1Vec.getDataAt(f % len1);
                    file2A[f] = file2Vec.getDataAt(f % len2);
                }
                for (int f = 0; f < len; f++) {
                    String file1 = file1A[f];
                    if (!RRuntime.isNA(file1)) {
                        RContext context = ctxRef.get();
                        try (BufferedOutputStream out = new BufferedOutputStream(
                                        context.getSafeTruffleFile(file1).newOutputStream(StandardOpenOption.APPEND))) {
                            String file2 = file2A[f];
                            status[f] = RRuntime.asLogical(appendFile(context, out, file2));
                        } catch (IOException ex) {
                            // status is set
                        }
                    }
                }
            }
            return RDataFactory.createLogicalVector(status, RDataFactory.COMPLETE_VECTOR);
        }

        private boolean appendFile(RContext context, BufferedOutputStream out, String pathArg) {
            if (RRuntime.isNA(pathArg)) {
                return false;
            }
            TruffleFile file = context.getSafeTruffleFile(pathArg);
            if (!file.exists()) {
                // not an error (cf GnuR), just status
                return false;
            } else {
                byte[] buf;
                try {
                    buf = new byte[(int) file.size()];
                } catch (IOException ex) {
                    return false;
                }
                try (BufferedInputStream in = new BufferedInputStream(file.newInputStream())) {
                    in.read(buf);
                } catch (IOException ex) {
                    // not an error (cf GnuR), just status
                    return false;
                }
                try {
                    out.write(buf);
                    return true;
                } catch (IOException ex) {
                    warning(RError.Message.FILE_APPEND_WRITE);
                    return false;
                }
            }
        }
    }

    @RBuiltin(name = "file.create", kind = INTERNAL, parameterNames = {"vec", "showWarnings"}, behavior = IO)
    public abstract static class FileCreate extends RBuiltinNode.Arg2 {

        static {
            Casts casts = new Casts(FileCreate.class);
            casts.arg("vec").mustBe(stringValue()).asStringVector();
            casts.arg("showWarnings").asLogicalVector().findFirst().mapIf(logicalNA(), constant(RRuntime.LOGICAL_FALSE));
        }

        @Specialization
        @TruffleBoundary
        protected Object doFileCreate(RStringVector vec, byte showWarnings,
                        @CachedContext(TruffleRLanguage.class) TruffleLanguage.ContextReference<RContext> ctxRef) {
            byte[] status = new byte[vec.getLength()];
            for (int i = 0; i < status.length; i++) {
                String path = vec.getDataAt(i);
                if (RRuntime.isNA(path)) {
                    status[i] = RRuntime.LOGICAL_FALSE;
                } else {
                    boolean ok = true;
                    try {
                        ctxRef.get().getSafeTruffleFile(path).newOutputStream().close();
                    } catch (IOException ex) {
                        ok = false;
                        if (showWarnings == RRuntime.LOGICAL_TRUE) {
                            warning(RError.Message.FILE_CANNOT_CREATE, path);
                        }
                    }
                    status[i] = RRuntime.asLogical(ok);
                }
            }
            return RDataFactory.createLogicalVector(status, RDataFactory.COMPLETE_VECTOR);
        }
    }

    @RBuiltin(name = "file.info", kind = INTERNAL, parameterNames = {"fn", "extra_cols"}, behavior = IO)
    public abstract static class FileInfo extends RBuiltinNode.Arg2 {
        // @formatter:off
        private  enum Column {
            size, isdir, mode, mtime, ctime, atime, uid, gid, uname, grname;
            private static final Column[] VALUES = values();
        }
        // @formatter:on
        private static final String[] NAMES = new String[]{Column.size.name(), Column.isdir.name(), Column.mode.name(), Column.mtime.name(), Column.ctime.name(), Column.atime.name(),
                        Column.uid.name(), Column.gid.name(), Column.uname.name(), Column.grname.name()};
        private static final RStringVector NAMES_VECTOR = RDataFactory.createStringVector(NAMES, RDataFactory.COMPLETE_VECTOR);
        private static final RStringVector OCTMODE = RDataFactory.createStringVectorFromScalar("octmode");

        @Child private SetClassAttributeNode setClassAttrNode;

        static {
            Casts casts = new Casts(FileInfo.class);
            casts.arg("extra_cols").asLogicalVector().findFirst().map(toBoolean());
        }

        @Specialization
        @TruffleBoundary
        protected RList doFileInfo(RStringVector vec, @SuppressWarnings("unused") Boolean extraCols,
                        @CachedContext(TruffleRLanguage.class) TruffleLanguage.ContextReference<RContext> ctxRef) {
            /*
             * Create a list, the elements of which are vectors of length vec.getLength() containing
             * the information. The R closure that called the .Internal turns the result into a
             * dataframe and sets the row.names attributes to the paths in vec. It also updates the
             * mtime, ctime, atime fields using .POSIXct.
             *
             * We try to use the JDK classes, even though they provide a more abstract interface
             * than R. In particular there seems to be no way to get the uid/gid values. We might be
             * better off justing using a native call.
             *
             * TODO implement extras_cols=FALSE
             */
            int vecLength = vec.getLength();
            Object[] data = new Object[NAMES.length];
            boolean[] complete = new boolean[NAMES.length];
            for (int n = 0; n < Column.VALUES.length; n++) {
                data[n] = createColumnData(Column.VALUES[n], vecLength);
                complete[n] = RDataFactory.COMPLETE_VECTOR; // optimistic
            }
            for (int i = 0; i < vecLength; i++) {
                String vecPath = vec.getDataAt(i);
                TruffleFile file = ctxRef.get().getSafeTruffleFile(vecPath);
                // missing defaults to NA
                if (file.exists()) {
                    double size = RRuntime.DOUBLE_NA;
                    byte isdir = RRuntime.LOGICAL_NA;
                    int mode = RRuntime.INT_NA;
                    int mtime = RRuntime.INT_NA;
                    int atime = RRuntime.INT_NA;
                    int ctime = RRuntime.INT_NA;
                    int uid = RRuntime.INT_NA;
                    int gid = RRuntime.INT_NA;
                    String uname = RRuntime.STRING_NA;
                    String grname = RRuntime.STRING_NA;
                    try {
                        size = file.size();
                        isdir = RRuntime.asLogical(file.isDirectory());
                        mtime = Utils.getTimeInSecs(file.getLastModifiedTime());
                        ctime = Utils.getTimeInSecs(file.getCreationTime());
                        atime = Utils.getTimeInSecs(file.getLastAccessTime());
                        mode = Utils.intFilePermissions(file.getPosixPermissions());
                        uname = file.getOwner().getName();
                        grname = file.getGroup().getName();
                    } catch (IOException ex) {
                        // ok, NA value is used
                    }
                    setColumnValue(Column.size, data, complete, i, size);
                    setColumnValue(Column.isdir, data, complete, i, isdir);
                    setColumnValue(Column.mode, data, complete, i, mode);
                    setColumnValue(Column.mtime, data, complete, i, mtime);
                    setColumnValue(Column.ctime, data, complete, i, ctime);
                    setColumnValue(Column.atime, data, complete, i, atime);
                    setColumnValue(Column.uid, data, complete, i, uid);
                    setColumnValue(Column.gid, data, complete, i, gid);
                    setColumnValue(Column.uname, data, complete, i, uname);
                    setColumnValue(Column.grname, data, complete, i, grname);
                } else {
                    for (int n = 0; n < Column.VALUES.length; n++) {
                        setNA(Column.VALUES[n], data, i);
                        complete[n] = false;
                    }
                }
            }
            for (int n = 0; n < Column.VALUES.length; n++) {
                data[n] = createColumnResult(Column.VALUES[n], data[n], complete[n]);
            }
            return RDataFactory.createList(data, NAMES_VECTOR);
        }

        private static Object createColumnData(Column column, int vecLength) {
            // @formatter:off
            switch(column) {
                case size: return new double[vecLength];
                case isdir: return new byte[vecLength];
                case mode: case mtime: case ctime: case atime:
                case uid: case gid: return new int[vecLength];
                case uname: case grname: return new String[vecLength];
                default: throw RInternalError.shouldNotReachHere();
            }
            // @formatter:on
        }

        private static void updateComplete(int slot, boolean[] complete, boolean update) {
            if (complete[slot]) {
                complete[slot] = update;
            }
        }

        private static void setColumnValue(Column column, Object[] data, boolean[] complete, int index, Object value) {
            int slot = column.ordinal();
            // @formatter:off
            switch(column) {
                case size: ((double[]) data[slot])[index] = (double) value; updateComplete(slot, complete, (double) value != RRuntime.DOUBLE_NA); return;
                case isdir: ((byte[]) data[slot])[index] = (byte) value; updateComplete(slot, complete, (byte) value != RRuntime.LOGICAL_NA); return;
                case mode: case mtime: case ctime: case atime:
                case uid: case gid: ((int[]) data[slot])[index] = (int) value; updateComplete(slot, complete, (int) value != RRuntime.INT_NA); return;
                case uname: case grname: ((String[]) data[slot])[index] = (String) value; updateComplete(slot, complete, !RRuntime.isNA((String) value)); return;
                default: throw RInternalError.shouldNotReachHere();
            }
            // @formatter:on
        }

        private static void setNA(Column column, Object[] data, int index) {
            int slot = column.ordinal();
            // @formatter:off
            switch(column) {
                case size: ((double[]) data[slot])[index] = RRuntime.DOUBLE_NA; return;
                case isdir: ((byte[]) data[slot])[index] = RRuntime.LOGICAL_NA; return;
                case mode: case mtime: case ctime: case atime:
                case uid: case gid: ((int[]) data[slot])[index] = RRuntime.INT_NA; return;
                case uname: case grname: ((String[]) data[slot])[index] = RRuntime.STRING_NA; return;
                default: throw RInternalError.shouldNotReachHere();
            }
            // @formatter:on
        }

        private Object createColumnResult(Column column, Object data, boolean complete) {
            // @formatter:off
            switch(column) {
                case size: return RDataFactory.createDoubleVector((double[]) data, complete);
                case isdir: return RDataFactory.createLogicalVector((byte[]) data, complete);
                case mode:
                    if (setClassAttrNode == null) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        setClassAttrNode = insert(SetClassAttributeNode.create());
                    }
                    RIntVector res = RDataFactory.createIntVector((int[]) data, complete);
                    setClassAttrNode.setAttr(res, OCTMODE);
                    return res;
                case mtime: case ctime: case atime:
                case uid: case gid: return RDataFactory.createIntVector((int[]) data, complete);
                case uname: case grname: return RDataFactory.createStringVector((String[]) data, complete);
                default: throw RInternalError.shouldNotReachHere();
            }
            // @formatter:on
        }
    }

    private abstract static class FileLinkAdaptor extends RBuiltinNode.Arg2 {

        protected static void casts(Class<? extends FileLinkAdaptor> builtinClass) {
            Casts casts = new Casts(builtinClass);
            casts.arg("from").mustBe(stringValue(), RError.Message.INVALID_FIRST_FILENAME).asStringVector();
            casts.arg("to").mustBe(stringValue(), RError.Message.INVALID_SECOND_FILENAME).asStringVector();
        }

        @TruffleBoundary
        protected Object doFileLink(RContext context, RStringVector vecFrom, RStringVector vecTo, boolean symbolic) {
            int lenFrom = vecFrom.getLength();
            int lenTo = vecTo.getLength();
            if (lenFrom < 1) {
                throw error(RError.Message.NOTHING_TO_LINK);
            }
            if (lenTo < 1) {
                return RDataFactory.createLogicalVector(0);
            }
            int len = lenFrom > lenTo ? lenFrom : lenTo;

            byte[] status = new byte[len];
            for (int i = 0; i < len; i++) {
                String from = vecFrom.getDataAt(i % lenFrom);
                String to = vecTo.getDataAt(i % lenTo);
                if (RRuntime.isNA(from) || RRuntime.isNA(to)) {
                    status[i] = RRuntime.LOGICAL_FALSE;
                } else {
                    TruffleFile fromFile = context.getSafeTruffleFile(from);
                    TruffleFile toFile = context.getSafeTruffleFile(to);
                    status[i] = RRuntime.LOGICAL_TRUE;
                    try {
                        if (symbolic) {
                            toFile.createSymbolicLink(fromFile);
                        } else {
                            toFile.createLink(fromFile);
                        }
                    } catch (UnsupportedOperationException | IOException ex) {
                        status[i] = RRuntime.LOGICAL_FALSE;
                        warning(RError.Message.FILE_CANNOT_LINK, from, to, ex.getMessage());
                    }
                }
            }
            return RDataFactory.createLogicalVector(status, RDataFactory.COMPLETE_VECTOR);
        }
    }

    @RBuiltin(name = "file.link", kind = INTERNAL, parameterNames = {"from", "to"}, behavior = IO)
    public abstract static class FileLink extends FileLinkAdaptor {

        static {
            Casts casts = new Casts(FileLink.class);
            casts.arg("from").mustBe(stringValue(), RError.Message.INVALID_FIRST_FILENAME).asStringVector();
            casts.arg("to").mustBe(stringValue(), RError.Message.INVALID_SECOND_FILENAME).asStringVector();
        }

        @Specialization
        @TruffleBoundary
        protected Object doFileLink(RStringVector vecFrom, RStringVector vecTo,
                        @CachedContext(TruffleRLanguage.class) TruffleLanguage.ContextReference<RContext> ctxRef) {
            return doFileLink(ctxRef.get(), vecFrom, vecTo, false);
        }
    }

    @RBuiltin(name = "file.symlink", kind = INTERNAL, parameterNames = {"from", "to"}, behavior = IO)
    public abstract static class FileSymLink extends FileLinkAdaptor {

        static {
            Casts casts = new Casts(FileSymLink.class);
            casts.arg("from").mustBe(stringValue(), RError.Message.INVALID_FIRST_FILENAME).asStringVector();
            casts.arg("to").mustBe(stringValue(), RError.Message.INVALID_SECOND_FILENAME).asStringVector();
        }

        @Specialization
        @TruffleBoundary
        protected Object doFileSymLink(RStringVector vecFrom, RStringVector vecTo,
                        @CachedContext(TruffleRLanguage.class) TruffleLanguage.ContextReference<RContext> ctxRef) {
            return doFileLink(ctxRef.get(), vecFrom, vecTo, true);
        }
    }

    @RBuiltin(name = "file.remove", kind = INTERNAL, parameterNames = {"file"}, behavior = IO)
    public abstract static class FileRemove extends RBuiltinNode.Arg1 {

        static {
            Casts casts = new Casts(FileRemove.class);
            casts.arg("file").mustBe(stringValue(), RError.Message.INVALID_FIRST_FILENAME).asStringVector();
        }

        @Specialization
        @TruffleBoundary
        protected Object doFileRemove(RStringVector vec,
                        @CachedContext(TruffleRLanguage.class) TruffleLanguage.ContextReference<RContext> ctxRef) {
            byte[] status = new byte[vec.getLength()];
            for (int i = 0; i < status.length; i++) {
                String path = vec.getDataAt(i);
                if (RRuntime.isNA(path)) {
                    status[i] = RRuntime.LOGICAL_FALSE;
                } else {
                    TruffleFile f = ctxRef.get().getSafeTruffleFile(path);
                    boolean ok;
                    try {
                        f.delete();
                        ok = true;
                    } catch (NoSuchFileException ex) {
                        ok = false;
                    } catch (IOException ex) {
                        ok = false;
                    }
                    status[i] = RRuntime.asLogical(ok);
                    if (!ok) {
                        warning(RError.Message.FILE_CANNOT_REMOVE, path);
                    }
                }
            }
            return RDataFactory.createLogicalVector(status, RDataFactory.COMPLETE_VECTOR);
        }
    }

    @RBuiltin(name = "file.rename", kind = INTERNAL, parameterNames = {"from", "to"}, behavior = IO)
    public abstract static class FileRename extends RBuiltinNode.Arg2 {

        static {
            Casts casts = new Casts(FileRename.class);
            casts.arg("from").mustBe(stringValue()).asStringVector();
            casts.arg("to").mustBe(stringValue()).asStringVector();
        }

        @Specialization
        @TruffleBoundary
        protected Object doFileRename(RStringVector vecFrom, RStringVector vecTo,
                        @CachedContext(TruffleRLanguage.class) TruffleLanguage.ContextReference<RContext> ctxRef) {
            int len = vecFrom.getLength();
            if (len != vecTo.getLength()) {
                throw error(RError.Message.FROM_TO_DIFFERENT);
            }
            byte[] status = new byte[len];
            for (int i = 0; i < len; i++) {
                String from = vecFrom.getDataAt(i);
                String to = vecTo.getDataAt(i);
                // GnuR's behavior regarding NA is quite inconsistent (error, warning, ignored)
                // we choose ignore
                if (RRuntime.isNA(from) || RRuntime.isNA(to)) {
                    status[i] = RRuntime.LOGICAL_FALSE;
                } else {
                    boolean ok;
                    try {
                        RContext context = ctxRef.get();
                        context.getSafeTruffleFile(from).move(context.getSafeTruffleFile(to));
                        ok = true;
                    } catch (IOException ex) {
                        ok = false;
                    }
                    status[i] = RRuntime.asLogical(ok);
                    if (!ok) {
                        warning(RError.Message.FILE_CANNOT_RENAME, from, to);
                    }
                }
            }
            return RDataFactory.createLogicalVector(status, RDataFactory.COMPLETE_VECTOR);
        }
    }

    @RBuiltin(name = "file.exists", kind = INTERNAL, parameterNames = {"file"}, behavior = IO)
    public abstract static class FileExists extends RBuiltinNode.Arg1 {

        static {
            Casts casts = new Casts(FileExists.class);
            casts.arg("file").mustBe(stringValue()).asStringVector();
        }

        @Specialization
        @TruffleBoundary
        protected Object doFileExists(RStringVector vec,
                        @CachedContext(TruffleRLanguage.class) TruffleLanguage.ContextReference<RContext> ctxRef) {
            byte[] status = new byte[vec.getLength()];
            for (int i = 0; i < status.length; i++) {
                String path = vec.getDataAt(i);
                if (RRuntime.isNA(path) || path.isEmpty()) {
                    status[i] = RRuntime.LOGICAL_FALSE;
                } else {
                    TruffleFile f = ctxRef.get().getSafeTruffleFile(path);
                    // TODO R's notion of exists may not match Java - check
                    if (f.exists()) {
                        try {
                            status[i] = f.getCanonicalFile().exists() ? RRuntime.LOGICAL_TRUE : RRuntime.LOGICAL_FALSE;
                        } catch (NoSuchFileException ex) {
                            // e.g. /dirExists/dirDoesNotExist/..")
                            // technicaly speaking the file exists,
                            // but getCanonicalFile() seems to be in accord with GNUR on this
                            // see also .getSafeTruffleFile()
                            status[i] = RRuntime.LOGICAL_FALSE;
                        } catch (IOException ex) {
                            throw RInternalError.shouldNotReachHere();
                        }
                    } else {
                        status[i] = RRuntime.LOGICAL_FALSE;
                    }
                }
            }
            return RDataFactory.createLogicalVector(status, RDataFactory.COMPLETE_VECTOR);
        }
    }

    // TODO Implement all the options
    @RBuiltin(name = "list.files", kind = INTERNAL, parameterNames = {"path", "pattern", "all.files", "full.names", "recursive", "ignore.case", "include.dirs", "no.."}, behavior = IO)
    public abstract static class ListFiles extends RBuiltinNode.Arg8 {
        private static final String DOT = ".";
        private static final String DOTDOT = "..";

        static {
            Casts casts = new Casts(ListFiles.class);
            casts.arg("path").mustBe(stringValue()).asStringVector();
            casts.arg("pattern").allowNull().mustBe(stringValue());
            casts.arg("all.files").asLogicalVector().findFirst().mustNotBeNA().map(toBoolean());
            casts.arg("full.names").asLogicalVector().findFirst().mustNotBeNA().map(toBoolean());
            casts.arg("recursive").asLogicalVector().findFirst().mustNotBeNA().map(toBoolean());
            casts.arg("ignore.case").asLogicalVector().findFirst().mustNotBeNA().map(toBoolean());
            casts.arg("include.dirs").asLogicalVector().findFirst().mustNotBeNA().map(toBoolean());
            casts.arg("no..").asLogicalVector().findFirst().mustNotBeNA().map(toBoolean());
        }

        @SuppressWarnings("unused")
        @Specialization
        @TruffleBoundary
        protected RStringVector doListFiles(RStringVector vec, RNull patternVec, boolean allFiles, boolean fullNames, boolean recursive, boolean ignoreCase, boolean includeDirs,
                        boolean noDotDot,
                        @CachedContext(TruffleRLanguage.class) TruffleLanguage.ContextReference<RContext> ctxRef) {
            return doListFilesBody(vec, null, allFiles, fullNames, recursive, ignoreCase, includeDirs, noDotDot, ctxRef.get());
        }

        @Specialization
        @TruffleBoundary
        protected RStringVector doListFiles(RStringVector vec, RStringVector patternVec, boolean allFiles, boolean fullNames, boolean recursive, boolean ignoreCase,
                        boolean includeDirs, boolean noDotDot,
                        @CachedContext(TruffleRLanguage.class) TruffleLanguage.ContextReference<RContext> ctxRef) {
            /*
             * Pattern in first element of vector, remaining elements are ignored (as per GnuR).
             * N.B. The pattern matches file names not paths, which means we cannot just use the
             * Java File path matcher.
             */

            String pattern = null;
            if (patternVec.getLength() > 0) {
                if (RRuntime.isNA(patternVec.getDataAt(0))) {
                    throw error(RError.Message.INVALID_ARGUMENT, "pattern");
                } else {
                    pattern = patternVec.getDataAt(0);
                }
            }

            return doListFilesBody(vec, pattern, allFiles, fullNames, recursive, ignoreCase, includeDirs, noDotDot, ctxRef.get());
        }

        private static RStringVector doListFilesBody(RStringVector vec, String patternString, boolean allFiles, boolean fullNames, boolean recursive,
                        boolean ignoreCase, boolean includeDirsIn, boolean noDotDot, RContext context) {
            boolean includeDirs = !recursive || includeDirsIn;
            int flags = ignoreCase ? Pattern.CASE_INSENSITIVE : 0;
            Pattern pattern = null;
            try {
                pattern = patternString == null ? null : Pattern.compile(patternString, flags);
            } catch (PatternSyntaxException e) {
                // Try to convert the pattern from the wildcard syntax to regexp
                String regexpPatternString = Utils.wildcardToRegex(patternString);
                pattern = Pattern.compile(regexpPatternString, flags);
            }
            // Curiously the result is not a vector of same length as the input,
            // as typical for R, but a single vector, which means duplicates may occur
            ArrayList<String> files = new ArrayList<>();
            for (int i = 0; i < vec.getLength(); i++) {
                String vecPathString = vec.getDataAt(i);
                TruffleFile root = context.getSafeTruffleFile(vecPathString);
                if (vecPathString.isEmpty() || !root.exists()) {
                    // File.exists() returns false for "" but TF gives true
                    continue;
                }
                try (Stream<TruffleFile> stream = FileSystemUtils.find(root, recursive ? Integer.MAX_VALUE : 1, new FileMatcher(pattern, allFiles, includeDirs))) {
                    Iterator<TruffleFile> iter = stream.iterator();
                    while (iter.hasNext()) {
                        TruffleFile file = iter.next();
                        if (file.equals(root)) {
                            continue;
                        }
                        file = root.relativize(file);
                        String p = file.getPath();
                        if (p.startsWith(vecPathString)) {
                            p = p.substring(vecPathString.length());
                            if (p.startsWith("/")) {
                                p = p.substring(1);
                            }
                        }
                        if (fullNames) {
                            p = p.isEmpty() ? vecPathString : vecPathString + context.getEnv().getFileNameSeparator() + p;
                            files.add(p);
                        } else {
                            files.add(p);
                        }
                    }
                    /*
                     * Annoyingly "." and ".." are never visited by Files.find, so we have to
                     * process them manually.
                     */
                    if (!recursive && allFiles && !noDotDot) {
                        if (pattern == null || pattern.matcher(DOT).find()) {
                            files.add(fullNames ? context.getSafeTruffleFile(vecPathString).resolve(DOT).getPath() : DOT);
                        }
                        if (pattern == null || pattern.matcher(DOTDOT).find()) {
                            files.add(fullNames ? context.getSafeTruffleFile(vecPathString).resolve(DOTDOT).getPath() : DOTDOT);
                        }
                    }
                } catch (IOException | UncheckedIOException ex) {
                    // ignored
                }
            }
            if (files.size() == 0) {
                // The manual says "" but GnuR returns an empty vector
                return RDataFactory.createEmptyStringVector();
            } else {
                String fileSeparator = context.getEnv().getFileNameSeparator();
                String[] data = new String[files.size()];
                files.toArray(data);
                /*
                 * During sorting, GNU-R ignores the dot prefix in hidden files ie. we have to
                 * consider every hidden file without the dot at the beginning.
                 */
                Arrays.sort(data, (filePath1, filePath2) -> {
                    String filename1WithoutDot = skipLeadingDotInFilename(filePath1, fileSeparator);
                    String filename2WithoutDot = skipLeadingDotInFilename(filePath2, fileSeparator);
                    return filename1WithoutDot.compareTo(filename2WithoutDot);
                });
                return RDataFactory.createStringVector(data, RDataFactory.COMPLETE_VECTOR);
            }
        }

        private static String skipLeadingDotInFilename(String filePath, String fileSeparator) {
            String[] items = filePath.split(Pattern.quote(fileSeparator));
            String baseName = items[items.length - 1];
            String baseNameWithoutDot = baseName.charAt(0) == '.' ? baseName.substring(1) : baseName;
            if (!baseNameWithoutDot.equals(baseName)) {
                items[items.length - 1] = baseNameWithoutDot;
                return String.join(fileSeparator, items);
            } else {
                return filePath;
            }
        }

        private static class FileMatcher implements Predicate<TruffleFile> {
            final Pattern pattern;
            final boolean includeDirs;
            final boolean allFiles;

            FileMatcher(Pattern pattern, boolean allFiles, boolean includeDirs) {
                this.allFiles = allFiles;
                this.includeDirs = includeDirs;
                this.pattern = pattern;
            }

            @Override
            public boolean test(TruffleFile f) {
                if (f.isDirectory() && !includeDirs) {
                    return false;
                }
                // Note: getName on "/" causes NPE
                String name = f.getPath().equals("/") ? "" : f.getName();
                if (!allFiles && !name.isEmpty() && name.charAt(0) == '.') {
                    return false;
                }
                if (pattern == null) {
                    return true;
                }
                Matcher m = pattern.matcher(name);
                boolean result = m.find();
                return result;
            }
        }
    }

    @RBuiltin(name = "list.dirs", kind = INTERNAL, parameterNames = {"directory", "full.names", "recursive"}, behavior = IO)
    public abstract static class ListDirs extends RBuiltinNode.Arg3 {

        static {
            Casts casts = new Casts(ListDirs.class);
            casts.arg("directory").mustBe(stringValue()).asStringVector();
            casts.arg("full.names").asLogicalVector().findFirst().mustNotBeNA().map(toBoolean());
            casts.arg("recursive").asLogicalVector().findFirst().mustNotBeNA().map(toBoolean());
        }

        @Specialization
        @TruffleBoundary
        protected RStringVector listDirs(RStringVector paths, boolean fullNames, boolean recursive,
                        @CachedContext(TruffleRLanguage.class) TruffleLanguage.ContextReference<RContext> ctxRef) {
            ArrayList<String> dirList = new ArrayList<>();
            for (int i = 0; i < paths.getLength(); i++) {
                String vecPathString = paths.getDataAt(i);
                RContext context = ctxRef.get();
                TruffleFile root = context.getSafeTruffleFile(vecPathString);
                if (!root.exists()) {
                    continue;
                }
                try (Stream<TruffleFile> stream = FileSystemUtils.find(root, recursive ? Integer.MAX_VALUE : 1, new FileMatcher())) {
                    Iterator<TruffleFile> iter = stream.iterator();
                    while (iter.hasNext()) {
                        TruffleFile dir = iter.next();
                        if (!recursive && dir.equals(root)) {
                            continue;
                        }
                        dir = root.relativize(dir);
                        if (fullNames) {
                            String p = dir.getPath();
                            p = p.isEmpty() ? vecPathString : vecPathString + context.getEnv().getFileNameSeparator() + p;
                            dirList.add(p);
                        } else {
                            dirList.add(dir.getPath());
                        }
                    }
                } catch (IOException ex) {
                    // ignored
                }
            }
            String[] data = new String[dirList.size()];
            dirList.toArray(data);
            Arrays.sort(data);
            return RDataFactory.createStringVector(data, RDataFactory.COMPLETE_VECTOR);
        }

        private static class FileMatcher implements Predicate<TruffleFile> {
            @Override
            public boolean test(TruffleFile f) {
                boolean result = f.isDirectory();
                return result;
            }
        }
    }

    // TODO handle the general case, which is similar to paste, dispatch to as.character S3
    // methods
    @RBuiltin(name = "file.path", kind = INTERNAL, parameterNames = {"paths", "fsep"}, behavior = IO)
    public abstract static class FilePath extends RBuiltinNode.Arg2 {

        static {
            Casts casts = new Casts(FilePath.class);
            casts.arg("paths").mustBe(instanceOf(RList.class), RError.Message.INVALID_FIRST_ARGUMENT);
            casts.arg("fsep").mustBe(stringValue()).asStringVector().findFirst().mustNotBeNA();
        }

        @Child private CastStringNode castStringNode;

        private CastStringNode initCastStringNode() {
            if (castStringNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                castStringNode = insert(CastStringNodeGen.create(false, false, false));
            }
            return castStringNode;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "lengthZero(vec)")
        @TruffleBoundary
        protected RStringVector doFilePathZero(RList vec, String fsep) {
            return RDataFactory.createEmptyStringVector();
        }

        @Specialization(guards = "!lengthZero(args)")
        @TruffleBoundary
        protected RStringVector doFilePath(RList args, String fsep) {
            int resultLength = 0;
            int argsLen = args.getLength();
            for (int i = 0; i < argsLen; i++) {
                Object elem = args.getDataAt(i);
                int argLength;
                if (elem instanceof RAbstractContainer) {
                    argLength = ((RAbstractContainer) elem).getLength();
                } else {
                    argLength = 1;
                }
                if (argLength > resultLength) {
                    resultLength = argLength;
                }
            }
            if (resultLength == 0) {
                return RDataFactory.createEmptyStringVector();
            }
            String[] result = new String[resultLength];
            String[][] inputs = new String[argsLen][];
            for (int i = 0; i < argsLen; i++) {
                Object elem = args.getDataAt(i);
                if (elem instanceof RBaseObject && ((RBaseObject) elem).isS4()) {
                    throw RError.nyi(this, "list files: S4 elem");
                } else if (elem instanceof RSymbol) {
                    inputs[i] = new String[]{((RSymbol) elem).getName()};
                } else {
                    if (!(elem instanceof String || elem instanceof RStringVector)) {
                        elem = initCastStringNode().executeString(elem);
                    }
                }
                if (elem instanceof String) {
                    inputs[i] = new String[]{(String) elem};
                } else if (elem instanceof RStringVector) {
                    if (((RStringVector) elem).getLength() == 0) {
                        return RDataFactory.createEmptyStringVector();
                    }
                    inputs[i] = ((RStringVector) elem).getReadonlyStringData();
                } else {
                    throw error(RError.Message.NON_STRING_ARG_TO_INTERNAL_PASTE);
                }
            }
            StringBuilder pathBuilder = new StringBuilder();
            for (int i = 0; i < resultLength; i++) {
                pathBuilder.setLength(0);
                int len = 0;
                for (int j = 0; j < inputs.length; j++) {
                    len += inputs[j][i % inputs[j].length].length() + 1;
                }
                pathBuilder.ensureCapacity(len);
                for (int j = 0; j < inputs.length; j++) {
                    pathBuilder.append(inputs[j][i % inputs[j].length]);
                    if (j != inputs.length - 1) {
                        pathBuilder.append(fsep);
                    }
                }
                result[i] = pathBuilder.toString();
            }
            return RDataFactory.createStringVector(result, RDataFactory.COMPLETE_VECTOR);
        }

        protected static boolean lengthZero(RList list) {
            if (list.getLength() == 0) {
                return true;
            }
            for (int i = 0; i < list.getLength(); i++) {
                Object elem = list.getDataAt(i);
                if (elem instanceof RAbstractContainer) {
                    if (((RAbstractContainer) elem).getLength() == 0) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    /**
     * {@code file.copy} builtin. This is only called when the target is a directory.
     */
    @RBuiltin(name = "file.copy", kind = INTERNAL, parameterNames = {"from", "to", "overwrite", "recursive", "copy.mode", "copy.date"}, behavior = IO)
    public abstract static class FileCopy extends RBuiltinNode.Arg6 {

        static {
            Casts casts = new Casts(FileCopy.class);
            casts.arg("from").mustBe(stringValue()).asStringVector();
            casts.arg("to").mustBe(stringValue()).asStringVector();
            casts.arg("overwrite").asLogicalVector().findFirst().mustNotBeNA().map(toBoolean());
            casts.arg("recursive").asLogicalVector().findFirst().mustNotBeNA().map(toBoolean());
            casts.arg("copy.mode").asLogicalVector().findFirst().mustNotBeNA().map(toBoolean());
            casts.arg("copy.date").asLogicalVector().findFirst().mustNotBeNA().map(toBoolean());
        }

        @Specialization
        @TruffleBoundary
        protected RLogicalVector fileCopy(RStringVector vecFrom, RStringVector vecTo, boolean overwrite, boolean recursiveA, boolean copyMode, boolean copyDate,
                        @CachedContext(TruffleRLanguage.class) TruffleLanguage.ContextReference<RContext> ctxRef) {
            boolean recursive = recursiveA;
            int lenFrom = vecFrom.getLength();
            byte[] status = new byte[lenFrom];
            if (lenFrom > 0) {
                int lenTo = vecTo.getLength();
                if (lenTo != 1) {
                    throw error(RError.Message.INVALID_ARGUMENT, "to");
                }

                // Java cannot distinguish copy.mode and copy.dates
                CopyOption[] copyOptions;
                if (copyMode || copyDate) {
                    copyOptions = new CopyOption[overwrite ? 2 : 1];
                    copyOptions[overwrite ? 1 : 0] = StandardCopyOption.COPY_ATTRIBUTES;
                } else {
                    copyOptions = new CopyOption[overwrite ? 1 : 0];
                }
                if (overwrite) {
                    copyOptions[0] = StandardCopyOption.REPLACE_EXISTING;
                }
                TruffleFile toDir = null;
                RContext context = ctxRef.get();
                if (vecTo.getLength() == 1) {
                    TruffleFile vecTo0Path = context.getSafeTruffleFile(vecTo.getDataAt(0));
                    if (vecTo0Path.isDirectory()) {
                        toDir = vecTo0Path;
                    }
                }
                if (recursive) {
                    if (toDir == null) {
                        warning(RError.Message.FILE_COPY_RECURSIVE_IGNORED);
                        recursive = false;
                    }
                }

                for (int i = 0; i < lenFrom; i++) {
                    String from = vecFrom.getDataAt(i % lenFrom);
                    TruffleFile fromPath = context.getSafeTruffleFile(from);
                    String to = vecTo.getDataAt(i % lenTo);
                    TruffleFile toPath = context.getSafeTruffleFile(to);
                    assert !recursive || toDir != null;
                    status[i] = RRuntime.LOGICAL_FALSE;
                    try {
                        if (toDir != null) {
                            toPath = toDir.resolve(fromPath.getName());
                        }
                        if (fromPath.isDirectory()) {
                            if (recursive) {
                                assert toDir != null;
                                // to is just one dir (checked above)
                                boolean copyDirResult;
                                if (!overwrite) {
                                    try {
                                        copyDirResult = copyDir(fromPath, toPath, copyOptions);
                                    } catch (FileAlreadyExistsException ex) { // Ignore
                                        copyDirResult = false;
                                    }
                                } else {
                                    copyDirResult = copyDir(fromPath, toPath, copyOptions);
                                }
                                status[i] = RRuntime.asLogical(copyDirResult);
                            }
                        } else {
                            // copy to existing files is skipped unless overWrite
                            if (!toPath.exists() || overwrite) {
                                /*
                                 * toB Be careful if toPath is a directory, if empty Java will
                                 * replace it with a plain file, otherwise the copy will fail
                                 */
                                fromPath.copy(toPath, copyOptions);
                                status[i] = RRuntime.LOGICAL_TRUE;
                            }
                        }
                    } catch (UnsupportedOperationException | IOException ex) {
                        warning(RError.Message.FILE_CANNOT_COPY, from, to, ex.getMessage());
                    }
                }
            }
            return RDataFactory.createLogicalVector(status, RDataFactory.COMPLETE_VECTOR);
        }

        private static final class DirCopy implements FileVisitor<TruffleFile> {
            private final TruffleFile fromDir;
            private final TruffleFile toDir;
            private final CopyOption[] copyOptions;
            private boolean error;

            private DirCopy(TruffleFile fromDir, TruffleFile toDir, CopyOption[] copyOptions) {
                this.fromDir = fromDir;
                this.toDir = toDir;
                this.copyOptions = copyOptions;
            }

            @Override
            public FileVisitResult preVisitDirectory(TruffleFile dir, BasicFileAttributes attrs) {
                TruffleFile newDir = toDir.resolve(fromDir.relativize(dir).getPath());
                try {
                    dir.copy(newDir, copyOptions);
                } catch (FileAlreadyExistsException x) {
                    error = true;
                    // ok
                } catch (DirectoryNotEmptyException x) {
                    // ok
                } catch (IOException ex) {
                    error = true;
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(TruffleFile file, BasicFileAttributes attrs) throws IOException {
                TruffleFile newFile = toDir.resolve(fromDir.relativize(file).getPath());
                try {
                    file.copy(newFile, copyOptions);
                } catch (FileAlreadyExistsException x) {
                    error = true;
                    // ok
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(TruffleFile f, IOException exc) throws IOException {
                throw exc;
            }

            @Override
            public FileVisitResult postVisitDirectory(TruffleFile f, IOException exc) throws IOException {
                if (exc != null) {
                    throw exc;
                }
                return FileVisitResult.CONTINUE;
            }
        }

        private static boolean copyDir(TruffleFile fromDir, TruffleFile toDir, CopyOption[] copyOptions) throws IOException {
            DirCopy dirCopy = new DirCopy(fromDir, toDir, copyOptions);
            FileSystemUtils.walkFileTree(fromDir, dirCopy);
            return !dirCopy.error;
        }
    }

    @RBuiltin(name = "file.show", kind = INTERNAL, parameterNames = {"files", "header", "title", "delete.file", "pager"}, visibility = OFF, behavior = IO)
    public abstract static class FileShow extends RBuiltinNode.Arg5 {

        static {
            Casts casts = new Casts(FileShow.class);
            casts.arg("files").mustNotBeMissing().mustBe(nullValue().not(), Message.INVALID_FILENAME_SPECIFICATION).asStringVector();
            casts.arg("header").mustNotBeMissing().mustBe(nullValue().not(), Message.INVALID_ARG, "'headers'").asStringVector();
            casts.arg("title").mustNotBeMissing().mustBe(nullValue().not(), Message.INVALID_ARG, "'title'").asStringVector();
            casts.arg("delete.file").asLogicalVector().findFirst().map(toBoolean());
            casts.arg("pager").asStringVector().findFirst();
        }

        @Specialization
        @TruffleBoundary
        protected RNull show(RStringVector files, RStringVector header, RStringVector title, boolean deleteFile, @SuppressWarnings("unused") String pager,
                        @CachedContext(TruffleRLanguage.class) TruffleLanguage.ContextReference<RContext> ctxRef) {
            RContext ctx = ctxRef.get();
            ConsoleIO console = ctx.getConsole();
            for (int i = 0; i < title.getLength(); i++) {
                console.println("==== " + title.getDataAt(i) + " ====");
            }
            for (int i = 0; i < files.getLength(); i++) {
                if (i < header.getLength() && !header.getDataAt(i).isEmpty()) {
                    console.println("== " + header.getDataAt(i) + " ==");
                }
                try {
                    TruffleFile path = ctx.getSafeTruffleFile(files.getDataAt(i));
                    List<String> lines = FileSystemUtils.readAllLines(path);
                    for (String line : lines) {
                        console.println(line);
                    }
                    if (deleteFile) {
                        path.delete();
                    }
                } catch (IOException e) {
                    throw error(Message.GENERIC, e.getMessage());
                }
            }
            return RNull.instance;
        }
    }

    protected static RStringVector doXyzName(RStringVector vec, RContext context, BiFunction<RContext, String, String> fun) {
        boolean complete = RDataFactory.COMPLETE_VECTOR;
        String[] data = new String[vec.getLength()];
        for (int i = 0; i < data.length; i++) {
            String name = vec.getDataAt(i);
            if (RRuntime.isNA(name)) {
                data[i] = name;
                complete = RDataFactory.INCOMPLETE_VECTOR;
            } else if (name.length() == 0) {
                data[i] = name;
            } else {
                data[i] = fun.apply(context, name);
            }
        }
        return RDataFactory.createStringVector(data, complete);
    }

    @RBuiltin(name = "dirname", kind = INTERNAL, parameterNames = {"path"}, behavior = IO)
    public abstract static class DirName extends RBuiltinNode.Arg1 {

        static {
            Casts casts = new Casts(DirName.class);
            casts.arg("path").mustBe(stringValue(), RError.Message.CHAR_VEC_ARGUMENT);
        }

        @Specialization
        @TruffleBoundary
        protected RStringVector doDirName(RStringVector vec,
                        @CachedContext(TruffleRLanguage.class) TruffleLanguage.ContextReference<RContext> ctxRef) {
            return doXyzName(vec, ctxRef.get(), (context, name) -> {
                TruffleFile path = context.getSafeTruffleFile(name);
                TruffleFile parent = path.getParent();
                return parent != null ? parent.toString() : (path.getAbsoluteFile().getParent() != null ? "." : ctxRef.get().getEnv().getFileNameSeparator());
            });
        }
    }

    @RBuiltin(name = "basename", kind = INTERNAL, parameterNames = {"path"}, behavior = IO)
    public abstract static class BaseName extends RBuiltinNode.Arg1 {

        static {
            Casts casts = new Casts(BaseName.class);
            casts.arg("path").mustBe(stringValue(), RError.Message.CHAR_VEC_ARGUMENT);
        }

        @Specialization
        @TruffleBoundary
        protected RStringVector doDirName(RStringVector vec,
                        @CachedContext(TruffleRLanguage.class) TruffleLanguage.ContextReference<RContext> ctxRef) {
            return doXyzName(vec, ctxRef.get(), (context, name) -> {
                TruffleFile path = context.getSafeTruffleFile(name);
                String fileName = path.getName();
                return fileName != null ? fileName.toString() : name;
            });
        }
    }

    @RBuiltin(name = "unlink", visibility = OFF, kind = INTERNAL, parameterNames = {"x", "recursive", "force", "expand"}, behavior = IO)
    public abstract static class Unlink extends RBuiltinNode.Arg4 {

        static {
            Casts casts = new Casts(Unlink.class);
            casts.arg("x").mustBe(stringValue(), RError.Message.CHAR_VEC_ARGUMENT);
            casts.arg("recursive").asLogicalVector().findFirst().mustNotBeNA().map(toBoolean());
            casts.arg("force").asLogicalVector().findFirst().mustNotBeNA().map(toBoolean());
            casts.arg("expand").asLogicalVector().findFirst().mustNotBeNA().map(toBoolean());
        }

        @Specialization
        @TruffleBoundary
        protected static int doUnlink(RStringVector vec, boolean recursive, @SuppressWarnings("unused") boolean force, boolean expand,
                        @CachedContext(TruffleRLanguage.class) TruffleLanguage.ContextReference<RContext> ctxRef) {
            int result = 0;
            for (int i = -0; i < vec.getLength(); i++) {
                String pathPattern = vec.getDataAt(i);
                if (pathPattern.length() == 0 || RRuntime.isNA(pathPattern)) {
                    continue;
                }
                int firstGlobCharIdx = containsGlobChar(pathPattern);
                if (firstGlobCharIdx >= 0) {
                    result = removeGlob(ctxRef.get(), pathPattern, recursive, firstGlobCharIdx, result);
                } else {
                    TruffleFile file;
                    if (!expand && pathPattern.startsWith("~")) {
                        file = ctxRef.get().getEnv().getInternalTruffleFile(pathPattern);
                    } else {
                        file = ctxRef.get().getSafeTruffleFile(Utils.tildeExpand(pathPattern));
                    }
                    result = removeFile(file, recursive, result);
                }
            }
            return result;
        }

        private static int removeGlob(RContext context, String pathPattern, boolean recursive, int firstGlobCharIdx, int result) {
            String fileSeparator = context.getEnv().getFileNameSeparator();
            // we take as much as we can from the pathPatter as the search root
            int lastSeparator = pathPattern.substring(0, firstGlobCharIdx).lastIndexOf(fileSeparator);
            String searchRoot = (lastSeparator != -1) ? pathPattern.substring(0, lastSeparator) : "";
            try {
                int[] tmpResult = new int[]{result};
                final Pattern globRegex = Pattern.compile(FileSystemUtils.toUnixRegexPattern(pathPattern));
                FileSystemUtils.walkFileTree(context.getSafeTruffleFile(searchRoot), new SimpleFileVisitor<TruffleFile>() {
                    @Override
                    public FileVisitResult visitFile(TruffleFile file, BasicFileAttributes attrs) {
                        if (globRegex.matcher(file.getName()).matches()) {
                            tmpResult[0] = removeFile(context.getSafeTruffleFile(file.toString()), recursive, tmpResult[0]);
                            return recursive ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
                return tmpResult[0];
            } catch (IOException e) {
                return 0;
            }
        }

        private static int removeFile(TruffleFile file, boolean recursive, int resultIn) {
            int result = resultIn;
            if (file.isDirectory()) {
                if (!recursive) {
                    return result;
                } else {
                    result = recursiveDelete(file);
                }
            }
            try {
                file.delete();
            } catch (NoSuchFileException ex) {
                // Not deleting a non-existent file is not a failure.
                result = 0;
            } catch (IOException ex) {
                result = 1;
            }
            return result;
        }

        private static final char[] GLOBCHARS = new char[]{'*', '?', '['};

        private static int containsGlobChar(String pathPattern) {
            for (int i = 0; i < pathPattern.length(); i++) {
                char ch = pathPattern.charAt(i);
                for (int j = 0; j < GLOBCHARS.length; j++) {
                    if (ch == GLOBCHARS[j]) {
                        return i;
                    }
                }
            }
            return -1;
        }

        private static int recursiveDelete(TruffleFile f) {
            try (DirectoryStream<TruffleFile> stream = f.newDirectoryStream()) {
                for (TruffleFile entry : stream) {
                    if (entry.isDirectory()) {
                        recursiveDelete(entry);
                    }
                    FileSystemUtils.deleteIfExists(entry);
                }
                return 1;
            } catch (IOException ex) {
                return 0;
            }
        }
    }

    @RBuiltin(name = "dir.create", visibility = OFF, kind = INTERNAL, parameterNames = {"path", "showWarnings", "recursive", "mode"}, behavior = IO)
    public abstract static class DirCreate extends RBuiltinNode.Arg4 {

        static {
            Casts casts = new Casts(DirCreate.class);
            casts.arg("path").mustBe(stringValue()).asStringVector().mustBe(size(1)).findFirst();
            casts.arg("showWarnings").asLogicalVector().findFirst().map(toBoolean());
            casts.arg("recursive").asLogicalVector().findFirst().map(toBoolean());
            casts.arg("mode").asIntegerVector().findFirst().mapIf(intNA(), constant(0777));
        }

        @Specialization
        @TruffleBoundary
        protected byte dirCreate(String pathIn, boolean showWarnings, boolean recursive, int octMode,
                        @CachedContext(TruffleRLanguage.class) TruffleLanguage.ContextReference<RContext> ctxRef) {
            boolean ok;
            if (RRuntime.isNA(pathIn)) {
                ok = false;
            } else {
                ok = true;
                String path = pathIn;
                TruffleFile newDir = ctxRef.get().getSafeTruffleFile(path);
                if (newDir.exists()) {
                    ok = false;
                    if (showWarnings) {
                        warning(Message.ALREADY_EXISTS, newDir);
                    }
                } else {
                    if (!recursive) {
                        TruffleFile par = newDir.getParent();
                        if (par != null && !par.exists()) {
                            ok = false;
                            if (showWarnings) {
                                warning(Message.DIR_CANNOT_CREATE_NO_SUCH, newDir);
                            }
                        }
                    } else {
                        ok = mkparentdirs(newDir.getAbsoluteFile().getParent(), showWarnings, octMode);
                    }
                    if (ok) {
                        ok = mkdir(newDir, showWarnings, octMode);
                    }
                }
            }
            return RRuntime.asLogical(ok);
        }

        private boolean mkparentdirs(TruffleFile file, boolean showWarnings, int mode) {
            if (file.isDirectory()) {
                return true;
            }
            if (file.exists()) {
                return false;
            }
            if (mkparentdirs(file.getParent(), showWarnings, mode)) {
                return mkdir(file.getAbsoluteFile(), showWarnings, mode);
            } else {
                return false;
            }
        }

        private boolean mkdir(TruffleFile dir, boolean showWarnings, int mode) {
            try {
                FileSystemUtils.mkdir(dir, mode);
                return true;
            } catch (IOException ex) {
                if (showWarnings) {
                    warning(RError.Message.DIR_CANNOT_CREATE, dir);
                }
                return false;
            }
        }
    }

    @RBuiltin(name = "dir.exists", kind = INTERNAL, parameterNames = "paths", behavior = IO)
    public abstract static class DirExists extends RBuiltinNode.Arg1 {

        static {
            Casts casts = new Casts(DirExists.class);
            casts.arg("paths").mustBe(stringValue()).asStringVector();
        }

        @Specialization
        @TruffleBoundary
        protected RLogicalVector dirExists(RStringVector pathVec,
                        @CachedContext(TruffleRLanguage.class) TruffleLanguage.ContextReference<RContext> ctxRef) {
            byte[] data = new byte[pathVec.getLength()];
            for (int i = 0; i < data.length; i++) {
                String pathString = pathVec.getDataAt(i);
                TruffleFile path = ctxRef.get().getSafeTruffleFile(pathString);
                data[i] = RRuntime.asLogical(path.isDirectory());
            }
            return RDataFactory.createLogicalVector(data, RDataFactory.COMPLETE_VECTOR);
        }
    }
}
