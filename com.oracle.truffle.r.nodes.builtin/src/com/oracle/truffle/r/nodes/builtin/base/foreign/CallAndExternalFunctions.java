/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base.foreign;

import static com.oracle.truffle.r.runtime.RVisibility.CUSTOM;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.library.grDevices.DevicesCCalls;
import com.oracle.truffle.r.library.graphics.GraphicsCCalls;
import com.oracle.truffle.r.library.graphics.GraphicsCCalls.C_Par;
import com.oracle.truffle.r.library.graphics.GraphicsCCalls.C_PlotXY;
import com.oracle.truffle.r.library.grid.GridFunctionsFactory.InitGridNodeGen;
import com.oracle.truffle.r.library.grid.GridFunctionsFactory.ValidUnitsNodeGen;
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
import com.oracle.truffle.r.library.methods.SlotFactory.R_setSlotNodeGen;
import com.oracle.truffle.r.library.methods.SubstituteDirectNodeGen;
import com.oracle.truffle.r.library.parallel.ParallelFunctionsFactory.MCIsChildNodeGen;
import com.oracle.truffle.r.library.stats.Cauchy;
import com.oracle.truffle.r.library.stats.Cauchy.DCauchy;
import com.oracle.truffle.r.library.stats.Cauchy.PCauchy;
import com.oracle.truffle.r.library.stats.Cauchy.RCauchy;
import com.oracle.truffle.r.library.stats.CdistNodeGen;
import com.oracle.truffle.r.library.stats.Chisq;
import com.oracle.truffle.r.library.stats.Chisq.RChisq;
import com.oracle.truffle.r.library.stats.CompleteCases;
import com.oracle.truffle.r.library.stats.CovcorNodeGen;
import com.oracle.truffle.r.library.stats.CutreeNodeGen;
import com.oracle.truffle.r.library.stats.DBeta;
import com.oracle.truffle.r.library.stats.DHyper;
import com.oracle.truffle.r.library.stats.DNBeta;
import com.oracle.truffle.r.library.stats.DNChisq;
import com.oracle.truffle.r.library.stats.DNorm;
import com.oracle.truffle.r.library.stats.DPois;
import com.oracle.truffle.r.library.stats.Dbinom;
import com.oracle.truffle.r.library.stats.Df;
import com.oracle.truffle.r.library.stats.Dnf;
import com.oracle.truffle.r.library.stats.DoubleCentreNodeGen;
import com.oracle.truffle.r.library.stats.Dt;
import com.oracle.truffle.r.library.stats.Exp.DExp;
import com.oracle.truffle.r.library.stats.Exp.PExp;
import com.oracle.truffle.r.library.stats.Exp.QExp;
import com.oracle.truffle.r.library.stats.Exp.RExp;
import com.oracle.truffle.r.library.stats.GammaFunctions.DGamma;
import com.oracle.truffle.r.library.stats.GammaFunctions.QgammaFunc;
import com.oracle.truffle.r.library.stats.Geom;
import com.oracle.truffle.r.library.stats.Geom.DGeom;
import com.oracle.truffle.r.library.stats.Geom.RGeom;
import com.oracle.truffle.r.library.stats.LogNormal;
import com.oracle.truffle.r.library.stats.LogNormal.DLNorm;
import com.oracle.truffle.r.library.stats.LogNormal.PLNorm;
import com.oracle.truffle.r.library.stats.LogNormal.QLNorm;
import com.oracle.truffle.r.library.stats.Logis;
import com.oracle.truffle.r.library.stats.Logis.DLogis;
import com.oracle.truffle.r.library.stats.Logis.RLogis;
import com.oracle.truffle.r.library.stats.PGamma;
import com.oracle.truffle.r.library.stats.PHyper;
import com.oracle.truffle.r.library.stats.PNBeta;
import com.oracle.truffle.r.library.stats.PNChisq;
import com.oracle.truffle.r.library.stats.PPois;
import com.oracle.truffle.r.library.stats.Pbeta;
import com.oracle.truffle.r.library.stats.Pbinom;
import com.oracle.truffle.r.library.stats.Pf;
import com.oracle.truffle.r.library.stats.Pnf;
import com.oracle.truffle.r.library.stats.Pnorm;
import com.oracle.truffle.r.library.stats.Pt;
import com.oracle.truffle.r.library.stats.QBeta;
import com.oracle.truffle.r.library.stats.QHyper;
import com.oracle.truffle.r.library.stats.QNBeta;
import com.oracle.truffle.r.library.stats.QNChisq;
import com.oracle.truffle.r.library.stats.QPois;
import com.oracle.truffle.r.library.stats.Qbinom;
import com.oracle.truffle.r.library.stats.Qf;
import com.oracle.truffle.r.library.stats.Qnf;
import com.oracle.truffle.r.library.stats.Qnorm;
import com.oracle.truffle.r.library.stats.Qt;
import com.oracle.truffle.r.library.stats.RBeta;
import com.oracle.truffle.r.library.stats.RGamma;
import com.oracle.truffle.r.library.stats.RHyper;
import com.oracle.truffle.r.library.stats.RMultinomNodeGen;
import com.oracle.truffle.r.library.stats.RNbinomMu;
import com.oracle.truffle.r.library.stats.RNchisq;
import com.oracle.truffle.r.library.stats.RPois;
import com.oracle.truffle.r.library.stats.RWeibull;
import com.oracle.truffle.r.library.stats.RandGenerationFunctions;
import com.oracle.truffle.r.library.stats.RandGenerationFunctions.RandFunction1Node;
import com.oracle.truffle.r.library.stats.RandGenerationFunctions.RandFunction2Node;
import com.oracle.truffle.r.library.stats.RandGenerationFunctions.RandFunction3Node;
import com.oracle.truffle.r.library.stats.Rbinom;
import com.oracle.truffle.r.library.stats.Rf;
import com.oracle.truffle.r.library.stats.Rnorm;
import com.oracle.truffle.r.library.stats.Rt;
import com.oracle.truffle.r.library.stats.Signrank.RSignrank;
import com.oracle.truffle.r.library.stats.SplineFunctionsFactory.SplineCoefNodeGen;
import com.oracle.truffle.r.library.stats.SplineFunctionsFactory.SplineEvalNodeGen;
import com.oracle.truffle.r.library.stats.StatsFunctions;
import com.oracle.truffle.r.library.stats.StatsFunctionsFactory;
import com.oracle.truffle.r.library.stats.Unif.DUnif;
import com.oracle.truffle.r.library.stats.Unif.PUnif;
import com.oracle.truffle.r.library.stats.Unif.QUnif;
import com.oracle.truffle.r.library.stats.Unif.Runif;
import com.oracle.truffle.r.library.stats.Wilcox.RWilcox;
import com.oracle.truffle.r.library.tools.C_ParseRdNodeGen;
import com.oracle.truffle.r.library.tools.DirChmodNodeGen;
import com.oracle.truffle.r.library.tools.Rmd5NodeGen;
import com.oracle.truffle.r.library.tools.ToolsTextFactory.CodeFilesAppendNodeGen;
import com.oracle.truffle.r.library.tools.ToolsTextFactory.DoTabExpandNodeGen;
import com.oracle.truffle.r.library.utils.CountFields;
import com.oracle.truffle.r.library.utils.Crc64NodeGen;
import com.oracle.truffle.r.library.utils.DownloadNodeGen;
import com.oracle.truffle.r.library.utils.MenuNodeGen;
import com.oracle.truffle.r.library.utils.ObjectSizeNodeGen;
import com.oracle.truffle.r.library.utils.RprofNodeGen;
import com.oracle.truffle.r.library.utils.RprofmemNodeGen;
import com.oracle.truffle.r.library.utils.TypeConvertNodeGen;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.RInternalCodeBuiltinNode;
import com.oracle.truffle.r.nodes.objects.GetPrimNameNodeGen;
import com.oracle.truffle.r.nodes.objects.NewObjectNodeGen;
import com.oracle.truffle.r.runtime.FastROptions;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalCode;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.ffi.CallRFFI;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.NativeCallInfo;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;

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

    abstract static class CallRFFIAdapter extends LookupAdapter {
        @Child CallRFFI.CallRFFINode callRFFINode = RFFIFactory.getRFFI().getCallRFFI().createCallRFFINode();
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
    public abstract static class DotCall extends CallRFFIAdapter {

        private final BranchProfile errorProfile = BranchProfile.create();

        @Override
        public Object[] getDefaultParameterValues() {
            return new Object[]{RMissing.instance, RArgsValuesAndNames.EMPTY, RMissing.instance};
        }

        @Override
        @TruffleBoundary
        protected RExternalBuiltinNode lookupBuiltin(RList symbol) {
            String name = lookupName(symbol);
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
                    return new UnimplementedExternal(name);
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
                    return StatsFunctionsFactory.Function3_2NodeGen.create(new Pnorm());
                case "qnorm":
                    return StatsFunctionsFactory.Function3_2NodeGen.create(new Qnorm());
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
                    return RandFunction2Node.createDouble(new RNbinomMu());
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
                case "rt":
                    return RandFunction1Node.createDouble(new Rt());
                case "rsignrank":
                    return RandFunction1Node.createInt(new RSignrank());
                case "rhyper":
                    return RandFunction3Node.createInt(new RHyper());
                case "phyper":
                    return StatsFunctions.Function4_2Node.create(new PHyper());
                case "dhyper":
                    return StatsFunctions.Function4_1Node.create(new DHyper());
                case "qhyper":
                    return StatsFunctions.Function4_2Node.create(new QHyper());
                case "pnchisq":
                    return StatsFunctionsFactory.Function3_2NodeGen.create(new PNChisq());
                case "qnchisq":
                    return StatsFunctionsFactory.Function3_2NodeGen.create(new QNChisq());
                case "dnchisq":
                    return StatsFunctionsFactory.Function3_1NodeGen.create(new DNChisq());
                case "qt":
                    return StatsFunctionsFactory.Function2_2NodeGen.create(new Qt());
                case "pt":
                    return StatsFunctionsFactory.Function2_2NodeGen.create(new Pt());
                case "qgamma":
                    return StatsFunctionsFactory.Function3_2NodeGen.create(new QgammaFunc());
                case "dbinom":
                    return StatsFunctionsFactory.Function3_1NodeGen.create(new Dbinom());
                case "qbinom":
                    return StatsFunctionsFactory.Function3_2NodeGen.create(new Qbinom());
                case "punif":
                    return StatsFunctionsFactory.Function3_2NodeGen.create(new PUnif());
                case "dunif":
                    return StatsFunctionsFactory.Function3_1NodeGen.create(new DUnif());
                case "qunif":
                    return StatsFunctionsFactory.Function3_2NodeGen.create(new QUnif());
                case "ppois":
                    return StatsFunctionsFactory.Function2_2NodeGen.create(new PPois());
                case "qpois":
                    return StatsFunctionsFactory.Function2_2NodeGen.create(new QPois());
                case "rbinom":
                    return RandFunction2Node.createInt(new Rbinom());
                case "pbinom":
                    return StatsFunctionsFactory.Function3_2NodeGen.create(new Pbinom());
                case "pbeta":
                    return StatsFunctionsFactory.Function3_2NodeGen.create(new Pbeta());
                case "qbeta":
                    return StatsFunctionsFactory.Function3_2NodeGen.create(new QBeta());
                case "dcauchy":
                    return StatsFunctionsFactory.Function3_1NodeGen.create(new DCauchy());
                case "pcauchy":
                    return StatsFunctionsFactory.Function3_2NodeGen.create(new PCauchy());
                case "qcauchy":
                    return StatsFunctionsFactory.Function3_2NodeGen.create(new Cauchy.QCauchy());
                case "pf":
                    return StatsFunctionsFactory.Function3_2NodeGen.create(new Pf());
                case "qf":
                    return StatsFunctionsFactory.Function3_2NodeGen.create(new Qf());
                case "df":
                    return StatsFunctionsFactory.Function3_1NodeGen.create(new Df());
                case "dgamma":
                    return StatsFunctionsFactory.Function3_1NodeGen.create(new DGamma());
                case "pgamma":
                    return StatsFunctionsFactory.Function3_2NodeGen.create(new PGamma());
                case "dchisq":
                    return StatsFunctionsFactory.Function2_1NodeGen.create(new Chisq.DChisq());
                case "qchisq":
                    return StatsFunctionsFactory.Function2_2NodeGen.create(new Chisq.QChisq());
                case "qgeom":
                    return StatsFunctionsFactory.Function2_2NodeGen.create(new Geom.QGeom());
                case "pchisq":
                    return StatsFunctionsFactory.Function2_2NodeGen.create(new Chisq.PChisq());
                case "dexp":
                    return StatsFunctionsFactory.Function2_1NodeGen.create(new DExp());
                case "pexp":
                    return StatsFunctionsFactory.Function2_2NodeGen.create(new PExp());
                case "qexp":
                    return StatsFunctionsFactory.Function2_2NodeGen.create(new QExp());
                case "dgeom":
                    return StatsFunctionsFactory.Function2_1NodeGen.create(new DGeom());
                case "dpois":
                    return StatsFunctionsFactory.Function2_1NodeGen.create(new DPois());
                case "dbeta":
                    return StatsFunctionsFactory.Function3_1NodeGen.create(new DBeta());
                case "dnbeta":
                    return StatsFunctions.Function4_1Node.create(new DNBeta());
                case "qnbeta":
                    return StatsFunctions.Function4_2Node.create(new QNBeta());
                case "dnf":
                    return StatsFunctions.Function4_1Node.create(new Dnf());
                case "qnf":
                    return StatsFunctions.Function4_2Node.create(new Qnf());
                case "pnf":
                    return StatsFunctions.Function4_2Node.create(new Pnf());
                case "pnbeta":
                    return StatsFunctions.Function4_2Node.create(new PNBeta());
                case "dt":
                    return StatsFunctionsFactory.Function2_1NodeGen.create(new Dt());
                case "rlnorm":
                    return RandFunction2Node.createDouble(new LogNormal.RLNorm());
                case "dlnorm":
                    return StatsFunctionsFactory.Function3_1NodeGen.create(new DLNorm());
                case "qlnorm":
                    return StatsFunctionsFactory.Function3_2NodeGen.create(new QLNorm());
                case "plnorm":
                    return StatsFunctionsFactory.Function3_2NodeGen.create(new PLNorm());
                case "dlogis":
                    return StatsFunctionsFactory.Function3_1NodeGen.create(new DLogis());
                case "qlogis":
                    return StatsFunctionsFactory.Function3_2NodeGen.create(new Logis.QLogis());
                case "plogis":
                    return StatsFunctionsFactory.Function3_2NodeGen.create(new Logis.PLogis());
                case "pgeom":
                    return StatsFunctionsFactory.Function2_2NodeGen.create(new Geom.PGeom());
                case "rmultinom":
                    return RMultinomNodeGen.create();
                case "Approx":
                    return StatsFunctionsFactory.ApproxNodeGen.create();
                case "ApproxTest":
                    return StatsFunctionsFactory.ApproxTestNodeGen.create();
                case "Cdist":
                    return CdistNodeGen.create();
                case "DoubleCentre":
                    return DoubleCentreNodeGen.create();
                case "cutree":
                    return CutreeNodeGen.create();
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
                case "BinDist":
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
                case "pp_sum":
                case "Fexact":
                case "Fisher_sim":
                case "chisq_sim":
                case "d2x2xk":
                    return new UnimplementedExternal(name);

                case "updateform":
                    return getExternalModelBuiltinNode("updateform");

                case "Cdqrls":
                    return new RInternalCodeBuiltinNode(RContext.getInstance(), "stats", RInternalCode.loadSourceRelativeTo(RandGenerationFunctions.class, "lm.R"), "Cdqrls");

                case "dnorm":
                    return StatsFunctionsFactory.Function3_1NodeGen.create(new DNorm());

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

                // grDevices
                case "cairoProps":
                    return CairoPropsNodeGen.create();
                case "makeQuartzDefault":
                    return new MakeQuartzDefault();

                // grid
                case "L_initGrid":
                    return InitGridNodeGen.create();
                case "L_validUnits":
                    return ValidUnitsNodeGen.create();

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
        @Specialization(limit = "1", guards = {"cached == symbol", "builtin != null"})
        protected Object doExternal(VirtualFrame frame, RList symbol, RArgsValuesAndNames args, Object packageName, //
                        @Cached("symbol") RList cached, //
                        @Cached("lookupBuiltin(symbol)") RExternalBuiltinNode builtin) {
            return builtin.call(frame, args);
        }

        /**
         * {@code .NAME = NativeSymbolInfo} implementation remains in native code (e.g. non-builtin
         * package)
         */
        @SuppressWarnings("unused")
        @Specialization(limit = "1", guards = {"cached == symbol"})
        protected Object callNamedFunction(VirtualFrame frame, RList symbol, RArgsValuesAndNames args, Object packageName, //
                        @Cached("symbol") RList cached, //
                        @Cached("extractSymbolInfo(frame, symbol)") NativeCallInfo nativeCallInfo) {
            return callRFFINode.invokeCall(nativeCallInfo, args.getArguments());
        }

        /**
         * {@code .NAME = string}, no package specified.
         */
        @Specialization
        protected Object callNamedFunction(String symbol, RArgsValuesAndNames args, @SuppressWarnings("unused") RMissing packageName, //
                        @Cached("createRegisteredNativeSymbol(CallNST)") DLL.RegisteredNativeSymbol rns) {
            return callNamedFunctionWithPackage(symbol, args, null, rns);
        }

        /**
         * {@code .NAME = string, .PACKAGE = package}. An error if package provided and it does not
         * define that symbol.
         */
        @Specialization
        protected Object callNamedFunctionWithPackage(String symbol, RArgsValuesAndNames args, String packageName, //
                        @Cached("createRegisteredNativeSymbol(CallNST)") DLL.RegisteredNativeSymbol rns) {
            DLL.SymbolHandle func = DLL.findSymbol(symbol, packageName, rns);
            if (func == DLL.SYMBOL_NOT_FOUND) {
                errorProfile.enter();
                throw RError.error(this, RError.Message.SYMBOL_NOT_IN_TABLE, symbol, "Call", packageName);
            }
            return callRFFINode.invokeCall(new NativeCallInfo(symbol, func, rns.getDllInfo()), args.getArguments());
        }

        @SuppressWarnings("unused")
        @Fallback
        protected Object dotCallFallback(Object symbol, Object args, Object packageName) {
            throw fallback(symbol);
        }

    }

    /**
     * The interpretation of the {@code .NAME} and {code .PACKAGE} arguments as are for
     * {@link DotCall}.
     */
    @RBuiltin(name = ".External", kind = PRIMITIVE, parameterNames = {".NAME", "...", "PACKAGE"}, behavior = COMPLEX)
    public abstract static class DotExternal extends CallRFFIAdapter {

        private final BranchProfile errorProfile = BranchProfile.create();

        @Override
        @TruffleBoundary
        protected RExternalBuiltinNode lookupBuiltin(RList f) {
            String name = lookupName(f);
            if (FastROptions.UseInternalGraphics.getBooleanValue()) {
                switch (name) {
                    case "PDF":
                        return new DevicesCCalls.C_PDF();
                    case "devoff":
                        return new DevicesCCalls.C_DevOff();
                    case "devcur":
                        return new DevicesCCalls.C_DevCur();
                }
            }
            switch (name) {
                case "compcases":
                    return new CompleteCases();
                // utils
                case "countfields":
                    return new CountFields();
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
                case "unzip":
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
        protected Object doExternal(VirtualFrame frame, RList symbol, RArgsValuesAndNames args, Object packageName, //
                        @Cached("symbol") RList cached, //
                        @Cached("lookupBuiltin(symbol)") RExternalBuiltinNode builtin) {
            return builtin.call(frame, args);
        }

        @SuppressWarnings("unused")
        @Specialization(limit = "1", guards = {"cached == symbol"})
        protected Object callNamedFunction(VirtualFrame frame, RList symbol, RArgsValuesAndNames args, Object packageName, //
                        @Cached("symbol") RList cached, //
                        @Cached("extractSymbolInfo(frame, symbol)") NativeCallInfo nativeCallInfo) {
            Object list = encodeArgumentPairList(args, nativeCallInfo.name);
            return callRFFINode.invokeCall(nativeCallInfo, new Object[]{list});
        }

        @Specialization
        protected Object callNamedFunction(String symbol, RArgsValuesAndNames args, @SuppressWarnings("unused") RMissing packageName, // )
                        @Cached("createRegisteredNativeSymbol(ExternalNST)") DLL.RegisteredNativeSymbol rns) {
            return callNamedFunctionWithPackage(symbol, args, null, rns);
        }

        @Specialization
        protected Object callNamedFunctionWithPackage(String symbol, RArgsValuesAndNames args, String packageName, //
                        @Cached("createRegisteredNativeSymbol(ExternalNST)") DLL.RegisteredNativeSymbol rns) {
            DLL.SymbolHandle func = DLL.findSymbol(symbol, packageName, rns);
            if (func == DLL.SYMBOL_NOT_FOUND) {
                errorProfile.enter();
                throw RError.error(this, RError.Message.SYMBOL_NOT_IN_TABLE, symbol, "External", packageName);
            }
            Object list = encodeArgumentPairList(args, symbol);
            return callRFFINode.invokeCall(new NativeCallInfo(symbol, func, rns.getDllInfo()), new Object[]{list});
        }

        @Fallback
        protected Object fallback(Object f, @SuppressWarnings("unused") Object args, @SuppressWarnings("unused") Object packageName) {
            throw fallback(f);
        }
    }

    @RBuiltin(name = ".External2", visibility = CUSTOM, kind = PRIMITIVE, parameterNames = {".NAME", "...", "PACKAGE"}, behavior = COMPLEX)
    public abstract static class DotExternal2 extends CallRFFIAdapter {
        private static final Object CALL = "call";
        private static final Object OP = "op";
        private static final Object RHO = "rho";

        private final BranchProfile errorProfile = BranchProfile.create();

        @Override
        @TruffleBoundary
        protected RExternalBuiltinNode lookupBuiltin(RList symbol) {
            if (FastROptions.UseInternalGraphics.getBooleanValue()) {
                switch (lookupName(symbol)) {
                    case "C_par":
                        return new C_Par();
                }
            }
            String name = lookupName(symbol);
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
        protected Object doExternal(VirtualFrame frame, RList symbol, RArgsValuesAndNames args, Object packageName, //
                        @Cached("symbol") RList cached, //
                        @Cached("lookupBuiltin(symbol)") RExternalBuiltinNode builtin) {
            return builtin.call(frame, args);
        }

        @SuppressWarnings("unused")
        @Specialization(limit = "1", guards = {"cached == symbol"})
        protected Object callNamedFunction(VirtualFrame frame, RList symbol, RArgsValuesAndNames args, Object packageName, //
                        @Cached("symbol") RList cached, //
                        @Cached("extractSymbolInfo(frame, symbol)") NativeCallInfo nativeCallInfo) {
            Object list = encodeArgumentPairList(args, nativeCallInfo.name);
            // TODO: provide proper values for the CALL, OP and RHO parameters
            return callRFFINode.invokeCall(nativeCallInfo, new Object[]{CALL, OP, list, RHO});
        }

        @Specialization
        protected Object callNamedFunction(String symbol, RArgsValuesAndNames args, @SuppressWarnings("unused") RMissing packageName, // )
                        @Cached("createRegisteredNativeSymbol(ExternalNST)") DLL.RegisteredNativeSymbol rns) {
            return callNamedFunctionWithPackage(symbol, args, null, rns);
        }

        @Specialization
        protected Object callNamedFunctionWithPackage(String symbol, RArgsValuesAndNames args, String packageName, //
                        @Cached("createRegisteredNativeSymbol(ExternalNST)") DLL.RegisteredNativeSymbol rns) {
            DLL.SymbolHandle func = DLL.findSymbol(symbol, packageName, rns);
            if (func == DLL.SYMBOL_NOT_FOUND) {
                errorProfile.enter();
                throw RError.error(this, RError.Message.SYMBOL_NOT_IN_TABLE, symbol, "External2", packageName);
            }
            Object list = encodeArgumentPairList(args, symbol);
            // TODO: provide proper values for the CALL, OP and RHO parameters
            return callRFFINode.invokeCall(new NativeCallInfo(symbol, func, rns.getDllInfo()), new Object[]{CALL, OP, list, RHO});
        }

        @Fallback
        protected Object fallback(Object f, @SuppressWarnings("unused") Object args, @SuppressWarnings("unused") Object packageName) {
            throw fallback(f);
        }
    }

    @RBuiltin(name = ".External.graphics", kind = PRIMITIVE, parameterNames = {".NAME", "...", "PACKAGE"}, behavior = COMPLEX)
    public abstract static class DotExternalGraphics extends CallRFFIAdapter {

        private final BranchProfile errorProfile = BranchProfile.create();

        @Override
        @TruffleBoundary
        protected RExternalBuiltinNode lookupBuiltin(RList f) {
            if (FastROptions.UseInternalGraphics.getBooleanValue()) {
                switch (lookupName(f)) {
                    case "C_mtext":
                        return new GraphicsCCalls.C_mtext();
                    case "C_plotXY":
                        return new C_PlotXY();
                }
            }
            return null;
        }

        @SuppressWarnings("unused")
        @Specialization(limit = "1", guards = {"cached == f", "builtin != null"})
        protected Object doExternal(VirtualFrame frame, RList f, RArgsValuesAndNames args, RMissing packageName, //
                        @Cached("f") RList cached, //
                        @Cached("lookupBuiltin(f)") RExternalBuiltinNode builtin) {
            return builtin.call(frame, args);
        }

        @Specialization
        protected Object callNamedFunction(VirtualFrame frame, RList symbol, RArgsValuesAndNames args, @SuppressWarnings("unused") Object packageName) {
            NativeCallInfo nativeCallInfo = extractSymbolInfo(frame, symbol);
            Object list = encodeArgumentPairList(args, nativeCallInfo.name);
            return callRFFINode.invokeCall(nativeCallInfo, new Object[]{list});
        }

        @Specialization
        protected Object callNamedFunction(String name, RArgsValuesAndNames args, @SuppressWarnings("unused") RMissing packageName) {
            return callNamedFunctionWithPackage(name, args, null);
        }

        @Specialization
        protected Object callNamedFunctionWithPackage(String name, RArgsValuesAndNames args, String packageName) {
            DLL.RegisteredNativeSymbol rns = new DLL.RegisteredNativeSymbol(DLL.NativeSymbolType.External, null, null);
            DLL.SymbolHandle func = DLL.findSymbol(name, packageName, rns);
            if (func == DLL.SYMBOL_NOT_FOUND) {
                errorProfile.enter();
                throw RError.error(this, RError.Message.C_SYMBOL_NOT_IN_TABLE, name);
            }
            Object list = encodeArgumentPairList(args, name);
            return callRFFINode.invokeCall(new NativeCallInfo(name, func, rns.getDllInfo()), new Object[]{list});
        }

        @Fallback
        protected Object fallback(Object f, @SuppressWarnings("unused") Object args, @SuppressWarnings("unused") Object packageName) {
            throw fallback(f);
        }
    }

    @RBuiltin(name = ".Call.graphics", kind = PRIMITIVE, parameterNames = {".NAME", "...", "PACKAGE"}, behavior = COMPLEX)
    public abstract static class DotCallGraphics extends CallRFFIAdapter {

        private final BranchProfile errorProfile = BranchProfile.create();

        @Override
        public Object[] getDefaultParameterValues() {
            return new Object[]{RMissing.instance, RArgsValuesAndNames.EMPTY, RMissing.instance};
        }

        @Override
        @TruffleBoundary
        protected RExternalBuiltinNode lookupBuiltin(RList f) {
            switch (lookupName(f)) {
                default:
                    return null;
            }
        }

        @SuppressWarnings("unused")
        @Specialization(limit = "1", guards = {"cached == f", "builtin != null"})
        protected Object doExternal(VirtualFrame frame, RList f, RArgsValuesAndNames args, RMissing packageName, //
                        @Cached("f") RList cached, //
                        @Cached("lookupBuiltin(f)") RExternalBuiltinNode builtin) {
            return builtin.call(frame, args);
        }

        @Specialization
        protected Object callNamedFunction(VirtualFrame frame, RList symbol, RArgsValuesAndNames args, @SuppressWarnings("unused") Object packageName) {
            NativeCallInfo nativeCallInfo = extractSymbolInfo(frame, symbol);
            return callRFFINode.invokeCall(nativeCallInfo, args.getArguments());
        }

        @Specialization
        protected Object callNamedFunction(String name, RArgsValuesAndNames args, @SuppressWarnings("unused") RMissing packageName) {
            return callNamedFunctionWithPackage(name, args, null);
        }

        @Specialization
        @TruffleBoundary
        protected Object callNamedFunctionWithPackage(String name, RArgsValuesAndNames args, String packageName) {
            DLL.RegisteredNativeSymbol rns = new DLL.RegisteredNativeSymbol(DLL.NativeSymbolType.Call, null, null);
            DLL.SymbolHandle func = DLL.findSymbol(name, packageName, rns);
            if (func == DLL.SYMBOL_NOT_FOUND) {
                errorProfile.enter();
                throw RError.error(this, RError.Message.C_SYMBOL_NOT_IN_TABLE, name);
            }
            return callRFFINode.invokeCall(new NativeCallInfo(name, func, rns.getDllInfo()), args.getArguments());
        }

        @Fallback
        protected Object dotCallFallback(Object fobj, @SuppressWarnings("unused") Object args, @SuppressWarnings("unused") Object packageName) {
            throw fallback(fobj);
        }
    }
}
