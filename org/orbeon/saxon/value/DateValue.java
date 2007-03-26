package org.orbeon.saxon.value;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.Err;
import org.orbeon.saxon.sort.ComparisonKey;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.functions.Component;
import org.orbeon.saxon.om.FastStringBuffer;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.*;

import java.util.*;

/**
* A value of type Date. Note that a Date may include a TimeZone.
*/

public class DateValue extends CalendarValue {

    protected int year;         // unlike the lexical representation, includes a year zero
    protected byte month;
    protected byte day;

    /**
     * Default constructor needed for subtyping
     */

    protected DateValue() {}

    /**
     * Constructor given a year, month, and day. Performs no validation.
     * @param year The year as held internally (note that the year before 1AD is 0)
     * @param month The month, 1-12
     * @param day The day 1-31
     */

    public DateValue(int year, byte month, byte day) {
        this.year = year;
        this.month = month;
        this.day = day;
    }

    /**
     * Constructor given a year, month, and day, and timezone. Performs no validation.
     * @param year The year as held internally (note that the year before 1AD is 0)
     * @param month The month, 1-12
     * @param day The day 1-31
     * @param tz the timezone displacement in minutes from UTC. Supply the value
     * {@link CalendarValue#NO_TIMEZONE} if there is no timezone component.
     */

    public DateValue(int year, byte month, byte day, int tz) {
        this.year = year;
        this.month = month;
        this.day = day;
        setTimezoneInMinutes(tz);
    }

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
     * @param tz The timezone offset from GMT in minutes, positive or negative; or the special
     * value NO_TIMEZONE indicating that the value is not in a timezone
     */
    public DateValue(GregorianCalendar calendar, int tz) {
        // Note: this constructor is not used by Saxon itself, but might be used by applications
        int era = calendar.get(GregorianCalendar.ERA);
        year = calendar.get(Calendar.YEAR);
        if (era == GregorianCalendar.BC) {
            year = 1-year;
        }
        month = (byte)(calendar.get(Calendar.MONTH)+1);
        day = (byte)(calendar.get(Calendar.DATE));
        setTimezoneInMinutes(tz);
    }

    /**
     * Initialize the DateValue using a character string in the format yyyy-mm-dd and an optional time zone.
     * Input must have format [-]yyyy-mm-dd[([+|-]hh:mm | Z)]
     * @param s the supplied string value
     * @throws org.orbeon.saxon.trans.XPathException
     */
    public void setLexicalValue(CharSequence s) throws XPathException {
        StringTokenizer tok = new StringTokenizer(Whitespace.trimWhitespace(s).toString(), "-:+Z", true);
        try {
            if (!tok.hasMoreElements()) badDate("Too short", s);
            String part = (String)tok.nextElement();
            int era = +1;
            if ("+".equals(part)) {
                badDate("Date may not start with '+' sign", s);
            } else if ("-".equals(part)) {
                era = -1;
                part = (String)tok.nextElement();
            }

            if (part.length() < 4) {
                badDate("Year is less than four digits", s);
            }
            if (part.length() > 4 && part.charAt(0) == '0') {
                badDate("When year exceeds 4 digits, leading zeroes are not allowed", s);
            }
            year = Integer.parseInt(part) * era;
            if (year==0) {
                badDate("Year zero is not allowed", s);
            }
            if (era < 0) {
                year++;     // internal representation allows a year zero.
            }
            if (!tok.hasMoreElements()) badDate("Too short", s);
            if (!"-".equals(tok.nextElement())) badDate("Wrong delimiter after year", s);

            if (!tok.hasMoreElements()) badDate("Too short", s);
            part = (String)tok.nextElement();
            if (part.length() != 2) badDate("Month must be two digits", s);
            month = (byte)Integer.parseInt(part);
            if (month < 1 || month > 12) badDate("Month is out of range", s);
            if (!tok.hasMoreElements()) badDate("Too short", s);
            if (!"-".equals(tok.nextElement())) badDate("Wrong delimiter after month", s);

            if (!tok.hasMoreElements()) badDate("Too short", s);
            part = (String)tok.nextElement();
            if (part.length() != 2) badDate("Day must be two digits", s);
            day = (byte)Integer.parseInt(part);
            if (day < 1 || day > 31) badDate("Day is out of range", s);

            int tzOffset;
            if (tok.hasMoreElements()) {

                String delim = (String)tok.nextElement();

                if ("Z".equals(delim)) {
                    tzOffset = 0;
                    if (tok.hasMoreElements()) badDate("Continues after 'Z'", s);
                    setTimezoneInMinutes(tzOffset);

                } else if (!(!"+".equals(delim) && !"-".equals(delim))) {
                    if (!tok.hasMoreElements()) badDate("Missing timezone", s);
                    part = (String)tok.nextElement();
                    int tzhour = Integer.parseInt(part);
                    if (part.length() != 2) badDate("Timezone hour must be two digits", s);
                    if (tzhour > 14) badDate("Timezone hour is out of range", s);
                    //if (tzhour > 12) badDate("Because of Java limitations, Saxon currently limits the timezone to +/- 12 hours", s);
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
                    setTimezoneInMinutes(tzOffset);

                } else {
                    badDate("Timezone format is incorrect", s);
                }
            }

            if (!isValidDate(year, month, day)) {
                badDate("Non-existent date", s);
            }


        } catch (NumberFormatException err) {
            badDate("Non-numeric component", s);
        }
    }

    private void badDate(String msg, CharSequence value) throws ValidationException {
        ValidationException err = new ValidationException(
                "Invalid date " + Err.wrap(value, Err.VALUE) + " (" + msg + ")");
        err.setErrorCode("FORG0001");
        throw err;
    }

    /**
     * Get the year component of the date (in local form)
     */

    public int getYear() {
        return year;
    }

    /**
     * Get the month component of the date (in local form)
     */

    public byte getMonth() {
        return month;
    }

    /**
     * Get the day component of the date (in local form)
     */

    public byte getDay() {
        return day;
    }

    /**
     * Test whether a candidate date is actually a valid date in the proleptic Gregorian calendar
     */

    private static byte[] daysPerMonth = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
    public static boolean isValidDate(int year, int month, int day) {
        if (month > 0 && month <= 12 && day > 0 && day <= daysPerMonth[month-1]) {
            return true;
        }
        if (month == 2 && day == 29) {
            return isLeapYear(year);
        }
        return false;
    }

    /**
     * Test whether a year is a leap year
     */

    public static boolean isLeapYear(int year) {
        return (year % 4 == 0) && !(year % 100 == 0 && !(year % 400 == 0));
    }

    /**
     * Get the date that immediately follows a given date
     * @return a new DateValue with no timezone information
     */

    public static DateValue tomorrow(int year, byte month, byte day) {
        if (DateValue.isValidDate(year, month, day+1)) {
            return new DateValue(year, month, (byte)(day+1));
        } else if (month < 12) {
            return new DateValue(year, (byte)(month+1), (byte)1);
        } else {
            return new DateValue(year+1, (byte)1, (byte)1);
        }
    }

    /**
     * Get the date that immediately precedes a given date
     * @return a new DateValue with no timezone information
     */

    public static DateValue yesterday(int year, byte month, byte day) {
        if (day > 1) {
            return new DateValue(year, month, (byte)(day-1));
        } else if (month > 1) {
            if (month == 3 && isLeapYear(year)) {
                return new DateValue(year, (byte)2, (byte)29);
            } else {
                return new DateValue(year, (byte)(month-1), daysPerMonth[month-2]);
            }
        } else {
            return new DateValue(year-1, (byte)12, (byte)31);
        }
    }

    /**
    * Convert to target data type
    * @param requiredType an integer identifying the required atomic type
    * @param context
     * @return an AtomicValue, a value of the required type; or an ErrorValue
    */

    public AtomicValue convertPrimitive(BuiltInAtomicType requiredType, boolean validate, XPathContext context) {
        switch(requiredType.getPrimitiveType()) {
        case Type.DATE:
        case Type.ANY_ATOMIC:
        case Type.ITEM:
            return this;
        case Type.DATE_TIME:
            return toDateTime();

        case Type.STRING:
            return new StringValue(getStringValueCS());

        case Type.UNTYPED_ATOMIC:
            return new UntypedAtomicValue(getStringValueCS());

        case Type.G_YEAR: {
            return new GYearValue(year, getTimezoneInMinutes());
        }
        case Type.G_YEAR_MONTH: {
            return new GYearMonthValue(year, month, getTimezoneInMinutes());
        }
        case Type.G_MONTH: {
            return new GMonthValue(month, getTimezoneInMinutes());
        }
        case Type.G_MONTH_DAY: {
            return new GMonthDayValue(month, day, getTimezoneInMinutes());
        }
        case Type.G_DAY:{
            return new GDayValue(day, getTimezoneInMinutes());
        }

        default:
            ValidationException err = new ValidationException("Cannot convert date to " +
                                     requiredType.getDisplayName());
            err.setErrorCode("XPTY0004");
            err.setIsTypeError(true);
            return new ValidationErrorValue(err);
        }
    }

    /**
     * Convert to DateTime
     */

    public DateTimeValue toDateTime() {
        return new DateTimeValue(year, month, day, (byte)0, (byte)0, (byte)0, 0, getTimezoneInMinutes());
    }

    /**
    * Convert to string
    * @return ISO 8601 representation.
    */

    public CharSequence getStringValueCS() {

        FastStringBuffer sb = new FastStringBuffer(16);
        int yr = year;
        if (year <= 0) {
            sb.append('-');
            yr = -yr +1;           // no year zero in lexical space
        }
        appendString(sb, yr, (yr>9999 ? (yr+"").length() : 4));
        sb.append('-');
        appendTwoDigits(sb, month);
        sb.append('-');
        appendTwoDigits(sb, day);

        if (hasTimezone()) {
            appendTimezone(sb);
        }

        return sb;

    }

    public GregorianCalendar getCalendar() {

        int tz = (hasTimezone() ? getTimezoneInMinutes() : 0);
        TimeZone zone = new SimpleTimeZone(tz*60000, "LLL");
        GregorianCalendar calendar = new GregorianCalendar(zone);
        calendar.setGregorianChange(new Date(Long.MIN_VALUE));
        calendar.clear();
        calendar.setLenient(false);
        int yr = year;
        if (year <= 0) {
            yr = 1-year;
            calendar.set(Calendar.ERA, GregorianCalendar.BC);
        }
        calendar.set(yr, month-1, day);
        calendar.set(Calendar.ZONE_OFFSET, tz*60000);
        calendar.set(Calendar.DST_OFFSET, 0);
        calendar.getTime();
        return calendar;
    }

    /**
    * Determine the data type of the expression
    * @return Type.DATE_TYPE,
     * @param th
     */

    public ItemType getItemType(TypeHierarchy th) {
        return Type.DATE_TYPE;
    }

    /**
     * Make a copy of this date, time, or dateTime value
     */

    public CalendarValue copy() {
        return new DateValue(year, month, day, getTimezoneInMinutes());
    }

    /**
     * Return a new date with the same normalized value, but
     * in a different timezone. This is called only for a DateValue that has an explicit timezone
     * @param timezone the new timezone offset, in minutes
     * @return the time in the new timezone. This will be a new TimeValue unless no change
     * was required to the original value
     */

    public CalendarValue adjustTimezone(int timezone) {
        DateTimeValue dt = (DateTimeValue)toDateTime().adjustTimezone(timezone);
        return new DateValue(dt.getYear(), dt.getMonth(), dt.getDay(), dt.getTimezoneInMinutes());
    }

    /**
    * Convert to Java object (for passing to external functions)
    */

    public Object convertToJava(Class target, XPathContext context) throws XPathException {
        if (target.isAssignableFrom(Date.class)) {
            return getCalendar().getTime();
        } else if (target.isAssignableFrom(GregorianCalendar.class)) {
            return getCalendar();
        } else if (target.isAssignableFrom(DateValue.class)) {
            return this;
        } else if (target==String.class) {
            return getStringValue();
        } else if (target.isAssignableFrom(CharSequence.class)) {
            return getStringValueCS();
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
            return new IntegerValue((year > 0 ? year : year-1));
        case Component.MONTH:
            return new IntegerValue(month);
        case Component.DAY:
            return new IntegerValue(day);
        case Component.TIMEZONE:
            if (hasTimezone()) {
                return SecondsDurationValue.fromMilliseconds(getTimezoneInMinutes()*60000);
            } else {
                return null;
            }
        default:
            throw new IllegalArgumentException("Unknown component for date: " + component);
        }
    }

    /**
    * Compare the value to another date value. This method is used only during schema processing,
    * and uses XML Schema semantics rather than XPath semantics.
    * @param other The other date value. Must be an object of class DateValue.
    * @return negative value if this one is the earlier, 0 if they are chronologically equal,
    * positive value if this one is the later. For this purpose, dateTime values with an unknown
    * timezone are considered to be UTC values (the Comparable interface requires
    * a total ordering).
    * @throws ClassCastException if the other value is not a DateValue (the parameter
    * is declared as Object to satisfy the Comparable interface)
    */

    public int compareTo(Object other) {
        if (other instanceof AtomicValue) {
            other = ((AtomicValue)other).getPrimitiveValue();
        }
        if (!(other instanceof DateValue)) {
            throw new ClassCastException("Date values are not comparable to " + other.getClass());
        }
        return compareTo((DateValue)other, new Configuration());
    }

    /**
     * Compare this value to another value of the same type, using the supplied context object
     * to get the implicit timezone if required. This method implements the XPath comparison semantics.
     */

    public int compareTo(CalendarValue other, Configuration config) {
        final TypeHierarchy th = config.getTypeHierarchy();
        if (this.getItemType(th).getPrimitiveType() != other.getItemType(th).getPrimitiveType()) {
            throw new ClassCastException("Cannot compare values of different types");
                        // covers, for example, comparing a gYear to a gYearMonth
        }
        // This code allows comparison of a gYear (etc) to a date, but this is prevented at a higher level
        return toDateTime().compareTo(other.toDateTime(), config);
    }

    /**
     * Get a comparison key for this value. Two values are equal if and only if they their comparison
     * keys are equal
     */


    public ComparisonKey getComparisonKey(Configuration config) {
        return new ComparisonKey(Type.DATE, toDateTime().normalize(config));
    }

    public boolean equals(Object other) {
        return compareTo(other) == 0;
    }

    public int hashCode() {
        // Equality must imply same hashcode, but not vice-versa
        return getCalendar().getTime().hashCode() + getTimezoneInMinutes();
    }

    /**
     * Add a duration to a date
     * @param duration the duration to be added (may be negative)
     * @return the new date
     * @throws org.orbeon.saxon.trans.XPathException if the duration is an xs:duration, as distinct from
     * a subclass thereof
     */

    public CalendarValue add(DurationValue duration) throws XPathException {
        if (duration instanceof SecondsDurationValue) {
            long microseconds = ((SecondsDurationValue)duration).getLengthInMicroseconds();
            boolean negative = (microseconds < 0);
            microseconds = Math.abs(microseconds);
            int days = (int)Math.floor((double)microseconds / (1000000L*60L*60L*24L));
            boolean partDay = (microseconds % (1000000L*60L*60L*24L)) > 0;
            int julian = getJulianDayNumber(year, month, day);
            DateValue d = dateFromJulianDayNumber(julian + (negative ? -days : days));
            if (partDay) {
                if (negative) {
                    d = yesterday(d.year, d.month, d.day);
                }
            }
            d.setTimezoneInMinutes(getTimezoneInMinutes());
            return d;
        } else if (duration instanceof MonthDurationValue) {
            int months = ((MonthDurationValue)duration).getLengthInMonths();
            int m = (month-1) + months;
            int y = year + m / 12;
            m = m % 12;
            if (m < 0) {
                m += 12;
                y -= 1;
            }
            m++;
            int d = day;
            while (!isValidDate(y, m, d)) {
                d -= 1;
            }
            return new DateValue(y, (byte)m, (byte)d, getTimezoneInMinutes());
        } else {
            DynamicError err = new DynamicError(
                    "Date arithmetic is not supported on xs:duration, only on its subtypes");
            err.setIsTypeError(true);
            err.setErrorCode("XPTY0004");
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
        if (!(other instanceof DateValue)) {
            DynamicError err = new DynamicError(
                    "First operand of '-' is a date, but the second is not");
            err.setIsTypeError(true);
            err.setErrorCode("XPTY0004");
            throw err;
        }
        return super.subtract(other, context);
    }

    /**
     * Calculate the Julian day number at 00:00 on a given date. This algorithm is taken from
     * http://vsg.cape.com/~pbaum/date/jdalg.htm and
     * http://vsg.cape.com/~pbaum/date/jdalg2.htm
     * (adjusted to handle BC dates correctly)
     */

    public static int getJulianDayNumber(int year, int month, int day) {
        int z = year - (month<3 ? 1 : 0);
        short f = monthData[month-1];
        if (z >= 0) {
            return day + f + 365*z + z/4 - z/100 + z/400 + 1721118;
        } else {
            // for negative years, add 12000 years and then subtract the days!
            z += 12000;
            int j = day + f + 365*z + z/4 - z/100 + z/400 + 1721118;
            return j - (365*12000 + 12000/4 - 12000/100 + 12000/400);  // number of leap years in 12000 years
        }
    }

    /**
     * Get the Gregorian date corresponding to a particular Julian day number. The algorithm
     * is taken from http://www.hermetic.ch/cal_stud/jdn.htm#comp
     * @return a DateValue with no timezone information set
     */

    public static DateValue dateFromJulianDayNumber(int julianDayNumber) {
        if (julianDayNumber >= 0) {
            int L = julianDayNumber + 68569 + 1;    // +1 adjustment for days starting at noon
            int n = ( 4 * L ) / 146097;
            L = L - ( 146097 * n + 3 ) / 4;
            int i = ( 4000 * ( L + 1 ) ) / 1461001;
            L = L - ( 1461 * i ) / 4 + 31;
            int j = ( 80 * L ) / 2447;
            int d = L - ( 2447 * j ) / 80;
            L = j / 11;
            int m = j + 2 - ( 12 * L );
            int y = 100 * ( n - 49 ) + i + L;
            return new DateValue(y, (byte)m, (byte)d);
        } else {
            // add 12000 years and subtract them again...
            DateValue dt = dateFromJulianDayNumber(julianDayNumber +
                    (365*12000 + 12000/4 - 12000/100 + 12000/400));
            dt.year -= 12000;
            return dt;
        }
    }

    private static final short[] monthData = {306, 337, 0, 31, 61, 92, 122, 153, 184, 214, 245, 275};

    /**
     * Get the ordinal day number within the year (1 Jan = 1, 1 Feb = 32, etc)
     */

    public static final int getDayWithinYear(int year, int month, int day) {
        int j = getJulianDayNumber(year, month, day);
        int k = getJulianDayNumber(year, 1, 1);
        return j - k + 1;
    }

    /**
     * Get the day of the week.  The days of the week are numbered from
     * 1 (Monday) to 7 (Sunday)
     */

    public static final int getDayOfWeek(int year, int month, int day) {
        int d = getJulianDayNumber(year, month, day);
        d -= 2378500;   // 1800-01-05 - any Monday would do
        while (d <= 0) {
            d += 70000000;  // any sufficiently-high multiple of 7 would do
        }
        return (d-1)%7 + 1;
    }

    /**
     * Get the ISO week number for a given date.  The days of the week are numbered from
     * 1 (Monday) to 7 (Sunday), and week 1 in any calendar year is the week (from Monday to Sunday)
     * that includes the first Thursday of that year
     */

    public static final int getWeekNumber(int year, int month, int day) {
        int d = getDayWithinYear(year, month, day);
        int firstDay = getDayOfWeek(year, 1, 1);
        if (firstDay > 4 && (firstDay + d) <= 8) {
            // days before week one are part of the last week of the previous year (52 or 53)
            return getWeekNumber(year-1, 12, 31);
        }
        int inc = (firstDay < 5 ? 1 : 0);   // implements the First Thursday rule
        return ((d + firstDay - 2) / 7) + inc;

    }

    /**
     * Get the week number within a month. This is required for the XSLT format-date() function,
     * and the rules are not entirely clear. The days of the week are numbered from
     * 1 (Monday) to 7 (Sunday), and by analogy with the ISO week number, we consider that week 1
     * in any calendar month is the week (from Monday to Sunday) that includes the first Thursday
     * of that month. Unlike the ISO week number, we put the previous days in week zero.
     */

    public static final int getWeekNumberWithinMonth(int year, int month, int day) {
        int firstDay = getDayOfWeek(year, month, 1);
        int inc = (firstDay < 5 ? 1 : 0);   // implements the First Thursday rule
        return ((day + firstDay - 2) / 7) + inc;
    }

    /**
     * Temporary test rig
     */

    public static void main(String[] args) throws Exception {
        DateValue date = new DateValue(args[0]);
        System.out.println(date.getStringValue());
        int jd = getJulianDayNumber(date.year,  date.month, date.day);
        System.out.println(jd);
        System.out.println(dateFromJulianDayNumber(jd).getStringValue());
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

