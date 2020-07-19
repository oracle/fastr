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
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.CharSXPWrapper;
import com.oracle.truffle.r.runtime.data.RAltrepVectorData;
import com.oracle.truffle.r.runtime.data.RBaseObject;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.altrep.AltComplexClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.AltIntegerClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.AltLogicalClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.AltRawClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.AltRealClassDescriptor;
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

    private static int expectInteger(InteropLibrary interop, Object value) {
        RInternalError.guarantee(interop.isNumber(value), "Value from downcall should be an integer");
        try {
            return interop.asInt(value);
        } catch (UnsupportedMessageException e) {
            throw RInternalError.shouldNotReachHere(e);
        }
    }

    private static byte expectByte(InteropLibrary interop, Object value) {
        RInternalError.guarantee(interop.isNumber(value), "Value from downcall should be byte");
        try {
            return interop.asByte(value);
        } catch (UnsupportedMessageException e) {
            throw RInternalError.shouldNotReachHere(e);
        }
    }

    private static double expectDouble(InteropLibrary interop, Object value) {
        RInternalError.guarantee(interop.isNumber(value), "Value from downcall should be double");
        try {
            return interop.asDouble(value);
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

        @Specialization(guards = "classDescriptor == getAltRealDescriptor(altRealVector)",
                        assumptions = "getNoMethodRedefinedAssumption()")
        protected double eltOfAltReal(RDoubleVector altRealVector, int index,
                                  @Cached AltrepDownCallNode downCallNode,
                                  @CachedLibrary(limit = "1") InteropLibrary retValueInterop,
                                  @Cached("getAltRealDescriptor(altRealVector)") @SuppressWarnings("unused") AltRealClassDescriptor classDescriptor,
                                  @Cached("classDescriptor.getEltMethodDescriptor()") AltrepMethodDescriptor eltMethod) {
            Object retValue = downCallNode.execute(eltMethod, AltRealClassDescriptor.eltMethodUnwrapResult,
                    AltRealClassDescriptor.eltMethodWrapArguments, new Object[]{altRealVector, index});
            return expectDouble(retValueInterop, retValue);
        }

        @Specialization(replaces = "eltOfAltReal")
        protected double eltOfAltRealUncached(RDoubleVector altRealVector, int index,
                                          @CachedLibrary(limit = "1") InteropLibrary retValueInterop,
                                          @Cached AltrepDownCallNode downCallNode) {
            return eltOfAltReal(altRealVector, index, downCallNode, retValueInterop,
                    AltrepUtilities.getAltRealDescriptor(altRealVector), AltrepUtilities.getEltMethodDescriptor(altRealVector));
        }

        @Specialization(guards = "classDescriptor == getAltLogicalDescriptor(altLogicalVector)",
                        assumptions = "getNoMethodRedefinedAssumption()")
        protected byte eltOfAltLogical(RLogicalVector altLogicalVector, int index,
                                         @Cached AltrepDownCallNode downCallNode,
                                         @CachedLibrary(limit = "1") InteropLibrary retValueInterop,
                                         @Cached("getAltLogicalDescriptor(altLogicalVector)") @SuppressWarnings("unused") AltLogicalClassDescriptor classDescriptor,
                                         @Cached("classDescriptor.getEltMethodDescriptor()") AltrepMethodDescriptor eltMethod) {
            Object retValue = downCallNode.execute(eltMethod, AltLogicalClassDescriptor.eltMethodUnwrapResult,
                    AltLogicalClassDescriptor.eltMethodWrapArguments, new Object[]{altLogicalVector, index});
            int intValue = expectInteger(retValueInterop, retValue);
            return RRuntime.int2logical(intValue);
        }

        @Specialization(replaces = "eltOfAltLogical")
        protected byte eltOfAltLogicalUncached(RLogicalVector altLogicalVector, int index,
                                              @CachedLibrary(limit = "1") InteropLibrary retValueInterop,
                                              @Cached AltrepDownCallNode downCallNode) {
            return eltOfAltLogical(altLogicalVector, index, downCallNode, retValueInterop,
                    AltrepUtilities.getAltLogicalDescriptor(altLogicalVector), AltrepUtilities.getEltMethodDescriptor(altLogicalVector));
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

        @Specialization
        protected RComplex eltOfAltComplex(RComplexVector altComplexVector, int index,
                                           @Cached AltrepDownCallNode downCallNode) {
            AltrepMethodDescriptor methodDescr = AltrepUtilities.getEltMethodDescriptor(altComplexVector);
            Object retValue = downCallNode.execute(methodDescr, AltComplexClassDescriptor.eltMethodUnwrapResult,
                    AltComplexClassDescriptor.eltMethodWrapArguments, new Object[]{altComplexVector, index});
            assert retValue instanceof RComplex;
            return (RComplex) retValue;
        }

        @Specialization
        protected byte eltOfAltRaw(RRawVector altRawVector, int index,
                                        @Cached AltrepDownCallNode downCallNode,
                                        @CachedLibrary(limit = "1") InteropLibrary retValueInterop) {
            AltrepMethodDescriptor methodDescr = AltrepUtilities.getEltMethodDescriptor(altRawVector);
            Object retValue = downCallNode.execute(methodDescr, AltRawClassDescriptor.eltMethodUnwrapResult,
                    AltRawClassDescriptor.eltMethodWrapArguments, new Object[]{altRawVector, index});
            return expectByte(retValueInterop, retValue);
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

        @Specialization(guards = {"altrepVector == cachedAltrepVector"},
                        assumptions = "getNoMethodRedefinedAssumption()",
                        limit = "4")
        protected long dataptrOfAltrep(RAbstractAtomicVector altrepVector, @SuppressWarnings("unused") boolean writeable,
                       @Cached @SuppressWarnings("unused") AltrepDownCallNode downCallNode,
                       @CachedLibrary(limit = "1") @SuppressWarnings("unused") InteropLibrary interop,
                       @Cached("altrepVector") @SuppressWarnings("unused") RAbstractAtomicVector cachedAltrepVector,
                       @Cached("getAltVecClassDescriptor(altrepVector)") @SuppressWarnings("unused") AltVecClassDescriptor classDescriptor,
                       @Cached("getAltRepVectorData(altrepVector)") RAltrepVectorData altRepVectorData,
                       @Cached("classDescriptor.getDataptrMethodDescriptor()") @SuppressWarnings("unused") AltrepMethodDescriptor dataptrMethod,
                       @Cached("getDataptrForAltRep(altrepVector, writeable, dataptrMethod, interop, downCallNode)") long cachedDataptrAddr) {
            assert altrepVector.isAltRep();
            altRepVectorData.setDataptrCalled();
            altrepLogger.fine(() -> String.format("DataptrNode(cached): returning dataptrAddr=%d of %s",
                    cachedDataptrAddr, altrepVector.getData()));
            return cachedDataptrAddr;
        }

        protected static long getDataptrForAltRep(RAbstractAtomicVector altrepVector, boolean writeable, AltrepMethodDescriptor dataptrMethod,
                                                  InteropLibrary pointerInterop, AltrepDownCallNode downCallNode) {
            Object ret = downCallNode.execute(dataptrMethod, AltVecClassDescriptor.dataptrMethodUnwrapResult,
                    AltVecClassDescriptor.dataptrMethodWrapArguments, new Object[]{altrepVector, writeable});
            long dataptrAddr = expectPointer(pointerInterop, ret);
            altrepLogger.fine(() -> "DataptrNode: caching dataptrAddr=" + dataptrAddr + " for " + altrepVector.getData());
            return dataptrAddr;
        }

        @Specialization(replaces = "dataptrOfAltrep")
        protected long dataptrOfAltrepUncached(RAbstractAtomicVector altrepVector, boolean writeable,
                           @Cached AltrepDownCallNode downCallNode,
                           @CachedLibrary(limit = "1") InteropLibrary retValInterop) {
            RAltrepVectorData altrepVectorData = AltrepUtilities.getAltRepVectorData(altrepVector);
            AltVecClassDescriptor classDescriptor = AltrepUtilities.getAltVecClassDescriptor(altrepVector);
            AltrepMethodDescriptor dataptrMethod = classDescriptor.getDataptrMethodDescriptor();
            Object ret = downCallNode.execute(dataptrMethod, AltVecClassDescriptor.dataptrMethodUnwrapResult,
                    AltVecClassDescriptor.dataptrMethodWrapArguments, new Object[]{altrepVector, writeable});
            altrepVectorData.setDataptrCalled();
            long dataptr = expectPointer(retValInterop, ret);
            altrepLogger.fine(() -> String.format("DataptrNode(uncached): returning dataptrAddr=%d of %s",
                    dataptr, altrepVectorData));
            return dataptr;
        }
    }

    @GenerateUncached
    public abstract static class DataptrOrNullNode extends Node {
        public abstract Object execute(RAbstractAtomicVector altrepVector);

        @Specialization
        protected Object dataptrOrNullUncached(RAbstractAtomicVector altVec,
                              @CachedLibrary(limit = "1") InteropLibrary interopLib,
                              @Cached AltrepDownCallNode downCallNode) {
            AltVecClassDescriptor classDescriptor = AltrepUtilities.getAltVecClassDescriptor(altVec);
            AltrepMethodDescriptor dataptrOrNullMethod = classDescriptor.getDataptrOrNullMethodDescriptor();
            Object ret = downCallNode.execute(dataptrOrNullMethod, AltVecClassDescriptor.dataptrOrNullMethodUnwrapResult,
                    AltVecClassDescriptor.dataptrOrNullMethodWrapArguments, new Object[] {altVec});
            return expectPointer(interopLib, ret);
        }
    }

    @GenerateUncached
    @ImportStatic({AltrepUtilities.class, AltRepClassDescriptor.class})
    public abstract static class IsSortedNode extends Node {
        public abstract AltrepSortedness execute(Object altrepVector);

        // Alt integers:

        @Specialization(guards = "altIntClassDescriptor == getAltIntDescriptor(altIntVector)",
                        assumptions = "getNoMethodRedefinedAssumption()")
        protected AltrepSortedness isAltIntSorted(RIntVector altIntVector,
                          @Cached AltrepDownCallNode downCallNode,
                          @CachedLibrary(limit = "1") InteropLibrary retValueInterop,
                          @Cached("getAltIntDescriptor(altIntVector)") @SuppressWarnings("unused") AltIntegerClassDescriptor altIntClassDescriptor,
                          @Cached("altIntClassDescriptor.getIsSortedMethodDescriptor()") AltrepMethodDescriptor isSortedMethod) {
            Object retValue = downCallNode.execute(isSortedMethod, AltIntegerClassDescriptor.isSortedMethodUnwrapResult,
                    AltIntegerClassDescriptor.isSortedMethodWrapArguments, new Object[]{altIntVector});
            return AltrepSortedness.fromInt(expectInteger(retValueInterop, retValue));
        }

        @Specialization(replaces = "isAltIntSorted")
        protected AltrepSortedness isAltIntSortedUncached(RIntVector altIntVector,
                          @Cached AltrepDownCallNode downCallNode,
                          @CachedLibrary(limit = "1") InteropLibrary retValueInterop) {
            return isAltIntSorted(altIntVector, downCallNode, retValueInterop,
                    AltrepUtilities.getAltIntDescriptor(altIntVector),
                    AltrepUtilities.getIsSortedMethodDescriptor(altIntVector));
        }

        // Alt reals:

        @Specialization(guards = "altRealClassDescriptor == getAltRealDescriptor(altRealVector)",
                        assumptions = "getNoMethodRedefinedAssumption()")
        protected AltrepSortedness isAltRealSorted(RDoubleVector altRealVector,
                          @Cached AltrepDownCallNode downCallNode,
                          @CachedLibrary(limit = "1") InteropLibrary retValueInterop,
                          @Cached("getAltRealDescriptor(altRealVector)") @SuppressWarnings("unused") AltRealClassDescriptor altRealClassDescriptor,
                          @Cached("altRealClassDescriptor.getIsSortedMethodDescriptor()") AltrepMethodDescriptor isSortedMethod) {
            Object retValue = downCallNode.execute(isSortedMethod, AltRealClassDescriptor.isSortedMethodUnwrapResult,
                    AltRealClassDescriptor.isSortedMethodWrapArguments, new Object[]{altRealVector});
            return AltrepSortedness.fromInt(expectInteger(retValueInterop, retValue));
        }

        @Specialization(replaces = "isAltRealSorted")
        protected AltrepSortedness isAltRealSortedUncached(RDoubleVector altRealVector,
                                  @Cached AltrepDownCallNode downCallNode,
                                  @CachedLibrary(limit = "1") InteropLibrary retValueInterop) {
            return isAltRealSorted(altRealVector, downCallNode, retValueInterop,
                    AltrepUtilities.getAltRealDescriptor(altRealVector),
                    AltrepUtilities.getIsSortedMethodDescriptor(altRealVector));
        }

        // Alt logicals:

        @Specialization
        protected AltrepSortedness isAltLogicalSortedUncached(RLogicalVector altLogicalVector,
                              @Cached AltrepDownCallNode downCallNode,
                              @CachedLibrary(limit = "1") InteropLibrary retValueInterop) {
            AltLogicalClassDescriptor altLogicalClassDescriptor = AltrepUtilities.getAltLogicalDescriptor(altLogicalVector);
            AltrepMethodDescriptor isSortedMethod = altLogicalClassDescriptor.getIsSortedMethodDescriptor();
            Object retValue = downCallNode.execute(isSortedMethod, AltLogicalClassDescriptor.isSortedMethodUnwrapResult,
                    AltLogicalClassDescriptor.isSortedMethodWrapArguments, new Object[]{altLogicalVector});
            return AltrepSortedness.fromInt(expectInteger(retValueInterop, retValue));
        }

        // Alt strings:

        @Specialization(guards = "altStringClassDescriptor == getAltStringDescriptor(altStringVector)",
                        assumptions = "getNoMethodRedefinedAssumption()")
        protected AltrepSortedness isAltStringSorted(RStringVector altStringVector,
                                   @Cached AltrepDownCallNode downCallNode,
                                   @CachedLibrary(limit = "1") InteropLibrary retValueInterop,
                                   @Cached("getAltStringDescriptor(altStringVector)") @SuppressWarnings("unused") AltStringClassDescriptor altStringClassDescriptor,
                                   @Cached("altStringClassDescriptor.getIsSortedMethodDescriptor()") AltrepMethodDescriptor isSortedMethod) {
            Object retValue = downCallNode.execute(isSortedMethod, AltStringClassDescriptor.isSortedMethodUnwrapResult,
                    AltStringClassDescriptor.isSortedMethodWrapArguments, new Object[]{altStringVector});
            return AltrepSortedness.fromInt(expectInteger(retValueInterop, retValue));
        }

        @Specialization(replaces = "isAltStringSorted")
        protected AltrepSortedness isAltStringSortedUncached(RStringVector altStringVector,
                                                           @Cached AltrepDownCallNode downCallNode,
                                                           @CachedLibrary(limit = "1") InteropLibrary retValueInterop) {
            return isAltStringSorted(altStringVector, downCallNode, retValueInterop,
                    AltrepUtilities.getAltStringDescriptor(altStringVector),
                    AltrepUtilities.getIsSortedMethodDescriptor(altStringVector));
        }
    }

    @GenerateUncached
    public abstract static class NoNANode extends Node {
        public abstract boolean execute(Object altrepVector);

        @Specialization
        protected boolean altIntNoNA(RIntVector altIntVector,
                            @Cached AltrepDownCallNode downCallNode,
                            @CachedLibrary(limit = "1") InteropLibrary retValueInterop) {
            AltrepMethodDescriptor methodDescr = AltrepUtilities.getNoNAMethodDescriptor(altIntVector);
            Object retValue = downCallNode.execute(methodDescr, AltIntegerClassDescriptor.noNAMethodUnwrapResult,
                    AltIntegerClassDescriptor.noNAMethodWrapArguments, new Object[]{altIntVector});
            return expectBoolean(retValueInterop, retValue);
        }

        @Specialization
        protected boolean altRealNoNA(RDoubleVector altRealVector,
                                     @Cached AltrepDownCallNode downCallNode,
                                     @CachedLibrary(limit = "1") InteropLibrary retValueInterop) {
            AltrepMethodDescriptor methodDescr = AltrepUtilities.getNoNAMethodDescriptor(altRealVector);
            Object retValue = downCallNode.execute(methodDescr, AltRealClassDescriptor.noNAMethodUnwrapResult,
                    AltRealClassDescriptor.noNAMethodWrapArguments, new Object[]{altRealVector});
            return expectBoolean(retValueInterop, retValue);
        }

        @Specialization
        protected boolean altLogicalNoNA(RLogicalVector altLogicalVector,
                                      @Cached AltrepDownCallNode downCallNode,
                                      @CachedLibrary(limit = "1") InteropLibrary retValueInterop) {
            AltrepMethodDescriptor methodDescr = AltrepUtilities.getNoNAMethodDescriptor(altLogicalVector);
            Object retValue = downCallNode.execute(methodDescr, AltLogicalClassDescriptor.noNAMethodUnwrapResult,
                    AltLogicalClassDescriptor.noNAMethodWrapArguments, new Object[]{altLogicalVector});
            return expectBoolean(retValueInterop, retValue);
        }

        @Specialization
        protected boolean altStringNoNA(RStringVector altStringVector,
                                         @Cached AltrepDownCallNode downCallNode,
                                         @CachedLibrary(limit = "1") InteropLibrary retValueInterop) {
            AltrepMethodDescriptor methodDescr = AltrepUtilities.getNoNAMethodDescriptor(altStringVector);
            Object retValue = downCallNode.execute(methodDescr, AltStringClassDescriptor.noNAMethodUnwrapResult,
                    AltStringClassDescriptor.noNAMethodWrapArguments, new Object[]{altStringVector});
            return expectBoolean(retValueInterop, retValue);
        }

        private static boolean expectBoolean(InteropLibrary interop, Object value) {
            return expectInteger(interop, value) == 1;
        }
    }

    @GenerateUncached
    @ImportStatic({AltrepUtilities.class, AltRepClassDescriptor.class})
    public abstract static class GetRegionNode extends Node {
        public abstract int execute(Object altrepVector, int fromIdx, int size, Object buffer);

        @Specialization(guards = "classDescriptor == getAltIntDescriptor(altIntVector)",
                        assumptions = "getNoMethodRedefinedAssumption()")
        protected int getRegionAltInt(RIntVector altIntVector, int fromIdx, int size, Object buffer,
                        @Cached AltrepDownCallNode downCallNode,
                        @CachedLibrary(limit = "1") InteropLibrary retValueInterop,
                          @Cached("getAltIntDescriptor(altIntVector)") @SuppressWarnings("unused") AltIntegerClassDescriptor classDescriptor,
                          @Cached("classDescriptor.getGetRegionMethodDescriptor()") AltrepMethodDescriptor getRegionMethod) {
            Object ret = downCallNode.execute(getRegionMethod, AltIntegerClassDescriptor.getRegionMethodUnwrapResult,
                    AltIntegerClassDescriptor.getRegionMethodWrapArguments, new Object[]{altIntVector, fromIdx, size, buffer});
            return expectInteger(retValueInterop, ret);
        }

        @Specialization(replaces = "getRegionAltInt")
        protected int getRegionAltIntUncached(RIntVector altIntVector, int fromIdx, int size, Object buffer,
                                              @Cached AltrepDownCallNode downCallNode,
                                              @CachedLibrary(limit = "1") InteropLibrary retValueInterop) {
            return getRegionAltInt(altIntVector, fromIdx, size, buffer, downCallNode, retValueInterop,
                    AltrepUtilities.getAltIntDescriptor(altIntVector),
                    AltrepUtilities.getGetRegionMethodDescriptor(altIntVector));
        }

        @Specialization(guards = "classDescriptor == getAltRealDescriptor(altRealVector)",
                assumptions = "getNoMethodRedefinedAssumption()")
        protected int getRegionAltReal(RDoubleVector altRealVector, int fromIdx, int size, Object buffer,
                                      @Cached AltrepDownCallNode downCallNode,
                                      @CachedLibrary(limit = "1") InteropLibrary retValueRealerop,
                                      @Cached("getAltRealDescriptor(altRealVector)") @SuppressWarnings("unused") AltRealClassDescriptor classDescriptor,
                                      @Cached("classDescriptor.getGetRegionMethodDescriptor()") AltrepMethodDescriptor getRegionMethod) {
            Object ret = downCallNode.execute(getRegionMethod, AltRealClassDescriptor.getRegionMethodUnwrapResult,
                    AltRealClassDescriptor.getRegionMethodWrapArguments, new Object[]{altRealVector, fromIdx, size, buffer});
            return expectInteger(retValueRealerop, ret);
        }

        @Specialization(replaces = "getRegionAltReal")
        protected int getRegionAltRealUncached(RDoubleVector altRealVector, int fromIdx, int size, Object buffer,
                                              @Cached AltrepDownCallNode downCallNode,
                                              @CachedLibrary(limit = "1") InteropLibrary retValueInterop) {
            return getRegionAltReal(altRealVector, fromIdx, size, buffer, downCallNode, retValueInterop,
                    AltrepUtilities.getAltRealDescriptor(altRealVector),
                    AltrepUtilities.getGetRegionMethodDescriptor(altRealVector));
        }

        @Specialization
        protected int getRegionAltLogicalUncached(RLogicalVector altLogicalVector, int fromIdx, int size, Object buffer,
                                                  @Cached AltrepDownCallNode downCallNode,
                                                  @CachedLibrary(limit = "1") InteropLibrary retValueInterop) {
            AltLogicalClassDescriptor classDescriptor = AltrepUtilities.getAltLogicalDescriptor(altLogicalVector);
            Object ret = downCallNode.execute(classDescriptor.getGetRegionMethodDescriptor(),
                    AltLogicalClassDescriptor.getRegionMethodUnwrapResult,
                    AltLogicalClassDescriptor.getRegionMethodWrapArguments,
                    new Object[]{altLogicalVector, fromIdx, size, buffer});
            return expectInteger(retValueInterop, ret);
        }

        @Specialization
        protected int getRegionAltComplexUncached(RComplexVector altComplexVector, int fromIdx, int size, Object buffer,
                                                  @Cached AltrepDownCallNode downCallNode,
                                                  @CachedLibrary(limit = "1") InteropLibrary retValueInterop) {
            AltComplexClassDescriptor classDescriptor = AltrepUtilities.getAltComplexDescriptor(altComplexVector);
            Object ret = downCallNode.execute(classDescriptor.getGetRegionMethodDescriptor(),
                    AltComplexClassDescriptor.getRegionMethodUnwrapResult,
                    AltComplexClassDescriptor.getRegionMethodWrapArguments,
                    new Object[]{altComplexVector, fromIdx, size, buffer});
            return expectInteger(retValueInterop, ret);
        }

        @Specialization
        protected int getRegionAltRawUncached(RRawVector altRawVector, int fromIdx, int size, Object buffer,
                                              @Cached AltrepDownCallNode downCallNode,
                                              @CachedLibrary(limit = "1") InteropLibrary retValueInterop) {
            AltRawClassDescriptor classDescriptor = AltrepUtilities.getAltRawDescriptor(altRawVector);
            Object ret = downCallNode.execute(classDescriptor.getGetRegionMethodDescriptor(),
                    AltRawClassDescriptor.getRegionMethodUnwrapResult,
                    AltRawClassDescriptor.getRegionMethodWrapArguments,
                    new Object[]{altRawVector, fromIdx, size, buffer});
            return expectInteger(retValueInterop, ret);
        }
    }

    @GenerateUncached
    @ImportStatic({AltrepUtilities.class, AltRepClassDescriptor.class})
    public abstract static class DuplicateNode extends Node {
        public abstract Object execute(Object altrepVector, boolean deep);

        @Specialization(guards = "classDescriptor == getAltRepClassDescriptor(altrepVector)",
                        assumptions = "getNoMethodRedefinedAssumption()")
        protected Object duplicateAltrepVec(RAbstractAtomicVector altrepVector, boolean deep,
                               @Cached AltrepDownCallNode downCallNode,
                               @Cached("getAltRepClassDescriptor(altrepVector)") @SuppressWarnings("unused") AltRepClassDescriptor classDescriptor,
                               @Cached("classDescriptor.getDuplicateMethodDescriptor()") AltrepMethodDescriptor duplicateMethod) {
            return downCallNode.execute(duplicateMethod, AltRepClassDescriptor.duplicateMethodUnwrapResult,
                    AltRepClassDescriptor.duplicateMethodWrapArguments, new Object[]{altrepVector, deep});
        }

        @Specialization(replaces = "duplicateAltrepVec")
        protected Object duplicateAltrepVecUncached(RAbstractAtomicVector altrepVector, boolean deep,
                                                    @Cached AltrepDownCallNode downCallNode) {
            return duplicateAltrepVec(altrepVector, deep, downCallNode, AltrepUtilities.getAltRepClassDescriptor(altrepVector),
                    AltrepUtilities.getDuplicateMethodDescriptor(altrepVector));
        }
    }

    @GenerateUncached
    public abstract static class SumNode extends Node {
        public abstract Object execute(Object altrepVector, boolean naRm);

        @Specialization
        protected Object altIntSum(RIntVector altIntVec, boolean naRm,
                           @Cached AltrepDownCallNode downCallNode) {
            AltrepMethodDescriptor sumMethod = AltrepUtilities.getSumMethodDescriptor(altIntVec);
            return invokeSumMethod(downCallNode, sumMethod, altIntVec, naRm);
        }

        @Specialization
        protected Object altRealSum(RDoubleVector altRealVec, boolean naRm,
                                   @Cached AltrepDownCallNode downCallNode) {
            AltrepMethodDescriptor sumMethod = AltrepUtilities.getSumMethodDescriptor(altRealVec);
            return invokeSumMethod(downCallNode, sumMethod, altRealVec, naRm);
        }

        @Specialization
        protected Object altLogicalSum(RLogicalVector altLogicalVec, boolean naRm,
                                    @Cached AltrepDownCallNode downCallNode) {
            AltrepMethodDescriptor sumMethod = AltrepUtilities.getSumMethodDescriptor(altLogicalVec);
            return invokeSumMethod(downCallNode, sumMethod, altLogicalVec, naRm);
        }

        private static Object invokeSumMethod(AltrepDownCallNode downCallNode, AltrepMethodDescriptor sumMethod,
                                              RAbstractAtomicVector altrepVec, boolean naRm) {
            // Sum method has same signature in every class descriptor, therefore we can use just wrapping/unwrapping
            // argument constants from AltIntegerClassDescriptor.
            return downCallNode.execute(sumMethod, AltIntegerClassDescriptor.sumMethodUnwrapResult,
                    AltIntegerClassDescriptor.sumMethodWrapArguments, new Object[]{altrepVec, naRm});
        }
    }

    @GenerateUncached
    public abstract static class MaxNode extends Node {
        public abstract Object execute(Object altrepVector, boolean naRm);

        @Specialization
        protected Object altIntMax(RIntVector altIntVec, boolean naRm,
                           @Cached AltrepDownCallNode downCallNode) {
            AltrepMethodDescriptor maxMethod = AltrepUtilities.getMaxMethodDescriptor(altIntVec);
            return downCallNode.execute(maxMethod, AltIntegerClassDescriptor.maxMethodUnwrapResult,
                    AltIntegerClassDescriptor.maxMethodWrapArguments, new Object[]{altIntVec, naRm});
        }

        @Specialization
        protected Object altRealMax(RDoubleVector altRealVec, boolean naRm,
                                    @Cached AltrepDownCallNode downCallNode) {
            AltrepMethodDescriptor maxMethod = AltrepUtilities.getMaxMethodDescriptor(altRealVec);
            return downCallNode.execute(maxMethod, AltRealClassDescriptor.maxMethodUnwrapResult,
                    AltRealClassDescriptor.maxMethodWrapArguments, new Object[]{altRealVec, naRm});
        }
    }

    @GenerateUncached
    public abstract static class MinNode extends Node {
        public abstract Object execute(Object altrepVector, boolean naRm);

        @Specialization
        protected Object altIntMin(RIntVector altIntVec, boolean naRm,
                                   @Cached AltrepDownCallNode downCallNode) {
            AltrepMethodDescriptor minMethod = AltrepUtilities.getMinMethodDescriptor(altIntVec);
            return downCallNode.execute(minMethod, AltIntegerClassDescriptor.minMethodUnwrapResult,
                    AltIntegerClassDescriptor.minMethodWrapArguments, new Object[]{altIntVec, naRm});
        }

        @Specialization
        protected Object altRealMin(RDoubleVector altRealVec, boolean naRm,
                                    @Cached AltrepDownCallNode downCallNode) {
            AltrepMethodDescriptor minMethod = AltrepUtilities.getMinMethodDescriptor(altRealVec);
            return downCallNode.execute(minMethod, AltRealClassDescriptor.minMethodUnwrapResult,
                    AltRealClassDescriptor.minMethodWrapArguments, new Object[]{altRealVec, naRm});
        }
    }
}
