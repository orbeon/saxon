package org.orbeon.saxon.value;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.functions.Component;
import org.orbeon.saxon.om.FastStringBuffer;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.trans.SaxonErrorCode;
import org.orbeon.saxon.type.*;

import java.util.StringTokenizer;

/**
* A value of type xs:duration
*/

public class DurationValue extends AtomicValue {

    // TODO: the spec has moved away from treating a duration as a 6-tuple to treating it as (months + seconds)

    protected boolean negative = false;
    protected int years = 0;
    protected int months = 0;
    protected int days = 0;
    protected int hours = 0;
    protected int minutes = 0;
    protected int seconds = 0;
    protected int microseconds = 0;
    protected boolean normalized = false;

    /**
    * Private constructor for internal use
    */

    protected DurationValue() {
    }

    public DurationValue(boolean positive, int years, int months, int days,
                         int hours, int minutes, int seconds, int microseconds) {
        this.negative = !positive;
        this.years = years;
        this.months = months;
        this.days = days;
        this.hours = hours;
        this.minutes = minutes;
        this.seconds = seconds;
        this.microseconds = microseconds;
        normalizeZeroDuration();
        normalized = (months<12 && hours<24 && minutes<60 && seconds<60 && microseconds<1000000);
    }

    protected void normalizeZeroDuration() {
        if (years==0 && months==0 && days==0 && hours==0 && minutes==0 && seconds==0 && microseconds==0) {
            // don't allow negative zero
            negative = false;
        }
    }

    /**
    * Constructor: create a duration value from a supplied string, in
    * ISO 8601 format [-]PnYnMnDTnHnMnS
    */

    public DurationValue(CharSequence s) throws XPathException {
        StringTokenizer tok = new StringTokenizer(Whitespace.trimWhitespace(s).toString(), "-+.PYMDTHS", true);
        int components = 0;
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
                    case 'Y':
                        if (state > 0) badDuration("Y is out of sequence", s);
                        years = value;
                        state = 1;
                        components++;
                        break;
                    case 'M':
                        if (state == 4 || state==5) {
                            minutes = value;
                            state = 6;
                            components++;
                            break;
                        } else if (state == 0 || state==1) {
                            months = value;
                            state = 2;
                            components++;
                            break;
                        } else {
                            badDuration("M is out of sequence", s);
                        }
                    case 'D':
                        if (state > 2) badDuration("D is out of sequence", s);
                        days = value;
                        state = 3;
                        components++;
                        break;
                    case 'H':
                        if (state != 4) badDuration("H is out of sequence", s);
                        hours = value;
                        state = 5;
                        components++;
                        break;
                    case '.':
                        if (state < 4 || state > 6) badDuration("misplaced decimal point", s);
                        seconds = value;
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
                        state = 8;
                        components++;
                        break;
                   default:
                        badDuration("misplaced " + delim, s);
                }
            }

            if (components == 0) {
                badDuration("Duration specifies no components", s);
            }
            // Note, duration values (unlike the two xdt: subtypes) are not normalized.
            // However, negative zero durations are normalized to positive

            normalizeZeroDuration();

        } catch (NumberFormatException err) {
            badDuration("non-numeric component", s);
        }
    }

    protected void badDuration(String msg, CharSequence s) throws XPathException {
        DynamicError err = new DynamicError("Invalid duration value '" + s + "' (" + msg + ')');
        err.setErrorCode("FORG0001");
        throw err;
    }

    /**
    * Convert to target data type
     * @param requiredType an integer identifying the required atomic type
     * @param validate if set to false, the caller asserts that the value is known to be valid
     * @param context
     * @return an AtomicValue, a value of the required type; or a {@link ValidationErrorValue} if
     * the value cannot be converted.
    */

    public AtomicValue convertPrimitive(BuiltInAtomicType requiredType, boolean validate, XPathContext context) {
        //System.err.println("Convert duration " + getClass() + " to " + Type.getTypeName(requiredType));
        switch(requiredType.getPrimitiveType()) {
        case Type.DURATION:
        case Type.ANY_ATOMIC:
        case Type.ITEM:
            return this;
        case Type.STRING:
            return new StringValue(getStringValueCS());
        case Type.UNTYPED_ATOMIC:
            return new UntypedAtomicValue(getStringValueCS());
        case Type.YEAR_MONTH_DURATION:
            return MonthDurationValue.fromMonths((years*12 + months) * (negative ? -1 : +1));
        case Type.DAY_TIME_DURATION:
            try {
                return new SecondsDurationValue((negative?-1:+1), days, hours, minutes, seconds, microseconds);
            } catch (ValidationException err) {
                return new ValidationErrorValue(err);
            }
        default:
            ValidationException err = new ValidationException("Cannot convert duration to " +
                                     requiredType.getDisplayName());
            err.setErrorCode("XPTY0004");
            err.setIsTypeError(true);
            return new ValidationErrorValue(err);
        }
    }

    /**
     * Normalize the duration, so that months<12, hours<24, minutes<60, seconds<60.
     * At present we do this when converting to a string. It's possible that it should be done immediately
     * on constructing the duration (so that component extraction functions get the normalized value).
     * We're awaiting clarification of the spec (bugzilla 3369)
     * @return a new, normalized duration
     */

    public DurationValue normalizeDuration() {
        int totalMonths = years*12 + months;
        int years = totalMonths / 12;
        int months = totalMonths % 12;
        long totalMicroSeconds = ((((((days*24L + hours)*60L)+minutes)*60L)+seconds)*1000000L)+microseconds;
        int microseconds = (int)(totalMicroSeconds % 1000000L);
        int totalSeconds = (int)(totalMicroSeconds / 1000000L);
        int seconds = totalSeconds % 60;
        int totalMinutes = totalSeconds / 60;
        int minutes = totalMinutes % 60;
        int totalHours = totalMinutes / 60;
        int hours = totalHours % 24;
        int days = totalHours / 24;
        return new DurationValue(!negative, years, months, days, hours, minutes, seconds, microseconds);

    }

    /**
     * Return the signum of the value
     * @return -1 if the duration is negative, zero if it is zero-length, +1 if it is positive
     */

    public int signum() {
        if (negative) {
            return -1;
        }
        if (years == 0 && months ==0 && days == 0 && hours == 0 && minutes == 0 && seconds == 0 && microseconds == 0) {
            return 0;
        }
        return +1;
    }


    /**
     * Convert the value to a string, using the serialization rules.
     * For atomic values this is the same as a cast; for sequence values
     * it gives a space-separated list. This method is refined for AtomicValues
     * so that it never throws an Exception.
     */

    public String getStringValue() {
        return getStringValueCS().toString();
    }

    /**
    * Convert to string
    * @return ISO 8601 representation.
    */

    public CharSequence getStringValueCS() {

        // Note, Schema does not define a canonical representation. We omit all zero components, unless
        // the duration is zero-length, in which case we output PT0S.

        if (years==0 && months==0 && days==0 && hours==0 && minutes==0 && seconds==0 && microseconds==0) {
            return "PT0S";
        }

        if (!normalized) {
            return normalizeDuration().getStringValueCS();
        }

        FastStringBuffer sb = new FastStringBuffer(32);
        if (negative) {
            sb.append('-');
        }
        sb.append("P");
        if (years != 0) {
            sb.append(years + "Y");
        }
        if (months != 0) {
            sb.append(months + "M");
        }
        if (days != 0) {
            sb.append(days + "D");
        }
        if (hours != 0 || minutes != 0 || seconds != 0 || microseconds != 0) {
            sb.append("T");
        }
        if (hours != 0) {
            sb.append(hours + "H");
        }
        if (minutes != 0) {
            sb.append(minutes + "M");
        }
        if (seconds != 0 || microseconds != 0) {
            if (seconds != 0 && microseconds == 0) {
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
    * Get length of duration in seconds, assuming an average length of month. (Note, this defines a total
    * ordering on durations which is different from the partial order defined in XML Schema; XPath 2.0
    * currently avoids defining an ordering at all. But the ordering here is consistent with the ordering
    * of the two duration subtypes in XPath 2.0.)
    */

    public double getLengthInSeconds() {
        double a = years;
        a = a*12 + months;
        a = a*(365.242199/12.0) + days;
        a = a*24 + hours;
        a = a*60 + minutes;
        a = a*60 + seconds;
        a = a + ((double)microseconds / 1000000);
        return (negative ? -a : a);
    }

    /**
    * Determine the data type of the exprssion
    * @return Type.DURATION,
     * @param th
     */

    public ItemType getItemType(TypeHierarchy th) {
        return Type.DURATION_TYPE;
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
            Object o = super.convertToJava(target, context);
            if (o == null) {
                DynamicError err = new DynamicError("Conversion of xs:duration to " + target.getName() +
                        " is not supported");
                err.setXPathContext(context);
                err.setErrorCode(SaxonErrorCode.SXJE0003);
            }
            return o;
        }
    }

    /**
    * Get a component of the normalized value
    */

    public AtomicValue getComponent(int component) throws XPathException {
        // TODO: there's no longer any need to maintain unnormalized values
        if (!normalized) {
            return normalizeDuration().getComponent(component);
        }
        switch (component) {
        case Component.YEAR:
            return new IntegerValue((negative?-years:years));
        case Component.MONTH:
            return new IntegerValue((negative?-months:months));
        case Component.DAY:
            return new IntegerValue((negative?-days:days));
        case Component.HOURS:
            return new IntegerValue((negative?-hours:hours));
        case Component.MINUTES:
            return new IntegerValue((negative?-minutes:minutes));
        case Component.SECONDS:
            FastStringBuffer sb = new FastStringBuffer(16);
            String ms = ("000000" + microseconds);
            ms = ms.substring(ms.length()-6);
            sb.append((negative?"-":"") + seconds + '.' + ms);
            return DecimalValue.makeDecimalValue(sb, false);
        case Component.WHOLE_SECONDS:
            return new IntegerValue((negative?-seconds:seconds));
        case Component.MICROSECONDS:
            return new IntegerValue((negative?-microseconds:microseconds));
        default:
            throw new IllegalArgumentException("Unknown component for duration: " + component);
        }
    }


    /**
    * Compare the value to another duration value
    * @param other The other dateTime value
    * @return negative value if this one is the shorter duration, 0 if they are equal,
    * positive value if this one is the longer duration. For this purpose, a year is considered
    * to be equal to 365.242199 days.
    * @throws ClassCastException if the other value is not a DurationValue (the parameter
    * is declared as Object to satisfy the Comparable interface)
    */

//    public int compareTo(Object other) {
//        if (!(other instanceof DurationValue)) {
//            throw new ClassCastException("Duration values are not comparable to " + other.getClass());
//        }
//        double s1 = this.getLengthInSeconds();
//        double s2 = ((DurationValue)other).getLengthInSeconds();
//        if (s1==s2) return 0;
//        if (s1<s2) return -1;
//        return +1;
//    }

    /**
    * Test if the two durations are of equal length. Note: this function is defined
    * in XPath 2.0, but its semantics are currently unclear.
     * @throws ClassCastException if the other value is not an xs:duration or subtype thereof
    */

    public boolean equals(Object other) {
        DurationValue val = (DurationValue)((AtomicValue)other).getPrimitiveValue();

        DurationValue d1 = normalizeDuration();
        DurationValue d2 = val.normalizeDuration();
        return d1.negative == d2.negative &&
                d1.years == d2.years &&
                d1.months == d2.months &&
                d1.days == d2.days &&
                d1.hours == d2.hours &&
                d1.minutes == d2.minutes &&
                d1.seconds == d2.seconds &&
                d1.microseconds == d2.microseconds;
    }

    public int hashCode() {
        return new Double(getLengthInSeconds()).hashCode();
    }

    /**
    * Add two durations
    */

    public DurationValue add(DurationValue other) throws XPathException {
        DynamicError err = new DynamicError("Only subtypes of xs:duration can be added");
        err.setErrorCode("XPTY0004");
        err.setIsTypeError(true);
        throw err;
    }

    /**
    * Subtract two durations
    */

    public DurationValue subtract(DurationValue other) throws XPathException{
        DynamicError err = new DynamicError("Only subtypes of xs:duration can be subtracted");
        err.setErrorCode("XPTY0004");
        err.setIsTypeError(true);
        throw err;
    }

    /**
     * Negate a duration (same as subtracting from zero, but it preserves the type of the original duration)
     */

    public DurationValue negate() {
        return new DurationValue(negative, years, months, days, hours, minutes, seconds, microseconds);
    }

    /**
    * Multiply a duration by a number
    */

    public DurationValue multiply(double factor) throws XPathException {
        DynamicError err = new DynamicError("Only subtypes of xs:duration can be multiplied by a number");
        err.setErrorCode("XPTY0004");
        err.setIsTypeError(true);
        throw err;
    }

    /**
    * Divide a duration by a another duration
    */

    public DecimalValue divide(DurationValue other) throws XPathException {
        DynamicError err = new DynamicError("Only subtypes of xs:duration can be divided by another duration");
        err.setErrorCode("XPTY0004");
        err.setIsTypeError(true);
        throw err;
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

