package org.orbeon.saxon.value;

import org.orbeon.saxon.trans.Err;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.functions.Component;
import org.orbeon.saxon.om.FastStringBuffer;
import org.orbeon.saxon.om.StandardNames;
import org.orbeon.saxon.sort.ComparisonKey;
import org.orbeon.saxon.trans.NoDynamicContextException;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.AtomicType;
import org.orbeon.saxon.type.BuiltInAtomicType;
import org.orbeon.saxon.type.ConversionResult;
import org.orbeon.saxon.type.ValidationFailure;

import java.math.BigDecimal;
import java.util.*;

/**
 * A value of type xs:time
 */

public final class TimeValue extends CalendarValue implements Comparable {

    private byte hour;
    private byte minute;
    private byte second;
    private int microsecond;

    private TimeValue() {
    }

    /**
     * Construct a time value given the hour, minute, second, and microsecond components.
     * This constructor performs no validation.
     *
     * @param hour        the hour value, 0-23
     * @param minute      the minutes value, 0-59
     * @param second      the seconds value, 0-59
     * @param microsecond the number of microseconds, 0-999999
     * @param tz          the timezone displacement in minutes from UTC. Supply the value
     *                    {@link CalendarValue#NO_TIMEZONE} if there is no timezone component.
     */

    public TimeValue(byte hour, byte minute, byte second, int microsecond, int tz) {
        this.hour = hour;
        this.minute = minute;
        this.second = second;
        this.microsecond = microsecond;
        setTimezoneInMinutes(tz);
        typeLabel = BuiltInAtomicType.TIME;
    }

    /**
     * Constructor: create a time value given a Java calendar object
     *
     * @param calendar holds the date and time
     * @param tz       the timezone offset in minutes, or NO_TIMEZONE indicating that there is no timezone
     */

    public TimeValue(GregorianCalendar calendar, int tz) {
        hour = (byte)(calendar.get(Calendar.HOUR_OF_DAY));
        minute = (byte)(calendar.get(Calendar.MINUTE));
        second = (byte)(calendar.get(Calendar.SECOND));
        microsecond = calendar.get(Calendar.MILLISECOND) * 1000;
        setTimezoneInMinutes(tz);
        typeLabel = BuiltInAtomicType.TIME;
    }

    /**
     * Static factory method: create a time value from a supplied string, in
     * ISO 8601 format
     *
     * @param s the time in the lexical format hh:mm:ss[.ffffff] followed optionally by
     *          timezone in the form [+-]hh:mm or Z
     * @return either a TimeValue corresponding to the xs:time, or a ValidationFailure
     *         if the supplied value was invalid
     */

    public static ConversionResult makeTimeValue(CharSequence s) throws XPathException {
        // input must have format hh:mm:ss[.fff*][([+|-]hh:mm | Z)]
        TimeValue tv = new TimeValue();
        StringTokenizer tok = new StringTokenizer(Whitespace.trimWhitespace(s).toString(), "-:.+Z", true);
        if (!tok.hasMoreElements()) {
            return badTime("too short", s);
        }
        String part = (String)tok.nextElement();

        if (part.length() != 2) {
            return badTime("hour must be two digits", s);
        }
        int value = DurationValue.simpleInteger(part);
        if (value < 0) {
            return badTime("Non-numeric hour component", s);
        }
        tv.hour = (byte)value;
        if (tv.hour > 24) {
            return badTime("hour is out of range", s);
        }
        if (!tok.hasMoreElements()) {
            return badTime("too short", s);
        }
        if (!":".equals(tok.nextElement())) {
            return badTime("wrong delimiter after hour", s);
        }

        if (!tok.hasMoreElements()) {
            return badTime("too short", s);
        }
        part = (String)tok.nextElement();
        if (part.length() != 2) {
            return badTime("minute must be two digits", s);
        }
        value = DurationValue.simpleInteger(part);
        if (value < 0) {
            return badTime("Non-numeric minute component", s);
        }
        tv.minute = (byte)value;
        if (tv.minute > 59) {
            return badTime("minute is out of range", s);
        }
        if (tv.hour == 24 && tv.minute != 0) {
            return badTime("If hour is 24, minute must be 00", s);
        }
        if (!tok.hasMoreElements()) {
            return badTime("too short", s);
        }
        if (!":".equals(tok.nextElement())) {
            return badTime("wrong delimiter after minute", s);
        }

        if (!tok.hasMoreElements()) {
            return badTime("too short", s);
        }
        part = (String)tok.nextElement();
        if (part.length() != 2) {
            return badTime("second must be two digits", s);
        }
        value = DurationValue.simpleInteger(part);
        if (value < 0) {
            return badTime("Non-numeric second component", s);
        }
        tv.second = (byte)value;
        if (tv.second > 59) {
            return badTime("second is out of range", s);
        }
        if (tv.hour == 24 && tv.second != 0) {
            return badTime("If hour is 24, second must be 00", s);
        }

        int tz = 0;

        int state = 0;
        while (tok.hasMoreElements()) {
            if (state == 9) {
                return badTime("characters after the end", s);
            }
            String delim = (String)tok.nextElement();
            if (".".equals(delim)) {
                if (state != 0) {
                    return badTime("decimal separator occurs twice", s);
                }
                if (!tok.hasMoreElements()) {
                    return badTime("decimal point must be followed by digits", s);
                }
                part = (String)tok.nextElement();
                value = DurationValue.simpleInteger(part);
                if (value < 0) {
                    return badTime("Non-numeric fractional seconds component", s);
                }
                double fractionalSeconds = Double.parseDouble('.' + part);
                tv.microsecond = (int)(Math.round(fractionalSeconds * 1000000));
                if (tv.hour == 24 && tv.microsecond != 0) {
                    return badTime("If hour is 24, fractional seconds must be 0", s);
                }
                state = 1;
            } else if ("Z".equals(delim)) {
                if (state > 1) {
                    return badTime("Z cannot occur here", s);
                }
                tz = 0;
                state = 9;  // we've finished
                tv.setTimezoneInMinutes(0);
            } else if ("+".equals(delim) || "-".equals(delim)) {
                if (state > 1) {
                    return badTime(delim + " cannot occur here", s);
                }
                state = 2;
                if (!tok.hasMoreElements()) {
                    return badTime("missing timezone", s);
                }
                part = (String)tok.nextElement();
                if (part.length() != 2) {
                    return badTime("timezone hour must be two digits", s);
                }
                value = DurationValue.simpleInteger(part);
                if (value < 0) {
                    return badTime("Non-numeric timezone hour component", s);
                }
                tz = value * 60;
                if (tz > 14 * 60) {
                    return badTime("timezone hour is out of range", s);
                }
                //if (tz > 12 * 60) return badTime("Because of Java limitations, Saxon currently limits the timezone to +/- 12 hours");
                if ("-".equals(delim)) {
                    tz = -tz;
                }
            } else if (":".equals(delim)) {
                if (state != 2) {
                    return badTime("colon cannot occur here", s);
                }
                state = 9;
                part = (String)tok.nextElement();
                value = DurationValue.simpleInteger(part);
                if (value < 0) {
                    return badTime("Non-numeric timezone minute component", s);
                }
                int tzminute = value;
                if (part.length() != 2) {
                    return badTime("timezone minute must be two digits", s);
                }
                if (tzminute > 59) {
                    return badTime("timezone minute is out of range", s);
                }
                if (tz < 0) {
                    tzminute = -tzminute;
                }
                tz += tzminute;
                tv.setTimezoneInMinutes(tz);
            } else {
                return badTime("timezone format is incorrect", s);
            }
        }

        if (state == 2 || state == 3) {
            return badTime("timezone incomplete", s);
        }

        if (tv.hour == 24) {
            tv.hour = 0;
        }

        tv.typeLabel = BuiltInAtomicType.TIME;
        return tv;
    }

    private static ValidationFailure badTime(String msg, CharSequence value) {
        ValidationFailure err = new ValidationFailure(
                "Invalid time " + Err.wrap(value, Err.VALUE) + " (" + msg + ")");
        err.setErrorCode("FORG0001");
        return err;
    }

    /**
     * Determine the primitive type of the value. This delivers the same answer as
     * getItemType().getPrimitiveItemType(). The primitive types are
     * the 19 primitive types of XML Schema, plus xs:integer, xs:dayTimeDuration and xs:yearMonthDuration,
     * and xs:untypedAtomic. For external objects, the result is AnyAtomicType.
     */

    public BuiltInAtomicType getPrimitiveType() {
        return BuiltInAtomicType.TIME;
    }

    /**
     * Get the hour component, 0-23
     *
     * @return the hour
     */

    public byte getHour() {
        return hour;
    }

    /**
     * Get the minute component, 0-59
     *
     * @return the minute
     */

    public byte getMinute() {
        return minute;
    }

    /**
     * Get the second component, 0-59
     *
     * @return the second
     */

    public byte getSecond() {
        return second;
    }

    /**
     * Get the microsecond component, 0-999999
     *
     * @return the microseconds
     */

    public int getMicrosecond() {
        return microsecond;
    }


    /**
     * Convert to target data type
     *
     * @param requiredType an integer identifying the required atomic type
     * @param context      XPath dynamic evaluation context
     * @return an AtomicValue, a value of the required type; or an ErrorValue
     */

    public ConversionResult convertPrimitive(BuiltInAtomicType requiredType, boolean validate, XPathContext context) {
        switch (requiredType.getPrimitiveType()) {
        case StandardNames.XS_TIME:
        case StandardNames.XS_ANY_ATOMIC_TYPE:
            return this;
        case StandardNames.XS_STRING:
            return new StringValue(getStringValueCS());
        case StandardNames.XS_UNTYPED_ATOMIC:
            return new UntypedAtomicValue(getStringValueCS());
        default:
            ValidationFailure err = new ValidationFailure("Cannot convert time to " +
                    requiredType.getDisplayName());
            err.setErrorCode("XPTY0004");
            return err;
        }
    }

    /**
     * Convert to string
     *
     * @return ISO 8601 representation, in the localized timezone
     *         (the timezone held within the value).
     */

    public CharSequence getStringValueCS() {

        FastStringBuffer sb = new FastStringBuffer(16);

        appendTwoDigits(sb, hour);
        sb.append(':');
        appendTwoDigits(sb, minute);
        sb.append(':');
        appendTwoDigits(sb, second);
        if (microsecond != 0) {
            sb.append('.');
            int ms = microsecond;
            int div = 100000;
            while (ms > 0) {
                int d = ms / div;
                sb.append((char)(d + '0'));
                ms = ms % div;
                div /= 10;
            }
        }

        if (hasTimezone()) {
            appendTimezone(sb);
        }

        return sb;

    }


    /**
     * Get the canonical lexical representation as defined in XML Schema. This is not always the same
     * as the result of casting to a string according to the XPath rules. For an xs:time it is the
     * time adjusted to UTC
     *
     * @return the canonical lexical representation if defined in XML Schema
     */

    public CharSequence getCanonicalLexicalRepresentation() {
        if (hasTimezone() && getTimezoneInMinutes() != 0) {
            return adjustTimezone(0).getStringValueCS();
        } else {
            return getStringValueCS();
        }
    }



    /**
     * Convert to a DateTime value. The date components represent a reference date, as defined
     * in the spec for comparing times.
     */

    public DateTimeValue toDateTime() {
        return new DateTimeValue(1972, (byte)12, (byte)31, hour, minute, second, microsecond, getTimezoneInMinutes());
    }

    /**
     * Get a Java Calendar object corresponding to this time, on a reference date
     */

    public GregorianCalendar getCalendar() {
        // create a calendar using the specified timezone
        int tz = (hasTimezone() ? getTimezoneInMinutes() : 0);
        TimeZone zone = new SimpleTimeZone(tz * 60000, "LLL");
        GregorianCalendar calendar = new GregorianCalendar(zone);
        calendar.setLenient(false);

        // use a reference date of 1972-12-31
        int year = 1972;
        int month = 11;
        int day = 31;


        calendar.set(year, month, day, hour, minute, second);
        calendar.set(Calendar.MILLISECOND, microsecond / 1000);
        calendar.set(Calendar.ZONE_OFFSET, tz * 60000);
        calendar.set(Calendar.DST_OFFSET, 0);

        calendar.getTime();
        return calendar;
    }

    /**
     * Make a copy of this time value,
     * but with a different type label
     *
     * @param typeLabel the new type label. This must be a subtype of xs:time.
     */

    public AtomicValue copyAsSubType(AtomicType typeLabel) {
        TimeValue v = new TimeValue(hour, minute, second, microsecond, getTimezoneInMinutes());
        v.typeLabel = typeLabel;
        return v;
    }

    /**
     * Return a new time with the same normalized value, but
     * in a different timezone. This is called only for a TimeValue that has an explicit timezone
     *
     * @param timezone the new timezone offset, in minutes
     * @return the time in the new timezone. This will be a new TimeValue unless no change
     *         was required to the original value
     */

    public CalendarValue adjustTimezone(int timezone) {
        DateTimeValue dt = (DateTimeValue)toDateTime().adjustTimezone(timezone);
        return new TimeValue(dt.getHour(), dt.getMinute(), dt.getSecond(),
                dt.getMicrosecond(), dt.getTimezoneInMinutes());
    }


    /**
     * Convert to Java object (for passing to external functions)
     */

//    public Object convertAtomicToJava(Class target, XPathContext context) throws XPathException {
//        if (target.isAssignableFrom(TimeValue.class)) {
//            return this;
//        } else if (target == String.class) {
//            return getStringValue();
//        } else if (target == Object.class) {
//            return getStringValue();
//        } else {
//            Object o = super.convertSequenceToJava(target, context);
//            if (o == null) {
//                throw new XPathException("Conversion of time to " + target.getName() +
//                        " is not supported");
//            }
//            return o;
//        }
//    }
//
    /**
     * Get a component of the value. Returns null if the timezone component is
     * requested and is not present.
     */

    public AtomicValue getComponent(int component) throws XPathException {
        switch (component) {
        case Component.HOURS:
            return Int64Value.makeIntegerValue(hour);
        case Component.MINUTES:
            return Int64Value.makeIntegerValue(minute);
        case Component.SECONDS:
            BigDecimal d = BigDecimal.valueOf(microsecond);
            d = d.divide(DecimalValue.BIG_DECIMAL_ONE_MILLION, 6, BigDecimal.ROUND_HALF_UP);
            d = d.add(BigDecimal.valueOf(second));
            return new DecimalValue(d);
        case Component.WHOLE_SECONDS: //(internal use only)
            return Int64Value.makeIntegerValue(second);
        case Component.MICROSECONDS:
            return new Int64Value(microsecond);
        case Component.TIMEZONE:
            if (hasTimezone()) {
                return DayTimeDurationValue.fromMilliseconds(getTimezoneInMinutes() * 60000);
            } else {
                return null;
            }
        default:
            throw new IllegalArgumentException("Unknown component for time: " + component);
        }
    }

    /**
     * Get a Comparable value that implements the XPath ordering comparison semantics for this value.
     * Returns null if the value is not comparable according to XPath rules. The default implementation
     * returns null. This is overridden for types that allow ordered comparisons in XPath: numeric, boolean,
     * string, date, time, dateTime, yearMonthDuration, dayTimeDuration, and anyURI.
     * @param ordered true if an ordered comparison is required
     * @param collator collation to be used for strings
     * @param context XPath dynamic evaluation context
     */

//    public Object getXPathComparable(boolean ordered, StringCollator collator, XPathContext context) {
//        return this;
//    }

    /**
     * Compare the value to another dateTime value
     *
     * @param other The other dateTime value
     * @return negative value if this one is the earler, 0 if they are chronologically equal,
     *         positive value if this one is the later. For this purpose, dateTime values with an unknown
     *         timezone are considered to be UTC values (the Comparable interface requires
     *         a total ordering).
     * @throws ClassCastException if the other value is not a TimeValue (the parameter
     *                            is declared as Object to satisfy the Comparable interface)
     */

    public int compareTo(Object other) {
        TimeValue otherTime = (TimeValue)other;
        if (getTimezoneInMinutes() == otherTime.getTimezoneInMinutes()) {
            if (hour != otherTime.hour) {
                return IntegerValue.signum(hour - otherTime.hour);
            } else if (minute != otherTime.minute) {
                return IntegerValue.signum(minute - otherTime.minute);
            } else if (second != otherTime.second) {
                return IntegerValue.signum(second - otherTime.second);
            } else if (microsecond != otherTime.microsecond) {
                return IntegerValue.signum(microsecond - otherTime.microsecond);
            } else {
                return 0;
            }
        } else {
            return toDateTime().compareTo(otherTime.toDateTime());
        }
    }

    /**
     * Compare the value to another dateTime value
     *
     * @param other The other dateTime value
     * @param context the XPath dynamic evaluation context
     * @return negative value if this one is the earler, 0 if they are chronologically equal,
     *         positive value if this one is the later. For this purpose, dateTime values with an unknown
     *         timezone are considered to be UTC values (the Comparable interface requires
     *         a total ordering).
     * @throws ClassCastException if the other value is not a DateTimeValue (the parameter
     *                            is declared as Object to satisfy the Comparable interface)
     * @throws NoDynamicContextException if the implicit timezone is required and is not available
     * (because the function is called at compile time)
     */

    public int compareTo(CalendarValue other, XPathContext context) throws NoDynamicContextException {
        if (!(other instanceof TimeValue)) {
            throw new ClassCastException("Time values are not comparable to " + other.getClass());
        }
        TimeValue otherTime = (TimeValue)other;
        if (getTimezoneInMinutes() == otherTime.getTimezoneInMinutes()) {
            // The values have the same time zone, or neither has a timezone
            return compareTo(other);
        } else {
            return toDateTime().compareTo(otherTime.toDateTime(), context);
        }
    }


    public Comparable getSchemaComparable() {
        return new TimeComparable();
    }

    private class TimeComparable implements Comparable {

        public TimeValue asTimeValue() {
            return TimeValue.this;
        }
        public int compareTo(Object o) {
            if (o instanceof TimeComparable) {
                DateTimeValue dt0 = asTimeValue().toDateTime();
                DateTimeValue dt1 = ((TimeComparable)o).asTimeValue().toDateTime();
                return dt0.getSchemaComparable().compareTo(dt1.getSchemaComparable());
            } else {
                return INDETERMINATE_ORDERING;
            }
        }
        public boolean equals(Object o) {
            return compareTo(o) == 0;
        }
        public int hashCode() {
            return TimeValue.this.toDateTime().getSchemaComparable().hashCode();
        }
    }

    /**
     * Get a comparison key for this value. Two values are equal if and only if they their comparison
     * keys are equal
     * @param context XPath dynamic context
     * @throws NoDynamicContextException if the implicit timezone is required and is not available
     */

    public ComparisonKey getComparisonKey(XPathContext context) throws NoDynamicContextException {
        return new ComparisonKey(StandardNames.XS_TIME, toDateTime().normalize(context));
    }


    public boolean equals(Object other) {
        return compareTo(other) == 0;
    }

    public int hashCode() {
        return DateTimeValue.hashCode(
                1951, (byte)10, (byte)11, hour, minute, second, microsecond, getTimezoneInMinutes());
    }

    /**
     * Add a duration to a dateTime
     *
     * @param duration the duration to be added (may be negative)
     * @return the new date
     * @throws org.orbeon.saxon.trans.XPathException
     *          if the duration is an xs:duration, as distinct from
     *          a subclass thereof
     */

    public CalendarValue add(DurationValue duration) throws XPathException {
        if (duration instanceof DayTimeDurationValue) {
            DateTimeValue dt = (DateTimeValue)toDateTime().add(duration);
            return new TimeValue(dt.getHour(), dt.getMinute(), dt.getSecond(),
                    dt.getMicrosecond(), getTimezoneInMinutes());
        } else {
            XPathException err = new XPathException("Time+Duration arithmetic is supported only for xs:dayTimeDuration");
            err.setErrorCode("XPTY0004");
            err.setIsTypeError(true);
            throw err;
        }
    }

    /**
     * Determine the difference between two points in time, as a duration
     *
     * @param other   the other point in time
     * @param context XPath dynamic evaluation context
     * @return the duration as an xs:dayTimeDuration
     * @throws XPathException for example if one value is a date and the other is a time
     */

    public DayTimeDurationValue subtract(CalendarValue other, XPathContext context) throws XPathException {
        if (!(other instanceof TimeValue)) {
            XPathException err = new XPathException("First operand of '-' is a time, but the second is not");
            err.setIsTypeError(true);
            throw err;
        }
        return super.subtract(other, context);
    }


}

//
// The contents of this file are subject to the Mozilla Public License Version 1.0 (the "License");
// you may not use this file except in compliance with the License. You may obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//

