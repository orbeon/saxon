package org.orbeon.saxon.value;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.Controller;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.functions.Component;
import org.orbeon.saxon.style.StandardNames;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.xpath.DynamicError;
import org.orbeon.saxon.xpath.XPathException;

import java.math.BigDecimal;
import java.util.*;

/**
* A value of type DateTime
*/

public final class DateTimeValue extends CalendarValue implements Comparable {

    // UTCDate is held as a redundant representation of the
    // normalized (UTC) value.
    private Date UTCDate = null;   // Always read this using getUTCDate


    /**
    * Constructor: create a dateTime value representing the nominal
    * date/time of this transformation run. Two calls within the same
    * transformation will always return the same answer.
    */

    public DateTimeValue(XPathContext context) {
        Controller c = context.getController();
        if (c==null) {
            // non-XSLT environment
            calendar = new GregorianCalendar();
        } else {
            calendar = c.getCurrentDateTime();
        }
        zoneSpecified = true;
    }

    /**
    * Constructor: create a dateTime value given a Java calendar object
    * @param calendar holds the date and time
    * @param tzSpecified indicates whether the timezone is specified
    */

    public DateTimeValue(GregorianCalendar calendar, boolean tzSpecified) {
        this.calendar = calendar;
        zoneSpecified = tzSpecified;
    }

    /**
     * Constructor: create a dateTime value given a date and a time.
     * @param date the date
     * @param time the time
     * @throws XPathException if the timezones are both present and inconsistent
     */

    public DateTimeValue(DateValue date, TimeValue time) throws XPathException {
        SecondsDurationValue tz1 = (SecondsDurationValue)date.getComponent(Component.TIMEZONE);
        SecondsDurationValue tz2 = (SecondsDurationValue)time.getComponent(Component.TIMEZONE);
        zoneSpecified = (tz1 != null || tz2 != null);
        if (tz1 != null && tz2 != null && !tz1.equals(tz2)) {
            DynamicError err = new DynamicError("Supplied date and time are in different timezones");
            err.setErrorCode("FORG0008");
            throw err;
        }
        // create a calendar that uses the timezone actually specified, or GMT otherwise
        //calendar = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        TimeZone zone;
        SecondsDurationValue tz = (tz1 == null ? tz2 : tz1);
        int zoneOffset = (tz == null ? 0 : (int)tz.getLengthInMilliseconds());
        zone = (tz == null ? TimeZone.getTimeZone("GMT") : new SimpleTimeZone(zoneOffset, "LLL"));
        calendar = new GregorianCalendar(zone);
        calendar.setLenient(false);
        final int year = (int)((IntegerValue)date.getComponent(Component.YEAR)).longValue();
        final int month = (int)((IntegerValue)date.getComponent(Component.MONTH)).longValue();
        final int day = (int)((IntegerValue)date.getComponent(Component.DAY)).longValue();
        final int hour = (int)((IntegerValue)time.getComponent(Component.HOURS)).longValue();
        final int minute = (int)((IntegerValue)time.getComponent(Component.MINUTES)).longValue();
        final BigDecimal secs = ((DecimalValue)time.getComponent(Component.SECONDS)).getValue();
        final int second = secs.intValue();
        final int millisec = secs.multiply(BigDecimal.valueOf(1000)).intValue() % 1000;

        calendar.set(Math.abs(year), month-1, day, hour, minute, second);
        if (year < 0) {
            calendar.set(Calendar.ERA, GregorianCalendar.BC);
        }
        calendar.set(Calendar.MILLISECOND, millisec);
        calendar.set(Calendar.ZONE_OFFSET, zoneOffset);
        calendar.set(Calendar.DST_OFFSET, 0);
    }
    /**
    * Constructor: create a dateTime value from a supplied string, in
    * ISO 8601 format
    */

    public DateTimeValue(CharSequence s) throws XPathException {
        // input must have format yyyy-mm-ddThh:mm:ss[.fff*][([+|-]hh:mm | Z)]
        zoneSpecified = false;
        StringTokenizer tok = new StringTokenizer(trimWhitespace(s).toString(), "-:.+TZ", true);
        try {
            if (!tok.hasMoreElements()) badDate("too short");
            String part = (String)tok.nextElement();
            int era = +1;
            if ("+".equals(part)) {
                part = (String)tok.nextElement();
            } else if ("-".equals(part)) {
                era = -1;
                part = (String)tok.nextElement();
            }
            int year = Integer.parseInt(part) * era;
            if (part.length() < 4) badDate("Year is less than four digits");
            if (year==0) badDate("Year zero is not allowed");
            if (!tok.hasMoreElements()) badDate("Too short");
            if (!"-".equals(tok.nextElement())) badDate("Wrong delimiter after year");

            if (!tok.hasMoreElements()) badDate("Too short");
            part = (String)tok.nextElement();
            int month = Integer.parseInt(part);
            if (part.length() != 2) badDate("Month must be two digits");
            if (month < 1 || month > 12) badDate("Month is out of range");
            if (!tok.hasMoreElements()) badDate("Too short");
            if (!"-".equals(tok.nextElement())) badDate("Wrong delimiter after month");

            if (!tok.hasMoreElements()) badDate("Too short");
            part = (String)tok.nextElement();
            int day = Integer.parseInt(part);
            if (part.length() != 2) badDate("Day must be two digits");
            if (day < 1 || day > 31) badDate("Day is out of range");
            if (!tok.hasMoreElements()) badDate("Too short");
            if (!"T".equals(tok.nextElement())) badDate("Wrong delimiter after day");

            if (!tok.hasMoreElements()) badDate("Too short");
            part = (String)tok.nextElement();
            int hour = Integer.parseInt(part);
            if (part.length() != 2) badDate("Hour must be two digits");
            if (hour > 23) badDate("Hour is out of range");
            if (!tok.hasMoreElements()) badDate("Too short");
            if (!":".equals(tok.nextElement())) badDate("Wrong delimiter after hour");

            if (!tok.hasMoreElements()) badDate("Too short");
            part = (String)tok.nextElement();
            int minute = Integer.parseInt(part);
            if (part.length() != 2) badDate("Minute must be two digits");
            if (minute > 59) badDate("Minute is out of range");
            if (!tok.hasMoreElements()) badDate("Too short");
            if (!":".equals(tok.nextElement())) badDate("Wrong delimiter after minute");

            if (!tok.hasMoreElements()) badDate("Too short");
            part = (String)tok.nextElement();
            int second = Integer.parseInt(part);
            if (part.length() != 2) badDate("Second must be two digits");
            if (hour > 61) badDate("Second is out of range");

            int millisecond = 0;
            int tz = 0;

            int state = 0;
            while (tok.hasMoreElements()) {
                if (state==9) {
                    badDate("Characters after the end");
                }
                String delim = (String)tok.nextElement();
                if (".".equals(delim)) {
                    if (state != 0) {
                        badDate("Decimal separator occurs twice");
                    }
                    part = (String)tok.nextElement();
                    double fractionalSeconds = Double.parseDouble('.' + part);
                    millisecond = (int)(Math.round(fractionalSeconds * 1000));
                    state = 1;
                } else if ("Z".equals(delim)) {
                    if (state > 1) {
                        badDate("Z cannot occur here");
                    }
                    zoneSpecified = true;
                    tz = 0;
                    state = 9;  // we've finished
                } else if ("+".equals(delim) || "-".equals(delim)) {
                    if (state > 1) {
                        badDate(delim + " cannot occur here");
                    }
                    state = 2;
                    zoneSpecified = true;
                    if (!tok.hasMoreElements()) badDate("Missing timezone");
                    part = (String)tok.nextElement();
                    if (part.length() != 2) badDate("Timezone hour must be two digits");
                    tz = Integer.parseInt(part) * 60;
                    if (tz > 14*60) badDate("Timezone hour is out of range");
                    if ("-".equals(delim)) tz = -tz;

                } else if (":".equals(delim)) {
                    if (state != 2) {
                        badDate("Misplaced ':'");
                    }
                    state = 9;
                    part = (String)tok.nextElement();
                    int tzminute = Integer.parseInt(part);
                    if (part.length() != 2) badDate("Timezone minute must be two digits");
                    if (tzminute > 59) badDate("Timezone minute is out of range");
                    if (tz<0) tzminute = -tzminute;
                    tz += tzminute;
                } else {
                    badDate("Timezone format is incorrect");
                }
            }

            if (state == 2 || state == 3) {
                badDate("Timezone incomplete");
            }

            // create a calendar that uses the timezone actually specified, or GMT otherwise
            //calendar = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
            TimeZone zone = new SimpleTimeZone(tz*60000, "LLL");
            calendar = new GregorianCalendar(zone);
            calendar.setLenient(false);
            calendar.set(Math.abs(year), month-1, day, hour, minute, second);
            if (year < 0) {
                calendar.set(Calendar.ERA, GregorianCalendar.BC);
            }
            calendar.set(Calendar.MILLISECOND, millisecond);
            calendar.set(Calendar.ZONE_OFFSET, tz*60000);
            calendar.set(Calendar.DST_OFFSET, 0);

            try {
                calendar.getTime();
            } catch (IllegalArgumentException err) {
                badDate("Non-existent date");
            }


        } catch (NumberFormatException err) {
            badDate("Non-numeric component");
        }
    }

    private void badDate(String msg) throws XPathException {
        DynamicError err = new DynamicError("Invalid dateTime value. " + msg);
        err.setErrorCode("FORG0001");
        throw err;
    }


    /**
    * Get the UTC date/time corresponding to this dateTime. This normalizes
    * the value to incorporate the timezone information, for example
    * 2002-01-01T07:00:00-05:00 gives the same answer as 2002-01-01T12:00:00Z
    */

    public Date getUTCDate() {
        // implement this as a memo function
        if (UTCDate==null) {
            UTCDate = calendar.getTime();
        }
        return UTCDate;
    }

    /**
     * Get the Calendar object representing the value of this DateTime
     */

    public Calendar getCalendar() {
        return calendar;
    }

    /**
    * Convert to target data type
    * @param requiredType an integer identifying the required atomic type
    * @return an AtomicValue, a value of the required type
    * @throws XPathException if the conversion is not possible
    */

    public AtomicValue convert(int requiredType, XPathContext context) throws XPathException {
        switch(requiredType) {
        case Type.DATE_TIME:
        case Type.ATOMIC:
        case Type.ITEM:
            return this;
        case Type.DATE:
            String ds = getStringValue();
            int sep = ds.indexOf('T');
            if (zoneSpecified) {
                int z = ds.indexOf('Z', sep);
                if (z < 0) {
                    z = ds.indexOf('+', sep);
                }
                if (z < 0) {
                    z = ds.indexOf('-', sep);
                }
                if (z < 0) {
                    // something's gone wrong
                    throw new IllegalArgumentException("Internal date formatting error " + ds);
                }
                return new DateValue(ds.substring(0, sep) + ds.substring(z));
            } else {
                return new DateValue(ds.substring(0, sep));
            }
        case Type.TIME:
            ds = getStringValue();
            sep = ds.indexOf('T');
            return new TimeValue(ds.substring(sep+1));

        case Type.G_YEAR:
            return(convert(Type.DATE, context).convert(Type.G_YEAR, context));

        case Type.G_YEAR_MONTH:
            return(convert(Type.DATE, context).convert(Type.G_YEAR_MONTH, context));

        case Type.G_MONTH:
            return(convert(Type.DATE, context).convert(Type.G_MONTH, context));

        case Type.G_MONTH_DAY:
            return(convert(Type.DATE, context).convert(Type.G_MONTH_DAY, context));

        case Type.G_DAY:
            return(convert(Type.DATE, context).convert(Type.G_DAY, context));

        case Type.STRING:
            return new StringValue(getStringValue());

        case Type.UNTYPED_ATOMIC:
            return new UntypedAtomicValue(getStringValue());
        default:
            DynamicError err = new DynamicError("Cannot convert dateTime to " +
                                     StandardNames.getDisplayName(requiredType));
            err.setXPathContext(context);
            err.setErrorCode("FORG0001");
            throw err;
        }
    }

    /**
    * Convert to string
    * @return ISO 8601 representation. The value returned is the localized representation,
     * that is it uses the timezone contained within the value itself.
    */

    public String getStringValue() {

        StringBuffer sb = new StringBuffer(30);
        int era = calendar.get(GregorianCalendar.ERA);
        int year = calendar.get(Calendar.YEAR);
        if (era == GregorianCalendar.BC) {
            sb.append('-');
        }
        if (year<0) {
            sb.append('-');
            year = -year;
        }
        appendString(sb, year, (year>9999 ? (calendar.get(Calendar.YEAR)+"").length() : 4));
        sb.append('-');
        appendString(sb, calendar.get(Calendar.MONTH)+1, 2);
        sb.append('-');
        appendString(sb, calendar.get(Calendar.DATE), 2);
        sb.append('T');
        appendString(sb, calendar.get(Calendar.HOUR_OF_DAY), 2);
        sb.append(':');
        appendString(sb, calendar.get(Calendar.MINUTE), 2);
        sb.append(':');
        appendSeconds(calendar, sb);

        if (zoneSpecified) {
            appendTimezone(calendar, sb);
        }

        return sb.toString();

    }

    static void appendString(StringBuffer sb, int value, int size) {
        String s = "000"+value;
        sb.append( s.substring(s.length()-size) );
    }

    static void appendSeconds(Calendar calendar, StringBuffer sb) {
        appendString(sb, calendar.get(Calendar.SECOND), 2);
        int millis = calendar.get(Calendar.MILLISECOND);
        if (millis != 0) {
            sb.append('.');
            String m = calendar.get(Calendar.MILLISECOND) + "";
            while (m.length() < 3) m = '0' + m;
            while (m.endsWith("0")) m = m.substring(0, m.length()-1);
            sb.append(m);
        }
        return;
    }

    /**
     * Add a string representation of the timezone, typically
     * formatted as "Z" or "+03:00" or "-10:00", to the supplied
     * string buffer
     * @param calendar The Calendar whose timezone value is required
     * @param sb The StringBuffer that will be updated with the resulting string
     * representation
     */

    public static void appendTimezone(Calendar calendar, StringBuffer sb) {
        int timeZoneOffset = (calendar.get(Calendar.ZONE_OFFSET) +
                              calendar.get(Calendar.DST_OFFSET)) / 60000;
        appendTimezone(timeZoneOffset, sb);
    }

     /**
     * Add a string representation of the timezone, typically
     * formatted as "Z" or "+03:00" or "-10:00", to the supplied
     * string buffer
     * @param timeZoneOffset The timezone offset in minutes
     * @param sb The StringBuffer that will be updated with the resulting string
     * representation
     */

    static void appendTimezone(int timeZoneOffset, StringBuffer sb) {
        if (timeZoneOffset == 0) {
            sb.append('Z');
        } else {
            sb.append((timeZoneOffset<0 ? "-" : "+"));
            int tzo = timeZoneOffset;
            if (tzo < 0) tzo = -tzo;
            int tzhours = tzo / 60;
            appendString(sb, tzhours, 2);
            sb.append(':');
            int tzminutes = tzo % 60;
            appendString(sb, tzminutes, 2);
        }
        return;
    }

    /**
    * Determine the data type of the exprssion
    * @return Type.DATE_TIME,
    */

    public ItemType getItemType() {
        return Type.DATE_TIME_TYPE;
    }

    /**
     * Return a dateTime with the same localized value, but
     * without the timezone component
     * @return the result of removing the timezone
     */

    public CalendarValue removeTimezone() {
        return new DateTimeValue(calendar, false);
    }

    /**
     * Return a date, time, or dateTime with the same normalized value, but
     * in a different timezone
     * @return the date/time in the new timezone
     * @throws XPathException
     */

    public CalendarValue setTimezone(SecondsDurationValue tz) throws XPathException {
        if (zoneSpecified) {
            SimpleTimeZone stz = new SimpleTimeZone((int)(tz.getLengthInSeconds()*1000), "xxz");
            GregorianCalendar cal = new GregorianCalendar(stz);
            cal.setTime(getUTCDate());
            return new DateTimeValue(cal, true);
        } else {
            StringBuffer sb = new StringBuffer(10);
            sb.append(getStringValue());
            appendTimezone((int)(tz.getLengthInSeconds()/60.0), sb);
            return new DateTimeValue(sb);
        }
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
            GregorianCalendar cal2 = (GregorianCalendar)calendar.clone();
            cal2.add(Calendar.SECOND, (int)seconds);
            cal2.add(Calendar.MILLISECOND, (int)((seconds % 1) * 1000));
            return new DateTimeValue(cal2, zoneSpecified);
        } else if (duration instanceof MonthDurationValue) {
            int months = ((MonthDurationValue)duration).getLengthInMonths();
            GregorianCalendar cal2 = (GregorianCalendar)calendar.clone();
            cal2.add(Calendar.MONTH, months);
            return new DateTimeValue(cal2, zoneSpecified);
        } else {
            DynamicError err = new DynamicError(
                    "DateTime arithmetic is not supported on xs:duration, only on its subtypes");
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
        if (!(other instanceof DateTimeValue)) {
            DynamicError err = new DynamicError(
                    "First operand of '-' is a dateTime, but the second is not");
            err.setIsTypeError(true);
            throw err;
        }
        return super.subtract(other);
    }

    /**
    * Convert to Java object (for passing to external functions)
    */

    public Object convertToJava(Class target, Configuration config, XPathContext context) throws XPathException {
        if (target.isAssignableFrom(Date.class)) {
            return getUTCDate();
        } else if (target.isAssignableFrom(DateTimeValue.class)) {
            return this;
        } else if (target==String.class || target==CharSequence.class) {
            return getStringValue();
        } else if (target==Object.class) {
            return getStringValue();
        } else {
            Object o = super.convertToJava(target, config, context);
            if (o == null) {
                throw new DynamicError("Conversion of dateTime to " + target.getName() +
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
        case Component.YEAR:
            return new IntegerValue(calendar.get(Calendar.YEAR));
        case Component.MONTH:
            return new IntegerValue(calendar.get(Calendar.MONTH) + 1);
        case Component.DAY:
            return new IntegerValue(calendar.get(Calendar.DATE));
        case Component.HOURS:
            return new IntegerValue(calendar.get(Calendar.HOUR_OF_DAY));
        case Component.MINUTES:
            return new IntegerValue(calendar.get(Calendar.MINUTE));
        case Component.SECONDS:
            StringBuffer sb = new StringBuffer(10);
            appendSeconds(calendar, sb);
            return new DecimalValue(sb);
        case Component.TIMEZONE:
            if (zoneSpecified) {
                int tzmsecs = (calendar.get(Calendar.ZONE_OFFSET) +
                              calendar.get(Calendar.DST_OFFSET));
                return SecondsDurationValue.fromMilliseconds(tzmsecs);
            } else {
                return null;
            }
        default:
            throw new IllegalArgumentException("Unknown component for dateTime: " + component);
        }
    }


    /**
    * Compare the value to another dateTime value
    * @param other The other dateTime value
    * @return negative value if this one is the earler, 0 if they are chronologically equal,
    * positive value if this one is the later. For this purpose, dateTime values with an unknown
    * timezone are considered to be values in the implicit timezone (the Comparable interface requires
    * a total ordering).
    * @throws ClassCastException if the other value is not a DateTimeValue (the parameter
    * is declared as Object to satisfy the Comparable interface)
    */

    public int compareTo(Object other) {
        // TODO: getting the implicit timezone technically requires access to the context, because it is
        // not allowed to change during the course of a query/transformation. Getting it directly from Java,
        // as we do here, has a small chance of giving wrong results if the query spans a change to or from
        // daylight savings time.
        if (!(other instanceof DateTimeValue)) {
            throw new ClassCastException("DateTime values are not comparable to " + other.getClass());
        }
        DateTimeValue v1 = this;
        DateTimeValue v2 = (DateTimeValue)other;
        if (v1.zoneSpecified == v2.zoneSpecified) {
            // both have a timezone, or neither has a timezone
            return getUTCDate().compareTo(((DateTimeValue)other).getUTCDate());
        } else {
            // one has a timezone and the other doesn't
            try {
                GregorianCalendar cal = new GregorianCalendar();
                int tz = (cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET)) / 1000;
                SecondsDurationValue tzd = SecondsDurationValue.fromSeconds(tz);
                if (!v1.zoneSpecified) {
                    v1 = (DateTimeValue)v1.setTimezone(tzd);
                }
                if (!v2.zoneSpecified) {
                    v2 = (DateTimeValue)v2.setTimezone(tzd);
                }
                return v1.getUTCDate().compareTo(v2.getUTCDate());
            } catch (XPathException e) {
                throw new AssertionError("Java timezone is out of range");
            }
        }
    }

    public boolean equals(Object other) {
        return compareTo(other) == 0;
    }

    public int hashCode() {
        if (zoneSpecified) {
            return getUTCDate().hashCode();
        } else {
            try {
                GregorianCalendar cal = new GregorianCalendar();
                int tz = (cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET)) / 60000;
                SecondsDurationValue tzd = SecondsDurationValue.fromSeconds(tz);
                DateTimeValue v1 = (DateTimeValue)setTimezone(tzd);
                return v1.getUTCDate().hashCode();
            } catch (XPathException e) {
                throw new AssertionError("Java timezone is out of range");
            }
        }
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

