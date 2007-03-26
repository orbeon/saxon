package org.orbeon.saxon.value;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.FastStringBuffer;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.type.ValidationException;

import java.util.StringTokenizer;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
* A value of type xsd:dayTimeDuration
*/

public final class SecondsDurationValue extends DurationValue implements Comparable {

    /**
    * Private constructor for internal use
    */

    private SecondsDurationValue() {
    }

    /**
    * Constructor: create a duration value from a supplied string, in
    * ISO 8601 format [-]PnDTnHnMnS
    */

    public SecondsDurationValue(CharSequence s) throws XPathException {

        int components = 0;
        StringTokenizer tok = new StringTokenizer(Whitespace.trimWhitespace(s).toString(), "-+.PDTHMS", true);
        try {
            if (!tok.hasMoreElements()) badDuration("empty string", s);
            String part = (String)tok.nextElement();
            if ("+".equals(part)) {
                badDuration("+ sign not allowed in a duration", s);
            } else if ("-".equals(part)) {
                negative = true;
                part = (String)tok.nextElement();
            }
            if (!"P".equals(part)) badDuration("missing 'P'", s);
            int state = 0;
            while (tok.hasMoreElements()) {
                part = (String)tok.nextElement();
                if ("T".equals(part)) {
                    state = 4;
                    if (!tok.hasMoreElements()) {
                        badDuration("T must be followed by time components", s);
                    }
                    part = (String)tok.nextElement();
                }
                int value = Integer.parseInt(part);
                if (!tok.hasMoreElements()) badDuration("missing unit letter at end", s);
                char delim = ((String)tok.nextElement()).charAt(0);
                switch (delim) {
                    case 'D':
                        if (state > 2) badDuration("D is out of sequence", s);
                        days = value;
                        components++;
                        state = 3;
                        break;
                    case 'H':
                        if (state != 4) badDuration("H is out of sequence", s);
                        hours = value;
                        components++;
                        state = 5;
                        break;
                    case 'M':
                        if (state < 4 || state > 5) badDuration("M is out of sequence", s);
                        minutes = value;
                        components++;
                        state = 6;
                        break;
                    case '.':
                        if (state < 4 || state > 6) badDuration("misplaced decimal point", s);
                        seconds = value;
                        components++;
                        state = 7;
                        break;
                    case 'S':
                        if (state < 4 || state > 7) badDuration("S is out of sequence", s);
                        if (state==7) {
                            while (part.length() < 6) part += "0";
                            if (part.length() > 6) part = part.substring(0, 6);
                            microseconds = Integer.parseInt(part);
                        } else {
                            seconds = value;
                        }
                        components++;
                        state = 8;
                        break;
                   default:
                        badDuration("misplaced " + delim, s);
                }
            }
            if (components == 0) {
                badDuration("Duration specifies no components", s);
            }
            normalize();

        } catch (NumberFormatException err) {
            badDuration("non-numeric or out-of-range component", s);
        }
    }

    /**
     * Create a dayTimeDuration given the number of days, hours, minutes, and seconds
     */

    public SecondsDurationValue(int sign, int days, int hours, int minutes, int seconds, int microseconds)
    throws ValidationException {
        this.negative = (sign<0);
        this.years = 0;
        this.months = 0;
        this.days = days;
        this.hours = hours;
        this.minutes = minutes;
        this.seconds = seconds;
        this.microseconds = microseconds;
        normalize();
    }

    /**
    * Convert to string
    * @return ISO 8601 representation.
    */

    public CharSequence getStringValueCS() {

        FastStringBuffer sb = new FastStringBuffer(32);
        if (negative) {
            sb.append('-');
        }
        sb.append('P');
        if (days != 0) {
            sb.append(days + "D");
        }
        if ( days==0 || hours!=0 || minutes!=0 || seconds!=0 || microseconds!=0) {
            sb.append('T');
        }
        if (hours != 0) {
            sb.append(hours + "H");
        }
        if (minutes != 0) {
            sb.append(minutes + "M");
        }
        if (seconds != 0 || microseconds != 0 || (days==0 && minutes==0 && hours==0)) {
            if (microseconds == 0) {
                sb.append(seconds + "S");
            } else {
                long ms = (seconds * 1000000) + microseconds;
                String mss = ms + "";
                if (seconds == 0) {
                    mss = "0000000" + mss;
                    mss = mss.substring(mss.length()-7);
                }
                sb.append(mss.substring(0, mss.length()-6));
                sb.append('.');
                int lastSigDigit = mss.length()-1;
                while (mss.charAt(lastSigDigit) == '0') {
                    lastSigDigit--;
                }
                sb.append(mss.substring(mss.length()-6, lastSigDigit+1));
                sb.append('S');
            }
        }
        return sb;
    }

    /**
    * Normalize the value, for example 90M becomes 1H30M
    */

    public void normalize() throws ValidationException {
        long seconds2 = seconds;
        long minutes2 = minutes;
        long hours2 = hours;
        long days2 = days;
        if (microseconds >= 1000000) {
            seconds2 += (microseconds / 1000000);
            microseconds = microseconds % 1000000;
        }
        if (seconds >= 60) {
            minutes2 += (seconds2 / 60);
            seconds2 = (int)(seconds2 % 60);
        }
        if (minutes2 >= 60) {
            hours2 += (minutes2 / 60);
            minutes2 = (int)(minutes2 % 60);
        }
        if (hours2 >= 24) {
            days2 += (hours2 / 24);
            if (days2 > Integer.MAX_VALUE || days2 < Integer.MIN_VALUE) {
                throw new ValidationException("Duration exceeds implementation-defined limits");
            }
            hours2 = (int)(hours2 % 24);
        }
        days = (int)days2;
        hours = (int)hours2;
        minutes = (int)minutes2;
        seconds = (int)seconds2;
        normalizeZeroDuration();
        normalized = true;
    }

    /**
    * Get length of duration in seconds
    */

    public double getLengthInSeconds() {
        double a = days;
        a = a*24 + hours;
        a = a*60 + minutes;
        a = a*60 + seconds;
        a += ((double)microseconds / 1000000);
        // System.err.println("Duration length " + days + "/" + hours + "/" + minutes + "/" + seconds + " is " + a);
        return (negative ? -a : a);
    }

    /**
     * Get length of duration in milliseconds, as a long
     */

    public long getLengthInMilliseconds() {
        long a = days;
        a = a*24 + hours;
        a = a*60 + minutes;
        a = a*60 + seconds;
        a = a*1000 + (microseconds / 1000);
        return (negative ? -a : a);
    }

    /**
     * Get length of duration in microseconds, as a long
     */

    public long getLengthInMicroseconds() {
        long a = days;
        a = a*24 + hours;
        a = a*60 + minutes;
        a = a*60 + seconds;
        a = a*1000000 + microseconds;
        return (negative ? -a : a);
    }


    /**
    * Construct a duration value as a number of seconds.
    */

    public static SecondsDurationValue fromSeconds(BigDecimal seconds) throws XPathException {
        SecondsDurationValue sdv = new SecondsDurationValue();
        sdv.negative = (seconds.signum()<0);
        if (sdv.negative) {
            seconds = seconds.negate();
        }
        BigDecimal microseconds = seconds.multiply(DecimalValue.BIG_DECIMAL_ONE_MILLION);
        BigInteger intMicros = microseconds.toBigInteger();
        BigInteger[] parts = intMicros.divideAndRemainder(BigInteger.valueOf(1000000));
        long secs = parts[0].longValue();
        // done this way to avoid overflow
        sdv.days = (int)(secs / (24L*60L*60L));
        sdv.seconds = (int)(secs % (24L*60L*60L));
        sdv.microseconds = parts[1].intValue();
        sdv.normalize();
        return sdv;
    }

    /**
    * Construct a duration value as a number of milliseconds.
    */

    public static SecondsDurationValue fromMilliseconds(long milliseconds) throws XPathException {
        SecondsDurationValue sdv = new SecondsDurationValue();
        sdv.negative = (milliseconds<0);
        milliseconds = Math.abs(milliseconds);
        long seconds = milliseconds/1000;
        sdv.days = (int)(seconds / (3600*24));
        sdv.seconds = (int)(seconds % (3600*24));
        sdv.microseconds = (int)(milliseconds % 1000) * 1000;
        sdv.normalize();
        return sdv;
    }

    /**
    * Construct a duration value as a number of microseconds.
    */

    public static SecondsDurationValue fromMicroseconds(long microseconds) throws XPathException {
        SecondsDurationValue sdv = new SecondsDurationValue();
        sdv.negative = (microseconds<0);
        microseconds = Math.abs(microseconds);
        long seconds = microseconds/1000000L;
        sdv.days = (int)(seconds / (3600*24));
        sdv.seconds = (int)(seconds % (3600*24));
        sdv.microseconds = (int)(microseconds % 1000000L);
        sdv.normalize();
        return sdv;
    }


    /**
    * Multiply duration by a number. This is also used when dividing a duration by a number.
    */

    public DurationValue multiply(double n) throws XPathException {
        if (Double.isNaN(n)) {
            DynamicError err = new DynamicError("Cannot multiply/divide a duration by NaN");
            err.setErrorCode("FOCA0005");
            throw err;
        }
        double m = (double)getLengthInMicroseconds();
        double product = n*m;
        if (Double.isInfinite(product) || Double.isNaN(product) ||
                product > Long.MAX_VALUE || product < Long.MIN_VALUE) {
            DynamicError err = new DynamicError("Overflow when multiplying/dividing a duration by a number");
            err.setErrorCode("FODT0002");
            throw err;
        }
        return fromMicroseconds((long)product);
    }

    /**
     * Find the ratio between two durations
     * @param other the dividend
     * @return the ratio, as a decimal
     * @throws XPathException
     */
    public DecimalValue divide(DurationValue other) throws XPathException {
        if (other instanceof SecondsDurationValue) {
            BigDecimal v1 = BigDecimal.valueOf(this.getLengthInMicroseconds());
            BigDecimal v2 = BigDecimal.valueOf(((SecondsDurationValue)other).getLengthInMicroseconds());
            if (v2.signum() == 0) {
                DynamicError err = new DynamicError("Divide by zero (durations)");
                err.setErrorCode("FOAR0001");
                throw err;
            }
            return new DecimalValue(v1.divide(v2, 20, BigDecimal.ROUND_HALF_EVEN));
        } else {
            throw new DynamicError("Cannot divide two durations of different type");
        }
    }

    /**
    * Add two dayTimeDurations
    */

    public DurationValue add(DurationValue other) throws XPathException {
        if (other instanceof SecondsDurationValue) {
            return fromMicroseconds(this.getLengthInMicroseconds() +
                    ((SecondsDurationValue)other).getLengthInMicroseconds());
        } else {
            DynamicError err = new DynamicError("Cannot add two durations of different type");
            err.setErrorCode("XPTY0004");
            throw err;
        }
    }

    /**
    * Subtract two dayTime-durations
    */

    public DurationValue subtract(DurationValue other) throws XPathException {
        if (other instanceof SecondsDurationValue) {
            return fromMicroseconds(this.getLengthInMicroseconds() -
                    ((SecondsDurationValue)other).getLengthInMicroseconds());
        } else {
            DynamicError err = new DynamicError("Cannot subtract two durations of different type");
            err.setErrorCode("XPTY0004");
            throw err;
        }
    }

    /**
     * Negate a duration (same as subtracting from zero, but it preserves the type of the original duration)
     */

    public DurationValue negate() {
        try {
            return fromMicroseconds(-this.getLengthInMicroseconds());
        } catch (XPathException err) {
            throw new AssertionError("Failed to negate a dayTimeDuration");
        }
    }

    /**
    * Compare the value to another duration value
    * @param other The other dateTime value
    * @return negative value if this one is the earler, 0 if they are chronologically equal,
    * positive value if this one is the later. For this purpose, dateTime values with an unknown
    * timezone are considered to be UTC values (the Comparable interface requires
    * a total ordering).
    * @throws ClassCastException if the other value is not a DateTimeValue (the parameter
    * is declared as Object to satisfy the Comparable interface)
    */

    public int compareTo(Object other) {
        if (other instanceof SecondsDurationValue) {
            long diff = this.getLengthInMicroseconds() - ((SecondsDurationValue)other).getLengthInMicroseconds();
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
    * Determine the data type of the exprssion
    * @return Type.DAY_TIME_DURATION,
     * @param th
     */

    public ItemType getItemType(TypeHierarchy th) {
        return Type.DAY_TIME_DURATION_TYPE;
    }

    /**
    * Convert to Java object (for passing to external functions)
    */

    public Object convertToJava(Class target, XPathContext context) throws XPathException {
        if (target.isAssignableFrom(DurationValue.class)) {
            return this;
        } else if (target==String.class || target==CharSequence.class) {
            return getStringValue();
        } else if (target==Object.class) {
            return getStringValue();
        } else {
            throw new DynamicError("Conversion of dayTimeDuration to " + target.getName() +
                        " is not supported");
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

