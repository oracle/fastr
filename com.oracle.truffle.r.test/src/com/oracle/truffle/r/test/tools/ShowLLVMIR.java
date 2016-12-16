/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test.tools;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import com.oracle.truffle.r.runtime.ProcessOutputManager;
import com.oracle.truffle.r.runtime.ffi.truffle.LLVM_IR;

public class ShowLLVMIR {
    public static void main(String[] args) {
        String objPath = null;
        String llpart = null;
        boolean list = false;
        boolean xxports = false;
        boolean dis = false;
        int i = 0;
        while (i < args.length) {
            String arg = args[i];
            switch (arg) {
                case "-o":
                    i++;
                    objPath = args[i];
                    break;
                case "-ll":
                case "--module":
                    i++;
                    llpart = args[i];
                    break;
                case "--names":
                case "--list":
                    list = true;
                    break;
                case "--xxports":
                    xxports = true;
                    break;
                case "--dis":
                    dis = true;
                    break;

            }
            i++;
        }
        if (objPath == null) {
            usage();
        }
        try {
            LLVM_IR[] irs = LLVM_IR.getLLVMIR(objPath);
            if (irs == null) {
                System.out.printf("no llvm ir in %s\n", objPath);
                System.exit(1);
            }
            for (LLVM_IR ir : irs) {
                if (list) {
                    System.out.println(ir.name);
                } else {
                    if (llpart == null || ir.name.equals(llpart)) {
                        System.out.printf("Module: %s%n", ir.name);
                        if (xxports) {
                            System.out.println("Exports");
                            System.out.println("=======");
                            for (String export : ir.exports) {
                                System.out.println(export);
                            }
                            System.out.println("Imports");
                            System.out.println("=======");
                            for (String importx : ir.imports) {
                                System.out.println(importx);
                            }
                        }
                        if (dis) {
                            String text = null;
                            if (ir instanceof LLVM_IR.Binary) {
                                LLVM_IR.Binary irb = (LLVM_IR.Binary) ir;
                                try {
                                    ProcessBuilder pb = new ProcessBuilder("llvm-dis");
                                    Process p = pb.start();
                                    InputStream os = p.getInputStream();
                                    OutputStream is = p.getOutputStream();
                                    ProcessOutputManager.OutputThreadVariable readThread = new ProcessOutputManager.OutputThreadVariable("llvm-dis", os);
                                    readThread.start();
                                    is.write(irb.binary);
                                    is.close();
                                    @SuppressWarnings("unused")
                                    int rc = p.waitFor();
                                    text = new String(readThread.getData(), 0, readThread.getTotalRead());
                                } catch (IOException ex) {
                                    System.err.println(ex);
                                    System.exit(2);
                                }
                            } else {
                                LLVM_IR.Text tir = (LLVM_IR.Text) ir;
                                text = tir.text;
                            }
                            System.out.println(text);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println(e);
        }

    }

    private static void usage() {
        System.err.print("usage: -o objfile");
        System.exit(1);
    }

}
