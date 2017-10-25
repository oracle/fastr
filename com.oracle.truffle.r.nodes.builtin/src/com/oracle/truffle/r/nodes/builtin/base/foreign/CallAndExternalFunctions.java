/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base.foreign;

import static com.oracle.truffle.r.runtime.RVisibility.CUSTOM;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.library.fastrGrid.FastRGridExternalLookup;
import com.oracle.truffle.r.library.methods.MethodsListDispatchFactory.R_M_setPrimitiveMethodsNodeGen;
import com.oracle.truffle.r.library.methods.MethodsListDispatchFactory.R_externalPtrPrototypeObjectNodeGen;
import com.oracle.truffle.r.library.methods.MethodsListDispatchFactory.R_getClassFromCacheNodeGen;
import com.oracle.truffle.r.library.methods.MethodsListDispatchFactory.R_getGenericNodeGen;
import com.oracle.truffle.r.library.methods.MethodsListDispatchFactory.R_identCNodeGen;
import com.oracle.truffle.r.library.methods.MethodsListDispatchFactory.R_initMethodDispatchNodeGen;
import com.oracle.truffle.r.library.methods.MethodsListDispatchFactory.R_methodsPackageMetaNameNodeGen;
import com.oracle.truffle.r.library.methods.MethodsListDispatchFactory.R_nextMethodCallNodeGen;
import com.oracle.truffle.r.library.methods.MethodsListDispatchFactory.R_set_method_dispatchNodeGen;
import com.oracle.truffle.r.library.methods.SlotFactory.R_getSlotNodeGen;
import com.oracle.truffle.r.library.methods.SlotFactory.R_hasSlotNodeGen;
import com.oracle.truffle.r.library.methods.SlotFactory.R_setSlotNodeGen;
import com.oracle.truffle.r.library.methods.SubstituteDirectNodeGen;
import com.oracle.truffle.r.library.parallel.ParallelFunctionsFactory.MCIsChildNodeGen;
import com.oracle.truffle.r.library.stats.BinDist;
import com.oracle.truffle.r.library.stats.CdistNodeGen;
import com.oracle.truffle.r.library.stats.CompleteCases;
import com.oracle.truffle.r.library.stats.CovcorNodeGen;
import com.oracle.truffle.r.library.stats.CutreeNodeGen;
import com.oracle.truffle.r.library.stats.DoubleCentreNodeGen;
import com.oracle.truffle.r.library.stats.PPSum;
import com.oracle.truffle.r.library.stats.RMultinomNode;
import com.oracle.truffle.r.library.stats.RandFunctionsNodes;
import com.oracle.truffle.r.library.stats.RandFunctionsNodes.RandFunction1Node;
import com.oracle.truffle.r.library.stats.RandFunctionsNodes.RandFunction2Node;
import com.oracle.truffle.r.library.stats.RandFunctionsNodes.RandFunction3Node;
import com.oracle.truffle.r.library.stats.SignrankFreeNode;
import com.oracle.truffle.r.library.stats.SplineFunctionsFactory.SplineCoefNodeGen;
import com.oracle.truffle.r.library.stats.SplineFunctionsFactory.SplineEvalNodeGen;
import com.oracle.truffle.r.library.stats.StatsFunctionsNodes;
import com.oracle.truffle.r.library.stats.WilcoxFreeNode;
import com.oracle.truffle.r.library.stats.deriv.D;
import com.oracle.truffle.r.library.stats.deriv.Deriv;
import com.oracle.truffle.r.library.tools.C_ParseRdNodeGen;
import com.oracle.truffle.r.library.tools.DirChmodNodeGen;
import com.oracle.truffle.r.library.tools.Rmd5NodeGen;
import com.oracle.truffle.r.library.tools.ToolsTextFactory.CodeFilesAppendNodeGen;
import com.oracle.truffle.r.library.tools.ToolsTextFactory.DoTabExpandNodeGen;
import com.oracle.truffle.r.library.utils.CountFieldsNodeGen;
import com.oracle.truffle.r.library.utils.Crc64NodeGen;
import com.oracle.truffle.r.library.utils.DownloadNodeGen;
import com.oracle.truffle.r.library.utils.MenuNodeGen;
import com.oracle.truffle.r.library.utils.ObjectSizeNodeGen;
import com.oracle.truffle.r.library.utils.RprofNodeGen;
import com.oracle.truffle.r.library.utils.RprofmemNodeGen;
import com.oracle.truffle.r.library.utils.TypeConvertNodeGen;
import com.oracle.truffle.r.library.utils.UnzipNodeGen;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.RInternalCodeBuiltinNode;
import com.oracle.truffle.r.nodes.helpers.MaterializeNode;
import com.oracle.truffle.r.nodes.objects.GetPrimNameNodeGen;
import com.oracle.truffle.r.nodes.objects.NewObjectNodeGen;
import com.oracle.truffle.r.runtime.FastROptions;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalCode;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RExternalPtr;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.ffi.CallRFFI;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.NativeCallInfo;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;
import com.oracle.truffle.r.runtime.nmath.distr.Cauchy;
import com.oracle.truffle.r.runtime.nmath.distr.Cauchy.DCauchy;
import com.oracle.truffle.r.runtime.nmath.distr.Cauchy.PCauchy;
import com.oracle.truffle.r.runtime.nmath.distr.Cauchy.RCauchy;
import com.oracle.truffle.r.runtime.nmath.distr.Chisq;
import com.oracle.truffle.r.runtime.nmath.distr.Chisq.RChisq;
import com.oracle.truffle.r.runtime.nmath.distr.DBeta;
import com.oracle.truffle.r.runtime.nmath.distr.DGamma;
import com.oracle.truffle.r.runtime.nmath.distr.DHyper;
import com.oracle.truffle.r.runtime.nmath.distr.DNBeta;
import com.oracle.truffle.r.runtime.nmath.distr.DNBinom.DNBinomFunc;
import com.oracle.truffle.r.runtime.nmath.distr.DNBinom.DNBinomMu;
import com.oracle.truffle.r.runtime.nmath.distr.DNChisq;
import com.oracle.truffle.r.runtime.nmath.distr.DNorm;
import com.oracle.truffle.r.runtime.nmath.distr.DPois;
import com.oracle.truffle.r.runtime.nmath.distr.Dbinom;
import com.oracle.truffle.r.runtime.nmath.distr.Df;
import com.oracle.truffle.r.runtime.nmath.distr.Dnf;
import com.oracle.truffle.r.runtime.nmath.distr.Dnt;
import com.oracle.truffle.r.runtime.nmath.distr.Dt;
import com.oracle.truffle.r.runtime.nmath.distr.Exp.DExp;
import com.oracle.truffle.r.runtime.nmath.distr.Exp.PExp;
import com.oracle.truffle.r.runtime.nmath.distr.Exp.QExp;
import com.oracle.truffle.r.runtime.nmath.distr.Exp.RExp;
import com.oracle.truffle.r.runtime.nmath.distr.Geom;
import com.oracle.truffle.r.runtime.nmath.distr.Geom.DGeom;
import com.oracle.truffle.r.runtime.nmath.distr.Geom.RGeom;
import com.oracle.truffle.r.runtime.nmath.distr.LogNormal;
import com.oracle.truffle.r.runtime.nmath.distr.LogNormal.DLNorm;
import com.oracle.truffle.r.runtime.nmath.distr.LogNormal.PLNorm;
import com.oracle.truffle.r.runtime.nmath.distr.LogNormal.QLNorm;
import com.oracle.truffle.r.runtime.nmath.distr.Logis;
import com.oracle.truffle.r.runtime.nmath.distr.Logis.DLogis;
import com.oracle.truffle.r.runtime.nmath.distr.Logis.RLogis;
import com.oracle.truffle.r.runtime.nmath.distr.PGamma;
import com.oracle.truffle.r.runtime.nmath.distr.PHyper;
import com.oracle.truffle.r.runtime.nmath.distr.PNBeta;
import com.oracle.truffle.r.runtime.nmath.distr.PNBinom.PNBinomFunc;
import com.oracle.truffle.r.runtime.nmath.distr.PNBinom.PNBinomMu;
import com.oracle.truffle.r.runtime.nmath.distr.PNChisq;
import com.oracle.truffle.r.runtime.nmath.distr.PPois;
import com.oracle.truffle.r.runtime.nmath.distr.PTukey;
import com.oracle.truffle.r.runtime.nmath.distr.Pbeta;
import com.oracle.truffle.r.runtime.nmath.distr.Pbinom;
import com.oracle.truffle.r.runtime.nmath.distr.Pf;
import com.oracle.truffle.r.runtime.nmath.distr.Pnf;
import com.oracle.truffle.r.runtime.nmath.distr.Pnorm;
import com.oracle.truffle.r.runtime.nmath.distr.Pnt;
import com.oracle.truffle.r.runtime.nmath.distr.Pt;
import com.oracle.truffle.r.runtime.nmath.distr.QBeta;
import com.oracle.truffle.r.runtime.nmath.distr.QGamma;
import com.oracle.truffle.r.runtime.nmath.distr.QHyper;
import com.oracle.truffle.r.runtime.nmath.distr.QNBeta;
import com.oracle.truffle.r.runtime.nmath.distr.QNBinom.QNBinomFunc;
import com.oracle.truffle.r.runtime.nmath.distr.QNBinom.QNBinomMu;
import com.oracle.truffle.r.runtime.nmath.distr.QNChisq;
import com.oracle.truffle.r.runtime.nmath.distr.QPois;
import com.oracle.truffle.r.runtime.nmath.distr.QTukey;
import com.oracle.truffle.r.runtime.nmath.distr.Qbinom;
import com.oracle.truffle.r.runtime.nmath.distr.Qf;
import com.oracle.truffle.r.runtime.nmath.distr.Qnf;
import com.oracle.truffle.r.runtime.nmath.distr.Qnorm;
import com.oracle.truffle.r.runtime.nmath.distr.Qnt;
import com.oracle.truffle.r.runtime.nmath.distr.Qt;
import com.oracle.truffle.r.runtime.nmath.distr.RBeta;
import com.oracle.truffle.r.runtime.nmath.distr.RGamma;
import com.oracle.truffle.r.runtime.nmath.distr.RHyper;
import com.oracle.truffle.r.runtime.nmath.distr.RNBinom.RNBinomFunc;
import com.oracle.truffle.r.runtime.nmath.distr.RNBinom.RNBinomMu;
import com.oracle.truffle.r.runtime.nmath.distr.RNchisq;
import com.oracle.truffle.r.runtime.nmath.distr.RPois;
import com.oracle.truffle.r.runtime.nmath.distr.Rbinom;
import com.oracle.truffle.r.runtime.nmath.distr.Rf;
import com.oracle.truffle.r.runtime.nmath.distr.Rnorm;
import com.oracle.truffle.r.runtime.nmath.distr.Rt;
import com.oracle.truffle.r.runtime.nmath.distr.Signrank.DSignrank;
import com.oracle.truffle.r.runtime.nmath.distr.Signrank.PSignrank;
import com.oracle.truffle.r.runtime.nmath.distr.Signrank.QSignrank;
import com.oracle.truffle.r.runtime.nmath.distr.Signrank.RSignrank;
import com.oracle.truffle.r.runtime.nmath.distr.Unif.DUnif;
import com.oracle.truffle.r.runtime.nmath.distr.Unif.PUnif;
import com.oracle.truffle.r.runtime.nmath.distr.Unif.QUnif;
import com.oracle.truffle.r.runtime.nmath.distr.Unif.Runif;
import com.oracle.truffle.r.runtime.nmath.distr.Weibull.DWeibull;
import com.oracle.truffle.r.runtime.nmath.distr.Weibull.PWeibull;
import com.oracle.truffle.r.runtime.nmath.distr.Weibull.QWeibull;
import com.oracle.truffle.r.runtime.nmath.distr.Weibull.RWeibull;
import com.oracle.truffle.r.runtime.nmath.distr.Wilcox.DWilcox;
import com.oracle.truffle.r.runtime.nmath.distr.Wilcox.PWilcox;
import com.oracle.truffle.r.runtime.nmath.distr.Wilcox.QWilcox;
import com.oracle.truffle.r.runtime.nmath.distr.Wilcox.RWilcox;

/**
 * {@code .Call}, {@code .Call.graphics}, {@code .External}, {@code .External2},
 * {@code External.graphics} functions, which share a common signature.
 *
 * TODO Completeness (more types, more error checks), Performance (copying). Especially all the
 * subtleties around copying.
 *
 * See <a href="https://stat.ethz.ch/R-manual/R-devel/library/base/html/Foreign.html">here</a>.
 */
public class CallAndExternalFunctions {

    @TruffleBoundary
    private static Object encodeArgumentPairList(RArgsValuesAndNames args, String symbolName) {
        Object list = RNull.instance;
        for (int i = args.getLength() - 1; i >= 0; i--) {
            String name = args.getSignature().getName(i);
            list = RDataFactory.createPairList(args.getArgument(i), list, name == null ? RNull.instance : RDataFactory.createSymbolInterned(name));
        }
        list = RDataFactory.createPairList(symbolName, list);
        return list;
    }

    /**
     * Handles the generic case, but also many special case functions that are called from the
     * default packages.
     *
     * The native function to be called can be specified in two ways:
     * <ol>
     * <li>as an object of R class {@code NativeSymbolInfo} (passed as an {@link RList}. In this
     * case {@code .PACKAGE} is ignored even if provided.</li>
     * <li>as a character string. If {@code .PACKAGE} is provided the search is restricted to that
     * package, else the symbol is searched in all loaded packages (evidently dangerous as the
     * symbol could be duplicated)</li>
     * </ol>
     * Many of the functions in the builtin packages have been translated to Java which is handled
     * by specializations that {@link #lookupBuiltin(RList)}. N.N. In principle such a function
     * could be invoked by a string but experimentally that situation has never been encountered.
     */
    @RBuiltin(name = ".Call", kind = PRIMITIVE, parameterNames = {".NAME", "...", "PACKAGE"}, behavior = COMPLEX)
    public abstract static class DotCall extends LookupAdapter {

        @Child private CallRFFI.InvokeCallNode callRFFINode = RFFIFactory.getCallRFFI().createInvokeCallNode();
        @Child private MaterializeNode materializeNode = MaterializeNode.create(true);

        static {
            Casts.noCasts(DotCall.class);
        }

        @Override
        public Object[] getDefaultParameterValues() {
            return new Object[]{RMissing.instance, RArgsValuesAndNames.EMPTY, RMissing.instance};
        }

        private Object[] materializeArgs(Object[] args) {
            Object[] materializedArgs = new Object[args.length];
            for (int i = 0; i < args.length; i++) {
                materializedArgs[i] = materializeNode.execute(args[i]);
            }
            return materializedArgs;
        }

        @Override
        @TruffleBoundary
        public RExternalBuiltinNode lookupBuiltin(RList symbol) {
            String name = lookupName(symbol);
            if (FastROptions.UseInternalGridGraphics.getBooleanValue() && name != null) {
                RExternalBuiltinNode gridExternal = FastRGridExternalLookup.lookupDotCall(name);
                if (gridExternal != null) {
                    return gridExternal;
                }
            }
            switch (name) {
                // methods
                case "R_initMethodDispatch":
                    return R_initMethodDispatchNodeGen.create();
                case "R_methodsPackageMetaName":
                    return R_methodsPackageMetaNameNodeGen.create();
                case "R_set_method_dispatch":
                    return R_set_method_dispatchNodeGen.create();
                case "R_M_setPrimitiveMethods":
                    return R_M_setPrimitiveMethodsNodeGen.create();
                case "R_getClassFromCache":
                    return R_getClassFromCacheNodeGen.create();
                case "R_clear_method_selection":
                case "R_dummy_extern_place":
                case "R_el_named":
                    return new UnimplementedExternal(name);
                case "R_externalptr_prototype_object":
                    return R_externalPtrPrototypeObjectNodeGen.create();
                case "R_getGeneric":
                    return R_getGenericNodeGen.create();
                case "R_get_slot":
                    return R_getSlotNodeGen.create();
                case "R_hasSlot":
                    return R_hasSlotNodeGen.create();
                case "R_identC":
                    return R_identCNodeGen.create();
                case "R_methods_test_MAKE_CLASS":
                case "R_methods_test_NEW":
                case "R_missingArg":
                case "R_nextMethodCall":
                    return R_nextMethodCallNodeGen.create();
                case "R_quick_method_check":
                case "R_selectMethod":
                case "R_set_el_named":
                    return new UnimplementedExternal(name);
                case "R_set_slot":
                    return R_setSlotNodeGen.create();
                case "R_standardGeneric":
                    return new UnimplementedExternal(name);
                case "do_substitute_direct":
                    return SubstituteDirectNodeGen.create();
                case "Rf_allocS4Object":
                    return new UnimplementedExternal(name);
                case "R_get_primname":
                    return GetPrimNameNodeGen.create();
                case "new_object":
                    return NewObjectNodeGen.create();

                // stats

                case "fft":
                    return FftNodeGen.create();
                case "cov":
                    return CovcorNodeGen.create(false);
                case "cor":
                    return CovcorNodeGen.create(true);
                case "SplineCoef":
                    return SplineCoefNodeGen.create();
                case "SplineEval":
                    return SplineEvalNodeGen.create();
                case "pnorm":
                    return StatsFunctionsNodes.Function3_2Node.create(new Pnorm());
                case "qnorm":
                    return StatsFunctionsNodes.Function3_2Node.create(new Qnorm());
                case "rnorm":
                    return RandFunction2Node.createDouble(new Rnorm());
                case "runif":
                    return RandFunction2Node.createDouble(new Runif());
                case "rbeta":
                    return RandFunction2Node.createDouble(new RBeta());
                case "rgamma":
                    return RandFunction2Node.createDouble(new RGamma());
                case "rcauchy":
                    return RandFunction2Node.createDouble(new RCauchy());
                case "rf":
                    return RandFunction2Node.createDouble(new Rf());
                case "rlogis":
                    return RandFunction2Node.createDouble(new RLogis());
                case "rweibull":
                    return RandFunction2Node.createDouble(new RWeibull());
                case "rnchisq":
                    return RandFunction2Node.createDouble(new RNchisq());
                case "rnbinom_mu":
                    return RandFunction2Node.createDouble(new RNBinomMu());
                case "rwilcox":
                    return RandFunction2Node.createInt(new RWilcox());
                case "rchisq":
                    return RandFunction1Node.createDouble(new RChisq());
                case "rexp":
                    return RandFunction1Node.createDouble(new RExp());
                case "rgeom":
                    return RandFunction1Node.createInt(new RGeom());
                case "rpois":
                    return RandFunction1Node.createInt(new RPois());
                case "rnbinom":
                    return RandFunction2Node.createInt(new RNBinomFunc());
                case "rt":
                    return RandFunction1Node.createDouble(new Rt());
                case "rsignrank":
                    return RandFunction1Node.createInt(new RSignrank());
                case "rhyper":
                    return RandFunction3Node.createInt(new RHyper());
                case "phyper":
                    return StatsFunctionsNodes.Function4_2Node.create(new PHyper());
                case "dhyper":
                    return StatsFunctionsNodes.Function4_1Node.create(new DHyper());
                case "qhyper":
                    return StatsFunctionsNodes.Function4_2Node.create(new QHyper());
                case "pnchisq":
                    return StatsFunctionsNodes.Function3_2Node.create(new PNChisq());
                case "qnchisq":
                    return StatsFunctionsNodes.Function3_2Node.create(new QNChisq());
                case "dnchisq":
                    return StatsFunctionsNodes.Function3_1Node.create(new DNChisq());
                case "qt":
                    return StatsFunctionsNodes.Function2_2Node.create(new Qt());
                case "pt":
                    return StatsFunctionsNodes.Function2_2Node.create(new Pt());
                case "qgamma":
                    return StatsFunctionsNodes.Function3_2Node.create(new QGamma());
                case "dbinom":
                    return StatsFunctionsNodes.Function3_1Node.create(new Dbinom());
                case "qbinom":
                    return StatsFunctionsNodes.Function3_2Node.create(new Qbinom());
                case "punif":
                    return StatsFunctionsNodes.Function3_2Node.create(new PUnif());
                case "dunif":
                    return StatsFunctionsNodes.Function3_1Node.create(new DUnif());
                case "qunif":
                    return StatsFunctionsNodes.Function3_2Node.create(new QUnif());
                case "ppois":
                    return StatsFunctionsNodes.Function2_2Node.create(new PPois());
                case "qpois":
                    return StatsFunctionsNodes.Function2_2Node.create(new QPois());
                case "qweibull":
                    return StatsFunctionsNodes.Function3_2Node.create(new QWeibull());
                case "pweibull":
                    return StatsFunctionsNodes.Function3_2Node.create(new PWeibull());
                case "dweibull":
                    return StatsFunctionsNodes.Function3_1Node.create(new DWeibull());
                case "rbinom":
                    return RandFunction2Node.createInt(new Rbinom());
                case "pbinom":
                    return StatsFunctionsNodes.Function3_2Node.create(new Pbinom());
                case "pbeta":
                    return StatsFunctionsNodes.Function3_2Node.create(new Pbeta());
                case "qbeta":
                    return StatsFunctionsNodes.Function3_2Node.create(new QBeta());
                case "dcauchy":
                    return StatsFunctionsNodes.Function3_1Node.create(new DCauchy());
                case "pcauchy":
                    return StatsFunctionsNodes.Function3_2Node.create(new PCauchy());
                case "qcauchy":
                    return StatsFunctionsNodes.Function3_2Node.create(new Cauchy.QCauchy());
                case "pf":
                    return StatsFunctionsNodes.Function3_2Node.create(new Pf());
                case "qf":
                    return StatsFunctionsNodes.Function3_2Node.create(new Qf());
                case "df":
                    return StatsFunctionsNodes.Function3_1Node.create(new Df());
                case "dgamma":
                    return StatsFunctionsNodes.Function3_1Node.create(new DGamma());
                case "pgamma":
                    return StatsFunctionsNodes.Function3_2Node.create(new PGamma());
                case "dchisq":
                    return StatsFunctionsNodes.Function2_1Node.create(new Chisq.DChisq());
                case "qchisq":
                    return StatsFunctionsNodes.Function2_2Node.create(new Chisq.QChisq());
                case "qgeom":
                    return StatsFunctionsNodes.Function2_2Node.create(new Geom.QGeom());
                case "pchisq":
                    return StatsFunctionsNodes.Function2_2Node.create(new Chisq.PChisq());
                case "dexp":
                    return StatsFunctionsNodes.Function2_1Node.create(new DExp());
                case "pexp":
                    return StatsFunctionsNodes.Function2_2Node.create(new PExp());
                case "qexp":
                    return StatsFunctionsNodes.Function2_2Node.create(new QExp());
                case "dgeom":
                    return StatsFunctionsNodes.Function2_1Node.create(new DGeom());
                case "dpois":
                    return StatsFunctionsNodes.Function2_1Node.create(new DPois());
                case "dbeta":
                    return StatsFunctionsNodes.Function3_1Node.create(new DBeta());
                case "dnbeta":
                    return StatsFunctionsNodes.Function4_1Node.create(new DNBeta());
                case "qnbeta":
                    return StatsFunctionsNodes.Function4_2Node.create(new QNBeta());
                case "dnf":
                    return StatsFunctionsNodes.Function4_1Node.create(new Dnf());
                case "qnf":
                    return StatsFunctionsNodes.Function4_2Node.create(new Qnf());
                case "pnf":
                    return StatsFunctionsNodes.Function4_2Node.create(new Pnf());
                case "pnbeta":
                    return StatsFunctionsNodes.Function4_2Node.create(new PNBeta());
                case "dt":
                    return StatsFunctionsNodes.Function2_1Node.create(new Dt());
                case "rlnorm":
                    return RandFunction2Node.createDouble(new LogNormal.RLNorm());
                case "dlnorm":
                    return StatsFunctionsNodes.Function3_1Node.create(new DLNorm());
                case "qlnorm":
                    return StatsFunctionsNodes.Function3_2Node.create(new QLNorm());
                case "plnorm":
                    return StatsFunctionsNodes.Function3_2Node.create(new PLNorm());
                case "dlogis":
                    return StatsFunctionsNodes.Function3_1Node.create(new DLogis());
                case "qlogis":
                    return StatsFunctionsNodes.Function3_2Node.create(new Logis.QLogis());
                case "plogis":
                    return StatsFunctionsNodes.Function3_2Node.create(new Logis.PLogis());
                case "pgeom":
                    return StatsFunctionsNodes.Function2_2Node.create(new Geom.PGeom());
                case "qnbinom":
                    return StatsFunctionsNodes.Function3_2Node.create(new QNBinomFunc());
                case "dnbinom":
                    return StatsFunctionsNodes.Function3_1Node.create(new DNBinomFunc());
                case "pnbinom":
                    return StatsFunctionsNodes.Function3_2Node.create(new PNBinomFunc());
                case "qnbinom_mu":
                    return StatsFunctionsNodes.Function3_2Node.create(new QNBinomMu());
                case "dnbinom_mu":
                    return StatsFunctionsNodes.Function3_1Node.create(new DNBinomMu());
                case "pnbinom_mu":
                    return StatsFunctionsNodes.Function3_2Node.create(new PNBinomMu());
                case "qwilcox":
                    return StatsFunctionsNodes.Function3_2Node.create(new QWilcox());
                case "pwilcox":
                    return StatsFunctionsNodes.Function3_2Node.create(new PWilcox());
                case "dwilcox":
                    return StatsFunctionsNodes.Function3_1Node.create(new DWilcox());
                case "dsignrank":
                    return StatsFunctionsNodes.Function2_1Node.create(new DSignrank());
                case "psignrank":
                    return StatsFunctionsNodes.Function2_2Node.create(new PSignrank());
                case "dnt":
                    return StatsFunctionsNodes.Function3_1Node.create(new Dnt());
                case "pnt":
                    return StatsFunctionsNodes.Function3_2Node.create(new Pnt());
                case "qnt":
                    return StatsFunctionsNodes.Function3_2Node.create(new Qnt());
                case "qsignrank":
                    return StatsFunctionsNodes.Function2_2Node.create(new QSignrank());
                case "qtukey":
                    return StatsFunctionsNodes.Function4_2Node.create(new QTukey());
                case "ptukey":
                    return StatsFunctionsNodes.Function4_2Node.create(new PTukey());
                case "rmultinom":
                    return RMultinomNode.create();
                case "Approx":
                    return StatsFunctionsNodes.Approx.create();
                case "ApproxTest":
                    return StatsFunctionsNodes.ApproxTest.create();
                case "Cdist":
                    return CdistNodeGen.create();
                case "DoubleCentre":
                    return DoubleCentreNodeGen.create();
                case "cutree":
                    return CutreeNodeGen.create();
                case "BinDist":
                    return BinDist.create();
                case "isoreg":
                case "monoFC_m":
                case "numeric_deriv":
                case "nls_iter":
                case "setup_starma":
                case "free_starma":
                case "set_trans":
                case "arma0fa":
                case "get_s2":
                case "get_resid":
                case "Dotrans":
                case "arma0_kfore":
                case "Starma_method":
                case "Invtrans":
                case "Gradtrans":
                case "ARMAtoMA":
                case "KalmanLike":
                case "KalmanFore":
                case "KalmanSmooth":
                case "ARIMA_undoPars":
                case "ARIMA_transPars":
                case "ARIMA_Invtrans":
                case "ARIMA_Gradtrans":
                case "ARIMA_Like":
                case "ARIMA_CSS":
                case "TSconv":
                case "getQ0":
                case "getQ0bis":
                case "port_ivset":
                case "port_nlminb":
                case "port_nlsb":
                case "logit_link":
                case "logit_linkinv":
                case "logit_mu_eta":
                case "binomial_dev_resids":
                case "rWishart":
                case "mvfft":
                case "nextn":
                case "r2dtable":
                case "cfilter":
                case "rfilter":
                case "lowess":
                case "Rsm":
                case "tukeyline":
                case "runmed":
                case "influence":
                case "pSmirnov2x":
                case "pKolmogorov2x":
                case "pKS2":
                case "ksmooth":
                case "LogLin":
                case "pAnsari":
                case "qAnsari":
                case "pKendall":
                case "pRho":
                case "SWilk":
                case "bw_den":
                case "bw_ucv":
                case "bw_bcv":
                case "bw_phi4":
                case "bw_phi6":
                case "acf":
                case "pacf1":
                case "ar2ma":
                case "Burg":
                case "intgrt_vec":
                    return PPSum.IntgrtVecNode.create();
                case "pp_sum":
                case "Fexact":
                case "Fisher_sim":
                case "chisq_sim":
                case "d2x2xk":
                    return new UnimplementedExternal(name);

                case "updateform":
                    return getExternalModelBuiltinNode("updateform");

                case "Cdqrls":
                    return new RInternalCodeBuiltinNode("stats", RInternalCode.loadSourceRelativeTo(RandFunctionsNodes.class, "lm.R"), "Cdqrls");

                case "dnorm":
                    return StatsFunctionsNodes.Function3_1Node.create(new DNorm());

                // tools
                case "doTabExpand":
                    return DoTabExpandNodeGen.create();
                case "codeFilesAppend":
                    return CodeFilesAppendNodeGen.create();
                case "Rmd5":
                    return Rmd5NodeGen.create();
                case "dirchmod":
                    return DirChmodNodeGen.create();
                case "delim_match":
                case "C_getfmts":
                case "check_nonASCII":
                case "check_nonASCII2":
                case "ps_kill":
                case "ps_sigs":
                case "ps_priority":
                case "startHTTPD":
                case "stopHTTPD":
                case "C_deparseRd":
                    return new UnimplementedExternal(name);

                // utils
                case "crc64":
                    return Crc64NodeGen.create();
                case "flushconsole":
                    return new Flushconsole();
                case "menu":
                    return MenuNodeGen.create();
                case "nsl":
                    return new UnimplementedExternal(name);
                case "objectSize":
                    return ObjectSizeNodeGen.create();
                case "processevents":
                case "octsize":
                case "sockconnect":
                case "sockread":
                case "sockclose":
                case "sockopen":
                case "socklisten":
                case "sockwrite":
                    return new UnimplementedExternal(name);

                // parallel
                case "mc_is_child":
                    return MCIsChildNodeGen.create();
                default:
                    return null;
            }
        }

        /**
         * {@code .NAME = NativeSymbolInfo} implemented as a builtin.
         */
        @SuppressWarnings("unused")
        @Specialization(limit = "99", guards = {"cached == symbol", "builtin != null"})
        protected Object doExternal(VirtualFrame frame, RList symbol, RArgsValuesAndNames args, Object packageName,
                        @Cached("symbol") RList cached,
                        @Cached("lookupBuiltin(symbol)") RExternalBuiltinNode builtin) {
            return builtin.call(frame, args);
        }

        /**
         * {@code .NAME = NativeSymbolInfo} implementation remains in native code (e.g. non-builtin
         * package)
         */
        @SuppressWarnings("unused")
        @Specialization(limit = "2", guards = {"cached == symbol", "builtin == null"})
        protected Object callNamedFunction(RList symbol, RArgsValuesAndNames args, Object packageName,
                        @Cached("symbol") RList cached,
                        @Cached("lookupBuiltin(symbol)") RExternalBuiltinNode builtin,
                        @Cached("new()") ExtractNativeCallInfoNode extractSymbolInfo,
                        @Cached("extractSymbolInfo.execute(symbol)") NativeCallInfo nativeCallInfo) {
            return callRFFINode.dispatch(nativeCallInfo, materializeArgs(args.getArguments()));
        }

        /**
         * For some reason, the list instance may change, although it carries the same info. For
         * such cases there is this generic version.
         */
        @Specialization(replaces = {"callNamedFunction", "doExternal"})
        protected Object callNamedFunctionGeneric(RList symbol, RArgsValuesAndNames args, @SuppressWarnings("unused") Object packageName,
                        @Cached("new()") ExtractNativeCallInfoNode extractSymbolInfo) {
            RExternalBuiltinNode builtin = lookupBuiltin(symbol);
            if (builtin != null) {
                throw RInternalError.shouldNotReachHere("Cache for .Calls with FastR reimplementation (lookupBuiltin(...) != null) exceeded the limit");
            }
            NativeCallInfo nativeCallInfo = extractSymbolInfo.execute(symbol);
            return callRFFINode.dispatch(nativeCallInfo, materializeArgs(args.getArguments()));
        }

        /**
         * {@code .NAME = string}, no package specified.
         */
        @Specialization
        protected Object callNamedFunction(String symbol, RArgsValuesAndNames args, @SuppressWarnings("unused") RMissing packageName,
                        @Cached("createRegisteredNativeSymbol(CallNST)") DLL.RegisteredNativeSymbol rns,
                        @Cached("create()") DLL.RFindSymbolNode findSymbolNode) {
            return callNamedFunctionWithPackage(symbol, args, null, rns, findSymbolNode);
        }

        /**
         * {@code .NAME = string, .PACKAGE = package}. An error if package provided and it does not
         * define that symbol.
         */
        @Specialization
        protected Object callNamedFunctionWithPackage(String symbol, RArgsValuesAndNames args, String packageName,
                        @Cached("createRegisteredNativeSymbol(CallNST)") DLL.RegisteredNativeSymbol rns,
                        @Cached("create()") DLL.RFindSymbolNode findSymbolNode) {
            DLL.SymbolHandle func = findSymbolNode.execute(symbol, packageName, rns);
            if (func == DLL.SYMBOL_NOT_FOUND) {
                throw error(RError.Message.SYMBOL_NOT_IN_TABLE, symbol, "Call", packageName);
            }
            return callRFFINode.dispatch(new NativeCallInfo(symbol, func, rns.getDllInfo()), materializeArgs(args.getArguments()));
        }

        @Specialization
        protected Object callNamedFunctionWithPackage(RExternalPtr symbol, RArgsValuesAndNames args, @SuppressWarnings("unused") RMissing packageName) {
            return callRFFINode.dispatch(new NativeCallInfo("", symbol.getAddr(), null), materializeArgs(args.getArguments()));
        }

        @SuppressWarnings("unused")
        @Fallback
        protected Object dotCallFallback(Object symbol, Object args, Object packageName) {
            throw fallback(this, symbol);
        }
    }

    /**
     * The interpretation of the {@code .NAME} and {code .PACKAGE} arguments as are for
     * {@link DotCall}.
     */
    @RBuiltin(name = ".External", kind = PRIMITIVE, parameterNames = {".NAME", "...", "PACKAGE"}, behavior = COMPLEX)
    public abstract static class DotExternal extends LookupAdapter {

        @Child private CallRFFI.InvokeCallNode callRFFINode = RFFIFactory.getCallRFFI().createInvokeCallNode();

        static {
            Casts.noCasts(DotExternal.class);
        }

        @Override
        @TruffleBoundary
        public RExternalBuiltinNode lookupBuiltin(RList f) {
            String name = lookupName(f);
            if (FastROptions.UseInternalGridGraphics.getBooleanValue()) {
                RExternalBuiltinNode gridExternal = FastRGridExternalLookup.lookupDotExternal(name);
                if (gridExternal != null) {
                    return gridExternal;
                }
            }
            switch (name) {
                case "compcases":
                    return new CompleteCases();
                // stats
                case "doD":
                    return D.create();
                case "deriv":
                    return Deriv.create();
                // utils
                case "countfields":
                    return CountFieldsNodeGen.create();
                case "readtablehead":
                    return ReadTableHeadNodeGen.create();
                case "download":
                    return DownloadNodeGen.create();
                case "termsform":
                    return getExternalModelBuiltinNode("termsform");
                case "Rprof":
                    return RprofNodeGen.create();
                case "Rprofmem":
                    return RprofmemNodeGen.create();
                case "wilcox_free":
                    return new WilcoxFreeNode();
                case "signrank_free":
                    return new SignrankFreeNode();
                case "unzip":
                    return UnzipNodeGen.create();
                case "addhistory":
                case "loadhistory":
                case "savehistory":
                case "dataentry":
                case "dataviewer":
                case "edit":
                case "fileedit":
                case "selectlist":
                    return new UnimplementedExternal(name);
                default:
                    return null;
            }
        }

        @SuppressWarnings("unused")
        @Specialization(limit = "1", guards = {"cached == symbol", "builtin != null"})
        protected Object doExternal(VirtualFrame frame, RList symbol, RArgsValuesAndNames args, Object packageName,
                        @Cached("symbol") RList cached,
                        @Cached("lookupBuiltin(symbol)") RExternalBuiltinNode builtin) {
            return builtin.call(frame, args);
        }

        @SuppressWarnings("unused")
        @Specialization(limit = "2", guards = {"cached == symbol"})// limit="2" because of DSL bug
        protected Object callNamedFunction(RList symbol, RArgsValuesAndNames args, Object packageName,
                        @Cached("symbol") RList cached,
                        @Cached("new()") ExtractNativeCallInfoNode extractSymbolInfo,
                        @Cached("extractSymbolInfo.execute(symbol)") NativeCallInfo nativeCallInfo) {
            Object list = encodeArgumentPairList(args, nativeCallInfo.name);
            return callRFFINode.dispatch(nativeCallInfo, new Object[]{list});
        }

        @Specialization
        protected Object callNamedFunction(String symbol, RArgsValuesAndNames args, @SuppressWarnings("unused") RMissing packageName,
                        @Cached("createRegisteredNativeSymbol(ExternalNST)") DLL.RegisteredNativeSymbol rns,
                        @Cached("create()") DLL.RFindSymbolNode findSymbolNode) {
            return callNamedFunctionWithPackage(symbol, args, null, rns, findSymbolNode);
        }

        @Specialization
        protected Object callNamedFunctionWithPackage(String symbol, RArgsValuesAndNames args, String packageName,
                        @Cached("createRegisteredNativeSymbol(ExternalNST)") DLL.RegisteredNativeSymbol rns,
                        @Cached("create()") DLL.RFindSymbolNode findSymbolNode) {
            DLL.SymbolHandle func = findSymbolNode.execute(symbol, packageName, rns);
            if (func == DLL.SYMBOL_NOT_FOUND) {
                throw error(RError.Message.SYMBOL_NOT_IN_TABLE, symbol, "External", packageName);
            }
            Object list = encodeArgumentPairList(args, symbol);
            return callRFFINode.dispatch(new NativeCallInfo(symbol, func, rns.getDllInfo()), new Object[]{list});
        }

        @Fallback
        protected Object fallback(Object f, @SuppressWarnings("unused") Object args, @SuppressWarnings("unused") Object packageName) {
            throw fallback(this, f);
        }
    }

    @RBuiltin(name = ".External2", visibility = CUSTOM, kind = PRIMITIVE, parameterNames = {".NAME", "...", "PACKAGE"}, behavior = COMPLEX)
    public abstract static class DotExternal2 extends LookupAdapter {
        private static final Object CALL = "call";
        private static final Object RHO = "rho";
        /**
         * This argument for the native function should be SPECIALSXP reprenting the .External2
         * builtin. In GnuR SPECIALSXP is index into the table of builtins. External2 and External
         * are in fact one native function with two entries in this table, the "op" argument is used
         * to determine whether the call was made to .External or .External2. The actual code of the
         * native function that is eventually invoked will always get SPECIALSXP reprenting the
         * .External2, becuase functions exported as .External do not take the "op" argument.
         */
        @CompilationFinal private Object op = null;

        @Child private CallRFFI.InvokeCallNode callRFFINode = RFFIFactory.getCallRFFI().createInvokeCallNode();

        static {
            Casts.noCasts(DotExternal2.class);
        }

        private Object getOp() {
            if (op == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                op = RContext.getInstance().lookupBuiltin(".External2");
            }
            return op;
        }

        @Override
        @TruffleBoundary
        public RExternalBuiltinNode lookupBuiltin(RList symbol) {
            String name = lookupName(symbol);
            if (FastROptions.UseInternalGridGraphics.getBooleanValue()) {
                RExternalBuiltinNode gridExternal = FastRGridExternalLookup.lookupDotExternal2(name);
                if (gridExternal != null) {
                    return gridExternal;
                }
            }
            switch (name) {
                // tools
                case "writetable":
                    return WriteTableNodeGen.create();
                case "typeconvert":
                    return TypeConvertNodeGen.create();
                case "C_parseRd":
                    return C_ParseRdNodeGen.create();
                case "modelmatrix":
                case "modelframe":
                    return getExternalModelBuiltinNode(name);
                default:
                    return null;
            }
        }

        @SuppressWarnings("unused")
        @Specialization(limit = "1", guards = {"cached == symbol", "builtin != null"})
        protected Object doExternal(VirtualFrame frame, RList symbol, RArgsValuesAndNames args, Object packageName,
                        @Cached("symbol") RList cached,
                        @Cached("lookupBuiltin(symbol)") RExternalBuiltinNode builtin) {
            return builtin.call(frame, args);
        }

        @SuppressWarnings("unused")
        @Specialization(limit = "2", guards = {"cached == symbol"}) // limit="2" because of DSL bug
        protected Object callNamedFunction(RList symbol, RArgsValuesAndNames args, Object packageName,
                        @Cached("symbol") RList cached,
                        @Cached("new()") ExtractNativeCallInfoNode extractSymbolInfo,
                        @Cached("extractSymbolInfo.execute(symbol)") NativeCallInfo nativeCallInfo) {
            Object list = encodeArgumentPairList(args, nativeCallInfo.name);
            return callRFFINode.dispatch(nativeCallInfo, new Object[]{CALL, getOp(), list, RHO});
        }

        @Specialization
        protected Object callNamedFunction(String symbol, RArgsValuesAndNames args, @SuppressWarnings("unused") RMissing packageName,
                        @Cached("createRegisteredNativeSymbol(ExternalNST)") DLL.RegisteredNativeSymbol rns,
                        @Cached("create()") DLL.RFindSymbolNode findSymbolNode) {
            return callNamedFunctionWithPackage(symbol, args, null, rns, findSymbolNode);
        }

        @Specialization
        protected Object callNamedFunctionWithPackage(String symbol, RArgsValuesAndNames args, String packageName,
                        @Cached("createRegisteredNativeSymbol(ExternalNST)") DLL.RegisteredNativeSymbol rns,
                        @Cached("create()") DLL.RFindSymbolNode findSymbolNode) {
            DLL.SymbolHandle func = findSymbolNode.execute(symbol, packageName, rns);
            if (func == DLL.SYMBOL_NOT_FOUND) {
                throw error(RError.Message.SYMBOL_NOT_IN_TABLE, symbol, "External2", packageName);
            }
            Object list = encodeArgumentPairList(args, symbol);
            return callRFFINode.dispatch(new NativeCallInfo(symbol, func, rns.getDllInfo()), new Object[]{CALL, getOp(), list, RHO});
        }

        @Fallback
        protected Object fallback(Object f, @SuppressWarnings("unused") Object args, @SuppressWarnings("unused") Object packageName) {
            throw fallback(this, f);
        }
    }

    @RBuiltin(name = ".External.graphics", kind = PRIMITIVE, parameterNames = {".NAME", "...", "PACKAGE"}, behavior = COMPLEX)
    public abstract static class DotExternalGraphics extends LookupAdapter {

        @Child private CallRFFI.InvokeCallNode callRFFINode = RFFIFactory.getCallRFFI().createInvokeCallNode();

        static {
            Casts.noCasts(DotExternalGraphics.class);
        }

        @Override
        @TruffleBoundary
        public RExternalBuiltinNode lookupBuiltin(RList f) {
            return null;
        }

        @SuppressWarnings("unused")
        @Specialization(limit = "1", guards = {"cached == f", "builtin != null"})
        protected Object doExternal(VirtualFrame frame, RList f, RArgsValuesAndNames args, RMissing packageName,
                        @Cached("f") RList cached,
                        @Cached("lookupBuiltin(f)") RExternalBuiltinNode builtin) {
            return builtin.call(frame, args);
        }

        @Specialization
        protected Object callNamedFunction(RList symbol, RArgsValuesAndNames args, @SuppressWarnings("unused") Object packageName,
                        @Cached("new()") ExtractNativeCallInfoNode extractSymbolInfo) {
            NativeCallInfo nativeCallInfo = extractSymbolInfo.execute(symbol);
            Object list = encodeArgumentPairList(args, nativeCallInfo.name);
            return callRFFINode.dispatch(nativeCallInfo, new Object[]{list});
        }

        @Specialization
        protected Object callNamedFunction(String name, RArgsValuesAndNames args, @SuppressWarnings("unused") RMissing packageName,
                        @Cached("create()") DLL.RFindSymbolNode findSymbolNode) {
            return callNamedFunctionWithPackage(name, args, null, findSymbolNode);
        }

        @Specialization
        protected Object callNamedFunctionWithPackage(String name, RArgsValuesAndNames args, String packageName,
                        @Cached("create()") DLL.RFindSymbolNode findSymbolNode) {
            DLL.RegisteredNativeSymbol rns = new DLL.RegisteredNativeSymbol(DLL.NativeSymbolType.External, null, null);
            DLL.SymbolHandle func = findSymbolNode.execute(name, packageName, rns);
            if (func == DLL.SYMBOL_NOT_FOUND) {
                throw error(RError.Message.C_SYMBOL_NOT_IN_TABLE, name);
            }
            Object list = encodeArgumentPairList(args, name);
            return callRFFINode.dispatch(new NativeCallInfo(name, func, rns.getDllInfo()), new Object[]{list});
        }

        @Fallback
        protected Object fallback(Object f, @SuppressWarnings("unused") Object args, @SuppressWarnings("unused") Object packageName) {
            throw fallback(this, f);
        }
    }

    @RBuiltin(name = ".Call.graphics", kind = PRIMITIVE, parameterNames = {".NAME", "...", "PACKAGE"}, behavior = COMPLEX)
    public abstract static class DotCallGraphics extends LookupAdapter {

        @Child private CallRFFI.InvokeCallNode callRFFINode = RFFIFactory.getCallRFFI().createInvokeCallNode();

        static {
            Casts.noCasts(DotCallGraphics.class);
        }

        @Override
        public Object[] getDefaultParameterValues() {
            return new Object[]{RMissing.instance, RArgsValuesAndNames.EMPTY, RMissing.instance};
        }

        @Override
        @TruffleBoundary
        public RExternalBuiltinNode lookupBuiltin(RList f) {
            return FastRGridExternalLookup.lookupDotCallGraphics(lookupName(f));
        }

        @SuppressWarnings("unused")
        @Specialization(limit = "1", guards = {"cached == f", "builtin != null"})
        protected Object doExternal(VirtualFrame frame, RList f, RArgsValuesAndNames args, RMissing packageName,
                        @Cached("f") RList cached,
                        @Cached("lookupBuiltin(f)") RExternalBuiltinNode builtin) {
            return builtin.call(frame, args);
        }

        @Specialization
        protected Object callNamedFunction(RList symbol, RArgsValuesAndNames args, @SuppressWarnings("unused") Object packageName,
                        @Cached("new()") ExtractNativeCallInfoNode extractSymbolInfo) {
            NativeCallInfo nativeCallInfo = extractSymbolInfo.execute(symbol);
            return callRFFINode.dispatch(nativeCallInfo, args.getArguments());
        }

        @Specialization
        protected Object callNamedFunction(String name, RArgsValuesAndNames args, @SuppressWarnings("unused") RMissing packageName,
                        @Cached("create()") DLL.RFindSymbolNode findSymbolNode) {
            return callNamedFunctionWithPackage(name, args, null, findSymbolNode);
        }

        @Specialization
        @TruffleBoundary
        protected Object callNamedFunctionWithPackage(String name, RArgsValuesAndNames args, String packageName,
                        @Cached("create()") DLL.RFindSymbolNode findSymbolNode) {
            DLL.RegisteredNativeSymbol rns = new DLL.RegisteredNativeSymbol(DLL.NativeSymbolType.Call, null, null);
            DLL.SymbolHandle func = findSymbolNode.execute(name, packageName, rns);
            if (func == DLL.SYMBOL_NOT_FOUND) {
                throw error(RError.Message.C_SYMBOL_NOT_IN_TABLE, name);
            }
            return callRFFINode.dispatch(new NativeCallInfo(name, func, rns.getDllInfo()), args.getArguments());
        }

        @Fallback
        protected Object dotCallFallback(Object fobj, @SuppressWarnings("unused") Object args, @SuppressWarnings("unused") Object packageName) {
            throw fallback(this, fobj);
        }
    }
}
