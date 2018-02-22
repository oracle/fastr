package com.oracle.truffle.r.ffi.impl.nodes;

import static com.oracle.truffle.r.runtime.data.NativeDataAccess.readNativeString;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.RInternalError;

public abstract class NativeStringCastNode extends Node {

    @Child private Node isPtrNode;

    public static NativeStringCastNode create() {
        return NativeStringCastNodeGen.create();
    }

    public abstract String executeObject(Object s);

    @Specialization
    @TruffleBoundary
    String handleString(String s) {
        return s;
    }

    protected static Node createAsPointerNode() {
        return Message.AS_POINTER.createNode();
    }

    protected static Node createToNativeNode() {
        return Message.TO_NATIVE.createNode();
    }

    protected boolean isPointerNode(TruffleObject s) {
        if (isPtrNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            isPtrNode = insert(Message.IS_POINTER.createNode());
        }
        return ForeignAccess.sendIsPointer(isPtrNode, s);
    }

    @Specialization(guards = "isPointerNode(s)")
    String handlePointerAddress(TruffleObject s, @Cached("createAsPointerNode()") Node sAsPtrNode) {
        try {
            return readNativeString(ForeignAccess.sendAsPointer(sAsPtrNode, s));
        } catch (UnsupportedMessageException e) {
            throw RInternalError.shouldNotReachHere(e);
        }
    }

    @Specialization(guards = "!isPointerNode(s)")
    String handleNonPointerAddress(TruffleObject s, @Cached("createToNativeNode()") Node sToNativeNode, @Cached("createAsPointerNode()") Node sAsPtrNode) {
        try {
            Object sNative = ForeignAccess.sendToNative(sToNativeNode, s);
            return readNativeString(ForeignAccess.sendAsPointer(sAsPtrNode, (TruffleObject) sNative));
        } catch (UnsupportedMessageException e) {
            throw RInternalError.shouldNotReachHere(e);
        }
    }
}
