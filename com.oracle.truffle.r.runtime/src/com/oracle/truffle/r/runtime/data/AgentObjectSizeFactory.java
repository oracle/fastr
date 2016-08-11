/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import javax.tools.ToolProvider;

import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.data.RObjectSize.IgnoreObjectHandler;
import com.oracle.truffle.r.runtime.data.RObjectSize.TypeCustomizer;

/**
 * Uses an instrumentation agent to get an accurate estimate of an objects size, plus reflection to
 * aggregate the size of object-valued fields. Sharing is not handled in the general case, although
 * some special cases are handed, such as the fact that {@link RNull} is a singleton.
 *
 * In order to satisfy the requirements of the Java instrumentation API, we have to load the agent
 * from a jar file. The creation and loading is all orchestrated by this class.
 */
public class AgentObjectSizeFactory extends ObjectSizeFactory {

    private Map<Class<?>, ArrayList<Field>> objectFieldsMap = new HashMap<>();
    private static Map<Class<?>, TypeCustomizer> customizerMap = new HashMap<>(); // system wide

    public AgentObjectSizeFactory() {
        if (!ObjSizeAgent.isInitialized()) {
            try {
                createAgentJar();
            } catch (Exception ex) {
                // not available
                Utils.rSuicide("failed to load ObjSizeAgent: " + ex.getMessage());
            }
        }
    }

    /**
     * Adds the class file bytes for a given class to a JAR stream.
     */
    static void add(JarOutputStream jar, Class<?> c) throws IOException {
        String name = c.getName();
        String classAsPath = name.replace('.', '/') + ".class";
        jar.putNextEntry(new JarEntry(classAsPath));

        InputStream stream = c.getClassLoader().getResourceAsStream(classAsPath);

        int nRead;
        byte[] buf = new byte[1024];
        while ((nRead = stream.read(buf, 0, buf.length)) != -1) {
            jar.write(buf, 0, nRead);
        }

        jar.closeEntry();
    }

    protected void createAgentJar() throws Exception {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        Attributes mainAttrs = manifest.getMainAttributes();
        mainAttrs.putValue("Agent-Class", ObjSizeAgent.class.getName());
        mainAttrs.putValue("Premain-Class", ObjSizeAgent.class.getName());

        Path jar = Files.createTempFile("myagent", ".jar");
        try {
            JarOutputStream jarStream = new JarOutputStream(new FileOutputStream(jar.toFile()), manifest);
            add(jarStream, ObjSizeAgent.class);
            jarStream.close();

            loadAgent(jar);
        } finally {
            Files.deleteIfExists(jar);
        }
    }

    public static void loadAgent(Path agent) throws Exception {
        String vmName = ManagementFactory.getRuntimeMXBean().getName();
        int p = vmName.indexOf('@');
        String pid = vmName.substring(0, p);
        ClassLoader cl = ToolProvider.getSystemToolClassLoader();
        Class<?> c = Class.forName("com.sun.tools.attach.VirtualMachine", true, cl);
        Method attach = c.getDeclaredMethod("attach", String.class);
        Method loadAgent = c.getDeclaredMethod("loadAgent", String.class, String.class);
        Object vm = attach.invoke(null, pid);
        loadAgent.invoke(vm, agent.toString(), "");
    }

    @Override
    public long getObjectSize(Object obj, IgnoreObjectHandler ignoreObjectHandler) {
        return getObjectSize(obj, obj, ignoreObjectHandler);
    }

    private long getObjectSize(Object rootObj, Object obj, IgnoreObjectHandler ignoreObjectHandler) {
        try {
            long basicSize = ObjSizeAgent.objectSize(obj);
            long size = basicSize;
            Class<?> klass = obj.getClass();
            if (klass.isArray() && !klass.getComponentType().isPrimitive()) {
                for (int i = 0; i < Array.getLength(obj); i++) {
                    Object elem = Array.get(obj, i);
                    if (elem == null || isNa(elem)) {
                        continue;
                    } else {
                        size += getObjectSize(rootObj, elem, ignoreObjectHandler);
                    }
                }
            } else {
                ArrayList<Field> objectFields = objectFieldsMap.get(klass);
                if (objectFields == null) {
                    objectFields = new ArrayList<>();
                    findObjectFields(obj.getClass(), objectFields);
                    objectFieldsMap.put(klass, objectFields);
                }
                for (Field objectField : objectFields) {
                    Object fieldObj = objectField.get(obj);
                    if (fieldObj == null || ignoreObjectHandler.ignore(rootObj, fieldObj)) {
                        continue;
                    } else {
                        TypeCustomizer typeCustomizer = getCustomizer(fieldObj.getClass());
                        if (typeCustomizer == null) {
                            size += getObjectSize(rootObj, fieldObj, ignoreObjectHandler);
                        } else {
                            size += typeCustomizer.getObjectSize(fieldObj);
                        }
                    }
                }
            }
            return size;
        } catch (Throwable t) {
            throw RInternalError.shouldNotReachHere(t);
        }

    }

    private static boolean isNa(Object elem) {
        String typeName = elem.getClass().getSimpleName();
        switch (typeName) {
            case "Integer":
                return RRuntime.isNA((int) elem);
            case "Double":
                return RRuntime.isNA((double) elem);
            case "String":
                return RRuntime.isNA((String) elem);
            default:
                return false;
        }
    }

    /**
     * Walks the superclass hierarchy of {@code klass} and accumulates all object-valued fields in
     * {@code objectFields}.
     */
    private static void findObjectFields(Class<?> klass, ArrayList<Field> objectFields) {
        if (klass != Object.class) {
            findObjectFields(klass.getSuperclass(), objectFields);
            Field[] fields = klass.getDeclaredFields();
            for (Field field : fields) {
                Class<?> fieldClass = field.getType();
                if (fieldClass.isPrimitive()) {
                    continue;
                }
                int modifiers = field.getModifiers();
                if (Modifier.isStatic(modifiers)) {
                    continue;
                }
                // check for special case of an completely ignored type
                if (getCustomizer(fieldClass) == RObjectSize.IGNORE) {
                    continue;
                }
                field.setAccessible(true);
                objectFields.add(field);
            }
        }
    }

    private static TypeCustomizer getCustomizer(Class<?> objClass) {
        for (Map.Entry<Class<?>, TypeCustomizer> entry : customizerMap.entrySet()) {
            if (entry.getKey().isAssignableFrom(objClass)) {
                return entry.getValue();
            }
        }
        return null;

    }

    @Override
    public void registerTypeCustomizer(Class<?> klass, TypeCustomizer typeCustomizer) {
        customizerMap.put(klass, typeCustomizer);
    }

}
