#
# Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
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

import os
import subprocess
import mx
from os.path import join
from argparse import ArgumentParser, REMAINDER

def _gather_test_outputs_forpkg(pkgdirpath):
    '''return a list of paths to .Rout/.fail files in pkgdirpath'''
    result = []
    for dirpath, _, files in os.walk(pkgdirpath):
        for f in files:
            if f.endswith(".Rout") or f.endswith(".fail"):
                result.append(join(dirpath, f))
    result.sort()
    return result

def _gather_test_outputs(testdir, pkgs):
    '''return a dict mapping package names to list of output file paths'''
    result = dict()
    for dirpath, dirs, _ in os.walk(testdir):
        for d in dirs:
            if len(pkgs) == 0 or d in pkgs:
                result[d] = _gather_test_outputs_forpkg(join(dirpath, d))
        # only interested in top level
        break
    return result

def _find_start(content):
    marker = "Type 'q()' to quit R."
    for i in range(len(content)):
        line = content[i]
        if marker in line:
            return i + 1

def _find_end(content):
    marker = "Time elapsed:"
    for i in range(len(content)):
        line = content[i]
        if marker in line:
            return i - 1

def _fuzzy_compare(gnur_content, fastr_content):
    gnur_start = _find_start(gnur_content) + 1 # Gnu has extra empty line
    gnur_end = _find_end(gnur_content)
    fastr_start = _find_start(fastr_content)
    fastr_len = len(fastr_content)
    result = 0
    i = gnur_start
    while i + gnur_start < gnur_end:
        gnur_line = gnur_content[i + gnur_start]
        if i + fastr_start >= fastr_len:
            result = 1
            break

        fastr_line = fastr_content[i + fastr_start]
        if gnur_line != fastr_line:
            result = 1
            break
        i = i + 1
    return result

def pkgtestanalyze(args):
    '''
    Analyze test package test results by comparing with GnuR output.
    Return 0 if passed, non-zero if failed
    '''
    parser = ArgumentParser(prog='mx pkgtestanalyze')
    parser.add_argument('--dir', action='store', help='dir containing results', default=os.getcwd())
    parser.add_argument('--pkg', action='store', help='pkg to compare, default all')
    parser.add_argument('--verbose', action='store_true', help='print names of files that differ')
    parser.add_argument('--diff', action='store_true', help='execute given diff program on differing outputs')
    parser.add_argument('--difftool', action='store', help='diff tool', default='diff')
    parser.add_argument('pkgs', nargs=REMAINDER, metavar='pkg1 pkg2 ...')
    args = parser.parse_args(args)

    pkgs = args.pkgs
    if args.pkg:
        pkgs = [args.pkg] + pkgs

    verbose = args.verbose
    gnur = _gather_test_outputs(join(args.dir, "test_gnur"), pkgs)
    if args.pkg:
        if not gnur.has_key(args.pkg):
            mx.abort('no gnur output to compare')

    fastr = _gather_test_outputs(join(args.dir, "test"), pkgs)
    # gnur is definitive
    result = 0 # optimistic
    for pkg in pkgs:
        if not fastr.has_key(pkg):
            result = 1
            continue

        if not gnur.has_key(pkg):
            print 'no gnur output to compare: ' + pkg

        fastr_outputs = fastr[pkg]
        gnur_outputs = gnur[pkg]
        if len(fastr_outputs) != len(gnur_outputs):
            if verbose:
                print 'fastr is missing some output files'
                # TODO continue but handle missing files in loop?
                # does it ever happen in practice?
            result = 1
            continue
        for i in range(len(gnur_outputs)):
            fastr_output = fastr_outputs[i]
            gnur_output = gnur_outputs[i]
            gnur_content = None
            with open(gnur_output) as f:
                gnur_content = f.readlines()
            fastr_content = None
            with open(fastr_output) as f:
                fastr_content = f.readlines()
            result = _fuzzy_compare(gnur_content, fastr_content)
            if result != 0:
                if verbose:
                    print 'mismatch on file: ' + fastr_output
                if args.diff:
                    cmd = [args.difftool, gnur_output, fastr_output]
                    print ' '.join(cmd)
                    subprocess.call(cmd)
                break
    return result

