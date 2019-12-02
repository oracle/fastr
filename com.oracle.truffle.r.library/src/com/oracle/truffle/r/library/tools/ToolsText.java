/*
 * Copyright (c) 1995-2015, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates
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
package com.oracle.truffle.r.library.tools;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.integerValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.singleElement;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.typeName;

import java.io.BufferedOutputStream;
import java.io.IOException;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleFile;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.TruffleRLanguage;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import java.nio.file.StandardOpenOption;

public class ToolsText {

    public abstract static class DoTabExpand extends RExternalBuiltinNode.Arg2 {

        static {
            Casts casts = new Casts(DoTabExpand.class);
            casts.arg(0, "strings").defaultError(RError.Message.MACRO_CAN_BE_APPLIED_TO, "STRING_ELT()", "character vector", typeName()).mustBe(stringValue());
            casts.arg(1, "starts").defaultError(RError.Message.MACRO_CAN_BE_APPLIED_TO, "INTEGER()", "integer", typeName()).mustBe(integerValue()).asIntegerVector();
        }

        @Specialization
        @TruffleBoundary
        protected Object doTabExpand(RAbstractStringVector strings, RAbstractIntVector starts) {
            String[] data = new String[strings.getLength()];
            for (int i = 0; i < data.length; i++) {
                String input = strings.getDataAt(i);
                if (input.indexOf('\t') >= 0) {
                    StringBuilder sb = new StringBuilder();
                    int b = 0;
                    int start = starts.getDataAt(i % starts.getLength());
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

        static {
            Casts casts = new Casts(CodeFilesAppend.class);
            casts.arg(0, "file1").mustBe(stringValue()).asStringVector().mustBe(singleElement()).findFirst();
            casts.arg(1, "file2").mustBe(stringValue()).asStringVector();
        }

        @Specialization
        @TruffleBoundary
        protected Object codeFilesAppend(String file1, RAbstractStringVector file2,
                        @CachedContext(TruffleRLanguage.class) TruffleLanguage.ContextReference<RContext> ctxRef) {
            if (file2.getLength() < 1) {
                return RDataFactory.createEmptyLogicalVector();
            }
            int n2 = file2.getLength();
            byte[] data = new byte[n2];
            if (!RRuntime.isNA(file1)) {
                TruffleFile tFile1 = ctxRef.get().getSafeTruffleFile(file1);
                try (BufferedOutputStream out = new BufferedOutputStream(tFile1.newOutputStream(StandardOpenOption.APPEND))) {
                    for (int i = 0; i < file2.getLength(); i++) {
                        String file2e = file2.getDataAt(i);
                        if (RRuntime.isNA(file2e)) {
                            continue;
                        }
                        TruffleFile tFile2 = ctxRef.get().getSafeTruffleFile(file2e);
                        try {
                            byte[] path2Data = tFile2.readAllBytes();
                            byte[] header = ("#line 1 \"" + file2e + "\"\n").getBytes();
                            out.write(header);
                            out.write(path2Data);
                            if (!(path2Data.length > 0 && path2Data[path2Data.length - 1] == '\n')) {
                                out.write('\n');
                            }
                            data[i] = RRuntime.LOGICAL_TRUE;
                        } catch (IOException ex) {
                            warning(RError.Message.GENERIC, "IO error during file append");
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
