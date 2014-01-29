#
# Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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
import subprocess
from os.path import join, sep
from argparse import ArgumentParser
import shlex
import mx
import mx_graal
import os

def runRCommand(args):
    '''run R program or shell [--J @VMargs] [path]'''
    vmArgs, rArgs = _extract_vmArgs(args)
    os.environ['R_HOME'] = mx.suite('fastr').dir
    mx_graal.vm(vmArgs + ['-cp', rShellCp(), rCommandClass()] + rArgs)

def rShellCp():
    return mx.classpath("com.oracle.truffle.r.shell")


def rCommandClass():
    return "com.oracle.truffle.r.shell.RCommand"

def _extract_vmArgs(args):
    '''custom version of mx._extract_VM_args that supports --J'''
    rArgs = []
    vmArgs = []
    # mx._extract_VM_args style
    doubleDash = args.index('--') if '--' in args else None
    if doubleDash is not None:
        return (args[:doubleDash], args[doubleDash + 1:])

    args_len = len(args)
    i = 0
    while i < args_len:
        arg = args[i]
        if arg.startswith('--J'):
            if i == args_len - 1 or not args[i + 1].startswith('@'):
                mx.abort("@args expected after --J")
            else:
                i = i + 1
                vmArgs = vmArgs + shlex.split(args[i].lstrip('@'))
        else:
            rArgs.append(arg)
        i = i + 1
    return (vmArgs, rArgs)

def _truffle_r_gate_body(args, tasks):
    t = mx_graal.Task('BuildHotSpotGraalServer: product')
    mx_graal.buildvms(['--vms', 'server', '--builds', 'product'])
    tasks.append(t.stop())

    with mx_graal.VM('original', 'product'):
        # check that the expected test output file is up to date
        t = mx_graal.Task('UnitTests: ExpectedTestOutput file check')
        junit(['--tests', _default_unit_tests(), '--check-expected-output'])
        tasks.append(t.stop())
        t = mx_graal.Task('UnitTests: simple')
        junit(['--tests', _default_unit_tests()])
        tasks.append(t.stop())

    if args.jacocout is not None:
        mx_graal.jacocoreport([args.jacocout])

    mx_graal._jacoco = 'off'

def gate(args):
    '''Run the R gate'''
    # ideally would be a standard gate task - we do it early
    t = mx_graal.Task('Copyright check')
    rc = mx.checkcopyrights(['--primary'])
    t.stop()
    if rc != 0:
        mx.abort('copyright errors')
    mx_graal.gate(args, _truffle_r_gate_body)

def _junit_r_harness(args, vmArgs, junitArgs):
    # always pass the directory where the expected output file should reside
    tp = 'com.oracle.truffle.r.test'
    runlistener_arg = 'expected=' + join(mx.project(tp).dir, 'src', tp.replace('.', sep))
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

    if args.gen_diff_output:
        runlistener_arg = add_arg_separator()
        runlistener_arg = 'gen-diff=' + args.gen_diff_output

#    if args.test_methods:
#        runlistener_arg = add_arg_separator()
#        runlistener_arg = 'test-methods=' + args.test_methods

    # use a custom junit.RunListener
    runlistener = 'com.oracle.truffle.r.test.TestBase$RunListener'
    if len(runlistener_arg) > 0:
        runlistener += ':' + runlistener_arg

    junitArgs += ['--runlistener', runlistener]
    if mx_graal._get_vm() == 'graal':
        vmArgs += '-XX:-BootstrapGraal'
    return mx_graal.vm(vmArgs + junitArgs, nonZeroIsFatal=False)

def junit(args):
    '''run R Junit tests'''
    parser = ArgumentParser(prog='r junit')
    parser.add_argument('--gen-expected-output', action='store_true', help='generate/update expected test output file')
    parser.add_argument('--keep-trailing-whitespace', action='store_true', help='keep trailing whitespace in expected test output file')
    parser.add_argument('--check-expected-output', action='store_true', help='check but do not update expected test output file')
    parser.add_argument('--gen-fastr-output', action='store', metavar='<path>', help='generate FastR test output file')
    parser.add_argument('--gen-diff-output', action='store', metavar='<path>', help='generate difference test output file ')
    # parser.add_argument('--test-methods', action='store', help='pattern to match test methods in test classes')

    return mx.junit(args, _junit_r_harness, parser=parser)

def _default_unit_tests():
    return 'com.oracle.truffle.r.test.simple'

def testgen(args):
    '''generate the expected output for unit tests'''
    # we just invoke junit with the appropriate options
    junit(args + ['--tests', _default_unit_tests(), '--gen-expected-output'])

def ignoredtests(args):
    """generate the ignored unit tests file using annotation processor"""
    testOnly = ['--projects', 'com.oracle.truffle.r.test']
    mx.clean(testOnly)
    mx.build(testOnly)

_fastr_suite = None

def rbench(args):
    '''run an R benchmark'''
    parser = ArgumentParser(prog='mx rbench')
    parser.add_argument('bm', action='store', metavar='benchmarkgroup.name', help='qualified name of benchmark')
    parser.add_argument('--path', action='store_true', help='print path to benchmark')
    parser.add_argument('--J', dest='vm_args', help='Graal VM arguments (e.g. --J @-dsa)', metavar='@<args>')
    parser.add_argument('--gnur', action='store_true', help='run under GnuR')
    parser.add_argument('--gnur-jit', action='store_true', help='enable GnuR JIT')
    args = parser.parse_args(args)

    # dynamically load the benchmarks suite
    hg_base = mx.get_env('HG_BASE')
    alternate = None if hg_base is None else join(hg_base, 'r_benchmarks')
    bm_suite = _fastr_suite.import_suite('r_benchmarks', version=None, alternate=alternate)
    mx.build_suite(bm_suite)

    # Get the R script location via helper app
    # N.B. we do not use mx.java() as that might check options we don't want for the helper, e.g. debugging agent
    javacmd = ['java', '-cp', mx.classpath('r.benchmarks'), 'r.benchmarks.RBenchmarks', args.bm]
    try:
        bmpath = subprocess.check_output(javacmd).rstrip()
        if args.path:
            print bmpath
        else:
            command = []
            if args.vm_args is not None:
                command = ['--J', args.vm_args]
            command = command + ['-f', bmpath]
            if args.gnur:
                env = os.environ
                if args.gnur_jit:
                    env['R_ENABLE_JIT'] = '3'
                rc = subprocess.call(['R', '--slave'] + command, env=env)
                if rc != 0:
                    mx.abort('GnuR failed with rc: ' + rc)
            else:
                runRCommand(command)
    except subprocess.CalledProcessError:
        mx.abort(1)

def _bench_harness_body(args, vmArgs):
    mx_graal.buildvms(['--vms', 'server', '--builds', 'product'])
    marks = ['shootout.binarytrees', 'shootout.fannkuchredux', 'shootout.fasta', 'shootout.fastaredux',
             'shootout.knucleotide', 'shootout.mandelbrot-ascii', 'shootout.nbody', 'shootout.pidigits',
             'shootout.regexdna', 'shootout.reversecomplement', 'shootout.spectralnorm']
    for mark in marks:
        rbench([mark])

def bench(args):
    mx.bench(args, harness=_bench_harness_body)

def mx_init(suite):
    global _fastr_suite
    _fastr_suite = suite
    commands = {
        'gate' : [gate, ''],
        'r' : [runRCommand, '[options]'],
        'R' : [runRCommand, '[options]'],
        'rtestgen' : [testgen, ''],
        'rignoredtests' : [ignoredtests, ''],
        'junit' : [junit, ['options']],
        'rbench' : [rbench, 'options'],
        'bench' : [bench, 'options'],
    }
    mx.update_commands(suite, commands)

