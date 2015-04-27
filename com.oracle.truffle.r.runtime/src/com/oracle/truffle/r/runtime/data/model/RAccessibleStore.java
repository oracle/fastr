package com.oracle.truffle.r.runtime.data.model;

/**
 * Marks a vectors internal store to be accessible. As the internal store depends on the
 * implementation of the vector, users of this method are discouraged to use this interface unless
 * it is really necessary. One reason might be to avoid store field lookups when traversing vectors.
 */
public interface RAccessibleStore<T> {

    T getInternalStore();

}
