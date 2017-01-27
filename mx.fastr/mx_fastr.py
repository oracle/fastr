#
# Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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
import platform, subprocess, sys, shlex
from os.path import join, sep
from argparse import ArgumentParser
import mx
import mx_gate
import mx_fastr_pkgs
import mx_fastr_compile
import mx_fastr_dists
import mx_fastr_junit
from mx_fastr_dists import FastRNativeProject, FastRTestNativeProject, FastRReleaseProject, FastRNativeRecommendedProject #pylint: disable=unused-import
import mx_copylib
import mx_fastr_mkgramrd

import os

'''
This is the launchpad for all the functions available for building/running/testing/analyzing
FastR. FastR can run with or without the Graal compiler enabled. As a convenience if the
graal-core suite is detected then the use of the Graal compiler is enabled without any
additional command line options being required to the mx command, i.e. it is as if --jdk jvmci
was passed as an mx global option.
'''

_fastr_suite = mx.suite('fastr')
'''
If this is None, then we run under the standard VM in interpreted mode only.
'''
_mx_graal = mx.suite("graal-core", fatalIfMissing=False)
_mx_sulong = mx.suite("sulong", fatalIfMissing=False)

_r_command_package = 'com.oracle.truffle.r.engine'
_repl_command = 'com.oracle.truffle.tools.debug.shell.client.SimpleREPLClient'
_command_class_dict = {'r': _r_command_package + ".shell.RCommand",
                       'rscript': _r_command_package + ".shell.RscriptCommand",
                        'rrepl': _repl_command,
                        'rembed': _r_command_package + ".shell.REmbedded",
                    }
# benchmarking support
def r_path():
    return join(_fastr_suite.dir, 'bin', 'R')

def r_version():
    # Could figure this out dynamically
    return 'R-3.3.2'

def get_default_jdk():
    if _mx_graal:
        tag = 'jvmci'
    else:
        tag = None
    return mx.get_jdk(tag=tag)

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

    dists = ['FASTR']
    if _mx_sulong:
        dists.append('SULONG')

    vmArgs = mx.get_runtime_jvm_args(dists, jdk=jdk)

    vmArgs += set_graal_options()
    vmArgs += _sulong_options()

    if extraVmArgs is None or not '-da' in extraVmArgs:
        # unless explicitly disabled we enable assertion checking
        vmArgs += ['-ea', '-esa']

    if extraVmArgs:
        vmArgs += extraVmArgs

    vmArgs = _sanitize_vmArgs(jdk, vmArgs)
    if command:
        vmArgs.append(_command_class_dict[command.lower()])
    return mx.run_java(vmArgs + args, jdk=jdk, **kwargs)

def r_classpath(args):
    print mx.classpath('FASTR', jdk=mx.get_jdk())

def _sanitize_vmArgs(jdk, vmArgs):
    '''
    jdk dependent analysis of vmArgs to remove those that are not appropriate for the
    chosen jdk. It is easier to allow clients to set anything they want and filter them
    out here.
    '''
    jvmci_jdk = jdk.tag is not None and 'jvmci' in jdk.tag
    jvmci_disabled = '-XX:-EnableJVMCI' in vmArgs

    xargs = []
    i = 0
    while i < len(vmArgs):
        vmArg = vmArgs[i]
        if vmArg != '-XX:-EnableJVMCI':
            if vmArg.startswith("-") and '-Dgraal' in vmArg or 'JVMCI' in vmArg:
                if not jvmci_jdk or jvmci_disabled:
                    i = i + 1
                    continue
        xargs.append(vmArg)
        i = i + 1
    return xargs

def set_graal_options():
    '''
    If Graal is enabled, set some options specific to FastR
    '''
    if _mx_graal:
        result = ['-Dgraal.InliningDepthError=500', '-Dgraal.EscapeAnalysisIterations=3', '-XX:JVMCINMethodSizeLimit=1000000']
        return result
    else:
        return []

def _sulong_options():
    if _mx_sulong:
        return ['-Dfastr.ffi.factory.class=com.oracle.truffle.r.engine.interop.ffi.llvm.TruffleLLVM_RFFIFactory',
                '-XX:-UseJVMCIClassLoader']
    else:
        return []

def _get_ldpaths(env, lib_env_name):
    ldpaths = os.path.join(env['R_HOME'], 'etc', 'ldpaths')
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
            lib_value = _get_ldpaths(env, lib_env)

        env[lib_env] = lib_value

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
        j_extraVmArgsList = split_j_args(ns.extraVmArgsList)
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

def split_j_args(extraVmArgsList):
    extraVmArgs = []
    if extraVmArgsList:
        for e in extraVmArgsList:
            extraVmArgs += [x for x in shlex.split(e.lstrip('@'))]
    return extraVmArgs

def rshell(args):
    '''run R shell'''
    return run_r(args, 'r')

def rscript(args, parser=None, **kwargs):
    '''run Rscript'''
    return run_r(args, 'rscript', parser=parser, **kwargs)

def rrepl(args, nonZeroIsFatal=True, extraVmArgs=None):
    '''run R repl'''
    run_r(args, 'rrepl')

def rembed(args, nonZeroIsFatal=True, extraVmArgs=None):
    run_r(args, 'rembed')

def _fastr_gate_runner(args, tasks):
    '''
    The specific additional gates tasks provided by FastR:
    1. Copyright check
    2. Check that ExpectedTestOutput file is in sync with unit tests
    3. Unit tests
    '''
    # FastR has custom copyright check
    with mx_gate.Task('Copyright check', tasks) as t:
        if t:
            if mx.checkcopyrights(['--primary']) != 0:
                t.abort('copyright errors')

    # check that the expected test output file is up to date
    with mx_gate.Task('UnitTests: ExpectedTestOutput file check', tasks) as t:
        if t:
            if junit(['--tests', _gate_unit_tests(), '--check-expected-output']) != 0:
                t.abort('unit tests expected output check failed')

    with mx_gate.Task('UnitTests: no specials', tasks) as t:
        if t:
            if junit(['--J', '@-DR:-UseSpecials', '--tests', _gate_noapps_unit_tests()]) != 0:
                t.abort('unit tests failed')

    with mx_gate.Task('UnitTests: with specials', tasks) as t:
        if t:
            if junit(['--tests', _gate_noapps_unit_tests()]) != 0:
                t.abort('unit tests failed')

    with mx_gate.Task('UnitTests: apps', tasks) as t:
        if t:
            if junit(['--tests', _apps_unit_tests()]) != 0:
                t.abort('unit tests failed')

mx_gate.add_gate_runner(_fastr_suite, _fastr_gate_runner)

def rgate(args):
    '''
    Run 'mx.gate' with given args (used in CI system).
    N.B. This will fail if run without certain exclusions; use the local
    'gate' command for that.
    '''
    mx_gate.gate(args)

def gate(args):
    '''Run 'mx.gate' with some standard tasks excluded as they currently fail'''
    mx_gate.gate(args + ['-x', '-t', 'FindBugs,Checkheaders,Distribution Overlap Check,BuildJavaWithEcj'])

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

    if args.trace_tests:
        runlistener_arg = add_arg_separator()
        runlistener_arg += 'trace-tests'

#    if args.test_methods:
#        runlistener_arg = add_arg_separator()
#        runlistener_arg = 'test-methods=' + args.test_methods

    runlistener_arg = add_arg_separator()
    runlistener_arg += 'test-project-output-dir=' + mx.project('com.oracle.truffle.r.test').output_dir()

    # use a custom junit.RunListener
    runlistener = 'com.oracle.truffle.r.test.TestBase$RunListener'
    if len(runlistener_arg) > 0:
        runlistener += ':' + runlistener_arg

    junitArgs += ['--runlistener', runlistener]

    # on some systems a large Java stack seems necessary
    vmArgs += ['-Xss12m']
    # no point in printing errors to file when running tests (that contain errors on purpose)
    vmArgs += ['-DR:-PrintErrorStacktracesToFile']
    vmArgs += _sulong_options()

    setREnvironment()

    return mx.run_java(vmArgs + junitArgs, nonZeroIsFatal=False, jdk=jdk)

def junit(args):
    '''run R Junit tests'''
    parser = ArgumentParser(prog='r junit')
    parser.add_argument('--gen-expected-output', action='store_true', help='generate/update expected test output file')
    parser.add_argument('--gen-expected-quiet', action='store_true', help='suppress output on new tests being added')
    parser.add_argument('--keep-trailing-whitespace', action='store_true', help='keep trailing whitespace in expected test output file')
    parser.add_argument('--check-expected-output', action='store_true', help='check but do not update expected test output file')
    parser.add_argument('--gen-fastr-output', action='store', metavar='<path>', help='generate FastR test output file in given directory (e.g. ".")')
    parser.add_argument('--gen-diff-output', action='store', metavar='<path>', help='generate difference test output file in given directory (e.g. ".")')
    parser.add_argument('--trace-tests', action='store_true', help='trace the actual @Test methods as they are executed')
    # parser.add_argument('--test-methods', action='store', help='pattern to match test methods in test classes')

    if os.environ.has_key('R_PROFILE_USER'):
        mx.abort('unset R_PROFILE_USER before running unit tests')
    _unset_conflicting_envs()
    return mx_fastr_junit.junit(args, _junit_r_harness, parser=parser, jdk_default=get_default_jdk())

def junit_simple(args):
    return mx.command_function('junit')(['--tests', _simple_unit_tests()] + args)

def junit_noapps(args):
    return mx.command_function('junit')(['--tests', _gate_noapps_unit_tests()] + args)

def junit_nopkgs(args):
    return mx.command_function('junit')(['--tests', ','.join([_simple_unit_tests(), _nodes_unit_tests()])] + args)

def junit_default(args):
    return mx.command_function('junit')(['--tests', _all_unit_tests()] + args)

def junit_gate(args):
    return mx.command_function('junit')(['--tests', _gate_unit_tests()] + args)

def _test_package():
    return 'com.oracle.truffle.r.test'

def _test_subpackage(name):
    return '.'.join((_test_package(), name))

def _simple_unit_tests():
    return ','.join(map(_test_subpackage, ['library.base', 'library.stats', 'library.utils', 'library.fastr', 'builtins', 'functions', 'tck', 'parser', 'S4', 'rng', 'runtime.data']))

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
        version_check = vardef and varval != 'internal'
        if version_check:
            rpath = join(varval, 'bin', 'R')
        else:
            rpath = None
        return version_check, rpath

    version_check, rpath = need_version_check()
    if version_check:
        # check the version of GnuR against FastR
        try:
            fastr_version = subprocess.check_output([mx.get_jdk().java, mx.get_runtime_jvm_args('com.oracle.truffle.r.runtime'), 'com.oracle.truffle.r.runtime.RVersionNumber'])
            gnur_version = subprocess.check_output([rpath, '--version'])
            if not gnur_version.startswith(fastr_version):
                mx.abort('R version is incompatible with FastR, please update to ' + fastr_version)
        except subprocess.CalledProcessError:
            mx.abort('RVersionNumber.main failed')

    # now just invoke junit with the appropriate options
    mx.log("generating expected output for packages: ")
    for pkg in args.tests.split(','):
        mx.log("    " + str(pkg))
    os.environ["TZDIR"] = "/usr/share/zoneinfo/"
    _unset_conflicting_envs()
    junit(['--tests', args.tests, '--gen-expected-output', '--gen-expected-quiet'])

def _unset_conflicting_envs():
    # this can interfere with the recommended packages
    if os.environ.has_key('R_LIBS_USER'):
        del os.environ['R_LIBS_USER']
    # the default must be vi for unit tests
    if os.environ.has_key('EDITOR'):
        del os.environ['EDITOR']

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
    vmArgs = mx.get_runtime_jvm_args('com.oracle.truffle.r.test')
    args.append("--suite-path")
    args.append(mx.primary_suite().dir)
    vmArgs += ['com.oracle.truffle.r.test.tools.RBuiltinCheck']
    mx.run_java(vmArgs + args)

def rbdiag(args):
    '''Diagnoses FastR builtins

	-v		Verbose output including the list of unimplemented specializations
	-n		Ignore RNull as an argument type
	-m		Ignore RMissing as an argument type
    --mnonly		Uses the RMissing and RNull values as the only samples for the chimney-sweeping
    --noSelfTest	Does not perform the pipeline self-test using the generated samples as the intro to each chimney-sweeping. It has no effect when --mnonly is specified as the self-test is never performed in that case.
    --sweep		Performs the 'chimney-sweeping'. The sample combination selection method is determined automatically.
    --sweep=lite	Performs the 'chimney-sweeping'. The diagonal sample selection method is used.
    --sweep=total	Performs the 'chimney-sweeping'. The total sample selection method is used.
    --matchLevel=same	Outputs produced by FastR and GnuR must be same (default)
    --matchLevel=error	Outputs are considered matching if none or both outputs contain an error
    --maxSweeps=N		Sets the maximum number of sweeps
    --outMaxLev=N		Sets the maximum output detail level for report messages. Use 0 for the basic messages only.

	If no builtin is specified, all registered builtins are diagnosed.
	An external builtin is specified by the fully qualified name of its node class.

	Examples:

    	mx rbdiag
		mx rbdiag colSums colMeans -v
		mx rbdiag scan -m -n
    	mx rbdiag colSums --sweep
    	mx rbdiag com.oracle.truffle.r.library.stats.Rnorm
    '''
    vmArgs = mx.get_runtime_jvm_args('com.oracle.truffle.r.nodes.test')

    setREnvironment()
    os.environ["FASTR_TESTGEN_GNUR"] = "internal"
    # this should work for Linux and Mac:
    os.environ["TZDIR"] = "/usr/share/zoneinfo/"

    vmArgs += ['com.oracle.truffle.r.nodes.test.RBuiltinDiagnostics']
    mx.run_java(vmArgs + args)

def _gnur_path():
    np = mx.project('com.oracle.truffle.r.native')
    return join(np.dir, 'gnur', r_version(), 'bin')

def gnu_r(args):
    '''
    run the internally built GNU R executable'
    '''
    cmd = [join(_gnur_path(), 'R')] + args
    return mx.run(cmd, nonZeroIsFatal=False)

def gnu_rscript(args, env=None):
    '''
    run the internally built GNU Rscript executable
    env arg is used by pkgtest
    '''
    cmd = [join(_gnur_path(), 'Rscript')] + args
    return mx.run(cmd, nonZeroIsFatal=False, env=env)

def nativebuild(args):
    '''
    force the build of part or all of the native project
    '''
    parser = ArgumentParser(prog='nativebuild')
    parser.add_argument('--all', action='store_true', help='clean and build everything, else just ffi')
    args = parser.parse_args(args)
    nativedir = mx.project('com.oracle.truffle.r.native').dir
    if args.all:
        return subprocess.call(['make clean && make'], shell=True, cwd=nativedir)
    else:
        ffidir = join(nativedir, 'fficall')
        jni_done = join(ffidir, 'jni.done')
        jniboot_done = join(ffidir, 'jniboot.done')
        if os.path.exists(jni_done):
            os.remove(jni_done)
        if os.path.exists(jniboot_done):
            os.remove(jniboot_done)
        return mx.build(['--no-java'])

def mx_post_parse_cmd_line(opts):
    mx_fastr_dists.mx_post_parse_cmd_line(opts)

_commands = {
    'r' : [rshell, '[options]'],
    'R' : [rshell, '[options]'],
    'rscript' : [rscript, '[options]'],
    'Rscript' : [rscript, '[options]'],
    'rtestgen' : [testgen, ''],
    'rgate' : [rgate, ''],
    'gate' : [gate, ''],
    'junit' : [junit, ['options']],
    'junitsimple' : [junit_simple, ['options']],
    'junitdefault' : [junit_default, ['options']],
    'junitgate' : [junit_gate, ['options']],
    'junitnoapps' : [junit_noapps, ['options']],
    'junitnopkgs' : [junit_nopkgs, ['options']],
    'unittest' : [unittest, ['options']],
    'rbcheck' : [rbcheck, '--filter [gnur-only,fastr-only,both,both-diff]'],
    'rbdiag' : [rbdiag, '(builtin)* [-v] [-n] [-m] [--sweep | --sweep=lite | --sweep=total] [--mnonly] [--noSelfTest] [--matchLevel=same | --matchLevel=error] [--maxSweeps=N] [--outMaxLev=N]'],
    'rrepl' : [rrepl, '[options]'],
    'rembed' : [rembed, '[options]'],
    'r-cp' : [r_classpath, '[options]'],
    'pkgtest' : [mx_fastr_pkgs.pkgtest, ['options']],
    'installpkgs' : [mx_fastr_pkgs.installpkgs, '[options]'],
    'mkgramrd': [mx_fastr_mkgramrd.mkgramrd, '[options]'],
    'rcopylib' : [mx_copylib.copylib, '[]'],
    'rupdatelib' : [mx_copylib.updatelib, '[]'],
    'gnu-r' : [gnu_r, '[]'],
    'gnu-rscript' : [gnu_rscript, '[]'],
    'nativebuild' : [nativebuild, '[]'],
    }

_commands.update(mx_fastr_compile._commands)
mx.update_commands(_fastr_suite, _commands)
