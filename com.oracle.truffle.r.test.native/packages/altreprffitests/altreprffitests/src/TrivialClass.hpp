#ifndef TRIVIAL_CLASS_HPP_
#define TRIVIAL_CLASS_HPP_

#include <R.h>
#include <Rinternals.h>
#include <R_ext/Altrep.h>
#include <vector>


/**
 * Simplest possible altrep class that does not have any data. To Dataptr it returns an address
 * to statically-allocated buffer.
 */
class TrivialClass {
public:
    static SEXP createInstance();

private:
    static constexpr size_t m_size = 5;
    static const int m_content[m_size];

    static R_xlen_t Length(SEXP instance);
    static void * Dataptr(SEXP instance, Rboolean writeabble);
    static int Elt(SEXP instance, R_xlen_t idx);
};

#endif //TRIVIAL_CLASS_HPP_