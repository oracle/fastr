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
package com.oracle.truffle.r.runtime;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.oracle.truffle.r.runtime.ResourceHandlerFactory.Handler;

/**
 * Default implementation uses the default mechanism in {@code java.lang.Class}.
 */
class DefaultResourceHandlerFactory extends ResourceHandlerFactory implements Handler {

    @Override
    public URL getResource(Class<?> accessor, String name) {
        return accessor.getResource(name);
    }

    @Override
    public InputStream getResourceAsStream(Class<?> accessor, String name) {
        return accessor.getResourceAsStream(name);
    }

    @Override
    protected Handler newHandler() {
        return this;
    }

    @Override
    public String[] getRFiles(Class<?> accessor, String pkgName) {
        CodeSource source = accessor.getProtectionDomain().getCodeSource();
        ArrayList<String> list = new ArrayList<>();
        try {
            URL url = source.getLocation();
            Path sourcePath = Paths.get(url.toURI().getPath());
            File sourceFile = sourcePath.toFile();
            if (sourceFile.isDirectory()) {
                InputStream is = accessor.getResourceAsStream(pkgName + "/R");
                if (is != null) {
                    try (BufferedReader r = new BufferedReader(new InputStreamReader(is))) {
                        String line;
                        while ((line = r.readLine()) != null) {
                            if (line.endsWith(".r") || line.endsWith(".R")) {
                                final String rResource = pkgName + "/R/" + line.trim();
                                list.add(Utils.getResourceAsString(accessor, rResource, true));
                            }
                        }
                    }
                }
            } else {
                JarFile fastrJar = new JarFile(sourceFile);
                Enumeration<JarEntry> iter = fastrJar.entries();
                while (iter.hasMoreElements()) {
                    JarEntry entry = iter.nextElement();
                    String name = entry.getName();
                    if (name.endsWith(".R") || name.endsWith(".r")) {
                        Path p = Paths.get(name);
                        String entryPkg = p.getName(p.getNameCount() - 3).getFileName().toString();
                        String entryParent = p.getName(p.getNameCount() - 2).getFileName().toString();
                        if (entryParent.equals("R") && entryPkg.equals(pkgName)) {
                            int size = (int) entry.getSize();
                            byte[] buf = new byte[size];
                            InputStream is = fastrJar.getInputStream(entry);
                            int totalRead = 0;
                            int n;
                            while ((n = is.read(buf, totalRead, buf.length - totalRead)) > 0) {
                                totalRead += n;
                            }
                            list.add(new String(buf));
                        }
                    }
                }
            }
            String[] result = new String[list.size()];
            list.toArray(result);
            return result;
        } catch (Exception ex) {
            Utils.rSuicide(ex.getMessage());
            return null;
        }
    }

}
