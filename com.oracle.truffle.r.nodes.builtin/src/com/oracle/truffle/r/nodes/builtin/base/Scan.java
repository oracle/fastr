/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996, Robert Gentleman and Ross Ihaka
 * Copyright (c) 1998-2013, The R Core Team
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.charAt0;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.constant;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.length;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.lengthLte;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.logicalValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.lt;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.singleElement;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.IO;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import java.io.IOException;
import java.util.LinkedList;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetNamesAttributeNode;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.unary.CastToVectorNode;
import com.oracle.truffle.r.nodes.unary.CastToVectorNodeGen;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.conn.RConnection;
import com.oracle.truffle.r.runtime.conn.StdConnections;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDouble;
import com.oracle.truffle.r.runtime.data.RInteger;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RLogical;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RRaw;
import com.oracle.truffle.r.runtime.data.RString;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

@RBuiltin(name = "scan", kind = INTERNAL, parameterNames = {"file", "what", "nmax", "sep", "dec", "quote", "skip", "nlines", "na.strings", "flush", "fill", "strip.white", "quiet", "blank.lines.skip",
                "multi.line", "comment.char", "allowEscapes", "encoding", "skipNull"}, behavior = IO)
public abstract class Scan extends RBuiltinNode {

    private static final int SCAN_BLOCKSIZE = 1000;
    private static final int NO_COMCHAR = 100000; /* won't occur even in Unicode */

    private final NACheck naCheck = NACheck.create();
    private final BranchProfile errorProfile = BranchProfile.create();
    @Child private GetNamesAttributeNode getNames = GetNamesAttributeNode.create();

    @Child private CastToVectorNode castVector;

    private RAbstractVector castVector(Object value) {
        if (castVector == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castVector = insert(CastToVectorNodeGen.create(false));
        }
        return ((RAbstractVector) castVector.execute(value)).materialize();
    }

    @SuppressWarnings("unused")
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

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.arg("file").defaultError(Message.INVALID_CONNECTION).mustNotBeNull().asIntegerVector().findFirst();

        casts.arg("what").asVector();

        casts.arg("nmax").asIntegerVector().findFirst(0).notNA(0).mapIf(lt(0), constant(0));

        casts.arg("sep").allowNull().mustBe(stringValue()).asStringVector().findFirst("").mustBe(lengthLte(1), RError.Message.MUST_BE_ONE_BYTE, "'sep' value");

        casts.arg("dec").allowNull().defaultError(RError.Message.INVALID_DECIMAL_SEP).mustBe(stringValue()).asStringVector().findFirst(".").mustBe(length(1),
                        RError.Message.MUST_BE_ONE_BYTE, "'sep' value");

        casts.arg("quote").defaultError(RError.Message.INVALID_QUOTE_SYMBOL).mapNull(constant("")).mustBe(stringValue()).asStringVector().findFirst("");

        casts.arg("skip").asIntegerVector().findFirst(0).notNA(0).mapIf(lt(0), constant(0));

        casts.arg("nlines").asIntegerVector().findFirst(0).notNA(0).mapIf(lt(0), constant(0));

        casts.arg("na.strings").mustBe(stringValue());

        casts.arg("flush").asLogicalVector().findFirst(RRuntime.LOGICAL_NA).map(toBoolean());

        casts.arg("fill").asLogicalVector().findFirst(RRuntime.LOGICAL_NA).map(toBoolean());

        casts.arg("strip.white").mustBe(logicalValue());

        casts.arg("quiet").asLogicalVector().findFirst(RRuntime.LOGICAL_FALSE).notNA(RRuntime.LOGICAL_FALSE).map(toBoolean());

        casts.arg("blank.lines.skip").asLogicalVector().findFirst(RRuntime.LOGICAL_TRUE).notNA(RRuntime.LOGICAL_TRUE).map(toBoolean());

        casts.arg("multi.line").asLogicalVector().findFirst(RRuntime.LOGICAL_TRUE).notNA(RRuntime.LOGICAL_TRUE).map(toBoolean());

        casts.arg("comment.char").mustBe(stringValue()).asStringVector().mustBe(singleElement()).findFirst().mustBe(lengthLte(1)).map(charAt0(RRuntime.INT_NA)).notNA(NO_COMCHAR);

        casts.arg("allowEscapes").asLogicalVector().findFirst().notNA().map(toBoolean());

        casts.arg("encoding").mustBe(stringValue()).asStringVector().mustBe(singleElement()).findFirst();

        casts.arg("skipNull").asLogicalVector().findFirst().notNA().map(toBoolean());

    }

    @Specialization
    @TruffleBoundary
    protected Object doScan(int file, RAbstractVector what, int nmax, String sep, String dec, String quotes, int nskip,
                    int nlines, RAbstractStringVector naStringsVec, boolean flush, boolean fill, RAbstractLogicalVector stripVec,
                    boolean quiet, boolean blSkip, boolean multiLine, int commentChar, boolean escapes,
                    String encoding, boolean skipNull) {

        LocalData data = new LocalData();

        // TODO: some sort of character translation happens here?
        data.sepchar = sep.isEmpty() ? null : sep.substring(0, 1);

        // TODO: some sort of character translation happens here?
        data.decchar = dec.charAt(0);

        // TODO: some sort of character translation happens here?
        data.quoteset = quotes;

        data.naStrings = naStringsVec;

        if (stripVec.getLength() != 1 && stripVec.getLength() != what.getLength()) {
            errorProfile.enter();
            throw RError.error(this, RError.Message.INVALID_LENGTH, "strip.white");
        }
        byte strip = stripVec.getDataAt(0);

        data.quiet = quiet;

        data.comchar = commentChar;

        data.escapes = escapes;

        if (encoding.equals("latin1")) {
            data.isLatin1 = true;
        }
        if (encoding.equals("UTF-8")) {
            data.isUTF8 = true;
        }

        data.skipNull = skipNull;

        // TODO: quite a few more things happen in GNU R around connections
        data.con = RConnection.fromIndex(file);

        data.save = 0;

        try (RConnection openConn = data.con.forceOpen("r")) {
            if (nskip > 0) {
                openConn.readLines(nskip, true, skipNull);
            }
            if (what instanceof RList) {
                return scanFrame((RList) what, nmax, nlines, flush, fill, strip == RRuntime.LOGICAL_TRUE, blSkip, multiLine, data);
            } else {
                return scanVector(what, nmax, nlines, flush, strip == RRuntime.LOGICAL_TRUE, blSkip, data);
            }
        } catch (IOException x) {
            throw RError.error(this, RError.Message.CANNOT_READ_CONNECTION);
        }
    }

    private static int getFirstQuoteInd(String str, char sepChar) {
        int quoteInd = str.indexOf(sepChar);
        if (quoteInd >= 0) {
            if (quoteInd == 0 || str.charAt(quoteInd - 1) == ' ' || str.charAt(quoteInd - 1) == '\t') {
                // it's a quote character if it starts the string or is preceded by a blank space
                return quoteInd;
            }
        }
        return -1;
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

            int quoteInd = getFirstQuoteInd(str, data.quoteset.charAt(0));
            char quoteChar = data.quoteset.charAt(0);
            for (int i = 1; i < data.quoteset.length(); i++) {
                int ind = getFirstQuoteInd(str, data.quoteset.charAt(i));
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
            String[] str = data.con.readLines(1, true, false);
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
            RVector<?> vec = (RVector<?>) list.getDataAt(i);
            vec.updateDataAtAsObject(records, extractItem(vec, "", data), naCheck);
        }
    }

    private RVector<?> scanFrame(RList what, int maxRecords, int maxLines, boolean flush, boolean fill, @SuppressWarnings("unused") boolean stripWhite, boolean blSkip, boolean multiLine,
                    LocalData data)
                    throws IOException {

        int nc = what.getLength();
        if (nc == 0) {
            throw RError.error(this, RError.Message.EMPTY_WHAT);
        }
        int blockSize = maxRecords > 0 ? maxRecords : (maxLines > 0 ? maxLines : SCAN_BLOCKSIZE);

        RList list = RDataFactory.createList(new Object[nc]);
        for (int i = 0; i < nc; i++) {
            if (what.getDataAt(i) == RNull.instance) {
                errorProfile.enter();
                throw RError.error(this, RError.Message.INVALID_ARGUMENT, "what");
            } else {
                RAbstractVector vec = castVector(what.getDataAt(i));
                list.updateDataAt(i, vec.createEmptySameType(blockSize, RDataFactory.COMPLETE_VECTOR), null);
            }
        }
        list.setNames(getNames.getNames(what));

        naCheck.enable(true);

        return scanFrameInternal(maxRecords, maxLines, flush, fill, blSkip, multiLine, data, nc, blockSize, list);
    }

    @TruffleBoundary
    private RVector<?> scanFrameInternal(int maxRecords, int maxLines, boolean flush, boolean fill, boolean blSkip, boolean multiLine, LocalData data, int nc, int initialBlockSize, RList list)
                    throws IOException {
        int blockSize = initialBlockSize;
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
                        throw RError.error(this, RError.Message.LINE_ELEMENTS, lines + 1, nc);
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
                        RVector<?> vec = (RVector<?>) list.getDataAt(j);
                        vec = vec.copyResized(blockSize, false);
                        list.updateDataAt(j, vec, null);
                    }
                }

                RVector<?> vec = (RVector<?>) list.getDataAt(n);
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
                RError.warning(this, RError.Message.ITEMS_NOT_MULTIPLE);
            }
            fillEmpty(n, nc, records, list, data);
            records++;
        }

        if (!data.quiet) {
            String s = String.format("Read %d record%s", records, (records == 1) ? "" : "s");
            StdConnections.getStdout().writeString(s, true);
        }
        // trim vectors if necessary
        for (int i = 0; i < nc; i++) {
            RVector<?> vec = (RVector<?>) list.getDataAt(i);
            if (vec.getLength() > records) {
                list.updateDataAt(i, vec.copyResized(records, false), null);
            }
        }

        return list;
    }

    @TruffleBoundary
    private RVector<?> scanVector(RAbstractVector what, int maxItems, int maxLines, @SuppressWarnings("unused") boolean flush, @SuppressWarnings("unused") boolean stripWhite, boolean blSkip,
                    LocalData data) throws IOException {
        int blockSize = maxItems > 0 ? maxItems : SCAN_BLOCKSIZE;
        RVector<?> vec = what.createEmptySameType(blockSize, RDataFactory.COMPLETE_VECTOR);
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
            String s = String.format("Read %d item%s", n, (n == 1) ? "" : "s");
            StdConnections.getStdout().writeString(s, true);
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
        if (what.getElementClass() == RInteger.class) {
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
