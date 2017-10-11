/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.ffi.processor;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

public final class FFIProcessor extends AbstractProcessor {

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> annotations = new HashSet<>();
        annotations.add("com.oracle.truffle.r.ffi.processor.RFFIUpCallRoot");
        return annotations;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            return false;
        }
        process0(roundEnv);
        return true;
    }

    private void process0(RoundEnvironment roundEnv) {
        for (Element e : roundEnv.getElementsAnnotatedWith(RFFIUpCallRoot.class)) {
            try {
                processElement(e);
            } catch (Throwable ex) {
                ex.printStackTrace();
                String message = "Uncaught error in " + this.getClass();
                processingEnv.getMessager().printMessage(Kind.ERROR, message + ": " + printException(ex), e);
            }
        }
    }

    private void processElement(Element e) throws IOException {
        if (e.getKind() != ElementKind.INTERFACE) {
            processingEnv.getMessager().printMessage(Kind.ERROR, "RFFIUpCallRoot must annotate an interface");
        }
        Types types = processingEnv.getTypeUtils();
        TypeElement typeElement = (TypeElement) e;
        List<? extends TypeMirror> extended = typeElement.getInterfaces();
        int count = 0;
        for (TypeMirror tm : extended) {
            TypeElement x = (TypeElement) types.asElement(tm);
            List<? extends Element> methods = x.getEnclosedElements();
            count += methods.size();
        }
        ExecutableElement[] methods = new ExecutableElement[count];
        count = 0;
        for (TypeMirror tm : extended) {
            TypeElement x = (TypeElement) types.asElement(tm);
            List<? extends Element> encMethods = x.getEnclosedElements();
            for (Element encMethod : encMethods) {
                methods[count++] = (ExecutableElement) encMethod;
            }
        }
        Arrays.sort(methods, new Comparator<ExecutableElement>() {
            @Override
            public int compare(ExecutableElement e1, ExecutableElement e2) {
                return e1.getSimpleName().toString().compareTo(e2.getSimpleName().toString());
            }
        });
        generateTable(methods);
        generateMessageClasses(methods);
        generateCallbacks(methods);
        generateCallbacksIndexHeader(methods);
    }

    private void generateTable(ExecutableElement[] methods) throws IOException {
        JavaFileObject fileObj = processingEnv.getFiler().createSourceFile("com.oracle.truffle.r.ffi.impl.upcalls.RFFIUpCallTable");
        Writer w = fileObj.openWriter();
        w.append("// GENERATED by com.oracle.truffle.r.ffi.processor.FFIProcessor class; DO NOT EDIT\n");
        w.append("package com.oracle.truffle.r.ffi.impl.upcalls;\n");
        w.append("/**\n" +
                        " * The following enum contains an entry for each method in the interface annoated\n" +
                        " * with @RFFIUpCallRoot including inherited methods.\n" +
                        " */");
        w.append("public enum RFFIUpCallTable {\n");
        for (int i = 0; i < methods.length; i++) {
            ExecutableElement method = methods[i];
            w.append("    ").append(method.getSimpleName().toString()).append(i == methods.length - 1 ? ";" : ",").append('\n');
        }

        w.append("}\n");
        w.close();
    }

    private void generateCallbacksIndexHeader(ExecutableElement[] methods) throws IOException {
        FileObject fileObj = processingEnv.getFiler().createResource(StandardLocation.SOURCE_OUTPUT, "com.oracle.truffle.r.ffi.impl.upcalls", "rffi_upcallsindex.h");
        note("If you edited any UpCallsRFFI interfaces do: cp " + fileObj.toUri().getPath() + " com.oracle.truffle.r.native/fficall/src/common\n");
        Writer w = fileObj.openWriter();
        w.append("// GENERATED; DO NOT EDIT\n");
        w.append("#ifndef RFFI_UPCALLSINDEX_H\n");
        w.append("#define RFFI_UPCALLSINDEX_H\n");
        w.append('\n');
        for (int i = 0; i < methods.length; i++) {
            ExecutableElement method = methods[i];
            w.append("#define ").append(method.getSimpleName().toString()).append("_x ").append(Integer.toString(i)).append('\n');
        }
        w.append('\n');
        w.append("#define ").append("UPCALLS_TABLE_SIZE ").append(Integer.toString(methods.length)).append('\n');
        w.append('\n');
        w.append("#endif // RFFI_UPCALLSINDEX_H\n");
        w.close();
    }

    private void generateMessageClasses(ExecutableElement[] methods) throws IOException {
        for (int i = 0; i < methods.length; i++) {
            ExecutableElement m = methods[i];
            generateCallClass(m);
        }
    }

    private void generateCallClass(ExecutableElement m) throws IOException {
        RFFIUpCallNode nodeAnnotation = m.getAnnotation(RFFIUpCallNode.class);
        String canRunGc = m.getAnnotation(RFFIRunGC.class) == null ? "false" : "true";
        String nodeClassName = null;
        TypeElement nodeClass = null;
        if (nodeAnnotation != null) {
            try {
                nodeAnnotation.value();
            } catch (MirroredTypeException e) {
                nodeClass = (TypeElement) processingEnv.getTypeUtils().asElement(e.getTypeMirror());
                nodeClassName = nodeClass.getQualifiedName().toString();
            }
        }
        // process arguments first to see if unwrap is necessary
        List<? extends VariableElement> params = m.getParameters();
        StringBuilder arguments = new StringBuilder();
        StringBuilder unwrapNodes = new StringBuilder();
        for (int i = 0; i < params.size(); i++) {
            if (i != 0) {
                arguments.append(", ");
            }
            TypeMirror paramType = params.get(i).asType();

            RFFICpointer[] pointerAnnotations = params.get(i).getAnnotationsByType(RFFICpointer.class);
            RFFICstring[] stringAnnotations = params.get(i).getAnnotationsByType(RFFICstring.class);

            String paramName = params.get(i).getSimpleName().toString();
            String paramTypeName = getTypeName(paramType);
            boolean needsUnwrap = !paramType.getKind().isPrimitive() &&
                            !paramTypeName.equals("java.lang.String") &&
                            pointerAnnotations.length == 0 &&
                            (stringAnnotations.length == 0 || stringAnnotations[0].convert());
            boolean needCast = !paramTypeName.equals("java.lang.Object");
            if (needCast) {
                arguments.append('(').append(paramTypeName).append(") ");
            }
            if (needsUnwrap) {
                arguments.append(paramName).append("Unwrap").append(".execute(");
                unwrapNodes.append("                @Child private FFIUnwrapNode ").append(paramName).append("Unwrap").append(" = FFIUnwrapNode.create();\n");
            }
            arguments.append("arguments.get(").append(i).append(")");
            if (needsUnwrap) {
                arguments.append(')');
            }
        }

        TypeKind returnKind = m.getReturnType().getKind();
        boolean needsReturnWrap = returnKind != TypeKind.VOID && !returnKind.isPrimitive() &&
                        !"java.lang.String".equals(getTypeName(m.getReturnType())) &&
                        m.getAnnotationsByType(RFFICpointer.class).length == 0;
        if (needsReturnWrap) {
            unwrapNodes.append("                @Child private FFIWrapNode returnWrap").append(" = FFIWrapNode.create();\n");
        }

        String name = m.getSimpleName().toString();
        String callName = name + "Call";
        JavaFileObject fileObj = processingEnv.getFiler().createSourceFile("com.oracle.truffle.r.ffi.impl.upcalls." + callName);
        Writer w = fileObj.openWriter();
        w.append("// GENERATED by FFIProcessor; DO NOT EDIT\n");
        w.append("\n");
        w.append("package com.oracle.truffle.r.ffi.impl.upcalls;\n");
        w.append("\n");
        w.append("import java.util.List;\n");
        w.append("\n");
        w.append("import com.oracle.truffle.api.CallTarget;\n");
        w.append("import com.oracle.truffle.api.CompilerDirectives;\n");
        w.append("import com.oracle.truffle.api.Truffle;\n");
        w.append("import com.oracle.truffle.api.frame.VirtualFrame;\n");
        w.append("import com.oracle.truffle.api.interop.ForeignAccess;\n");
        w.append("import com.oracle.truffle.api.interop.TruffleObject;\n");
        w.append("import com.oracle.truffle.api.nodes.RootNode;\n");
        w.append("import com.oracle.truffle.r.runtime.context.RContext;\n");
        if (!returnKind.isPrimitive() && returnKind != TypeKind.VOID) {
            w.append("import com.oracle.truffle.r.runtime.data.RDataFactory;\n");
        }
        w.append("import com.oracle.truffle.r.runtime.ffi.CallRFFI.HandleUpCallExceptionNode;\n");
        w.append("import com.oracle.truffle.r.runtime.ffi.RFFIContext;\n");
        w.append("import com.oracle.truffle.r.ffi.impl.common.RFFIUtils;\n");
        w.append("import com.oracle.truffle.r.ffi.impl.upcalls.UpCallsRFFI;\n");
        w.append("import com.oracle.truffle.r.runtime.data.RTruffleObject;\n");
        w.append("\n");
        w.append("// Checkstyle: stop method name check\n");
        w.append("\n");
        w.append("final class " + callName + " implements RTruffleObject {\n");
        w.append('\n');
        if (nodeClass == null) {
            w.append("    private final UpCallsRFFI upCallsImpl;\n");
            w.append('\n');
        }
        w.append("    " + callName + "(UpCallsRFFI upCallsImpl) {\n");
        w.append("        assert upCallsImpl != null;\n");
        if (nodeClass == null) {
            w.append("        this.upCallsImpl = upCallsImpl;\n");
        }
        w.append("    }\n");
        w.append('\n');
        w.append("    private static final class " + callName + "Factory extends AbstractDowncallForeign {\n");
        w.append("        @Override\n");
        w.append("        public boolean canHandle(TruffleObject obj) {\n");
        w.append("            return obj instanceof " + callName + ";\n");
        w.append("        }\n");
        w.append("\n");
        w.append("        @Override\n");
        w.append("        public CallTarget accessExecute(int argumentsLength) {\n");
        w.append("            return Truffle.getRuntime().createCallTarget(new RootNode(null) {\n");
        w.append("\n");
        if (unwrapNodes.length() > 0) {
            w.append(unwrapNodes);
            w.append("\n");
        }
        if (nodeClass != null) {
            boolean createFunction = false;
            for (Element element : nodeClass.getEnclosedElements()) {
                if (element.getKind() == ElementKind.METHOD && element.getModifiers().contains(Modifier.STATIC) && "create".equals(element.getSimpleName().toString())) {
                    createFunction = true;
                    break;
                }
            }
            if (createFunction) {
                w.append("                @Child private " + nodeClassName + " node = " + nodeClassName + ".create();\n");
            } else if (nodeClass.getModifiers().contains(Modifier.ABSTRACT)) {
                w.append("                @Child private " + nodeClassName + " node;\n");
                processingEnv.getMessager().printMessage(Kind.ERROR, "Need static create for abstract classes", m);
            } else {
                w.append("                @Child private " + nodeClassName + " node = new " + nodeClassName + "();\n");
            }

        }
        w.append("                HandleUpCallExceptionNode handleExceptionNode = HandleUpCallExceptionNode.create();");
        w.append("\n");
        w.append("                @Override\n");
        w.append("                public Object execute(VirtualFrame frame) {\n");
        w.append("                    List<Object> arguments = ForeignAccess.getArguments(frame);\n");
        w.append("                    assert arguments.size() == " + params.size() + " : \"wrong number of arguments passed to " + name + "\";\n");
        w.append("                    if (RFFIUtils.traceEnabled) {\n");
        w.append("                        RFFIUtils.traceUpCall(\"" + name + "\", arguments);\n");
        w.append("                    }\n");
        w.append("                    RFFIContext ctx = RContext.getInstance().getStateRFFI();\n");
        if (returnKind != TypeKind.VOID) {
            w.append("                Object resultRObj;");
        }
        w.append("                    ctx.beforeUpcall(" + canRunGc + ");\n");
        w.append("                    try {\n");
        if (returnKind == TypeKind.VOID) {
            w.append("                        ");
        } else {
            w.append("                        resultRObj = ");
        }
        if (needsReturnWrap) {
            w.append("returnWrap.execute(");
        }
        if (nodeClass != null) {
            w.append("node.executeObject");
        } else {
            w.append("((" + callName + ") ForeignAccess.getReceiver(frame)).upCallsImpl." + name);
        }
        w.append("(" + arguments + ")");
        if (needsReturnWrap) {
            w.append(");\n");
        } else {
            w.append(";\n");
        }
        w.append("                    } catch (Exception ex) {\n");
        w.append("                        CompilerDirectives.transferToInterpreter();\n");
        w.append("                        handleExceptionNode.execute(ex);\n");
        if (returnKind.isPrimitive()) {
            w.append("                        resultRObj = Integer.valueOf(-1);\n");
        } else if (returnKind != TypeKind.VOID) {
            w.append("                        resultRObj = RDataFactory.createIntVectorFromScalar(-1);\n");
        }
        w.append("                    }\n");
        w.append("                    ctx.afterUpcall(" + canRunGc + ");\n");
        if (returnKind == TypeKind.VOID) {
            w.append("                    return 0; // void return type\n");
        } else {
            if (!returnKind.isPrimitive() && m.getAnnotationsByType(RFFICpointer.class).length == 0) {
                w.append("                    ctx.registerReferenceUsedInNative(resultRObj); \n");
            }
            w.append("                    return resultRObj;\n");
        }
        w.append("                }\n");
        w.append("            });\n");
        w.append("        }\n");
        w.append("    }\n");
        w.append("\n");
        w.append("    private static final ForeignAccess ACCESS = ForeignAccess.create(new " + callName + "Factory(), null);\n");
        w.append("\n");
        w.append("    @Override\n");
        w.append("    public ForeignAccess getForeignAccess() {\n");
        w.append("        return ACCESS;\n");
        w.append("    }\n");
        w.append("}\n");
        w.close();
    }

    private void generateCallbacks(ExecutableElement[] methods) throws IOException {
        JavaFileObject fileObj = processingEnv.getFiler().createSourceFile("com.oracle.truffle.r.ffi.impl.upcalls.Callbacks");
        Writer w = fileObj.openWriter();
        w.append("// GENERATED; DO NOT EDIT\n\n");
        w.append("package com.oracle.truffle.r.ffi.impl.upcalls;\n\n");
        w.append("import com.oracle.truffle.api.interop.TruffleObject;\n");
        w.append("import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;\n");
        w.append("import com.oracle.truffle.r.ffi.impl.upcalls.UpCallsRFFI;\n\n");
        w.append("public enum Callbacks {\n");
        for (int i = 0; i < methods.length; i++) {
            ExecutableElement m = methods[i];
            String sig = getNFISignature(m);
            w.append("    ").append(m.getSimpleName().toString()).append('(').append('"').append(sig).append('"').append(')');
            w.append(i == methods.length - 1 ? ';' : ',');
            w.append("\n");
        }
        w.append('\n');
        w.append("    public final String nfiSignature;\n");
        w.append("    @CompilationFinal public TruffleObject call;\n\n");
        w.append("    Callbacks(String signature) {\n");
        w.append("        this.nfiSignature = signature;\n");
        w.append("    }\n\n");

        w.append("    public static void createCalls(UpCallsRFFI upCallsRFFIImpl) {\n");
        w.append("        for (Callbacks callback : values()) {\n");
        w.append("            switch (callback) {\n");
        for (int i = 0; i < methods.length; i++) {
            ExecutableElement m = methods[i];
            String callName = m.getSimpleName().toString() + "Call";
            w.append("                case ").append(m.getSimpleName().toString()).append(":\n");
            w.append("                    callback.call = new ").append(callName).append("(upCallsRFFIImpl);\n");
            w.append("                    break;\n\n");
        }
        w.append("            }\n");
        w.append("        }\n");
        w.append("    }\n");
        w.append("}\n");
        w.close();
    }

    private String getNFISignature(ExecutableElement m) {
        List<? extends VariableElement> params = m.getParameters();
        int lparams = params.size();
        StringBuilder sb = new StringBuilder();
        sb.append('(');
        for (int i = 0; i < lparams; i++) {
            VariableElement param = params.get(i);
            RFFICstring[] annotations = param.getAnnotationsByType(RFFICstring.class);
            String nfiParam = nfiParamName(param.asType(), annotations.length == 0 ? null : annotations[0], false, param);
            sb.append(nfiParam);
            if (i != lparams - 1) {
                sb.append(", ");
            }
        }
        sb.append(')');
        sb.append(" : ");
        sb.append(nfiParamName(m.getReturnType(), null, true, m));
        return sb.toString();
    }

    private String nfiParamName(TypeMirror paramType, RFFICstring rffiCstring, boolean isReturn, Element m) {
        String paramTypeName = getTypeName(paramType);
        switch (paramTypeName) {
            case "java.lang.Object":
                if (rffiCstring == null) {
                    return "pointer";
                } else {
                    return rffiCstring.convert() ? "string" : "pointer";
                }
            case "boolean":
                return "uint8";
            case "int":
                return "sint32";
            case "long":
                return "sint64";
            case "double":
                return "double";
            case "void":
                return "void";
            case "int[]":
                return "[sint32]";
            case "long[]":
                return "[sint64]";
            case "double[]":
                return "[double]";
            case "byte[]":
                return "[uint8]";
            default:
                if ("java.lang.String".equals(paramTypeName)) {
                    if (isReturn) {
                        return "string";
                    } else if (rffiCstring != null) {
                        if (!rffiCstring.convert()) {
                            processingEnv.getMessager().printMessage(Kind.ERROR, "Invalid parameter type (without conversion) " + paramTypeName, m);
                        }
                        return "string";
                    }
                }
                if (isReturn) {
                    processingEnv.getMessager().printMessage(Kind.ERROR, "Invalid return type " + paramTypeName, m);
                } else {
                    processingEnv.getMessager().printMessage(Kind.ERROR, "Invalid parameter type " + paramTypeName, m);
                }
                return "pointer";
        }
    }

    private String getTypeName(TypeMirror type) {
        Types types = processingEnv.getTypeUtils();
        TypeKind kind = type.getKind();
        String returnType;
        if (kind.isPrimitive() || kind == TypeKind.VOID) {
            returnType = kind.name().toLowerCase();
        } else {
            Element rt = types.asElement(type);
            returnType = rt.toString();
        }
        return returnType;
    }

    private void note(String msg) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, msg);
    }

    private static String printException(Throwable e) {
        StringWriter string = new StringWriter();
        PrintWriter writer = new PrintWriter(string);
        e.printStackTrace(writer);
        writer.flush();
        string.flush();
        return e.getMessage() + "\r\n" + string.toString();
    }
}
