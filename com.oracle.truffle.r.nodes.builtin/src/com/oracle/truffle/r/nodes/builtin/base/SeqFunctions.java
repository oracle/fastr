/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1998  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1998-2015,  The R Core Team
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.gte;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.missingConstant;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.notIntNA;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.nullValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.size;
import static com.oracle.truffle.r.nodes.builtin.casts.fluent.CastNodeBuilder.newCastBuilder;
import static com.oracle.truffle.r.runtime.RDispatch.INTERNAL_GENERIC;
import static com.oracle.truffle.r.runtime.RError.NO_CALLER;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.ffi.impl.nodes.AsRealNode;
import com.oracle.truffle.r.ffi.impl.nodes.AsRealNodeGen;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetClassAttributeNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.SeqFunctions.SeqInt.IsIntegralNumericNode;
import com.oracle.truffle.r.nodes.builtin.base.SeqFunctionsFactory.GetIntegralNumericNodeGen;
import com.oracle.truffle.r.nodes.builtin.base.SeqFunctionsFactory.IsMissingOrNumericNodeGen;
import com.oracle.truffle.r.nodes.builtin.base.SeqFunctionsFactory.IsNumericNodeGen;
import com.oracle.truffle.r.nodes.builtin.base.SeqFunctionsFactory.SeqIntNodeGen;
import com.oracle.truffle.r.nodes.builtin.base.SeqFunctionsFactory.SeqIntNodeGen.IsIntegralNumericNodeGen;
import com.oracle.truffle.r.nodes.control.RLengthNode;
import com.oracle.truffle.r.nodes.function.CallMatcherNode.CallMatcherGenericNode;
import com.oracle.truffle.r.nodes.function.ClassHierarchyNode;
import com.oracle.truffle.r.nodes.function.call.RExplicitBaseEnvCallDispatcher;
import com.oracle.truffle.r.nodes.unary.CastNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleSequence;
import com.oracle.truffle.r.runtime.data.REmpty;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RIntSequence;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RSequence;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RTypes;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.RFastPathNode;

/**
 * Sequence builtins, {@code seq_along}, {@code seq_len}, {@code seq.int} and fast paths for
 * {@code seq} and {@code seq.default}.
 *
 * Why the fast paths for {@code seq} and {@code seq.default}?. Despite the provision of the more
 * efficient builtins, and encouragement to use them in when appropriate in the R documentation, it
 * seems that many programmers do not heed this advice. Since {@code seq} is generic and the default
 * method {@code seq.default} is coded in R, this can cause a considerable reduction in performance,
 * which is more noticeable in FastR than GNU R.
 *
 * Superficially {@code seq.default} appears to be an R translation of the C code in {@code seq.int}
 * (or vice-versa). This appears to be true for numeric types, but there are some differences. E.g.,
 * {@code seq.int} coerces a character string whereas {@code seq.default} reports an error. Owing to
 * these differences the fast paths do not routinely redirect to {@code seq.int}, only for cases
 * where the arguments are numeric (which is really what we care about anyway for performance).
 * There are also some slight differences in behavior for numeric arguments that may be fixed in an
 * upcoming GNU R release. Currently these are handled by passing a flag when creating the
 * {@link SeqInt} node for the fast paths.
 *
 */
public final class SeqFunctions {

    public abstract static class FastPathAdapter extends RFastPathNode {
        public static IsMissingOrNumericNode createIsMissingOrNumericNode() {
            return IsMissingOrNumericNodeGen.create();
        }

        public static IsNumericNode createIsNumericNode() {
            return IsNumericNodeGen.create();
        }
    }

    @TypeSystemReference(RTypes.class)
    public abstract static class IsNumericNode extends Node {
        public abstract boolean execute(Object obj);

        @Specialization
        protected boolean isNumericNode(@SuppressWarnings("unused") Integer obj) {
            return true;
        }

        @Specialization
        protected boolean isNumericNode(@SuppressWarnings("unused") Double obj) {
            return true;
        }

        @Specialization
        protected boolean isNumericNode(@SuppressWarnings("unused") RAbstractIntVector obj) {
            return true;
        }

        @Specialization
        protected boolean isNumericNode(@SuppressWarnings("unused") RAbstractDoubleVector obj) {
            return true;
        }

        @Fallback
        protected boolean isNumericNode(@SuppressWarnings("unused") Object obj) {
            return false;
        }
    }

    @TypeSystemReference(RTypes.class)
    public abstract static class IsMissingOrNumericNode extends IsNumericNode {

        @Specialization
        protected boolean isMissingOrNumericNode(@SuppressWarnings("unused") RMissing obj) {
            return true;
        }
    }

    @TypeSystemReference(RTypes.class)
    public abstract static class GetIntegralNumericNode extends Node {

        public abstract int execute(Object obj);

        @Specialization
        protected int getIntegralNumeric(Integer integer) {
            return integer;
        }

        @Specialization
        protected int getIntegralNumeric(RAbstractIntVector intVec) {
            return intVec.getDataAt(0);
        }

        @Specialization
        protected int getIntegralNumeric(Double d) {
            return (int) (double) d;
        }

        @Specialization
        protected int getIntegralNumeric(RAbstractDoubleVector doubleVec) {
            return (int) doubleVec.getDataAt(0);
        }

        @Fallback
        protected int getIntegralNumeric(@SuppressWarnings("unused") Object obj) {
            throw RInternalError.shouldNotReachHere();
        }
    }

    public static GetIntegralNumericNode createGetIntegralNumericNode() {
        return GetIntegralNumericNodeGen.create();
    }

    public static IsIntegralNumericNode createIsIntegralNumericNodeNoLengthCheck() {
        return IsIntegralNumericNodeGen.create(false);
    }

    public static IsIntegralNumericNode createIsIntegralNumericNodeLengthCheck() {
        return IsIntegralNumericNodeGen.create(true);
    }

    @TypeSystemReference(RTypes.class)
    @ImportStatic(SeqFunctions.class)
    public abstract static class SeqFastPath extends FastPathAdapter {

        @Specialization(guards = {"!hasClass(args, cache.getClassAttributeNode)", "lengthSpecials(args)"}, limit = "1")
        protected Object seqNoClassFromAndLength(RArgsValuesAndNames args, //
                        @Cached("new()") SeqNoClassFromAndLengthNode cache) {
            if (cache.isNumericProfile.profile(cache.fromCheck.execute(args.getArgument(0)))) {
                if (args.getLength() == 1) {
                    return cache.seqInt.execute(RMissing.instance, RMissing.instance, RMissing.instance, args.getArgument(0), RMissing.instance, RMissing.instance);
                } else {
                    return cache.seqInt.execute(args.getArgument(0), RMissing.instance, RMissing.instance, args.getArgument(1), RMissing.instance, RMissing.instance);
                }
            } else {
                return null;
            }
        }

        public static class SeqNoClassFromAndLengthNode {

            final SeqInt seqInt;
            final RFunction seqIntFunction;
            final ConditionProfile isNumericProfile;
            @Child public GetClassAttributeNode getClassAttributeNode;
            @Child IsMissingOrNumericNode fromCheck;

            public SeqNoClassFromAndLengthNode() {
                this.seqInt = SeqInt.createSeqIntForFastPath();
                this.seqIntFunction = lookupSeqInt();
                this.isNumericProfile = ConditionProfile.createBinaryProfile();
                this.getClassAttributeNode = createGetClassAttributeNode();
                this.fromCheck = createIsMissingOrNumericNode();
            }

        }

        @Specialization(guards = {"!hasClass(args, cache.getClassAttributeNode)"}, limit = "1")
        protected Object seqNoClassAndNumeric(RArgsValuesAndNames args,
                        @Cached("new()") SeqNoClassAndNumericNode cache) {
            Object[] rargs = reorderedArguments(args, cache.seqIntFunction);
            if (cache.isNumericProfile.profile(cache.fromCheck.execute(rargs[0]) && cache.toCheck.execute(rargs[1]) && cache.toCheck.execute(rargs[2]))) {
                return cache.seqInt.execute(rargs[0], rargs[1], rargs[2], rargs[3], rargs[4], RMissing.instance);
            } else {
                return null;
            }
        }

        public static class SeqNoClassAndNumericNode extends SeqNoClassFromAndLengthNode {
            @Child IsMissingOrNumericNode toCheck;
            @Child IsMissingOrNumericNode byCheck;

            public SeqNoClassAndNumericNode() {
                this.toCheck = createIsMissingOrNumericNode();
                this.byCheck = createIsMissingOrNumericNode();
            }

        }

        @Fallback
        protected Object seqFallback(@SuppressWarnings("unused") Object args) {
            return null;
        }

        public static RFunction lookupSeqInt() {
            return RContext.getInstance().lookupBuiltin("seq.int");
        }

        public static GetClassAttributeNode createGetClassAttributeNode() {
            return GetClassAttributeNode.create();
        }

        /**
         * The arguments are reordered if any are named, and later will be checked for missing or
         * numeric.
         *
         * N.B: the reordering has a significant performance cost, e.g.
         *
         * {@code seq(1L, length.out=20L)} is MUCH slower than {@code seq(1L, , , 20L)}
         *
         * TODO we special case the above, as it is a common idiom, but can we improve the general
         * case?
         */
        public static Object[] reorderedArguments(RArgsValuesAndNames argsIn, RFunction seqIntFunction) {
            RArgsValuesAndNames args = argsIn;
            if (args.getSignature().getNonNullCount() != 0) {
                return CallMatcherGenericNode.reorderArguments(args.getArguments(), seqIntFunction, args.getSignature(), NO_CALLER).getArguments();
            } else {
                int len = argsIn.getLength();
                Object[] xArgs = new Object[5];
                for (int i = 0; i < xArgs.length; i++) {
                    xArgs[i] = i < len ? argsIn.getArgument(i) : RMissing.instance;
                }
                return xArgs;
            }
        }

        /**
         * This guard checks whether the first argument (before reordering) has a class (as it might
         * have an S3 {@code seq} method).
         */
        public boolean hasClass(RArgsValuesAndNames args, GetClassAttributeNode getClassAttributeNode) {
            if (args.getLength() > 0) {
                Object arg = args.getArgument(0);
                if (arg instanceof RAbstractVector && getClassAttributeNode.execute(arg) != null) {
                    return true;
                }
            }
            return false;
        }

        private static final String lengthOut = "length.out";

        /**
         * Guard that picks out the common idioms {@code seq(length.out=N)} and
         * {@code seq(M, length.out=N)} N.B. assert: signature names are interned strings
         */
        public boolean lengthSpecials(RArgsValuesAndNames args) {
            int argsLen = args.getLength();
            if (argsLen == 1) {
                String sig0 = args.getSignature().getName(0);
                return sig0 != null && sig0 == lengthOut;
            } else if (argsLen == 2) {
                String sig0 = args.getSignature().getName(0);
                String sig1 = args.getSignature().getName(1);
                return sig0 == null && sig1 != null && sig1 == lengthOut;
            } else {
                return false;
            }
        }
    }

    /**
     * Essentially the same as {@link SeqFastPath} but since the signature is explicit there is no
     * need to reorder arguments.
     */
    @TypeSystemReference(RTypes.class)
    public abstract static class SeqDefaultFastPath extends FastPathAdapter {
        @Specialization(guards = {"cache.fromCheck.execute(fromObj)", "cache.toCheck.execute(toObj)", "cache.byCheck.execute(byObj)"}, limit = "1")
        protected Object seqDefaultNumeric(Object fromObj, Object toObj, Object byObj, Object lengthOut, Object alongWith,
                        @Cached("new()") SeqDefaultNumericNode cache) {
            return cache.seqInt.execute(fromObj, toObj, byObj, lengthOut, alongWith, RMissing.instance);
        }

        public class SeqDefaultNumericNode extends Node {
            @Child SeqInt seqInt;
            @Child public IsMissingOrNumericNode fromCheck;
            @Child public IsMissingOrNumericNode toCheck;
            @Child public IsMissingOrNumericNode byCheck;

            public SeqDefaultNumericNode() {
                seqInt = SeqInt.createSeqIntForFastPath();
                fromCheck = createIsMissingOrNumericNode();
                toCheck = createIsMissingOrNumericNode();
                byCheck = createIsMissingOrNumericNode();
            }
        }

        /**
         * For everything else (not performance-centric) we invoke the original R code.
         */
        @SuppressWarnings("unused")
        @Fallback
        protected Object seqDefaultFallback(Object fromObj, Object toObj, Object byObj, Object lengthOut, Object alongWith) {
            return null;
        }
    }

    @TypeSystemReference(RTypes.class)
    @RBuiltin(name = "seq_along", kind = PRIMITIVE, parameterNames = {"along.with"}, behavior = PURE)
    public abstract static class SeqAlong extends RBuiltinNode.Arg1 {
        @Child private ClassHierarchyNode classHierarchyNode = ClassHierarchyNode.create();

        static {
            Casts.noCasts(SeqAlong.class);
        }

        @Specialization(guards = "!hasClass(value)")
        protected RIntSequence seq(Object value,
                        @Cached("create()") RLengthNode length) {
            return RDataFactory.createIntSequence(1, 1, length.executeInteger(value));
        }

        /**
         * Invokes the 'length' function, which may dispatch to some other function than the default
         * length depending on the class of the argument.
         */
        @Specialization(guards = "hasClass(value)")
        protected RIntSequence seq(VirtualFrame frame, Object value,
                        @Cached("createLengthResultCast()") CastNode resultCast,
                        @Cached("createLengthDispatcher()") RExplicitBaseEnvCallDispatcher dispatcher) {
            int result = (Integer) resultCast.doCast(dispatcher.call(frame, value));
            return RDataFactory.createIntSequence(1, 1, result);
        }

        boolean hasClass(Object obj) {
            final RStringVector classVec = classHierarchyNode.execute(obj);
            return classVec != null && classVec.getLength() != 0;
        }

        RExplicitBaseEnvCallDispatcher createLengthDispatcher() {
            return RExplicitBaseEnvCallDispatcher.create("length");
        }

        CastNode createLengthResultCast() {
            return newCastBuilder().defaultError(Message.NEGATIVE_LENGTH_VECTORS_NOT_ALLOWED).asIntegerVector(false, false, false).findFirst().mustBe(
                            gte(0).and(notIntNA())).buildCastNode();
        }
    }

    @TypeSystemReference(RTypes.class)
    @RBuiltin(name = "seq_len", kind = PRIMITIVE, parameterNames = {"length.out"}, behavior = PURE)
    public abstract static class SeqLen extends RBuiltinNode.Arg1 {

        static {
            Casts casts = new Casts(SeqLen.class);
            /*
             * This is slightly different than what GNU R does as it will report coercion warning
             * for: seq_len(c("7", "b")) GNU R (presumably) gets the first element before doing a
             * coercion but I don't think we can do it with our API
             */
            casts.arg("length.out").asIntegerVector().shouldBe(size(1).or(size(0)), RError.Message.FIRST_ELEMENT_USED, "length.out").findFirst(RRuntime.INT_NA,
                            RError.Message.FIRST_ELEMENT_USED, "length.out").mustBe(gte(0), RError.Message.MUST_BE_COERCIBLE_INTEGER);
        }

        @Specialization
        protected RIntSequence seqLen(int length) {
            return RDataFactory.createIntSequence(1, 1, length);
        }
    }

    /**
     * The GNU R logic for this builtin is a complex sequence (sic) of "if" statements, that handle
     * the presence/absence of the arguments. Converting this to Truffle, where we want to tease out
     * specific argument combinations for efficiency is not straightforward and arguably is less
     * transparent.
     *
     * The fact that any of the arguments can be missing is a complicating factor. There is no FastR
     * type that signifies "any type except RMissing", so we have to use guards. We also have to be
     * careful that specializations do not overlap due to the possibility of a missing value.
     *
     * Converted from GNU R src/main/seq.c
     *
     * The specializations are broken into five groups, corresponding to the five "forms" described
     * in <a href="https://stat.ethz.ch/R-manual/R-devel/library/base/html/seq.html">Sequence
     * Generation</a>, (but in a different order).
     *
     * N.B. javac gives error "cannot find symbol" on plain "@RBuiltin".
     */
    @ImportStatic({AsRealNodeGen.class, SeqFunctions.class})
    @SuppressWarnings("unused")
    @com.oracle.truffle.r.runtime.builtins.RBuiltin(name = "seq.int", kind = PRIMITIVE, parameterNames = {"from", "to", "by", "length.out", "along.with",
                    "..."}, dispatch = INTERNAL_GENERIC, genericName = "seq", behavior = PURE)
    public abstract static class SeqInt extends RBuiltinNode.Arg6 {
        private final boolean seqFastPath;

        /**
         * Used by {@link #getLength} guard. It would be good to cache this in the relevant
         * specializations but it does not use {@link RTypes} and that causes an
         * IllegalStateException (no parent).
         */
        @Child private RLengthNode lengthNode = RLengthNode.create();

        private static final double FLT_EPSILON = 1.19209290e-7;

        static {
            Casts casts = new Casts(SeqInt.class);
            casts.arg("length.out").allowMissing().mapIf(nullValue(), missingConstant());
        }

        @Override
        public abstract Object execute(VirtualFrame frame, Object start, Object to, Object by, Object lengthOut, Object alongWith, Object dotdotdot);

        public abstract Object execute(Object start, Object to, Object by, Object lengthOut, Object alongWith, Object dotdotdot);

        protected SeqInt(boolean seqFastPath) {
            this.seqFastPath = seqFastPath;
        }

        protected SeqInt() {
            this(false);
        }

        public static SeqInt createSeqInt() {
            return SeqIntNodeGen.create(false);
        }

        public static SeqInt createSeqIntForFastPath() {
            return SeqIntNodeGen.create(true);
        }

        // No matching args (special case)

        @Specialization
        protected RIntSequence allMissing(RMissing from, RMissing to, RMissing by, RMissing lengthOut, RMissing alongWith, Object dotdotdot) {
            // GNU R allows this and returns 1
            return RDataFactory.createIntSequence(1, 1, 1);
        }

        /*
         * seq(from) One "from" arg: THE most common case? ASSERT: this handles ALL the cases where
         * "from" is not missing, i.e. the "One" case. Therefore, in subsequent specializations we
         * should be careful about an overlap where "from" might or might not be missing.
         */

        /**
         * Irrespective of the R type, if the length is zero the result is an empty sequence.
         */
        @Specialization(guards = {"!isMissing(from)", "getLength(from) == 0"})
        protected RIntVector emptySeqFromOneArg(Object from, RMissing to, RMissing by, RMissing lengthOut, RMissing alongWith, Object dotdotdot) {
            return RDataFactory.createEmptyIntVector();
        }

        /**
         * Also, irrespective of the R type, if the length is greater than 1, the length itself is
         * used as the upper bound of the sequence. This is slightly counter-intuitive as most
         * builtins take the </i>value</i> of the first element and warn about ignoring the rest,
         * but the value likely could not be coerced.
         */
        @Specialization(guards = {"!isMissing(from)", "getLength(from) > 1"})
        protected RIntSequence lenSeqFromOneArg(Object from, RMissing to, RMissing by, RMissing lengthOut, RMissing alongWith, Object dotdotdot) {
            return RDataFactory.createIntSequence(1, 1, getLength(from));
        }

        /**
         * A length-1 REAL. Return "1:(int) from" where from is positive integral
         */
        @Specialization(guards = {"fromVec.getLength() == 1", "isPositiveIntegralDouble(fromVec.getDataAt(0))"})
        protected RAbstractVector seqFromOneArgIntDouble(RAbstractDoubleVector fromVec, RMissing to, RMissing by, RMissing lengthOut, RMissing alongWith, Object dotdotdot) {
            int len = (int) fromVec.getDataAt(0);
            return RDataFactory.createIntSequence(1, 1, len);
        }

        /**
         * A length-1 REAL. Return "1:(int) from" (N.B. from may be negative) EXCEPT
         * {@code seq(0.2)} is NOT the same as {@code seq(0.0)} (according to GNU R)
         */
        @Specialization(guards = "fromVec.getLength() == 1")
        protected RAbstractVector seqFromOneArgDouble(RAbstractDoubleVector fromVec, RMissing to, RMissing by, RMissing lengthOut, RMissing alongWith, Object dotdotdot) {
            double from = validateDoubleParam(fromVec.getDataAt(0), fromVec, "from");
            int len = effectiveLength(1, from);
            return RDataFactory.createIntSequence(1, from > 0 ? 1 : -1, len);
        }

        /**
         * A length-1 INT. Return "1:from" (N.B. from may be negative)
         */
        @Specialization(guards = "fromVec.getLength() == 1")
        protected RIntSequence seqFromOneArgInt(RAbstractIntVector fromVec, RMissing to, RMissing by, RMissing lengthOut, RMissing alongWith, Object dotdotdot) {
            int from = validateIntParam(fromVec.getDataAt(0), "from");
            int len = from > 0 ? from : 2 - from;
            return RDataFactory.createIntSequence(1, from > 0 ? 1 : -1, len);
        }

        /**
         * A length-1 something other than REAL/INT. Again, use the length, not the value (which
         * likely would not make sense, e.g. {@code expression(x, y)}). N.B. Without
         * {@code !isNumeric(from)} guard this would "contain" the previous two specializations,
         * which would be incorrect as the result is different.
         */
        @Specialization(guards = {"!isMissing(from)", "getLength(from) == 1", "!isNumeric(from)"})
        protected RIntSequence seqFromOneArgObj(Object from, RMissing to, RMissing by, RMissing lengthOut, RMissing alongWith, Object dotdotdot) {
            return RDataFactory.createIntSequence(1, 1, 1);
        }

        /*
         * seq(from,to) but either could be missing. "along.with" is missing and "length.out" is
         * missing (or NULL), and "by" (by) is missing. N.B. we are only interested in the cases
         * "from=missing, to!=missing" and "from!=missing, to!=missing" as
         * "from!=missing, to=missing" is already covered in the "One" specializations.
         *
         * The first two specializations handle the expected common cases with valid arguments. The
         * third specialization handles other types and invalid arguments.
         */

        @Specialization(guards = "validDoubleParams(fromVec, toVec)")
        protected RAbstractVector seqLengthByMissingDouble(RAbstractDoubleVector fromVec, RAbstractDoubleVector toVec, RMissing by, RMissing lengthOut, RMissing alongWith, Object dotdotdot,
                        @Cached("createBinaryProfile()") ConditionProfile directionProfile) {
            double from = fromVec.getDataAt(0);
            double to = toVec.getDataAt(0);
            RAbstractVector result = createRSequence(from, to, directionProfile);
            return result;
        }

        @Specialization(guards = "validIntParams(fromVec, toVec)")
        protected RAbstractVector seqLengthByMissingInt(RAbstractIntVector fromVec, RAbstractIntVector toVec, RMissing by, RMissing lengthOut, RMissing alongWith, Object dotdotdot,
                        @Cached("createBinaryProfile()") ConditionProfile directionProfile) {
            int from = fromVec.getDataAt(0);
            int to = toVec.getDataAt(0);
            RIntSequence result = createRIntSequence(from, to, directionProfile);
            return result;
        }

        /**
         * The performance of this specialization, we assert, is not important. It captures a
         * mixture of coercions from improbable types and error cases. N.B. However, mixing doubles
         * and ints <b<will</b> hit this specialization; is that likely and a concern? If "from
         * ==missing", it defaults to 1.0. "to" cannot be missing as that would overlap with
         * previous specializations.
         */
        @Specialization(guards = {"!isMissing(toObj)"})
        protected RAbstractVector seqLengthByMissing(Object fromObj, Object toObj, RMissing by, RMissing lengthOut, RMissing alongWith, Object dotdotdot,
                        @Cached("create()") AsRealNode asRealFrom,
                        @Cached("create()") AsRealNode asRealTo,
                        @Cached("createBinaryProfile()") ConditionProfile directionProfile) {
            double from;
            if (isMissing(fromObj)) {
                from = 1.0;
            } else {
                validateLength(fromObj, "from");
                from = asRealFrom.execute(fromObj);
                validateDoubleParam(from, fromObj, "from");
            }
            validateLength(toObj, "to");
            double to = asRealTo.execute(toObj);
            validateDoubleParam(to, toObj, "to");
            return createRSequence(from, to, directionProfile);
        }

        /*
         * seq(from, to, by=). As above but with "by" not missing. Except for the special case of
         * from/to/by all ints, we do not specialize on "by". Again, "from != missing" is already
         * handled in the "One" specializations.
         */

        @Specialization(guards = {"validDoubleParams(fromVec, toVec)", "!isMissing(byObj)"})
        protected Object seqLengthMissing(RAbstractDoubleVector fromVec, RAbstractDoubleVector toVec, Object byObj, RMissing lengthOut, RMissing alongWith, Object dotdotdot,
                        @Cached("create()") AsRealNode asRealby) {
            validateLength(byObj, "by");
            double by = asRealby.execute(byObj);
            return doSeqLengthMissing(fromVec.getDataAt(0), toVec.getDataAt(0), by, false);
        }

        @Specialization(guards = {"validIntParams(fromVec, toVec)", "validIntParam(byVec)", "byVec.getDataAt(0) != 0"})
        protected RAbstractVector seqLengthMissing(RAbstractIntVector fromVec, RAbstractIntVector toVec, RAbstractIntVector byVec, RMissing lengthOut, RMissing alongWith, Object dotdotdot,
                        @Cached("createBinaryProfile()") ConditionProfile directionProfile) {
            int by = byVec.getDataAt(0);
            int from = fromVec.getDataAt(0);
            int to = toVec.getDataAt(0);
            RIntSequence result;
            if (directionProfile.profile(from < to)) {
                if (by < 0) {
                    throw error(RError.Message.WRONG_SIGN_IN_BY);
                }
                result = RDataFactory.createIntSequence(from, by, (to - from) / by + 1);
            } else {
                if (from == to) {
                    return RDataFactory.createIntVectorFromScalar(from);
                }
                if (by > 0) {
                    throw error(RError.Message.WRONG_SIGN_IN_BY);
                }
                result = RDataFactory.createIntSequence(from, by, (from - to) / (-by) + 1);
            }
            return result;
        }

        /**
         * See comment in {@link #seqLengthByMissing}.
         */
        @Specialization(guards = {"!isMissing(byObj)"})
        protected Object seqLengthMissing(Object fromObj, Object toObj, Object byObj, RMissing lengthOut, RMissing alongWith, Object dotdotdot,
                        @Cached("create()") AsRealNode asRealFrom,
                        @Cached("create()") AsRealNode asRealTo,
                        @Cached("create()") AsRealNode asRealby) {
            double from;
            boolean allInt = true;
            if (isMissing(fromObj)) {
                from = 1.0;
                allInt = false;
            } else {
                validateLength(fromObj, "from");
                from = asRealFrom.execute(fromObj);
                validateDoubleParam(from, fromObj, "from");
                allInt &= isInt(fromObj);
            }
            double to;
            if (isMissing(toObj)) {
                to = 1.0;
                allInt = false;
            } else {
                validateLength(toObj, "to");
                to = asRealTo.execute(toObj);
                validateDoubleParam(to, toObj, "to");
                allInt &= isInt(toObj);
            }
            validateLength(byObj, "by");
            allInt &= isInt(byObj);
            double by = asRealby.execute(byObj);
            return doSeqLengthMissing(from, to, by, allInt);
        }

        private static final double FEPS = 1E-10;

        private RAbstractVector doSeqLengthMissing(double from, double to, double by, boolean allInt) {
            double del = to - from;
            if (del == 0.0 && to == 0.0) {
                return RDataFactory.createDoubleVectorFromScalar(to);
            }
            double n = del / by;
            if (!isFinite(n)) {
                if (del == 0.0 && by == 0.0) {
                    // N.B. GNU R returns the original "from" argument (which might be missing)
                    return RDataFactory.createDoubleVectorFromScalar(from);
                } else {
                    // This should go away in an upcoming GNU R release
                    throw error(seqFastPath ? RError.Message.INVALID_TFB_SD : RError.Message.INVALID_TFB);
                }
            }
            double dd = Math.abs(del) / Math.max(Math.abs(to), Math.abs(from));
            if (dd < 100 * RRuntime.EPSILON) {
                // N.B. GNU R returns the original "from" argument (which might be missing)
                return RDataFactory.createDoubleVectorFromScalar(from);
            }
            if (n > Integer.MAX_VALUE) {
                throw error(RError.Message.BY_TOO_SMALL);
            }
            if (n < -FEPS) {
                throw error(RError.Message.WRONG_SIGN_IN_BY);
            }
            RAbstractVector result;
            if (allInt) {
                result = RDataFactory.createIntSequence((int) from, (int) by, (int) (n + 1));
            } else {
                int nn = (int) (n + FEPS);
                if (nn == 0) {
                    return RDataFactory.createDoubleVectorFromScalar(from);
                }
                double datann = from + nn * by;
                // Added in 2.9.0
                boolean datannAdjust = (by > 0 && datann > to) || (by < 0 && datann < to);
                if (!datannAdjust) {
                    result = RDataFactory.createDoubleSequence(from, by, nn + 1);
                } else {
                    // GNU R creates actual vectors and adjusts the last element to "to"
                    // We can't do that with RDoubleSequence without breaking the intermediate
                    // values
                    double[] data = new double[nn + 1];
                    for (int i = 0; i < nn; i++) {
                        data[i] = from + i * by;
                    }
                    data[nn] = to;
                    result = RDataFactory.createDoubleVector(data, RDataFactory.COMPLETE_VECTOR);
                }
            }
            return result;

        }

        /*
         * seq(length.out=)
         */

        @Specialization(guards = "!isMissing(lengthOut)")
        protected RAbstractVector seqJustLength(RMissing from, RMissing to, RMissing by, Object lengthOut, RMissing alongWith, Object dotdotdot,
                        @Cached("create()") AsRealNode asRealLen) {
            int n = checkLength(lengthOut, asRealLen);
            return n == 0 ? RDataFactory.createEmptyIntVector() : RDataFactory.createIntSequence(1, 1, n);
        }

        // seq(along,with=)

        @Specialization(guards = "!isMissing(alongWith)")
        protected RAbstractVector seqFromJustAlong(RMissing from, RMissing to, RMissing by, RMissing lengthOut, Object alongWith, Object dotdotdot) {
            int len = getLength(alongWith);
            return len == 0 ? RDataFactory.createEmptyIntVector() : RDataFactory.createIntSequence(1, 1, len);
        }

        /*
         * The remaining non-error cases are when either length.out or along.with are provided in
         * addition to one or more of from/to/by. Unfortunately this is still a combinatorial
         * explosion of possibilities. We break this into three and the logic follows that in seq.c.
         *
         * The "oneNotMissing(alongWith, lengthOut)" ensure no overlap with the preceding
         * specializations where these were missing.
         *
         * N.B. Counter-intuitive; in the cases where "from" or "to" is missing, but "by" is
         * integral, GNU R returns an int sequence truncating "from" or "to". So seq.int(2.7, by=2,
         * length.out=4) produces [2,4,6,8], rather than [2.7,4.7,6.7,8.7]. But, seq.int(2.7,
         * by=2.1, length.out=4) produces [2.7,4.8,6.9,9.0]
         *
         * N.B. Also, there is no length check in these forms, so "seq.int(from=c(1,2), by=2,
         * length.out=10)" is legal.
         *
         * The only special case we define is "seq.int(from=k, length.lout=lout)" where "k" and
         * "lout" are integral (not just integer as programmers are casual about numeric literals
         * and often use "1" where "1L" is more appropriate).
         */

        @TypeSystemReference(RTypes.class)
        public abstract static class IsIntegralNumericNode extends Node {
            private final boolean checkLength;

            public abstract boolean execute(Object obj);

            public IsIntegralNumericNode(boolean checkLength) {
                this.checkLength = checkLength;
            }

            @Specialization
            protected boolean isIntegralNumericNode(int obj) {
                if (checkLength) {
                    return obj >= 0;
                } else {
                    return true;
                }
            }

            @Specialization
            protected boolean isIntegralNumericNode(RAbstractIntVector intVec) {
                return intVec.getLength() == 1 && (checkLength ? intVec.getDataAt(0) >= 0 : true);
            }

            @Specialization
            protected boolean isIntegralNumericNode(double obj) {
                double d = obj;
                return d == (int) d && (checkLength ? d >= 0 : true);
            }

            @Specialization
            protected boolean isIntegralNumericNode(RAbstractDoubleVector doubleVec) {
                if (doubleVec.getLength() == 1) {
                    double d = doubleVec.getDataAt(0);
                    return d == (int) d && (checkLength ? d >= 0 : true);
                } else {
                    return false;
                }
            }

            @Fallback
            protected boolean isIntegralNumericNode(Object obj) {
                return false;
            }
        }

        // common idiom
        @Specialization(guards = {"cached.fromCheck.execute(fromObj)", "cached.lengthCheck.execute(lengthOut)"}, limit = "1")
        protected RAbstractVector seqWithFromLengthIntegralNumeric(Object fromObj, RMissing toObj, RMissing byObj, Object lengthOut, RMissing alongWith, Object dotdotdot,
                        @Cached("new()") SeqWithFromLengthIntegralNumericNode cached) {
            int from = cached.getIntegralNumericNode.execute(fromObj);
            int lout = cached.getIntegralNumericNode.execute(lengthOut);
            if (lout == 0) {
                return RDataFactory.createEmptyIntVector();
            }
            return RDataFactory.createDoubleSequence(from, 1, lout);
        }

        public class SeqWithFromLengthIntegralNumericNode extends Node {
            @Child GetIntegralNumericNode getIntegralNumericNode;
            @Child public IsIntegralNumericNode fromCheck;
            @Child public IsIntegralNumericNode lengthCheck;

            public SeqWithFromLengthIntegralNumericNode() {
                getIntegralNumericNode = createGetIntegralNumericNode();
                fromCheck = createIsIntegralNumericNodeNoLengthCheck();
                lengthCheck = createIsIntegralNumericNodeLengthCheck();
            }
        }

        // "by" missing
        @Specialization(guards = {"oneNotMissing(alongWith, lengthOut)", "oneNotMissing(fromObj, toObj)"})
        protected RAbstractVector seqWithLength(Object fromObj, Object toObj, RMissing byObj, Object lengthOut, Object alongWith, Object dotdotdot,
                        @Cached("create()") AsRealNode asRealFrom,
                        @Cached("create()") AsRealNode asRealTo,
                        @Cached("create()") AsRealNode asRealLen) {
            int lout = checkLengthAlongWith(lengthOut, alongWith, asRealLen);
            if (lout == 0) {
                return RDataFactory.createEmptyIntVector();
            }
            boolean fromMissing = isMissing(fromObj);
            boolean toMissing = isMissing(toObj);
            double from = asRealFrom.execute(fromObj);
            double to = asRealTo.execute(toObj);
            if (toMissing) {
                to = from + lout - 1;
            }
            if (fromMissing) {
                from = to - lout + 1;
            }
            validateDoubleParam(from, fromObj, "from");
            validateDoubleParam(to, toObj, "to");
            RAbstractVector result;
            if (lout > 2) {
                double by = (to - from) / (lout - 1);
                // double computedTo = from + (lout - 1) * by;
                /*
                 * GNU R sets data[lout-1] to "to". Experimentally using an RDoubleSequence
                 * sometimes produces a value that differs by a very small amount instead, so we use
                 * a vector.
                 */
                double[] data = new double[lout];
                data[0] = from;
                data[lout - 1] = to;
                for (int i = 1; i < lout - 1; i++) {
                    data[i] = from + i * by;
                }
                result = RDataFactory.createDoubleVector(data, RDataFactory.COMPLETE_VECTOR);
            } else {
                if (lout == 1) {
                    result = RDataFactory.createDoubleVectorFromScalar(from);
                } else {
                    boolean useDouble = fromMissing && !isInt(lengthOut);
                    if ((int) from == from && (int) to == to && !useDouble) {
                        result = RDataFactory.createIntVector(new int[]{(int) from, (int) to}, RDataFactory.COMPLETE_VECTOR);
                    } else {
                        result = RDataFactory.createDoubleVector(new double[]{from, to}, RDataFactory.COMPLETE_VECTOR);
                    }
                }
            }
            return result;
        }

        // "to" missing
        @Specialization(guards = {"oneNotMissing(alongWith, lengthOut)", "oneNotMissing(fromObj, byObj)"})
        protected RAbstractVector seqWithLength(Object fromObj, RMissing toObj, Object byObj, Object lengthOut, Object alongWith, Object dotdotdot,
                        @Cached("create()") AsRealNode asRealFrom,
                        @Cached("create()") AsRealNode asRealby,
                        @Cached("create()") AsRealNode asRealLen) {
            int lout = checkLengthAlongWith(lengthOut, alongWith, asRealLen);
            if (lout == 0) {
                return RDataFactory.createEmptyIntVector();
            }
            double from;
            if (isMissing(fromObj)) {
                from = 1.0;
            } else {
                from = asRealFrom.execute(fromObj);
                validateDoubleParam(from, fromObj, "from");
            }
            double by = asRealby.execute(byObj);
            validateDoubleParam(by, byObj, "by");
            double to = from + (lout - 1) * by;
            if (useIntVector(from, to, by)) {
                return RDataFactory.createIntSequence((int) from, (int) by, lout);
            } else {
                return RDataFactory.createDoubleSequence(from, by, lout);
            }
        }

        // "from" missing
        @Specialization(guards = {"oneNotMissing(alongWith, lengthOut)", "oneNotMissing(toObj, byObj)"})
        protected RAbstractVector seqWithLength(RMissing fromObj, Object toObj, Object byObj, Object lengthOut, Object alongWith, Object dotdotdot,
                        @Cached("create()") AsRealNode asRealTo,
                        @Cached("create()") AsRealNode asRealby,
                        @Cached("create()") AsRealNode asRealLen) {
            int lout = checkLengthAlongWith(lengthOut, alongWith, asRealLen);
            if (lout == 0) {
                return RDataFactory.createEmptyIntVector();
            }
            double to = asRealTo.execute(toObj);
            double by = asRealby.execute(byObj);
            double from = to - (lout - 1) * by;
            validateDoubleParam(to, toObj, "to");
            validateDoubleParam(by, byObj, "by");
            if (useIntVector(from, to, by)) {
                return RDataFactory.createIntSequence((int) from, (int) by, lout);
            } else {
                return RDataFactory.createDoubleSequence(from, by, lout);
            }
        }

        @Fallback
        protected RAbstractVector seqFallback(Object fromObj, Object toObj, Object byObj, Object lengthOut, Object alongWith, Object dotdotdot) {
            throw error(RError.Message.TOO_MANY_ARGS);
        }

        // Guard methods

        public static boolean validDoubleParams(RAbstractDoubleVector from, RAbstractDoubleVector to) {
            return from.getLength() == 1 && to.getLength() == 1 && isFinite(from.getDataAt(0)) && isFinite(to.getDataAt(0));
        }

        public static boolean validIntParams(RAbstractIntVector from, RAbstractIntVector to) {
            return validIntParam(from) && validIntParam(to);
        }

        public static boolean validIntParam(RAbstractIntVector vec) {
            return vec.getLength() == 1 && vec.getDataAt(0) != RRuntime.INT_NA;
        }

        public final int getLength(Object obj) {
            return lengthNode.executeInteger(obj);
        }

        public static boolean isNumeric(Object obj) {
            return obj instanceof Double || obj instanceof Integer || obj instanceof RAbstractDoubleVector || obj instanceof RAbstractIntVector;
        }

        public static boolean isInt(Object obj) {
            return obj instanceof Integer || obj instanceof RAbstractIntVector;
        }

        public static boolean isMissing(Object obj) {
            return obj == RMissing.instance || obj == REmpty.instance;
        }

        public static boolean oneNotMissing(Object obj1, Object obj2) {
            return !isMissing(obj1) || !isMissing(obj2);
        }

        public static boolean isPositiveIntegralDouble(double d) {
            int id = (int) d;
            return id == d && id > 0;
        }

        // Utility methods

        private static boolean isFinite(double v) {
            return !(RRuntime.isNAorNaN(v) || Double.isInfinite(v));
        }

        private int validateIntParam(int v, String vName) {
            if (RRuntime.isNA(v)) {
                throw error(RError.Message.MUST_BE_FINITE, vName);
            }
            return v;
        }

        /**
         * Unless {@code vObj} is missing, check whether {@code isFinite}. Return {@code v}
         * unmodified.
         */
        private double validateDoubleParam(double v, Object vObj, String vName) {
            if (vObj != RMissing.instance) {
                if (!isFinite(v)) {
                    throw error(RError.Message.MUST_BE_FINITE, vName);
                }
            }
            return v;
        }

        /**
         * Unless {@code obj} is missing, check whether length is 1.
         */
        private void validateLength(Object obj, String vName) {
            if (obj != RMissing.instance) {
                if (getLength(obj) != 1) {
                    throw error(RError.Message.MUST_BE_SCALAR, vName);
                }
            }
        }

        private int checkLength(Object lengthOut, AsRealNode asRealLen) {
            double len = asRealLen.execute(lengthOut);
            if (RRuntime.isNAorNaN(len) || len <= -0.5) {
                throw error(seqFastPath ? RError.Message.MUST_BE_POSITIVE_SD : RError.Message.MUST_BE_POSITIVE, seqFastPath ? "length" : "length.out");
            }
            if (getLength(lengthOut) != 1) {
                warning(RError.Message.FIRST_ELEMENT_USED, "length.out");
            }
            return (int) Math.ceil(len);
        }

        private static boolean isInIntRange(double d) {
            return d <= Integer.MAX_VALUE && d >= Integer.MIN_VALUE;
        }

        private static boolean useIntVector(double from, double to, double by) {
            return (int) by == by && isInIntRange(from) && isInIntRange(to);
        }

        private int checkLengthAlongWith(Object lengthOut, Object alongWith, AsRealNode asRealLen) {
            if (alongWith != RMissing.instance) {
                return getLength(alongWith);
            } else if (lengthOut != RMissing.instance) {
                return checkLength(lengthOut, asRealLen);
            } else {
                throw RInternalError.shouldNotReachHere();
            }
        }

        private static int effectiveLength(double n1, double n2) {
            double r = Math.abs(n2 - n1);
            return (int) (r + 1 + FLT_EPSILON);
        }

        private int checkVecLength(double from, double to) {
            double r = Math.abs(to - from);
            if (r > Integer.MAX_VALUE) {
                throw error(RError.Message.TOO_LONG_VECTOR);
            }
            int length = (int) (r + 1 + FLT_EPSILON);
            return length;
        }

        /**
         * Maps from {@code from} and {@code to} to the {@link RSequence} interface.
         */
        private static RIntSequence createRIntSequence(int from, int to, ConditionProfile directionProfile) {
            if (directionProfile.profile(from <= to)) {
                int length = to - from + 1;
                return RDataFactory.createIntSequence(from, 1, length);
            } else {
                int length = from - to + 1;
                return RDataFactory.createIntSequence(from, -1, length);
            }
        }

        /**
         * Similar to {@link #createRIntSequence} but chooses the type of sequence based on the
         * argument values.
         */
        private RAbstractVector createRSequence(double from, double to, ConditionProfile directionProfile) {
            boolean useInt = from <= Integer.MAX_VALUE && (from == (int) from);
            int length = 0;
            if (useInt) {
                if (from <= Integer.MIN_VALUE || from > Integer.MAX_VALUE) {
                    useInt = false;
                } else {
                    /* r := " the effective 'to' " of from:to */
                    double dn = Math.abs(to - from) + 1 + FLT_EPSILON;
                    length = (int) dn;
                    double r = from + ((from <= to) ? dn - 1 : -(dn - 1));
                    if (r <= Integer.MIN_VALUE || r > Integer.MAX_VALUE) {
                        useInt = false;
                    }
                }
            }
            if (useInt) {
                RIntSequence result = RDataFactory.createIntSequence((int) from, directionProfile.profile(from <= to) ? 1 : -1, length);
                return result;
            } else {
                length = checkVecLength(from, to);
                RDoubleSequence result = RDataFactory.createDoubleSequence(from, directionProfile.profile(from <= to) ? 1 : -1, length);
                return result;
            }
        }
    }
}
