package com.oracle.truffle.r.nodes.altrep;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.isA;
import static org.junit.Assert.assertThat;
import com.oracle.truffle.r.ffi.impl.nodes.NewAltRepNode;
import com.oracle.truffle.r.nodes.test.TestBase;
import com.oracle.truffle.r.nodes.test.TestUtilities.NodeHandle;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.altrep.AltIntegerClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.AltrepUtilities;
import com.oracle.truffle.r.test.altrep.SimpleDescriptorWrapper;
import org.junit.Test;

import static com.oracle.truffle.r.nodes.test.TestUtilities.createHandle;
import static org.junit.Assert.assertTrue;

public class NewAltrepNodeTests extends TestBase {
    private NodeHandle<NewAltRepNode> newAltRepNodeHandle;

    public NewAltrepNodeTests() {
        execInContext(() -> {
            newAltRepNodeHandle = createHandle(NewAltRepNode.create(),
                    (node, args) -> node.executeObject(args[0], args[1], args[2]));
            return null;
        });
    }

    @Test
    public void testSimple() {
        execInContext(() -> {
            SimpleDescriptorWrapper simpleDescriptorWrapper = new SimpleDescriptorWrapper();
            AltIntegerClassDescriptor descriptor = simpleDescriptorWrapper.getDescriptor();
            Object newAltIntVecObj = newAltRepNodeHandle.call(descriptor, RNull.instance, RNull.instance);

            assertThat(newAltIntVecObj, is(instanceOf(RIntVector.class)));
            RIntVector newAltIntVec = (RIntVector) newAltIntVecObj;
            assertTrue(newAltIntVec.isAltRep());
            assertThat(descriptor, equalTo(AltrepUtilities.getAltIntDescriptor(newAltIntVec)));
            return null;
        });
    }
}
