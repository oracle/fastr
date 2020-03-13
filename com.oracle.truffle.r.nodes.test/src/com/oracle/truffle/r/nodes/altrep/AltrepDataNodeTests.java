package com.oracle.truffle.r.nodes.altrep;

import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.CoreMatchers.instanceOf;
import static com.oracle.truffle.r.nodes.test.TestUtilities.createHandle;

import com.oracle.truffle.r.ffi.impl.nodes.AltrepData1Node;
import com.oracle.truffle.r.ffi.impl.nodes.AltrepData2Node;
import com.oracle.truffle.r.nodes.test.TestBase;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.nodes.test.TestUtilities.NodeHandle;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.test.altrep.IntVectorDescriptorWrapper;
import com.oracle.truffle.r.test.altrep.SimpleDescriptorWrapper;
import org.junit.Assert;
import org.junit.Test;
import org.junit.Assume;

public class AltrepDataNodeTests extends TestBase {

    private NodeHandle<AltrepData1Node> altrepData1NodeNodeHandle;
    private NodeHandle<AltrepData2Node> altrepData2NodeNodeHandle;

    public AltrepDataNodeTests() {
        execInContext(() -> {
            altrepData1NodeNodeHandle = createHandle(AltrepData1Node.create(),
                    (node, args) -> node.executeObject(args[0]));
            altrepData2NodeNodeHandle = createHandle(AltrepData2Node.create(),
                    (node, args) -> node.executeObject(args[0]));
            return null;
        });
    }

    @Test
    public void testSimple() {
        execInContext(() -> {
            SimpleDescriptorWrapper simpleDescriptorWrapper = new SimpleDescriptorWrapper();
            RIntVector altIntVector = simpleDescriptorWrapper.getAltIntVector();

            Object data1 = altrepData1NodeNodeHandle.call(altIntVector);
            Object data2 = altrepData2NodeNodeHandle.call(altIntVector);

            assertThat(data1, is(RNull.instance));
            assertThat(data2, is(RNull.instance));
            return null;
        });
    }

    @Test
    public void testIntVectorWrapper() {
        execInContext(() -> {
            IntVectorDescriptorWrapper intVectorDescriptorWrapper = new IntVectorDescriptorWrapper();
            RIntVector altIntVector = intVectorDescriptorWrapper.getAltIntVector();

            Object data1 = altrepData1NodeNodeHandle.call(altIntVector);
            Object data2 = altrepData2NodeNodeHandle.call(altIntVector);

            assertThat(data1, is(instanceOf(RIntVector.class)));
            assertThat(data2, is(RNull.instance));
            return null;
        });
    }
}
