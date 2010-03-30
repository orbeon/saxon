package org.orbeon.saxon.value;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.FastStringBuffer;
import org.orbeon.saxon.sort.ComparisonKey;
import org.orbeon.saxon.sort.StringCollator;
import org.orbeon.saxon.trans.NoDynamicContextException;
import org.orbeon.saxon.trans.XPathException;

import java.math.BigDecimal;
import java.util.GregorianCalendar;



/**
* Abstract superclass for Date, Time, and DateTime.
*/

public abstract class CalendarValue extends AtomicValue {

    // This is a reimplementation that makes no use of the Java Calendar/Date types except for computations.

    private int tzMinutes = NO_TIMEZONE;  // timezone offset in minutes: or the special value NO_TIMEZONE
    public static final int NO_TIMEZONE = Integer.MIN_VALUE;

    /**
     * Determine whether this value includes a timezone
     * @return true if there is a timezone in the value, false if not
     */

    public final boolean hasTimezone() {
        return tzMinutes != NO_TIMEZONE;
    }

    /**
     * Modify the timezone value held in this object. This must be done only while the value is being
     * constructed.
     * @param minutes The timezone offset from GMT in minutes, positive or negative; or the special
     * value NO_TIMEZONE indicating that the value is not in a timezone (this is the default if this
     * method is not called)
     */

    public final void setTimezoneInMinutes(int minutes) {
        tzMinutes = minutes;
    }

    /**
     * Convert the value to a DateTime, retaining all the components that are actually present, and
     * substituting conventional values for components that are missing
     * @return the equivalent DateTimeValue
     */

    public abstract DateTimeValue toDateTime();

   /**
     * Get the timezone value held in this object.
     * @return The timezone offset from GMT in minutes, positive or negative; or the special
     * value NO_TIMEZONE indicating that the value is not in a timezone
     */

    public final int getTimezoneInMinutes() {
        return tzMinutes;
    }

    /**
     * Convert the value to a string
     */

    public final String getStringValue() {
        return getStringValueCS().toString();
    }

    /**
     * Get a Java Calendar object that represents this date/time value. The Calendar
     * object will be newly created for the purpose
     * @return A Calendar object representing the date and time. Note that Java can only
     * represent the time to millisecond precision, and that it does not support the full
     * range of timezones required by XPath (-14:00 to +14:00)
     */

    public abstract GregorianCalendar getCalendar();

    /**
     * Add a duration to this date/time value
     * @param duration the duration to be added (which might be negative)
     * @return a new date/time value representing the result of adding the duration. The original
     * object is not modified.
     * @throws XPathException
     */

    public abstract CalendarValue add(DurationValue duration) throws XPathException;

    /**
     * Determine the difference between two points in time, as a duration
     * @param other the other point in time
     * @param context the dynamic context, used to obtain timezone information. May be set to null
     * only if both values contain an explicit timezone, or if neither does so.
     * @return the duration as an xs:dayTimeDuration
     * @throws org.orbeon.saxon.trans.XPathException for example if one value is a date and the other is a time
     */

    public DayTimeDurationValue subtract(CalendarValue other, XPathContext context) throws XPathException {
        DateTimeValue dt1 = toDateTime();
        DateTimeValue dt2 = other.toDateTime();
        if (dt1.getTimezoneInMinutes() != dt2.getTimezoneInMinutes()) {
            dt1 = dt1.normalize(context);
            dt2 = dt2.normalize(context);
        }
        BigDecimal d1 = dt1.toJulianInstant();
        BigDecimal d2 = dt2.toJulianInstant();
        BigDecimal difference = d1.subtract(d2);
        return DayTimeDurationValue.fromSeconds(difference);
    }

    /**
     * Return a date, time, or dateTime with the same localized value, but
     * without the timezone component
     * @return the result of removing the timezone
     */

    public final CalendarValue removeTimezone() {
        CalendarValue c = (CalendarValue)copyAsSubType(typeLabel);
        c.tzMinutes = NO_TIMEZONE;
        return c;
    }

    /**
     * Return a new date, time, or dateTime with the same normalized value, but
     * in a different timezone
     * @param tz the new timezone offset from UTC, in minutes
     * @return the date/time in the new timezone
     */

    public abstract CalendarValue adjustTimezone(int tz);

   /**
     * Return a new date, time, or dateTime with the same normalized value, but
     * in a different timezone, specified as a dayTimeDuration
     * @param tz the new timezone, in minutes
     * @return the date/time in the new timezone
     */

    public final CalendarValue adjustTimezone(DayTimeDurationValue tz) throws XPathException {
        long microseconds = tz.getLengthInMicroseconds();
        if (microseconds%60000000 != 0) {
            XPathException err = new XPathException("Timezone is not an integral number of minutes");
            err.setErrorCode("FODT0003");
            throw err;
        }
        int tzminutes = (int)(microseconds / 60000000);
        if (Math.abs(tzminutes) > 14*60) {
            XPathException err = new XPathException("Timezone out of range (-14:00 to +14:00)");
            err.setErrorCode("FODT0003");
            throw err;
        }
        return adjustTimezone(tzminutes);
   }


    /**
     * Get an object value that implements the XPath equality and ordering comparison semantics for this value.
     * If the ordered parameter is set to true, the result will be a Comparable and will support a compareTo()
     * method with the semantics of the XPath lt/gt operator, provided that the other operand is also obtained
     * using the getXPathComparable() method. In all cases the result will support equals() and hashCode() methods
     * that support the semantics of the XPath eq operator, again provided that the other operand is also obtained
     * using the getXPathComparable() method. A context argument is supplied for use in cases where the comparison
     * semantics are context-sensitive, for example where they depend on the implicit timezone or the default
     * collation.
     *
     * @param ordered true if an ordered comparison is required. In this case the result is null if the
     *                type is unordered; in other cases the returned value will be a Comparable.
     * @param collator collation used for strings
     * @param context the XPath dynamic evaluation context, used in cases where the comparison is context
     *                sensitive @return an Object whose equals() and hashCode() methods implement the XPath comparison semantics
     *         with respect to this atomic value. If ordered is specified, the result will either be null if
     *         no ordering is defined, or will be a Comparable
     */

    public Object getXPathComparable(boolean ordered, StringCollator collator, XPathContext context) throws NoDynamicContextException {
        if (ordered && !(this instanceof Comparable)) {
            return null;
        }
        return (hasTimezone() ? this : adjustTimezone(context.getImplicitTimezone()));
    }

    /**
     * Compare this value to another value of the same type, using the supplied Configuration
     * to get the implicit timezone if required.
     * @param other the other value to be compared
     * @param context the XPath dynamic evaluation context
     * @return the comparison result
     * @throws NoDynamicContextException if the supplied context is an early evaluation context and the
     * result depends on the implicit timezone, which is not available at compile time
     */

    public abstract int compareTo(CalendarValue other, XPathContext context) throws NoDynamicContextException;

    /**
     * Get a comparison key for this value. Two values are equal if and only if they their comparison
     * keys are equal
     * @param context XPath dynamic evaluation context, used to obtain implicit timezone
     * @return a comparison key
     * @throws NoDynamicContextException if the implicit timezone is needed and is not available
     */

    public abstract ComparisonKey getComparisonKey(XPathContext context) throws NoDynamicContextException;

    /**
     * Add a string representation of the timezone, typically
     * formatted as "Z" or "+03:00" or "-10:00", to a supplied
     * string buffer
     * @param sb The StringBuffer that will be updated with the resulting string
     * representation
     */

    public final void appendTimezone(FastStringBuffer sb) {
        if (hasTimezone()) {
            appendTimezone(getTimezoneInMinutes(), sb);
        }
    }

    /**
     * Format a timezone and append it to a buffer
     * @param tz the timezone
     * @param sb the buffer
     */

    public static void appendTimezone(int tz, FastStringBuffer sb) {
        if (tz == 0) {
            sb.append("Z");
        } else {
            sb.append(tz > 0 ? "+" : "-");
            tz = Math.abs(tz);
            appendTwoDigits(sb, tz/60);
            sb.append(':');
            appendTwoDigits(sb, tz%60);
        }
    }

    /**
     * Append an integer, formatted with leading zeros to a fixed size, to a string buffer
     * @param sb the string buffer
     * @param value the integer to be formatted
     * @param size the number of digits required (max 9)
     */

    static void appendString(FastStringBuffer sb, int value, int size) {
        String s = "000000000"+value;
        sb.append( s.substring(s.length()-size) );
    }

/**
     * Append an integer, formatted as two digits, to a string buffer
     * @param sb the string buffer
     * @param value the integer to be formatted (must be in the range 0..99
     */

    static void appendTwoDigits(FastStringBuffer sb, int value) {
        sb.append((char)(value/10 + '0'));
        sb.append((char)(value%10 + '0'));
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

