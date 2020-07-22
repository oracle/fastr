package com.oracle.truffle.r.runtime.data.altrep;

import com.oracle.truffle.r.runtime.RInternalError;

/**
 * This enum is transcribed from Rinternals.h.
 */
public enum AltrepSortedness {
    SORTED_DECR_NA_1ST(-2),
    SORTED_DECR(-1),
    UNKNOWN_SORTEDNESS(Integer.MIN_VALUE),
    SORTED_INCR(1),
    SORTED_INCR_NA_1ST(2),
    KNOWN_UNSORTED(0);

    private final int value;

    AltrepSortedness(int value) {
        this.value = value;
    }

    public static AltrepSortedness fromInt(int value) {
        switch (value) {
            case -2:
                return SORTED_DECR_NA_1ST;
            case -1:
                return SORTED_DECR;
            case Integer.MIN_VALUE:
                return UNKNOWN_SORTEDNESS;
            case 1:
                return SORTED_INCR;
            case 2:
                return SORTED_INCR_NA_1ST;
            case 0:
                return KNOWN_UNSORTED;
        }
        throw RInternalError.shouldNotReachHere("Got unexpected integer value: " + value);
    }

    public int getValue() {
        return value;
    }
}
