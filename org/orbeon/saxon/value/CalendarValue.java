package org.orbeon.saxon.value;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.FastStringBuffer;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.sort.ComparisonKey;

import java.math.BigDecimal;
import java.util.GregorianCalendar;



/**
* Abstract superclass for Date, Time, and DateTime.
*/

public abstract class CalendarValue extends AtomicValue implements Comparable {

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
     * @return the duration as an xdt:dayTimeDuration
     * @throws org.orbeon.saxon.trans.XPathException for example if one value is a date and the other is a time
     */

    public SecondsDurationValue subtract(CalendarValue other, XPathContext context) throws XPathException {
        DateTimeValue dt1 = this.toDateTime();
        DateTimeValue dt2 = other.toDateTime();
        if (dt1.getTimezoneInMinutes() != dt2.getTimezoneInMinutes()) {
            Configuration config = context.getConfiguration();
            dt1 = dt1.normalize(config);
            dt2 = dt2.normalize(config);
        }
        BigDecimal d1 = dt1.toJulianInstant();
        BigDecimal d2 = dt2.toJulianInstant();
        BigDecimal difference = d1.subtract(d2);
        return SecondsDurationValue.fromSeconds(difference);
    }

    /**
     * Return a date, time, or dateTime with the same localized value, but
     * without the timezone component
     * @return the result of removing the timezone
     */

    public final CalendarValue removeTimezone() {
        CalendarValue c = copy();
        c.tzMinutes = NO_TIMEZONE;
        return c;
    }

    /**
     * Return a new date, time, or dateTime with the same normalized value, but
     * in a different timezone
     * @param tz the new timezone, in minutes
     * @return the date/time in the new timezone
     */

    public abstract CalendarValue adjustTimezone(int tz);

    /**
     * Make a copy of this date, time, or dateTime value
     */

    public abstract CalendarValue copy();

    /**
     * Compare this value to another value of the same type, using the supplied ConversionContext
     * to get the implicit timezone if required.
     */

    public abstract int compareTo(CalendarValue other, Configuration config);

    /**
     * Get a comparison key for this value. Two values are equal if and only if they their comparison
     * keys are equal
     */

    public abstract ComparisonKey getComparisonKey(Configuration config);

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

    public static final void appendTimezone(int tz, FastStringBuffer sb) {
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

