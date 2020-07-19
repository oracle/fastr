package com.oracle.truffle.r.runtime.data.altrep;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RAltComplexVectorData;
import com.oracle.truffle.r.runtime.data.RAltIntVectorData;
import com.oracle.truffle.r.runtime.data.RAltLogicalVectorData;
import com.oracle.truffle.r.runtime.data.RAltRawVectorData;
import com.oracle.truffle.r.runtime.data.RAltRealVectorData;
import com.oracle.truffle.r.runtime.data.RAltStringVectorData;
import com.oracle.truffle.r.runtime.data.RAltrepVectorData;
import com.oracle.truffle.r.runtime.data.RBaseObject;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractAtomicVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.ffi.AltrepRFFI;
import com.oracle.truffle.r.runtime.ffi.AltrepRFFIFactory;

public class AltrepUtilities {
    public static boolean isAltrep(Object object) {
        return object instanceof RBaseObject && ((RBaseObject) object).isAltRep();
    }

    // Helper methods for getting altrep vector data from vectors:

    public static RAltrepVectorData getAltRepVectorData(RAbstractAtomicVector altrepVector) {
        assert altrepVector.isAltRep();
        return (RAltrepVectorData) altrepVector.getData();
    }

    public static RAltIntVectorData getAltIntVectorData(RIntVector altIntVector) {
        assert altIntVector.isAltRep();
        return (RAltIntVectorData) altIntVector.getData();
    }

    public static RAltRealVectorData getAltRealVectorData(RDoubleVector altRealVector) {
        assert altRealVector.isAltRep();
        return (RAltRealVectorData) altRealVector.getData();
    }

    public static RAltLogicalVectorData getAltLogicalVectorData(RLogicalVector altLogicalVector) {
        assert altLogicalVector.isAltRep();
        return (RAltLogicalVectorData) altLogicalVector.getData();
    }

    public static RAltComplexVectorData getAltComplexVectorData(RComplexVector altComplexVector) {
        assert altComplexVector.isAltRep();
        return (RAltComplexVectorData) altComplexVector.getData();
    }

    public static RAltRawVectorData getAltRawVectorData(RRawVector altRawVector) {
        assert altRawVector.isAltRep();
        return (RAltRawVectorData) altRawVector.getData();
    }

    private static RAltStringVectorData getAltStringVectorData(RStringVector altStringVector) {
        assert altStringVector.isAltRep();
        return (RAltStringVectorData) altStringVector.getData();
    }

    // Helper methods for getting class descriptors from vectors:

    public static AltIntegerClassDescriptor getAltIntDescriptor(RIntVector altIntVector) {
        assert altIntVector.isAltRep();
        return getAltIntVectorData(altIntVector).getDescriptor();
    }

    public static AltRealClassDescriptor getAltRealDescriptor(RDoubleVector altRealVector) {
        assert altRealVector.isAltRep();
        return getAltRealVectorData(altRealVector).getDescriptor();
    }

    public static AltLogicalClassDescriptor getAltLogicalDescriptor(RLogicalVector altLogicalVector) {
        assert altLogicalVector.isAltRep();
        return getAltLogicalVectorData(altLogicalVector).getDescriptor();
    }

    public static AltComplexClassDescriptor getAltComplexDescriptor(RComplexVector altComplexVector) {
        assert altComplexVector.isAltRep();
        return getAltComplexVectorData(altComplexVector).getDescriptor();
    }

    public static AltRawClassDescriptor getAltRawDescriptor(RRawVector altRawVector) {
        assert altRawVector.isAltRep();
        return getAltRawVectorData(altRawVector).getDescriptor();
    }

    public static AltStringClassDescriptor getAltStringDescriptor(RStringVector altStringVector) {
        assert altStringVector.isAltRep();
        return getAltStringVectorData(altStringVector).getDescriptor();
    }

    public static AltRepClassDescriptor getAltRepClassDescriptor(RBaseObject altrepObject) {
        assert altrepObject.isAltRep();

        if (altrepObject instanceof RIntVector) {
            return getAltIntDescriptor((RIntVector) altrepObject);
        } else if (altrepObject instanceof RStringVector) {
            return getAltStringDescriptor((RStringVector) altrepObject);
        } else if (altrepObject instanceof RDoubleVector) {
            return getAltRealDescriptor((RDoubleVector) altrepObject);
        } else if (altrepObject instanceof RLogicalVector) {
            return getAltLogicalDescriptor((RLogicalVector) altrepObject);
        } else {
            throw RInternalError.unimplemented();
        }
    }

    public static AltVecClassDescriptor getAltVecClassDescriptor(RBaseObject altrepObject) {
        // Currently all the AltRepClassDescriptors are also AltVecClassDescriptors (GNU-R version 3.6.1)
        return (AltVecClassDescriptor) getAltRepClassDescriptor(altrepObject);
    }

    public static RPairList getPairListData(RAbstractAtomicVector altrepVec) {
        assert altrepVec.isAltRep();
        assert altrepVec.getData() instanceof RAltrepVectorData;
        RAltrepVectorData vectorData = (RAltrepVectorData) altrepVec.getData();
        return vectorData.getAltrepData().getDataPairList();
    }

    public static AltrepMethodDescriptor getDuplicateMethodDescriptor(RAbstractAtomicVector altrepVector) {
        return getAltRepClassDescriptor(altrepVector).getDuplicateMethodDescriptor();
    }

    // Method descriptor getters from altinteger vectors:

    public static AltrepMethodDescriptor getLengthMethodDescriptor(RIntVector altIntVector) {
        return getAltIntDescriptor(altIntVector).getLengthMethodDescriptor();
    }

    public static AltrepMethodDescriptor getEltMethodDescriptor(RIntVector altIntVector) {
        return getAltIntDescriptor(altIntVector).getEltMethodDescriptor();
    }

    public static AltrepMethodDescriptor getDataptrMethodDescriptor(RIntVector altIntVector) {
        return getAltIntDescriptor(altIntVector).getDataptrMethodDescriptor();
    }

    public static AltrepMethodDescriptor getIsSortedMethodDescriptor(RIntVector altIntVector) {
        return getAltIntDescriptor(altIntVector).getIsSortedMethodDescriptor();
    }

    public static AltrepMethodDescriptor getNoNAMethodDescriptor(RIntVector altIntVector) {
        return getAltIntDescriptor(altIntVector).getNoNAMethodDescriptor();
    }

    public static AltrepMethodDescriptor getGetRegionMethodDescriptor(RIntVector altIntVector) {
        return getAltIntDescriptor(altIntVector).getGetRegionMethodDescriptor();
    }

    public static AltrepMethodDescriptor getSumMethodDescriptor(RIntVector altIntVector) {
        return getAltIntDescriptor(altIntVector).getSumMethodDescriptor();
    }

    public static AltrepMethodDescriptor getMaxMethodDescriptor(RIntVector altIntVector) {
        return getAltIntDescriptor(altIntVector).getMaxMethodDescriptor();
    }

    public static AltrepMethodDescriptor getMinMethodDescriptor(RIntVector altIntVector) {
        return getAltIntDescriptor(altIntVector).getMinMethodDescriptor();
    }

    // Method descriptor getters from altreal vectors:

    public static AltrepMethodDescriptor getEltMethodDescriptor(RDoubleVector altRealVector) {
        return getAltRealDescriptor(altRealVector).getEltMethodDescriptor();
    }

    public static AltrepMethodDescriptor getIsSortedMethodDescriptor(RDoubleVector altRealVector) {
        return getAltRealDescriptor(altRealVector).getIsSortedMethodDescriptor();
    }

    public static AltrepMethodDescriptor getNoNAMethodDescriptor(RDoubleVector altRealVector) {
        return getAltRealDescriptor(altRealVector).getNoNAMethodDescriptor();
    }

    public static AltrepMethodDescriptor getSumMethodDescriptor(RDoubleVector altRealVector) {
        return getAltRealDescriptor(altRealVector).getSumMethodDescriptor();
    }

    public static AltrepMethodDescriptor getMaxMethodDescriptor(RDoubleVector altRealVector) {
        return getAltRealDescriptor(altRealVector).getMaxMethodDescriptor();
    }

    public static AltrepMethodDescriptor getMinMethodDescriptor(RDoubleVector altRealVector) {
        return getAltRealDescriptor(altRealVector).getMinMethodDescriptor();
    }

    public static AltrepMethodDescriptor getGetRegionMethodDescriptor(RDoubleVector altRealVector) {
        return getAltRealDescriptor(altRealVector).getGetRegionMethodDescriptor();
    }

    // Method descriptor getters from altlogical vectors:

    public static AltrepMethodDescriptor getEltMethodDescriptor(RLogicalVector altLogicalVector) {
        return getAltLogicalDescriptor(altLogicalVector).getEltMethodDescriptor();
    }

    public static AltrepMethodDescriptor getNoNAMethodDescriptor(RLogicalVector altLogicalVector) {
        return getAltLogicalDescriptor(altLogicalVector).getNoNAMethodDescriptor();
    }

    public static AltrepMethodDescriptor getSumMethodDescriptor(RLogicalVector altLogicalVector) {
        return getAltLogicalDescriptor(altLogicalVector).getSumMethodDescriptor();
    }

    public static AltrepMethodDescriptor getGetRegionMethodDescriptor(RLogicalVector altLogicalVector) {
        return getAltLogicalDescriptor(altLogicalVector).getGetRegionMethodDescriptor();
    }

    // Method descriptor getter for altcomplex vectors:

    public static AltrepMethodDescriptor getEltMethodDescriptor(RComplexVector altComplexVector) {
        return getAltComplexDescriptor(altComplexVector).getEltMethodDescriptor();
    }

    public static AltrepMethodDescriptor getGetRegionMethodDescriptor(RComplexVector altComplexVector) {
        return getAltComplexDescriptor(altComplexVector).getGetRegionMethodDescriptor();
    }

    // Method descriptor getter for altraw vectors:

    public static AltrepMethodDescriptor getEltMethodDescriptor(RRawVector altRawVector) {
        return getAltRawDescriptor(altRawVector).getEltMethodDescriptor();
    }

    public static AltrepMethodDescriptor getGetRegionMethodDescriptor(RRawVector altRawVector) {
        return getAltRawDescriptor(altRawVector).getGetRegionMethodDescriptor();
    }

    // Method descriptor getters from altstring vectors:

    public static AltrepMethodDescriptor getEltMethodDescriptor(RStringVector altStringVector) {
        return getAltStringDescriptor(altStringVector).getEltMethodDescriptor();
    }

    public static AltrepMethodDescriptor getSetEltMethodDescriptor(RStringVector altStringVector) {
        return getAltStringDescriptor(altStringVector).getSetEltMethodDescriptor();
    }

    public static AltrepMethodDescriptor getLengthMethodDescriptor(RStringVector altStringVector) {
        return getAltStringDescriptor(altStringVector).getLengthMethodDescriptor();
    }

    public static AltrepMethodDescriptor getIsSortedMethodDescriptor(RStringVector altStringVector) {
        return getAltStringDescriptor(altStringVector).getIsSortedMethodDescriptor();
    }

    public static AltrepMethodDescriptor getNoNAMethodDescriptor(RStringVector altStringVector) {
        return getAltStringDescriptor(altStringVector).getNoNAMethodDescriptor();
    }


    public static boolean hasCoerceMethodRegistered(Object object) {
        if (!isAltrep(object)) {
            return false;
        }
        if (object instanceof RIntVector) {
            return getAltIntDescriptor((RIntVector) object).isCoerceMethodRegistered();
        } else if (object instanceof RDoubleVector) {
            return getAltRealDescriptor((RDoubleVector) object).isCoerceMethodRegistered();
        } else if (object instanceof RLogicalVector) {
            return getAltLogicalDescriptor((RLogicalVector) object).isCoerceMethodRegistered();
        } else {
            throw RInternalError.shouldNotReachHere("Unexpected altrep type");
        }
    }

    public static boolean hasDuplicateMethodRegistered(Object object) {
        if (!isAltrep(object)) {
            return false;
        }
        if (object instanceof RIntVector) {
            return getAltIntDescriptor((RIntVector) object).isDuplicateMethodRegistered();
        } else if (object instanceof RDoubleVector) {
            return getAltRealDescriptor((RDoubleVector) object).isDuplicateMethodRegistered();
        } else if (object instanceof RLogicalVector) {
            return getAltLogicalDescriptor((RLogicalVector) object).isDuplicateMethodRegistered();
        } else {
            throw RInternalError.shouldNotReachHere("Unexpected altrep type");
        }
    }

    public static boolean hasEltMethodRegistered(Object object) {
        if (!isAltrep(object)) {
            return false;
        }
        if (object instanceof RIntVector) {
            return getAltIntDescriptor((RIntVector) object).isEltMethodRegistered();
        } else if (object instanceof RDoubleVector) {
            return getAltRealDescriptor((RDoubleVector) object).isEltMethodRegistered();
        } else if (object instanceof RLogicalVector) {
            return getAltLogicalDescriptor((RLogicalVector) object).isEltMethodRegistered();
        } else {
            throw RInternalError.shouldNotReachHere("Unexpected altrep type");
        }
    }

    public static boolean hasMaxMethodRegistered(Object object) {
        if (!isAltrep(object)) {
            return false;
        }
        if (object instanceof RIntVector) {
            return getAltIntDescriptor((RIntVector) object).isMaxMethodRegistered();
        } else if (object instanceof RDoubleVector) {
            return getAltRealDescriptor((RDoubleVector) object).isMaxMethodRegistered();
        } else {
            throw RInternalError.shouldNotReachHere("Unexpected altrep type");
        }
    }

    public static boolean hasMinMethodRegistered(Object object) {
        if (!isAltrep(object)) {
            return false;
        }
        if (object instanceof RIntVector) {
            return getAltIntDescriptor((RIntVector) object).isMinMethodRegistered();
        } else if (object instanceof RDoubleVector) {
            return getAltRealDescriptor((RDoubleVector) object).isMinMethodRegistered();
        } else {
            throw RInternalError.shouldNotReachHere("Unexpected altrep type");
        }
    }

    public static boolean hasSumMethodRegistered(Object object) {
        if (!isAltrep(object)) {
            return false;
        }
        if (object instanceof RIntVector) {
            return getAltIntDescriptor((RIntVector) object).isSumMethodRegistered();
        } else if (object instanceof RDoubleVector) {
            return getAltRealDescriptor((RDoubleVector) object).isSumMethodRegistered();
        } else if (object instanceof RLogicalVector) {
            return getAltLogicalDescriptor((RLogicalVector) object).isSumMethodMethodRegistered();
        } else {
            throw RInternalError.shouldNotReachHere("Unexpected altrep type");
        }
    }

    public static boolean hasGetRegionMethodRegistered(Object object) {
        if (!isAltrep(object)) {
            return false;
        }
        if (object instanceof RIntVector) {
            return getAltIntDescriptor((RIntVector) object).isGetRegionMethodRegistered();
        } else if (object instanceof RDoubleVector) {
            return getAltRealDescriptor((RDoubleVector) object).isGetRegionMethodRegistered();
        } else if (object instanceof RLogicalVector) {
            return getAltLogicalDescriptor((RLogicalVector) object).isGetRegionMethodRegistered();
        } else {
            throw RInternalError.shouldNotReachHere("Unexpected altrep type");
        }
    }

    public static boolean hasSortedMethodRegistered(Object object) {
        if (!isAltrep(object)) {
            return false;
        }
        if (object instanceof RIntVector) {
            return getAltIntDescriptor((RIntVector) object).isIsSortedMethodRegistered();
        } else if (object instanceof RDoubleVector) {
            return getAltRealDescriptor((RDoubleVector) object).isIsSortedMethodRegistered();
        } else if (object instanceof RLogicalVector) {
            return getAltLogicalDescriptor((RLogicalVector) object).isIsSortedMethodRegistered();
        } else {
            throw RInternalError.shouldNotReachHere("Unexpected altrep type");
        }
    }

    public static boolean hasNoNAMethodRegistered(Object object) {
        if (!isAltrep(object)) {
            return false;
        }
        if (object instanceof RIntVector) {
            return getAltIntDescriptor((RIntVector) object).isNoNAMethodRegistered();
        } else if (object instanceof RDoubleVector) {
            return getAltRealDescriptor((RDoubleVector) object).isNoNAMethodRegistered();
        } else if (object instanceof RLogicalVector) {
            return getAltLogicalDescriptor((RLogicalVector) object).isNoNAMethodRegistered();
        } else {
            throw RInternalError.shouldNotReachHere("Unexpected altrep type");
        }
    }

    public static int getLengthUncached(RAbstractVector altrepVec) {
        assert altrepVec.isAltRep();
        AltrepRFFI.LengthNode lengthNode = AltrepRFFIFactory.LengthNodeGen.getUncached();
        return lengthNode.execute(altrepVec);
    }

    public abstract static class AltrepSumMethodInvokerNode extends Node {
        public abstract Object execute(Object altrepVector, boolean naRm);

        public static AltrepSumMethodInvokerNode create() {
            return AltrepUtilitiesFactory.AltrepSumMethodInvokerNodeGen.create();
        }

        @Specialization
        Object doAltInt(RIntVector altIntVec, boolean naRm,
                        @Cached AltrepRFFI.SumNode sumNode) {
            return sumNode.execute(altIntVec, naRm);
        }

        @Fallback
        Object fallback(Object vector, @SuppressWarnings("unused") boolean naRm) {
            throw RInternalError.shouldNotReachHere("AltrepSumMethodInvoker: Unknown type of vector: " + vector.toString());
        }
    }

    public abstract static class AltrepMaxMethodInvokerNode extends Node {
        public abstract Object execute(Object altrepVector, boolean naRm);

        public static AltrepMaxMethodInvokerNode create() {
            return AltrepUtilitiesFactory.AltrepMaxMethodInvokerNodeGen.create();
        }

        @Specialization
        Object doAltInt(RIntVector altIntVec, boolean naRm,
                        @Cached AltrepRFFI.MaxNode maxNode) {
            return maxNode.execute(altIntVec, naRm);
        }

        @Fallback
        Object fallback(Object vector, @SuppressWarnings("unused") boolean naRm) {
            throw RInternalError.shouldNotReachHere("AltrepMaxMethodInvoker: Unknown type of vector: " + vector.toString());
        }
    }

    public abstract static class AltrepMinMethodInvokerNode extends Node {
        public abstract Object execute(Object altrepVector, boolean naRm);

        public static AltrepMinMethodInvokerNode create() {
            return AltrepUtilitiesFactory.AltrepMinMethodInvokerNodeGen.create();
        }

        @Specialization
        Object doAltInt(RIntVector altIntVec, boolean naRm,
                        @Cached AltrepRFFI.MinNode minNode) {
            return minNode.execute(altIntVec, naRm);
        }

        @Fallback
        Object fallback(Object vector, @SuppressWarnings("unused") boolean naRm) {
            throw RInternalError.shouldNotReachHere("AltrepMinMethodInvokerNode: Unknown type of vector: " + vector.toString());
        }
    }
}
