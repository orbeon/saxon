package org.orbeon.saxon.value;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.functions.Component;
import org.orbeon.saxon.style.StandardNames;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.xpath.DynamicError;
import org.orbeon.saxon.xpath.XPathException;

import java.util.*;

/**
 * A value of type xs:time
 */

public final class TimeValue extends CalendarValue implements Comparable {


    /**
     * Constructor: create a time value given a Java calendar object
     * @param calendar holds the date and time
     * @param tzSpecified indicates whether the timezone is specified
     */

    public TimeValue(GregorianCalendar calendar, boolean tzSpecified) {
        this.calendar = calendar;
        zoneSpecified = tzSpecified;
    }

    /**
     * Constructor: create a dateTime value from a supplied string, in
     * ISO 8601 format
     */

    public TimeValue(CharSequence s) throws XPathException {
        // input must have format hh:mm:ss[.fff*][([+|-]hh:mm | Z)]

        zoneSpecified = false;
        StringTokenizer tok = new StringTokenizer(trimWhitespace(s).toString(), "-:.+Z", true);
        try {
            if (!tok.hasMoreElements()) badTime("too short");
            String part = (String) tok.nextElement();

            int hour = Integer.parseInt(part);
            if (part.length() != 2) badTime("hour must be two digits");
            if (hour > 23) badTime("hour is out of range");
            if (!tok.hasMoreElements()) badTime("too short");
            if (!":".equals(tok.nextElement())) badTime("wrong delimiter after hour");

            if (!tok.hasMoreElements()) badTime("too short");
            part = (String) tok.nextElement();
            int minute = Integer.parseInt(part);
            if (part.length() != 2) badTime("minute must be two digits");
            if (minute > 59) badTime("minute is out of range");
            if (!tok.hasMoreElements()) badTime("too short");
            if (!":".equals(tok.nextElement())) badTime("wrong delimiter after minute");

            if (!tok.hasMoreElements()) badTime("too short");
            part = (String) tok.nextElement();
            int second = Integer.parseInt(part);
            if (part.length() != 2) badTime("second must be two digits");
            if (hour > 61) badTime("second is out of range");

            int millisecond = 0;
            int tz = 0;

            int state = 0;
            while (tok.hasMoreElements()) {
                if (state == 9) {
                    badTime("characters after the end");
                }
                String delim = (String) tok.nextElement();
                if (".".equals(delim)) {
                    if (state != 0) {
                        badTime("decimal separator occurs twice");
                    }
                    part = (String) tok.nextElement();
                    double fractionalSeconds = Double.parseDouble("." + part);
                    millisecond = (int) (Math.round(fractionalSeconds * 1000));
                    state = 1;
                } else if ("Z".equals(delim)) {
                    if (state > 1) {
                        badTime("Z cannot occur here");
                    }
                    zoneSpecified = true;
                    tz = 0;
                    state = 9;  // we've finished
                } else if ("+".equals(delim) || "-".equals(delim)) {
                    if (state > 1) {
                        badTime(delim + " cannot occur here");
                    }
                    state = 2;
                    zoneSpecified = true;
                    if (!tok.hasMoreElements()) badTime("missing timezone");
                    part = (String) tok.nextElement();
                    if (part.length() != 2) badTime("timezone hour must be two digits");
                    tz = Integer.parseInt(part) * 60;
                    if (tz > 14 * 60) badTime("timezone hour is out of range");
                    if ("-".equals(delim)) tz = -tz;

                } else if (":".equals(delim)) {
                    if (state != 2) {
                        badTime("colon cannot occur here");
                    }
                    state = 9;
                    part = (String) tok.nextElement();
                    int tzminute = Integer.parseInt(part);
                    if (part.length() != 2) badTime("timezone minute must be two digits");
                    if (tzminute > 59) badTime("timezone minute is out of range");
                    if (tz < 0) tzminute = -tzminute;
                    tz += tzminute;
                } else {
                    badTime("timezone format is incorrect");
                }
            }

            if (state == 2 || state == 3) {
                badTime("timezone incomplete");
            }

            // create a calendar using the specified timezone
            TimeZone zone = new SimpleTimeZone(tz * 60000, "LLL");
            calendar = new GregorianCalendar(zone);
            calendar.setLenient(false);
            calendar.set(2000, 0, 1, hour, minute, second);
            calendar.set(Calendar.MILLISECOND, millisecond);
            calendar.set(Calendar.ZONE_OFFSET, tz * 60000);
            calendar.set(Calendar.DST_OFFSET, 0);

            try {
                calendar.getTime();
            } catch (IllegalArgumentException err) {
                badTime("time components out of range");
            }


        } catch (NumberFormatException err) {
            badTime("non-numeric component");
        }
    }

    private void badTime(String msg) throws XPathException {
        throw new DynamicError("Invalid time value (" + msg + ")");
    }

    /**
     * Convert to target data type
     * @param requiredType an integer identifying the required atomic type
     * @return an AtomicValue, a value of the required type
     * @throws XPathException if the conversion is not possible
     */

    public AtomicValue convert(int requiredType, XPathContext context) throws XPathException {
        switch (requiredType) {
            case Type.TIME:
            case Type.ATOMIC:
            case Type.ITEM:
                return this;
            case Type.STRING:
                return new StringValue(getStringValue());
            case Type.UNTYPED_ATOMIC:
                return new UntypedAtomicValue(getStringValue());
//            case Type.DATE_TIME:
//                return new DateTimeValue(calendar, zoneSpecified);
            default:
                DynamicError err = new DynamicError("Cannot convert time to " +
                        StandardNames.getDisplayName(requiredType));
                err.setXPathContext(context);
                err.setErrorCode("FORG0001");
                throw err;
        }
    }

    /**
     * Convert to string
     * @return ISO 8601 representation, in the localized timezone
     * (the timezone held within the value).
     */

    public String getStringValue() {

        StringBuffer sb = new StringBuffer(16);

        DateTimeValue.appendString(sb, calendar.get(Calendar.HOUR_OF_DAY), 2);
        sb.append(':');
        DateTimeValue.appendString(sb, calendar.get(Calendar.MINUTE), 2);
        sb.append(':');
        DateTimeValue.appendSeconds(calendar, sb);

        if (zoneSpecified) {
            DateTimeValue.appendTimezone(calendar, sb);
        }

        return sb.toString();

    }

    /**
     * Convert to a DateTime value (used internally only: the date components should be ignored)
     */

    public DateTimeValue toDateTime() {
        return new DateTimeValue(calendar, zoneSpecified);
    }

    /**
     * Determine the data type of the exprssion
     * @return Type.TIME,
     */

    public ItemType getItemType() {
        return Type.TIME_TYPE;
    }

    /**
     * Return a dateTime with the same localized value, but
     * without the timezone component
     * @return the result of removing the timezone
     */

    public CalendarValue removeTimezone() throws XPathException {
        return (CalendarValue)
                new DateTimeValue(calendar, zoneSpecified)
                .removeTimezone()
                .convert(Type.TIME, null);
    }

    /**
     * Return a date, time, or dateTime with the same normalized value, but
     * in a different timezone
     * @return the date/time in the new timezone
     * @throws XPathException
     */

    public CalendarValue setTimezone(SecondsDurationValue tz) throws XPathException {
        return (CalendarValue)
                new DateTimeValue(calendar, zoneSpecified)
                .setTimezone(tz)
                .convert(Type.TIME, null);
    }

    /**
     * Convert to Java object (for passing to external functions)
     */

    public Object convertToJava(Class target, Configuration config, XPathContext context) throws XPathException {
        if (target.isAssignableFrom(TimeValue.class)) {
            return this;
        } else if (target == String.class) {
            return getStringValue();
        } else if (target == Object.class) {
            return getStringValue();
        } else {
            Object o = super.convertToJava(target, config, context);
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
                return new IntegerValue(calendar.get(Calendar.HOUR_OF_DAY));
            case Component.MINUTES:
                return new IntegerValue(calendar.get(Calendar.MINUTE));
            case Component.SECONDS:
                StringBuffer sb = new StringBuffer(10);
                DateTimeValue.appendSeconds(calendar, sb);
                return new DecimalValue(sb.toString());
            case Component.TIMEZONE:
                if (zoneSpecified) {
                    int tzsecs = (calendar.get(Calendar.ZONE_OFFSET) +
                            calendar.get(Calendar.DST_OFFSET));
                    return SecondsDurationValue.fromMilliseconds(tzsecs);
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
        if (zoneSpecified == otherTime.zoneSpecified) {
            GregorianCalendar cal2 = otherTime.calendar;
            return calendar.getTime().compareTo(cal2.getTime());
        } else {
            return new DateTimeValue(calendar, zoneSpecified).compareTo(
                    new DateTimeValue(otherTime.calendar, otherTime.zoneSpecified));
        }
    }

    public boolean equals(Object other) {
        return compareTo(other) == 0;
    }

    public int hashCode() {
        return new DateTimeValue(calendar, zoneSpecified).hashCode();
    }

    /**
     * Add a duration to a dateTime
     * @param duration the duration to be added (may be negative)
     * @return the new date
     * @throws XPathException if the duration is an xs:duration, as distinct from
     * a subclass thereof
     */

    public CalendarValue add(DurationValue duration) throws XPathException {
        if (duration instanceof SecondsDurationValue) {
            double seconds = duration.getLengthInSeconds();
            GregorianCalendar cal2 = (GregorianCalendar) calendar.clone();
            cal2.add(Calendar.SECOND, (int) seconds);
            cal2.add(Calendar.MILLISECOND, (int) ((seconds % 1) * 1000));
            return new TimeValue(cal2, zoneSpecified);
        } else {
            DynamicError err = new DynamicError(
                    "Time+Duration arithmetic is supported only for xdt:dayTimeDuration");
            err.setIsTypeError(true);
            throw err;
        }
    }

    /**
     * Determine the difference between two points in time, as a duration
     * @param other the other point in time
     * @return the duration as an xdt:dayTimeDuration
     * @throws XPathException for example if one value is a date and the other is a time
     */

    public SecondsDurationValue subtract(CalendarValue other) throws XPathException {
        if (!(other instanceof TimeValue)) {
            DynamicError err = new DynamicError(
                    "First operand of '-' is a time, but the second is not");
            err.setIsTypeError(true);
            throw err;
        }
        return super.subtract(other);
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

