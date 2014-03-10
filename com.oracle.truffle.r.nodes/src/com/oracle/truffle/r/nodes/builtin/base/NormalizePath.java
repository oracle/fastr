package com.oracle.truffle.r.nodes.builtin.base;

import java.io.*;
import java.nio.file.*;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin("normalizePath")
public abstract class NormalizePath extends RBuiltinNode {

    @Specialization(order = 0)
    public Object doNormalizePath(RAbstractStringVector pathVec, @SuppressWarnings("unused") RMissing winslash, @SuppressWarnings("unused") RMissing mustWork) {
        return doNormalizePath(pathVec, null, RRuntime.LOGICAL_NA);
    }

    @Specialization(order = 1)
    public Object doNormalizePath(RAbstractStringVector pathVec, @SuppressWarnings("unused") String winslash, byte mustWork) {
        String[] results = new String[pathVec.getLength()];
        FileSystem fileSystem = FileSystems.getDefault();
        for (int i = 0; i < results.length; i++) {
            String path = pathVec.getDataAt(i);
            String normPath = PathExpand.check(path);
            try {
                normPath = fileSystem.getPath(path).toRealPath().toString();
            } catch (IOException e) {
                if (mustWork != RRuntime.LOGICAL_FALSE) {
                    String msg = e instanceof NoSuchFileException ? "No such file or directory: " + path : e.toString();
                    throw RError.getGenericError(getSourceSection(), msg);
                }
            }
            results[i] = normPath;
        }
        return RDataFactory.createStringVector(results, RDataFactory.COMPLETE_VECTOR);
    }

    @SuppressWarnings("unused")
    @Generic
    public Object doNormalizePath(Object path, Object winslash, Object mustWork) {
        throw RError.getWrongTypeOfArgument(getSourceSection());
    }
}
