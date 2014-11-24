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
package com.oracle.truffle.r.nodes.wrapper.processor;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.type.*;
import javax.lang.model.util.*;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/**
 * Analyzes classes tagged with {@code CreateWrapper} and generates a custom wrapper subclass.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("com.oracle.truffle.r.nodes.instrument.CreateWrapper")
public class WrapperProcessor extends AbstractProcessor {
    private static final boolean trace = false;
    private static final String INDENT4 = "    ";
    private static final String INDENT8 = INDENT4 + INDENT4;
    private static final String INDENT12 = INDENT8 + INDENT4;
    private TypeElement createWrapperElement;

    @Override
    public synchronized void init(ProcessingEnvironment pe) {
        super.init(pe);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            if (roundEnv.processingOver()) {
                return true;
            }
            note("CreateWrapperProcessor: analyzing classes");
            createWrapperElement = processingEnv.getElementUtils().getTypeElement("com.oracle.truffle.r.nodes.instrument.CreateWrapper");
            for (Element element : roundEnv.getElementsAnnotatedWith(createWrapperElement)) {
                if (element instanceof TypeElement) {
                    TypeElement classElement = (TypeElement) element;
                    PackageElement packageElement = getPackage(classElement);
                    WrappedClassVisitor methodVisitor = new WrappedClassVisitor(classElement);
                    classElement.accept(methodVisitor, null);
                    generate(packageElement, classElement, methodVisitor.wrappedMethods);
                }
            }
        } catch (Exception ex) {
            error("error generating Wrapper classes: " + ex);
            StackTraceElement[] elements = ex.getStackTrace();
            for (StackTraceElement element : elements) {
                error(element.toString());
            }
        }
        return true;
    }

    private void generate(PackageElement packageElement, TypeElement classElement, Set<ExecutableElement> wrappedMethods) throws IOException {
        String packageName = packageElement.getQualifiedName().toString();
        String className = classElement.getSimpleName().toString();
        String qualClassName = classElement.getQualifiedName().toString();
        String wrapperClassName = classElement.getSimpleName().toString() + "Wrapper";
        JavaFileObject srcLocator = processingEnv.getFiler().createSourceFile(packageName + "." + wrapperClassName);
        try (PrintWriter wr = new PrintWriter(new BufferedWriter(srcLocator.openWriter()))) {
            wr.println("// DO NOT EDIT, generated automatically");
            wr.printf("package %s;%n", packageName);
            wr.println();
            wr.printf("import com.oracle.truffle.api.instrument.Probe;%n");
            wr.printf("import com.oracle.truffle.api.instrument.ProbeNode;%n");
            wr.printf("import com.oracle.truffle.api.instrument.ProbeNode.WrapperNode;%n");
            wr.printf("import com.oracle.truffle.api.nodes.Node;%n");
            wr.printf("import com.oracle.truffle.r.nodes.RNode;%n");
            wr.printf("import com.oracle.truffle.r.runtime.RDeparse;%n");
            wr.printf("import com.oracle.truffle.r.runtime.env.REnvironment;%n");
            wr.println();
            wr.printf("public final class %s  extends %s implements WrapperNode {%n", wrapperClassName, qualClassName);
            wr.printf("%s@Child %s child;%n", INDENT4, qualClassName);
            wr.printf("%s@Child private ProbeNode probeNode;%n", INDENT4);
            wr.println();
            wr.printf("%spublic %s(%s child) {%n", INDENT4, wrapperClassName, qualClassName);
            wr.printf("%sassert child != null;%n", INDENT8);
            wr.printf("%sassert !(child instanceof %s);%n", INDENT8, wrapperClassName);
            wr.printf("%sthis.child = child;%n", INDENT8);
            wr.printf("%s}%n", INDENT4);
            wr.printf("%spublic String instrumentationInfo() {%n", INDENT4);
            wr.printf("%sreturn \"Wrapper node for %s\";%n", INDENT8, qualClassName);
            wr.printf("%s}%n", INDENT4);
            wr.println();
            wr.printf("%spublic Node getChild() {%n", INDENT4);
            wr.printf("%sreturn child;%n", INDENT8);
            wr.printf("%s}%n", INDENT4);
            wr.println();
            wr.printf("%spublic Probe getProbe() {%n", INDENT4);
            wr.printf("%stry {%n", INDENT8);
            wr.printf("%s    return probeNode.getProbe();%n", INDENT8);
            wr.printf("%s} catch (IllegalStateException e) {%n", INDENT8);
            wr.printf("%sthrow new IllegalStateException(\"A lite-Probed wrapper has no explicit Probe\");%n", INDENT12);
            wr.printf("%s}%n", INDENT8);
            wr.printf("%s}%n", INDENT4);
            wr.printf("%spublic void insertProbe(ProbeNode newProbeNode) {%n", INDENT4);
            wr.printf("%sthis.probeNode = newProbeNode;%n", INDENT8);
            wr.printf("%s}%n", INDENT4);
            wr.println();

            for (ExecutableElement wrappedMethod : wrappedMethods) {
                String methodName = wrappedMethod.getSimpleName().toString();
                TypeMirror returnType = wrappedMethod.getReturnType();
                boolean voidReturn = returnType.getKind() == TypeKind.VOID;
                List<? extends VariableElement> formals = wrappedMethod.getParameters();
                wr.printf("%s@Override%n", INDENT4);
                wr.printf("%spublic %s %s(", INDENT4, returnType, methodName);
                if (formals.size() > 0) {
                    for (int i = 0; i < formals.size(); i++) {
                        wr.printf("%s %s", formals.get(i).asType(), formals.get(i));
                        if (i != formals.size() - 1) {
                            wr.print(", ");
                        }
                    }
                }
                wr.println(") {");

                boolean isExecuteMethod = isExecuteMethod(methodName, formals);
                if (isExecuteMethod) {
                    wr.printf("%sprobeNode.enter(child, %s);%n", INDENT8, formals.get(0));
                    wr.println();
                    if (!voidReturn) {
                        wr.printf("%s%s result;%n", INDENT8, returnType);
                    }
                    wr.printf("%stry {%n", INDENT8);
                }

                String prefix = isExecuteMethod ? (voidReturn ? "" : "result = ") : "return ";
                wr.printf("%s%schild.%s(", isExecuteMethod ? INDENT12 : INDENT8, prefix, methodName);
                if (formals.size() > 0) {
                    for (int i = 0; i < formals.size(); i++) {
                        wr.printf("%s", formals.get(i));
                        if (i != formals.size() - 1) {
                            wr.print(", ");
                        }
                    }
                }
                wr.println(");");

                if (isExecuteMethod) {
                    wr.printf("%sprobeNode.return%s(child, %s%s);%n", INDENT12, voidReturn ? "Void" : "Value", formals.get(0), voidReturn ? "" : ", result");
                    if (returnType.getKind() != TypeKind.VOID) {
                        wr.printf("%sreturn result;%n", INDENT12);
                    }
                    wr.printf("%s} catch (Exception e) {%n", INDENT8);
                    wr.printf("%sprobeNode.returnExceptional(child, %s, e);%n", INDENT12, formals.get(0));
                    wr.printf("%sthrow (e);%n", INDENT12);
                    wr.printf("%s}%n", INDENT8);
                }

                wr.printf("%s}%n", INDENT4);
                wr.println();
            }

            // FastR specific
            wr.printf("%s@Override%n", INDENT4);
            wr.printf("%spublic void deparse(RDeparse.State state) {%n", INDENT4);
            wr.printf("%schild.deparse(state);%n", INDENT8);
            wr.printf("%s}%n", INDENT4);
            wr.println();
            wr.printf("%s@Override%n", INDENT4);
            wr.printf("%spublic boolean isInstrumentable() {%n", INDENT4);
            wr.printf("%sreturn false;%n", INDENT8);
            wr.printf("%s}%n", INDENT4);
            wr.println();
            wr.printf("%s@Override%n", INDENT4);
            wr.printf("%spublic boolean isSyntax() {%n", INDENT4);
            wr.printf("%sreturn false;%n", INDENT8);
            wr.printf("%s}%n", INDENT4);
            wr.println();
            wr.printf("%s@Override%n", INDENT4);
            wr.printf("%spublic RNode substitute(REnvironment env) {%n", INDENT4);
            wr.printf("%s%s wrapperSub = new %s((%s) child.substitute(env));%n", INDENT8, wrapperClassName, wrapperClassName, qualClassName);
            wr.printf("%sProbeNode.insertProbe(wrapperSub);%n", INDENT8);
            wr.printf("%sreturn wrapperSub;%n", INDENT8);
            wr.printf("%s}%n", INDENT8);

            wr.println("}");
        }

    }

    private static boolean isExecuteMethod(String methodName, List<? extends VariableElement> formals) {
        return methodName.startsWith("execute") && formals.size() >= 1 && simpleName(formals.get(0).asType()).equals("VirtualFrame");
    }

    private static String simpleName(TypeMirror typeMirror) {
        String name = typeMirror.toString();
        int index = name.lastIndexOf('.');
        return name.substring(index + 1);
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

    @SuppressWarnings("unused")
    private void warning(String msg) {
        diagnostic(Diagnostic.Kind.WARNING, msg);
    }

    private void error(String msg) {
        diagnostic(Diagnostic.Kind.ERROR, msg);
    }

    public void note(String msg) {
        if (trace) {
            diagnostic(Diagnostic.Kind.NOTE, msg);
        }
    }

    private String toFirstLower(String s) {
        return s.substring(0, 1).toLowerCase() + s.substring(1);
    }

    private class WrappedClassVisitor extends ElementScanner8<Void, Void> {
        private final Set<ExecutableElement> wrappedMethods = new HashSet<>();
        private final TypeElement root;

        WrappedClassVisitor(TypeElement root) {
            this.root = root;
        }

        @Override
        public Void visitTypeParameter(TypeParameterElement e, Void p) {
            return null;
        }

        @Override
        public Void visitExecutable(ExecutableElement element, Void p) {
            Set<Modifier> modifiers = element.getModifiers();
            for (Modifier m : modifiers) {
                if (m == Modifier.ABSTRACT || hasCreateWrapper(element)) {
                    wrappedMethods.add(element);
                }
            }
            return null;
        }

        @Override
        public Void visitVariable(VariableElement element, Void p) {
            return null;
        }

        @Override
        public Void visitType(TypeElement element, Void p) {
            // note("visit: " + element.getSimpleName().toString() + " nkind " +
            // element.getNestingKind());
            if (element == root || element.getNestingKind() == NestingKind.TOP_LEVEL) {
                // visit superclass
                TypeMirror superClassMirror = element.getSuperclass();
                if (superClassMirror instanceof DeclaredType) {
                    TypeElement superClassElement = (TypeElement) ((DeclaredType) superClassMirror).asElement();
                    superClassElement.accept(this, p);
                }
                // visit this class
                return super.visitType(element, p);
            } else {
                // ignore nested classes
                return null;
            }
        }

        @Override
        public Void visitPackage(PackageElement element, Void p) {
            return null;
        }

        private boolean hasCreateWrapper(ExecutableElement element) {
            List<? extends AnnotationMirror> mirrors = element.getAnnotationMirrors();
            for (AnnotationMirror m : mirrors) {
                if (m.getAnnotationType().asElement() == createWrapperElement) {
                    return true;
                }
            }
            return false;
        }

    }

}
