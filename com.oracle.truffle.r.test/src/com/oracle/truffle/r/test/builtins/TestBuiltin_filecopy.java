/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestBuiltin_filecopy extends TestBase {

    @Test
    public void testfilecopy() {
        // Copy file into existing destination dir
        assertEval(Ignored.NewRVersionMigration, "baseD<-paste0(tempdir(), '/file.copy.test'); nameF<-'/srcFile.txt'; srcF<-paste0(baseD, nameF); dstD<-paste0(baseD, '/dst'); dstF<-paste0(dstD, nameF); " +
                        "tryCatch({dir.create(dstD, rec=TRUE); cat('Hello\\n', file=srcF); print(file.copy(from=srcF, to=dstD)); print(readLines(dstF)) }, finally=unlink(baseD, recursive=TRUE))");
        // Copy file into destination dir where target file exists (overwrite=FALSE => original file
        // retained)
        assertEval(Ignored.NewRVersionMigration, "baseD<-paste0(tempdir(), '/file.copy.test'); nameF<-'/srcFile.txt'; srcF<-paste0(baseD, nameF); dstD<-paste0(baseD, '/dst'); dstF<-paste0(dstD, nameF); " +
                        "tryCatch({dir.create(dstD, rec=TRUE); cat('New\\n', file=srcF); cat('Old\\n', file=dstF); print(file.copy(from=srcF, to=dstD)); print(readLines(dstF)) }, finally=unlink(baseD, recursive=TRUE))");
        // Copy file into destination dir where target file exists (overwrite=TRUE => original file
        // overwritten)
        assertEval(Ignored.NewRVersionMigration, "baseD<-paste0(tempdir(), '/file.copy.test'); nameF<-'/srcFile.txt'; srcF<-paste0(baseD, nameF); dstD<-paste0(baseD, '/dst'); dstF<-paste0(dstD, nameF); " +
                        "tryCatch({dir.create(dstD, rec=TRUE); cat('New\\n', file=srcF); cat('Old\\n', file=dstF); print(file.copy(from=srcF, to=dstD, overwrite=TRUE)); print(readLines(dstF)) }, finally=unlink(baseD, recursive=TRUE))");
        // Copy src dir with a file into existing destination dir - no recursive flag
        assertEval(Ignored.NewRVersionMigration, "baseD<-paste0(tempdir(), '/file.copy.test'); nameF<-'/srcFile.txt'; srcD<-paste0(baseD, '/src'); srcF<-paste0(srcD, nameF); dstD<-paste0(baseD, '/dst'); dstF<-paste0(dstD, nameF); " +
                        "tryCatch({dir.create(dstD, rec=TRUE); dir.create(srcD, rec=TRUE); cat('Hello\\n', file=srcF); print(file.copy(from=srcD, to=dstD)); print(file.exists(dstF)) }, finally=unlink(baseD, recursive=TRUE))");
        // Copy src dir with a file into existing destination dir - recursive flag
        assertEval(Ignored.NewRVersionMigration, "baseD<-paste0(tempdir(), '/file.copy.test'); nameF<-'/srcFile.txt'; srcD<-paste0(baseD, '/src'); srcF<-paste0(srcD, nameF); dstD<-paste0(baseD, '/dst'); dstSrcD<-paste0(dstD, '/src'); dstSrcF<-paste0(dstSrcD, nameF); " +
                        "tryCatch({dir.create(dstD, rec=TRUE); dir.create(srcD, rec=TRUE); cat('Hello\\n', file=srcF); print(file.copy(from=srcD, to=dstD, rec=TRUE)); print(file.exists(dstSrcD)); print(readLines(dstSrcF)); }, finally=unlink(baseD, recursive=TRUE))");
        // Copy src dir with a file into destination dir - recursive flag and no overwrite flag
        assertEval(Ignored.NewRVersionMigration, "baseD<-paste0(tempdir(), '/file.copy.test'); nameF<-'/srcFile.txt'; srcD<-paste0(baseD, '/src'); srcF<-paste0(srcD, nameF); dstD<-paste0(baseD, '/dst'); dstF<-paste0(dstD, '/src', nameF); " +
                        "tryCatch({dir.create(paste0(dstD, '/src'), rec=TRUE); dir.create(srcD, rec=TRUE); cat('New\\n', file=srcF); cat('Old\\n', file=dstF); print(file.copy(from=srcD, to=dstD, recursive=TRUE, overwrite=FALSE)); print(readLines(dstF)); }, finally=unlink(baseD, recursive=TRUE))");
        // Copy src dir with a file into destination dir - recursive flag and no overwrite flag; one
        // file exists at the destination another not
        assertEval(Ignored.NewRVersionMigration, "baseD<-paste0(tempdir(), '/file.copy.test'); nameF<-'/srcFile.txt'; srcD<-paste0(baseD, '/src'); srcF<-paste0(srcD, nameF); srcF2<-paste0(srcD, nameF, '2'); dstD<-paste0(baseD, '/dst'); dstF<-paste0(dstD, '/src', nameF); dstF2<-paste0(dstD, '/src', nameF, '2');" +
                        "tryCatch({dir.create(paste0(dstD, '/src'), rec=TRUE); dir.create(srcD, rec=TRUE); cat('New\\n', file=srcF); cat('New2\\n', file=srcF2); cat('Old\\n', file=dstF); print(file.copy(from=srcD, to=dstD, recursive=TRUE, overwrite=FALSE)); print(readLines(dstF)); print(readLines(dstF2)) }, finally=unlink(baseD, recursive=TRUE))");
        // Copy src dir with a file into destination dir (overwrite=FALSE => original file retained)
        assertEval(Ignored.NewRVersionMigration, "baseD<-paste0(tempdir(), '/file.copy.test'); nameF<-'/srcFile.txt'; srcD<-paste0(baseD, '/src'); srcF<-paste0(srcD, nameF); dstD<-paste0(baseD, '/dst'); dstF<-paste0(dstD, '/src', nameF); " +
                        "tryCatch({dir.create(paste0(dstD, '/src'), rec=TRUE); dir.create(srcD, rec=TRUE); cat('New\\n', file=srcF); cat('Old\\n', file=dstF); print(file.copy(from=srcD, to=dstD, recursive=TRUE, overwrite=TRUE)); print(readLines(dstF)) }, finally=unlink(baseD, recursive=TRUE))");
        // Copy src dir with a file into destination dir (overwrite=TRUE => original file
        // overwritten)
    }

}
