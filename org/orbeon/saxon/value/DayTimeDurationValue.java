package org.orbeon.saxon.value;

import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.FastStringBuffer;
import org.orbeon.saxon.sort.StringCollator;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.StringTokenizer;

/**
 * A value of type xs:dayTimeDuration
 */

public final class DayTimeDurationValue extends DurationValue implements Comparable {

    /**
     * Private constructor for internal use
     */

    private DayTimeDurationValue() {
        typeLabel = BuiltInAtomicType.DAY_TIME_DURATION;
    }

    /**
     * Factory method: create a duration value from a supplied string, in
     * ISO 8601 format [-]PnDTnHnMnS
     *
     * @param s the lexical representation of the xs:dayTimeDuration value
     * @return a DayTimeDurationValue if the format is correct, or a ValidationErrorValue if not
     */

    public static ConversionResult makeDayTimeDurationValue(CharSequence s) {
        int days = 0, hours = 0, minutes = 0, seconds = 0, microseconds = 0;
        boolean negative = false;
        int components = 0;
        StringTokenizer tok = new StringTokenizer(Whitespace.trimWhitespace(s).toString(), "-+.PDTHMS", true);
        if (!tok.hasMoreElements()) {
            return badDuration("empty string", s);
        }
        String part = (String)tok.nextElement();
        if ("+".equals(part)) {
            return badDuration("+ sign not allowed in a duration", s);
        } else if ("-".equals(part)) {
            negative = true;
            part = (String)tok.nextElement();
        }
        if (!"P".equals(part)) {
            return badDuration("missing 'P'", s);
        }
        int state = 0;
        while (tok.hasMoreElements()) {
            part = (String)tok.nextElement();
            if ("T".equals(part)) {
                state = 4;
                if (!tok.hasMoreElements()) {
                    return badDuration("T must be followed by time components", s);
                }
                part = (String)tok.nextElement();
            }
            int value = simpleInteger(part);
            if (value < 0) {
                return badDuration("non-numeric component", s);
            }
            if (!tok.hasMoreElements()) {
                return badDuration("missing unit letter at end", s);
            }
            char delim = ((String)tok.nextElement()).charAt(0);
            switch (delim) {
            case'D':
                if (state > 2) {
                    return badDuration("D is out of sequence", s);
                }
                days = value;
                components++;
                state = 3;
                break;
            case'H':
                if (state != 4) {
                    return badDuration("H is out of sequence", s);
                }
                hours = value;
                components++;
                state = 5;
                break;
            case'M':
                if (state < 4 || state > 5) {
                    return badDuration("M is out of sequence", s);
                }
                minutes = value;
                components++;
                state = 6;
                break;
            case'.':
                if (state < 4 || state > 6) {
                    return badDuration("misplaced decimal point", s);
                }
                seconds = value;
                components++;
                state = 7;
                break;
            case'S':
                if (state < 4 || state > 7) {
                    return badDuration("S is out of sequence", s);
                }
                if (state == 7) {
                    while (part.length() < 6) {
                        part += "0";
                    }
                    if (part.length() > 6) {
                        part = part.substring(0, 6);
                    }
                    value = simpleInteger(part);
                    if (value < 0) {
                        return badDuration("non-numeric microseconds component", s);
                    }
                    microseconds = value;
                } else {
                    seconds = value;
                }
                components++;
                state = 8;
                break;
            default:
                return badDuration("misplaced " + delim, s);
            }
        }
        if (components == 0) {
            return badDuration("Duration specifies no components", s);
        }
        try {
            //System.err.println(days + "days " + hours + " hours " + minutes + " minutes " + seconds + " seconds");
            return new DayTimeDurationValue((negative ? -1 : +1), days, hours, minutes, seconds, microseconds);
        } catch (IllegalArgumentException err) {
            return new ValidationFailure(err.getMessage());
        }
    }

    /**
     * Create a dayTimeDuration given the number of days, hours, minutes, and seconds. This
     * constructor performs no validation. The components (apart from sign) must all be non-negative
     * integers; they need not be normalized (for example, 36 hours is acceptable)
     *
     * @param sign         positive number for positive durations, negative for negative duratoins
     * @param days         number of days
     * @param hours        number of hours
     * @param minutes      number of minutes
     * @param seconds      number of seconds
     * @param microseconds number of microseconds
     * @throws IllegalArgumentException if the value is out of range; specifically, if the total
     * number of seconds exceeds 2^63; or if any of the values is negative
     */

    public DayTimeDurationValue(int sign, int days, int hours, int minutes, long seconds, int microseconds)
    throws IllegalArgumentException {
        if (days < 0 || hours < 0 || minutes < 0 || seconds < 0 || microseconds < 0) {
            throw new IllegalArgumentException("Negative component value");
        }
        if (((double)days)*(24*60*60) + ((double)hours)*(60*60) +
                ((double)minutes)*60 + (double)seconds > Long.MAX_VALUE) {
            throw new IllegalArgumentException("Duration seconds limit exceeded");
        }
        negative = (sign < 0);
        months = 0;
        long h = (long)days * 24L + (long)hours;
        long m = h * 60L + (long)minutes;
        long s = m * 60L + seconds;
        if (microseconds > 1000000) {
            s += microseconds / 1000000;
            microseconds %= 1000000;
        }
        this.seconds = s;
        this.microseconds = microseconds;
        if (s == 0 && microseconds == 0) {
            negative = false;
        }
        typeLabel = BuiltInAtomicType.DAY_TIME_DURATION;
    }

    /**
     * Create a copy of this atomic value, with a different type label
     *
     * @param typeLabel the type label of the new copy. The caller is responsible for checking that
     *                  the value actually conforms to this type.
     */

    public AtomicValue copyAsSubType(AtomicType typeLabel) {
        DayTimeDurationValue v = DayTimeDurationValue.fromMicroseconds(getLengthInMicroseconds());
        v.typeLabel = typeLabel;
        return v;
    }

    /**
     * Determine the primitive type of the value. This delivers the same answer as
     * getItemType().getPrimitiveItemType(). The primitive types are
     * the 19 primitive types of XML Schema, plus xs:integer, xs:dayTimeDuration and xs:yearMonthDuration,
     * and xs:untypedAtomic. For external objects, the result is AnyAtomicType.
     */

    public BuiltInAtomicType getPrimitiveType() {
        return BuiltInAtomicType.DAY_TIME_DURATION;
    }

    /**
     * Convert to string
     *
     * @return ISO 8601 representation.
     */

    public CharSequence getStringValueCS() {

        FastStringBuffer sb = new FastStringBuffer(32);
        if (negative) {
            sb.append('-');
        }

        int days = getDays();
        int hours = getHours();
        int minutes = getMinutes();
        int seconds = getSeconds();

        sb.append('P');
        if (days != 0) {
            sb.append(days + "D");
        }
        if (days == 0 || hours != 0 || minutes != 0 || seconds != 0 || microseconds != 0) {
            sb.append('T');
        }
        if (hours != 0) {
            sb.append(hours + "H");
        }
        if (minutes != 0) {
            sb.append(minutes + "M");
        }
        if (seconds != 0 || microseconds != 0 || (days == 0 && minutes == 0 && hours == 0)) {
            if (microseconds == 0) {
                sb.append(seconds + "S");
            } else {
                long ms = (seconds * 1000000) + microseconds;
                String mss = ms + "";
                if (seconds == 0) {
                    mss = "0000000" + mss;
                    mss = mss.substring(mss.length() - 7);
                }
                sb.append(mss.substring(0, mss.length() - 6));
                sb.append('.');
                int lastSigDigit = mss.length() - 1;
                while (mss.charAt(lastSigDigit) == '0') {
                    lastSigDigit--;
                }
                sb.append(mss.substring(mss.length() - 6, lastSigDigit + 1));
                sb.append('S');
            }
        }
        return sb;
    }

//    /**
//     * Normalize the value, for example 90M becomes 1H30M
//     */
//
//    public void normalize() throws ValidationException {
//        long seconds2 = seconds;
//        long minutes2 = minutes;
//        long hours2 = hours;
//        long days2 = days;
//        if (microseconds >= 1000000) {
//            seconds2 += (microseconds / 1000000);
//            microseconds = microseconds % 1000000;
//        }
//        if (seconds >= 60) {
//            minutes2 += (seconds2 / 60);
//            seconds2 = (int)(seconds2 % 60);
//        }
//        if (minutes2 >= 60) {
//            hours2 += (minutes2 / 60);
//            minutes2 = (int)(minutes2 % 60);
//        }
//        if (hours2 >= 24) {
//            days2 += (hours2 / 24);
//            if (days2 > Integer.MAX_VALUE || days2 < Integer.MIN_VALUE) {
//                throw new ValidationException("Duration exceeds implementation-defined limits");
//            }
//            hours2 = (int)(hours2 % 24);
//        }
//        days = (int)days2;
//        hours = (int)hours2;
//        minutes = (int)minutes2;
//        seconds = (int)seconds2;
//        normalizeZeroDuration();
//        normalized = true;
//    }

    /**
     * Get length of duration in seconds
     */

    public double getLengthInSeconds() {
        double a = seconds + ((double)microseconds / 1000000);
        // System.err.println("Duration length " + days + "/" + hours + "/" + minutes + "/" + seconds + " is " + a);
        return (negative ? -a : a);
    }

    /**
     * Get length of duration in milliseconds, as a long
     *
     * @return the length of the duration rounded to milliseconds (may be negative)
     */

    public long getLengthInMilliseconds() {
        long a = seconds * 1000 + (microseconds / 1000);
        return (negative ? -a : a);
    }

    /**
     * Get length of duration in microseconds, as a long
     *
     * @return the length in microseconds
     */

    public long getLengthInMicroseconds() {
        long a = seconds * 1000000 + microseconds;
        return (negative ? -a : a);
    }


    /**
     * Construct a duration value as a number of seconds.
     *
     * @param seconds the number of seconds in the duration. May be negative
     * @return the xs:dayTimeDuration value with the specified length
     */

    public static DayTimeDurationValue fromSeconds(BigDecimal seconds) throws XPathException {
        DayTimeDurationValue sdv = new DayTimeDurationValue();
        sdv.negative = (seconds.signum() < 0);
        if (sdv.negative) {
            seconds = seconds.negate();
        }
        BigDecimal microseconds = seconds.multiply(DecimalValue.BIG_DECIMAL_ONE_MILLION);
        BigInteger intMicros = microseconds.toBigInteger();
        BigInteger[] parts = intMicros.divideAndRemainder(BigInteger.valueOf(1000000));
        sdv.seconds = parts[0].longValue();
        sdv.microseconds = parts[1].intValue();
        return sdv;
    }

    /**
     * Construct a duration value as a number of milliseconds.
     *
     * @param milliseconds the number of milliseconds in the duration (may be negative)
     * @return the corresponding xs:dayTimeDuration value
     * @throws ValidationException if implementation-defined limits are exceeded, specifically
     * if the total number of seconds exceeds 2^63.
     */

    public static DayTimeDurationValue fromMilliseconds(long milliseconds) throws ValidationException {
        int sign = longSignum(milliseconds);
        if (sign < 0) {
            milliseconds = -milliseconds;
        }
        try {
            return new DayTimeDurationValue(
                    sign, 0, 0, 0, milliseconds / 1000, (int)(milliseconds % 1000) * 1000);
        } catch (IllegalArgumentException err) {
            // limits exceeded
            throw new ValidationException("Duration exceeds limits");
        }
    }

    /**
     * Get the signum of a long (Not available as Long.signum() until JDK 1.5)
     * @param value the supplied long
     * @return the signum of the supplied value
     */

    private static int longSignum(long value) {
        if (value > 0) {
            return +1;
        } else if (value == 0) {
            return 0;
        } else {
            return -1;
        }
    }

    /**
     * Construct a duration value as a number of microseconds.
     *
     * @param microseconds the number of microseconds in the duration. The maximum and minimum
     *                     limits are such that the number of days in the duration must fit in a 32-bit signed integer.
     * @return the xs:dayTimeDuration represented by the given number of microseconds
     * @throws IllegalArgumentException if the value is out of range.
     */

    public static DayTimeDurationValue fromMicroseconds(long microseconds) throws IllegalArgumentException {
        int sign = longSignum(microseconds);
        if (sign < 0) {
            microseconds = -microseconds;
        }
        return new DayTimeDurationValue(
                sign, 0, 0, 0, microseconds / 1000000, (int)(microseconds % 1000000));

    }


    /**
     * Multiply duration by a number. This is also used when dividing a duration by a number.
     */

    public DurationValue multiply(double n) throws XPathException {
        if (Double.isNaN(n)) {
            XPathException err = new XPathException("Cannot multiply/divide a duration by NaN");
            err.setErrorCode("FOCA0005");
            throw err;
        }
        double m = (double)getLengthInMicroseconds();
        double product = n * m;
        if (Double.isInfinite(product) || Double.isNaN(product) ||
                product > Long.MAX_VALUE || product < Long.MIN_VALUE) {
            XPathException err = new XPathException("Overflow when multiplying/dividing a duration by a number");
            err.setErrorCode("FODT0002");
            throw err;
        }
        try {
            return fromMicroseconds((long)product);
        } catch (IllegalArgumentException err) {
            if (err.getCause() instanceof XPathException) {
                throw (XPathException)err.getCause();
            } else {
                XPathException err2 = new XPathException("Overflow when multiplying/dividing a duration by a number", err);
                err2.setErrorCode("FODT0002");
                throw err2;
            }
        }
    }

    /**
     * Find the ratio between two durations
     *
     * @param other the dividend
     * @return the ratio, as a decimal
     * @throws XPathException
     */
    public DecimalValue divide(DurationValue other) throws XPathException {
        if (other instanceof DayTimeDurationValue) {
            BigDecimal v1 = BigDecimal.valueOf(getLengthInMicroseconds());
            BigDecimal v2 = BigDecimal.valueOf(((DayTimeDurationValue)other).getLengthInMicroseconds());
            if (v2.signum() == 0) {
                XPathException err = new XPathException("Divide by zero (durations)");
                err.setErrorCode("FOAR0001");
                throw err;
            }
            return new DecimalValue(v1.divide(v2, 20, BigDecimal.ROUND_HALF_EVEN));
        } else {
            XPathException err = new XPathException("Cannot divide two durations of different type");
            err.setErrorCode("XPTY0004");
            throw err;
        }
    }

    /**
     * Add two dayTimeDurations
     */

    public DurationValue add(DurationValue other) throws XPathException {
        if (other instanceof DayTimeDurationValue) {
            try {
                return fromMicroseconds(getLengthInMicroseconds() +
                        ((DayTimeDurationValue)other).getLengthInMicroseconds());
            } catch (IllegalArgumentException e) {
                XPathException err = new XPathException("Overflow when adding two durations");
                err.setErrorCode("FODT0002");
                throw err;
            }
        } else {
            XPathException err = new XPathException("Cannot add two durations of different type");
            err.setErrorCode("XPTY0004");
            throw err;
        }
    }

    /**
     * Subtract two dayTime-durations
     */

    public DurationValue subtract(DurationValue other) throws XPathException {
        if (other instanceof DayTimeDurationValue) {
            try {
                return fromMicroseconds(getLengthInMicroseconds() -
                        ((DayTimeDurationValue)other).getLengthInMicroseconds());
            } catch (IllegalArgumentException e) {
                XPathException err = new XPathException("Overflow when subtracting two durations");
                err.setErrorCode("FODT0002");
                throw err;
            }
        } else {
            XPathException err = new XPathException("Cannot subtract two durations of different type");
            err.setErrorCode("XPTY0004");
            throw err;
        }
    }

    /**
     * Negate a duration (same as subtracting from zero, but it preserves the type of the original duration)
     *
     * @throws IllegalArgumentException in the extremely unlikely event that the duration is one that cannot
     *          be negated (because the limit for positive durations is one second 
     *          off from the limit for negative durations)
     */

    public DurationValue negate() throws IllegalArgumentException {
        return fromMicroseconds(-getLengthInMicroseconds());
    }

    /**
     * Compare the value to another duration value
     *
     * @param other The other dateTime value
     * @return negative value if this one is the earler, 0 if they are chronologically equal,
     *         positive value if this one is the later. For this purpose, dateTime values with an unknown
     *         timezone are considered to be UTC values (the Comparable interface requires
     *         a total ordering).
     * @throws ClassCastException if the other value is not a DateTimeValue (the parameter
     *                            is declared as Object to satisfy the Comparable interface)
     */

    public int compareTo(Object other) {
        if (other instanceof DayTimeDurationValue) {
            long diff = getLengthInMicroseconds() - ((DayTimeDurationValue)other).getLengthInMicroseconds();
            if (diff < 0) {
                return -1;
            } else if (diff > 0) {
                return +1;
            } else {
                return 0;
            }
        } else {
            throw new ClassCastException("Cannot compare a dayTimeDuration to an object of class "
                    + other.getClass());
        }
    }

    /**
     * Get a Comparable value that implements the XPath ordering comparison semantics for this value.
     * Returns null if the value is not comparable according to XPath rules. The default implementation
     * returns the value itself. This is modified for types such as
     * xs:duration which allow ordering comparisons in XML Schema, but not in XPath.
     * @param ordered true if an ordered comparable is needed
     * @param collator Collation used for string comparison
     * @param context XPath dynamic context
     */

    public Object getXPathComparable(boolean ordered, StringCollator collator, XPathContext context) {
        return this;
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

