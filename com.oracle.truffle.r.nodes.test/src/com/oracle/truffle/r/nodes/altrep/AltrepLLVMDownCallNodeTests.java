package com.oracle.truffle.r.nodes.altrep;

import com.oracle.truffle.r.ffi.impl.llvm.AltrepLLVMDownCallNode;
import com.oracle.truffle.r.nodes.test.TestBase;
import com.oracle.truffle.r.nodes.test.TestUtilities.NodeHandle;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.ffi.NativeFunction;
import com.oracle.truffle.r.test.altrep.SimpleDescriptorWrapper;
import org.junit.Test;

import java.util.Arrays;

import static com.oracle.truffle.r.nodes.test.TestUtilities.createHandle;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class AltrepLLVMDownCallNodeTests extends TestBase {
    private NodeHandle<AltrepLLVMDownCallNode> altrepDownCallNodeHandle;

    public AltrepLLVMDownCallNodeTests() {
        execInContext(() -> {
            altrepDownCallNodeHandle = createHandle(AltrepLLVMDownCallNode.create(),
                    (node, args) -> {
                        assert args[0] instanceof NativeFunction;
                        Object[] restOfArgs = Arrays.copyOfRange(args, 1, args.length);
                        return node.call((NativeFunction) args[0], restOfArgs);
                    });
            return null;
        });
    }

    @Test
    public void testSimple() {
        execInContext(() -> {
            SimpleDescriptorWrapper simpleDescriptorWrapper = new SimpleDescriptorWrapper();
            RIntVector altIntVec = simpleDescriptorWrapper.getAltIntVector();
            final int index = 1;
            Object elem = altrepDownCallNodeHandle.call(NativeFunction.AltInteger_Elt, altIntVec, index);
            assertThat(elem, is(instanceOf(Integer.class)));
            assertThat(elem, equalTo(altIntVec.getDataAt(index)));
            return null;
        });
    }
}
