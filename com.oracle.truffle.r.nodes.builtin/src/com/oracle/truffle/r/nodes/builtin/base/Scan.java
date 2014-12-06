/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996, Robert Gentleman and Ross Ihaka
 * Copyright (c) 1998-2013, The R Core Team
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import java.io.*;
import java.util.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.Node.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.na.*;

@SuppressWarnings("unused")
@RBuiltin(name = "scan", kind = INTERNAL, parameterNames = {"file", "what", "nmax", "sep", "dec", "quote", "skip", "nlines", "na.strings", "flush", "fill", "strip.white", "quiet", "blank.lines.skip",
                "multi.line", "comment.char", "allowEscapes", "encoding", "skipNull"})
public abstract class Scan extends RBuiltinNode {

    private static final int SCAN_BLOCKSIZE = 1000;
    private static final int NO_COMCHAR = 100000; /* won't occur even in Unicode */

    private final NACheck naCheck = new NACheck();

    @Child private CastToVectorNode castVector;

    private RAbstractVector castVector(VirtualFrame frame, Object value) {
        if (castVector == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castVector = insert(CastToVectorNodeFactory.create(null, false, false, false, false));
        }
        return ((RAbstractVector) castVector.executeObject(frame, value)).materialize();
    }

    private static class LocalData {
        RAbstractStringVector naStrings = null;
        boolean quiet = false;
        String sepchar = null;
        char decchar = '.';
        String quoteset = null;
        int comchar = NO_COMCHAR;
        // connection-related (currently not supported)
        // int ttyflag = 0;
        RConnection con = null;
        // connection-related (currently not supported)
        // boolean wasopen = false;
        boolean escapes = false;
        int save = 0;
        boolean isLatin1 = false;
        boolean isUTF8 = false;
        boolean atStart = false;
        boolean embedWarn = false;
        boolean skipNull = false;
    }

    @CreateCast({"arguments"})
    public RNode[] createCastValue(RNode[] children) {
        RNode file = children[0];
        RNode what = CastToVectorNodeFactory.create(children[1], false, false, false, false);
        RNode nmax = CastToVectorNodeFactory.create(CastIntegerNodeFactory.create(children[2], false, false, false), false, false, false, false);
        RNode sep = CastToVectorNodeFactory.create(children[3], false, false, false, false);
        RNode dec = CastToVectorNodeFactory.create(children[4], false, false, false, false);
        RNode quotes = CastToVectorNodeFactory.create(children[5], false, false, false, false);
        RNode nskip = CastToVectorNodeFactory.create(CastIntegerNodeFactory.create(children[6], false, false, false), false, false, false, false);
        RNode nlines = CastToVectorNodeFactory.create(CastIntegerNodeFactory.create(children[7], false, false, false), false, false, false, false);
        RNode naStrings = CastToVectorNodeFactory.create(children[8], false, false, false, false);
        RNode flush = CastToVectorNodeFactory.create(CastLogicalNodeFactory.create(children[9], false, false, false), false, false, false, false);
        RNode fill = CastToVectorNodeFactory.create(CastLogicalNodeFactory.create(children[10], false, false, false), false, false, false, false);
        RNode stripWhite = CastToVectorNodeFactory.create(children[11], false, false, false, false);
        RNode quiet = CastToVectorNodeFactory.create(CastLogicalNodeFactory.create(children[12], false, false, false), false, false, false, false);
        RNode blSkip = CastToVectorNodeFactory.create(CastLogicalNodeFactory.create(children[13], false, false, false), false, false, false, false);
        RNode multiLine = CastToVectorNodeFactory.create(CastLogicalNodeFactory.create(children[14], false, false, false), false, false, false, false);
        RNode commentChar = CastToVectorNodeFactory.create(children[15], false, false, false, false);
        RNode allowEscapes = CastToVectorNodeFactory.create(CastLogicalNodeFactory.create(children[16], false, false, false), false, false, false, false);
        RNode encoding = CastToVectorNodeFactory.create(children[17], false, false, false, false);
        RNode skipNull = CastToVectorNodeFactory.create(CastLogicalNodeFactory.create(children[18], false, false, false), false, false, false, false);

        return new RNode[]{file, what, nmax, sep, dec, quotes, nskip, nlines, naStrings, flush, fill, stripWhite, quiet, blSkip, multiLine, commentChar, allowEscapes, encoding, skipNull};
    }

    @Specialization
    Object doScan(VirtualFrame frame, RConnection file, RAbstractVector what, RAbstractIntVector nmaxVec, RAbstractVector sepVec, RAbstractVector decVec, RAbstractVector quotesVec,
                    RAbstractIntVector nskipVec, RAbstractIntVector nlinesVec, RAbstractVector naStringsVec, RAbstractLogicalVector flushVec, RAbstractLogicalVector fillVec, RAbstractVector stripVec,
                    RAbstractLogicalVector dataQuietVec, RAbstractLogicalVector blSkipVec, RAbstractLogicalVector multiLineVec, RAbstractVector commentCharVec, RAbstractLogicalVector escapesVec,
                    RAbstractVector encodingVec, RAbstractLogicalVector skipNullVec) {

        LocalData data = new LocalData();

        int nmax = nmaxVec.getLength() == 0 ? RRuntime.INT_NA : nmaxVec.getDataAt(0);

        if (sepVec.getLength() == 0) {
            data.sepchar = null;
        } else if (sepVec.getElementClass() != RString.class) {
            throw RError.error(RError.Message.INVALID_ARGUMENT, "sep");
        }
        // TODO: some sort of character translation happens here?
        String sep = ((RAbstractStringVector) sepVec).getDataAt(0);
        if (sep.length() > 1) {
            throw RError.error(RError.Message.MUST_BE_ONE_BYTE, "'sep' value");
        }
        data.sepchar = sep.length() == 0 ? null : sep.substring(0, 1);

        if (decVec.getLength() == 0) {
            data.decchar = '.';
        } else if (decVec.getElementClass() != RString.class) {
            throw RError.error(RError.Message.INVALID_DECIMAL_SEP);
        }
        // TODO: some sort of character translation happens here?
        String dec = ((RAbstractStringVector) decVec).getDataAt(0);
        if (dec.length() > 1) {
            throw RError.error(RError.Message.MUST_BE_ONE_BYTE, "decimal separator");
        }
        data.decchar = dec.charAt(0);

        if (quotesVec.getLength() == 0) {
            data.quoteset = "";
        } else if (quotesVec.getElementClass() != RString.class) {
            throw RError.error(RError.Message.INVALID_QUOTE_SYMBOL);
        }
        // TODO: some sort of character translation happens here?
        data.quoteset = ((RAbstractStringVector) quotesVec).getDataAt(0);

        int nskip = nskipVec.getLength() == 0 ? RRuntime.INT_NA : nskipVec.getDataAt(0);

        int nlines = nlinesVec.getLength() == 0 ? RRuntime.INT_NA : nlinesVec.getDataAt(0);

        if (naStringsVec.getElementClass() != RString.class) {
            throw RError.error(RError.Message.INVALID_ARGUMENT, "na.strings");
        }
        data.naStrings = (RAbstractStringVector) naStringsVec;

        byte flush = flushVec.getLength() == 0 ? RRuntime.LOGICAL_NA : flushVec.getDataAt(0);

        byte fill = fillVec.getLength() == 0 ? RRuntime.LOGICAL_NA : fillVec.getDataAt(0);

        if (stripVec.getElementClass() != RLogical.class) {
            throw RError.error(RError.Message.INVALID_ARGUMENT, "strip.white");
        }
        if (stripVec.getLength() != 1 && stripVec.getLength() != what.getLength()) {
            throw RError.error(RError.Message.INVALID_LENGTH, "strip.white");
        }
        byte strip = ((RAbstractLogicalVector) stripVec).getDataAt(0);

        data.quiet = dataQuietVec.getLength() == 0 || RRuntime.isNA(dataQuietVec.getDataAt(0)) ? false : dataQuietVec.getDataAt(0) == RRuntime.LOGICAL_TRUE;

        byte blSkip = blSkipVec.getLength() == 0 ? RRuntime.LOGICAL_NA : blSkipVec.getDataAt(0);

        byte multiLine = multiLineVec.getLength() == 0 ? RRuntime.LOGICAL_NA : multiLineVec.getDataAt(0);

        if (commentCharVec.getElementClass() != RString.class || commentCharVec.getLength() != 1) {
            throw RError.error(RError.Message.INVALID_ARGUMENT, "comment.char");
        }
        String commentChar = ((RAbstractStringVector) commentCharVec).getDataAt(0);
        data.comchar = NO_COMCHAR;
        if (commentChar.length() > 1) {
            throw RError.error(RError.Message.INVALID_ARGUMENT, "comment.char");
        } else if (commentChar.length() == 1) {
            data.comchar = commentChar.charAt(0);
        }

        byte escapes = escapesVec.getLength() == 0 ? RRuntime.LOGICAL_NA : escapesVec.getDataAt(0);
        if (RRuntime.isNA(escapes)) {
            throw RError.error(RError.Message.INVALID_ARGUMENT, "allowEscapes");
        }
        data.escapes = escapes != RRuntime.LOGICAL_FALSE;

        if (encodingVec.getElementClass() != RString.class || encodingVec.getLength() != 1) {
            throw RError.error(RError.Message.INVALID_ARGUMENT, "encoding");
        }
        String encoding = ((RAbstractStringVector) encodingVec).getDataAt(0);
        if (encoding.equals("latin1")) {
            data.isLatin1 = true;
        }
        if (encoding.equals("UTF-8")) {
            data.isUTF8 = true;
        }

        byte skipNull = skipNullVec.getLength() == 0 ? RRuntime.LOGICAL_NA : skipNullVec.getDataAt(0);
        if (RRuntime.isNA(skipNull)) {
            throw RError.error(RError.Message.INVALID_ARGUMENT, "skipNull");
        }
        data.skipNull = skipNull != RRuntime.LOGICAL_FALSE;

        if (blSkip == RRuntime.LOGICAL_NA) {
            blSkip = RRuntime.LOGICAL_TRUE;
        }
        if (multiLine == RRuntime.LOGICAL_NA) {
            multiLine = RRuntime.LOGICAL_TRUE;
        }
        if (nskip < 0 || nskip == RRuntime.INT_NA) {
            nskip = 0;
        }
        if (nlines < 0 || nlines == RRuntime.INT_NA) {
            nlines = 0;
        }
        if (nmax < 0 || nmax == RRuntime.INT_NA) {
            nmax = 0;
        }

        // TODO: quite a few more things happen in GNU R around connections
        data.con = file;

        Object result = RNull.instance;
        data.save = 0;

        try {
            if (nskip > 0) {
                data.con.readLines(nskip);
            }
            if (what.getElementClass() == Object.class) {
                return scanFrame(frame, (RList) what, nmax, nlines, flush == RRuntime.LOGICAL_TRUE, fill == RRuntime.LOGICAL_TRUE, strip == RRuntime.LOGICAL_TRUE, blSkip == RRuntime.LOGICAL_TRUE,
                                multiLine == RRuntime.LOGICAL_TRUE, data);
            } else {
                return scanVector(what, nmax, nlines, flush == RRuntime.LOGICAL_TRUE, strip == RRuntime.LOGICAL_TRUE, blSkip == RRuntime.LOGICAL_TRUE, data);
            }

        } catch (IOException x) {
            throw RError.error(RError.Message.CANNOT_READ_CONNECTION);
        }
    }

    private static String[] getQuotedItems(LocalData data, String s) {
        LinkedList<String> items = new LinkedList<>();

        String str = s;
        StringBuilder sb = null;

        while (true) {
            int sepInd;
            if (data.sepchar == null) {
                int blInd = str.indexOf(' ');
                int tabInd = str.indexOf('\t');
                if (blInd == -1) {
                    sepInd = tabInd;
                } else if (tabInd == -1) {
                    sepInd = blInd;
                } else {
                    sepInd = Math.min(blInd, tabInd);
                }
            } else {
                assert data.sepchar.length() == 1;
                sepInd = str.indexOf(data.sepchar.charAt(0));
            }

            int quoteInd = str.indexOf(data.quoteset.charAt(0));
            char quoteChar = data.quoteset.charAt(0);
            for (int i = 1; i < data.quoteset.length(); i++) {
                int ind = str.indexOf(data.quoteset.charAt(i));
                if (ind >= 0 && (quoteInd == -1 || (quoteInd >= 0 && ind < quoteInd))) {
                    // update quoteInd if either the new index is smaller or the previous one (for
                    // another separator) was not found
                    quoteInd = ind;
                    quoteChar = data.quoteset.charAt(i);
                }
            }

            if (sb == null) {
                // first iteration
                if (quoteInd == -1) {
                    // no quotes at all
                    return data.sepchar == null ? s.split("\\s+") : s.split(data.sepchar);
                } else {
                    sb = new StringBuilder();
                }
            }

            if (sepInd == -1 && quoteInd == -1) {
                // no more separators and no more quotes - add the last item and return
                sb.append(str);
                items.add(sb.toString());
                break;
            }

            if (quoteInd >= 0 && (sepInd == -1 || (sepInd >= 0 && quoteInd < sepInd))) {
                // quote character was found before the separator character - everything from the
                // beginning of str up to the end of quote becomes part of this item
                sb.append(str.substring(0, quoteInd));
                int nextQuoteInd = str.indexOf(quoteChar, quoteInd + 1);
                sb.append(str.substring(quoteInd + 1, nextQuoteInd));
                str = str.substring(nextQuoteInd + 1, str.length());
            } else {
                assert sepInd >= 0;
                // everything from the beginning of str becomes part of this time and item
                // processing is completed (also eat up separators)
                String[] tuple = data.sepchar == null ? str.split("\\s+", 2) : str.split(data.sepchar, 2);
                assert tuple.length == 2;
                sb.append(tuple[0]);
                str = tuple[1];
                items.add(sb.toString());
                sb = new StringBuilder();
            }
        }

        return items.toArray(new String[items.size()]);
    }

    private static String[] getItems(LocalData data, boolean blSkip) throws IOException {
        while (true) {
            String[] str = data.con.readLines(1);
            if (str == null || str.length == 0) {
                return null;
            } else {
                String s = str[0].trim();
                if (blSkip && s.length() == 0) {
                    continue;
                } else {
                    if (data.quoteset.length() == 0) {
                        return data.sepchar == null ? s.split("\\s+") : s.split(data.sepchar);
                    } else {
                        return getQuotedItems(data, s);
                    }
                }
            }
        }

    }

    private void fillEmpty(int from, int to, int records, RList list, LocalData data) {
        for (int i = from; i < to; i++) {
            RVector vec = (RVector) list.getDataAt(i);
            vec.updateDataAtAsObject(records, extractItem(vec, "", data), naCheck);
        }
    }

    @TruffleBoundary
    private RVector scanFrame(VirtualFrame frame, RList what, int maxRecords, int maxLines, boolean flush, boolean fill, boolean stripWhite, boolean blSkip, boolean multiLine, LocalData data)
                    throws IOException {

        int nc = what.getLength();
        if (nc == 0) {
            throw RError.error(RError.Message.EMPTY_WHAT);
        }
        int blockSize = maxRecords > 0 ? maxRecords : (maxLines > 0 ? maxLines : SCAN_BLOCKSIZE);

        RList list = RDataFactory.createList(new Object[nc]);
        for (int i = 0; i < nc; i++) {
            if (what.getDataAt(i) == RNull.instance) {
                throw RError.error(RError.Message.INVALID_ARGUMENT, "what");
            } else {
                RAbstractVector vec = castVector(frame, what.getDataAt(i));
                list.updateDataAt(i, vec.createEmptySameType(blockSize, RDataFactory.COMPLETE_VECTOR), null);
            }
        }
        list.setNames(what.getNames());

        naCheck.enable(true);

        int n = 0;
        int lines = 0;
        int records = 0;
        while (true) {
            // TODO: does not do any fancy stuff, like handling comments
            String[] strItems = getItems(data, blSkip);
            if (strItems == null) {
                break;
            }

            boolean done = false;
            for (int i = 0; i < Math.max(nc, strItems.length); i++) {

                if (n == strItems.length) {
                    if (fill) {
                        fillEmpty(n, nc, records, list, data);
                        records++;
                        n = 0;
                        break;
                    } else if (!multiLine) {
                        throw RError.error(getEncapsulatingSourceSection(), RError.Message.LINE_ELEMENTS, lines + 1, nc);
                    } else {
                        strItems = getItems(data, blSkip);
                        // Checkstyle: stop modified control variable check
                        i = 0;
                        // Checkstyle: resume modified control variable check
                        if (strItems == null) {
                            done = true;
                            break;
                        }
                    }
                }
                Object item = extractItem((RAbstractVector) list.getDataAt(n), strItems[i], data);

                if (records == blockSize) {
                    // enlarge the vector
                    blockSize = blockSize * 2;
                    for (int j = 0; j < nc; j++) {
                        RVector vec = (RVector) list.getDataAt(j);
                        vec = vec.copyResized(blockSize, false);
                        list.updateDataAt(j, vec, null);
                    }
                }

                RVector vec = (RVector) list.getDataAt(n);
                vec.updateDataAtAsObject(records, item, naCheck);
                n++;
                if (n == nc) {
                    records++;
                    n = 0;
                    if (records == maxRecords) {
                        done = true;
                        break;
                    }
                    if (flush) {
                        break;
                    }
                }
            }
            if (done) {
                break;
            }
            lines++;
            if (lines == maxLines) {
                break;
            }

        }

        if (n > 0 && n < nc) {
            if (!fill) {
                RError.warning(getEncapsulatingSourceSection(), RError.Message.ITEMS_NOT_MULTIPLE);
            }
            fillEmpty(n, nc, records, list, data);
            records++;
        }

        if (!data.quiet) {
            RContext.getInstance().getConsoleHandler().printf("Read %d record%s\n", records, (records == 1) ? "" : "s");
        }
        // trim vectors if necessary
        for (int i = 0; i < nc; i++) {
            RVector vec = (RVector) list.getDataAt(i);
            if (vec.getLength() > records) {
                list.updateDataAt(i, vec.copyResized(records, false), null);
            }
        }

        return list;
    }

    @TruffleBoundary
    private RVector scanVector(RAbstractVector what, int maxItems, int maxLines, boolean flush, boolean stripWhite, boolean blSkip, LocalData data) throws IOException {
        int blockSize = maxItems > 0 ? maxItems : SCAN_BLOCKSIZE;
        RVector vec = what.createEmptySameType(blockSize, RDataFactory.COMPLETE_VECTOR);
        naCheck.enable(true);

        int n = 0;
        int lines = 0;
        while (true) {
            // TODO: does not do any fancy stuff, like handling comments
            String[] strItems = getItems(data, blSkip);
            if (strItems == null) {
                break;
            }

            boolean done = false;
            for (int i = 0; i < strItems.length; i++) {

                Object item = extractItem(what, strItems[i], data);

                if (n == blockSize) {
                    // enlarge the vector
                    blockSize = blockSize * 2;
                    vec = vec.copyResized(blockSize, false);
                }

                vec.updateDataAtAsObject(n, item, naCheck);
                n++;
                if (n == maxItems) {
                    done = true;
                    break;
                }
            }
            if (done) {
                break;
            }
            lines++;
            if (lines == maxLines) {
                break;
            }

        }
        if (!data.quiet) {
            RContext.getInstance().getConsoleHandler().printf("Read %d item%s\n", n, (n == 1) ? "" : "s");
        }
        // trim vector if necessary
        return vec.getLength() > n ? vec.copyResized(n, false) : vec;
    }

    // If mode = 0 use for numeric fields where "" is NA
    // If mode = 1 use for character fields where "" is verbatim unless
    // na.strings includes ""
    private static boolean isNaString(String buffer, int mode, LocalData data) {
        int i;

        if (mode == 0 && buffer.length() == 0) {
            return true;
        }
        for (i = 0; i < data.naStrings.getLength(); i++) {
            if (data.naStrings.getDataAt(i).equals(buffer)) {
                return true;
            }
        }
        return false;
    }

    private static Object extractItem(RAbstractVector what, String buffer, LocalData data) {
        if (what.getElementClass() == RLogical.class) {
            if (isNaString(buffer, 0, data)) {
                return RRuntime.LOGICAL_NA;
            } else {
                return RRuntime.string2logicalNoCheck(buffer);
            }
        }
        if (what.getElementClass() == RInt.class) {
            if (isNaString(buffer, 0, data)) {
                return RRuntime.INT_NA;
            } else {
                return RRuntime.string2intNoCheck(buffer);
            }
        }

        if (what.getElementClass() == RDouble.class) {
            if (isNaString(buffer, 0, data)) {
                return RRuntime.DOUBLE_NA;
            } else {
                return RRuntime.string2doubleNoCheck(buffer);
            }
        }

        if (what.getElementClass() == RComplex.class) {
            if (isNaString(buffer, 0, data)) {
                return RRuntime.createComplexNA();
            } else {
                return RRuntime.string2complexNoCheck(buffer);
            }
        }

        if (what.getElementClass() == RString.class) {
            if (isNaString(buffer, 1, data)) {
                return RRuntime.STRING_NA;
            } else {
                return buffer;
            }
        }

        if (what.getElementClass() == RRaw.class) {
            if (isNaString(buffer, 0, data)) {
                return RDataFactory.createRaw((byte) 0);
            } else {
                return RRuntime.string2raw(buffer);
            }
        }

        throw RInternalError.shouldNotReachHere();
    }
}
