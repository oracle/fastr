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

import com.oracle.truffle.api.CompilerDirectives.*;
import com.oracle.truffle.r.parser.ast.*;

public class TreeViewer extends JTree {

    private static final long serialVersionUID = 1L;

    private static Map<Class, Field[]> fieldsForClass = new LinkedHashMap<>();

    private static TreeViewer treeViewer;

    public static void showTree(ASTNode root) {
        if (treeViewer == null) {
            treeViewer = new TreeViewer("Basic Tree viewer (using reflection)", root);
        } else {
            treeViewer.setRoot(root);
        }
    }

    ASTNode root;
    JFrame frame;

    private static Field[] getFieldsFor(Class clazz) {
        if (fieldsForClass.containsKey(clazz)) {
            return fieldsForClass.get(clazz);
        }
        Class current = clazz;
        ArrayList<Field> fields = new ArrayList<>();
        while (current != ASTNode.class) {
            Field[] f = current.getDeclaredFields();
            for (int i = 0; i < f.length; i++) {
                if (ASTNode.class.isAssignableFrom(f[i].getType())) {
                    f[i].setAccessible(true);
                    fields.add(f[i]);
                }
            }
            current = current.getSuperclass();
        }
        Field[] res = fields.toArray(new Field[fields.size()]);
        fieldsForClass.put(clazz, res);
        return res;
    }

    void setRoot(ASTNode newRoot) {
        root = newRoot;
        setModel(newModel());
        treeDidChange();
        frame.setVisible(true);
    }

    public TreeViewer(String title, ASTNode node) {
        super();

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

        setRoot(node);

        frame.setVisible(true);
    }

    @Override
    @SlowPath
    public String convertValueToText(Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        StringBuffer res = new StringBuffer();
        if (hasFocus) {
            ASTNode parent = ((ASTNode) value).getParent();
            if (parent != null) {
                for (Field f : getFieldsFor(parent.getClass())) {
                    try {
                        if (f.get(parent) == value) {
                            res.append('[');
                            res.append(f.getName());
                            res.append("] ");
                            break;
                        }
                    } catch (IllegalArgumentException | IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        res.append(value.getClass().getSimpleName());
        res.append(": ");
        res.append(value.toString());
        if (!hasFocus) {
            res.append("                        ");
        }
        return res.toString();

    }

    private TreeModel newModel() {
        return new TreeModel() {

            @Override
            public Object getRoot() {
                return root;
            }

            @Override
            public Object getChild(Object parent, int index) {
                try {
                    return getFieldsFor(parent.getClass())[index].get(parent);
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            public int getChildCount(Object parent) {
                return getFieldsFor(parent.getClass()).length;
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
                int i = 0;
                Field[] fields = getFieldsFor(parent.getClass());
                for (Field field : fields) {
                    if (field == child) {
                        return i;
                    }
                    i++;
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
