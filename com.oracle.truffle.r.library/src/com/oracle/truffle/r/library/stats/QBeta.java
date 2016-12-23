/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996, Robert Gentleman and Ross Ihaka
 * Copyright (c) 1998-2015, The R Core Team
 * Copyright (c) 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
/*
 *  based on code (C) 1979 and later Royal Statistical Society
 *
 * Reference:
 * Cran, G. W., K. J. Martin and G. E. Thomas (1977).
 *      Remark AS R19 and Algorithm AS 109,
 *      Applied Statistics, 26(1), 111-114.
 * Remark AS R83 (v.39, 309-310) and the correction (v.40(1) p.236)
 *      have been incorporated in this version.
 */
package com.oracle.truffle.r.library.stats;

import static com.oracle.truffle.r.library.stats.Arithmetic.powDi;
import static com.oracle.truffle.r.library.stats.LBeta.lbeta;
import static com.oracle.truffle.r.library.stats.MathConstants.DBL_MANT_DIG;
import static com.oracle.truffle.r.library.stats.MathConstants.DBL_MIN_EXP;
import static com.oracle.truffle.r.library.stats.MathConstants.ML_NAN;
import static com.oracle.truffle.r.library.stats.MathConstants.M_LN2;
import static com.oracle.truffle.r.library.stats.Pbeta.pbetaRaw;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.library.stats.RMath.MLError;
import com.oracle.truffle.r.library.stats.StatsFunctions.Function3_2;
import com.oracle.truffle.r.runtime.RError.Message;

public final class QBeta implements Function3_2 {
    private static final double USE_LOG_X_CUTOFF = -5.;
    private static final int N_NEWTON_FREE = 4;
    // TODO: find out why this??? Is swap_01 R logical??
    private static final int MLOGICAL_NA = -1;

    @Override
    public double evaluate(double alpha, double p, double q, boolean lowerTail, boolean logP) {
        if (Double.isNaN(p) || Double.isNaN(q) || Double.isNaN(alpha)) {
            return p + q + alpha;
        }

        if (p < 0. || q < 0.) {
            return RMath.mlError();
        }
        // allowing p==0 and q==0 <==> treat as one- or two-point mass

        double[] qbet = new double[2]; // = { qbeta(), 1 - qbeta() }
        new QBetaRawMethod().qbeta_raw(alpha, p, q, lowerTail, logP, MLOGICAL_NA, USE_LOG_X_CUTOFF, N_NEWTON_FREE, qbet);
        return qbet[0];

    }

    /**
     * Debugging printfs should print exactly the same output as GnuR, this can help with debugging.
     */
    private static void debugPrintf(@SuppressWarnings("unused") String fmt, @SuppressWarnings("unused") Object... args) {
        // System.out.printf(fmt + "\n", args);
    }

    private static final double acu_min = 1e-300;
    private static final double log_eps_c = M_LN2 * (1. - DBL_MANT_DIG); // = log(DBL_EPSILON) =
                                                                         // -36.04..;
    private static final double fpu = 3e-308;
    private static final double p_lo = fpu;
    private static final double p_hi = 1 - 2.22e-16;

    private static final double const1 = 2.30753;
    private static final double const2 = 0.27061;
    private static final double const3 = 0.99229;
    private static final double const4 = 0.04481;

    private static final double DBL_very_MIN = Double.MIN_VALUE / 4.;
    private static final double DBL_log_v_MIN = M_LN2 * (DBL_MIN_EXP - 2);
    private static final double DBL_1__eps = 0x1.fffffffffffffp-1; // = 1 - 2^-53

    // The fields and variables are named after local variables from GnuR, we keep the original
    // names for the ease of debugging
    // Checkstyle: stop field name check
    private static class QBetaRawMethod {
        private boolean give_log_q;
        private boolean use_log_x;
        private boolean add_N_step;
        private double logbeta;
        private boolean swap_tail;
        private double a;
        private double la;
        private double pp;
        private double qq;
        private double r;
        private double t;
        private double tx;
        private double u_n;
        private double xinbta;
        private double u;
        private boolean warned;
        private double acu;
        private double y;
        private double w;
        private boolean log_; // or u < log_q_cut below
        private double p_;
        private double u0;
        private double s;
        private double h;
        boolean bad_u;
        boolean bad_init;
        private double g;
        private double D;

        /**
         *
         * @param alpha
         * @param p
         * @param q
         * @param lower_tail
         * @param log_p
         * @param swap_01 {true, NA, false}: if NA, algorithm decides swap_tail
         * @param log_q_cut if == Inf: return Math.log(qbeta(..)); otherwise, if finite: the bound
         *            for switching to Math.log(x)-scale; see use_log_x
         * @param n_N number of "unconstrained" Newton steps before switching to constrained
         * @param qb The result will be saved to this 2 dimensional array = qb[0:1] = { qbeta(), 1 -
         *            qbeta() }.
         */
        @TruffleBoundary
        private void qbeta_raw(double alpha, double p, double q, boolean lower_tail, boolean log_p,
                        int swap_01,
                        double log_q_cut,
                        int n_N,
                        double[] qb) {
            give_log_q = (log_q_cut == Double.POSITIVE_INFINITY);
            use_log_x = give_log_q;
            add_N_step = true;
            y = -1.;

            // Assuming p >= 0, q >= 0 here ...

            // Deal with boundary cases here:
            if (alpha == DPQ.rdt0(lower_tail, log_p)) {
                returnQ0(qb, give_log_q);
                return;
            }
            if (alpha == DPQ.rdt1(lower_tail, log_p)) {
                returnQ1(qb, give_log_q);
                return;
            }

            // check alpha {*before* transformation which may all accuracy}:
            if ((log_p && alpha > 0) ||
                            (!log_p && (alpha < 0 || alpha > 1))) { // alpha is outside
                debugPrintf("qbeta(alpha=%g, %g, %g, .., log_p=%d): %s%s\n",
                                alpha, p, q, log_p, "alpha not in ",
                                log_p ? "[-Inf, 0]" : "[0,1]");
                // ML_ERR_return_NAN :
                RMath.mlError(MLError.DOMAIN, "");
                qb[0] = qb[1] = ML_NAN;
                return;
            }

            // p==0, q==0, p = Inf, q = Inf <==> treat as one- or two-point mass
            if (p == 0 || q == 0 || !Double.isFinite(p) || !Double.isFinite(q)) {
                // We know 0 < T(alpha) < 1 : pbeta() is constant and trivial in {0, 1/2, 1}
                debugPrintf(
                                "qbeta(%g, %g, %g, lower_t=%d, log_p=%d): (p,q)-boundary: trivial\n",
                                alpha, p, q, lower_tail, log_p);
                if (p == 0 && q == 0) { // point mass 1/2 at each of {0,1} :
                    if (alpha < DPQ.rdhalf(log_p)) {
                        returnQ0(qb, give_log_q);
                    } else if (alpha > DPQ.rdhalf(log_p)) {
                        returnQ1(qb, give_log_q);
                    } else {
                        returnQHalf(qb, give_log_q);
                    }
                    return;
                } else if (p == 0 || p / q == 0) { // point mass 1 at 0 - "flipped around"
                    returnQ0(qb, give_log_q);
                } else if (q == 0 || q / p == 0) { // point mass 1 at 0 - "flipped around"
                    returnQ1(qb, give_log_q);
                } else {
                    // else: p = q = Inf : point mass 1 at 1/2
                    returnQHalf(qb, give_log_q);
                }
                return;
            }

            p_ = DPQ.rdtqiv(alpha, lower_tail, log_p);
            logbeta = lbeta(p, q);
            boolean swap_choose = (swap_01 == MLOGICAL_NA);
            swap_tail = swap_choose ? (p_ > 0.5) : swap_01 != 0;
            if (swap_tail) { /* change tail, swap p <-> q : */
                a = DPQ.rdtciv(alpha, lower_tail, log_p); // = 1 - p_ < 1/2
                /* la := log(a), but without numerical cancellation: */
                la = DPQ.rdtclog(alpha, lower_tail, log_p);
                pp = q;
                qq = p;
            } else {
                a = p_;
                la = DPQ.rdtlog(alpha, lower_tail, log_p);
                pp = p;
                qq = q;
            }

            /* calculate the initial approximation */

            /*
             * Desired accuracy for Newton iterations (below) should depend on (a,p) This is from
             * Remark .. on AS 109, adapted. However, it's not clear if this is "optimal" for IEEE
             * double prec.
             *
             * acu = fmax2(acu_min, pow(10., -25. - 5./(pp * pp) - 1./(a * a)));
             *
             * NEW: 'acu' accuracy NOT for squared adjustment, but simple; ---- i.e., "new acu" =
             * sqrt(old acu)
             */
            acu = Math.max(acu_min, Math.pow(10., -13. - 2.5 / (pp * pp) - 0.5 / (a * a)));
            // try to catch "extreme left tail" early
            u0 = (la + Math.log(pp) + logbeta) / pp; // = log(x_0)
            r = pp * (1. - qq) / (pp + 1.);
            t = 0.2;

            debugPrintf(
                            "qbeta(%g, %g, %g, lower_t=%d, log_p=%d):%s\n" +
                                            "  swap_tail=%d, la=%g, u0=%g (bnd: %g (%g)) ",
                            alpha, p, q, lower_tail, log_p,
                            (log_p && (p_ == 0. || p_ == 1.)) ? (p_ == 0. ? " p_=0" : " p_=1") : "",
                            swap_tail, la, u0,
                            (t * log_eps_c - Math.log(Math.abs(pp * (1. - qq) * (2. - qq) / (2. * (pp + 2.))))) / 2.,
                            t * log_eps_c - Math.log(Math.abs(r)));

            tx = 0.;
            if (M_LN2 * DBL_MIN_EXP < u0 && // cannot allow exp(u0) = 0 ==> exp(u1) = exp(u0) = 0
                            u0 < -0.01 && // (must: u0 < 0, but too close to 0 <==> x = exp(u0) =
                            // 0.99..)
                            // qq <= 2 && // <--- "arbitrary"
                            // u0 < t*log_eps_c - log(fabs(r)) &&
                            u0 < (t * log_eps_c - Math.log(Math.abs(pp * (1. - qq) * (2. - qq) / (2. * (pp + 2.))))) / 2.) {
                // TODO: maybe jump here from below, when initial u "fails" ?
                // L_tail_u:
                // MM's one-step correction (cheaper than 1 Newton!)
                r = r * Math.exp(u0); // = r*x0
                if (r > -1.) {
                    u = u0 - RMath.log1p(r) / pp;
                    debugPrintf("u1-u0=%9.3g --> choosing u = u1\n", u - u0);
                } else {
                    u = u0;
                    debugPrintf("cannot cheaply improve u0\n");
                }
                tx = xinbta = Math.exp(u);
                use_log_x = true; // or (u < log_q_cut) ??
                newton(n_N, log_p, qb);
                return;
            }

            // y := y_\alpha in AS 64 := Hastings(1955) approximation of qnorm(1 - a) :
            r = Math.sqrt(-2 * la);
            y = r - (const1 + const2 * r) / (1. + (const3 + const4 * r) * r);

            if (pp > 1 && qq > 1) { // use Carter(1947), see AS 109, remark '5.'
                r = (y * y - 3.) / 6.;
                s = 1. / (pp + pp - 1.);
                t = 1. / (qq + qq - 1.);
                h = 2. / (s + t);
                w = y * Math.sqrt(h + r) / h - (t - s) * (r + 5. / 6. - 2. / (3. * h));
                debugPrintf("p,q > 1 => w=%g", w);
                if (w > 300) { // Math.exp(w+w) is huge or overflows
                    t = w + w + Math.log(qq) - Math.log(pp); // = argument of log1pMath.exp(.)
                    u = // Math.log(xinbta) = - log1p(qq/pp * Math.exp(w+w)) = -Math.log(1 +
                        // Math.exp(t))
                                    (t <= 18) ? -RMath.log1p(Math.exp(t)) : -t - Math.exp(-t);
                    xinbta = Math.exp(u);
                } else {
                    xinbta = pp / (pp + qq * Math.exp(w + w));
                    u = // Math.log(xinbta)
                                    -RMath.log1p(qq / pp * Math.exp(w + w));
                }
            } else { // use the original AS 64 proposal, Scheffé-Tukey (1944) and Wilson-Hilferty
                r = qq + qq;
                /*
                 * A slightly more stable version of t := \chi^2_{alpha} of AS 64 t = 1. / (9. *
                 * qq); t = r * R_pow_di(1. - t + y * Math.sqrt(t), 3);
                 */
                t = 1. / (3. * Math.sqrt(qq));
                t = r * powDi(1. + t * (-t + y), 3); // = \chi^2_{alpha} of AS 64
                s = 4. * pp + r - 2.; // 4p + 2q - 2 = numerator of new t = (...) / chi^2
                debugPrintf("min(p,q) <= 1: t=%g", t);
                if (t == 0 || (t < 0. && s >= t)) { // cannot use chisq approx
                    // x0 = 1 - { (1-a)*q*B(p,q) } ^{1/q} {AS 65}
                    // xinbta = 1. - Math.exp((Math.log(1-a)+ Math.log(qq) + logbeta) / qq);
                    double l1ma; /*
                                  * := Math.log(1-a), directly from alpha (as 'la' above): FIXME:
                                  * not worth it? log1p(-a) always the same ??
                                  */
                    if (swap_tail) {
                        l1ma = DPQ.rdtlog(alpha, lower_tail, log_p);
                    } else {
                        l1ma = DPQ.rdtclog(alpha, lower_tail, log_p);
                    }
                    debugPrintf(" t <= 0 : log1p(-a)=%.15g, better l1ma=%.15g\n", RMath.log1p(-a), l1ma);
                    double xx = (l1ma + Math.log(qq) + logbeta) / qq;
                    if (xx <= 0.) {
                        xinbta = -RMath.expm1(xx);
                        u = DPQ.rlog1exp(xx); // = Math.log(xinbta) = Math.log(1 -
                                              // Math.exp(...A...))
                    } else { // xx > 0 ==> 1 - e^xx < 0 .. is nonsense
                        debugPrintf(" xx=%g > 0: xinbta:= 1-e^xx < 0\n", xx);
                        xinbta = 0;
                        u = Double.NEGATIVE_INFINITY; /// FIXME can do better?
                    }
                } else {
                    t = s / t;
                    debugPrintf(" t > 0 or s < t < 0:  new t = %g ( > 1 ?)\n", t);
                    if (t <= 1.) { // cannot use chisq, either
                        u = (la + Math.log(pp) + logbeta) / pp;
                        xinbta = Math.exp(u);
                    } else { // (1+x0)/(1-x0) = t, solved for x0 :
                        xinbta = 1. - 2. / (t + 1.);
                        u = RMath.log1p(-2. / (t + 1.));
                    }
                }
            }

            // Problem: If initial u is completely wrong, we make a wrong decision here
            if (swap_choose &&
                            ((swap_tail && u >= -Math.exp(log_q_cut)) || // ==> "swap back"
                                            (!swap_tail && u >= -Math.exp(4 * log_q_cut) && pp / qq < 1000.))) { // ==>
                // "swap
                // now"
                // (much
                // less
                // easily)
                // "revert swap" -- and use_log_x
                swap_tail = !swap_tail;
                debugPrintf(" u = %g (e^u = xinbta = %.16g) ==> ", u, xinbta);
                if (swap_tail) {
                    a = DPQ.rdtciv(alpha, lower_tail, log_p); // needed ?
                    la = DPQ.rdtclog(alpha, lower_tail, log_p);
                    pp = q;
                    qq = p;
                } else {
                    a = p_;
                    la = DPQ.rdtlog(alpha, lower_tail, log_p);
                    pp = p;
                    qq = q;
                }
                debugPrintf("\"%s\"; la = %g\n",
                                (swap_tail ? "swap now" : "swap back"), la);
                // we could redo computations above, but this should be stable
                u = DPQ.rlog1exp(u);
                xinbta = Math.exp(u);

                /*
                 * Careful: "swap now" should not fail if 1) the above initial xinbta is
                 * "completely wrong" 2) The correction step can go outside (u_n > 0 ==> e^u > 1 is
                 * illegal) e.g., for qbeta(0.2066, 0.143891, 0.05)
                 */
            }

            if (!use_log_x) {
                use_log_x = (u < log_q_cut); // (per default) <==> xinbta = e^u < 4.54e-5
            }
            bad_u = !Double.isFinite(u);
            bad_init = bad_u || xinbta > p_hi;

            debugPrintf(" -> u = %g, e^u = xinbta = %.16g, (Newton acu=%g%s)\n",
                            u, xinbta, acu,
                            (bad_u ? ", ** bad u **" : (use_log_x ? ", on u = Math.log(x) scale" : "")));

            u_n = 1.;
            tx = xinbta; // keeping "original initial x" (for now)

            if (bad_u || u < log_q_cut) { /*
                                           * e.g. qbeta(0.21, .001, 0.05) try "left border" quickly,
                                           * i.e., try at smallest positive number:
                                           */
                w = pbetaRaw(DBL_very_MIN, pp, qq, true, log_p);
                if (w > (log_p ? la : a)) {
                    debugPrintf(" quantile is left of smallest positive number; \"convergence\"\n");
                    if (log_p || Math.abs(w - a) < Math.abs(0 - a)) { // DBL_very_MIN is better than
                                                                      // 0
                        tx = DBL_very_MIN;
                        u_n = DBL_log_v_MIN; // = Math.log(DBL_very_MIN)
                    } else {
                        tx = 0.;
                        u_n = Double.NEGATIVE_INFINITY;
                    }
                    use_log_x = log_p;
                    add_N_step = false;
                    finalStep(log_p, qb);
                    return;
                } else {
                    debugPrintf(" pbeta(smallest pos.) = %g <= %g  --> continuing\n",
                                    w, (log_p ? la : a));
                    if (u < DBL_log_v_MIN) {
                        u = DBL_log_v_MIN; // = Math.log(DBL_very_MIN)
                        xinbta = DBL_very_MIN;
                    }
                }
            }

            /* Sometimes the approximation is negative (and == 0 is also not "ok") */
            if (bad_init && !(use_log_x && tx > 0)) {
                if (u == Double.NEGATIVE_INFINITY) {
                    debugPrintf("  u = -Inf;");
                    u = M_LN2 * DBL_MIN_EXP;
                    xinbta = Double.MIN_VALUE;
                } else {
                    debugPrintf(" bad_init: u=%g, xinbta=%g;", u, xinbta);
                    xinbta = (xinbta > 1.1) // i.e. "way off"
                                    ? 0.5 // otherwise, keep the respective boundary:
                                    : ((xinbta < p_lo) ? Math.exp(u) : p_hi);
                    if (bad_u) {
                        u = Math.log(xinbta);
                    }
                    // otherwise: not changing "potentially better" u than the above
                }
                debugPrintf(" -> (partly)new u=%g, xinbta=%g\n", u, xinbta);
            }

            newton(n_N, log_p, qb);
            // note: newton calls converged which calls finalStep
        }

        private void newton(int n_N, boolean log_p, double[] qb) {
            /*
             * --------------------------------------------------------------------
             *
             * Solve for x by a modified Newton-Raphson method, using pbeta_raw()
             */
            r = 1 - pp;
            t = 1 - qq;
            double wprev = 0.;
            double prev = 1.;
            double adj = 1.;

            if (use_log_x) { // find Math.log(xinbta) -- work in u := Math.log(x) scale
                // if (bad_init && tx > 0) { xinbta = tx; }// may have been better
                for (int i_pb = 0; i_pb < 1000; i_pb++) {
                    // using log_p == true unconditionally here
                    // FIXME: if Math.exp(u) = xinbta underflows to 0, like different formula
                    // pbeta_Math.log(u, *)
                    y = pbetaRaw(xinbta, pp, qq, /* lower_tail = */ true, true);

                    /*
                     * w := Newton step size for L(u) = log F(e^u) =!= 0; u := Math.log(x) = (L(.) -
                     * la) / L'(.); L'(u)= (F'(e^u) * e^u ) / F(e^u) = (L(.) - la)*F(.) / {F'(e^u) *
                     * e^u } = = (L(.) - la) * e^L(.) * e^{-log F'(e^u) - u} = ( y - la) * e^{ y - u
                     * -log F'(e^u)} and -log F'(x)= -log f(x) = + logbeta + (1-p) Math.log(x) +
                     * (1-q) Math.log(1-x) = logbeta + (1-p) u + (1-q) Math.log(1-e^u)
                     */
                    w = (y == Double.NEGATIVE_INFINITY) // y = -Inf well possible: we are on
                                                        // log scale!
                                    ? 0. : (y - la) * Math.exp(y - u + logbeta + r * u + t * DPQ.rlog1exp(u));
                    if (!Double.isFinite(w)) {
                        break;
                    }
                    if (i_pb >= n_N && w * wprev <= 0.) {
                        prev = RMath.fmax2(Math.abs(adj), fpu);
                    }
                    debugPrintf("N(i=%2d): u=%#20.16g, pb(e^u)=%#12.6g, w=%#15.9g, %s prev=%11g,",
                                    i_pb, u, y, w, (w * wprev <= 0.) ? "new" : "old", prev);
                    g = 1;
                    int i_inn;
                    for (i_inn = 0; i_inn < 1000; i_inn++) {
                        adj = g * w;
                        // take full Newton steps at the beginning; only then safe guard:
                        if (i_pb < n_N || Math.abs(adj) < prev) {
                            u_n = u - adj; // u_{n+1} = u_n - g*w
                            if (u_n <= 0.) { // <==> 0 < xinbta := e^u <= 1
                                if (prev <= acu || Math.abs(w) <= acu) {
                                    /* R_ifDEBUG_printf(" -adj=%g, %s <= acu  ==> convergence\n", */
                                    /* -adj, (prev <= acu) ? "prev" : "|w|"); */
                                    debugPrintf(" it{in}=%d, -adj=%g, %s <= acu  ==> convergence\n",
                                                    i_inn, -adj, (prev <= acu) ? "prev" : "|w|");
                                    converged(log_p, qb);
                                    return;
                                }
                                // if (u_n != Double.NEGATIVE_INFINITY && u_n != 1)
                                break;
                            }
                        }
                        g /= 3;
                    }
                    // (cancellation in (u_n -u) => may differ from adj:
                    D = RMath.fmin2(Math.abs(adj), Math.abs(u_n - u));
                    /* R_ifDEBUG_printf(" delta(u)=%g\n", u_n - u); */
                    debugPrintf(" it{in}=%d, delta(u)=%9.3g, D/|.|=%.3g\n",
                                    i_inn, u_n - u, D / Math.abs(u_n + u));
                    if (D <= 4e-16 * Math.abs(u_n + u)) {
                        converged(log_p, qb);
                        return;
                    }
                    u = u_n;
                    xinbta = Math.exp(u);
                    wprev = w;
                } // for(i )

            } else {
                for (int i_pb = 0; i_pb < 1000; i_pb++) {
                    y = pbetaRaw(xinbta, pp, qq, /* lower_tail = */ true, log_p);
                    // delta{y} : d_y = y - (log_p ? la : a);

                    if (!Double.isFinite(y) && !(log_p && y == Double.NEGATIVE_INFINITY)) { // y =
                                                                                            // -Inf
                        // is ok if
                        // (log_p)
                        RMath.mlError(MLError.DOMAIN, "");
                        qb[0] = qb[1] = ML_NAN;
                        return;
                    }

                    /*
                     * w := Newton step size (F(.) - a) / F'(.) or, -- log: (lF - la) / (F' / F) =
                     * Math.exp(lF) * (lF - la) / F'
                     */
                    w = log_p
                                    ? (y - la) * Math.exp(y + logbeta + r * Math.log(xinbta) + t * Math.log1p(-xinbta))
                                    : (y - a) * Math.exp(logbeta + r * Math.log(xinbta) + t * Math.log1p(-xinbta));
                    if (i_pb >= n_N && w * wprev <= 0.)
                        prev = RMath.fmax2(Math.abs(adj), fpu);
                    debugPrintf("N(i=%2d): x0=%#17.15g, pb(x0)=%#17.15g, w=%#17.15g, %s prev=%g,",
                                    i_pb, xinbta, y, w, (w * wprev <= 0.) ? "new" : "old", prev);
                    g = 1;
                    int i_inn;
                    for (i_inn = 0; i_inn < 1000; i_inn++) {
                        adj = g * w;
                        // take full Newton steps at the beginning; only then safe guard:
                        if (i_pb < n_N || Math.abs(adj) < prev) {
                            tx = xinbta - adj; // x_{n+1} = x_n - g*w
                            if (0. <= tx && tx <= 1.) {
                                if (prev <= acu || Math.abs(w) <= acu) {
                                    debugPrintf(" it{in}=%d, delta(x)=%g, %s <= acu  ==> convergence\n",
                                                    i_inn, -adj, (prev <= acu) ? "prev" : "|w|");
                                    converged(log_p, qb);
                                    return;
                                }
                                if (tx != 0. && tx != 1) {
                                    break;
                                }
                            }
                        }
                        g /= 3;
                    }
                    debugPrintf(" it{in}=%d, delta(x)=%g\n", i_inn, tx - xinbta);
                    if (Math.abs(tx - xinbta) <= 4e-16 * (tx + xinbta)) { // "<=" : (.) == 0
                        converged(log_p, qb);
                        return;
                    }
                    xinbta = tx;
                    if (tx == 0) { // "we have lost"
                        break;
                    }
                    wprev = w;
                }
            }

            /*-- NOT converged: Iteration count --*/
            warned = true;
            RMath.mlError(MLError.PRECISION, "qbeta");

            converged(log_p, qb);
        }

        private void converged(boolean log_p, double[] qb) {
            log_ = log_p || use_log_x; // only for printing
            debugPrintf(" %s: Final delta(y) = %g%s\n",
                            warned ? "_NO_ convergence" : "converged",
                            y - (log_ ? la : a), (log_ ? " (log_)" : ""));
            if ((log_ && y == Double.NEGATIVE_INFINITY) || (!log_ && y == 0)) {
                // stuck at left, try if smallest positive number is "better"
                w = pbetaRaw(DBL_very_MIN, pp, qq, true, log_);
                if (log_ || Math.abs(w - a) <= Math.abs(y - a)) {
                    tx = DBL_very_MIN;
                    u_n = DBL_log_v_MIN; // = Math.log(DBL_very_MIN)
                }
                add_N_step = false; // not trying to do better anymore
            } else if (!warned && (log_ ? Math.abs(y - la) > 3 : Math.abs(y - a) > 1e-4)) {
                if (!(log_ && y == Double.NEGATIVE_INFINITY &&
                                // e.g. qbeta(-1e-10, .2, .03, log=true) cannot get accurate ==> do
                                // NOT
                                // warn
                                pbetaRaw(DBL_1__eps, // = 1 - eps
                                                pp, qq, true, true) > la + 2)) {
                    RMath.mlWarning(Message.QBETA_ACURACY_WARNING, (log_ ? ", log_" : ""), Math.abs(y - (log_ ? la : a)));
                }
            }

            finalStep(log_p, qb);
        }

        /**
         * Represents a block of code that is labelled "L_return" in the original source, should be
         * followed by a return.
         */
        private void finalStep(boolean log_p, double[] qb) {
            if (give_log_q) { // ==> use_log_x , too
                if (!use_log_x) { // (see if claim above is true)
                    RMath.mlWarning(Message.GENERIC,
                                    "qbeta() L_return, u_n=%g;  give_log_q=true but use_log_x=false -- please report!",
                                    u_n);
                }

                double rr = DPQ.rlog1exp(u_n);
                swapTail(qb, swap_tail, u_n, rr);
            } else {
                if (use_log_x) {
                    if (add_N_step) {
                        /*
                         * add one last Newton step on original x scale, e.g., for qbeta(2^-98,
                         * 0.125, 2^-96)
                         */
                        double tmpXinbta = Math.exp(u_n);
                        y = pbetaRaw(tmpXinbta, pp, qq, /* lower_tail = */ true, log_p);
                        w = log_p
                                        ? (y - la) * Math.exp(y + logbeta + r * Math.log(tmpXinbta) + t * RMath.log1p(-tmpXinbta))
                                        : (y - a) * Math.exp(logbeta + r * Math.log(tmpXinbta) + t * RMath.log1p(-tmpXinbta));
                        tx = tmpXinbta - w;
                        debugPrintf(
                                        "Final Newton correction(non-log scale): xinbta=%.16g, y=%g, w=%g. => new tx=%.16g\n",
                                        tmpXinbta, y, w, tx);
                    } else {
                        swapTail(qb, swap_tail, Math.exp(u_n), -RMath.expm1(u_n));
                        return;
                    }
                }
                swapTail(qb, swap_tail, tx, 1 - tx);
            }
        }

        private static void swapTail(double[] qb, boolean swap_tail, double val0, double val1) {
            if (swap_tail) {
                qb[0] = val1;
                qb[1] = val0;
            } else {
                qb[0] = val0;
                qb[1] = val1;
            }
        }

        private static void returnQ0(double[] qb, boolean give_log_q) {
            qb[0] = DPQ.rd0(give_log_q);
            qb[1] = DPQ.rd1(give_log_q);
        }

        private static void returnQ1(double[] qb, boolean give_log_q) {
            qb[0] = DPQ.rd1(give_log_q);
            qb[1] = DPQ.rd0(give_log_q);
        }

        private static void returnQHalf(double[] qb, boolean give_log_q) {
            qb[0] = DPQ.rdhalf(give_log_q);
            qb[1] = DPQ.rdhalf(give_log_q);
        }
    }
}
