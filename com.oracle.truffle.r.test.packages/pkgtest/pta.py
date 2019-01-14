_pta_main_class = 'com.oracle.truffle.r.test.packages.analyzer.PTAMain'


def _pta_project():
    return 'com.oracle.truffle.r.test.packages.analyzer'


def pta(args, **kwargs):
    '''
    Run analysis for package installation/testing results.
    '''
    vmArgs = mx.get_runtime_jvm_args(_pta_project())
    vmArgs += [_pta_main_class]
    mx.run_java(vmArgs + args)
