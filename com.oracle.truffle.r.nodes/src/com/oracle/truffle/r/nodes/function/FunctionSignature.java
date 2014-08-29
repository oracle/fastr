package com.oracle.truffle.r.nodes.function;

import java.util.*;

/**
 * TODO Gero, add comment!
 */
public class FunctionSignature {
    private final String[] suppliedNames;

    public FunctionSignature(String[] suppliedNames) {
        this.suppliedNames = suppliedNames;
    }

    public boolean isEqualTo(FunctionSignature other) {
        return equals(other);
    }

    public boolean isNotEqualTo(FunctionSignature other) {
        return !equals(other);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(suppliedNames);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        FunctionSignature other = (FunctionSignature) obj;
        if (!Arrays.equals(suppliedNames, other.suppliedNames))
            return false;
        return true;
    }
}
