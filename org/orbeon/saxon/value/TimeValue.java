package org.orbeon.saxon.value;

import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.functions.Component;
import org.orbeon.saxon.om.FastStringBuffer;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.*;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.Err;
import org.orbeon.saxon.sort.ComparisonKey;

import java.math.BigDecimal;
import java.util.*;

/**
 * A value of type xs:time
 */

public final class TimeValue extends CalendarValue {

    private byte hour;
    private byte minute;
    private byte second;
    private int microsecond;

    /**
     * Construct a time value given the hour, minute, second, and microsecond components.
     * This constructor performs no validation.
     * @param hour the hour value, 0-23
     * @param minute the minutes value, 0-59
     * @param second the seconds value, 0-59
     * @param microsecond the number of microseconds, 0-999999
     * @param tz the timezone displacement in minutes from UTC. Supply the value
     * {@link CalendarValue#NO_TIMEZONE} if there is no timezone component.
     */

    public TimeValue(byte hour, byte minute, byte second, int microsecond, int tz) {
        this.hour = hour;
        this.minute = minute;
        this.second = second;
        this.microsecond = microsecond;
        setTimezoneInMinutes(tz);
    }

    /**
     * Constructor: create a time value given a Java calendar object
     * @param calendar holds the date and time
     * @param tz the timezone offset in minutes, or NO_TIMEZONE indicating that there is no timezone
     */

    public TimeValue(GregorianCalendar calendar, int tz) {
        hour = (byte)(calendar.get(Calendar.HOUR_OF_DAY));
        minute = (byte)(calendar.get(Calendar.MINUTE));
        second = (byte)(calendar.get(Calendar.SECOND));
        microsecond = calendar.get(Calendar.MILLISECOND) * 1000;
        setTimezoneInMinutes(tz);
    }

    /**
     * Constructor: create a dateTime value from a supplied string, in
     * ISO 8601 format
     */

    public TimeValue(CharSequence s) throws XPathException {
        // input must have format hh:mm:ss[.fff*][([+|-]hh:mm | Z)]

        StringTokenizer tok = new StringTokenizer(Whitespace.trimWhitespace(s).toString(), "-:.+Z", true);
        try {
            if (!tok.hasMoreElements()) badTime("too short", s);
            String part = (String) tok.nextElement();

            if (part.length() != 2) badTime("hour must be two digits", s);
            hour = (byte)Integer.parseInt(part);
            if (hour > 24) badTime("hour is out of range", s);
            if (!tok.hasMoreElements()) badTime("too short", s);
            if (!":".equals(tok.nextElement())) badTime("wrong delimiter after hour", s);

            if (!tok.hasMoreElements()) badTime("too short", s);
            part = (String) tok.nextElement();
            if (part.length() != 2) badTime("minute must be two digits", s);
            minute = (byte)Integer.parseInt(part);
            if (minute > 59) badTime("minute is out of range", s);
            if (hour == 24 && minute != 0) badTime("If hour is 24, minute must be 00", s);
            if (!tok.hasMoreElements()) badTime("too short", s);
            if (!":".equals(tok.nextElement())) badTime("wrong delimiter after minute", s);

            if (!tok.hasMoreElements()) badTime("too short", s);
            part = (String) tok.nextElement();
            if (part.length() != 2) badTime("second must be two digits", s);
            second = (byte)Integer.parseInt(part);
            if (second > 59) badTime("second is out of range", s);
            if (hour == 24 && second != 0) badTime("If hour is 24, second must be 00", s);

            int tz = 0;

            int state = 0;
            while (tok.hasMoreElements()) {
                if (state == 9) {
                    badTime("characters after the end", s);
                }
                String delim = (String) tok.nextElement();
                if (".".equals(delim)) {
                    if (state != 0) {
                        badTime("decimal separator occurs twice", s);
                    }
                    part = (String) tok.nextElement();
                    double fractionalSeconds = Double.parseDouble('.' + part);
                    microsecond = (int) (Math.round(fractionalSeconds * 1000000));
                    if (hour == 24 && microsecond != 0) {
                        badTime("If hour is 24, fractional seconds must be 0", s);
                    }
                    state = 1;
                } else if ("Z".equals(delim)) {
                    if (state > 1) {
                        badTime("Z cannot occur here", s);
                    }
                    tz = 0;
                    state = 9;  // we've finished
                    setTimezoneInMinutes(0);
                } else if ("+".equals(delim) || "-".equals(delim)) {
                    if (state > 1) {
                        badTime(delim + " cannot occur here", s);
                    }
                    state = 2;
                    if (!tok.hasMoreElements()) badTime("missing timezone", s);
                    part = (String) tok.nextElement();
                    if (part.length() != 2) badTime("timezone hour must be two digits", s);
                    tz = Integer.parseInt(part) * 60;
                    if (tz > 14 * 60) badTime("timezone hour is out of range", s);
                    //if (tz > 12 * 60) badTime("Because of Java limitations, Saxon currently limits the timezone to +/- 12 hours");
                    if ("-".equals(delim)) tz = -tz;
                } else if (":".equals(delim)) {
                    if (state != 2) {
                        badTime("colon cannot occur here", s);
                    }
                    state = 9;
                    part = (String) tok.nextElement();
                    int tzminute = Integer.parseInt(part);
                    if (part.length() != 2) badTime("timezone minute must be two digits", s);
                    if (tzminute > 59) badTime("timezone minute is out of range", s);
                    if (tz < 0) tzminute = -tzminute;
                    tz += tzminute;
                    setTimezoneInMinutes(tz);
                } else {
                    badTime("timezone format is incorrect", s);
                }
            }

            if (state == 2 || state == 3) {
                badTime("timezone incomplete", s);
            }

            if (hour == 24) {
                hour = 0;
            }

        } catch (NumberFormatException err) {
            badTime("non-numeric component", s);
        }
    }

    private void badTime(String msg, CharSequence value) throws XPathException {
        ValidationException err = new ValidationException(
                "Invalid time " + Err.wrap(value, Err.VALUE) + " (" + msg + ")");
        err.setErrorCode("FORG0001");
        throw err;
    }

    /**
     * Convert to target data type
     * @param requiredType an integer identifying the required atomic type
     * @param context
     * @return an AtomicValue, a value of the required type; or an ErrorValue
     */

    public AtomicValue convertPrimitive(BuiltInAtomicType requiredType, boolean validate, XPathContext context) {
        switch (requiredType.getPrimitiveType()) {
            case Type.TIME:
            case Type.ANY_ATOMIC:
            case Type.ITEM:
                return this;
            case Type.STRING:
                return new StringValue(getStringValueCS());
            case Type.UNTYPED_ATOMIC:
                return new UntypedAtomicValue(getStringValueCS());
            default:
                ValidationException err = new ValidationException("Cannot convert time to " +
                        requiredType.getDisplayName());
                err.setErrorCode("XPTY0004");
                err.setIsTypeError(true);
                return new ValidationErrorValue(err);
        }
    }

    /**
     * Convert to string
     * @return ISO 8601 representation, in the localized timezone
     * (the timezone held within the value).
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
                sb.append((char)(d+'0'));
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
     * Determine the data type of the expression
     * @return Type.TIME_TYPE,
     * @param th
     */

    public ItemType getItemType(TypeHierarchy th) {
        return Type.TIME_TYPE;
    }

    /**
     * Make a copy of this date, time, or dateTime value
     */

    public CalendarValue copy() {
        return new TimeValue(hour, minute, second, microsecond, getTimezoneInMinutes());
    }

    /**
     * Return a new time with the same normalized value, but
     * in a different timezone. This is called only for a TimeValue that has an explicit timezone
     * @param timezone the new timezone offset, in minutes
     * @return the time in the new timezone. This will be a new TimeValue unless no change
     * was required to the original value
     */

    public CalendarValue adjustTimezone(int timezone) {
        DateTimeValue dt = (DateTimeValue)toDateTime().adjustTimezone(timezone);
        return new TimeValue(dt.getHour(), dt.getMinute(), dt.getSecond(),
                dt.getMicrosecond(), dt.getTimezoneInMinutes());
    }


    /**
     * Convert to Java object (for passing to external functions)
     */

    public Object convertToJava(Class target, XPathContext context) throws XPathException {
        if (target.isAssignableFrom(TimeValue.class)) {
            return this;
        } else if (target == String.class) {
            return getStringValue();
        } else if (target == Object.class) {
            return getStringValue();
        } else {
            Object o = super.convertToJava(target, context);
            if (o == null) {
                throw new DynamicError("Conversion of time to " + target.getName() +
                        " is not supported");
            }
            return o;
        }
    }

    /**
     * Get a component of the value. Returns null if the timezone component is
     * requested and is not present.
     */

    public AtomicValue getComponent(int component) throws XPathException {
        switch (component) {
            case Component.HOURS:
                return new IntegerValue(hour);
            case Component.MINUTES:
                return new IntegerValue(minute);
            case Component.SECONDS:
                BigDecimal d = BigDecimal.valueOf(microsecond);
                d = d.divide(DecimalValue.BIG_DECIMAL_ONE_MILLION, 6, BigDecimal.ROUND_HALF_UP);
                d = d.add(BigDecimal.valueOf(second));
                return new DecimalValue(d);
            case Component.MICROSECONDS:
                return new IntegerValue(microsecond);             
            case Component.TIMEZONE:
                if (hasTimezone()) {
                    return SecondsDurationValue.fromMilliseconds(getTimezoneInMinutes()*60000);
                } else {
                    return null;
                }
            default:
                throw new IllegalArgumentException("Unknown component for time: " + component);
        }
    }

    /**
     * Compare the value to another dateTime value
     * @param other The other dateTime value
     * @return negative value if this one is the earler, 0 if they are chronologically equal,
     * positive value if this one is the later. For this purpose, dateTime values with an unknown
     * timezone are considered to be UTC values (the Comparable interface requires
     * a total ordering).
     * @throws ClassCastException if the other value is not a DateTimeValue (the parameter
     * is declared as Object to satisfy the Comparable interface)
     */

    public int compareTo(Object other) {
        if (!(other instanceof TimeValue)) {
            throw new ClassCastException("Time values are not comparable to " + other.getClass());
        }

        TimeValue otherTime = (TimeValue)other;
        if (getTimezoneInMinutes() == otherTime.getTimezoneInMinutes()) {
            if (hour != otherTime.hour) {
                return (hour - otherTime.hour);
            } else if (minute != otherTime.minute) {
                return (minute - otherTime.minute);
            } else if (second != otherTime.second) {
                return (second - otherTime.second);
            } else if (microsecond != otherTime.microsecond) {
                return (microsecond - otherTime.microsecond);
            } else {
                return 0;
            }
        } else {
            return toDateTime().compareTo(otherTime.toDateTime());
        }
    }

    /**
     * Compare the value to another dateTime value
     * @param other The other dateTime value
     * @return negative value if this one is the earler, 0 if they are chronologically equal,
     * positive value if this one is the later. For this purpose, dateTime values with an unknown
     * timezone are considered to be UTC values (the Comparable interface requires
     * a total ordering).
     * @throws ClassCastException if the other value is not a DateTimeValue (the parameter
     * is declared as Object to satisfy the Comparable interface)
     */

    public int compareTo(CalendarValue other, Configuration config) {
        if (!(other instanceof TimeValue)) {
            throw new ClassCastException("Time values are not comparable to " + other.getClass());
        }
        TimeValue otherTime = (TimeValue)other;
        if (getTimezoneInMinutes() == otherTime.getTimezoneInMinutes()) {
            // The values have the same time zone, or neither has a timezone
            return compareTo(other);
        } else {
            return toDateTime().compareTo(otherTime.toDateTime(), config);
        }
    }
    
    /**
     * Get a comparison key for this value. Two values are equal if and only if they their comparison
     * keys are equal
     */

    public ComparisonKey getComparisonKey(Configuration config) {
        return new ComparisonKey(Type.TIME, toDateTime().normalize(config));
    }



    public boolean equals(Object other) {
        return compareTo(other) == 0;
    }

    public int hashCode() {
        return toDateTime().hashCode();
    }

    /**
     * Add a duration to a dateTime
     * @param duration the duration to be added (may be negative)
     * @return the new date
     * @throws org.orbeon.saxon.trans.XPathException if the duration is an xs:duration, as distinct from
     * a subclass thereof
     */

    public CalendarValue add(DurationValue duration) throws XPathException {
        if (duration instanceof SecondsDurationValue) {
            DateTimeValue dt = (DateTimeValue)toDateTime().add(duration);
            return new TimeValue(dt.getHour(), dt.getMinute(), dt.getSecond(),
                    dt.getMicrosecond(), getTimezoneInMinutes());
        } else {
            DynamicError err = new DynamicError(
                    "Time+Duration arithmetic is supported only for xdt:dayTimeDuration");
            err.setErrorCode("XPTY0004");
            err.setIsTypeError(true);
            throw err;
        }
    }

    /**
     * Determine the difference between two points in time, as a duration
     * @param other the other point in time
     * @param context
     * @return the duration as an xdt:dayTimeDuration
     * @throws org.orbeon.saxon.trans.XPathException for example if one value is a date and the other is a time
     */

    public SecondsDurationValue subtract(CalendarValue other, XPathContext context) throws XPathException {
        if (!(other instanceof TimeValue)) {
            DynamicError err = new DynamicError(
                    "First operand of '-' is a time, but the second is not");
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

