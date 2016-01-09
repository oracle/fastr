package com.oracle.truffle.r.engine.interop;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ForeignAccess.Factory10;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.r.engine.TruffleRLanguage;
import com.oracle.truffle.r.engine.interop.RAbstractVectorAccessFactory.InteropRootNode;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

public final class RListAccessFactory implements Factory10 {

    public abstract class InteropRootNode extends RootNode {
        public InteropRootNode() {
            super(TruffleRLanguage.class, null, null);
        }
    }

    public CallTarget accessIsNull() {
        return Truffle.getRuntime().createCallTarget(new InteropRootNode() {
            @Override
            public Object execute(VirtualFrame frame) {
                return false;
            }
        });
    }

    public CallTarget accessIsExecutable() {
        throw RInternalError.unimplemented("accessIsExecutable");
    }

    public CallTarget accessIsBoxed() {
        return Truffle.getRuntime().createCallTarget(new InteropRootNode() {
            @Override
            public Object execute(VirtualFrame frame) {
                return false;
            }
        });
    }

    public CallTarget accessHasSize() {
        return Truffle.getRuntime().createCallTarget(new InteropRootNode() {
            @Override
            public Object execute(VirtualFrame frame) {
                return true;
            }
        });
    }

    public CallTarget accessGetSize() {
        throw RInternalError.unimplemented("accessGetSize");
    }

    public CallTarget accessUnbox() {
        throw RInternalError.unimplemented("accessUnbox");
    }

    public CallTarget accessRead() {
        throw RInternalError.unimplemented("accessRead");
    }

    public CallTarget accessWrite() {
        throw RInternalError.unimplemented("accessWrite");
    }

    public CallTarget accessExecute(int argumentsLength) {
        throw RInternalError.unimplemented("accessExecute");
    }

    public CallTarget accessInvoke(int argumentsLength) {
        throw RInternalError.unimplemented("accessInvoke");
    }

    public CallTarget accessNew(int argumentsLength) {
        throw RInternalError.unimplemented("accessNew");
    }

    public CallTarget accessMessage(Message unknown) {
        throw RInternalError.unimplemented("accessMessage");
    }

}
