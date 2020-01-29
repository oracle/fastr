#ifndef _FIRST_CHAR_CHANGER_CLASS_HPP_
#define _FIRST_CHAR_CHANGER_CLASS_HPP_

#include <R.h>
#include <Rinternals.h>
#include <R_ext/Altrep.h>

/**
 * An altstring class that gets a character vector V as input and one character X and
 * changes every first character of every element in V to X.
 */
class FirstCharChangerClass {
public:
    static SEXP createInstance(SEXP char_vec, SEXP replace_char);
private:
    static R_xlen_t Length(SEXP instance);
    static void * Dataptr(SEXP instance, Rboolean writeabble);
    static SEXP Elt(SEXP instance, R_xlen_t idx);
    static void Set_elt(SEXP instance, R_xlen_t idx, SEXP elem);
};

#endif // _FIRST_CHAR_CHANGER_CLASS_HPP_