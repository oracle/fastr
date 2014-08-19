/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.parser.tools;

import java.awt.*;
import java.lang.reflect.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;

public class TreeViewer extends JTree {

    private static final long serialVersionUID = 1L;

    private static Map<Class<?>, Field[]> fieldsForClass = new LinkedHashMap<>();

    private static TreeViewer treeViewer;
    private static final Object[] roots = new Object[100];

    public static void showTree(Object root) {
        System.arraycopy(roots, 0, roots, 1, roots.length - 1);
        roots[0] = root;
        if (treeViewer == null) {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
                e.printStackTrace();
            }
            treeViewer = new TreeViewer("Basic Tree viewer (using reflection)");
        } else {
            treeViewer.updateRoots();
        }
    }

    private JFrame frame;

    private static Field[] getFieldsFor(Class<?> clazz) {
        if (clazz.getName().startsWith("java.lang.") || Enum.class.isAssignableFrom(clazz)) {
            return new Field[0];
        }
        if (fieldsForClass.containsKey(clazz)) {
            return fieldsForClass.get(clazz);
        }
        Class<?> current = clazz;
        ArrayList<Field> fields = new ArrayList<>();
        while (current != Object.class) {
            for (Field f : current.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers()) || Modifier.isSynchronized(f.getModifiers())) {
                    continue;
                }
                if (f.getName().equals("parent") || f.getName().equals("source")) {
                    continue;
                }
                f.setAccessible(true);
                fields.add(f);
            }
            current = current.getSuperclass();
        }
        Field[] res = fields.toArray(new Field[fields.size()]);
        fieldsForClass.put(clazz, res);
        return res;
    }

    private void updateRoots() {
        setModel(newModel());
        treeDidChange();
        frame.setVisible(true);
    }

    public TreeViewer(String title) {
        setRootVisible(false);
        setShowsRootHandles(true);
        frame = new JFrame(title);
        JScrollPane scrollPane = new JScrollPane(this);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(scrollPane);

        Container content = frame.getContentPane();
        content.add(panel, BorderLayout.CENTER);
        frame.setAlwaysOnTop(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setSize(800, 400);

        frame.setVisible(true);
    }

    private static final class Node {
        public final String text;
        public final Object value;

        public Node(String text, Object value) {
            if (value instanceof Object[]) {
                this.text = text + " = " + value.getClass().getComponentType().getSimpleName() + "[" + ((Object[]) value).length + "]";
            } else {
                String valueString = String.valueOf(value);
                if (valueString.length() > 80) {
                    valueString = valueString.substring(0, 80) + "...";
                }
                this.text = text + " = " + (value == null ? "null" : ("\"" + valueString + "\" (" + value.getClass().getSimpleName() + ")"));
            }
            this.value = value;
        }
    }

    @Override
    public String convertValueToText(Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        if (value instanceof Node) {
            return ((Node) value).text;
        } else {
            return String.valueOf(value);
        }
    }

    private static TreeModel newModel() {
        return new TreeModel() {

            @Override
            public Object getRoot() {
                return new Node("root", roots);
            }

            @Override
            public Object getChild(Object parent, int index) {
                Object parentValue = ((Node) parent).value;
                try {
                    if (parentValue instanceof Object[]) {
                        return new Node("[" + index + "]", ((Object[]) parentValue)[index]);
                    } else {
                        Field field = getFieldsFor(parentValue.getClass())[index];
                        return new Node(field.getName(), field.get(parentValue));
                    }
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            public int getChildCount(Object parent) {
                Object parentValue = ((Node) parent).value;
                if (parentValue instanceof Object[]) {
                    return ((Object[]) parentValue).length;
                } else if (parentValue != null) {
                    return getFieldsFor(parentValue.getClass()).length;
                } else {
                    return 0;
                }
            }

            @Override
            public boolean isLeaf(Object node) {
                return getChildCount(node) == 0;
            }

            @Override
            public void valueForPathChanged(TreePath path, Object newValue) {
            }

            @Override
            public int getIndexOfChild(Object parent, Object child) {
                Object parentValue = ((Node) parent).value;
                Object childValue = ((Node) child).value;
                if (parentValue instanceof Object[]) {
                    Object[] array = (Object[]) parentValue;
                    for (int i = 0; i < array.length; i++) {
                        if (array[i] == childValue) {
                            return i;
                        }
                    }
                } else {
                    int i = 0;
                    Field[] fields = getFieldsFor(parentValue.getClass());
                    for (Field field : fields) {
                        try {
                            if (field.get(parentValue) == childValue) {
                                return i;
                            }
                        } catch (IllegalArgumentException | IllegalAccessException e) {
                            e.printStackTrace();
                        }
                        i++;
                    }
                }
                return -1;
            }

            @Override
            public void addTreeModelListener(TreeModelListener l) {
            }

            @Override
            public void removeTreeModelListener(TreeModelListener l) {
            }
        };
    }
}
