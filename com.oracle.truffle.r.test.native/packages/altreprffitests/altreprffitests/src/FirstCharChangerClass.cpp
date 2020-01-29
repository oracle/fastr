#include "FirstCharChangerClass.hpp"


SEXP FirstCharChangerClass::createInstance(SEXP char_vec, SEXP replace_char)
{
    if (TYPEOF(char_vec) != STRSXP || TYPEOF(replace_char) != STRSXP ||
        LENGTH(replace_char) > 1)
    {
        error("Wrong parameters");
    }

    auto descr = R_make_altstring_class("FirstCharChanger", "altreprffitests", nullptr);
    R_set_altrep_Length_method(descr, &Length);
    R_set_altvec_Dataptr_method(descr, &Dataptr);
    R_set_altstring_Elt_method(descr, &Elt);
    R_set_altstring_Set_elt_method(descr, &Set_elt);

    return R_new_altrep(descr, char_vec, STRING_ELT(replace_char, 0));
}

R_xlen_t FirstCharChangerClass::Length(SEXP instance)
{
    return LENGTH(R_altrep_data1(instance));
}

void * FirstCharChangerClass::Dataptr(SEXP instance, Rboolean writeabble)
{
    return DATAPTR(R_altrep_data1(instance));
}

SEXP FirstCharChangerClass::Elt(SEXP instance, R_xlen_t idx)
{
    SEXP char_vec = R_altrep_data1(instance);
    SEXP replace_char = R_altrep_data2(instance);
    if (TYPEOF(replace_char) != CHARSXP) {
        error("Internal error in Elt");
    }

    const char *old_string = translateChar(STRING_ELT(char_vec, idx));
    char *new_string = static_cast<char *>( std::malloc( std::strlen(old_string)));
    std::strcpy(new_string, old_string);
    new_string[0] = translateChar(replace_char)[0];
    SEXP new_elem = mkChar(new_string);
    // TODO: Valid here?
    std::free(new_string);
    return new_elem;
}

void FirstCharChangerClass::Set_elt(SEXP instance, R_xlen_t idx, SEXP elem)
{
    error("Should not reach here");
}
