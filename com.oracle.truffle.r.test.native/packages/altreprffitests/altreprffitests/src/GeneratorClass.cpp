
#include "GeneratorClass.hpp"

int GeneratorClass::m_data_length = 0;

SEXP GeneratorClass::createInstance(SEXP data_length, SEXP generator_func, SEXP rho)
{
    if (TYPEOF(data_length) != INTSXP || TYPEOF(generator_func) != SYMSXP
        || TYPEOF(rho) != ENVSXP)
    {
        error("Wrong parameter types");
    }
    m_data_length = INTEGER_ELT(data_length, 0);
    SEXP fcall = lang2(generator_func, R_NilValue);

    R_altrep_class_t descr = R_make_altinteger_class("GeneratorClass", "altreprffitests", NULL);
    R_set_altrep_Length_method(descr, &Length);
    R_set_altvec_Dataptr_method(descr, &Dataptr);
    R_set_altinteger_Elt_method(descr, &Elt);
    return R_new_altrep(descr, fcall, rho);
}

R_xlen_t GeneratorClass::Length(SEXP instance)
{
    return static_cast<R_xlen_t>(m_data_length);
}

void * GeneratorClass::Dataptr(SEXP instance, Rboolean writeabble)
{
    // Deliberate violation of Dataptr contract.
    error("GeneratorClass::Dataptr should not be called");
}

int GeneratorClass::Elt(SEXP instance, R_xlen_t idx)
{
    SEXP fcall = R_altrep_data1(instance);
    SEXP rho = R_altrep_data2(instance);
    // Note that there needs to be a conversion from 0-based C index to 1-base R index.
    SETCADR(fcall, ScalarInteger(idx + 1));
    // call f(idx)
    SEXP elem = eval(fcall, rho);
    if (TYPEOF(elem) == INTSXP) {
        return INTEGER_ELT(elem, 0);
    }
    else {
        error("elem should be integer");
    }
}
