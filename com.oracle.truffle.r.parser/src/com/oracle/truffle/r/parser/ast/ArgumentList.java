/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2012-2013, Purdue University
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.parser.ast;

import java.util.*;

public interface ArgumentList extends Collection<ArgumentList.Entry> {

    Entry first();

    void add(ASTNode value);

    void add(String name, ASTNode value);

    void add(Symbol name, ASTNode value);

    interface Entry {

        Symbol getName();

        ASTNode getValue();
    }

    ASTNode getNode(int i);

    class Default extends ArrayList<Entry> implements ArgumentList {

        private static final long serialVersionUID = 1L;

        @Override
        public void add(ASTNode e) {
            super.add(new DefaultEntry(null, e));
        }

        @Override
        public void add(Symbol name, ASTNode value) {
            super.add(new DefaultEntry(name, value));
        }

        @Override
        public void add(String name, ASTNode value) {
            add(Symbol.getSymbol(name), value);
        }

        @Override
        public Entry first() {
            return this.get(0);
        }

        @Override
        public ASTNode getNode(int i) {
            return this.get(i).getValue();
        }

        public static void updateParent(ASTNode parent, ArgumentList list) {
            for (Entry e : list) {
                parent.updateParent((ASTNode) e);
            }
        }

        public static final class DefaultEntry extends ASTNode implements Entry {

            Symbol name;
            ASTNode value;

            private DefaultEntry(Symbol n, ASTNode v) {
                name = n;
                value = updateParent(v);
            }

            @Override
            public Symbol getName() {
                return name;
            }

            @Override
            public ASTNode getValue() {
                return value;
            }

            @Override
            public <R> List<R> visitAll(Visitor<R> v) {
                return Arrays.asList(getValue().accept(v));
            }

            @Override
            public <R> R accept(Visitor<R> v) {
                return v.visit(this);
            }
        }
    }
}
