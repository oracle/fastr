package com.oracle.truffle.r.runtime.data;

import com.oracle.truffle.api.library.GenerateLibrary;
import com.oracle.truffle.api.library.Library;
import com.oracle.truffle.api.library.LibraryFactory;

@GenerateLibrary
public abstract class RPairListLibrary extends Library {

    private static final LibraryFactory<RPairListLibrary> FACTORY = LibraryFactory.resolve(RPairListLibrary.class);

    public static LibraryFactory<RPairListLibrary> getFactory() {
        return FACTORY;
    }

    public static RPairListLibrary getUncached() {
        return FACTORY.getUncached();
    }

    public abstract Object car(Object target);

    public abstract Object cdr(Object target);

    public abstract Object getTag(Object target);

}
