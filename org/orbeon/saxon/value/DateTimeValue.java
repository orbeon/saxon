package org.orbeon.saxon.value;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.Controller;
import org.orbeon.saxon.Err;
import org.orbeon.saxon.sort.ComparisonKey;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.functions.Component;
import org.orbeon.saxon.om.FastStringBuffer;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/**
* A value of type DateTime
*/

public final class DateTimeValue extends CalendarValue {

    private int year;       // the year as written, +1 for BC years
    private byte month;     // the month as written, range 1-12
    private byte day;       // the day as written, range 1-31
    private byte hour;      // the hour as written (except for midnight), range 0-23
    private byte minute;   // the minutes as written, range 0-59
    private byte second;   // the seconds as written, range 0-59 (no leap seconds)
    private int microsecond;

    /**
    * Get the dateTime value representing the nominal
    * date/time of this transformation run. Two calls within the same
    * query or transformation will always return the same answer.
    */

    public static DateTimeValue getCurrentDateTime(XPathContext context) {
        Controller c;
        if (context==null || (c = context.getController()) == null) {
            // non-XSLT/XQuery environment
            // We also take this path when evaluating compile-time expressions that require an implicit timezone.
            return new DateTimeValue(new GregorianCalendar(), true);
        } else {
            return c.getCurrentDateTime();
        }
    }

    /**
    * Constructor: create a dateTime value given a Java calendar object
    * @param calendar holds the date and time
    * @param tzSpecified indicates whether the timezone is specified
    */

    public DateTimeValue(Calendar calendar, boolean tzSpecified) {
        int era = calendar.get(GregorianCalendar.ERA);
        year = calendar.get(Calendar.YEAR);
        if (era == GregorianCalendar.BC) {
            year = 1-year;
        }
        month = (byte)(calendar.get(Calendar.MONTH)+1);
        day = (byte)(calendar.get(Calendar.DATE));
        hour = (byte)(calendar.get(Calendar.HOUR_OF_DAY));
        minute = (byte)(calendar.get(Calendar.MINUTE));
        second = (byte)(calendar.get(Calendar.SECOND));
        microsecond = calendar.get(Calendar.MILLISECOND) * 1000;
        if (tzSpecified) {
            int tz = (calendar.get(Calendar.ZONE_OFFSET) +
                                    calendar.get(Calendar.DST_OFFSET)) / 60000;
            setTimezoneInMinutes(tz);
        }
    }

    /**
     * Constructor: create a dateTime value given a date and a time.
     * @param date the date
     * @param time the time
     * @throws org.orbeon.saxon.trans.XPathException if the timezones are both present and inconsistent
     */

    public DateTimeValue(DateValue date, TimeValue time) throws XPathException {
        SecondsDurationValue tz1 = (SecondsDurationValue)date.getComponent(Component.TIMEZONE);
        SecondsDurationValue tz2 = (SecondsDurationValue)time.getComponent(Component.TIMEZONE);
        boolean zoneSpecified = (tz1 != null || tz2 != null);
        if (tz1 != null && tz2 != null && !tz1.equals(tz2)) {
            DynamicError err = new DynamicError("Supplied date and time are in different timezones");
            err.setErrorCode("FORG0008");
            throw err;
        }

        year = (int)((IntegerValue)date.getComponent(Component.YEAR)).longValue();
        month = (byte)((IntegerValue)date.getComponent(Component.MONTH)).longValue();
        day = (byte)((IntegerValue)date.getComponent(Component.DAY)).longValue();
        hour = (byte)((IntegerValue)time.getComponent(Component.HOURS)).longValue();
        minute = (byte)((IntegerValue)time.getComponent(Component.MINUTES)).longValue();
        final BigDecimal secs = ((DecimalValue)time.getComponent(Component.SECONDS)).getValue();
        second = (byte)secs.intValue();
        microsecond = secs.multiply(BigDecimal.valueOf(1000000)).intValue() % 1000000;
        if (zoneSpecified) {
            if (tz1 == null) {
                tz1 = tz2;
            }
            setTimezoneInMinutes((int)(tz1.getLengthInMicroseconds() / 60000000));
        }
    }

    /**
    * Constructor: create a dateTime value from a supplied string, in
    * ISO 8601 format
    */

    public DateTimeValue(CharSequence s) throws XPathException {
        // input must have format [-]yyyy-mm-ddThh:mm:ss[.fff*][([+|-]hh:mm | Z)]
        StringTokenizer tok = new StringTokenizer(Whitespace.trimWhitespace(s).toString(), "-:.+TZ", true);
        try {
            if (!tok.hasMoreElements()) {
                badDate("too short", s);
            }
            String part = (String)tok.nextElement();
            int era = +1;
            if ("+".equals(part)) {
                badDate("Date may not start with '+' sign", s);
            } else if ("-".equals(part)) {
                era = -1;
                part = (String)tok.nextElement();
            }
            year = Integer.parseInt(part) * era;
            if (part.length() < 4) {
                badDate("Year is less than four digits", s);
            }
            if (part.length() > 4 && part.charAt(0) == '0') {
                badDate("When year exceeds 4 digits, leading zeroes are not allowed", s);
            }
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

            if (!tok.hasMoreElements()) badDate("Too short", s);
            if (!"T".equals(tok.nextElement())) badDate("Wrong delimiter after day", s);

            if (!tok.hasMoreElements()) badDate("Too short", s);
            part = (String)tok.nextElement();
            if (part.length() != 2) badDate("Hour must be two digits", s);
            hour = (byte)Integer.parseInt(part);
            if (hour > 24) badDate("Hour is out of range", s);

            if (!tok.hasMoreElements()) badDate("Too short", s);
            if (!":".equals(tok.nextElement())) badDate("Wrong delimiter after hour", s);

            if (!tok.hasMoreElements()) badDate("Too short", s);
            part = (String)tok.nextElement();
            if (part.length() != 2) badDate("Minute must be two digits", s);
            minute = (byte)Integer.parseInt(part);
            if (minute > 59) badDate("Minute is out of range", s);
            if (hour == 24 && minute != 0) badDate("If hour is 24, minute must be 00", s);
            if (!tok.hasMoreElements()) badDate("Too short", s);
            if (!":".equals(tok.nextElement())) badDate("Wrong delimiter after minute", s);

            if (!tok.hasMoreElements()) badDate("Too short", s);
            part = (String)tok.nextElement();
            if (part.length() != 2) badDate("Second must be two digits", s);
            second = (byte)Integer.parseInt(part);

            if (second > 59) badDate("Second is out of range", s);
            if (hour == 24 && second != 0) badDate("If hour is 24, second must be 00", s);

            int tz = 0;

            int state = 0;
            while (tok.hasMoreElements()) {
                if (state==9) {
                    badDate("Characters after the end", s);
                }
                String delim = (String)tok.nextElement();
                if (".".equals(delim)) {
                    if (state != 0) {
                        badDate("Decimal separator occurs twice", s);
                    }
                    part = (String)tok.nextElement();
                    double fractionalSeconds = Double.parseDouble('.' + part);
                    microsecond = (int)(Math.round(fractionalSeconds * 1000000));
                    if (hour == 24 && microsecond != 0) {
                        badDate("If hour is 24, fractional seconds must be 0", s);
                    }
                    state = 1;
                } else if ("Z".equals(delim)) {
                    if (state > 1) {
                        badDate("Z cannot occur here", s);
                    }
                    tz = 0;
                    state = 9;  // we've finished
                    setTimezoneInMinutes(0);
                } else if ("+".equals(delim) || "-".equals(delim)) {
                    if (state > 1) {
                        badDate(delim + " cannot occur here", s);
                    }
                    state = 2;
                    if (!tok.hasMoreElements()) badDate("Missing timezone", s);
                    part = (String)tok.nextElement();
                    if (part.length() != 2) badDate("Timezone hour must be two digits", s);

                    tz = Integer.parseInt(part);
                    if (tz > 14) badDate("Timezone is out of range (-14:00 to +14:00)", s);
                    tz *= 60;

                    //if (tz > 12*60) badDate("Because of Java limitations, Saxon currently limits the timezone to +/- 12 hours");
                    if ("-".equals(delim)) tz = -tz;

                } else if (":".equals(delim)) {
                    if (state != 2) {
                        badDate("Misplaced ':'", s);
                    }
                    state = 9;
                    part = (String)tok.nextElement();
                    int tzminute = Integer.parseInt(part);
                    if (part.length() != 2) badDate("Timezone minute must be two digits", s);
                    if (tzminute > 59) badDate("Timezone minute is out of range", s);
                    if (tz<0) tzminute = -tzminute;
                    if (Math.abs(tz) == 14*60 && tzminute != 0) {
                        badDate("Timezone is out of range (-14:00 to +14:00)", s);
                    }
                    tz += tzminute;
                    setTimezoneInMinutes(tz);
                } else {
                    badDate("Timezone format is incorrect", s);
                }
            }

            if (state == 2 || state == 3) {
                badDate("Timezone incomplete", s);
            }

            boolean midnight = false;
            if (hour == 24) {
                hour = 0;
                midnight = true;
            }

            // Check that this is a valid calendar date
            if (!DateValue.isValidDate(year, month, day)) {
                badDate("Non-existent date", s);
            }

            // Adjust midnight to 00:00:00 on the next day
            if (midnight) {
                DateValue t = DateValue.tomorrow(year, month, day);
                year = t.getYear();
                month = t.getMonth();
                day = t.getDay();
            }


        } catch (NumberFormatException err) {
            badDate("Non-numeric component", s);
        }
    }

    private void badDate(String msg, CharSequence value) throws XPathException {
        ValidationException err = new ValidationException(
                "Invalid dateTime value " + Err.wrap(value, Err.VALUE) + " (" + msg + ")");
        err.setErrorCode("FORG0001");
        throw err;
    }

    /**
     * Constructor: construct a DateTimeValue from its components.
     * This constructor performs no validation.
     * @param year The year as held internally (note that the year before 1AD is 0)
     * @param month The month, 1-12
     * @param day The day 1-31
     * @param hour the hour value, 0-23
     * @param minute the minutes value, 0-59
     * @param second the seconds value, 0-59
     * @param microsecond the number of microseconds, 0-999999
     * @param tz the timezone displacement in minutes from UTC. Supply the value
     * {@link CalendarValue#NO_TIMEZONE} if there is no timezone component.
     */

    public DateTimeValue(int year, byte month, byte day,
                         byte hour, byte minute, byte second, int microsecond, int tz) {
        this.year = year;
        this.month = month;
        this.day = day;
        this.hour = hour;
        this.minute = minute;
        this.second = second;
        this.microsecond = microsecond;
        setTimezoneInMinutes(tz);
    }

    /**
     * Get the year component, in its internal form (which allows a year zero)
     */

    public int getYear() {
        return year;
    }

    /**
     * Get the month component, 1-12
     */

    public byte getMonth() {
        return month;
    }

    /**
     * Get the day component, 1-31
     */

    public byte getDay() {
        return day;
    }

    /**
     * Get the hour component, 0-23
     */

    public byte getHour() {
        return hour;
    }

    /**
     * Get the minute component, 0-59
     */

    public byte getMinute() {
        return minute;
    }

    /**
     * Get the second component, 0-59
     */

    public byte getSecond() {
        return second;
    }

    /**
     * Get the microsecond component, 0-999999
     */

    public int getMicrosecond() {
        return microsecond;
    }

    /**
     * Convert the value to a DateTime, retaining all the components that are actually present, and
     * substituting conventional values for components that are missing
     */

    public DateTimeValue toDateTime() {
        return this;
    }

    /**
     * Normalize the date and time to be in timezone Z.
     * @param cc used to supply the implicit timezone, used when the value has
     * no explicit timezone
     * @return in general, a new DateTimeValue in timezone Z, representing the same instant in time.
     * Returns the original DateTimeValue if this is already in timezone Z.
     */

    public DateTimeValue normalize(Configuration cc) {
        if (hasTimezone()) {
            return (DateTimeValue)adjustTimezone(0);
        } else {
            DateTimeValue dt = (DateTimeValue)copy();
            dt.setTimezoneInMinutes(cc.getImplicitTimezone());
            return (DateTimeValue)dt.adjustTimezone(0);
        }
    }

    /**
     * Get a comparison key for this value. Two values are equal if and only if they their comparison
     * keys are equal
     */

    public ComparisonKey getComparisonKey(Configuration config) {
        return new ComparisonKey(Type.DATE_TIME, normalize(config));
    }

    /**
     * Get the Julian instant: a decimal value whose integer part is the Julian day number
     * multiplied by the number of seconds per day,
     * and whose fractional part is the fraction of the second.
     * This method operates on the local time, ignoring the timezone. The caller should call normalize()
     * before calling this method to get a normalized time.
     */

    public BigDecimal toJulianInstant() {
        int julianDay = DateValue.getJulianDayNumber(year, month, day);
        long julianSecond = julianDay*(24L*60L*60L);
        julianSecond += (((hour*60L + minute)*60L) + second);
        BigDecimal j = BigDecimal.valueOf(julianSecond);
        if (microsecond == 0) {
            return j;
        } else {
            return j.add(BigDecimal.valueOf(microsecond).divide(DecimalValue.BIG_DECIMAL_ONE_MILLION, 6, BigDecimal.ROUND_HALF_EVEN));
        }
    }

    /**
     * Get the DateTimeValue corresponding to a given Julian instant
     */

    public static DateTimeValue fromJulianInstant(BigDecimal instant) {
        BigInteger julianSecond = instant.toBigInteger();
        BigDecimal microseconds = instant.subtract(new BigDecimal(julianSecond)).multiply(DecimalValue.BIG_DECIMAL_ONE_MILLION);
        long js = julianSecond.longValue();
        long jd = js / (24L*60L*60L);
        DateValue date = DateValue.dateFromJulianDayNumber((int)jd);
        js = js % (24L*60L*60L);
        byte hour = (byte)(js / (60L*60L));
        js = js % (60L*60L);
        byte minute = (byte)(js / (60L));
        js = js % (60L);
        return new DateTimeValue(date.getYear(), date.getMonth(), date.getDay(),
                hour, minute, (byte)js, microseconds.intValue(), 0);
    }

    /**
     * Get a Calendar object representing the value of this DateTime. This will respect the timezone
     * if there is one, or be in GMT otherwise.
     */

    public GregorianCalendar getCalendar() {
        int tz = (hasTimezone() ? getTimezoneInMinutes()*60000 : 0);
        TimeZone zone = new SimpleTimeZone(tz, "LLL");
        GregorianCalendar calendar = new GregorianCalendar(zone);
        calendar.setGregorianChange(new Date(Long.MIN_VALUE));
        calendar.setLenient(false);
        int yr = year;
        if (year <= 0) {
            yr = 1-year;
            calendar.set(Calendar.ERA, GregorianCalendar.BC);
        }
        calendar.set(yr, month-1, day, hour, minute, second);
        calendar.set(Calendar.MILLISECOND, microsecond / 1000);   // loses precision unavoidably
        calendar.set(Calendar.ZONE_OFFSET, tz);
        calendar.set(Calendar.DST_OFFSET, 0);
        return calendar;
    }

    /**
    * Convert to target data type
    * @param requiredType an integer identifying the required atomic type
    * @param context
     * @return an AtomicValue, a value of the required type; or an ErrorValue
    */

    public AtomicValue convertPrimitive(BuiltInAtomicType requiredType, boolean validate, XPathContext context) {
        switch(requiredType.getPrimitiveType()) {
        case Type.DATE_TIME:
        case Type.ANY_ATOMIC:
        case Type.ITEM:
            return this;

        case Type.DATE:
            return new DateValue(year, month, day, getTimezoneInMinutes());

        case Type.TIME:
            return new TimeValue(hour, minute, second, microsecond, getTimezoneInMinutes());

        case Type.G_YEAR:
            return new GYearValue(year, getTimezoneInMinutes());

        case Type.G_YEAR_MONTH:
            return new GYearMonthValue(year, month, getTimezoneInMinutes());

        case Type.G_MONTH:
            return new GMonthValue(month, getTimezoneInMinutes());

        case Type.G_MONTH_DAY:
            return new GMonthDayValue(month, day, getTimezoneInMinutes());

        case Type.G_DAY:
            return new GDayValue(day, getTimezoneInMinutes());

        case Type.STRING:
            return new StringValue(getStringValueCS());

        case Type.UNTYPED_ATOMIC:
            return new UntypedAtomicValue(getStringValueCS());

        default:
            ValidationException err = new ValidationException("Cannot convert dateTime to " +
                                     requiredType.getDisplayName());
            err.setErrorCode("XPTY0004");
            err.setIsTypeError(true);
            return new ValidationErrorValue(err);
        }
    }

    /**
    * Convert to string
    * @return ISO 8601 representation. The value returned is the localized representation,
     * that is it uses the timezone contained within the value itself.
    */

    public CharSequence getStringValueCS() {

        FastStringBuffer sb = new FastStringBuffer(30);
        int yr = year;
        if (year <= 0) {
            sb.append('-');
            yr = -yr +1;    // no year zero in lexical space
        }
        appendString(sb, yr, (yr>9999 ? (yr+"").length() : 4));
        sb.append('-');
        appendTwoDigits(sb, month);
        sb.append('-');
        appendTwoDigits(sb, day);
        sb.append('T');
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
    * Determine the data type of the exprssion
    * @return Type.DATE_TIME,
     * @param th
     */

    public ItemType getItemType(TypeHierarchy th) {
        return Type.DATE_TIME_TYPE;
    }

    /**
     * Make a copy of this date, time, or dateTime value
     */

    public CalendarValue copy() {
        return new DateTimeValue(year, month, day,
                hour, minute, second, microsecond, getTimezoneInMinutes());
    }

    /**
     * Return a new dateTime with the same normalized value, but
     * in a different timezone. This is called only for a DateTimeValue that has an explicit timezone
     * @param timezone the new timezone offset, in minutes
     * @return the date/time in the new timezone. This will be a new DateTimeValue unless no change
     * was required to the original value
     */

    public CalendarValue adjustTimezone(int timezone) {
        if (!hasTimezone()) {
            CalendarValue in = copy();
            in.setTimezoneInMinutes(timezone);
            return in;
        }
        int oldtz = getTimezoneInMinutes();
        if (oldtz == timezone) {
            return this;
        }
        int tz = timezone - oldtz;
        int h = hour;
        int mi = minute;
        mi += tz;
        if (mi < 0 || mi > 59) {
            h += Math.floor(mi/60.0);
            mi = (mi+60*24)%60;
        }

        if (h >= 0 && h < 24) {
           return new DateTimeValue(year, month, day, (byte)h, (byte)mi, second, microsecond, timezone);
        }

        // Following code is designed to handle the corner case of adjusting from -14:00 to +14:00 or
        // vice versa, which can cause a change of two days in the date
        DateTimeValue dt = this;
        while (h < 0) {
            h += 24;
            DateValue t = DateValue.yesterday(dt.getYear(), dt.getMonth(), dt.getDay());
            dt = new DateTimeValue(t.getYear(), t.getMonth(), t.getDay(),
                    (byte)h, (byte)mi, second, microsecond, timezone);
        }
        while (h > 23) {
            h -= 24;
            DateValue t = DateValue.tomorrow(year, month, day);
            return new DateTimeValue(t.getYear(), t.getMonth(), t.getDay(),
                    (byte)h, (byte)mi, second, microsecond, timezone);
        }
        return dt;
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
            long microseconds = ((SecondsDurationValue)duration).getLengthInMicroseconds();
            BigDecimal seconds = BigDecimal.valueOf(microseconds).divide(
                    DecimalValue.BIG_DECIMAL_ONE_MILLION, 6, BigDecimal.ROUND_HALF_EVEN);
            BigDecimal julian = toJulianInstant();
            julian = julian.add(seconds);
            DateTimeValue dt = fromJulianInstant(julian);
            dt.setTimezoneInMinutes(getTimezoneInMinutes());
            return dt;
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
            while (!DateValue.isValidDate(y, m, d)) {
                d -= 1;
            }
            return new DateTimeValue(y, (byte)m, (byte)d,
                    hour, minute, second, microsecond, getTimezoneInMinutes());
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
     * @param context
     * @return the duration as an xdt:dayTimeDuration
     * @throws org.orbeon.saxon.trans.XPathException for example if one value is a date and the other is a time
     */

    public SecondsDurationValue subtract(CalendarValue other, XPathContext context) throws XPathException {
        if (!(other instanceof DateTimeValue)) {
            DynamicError err = new DynamicError(
                    "First operand of '-' is a dateTime, but the second is not");
            err.setIsTypeError(true);
            throw err;
        }
        return super.subtract(other, context);
    }

    /**
    * Convert to Java object (for passing to external functions)
    */

    public Object convertToJava(Class target, XPathContext context) throws XPathException {
        if (target.isAssignableFrom(Date.class)) {
            return getCalendar().getTime();
        } else if (target.isAssignableFrom(GregorianCalendar.class)) {
            return getCalendar();
        } else if (target.isAssignableFrom(DateTimeValue.class)) {
            return this;
        } else if (target==String.class || target==CharSequence.class) {
            return getStringValue();
        } else if (target==Object.class) {
            return getStringValue();
        } else {
            Object o = super.convertToJava(target, context);
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
            return new IntegerValue((year > 0 ? year : year-1));
        case Component.MONTH:
            return new IntegerValue(month);
        case Component.DAY:
            return new IntegerValue(day);
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
            // internal use only
            return new IntegerValue(microsecond);
        case Component.TIMEZONE:
            if (hasTimezone()) {
                return SecondsDurationValue.fromMilliseconds(getTimezoneInMinutes()*60*1000);
            } else {
                return null;
            }
        default:
            throw new IllegalArgumentException("Unknown component for dateTime: " + component);
        }
    }


    /**
    * Compare the value to another dateTime value.
     * <p>
     * This method is not used for XPath comparisons because it does not have access to the implicitTimezone
     * from the dynamic context. It is available for schema comparisons, although it does not currently
     * implement the XML Schema semantics for timezone comparison (which involve partial ordering)
    * @param other The other dateTime value
    * @return negative value if this one is the earler, 0 if they are chronologically equal,
    * positive value if this one is the later. For this purpose, dateTime values with an unknown
    * timezone are considered to be values in the implicit timezone (the Comparable interface requires
    * a total ordering).
    * @throws ClassCastException if the other value is not a DateTimeValue (the parameter
    * is declared as Object to satisfy the Comparable interface)
    */

    public int compareTo(Object other) {
        // TODO: implement the XML Schema comparison semantics (and remove the gross inefficiency)
        if (!(other instanceof DateTimeValue)) {
            throw new ClassCastException("DateTime values are not comparable to " + other.getClass());
        }
        return compareTo((DateTimeValue)other, new Configuration());
    }

    /**
    * Compare the value to another dateTime value, following the XPath comparison semantics
    * @param other The other dateTime value
    * @param config A Configuration used to supply the implicit timezone
    * @return negative value if this one is the earler, 0 if they are chronologically equal,
    * positive value if this one is the later. For this purpose, dateTime values with an unknown
    * timezone are considered to be values in the implicit timezone (the Comparable interface requires
    * a total ordering).
    * @throws ClassCastException if the other value is not a DateTimeValue (the parameter
    * is declared as Object to satisfy the Comparable interface)
    */

    public int compareTo(CalendarValue other, Configuration config) {
        if (!(other instanceof DateTimeValue)) {
            throw new ClassCastException("DateTime values are not comparable to " + other.getClass());
        }
        DateTimeValue v2 = (DateTimeValue)other;
        if (getTimezoneInMinutes() == v2.getTimezoneInMinutes()) {
            // both values are in the same timezone (explicitly or implicitly)
            if (year != v2.year) {
                return year - v2.year;
            }
            if (month != v2.month) {
                return month - v2.month;
            }
            if (day != v2.day) {
                return day - v2.day;
            }
            if (hour != v2.hour) {
                return hour - v2.hour;
            }
            if (minute != v2.minute) {
                return minute - v2.minute;
            }
            if (second != v2.second) {
                return second - v2.second;
            }
            if (microsecond != v2.microsecond) {
                return microsecond - v2.microsecond;
            }
            return 0;
        }
        return normalize(config).compareTo(v2.normalize(config), config);
    }



    public boolean equals(Object other) {
        // TODO: support schema semantics here
        return compareTo(other) == 0;
    }

    public int hashCode() {
        if (hasTimezone()) {
            return getCalendar().getTime().hashCode();
        } else {
            GregorianCalendar cal = new GregorianCalendar();
            int tz = (cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET)) / 60000;
            DateTimeValue v1 = (DateTimeValue)adjustTimezone(tz);
            return v1.getCalendar().getTime().hashCode();
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

