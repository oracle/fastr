/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.SetClassAttributeNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.unary.CastStringNode;
import com.oracle.truffle.r.nodes.unary.CastStringNodeGen;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.RContext.ConsoleIO;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.RTypedValue;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.ffi.BaseRFFI;

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
        protected Object fileAccess(RAbstractStringVector names, int mode) {
            int[] data = new int[names.getLength()];
            for (int i = 0; i < data.length; i++) {
                File file = new File(Utils.tildeExpand(names.getDataAt(i)));
                if (file.exists()) {
                    if ((mode & EXECUTE) != 0 && !file.canExecute()) {
                        data[i] = -1;
                    }
                    if ((mode & READ) != 0 && !file.canRead()) {
                        data[i] = -1;
                    }
                    if ((mode & WRITE) != 0 && !file.canWrite()) {
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
        protected RLogicalVector doFileAppend(RAbstractStringVector file1Vec, RAbstractStringVector file2Vec) {
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
                if (file1 != RRuntime.STRING_NA) {
                    file1 = Utils.tildeExpand(file1);
                    try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file1, true))) {
                        for (int f2 = 0; f2 < len2; f2++) {
                            String file2 = file2Vec.getDataAt(f2);
                            status[f2] = RRuntime.asLogical(appendFile(out, file2));
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
                    if (file1 != RRuntime.STRING_NA) {
                        file1 = Utils.tildeExpand(file1);
                        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file1, true))) {
                            String file2 = file2A[f];
                            status[f] = RRuntime.asLogical(appendFile(out, file2));
                        } catch (IOException ex) {
                            // status is set
                        }
                    }
                }
            }
            return RDataFactory.createLogicalVector(status, RDataFactory.COMPLETE_VECTOR);
        }

        private boolean appendFile(BufferedOutputStream out, String pathArg) {
            if (pathArg == RRuntime.STRING_NA) {
                return false;
            }
            String path = Utils.tildeExpand(pathArg);
            File file = new File(path);
            if (!file.exists()) {
                // not an error (cf GnuR), just status
                return false;
            } else {
                byte[] buf = new byte[(int) file.length()];
                try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(path))) {
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
        protected Object doFileCreate(RAbstractStringVector vec, byte showWarnings) {
            byte[] status = new byte[vec.getLength()];
            for (int i = 0; i < status.length; i++) {
                String path = vec.getDataAt(i);
                if (RRuntime.isNA(path)) {
                    status[i] = RRuntime.LOGICAL_FALSE;
                } else {
                    boolean ok = true;
                    try {
                        new FileOutputStream(Utils.tildeExpand(path)).close();
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
        protected RList doFileInfo(RAbstractStringVector vec, @SuppressWarnings("unused") Boolean extraCols) {
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
            FileSystem fileSystem = FileSystems.getDefault();
            for (int i = 0; i < vecLength; i++) {
                String vecPath = vec.getDataAt(i);
                Path path = fileSystem.getPath(Utils.tildeExpand(vecPath));
                // missing defaults to NA
                if (Files.exists(path)) {
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
                        PosixFileAttributes pfa = Files.readAttributes(path, PosixFileAttributes.class);
                        size = pfa.size();
                        isdir = RRuntime.asLogical(pfa.isDirectory());
                        mtime = Utils.getTimeInSecs(pfa.lastModifiedTime());
                        ctime = Utils.getTimeInSecs(pfa.creationTime());
                        atime = Utils.getTimeInSecs(pfa.lastAccessTime());
                        uname = pfa.owner().getName();
                        grname = pfa.group().getName();
                        mode = Utils.intFilePermissions(pfa.permissions());
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
                case uname: case grname: ((String[]) data[slot])[index] = (String) value; updateComplete(slot, complete, (String) value != RRuntime.STRING_NA); return;
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
                    setClassAttrNode.execute(res, OCTMODE);
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

        protected Object doFileLink(RAbstractStringVector vecFrom, RAbstractStringVector vecTo, boolean symbolic) {
            int lenFrom = vecFrom.getLength();
            int lenTo = vecTo.getLength();
            if (lenFrom < 1) {
                throw error(RError.Message.NOTHING_TO_LINK);
            }
            if (lenTo < 1) {
                return RDataFactory.createLogicalVector(0);
            }
            int len = lenFrom > lenTo ? lenFrom : lenTo;
            FileSystem fileSystem = FileSystems.getDefault();
            byte[] status = new byte[len];
            for (int i = 0; i < len; i++) {
                String from = vecFrom.getDataAt(i % lenFrom);
                String to = vecTo.getDataAt(i % lenTo);
                if (RRuntime.isNA(from) || RRuntime.isNA(to)) {
                    status[i] = RRuntime.LOGICAL_FALSE;
                } else {
                    Path fromPath = fileSystem.getPath(Utils.tildeExpand(from));
                    Path toPath = fileSystem.getPath(Utils.tildeExpand(to));
                    status[i] = RRuntime.LOGICAL_TRUE;
                    try {
                        if (symbolic) {
                            Files.createSymbolicLink(toPath, fromPath);
                        } else {
                            Files.createLink(toPath, fromPath);
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
        protected Object doFileLink(RAbstractStringVector vecFrom, RAbstractStringVector vecTo) {
            return doFileLink(vecFrom, vecTo, false);
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
        protected Object doFileSymLink(RAbstractStringVector vecFrom, RAbstractStringVector vecTo) {
            return doFileLink(vecFrom, vecTo, true);
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
        protected Object doFileRemove(RAbstractStringVector vec) {
            byte[] status = new byte[vec.getLength()];
            for (int i = 0; i < status.length; i++) {
                String path = vec.getDataAt(i);
                if (RRuntime.isNA(path)) {
                    status[i] = RRuntime.LOGICAL_FALSE;
                } else {
                    File f = new File(Utils.tildeExpand(path));
                    boolean ok = f.delete();
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
        protected Object doFileRename(RAbstractStringVector vecFrom, RAbstractStringVector vecTo) {
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
                    boolean ok = new File(Utils.tildeExpand(from)).renameTo(new File(Utils.tildeExpand(to)));
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
        protected Object doFileExists(RAbstractStringVector vec) {
            byte[] status = new byte[vec.getLength()];
            for (int i = 0; i < status.length; i++) {
                String path = vec.getDataAt(i);
                if (RRuntime.isNA(path) || path.isEmpty()) {
                    status[i] = RRuntime.LOGICAL_FALSE;
                } else {
                    File f = new File(Utils.tildeExpand(path));
                    // TODO R's notion of exists may not match Java - check
                    status[i] = f.exists() ? RRuntime.LOGICAL_TRUE : RRuntime.LOGICAL_FALSE;
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
        protected RStringVector doListFiles(RAbstractStringVector vec, RNull patternVec, boolean allFiles, boolean fullNames, boolean recursive, boolean ignoreCase, boolean includeDirs,
                        boolean noDotDot) {
            return doListFilesBody(vec, null, allFiles, fullNames, recursive, ignoreCase, includeDirs, noDotDot);
        }

        @Specialization
        @TruffleBoundary
        protected RStringVector doListFiles(RAbstractStringVector vec, RAbstractStringVector patternVec, boolean allFiles, boolean fullNames, boolean recursive, boolean ignoreCase,
                        boolean includeDirs,
                        boolean noDotDot) {
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

            return doListFilesBody(vec, pattern, allFiles, fullNames, recursive, ignoreCase, includeDirs, noDotDot);
        }

        private RStringVector doListFilesBody(RAbstractStringVector vec, String patternString, boolean allFiles, boolean fullNames, boolean recursive,
                        boolean ignoreCaseIn, boolean includeDirsIn, boolean noDotDot) {
            boolean includeDirs = !recursive || includeDirsIn;
            @SuppressWarnings("unused")
            boolean ignoreCase = check(ignoreCaseIn, "ignoreCase");
            Pattern pattern = patternString == null ? null : Pattern.compile(patternString);
            // Curiously the result is not a vector of same length as the input,
            // as typical for R, but a single vector, which means duplicates may occur
            ArrayList<String> files = new ArrayList<>();
            for (int i = 0; i < vec.getLength(); i++) {
                String vecPathString = vec.getDataAt(i);
                String pathString = Utils.tildeExpand(vecPathString, true);
                File root = new File(pathString);
                if (!root.exists()) {
                    continue;
                }
                Path rootPath = root.toPath();
                try (Stream<Path> stream = Files.find(rootPath, recursive ? Integer.MAX_VALUE : 1, new FileMatcher(pattern, allFiles, includeDirs))) {
                    Iterator<Path> iter = stream.iterator();
                    Path vecPath = null;
                    if (!fullNames) {
                        vecPath = FileSystems.getDefault().getPath(vecPathString);
                    }
                    while (iter.hasNext()) {
                        Path file = iter.next();
                        if (Files.isSameFile(file, rootPath)) {
                            continue;
                        }
                        if (!fullNames) {
                            file = vecPath.relativize(file);
                        }
                        files.add(file.toString());
                    }
                    /*
                     * Annoyingly "." and ".." are never visited by Files.find, so we have to
                     * process them manually.
                     */
                    if (!noDotDot) {
                        if (pattern == null || pattern.matcher(DOT).find()) {
                            files.add(fullNames ? FileSystems.getDefault().getPath(vecPathString, DOT).toString() : DOT);
                        }
                        if (pattern == null || pattern.matcher(DOTDOT).find()) {
                            files.add(fullNames ? FileSystems.getDefault().getPath(vecPathString, DOTDOT).toString() : DOTDOT);
                        }
                    }
                } catch (IOException ex) {
                    // ignored
                }
            }
            if (files.size() == 0) {
                // The manual says "" but GnuR returns an empty vector
                return RDataFactory.createEmptyStringVector();
            } else {
                String[] data = new String[files.size()];
                files.toArray(data);
                Arrays.sort(data);
                return RDataFactory.createStringVector(data, RDataFactory.COMPLETE_VECTOR);
            }
        }

        private boolean check(boolean value, String argName) {
            if (value) {
                warning(RError.Message.GENERIC, "'" + argName + "'" + " is not implemented");
            }
            return value;
        }

        private static class FileMatcher implements BiPredicate<Path, BasicFileAttributes> {
            final Pattern pattern;
            final boolean includeDirs;
            final boolean allFiles;

            FileMatcher(Pattern pattern, boolean allFiles, boolean includeDirs) {
                this.allFiles = allFiles;
                this.includeDirs = includeDirs;
                this.pattern = pattern;
            }

            @Override
            public boolean test(Path path, BasicFileAttributes u) {
                if (u.isDirectory() && !includeDirs) {
                    return false;
                }
                if (!allFiles && path.getFileName().toString().charAt(0) == '.') {
                    return false;
                }
                if (pattern == null) {
                    return true;
                }
                Matcher m = pattern.matcher(path.getFileName().toString());
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
        protected RStringVector listDirs(RAbstractStringVector paths, boolean fullNames, boolean recursive) {
            ArrayList<String> dirList = new ArrayList<>();
            for (int i = 0; i < paths.getLength(); i++) {
                String vecPathString = paths.getDataAt(i);
                String pathString = Utils.tildeExpand(vecPathString, true);
                File root = new File(pathString);
                if (!root.exists()) {
                    continue;
                }
                Path path = root.toPath();
                try (Stream<Path> stream = Files.find(path, recursive ? Integer.MAX_VALUE : 1, new FileMatcher())) {
                    Iterator<Path> iter = stream.iterator();
                    Path vecPath = null;
                    if (!fullNames) {
                        FileSystem fileSystem = FileSystems.getDefault();
                        vecPath = fileSystem.getPath(vecPathString);
                    }
                    while (iter.hasNext()) {
                        Path dir = iter.next();
                        if (!fullNames) {
                            dir = vecPath.relativize(dir);
                        }
                        dirList.add(dir.toString());
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

        private static class FileMatcher implements BiPredicate<Path, BasicFileAttributes> {
            @Override
            public boolean test(Path path, BasicFileAttributes u) {
                boolean result = u.isDirectory();
                return result;
            }
        }
    }

    // TODO handle the general case, which is similar to paste
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
            Object[] argValues = args.getDataWithoutCopying();
            int resultLength = 0;
            for (int i = 0; i < argValues.length; i++) {
                Object elem = argValues[i];
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
            String[][] inputs = new String[argValues.length][];
            for (int i = 0; i < argValues.length; i++) {
                Object elem = args.getDataAt(i);
                if (elem instanceof RTypedValue && ((RTypedValue) elem).isS4()) {
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
                    inputs[i] = ((RStringVector) elem).getReadonlyData();
                } else {
                    throw error(RError.Message.NON_STRING_ARG_TO_INTERNAL_PASTE);
                }
            }
            for (int i = 0; i < resultLength; i++) {
                String path = "";
                for (int j = 0; j < inputs.length; j++) {
                    path += inputs[j][i % inputs[j].length];
                    if (j != inputs.length - 1) {
                        path += fsep;
                    }
                }
                result[i] = path;
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
        protected RLogicalVector fileCopy(RAbstractStringVector vecFrom, RAbstractStringVector vecTo, boolean overwrite, boolean recursiveA, boolean copyMode, boolean copyDate) {
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
                } else if (overwrite) {
                    copyOptions = new CopyOption[1];
                } else {
                    copyOptions = new CopyOption[0];
                }
                if (overwrite) {
                    copyOptions[0] = StandardCopyOption.REPLACE_EXISTING;
                }
                FileSystem fileSystem = FileSystems.getDefault();
                Path toDir = null;
                if (vecTo.getLength() == 1) {
                    Path vecTo0Path = fileSystem.getPath(Utils.tildeExpand(vecTo.getDataAt(0)));
                    if (Files.isDirectory(vecTo0Path)) {
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
                    String to = vecTo.getDataAt(i % lenTo);
                    Path fromPathKeepRel = fileSystem.getPath(Utils.tildeExpand(from, true));
                    if (toDir != null && !fromPathKeepRel.isAbsolute()) {
                        to = toDir.resolve(fromPathKeepRel.getFileName()).toString();
                    }
                    Path fromPath = fileSystem.getPath(Utils.tildeExpand(from));
                    Path toPath = fileSystem.getPath(Utils.tildeExpand(to));
                    status[i] = RRuntime.LOGICAL_TRUE;
                    try {
                        if (recursive && Files.isDirectory(fromPath)) {
                            // to is just one dir (checked above)
                            boolean copyError = copyDir(fromPath, toPath, copyOptions);
                            if (copyError) {
                                status[i] = RRuntime.LOGICAL_FALSE;
                            }
                        } else {
                            // copy to existing files is skipped unless overWrite
                            if (!Files.exists(toPath) || overwrite) {
                                /*
                                 * toB Be careful if toPath is a directory, if empty Java will
                                 * replace it with a plain file, otherwise the copy will fail
                                 */
                                if (Files.isDirectory(toPath)) {
                                    Path fromFileNamePath = fromPath.getFileName();
                                    toPath = toPath.resolve(fromFileNamePath);
                                }
                                Files.copy(fromPath, toPath, copyOptions);
                            }
                        }
                    } catch (UnsupportedOperationException | IOException ex) {
                        status[i] = RRuntime.LOGICAL_FALSE;
                        warning(RError.Message.FILE_CANNOT_COPY, from, to, ex.getMessage());
                    }
                }
            }
            return RDataFactory.createLogicalVector(status, RDataFactory.COMPLETE_VECTOR);
        }

        private static final class DirCopy extends SimpleFileVisitor<Path> {
            private final Path fromDir;
            private final Path toDir;
            private final CopyOption[] copyOptions;
            private boolean error;

            private DirCopy(Path fromDir, Path toDir, CopyOption[] copyOptions) {
                this.fromDir = fromDir;
                this.toDir = toDir;
                this.copyOptions = copyOptions;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                Path newDir = toDir.resolve(fromDir.relativize(fromDir));
                try {
                    Files.copy(dir, newDir, copyOptions);
                } catch (FileAlreadyExistsException x) {
                    // ok
                } catch (IOException ex) {
                    error = true;
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path newFile = toDir.resolve(fromDir.relativize(file));
                Files.copy(file, newFile, copyOptions);
                return FileVisitResult.CONTINUE;
            }
        }

        @SuppressWarnings("static-method")
        private boolean copyDir(Path fromDir, Path toDir, CopyOption[] copyOptions) throws IOException {
            DirCopy dirCopy = new DirCopy(fromDir, toDir, copyOptions);
            Files.walkFileTree(fromDir, dirCopy);
            return dirCopy.error;
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
        protected RNull show(RAbstractStringVector files, RAbstractStringVector header, RAbstractStringVector title, boolean deleteFile, @SuppressWarnings("unused") String pager) {
            ConsoleIO console = RContext.getInstance().getConsole();
            for (int i = 0; i < title.getLength(); i++) {
                console.println("==== " + title.getDataAt(i) + " ====");
            }
            for (int i = 0; i < files.getLength(); i++) {
                if (i < header.getLength() && !header.getDataAt(i).isEmpty()) {
                    console.println("== " + header.getDataAt(i) + " ==");
                }
                try {
                    Path path = Paths.get(files.getDataAt(i));
                    List<String> lines = Files.readAllLines(path);
                    for (String line : lines) {
                        console.println(line);
                    }
                    if (deleteFile) {
                        path.toFile().delete();
                    }
                } catch (IOException e) {
                    throw error(Message.GENERIC, e.getMessage());
                }
            }
            return RNull.instance;
        }
    }

    protected static RStringVector doXyzName(RAbstractStringVector vec, BiFunction<FileSystem, String, String> fun) {
        FileSystem fileSystem = FileSystems.getDefault();
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
                data[i] = fun.apply(fileSystem, name);
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
        protected RStringVector doDirName(RAbstractStringVector vec) {
            return doXyzName(vec, (fileSystem, name) -> {
                Path path = fileSystem.getPath(Utils.tildeExpand(name));
                Path parent = path.getParent();
                return parent != null ? parent.toString() : ".";
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
        protected RStringVector doDirName(RAbstractStringVector vec) {
            return doXyzName(vec, (fileSystem, name) -> {
                Path path = fileSystem.getPath(name);
                Path parent = path.getFileName();
                return parent != null ? parent.toString() : name;
            });
        }
    }

    @RBuiltin(name = "unlink", visibility = OFF, kind = INTERNAL, parameterNames = {"x", "recursive", "force"}, behavior = IO)
    public abstract static class Unlink extends RBuiltinNode.Arg3 {

        static {
            Casts casts = new Casts(Unlink.class);
            casts.arg("x").mustBe(stringValue(), RError.Message.CHAR_VEC_ARGUMENT);
            casts.arg("recursive").asLogicalVector().findFirst().mustNotBeNA().map(toBoolean());
            casts.arg("force").asLogicalVector().findFirst().mustNotBeNA().map(toBoolean());
        }

        @Specialization
        @TruffleBoundary
        protected int doUnlink(RAbstractStringVector vec, boolean recursive, @SuppressWarnings("unused") boolean force) {
            int result = 1;
            for (int i = -0; i < vec.getLength(); i++) {
                String pathPattern = Utils.tildeExpand(vec.getDataAt(i));
                if (pathPattern.length() == 0 || RRuntime.isNA(pathPattern)) {
                    continue;
                }
                int firstGlobCharIdx = containsGlobChar(pathPattern);
                if (firstGlobCharIdx >= 0) {
                    result = removeGlob(pathPattern, recursive, firstGlobCharIdx, result);
                } else {
                    result = removeFile(FileSystems.getDefault().getPath(pathPattern), recursive, result);
                }
            }
            return result;
        }

        private int removeGlob(String pathPattern, boolean recursive, int firstGlobCharIdx, int result) {
            // we take as much as we can from the pathPatter as the search root
            int lastSeparator = pathPattern.substring(0, firstGlobCharIdx).lastIndexOf(File.separatorChar);
            String searchRoot = pathPattern.substring(0, lastSeparator);
            try {
                int[] tmpResult = new int[]{result};
                PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pathPattern);
                Files.walkFileTree(Paths.get(searchRoot), new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        if (matcher.matches(file)) {
                            tmpResult[0] = removeFile(file, recursive, tmpResult[0]);
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

        private int removeFile(Path path, boolean recursive, int resultIn) {
            int result = resultIn;
            if (Files.isDirectory(path)) {
                if (!recursive) {
                    return result;
                } else {
                    result = recursiveDelete(path);
                }
            }
            try {
                Files.deleteIfExists(path);
            } catch (IOException ex) {
                result = 0;
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

        private int recursiveDelete(Path path) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                for (Path entry : stream) {
                    if (Files.isDirectory(entry)) {
                        recursiveDelete(entry);
                    }
                    Files.deleteIfExists(entry);
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
                        @Cached("create()") BaseRFFI.MkdirNode mkdirNode) {
            boolean ok;
            if (RRuntime.isNA(pathIn)) {
                ok = false;
            } else {
                ok = true;
                String path = Utils.tildeExpand(pathIn);
                if (recursive) {
                    ok = mkparentdirs(mkdirNode, new File(path).getAbsoluteFile().getParentFile(), showWarnings, octMode);
                }
                if (ok) {
                    ok = mkdir(mkdirNode, path, showWarnings, octMode);
                }
            }
            return RRuntime.asLogical(ok);
        }

        private boolean mkparentdirs(BaseRFFI.MkdirNode mkdirNode, File file, boolean showWarnings, int mode) {
            if (file.isDirectory()) {
                return true;
            }
            if (file.exists()) {
                return false;
            }
            if (mkparentdirs(mkdirNode, file.getParentFile(), showWarnings, mode)) {
                return mkdir(mkdirNode, file.getAbsolutePath(), showWarnings, mode);
            } else {
                return false;
            }
        }

        private boolean mkdir(BaseRFFI.MkdirNode mkdirNode, String path, boolean showWarnings, int mode) {
            try {
                mkdirNode.execute(path, mode);
                return true;
            } catch (IOException ex) {
                if (showWarnings) {
                    warning(RError.Message.DIR_CANNOT_CREATE, path);
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
        protected RLogicalVector dirExists(RAbstractStringVector pathVec) {
            byte[] data = new byte[pathVec.getLength()];
            for (int i = 0; i < data.length; i++) {
                String pathString = Utils.tildeExpand(pathVec.getDataAt(i));
                Path path = FileSystems.getDefault().getPath(pathString);
                data[i] = RRuntime.asLogical(Files.exists(path) && Files.isDirectory(path));
            }
            return RDataFactory.createLogicalVector(data, RDataFactory.COMPLETE_VECTOR);
        }
    }
}
