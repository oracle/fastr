/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.emptyDoubleVector;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.emptyStringVector;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.notEmpty;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.READS_STATE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import java.text.ParsePosition;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.HashMap;
import java.util.TimeZone;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetNamesAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.SetClassAttributeNode;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RAttributeProfiles;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

// from GnuR datatime.c

public class DatePOSIXFunctions {

    private static final class POSIXltBuilder {

        private static final String[] LT_NAMES = new String[]{"sec", "min", "hour", "mday", "mon", "year", "wday", "yday", "isdst"};
        private static final RStringVector LT_NAMES_VEC = (RStringVector) RDataFactory.createStringVector(LT_NAMES, RDataFactory.COMPLETE_VECTOR).makeSharedPermanent();
        private static final RStringVector CLASS_ATTR = (RStringVector) RDataFactory.createStringVector(new String[]{"POSIXlt", "POSIXt"}, RDataFactory.COMPLETE_VECTOR).makeSharedPermanent();

        public final double[] sec;
        public final int[] min;
        public final int[] hour;
        public final int[] mday;
        public final int[] mon;
        public final int[] year;
        public final int[] wday;
        public final int[] yday;
        public final int[] isdst;
        private boolean complete = true;
        private final String zone;

        POSIXltBuilder(int length, String zone) {
            this.zone = zone;
            sec = new double[length];
            min = new int[length];
            hour = new int[length];
            mday = new int[length];
            mon = new int[length];
            year = new int[length];
            wday = new int[length];
            yday = new int[length];
            isdst = new int[length];
        }

        public void setEntry(int index, double newSec, int newMin, int newHour, int newMDay, int newMon, int newYear, int newWDay, int newYDay, int newIsDst) {
            sec[index] = newSec;
            min[index] = newMin;
            hour[index] = newHour;
            mday[index] = newMDay;
            mon[index] = newMon;
            year[index] = newYear;
            wday[index] = newWDay;
            yday[index] = newYDay;
            isdst[index] = newIsDst;
        }

        public void setIncompleteEntry(int index) {
            sec[index] = RRuntime.DOUBLE_NA;
            min[index] = RRuntime.INT_NA;
            hour[index] = RRuntime.INT_NA;
            mday[index] = RRuntime.INT_NA;
            mon[index] = RRuntime.INT_NA;
            year[index] = RRuntime.INT_NA;
            wday[index] = RRuntime.INT_NA;
            yday[index] = RRuntime.INT_NA;
            isdst[index] = -1;
            complete = false;
        }

        public RList finish() {
            Object[] data = new Object[LT_NAMES.length];
            data[0] = RDataFactory.createDoubleVector(sec, complete);
            data[1] = RDataFactory.createIntVector(min, complete);
            data[2] = RDataFactory.createIntVector(hour, complete);
            data[3] = RDataFactory.createIntVector(mday, complete);
            data[4] = RDataFactory.createIntVector(mon, complete);
            data[5] = RDataFactory.createIntVector(year, complete);
            data[6] = RDataFactory.createIntVector(wday, complete);
            data[7] = RDataFactory.createIntVector(yday, complete);
            data[8] = RDataFactory.createIntVector(isdst, true);
            RList result = RDataFactory.createList(data, LT_NAMES_VEC);
            result.setAttr("tzone", zone);
            result.setClassAttr(CLASS_ATTR);
            return result;
        }
    }

    @RBuiltin(name = "Date2POSIXlt", kind = INTERNAL, parameterNames = "x", behavior = PURE)
    public abstract static class Date2POSIXlt extends RBuiltinNode {

        @Child private GetNamesAttributeNode getNamesNode = GetNamesAttributeNode.create();

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg("x").mapNull(emptyDoubleVector()).asDoubleVector();
        }

        @Specialization
        @TruffleBoundary
        protected RList doDate2POSIXlt(RAbstractDoubleVector x) {
            int xLen = x.getLength();
            TimeZone zone = TimeZone.getTimeZone("UTC");
            POSIXltBuilder builder = new POSIXltBuilder(xLen, "UTC");
            for (int i = 0; i < xLen; i++) {
                double d = x.getDataAt(i);
                if (RRuntime.isFinite(d)) {
                    int day = (int) Math.floor(d);
                    Instant instant = Instant.ofEpochSecond(day * 3600L * 24L);
                    ZonedDateTime date = ZonedDateTime.ofInstant(instant, zone.toZoneId());
                    builder.setEntry(i, 0, 0, 0, date.getDayOfMonth(), date.getMonthValue() - 1, date.getYear() - 1900, date.getDayOfWeek().ordinal(), date.getDayOfYear(), 0);
                } else {
                    builder.setIncompleteEntry(i);
                }
            }
            RList result = builder.finish();
            RStringVector xNames = getNamesNode.getNames(x);
            if (xNames != null) {
                ((RIntVector) result.getDataAt(5)).copyNamesFrom(x);
            }
            return result;
        }
    }

    @RBuiltin(name = "as.POSIXlt", kind = INTERNAL, parameterNames = {"x", "tz"}, behavior = READS_STATE)
    public abstract static class AsPOSIXlt extends RBuiltinNode {

        @Child private GetNamesAttributeNode getNamesNode = GetNamesAttributeNode.create();

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg("x").mapNull(emptyDoubleVector()).asDoubleVector(true, false, false);
            casts.arg("tz").asStringVector().findFirst("");
        }

        @Specialization
        @TruffleBoundary
        protected RList asPOSIXlt(RAbstractDoubleVector x, String tz) {
            TimeZone zone;
            if (tz.isEmpty()) {
                zone = RContext.getInstance().getSystemTimeZone();
            } else {
                zone = TimeZone.getTimeZone(tz);
            }
            int xLen = x.getLength();
            POSIXltBuilder builder = new POSIXltBuilder(xLen, zone.getDisplayName(false, TimeZone.SHORT));
            for (int i = 0; i < xLen; i++) {
                double second = x.getDataAt(i);
                if (RRuntime.isFinite(second)) {
                    Instant instant = Instant.ofEpochSecond((long) second);
                    ZonedDateTime date = ZonedDateTime.ofInstant(instant, zone.toZoneId());
                    builder.setEntry(i, date.getSecond(), date.getMinute(), date.getHour(), date.getDayOfMonth(), date.getMonthValue() - 1, date.getYear() - 1900, date.getDayOfWeek().ordinal(),
                                    date.getDayOfYear(), 0);
                } else {
                    builder.setIncompleteEntry(i);
                }
            }
            RList result = builder.finish();
            RStringVector xNames = getNamesNode.getNames(x);
            if (xNames != null) {
                ((RIntVector) result.getDataAt(5)).copyNamesFrom(x);
            }
            return result;
        }
    }

    @RBuiltin(name = "as.POSIXct", kind = INTERNAL, parameterNames = {"x", "tz"}, behavior = READS_STATE)
    public abstract static class AsPOSIXct extends RBuiltinNode {

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg("x").mustBe(RAbstractListVector.class);
            casts.arg("tz").asStringVector().findFirst("");
        }

        @Specialization
        @TruffleBoundary
        protected RDoubleVector asPOSIXct(RAbstractListVector x, String tz) {
            RAbstractVector secVector = (RAbstractVector) RRuntime.asAbstractVector(x.getDataAt(0));
            RAbstractVector minVector = (RAbstractVector) RRuntime.asAbstractVector(x.getDataAt(1));
            RAbstractVector hourVector = (RAbstractVector) RRuntime.asAbstractVector(x.getDataAt(2));
            RAbstractVector mdayVector = (RAbstractVector) RRuntime.asAbstractVector(x.getDataAt(3));
            RAbstractVector monVector = (RAbstractVector) RRuntime.asAbstractVector(x.getDataAt(4));
            RAbstractVector yearVector = (RAbstractVector) RRuntime.asAbstractVector(x.getDataAt(5));
            TimeZone zone;
            if (tz.isEmpty()) {
                zone = RContext.getInstance().getSystemTimeZone();
            } else {
                zone = TimeZone.getTimeZone(tz);
            }

            ZoneId zoneId = zone.toZoneId();
            int length = max(secVector.getLength(), minVector.getLength(), hourVector.getLength(), mdayVector.getLength(), monVector.getLength(), yearVector.getLength());
            double[] data = new double[length];
            boolean complete = true;
            for (int i = 0; i < length; i++) {
                double sec = getDouble(secVector, i);
                if (RRuntime.isFinite(sec)) {
                    int min = getInt(minVector, i);
                    int hour = getInt(hourVector, i);
                    int mday = getInt(mdayVector, i);
                    int mon = getInt(monVector, i);
                    int year = getInt(yearVector, i);
                    if (mon >= 12) {
                        year += mon / 12;
                        mon %= 12;
                    }
                    if (mon < 0) {
                        int delta = 1 + (-mon) / 12;
                        year -= delta;
                        mon += delta * 12;
                    }
                    LocalDateTime time = LocalDateTime.of(year + 1900, mon + 1, 1, hour, min, (int) sec).plusDays(mday - 1);
                    ZonedDateTime zoned = time.atZone(zoneId);
                    data[i] = zoned.toInstant().getEpochSecond() + (sec - Math.floor(sec));
                } else {
                    data[i] = RRuntime.DOUBLE_NA;
                    complete = false;
                }
            }
            return RDataFactory.createDoubleVector(data, complete);
        }
    }

    @RBuiltin(name = "POSIXlt2Date", kind = INTERNAL, parameterNames = {"x"}, behavior = PURE)
    public abstract static class POSIXlt2Date extends RBuiltinNode {
        private static final RStringVector CLASS_ATTR = (RStringVector) RDataFactory.createStringVectorFromScalar("Date").makeSharedPermanent();

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg("x").mustBe(RAbstractListVector.class);
        }

        @Specialization
        @TruffleBoundary
        protected RDoubleVector posix2date(RAbstractListVector x,
                        @Cached("create()") SetClassAttributeNode setClassAttrNode) {
            RAbstractVector secVector = (RAbstractVector) RRuntime.asAbstractVector(x.getDataAt(0));
            RAbstractVector minVector = (RAbstractVector) RRuntime.asAbstractVector(x.getDataAt(1));
            RAbstractVector hourVector = (RAbstractVector) RRuntime.asAbstractVector(x.getDataAt(2));
            RAbstractVector mdayVector = (RAbstractVector) RRuntime.asAbstractVector(x.getDataAt(3));
            RAbstractVector monVector = (RAbstractVector) RRuntime.asAbstractVector(x.getDataAt(4));
            RAbstractVector yearVector = (RAbstractVector) RRuntime.asAbstractVector(x.getDataAt(5));

            int length = max(secVector.getLength(), minVector.getLength(), hourVector.getLength(), mdayVector.getLength(), monVector.getLength(), yearVector.getLength());
            double[] data = new double[length];
            boolean complete = true;
            for (int i = 0; i < length; i++) {
                double sec = getDouble(secVector, i);
                if (RRuntime.isFinite(sec)) {
                    int mday = getInt(mdayVector, i);
                    int mon = getInt(monVector, i);
                    int year = getInt(yearVector, i);
                    if (mon >= 12) {
                        year += mon / 12;
                        mon %= 12;
                    }
                    if (mon < 0) {
                        int delta = 1 + (-mon) / 12;
                        year -= delta;
                        mon += delta * 12;
                    }
                    LocalDate date = LocalDate.of(year + 1900, mon + 1, 1).plusDays(mday - 1);
                    data[i] = date.toEpochDay();
                } else {
                    data[i] = RRuntime.DOUBLE_NA;
                    complete = false;
                }
            }
            RDoubleVector result = RDataFactory.createDoubleVector(data, complete);
            setClassAttrNode.execute(result, CLASS_ATTR);
            return result;
        }
    }

    @RBuiltin(name = "format.POSIXlt", kind = INTERNAL, parameterNames = {"x", "format", "usetz"}, behavior = READS_STATE)
    public abstract static class FormatPOSIXlt extends RBuiltinNode {

        private static final HashMap<String, String> TIME_ZONE_MAPPING = new HashMap<>();

        static {
            TIME_ZONE_MAPPING.put("EST", "America/New_York");
            TIME_ZONE_MAPPING.put("EDT", "America/New_York");
            // TODO: find a proper source for this mapping
        }

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg("x").mustBe(RAbstractListVector.class);
            casts.arg("format").asStringVector().mustBe(notEmpty());
            casts.arg("usetz").asLogicalVector().findFirst(RRuntime.LOGICAL_FALSE).map(toBoolean());
        }

        @Specialization
        @TruffleBoundary
        protected RStringVector format(RAbstractListVector x, RAbstractStringVector format, boolean usetz) {
            RAbstractDoubleVector secVector = (RAbstractDoubleVector) RRuntime.asAbstractVector(x.getDataAt(0));
            RAbstractIntVector minVector = (RAbstractIntVector) RRuntime.asAbstractVector(x.getDataAt(1));
            RAbstractIntVector hourVector = (RAbstractIntVector) RRuntime.asAbstractVector(x.getDataAt(2));
            RAbstractIntVector mdayVector = (RAbstractIntVector) RRuntime.asAbstractVector(x.getDataAt(3));
            RAbstractIntVector monVector = (RAbstractIntVector) RRuntime.asAbstractVector(x.getDataAt(4));
            RAbstractIntVector yearVector = (RAbstractIntVector) RRuntime.asAbstractVector(x.getDataAt(5));
            ZoneId zone;
            DateTimeFormatterBuilder[] builders = createFormatters(format, false);
            String tzone = getTimeZomeFromAttribute(x);
            if (usetz && !tzone.isEmpty()) {
                zone = ZoneId.of(tzone, TIME_ZONE_MAPPING);
                for (DateTimeFormatterBuilder builder : builders) {
                    builder.appendLiteral(' ').appendZoneText(TextStyle.SHORT);
                }
            } else {
                zone = RContext.getInstance().getSystemTimeZone().toZoneId();
            }

            DateTimeFormatter[] formatters = new DateTimeFormatter[builders.length];
            for (int i = 0; i < builders.length; i++) {
                formatters[i] = builders[i].toFormatter();
            }
            int length = secVector.getLength();
            String[] data = new String[length];
            boolean complete = true;
            for (int i = 0; i < length; i++) {
                double sec = secVector.getDataAt(i);
                if (RRuntime.isFinite(sec)) {
                    int min = minVector.getDataAt(i);
                    int hour = hourVector.getDataAt(i);
                    int mday = mdayVector.getDataAt(i);
                    int mon = monVector.getDataAt(i) + 1;
                    int year = yearVector.getDataAt(i) + 1900;
                    LocalDateTime time = LocalDateTime.of(year, mon, mday, hour, min, (int) sec, (int) ((sec - Math.floor(sec)) * 1000000000L));
                    ZonedDateTime zoned = time.atZone(zone);
                    data[i] = formatters[i % formatters.length].format(zoned);
                } else {
                    data[i] = RRuntime.STRING_NA;
                    complete = false;
                }
            }
            return RDataFactory.createStringVector(data, complete);
        }
    }

    @RBuiltin(name = "strptime", kind = INTERNAL, parameterNames = {"x", "format", "tz"}, behavior = PURE)
    public abstract static class StrPTime extends RBuiltinNode {

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg("x").mapNull(emptyStringVector()).asStringVector();
            casts.arg("format").mapNull(emptyStringVector()).asStringVector();
            casts.arg("tz").mapNull(emptyStringVector()).asStringVector();
        }

        @Specialization
        @TruffleBoundary
        protected RList strptime(RAbstractStringVector x, RAbstractStringVector format, RAbstractStringVector tz) {
            TimeZone zone;
            String zoneString = RRuntime.asString(tz);
            if (zoneString.isEmpty()) {
                zone = RContext.getInstance().getSystemTimeZone();
            } else {
                zone = TimeZone.getTimeZone(zoneString);
            }
            int length = x.getLength();
            POSIXltBuilder builder = new POSIXltBuilder(length, zone.getDisplayName(false, TimeZone.SHORT));
            DateTimeFormatterBuilder[] builders = createFormatters(format, true);
            DateTimeFormatter[] formatters = new DateTimeFormatter[builders.length];
            for (int i = 0; i < builders.length; i++) {
                formatters[i] = builders[i].toFormatter();
            }

            for (int i = 0; i < length; i++) {
                String str = x.getDataAt(i);
                TemporalAccessor parse;
                try {
                    parse = formatters[i % formatters.length].parse(str, new ParsePosition(0));
                } catch (DateTimeParseException e) {
                    builder.setIncompleteEntry(i);
                    continue;
                }
                try {
                    LocalDateTime time = LocalDateTime.from(parse);
                    builder.setEntry(i, time.getSecond(), time.getMinute(), time.getHour(), time.getDayOfMonth(), time.getMonthValue() - 1, time.getYear() - 1900, time.getDayOfWeek().ordinal(),
                                    time.getDayOfYear(), 0);
                    continue;
                } catch (DateTimeException e) {
                    // try without time
                }
                try {
                    LocalDate date = LocalDate.from(parse);
                    builder.setEntry(i, 0, 0, 0, date.getDayOfMonth(), date.getMonthValue() - 1, date.getYear() - 1900, date.getDayOfWeek().ordinal(), date.getDayOfYear(), 0);
                } catch (DateTimeException e) {
                    throw RInternalError.shouldNotReachHere(e);
                }
            }
            return builder.finish();
        }
    }

    private static DateTimeFormatterBuilder[] createFormatters(RAbstractStringVector formats, boolean forInput) {
        DateTimeFormatterBuilder[] result = new DateTimeFormatterBuilder[formats.getLength()];
        for (int i = 0; i < result.length; i++) {
            result[i] = createFormatter(formats.getDataAt(i), forInput);
        }
        return result;
    }

    private static DateTimeFormatterBuilder createFormatter(String format, boolean forInput) {
        DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder();
        boolean escaped = false;
        int i = 0;
        while (i < format.length()) {
            char c = format.charAt(i);
            if (escaped) {
                switch (c) {
                    case 'a':
                        /*
                         * Abbreviated weekday name in the current locale. (Also matches full name
                         * on input.)
                         */
                        builder.appendText(ChronoField.DAY_OF_WEEK, TextStyle.SHORT);
                        break;
                    case 'A':
                        /*
                         * Full weekday name in the current locale. (Also matches abbreviated name
                         * on input.)
                         */
                        builder.appendText(ChronoField.DAY_OF_WEEK);
                        break;
                    case 'b':
                    case 'h':
                        /*
                         * Abbreviated month name in the current locale. (Also matches full name on
                         * input.)
                         */
                        builder.appendText(ChronoField.MONTH_OF_YEAR, TextStyle.SHORT);
                        break;
                    case 'B':
                        /*
                         * Full month name in the current locale. (Also matches abbreviated name on
                         * input.)
                         */
                        builder.appendText(ChronoField.MONTH_OF_YEAR);
                        break;
                    case 'c':
                        /*
                         * Date and time. Locale-specific on output, "%a %b %e %H:%M:%S %Y" on
                         * input.
                         */
                        builder.append(createFormatter("%a %b %e %H:%M:%S %Y", forInput).toFormatter());
                        break;
                    case 'C':
                        // Century (00–99): the integer part of the year divided by 100.
                        builder.appendValue(ChronoField.YEAR, 2);
                        break;
                    case 'd':
                        // Day of the month as decimal number (01–31).
                        // TODO: leading zero
                        builder.appendValue(ChronoField.DAY_OF_MONTH, 2);
                        break;
                    case 'D':
                        /*
                         * Date format such as %m/%d/%y: ISO C99 says it should be that exact
                         * format.
                         */
                        builder.append(createFormatter("%m/%d/%y", forInput).toFormatter());
                        break;
                    case 'e':
                        /*
                         * Day of the month as decimal number (1–31), with a leading space for a
                         * single-digit number.
                         */
                        // TODO: leading space
                        builder.appendValue(ChronoField.DAY_OF_MONTH, 2);
                        break;
                    case 'F':
                        // Equivalent to %Y-%m-%d (the ISO 8601 date format).
                        builder.append(createFormatter("%Y-%m-%d", forInput).toFormatter());
                        break;
                    case 'g':
                        /*
                         * The last two digits of the week-based year (see %V). (Accepted but
                         * ignored on input.)
                         */
                        throw RInternalError.unimplemented();
                    case 'G':
                        /*
                         * The week-based year (see %V) as a decimal number. (Accepted but ignored
                         * on input.)
                         */
                        throw RInternalError.unimplemented();
                    case 'H':
                        /*
                         * Hours as decimal number (00–23). As a special exception strings such as
                         * 24:00:00 are accepted for input, since ISO 8601 allows these.
                         */
                        builder.appendValue(ChronoField.CLOCK_HOUR_OF_DAY, 2);
                        break;
                    case 'I':
                        // Hours as decimal number (01–12).
                        builder.appendValue(ChronoField.CLOCK_HOUR_OF_AMPM, 2);
                        break;
                    case 'j':
                        // Day of year as decimal number (001–366).
                        builder.appendValue(ChronoField.DAY_OF_YEAR, 3);
                        break;
                    case 'm':
                        // Month as decimal number (01–12).
                        builder.appendValue(ChronoField.MONTH_OF_YEAR, 2);
                        break;
                    case 'M':
                        // Minute as decimal number (00–59).
                        builder.appendValue(ChronoField.MINUTE_OF_HOUR, 2);
                        break;
                    case 'n':
                        // Newline on output, arbitrary whitespace on input.
                        builder.appendLiteral('\n');
                        break;
                    case 'O':
                        if (i + 2 < format.length() && format.charAt(i + 1) == 'S' && format.charAt(i + 2) == '3') {
                            builder.appendValue(ChronoField.SECOND_OF_MINUTE, 2).appendLiteral('.').appendValue(ChronoField.MILLI_OF_SECOND, 3);
                            i += 2;
                        } else {
                            builder.appendLiteral(c);
                        }
                        break;
                    case 'p':
                    case 'P':
                        /*
                         * AM/PM indicator in the locale. Used in conjunction with %I and not with
                         * %H. An empty string in some locales (and the behaviour is undefined if
                         * used for input in such a locale). Some platforms accept %P for output,
                         * which uses a lower-case version: others will output P.
                         */
                        builder.appendText(ChronoField.AMPM_OF_DAY);
                        break;
                    case 'r':
                        /*
                         * The 12-hour clock time (using the locale's AM or PM). Only defined in
                         * some locales.
                         */
                        builder.appendValue(ChronoField.CLOCK_HOUR_OF_AMPM, 2).appendText(ChronoField.AMPM_OF_DAY);
                        break;
                    case 'R':
                        // Equivalent to %H:%M.
                        builder.appendValue(ChronoField.CLOCK_HOUR_OF_DAY, 2).appendLiteral(':').appendValue(ChronoField.MINUTE_OF_HOUR, 2);
                        break;
                    case 'S':
                        /*
                         * Second as decimal number (00–61), allowing for up to two leap-seconds
                         * (but POSIX-compliant implementations will ignore leap seconds).
                         */
                        builder.appendValue(ChronoField.SECOND_OF_MINUTE, 2);
                        break;
                    case 't':
                        // Tab on output, arbitrary whitespace on input.
                        builder.appendLiteral('\t');
                        break;
                    case 'T':
                        // Equivalent to %H:%M:%S.
                        builder.appendValue(ChronoField.CLOCK_HOUR_OF_DAY, 2).appendLiteral(':').appendValue(ChronoField.MINUTE_OF_HOUR, 2).appendLiteral(':').appendValue(
                                        ChronoField.SECOND_OF_MINUTE, 2);
                        break;
                    case 'u':
                        // Weekday as a decimal number (1–7, Monday is 1).
                        builder.appendValue(ChronoField.DAY_OF_WEEK);
                        break;
                    case 'U':
                        /*
                         * Week of the year as decimal number (00–53) using Sunday as the first day
                         * 1 of the week (and typically with the first Sunday of the year as day 1
                         * of week 1). The US convention.
                         */
                        throw RInternalError.unimplemented();
                    case 'V':
                        /*
                         * Week of the year as decimal number (01–53) as defined in ISO 8601. If the
                         * week (starting on Monday) containing 1 January has four or more days in
                         * the new year, then it is considered week 1. Otherwise, it is the last
                         * week of the previous year, and the next week is week 1. (Accepted but
                         * ignored on input.)
                         */
                        throw RInternalError.unimplemented();
                    case 'w':
                        // Weekday as decimal number (0–6, Sunday is 0).
                        throw RInternalError.unimplemented();
                    case 'W':
                        /*
                         * Week of the year as decimal number (00–53) using Monday as the first day
                         * of week (and typically with the first Monday of the year as day 1 of week
                         * 1). The UK convention.
                         */
                        throw RInternalError.unimplemented();
                    case 'x':
                        // Date. Locale-specific on output, "%y/%m/%d" on input.
                        assert forInput;
                        builder.append(createFormatter("%y/%m/%d", forInput).toFormatter());
                        break;
                    case 'X':
                        // Time. Locale-specific on output, "%H:%M:%S" on input.
                        assert forInput;
                        builder.append(createFormatter("%H:%M:%S", forInput).toFormatter());
                        break;
                    case 'y':
                        /*
                         * Year without century (00–99). On input, values 00 to 68 are prefixed by
                         * 20 and 69 to 99 by 19 – that is the behaviour specified by the 2004 and
                         * 2008 POSIX standards, but they do also say ‘it is expected that in a
                         * future version the default century inferred from a 2-digit year will
                         * change’.
                         */
                        builder.appendPattern("yy");
                        break;
                    case 'Y':
                        /*
                         * Year with century. Note that whereas there was no zero in the original
                         * Gregorian calendar, ISO 8601:2004 defines it to be valid (interpreted as
                         * 1BC): see http://en.wikipedia.org/wiki/0_(year). Note that the standards
                         * also say that years before 1582 in its calendar should only be used with
                         * agreement of the parties involved. For input, only years 0:9999 are
                         * accepted.
                         */
                        builder.appendValue(ChronoField.YEAR, 4);
                        break;
                    case 'z':
                        /*
                         * Signed offset in hours and minutes from UTC, so -0800 is 8 hours behind
                         * UTC. Values up to +1400 are accepted as from R 3.1.1: previous versions
                         * only accepted up to +1200. (Standard only for output.)
                         */
                        throw RInternalError.unimplemented();
                    case 'Z':
                        /*
                         * (Output only.) Time zone abbreviation as a character string (empty if not
                         * available). This may not be reliable when a time zone has changed
                         * abbreviations over the years.
                         */
                        throw RInternalError.unimplemented();
                    default:
                        builder.appendLiteral(c);
                        break;
                }
                escaped = false;
            } else {
                if (c == '%') {
                    escaped = true;
                } else {
                    builder.appendLiteral(c);
                }
            }
            i++;
        }
        return builder;
    }

    private static double getDouble(RAbstractVector v, int index) {
        int i = index % v.getLength();
        if (v instanceof RAbstractDoubleVector) {
            return ((RAbstractDoubleVector) v).getDataAt(i);
        } else if (v instanceof RAbstractIntVector) {
            return ((RAbstractIntVector) v).getDataAt(i);
        } else {
            throw RInternalError.shouldNotReachHere();
        }
    }

    private static int getInt(RAbstractVector v, int index) {
        int i = index % v.getLength();
        if (v instanceof RAbstractIntVector) {
            return ((RAbstractIntVector) v).getDataAt(i);
        } else if (v instanceof RAbstractDoubleVector) {
            return (int) ((RAbstractDoubleVector) v).getDataAt(i);
        } else {
            throw RInternalError.shouldNotReachHere();
        }
    }

    private static int max(int... values) {
        int result = Integer.MIN_VALUE;
        for (int v : values) {
            result = Math.max(result, v);
        }
        return result;
    }

    private static String getTimeZomeFromAttribute(RAbstractListVector x) {
        Object attr = x.getAttributes().get("tzone");
        RAbstractVector vector = (RAbstractVector) RRuntime.asAbstractVector(attr);
        if (vector.getLength() == 0) {
            return "";
        }
        String zone = RRuntime.asString(vector.getDataAtAsObject(0));
        if (zone.isEmpty() && vector.getLength() > 1) {
            zone = RRuntime.asString(vector.getDataAtAsObject(1));
        }
        return zone;
    }
}
