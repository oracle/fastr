/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2015, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.tools;

import java.io.*;

import com.oracle.truffle.api.CompilerDirectives.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

public class ToolsText {
    public static RStringVector doTabExpand(RStringVector strings, RIntVector starts) {
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

    @TruffleBoundary
    public static RLogicalVector filesAppendLF(String file1, RStringVector file2Vec) {
        int n2 = file2Vec.getLength();
        byte[] data = new byte[n2];
        if (!RRuntime.isNA(file1)) {
            try (BufferedWriter out = new BufferedWriter(new FileWriter(file1, true))) {
                for (int i = 0; i < file2Vec.getLength(); i++) {
                    String path2 = file2Vec.getDataAt(i);
                    if (RRuntime.isNA(path2)) {
                        continue;
                    }
                    File path2File = new File(path2);
                    if (!(path2File.exists() && path2File.canRead())) {
                        continue;
                    }
                    char[] path2Data = new char[(int) path2File.length()];
                    try (BufferedReader in = new BufferedReader(new FileReader(path2File))) {
                        out.write("#line 1 \"" + path2 + "\"\n");
                        in.read(path2Data);
                        out.write(path2Data);
                        if (!(path2Data.length > 0 && path2Data[path2Data.length - 1] == '\n')) {
                            out.write('\n');
                        }
                        data[i] = RRuntime.LOGICAL_TRUE;
                    } catch (IOException ex) {
                        RError.warning(RError.Message.GENERIC, "write error during file append");
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
