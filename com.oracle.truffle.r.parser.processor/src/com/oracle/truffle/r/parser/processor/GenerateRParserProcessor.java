/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.parser.processor;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

import org.antlr.v4.Tool;
import org.antlr.v4.tool.ANTLRMessage;
import org.antlr.v4.tool.ANTLRToolListener;

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("com.oracle.truffle.r.parser.processor.GenerateRParser")
public class GenerateRParserProcessor extends AbstractProcessor {
    private static final class Listener implements ANTLRToolListener {
        public final Tool tool;
        public final StringBuilder contents = new StringBuilder();

        Listener(Tool tool) {
            this.tool = tool;
        }

        @Override
        public void info(String msg) {
            contents.append(msg).append('\n');
        }

        @Override
        public void error(ANTLRMessage msg) {
            contents.append(tool.errMgr.getMessageTemplate(msg).render()).append('\n');
        }

        @Override
        public void warning(ANTLRMessage msg) {
            error(msg);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            return true;
        }
        for (TypeElement annotation : annotations) {
            for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                File antlrGenDir = null;
                try {
                    String pkg = ((PackageElement) element.getEnclosingElement()).getQualifiedName().toString();

                    Filer filer = processingEnv.getFiler();
                    File srcGenDir = getSrcGenDir(filer);
                    // note("srcgendir: " + srcGenDir.getAbsolutePath());
                    File suiteRoot = srcGenDir.getCanonicalFile().getParentFile().getParentFile().getParentFile();
                    // note("suiteRoot: " + suiteRoot.getAbsolutePath());

                    // Our src directory
                    File parserSrcDir = join(suiteRoot, pkg, "src", pkg.replace('.', File.separatorChar));
                    // note("parserSrcDir: " + parserSrcDir.getAbsolutePath());
                    antlrGenDir = mkTmpDir(null);
                    String[] command = new String[]{"-no-listener", "-no-visitor", "-o", antlrGenDir.getAbsolutePath(), join(parserSrcDir, "R.g").getAbsolutePath()};
                    // noteCommand(command);

                    Tool tool = new Tool(command);
                    Listener listener = new Listener(tool);
                    tool.addListener(listener);
                    tool.processGrammarsOnCommandLine();
                    if (tool.errMgr.getNumErrors() > 0) {
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                                        String.format("Parser failed to execute command %s. Output:%s", Arrays.toString(command), listener.contents), element);
                        return false;
                    }
                    // Now create the actual source files, copying the ANTLR output
                    createSourceFile(filer, pkg, "RParser", antlrGenDir);
                    createSourceFile(filer, pkg, "RLexer", antlrGenDir);
                } catch (Exception ex) {
                    handleThrowable(ex, element);
                    return false;
                } finally {
                    if (antlrGenDir != null) {
                        deleteTmpDir(antlrGenDir);
                    }
                }
            }
        }
        return true;
    }

    private void note(String msg) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, msg);
    }

    @SuppressWarnings("unused")
    private void noteCommand(String[] command) {
        for (String e : command) {
            note(e);
        }
    }

    private void handleThrowable(Throwable t, Element e) {
        String message = "Uncaught error in " + getClass().getSimpleName() + " while processing " + e + " ";
        processingEnv.getMessager().printMessage(Kind.ERROR, message + ": " + printException(t), e);
    }

    private static String printException(Throwable e) {
        StringWriter string = new StringWriter();
        PrintWriter writer = new PrintWriter(string);
        e.printStackTrace(writer);
        writer.flush();
        string.flush();
        return e.getMessage() + "\n" + string.toString();
    }

    private static File join(File parent, String... args) {
        File result = parent;
        for (String arg : args) {
            result = new File(result, arg);
        }
        return result;
    }

    private static void createSourceFile(Filer filer, String pkg, String className, File antlrGenDir) throws IOException {
        JavaFileObject file = filer.createSourceFile(pkg + "." + className);
        File antlrFile = join(antlrGenDir, className + ".java");
        byte[] content = new byte[(int) antlrFile.length()];
        try (BufferedInputStream is = new BufferedInputStream(new FileInputStream(antlrFile)); BufferedOutputStream os = new BufferedOutputStream(file.openOutputStream())) {
            is.read(content);
            os.write("// GENERATED CONTENT - DO NOT EDIT\n".getBytes());
            os.write("// Checkstyle: stop\n".getBytes());
            String contentString = new String(content, StandardCharsets.UTF_8);
            // make the parser generic
            os.write(contentString.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static File mkTmpDir(File dir) throws IOException {
        File tmp = File.createTempFile("antlr", null, dir);
        String tmpName = tmp.getAbsolutePath();
        tmp.delete();
        File result = new File(tmpName);
        result.mkdir();
        return result;
    }

    private static void deleteTmpDir(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                f.delete();
            }
        }
        dir.delete();
    }

    /**
     * Find out the directory where generated sources will be put, which is a path to the suite root
     * that works in mx and Eclipse.
     */
    private static File getSrcGenDir(Filer filer) throws IOException {
        // The tmp file is not actually created because we don't create a stream to it.
        return new File(filer.createResource(StandardLocation.SOURCE_OUTPUT, "", "tmp").toUri().getPath()).getParentFile();
    }
}
