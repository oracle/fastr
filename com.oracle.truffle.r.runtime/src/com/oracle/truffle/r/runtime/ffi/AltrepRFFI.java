package com.oracle.truffle.r.runtime.ffi;

import com.oracle.truffle.api.TruffleLogger;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RLogger;
import com.oracle.truffle.r.runtime.data.CharSXPWrapper;
import com.oracle.truffle.r.runtime.data.RAltIntVectorData;
import com.oracle.truffle.r.runtime.data.RBaseObject;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.altrep.AltIntegerClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.AltRepClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.AltStringClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.AltVecClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.AltrepMethodDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.AltrepSortedness;
import com.oracle.truffle.r.runtime.data.altrep.AltrepUtilities;
import com.oracle.truffle.r.runtime.data.model.RAbstractAtomicVector;
import com.oracle.truffle.r.runtime.nodes.altrep.AltrepDownCallNode;

public final class AltrepRFFI {
    private final AltrepDownCallNodeFactory downCallNodeFactory;

    public AltrepRFFI(AltrepDownCallNodeFactory downCallNodeFactory) {
        this.downCallNodeFactory = downCallNodeFactory;
    }

    public static AltrepDownCallNode createDownCallNode() {
        return RFFIFactory.getAltrepRFFI().downCallNodeFactory.createDownCallNode();
    }

    public static AltrepDownCallNode createUncachedDownCallNode() {
        return RFFIFactory.getAltrepRFFI().downCallNodeFactory.getUncached();
    }

    private static boolean expectBoolean(InteropLibrary interop, Object value) {
        RInternalError.guarantee(interop.isBoolean(value), "Value from downcall should be boolean");
        try {
            return interop.asBoolean(value);
        } catch (UnsupportedMessageException e) {
            throw RInternalError.shouldNotReachHere(e);
        }
    }

    private static int expectInteger(InteropLibrary interop, Object value) {
        RInternalError.guarantee(interop.isNumber(value), "Value from downcall should be an integer");
        try {
            return interop.asInt(value);
        } catch (UnsupportedMessageException e) {
            throw RInternalError.shouldNotReachHere(e);
        }
    }

    private static String expectString(InteropLibrary interop, Object value) {
        RInternalError.guarantee(interop.isString(value), "Value from downcall should be a string");
        try {
            return interop.asString(value);
        } catch (UnsupportedMessageException e) {
            throw RInternalError.shouldNotReachHere(e);
        }
    }

    private static long expectPointer(InteropLibrary interop, Object value) {
        if (!interop.isPointer(value)) {
            interop.toNative(value);
        }
        try {
            return interop.asPointer(value);
        } catch (UnsupportedMessageException e) {
            throw RInternalError.shouldNotReachHere(e);
        }
    }


    @GenerateUncached
    @ImportStatic({AltrepUtilities.class, AltRepClassDescriptor.class})
    public abstract static class LengthNode extends Node {
        public abstract int execute(Object altrepVector);

        @Specialization(guards = "classDescriptor == getAltRepClassDescriptor(altrepVector)",
                        assumptions = "getNoMethodRedefinedAssumption()")
        protected int lengthOfAltrep(RAbstractAtomicVector altrepVector,
                                     @Cached AltrepDownCallNode downCallNode,
                                     @CachedLibrary(limit = "1") InteropLibrary returnValueInterop,
                                     @Cached("getAltRepClassDescriptor(altrepVector)") @SuppressWarnings("unused") AltRepClassDescriptor classDescriptor,
                                     @Cached("classDescriptor.getLengthMethodDescriptor()") AltrepMethodDescriptor lengthMethod) {
            Object retVal = downCallNode.execute(lengthMethod, AltRepClassDescriptor.lengthMethodUnwrapResult,
                    AltRepClassDescriptor.lengthMethodWrapArguments, new Object[]{altrepVector});
            return expectInteger(returnValueInterop, retVal);
        }

        @Specialization(replaces = "lengthOfAltrep")
        protected int lengthOfAltrepUncached(RAbstractAtomicVector altrepVector,
                                             @Cached AltrepDownCallNode downCallNode,
                                             @CachedLibrary(limit = "1") InteropLibrary returnValueInterop) {
            AltRepClassDescriptor classDescriptor = AltrepUtilities.getAltRepClassDescriptor(altrepVector);
            AltrepMethodDescriptor lengthMethod = classDescriptor.getLengthMethodDescriptor();
            return lengthOfAltrep(altrepVector, downCallNode, returnValueInterop, classDescriptor, lengthMethod);
        }
    }

    @GenerateUncached
    @ImportStatic({AltrepUtilities.class, AltRepClassDescriptor.class})
    public abstract static class EltNode extends Node {
        public abstract Object execute(Object altrepVector, int index);

        @Specialization(guards = "classDescriptor == getAltIntDescriptor(altIntVector)",
                        assumptions = "getNoMethodRedefinedAssumption()")
        protected int eltOfAltInt(RIntVector altIntVector, int index,
                    @Cached AltrepDownCallNode downCallNode,
                    @CachedLibrary(limit = "1") InteropLibrary retValueInterop,
                    @Cached("getAltIntDescriptor(altIntVector)") @SuppressWarnings("unused") AltIntegerClassDescriptor classDescriptor,
                    @Cached("classDescriptor.getEltMethodDescriptor()") AltrepMethodDescriptor eltMethod) {
            Object retValue = downCallNode.execute(eltMethod, AltIntegerClassDescriptor.eltMethodUnwrapResult,
                    AltIntegerClassDescriptor.eltMethodWrapArguments, new Object[]{altIntVector, index});
            return expectInteger(retValueInterop, retValue);
        }

        @Specialization(replaces = "eltOfAltInt")
        protected int eltOfAltIntUncached(RIntVector altIntVector, int index,
                @CachedLibrary(limit = "1") InteropLibrary retValueInterop,
                @Cached AltrepDownCallNode downCallNode) {
            return eltOfAltInt(altIntVector, index, downCallNode, retValueInterop,
                   AltrepUtilities.getAltIntDescriptor(altIntVector), AltrepUtilities.getEltMethodDescriptor(altIntVector));
        }

        @Specialization
        protected String eltOfAltString(RStringVector altStringVector, int index,
                                        @Cached AltrepDownCallNode downCallNode,
                                        @CachedLibrary(limit = "1") InteropLibrary retValueInterop) {
            AltrepMethodDescriptor methodDescr = AltrepUtilities.getEltMethodDescriptor(altStringVector);
            Object retValue = downCallNode.execute(methodDescr, AltStringClassDescriptor.eltMethodUnwrapResult,
                    AltStringClassDescriptor.eltMethodWrapArguments, new Object[]{altStringVector, index});
            return expectString(retValueInterop, retValue);
        }
    }

    @GenerateUncached
    public abstract static class SetEltNode extends Node {
        public abstract void execute(RStringVector altStringVector, int index, Object element);

        @Specialization
        protected void setEltWithCharSXPWrapper(RStringVector altStringVector, int index, CharSXPWrapper element,
                                                @Shared("downCallNode") @Cached AltrepDownCallNode downCallNode) {
            AltrepMethodDescriptor methodDescr = AltrepUtilities.getSetEltMethodDescriptor(altStringVector);
            downCallNode.execute(methodDescr, AltStringClassDescriptor.setEltMethodUnwrapResult,
                    AltStringClassDescriptor.setEltMethodWrapArguments, new Object[]{altStringVector, index, element});
        }

        @Specialization
        protected void setEltWithString(RStringVector altStringVector, int index, String element,
                                        @Shared("downCallNode") @Cached AltrepDownCallNode downCallNode) {
            CharSXPWrapper charWrapper = CharSXPWrapper.create(element);
            setEltWithCharSXPWrapper(altStringVector, index, charWrapper, downCallNode);
        }
    }

    @ImportStatic({AltrepUtilities.class, AltRepClassDescriptor.class})
    public abstract static class DataptrNode extends Node {
        public abstract long execute(Object altrepVector, boolean writeable);

        private static final TruffleLogger altrepLogger = RLogger.getLogger(RLogger.LOGGER_ALTREP);

        public static DataptrNode create() {
            return AltrepRFFIFactory.DataptrNodeGen.create();
        }

        public static DataptrNode getUncached() {
            return new DataptrNode() {
                @Override
                public long execute(Object altrepVector, boolean writeable) {
                    AltrepDownCallNode downCallNode = AltrepDownCallNode.getUncached();
                    InteropLibrary retValueInterop = InteropLibrary.getUncached();

                    AltVecClassDescriptor classDescriptor = AltrepUtilities.getAltVecClassDescriptor((RBaseObject) altrepVector);
                    AltrepMethodDescriptor dataptrMethodDescriptor = classDescriptor.getDataptrMethodDescriptor();
                    Object ret = downCallNode.execute(dataptrMethodDescriptor, AltVecClassDescriptor.dataptrMethodUnwrapResult,
                            AltVecClassDescriptor.dataptrMethodWrapArguments, new Object[]{altrepVector, writeable});
                    return expectPointer(retValueInterop, ret);
                }
            };
        }

        @Specialization(guards = "altIntVector == cachedAltIntVector",
                        assumptions = "getNoMethodRedefinedAssumption()",
                        limit = "4")
        protected long dataptrOfAltInt(RIntVector altIntVector, @SuppressWarnings("unused") boolean writeable,
                @Cached @SuppressWarnings("unused") AltrepDownCallNode downCallNode,
                @CachedLibrary(limit = "1") @SuppressWarnings("unused") InteropLibrary interop,
                @Cached("altIntVector") @SuppressWarnings("unused") RIntVector cachedAltIntVector,
                @Cached("getAltIntDescriptor(altIntVector)") @SuppressWarnings("unused") AltIntegerClassDescriptor classDescriptor,
                @Cached("getAltIntVectorData(altIntVector)") RAltIntVectorData altIntVecData,
                @Cached("classDescriptor.getDataptrMethodDescriptor()") @SuppressWarnings("unused") AltrepMethodDescriptor dataptrMethod,
                @Cached("getDataptrForAltInt(altIntVector, writeable, dataptrMethod, interop, downCallNode)") long cachedDataptrAddr) {
            assert AltrepUtilities.isAltrep(altIntVector);
            altIntVecData.setDataptrCalled();
            altrepLogger.fine(() -> String.format("DataptrNode(cached): returning dataptrAddr=%d of %s",
                    cachedDataptrAddr, altIntVector.getData()));
            return cachedDataptrAddr;
        }

        protected static long getDataptrForAltInt(RIntVector altIntVector, boolean writeable, AltrepMethodDescriptor dataptrMethod,
                InteropLibrary pointerInterop, AltrepDownCallNode downCallNode) {
            Object ret = downCallNode.execute(dataptrMethod, AltIntegerClassDescriptor.dataptrMethodUnwrapResult,
                    AltIntegerClassDescriptor.dataptrMethodWrapArguments, new Object[]{altIntVector, writeable});
            long dataptrAddr = expectPointer(pointerInterop, ret);
            altrepLogger.fine(() -> "DataptrNode: caching dataptrAddr=" + dataptrAddr + " for " + altIntVector.getData());
            return dataptrAddr;
        }

        @Specialization(replaces = "dataptrOfAltInt")
        protected long dataptrOfAltIntUncached(RIntVector altIntVector, boolean writeable,
                @Cached AltrepDownCallNode downCallNode,
                @CachedLibrary(limit = "1") InteropLibrary retValInterop) {
            RAltIntVectorData vectorData = AltrepUtilities.getAltIntVectorData(altIntVector);
            AltIntegerClassDescriptor descriptor =  vectorData.getDescriptor();
            AltrepMethodDescriptor dataptrMethod = descriptor.getDataptrMethodDescriptor();
            Object ret = downCallNode.execute(dataptrMethod, AltIntegerClassDescriptor.dataptrMethodUnwrapResult,
                    AltIntegerClassDescriptor.dataptrMethodWrapArguments, new Object[]{altIntVector, writeable});
            assert altIntVector.getData() instanceof RAltIntVectorData;
            vectorData.setDataptrCalled();
            long dataptr = expectPointer(retValInterop, ret);
            altrepLogger.fine(() -> String.format("DataptrNode(uncached): returning dataptrAddr=%d of %s",
                    dataptr, altIntVector.getData()));
            return dataptr;
        }
    }

    @GenerateUncached
    public abstract static class DataptrOrNullNode extends Node {
        public abstract Object execute(RAbstractAtomicVector altrepVector);

        @Specialization
        protected Object doIt(RAbstractAtomicVector altVec,
                              @CachedLibrary(limit = "1") InteropLibrary interopLib,
                              @Cached AltrepDownCallNode downCallNode) {
            AltVecClassDescriptor classDescriptor = AltrepUtilities.getAltVecClassDescriptor(altVec);
            AltrepMethodDescriptor dataptrOrNullMethod = classDescriptor.getDataptrOrNullMethodDescriptor();
            Object ret = downCallNode.execute(dataptrOrNullMethod, AltVecClassDescriptor.dataptrOrNullMethodUnwrapResult,
                    AltVecClassDescriptor.dataptrOrNullMethodWrapArguments, new Object[] {altVec});
            assert interopLib.isPointer(ret);
            return ret;
        }
    }

    @GenerateUncached
    public abstract static class IsSortedNode extends Node {
        public abstract AltrepSortedness execute(Object altrepVector);

        @Specialization
        protected AltrepSortedness doIt(RIntVector altIntVector,
                                     @Cached AltrepDownCallNode downCallNode,
                                     @CachedLibrary(limit = "1") InteropLibrary retValueInterop) {
            assert AltrepUtilities.isAltrep(altIntVector);
            AltrepMethodDescriptor methodDescr = AltrepUtilities.getIsSortedMethodDescriptor(altIntVector);
            Object retValue = downCallNode.execute(methodDescr, AltIntegerClassDescriptor.isSortedMethodUnwrapResult,
                    AltIntegerClassDescriptor.isSortedMethodWrapArguments, new Object[]{altIntVector});
            return AltrepSortedness.fromInt(expectInteger(retValueInterop, retValue));
        }
    }

    @GenerateUncached
    public abstract static class NoNANode extends Node {
        public abstract boolean execute(Object altrepVector);

        @Specialization
        protected boolean doIt(RIntVector altIntVector,
                            @Cached AltrepDownCallNode downCallNode,
                            @CachedLibrary(limit = "1") InteropLibrary retValueInterop) {
            assert AltrepUtilities.isAltrep(altIntVector);
            AltrepMethodDescriptor methodDescr = AltrepUtilities.getNoNAMethodDescriptor(altIntVector);
            Object retValue = downCallNode.execute(methodDescr, AltIntegerClassDescriptor.noNAMethodUnwrapResult,
                    AltIntegerClassDescriptor.noNAMethodWrapArguments, new Object[]{altIntVector});
            return expectInteger(retValueInterop, retValue) == 1;
        }
    }

    @GenerateUncached
    public abstract static class GetRegionNode extends Node {
        public abstract int execute(Object altrepVector, int fromIdx, int size, Object buffer);

        @Specialization
        protected int doIt(RIntVector altIntVector, int fromIdx, int size, Object buffer,
                        @Cached AltrepDownCallNode downCallNode,
                        @CachedLibrary(limit = "1") InteropLibrary retValueInterop) {
            assert AltrepUtilities.isAltrep(altIntVector);
            assert InteropLibrary.getUncached().hasArrayElements(buffer);
            AltrepMethodDescriptor methodDescr = AltrepUtilities.getGetRegionMethodDescriptor(altIntVector);
            Object ret = downCallNode.execute(methodDescr, AltIntegerClassDescriptor.getRegionMethodUnwrapResult,
                    AltIntegerClassDescriptor.getRegionMethodWrapArguments, new Object[]{altIntVector, fromIdx, size, buffer});
            return expectInteger(retValueInterop, ret);
        }
    }

    @GenerateUncached
    public abstract static class DuplicateNode extends Node {
        public abstract Object execute(Object altrepVector, boolean deep);

        @Specialization
        protected Object doIt(RIntVector altIntVector, boolean deep,
                           @Cached AltrepDownCallNode downCallNode) {
            assert AltrepUtilities.isAltrep(altIntVector);
            AltrepMethodDescriptor methodDescriptor = AltrepUtilities.getDuplicateMethodDescriptor(altIntVector);
            return downCallNode.execute(methodDescriptor, AltIntegerClassDescriptor.duplicateMethodUnwrapResult,
                    AltIntegerClassDescriptor.duplicateMethodWrapArguments, new Object[]{altIntVector, deep});
        }
    }

    @GenerateUncached
    public abstract static class SumNode extends Node {
        public abstract Object execute(Object altrepVector, boolean naRm);

        @Specialization
        protected int doIt(RIntVector altIntVec, boolean naRm,
                           @Cached AltrepDownCallNode downCallNode,
                           @CachedLibrary(limit = "1") InteropLibrary interopLib) {
            AltrepMethodDescriptor altrepMethodDescriptor = AltrepUtilities.getSumMethodDescriptor(altIntVec);
            Object ret = downCallNode.execute(altrepMethodDescriptor, AltIntegerClassDescriptor.sumMethodUnwrapResult,
                    AltIntegerClassDescriptor.sumMethodWrapArguments, new Object[]{altIntVec, naRm});
            return expectInteger(interopLib, ret);
        }
    }

    @GenerateUncached
    public abstract static class MaxNode extends Node {
        public abstract Object execute(Object altrepVector, boolean naRm);

        @Specialization
        protected Object doIt(RIntVector altIntVec, boolean naRm,
                           @Cached AltrepDownCallNode downCallNode) {
            AltrepMethodDescriptor altrepMethodDescriptor = AltrepUtilities.getMaxMethodDescriptor(altIntVec);
            return downCallNode.execute(altrepMethodDescriptor, AltIntegerClassDescriptor.maxMethodUnwrapResult,
                    AltIntegerClassDescriptor.maxMethodWrapArguments, new Object[]{altIntVec, naRm});
        }
    }

    @GenerateUncached
    public abstract static class MinNode extends Node {
        public abstract Object execute(Object altrepVector, boolean naRm);

        @Specialization
        protected Object doIt(RIntVector altIntVec, boolean naRm,
                              @Cached AltrepDownCallNode downCallNode) {
            AltrepMethodDescriptor altrepMethodDescriptor = AltrepUtilities.getMinMethodDescriptor(altIntVec);
            return downCallNode.execute(altrepMethodDescriptor, AltIntegerClassDescriptor.minMethodUnwrapResult,
                    AltIntegerClassDescriptor.minMethodWrapArguments, new Object[]{altIntVec, naRm});
        }
    }
}
