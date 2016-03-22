/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2015, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.tools;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

public class ToolsText {

    public abstract static class DoTabExpand extends RExternalBuiltinNode.Arg2 {

        @Specialization
        @TruffleBoundary
        protected Object doTabExpand(RAbstractStringVector strings, RAbstractIntVector starts) {
            String[] data = new String[strings.getLength()];
            for (int i = 0; i < data.length; i++) {
                String input = strings.getDataAt(i);
                if (input.indexOf('\t') >= 0) {
                    StringBuffer sb = new StringBuffer();
                    int b = 0;
                    int start = starts.getDataAt(i % data.length);
                    for (int sx = 0; sx < input.length(); sx++) {
                        char ch = input.charAt(sx);
                        if (ch == '\n') {
                            start = -b - 1;
                        }
                        if (ch == '\t') {
                            do {
                                sb.append(' ');
                                b++;
                            } while (((b + start) & 7) != 0);
                        } else {
                            sb.append(ch);
                            b++;
                        }
                    }
                    data[i] = sb.toString();
                } else {
                    data[i] = input;
                }
            }
            return RDataFactory.createStringVector(data, RDataFactory.COMPLETE_VECTOR);
        }
    }

    public abstract static class CodeFilesAppend extends RExternalBuiltinNode.Arg2 {

        @Specialization
        @TruffleBoundary
        protected Object codeFilesAppend(RAbstractStringVector file1Vector, RAbstractStringVector file2) {
            if (file1Vector.getLength() != 1) {
                throw RError.error(this, RError.Message.INVALID_ARGUMENT, "file1");
            }
            if (file2.getLength() < 1) {
                return RDataFactory.createEmptyLogicalVector();
            }
            String file1 = file1Vector.getDataAt(0);
            int n2 = file2.getLength();
            byte[] data = new byte[n2];
            if (!RRuntime.isNA(file1)) {
                try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file1, true))) {
                    for (int i = 0; i < file2.getLength(); i++) {
                        String file2e = file2.getDataAt(i);
                        if (RRuntime.isNA(file2e)) {
                            continue;
                        }
                        Path path2 = FileSystems.getDefault().getPath(file2e);
                        try {
                            byte[] path2Data = Files.readAllBytes(path2);
                            byte[] header = ("#line 1 \"" + file2e + "\"\n").getBytes();
                            out.write(header);
                            out.write(path2Data);
                            if (!(path2Data.length > 0 && path2Data[path2Data.length - 1] == '\n')) {
                                out.write('\n');
                            }
                            data[i] = RRuntime.LOGICAL_TRUE;
                        } catch (IOException ex) {
                            RError.warning(this, RError.Message.GENERIC, "IO error during file append");
                            // shouldn't happen, just continue with false result
                        }

                    }
                } catch (IOException ex) {
                    // just return logical false
                }
            }
            return RDataFactory.createLogicalVector(data, RDataFactory.COMPLETE_VECTOR);
        }
    }
}
