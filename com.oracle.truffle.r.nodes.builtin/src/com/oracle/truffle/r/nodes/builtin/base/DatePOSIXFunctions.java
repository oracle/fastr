/*
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, a copy is available at
 * https://www.R-project.org/Licenses/
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.emptyDoubleVector;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.emptyStringVector;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.missingValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.notEmpty;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.nullValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.size;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.READS_STATE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import java.text.ParsePosition;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.time.chrono.ChronoPeriod;
import java.time.chrono.ChronoZonedDateTime;
import java.time.chrono.Chronology;
import java.time.chrono.Era;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;
import java.time.format.ResolverStyle;
import java.time.format.SignStyle;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.ValueRange;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.attributes.SpecialAttributesFunctions.GetNamesAttributeNode;
import com.oracle.truffle.r.runtime.data.nodes.attributes.SpecialAttributesFunctions.SetClassAttributeNode;

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
        private final RStringVector zone;

        private final TimeZone realZone;

        POSIXltBuilder(int length, String explicitZone) {
            String[] zones = new String[3];
            zones[0] = explicitZone;
            realZone = explicitZone.isEmpty() ? RContext.getInstance().stateREnvVars.getSystemTimeZone() : TimeZone.getTimeZone(explicitZone);
            // getTimeZone returns the default if the ID does not exist, by comparing the return's
            // value ID to the explicit one we can find out the if the zone was found.
            if (explicitZone.isEmpty() || realZone.getID().equals(explicitZone)) {
                zones[1] = realZone.getDisplayName(false, TimeZone.SHORT, Locale.getDefault());
                zones[2] = realZone.useDaylightTime() ? realZone.getDisplayName(true, TimeZone.SHORT, Locale.getDefault()) : "";
            } else {
                zones[1] = explicitZone;
                zones[2] = "";
            }
            this.zone = RDataFactory.createStringVector(zones, RDataFactory.COMPLETE_VECTOR);
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

        public TimeZone getRealZone() {
            return realZone;
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
    public abstract static class Date2POSIXlt extends RBuiltinNode.Arg1 {

        @Child private GetNamesAttributeNode getNamesNode = GetNamesAttributeNode.create();

        static {
            Casts casts = new Casts(Date2POSIXlt.class);
            casts.arg("x").mustNotBeMissing().mapIf(nullValue(), emptyDoubleVector()).asDoubleVector();
        }

        @Specialization
        @TruffleBoundary
        protected RList doDate2POSIXlt(RDoubleVector x) {
            int xLen = x.getLength();
            POSIXltBuilder builder = new POSIXltBuilder(xLen, "UTC");
            for (int i = 0; i < xLen; i++) {
                double d = x.getDataAt(i);
                if (RRuntime.isFinite(d)) {
                    int day = (int) Math.floor(d);
                    Instant instant = Instant.ofEpochSecond(day * 3600L * 24L);
                    ZonedDateTime date = ZonedDateTime.ofInstant(instant, builder.getRealZone().toZoneId());
                    boolean dst = builder.getRealZone().inDaylightTime(Date.from(instant));
                    builder.setEntry(i, 0, 0, 0, date.getDayOfMonth(), date.getMonthValue() - 1, date.getYear() - 1900, date.getDayOfWeek().ordinal(), date.getDayOfYear(), dst ? 1 : 0);
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
    public abstract static class AsPOSIXlt extends RBuiltinNode.Arg2 {

        @Child private GetNamesAttributeNode getNamesNode = GetNamesAttributeNode.create();

        static {
            Casts casts = new Casts(AsPOSIXlt.class);
            casts.arg("x").mapNull(emptyDoubleVector()).mustBe(missingValue().not()).asDoubleVector(true, false, false);
            casts.arg("tz").mustNotBeMissing().asStringVector().findFirst("");
        }

        @Specialization
        @TruffleBoundary
        protected RList asPOSIXlt(RDoubleVector x, String tz) {
            int xLen = x.getLength();
            POSIXltBuilder builder = new POSIXltBuilder(xLen, tz);
            for (int i = 0; i < xLen; i++) {
                double second = x.getDataAt(i);
                if (RRuntime.isFinite(second)) {
                    Instant instant = Instant.ofEpochSecond((long) second);
                    double miliseconds = second - Math.floor(second);
                    ZonedDateTime date = ZonedDateTime.ofInstant(instant, builder.getRealZone().toZoneId());
                    boolean dst = builder.getRealZone().inDaylightTime(Date.from(instant));
                    builder.setEntry(i, date.getSecond() + miliseconds, date.getMinute(), date.getHour(), date.getDayOfMonth(), date.getMonthValue() - 1, date.getYear() - 1900,
                                    date.getDayOfWeek().ordinal(),
                                    date.getDayOfYear(), dst ? 1 : 0);
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
    public abstract static class AsPOSIXct extends RBuiltinNode.Arg2 {

        static {
            Casts casts = new Casts(AsPOSIXct.class);
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
                zone = RContext.getInstance().stateREnvVars.getSystemTimeZone();
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
    public abstract static class POSIXlt2Date extends RBuiltinNode.Arg1 {
        private static final RStringVector CLASS_ATTR = (RStringVector) RDataFactory.createStringVectorFromScalar("Date").makeSharedPermanent();

        static {
            Casts casts = new Casts(POSIXlt2Date.class);
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
            setClassAttrNode.setAttr(result, CLASS_ATTR);
            return result;
        }
    }

    @RBuiltin(name = "format.POSIXlt", kind = INTERNAL, parameterNames = {"x", "format", "usetz"}, behavior = READS_STATE)
    public abstract static class FormatPOSIXlt extends RBuiltinNode.Arg3 {

        private static final HashMap<String, String> TIME_ZONE_MAPPING = new HashMap<>();

        static {
            TIME_ZONE_MAPPING.put("EST", "America/New_York");
            TIME_ZONE_MAPPING.put("EDT", "America/New_York");
            // TODO: find a proper source for this mapping
        }

        static {
            Casts casts = new Casts(FormatPOSIXlt.class);
            casts.arg("x").mustBe(RAbstractListVector.class);
            casts.arg("format").asStringVector().mustBe(notEmpty());
            casts.arg("usetz").asLogicalVector().findFirst(RRuntime.LOGICAL_FALSE).map(toBoolean());
        }

        @Specialization
        @TruffleBoundary
        protected RStringVector format(RAbstractListVector x, RStringVector format, boolean usetz) {
            RDoubleVector secVector = (RDoubleVector) RRuntime.asAbstractVector(x.getDataAt(0));
            RIntVector minVector = (RIntVector) RRuntime.asAbstractVector(x.getDataAt(1));
            RIntVector hourVector = (RIntVector) RRuntime.asAbstractVector(x.getDataAt(2));
            RIntVector mdayVector = (RIntVector) RRuntime.asAbstractVector(x.getDataAt(3));
            RIntVector monVector = (RIntVector) RRuntime.asAbstractVector(x.getDataAt(4));
            RIntVector yearVector = (RIntVector) RRuntime.asAbstractVector(x.getDataAt(5));
            ZoneId zone;
            DateTimeFormatterBuilder[] builders = createFormatters(format, false);
            String tzone = getTimeZomeFromAttribute(x);
            if (usetz && !tzone.isEmpty()) {
                zone = ZoneId.of(tzone, TIME_ZONE_MAPPING);
                for (DateTimeFormatterBuilder builder : builders) {
                    builder.appendLiteral(' ').appendZoneText(TextStyle.SHORT);
                }
            } else {
                zone = RContext.getInstance().stateREnvVars.getSystemTimeZone().toZoneId();
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
    public abstract static class StrPTime extends RBuiltinNode.Arg3 {

        static {
            Casts casts = new Casts(StrPTime.class);
            casts.arg("x").mapNull(emptyStringVector()).mustBe(missingValue().not()).asStringVector();
            casts.arg("format").mapNull(emptyStringVector()).mustBe(missingValue().not()).asStringVector();
            casts.arg("tz").mapNull(emptyStringVector()).mustBe(missingValue().not()).asStringVector().mustBe(size(1).or(size(0)));
        }

        @Specialization
        @TruffleBoundary
        protected RList strptime(RStringVector x, RStringVector format, RStringVector tz) {
            String zoneString = RRuntime.asString(tz);
            int length = x.getLength();
            TimeZone timeZone = TimeZone.getDefault();
            if (tz.getLength() > 0) {
                timeZone = TimeZone.getTimeZone(tz.getDataAt(0));
            }
            POSIXltBuilder builder = new POSIXltBuilder(length, zoneString);
            DateTimeFormatterBuilder[] builders = createFormatters(format, true);
            DateTimeFormatter[] formatters = new DateTimeFormatter[builders.length];
            for (int i = 0; i < builders.length; i++) {
                formatters[i] = builders[i].toFormatter().withChronology(LeapYearChronology.INSTANCE);
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
                    LocalDateTime time;
                    try {
                        time = LocalDateTime.from(parse);
                    } catch (DateTimeException e) {
                        // Try just time and use current date
                        LocalTime tm = LocalTime.from(parse);
                        time = LocalDateTime.of(LocalDate.now(), tm);
                    }
                    ZoneOffset zoneOffset = timeZone.toZoneId().getRules().getOffset(time);
                    double ms = (time.toInstant(zoneOffset).toEpochMilli() % 1000) / 1000.0;
                    boolean dst = builder.getRealZone().inDaylightTime(java.util.Date.from(time.toLocalDate().atStartOfDay(timeZone.toZoneId()).toInstant()));
                    builder.setEntry(i, time.getSecond() + ms, time.getMinute(), time.getHour(), time.getDayOfMonth(), time.getMonthValue() - 1, time.getYear() - 1900, time.getDayOfWeek().ordinal(),
                                    time.getDayOfYear(), dst ? 1 : 0);
                    continue;
                } catch (DateTimeException e) {
                    // try without time
                }
                try {
                    LocalDate date = LocalDate.from(parse);
                    boolean dst = builder.getRealZone().inDaylightTime(java.util.Date.from(date.atStartOfDay(timeZone.toZoneId()).toInstant()));
                    builder.setEntry(i, 0, 0, 0, date.getDayOfMonth(), date.getMonthValue() - 1, date.getYear() - 1900, date.getDayOfWeek().ordinal(), date.getDayOfYear(), dst ? 1 : 0);
                } catch (DateTimeException e) {
                    throw RInternalError.shouldNotReachHere(e);
                }
            }
            return builder.finish();
        }
    }

    private static DateTimeFormatterBuilder[] createFormatters(RStringVector formats, boolean forInput) {
        DateTimeFormatterBuilder[] result = new DateTimeFormatterBuilder[formats.getLength()];
        for (int i = 0; i < result.length; i++) {
            result[i] = createFormatter(formats.getDataAt(i), forInput);
        }
        return result;
    }

    private static DateTimeFormatterBuilder createFormatter(String format, boolean forInput) {
        DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder();
        if (forInput) {
            // Lenient parsing required to parse datetimes like "2002-6-24-0-0-10"
            builder.parseLenient();
        }
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
                    case 'H':
                        /*
                         * Hours as decimal number (00–23). As a special exception strings such as
                         * 24:00:00 are accepted for input, since ISO 8601 allows these. For output
                         * 00-23 is required thus using HOUR_OF_DAY.
                         */
                        if (forInput) {
                            builder.appendValue(ChronoField.CLOCK_HOUR_OF_DAY, 1, 2, SignStyle.NOT_NEGATIVE);
                        } else {
                            builder.appendValue(ChronoField.HOUR_OF_DAY, 2);
                        }
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
                        if (forInput) {
                            builder.appendValue(ChronoField.MINUTE_OF_HOUR, 1, 2, SignStyle.NOT_NEGATIVE);
                        } else {
                            builder.appendValue(ChronoField.MINUTE_OF_HOUR, 2);
                        }
                        break;
                    case 'n':
                        // Newline on output, arbitrary whitespace on input.
                        builder.appendLiteral('\n');
                        break;
                    case 'O':
                        int fLen = format.length();
                        if (i + 1 < fLen && format.charAt(i + 1) == 'S') {
                            builder.appendValue(ChronoField.SECOND_OF_MINUTE, 2);
                            i++;
                            if (i + 1 < fLen && format.charAt(i + 1) == '3') {
                                builder.appendLiteral('.').appendValue(ChronoField.MILLI_OF_SECOND, 3);
                                i++;
                            } else if (i + 1 < fLen && format.charAt(i + 1) == '6') {
                                builder.appendLiteral('.').appendValue(ChronoField.MICRO_OF_SECOND, 6);
                                i++;
                            }
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
                        if (forInput) {
                            builder.appendValue(ChronoField.SECOND_OF_MINUTE, 1, 2, SignStyle.NOT_NEGATIVE);
                        } else {
                            builder.appendValue(ChronoField.SECOND_OF_MINUTE, 2);
                        }
                        break;
                    case 't':
                        // Tab on output, arbitrary whitespace on input.
                        builder.appendLiteral('\t');
                        break;
                    case 'T':
                        // Equivalent to %H:%M:%S.
                        builder.appendValue(ChronoField.CLOCK_HOUR_OF_DAY, forInput ? 1 : 2).appendLiteral(':').appendValue(ChronoField.MINUTE_OF_HOUR, forInput ? 1 : 2).appendLiteral(
                                        ':').appendValue(
                                                        ChronoField.SECOND_OF_MINUTE, forInput ? 1 : 2);
                        break;
                    case 'u':
                        // Weekday as a decimal number (1–7, Monday is 1).
                        builder.appendValue(ChronoField.DAY_OF_WEEK);
                        break;
                    case 'x':
                        // Date. Locale-specific on output, "%y/%m/%d" on input.
                        if (forInput) {
                            builder.append(createFormatter("%y/%m/%d", forInput).toFormatter());
                        } else {
                            builder.appendLocalized(FormatStyle.SHORT, null);
                        }
                        break;
                    case 'X':
                        // Time. Locale-specific on output, "%H:%M:%S" on input.
                        if (forInput) {
                            builder.append(createFormatter("%H:%M:%S", forInput).toFormatter());
                        } else {
                            builder.appendLocalized(null, FormatStyle.SHORT);
                        }
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
                    case 'Z':
                        /*
                         * (Output only.) Time zone abbreviation as a character string (empty if not
                         * available). This may not be reliable when a time zone has changed
                         * abbreviations over the years.
                         */
                        builder.appendZoneText(TextStyle.SHORT);
                        break;
                    case 'z':
                        /*
                         * Signed offset in hours and minutes from UTC, so -0800 is 8 hours behind
                         * UTC. Values up to +1400 are accepted as from R 3.1.1: previous versions
                         * only accepted up to +1200. (Standard only for output.)
                         */
                        builder.appendPattern("Z");
                        break;
                    // Following formatters are not implemented and fall through to error
                    case 'g':
                        /*
                         * The last two digits of the week-based year (see %V). (Accepted but
                         * ignored on input.)
                         */
                    case 'G':
                        /*
                         * The week-based year (see %V) as a decimal number. (Accepted but ignored
                         * on input.)
                         */
                    case 'U':
                        /*
                         * Week of the year as decimal number (00–53) using Sunday as the first day
                         * 1 of the week (and typically with the first Sunday of the year as day 1
                         * of week 1). The US convention.
                         */
                    case 'V':
                        /*
                         * Week of the year as decimal number (01–53) as defined in ISO 8601. If the
                         * week (starting on Monday) containing 1 January has four or more days in
                         * the new year, then it is considered week 1. Otherwise, it is the last
                         * week of the previous year, and the next week is week 1. (Accepted but
                         * ignored on input.)
                         */
                    case 'w':
                        // Weekday as decimal number (0–6, Sunday is 0).
                    case 'W':
                        /*
                         * Week of the year as decimal number (00–53) using Monday as the first day
                         * of week (and typically with the first Monday of the year as day 1 of week
                         * 1). The UK convention.
                         */
                        throw RError.error(RError.NO_CALLER, Message.DATE_TIME_CONVERSION_SPEC_NOT_IMPLEMENTED, c);
                    default:
                        builder.appendLiteral(c);
                        break;
                }
                escaped = false;
            } else {
                if (c == '%') {
                    escaped = true;
                } else if (forInput && Character.isWhitespace(c)) {
                    builder.appendPattern("['\t']['    ']['  '][' ']['\t']");
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
        if (v instanceof RDoubleVector) {
            return ((RDoubleVector) v).getDataAt(i);
        } else if (v instanceof RIntVector) {
            return ((RIntVector) v).getDataAt(i);
        } else {
            throw RInternalError.shouldNotReachHere();
        }
    }

    private static int getInt(RAbstractVector v, int index) {
        int i = index % v.getLength();
        if (v instanceof RIntVector) {
            return ((RIntVector) v).getDataAt(i);
        } else if (v instanceof RDoubleVector) {
            return (int) ((RDoubleVector) v).getDataAt(i);
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
        Object attr = DynamicObjectLibrary.getUncached().getOrDefault(x.getAttributes(), "tzone", "");
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

    private static final class LeapYearChronology implements Chronology {

        static LeapYearChronology INSTANCE = new LeapYearChronology(IsoChronology.INSTANCE);

        private static final int[] maxDayOfMonths = new int[]{31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

        private final Chronology delegate;

        LeapYearChronology(Chronology delegate) {
            this.delegate = delegate;
        }

        @Override
        public String getId() {
            return delegate.getId();
        }

        @Override
        public String getCalendarType() {
            return delegate.getCalendarType();
        }

        @Override
        public ChronoLocalDate date(Era era, int yearOfEra, int month, int dayOfMonth) {
            return delegate.date(era, yearOfEra, month, dayOfMonth);
        }

        @Override
        public ChronoLocalDate date(int prolepticYear, int month, int dayOfMonth) {
            return delegate.date(prolepticYear, month, dayOfMonth);
        }

        @Override
        public ChronoLocalDate dateYearDay(Era era, int yearOfEra, int dayOfYear) {
            return delegate.dateYearDay(era, yearOfEra, dayOfYear);
        }

        @Override
        public ChronoLocalDate dateYearDay(int prolepticYear, int dayOfYear) {
            return delegate.dateYearDay(prolepticYear, dayOfYear);
        }

        @Override
        public ChronoLocalDate dateEpochDay(long epochDay) {
            return delegate.dateEpochDay(epochDay);
        }

        @Override
        public ChronoLocalDate dateNow() {
            return delegate.dateNow();
        }

        @Override
        public ChronoLocalDate dateNow(ZoneId zone) {
            return delegate.dateNow(zone);
        }

        @Override
        public ChronoLocalDate dateNow(Clock clock) {
            return delegate.dateNow(clock);
        }

        @Override
        public ChronoLocalDate date(TemporalAccessor temporal) {
            return delegate.date(temporal);
        }

        @Override
        public ChronoLocalDateTime<? extends ChronoLocalDate> localDateTime(TemporalAccessor temporal) {
            return delegate.localDateTime(temporal);
        }

        @Override
        public ChronoZonedDateTime<? extends ChronoLocalDate> zonedDateTime(TemporalAccessor temporal) {
            return delegate.zonedDateTime(temporal);
        }

        @Override
        public ChronoZonedDateTime<? extends ChronoLocalDate> zonedDateTime(Instant instant, ZoneId zone) {
            return delegate.zonedDateTime(instant, zone);
        }

        @Override
        public boolean isLeapYear(long prolepticYear) {
            return delegate.isLeapYear(prolepticYear);
        }

        @Override
        public int prolepticYear(Era era, int yearOfEra) {
            return delegate.prolepticYear(era, yearOfEra);
        }

        @Override
        public Era eraOf(int eraValue) {
            return delegate.eraOf(eraValue);
        }

        @Override
        public List<Era> eras() {
            return delegate.eras();
        }

        @Override
        public ValueRange range(ChronoField field) {
            return delegate.range(field);
        }

        @Override
        public String getDisplayName(TextStyle style, Locale locale) {
            return delegate.getDisplayName(style, locale);
        }

        @Override
        public ChronoLocalDate resolveDate(Map<TemporalField, Long> fieldValues, ResolverStyle resolverStyle) {
            // date(int prolepticYear, int month, int dayOfMonth) not called -> handle here
            Long day = fieldValues.get(ChronoField.DAY_OF_MONTH);
            if (day != null && day >= 29) {
                Long month = fieldValues.get(ChronoField.MONTH_OF_YEAR);
                if (month != null && month <= 12) {
                    if (month == 2 && day == 29) {
                        Long year = fieldValues.get(ChronoField.YEAR);
                        if (year != null && !isLeapYear(year)) {
                            throw new DateTimeException("Invalid date 'February 29' as '" + year + "' is not a leap year");
                        }
                    } else {
                        int monthInt = (int) (long) month;
                        if (day > maxDayOfMonths[(monthInt) - 1]) {
                            throw new DateTimeException("Invalid date '" + Month.of(monthInt).name() + " " + day + "'");
                        }
                    }
                }
            }
            return delegate.resolveDate(fieldValues, resolverStyle);
        }

        @Override
        public ChronoPeriod period(int years, int months, int days) {
            return delegate.period(years, months, days);
        }

        @Override
        public int compareTo(Chronology other) {
            return delegate.compareTo(other);
        }

        @Override
        public boolean equals(Object obj) {
            return delegate.equals(obj);
        }

        @Override
        public int hashCode() {
            return delegate.hashCode();
        }

        @Override
        public String toString() {
            return delegate.toString();
        }
    }
}
