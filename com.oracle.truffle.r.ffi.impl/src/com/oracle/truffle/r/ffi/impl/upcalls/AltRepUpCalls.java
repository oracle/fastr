package com.oracle.truffle.r.ffi.impl.upcalls;

import com.oracle.truffle.r.ffi.impl.nodes.AltrepData1Node;
import com.oracle.truffle.r.ffi.impl.nodes.AltrepData2Node;
import com.oracle.truffle.r.ffi.impl.nodes.MakeAltComplexClassNode;
import com.oracle.truffle.r.ffi.impl.nodes.MakeAltIntegerClassNode;
import com.oracle.truffle.r.ffi.impl.nodes.MakeAltLogicalClassNode;
import com.oracle.truffle.r.ffi.impl.nodes.MakeAltRawClassNode;
import com.oracle.truffle.r.ffi.impl.nodes.MakeAltRealClassNode;
import com.oracle.truffle.r.ffi.impl.nodes.MakeAltStringClassNode;
import com.oracle.truffle.r.ffi.impl.nodes.NewAltRepNode;
import com.oracle.truffle.r.ffi.impl.nodes.SetAltrepData1Node;
import com.oracle.truffle.r.ffi.impl.nodes.SetAltrepData2Node;
import com.oracle.truffle.r.ffi.processor.RFFICpointer;
import com.oracle.truffle.r.ffi.processor.RFFIUpCallNode;

public interface AltRepUpCalls {
    int ALTREP(Object x);

    boolean R_altrep_inherits(Object instance, Object classDescriptor);

    @RFFIUpCallNode(AltrepData1Node.class)
    Object R_altrep_data1(Object instance);

    @RFFIUpCallNode(AltrepData2Node.class)
    Object R_altrep_data2(Object instance);

    @RFFIUpCallNode(SetAltrepData1Node.class)
    void R_set_altrep_data1(Object instance, Object data1);

    @RFFIUpCallNode(SetAltrepData2Node.class)
    void R_set_altrep_data2(Object instance, Object data2);

    @RFFIUpCallNode(MakeAltIntegerClassNode.class)
    Object R_make_altinteger_class(String className, String packageName, @RFFICpointer Object dllInfo);

    @RFFIUpCallNode(MakeAltRealClassNode.class)
    Object R_make_altreal_class(String className, String packageName, @RFFICpointer Object dllInfo);

    @RFFIUpCallNode(MakeAltLogicalClassNode.class)
    Object R_make_altlogical_class(String className, String packageName, @RFFICpointer Object dllInfo);

    @RFFIUpCallNode(MakeAltStringClassNode.class)
    Object R_make_altstring_class(String className, String packageName, @RFFICpointer Object dllInfo);

    @RFFIUpCallNode(MakeAltRawClassNode.class)
    Object R_make_altraw_class(String className, String packageName, @RFFICpointer Object dllInfo);

    @RFFIUpCallNode(MakeAltComplexClassNode.class)
    Object R_make_altcomplex_class(String className, String packageName, @RFFICpointer Object dllInfo);

    void R_set_altrep_Unserialize_method(Object classDescriptor, @RFFICpointer Object method);

    void R_set_altrep_UnserializeEX_method(Object classDescriptor, @RFFICpointer Object method);

    void R_set_altrep_Serialized_state_method(Object classDescriptor, @RFFICpointer Object method);

    void R_set_altrep_Duplicate_method(Object classDescriptor, @RFFICpointer Object method);

    void R_set_altrep_DuplicateEX_method(Object classDescriptor, @RFFICpointer Object method);

    void R_set_altrep_Coerce_method(Object classDescriptor, @RFFICpointer Object method);

    void R_set_altrep_Inspect_method(Object classDescriptor, @RFFICpointer Object method);

    void R_set_altrep_Length_method(Object classDescriptor, @RFFICpointer Object method);

    void R_set_altvec_Dataptr_method(Object classDescriptor, @RFFICpointer Object method);

    void R_set_altvec_Dataptr_or_null_method(Object classDescriptor, @RFFICpointer Object method);

    void R_set_altvec_Extract_subset_method(Object classDescriptor, @RFFICpointer Object method);

    void R_set_altinteger_Elt_method(Object classDescriptor, @RFFICpointer Object eltMethod);

    void R_set_altinteger_Get_region_method(Object classDescriptor, @RFFICpointer Object getRegionMethod);

    void R_set_altinteger_Is_sorted_method(Object classDescriptor, @RFFICpointer Object method);

    void R_set_altinteger_No_NA_method(Object classDescriptor, @RFFICpointer Object method);

    void R_set_altinteger_Sum_method(Object classDescriptor, @RFFICpointer Object method);

    void R_set_altinteger_Min_method(Object classDescriptor, @RFFICpointer Object method);

    void R_set_altinteger_Max_method(Object classDescriptor, @RFFICpointer Object method);

    void R_set_altreal_Elt_method(Object classDescriptor, @RFFICpointer Object eltMethod);

    void R_set_altreal_Get_region_method(Object classDescriptor, @RFFICpointer Object getRegionMethod);

    void R_set_altreal_Is_sorted_method(Object classDescriptor, @RFFICpointer Object method);

    void R_set_altreal_No_NA_method(Object classDescriptor, @RFFICpointer Object method);

    void R_set_altreal_Sum_method(Object classDescriptor, @RFFICpointer Object method);

    void R_set_altreal_Min_method(Object classDescriptor, @RFFICpointer Object method);

    void R_set_altreal_Max_method(Object classDescriptor, @RFFICpointer Object method);

    void R_set_altlogical_Elt_method(Object classDescriptor, @RFFICpointer Object eltMethod);

    void R_set_altlogical_Get_region_method(Object classDescriptor, @RFFICpointer Object getRegionMethod);

    void R_set_altlogical_Is_sorted_method(Object classDescriptor, @RFFICpointer Object method);

    void R_set_altlogical_No_NA_method(Object classDescriptor, @RFFICpointer Object method);

    void R_set_altlogical_Sum_method(Object classDescriptor, @RFFICpointer Object method);

    void R_set_altraw_Elt_method(Object classDescriptor, @RFFICpointer Object eltMethod);

    void R_set_altraw_Get_region_method(Object classDescriptor, @RFFICpointer Object getRegionMethod);

    void R_set_altcomplex_Elt_method(Object classDescriptor, @RFFICpointer Object eltMethod);

    void R_set_altcomplex_Get_region_method(Object classDescriptor, @RFFICpointer Object getRegionMethod);

    void R_set_altstring_Elt_method(Object classDescriptor, @RFFICpointer Object eltMethod);

    void R_set_altstring_Set_elt_method(Object classDescriptor, @RFFICpointer Object method);

    void R_set_altstring_Is_sorted_method(Object classDescriptor, @RFFICpointer Object method);

    void R_set_altstring_No_NA_method(Object classDescriptor, @RFFICpointer Object method);

    @RFFIUpCallNode(NewAltRepNode.class)
    Object R_new_altrep(Object classDescriptor, Object data1, Object data2);
}
