#include "altrep_classes.hpp"
#include <string>

SEXP VecWrapper::createInstance(SEXP data, SEXP gen_Duplicate, SEXP gen_Coerce, SEXP gen_Elt, SEXP gen_Sum,
    SEXP gen_Min, SEXP gen_Max, SEXP gen_Get_region, SEXP gen_Is_sorted)
{
    Args args {
        (Rboolean) INTEGER_ELT(gen_Duplicate, 0),
        (Rboolean) INTEGER_ELT(gen_Coerce, 0),
        (Rboolean) INTEGER_ELT(gen_Elt, 0),
        (Rboolean) INTEGER_ELT(gen_Sum, 0),
        (Rboolean) INTEGER_ELT(gen_Min, 0),
        (Rboolean) INTEGER_ELT(gen_Max, 0),
        (Rboolean) INTEGER_ELT(gen_Get_region, 0),
        (Rboolean) INTEGER_ELT(gen_Is_sorted, 0)
    };
    R_altrep_class_t descr = createDescriptor(TYPEOF(data), args);

    // Duplicate instance data
    SEXP duplicated_data = Rf_duplicate(data);
    return R_new_altrep(descr, duplicated_data, R_NilValue);
}

SEXP VecWrapper::createInstanceFromArgs(SEXP data, const Args &args)
{
    R_altrep_class_t descr = createDescriptor(TYPEOF(data), args);
    return R_new_altrep(descr, data, R_NilValue);
}

void VecWrapper::registerCommonMethods(R_altrep_class_t descr, const Args &args)
{
    // Set mandatory methods
    R_set_altrep_Length_method(descr, Length);
    R_set_altvec_Dataptr_method(descr, Dataptr);

    if (args.gen_Coerce) R_set_altrep_Coerce_method(descr, Coerce);
    if (args.gen_Duplicate) R_set_altrep_Duplicate_method(descr, Duplicate);

    R_set_altrep_Inspect_method(descr, Inspect);
}

R_altrep_class_t VecWrapper::createDescriptor(int type, const Args &args)
{
    switch (type) {
        case INTSXP:
            return SimpleIntVecWrapper::createDescriptor(args);
        case REALSXP:
            return SimpleRealVecWrapper::createDescriptor(args);
        case LGLSXP:
            return SimpleLogicalVecWrapper::createDescriptor(args);
        case RAWSXP:
            return SimpleRawVecWrapper::createDescriptor(args);
        case CPLXSXP:
            return SimpleComplexVecWrapper::createDescriptor(args);
        case STRSXP:
            return SimpleStringVecWrapper::createDescriptor(args);
        default:
            error("Not implemented");
    }
}

void * VecWrapper::Dataptr(SEXP instance, Rboolean writeabble)
{
    return DATAPTR(R_altrep_data1(instance));
}

SEXP VecWrapper::Duplicate(SEXP instance, Rboolean deep)
{
    return Rf_duplicate(R_altrep_data1(instance));
}

SEXP VecWrapper::Coerce(SEXP instance, int type)
{
    return Rf_coerceVector(R_altrep_data1(instance), type);
}

R_xlen_t VecWrapper::Length(SEXP instance)
{
    return LENGTH(R_altrep_data1(instance));
}

Rboolean VecWrapper::Inspect(SEXP x, int pre, int deep, int pvec, void (*inspect_subtree)(SEXP, int, int, int))
{
    Rprintf("VecWrapper class\n");
    return TRUE;
}

R_altrep_class_t SimpleIntVecWrapper::createDescriptor(const Args &args)
{
    R_altrep_class_t desc = R_make_altinteger_class("int_class", "altreprffitests", NULL);
    registerCommonMethods(desc, args);
    if (args.gen_Elt) R_set_altinteger_Elt_method(desc, Elt);
    if (args.gen_Sum) R_set_altinteger_Sum_method(desc, Sum);
    if (args.gen_Min) R_set_altinteger_Min_method(desc, Min);
    if (args.gen_Max) R_set_altinteger_Max_method(desc, Max);
    if (args.gen_Get_region) R_set_altinteger_Get_region_method(desc, Get_region);
    if (args.gen_Is_sorted) R_set_altinteger_Is_sorted_method(desc, Is_sorted);
    return desc;
}

int SimpleIntVecWrapper::Elt(SEXP instance, R_xlen_t idx)
{
    return INTEGER_ELT(R_altrep_data1(instance), idx);
}

SEXP SimpleIntVecWrapper::Sum(SEXP instance, Rboolean na_rm)
{
    SEXP data = R_altrep_data1(instance);
    int acc = 0;
    for (int i = 0; i < LENGTH(data); i++) {
        acc += INTEGER_ELT(data, i);
    }
    return ScalarInteger(acc);
}

SEXP SimpleIntVecWrapper::Max(SEXP instance, Rboolean na_rm)
{
    int max = -1000000;
    SEXP data = R_altrep_data1(instance);
    for (int i = 0; i < LENGTH(data); i++) {
        if (INTEGER_ELT(data, i) > max)
            max = INTEGER_ELT(data, i);
    }
    return ScalarInteger(max);
}

SEXP SimpleIntVecWrapper::Min(SEXP instance, Rboolean na_rm)
{
    int min = 1000000;
    SEXP data = R_altrep_data1(instance);
    for (int i = 0; i < LENGTH(data); i++) {
        if (INTEGER_ELT(data, i) < min)
            min = INTEGER_ELT(data, i);
    }
    return ScalarInteger(min);
}

R_xlen_t SimpleIntVecWrapper::Get_region(SEXP instance, R_xlen_t from_idx, R_xlen_t size, int *buffer)
{
    SEXP data = R_altrep_data1(instance);
    if (size > LENGTH(data) ||
        (!(0 <= from_idx && from_idx < LENGTH(data))) ||
        buffer == NULL)
    {
        return R_NaInt;
    }
    int *data_ptr = INTEGER(data);
    std::memcpy(buffer, data_ptr + from_idx, size * sizeof(int));
    return size;
}

int SimpleIntVecWrapper::Is_sorted(SEXP instance)
{
    SEXP data = R_altrep_data1(instance);
    int *data_ptr = INTEGER(data);

    Rboolean ascending = TRUE;
    for (int i = 1; i < LENGTH(data); i++) {
        if (!(data_ptr[i-1] <= data_ptr[i]))
            ascending = FALSE;
    }
    if (ascending)
        return SORTED_INCR;
    
    Rboolean descending = TRUE;
    for (int i = 1; i < LENGTH(data); i++) {
        if (!(data_ptr[i-1] >= data_ptr[i]))
            descending = FALSE;
    }
    if (descending)
        return SORTED_DECR;
    
    return KNOWN_UNSORTED;
}


R_altrep_class_t SimpleRealVecWrapper::createDescriptor(const Args &args)
{
    R_altrep_class_t desc = R_make_altreal_class("SimpleRealVecWrapper", "altreprffitests", NULL);
    registerCommonMethods(desc, args);
    if (args.gen_Elt) R_set_altreal_Elt_method(desc, Elt);
    if (args.gen_Sum) R_set_altreal_Sum_method(desc, Sum);
    if (args.gen_Min) R_set_altreal_Min_method(desc, Min);
    if (args.gen_Max) R_set_altreal_Max_method(desc, Max);
    if (args.gen_Get_region) R_set_altreal_Get_region_method(desc, Get_region);
    if (args.gen_Is_sorted) R_set_altreal_Is_sorted_method(desc, Is_sorted);
    return desc;
}

double SimpleRealVecWrapper::Elt(SEXP instance, R_xlen_t idx)
{
    return REAL_ELT(R_altrep_data1(instance), idx);
}

SEXP SimpleRealVecWrapper::Sum(SEXP instance, Rboolean na_rm)
{
    SEXP data = R_altrep_data1(instance);
    int acc = 0;
    for (int i = 0; i < LENGTH(data); i++) {
        acc += REAL_ELT(data, i);
    }
    return ScalarReal(acc);
}

SEXP SimpleRealVecWrapper::Max(SEXP instance, Rboolean na_rm)
{
    double max = 0;
    SEXP data = R_altrep_data1(instance);
    for (int i = 0; i < LENGTH(data); i++) {
        if (REAL_ELT(data, i) > max)
            max = REAL_ELT(data, i);
    }
    return ScalarReal(max);
}

SEXP SimpleRealVecWrapper::Min(SEXP instance, Rboolean na_rm)
{
    double min = 1000000;
    SEXP data = R_altrep_data1(instance);
    for (int i = 0; i < LENGTH(data); i++) {
        if (REAL_ELT(data, i) < min)
            min = REAL_ELT(data, i);
    }
    return ScalarReal(min);
}

R_xlen_t SimpleRealVecWrapper::Get_region(SEXP instance, R_xlen_t from_idx, R_xlen_t size, double *buffer)
{
    SEXP data = R_altrep_data1(instance);
    if (size > LENGTH(data) ||
        (!(0 <= from_idx && from_idx < LENGTH(data))) ||
        buffer == NULL)
    {
        return R_NaInt;
    }
    double *data_ptr = REAL(data);
    std::memcpy(buffer, data_ptr + from_idx, size * sizeof(double));
    return size;
}

int SimpleRealVecWrapper::Is_sorted(SEXP instance)
{
    SEXP data = R_altrep_data1(instance);
    double *data_ptr = REAL(data);

    Rboolean ascending = TRUE;
    for (int i = 1; i < LENGTH(data); i++) {
        if (!(data_ptr[i-1] <= data_ptr[i]))
            ascending = FALSE;
    }
    if (ascending)
        return SORTED_INCR;
    
    Rboolean descending = TRUE;
    for (int i = 1; i < LENGTH(data); i++) {
        if (!(data_ptr[i-1] >= data_ptr[i]))
            descending = FALSE;
    }
    if (descending)
        return SORTED_DECR;
    
    return KNOWN_UNSORTED;
}


R_altrep_class_t SimpleLogicalVecWrapper::createDescriptor(const Args &args)
{
    R_altrep_class_t desc = R_make_altlogical_class("SimpleLogicalVecWrapper", "altreprffitests", nullptr);
    registerCommonMethods(desc, args);
    if (args.gen_Elt) R_set_altlogical_Elt_method(desc, Elt);
    if (args.gen_Sum) R_set_altlogical_Sum_method(desc, Sum);
    if (args.gen_Get_region) R_set_altlogical_Get_region_method(desc, Get_region);
    if (args.gen_Is_sorted) R_set_altlogical_Is_sorted_method(desc, Is_sorted);
    return desc;
}

int SimpleLogicalVecWrapper::Elt(SEXP instance, R_xlen_t idx)
{
    return LOGICAL_ELT(R_altrep_data1(instance), idx);
}

SEXP SimpleLogicalVecWrapper::Sum(SEXP instance, Rboolean na_rm)
{
    SEXP data = R_altrep_data1(instance);
    int acc = 0;
    for (int i = 0; i < LENGTH(data); i++) {
        acc += LOGICAL_ELT(data, i);
    }
    return ScalarLogical(acc);
}

R_xlen_t SimpleLogicalVecWrapper::Get_region(SEXP instance, R_xlen_t from_idx, R_xlen_t size, int *buffer)
{
    return LOGICAL_GET_REGION(R_altrep_data1(instance), from_idx, size, buffer);
}

int SimpleLogicalVecWrapper::Is_sorted(SEXP instance)
{
    return UNKNOWN_SORTEDNESS;
}

R_altrep_class_t SimpleRawVecWrapper::createDescriptor(const Args &args)
{
    R_altrep_class_t descr = R_make_altraw_class("SimpleRawVecWrapper", "altreprffitests", nullptr);
    registerCommonMethods(descr, args);
    if (args.gen_Elt) R_set_altraw_Elt_method(descr, Elt);
    if (args.gen_Get_region) R_set_altraw_Get_region_method(descr, Get_region);
    return descr;
}

Rbyte SimpleRawVecWrapper::Elt(SEXP instance, R_xlen_t idx)
{
    return RAW_ELT(R_altrep_data1(instance), idx);
}

R_xlen_t SimpleRawVecWrapper::Get_region(SEXP instance, R_xlen_t from_idx, R_xlen_t size, Rbyte *buffer)
{
    return RAW_GET_REGION(R_altrep_data1(instance), from_idx, size, buffer);
}

R_altrep_class_t SimpleComplexVecWrapper::createDescriptor(const Args &args)
{
    R_altrep_class_t descr = R_make_altcomplex_class("SimpleComplexVecWrapper", "altreprffitests", nullptr);
    registerCommonMethods(descr, args);
    if (args.gen_Elt) R_set_altcomplex_Elt_method(descr, Elt);
    if (args.gen_Get_region) R_set_altcomplex_Get_region_method(descr, Get_region);
    return descr;
}

Rcomplex SimpleComplexVecWrapper::Elt(SEXP instance, R_xlen_t idx)
{
    return COMPLEX_ELT(R_altrep_data1(instance), idx);
}

R_xlen_t SimpleComplexVecWrapper::Get_region(SEXP instance, R_xlen_t from_idx, R_xlen_t size, Rcomplex *buffer)
{
    return COMPLEX_GET_REGION(R_altrep_data1(instance), from_idx, size, buffer);
}


R_altrep_class_t SimpleStringVecWrapper::createDescriptor(const Args &args)
{
    R_altrep_class_t descr = R_make_altstring_class("SimpleStringVecWrapper", "altreprffitests", nullptr);
    registerCommonMethods(descr, args);
    // Altstring must have Elt, Set_Elt defined
    R_set_altstring_Elt_method(descr, Elt);
    R_set_altstring_Set_elt_method(descr, Set_elt);
    if (args.gen_Is_sorted) R_set_altstring_Is_sorted_method(descr, Is_sorted);
    return descr;
}

SEXP SimpleStringVecWrapper::Elt(SEXP instance, R_xlen_t idx)
{
    return STRING_ELT(R_altrep_data1(instance), idx);
}

void SimpleStringVecWrapper::Set_elt(SEXP instance, R_xlen_t idx, SEXP value)
{
    SET_STRING_ELT(R_altrep_data1(instance), idx, value);
}

int SimpleStringVecWrapper::Is_sorted(SEXP instance)
{
    return STRING_IS_SORTED(R_altrep_data1(instance));
}

/***********************************************************************/
/************************* LoggingVecWrapper ***************************/
/***********************************************************************/
std::set< std::pair< SEXP, Method>> LoggingVecWrapper::m_called_methods;


SEXP LoggingVecWrapper::createInstance(SEXP data, SEXP gen_Duplicate, SEXP gen_Coerce, SEXP gen_Elt, SEXP gen_Sum,
    SEXP gen_Min, SEXP gen_Max, SEXP gen_Get_region, SEXP gen_Is_sorted)
{
    Args args {
        (Rboolean) INTEGER_ELT(gen_Duplicate, 0),
        (Rboolean) INTEGER_ELT(gen_Coerce, 0),
        (Rboolean) INTEGER_ELT(gen_Elt, 0),
        (Rboolean) INTEGER_ELT(gen_Sum, 0),
        (Rboolean) INTEGER_ELT(gen_Min, 0),
        (Rboolean) INTEGER_ELT(gen_Max, 0),
        (Rboolean) INTEGER_ELT(gen_Get_region, 0),
        (Rboolean) INTEGER_ELT(gen_Is_sorted, 0)
    };
    R_altrep_class_t descr = createDescriptor(TYPEOF(data), args);

    // Duplicate instance data
    SEXP duplicated_data = Rf_duplicate(data);
    return R_new_altrep(descr, duplicated_data, R_NilValue);
}

SEXP LoggingVecWrapper::wasMethodCalled(SEXP instance, SEXP method_type)
{
    std::string method_type_str(CHAR(Rf_asChar(method_type)));
    Method method;
    if (method_type_str == "Duplicate") {
        method = Method::Duplicate;
    }
    else if (method_type_str == "Coerce") {
        method = Method::Coerce;
    }
    else if (method_type_str == "Elt") {
        method = Method::Elt;
    }
    else if (method_type_str == "Sum") {
        method = Method::Sum;
    }
    else if (method_type_str == "Min") {
        method = Method::Min;
    }
    else if (method_type_str == "Max") {
        method = Method::Max;
    }
    else if (method_type_str == "Get_region") {
        method = Method::Get_region;
    }
    else if (method_type_str == "Is_sorted") {
        method = Method::Is_sorted;
    }
    else {
        Rf_error("Unknown method type: %s", method_type_str.c_str());
    }
    
    auto it = m_called_methods.find(std::make_pair(instance, method));
    bool was_called = it != m_called_methods.end();
    return ScalarLogical(was_called);
}

SEXP LoggingVecWrapper::clearCalledMethods()
{
    m_called_methods.clear();
    return R_NilValue;
}

void LoggingVecWrapper::logMethodCall(Method method_type, SEXP instance)
{
    m_called_methods.insert(std::make_pair(instance, method_type));
}

void LoggingVecWrapper::registerCommonMethods(R_altrep_class_t descr, const Args &args)
{
    // Set mandatory methods
    R_set_altrep_Length_method(descr, Length);
    R_set_altvec_Dataptr_method(descr, Dataptr);

    if (args.gen_Coerce) R_set_altrep_Coerce_method(descr, Coerce);
    if (args.gen_Duplicate) R_set_altrep_Duplicate_method(descr, Duplicate);

    R_set_altrep_Inspect_method(descr, Inspect);
}

R_altrep_class_t LoggingVecWrapper::createDescriptor(int type, const Args &args)
{
    switch (type) {
        case INTSXP:
            return LoggingIntVecWrapper::createDescriptor(args);
        case REALSXP:
            return LoggingRealVecWrapper::createDescriptor(args);
        default:
            error("Not yet implemented");
    }
}

void * LoggingVecWrapper::Dataptr(SEXP instance, Rboolean writeabble)
{
    logMethodCall(Method::Dataptr, instance);
    return DATAPTR(R_altrep_data1(instance));
}

SEXP LoggingVecWrapper::Duplicate(SEXP instance, Rboolean deep)
{
    logMethodCall(Method::Duplicate, instance);
    return Rf_duplicate(R_altrep_data1(instance));
}

SEXP LoggingVecWrapper::Coerce(SEXP instance, int type)
{
    logMethodCall(Method::Coerce, instance);
    return Rf_coerceVector(R_altrep_data1(instance), type);
}

R_xlen_t LoggingVecWrapper::Length(SEXP instance)
{
    logMethodCall(Method::Length, instance);
    return LENGTH(R_altrep_data1(instance));
}

Rboolean LoggingVecWrapper::Inspect(SEXP x, int pre, int deep, int pvec, void (*inspect_subtree)(SEXP, int, int, int))
{
    Rprintf("LoggingVecWrapper class\n");
    return TRUE;
}

R_altrep_class_t LoggingIntVecWrapper::createDescriptor(const Args &args)
{
    R_altrep_class_t desc = R_make_altinteger_class("LoggingIntVecWrapper", "altreprffitests", NULL);
    LoggingVecWrapper::registerCommonMethods(desc, args);
    if (args.gen_Elt) R_set_altinteger_Elt_method(desc, LoggingIntVecWrapper::Elt);
    if (args.gen_Sum) R_set_altinteger_Sum_method(desc, LoggingIntVecWrapper::Sum);
    if (args.gen_Min) R_set_altinteger_Min_method(desc, LoggingIntVecWrapper::Min);
    if (args.gen_Max) R_set_altinteger_Max_method(desc, LoggingIntVecWrapper::Max);
    if (args.gen_Get_region) R_set_altinteger_Get_region_method(desc, LoggingIntVecWrapper::Get_region);
    if (args.gen_Is_sorted) R_set_altinteger_Is_sorted_method(desc, LoggingIntVecWrapper::Is_sorted);
    return desc;
}

int LoggingIntVecWrapper::Elt(SEXP instance, R_xlen_t idx)
{
    logMethodCall(Method::Elt, instance);
    return SimpleIntVecWrapper::Elt(instance, idx);
}

SEXP LoggingIntVecWrapper::Sum(SEXP instance, Rboolean na_rm)
{
    logMethodCall(Method::Sum, instance);
    return SimpleIntVecWrapper::Sum(instance, na_rm);
}

SEXP LoggingIntVecWrapper::Max(SEXP instance, Rboolean na_rm)
{
    logMethodCall(Method::Max, instance);
    return SimpleIntVecWrapper::Max(instance, na_rm);
}

SEXP LoggingIntVecWrapper::Min(SEXP instance, Rboolean na_rm)
{
    logMethodCall(Method::Min, instance);
    return SimpleIntVecWrapper::Min(instance, na_rm);
}

R_xlen_t LoggingIntVecWrapper::Get_region(SEXP instance, R_xlen_t from_idx, R_xlen_t size, int *buffer)
{
    logMethodCall(Method::Get_region, instance);
    return SimpleIntVecWrapper::Get_region(instance, from_idx, size, buffer);
}

int LoggingIntVecWrapper::Is_sorted(SEXP instance)
{
    logMethodCall(Method::Is_sorted, instance);
    return SimpleIntVecWrapper::Is_sorted(instance);
}

R_altrep_class_t LoggingRealVecWrapper::createDescriptor(const Args &args)
{
    R_altrep_class_t desc = R_make_altreal_class("LoggingRealVecWrapper", "altreprffitests", NULL);
    LoggingVecWrapper::registerCommonMethods(desc, args);
    if (args.gen_Elt) R_set_altreal_Elt_method(desc, LoggingRealVecWrapper::Elt);
    if (args.gen_Sum) R_set_altreal_Sum_method(desc, LoggingRealVecWrapper::Sum);
    if (args.gen_Min) R_set_altreal_Min_method(desc, LoggingRealVecWrapper::Min);
    if (args.gen_Max) R_set_altreal_Max_method(desc, LoggingRealVecWrapper::Max);
    if (args.gen_Get_region) R_set_altreal_Get_region_method(desc, LoggingRealVecWrapper::Get_region);
    if (args.gen_Is_sorted) R_set_altreal_Is_sorted_method(desc, LoggingRealVecWrapper::Is_sorted);
    return desc;
}

double LoggingRealVecWrapper::Elt(SEXP instance, R_xlen_t idx)
{
    logMethodCall(Method::Elt, instance);
    return SimpleRealVecWrapper::Elt(instance, idx);
}

SEXP LoggingRealVecWrapper::Sum(SEXP instance, Rboolean na_rm)
{
    logMethodCall(Method::Sum, instance);
    return SimpleRealVecWrapper::Sum(instance, na_rm);
}

SEXP LoggingRealVecWrapper::Max(SEXP instance, Rboolean na_rm)
{
    logMethodCall(Method::Max, instance);
    return SimpleRealVecWrapper::Max(instance, na_rm);
}

SEXP LoggingRealVecWrapper::Min(SEXP instance, Rboolean na_rm)
{
    logMethodCall(Method::Min, instance);
    return SimpleRealVecWrapper::Min(instance, na_rm);
}

R_xlen_t LoggingRealVecWrapper::Get_region(SEXP instance, R_xlen_t from_idx, R_xlen_t size, double *buffer)
{
    logMethodCall(Method::Get_region, instance);
    return SimpleRealVecWrapper::Get_region(instance, from_idx, size, buffer);
}

int LoggingRealVecWrapper::Is_sorted(SEXP instance)
{
    logMethodCall(Method::Is_sorted, instance);
    return SimpleRealVecWrapper::Is_sorted(instance);
}


int *NativeMemVec::native_mem_ptr = nullptr;
int NativeMemVec::data_length = 0;

SEXP NativeMemVec::createInstance(SEXP data_length)
{
    // Create descriptor
    R_altrep_class_t descr = R_make_altinteger_class("NativeMemVec", "altreprffitests", NULL);
    R_set_altrep_Length_method(descr, Length);
    R_set_altvec_Dataptr_method(descr, Dataptr);
    R_set_altinteger_Elt_method(descr, Elt);

    // Allocate native memory
    NativeMemVec::data_length = INTEGER_ELT(data_length, 0);
    NativeMemVec::native_mem_ptr = new int[NativeMemVec::data_length];

    return R_new_altrep(descr, R_NilValue, R_NilValue);
}

SEXP NativeMemVec::deleteInstance(SEXP instance)
{
    delete native_mem_ptr;
    return R_NilValue;
}

void * NativeMemVec::Dataptr(SEXP instance, Rboolean writeabble)
{
    return (void *)native_mem_ptr;
}

R_xlen_t NativeMemVec::Length(SEXP instance)
{
    return data_length;
}

int NativeMemVec::Elt(SEXP instance, R_xlen_t idx)
{
    return native_mem_ptr[idx];
}
