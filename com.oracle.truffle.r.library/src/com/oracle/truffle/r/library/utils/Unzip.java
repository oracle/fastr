/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.library.utils;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.logicalValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.singleElement;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

public abstract class Unzip extends RExternalBuiltinNode.Arg7 {

    static {
        Casts casts = new Casts(Unzip.class);
        casts.arg(0, "zipfile").mustBe(stringValue()).asStringVector().mustBe(singleElement()).findFirst();
        casts.arg(1, "files").allowNull().mustBe(stringValue()).asStringVector();
        casts.arg(2, "exdir").mustBe(stringValue()).asStringVector().mustBe(singleElement()).findFirst();
        casts.arg(3, "list").mustBe(logicalValue()).asLogicalVector().mustBe(singleElement()).findFirst().map(toBoolean());
        casts.arg(4, "overwrite").mustBe(logicalValue()).asLogicalVector().mustBe(singleElement()).findFirst().map(toBoolean());
        casts.arg(5, "junkpaths").mustBe(logicalValue()).asLogicalVector().mustBe(singleElement()).findFirst().map(toBoolean());
        casts.arg(6, "setTimes").mustBe(logicalValue()).asLogicalVector().mustBe(singleElement()).findFirst().map(toBoolean());
    }

    @Specialization
    @TruffleBoundary
    protected Object unzip(String zipfile, @SuppressWarnings("unused") RNull files, String exdir, boolean list, boolean overwrite, boolean junkpaths, boolean setTimes) {
        return unzip(zipfile, (RAbstractStringVector) null, exdir, list, overwrite, junkpaths, setTimes);
    }

    @Override
    protected RBaseNode getErrorContext() {
        return RError.SHOW_CALLER;
    }

    @Specialization
    @TruffleBoundary
    protected Object unzip(String zipfile, RAbstractStringVector files, String exdir, boolean list, boolean overwrite, boolean junkpaths, boolean setTimes) {
        if (list) {
            return list(zipfile);
        }
        Predicate<String> filter;
        boolean[] found;
        if (files == null) {
            found = null;
            filter = x -> true;
        } else {
            found = new boolean[files.getLength()];
            filter = x -> {
                for (int i = 0; i < files.getLength(); i++) {
                    if (x.equals(files.getDataAt(i))) {
                        found[i] = true;
                        return true;
                    }
                }
                return false;
            };
        }

        File targetDir = new File(exdir);
        if (!targetDir.exists() || !targetDir.isDirectory()) {
            throw error(Message.GENERIC, "invalid target directory");
        }
        try (ZipInputStream stream = new ZipInputStream(new FileInputStream(Utils.tildeExpand(zipfile)))) {
            ZipEntry entry;
            ArrayList<String> extracted = new ArrayList<>();
            byte[] buffer = new byte[2048];
            while ((entry = stream.getNextEntry()) != null) {
                if (filter.test(entry.getName())) {
                    File target = new File(targetDir, junkpaths ? new File(entry.getName()).getName() : entry.getName());
                    if (!target.exists() || overwrite) {
                        try (FileOutputStream output = new FileOutputStream(target)) {
                            extracted.add(target.getPath());
                            int length;
                            while ((length = stream.read(buffer)) > 0) {
                                output.write(buffer, 0, length);
                            }
                        }
                        if (setTimes) {
                            target.setLastModified(entry.getTime());
                        }
                    }
                }
            }
            if (files != null) {
                for (int i = 0; i < found.length; i++) {
                    if (!found[i]) {
                        warning(Message.FILE_NOT_FOUND_IN_ZIP);
                        break;
                    }
                }
            }
            RIntVector result = RDataFactory.createIntVector(new int[]{0}, true);
            result.setAttr("extracted", RDataFactory.createStringVector(extracted.toArray(new String[0]), true));
            return result;
        } catch (IOException e) {
            throw error(Message.GENERIC, "error while extracting zip: " + e.getMessage());
        }
    }

    @SuppressWarnings("deprecation")
    private Object list(String zipfile) {
        try (ZipInputStream stream = new ZipInputStream(new FileInputStream(Utils.tildeExpand(zipfile)))) {
            ArrayList<ZipEntry> entryList = new ArrayList<>();
            ZipEntry entry;
            while ((entry = stream.getNextEntry()) != null) {
                entryList.add(entry);
            }
            String[] names = new String[entryList.size()];
            double[] sizes = new double[entryList.size()];
            String[] dates = new String[entryList.size()];
            for (int i = 0; i < entryList.size(); i++) {
                entry = entryList.get(i);
                names[i] = entry.getName();
                sizes[i] = entry.getSize();
                // rounding up to minutes
                Date date = new Date(entry.getTime() + (30 * 1000));
                dates[i] = String.format("%04d-%02d-%02d %02d:%02d", date.getYear() + 1900, date.getMonth() + 1, date.getDate(), date.getHours(), date.getMinutes());
            }
            return RDataFactory.createList(new Object[]{RDataFactory.createStringVector(names, true), RDataFactory.createDoubleVector(sizes, true), RDataFactory.createStringVector(dates, true)});
        } catch (IOException e) {
            throw error(Message.GENERIC, "error while extracting zip: " + e.getMessage());
        }
    }
}
