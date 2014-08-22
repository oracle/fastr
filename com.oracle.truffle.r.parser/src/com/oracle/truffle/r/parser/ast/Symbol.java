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
package com.oracle.truffle.r.parser.ast;

import java.util.*;

public final class Symbol {

    private static final SymbolTable symbolTable = new SymbolTable(); // TODO put in Context ??!!

    private final String name;

    private Symbol(String name) {
        this.name = name;
    }

    public int id() { // TODO add a field for global numbering and use it !
        return hashCode(); // id = currentId++;
    }

    public int hash() { // TODO add a field for filtering!
        return hashCode(); // hash = 1 << (currentHash = currentHash + 1 % Integer.size);
    }

    public String pretty() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    private static class SymbolTable {

        // TODO A less stupid implementation for symbol table
        // i.e., close to a set implementation with linear probing
        private final Map<String, Symbol> table = new HashMap<>();

        private Symbol get(String name) {
            Symbol sym = table.get(name);
            if (sym == null) {
                table.put(name, sym = new Symbol(name));
            }
            return sym;
        }

        private Symbol[] list() { // NOTE: this includes "null" values for symbols currently unused
            Symbol[] res = new Symbol[table.size()];
            return table.values().toArray(res);
        }
    }

    public static Symbol getSymbol(String name) {
        return symbolTable.get(name);
    }

    public static Symbol[] listSymbols() { // NOTE: this uses null values for symbols currently
        // unused
        return symbolTable.list();
    }

    public static Symbol[] getSymbols(String[] names) {
        Symbol[] symbols = new Symbol[names.length];
        for (int i = 0; i < names.length; i++) {
            symbols[i] = Symbol.getSymbol(names[i]);
        }
        return symbols;
    }

}
