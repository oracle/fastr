/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2015, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.runtime;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributes;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;

/**
 * Utilities for handling R srcref attributes, in particular conversion from {@link Source},
 * {@link SourceSection} instances.
 *
 * {@code srcfile} corresponds loosely to a {@link Source} and a {@code srcref} to a
 * {@link SourceSection}.
 *
 * GnuR creates a significant amount of detail in the parser that is captured in a srcref, but the
 * FastR parser does not match it in detail.
 *
 *
 * See https://stat.ethz.ch/R-manual/R-devel/library/base/html/srcfile.html.
 */
public class RSrcref {
    public enum SrcrefFields {
        Enc,
        encoding,
        timestamp,
        filename,
        isFile,
        wd;
    }

    private static final RStringVector SRCREF_ATTR = RDataFactory.createStringVectorFromScalar(RRuntime.R_SRCREF);
    private static final RStringVector SRCFILE_ATTR = RDataFactory.createStringVectorFromScalar(RRuntime.R_SRCFILE);

    /**
     * Internal version of srcfile(path).
     */
    public static REnvironment createSrcfile(String path) {
        return createSrcfile(FileSystems.getDefault().getPath(Utils.tildeExpand(path)));
    }

    @TruffleBoundary
    private static REnvironment createSrcfile(Path path) {
        // A srcref is an environment
        double mtime;
        try {
            PosixFileAttributes pfa = Files.readAttributes(path, PosixFileAttributes.class);
            mtime = pfa.lastModifiedTime().toMillis();
        } catch (IOException ex) {
            mtime = RRuntime.DOUBLE_NA;
        }
        REnvironment env = RDataFactory.createNewEnv("");
        env.safePut(SrcrefFields.Enc.name(), "unknown");
        env.safePut(SrcrefFields.encoding.name(), "native.enc");
        env.safePut(SrcrefFields.timestamp.name(), mtime);
        env.safePut(SrcrefFields.filename.name(), path.toString());
        env.safePut(SrcrefFields.isFile.name(), RRuntime.LOGICAL_TRUE);
        env.safePut(SrcrefFields.wd.name(), RFFIFactory.getRFFI().getBaseRFFI().getwd());
        env.setClassAttr(SRCFILE_ATTR);
        return env;
    }

    /**
     * Creates an {@code lloc} structure from a {@link SourceSection} and associated {@code srcfile}
     * . assert: srcfile was created from the {@link Source} associated with {@code ss} or is
     * {@code null} in which case it will be created from {@code ss}.
     */
    public static RIntVector createLloc(SourceSection ss, String path) {
        return createLloc(ss, createSrcfile(path));
    }

    @TruffleBoundary
    public static RIntVector createLloc(SourceSection ss, REnvironment srcfile) {
        int[] llocData = new int[8];
        int startLine = ss.getStartLine();
        int startColumn = ss.getStartColumn();
        int lastLine = ss.getEndLine();
        int lastColumn = ss.getEndColumn();
        // no multi-byte support, so byte==char
        llocData[0] = startLine;
        llocData[1] = startColumn;
        llocData[2] = lastLine;
        llocData[3] = lastColumn;
        llocData[4] = startColumn;
        llocData[5] = lastColumn;
        llocData[6] = startLine;
        llocData[7] = lastLine;
        RIntVector lloc = RDataFactory.createIntVector(llocData, RDataFactory.COMPLETE_VECTOR);
        lloc.setClassAttr(SRCREF_ATTR);
        lloc.setAttr(RRuntime.R_SRCFILE, srcfile);
        return lloc;
    }

}
