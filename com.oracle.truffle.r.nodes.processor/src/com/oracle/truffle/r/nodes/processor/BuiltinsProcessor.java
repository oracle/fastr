/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.processor;

import java.io.*;
import java.util.*;

import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;
import javax.tools.*;

import com.oracle.truffle.r.runtime.*;

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("com.oracle.truffle.r.runtime.RBuiltin")
public class BuiltinsProcessor extends AbstractProcessor {

    private static int round;

    private static class PackageBuiltins {
        PackageElement packageElement;
        ArrayList<TypeElement> builtinClassElements = new ArrayList<>();

        PackageBuiltins(PackageElement packageElement) {
            this.packageElement = packageElement;
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver() || round > 0) {
            return true;
        }
        round++;
        try {
            note("BuiltinProcessor: analyzing RBuiltins");
            Map<PackageElement, PackageBuiltins> map = new HashMap<>();
            for (Element element : roundEnv.getElementsAnnotatedWith(RBuiltin.class)) {
                TypeElement classElement = (TypeElement) element;
                PackageElement packageElement = getPackage(classElement);
                PackageBuiltins packageBuiltins = map.get(packageElement);
                if (packageBuiltins == null) {
                    packageBuiltins = new PackageBuiltins(packageElement);
                    map.put(packageElement, packageBuiltins);
                }
                packageBuiltins.builtinClassElements.add(classElement);
            }
            writeBuiltinsFiles(map);
        } catch (Exception ex) {
            error("error generating RBUILTINS: " + ex);
        }
        return true;
    }

    private void writeBuiltinsFiles(Map<PackageElement, PackageBuiltins> map) throws IOException {
        for (PackageBuiltins packageBuiltins : map.values()) {
            String packageName = packageBuiltins.packageElement.getQualifiedName().toString();
            FileObject locator = processingEnv.getFiler().createResource(StandardLocation.SOURCE_OUTPUT, packageName, "RBUILTINS");
            try (BufferedOutputStream bs = new BufferedOutputStream(new FileOutputStream(locator.toUri().getPath()))) {
                for (TypeElement builtinClassElement : packageBuiltins.builtinClassElements) {
                    String qualName = builtinClassElement.getQualifiedName().toString();
                    if (!(builtinClassElement.getEnclosingElement() instanceof PackageElement)) {
                        // nested class
                        int lastDotIndex = qualName.lastIndexOf('.');
                        qualName = qualName.substring(0, lastDotIndex) + '$' + qualName.substring(lastDotIndex + 1);
                    }
                    bs.write(qualName.getBytes());
                    bs.write('\n');
                }
            }
        }
    }

    private static PackageElement getPackage(Element element) {
        Element enclosing = element.getEnclosingElement();
        while (!(enclosing instanceof PackageElement)) {
            enclosing = enclosing.getEnclosingElement();
        }
        return (PackageElement) enclosing;
    }

    private void diagnostic(Diagnostic.Kind kind, String msg) {
        processingEnv.getMessager().printMessage(kind, msg);
    }

    private void error(String msg) {
        diagnostic(Diagnostic.Kind.ERROR, msg);
    }

    public void note(String msg) {
        diagnostic(Diagnostic.Kind.NOTE, msg);
    }

}
