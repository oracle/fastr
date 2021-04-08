#
# Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 3 only, as
# published by the Free Software Foundation.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 3 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 3 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
# or visit www.oracle.com if you need additional information or have any
# questions.
#
import glob
import platform, subprocess, sys, shlex
from os.path import join, sep
from argparse import ArgumentParser
import tarfile
import mx
import mx_gate
import mx_fastr_dists
import mx_subst
from mx_fastr_dists import FastRReleaseProject #pylint: disable=unused-import
import mx_fastr_edinclude
import mx_unittest

import os
import shutil

'''
This is the launchpad for all the functions available for building/running/testing/analyzing
FastR. FastR can run with or without the Graal compiler enabled. As a convenience if the
compiler suite is detected then the use of the Graal compiler is enabled without any
additional command line options being required to the mx command, i.e. it is as if --jdk jvmci
was passed as an mx global option.
'''

_fastr_suite = mx.suite('fastr')

_command_class_dict = {'r': ["com.oracle.truffle.r.launcher.RMain", "R"],
                       'rscript': ["com.oracle.truffle.r.launcher.RMain", "Rscript"],
                        'rrepl': ["com.oracle.truffle.tools.debug.shell.client.SimpleREPLClient"],
                        'rembed': ["com.oracle.truffle.r.engine.shell.REmbedded"],
                    }


# benchmarking support
def r_path():
    return join(_fastr_suite.dir, 'bin', 'R')

def r_version():
    # Could figure this out dynamically?
    return 'R-4.0.3'

def gnur_path():
    if 'GNUR_HOME_BINARY' in os.environ:
        return os.environ['GNUR_HOME_BINARY']
    return os.path.join(_fastr_suite.dir, 'libdownloads', r_version())

def get_default_jdk():
    if mx.suite("compiler", fatalIfMissing=False):
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
    if mx.suite("sulong", fatalIfMissing=False):
        dists.append('SULONG_NATIVE')

    vmArgs = mx.get_runtime_jvm_args(dists, jdk=jdk)

    vmArgs += set_graal_options()
    vmArgs += _sulong_options()
    args = _sulong_args() + args

    if not "FASTR_NO_ASSERTS" in os.environ and (extraVmArgs is None or not '-da' in extraVmArgs):
        # unless explicitly disabled we enable assertion checking
        vmArgs += ['-ea', '-esa']

    if extraVmArgs:
        vmArgs += extraVmArgs

    vmArgs = _sanitize_vmArgs(jdk, vmArgs)
    if command:
        vmArgs.extend(_command_class_dict[command.lower()])
    return mx.run_java(vmArgs + args, jdk=jdk, **kwargs)

def run_grid_server(args, **kwargs):
    vmArgs = mx.get_runtime_jvm_args(['GRID_DEVICE_REMOTE_SERVER'], jdk=get_default_jdk())
    vmArgs.append('com.oracle.truffle.r.library.fastrGrid.device.remote.server.RemoteDeviceServer')
    return mx.run_java(vmArgs + args, jdk=get_default_jdk(), **kwargs)

def r_classpath(args):
    print(mx.classpath('FASTR', jdk=mx.get_jdk()) + ":" + mx.classpath('SULONG_NATIVE', jdk=mx.get_jdk()))  # pylint: disable=superfluous-parens

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
    if mx.suite("compiler", fatalIfMissing=False):
        result = ['-Dgraal.InliningDepthError=500', '-Dgraal.EscapeAnalysisIterations=3', '-XX:JVMCINMethodSizeLimit=1000000']
        return result
    else:
        return []

def _sulong_args():
    mx_sulong = mx.suite("sulong", fatalIfMissing=False)
    if mx_sulong:
        return ['--experimental-options']
    else:
        return []

def _sulong_options():
    mx_sulong = mx.suite("sulong", fatalIfMissing=False)
    if mx_sulong:
        return ['-Dpolyglot.llvm.libraryPath=' + mx_sulong.dir + '/mxbuild/sulong-libs']
    else:
        return []

def _get_ldpaths(env, lib_env_name):
    ldpaths = os.path.join(env['R_HOME'], 'etc', 'ldpaths')
    command = ['bash', '-c', 'source ' + ldpaths + ' && env']

    try:
        proc = subprocess.Popen(command, stdout=subprocess.PIPE, universal_newlines=True)
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

def setUnitTestEnvironment(args):
    env = os.environ
    rOptions = []
    for arg in args:
        if arg.startswith("--R."):
            ss = arg.split("=")
            env['FASTR_OPTION_' + ss[0][4:]] = ss[1]
            rOptions.append(arg)
    for rOption in rOptions:
        args.remove(rOption)

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
            print(_fastr_suite.dir)  # pylint: disable=superfluous-parens
            sys.exit(0)
        elif rargs[0] == 'CMD':
            print('CMD not implemented via mx, use: bin/R CMD ...')  # pylint: disable=superfluous-parens
            sys.exit(1)

    return do_run_r(rargs, command, extraVmArgs=extraVmArgs, jdk=jdk, **kwargs)

def split_j_args(extraVmArgsList):
    extraVmArgs = []
    if extraVmArgsList:
        for e in extraVmArgsList:
            extraVmArgs += shlex.split(e.lstrip('@'))
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
    '''
    Runs pure Java program that simulates the embedding scenario doing the same up-calls as embedded would call.
    '''
    run_r(args, 'rembed')

def rembedtest(args, nonZeroIsFatal=False, extraVmArgs=None):
    '''
    Runs simple R embedding API tests located in com.oracle.truffle.r.test.native/embedded.
    The tests should be compiled by mx build before they can be run.
    Each test (native application) is run and its output compared to the expected output
    file located next to the source file.
    '''
    env = os.environ.copy()
    env['R_HOME'] = _fastr_suite.dir
    so_suffix = '.dylib' if platform.system().lower() == 'darwin' else '.so'
    env['NFI_LIB'] = join(mx.distribution('TRUFFLE_NFI_NATIVE').get_output(), 'bin', 'libtrufflenfi' + so_suffix)
    tests_script = join(_fastr_suite.dir, 'com.oracle.truffle.r.test.native/embedded/test.sh')
    return mx.run([tests_script], env=env, nonZeroIsFatal=nonZeroIsFatal)

class FastRGateTags:
    unit_tests = 'unit_tests'
    very_slow_asserts = 'very_slow_asserts'
    basic_tests = 'basic_tests'
    internal_pkgs_test = 'internal_pkgs_test' # runs pkgtest on internal packages in com.oracle.truffle.r.test.native/packages
    # cran_pkgs_testX runs pkgtest on selected CRAN packages listed in file com.oracle.truffle.r.test.packages/gatedX
    # additional tag cran_pkgs_test_check_last runs check that com.oracle.truffle.r.test.packages/gated{X+1} doesn't exist

    llvm = 'llvm'
    no_specials = 'no_specials'
    no_dsl_cache = 'no_dsl_cache'

    cran_pkgs_test = 'cran_pkgs_test'
    cran_pkgs_test_check_last = 'cran_pkgs_test_check_last'

    recommended_load = 'recommended_load'

    gc_torture1 = 'gc_torture1'
    gc_torture3 = 'gc_torture3'

def _fastr_gate_runner(args, tasks):
    with mx_gate.Task('Setup no specials', tasks, tags=[FastRGateTags.no_specials]) as t:
        if t:
            os.environ['FASTR_OPTION_UseSpecials'] = 'false'

    with mx_gate.Task('Setup no dsl cache', tasks, tags=[FastRGateTags.no_dsl_cache]) as t:
        if t:
            os.environ['FASTR_OPTION_DSLCacheSizeFactor'] = '0'

    with mx_gate.Task('SetupLLVM', tasks, tags=[FastRGateTags.llvm]) as t:
        if t:
            os.environ['FASTR_RFFI'] = 'llvm'

    with mx_gate.Task('GCTorture1', tasks, tags=[FastRGateTags.gc_torture1]) as t:
        if t:
            os.environ['FASTR_GCTORTURE'] = '1'

    with mx_gate.Task('GCTorture3', tasks, tags=[FastRGateTags.gc_torture3]) as t:
        if t:
            os.environ['FASTR_GCTORTURE'] = '3'

    with mx_gate.Task('VerySlowAsserts', tasks, tags=[FastRGateTags.very_slow_asserts]) as t:
        if t:
            os.environ['FASTR_TEST_VERY_SLOW_ASSERTS'] = 'true'

    '''
    The specific additional gates tasks provided by FastR.
    '''
    with mx_gate.Task('ExtSoftVersions', tasks, tags=[mx_gate.Tags.always]) as t:
        if t:
            new_env = os.environ.copy()
            new_env['R_DEFAULT_PACKAGES'] = 'base'
            run_r(['-q', '-e', 'extSoftVersion()'], 'R', env=new_env)

    with mx_gate.Task('LibsInfo', tasks, tags=[mx_gate.Tags.always]) as t:
        if t:
            mx.log("Libraries captured in FASTR_HOME/lib:")
            lib_dir = os.path.join(_fastr_suite.dir, 'lib')
            ldd = ['otool', '-L'] if platform.system() == 'Darwin' else ['ldd']
            for f in os.listdir(lib_dir):
                full_path = os.path.join(lib_dir, f)
                mx.run(['file', full_path], nonZeroIsFatal=False)
                mx.log('---\nobjdump:')
                mx.run(['objdump', '-s', '--section', '.comment', full_path], nonZeroIsFatal=False)
                mx.log('---\nlinking info:')
                mx.run(ldd + [full_path], nonZeroIsFatal=False)
                mx.log('---------')

    # ---------------------------------
    # Style checks:

    # FastR has custom copyright check
    with mx_gate.Task('Copyright check', tasks, tags=[mx_gate.Tags.style]) as t:
        if t:
            if mx.checkcopyrights(['--primary']) != 0:
                t.abort('copyright errors')

    # check that the expected test output file is up to date
    with mx_gate.Task('UnitTests: ExpectedTestOutput file check', tasks, tags=[mx_gate.Tags.style]) as t:
        if t:
            mx_unittest.unittest(['-Dfastr.test.gen.expected=' + _test_srcdir(), '-Dfastr.test.check.expected=true'] + _gate_unit_tests())

    # ----------------------------------
    # Basic tests:

    with mx_gate.Task('UnitTests', tasks, tags=[FastRGateTags.basic_tests, FastRGateTags.unit_tests]) as t:
        if t:
            mx_unittest.unittest(_gate_noapps_unit_tests())

    with mx_gate.Task('Rembedded', tasks, tags=[FastRGateTags.basic_tests]) as t:
        if t:
            if rembedtest([]) != 0:
                t.abort("Rembedded tests failed")

    # ----------------------------------
    # Package tests:

    with mx_gate.Task('Recommended load test', tasks, tags=[FastRGateTags.recommended_load]) as t:
        if t:
            # Note: this is a convenience mx gate job for testing the loading of recommended packages
            # We also test the loading of recommended pkgs in the "graalvm-tests"
            if not os.path.exists(os.path.join(_fastr_suite.dir, 'library', 'spatial')):
                mx.abort('Recommended packages seem to be not installed in FastR. Did you forget to build with FASTR_RELEASE=true?')
            pkgs = ['codetools', 'MASS', 'boot', 'class', 'cluster', 'lattice', 'nnet', 'spatial', 'Matrix', 'KernSmooth', 'foreign', 'nlme', 'rpart', 'survival']
            # Creates code that looks like: require(codetools) && require(MASS) && ...
            require_stmts = ' && '.join(['require(' + pkg + ')' for pkg in pkgs])
            test_load = 'if (!(' + require_stmts + ')) q(status=1) else q(status=42)'
            if run_r(['--vanilla', '-e', test_load], 'R', nonZeroIsFatal=False) != 42:
                mx.abort("Loading of recommended packages failed")

    with mx_gate.Task('Internal pkg test', tasks, tags=[FastRGateTags.internal_pkgs_test]) as t:
        if t:
            internal_pkg_tests()

    # CRAN packages are listed in files com.oracle.truffle.r.test.packages/gated0, gated1, ...
    # We loop over all such files and crete gate task for each of them
    # See also documentation in FastRGateTags.cran_pkgs_tests
    for i in range(1, 1000):
        list_file = os.path.join(_fastr_suite.dir, 'com.oracle.truffle.r.test.packages/gated' + str(i))
        if not os.path.exists(list_file):
            break
        is_file_empty = False
        with open(list_file, 'r') as f:
            if len(f.read().strip()) == 0:
                is_file_empty = True
        with mx_gate.Task('CRAN pkg test: ' + str(i), tasks, tags=[FastRGateTags.cran_pkgs_test + str(i)]) as t:
            if t:
                check_last = False if mx_gate.Task.tags is None else FastRGateTags.cran_pkgs_test_check_last in mx_gate.Task.tags # pylint: disable=unsupported-membership-test
                if check_last:
                    next_file = os.path.join(_fastr_suite.dir, 'com.oracle.truffle.r.test.packages/gated' + str(i + 1))
                    if os.path.exists(next_file):
                        mx.abort("File %s exists, but the gate thinks that %s is the last file. Did you forget to update the gate configuration?" % (next_file, list_file))
                if not is_file_empty:
                    cran_pkg_tests(list_file)
                else:
                    mx.warn('File %s is empty, skipping cran_pkg_test' % list_file)

def common_pkg_tests_args(graalvm_home):
    gvm_home_arg = [] if graalvm_home is None else ['--graalvm-home', graalvm_home]
    # SNAPSHOT is transformed by the pkgtest script, where it also checks for FASTR_MRAN_MIRROR env. variable
    # FASTR cannot be transformed by the pkgtest script, because it may not know FastR repo home, just graalvm_home
    fastr_pkgs_repo = os.path.join(_fastr_suite.dir, 'com.oracle.truffle.r.test.native/packages', 'repo')
    return gvm_home_arg + ["--gnur-home", gnur_path(), "--fastr-home", _fastr_suite.dir, "--repos", "SNAPSHOT,FASTR=file://" + fastr_pkgs_repo]

def cran_pkg_tests(list_file, graalvm_home=None):
    cache_args = []
    cache = os.environ.get('FASTR_PKGS_CACHE_OPT')
    if cache is None:
        mx.warn("If you want to use R packages cache, export environment variable FASTR_PKGS_CACHE_OPT. See option '--cache-pkgs' of 'mx pkgtest' for the syntax.")
    else:
        cache_args += ['--cache-pkgs', cache]
    result = pkgtest(["--verbose"] + cache_args + common_pkg_tests_args(graalvm_home) + ["--pkg-filelist", list_file])
    if result != 0:
        mx.abort("package test failed")

def internal_pkg_tests(graalvm_home=None):
    list_file = os.path.join(_fastr_suite.dir, 'com.oracle.truffle.r.test.native/packages/pkg-filelist')
    if os.environ.get('FASTR_RFFI') == 'llvm':
        list_file_llvm = list_file + '.llvm'
        if os.path.exists(list_file_llvm):
            list_file = list_file_llvm
    if 'FASTR_GCTORTURE' in os.environ:
        list_file_gctorture = list_file + '.gctorture'
        if os.path.exists(list_file_gctorture):
            list_file = list_file_gctorture
    result = pkgtest(["--verbose", "--pkg-filelist", list_file] + common_pkg_tests_args(graalvm_home))
    if result != 0:
        mx.abort("internal package test failed")

mx_gate.add_gate_runner(_fastr_suite, _fastr_gate_runner)

# Running this in "mx gate" would require to build FastR, because "mx gate" doesn't work with un-built FastR
def gnur_packages_test(args):
    '''
    Runs tests of packages intended to be used in GNU-R only (for now only fastRCluster).
    '''
    package = os.path.join(_fastr_suite.dir, 'com.oracle.truffle.r.pkgs/fastRCluster')
    gnur_binary = os.path.join(gnur_path(), 'bin', 'R')
    if not os.path.exists(gnur_binary):
        # FastR is not build yet, or GNUR_HOME_BINARY env variable is not set.
        mx.log("Ignoring gnur_packages_test - fastr is either not build or GNUR_HOME_BINARY env var unset")
        return
    mx.run([gnur_binary, 'CMD', 'build', package])
    result = glob.glob('fastRCluster*.tar.gz')
    if len(result) != 1:
        mx.abort('Found more than one file matching "fastRCluster_*.tar.gz". Are these some left over files or did R CMD build fastRCluster produce the package tarball?')
    new_env = os.environ.copy()
    new_env['_R_CHECK_FORCE_SUGGESTS_'] = 'false'
    mx.run([gnur_binary, 'CMD', 'check', '--no-manual', result[0]], env=new_env)

def rgate(args):
    '''
    Run 'mx.gate' with given args (used in CI system).
    N.B. This will fail if run without certain exclusions; use the local
    'gate' command for that.
    '''
    mx_gate.gate(args)

def _unittest_config_participant(config):
    vmArgs, mainClass, mainClassArgs = config
    # need to pass location of FASTR_UNIT_TESTS_NATIVE
    d = mx.distribution('FASTR_UNIT_TESTS_NATIVE')
    vmArgs = ['-Dfastr.test.native=' + d.path] + vmArgs
    return (vmArgs, mainClass, mainClassArgs)

def ut_simple(args):
    setUnitTestEnvironment(args)
    return mx_unittest.unittest(args + _simple_unit_tests())

def ut_noapps(args):
    setUnitTestEnvironment(args)
    return mx_unittest.unittest(args + _gate_noapps_unit_tests())

def ut_default(args):
    setUnitTestEnvironment(args)
    return mx_unittest.unittest(args + _all_unit_tests())

def ut_gate(args):
    setUnitTestEnvironment(args)
    return mx_unittest.unittest(args + _gate_unit_tests())

def ut_gen(args):
    setUnitTestEnvironment(args)
    return mx_unittest.unittest(args + _all_generated_unit_tests())

def ut(args):
    setUnitTestEnvironment(args)
    return mx_unittest.unittest(args)

def _test_package():
    return 'com.oracle.truffle.r.test'

def _test_subpackage(name):
    return '.'.join((_test_package(), name))

def _simple_generated_unit_tests():
    return list(map(_test_subpackage, ['engine.shell', 'engine.interop', 'library.base', 'library.grid', 'library.fastrGrid', 'library.methods', 'library.parallel', 'library.stats', 'library.tools', 'library.utils', 'library.fastr', 'builtins', 'functions', 'parser', 'rffi', 'rng', 'runtime.data', 'S4']))  # pylint: disable=line-too-long

def _simple_unit_tests():
    # com.oracle.truffle.tck.tests - truffle language inter-operability tck in com.oracle.truffle.r.test.tck/
    # com.oracle.truffle.r.test.tck - other tck tests in com.oracle.truffle.r.test/ e.g. FastRDebugTest
    # return _simple_generated_unit_tests() + ['com.oracle.truffle.r.nodes.castsTests', 'com.oracle.truffle.tck.tests', 'com.oracle.truffle.r.test.tck']
    return _simple_generated_unit_tests() + ['com.oracle.truffle.tck.tests', 'com.oracle.truffle.r.test.tck']

def _nodes_unit_tests():
    return ['com.oracle.truffle.r.nodes.test', 'com.oracle.truffle.r.nodes.access.vector']

def _apps_unit_tests():
    return [_test_subpackage('apps')]

def _gate_noapps_unit_tests():
    return _simple_unit_tests() + _nodes_unit_tests()

def _gate_unit_tests():
    return _gate_noapps_unit_tests() +  _apps_unit_tests()

def _all_unit_tests():
    return _gate_unit_tests()

def _all_generated_unit_tests():
    return _simple_generated_unit_tests()

def _test_srcdir():
    tp = 'com.oracle.truffle.r.test'
    return join(mx.project(tp).dir, 'src', tp.replace('.', sep))

def testgen(args):
    '''generate the expected output for unit tests'''
    # check we are in the home directory
    if os.getcwd() != _fastr_suite.dir:
        mx.abort('must run rtestgen from FastR home directory')

    def need_version_check():
        vardef = 'FASTR_TESTGEN_GNUR' in os.environ
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

    tests = _all_generated_unit_tests()
    # now just invoke unittst with the appropriate options
    mx.log("generating expected output for packages: ")
    for pkg in tests:
        mx.log("    " + str(pkg))
    os.environ["TZDIR"] = "/usr/share/zoneinfo/"
    _unset_conflicting_envs()
    mx_unittest.unittest(['-Dfastr.test.gen.expected=' + _test_srcdir(), '-Dfastr.test.gen.expected.quiet', '-Dfastr.test.project.output.dir=' + mx.project('com.oracle.truffle.r.test').output_dir()] + tests)

def _unset_conflicting_envs():
    # this can interfere with the recommended packages
    if 'R_LIBS_USER' in os.environ:
        del os.environ['R_LIBS_USER']
    # the default must be vi for unit tests
    if 'EDITOR' in os.environ:
        del os.environ['EDITOR']

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
    return os.environ.get('GNUR_HOME_BINARY', join(_fastr_suite.dir, 'libdownloads', r_version()))

def gnu_r(args):
    '''
    run the internally built GNU R executable'
    '''
    cmd = [join(_gnur_path(), 'bin', 'R')] + args
    return mx.run(cmd, nonZeroIsFatal=False)

def gnu_rscript(args, env=None):
    '''
    run the internally built GNU Rscript executable
    env arg is used by pkgtest
    '''
    cmd = [join(_gnur_path(), 'bin', 'Rscript')] + args
    return mx.run(cmd, nonZeroIsFatal=False, env=env)

def gnu_rtests(args, env=None):
    '''
    run tests of the internally built GNU R under tests subdirectory
    '''
    os.chdir(_fastr_suite.dir) # Packages install fails otherwise
 #   mx_fastr_pkgs.installpkgs(['--pkg-pattern', '^MASS$']) # required by tests/Examples/base-Ex.R
    np = mx.project('com.oracle.truffle.r.native')
    tst = join(np.dir, 'gnur', 'tests')
    tstsrc = join(tst, 'src')
    tstlog = join(tst, 'log')
    shutil.rmtree(tstlog, True)
    os.mkdir(tstlog)
    diffname = join(tstlog, 'all.diff')
    diff = open(diffname, 'a')
    try:
        for subd in ['Examples', '']:
            logd = join(tstlog, subd)
            if subd != '':
                os.mkdir(logd)
            os.chdir(logd)
            srcd = join(tstsrc, subd)
            for f in sorted(os.listdir(srcd)):
                if f.endswith('.R'):
                    print('Running {} explicitly by FastR CMD BATCH ...'.format(f))  # pylint: disable=superfluous-parens
                    mx.run([r_path(), '--vanilla', 'CMD', 'BATCH', join(srcd, f)] + args, nonZeroIsFatal=False, env=env, timeout=90)
                    outf = f + 'out'
                    if os.path.isfile(outf):
                        outff = outf + '.fastr'
                        os.rename(outf, outff)
                        print('Running {} explicitly by GnuR CMD BATCH ...'.format(f))  # pylint: disable=superfluous-parens
                        mx.run([join(_gnur_path(), 'bin', 'R'), '--vanilla', 'CMD', 'BATCH', join(srcd, f)] + args, nonZeroIsFatal=False, env=env, timeout=90)
                        if os.path.isfile(outf):
                            outfg = outf + '.gnur'
                            os.rename(outf, outfg)
                            diff.write('\nRdiff {} {}:\n'.format(outfg, outff))
                            diff.flush()
                            subprocess.Popen([r_path(), 'CMD', 'Rdiff', outfg, outff], stdout=diff, stderr=diff, shell=False)
                            diff.flush()
        diff.close()
        print('FastR to GnuR diff was written to {}'.format(diffname))  # pylint: disable=superfluous-parens
    finally:
        shutil.rmtree(join(_fastr_suite.dir, 'deparse'), True)

def run_codegen(main, args, **kwargs):
    '''
    Runs java with the com.oracle.truffle.r.ffi.codegen project on the class path and "main" as the entry point.
    '''
    jdk = get_default_jdk()
    vmArgs = mx.get_runtime_jvm_args('com.oracle.truffle.r.ffi.codegen', jdk=jdk)
    vmArgs += ['-ea', '-esa']
    vmArgs = _sanitize_vmArgs(jdk, vmArgs)
    vmArgs.append(main)
    return mx.run_java(vmArgs + args, jdk=jdk, **kwargs)

def run_testrfficodegen(args):
    '''
    Regenerates the generated code in com.oracle.truffle.r.test.native/packages/testrffi/testrffi package.
    '''
    testrffi_path = join(_fastr_suite.dir, 'com.oracle.truffle.r.test.native/packages/testrffi/testrffi')
    package = 'com.oracle.truffle.r.ffi.codegen.'
    run_codegen(package + 'FFITestsCodeGen', [join(testrffi_path, 'src/rffiwrappers.c')])
    run_codegen(package + 'FFITestsCodeGen', ['-h', join(testrffi_path, 'src/rffiwrappers.h')])
    run_codegen(package + 'FFITestsCodeGen', ['-init', join(testrffi_path, 'src/init_api.h')])
    run_codegen(package + 'FFITestsCodeGen', ['-r', join(testrffi_path, 'R/api.R')])

def run_rfficodegen(args):
    '''
    Regenerates the generated code that glues together the Java and C part.
    The generated files are located in in com.oracle.truffle.r.native/fficall/src.
    '''
    rffisrc_path = join(_fastr_suite.dir, 'com.oracle.truffle.r.native/fficall/src')
    package = 'com.oracle.truffle.r.ffi.codegen.'
    run_codegen(package + 'FFIUpCallsIndexCodeGen', [join(rffisrc_path, 'common/rffi_upcallsindex.h')])

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
    mx_subst.results_substitutions.register_no_arg('graalvm_version', mx.suite('sdk').release_version())
    if mx.suite("sulong", fatalIfMissing=False) and not _fastr_suite.isBinarySuite():
        # native.recommended runs FastR, it already has a build dependency to the FASTR distribution
        # if we are running with sulong we also need the SULONG_NATIVE distribution
        rec = mx.project('com.oracle.truffle.r.native.recommended')
        rec.buildDependencies += [mx.distribution('SULONG_NATIVE')]


# R package testing
_pkgtest_project = 'com.oracle.truffle.r.test.packages'
_pkgtest_analyzer_project = 'com.oracle.truffle.r.test.packages.analyzer'
_pkgtest_analyzer_main_class = _pkgtest_analyzer_project + '.PTAMain'
_pkgtest_module = None


def pkgtest_load():
    global _pkgtest_module
    if not _pkgtest_module:
        sys.path.append(join(_fastr_suite.dir, _pkgtest_project))
        import pkgtest
        _pkgtest_module = pkgtest
    return _pkgtest_module


def _pkgtest_args(args):
    graalvm_home = None
    if 'FASTR_GRAALVM' in os.environ:
        graalvm_home = os.environ['FASTR_GRAALVM']
    elif 'GRAALVM_FASTR' in os.environ:
        graalvm_home = os.environ['GRAALVM_FASTR']

    pkgtest_args = ['--very-verbose']
    pkgtest_args += ["--fastr-home"]
    pkgtest_args += [_fastr_suite.dir]
    if graalvm_home:
        # In GRAALVM mode, we assume FastR is not built so we need to
        _gnur_suite = mx.suite('gnur')
        pkgtest_args += ["--gnur-home"]
        pkgtest_args += [join(_gnur_suite.dir, 'gnur', _gnur_suite.extensions.r_version())]
        pkgtest_args += ["--graalvm-home"]
        pkgtest_args += [graalvm_home]
    else:
        pkgtest_args += ["--gnur-home"]
        pkgtest_args += [_gnur_path()]
    mx.logv(args)
    full_args = pkgtest_args + list(args)
    mx.logv(full_args)
    return full_args


def pkgtest(args, **kwargs):
    full_args = _pkgtest_args(args)
    mx.logv(["pkgtest"] + full_args)
    return pkgtest_load().pkgtest(full_args)


def installpkgs(args, **kwargs):
    full_args = _pkgtest_args(args)
    mx.logv(["installpkgs"] + full_args)
    return pkgtest_load().installpkgs(full_args)


def find_top100(*args, **kwargs):
    full_args = _pkgtest_args(args) + ["100"]
    mx.logv(["find_top"] + full_args)
    return pkgtest_load().find_top(full_args)


def find_top(*args, **kwargs):
    full_args = _pkgtest_args(args)
    mx.logv(["find_top"] + full_args)
    return pkgtest_load().find_top(args)


def r_pkgtest_analyze(args, **kwargs):
    '''
    Run analysis for package installation/testing results.
    '''
    vmArgs = mx.get_runtime_jvm_args(_pkgtest_analyzer_project)
    vmArgs += [_pkgtest_analyzer_main_class]
    mx.run_java(vmArgs + args)


def pkgcache(args, **kwargs):
    full_args = _pkgtest_args(args)
    mx.logv(["r-pkgcache"] + full_args)
    return pkgtest_load().pkgcache(full_args)

def build_binary_pkgs(args_in, **kwargs):
    '''
    Builds binary packages of components that we cache for build speed-up.
    See the CI scripts for details.
    '''
    parser = ArgumentParser()
    parser.add_argument('--f2c-version', type=int, dest='f2c_version', required=True, help='Current version of f2c, the tarball will use this + 1')
    parser.add_argument('--recommended-pkgs-version', default=0, type=int, dest='recommended_version', help='Current version of recommended packages binary, the tarball will use this + 1')
    parser.add_argument('--recommended-pkgs-list', dest='recommended_list', required=True, help='Comma separated list of recommended packages')
    args = parser.parse_args(args_in)

    os_name = platform.system().lower()
    dest_dir = os.path.join(_fastr_suite.dir, 'binary-packages')
    shutil.rmtree(dest_dir, ignore_errors=True)
    mx.ensure_dir_exists(dest_dir)

    # F2C
    # creates binary-packages/f2c-binary-{version}-{osname}-amd64/f2c with contents of FASTR_HOME/f2c
    f2c_name = 'f2c-binary-' + str(args.f2c_version + 1) + '-' + os_name + '-amd64'
    f2c_path = os.path.join(dest_dir, f2c_name)
    shutil.copytree(os.path.join(_fastr_suite.dir, 'f2c'), os.path.join(f2c_path, 'f2c'))
    # creates the tarball
    result_tarball = os.path.join(dest_dir, f2c_name + '.tar.gz')
    with tarfile.open(result_tarball, "w:gz") as tar:
        tar.add(f2c_path, arcname=os.path.basename(f2c_path))
    mx.log("Binary package created at: " + result_tarball)

    # Recommended packages
    # creates binary-packages/fastr-recommended-pkgs-{version}-{osname}-amd64/fastr-recommended-pkgs
    pkgs_name = 'fastr-recommended-pkgs-' + str(args.recommended_version + 1) + '-' + os_name + '-amd64'
    pkgs_path = os.path.join(dest_dir, pkgs_name)
    pkgs_pkgs_path = os.path.join(pkgs_path, 'pkgs')
    mx.ensure_dir_exists(pkgs_pkgs_path)
    for pkg_name in args.recommended_list.split(','):
        shutil.copytree(os.path.join(_fastr_suite.dir, 'library', pkg_name), os.path.join(pkgs_pkgs_path, pkg_name))
    # add file with API digest
    import io
    string_io = io.StringIO()
    try:
        sys.stdout = string_io
        pkgcache(['--print-api-checksum', '--vm', 'fastr'])
    finally:
        sys.stdout = sys.__stdout__
    with open(os.path.join(pkgs_path, 'api-checksum.txt'), 'w') as f:
        checksum = string_io.getvalue().splitlines(keepends=False)[-1]
        print(checksum, f)
    # creates the tarball
    result_tarball = os.path.join(dest_dir, pkgs_name + '.tar.gz')
    with tarfile.open(result_tarball, "w:gz") as tar:
        tar.add(pkgs_path, arcname=os.path.basename(pkgs_path))
    mx.log("Binary package created at: " + result_tarball)

    mx.log("Contents of the " + dest_dir + "directory: ")
    mx.run(['ls', '-R', dest_dir])
    return 0

COPYRIGHT_HEADER_GPL3 = """\
/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
// Checkstyle: stop
//@formatter:off
{0}
"""

def generate_parser(args=None, out=None):
    import re
    cast_pattern = re.compile(r"\(\(RuleContext\)_localctx,")
    mx.suite("truffle").extensions.create_parser(
        grammar_project="com.oracle.truffle.r.parser",
        grammar_package="com.oracle.truffle.r.parser",
        grammar_name="R",
        copyright_template=COPYRIGHT_HEADER_GPL3,
        postprocess=lambda content: cast_pattern.sub("(_localctx,", content),
        args=args, out=out)

mx_register_dynamic_suite_constituents = mx_fastr_dists.mx_register_dynamic_suite_constituents  # pylint: disable=C0103


mx_unittest.add_config_participant(_unittest_config_participant)

_commands = {
    'r' : [rshell, '[options]'],
    'R' : [rshell, '[options]'],
    'rscript' : [rscript, '[options]'],
    'Rscript' : [rscript, '[options]'],
    'gridserver' : [run_grid_server, ''],
    'rtestgen' : [testgen, ''],
    'rgate' : [rgate, ''],
    'rutsimple' : [ut_simple, ['options']],
    'rutdefault' : [ut_default, ['options']],
    'rutgate' : [ut_gate, ['options']],
    'rutgen' : [ut_gen, ['options']],
    'unittest' : [ut, ['options']],
    'rutnoapps' : [ut_noapps, ['options']],
    'rbcheck' : [rbcheck, '--filter [gnur-only,fastr-only,both,both-diff]'],
    'rbdiag' : [rbdiag, '(builtin)* [-v] [-n] [-m] [--sweep | --sweep=lite | --sweep=total] [--mnonly] [--noSelfTest] [--matchLevel=same | --matchLevel=error] [--maxSweeps=N] [--outMaxLev=N]'],
    'rrepl' : [rrepl, '[options]'],
    'rembed' : [rembed, '[options]'],
    'rembedtest' : [rembedtest, '[options]'],
    'r-cp' : [r_classpath, '[options]'],
    'pkgtest' : [pkgtest, ['options']],
    'r-pkgtest-analyze' : [r_pkgtest_analyze, ['options']],
    'r-findtop100' : [find_top100, ['options']],
    'r-findtop' : [find_top, ['options']],
    'r-pkgcache' : [pkgcache, ['options']],
    'installpkgs' : [installpkgs, '[options]'],
    'edinclude' : [mx_fastr_edinclude.edinclude, '[]'],
    'gnu-r' : [gnu_r, '[]'],
    'gnu-rscript' : [gnu_rscript, '[]'],
    'gnu-rtests' : [gnu_rtests, '[]'],
    'nativebuild' : [nativebuild, '[]'],
    'testrfficodegen' : [run_testrfficodegen, '[]'],
    'rfficodegen' : [run_rfficodegen, '[]'],
    'gnur-packages-test': [gnur_packages_test, '[]'],
    'build-binary-pkgs': [build_binary_pkgs, '[]'],
    'generate-r-parser': [generate_parser],
    }

mx.update_commands(_fastr_suite, _commands)
