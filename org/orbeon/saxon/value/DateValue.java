package org.orbeon.saxon.value;
import org.orbeon.saxon.Err;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.functions.Component;
import org.orbeon.saxon.om.FastStringBuffer;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.BuiltInAtomicType;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.type.ValidationException;

import java.util.*;

/**
* A value of type Date. Note that a Date may include a TimeZone, and unlike the situation
* with dateTime, the timezone is part of the value space.
*/

public class DateValue extends CalendarValue implements Comparable {

    protected int tzOffset = 0;       // maintained in minutes


    // UTCDate is held as a redundant representation of the information
    private Date UTCDate = null;   // Always read this using getUTCDate

    /**
     * Default constructor needed for subtyping
     */

    protected DateValue() {}

    /**
    * Constructor: create a dateTime value from a supplied string, in
    * ISO 8601 format
    */
    public DateValue(CharSequence s) throws XPathException {
        setLexicalValue(s);
    }

    /**
     * Create a DateValue
     * @param calendar the absolute date/time value
     * @param timeZoneSpecified true if there is a timezone
     * @param timeZoneOffset the timezone offset from UTC in minutes
     */
    public DateValue(GregorianCalendar calendar, boolean timeZoneSpecified, int timeZoneOffset) {
        this.calendar = calendar;
        this.zoneSpecified = timeZoneSpecified;
        this.tzOffset = timeZoneOffset;
    }

    public void setLexicalValue(CharSequence s) throws XPathException {
        // Input must have format [+|-]yyyy-mm-dd[([+|-]hh:mm | Z)]


        zoneSpecified = false;
        StringTokenizer tok = new StringTokenizer(trimWhitespace(s).toString(), "-:+Z", true);
        try {
            if (!tok.hasMoreElements()) badDate("Too short", s);
            String part = (String)tok.nextElement();
            int era = +1;
            if ("+".equals(part)) {
                part = (String)tok.nextElement();
            } else if ("-".equals(part)) {
                era = -1;
                part = (String)tok.nextElement();
            }
            int year = Integer.parseInt(part) * era;
            if (part.length() < 4) badDate("Year is less than four digits", s);
            if (year==0) badDate("Year zero is not allowed", s);
            if (!tok.hasMoreElements()) badDate("Too short", s);
            if (!"-".equals(tok.nextElement())) badDate("Wrong delimiter after year", s);

            if (!tok.hasMoreElements()) badDate("Too short", s);
            part = (String)tok.nextElement();
            int month = Integer.parseInt(part);
            if (part.length() != 2) badDate("Month must be two digits", s);
            if (month < 1 || month > 12) badDate("Month is out of range", s);
            if (!tok.hasMoreElements()) badDate("Too short", s);
            if (!"-".equals(tok.nextElement())) badDate("Wrong delimiter after month", s);

            if (!tok.hasMoreElements()) badDate("Too short", s);
            part = (String)tok.nextElement();
            int day = Integer.parseInt(part);
            if (part.length() != 2) badDate("Day must be two digits", s);
            if (day < 1 || day > 31) badDate("Day is out of range", s);

            if (tok.hasMoreElements()) {

                String delim = (String)tok.nextElement();

                if ("Z".equals(delim)) {
                    zoneSpecified = true;
                    tzOffset = 0;
                    if (tok.hasMoreElements()) badDate("Continues after 'Z'", s);
                } else if (!(!"+".equals(delim) && !"-".equals(delim))) {
                    zoneSpecified = true;
                    if (!tok.hasMoreElements()) badDate("Missing timezone", s);
                    part = (String)tok.nextElement();
                    int tzhour = Integer.parseInt(part);
                    if (part.length() != 2) badDate("Timezone hour must be two digits", s);
                    if (tzhour > 14) badDate("Timezone hour is out of range", s);
                    if (!tok.hasMoreElements()) badDate("No minutes in timezone", s);
                    if (!":".equals(tok.nextElement())) badDate("Wrong delimiter after timezone hour", s);

                    if (!tok.hasMoreElements()) badDate("No minutes in timezone", s);
                    part = (String)tok.nextElement();
                    int tzminute = Integer.parseInt(part);
                    if (part.length() != 2) badDate("Timezone minute must be two digits", s);
                    if (tzminute > 59) badDate("Timezone minute is out of range", s);
                    if (tok.hasMoreElements()) badDate("Continues after timezone", s);

                    tzOffset = (tzhour*60 + tzminute);
                    if ("-".equals(delim)) tzOffset = -tzOffset;
                } else {
                    badDate("Timezone format is incorrect", s);
                }
            }

            TimeZone zone = new SimpleTimeZone(tzOffset*60000, "LLL");
            calendar = new GregorianCalendar(zone);
            calendar.clear();
            calendar.setLenient(false);
            calendar.set(Math.abs(year), month-1, day);
            calendar.set(Calendar.ZONE_OFFSET, tzOffset*60000);
            calendar.set(Calendar.DST_OFFSET, 0);
            if (year < 0) {
                calendar.set(Calendar.ERA, GregorianCalendar.BC);
            }
            try {
                calendar.getTime();
            } catch (IllegalArgumentException err) {
                badDate("Non-existent date", s);
            }


        } catch (NumberFormatException err) {
            badDate("Non-numeric component", s);
        }
    }

    private void badDate(String msg, CharSequence value) throws ValidationException {
        ValidationException err = new ValidationException("Invalid date " + Err.wrap(value, Err.VALUE) + ". " + msg);
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
    * Convert to target data type
    * @param requiredType an integer identifying the required atomic type
    * @return an AtomicValue, a value of the required type; or an ErrorValue
    */

    public AtomicValue convertPrimitive(BuiltInAtomicType requiredType, boolean validate) {
        switch(requiredType.getPrimitiveType()) {
        case Type.DATE:
        case Type.ATOMIC:
        case Type.ITEM:
            return this;
        case Type.DATE_TIME:
            return new DateTimeValue(calendar, zoneSpecified);

        case Type.STRING:
            return new StringValue(getStringValueCS());

        case Type.UNTYPED_ATOMIC:
            return new UntypedAtomicValue(getStringValueCS());

        case Type.G_YEAR:
            GYearValue gy = new GYearValue();
            gy.setDateValue(this);
            return gy;

        case Type.G_YEAR_MONTH:
            GYearMonthValue gmy = new GYearMonthValue();
            gmy.setDateValue(this);
            return gmy;

        case Type.G_MONTH:
            GMonthValue gm = new GMonthValue();
            gm.setDateValue(this);
            return gm;

        case Type.G_MONTH_DAY:
            GMonthDayValue gmd = new GMonthDayValue();
            gmd.setDateValue(this);
            return gmd;

        case Type.G_DAY:
            GDayValue gd = new GDayValue();
            gd.setDateValue(this);
            return gd;

        default:
            ValidationException err = new ValidationException("Cannot convert date to " +
                                     requiredType.getDisplayName());
            //err.setXPathContext(context);
            err.setErrorCode("FORG0001");
            return new ErrorValue(err);
        }
    }

    /**
     * Set the value (used for creating subtypes)
     */

    public void setDateValue(DateValue d) {
        calendar = d.calendar;
        zoneSpecified = d.zoneSpecified;
        tzOffset = d.tzOffset;
    }

    /**
    * Convert to string
    * @return ISO 8601 representation.
    */

    public String getStringValue() {

        FastStringBuffer sb = new FastStringBuffer(16);
        int era = calendar.get(GregorianCalendar.ERA);
        int year = calendar.get(Calendar.YEAR);
        if (era == GregorianCalendar.BC) {
            sb.append('-');
        }
        DateTimeValue.appendString(sb, year, (year>9999 ? (calendar.get(Calendar.YEAR)+"").length() : 4));
        sb.append('-');
        DateTimeValue.appendString(sb, calendar.get(Calendar.MONTH)+1, 2);
        sb.append('-');
        DateTimeValue.appendString(sb, calendar.get(Calendar.DATE), 2);

        if (zoneSpecified) {
            DateTimeValue.appendTimezone(tzOffset, sb);
        }

        return sb.toString();

    }

    /**
    * Determine the data type of the expression
    * @return Type.DATE,
    */

    public ItemType getItemType() {
        return Type.DATE_TYPE;
    }

    /**
     * Return a dateTime with the same localized value, but
     * without the timezone component
     * @return the result of removing the timezone
     */

    public CalendarValue removeTimezone() throws XPathException {
        return (CalendarValue)
                    ((DateTimeValue)convert(Type.DATE_TIME))
                        .removeTimezone()
                        .convert(Type.DATE);
    }

    /**
     * Return a date, time, or dateTime with the same normalized value, but
     * in a different timezone
     * @return the date/time in the new timezone
     * @throws XPathException
     */

    public CalendarValue setTimezone(SecondsDurationValue tz) throws XPathException {
       return (CalendarValue)
                    ((DateTimeValue)convert(Type.DATE_TIME))
                        .setTimezone(tz)
                        .convert(Type.DATE);
    }

    /**
    * Convert to Java object (for passing to external functions)
    */

    public Object convertToJava(Class target, XPathContext context) throws XPathException {
        if (target.isAssignableFrom(Date.class)) {
            return getUTCDate();
        } else if (target.isAssignableFrom(DateTimeValue.class)) {
            return this;
        } else if (target==String.class || target==CharSequence.class) {
            return getStringValue();
        } else if (target==Object.class) {
            return getStringValue();
        } else {
            Object o = super.convertToJava(target, context);
            if (o == null) {
                throw new DynamicError("Conversion of date to " + target.getName() +
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
        case Component.TIMEZONE:
            if (zoneSpecified) {
                return SecondsDurationValue.fromMilliseconds(tzOffset*60000);
            } else {
                return null;
            }
        default:
            throw new IllegalArgumentException("Unknown component for date: " + component);
        }
    }

    /**
    * Compare the value to another date value
    * @param other The other date value. Must be an object of class DateValue.
    * @return negative value if this one is the earlier, 0 if they are chronologically equal,
    * positive value if this one is the later. For this purpose, dateTime values with an unknown
    * timezone are considered to be UTC values (the Comparable interface requires
    * a total ordering).
    * @throws ClassCastException if the other value is not a DateValue (the parameter
    * is declared as Object to satisfy the Comparable interface)
    */

    public int compareTo(Object other) {
        if (!(other instanceof DateValue)) {
            throw new ClassCastException("Date values are not comparable to " + other.getClass());
        }
        int primaryDiff = getUTCDate().compareTo(((DateValue)other).getUTCDate());
        if (primaryDiff==0) {
            // 1 Jan 2002 in New York is later than 1 Jan 2002 in London...
            return ((DateValue)other).tzOffset - tzOffset;
        } else {
            return primaryDiff;
        }

    }

    public boolean equals(Object other) {
        return compareTo(other) == 0;
    }

    public int hashCode() {
        // Equality must imply same hashcode, but not vice-versa
        return getUTCDate().hashCode() + new Integer(tzOffset).hashCode();
    }

    /**
     * Add a duration to a date
     * @param duration the duration to be added (may be negative)
     * @return the new date
     * @throws XPathException if the duration is an xs:duration, as distinct from
     * a subclass thereof
     */

    public CalendarValue add(DurationValue duration) throws XPathException {
        if (duration instanceof SecondsDurationValue) {
            int days = (int)duration.getLengthInSeconds() / (60*60*24);
            GregorianCalendar cal2 = (GregorianCalendar)calendar.clone();
            cal2.add(Calendar.DATE, days);
            return new DateValue(cal2, zoneSpecified, tzOffset);
        } else if (duration instanceof MonthDurationValue) {
            int months = ((MonthDurationValue)duration).getLengthInMonths();
            GregorianCalendar cal2 = (GregorianCalendar)calendar.clone();
            cal2.add(Calendar.MONTH, months);
            return new DateValue(cal2, zoneSpecified, tzOffset);
        } else {
            DynamicError err = new DynamicError(
                    "Date arithmetic is not supported on xs:duration, only on its subtypes");
            err.setIsTypeError(true);
            throw err;
        }
    }

  /**
     * Determine the difference between two points in time, as a duration
     * @param other the other point in time
     * @param context
   * @return the duration as an xdt:dayTimeDuration
     * @throws XPathException for example if one value is a date and the other is a time
     */

    public SecondsDurationValue subtract(CalendarValue other, XPathContext context) throws XPathException {
        if (!(other instanceof DateValue)) {
            DynamicError err = new DynamicError(
                    "First operand of '-' is a date, but the second is not");
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

