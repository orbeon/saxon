package org.orbeon.saxon.value;

import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.FastStringBuffer;
import org.orbeon.saxon.sort.StringCollator;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.AtomicType;
import org.orbeon.saxon.type.BuiltInAtomicType;
import org.orbeon.saxon.type.ConversionResult;

import java.math.BigDecimal;
import java.util.StringTokenizer;

/**
 * A value of type xs:yearMonthDuration
 */

public final class YearMonthDurationValue extends DurationValue implements Comparable {

    /**
     * Private constructor for internal use
     */

    private YearMonthDurationValue() {
        typeLabel = BuiltInAtomicType.YEAR_MONTH_DURATION;
    }

    /**
     * Static factory: create a duration value from a supplied string, in
     * ISO 8601 format [+|-]PnYnM
     *
     * @param s a string in the lexical space of xs:yearMonthDuration.
     * @return either a YearMonthDurationValue, or a ValidationFailure if the string was
     *         not in the lexical space of xs:yearMonthDuration.
     */

    public static ConversionResult makeYearMonthDurationValue(CharSequence s) {
        int years = 0, months = 0;
        boolean negative = false;
        int components = 0;
        StringTokenizer tok = new StringTokenizer(Whitespace.trimWhitespace(s).toString(), "-+PYM", true);
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
            int value = simpleInteger(part);
            if (value < 0) {
                return badDuration("non-numeric component", s);
            }
            if (!tok.hasMoreElements()) {
                return badDuration("missing unit letter at end", s);
            }
            char delim = ((String)tok.nextElement()).charAt(0);
            switch (delim) {
            case'Y':
                if (state > 0) {
                    return badDuration("Y is out of sequence", s);
                }
                years = value;
                components++;
                state = 1;
                break;
            case'M':
                if (state == 0 || state == 1) {
                    months = value;
                    components++;
                    state = 2;
                    break;
                } else {
                    return badDuration("M is out of sequence", s);
                }

            default:
                return badDuration("misplaced " + delim, s);
            }
        }
        if (components == 0) {
            return badDuration("duration specifies no components", s);
        }
        if ((long)years + ((long)months) * 12 > Integer.MAX_VALUE) {
            return badDuration("duration exceeds limits", s);
        }
        return YearMonthDurationValue.fromMonths((years*12 + months) * (negative ? -1 : +1));
    }

    /**
     * Create a copy of this atomic value, with a different type label
     *
     * @param typeLabel the type label of the new copy. The caller is responsible for checking that
     *                  the value actually conforms to this type.
     */

    public AtomicValue copyAsSubType(AtomicType typeLabel) {
        YearMonthDurationValue v = YearMonthDurationValue.fromMonths(getLengthInMonths());
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
        return BuiltInAtomicType.YEAR_MONTH_DURATION;
    }

    /**
     * Convert to string
     *
     * @return ISO 8601 representation.
     */

    public CharSequence getStringValueCS() {

        // The canonical representation has months in the range 0-11

        int y = getYears();
        int m = getMonths();

        FastStringBuffer sb = new FastStringBuffer(32);
        if (negative) {
            sb.append('-');
        }
        sb.append('P');
        if (y != 0) {
            sb.append(y + "Y");
        }
        if (m != 0 || y == 0) {
            sb.append(m + "M");
        }
        return sb;

    }

    /**
     * Get the number of months in the duration
     *
     * @return the number of months in the duration
     */

    public int getLengthInMonths() {
        return (months) * (negative ? -1 : +1);
    }

    /**
     * Construct a duration value as a number of months.
     *
     * @param months the number of months (may be negative)
     * @return the corresponding xs:yearMonthDuration value
     */

    public static YearMonthDurationValue fromMonths(int months) {
        YearMonthDurationValue mdv = new YearMonthDurationValue();
        mdv.negative = (months < 0);
        mdv.months = (months < 0 ? -months : months);
        mdv.seconds = 0;
        mdv.microseconds = 0;
        return mdv;
    }

    /**
     * Multiply duration by a number. Also used when dividing a duration by a number
     */

    public DurationValue multiply(double n) throws XPathException {
        if (Double.isNaN(n)) {
            XPathException err = new XPathException("Cannot multiply/divide a duration by NaN");
            err.setErrorCode("FOCA0005");
            throw err;
        }
        double m = (double)getLengthInMonths();
        double product = n * m;
        if (Double.isInfinite(product) || product > Integer.MAX_VALUE || product < Integer.MIN_VALUE) {
            XPathException err = new XPathException("Overflow when multiplying/dividing a duration by a number");
            err.setErrorCode("FODT0002");
            throw err;
        }
        return fromMonths((int)Math.round(product));
    }

    /**
     * Find the ratio between two durations
     *
     * @param other the dividend
     * @return the ratio, as a decimal
     * @throws XPathException
     */

    public DecimalValue divide(DurationValue other) throws XPathException {
        if (other instanceof YearMonthDurationValue) {
            BigDecimal v1 = BigDecimal.valueOf(getLengthInMonths());
            BigDecimal v2 = BigDecimal.valueOf(((YearMonthDurationValue)other).getLengthInMonths());
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
     * Add two year-month-durations
     */

    public DurationValue add(DurationValue other) throws XPathException {
        if (other instanceof YearMonthDurationValue) {
            return fromMonths(getLengthInMonths() +
                    ((YearMonthDurationValue)other).getLengthInMonths());
        } else {
            XPathException err = new XPathException("Cannot add two durations of different type");
            err.setErrorCode("XPTY0004");
            throw err;
        }
    }

    /**
     * Subtract two year-month-durations
     */

    public DurationValue subtract(DurationValue other) throws XPathException {
        if (other instanceof YearMonthDurationValue) {
            return fromMonths(getLengthInMonths() -
                    ((YearMonthDurationValue)other).getLengthInMonths());
        } else {
            XPathException err = new XPathException("Cannot subtract two durations of different type");
            err.setErrorCode("XPTY0004");
            throw err;
        }
    }

    /**
     * Negate a duration (same as subtracting from zero, but it preserves the type of the original duration)
     */

    public DurationValue negate() {
        return fromMonths(-getLengthInMonths());
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
        if (other instanceof YearMonthDurationValue) {
            return getLengthInMonths() - ((YearMonthDurationValue)other).getLengthInMonths();
        } else {
            throw new ClassCastException("Cannot compare a yearMonthDuration to an object of class "
                    + other.getClass());
        }
    }

    /**
     * Get a Comparable value that implements the XPath ordering comparison semantics for this value.
     * Returns null if the value is not comparable according to XPath rules. The default implementation
     * returns the value itself. This is modified for types such as
     * xs:duration which allow ordering comparisons in XML Schema, but not in XPath.
     * @param ordered
     * @param collator
     * @param context
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

