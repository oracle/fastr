/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.ffi.impl.llvm;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

import com.oracle.truffle.r.runtime.RInternalError;

/**
 * (Limited) Access to Mach_O 64-bit format files. See /usr/include/mach-o/*.h for source. Note that
 * a file may (unusually) contain multiple binaries for different architectures, see
 * /usr/include/mach-o/fat.h. Such a file is called a universal binary file, (cf an archive file).
 *
 */
@SuppressWarnings("unused")
final class MachOAccess implements AutoCloseable {
    private final RandomAccessFile raf;
    private final Header header;
    private final LoadCommand[] loadCommands;

    private MachOAccess(RandomAccessFile raf) throws IOException {
        this.raf = raf;
        this.header = new Header();
        this.loadCommands = getLoadCommands();
    }

    static LLVM_IR[] getLLVMIR(String path) throws IOException {
        try (MachOAccess ma = new MachOAccess(new RandomAccessFile(path, "r"))) {
            return ma.getLLVMIR();
        }
    }

    /**
     * Return an array of {@link LLVM_IR} instances corresponding to the "modules" in the library,
     * or {@code null} of there none.
     */
    private LLVM_IR[] getLLVMIR() throws IOException {
        SymTabLoadCommand symtab = null;
        for (LoadCommand lc : loadCommands) {
            if (lc.cmd == LC_TYPE.LC_SYMTAB) {
                symtab = (SymTabLoadCommand) lc;
                break;
            }
        }
        assert symtab != null;
        ArrayList<LLVM_IR> list = new ArrayList<>();
        NList64[] syms = symtab.getSymbolTable();
        for (NList64 sym : syms) {
            String name = symtab.getSymbolName(sym);
            if (name.startsWith("__llvm_")) {
                String module = name.substring(7);
                getSection(loadCommands, sym.sect);
                raf.seek(sym.value);
                int type = raf.read();
                int len = readInt();
                byte[] buf = new byte[len];
                // exported symbols
                String[] exports = readXXPorts();
                // imported symbols
                String[] imports = readXXPorts();
                raf.read(buf);
                LLVM_IR ir;
                if (type == LLVM_IR.TEXT_CODE) {
                    ir = new LLVM_IR.Text(module, new String(buf), exports, imports);
                } else if (type == LLVM_IR.BINARY_CODE) {
                    ir = new LLVM_IR.Binary(module, buf, exports, imports);
                } else {
                    throw RInternalError.shouldNotReachHere();
                }
                list.add(ir);
            }
        }
        if (list.size() == 0) {
            return null;
        } else {
            LLVM_IR[] result = new LLVM_IR[list.size()];
            list.toArray(result);
            return result;
        }
    }

    String[] readXXPorts() throws IOException {
        int numxxports = readInt();
        String[] xxports = new String[numxxports];
        for (int i = 0; i < numxxports; i++) {
            int xxportLen = raf.read();
            byte[] xxportBuf = new byte[xxportLen];
            for (int j = 0; j < xxportLen; j++) {
                xxportBuf[j] = (byte) raf.read();
            }
            xxports[i] = new String(xxportBuf);
        }
        return xxports;
    }

    @Override
    public void close() throws IOException {
        raf.close();
    }

    private final class Header implements Cloneable {
        private static final int FAT_MAGIC = 0xcafebabe;
        private final int magic;
        private final int cputype;
        private final int cpusubtype;
        private final int filetype;
        private final int ncmds;
        private final int sizeofcmds;
        private final int flags;
        private final int reserved;

        private Header() throws IOException {
            this.magic = raf.readInt();
            assert magic != FAT_MAGIC;
            cputype = readInt();
            cpusubtype = readInt();
            filetype = readInt();
            ncmds = readInt();
            sizeofcmds = readInt();
            flags = readInt();
            reserved = readInt();
        }
    }

    private enum LC_TYPE {
        LC_SYMTAB(0x2),
        LC_THREAD(0x4),
        LC_DYSYMTAB(0xb),
        LC_LOAD_DYLIB(0xc),
        LC_ID_DYLIB(0xd),
        LC_SUB_FRAMEWORK(0x12),
        LC_LOAD_WEAK_DYLIB(0x18),
        LC_SEGMENT_64(0x19),
        LC_UUID(0x1b),
        LC_RPATH(0x1C),
        LC_DYLD_INFO(0x22),
        LC_VERSION_MIN_MACOSX(0x24),
        LC_FUNCTION_STARTS(0x26),
        LC_DATA_IN_CODE(0x29),
        LC_SOURCE_VERSION(0x2A),
        LC_USER(0x32);

        private int code;

        LC_TYPE(int code) {
            this.code = code;
        }

        static int getCode(int codeIn) {
            return codeIn & ~LoadCommand.LC_REQ_DYLD;
        }

        static LC_TYPE getType(int code) {
            for (LC_TYPE lct : LC_TYPE.values()) {
                if (code == lct.code) {
                    return lct;
                }
            }
            assert false : "unknown load cmd: " + code;
            return null;
        }
    }

    /**
     * Common base class for all Mach-O load command types.
     */
    private class LoadCommand {
        private static final int LC_REQ_DYLD = 0x80000000;

        private long cmdFileOffset;
        private final int code;
        private final LC_TYPE cmd;
        private final int cmdsize;

        protected LoadCommand(int index) throws IOException {
            cmdFileOffset = raf.getFilePointer();
            this.code = readInt();
            this.cmd = LC_TYPE.getType(LC_TYPE.getCode(this.code));
            this.cmdsize = readInt();
        }

        protected LoadCommand(int index, LC_TYPE cmd, int cmdsize) {
            this.cmd = cmd;
            this.code = cmd.code;
            this.cmdsize = cmdsize;
        }

        private String typeName() {
            return cmd.name();
        }
    }

    /**
     * Reads a load command structure starting at the current file position, invoking the
     * appropriate subclass {@code read} command, based on the {@code cmd} field. Leaves the file
     * pointer at the next load command (if any).
     *
     * @return instance of the appropriate subclass for discovered command type
     * @throws IOException
     */
    private LoadCommand readNextLoadCommand(int index) throws IOException {
        LoadCommand result = null;
        final long ptr = raf.getFilePointer();
        final LC_TYPE cmd = LC_TYPE.getType(LC_TYPE.getCode(readInt()));
        final int cmdsize = readInt();
        /* The LoadCommand class reads the two prior fields again. */
        raf.seek(ptr);
        switch (cmd) {
            case LC_SEGMENT_64:
                result = new Segment64LoadCommand(index);
                break;
            case LC_SYMTAB:
                result = new SymTabLoadCommand(index);
                break;
            default:
                result = new LoadCommand(index);
                break;
        }
        // skip over entire command
        raf.seek(ptr + cmdsize);
        return result;
    }

    private LoadCommand[] getLoadCommands() throws IOException {
        LoadCommand[] result = new LoadCommand[header.ncmds];
        for (int i = 0; i < header.ncmds; i++) {
            result[i] = readNextLoadCommand(i);
        }
        return result;
    }

    private final class Segment64LoadCommand extends LoadCommand {
        private final String segName;
        private final long vmaddr;
        private final long vmsize;
        private final long fileoff;
        private final long filesize;
        private final int maxprot;
        private final int initprot;
        private final int nsects;
        private final int flags;
        private final Section64[] sections;

        private Segment64LoadCommand(int index) throws IOException {
            super(index);
            final byte[] segname = new byte[16];
            for (int i = 0; i < 16; i++) {
                segname[i] = raf.readByte();
            }
            segName = new String(segname);
            vmaddr = readLong();
            vmsize = readLong();
            fileoff = readLong();
            filesize = readLong();
            maxprot = readInt();
            initprot = readInt();
            nsects = readInt();
            flags = readInt();
            sections = new Section64[nsects];
            for (int i = 0; i < nsects; i++) {
                sections[i] = new Section64(this);
            }
        }
    }

    private final class Section64 {
        private final String sectname;
        private final String segname;
        private final long addr;
        private final long size;
        private final int offset;
        private final int align;
        private final int reloff;
        private final int nreloc;
        private final int flags;
        private final int reserved1;
        private final int reserved2;
        private final int reserved3;

        private Section64(Segment64LoadCommand segment64) throws IOException {
            sectname = readName();
            segname = readName();
            addr = readLong();
            size = readLong();
            offset = readInt();
            align = readInt();
            reloff = readInt();
            nreloc = readInt();
            flags = readInt();
            reserved1 = readInt();
            reserved2 = readInt();
            reserved3 = readInt();
        }

        private String readName() throws IOException {
            byte[] nameBytes = new byte[16];
            int length = 0;
            for (int i = 0; i < nameBytes.length; i++) {
                nameBytes[i] = raf.readByte();
                if (nameBytes[i] != 0) {
                    length++;
                }
            }
            return new String(nameBytes, 0, length);
        }

        private boolean isText() {
            return segname.equals("__TEXT");
        }
    }

    private class SymTabLoadCommand extends LoadCommand {
        private final int symoff;
        private final int nsyms;
        private final int stroff;
        private final int strsize;
        /**
         * Lazily created string table.
         */
        private byte[] stringTable;
        /**
         * Lazily created symbol table.
         */
        private NList64[] symbolTable;

        SymTabLoadCommand(int index) throws IOException {
            super(index);
            symoff = readInt();
            nsyms = readInt();
            stroff = readInt();
            strsize = readInt();
        }

        private NList64[] getSymbolTable() throws IOException {
            if (symbolTable != null) {
                return symbolTable;
            }
            stringTable = new byte[strsize];
            raf.seek(stroff);
            for (int i = 0; i < strsize; i++) {
                stringTable[i] = raf.readByte();
            }
            symbolTable = new NList64[nsyms];
            raf.seek(symoff);
            for (int i = 0; i < nsyms; i++) {
                symbolTable[i] = new NList64();
            }
            return symbolTable;
        }

        private String getSymbolName(NList64 nlist64) {
            String symbol = "";
            if (nlist64.strx != 0) {
                byte sb = stringTable[nlist64.strx];
                int sl = 0;
                while (sb != 0) {
                    sb = stringTable[nlist64.strx + sl];
                    sl++;
                }
                if (sl > 0) {
                    symbol = new String(stringTable, nlist64.strx, sl - 1);
                }
            }
            return symbol;
        }
    }

    private class NList64 {
        private final int strx;
        private final byte type;
        private final byte sect;
        private final short desc;
        private final long value;

        NList64() throws IOException {
            strx = readInt();
            type = raf.readByte();
            sect = raf.readByte();
            desc = readShort();
            value = readLong();
        }

        void print() {

        }
    }

    /**
     * Locates a given section within a given array of load commands. Sections are numbered from 1
     * as they occur within SEGMENT_64 commands.
     *
     * @param loadCommands
     * @param sectToFind
     */
    private static Section64 getSection(LoadCommand[] loadCommands, int sectToFind) {
        int sect = 1;
        for (int i = 0; i < loadCommands.length; i++) {
            if (loadCommands[i].cmd == LC_TYPE.LC_SEGMENT_64) {
                Segment64LoadCommand slc = (Segment64LoadCommand) loadCommands[i];
                if (sectToFind < sect + slc.nsects) {
                    return slc.sections[sectToFind - sect];
                }
                sect += slc.nsects;
            }
        }
        return null;
    }

    private short readShort() throws IOException {
        final int b1 = raf.read();
        final int b2 = raf.read();
        return (short) (((b2 << 8) | b1) & 0xFFFF);
    }

    private int readInt() throws IOException {
        final int b1 = raf.read();
        final int b2 = raf.read();
        final int b3 = raf.read();
        final int b4 = raf.read();
        return (b4 << 24) | (b3 << 16) | (b2 << 8) | b1;
    }

    private long readLong() throws IOException {
        final long lw = readInt();
        final long hw = readInt();
        return hw << 32 | (lw & 0xFFFFFFFFL);
    }
}
