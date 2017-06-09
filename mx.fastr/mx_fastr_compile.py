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
"""
A wrapper for the C/C++/Fortran compilers that optionally handles the generation of LLVM bitcode.
When not running with sulong is simply forwards to the default compiler for the platform.
When running under sulong, it uses sulong to do two compilations; first to generate object code
and second to generate LLVM bitcode.
"""
import os, sys, zipfile
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
    return os.path.join(mx_fastr._fastr_suite.dir, 'com.oracle.truffle.r.native', 'fficall', 'src', 'truffle_llvm', 'llvm_dummy.c')

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
        rc = 0
        if analyzed_args.is_link:
            rc = _create_bc_lib(args)
        else:
            if analyzed_args.llvm_ir_file:
                rc = sulong.compileWithClang(analyzed_args.emit_llvm_args)
        if rc == 0 and not analyzed_args.is_link and analyzed_args.llvm_ir_file:
            rc = _mem2reg_opt(analyzed_args.llvm_ir_file)
            _fake_obj(analyzed_args.llvm_ir_file.replace('.bc', '.o'))
    else:
        compiler = 'clang'
        rc = mx.run([compiler] + args, nonZeroIsFatal=False)

    return rc

def fc(args):
    _log('fastr:fc', args)
    compiler = None
    sulong = _sulong()
    if sulong:
        analyzed_args = _analyze_args(args, dragonEgg=True)
        rc = 0
        rc = sulong.dragonEggGFortran(analyzed_args.emit_llvm_args)
        if rc == 0 and analyzed_args.llvm_ir_file:
            # create bitcode from textual IR
            llvm_as = sulong.findLLVMProgram('llvm-as')
            llvm_bc_file = os.path.splitext(analyzed_args.llvm_ir_file)[0] + '.bc'
            rc = mx.run([llvm_as, analyzed_args.llvm_ir_file, '-o', llvm_bc_file])
            os.remove(analyzed_args.llvm_ir_file)
            rc = _mem2reg_opt(llvm_bc_file)
            _fake_obj(llvm_bc_file.replace('.bc', '.o'))
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
    _log('mem2reg', llvm_ir_file)
    filename = os.path.splitext(llvm_ir_file)[0]
    ext = os.path.splitext(llvm_ir_file)[1]
    opt_filename = filename + '.opt' + ext
    rc = _sulong().opt(['-mem2reg', llvm_ir_file, '-o', opt_filename])
    if rc == 0:
        os.rename(opt_filename, llvm_ir_file)
    return rc

def _fake_obj(name):
    '''create an empty object file to keep make happy'''
#    print 'creating ' + name
    open(name, 'w').close()

def mem2reg(args):
    _mem2reg_opt(args[0])

def _create_bc_lib(args):
    i = 0
    bcfiles = []
    while i < len(args):
        arg = args[i]
        if arg == '-o' :
            # library file
            i = i + 1
            lib = args[i]
        else:
            if '.o' in arg:
                bcfiles.append(arg.replace('.o', '.bc'))
        i = i + 1

    with zipfile.ZipFile(lib, 'w') as arc:
        for bcfile in bcfiles:
            arc.write(bcfile, os.path.basename(bcfile).replace('.bc', ''))
    return 0

_commands = {
    'fastr-cc' : [cc, '[options]'],
    'fastr-fc' : [fc, '[options]'],
    'fastr-c++' : [cpp, '[options]'],
    'fastr-cpp' : [cppcpp, '[options]'],
    'mem2reg' : [mem2reg, '[options]'],
}
