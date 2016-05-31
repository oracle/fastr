#
# Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 2 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 2 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
# or visit www.oracle.com if you need additional information or have any
# questions.
#
import platform, subprocess, sys
from os.path import join, sep
from argparse import ArgumentParser
import mx
import mx_gate
import mx_fastr_pkgs
import os

'''
This is the launchpad for all the functions available for building/running/testing/analyzing
FastR. Ideally this code would be completely VM agnostic, i.e., be able to build/run with a variety
of VMs without change. That is currently not feasible due to the requirement that building
must use an pre-existing VM and running (for performance testing) must use a Graal-enabled VM.
It would require separate build/run steps to finesse this. However, running under a standards
VM is supported by dynamically checking if the jvmci suite is available
'''

_fastr_suite = mx.suite('fastr')
'''
If this is None, then we run under the standard VM in interpreted mode only.
Even if this is not None the global mx option --vm original forces interpreted mode
'''
_mx_jvmci = mx.suite("jvmci", fatalIfMissing=False)

class FakeJVMCI:
    def get_vm(self):
        # should only happen if jvmci vm selected
        mx.abort('FakeJVMCI.get_vm called')

    def get_jvmci_jdk(self):
        return mx.get_jdk()

    def build(self, args):
        return mx.build(args)

def mx_jvm():
    '''
    Check if the jvmci suite is available and if not return a fake one
    that uses the standard vm
    '''
    if not _mx_jvmci:
        return FakeJVMCI()
    else:
        return _mx_jvmci.extensions

_r_command_project = 'com.oracle.truffle.r.engine'
_repl_command = 'com.oracle.truffle.tools.debug.shell.client.SimpleREPLClient'
_command_class_dict = {'r': _r_command_project + ".shell.RCommand",
                       'rscript': _r_command_project + ".shell.RscriptCommand",
                        'rrepl': _repl_command}

def do_run_r(args, command, extraVmArgs=None, jdk=None, **kwargs):
    '''
    This is the basic function that runs a FastR process, where args have already been parsed.
    Args:
      args: a list of command arguments
      command: e.g. 'R', implicitly defines the entry class (can be None for AOT)
      extraVmArgs: additional vm arguments
      jdk: jdk (an mx.JDKConfig instance) to use
      **kwargs other keyword args understood by run_java
      nonZeroIsFatal: whether to terminate the execution run fails
      out,err possible redirects to collect output

    By default a non-zero return code will cause an mx.abort, unless nonZeroIsFatal=False
    The assumption is that the VM is already built and available.
    '''
    env = kwargs['env'] if 'env' in kwargs else os.environ

    setREnvironment(env)
    if not jdk:
        jdk = get_default_jdk()

    vmArgs = ['-cp', mx.classpath(_r_command_project)]

    vmArgs += _graal_options()

    if extraVmArgs is None or not '-da' in extraVmArgs:
        # unless explicitly disabled we enable assertion checking
        vmArgs += ['-ea', '-esa']

    if extraVmArgs:
        vmArgs += extraVmArgs

    vmArgs = _sanitize_vmArgs(jdk, vmArgs)
    if command:
        vmArgs.append(_command_class_dict[command.lower()])
    return mx.run_java(vmArgs + args, jdk=jdk, **kwargs)

def _sanitize_vmArgs(jdk, vmArgs):
    '''
    jdk/vm dependent analysis of vmArgs to remove those that are not appropriate for the
    chosen jdk/vm. It is easier to allow clients to set anything they want and filter them
    out here.
    '''
    if jdk.tag == 'jvmci':
        vm = mx_jvm().get_vm()
    else:
        vm = None
    xargs = []
    i = 0
    while i < len(vmArgs):
        vmArg = vmArgs[i]
        if 'graal' in vmArg or 'JVMCI' in vmArg:
            if vm and vm == "original":
                i = i + 1
                continue
        xargs.append(vmArg)
        i = i + 1
    return xargs

def _graal_options(nocompile=False):
    if _mx_jvmci and not mx_jvm().get_vm().endswith('nojvmci'):
        result = ['-Dgraal.InliningDepthError=500', '-Dgraal.EscapeAnalysisIterations=3', '-XX:JVMCINMethodSizeLimit=1000000']
        if nocompile:
            result += ['-Dgraal.TruffleCompilationThreshold=100000']
        return result
    else:
        return []

def _get_ldpaths(lib_env_name):
    ldpaths = os.path.join(os.environ['R_HOME'], 'etc', 'ldpaths')
    command = ['bash', '-c', 'source ' + ldpaths + ' && env']

    try:
        proc = subprocess.Popen(command, stdout=subprocess.PIPE)
        for line in proc.stdout:
            (key, _, value) = line.partition("=")
            if key == lib_env_name:
                return value.rstrip()
        # error if not found
        mx.abort('etc/ldpaths does not define ' + lib_env_name)
    except subprocess.CalledProcessError:
        mx.abort('error retrieving etc/ldpaths')

def setREnvironment(env=None):
    '''
    If R is run via mx, then the library path will not be set, whereas if it is
    run from 'bin/R' it will be, via etc/ldpaths.
    On Mac OS X El Capitan and beyond, this is moot as the variable is not
    passed down. It is TBD if we can avoid this on Linux.
    '''
    if not env:
        env = os.environ
    # This may have been set by a higher power
    if not 'R_HOME' in env:
        env['R_HOME'] = _fastr_suite.dir

    # Make sure that native code formats numbers consistently
    env['LC_NUMERIC'] = 'C'

    osname = platform.system()
    if osname != 'Darwin':
        lib_env = 'LD_LIBRARY_PATH'

        if lib_env in env:
            lib_value = env[lib_env]
        else:
            lib_value = _get_ldpaths(lib_env)

        env[lib_env] = lib_value

def get_default_jdk():
    '''
    Returns the (default) jdk under which to run.
    N.B. The jvmci jdk actually comes in three variants and the choice
    is controlled either by the DEFAULT_VM environment variable (recommended) or
    the --vm global option to mx.
    '''
    return mx_jvm().get_jvmci_jdk()

def run_r(args, command, parser=None, extraVmArgs=None, jdk=None, **kwargs):
    '''
    Common function for running either R, Rscript (or rrepl).
    args are a list of strings that came after 'command' on the command line
    '''
    parser = parser if parser is not None else ArgumentParser(prog='mx ' + command)
    parser.add_argument('--J', dest='extraVmArgsList', action='append', help='extra Java VM arguments', metavar='@<args>')
    parser.add_argument('--jdk', action='store', help='jdk to use')
    ns, rargs = parser.parse_known_args(args)

    if ns.extraVmArgsList:
        j_extraVmArgsList = mx.split_j_args(ns.extraVmArgsList)
        if extraVmArgs is None:
            extraVmArgs = []
        extraVmArgs += j_extraVmArgsList

    if not jdk and ns.jdk:
        jdk = mx.get_jdk(tag=ns.jdk)

    # special cases normally handled in shell script startup
    if command == 'r' and len(rargs) > 0:
        if rargs[0] == 'RHOME':
            print _fastr_suite.dir
            sys.exit(0)
        elif rargs[0] == 'CMD':
            print 'CMD not implemented via mx, use: bin/R CMD ...'
            sys.exit(1)

    return do_run_r(rargs, command, extraVmArgs=extraVmArgs, jdk=jdk, **kwargs)

def rshell(args):
    '''run R shell'''
    return run_r(args, 'r')

def rscript(args, parser=None, **kwargs):
    '''run Rscript'''
    return run_r(args, 'rscript', parser=parser, **kwargs)

def rrepl(args, nonZeroIsFatal=True, extraVmArgs=None):
    '''run R repl'''
    run_r(args, 'rrepl')

def process_bm_args(args):
    '''
    Analyze args for those specific to the rhome of this suite for benchmarks.
    Return a dict with keys:
      'suite': this suite
      'printname': print name for suite
      'args': non-rhome-specific args
      'rhome_args: rhome-specific-args
      'executor': function to execute a command using the rhome
    Other keys can be stored based on the analysis
    '''
    def getVmArgValue(key, args, i):
        if i < len(args) - 1:
            return args[i + 1]
        else:
            mx.abort('value expected after ' + key)

    non_rhome_args = []
    rhome_args = []
    rhome_args.append('-Xmx6g')
    # This turns off all visibility support
    rhome_args.append('-DR:+IgnoreVisibility')
    # Evidently these options are specific to Graal but do_run_r filters inappropriate options
    # if we are running under a different VM
#    rhome_args.append('-Dgraal.TruffleCompilationExceptionsAreFatal=true')
    rhome_args.append('-Dgraal.TraceTruffleCompilation=true')
    rhome_args += ['-da', '-dsa']
    error_args = []
    i = 0
    jdk = None
    while i < len(args):
        arg = args[i]
        if arg == '--J':
            argslist = mx.split_j_args([getVmArgValue(arg, args, i)])
            rhome_args += argslist
            i = i + 1
        elif arg == '--jdk':
            jdk = getVmArgValue(arg, args, i)
            i = i + 1
        elif args == '--print-internal-errors':
            # Non-interactively we don't want exceptions going to fastr_errors.log
            error_args = ['-DR:+PrintErrorStacktraces', '-DR:-PrintErrorStacktracesToFile']
        else:
            non_rhome_args.append(arg)
        i = i + 1
    return {'suite': _fastr_suite, 'printname': 'FastR',
            'args': non_rhome_args, 'rhome-args': rhome_args + error_args,
            'executor': _bm_runner,
            'jdk': jdk}

def _bm_runner(command, rhome_dict, out, err, nonZeroIsFatal=False):
    return do_run_r(command, 'R', nonZeroIsFatal=False, extraVmArgs=rhome_dict['rhome-args'], jdk=rhome_dict['jdk'], out=out, err=out)

def build(args):
    '''FastR build'''
    # workaround for Hotspot Mac OS X build problem
    osname = platform.system()
    if osname == 'Darwin':
        os.environ['COMPILER_WARNINGS_FATAL'] = 'false'
        os.environ['USE_CLANG'] = 'true'
        os.environ['LFLAGS'] = '-Xlinker -lstdc++'
    return mx_jvm().build(args)

def _fastr_gate_runner(args, tasks):
    # Until fixed, we call Checkstyle here and limit to primary
    with mx_gate.Task('Checkstyle check', tasks) as t:
        if t:
            if mx.checkstyle(['--primary']) != 0:
                t.abort('Checkstyle warnings were found')

    # FastR has custom copyright check
    with mx_gate.Task('Copyright check', tasks) as t:
        if t:
            if mx.checkcopyrights(['--primary']) != 0:
                t.abort('copyright errors')

    # build the native projects (GnuR/VM)
    with mx_gate.Task('BuildNative', tasks) as t:
        if t:
            build([])

    # check that the expected test output file is up to date
    with mx_gate.Task('UnitTests: ExpectedTestOutput file check', tasks) as t:
        if t:
            if junit(['--tests', _gate_noapps_unit_tests(), '--check-expected-output']) != 0:
                t.abort('unit tests expected output check failed')

    with mx_gate.Task('UnitTests', tasks) as t:
        if t:
            if junit(['--tests', _gate_noapps_unit_tests()]) != 0:
                t.abort('unit tests failed')

mx_gate.add_gate_runner(_fastr_suite, _fastr_gate_runner)

def gate(args):
    '''Run the R gate'''
    # exclude findbugs until compliant
    mx_gate.gate(args + ['-x', '-t', 'FindBugs,Checkheaders,Distribution Overlap Check,BuildJavaWithEcj'])

def original_gate(args):
    '''Run the R gate (without filtering gate tasks)'''
    mx_gate.gate(args)

def _test_srcdir():
    tp = 'com.oracle.truffle.r.test'
    return join(mx.project(tp).dir, 'src', tp.replace('.', sep))

def _junit_r_harness(args, vmArgs, jdk, junitArgs):
    # always pass the directory where the expected output file should reside
    runlistener_arg = 'expected=' + _test_srcdir()
    # there should not be any unparsed arguments at this stage
    if args.remainder:
        mx.abort('unexpected arguments: ' + str(args.remainder).strip('[]') + '; did you forget --tests')

    def add_arg_separator():
        # can't update in Python 2.7
        arg = runlistener_arg
        if len(arg) > 0:
            arg += ','
        return arg

    if args.gen_fastr_output:
        runlistener_arg = add_arg_separator()
        runlistener_arg += 'gen-fastr=' + args.gen_fastr_output

    if args.check_expected_output:
        args.gen_expected_output = True
        runlistener_arg = add_arg_separator()
        runlistener_arg += 'check-expected'

    if args.gen_expected_output:
        runlistener_arg = add_arg_separator()
        runlistener_arg += 'gen-expected'
        if args.keep_trailing_whitespace:
            runlistener_arg = add_arg_separator()
            runlistener_arg += 'keep-trailing-whitespace'
        if args.gen_expected_quiet:
            runlistener_arg = add_arg_separator()
            runlistener_arg += 'gen-expected-quiet'

    if args.gen_diff_output:
        runlistener_arg = add_arg_separator()
        runlistener_arg += 'gen-diff=' + args.gen_diff_output

#    if args.test_methods:
#        runlistener_arg = add_arg_separator()
#        runlistener_arg = 'test-methods=' + args.test_methods

    # use a custom junit.RunListener
    runlistener = 'com.oracle.truffle.r.test.TestBase$RunListener'
    if len(runlistener_arg) > 0:
        runlistener += ':' + runlistener_arg

    junitArgs += ['--runlistener', runlistener]

    # on some systems a large Java stack seems necessary
    vmArgs += ['-Xss12m']
    # no point in printing errors to file when running tests (that contain errors on purpose)
    vmArgs += ['-DR:-PrintErrorStacktracesToFile']

    vmArgs += _graal_options(nocompile=True)

    setREnvironment()
    vmArgs = _sanitize_vmArgs(jdk, vmArgs)

    return mx.run_java(vmArgs + junitArgs, nonZeroIsFatal=False, jdk=jdk)

def junit(args):
    '''run R Junit tests'''
    parser = ArgumentParser(prog='r junit')
    parser.add_argument('--gen-expected-output', action='store_true', help='generate/update expected test output file')
    parser.add_argument('--gen-expected-quiet', action='store_true', help='suppress output on new tests being added')
    parser.add_argument('--keep-trailing-whitespace', action='store_true', help='keep trailing whitespace in expected test output file')
    parser.add_argument('--check-expected-output', action='store_true', help='check but do not update expected test output file')
    parser.add_argument('--gen-fastr-output', action='store', metavar='<path>', help='generate FastR test output file')
    parser.add_argument('--gen-diff-output', action='store', metavar='<path>', help='generate difference test output file ')
    # parser.add_argument('--test-methods', action='store', help='pattern to match test methods in test classes')

    if os.environ.has_key('R_PROFILE_USER'):
        mx.abort('unset R_PROFILE_USER before running unit tests')

    return mx.junit(args, _junit_r_harness, parser=parser)

def junit_simple(args):
    return mx.command_function('junit')(['--tests', _simple_unit_tests()] + args)

def junit_noapps(args):
    return mx.command_function('junit')(['--tests', _gate_noapps_unit_tests()] + args)

def junit_default(args):
    return mx.command_function('junit')(['--tests', _all_unit_tests()] + args)

def junit_gate(args):
    return mx.command_function('junit')(['--tests', _gate_unit_tests()] + args)

def _test_package():
    return 'com.oracle.truffle.r.test'

def _test_subpackage(name):
    return '.'.join((_test_package(), name))

def _simple_unit_tests():
    return ','.join(map(_test_subpackage, ['library.base', 'library.stats', 'library.utils', 'library.fastr', 'builtins', 'functions', 'tck', 'parser', 'S4']))

def _package_unit_tests():
    return ','.join(map(_test_subpackage, ['rffi', 'rpackages']))

def _nodes_unit_tests():
    return 'com.oracle.truffle.r.nodes.test'

def _apps_unit_tests():
    return _test_subpackage('apps')

def _gate_noapps_unit_tests():
    return ','.join([_simple_unit_tests(), _nodes_unit_tests(), _package_unit_tests()])

def _gate_unit_tests():
    return ','.join([_gate_noapps_unit_tests(), _apps_unit_tests()])

def _all_unit_tests():
    return _gate_unit_tests()

def testgen(args):
    '''generate the expected output for unit tests, and All/Failing test classes'''
    parser = ArgumentParser(prog='r testgen')
    parser.add_argument('--tests', action='store', default=_all_unit_tests(), help='pattern to match test classes')
    args = parser.parse_args(args)
    # check we are in the home directory
    if os.getcwd() != _fastr_suite.dir:
        mx.abort('must run rtestgen from FastR home directory')

    def need_version_check():
        vardef = os.environ.has_key('FASTR_TESTGEN_GNUR')
        varval = os.environ['FASTR_TESTGEN_GNUR'] if vardef else None
        version_check = not vardef or varval != 'internal'
        if version_check:
            if vardef and varval != 'internal':
                rpath = join(varval, 'bin', 'R')
            else:
                rpath = 'R'
        else:
            rpath = None
        return version_check, rpath

    version_check, rpath = need_version_check()
    if version_check:
        # check the version of GnuR against FastR
        try:
            fastr_version = subprocess.check_output([mx.get_jdk().java, '-cp', mx.classpath('com.oracle.truffle.r.runtime'), 'com.oracle.truffle.r.runtime.RVersionNumber'])
            gnur_version = subprocess.check_output([rpath, '--version'])
            if not gnur_version.startswith(fastr_version):
                mx.abort('R version is incompatible with FastR, please update to ' + fastr_version)
        except subprocess.CalledProcessError:
            mx.abort('RVersionNumber.main failed')

    # clean the test project to invoke the test analyzer AP
    testOnly = ['--projects', 'com.oracle.truffle.r.test']
    mx.clean(['--no-dist', ] + testOnly)
    mx.build(testOnly)
    # now just invoke junit with the appropriate options
    mx.log("generating expected output for packages: ")
    for pkg in args.tests.split(','):
        mx.log("    " + str(pkg))
    junit(['--tests', args.tests, '--gen-expected-output', '--gen-expected-quiet'])

def unittest(args):
    print "use 'junit --tests testclasses' or 'junitsimple' to run FastR unit tests"

def rbcheck(args):
    '''Checks FastR builtins against GnuR

    gnur-only:    GnuR builtins not implemented in FastR (i.e. TODO list).
    fastr-only:   FastR builtins not implemented in GnuR
    both-diff:    implemented in both GnuR and FastR, but with difference
                  in signature (e.g. visibility)
    both:         implemented in both GnuR and FastR with matching signature

    If the option --filter is not given, shows all groups.
    Multiple groups can be combined: e.g. "--filter gnur-only,fastr-only"'''
    cp = mx.classpath('com.oracle.truffle.r.test')
    args.append("--suite-path")
    args.append(mx.primary_suite().dir)
    mx.run_java(['-cp', cp, 'com.oracle.truffle.r.test.tools.RBuiltinCheck'] + args)

def rcmplib(args):
    '''compare FastR library R sources against GnuR'''
    parser = ArgumentParser(prog='mx rcmplib')
    parser.add_argument('--gnurhome', action='store', help='path to GnuR sources', required=True)
    parser.add_argument('--package', action='store', help='package to check', default="base")
    parser.add_argument('--paths', action='store_true', help='print full paths of files that differ')
    parser.add_argument('--diffapp', action='store', help='diff application', default="diff")
    args = parser.parse_args(args)
    cmpArgs = []
    cmpArgs.append("--gnurhome")
    cmpArgs.append(args.gnurhome)
    cmpArgs.append("--package")
    cmpArgs.append(args.package)
    if args.paths:
        cmpArgs.append("--paths")
        cmpArgs.append("--diffapp")
        cmpArgs.append(args.diffapp)

    cp = mx.classpath([pcp.name for pcp in mx.projects_opt_limit_to_suites()])
    mx.run_java(['-cp', cp, 'com.oracle.truffle.r.test.tools.cmpr.CompareLibR'] + cmpArgs)

def _cran_test_project():
    return 'com.oracle.truffle.r.test.cran'

def _cran_test_project_dir():
    return mx.project(_cran_test_project()).dir

def installpkgs(args):
    _installpkgs(args)

def _installpkgs_script():
    cran_test = _cran_test_project_dir()
    return join(cran_test, 'r', 'install.cran.packages.R')

def _installpkgs(args, **kwargs):
    script = _installpkgs_script()
    return rscript([script] + args, **kwargs)

_commands = {
    'r' : [rshell, '[options]'],
    'R' : [rshell, '[options]'],
    'rscript' : [rscript, '[options]'],
    'Rscript' : [rscript, '[options]'],
    'rtestgen' : [testgen, ''],
    'originalgate' : [original_gate, '[options]'],
    'build' : [build, ''],
    'gate' : [gate, ''],
    'junit' : [junit, ['options']],
    'junitsimple' : [junit_simple, ['options']],
    'junitdefault' : [junit_default, ['options']],
    'junitgate' : [junit_gate, ['options']],
    'junitnoapps' : [junit_noapps, ['options']],
    'unittest' : [unittest, ['options']],
    'rbcheck' : [rbcheck, '--filter [gnur-only,fastr-only,both,both-diff]'],
    'rcmplib' : [rcmplib, ['options']],
    'pkgtest' : [mx_fastr_pkgs.pkgtest, ['options']],
    'rrepl' : [rrepl, '[options]'],
    'installpkgs' : [installpkgs, '[options]'],
    'installcran' : [installpkgs, '[options]'],
    }

mx.update_commands(_fastr_suite, _commands)
