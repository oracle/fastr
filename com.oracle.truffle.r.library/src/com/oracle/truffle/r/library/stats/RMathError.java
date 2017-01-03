/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1998-2016, The R Core Team
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.stats;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;

/**
 * Encapsulates functionality related to errors/warnings reporting in FastR port of R's math
 * library. Contains methods that correspond to various macros such as {@code ML_ERROR} or
 * {@code ML_ERR_return_NAN}.
 */
public final class RMathError {
    private RMathError() {
        // only static members
    }

    public enum MLError {
        DOMAIN(Message.GENERIC),
        RANGE(Message.ML_ERROR_RANGE),
        NOCONV(Message.ML_ERROR_NOCONV),
        PRECISION(Message.ML_ERROR_PRECISION),
        UNDERFLOW(Message.ML_ERROR_UNDERFLOW);

        private final RError.Message message;

        MLError(Message message) {
            this.message = message;
        }

        @TruffleBoundary
        public void warning(String arg) {
            RError.warning(RError.SHOW_CALLER, message, arg);
        }
    }

    /**
     * Corresponds to macro {@code ML_ERR_return_NAN} in GnuR. We also do not report the default
     * warning directly and let the caller handle the {@code NaN} value.
     */
    public static double defaultError() {
        return Double.NaN;
    }

    /**
     * Corresponds to macro {@code ML_ERR} in GnuR. As long as the error is not the default
     * {@link MLError#DOMAIN} a warning is reported by this method, otherwise the caller should
     * return {@code NaN}, which should be handled by the caller's caller.
     */
    public static double error(@SuppressWarnings("unused") MLError error, @SuppressWarnings("unused") String messageArg) {
        if (error != MLError.DOMAIN) {
            error.warning(messageArg);
        }
        return Double.NaN;
    }

    /**
     * Corresponds to macros {@code MATHLIB_WARNINGX} in GnuR.
     */
    @TruffleBoundary
    public static void warning(RError.Message message, Object... args) {
        RError.warning(RError.SHOW_CALLER, message, args);
    }
}
