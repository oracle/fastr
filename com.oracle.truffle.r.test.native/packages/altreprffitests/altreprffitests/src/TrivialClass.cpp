#include "TrivialClass.hpp"


const int TrivialClass::m_content[TrivialClass::m_size] = {1, 2, 3, 4, 5};


SEXP TrivialClass::createInstance()
{
    R_altrep_class_t descr = R_make_altinteger_class("TrivialClass", "altreprffitests", nullptr);
    R_set_altrep_Length_method(descr, &Length);
    R_set_altvec_Dataptr_method(descr, &Dataptr);
    R_set_altinteger_Elt_method(descr, &Elt);
    return R_new_altrep(descr, R_NilValue, R_NilValue);
}

R_xlen_t TrivialClass::Length(SEXP instance)
{
    return m_size;
}

void * TrivialClass::Dataptr(SEXP instance, Rboolean writeabble)
{
    return (void *) m_content;
}

int TrivialClass::Elt(SEXP instance, R_xlen_t idx)
{
    if (idx >= m_size) {
        Rf_error("TrivialClass::Elt: Index out of range");
    }
    return m_content[idx];
}
