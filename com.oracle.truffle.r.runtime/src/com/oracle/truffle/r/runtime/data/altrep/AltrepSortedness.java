package com.oracle.truffle.r.runtime.data.altrep;

/**
 * This enum is transcribed from Rinternals.h
 */
public enum AltrepSortedness {
    SORTED_DECR_NA_1ST (-2),
    SORTED_DECR (-1),
    UNKNOWN_SORTEDNESS (Integer.MIN_VALUE),
    SORTED_INCR (1),
    SORTED_INCR_NA_1ST (2),
    KNOWN_UNSORTED (0);

    private final int value;

    AltrepSortedness(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
