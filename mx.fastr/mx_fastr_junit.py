#
# Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
from argparse import ArgumentParser, REMAINDER
import os, tempfile
import mx
import mx_fastr

def junit(args, harness, parser=None, jdk_default=None):
    """run Junit tests"""
    suppliedParser = parser is not None
    parser = parser if suppliedParser else ArgumentParser(prog='mx junit')
    parser.add_argument('--tests', action='store', help='pattern to match test classes')
    parser.add_argument('--J', dest='vm_args', action='append', help='target VM arguments (e.g. --J @-dsa)', metavar='@<args>')
    parser.add_argument('--jdk', action='store', help='jdk to use')
    if suppliedParser:
        parser.add_argument('remainder', nargs=REMAINDER, metavar='...')
    args = parser.parse_args(args)

    vmArgs = ['-ea', '-esa']

    if args.vm_args:
        vmArgs = vmArgs + mx_fastr.split_j_args(args.vm_args)

    testfile = os.environ.get('MX_TESTFILE', None)
    if testfile is None:
        (_, testfile) = tempfile.mkstemp(".testclasses", "mx")
        os.close(_)

    candidates = []
    if args.jdk:
        jdk = mx.get_jdk(tag=args.jdk)
        if not jdk:
            mx.abort("jdk '" + args.jdk + "' not found")
    else:
        if not jdk_default:
            jdk = mx.get_jdk()
        else:
            jdk = jdk_default

    for p in mx.projects(opt_limit_to_suite=True):
        if not p.isJavaProject() or jdk.javaCompliance < p.javaCompliance:
            continue
        candidates += _find_classes_with_annotations(p, None, ['@Test']).keys()

    tests = [] if args.tests is None else [name for name in args.tests.split(',')]
    classes = []
    if len(tests) == 0:
        classes = candidates
    else:
        for t in tests:
            found = False
            for c in candidates:
                if t in c:
                    found = True
                    classes.append(c)
            if not found:
                mx.warn('no tests matched by substring "' + t + '"')

    dists = ['FASTR', 'FASTR_UNIT_TESTS']
    if mx.suite('r-apptests', fatalIfMissing=False):
        dists.append('com.oracle.truffle.r.test.apps')
    vmArgs += mx.get_runtime_jvm_args(dists, jdk=jdk)

    if len(classes) != 0:
        if len(classes) == 1:
            testClassArgs = ['--testclass', classes[0]]
        else:
            with open(testfile, 'w') as f:
                for c in classes:
                    f.write(c + '\n')
            testClassArgs = ['--testsfile', testfile]
        junitArgs = ['com.oracle.truffle.r.test.FastRJUnitWrapper'] + testClassArgs
        rc = harness(args, vmArgs, jdk, junitArgs)
        return rc
    else:
        return 0

def _find_classes_with_annotations(p, pkgRoot, annotations, includeInnerClasses=False):
    """
    Scan the sources of project `p` for Java source files containing a line starting with `annotation`
    (ignoring preceding whitespace) and return the fully qualified class name for each Java
    source file matched in a list.
    """

    matches = lambda line: len([a for a in annotations if line == a or line.startswith(a + '(')]) != 0
    return p.find_classes_with_matching_source_line(pkgRoot, matches, includeInnerClasses)

