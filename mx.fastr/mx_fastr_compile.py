#
# Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
"""
A wrapper for the C/C++/Fortran compilers that optionally handles the generation of LLVM bitcode.
When not running with sulong is simply forwards to the default compiler for the platform.
When running under sulong, it uses sulong to do two compilations; first to generate object code
and second to generate LLVM bitcode.
"""
import os, sys
import mx
import mx_fastr

def _sulong():
    sulong = mx.suite('sulong', fatalIfMissing=False)
    if sulong:
        return sulong.extensions
    else:
        return None

def _is_linux():
    return sys.platform.startswith('linux')

def _is_darwin():
    return sys.platform.startswith('darwin')

def _log(cmd, args):
    if os.environ.has_key('FASTR_COMPILE_LOGFILE'):
        with open(os.environ['FASTR_COMPILE_LOGFILE'], 'a') as f:
            f.write(cmd)
            f.write('(')
            f.write(os.getcwd())
            f.write(')')
            f.write(' ')
            f.write(' '.join(args))
            f.write('\n')

class AnalyzedArgs:
    '''
    is_link: True iff the command is a shared library link
    llvm_ir_file: the target file for the ir derived from the .o file (only set if is_link=False)
    compile_args: possibly modified args for C compilation
    emit_llvm_args: the args to generate the llvm ir
    '''
    def __init__(self, llvm_ir_file, is_link, compile_args, emit_llvm_args):
        self.llvm_ir_file = llvm_ir_file
        self.is_link = is_link
        self.compile_args = compile_args
        self.emit_llvm_args = emit_llvm_args


def _c_dummy_file():
    return os.path.join(mx_fastr._fastr_suite.dir, 'com.oracle.truffle.r.native', 'fficall', 'src', 'truffle', 'llvm_dummy.c')

def _analyze_args(args, dragonEgg=False):
    '''
    Analyzes the original arguments to the compiler and returns an adjusted
    list that will run the compiler (via sulong) to extract the llvm ir.
    Result is an instance of AnalyzedArgs:
    '''
    compile_args = []
    emit_llvm_args = []
    llvm_ir_file_ext = '.bc'
    if not dragonEgg:
        emit_llvm_args.append('-emit-llvm')
    else:
        # dragonEgg plugin doesn't seem able to make bitcode directly
        emit_llvm_args.append('-S')
        llvm_ir_file_ext = '.ll'

    is_link = False
    llvm_ir_file = None
    c_dummy = False
    i = 0
    while i < len(args):
        arg = args[i]
        if arg == '-DFASTR_LLVM':
            c_dummy = True
            i = i + 1
            continue

        emit_llvm_args.append(arg)
        compile_args.append(arg)
        if arg == '-c':
            cfile = args[i + 1]
            if c_dummy:
                cfile = _c_dummy_file()
            compile_args.append(cfile)
            emit_llvm_args.append(args[i + 1])
            i = i + 1

        if arg == '-o':
            ext = os.path.splitext(args[i + 1])[1]
            is_link = ext == '.so' or ext == '.dylib'
            compile_args.append(args[i + 1])
            if ext == '.o':
                llvm_ir_file = os.path.splitext(args[i + 1])[0] + llvm_ir_file_ext
                emit_llvm_args.append(llvm_ir_file)
            i = i + 1

        i = i + 1
    _log('adjusted-compile-args', compile_args)
    _log('emit-llvm-args', emit_llvm_args)
    return AnalyzedArgs(llvm_ir_file, is_link, compile_args, emit_llvm_args)

def cc(args):
    _log('fastr:cc', args)
    compiler = None
    sulong = _sulong()
    if sulong:
        analyzed_args = _analyze_args(args)
        if _is_linux():
            rc = sulong.compileWithGCC(analyzed_args.compile_args)
            if rc == 0 and analyzed_args.llvm_ir_file:
                if not analyzed_args.is_link:
                    rc = sulong.compileWithGCC(analyzed_args.emit_llvm_args)
        elif _is_darwin():
            rc = sulong.compileWithClang(analyzed_args.compile_args)
            if rc == 0 and analyzed_args.llvm_ir_file:
                if not analyzed_args.is_link:
                    rc = sulong.compileWithClang(analyzed_args.emit_llvm_args)
        else:
            mx.abort('unsupported platform')
        if rc == 0 and not analyzed_args.is_link and analyzed_args.llvm_ir_file:
            rc = _mem2reg_opt(analyzed_args.llvm_ir_file)
            if rc == 0:
                rc = _embed_ir(analyzed_args.llvm_ir_file)
    else:
        if _is_linux():
            compiler = 'gcc'
        elif _is_darwin():
            compiler = 'clang'
        else:
            mx.abort('unsupported platform')

        rc = mx.run([compiler] + args, nonZeroIsFatal=False)

    return rc

def fc(args):
    _log('fastr:fc', args)
    compiler = None
    sulong = _sulong()
    if sulong:
        analyzed_args = _analyze_args(args, dragonEgg=True)
        rc = mx.run([sulong.getGFortran()] + analyzed_args.compile_args, nonZeroIsFatal=False)
        if rc == 0:
            rc = sulong.dragonEggGFortran(analyzed_args.emit_llvm_args)
            if rc == 0 and analyzed_args.llvm_ir_file:
                # create bitcode from textual IR
                llvm_as = sulong.findLLVMProgram('llvm-as')
                llvm_bc_file = os.path.splitext(analyzed_args.llvm_ir_file)[0] + '.bc'
                rc = mx.run([llvm_as, analyzed_args.llvm_ir_file, '-o', llvm_bc_file])
                rc = _embed_ir(llvm_bc_file)
    else:
        compiler = 'gfortran'
        rc = mx.run([compiler] + args, nonZeroIsFatal=False)

    return rc

def cpp(args):
    _log('fastr:c++', args)
    compiler = None
    sulong = _sulong()
    if sulong:
        analyzed_args = _analyze_args(args)
        if _is_linux():
            rc = sulong.dragonEggGPP(analyzed_args.compile_args)
        elif _is_darwin():
            rc = sulong.compileWithClangPP(analyzed_args.compile_args)
            if rc == 0:
                if analyzed_args.llvm_ir_file:
                    rc = sulong.compileWithClangPP(analyzed_args.emit_llvm_args)
        else:
            mx.abort('unsupported platform')
        if rc == 0 and not analyzed_args.is_link:
            rc = _embed_ir(analyzed_args.llvm_ir_file)
    else:
        compiler = 'g++'
        rc = mx.run([compiler] + args, nonZeroIsFatal=False)

    return rc

def cppcpp(args):
    '''C++ pre-preprocessor'''
    _log('fastr:cpp', args)
    rc = mx.run(['cpp'] + args)
    return rc

def _mem2reg_opt(llvm_ir_file):
    filename = os.path.splitext(llvm_ir_file)[0]
    ext = os.path.splitext(llvm_ir_file)[1]
    opt_filename = filename + '.opt' + ext
    rc = _sulong().opt(['-mem2reg', llvm_ir_file, '-o', opt_filename])
    if rc == 0:
        os.rename(opt_filename, llvm_ir_file)
    return rc

def _embed_ir(llvm_ir_file):
    '''
    Given an llvm_ir_file, generates an assembler file containing the content as a sequence
    of .byte directives, then uses ld to merge that with the original .o file, replacing
    the original .o file.
    '''

    def write_hexbyte(f, b):
        f.write("0x%0.2X" % b)

    def write_int(f, n):
        write_hexbyte(f, n & 255)
        f.write(',')
        write_hexbyte(f, (n >> 8) & 255)
        f.write(',')
        write_hexbyte(f, (n >> 16) & 255)
        f.write(',')
        write_hexbyte(f, (n >> 24) & 255)

    def write_symbol(f, sym):
        write_dot_byte(f)
        write_hexbyte(f, len(sym))
        f.write(', ')
        first = True
        for ch in sym:
            if first:
                first = False
            else:
                f.write(', ')
            write_hexbyte(f, ord(ch))
        f.write('\n')

    def write_dot_byte(f):
        f.write('        .byte ')

    def checkchars(s):
        return s.replace("-", "_")

    # find the exported symbols
    llvm_nm = _sulong().findLLVMProgram("llvm-nm")

    class NMOutputCapture:
        def __init__(self):
            self.exports = []
            self.imports = []

        def __call__(self, data):
            # T name
            s = data.strip()
            if s[0] == 'T':
                self.exports.append(s[2:])
            elif s[0] == 'U':
                self.imports.append(s[2:])

    llvm_nm_out = NMOutputCapture()
    mx.run([llvm_nm, llvm_ir_file], out=llvm_nm_out)

    with open(llvm_ir_file) as f:
        content = bytearray(f.read())
    filename = os.path.splitext(llvm_ir_file)[0]
    ext = os.path.splitext(llvm_ir_file)[1]
    as_file = llvm_ir_file + '.s'
    gsym = "__llvm_" + checkchars(os.path.basename(filename))
    with open(as_file, 'w') as f:
        f.write('        .const\n')
        f.write('        .globl ' + gsym + '\n')
        f.write(gsym + ':\n')
        count = 0
        lenc = len(content)
        write_dot_byte(f)
        # 1 for text, 2 for binary, followed by length
        write_hexbyte(f, 1 if ext == '.ll' else 2)
        f.write(',')
        write_int(f, lenc)
        f.write('\n')
        # now the exported symbols
        write_dot_byte(f)
        write_int(f, len(llvm_nm_out.exports))
        f.write('\n')
        for sym in llvm_nm_out.exports:
            write_symbol(f, sym)
        # now the imported symbols
        write_dot_byte(f)
        write_int(f, len(llvm_nm_out.imports))
        f.write('\n')
        for sym in llvm_nm_out.imports:
            write_symbol(f, sym)
        # now the content
        write_dot_byte(f)
        first = True
        for b in content:
            if first:
                first = False
            else:
                f.write(',')
            write_hexbyte(f, b)
            count = count + 1
            if count % 20 == 0 and count < lenc:
                f.write('\n')
                write_dot_byte(f)
                first = True
        f.write('\n')

    ll_o_file = llvm_ir_file + '.o'
    rc = mx.run(['gcc', '-c', as_file, '-o', ll_o_file], nonZeroIsFatal=False)
    if rc == 0:
        # combine
        o_file = filename + '.o'
        dot_o_file = o_file + '.o'
        os.rename(o_file, dot_o_file)
        rc = mx.run(['ld', '-r', dot_o_file, ll_o_file, '-o', o_file], nonZeroIsFatal=False)
        os.remove(dot_o_file)
        os.remove(as_file)
        os.remove(ll_o_file)
    return rc

def mem2reg(args):
    _mem2reg_opt(args[0])

_commands = {
    'fastr-cc' : [cc, '[options]'],
    'fastr-fc' : [fc, '[options]'],
    'fastr-c++' : [cpp, '[options]'],
    'fastr-cpp' : [cppcpp, '[options]'],
    'mem2reg' : [mem2reg, '[options]'],
}
