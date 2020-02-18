/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.runtime.data;

import java.util.Iterator;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.library.ExportMessage.Ignore;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RDeparse;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RSrcref;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RDataFactory.BaseVectorFactory;
import com.oracle.truffle.r.runtime.data.RPairListFactory.RPairListSnapshotNodeGen;
import com.oracle.truffle.r.runtime.data.RSharingAttributeStorage.Shareable;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.nodes.FastPathVectorAccess.FastPathFromListAccess;
import com.oracle.truffle.r.runtime.data.nodes.SlowPathVectorAccess.SlowPathFromListAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.gnur.SEXPTYPE;
import com.oracle.truffle.r.runtime.interop.R2Foreign;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxCall;
import com.oracle.truffle.r.runtime.nodes.RSyntaxConstant;
import com.oracle.truffle.r.runtime.nodes.RSyntaxElement;
import com.oracle.truffle.r.runtime.nodes.RSyntaxFunction;
import com.oracle.truffle.r.runtime.nodes.RSyntaxLookup;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

/**
 * RPairList objects can represent both "normal" pairlists (which are rarely used from R directly)
 * and language objects. They are unified in one implementation class because native code often
 * changes the type of these in-place.
 *
 * A pair-list consists of the data in {@link #car()}, link to the next cell in {@link #cdr()},
 * which can be {@link RNull} and optionally tag (~name) in {@link #getTag()}.
 *
 * Pair-lists with {@link #cdr()} different from {@link RNull} or {@link RPairList} are possible to
 * construct internally, but not supported everywhere, e.g. built-in length fails with them.
 * However, they are used internally during serialization and by some packages. For example, during
 * installation of the rlang package, the following {@link SEXPTYPE} are used as {@code CDR} value
 * in GNUR: 0, 1, 2, 6, 10, 19, 21.
 */
@ExportLibrary(RPairListLibrary.class)
@ExportLibrary(InteropLibrary.class)
@ExportLibrary(AbstractContainerLibrary.class)
public final class RPairList extends RAbstractContainer implements Iterable<RPairList>, Shareable {

    private static final RSymbol FUNCTION_SYMBOL = RDataFactory.createSymbolInterned("function");

    /**
     * Data of the current pair list cell (can never be {@code null}).
     */
    private Object car;

    /**
     * Link to the next {@link RPairList} cell or {@link RNull} if last (can never be {@code null}).
     */
    private Object cdr;

    /**
     * Externally, i.e., when serialized, this is either a SYMSXP ({@link RSymbol}) or an
     * {@link RNull}. Internally it may take on other values (can never be {@code null}).
     */
    private Object tag;

    /**
     * Denotes the (GnuR) type of entity that the pairlist represents. (Internal use only).
     */
    private SEXPTYPE type;

    /**
     * A closure representing the language pairlist. If this is non-null, then car/cdr/tag are
     * uninitialized and were never accessed.
     */
    private Closure closure;

    /**
     * If this is {@code true}, then the car/cdr/tag fields were not accessed yet, and thus the
     * closure field can be initialized.
     */
    private boolean mayBeClosure;

    /**
     * Uninitialized pairlist.
     */
    RPairList() {
        // defaults to the uninitialized list case
        this.car = RNull.instance;
        this.cdr = RNull.instance;
        this.tag = RNull.instance;
    }

    /**
     * Normal pairlist with values for car, cdr, tag and type.
     */
    RPairList(Object car, Object cdr, Object tag, SEXPTYPE type) {
        assert car != null;
        assert cdr != null;
        assert tag != null;
        this.car = car;
        this.cdr = cdr;
        this.tag = tag;
        this.type = type;
    }

    /**
     * A "language" pairlist that is initialized with an AST (and which will be deconstructed into a
     * real pairlist if needed).
     */
    RPairList(Closure closure) {
        assert assertClosure(closure);
        this.closure = closure;
        this.type = SEXPTYPE.LANGSXP;
        this.mayBeClosure = true;
        copyAttributesFromClosure();
        // patternState = PatternState.none;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    boolean hasArrayElements() {
        return true;
    }

    @ExportMessage
    long getArraySize() {
        return getLength();
    }

    @ExportMessage
    boolean isArrayElementReadable(long index) {
        return index >= 0 && index < getLength();
    }

    @ExportMessage
    Object readArrayElement(long index,
                    @Cached.Shared("r2Foreign") @Cached() R2Foreign r2Foreign,
                    @Cached.Shared("unknownIdentifier") @Cached("createBinaryProfile()") ConditionProfile invalidIndex) throws InvalidArrayIndexException {
        if (!invalidIndex.profile(isArrayElementReadable(index))) {
            throw InvalidArrayIndexException.create(index);
        }
        return r2Foreign.convert(getDataAtAsObject((int) index));
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    boolean hasMembers() {
        return true;
    }

    @ExportMessage
    Object getMembers(@SuppressWarnings("unused") boolean includeInternal) {
        RStringVector names = getNames();
        return names != null ? names : RDataFactory.createEmptyStringVector();
    }

    @ExportMessage
    boolean isMemberReadable(String member) {
        int idx = getElementIndexByName(member);
        return isArrayElementReadable(idx);
    }

    @ExportMessage
    boolean isMemberInvocable(String member) {
        int idx = getElementIndexByName(member);
        return isArrayElementReadable(idx) && getDataAtAsObject(idx) instanceof RFunction;
    }

    @ExportMessage
    Object readMember(String member,
                    @Cached.Shared("r2Foreign") @Cached() R2Foreign r2Foreign,
                    @Cached.Shared("unknownIdentifier") @Cached("createBinaryProfile()") ConditionProfile unknownIdentifier) throws UnknownIdentifierException {
        int idx = getElementIndexByName(member);
        if (unknownIdentifier.profile(!isArrayElementReadable(idx))) {
            throw UnknownIdentifierException.create(member);
        }
        return r2Foreign.convert(getDataAtAsObject(idx));
    }

    @ExportMessage
    Object invokeMember(String member, Object[] arguments,
                    @Cached.Shared("unknownIdentifier") @Cached("createBinaryProfile()") ConditionProfile unknownIdentifier,
                    @Cached() RFunction.ExplicitCall c) throws UnknownIdentifierException, UnsupportedMessageException {
        int idx = getElementIndexByName(member);
        if (unknownIdentifier.profile(!isArrayElementReadable(idx))) {
            throw UnknownIdentifierException.create(member);
        }
        Object f = getDataAtAsObject(idx);
        if (f instanceof RFunction) {
            return c.execute((RFunction) f, arguments);
        }
        throw UnsupportedMessageException.create();
    }

    @TruffleBoundary
    private void copyAttributesFromClosure() {
        RAttributable.copyAttributes(this, closure.getSyntaxElement().getAttributes());
    }

    private static boolean assertClosure(Closure closure) {
        RSyntaxNode node = closure.getExpr().asRSyntaxNode();
        if (node instanceof RSyntaxCall) {
            RSyntaxCall call = (RSyntaxCall) node;
            if (call.getSyntaxLHS() instanceof RSyntaxLookup && ((RSyntaxLookup) call.getSyntaxLHS()).getIdentifier().equals("function")) {
                boolean valid = true;
                valid &= call.getSyntaxSignature().getLength() >= 2;
                if (valid) {
                    RSyntaxElement argList = call.getSyntaxArguments()[1];
                    valid &= argList instanceof RSyntaxConstant;
                    if (valid) {
                        Object list = ((RSyntaxConstant) argList).getValue();
                        valid &= list instanceof RNull || list instanceof RPairList;
                    }
                }
                assert !valid : "valid calls to 'function' should be instances of RSyntaxFunction";
            }
        } else {
            assert node instanceof RSyntaxFunction : "invalid contents of 'language' pairlist: " + node;
        }
        return true;
    }

    /**
     * Converts the given vector to a pairlist of the given type. This will apply the "names"
     * attribute by putting the names into tags, and copy all other attributes.
     */
    @TruffleBoundary
    public static Object asPairList(RAbstractContainer vector, SEXPTYPE type) {
        BaseVectorFactory dataFactory = RDataFactory.getInstance();
        Object result = RNull.instance;
        RStringVector names = vector.getNames();
        for (int i = vector.getLength() - 1; i >= 0; i--) {
            Object item = vector.getDataAtAsObject(i);
            // Note: RSymbol.MISSING is converted to syntax constant with value REmpty only once the
            // pairlist data are converted to a closure.
            if (item == RMissing.instance) {
                // If we get directly RMissing, we convert it to REmpty, because RMissing constant
                // should not appear in AST. See JavaDoc of REmpty for more details.
                item = REmpty.instance;
            }
            result = dataFactory.createPairList(item, result, names != null ? RDataFactory.createSymbolInterned(names.getDataAt(i)) : RNull.instance, SEXPTYPE.LISTSXP);
        }
        if (result != RNull.instance) {
            RPairList list = (RPairList) result;
            list.mayBeClosure = true;
            list.setType(type);
            DynamicObject attrs = vector.getAttributes();
            if (attrs != null) {
                DynamicObject resultAttrs = list.initAttributes();
                Iterator<RAttributesLayout.RAttribute> iter = RAttributesLayout.asIterable(attrs).iterator();
                while (iter.hasNext()) {
                    RAttributesLayout.RAttribute attr = iter.next();
                    String attrName = attr.getName();
                    if (!(attrName.equals(RRuntime.NAMES_ATTR_KEY) || attrName.equals(RRuntime.DIM_ATTR_KEY) || attrName.equals(RRuntime.DIMNAMES_ATTR_KEY))) {
                        resultAttrs.define(attrName, attr.getValue());
                    }
                }
            }
            list.setTypedValueInfo(vector.getTypedValueInfo());
        }
        return result;
    }

    /**
     * Creates a new pair list of given size and type.
     */
    @TruffleBoundary
    static RPairList create(int size, SEXPTYPE type) {
        assert size > 0 : "a pair list of size = 0 does not exist, it should be NULL";
        RPairList result = new RPairList();
        for (int i = 1; i < size; i++) {
            RPairList tmp = result;
            result = new RPairList();
            result.setCdr(tmp);
        }
        result.setType(type);
        return result;
    }

    public boolean isLanguage() {
        return type == SEXPTYPE.LANGSXP;
    }

    @Override
    public RType getRType() {
        return isLanguage() ? RType.Language : RType.PairList;
    }

    /**
     * Can be called after constructing a new pairlist to signal that the individual elements are
     * not referenced and thus the closure field can be initialized.
     */
    public void allowClosure() {
        this.mayBeClosure = true;
    }

    @Override
    public Object getInternalStore() {
        return this;
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        if (isLanguage()) {
            return String.format("language(%s)", RDeparse.deparseSyntaxElement(getSyntaxElement()));
        } else {
            return String.format("pairlist(type=%s, tag=%s, car=%s, cdr=%s)", type, tag, toStringHelper(car), toStringHelper(cdr));
        }
    }

    private static String toStringHelper(Object obj) {
        return obj == null ? "null" : obj.getClass().getSimpleName();
    }

    /**
     * Creates an {@link RList} from this pairlist.
     */
    @TruffleBoundary
    public RList toRList() {
        int len = 0;
        boolean named = false;
        for (RPairList item : this) {
            named = named || !item.isNullTag();
            len++;
        }
        Object[] data = new Object[len];
        String[] names = named ? new String[len] : null;
        int i = 0;
        for (RPairList plt : this) {
            data[i] = plt.car();
            if (named) {
                Object ptag = plt.getTag();
                if (RRuntime.isNull(ptag)) {
                    names[i] = RRuntime.NAMES_ATTR_EMPTY_VALUE;
                } else if (ptag instanceof RSymbol) {
                    names[i] = ((RSymbol) ptag).getName();
                } else {
                    names[i] = RRuntime.asString(ptag);
                    assert names[i] != null : "unexpected type of tag in RPairList";
                }
            }
            i++;
        }
        RList result = named ? RDataFactory.createList(data, RDataFactory.createStringVector(names, RDataFactory.COMPLETE_VECTOR)) : RDataFactory.createList(data);
        DynamicObject attrs = getAttributes();
        if (attrs != null) {
            DynamicObject resultAttrs = result.initAttributes();
            Iterator<RAttributesLayout.RAttribute> iter = RAttributesLayout.asIterable(attrs).iterator();
            while (iter.hasNext()) {
                RAttributesLayout.RAttribute attr = iter.next();
                String attrName = attr.getName();
                if (!(attrName.equals(RRuntime.NAMES_ATTR_KEY) || attrName.equals(RRuntime.DIM_ATTR_KEY) || attrName.equals(RRuntime.DIMNAMES_ATTR_KEY))) {
                    resultAttrs.define(attrName, attr.getValue());
                }
            }
        }
        return result;
    }

    @Ignore
    public Object car() {
        return RPairListLibrary.getUncached().car(this);
    }

    @ExportMessage
    static class Car {

        @Specialization(guards = "!pl.hasClosure()")
        static Object withoutClosure(RPairList pl) {
            pl.mayBeClosure = false;
            return pl.car;
        }

        @Specialization(guards = "pl.hasClosure()")
        static Object withClosure(RPairList pl) {
            return pl.getDataAtAsObject(0);
        }
    }

    @Ignore
    public void setCar(Object car) {
        RPairListLibrary.getUncached().setCar(this, car);
    }

    @ExportMessage
    static class SetCar {
        @Specialization(guards = "!pl.hasClosure()")
        static void withoutClosureAndOwner(RPairList pl, Object value) {
            pl.mayBeClosure = false;
            pl.car = value;
        }

        @TruffleBoundary
        @Specialization(guards = "pl.hasClosure()")
        static void withClosure(RPairList pl, Object value) {
            pl.ensurePairList();
            pl.car = value;
        }
    }

    @Ignore
    public Object cdr() {
        return RPairListLibrary.getUncached().cdr(this);
    }

    @ExportMessage
    static class Cdr {

        @Specialization(guards = "!pl.hasClosure()")
        static Object withoutClosure(RPairList pl) {
            pl.mayBeClosure = false;
            return pl.cdr;
        }

        @Specialization(guards = "pl.hasClosure()")
        static Object withClosure(RPairList pl) {
            pl.mayBeClosure = false;
            pl.convertToPairList();
            return pl.cdr;
        }
    }

    public void setCdr(Object newCdr) {
        ensurePairList();
        assert newCdr != null;
        cdr = newCdr;
    }

    public Object cadr() {
        ensurePairList();
        return ((RPairList) cdr).car();
    }

    public Object cddr() {
        ensurePairList();
        return ((RPairList) cdr).cdr();
    }

    public Object caddr() {
        ensurePairList();
        RPairList pl = (RPairList) ((RPairList) cdr).cdr();
        return pl.car();
    }

    @Ignore
    public Object getTag() {
        return RPairListLibrary.getUncached().getTag(this);
    }

    @ExportMessage
    static class GetTag {

        @Specialization(guards = "!pl.hasClosure()")
        static Object withoutClosure(RPairList pl) {
            return pl.tag;
        }

        static boolean hasSyntaxLHSName(RPairList pl) {
            return pl.closure.getSyntaxLHSName() != null;
        }

        @Specialization(guards = {"pl.hasClosure()", "hasSyntaxLHSName(pl)"})
        static Object withClosureAndLHSName(RPairList pl) {
            return pl.closure.getSyntaxLHSName();
        }

        @Specialization(guards = {"pl.hasClosure()", "!hasSyntaxLHSName(pl)"})
        static Object withClosureAndNoLHSName(@SuppressWarnings("unused") RPairList pl) {
            return RNull.instance;
        }
    }

    @Ignore
    public void setTag(Object tag) {
        RPairListLibrary.getUncached().setTag(this, tag);
    }

    @ExportMessage
    static class SetTag {
        @Specialization(guards = "!pl.hasClosure()")
        static void withoutClosureWithoutOwner(RPairList pl, Object value) {
            pl.mayBeClosure = false;
            pl.tag = value;
        }

        @TruffleBoundary
        @Specialization(guards = "pl.hasClosure()")
        static void withClosure(RPairList pl, Object value) {
            pl.ensurePairList();
            pl.tag = value;
        }
    }

    public void setType(SEXPTYPE type) {
        if (type != this.type) {
            ensurePairList();
            this.type = type;
        }
    }

    public boolean isNullTag() {
        if (closure != null) {
            return closure.getSyntaxLHSName() == null;
        } else {
            return tag == RNull.instance;
        }
    }

    public SEXPTYPE getType() {
        return type;
    }

    /**
     * Appends given value as the last element of the pairlist.
     */
    public void appendToEnd(RPairList value) {
        ensurePairList();
        RPairList last = null;
        for (RPairList item : this) {
            last = item;
        }
        last.setCdr(value);
    }

    @Override
    @ExportMessage
    public boolean isComplete() {
        return false;
    }

    @Override
    @Ignore
    public int getLength() {
        return RPairListLibrary.getUncached().getLength(this);
    }

    @ExportMessage
    static class GetLength {

        @Specialization(guards = "pl.hasClosure()")
        // @TruffleBoundary - is it necessary?
        static int getLengthWithClosure(RPairList pl, @Cached BranchProfile profile) {
            RSyntaxElement s = pl.closure.getSyntaxElement();
            if (s instanceof RSyntaxCall) {
                profile.enter();
                return ((RSyntaxCall) s).getSyntaxSignature().getLength() + 1;
            }
            assert s instanceof RSyntaxFunction : "unexpected type: " + s.getClass();
            return 4;
        }

        @Specialization(guards = "!pl.hasClosure()")
        static int getLengthWithoutClosure(RPairList pl, @CachedLibrary(limit = "1") RPairListLibrary plLib) {
            int result = 1;
            Object tcdr = plLib.cdr(pl);
            while (!RRuntime.isNull(tcdr) && tcdr instanceof RPairList) {
                tcdr = plLib.cdr(tcdr);
                result++;
            }
            return result;
        }
    }

    @Override
    public void setLength(int l) {
        throw RInternalError.shouldNotReachHere();
    }

    @Override
    public int getTrueLength() {
        throw RInternalError.shouldNotReachHere();
    }

    @Override
    public void setTrueLength(int l) {
        throw RInternalError.shouldNotReachHere();
    }

    @Override
    public RAbstractContainer resize(int size) {
        throw RInternalError.shouldNotReachHere();
    }

    @Override
    public boolean hasDimensions() {
        return !isLanguage();
    }

    @Override
    public int[] getDimensions() {
        if (isLanguage()) {
            return null;
        } else {
            return new int[]{1};
        }
    }

    @Override
    public void setDimensions(int[] newDimensions) {
        throw RInternalError.shouldNotReachHere();
    }

    @Override
    @TruffleBoundary
    @ExportMessage
    public RPairList copy() {
        if (closure != null) {
            RPairList result = new RPairList(closure);
            if (getAttributes() != null) {
                result.initAttributes(RAttributesLayout.copy(getAttributes()));
            }
            result.setTypedValueInfo(getTypedValueInfo());
            return result;
        } else {
            BaseVectorFactory dataFactory = RDataFactory.getInstance();
            RPairList curr = dataFactory.createPairList();
            RPairList result = curr;
            Object original = this;
            while (true) {
                RPairList origList = (RPairList) original;
                curr.setCar(origList.car());
                curr.setTag(origList.getTag());
                curr.setType(origList.getType());
                original = origList.cdr();
                if (RRuntime.isNull(original)) {
                    curr.setCdr(RNull.instance);
                    break;
                }
                curr.setCdr(dataFactory.createPairList());
                curr = (RPairList) curr.cdr();
            }
            if (getAttributes() != null) {
                result.initAttributes(RAttributesLayout.copy(getAttributes()));
            }
            result.allowClosure();
            result.setTypedValueInfo(getTypedValueInfo());
            return result;
        }
    }

    @Override
    @TruffleBoundary
    public RSharingAttributeStorage deepCopy() {
        if (closure != null) {
            return copy();
        } else {
            RPairList result = copy();
            RPairList current = result;
            while (true) {
                Object c = current.car();
                if (RSharingAttributeStorage.isShareable(c)) {
                    current.setCar(((RSharingAttributeStorage) c).deepCopy());
                }
                Object next = current.cdr();
                if (next == RNull.instance) {
                    break;
                }
                current = (RPairList) next;
            }
            result.allowClosure();
            return result;
        }
    }

    @ExportMessage
    @Override
    public RPairList materialize() {
        return this;
    }

    @ExportMessage
    void materializeData() {
        // nop
    }

    @Override
    @TruffleBoundary
    public Object getDataAtAsObject(int index) {
        if (closure != null) {
            return getClosureDataAtAsObject(index);
        } else {
            Object pl = this;
            int i = 0;
            while (!RRuntime.isNull(pl) && i < index && pl instanceof RPairList) {
                pl = ((RPairList) pl).cdr();
                i++;
            }
            if (!(pl instanceof RPairList)) {
                throw new IndexOutOfBoundsException("Indexing into RPairList");
            }
            return ((RPairList) pl).car();
        }
    }

    @Override
    @TruffleBoundary
    public RStringVector getNames() {
        if (closure != null) {
            return closure.getNamesVector();
        } else {
            boolean hasNames = false;
            int length = 1;
            RPairList current = this;
            while (true) {
                if (current.getTag() != RNull.instance) {
                    hasNames = true;
                }
                Object next = current.cdr();
                if (RRuntime.isNull(next)) {
                    break;
                }
                current = (RPairList) next;
                length++;
            }
            if (!hasNames) {
                return null;
            }
            String[] data = new String[length];
            current = this;
            int i = 0;
            while (true) {
                Object name = current.getTag();
                assert name == RNull.instance || name instanceof RSymbol;
                data[i] = name == RNull.instance ? "" : ((RSymbol) name).getName();
                Object next = current.cdr();
                if (RRuntime.isNull(next)) {
                    break;
                }
                current = (RPairList) next;
                i++;
            }
            // there can never be NAs in the names
            return RDataFactory.createStringVector(data, true);
        }
    }

    @Override
    @TruffleBoundary
    public void setNames(RStringVector newNames) {
        ensurePairList();
        Object p = this;
        if (newNames != null) {
            for (int i = 0; i < newNames.getLength() && !RRuntime.isNull(p); i++) {
                RPairList pList = (RPairList) p;
                String newNameVal = newNames.getDataAt(i);
                Object newTag = newNameVal.isEmpty() ? RNull.instance : RDataFactory.createSymbolInterned(newNameVal);
                pList.setTag(newTag);
                p = pList.cdr();
            }
        } else {
            while (!RRuntime.isNull(p)) {
                RPairList pList = (RPairList) p;
                pList.setTag(RNull.instance);
                p = pList.cdr();
            }
        }

    }

    @Override
    public Iterator<RPairList> iterator() {
        return RPairListLibrary.getUncached().iterable(this).iterator();
    }

    @Ignore
    public java.lang.Iterable<RPairList> iterable() {
        return RPairListLibrary.getUncached().iterable(this);
    }

    @ExportMessage
    static class Iterable {

        @Specialization
        static java.lang.Iterable<RPairList> iterable(RPairList pl, @CachedLibrary(limit = "1") RPairListLibrary plLib) {
            pl.ensurePairList();

            return new java.lang.Iterable<RPairList>() {

                @Override
                public Iterator<RPairList> iterator() {
                    return new Iterator<RPairList>() {
                        private Object plt = pl;

                        @Override
                        public boolean hasNext() {
                            return !RRuntime.isNull(plt);
                        }

                        @Override
                        public RPairList next() {
                            assert plt instanceof RPairList;
                            RPairList curr = (RPairList) plt;
                            plt = plLib.cdr(curr);
                            return curr;
                        }
                    };
                }

            };

        }
    }

    @TruffleBoundary
    public int getElementIndexByName(String name) {
        if (getNames() == null) {
            return -1;
        }
        RStringVector names = getNames();
        for (int i = 0; i < names.getLength(); i++) {
            if (names.getDataAt(i).equals(name)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns a new node that contains the code represented by this language pairlist. This node is
     * not used anywhere else and can be executed.
     */
    public RSyntaxNode createNode() {
        assert isLanguage();
        return RContext.getASTBuilder().process(getSyntaxElement());
    }

    private static RSyntaxElement unwrapToRSyntaxElement(Object obj, boolean functionLookup) {
        if (obj == RSymbol.MISSING) {
            return RContext.getASTBuilder().constant(RSyntaxNode.LAZY_DEPARSE, REmpty.instance);
        } else if ((obj instanceof RPairList && ((RPairList) obj).isLanguage())) {
            return ((RPairList) obj).getSyntaxElement();
        } else if (obj instanceof RSymbol) {
            return RContext.getASTBuilder().lookup(RSyntaxNode.LAZY_DEPARSE, ((RSymbol) obj).getName(), functionLookup);
        } else if (obj instanceof RPromise) {
            // Evaluate promise and return the result as constant.
            return RContext.getASTBuilder().constant(RSyntaxNode.LAZY_DEPARSE, RContext.getRRuntimeASTAccess().forcePromise("unwrapToRNode", obj));
        } else {
            return RContext.getASTBuilder().constant(RSyntaxNode.LAZY_DEPARSE, obj);
        }
    }

    public SourceSection getLazySourceSection() {
        return getSyntaxElement().getLazySourceSection();
    }

    public SourceSection getSourceSection() {
        return getSyntaxElement().getSourceSection();
    }

    public RSyntaxElement getSyntaxElement() {
        CompilerAsserts.neverPartOfCompilation();
        assert isLanguage();
        if (closure != null) {
            return closure.getSyntaxElement();
        } else {
            final int length = getLength();
            if (car == FUNCTION_SYMBOL) {
                if (length < 3) {
                    throw RError.error(RError.SHOW_CALLER, Message.BAD_FUNCTION_EXPR);
                }
                Object argsList = cadr();
                if (!(argsList instanceof RPairList || argsList == RNull.instance)) {
                    throw RError.error(RError.SHOW_CALLER, Message.BAD_FUNCTION_EXPR);
                }
                return new RSyntaxFunction() {
                    @Override
                    public SourceSection getSourceSection() {
                        return RSyntaxNode.INTERNAL;
                    }

                    @Override
                    public SourceSection getLazySourceSection() {
                        return RSyntaxNode.INTERNAL;
                    }

                    @Override
                    public void setSourceSection(SourceSection source) {
                        // ignored
                    }

                    @Override
                    public ArgumentsSignature getSyntaxSignature() {
                        if (argsList == RNull.instance) {
                            return ArgumentsSignature.empty(0);
                        }
                        RStringVector argsNames = ((RPairList) argsList).getNames();
                        return ArgumentsSignature.get(argsNames.getReadonlyStringData());
                    }

                    @Override
                    public RSyntaxElement[] getSyntaxArgumentDefaults() {
                        if (argsList == RNull.instance) {
                            return new RSyntaxElement[0];
                        }
                        RPairList current = (RPairList) argsList;
                        int argsLength = current.getLength();
                        RSyntaxElement[] result = new RSyntaxElement[argsLength];
                        int i = 0;
                        while (true) {
                            result[i] = unwrapToRSyntaxElement(current.car(), false);
                            Object next = current.cdr();
                            if (next == RNull.instance) {
                                break;
                            }
                            current = (RPairList) next;
                            i++;
                        }
                        return result;
                    }

                    @Override
                    public RSyntaxElement getSyntaxBody() {
                        return unwrapToRSyntaxElement(caddr(), false);
                    }

                    @Override
                    public String getSyntaxDebugName() {
                        return null;
                    }
                };
            } else {
                return new RSyntaxCall() {

                    @Override
                    public SourceSection getSourceSection() {
                        return RSyntaxNode.INTERNAL;
                    }

                    @Override
                    public SourceSection getLazySourceSection() {
                        return RSyntaxNode.INTERNAL;
                    }

                    @Override
                    public void setSourceSection(SourceSection source) {
                        // ignored
                    }

                    @Override
                    public RSyntaxElement getSyntaxLHS() {
                        return unwrapToRSyntaxElement(getDataAtAsObject(0), true);
                    }

                    @Override
                    public ArgumentsSignature getSyntaxSignature() {
                        RStringVector names = getNames();
                        if (names == null) {
                            return ArgumentsSignature.empty(length - 1);
                        } else {
                            String[] signature = new String[length - 1];
                            for (int i = 0; i < names.getLength() && i < (length - 1); i++) {
                                String name = names.getDataAt(i + 1);
                                if (name != null && !name.isEmpty()) {
                                    // in signatures, null is designated for unnamed arguments
                                    signature[i] = name;
                                }
                            }
                            return ArgumentsSignature.get(signature);
                        }
                    }

                    @Override
                    public RSyntaxElement[] getSyntaxArguments() {
                        RSyntaxElement[] result = new RSyntaxElement[length - 1];
                        for (int i = 1; i < length; i++) {
                            result[i - 1] = unwrapToRSyntaxElement(getDataAtAsObject(i), false);
                        }
                        return result;
                    }

                    @Override
                    public DynamicObject getAttributes() {
                        return RPairList.this.getAttributes();
                    }

                };
            }
        }
    }

    /**
     * Used mainly for serialization (because it has special handling for converting nodes to
     * pairlists).
     */
    public boolean hasClosure() {
        return closure != null;
    }

    @Ignore
    public Closure getClosure(ClosureCache<RBaseNode> cache) {
        if (closure != null) {
            return closure;
        } else {
            return createClosure(cache);
        }
    }

    @TruffleBoundary
    private Closure createClosure(ClosureCache<RBaseNode> cache) {
        RNode node = createNode().asRNode();
        Closure result = cache == null ? Closure.createLanguageClosure(node) : cache.getOrCreateLanguageClosure(node);
        if (mayBeClosure) {
            closure = result;
        }
        return result;
    }

    @Ignore
    public Closure getClosure() {
        return RPairListLibrary.getUncached().getClosure(this);
    }

    @ExportMessage
    static class GetClosure {

        @Specialization(guards = "!pl.hasClosure()")
        static Closure withoutClosure(RPairList pl) {
            return pl.createClosure(null);
        }

        @Specialization(guards = "pl.hasClosure()")
        static Closure withClosure(RPairList pl) {
            return pl.closure;
        }
    }

    @TruffleBoundary
    public Object getClosureDataAtAsObject(int index) {
        assert closure != null;
        // index has already been range checked based on getLength
        RSyntaxElement s = closure.getSyntaxElement();

        RSyntaxElement result;
        if (s instanceof RSyntaxCall) {
            RSyntaxCall call = (RSyntaxCall) s;
            if (index == 0) {
                result = call.getSyntaxLHS();
            } else {
                result = call.getSyntaxArguments()[index - 1];
                if (result == null) {
                    return RSymbol.MISSING;
                }
            }
        } else {
            /*
             * We do not expect RSyntaxConstant and RSyntaxLookup here: RSyntaxConstant should have
             * been converted to the constant value, and RSyntaxLookup should have been converted to
             * an RSymbol (see below).
             */
            assert s instanceof RSyntaxFunction;

            RSyntaxFunction function = (RSyntaxFunction) s;
            switch (index) {
                case 0:
                    return RDataFactory.createSymbol("function");
                case 1:
                    ArgumentsSignature sig = function.getSyntaxSignature();
                    RSyntaxElement[] defaults = function.getSyntaxArgumentDefaults();

                    Object lst = RNull.instance;
                    for (int i = sig.getLength() - 1; i >= 0; i--) {
                        lst = RDataFactory.createPairList(defaults[i] == null ? RSymbol.MISSING : RContext.getRRuntimeASTAccess().createLanguageElement(defaults[i]), lst,
                                        RDataFactory.createSymbolInterned(sig.getName(i)));
                    }
                    return lst;
                case 2:
                    result = function.getSyntaxBody();
                    break;
                case 3:
                    // srcref
                    return RSrcref.createLloc(RContext.getInstance(), s.getLazySourceSection());
                default:
                    throw RInternalError.shouldNotReachHere();
            }
        }

        // Constants and lookups are converted to their intrinsic value: including conversion from
        // syntax constant for RMissing/REmpty -> RSymbol.MISSING.
        return RContext.getRRuntimeASTAccess().createLanguageElement(result);
    }

    /**
     * Ensures that the car, cdr and tag fields are initialized from the contents of closure if
     * necessary.
     */
    private void ensurePairList() {
        mayBeClosure = false;
        if (closure != null) {
            convertToPairList();
        }
    }

    @TruffleBoundary
    private void convertToPairList() {
        RStringVector names = getNames();

        Object obj = RNull.instance;
        for (int i = getLength() - 1; i > 0; i--) {
            obj = RDataFactory.createPairList(getDataAtAsObject(i), obj);
        }
        this.car = getDataAtAsObject(0);
        this.cdr = obj;
        // names have to be taken before list is assigned
        closure = null;
        mayBeClosure = false;
        if (names != null) {
            setNames(names);
        } else {
            // needs to be initialized
            this.tag = RNull.instance;
        }
    }

    private static final class FastPathAccess extends FastPathFromListAccess {

        FastPathAccess(RAbstractContainer value) {
            super(value);
        }

        @Override
        public RType getType() {
            return RType.PairList;
        }

        @TruffleBoundary
        @Override
        protected Object getListElementImpl(AccessIterator accessIter, int index) {
            return ((RPairList) accessIter.getStore()).getDataAtAsObject(index);
        }
    }

    @Override
    public VectorAccess access() {
        return new FastPathAccess(this);
    }

    private static final SlowPathFromListAccess SLOW_PATH_ACCESS = new SlowPathFromListAccess() {
        @Override
        public RType getType() {
            return RType.PairList;
        }

        @TruffleBoundary
        @Override
        protected Object getListElementImpl(AccessIterator accessIter, int index) {
            return ((RPairList) accessIter.getStore()).getDataAtAsObject(index);
        }
    };

    @Override
    public VectorAccess slowPathAccess() {
        return SLOW_PATH_ACCESS;
    }

    private static final RPairList[] EMPTY_CELLS = new RPairList[0];

    public static RPairList[] getCells(RPairList head) {
        if (head == null) {
            return EMPTY_CELLS;
        }
        RPairList[] cells = new RPairList[head.getLength()];
        int cellIdx = 0;
        for (RPairList cell : head) {
            cells[cellIdx++] = cell;
        }
        return cells;
    }

    public static final class RPairListSnapshot {
        final Object root;
        final RPairListSnapshot car;
        final RPairListSnapshot cdr;

        public RPairListSnapshot(Object root) {
            this.root = root;
            if (root instanceof RPairList) {
                this.car = new RPairListSnapshot(((RPairList) root).car);
                this.cdr = new RPairListSnapshot(((RPairList) root).cdr);
            } else {
                this.car = null;
                this.cdr = null;
            }
        }

        public boolean isSame(Object otherRoot) {
            return checkStructure(otherRoot);
        }

        private boolean checkStructure(Object otherRoot) {
            if (this.root instanceof RPairList) {
                if (otherRoot instanceof RPairList) {
                    if (((RPairList) root).hasClosure() && ((RPairList) otherRoot).hasClosure()) {
                        return ((RPairList) root).getClosure() == ((RPairList) otherRoot).getClosure();
                    } else {
                        return recursiveCheck((RPairList) otherRoot);
                    }
                } else {
                    return false;
                }
            } else {
                // scalars
                return this.root == otherRoot;
            }
        }

        @TruffleBoundary
        private boolean recursiveCheck(RPairList otherRoot) {
            return this.car.checkStructure(otherRoot.car) && this.cdr.checkStructure(otherRoot.cdr);
        }

        public static RPairListSnapshot create(Object root) {
            return new RPairListSnapshot(root);
        }
    }

    public abstract static class RPairListSnapshotNode extends Node {

        final RPairListSnapshot snapshot;
        final boolean isRootPairList;
        final boolean hasRootClosure;

        public RPairListSnapshotNode(RPairListSnapshot snapshot) {
            this.snapshot = snapshot;
            this.isRootPairList = snapshot.root instanceof RPairList;
            this.hasRootClosure = this.isRootPairList && ((RPairList) snapshot.root).hasClosure();
        }

        public static RPairListSnapshotNode create(Object root) {
            return RPairListSnapshotNodeGen.create(RPairListSnapshot.create(root));
        }

        public abstract boolean execute(Object otherRoot);

        static boolean isPairList(Object x) {
            return x instanceof RPairList;
        }

        static boolean hasClosure(Object x) {
            return ((RPairList) x).hasClosure();
        }

        @Specialization(guards = {"isRootPairList", "isPairList(otherRoot)", "hasRootClosure", "hasClosure(otherRoot)"})
        boolean isSame1(RPairList otherRoot) {
            return ((RPairList) snapshot.root).getClosure() == otherRoot.getClosure();
        }

        @Specialization(guards = {"isRootPairList", "isPairList(otherRoot)", "!hasRootClosure || !hasClosure(otherRoot)"})
        boolean isSame2(RPairList otherRoot) {
            return snapshot.recursiveCheck(otherRoot);
        }

        @Specialization(guards = {"isRootPairList", "!isPairList(otherRoot)"})
        @SuppressWarnings("unused")
        boolean isSame3(RPairList otherRoot) {
            return false;
        }

        @Specialization(guards = {"!isRootPairList"})
        boolean isSame4(Object other) {
            // scalars
            return snapshot.root == other;
        }

    }
}
