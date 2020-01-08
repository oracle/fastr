#ifndef ALTREP_CLASSES_HPP_
#define ALTREP_CLASSES_HPP_

#include <R.h>
#include <Rinternals.h>
#include <R_ext/Rdynload.h>
#include <R_ext/Altrep.h>
#include <set>


struct Args {
    Rboolean gen_Duplicate;
    Rboolean gen_Coerce;
    Rboolean gen_Elt;
    Rboolean gen_Sum;
    Rboolean gen_Min;
    Rboolean gen_Max;
    Rboolean gen_Get_region;
    Rboolean gen_Is_sorted;
};

enum class Method {
    Duplicate,
    Coerce,
    Length,
    Dataptr,
    Elt,
    Sum,
    Max,
    Min,
    Get_region,
    Is_sorted
};

class VecWrapper {
public:
    static SEXP createInstance(SEXP data, SEXP gen_Duplicate, SEXP gen_Coerce, SEXP gen_Elt, SEXP gen_Sum,
        SEXP gen_Min, SEXP gen_Max, SEXP gen_Get_region, SEXP gen_Is_sorted);
    static SEXP createInstanceFromArgs(SEXP data, const Args &args);
protected:
    static void registerCommonMethods(R_altrep_class_t descr, const Args &args);
private:
    static R_altrep_class_t createDescriptor(int type, const Args &args);
    static void * Dataptr(SEXP instance, Rboolean writeabble);
    static SEXP Duplicate(SEXP instance, Rboolean deep);
    static SEXP Coerce(SEXP instance, int type);
    static R_xlen_t Length(SEXP instance);
    static Rboolean Inspect(SEXP x, int pre, int deep, int pvec, void (*inspect_subtree)(SEXP, int, int, int));
};

class SimpleIntVecWrapper : public VecWrapper {
public:
    static R_altrep_class_t createDescriptor(const Args &args);
protected:
    static int Elt(SEXP instance, R_xlen_t idx);
    static SEXP Sum(SEXP instance, Rboolean na_rm);
    static SEXP Max(SEXP instance, Rboolean na_rm);
    static SEXP Min(SEXP instance, Rboolean na_rm);
    static R_xlen_t Get_region(SEXP instance, R_xlen_t from_idx, R_xlen_t size, int *buffer);
    static int Is_sorted(SEXP instnace);
};

class SimpleRealVecWrapper : public VecWrapper {
public:
    static R_altrep_class_t createDescriptor(const Args &args);
protected:
    static double Elt(SEXP instance, R_xlen_t idx);
    static SEXP Sum(SEXP instance, Rboolean na_rm);
    static SEXP Max(SEXP instance, Rboolean na_rm);
    static SEXP Min(SEXP instance, Rboolean na_rm);
    static R_xlen_t Get_region(SEXP instance, R_xlen_t from_idx, R_xlen_t size, double *buffer);
    static int Is_sorted(SEXP instnace);
};


class LoggingVecWrapper {
public:
    static SEXP createInstance(SEXP data, SEXP gen_Duplicate, SEXP gen_Coerce, SEXP gen_Elt, SEXP gen_Sum,
        SEXP gen_Min, SEXP gen_Max, SEXP gen_Get_region, SEXP gen_Is_sorted);
    static SEXP wasMethodCalled(SEXP instance, SEXP method_type_str);
    static SEXP clearCalledMethods();
protected:
    static void logMethodCall(Method method_type, SEXP instance);
    static void registerCommonMethods(R_altrep_class_t descr, const Args &args);
private:
    static std::set< std::pair< SEXP, Method>> m_called_methods;
    static R_altrep_class_t createDescriptor(int type, const Args &args);
    static void * Dataptr(SEXP instance, Rboolean writeabble);
    static SEXP Duplicate(SEXP instance, Rboolean deep);
    static SEXP Coerce(SEXP instance, int type);
    static R_xlen_t Length(SEXP instance);
    static Rboolean Inspect(SEXP x, int pre, int deep, int pvec, void (*inspect_subtree)(SEXP, int, int, int));
};

class LoggingIntVecWrapper : public LoggingVecWrapper, SimpleIntVecWrapper {
public:
    static R_altrep_class_t createDescriptor(const Args &args);
private:
    static int Elt(SEXP instance, R_xlen_t idx);
    static SEXP Sum(SEXP instance, Rboolean na_rm);
    static SEXP Max(SEXP instance, Rboolean na_rm);
    static SEXP Min(SEXP instance, Rboolean na_rm);
    static R_xlen_t Get_region(SEXP instance, R_xlen_t from_idx, R_xlen_t size, int *buffer);
    static int Is_sorted(SEXP instnace);
};

class LoggingRealVecWrapper : public LoggingVecWrapper, SimpleRealVecWrapper {
public:
    static R_altrep_class_t createDescriptor(const Args &args);
private:
    static double Elt(SEXP instance, R_xlen_t idx);
    static SEXP Sum(SEXP instance, Rboolean na_rm);
    static SEXP Max(SEXP instance, Rboolean na_rm);
    static SEXP Min(SEXP instance, Rboolean na_rm);
    static R_xlen_t Get_region(SEXP instance, R_xlen_t from_idx, R_xlen_t size, double *buffer);
    static int Is_sorted(SEXP instnace);
};

#endif //ALTREP_CLASSES_HPP_