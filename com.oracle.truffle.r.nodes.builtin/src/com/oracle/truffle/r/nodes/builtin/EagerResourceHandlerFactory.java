/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.ResourceHandlerFactory;
import com.oracle.truffle.r.runtime.ResourceHandlerFactory.Handler;

/**
 * Implementation of {@link ResourceHandlerFactory} that pre-loads all the resources in its static
 * constructor.
 */
public class EagerResourceHandlerFactory extends ResourceHandlerFactory implements Handler {
    private static class FileInfo {
        private final String name;
        private final URL url;
        private byte[] data;

        FileInfo(String name, URL url, byte[] data) {
            this.name = name;
            this.url = url;
            this.data = data;
        }

        @Override
        public String toString() {
            return "name: " + name + ", url: " + url.toString();
        }
    }

    private static HashMap<String, FileInfo> files = new HashMap<>();

    static {
        gatherResources();
    }

    @Override
    public URL getResource(Class<?> accessor, String name) {
        return files.get(name).url;
    }

    @Override
    public InputStream getResourceAsStream(Class<?> accessor, String name) {
        // actual resource
        Path fileNamePath = Paths.get(name).getFileName();
        assert fileNamePath != null;
        String fileName = fileNamePath.toString();
        FileInfo fileInfo = files.get(fileName);
        if (fileInfo == null || fileInfo.data == null) {
            return null;
        } else {
            return new ByteArrayInputStream(fileInfo.data);
        }
    }

    @Override
    protected Handler newHandler() {
        return this;
    }

    private static void gatherResources() {
        CodeSource source = RBuiltinPackage.class.getProtectionDomain().getCodeSource();
        try {
            URL jarURL = source.getLocation();
            JarFile fastrJar = new JarFile(new File(jarURL.toURI()));
            Enumeration<JarEntry> iter = fastrJar.entries();
            while (iter.hasMoreElements()) {
                JarEntry entry = iter.nextElement();
                String name = entry.getName();
                if (name.endsWith(".R") || name.endsWith("CONTRIBUTORS") || name.endsWith(".Rd")) {
                    int size = (int) entry.getSize();
                    byte[] buf = new byte[size];
                    InputStream is = fastrJar.getInputStream(entry);
                    int totalRead = 0;
                    int n;
                    while ((n = is.read(buf, totalRead, buf.length - totalRead)) > 0) {
                        totalRead += n;
                    }
                    // using a proper jar URL causes build image problems
                    // and no-one really cares what the URL is - we have the data already
                    Path fileNamePath = Paths.get(name).getFileName();
                    assert fileNamePath != null;
                    String fileName = fileNamePath.toString();
                    files.put(fileName, new FileInfo(fileName, new URL("file://" + name), buf));
                }
            }
        } catch (Exception ex) {
            RInternalError.shouldNotReachHere("error locating resources: " + ex.getMessage());
        }
    }

    @Override
    public Map<String, String> getRFiles(Class<?> accessor, String pkgName) {
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, FileInfo> entry : files.entrySet()) {
            String url = entry.getValue().url.toString();
            if (url.endsWith(".R") && url.contains(pkgName + "/R")) {
                String content = new String(entry.getValue().data);
                result.put(url, content);
            }
        }
        return result;
    }
}
