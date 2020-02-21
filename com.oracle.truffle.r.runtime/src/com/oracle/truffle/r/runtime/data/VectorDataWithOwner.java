package com.oracle.truffle.r.runtime.data;

import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

/**
 * Interface for vector data objects (see {@link VectorDataLibrary}) that need a reference to their
 * owning vector.
 */
interface VectorDataWithOwner {
    void setOwner(RAbstractVector owner);
}
