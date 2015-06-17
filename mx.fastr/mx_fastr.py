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
import tempfile, shutil, platform, zipfile, sys, subprocess
from os.path import join, sep, exists
from argparse import ArgumentParser
import mx
import mx_graal
import os

_fastr_suite = None

def runR(args, className, nonZeroIsFatal=True, extraVmArgs=None, runBench=False, graal_vm='server'):
    # extraVmArgs is not normally necessary as the global --J option can be used running R/RScript
    # However, the bench command invokes other Java VMs along the way, so it must use extraVmArgs
    setREnvironment(graal_vm)
    project = className.rpartition(".")[0]
    vmArgs = ['-cp', mx.classpath(project)]
    vmArgs = vmArgs + ["-Drhome.path=" + _fastr_suite.dir]


    if runBench == False:
        vmArgs = vmArgs + ['-ea', '-esa']
    if extraVmArgs:
        vmArgs = vmArgs + extraVmArgs
    return mx_graal.vm(vmArgs + [className] + args, vm=graal_vm, nonZeroIsFatal=nonZeroIsFatal)

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
    Check for the --vm global mx argument by checking mx.graal._vm.
    '''
    return "server" if mx_graal._vm is None else mx_graal._vm

def rshell(args, nonZeroIsFatal=True, extraVmArgs=None, runBench=False):
    '''run R shell'''
    # Optional args for external use by benchmarks
    graal_vm = _get_graal_vm()
    return runR(args, "com.oracle.truffle.r.shell.RCommand", nonZeroIsFatal=nonZeroIsFatal, extraVmArgs=extraVmArgs, runBench=runBench, graal_vm=graal_vm)

def rscript(args):
    '''run Rscript'''
    graal_vm = _get_graal_vm()
    return runR(args, "com.oracle.truffle.r.shell.RscriptCommand", graal_vm=graal_vm)

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
    mx_graal.build(args, vm=graal_vm) # this calls mx.build

def findbugs(args):
    '''run FindBugs against non-test Java projects'''
    findBugsHome = mx.get_env('FINDBUGS_HOME', None)
    if findBugsHome:
        findbugsJar = join(findBugsHome, 'lib', 'findbugs.jar')
    else:
        findbugsLib = join(_fastr_suite.dir, 'lib', 'findbugs-3.0.0')
        if not exists(findbugsLib):
            tmp = tempfile.mkdtemp(prefix='findbugs-download-tmp', dir=_fastr_suite.dir)
            try:
                findbugsDist = join(tmp, 'findbugs.zip')
                mx.download(findbugsDist, ['http://lafo.ssw.uni-linz.ac.at/graal-external-deps/findbugs-3.0.0.zip', 'http://sourceforge.net/projects/findbugs/files/findbugs/3.0.0/findbugs-3.0.0.zip'])
                with zipfile.ZipFile(findbugsDist) as zf:
                    candidates = [e for e in zf.namelist() if e.endswith('/lib/findbugs.jar')]
                    assert len(candidates) == 1, candidates
                    libDirInZip = os.path.dirname(candidates[0])
                    zf.extractall(tmp)
                shutil.copytree(join(tmp, libDirInZip), findbugsLib)
            finally:
                shutil.rmtree(tmp)
        findbugsJar = join(findbugsLib, 'findbugs.jar')
    assert exists(findbugsJar)
    nonTestProjects = [p for p in _fastr_suite.projects if not p.name.endswith('.test') and not p.name.endswith('.processor') and not p.native]
    outputDirs = [p.output_dir() for p in nonTestProjects]
    findbugsResults = join(_fastr_suite.dir, 'findbugs.html')

    cmd = ['-jar', findbugsJar, '-low', '-maxRank', '15', '-exclude', join(_fastr_suite.mxDir, 'findbugs-exclude.xml'), '-html']
    if sys.stdout.isatty():
        cmd.append('-progress')
    cmd = cmd + ['-auxclasspath', mx.classpath([p.name for p in nonTestProjects]), '-output', findbugsResults, '-exitcode'] + args + outputDirs
    exitcode = mx.run_java(cmd, nonZeroIsFatal=False)
    return exitcode

def _fastr_gate_body(args, tasks):
    with mx.GateTask('BuildJavaWithJavac', tasks) as t:
        if t: build([])
    # check that the expected test output file is up to date
    with mx.GateTask('UnitTests: ExpectedTestOutput file check', tasks) as t:
        if t:
            rc1 = junit(['--tests', _all_unit_tests(), '--check-expected-output'])
            if rc1 != 0:
                mx.abort('unit tests expected output check failed')

    with mx.GateTask('UnitTests: gate', tasks) as t:
        if t:
            rc2 = junit(['--tests', _gate_unit_tests()])
            if rc2 != 0:
                mx.abort('unit tests failed')

def gate(args):
    '''Run the R gate'''
    # suppress the download meter
    mx._opts.no_download_progress = True

    # FastR has custom copyright check

    t = mx.GateTask('Copyright check')
    rc = mx.checkcopyrights(['--primary'])
    t.stop()
    if rc != 0:
        mx.abort('copyright errors')

    # Enforce checkstyle (not dedfault task in mx.gate)
    t = mx.GateTask('Checkstyle check')
    rc = mx.checkstyle(['--primary'])
    t.stop()
    if rc != 0:
        mx.abort('checkstyle errors')

# temp disable due to non-determinism
#    t = mx.GateTask('FindBugs')
#    rc = findbugs([])
#    t.stop()
#    if rc != 0:
#        mx.abort('FindBugs warnings were found')

    mx.gate(args, _fastr_gate_body)

def _test_harness_body(args, vmArgs):
    '''the callback from mx.bench'''
    print "placeholder for mx test"

def test(args):
    vm = mx_graal.VM('server' if mx_graal._vm is None else mx_graal._vm)
    with vm:
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

    graal_vm = _get_graal_vm()
    setREnvironment(graal_vm)

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

def _gate_unit_tests():
    return ','.join((_library_unit_tests(), _rffi_unit_tests(), _rpackages_unit_tests(), _builtins_unit_tests(), _functions_unit_tests(), _ser_unit_tests(), _app_unit_tests(), _nodes_unit_tests()))

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
        fastr_version = subprocess.check_output([mx.java().java, '-cp', mx.classpath('com.oracle.truffle.r.runtime'), 'com.oracle.truffle.r.runtime.RVersionNumber'])
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
    return "com.oracle.truffle.r.repl.RREPLServer"

def runRREPL(args, nonZeroIsFatal=True, extraVmArgs=None):
    '''run R repl'''
    return runR(args, _rREPLClass(), nonZeroIsFatal=nonZeroIsFatal, extraVmArgs=['-DR:+Instrument'])

def load_optional_suite(name):
    hg_base = mx.get_env('MX_HG_BASE')
    urls = [] if hg_base is None else [join(hg_base, name)]
    opt_suite = _fastr_suite.import_suite(name, version=None, urls=urls)
    if opt_suite:
        mx.build_suite(opt_suite)
    return opt_suite

def mx_post_parse_cmd_line(opts):
    # load optional suites, r_apptests first so r_benchmarks can find it
    load_optional_suite('r_apptests')
    load_optional_suite('r_benchmarks')

def mx_init(suite):
    global _fastr_suite
    _fastr_suite = suite
    commands = {
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
        'findbugs' : [findbugs, ''],
        'test' : [test, ['options']],
        'rrepl' : [runRREPL, '[options]'],
    }
    mx.update_commands(suite, commands)
