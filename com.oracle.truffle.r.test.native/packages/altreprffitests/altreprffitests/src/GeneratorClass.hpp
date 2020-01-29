#ifndef _GENERATOR_CLASS_HPP_
#define _GENERATOR_CLASS_HPP_

#include <R.h>
#include <Rinternals.h>
#include <R_ext/Altrep.h>

class GeneratorClass {
public:
    static SEXP createInstance(SEXP data_length, SEXP generator_func, SEXP rho);
private:
    static int m_data_length;

    static R_xlen_t Length(SEXP instance);
    static void * Dataptr(SEXP instance, Rboolean writeabble);
    static int Elt(SEXP instance, R_xlen_t idx);
};

#endif // _GENERATOR_CLASS_HPP_