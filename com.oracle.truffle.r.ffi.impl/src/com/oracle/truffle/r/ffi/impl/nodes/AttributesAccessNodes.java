package com.oracle.truffle.r.ffi.impl.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.attributes.CopyOfRegAttributesNode;
import com.oracle.truffle.r.nodes.attributes.GetAttributesNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetNamesAttributeNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RAttributeStorage;
import com.oracle.truffle.r.runtime.data.RExternalPtr;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RStringVector;

public final class AttributesAccessNodes {

    abstract static class ATTRIB extends FFIUpCallNode.Arg1 {
        @Child private GetAttributesNode getAttributesNode = GetAttributesNode.create();

        @Specialization
        public Object doAttributable(RAttributable obj) {
            return getAttributesNode.execute(obj);
        }

        @Fallback
        public RNull doOthers(Object obj) {
            if (obj == RNull.instance || RRuntime.isForeignObject(obj)) {
                return RNull.instance;
            } else {
                CompilerDirectives.transferToInterpreter();
                String type = obj == null ? "null" : obj.getClass().getSimpleName();
                throw RError.error(RError.NO_CALLER, Message.GENERIC, "object of type '" + type + "' cannot be attributed");
            }
        }
    }

    abstract static class TAG extends FFIUpCallNode.Arg1 {

        @Specialization
        public Object doPairlist(RPairList obj) {
            return obj.getTag();
        }

        @Specialization
        public Object doArgs(RArgsValuesAndNames obj) {
            ArgumentsSignature signature = obj.getSignature();
            if (signature.getLength() > 0 && signature.getName(0) != null) {
                return signature.getName(0);
            }
            return RNull.instance;
        }

        @Specialization
        public Object doExternalPtr(RExternalPtr obj) {
            return obj.getTag();
        }

        @Specialization
        public Object doList(RList obj,
                        @Cached("create()") GetNamesAttributeNode getNamesAttributeNode) {
            RStringVector names = getNamesAttributeNode.getNames(obj);
            if (names != null && names.getLength() > 0) {
                return names.getDataAt(0);
            }
            return RNull.instance;
        }

        @Fallback
        @TruffleBoundary
        public RNull doOthers(Object obj) {
            throw RInternalError.unimplemented("TAG is not implemented for type " + obj.getClass().getSimpleName());
        }
    }

    abstract static class CopyMostAttrib extends FFIUpCallNode.Arg2 {

        @Child protected CopyOfRegAttributesNode copyRegAttributes = CopyOfRegAttributesNode.create();

        @Specialization
        public Object doList(RAttributeStorage x, RAttributeStorage y) {
// if (copyRegAttributes == null) {
// CompilerDirectives.transferToInterpreterAndInvalidate();
// copyRegAttributes = CopyOfRegAttributesNode.create();
// }
            copyRegAttributes.execute(x, y);
            return null;
        }

        @Fallback
        @SuppressWarnings("unused")
        public Void doOthers(Object x, Object y) {
            throw RInternalError.unimplemented("Rf_copyMostAttrib only works with atrributables.");
        }
    }
}
