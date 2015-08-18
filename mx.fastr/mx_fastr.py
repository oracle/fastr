#
# Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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
import tempfile, platform, subprocess, shlex
from os.path import join, sep
from argparse import ArgumentParser
import mx
import mx_gate
import mx_graal
import mx_jvmci
import os

_fastr_suite = mx.suite('fastr')

def r_command_project():
    return "com.oracle.truffle.r.engine"

def r_command_class():
    return r_command_project() + ".shell.RCommand"

def rscript_command_class():
    return r_command_project() + ".shell.RscriptCommand"

def runR(args, className, nonZeroIsFatal=True, extraVmArgs=None, runBench=False, graal_vm='server'):
    setREnvironment(graal_vm)
    vmArgs = ['-cp', mx.classpath(r_command_project())]
    vmArgs += ["-Drhome.path=" + _fastr_suite.dir]

    vmArgs += ['-G:InliningDepthError=500', '-XX:JVMCINMethodSizeLimit=1000000']

    if runBench == False:
        vmArgs += ['-ea', '-esa']
    if extraVmArgs:
        vmArgs += extraVmArgs
    vmArgs += _add_truffle_jar()
    return mx_graal.vm(vmArgs + [className] + args, vm=graal_vm, nonZeroIsFatal=nonZeroIsFatal)

def _add_truffle_jar():
    # Unconditionally prepend truffle.jar to the boot class path.
    # This used to be done by the VM itself but was removed to
    # separate the VM from Truffle.
    truffle_jar = mx.distribution('truffle:TRUFFLE_API').path
    return ['-XX:-UseJVMCIClassLoader', '-Xbootclasspath/p:' + truffle_jar]

def setREnvironment(graal_vm):
    osname = platform.system()
    lib_base = join(_fastr_suite.dir, 'com.oracle.truffle.r.native', 'builtinlibs', 'lib')
    lib_value = lib_base
    if osname == 'Darwin':
        lib_env = 'DYLD_FALLBACK_LIBRARY_PATH'
        lib_value = lib_value + os.pathsep + '/usr/lib'
    else:
        lib_env = 'LD_LIBRARY_PATH'
    os.environ[lib_env] = lib_value
    # For R sub-processes we need to set the DEFAULT_VM environment variable
    os.environ['DEFAULT_VM'] = graal_vm

def _get_graal_vm():
    '''
    Check for the --vm global mx argument by checking mx.jvmci._vm.
    '''
    return "server" if mx_jvmci._vm is None else mx_jvmci._vm

def rcommon(args, command, klass):
    parser = ArgumentParser(prog='mx ' + command)
    parser.add_argument('--J', dest='extraVmArgsList', action='append', help='extra Java VM arguments', metavar='@<args>')
    ns, rargs = parser.parse_known_args(args)
    extraVmArgs = []
    if ns.extraVmArgsList:
        for e in ns.extraVmArgsList:
            extraVmArgs += [x for x in shlex.split(e.lstrip('@'))]
    graal_vm = _get_graal_vm()
    return runR(rargs, klass, extraVmArgs=extraVmArgs, graal_vm=graal_vm)

def rshell(args):
    '''run R shell'''
    return rcommon(args, 'R', r_command_class())

def rscript(args):
    '''run Rscript'''
    return rcommon(args, 'Rscript', rscript_command_class())

def build(args):
    '''FastR build'''
    graal_vm = _get_graal_vm()
    # Overridden in case we ever want to do anything non-standard
    # workaround for Hotspot Mac OS X build problem
    osname = platform.system()
    if osname == 'Darwin':
        os.environ['COMPILER_WARNINGS_FATAL'] = 'false'
        os.environ['USE_CLANG'] = 'true'
        os.environ['LFLAGS'] = '-Xlinker -lstdc++'
    mx_jvmci.build(args, vm=graal_vm) # this calls mx.build

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
    with mx_gate.Task('BuildNative', tasks):
        build([])

    # check that the expected test output file is up to date
    with mx_gate.Task('UnitTests: ExpectedTestOutput file check', tasks) as t:
        if t:
            if junit(['--tests', _all_unit_tests(), '--check-expected-output']) != 0:
                t.abort('unit tests expected output check failed')

    with mx_gate.Task('UnitTests: +EST', tasks) as t:
        if t:
            if junit(['--J', '@-DR:+NewStateTransition', '--tests', _gate_unit_tests()]) != 0:
                t.abort('unit tests failed')

    with mx_gate.Task('UnitTests: -EST', tasks) as t:
        if t:
            if junit(['--J', '@-DR:-NewStateTransition', '--tests', _gate_unit_tests()]) != 0:
                t.abort('unit tests failed')

mx_gate.add_gate_runner(_fastr_suite, _fastr_gate_runner)

def gate(args):
    '''Run the R gate'''

    # exclude findbugs until compliant
    mx_gate.gate(args + ['-x', '-t', 'FindBugs,Checkheaders,Checkstyle,Distribution Overlap Check'])

def _test_harness_body(args, vmArgs):
    '''the callback from mx.bench'''
    print "placeholder for mx test"

def test(args):
    vm = _get_graal_vm()
    with mx_jvmci.VM(vm):
        mx.test(args, harness=_test_harness_body)

def _test_srcdir():
    tp = 'com.oracle.truffle.r.test'
    return join(mx.project(tp).dir, 'src', tp.replace('.', sep))

def _junit_r_harness(args, vmArgs, junitArgs):
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

    # suppress Truffle compilation by using a high threshold
    vmArgs += ['-G:TruffleCompilationThreshold=100000']

    vmArgs += ['-G:InliningDepthError=500', '-XX:JVMCINMethodSizeLimit=1000000']

    graal_vm = _get_graal_vm()
    setREnvironment(graal_vm)
    vmArgs += _add_truffle_jar()

    return mx_graal.vm(vmArgs + junitArgs, vm=graal_vm, nonZeroIsFatal=False)

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
    return junit(['--tests', _library_unit_tests()] + args)

def junit_default(args):
    return junit(['--tests', _all_unit_tests()] + args)

def junit_gate(args):
    return junit(['--tests', _gate_unit_tests()] + args)

def _test_package():
    return 'com.oracle.truffle.r.test'

def _test_subpackage(name):
    return '.'.join((_test_package(), name))

def _nodes_unit_tests():
    return 'com.oracle.truffle.r.nodes.test'

def _library_unit_tests():
    return ','.join((_test_subpackage('library.base'), _test_subpackage('library.stats'), _test_subpackage('library.utils')))

def _builtins_unit_tests():
    return _test_subpackage('builtins')

def _functions_unit_tests():
    return _test_subpackage('functions')

def _rffi_unit_tests():
    return _test_subpackage('rffi')

def _rpackages_unit_tests():
    return _test_subpackage('rpackages')

def _ser_unit_tests():
    return _test_subpackage('ser')

def _app_unit_tests():
    return _test_subpackage('apps')

def _tck_unit_tests():
    return _test_subpackage('tck')

def _gate_unit_tests():
    return ','.join((_library_unit_tests(), _rffi_unit_tests(), _rpackages_unit_tests(), _builtins_unit_tests(), _functions_unit_tests(), _ser_unit_tests(), _app_unit_tests(), _nodes_unit_tests(), _tck_unit_tests()))

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
    # check the version of GnuR against FastR
    try:
        fastr_version = subprocess.check_output([mx.get_jdk().java, '-cp', mx.classpath('com.oracle.truffle.r.runtime'), 'com.oracle.truffle.r.runtime.RVersionNumber'])
        gnur_version = subprocess.check_output(['R', '--version'])
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
    '''check FastR builtins against GnuR'''
    parser = ArgumentParser(prog='mx rbcheck')
    parser.add_argument('--check-internal', action='store_const', const='--check-internal', help='check .Internal functions')
    parser.add_argument('--unknown-to-gnur', action='store_const', const='--unknown-to-gnur', help='list builtins not in GnuR FUNCTAB')
    parser.add_argument('--todo', action='store_const', const='--todo', help='show unimplemented')
    parser.add_argument('--no-eval-args', action='store_const', const='--no-eval-args', help='list functions that do not evaluate their args')
    parser.add_argument('--visibility', action='store_const', const='--visibility', help='list visibility specification')
    parser.add_argument('--printGnuRFunctions', action='store', help='ask GnuR to "print" value of functions')
    parser.add_argument('--packageBase', action='store', help='directory to be recursively scanned for R sources (used to get frequencies for builtins)')
    parser.add_argument('--interactive', action='store_const', const='--interactive', help='interactive querying of the word frequencies')
    args = parser.parse_args(args)

    class_map = mx.project('com.oracle.truffle.r.nodes').find_classes_with_matching_source_line(None, lambda line: "@RBuiltin" in line, True)
    classes = []
    for className, path in class_map.iteritems():
        classNameX = className.split("$")[0] if '$' in className else className

        if not classNameX.endswith('Factory'):
            classes.append([className, path])

    class_map = mx.project('com.oracle.truffle.r.nodes.builtin').find_classes_with_matching_source_line(None, lambda line: "@RBuiltin" in line, True)
    for className, path in class_map.iteritems():
        classNameX = className.split("$")[0] if '$' in className else className

        if not classNameX.endswith('Factory'):
            classes.append([className, path])

    (_, testfile) = tempfile.mkstemp(".classes", "mx")
    os.close(_)
    with open(testfile, 'w') as f:
        for c in classes:
            f.write(c[0] + ',' + c[1] + '\n')
    analyzeArgs = []
    if args.check_internal:
        analyzeArgs.append(args.check_internal)
    if args.unknown_to_gnur:
        analyzeArgs.append(args.unknown_to_gnur)
    if args.todo:
        analyzeArgs.append(args.todo)
    if args.no_eval_args:
        analyzeArgs.append(args.no_eval_args)
    if args.visibility:
        analyzeArgs.append(args.visibility)
    if args.interactive:
        analyzeArgs.append(args.interactive)
    if args.printGnuRFunctions:
        analyzeArgs.append('--printGnuRFunctions')
        analyzeArgs.append(args.printGnuRFunctions)
    if args.packageBase:
        analyzeArgs.append('--packageBase')
        analyzeArgs.append(args.packageBase)
    analyzeArgs.append(testfile)
    cp = mx.classpath([pcp.name for pcp in mx.projects_opt_limit_to_suites()])
    mx.run_java(['-cp', cp, 'com.oracle.truffle.r.test.tools.AnalyzeRBuiltin'] + analyzeArgs)

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

def bench(args):
    mx.abort("no benchmarks available")

def _rREPLClass():
    return "com.oracle.truffle.r.engine.repl.RREPLServer"

def runRREPL(args, nonZeroIsFatal=True, extraVmArgs=None):
    '''run R repl'''
    return runR(args, _rREPLClass(), nonZeroIsFatal=nonZeroIsFatal, extraVmArgs=['-DR:+Instrument'])

def load_optional_suite(name):
    hg_base = mx.get_env('MX_HG_BASE')
    urlinfos = None if hg_base is None else [mx.SuiteImportURLInfo(join(hg_base, name), 'hg', mx.vc_system('hg'))]
    opt_suite = _fastr_suite.import_suite(name, version=None, urlinfos=urlinfos)
    if opt_suite:
        mx.build_suite(opt_suite)
    return opt_suite

def mx_post_parse_cmd_line(opts):
    # load optional suites, r_apptests first so r_benchmarks can find it
    load_optional_suite('r_apptests')
    load_optional_suite('r_benchmarks')

_commands = {
    # new commands
    'r' : [rshell, '[options]'],
    'R' : [rshell, '[options]'],
    'rscript' : [rscript, '[options]'],
    'Rscript' : [rscript, '[options]'],
    'rtestgen' : [testgen, ''],
    # core overrides
    'bench' : [bench, ''],
    'build' : [build, ''],
    'gate' : [gate, ''],
    'junit' : [junit, ['options']],
    'junitsimple' : [junit_simple, ['options']],
    'junitdefault' : [junit_default, ['options']],
    'junitgate' : [junit_gate, ['options']],
    'unittest' : [unittest, ['options']],
    'rbcheck' : [rbcheck, ['options']],
    'rcmplib' : [rcmplib, ['options']],
    'test' : [test, ['options']],
    'rrepl' : [runRREPL, '[options]'],
    }

mx.update_commands(_fastr_suite, _commands)
